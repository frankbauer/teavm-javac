import fs from 'node:fs/promises';
import path from 'node:path';
import { pathToFileURL } from 'node:url';
import vm from 'node:vm';

const rootDir = process.cwd();
const distDir = path.join(rootDir, 'dist', 'v102');

// Change working directory to distDir so relative paths in worker scripts work
process.chdir(distDir);

class MockXHR {
    constructor() {
        this.onload = null;
        this.onerror = null;
        this.onreadystatechange = null;
        this.status = 0;
        this.readyState = 0;
        this.response = null;
        this.responseType = '';
    }
    open(method, url) { 
        this.url = url; 
        this.readyState = 1;
        if (this.onreadystatechange) this.onreadystatechange();
    }
    send() {
        fs.readFile(this.url).then(data => {
            this.status = 200;
            this.readyState = 4;
            this.response = data.buffer;
            if (this.onreadystatechange) this.onreadystatechange();
            if (this.onload) this.onload();
        }).catch(err => {
            this.status = 404;
            this.readyState = 4;
            if (this.onreadystatechange) this.onreadystatechange();
            if (this.onerror) this.onerror(err);
        });
    }
    setResponseType(type) { this.responseType = type; }
    getResponse() { return this.response; }
    addEventListener(type, listener) {
        if (type === 'load') this.onload = listener;
        if (type === 'error') this.onerror = listener;
        if (type === 'readystatechange') this.onreadystatechange = listener;
    }
}

async function runTest(javaFilePath) {
    javaFilePath = path.resolve(rootDir, javaFilePath);
    const javaCode = await fs.readFile(javaFilePath, 'utf8');
    const mainClassMatch = javaCode.match(/public\s+class\s+([a-zA-Z_$0-9]+)/);
    const mainClass = mainClassMatch ? mainClassMatch[1] : 'Main';

    console.log(`[Testing Compilation of ${mainClass}]`);

    // 1. Setup Compiler Worker
    const workerScript = await fs.readFile(path.join(distDir, 'worker.js'), 'utf8');
    const compilerMessages = [];
    
    const selfMock = {
        postMessage: (msg) => {
            compilerMessages.push(msg);
            console.log('[Compiler -> Page]', msg.command, msg.status || '');
            if (msg.command === 'compiler-diagnostic' || msg.command === 'diagnostic') {
                console.log(`  ${msg.severity}: ${msg.message} (${msg.fileName}:${msg.lineNumber})`);
            }
        },
        addEventListener: (type, listener) => {
            if (type === 'message') compilerContext.onmessage = listener;
        }
    };
    
    global.self = selfMock;
    global.performance = performance;
    global.XMLHttpRequest = MockXHR;

    const compilerContext = vm.createContext({
        Error,
        Int8Array,
        console,
        performance,
        setTimeout,
        clearTimeout,
        self: selfMock,
        global: selfMock,
        globalThis: selfMock,
        XMLHttpRequest: MockXHR,
        process: { env: {} }
    });
    
    compilerContext.import = async (modulePath) => {
        const fullPath = path.join(distDir, modulePath);
        const module = await import(pathToFileURL(fullPath).href);
        if (modulePath.endsWith('compiler.wasm-runtime.js')) {
            return {
                ...module,
                load: async (wasm, options) => {
                    const oldInstall = options.installImports;
                    options.installImports = (o) => {
                        if (oldInstall) oldInstall(o);
                        o.teavmConsole = o.teavmConsole || {};
                        o.teavmConsole.putcharStdout = (ch) => process.stdout.write(String.fromCharCode(ch));
                        o.teavmConsole.putcharStderr = (ch) => process.stderr.write(String.fromCharCode(ch));
                    };
                    return module.load(wasm, options);
                }
            };
        }
        return module;
    };

    const compilerVm = new vm.Script(workerScript.replace(/import\(/g, 'import_('), { filename: 'worker.js' });
    compilerContext.import_ = compilerContext.import;
    compilerVm.runInContext(compilerContext);

    // Wait for initialized
    while (!compilerMessages.some(m => m.command === 'initialized')) await new Promise(r => setTimeout(r, 10));

    // 2. Load Classlibs
    console.log('[Page -> Compiler] load-classlib');
    compilerContext.onmessage({ data: {
        command: 'load-classlib',
        id: 'L1',
        url: 'compile-classlib-teavm.bin',
        runtimeUrl: 'runtime-classlib-teavm.bin'
    }});

    while (!compilerMessages.some(m => m.command === 'ok' && m.id === 'L1')) {
        const err = compilerMessages.find(m => m.command === 'error' && m.id === 'L1');
        if (err) throw new Error('Compiler failed to load classlibs: ' + err.text);
        await new Promise(r => setTimeout(r, 100));
    }

    // 3. Compile
    console.log('[Page -> Compiler] compile');
    compilerContext.onmessage({ data: {
        command: 'compile',
        id: 'C1',
        text: javaCode,
        mainClass: mainClass
    }});

    let compilationResult;
    while (!(compilationResult = compilerMessages.find(m => m.command === 'compilation-complete' && m.id === 'C1'))) {
        const err = compilerMessages.find(m => m.command === 'error' && m.id === 'C1');
        if (err) throw new Error('Compiler crashed: ' + (err.text || err.message));
        await new Promise(r => setTimeout(r, 100));
    }

    if (compilationResult.status !== 'successful') {
        console.error('Compilation failed with errors.');
        return;
    }

    console.log('Compilation successful!');

    // 4. Setup Execution Worker
    console.log('[Testing Execution]');
    const runnerScript = await fs.readFile(path.join(distDir, 'workerrun.js'), 'utf8');
    const runnerMessages = [];
    const runnerSelfMock = {
        postMessage: (msg) => {
            runnerMessages.push(msg);
            if (msg.command === 'stdout') process.stdout.write(msg.line);
            else if (msg.command === 'stderr') process.stderr.write(msg.line);
            else console.log('[Runner -> Page]', msg.command);
        },
        addEventListener: (type, listener) => {
            if (type === 'message') runnerContext.onmessage = listener;
        }
    };
    
    global.self = runnerSelfMock;

    const runnerContext = vm.createContext({
        Error,
        Int8Array,
        console,
        performance,
        setTimeout,
        clearTimeout,
        Object,
        Array,
        String,
        self: runnerSelfMock,
        global: runnerSelfMock,
        globalThis: runnerSelfMock,
        XMLHttpRequest: MockXHR,
        process: { env: {} }
    });
    runnerContext.import = compilerContext.import;

    const runnerVm = new vm.Script(runnerScript.replace(/import\(/g, 'import_('), { filename: 'workerrun.js' });
    runnerContext.import_ = runnerContext.import;
    runnerVm.runInContext(runnerContext);

    // 5. Run
    console.log('[Page -> Runner] run');
    runnerContext.onmessage({ data: {
        command: 'run',
        id: 'R1',
        code: compilationResult.script,
        args: [],
        messagePosting: true,
        keepAlive: false
    }});

    while (!runnerMessages.some(m => m.command === 'run-completed' && m.id === 'R1')) {
        await new Promise(r => setTimeout(r, 100));
    }

    console.log('Execution finished.');
}

const args = process.argv.slice(2);
if (args.length < 1) {
    console.log('Usage: node scripts/test-cli.mjs <JavaFile>');
    process.exit(1);
}

runTest(args[0]).catch(err => {
    console.error('Test script failed:');
    console.error(err);
    process.exit(1);
});

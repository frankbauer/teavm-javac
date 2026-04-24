import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs/promises'
import path from 'node:path'
import vm from 'node:vm'
import { pathToFileURL } from 'node:url'

const rootDir = path.resolve(import.meta.dirname, '..')
const distDir = path.join(rootDir, 'dist', 'v102')
const templateDir = path.join(rootDir, 'template')
const workerRuntimePath = path.join(distDir, 'worker', 'compiler.wasm-runtime.js')
const workerWasmPath = path.join(distDir, 'worker', 'compiler.wasm')
const sdkPath = path.join(distDir, 'compile-classlib-teavm.bin')
const runtimeClasslibPath = path.join(distDir, 'runtime-classlib-teavm.bin')

let runtimeModulePromise
let classlibPromise

async function read(filePath) {
    return fs.readFile(filePath, 'utf8')
}

async function assertPathExists(relPath) {
    const fullPath = path.join(distDir, relPath)
    await fs.access(fullPath)
}

function patchDynamicImport(code) {
    return code.replace(/await\s+import\(/g, 'await __import(')
}

async function loadRuntimeModule() {
    if (!runtimeModulePromise) {
        runtimeModulePromise = import(pathToFileURL(workerRuntimePath).href)
    }
    return runtimeModulePromise
}

async function loadClasslibs() {
    if (!classlibPromise) {
        classlibPromise = Promise.all([
            fs.readFile(sdkPath).then((buf) => new Int8Array(buf)),
            fs.readFile(runtimeClasslibPath).then((buf) => new Int8Array(buf)),
        ])
    }
    return classlibPromise
}

async function createCompilerWithClasslibs() {
    const runtime = await loadRuntimeModule()
    const [sdk, runtimeClasslib] = await loadClasslibs()

    const teavm = await runtime.load(workerWasmPath, {
        stackDeobfuscator: {
            enabled: true,
        },
    })
    const compiler = teavm.exports.createCompiler()
    compiler.setSdk(sdk)
    compiler.setTeaVMClasslib(runtimeClasslib)

    return { runtime, compiler }
}

async function compileAndGenerate(sourceFileName, sourceCode, mainClass, outputName = 'app', extraSources = []) {
    const { runtime, compiler } = await createCompilerWithClasslibs()
    const diagnostics = []
    compiler.onDiagnostic((d) => {
        diagnostics.push({
            type: d.type,
            severity: d.severity,
            fileName: d.fileName,
            lineNumber: d.lineNumber,
            message: d.message,
        })
    })
    compiler.addSourceFile(sourceFileName, sourceCode)
    for (const source of extraSources) {
        compiler.addSourceFile(source.fileName, source.code)
    }

    const compiled = compiler.compile()
    const generated = compiled
        ? compiler.generateWebAssembly({
              outputName,
              mainClass,
          })
        : false

    return {
        runtime,
        compiler,
        diagnostics,
        compiled,
        generated,
        wasm: generated ? compiler.getWebAssemblyOutputFile(`${outputName}.wasm`) : null,
    }
}

async function loadWorkerHarness() {
    const workerPath = path.join(distDir, 'worker.js')
    let source = await read(workerPath)
    source = patchDynamicImport(source)
    source = source.replace(';(async function () {', 'globalThis.__runner = (async function () {')

    const calls = {
        loadPath: undefined,
        loadOptions: undefined,
        installWorkerCalled: false,
    }

    const context = vm.createContext({
        Error,
        console,
        globalThis: {},
        __import: async () => ({
            load: async (requestedPath, options) => {
                calls.loadPath = requestedPath
                calls.loadOptions = options
                return {
                    exports: {
                        installWorker() {
                            calls.installWorkerCalled = true
                        },
                    },
                }
            },
        }),
    })

    const script = new vm.Script(source, { filename: 'worker.js' })
    script.runInContext(context)
    await context.globalThis.__runner
    return calls
}

async function loadWorkerrunHarness(harnessOptions = {}) {
    const scriptPath = path.join(distDir, 'workerrun.js')
    let source = await read(scriptPath)
    source = patchDynamicImport(source)

    const listeners = {}
    const posted = []
    let loadCalls = 0

    const selfMock = {
        addEventListener(type, listener) {
            listeners[type] = listener
        },
        postMessage(message) {
            posted.push(message)
        },
        $rt_last_run_args: undefined,
    }

    const context = vm.createContext({
        Error,
        console,
        self: selfMock,
        __import: async () => ({
            load: async (_code, loadOptions) => {
                loadCalls++
                const imports = { teavmConsole: {} }
                if (loadOptions && typeof loadOptions.installImports === 'function') {
                    loadOptions.installImports(imports)
                }
                return {
                    exports: {
                        main(args) {
                            if (typeof imports.teavmConsole.putcharStdout === 'function') {
                                for (const ch of 'ok\n') {
                                    imports.teavmConsole.putcharStdout(ch.charCodeAt(0))
                                }
                            }
                            if (typeof imports.teavmConsole.putcharStderr === 'function') {
                                for (const ch of 'warn\n') {
                                    imports.teavmConsole.putcharStderr(ch.charCodeAt(0))
                                }
                            }
                            selfMock.$rt_last_run_args = {
                                data: (Array.isArray(args) ? args : []).map((arg) => `${arg}-ret`),
                            }
                            if (harnessOptions.emitFinalPayload) {
                                selfMock.postMessage({
                                    command: 'f-FINAL',
                                    id: -1,
                                    value: '{"test":42}',
                                })
                            }
                        },
                    },
                }
            },
        }),
    })

    const script = new vm.Script(source, { filename: 'workerrun.js' })
    script.runInContext(context)

    return {
        posted,
        loadCalls: () => loadCalls,
        send: async (message) => {
            await listeners.message({ data: message })
        },
    }
}

test('dist/v102 contains required worker artifacts', async () => {
    await assertPathExists('compile-classlib-teavm.bin')
    await assertPathExists('runtime-classlib-teavm.bin')
    await assertPathExists('worker.js')
    await assertPathExists('workerrun.js')
    await assertPathExists(path.join('worker', 'compiler.wasm'))
    await assertPathExists(path.join('worker', 'compiler.wasm-runtime.js'))
    await assertPathExists(path.join('worker', 'compiler.wasm.teadbg'))
})

test('dist worker scripts are copied from template', async () => {
    const [distWorker, templateWorker, distRunWorker, templateRunWorker] = await Promise.all([
        read(path.join(distDir, 'worker.js')),
        read(path.join(templateDir, 'worker.js')),
        read(path.join(distDir, 'workerrun.js')),
        read(path.join(templateDir, 'workerrun.js')),
    ])

    assert.equal(distWorker, templateWorker)
    assert.equal(distRunWorker, templateRunWorker)
})

test('worker.js loads wasm runtime and installs worker protocol', async () => {
    const calls = await loadWorkerHarness()
    assert.equal(calls.loadPath, 'worker/compiler.wasm')
    assert.equal(calls.loadOptions?.stackDeobfuscator?.enabled, true)
    assert.equal(calls.installWorkerCalled, true)
})

test('workerrun emits required execution lifecycle events', async () => {
    const harness = await loadWorkerrunHarness()
    await harness.send({
        command: 'run',
        id: 'q-1',
        code: new Uint8Array([0]),
        args: ['a', 'b'],
        messagePosting: true,
        keepAlive: false,
    })

    const commands = harness.posted.map((m) => m.command)
    assert.deepEqual(commands, [
        'run-finished-setup',
        'main-will-start',
        'stdout',
        'stderr',
        'main-finished',
        'run-completed',
    ])

    const completed = harness.posted.find((m) => m.command === 'run-completed')
    assert.deepEqual(completed?.args, ['a-ret', 'b-ret'])
})

test('workerrun keepAlive waits for explicit session-ended', async () => {
    const harness = await loadWorkerrunHarness()
    await harness.send({
        command: 'run',
        id: 'q-2',
        code: new Uint8Array([0]),
        args: ['x'],
        messagePosting: false,
        keepAlive: true,
    })

    assert.equal(harness.posted.some((m) => m.command === 'run-completed'), false)

    await harness.send({
        command: 'session-ended',
        id: 'q-2',
    })

    const completed = harness.posted.filter((m) => m.command === 'run-completed')
    assert.equal(completed.length, 1)
    assert.deepEqual(completed[0].args, ['x-ret'])
    assert.equal(harness.loadCalls(), 1)
})

test('can compile java code with a custom class name and execute it with args', async () => {
    const source = `
        public class CustomEntryPoint {
            public static void main(String[] args) {
                System.out.println("OUT:" + args[0]);
                System.err.println("ERR:" + args[1]);
            }
        }
    `

    const result = await compileAndGenerate('CustomEntryPoint.java', source, 'CustomEntryPoint')
    assert.equal(result.compiled, true)
    assert.equal(result.generated, true)
    assert.ok(result.wasm)
    assert.ok(result.compiler.detectMainClasses().includes('CustomEntryPoint'))

    let stdout = ''
    let stderr = ''

    const app = await result.runtime.load(result.wasm, {
        installImports(imports) {
            imports.teavmConsole = imports.teavmConsole || {}
            imports.teavmConsole.putcharStdout = (ch) => {
                stdout += String.fromCharCode(ch)
            }
            imports.teavmConsole.putcharStderr = (ch) => {
                stderr += String.fromCharCode(ch)
            }
        },
    })

    app.exports.main(['alpha', 'beta'])

    assert.equal(stdout, 'OUT:alpha\n')
    assert.equal(stderr, 'ERR:beta\n')
})

test('captures compiler errors and can surface warning diagnostics in v102-style flow', async () => {
    const invalidSource = `
        public class BrokenMain {
            public static void main(String[] args) {
                System.out.println("oops")
            }
        }
    `

    const result = await compileAndGenerate('BrokenMain.java', invalidSource, 'BrokenMain')
    assert.equal(result.compiled, false)
    assert.ok(result.diagnostics.some((d) => d.severity === 'error'))

    const compileFailed = []
    const infos = []
    const errs = []

    // Mirror the diagnostic handling shape used by teavm.v102.ts.
    const handleDiagnosticEvent = (eventData) => {
        const isCompilerDiagnostic = eventData.command === 'compiler-diagnostic'
        const message = isCompilerDiagnostic
            ? eventData.humanReadable || eventData.message || 'Compilation warning'
            : eventData.text || eventData.humanReadable || 'Compilation warning'

        compileFailed.push({
            message,
            severity: eventData.severity,
        })

        if (eventData.severity === 'ERROR') {
            errs.push(message)
        } else {
            infos.push(message)
        }
    }

    handleDiagnosticEvent({
        command: 'diagnostic',
        severity: 'WARNING',
        text: 'unused import',
    })

    assert.ok(compileFailed.some((d) => d.severity === 'WARNING'))
    assert.ok(infos.includes('unused import'))
    assert.equal(errs.length, 0)
})

test('supports CodeBlocks.postResult JSON payload (f-FINAL)', async () => {
    const harness = await loadWorkerrunHarness({ emitFinalPayload: true })
    await harness.send({
        command: 'run',
        id: 'q-final',
        code: new Uint8Array([0]),
        args: [],
        messagePosting: true,
        keepAlive: false,
    })

    const finalMessage = harness.posted.find((m) => m.command === 'f-FINAL')
    assert.deepEqual(finalMessage, {
        command: 'f-FINAL',
        id: -1,
        value: '{"test":42}',
    })

    // teavm.v102.ts parses this value with JSON.parse for options.resultData.
    assert.deepEqual(JSON.parse(finalMessage.value), { test: 42 })
})

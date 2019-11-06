self.importScripts("worker/runtime.js");

var $stdoutBuffer = "";
function $rt_putStdoutCustom(ch) {
    if (ch === 0xA) {
        //window.postMessage(JSON.stringify({ command: "stdout", line: $stdoutBuffer }), "*");
        console.log($stdoutBuffer);
        $stdoutBuffer = "";
    } else {
        $stdoutBuffer += String.fromCharCode(ch);
    }
}

var $stderrBuffer = "";
function $rt_putStderrCustom(ch) {
    if (ch === 0xA) {
        //window.postMessage(JSON.stringify({ command: "stderr", line: $stderrBuffer }), "*");
        console.log($stderrBuffer);
        $stderrBuffer = "";
    } else {
        $stderrBuffer += String.fromCharCode(ch);
    }
}

self.importScripts("worker/classes.js");
main();
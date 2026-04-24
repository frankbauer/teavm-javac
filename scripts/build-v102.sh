#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

cd "$ROOT_DIR"

if [[ "$(uname -s)" == "Darwin" ]]; then
    JAVA_25_HOME="$(/usr/libexec/java_home -v 25 2>/dev/null || true)"
    if [[ -z "$JAVA_25_HOME" ]]; then
        echo "Unable to locate Java 25 via /usr/libexec/java_home -v 25"
        exit 1
    fi

    JAVA_VERSION_OUTPUT="$($JAVA_25_HOME/bin/java -version 2>&1 | head -n 1)"
    if [[ "$JAVA_VERSION_OUTPUT" != *'"25'* ]]; then
        echo "Java 25 requested, but resolved JAVA_HOME points to: $JAVA_VERSION_OUTPUT"
        echo "Resolved JAVA_HOME was: $JAVA_25_HOME"
        exit 1
    fi

    export JAVA_HOME="$JAVA_25_HOME"
    export PATH="$JAVA_HOME/bin:$PATH"
    echo "Using JAVA_HOME=$JAVA_HOME"
fi

echo "Building TeaVM compiler artifacts..."
GRADLE_JAVA_OPTS=()
if [[ -n "${JAVA_HOME:-}" ]]; then
    GRADLE_JAVA_OPTS+=("-Dorg.gradle.java.home=$JAVA_HOME")
fi

./gradlew "${GRADLE_JAVA_OPTS[@]}" \
    :compiler:buildWasmGC :compiler:generateClassLib :compiler:buildTeaVMClassLib

DIST_DIR="$ROOT_DIR/dist/v102"
WORKER_DIR="$DIST_DIR/worker"
WASM_OUT_DIR="$ROOT_DIR/compiler/build/generated/teavm/wasm-gc"
CLASSLIB_DIR="$ROOT_DIR/compiler/build/classlib"

mkdir -p "$WORKER_DIR"

cp "$CLASSLIB_DIR/compile-classlib-teavm.bin" "$DIST_DIR/compile-classlib-teavm.bin"
cp "$CLASSLIB_DIR/runtime-classlib-teavm.bin" "$DIST_DIR/runtime-classlib-teavm.bin"
cp "$WASM_OUT_DIR/compiler.wasm" "$WORKER_DIR/compiler.wasm"
cp "$WASM_OUT_DIR/compiler.wasm-runtime.js" "$WORKER_DIR/compiler.wasm-runtime.js"

if [[ -f "$WASM_OUT_DIR/compiler.wasm-deobfuscator.wasm" ]]; then
    cp "$WASM_OUT_DIR/compiler.wasm-deobfuscator.wasm" "$WORKER_DIR/compiler.wasm-deobfuscator.wasm"
fi

if [[ -f "$WASM_OUT_DIR/compiler.wasm.teadbg" ]]; then
    cp "$WASM_OUT_DIR/compiler.wasm.teadbg" "$WORKER_DIR/compiler.wasm.teadbg"
fi

if [[ -f "$WASM_OUT_DIR/compiler.wasm.map" ]]; then
    cp "$WASM_OUT_DIR/compiler.wasm.map" "$WORKER_DIR/compiler.wasm.map"
fi

cp "$ROOT_DIR/template/worker.js" "$DIST_DIR/worker.js"
cp "$ROOT_DIR/template/workerrun.js" "$DIST_DIR/workerrun.js"

echo "dist/v102 has been refreshed successfully."

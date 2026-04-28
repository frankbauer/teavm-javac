/*
 *  Copyright 2025 Alexey Andreev.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.teavm.javac;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.function.Consumer;
import org.teavm.javac.protocol.AstResultMessage;
import org.teavm.javac.protocol.CompilationResultMessage;
import org.teavm.javac.protocol.CompileMessage;
import org.teavm.javac.protocol.CompilerDiagnosticMessage;
import org.teavm.javac.protocol.ErrorMessage;
import org.teavm.javac.protocol.LoadStdlibMessage;
import org.teavm.javac.protocol.TeaVMDiagnosticMessage;
import org.teavm.javac.protocol.TeaVMPhaseMessage;
import org.teavm.javac.protocol.WorkerMessage;
import org.teavm.jso.ajax.XMLHttpRequest;
import org.teavm.jso.browser.Window;
import org.teavm.jso.core.JSArray;
import org.teavm.jso.core.JSObjects;
import org.teavm.jso.core.JSPromise;
import org.teavm.jso.core.JSString;
import org.teavm.jso.dom.events.MessageEvent;
import org.teavm.jso.typedarrays.ArrayBuffer;
import org.teavm.jso.typedarrays.Int8Array;
import org.teavm.vm.TeaVMPhase;

public final class Worker {
    private boolean isBusy;
    private String mainClass;
    private final Compiler compiler;

    Worker(Compiler compiler) {
        this.compiler = compiler;
        Window.worker().addEventListener("message", (MessageEvent event) -> {
            handleEvent(event);
        });
        WorkerMessage response = JSObjects.createWithoutProto();
        response.setCommand("initialized");
        Window.worker().postMessage(response);
    }

    private void handleEvent(MessageEvent event) {
        var request = (WorkerMessage) event.getData();
        try {
            processResponse(request);
        } catch (Throwable e) {
            log("Error occurred");
            e.printStackTrace();
            Window.worker().postMessage(createErrorResponse(request, "Error occurred processing message: "
                    + e.getMessage()));
        }
    }

    private long initializationStartTime;

    private void processResponse(WorkerMessage request) throws Exception {
        log("Message received: " + request.getId());

        if (isBusy) {
            log("Responded busy status");
            Window.worker().postMessage(createErrorResponse(request, "Busy"));
            return;
        }

        isBusy = true;
        switch (request.getCommand()) {
            case "load-classlib":
                var loadLibReq = (LoadStdlibMessage) request;
                init(request, loadLibReq.getUrl(), loadLibReq.getRuntimeUrl(), success -> {
                    if (success) {
                        respondOk(request);
                    }
                    isBusy = false;
                });
                break;
            case "compile":
                compileAll((CompileMessage) request);
                log("Done processing message: " + request.getId());
                isBusy = false;
                break;
        }
    }

    private void compileAll(CompileMessage request) throws IOException {
        compiler.clearSourceFiles();
        compiler.clearOutputFiles();

        // Extract mainClass from request if provided, otherwise will be auto-detected
        String requestedMainClass = null;
        if (!JSObjects.isUndefined(request.getMainClass()) && request.getMainClass() != null) {
            requestedMainClass = request.getMainClass();
        }
        
        // Derive source file name from main class (e.g., "com.example.MyClass" -> "MyClass.java")
        String sourceFileName;
        if (requestedMainClass != null) {
            String className = requestedMainClass.contains(".") 
                ? requestedMainClass.substring(requestedMainClass.lastIndexOf('.') + 1)
                : requestedMainClass;
            sourceFileName = className + ".java";
        } else {
            sourceFileName = "Main.java";  // Default fallback
        }
        
        createSourceFile(request.getText(), sourceFileName);

        if (request.isEmitAst()) {
            var astJson = compiler.parseToAst();
            AstResultMessage astMsg = JSObjects.createWithoutProto();
            astMsg.setCommand("ast");
            astMsg.setId(request.getId());
            astMsg.setAst(astJson);
            Window.worker().postMessage(astMsg);
        }

        CompilationResultMessage response = JSObjects.createWithoutProto();
        response.setId(request.getId());
        response.setCommand("compilation-complete");

        if (doCompile(request) && detectMainClass(request, requestedMainClass) && generateWebAssembly(request.getId())) {
            response.setStatus("successful");
            response.setScript(readResultingFile());
        } else {
            response.setStatus("errors");
        }

        Window.worker().postMessage(response);
    }

    private void respondOk(WorkerMessage message) {
        WorkerMessage response = JSObjects.createWithoutProto();
        response.setCommand("ok");
        response.setId(message.getId());
        Window.worker().postMessage(response);
    }

    private <T extends ErrorMessage> T createErrorResponse(WorkerMessage request, String text) {
        T message = JSObjects.createWithoutProto();
        message.setId(request.getId());
        message.setCommand("error");
        message.setText(text);
        return message;
    }

    private void init(WorkerMessage request, String url, String runtimeUrl, Consumer<Boolean> next) {
        log("Initializing");

        initializationStartTime = System.currentTimeMillis();
        loadTeaVMClasslib(request, url, runtimeUrl, success -> {
            long end = System.currentTimeMillis();
            log("Initialized in " + (end - initializationStartTime) + " ms");
            next.accept(success);
        });
    }

    private boolean doCompile(WorkerMessage request) {
        var requestId = request.getId();
        var reg = compiler.onDiagnostic(diagnostic -> handleDiagnostic((JavaDiagnostic) diagnostic, requestId));
        var result = compiler.compile();
        reg.destroy();
        return result;
    }

    private void handleDiagnostic(JavaDiagnostic diagnostic, String requestId) {
        CompilerDiagnosticMessage response = JSObjects.createWithoutProto();
        response.setCommand("compiler-diagnostic");
        response.setId(requestId);

        response.setSeverity(diagnostic.getSeverity());
        response.setFileName(diagnostic.getFileName());

        response.setStartPosition(diagnostic.getStartPosition());
        response.setPosition(diagnostic.getPosition());
        response.setEndPosition( diagnostic.getEndPosition());

        response.setLineNumber(diagnostic.getLineNumber());
        response.setColumnNumber(diagnostic.getColumnNumber());

        response.setMessage(diagnostic.getMessage());
        response.setHumanReadable(buildDiagnosticString(response));

        Window.worker().postMessage(response);
    }

    private void handleTeaVMDiagnostic(TeaVMDiagnostic diagnostic, String requestId) {
        CompilerDiagnosticMessage response = JSObjects.createWithoutProto();
        response.setCommand("diagnostic");
        response.setId(requestId);

        response.setSeverity(diagnostic.getSeverity());
        response.setFileName(diagnostic.getFileName());

        response.setLineNumber(diagnostic.getLineNumber());
        response.setColumnNumber(0);

        response.setMessage(diagnostic.getMessage());
        response.setHumanReadable(buildDiagnosticString(response));

        Window.worker().postMessage(response);
    }

    private static String buildDiagnosticString(CompilerDiagnosticMessage request) {
        StringBuilder sb = new StringBuilder();
        switch (request.getSeverity()) {
            case "ERROR":
                sb.append("ERROR ");
                break;
            case "WARNING":
            case "MANDATORY_WARNING":
                sb.append("WARNING ");
                break;
            default:
                break;
        }

        if (request.getFileName() != null && !request.getFileName().isEmpty()) {
            sb.append("at ").append(request.getFileName());
            if (request.getLineNumber() >= 0) {
                sb.append("(").append(request.getLineNumber());
                if (request.getColumnNumber() >= 0) {
                    sb.append(":").append(request.getColumnNumber());
                }
                sb.append(")");
            }
            sb.append(' ');
        }

        if (request.getMessage() != null) {
            sb.append(request.getMessage());
        }
        return sb.toString();
    }

    private long lastPhaseTime = System.currentTimeMillis();
    private TeaVMPhase lastPhase;

    private static final String MAIN_OVERRIDE_CLASS = "MainOverride";

    private boolean detectMainClass(WorkerMessage request, String requestedMainClass) throws IOException {
        var allCandidates = compiler.detectMainClasses();

        // Separate MainOverride class from regular main-class candidates
        String overrideClass = null;
        var regularCandidates = new ArrayList<String>();
        for (var candidate : allCandidates) {
            var simpleName = candidate.contains("/")
                    ? candidate.substring(candidate.lastIndexOf('/') + 1)
                    : candidate;
            if (MAIN_OVERRIDE_CLASS.equals(simpleName)) {
                overrideClass = candidate;
            } else {
                regularCandidates.add(candidate);
            }
        }

        // MainOverride takes priority over everything
        if (overrideClass != null) {
            mainClass = overrideClass.replace('/', '.');
            return true;
        }

        // Use the explicitly-requested main class if provided and it exists among candidates
        if (requestedMainClass != null) {
            var normalizedRequestedMainClass = requestedMainClass.replace('/', '.');
            for (var candidate : allCandidates) {
                var candidateFull = candidate.replace('/', '.');
                var candidateSimple = candidateFull.contains(".")
                        ? candidateFull.substring(candidateFull.lastIndexOf('.') + 1)
                        : candidateFull;
                if (candidateFull.equals(normalizedRequestedMainClass) || candidateSimple.equals(normalizedRequestedMainClass)) {
                    mainClass = candidateFull;
                    return true;
                }
            }
        }

        // Auto-detect from compiled code
        if (regularCandidates.size() != 1) {
            var text = regularCandidates.isEmpty() ? "Main method not found" : "Multiple main methods found";
            TeaVMDiagnosticMessage message = JSObjects.createWithoutProto();
            message.setId(request.getId());
            message.setCommand("diagnostic");
            message.setSeverity("ERROR");
            message.setText(text);
            message.setFileName(null);
            Window.worker().postMessage(message);
            return false;
        }

        mainClass = regularCandidates.get(0).replace('/', '.');
        return true;
    }

    private boolean generateWebAssembly(String requestId) {
        var options = new WebAssemblyCompilationOptions() {
            @Override
            public JSString getOutputName() {
                return JSString.valueOf("app");
            }

            @Override
            public JSString getMainClass() {
                return JSString.valueOf(mainClass);
            }
        };
        var reg = compiler.onDiagnostic(diagnostic -> handleTeaVMDiagnostic((TeaVMDiagnostic) diagnostic, requestId));
        var result = compiler.generateWebAssembly(options);
        reg.destroy();
        return result;
    }

    private void reportPhase(WorkerMessage request, TeaVMPhase phase) {
        TeaVMPhaseMessage phaseMessage = JSObjects.createWithoutProto();
        phaseMessage.setId(request.getId());
        phaseMessage.setCommand("phase");
        phaseMessage.setPhase(phase.name());
        Window.worker().postMessage(phaseMessage);
    }

    private void loadTeaVMClasslib(WorkerMessage request, String url, String runtimeUrl, Consumer<Boolean> next) {
        File baseDir = new File("/teavm-stdlib");
        baseDir.mkdirs();

        JSPromise.all(JSArray.of(downloadFile(url), downloadFile(runtimeUrl)))
                .then(arr -> {
                    boolean success;
                    var file = arr.get(0);
                    var runtimeFile = arr.get(1);
                    try {
                        compiler.setSdk(file);
                        compiler.setTeaVMClasslib(runtimeFile);
                        success = true;
                    } catch (IOException e) {
                        success = false;
                        Window.worker().postMessage(createErrorResponse(request, "Error occurred downloading classlib: "
                                + e.getMessage()));
                    }
                    next.accept(success);
                    return null;
                });
    }


    private static JSPromise<Int8Array> downloadFile(String url) {
        return new JSPromise<>((resolve, reject) -> {
            var xhr = new XMLHttpRequest();
            xhr.open("GET", url, true);
            xhr.setResponseType("arraybuffer");
            xhr.onComplete(() -> {
                var buffer = (ArrayBuffer) xhr.getResponse();
                resolve.accept(new Int8Array(buffer));
            });
            xhr.onError(ignore -> reject.accept(new RuntimeException("Error downloading file: " + url)));
            xhr.send();
        });
    }

    private void createSourceFile(String content, String sourceFileName) {
        compiler.addSourceFile(sourceFileName, content);
    }

    private static void log(String message) {
        System.out.println(message);
    }

    private Int8Array readResultingFile() {
        return compiler.getWebAssemblyOutputFile("app.wasm");
    }
}

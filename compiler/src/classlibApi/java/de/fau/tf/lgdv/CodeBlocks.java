/*
 *  Copyright 2021 frank bauer.
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
package de.fau.tf.lgdv;

import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.events.MessageEvent;
import de.fau.tf.lgdv.json.*;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import de.fau.tf.lgdv.runtime.RemoteObject;
import de.fau.tf.lgdv.runtime.NewRemoteObjectMessage;
import de.fau.tf.lgdv.runtime.ObjectReplyMessage;

public class CodeBlocks {
    @JSBody(script = "return {  };")
    public static native <T extends JSObject> T createJSObject();

    /**
     * This method is patched during link-time to return the actual session ID.
     */
    public static String getSessionId() {
        return "";
    }

    public static void exit(int code){
        CodeBlocksStringMessage msg = createJSObject();
        msg.setCommand("f-EXIT");
        msg.setSessionId(getSessionId());
        Window.worker().postMessage(msg);
        if (listener != null) {
            stopReceivingEvents();
        }
        System.exit(code);
    }

    public static void postResult(de.fau.tf.lgdv.json.JsonSerializer jsonObject){
        postResult(jsonObject.toJson());
    }

    public static void postResult(String jsonObject){
        CodeBlocksStringMessage msg = createJSObject();
        msg.setCommand("f-FINAL");
        msg.setSessionId(getSessionId());
        msg.setValue(jsonObject);
        Window.worker().postMessage(msg);
    }

    @JSBody(params = "obj", script = "return JSON.stringify(obj);")
    public static native String stringify(JSObject obj);

    private static Map<String, Object> eventHandlers = new HashMap<>();
    private static Map<Integer, AsyncCallback<JsonElement>> pendingSyncCalls = new HashMap<>();
    private static Map<String, AsyncCallback<JsonElement>> pendingEventCalls = new HashMap<>();
    private static int nextQueryId = 1000000;

    public static int generateQueryId() {
        return nextQueryId++;
    }

    @Async
    public static native JsonElement waitForQueryReply(int queryId);

    private static void waitForQueryReply(final int queryId, final AsyncCallback<JsonElement> callback) {
        // System.out.println("Registration: waitForQueryReply for queryId " + queryId + " with callback " + callback);
        startReceivingEvents(null);
        pendingSyncCalls.put(queryId, callback);
    }

    @Async
    public static native JsonElement waitForEvent(String key);

    private static void waitForEvent(final String key, final AsyncCallback<JsonElement> callback) {
        // System.out.println("Registration: waitForEvent for key " + key + " with callback " + callback);
        startReceivingEvents(null);
        pendingEventCalls.put(key, callback);
    }

    public static JsonElement sendQuery(String cmd, JsonSerializer json) {
        int qId = generateQueryId();
        postQuery(cmd, qId, json);
        return waitForQueryReply(qId);
    }

    public static void postMessage(NewRemoteObjectMessage message, RemoteObject handler){
        eventHandlers.put(String.valueOf(handler.ID), handler);
        CodeBlocks.postMessage(message);
    }

    public static void postMessage(CodeBlocksBaseMessage message){
        if (!message.getCommand().startsWith("w-")){
            message.setCommand("w-"+message.getCommand());
        }
        message.setSessionId(getSessionId());
        Window.worker().postMessage(message);
    }

    @JSBody(params = "obj", script = "var q = obj.queryId; return (typeof q === 'number') ? q : (typeof q === 'string' ? parseInt(q) : -1);")
    private static native int getQueryId(JSObject obj);

    @JSBody(params = "obj", script = "return (typeof obj.json === 'string') ? obj.json : \"{}\";")
    private static native String getJSONString(JSObject obj);

    public static JsonElement parseMessageJSON(CodeBlocksBaseMessage msg) {
        // System.out.println("Parsing JSON from message: " + msg + ", jsonStr=" + getJSONString(msg));
        return JsonParser.parse(getJSONString(msg));
    }

    private static EventListener listener;
    private static List<CodeBlocksEventFunction> eventFunctions = new ArrayList<>();
    public static void startReceivingEvents(CodeBlocksEventFunction handler){
        if (handler != null && !eventFunctions.contains(handler)) eventFunctions.add(handler);
        if (listener != null) return;
        
        listener = (EventListener<MessageEvent>) (MessageEvent event) -> {
            CodeBlocksBaseMessage request = (CodeBlocksBaseMessage) event.getData();
            if (request != null) {
                String cmd = request.getCommand();
                if (cmd != null && cmd.startsWith("d-")) {
                    cmd = cmd.substring(2);
                    request.setCommand(cmd);

                    int qId = getQueryId(request);
                    //System.out.println("Processing message: cmd=" + cmd + ", queryId=" + qId);

                    boolean completed = false;
                    // Match by queryId (for direct sendQuery and blocking sendNew)
                    if (qId > -1) {
                        AsyncCallback<JsonElement> syncCallback = pendingSyncCalls.remove(qId);
                        if (syncCallback != null) {
                            String jsonStr = getJSONString(request);
                            JsonElement element = JsonParser.parse(jsonStr);
                            // System.out.println("Completing syncCallback for queryId "+qId+": "+jsonStr+", " + element +"," +syncCallback);
                            syncCallback.complete(element);
                            completed = true;                        
                        }
                    }

                    if (cmd.equals("o")) {
                        ObjectReplyMessage orm = (ObjectReplyMessage) request;
                        
                        // Match by event key (e.g. "o:1:ready") for named event waits
                        String eventKey = "o:" + orm.getObjId() + ":" + orm.getCmd();
                        AsyncCallback<JsonElement> eventCallback = pendingEventCalls.remove(eventKey);
                        if (eventCallback != null) {
                            String jsonStr = getJSONString(request);
                            JsonElement element = JsonParser.parse(jsonStr);
                            // System.out.println("Completing eventCallback for key "+eventKey+": "+jsonStr +", " + element);
                            eventCallback.complete(element);
                        }

                        int id = orm.getObjId();
                        RemoteObject rObj = (RemoteObject) eventHandlers.get(String.valueOf(id));
                        
                        if (rObj != null ) {
                            if (rObj.TYPE.equals(orm.getType())) {
                                JsonElement json = orm.getJSON();
                                rObj.handleEvent(orm.getCmd(), json);
                            }
                        }
                    } else if (!completed) {
                        // Match by command-based event key
                        String eventKey = cmd + ":" + qId;
                        AsyncCallback<JsonElement> eventCallback = pendingEventCalls.remove(eventKey);
                        if (eventCallback != null) {
                            String jsonStr = getJSONString(request);
                            JsonElement element = JsonParser.parse(jsonStr);
                            // System.out.println("Completing eventCallback for key "+eventKey+": "+jsonStr +", " + element);
                            eventCallback.complete(element);
                        }

                        for (CodeBlocksEventFunction f : eventFunctions) {
                            f.handleEvent(request);
                        }
                    }
                } else {
                    // System.out.println("Skipping message (no 'd-' prefix): cmd=" + cmd);
                }
            }
        };
        Window.worker().addEventListener("message", listener);
    }

    public static void stopReceivingEvents(){
        if (listener!=null) {
            Window.worker().removeEventListener("message", listener);
            listener = null;
        }
        eventFunctions.clear();
    }

    public static void postMessage(String cmd){
        postMessage(createMessage(cmd));
    }

    public static CodeBlocksBaseMessage createMessage(String cmd){
        CodeBlocksBaseMessage msg = createJSObject();
        msg.setCommand("w-"+cmd);
        return msg;
    }

    public static void postMessage(String cmd, int value){
        postMessage(createMessage(cmd, value));
    }

    public static CodeBlocksIntMessage createMessage(String cmd, int value){
        CodeBlocksIntMessage msg = createJSObject();
        msg.setCommand("w-"+cmd);
        msg.setValue(value);
        return msg;
    }

    public static void postMessage(String cmd, int[] value){
        postMessage(createMessage(cmd, value));
    }

    public static CodeBlocksIntArrayMessage createMessage(String cmd, int[] value){
        CodeBlocksIntArrayMessage msg = createJSObject();
        msg.setCommand("w-"+cmd);
        msg.setValue(value);
        return msg;
    }

    public static void postMessage(String cmd, double value){
        postMessage(createMessage(cmd, value));
    }

    public static CodeBlocksDoubleMessage createMessage(String cmd, double value){
        CodeBlocksDoubleMessage msg = createJSObject();
        msg.setCommand("w-"+cmd);
        msg.setValue(value);
        return msg;
    }

    public static void postMessage(String cmd, double[] value){
        postMessage(createMessage(cmd, value));
    }

    public static CodeBlocksDoubleArrayMessage createMessage(String cmd, double[] value){
        CodeBlocksDoubleArrayMessage msg = createJSObject();
        msg.setCommand("w-"+cmd);
        msg.setValue(value);
        return msg;
    }

    public static void postMessage(String cmd, String value){
        postMessage(createMessage(cmd, value));
    }

    public static CodeBlocksStringMessage createMessage(String cmd, String value){
        CodeBlocksStringMessage msg = createJSObject();
        msg.setCommand("w-"+cmd);
        msg.setValue(value);
        return msg;
    }

    public static void postMessage(String cmd, String[] value){
        postMessage(createMessage(cmd, value));
    }

    public static CodeBlocksStringArrayMessage createMessage(String cmd, String[] value){
        CodeBlocksStringArrayMessage msg = createJSObject();
        msg.setCommand("w-"+cmd);
        msg.setValue(value);
        return msg;
    }

    public static void postMessage(String cmd, JsonSerializer value){
        postMessage(createMessage(cmd, value.toJson()));
    }

    // Query-specific overloads for sendQuery support
    @JSBody(params = {"msg", "queryId"}, script = "msg.queryId = queryId;")
    private static native void setQueryId(JSObject msg, int queryId);

    public static void postQuery(String cmd, int queryId, JsonSerializer value){
        CodeBlocksQueryMessage msg = createJSObject();
        msg.setCommand("w-"+cmd);
        setQueryId(msg, queryId);
        msg.setJSON(value);
        postMessage(msg);
    }
}

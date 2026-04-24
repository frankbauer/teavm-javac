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

import de.fau.tf.lgdv.math.Vec2D;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.events.MessageEvent;

public class CodeBlocks {
    @JSBody(script = "return {  };")
    public static native <T extends JSObject> T createJSObject();

    public static void postResult(String jsonObject){
        CodeBlocksStringMessage msg = createJSObject();
        msg.setCommand("f-FINAL");
        msg.setId(-1);
        msg.setValue(jsonObject);
        Window.worker().postMessage(msg);
    }

    public static void postMessage(CodeBlocksBaseMessage message){
        if (!message.getCommand().startsWith("w-")){
            message.setCommand("w-"+message.getCommand());
        }
        Window.worker().postMessage(message);
    }

    private static EventListener<?> listener;
    public static void startReceivingEvents(CodeBlocksEventFunction handler){
        if (listener != null) {
            stopReceivingEvents();
        }
        listener = (MessageEvent event) -> {
            CodeBlocksBaseMessage request = (CodeBlocksBaseMessage) event.getData();
            String cmd = request.getCommand();
            if (cmd.startsWith("d-")) {
                cmd = cmd.substring(2);
                request.setCommand(cmd);
                handler.handleEvent(request);
            }
        };
        Window.worker().addEventListener("message", listener);
    }

    public static void stopReceivingEvents(){
        if (listener!=null) {
            Window.worker().removeEventListener("message", listener);
            listener = null;
        }
    }

    public static void postMessage(String cmd, int id){
        postMessage(createMessage(cmd, id));
    }

    public static CodeBlocksBaseMessage createMessage(String cmd, int id){
        CodeBlocksBaseMessage msg = createJSObject();
        msg.setCommand("w-"+cmd);
        msg.setId(id);
        return msg;
    }

    public static void postMessage(String cmd, int id, int value){
        postMessage(createMessage(cmd, id, value));
    }

    public static CodeBlocksIntMessage createMessage(String cmd, int id, int value){
        CodeBlocksIntMessage msg = createJSObject();
        msg.setCommand("w-"+cmd);
        msg.setId(id);
        msg.setValue(value);
        return msg;
    }

    public static void postMessage(String cmd, int id, int[] value){
        postMessage(createMessage(cmd, id, value));
    }

    public static CodeBlocksIntArrayMessage createMessage(String cmd, int id, int[] value){
        CodeBlocksIntArrayMessage msg = createJSObject();
        msg.setCommand("w-"+cmd);
        msg.setId(id);
        msg.setValue(value);
        return msg;
    }

    public static void postMessage(String cmd, int id, double value){
        postMessage(createMessage(cmd, id, value));
    }

    public static CodeBlocksDoubleMessage createMessage(String cmd, int id, double value){
        CodeBlocksDoubleMessage msg = createJSObject();
        msg.setCommand("w-"+cmd);
        msg.setId(id);
        msg.setValue(value);
        return msg;
    }

    public static void postMessage(String cmd, int id, double[] value){
        postMessage(createMessage(cmd, id, value));
    }

    public static CodeBlocksDoubleArrayMessage createMessage(String cmd, int id, double[] value){
        CodeBlocksDoubleArrayMessage msg = createJSObject();
        msg.setCommand("w-"+cmd);
        msg.setId(id);
        msg.setValue(value);
        return msg;
    }

    public static void postMessage(String cmd, int id, String value){
        postMessage(createMessage(cmd, id, value));
    }

    public static CodeBlocksStringMessage createMessage(String cmd, int id, String value){
        CodeBlocksStringMessage msg = createJSObject();
        msg.setCommand("w-"+cmd);
        msg.setId(id);
        msg.setValue(value);
        return msg;
    }

    public static void postMessage(String cmd, int id, String[] value){
        postMessage(createMessage(cmd, id, value));
    }

    public static CodeBlocksStringArrayMessage createMessage(String cmd, int id, String[] value){
        CodeBlocksStringArrayMessage msg = createJSObject();
        msg.setCommand("w-"+cmd);
        msg.setId(id);
        msg.setValue(value);
        return msg;
    }

    public static void postMessage(String cmd, int id, Vec2D value){
        postMessage(createMessage(cmd, id, value.toJSON()));
    }
}

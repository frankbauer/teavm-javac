/*
 *  Copyright 2026 frank bauer.
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
package de.fau.tf.lgdv.runtime;

import de.fau.tf.lgdv.CodeBlocks;
import de.fau.tf.lgdv.CodeBlocksBaseMessage;
import de.fau.tf.lgdv.json.*;
import org.teavm.jso.JSProperty;



public abstract class RemoteObject implements JsonObjectable { 
    public interface NewRemoteObjectMessage extends CodeBlocksBaseMessage {
        @JSProperty("json")
        String _getJSON();

        default JsonElement getJSON() {
            String s = _getJSON();
            return s != null ? JsonParser.parse(s) : null;
        }

        @JSProperty("json")
        void _setJSON(String value);    

        default void setJSON(JsonObjectable json) {
            String s = json != null ? json.toJson() : null;
            _setJSON(s);
        }    
    }   
    
    public interface ObjectReplyMessage extends NewRemoteObjectMessage {
        @JSProperty("objid")
        int getId();

        @JSProperty("objid")
        void setId(int value);

        @JSProperty("type")
        String _getType();

        default String getType() {
            String s = _getType();
            return s != null ? new String(s) : null;
        }

        @JSProperty("type")
        void setType(String value);

        @JSProperty("cmd")
        String _getCmd();

        default String getCmd() {
            String s = _getCmd();
            return s != null ? new String(s) : null;
        }

        @JSProperty("cmd")
        void setCmd(String value);
    }  

    private static int NEXT_OBJECT_ID = 1;
    public final String TYPE;
    public final int ID;

   
    protected RemoteObject(String typeString) {
        this.TYPE = typeString;
        this.ID = NEXT_OBJECT_ID++;                
    }

    protected void sendNew() {
        NewRemoteObjectMessage message = CodeBlocks.createJSObject();
        message.setCommand("n");
        message.setJSON(this.toJsonObject());
        
        CodeBlocks.postMessage(message, this);        
    }

    protected void sendCommand(String cmd, JsonObjectable json){
        ObjectReplyMessage message = CodeBlocks.createJSObject();
        message.setCommand("o");
        message.setCmd(cmd);
        message.setId(ID);
        message.setType(TYPE);
        message.setJSON(json);
        
        CodeBlocks.postMessage(message);        
    }

    protected abstract void addAttributes(JsonObject json);  

    public void handleEvent(String cmd, JsonElement json){
        // Default implementation - can be overridden by subclasses
    }  

    public JsonObject toJsonReference(){
        return new JsonObject().put("type", TYPE).put("id", ID);        
    }

    @Override
    public JsonObject toJsonObject(){
        JsonObject obj = this.toJsonReference();
        this.addAttributes(obj);
        return obj;
    }
}
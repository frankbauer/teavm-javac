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
import de.fau.tf.lgdv.NewRemoteObjectMessage;
import de.fau.tf.lgdv.ObjectReplyMessage;
import de.fau.tf.lgdv.json.*;

public abstract class RemoteObject { 
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

    protected void sendCommand(String cmd, JsonSerializer json){
        ObjectReplyMessage message = CodeBlocks.createJSObject();
        message.setCommand("o");
        message.setCmd(cmd);
        message.setObjId(ID);
        message.setType(TYPE);
        message.setJSON(json);
        
        CodeBlocks.postMessage(message);        
    }

    protected abstract void addAttributes(JsonObject json);  

    public void handleEvent(String cmd, Object json){
        // Default implementation - can be overridden by subclasses
    }  

    public JsonObject toJsonReference(){
        return new JsonObject().put("type", TYPE).put("id", ID);        
    }

    public JsonObject toJsonObject(){
        JsonObject obj = this.toJsonReference();
        this.addAttributes(obj);
        return obj;
    }
}

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

public abstract class RemoteObject { 
    private static int NEXT_OBJECT_ID = 1;
    public final String TYPE;
    public final int ID;
    protected int creationQueryId ;

    protected RemoteObject(String typeString) {
        this.creationQueryId = CodeBlocks.generateQueryId();
        this.TYPE = typeString;
        this.ID = NEXT_OBJECT_ID++;                
    }

    protected void sendNew() {
        if (creationQueryId<0) {
            throw new IllegalStateException("Object already created");
        }

        NewRemoteObjectMessage message = CodeBlocks.createJSObject();
        message.setCommand("n");
        message.setObjId(ID);
        message.setQueryId(creationQueryId);
        message.setJSON(this.toJsonObject());
        
        CodeBlocks.postMessage(message, this);        
    }

    protected JsonElement waitForCreated() {
        if (creationQueryId > -1) {
            //System.out.println("Waiting for object "+TYPE+"#"+ID+" to be ready... (queryId="+creationQueryId+")");
            JsonElement res = CodeBlocks.waitForQueryReply(creationQueryId);
            creationQueryId = -1;
            //System.out.println("Object "+TYPE+"#"+ID+" is ready. Received: "+res);
            return res;
        }
        return null;
    }

    protected void sendCommand(String cmd, JsonSerializer json) {
        ObjectReplyMessage message = CodeBlocks.createJSObject();
        message.setCommand("o");
        message.setCmd(cmd);
        message.setObjId(ID);
        message.setType(TYPE);
        message.setJSON(json);
        CodeBlocks.postMessage(message);
    }

    protected JsonElement sendQuery(String cmd, JsonSerializer json) {
        final int queryId = CodeBlocks.generateQueryId();
        ObjectReplyMessage message = CodeBlocks.createJSObject();
        message.setCommand("o");
        message.setCmd(cmd);
        message.setObjId(ID);
        message.setQueryId(queryId);
        message.setType(TYPE);
        message.setJSON(json);
        CodeBlocks.postMessage(message);
        return CodeBlocks.waitForQueryReply(queryId);
    }

    protected abstract void addAttributes(JsonObject json);

    public void handleEvent(String cmd, JsonElement json) {
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

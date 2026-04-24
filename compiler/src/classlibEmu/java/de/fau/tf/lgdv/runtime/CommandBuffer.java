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

import de.fau.tf.lgdv.json.*;

public class CommandBuffer {
    private boolean didSubmit = false;
    protected final java.util.List<String>  commands = new java.util.LinkedList<>();

    public static JsonObject of(RemoteObject obj){
        return obj.toJsonReference();
    }

    public void addEmptyCommand(String command){
        this.addCommand(new JsonObject().put("command", command));
    }

    public void addCommand(String command, int value){
        this.addCommand(new JsonObject()
            .put("command", command)
            .put("value", value)
        );
    }

    public void addCommand(String command, boolean value){
        this.addCommand(new JsonObject()
            .put("command", command)
            .put("value", value)
        );
    }

    public void addCommand(String command, RemoteObject obj){
        this.addCommand(command, obj, new JsonObject());
    }

    public void addCommand(String command, RemoteObject obj, JsonObject data){
        this.addCommand(data
            .put("command", command)
            .put("object", of(obj))
        );
    }

    public void addCommand(String command, JsonObject data){
        this.addCommand(data
            .put("command", command)            
        );
    }

    public void addCommand(String command, RemoteObject obj, int value){
        this.addCommand(new JsonObject()
            .put("command", command)
            .put("object", of(obj))
            .put("value", value)
        );
    }

    public void addNewObject(RemoteObject obj){
        this.addCommand(new JsonObject()
            .put("command", "new")
            .put("object",obj.toJsonObject())
        );
    }
    
    public void addCommand(String commandJson){
        commands.add(commandJson);
    }

    public void addCommand(JsonSerializer command){
        addCommand(command.toJson());
    }

    public void sendCommands(String errorOnSecond){
        if (!didSubmit) {
            sendCommands();
        } else {
            System.err.println(errorOnSecond);                                
        }
    }
    
    public void sendCommands(){
        didSubmit = true;
        // System.out.println("Sending commands: [" );
        // for (String command : commands){
        //     System.out.println(command+",");
        // }
        // System.out.println("]");
        de.fau.tf.lgdv.CodeBlocks.postResult(commands.toString());        
    }
}

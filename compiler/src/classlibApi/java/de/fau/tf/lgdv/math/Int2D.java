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
package de.fau.tf.lgdv.math;

import de.fau.tf.lgdv.json.JsonElement;
import de.fau.tf.lgdv.json.JsonObject;
import de.fau.tf.lgdv.json.JsonObjectable;

/**
 * Store an integer position
 * 
 * @author frank
 *
 */
public class Int2D implements JsonObjectable{
    public final int x;
    public final int y;

    public Int2D(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public JsonElement toJsonElement() {
        return new JsonObject().put("x", this.x).put("y", this.y).toJsonElement();
    }
}

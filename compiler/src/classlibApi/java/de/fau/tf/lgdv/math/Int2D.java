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

import de.fau.tf.lgdv.json.JsonArray;
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

    /**
     * Creates a new Int2D from a JsonObject.
     * Expected keys: "x" (default 0), "y" (default 0).
     * 
     * @param o The JsonObject to deserialize from.
     */
    public Int2D(JsonObject o) {
        this.x = o != null ? o.getInt("x", 0) : 0;
        this.y = o != null ? o.getInt("y", 0) : 0;
    }

    /**
     * Creates a new Int2D from a JsonArray.
     * Expected order: [x, y].
     * 
     * @param a The JsonArray to deserialize from.
     */
    public Int2D(JsonArray a) {
        this.x = a != null && a.size() > 0 ? a.get(0).getInt(0) : 0;
        this.y = a != null && a.size() > 1 ? a.get(1).getInt(0) : 0;
    }

    /**
     * Creates a new Int2D from a JsonElement.
     * Supports:
     * <ul>
     *   <li>JsonObject with keys "x", "y" (default 0)</li>
     *   <li>JsonArray with order [x, y]</li>
     * </ul>
     * 
     * @param el The JsonElement to deserialize from.
     * @return A new Int2D instance.
     */
    public static Int2D fromJsonElement(JsonElement el) {
        if (el == null || el.isNull()) {
            return new Int2D(0, 0);
        } else if (el.isObject()) {
            return new Int2D(el.getObject());
        } else if (el.isArray()) {
            return new Int2D(el.getArray());
        } else {
            return new Int2D(0, 0);
        }
    }

    public Int2D(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public JsonElement toJsonElement() {
        return new JsonObject().put("x", this.x).put("y", this.y).toJsonElement();
    }
}

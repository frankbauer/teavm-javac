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
 * Store an integer size
 * 
 * @author frank
 *
 */
public class Size implements JsonObjectable {
    public final int width;
    public final int height;

    /**
     * Creates a new Size from a JsonObject.
     * Supported keys for width: "width", "w".
     * Supported keys for height: "height", "h".
     * Default value is 0 for both if missing.
     * 
     * @param o The JsonObject to deserialize from.
     */
    public Size(JsonObject o) {
        if (o != null) {
            this.width = o.has("width") ? o.getInt("width", 0) : o.getInt("w", 0);
            this.height = o.has("height") ? o.getInt("height", 0) : o.getInt("h", 0);
        } else {
            this.width = 0;
            this.height = 0;
        }
    }

    /**
     * Creates a new Size from a JsonArray.
     * Expected order: [width, height].
     * 
     * @param a The JsonArray to deserialize from.
     */
    public Size(JsonArray a) {
        this.width = a != null && a.size() > 0 ? a.get(0).getInt(0) : 0;
        this.height = a != null && a.size() > 1 ? a.get(1).getInt(0) : 0;
    }

    /**
     * Creates a new Size from a JsonElement.
     * Supports:
     * <ul>
     *   <li>JsonObject with keys "width"/"w", "height"/"h" (default 0)</li>
     *   <li>JsonArray with order [width, height]</li>
     * </ul>
     * 
     * @param el The JsonElement to deserialize from.
     * @return A new Size instance.
     */
    public static Size fromJsonElement(JsonElement el) {
        if (el == null || el.isNull()) {
            return new Size(0, 0);
        } else if (el.isObject()) {
            return new Size(el.getObject());
        } else if (el.isArray()) {
            return new Size(el.getArray());
        } else {
            return new Size(0, 0);
        }
    }

    public Size(int w, int h) {
        this.width = w;
        this.height = h;
    }

    @Override
    public JsonElement toJsonElement() {
        return new JsonObject().put("width", this.width).put("height", this.height).toJsonElement();
    }
}

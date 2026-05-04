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

public class Geometry implements JsonObjectable {
    public final Vec3D origin;

    /**
     * Creates a new Geometry from a JsonObject.
     * Supported keys for the origin: "origin", "pos", "position", "p", "a".
     * If no key is found or o is null, Vec3D.Zero is used.
     * 
     * @param o The JsonObject to deserialize from.
     */
    public Geometry(JsonObject o) {
        if (o!=null){
            if (o.has("origin")){
                this.origin = Vec3D.fromJsonElement(o.get("origin"));
            } else if (o.has("pos")){
                this.origin = Vec3D.fromJsonElement(o.get("pos"));
            } else if (o.has("position")){
                this.origin = Vec3D.fromJsonElement(o.get("position"));
            } else if (o.has("p")){
                this.origin = Vec3D.fromJsonElement(o.get("p"));
            } else if (o.has("a")){
                this.origin = Vec3D.fromJsonElement(o.get("a"));
            } else {
                this.origin = Vec3D.Zero;
            }
        } else {
            this.origin = Vec3D.Zero;
        }
    }

    /**
     * Creates a new Geometry from a JsonElement.
     * 
     * @param el The JsonElement to deserialize from.
     * @return A new Geometry instance.
     */
    public static Geometry fromJsonElement(JsonElement el) {
        if (el == null || !el.isObject()) return new Geometry(Vec3D.Zero);
        return new Geometry(el.getObject());
    }

    public Geometry(final Vec3D origin) {
        this.origin = origin;
    }

    /**
     * The origin point of the Ray
     * 
     * @return The point of origin (where was the ray emitted from)
     */
    public final Vec3D getOrigin() {
        return origin;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "@" + origin;
    }

    @Override
    public JsonElement toJsonElement() {
        return new JsonObject().put("origin", origin).toJsonElement();
    }
}
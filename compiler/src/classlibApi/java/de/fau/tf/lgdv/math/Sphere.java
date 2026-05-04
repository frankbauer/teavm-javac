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

public class Sphere extends Geometry implements JsonObjectable {
    /**
     * The Radius of the Sphere
     */
    public final double radius;

    /**
     * Creates a new Sphere from a JsonObject.
     * Inherits origin parsing from {@link Geometry#Geometry(JsonObject)}.
     * Supported keys for radius: "radius", "r". Default is 1.0.
     * 
     * @param o The JsonObject to deserialize from.
     */
    public Sphere(JsonObject o) {
        super(o);
         if (o != null ){
            if (o.has("r")) {
                this.radius = o.getDouble("r", 1.0);
            } else if (o.has("radius")) {
                this.radius = o.getDouble("radius", 1.0);
            } else {
                this.radius = 1.0;
            }
        } else {
            this.radius = 1.0;           
        }     
    }

    /**
     * Creates a new Sphere from a Point and Radius
     * 
     * @param p The center of the Sphere
     * @param r The spheres Radius
     */
    public Sphere(Vec3D p, double r) {
        super(p);
        this.radius = r;
    }

    @Override
    public String toString() {
        return super.toString() + " | " + radius;
    }

    /**
     * Returns the radius of the Sphere
     * 
     * @return The spheres radius
     */
    public double getRadius() {
        return radius;
    }

    /**
     * Creates a new Sphere from a JsonElement.
     * 
     * @param el The JsonElement to deserialize from.
     * @return A new Sphere instance.
     */
    public static Sphere fromJsonElement(JsonElement el) {
        if (el == null || !el.isObject()) return new Sphere(Vec3D.Zero, 1.0);
        return new Sphere(el.getObject());
    }

    @Override
    public JsonElement toJsonElement() {
        JsonElement base = super.toJsonElement();
        base.getObject().put("radius", radius);
        return base;
    }
}

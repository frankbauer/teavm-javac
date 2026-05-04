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

public class Cone extends Geometry implements JsonObjectable{
    /**
     * The Radius of the Cone
     */
    public final double radius;
    /**
     * The Tip of the Cone
     */
    public final Vec3D apex;
    
    /**
     * Creates a new Cone from a JsonObject.
     * Inherits origin parsing (base center) from {@link Geometry#Geometry(JsonObject)}.
     * Supported keys for radius: "radius", "r" (default 1.0).
     * The tip (apex) can be defined by:
     * <ul>
     *   <li>"apex": A Vec3D point</li>
     *   <li>"height" or "h": A double value (apex = origin + XAxis * height)</li>
     * </ul>
     * 
     * @param o The JsonObject to deserialize from.
     */
    public Cone(JsonObject o) {
        super(o);
        
        if (o != null ){
            if (o.has("r")) {
                this.radius = o.getDouble("r", 1.0);
            } else if (o.has("radius")) {
                this.radius = o.getDouble("radius", 1.0);
            } else {
                this.radius = 1.0;
            }

            if (o.has("apex")){
                 this.apex = Vec3D.fromJsonElement(o.get("apex"));
            } else if (o.has("height")) {
                double h = o.getDouble("height", 1.0);
                this.apex = this.origin.add(Vec3D.XAxis.mul(h));
            } else {
                double h = o.getDouble("h", 1.0);
                this.apex = this.origin.add(Vec3D.XAxis.mul(h));
            }            

        } else {
            this.radius = 1.0;
            this.apex = this.origin.add(Vec3D.XAxis);
        }        
    }

    /**
     * Creates a new Cone from a JsonElement.
     * 
     * @param el The JsonElement to deserialize from.
     * @return A new Cone instance.
     */
    public static Cone fromJsonElement(JsonElement el) {
        if (el == null || !el.isObject()) return new Cone(Vec3D.Zero, 1.0, 1.0);
        return new Cone(el.getObject());
    }

    /**
     * Creates a new Cone from a Point, a Radius and a Height
     * 
     * @param p The center of the Cones base Circle
     * @param r The Cones radius
     * @param h The Cones height
     */
    public Cone(Vec3D p, double r, double h) {
        super(p);
        this.radius = r;
        this.apex = p.add(Vec3D.XAxis.mul(h));
    }

    /**
     * Creates a new Cone from a Point, a Radius and the location of the Tip
     * 
     * @param p The center of the Cones base Circle
     * @param r The Cones radius
     * @param apex The Focus Point or apex of the Cone
     */
    public Cone(Vec3D p, double r, Vec3D apex) {
        super(apex);
        this.radius = r;
        this.apex = p;
    }

    @Override
    public String toString() {
        return super.toString() + " | " + radius + ", " + apex;
    }

    /**
     * Returns the radius of the Cone
     * 
     * @return The Cones radius
     */
    public double getRadius() {
        return radius;
    }

    /**
     * Returns the focus point (or apex) of the Cone
     * 
     * @return The Cones focus point in world space
     */
    public Vec3D getApex() {
        return apex;
    }

    /**
     * Returns the height of the Cone
     * 
     * @return The Cones height
     */
    public double getHeight() {
        return apex.sub(origin).length();
    }

    /**
     * Returns the opening angle between the center line and the outer bound
     * 
     * @return The Angle in degree
     */
    public double getAngle() {
        final double h = getHeight();
        final double a = Math.sqrt(radius * radius + h * h);
        return Math.toDegrees(Math.asin(radius / a));
    }

    /**
     * Returns the direction of the cone with unit length
     * 
     * @return The cones direction
     */
    public Vec3D getDirection() {
        return apex.sub(origin).normalize();
    }

    @Override
    public JsonElement toJsonElement() {
        JsonElement base = super.toJsonElement();
        base.getObject().put("radius", radius).put("apex", apex);
        return base;
    }
}

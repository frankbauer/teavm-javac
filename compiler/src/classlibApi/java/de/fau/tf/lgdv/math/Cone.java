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

import de.fau.tf.lgdv.JSON;

public class Cone extends Geometry implements JSON.Stringable{
    /**
     * The Radius of the Cone
     */
    public final double radius;
    /**
     * The Tip of the Cone
     */
    public final Vec3D apex;

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

    public String toJSON(){
        return "{\"radius\":"+radius+", \"apex\":"+apex.toJSON()+", \"origin\":"+origin.toJSON()+"}";
    }
}

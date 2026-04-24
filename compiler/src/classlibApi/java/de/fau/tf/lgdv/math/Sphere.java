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

public class Sphere extends Geometry implements JSON.Stringable {
    /**
     * The Radius of the Sphere
     */
    public final double radius;

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

    public String toJSON(){
        return "{\"radius\":"+radius+", \"origin\":"+origin.toJSON()+"}";
    }
}

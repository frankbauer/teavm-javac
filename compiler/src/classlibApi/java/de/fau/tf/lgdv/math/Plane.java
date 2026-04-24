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

/**
 * Represents a Plane in 3D-Space.
 * 
 * @author frank
 *
 */
public class Plane extends Geometry implements JSON.Stringable {
    /**
     * The Planes normal. This value is guaranteed to be normalized!
     */
    public final Vec3D normal;
    /**
     * The x-Axis of the Planes local coordinate frame. The origin of the Planes local coordinate frame can be retrieved
     * in {@link Geometry#origin} or {@link Geometry#getOrigin()}
     */
    public final Vec3D xAxis;
    /**
     * The y-Axis of the Planes local coordinate frame. The origin of the Planes local coordinate frame can be retrieved
     * in {@link Geometry#origin} or {@link Geometry#getOrigin()}
     */
    public final Vec3D yAxis;

    /**
     * Creates a new Plane from a Point and Normal
     * 
     * @param p One Point on the PLane. This Point will act as the origin of the Planes local coordinate frame
     *        {@link Geometry#origin} or {@link Geometry#getOrigin()}
     * @param normal The Planes normal. The Normal is stored normalized ({@link Vec3D#normalize()}).
     */
    public Plane(final Vec3D p, final Vec3D normal) {
        super(p);
        this.normal = normal.normalize();

        xAxis = normal.createPerpendicular().normalize();
        yAxis = normal.cross(xAxis).normalize();
    }

    /**
     * Creates a new Plane from a Point and Normal
     * 
     * @param a One Point on the lLane. This Point will act as the origin of the Planes local coordinate frame
     *        {@link Geometry#origin} or {@link Geometry#getOrigin()}
     * @param b A second point on the plane
     * @param b A third point on the plane
     */
    public Plane(final Vec3D a, final Vec3D b, final Vec3D c) {
        super(a);
        this.normal = b.sub(a).normalize().cross(c.sub(a).normalize()).normalize();

        xAxis = normal.createPerpendicular().normalize();
        yAxis = normal.cross(xAxis).normalize();
    }

    @Override
    public String toString() {
        return super.toString() + " | " + normal;
    }

    /**
     * The Planes normal. This value is guaranteed to be normalized!
     * 
     * @return The normal of the plane
     */
    public Vec3D getNormal() {
        return normal;
    }

    /**
     * The x-Axis of the Planes local coordinate frame in world Space
     * 
     * @return The x-Axis of the 2D-Space created by this plane
     */
    public Vec3D getXAxis() {
        return xAxis;
    }

    /**
     * The y-Axis of the Planes local coordinate frame in world Space
     * 
     * @return The y-Axis of the 2D-Space created by this plane
     */
    public Vec3D getInYAxis() {
        return yAxis;
    }

    public String toJSON(){
        return "{\"xAxis\":"+xAxis.toJSON()+", \"yAxis\":"+yAxis.toJSON()+"\"normal\":"+normal.toJSON()+"}";
    }
}

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
 * Represents an immutable, 3-Dimensional Vector
 * 
 * @author frank
 *
 */
public class Vec3D implements JSON.Stringable {
    /**
     * A Vector pointing in the positive X-Direction
     */
    public static final Vec3D XAxis = new Vec3D(1, 0, 0);
    /**
     * A Vector pointing in the positive Y-Direction
     */
    public static final Vec3D YAxis = new Vec3D(0, 1, 0);
    /**
     * A Vector pointing in the positive Z-Direction
     */
    public static final Vec3D ZAxis = new Vec3D(0, 0, 1);
    /**
     * A 0-Vector
     */
    public static final Vec3D Zero = new Vec3D(0, 0, 0);
    /**
     * A Small Number
     */
    public static final double EPSILON = 0.0001;
    /**
     * The x-Component.
     */
    public final double x;
    /**
     * The y-Component.
     */
    public final double y;
    /**
     * The z-Component.
     */
    public final double z;

    /**
     * Creates a new Vector
     * 
     * @param x The horizontal component
     * @param y The vertical component
     * @param z The depth component
     */
    public Vec3D(final double x, final double y, final double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Creates a new Vector
     * 
     * @param v a 2-Dimensional Vector
     */
    public Vec3D(final Vec2D v) {
        this(v, 0);
    }

    /**
     * Creates a new Vector
     * 
     * @param v A 2-Dimensional Vector
     * @param z The depth component of that Vector
     */
    public Vec3D(final Vec2D v, double z) {
        this.x = v.x;
        this.y = v.y;
        this.z = z;
    }

    /**
     * Compares two Vectors for equality.
     * 
     * @param o The Object to compare with.
     * @return {@code true} when o is an Instance of Vec3D and the difference for all three components is less than
     *         0.01%.
     */
    @Override
    public boolean equals(final Object o) {
        if (o instanceof Vec3D) {
            final Vec3D v = (Vec3D) o;
            return Vec2D.equals(x, v.x)
                    && Vec2D.equals(y, v.y)
                    && Vec2D.equals(z, v.z);
        }

        return super.equals(o);
    }

    /**
     * Compares two Vectors for "directional"-equality. This Version of equals compares the normlized Version of the
     * vectors.
     * 
     * @param o The Object to compare with.
     * @return {@code true} when o is an Instance of Vec3D and the difference from all three normalized components is
     *         less than 0.01%.
     * @see Vec3D#normalize()
     */
    public boolean equalsDirection(final Object o) {
        if (o instanceof Vec3D) {
            final Vec3D v1 = this.normalize();
            final Vec3D v2 = ((Vec3D) o).normalize();

            return v1.equals(v2) || v1.equals(v2.mul(-1));
        }

        return super.equals(o);
    }

    @Override
    public String toString() {
        return "(" + x + "/" + y + "/" + z + ")";
    }

    /**
     * The x-Component.
     * 
     * @return The horizontal component
     */
    final public double getX() {
        return x;
    }

    /**
     * The y-Component.
     * 
     * @return The vertical component
     */
    final public double getY() {
        return y;
    }

    /**
     * The z-Component.
     * 
     * @return The depth component
     */
    final public double getZ() {
        return z;
    }

    /**
     * Divides the components of this vector with a scalar Value and returns the result as a new Vector.
     * 
     * @param s The Scalar to divide with
     * @return The resulting, new Vector
     */
    final public Vec3D div(final double s) {
        return new Vec3D(x / s, y / s, z / s);
    }

    /**
     * Multiplies a scalar Value to the components of this vector and returns the result as a new Vector.
     * 
     * @param s The Scalar to multiply with
     * @return The resulting, new Vector
     */
    final public Vec3D mul(final double s) {
        return new Vec3D(s * x, s * y, s * z);
    }

    /**
     * Adds another Vector to this one and returns the result as a new Vector.
     * 
     * @param v The Vector to add
     * @return The resulting, new Vector
     */
    final public Vec3D add(final Vec3D v) {
        return new Vec3D(v.x + x, v.y + y, v.z + z);
    }

    /**
     * Subtracts another Vector from this one and returns the result as a new Vector (this-v).
     * 
     * @param v The Vector to subtract
     * @return The resulting, new Vector
     */
    final public Vec3D sub(final Vec3D v) {
        return new Vec3D(x - v.x, y - v.y, z - v.z);
    }

    /**
     * Returns a normalized Vector (unit length).
     * 
     * @return The normalized Version of this Vector
     */
    final public Vec3D normalize() {
        final double len = length();
        if (Math.abs(len) < Vec3D.EPSILON)
            return this;

        return this.mul(1.0 / len);
    }

    /**
     * Calculates the length of the vector
     * 
     * @return Length of the Vector
     */
    final public double length() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    /**
     * Calculates the dot- or inner- or scalar-Product.
     * 
     * @param v The Vector to calculate the dot-product with.
     * @return The resulting value of the dot-product of this Vector with the passed one.
     */
    final public double dot(final Vec3D v) {
        return x * v.x + y * v.y + z * v.z;
    }

    /**
     * Calculates the cross- or outer- or vector-Product.
     * 
     * @param v The Vector to calculate the cross-product with.
     * @return The resulting value of the cross-product of this Vector with the passed one.
     */
    final public Vec3D cross(final Vec3D v) {
        return new Vec3D(y * v.z - z * v.y, z * v.x - x * v.z, x * v.y - y
                * v.x);
    }

    /**
     * Create a perpendicular Vector
     * 
     * @return A Vector perpendicular to this one
     */
    final public Vec3D createPerpendicular() {
        if (equals(Vec3D.XAxis))
            return new Vec3D(0, -1, 0);
        else if (equals(Vec3D.XAxis.mul(-1.0)))
            return new Vec3D(0, 1, 0);
        else if (Math.abs(getX()) > Vec3D.EPSILON) {
            final Vec3D d = new Vec3D(0, getY(), getZ());
            return cross(d);
        } else if (equals(Vec3D.YAxis))
            return new Vec3D(-1, 0, 0);
        else if (equals(Vec3D.YAxis.mul(-1.0)))
            return new Vec3D(1, 0, 0);
        else if (Math.abs(getY()) > Vec3D.EPSILON) {
            final Vec3D d = new Vec3D(getX(), 0, getZ());
            return cross(d);
        } else if (equals(Vec3D.ZAxis))
            return new Vec3D(1, 0, 0);
        else if (equals(Vec3D.ZAxis.mul(-1.0)))
            return new Vec3D(-1, 0, 0);
        else {
            final Vec3D d = new Vec3D(getX(), getY(), 0);
            return cross(d);
        }
    }

    /**
     * Reflects the Vector along the given axis and returns the result as a new Vector.
     * 
     * @param axis The axis to reflect this vector on.
     * @return The reflected Vector.
     */
    final public Vec3D reflect(final Vec3D axis) {
        final Vec3D r = this;
        final Vec3D nnn = axis.mul(2 * axis.dot(r));
        return r.sub(nnn);
    }

    /**
     * Projects the Vector to the given axis and returns the result as a new Vector.
     * 
     * @param axis The axis to project this vector on.
     * @return The projected Vector.
     */
    final public Vec3D project(final Vec3D axis) {
        final Vec3D b = axis.normalize();
        final double a1 = this.dot(b);
        return b.mul(a1);
    }

    /**
     * Rejection of the Vector with the given axis. The projection of a Vector v plus the rejection of the Vector v is
     * equal to v.
     * 
     * @param axis The axis to reject this vector on.
     * @return The rejected Vector.
     */
    final public Vec3D reject(final Vec3D axis) {
        return this.sub(project(axis));
    }

    /**
     * Calculates the distance between two vectors
     * 
     * @param v The second Vector
     * @return The eucledian-distance between both Vectors (this - v).
     */
    public double distance(Vec3D v) {
        return this.sub(v).length();
    }

    /**
     * Projects the Vector to the given plane and returns the result as a new Vector.
     * 
     * @param plane The plane to project this vector on.
     * @return The projected Vector.
     */
    final public Vec3D project(final Plane plane) {
        final double d = signedDistance(plane);
        return this.sub(plane.normal.mul(d));
    }

    /**
     * Projects the Vector to the given ray and returns the result as a new Vector.
     * 
     * @param ray The ray to project this vector on.
     * @return The projected Vector or {@code null} if no projection exists.
     */
    final public Vec3D project(final Ray ray) {
        final Vec3D A = ray.origin;
        final Vec3D AP = this.sub(ray.origin);
        final Vec3D AB = ray.dir;

        final Vec3D proj = AB.mul(AP.dot(AB) / AB.dot(AB));
        /*if (proj.dot(AB) < 0)
            return null;*/
        return A.add(proj);
    }

    /**
     * Calculates the signed distance between this Vector (as Point) and a Plane
     * 
     * @param p The Plane
     * @return The signed-distance (this - p).
     */
    public double signedDistance(Plane p) {
        return p.normal.dot(this.sub(p.origin));
    }

    /**
     * Calculates the distance between this Vector (as Point) and a Plane
     * 
     * @param p The Plane
     * @return The eucledian-distance |this - p|.
     */
    public double distance(Plane p) {
        return Math.abs(signedDistance(p));
    }

    public String toJSON(){
        return "{\"x\":" + this.x + ",\"y\":"+this.y+ ",\"z\":"+this.z+"}";
    }
}

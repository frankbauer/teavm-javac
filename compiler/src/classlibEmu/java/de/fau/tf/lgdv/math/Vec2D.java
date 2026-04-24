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

import java.awt.Point;
import de.fau.tf.lgdv.JSON;

/**
 * Represents an immutable 2-Dimensional Vector
 * 
 * @author frank
 *
 */
public class Vec2D implements JSON.Stringable{
    /**
     * A Vector pointing in the positive X-Direction
     */
    public static final Vec2D XAxis = new Vec2D(1, 0);
    /**
     * A Vector pointing in the positive Y-Direction
     */
    public static final Vec2D YAxis = new Vec2D(0, 1);
    /**
     * A 0-Vector
     */
    public static final Vec2D Zero = new Vec2D(0, 0);
    /**
     * The x-Component.
     */
    public final double x;
    /**
     * The y-Component.
     */
    public final double y;

    /**
     * Creates a new Vector at the Origin
     */
    public Vec2D() {
        this(0.0, 0.0);
    }

    /**
     * Converts a Point (form the Java standards API) to a Vector
     * 
     * @param p The Point to convert.
     */
    public Vec2D(Point p) {
        this(p.x, p.y);
    }

    /**
     * Creates a new Vector
     * 
     * @param x The horizontal component
     * @param y The vertical component
     */
    public Vec2D(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /**
     * The x-Component.
     * 
     * @return The horizontal component
     */
    public double getX() {
        return x;
    }

    /**
     * The y-Component.
     * 
     * @return The vertical component
     */
    public double getY() {
        return y;
    }

    /**
     * Converts a Vector to a Point (form the Java standards API).
     * 
     * @return The Point representing this Vector. The components are rounded!
     */
    public Point toPoint() {
        return new Point((int) Math.round(x), (int) Math.round(y));
    }

    /**
     * Creates a normalized Vector
     * 
     * @return A Vector in the same direction with unit length.
     */
    public Vec2D toUnitLength() {
        final double l = length();
        if (l == 0) {
            return new Vec2D();
        }
        return new Vec2D(x / l, y / l);
    }

    /**
     * Create a perpendicular Vector
     * 
     * @return A Vector perpendicular to this one
     */
    public Vec2D toPerpendicular() {
        return new Vec2D(y, -x);
    }

    /**
     * Create an inverted Vector
     * 
     * @return Returns a Vector pointing in the negative direction.
     */
    public Vec2D toInverted() {
        return new Vec2D(-x, -y);
    }

    /**
     * Creates a convenient String representation of the Vector
     * 
     * @return A String representing this Vector
     */
    @Override
    public String toString() {
        return "(" + x + "/" + y + ")";
    }

    /**
     * Calculates the l2-norm of this Vector
     * 
     * @return The Sqared Length of the Vector
     */
    public double norm() {
        return x * x + y * y;
    }

    /**
     * Calculates the length of the vector
     * 
     * @return Length of the Vector
     */
    public double length() {
        return Math.sqrt(x * x + y * y);
    }

    /**
     * Adds another Vector to this one and returns the result as a new Vector.
     * 
     * @param v The Vector to add
     * @return The resulting, new Vector
     */
    public Vec2D add(Vec2D v) {
        return new Vec2D(x + v.x, y + v.y);
    }

    /**
     * Subtracts another Vector from this one and returns the result as a new Vector.
     * 
     * @param v The Vector to subtract
     * @return The resulting, new Vector
     */
    public Vec2D sub(Vec2D v) {
        return new Vec2D(x - v.x, y - v.y);
    }

    /**
     * Multiplies a scalar Value to the components of this vector and returns the result as a new Vector.
     * 
     * @param f The Scalar to multiply with
     * @return The resulting, new Vector
     */
    public Vec2D mul(double f) {
        return new Vec2D(x * f, y * f);

    }

    /**
     * {@gdi.internal} Tests whether or not two values are considered equal.A value is equal when both differ by less
     * than 0.001%.
     * 
     * @param ref The first (reference) value
     * @param stud The second value
     * @return {@code true} If the passed Vector is euql to this one.
     */
    static boolean equals(double ref, double stud) {
        return equalsRelaxed(ref, stud);
    }

    private static boolean equalsRelaxed(double ref, double stud) {
        if (Double.isNaN(ref)) {
            return stud == 0.0 || Double.isNaN(stud);
        }
        if (Math.abs(ref) < 0.000001) {
            return Math.abs(stud) < 0.000001;
        }
        double rel = Math.abs(stud / ref - 1.0);
        return (rel < 0.00001);
    }

    /**
     * Tests whether or not two vectors are considered equal.A Vector is equal when both components differ by less than
     * 0.001%.
     * 
     * @param obj The vector to test against
     * @return {@code true} If the passed Vector is equal to this one.
     */
    @Override
    public boolean equals(Object obj) {

        if (!(obj instanceof Vec2D)) {
            return false;
        }

        Vec2D other = (Vec2D) obj;
        return equalsRelaxed(this.x, other.x) && equalsRelaxed(this.y, other.y);
    }

    /**
     * Generates a hash-Code for this Vector.
     * 
     * @return a HasCode (x-or of the components hash-Codes.)
     */
    @Override
    public int hashCode() {
        return new Double(x).hashCode() ^ new Double(y).hashCode();
    }

    /**
     * Calculates the distance between two vectors
     * 
     * @param a The first Vector
     * @param b The second Vector
     * @return The eucledian-distance between both Vectors (a-b).
     */
    public static double distance(Vec2D a, Vec2D b) {
        return a.sub(b).length();
    }

    /**
     * Calculates the distance between two vectors
     * 
     * @param v The second Vector
     * @return The eucledian-distance between both Vectors (this - v).
     */
    public double distance(Vec2D v) {
        return distance(this, v);
    }

    /**
     * Rotates this point around the given Pivot
     * 
     * @param pivot The center of rotation
     * @param angle The rotation angle (in degrees)
     * @return The rotated point
     */
    public Vec2D rotateAround(Vec2D pivot, double angle) {
        angle = Math.toRadians(angle);
        final double s = Math.sin(angle);
        final double c = Math.cos(angle);

        Vec2D p = this.sub(pivot);
        p = new Vec2D(p.x * c - p.y * s, p.x * s + p.y * c);
        return p.add(pivot);
    }

    public String toJSON(){
        return "{\"x\":" + this.x + ",\"y\":"+this.y+"}";
    }
}

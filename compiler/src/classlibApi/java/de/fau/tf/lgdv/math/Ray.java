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
 * 
 * Describes a Ray in Space (Point and Direction)
 * 
 */
public class Ray extends Geometry  implements JSON.Stringable{
    /**
     * The direction of the Ray
     */
    public final Vec3D dir;

    /**
     * Creates a new Ray: ray = p + t*dir with t from R+. <br>
     * A normalized Version of the Direction is stored and returned by {@link #getDirection()}
     * 
     * @param p Starting Point of the Ray
     * @param dir Direction of the Ray
     */
    public Ray(final Vec3D p, final Vec3D dir) {
        super(p);
        this.dir = dir.normalize();
    }

    @Override
    public String toString() {
        return "[" + origin + " -> " + dir + "]";
    }

    /**
     * The direction of the Ray
     * 
     * @return The direction the ray is pointing at. The direction is guaranteed to be normalized.
     */
    final public Vec3D getDirection() {
        return dir;
    }

    /**
     * Creates a reflected Ray
     * 
     * @param n The axis the ray is reflected on
     * @return The ray reflected on n
     */
    final public Ray reflect(final Vec3D n) {
        final Vec3D d = dir.reflect(n);
        return new Ray(origin, d);
    }

    /**
     * Test whether or not a Point is on the Ray (in positive direction)
     * 
     * @param p The point to test with
     * @return {@code true} if the Point is on the Ray.
     */
    public boolean contains(Vec3D p) {
        if (dir.x == 0)
            return Math.abs(p.getX() - origin.x) < 0.00001;
        if (dir.y == 0)
            return Math.abs(p.getY() - origin.y) < 0.00001;

        final double t1 = (p.getX() - origin.x) / dir.x;
        final double t2 = (p.getY() - origin.y) / dir.y;

        return Math.abs(t1 - t2) < 0.00001 && t1 >= 0;
    }

    @Override
    public boolean equals(final Object o) {
        if (o instanceof Ray) {
            final Ray r = (Ray) o;
            //-direction is wrong, direction is normalized in the constructor so we can use equals (and not qualsDirection)
            return getOrigin().equals(r.getOrigin()) && getDirection().equals(r.getDirection());
        }

        return super.equals(o);
    }

    /**
     * Calculates the intersection of this ray with a given {@link Plane}
     * 
     * @param plane The {@link Plane} to intersect with
     * @return The Position of the Intersection with the Plane or {@code null} if the ray does not intersect the plane
     */
    public Vec3D intersect(final Plane plane) {
        final double d = plane.origin.sub(this.getOrigin()).dot(plane.normal)
                / this.getDirection().dot(plane.normal);
        if (d < 0)
            return null;

        final Vec3D v = this.getDirection().mul(d);

        return v.add(this.getOrigin());
    }

    /**
     * Calculates the intersection(s) of this ray with a given {@link Sphere}
     * 
     * @param sphere The {@link Sphere} to intersect with
     * @param outNormal {@code null} or an array with at least two entries. For each calculated intersection point a
     *        normal direction is calculated and stored. All other entries are set to {@code null}.
     * @return The Position of the Intersection(s) with the Sphere. This call will never return {@code null} but may
     *         return an empty array if no intersection was found. The result is sorted by distance to the rays origin.
     */
    public Vec3D[] intersect(final Sphere sphere, Vec3D[] outNormal) {
        final Vec3D d = this.getDirection();
        final Vec3D o = this.getOrigin().sub(sphere.origin);

        final double a = d.dot(d);
        final double b = 2 * o.dot(d);
        final double c = o.dot(o) - (sphere.radius * sphere.radius);

        final double discriminant = b * b - 4 * a * c;
        // no solution
        if (discriminant < 0)
            return new Vec3D[0];

        // tackle numerical instabilities
        final double sqrtDiscri = Math.sqrt(discriminant);
        double q;
        if (b < 0) {
            q = (-b - sqrtDiscri) / 2.0;
        } else {
            q = (-b + sqrtDiscri) / 2.0;
        }

        // solve quadratic eqn
        double t0 = q / a;
        double t1 = c / q;

        // sort the result by distance on the ray
        if (t0 > t1) {
            final double dum = t0;
            t0 = t1;
            t1 = dum;
        }

        // both intersections are in the wrong direction
        if (t1 < 0)
            return new Vec3D[0];

        Vec3D[] res;
        // take the intersection closest to the rays origin
        if (t0 < 0) {
            res = new Vec3D[] { sphere.origin.add(o.add(d.mul(t1))) };
        } else {
            res = new Vec3D[] { sphere.origin.add(o.add(d.mul(t0))), sphere.origin.add(o.add(d.mul(t1))) };
        }

        if (outNormal != null) {
            for (int i = 0; i < outNormal.length; i++) {
                if (i < res.length) {
                    outNormal[i] = res[i].sub(sphere.origin);
                } else {
                    outNormal[i] = null;
                }
            }
        }

        return res;
    }

    /**
     * Calculates the intersection(s) of this ray with a given {@link Cone} Adapted from
     * https://www.geometrictools.com/GTEngine/Include/Mathematics/GteIntrLine3Cone3.h
     * 
     * @param cone The {@link Cone} to intersect with
     * @param outNormal {@code null} or an array with at least two entries. For each calculated intersection point a
     *        normal direction is calculated and stored. All other entries are set to {@code null}.
     * @return The Position of the Intersection(s) with the Cone. This call will never return {@code null} but may
     *         return an empty array if no intersection was found. The result is sorted by distance to the rays origin.
     * 
     */
    public Vec3D[] intersect(final Cone cone, Vec3D[] outNormal) {
        final double[] parameter = new double[2];
        final boolean[] onTop = new boolean[2];
        int type = -1;
        boolean intersect = false;

        final Vec3D V = cone.origin;
        final Vec3D D = cone.getDirection();
        final double h = cone.getHeight();
        final double theta = Math.toRadians(cone.getAngle());
        final double g = Math.cos(theta);

        final Vec3D P = this.getOrigin();
        final Vec3D U = this.getDirection().normalize();

        // The cone has vertex V, unit-length axis direction D, angle theta in
        // (0,pi/2), and height h in (0,+infinity).  The line is P + t*U, where U
        // is a unit-length direction vector.  Define g = cos(theta).  The cone
        // is represented by
        //   (X-V)^T * (D*D^T - g^2*I) * (X-V) = 0,  0 <= Dot(D,X-V) <= h
        // The first equation defines a double-sided cone.  The first inequality
        // in the second equation limits this to a single-sided cone containing
        // the ray V + s*D with s >= 0.  We will call this the 'positive cone'.
        // The single-sided cone containing ray V + s * t with s <= 0 is called
        // the 'negative cone'.  The double-sided cone is the union of the
        // positive cone and negative cone.  The second inequality in the second
        // equation limits the single-sided cone to the region bounded by the
        // height.  Setting X(t) = P + t*U, the equations are
        //   c2*t^2 + 2*c1*t + c0 = 0,  0 <= Dot(D,U)*t + Dot(D,P-V) <= h
        // where
        //   c2 = Dot(D,U)^2 - g^2
        //   c1 = Dot(D,U)*Dot(D,P-V) - g^2*Dot(U,P-V)
        //   c0 = Dot(D,P-V)^2 - g^2*Dot(P-V,P-V)
        // The following code computes the t-interval that satisfies the quadratic
        // equation subject to the linear inequality constraints.

        final Vec3D PmV = P.sub(V);
        final double DdU = D.dot(U);
        final double DdPmV = D.dot(PmV);
        final double UdPmV = U.dot(PmV);
        final double PmVdPmV = PmV.dot(PmV);
        final double cosAngleSqr = g * g;

        final double c2 = DdU * DdU - cosAngleSqr;
        final double c1 = DdU * DdPmV - cosAngleSqr * UdPmV;
        final double c0 = DdPmV * DdPmV - cosAngleSqr * PmVdPmV;
        double t;

        if (c2 != 0) {
            double discr = c1 * c1 - c0 * c2;
            if (discr < 0) {
                // The quadratic has no double-valued roots.  The line does not
                // intersect the double-sided cone.
                //System.out.println("no double-valued roots:" + discr);
                return new Vec3D[0];
            } else if (discr > 0) {
                // The quadratic has two distinct double-valued roots.  However, one
                // or both of them might intersect the negative cone.  We are
                // interested only in those intersections with the positive cone.
                final double root = Math.sqrt(discr);
                final double invC2 = (1.0) / c2;
                int numParameters = 0;

                t = (-c1 - root) * invC2;
                if (DdU * t + DdPmV >= 0 && t > 0) {
                    parameter[numParameters++] = t;
                }

                t = (-c1 + root) * invC2;
                if (DdU * t + DdPmV >= 0 && t > 0) {
                    parameter[numParameters++] = t;
                }

                if (numParameters == 2) {
                    // The line intersects the positive cone in two distinct
                    // points.
                    intersect = true;
                    type = 2;
                    if (parameter[0] > parameter[1]) {
                        double tmp = parameter[0];
                        parameter[0] = parameter[1];
                        parameter[1] = tmp;
                    }
                } else if (numParameters == 1) {
                    // The line intersects the positive cone in a single point and
                    // the negative cone in a single point.  We report only the
                    // intersection with the positive cone.
                    intersect = true;
                    if (DdU > 0) {
                        type = 3;
                        parameter[1] = Double.MAX_VALUE;
                    } else {
                        type = 4;
                        parameter[1] = parameter[0];
                        parameter[0] = -Double.MAX_VALUE;

                    }
                } else {
                    // The line intersects the negative cone in two distinct
                    // points, but we are interested only in the intersections
                    // with the positive cone.
                    intersect = false;
                    type = 0;
                    //System.out.println("intersects the negative cone");
                    return new Vec3D[0];
                }
            } else  // discr == 0
            {
                // One repeated double root; the line is tangent to the double-sided
                // cone at a single point.  Report only the point if it is on the
                // positive cone.
                t = -c1 / c2;
                if (DdU * t + DdPmV >= 0 && t > 0) {
                    intersect = true;
                    type = 1;
                    parameter[0] = t;
                    parameter[1] = t;
                } else {
                    intersect = false;
                    type = 0;
                    //System.out.println("One repeated double root");
                    return new Vec3D[0];
                }
            }
        } else if (c1 != 0) {
            // c2 = 0, c1 != 0; U is a direction vector on the cone boundary
            t = -0.5 * c0 / c1;
            if (DdU * t + DdPmV >= 0 && t > 0) {
                // The line intersects the positive cone and the ray of
                // intersection is interior to the positive cone.
                intersect = true;
                if (DdU > 0) {
                    type = 3;
                    parameter[0] = t;
                    parameter[1] = Double.MAX_VALUE;
                } else {
                    type = 4;
                    parameter[0] = -Double.MAX_VALUE;
                    parameter[1] = t;
                }
            } else {
                // The line intersects the negative cone and the ray of
                // intersection is interior to the positive cone.
                intersect = false;
                type = 0;
                //System.out.println("The line intersects the negative, interior to positive");
                return new Vec3D[0];
            }
        } else if (c0 != 0) {
            // c2 = c1 = 0, c0 != 0.  Cross(D,U) is perpendicular to Cross(P-V,U)
            intersect = false;
            type = 0;
            //System.out.println("perpendicular to Cross(P-V,U)");
            return new Vec3D[0];
        } else {
            // c2 = c1 = c0 = 0; the line is on the cone boundary.
            intersect = true;
            type = 5;
            parameter[0] = -Double.MAX_VALUE;
            parameter[1] = +Double.MAX_VALUE;
        }

        if (h < Double.MAX_VALUE) {
            if (DdU != 0) {
                // Clamp the intersection to the height of the cone.
                double invDdU = (1) / DdU;
                double[] hInterval = new double[2];
                if (DdU > 0) {
                    hInterval[0] = -DdPmV * invDdU;
                    hInterval[1] = (h - DdPmV) * invDdU;
                } else // (DdU < (double)0)
                {
                    hInterval[0] = (h - DdPmV) * invDdU;
                    hInterval[1] = -DdPmV * invDdU;
                }
                if (hInterval[0] > 0) {
                    if (hInterval[0] > parameter[0]) {
                        //System.out.println("0>0");
                        onTop[0] = true;
                        parameter[0] = hInterval[0];
                    }
                    if (hInterval[0] > parameter[1]) {
                        //System.out.println("0>1");
                        intersect = false;
                    }
                }

                if (hInterval[1] > 0) {
                    if (hInterval[1] < parameter[1]) {
                        //System.out.println("1<1");
                        onTop[1] = true;
                        parameter[1] = hInterval[1];
                    }
                    if (hInterval[1] < parameter[0]) {
                        //System.out.println("1<0");
                        intersect = false;
                    }
                }
                //System.out.println("???" + hInterval[0] + " " + hInterval[1] + " - " + parameter[0] + " " + parameter[1]);
            } else if (intersect) {
                if (DdPmV > h) {
                    intersect = false;
                    type = 0;
                }
            }
        }

        if (!intersect) {
            //System.out.println("Nothing");
            return new Vec3D[0];
        }

        Vec3D[] res = null;
        if (type == 1 || type == 3) { //point and ray
            res = new Vec3D[] {
                    P.add(U.mul(parameter[0]))
            };
        } else if (type == 2) { //segment
            res = new Vec3D[] {
                    P.add(U.mul(parameter[0])),
                    P.add(U.mul(parameter[1]))
            };
        } else if (type == 4) { //ray
            res = new Vec3D[] {
                    P.add(U.mul(parameter[1]))
            };
        } else if (type == 5) { //line
            res = new Vec3D[] {
                    P
            };
        }

        if (res == null)
            return new Vec3D[0];

        if (outNormal != null) {
            final Ray axis = new Ray(V, cone.getDirection());

            for (int i = 0; i < outNormal.length; i++) {
                if (i < res.length) {
                    if (onTop[i]) {
                        outNormal[i] = axis.dir;
                    } else {
                        final Vec3D d1 = cone.apex.sub(cone.origin).normalize();
                        final Vec3D d2 = res[i].sub(cone.origin).normalize();
                        final Vec3D tangent = d1.cross(d2);
                        final Vec3D normal = tangent.cross(d2);
                        outNormal[i] = normal;
                    }

                } else {
                    outNormal[i] = null;
                }
            }
        }
        return res;
    }

    public String toJSON(){
        return "{\"origin\":"+origin.toJSON()+", \"dir\":"+dir.toJSON()+"}";
    }
}

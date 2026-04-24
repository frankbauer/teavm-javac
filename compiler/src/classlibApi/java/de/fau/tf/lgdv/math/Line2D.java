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

public class Line2D implements JSON.Stringable{
    public final Vec2D origin;
    public final Vec2D dir;

    public Line2D(Vec2D start, Vec2D dir) {
        this.origin = start;
        this.dir = dir;
    }

    public static Line2D fromPoints(Vec2D start, Vec2D end) {
        return new Line2D(start, end.sub(start));
    }

    public Vec2D getOrigin() {
        return origin;
    }

    public Vec2D getDirection() {
        return dir;
    }

    public Line2D lineWithUnitDirection() {
        return new Line2D(origin, dir.toUnitLength());
    }

    public Vec2D poinAt(double t) {
        return origin.add(dir.mul(t));
    }

    @Override
    public String toString() {
        return "[line:" + origin + "/" + dir + "]";
    }

    public Line2D rotateAroundStart(double angle) {
        Vec2D end = dir.rotateAround(Vec2D.Zero, angle);
        return new Line2D(origin, end);
    }

    public Line2D rotateAroundEnd(double angle) {
        Vec2D end = dir.mul(-1).rotateAround(Vec2D.Zero, angle);
        return new Line2D(poinAt(1), end);
    }

    public double length() {
        return dir.length();
    }

    public Line2D invertDirection() {
        return new Line2D(poinAt(1), dir.mul(-1));
    }

    public Line2D lineWithNewLength(double len) {
        return new Line2D(origin, dir.toUnitLength().mul(len));
    }

    public String toJSON(){
        return "{\"origin\":"+ origin.toJSON()+", \"dir\":"+dir.toJSON()+"}";
    }
}

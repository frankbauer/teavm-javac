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

public class Line2D implements JsonObjectable{
    public final Vec2D origin;
    public final Vec2D dir;

    /**
     * Creates a new Line2D from a JsonObject.
     * Supported keys for origin: "origin", "start", "p1".
     * Supported keys for direction/end: "dir" (as vector), "end" (as point), "p2" (as point).
     * If "end" or "p2" is provided, the direction is calculated as (end - origin).
     * Defaults to origin (0,0) and direction (1,0).
     * 
     * @param o The JsonObject to deserialize from.
     */
    public Line2D(JsonObject o) {
        if (o != null) {
            if (o.has("origin")) {
                this.origin = Vec2D.fromJsonElement(o.get("origin"));
            } else if (o.has("start")) {
                this.origin = Vec2D.fromJsonElement(o.get("start"));
            } else if (o.has("p1")) {
                this.origin = Vec2D.fromJsonElement(o.get("p1"));
            } else {
                this.origin = Vec2D.Zero;
            }

            if (o.has("dir")) {
                this.dir = Vec2D.fromJsonElement(o.get("dir"));
            } else if (o.has("end")) {
                this.dir = Vec2D.fromJsonElement(o.get("end")).sub(this.origin);
            } else if (o.has("p2")) {
                this.dir = Vec2D.fromJsonElement(o.get("p2")).sub(this.origin);
            } else {
                this.dir = Vec2D.XAxis;
            }
        } else {
            this.origin = Vec2D.Zero;
            this.dir = Vec2D.XAxis;
        }
    }

    /**
     * Creates a new Line2D from a JsonElement.
     * 
     * @param el The JsonElement to deserialize from.
     * @return A new Line2D instance.
     */
    public static Line2D fromJsonElement(JsonElement el) {
        if (el == null || !el.isObject()) return new Line2D(Vec2D.Zero, Vec2D.XAxis);
        return new Line2D(el.getObject());
    }

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

    @Override
    public JsonElement toJsonElement() {
        return new JsonObject().put("origin", origin).put("dir", dir).toJsonElement();
    }
}

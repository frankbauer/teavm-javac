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
 * Store an integer size
 * 
 * @author frank
 *
 */
public class Size implements JSON.Stringable {
    public final int width;
    public final int height;

    public Size(int w, int h) {
        this.width = w;
        this.height = h;
    }

    public String toJSON(){
        return "{\"width\":"+width+", \"height\":"+height+"}";
    }
}

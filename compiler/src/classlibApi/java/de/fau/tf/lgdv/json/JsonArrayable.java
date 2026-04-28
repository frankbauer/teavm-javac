/*
 *  Copyright 2026 frank bauer.
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
package de.fau.tf.lgdv.json;

/**
 * An interface for objects that can be converted to a JsonArray
 */
public interface JsonArrayable extends JsonSerializer {
    /**
     * Converts the object to a JsonArray
     * @return the JsonArray representation of the object
     */
    JsonArray toJsonArray();

    @Override
    default String toJson() {
        return toJsonArray().toJson();
    }
}

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
package de.fau.tf.lgdv;

import org.teavm.jso.JSProperty;
import de.fau.tf.lgdv.json.*;

/**
 * Specialized message for request-reply queries.
 */
public interface CodeBlocksQueryMessage extends CodeBlocksBaseMessage {
    @JSProperty("queryId")
    int getQueryId();

    @JSProperty("queryId")
    void setQueryId(int queryId);@JSProperty("json")
    String _getJSON();

    default JsonElement getJSON() {
        String s = _getJSON();
        return s != null ? JsonParser.parse(s) : null;
    }

    @JSProperty("json")
    void _setJSON(String value);    

    default void setJSON(JsonSerializer json) {
        String s = json != null ? json.toJson() : null;
        _setJSON(s);
    } 
}

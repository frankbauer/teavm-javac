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

public class JsonObject implements JsonObjectable {
    private final java.util.Map<String, JsonElement> map = new java.util.HashMap<>();

@Override
    public JsonElement toJsonElement() {
        return new JsonElement(this);
    }

    public JsonObject put(String key, JsonArray o){
        map.put(JsonParser.validKey(key), new JsonElement(o));
        return this;
    }

    public JsonObject put(String key, JsonObject o){
        map.put(JsonParser.validKey(key), new JsonElement(o));
        return this;
    }

    public JsonObject put(String key, JsonObjectable o){
        return put(key, o.toJsonElement());
    }

    public JsonObject putNull(String key){
        map.put(JsonParser.validKey(key), new JsonElement(null));
        return this;
    }

    public JsonObject put(String key, JsonElement o){
        map.put(JsonParser.validKey(key), o);
        return this;
    }

    public JsonObject put(String key, int i){
        map.put(JsonParser.validKey(key), new JsonElement(i));
        return this;
    }

    public JsonObject put(String key, double d){
        map.put(JsonParser.validKey(key), new JsonElement(d));
        return this;
    }

    public JsonObject put(String key, String s){
        map.put(JsonParser.validKey(key), new JsonElement(s));
        return this;
    }

    public JsonObject put(String key, java.util.Date d){
        map.put(JsonParser.validKey(key), new JsonElement(JsonParser.dateToString(d)));
        return this;
    }

    public JsonObject put(String key, boolean b){
        map.put(JsonParser.validKey(key), new JsonElement(b));
        return this;
    }

    public int getInt(String key){
        return map.get(key).getInteger();
    }
    public int getInt(String key, int defaultValue){
        return map.get(key).getInteger(defaultValue);
    }

    public double getDouble(String key){
        return map.get(key).getDouble();
    }
    public double getDouble(String key, Double defaultValue){
        return map.get(key).getDouble(defaultValue);
    }

    public boolean getBoolean(String key){
        return map.get(key).getBoolean();
    }

    public boolean getBoolean(String key, Boolean defaultValue){
        return map.get(key).getBoolean(defaultValue);
    }

    public String getString(String key){
        return map.get(key).getString();
    }

    public String getString(String key, String defaultValue){
        return map.get(key).getString(defaultValue);
    }

    public java.util.Date getDate(String key){
        return map.get(key).getDate();
    }
    public java.util.Date getDate(String key, java.util.Date defaultValue){
        return map.get(key).getDate(defaultValue);
    }

    public JsonElement get(String key){
        return map.get(key);
    }

    public boolean isNull(String key){
        return map.get(key).isNull();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        boolean first = true;
        for (String key : map.keySet()){
            if (first){
                first = false;
            } else {
                sb.append(',');
            }
            sb.append(key);
            sb.append(':');
            sb.append(map.get(key));
        }
        sb.append("}");
        return sb.toString();
    }

    public String toJson(){
        return map
                .entrySet()
                .stream()
                .map(e -> "\"" + JsonParser.validKey(e.getKey()) + "\":" + e.getValue().toJson())
                .collect(java.util.stream.Collectors.joining(", ", "{", "}"));
    }
}
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

public class JsonElement implements JsonObjectable {
    private final Object value;
    JsonElement(Object value) {
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    public int getInteger(){
        return (Integer) value;
    }

    public int getInteger(int defaultValue){
        if (value==null) return defaultValue;
        if (isDouble()){
            return (int) ((Double) value);
        }
        if (!isInteger()) return defaultValue;
        return (Integer) value;
    }


    public double getDouble(){
        return (Double) value;
    }

    public double getDouble(double defaultValue){
        if (value==null || !isDouble()) return defaultValue;
        if (isInteger()){
            return (double)((int) value);
        }
        return (double) value;
    }

    public String getString(){
        return (String) value;
    }

    public String getString(String defaultValue){
        if (value==null || !isString()) return defaultValue;
        return (String) value;
    }

    public boolean getBoolean(){
        return (Boolean) value;
    }

    public boolean getBoolean(boolean defaultValue){
        if (value==null || !isBoolean()) return defaultValue;
        return (Boolean) value;
    }

    public JsonObject getObject(){
        return (JsonObject) value;
    }

    public JsonObject getObject(JsonObject defaultValue){
        if (value==null || !isObject()) return defaultValue;
        return (JsonObject) value;
    }

    public JsonArray getArray(){
        return (JsonArray) value;
    }

    public JsonArray getArray(JsonArray defaultValue){
        if (value==null || !isArray()) return defaultValue;
        return (JsonArray) value;
    }

    public java.util.Date getDate(){
        return JsonParser.stringToDate((String) value);
    }

    public java.util.Date getDate(java.util.Date defaultValue){
        if (value==null || !isDate()) return defaultValue;
        return getDate();
    }


    //a method that tests if the value is a String
    public boolean isString(){
        return value instanceof String;
    }

    //a method that tests if the value is a Double
    public boolean isDouble(){
        return (value instanceof Double) || (value instanceof Integer);
    }

    //a method that tests if the value is an Integer
    public boolean isInteger(){
        return value instanceof Integer;
    }

    //a method that tests if the value is a Boolean
    public boolean isBoolean(){
        return value instanceof Boolean;
    }

    //a method that tests if the value is a JsonObject
    public boolean isObject(){
        return value instanceof JsonObject;
    }

    //a method that tests if the value is a JsonArray
    public boolean isArray(){
        return value instanceof JsonArray;
    }

    public boolean isDate(){
        if (isString()){
            try {
                ISOTimestampConverter.isValidISO((String) value);
            } catch (Exception e){
                return false;
            }
            return true;
        }
        return false;
    }

    public boolean isNull(){
        return value==null;
    }

    public static JsonElement from(String value){
        return new JsonElement(value);
    }

    public static JsonElement from(int value){
        return new JsonElement(value);
    }

    public static JsonElement from(double value){
        return new JsonElement(value);
    }

    public static JsonElement from(boolean value){
        return new JsonElement(value);
    }

    public static JsonElement from(java.util.Date value){
        return new JsonElement(value);
    }

    public static JsonElement from(JsonObjectable value){
        return value.toJsonElement();
    }

    @Override
    public String toString() {
        return new StringBuilder().append("Element[").append(getTypeString()).append("]: ").append(toJson()).toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof JsonElement)) return false;
        JsonElement that = (JsonElement) o;
        return java.util.Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(value);
    }

    public String getTypeString(){
        if (value instanceof JsonArray){
            return "Array";
        }

        if (value instanceof JsonObject){
            return "Object";
        }

        if (value instanceof Number){
            return "Number";
        }

        if (value instanceof Boolean){
            return "Boolean";
        }

        if (value instanceof String){
            return "String";
        }

        return "none";
    }

    @Override
    public JsonElement toJsonElement() {
        return this;
    }

    @Override
    public String toJson(){
        if (value instanceof JsonArray){
            return ((JsonArray)value).toJson();
        }

        if (value instanceof JsonObject){
            return ((JsonObject)value).toJson();
        }

        if (value instanceof Number){
            return ((Number)value).toString();
        }

        if (value instanceof Boolean){
            return ((Boolean)value).toString();
        }

        if (value instanceof String){
            return "\"" + JsonParser.escape((String)value) + "\"";
        }

        return "null";
    }
}

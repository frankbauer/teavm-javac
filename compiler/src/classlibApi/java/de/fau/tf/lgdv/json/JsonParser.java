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

public class JsonParser {
    private static class IndexedString{
        public int index;
        public final String value;

        public IndexedString(String value){
            this.index = 0;
            this.value = value;
        }

        public char charAt(int index){
            return value.charAt(index);
        }
        public char[] toCharArray(){
            return value.toCharArray();
        }

        public boolean hasNext(){
            return index < value.length();
        }

        public char nextChar() {
            return value.charAt(index++);
        }

        public void goBack(){
            index--;
        }

        public char nextNonWhitespaceChar() {
            if (index >= value.length()) throw new IllegalArgumentException("Unexpected end of JSON input");
            char currentChar = value.charAt(index);
            while (isWhitespace(currentChar)) {
                index++;
                if (index >= value.length()) throw new IllegalArgumentException("Unexpected end of JSON input");
                currentChar = value.charAt(index);
            }
            index++;
            return currentChar;
        }

        public String nextToken(){
            final StringBuilder builder = new StringBuilder();
            if (index >= value.length()) return "";
            char c = nextNonWhitespaceChar();

            while (!isWhitespace(c)){
                builder.append(c);
                if (index >= value.length()) break;
                c = value.charAt(index++);
            }

            return builder.toString();
        }

        @Override
        public String toString() {
            return value.substring(index);
        }
    }
    private static JsonObject parseObject(IndexedString json)  {
        JsonObject object = new JsonObject();
        char c = json.nextNonWhitespaceChar();
        if (c != '{') {
            throw new IllegalArgumentException("Expected '{' at " + json.index + " but got '" + c + "'");
        }

        c = json.nextNonWhitespaceChar();
        while (c != '}') {
            if (c != '\"') {
                throw new IllegalArgumentException("Expected a Key-String at " + json.index + " but got '" + c + "'");
            }

            StringBuilder keyBuilder = new StringBuilder();
            c = json.nextChar();
            while (c != '\"') {
                keyBuilder.append(c);
                if (!json.hasNext()) {
                    throw new IllegalArgumentException("Content ended before finished reading key value '" + keyBuilder.toString() + "'");
                }
                c = json.nextChar();
            }

            final String key = keyBuilder.toString();
            c = json.nextNonWhitespaceChar();
            if (c != ':') {
                throw new IllegalArgumentException("Expected ':' at " + json.index + " but got '" + c + "'");
            }

            final JsonElement value = parseValue(json);
            object.put(key, value);
            c = json.nextNonWhitespaceChar();

            if (c != ',' && c != '}') {
                throw new IllegalArgumentException("Expected ',' or '}' at " + json.index + " but got '" + c + "'");
            }
            if (c==',') {
                c = json.nextNonWhitespaceChar();
            }
        }
        return object;
    }

    private static JsonArray parseArray(IndexedString json)  {
        char c = json.nextNonWhitespaceChar();
        if (c != '[') {
            throw new IllegalArgumentException("Expected '[' at " + json.index + " but got '" + c + "'");
        }

        JsonArray array = new JsonArray();
        while (c!=']'){
            JsonElement value = parseValue(json);
            array.add(value);
            c = json.nextNonWhitespaceChar();
            if (c != ',' && c != ']') {
                throw new IllegalArgumentException("Expected ',' or '}' at " + json.index + " but got '" + c + "'");
            }
        }
        return array;
    }

    private static String parseString(IndexedString json)  {
        char c = json.nextNonWhitespaceChar();
        if (c != '\"') {
            throw new IllegalArgumentException("Expected '\"' at " + json.index + " but got '" + c + "'");
        }

        boolean isEscape = false;
        StringBuilder stringBuilder = new StringBuilder();
        c = json.nextChar();
        while (c != '\"' || isEscape) {
            stringBuilder.append(c);
            if (!json.hasNext()) {
                throw new IllegalArgumentException("Content ended before finished reading key value '" + stringBuilder.toString() + "'");
            }
            if (isEscape){
                isEscape = false;
            } else if (c=='\\' && !isEscape){
                isEscape = true;
            }
            c = json.nextChar();
        }

        return unescapeString(stringBuilder.toString());
    }

    private static Number parseNumber(IndexedString json)  {
        char c = json.nextNonWhitespaceChar();
        boolean isDouble = false;
        StringBuilder stringBuilder = new StringBuilder();

        //sign
        if (c=='-' || c=='+'){
            stringBuilder.append(c);
            c = json.nextChar();
        }

        //digits before decimal point
        while (isDigit(c)){
            stringBuilder.append(c);
            c = json.nextChar();
        }

        //digits after decimal point
        if (c == '.'){
            isDouble = true;
            stringBuilder.append(c);
            c = json.nextChar();
            while (isDigit(c)){
                stringBuilder.append(c);
                c = json.nextChar();
            }
        }

        //exponent
        if (c == 'e' || c=='E'){
            isDouble = true;
            stringBuilder.append(c);
            if (c=='-' || c=='+'){
                stringBuilder.append(c);
                c = json.nextChar();
            }
            while (isDigit(c)){
                stringBuilder.append(c);
                c = json.nextChar();
            }
        }

        json.goBack();
        if (isDouble) return Double.parseDouble(stringBuilder.toString());
        return Integer.parseInt(stringBuilder.toString());
    }

    private static boolean parseBoolean(IndexedString json)  {
        final String bool = json.nextToken();
        if ("true".equals(bool)) return true;
        else if ("false".equals(bool)) return false;
        throw new IllegalArgumentException("Expected boolean value at " + json.index + " but got '" + bool + "'");
    }

    private static Object parseNull(IndexedString json)  {
        final String bool = json.nextToken();
        if ("null".equals(bool)) return null;
        throw new IllegalArgumentException("Expected 'null' value at " + json.index + " but got '" + bool + "'");
    }

    private static JsonElement parseValue(IndexedString json)  {
        char first = json.nextNonWhitespaceChar();
        json.goBack();

        if (first=='{'){
            return new JsonElement(parseObject(json));
        } else if (first=='['){
            return new JsonElement(parseArray(json));
        } else if (first=='"'){
            return new JsonElement(parseString(json));
        } else if (first=='t' || first=='f'){
            return new JsonElement(parseBoolean(json));
        } else if (first=='n'){
            return new JsonElement(parseNull(json));
        } else if (isDigit(first) || first=='-'){
            return new JsonElement(parseNumber(json));
        } else {
            throw new RuntimeException("Invalid JSON " + first);
        }
    }

    /**
     * Parses a valid json string to a JsonElement
     * @param json the json string to parse
     * @return the JsonElement
     */
    public static JsonElement parse(String json)  {
        return parseValue(new IndexedString(json));
    }

    /**
     * Converts a valid json string to a normal string
     * @param input the json string to convert
     * @return the normal string
     */
    public static String unescapeString(String input) {
        return input.replace("\\\"", "\"")
                    .replace("\\\\", "\\")
                    .replace("\\/", "/")
                    .replace("\\b", "\b")
                    .replace("\\f", "\f")
                    .replace("\\n", "\n")
                    .replace("\\r", "\r")
                    .replace("\\t", "\t");
    }

    /**
     * Converts a string to a valid json string
     * @param input the string to convert
     * @return the valid json string
     */
    public static String escape(String input){
        return input.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("/", "\\/")
                    .replace("\b", "\\b")
                    .replace("\f", "\\f")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }

    /**
     * Converts a string to a valid key for a json object
     * @param input the string to convert
     * @return the valid key
     */
    public static String validKey(String input){
        return input.replace("\\", "_")
                    .replace("\"", "_")
                    .replace("/", "_")
                    .replace("\b", "_")
                    .replace("\f", "_")
                    .replace("\n", "_")
                    .replace("\r", "_")
                    .replace("\t", "_");
    }

    /**
     * Converts a date to a string
     * @param date the date to convert
     * @return the string representation of the date
     */
    public static String dateToString(java.util.Date date){
        return ISOTimestampConverter.toISO(date);
    }

    /**
     * Converts a string to a date
     * @param timestamp the string to convert
     * @return the date representation of the string
     */
    public static java.util.Date stringToDate(String timestamp){
        return ISOTimestampConverter.toDate(timestamp);
    }

    private static boolean isDigit(char c) {
        return c=='0' || c=='1' || c=='2' || c=='3' || c=='4' || c=='5' || c=='6' || c=='7' || c=='8' || c=='9';
    }
    private static boolean isWhitespace(char c) {
        return c==' ' || c=='\n' || c=='\r' || c=='\t';
    }
}
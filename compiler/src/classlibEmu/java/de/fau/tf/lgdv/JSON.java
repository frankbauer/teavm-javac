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
package de.fau.tf.lgdv;

import java.util.List;
import java.util.Map;

public class JSON {
    public static interface Stringable {
        String toJSON();
    }

    public static String stringify(Stringable[] list){
        String s = "";
        for (Stringable v : list){
            if (!"".equals(s)) {
                s += ",";
            }
            s+=v.toJSON();
        }
        return "[" + s + "]";
    }

    public static String stringify(Object o){
        if (o instanceof Integer || o instanceof Double || o instanceof Boolean || o instanceof Float || o instanceof Byte || o instanceof Short || o instanceof Long){
            return ""+o;
        }
        if (o instanceof  Stringable){
            Stringable v = (Stringable)o;
            return v.toJSON();
        }
        if (o instanceof List<?>){
            return stringifyList((List<Object>)o);
        }
        if (o instanceof Map<?,?>){
            return stringifyMap((Map<String, Object>)o);
        }

        return "\""+o.toString()+"\"";
    }

    public static String stringify(List<Object> list) {
        return stringifyList(list);
    }
    public static String stringify(Map<String, Object> map) {
        return stringifyMap(map);
    }

    private static String stringifyList(List<Object> list){
        String s = "";
        for (Object o : list){
            if (!"".equals(s)) {
                s += ",";
            }
            s+=stringify(o);
        }
        return "[" + s + "]";
    }

    private static String stringifyMap(Map<String, Object> map){
        String s = "";
        for (Map.Entry<String, Object> e : map.entrySet()){
            if (!"".equals(s)) {
                s += ",";
            }

            s+="\""+e.getKey()+"\":" + stringify(e.getValue());
        }
        return "{" + s + "}";
    }
}

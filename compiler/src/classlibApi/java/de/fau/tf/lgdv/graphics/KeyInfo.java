package de.fau.tf.lgdv.graphics;

import de.fau.tf.lgdv.json.*;

public class KeyInfo implements JsonObjectable {
    public final String key;
    public final String code;
    public final int keyCode;

    public KeyInfo(JsonObject obj) {
        if (obj==null) {
            this.key = "";
            this.code = "";
            this.keyCode = 0;         
        } else {
            this.key = obj.getString("key", "");
            this.code = obj.getString("code", "");
            this.keyCode = obj.getInt("keyCode", 0);
        }
    }

    @Override
    public JsonElement toJsonElement() {
        JsonObject obj = new JsonObject();
        obj.put("key", key);
        obj.put("code", code);
        obj.put("keyCode", keyCode);
        return obj.toJsonElement();
    }
}  
package de.fau.tf.lgdv.graphics;

import de.fau.tf.lgdv.json.*;

public class ModifiersInfo implements JsonObjectable {
    public final boolean ctrl;
    public final boolean alt;
    public final boolean shift;
    public final boolean meta;

    public ModifiersInfo(JsonObject obj) {
        this.ctrl = obj.getBoolean("ctrl", false);
        this.alt = obj.getBoolean("alt", false);
        this.shift = obj.getBoolean("shift", false);
        this.meta = obj.getBoolean("meta", false);
    }

    @Override
    public JsonElement toJsonElement() {
        JsonObject obj = new JsonObject();
        obj.put("ctrl", ctrl);
        obj.put("alt", alt);
        obj.put("shift", shift);
        obj.put("meta", meta);
        return obj.toJsonElement();
    }
}
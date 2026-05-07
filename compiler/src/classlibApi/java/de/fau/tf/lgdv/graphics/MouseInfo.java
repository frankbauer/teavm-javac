package de.fau.tf.lgdv.graphics;

import de.fau.tf.lgdv.json.*;
import de.fau.tf.lgdv.math.Vec2D;

public class MouseInfo implements JsonObjectable {
    public final Vec2D position;
    public final int buttons;
    
    public MouseInfo(JsonObject obj) {
        JsonElement pElement = obj.get("p");
        this.position = new Vec2D(pElement.getObject());
        this.buttons = obj.getInt("b", 0);        
    }

    @Override
    public JsonElement toJsonElement() {
        JsonObject obj = new JsonObject();
        obj.put("p", position.toJsonElement());
        obj.put("b", buttons);
        return obj.toJsonElement();
    }
}
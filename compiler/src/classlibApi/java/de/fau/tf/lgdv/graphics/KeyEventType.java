package de.fau.tf.lgdv.graphics;

import de.fau.tf.lgdv.json.*;

public enum KeyEventType implements JsonObjectable{
    KEY_DOWN("keydown"),
    KEY_UP("keyup"),
    UNKNOWN("unknown");

    private final String eventName;

    KeyEventType(String eventName) {
        this.eventName = eventName;
    }

    public String getEventName() {
        return eventName;
    }

    @Override
    public JsonElement toJsonElement(){
        return JsonElement.from(eventName);
    }

    public static KeyEventType fromJsonElement(JsonElement el) {
        return fromString(el.getString(""));
    }

    public static KeyEventType fromString(String s) {
        for (KeyEventType v : values()) {
            if (v.eventName.equals(s)) return v;
        }
        return UNKNOWN;
    }
}
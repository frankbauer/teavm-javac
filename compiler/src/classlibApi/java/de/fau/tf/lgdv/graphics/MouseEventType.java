package de.fau.tf.lgdv.graphics;

import de.fau.tf.lgdv.json.*;

public enum MouseEventType  implements JsonObjectable{
    MOUSE_MOVE("mousemove"),
    MOUSE_DOWN("mousedown"),
    MOUSE_UP("mouseup"),
    CLICK("click"),
    MOUSE_ENTER("mouseenter"),
    MOUSE_LEAVE("mouseleave"),
    UNKNOWN("unknown");

    private final String eventName;

    MouseEventType(String eventName) {
        this.eventName = eventName;
    }

    public String getEventName() {
        return eventName;
    }

    @Override
    public JsonElement toJsonElement(){
        return JsonElement.from(eventName);
    }

    public static MouseEventType fromJsonElement(JsonElement el) {
        return fromString(el.getString(""));
    }

    public static MouseEventType fromString(String s) {
        for (MouseEventType v : values()) {
            if (v.eventName.equals(s)) return v;
        }
        return UNKNOWN;
    }
}
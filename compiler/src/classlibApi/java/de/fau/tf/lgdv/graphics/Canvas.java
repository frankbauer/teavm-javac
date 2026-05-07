package de.fau.tf.lgdv.graphics;

import de.fau.tf.lgdv.CodeBlocks;
import de.fau.tf.lgdv.runtime.annotations.*;
import de.fau.tf.lgdv.json.*;
import java.util.List;
import java.util.ArrayList;
import de.fau.tf.lgdv.math.Vec2D;
import de.fau.tf.lgdv.math.Int2D;

public class Canvas {
    @JSQuery
    private static native JsonElement getScreenSize();

    public static Int2D getScreenDimensions() {
        JsonElement el = getScreenSize();
        if (el != null && el.isObject()) {
            JsonObject obj = el.getObject();
            return new Int2D(obj.getInt("width", 0), obj.getInt("height", 0));
        }
        return new Int2D(0, 0);
    }

    private static List<MouseEvent> mouseEventListeners = new ArrayList<>();
    private static List<KeyEvent> keyEventListeners = new ArrayList<>();
    private static List<TickEvent> tickEventListeners = new ArrayList<>();

    public static void addMouseEventListener(MouseEvent listener) {
        mouseEventListeners.add(listener);
    }

    public static void addKeyEventListener(KeyEvent listener) {
        keyEventListeners.add(listener);
    }

    public static void addTickEventListener(TickEvent listener) {
        tickEventListeners.add(listener);
    }

    @JSCommand
    public static native void enableTicks();

    @JSCommand
    public static native void disableTicks();

    @JSCommand
    public static native void setTickMode(boolean enabled);

    @JSCommand
    public static native void clear();

    public static void clear(Color color) {
        CodeBlocks.postMessage("clear", color.toRgbaString());
    }

    public static void setInputEventEnabled(MouseEventType type, boolean enabled) {
        CodeBlocks.postMessage(enabled ? "enableInputEvent" : "disableInputEvent", type.getEventName());
    }

    public static void setInputEventEnabled(KeyEventType type, boolean enabled) {
        CodeBlocks.postMessage(enabled ? "enableInputEvent" : "disableInputEvent", type.getEventName());
    }

    @JSCommand
    public static native void setStrokeStyle(String style);
    public static void setStrokeStyle(Color color) { setStrokeStyle(color.toRgbaString()); }

    @JSCommand
    public static native void setFillStyle(String style);
    public static void setFillStyle(Color color) { setFillStyle(color.toRgbaString()); }

    @JSCommand
    public static native void setLineWidth(double width);

    @JSCommand
    public static native void setFont(String font);

    @JSCommand
    public static native void setTextAlign(String align);

    @JSCommand
    public static native void beginPath();

    @JSCommand
    public static native void closePath();

    @JSCommand
    public static native void stroke();

    @JSCommand
    public static native void fill();

    @JSCommand
    public static native void moveTo(double x, double y);

    @JSCommand
    public static native void lineTo(double x, double y);

    @JSCommand
    public static native void fillRect(double x, double y, double w, double h);

    @JSCommand
    public static native void strokeRect(double x, double y, double w, double h);

    @JSCommand
    public static native void clearRect(double x, double y, double w, double h);

    @JSCommand
    public static native void arc(double x, double y, double radius, double startAngle, double endAngle, boolean anticlockwise);

    @JSCommand
    public static native void fillText(String text, double x, double y);

    @JSCommand
    public static native void strokeText(String text, double x, double y);

    @JSCommand
    public static native void save();

    @JSCommand
    public static native void restore();

    @JSCommand
    public static native void translate(double x, double y);

    @JSCommand
    public static native void rotate(double angle);

    @JSCommand
    public static native void scale(double x, double y);

    public static void line(double x1, double y1, double x2, double y2) {
        beginPath();
        moveTo(x1, y1);
        lineTo(x2, y2);
        stroke();
    }

    public static void rectangle(double x1, double y1, double x2, double y2, boolean fill) {
        double x = Math.min(x1, x2);
        double y = Math.min(y1, y2);
        double w = Math.abs(x2 - x1);
        double h = Math.abs(y2 - y1);
        if (fill) fillRect(x, y, w, h);
        else strokeRect(x, y, w, h);
    }

    public static void circle(double x, double y, double radius, boolean fill) {
        beginPath();
        arc(x, y, radius, 0, Math.PI * 2, false);
        if (fill) fill();
        else stroke();
    }

    public static void circle(double x1, double y1, double x2, double y2, boolean fill) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double radius = Math.sqrt(dx * dx + dy * dy);
        circle(x1, y1, radius, fill);
    }

    public static void drawImage(Image img, Vec2D position) {
        img.draw( position, 1.0);
    }

    public static void drawImage(Image img, Vec2D position, Vec2D anchor) {
        img.draw( position, 1.0, anchor);
    }

    public static void drawImage(Image img, Vec2D position, double scale) {
        img.draw( position, scale);
    }

    public static void drawImage(Image img, Vec2D position, double scale, Vec2D anchor) {
        img.draw( position, scale, anchor);
    }

    public static void drawImage(Image img, Vec2D position, Int2D size) {
        img.draw(position, size);
    }

    @JSEvent("tick")
    private static void onTick(double time, double delta) {
        tickEventListeners.forEach(listener -> listener.onTick(time, delta));
    }

    @JSEvent("input")
    private static void onInput(String t, MouseInfo m, ModifiersInfo d, KeyInfo k) {
        if (t.startsWith("key")) {
            KeyEventType type = KeyEventType.fromString(t);            
            if (type != null && type != KeyEventType.UNKNOWN) {
                KeyEventType finalType = type;
                String key = new String(k.key);
                String code = new String(k.code);
                int keyCode = k.keyCode;
                keyEventListeners.forEach(l -> l.onKeyEvent(finalType, k, d, m));
            }
        } else {
            MouseEventType type = MouseEventType.fromString(t);
            if (type != null && type != MouseEventType.UNKNOWN) {
                MouseEventType finalType = type;
                mouseEventListeners.forEach(l -> l.onMouseEvent(finalType, m, d));
            }
        }
    }
}

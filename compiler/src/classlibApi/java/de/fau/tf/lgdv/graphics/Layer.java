package de.fau.tf.lgdv.graphics;

import de.fau.tf.lgdv.runtime.RemoteObject;
import de.fau.tf.lgdv.runtime.annotations.JSCommand;
import de.fau.tf.lgdv.json.JsonObject;
import de.fau.tf.lgdv.math.Vec2D;

public class Layer extends RemoteObject {
    public enum BorderType {
        NONE,
        SOLID,
        DOTTED,
        DASHED,
        DOUBLE
    }

    public enum TextAlign {
        LEFT,
        CENTER,
        RIGHT,
        JUSTIFY
    }

    public enum TextVerticalAlign {
        TOP,
        MIDDLE,
        BOTTOM
    }

    public enum FontWeight {
        THIN,
        LIGHT,
        NORMAL,
        MEDIUM,
        BOLD,
        EXTRA_BOLD
    }

    public enum FontFamily {
        SANS_SERIF,
        SERIF,
        MONOSPACE,
        ARIAL,
        GEORGIA,
        TIMES_NEW_ROMAN,
        COURIER_NEW,
        VERDANA,
        TREBUCHET_MS,
        IMPACT,
        ROBOTO,
        HELVETICA
    }

    public final String text;
    public final String className;
    public final Layer parent;

    private static Layer requireParent(Layer parent) {
        if (parent == null) {
            throw new IllegalArgumentException("parent must not be null");
        }
        return parent;
    }

    public Layer() {
        this(null, null, null);
    }

    public Layer(String text) {
        this(text, null, null);
    }

    public Layer(Layer parent) {
        this(null, null, requireParent(parent));
    }

    public Layer(String text, String className) {
        this(text, className, null);
    }

    public Layer(String text, Layer parent) {
        this(text, null, requireParent(parent));
    }

    public Layer(String text, String className, Layer parent) {
        super("LAYER");
        this.text = text;
        this.className = className;
        this.parent = parent;
        this.sendNew();
        this.waitForReady();
    }

    public boolean isReady() {
        return this.didReceiveReady();
    }

    private void waitForReady() {
        super.waitForCreated();
    }

    @Override
    protected void addAttributes(JsonObject json) {
        if (text != null) {
            json.put("text", text);
        }
        if (className != null) {
            json.put("className", className);
        }
        if (parent != null) {
            json.put("parent", parent.toJsonReference());
        }
    }

    @JSCommand(params = {"position"})
    public native void setPosition(Vec2D position);

    @JSCommand(params = {"size"})
    public native void setSize(Vec2D size);

    @JSCommand(params = {"visible"})
    public native void setVisible(boolean visible);

    @JSCommand(params = {"opacity"})
    public native void setOpacity(double opacity);

    @JSCommand(value = "setBackgroundColor", params = {"color"})
    public native void setBackground(Color color);

    @JSCommand(value = "setBorderWidth", params = {"borderWidth"})
    public native void setBorderWidth(int borderWidth);

    @JSCommand(value = "setBorderColor", params = {"borderColor"})
    public native void setBorderColor(Color borderColor);

    @JSCommand(value = "setBorderType", params = {"borderType"})
    public native void setBorderType(BorderType borderType);

    @JSCommand(value = "setShadow", params = {"offsetX", "offsetY", "blurRadius", "spreadRadius", "color"})
    public native void setShadow(int offsetX, int offsetY, int blurRadius, int spreadRadius, Color color);

    @JSCommand(params = {"roundness"})
    public native void setRoundness(double roundness);

    @JSCommand(params = {"zIndex"})
    public native void setZIndex(int zIndex);

    @JSCommand(params = {"text"})
    public native void setText(String text);

    @JSCommand(params = {"markdown"})
    public native void setMarkdown(String markdown);

    @JSCommand(value = "setTextAlign", params = {"align"})
    public native void setTextAlign(TextAlign align);

    @JSCommand(value = "setTextVerticalAlign", params = {"align"})
    public native void setTextVerticalAlign(TextVerticalAlign align);

    @JSCommand(value = "setTextColor", params = {"color"})
    public native void setTextColor(Color color);

    @JSCommand(value = "setFontSize", params = {"size"})
    public native void setFontSize(int size);

    @JSCommand(value = "setFontFamily", params = {"family"})
    public native void setFontFamily(FontFamily family);

    @JSCommand(value = "setFontWeight", params = {"weight"})
    public native void setFontWeight(FontWeight weight);

    @JSCommand(value = "setTextShadow", params = {"offsetX", "offsetY", "blurRadius", "color"})
    public native void setTextShadow(int offsetX, int offsetY, int blurRadius, Color color);

    public void setBackground(Image image) {
        JsonObject payload = new JsonObject();
        if (image != null) {
            payload.put("image", image.toJsonReference());
        }
        this.sendCommand("setBackgroundImage", payload);
    }
}

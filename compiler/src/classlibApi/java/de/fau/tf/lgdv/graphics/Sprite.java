package de.fau.tf.lgdv.graphics;

import de.fau.tf.lgdv.runtime.annotations.*;
import de.fau.tf.lgdv.runtime.RemoteObject;
import de.fau.tf.lgdv.json.*;
import de.fau.tf.lgdv.math.Vec2D;

/**
 * An animated sprite whose frames are cut from a sprite sheet ({@link Image}).
 *
 * <p>Frames are numbered row by row (left to right, top to bottom) across the whole sheet. A sprite
 * uses the frames {@code firstFrame ... firstFrame + frameCount - 1} of that sheet, so several
 * sprites can share one sheet (e.g. one animation per row).</p>
 *
 * <p>The animation runs on the JavaScript side: after {@link #play()} the sprite advances on its own
 * with the given frames per second, without any further messages from Java.</p>
 *
 * <p>A sprite can be used in two ways:</p>
 * <ul>
 *   <li><b>Retained:</b> {@link #show(Vec2D, double, Vec2D)} places the sprite on the sprite layer
 *   above the canvas. It is then redrawn automatically in every animation frame (sorted by
 *   {@link #setDepth(double) depth}) until {@link #hide()} is called. Anything drawn with
 *   {@link Canvas} stays below the sprite layer.</li>
 *   <li><b>Immediate:</b> {@link #draw(Vec2D, double, Vec2D)} draws the current frame onto the
 *   canvas once, like {@link Image#draw(Vec2D, double, Vec2D)}.</li>
 * </ul>
 */
public class Sprite extends RemoteObject {
    public interface OnEnded { void onEnded(Sprite sprite); }

    /** Pass as {@code fromFrame} to {@link #play(double, int, boolean)} to continue at the current frame. */
    public static final int CURRENT_FRAME = -1;

    public final Image image;
    public final int frameWidth;
    public final int frameHeight;
    public final int firstFrame;
    public final int frameCount;
    private double fps;
    private boolean loop;
    private OnEnded onEndedCallback;

    /** A sprite that uses all frames of the sheet, 12 frames per second, looping. */
    public Sprite(Image image, int frameWidth, int frameHeight) {
        this(image, frameWidth, frameHeight, 0, -1, 12, true);
    }

    /** A looping sprite that uses {@code frameCount} frames starting at {@code firstFrame}. */
    public Sprite(Image image, int frameWidth, int frameHeight, int firstFrame, int frameCount, double fps) {
        this(image, frameWidth, frameHeight, firstFrame, frameCount, fps, true);
    }

    /**
     * @param image       the sprite sheet
     * @param frameWidth  width of one frame in pixels
     * @param frameHeight height of one frame in pixels
     * @param firstFrame  index of the first frame of the animation in the sheet
     * @param frameCount  number of frames of the animation ({@code -1}: all remaining frames of the sheet)
     * @param fps         frames per second
     * @param loop        {@code true}: start over after the last frame, {@code false}: stop at the last frame
     */
    public Sprite(Image image, int frameWidth, int frameHeight, int firstFrame, int frameCount, double fps, boolean loop) {
        super("SPRITE");
        if (image == null) {
            throw new IllegalArgumentException("image must not be null");
        }
        if (frameWidth <= 0 || frameHeight <= 0) {
            throw new IllegalArgumentException("frameWidth and frameHeight must be positive");
        }
        this.image = image;
        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;
        this.firstFrame = Math.max(0, firstFrame);
        this.frameCount = frameCount;
        this.fps = fps;
        this.loop = loop;
        this.sendNew();
        this.waitForCreated();
    }

    @Override
    protected void addAttributes(JsonObject json) {
        json.put("image", image.ID)
            .put("frameWidth", frameWidth)
            .put("frameHeight", frameHeight)
            .put("firstFrame", firstFrame)
            .put("frameCount", frameCount)
            .put("fps", fps)
            .put("loop", loop);
    }

    public double getFps() { return fps; }
    public boolean isLooping() { return loop; }

    /** Called (on the Java side) when a non-looping animation reached its last (or, in reverse, first) frame. */
    public void setOnEnded(OnEnded callback) { this.onEndedCallback = callback; }

    @JSEvent("ended")
    public void onEndedEvent(JsonElement json) {
        if (this.onEndedCallback != null) {
            this.onEndedCallback.onEnded(this);
        }
    }

    // ---------------------------------------------------------------- animation

    /** Plays forward from the current frame with the configured fps. */
    public void play() { play(fps, CURRENT_FRAME, false); }

    /** Plays from the current frame, forward or in reverse. */
    public void play(boolean reverse) { play(fps, CURRENT_FRAME, reverse); }

    /** Plays from the current frame with the given fps. */
    public void play(double fps, boolean reverse) { play(fps, CURRENT_FRAME, reverse); }

    /**
     * Starts the animation.
     *
     * @param fps       frames per second (also becomes the new default for {@link #play()})
     * @param fromFrame frame to start at (relative to {@code firstFrame}), or {@link #CURRENT_FRAME}
     * @param reverse   {@code true} plays the frames backwards
     */
    public void play(double fps, int fromFrame, boolean reverse) {
        this.fps = fps;
        sendPlay(fps, fromFrame, reverse);
    }

    @JSCommand(value = "play", params = {"fps", "fromFrame", "reverse"})
    private native void sendPlay(double fps, int fromFrame, boolean reverse);

    /** Stops the animation at the current frame. {@link #play()} continues from there. */
    @JSCommand(value = "pause")
    public native void pause();

    /** Stops the animation and goes back to the first frame. */
    @JSCommand(value = "stop")
    public native void stop();

    /** Shows the given frame (relative to {@code firstFrame}) without changing whether the animation plays. */
    @JSCommand(value = "setFrame", params = {"frame"})
    public native void setFrame(int frame);

    public void setLoop(boolean loop) {
        this.loop = loop;
        sendLoop(loop);
    }

    @JSCommand(value = "setLoop", params = {"loop"})
    private native void sendLoop(boolean loop);

    // ---------------------------------------------------------------- retained drawing (sprite layer)

    /** Places the sprite on the sprite layer at {@code position} (anchor top-left, scale 1). */
    public void show(Vec2D position) { show(position, 1.0, new Vec2D(0, 0)); }

    /**
     * Places the sprite on the sprite layer. It is redrawn in every animation frame until {@link #hide()}.
     *
     * @param position position in canvas coordinates (CSS pixels)
     * @param scale    scale factor for the frame size
     * @param anchor   normalized anchor inside the frame (0/0 top-left, 0.5/0.5 center, 1/1 bottom-right)
     */
    @JSCommand(value = "show", params = {"position", "scale", "anchor"})
    public native void show(Vec2D position, double scale, Vec2D anchor);

    /** Removes the sprite from the sprite layer (the animation keeps its state). */
    @JSCommand(value = "hide")
    public native void hide();

    @JSCommand(value = "setPosition", params = {"position"})
    public native void setPosition(Vec2D position);

    @JSCommand(value = "setScale", params = {"scale"})
    public native void setScale(double scale);

    @JSCommand(value = "setAnchor", params = {"anchor"})
    public native void setAnchor(Vec2D anchor);

    /** Sprites with a larger depth are drawn in front of sprites with a smaller depth (default 0). */
    @JSCommand(value = "setDepth", params = {"depth"})
    public native void setDepth(double depth);

    // ---------------------------------------------------------------- immediate drawing (canvas)

    /** Draws the current frame onto the canvas once (buffered like all canvas commands in tick mode). */
    @JSCommand(value = "draw", params = {"position", "scale", "anchor"})
    public native void draw(Vec2D position, double scale, Vec2D anchor);

    public void draw(Vec2D position) { draw(position, 1.0, new Vec2D(0, 0)); }
}

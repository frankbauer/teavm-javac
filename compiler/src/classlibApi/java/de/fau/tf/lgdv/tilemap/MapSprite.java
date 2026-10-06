package de.fau.tf.lgdv.tilemap;

import de.fau.tf.lgdv.json.JsonElement;
import de.fau.tf.lgdv.json.JsonObject;
import de.fau.tf.lgdv.runtime.annotations.JSEvent;

/**
 * A static or animated object on a {@link TileMap}, e.g. a castle, a tree, a volcano or a flashing
 * level point (see {@link SpriteType}).
 *
 * <p>The sprite covers {@code type.columns x type.rows} cells; (column, row) is its top left cell.
 * Animated sprites start their default animation automatically.</p>
 */
public class MapSprite extends MapObject {
    public interface OnAnimationEnded { void onAnimationEnded(MapSprite sprite, String animation); }

    public final TileMap map;
    public final SpriteType type;
    private int column;
    private int row;
    private boolean visible = true;
    private OnAnimationEnded onAnimationEnded;

    public MapSprite(TileMap map, SpriteType type, int column, int row) {
        super("MAPSPRITE");
        if (map == null || type == null) {
            throw new IllegalArgumentException("map and type must not be null");
        }
        this.map = map;
        this.type = type;
        this.column = column;
        this.row = row;
        publish();
    }

    @Override
    protected void addAttributes(JsonObject json) {
        json.put("map", map.ID)
            .put("sprite", type.name())
            .put("col", column)
            .put("row", row);
    }

    public int getColumn() { return column; }
    public int getRow() { return row; }
    public boolean isVisible() { return visible; }

    /** Moves the sprite to another cell immediately. */
    public void setPosition(int column, int row) {
        this.column = column;
        this.row = row;
        send("setPosition", TileMap.cell(column, row));
    }

    /** Glides the sprite to another cell within {@code seconds}. */
    public void moveTo(int column, int row, double seconds) {
        this.column = column;
        this.row = row;
        send("moveTo", TileMap.cell(column, row).put("seconds", seconds));
    }

    /** Plays the default animation of the sprite. */
    public void play() {
        send("play", new JsonObject());
    }

    /** Plays an animation (see {@link SpriteType}) with its default looping. */
    public void play(String animation) {
        send("play", new JsonObject().put("animation", animation));
    }

    /** Plays an animation once ({@code loop = false}) or repeatedly. */
    public void play(String animation, boolean loop) {
        send("play", new JsonObject().put("animation", animation).put("loop", loop));
    }

    /** Stops the animation at the current frame. */
    public void stop() {
        send("stop");
    }

    /** Shows a frame of the sprite sheet (0 = first frame) and stops the animation. */
    public void setFrame(int frame) {
        send("setFrame", new JsonObject().put("frame", frame));
    }

    public void show() {
        visible = true;
        send("setVisible", new JsonObject().put("visible", true));
    }

    public void hide() {
        visible = false;
        send("setVisible", new JsonObject().put("visible", false));
    }

    /** Sprites with a larger depth are drawn in front of others in the same row (default 0). */
    public void setDepth(int depth) {
        send("setDepth", new JsonObject().put("depth", depth));
    }

    /** Removes the sprite from the map. */
    public void remove() {
        send("remove");
        markRemoved();
    }

    /** Called when an animation that does not loop reached its last frame (RPC mode only). */
    public void setOnAnimationEnded(OnAnimationEnded callback) {
        this.onAnimationEnded = callback;
    }

    @JSEvent("ended")
    private void onEndedEvent(JsonElement json) {
        if (onAnimationEnded != null) {
            onAnimationEnded.onAnimationEnded(this, json.getObject().getString("animation", ""));
        }
    }
}

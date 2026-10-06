package de.fau.tf.lgdv.tilemap;

import de.fau.tf.lgdv.json.JsonArray;
import de.fau.tf.lgdv.json.JsonElement;
import de.fau.tf.lgdv.json.JsonObject;
import de.fau.tf.lgdv.runtime.annotations.JSEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * A character that moves from cell to cell on a {@link TileMap}, e.g. the {@link CharacterType#HERO}
 * on land or the {@link CharacterType#SHIP} on water.
 *
 * <p>Moves are queued: every call adds steps that the playground animates one after the other,
 * the method returns immediately. {@link #getColumn()} / {@link #getRow()} already return the
 * cell the character will reach after all queued steps. A move into a cell the character cannot
 * enter (wrong terrain, blocked or outside of the map) is not queued; the character only turns
 * into that direction.</p>
 *
 * <pre>
 * MapCharacter hero = new MapCharacter(map, CharacterType.HERO, 2, 3);
 * hero.moveRight();
 * hero.moveNE();
 * hero.goTo(7, 1);
 * </pre>
 */
public class MapCharacter extends MapObject {
    public interface OnStep { void onStep(MapCharacter character, int column, int row); }
    public interface OnIdle { void onIdle(MapCharacter character); }

    public final TileMap map;
    public final CharacterType type;
    private int column;
    private int row;
    private Direction direction = Direction.S;
    private double speed;
    private int pendingSteps;
    private OnStep onStep;
    private OnIdle onIdle;

    public MapCharacter(TileMap map, CharacterType type, int column, int row) {
        super("MAPCHARACTER");
        if (map == null || type == null) {
            throw new IllegalArgumentException("map and type must not be null");
        }
        map.checkInside(column, row);
        this.map = map;
        this.type = type;
        this.column = column;
        this.row = row;
        this.speed = type.speed;
        publish();
    }

    @Override
    protected void addAttributes(JsonObject json) {
        json.put("map", map.ID)
            .put("character", type.name())
            .put("col", column)
            .put("row", row)
            .put("dir", direction.name())
            .put("speed", speed);
    }

    /** Column the character reaches after all queued moves. */
    public int getColumn() { return column; }

    /** Row the character reaches after all queued moves. */
    public int getRow() { return row; }

    /** Direction the character looks at after all queued moves. */
    public Direction getDirection() { return direction; }

    public double getSpeed() { return speed; }

    /** Speed in tiles per second for the following moves. */
    public void setSpeed(double tilesPerSecond) {
        this.speed = Math.max(0.1, tilesPerSecond);
        send("setSpeed", new JsonObject().put("speed", speed));
    }

    // ---------------------------------------------------------------- moving

    /** {@code true} if the character can make a step into the given direction from where it is now. */
    public boolean canMove(Direction dir) {
        return canStep(column, row, dir);
    }

    /** Queues one step into the given direction. Returns {@code false} if the step is not possible. */
    public boolean move(Direction dir) {
        return move(dir, 1) == 1;
    }

    /** Queues up to {@code steps} steps into one direction, stops in front of an obstacle. */
    public int move(Direction dir, int steps) {
        List<int[]> path = new ArrayList<>();
        int c = column, r = row;
        for (int i = 0; i < steps && canStep(c, r, dir); i++) {
            c += dir.dx;
            r += dir.dy;
            path.add(new int[] {c, r});
        }
        if (path.isEmpty()) {
            face(dir);
            return 0;
        }
        walk(path);
        return path.size();
    }

    public boolean moveUp() { return move(Direction.N); }
    public boolean moveDown() { return move(Direction.S); }
    public boolean moveLeft() { return move(Direction.W); }
    public boolean moveRight() { return move(Direction.E); }
    public boolean moveNE() { return move(Direction.NE); }
    public boolean moveSE() { return move(Direction.SE); }
    public boolean moveSW() { return move(Direction.SW); }
    public boolean moveNW() { return move(Direction.NW); }

    /**
     * Walks to a cell along the shortest path (in steps, all eight directions) around obstacles.
     * Returns {@code false} and does not move if the cell cannot be reached.
     */
    public boolean goTo(int targetColumn, int targetRow) {
        if (targetColumn == column && targetRow == row) {
            return true;
        }
        if (!map.canEnter(type, targetColumn, targetRow)) {
            return false;
        }
        int w = map.columns, h = map.rows;
        int[] from = new int[w * h];
        java.util.Arrays.fill(from, -1);
        int start = row * w + column, goal = targetRow * w + targetColumn;
        int[] queue = new int[w * h];
        int head = 0, tail = 0;
        queue[tail++] = start;
        from[start] = start;
        // straight steps first, so paths prefer straight lines over zig-zags
        Direction[] order = {Direction.N, Direction.E, Direction.S, Direction.W,
                             Direction.NE, Direction.SE, Direction.SW, Direction.NW};
        while (head < tail && from[goal] < 0) {
            int cur = queue[head++];
            int c = cur % w, r = cur / w;
            for (Direction d : order) {
                if (!canStep(c, r, d)) continue;
                int next = (r + d.dy) * w + (c + d.dx);
                if (from[next] >= 0) continue;
                from[next] = cur;
                queue[tail++] = next;
            }
        }
        if (from[goal] < 0) {
            return false;
        }
        List<int[]> path = new ArrayList<>();
        for (int cur = goal; cur != start; cur = from[cur]) {
            path.add(0, new int[] {cur % w, cur / w});
        }
        walk(path);
        return true;
    }

    /** Turns the character into a direction without moving. */
    public void face(Direction dir) {
        direction = dir;
        send("face", new JsonObject().put("dir", dir.name()));
    }

    /** Waits before the following queued moves are executed. */
    public void pause(double seconds) {
        send("pause", new JsonObject().put("seconds", seconds));
    }

    /** Puts the character onto a cell without walking (after the queued moves). */
    public void teleport(int column, int row) {
        map.checkInside(column, row);
        this.column = column;
        this.row = row;
        send("teleport", TileMap.cell(column, row));
    }

    /** Cancels all queued moves that have not started yet (RPC mode). */
    public void stopMoving() {
        send("cancel");
    }

    public void show() { send("setVisible", new JsonObject().put("visible", true)); }
    public void hide() { send("setVisible", new JsonObject().put("visible", false)); }

    /** Removes the character from the map. */
    public void remove() {
        send("remove");
        markRemoved();
    }

    // ---------------------------------------------------------------- state & events

    /**
     * {@code true} while queued steps are still being animated. Only known in RPC mode, in
     * command queue mode it is always {@code false}.
     */
    public boolean isMoving() {
        return pendingSteps > 0;
    }

    /** Blocks until all queued moves are done (RPC mode, does nothing in command queue mode). */
    public void waitUntilIdle() {
        if (!queued && !isRemoved()) {
            sendQuery("whenIdle", new JsonObject());
            pendingSteps = 0;
        }
    }

    /** Called after every finished step with the new cell (RPC mode only). */
    public void setOnStep(OnStep callback) { this.onStep = callback; }

    /** Called when all queued moves are done (RPC mode only). */
    public void setOnIdle(OnIdle callback) { this.onIdle = callback; }

    @JSEvent("step")
    private void onStepEvent(JsonElement json) {
        if (pendingSteps > 0) pendingSteps--;
        if (onStep != null) {
            JsonObject o = json.getObject();
            onStep.onStep(this, o.getInt("col", column), o.getInt("row", row));
        }
    }

    @JSEvent("idle")
    private void onIdleEvent(JsonElement json) {
        pendingSteps = 0;
        if (onIdle != null) {
            onIdle.onIdle(this);
        }
    }

    // ---------------------------------------------------------------- helpers

    private boolean canStep(int c, int r, Direction d) {
        int nc = c + d.dx, nr = r + d.dy;
        if (!map.canEnter(type, nc, nr)) {
            return false;
        }
        // no diagonal steps across a corner the character cannot enter
        return !d.isDiagonal() || (map.canEnter(type, nc, r) && map.canEnter(type, c, nr));
    }

    private void walk(List<int[]> path) {
        JsonArray steps = new JsonArray();
        int c = column, r = row;
        for (int[] p : path) {
            Direction d = Direction.of(p[0] - c, p[1] - r);
            steps.add(new JsonObject().put("col", p[0]).put("row", p[1]).put("dir", d.name()));
            c = p[0];
            r = p[1];
            direction = d;
        }
        column = c;
        row = r;
        if (!queued) {
            pendingSteps += path.size();
        }
        send("walk", new JsonObject().put("steps", steps));
    }
}

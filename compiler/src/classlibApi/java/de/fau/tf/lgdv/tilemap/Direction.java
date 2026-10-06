package de.fau.tf.lgdv.tilemap;

/** The eight directions a {@link MapCharacter} can move in. North is up (row - 1). */
public enum Direction {
    N(0, -1), NE(1, -1), E(1, 0), SE(1, 1), S(0, 1), SW(-1, 1), W(-1, 0), NW(-1, -1);

    /** Synonyms for the four main directions. */
    public static final Direction UP = N, DOWN = S, LEFT = W, RIGHT = E;

    /** Change of the column / row for one step. */
    public final int dx, dy;

    Direction(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    /** {@code true} for NE, SE, SW and NW. */
    public boolean isDiagonal() {
        return dx != 0 && dy != 0;
    }

    /** The direction for a step by (dx, dy), each -1, 0 or 1. {@code null} for (0, 0). */
    public static Direction of(int dx, int dy) {
        dx = Integer.signum(dx);
        dy = Integer.signum(dy);
        for (Direction d : values()) {
            if (d.dx == dx && d.dy == dy) {
                return d;
            }
        }
        return null;
    }
}

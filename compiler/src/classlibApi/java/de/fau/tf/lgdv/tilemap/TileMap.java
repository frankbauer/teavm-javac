package de.fau.tf.lgdv.tilemap;

import de.fau.tf.lgdv.json.JsonArray;
import de.fau.tf.lgdv.json.JsonElement;
import de.fau.tf.lgdv.json.JsonObject;
import de.fau.tf.lgdv.runtime.annotations.JSEvent;

/**
 * A 2D pixel art map of 16 x 16 px tiles, drawn by the {@code tileMap} playground library.
 *
 * <p>The map has three layers that are set with 2D arrays of numeric constants. The arrays are
 * indexed {@code [row][column]}, so they look like the map in the source code:</p>
 * <ul>
 *   <li><b>terrain</b> ({@link Terrain}): land and water. {@link Terrain#WATER} gets its coast
 *   automatically (auto tiling).</li>
 *   <li><b>decorations</b> ({@link Decoration}): trees, houses, paths, routes, ... on top of the
 *   terrain. {@link Decoration#PATH} connects automatically.</li>
 *   <li><b>fog</b> ({@link #FOG_LIGHT}, {@link #FOG_MEDIUM}, {@link #FOG_DENSE}): clouds above
 *   everything else, e.g. to hide unexplored parts of the map.</li>
 * </ul>
 * {@link MapSprite}s and {@link MapCharacter}s are placed on top of the decorations.
 *
 * <pre>
 * int L = Terrain.LAND, W = Terrain.WATER;
 * TileMap map = new TileMap(Theme.VALLEY, new int[][] {
 *     {W, W, W, W},
 *     {W, L, L, W},
 *     {W, W, W, W},
 * });
 * </pre>
 */
public class TileMap extends MapObject {
    public interface OnTileClicked { void onTileClicked(TileMap map, int column, int row); }

    public static final int FOG_NONE = 0;
    public static final int FOG_LIGHT = 1;
    public static final int FOG_MEDIUM = 2;
    public static final int FOG_DENSE = 3;

    /** Pass to {@link #setZoom(double)} to fit the whole map into the playground (default). */
    public static final double ZOOM_FIT = 0;

    public final int columns;
    public final int rows;
    private Theme theme;
    private final int[][] terrain;
    private final int[][] decorations;
    private final int[][] fog;
    private final boolean[][] blocked;
    private boolean autoTiling = true;
    private double zoom = ZOOM_FIT;
    private OnTileClicked onTileClicked;

    /** A map of the given size, filled with water. */
    public TileMap(Theme theme, int columns, int rows) {
        this(theme, filled(columns, rows, Terrain.WATER));
    }

    /** A map with the valley theme. */
    public TileMap(int[][] terrain) {
        this(Theme.VALLEY, terrain);
    }

    /**
     * @param theme   look of the map
     * @param terrain terrain tiles ({@link Terrain}), indexed {@code [row][column]}. All rows must
     *                have the same length.
     */
    public TileMap(Theme theme, int[][] terrain) {
        super("TILEMAP");
        if (terrain == null || terrain.length == 0 || terrain[0].length == 0) {
            throw new IllegalArgumentException("terrain must have at least one row and one column");
        }
        this.theme = theme == null ? Theme.VALLEY : theme;
        this.rows = terrain.length;
        this.columns = terrain[0].length;
        this.terrain = new int[rows][columns];
        this.decorations = new int[rows][columns];
        this.fog = new int[rows][columns];
        this.blocked = new boolean[rows][columns];
        copyInto(this.terrain, terrain, "terrain");
        publish();
    }

    @Override
    protected void addAttributes(JsonObject json) {
        json.put("theme", theme.id())
            .put("columns", columns)
            .put("rows", rows)
            .put("terrain", flatten(terrain))
            .put("decorations", flatten(decorations))
            .put("fog", flatten(fog))
            .put("autoTiling", autoTiling)
            .put("zoom", zoom);
    }

    // ---------------------------------------------------------------- terrain

    public int getTerrain(int column, int row) {
        checkInside(column, row);
        return terrain[row][column];
    }

    public void setTerrain(int column, int row, int tile) {
        checkInside(column, row);
        terrain[row][column] = tile;
        send("setTerrain", cell(column, row).put("tile", tile));
    }

    /** Replaces the whole terrain. The array must have the size of the map. */
    public void setTerrain(int[][] tiles) {
        copyInto(terrain, tiles, "terrain");
        send("setTerrainAll", new JsonObject().put("tiles", flatten(terrain)));
    }

    /** Fills a rectangle (inclusive corners) with one terrain tile. */
    public void fillTerrain(int column1, int row1, int column2, int row2, int tile) {
        for (int r = Math.max(0, Math.min(row1, row2)); r <= Math.min(rows - 1, Math.max(row1, row2)); r++) {
            for (int c = Math.max(0, Math.min(column1, column2)); c <= Math.min(columns - 1, Math.max(column1, column2)); c++) {
                terrain[r][c] = tile;
            }
        }
        send("setTerrainAll", new JsonObject().put("tiles", flatten(terrain)));
    }

    // ---------------------------------------------------------------- decorations

    public int getDecoration(int column, int row) {
        checkInside(column, row);
        return decorations[row][column];
    }

    /** Places a decoration tile ({@link Decoration}), {@link Decoration#NONE} removes it. */
    public void setDecoration(int column, int row, int tile) {
        checkInside(column, row);
        decorations[row][column] = tile;
        send("setDecoration", cell(column, row).put("tile", tile));
    }

    /** Replaces all decorations. The array must have the size of the map. */
    public void setDecorations(int[][] tiles) {
        copyInto(decorations, tiles, "decorations");
        send("setDecorationsAll", new JsonObject().put("tiles", flatten(decorations)));
    }

    // ---------------------------------------------------------------- fog

    public int getFog(int column, int row) {
        checkInside(column, row);
        return fog[row][column];
    }

    /** Sets the fog level ({@link #FOG_NONE} ... {@link #FOG_DENSE}) of one cell. */
    public void setFog(int column, int row, int level) {
        checkInside(column, row);
        fog[row][column] = clampFog(level);
        send("setFog", cell(column, row).put("level", fog[row][column]));
    }

    /** Replaces the fog of all cells. The array must have the size of the map. */
    public void setFog(int[][] levels) {
        copyInto(fog, levels, "fog");
        for (int[] row : fog) {
            for (int c = 0; c < row.length; c++) row[c] = clampFog(row[c]);
        }
        send("setFogAll", new JsonObject().put("tiles", flatten(fog)));
    }

    /** Covers the whole map with fog of the given level. */
    public void fillFog(int level) {
        for (int[] row : fog) java.util.Arrays.fill(row, clampFog(level));
        send("setFogAll", new JsonObject().put("tiles", flatten(fog)));
    }

    /** Removes the fog from all cells within {@code radius} tiles of (column, row). */
    public void reveal(int column, int row, int radius) {
        for (int r = Math.max(0, row - radius); r <= Math.min(rows - 1, row + radius); r++) {
            for (int c = Math.max(0, column - radius); c <= Math.min(columns - 1, column + radius); c++) {
                if ((c - column) * (c - column) + (r - row) * (r - row) <= radius * radius + radius) {
                    fog[r][c] = FOG_NONE;
                }
            }
        }
        send("reveal", cell(column, row).put("radius", radius));
    }

    // ---------------------------------------------------------------- walkability

    /** Marks a cell as blocked (or free again) for all characters, e.g. a house or a rock. */
    public void setBlocked(int column, int row, boolean isBlocked) {
        checkInside(column, row);
        blocked[row][column] = isBlocked;
    }

    public boolean isBlocked(int column, int row) {
        return isInside(column, row) && blocked[row][column];
    }

    public boolean isInside(int column, int row) {
        return column >= 0 && row >= 0 && column < columns && row < rows;
    }

    /** {@code true} if a character of the given type can stand on (column, row). */
    public boolean canEnter(CharacterType type, int column, int row) {
        return isInside(column, row) && !blocked[row][column] && type.canEnter(terrain[row][column]);
    }

    // ---------------------------------------------------------------- view

    public Theme getTheme() {
        return theme;
    }

    /** Switches the look of the map. Tiles keep their meaning in every theme. */
    public void setTheme(Theme theme) {
        this.theme = theme;
        send("setTheme", new JsonObject().put("theme", theme.id()));
    }

    /** Turns the automatic coast / path tiles on (default) or off. */
    public void setAutoTiling(boolean enabled) {
        this.autoTiling = enabled;
        send("setAutoTiling", new JsonObject().put("enabled", enabled));
    }

    /**
     * Sets the zoom: screen pixels per tile pixel. {@link #ZOOM_FIT} (default) fits the whole map
     * into the playground. With a larger zoom, use {@link #follow(MapCharacter)} to keep a
     * character in view.
     */
    public void setZoom(double zoom) {
        this.zoom = Math.max(0, zoom);
        send("setZoom", new JsonObject().put("zoom", this.zoom));
    }

    /** Lets the view follow a character, {@code null} stops following. */
    public void follow(MapCharacter character) {
        send("follow", new JsonObject().put("character", character == null ? -1 : character.ID));
    }

    /** Shows a grid with the cell coordinates (helpful while designing a map). */
    public void showGrid(boolean visible) {
        send("showGrid", new JsonObject().put("visible", visible));
    }

    // ---------------------------------------------------------------- events

    /** Called when the user clicks on a cell. Only in RPC mode (message passing). */
    public void setOnTileClicked(OnTileClicked callback) {
        this.onTileClicked = callback;
        send("listenClicks", new JsonObject().put("enabled", callback != null));
    }

    @JSEvent("click")
    private void onClickEvent(JsonElement json) {
        if (onTileClicked != null) {
            JsonObject o = json.getObject();
            onTileClicked.onTileClicked(this, o.getInt("col", 0), o.getInt("row", 0));
        }
    }

    // ---------------------------------------------------------------- helpers

    void checkInside(int column, int row) {
        if (!isInside(column, row)) {
            throw new IndexOutOfBoundsException("Cell (" + column + ", " + row + ") is outside of the "
                    + columns + " x " + rows + " map");
        }
    }

    static JsonObject cell(int column, int row) {
        return new JsonObject().put("col", column).put("row", row);
    }

    private static int clampFog(int level) {
        return Math.max(FOG_NONE, Math.min(FOG_DENSE, level));
    }

    private void copyInto(int[][] target, int[][] source, String what) {
        if (source == null || source.length != rows) {
            throw new IllegalArgumentException(what + " must have " + rows + " rows");
        }
        for (int r = 0; r < rows; r++) {
            if (source[r] == null || source[r].length != columns) {
                throw new IllegalArgumentException(what + " row " + r + " must have " + columns + " columns");
            }
            System.arraycopy(source[r], 0, target[r], 0, columns);
        }
    }

    private static int[][] filled(int columns, int rows, int tile) {
        int[][] a = new int[rows][columns];
        for (int[] row : a) java.util.Arrays.fill(row, tile);
        return a;
    }

    private static JsonArray flatten(int[][] a) {
        JsonArray out = new JsonArray();
        for (int[] row : a) {
            for (int v : row) out.add(JsonElement.from(v));
        }
        return out;
    }
}

package de.fau.tf.lgdv.tilemap;

/** How a {@link TileMap} is drawn. */
public enum Projection {
    /**
     * Seen from above with the pixel art tiles of the {@link Theme} (terrain, decorations, fog)
     * and the top-down {@link SpriteType}s / {@link CharacterType}s.
     */
    TOP_DOWN,
    /**
     * Isometric: the cells are drawn as diamonds (land and water in the colors of the
     * {@link Theme}) with the pre-rendered isometric {@link SpriteType}s / {@link CharacterType}s.
     * Column + 1 goes to the bottom right, row + 1 to the bottom left. Decorations and fog are
     * not shown.
     */
    ISOMETRIC
}

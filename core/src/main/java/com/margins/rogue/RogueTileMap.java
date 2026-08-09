package com.margins.rogue;

import java.util.Arrays;

public class RogueTileMap {
    public static final int STRUCTURE_NONE = -1;
    public static final int STRUCTURE_OLD_HOUSE = 0;
    public static final int STRUCTURE_GRAVEYARD = 1;
    public static final int STRUCTURE_DEEP_CAVE = 2;
    public static final int STRUCTURE_HUNTERS_BLIND = 3;
    public static final int STRUCTURE_FALLEN_LOG_HOLLOW = 4;
    public static final int STRUCTURE_FOREST_SHRINE = 5;
    public static final int STRUCTURE_BEEHIVE_GROVE = 6;
    public static final int STRUCTURE_KITCHEN_CAMP = 7;
    public static final int STRUCTURE_COLLAPSED_WATCHTOWER = 8;
    public static final int STRUCTURE_POACHERS_CAMP = 9;
    public static final int STRUCTURE_SUNKEN_WELL = 10;

    private int width;
    private int height;
    private int[][] tiles;
    /** Optional visual cell layered over the gameplay tile; -1 means ordinary forest terrain. */
    private int[][] structureTiles;
    /** Atlas identity for each visual cell. Kept separate so structures can reuse cell ordinals. */
    private int[][] structureTypes;
    private transient boolean[][] visible;  // recomputed each FOV pass (not saved)
    private boolean[][] explored;           // fog memory — persists in the save

    private RogueTileMap() {} // for libGDX Json deserialization

    public RogueTileMap(int width, int height) {
        this.width = width;
        this.height = height;
        this.tiles = new int[width][height];
        this.structureTiles = new int[width][height];
        this.structureTypes = new int[width][height];
        clearStructureTiles();
        this.visible = new boolean[width][height];
        this.explored = new boolean[width][height];
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }

    public int getTile(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) return -1;
        return tiles[x][y];
    }

    public void setTile(int x, int y, int type) {
        if (x < 0 || x >= width || y < 0 || y >= height) return;
        tiles[x][y] = type;
    }

    /** Row-major cell from a multi-tile structure atlas, or -1 outside a structure. */
    public int getStructureTile(int x, int y) {
        if (!inBounds(x, y) || structureTiles == null) return -1;
        return structureTiles[x][y];
    }

    /** Atlas identity for the structure cell, or {@link #STRUCTURE_NONE} outside structures. */
    public int getStructureType(int x, int y) {
        if (!inBounds(x, y) || getStructureTile(x, y) < 0) return STRUCTURE_NONE;
        // Saves written before multiple atlases had cells but no type layer; those cells are the
        // Old House, so retain their visual identity instead of making them disappear on load.
        if (structureTypes == null) return STRUCTURE_OLD_HOUSE;
        return structureTypes[x][y];
    }

    /** True when this map carries the typed structure layer (Story 3.1+). Legacy maps (cells but no
     *  type layer) return false — their getStructureType is the OLD_HOUSE compatibility shim, not
     *  real structure content (Story 3.2 review fix gates the loot backfill on this). */
    public boolean hasStructureTypeLayer() {
        return structureTypes != null;
    }

    public void setStructureTile(int x, int y, int cell) {
        setStructureTile(x, y, STRUCTURE_OLD_HOUSE, cell);
    }

    public void setStructureTile(int x, int y, int type, int cell) {
        if (!inBounds(x, y)) return;
        ensureStructureTiles();
        structureTiles[x][y] = cell;
        structureTypes[x][y] = type;
    }

    public boolean isWalkable(int x, int y) {
        int t = getTile(x, y);
        return t >= 0 && RogueTile.isWalkable(t);
    }

    public boolean isOpaque(int x, int y) {
        int t = getTile(x, y);
        return t >= 0 && RogueTile.isOpaque(t);
    }

    private boolean inBounds(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    public boolean isVisible(int x, int y) {
        return inBounds(x, y) && visible != null && visible[x][y];
    }

    public void setVisible(int x, int y, boolean v) {
        if (!inBounds(x, y)) return;
        if (visible == null) visible = new boolean[width][height];
        visible[x][y] = v;
    }

    public void clearVisible() {
        if (visible == null) { visible = new boolean[width][height]; return; }
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) visible[x][y] = false;
        }
    }

    public boolean isExplored(int x, int y) {
        return inBounds(x, y) && explored != null && explored[x][y];
    }

    public void setExplored(int x, int y, boolean v) {
        if (!inBounds(x, y)) return;
        if (explored == null) explored = new boolean[width][height];
        explored[x][y] = v;
    }

    public void fill(int type) {
        ensureStructureTiles();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                tiles[x][y] = type;
                structureTiles[x][y] = -1;
                structureTypes[x][y] = STRUCTURE_NONE;
            }
        }
    }

    private void ensureStructureTiles() {
        if (structureTiles == null) {
            structureTiles = new int[width][height];
            for (int[] column : structureTiles) Arrays.fill(column, -1);
        }
        if (structureTypes == null) {
            structureTypes = new int[width][height];
            for (int x = 0; x < width; x++) {
                Arrays.fill(structureTypes[x], STRUCTURE_NONE);
                for (int y = 0; y < height; y++) {
                    if (structureTiles[x][y] >= 0) structureTypes[x][y] = STRUCTURE_OLD_HOUSE;
                }
            }
        }
    }

    private void clearStructureTiles() {
        for (int[] column : structureTiles) Arrays.fill(column, -1);
        for (int[] column : structureTypes) Arrays.fill(column, STRUCTURE_NONE);
    }
}

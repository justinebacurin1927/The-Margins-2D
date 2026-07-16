package com.margins.rogue;

public class RogueTileMap {
    private int width;
    private int height;
    private int[][] tiles;
    private transient boolean[][] visible;  // recomputed each FOV pass (not saved)
    private boolean[][] explored;           // fog memory — persists in the save

    private RogueTileMap() {} // for libGDX Json deserialization

    public RogueTileMap(int width, int height) {
        this.width = width;
        this.height = height;
        this.tiles = new int[width][height];
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
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                tiles[x][y] = type;
            }
        }
    }
}

package com.margins.map;

public class TileMap {
    public static final int GRASS = 0;
    public static final int DIRT = 1;
    public static final int STONE = 2;

    private final int width;
    private final int height;
    private final int[][] tiles;

    public TileMap(int width, int height) {
        this.width = width;
        this.height = height;
        this.tiles = new int[width][height];
        fill(GRASS);
    }

    public void fill(int tileType) {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                tiles[x][y] = tileType;
            }
        }
    }

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
        return t != -1 && t != STONE;
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }
}

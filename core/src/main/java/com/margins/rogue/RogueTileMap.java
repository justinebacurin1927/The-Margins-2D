package com.margins.rogue;

public class RogueTileMap {
    private final int width;
    private final int height;
    private final int[][] tiles;

    public RogueTileMap(int width, int height) {
        this.width = width;
        this.height = height;
        this.tiles = new int[width][height];
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

    public void fill(int type) {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                tiles[x][y] = type;
            }
        }
    }
}

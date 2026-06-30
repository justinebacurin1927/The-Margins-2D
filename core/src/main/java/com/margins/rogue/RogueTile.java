package com.margins.rogue;

public class RogueTile {
    public static final int WALL = 0;
    public static final int FLOOR = 1;
    public static final int DOOR = 2;
    public static final int STAIRS_DOWN = 3;
    public static final int STAIRS_UP = 4;

    public static boolean isWalkable(int tile) {
        return tile == FLOOR || tile == DOOR || tile == STAIRS_DOWN || tile == STAIRS_UP;
    }

    public static boolean isOpaque(int tile) {
        return tile == WALL;
    }
}

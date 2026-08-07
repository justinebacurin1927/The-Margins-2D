package com.margins.rogue;

public class RogueTile {
    public static final int WALL = 0;
    public static final int FLOOR = 1;
    public static final int DOOR = 2;
    // Water-source features (FR-6, Story 1.5): walkable, non-opaque tiles you COLLECT raw water
    // from. A minimal forward-pull — Epic 3 folds these into the real world-structure system.
    public static final int WELL = 3;
    public static final int POND = 4;
    public static final int RIVER = 5;

    public static boolean isWalkable(int tile) {
        return tile == FLOOR || tile == DOOR || tile == WELL || tile == POND || tile == RIVER;
    }

    public static boolean isOpaque(int tile) {
        return tile == WALL;
    }

    /** True for a water-source feature (Well/Pond/River). */
    public static boolean isWaterSource(int tile) {
        return tile == WELL || tile == POND || tile == RIVER;
    }
}

package com.margins.rogue.world;

/**
 * Story 3.1 (FR-9, AC-1/AC-2): the authored landmark geography of Herois — the Route→
 * "landmark geography" home AD-8 names. The four canon landmarks sit at fixed, authored
 * positions expressed as map fractions (Decision 1: "placed consistently" = authored, so the
 * skeleton survives a map-size change); the procedural wilderness fills between them. The
 * spine is deterministic from constants + the map dims and held transiently (AD-6) — no
 * persisted {@code RunState} field. Pure model, no libGDX (AD-2), headless-testable.
 *
 * <p>Danger rises east (AC-2): the invasion came down the Copper Road from the east, so the
 * spine's single danger/loot truth is {@link #dangerAt(int)} — monotonic from west (safe,
 * home/border) to east (the interior). Enemy placement (3.1), world-structure tiers (3.2),
 * occupation escalation (4.3) and the border cordon (5.7) all read it.
 */
public class WorldSpine {

    /** Authored landmark positions as map fractions (Decision 1) — the whole spine is these constants. */
    public static final float CORNEO_X = 1f / 6f;     // west — the home cluster
    public static final float CORNEO_Y = 0.5f;        // on the Copper Road
    public static final float ROAD_Y = 0.5f;          // the Copper Road's row
    public static final float ROAD_X_START = 0.1f;    // the road begins near the west edge
    public static final float ROAD_X_END = 0.95f;     // and runs east into the interior
    public static final float BORDER_X = 0.05f;       // the NW border crossing: far west
    public static final float BORDER_Y = 0.9f;        //   and far north (northwest = border)
    public static final float WATCHTOWER_X = 2f / 3f; // the Watchtower: east of Corneo
    public static final float WATCHTOWER_Y = 0.5f;    //   on the road row

    private final int width;
    private final int height;

    public WorldSpine(int width, int height) {
        this.width = width;
        this.height = height;
    }

    /** A map fraction → its tile coordinate (rounds to the nearest tile, clamped into bounds). */
    public int tileX(float fraction) { return Math.round(fraction * (width - 1)); }
    public int tileY(float fraction) { return Math.round(fraction * (height - 1)); }

    public int getWidth() { return width; }
    public int getHeight() { return height; }

    /** Corneo, the west home cluster (its town-plaza center tile). */
    public int corneoX() { return tileX(CORNEO_X); }
    public int corneoY() { return tileY(CORNEO_Y); }

    /** The Copper Road: a single mid-y row, spanning roadStartX..roadEndX. */
    public int roadY() { return tileY(ROAD_Y); }
    public int roadStartX() { return tileX(ROAD_X_START); }
    public int roadEndX() { return tileX(ROAD_X_END); }

    /** The NW border crossing: far west AND far north — the safe edge (AC-2). */
    public int borderX() { return tileX(BORDER_X); }
    public int borderY() { return tileY(BORDER_Y); }

    /** The Watchtower: on the road row, east of Corneo toward the interior. */
    public int watchtowerX() { return tileX(WATCHTOWER_X); }
    public int watchtowerY() { return tileY(WATCHTOWER_Y); }

    /**
     * 0 at the west edge → 1 at the east edge — the east/west spine's coordinate. The single
     * authority for the gradient; no ad-hoc east-half/west-half cuts anywhere else.
     */
    public float eastness(int x) {
        return x / (width - 1f);
    }

    /** Danger rises east (AC-2): safety lies west, the invasion east. Currently just the eastness. */
    public float dangerAt(int x) {
        return eastness(x);
    }
}

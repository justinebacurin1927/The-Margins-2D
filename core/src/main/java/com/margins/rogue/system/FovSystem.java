package com.margins.rogue.system;

import com.margins.rogue.RogueTileMap;
import com.margins.rogue.state.RunState;

/**
 * Recursive shadowcasting field of view (AD-2: tile data only, no render types).
 * Recomputes which tiles the player can currently see and accumulates the
 * explored set. Walls ({@link RogueTileMap#isOpaque}) block sight; doors are
 * transparent. Runs as a pipeline step (AD-4) after the player acts.
 */
public final class FovSystem {
    public static final int RADIUS = 8; // PRD Balance: FOV sight radius

    // Per-octant transform multipliers for the 8 shadowcasting octants.
    private static final int[] XX = {1, 0, 0, -1, -1, 0, 0, 1};
    private static final int[] XY = {0, 1, -1, 0, 0, -1, 1, 0};
    private static final int[] YX = {0, 1, 1, 0, 0, -1, -1, 0};
    private static final int[] YY = {1, 0, 0, 1, -1, 0, 0, -1};

    private FovSystem() {}

    public static void compute(RunState state) {
        compute(state.getTileMap(), state.getPlayer().getTileX(), state.getPlayer().getTileY());
    }

    /** Map-level entry point (headless-testable with a hand-built map). */
    public static void compute(RogueTileMap map, int px, int py) {
        map.clearVisible();
        map.setVisible(px, py, true);
        map.setExplored(px, py, true);
        for (int oct = 0; oct < 8; oct++) {
            castLight(map, px, py, 1, 1.0f, 0.0f, XX[oct], XY[oct], YX[oct], YY[oct]);
        }
    }

    private static void castLight(RogueTileMap map, int cx, int cy, int row,
                                  float start, float end,
                                  int xx, int xy, int yx, int yy) {
        if (start < end) return;
        float nextStart = start;
        for (int i = row; i <= RADIUS; i++) {
            boolean blocked = false;
            for (int dx = -i, dy = -i; dx <= 0; dx++) {
                int mapX = cx + dx * xx + dy * xy;
                int mapY = cy + dx * yx + dy * yy;
                float lSlope = (dx - 0.5f) / (dy + 0.5f);
                float rSlope = (dx + 0.5f) / (dy - 0.5f);
                if (rSlope > start) continue;
                if (lSlope < end) break;
                if (dx * dx + dy * dy <= RADIUS * RADIUS) {
                    map.setVisible(mapX, mapY, true);
                    map.setExplored(mapX, mapY, true);
                }
                if (blocked) {
                    if (map.isOpaque(mapX, mapY)) {
                        nextStart = rSlope;
                        continue;
                    } else {
                        blocked = false;
                        start = nextStart;
                    }
                } else if (map.isOpaque(mapX, mapY) && i < RADIUS) {
                    blocked = true;
                    castLight(map, cx, cy, i + 1, start, lSlope, xx, xy, yx, yy);
                    nextStart = rSlope;
                }
            }
            if (blocked) break;
        }
    }
}

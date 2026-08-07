package com.margins.rogue.system;

import com.margins.rogue.RogueTileMap;
import com.margins.rogue.Weather;
import com.margins.rogue.state.RunState;

/**
 * Recursive shadowcasting field of view (AD-2: tile data only, no render types).
 * Recomputes which tiles the player can currently see and accumulates the
 * explored set. Walls ({@link RogueTileMap#isOpaque}) block sight; doors are
 * transparent. Runs as a pipeline step (AD-4) after the player acts.
 *
 * <p>The sight radius is DYNAMIC (AD-18, Story 1.4): Night or Fog shrink it, a lit
 * source (campfire/torch) restores it but still below the clear-day baseline. The
 * shadowcasting itself is unchanged — only the radius it runs to varies.
 */
public final class FovSystem {
    // FOV sight radii (PRD Balance / bible starting calibration — the PRD locks no exact value).
    // The invariant DARK < LIT < DAY is what makes "light restores but reduced" (AC-2) true.
    public static final int DAY_RADIUS = 8;  // clear-day baseline
    public static final int DARK_RADIUS = 4; // Night or Fog (AC-1)
    public static final int LIT_RADIUS = 6;  // a light restores sight, still < day (AC-2)

    // Per-octant transform multipliers for the 8 shadowcasting octants.
    private static final int[] XX = {1, 0, 0, -1, -1, 0, 0, 1};
    private static final int[] XY = {0, 1, -1, 0, 0, -1, 1, 0};
    private static final int[] YX = {0, 1, 1, 0, 0, -1, -1, 0};
    private static final int[] YY = {1, 0, 0, 1, -1, 0, 0, -1};

    private FovSystem() {}

    /**
     * True if nothing opaque sits strictly between the two tiles (endpoints not
     * tested). Bresenham walk over {@link RogueTileMap#isOpaque}; shared so FOV
     * and the detection system agree on what blocks sight.
     */
    public static boolean hasLineOfSight(RogueTileMap map, int x0, int y0, int x1, int y1) {
        int dx = Math.abs(x1 - x0), dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;
        int x = x0, y = y0;
        while (x != x1 || y != y1) {
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x += sx; }
            if (e2 < dx) { err += dx; y += sy; }
            if (x == x1 && y == y1) break;   // reached target; don't test it as a blocker
            if (map.isOpaque(x, y)) return false;
        }
        return true;
    }

    /**
     * The player's current sight radius (AD-18): Night or Fog shrink the ambient radius, and a
     * lit source can only ever RESTORE sight upward — never reduce it. So a torch in the dark
     * lifts 4→6, but a torch in clear day leaves the baseline 8 untouched (a fire adds nothing
     * to daylight, and must not penalize it). This is the honest reading of "restores it but
     * still below the clear-day baseline."
     */
    public static int radiusFor(RunState state) {
        int ambient = (!state.isDay() || state.getWeather() == Weather.FOG) ? DARK_RADIUS : DAY_RADIUS;
        return state.hasLight() ? Math.max(ambient, LIT_RADIUS) : ambient;
    }

    public static void compute(RunState state) {
        compute(state.getTileMap(), state.getPlayer().getTileX(), state.getPlayer().getTileY(),
                radiusFor(state));
    }

    /** Map-level entry point at a given sight radius (headless-testable with a hand-built map). */
    public static void compute(RogueTileMap map, int px, int py, int radius) {
        map.clearVisible();
        map.setVisible(px, py, true);
        map.setExplored(px, py, true);
        for (int oct = 0; oct < 8; oct++) {
            castLight(map, px, py, 1, 1.0f, 0.0f, XX[oct], XY[oct], YX[oct], YY[oct], radius);
        }
    }

    private static void castLight(RogueTileMap map, int cx, int cy, int row,
                                  float start, float end,
                                  int xx, int xy, int yx, int yy, int radius) {
        if (start < end) return;
        float nextStart = start;
        for (int i = row; i <= radius; i++) {
            boolean blocked = false;
            for (int dx = -i, dy = -i; dx <= 0; dx++) {
                int mapX = cx + dx * xx + dy * xy;
                int mapY = cy + dx * yx + dy * yy;
                float lSlope = (dx - 0.5f) / (dy + 0.5f);
                float rSlope = (dx + 0.5f) / (dy - 0.5f);
                if (rSlope > start) continue;
                if (lSlope < end) break;
                if (dx * dx + dy * dy <= radius * radius) {
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
                } else if (map.isOpaque(mapX, mapY) && i < radius) {
                    blocked = true;
                    castLight(map, cx, cy, i + 1, start, lSlope, xx, xy, yx, yy, radius);
                    nextStart = rSlope;
                }
            }
            if (blocked) break;
        }
    }
}

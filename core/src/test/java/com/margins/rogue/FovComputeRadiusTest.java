package com.margins.rogue;

import com.margins.rogue.system.FovSystem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The dynamic radius is actually wired into the shadowcasting cast (Story 1.4), not just the
 * selector: on an open map a tile beyond the shrunk radius is visible at the day radius but
 * hidden at the dark radius. Uses the map-level {@link FovSystem#compute} overload directly.
 */
class FovComputeRadiusTest {

    private static RogueTileMap openMap(int w, int h) {
        RogueTileMap m = new RogueTileMap(w, h);
        for (int x = 0; x < w; x++)
            for (int y = 0; y < h; y++)
                m.setTile(x, y, RogueTile.FLOOR);
        return m;
    }

    @Test
    void smallerRadiusSeesFewerTiles() {
        RogueTileMap m = openMap(20, 20);
        int px = 10, py = 10;
        int near = py + 3;  // distance 3
        int far = py + 6;   // distance 6 — inside DAY_RADIUS (8), outside DARK_RADIUS (4)

        FovSystem.compute(m, px, py, FovSystem.DAY_RADIUS);
        assertTrue(m.isVisible(px, far), "a tile 6 away is visible at the day radius");
        assertTrue(m.isVisible(px, near), "a near tile is visible at the day radius");

        FovSystem.compute(m, px, py, FovSystem.DARK_RADIUS);
        assertFalse(m.isVisible(px, far), "the same tile is hidden once the radius shrinks (AC-1)");
        assertTrue(m.isVisible(px, near), "a near tile stays visible at the dark radius");
    }

    @Test
    void theRadiusBoundaryIsExact() {
        // Pin the inclusive Euclidean edge (dx*dx+dy*dy <= radius*radius): the tile AT distance
        // == radius is the last visible one, and distance radius+1 is the first hidden — so an
        // off-by-one in the loop bound or the radius clamp can't slip through.
        RogueTileMap m = openMap(20, 20);
        int px = 10, py = 10;
        int r = FovSystem.DARK_RADIUS; // 4

        FovSystem.compute(m, px, py, r);
        assertTrue(m.isVisible(px, py + r), "the tile at distance == radius is visible");
        assertFalse(m.isVisible(px, py + r + 1), "the tile one past the radius is hidden");
    }
}

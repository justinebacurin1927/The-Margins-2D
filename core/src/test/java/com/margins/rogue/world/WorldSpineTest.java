package com.margins.rogue.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Story 3.1 (FR-9 / AC-1 / AC-2): the authored landmark geography — {@code WorldSpine} is the
 * Route→geography home AD-8 names. The four canon landmarks sit at fixed authored positions
 * (map fractions, so they survive a size change), the east/west query is monotonic, and the
 * spine is deterministic from the map dims alone (no persisted state — AD-6).
 */
class WorldSpineTest {

    /** The x-fraction a tile coordinate resolves to (the inverse of the spine's tileX). */
    private static float xFraction(int x, WorldSpine s) {
        return x / (s.getWidth() - 1f);
    }

    @Test
    void authoredPositionsAreFixedAcrossMapSizes() {
        // Decision 1: "placed consistently" = authored, not seeded. Two different map dims
        // resolve the SAME fraction constants to (differently-numbered but) proportional tiles.
        WorldSpine a = new WorldSpine(96, 48);
        WorldSpine b = new WorldSpine(50, 50);

        assertEquals(WorldSpine.CORNEO_X, xFraction(a.corneoX(), a), 0.06f, "Corneo x-const stays ~1/6");
        assertEquals(WorldSpine.CORNEO_X, xFraction(b.corneoX(), b), 0.06f, "Corneo x-const on any size");
        assertEquals(WorldSpine.CORNEO_Y, a.corneoY() / (a.getHeight() - 1f), 0.06f, "Corneo sits mid-y");
        assertEquals(WorldSpine.ROAD_Y, a.roadY() / (a.getHeight() - 1f), 0.06f, "the road row is mid-y");
        assertEquals(WorldSpine.BORDER_X, xFraction(a.borderX(), a), 0.06f, "the crossing is far west");
        assertEquals(WorldSpine.BORDER_Y, a.borderY() / (a.getHeight() - 1f), 0.06f, "and far north (NW)");
        assertEquals(WorldSpine.WATCHTOWER_X, xFraction(a.watchtowerX(), a), 0.06f, "the tower is ~2/3 x");
        assertEquals(WorldSpine.WATCHTOWER_X, xFraction(b.watchtowerX(), b), 0.06f, "the tower const holds");
    }

    @Test
    void eastnessIsMonotonicWestToEast() {
        WorldSpine s = new WorldSpine(96, 48);
        int w = s.getWidth();
        assertTrue(s.eastness(0) < s.eastness(w / 2), "west is less east than mid");
        assertTrue(s.eastness(w / 2) < s.eastness(w - 1), "mid is less east than the far edge");
        assertEquals(0f, s.eastness(0), 0f, "the west edge is eastness 0");
        assertEquals(1f, s.eastness(w - 1), 0f, "the east edge is eastness 1");
    }

    @Test
    void dangerRisesEastExactlyAlongTheSpine() {
        WorldSpine s = new WorldSpine(96, 48);
        assertEquals(s.eastness(10), s.dangerAt(10), 0f, "dangerAt is the eastness — no separate curve");
        assertTrue(s.dangerAt(s.corneoX()) < s.dangerAt(s.watchtowerX()),
                "safety (west, home) < the interior: danger rises east (AC-2)");
        assertTrue(s.dangerAt(s.borderX()) < s.dangerAt(s.watchtowerX()),
                "the NW border (safety) is west of the tower (interior)");
    }

    @Test
    void theSpineIsPureAndDeterministic() {
        // AD-6 by construction: the spine is constants + dims. Two identical dims give identical
        // answers, and there is nothing stateful to persist (no RunState coupling).
        WorldSpine s1 = new WorldSpine(96, 48);
        WorldSpine s2 = new WorldSpine(96, 48);
        for (int x : new int[]{0, 16, 63, 95}) {
            assertEquals(s1.dangerAt(x), s2.dangerAt(x), 0f, "deterministic at x=" + x);
        }
        assertEquals(96, s1.getWidth());
        assertEquals(48, s1.getHeight());
    }
}

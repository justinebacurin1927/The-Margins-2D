package com.margins.rogue.state;

import com.margins.rogue.RogueEnemy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Story 4.3 (AD-11 channel a): the occupation thickens per act. The enemy-count decision is a pure,
 * rng-free function of {@code (eastness, act, ny)} — Act 1 is today's baseline, each later act adds
 * {@code (act-1)} enemies to already-dangerous interior regions, and the NW border cordon (channel b,
 * Epic 5) is never touched. These pins hit the pure function directly (package-private test seam);
 * the last two prove the ramp bites end-to-end when a trigger (here, an explicit regen) fires, and
 * that the layout stays deterministic per seed+act (AD-5).
 */
class OccupationEscalationTest {

    // Representative eastness samples and their pre-4.3 base bands (RunState.baseEnemyCountFor).
    private static final float[] EASTNESS = {0.0f, 0.1f, 0.2f, 0.3f, 0.45f, 0.6f, 0.7f, 0.9f};
    private static final int[] BASE      = {0,    0,    0,    1,    2,     2,    3,    3};

    @Test
    void actOneIsBitIdenticalToTheBaseBands() {
        for (int i = 0; i < EASTNESS.length; i++) {
            assertEquals(BASE[i], RunState.enemyCountFor(EASTNESS[i], 1, 0.5f),
                    "Act 1 must equal the pre-4.3 band at eastness " + EASTNESS[i] + " (AC-3, no regression)");
            assertEquals(BASE[i], RunState.baseEnemyCountFor(EASTNESS[i]),
                    "the extracted base step is unchanged at eastness " + EASTNESS[i]);
        }
    }

    @Test
    void laterActsAddOneEnemyPerActToTheInterior() {
        // eastness 0.6 → base 2; 0.3 → base 1; 0.9 → base 3. Additive, monotonic, no compounding.
        assertEquals(3, RunState.enemyCountFor(0.6f, 2, 0.5f), "base 2 +1 at Act 2");
        assertEquals(4, RunState.enemyCountFor(0.6f, 3, 0.5f), "base 2 +2 at Act 3");
        assertEquals(2, RunState.enemyCountFor(0.3f, 2, 0.5f), "base 1 +1 at Act 2");
        assertEquals(3, RunState.enemyCountFor(0.3f, 3, 0.5f), "base 1 +2 at Act 3");
        assertEquals(5, RunState.enemyCountFor(0.9f, 3, 0.5f), "base 3 +2 at Act 3");
    }

    @Test
    void theWestSafeTierStaysEmptyAtEveryAct() {
        for (float e : new float[]{0.0f, 0.1f, 0.2f}) {
            for (int act = 1; act <= 3; act++) {
                assertEquals(0, RunState.enemyCountFor(e, act, 0.5f),
                        "the west safe tier (base 0) is never thickened — eastness " + e + ", act " + act);
            }
        }
    }

    @Test
    void theNwBorderCordonIsNeverThickened() {
        // The cordon corner: far-west (eastness < 0.2) AND far-north (ny > 0.8), per WorldSpine
        // BORDER_X=0.05 / BORDER_Y=0.9. It is base-0 anyway, but the explicit guard pins AC-2.
        for (int act = 1; act <= 3; act++) {
            assertEquals(0, RunState.enemyCountFor(0.05f, act, 0.9f),
                    "the NW border cordon stays untouched at act " + act + " (channel a ≠ channel b)");
        }
    }

    @Test
    void inCordonNamesTheFarWestFarNorthCorner() {
        assertTrue(RunState.inCordon(0.05f, 0.9f), "far-west + far-north = the cordon");
        assertTrue(RunState.inCordon(0.19f, 0.81f), "just inside both bounds");
        assertFalse(RunState.inCordon(0.05f, 0.5f), "far-west but not far-north → not the cordon");
        assertFalse(RunState.inCordon(0.5f, 0.9f), "far-north but not far-west (interior) → not the cordon");
        assertFalse(RunState.inCordon(0.2f, 0.9f), "0.2 is not strictly west of the safe tier");
    }

    // --- End-to-end: the ramp bites when a live trigger (here, an explicit regen) fires (AC-1) ---

    @Test
    void aHigherActRegeneratesADenserInterior() {
        // Regen BOTH sides from the same seed so the base map/room-centers are identical (FloorGenerator
        // runs before placeFloorActors and is act-independent); only the per-region act bump differs.
        long[] seeds = {1L, 2L, 3L, 5L, 8L, 13L, 42L, 100L, 777L, 2024L};
        int act1Total = 0, act3Total = 0;
        for (long seed : seeds) {
            act1Total += totalEnemies(regenAtAct(seed, 1));
            act3Total += totalEnemies(regenAtAct(seed, 3));
        }
        assertTrue(act3Total > act1Total,
                "Act 3 fields more enemies than Act 1 across the same seeds (" + act3Total + " vs " + act1Total + ")");
    }

    @Test
    void sameSeedAndActRegeneratesAnIdenticalLayout() {
        // AD-5: the count decision is rng-free, so a given seed+act reproduces the exact enemy layout.
        RunState a = regenAtAct(99L, 2);
        RunState b = regenAtAct(99L, 2);
        assertEquals(a.getEnemies().size(), b.getEnemies().size(), "same enemy count");
        for (int i = 0; i < a.getEnemies().size(); i++) {
            RogueEnemy ea = a.getEnemies().get(i);
            RogueEnemy eb = b.getEnemies().get(i);
            assertEquals(ea.getTileX(), eb.getTileX(), "enemy " + i + " x reproduces");
            assertEquals(ea.getTileY(), eb.getTileY(), "enemy " + i + " y reproduces");
        }
    }

    /** A run whose floor was (re)generated with the given act flag set — the Epic 5 trigger, stubbed. */
    private RunState regenAtAct(long seed, int act) {
        RunState s = new RunState(seed);
        s.getFlagStore().setAct(act);
        s.generateFloor(); // regenerate now that the act is set (Epic 5 will drive this live)
        return s;
    }

    private int totalEnemies(RunState s) {
        return s.getEnemies().size();
    }
}

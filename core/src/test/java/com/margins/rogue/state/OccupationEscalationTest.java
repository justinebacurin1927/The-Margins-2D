package com.margins.rogue.state;

import com.margins.rogue.RogueEnemy;
import com.margins.rogue.world.WorldSpine;
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
        // Non-vacuity guard (review 4.3): if the sampled seeds placed no interior enemies at Act 1,
        // "Act 3 > Act 1" could pass trivially (0 < something) and hide a broken ramp. Pin that the
        // baseline is real. (We assert the AGGREGATE, not per-seed: post-walkability placement isn't
        // strictly monotonic per seed — act 3 draws more positions, so a specific seed can reject a few
        // that act 1 kept. The intended per-region monotonicity is pinned by the unit tests above.)
        assertTrue(act1Total > 0, "sanity: the sampled seeds field interior enemies at Act 1 (test not vacuous)");
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

    // --- Channel b (Story 5.7): the NW border cordon THINS as the act advances (dual of channel a) ---

    @Test
    void theCordonThinsAsTheActAdvances() {
        assertEquals(3, RunState.cordonCountFor(1), "Act 1: the war has not yet consolidated east");
        assertEquals(2, RunState.cordonCountFor(2));
        assertEquals(1, RunState.cordonCountFor(3), "Act 3: thinnest — the crossing is survivable (AD-12)");
        assertEquals(0, RunState.cordonCountFor(4), "fully consolidated east → the homeward gate empties");
        assertEquals(3, RunState.cordonCountFor(0), "a never-set/0 act clamps to Act 1");
    }

    @Test
    void channelBIsSeparateFromChannelA_theCordonIsNeverThickenedByEnemyCountFor() {
        // The dual invariant to theNwBorderCordonIsNeverThickened: channel b lives in cordonCountFor,
        // NOT enemyCountFor, so the two channels never merge (AD-11) — enemyCountFor stays 0 there.
        for (int act = 1; act <= 4; act++) {
            assertEquals(0, RunState.enemyCountFor(0.05f, act, 0.9f),
                    "channel a still never touches the cordon at act " + act);
        }
    }

    @Test
    void aLaterActRegeneratesAThinnerCordon() {
        // The dual of aHigherActRegeneratesADenserInterior: the NW cordon box fields FEWER foes at
        // Act 3 than at Act 1 across the same seeds (aggregate — post-walkability placement isn't
        // strictly per-seed monotonic).
        long[] seeds = {1L, 2L, 3L, 5L, 8L, 13L, 42L, 100L, 777L, 2024L};
        int act1Cordon = 0, act3Cordon = 0;
        for (long seed : seeds) {
            act1Cordon += cordonEnemies(regenAtAct(seed, 1));
            act3Cordon += cordonEnemies(regenAtAct(seed, 3));
        }
        assertTrue(act1Cordon > 0, "sanity: the cordon fields foes at Act 1 (test not vacuous)");
        assertTrue(act3Cordon < act1Cordon,
                "the cordon thins by Act 3 (" + act3Cordon + " vs " + act1Cordon + ")");
    }

    /** Enemies standing inside the NW cordon box (far-west + far-north). */
    private int cordonEnemies(RunState s) {
        WorldSpine spine = new WorldSpine(s.getTileMap().getWidth(), s.getTileMap().getHeight());
        int n = 0;
        for (RogueEnemy e : s.getEnemies()) {
            float ny = e.getTileY() / (spine.getHeight() - 1f);
            if (RunState.inCordon(spine.eastness(e.getTileX()), ny)) n++;
        }
        return n;
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

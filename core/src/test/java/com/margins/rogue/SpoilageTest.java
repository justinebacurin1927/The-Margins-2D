package com.margins.rogue;

import com.margins.rogue.item.Supply;
import com.margins.rogue.state.RunState;
import com.margins.rogue.system.PlayerAction;
import com.margins.rogue.system.SpoilageSystem;
import com.margins.rogue.system.TurnEngine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Food spoilage (FR-6, Story 1.5 AC-3): food advances Raw → Half-Rotten → Fully Spoiled on the
 * acted path; cooked meat resists; a Salt stack slows the rate; a wall-bump advances nothing (AD-5).
 */
class SpoilageTest {

    private static int count(RunState s, Supply type) {
        return s.getInventory().count(type.ordinal());
    }

    private static void tickN(RunState s, int n) {
        for (int i = 0; i < n; i++) SpoilageSystem.tick(s);
    }

    @Test
    void meatAdvancesThroughTheSpoilageLadder() {
        RunState s = new RunState(1L);
        s.getInventory().tryAdd(Supply.RAW_MEAT.ordinal(), 1);

        tickN(s, SpoilageSystem.SPOIL_INTERVAL); // one interval → Half-Rotten
        assertEquals(0, count(s, Supply.RAW_MEAT), "raw meat has advanced");
        assertEquals(1, count(s, Supply.HALF_ROTTEN_MEAT), "to half-rotten");

        tickN(s, SpoilageSystem.SPOIL_INTERVAL); // next interval → Fully Spoiled
        assertEquals(0, count(s, Supply.HALF_ROTTEN_MEAT));
        assertEquals(1, count(s, Supply.SPOILED_MEAT), "to fully spoiled (terminal)");

        tickN(s, SpoilageSystem.SPOIL_INTERVAL); // spoiled is terminal
        assertEquals(1, count(s, Supply.SPOILED_MEAT), "fully spoiled does not advance further");
    }

    @Test
    void cookedMeatResistsSpoilage() {
        RunState s = new RunState(1L);
        s.getInventory().tryAdd(Supply.COOKED_MEAT.ordinal(), 1);
        tickN(s, SpoilageSystem.SPOIL_INTERVAL * 4);
        assertEquals(1, count(s, Supply.COOKED_MEAT), "cooked meat does not spoil (AC-3)");
    }

    @Test
    void saltSlowsTheSpoilageRate() {
        RunState s = new RunState(1L);
        s.getInventory().tryAdd(Supply.RAW_MEAT.ordinal(), 1);
        s.getInventory().tryAdd(Supply.SALT.ordinal(), 1); // doubles the interval

        tickN(s, SpoilageSystem.SPOIL_INTERVAL); // would spoil without salt — but salt holds it
        assertEquals(1, count(s, Supply.RAW_MEAT), "salt keeps the meat fresh past one interval");

        tickN(s, SpoilageSystem.SPOIL_INTERVAL); // reach the doubled interval
        assertEquals(1, count(s, Supply.HALF_ROTTEN_MEAT), "it advances at the slowed rate");
    }

    @Test
    void togglingSaltDoesNotShiftTheSpoilageCadence() {
        // M2-review: spoilage is ACCRUED (+2 unsalted / +1 salted per turn, threshold
        // 2*SPOIL_INTERVAL), so held-salt time and unsalted time both count proportionally —
        // picking salt up or putting it down mid-run never skips or delays a stage beyond the
        // exact turns it was held. (The old clock-modulo model would defer the 50-turn advance
        // to 100 if salt was up at 49, then resume a shifted phase.)
        RunState s = new RunState(1L);
        s.getInventory().tryAdd(Supply.RAW_MEAT.ordinal(), 1);

        // Salt held for one interval (50 salted turns → 50 progress), then dropped: 25 unsalted
        // turns (→ 50 + 50 = 100) must advance the meat exactly once.
        s.getInventory().tryAdd(Supply.SALT.ordinal(), 1);
        tickN(s, SpoilageSystem.SPOIL_INTERVAL);
        s.getInventory().remove(Supply.SALT.ordinal(), 1);
        tickN(s, SpoilageSystem.SPOIL_INTERVAL / 2);

        assertEquals(0, count(s, Supply.RAW_MEAT), "held-salt time and unsalted time both count");
        assertEquals(1, count(s, Supply.HALF_ROTTEN_MEAT), "advanced exactly once at the accrued threshold");
        assertEquals(0, s.getSpoilageProgress(), "progress resets after an advance (no drift)");
    }

    @Test
    void spoilageAdvancesOnlyOnActedTurns() {
        // AD-5: a wall-bump commits no turn, so the spoilage clock must not advance on it.
        RunState s = new RunState(1L);
        s.getInventory().tryAdd(Supply.RAW_MEAT.ordinal(), 1);
        TurnEngine te = new TurnEngine();
        int[] toWall = placeAgainstWall(s);

        for (int i = 0; i < SpoilageSystem.SPOIL_INTERVAL * 2; i++) {
            te.advance(s, PlayerAction.move(toWall[0], toWall[1], 0)); // all blocked — no turns commit
        }

        assertEquals(0, s.getSpoilageClock(), "wall-bumps advanced no spoilage clock");
        assertEquals(1, count(s, Supply.RAW_MEAT), "and the meat never spoiled");
    }

    /** Place the player on a floor tile adjacent to a wall; return the (dx,dy) toward the wall. */
    private static int[] placeAgainstWall(RunState s) {
        RogueTileMap m = s.getTileMap();
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int x = 1; x < m.getWidth() - 1; x++) {
            for (int y = 1; y < m.getHeight() - 1; y++) {
                if (m.getTile(x, y) != RogueTile.FLOOR) continue;
                for (int[] d : dirs) {
                    if (!m.isWalkable(x + d[0], y + d[1])) {
                        s.getPlayer().placeAt(x, y);
                        return d;
                    }
                }
            }
        }
        throw new IllegalStateException("no floor tile adjacent to a wall");
    }
}

package com.margins.rogue;

import com.margins.rogue.state.RunState;
import com.margins.rogue.system.CombatSystem;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Story 4.6 (FR-14): Permadeath and Last Stand. The once-per-run reprieve is now a GRIT roll —
 * a successful roll leaves Klein at 1 HP, a failed roll lets the death stand, and either way the
 * single check is spent (no re-roll on later lethal events). Permadeath (AC-2: save cleared, a
 * restart is a fresh life) is ratified brownfield here + in RunStatePersistence/SaveMigration.
 */
class LastStandTest {

    /** A stubbed RNG whose nextInt(100) is fixed — the dodge-test pattern, for a deterministic roll. */
    private static Random draw(int value) {
        return new Random() {
            @Override public int nextInt(int bound) { return value; }
        };
    }

    // --- AC-1: the GRIT roll on RoguePlayer (D2/D3) ---

    @Test
    void lastStandChanceIsSeventyAtTheDefaultGrit() {
        RoguePlayer p = new RunState(1L).getPlayer(); // default GRIT 5 → 30 + 5*8 = 70
        assertEquals(5, p.getGrit(), "precondition: the default GRIT is 5");
        assertEquals(70, p.lastStandChance(), "30 + GRIT*8 at GRIT 5");
    }

    @Test
    void tryLastStandSucceedsBelowTheChanceAndFailsAtOrAbove() {
        RoguePlayer p = new RunState(1L).getPlayer(); // chance 70
        assertTrue(p.tryLastStand(draw(0)),  "a draw of 0 is well below 70 → reprieve");
        assertTrue(p.tryLastStand(draw(69)), "a draw of 69 is below 70 → reprieve");
        assertFalse(p.tryLastStand(draw(70)), "a draw of 70 is NOT below 70 → death (roll is strict <)");
        assertFalse(p.tryLastStand(draw(99)), "a draw of 99 is above 70 → death");
    }

    // --- AC-1: checkLastStand wiring (D1) ---

    @Test
    void checkLastStandIsANoOpWhileAlive() {
        RunState s = new RunState(1L);
        CombatSystem.checkLastStand(s, new ArrayList<>());
        assertFalse(s.isLastStandUsed(), "a living player never spends the reprieve");
        assertTrue(s.getPlayer().isAlive());
    }

    @Test
    void theGritCheckIsSpentOnceAndHonorsItsRollAcrossSeeds() {
        // The roll rides the shared seeded stream, so the outcome varies by seed. We assert the
        // INVARIANT on every seed (the check is spent; a success ⟺ 1 HP ⟺ the "Last Stand!" line;
        // otherwise honest death) AND that the roll genuinely branches (both outcomes occur) — proving
        // checkLastStand wires the roll to revive/death rather than hard-coding either.
        int successes = 0, failures = 0;
        for (long seed = 0; seed < 50; seed++) {
            RunState s = new RunState(seed);
            RoguePlayer p = s.getPlayer();
            p.hurtRaw(p.getHp()); // drop to 0 HP — the first lethal event with the reprieve unused
            assertFalse(p.isAlive(), "precondition: at 0 HP");

            List<String> msgs = new ArrayList<>();
            CombatSystem.checkLastStand(s, msgs);

            assertTrue(s.isLastStandUsed(), "the one GRIT check is spent on the first 0-HP event (seed " + seed + ")");
            if (s.isLastStand()) {
                successes++;
                assertEquals(1, p.getHp(), "a successful roll revives to 1 HP (seed " + seed + ")");
                assertTrue(msgs.contains("Last Stand!"), "a successful reprieve announces itself (seed " + seed + ")");
            } else {
                failures++;
                assertFalse(p.isAlive(), "a failed roll lets the death stand (seed " + seed + ")");
                assertFalse(msgs.contains("Last Stand!"), "a failed roll is silent (seed " + seed + ")");
            }
        }
        assertTrue(successes > 0, "the GRIT roll succeeds on some seeds");
        assertTrue(failures > 0, "the GRIT roll fails on some seeds (~30% at GRIT 5) — the reprieve is not guaranteed");
    }

    @Test
    void aSpentCheckIsTrueDeathWithNoReRoll() {
        // D1: once the single check is spent, a later lethal event kills outright — no second roll.
        RunState s = new RunState(1L);
        s.setLastStandUsed(true); // the reprieve already used this run
        RoguePlayer p = s.getPlayer();
        p.hurtRaw(p.getHp());     // a later lethal event

        List<String> msgs = new ArrayList<>();
        CombatSystem.checkLastStand(s, msgs);

        assertFalse(p.isAlive(), "a spent reprieve → true permadeath, no second roll");
        assertFalse(s.isLastStand(), "no desperate revive");
        assertFalse(msgs.contains("Last Stand!"), "no reprieve line on true death");
    }

    // --- AC-2: permadeath ratified — a restart is a fresh life with a fresh reprieve ---

    @Test
    void restartGrantsAFreshReprieve() {
        RunState s = new RunState(1L);
        s.setLastStandUsed(true); // spent this life
        s.restart();
        assertFalse(s.isLastStandUsed(), "a new life on the fresh forest gets its Last Stand back (AC-2)");
    }
}

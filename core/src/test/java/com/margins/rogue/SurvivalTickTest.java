package com.margins.rogue;

import com.margins.rogue.state.RunState;
import com.margins.rogue.system.PlayerAction;
import com.margins.rogue.system.TurnEngine;
import com.margins.rogue.system.TurnResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Survival-clock honesty (AD-5, Story 1.2 AC-2): a wasted keypress (a move into a
 * wall) commits no turn and ticks NONE of the four survival tracks, while a real
 * action advances all four. Exercised through TurnEngine so it covers the wiring.
 */
class SurvivalTickTest {

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

    @Test
    void wallBumpTicksNoTrack() {
        RunState s = new RunState(1L);
        TurnEngine te = new TurnEngine();
        RoguePlayer p = s.getPlayer();
        p.adjustTemperature(10); // push off Neutral so a tick would be observable

        int hunger = p.getHunger(), thirst = p.getThirst(), temp = p.getTemperature(), clock = s.getClockTurns();
        int[] toWall = placeAgainstWall(s);

        te.advance(s, PlayerAction.move(toWall[0], toWall[1], 0)); // blocked move — no turn commits

        assertEquals(hunger, p.getHunger(), "hunger did not tick on a wall-bump");
        assertEquals(thirst, p.getThirst(), "thirst did not tick");
        assertEquals(temp, p.getTemperature(), "temperature did not tick");
        assertEquals(clock, s.getClockTurns(), "the Day/Night clock did not advance");
    }

    @Test
    void realActionTicksAllFour() {
        RunState s = new RunState(1L);
        s.setWeather(Weather.CLEAR); // Story 1.6 driver: pin so the temperature drift is deterministic
        TurnEngine te = new TurnEngine();
        RoguePlayer p = s.getPlayer();
        p.adjustTemperature(10); // Warm-ish, so the drift-toward-Neutral tick is observable

        int hunger = p.getHunger(), thirst = p.getThirst(), temp = p.getTemperature(), clock = s.getClockTurns();

        te.advance(s, PlayerAction.wait(0)); // a real action commits a turn

        assertEquals(hunger - 1, p.getHunger(), "hunger ticked once");
        assertEquals(thirst - 1, p.getThirst(), "thirst ticked once");
        assertEquals(temp - 1, p.getTemperature(), "temperature drifted one step toward Neutral");
        assertEquals(clock + 1, s.getClockTurns(), "the Day/Night clock advanced one turn");
    }

    @Test
    void lethalTemperatureHonorsLastStandReprieve() {
        // AD-4 ordering pin: the survival ticks run BEFORE checkLastStand, so a lethal
        // exposure tick (Frozen) earns the one-per-run reprieve instead of death. If the
        // pipeline were reordered to checkLastStand first, the reprieve would not fire.
        RunState s = new RunState(1L);
        s.setWeather(Weather.CLEAR); // pin: the Cold Snap driver is irrelevant to the reprieve ordering
        TurnEngine te = new TurnEngine();
        RoguePlayer p = s.getPlayer();
        p.adjustTemperature(-100);    // Frozen — the next acted turn deals -1 HP
        p.hurtRaw(p.getHp() - 1);     // to 1 HP so that single tick is lethal

        TurnResult r = te.advance(s, PlayerAction.wait(0));

        // Ordering proof (Story 4.6): the GRIT check is now probabilistic, but it can only fire
        // AFTER the lethal temperature tick — so isLastStandUsed flipping proves checkLastStand ran
        // post-damage (a pre-tick order would leave Klein alive at 1 HP and never spend the check).
        assertTrue(s.isLastStandUsed(), "the one-per-run GRIT check fires after the temperature tick");
        if (s.isLastStand()) { // the roll succeeded
            assertEquals(1, p.getHp(), "a successful reprieve revives to 1 HP instead of dying");
            assertTrue(r.messages.contains("Last Stand!"), "the reprieve announces itself");
        } else { // the roll failed — honest death, the ordering is still proven above
            assertFalse(p.isAlive(), "a failed GRIT roll lets the death stand");
        }
    }

    // --- Story 1.3 honesty: the clock phase + weather are also acted-turn-only (AD-5, FR-5) ---

    @Test
    void wallBumpAdvancesNeitherClockNorWeather() {
        RunState s = new RunState(1L);
        TurnEngine te = new TurnEngine();
        int clock = s.getClockTurns();
        DayPhase phase = s.getClockPhase();
        Weather weather = s.getWeather();
        int cycle = s.getCycleNumber();
        int[] toWall = placeAgainstWall(s);

        te.advance(s, PlayerAction.move(toWall[0], toWall[1], 0)); // blocked move — no turn commits

        assertEquals(clock, s.getClockTurns(), "the clock did not advance on a wall-bump");
        assertEquals(phase, s.getClockPhase(), "the phase did not change");
        assertEquals(weather, s.getWeather(), "weather did not re-roll");
        assertEquals(cycle, s.getCycleNumber(), "the cycle did not advance");
    }

    @Test
    void realActionAdvancesTheClockAndStaysInDay() {
        RunState s = new RunState(1L);
        TurnEngine te = new TurnEngine();
        int clock = s.getClockTurns();

        te.advance(s, PlayerAction.wait(0)); // a real action commits a turn

        assertEquals(clock + 1, s.getClockTurns(), "a real action advances the clock");
        assertEquals(DayPhase.DAY, s.getClockPhase(), "the run starts in Day and one turn stays Day");
    }
}

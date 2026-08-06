package com.margins.rogue;

import com.margins.rogue.state.RunState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Day/Night phase split (PRD FR-5, Story 1.3 AC-1): Day 100 turns / Night 70 per 170-turn
 * cycle, derived from the persisted clock. The phase is NEVER persisted itself — it derives
 * from clockTurns, so a load can never desync it.
 */
class DayNightClockTest {

    private static RunState state() { return new RunState(1L); }

    @Test
    void dayRunsForTheFirstHundredTurns() {
        RunState s = state();
        assertEquals(DayPhase.DAY, s.getClockPhase(), "turn 0 opens Day");
        assertTrue(s.isDay());
        for (int i = 1; i < 100; i++) s.tickClock();
        assertEquals(99, s.getClockTurns());
        assertEquals(DayPhase.DAY, s.getClockPhase(), "turn 99 is still Day");
    }

    @Test
    void nightRunsFromTurn100Through169() {
        RunState s = state();
        for (int i = 0; i < 100; i++) s.tickClock();
        assertEquals(DayPhase.NIGHT, s.getClockPhase(), "turn 100 opens Night");
        assertFalse(s.isDay());
        for (int i = 0; i < 69; i++) s.tickClock();
        assertEquals(169, s.getClockTurns());
        assertEquals(DayPhase.NIGHT, s.getClockPhase(), "turn 169 is the last Night turn");
    }

    @Test
    void newCycleRestartsAtDay() {
        RunState s = state();
        for (int i = 0; i < 170; i++) s.tickClock();
        assertEquals(170, s.getClockTurns());
        assertEquals(1, s.getCycleNumber(), "turn 170 begins cycle 1");
        assertEquals(DayPhase.DAY, s.getClockPhase(), "turn 170 is Day of the new cycle");
        for (int i = 0; i < 170; i++) s.tickClock();
        assertEquals(340, s.getClockTurns());
        assertEquals(2, s.getCycleNumber(), "turn 340 begins cycle 2");
        assertEquals(DayPhase.DAY, s.getClockPhase(), "turn 340 is Day again");
    }
}

package com.margins.rogue;

import com.margins.rogue.state.RunState;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Per-cycle weather (PRD FR-5, Story 1.3 AC-2): exactly one type is rolled per 170-turn cycle
 * on the weighted distribution (Clear 40 / Rain 25 / Fog 20 / Storm 10 / Cold Snap 5), drawn
 * from the seeded RNG (AD-5). The type is state only — its effects are later stories.
 */
class WeatherSystemTest {

    @Test
    void rollProducesOnlyTheFiveWeatherTypes() {
        Random rng = new Random(7L);
        EnumSet<Weather> seen = EnumSet.noneOf(Weather.class);
        for (int i = 0; i < 10000; i++) seen.add(Weather.roll(rng));
        assertEquals(EnumSet.allOf(Weather.class), seen, "every weather type appears; nothing else can");
    }

    @Test
    void rollIsDeterministicPerSeed() {
        Random a = new Random(42L);
        Random b = new Random(42L);
        for (int i = 0; i < 100; i++) {
            assertEquals(Weather.roll(a), Weather.roll(b), "same seed -> same weather stream (AD-5)");
        }
    }

    @Test
    void initialWeatherIsRolledNotStuckAtClear() {
        // The field default is CLEAR, so a non-CLEAR weather on construction proves the
        // constructor rolled cycle 0's weather from the weighted distribution.
        RunState rolled = null;
        for (long seed = 1; seed < 500 && rolled == null; seed++) {
            RunState candidate = new RunState(seed);
            if (candidate.getWeather() != Weather.CLEAR) rolled = candidate;
        }
        assertNotNull(rolled, "some seed rolls a non-CLEAR weather (the constructor rolled, not the field default)");
    }

    @Test
    void weatherIsConstantWithinACycleAndReRollsAtTheBoundary() {
        // Find a seed whose cycle-0 and cycle-1 weather differ, so the boundary re-roll is
        // actually observable (AC-2). A regression that deletes the re-roll fails this.
        RunState s = boundaryDifferingSeed();
        Weather cycle0 = s.getWeather();
        for (int i = 1; i < 170; i++) {
            s.tickClock();
            assertEquals(cycle0, s.getWeather(), "weather is constant across cycle 0");
            assertEquals(0, s.getCycleNumber(), "still cycle 0 before turn 170");
        }
        s.tickClock(); // turn 170 — a new cycle begins
        assertEquals(1, s.getCycleNumber(), "the boundary detection fires exactly at turn 170");
        assertNotEquals(cycle0, s.getWeather(), "a new cycle rolls a new weather (AC-2)");
    }

    @Test
    void restartResetsClockAndReRollsWeather() {
        RunState s = boundaryDifferingSeed();
        for (int i = 0; i < 200; i++) s.tickClock(); // deep into cycle 1 (Night)
        assertEquals(1, s.getCycleNumber());
        Weather before = s.getWeather();

        s.restart();

        assertEquals(0, s.getClockTurns(), "a fresh run starts at turn 0");
        assertEquals(DayPhase.DAY, s.getClockPhase(), "a fresh run starts in Day");
        assertEquals(0, s.getCycleNumber(), "a fresh run is in cycle 0");
        assertNotEquals(before, s.getWeather(), "restart rolls a different weather (a fresh world)");
    }

    /** A fresh-at-turn-0 RunState whose cycle-0 and cycle-1 weather differ, so the boundary
     *  re-roll is observable. The seed is found by consuming a probe instance (RunState is
     *  deterministic per seed, so a fresh build from the same seed reproduces both rolls). */
    private static RunState boundaryDifferingSeed() {
        for (long seed = 1; seed < 5000; seed++) {
            RunState probe = new RunState(seed);
            Weather c0 = probe.getWeather();
            for (int i = 0; i < 170; i++) probe.tickClock();
            Weather c1 = probe.getWeather();
            if (c1 == c0) continue;
            probe.restart();
            if (probe.getWeather() != c1) return new RunState(seed); // fresh, at turn 0
        }
        throw new IllegalStateException("no seed in 1..4999 differs across the tested weather rolls");
    }
}

package com.margins.rogue;

import com.margins.rogue.state.RunState;
import com.margins.rogue.system.FovSystem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Dynamic player-FOV radius (PRD FR-5, Story 1.4 AC-1/2, AD-18): Night or Fog shrink the
 * sight radius versus the clear-day baseline; a lit source restores it but keeps it below
 * clear day. The shadowcasting is unchanged — only the radius it runs to varies.
 */
class FovRadiusTest {

    /** A RunState whose cycle-0 weather is exactly {@code w} (weather is a seeded per-cycle roll). */
    private static RunState seedWithWeather(Weather w) {
        for (long seed = 1; seed < 10000; seed++) {
            RunState r = new RunState(seed);
            if (r.getWeather() == w) return r;
        }
        throw new IllegalStateException("no seed in 1..9999 rolls cycle-0 weather " + w);
    }

    @Test
    void restoredButReducedInvariantHolds() {
        // AC-2's "restored but reduced": a light must beat dark yet stay under clear day.
        assertTrue(FovSystem.DARK_RADIUS < FovSystem.LIT_RADIUS,
                "a light restores sight above the dark radius");
        assertTrue(FovSystem.LIT_RADIUS < FovSystem.DAY_RADIUS,
                "a light is still reduced versus the clear-day baseline");
    }

    @Test
    void clearDayUsesTheBaselineRadius() {
        RunState s = seedWithWeather(Weather.CLEAR); // turn 0 is Day
        assertTrue(s.isDay());
        assertEquals(FovSystem.DAY_RADIUS, FovSystem.radiusFor(s), "clear day = baseline radius");
    }

    @Test
    void nightShrinksTheRadius() {
        RunState s = seedWithWeather(Weather.CLEAR); // isolate Night from Fog
        for (int i = 0; i < 100; i++) s.tickClock(); // turn 100 = Night, still cycle 0 (weather unchanged)
        assertFalse(s.isDay());
        assertEquals(Weather.CLEAR, s.getWeather());
        assertEquals(FovSystem.DARK_RADIUS, FovSystem.radiusFor(s), "Night shrinks the radius (AC-1)");
    }

    @Test
    void fogShrinksTheRadiusEvenByDay() {
        RunState s = seedWithWeather(Weather.FOG); // turn 0 is Day, but Fog
        assertTrue(s.isDay());
        assertEquals(FovSystem.DARK_RADIUS, FovSystem.radiusFor(s), "Fog shrinks the radius by day too (AC-1)");
    }

    @Test
    void lightInClearDayDoesNotReduceSight() {
        // A fire adds nothing to daylight and must never penalize it: lighting one in clear day
        // keeps the full baseline radius, not the (smaller) lit radius. A light only restores UP.
        RunState s = seedWithWeather(Weather.CLEAR); // turn 0, Day
        assertEquals(FovSystem.DAY_RADIUS, FovSystem.radiusFor(s), "clear day baseline");

        s.setLight(s.getPlayer().getTileX(), s.getPlayer().getTileY());

        assertEquals(FovSystem.DAY_RADIUS, FovSystem.radiusFor(s),
                "a torch in clear day does not shrink sight from 8 to the lit radius");
    }

    @Test
    void lightRestoresTheRadiusAndTakesPrecedenceOverDark() {
        RunState s = seedWithWeather(Weather.FOG);
        for (int i = 0; i < 100; i++) s.tickClock(); // Night AND Fog — the darkest case
        assertEquals(FovSystem.DARK_RADIUS, FovSystem.radiusFor(s), "dark before lighting");

        s.setLight(s.getPlayer().getTileX(), s.getPlayer().getTileY());

        assertEquals(FovSystem.LIT_RADIUS, FovSystem.radiusFor(s),
                "a light restores the radius even at Night in Fog (AC-2, light precedence)");
    }
}

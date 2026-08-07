package com.margins.rogue;

import com.margins.rogue.state.RunState;
import com.margins.rogue.system.TemperatureSystem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Temperature spec (PRD FR-4) — Story 1.6 adds the real drivers the meter was waiting on:
 * the [-100,+100] exposure meter and its 7 bands, HP harm at the extreme bands, and the two
 * drivers — the weather (Cold Snap onset, ~half-rate recovery) and the campfire (warmth toward
 * the WARM-band comfort cap, never into HOT/OVERHEATED). Tests pin the weather with
 * {@code RunState.setWeather} so they are seed-independent.
 */
class TemperatureSystemTest {

    private static RunState state(Weather w) {
        RunState s = new RunState(1L);
        s.setWeather(w);
        return s;
    }

    private static RoguePlayer player() {
        return new RunState(1L).getPlayer();
    }

    @Test
    void startsNeutral() {
        RoguePlayer p = player();
        assertEquals(0, p.getTemperature());
        assertEquals(RoguePlayer.TempBand.NEUTRAL, p.getTempBand());
    }

    @Test
    void bandsMapToTheMeter() {
        assertBand(-100, RoguePlayer.TempBand.FROZEN);
        assertBand(-80, RoguePlayer.TempBand.FROZEN);
        assertBand(-60, RoguePlayer.TempBand.COLD);
        assertBand(-30, RoguePlayer.TempBand.CHILLED);
        assertBand(0, RoguePlayer.TempBand.NEUTRAL);
        assertBand(30, RoguePlayer.TempBand.WARM);
        assertBand(60, RoguePlayer.TempBand.HOT);
        assertBand(90, RoguePlayer.TempBand.OVERHEATED);
    }

    private static void assertBand(int target, RoguePlayer.TempBand expected) {
        RoguePlayer p = player();
        p.adjustTemperature(target); // from 0
        assertEquals(expected, p.getTempBand(), "temp " + target + " → " + expected);
    }

    @Test
    void adjustTemperatureClampsToRange() {
        RoguePlayer p = player();
        p.adjustTemperature(1000);
        assertEquals(100, p.getTemperature(), "clamped to +100");
        p.adjustTemperature(-5000);
        assertEquals(-100, p.getTemperature(), "clamped to -100");
    }

    // --- The driver-less baseline (no Cold Snap, no fire): drift + extreme-band harm ---

    @Test
    void clearWeatherDriftsTowardNeutralWithoutHarm() {
        RunState s = state(Weather.CLEAR);
        RoguePlayer p = s.getPlayer();
        p.adjustTemperature(40); // Warm — not an extreme band
        int hp = p.getHp();
        for (int i = 0; i < 40; i++) TemperatureSystem.tick(s);
        assertEquals(0, p.getTemperature(), "drifts back to Neutral absent a driver");
        assertEquals(RoguePlayer.TempBand.NEUTRAL, p.getTempBand());
        assertEquals(hp, p.getHp(), "no harm outside the extreme bands");
    }

    @Test
    void extremeBandHarmsEveryTurnAndStopsOutside() {
        RunState s = state(Weather.CLEAR);
        RoguePlayer p = s.getPlayer();
        p.adjustTemperature(-100); // Frozen
        int hp = p.getHp();
        TemperatureSystem.tick(s);
        TemperatureSystem.tick(s);
        assertEquals(hp - 2, p.getHp(), "Frozen costs -1 HP per turn spent there");
        while (p.getTempBand() == RoguePlayer.TempBand.FROZEN) TemperatureSystem.tick(s); // drift out
        int hpOut = p.getHp();
        TemperatureSystem.tick(s); // now Cold
        assertEquals(hpOut, p.getHp(), "harm stops once the meter leaves the extreme band");
    }

    // --- Story 1.6: the Cold Snap weather driver (FR-4, AC-1) ---

    @Test
    void coldSnapDropsTwoPerTurnAndReachesFrozenInsideOneNight() {
        RunState s = state(Weather.COLD_SNAP);
        RoguePlayer p = s.getPlayer();
        for (int i = 0; i < 10; i++) TemperatureSystem.tick(s);
        assertEquals(-20, p.getTemperature(), "Cold Snap onset is -2/turn");
        for (int i = 10; i < 40; i++) TemperatureSystem.tick(s);
        assertEquals(RoguePlayer.TempBand.FROZEN, p.getTempBand(),
                "from Neutral, a Cold Snap reaches Frozen (≤ -80) in 40 turns — inside one 70-turn Night");
    }

    @Test
    void coldSnapOnsetAndRecoveryAreTwoToOne() {
        // AC-1: recovery once the Snap ends is +1/turn — half the -2 onset.
        RunState s = state(Weather.COLD_SNAP);
        RoguePlayer p = s.getPlayer();
        for (int i = 0; i < 10; i++) TemperatureSystem.tick(s);
        assertEquals(-20, p.getTemperature(), "10 Cold Snap turns: -20");

        s.setWeather(Weather.CLEAR);
        for (int i = 0; i < 10; i++) TemperatureSystem.tick(s);
        assertEquals(-10, p.getTemperature(), "recovery is +1/turn — half the -2 onset");
    }

    @Test
    void coldSnapDroppingIntoFrozenHarmsThatTurn() {
        // The driver branches harm AFTER their delta: dropping to -80 harms the same turn the
        // player lands in Frozen (a harsher boundary than the neutral path's harm-then-drift).
        RunState s = state(Weather.COLD_SNAP);
        RoguePlayer p = s.getPlayer();
        p.adjustTemperature(-78); // Cold, two below the Frozen line
        int hp = p.getHp();
        TemperatureSystem.tick(s); // -2 → -80 (Frozen)
        assertEquals(hp - 1, p.getHp(), "the post-delta band decides: dropping into Frozen harms now");
    }

    // --- Story 1.6: campfire warmth (FR-7, AC-2) ---

    @Test
    void campfireWarmsAndBeatsAColdSnap() {
        RunState s = state(Weather.COLD_SNAP);
        RoguePlayer p = s.getPlayer();
        s.setCampfire(p.getTileX(), p.getTileY());
        assertTrue(s.isPlayerAtFire());
        for (int i = 0; i < 10; i++) TemperatureSystem.tick(s);
        assertEquals(20, p.getTemperature(),
                "at the fire under a Cold Snap the player nets +2/turn (4 warmth − 2 onset)");
        assertTrue(p.getTemperature() > 0, "the fire 'solves warmth' even in the cold");
    }

    @Test
    void campfireWarmthCapsAtTheWarmBandNeverIntoHot() {
        RunState s = state(Weather.CLEAR);
        RoguePlayer p = s.getPlayer();
        s.setCampfire(p.getTileX(), p.getTileY());
        for (int i = 0; i < 60; i++) TemperatureSystem.tick(s); // well past the cap
        assertEquals(TemperatureSystem.FIRE_COMFORT, p.getTemperature(),
                "warmth caps at the top of the WARM band");
        assertEquals(RoguePlayer.TempBand.WARM, p.getTempBand(),
                "a fire never pushes into HOT/OVERHEATED (AC-2 guard)");
    }

    @Test
    void campfireRequiresThePlayerAtTheFire() {
        // The stationary trade (AC-4): warmth only while on/4-adjacent to the fire tile.
        RunState s = state(Weather.CLEAR);
        RoguePlayer p = s.getPlayer();
        s.setCampfire(p.getTileX(), p.getTileY());
        TemperatureSystem.tick(s);
        int warmed = p.getTemperature();
        assertTrue(warmed > 0, "standing at the fire warms");

        // Walk away: the warmth stops and the meter drifts back toward Neutral.
        p.placeAt(p.getTileX() + 3, p.getTileY() + 3);
        assertFalse(s.isPlayerAtFire());
        for (int i = 0; i < 20; i++) TemperatureSystem.tick(s);
        assertTrue(p.getTemperature() < warmed, "away from the fire the warmth stops and fades");
    }
}

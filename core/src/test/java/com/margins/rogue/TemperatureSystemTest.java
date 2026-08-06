package com.margins.rogue;

import com.margins.rogue.state.RunState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Temperature spec (PRD FR-4) — the meter only (drivers are Stories 1.3/1.6):
 * the [-100,+100] exposure meter and its 7 bands, HP harm at the extreme bands,
 * the driver-less drift toward Neutral, and adjustTemperature clamping.
 */
class TemperatureSystemTest {

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

    @Test
    void extremeBandHarmsEveryTurnAndStopsOutside() {
        RoguePlayer p = player();
        p.adjustTemperature(-100); // Frozen
        int hp = p.getHp();
        p.tickTemperature();
        p.tickTemperature();
        assertEquals(hp - 2, p.getHp(), "Frozen costs -1 HP per turn spent there");
        while (p.getTempBand() == RoguePlayer.TempBand.FROZEN) p.tickTemperature(); // drift out
        int hpOut = p.getHp();
        p.tickTemperature(); // now Cold
        assertEquals(hpOut, p.getHp(), "harm stops once the meter leaves the extreme band");
    }

    @Test
    void driverlessMeterDriftsTowardNeutralWithoutHarm() {
        RoguePlayer p = player();
        p.adjustTemperature(40); // Warm — not an extreme band
        int hp = p.getHp();
        for (int i = 0; i < 40; i++) p.tickTemperature();
        assertEquals(0, p.getTemperature(), "drifts back to Neutral absent a driver");
        assertEquals(RoguePlayer.TempBand.NEUTRAL, p.getTempBand());
        assertEquals(hp, p.getHp(), "no harm outside the extreme bands");
    }
}

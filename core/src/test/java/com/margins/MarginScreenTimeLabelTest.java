package com.margins;

import com.margins.rogue.RoguePlayer;
import com.margins.rogue.Weather;
import com.margins.rogue.state.RunState;
import com.margins.rogue.system.PlayerAction;
import com.margins.rogue.system.TurnEngine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Story 3.3 (AC-2): the top-panel time readout shows the foray budget — the cycle/day number and
 * turns-until-nightfall during the day ("make it home before dark", UJ-2), the raw night clock
 * after dark. A pure string build from core state (AD-1/AD-2 — no Gdx), so it's headless-testable
 * like the other MarginScreen statics (MarginScreenStructureLayerTest precedent).
 */
class MarginScreenTimeLabelTest {

    @Test
    void dayReadoutShowsTheFullBudgetAtDayZero() {
        RunState s = new RunState(42L);
        s.setWeather(Weather.CLEAR);
        String label = MarginScreen.timeLabel(s);
        assertEquals("DAY 0 100T CLEAR", label,
                "Day 0 opens with the cycle number and the full 100-turn budget");
    }

    @Test
    void readoutDerivesAtCycleBoundaries() {
        RunState s = new RunState(42L);
        s.setWeather(Weather.CLEAR);
        TurnEngine engine = new TurnEngine();
        for (int i = 0; i < 75; i++) engine.advance(s, PlayerAction.wait(RoguePlayer.SOUTH));
        assertEquals("DAY 0 25T CLEAR", MarginScreen.timeLabel(s),
                "mid-day the readout shows the remaining budget");

        for (int i = 0; i < 25; i++) engine.advance(s, PlayerAction.wait(RoguePlayer.SOUTH)); // → 100 (Night)
        s.setWeather(Weather.CLEAR); // re-pin: the 170-boundary roll is not this test's concern
        assertEquals("NIGHT 100 CLEAR", MarginScreen.timeLabel(s),
                "in Night the budget is spent — the raw night clock reads");

        for (int i = 0; i < 70; i++) engine.advance(s, PlayerAction.wait(RoguePlayer.SOUTH)); // → 170 (Day 1)
        s.setWeather(Weather.CLEAR); // re-pin after the cycle-boundary roll
        assertEquals("DAY 1 100T CLEAR", MarginScreen.timeLabel(s),
                "a new cycle restores the budget under cycle number 1");
    }

    @Test
    void torchReadoutKeepsTheBudgetAndBurn() {
        RunState s = new RunState(42L);
        s.setWeather(Weather.CLEAR);
        s.lightTorch(60);
        String label = MarginScreen.timeLabel(s);
        assertTrue(label.startsWith("D0 100T"), "a lit torch keeps the day budget in the readout: " + label);
        assertTrue(label.contains("CLEAR"), "the weather still reads beside a torch: " + label);
    }
}

package com.margins;

import com.margins.rogue.Weather;
import com.margins.rogue.state.RunState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MarginScreenEventToneTest {

    @Test
    void routineActionsStayWhite() {
        assertEquals(MarginScreen.EventTone.ROUTINE, MarginScreen.eventToneFor("Wait"));
        assertEquals(MarginScreen.EventTone.ROUTINE,
                MarginScreen.eventToneFor("Picked up Small Tin"));
    }

    @Test
    void weatherAndCycleChangesAreYellow() {
        assertEquals(MarginScreen.EventTone.TIME,
                MarginScreen.eventToneFor(Weather.STORM.onsetLine()));
        assertEquals(MarginScreen.EventTone.TIME,
                MarginScreen.eventToneFor(RunState.LINE_DUSK));
        assertEquals(MarginScreen.EventTone.TIME,
                MarginScreen.eventToneFor(RunState.LINE_DAWN));
    }

    @Test
    void defeatsAreRedAndQuotedSpeechIsOrange() {
        assertEquals(MarginScreen.EventTone.DEFEAT,
                MarginScreen.eventToneFor("Enemy defeated."));
        assertEquals(MarginScreen.EventTone.DEFEAT,
                MarginScreen.eventToneFor("Cause of death: crushed by a collapsing floor."));
        assertEquals(MarginScreen.EventTone.DIALOGUE,
                MarginScreen.eventToneFor("Aldric: \"Keep to the trees.\""));
    }

    @Test
    void theFinalObservedHazardBecomesTheCauseOfDeath() {
        RunState state = new RunState(42L);
        state.appendMessages(java.util.List.of(
                "Wait",
                "The floor groans and a section collapses beneath you."));

        assertEquals("Cause of death: crushed by a collapsing floor.",
                MarginScreen.inferDeathCause(state));
    }

    @Test
    void shortNarrationUsesACompactScrollWhileLongPagesRemainBounded() {
        assertEquals(102, MarginScreen.narrativePanelHeight(4, 0));
        assertEquals(96, MarginScreen.narrativePanelHeight(1, 0));
        assertEquals(252, MarginScreen.narrativePanelHeight(40, 8));
    }
}

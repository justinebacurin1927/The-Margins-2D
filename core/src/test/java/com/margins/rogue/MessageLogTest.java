package com.margins.rogue;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import com.margins.rogue.Companion;
import com.margins.rogue.RogueEnemy;
import com.margins.rogue.item.FloorItem;
import com.margins.rogue.item.Supply;
import com.margins.rogue.state.FlagStore;
import com.margins.rogue.state.RunState;
import com.margins.rogue.system.PlayerAction;
import com.margins.rogue.system.TurnEngine;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The bottom message log (NFR-3, AD-15; Story 1.8 AC-2): a TRANSIENT, bounded list on RunState
 * (AD-3 — the primary text surface is run data, owned by the core). TurnEngine appends each
 * turn's messages (acted AND refused — the log is the text surface for all feedback); the log
 * is cleared on restart and does NOT survive save/load (AD-6).
 */
class MessageLogTest {

    /** Mirrors SaveService.json() element-type registration AND usePrototypes(false) (Story 1.1),
     *  so the test serializer matches production (the divergence would mask save-format bugs). */
    private static Json json() {
        Json json = new Json();
        json.setOutputType(JsonWriter.OutputType.json);
        json.setUsePrototypes(false);
        json.setElementType(RunState.class, "enemies", RogueEnemy.class);
        json.setElementType(RunState.class, "floorItems", FloorItem.class);
        json.setElementType(RunState.class, "companions", Companion.class);
        json.setElementType(FlagStore.class, "flags", Integer.class);
        return json;
    }

    @Test
    void turnMessagesLandInTheLogInOrder() {
        RunState s = new RunState(1L);
        List<String> log = s.getMessageLog();
        assertEquals(1, log.size(), "the log opens seeded (the surface is never empty at first render)");

        new TurnEngine().advance(s, PlayerAction.wait(0));       // acted → "Wait"
        new TurnEngine().advance(s, PlayerAction.buildCampfire(0)); // acted → "Built a campfire."

        assertEquals(3, log.size(), "each acted turn's message appends");
        assertEquals("Wait", log.get(1), "messages land in emission order");
        assertEquals("Built a campfire.", log.get(2));
    }

    @Test
    void logTrimsAtTheCap() {
        RunState s = new RunState(1L);
        TurnEngine te = new TurnEngine();
        int cap = RunState.MESSAGE_LOG_CAP;
        for (int i = 0; i < cap + 20; i++) te.advance(s, PlayerAction.wait(0)); // cap+20 "Wait"s
        assertEquals(cap, s.getMessageLog().size(), "the log is bounded at the cap");
        assertEquals("Wait", s.getMessageLog().get(cap - 1), "the newest message survives the trim");
        assertEquals("Wait", s.getMessageLog().get(0), "the oldest over-cap messages fell off the front");
    }

    @Test
    void restartClearsTheLogAndReseedsIt() {
        RunState s = new RunState(1L);
        new TurnEngine().advance(s, PlayerAction.wait(0));
        new TurnEngine().advance(s, PlayerAction.buildCampfire(0));
        assertTrue(s.getMessageLog().size() > 1, "the log accumulated across turns");
        s.restart();
        assertEquals(1, s.getMessageLog().size(), "a new run clears the log");
        assertEquals("You flee into the pines. Aldric is beside you.", s.getMessageLog().get(0),
                "and re-opens with the opening line");
    }

    @Test
    void refusedActionMessageLandsWithoutCommittingATurn() {
        // A refused action emits feedback but commits no turn (AD-5 honesty) — the log is the
        // surface for BOTH: the refusal message must land while the survival clock stays still.
        RunState s = new RunState(1L);
        assertTrue(s.getInventory().count(Supply.WOOD.ordinal()) == 0
                && s.getInventory().count(Supply.COAL.ordinal()) == 0,
                "a fresh backpack has no torch materials — the refusal is deterministic");
        int clock = s.getClockTurns();
        new TurnEngine().advance(s, PlayerAction.craftTorch(0));
        assertEquals(clock, s.getClockTurns(), "a refused craft commits no turn (AD-5)");
        assertTrue(s.getMessageLog().stream().anyMatch(m -> m.contains("Wood and Coal")),
                "the refusal message still reaches the log");
    }

    @Test
    void messageLogDoesNotSurviveSaveLoad() {
        RunState s = new RunState(1L);
        new TurnEngine().advance(s, PlayerAction.wait(0));
        new TurnEngine().advance(s, PlayerAction.buildCampfire(0));
        assertTrue(s.getMessageLog().size() > 1, "played turns accumulated log entries");

        RunState loaded = json().fromJson(RunState.class, json().toJson(s));
        loaded.restoreAfterLoad();

        assertEquals(1, loaded.getMessageLog().size(),
                "the log is transient — played messages never survive save/load (AD-6)");
        assertNotNull(loaded.getMessageLog(), "field-initialized, so a loaded run is non-null");
    }
}

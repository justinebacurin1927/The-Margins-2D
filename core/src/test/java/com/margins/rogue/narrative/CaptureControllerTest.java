package com.margins.rogue.narrative;

import com.margins.rogue.Companion;
import com.margins.rogue.item.FloorItem;
import com.margins.rogue.item.Inventory;
import com.margins.rogue.item.Supply;
import com.margins.rogue.state.FlagStore;
import com.margins.rogue.state.RunState;
import com.margins.rogue.system.CompanionSystem;
import com.margins.rogue.system.PlayerAction;
import com.margins.rogue.system.TurnEngine;
import com.margins.rogue.system.TurnResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Story 2.4 (FR-3) — Aldric's capture and the rescue seed: the one-shot capture beat, the
 *  discovery note, and the "Klein escapes alone" aftermath. */
class CaptureControllerTest {

    private RunState state() {
        return new RunState(42L);
    }

    private int countNotes(RunState s) {
        int n = 0;
        for (FloorItem it : s.getFloorItems()) {
            if (it.type == Supply.TORN_PAGE.ordinal()) n++;
        }
        return n;
    }

    @Test
    void resolveSetsTheFlagEmptiesThePartyPlantsTheNoteAndAppendsTheBeat() {
        RunState s = state();
        Companion aldric = s.getActiveCompanion();
        assertNotNull(aldric);
        int x = aldric.getTileX(), y = aldric.getTileY();

        new CaptureController().resolve(s);

        // AC-1: captured (a FlagStore flag), not death, and Klein escapes alone.
        assertEquals(1, s.getFlagStore().get(FlagStore.KEY_ALDRIC_CAPTURED));
        assertTrue(s.getCompanions().isEmpty(), "the party is empty — Klein escapes alone");
        assertNull(s.getActiveCompanion());

        // AC-2: the discovery seed lies where Aldric was taken.
        FloorItem note = null;
        for (FloorItem it : s.getFloorItems()) {
            if (it.type == Supply.TORN_PAGE.ordinal()) note = it;
        }
        assertNotNull(note, "a torn page is planted at Aldric's last tile");
        assertEquals(x, note.x);
        assertEquals(y, note.y);

        // The four-line beat, in order (the message-log half of AC-2).
        List<String> log = s.getMessageLog();
        int i = log.indexOf(CaptureController.LINE_CHASE);
        assertTrue(i >= 0, "the chase line appears");
        assertEquals(CaptureController.LINE_TAKE, log.get(i + 1));
        assertEquals(CaptureController.LINE_ALONE, log.get(i + 2));
        assertEquals(CaptureController.LINE_NOTE, log.get(i + 3));
    }

    @Test
    void resolveFiresOnlyOnce() {
        RunState s = state();
        CaptureController c = new CaptureController();
        c.resolve(s);
        int notes = countNotes(s);
        int lines = s.getMessageLog().size();

        c.resolve(s); // a second call (the screen's every-frame gate) is a no-op

        assertEquals(1, s.getFlagStore().get(FlagStore.KEY_ALDRIC_CAPTURED));
        assertEquals(notes, countNotes(s), "no second note");
        assertEquals(lines, s.getMessageLog().size(), "no duplicate beat");
        assertNull(s.getActiveCompanion());
    }

    @Test
    void resolveNoOpsWhenThePartyIsAlreadyEmpty() {
        RunState s = state();
        s.removeActiveCompanion();
        CaptureController c = new CaptureController();

        c.resolve(s);

        assertEquals(0, s.getFlagStore().get(FlagStore.KEY_ALDRIC_CAPTURED));
        assertEquals(0, countNotes(s));
        assertEquals(1, s.getMessageLog().size(), "only the seeded opening line — no capture lines");
    }

    @Test
    void resolveNoOpsWhenTheCompanionIsAlreadyDead() {
        RunState s = state();
        Companion aldric = s.getActiveCompanion();
        aldric.takeDamage(999); // Aldric falls before the tutorial completes — capture is not death (review M2)
        assertFalse(aldric.isAlive());

        new CaptureController().resolve(s);

        assertEquals(0, s.getFlagStore().get(FlagStore.KEY_ALDRIC_CAPTURED), "a dead Aldric is not captured");
        assertEquals(0, countNotes(s), "no seed planted on a corpse tile");
        assertEquals(1, s.getMessageLog().size(), "no capture beat over a corpse");
        assertSame(aldric, s.getActiveCompanion(), "the corpse is not removed either");
    }

    @Test
    void afterTheCaptureTheCompanionSystemsNoOpCleanly() {
        RunState s = state();
        new CaptureController().resolve(s);

        // CompanionSystem.act: no companion, nothing moves, nothing narrates.
        List<String> msgs = new ArrayList<>();
        CompanionSystem.act(s, msgs);
        assertTrue(msgs.isEmpty(), "no behavior-machine messages without a companion");

        // DISTRACT: refused without a companion — the existing refusal line, no turn.
        int clock = s.getClockTurns();
        TurnResult r = new TurnEngine().advance(s, PlayerAction.distract(0));
        assertEquals(clock, s.getClockTurns(), "a refused shout commits no turn");
        assertTrue(r.messages.contains("No companion to call on."), "the existing refusal is honest");
    }

    @Test
    void aldricIsCapturedNotKilled() {
        RunState s = state();
        Companion aldric = s.getActiveCompanion();
        int x = aldric.getTileX(), y = aldric.getTileY();

        new CaptureController().resolve(s);

        // Removed from the party — not a corpse in it — and no body left on his tile.
        assertTrue(s.getCompanions().isEmpty());
        assertFalse(s.isOccupiedByEnemy(x, y), "no corpse enemy where Aldric was taken");
        assertNull(s.getActiveCompanion());
    }

    @Test
    void readingTheTornPageRevealsTheEastLoreAndCostsNoTurn() {
        RunState s = state();
        // Plant the note directly in the backpack (the pickup flow is Epic 1's already-covered concern).
        assertEquals(Inventory.AddResult.ADDED, s.getInventory().tryAdd(Supply.TORN_PAGE.ordinal(), 1));
        TurnEngine engine = new TurnEngine();

        // First read: reveals the east/Copper-Road lore (AC-2), costs no turn, note stays.
        int clock = s.getClockTurns();
        TurnResult r = engine.advance(s, PlayerAction.use(Supply.TORN_PAGE.ordinal(), 0));
        assertEquals(clock, s.getClockTurns(), "reading is narration — no turn");
        assertTrue(r.messages.stream().anyMatch(m -> m.contains("Copper Road")), "the east lore is revealed");
        assertTrue(s.getIdentifyMap().isIdentified(Supply.TORN_PAGE.ordinal()), "the type is known after first read (FR-12)");
        assertEquals(1, s.getInventory().count(Supply.TORN_PAGE.ordinal()), "the note stays as the 2.5 quest seed");

        // Second read: a no-op, still no turn, no repeated lore.
        int clock2 = s.getClockTurns();
        TurnResult r2 = engine.advance(s, PlayerAction.use(Supply.TORN_PAGE.ordinal(), 0));
        assertEquals(clock2, s.getClockTurns());
        assertTrue(r2.messages.stream().anyMatch(m -> m.contains("You've read the note.")));
        assertFalse(r2.messages.stream().anyMatch(m -> m.contains("Copper Road")));
    }
}

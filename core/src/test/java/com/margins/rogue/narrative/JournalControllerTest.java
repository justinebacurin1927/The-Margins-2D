package com.margins.rogue.narrative;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import com.margins.dialog.DialogNode;
import com.margins.dialog.DialogNode.DialogOption;
import com.margins.rogue.Companion;
import com.margins.rogue.item.FloorItem;
import com.margins.rogue.item.Inventory;
import com.margins.rogue.item.Supply;
import com.margins.rogue.narrative.JournalController.JournalEntry;
import com.margins.rogue.narrative.JournalController.QuestStatus;
import com.margins.rogue.state.FlagStore;
import com.margins.rogue.state.RunState;
import com.margins.rogue.system.PlayerAction;
import com.margins.rogue.system.TurnEngine;
import com.margins.rogue.system.TurnResult;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Story 2.5 (FR-19) — quest flags and the passive Journal.
 *
 * <p>AC-1 (discovery auto-start + NPC-line trigger + void-on-giver-death) and AC-2
 * (the Journal is a passive lookup — {@code entries} recomputes from the FlagStore
 * every call and holds no quest-state copy, so it can never drift from the flags or
 * advance a quest). Quest state is a namespaced flag family with the controller's key
 * helpers as the single authority; the NPC-line path rides the ratified SET_FLAG
 * dialogue effect through the real {@link DialogController}. AD-14 (opening the Journal
 * is a suspended text surface — ticks nothing) and AD-6 (the controller is transient;
 * a reloaded run starts with no Journal open and the started flag survives) are pinned
 * the same way DialogueSafePauseTest pins the dialogue scene.
 */
class JournalControllerTest {

    private RunState state() {
        return new RunState(42L);
    }

    /** Mirrors RunStatePersistenceTest.json() — the production serializer (AD-6). */
    private static Json json() {
        Json json = new Json();
        json.setOutputType(JsonWriter.OutputType.json);
        json.setUsePrototypes(false);
        json.setElementType(RunState.class, "enemies", com.margins.rogue.RogueEnemy.class);
        json.setElementType(RunState.class, "floorItems", FloorItem.class);
        json.setElementType(RunState.class, "companions", Companion.class);
        json.setElementType(FlagStore.class, "flags", Integer.class);
        return json;
    }

    // --- AC-1: the discovery trigger (Story 2.4 note -> Story 2.5 quest) ---

    @Test
    void theFirstNoteReadAfterTheCaptureStartsTheRoadEast() {
        RunState s = state();
        // The 2.4 beat: Aldric is taken, the note is planted on his tile.
        new CaptureController().resolve(s);
        assertEquals(1, s.getFlagStore().get(FlagStore.KEY_ALDRIC_CAPTURED));
        // Plant the note in the backpack (the pickup flow is Epic 1's already-covered concern).
        assertEquals(Inventory.AddResult.ADDED, s.getInventory().tryAdd(Supply.TORN_PAGE.ordinal(), 1));

        int clock = s.getClockTurns();
        TurnResult r = new TurnEngine().advance(s, PlayerAction.use(Supply.TORN_PAGE.ordinal(), 0));

        // AC-1: the FIRST read auto-starts the rescue thread via FlagStore.
        assertEquals(1, s.getFlagStore().get(JournalController.startedKey(JournalController.QUEST_ROAD_EAST)));
        assertTrue(r.messages.contains(JournalController.LINE_STARTED), "the start is announced (observation)");
        // Narration, not action: no turn, the note stays as the seed.
        assertEquals(clock, s.getClockTurns(), "reading is narration — no turn");
        assertEquals(1, s.getInventory().count(Supply.TORN_PAGE.ordinal()), "the note stays in the backpack");
        assertEquals(1, Collections.frequency(s.getMessageLog(), JournalController.LINE_STARTED),
                "the start line is emitted exactly once");

        // AC-2: the Journal lists the quest, derived from the flags.
        List<JournalEntry> entries = new JournalController().entries(s);
        assertEquals(1, entries.size(), "the production catalog holds exactly one quest");
        JournalEntry e = entries.get(0);
        assertEquals("The Road East", e.title());
        assertEquals(QuestStatus.ACTIVE, e.status());
        assertTrue(e.objective().contains("Copper Road"), "the objective is the authored rescue text");
    }

    @Test
    void theCaptureFlagIsThePreconditionForStarting() {
        // Without the capture the note still reveals its lore, but no quest exists to start.
        RunState s = state();
        assertEquals(0, s.getFlagStore().get(FlagStore.KEY_ALDRIC_CAPTURED));
        assertEquals(Inventory.AddResult.ADDED, s.getInventory().tryAdd(Supply.TORN_PAGE.ordinal(), 1));

        TurnResult r = new TurnEngine().advance(s, PlayerAction.use(Supply.TORN_PAGE.ordinal(), 0));

        assertTrue(r.messages.stream().anyMatch(m -> m.contains("Copper Road")), "the lore still reveals");
        assertEquals(0, s.getFlagStore().get(JournalController.startedKey(JournalController.QUEST_ROAD_EAST)),
                "the quest cannot exist before Aldric is taken");
        assertTrue(new JournalController().entries(s).isEmpty(), "the Journal shows nothing");
    }

    @Test
    void aLaterReadDoesNotRestartOrReAnnounce() {
        RunState s = state();
        new CaptureController().resolve(s);
        s.getInventory().tryAdd(Supply.TORN_PAGE.ordinal(), 1);
        TurnEngine engine = new TurnEngine();
        engine.advance(s, PlayerAction.use(Supply.TORN_PAGE.ordinal(), 0));
        int logSize = s.getMessageLog().size();

        TurnResult r2 = engine.advance(s, PlayerAction.use(Supply.TORN_PAGE.ordinal(), 0));

        assertTrue(r2.messages.stream().anyMatch(m -> m.contains("You've read the note.")));
        assertFalse(r2.messages.contains(JournalController.LINE_STARTED), "no re-announce");
        assertEquals(logSize + 1, s.getMessageLog().size(), "only the one-line no-op lands");
        assertEquals(1, s.getFlagStore().get(JournalController.startedKey(JournalController.QUEST_ROAD_EAST)),
                "the started flag is not disturbed");
    }

    // --- AC-2: the passive derivation (status precedence: VOIDED > COMPLETED > ACTIVE > unlisted) ---

    @Test
    void entriesReflectTheFlagStoreWithPrecedenceAndHoldNoState() {
        JournalController j = new JournalController();
        j.register(new JournalController.QuestDefinition("trial", "Trial", "do a thing", null));
        RunState s = state();

        assertTrue(j.entries(s).isEmpty(), "a registered-but-unstarted quest does not appear");
        s.getFlagStore().set(JournalController.startedKey("trial"), 1);
        assertEquals(QuestStatus.ACTIVE, j.entries(s).get(0).status());
        s.getFlagStore().set(JournalController.completedKey("trial"), 1);
        assertEquals(QuestStatus.COMPLETED, j.entries(s).get(0).status(), "COMPLETED beats ACTIVE");
        s.getFlagStore().set(JournalController.voidedKey("trial"), 1);
        assertEquals(QuestStatus.VOIDED, j.entries(s).get(0).status(), "VOIDED beats COMPLETED");

        // Passive: the Journal's own state changed nowhere — the derivation is a pure read.
        assertFalse(j.isActive());
        assertTrue(j.entries(s).get(0).status() == QuestStatus.VOIDED);
    }

    @Test
    void unstartedQuestsDoNotAppearInTheProductionCatalog() {
        RunState s = state();
        assertTrue(new JournalController().entries(s).isEmpty(),
                "a fresh run has no started quest, so the Journal is empty");
    }

    // --- AC-1: the NPC-line trigger rides the ratified dialogue SET_FLAG effect ---

    @Test
    void anNpcLineStartsAGiverQuestThroughTheRealDialogController() {
        JournalController j = new JournalController();
        j.register(new JournalController.QuestDefinition("giverQuest", "Giver's Errand", "fetch it", "oldfen"));
        RunState s = state();
        DialogController c = new DialogController();
        DialogNode errand = new DialogNode("Do me a service.",
                new DialogOption("I accept.", new DialogNode("Good.")
                        .withFlag(JournalController.startedKey("giverQuest"), 1)));
        c.start(new DialogNode("Old Fen nods.",
                new DialogOption("What errand?", errand)), s);
        assertTrue(j.entries(s).isEmpty(), "not started until the line is chosen");

        c.select(0, s); // "What errand?" -> errand node
        c.select(0, s); // "I accept." -> leaf carrying the SetFlag

        assertEquals(1, s.getFlagStore().get(JournalController.startedKey("giverQuest")),
                "the NPC line set the quest-started flag through the real effect pipeline");
        assertEquals(QuestStatus.ACTIVE, j.entries(s).get(0).status());
    }

    // --- AC-1: void-on-giver-death (the killer is deferred — Epic 2 has no named NPC combat) ---

    @Test
    void aDeadGiverVoidsTheQuest() {
        JournalController j = new JournalController();
        j.register(new JournalController.QuestDefinition("giverQuest", "Giver's Errand", "fetch it", "oldfen"));
        RunState s = state();
        s.getFlagStore().set(JournalController.startedKey("giverQuest"), 1);
        assertEquals(QuestStatus.ACTIVE, j.entries(s).get(0).status());

        s.getFlagStore().set(JournalController.giverDeadKey("oldfen"), 1);
        assertEquals(QuestStatus.VOIDED, j.entries(s).get(0).status(),
                "a giver killed voids the quest regardless of completion (AC-1)");
    }

    @Test
    void aScriptedVoidWinsOverCompletion() {
        JournalController j = new JournalController();
        j.register(new JournalController.QuestDefinition("giverQuest", "Giver's Errand", "fetch it", "oldfen"));
        RunState s = state();
        s.getFlagStore().set(JournalController.startedKey("giverQuest"), 1);
        s.getFlagStore().set(JournalController.completedKey("giverQuest"), 1);
        s.getFlagStore().set(JournalController.voidedKey("giverQuest"), 1);
        assertEquals(QuestStatus.VOIDED, j.entries(s).get(0).status(),
                "the scripted void is the top precedence (Decision 8)");
    }

    // --- AD-14: the Journal is a suspended text surface ---

    @Test
    void openingAndClosingTheJournalTicksNothing() {
        RunState s = state();
        JournalController j = new JournalController();
        int clock = s.getClockTurns();
        int hunger = s.getPlayer().getHunger();
        int thirst = s.getPlayer().getThirst();
        int temp = s.getPlayer().getTemperature();
        int hp = s.getPlayer().getHp();

        j.open();
        assertTrue(j.isActive());
        j.close();
        assertFalse(j.isActive());

        assertEquals(clock, s.getClockTurns(), "no turn while the Journal is open (AD-14)");
        assertEquals(hunger, s.getPlayer().getHunger(), "hunger does not tick");
        assertEquals(thirst, s.getPlayer().getThirst(), "thirst does not tick");
        assertEquals(temp, s.getPlayer().getTemperature(), "temperature does not drift");
        assertEquals(hp, s.getPlayer().getHp(), "no combat/last-stand resolution runs");
    }

    // --- AD-6: the controller is transient view-session state, nothing serialized ---

    @Test
    void aReloadedRunStartsWithNoJournalOpenAndTheStartedFlagSurvives() {
        RunState s = state();
        new CaptureController().resolve(s);
        s.getInventory().tryAdd(Supply.TORN_PAGE.ordinal(), 1);
        new TurnEngine().advance(s, PlayerAction.use(Supply.TORN_PAGE.ordinal(), 0));

        RunState loaded = json().fromJson(RunState.class, json().toJson(s));
        loaded.restoreAfterLoad();

        JournalController j = new JournalController(); // a fresh controller, like a fresh screen
        assertFalse(j.isActive(), "a reloaded run starts with no Journal open");
        assertEquals(1, loaded.getFlagStore().get(JournalController.startedKey(JournalController.QUEST_ROAD_EAST)),
                "the started quest survives the round-trip (AD-6)");
        assertEquals(QuestStatus.ACTIVE, j.entries(loaded).get(0).status(),
                "the Journal re-derives from the reloaded flags");
    }
}

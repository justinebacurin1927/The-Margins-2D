package com.margins.rogue.narrative;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import com.margins.dialog.DialogEffect;
import com.margins.dialog.DialogNode;
import com.margins.dialog.DialogNode.DialogOption;
import com.margins.dialog.GateStat;
import com.margins.rogue.Companion;
import com.margins.rogue.RogueEnemy;
import com.margins.rogue.item.FloorItem;
import com.margins.rogue.item.Supply;
import com.margins.rogue.state.FlagStore;
import com.margins.rogue.state.RunState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Story 2.1 (AD-14 + carry #5): safe pause and the dialogue authoring contract.
 *
 * <p>Safe pause — a full scene through the controller ticks nothing: the clock and
 * the four survival tracks are unchanged by reading and choosing (mirroring the
 * AD-5 honesty pins), and an effect's outcome is observed in the log while the
 * turn loop is still suspended (the 1.8 silent-mutation lesson).
 *
 * <p>Authoring-contract hardening (the old engine's deferred findings): >4-option
 * nodes navigate, null labels never crash, and scene keys stay namespaced /
 * single-authority.
 */
class DialogueSafePauseTest {

    private RunState run() { return new RunState(42L); }

    /** Mirrors RunStatePersistenceTest.json() — the production serializer (AD-6), so the
     *  test's round-trip matches production (a divergence would mask save-format bugs). */
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

    // --- safe pause (AD-14) ---

    @Test
    void readingAndChoosingATurnTicksNothing() {
        RunState s = run();
        int clock = s.getClockTurns();
        int hunger = s.getPlayer().getHunger();
        int thirst = s.getPlayer().getThirst();
        int temp = s.getPlayer().getTemperature();
        int hp = s.getPlayer().getHp();

        // A full scene: advance twice (an ungated hop + a close), like a real conversation.
        DialogController c = new DialogController();
        DialogNode leaf = new DialogNode("leaf", new DialogOption("close", null));
        c.start(new DialogNode("root",
                new DialogOption("ungated", leaf),
                new DialogOption("gated", GateStat.VOICE, 2, leaf, null)), s);
        c.select(0, s); // -> leaf
        c.select(0, s); // -> close (null next)
        assertFalse(c.isActive(), "the scene closes cleanly");

        assertEquals(clock, s.getClockTurns(), "no turn is committed while the scene is open (AD-14)");
        assertEquals(hunger, s.getPlayer().getHunger(), "hunger does not tick");
        assertEquals(thirst, s.getPlayer().getThirst(), "thirst does not tick");
        assertEquals(temp, s.getPlayer().getTemperature(), "temperature does not drift");
        assertEquals(hp, s.getPlayer().getHp(), "no combat/last-stand resolution runs");
    }

    @Test
    void anEffectIsObservedWhileTheTurnLoopIsStillSuspended() {
        // The 1.8 observation pin, at the safe-pause seam: an effect's line lands in the log
        // immediately, while the clock still stands still.
        RunState s = run();
        int clock = s.getClockTurns();
        DialogController c = new DialogController();
        DialogNode node = new DialogNode("bond", new DialogOption("close", null))
                .withEffect(new DialogEffect.Bond(FlagStore.BOND_TAG_HONEST));
        c.start(node, s);
        assertEquals(1, s.getFlagStore().getBond(), "the effect fires on node entry");
        assertTrue(s.getMessageLog().contains("He warms to you."), "the effect is observed in the log");
        assertEquals(clock, s.getClockTurns(), "and still no turn was committed");
    }

    // --- authoring contract (carry #5) ---

    @Test
    void moreThanFourOptionsNavigateFine() {
        // The old engine capped choices at 4; the controller navigates any count.
        RunState s = run();
        DialogController c = new DialogController();
        DialogOption[] opts = new DialogOption[6];
        for (int i = 0; i < opts.length; i++) opts[i] = new DialogOption("choice " + i, null);
        DialogNode root = new DialogNode("six options", opts);
        c.start(root, s);
        assertEquals(6, root.options.length, "a >4-option node is authored without a cap");
        c.select(5, s);
        assertFalse(c.isActive(), "the 6th choice (index 5) resolves normally");
    }

    @Test
    void aNullOptionLabelNeverThrows() {
        RunState s = run();
        DialogController c = new DialogController();
        DialogNode leaf = new DialogNode("leaf", new DialogOption("close", null));
        DialogNode root = new DialogNode("root", new DialogOption(null, leaf)); // null label
        c.start(root, s);
        assertDoesNotThrow(() -> c.select(0, s), "navigation is by index — a null label never crashes");
        assertSame(leaf, c.getCurrent(), "the null-labeled option still advances");
    }

    @Test
    void sceneKeysAreNamespacedPerScene() {
        // The authoring-contract key rule (the old multi-cache collision finding): keys are
        // per-scene and single-authority — two scenes never share a namespace.
        assertEquals("scene.smoke.read", SampleDialog.KEY_SMOKE_READ);
        assertNotEquals(SceneEffects.KEY_CACHE_REVEALED, SampleDialog.KEY_SMOKE_READ,
                "two scenes never share a key namespace");
    }

    // --- serialization (AD-6) ---

    @Test
    void aReloadedRunStartsWithNoSceneOpenAndRoundTripsEffectState() {
        // Story 2.1 adds no persisted field: the controller/surface are transient (a reloaded
        // run has no open scene), while the stores effects write to (FlagStore, Inventory)
        // round-trip unchanged.
        RunState s = run();
        s.getFlagStore().set("disposition.aldric", 2);
        s.getFlagStore().adjustBond(-1);
        s.getInventory().tryAdd(Supply.COAL.ordinal(), 1);

        RunState loaded = json().fromJson(RunState.class, json().toJson(s));
        loaded.restoreAfterLoad();

        DialogController c = new DialogController(); // a fresh controller, like a fresh screen
        assertFalse(c.isActive(), "a reloaded run starts with no scene open");
        assertEquals(2, loaded.getFlagStore().get("disposition.aldric"), "disposition survives");
        assertEquals(-1, loaded.getFlagStore().getBond(), "Bond survives");
        assertEquals(1, loaded.getInventory().count(Supply.COAL.ordinal()), "the given item survives");
    }
}

package com.margins.rogue.narrative;

import com.margins.dialog.DialogEffect;
import com.margins.dialog.DialogNode;
import com.margins.dialog.DialogNode.DialogOption;
import com.margins.rogue.item.Inventory;
import com.margins.rogue.item.Supply;
import com.margins.rogue.state.FlagStore;
import com.margins.rogue.state.RunState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Story 2.1 (AC-2): the node-entry effect model. Each player-facing effect kind
 * mutates its store AND emits an SPD-tone line to the message log (1.8 observation
 * discipline — no silent mutation). {@code withFlag} (SET_FLAG) is scene
 * bookkeeping and writes silently by design (the node text is the observation).
 */
class DialogueEffectTest {

    private RunState run() { return new RunState(42L); }

    @Test
    void setFlagWritesTheExactValue() {
        // Authoring-contract hardening (carry #5, non-1 flag values): != 0 is the truth
        // test, never coerced to 1.
        RunState s = run();
        DialogController c = new DialogController();
        DialogNode flagged = new DialogNode("sets", new DialogOption("go", null)).withFlag("scene.test", 7);
        c.start(flagged, s);
        assertEquals(7, s.getFlagStore().get("scene.test"), "withFlag writes the exact value, not 1");
    }

    @Test
    void setFlagIsSilentBookkeeping() {
        // A node whose only effect is a flag write appends nothing — the flag is observed
        // through the scene continuing, not a log line (documented Design Decision). The fresh
        // log already holds the seeded opening line, so "no line" means "size unchanged".
        RunState s = run();
        int before = s.getMessageLog().size();
        DialogController c = new DialogController();
        DialogNode flagged = new DialogNode("sets", new DialogOption("go", null)).withFlag("scene.test", 1);
        c.start(flagged, s);
        assertEquals(before, s.getMessageLog().size(), "SET_FLAG alone adds no log line");
    }

    @Test
    void bondHonestGainsAndEmits() {
        RunState s = run();
        DialogController c = new DialogController();
        DialogNode node = new DialogNode("bond", new DialogOption("go", null))
                .withEffect(new DialogEffect.Bond(FlagStore.BOND_TAG_HONEST));
        c.start(node, s);
        assertEquals(1, s.getFlagStore().getBond(), "an honest choice raises Bond by 1");
        assertTrue(s.getMessageLog().contains("He warms to you."), "the Bond shift is observed in the log");
    }

    @Test
    void bondDismissiveLosesAndEmits() {
        RunState s = run();
        DialogController c = new DialogController();
        DialogNode node = new DialogNode("bond", new DialogOption("go", null))
                .withEffect(new DialogEffect.Bond(FlagStore.BOND_TAG_DISMISSIVE));
        c.start(node, s);
        assertEquals(-1, s.getFlagStore().getBond(), "a dismissive choice lowers Bond by 1");
        assertTrue(s.getMessageLog().contains("His eyes narrow."), "the Bond shift is observed in the log");
    }

    @Test
    void bondUnknownTagMutatesNothingAndEmitsNothing() {
        RunState s = run();
        DialogController c = new DialogController();
        DialogNode node = new DialogNode("bond", new DialogOption("go", null))
                .withEffect(new DialogEffect.Bond("bond.nonexistent"));
        int before = s.getMessageLog().size();
        c.start(node, s);
        assertEquals(0, s.getFlagStore().getBond(), "an unknown tag is a no-op");
        assertEquals(before, s.getMessageLog().size(), "a no-op emits no line");
    }

    @Test
    void giveItemAddsToTheBackpackAndEmits() {
        RunState s = run();
        DialogController c = new DialogController();
        DialogNode node = new DialogNode("gift", new DialogOption("go", null))
                .withEffect(new DialogEffect.GiveItem(Supply.COAL.ordinal(), 1));
        c.start(node, s);
        assertEquals(1, s.getInventory().count(Supply.COAL.ordinal()), "the coal lands in the backpack");
        assertTrue(s.getMessageLog().contains("He hands you a Coal."), "the gift is observed in the log");
    }

    @Test
    void giveItemWithAFullPackEmitsRefusalAndAddsNothing() {
        RunState s = run();
        // Fill all 8 stacks with types that are NOT Coal (ordinal 5) so the gift has nowhere
        // to stack or land.
        int[] fillers = {0, 1, 2, 3, 4, 6, 7, 8};
        for (int t : fillers) {
            assertEquals(Inventory.AddResult.ADDED, s.getInventory().tryAdd(t, 1), "each filler stack lands");
        }
        assertTrue(s.getInventory().isBackpackFull(), "the pack is full before the gift");

        DialogController c = new DialogController();
        DialogNode node = new DialogNode("gift", new DialogOption("go", null))
                .withEffect(new DialogEffect.GiveItem(Supply.COAL.ordinal(), 1));
        c.start(node, s);
        assertEquals(0, s.getInventory().count(Supply.COAL.ordinal()), "nothing is added when the pack is full");
        assertTrue(s.getMessageLog().contains("No room in your pack."), "the refusal is observed in the log");
    }

    @Test
    void takeItemRemovesAndEmits() {
        RunState s = run();
        s.getInventory().tryAdd(Supply.COAL.ordinal(), 1);
        DialogController c = new DialogController();
        DialogNode node = new DialogNode("give", new DialogOption("go", null))
                .withEffect(new DialogEffect.TakeItem(Supply.COAL.ordinal(), 1));
        c.start(node, s);
        assertEquals(0, s.getInventory().count(Supply.COAL.ordinal()), "the coal leaves the backpack");
        assertTrue(s.getMessageLog().contains("You give him the Coal."), "the giving is observed in the log");
    }

    @Test
    void takeItemWithoutEnoughEmitsAndChangesNothing() {
        RunState s = run();
        DialogController c = new DialogController();
        DialogNode node = new DialogNode("give", new DialogOption("go", null))
                .withEffect(new DialogEffect.TakeItem(Supply.COAL.ordinal(), 1));
        c.start(node, s);
        assertEquals(0, s.getInventory().count(Supply.COAL.ordinal()), "nothing is removed when absent");
        assertTrue(s.getMessageLog().contains("You don't have the Coal."), "the failure is observed in the log");
    }

    @Test
    void dispositionShiftsTheNamedCounterAndEmits() {
        RunState s = run();
        DialogController c = new DialogController();
        DialogNode node = new DialogNode("read", new DialogOption("go", null))
                .withEffect(new DialogEffect.Disposition("aldric", 1));
        c.start(node, s);
        assertEquals(1, s.getFlagStore().get("disposition.aldric"), "the disposition counter shifts");
        assertTrue(s.getMessageLog().contains("He warms to you."), "the shift is observed in the log");
    }

    @Test
    void dispositionNegativeShiftsAndEmits() {
        RunState s = run();
        DialogController c = new DialogController();
        DialogNode node = new DialogNode("snap", new DialogOption("go", null))
                .withEffect(new DialogEffect.Disposition("aldric", -1));
        c.start(node, s);
        assertEquals(-1, s.getFlagStore().get("disposition.aldric"), "a negative delta cools the NPC");
        assertTrue(s.getMessageLog().contains("His eyes narrow."), "the shift is observed in the log");
    }
}

package com.margins.rogue.narrative;

import com.margins.dialog.DialogNode;
import com.margins.dialog.DialogNode.DialogOption;
import com.margins.dialog.GateStat;
import com.margins.rogue.state.RunState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Epic 5 dialogue engine: branching + turn-loop suspension seam (5.1), the
 * deterministic INSTINCT gate (5.2), and node-entry flag writes (5.3).
 * A fresh RunState's player has INSTINCT 7.
 */
class DialogControllerTest {

    private RunState run() { return new RunState(42L); }

    // --- 5.1: branching / navigation ---

    @Test
    void startOpensAndSelectAdvances() {
        RunState s = run();
        DialogController c = new DialogController();
        DialogNode leaf = new DialogNode("leaf");
        DialogNode root = new DialogNode("root", new DialogOption("go", leaf));
        c.start(root, s);
        assertTrue(c.isActive());
        assertSame(root, c.getCurrent());
        c.select(0, s);
        assertSame(leaf, c.getCurrent(), "select advances to the linked node");
    }

    @Test
    void choiceWithNullNextClosesTheScene() {
        RunState s = run();
        DialogController c = new DialogController();
        c.start(new DialogNode("root", new DialogOption("leave", null)), s);
        c.select(0, s);
        assertFalse(c.isActive(), "a null-next choice ends the scene");
    }

    @Test
    void outOfRangeAndInactiveSelectsAreNoOps() {
        RunState s = run();
        DialogController c = new DialogController();
        DialogNode root = new DialogNode("root", new DialogOption("go", null));
        c.start(root, s);
        c.select(5, s);
        c.select(-1, s);
        assertSame(root, c.getCurrent(), "out-of-range indices do nothing");
        c.end();
        assertDoesNotThrow(() -> c.select(0, s), "selecting with no scene open is a no-op");
    }

    // --- 5.2: deterministic INSTINCT gate (AD-8) ---

    @Test
    void gatePassesAtOrAboveThreshold() {
        RunState s = run(); // instinct 7
        DialogController c = new DialogController();
        DialogNode ok = new DialogNode("SUCCESS");
        DialogNode no = new DialogNode("FAIL");
        c.start(new DialogNode("g", new DialogOption("read", GateStat.INSTINCT, 5, ok, no)), s);
        c.select(0, s);
        assertSame(ok, c.getCurrent(), "7 >= 5 routes to success");
    }

    @Test
    void gateFailsBelowThreshold() {
        RunState s = run(); // instinct 7
        DialogController c = new DialogController();
        DialogNode ok = new DialogNode("SUCCESS");
        DialogNode no = new DialogNode("FAIL");
        c.start(new DialogNode("g", new DialogOption("read", GateStat.INSTINCT, 9, ok, no)), s);
        c.select(0, s);
        assertSame(no, c.getCurrent(), "7 < 9 routes to failure");
    }

    @Test
    void gateBoundaryIsSuccess() {
        RunState s = run(); // instinct 7
        DialogController c = new DialogController();
        DialogNode ok = new DialogNode("SUCCESS");
        DialogNode no = new DialogNode("FAIL");
        c.start(new DialogNode("g", new DialogOption("read", GateStat.INSTINCT, 7, ok, no)), s);
        c.select(0, s);
        assertSame(ok, c.getCurrent(), "instinct == threshold passes (>=)");
    }

    @Test
    void ungatedChoiceIgnoresInstinct() {
        RunState s = run();
        DialogController c = new DialogController();
        DialogNode leaf = new DialogNode("leaf");
        c.start(new DialogNode("root", new DialogOption("go", leaf)), s);
        c.select(0, s);
        assertSame(leaf, c.getCurrent(), "an ungated option never consults instinct");
    }

    // --- 5.3: node-entry flag write (AD-7) ---

    @Test
    void enteringAFlaggedNodeWritesThroughTheFlagStore() {
        RunState s = run();
        DialogController c = new DialogController();
        DialogNode flagged = new DialogNode("sets").withFlag("scene.test", 1);
        c.start(flagged, s);
        assertEquals(1, s.getFlagStore().get("scene.test"), "entering the node wrote the flag");
    }

    @Test
    void aNodeWithoutAFlagWritesNothing() {
        RunState s = run();
        DialogController c = new DialogController();
        c.start(new DialogNode("plain", new DialogOption("go", null)), s);
        assertEquals(0, s.getFlagStore().get("scene.test"));
    }
}

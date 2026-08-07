package com.margins.rogue.narrative;

import com.margins.dialog.DialogNode;
import com.margins.dialog.DialogNode.DialogOption;
import com.margins.dialog.GateStat;
import com.margins.rogue.state.RunState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Story 2.1 (FR-19): the stat-agnostic gate. A gated choice routes by the gating
 * stat — VOICE primary, occasional INSTINCT — deterministically (stat >= threshold
 * → success branch, below → fail branch; no dice). A fresh RunState's player has
 * INSTINCT 7 and VOICE 3.
 */
class DialogueGateTest {

    private RunState run() { return new RunState(42L); }

    @Test
    void voiceGatePassesAtOrAboveThreshold() {
        RunState s = run(); // voice 3
        DialogController c = new DialogController();
        DialogNode ok = new DialogNode("SUCCESS");
        DialogNode no = new DialogNode("FAIL");
        c.start(new DialogNode("g", new DialogOption("talk", GateStat.VOICE, 2, ok, no)), s);
        c.select(0, s);
        assertSame(ok, c.getCurrent(), "VOICE 3 >= 2 routes to success");
    }

    @Test
    void voiceGateFailsBelowThreshold() {
        RunState s = run(); // voice 3
        DialogController c = new DialogController();
        DialogNode ok = new DialogNode("SUCCESS");
        DialogNode no = new DialogNode("FAIL");
        c.start(new DialogNode("g", new DialogOption("talk", GateStat.VOICE, 4, ok, no)), s);
        c.select(0, s);
        assertSame(no, c.getCurrent(), "VOICE 3 < 4 routes to failure");
    }

    @Test
    void voiceGateBoundaryIsSuccess() {
        RunState s = run(); // voice 3
        DialogController c = new DialogController();
        DialogNode ok = new DialogNode("SUCCESS");
        DialogNode no = new DialogNode("FAIL");
        c.start(new DialogNode("g", new DialogOption("talk", GateStat.VOICE, 3, ok, no)), s);
        c.select(0, s);
        assertSame(ok, c.getCurrent(), "VOICE == threshold passes (>=)");
    }

    @Test
    void instinctGateStillResolvesByInstinct() {
        RunState s = run(); // instinct 7
        DialogController c = new DialogController();
        DialogNode ok = new DialogNode("SUCCESS");
        DialogNode no = new DialogNode("FAIL");
        c.start(new DialogNode("g", new DialogOption("read", GateStat.INSTINCT, 7, ok, no)), s);
        c.select(0, s);
        assertSame(ok, c.getCurrent(), "INSTINCT 7 >= 7 passes (boundary)");
    }

    @Test
    void gatedChoiceNeverConsultsTheOtherStat() {
        // A VOICE gate must be decided by VOICE (3), never INSTINCT (7) — the 3 < 4 fail
        // would have passed if the gate read the wrong stat.
        RunState s = run();
        DialogController c = new DialogController();
        DialogNode ok = new DialogNode("SUCCESS");
        DialogNode no = new DialogNode("FAIL");
        c.start(new DialogNode("g", new DialogOption("talk", GateStat.VOICE, 4, ok, no)), s);
        c.select(0, s);
        assertSame(no, c.getCurrent(), "a VOICE gate is decided by VOICE (3), not INSTINCT (7)");
    }

    @Test
    void ungatedChoiceIgnoresBothStats() {
        RunState s = run();
        DialogController c = new DialogController();
        DialogNode leaf = new DialogNode("leaf");
        c.start(new DialogNode("root", new DialogOption("go", leaf)), s);
        c.select(0, s);
        assertSame(leaf, c.getCurrent(), "an ungated option never consults a stat");
    }
}

package com.margins.rogue;

import com.margins.rogue.state.RunState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Story 1.8 AC-1 + Story 1.7 deferral F-A1: {@code RoguePlayer.getActiveDebuffLabels()} — the
 * composite debuff query seam the HUD renders. It composes ONLY the closed debuff shape (no new
 * flags — spine line 186); the hidden Honeymoon countdown NEVER appears (Story 1.7 AC-2 keeps it
 * silent), even while it ticks toward Collapse.
 */
class DebuffLabelTest {

    private static RoguePlayer player() { return new RunState(1L).getPlayer(); }

    @Test
    void cleanPlayerHasNoLabels() {
        assertTrue(player().getActiveDebuffLabels().isEmpty(), "a fresh player is clean");
    }

    @Test
    void nauseaComposesWithItsTimer() {
        RoguePlayer p = player();
        p.beginBacterial(); // Nausea 30 + the parallel Diarrhea Stage 1 30
        assertEquals(List.of("Nausea (30)", "Diarrhea (30)"), p.getActiveDebuffLabels());
    }

    @Test
    void feverComposesWithItsTimer() {
        RoguePlayer p = player();
        p.escalateToFever();
        assertEquals(List.of("Fever (25)"), p.getActiveDebuffLabels());
    }

    @Test
    void deliriumComposesWithItsTimer() {
        RoguePlayer p = player();
        p.escalateToDelirium();
        assertEquals(List.of("Delirium (40)"), p.getActiveDebuffLabels());
    }

    @Test
    void treatedDeliriumStillShowsWithTheShortenedTimer() {
        RoguePlayer p = player();
        p.escalateToDelirium();
        p.treatDelirium(); // ×0.25: 40 → 10
        assertEquals(List.of("Delirium (10)"), p.getActiveDebuffLabels(),
                "a treated Delirium is still active — the label shows the short course");
    }

    @Test
    void diarrheaStageTwoLatchesAtZeroInTheLabel() {
        RoguePlayer p = player();
        p.escalateDiarrhea();
        assertEquals(List.of("Diarrhea (0)"), p.getActiveDebuffLabels(),
                "Stage 2's latched timer reads 0 — the 3× drain that never ends");
    }

    @Test
    void rotgutComposesTheCrippleLabel() {
        RoguePlayer p = player();
        p.beginRotgut(); // Nausea + Diarrhea Stage 1 + the instant Crippled bundle
        assertEquals(List.of("Nausea (30)", "Diarrhea (30)", "Rotgut"), p.getActiveDebuffLabels());
    }

    @Test
    void collapseComposesTheMaxHpCap() {
        RoguePlayer p = player();
        p.collapse(); // base 20 capped at 40%
        assertEquals(8, p.getMaxHp());
        assertEquals(List.of("Collapsed (max HP 8)"), p.getActiveDebuffLabels());
    }

    @Test
    void honeymoonCountdownNeverLeaksIntoTheLabels() {
        RoguePlayer p = player();
        p.beginRotgut();   // a visible debuff stack…
        p.beginHoneymoon(); // …while the hidden countdown ticks alongside it
        assertEquals(60, p.getHoneymoonCountdown(), "the countdown IS active");
        assertEquals(List.of("Nausea (30)", "Diarrhea (30)", "Rotgut"), p.getActiveDebuffLabels(),
                "and the labels are exactly the visible debuffs — no countdown line (AC-2)");
    }

    @Test
    void nourishOutClearsNauseaAndFeverFromTheLabels() {
        RoguePlayer p = player();
        p.beginBacterial(); // Nausea + Diarrhea
        p.eat(10); // eating settles the recoverable sickness (AC-3)
        assertEquals(List.of("Diarrhea (30)"), p.getActiveDebuffLabels(),
                "Nausea clears; Diarrhea is not recoverable by nourishment");

        RoguePlayer p2 = player();
        p2.escalateToFever();
        p2.eat(10);
        assertEquals(List.of(), p2.getActiveDebuffLabels(), "Fever clears the same way");
    }
}

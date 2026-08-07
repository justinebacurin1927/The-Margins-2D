package com.margins.rogue.narrative;

import com.margins.dialog.DialogEffect;
import com.margins.dialog.DialogNode;
import com.margins.dialog.DialogNode.DialogOption;
import com.margins.dialog.GateStat;
import com.margins.rogue.item.Supply;
import com.margins.rogue.state.FlagStore;

/**
 * The Story 2.1 smoke scene — a verification seam for the dialogue mechanic
 * (FR-6 branching, safe pause, gated choices, node-entry effects), opened by the
 * debug N key in {@code MarginScreen}. NOT shippable content: the real intro is
 * Story 2.2, which removes this debug key.
 *
 * <p>Every AC-2 capability is exercised here: a zero-option terminal node (Decision 7 — a text
 * page closed by SPACE/E), an ungated advance, a VOICE-gated choice AND an INSTINCT-gated choice
 * (a fresh player's fixed stats make the PASS branches the reachable in-game tour; both pass/fail
 * routes are pinned headlessly by {@code DialogueGateTest}), a node with a NON-1 flag value
 * (authoring-contract hardening — {@code != 0} is the truth test), a Bond effect, an item-give
 * effect, and a disposition effect.
 */
public final class SampleDialog {

    /** The smoke scene's flag key — namespaced under the scene (single-authority, AD-7). */
    public static final String KEY_SMOKE_READ = "scene.smoke.read";

    private SampleDialog() {}

    public static DialogNode build() {
        DialogNode end = new DialogNode(
                "\"Safe roads, then.\"",
                new DialogOption("Leave", null));

        // The VOICE-gated telling (FR-19, VOICE primary): a fresh player's VOICE 3 >= 2 passes
        // → Bond +1 (honest). A cold-distant player (< 2) fails onto the same end.
        DialogNode honest = new DialogNode(
                "He nods slowly. \"Magdalene's letter. That's why you keep going.\"",
                new DialogOption("Leave", end))
                .withSpeaker("Aldric")
                .withEffect(new DialogEffect.Bond(FlagStore.BOND_TAG_HONEST));

        // Aldric offers a coal (GIVE_ITEM) — a full pack emits "No room in your pack."
        DialogNode coal = new DialogNode(
                "\"Coal's worth its weight now. Take it.\"",
                new DialogOption("Take the coal.", end))
                .withSpeaker("Aldric")
                .withEffect(new DialogEffect.GiveItem(Supply.COAL.ordinal(), 1));

        // The INSTINCT-gated read (FR-19 occasional INS gate): gate 5 so a fresh player's
        // INSTINCT 7 >= 5 passes — the read tour (NON-1 flag value + a disposition shift) is
        // reachable in-game, not dead content. It is a ZERO-OPTION node (Decision 7): a terminal
        // text page closed by SPACE/E, exercising that close path in the seam.
        DialogNode read = new DialogNode(
                "You catch the tell — his eyes flick to the fire. He's holding something back.")
                .withFlag(KEY_SMOKE_READ, 7)
                .withEffect(new DialogEffect.Disposition("aldric", 1));
        DialogNode noRead = new DialogNode(
                "Nothing reads. Just a tired man on a hard road.",
                new DialogOption("Leave", end));

        // Narrator root (nullable speaker — Task 4): third-person narration, so no speaker line.
        return new DialogNode(
                "Aldric squats by the fire, turning a coal in his fingers. \"You're doing well, Klein.\"",
                new DialogOption("Tell him about Magdalene's letter.", GateStat.VOICE, 2, honest, end),
                new DialogOption("Ask about the road ahead.", coal),
                new DialogOption("Read whether he's hiding something.", GateStat.INSTINCT, 5, read, noRead),
                new DialogOption("Say nothing and move on.", null));
    }
}

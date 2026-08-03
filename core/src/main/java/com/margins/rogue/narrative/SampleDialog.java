package com.margins.rogue.narrative;

import com.margins.dialog.DialogNode;
import com.margins.dialog.DialogNode.DialogOption;

/**
 * A tiny placeholder scene so the FR-6 branching + turn-suspension mechanic is
 * exercisable in-game (opened by the debug {@code T} key). Real authored scenes —
 * the "Five Nights, Again" opening and the Galleon reunion — are Epic 6; this is
 * scaffolding for that, not shippable content.
 *
 * <p>Shape: a root with two branches; one leads to a follow-up node with a single
 * closing choice, the other closes immediately (a {@code null} next). That covers
 * branch, advance-to-linked-node, and scene-ending-choice for review.
 */
public final class SampleDialog {

    private SampleDialog() {}

    public static DialogNode build() {
        DialogNode farewell = new DialogNode(
                "\"Safe roads, then.\"",
                new DialogOption("Leave", null));

        // Success / failure branches for the INSTINCT-gated read (FR-7). The success
        // node sets the "cache revealed" flag (FR-8) — a cunning read reveals the cache (UJ-1).
        DialogNode read = new DialogNode(
                "You catch the tell — his eyes flick to a loose board. Something's stashed there.",
                new DialogOption("Leave", null))
                .withFlag(SceneEffects.KEY_CACHE_REVEALED, 1);
        DialogNode noRead = new DialogNode(
                "Nothing reads. Just a tired man on a hard road.",
                new DialogOption("Leave", null));

        return new DialogNode(
                "A scavenger eyes your pack. \"Traveling light?\"",
                new DialogOption("\"Light enough.\"", farewell),
                // Gated at 5 — the default INSTINCT (7) passes; below it routes to noRead.
                new DialogOption("Read him.", 5, read, noRead),
                new DialogOption("Say nothing and move on.", null));
    }
}

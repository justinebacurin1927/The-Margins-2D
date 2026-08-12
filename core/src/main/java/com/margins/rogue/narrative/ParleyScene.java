package com.margins.rogue.narrative;

import com.margins.dialog.DialogEffect;
import com.margins.dialog.DialogNode;
import com.margins.dialog.DialogNode.DialogOption;
import com.margins.dialog.GateStat;

/**
 * The wary-patrol parley (Story 4.2, AC-2, FR-19): a minimal VOICE-gated scene Klein can open when
 * a SUSPICIOUS patrol is adjacent. A passed VOICE check routes to a success node carrying a
 * {@link DialogEffect.Deescalate} — the talk-down drops the nearby wary enemies to UNAWARE; a
 * failed check leaves them wary. This is the <em>mechanism</em> proved end-to-end (Story 4.2 D1);
 * rich per-faction negotiation content is Epic 5's. Pure content model (AD-2), like {@code CorneoIntro}.
 */
public final class ParleyScene {

    /** How far the talk-down reaches — catches the wary patrol clustered around Klein (AD-9 scale). */
    public static final int PARLEY_RADIUS = 4;

    /** VOICE needed to talk a wary patrol down. Tuned to Klein's starting VOICE (3): a steady voice
     *  suffices at rest, but a VOICE penalty (a debuff, or a future loss) drops the check to the
     *  failure branch (FR-19 — deterministic stat &gt;= threshold, no dice). */
    public static final int VOICE_THRESHOLD = 3;

    private ParleyScene() {}

    /** Build the parley scene's root node. */
    public static DialogNode build() {
        DialogNode success = new DialogNode(
                "You keep your voice level and give them a reason to stand down. The hands come off the hilts.")
                .withEffect(new DialogEffect.Deescalate(PARLEY_RADIUS));
        DialogNode fail = new DialogNode(
                "The words don't land. They stay wary, watching your hands.");
        return new DialogNode(
                "A patrol eyes you, wary — hands near their blades.",
                new DialogOption("Talk them down. (VOICE)", GateStat.VOICE, VOICE_THRESHOLD, success, fail),
                new DialogOption("Say nothing.", (DialogNode) null))
                .withSpeaker("Patrol");
    }
}

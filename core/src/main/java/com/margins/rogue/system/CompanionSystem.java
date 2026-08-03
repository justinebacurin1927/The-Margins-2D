package com.margins.rogue.system;

import com.margins.rogue.Companion;
import com.margins.rogue.state.RunState;

import java.util.List;

/**
 * Turn pipeline step (AD-4): the active companion takes one follow step toward
 * the player's current (post-move) position. Runs in the Companion+Enemy-AI
 * phase — after detection, before the enemy phase — so the ally moves as part
 * of that phase rather than being inlined into input/rendering (AD-10).
 */
public final class CompanionSystem {
    /** PRD Balance: how far a shout travels. Louder than an attack swing (4); matches enemy vision (6). */
    public static final int DISTRACTION_RADIUS = 6;

    private CompanionSystem() {}

    public static void follow(RunState state) {
        Companion c = state.getActiveCompanion();
        if (c == null) return;
        c.followStep(state.getPlayer().getTileX(), state.getPlayer().getTileY());
    }

    /**
     * Galleon shouts: emit a Noise event at the companion's position (AD-9/AD-10 —
     * Distraction produces noise; it never touches enemies directly). Returns true
     * if a turn was committed. Refused without a turn when there's no companion or
     * the per-floor limit is spent (FR-14).
     */
    public static boolean distract(RunState state, List<String> messages) {
        Companion c = state.getActiveCompanion();
        if (c == null) {
            messages.add("No companion to call on.");
            return false;
        }
        if (!c.canDistract()) {
            messages.add("Galleon has no shouts left this floor.");
            return false;
        }
        c.useDistraction();
        state.emitNoise(c.getTileX(), c.getTileY(), DISTRACTION_RADIUS);
        messages.add("Galleon shouts!");
        return true;
    }
}

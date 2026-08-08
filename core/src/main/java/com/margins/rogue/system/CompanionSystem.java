package com.margins.rogue.system;

import com.margins.rogue.Companion;
import com.margins.rogue.RogueEnemy;
import com.margins.rogue.state.RunState;

import java.util.List;

/**
 * Turn pipeline step (AD-4): the active companion takes its turn in the
 * Companion+Enemy-AI phase — after detection, before the enemy phase — so the
 * ally acts as part of that phase rather than being inlined into input/rendering
 * (AD-10). Combat fix #1: an engaged companion FIGHTS — if an enemy is in melee
 * range it strikes it instead of trailing the player; only an un-engaged
 * companion takes its follow step.
 */
public final class CompanionSystem {
    /** PRD Balance: how far a shout travels. Louder than an attack swing (4); matches enemy vision (6). */
    public static final int DISTRACTION_RADIUS = 6;

    private CompanionSystem() {}

    /** The companion's turn: strike the adjacent enemy if one is in melee range, else follow.
     *  Writes only the strike line (observation discipline) — a dead companion does nothing. */
    public static void follow(RunState state, List<String> messages) {
        Companion c = state.getActiveCompanion();
        if (c == null || !c.isAlive()) return;
        RogueEnemy target = enemyAdjacentTo(state, c);
        if (target != null) {
            target.takeDamage(c.getDamage());
            messages.add("Aldric strikes for " + c.getDamage() + "!");
        } else {
            c.followStep(state.getPlayer().getTileX(), state.getPlayer().getTileY());
        }
    }

    /** The living enemy standing on a tile adjacent to the companion, or null. Melee is cardinal
     *  (same range as the enemies' own attacks — Aldric holds his ground, he doesn't chase). */
    private static RogueEnemy enemyAdjacentTo(RunState state, Companion c) {
        for (RogueEnemy e : state.getEnemies()) {
            if (e.isAlive() && e.isAdjacentTo(c.getTileX(), c.getTileY())) return e;
        }
        return null;
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

package com.margins.rogue.system;

import com.margins.rogue.Companion;
import com.margins.rogue.Detection;
import com.margins.rogue.RogueEnemy;
import com.margins.rogue.RoguePlayer;
import com.margins.rogue.state.RunState;

import java.util.List;

/**
 * Combat rules extracted from the screen (AD-2, AD-4): the player's melee
 * attack and the enemy phase (arrival-grace, dodge/armor/block resolution,
 * enemy movement). No libGDX types — headless-testable.
 */
public final class CombatSystem {
    private CombatSystem() {}

    /** Resolve the player's melee attack in the aimed direction (8-dir, combat fix #3): the tile
     *  at {@code dir}'s offset — cardinal OR diagonal (the diagonals complete the aim arc). */
    public static void playerAttack(RunState state, int dir, List<String> messages) {
        RoguePlayer player = state.getPlayer();
        int tx = player.getTileX() + RoguePlayer.directionX(dir);
        int ty = player.getTileY() + RoguePlayer.directionY(dir);
        RogueEnemy target = enemyAt(state, tx, ty);
        if (target != null) {
            target.takeDamage(player.getStr());
            messages.add("Hit! " + target.getHp() + "/" + target.getMaxHp());
        } else {
            messages.add("Nothing there");
        }
        state.emitNoise(player.getTileX(), player.getTileY(), ATTACK_NOISE_RADIUS); // the swing is loud (FR-5)
    }

    private static final int ATTACK_NOISE_RADIUS = 4;

    /**
     * Every living enemy takes its turn. Only ALERTED enemies pursue and attack
     * (arrival-grace, attack if adjacent, else chase); UNAWARE/SUSPICIOUS enemies
     * idle-wander and never initiate combat (AD-9). Escalation is wired in 2.4.
     *
     * <p>Combat fix #1: an alerted enemy whose path is barred by the living companion attacks HIM
     * instead of walking through him — Aldric is a real target and a real liability, not scenery.
     * The player is still the prime target (an enemy adjacent to the player always strikes the
     * player). Movement (chase/investigate/wander) treats the companion's tile as blocked along
     * with the player's (combat fix #2): enemies cannot occupy a party member's tile.
     */
    public static void enemyPhase(RunState state, List<String> messages) {
        RoguePlayer player = state.getPlayer();
        int px = player.getTileX();
        int py = player.getTileY();
        Companion companion = state.getActiveCompanion();
        boolean companionPresent = companion != null && companion.isAlive();
        int bx = companionPresent ? companion.getTileX() : -1; // -1 = no companion to block on
        int by = companionPresent ? companion.getTileY() : -1;
        for (RogueEnemy e : state.getEnemies()) {
            if (!e.isAlive()) continue;
            if (e.getDetection() == Detection.SUSPICIOUS) {
                e.takeTurn(e.getLastSeenX(), e.getLastSeenY(), bx, by); // investigate, no attack
                continue;
            }
            if (e.getDetection() != Detection.ALERTED) {
                e.wander(state.rng(), px, py, bx, by);
                continue;
            }
            if (e.hasJustArrived()) {
                e.setJustArrived(false);
                continue;
            }
            if (e.isAdjacentTo(px, py)) {
                if (player.tryDodge(state.rng())) {
                    messages.add("Dodge!");
                } else {
                    boolean blocked = player.isBlocking();
                    int dealt = player.takeDamage(e.getDamage());
                    if (blocked) {
                        messages.add("Brace! Blocked " + e.getDamage() + "→" + dealt);
                    } else {
                        messages.add("Hit for " + dealt + "!");
                    }
                }
            } else if (companionPresent && e.isAdjacentTo(bx, by)) {
                // The enemy can't reach the player without passing Aldric — it strikes him instead.
                int dealt = companion.takeDamage(e.getDamage());
                messages.add("Aldric is hit for " + dealt + "!");
                if (!companion.isAlive()) messages.add("Aldric falls!");
            } else {
                e.takeTurn(px, py, bx, by);
            }
        }
    }

    /**
     * Post-damage HP floor (FR-16/17): the first lethal blow per run leaves Milek
     * at 1 HP in a desperate state instead of dead; once Last Stand is spent, the
     * next lethal event is allowed to kill. Runs after all damage this turn, so it
     * covers every lethal source (enemy hits and hunger starvation).
     */
    public static void checkLastStand(RunState state, List<String> messages) {
        RoguePlayer player = state.getPlayer();
        if (player.isAlive()) return;        // survived on its own — nothing to do
        if (state.isLastStandUsed()) return; // reprieve already spent → true death
        player.reviveTo(1);
        state.setLastStandUsed(true);
        state.setLastStand(true);
        messages.add("Last Stand!");
    }

    public static RogueEnemy enemyAt(RunState state, int x, int y) {
        for (RogueEnemy e : state.getEnemies()) {
            if (e.isAlive() && e.getTileX() == x && e.getTileY() == y) return e;
        }
        return null;
    }
}

package com.margins.rogue.system;

import com.margins.rogue.Companion;
import com.margins.rogue.Detection;
import com.margins.rogue.RogueEnemy;
import com.margins.rogue.RoguePlayer;
import com.margins.rogue.state.RunState;

import java.util.Comparator;
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
            if (!target.isAlive()) messages.add("Enemy defeated.");
        } else {
            messages.add("Nothing there");
        }
        state.emitNoise(player.getTileX(), player.getTileY(), ATTACK_NOISE_RADIUS); // the swing is loud (FR-5)
    }

    private static final int ATTACK_NOISE_RADIUS = 4;
    /** The Dodge action's evasion multiplier (Story 4.1, FR-12): while evading, the dodge chance is
     *  min(90, dodgePercent × 2) — a 200% boost capped short of a guarantee. */
    private static final int DODGE_BOOST_PERCENT = 200;

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
        // Turn order by AG (Story 4.1, FR-12, AC-1): living enemies act highest-AG first. Java's
        // List.sort is a stable TimSort, so equal-AG enemies keep their insertion order (D3) — no
        // existing multi-enemy behavior changes, and the sort is AD-5-safe (ordering is not an rng
        // draw). Dead enemies are still skipped below, so the sort can't make a corpse act (AD-4).
        state.getEnemies().sort(Comparator.comparingInt(RogueEnemy::getAg).reversed());
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
                // Story 4.1 (FR-12): while the Dodge action's evasion is active, the dodge roll uses
                // the boosted AG-scaled chance (min 90, dodgePercent × 2). One seeded draw either
                // way (AD-5); the base path is unchanged when not evading.
                boolean dodged = player.isEvading()
                        ? player.tryDodge(state.rng(), DODGE_BOOST_PERCENT)
                        : player.tryDodge(state.rng());
                if (dodged) {
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
     * The Flee action (Story 4.1, FR-12): break away from the nearest living enemy — move to the
     * walkable cardinal neighbor (dir 0/1/2/3) that STRICTLY maximizes distance to that enemy;
     * the earliest dir wins ties. Returns whether a turn was spent. A refusal (boxed in / already
     * maximally far / no enemies) spends NO turn — the inert-USE / wall-bump precedent (AD-5).
     * Resolved in the acted step, so the enemy phase still pursues after a successful flee.
     */
    public static boolean flee(RunState state, List<String> messages) {
        RoguePlayer player = state.getPlayer();
        int px = player.getTileX(), py = player.getTileY();
        int bestDist = nearestEnemyDist(state, px, py); // the player's current distance is the bar
        int bestDir = -1;
        for (int d = 0; d < 4; d++) {
            int nx = px + RoguePlayer.directionX(d);
            int ny = py + RoguePlayer.directionY(d);
            if (!state.getTileMap().isWalkable(nx, ny)) continue;
            if (state.isOccupiedByEnemy(nx, ny)) continue;
            int dist = nearestEnemyDist(state, nx, ny);
            if (dist > bestDist) { bestDist = dist; bestDir = d; } // strictly farther; first dir wins ties
        }
        if (bestDir < 0) {
            messages.add("No way out!");
            return false;
        }
        player.tryMove(RoguePlayer.directionX(bestDir), RoguePlayer.directionY(bestDir));
        messages.add("You flee!");
        return true;
    }

    /** Manhattan distance to the nearest LIVING enemy, or a huge sentinel when none is left (a
     *  defensible "already maximally far" — fleeing with no enemies refuses, spending no turn). */
    private static int nearestEnemyDist(RunState state, int x, int y) {
        int best = Integer.MAX_VALUE;
        for (RogueEnemy e : state.getEnemies()) {
            if (!e.isAlive()) continue;
            best = Math.min(best, Math.abs(e.getTileX() - x) + Math.abs(e.getTileY() - y));
        }
        return best;
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

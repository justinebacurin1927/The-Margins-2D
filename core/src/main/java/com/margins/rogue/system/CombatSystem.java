package com.margins.rogue.system;

import com.margins.rogue.Companion;
import com.margins.rogue.CompanionLoss;
import com.margins.rogue.Detection;
import com.margins.rogue.RogueEnemy;
import com.margins.rogue.RoguePlayer;
import com.margins.rogue.item.Weapon;
import com.margins.rogue.state.RunState;

import java.util.ArrayList;
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
            target.takeDamage(attackDamage(state)); // Story 4.4: base STR + the wielded weapon's tier bonus
            messages.add("Hit! " + target.getHp() + "/" + target.getMaxHp());
            if (!target.isAlive()) messages.add("Enemy defeated.");
        } else {
            messages.add("Nothing there");
        }
        state.emitNoise(player.getTileX(), player.getTileY(), ATTACK_NOISE_RADIUS); // the swing is loud (FR-5)
        decayWielded(state, messages); // Story 4.4 (AC-1): the swing wears the weapon, hit or miss
    }

    /**
     * Resolve a combatant companion's melee strike on a target (Story 5.4, AC-1): the companion's
     * damage applies here, through the single HP authority, at the Companion-AI step — not inline in
     * {@code CompanionSystem}. Same damage and observation lines as the extracted engage code, so
     * no HP pool has a second owner (AD-10).
     */
    public static void companionAttack(RunState state, Companion companion, RogueEnemy target, List<String> messages) {
        target.takeDamage(companion.getDamage());
        messages.add(companion.getId().displayName() + " strikes for " + companion.getDamage() + "!");
        if (!target.isAlive()) messages.add("Enemy defeated.");
    }

    /** Klein's melee damage (Story 4.4, D3): base STR plus the wielded weapon's tier bonus — 0 when
     *  unarmed or when the wielded weapon is broken, so a broken weapon deals exactly base STR. */
    public static int attackDamage(RunState state) {
        Weapon w = state.getWieldedWeapon();
        return state.getPlayer().getStr() + (w != null ? w.damageBonus() : 0);
    }

    /** Story 4.4 (AC-1): spend one action's durability on the wielded weapon. A break reverts Klein to
     *  unarmed (base STR from the next swing) and is observed once. Shared by ATTACK and BLOCK; a no-op
     *  when unarmed. Detection/HP authorities are untouched — this only wears the weapon. */
    public static void decayWielded(RunState state, List<String> messages) {
        Weapon w = state.getWieldedWeapon();
        if (w == null) return;
        w.decay(Weapon.DECAY_PER_ACTION);
        if (w.isBroken()) {
            state.breakWielded();
            messages.add("Your " + w.displayName() + " breaks.");
        }
    }

    private static final int ATTACK_NOISE_RADIUS = 4;

    /** The Block/Brace action is as loud as a swing (Story 4.2, AC-1, D2): emit one NoiseEvent at
     *  Klein's tile at {@link #ATTACK_NOISE_RADIUS} so a brace draws reinforcements exactly like an
     *  attack (AD-9). Co-located with the attack noise so all combat noise has one home. */
    public static void blockNoise(RunState state) {
        RoguePlayer player = state.getPlayer();
        state.emitNoise(player.getTileX(), player.getTileY(), ATTACK_NOISE_RADIUS);
    }

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
        // Turn order by AG (Story 4.1, FR-12, AC-1): living enemies act highest-AG first. We sort a
        // COPY, never the live backing list — the canonical enemy order (rendering, save/load) must
        // stay put; only this phase's iteration order changes. Java's List.sort is a stable TimSort,
        // so equal-AG enemies keep their insertion order (D3), and the sort is AD-5-safe (ordering is
        // not an rng draw). Dead enemies are still skipped below, so the sort can't make a corpse act.
        List<RogueEnemy> order = new ArrayList<>(state.getEnemies());
        order.sort(Comparator.comparingInt(RogueEnemy::getAg).reversed());
        for (RogueEnemy e : order) {
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
                if (!companion.isAlive()) {
                    messages.add("Aldric falls!");
                    state.loseCompanion(companion.getId(), CompanionLoss.DEAD); // Story 5.5: the permanent-death shape (corpse stays)
                }
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
        if (bestDist == Integer.MAX_VALUE) { // nothing alive on the map to flee from
            messages.add("Nothing to flee from.");
            return false;
        }
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
     * Post-damage HP floor (FR-14): the first lethal blow per run triggers a GRIT roll
     * (Story 4.6) that MAY leave Klein at 1 HP in a desperate state instead of dead; once
     * that single check is spent, the next lethal event is allowed to kill. Runs after all
     * damage this turn, so it covers every lethal source (enemy hits and hunger starvation).
     * The GRIT check is the once-per-run event: a failed roll lets the death stand and does
     * not re-roll on later lethal events (D1). One seeded draw per run (AD-5).
     */
    public static void checkLastStand(RunState state, List<String> messages) {
        RoguePlayer player = state.getPlayer();
        if (player.isAlive()) return;        // survived on its own — nothing to do
        if (state.isLastStandUsed()) return; // the one reprieve already spent → true death
        state.setLastStandUsed(true);        // spend the single GRIT check (pass or fail)
        if (!player.tryLastStand(state.rng())) return; // the roll failed → the death stands
        player.reviveTo(1);
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

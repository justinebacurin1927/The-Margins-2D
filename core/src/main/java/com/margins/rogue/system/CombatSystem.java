package com.margins.rogue.system;

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

    /** Resolve the player's melee attack in the facing direction. */
    public static void playerAttack(RunState state, int dir, List<String> messages) {
        RoguePlayer player = state.getPlayer();
        int tx = player.getTileX() + (dir == 2 ? -1 : dir == 3 ? 1 : 0);
        int ty = player.getTileY() + (dir == 1 ? 1 : dir == 0 ? -1 : 0);
        RogueEnemy target = enemyAt(state, tx, ty);
        if (target != null) {
            target.takeDamage(player.getStr());
            messages.add("Hit! " + target.getHp() + "/" + target.getMaxHp());
        } else {
            messages.add("Nothing there");
        }
    }

    /** Every living enemy takes its turn: arrival-grace, attack if adjacent, else move. */
    public static void enemyPhase(RunState state, List<String> messages) {
        RoguePlayer player = state.getPlayer();
        int px = player.getTileX();
        int py = player.getTileY();
        for (RogueEnemy e : state.getEnemies()) {
            if (!e.isAlive()) continue;
            if (e.hasJustArrived()) {
                e.setJustArrived(false);
                continue;
            }
            if (e.isAdjacentTo(px, py)) {
                if (player.tryDodge()) {
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
            } else {
                e.takeTurn(px, py);
            }
        }
    }

    public static RogueEnemy enemyAt(RunState state, int x, int y) {
        for (RogueEnemy e : state.getEnemies()) {
            if (e.isAlive() && e.getTileX() == x && e.getTileY() == y) return e;
        }
        return null;
    }
}

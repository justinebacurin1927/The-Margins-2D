package com.margins.rogue.system;

import com.margins.rogue.RoguePlayer;
import com.margins.rogue.RogueTile;
import com.margins.rogue.item.FloorItem;
import com.margins.rogue.item.Inventory;
import com.margins.rogue.item.Supply;
import com.margins.rogue.item.TrueIdentity;
import com.margins.rogue.state.RunState;

/**
 * Advances one turn by running systems in fixed order (AD-4):
 * PlayerAction -> Hunger -> Detection -> Companion follow -> Enemy AI (Combat)
 * -> Noise resolve -> Last Stand -> cleanup.
 * The screen builds a {@link PlayerAction} and calls {@link #advance}; no turn
 * rule lives in the screen (AD-2). Enemy movement/hunger only occur when the
 * player actually acted, preserving the original "wasted keypress into a wall"
 * behavior.
 */
public class TurnEngine {

    public TurnResult advance(RunState state, PlayerAction action) {
        TurnResult result = new TurnResult();
        RoguePlayer player = state.getPlayer();

        // Turn-boundary cleanup: the desperate flag only lasts the turn it fired.
        state.setLastStand(false);

        // Facing updates regardless of whether the action commits a turn.
        player.setFacing(action.dir);

        boolean acted = false;
        switch (action.kind) {
            case MOVE:
                int tx = player.getTileX() + action.dx;
                int ty = player.getTileY() + action.dy;
                if (state.getTileMap().isWalkable(tx, ty)) {
                    player.tryMove(action.dx, action.dy);
                    acted = true;
                }
                break;
            case ATTACK:
                CombatSystem.playerAttack(state, action.dir, result.messages);
                acted = true;
                break;
            case BLOCK:
                player.setBlocking(true);
                result.messages.add("Brace!");
                acted = true;
                break;
            case WAIT:
                acted = true;
                break;
            case USE: {
                Supply s = Supply.byOrdinal(action.itemType);
                if (s != null && state.getInventory().count(action.itemType) > 0) {
                    TrueIdentity id = state.getIdentifyMap().identityOf(action.itemType);
                    if (id != null) id.apply(player); // effect is the per-seed bound identity (FR-11)
                    if (s.isConsumedOnUse()) {
                        boolean wasIdentified = state.getIdentifyMap().isIdentified(action.itemType);
                        state.getIdentifyMap().markIdentified(action.itemType); // reveal the whole type (FR-12)
                        state.getInventory().remove(action.itemType, 1);
                        // First use reveals what it was; later uses just name the known identity.
                        result.messages.add(!wasIdentified && id != null
                                ? s.displayName() + ": " + id.displayName() + "!"
                                : "Used " + state.getIdentifyMap().displayNameFor(action.itemType));
                        acted = true;
                    } else {
                        // Inert item (e.g. the unreadable Sealed Letter): a no-op costs no turn,
                        // matching the empty-pickup / walk-into-wall precedent.
                        result.messages.add("Milek can't read it.");
                    }
                }
                break;
            }
            case DROP: {
                Supply s = Supply.byOrdinal(action.itemType);
                int n = state.getInventory().count(action.itemType);
                if (s != null && n > 0 && state.getInventory().drop(action.itemType)) {
                    state.addFloorItem(action.itemType, n, player.getTileX(), player.getTileY());
                    result.messages.add("Dropped " + state.getIdentifyMap().displayNameFor(action.itemType));
                    acted = true;
                }
                break;
            }
            case PICKUP: {
                FloorItem it = state.takeItemAt(player.getTileX(), player.getTileY());
                if (it != null) {
                    if (state.getInventory().tryAdd(it.type, it.count) == Inventory.AddResult.ADDED) {
                        Supply s = Supply.byOrdinal(it.type);
                        result.messages.add("Picked up " + (s != null ? state.getIdentifyMap().displayNameFor(it.type) : "item"));
                        acted = true;
                    } else {
                        // Backpack full: return the stack to the tile and spend no turn.
                        // The screen gates on fullness before submitting, so this is a safety net.
                        state.addFloorItem(it.type, it.count, it.x, it.y);
                    }
                }
                break;
            }
            case DISTRACT: {
                // A refused shout (no companion / no uses left) spends no turn — mirrors the
                // inert-USE precedent. On success the noise resolves in the Noise step below.
                acted = CompanionSystem.distract(state, result.messages);
                break;
            }
        }

        if (acted) {
            // Stepping onto the down-stairs descends: rebuild the floor and carry
            // the party, treating the descent as the whole turn (hunger ticks, FOV
            // recomputes, but the old floor's actors are gone — enemy/noise phases
            // are skipped for the arrival turn).
            if (action.kind == PlayerAction.Kind.MOVE
                    && state.getTileMap().getTile(player.getTileX(), player.getTileY()) == RogueTile.STAIRS_DOWN) {
                state.descend();
                result.messages.add("You descend to floor " + state.getFloorDepth());
                HungerSystem.tick(player);
                // The hunger tick can be lethal on the arrival tile; honor the once-per-run
                // reprieve here too, so descending never bypasses Last Stand (FR-16/17).
                CombatSystem.checkLastStand(state, result.messages);
                FovSystem.compute(state);
            } else {
                HungerSystem.tick(player);
                DetectionSystem.update(state); // advance awareness before enemies move (AD-4)
                CompanionSystem.follow(state); // the ally moves in the Companion+Enemy-AI phase (AD-4, AD-10)
                CombatSystem.enemyPhase(state, result.messages);
                NoiseSystem.resolve(state); // Noise resolve step (AD-4)
                if (action.kind == PlayerAction.Kind.WAIT) {
                    result.messages.add("Wait");
                }
                // Reprieve check after all damage this turn; added last so "Last Stand!"
                // wins the message display over combat/wait text.
                CombatSystem.checkLastStand(state, result.messages);
                // Recompute sight from the player's (possibly new) position.
                FovSystem.compute(state);
            }
        }

        return result;
    }
}

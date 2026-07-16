package com.margins.rogue.system;

import com.margins.rogue.RoguePlayer;
import com.margins.rogue.state.RunState;

/**
 * Advances one turn by running systems in fixed order (AD-4):
 * PlayerAction -> Hunger -> Enemy AI (Combat) -> Noise resolve -> cleanup.
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
        }

        if (acted) {
            HungerSystem.tick(player);
            CombatSystem.enemyPhase(state, result.messages);
            // Noise resolve — placeholder step (Story 2.5 fills this in).
            if (action.kind == PlayerAction.Kind.WAIT) {
                result.messages.add("Wait");
            }
            // Reprieve check after all damage this turn; added last so "Last Stand!"
            // wins the message display over combat/wait text.
            CombatSystem.checkLastStand(state, result.messages);
        }

        return result;
    }
}

package com.margins.rogue.narrative;

import com.margins.dialog.DialogNode;
import com.margins.dialog.DialogNode.DialogOption;
import com.margins.rogue.state.RunState;

/**
 * Drives one authored dialogue scene (FR-6). Holds the current {@link DialogNode}
 * and advances it when the player picks a choice; the scene ends when a chosen
 * option has no linked node (or a terminal node is closed). This is the
 * navigation authority — the rule lives here, not in the screen (AD-2) — and it
 * is pure model (no libGDX). The screen renders {@link #getCurrent()} and forwards
 * the chosen index to {@link #select(int)}.
 *
 * <p>The controller is transient view-session state held by the screen (beside
 * {@code uiMode}); an in-progress scene is not part of {@code RunState}/save. The
 * narrative state that must persist (flags, Bond) lives in {@code RunState}'s
 * FlagStore (AD-7) and is written there by later stories, not here.
 */
public class DialogController {

    private DialogNode current;

    /** Whether a scene is open (turn play should be suspended while true). */
    public boolean isActive() {
        return current != null;
    }

    /** The node currently on screen, or null when no scene is open. */
    public DialogNode getCurrent() {
        return current;
    }

    /** Open a scene at the given root node (applies the root's flag effect, if any). */
    public void start(DialogNode root, RunState state) {
        enter(root, state);
    }

    /** Close the scene (terminal node, or explicit cancel). */
    public void end() {
        current = null;
    }

    /**
     * Pick a choice on the current node. An ungated option advances to its linked
     * node (FR-6). An INSTINCT-gated option resolves as a deterministic threshold
     * compare — {@code instinct >= threshold} routes to the success branch
     * ({@code next}), below it routes to {@code failNext} (AD-8, FR-7); no dice.
     * The entered node's flag effect, if any, is written to the run's FlagStore
     * (FR-8, AD-7). Either branch may be null → the scene ends. Out-of-range indices
     * and calls while no scene is open are no-ops.
     */
    public void select(int choiceIndex, RunState state) {
        if (current == null) return;
        if (choiceIndex < 0 || choiceIndex >= current.options.length) return;
        DialogOption opt = current.options[choiceIndex];
        DialogNode target;
        if (opt.isGated() && state.getPlayer().getInstinct() < opt.instinctThreshold) {
            target = opt.failNext;    // failed check → failure branch
        } else {
            target = opt.next;        // ungated, or passed check (instinct >= threshold)
        }
        enter(target, state);
    }

    /**
     * Make {@code node} current and apply its flag effect, if any, through the run's
     * FlagStore — dialogue writes narrative state only via the store (AD-7).
     */
    private void enter(DialogNode node, RunState state) {
        current = node;
        if (node != null && node.setFlagKey != null && state != null) {
            state.getFlagStore().set(node.setFlagKey, node.setFlagValue);
        }
    }
}

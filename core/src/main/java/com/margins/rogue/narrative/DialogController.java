package com.margins.rogue.narrative;

import com.margins.dialog.DialogEffect;
import com.margins.dialog.DialogNode;
import com.margins.dialog.DialogNode.DialogOption;
import com.margins.dialog.GateStat;
import com.margins.rogue.item.Inventory;
import com.margins.rogue.state.FlagStore;
import com.margins.rogue.state.RunState;

import java.util.ArrayList;
import java.util.List;

/**
 * Drives one authored dialogue scene (FR-6). Holds the current {@link DialogNode}
 * and advances it when the player picks a choice; the scene ends when a chosen
 * option has no linked node (or a terminal node is closed). This is the
 * navigation authority — the rule lives here, not in the screen (AD-2) — and it
 * is pure model (no libGDX). The screen renders {@link #getCurrent()} and forwards
 * the chosen index to {@link #select(int)}.
 *
 * <p>Safe pause (AD-14): a scene is open while {@link #isActive()}; the screen
 * suspends the turn loop for its whole duration. {@code select} never touches the
 * turn engine, the clock, or the survival tracks — reading and choosing cost no
 * turn.
 *
 * <p>The controller is transient view-session state held by the screen (beside
 * {@code uiMode}); an in-progress scene is not part of {@code RunState}/save. The
 * narrative state that must persist (flags, Bond, disposition) lives in
 * {@code RunState}'s FlagStore (AD-7) and is written here through the store.
 *
 * <p>Observation discipline (Story 2.1): every player-facing effect (Bond, item
 * give/take, disposition) emits an SPD-tone line this controller appends to the
 * run's message log — the log's second core writer after {@code TurnEngine}
 * (AD-4's single acted branch is untouched: dialogue is a suspended surface, not
 * a turn). A {@code SetFlag} (the {@code withFlag} convenience) is scene
 * bookkeeping: the flag it sets is observed through the scene continuing (the
 * node text), so it writes silently by design.
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

    /** Open a scene at the given root node (applies the root's effects, if any). */
    public void start(DialogNode root, RunState state) {
        enter(root, state);
    }

    /** Close the scene (terminal node, or explicit cancel). */
    public void end() {
        current = null;
    }

    /**
     * Pick a choice on the current node. An ungated option advances to its linked
     * node (FR-6). A gated option resolves as a deterministic threshold compare —
     * {@code stat >= threshold} routes to the success branch ({@code next}), below
     * it routes to {@code failNext} (FR-19); no dice. The entered node's effects
     * (flag, Bond, item give/take, disposition) are executed and their SPD lines
     * appended to the message log. Either branch may be null → the scene ends.
     * Out-of-range indices and calls while no scene is open are no-ops.
     */
    public void select(int choiceIndex, RunState state) {
        if (current == null) return;
        if (choiceIndex < 0 || choiceIndex >= current.options.length) return;
        DialogOption opt = current.options[choiceIndex];
        DialogNode target;
        if (opt.isGated() && gateValue(opt, state) < opt.gateThreshold) {
            target = opt.failNext;    // failed check → failure branch
        } else {
            target = opt.next;        // ungated, or passed check (stat >= threshold)
        }
        enter(target, state);
    }

    /** The player's value of an option's gating stat (FR-19: VOICE primary, occasional INS). */
    private int gateValue(DialogOption opt, RunState state) {
        return opt.gatedStat == GateStat.VOICE
                ? state.getPlayer().getVoice()
                : state.getPlayer().getInstinct();
    }

    /**
     * Make {@code node} current, execute its effects through the run's
     * FlagStore/Inventory, and append their SPD lines to the message log —
     * dialogue writes narrative state only via the store (AD-7), and never
     * silently (observation discipline).
     */
    private void enter(DialogNode node, RunState state) {
        current = node;
        if (node == null || state == null) return;
        List<String> lines = new ArrayList<>();
        for (DialogEffect e : node.effects()) {
            String line = apply(e, state);
            if (line != null) lines.add(line);
        }
        if (!lines.isEmpty()) state.appendMessages(lines);
    }

    /** Execute one effect; returns its SPD-tone observation line, or null when nothing player-facing changed.
     *  instanceof chains (Java 17 — no pattern switch); the sealed interface makes this exhaustive. */
    private String apply(DialogEffect e, RunState state) {
        if (e instanceof DialogEffect.SetFlag f) {
            state.getFlagStore().set(f.key(), f.value());
            return null; // scene bookkeeping — the node text is the observation
        }
        if (e instanceof DialogEffect.Bond b) return bondLine(state.getFlagStore(), b.tag());
        if (e instanceof DialogEffect.GiveItem g) return giveItemLine(state, g.type(), g.count());
        if (e instanceof DialogEffect.TakeItem t) return takeItemLine(state, t.type(), t.count());
        if (e instanceof DialogEffect.Disposition d) return dispositionLine(state.getFlagStore(), d.npc(), d.delta());
        return null; // unreachable — sealed DialogEffect is exhaustive here
    }

    private String bondLine(FlagStore fs, String tag) {
        if (!FlagStore.BOND_TAG_HONEST.equals(tag) && !FlagStore.BOND_TAG_DISMISSIVE.equals(tag)) {
            return null; // unknown tag — applyBondTag is a no-op, nothing player-facing changed
        }
        fs.applyBondTag(tag);
        return FlagStore.BOND_TAG_HONEST.equals(tag) ? "He warms to you." : "His eyes narrow.";
    }

    private String giveItemLine(RunState state, int type, int count) {
        String name = nameFor(state, type);
        if (state.getInventory().tryAdd(type, count) == Inventory.AddResult.ADDED) {
            return "He hands you " + (count > 1 ? count + " " + name : "a " + name) + ".";
        }
        return "No room in your pack.";
    }

    private String takeItemLine(RunState state, int type, int count) {
        String name = nameFor(state, type);
        if (state.getInventory().remove(type, count)) {
            return "You give him " + (count > 1 ? count + " " + name : "the " + name) + ".";
        }
        return "You don't have " + (count > 1 ? "that much " + name : "the " + name) + ".";
    }

    private String dispositionLine(FlagStore fs, String npc, int delta) {
        fs.add("disposition." + npc, delta);
        return delta > 0 ? "He warms to you." : "His eyes narrow.";
    }

    /** The identified-or-hidden display name for a supply type (FR-12). */
    private String nameFor(RunState state, int type) {
        return state.getIdentifyMap().displayNameFor(type);
    }
}

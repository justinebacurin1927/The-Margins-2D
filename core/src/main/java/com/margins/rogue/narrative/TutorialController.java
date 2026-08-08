package com.margins.rogue.narrative;

import com.margins.rogue.RogueTile;
import com.margins.rogue.RogueTileMap;
import com.margins.rogue.item.Supply;
import com.margins.rogue.state.RunState;
import com.margins.rogue.system.PlayerAction;
import com.margins.rogue.system.PlayerAction.Kind;

import java.util.ArrayList;
import java.util.List;

/**
 * Aldric's diegetic tutorial (FR-2): a PASSIVE coach that teaches the six opening
 * controls — move, scavenge, eat, craft, hide, rest — as in-world dialogue during
 * the flight after the intro (Story 2.2). Pure model, no libGDX (AD-2),
 * headless-testable.
 *
 * <p>Unlike {@link DialogController} (2.1) and {@link IntroController} (2.2), which
 * SUSPEND the turn loop, this controller runs during LIVE play: after each committed
 * turn the screen calls {@link #onAction}, and the coach checks off whichever opening
 * control the player just performed and prompts the next un-demonstrated one. It never
 * suspends the loop, never gates input, and never returns a {@code PlayerAction} —
 * it only appends Aldric's lines to the message log (AD-15). The AD-4 single acted
 * branch is untouched (the coach observes a turn; it is not a turn).
 *
 * <p>Progress is transient view-session state held by the screen — no {@code RunState}
 * field, nothing serialized (AD-6 by construction). Completion is exposed as
 * {@link #isComplete()} for Story 2.4 to hook; this story persists nothing.
 */
public class TutorialController {

    /** The six opening controls (FR-2), each with Aldric's prompt + acknowledgement. */
    enum Control {
        MOVE("Aldric: \"Move — stay low and keep to the trees. [WASD]\"",
             "Aldric: \"Good. Keep your feet under you.\""),
        SCAVENGE("Aldric: \"We'll need supplies. Forage what the forest gives — grass, logs, rock. [C]\"",
                 "Aldric: \"That's it. The forest feeds those who look.\""),
        EAT("Aldric: \"Don't hoard it — eat when you're able. [E]\"",
            "Aldric: \"Better. A full stomach walks farther.\""),
        CRAFT("Aldric: \"Put your hands to work — a fire, a clean drink, a mended tool. [B/K/F/V/T]\"",
              "Aldric: \"Well made. Out here you build or you go without.\""),
        HIDE("Aldric: \"When they come, break their line of sight — put a trunk between you and the road.\"",
             "Aldric: \"Good. They can't see through wood.\""),
        REST("Aldric: \"And when there's nothing to do but wait, wait — let the moment pass. [SPACE]\"",
             "Aldric: \"Rest when you can. It's a weapon too.\"");

        final String prompt;
        final String ack;

        Control(String prompt, String ack) {
            this.prompt = prompt;
            this.ack = ack;
        }
    }

    /** Aldric's line when all six are learned — hands the flight forward (the Story 2.4 seam). */
    static final String CLOSING = "Aldric: \"You've got it, Klein. Now stay close — we're not clear yet.\"";

    private final List<Control> remaining = new ArrayList<>(List.of(Control.values()));
    private boolean armed = true;   // may still begin (disarmed by skip / by beginning)
    private boolean active = false; // coaching in progress
    private boolean completed = false;

    /** Begin coaching (fires once): append the first prompt and go active. A no-op if already
     *  active, complete, or disarmed (e.g. after {@link #skip()}). The screen calls this every frame
     *  once the intro closes; the guard makes it safe. */
    public void begin(RunState state) {
        if (active || completed || !armed) return;
        armed = false;
        active = true;
        state.appendMessages(List.of(remaining.get(0).prompt));
    }

    /** Observe a committed turn: check off the FIRST still-remaining control this action satisfies
     *  (Decision 3 — first-match by list order), acknowledge it, and prompt the next; when the last
     *  is learned, append the closing line and complete. A no-op when not active. Writes only log
     *  lines — no turn, no flag, no inventory (observation discipline). */
    public void onAction(PlayerAction action, RunState state) {
        if (!active || action == null) return;
        for (Control c : remaining) {
            if (matches(c, action, state)) {
                remaining.remove(c);
                List<String> lines = new ArrayList<>();
                lines.add(c.ack);
                if (remaining.isEmpty()) {
                    lines.add(CLOSING);
                    active = false;
                    completed = true;
                } else {
                    lines.add(remaining.get(0).prompt);
                }
                state.appendMessages(lines);
                return;
            }
        }
    }

    /** Abort coaching without completing (the restart path — a new life after death gets no
     *  coaching, and Story 2.4's completion seam is not spuriously tripped). Also resets a prior
     *  completion so a post-death life that never ran the tutorial does not read as complete
     *  (review M1: completion is per-life, not per screen session). */
    public void skip() {
        armed = false;
        active = false;
        completed = false;
    }

    /** Whether the coach is currently prompting (a scene is NOT suspended — this is live play). */
    public boolean isActive() {
        return active;
    }

    /** Whether all six controls were demonstrated (the Story 2.4 hook). False after {@link #skip()}. */
    public boolean isComplete() {
        return completed;
    }

    /** Does {@code action} (in the current {@code state}) demonstrate control {@code c}? */
    private boolean matches(Control c, PlayerAction action, RunState state) {
        switch (c) {
            case MOVE:     return action.kind == Kind.MOVE;
            case SCAVENGE: return action.kind == Kind.COLLECT;
            case EAT:      return action.kind == Kind.USE && isFoodUse(action);
            case CRAFT:    return action.kind == Kind.BUILD_CAMPFIRE || action.kind == Kind.COOK
                                || action.kind == Kind.FILTER || action.kind == Kind.BOIL
                                || action.kind == Kind.CRAFT_TORCH;
            case HIDE:     return action.kind == Kind.MOVE && movedIntoCover(state);
            case REST:     return action.kind == Kind.WAIT;
            default:       return false;
        }
    }

    /** A USE that consumes a FOOD provision (not water or a cure) — drinking must not check off
     *  "eat" (review m1). */
    private boolean isFoodUse(PlayerAction action) {
        if (action.itemType < 0) return false;
        Supply s = Supply.byOrdinal(action.itemType);
        return s != null && s.isFood();
    }

    /** True when this MOVE ended the player adjacent to a blocking (opaque) trunk/rock — "moving to
     *  a tile adjacent to a blocking trunk/rock" (spec Decision 2). Only reached on a COMMITTED move
     *  (the H1 seam gate), so a refused wall-bump or an open-field move never demonstrates hide; the
     *  check does not consider the move's direction (the fiction is simplified to "ended next to
     *  cover" — the trunk's exact side is a cosmetic nit, not a spec requirement). */
    private boolean movedIntoCover(RunState state) {
        int px = state.getPlayer().getTileX();
        int py = state.getPlayer().getTileY();
        RogueTileMap map = state.getTileMap();
        return isCover(map, px + 1, py) || isCover(map, px - 1, py)
                || isCover(map, px, py + 1) || isCover(map, px, py - 1);
    }

    private boolean isCover(RogueTileMap map, int x, int y) {
        return RogueTile.isOpaque(map.getTile(x, y));
    }
}

package com.margins.rogue.narrative;

import com.margins.rogue.CompanionId;
import com.margins.rogue.CompanionLoss;
import com.margins.rogue.RoguePlayer;
import com.margins.rogue.state.FlagStore;
import com.margins.rogue.state.RunState;
import com.margins.rogue.world.WorldSpine;

import java.util.List;

/**
 * Story 5.6 (FR-18, AD-11): the act-gating quests. Two location-triggered story gates that advance
 * the act as Klein pushes east —
 * <ul>
 *   <li><b>"Follow the Road"</b>: reaching the Copper Road corridor (a Tier-2 push east, the
 *       {@link WorldSpine} Watchtower easting) completes the quest and flips Act 1→2;</li>
 *   <li><b>"The Rescue"</b>: reaching the road-head where Aldric is held (the road's far-east end)
 *       resolves the rescue and flips Act 2→3 on <em>either</em> outcome — success (Aldric rejoins)
 *       or failure (lost).</li>
 * </ul>
 * Advancing the act is exactly what Epic 4's escalation channel ({@link RunState#enemyCountFor},
 * AD-11 channel a) reads to thicken the interior — until now that ramp was tested-but-inert because
 * the act never left 1.
 *
 * <p>The controller is <b>stateless</b>: each gate is a one-shot guarded by persisted flags — the
 * current act ({@code act.current}) and the quest's {@code completed} flag — so it self-guards across
 * save/load and re-resolves harmlessly. Like {@link CaptureController} it is the "safe every-frame
 * call" the screen makes on committed turns (right after {@link CaptureController#resolve}); it is
 * NOT a {@code TurnEngine} pipeline step and ticks no survival (AD-4/AD-5). Pure model, no libGDX
 * (AD-2). No new persisted field — it reuses {@code act.current}, {@code quest.*}, and
 * {@code aldric.captured} (AD-6 by construction).
 */
public class ActGateController {

    /** Act 1→2: the Copper Road corridor is reached (AC-1). */
    public static final String LINE_ROAD =
            "You reach the Copper Road corridor — the occupation thickens behind you.";
    /** Act 2→3, success: Aldric is pulled free at the road-head (AC-2). */
    public static final String LINE_RESCUE_WIN =
            "The road-head — you tear Aldric free. He falls in beside you.";
    /** Act 2→3, failure: the road-head is reached, but Aldric is already lost (AC-2). */
    public static final String LINE_RESCUE_LOSS =
            "The road-head — but Aldric is already gone. The war has him now.";

    /**
     * Advance the act when a gate's landmark is reached. A safe every-frame call (D6): each gate
     * fires once, guarded by the act value and its quest's completed flag. No-op on a null run or an
     * empty map.
     */
    public void resolve(RunState state) {
        if (state == null || state.getPlayer() == null || state.getTileMap() == null) return;
        FlagStore fs = state.getFlagStore();
        RoguePlayer player = state.getPlayer();
        WorldSpine spine = new WorldSpine(state.getTileMap().getWidth(), state.getTileMap().getHeight());

        switch (fs.getAct()) {
            case 1 -> checkFollowTheRoad(state, fs, player, spine);
            case 2 -> checkRescue(state, fs, player, spine);
            default -> { /* Act 3+: no further act-gate quests in 5.6 (the border win is 5.7). */ }
        }
    }

    /** Act 1→2 gate: auto-start "Follow the Road" so it reads ACTIVE in the Journal (2.5), then
     *  complete it and advance the act once the player has pushed east to the Watchtower easting. */
    private void checkFollowTheRoad(RunState state, FlagStore fs, RoguePlayer player, WorldSpine spine) {
        String id = JournalController.QUEST_FOLLOW_THE_ROAD;
        if (fs.get(JournalController.startedKey(id)) == 0) {
            fs.set(JournalController.startedKey(id), 1); // auto-start in Act 1 (idempotent)
        }
        if (player.getTileX() < spine.watchtowerX()) return; // not yet the Tier-2 push east
        fs.set(JournalController.completedKey(id), 1);
        fs.setAct(2);
        state.appendMessages(List.of(LINE_ROAD));
    }

    /** Act 2→3 gate: reaching the road-head resolves "The Rescue". Reaching it while Aldric is still
     *  recoverable (CAPTURED) rescues him — the default success path (D4). The determinant that could
     *  make it FAIL (the prison encounter) is deferred content (5.7); {@link #resolveRescue} with
     *  {@code false} is the wired, tested failure branch. */
    private void checkRescue(RunState state, FlagStore fs, RoguePlayer player, WorldSpine spine) {
        if (fs.get(JournalController.completedKey(JournalController.QUEST_ROAD_EAST)) != 0) return;
        if (player.getTileX() < spine.roadEndX()) return; // not yet at the road-head prison
        boolean success = fs.get(FlagStore.KEY_ALDRIC_CAPTURED) != 0
                && fs.getLoss(CompanionId.ALDRIC) == CompanionLoss.CAPTURED;
        resolveRescue(state, success);
    }

    /**
     * Resolve "The Rescue" and flip the Act 2→3 gate on EITHER outcome (AC-2). Success rejoins
     * Aldric (clear the captured flag, clear his loss, materialize his body); failure leaves him
     * lost as he already was. One-shot via the quest's completed flag. The failed-rescue loss-shape
     * reconciliation (foreclosed-CAPTURED vs DEAD) is deferred to 5.7 (D4).
     */
    public void resolveRescue(RunState state, boolean success) {
        FlagStore fs = state.getFlagStore();
        if (fs.get(JournalController.completedKey(JournalController.QUEST_ROAD_EAST)) != 0) return; // one-shot
        if (success) {
            fs.set(FlagStore.KEY_ALDRIC_CAPTURED, 0);
            fs.setLoss(CompanionId.ALDRIC, CompanionLoss.NONE);
            state.activateCompanion(CompanionId.ALDRIC);
            state.appendMessages(List.of(LINE_RESCUE_WIN));
        } else {
            state.appendMessages(List.of(LINE_RESCUE_LOSS));
        }
        // Mark started as well as completed — resolving the rescue means Klein lived the thread, so it
        // surfaces in the Journal (2.5 Decision 8) even if he reached the road-head without first
        // reading the Torn Page. Idempotent; the Torn-Page discovery also sets started in normal play.
        fs.set(JournalController.startedKey(JournalController.QUEST_ROAD_EAST), 1);
        fs.set(JournalController.completedKey(JournalController.QUEST_ROAD_EAST), 1);
        fs.setAct(3);
    }
}

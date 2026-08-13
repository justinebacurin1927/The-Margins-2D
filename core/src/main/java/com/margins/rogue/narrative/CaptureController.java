package com.margins.rogue.narrative;

import com.margins.rogue.Companion;
import com.margins.rogue.CompanionId;
import com.margins.rogue.CompanionLoss;
import com.margins.rogue.item.Supply;
import com.margins.rogue.state.FlagStore;
import com.margins.rogue.state.RunState;

import java.util.List;

/**
 * Story 2.4 (FR-3): Aldric's capture — a ONE-SHOT scripted event that resolves the moment the
 * diegetic tutorial completes (the seam {@link TutorialController#isComplete()}, 2.3 Decision 7).
 * The screen calls {@link #resolve} every frame once the tutorial is complete; the guard makes it
 * a safe every-frame call that fires on the acted turn the tutorial's last control is performed and
 * never again.
 *
 * <p>The chasers are NARRATED, not simulated (Decision 4): occupation escalation that mechanically
 * catches Aldric is Epic 4-3's scope. This controller ships the beat and its consequences:
 * <ul>
 *   <li>the capture flag is set ({@link FlagStore#KEY_ALDRIC_CAPTURED} — recoverable later, not death),</li>
 *   <li>Aldric is REMOVED from the party ({@link RunState#removeActiveCompanion()} — Klein escapes
 *       alone; follow/distract no-op via their existing null-checks),</li>
 *   <li>a discovery note (a new Torn Page {@link Supply}) is planted at his last tile — the east /
 *       Copper-Road seed Story 2.5's discovery-triggered quests hook,</li>
 *   <li>and the four-line beat is appended to the message log (AD-15).</li>
 * </ul>
 *
 * <p>Pure model, no libGDX (AD-2), headless-testable. Transient view-session state — the
 * controller itself persists nothing; its consequences ride existing stores (AD-6 by construction).
 * Unlike the 2.3 coach (which writes only log lines), this is a scripted EVENT that legitimately
 * mutates the run — every mutation is announced in the beat, nothing silent (Decision 8).
 */
public class CaptureController {

    /** The chase line — the chasers are heard, not seen. */
    public static final String LINE_CHASE = "A horn cuts the pines — hoofbeats on the road. Chasers.";
    /** The take — capture, not death. */
    public static final String LINE_TAKE = "They take Aldric — he goes down fighting and is dragged off.";
    /** Klein escapes alone (AC-1). */
    public static final String LINE_ALONE = "Klein is alone in the pines.";
    /** The discovery seed is announced where it lies (AC-2). */
    public static final String LINE_NOTE = "A torn page lies where Aldric was taken.";

    private boolean resolved = false;

    /** Whether the capture has fired this life (false until {@link #resolve} actually captures). */
    public boolean isResolved() {
        return resolved;
    }

    /** Resolve the capture once. A no-op when already fired, or when the party is already empty
     *  or Aldric is dead (review M2: capture is "not death" — a dead Aldric cannot be captured,
     *  and the beat must not narrate a take over a corpse). The screen's every-frame gate stays
     *  cheap and safe. */
    public void resolve(RunState state) {
        if (resolved) return;
        Companion aldric = state.getActiveCompanion();
        if (aldric == null || !aldric.isAlive()) return;
        resolved = true;

        int x = aldric.getTileX(), y = aldric.getTileY();
        state.getFlagStore().set(FlagStore.KEY_ALDRIC_CAPTURED, 1); // the rescue-thread flag (Story 2.4/2.5)
        state.loseCompanion(CompanionId.ALDRIC, CompanionLoss.CAPTURED); // Story 5.5: the Captured shape (removes the body)
        state.addFloorItem(Supply.TORN_PAGE.ordinal(), 1, x, y);    // the discovery seed (AC-2)
        state.appendMessages(List.of(LINE_CHASE, LINE_TAKE, LINE_ALONE, LINE_NOTE)); // the beat (AD-15)
    }
}

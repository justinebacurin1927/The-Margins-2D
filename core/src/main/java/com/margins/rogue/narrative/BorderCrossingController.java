package com.margins.rogue.narrative;

import com.margins.rogue.CompanionId;
import com.margins.rogue.RoguePlayer;
import com.margins.rogue.state.FlagStore;
import com.margins.rogue.state.RunState;
import com.margins.rogue.world.WorldSpine;

import java.util.ArrayList;
import java.util.List;

/**
 * Story 5.7 (FR-18, AD-12, AD-11 channel b): the border-crossing win. In Act 3, reaching the NW
 * border landmark ends the run as a VICTORY — Klein crosses into Novelborne, {@code KEY_WON} is set,
 * and the epilogue seeds connect to main-story canon (Corneo → Coneros, the Mercenary Graveyard
 * filling now). The border is <b>always physically walkable</b> (AD-12 permadeath honesty); the
 * tension is the thinned channel-b cordon on the approach ({@link RunState#cordonCountFor}), not a
 * boss or an invisible wall.
 *
 * <p>The epilogue branches on Aldric's fate (SM-1 — the ending is lived through the systems): if he
 * was rescued (still the active companion) he crosses beside Klein; otherwise Klein crosses alone.
 *
 * <p>Stateless one-shot, guarded by persisted flags — {@code KEY_WON} and the {@code act >= 3} gate
 * (reached only via Story 5.6's rescue). Like {@link CaptureController}/{@link ActGateController} it
 * is the "safe every-frame call" the screen makes on committed turns; it is NOT a {@code TurnEngine}
 * pipeline step and ticks no survival (AD-4/AD-5). Pure model, no libGDX (AD-2). No new persisted
 * field beyond the plain {@code KEY_WON} flag (AD-6).
 */
public class BorderCrossingController {

    /** The border is a corner, not one tile — reaching within this band of the landmark counts. */
    static final int REACH_BAND = 2;

    public static final String LINE_HOME =
            "The pines thin. Novelborne fields open ahead — Klein crosses. Home.";
    public static final String LINE_ALDRIC_WITH =
            "Aldric crosses at his side — they made it out together.";
    public static final String LINE_ALDRIC_ALONE =
            "He crosses alone, carrying the weight of the ones the road kept.";
    public static final String LINE_CORNEO =
            "Word will reach Corneo — Coneros, they will call it in the years after.";
    public static final String LINE_GRAVEYARD =
            "Behind him the Mercenary Graveyard is filling. The war goes on. He does not.";

    /**
     * Win the run if Klein has reached the NW border in Act 3. A safe every-frame call: one-shot via
     * {@code KEY_WON}, gated on {@code act >= 3}. No-op on a null run/player/map, before Act 3, or
     * before the border is reached.
     */
    public void resolve(RunState state) {
        if (state == null || state.getPlayer() == null || state.getTileMap() == null) return;
        RoguePlayer p = state.getPlayer();
        if (!p.isAlive()) return; // you must SURVIVE the crossing (AC-2) — dying at the threshold is death, not a win
        FlagStore fs = state.getFlagStore();
        if (fs.get(FlagStore.KEY_WON) != 0) return; // already won (one-shot)
        if (fs.getAct() < 3) return;                // the win gate — Act 3 only (via 5.6's rescue)

        WorldSpine spine = new WorldSpine(state.getTileMap().getWidth(), state.getTileMap().getHeight());
        if (Math.abs(p.getTileX() - spine.borderX()) > REACH_BAND
                || Math.abs(p.getTileY() - spine.borderY()) > REACH_BAND) {
            return; // not at the border yet
        }

        fs.set(FlagStore.KEY_WON, 1);
        List<String> beat = new ArrayList<>();
        beat.add(LINE_HOME);
        beat.add(state.getActiveCompanionId() == CompanionId.ALDRIC ? LINE_ALDRIC_WITH : LINE_ALDRIC_ALONE);
        beat.add(LINE_CORNEO);
        beat.add(LINE_GRAVEYARD);
        state.appendMessages(beat);
    }
}

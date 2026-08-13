package com.margins.rogue.state;

import com.margins.rogue.CompanionId;
import com.margins.rogue.CompanionLoss;

import java.util.LinkedHashMap;

/**
 * Run-scoped narrative state (AD-7): a generic string→int key/value store plus
 * Galleon's Bond. Owned by {@code RunState}; {@code DialogNode}/quests read and
 * write narrative state only through this store. A plain {@code LinkedHashMap}
 * (insertion order → deterministic serialization), so it is pure model — no
 * libGDX types (AD-2) — and serializes under the {@code RunState} root (AD-6).
 *
 * <p>{@link #get(String)} returns 0 for never-set keys (the store's empty-slot
 * sentinel), so callers never null-check. Bond lives at a key like any flag, but
 * with typed accessors and a tone-tier read ({@link #getBondTier()}) that Epic 5
 * dialogue will use to select lines (FR-15).
 */
public class FlagStore {

    /** Per-companion Bond key (Story 5.1, AD-7): {@code "bond." + id.bindId()} (e.g. "bond.aldric").
     *  Bond is now keyed per roster member so each of the four carries its own relationship. */
    public static String bondKey(CompanionId id) { return "bond." + id.bindId(); }

    /** Map key for the default/primary companion's Bond — Aldric, the remake's canon combat
     *  companion (Story 5.1 retargets the old single {@code "bond.galleon"} to {@code bondKey(ALDRIC)};
     *  the no-arg Bond accessors below address Aldric so every existing single-companion call site
     *  and test stays correct). A pre-5.1 save's {@code bond.galleon} key is not read — Bond inherits
     *  the neutral 0 baseline (AD-6 field-absent default). */
    public static final String KEY_BOND = bondKey(CompanionId.ALDRIC);

    /** Flag: Aldric was captured after the tutorial (Story 2.4, FR-3) — the rescue thread's
     *  run-scoped signal (recoverable later, not death). Set once by CaptureController; the
     *  quest/Journal story (2.5) and the Act-2 rescue quest (Epic 5) read it. */
    public static final String KEY_ALDRIC_CAPTURED = "aldric.captured";

    /** The current story act (Story 4.3, AD-11). The occupation-escalation ramp reads this to
     *  thicken the interior per act. AD-11: the escalation trigger is a story-flag, NOT a timer —
     *  Epic 5's act-gating quests ("Follow the Road" 1→2, "The Rescue" 2→3, Story 5.6) own the
     *  writes via {@link #setAct}. The never-set sentinel (0) maps to Act 1 in {@link #getAct}, so a
     *  pre-4.3 save reads Act 1 by construction (AD-6 deterministic default — no migration). */
    public static final String KEY_ACT = "act.current";

    /** Flag: Klein survived the NW border crossing and reached Novelborne — the canonical win
     *  (Story 5.7, FR-18/AD-12). Set once by {@link com.margins.rogue.narrative.BorderCrossingController};
     *  the screen renders the victory end-state. Unset → 0 (AD-6 — no migration, no ctor default). */
    public static final String KEY_WON = "won";

    /** Stable tag carried by an honest dialogue choice → Bond +1 (FR-15). */
    public static final String BOND_TAG_HONEST = "bond.honest";
    /** Stable tag carried by a dismissive dialogue choice → Bond −1 (FR-15). */
    public static final String BOND_TAG_DISMISSIVE = "bond.dismissive";

    private LinkedHashMap<String, Integer> flags = new LinkedHashMap<>();

    /** Value for a key, or 0 if never set. */
    public int get(String key) {
        return flags.getOrDefault(key, 0);
    }

    public void set(String key, int value) {
        flags.put(key, value);
    }

    /** Shift a key by {@code delta} from its current value (0 if never set). */
    public void add(String key, int delta) {
        flags.put(key, get(key) + delta);
    }

    /** The current story act, ≥ 1 (Story 4.3, AD-11). The never-set/0 sentinel maps to Act 1, so a
     *  field-absent save reads Act 1 (AD-6 deterministic default), and no caller needs to special-case
     *  an unset flag. */
    public int getAct() {
        return Math.max(1, get(KEY_ACT));
    }

    /** Advance to (or set) the current act, clamped to ≥ 1 (Story 4.3). Epic 5's act-gating quests
     *  call this; 4.3 uses it as the test-only trigger for the otherwise-inert ramp (the map/enemies
     *  generate once per run at Act 1, so nothing re-reads this live yet — Epic 5 wires that). */
    public void setAct(int act) {
        set(KEY_ACT, Math.max(1, act));
    }

    /** The primary companion's (Aldric's) Bond value (0 = neutral baseline, AD-7 / FR-15). */
    public int getBond() {
        return getBond(CompanionId.ALDRIC);
    }

    /** A specific roster member's Bond value (Story 5.1, AD-7) — read an abstract (inactive)
     *  companion's Bond without a positioned body. */
    public int getBond(CompanionId id) {
        return get(bondKey(id));
    }

    /** Shift the primary companion's (Aldric's) Bond by {@code delta} (e.g. a tagged dialogue choice). */
    public void adjustBond(int delta) {
        adjustBond(CompanionId.ALDRIC, delta);
    }

    /** Shift a specific roster member's Bond (Story 5.1, AD-7) — each member's Bond is independent. */
    public void adjustBond(CompanionId id, int delta) {
        add(bondKey(id), delta);
    }

    /**
     * The Bond tone tier dialogue will branch on (FR-15): 0 = cold/distant
     * (≤ −2), 1 = neutral (−1..1), 2 = warm (≥ 2). Two honest choices warm
     * the companion; two dismissive choices chill them.
     */
    public int getBondTier() {
        return getBondTier(CompanionId.ALDRIC);
    }

    /** The Bond tone tier for a specific roster member (Story 5.1, AD-7). */
    public int getBondTier(CompanionId id) {
        int b = getBond(id);
        if (b <= -2) return 0;
        if (b >= 2) return 2;
        return 1;
    }

    // --- Story 5.5 (FR-17): Bond effect gates, per-companion loss shape, and hostility ---

    /** A real investment beyond the warm tier — high Bond unlocks lore/loyalty/personal quests. */
    public static final int LOYALTY_BOND = 3;
    /** The cold floor at which a companion withholds help (the tier-0 threshold). */
    public static final int WITHHOLD_BOND = -2;
    /** Bond this low and a companion departs the run (checked at a narrative beat, Story 5.6). */
    public static final int DEPARTURE_BOND = -4;

    /** High Bond gates loyalty/lore/personal-quest content for this companion (AC-1). Content reads it. */
    public boolean bondUnlocksLoyalty(CompanionId id) {
        return getBond(id) >= LOYALTY_BOND;
    }

    /** Low Bond withholds this companion's help (AC-1). Content branches on it. */
    public boolean bondWithholdsHelp(CompanionId id) {
        return getBond(id) <= WITHHOLD_BOND;
    }

    private static String lossKey(CompanionId id) { return "loss." + id.bindId(); }

    /** The recorded loss shape for a companion, or {@link CompanionLoss#NONE} if not lost (AD-6). */
    public CompanionLoss getLoss(CompanionId id) {
        return CompanionLoss.values()[get(lossKey(id))];
    }

    /** Record a companion's loss shape (Story 5.5, AC-2). */
    public void setLoss(CompanionId id, CompanionLoss loss) {
        set(lossKey(id), loss.ordinal());
    }

    private static String hostileKey(CompanionId id) { return "hostile." + id.bindId(); }

    /** Whether this companion has turned hostile (betrayal, AC-1). The seam a live hostile
     *  ex-companion (Story 5.6 content) will read. */
    public boolean isHostile(CompanionId id) {
        return get(hostileKey(id)) != 0;
    }

    public void setHostile(CompanionId id) {
        set(hostileKey(id), 1);
    }

    /**
     * Apply a Bond-tagged dialogue choice's delta. The tag→delta mapping lives
     * here as the single authority (mirrors Story 3.4's naming pattern): Epic 5
     * dialogue nodes carry a stable tag constant and call this on selection —
     * no scattered {@code adjustBond} literals across content. Unknown tags are
     * a no-op.
     */
    public void applyBondTag(String tag) {
        if (tag == null) return; // a node without a Bond tag is a no-op, not a crash
        switch (tag) {
            case BOND_TAG_HONEST:
                adjustBond(+1);
                break;
            case BOND_TAG_DISMISSIVE:
                adjustBond(-1);
                break;
            default:
                break;
        }
    }
}

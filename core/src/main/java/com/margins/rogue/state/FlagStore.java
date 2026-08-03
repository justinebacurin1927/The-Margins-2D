package com.margins.rogue.state;

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

    /** Map key for Galleon's Bond. */
    public static final String KEY_BOND = "bond.galleon";

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

    /** Galleon's Bond value (0 = neutral baseline, AD-7 / FR-15). */
    public int getBond() {
        return get(KEY_BOND);
    }

    /** Shift Bond by {@code delta} (e.g. a tagged dialogue choice, FR-15). */
    public void adjustBond(int delta) {
        add(KEY_BOND, delta);
    }

    /**
     * The Bond tone tier dialogue will branch on (FR-15): 0 = cold/distant
     * (≤ −2), 1 = neutral (−1..1), 2 = warm (≥ 2). Two honest choices warm
     * Galleon; two dismissive choices chill him.
     */
    public int getBondTier() {
        int b = getBond();
        if (b <= -2) return 0;
        if (b >= 2) return 2;
        return 1;
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

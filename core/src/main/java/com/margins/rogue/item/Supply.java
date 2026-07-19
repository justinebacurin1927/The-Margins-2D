package com.margins.rogue.item;

/**
 * The MVP Route-1 Supply set (FR-10/11): a stackable consumable TYPE. The backpack
 * stores each as its non-negative {@link #ordinal()} (honoring the {@code -1}
 * empty-slot sentinel reserved in {@link Inventory}).
 *
 * <p>A type's actual effect is its bound {@link TrueIdentity}, chosen per seed from
 * {@link #possibleIdentities()} by {@code IdentifyMap} (FR-11) — so "Sealed
 * Waterskin" can be clean water on one seed and tainted on another. Story 3.4 hides
 * the identity behind the unidentified {@link #displayName()} until first use. No
 * libGDX types (AD-2).
 */
public enum Supply {
    WRAPPED_BUNDLE("Wrapped Bundle", TrueIdentity.STALE_BREAD, TrueIdentity.SPOILED_MEAT),
    SEALED_WATERSKIN("Sealed Waterskin", TrueIdentity.CLEAN_WATER, TrueIdentity.TAINTED),
    SMALL_TIN("Small Tin", TrueIdentity.FEVERWORT, TrueIdentity.RENDERED_FAT),
    FOLDED_CLOTH("Folded Cloth", TrueIdentity.BANDAGES, TrueIdentity.OLD_RAGS),
    SEALED_LETTER("Sealed Letter", TrueIdentity.INERT_LETTER);

    private final String displayName;
    private final TrueIdentity[] possible;

    Supply(String displayName, TrueIdentity... possible) {
        this.displayName = displayName;
        this.possible = possible;
    }

    /** The identities this type may bind to for a run (FR-11). */
    public TrueIdentity[] possibleIdentities() {
        return possible;
    }

    /** All types are spent on use except the inert Sealed Letter. */
    public boolean isConsumedOnUse() {
        return this != SEALED_LETTER;
    }

    public String displayName() {
        return displayName;
    }

    private static final Supply[] VALUES = values();

    /** Map a backpack type id ({@link #ordinal()}) back to its Supply, or null if out of range. */
    public static Supply byOrdinal(int ordinal) {
        return (ordinal >= 0 && ordinal < VALUES.length) ? VALUES[ordinal] : null;
    }

    public static int count() {
        return VALUES.length;
    }
}

package com.margins.rogue.item;

import com.margins.rogue.RoguePlayer;

/**
 * The MVP Route-1 Supply set (FR-10/11). Each type is a stackable consumable the
 * player can use; the backpack stores it as its non-negative {@link #ordinal()}
 * (honoring the {@code -1} empty-slot sentinel reserved in {@link Inventory}).
 *
 * <p>Effects here are <b>known and fixed</b>. Story 3.3 binds each type to a
 * randomized true identity per seed, and Story 3.4 hides the identity until first
 * use — both layer on top of this enum without renaming it. Applying an effect
 * mutates the RunState-owned player, which is a model rule; this class holds no
 * libGDX types (AD-2).
 */
public enum Supply {
    WRAPPED_BUNDLE("Wrapped Bundle") { public void apply(RoguePlayer p) { p.eat(40); } },   // PRD Balance: bread +40
    SEALED_WATERSKIN("Sealed Waterskin") { public void apply(RoguePlayer p) { p.eat(15); } }, // first-pass, tunable
    SMALL_TIN("Small Tin") { public void apply(RoguePlayer p) { p.heal(4); } },              // first-pass
    FOLDED_CLOTH("Folded Cloth") { public void apply(RoguePlayer p) { p.heal(6); } },        // first-pass
    SEALED_LETTER("Sealed Letter") { public void apply(RoguePlayer p) { /* inert: Milek can't read it */ } };

    private final String displayName;

    Supply(String displayName) {
        this.displayName = displayName;
    }

    /** Apply this supply's (currently known) effect to the player. */
    public abstract void apply(RoguePlayer p);

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

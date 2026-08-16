package com.margins.rogue.item;

/**
 * Story 6.2 (FR-20, AC-1): a readied storage bag as a first-class instance carrying per-item
 * durability and an optional hidden {@link BagTrap}. Mirrors {@link Weapon}'s gear-with-durability
 * shape (AD-13): the flyweight int-ordinal store cannot hold mutable per-item state, so a readied
 * bag lives as a {@code Bag} in {@code Inventory}'s storage band instead of an int in an array.
 *
 * <p><b>Wear (AC-1):</b> {@link #decay(int)} spends durability; at 0 the bag {@link #isBroken()} and
 * is removed (its contents spill — see {@code Inventory.breakBag}). No repair curve in 6.2 (deferred).
 *
 * <p>Holds no libGDX types (AD-2); the no-arg constructor + plain fields let libGDX Json round-trip
 * it under the {@code RunState}→{@code Inventory} save root (AD-6).
 */
public class Bag {

    /** Fresh durability a readied bag starts with (PRD Balance — tunable content). */
    public static final int MAX_DURABILITY = 20;

    private Supply type;
    private int maxDurability;
    private int durability;
    private BagTrap trap;

    /** libGDX Json. */
    public Bag() {}

    public Bag(Supply type, int maxDurability, BagTrap trap) {
        this.type = type;
        this.maxDurability = maxDurability;
        this.durability = maxDurability;
        this.trap = trap == null ? BagTrap.NONE : trap;
    }

    /** A full-durability bag with the given trap (a found bag; the roll decides the trap). */
    public static Bag of(Supply type, BagTrap trap) {
        return new Bag(type, MAX_DURABILITY, trap);
    }

    /** A full-durability, untrapped bag (the Story-6.1 starting Traveler's Pack). */
    public static Bag untrapped(Supply type) {
        return of(type, BagTrap.NONE);
    }

    public Supply getType() { return type; }
    public int getDurability() { return durability; }
    public int getMaxDurability() { return maxDurability; }
    public BagTrap getTrap() { return trap; }

    /** Main-store slots this readied bag adds (AC-1, from {@link Supply#storageSlotBonus()}). */
    public int slotBonus() { return type == null ? 0 : type.storageSlotBonus(); }

    /** Whether this bag hides an armed trap (DART/FIRE/FREEZE) that has not yet fired. */
    public boolean isTrapped() { return trap != null && trap != BagTrap.NONE; }

    /** Spend durability for wear (AC-1); never drops below 0. */
    public void decay(int amount) {
        durability = Math.max(0, durability - Math.max(0, amount));
    }

    public boolean isBroken() { return durability <= 0; }

    /** Clear the trap once it has fired (a sprung trap can't fire twice before the bag breaks). */
    public void disarm() { trap = BagTrap.NONE; }
}

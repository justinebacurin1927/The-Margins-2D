package com.margins.rogue.item;

import java.util.Arrays;

/**
 * Run-scoped carry container (FR-9), owned by {@code RunState} (AD-3). Reuses the
 * proven stackable {@code int type}/{@code int count} model from
 * {@code com.margins.item.Inventory} (AD-12) rather than a per-item object model,
 * bounded to a backpack of {@value #BACKPACK_STACKS} stacks plus
 * {@value #EQUIPPED_SLOTS} equipped slots. Holds no libGDX types, so it stays
 * headless (AD-2) and serializes as plain int arrays under the {@code RunState}
 * save root (AD-6). Item types are opaque ints here; concrete supply types arrive
 * in Story 3.3.
 *
 * <p>Type ids must be non-negative: {@code -1} is reserved as the empty-slot
 * sentinel, so passing it as a real type would be indistinguishable from an empty
 * stack and silently corrupt the backpack.
 */
public class Inventory {

    public static final int BACKPACK_STACKS = 8;
    public static final int EQUIPPED_SLOTS = 2;
    private static final int EMPTY = -1;

    /**
     * Outcome of a pickup attempt. {@code BACKPACK_FULL} leaves the inventory
     * unchanged so the caller can prompt drop-or-leave (FR-9) instead of the item
     * being silently accepted or lost.
     */
    public enum AddResult { ADDED, BACKPACK_FULL }

    private int[] types = filled(new int[BACKPACK_STACKS]);
    private int[] counts = new int[BACKPACK_STACKS];
    private int[] equipped = filled(new int[EQUIPPED_SLOTS]);

    private static int[] filled(int[] a) {
        Arrays.fill(a, EMPTY);
        return a;
    }

    /**
     * Add {@code amount} of {@code type} to the backpack: stacks onto an existing
     * type if present, else consumes one of the {@value #BACKPACK_STACKS} stacks.
     * Returns {@code BACKPACK_FULL} WITHOUT mutating when the type is new and no
     * stack is free — the item is neither added nor lost.
     */
    public AddResult tryAdd(int type, int amount) {
        for (int i = 0; i < BACKPACK_STACKS; i++) {
            if (types[i] == type) {
                counts[i] += amount;
                return AddResult.ADDED;
            }
        }
        for (int i = 0; i < BACKPACK_STACKS; i++) {
            if (types[i] == EMPTY) {
                types[i] = type;
                counts[i] = amount;
                return AddResult.ADDED;
            }
        }
        return AddResult.BACKPACK_FULL;
    }

    /** Remove {@code amount} of a type; frees the stack when it reaches zero. False if not enough is held. */
    public boolean remove(int type, int amount) {
        for (int i = 0; i < BACKPACK_STACKS; i++) {
            if (types[i] == type) {
                if (counts[i] >= amount) {
                    counts[i] -= amount;
                    if (counts[i] == 0) types[i] = EMPTY;
                    return true;
                }
                return false;
            }
        }
        return false;
    }

    /** Drop one whole stack of a type (the drop half of drop-or-leave; on-tile placement is Story 3.2). */
    public boolean drop(int type) {
        for (int i = 0; i < BACKPACK_STACKS; i++) {
            if (types[i] == type) {
                types[i] = EMPTY;
                counts[i] = 0;
                return true;
            }
        }
        return false;
    }

    /**
     * Move one item of {@code type} from a backpack stack into the first free
     * equipped slot. False if the type isn't in the backpack or both equipped
     * slots are full.
     */
    public boolean equip(int type) {
        if (count(type) <= 0) return false;
        for (int s = 0; s < EQUIPPED_SLOTS; s++) {
            if (equipped[s] == EMPTY) {
                remove(type, 1);
                equipped[s] = type;
                return true;
            }
        }
        return false;
    }

    /** Return an equipped item to the backpack. False if the slot is empty or the backpack can't accept it. */
    public boolean unequip(int slot) {
        if (slot < 0 || slot >= EQUIPPED_SLOTS || equipped[slot] == EMPTY) return false;
        int type = equipped[slot];
        if (tryAdd(type, 1) == AddResult.BACKPACK_FULL) return false;
        equipped[slot] = EMPTY;
        return true;
    }

    public int count(int type) {
        for (int i = 0; i < BACKPACK_STACKS; i++) {
            if (types[i] == type) return counts[i];
        }
        return 0;
    }

    /** Type id in a backpack slot, or -1 if empty / out of range (for UI enumeration). */
    public int backpackType(int slot) {
        return (slot >= 0 && slot < BACKPACK_STACKS) ? types[slot] : EMPTY;
    }

    /** Count in a backpack slot (0 if empty / out of range). */
    public int backpackCount(int slot) {
        return (slot >= 0 && slot < BACKPACK_STACKS) ? counts[slot] : 0;
    }

    /** Index of the next occupied stack strictly after {@code from}, wrapping past the end to 0;
     *  -1 when the backpack is empty. The screen's selection cycle (Story 1.8, deferral F-09): it
     *  skips empty slots, so a single-stack backpack wraps back to itself in one full lap. Uses
     *  {@link Math#floorMod} so any negative {@code from} (a fresh, no-selection cycle) stays a
     *  valid slot index rather than indexing a negative array slot. */
    public int nextOccupiedStack(int from) {
        for (int step = 1; step <= BACKPACK_STACKS; step++) {
            int slot = Math.floorMod(from + step, BACKPACK_STACKS);
            if (types[slot] != EMPTY) return slot;
        }
        return EMPTY; // every slot empty — nothing to select
    }

    /** Index of the previous occupied stack strictly before {@code from}, wrapping past the start
     *  to the last occupied; -1 when the backpack is empty. The mirror of {@link #nextOccupiedStack}
     *  (Story 1.8 review — the screen's backward cycle walked 7 forward steps, which only equals
     *  "back" when the occupied count divides 8): forward and back must step symmetrically over the
     *  ring on any occupied count. A fresh (no-selection) backward press lands on the last occupied. */
    public int previousOccupiedStack(int from) {
        for (int step = 1; step <= BACKPACK_STACKS; step++) {
            int slot = Math.floorMod(from - step, BACKPACK_STACKS);
            if (types[slot] != EMPTY) return slot;
        }
        return EMPTY; // every slot empty — nothing to select
    }

    /** Number of occupied backpack stacks (0..{@value #BACKPACK_STACKS}). */
    public int backpackStackCount() {
        int n = 0;
        for (int t : types) if (t != EMPTY) n++;
        return n;
    }

    public boolean isBackpackFull() {
        return backpackStackCount() >= BACKPACK_STACKS;
    }

    /** Type id in an equipped slot, or -1 if empty / out of range. */
    public int equippedType(int slot) {
        return (slot >= 0 && slot < EQUIPPED_SLOTS) ? equipped[slot] : EMPTY;
    }
}

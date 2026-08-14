package com.margins.rogue.item;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Run-scoped carry container (FR-9/FR-20), owned by {@code RunState} (AD-3). Reuses the proven
 * stackable {@code int type}/{@code int count} model (AD-12) rather than a per-item object model.
 *
 * <p><b>Hybrid slots (Story 6.1, AC-1).</b> The carry is split into:
 * <ul>
 *   <li>a <b>main store</b> of {@value #MAIN_BASE_SLOTS} base stacks, expandable by readying up to
 *       {@value #MAX_STORAGE_ITEMS} storage items (bags) whose slot bonuses merge — see
 *       {@link #mainSlotCapacity()};</li>
 *   <li>a <b>Quick-Access</b> band of {@value #QUICK_GEAR_SLOTS} gear (weapon/armor-type) slots and
 *       {@value #QUICK_ARTIFACT_SLOTS} artifact/ring slots, always available.</li>
 * </ul>
 * The main {@code types}/{@code counts} arrays are physically sized to {@value #MAX_MAIN_SLOTS}
 * (base + the maximum merged storage bonus); only the first {@link #mainSlotCapacity()} of them
 * accept new stacks, so a bag readied mid-run grows the usable store without reallocating.
 *
 * <p><b>Weight (Story 6.1, AC-2).</b> Every held item carries a {@link Supply#weight()};
 * {@link #totalWeight()} vs a STR-scaled {@link #carryCapacity(int)} decides {@link #isEncumbered(int)}.
 * Encumbrance is a cost the turn loop pays (shorter foray range), never a hard carry block (drop-or-leave).
 *
 * <p>Holds no libGDX types, so it stays headless (AD-2) and serializes as plain int arrays under the
 * {@code RunState} save root (AD-6). Field names {@code types}/{@code counts}/{@code equipped} are
 * preserved from the earlier layout so a pre-6.1 save's items load into the same fields;
 * {@link #restoreAfterLoad()} grows any shorter loaded array to the new sizes.
 *
 * <p>Type ids must be non-negative: {@code -1} is reserved as the empty-slot sentinel, so passing it
 * as a real type would be indistinguishable from an empty stack and silently corrupt the store.
 */
public class Inventory {

    /** Base main-store stacks before any storage-item expansion (AC-1). */
    public static final int MAIN_BASE_SLOTS = 19;
    /** Max storage items (bags) that may be readied at once; their bonuses merge (AC-1, D2). */
    public static final int MAX_STORAGE_ITEMS = 5;
    /** Always-available Quick-Access gear (weapon/armor-type) slots (AC-1). */
    public static final int QUICK_GEAR_SLOTS = 5;
    /** Always-available Quick-Access artifact/ring slots (AC-1). */
    public static final int QUICK_ARTIFACT_SLOTS = 3;
    /** Physical size of the main store: base + the largest merged bonus the readied bags can add,
     *  so the store never reallocates when a bag is readied mid-run. */
    public static final int MAX_MAIN_SLOTS =
            MAIN_BASE_SLOTS + MAX_STORAGE_ITEMS * Supply.MAX_STORAGE_SLOT_BONUS;

    /** Weight capacity floor before STR (AC-2, D3). */
    public static final int BASE_CAPACITY = 10;
    /** Each STR point adds this much weight capacity (AC-2, D3). */
    public static final int STR_CAPACITY_FACTOR = 4;

    private static final int EMPTY = -1;

    /**
     * Outcome of a pickup attempt. {@code BACKPACK_FULL} leaves the inventory unchanged so the
     * caller can prompt drop-or-leave (FR-9) instead of the item being silently accepted or lost.
     */
    public enum AddResult { ADDED, BACKPACK_FULL }

    private int[] types = filled(new int[MAX_MAIN_SLOTS]);
    private int[] counts = new int[MAX_MAIN_SLOTS];
    private int[] equipped = filled(new int[QUICK_GEAR_SLOTS]);        // Quick-Access gear (was the 2 equipped slots)
    private int[] quickArtifact = filled(new int[QUICK_ARTIFACT_SLOTS]);
    // Story 6.2: readied bags are first-class Bag instances (durability + trap), not flyweight ints.
    private List<Bag> storageBags = new ArrayList<>();
    // Story 6.2 (AD-6 migration only): a pre-6.2 save serialized readied bags as this int[] of Supply
    // ordinals. restoreAfterLoad() converts it into full-durability untrapped storageBags, then nulls
    // it. Null on a fresh/new-format inventory, so new saves omit it.
    private int[] storageItems;

    private static int[] filled(int[] a) {
        Arrays.fill(a, EMPTY);
        return a;
    }

    /** Grow a loaded array to {@code size}, filling the appended tail with {@code fill}; a pre-6.1
     *  save's shorter arrays (main store 8, gear 2) become the new sizes with empty new slots (AD-6). */
    private static int[] grow(int[] a, int size, int fill) {
        if (a != null && a.length >= size) return a;
        int old = a == null ? 0 : a.length;
        int[] out = new int[size];
        if (a != null) System.arraycopy(a, 0, out, 0, old);
        Arrays.fill(out, old, size, fill);
        return out;
    }

    /** Re-normalize after a Json load (AD-6): grow the persisted arrays to the 6.1 sizes and default
     *  any field a pre-6.1 save omits to an empty structure. Called from {@code RunState.restoreAfterLoad}. */
    public void restoreAfterLoad() {
        types = grow(types, MAX_MAIN_SLOTS, EMPTY);
        counts = grow(counts, MAX_MAIN_SLOTS, 0);
        equipped = grow(equipped, QUICK_GEAR_SLOTS, EMPTY);
        quickArtifact = grow(quickArtifact, QUICK_ARTIFACT_SLOTS, EMPTY);
        if (storageBags == null) storageBags = new ArrayList<>();
        // Story 6.2 (AD-6): migrate a pre-6.2 save's flyweight storageItems int[] into full-durability
        // untrapped Bag instances, so a 6.1 save's readied bag(s) are not dropped on load. Only migrate
        // into an EMPTY bag list — never append legacy bags on top of an already-loaded new-format list.
        if (storageItems != null) {
            if (storageBags.isEmpty()) { // only migrate into an empty list — never double-add onto new-format bags
                for (int t : storageItems) {
                    if (t == EMPTY) continue;
                    Supply s = Supply.byOrdinal(t);
                    if (s != null && s.isStorage() && storageBags.size() < MAX_STORAGE_ITEMS) {
                        storageBags.add(Bag.untrapped(s));
                    }
                }
            }
            storageItems = null; // consumed either way, so it never lingers into a new-format save
        }
    }

    /** The current usable main-store size: {@value #MAIN_BASE_SLOTS} plus the merged slot bonus of the
     *  readied storage items, clamped to the physical {@value #MAX_MAIN_SLOTS} (AC-1, D2). */
    public int mainSlotCapacity() {
        int bonus = 0;
        for (Bag bag : storageBags) {
            if (bag != null && !bag.isBroken()) bonus += bag.slotBonus(); // null-safe vs a partial Json load
        }
        return Math.min(MAX_MAIN_SLOTS, MAIN_BASE_SLOTS + bonus);
    }

    /**
     * Add {@code amount} of {@code type} to the main store: stacks onto an existing type if present,
     * else consumes one of the currently usable {@link #mainSlotCapacity()} stacks. Returns
     * {@code BACKPACK_FULL} WITHOUT mutating when the type is new and no stack is free — the item is
     * neither added nor lost.
     */
    public AddResult tryAdd(int type, int amount) {
        for (int i = 0; i < types.length; i++) {
            if (types[i] == type) {
                counts[i] += amount;
                return AddResult.ADDED;
            }
        }
        int cap = Math.min(mainSlotCapacity(), types.length);
        for (int i = 0; i < cap; i++) {
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
        for (int i = 0; i < types.length; i++) {
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
        for (int i = 0; i < types.length; i++) {
            if (types[i] == type) {
                types[i] = EMPTY;
                counts[i] = 0;
                return true;
            }
        }
        return false;
    }

    /**
     * Ready one item of {@code type} from the main store into the first free Quick-Access slot for its
     * category (gear, artifact, or storage/bag — routed by {@link Supply}). False if the type isn't in
     * the store or its category band is full. A readied storage item expands {@link #mainSlotCapacity()}.
     */
    public boolean equip(int type) {
        Supply s = Supply.byOrdinal(type);
        if (s != null && s.isStorage()) return readyBagFromStore(type, BagTrap.NONE); // Story 6.2: readies an untrapped Bag
        if (count(type) <= 0) return false;
        int[] slots = slotsFor(type);
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == EMPTY) {
                remove(type, 1);
                slots[i] = type;
                return true;
            }
        }
        return false;
    }

    /** The Quick-Access band a NON-storage type routes into: artifacts → artifact slots, everything
     *  else → gear slots (the default). Storage bags route to {@link #readyBagFromStore} instead. */
    private int[] slotsFor(int type) {
        Supply s = Supply.byOrdinal(type);
        if (s != null && s.isQuickArtifact()) return quickArtifact;
        return equipped;
    }

    /**
     * Story 6.2 (AC-1): ready a storage bag held in the main store into a first-class {@link Bag}
     * (consuming one from the store), carrying {@code trap} — {@code BagTrap.NONE} for the untrapped
     * starting pack, or a rolled trap for a found bag (the roll lives in {@code BagSystem}, which owns
     * the RNG — capacity/weight stay pure here, AD-5). False if the type isn't a held storage item or
     * all {@value #MAX_STORAGE_ITEMS} bag slots are already readied.
     */
    public boolean readyBagFromStore(int type, BagTrap trap) {
        Supply s = Supply.byOrdinal(type);
        if (s == null || !s.isStorage() || count(type) <= 0) return false;
        if (storageBags.size() >= MAX_STORAGE_ITEMS) return false;
        remove(type, 1);
        storageBags.add(Bag.of(s, trap));
        return true;
    }

    /** The readied bags (Story 6.2) — the storage band. Live list so a system can decay/fire/break them. */
    public List<Bag> getStorageBags() {
        return storageBags;
    }

    /**
     * Story 6.2 (AC-1/AC-2, D4): break a readied bag — remove it (capacity shrinks) and return the
     * main-store stacks that no longer fit (the overflow at indices ≥ the new capacity), cleared from
     * the store so the caller can spill them to the ground (drop-or-leave). Deterministic — no RNG.
     */
    public List<int[]> breakBag(Bag bag) {
        if (!storageBags.remove(bag)) return new ArrayList<>(); // not readied → no capacity change, nothing spills
        int cap = mainSlotCapacity();
        // Compact occupied stacks toward index 0 first, so holes left by earlier removes are NOT counted
        // as overflow — only the genuine tail (occupiedCount − cap) spills (review fix).
        List<int[]> occupied = new ArrayList<>();
        for (int i = 0; i < types.length; i++) {
            if (types[i] != EMPTY) {
                occupied.add(new int[]{types[i], counts[i]});
                types[i] = EMPTY;
                counts[i] = 0;
            }
        }
        List<int[]> overflow = new ArrayList<>();
        for (int i = 0; i < occupied.size(); i++) {
            if (i < cap) {
                types[i] = occupied.get(i)[0];
                counts[i] = occupied.get(i)[1];
            } else {
                overflow.add(occupied.get(i)); // beyond the shrunk capacity — spills
            }
        }
        return overflow;
    }

    /** Return a Quick-Access gear item to the main store. False if the slot is empty or the store
     *  can't accept it. (Gear only — the body/loadout page's return path; bags stay readied in 6.1.) */
    public boolean unequip(int slot) {
        if (slot < 0 || slot >= QUICK_GEAR_SLOTS || equipped[slot] == EMPTY) return false;
        int type = equipped[slot];
        if (tryAdd(type, 1) == AddResult.BACKPACK_FULL) return false;
        equipped[slot] = EMPTY;
        return true;
    }

    public int count(int type) {
        for (int i = 0; i < types.length; i++) {
            if (types[i] == type) return counts[i];
        }
        return 0;
    }

    /** Type id in a main-store slot, or -1 if empty / out of range (for UI enumeration). */
    public int backpackType(int slot) {
        return (slot >= 0 && slot < types.length) ? types[slot] : EMPTY;
    }

    /** Count in a main-store slot (0 if empty / out of range). */
    public int backpackCount(int slot) {
        return (slot >= 0 && slot < counts.length) ? counts[slot] : 0;
    }

    /** Index of the next occupied stack strictly after {@code from}, wrapping past the end to 0;
     *  -1 when the store is empty. The screen's selection cycle (Story 1.8): it skips empty slots, so
     *  a single-stack store wraps back to itself in one full lap. Rings over the physical store size
     *  (count-agnostic); empty slots beyond the current capacity are simply skipped. Uses
     *  {@link Math#floorMod} so any negative {@code from} (a fresh, no-selection cycle) stays valid. */
    public int nextOccupiedStack(int from) {
        for (int step = 1; step <= types.length; step++) {
            int slot = Math.floorMod(from + step, types.length);
            if (types[slot] != EMPTY) return slot;
        }
        return EMPTY; // every slot empty — nothing to select
    }

    /** Index of the previous occupied stack strictly before {@code from}, wrapping past the start to
     *  the last occupied; -1 when the store is empty. The mirror of {@link #nextOccupiedStack}:
     *  forward and back step symmetrically over the ring on any occupied count. */
    public int previousOccupiedStack(int from) {
        for (int step = 1; step <= types.length; step++) {
            int slot = Math.floorMod(from - step, types.length);
            if (types[slot] != EMPTY) return slot;
        }
        return EMPTY; // every slot empty — nothing to select
    }

    /** Number of occupied main-store stacks. */
    public int backpackStackCount() {
        int n = 0;
        for (int t : types) if (t != EMPTY) n++;
        return n;
    }

    public boolean isBackpackFull() {
        return backpackStackCount() >= mainSlotCapacity();
    }

    /** Type id in a Quick-Access gear slot, or -1 if empty / out of range. */
    public int equippedType(int slot) {
        return (slot >= 0 && slot < QUICK_GEAR_SLOTS) ? equipped[slot] : EMPTY;
    }

    /** Type id in a Quick-Access artifact slot, or -1 if empty / out of range. */
    public int quickArtifactType(int slot) {
        return (slot >= 0 && slot < QUICK_ARTIFACT_SLOTS) ? quickArtifact[slot] : EMPTY;
    }

    /** Number of readied storage bags, 0..{@value #MAX_STORAGE_ITEMS} (AC-1). */
    public int storageItemCount() {
        return storageBags.size();
    }

    // --- Weight & encumbrance (Story 6.1, AC-2) ---

    /** Total carried weight: every main-store stack (count × {@link Supply#weight()}) plus one of each
     *  readied Quick-Access gear/artifact item and each readied bag. Pure function of what is held. */
    public int totalWeight() {
        int w = 0;
        for (int i = 0; i < types.length; i++) {
            if (types[i] != EMPTY) w += counts[i] * weightOf(types[i]);
        }
        for (int t : equipped) if (t != EMPTY) w += weightOf(t);
        for (int t : quickArtifact) if (t != EMPTY) w += weightOf(t);
        for (Bag bag : storageBags) if (bag != null && bag.getType() != null) w += weightOf(bag.getType().ordinal());
        return w;
    }

    private static int weightOf(int type) {
        Supply s = Supply.byOrdinal(type);
        return s == null ? 1 : s.weight(); // opaque/test type ids default to 1
    }

    /** STR-scaled weight capacity (AC-2, D3). Callers pass {@code RoguePlayer.getStr()}, which already
     *  folds the Story-1.7 bacterial/Starving STR penalty — so encumbrance tightens when weakened. */
    public int carryCapacity(int str) {
        return BASE_CAPACITY + str * STR_CAPACITY_FACTOR;
    }

    /** Whether the carried weight exceeds the STR-scaled capacity (AC-2). Never blocks carrying —
     *  the turn loop pays the cost (shorter foray range); you can always over-pack and drop later. */
    public boolean isEncumbered(int str) {
        return totalWeight() > carryCapacity(str);
    }
}

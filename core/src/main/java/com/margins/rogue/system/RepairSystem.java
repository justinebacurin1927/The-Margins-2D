package com.margins.rogue.system;

import com.margins.rogue.item.Inventory;
import com.margins.rogue.item.Supply;
import com.margins.rogue.item.Weapon;
import com.margins.rogue.state.RunState;

import java.util.List;

/**
 * Story 4.5 (FR-13, AD-13): the gear economy's material↔weapon exchange. Repair consumes
 * weapon-category materials and applies the AD-13 SKILL curve (via {@link Weapon#repair}); scavenge
 * strips a broken weapon for tier-scaled base materials and removes it. Both are lossy — the
 * 6-repair ceiling (Story 4.4) still caps a weapon's total fighting life, so no infinite-gear loop
 * exists (AD-17). Pure model, no libGDX (AD-2); owns only the exchange (no HP/detection mutation).
 *
 * <p>Materials are drawn from the supplies that already exist (Wood/Rope/Small Tools) — D1: the full
 * weapon-specific taxonomy (String/Sinew, dedicated Metal Scrap, rare/unique drops) is deferred
 * content, so this ships the mechanism without new {@code Supply} churn.
 */
public final class RepairSystem {

    private RepairSystem() {}

    private static final int WOOD = Supply.WOOD.ordinal();
    private static final int ROPE = Supply.ROPE.ordinal();
    private static final int TOOLS = Supply.SMALL_TOOLS.ordinal();

    /** Deterministic base-material yield by tier (D2, AD-5 rng-free): T1..T5 → 1/2/2/3/4. */
    private static final int[] SCAVENGE_BY_TIER = {1, 2, 2, 3, 4};

    /** The category's repair cost as {supplyOrdinal, count} pairs (D1). */
    static int[][] repairCost(Weapon.Category c) {
        switch (c) {
            case SPEAR: return new int[][]{{WOOD, 1}, {ROPE, 1}};
            case BOW:   return new int[][]{{WOOD, 1}, {ROPE, 1}}; // String/Sinew deferred → Rope
            case BLADE: return new int[][]{{TOOLS, 1}};           // Small Tools = the metal-scrap proxy
            case AXE:   return new int[][]{{WOOD, 1}, {TOOLS, 1}};
            case CLUB:  return new int[][]{{WOOD, 1}};
            default:    return new int[][]{{WOOD, 1}};
        }
    }

    /** The material a scavenged weapon of this category returns (D2). */
    static int primaryMaterial(Weapon.Category c) {
        return (c == Weapon.Category.BLADE || c == Weapon.Category.AXE) ? TOOLS : WOOD;
    }

    static int scavengeCount(int tier) {
        int i = Math.max(1, Math.min(SCAVENGE_BY_TIER.length, tier)) - 1;
        return SCAVENGE_BY_TIER[i];
    }

    /**
     * AC-2: repair the wielded weapon. Refuses (no turn) when unarmed, when the weapon is beyond
     * repair (6th), when it isn't damaged (would waste the ceiling), or when the materials are
     * absent. On success the category materials are consumed and the AD-13 SKILL curve is applied.
     */
    public static boolean repair(RunState state, List<String> messages) {
        Weapon w = state.getWieldedWeapon();
        if (w == null) { messages.add("Wield the weapon you mean to mend."); return false; }
        if (w.getDurability() >= w.getMaxDurability()) { messages.add("It doesn't need mending."); return false; }
        if (!w.isRepairable()) { messages.add("It's beyond repair — strip it for parts."); return false; }
        int[][] cost = repairCost(w.getCategory());
        Inventory inv = state.getInventory();
        for (int[] c : cost) {
            if (inv.count(c[0]) < c[1]) { messages.add("You lack the materials to mend it."); return false; }
        }
        for (int[] c : cost) inv.remove(c[0], c[1]);
        w.repair(state.getPlayer().getSkill());
        messages.add("You mend the " + w.displayName() + " — but it will never be what it was.");
        return true;
    }

    /**
     * AC-1: strip the first broken weapon for parts. Refuses (no turn) when nothing is broken or the
     * pack can't accept the yield (no material loss — {@code tryAdd} is all-or-nothing). On success
     * the tier-scaled base material is added and the weapon is removed (wieldedIndex kept valid).
     */
    public static boolean scavenge(RunState state, List<String> messages) {
        int idx = -1;
        for (int i = 0; i < state.getWeapons().size(); i++) {
            if (state.getWeapons().get(i).isBroken()) { idx = i; break; }
        }
        if (idx < 0) { messages.add("Nothing broken to scavenge."); return false; }
        Weapon w = state.getWeapons().get(idx);
        int mat = primaryMaterial(w.getCategory());
        int count = scavengeCount(w.getTier());
        if (state.getInventory().tryAdd(mat, count) == Inventory.AddResult.BACKPACK_FULL) {
            messages.add("No room in your pack for the parts.");
            return false;
        }
        state.removeWeapon(idx);
        messages.add("You strip the " + w.displayName() + " for parts (" + count + " " + Supply.byOrdinal(mat).displayName() + ").");
        return true;
    }
}

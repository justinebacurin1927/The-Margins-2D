package com.margins.rogue.system;

import com.margins.rogue.item.Inventory;
import com.margins.rogue.item.Supply;
import com.margins.rogue.state.RunState;

import java.util.List;

/**
 * Cooking at a fire (FR-6, Story 1.5, AC-3/4): a SKILL-governed roll (AD-5 RNG)
 * turns one raw/half-rotten meat into cooked meat, which resists spoilage. A botched
 * cook ruins the meat (→ spoiled). Requires the campfire fire-station in range. A
 * refused cook (no fire, no raw meat, no room) commits no turn. No libGDX types (AD-2).
 */
public final class CookingSystem {
    private CookingSystem() {}

    /** Cook one unit of {@code itemType} at the fire. Returns true if a turn was spent. */
    public static boolean cook(RunState state, int itemType, List<String> messages) {
        Supply s = Supply.byOrdinal(itemType);
        if (s == null || s.cooksTo() == null) return false;           // not cookable → no turn
        if (!state.isPlayerAtFire()) { messages.add("No fire to cook at."); return false; }
        Inventory inv = state.getInventory();
        if (inv.count(itemType) <= 0) return false;

        boolean success = state.rng().nextInt(100) < SurvivalCraft.skillChance(state);
        Supply out = success ? s.cooksTo() : Supply.SPOILED_MEAT;     // a failed cook ruins it
        inv.remove(itemType, 1);
        if (inv.tryAdd(out.ordinal(), 1) == Inventory.AddResult.BACKPACK_FULL) {
            inv.tryAdd(itemType, 1);                                  // undo: no room for the result
            messages.add("No room to cook.");
            return false;
        }
        messages.add(success ? "Cooked " + out.displayName() + "." : "Burned it — " + out.displayName() + ".");
        return true;
    }
}

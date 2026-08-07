package com.margins.rogue.system;

import com.margins.rogue.item.Inventory;
import com.margins.rogue.item.Supply;
import com.margins.rogue.state.RunState;

import java.util.List;

/**
 * Two-step water purification (FR-6, Story 1.5, AC-1/2/4):
 * <ul>
 *   <li><b>Filter</b> — a SKILL-governed roll (AD-5 RNG) turns raw water into filtered
 *       water, which <i>reduces</i> its poison risk but never to zero. No fire needed.</li>
 *   <li><b>Boil</b> — deterministic given fuel: at the campfire fire-station and with one
 *       Coal, raw/filtered water becomes clean (0% risk) Boiled Water. Boiling is what makes
 *       water safe.</li>
 * </ul>
 * A refused step (not filterable/boilable, no fire, no coal, no room) commits no turn.
 * No libGDX types (AD-2).
 */
public final class PurificationSystem {
    private PurificationSystem() {}

    /** Filter one unit of raw water (SKILL-based). Returns true if a turn was spent. */
    public static boolean filter(RunState state, int itemType, List<String> messages) {
        Supply s = Supply.byOrdinal(itemType);
        if (s == null || s.filtersTo() == null) return false;         // not filterable → no turn
        Inventory inv = state.getInventory();
        if (inv.count(itemType) <= 0) return false;

        if (state.rng().nextInt(100) >= SurvivalCraft.skillChance(state)) {
            messages.add("Filtering failed.");                        // turn spent, water unchanged
            return true;
        }
        Supply out = s.filtersTo();
        inv.remove(itemType, 1);
        if (inv.tryAdd(out.ordinal(), 1) == Inventory.AddResult.BACKPACK_FULL) {
            inv.tryAdd(itemType, 1);                                  // undo: no room for the result
            messages.add("No room to filter.");
            return false;
        }
        messages.add("Filtered water (still not safe).");
        return true;
    }

    /** Boil one unit of raw/filtered water at the fire, consuming one Coal. True if a turn was spent. */
    public static boolean boil(RunState state, int itemType, List<String> messages) {
        Supply s = Supply.byOrdinal(itemType);
        if (s == null || s.boilsTo() == null) return false;           // not boilable → no turn
        if (!state.isPlayerAtFire()) { messages.add("No fire to boil at."); return false; }
        Inventory inv = state.getInventory();
        if (inv.count(itemType) <= 0) return false;
        if (inv.count(Supply.COAL.ordinal()) <= 0) { messages.add("No coal to boil with."); return false; }

        Supply out = s.boilsTo();
        inv.remove(itemType, 1);
        if (inv.tryAdd(out.ordinal(), 1) == Inventory.AddResult.BACKPACK_FULL) {
            inv.tryAdd(itemType, 1);                                  // undo: no room for the result
            messages.add("No room to boil.");
            return false;
        }
        inv.remove(Supply.COAL.ordinal(), 1);                         // consume fuel only once committed
        messages.add("Boiled — clean water.");
        return true;
    }
}

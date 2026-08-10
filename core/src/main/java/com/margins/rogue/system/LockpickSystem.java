package com.margins.rogue.system;

import com.margins.rogue.RoguePlayer;
import com.margins.rogue.RogueTileMap;
import com.margins.rogue.item.Supply;
import com.margins.rogue.state.RunState;

import java.util.List;

/**
 * Story 3.5 (FR-11, AC-2): SKILL-governed lockpicking — Klein opens the Old House's locked cellar
 * ({@code StructureTable.lockedLoot}) with a single seeded SKILL-scaled roll (AD-5). The curve is
 * {@code SurvivalCraft.skillChance} — the same horizontal-growth axis that governs cooking and
 * purification (Story 1.5): higher SKILL opens more often. The opened state persists via FlagStore
 * (AD-7), so a reload neither re-locks the cellar nor double-places its loot (the 3.2
 * {@code structureLootPlaced} lesson, in FlagStore form — no new RunState field, AD-6). A refused
 * attempt (not at the Old House / no Small Tools / already open) commits no turn (the inert-USE
 * precedent); success AND failure spend the turn. Small Tools is NOT consumed (durability/repair is
 * Epic 4). Core-only (AD-2): effects land on the flag store, floor items, and message log — no new
 * tile, no noise, no extra clock tick beyond the acted pipeline.
 */
public final class LockpickSystem {
    private LockpickSystem() {}

    /** The run-scoped flag that the Old House's cellar has been opened (AD-7). Persisted, so a
     *  reload never re-locks the cellar nor double-places its loot. */
    public static final String KEY_CELLAR_OPENED = "old-house.cellar-opened";

    /** Try to open the Old House's locked cellar. Returns whether the attempt spent a turn.
     *  Preconditions (refused → no turn): Klein stands on an {@code STRUCTURE_OLD_HOUSE} cell, the
     *  cellar is not yet open, and he carries {@code SMALL_TOOLS} (the thematic lock tool, already a
     *  loot type). One seeded roll (AD-5) at the SurvivalCraft SKILL curve; on success the cellar's
     *  {@code lockedLoot} is placed in the Old House footprint (the placeStructureLoot pattern) and
     *  the opened flag is set; on failure the turn is spent with the lock holding. */
    public static boolean lockpick(RunState state, List<String> messages) {
        RoguePlayer player = state.getPlayer();
        if (state.getTileMap().getStructureType(player.getTileX(), player.getTileY())
                != RogueTileMap.STRUCTURE_OLD_HOUSE) {
            messages.add("There's nothing here to pick.");
            return false;
        }
        if (state.getFlagStore().get(KEY_CELLAR_OPENED) != 0) {
            messages.add("The cellar already stands open.");
            return false;
        }
        if (state.getInventory().count(Supply.SMALL_TOOLS.ordinal()) < 1) {
            messages.add("You need Small Tools to pick the lock.");
            return false;
        }
        if (state.rng().nextInt(100) < SurvivalCraft.skillChance(state)) {
            state.placeLockedCellarLoot(player.getTileX(), player.getTileY());
            state.getFlagStore().set(KEY_CELLAR_OPENED, 1);
            messages.add("You pick the lock — the cellar creaks open.");
        } else {
            messages.add("The lock holds.");
        }
        return true;
    }
}

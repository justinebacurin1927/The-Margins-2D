package com.margins.rogue.system;

import com.margins.rogue.item.Inventory;
import com.margins.rogue.item.Supply;
import com.margins.rogue.state.RunState;

import java.util.ArrayList;
import java.util.List;

/**
 * Food spoilage (FR-6, Story 1.5, AC-3). Runs on the acted path (AD-5): spoilage is ACCRUED
 * each acted turn (+2 unsalted, +1 with a Salt stack in the pack, threshold
 * {@code 2 * SPOIL_INTERVAL}) and at each threshold every perishable food stack advances one
 * stage (Raw → Half-Rotten → Fully Spoiled). Cooked meat and every water type resist
 * (no {@code spoilsTo}). Accrual, not a clock modulo, is why picking salt up or putting it
 * down mid-run never shifts a spoilage phase — the delay is exactly proportional to salt-held
 * turns (M2-review). Unsalted cadence is unchanged ({@link #SPOIL_INTERVAL} turns).
 *
 * <p>Batch model (the honest consequence of the stack inventory — no per-item age): a whole
 * stack advances together, and a stack that advances THIS tick is not advanced twice (the
 * pre-tick state is snapshotted first). No libGDX types (AD-2).
 */
public final class SpoilageSystem {
    public static final int SPOIL_INTERVAL = 50; // acted turns per spoilage stage (PRD Balance)

    private SpoilageSystem() {}

    public static void tick(RunState state) {
        state.tickSpoilageClock();
        Inventory inv = state.getInventory();
        // Accrual (M2-review): +2 per unsalted turn, +1 per salted turn. Toggling salt never
        // shifts a phase — the threshold is hit after exactly SPOIL_INTERVAL unsalted turns
        // (or twice that fully salted), regardless of when salt was held.
        boolean salted = inv.count(Supply.SALT.ordinal()) > 0;
        state.addSpoilageProgress(salted ? 1 : 2);
        if (state.getSpoilageProgress() < SPOIL_INTERVAL * 2) return;
        state.subtractSpoilageProgress(SPOIL_INTERVAL * 2);

        // Snapshot perishable stacks before any mutation, so a stack that advances this tick is
        // not advanced twice (a Raw that becomes Half-Rotten must not also become Spoiled now).
        List<int[]> advances = new ArrayList<>(); // {fromType, toType, count}
        for (int slot = 0; slot < Inventory.BACKPACK_STACKS; slot++) {
            int type = inv.backpackType(slot);
            if (type < 0) continue;
            Supply s = Supply.byOrdinal(type);
            if (s == null || s.spoilsTo() == null) continue;
            advances.add(new int[]{type, s.spoilsTo().ordinal(), inv.backpackCount(slot)});
        }
        for (int[] a : advances) {
            inv.remove(a[0], a[2]);
            inv.tryAdd(a[1], a[2]);
        }
    }
}

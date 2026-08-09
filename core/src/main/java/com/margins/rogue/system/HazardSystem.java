package com.margins.rogue.system;

import com.margins.rogue.state.RunState;
import com.margins.rogue.world.StructureTable;

import java.util.List;

/**
 * Story 3.2 (FR-10, AC-2): structure hazards fire when Klein steps onto a World-Structure cell.
 * The hazard is authored metadata ({@link StructureTable.Hazard}) — no new {@code RogueTile}, no
 * persisted state (AD-6); the effect lands on existing surfaces (HP, the message log). One seeded
 * roll per step (AD-5). The day/night baseline only — Story 3.4 flips several locations' danger
 * by overriding the hazard on the night path.
 */
public final class HazardSystem {

    private HazardSystem() {}

    /** Resolve the hazard (if any) on the tile the player just stepped onto (AC-2). A no-op on
     *  wilderness tiles (structure type NONE) or a structure with no hazard. */
    public static void step(RunState state, int x, int y, List<String> messages) {
        int type = state.getTileMap().getStructureType(x, y);
        if (type < 0) return; // wilderness — no hazard
        StructureTable.Structure structure = StructureTable.forType(type);
        if (structure == null) return; // defensive: an unknown structure type has no authored hazard
        structure.hazard.onStep(state.getPlayer(), state.rng(), messages);
    }
}

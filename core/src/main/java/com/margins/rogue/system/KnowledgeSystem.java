package com.margins.rogue.system;

import com.margins.rogue.state.RunState;
import com.margins.rogue.world.StructureTable;

/**
 * Story 3.5 (FR-11, AC-2): the queryable knowledge surface — answers "what does Klein know?",
 * composing the three knowledge sources WITHOUT duplicating their state (AD-6): item-safety stays
 * {@code IdentifyMap}'s job (the tests compose all three), map-fragment knowledge is the persisted
 * FlagStore count recorded on read (the Torn Page precedent — narration, no turn, never consumed,
 * Decision 4), and location-danger is a PURE query over Story 3.4's authored data
 * ({@code HazardSystem.nightFlipFor} / {@code StructureTable}) — never new state. Core-owned
 * (AD-1/AD-2): no libGDX types, headless-testable; no new RogueTile, no noise, no clock tick.
 */
public final class KnowledgeSystem {
    private KnowledgeSystem() {}

    /** The run-scoped count of map fragments read (AD-7) — the persisted, queryable map knowledge.
     *  Recorded on read by TurnEngine; a fragment is never consumed, so re-reading accumulates. */
    public static final String KEY_MAP_FRAGMENTS_READ = "knowledge.map-fragments-read";

    /** How many map fragments Klein has read (persisted knowledge; 0 = none). */
    public static int mapFragmentsRead(RunState state) {
        return state.getFlagStore().get(KEY_MAP_FRAGMENTS_READ);
    }

    /** The hazard Klein would face at a location at NIGHT — the location-danger knowledge (Story
     *  3.4's authored night flips): the Graveyard/Sunken Well/Poacher's Camp flip strictly worse,
     *  the Beehive Grove flips SAFER (NONE, the sole exception), every other structure keeps its
     *  authored hazard. A pure query over authored data — never persisted (AD-6). An unmapped
     *  structure type knows nothing (NONE, not an NPE). */
    public static StructureTable.Hazard locationNightHazard(int structureType) {
        StructureTable.Structure st = StructureTable.forType(structureType);
        if (st == null) return StructureTable.Hazard.NONE;
        StructureTable.Hazard flip = HazardSystem.nightFlipFor(structureType);
        return flip != null ? flip : st.hazard;
    }
}

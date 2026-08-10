package com.margins.rogue.system;

import com.margins.rogue.RogueTileMap;
import com.margins.rogue.Weather;
import com.margins.rogue.state.RunState;
import com.margins.rogue.world.StructureTable;

import java.util.List;

/**
 * Resolves the step-risks of moving onto a tile. Story 3.2 (AC-2) added the structures' authored
 * hazards; Story 3.3 (AC-3) adds the generic NIGHT OVERLAY on top — the concrete overreach of
 * "the return leg at night without light is more dangerous" (UJ-2). Core-only (AD-2): effects land
 * on existing surfaces (the message log + HP), never a new tile or persisted field; exactly one
 * seeded rng draw per roll (AD-5); no noise (only AD-9 emitters emit), no extra clock tick.
 */
public final class HazardSystem {

    /** Story 3.3 (AC-3): the generic night stumble's per-step chance (percent) and its cost. The
     *  overlay is probabilistic (one seeded draw per step, AD-5) and distinct from any structure's
     *  authored hazard. Tunable content (PRD §8) — the "return in the dark is riskier" overreach. */
    private static final int NIGHT_STUMBLE_CHANCE_PERCENT = 20;
    private static final int NIGHT_STUMBLE_DAMAGE = 1;
    /** The stumble's log line — announced with its damage (observation discipline, 1.8). */
    public static final String NIGHT_STUMBLE_MESSAGE = "You stumble in the dark.";

    /** Story 3.4 (AC-2): Storm's structural-collapse bonus (percentage points) added to a decayed
     *  structure's hazard chance while Storm is the active weather. Tunable content (PRD §8). */
    static final int STORM_STRUCTURAL_BONUS = 20;

    private HazardSystem() {}

    /** Resolve the step-risks on the tile the player just stepped onto: the generic NIGHT OVERLAY
     *  first (AC-3 — it applies to ANY step at night without light, wilderness or structure), then
     *  the structure's authored hazard (AC-2 — resolved through the {@link #nightHazardFor} seam).
     *  A no-op on wilderness tiles except for the night overlay. */
    public static void step(RunState state, int x, int y, List<String> messages) {
        nightOverlay(state, messages);
        int type = state.getTileMap().getStructureType(x, y);
        if (type < 0) return; // wilderness — no structure hazard
        StructureTable.Structure structure = StructureTable.forType(type);
        if (structure == null) return; // defensive: an unknown structure type has no authored hazard
        StructureTable.Hazard hazard = nightHazardFor(state, structure); // AC-1: the night flip (or day baseline)
        hazard.onStep(state.getPlayer(), state.rng(), messages, stormBonus(state, hazard)); // AC-2: Storm stacks
    }

    /** Story 3.4 (AC-2): the weather chance bonus for THIS hazard — Storm raises a structural
     *  hazard's collapse chance; every other weather (and every non-structural hazard) adds 0. The
     *  bonus lifts the chance of the SAME single seeded draw (AD-5), never a second roll. Fog and
     *  Cold Snap land their effects elsewhere (FovSystem/TemperatureSystem); Rain has no location
     *  effect — none touch the hazard chance here. */
    private static int stormBonus(RunState state, StructureTable.Hazard hazard) {
        return (state.getWeather() == Weather.STORM && hazard.isStructural()) ? STORM_STRUCTURAL_BONUS : 0;
    }

    /** Story 3.3 (AC-3, Decisions 3/4): the generic night-risk overlay. While it is Night AND Klein
     *  lacks a light — no torch burning, and not within his campfire's safe radius — every step
     *  risks a stumble in the dark: a small HP cost + a log line, distinct from any structure's
     *  authored hazard. STACKS with the structure hazard (a dark structure is doubly dangerous).
     *  Light is the counter (Decision 4): a torch's 60-turn burn and its Wood+Coal craft are the
     *  real cost — no free loop. The campfire suppression uses {@link RunState#isPlayerAtCampfireSafePoint()}
     *  — the SAME radius {@code onForay()} uses (review fix), so "at camp" and "protected at night"
     *  cover the same ground (a torch's light is carried, so it suppresses anywhere). Exactly one
     *  seeded rng draw per step where the overlay is probabilistic (AD-5); lands only on the message
     *  log + HP — no new tile, no persisted field, no noise, no extra clock tick. */
    private static void nightOverlay(RunState state, List<String> messages) {
        if (state.isDay()) return;                              // AC-3: night only
        if (state.getTorchTurns() > 0 || state.isPlayerAtCampfireSafePoint()) return; // under light / at camp
        if (state.rng().nextInt(100) < NIGHT_STUMBLE_CHANCE_PERCENT) {
            state.getPlayer().hurtRaw(NIGHT_STUMBLE_DAMAGE);
            messages.add(NIGHT_STUMBLE_MESSAGE);
        }
    }

    /** Story 3.4 (AC-1): the hazard to resolve for a structure — the per-location NIGHT flip, or the
     *  authored daytime baseline. By day every structure keeps {@code structure.hazard}. At night
     *  four named locations flip (FR-10): the Mercenary Graveyard's undead and the Sunken Well's
     *  creature become active, the Poacher's Camp patrols turn more aggressive (each strictly worse),
     *  and the Beehive Grove flips SAFER — its swarm goes dormant ({@code Hazard.NONE}, the sole
     *  exception). Every other structure keeps its authored hazard at all hours. Derived from
     *  {@code isDay()} (AD-6 — no persisted state); the flip REPLACES the day hazard, so a step still
     *  makes exactly one structure-hazard draw (AD-5). This is the seam Story 3.3 built; Story 3.4
     *  fills it without touching {@link #step}'s trigger. Package-private so the mapping is unit-testable. */
    static StructureTable.Hazard nightHazardFor(RunState state, StructureTable.Structure structure) {
        if (structure == null) return StructureTable.Hazard.NONE; // defensive: unmapped type has no hazard
                                                                  // (step() already guards this; harden the widened seam)
        if (state.isDay()) return structure.hazard;
        StructureTable.Hazard flip = nightFlipFor(structure.structureType);
        return flip != null ? flip : structure.hazard;            // unchanged at night unless it flips
    }

    /** Story 3.5 (AC-2): the NIGHT hazard for a structure type, independent of the current clock —
     *  the authored night flip from Story 3.4 (four named locations; the Beehive Grove flips SAFER),
     *  or null when a structure keeps its authored hazard at night. The location-danger knowledge
     *  query (KnowledgeSystem) reads this; {@link #nightHazardFor} uses it for the night branch, so
     *  the day baseline and the flip mapping each have one source. Package-private so the mapping
     *  is unit-testable. */
    static StructureTable.Hazard nightFlipFor(int structureType) {
        switch (structureType) {
            case RogueTileMap.STRUCTURE_GRAVEYARD:     return StructureTable.Hazard.GRAVE_UNDEAD;
            case RogueTileMap.STRUCTURE_SUNKEN_WELL:   return StructureTable.Hazard.WELL_CREATURE;
            case RogueTileMap.STRUCTURE_POACHERS_CAMP: return StructureTable.Hazard.POACHER_PATROL;
            case RogueTileMap.STRUCTURE_BEEHIVE_GROVE: return StructureTable.Hazard.NONE; // the sole safer-flip
            default:                                   return null;                        // no flip — keep the baseline
        }
    }
}

package com.margins.rogue.system;

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
        nightHazardFor(state, structure).onStep(state.getPlayer(), state.rng(), messages);
    }

    /** Story 3.3 (AC-3, Decisions 3/4): the generic night-risk overlay. While it is Night AND Klein
     *  lacks a light — no torch burning, and no campfire light at his tile (a lit campfire's light
     *  is stationary at the fire; walking away from it loses its protection, while a torch's light
     *  is carried) — every step risks a stumble in the dark: a small HP cost + a log line, distinct
     *  from any structure's authored hazard. STACKS with the structure hazard (a dark structure is
     *  doubly dangerous). Light is the counter (Decision 4): a torch's 60-turn burn and its
     *  Wood+Coal craft are the real cost — no free loop. Exactly one seeded rng draw per step where
     *  the overlay is probabilistic (AD-5); lands only on the message log + HP — no new tile, no
     *  persisted field, no noise, no extra clock tick. */
    private static void nightOverlay(RunState state, List<String> messages) {
        if (state.isDay()) return;                              // AC-3: night only
        if (state.getTorchTurns() > 0 || state.isPlayerAtFire()) return; // under light — you can see the ground
        if (state.rng().nextInt(100) < NIGHT_STUMBLE_CHANCE_PERCENT) {
            state.getPlayer().hurtRaw(NIGHT_STUMBLE_DAMAGE);
            messages.add(NIGHT_STUMBLE_MESSAGE);
        }
    }

    /** Story 3.4 SEAM: the hazard to resolve for a structure on the night path, or its daytime
     *  baseline. 3.3 authors NO per-location night states — every structure keeps its authored
     *  hazard at night; the generic {@link #nightOverlay} above is the only night change. 3.4 fills
     *  this with the per-location flips (Graveyard undead, Sunken Well creature, Poacher's Camp
     *  patrols, Beehive Grove safer) WITHOUT reworking the 3.3 trigger — extend here, not in
     *  {@link #step}. */
    private static StructureTable.Hazard nightHazardFor(RunState state, StructureTable.Structure structure) {
        return structure.hazard;
    }
}

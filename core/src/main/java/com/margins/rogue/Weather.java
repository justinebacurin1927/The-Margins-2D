package com.margins.rogue;

import java.util.Random;

/**
 * Per-cycle weather (PRD FR-5): exactly one type is rolled per 170-turn cycle on the
 * weighted distribution (Clear 40 / Rain 25 / Fog 20 / Storm 10 / Cold Snap 5). In this
 * story the type is queryable state — its EFFECTS (FOV shrink, temperature drift,
 * spoilage, structural collapse) are Stories 1.4/1.5/1.6/3.x, not built here.
 * No libGDX types (AD-2).
 */
public enum Weather {
    CLEAR("Clear", 40),
    RAIN("Rain", 25),
    FOG("Fog", 20),
    STORM("Storm", 10),
    COLD_SNAP("Cold Snap", 5);

    private final String label;
    private final int weight;

    Weather(String label, int weight) {
        this.label = label;
        this.weight = weight;
    }

    public String label() { return label; }

    /** Weighted roll over the FR-5 distribution (weights sum to 100). Draws from the seeded RNG (AD-5).
     *  The roll cap derives from the weights so a future nonzero-weight type is never silently
     *  un-rollable, and a weights-sum below the cap can't silently bias CLEAR. */
    public static Weather roll(Random rng) {
        int total = 0;
        for (Weather w : values()) total += w.weight;
        int roll = rng.nextInt(total);
        int cumulative = 0;
        for (Weather w : values()) {
            cumulative += w.weight;
            if (roll < cumulative) return w;
        }
        return CLEAR; // unreachable while total == sum(weights) and weights are positive
    }
}

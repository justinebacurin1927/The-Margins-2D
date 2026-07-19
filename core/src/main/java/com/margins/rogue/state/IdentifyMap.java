package com.margins.rogue.state;

import com.margins.rogue.item.Supply;
import com.margins.rogue.item.TrueIdentity;

import java.util.Random;

/**
 * The per-seed Supply → TrueIdentity binding (AD-12, FR-11). Built once at run
 * start from {@code RunState}'s seeded RNG (AD-5), so the same seed reproduces the
 * mapping and it is stable within a run. Stored as an enum array indexed by
 * {@link Supply#ordinal()} — plain data, serialized with {@code RunState} (AD-6),
 * no libGDX types (AD-2).
 *
 * <p>The paired {@code identified} set (AD-12) arrives in Story 3.4; this class is
 * the binding only.
 */
public class IdentifyMap {

    private TrueIdentity[] boundByOrdinal;

    public IdentifyMap() {} // for libGDX Json

    /** Bind each Supply type to one of its possible identities via the seeded RNG. */
    public static IdentifyMap build(Random rng) {
        IdentifyMap m = new IdentifyMap();
        Supply[] supplies = Supply.values();
        m.boundByOrdinal = new TrueIdentity[supplies.length];
        for (Supply s : supplies) {
            TrueIdentity[] opts = s.possibleIdentities();
            m.boundByOrdinal[s.ordinal()] = opts[rng.nextInt(opts.length)];
        }
        return m;
    }

    /** The true identity bound to a Supply type this run, or null if out of range. */
    public TrueIdentity identityOf(int supplyOrdinal) {
        return (boundByOrdinal != null && supplyOrdinal >= 0 && supplyOrdinal < boundByOrdinal.length)
                ? boundByOrdinal[supplyOrdinal] : null;
    }
}

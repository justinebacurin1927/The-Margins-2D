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
 * <p>It also owns the per-type {@code identified} set (AD-12, FR-12): using one
 * supply reveals its whole type for the rest of the run. This class is the single
 * source of truth for both the binding and what name a type shows — see
 * {@link #displayNameFor(int)} — so no other object duplicates identified state.
 */
public class IdentifyMap {

    private TrueIdentity[] boundByOrdinal;
    private boolean[] identifiedByOrdinal;

    public IdentifyMap() {} // for libGDX Json

    /** Bind each Supply type to one of its possible identities via the seeded RNG. */
    public static IdentifyMap build(Random rng) {
        IdentifyMap m = new IdentifyMap();
        Supply[] supplies = Supply.values();
        m.boundByOrdinal = new TrueIdentity[supplies.length];
        m.identifiedByOrdinal = new boolean[supplies.length]; // fresh run: nothing identified
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

    /** Whether the player has revealed this Supply type this run (FR-12). */
    public boolean isIdentified(int supplyOrdinal) {
        return (identifiedByOrdinal != null && supplyOrdinal >= 0 && supplyOrdinal < identifiedByOrdinal.length)
                && identifiedByOrdinal[supplyOrdinal];
    }

    /**
     * Reveal a Supply type for the rest of the run. Lazily allocates the set if it
     * is null — a pre-3.4 save has a binding but no {@code identifiedByOrdinal}
     * field, so it deserializes as null (AD-6). Out-of-range ordinals are ignored.
     */
    public void markIdentified(int supplyOrdinal) {
        if (supplyOrdinal < 0 || supplyOrdinal >= Supply.count()) return;
        if (identifiedByOrdinal == null) identifiedByOrdinal = new boolean[Supply.count()];
        identifiedByOrdinal[supplyOrdinal] = true;
    }

    /**
     * The name to show for a Supply type: its revealed {@link TrueIdentity} name once
     * identified, otherwise the unidentified {@link Supply#displayName()}. The one
     * place that decides identified-vs-hidden (FR-12).
     */
    public String displayNameFor(int supplyOrdinal) {
        Supply s = Supply.byOrdinal(supplyOrdinal);
        if (s == null) return "Item " + supplyOrdinal;
        if (isIdentified(supplyOrdinal)) {
            TrueIdentity id = identityOf(supplyOrdinal);
            if (id != null) return id.displayName();
        }
        return s.displayName();
    }
}

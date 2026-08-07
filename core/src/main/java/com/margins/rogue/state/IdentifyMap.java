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
            // A single-identity type needs no gamble — binding it would consume a seeded draw for
            // nothing and shift every downstream RNG consumer (H1-review). Only ambiguous types
            // (2+ identities) draw; the deterministic single-identity types (the Story 1.5
            // provisions) cost the stream nothing, keeping actor/supply placement byte-identical.
            m.boundByOrdinal[s.ordinal()] = opts.length > 1 ? opts[rng.nextInt(opts.length)] : opts[0];
        }
        return m;
    }

    /**
     * Grow the per-run binding to cover a Supply enum that has since gained values (AD-6,
     * appended-ordinal migration). A save written before the new values serializes a shorter
     * {@code boundByOrdinal}; without this the new ordinals read {@code identityOf(...) == null}
     * — nourishment silently skipped while the item is still consumed and its risk still rolled
     * (Edge #1-review). Appended types are deterministic single-identity provisions, so binding
     * them to {@code possible[0]} costs no RNG draw (the resumed stream stays aligned, AD-5).
     * Idempotent: a full save has nothing to grow.
     */
    public void reconcile(int supplyCount) {
        if (boundByOrdinal == null || boundByOrdinal.length >= supplyCount) return;
        TrueIdentity[] grown = new TrueIdentity[supplyCount];
        System.arraycopy(boundByOrdinal, 0, grown, 0, boundByOrdinal.length);
        boolean[] idGrown = new boolean[supplyCount];
        if (identifiedByOrdinal != null) {
            System.arraycopy(identifiedByOrdinal, 0, idGrown, 0, identifiedByOrdinal.length);
        }
        for (int i = boundByOrdinal.length; i < supplyCount; i++) {
            Supply s = Supply.byOrdinal(i);
            grown[i] = (s == null) ? null : s.possibleIdentities()[0];
        }
        boundByOrdinal = grown;
        identifiedByOrdinal = idGrown;
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

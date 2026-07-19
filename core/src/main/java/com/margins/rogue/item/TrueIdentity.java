package com.margins.rogue.item;

import com.margins.rogue.RoguePlayer;

/**
 * The real effect a {@link Supply} turns out to be (FR-11). Each Supply type is
 * bound to one of its possible identities per seed by {@code IdentifyMap}; using
 * a supply applies its bound identity's effect.
 *
 * <p>MVP note: the PRD's status-based "bad" outcomes (Weaken, Bleed) have no
 * status system yet, so they are approximated here with the HP/hunger effects
 * that exist — enough to keep the identify-by-use gamble a real risk (UJ-2).
 * Magnitudes are first-pass and tunable. No libGDX types (AD-2).
 */
public enum TrueIdentity {
    STALE_BREAD("Stale bread")     { public void apply(RoguePlayer p) { p.eat(40); } },        // PRD: food +40
    SPOILED_MEAT("Spoiled meat")   { public void apply(RoguePlayer p) { p.hurtRaw(10); p.starve(15); } },
    CLEAN_WATER("Clean water")     { public void apply(RoguePlayer p) { p.eat(15); } },
    TAINTED("Tainted water")       { public void apply(RoguePlayer p) { p.hurtRaw(5); } },      // Weaken approximated
    FEVERWORT("Feverwort paste")   { public void apply(RoguePlayer p) { p.heal(4); } },         // Bleed-cure approximated
    RENDERED_FAT("Rendered fat")   { public void apply(RoguePlayer p) { p.heal(4); } },
    BANDAGES("Bandages")           { public void apply(RoguePlayer p) { p.heal(6); } },
    OLD_RAGS("Old rags")           { public void apply(RoguePlayer p) { /* no effect, weight only */ } },
    INERT_LETTER("Sealed letter")  { public void apply(RoguePlayer p) { /* inert in MVP */ } };

    private final String displayName;

    TrueIdentity(String displayName) {
        this.displayName = displayName;
    }

    public abstract void apply(RoguePlayer p);

    public String displayName() {
        return displayName;
    }
}

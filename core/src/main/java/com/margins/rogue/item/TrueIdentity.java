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
    INERT_LETTER("Sealed letter")  { public void apply(RoguePlayer p) { /* inert in MVP */ } },

    // Story 1.5 provisions. apply() is the NOURISHMENT only; the poison-risk roll (which needs
    // the seeded RNG) is added on top by ConsumptionSystem for risky provisions (FR-6). Coal and
    // Salt are not consumed via use (fuel / storage) — their apply is inert.
    RAW_MEAT_ID("Raw meat")             { public void apply(RoguePlayer p) { p.eat(30); } },
    HALF_ROTTEN_ID("Half-rotten meat")  { public void apply(RoguePlayer p) { p.eat(20); } },
    SPOILED_MEAT_ID("Spoiled meat")     { public void apply(RoguePlayer p) { p.eat(10); } },
    COOKED_MEAT_ID("Cooked meat")       { public void apply(RoguePlayer p) { p.eat(60); } },
    WELL_WATER_ID("Well water")         { public void apply(RoguePlayer p) { p.drink(60); } },
    RIVER_WATER_ID("River water")       { public void apply(RoguePlayer p) { p.drink(50); } },
    POND_WATER_ID("Pond water")         { public void apply(RoguePlayer p) { p.drink(50); } },
    FILTERED_WATER_ID("Filtered water") { public void apply(RoguePlayer p) { p.drink(50); } },
    BOILED_WATER_ID("Boiled water")     { public void apply(RoguePlayer p) { p.drink(60); } },
    COAL_ID("Coal")                     { public void apply(RoguePlayer p) { /* fuel — inert on use */ } },
    SALT_ID("Salt")                     { public void apply(RoguePlayer p) { /* storage — inert on use */ } },
    WOOD_ID("Wood")                     { public void apply(RoguePlayer p) { /* fuel/material — inert on use */ } },

    // Story 1.7 debuff supplies. The mushroom effects are NOT applied here — the toxin needs the
    // run (messages) and is deterministic, so ConsumptionSystem routes it to DebuffSystem after
    // apply(). The cures are pure RoguePlayer state changes (no RNG), so they fit apply().
    TOXIC_MUSHROOM_ID("Toxic mushroom")     { public void apply(RoguePlayer p) { /* toxin handled by DebuffSystem (needs messages) */ } },
    HONEYMOON_MUSHROOM_ID("Honeymoon mushroom") { public void apply(RoguePlayer p) { /* toxin handled by DebuffSystem */ } },
    HONEY_ID("Honey")                       { public void apply(RoguePlayer p) { p.cureWithHoney(); } },
    HONEYCOMB_ID("Honeycomb")               { public void apply(RoguePlayer p) { p.cureWithHoney(); p.eat(10); } },
    BLOODVEIN_ID("Bloodvein mushroom")      { public void apply(RoguePlayer p) { p.hurtRaw(5); p.clearBloated(); } }, // 90% risk rides ConsumptionSystem's roll
    HERBAL_CURE_ID("Herbal cure")           { public void apply(RoguePlayer p) { p.applyHerbalCure(); } };

    private final String displayName;

    TrueIdentity(String displayName) {
        this.displayName = displayName;
    }

    public abstract void apply(RoguePlayer p);

    public String displayName() {
        return displayName;
    }
}

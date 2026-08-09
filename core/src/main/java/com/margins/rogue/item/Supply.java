package com.margins.rogue.item;

/**
 * The MVP Route-1 Supply set (FR-10/11): a stackable consumable TYPE. The backpack
 * stores each as its non-negative {@link #ordinal()} (honoring the {@code -1}
 * empty-slot sentinel reserved in {@link Inventory}).
 *
 * <p>A type's actual effect is its bound {@link TrueIdentity}, chosen per seed from
 * {@link #possibleIdentities()} by {@code IdentifyMap} (FR-11) — so "Sealed
 * Waterskin" can be clean water on one seed and tainted on another. Story 3.4 hides
 * the identity behind the unidentified {@link #displayName()} until first use. No
 * libGDX types (AD-2).
 */
public enum Supply {
    WRAPPED_BUNDLE("Wrapped Bundle", TrueIdentity.STALE_BREAD, TrueIdentity.SPOILED_MEAT),
    SEALED_WATERSKIN("Sealed Waterskin", TrueIdentity.CLEAN_WATER, TrueIdentity.TAINTED),
    SMALL_TIN("Small Tin", TrueIdentity.FEVERWORT, TrueIdentity.RENDERED_FAT),
    FOLDED_CLOTH("Folded Cloth", TrueIdentity.BANDAGES, TrueIdentity.OLD_RAGS),
    SEALED_LETTER("Sealed Letter", TrueIdentity.INERT_LETTER),

    // Story 1.5 provisions (FR-6). Self-evident, deterministic single-identity types — their
    // Supply name IS the real name, so they read correctly without the identify gamble. Appended
    // last so existing ordinals (and old saves) are unchanged (AD-6). Spoilage/cook/purify are
    // type-swaps (remove old + add new) within the stack model — the taxonomy below is the data.
    COAL("Coal", TrueIdentity.COAL_ID),
    RAW_MEAT("Raw Meat", TrueIdentity.RAW_MEAT_ID),
    HALF_ROTTEN_MEAT("Half-Rotten Meat", TrueIdentity.HALF_ROTTEN_ID),
    SPOILED_MEAT("Spoiled Meat", TrueIdentity.SPOILED_MEAT_ID),
    COOKED_MEAT("Cooked Meat", TrueIdentity.COOKED_MEAT_ID),
    WELL_WATER("Well Water", TrueIdentity.WELL_WATER_ID),
    POND_WATER("Pond Water", TrueIdentity.POND_WATER_ID),
    RIVER_WATER("River Water", TrueIdentity.RIVER_WATER_ID),
    FILTERED_WATER("Filtered Water", TrueIdentity.FILTERED_WATER_ID),
    BOILED_WATER("Boiled Water", TrueIdentity.BOILED_WATER_ID),
    SALT("Salt", TrueIdentity.SALT_ID),
    // Story 1.6 craft material (FR-7): fuel for the torch (1 Wood + 1 Coal). Single-identity
    // (inert on use — a material, not consumed via USE), so the H1 no-RNG-draw rule applies.
    WOOD("Wood", TrueIdentity.WOOD_ID),

    // Story 1.7 debuff supplies (FR-8). The mushrooms carry the toxin track (deterministic — no
    // risk roll); honey/honeycomb/bloodvein/herbal-cure are the cures. All single-identity and
    // self-evident (name IS the real name), appended last so existing ordinals are unchanged (AD-6).
    TOXIC_MUSHROOM("Toxic Mushroom", TrueIdentity.TOXIC_MUSHROOM_ID),
    HONEYMOON_MUSHROOM("Honeymoon Mushroom", TrueIdentity.HONEYMOON_MUSHROOM_ID),
    HONEY("Honey", TrueIdentity.HONEY_ID),
    HONEYCOMB("Honeycomb", TrueIdentity.HONEYCOMB_ID),
    BLOODVEIN_MUSHROOM("Bloodvein Mushroom", TrueIdentity.BLOODVEIN_ID),
    HERBAL_CURE("Herbal Cure", TrueIdentity.HERBAL_CURE_ID),

    // Story 2.4 discovery seed (FR-3): Aldric's torn order, left where he was taken. A single-
    // identity lore note (inert on use — reading is narration) so the H1 no-RNG-draw rule applies,
    // and it stays in the backpack as the seed Story 2.5's discovery-triggered quests hook.
    TORN_PAGE("Torn Page", TrueIdentity.CHASERS_ORDER),

    // Story 3.2 (FR-10, AC-2): the World-Structure loot items. Structure-destination loot ONLY —
    // Rope / Small Tools are inert craft materials (the repair economics are Story 4.5), Map
    // Fragment is the inert knowledge collectible (the query is Story 3.5), Preserved Food is a
    // nourishing provision. Appended last so existing ordinals (and old saves) are unchanged (AD-6).
    // Deliberately NOT scatterable: admitting them to the generic eastness scatter would grow its
    // pool and shift every existing seed's layout (AD-5 byte-identical stream) — structure loot is
    // an authored pass (RunState.placeStructureLoot), not random forest junk.
    ROPE("Rope", TrueIdentity.ROPE_ID),
    SMALL_TOOLS("Small Tools", TrueIdentity.SMALL_TOOLS_ID),
    MAP_FRAGMENT("Map Fragment", TrueIdentity.MAP_FRAGMENT_ID),
    PRESERVED_FOOD("Preserved Food", TrueIdentity.PRESERVED_FOOD_ID);

    private final String displayName;
    private final TrueIdentity[] possible;

    Supply(String displayName, TrueIdentity... possible) {
        this.displayName = displayName;
        this.possible = possible;
    }

    /** The identities this type may bind to for a run (FR-11). */
    public TrueIdentity[] possibleIdentities() {
        return possible;
    }

    /** Spent on use, except inert types (the Sealed Letter, the Torn Page, and fuel/storage/craft
     *  materials that aren't eaten). The Story 3.2 materials (Rope, Small Tools, Map Fragment) are
     *  carried loot with their uses in later stories (4.5/3.5) — USING one is a no-op. */
    public boolean isConsumedOnUse() {
        return this != SEALED_LETTER && this != TORN_PAGE && this != COAL && this != SALT && this != WOOD
                && this != ROPE && this != SMALL_TOOLS && this != MAP_FRAGMENT;
    }

    /** The toxin track a provision carries (Story 1.7, FR-8). Mushrooms are deterministic — a
     *  toxin applies on consumption with no risk roll (the player chose to eat it); everything
     *  else is NONE and rides the {@link #drinkRisk()} bacterial roll instead. */
    public enum Toxin { NONE, ROTGUT, HONEYMOON }

    /** A food/water provision consumed for nourishment (Story 1.5), vs. containers/fuel/storage.
     *  Story 1.7 adds the mushrooms and cures — they must route through ConsumptionSystem too. */
    public boolean isProvision() {
        switch (this) {
            case RAW_MEAT: case HALF_ROTTEN_MEAT: case SPOILED_MEAT: case COOKED_MEAT:
            case WELL_WATER: case POND_WATER: case RIVER_WATER: case FILTERED_WATER: case BOILED_WATER:
            case TOXIC_MUSHROOM: case HONEYMOON_MUSHROOM:
            case HONEY: case HONEYCOMB: case BLOODVEIN_MUSHROOM: case HERBAL_CURE:
            case PRESERVED_FOOD:
                return true;
            default: return false;
        }
    }

    /** The poison risk (percent 0..100) of consuming this provision untreated (FR-6). 0 = safe.
     *  Bloodvein Mushroom's 90% contamination rides this roll (PRD FR-8, Story 1.7). */
    public int drinkRisk() {
        switch (this) {
            case SPOILED_MEAT:     return 90;
            case BLOODVEIN_MUSHROOM: return 90; // the cure comes at a 90% poisoning cost (FR-8)
            case POND_WATER:       return 60;
            case HALF_ROTTEN_MEAT: return 40;
            case RIVER_WATER:      return 20; // AC-1: river direct-drink 20%
            case FILTERED_WATER:   return 10; // AC-2: filtration reduces but does not eliminate
            case RAW_MEAT:         return 10;
            default:               return 0;  // COOKED_MEAT, WELL_WATER, BOILED_WATER are safe
        }
    }

    /** The toxin this provision carries, or NONE if it rides the bacterial roll instead (FR-8). */
    public Toxin toxin() {
        switch (this) {
            case TOXIC_MUSHROOM:    return Toxin.ROTGUT;
            case HONEYMOON_MUSHROOM: return Toxin.HONEYMOON;
            default:                return Toxin.NONE;
        }
    }

    /** A medicine (Story 1.7, FR-8): never refused on a full stomach (Decision 6). */
    public boolean isCure() {
        switch (this) {
            case HONEY: case HONEYCOMB: case BLOODVEIN_MUSHROOM: case HERBAL_CURE:
                return true;
            default: return false;
        }
    }

    /** A drink (water) provision vs. a food (meat) provision — ConsumptionSystem picks the
     *  "already full" refusal guard by this (Edge #2-review). */
    public boolean isWater() {
        switch (this) {
            case WELL_WATER: case POND_WATER: case RIVER_WATER: case FILTERED_WATER: case BOILED_WATER:
                return true;
            default: return false;
        }
    }

    /** A food provision (nourishment that is not a drink) — the tutorial's EAT matcher keys on
     *  food USE so drinking water does not check off "eat" (review m1). */
    public boolean isFood() {
        switch (this) {
            case RAW_MEAT: case HALF_ROTTEN_MEAT: case SPOILED_MEAT: case COOKED_MEAT:
            case HONEY: case HONEYCOMB: case PRESERVED_FOOD:
                return true;
            default: return false;
        }
    }

    /** The next food-spoilage stage this type advances to over time, or null if it resists / n/a.
     *  Cooked meat and every water type resist the spoilage ladder (FR-6). */
    public Supply spoilsTo() {
        switch (this) {
            case RAW_MEAT:         return HALF_ROTTEN_MEAT;
            case HALF_ROTTEN_MEAT: return SPOILED_MEAT;
            default:               return null; // SPOILED_MEAT terminal; COOKED_MEAT + waters resist
        }
    }

    /** The meat this type cooks into at a fire, or null if not cookable. */
    public Supply cooksTo() {
        return (this == RAW_MEAT || this == HALF_ROTTEN_MEAT) ? COOKED_MEAT : null;
    }

    /** The filtered water this raw water yields (SKILL-gated), or null if not filterable. */
    public Supply filtersTo() {
        switch (this) {
            case WELL_WATER: case POND_WATER: case RIVER_WATER: return FILTERED_WATER;
            default: return null;
        }
    }

    /** The clean water this raw/filtered water yields when boiled (needs fire + coal), or null. */
    public Supply boilsTo() {
        switch (this) {
            case WELL_WATER: case POND_WATER: case RIVER_WATER: case FILTERED_WATER: return BOILED_WATER;
            default: return null;
        }
    }

    public String displayName() {
        return displayName;
    }

    private static final Supply[] VALUES = values();

    /** Map a backpack type id ({@link #ordinal()}) back to its Supply, or null if out of range. */
    public static Supply byOrdinal(int ordinal) {
        return (ordinal >= 0 && ordinal < VALUES.length) ? VALUES[ordinal] : null;
    }

    public static int count() {
        return VALUES.length;
    }

    /** Whether this type may appear in the random floor scatter (review M1). The quest-seed lore
     *  note (TORN_PAGE) must appear ONLY via its scripted capture placement, and the Story 3.2
     *  structure-loot items (Rope / Small Tools / Map Fragment / Preserved Food) must appear ONLY
     *  via the authored structure pass — excluding them keeps the generic scatter's pool length
     *  unchanged, so every existing seed's wilderness layout is byte-identical (AD-5). */
    public boolean isScatterable() {
        switch (this) {
            case TORN_PAGE: case ROPE: case SMALL_TOOLS: case MAP_FRAGMENT: case PRESERVED_FOOD:
                return false;
            default: return true;
        }
    }

    /** The scatterable types, precomputed once at class load for {@link #scatterableOrdinals()}. */
    private static final Supply[] SCATTERABLE = scatterableArray();

    private static Supply[] scatterableArray() {
        Supply[] all = values();
        int n = 0;
        for (Supply s : all) if (s.isScatterable()) n++;
        Supply[] out = new Supply[n];
        int i = 0;
        for (Supply s : all) if (s.isScatterable()) out[i++] = s;
        return out;
    }

    /** The ordinals the floor scatter may draw (RunState.placeFloorActors) — never the quest seed.
     *  One draw per placed item, so the seeded stream's call structure is unchanged (AD-5). */
    public static int[] scatterableOrdinals() {
        int[] out = new int[SCATTERABLE.length];
        for (int i = 0; i < SCATTERABLE.length; i++) out[i] = SCATTERABLE[i].ordinal();
        return out;
    }
}

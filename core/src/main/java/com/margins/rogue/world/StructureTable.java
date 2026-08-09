package com.margins.rogue.world;

import com.margins.rogue.RoguePlayer;
import com.margins.rogue.RogueTileMap;
import com.margins.rogue.item.Supply;

import java.util.List;
import java.util.Random;

/**
 * Story 3.2 (FR-9/FR-10, AC-1/AC-2): the authored content metadata for the 11 World-Structures.
 * One entry per {@link RogueTileMap#STRUCTURE_*} type, each carrying its display name, danger
 * tier (1-3), loot set and hazard. Deterministic from constants and transient (AD-6) — the
 * {@link WorldSpine} precedent: nothing here persists; RunState/TurnEngine derive per-structure
 * content by looking a structure type up from the persisted {@code structureTypes} layer.
 *
 * <p>Tier↔spine consistency (AC-1) is AUTHORED, not derived from position: the tier list matches
 * epics.md exactly, and the three canon exceptions (Deep Cave, Old House, Graveyard) carry a
 * written {@link Structure#tierExceptionReason} — so the monotonic-core test cannot silently
 * exempt them. Loot/hazard numbers are tunable content (PRD §8) except the two worked examples
 * (Hunter's Blind; Old House), which are contractual (PRD FR-10). No libGDX types (AD-2).
 */
public final class StructureTable {

    private StructureTable() {}

    /** The three danger tiers (AC-1): T1 mild, T2 mid, T3 severe — matching epics.md's list. */
    public enum Tier {
        ONE(1), TWO(2), THREE(3);
        public final int value;
        Tier(int value) { this.value = value; }
    }

    /**
     * A structure hazard: a step-risk that fires when Klein moves onto the structure (AC-2).
     * One seeded draw per step (AD-5); the effect lands on existing surfaces — HP and the message
     * log — never a new tile or persisted state. The day/night baseline only: Story 3.4 flips
     * several locations' danger (Graveyard undead, Sunken Well creature, Poacher's Camp patrols,
     * Beehive Grove safer) by overriding these on the night path.
     */
    public enum Hazard {
        NONE("none", 0, 0, ""),
        WEAK_FLOOR_PLANK("weak floor plank", 25, 1, "A floor plank gives way — you stumble."),
        SOFT_ROT("soft rot", 25, 1, "Rotting wood gives way — you sink a step."),
        COLLAPSING_STONE("collapsing stone", 20, 1, "A loose stone topples past you."),
        SWARM("swarm", 25, 1, "The grove swarms — you beat the bees off."),
        ASH_RESIDUE("ash residue", 20, 1, "Hot ash scorches your step."),
        TOWER_COLLAPSE("tower collapse", 20, 2, "Masonry falls around you."),
        SNARE_TRAP("snare trap", 25, 1, "A snare snaps shut on your ankle."),
        SLIP_AND_FALL("slip and fall", 20, 1, "The well's stones are slick — you slip."),
        STRUCTURAL_DECAY("structural decay", 20, 2, "The floor groans and a section collapses beneath you."),
        GRAVE_GROUND("grave-ground", 20, 1, "The grave-ground crumbles underfoot."),
        CAVE_IN("cave-in", 20, 2, "Stone shifts overhead at the cave mouth.");

        private final String displayName;
        private final int chancePercent;
        private final int damage;
        private final String message;

        Hazard(String displayName, int chancePercent, int damage, String message) {
            this.displayName = displayName;
            this.chancePercent = chancePercent;
            this.damage = damage;
            this.message = message;
        }

        public String displayName() { return displayName; }
        public int chancePercent() { return chancePercent; }
        public int damage() { return damage; }

        /** Step-trigger (AC-2): one seeded draw per step (AD-5); a hit applies the damage and
         *  announces it on the message log. NONE never fires. */
        public void onStep(RoguePlayer p, Random rng, List<String> messages) {
            if (chancePercent > 0 && rng.nextInt(100) < chancePercent) {
                if (damage > 0) p.hurtRaw(damage);
                if (!message.isEmpty()) messages.add(message);
            }
        }
    }

    /** One loot line: a {@link Supply} type, its count, and its chance (percent 1-100; 100 =
     *  guaranteed). Chance entries are one seeded draw per placement (AD-5). */
    public static final class LootEntry {
        public final Supply supply;
        public final int count;
        public final int chancePercent;
        LootEntry(Supply supply, int count, int chancePercent) {
            this.supply = supply;
            this.count = count;
            this.chancePercent = chancePercent;
        }
    }

    private static LootEntry loot(Supply supply, int count, int chancePercent) {
        return new LootEntry(supply, count, chancePercent);
    }

    /** The authored content for one World-Structure type. Instances are static finals; the loot
     *  arrays are FINAL and injected through the constructor, so a caller can never reassign
     *  another structure's content (review fix — the old withLoot/withLockedCellar fluents left
     *  mutable public arrays on the singletons). */
    public static final class Structure {
        public final int structureType;
        public final String displayName;
        public final Tier tier;
        public final Hazard hazard;
        /** Written reason when this structure is a named exception to tier↔eastness monotonicity
         *  (Decision 1); null for the monotone core. */
        public final String tierExceptionReason;
        /** The reachable floor loot (AC-2) — scattered in the structure's footprint by RunState. */
        public final LootEntry[] loot;
        /** The rich loot behind the Old House's locked cellar — data only in 3.2; the lockpicking
         *  roll that exposes it is Story 3.5's SKILL hook. Empty for every other structure. */
        public final LootEntry[] lockedLoot;

        Structure(int structureType, String displayName, Tier tier, Hazard hazard,
                  String tierExceptionReason, LootEntry[] loot, LootEntry[] lockedLoot) {
            this.structureType = structureType;
            this.displayName = displayName;
            this.tier = tier;
            this.hazard = hazard;
            this.tierExceptionReason = tierExceptionReason;
            this.loot = loot;
            this.lockedLoot = lockedLoot;
        }
    }

    /** The empty cellar marker: every structure except the Old House locks nothing. */
    private static final LootEntry[] NO_CELLAR = new LootEntry[0];

    // --- The 11 authored entries (AC-1/AC-2). Tiers match epics.md; the two worked examples
    // (Hunter's Blind, Old House) are contractual; the rest are tunable content (PRD §8).

    public static final Structure HUNTERS_BLIND = new Structure(
            RogueTileMap.STRUCTURE_HUNTERS_BLIND, "Hunter's Blind", Tier.ONE, Hazard.WEAK_FLOOR_PLANK, null,
            new LootEntry[]{loot(Supply.ROPE, 1, 100), loot(Supply.SMALL_TOOLS, 1, 100),
                    loot(Supply.MAP_FRAGMENT, 1, 20)},
            NO_CELLAR);

    public static final Structure FALLEN_LOG_HOLLOW = new Structure(
            RogueTileMap.STRUCTURE_FALLEN_LOG_HOLLOW, "Fallen Log Hollow", Tier.ONE, Hazard.SOFT_ROT, null,
            new LootEntry[]{loot(Supply.SMALL_TOOLS, 1, 100), loot(Supply.WOOD, 1, 100),
                    loot(Supply.SALT, 1, 50)},
            NO_CELLAR);

    public static final Structure FOREST_SHRINE = new Structure(
            RogueTileMap.STRUCTURE_FOREST_SHRINE, "Forest Shrine", Tier.ONE, Hazard.COLLAPSING_STONE, null,
            new LootEntry[]{loot(Supply.SALT, 1, 100), loot(Supply.COAL, 1, 60)},
            NO_CELLAR);

    public static final Structure BEEHIVE_GROVE = new Structure(
            RogueTileMap.STRUCTURE_BEEHIVE_GROVE, "Beehive Grove", Tier.ONE, Hazard.SWARM, null,
            new LootEntry[]{loot(Supply.HONEY, 1, 100), loot(Supply.HONEYCOMB, 1, 70)},
            NO_CELLAR);

    public static final Structure KITCHEN_CAMP = new Structure(
            RogueTileMap.STRUCTURE_KITCHEN_CAMP, "Worn Down Kitchen Camp", Tier.TWO, Hazard.ASH_RESIDUE, null,
            new LootEntry[]{loot(Supply.PRESERVED_FOOD, 1, 100), loot(Supply.SALT, 1, 100),
                    loot(Supply.COAL, 1, 60), loot(Supply.SMALL_TOOLS, 1, 50)},
            NO_CELLAR);

    public static final Structure COLLAPSED_WATCHTOWER = new Structure(
            RogueTileMap.STRUCTURE_COLLAPSED_WATCHTOWER, "Collapsed Watchtower", Tier.TWO,
            Hazard.TOWER_COLLAPSE, null,
            new LootEntry[]{loot(Supply.ROPE, 1, 100), loot(Supply.SMALL_TOOLS, 1, 80),
                    loot(Supply.MAP_FRAGMENT, 1, 25), loot(Supply.FOLDED_CLOTH, 1, 50)},
            NO_CELLAR);

    public static final Structure POACHERS_CAMP = new Structure(
            RogueTileMap.STRUCTURE_POACHERS_CAMP, "Poacher's Camp", Tier.TWO, Hazard.SNARE_TRAP, null,
            new LootEntry[]{loot(Supply.ROPE, 1, 100), loot(Supply.SMALL_TOOLS, 1, 70),
                    loot(Supply.RAW_MEAT, 1, 60), loot(Supply.FOLDED_CLOTH, 1, 40)},
            NO_CELLAR);

    public static final Structure SUNKEN_WELL = new Structure(
            RogueTileMap.STRUCTURE_SUNKEN_WELL, "Sunken Well", Tier.TWO, Hazard.SLIP_AND_FALL, null,
            new LootEntry[]{loot(Supply.COAL, 1, 100), loot(Supply.PRESERVED_FOOD, 1, 60),
                    loot(Supply.SALT, 1, 50), loot(Supply.SMALL_TOOLS, 1, 60)},
            NO_CELLAR);

    public static final Structure OLD_HOUSE = new Structure(
            RogueTileMap.STRUCTURE_OLD_HOUSE, "The Old House", Tier.THREE, Hazard.STRUCTURAL_DECAY,
            "home-cluster T3-by-hazard-depth (AD-8)",
            new LootEntry[]{loot(Supply.PRESERVED_FOOD, 2, 100), loot(Supply.FOLDED_CLOTH, 1, 100)},
            new LootEntry[]{loot(Supply.PRESERVED_FOOD, 3, 100), loot(Supply.FOLDED_CLOTH, 2, 100)});

    public static final Structure GRAVEYARD = new Structure(
            RogueTileMap.STRUCTURE_GRAVEYARD, "Mercenary Graveyard", Tier.THREE, Hazard.GRAVE_GROUND,
            "home-cluster T3-by-hazard-depth (AD-8)",
            new LootEntry[]{loot(Supply.FOLDED_CLOTH, 1, 100), loot(Supply.SMALL_TOOLS, 1, 50)},
            NO_CELLAR);

    public static final Structure DEEP_CAVE = new Structure(
            RogueTileMap.STRUCTURE_DEEP_CAVE, "Deep Cave Mouth", Tier.THREE, Hazard.CAVE_IN,
            "Region-2 threshold (AD-12) — T3 is transition depth, not surface east-west",
            new LootEntry[]{loot(Supply.PRESERVED_FOOD, 1, 100), loot(Supply.FOLDED_CLOTH, 1, 60),
                    loot(Supply.MAP_FRAGMENT, 1, 30)},
            NO_CELLAR);

    /** All 11 entries, in structure-type order (deterministic iteration for placement + tests). */
    public static final Structure[] ALL = {
            OLD_HOUSE, GRAVEYARD, DEEP_CAVE, HUNTERS_BLIND, FALLEN_LOG_HOLLOW, FOREST_SHRINE,
            BEEHIVE_GROVE, KITCHEN_CAMP, COLLAPSED_WATCHTOWER, POACHERS_CAMP, SUNKEN_WELL
    };

    /** The authored entry for a structure type, or null if the type has no content (defensive). */
    public static Structure forType(int structureType) {
        for (Structure st : ALL) {
            if (st.structureType == structureType) return st;
        }
        return null;
    }

    /** All authored structures (deterministic order — the placement + content tests iterate it). */
    public static Structure[] all() {
        return ALL;
    }
}

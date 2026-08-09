package com.margins.rogue;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import com.margins.rogue.item.FloorItem;
import com.margins.rogue.item.Supply;
import com.margins.rogue.state.FlagStore;
import com.margins.rogue.state.RunState;
import com.margins.rogue.system.PlayerAction;
import com.margins.rogue.system.TurnEngine;
import com.margins.rogue.world.StructureTable;
import com.margins.rogue.world.WorldSpine;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Story 3.2 (FR-9/FR-10, AC-1/AC-2): the 11 World-Structures' CONTENT layer — the authored danger
 * tiers consistent with the east/west spine (three named canon exceptions), the per-structure loot
 * sets (incl. the two contractual worked examples), and the step-trigger hazards. Placement,
 * footprints and connectivity are pinned by HybridMapTest/ContinuousMapTest; this suite owns what
 * a reached structure EXPOSES. Hazard/loot numbers are tunable content except the two worked
 * examples, which are contractual.
 */
class StructureContentTest {

    private static final int SEEDS = 24;

    // --- Task 1: the metadata model (AC: 1) ---

    @Test
    void theTableHasExactlyOneEntryPerStructureType() {
        for (int type = 0; type <= RogueTileMap.STRUCTURE_SUNKEN_WELL; type++) {
            assertNotNull(StructureTable.forType(type),
                    "structure type " + type + " has a table entry");
        }
        assertEquals(RogueTileMap.STRUCTURE_SUNKEN_WELL + 1, StructureTable.all().length,
                "one entry per STRUCTURE_* constant, no extras");
    }

    @Test
    void tiersMatchTheEpicsMembershipList() {
        assertEquals(StructureTable.Tier.ONE, tier(RogueTileMap.STRUCTURE_HUNTERS_BLIND));
        assertEquals(StructureTable.Tier.ONE, tier(RogueTileMap.STRUCTURE_FALLEN_LOG_HOLLOW));
        assertEquals(StructureTable.Tier.ONE, tier(RogueTileMap.STRUCTURE_FOREST_SHRINE));
        assertEquals(StructureTable.Tier.ONE, tier(RogueTileMap.STRUCTURE_BEEHIVE_GROVE));
        assertEquals(StructureTable.Tier.TWO, tier(RogueTileMap.STRUCTURE_KITCHEN_CAMP));
        assertEquals(StructureTable.Tier.TWO, tier(RogueTileMap.STRUCTURE_COLLAPSED_WATCHTOWER));
        assertEquals(StructureTable.Tier.TWO, tier(RogueTileMap.STRUCTURE_POACHERS_CAMP));
        assertEquals(StructureTable.Tier.TWO, tier(RogueTileMap.STRUCTURE_SUNKEN_WELL));
        assertEquals(StructureTable.Tier.THREE, tier(RogueTileMap.STRUCTURE_OLD_HOUSE));
        assertEquals(StructureTable.Tier.THREE, tier(RogueTileMap.STRUCTURE_GRAVEYARD));
        assertEquals(StructureTable.Tier.THREE, tier(RogueTileMap.STRUCTURE_DEEP_CAVE));
    }

    @Test
    void everyStructureResolvesALootSetAndAHazard() {
        for (StructureTable.Structure st : StructureTable.all()) {
            assertTrue(st.loot.length > 0, st.displayName + " has a loot set");
            assertNotEquals(StructureTable.Hazard.NONE, st.hazard, st.displayName + " has a hazard");
        }
    }

    @Test
    void theTwoWorkedExamplesResolve() {
        StructureTable.Structure blind = StructureTable.forType(RogueTileMap.STRUCTURE_HUNTERS_BLIND);
        assertEquals(StructureTable.Hazard.WEAK_FLOOR_PLANK, blind.hazard);
        assertTrue(hasLoot(blind, Supply.ROPE), "Hunter's Blind loots rope");
        assertTrue(hasLoot(blind, Supply.SMALL_TOOLS), "Hunter's Blind loots small tools");
        assertEquals(20, chanceOf(blind, Supply.MAP_FRAGMENT), "Map Fragment is a 20% drop");

        StructureTable.Structure house = StructureTable.forType(RogueTileMap.STRUCTURE_OLD_HOUSE);
        assertEquals(StructureTable.Hazard.STRUCTURAL_DECAY, house.hazard);
        assertTrue(hasLoot(house, Supply.PRESERVED_FOOD), "The Old House loots preserved food");
        assertTrue(hasLoot(house, Supply.FOLDED_CLOTH), "The Old House loots cloth");
        assertTrue(house.lockedLoot.length > 0, "The Old House has a locked cellar of rich loot");
    }

    // --- Task 2: the danger-tier spine (AC: 1) ---

    @Test
    void tiersRiseEastAcrossTheMonotoneCoreWithThreeNamedExceptions() {
        RunState s = new RunState(42L);
        RogueTileMap m = s.getTileMap();
        WorldSpine spine = new WorldSpine(m.getWidth(), m.getHeight());

        // Position each structure by the center of its stamped footprint (the generator's truth).
        List<StructureTable.Structure> byEastness = new ArrayList<>();
        for (StructureTable.Structure st : StructureTable.all()) {
            int[] box = footprint(m, st.structureType);
            assertNotNull(box, st.displayName + " is stamped on the map");
            byEastness.add(st);
        }
        byEastness.sort(Comparator.comparingDouble(st -> eastnessOf(m, spine, st)));

        // Test B first: the three canon exceptions are the ONLY non-monotone members, and each
        // carries a written reason — the check cannot silently pass by mass-exempting structures.
        List<StructureTable.Structure> withReason = new ArrayList<>();
        for (StructureTable.Structure st : StructureTable.all()) {
            if (st.tierExceptionReason != null) withReason.add(st);
        }
        assertEquals(3, withReason.size(), "exactly the three canon exceptions carry reasons");
        assertTrue(withReason.contains(StructureTable.forType(RogueTileMap.STRUCTURE_OLD_HOUSE)));
        assertTrue(withReason.contains(StructureTable.forType(RogueTileMap.STRUCTURE_GRAVEYARD)));
        assertTrue(withReason.contains(StructureTable.forType(RogueTileMap.STRUCTURE_DEEP_CAVE)));
        for (StructureTable.Structure st : withReason) {
            assertFalse(st.tierExceptionReason.isEmpty(), st.displayName + " reason is written");
        }

        // Every eastward descent must be caused by a too-high documented exception (the higher-tier
        // member of the pair), so a non-exception can never invert the gradient.
        for (int i = 1; i < byEastness.size(); i++) {
            StructureTable.Structure prev = byEastness.get(i - 1);
            StructureTable.Structure cur = byEastness.get(i);
            if (cur.tier.value < prev.tier.value) {
                assertNotNull(prev.tierExceptionReason,
                        "the descent " + prev.displayName + " (" + prev.tier
                                + ") → " + cur.displayName + " (" + cur.tier
                                + ") must be a named exception");
            }
        }

        // Test A: the monotone core (the 8 non-exception structures) is non-decreasing in eastness.
        StructureTable.Structure prevCore = null;
        for (StructureTable.Structure st : byEastness) {
            if (st.tierExceptionReason != null) continue;
            if (prevCore != null) {
                assertTrue(st.tier.value >= prevCore.tier.value,
                        "core structure " + st.displayName + " (" + st.tier + ") east of "
                                + prevCore.displayName + " (" + prevCore.tier + ") keeps tier rising east");
            }
            prevCore = st;
        }
    }

    // --- Task 3: loot sets on the structures (AC: 2) ---

    @Test
    void structureFootprintsYieldTheirAuthoredLoot() {
        // Guaranteed (100%) entries appear in the structure's footprint on a fixed seed; chance
        // entries appear across seeds (honored as a distribution, never a guarantee).
        RunState s = new RunState(42L);
        for (StructureTable.Structure st : StructureTable.all()) {
            int[] box = footprint(s.getTileMap(), st.structureType);
            for (StructureTable.LootEntry entry : st.loot) {
                if (entry.chancePercent >= 100) {
                    assertTrue(floorItemsOfTypeInBox(s, entry.supply.ordinal(), box) >= entry.count,
                            st.displayName + ": guaranteed " + entry.supply + " x" + entry.count
                                    + " sits in its footprint");
                }
            }
        }
        for (StructureTable.Structure st : StructureTable.all()) {
            for (StructureTable.LootEntry entry : st.loot) {
                if (entry.chancePercent >= 100) continue;
                boolean seen = false, missed = false;
                for (long seed = 0; seed < SEEDS * 2; seed++) {
                    RunState rs = new RunState(seed);
                    int[] box = footprint(rs.getTileMap(), st.structureType);
                    if (floorItemsOfTypeInBox(rs, entry.supply.ordinal(), box) > 0) seen = true;
                    else missed = true;
                }
                assertTrue(seen, st.displayName + ": " + entry.supply + " (" + entry.chancePercent
                        + "%) can drop");
                assertTrue(missed, st.displayName + ": " + entry.supply + " (" + entry.chancePercent
                        + "%) is a chance, not a guarantee");
            }
        }
    }

    @Test
    void authoredLootCannotBeFakedByTheGenericScatter() {
        // The generic eastness scatter also drops scatterable items near every room center, so a
        // count inside a structure box can pass even if placeStructureLoot placed nothing (review
        // fix). Prove the authored pass actually ran, three ways by the item's provability:
        //  (1) a guaranteed NON-scatterable item in the box can only be authored — the generic
        //      pool excludes the 4 structure items;
        //  (2) a guaranteed scatterable item in a box whose room gets ZERO generic items
        //      (eastness <= 0.2 → supplyCountFor 0) can only be authored;
        //  (3) otherwise (Beehive Grove: eastness ~0.46, generic ceiling 1, scatterable-only set)
        //      the guaranteed item is present on every fixed seed — one generic draw can't be
        //      HONEY on five different seeds.
        WorldSpine spine = new WorldSpine(96, 48);
        for (StructureTable.Structure st : StructureTable.all()) {
            for (StructureTable.LootEntry entry : st.loot) {
                if (entry.chancePercent < 100) continue; // guaranteed entries prove the pass ran
                RunState s = new RunState(42L);
                int[] box = footprint(s.getTileMap(), st.structureType);
                if (!entry.supply.isScatterable()) {
                    assertTrue(floorItemsOfTypeInBox(s, entry.supply.ordinal(), box) >= entry.count,
                            st.displayName + ": guaranteed non-scatterable " + entry.supply
                                    + " can only be authored (the generic pool excludes it)");
                } else {
                    float east = spine.eastness((box[0] + box[2]) / 2);
                    if (east <= 0.2f) {
                        assertTrue(floorItemsOfTypeInBox(s, entry.supply.ordinal(), box) >= entry.count,
                                st.displayName + " (eastness " + east + "): guaranteed " + entry.supply
                                        + " in a zero-generic room can only be authored");
                    } else {
                        for (long seed = 1; seed <= 5; seed++) {
                            RunState rs = new RunState(seed);
                            int[] b = footprint(rs.getTileMap(), st.structureType);
                            assertTrue(floorItemsOfTypeInBox(rs, entry.supply.ordinal(), b) >= entry.count,
                                    st.displayName + " (eastness " + east + "): guaranteed " + entry.supply
                                            + " present on seed " + seed
                                            + " — one generic draw cannot fake it on every seed");
                        }
                    }
                }
            }
        }
    }

    @Test
    void theNewLootItemsAreAppendedLastAndKeptOutOfTheGenericScatter() {
        // Appended last (AD-6): the 4 new types carry the highest ordinals, so old saves and
        // existing ordinals are unchanged.
        assertTrue(Supply.ROPE.ordinal() > Supply.TORN_PAGE.ordinal(), "ROPE appends after TORN_PAGE");
        assertTrue(Supply.SMALL_TOOLS.ordinal() > Supply.ROPE.ordinal(), "SMALL_TOOLS appends after ROPE");
        assertTrue(Supply.MAP_FRAGMENT.ordinal() > Supply.SMALL_TOOLS.ordinal(), "MAP_FRAGMENT appends last-ish");
        assertTrue(Supply.PRESERVED_FOOD.ordinal() > Supply.MAP_FRAGMENT.ordinal(), "PRESERVED_FOOD is the last ordinal");
        // Not scatterable: the generic eastness scatter's pool length is unchanged, so every
        // existing seed's wilderness layout stays byte-identical (AD-5).
        assertFalse(Supply.ROPE.isScatterable());
        assertFalse(Supply.SMALL_TOOLS.isScatterable());
        assertFalse(Supply.MAP_FRAGMENT.isScatterable());
        assertFalse(Supply.PRESERVED_FOOD.isScatterable());
        // The scatter pool excludes exactly the quest seed + the 4 structure items, so its length
        // is unchanged from the 3.1 baseline (byte-identical generic scatter, AD-5).
        assertEquals(Supply.count() - 5, Supply.scatterableOrdinals().length,
                "the generic scatter pool length is unchanged by the 4 appended structure items");
    }

    @Test
    void theSunkenWellIsAStableWaterSourceAtItsCenter() {
        RunState s = new RunState(42L);
        RogueTileMap m = s.getTileMap();
        int[] box = footprint(m, RogueTileMap.STRUCTURE_SUNKEN_WELL);
        assertNotNull(box);
        int cx = (box[0] + box[2]) / 2, cy = (box[1] + box[3]) / 2;
        assertEquals(RogueTile.WELL, m.getTile(cx, cy),
                "the Sunken Well's center is its namesake well — the stable water draw (PRD)");
        assertTrue(m.isWalkable(cx, cy), "the well cell stays walkable (WELL is a walkable tile)");
    }

    // --- Task 4: hazards (AC: 2) ---

    @Test
    void steppingOntoAStructureTriggersItsHazard() {
        for (StructureTable.Structure st : StructureTable.all()) {
            RunState s = runWithClearWeather(42L);
            int[] cell = walkableCellAndNeighbor(s.getTileMap(), st.structureType);
            assertNotNull(cell, st.displayName + " has a walkable cell with a walkable same-type neighbor");
            s.getPlayer().placeAt(cell[0], cell[1]);
            s.getEnemies().clear();
            s.getCompanions().clear();
            // Pace the room until the hazard fires: one seeded roll per landed step (AD-5), so a
            // 20–25% chance is bound to fire within a short deterministic sequence on this seed.
            // The OLD test accepted "0 OR damage", so it passed for any structure whose roll missed
            // — it never proved the damage path ran. Every landed step must drop ONLY the hazard's
            // damage (no hunger/cold leak), and the hazard must fire within the cap.
            boolean fired = false;
            int damageSeen = 0;
            TurnEngine engine = new TurnEngine();
            for (int i = 0; i < 40 && !fired; i++) {
                int dx = (i % 2 == 0 ? cell[2] : -cell[2]);
                int dy = (i % 2 == 0 ? cell[3] : -cell[3]);
                int hpBefore = s.getPlayer().getHp();
                engine.advance(s, PlayerAction.move(dx, dy, RoguePlayer.directionOf(dx, dy)));
                int dropped = hpBefore - s.getPlayer().getHp();
                assertTrue(dropped == 0 || dropped == st.hazard.damage(),
                        st.displayName + ": a step drops only the hazard's damage (got " + dropped + ")");
                if (dropped > 0) { fired = true; damageSeen = dropped; }
            }
            assertTrue(fired, st.displayName + ": its hazard fires on the fixed seed within 40 steps");
            assertEquals(st.hazard.damage(), damageSeen,
                    st.displayName + ": the firing step applies exactly the hazard's damage");
        }
    }

    @Test
    void enteringAStructureFromWildernessTriggersItsHazard() {
        // AC-2 "when I reach it": the trigger must fire on the wilderness→structure transition,
        // not only when pacing inside an interior. Find a wilderness cell beside the structure,
        // step in; the OUT step lands on wilderness and must stay a no-op.
        StructureTable.Structure st = StructureTable.forType(RogueTileMap.STRUCTURE_HUNTERS_BLIND);
        RunState s = runWithClearWeather(42L);
        int[] pair = wildernessNextToStructure(s.getTileMap(), st.structureType);
        assertNotNull(pair, "a structure has a wilderness entrance cell");
        s.getPlayer().placeAt(pair[0], pair[1]); // wilderness start
        s.getEnemies().clear();
        s.getCompanions().clear();
        boolean fired = false;
        TurnEngine engine = new TurnEngine();
        for (int i = 0; i < 40 && !fired; i++) {
            int dx = (i % 2 == 0 ? pair[2] : -pair[2]); // in (land on structure), then out
            int dy = (i % 2 == 0 ? pair[3] : -pair[3]);
            int hpBefore = s.getPlayer().getHp();
            engine.advance(s, PlayerAction.move(dx, dy, RoguePlayer.directionOf(dx, dy)));
            int dropped = hpBefore - s.getPlayer().getHp();
            assertTrue(dropped == 0 || dropped == st.hazard.damage(),
                    "the entry/exit step drops only the hazard's damage (got " + dropped + ")");
            if (dropped > 0) fired = true;
        }
        assertTrue(fired, "entering the structure from wilderness fires its hazard within 40 steps");
    }

    @Test
    void aZeroDisplacementMoveDoesNotTriggerTheHazard() {
        // A MOVE(0,0) is a no-op, not a step — the hazard must NOT fire on it (review fix). The
        // screen never submits one (readAction only emits cardinal moves), but the invariant is
        // pinned: start == destination must not spend a step-risk roll.
        RunState s = runWithClearWeather(42L);
        int[] cell = walkableCellAndNeighbor(s.getTileMap(), RogueTileMap.STRUCTURE_HUNTERS_BLIND);
        s.getPlayer().placeAt(cell[0], cell[1]);
        s.getEnemies().clear();
        s.getCompanions().clear();
        int hp = s.getPlayer().getHp();
        new TurnEngine().advance(s, PlayerAction.move(0, 0, RoguePlayer.EAST));
        assertEquals(hp, s.getPlayer().getHp(), "a (0,0) move does not step onto the hazard");
    }

    @Test
    void wildernessSteppingTriggersNoHazard() {
        RunState s = runWithClearWeather(42L);
        int[] cell = wildernessCellAndNeighbor(s.getTileMap());
        assertNotNull(cell, "the map has a wilderness walkable cell with a wilderness neighbor");
        s.getPlayer().placeAt(cell[0], cell[1]);
        s.getEnemies().clear();
        s.getCompanions().clear();
        int hp = s.getPlayer().getHp();
        new TurnEngine().advance(s,
                PlayerAction.move(cell[2], cell[3], RoguePlayer.directionOf(cell[2], cell[3])));
        assertEquals(hp, s.getPlayer().getHp(), "a wilderness step is a no-op (no hazard, no damage)");
    }

    @Test
    void hazardsFireAcrossSeedsAndHonorAd5() {
        // The weak floor plank (25%) over many seeds: at least one hits (hazards trigger, not
        // always-miss) and at least one misses (a chance, not a guarantee) — the distribution is
        // honored on the single seeded stream (AD-5).
        int hits = 0, misses = 0;
        for (long seed = 0; seed < SEEDS; seed++) {
            RunState s = runWithClearWeather(seed);
            int[] cell = walkableCellAndNeighbor(s.getTileMap(), RogueTileMap.STRUCTURE_HUNTERS_BLIND);
            s.getPlayer().placeAt(cell[0], cell[1]);
            s.getEnemies().clear();
            s.getCompanions().clear();
            int hp = s.getPlayer().getHp();
            new TurnEngine().advance(s,
                    PlayerAction.move(cell[2], cell[3], RoguePlayer.directionOf(cell[2], cell[3])));
            if (hp - s.getPlayer().getHp() > 0) hits++; else misses++;
        }
        assertTrue(hits > 0, "the weak floor plank fires on some seeds (" + hits + " of " + SEEDS + ")");
        assertTrue(misses > 0, "the weak floor plank is a chance, not a guarantee (" + misses + " missed)");
        // AD-5 determinism: the SAME seed must reproduce the SAME hazard outcome — a hazard drawn
        // from an unseeded/global Random would pass the hit/miss loop above but fail here.
        for (long seed : new long[]{2L, 7L, 21L}) {
            RunState a = runWithClearWeather(seed);
            RunState b = runWithClearWeather(seed);
            int[] ca = walkableCellAndNeighbor(a.getTileMap(), RogueTileMap.STRUCTURE_HUNTERS_BLIND);
            int[] cb = walkableCellAndNeighbor(b.getTileMap(), RogueTileMap.STRUCTURE_HUNTERS_BLIND);
            a.getPlayer().placeAt(ca[0], ca[1]);
            b.getPlayer().placeAt(cb[0], cb[1]);
            a.getEnemies().clear();
            b.getEnemies().clear();
            a.getCompanions().clear();
            b.getCompanions().clear();
            int ha = a.getPlayer().getHp(), hb = b.getPlayer().getHp();
            new TurnEngine().advance(a, PlayerAction.move(ca[2], ca[3], RoguePlayer.directionOf(ca[2], ca[3])));
            new TurnEngine().advance(b, PlayerAction.move(cb[2], cb[3], RoguePlayer.directionOf(cb[2], cb[3])));
            assertEquals(ha - a.getPlayer().getHp(), hb - b.getPlayer().getHp(),
                    "seed " + seed + ": same seed reproduces the same hazard outcome (AD-5)");
        }
    }

    // --- Task 5: the AD-6 seam (structure content is derived, never persisted) ---

    @Test
    void aThreeTwoRunRoundTripsWithStructureContent() {
        RunState s = new RunState(42L);
        int[] sample = firstStructureCell(s.getTileMap());
        assertNotNull(sample, "the map has a structure cell to sample");
        int sampleType = s.getTileMap().getStructureType(sample[0], sample[1]);
        List<String> layout = floorLayout(s);

        RunState loaded = json().fromJson(RunState.class, json().toJson(s));
        loaded.restoreAfterLoad();

        assertEquals(sampleType, loaded.getTileMap().getStructureType(sample[0], sample[1]),
                "structureTypes survive the round-trip (AD-6)");
        assertSame(StructureTable.forType(sampleType),
                StructureTable.forType(loaded.getTileMap().getStructureType(sample[0], sample[1])),
                "tier/loot/hazard derive from the persisted structure layer");
        assertEquals(layout, floorLayout(loaded),
                "the FULL floor layout (item types AND positions, incl. authored structure loot) "
                        + "survives the round-trip — count-only let a serializer that preserved "
                        + "count while mangling content pass (review fix)");
    }

    @Test
    void aPreThreeTwoSaveBackfillsStructureLootOnLoad() {
        // A 3.1-era save persisted the structureTypes layer but never ran the authored loot pass
        // and has no structureLootPlaced key → the flag loads false → restoreAfterLoad backfills
        // exactly once (review fix; the gap was invisible to round-trips of fresh state).
        RunState s = new RunState(42L); // fresh 3.2 state to serialize
        String json = json().toJson(s);
        String pre32 = json.replaceAll(",\\s*\"structureLootPlaced\":(true|false)", "");
        assertNotEquals(json, pre32, "a fresh 3.2 save carries the structureLootPlaced flag");

        RunState loaded = json().fromJson(RunState.class, pre32);
        loaded.restoreAfterLoad();
        StructureTable.Structure blind = StructureTable.forType(RogueTileMap.STRUCTURE_HUNTERS_BLIND);
        int[] box = footprint(loaded.getTileMap(), blind.structureType);
        assertTrue(floorItemsOfTypeInBox(loaded, Supply.ROPE.ordinal(), box) >= 1,
                "the load-time backfill restores Hunter's Blind's authored rope on a pre-3.2 save");

        // The flag flips true on backfill, so re-saving and re-loading must NOT double-place.
        RunState reloaded = json().fromJson(RunState.class, json().toJson(loaded));
        reloaded.restoreAfterLoad();
        assertEquals(loaded.getFloorItems().size(), reloaded.getFloorItems().size(),
                "re-loading a backfilled save does not double the structure loot (flag persisted)");
    }

    // --- Task 6: AC pins + the AD-16 budget with hazards active ---

    @Test
    void hazardActiveMovementStaysWithinBudget() {
        TurnEngine engine = new TurnEngine();
        long start = System.nanoTime();
        for (long seed = 0; seed < 3; seed++) {
            RunState s = new RunState(seed);
            // Wander from a structure's interior so MOVE steps land on hazard cells.
            int[] cell = walkableCellAndNeighbor(s.getTileMap(), RogueTileMap.STRUCTURE_BEEHIVE_GROVE);
            s.getPlayer().placeAt(cell[0], cell[1]);
            s.getEnemies().clear();
            int[] dirs = {RoguePlayer.EAST, RoguePlayer.NORTH, RoguePlayer.WEST, RoguePlayer.SOUTH};
            for (int t = 0; t < 200; t++) {
                int d = dirs[t % dirs.length];
                engine.advance(s, PlayerAction.move(
                        RoguePlayer.directionX(d), RoguePlayer.directionY(d), d));
            }
        }
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        assertTrue(elapsedMs < 10_000,
                "600 hazard-active acted turns resolved in " + elapsedMs + "ms (AD-16 seed)");
    }

    @Test
    void seedFortyTwoFloorLayoutIsByteIdentical() {
        // Task 3's "pinned pre-story seed's non-structure scatter is unchanged" pin, made concrete:
        // the FULL seed-42 floor layout (generic eastness scatter = the stream PREFIX, structure
        // loot = the additive suffix, AD-5) is frozen. Any disturbance to the generic pass — a new
        // scatterable type, a walkability change, an insertion into the generic draw sequence —
        // shifts this snapshot and fails here. Recorded from the verified implementation; update
        // deliberately when story content legitimately changes the layout.
        assertEquals(
                "COAL@76,32|COAL@8,29|COOKED_MEAT@74,9|FILTERED_WATER@57,10|FOLDED_CLOTH@14,17|"
                + "FOLDED_CLOTH@16,38|FOLDED_CLOTH@63,37|HALF_ROTTEN_MEAT@62,32|HERBAL_CURE@33,33|"
                + "HERBAL_CURE@74,9|HONEY@45,5|HONEY@83,24|HONEYCOMB@35,23|HONEYCOMB@45,2|"
                + "POND_WATER@58,10|PRESERVED_FOOD@13,12|PRESERVED_FOOD@17,15|PRESERVED_FOOD@42,31|"
                + "PRESERVED_FOOD@61,12|PRESERVED_FOOD@85,32|RAW_MEAT@77,7|RIVER_WATER@81,24|"
                + "ROPE@36,12|ROPE@62,37|ROPE@78,13|SALT@27,44|SALT@56,10|SALT@80,36|SALT@9,29|"
                + "SMALL_TOOLS@20,30|SMALL_TOOLS@35,44|SMALL_TOOLS@36,5|SMALL_TOOLS@74,10|"
                + "TOXIC_MUSHROOM@63,32|WELL_WATER@33,10|WOOD@31,41|WOOD@45,6|WOOD@80,36",
                String.join("|", floorLayout(new RunState(42L))),
                "seed 42's floor layout (generic + authored) is byte-identical (AD-5)");
    }

    // --- helpers ---

    /** Mirrors RunStatePersistenceTest.json() — the production serializer (AD-6). */
    private static Json json() {
        Json json = new Json();
        json.setOutputType(JsonWriter.OutputType.json);
        json.setUsePrototypes(false);
        json.setElementType(RunState.class, "enemies", RogueEnemy.class);
        json.setElementType(RunState.class, "floorItems", FloorItem.class);
        json.setElementType(RunState.class, "companions", Companion.class);
        json.setElementType(FlagStore.class, "flags", Integer.class);
        return json;
    }

    /** A fresh run with the weather pinned to CLEAR, so the one-move hazard assertions can rely on
     *  HP changing only via the hazard (no temperature harm, no clock crossing). */
    private static RunState runWithClearWeather(long seed) {
        RunState s = new RunState(seed);
        s.setWeather(Weather.CLEAR);
        return s;
    }

    private static StructureTable.Tier tier(int type) {
        return StructureTable.forType(type).tier;
    }

    private static boolean hasLoot(StructureTable.Structure st, Supply supply) {
        for (StructureTable.LootEntry entry : st.loot) {
            if (entry.supply == supply) return true;
        }
        return false;
    }

    private static int chanceOf(StructureTable.Structure st, Supply supply) {
        for (StructureTable.LootEntry entry : st.loot) {
            if (entry.supply == supply) return entry.chancePercent;
        }
        return -1;
    }

    /** The bounding box of a structure's stamped footprint, or null if the type isn't on the map. */
    private static int[] footprint(RogueTileMap m, int type) {
        int minX = m.getWidth(), minY = m.getHeight(), maxX = -1, maxY = -1;
        for (int x = 0; x < m.getWidth(); x++) {
            for (int y = 0; y < m.getHeight(); y++) {
                if (m.getStructureType(x, y) == type) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        return maxX < 0 ? null : new int[]{minX, minY, maxX, maxY};
    }

    /** Center eastness of a structure footprint (the generator's authored position). */
    private static float eastnessOf(RogueTileMap m, WorldSpine spine, StructureTable.Structure st) {
        int[] box = footprint(m, st.structureType);
        return spine.eastness((box[0] + box[2]) / 2);
    }

    private static int floorItemsOfTypeInBox(RunState s, int type, int[] box) {
        if (box == null) return 0;
        int n = 0;
        for (FloorItem it : s.getFloorItems()) {
            if (it.type == type && it.x >= box[0] && it.x <= box[2] && it.y >= box[1] && it.y <= box[3]) {
                n += it.count;
            }
        }
        return n;
    }

    /** {ax, ay, dx, dy}: a walkable structure cell with a 4-adjacent walkable cell of the SAME
     *  structure type (the destination the player steps onto to trigger the hazard), or null. */
    private static int[] walkableCellAndNeighbor(RogueTileMap m, int type) {
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int x = 0; x < m.getWidth(); x++) {
            for (int y = 0; y < m.getHeight(); y++) {
                if (m.getStructureType(x, y) != type || !m.isWalkable(x, y)) continue;
                for (int[] d : dirs) {
                    int nx = x + d[0], ny = y + d[1];
                    if (m.getStructureType(nx, ny) == type && m.isWalkable(nx, ny)) {
                        return new int[]{x, y, d[0], d[1]};
                    }
                }
            }
        }
        return null;
    }

    /** {ax, ay, dx, dy}: a non-structure walkable cell with a 4-adjacent non-structure walkable
     *  cell, or null. */
    private static int[] wildernessCellAndNeighbor(RogueTileMap m) {
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int x = 0; x < m.getWidth(); x++) {
            for (int y = 0; y < m.getHeight(); y++) {
                if (m.getStructureType(x, y) >= 0 || !m.isWalkable(x, y)) continue;
                for (int[] d : dirs) {
                    int nx = x + d[0], ny = y + d[1];
                    if (m.getStructureType(nx, ny) < 0 && m.isWalkable(nx, ny)) {
                        return new int[]{x, y, d[0], d[1]};
                    }
                }
            }
        }
        return null;
    }

    /** The first structure cell on the map (for the round-trip sample). */
    private static int[] firstStructureCell(RogueTileMap m) {
        for (int x = 0; x < m.getWidth(); x++) {
            for (int y = 0; y < m.getHeight(); y++) {
                if (m.getStructureType(x, y) >= 0) return new int[]{x, y};
            }
        }
        return null;
    }

    /** The full floor layout as a sorted list of "TYPE@x,y" — the byte-identity pin. */
    private static List<String> floorLayout(RunState s) {
        List<String> items = new ArrayList<>();
        for (FloorItem it : s.getFloorItems()) {
            items.add(Supply.byOrdinal(it.type).name() + "@" + it.x + "," + it.y);
        }
        items.sort(null);
        return items;
    }

    /** {wx, wy, dx, dy}: a walkable WILDERNESS cell with a 4-adjacent walkable cell of the given
     *  structure type (the destination the player steps onto to enter it), or null. */
    private static int[] wildernessNextToStructure(RogueTileMap m, int type) {
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int x = 0; x < m.getWidth(); x++) {
            for (int y = 0; y < m.getHeight(); y++) {
                if (m.getStructureType(x, y) >= 0 || !m.isWalkable(x, y)) continue;
                for (int[] d : dirs) {
                    int nx = x + d[0], ny = y + d[1];
                    if (m.getStructureType(nx, ny) == type && m.isWalkable(nx, ny)) {
                        return new int[]{x, y, d[0], d[1]};
                    }
                }
            }
        }
        return null;
    }
}

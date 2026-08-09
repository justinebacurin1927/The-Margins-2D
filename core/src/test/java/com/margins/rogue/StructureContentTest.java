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
            int hpBefore = s.getPlayer().getHp();
            new TurnEngine().advance(s,
                    PlayerAction.move(cell[2], cell[3], RoguePlayer.directionOf(cell[2], cell[3])));
            int dropped = hpBefore - s.getPlayer().getHp();
            assertTrue(dropped == 0 || dropped == st.hazard.damage(),
                    st.displayName + ": a step-risk drops exactly the hazard's damage (got " + dropped + ")");
        }
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
    }

    // --- Task 5: the AD-6 seam (structure content is derived, never persisted) ---

    @Test
    void aThreeTwoRunRoundTripsWithStructureContent() {
        RunState s = new RunState(42L);
        int[] sample = firstStructureCell(s.getTileMap());
        assertNotNull(sample, "the map has a structure cell to sample");
        int sampleType = s.getTileMap().getStructureType(sample[0], sample[1]);
        int itemCount = s.getFloorItems().size();

        RunState loaded = json().fromJson(RunState.class, json().toJson(s));
        loaded.restoreAfterLoad();

        assertEquals(sampleType, loaded.getTileMap().getStructureType(sample[0], sample[1]),
                "structureTypes survive the round-trip (AD-6)");
        assertSame(StructureTable.forType(sampleType),
                StructureTable.forType(loaded.getTileMap().getStructureType(sample[0], sample[1])),
                "tier/loot/hazard derive from the persisted structure layer");
        assertEquals(itemCount, loaded.getFloorItems().size(),
                "the authored structure loot survives the round-trip");
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
}

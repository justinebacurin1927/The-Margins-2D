package com.margins.rogue;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import com.margins.rogue.item.FloorItem;
import com.margins.rogue.state.RunState;
import com.margins.rogue.system.PlayerAction;
import com.margins.rogue.system.TurnEngine;
import com.margins.rogue.world.WorldSpine;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Story 3.1 (FR-9, AC-1/AC-2): the hybrid map's pins — the authored landmark skeleton is
 * stable across seeds while the procedural wilderness varies; danger and loot rise east; every
 * landmark stays reachable from the start (O4 carry); the wider map round-trips (AD-6). The
 * properties are asserted ACROSS SEEDS because the map varies per run — the honest shape for a
 * hybrid generator.
 */
class HybridMapTest {

    private static final int SEEDS = 24;

    /** Mirrors RunStatePersistenceTest.json() — the production serializer (AD-6). */
    private static Json json() {
        Json json = new Json();
        json.setOutputType(JsonWriter.OutputType.json);
        json.setUsePrototypes(false);
        json.setElementType(RunState.class, "enemies", RogueEnemy.class);
        json.setElementType(RunState.class, "floorItems", FloorItem.class);
        json.setElementType(RunState.class, "companions", Companion.class);
        json.setElementType(com.margins.rogue.state.FlagStore.class, "flags", Integer.class);
        return json;
    }

    private static String landmarkSnapshot(RogueTileMap m, WorldSpine s) {
        StringBuilder sb = new StringBuilder();
        sb.append("town=").append(m.getTile(s.corneoX(), s.corneoY()));
        sb.append(";roadW=").append(m.getTile(s.roadStartX(), s.roadY()));
        sb.append(";roadE=").append(m.getTile(s.roadEndX(), s.roadY()));
        sb.append(";border=").append(m.getTile(s.borderX(), s.borderY()));
        sb.append(";tower=").append(m.getTile(s.watchtowerX(), s.watchtowerY() + 2));
        return sb.toString();
    }

    // --- AC-1: the hybrid shape (landmarks consistent, wilderness varies) ---

    @Test
    void theAuthoredLandmarksAppearAtTheirPositionsAcrossSeeds() {
        for (long seed = 0; seed < SEEDS; seed++) {
            RunState s = new RunState(seed);
            RogueTileMap m = s.getTileMap();
            WorldSpine spine = new WorldSpine(m.getWidth(), m.getHeight());

            // The Copper Road spans the map on its row.
            for (int x = spine.roadStartX(); x <= spine.roadEndX(); x++) {
                assertTrue(m.isWalkable(x, spine.roadY()),
                        "seed " + seed + ": the road is walkable at x=" + x);
            }
            // Corneo's town plaza (the start) is on the road.
            assertTrue(m.isWalkable(spine.corneoX(), spine.corneoY()), "the town plaza is walkable");
            // The NW border crossing is a DOOR gate.
            assertEquals(RogueTile.DOOR, m.getTile(spine.borderX(), spine.borderY()),
                    "seed " + seed + ": the border crossing is a door at the NW");
            // The Watchtower is the furniture block east of Corneo on the road's shoulder.
            assertEquals(RogueTile.FURNITURE, m.getTile(spine.watchtowerX(), spine.watchtowerY() + 2),
                    "seed " + seed + ": the watchtower block is stamped east of Corneo");
            // The home-cluster structures are present (their atlas cells exist).
            assertTrue(hasStructure(m, RogueTileMap.STRUCTURE_OLD_HOUSE), "the Old House is stamped");
            assertTrue(hasStructure(m, RogueTileMap.STRUCTURE_GRAVEYARD), "the Graveyard is stamped");
        }
    }

    @Test
    void theLandmarkSkeletonIsIdenticalAcrossSeedsButTheWildernessVaries() {
        RunState a = new RunState(1L);
        RogueTileMap ma = a.getTileMap();
        WorldSpine spine = new WorldSpine(ma.getWidth(), ma.getHeight());
        String base = landmarkSnapshot(ma, spine);

        RogueTileMap other = null;
        for (long seed = 2; seed < SEEDS; seed++) {
            RogueTileMap m = new RunState(seed).getTileMap();
            // Decision 1: the skeleton is authored — it never varies with the seed.
            assertEquals(base, landmarkSnapshot(m, spine),
                    "the landmark skeleton is seed-independent (seed " + seed + ")");
            // The wilderness between the landmarks does vary.
            other = m;
        }
        boolean varied = false;
        for (int x = 0; x < ma.getWidth() && !varied; x++) {
            for (int y = 0; y < ma.getHeight(); y++) {
                if (ma.getTile(x, y) != other.getTile(x, y)) {
                    varied = true;
                    break;
                }
            }
        }
        assertTrue(varied, "the procedural wilderness differs between seeds");
    }

    // --- AC-2: the spatial spine (danger and loot rise east, safety west) ---

    @Test
    void enemyPlacementRisesEastAndTheWestHomeStaysSafe() {
        // The gradient is a property of the generator's distribution, not of any one run (a seed
        // with all its few rooms west proves nothing either way) — so total east vs west across
        // all seeds is the honest claim, while the west-home safety is per-seed (it must ALWAYS hold).
        int eastTotal = 0, westTotal = 0;
        for (long seed = 0; seed < SEEDS; seed++) {
            RunState s = new RunState(seed);
            RogueTileMap m = s.getTileMap();
            int midX = m.getWidth() / 2;
            int westQuarterX = m.getWidth() / 4;
            int westEnemies = 0;
            for (RogueEnemy e : s.getEnemies()) {
                if (e.getTileX() >= midX) eastTotal++;
                else westTotal++;
                if (e.getTileX() < westQuarterX) westEnemies++;
            }
            assertTrue(westEnemies <= 3, "seed " + seed + ": the west home stays near-empty (got "
                    + westEnemies + ")");
        }
        assertTrue(eastTotal > westTotal,
                "danger rises east (AC-2): " + eastTotal + " enemies east of mid vs " + westTotal + " west");
    }

    @Test
    void lootRisesEastWithTheDanger() {
        int eastTotal = 0, westTotal = 0;
        for (long seed = 0; seed < SEEDS; seed++) {
            RunState s = new RunState(seed);
            RogueTileMap m = s.getTileMap();
            int midX = m.getWidth() / 2;
            for (FloorItem it : s.getFloorItems()) {
                if (it.x >= midX) eastTotal += it.count;
                else westTotal += it.count;
            }
        }
        assertTrue(eastTotal > westTotal,
                "loot rises east (AC-2): " + eastTotal + " supply count east of mid vs " + westTotal + " west");
    }

    @Test
    void sameSeedReproducesEnemyAndSupplyLayout() {
        RunState a = new RunState(777L);
        RunState b = new RunState(777L);
        assertEquals(a.getEnemies().size(), b.getEnemies().size(), "same enemy count");
        assertEquals(a.getFloorItems().size(), b.getFloorItems().size(), "same supply count");
        for (int i = 0; i < a.getEnemies().size(); i++) {
            assertEquals(a.getEnemies().get(i).getTileX(), b.getEnemies().get(i).getTileX());
            assertEquals(a.getEnemies().get(i).getTileY(), b.getEnemies().get(i).getTileY());
        }
        for (int i = 0; i < a.getFloorItems().size(); i++) {
            assertEquals(a.getFloorItems().get(i).x, b.getFloorItems().get(i).x);
            assertEquals(a.getFloorItems().get(i).y, b.getFloorItems().get(i).y);
            assertEquals(a.getFloorItems().get(i).type, b.getFloorItems().get(i).type);
        }
    }

    // --- O4 carry (Task 3): connectivity is a generator guarantee ---

    @Test
    void everyLandmarkIsReachableFromTheStartAcrossSeeds() {
        for (long seed = 0; seed < SEEDS; seed++) {
            RunState s = new RunState(seed);
            RogueTileMap m = s.getTileMap();
            WorldSpine spine = new WorldSpine(m.getWidth(), m.getHeight());
            boolean[][] reached = floodFill(m, s.getPlayer().getTileX(), s.getPlayer().getTileY());

            assertTrue(reached[spine.corneoX()][spine.corneoY()], "seed " + seed + ": Corneo reached");
            assertTrue(reached[spine.roadStartX()][spine.roadY()], "seed " + seed + ": road west reached");
            assertTrue(reached[spine.roadEndX()][spine.roadY()], "seed " + seed + ": road east reached");
            assertTrue(reached[spine.borderX()][spine.borderY()], "seed " + seed + ": border reached");

            // Every door is on the network — the Old House entrance (the map's second door) is the
            // structure's way in, and the border gate is the other. A sealed structure entrance
            // would fail here.
            for (int x = 0; x < m.getWidth(); x++) {
                for (int y = 0; y < m.getHeight(); y++) {
                    if (m.getTile(x, y) == RogueTile.DOOR) {
                        assertTrue(reached[x][y], "seed " + seed + ": the door at (" + x + "," + y
                                + ") is reachable");
                    }
                }
            }
            // Each home-cluster structure opens onto the network somewhere (its entrance corridor).
            assertTrue(structureOpensOntoNetwork(m, reached, RogueTileMap.STRUCTURE_OLD_HOUSE),
                    "seed " + seed + ": the Old House interior is entered from the network");
            assertTrue(structureOpensOntoNetwork(m, reached, RogueTileMap.STRUCTURE_GRAVEYARD),
                    "seed " + seed + ": the Graveyard interior is entered from the network");
        }
    }

    @Test
    void theNonStructureWalkableRegionIsOneConnectedComponent() {
        // Scoped to the smooth-forest contract (O4/Task 3): the WILDERNESS walkable cells form one
        // component. Structure interiors are authored collision — a sealed furniture nook inside
        // the Old House cellar is design, not a carving failure (same structure carve the
        // no-divider contract applies); their ENTRANCES are covered by everyLandmarkIsReachable.
        for (long seed = 0; seed < SEEDS; seed++) {
            RunState s = new RunState(seed);
            RogueTileMap m = s.getTileMap();
            boolean[][] reached = floodFill(m, s.getPlayer().getTileX(), s.getPlayer().getTileY());
            for (int x = 0; x < m.getWidth(); x++) {
                for (int y = 0; y < m.getHeight(); y++) {
                    if (m.isWalkable(x, y) && m.getStructureTile(x, y) < 0) {
                        assertTrue(reached[x][y],
                                "seed " + seed + ": walkable wilderness tile at (" + x + "," + y
                                        + ") is a separate island");
                    }
                }
            }
        }
    }

    // --- AD-6 (Task 5): the wider map round-trips; a pre-resize save still loads ---

    @Test
    void theWiderMapRoundTripsWithActorsAndSupplies() {
        RunState s = new RunState(42L);
        int w = s.getTileMap().getWidth();
        int h = s.getTileMap().getHeight();
        assertTrue(w > h, "the map is the 2:1 horizontal shape (Decision 2)");
        int px = s.getPlayer().getTileX();
        int py = s.getPlayer().getTileY();
        int enemyCount = s.getEnemies().size();
        int itemCount = s.getFloorItems().size();

        RunState loaded = json().fromJson(RunState.class, json().toJson(s));
        loaded.restoreAfterLoad();

        assertEquals(w, loaded.getTileMap().getWidth(), "map width round-trips");
        assertEquals(h, loaded.getTileMap().getHeight(), "map height round-trips");
        assertEquals(px, loaded.getPlayer().getTileX(), "player position survives");
        assertEquals(py, loaded.getPlayer().getTileY(), "player position survives");
        assertEquals(enemyCount, loaded.getEnemies().size(), "enemies survive");
        assertEquals(itemCount, loaded.getFloorItems().size(), "scattered supplies survive");
    }

    @Test
    void aFiftyByFiftyEraSaveLoadsWithItsOwnDimensions() {
        // AD-6: the tilemap serializes inline with ITS dimensions — a save written before the
        // 3.1 resize carries a 50×50 tilemap and loads onto the new code unchanged (its dims come
        // from the save, not the current MAP_W/MAP_H). Emulate by shrinking a fresh save's
        // tilemap to the old shape.
        JsonValue root = new JsonReader().parse(json().toJson(new RunState(42L)));
        JsonValue tm = root.get("tileMap");
        tm.remove("width");
        tm.addChild("width", new JsonValue(50L));
        tm.remove("height");
        tm.addChild("height", new JsonValue(50L));
        for (String arr : new String[]{"tiles", "structureTiles", "structureTypes", "explored"}) {
            JsonValue cols = tm.get(arr);
            while (cols.size > 50) cols.remove(cols.size - 1);
            for (int x = 0; x < cols.size; x++) {
                JsonValue row = cols.get(x);
                while (row.size > 50) row.remove(row.size - 1);
            }
        }

        RunState loaded = json().fromJson(RunState.class, root.toJson(JsonWriter.OutputType.json));
        loaded.restoreAfterLoad();

        assertEquals(50, loaded.getTileMap().getWidth(), "the save's own dims win over the constants");
        assertEquals(50, loaded.getTileMap().getHeight(), "the save's own dims win over the constants");
        assertTrue(loaded.getTileMap().isWalkable(loaded.getPlayer().getTileX(),
                        loaded.getPlayer().getTileY()),
                "restoreAfterLoad re-injects the loaded (50×50) map into the player — his tile is on it");
    }

    // --- AD-16 seed (Task 6): a coarse turn-cost smoke on the wider map ---

    @Test
    void actedTurnsResolveWithinBudgetOnTheWiderMap() {
        TurnEngine engine = new TurnEngine();
        long start = System.nanoTime();
        for (long seed = 0; seed < 3; seed++) {
            RunState s = new RunState(seed);
            for (int t = 0; t < 200; t++) {
                engine.advance(s, PlayerAction.wait(0)); // a full acted turn: FOV + actors + survival
            }
        }
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        assertTrue(elapsedMs < 10_000,
                "600 acted turns on the wider map resolved in " + elapsedMs + "ms (AD-16 seed)");
    }

    // --- helpers ---

    /** True when at least one walkable cell of the structure type sits on the reached network —
     *  i.e. the structure has an open entrance the player can actually walk in through. */
    private static boolean structureOpensOntoNetwork(RogueTileMap m, boolean[][] reached, int type) {
        for (int x = 0; x < m.getWidth(); x++) {
            for (int y = 0; y < m.getHeight(); y++) {
                if (m.getStructureType(x, y) == type && m.isWalkable(x, y) && reached[x][y]) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasStructure(RogueTileMap m, int type) {
        for (int x = 0; x < m.getWidth(); x++) {
            for (int y = 0; y < m.getHeight(); y++) {
                if (m.getStructureType(x, y) == type) return true;
            }
        }
        return false;
    }

    private static boolean[][] floodFill(RogueTileMap m, int sx, int sy) {
        boolean[][] reached = new boolean[m.getWidth()][m.getHeight()];
        if (!m.isWalkable(sx, sy)) return reached;
        ArrayDeque<int[]> stack = new ArrayDeque<>();
        reached[sx][sy] = true;
        stack.push(new int[]{sx, sy});
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!stack.isEmpty()) {
            int[] c = stack.pop();
            for (int[] d : dirs) {
                int nx = c[0] + d[0], ny = c[1] + d[1];
                if (m.isWalkable(nx, ny) && !reached[nx][ny]) {
                    reached[nx][ny] = true;
                    stack.push(new int[]{nx, ny});
                }
            }
        }
        return reached;
    }
}

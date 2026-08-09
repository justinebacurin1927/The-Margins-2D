package com.margins.rogue;

import com.margins.rogue.state.RunState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The continuous-map contract after AD-8 retired floor-descent: one traversable
 * region, seed-reproducible, with no stairs tiles. Ports the seed-reproduction
 * regression from the retired RouteProgressionTest, minus floors.
 */
class ContinuousMapTest {

    @Test
    void sameSeedReproducesTheSameMapAndStart() {
        RunState a = new RunState(1234L);
        RunState b = new RunState(1234L);

        assertEquals(a.getPlayer().getTileX(), b.getPlayer().getTileX(), "same start X");
        assertEquals(a.getPlayer().getTileY(), b.getPlayer().getTileY(), "same start Y");

        RogueTileMap ma = a.getTileMap();
        RogueTileMap mb = b.getTileMap();
        assertEquals(ma.getWidth(), mb.getWidth());
        assertEquals(ma.getHeight(), mb.getHeight());
        for (int x = 0; x < ma.getWidth(); x++) {
            for (int y = 0; y < ma.getHeight(); y++) {
                assertEquals(ma.getTile(x, y), mb.getTile(x, y),
                        "same seed reproduces the same tile at (" + x + "," + y + ")");
                assertEquals(ma.getStructureTile(x, y), mb.getStructureTile(x, y),
                        "same seed reproduces the structure cell at (" + x + "," + y + ")");
                assertEquals(ma.getStructureType(x, y), mb.getStructureType(x, y),
                        "same seed reproduces the structure atlas at (" + x + "," + y + ")");
            }
        }
    }

    @Test
    void mapHasNoStairsTiles() {
        // AD-8 retired floor descent — no stairs/descent tiles. Water-source features (Well/Pond/
        // River, Story 1.5) are the only other valid tiles; every tile must be one of these known
        // types (a stray stairs tile would be an int outside this set).
        RogueTileMap m = new RunState(99L).getTileMap();
        for (int x = 0; x < m.getWidth(); x++) {
            for (int y = 0; y < m.getHeight(); y++) {
                int t = m.getTile(x, y);
                assertTrue(t == RogueTile.WALL || t == RogueTile.FLOOR || t == RogueTile.DOOR
                                || t == RogueTile.FURNITURE || RogueTile.isWaterSource(t),
                        "only known continuous-map tiles remain after AD-8; found " + t
                                + " at (" + x + "," + y + ")");
            }
        }
    }

    @Test
    void generatedForestHasNoOneTileOppositeSideDividers() {
        for (long seed = 0; seed < 12; seed++) {
            RogueTileMap map = new RunState(seed).getTileMap();
            for (int x = 1; x < map.getWidth() - 1; x++) {
                for (int y = 1; y < map.getHeight() - 1; y++) {
                    if (map.getTile(x, y) != RogueTile.WALL || map.getStructureTile(x, y) >= 0) continue;
                    boolean pinchedNorthSouth = map.isWalkable(x, y + 1)
                            && map.isWalkable(x, y - 1);
                    boolean pinchedEastWest = map.isWalkable(x + 1, y)
                            && map.isWalkable(x - 1, y);
                    assertFalse(pinchedNorthSouth || pinchedEastWest,
                            "seed " + seed + " left a one-cell forest divider at ("
                                    + x + "," + y + ")");
                }
            }
        }
    }

    @Test
    void generatedMapHasAtMostTwoDoors() {
        for (long seed = 0; seed < 32; seed++) {
            RogueTileMap map = new RunState(seed).getTileMap();
            int doors = 0;
            for (int x = 0; x < map.getWidth(); x++) {
                for (int y = 0; y < map.getHeight(); y++) {
                    if (map.getTile(x, y) == RogueTile.DOOR) doors++;
                }
            }
            assertTrue(doors <= 2, "seed " + seed + " generated " + doors + " doors");
        }
    }

    @Test
    void oldHouseOccupiesAFullFifteenByTenLayeredFootprint() {
        RogueTileMap map = new RunState(1234L).getTileMap();
        int cells = 0;
        int minX = map.getWidth(), minY = map.getHeight(), maxX = -1, maxY = -1;
        int structureDoors = 0;
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                if (map.getStructureType(x, y) != RogueTileMap.STRUCTURE_OLD_HOUSE) continue;
                cells++;
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
                if (map.getTile(x, y) == RogueTile.DOOR) structureDoors++;
            }
        }
        assertEquals(150, cells,
                "the Old House is 150 fog-aware map cells including its foundation apron");
        assertEquals(15, maxX - minX + 1);
        assertEquals(10, maxY - minY + 1);
        assertEquals(1, structureDoors, "the multi-room structure has one meaningful entrance");
        assertTrue(map.isWalkable(minX, minY), "the exterior foundation apron is walkable");
        assertFalse(map.isWalkable(minX + 1, minY + 1), "the inset perimeter blocks movement");
        assertEquals(RogueTile.DOOR, map.getTile(minX + 8, minY + 1),
                "the south entrance aligns inside the foundation apron");
        assertTrue(map.isWalkable(minX + 3, minY + 3), "the room interior is explorable");
        assertTrue(map.isWalkable(minX + 4, minY + 4), "the kitchen doorway is open");
        assertTrue(map.isWalkable(minX + 5, minY + 4),
                "the kitchen doorway opens onto clear main-room floor");
        assertTrue(map.isWalkable(minX + 10, minY + 4), "the cellar doorway is open");
        assertTrue(map.isWalkable(minX + 9, minY + 4),
                "the cellar doorway is not choked by the relocated table");
        assertFalse(map.isWalkable(minX + 7, minY + 4), "the broken table blocks movement");
        assertFalse(map.isOpaque(minX + 7, minY + 4), "furniture does not block line of sight");
        assertFalse(map.isWalkable(minX + 11, minY + 7), "the chest blocks movement");
    }

    @Test
    void graveyardIsAnExplorableElevenByNineOutdoorStructure() {
        RogueTileMap map = new RunState(1234L).getTileMap();
        int cells = 0;
        int minX = map.getWidth(), minY = map.getHeight(), maxX = -1, maxY = -1;
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                if (map.getStructureType(x, y) != RogueTileMap.STRUCTURE_GRAVEYARD) continue;
                cells++;
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
                assertFalse(map.isOpaque(x, y), "graveyard fence and markers never block sight");
            }
        }
        assertEquals(99, cells, "the graveyard is 99 fog-aware cells, not a landmark icon");
        assertEquals(11, maxX - minX + 1);
        assertEquals(9, maxY - minY + 1);
        assertTrue(map.isWalkable(minX, minY), "the transition apron is walkable");
        assertFalse(map.isWalkable(minX + 1, minY + 1), "the low south fence blocks feet");
        assertTrue(map.isWalkable(minX + 5, minY + 1), "the south gate is open");
        assertTrue(map.isWalkable(minX + 5, minY + 4), "the central dirt path stays clear");
        assertFalse(map.isWalkable(minX + 3, minY + 3), "a grave cluster blocks movement");
        assertFalse(map.isWalkable(minX + 7, minY + 5), "the opposite marker blocks movement");
    }

    @Test
    void deepCaveIsAFullElevenByNineRockLandmark() {
        RogueTileMap map = new RunState(1234L).getTileMap();
        int cells = 0;
        int minX = map.getWidth(), minY = map.getHeight(), maxX = -1, maxY = -1;
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                if (map.getStructureType(x, y) != RogueTileMap.STRUCTURE_DEEP_CAVE) continue;
                cells++;
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
            }
        }
        assertEquals(99, cells, "the cave mouth is a fog-aware landmark, not a one-tile icon");
        assertEquals(11, maxX - minX + 1);
        assertEquals(9, maxY - minY + 1);
        assertTrue(map.isWalkable(minX + 5, minY), "the south dirt approach is open");
        assertTrue(map.isWalkable(minX + 5, minY + 4), "the path reaches the cave threshold");
        assertFalse(map.isWalkable(minX + 5, maxY), "the black opening is solid for now");
        assertTrue(map.isOpaque(minX + 5, maxY), "the cave back blocks sight");
        assertFalse(map.isWalkable(minX + 2, minY + 3), "the western boulder bowl blocks feet");
        assertFalse(map.isWalkable(minX + 8, minY + 3), "the eastern boulder bowl blocks feet");
    }

    @Test
    void huntersBlindIsAnExplorableNineByNineRaisedDeck() {
        RogueTileMap map = new RunState(1234L).getTileMap();
        int cells = 0;
        int minX = map.getWidth(), minY = map.getHeight(), maxX = -1, maxY = -1;
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                if (map.getStructureType(x, y) != RogueTileMap.STRUCTURE_HUNTERS_BLIND) continue;
                cells++;
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
                assertFalse(map.isOpaque(x, y), "the timber blind never blocks line of sight");
            }
        }
        assertEquals(81, cells, "the blind is a full fog-aware structure, not a prop icon");
        assertEquals(9, maxX - minX + 1);
        assertEquals(9, maxY - minY + 1);
        assertTrue(map.isWalkable(minX + 4, minY), "the south approach is clear");
        assertTrue(map.isWalkable(minX + 4, minY + 2), "the ladder route is open");
        assertTrue(map.isWalkable(minX + 4, minY + 5), "the central deck is explorable");
        assertFalse(map.isWalkable(minX + 2, minY + 2), "under-deck braces block feet");
        assertFalse(map.isWalkable(minX + 7, minY + 5), "the east platform post blocks feet");
    }

    @Test
    void tierOneAndTierTwoStructuresUseTheirAuthoredFootprintsAndOpenSouthEntrances() {
        RogueTileMap map = new RunState(1234L).getTileMap();
        int[] types = {
                RogueTileMap.STRUCTURE_FALLEN_LOG_HOLLOW,
                RogueTileMap.STRUCTURE_FOREST_SHRINE,
                RogueTileMap.STRUCTURE_BEEHIVE_GROVE,
                RogueTileMap.STRUCTURE_KITCHEN_CAMP,
                RogueTileMap.STRUCTURE_COLLAPSED_WATCHTOWER,
                RogueTileMap.STRUCTURE_POACHERS_CAMP,
                RogueTileMap.STRUCTURE_SUNKEN_WELL
        };
        int[] widths = {9, 9, 11, 9, 11, 13, 11};
        int[] heights = {5, 9, 11, 9, 13, 11, 11};
        int[] pathXs = {4, 4, 5, 4, 5, 6, 5};
        for (int i = 0; i < types.length; i++) {
            int type = types[i];
            int cells = 0;
            int minX = map.getWidth(), minY = map.getHeight(), maxX = -1, maxY = -1;
            int obstacles = 0;
            for (int x = 0; x < map.getWidth(); x++) {
                for (int y = 0; y < map.getHeight(); y++) {
                    if (map.getStructureType(x, y) != type) continue;
                    cells++;
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                    if (!map.isWalkable(x, y)) obstacles++;
                }
            }
            assertEquals(widths[i] * heights[i], cells,
                    "structure type " + type + " must fill its complete authored atlas");
            assertEquals(widths[i], maxX - minX + 1);
            assertEquals(heights[i], maxY - minY + 1);
            assertTrue(map.isWalkable(minX + pathXs[i], minY),
                    "structure type " + type + " has a clear south approach");
            assertTrue(map.isWalkable(minX + pathXs[i], minY + 1),
                    "structure type " + type + " opens onto its interior");
            assertTrue(obstacles > 4, "structure type " + type + " has real prop collision");
        }
    }

    @Test
    void redesignedStructuresKeepTheirPromisedInteriorSpace() {
        RogueTileMap map = new RunState(1234L).getTileMap();

        int[] hive = bounds(map, RogueTileMap.STRUCTURE_BEEHIVE_GROVE);
        for (int x = hive[0] + 3; x <= hive[0] + 7; x++) {
            for (int y = hive[1] + 3; y <= hive[1] + 7; y++) {
                assertTrue(map.isWalkable(x, y), "the hive grove's central 5x5 must remain open");
            }
        }

        int[] log = bounds(map, RogueTileMap.STRUCTURE_FALLEN_LOG_HOLLOW);
        assertEquals(9, log[2] - log[0] + 1);
        assertEquals(5, log[3] - log[1] + 1);
        assertTrue(map.isWalkable(log[0] + 4, log[1] + 2), "the small log hollow is enterable");

        int[] tower = bounds(map, RogueTileMap.STRUCTURE_COLLAPSED_WATCHTOWER);
        assertTrue(map.isWalkable(tower[0] + 5, tower[1] + 3), "tower ground floor is open");
        assertTrue(map.isWalkable(tower[0] + 4, tower[1] + 7), "tower middle floor is open");
        assertTrue(map.isWalkable(tower[0] + 5, tower[1] + 11), "tower third floor is open");

        int[] camp = bounds(map, RogueTileMap.STRUCTURE_POACHERS_CAMP);
        for (int x = camp[0] + 4; x <= camp[0] + 6; x++) {
            for (int y = camp[1] + 2; y <= camp[1] + 5; y++) {
                assertTrue(map.isWalkable(x, y), "the expanded poacher yard stays spacious");
            }
        }

        int[] well = bounds(map, RogueTileMap.STRUCTURE_SUNKEN_WELL);
        assertTrue(map.isWalkable(well[0] + 2, well[1] + 5), "west wellhouse room is enterable");
        assertTrue(map.isWalkable(well[0] + 8, well[1] + 5), "east wellhouse room is enterable");
        assertFalse(map.isWalkable(well[0] + 5, well[1] + 7), "the open well shaft blocks feet");
    }

    private static int[] bounds(RogueTileMap map, int type) {
        int minX = map.getWidth(), minY = map.getHeight(), maxX = -1, maxY = -1;
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                if (map.getStructureType(x, y) != type) continue;
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
            }
        }
        return new int[]{minX, minY, maxX, maxY};
    }
}

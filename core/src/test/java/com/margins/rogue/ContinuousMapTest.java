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
}

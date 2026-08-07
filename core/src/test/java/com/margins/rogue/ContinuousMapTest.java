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
                                || RogueTile.isWaterSource(t),
                        "only WALL/FLOOR/DOOR + water sources remain after AD-8; found " + t + " at (" + x + "," + y + ")");
            }
        }
    }
}

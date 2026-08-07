package com.margins.rogue;

import com.margins.rogue.item.Supply;
import com.margins.rogue.state.RunState;
import com.margins.rogue.system.PlayerAction;
import com.margins.rogue.system.TurnEngine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Water-source tiles (FR-6, Story 1.5 AC-1): the floor gains Well / Pond / River features,
 * placed deterministically per seed (AD-5), and collecting from one yields its matching raw
 * water type.
 */
class WaterSourceTest {

    /** A serialised map of just the water-source tiles, for equality comparison. */
    private static String sourceMap(RogueTileMap m) {
        StringBuilder sb = new StringBuilder();
        for (int x = 0; x < m.getWidth(); x++)
            for (int y = 0; y < m.getHeight(); y++)
                if (RogueTile.isWaterSource(m.getTile(x, y)))
                    sb.append(x).append(',').append(y).append('=').append(m.getTile(x, y)).append(';');
        return sb.toString();
    }

    @Test
    void sourcesArePlacedAndDeterministicPerSeed() {
        RunState a = new RunState(7L);
        RunState b = new RunState(7L);
        String sa = sourceMap(a.getTileMap());
        assertFalse(sa.isEmpty(), "the floor has at least one water source");
        assertEquals(sa, sourceMap(b.getTileMap()), "the same seed reproduces the source layout (AD-5)");
    }

    @Test
    void collectingAtASourceYieldsItsWaterType() {
        RunState s = new RunState(7L);
        RogueTileMap m = s.getTileMap();
        int[] tile = firstSource(m);
        assertNotNull(tile, "seed 7 has a water source to collect from");

        s.getPlayer().placeAt(tile[0], tile[1]); // stand on the source
        Supply expected = waterFor(m.getTile(tile[0], tile[1]));

        new TurnEngine().advance(s, PlayerAction.collect(0));

        assertEquals(1, s.getInventory().count(expected.ordinal()),
                "collecting yields the matching raw water (" + expected + ")");
    }

    private static int[] firstSource(RogueTileMap m) {
        for (int x = 0; x < m.getWidth(); x++)
            for (int y = 0; y < m.getHeight(); y++)
                if (RogueTile.isWaterSource(m.getTile(x, y))) return new int[]{x, y};
        return null;
    }

    private static Supply waterFor(int tile) {
        switch (tile) {
            case RogueTile.WELL:  return Supply.WELL_WATER;
            case RogueTile.POND:  return Supply.POND_WATER;
            case RogueTile.RIVER: return Supply.RIVER_WATER;
            default:              return null;
        }
    }
}

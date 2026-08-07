package com.margins.rogue;

import com.margins.rogue.state.RunState;
import com.margins.rogue.system.LightSystem;
import com.margins.rogue.system.PlayerAction;
import com.margins.rogue.system.TurnEngine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Light → noise via the single AD-9 channel (Story 1.4 AC-3, AD-18): a lit source emits one
 * per-turn NoiseEvent at its tile on the acted path, LOS-ignoring, luring in-radius enemies.
 * The lure itself (Euclidean, no line-of-sight) already lives in NoiseSystem — this pins that
 * 1.4 feeds it correctly and only on a real action (AD-5).
 */
class LightNoiseTest {

    @Test
    void litSourceEnqueuesOneNoiseAtItsTile() {
        RunState s = new RunState(1L);
        s.setLight(12, 7);

        LightSystem.emitNoise(s);

        assertEquals(1, s.getNoiseQueue().size(), "a lit source emits exactly one noise");
        NoiseEvent n = s.getNoiseQueue().get(0);
        assertEquals(12, n.x, "noise is at the light's tile");
        assertEquals(7, n.y, "noise is at the light's tile");
        assertEquals(LightSystem.LIGHT_NOISE_RADIUS, n.radius, "at the light's noise radius");
    }

    @Test
    void unlitEmitsNothing() {
        RunState s = new RunState(1L);
        LightSystem.emitNoise(s);
        assertTrue(s.getNoiseQueue().isEmpty(), "no light, no noise");
    }

    @Test
    void halfSetLightIsNotLitAndEmitsNothing() {
        // The "-1 = none" sentinel is on the pair: one coordinate still -1 means unlit, so a
        // half-set light never emits a NoiseEvent at an off-map row (guards hasLight()).
        RunState s = new RunState(1L);
        s.setLight(5, -1);
        assertFalse(s.hasLight(), "a light missing its y coordinate is not lit");
        LightSystem.emitNoise(s);
        assertTrue(s.getNoiseQueue().isEmpty(), "a half-set light emits no noise");
    }

    @Test
    void litSourceLuresAnEnemyBehindWallOnAnActedTurn() {
        RunState s = new RunState(1L);
        RogueTileMap map = s.getTileMap();
        // Player at a known interior tile; the light well outside the player's vision range so
        // DetectionSystem never touches the enemy — isolating the light's noise lure. The enemy
        // sits at the light tile (in the noise radius, out of the player's sight — no LOS needed).
        s.getPlayer().placeAt(25, 25);
        int lx = 25, ly = 35; // distance 10 from the player (> enemy vision range 6)
        map.setTile(lx, ly, RogueTile.FLOOR);
        RogueEnemy e = new RogueEnemy(lx, ly, map);
        s.getEnemies().add(e);
        s.setLight(lx, ly);

        new TurnEngine().advance(s, PlayerAction.wait(0));

        assertEquals(Detection.SUSPICIOUS, e.getDetection(), "the fire lures an unaware enemy (AC-3)");
        assertEquals(lx, e.getLastSeenX(), "the enemy investigates the fire's tile");
        assertEquals(ly, e.getLastSeenY(), "the enemy investigates the fire's tile");
    }

    @Test
    void noLightNoLureOnAnActedTurn() {
        RunState s = new RunState(1L);
        RogueTileMap map = s.getTileMap();
        s.getPlayer().placeAt(25, 25);
        map.setTile(25, 35, RogueTile.FLOOR);
        RogueEnemy e = new RogueEnemy(25, 35, map);
        s.getEnemies().add(e);
        // no light set

        new TurnEngine().advance(s, PlayerAction.wait(0));

        assertEquals(Detection.UNAWARE, e.getDetection(), "with no fire, a far enemy stays unaware");
    }

    @Test
    void wallBumpEmitsNoLightNoise() {
        RunState s = new RunState(1L);
        RogueTileMap map = s.getTileMap();
        s.getPlayer().placeAt(25, 25);
        map.setTile(25, 35, RogueTile.FLOOR);
        RogueEnemy e = new RogueEnemy(25, 35, map);
        s.getEnemies().add(e);
        s.setLight(25, 35);
        // Aim the player at a wall so the move commits no turn (AD-5).
        int wx = 25, wy = 24;
        map.setTile(wx, wy, RogueTile.WALL);

        new TurnEngine().advance(s, PlayerAction.move(0, -1, 0)); // into the wall — no turn commits

        assertEquals(Detection.UNAWARE, e.getDetection(),
                "a wasted keypress emits no light noise (the emit is on the acted path only, AD-5)");
    }
}

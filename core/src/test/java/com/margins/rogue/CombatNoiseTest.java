package com.margins.rogue;

import com.margins.rogue.state.RunState;
import com.margins.rogue.system.CombatSystem;
import com.margins.rogue.system.NoiseSystem;
import com.margins.rogue.system.PlayerAction;
import com.margins.rogue.system.TurnEngine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Story 4.2 (AC-1): a Block is as loud as an attack — it emits one NoiseEvent at Klein's tile
 * (radius 4, D2), fed to the existing AD-9 NoiseSystem.resolve draw, so bracing draws
 * reinforcements exactly like a swing. The draw itself lives in NoiseSystem (pinned by
 * LightNoiseTest); this pins the new block producer and its TurnEngine wiring.
 */
class CombatNoiseTest {

    @Test
    void blockEmitsOneNoiseAtKleinsTile() {
        RunState s = new RunState(1L);
        s.getPlayer().placeAt(25, 25);

        CombatSystem.blockNoise(s);

        assertEquals(1, s.getNoiseQueue().size(), "a brace emits exactly one noise");
        NoiseEvent n = s.getNoiseQueue().get(0);
        assertEquals(25, n.x, "noise is at Klein's tile");
        assertEquals(25, n.y, "noise is at Klein's tile");
        assertEquals(4, n.radius, "block noise is as loud as an attack (radius 4, D2)");
    }

    @Test
    void blockNoiseDrawsAnUnawareEnemyToSuspicious() {
        RunState s = new RunState(1L);
        RogueTileMap map = s.getTileMap();
        s.getPlayer().placeAt(25, 25);
        int ex = 25, ey = 28; // distance 3 — within block radius 4
        map.setTile(ex, ey, RogueTile.FLOOR);
        RogueEnemy e = new RogueEnemy(ex, ey, map);
        s.getEnemies().add(e);
        assertEquals(Detection.UNAWARE, e.getDetection());

        CombatSystem.blockNoise(s);
        NoiseSystem.resolve(s);

        assertEquals(Detection.SUSPICIOUS, e.getDetection(), "the brace lures an unaware enemy in radius (AC-1)");
        assertEquals(25, e.getLastSeenX(), "the enemy investigates Klein's tile");
        assertEquals(25, e.getLastSeenY(), "the enemy investigates Klein's tile");
    }

    @Test
    void blockOnTheActedPathLuresReinforcementsViaNoise() {
        // End-to-end: a Block PlayerAction drives TurnEngine's BLOCK case → blockNoise → the Noise
        // resolve step. The enemy is in block-noise radius but has NO line of sight (a wall between),
        // so DetectionSystem's sight path can't be the cause — the LOS-ignoring noise is.
        RunState s = new RunState(1L);
        RogueTileMap map = s.getTileMap();
        s.getPlayer().placeAt(25, 25);
        map.setTile(25, 25, RogueTile.FLOOR);
        map.setTile(25, 27, RogueTile.WALL);  // blocks LOS between player and enemy
        int ex = 25, ey = 28; // distance 3 ≤ 4; LOS blocked by the wall at (25,27)
        map.setTile(ex, ey, RogueTile.FLOOR);
        RogueEnemy e = new RogueEnemy(ex, ey, map);
        s.getEnemies().add(e);

        new TurnEngine().advance(s, PlayerAction.block(0));

        assertEquals(Detection.SUSPICIOUS, e.getDetection(),
                "a brace on the acted path draws an in-radius enemy via noise (AC-1)");
        assertTrue(s.getNoiseQueue().isEmpty(), "the Noise resolve step drained the queue");
    }
}

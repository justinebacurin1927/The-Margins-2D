package com.margins.rogue.narrative;

import com.margins.dialog.DialogEffect;
import com.margins.dialog.DialogNode;
import com.margins.dialog.DialogNode.DialogOption;
import com.margins.dialog.GateStat;
import com.margins.rogue.Detection;
import com.margins.rogue.RogueEnemy;
import com.margins.rogue.RogueTile;
import com.margins.rogue.RogueTileMap;
import com.margins.rogue.state.RunState;
import com.margins.rogue.system.DetectionSystem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Story 4.2 (AC-2, D3): VOICE can talk a wary patrol down. The de-escalation mechanism lives in
 * DetectionSystem (SUSPICIOUS→UNAWARE in radius, ALERTED untouched); a parley scene rides it on a
 * passed VOICE check via the new {@link DialogEffect.Deescalate} effect. Klein's starting VOICE is
 * 3 and the parley gate is 3 (passes at rest); the failure branch is exercised with a higher
 * test-authored threshold since VOICE has no setter yet.
 */
class ParleyDeescalationTest {

    private RunState sceneWithEnemyAt(int ex, int ey, Detection det) {
        RunState s = new RunState(1L);
        RogueTileMap map = s.getTileMap();
        s.getPlayer().placeAt(25, 25);
        s.getEnemies().clear(); // drop the seed-spawned patrol so get(0) is our controlled fixture enemy
        map.setTile(ex, ey, RogueTile.FLOOR);
        RogueEnemy e = new RogueEnemy(ex, ey, map);
        e.setDetection(det);
        s.getEnemies().add(e);
        return s;
    }

    @Test
    void deescalateNearDropsSuspiciousToUnaware() {
        RunState s = sceneWithEnemyAt(25, 27, Detection.SUSPICIOUS); // distance 2 ≤ 4
        int n = DetectionSystem.deescalateNear(s, 25, 25, 4);
        assertEquals(1, n, "one wary enemy talked down");
        assertEquals(Detection.UNAWARE, s.getEnemies().get(0).getDetection());
    }

    @Test
    void deescalateNearLeavesAlertedUntouched() {
        RunState s = sceneWithEnemyAt(25, 27, Detection.ALERTED);
        int n = DetectionSystem.deescalateNear(s, 25, 25, 4);
        assertEquals(0, n, "an alerted enemy is past talking (D3)");
        assertEquals(Detection.ALERTED, s.getEnemies().get(0).getDetection());
    }

    @Test
    void deescalateNearIgnoresOutOfRadius() {
        RunState s = sceneWithEnemyAt(25, 35, Detection.SUSPICIOUS); // distance 10 > 4
        int n = DetectionSystem.deescalateNear(s, 25, 25, 4);
        assertEquals(0, n);
        assertEquals(Detection.SUSPICIOUS, s.getEnemies().get(0).getDetection());
    }

    @Test
    void hasSuspiciousAdjacentGatesOnAdjacencyAndWariness() {
        assertTrue(DetectionSystem.hasSuspiciousAdjacent(sceneWithEnemyAt(25, 26, Detection.SUSPICIOUS)),
                "an adjacent wary patrol offers a parley");
        assertFalse(DetectionSystem.hasSuspiciousAdjacent(sceneWithEnemyAt(25, 27, Detection.SUSPICIOUS)),
                "not adjacent → no parley");
        assertFalse(DetectionSystem.hasSuspiciousAdjacent(sceneWithEnemyAt(25, 26, Detection.ALERTED)),
                "an alerted enemy is past talking — no parley");
    }

    @Test
    void parleySuccessDeescalatesTheWaryPatrol() {
        RunState s = sceneWithEnemyAt(25, 26, Detection.SUSPICIOUS);
        // Klein's starting VOICE (3) == ParleyScene.VOICE_THRESHOLD → the check passes.
        DialogController c = new DialogController();
        c.start(ParleyScene.build(), s);
        c.select(0, s); // "Talk them down. (VOICE)"

        assertEquals(Detection.UNAWARE, s.getEnemies().get(0).getDetection(), "the patrol stands down (AC-2)");
        assertTrue(s.getMessageLog().contains("The patrol stands down."),
                "the talk-down is observed in the log");
    }

    @Test
    void aFailedVoiceCheckDoesNotDeescalate() {
        RunState s = sceneWithEnemyAt(25, 26, Detection.SUSPICIOUS);
        // Klein's VOICE is 3; gate this option at 4 so the check fails → the failure branch carries
        // no Deescalate effect. Proves the talk-down rides ONLY on a passed VOICE check.
        DialogNode success = new DialogNode("ok").withEffect(new DialogEffect.Deescalate(4));
        DialogNode fail = new DialogNode("no");
        DialogNode root = new DialogNode("gate",
                new DialogOption("talk", GateStat.VOICE, 4, success, fail));
        DialogController c = new DialogController();
        c.start(root, s);
        c.select(0, s);

        assertSame(fail, c.getCurrent(), "VOICE 3 < 4 routes to the failure branch");
        assertEquals(Detection.SUSPICIOUS, s.getEnemies().get(0).getDetection(),
                "a failed check does not talk them down");
    }
}

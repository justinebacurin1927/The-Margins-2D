package com.margins.rogue;

import com.margins.rogue.state.RunState;
import com.margins.rogue.system.PlayerAction;
import com.margins.rogue.system.TurnEngine;
import com.margins.rogue.system.TurnResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Story 5.3 (FR-16): simple orders steer the companion. The ORDER action cycles the standing order
 * (autonomous → HOLD → HIDE → autonomous) the 5.2 behavior machine honors, and the previously
 * unreachable distract shout is now bound. Driven through the real turn pipeline (AD-1: the order is
 * a PlayerAction; CompanionSystem owns the transition).
 */
class CompanionOrdersTest {

    private RunState clearState() {
        RunState s = new RunState(42L);
        RogueTileMap m = s.getTileMap();
        s.getPlayer().placeAt(25, 25);
        for (int x = 18; x <= 34; x++)
            for (int y = 18; y <= 34; y++)
                m.setTile(x, y, RogueTile.FLOOR);
        s.getEnemies().clear();
        return s;
    }

    @Test
    void orderCyclesHoldThenHideThenResume() {
        RunState s = clearState(); // default active companion Aldric, behavior FOLLOW
        TurnEngine engine = new TurnEngine();

        TurnResult r1 = engine.advance(s, PlayerAction.order(0));
        assertEquals(CompanionBehavior.HOLD, s.getActiveCompanion().getBehavior(), "first command → hold");
        assertTrue(r1.messages.contains("Aldric holds position."), r1.messages.toString());

        engine.advance(s, PlayerAction.order(0));
        assertEquals(CompanionBehavior.HIDE, s.getActiveCompanion().getBehavior(), "second command → hide");

        engine.advance(s, PlayerAction.order(0));
        assertEquals(CompanionBehavior.FOLLOW, s.getActiveCompanion().getBehavior(), "third command → resume autonomous");
    }

    @Test
    void orderCommitsATurn() {
        RunState s = clearState();
        int clock = s.getClockTurns();
        new TurnEngine().advance(s, PlayerAction.order(0));
        assertTrue(s.getClockTurns() > clock, "a command is a real action — it spends the turn (AD-5)");
    }

    @Test
    void aHeldCompanionStaysPutAndDoesNotFightAThreat() {
        RunState s = clearState();
        Companion c = s.getActiveCompanion();
        c.placeAt(27, 25);
        new TurnEngine().advance(s, PlayerAction.order(0)); // → HOLD
        assertEquals(CompanionBehavior.HOLD, c.getBehavior());

        // now a threat appears; a held companion honors the order (5.2) — no move, no strike
        RogueEnemy e = new RogueEnemy(28, 25, s.getTileMap());
        e.setDetection(Detection.ALERTED);
        s.getEnemies().add(e);
        int ex = c.getTileX(), ey = c.getTileY(), eHp = e.getHp();

        new TurnEngine().advance(s, PlayerAction.wait(0));

        assertEquals(CompanionBehavior.HOLD, c.getBehavior(), "the standing order persists under threat");
        assertEquals(ex, c.getTileX(), "a held companion does not move");
        assertEquals(ey, c.getTileY());
        assertEquals(eHp, e.getHp(), "and does not fight while held");
    }

    @Test
    void orderingCalmsAPriorPanic() {
        RunState s = clearState();
        s.activateCompanion(CompanionId.MARA);
        Companion m = s.getActiveCompanion();
        m.addCondition(Companion.Condition.PANICKED);

        new TurnEngine().advance(s, PlayerAction.order(0)); // → HOLD, steadies her

        assertEquals(CompanionBehavior.HOLD, m.getBehavior());
        assertFalse(m.hasCondition(Companion.Condition.PANICKED), "a command steadies the companion");
    }

    @Test
    void orderIsRefusedWithoutACompanionAndSpendsNoTurn() {
        RunState s = clearState();
        s.removeActiveCompanion();
        int clock = s.getClockTurns();

        TurnResult r = new TurnEngine().advance(s, PlayerAction.order(0));

        assertTrue(r.messages.contains("No companion to command."), "an honest refusal: " + r.messages);
        assertEquals(clock, s.getClockTurns(), "a refused order spends no turn (inert-USE precedent)");
    }

    @Test
    void distractIsReachableAndPullsPatrols() {
        // Story 5.3 binds the previously unreachable shout; the action still emits its pull-patrol
        // noise, resolved through the shared NoiseSystem (an UNAWARE bystander rises to SUSPICIOUS).
        // The companion shouts far from the player so the bystander can't see Klein (isolating the
        // noise channel from DetectionSystem's LOS path); the noise is queued at the shout's tile.
        RunState s = clearState();
        Companion c = s.getActiveCompanion();
        c.placeAt(25, 32); // 7 tiles from the player — beyond enemy vision (6)
        RogueEnemy bystander = new RogueEnemy(26, 32, s.getTileMap()); // next to the shout, far from Klein
        s.getEnemies().add(bystander);
        assertEquals(Detection.UNAWARE, bystander.getDetection());

        new TurnEngine().advance(s, PlayerAction.distract(0));

        assertEquals(Detection.SUSPICIOUS, bystander.getDetection(),
                "the shout drew the patrol (distraction reachable + working)");
    }
}

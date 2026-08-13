package com.margins.rogue;

import com.margins.rogue.state.RunState;
import com.margins.rogue.system.CompanionSystem;
import com.margins.rogue.system.NoiseSystem;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Story 5.2 (FR-16, AD-10, AD-9): the active companion runs a behavior state machine. The
 * combatant gate (FR-15) splits it — a combatant fights (FIGHT_RETREAT) / follows, a non-combatant
 * flees (FLEE) / takes cover and never fights. A fleeing companion emits stealth-blowing noise; a
 * hidden one is quiet. Driven directly through CompanionSystem.act for a deterministic AI turn
 * (the AD-4 slot Aldric's own pins in CompanionAiTest already cover end-to-end).
 */
class CompanionBehaviorTest {

    /** A cleared floor with the player centred and no ambient enemies — full control of the AI turn. */
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

    private RogueEnemy alerted(RunState s, int x, int y) {
        RogueEnemy e = new RogueEnemy(x, y, s.getTileMap());
        e.setDetection(Detection.ALERTED);
        s.getEnemies().add(e);
        return e;
    }

    private Companion mara(RunState s, int x, int y) {
        s.activateCompanion(CompanionId.MARA);
        Companion c = s.getActiveCompanion();
        c.placeAt(x, y);
        return c;
    }

    // --- AC-1 / D2: the combatant gate ---

    @Test
    void aCombatantFightRetreatsWhenAThreatIsNear() {
        RunState s = clearState(); // default active companion is Aldric (combatant)
        alerted(s, 28, 25); // within REACTION_RADIUS of the player
        CompanionSystem.act(s, new ArrayList<>());
        assertEquals(CompanionBehavior.FIGHT_RETREAT, s.getActiveCompanion().getBehavior());
    }

    @Test
    void aNonCombatantFleesAndNeverStrikesTheThreat() {
        RunState s = clearState();
        Companion m = mara(s, 27, 25);      // adjacent to the threat
        RogueEnemy e = alerted(s, 28, 25);  // within REACTION_RADIUS of the player
        int eHp = e.getHp();

        List<String> msgs = new ArrayList<>();
        CompanionSystem.act(s, msgs);

        assertEquals(CompanionBehavior.FLEE, m.getBehavior(), "a non-combatant panics, it does not engage");
        assertEquals(eHp, e.getHp(), "she never strikes — non-combatants don't fight (FR-15)");
        assertTrue(m.hasCondition(Companion.Condition.PANICKED), "she carries her own PANICKED condition");
        assertTrue(msgs.contains("Mara panics!"), "the panic is observed: " + msgs);
        int distToThreat = Math.abs(m.getTileX() - e.getTileX()) + Math.abs(m.getTileY() - e.getTileY());
        assertTrue(distToThreat > 1, "she stepped away from the threat, not toward it");
    }

    @Test
    void aCalmNonCombatantTakesCoverAndDoesNotFightAnAdjacentEnemy() {
        RunState s = clearState();
        Companion m = mara(s, 25, 27);
        RogueEnemy unaware = new RogueEnemy(26, 27, s.getTileMap()); // adjacent, but not committed
        s.getEnemies().add(unaware);
        int eHp = unaware.getHp();

        CompanionSystem.act(s, new ArrayList<>());

        assertEquals(CompanionBehavior.TAKE_COVER, m.getBehavior(), "no threat → the non-combatant takes cover");
        assertEquals(eHp, unaware.getHp(), "a non-combatant never strikes, even an adjacent enemy (FR-15)");
    }

    // --- AC-2: detection/noise — panic is loud, hiding is quiet ---

    @Test
    void aPanickingCompanionEmitsNoise() {
        RunState s = clearState();
        Companion m = mara(s, 27, 25);
        alerted(s, 28, 25);

        CompanionSystem.act(s, new ArrayList<>());

        List<NoiseEvent> queue = s.getNoiseQueue();
        assertFalse(queue.isEmpty(), "panic enqueues a NoiseEvent");
        NoiseEvent n = queue.get(0);
        assertEquals(m.getTileX(), n.x, "the cry comes from the companion's tile");
        assertEquals(m.getTileY(), n.y);
        assertEquals(CompanionSystem.PANIC_NOISE_RADIUS, n.radius, "panic carries the panic radius");
    }

    @Test
    void panicNoiseCanBlowKleinsStealth() {
        // The panic NoiseEvent, resolved by the same NoiseSystem enemies obey (AD-9), raises a nearby
        // UNAWARE enemy to SUSPICIOUS — the companion blew Klein's stealth. (Resolve is called
        // directly to isolate the noise channel from DetectionSystem's LOS path.)
        RunState s = clearState();
        mara(s, 30, 30); // far from the player so her own detection doesn't interfere
        alerted(s, 28, 25); // the threat near the player that triggers her flee
        RogueEnemy bystander = new RogueEnemy(31, 31, s.getTileMap()); // UNAWARE, next to Mara
        s.getEnemies().add(bystander);
        assertEquals(Detection.UNAWARE, bystander.getDetection());

        CompanionSystem.act(s, new ArrayList<>()); // she flees, emits panic noise
        NoiseSystem.resolve(s);                    // the shared noise channel resolves it

        assertEquals(Detection.SUSPICIOUS, bystander.getDetection(),
                "the panic drew the bystander — stealth blown (AC-2)");
    }

    @Test
    void aHiddenCompanionIsQuietAndItsOrderIsHonored() {
        RunState s = clearState();
        Companion m = mara(s, 25, 28);
        m.setBehavior(CompanionBehavior.HIDE); // a standing order (Story 5.3 sets this)
        alerted(s, 26, 25); // a threat is present, but hiding persists and stays quiet

        CompanionSystem.act(s, new ArrayList<>());

        assertEquals(CompanionBehavior.HIDE, m.getBehavior(), "the autonomous machine does not overwrite a HIDE order");
        assertEquals(25, m.getTileX(), "a hidden companion holds still");
        assertEquals(28, m.getTileY());
        assertTrue(s.getNoiseQueue().isEmpty(), "a hidden companion is quiet — no noise (AC-2)");
        assertFalse(m.hasCondition(Companion.Condition.PANICKED), "hiding is not panicking");
    }
}

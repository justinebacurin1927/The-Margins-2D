package com.margins.rogue;

import com.margins.rogue.item.Supply;
import com.margins.rogue.state.RunState;
import com.margins.rogue.system.CombatSystem;
import com.margins.rogue.system.CompanionSystem;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Story 5.4 (FR-17, AD-10): a combatant companion fights through the single CombatSystem authority,
 * and any active companion is a liability — it eats Klein's rations, adds a faint party noise, and
 * can be wounded. Non-combatants never fight (5.2 gate). Driven directly through CompanionSystem.act
 * for a deterministic AI turn.
 */
class CompanionLiabilityTest {

    private static final int COOKED = Supply.COOKED_MEAT.ordinal();

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

    private RogueEnemy enemyAt(RunState s, int x, int y, Detection d) {
        RogueEnemy e = new RogueEnemy(x, y, s.getTileMap());
        e.setDetection(d);
        s.getEnemies().add(e);
        return e;
    }

    // --- AC-1: companion combat routes through CombatSystem ---

    @Test
    void companionAttackAppliesDamageThroughTheCombatAuthority() {
        RunState s = clearState();
        Companion c = s.getActiveCompanion();
        RogueEnemy e = enemyAt(s, 27, 25, Detection.ALERTED);
        int hp = e.getHp();

        List<String> msgs = new ArrayList<>();
        CombatSystem.companionAttack(s, c, e, msgs);

        assertEquals(hp - c.getDamage(), e.getHp(), "the companion's damage applies via CombatSystem");
        assertTrue(msgs.stream().anyMatch(m -> m.contains("Aldric strikes for")), msgs.toString());
    }

    @Test
    void aldricStrikesAnAdjacentThreatThroughEngageWhichNowRoutesToCombatSystem() {
        RunState s = clearState();
        Companion c = s.getActiveCompanion();
        c.placeAt(26, 25);                         // adjacent to the threat
        RogueEnemy e = enemyAt(s, 27, 25, Detection.ALERTED);
        int hp = e.getHp();

        CompanionSystem.act(s, new ArrayList<>()); // FIGHT_RETREAT → engage → CombatSystem.companionAttack

        assertEquals(hp - c.getDamage(), e.getHp(), "the engage strike routes through the combat authority");
    }

    // --- AC-2: the food cost is on Klein's rations, not his hunger meter ---

    @Test
    void aDueMealEatsOneRationFromTheePackAndKeepsTheCompanionFed() {
        RunState s = clearState();
        Companion c = s.getActiveCompanion();
        s.getInventory().tryAdd(COOKED, 2);
        c.setMealTimer(1);                 // a meal comes due this turn
        int hungerBefore = s.getPlayer().getHunger();

        CompanionSystem.act(s, new ArrayList<>());

        assertEquals(1, s.getInventory().count(COOKED), "the companion ate exactly one ration");
        assertFalse(c.hasCondition(Companion.Condition.WOUNDED), "a fed companion is not hungry");
        assertEquals(hungerBefore, s.getPlayer().getHunger(), "the food cost is the ration, NOT Klein's hunger meter");
        assertTrue(c.getMealTimer() > 1, "the meal timer reset after eating");
    }

    @Test
    void withNoRationTheCompanionGoesHungryAndWounded() {
        RunState s = clearState();
        Companion c = s.getActiveCompanion();
        // empty pack of prepared rations
        c.setMealTimer(1);

        List<String> msgs = new ArrayList<>();
        CompanionSystem.act(s, msgs);

        assertTrue(c.hasCondition(Companion.Condition.WOUNDED), "an unfed companion weakens");
        assertTrue(msgs.contains("Aldric is hungry."), "hunger is warned once: " + msgs);
        assertEquals(Companion.HUNGRY_RETRY, c.getMealTimer(), "it retries sooner while unfed");
    }

    // --- AC-2: the noise penalty (a moving party isn't silent) ---

    @Test
    void aMovingCompanionEmitsAFaintPartyNoise() {
        RunState s = clearState();
        Companion c = s.getActiveCompanion();
        c.placeAt(25, 28); // off its rear station → FOLLOW will step it this turn

        CompanionSystem.act(s, new ArrayList<>());

        assertFalse(s.getNoiseQueue().isEmpty(), "the moving party emits a NoiseEvent (AD-10)");
        assertEquals(CompanionSystem.PARTY_NOISE_RADIUS, s.getNoiseQueue().get(0).radius, "the faint party radius");
    }

    @Test
    void aHeldCompanionMakesNoPartyNoise() {
        RunState s = clearState();
        Companion c = s.getActiveCompanion();
        c.placeAt(25, 28);
        c.setBehavior(CompanionBehavior.HOLD);

        CompanionSystem.act(s, new ArrayList<>());

        assertTrue(s.getNoiseQueue().isEmpty(), "a held (still) companion is quiet");
    }

    // --- AC-2 ratify: woundable ---

    @Test
    void aBadlyHurtCompanionCarriesTheWoundedMarker() {
        RunState s = clearState();
        Companion c = s.getActiveCompanion();
        c.takeDamage(c.getMaxHp() - 1); // down to 1 HP — badly hurt

        CompanionSystem.act(s, new ArrayList<>());

        assertTrue(c.hasCondition(Companion.Condition.WOUNDED), "a low-HP companion is WOUNDED (observability)");
    }
}

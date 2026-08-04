package com.margins.rogue;

import com.margins.rogue.state.RunState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Region 1 hunger spec (docs/REGION1-FOREST.md §1): the four-tier countdown
 * (Well Fed → Satisfied → Hungry → Starving), Starving's three stages and damage
 * cadence, the Bloated regen/slow trade-off, and the stage-based eating penalties.
 */
class HungerSystemTest {

    private static RoguePlayer player() {
        return new RunState(1L).getPlayer();
    }

    /** Run enough acted turns to drive the player into Starving (Satisfied 250 → Hungry 250 → Starving 150). */
    private static RoguePlayer starvingPlayer() {
        RoguePlayer p = player();
        for (int i = 0; i < 500; i++) p.tickHunger();
        assertEquals(RoguePlayer.HungerStatus.STARVING, p.getStatus());
        return p;
    }

    @Test
    void startsSatisfiedAtFullDuration() {
        RoguePlayer p = player();
        assertEquals(RoguePlayer.HungerStatus.SATISFIED, p.getStatus(), "Satisfied is the starting status");
        assertEquals(250, p.getHunger());
    }

    @Test
    void countdownDropsThroughTheTiers() {
        RoguePlayer p = player();
        for (int i = 0; i < 250; i++) p.tickHunger();
        assertEquals(RoguePlayer.HungerStatus.HUNGRY, p.getStatus(), "Satisfied ends after 250 turns");
        assertEquals(250, p.getHunger());
        for (int i = 0; i < 250; i++) p.tickHunger();
        assertEquals(RoguePlayer.HungerStatus.STARVING, p.getStatus(), "Hungry ends after 250 turns");
        assertEquals(150, p.getHunger(), "Starving has its own 150-turn countdown");
    }

    @Test
    void starvingDamagesEveryFourTurnsInFatigue() {
        RoguePlayer p = starvingPlayer();
        int hp = p.getHp();
        for (int i = 0; i < 3; i++) p.tickHunger();
        assertEquals(hp, p.getHp(), "no damage before the 4-turn cadence lands");
        p.tickHunger(); // 4th turn in Starving
        assertEquals(hp - 1, p.getHp(), "base Starving damage: -1 HP every 4 turns");
    }

    @Test
    void rottingDoublesTheDamageAndAcceleratesIt() {
        RoguePlayer p = starvingPlayer();
        p.starve(105); // 150 → 45 → Rotting (stage 3)
        int hp = p.getHp();
        p.tickHunger(); // odd tick — no damage
        assertEquals(hp, p.getHp());
        p.tickHunger(); // even tick
        assertEquals(hp - 3, p.getHp(), "Rotting: -3 HP every 2 turns");
    }

    @Test
    void eatingFromFatigueRecoversToHungryAtFullDuration() {
        RoguePlayer p = starvingPlayer(); // Fatigue (stage 1): no eating penalty
        p.eat(100);
        assertEquals(RoguePlayer.HungerStatus.HUNGRY, p.getStatus(), "eating out of Starving lands on Hungry");
        assertEquals(250, p.getHunger(), "Fatigue: full Hungry duration, no eating-bonus");
    }

    @Test
    void eatingFromTremblingCutsTheNextStatusByTwentyPercent() {
        RoguePlayer p = starvingPlayer();
        p.starve(60); // 150 → 90 → Trembling (stage 2)
        p.eat(100);
        assertEquals(RoguePlayer.HungerStatus.HUNGRY, p.getStatus());
        assertEquals(200, p.getHunger(), "Trembling: -20% on the next status duration");
    }

    @Test
    void eatingFromRottingCutsTheNextStatusInHalf() {
        RoguePlayer p = starvingPlayer();
        p.starve(105); // → Rotting (stage 3)
        p.eat(100);
        assertEquals(RoguePlayer.HungerStatus.HUNGRY, p.getStatus());
        assertEquals(125, p.getHunger(), "Rotting: -50% on the next status duration (250 → 125)");
    }

    @Test
    void eatingBumpsTiersUpToWellFed() {
        RoguePlayer p = player();
        for (int i = 0; i < 250; i++) p.tickHunger(); // → Hungry
        p.eat(100);
        assertEquals(RoguePlayer.HungerStatus.SATISFIED, p.getStatus());
        p.eat(100);
        assertEquals(RoguePlayer.HungerStatus.WELL_FED, p.getStatus(), "a second full meal reaches Well Fed");
        assertEquals(350, p.getHunger());
        assertTrue(p.isSlowed(), "Bloated slow is active on entering Well Fed");
    }

    @Test
    void bloatedSlowLastsTheFirstFiftyTurnsOfWellFed() {
        RoguePlayer p = player();
        for (int i = 0; i < 250; i++) p.tickHunger();
        p.eat(100);
        p.eat(100); // → Well Fed, bloatedSlowTurns = 50
        assertTrue(p.isSlowed());
        for (int i = 0; i < 50; i++) p.tickHunger();
        assertFalse(p.isSlowed(), "Bloated slow ends after 50 turns");
        assertEquals(300, p.getHunger(), "Well Fed countdown still running after the slow ends");
    }

    @Test
    void bloatedRegenHealsUpToMaxHp() {
        RoguePlayer p = player();
        for (int i = 0; i < 250; i++) p.tickHunger();
        p.eat(100);
        p.eat(100); // → Well Fed
        p.hurtRaw(10);
        assertTrue(p.getHp() < p.getMaxHp());
        for (int i = 0; i < 40; i++) p.tickHunger();
        assertEquals(p.getMaxHp(), p.getHp(), "Bloated regen eventually tops the player back up");
    }

    @Test
    void starvingCutsStrengthButEatingOutRestoresIt() {
        RoguePlayer p = starvingPlayer();
        assertEquals(3, p.getStr(), "Fatigue: -35% max Strength (floor(5 × 0.65))");
        p.eat(100); // out of Starving
        assertEquals(5, p.getStr());
    }

    @Test
    void tremblingCutsDodgeButFatigueDoesNot() {
        RoguePlayer p = starvingPlayer(); // Fatigue
        assertEquals(21, p.dodgePercent(), "instinct 7 → 21% dodge before the Agility penalty");
        p.starve(60); // 90 → Trembling (stage 2)
        assertEquals(18, p.dodgePercent(), "Trembling: -15% Agility → instinct 6 → 18% dodge");
    }
}

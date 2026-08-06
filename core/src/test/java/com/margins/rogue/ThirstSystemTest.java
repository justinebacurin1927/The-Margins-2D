package com.margins.rogue;

import com.margins.rogue.state.RunState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Thirst spec (PRD FR-4): the four-tier countdown (Hydrated 200 → Thirsty 150 →
 * Dehydrated 100 → Parched 80), Parched's three stages (Withered → Trembling →
 * Dried Out) and its -2 HP / 5-turn drain, and drink() recovery. Mirrors HungerSystemTest.
 */
class ThirstSystemTest {

    private static RoguePlayer player() {
        return new RunState(1L).getPlayer();
    }

    /** Drive to Parched: Hydrated 200 + Thirsty 150 + Dehydrated 100 = 450 acted ticks. */
    private static RoguePlayer parchedPlayer() {
        RoguePlayer p = player();
        for (int i = 0; i < 450; i++) p.tickThirst();
        assertEquals(RoguePlayer.ThirstStatus.PARCHED, p.getThirstStatus());
        return p;
    }

    @Test
    void startsHydratedAtFullDuration() {
        RoguePlayer p = player();
        assertEquals(RoguePlayer.ThirstStatus.HYDRATED, p.getThirstStatus(), "Hydrated is the starting status");
        assertEquals(200, p.getThirst());
    }

    @Test
    void drainsThroughEveryTierInOrder() {
        RoguePlayer p = player();
        for (int i = 0; i < 200; i++) p.tickThirst();
        assertEquals(RoguePlayer.ThirstStatus.THIRSTY, p.getThirstStatus());
        for (int i = 0; i < 150; i++) p.tickThirst();
        assertEquals(RoguePlayer.ThirstStatus.DEHYDRATED, p.getThirstStatus());
        for (int i = 0; i < 100; i++) p.tickThirst();
        assertEquals(RoguePlayer.ThirstStatus.PARCHED, p.getThirstStatus());
        assertEquals(80, p.getThirst(), "Parched starts at its full 80-turn duration");
    }

    @Test
    void parchedDrainsTwoHpEveryFiveTurns() {
        RoguePlayer p = parchedPlayer();
        int hp = p.getHp();
        for (int i = 0; i < 5; i++) p.tickThirst();
        assertEquals(hp - 2, p.getHp(), "Parched costs -2 HP per 5 turns");
        for (int i = 0; i < 5; i++) p.tickThirst();
        assertEquals(hp - 4, p.getHp(), "the drain continues");
    }

    @Test
    void parchedRunsWitheredThenTremblingThenDriedOut() {
        RoguePlayer p = parchedPlayer();
        assertTrue(p.thirstLabel().contains("Withered"), "Parched opens on Withered: " + p.thirstLabel());
        while (p.getThirst() > 53) p.tickThirst();       // into the Trembling stage (27..53)
        assertTrue(p.thirstLabel().contains("Trembling"), "mid-Parched is Trembling: " + p.thirstLabel());
        while (p.getThirst() > 0) p.tickThirst();
        assertTrue(p.thirstLabel().contains("Dried Out"), "late-Parched is Dried Out: " + p.thirstLabel());
    }

    @Test
    void parchedTremblingReducesDodgeButDoesNotStackWithHunger() {
        int baseline = player().dodgePercent();
        RoguePlayer p = parchedPlayer();
        while (p.getThirst() > 53) p.tickThirst();        // Parched → Trembling stage (27..53)
        int trembling = p.dodgePercent();
        assertTrue(trembling < baseline, "Parched-Trembling applies the -15% AG penalty");
        // Not stacked: the penalty is a single 0.85 multiplier regardless of source.
        assertEquals(Math.round(player().getInstinct() * 0.85f) * 3, trembling, "single -15%, not doubled");
    }

    @Test
    void drinkRisesOneTier() {
        RoguePlayer p = player();
        for (int i = 0; i < 200; i++) p.tickThirst();     // → Thirsty
        assertEquals(RoguePlayer.ThirstStatus.THIRSTY, p.getThirstStatus());
        p.drink(100);
        assertEquals(RoguePlayer.ThirstStatus.HYDRATED, p.getThirstStatus(), "a full drink bumps one tier up");
        assertEquals(200, p.getThirst());
    }
}

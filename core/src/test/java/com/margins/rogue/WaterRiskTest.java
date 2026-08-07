package com.margins.rogue;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import com.margins.rogue.Companion;
import com.margins.rogue.RogueEnemy;
import com.margins.rogue.item.FloorItem;
import com.margins.rogue.item.Supply;
import com.margins.rogue.state.FlagStore;
import com.margins.rogue.state.RunState;
import com.margins.rogue.system.ConsumptionSystem;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Consumption poison risk (FR-6, Story 1.5 AC-1 / Story 1.7): drinking untreated water rolls its
 * source's risk on the seeded RNG (AD-5) — river ~20%, clean water never. On a failed roll the
 * player starts the tiered bacterial track (Nausea + Diarrhea, Story 1.7) — the onset replaces
 * Story 1.5's flat HP sting, so "poisoned" is a debuff, not HP harm. Deterministic per seed.
 */
class WaterRiskTest {

    /** Consume one unit of {@code type} on a fresh seed; return true if it started the bacterial track. */
    private static boolean poisoned(long seed, Supply type) {
        RunState s = new RunState(seed);
        s.getInventory().tryAdd(type.ordinal(), 1);
        if (type.isWater()) {
            for (int i = 0; i < 210; i++) s.getPlayer().tickThirst(); // HYDRATED → THIRSTY, so a drink isn't refused (Edge #2)
        }
        ConsumptionSystem.consume(s, type.ordinal(), new ArrayList<>());
        return s.getPlayer().getBacterialStage() != RoguePlayer.BacterialStage.NONE;
    }

    private static int poisonCount(Supply type, int seeds) {
        int n = 0;
        for (long seed = 1; seed <= seeds; seed++) if (poisoned(seed, type)) n++;
        return n;
    }

    /** Mirrors SaveService.json() element-type registration AND usePrototypes(false), so the
     *  test serializer matches production (the divergence would mask save-format bugs). */
    private static Json json() {
        Json json = new Json();
        json.setOutputType(JsonWriter.OutputType.json);
        json.setUsePrototypes(false);
        json.setElementType(RunState.class, "enemies", RogueEnemy.class);
        json.setElementType(RunState.class, "floorItems", FloorItem.class);
        json.setElementType(RunState.class, "companions", Companion.class);
        json.setElementType(FlagStore.class, "flags", Integer.class);
        return json;
    }

    /** Load a run (a save taken right before its first risky consume) and consume one unit of
     *  {@code type}; return whether it started the bacterial track. */
    private static boolean poisonedAfterLoad(RunState loaded, Supply type) {
        loaded.restoreAfterLoad();
        assertTrue(ConsumptionSystem.consume(loaded, type.ordinal(), new ArrayList<>()),
                "the loaded run can still consume the provision");
        return loaded.getPlayer().getBacterialStage() != RoguePlayer.BacterialStage.NONE;
    }

    @Test
    void cleanWaterNeverPoisons() {
        assertEquals(0, poisonCount(Supply.BOILED_WATER, 300), "boiled water is 0% risk");
        assertEquals(0, poisonCount(Supply.WELL_WATER, 300), "well water is stable/safe");
    }

    @Test
    void riverWaterPoisonsAboutTwentyPercent() {
        int seeds = 600;
        int poisoned = poisonCount(Supply.RIVER_WATER, seeds);
        double rate = poisoned / (double) seeds;
        assertTrue(rate > 0.10 && rate < 0.32,
                "river direct-drink poisons ~20% of the time (was " + rate + ")");
    }

    @Test
    void spoiledMeatAlmostAlwaysPoisons() {
        int seeds = 300;
        int poisoned = poisonCount(Supply.SPOILED_MEAT, seeds);
        assertTrue(poisoned > seeds * 0.75, "spoiled meat (90% risk) usually poisons (was " + poisoned + "/" + seeds + ")");
    }

    @Test
    void riskOutcomeReproducesAfterSaveLoad() {
        // The old test compared a method to itself (tautological — a fresh RunState(seed) is
        // deterministic by construction). The meaningful contract: a run saved before its first
        // risky consume rolls the SAME outcome every time it is loaded — restoreAfterLoad
        // rebuilds the RNG from the stored seed (AD-5). This is what could actually break (e.g.
        // if the load path used an unseeded RNG instead).
        for (long seed = 1; seed <= 25; seed++) {
            RunState s = new RunState(seed);
            s.getInventory().tryAdd(Supply.RIVER_WATER.ordinal(), 1);
            for (int i = 0; i < 210; i++) s.getPlayer().tickThirst(); // save a THIRSTY player, so the loaded consume isn't refused
            String saved = json().toJson(s);

            boolean a = poisonedAfterLoad(json().fromJson(RunState.class, saved), Supply.RIVER_WATER);
            boolean b = poisonedAfterLoad(json().fromJson(RunState.class, saved), Supply.RIVER_WATER);

            assertEquals(a, b, "seed " + seed + ": the same save reproduces the same risk roll (AD-5)");
        }
    }

    @Test
    void eatingAndDrinkingWhenAlreadyFullAreRefused() {
        // Edge #2-review: a provision whose nourishment would be wasted (already Well Fed /
        // Hydrated) is refused — nothing spent, no turn, no poison roll. Otherwise a full player
        // could "self-poison for nothing" by drinking risky water.
        RunState s = new RunState(1L);
        s.getInventory().tryAdd(Supply.POND_WATER.ordinal(), 1);
        s.getInventory().tryAdd(Supply.RAW_MEAT.ordinal(), 1);
        int hp = s.getPlayer().getHp();

        // A fresh player is HYDRATED (drink track full) but SATISFIED (food track open).
        assertFalse(ConsumptionSystem.consume(s, Supply.POND_WATER.ordinal(), new ArrayList<>()),
                "drinking while hydrated is refused");
        assertEquals(1, s.getInventory().count(Supply.POND_WATER.ordinal()), "no water spent");
        assertEquals(hp, s.getPlayer().getHp(), "no poison rolled");

        // Reach Well Fed by eating a full tier, then raw meat is refused too.
        s.getPlayer().eat(100); // SATISFIED → WELL_FED (FOOD_PER_TIER)
        assertFalse(ConsumptionSystem.consume(s, Supply.RAW_MEAT.ordinal(), new ArrayList<>()),
                "eating while well-fed is refused");
        assertEquals(1, s.getInventory().count(Supply.RAW_MEAT.ordinal()), "no meat spent");
        assertEquals(hp, s.getPlayer().getHp(), "still no poison rolled");
    }

    @Test
    void aThirstyPlayerCanDrink() {
        // The refusal only fires when full: once thirst drops, drinking pond water works (and may
        // poison — the risk roll is live for a thirsty player).
        RunState s = new RunState(1L);
        for (int i = 0; i < 210; i++) s.getPlayer().tickThirst(); // HYDRATED(200) → THIRSTY
        s.getInventory().tryAdd(Supply.POND_WATER.ordinal(), 1);

        assertTrue(ConsumptionSystem.consume(s, Supply.POND_WATER.ordinal(), new ArrayList<>()),
                "a thirsty player can drink");
        assertEquals(0, s.getInventory().count(Supply.POND_WATER.ordinal()), "the water is consumed");
    }
}

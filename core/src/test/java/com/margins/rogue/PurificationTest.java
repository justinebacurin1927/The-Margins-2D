package com.margins.rogue;

import com.margins.rogue.item.Inventory;
import com.margins.rogue.item.Supply;
import com.margins.rogue.state.RunState;
import com.margins.rogue.system.PurificationSystem;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Two-step purification (FR-6, Story 1.5 AC-1/2/4): filtration (SKILL-based) reduces poison
 * risk but never to zero; boiling (fire + coal) makes water clean (0% risk). Refused steps
 * commit no turn. All rolls draw from the seeded RNG (AD-5).
 */
class PurificationTest {

    private static int add(RunState s, Supply type, int n) {
        s.getInventory().tryAdd(type.ordinal(), n);
        return type.ordinal();
    }

    @Test
    void filtrationReducesRiskButNeverToZero() {
        // AC-2: filtration reduces but does not eliminate — the filtered type still carries risk.
        assertTrue(Supply.RIVER_WATER.drinkRisk() > Supply.FILTERED_WATER.drinkRisk(),
                "filtering a river cuts its risk");
        assertTrue(Supply.FILTERED_WATER.drinkRisk() > 0, "filtered water is still not safe");
        assertEquals(0, Supply.BOILED_WATER.drinkRisk(), "only boiling reaches 0% risk");
    }

    @Test
    void boilingAtAFireWithCoalYieldsCleanWater() {
        RunState s = new RunState(1L);
        s.setCampfire(s.getPlayer().getTileX(), s.getPlayer().getTileY()); // player is at the fire
        int coal = add(s, Supply.COAL, 1);
        int water = add(s, Supply.FILTERED_WATER, 1);

        boolean acted = PurificationSystem.boil(s, water, new ArrayList<>());

        assertTrue(acted, "boiling spends a turn");
        assertEquals(1, s.getInventory().count(Supply.BOILED_WATER.ordinal()), "clean water produced");
        assertEquals(0, s.getInventory().count(water), "the raw water was consumed");
        assertEquals(0, s.getInventory().count(coal), "the coal fuel was consumed");
    }

    @Test
    void boilingWithoutCoalIsRefused() {
        RunState s = new RunState(1L);
        s.setCampfire(s.getPlayer().getTileX(), s.getPlayer().getTileY());
        int water = add(s, Supply.RIVER_WATER, 1); // no coal

        boolean acted = PurificationSystem.boil(s, water, new ArrayList<>());

        assertFalse(acted, "no coal → no turn");
        assertEquals(1, s.getInventory().count(water), "the water is untouched");
        assertEquals(0, s.getInventory().count(Supply.BOILED_WATER.ordinal()), "nothing was boiled");
    }

    @Test
    void boilingWithoutAFireIsRefused() {
        RunState s = new RunState(1L); // no campfire built
        add(s, Supply.COAL, 1);
        int water = add(s, Supply.RIVER_WATER, 1);

        assertFalse(PurificationSystem.boil(s, water, new ArrayList<>()), "no fire → no turn");
        assertEquals(0, s.getInventory().count(Supply.BOILED_WATER.ordinal()));
    }

    @Test
    void filteringIsSkillGoverned() {
        // Higher SKILL filters successfully more often across a seed sweep (AC-4).
        int lowSkillFiltered = filterSuccessCount(0);
        int highSkillFiltered = filterSuccessCount(10);
        assertTrue(highSkillFiltered > lowSkillFiltered,
                "SKILL 10 filters more often than SKILL 0 (" + highSkillFiltered + " vs " + lowSkillFiltered + ")");
    }

    private static int filterSuccessCount(int skill) {
        int produced = 0;
        for (long seed = 1; seed <= 300; seed++) {
            RunState s = new RunState(seed);
            s.getPlayer().setSkill(skill);
            int water = add(s, Supply.RIVER_WATER, 1);
            PurificationSystem.filter(s, water, new ArrayList<>());
            produced += s.getInventory().count(Supply.FILTERED_WATER.ordinal());
        }
        return produced;
    }
}

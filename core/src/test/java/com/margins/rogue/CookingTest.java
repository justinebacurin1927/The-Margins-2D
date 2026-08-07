package com.margins.rogue;

import com.margins.rogue.item.Supply;
import com.margins.rogue.state.RunState;
import com.margins.rogue.system.CookingSystem;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Cooking at a fire (FR-6, Story 1.5 AC-3/4): a SKILL-governed roll turns raw meat into
 * cooked meat (which resists spoilage) or ruins it. Refused when there is no fire or no raw
 * meat. Rolls draw from the seeded RNG (AD-5).
 */
class CookingTest {

    private static RunState atFireWithMeat(long seed) {
        RunState s = new RunState(seed);
        s.setCampfire(s.getPlayer().getTileX(), s.getPlayer().getTileY());
        s.getInventory().tryAdd(Supply.RAW_MEAT.ordinal(), 1);
        return s;
    }

    @Test
    void aFreshPlayerHasTheStartingSkill() {
        assertEquals(5, new RunState(1L).getPlayer().getSkill(), "SKILL starts at 5 (FR-11 axis)");
    }

    @Test
    void cookingConsumesTheRawMeatAndProducesAnOutcome() {
        RunState s = atFireWithMeat(1L);
        boolean acted = CookingSystem.cook(s, Supply.RAW_MEAT.ordinal(), new ArrayList<>());

        assertTrue(acted, "cooking at a fire spends a turn");
        assertEquals(0, s.getInventory().count(Supply.RAW_MEAT.ordinal()), "the raw meat is consumed");
        int cooked = s.getInventory().count(Supply.COOKED_MEAT.ordinal());
        int ruined = s.getInventory().count(Supply.SPOILED_MEAT.ordinal());
        assertEquals(1, cooked + ruined, "the outcome is exactly one cooked OR one ruined meat");
    }

    @Test
    void cookingIsSkillGoverned() {
        int lowSkillCooked = cookedCount(0);
        int highSkillCooked = cookedCount(10);
        assertTrue(highSkillCooked > lowSkillCooked,
                "SKILL 10 cooks more often than SKILL 0 (" + highSkillCooked + " vs " + lowSkillCooked + ")");
    }

    @Test
    void cookingWithoutAFireIsRefused() {
        RunState s = new RunState(1L); // no campfire
        s.getInventory().tryAdd(Supply.RAW_MEAT.ordinal(), 1);

        assertFalse(CookingSystem.cook(s, Supply.RAW_MEAT.ordinal(), new ArrayList<>()), "no fire → no turn");
        assertEquals(1, s.getInventory().count(Supply.RAW_MEAT.ordinal()), "the meat is untouched");
    }

    @Test
    void cookingWithoutRawMeatIsRefused() {
        RunState s = new RunState(1L);
        s.setCampfire(s.getPlayer().getTileX(), s.getPlayer().getTileY());
        // no meat in the pack
        assertFalse(CookingSystem.cook(s, Supply.RAW_MEAT.ordinal(), new ArrayList<>()), "no meat → no turn");
    }

    private static int cookedCount(int skill) {
        int cooked = 0;
        for (long seed = 1; seed <= 300; seed++) {
            RunState s = atFireWithMeat(seed);
            s.getPlayer().setSkill(skill);
            CookingSystem.cook(s, Supply.RAW_MEAT.ordinal(), new ArrayList<>());
            cooked += s.getInventory().count(Supply.COOKED_MEAT.ordinal());
        }
        return cooked;
    }
}

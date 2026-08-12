package com.margins.rogue.item;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Story 4.4 (FR-13, AD-13): the pure {@link Weapon} model — wear, break, and the gear-with-memory
 * repair curve. These pin the AD-13 percentages directly against a weapon whose original max is 100,
 * so the table values ARE the expected maxima.
 */
class WeaponTest {

    /** originalMax = 100 so each curve percentage maps 1:1 to the expected max. */
    private Weapon freshHundred() { return new Weapon(Weapon.Category.BLADE, 3, 4, 100); }

    @Test
    void decayReducesDurabilityAndBreaksAtZero() {
        Weapon w = freshHundred();
        w.decay(30);
        assertEquals(70, w.getDurability());
        assertFalse(w.isBroken());
        w.decay(70);
        assertEquals(0, w.getDurability());
        assertTrue(w.isBroken());
        w.decay(5);
        assertEquals(0, w.getDurability(), "durability never goes negative");
    }

    @Test
    void damageBonusIsZeroOnceBroken() {
        Weapon w = freshHundred();
        assertEquals(4, w.damageBonus(), "a usable weapon adds its tier bonus");
        w.decay(100);
        assertEquals(0, w.damageBonus(), "a broken weapon adds nothing");
    }

    @Test
    void skillBandsSplitLowMidHigh() {
        assertEquals(0, Weapon.skillBand(1));
        assertEquals(0, Weapon.skillBand(Weapon.SKILL_LOW_MAX), "3 is the top of Low");
        assertEquals(1, Weapon.skillBand(4));
        assertEquals(1, Weapon.skillBand(6));
        assertEquals(2, Weapon.skillBand(Weapon.SKILL_HIGH_MIN), "7 is the bottom of High");
        assertEquals(2, Weapon.skillBand(9));
    }

    @Test
    void repairCurveLowSkill() {
        int[] expected = {90, 78, 65, 50, 35}; // AD-13 Low column
        Weapon w = freshHundred();
        for (int i = 0; i < expected.length; i++) {
            w.decay(w.getDurability()); // wear it down first — repair should restore to the new max
            assertTrue(w.repair(1), "Low-skill repair #" + (i + 1) + " is allowed");
            assertEquals(expected[i], w.getMaxDurability(), "Low max after repair #" + (i + 1));
            assertEquals(expected[i], w.getDurability(), "durability restored to the new max");
            assertEquals(i + 1, w.getRepairCount());
        }
        assertFalse(w.repair(1), "the 6th repair is beyond repair");
    }

    @Test
    void repairCurveMidAndHighSkill() {
        int[] mid  = {93, 84, 74, 63, 51}; // AD-13 Mid column
        int[] high = {96, 91, 85, 78, 70}; // AD-13 High column
        Weapon wm = freshHundred();
        Weapon wh = freshHundred();
        for (int i = 0; i < 5; i++) {
            assertTrue(wm.repair(5));
            assertEquals(mid[i], wm.getMaxDurability(), "Mid max after repair #" + (i + 1));
            assertTrue(wh.repair(9));
            assertEquals(high[i], wh.getMaxDurability(), "High max after repair #" + (i + 1));
        }
        assertFalse(wm.repair(5), "Mid beyond repair at the 6th");
        assertFalse(wh.repair(9), "High beyond repair at the 6th");
    }

    @Test
    void beyondRepairDoesNotMutate() {
        Weapon w = freshHundred();
        for (int i = 0; i < Weapon.MAX_REPAIRS; i++) assertTrue(w.repair(5));
        int max = w.getMaxDurability(), dur = w.getDurability(), rc = w.getRepairCount();
        assertFalse(w.isRepairable());
        assertFalse(w.repair(5), "a 6th repair is refused");
        assertEquals(max, w.getMaxDurability(), "no mutation on a refused repair");
        assertEquals(dur, w.getDurability());
        assertEquals(rc, w.getRepairCount());
    }

    @Test
    void tierFactoriesCarryTierData() {
        assertEquals(Weapon.Category.SPEAR, Weapon.spearT1().getCategory());
        assertEquals(1, Weapon.spearT1().getTier());
        assertEquals(5, Weapon.bowT5().getTier());
        assertTrue(Weapon.bladeT3().getMaxDurability() > Weapon.spearT1().getMaxDurability(),
                "a higher tier is tougher (bigger ceiling)");
        assertTrue(Weapon.bladeT3().damageBonus() > Weapon.spearT1().damageBonus(),
                "a higher tier hits harder");
        assertEquals("Spear", Weapon.spearT1().displayName());
    }
}

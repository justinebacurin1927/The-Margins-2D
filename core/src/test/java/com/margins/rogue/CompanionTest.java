package com.margins.rogue;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Story 4.1/4.2 — the companion's greedy follow and per-floor Distraction budget (AD-10). */
class CompanionTest {

    /** A fully open (all-floor) map so movement is never wall-blocked. */
    private RogueTileMap openMap(int w, int h) {
        RogueTileMap m = new RogueTileMap(w, h);
        m.fill(RogueTile.FLOOR);
        return m;
    }

    @Test
    void stepToMovesOnlyOntoWalkableTiles() {
        RogueTileMap m = openMap(10, 10);
        m.setTile(1, 0, RogueTile.WALL); // block the first x step
        Companion c = new Companion(0, 0, m, "galleon");
        c.stepTo(1, 0); // onto the wall — refused
        assertEquals(0, c.getTileX());
        assertEquals(0, c.getTileY());
        c.stepTo(0, 1); // onto open floor — moves
        assertEquals(0, c.getTileX());
        assertEquals(1, c.getTileY());
        c.stepTo(0, 1); // already there — no-op
        assertEquals(0, c.getTileX());
        assertEquals(1, c.getTileY());
    }

    @Test
    void distractionBudgetIsTwoPerFloorAndRefills() {
        RogueTileMap m = openMap(5, 5);
        Companion c = new Companion(1, 1, m, "galleon");
        assertEquals(Companion.MAX_DISTRACTIONS_PER_FLOOR, c.getDistractionsLeft());
        assertTrue(c.canDistract());

        c.useDistraction();
        assertTrue(c.canDistract(), "one shout left after spending one of two");
        c.useDistraction();
        assertFalse(c.canDistract(), "spent both");
        assertEquals(0, c.getDistractionsLeft());

        c.useDistraction(); // floored, never negative
        assertEquals(0, c.getDistractionsLeft());

        c.resetDistractions();
        assertEquals(Companion.MAX_DISTRACTIONS_PER_FLOOR, c.getDistractionsLeft(), "resetDistractions refills the budget");
    }

    // --- combat model (fix #1): Aldric is a fighter with HP and damage, not a tail ---

    @Test
    void companionHasCombatStats() {
        RogueTileMap m = openMap(10, 10);
        Companion c = new Companion(0, 0, m, "galleon");
        assertEquals(14, c.getMaxHp(), "sturdier than a soldier (8), lighter than the player (20)");
        assertEquals(14, c.getHp(), "starts at full HP");
        assertEquals(3, c.getDamage(), "strikes like a soldier");
        assertTrue(c.isAlive());
    }

    @Test
    void companionTakesDamageAndCanFall() {
        RogueTileMap m = openMap(10, 10);
        Companion c = new Companion(0, 0, m, "galleon");
        assertEquals(5, c.takeDamage(5), "takeDamage reports the dealt amount");
        assertEquals(9, c.getHp());
        assertTrue(c.isAlive());
        c.takeDamage(20); // overkill clamps at 0
        assertEquals(0, c.getHp());
        assertFalse(c.isAlive());
    }
}

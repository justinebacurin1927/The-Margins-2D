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
    void greedyStepTakesTheXAxisFirst() {
        RogueTileMap m = openMap(10, 10);
        Companion c = new Companion(0, 0, m, "galleon");
        c.followStep(3, 3);
        assertEquals(1, c.getTileX(), "x step is tried before y");
        assertEquals(0, c.getTileY());
    }

    @Test
    void followsUntilAdjacentThenStopsWithoutLandingOnTheTarget() {
        RogueTileMap m = openMap(10, 10);
        Companion c = new Companion(0, 0, m, "galleon");
        int targetX = 5, targetY = 0;
        for (int i = 0; i < 20; i++) c.followStep(targetX, targetY);
        assertTrue(c.isAdjacentTo(targetX, targetY), "it closes the distance to adjacent");
        assertFalse(c.getTileX() == targetX && c.getTileY() == targetY, "it never stands on the player's tile");
    }

    @Test
    void adjacentCompanionDoesNotMove() {
        RogueTileMap m = openMap(10, 10);
        Companion c = new Companion(4, 0, m, "galleon");
        c.followStep(5, 0); // already adjacent
        assertEquals(4, c.getTileX());
        assertEquals(0, c.getTileY());
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
}

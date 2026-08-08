package com.margins.rogue;

import com.margins.rogue.state.RunState;
import com.margins.rogue.system.CompanionSystem;
import com.margins.rogue.system.PlayerAction;
import com.margins.rogue.system.TurnEngine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Aldric's AI (CompanionSystem): he FOLLOWS with purpose — holding a rear-guard station two tiles
 * behind the player (opposite the facing) when calm, charging an ALERTED enemy to fight it, and
 * returning to his station once the threat is gone. He ignores threats beyond his reaction radius
 * and never pulls unprovoked fights. All driven through the real turn pipeline
 * (CompanionSystem.follow runs before the enemy phase, AD-4).
 */
class CompanionAiTest {

    /** A run with the player on cleared open floor and no ambient enemies — full control. The
     *  clear covers the player's full patrol radius (6) so patrol steps always find floor. */
    private RunState combatState() {
        RunState s = new RunState(42L);
        RogueTileMap m = s.getTileMap();
        s.getPlayer().placeAt(25, 25);
        for (int x = 19; x <= 33; x++)
            for (int y = 19; y <= 33; y++)
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

    @Test
    void aldricTrailsTwoTilesBehindThePlayersMovement() {
        RunState s = combatState();
        Companion c = s.getActiveCompanion();
        c.placeAt(25, 23); // two north of the player — already at the rear station for a NORTH move

        new TurnEngine().advance(s, PlayerAction.move(0, 1, 1)); // player -> (25,26), station -> (25,24)
        assertEquals(24, c.getTileY(), "Aldric trails the first step, two tiles behind");
        new TurnEngine().advance(s, PlayerAction.move(0, 1, 1)); // player -> (25,27), station -> (25,25)
        assertEquals(25, c.getTileY(), "he trails the second step, walking where the player just was");
        new TurnEngine().advance(s, PlayerAction.move(0, 1, 1)); // player -> (25,28), station -> (25,26)
        assertEquals(26, c.getTileY());

        assertEquals(28, s.getPlayer().getTileY());
        assertEquals(2, Math.abs(c.getTileX() - s.getPlayer().getTileX())
                + Math.abs(c.getTileY() - s.getPlayer().getTileY()),
                "always exactly two tiles behind — a purposeful follow, not a tail");
    }

    @Test
    void aldricHoldsHisRearStationWhileThePlayerWaits() {
        RunState s = combatState();
        Companion c = s.getActiveCompanion();
        c.placeAt(25, 23); // the rear station for a NORTH-facing player
        for (int i = 0; i < 5; i++) new TurnEngine().advance(s, PlayerAction.wait(1)); // facing NORTH
        assertEquals(25, c.getTileX());
        assertEquals(23, c.getTileY(), "he holds his post behind the player — purposeful, not drifting");
    }

    @Test
    void aldricBacksOffToHisStationInsteadOfCrowding() {
        RunState s = combatState();
        Companion c = s.getActiveCompanion();
        c.placeAt(25, 24); // adjacent — one tile behind, too close

        new TurnEngine().advance(s, PlayerAction.wait(1)); // facing NORTH, station (25,23)

        assertEquals(25, c.getTileX());
        assertEquals(23, c.getTileY(), "Aldric steps back to his rear station instead of crowding Klein");
    }

    @Test
    void aldricChargesAndFightsAnAlertedEnemyOnHisOwn() {
        RunState s = combatState();
        Companion c = s.getActiveCompanion();
        c.placeAt(27, 25);
        RogueEnemy e = alerted(s, 29, 25); // dist 4 from the player — inside his reaction radius
        int eHp = e.getHp();

        for (int i = 0; i < 10 && e.isAlive(); i++) {
            new TurnEngine().advance(s, PlayerAction.wait(0));
        }

        assertFalse(e.isAlive(), "Aldric alone kills the alerted enemy (he fights on his own)");
        assertTrue(eHp > 0 && e.getHp() == 0, "the enemy went from " + eHp + " to 0");
        assertTrue(c.getHp() < c.getMaxHp(), "and he takes hits doing it — fighting is a real cost");
        assertTrue(s.getMessageLog().stream().anyMatch(m -> m.contains("Aldric strikes")),
                "his strikes are observed: " + s.getMessageLog());
    }

    @Test
    void aldricDoesNotChaseAThreatOutsideHisReactionRadius() {
        RunState s = combatState();
        Companion c = s.getActiveCompanion();
        c.placeAt(25, 27); // his station for a SOUTH-facing player
        RogueEnemy e = alerted(s, 40, 25); // dist 15 from the player — far beyond REACTION_RADIUS
        int eHp = e.getHp();

        for (int i = 0; i < 2; i++) new TurnEngine().advance(s, PlayerAction.wait(0)); // facing SOUTH

        assertEquals(25, c.getTileX());
        assertEquals(27, c.getTileY(), "Aldric holds his rear station — no cross-map charge");
        assertEquals(eHp, e.getHp(), "the far enemy is never engaged");
    }

    @Test
    void aldricReturnsToHisStationAfterTheThreatIsGone() {
        RunState s = combatState();
        Companion c = s.getActiveCompanion();
        c.placeAt(25, 27); // his rear station for a SOUTH-facing player
        RogueEnemy e = alerted(s, 27, 27);
        for (int i = 0; i < 15 && e.isAlive(); i++) new TurnEngine().advance(s, PlayerAction.wait(0));
        assertFalse(e.isAlive(), "the fight resolved");

        for (int i = 0; i < 10; i++) new TurnEngine().advance(s, PlayerAction.wait(0));

        assertEquals(25, c.getTileX());
        assertEquals(27, c.getTileY(), "with no threat left, Aldric returns to his rear station");
    }

    @Test
    void aldricLeavesUnawareEnemiesAlone() {
        RunState s = combatState();
        Companion c = s.getActiveCompanion();
        c.placeAt(26, 25);
        RogueEnemy e = new RogueEnemy(27, 25, s.getTileMap()); // unaware, adjacent to Aldric
        s.getEnemies().add(e);
        int eHp = e.getHp();

        new TurnEngine().advance(s, PlayerAction.wait(0));

        assertEquals(eHp, e.getHp(), "Aldric does not strike an enemy that isn't committed to the fight");
        assertTrue(s.getMessageLog().stream().noneMatch(m -> m.contains("Aldric strikes")),
                "no unprovoked strike: " + s.getMessageLog());
    }
}

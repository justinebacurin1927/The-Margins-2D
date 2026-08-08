package com.margins.rogue;

import com.margins.rogue.state.RunState;
import com.margins.rogue.system.PlayerAction;
import com.margins.rogue.system.TurnEngine;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Combat fixes (all three driven through the real turn pipeline):
 * <ul>
 *   <li>#3 — the player's melee aims any of the 8 directions (cardinals AND diagonals).</li>
 *   <li>#2 — actors never share a tile: the player can't walk onto an enemy, and enemies can't
 *       walk onto the player or the companion.</li>
 *   <li>#1 — Aldric fights (strikes an adjacent enemy instead of trailing) and is a real enemy
 *       target who can fall.</li>
 * </ul>
 */
class CombatTest {

    /** A run with the player on cleared open floor and no ambient enemies — full control. */
    private RunState combatState() {
        RunState s = new RunState(42L);
        RogueTileMap m = s.getTileMap();
        s.getPlayer().placeAt(25, 25);
        for (int x = 21; x <= 29; x++)
            for (int y = 21; y <= 29; y++)
                m.setTile(x, y, RogueTile.FLOOR);
        s.getEnemies().clear();
        return s;
    }

    /** Park the companion well away from the fight so his own follow/attack can't skew an
     *  attack-or-move assertion. */
    private void parkCompanion(RunState s) {
        s.getActiveCompanion().placeAt(29, 29);
    }

    /** An ALERTED enemy (so the enemy phase pursues/attacks) at (x,y), already in the list. */
    private RogueEnemy alerted(RunState s, int x, int y) {
        RogueEnemy e = new RogueEnemy(x, y, s.getTileMap());
        e.setDetection(Detection.ALERTED);
        s.getEnemies().add(e);
        return e;
    }

    // --- #3: the 8-direction aimed melee ---

    @Test
    void attackHitsAnEnemyOnEveryDiagonal() {
        // Each diagonal gets a FRESH state: an alerted enemy chases toward the player during the
        // enemy phase, so a second swing in the same run would find the tile vacated.
        strikeDiagonal(RoguePlayer.NORTHEAST, 26, 26);
        strikeDiagonal(RoguePlayer.SOUTHWEST, 24, 24);
        strikeDiagonal(RoguePlayer.NORTHWEST, 24, 26);
        strikeDiagonal(RoguePlayer.SOUTHEAST, 26, 24);
    }

    /** A fresh run: aim one swing at the diagonal enemy and assert it lands (8 - str 5 = 3). */
    private void strikeDiagonal(int dir, int ex, int ey) {
        RunState s = combatState();
        parkCompanion(s);
        RogueEnemy e = alerted(s, ex, ey);
        new TurnEngine().advance(s, PlayerAction.attack(dir));
        assertEquals(3, e.getHp(), "the " + dir + " diagonal is struck");
    }

    @Test
    void cardinalAttackStillHitsDirectlyAdjacentEnemies() {
        RunState s = combatState();
        parkCompanion(s);
        RogueEnemy north = alerted(s, 25, 26);
        int hp = north.getHp();

        new TurnEngine().advance(s, PlayerAction.attack(RoguePlayer.NORTH));

        assertEquals(hp - 5, north.getHp(), "the cardinal strike lands unchanged");
    }

    @Test
    void attackIntoAnEmptyTileStillCommitsATurn() {
        RunState s = combatState();
        parkCompanion(s);
        int clock = s.getClockTurns();

        new TurnEngine().advance(s, PlayerAction.attack(RoguePlayer.NORTHWEST));

        assertEquals(clock + 1, s.getClockTurns(), "a swing is a committed turn even into air");
        assertTrue(s.getMessageLog().stream().anyMatch(m -> m.contains("Nothing there")),
                "an empty swing is called out: " + s.getMessageLog());
    }

    // --- #2: actors never share a tile ---

    @Test
    void playerCannotWalkOntoAnEnemysTile() {
        RunState s = combatState();
        parkCompanion(s);
        alerted(s, 26, 25); // directly east of the player
        int clock = s.getClockTurns();
        int px = s.getPlayer().getTileX(), py = s.getPlayer().getTileY();

        new TurnEngine().advance(s, PlayerAction.move(1, 0, RoguePlayer.EAST));

        assertEquals(px, s.getPlayer().getTileX(), "the player stays put");
        assertEquals(py, s.getPlayer().getTileY(), "the player stays put");
        assertEquals(clock, s.getClockTurns(), "the refused move commits no turn (AD-5)");
        assertTrue(s.getMessageLog().stream().anyMatch(m -> m.contains("blocks the way")),
                "the refusal is announced: " + s.getMessageLog());
    }

    @Test
    void playerCanWalkOntoTheCompanionsTile() {
        RunState s = combatState();
        s.getActiveCompanion().placeAt(26, 25); // east of the player
        int clock = s.getClockTurns();

        new TurnEngine().advance(s, PlayerAction.move(1, 0, RoguePlayer.EAST));

        assertEquals(26, s.getPlayer().getTileX(), "an ally is walkable-over — the party shares tiles");
        assertEquals(25, s.getPlayer().getTileY());
        assertEquals(clock + 1, s.getClockTurns(), "the move commits a turn");
    }

    @Test
    void alertedEnemyChaseStopsAtTheCompanionsTile() {
        RunState s = combatState();
        s.getActiveCompanion().placeAt(26, 25);
        RogueEnemy e = alerted(s, 28, 25); // two east of the player, one east of Aldric

        // Turn 1: the enemy closes to one east of Aldric (27,25).
        new TurnEngine().advance(s, PlayerAction.wait(0));
        assertEquals(27, e.getTileX(), "turn 1: the enemy closes toward the player");
        assertEquals(25, e.getTileY());

        // Turn 2: the only remaining step is Aldric's tile — blocked. It stops, never sharing.
        new TurnEngine().advance(s, PlayerAction.wait(0));
        assertEquals(27, e.getTileX(), "turn 2: the enemy cannot occupy Aldric's tile");
        assertEquals(25, e.getTileY());
    }

    @Test
    void unawareWanderNeverStepsOntoThePlayerOrCompanion() {
        RogueTileMap m = new RogueTileMap(10, 10);
        m.fill(RogueTile.FLOOR);
        RogueEnemy e = new RogueEnemy(5, 3, m);
        // (5,4) is the player's tile (avoided) and (5,2) the companion's (blocked). Hundreds of
        // wander rolls must never land on either.
        Random rng = new Random(1L);
        for (int i = 0; i < 500; i++) {
            e.wander(rng, 5, 4, 5, 2);
            assertFalse(e.getTileX() == 5 && e.getTileY() == 4, "never the player's tile");
            assertFalse(e.getTileX() == 5 && e.getTileY() == 2, "never the companion's tile");
        }
    }

    // --- #1: Aldric fights and can be hurt ---

    @Test
    void companionStrikesAnAdjacentEnemyInsteadOfFollowing() {
        RunState s = combatState();
        Companion c = s.getActiveCompanion();
        c.placeAt(26, 25);
        RogueEnemy e = alerted(s, 27, 25); // adjacent to Aldric
        int eHp = e.getHp();

        new TurnEngine().advance(s, PlayerAction.wait(0));

        assertTrue(e.getHp() < eHp, "Aldric damaged the enemy");
        assertEquals(26, c.getTileX(), "Aldric held his ground — he did not trail the player");
        assertEquals(25, c.getTileY());
        assertTrue(s.getMessageLog().stream().anyMatch(m -> m.contains("Aldric strikes")),
                "Aldric's strike is observed: " + s.getMessageLog());
    }

    @Test
    void companionStillFollowsWhenNoEnemyIsInRange() {
        RunState s = combatState();
        Companion c = s.getActiveCompanion();
        c.placeAt(29, 25); // east of the player, well away from the fight
        alerted(s, 27, 27);

        new TurnEngine().advance(s, PlayerAction.wait(0));

        assertEquals(28, c.getTileX(), "an un-engaged Aldric closes the gap toward the player");
        assertEquals(25, c.getTileY());
    }

    @Test
    void alertedEnemyAttacksTheCompanionInsteadOfWalkingPastHim() {
        RunState s = combatState();
        Companion c = s.getActiveCompanion();
        c.placeAt(26, 25);
        alerted(s, 27, 25); // adjacent to Aldric, two from the player
        int cHp = c.getHp();
        int pHp = s.getPlayer().getHp();

        new TurnEngine().advance(s, PlayerAction.wait(0));

        assertTrue(c.getHp() < cHp, "Aldric takes the hit — he's the enemy's closest target");
        assertEquals(pHp, s.getPlayer().getHp(), "the player is out of the enemy's reach");
        assertTrue(s.getMessageLog().stream().anyMatch(m -> m.contains("Aldric is hit")),
                "Aldric's wound is observed: " + s.getMessageLog());
    }

    @Test
    void alertedEnemyStillAttacksThePlayerWhenAdjacentToHim() {
        RunState s = combatState();
        Companion c = s.getActiveCompanion();
        c.placeAt(28, 25); // far from the enemy — no companion interference
        alerted(s, 26, 25); // adjacent to the player
        int cHp = c.getHp();
        int pHp = s.getPlayer().getHp();

        new TurnEngine().advance(s, PlayerAction.wait(0));

        boolean struckOrDodged = s.getPlayer().getHp() < pHp
                || s.getMessageLog().stream().anyMatch(m -> m.equals("Dodge!"));
        assertTrue(struckOrDodged, "the player is struck or dodges — the prime target stays the player");
        assertEquals(cHp, c.getHp(), "Aldric is not hit — the player was in the way");
    }

    @Test
    void companionCanFallAndIsThenIgnored() {
        RunState s = combatState();
        Companion c = s.getActiveCompanion();
        c.placeAt(26, 25);
        c.takeDamage(12); // 14 -> 2: one soldier's hit finishes him
        RogueEnemy e = alerted(s, 27, 25);

        new TurnEngine().advance(s, PlayerAction.wait(0));

        assertFalse(c.isAlive(), "Aldric falls in combat");
        assertEquals(0, c.getHp());
        assertTrue(s.getMessageLog().stream().anyMatch(m -> m.contains("Aldric falls")),
                "his fall is observed: " + s.getMessageLog());

        // A second soldier arrives; the corpse is neither blocked on nor attacked. Check only the
        // turn's own lines (turn 1 legitimately contains the killing blow).
        int logSize = s.getMessageLog().size();
        alerted(s, 29, 25);
        new TurnEngine().advance(s, PlayerAction.wait(0));
        assertEquals(0, c.getHp(), "a dead Aldric takes no further damage");
        List<String> after = new ArrayList<>(s.getMessageLog().subList(logSize, s.getMessageLog().size()));
        assertFalse(after.stream().anyMatch(m -> m.contains("Aldric is hit")),
                "no one hits a corpse: " + after);
    }
}

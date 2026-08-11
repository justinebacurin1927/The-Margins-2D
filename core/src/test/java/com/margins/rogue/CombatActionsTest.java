package com.margins.rogue;

import com.margins.rogue.item.Supply;
import com.margins.rogue.state.RunState;
import com.margins.rogue.system.PlayerAction;
import com.margins.rogue.system.TurnEngine;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Story 4.1 (FR-12, AD-4): combat resolves at the actor's point via CombatSystem. Pins the
 * turn-order-by-AG rule, the five-action set (Attack/Block/Dodge/Use Item/Flee), the
 * dead-before-act invariant, and the single-CombatSystem-authority guard. All driven through the
 * real turn pipeline (the combat-fix test pattern).
 */
class CombatActionsTest {

    /** A run with the player on cleared open floor and no ambient enemies — full control. */
    private RunState combatState(long seed) {
        RunState s = new RunState(seed);
        RogueTileMap m = s.getTileMap();
        s.getPlayer().placeAt(25, 25);
        for (int x = 19; x <= 33; x++)
            for (int y = 19; y <= 33; y++)
                m.setTile(x, y, RogueTile.FLOOR);
        s.getEnemies().clear();
        return s;
    }

    /** Park the companion well away so his own follow/attack can't skew an attack-or-move assertion. */
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

    /** First index of a log line matching the predicate, or -1. */
    private static int indexOfAny(List<String> log, Predicate<String> pred) {
        for (int i = 0; i < log.size(); i++) if (pred.test(log.get(i))) return i;
        return -1;
    }

    // --- Task 1: AG is a real stat; dodge is AG-derived (AC-3, D3) ---

    @Test
    void dodgeIsDerivedFromTheRealAgStat() {
        // D3: AG=7 reproduces the old instinct=7 value EXACTLY (21%) — the refactor is
        // value-preserving, AD-5-safe. A raised AG raises the dodge.
        RunState s = combatState(42L);
        assertEquals(21, s.getPlayer().dodgePercent(), "AG 7 → the exact old 21% base dodge (D3)");
        s.getPlayer().setAg(10);
        assertEquals(30, s.getPlayer().dodgePercent(), "dodge scales with the real AG stat");
    }

    // --- Task 2: turn order by AG (AC-1, D3) ---

    @Test
    void higherAgEnemyActsFirst() {
        // AC-1 (FR-12): the enemy phase runs highest-AG first. The companion is placed ADJACENT to
        // the fast (player-adjacent) enemy so he strikes it and holds his ground during follow —
        // staying put for the slow enemy to strike in the enemy phase. In the enemy phase the
        // AG-9 enemy's player line must precede the AG-3 enemy's Aldric line.
        RunState s = combatState(42L);
        s.getActiveCompanion().placeAt(26, 26); // adjacent to the fast enemy below
        RogueEnemy fast = alerted(s, 25, 26);   // north, adjacent to the player
        fast.setAg(9);
        alerted(s, 27, 26);                     // east of the companion only; default AG 3

        new TurnEngine().advance(s, PlayerAction.wait(0));

        List<String> log = s.getMessageLog();
        int playerLine = indexOfAny(log, m -> m.equals("Dodge!") || m.startsWith("Hit for"));
        int aldricLine = indexOfAny(log, m -> m.startsWith("Aldric is hit"));
        assertTrue(playerLine >= 0 && aldricLine >= 0, "both enemies acted this turn: " + log);
        assertTrue(playerLine < aldricLine, "the AG-9 enemy acts before the AG-3 enemy: " + log);
    }

    @Test
    void equalAgPreservesInsertionOrder() {
        // D3: the AG sort is stable — two equal-AG enemies keep insertion order (the first-added
        // acts first). If the sort reordered, the second-added's line would precede the first's.
        // Same companion-hold setup as higherAgEnemyActsFirst (companion strikes the first-added,
        // player-adjacent enemy and holds; the second-added strikes Aldric in the enemy phase).
        RunState s = combatState(42L);
        s.getActiveCompanion().placeAt(26, 26); // adjacent to the first-added enemy
        alerted(s, 25, 26);                     // first-added, adjacent to the player — default AG 3
        alerted(s, 27, 26);                     // second-added, adjacent to the companion — default AG 3

        new TurnEngine().advance(s, PlayerAction.wait(0));

        List<String> log = s.getMessageLog();
        int playerLine = indexOfAny(log, m -> m.equals("Dodge!") || m.startsWith("Hit for"));
        int aldricLine = indexOfAny(log, m -> m.startsWith("Aldric is hit"));
        assertTrue(playerLine >= 0 && aldricLine >= 0, "both enemies acted this turn: " + log);
        assertTrue(playerLine < aldricLine, "equal-AG keeps insertion order — the first-added acts first: " + log);
    }

    // --- Task 3: the five-action set (AC-1) ---

    @Test
    void blockActionBracesAndCommitsATurn() {
        // The BLOCK action already exists core-side (TurnEngine "Brace!"). The H key is the screen
        // binding (Task 5); here we pin the core: bracing commits a turn and the brace holds until
        // a hit consumes it.
        RunState s = combatState(42L);
        parkCompanion(s);
        int clock = s.getClockTurns();

        new TurnEngine().advance(s, PlayerAction.block(RoguePlayer.NORTH));

        assertEquals(clock + 1, s.getClockTurns(), "bracing commits a turn");
        assertTrue(s.getPlayer().isBlocking(), "the brace holds until a hit consumes it");
        assertTrue(s.getMessageLog().stream().anyMatch(m -> m.equals("Brace!")),
                "bracing is announced: " + s.getMessageLog());
    }

    @Test
    void dodgeActionBoostsEvasionThatTurn() {
        // AC-1 (FR-12): a Dodge turn evades a hit that a non-Dodge turn at the SAME seed takes —
        // the boosted chance (min 90, 21%×2 = 42%) catches the roll that the base 21% missed. Both
        // runs draw an identical rng stream up to the enemy's dodge roll (no rng earlier in the
        // acted pipeline), so the search is deterministic: find a seed whose first combat roll
        // lands in [21, 42).
        for (long seed = 1; seed < 5000; seed++) {
            RunState waitRun = combatState(seed);
            parkCompanion(waitRun);
            alerted(waitRun, 25, 26);
            new TurnEngine().advance(waitRun, PlayerAction.wait(0));
            boolean baseDodged = waitRun.getMessageLog().stream().anyMatch(m -> m.equals("Dodge!"));

            RunState dodgeRun = combatState(seed);
            parkCompanion(dodgeRun);
            alerted(dodgeRun, 25, 26);
            new TurnEngine().advance(dodgeRun, PlayerAction.dodge(RoguePlayer.EAST));
            boolean boostedDodged = dodgeRun.getMessageLog().stream().anyMatch(m -> m.equals("Dodge!"));

            if (!baseDodged && boostedDodged) {
                assertTrue(true, "seed " + seed + ": boosted dodge catches a hit the base dodge missed");
                return;
            }
        }
        fail("no seed found where the boosted dodge exceeds the base dodge (the [21,42) roll band is 21% — should find one in a few tries)");
    }

    @Test
    void dodgeActionIsAOneTurnEffectClearedAfterTheTurn() {
        // AC-1 (FR-12): the Dodge action's evasion lasts exactly one turn — TurnEngine clears it
        // after the enemy phase, so the next turn dodges at the base rate again.
        RunState s = combatState(42L);
        parkCompanion(s);
        alerted(s, 25, 26);
        int clock = s.getClockTurns();

        new TurnEngine().advance(s, PlayerAction.dodge(RoguePlayer.EAST));

        assertEquals(clock + 1, s.getClockTurns(), "the dodge commits a turn");
        assertTrue(s.getMessageLog().stream().anyMatch(m -> m.equals("You sidestep, ready.")),
                "the dodge is announced: " + s.getMessageLog());
        assertFalse(s.getPlayer().isEvading(), "evasion is a one-turn effect — cleared after the enemy phase");
    }

    @Test
    void useItemInCombatCommitsTurnAndTheEnemyPhaseStillRuns() {
        // AC-1 (FR-12): Use Item is a real combat action — using a provision with a hostile
        // adjacent commits the turn AND the enemy phase still resolves (the enemy strikes or is
        // dodged after the use).
        RunState s = combatState(42L);
        parkCompanion(s);
        s.getInventory().tryAdd(Supply.PRESERVED_FOOD.ordinal(), 1); // a safe, no-roll eatable
        int clock = s.getClockTurns();
        int pHp = s.getPlayer().getHp();
        alerted(s, 25, 26);

        new TurnEngine().advance(s, PlayerAction.use(Supply.PRESERVED_FOOD.ordinal(), RoguePlayer.NORTH));

        assertEquals(clock + 1, s.getClockTurns(), "using an item in combat commits the turn");
        assertEquals(0, s.getInventory().count(Supply.PRESERVED_FOOD.ordinal()), "the provision was consumed");
        boolean struckOrDodged = s.getPlayer().getHp() < pHp
                || s.getMessageLog().stream().anyMatch(m -> m.equals("Dodge!"));
        assertTrue(struckOrDodged, "the enemy phase still runs — the adjacent enemy strikes or is dodged");
    }

    @Test
    void fleeMovesToTheStrictlyFarthestTileAndCommitsTurn() {
        // AC-1 (FR-12): Flee moves to the walkable cardinal neighbor that STRICTLY maximizes
        // distance to the nearest living enemy (tie-break by dir order). Nearest enemy is the east
        // one (dist 2); north and west both reach dist 3, north (dir 1) wins the tie. South and
        // east are rejected because they do not strictly increase the distance.
        RunState s = combatState(42L);
        parkCompanion(s);
        alerted(s, 27, 25); // east, distance 2 — the nearest
        alerted(s, 25, 23); // north-north, distance 3
        int clock = s.getClockTurns();

        new TurnEngine().advance(s, PlayerAction.flee(RoguePlayer.EAST));

        assertEquals(25, s.getPlayer().getTileX(), "flee lands north");
        assertEquals(26, s.getPlayer().getTileY(), "flee lands north — the strictly farthest, dir-1 tie");
        assertEquals(clock + 1, s.getClockTurns(), "a successful flee commits the turn");
        assertTrue(s.getMessageLog().stream().anyMatch(m -> m.equals("You flee!")),
                "the flee is announced: " + s.getMessageLog());
    }

    @Test
    void fleeBoxedInRefusesWithoutSpendingATurn() {
        // AC-1 (FR-12): boxed in on all four cardinals, Flee refuses — no move, no turn (the
        // inert-USE / wall-bump precedent, AD-5).
        RunState s = combatState(42L);
        parkCompanion(s);
        alerted(s, 25, 26);
        alerted(s, 25, 24);
        alerted(s, 24, 25);
        alerted(s, 26, 25);
        int clock = s.getClockTurns();
        int px = s.getPlayer().getTileX(), py = s.getPlayer().getTileY();

        new TurnEngine().advance(s, PlayerAction.flee(RoguePlayer.NORTH));

        assertEquals(px, s.getPlayer().getTileX(), "the player stays put");
        assertEquals(py, s.getPlayer().getTileY(), "the player stays put");
        assertEquals(clock, s.getClockTurns(), "a refused flee spends no turn (AD-5)");
        assertTrue(s.getMessageLog().stream().anyMatch(m -> m.equals("No way out!")),
                "the refusal is announced: " + s.getMessageLog());
    }

    // --- Task 4: dead-before-act + single-authority pins (AC-2) ---

    @Test
    void deadEnemyNeverActsLaterThatTurn() {
        // AC-2 (AD-4): a kill in the acted step means the dead enemy never acts later that turn —
        // the enemy phase skips corpses. The AG sort must not regress this.
        RunState s = combatState(42L);
        parkCompanion(s);
        RogueEnemy e = alerted(s, 25, 26); // adjacent north
        e.takeDamage(3);                   // 8 -> 5: the player's single strike (str 5) finishes it
        int pHp = s.getPlayer().getHp();

        new TurnEngine().advance(s, PlayerAction.attack(RoguePlayer.NORTH));

        assertFalse(e.isAlive(), "the attack killed the enemy");
        assertEquals(pHp, s.getPlayer().getHp(), "the dead enemy never acted — no HP loss");
        assertTrue(s.getMessageLog().stream().anyMatch(m -> m.startsWith("Hit!")),
                "the killing strike is announced: " + s.getMessageLog());
        assertTrue(s.getMessageLog().contains("Enemy defeated."),
                "a killing blow emits the dedicated red-feed event: " + s.getMessageLog());
        assertFalse(s.getMessageLog().stream().anyMatch(m -> m.equals("Dodge!") || m.startsWith("Hit for")),
                "no attack line from the dead enemy: " + s.getMessageLog());
    }

    @Test
    void combatDamageRoutesOnlyThroughCombatSystem() {
        // AC-2's "by construction" half: TurnEngine (the acted-step router) carries NO combat-HP
        // mutation method of its own — the only combat HP mutation lives in CombatSystem. The name
        // guard mirrors the 3.5 AC-1 no-leveling-field pattern: it catches a regression that sneaks
        // an HP-mutating helper into the router, the exact silent-mutation failure mode the retro
        // flagged. (The behavioral half is deadEnemyNeverActsLaterThatTurn.)
        for (Method m : TurnEngine.class.getDeclaredMethods()) {
            String n = m.getName().toLowerCase();
            assertFalse(n.contains("damage") || n.contains("hurtraw") || n.contains("revive")
                            || n.contains("sethp") || n.contains("heal") || n.contains("applyhit"),
                    "TurnEngine mutates combat HP outside CombatSystem (AC-2): " + m.getName());
        }
    }
}

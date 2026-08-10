package com.margins.rogue;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import com.margins.rogue.Companion;
import com.margins.rogue.RogueEnemy;
import com.margins.rogue.item.FloorItem;
import com.margins.rogue.item.Supply;
import com.margins.rogue.state.FlagStore;
import com.margins.rogue.state.RunState;
import com.margins.rogue.system.HazardSystem;
import com.margins.rogue.system.PlayerAction;
import com.margins.rogue.system.TurnEngine;
import com.margins.rogue.system.TurnResult;
import com.margins.rogue.world.WorldSpine;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Story 3.3 (FR-10, AC-1/2/3): the foray loop end to end — the derived foray framing (Task 1),
 * the day/night flip log lines + HUD budget readout (Task 2), the generic night-overlay with its
 * 3.4 seam (Task 3), and the loot-carried-back arc (Task 4). All derivations are position +
 * clockTurns + constants (AD-6 — no new persisted field); the AC pins are turn-level assertions,
 * deterministic on a fixed seed.
 */
class ForayLoopTest {

    /** A fresh run with the weather pinned to CLEAR, so acted turns never take temperature harm
     *  and the HP deltas in the night/hazard tests come only from the step risk under test. */
    private static RunState clear(long seed) {
        RunState s = new RunState(seed);
        s.setWeather(Weather.CLEAR);
        return s;
    }

    // --- Task 1: the foray derivation + safe-point query (AC: 1) ---

    @Test
    void onForayIsFalseAtTheSpawnAndTrueInTheInterior() {
        RunState s = clear(42L);
        RogueTileMap m = s.getTileMap();
        WorldSpine spine = new WorldSpine(m.getWidth(), m.getHeight());
        assertTrue(spine.eastness(s.getPlayer().getTileX()) <= RunState.SAFE_TIER_EASTNESS,
                "the seed-42 spawn sits inside the safe home cluster (eastness ≤ 0.2)");
        assertFalse(s.onForay(), "at the spawn (the home cluster) Klein is not on a foray");

        // A mid-map wilderness position is beyond the safe tier → out on a foray.
        s.getPlayer().placeAt(m.getWidth() / 2, m.getHeight() / 2);
        assertTrue(spine.eastness(m.getWidth() / 2) > RunState.SAFE_TIER_EASTNESS,
                "mid-map is east of the safe tier");
        assertTrue(s.onForay(), "mid-map Klein is out on a foray");

        // Back west (inside the home cluster: x = 1/6 of the width → eastness ≤ 0.2) → safe again.
        int westX = spine.tileX(WorldSpine.CORNEO_X);
        s.getPlayer().placeAt(westX, m.getHeight() / 2);
        assertTrue(spine.eastness(westX) <= RunState.SAFE_TIER_EASTNESS);
        assertFalse(s.onForay(), "returning inside the home cluster ends the foray");
    }

    @Test
    void aBuiltCampfireIsASafePoint() {
        RunState s = clear(42L);
        int midX = s.getTileMap().getWidth() / 2;
        int y = s.getPlayer().getTileY();
        s.getPlayer().placeAt(midX, y);
        assertTrue(s.onForay(), "mid-map before a campfire, Klein is out on a foray");

        // Build the campfire at his tile: the "camp" case (Decision 1) — standing within its
        // radius is at camp, not on a foray, even far from the home cluster.
        s.setCampfire(midX, y);
        assertFalse(s.onForay(), "within the campfire radius, Klein is at camp, not on a foray");

        s.getPlayer().placeAt(midX + RunState.CAMPFIRE_SAFE_RADIUS + 1, y);
        assertTrue(s.onForay(), "beyond the campfire radius, Klein is back on a foray");
    }

    @Test
    void turnsUntilNightfallAndDayCountDeriveAtCycleBoundaries() {
        // Drive the clock the honest way — acted turns (WAIT) through the real pipeline (AD-4),
        // so the derivations are pinned turn-level, not formulaically.
        RunState s = clear(42L);
        TurnEngine engine = new TurnEngine();
        assertEquals(100, s.turnsUntilNightfall(), "Day 0 opens with the full 100-turn budget");
        assertEquals(0, s.dayNumber(), "Day 0 is the first cycle");

        for (int i = 0; i < 50; i++) engine.advance(s, PlayerAction.wait(RoguePlayer.SOUTH));
        assertEquals(50, s.getClockTurns());
        assertEquals(50, s.turnsUntilNightfall(), "mid-day the budget is what remains");
        assertEquals(0, s.dayNumber(), "turn 50 is still cycle 0");

        for (int i = 0; i < 49; i++) engine.advance(s, PlayerAction.wait(RoguePlayer.SOUTH));
        assertEquals(99, s.getClockTurns());
        assertEquals(1, s.turnsUntilNightfall(), "turn 99 is the last Day turn of cycle 0");

        engine.advance(s, PlayerAction.wait(RoguePlayer.SOUTH)); // clock 100 → Night
        assertEquals(0, s.turnsUntilNightfall(), "in Night the day budget is spent (0)");
        assertEquals(0, s.dayNumber(), "turn 100 is still cycle 0");

        for (int i = 0; i < 69; i++) engine.advance(s, PlayerAction.wait(RoguePlayer.SOUTH));
        assertEquals(169, s.getClockTurns());
        engine.advance(s, PlayerAction.wait(RoguePlayer.SOUTH)); // clock 170 → Day of cycle 1
        assertEquals(100, s.turnsUntilNightfall(), "a new cycle restores the full budget");
        assertEquals(1, s.dayNumber(), "turn 170 begins cycle 1");
    }

    // --- Task 2: the day/night flip log lines (AC: 2) ---

    @Test
    void flipLinesEmitExactlyOnTheBoundaryTurns() {
        // The flip lines are emitted by the acted-turn pipeline (AD-4) like the Weather onsetLine
        // (Story 1.3): exactly on the DAY→NIGHT turn (clock 100) and the NIGHT→DAY turn (clock
        // 170), and on NO other turn across the whole cycle.
        RunState s = clear(42L);
        TurnEngine engine = new TurnEngine();
        for (int turn = 1; turn <= 170; turn++) {
            TurnResult r = engine.advance(s, PlayerAction.wait(RoguePlayer.SOUTH));
            assertEquals(turn == 100, r.messages.contains(RunState.LINE_DUSK),
                    "Dusk falls exactly on turn 100 (saw it on turn " + turn + ")");
            assertEquals(turn == 170, r.messages.contains(RunState.LINE_DAWN),
                    "Dawn breaks exactly on turn 170 (saw it on turn " + turn + ")");
        }
        assertEquals(1, s.dayNumber(), "the full walk lands at Day 1 with a fresh budget");
        assertEquals(100, s.turnsUntilNightfall(), "a fresh day's budget restored after the cycle");
    }

    @Test
    void aRefusedTurnNeverEmitsAFlipLine() {
        // A refused (un-acted) turn commits nothing (AD-5) — so it can never trip the day/night
        // boundary, and no flip line may appear on it.
        RunState s = clear(42L);
        TurnEngine engine = new TurnEngine();
        for (int i = 0; i < 99; i++) engine.advance(s, PlayerAction.wait(RoguePlayer.SOUTH));
        assertEquals(99, s.getClockTurns(), "turn 99: the last Day turn of cycle 0");

        // Put the player against a wall so the boundary turn is REFUSED rather than acted.
        int[] spot = wallLeft(s.getTileMap());
        assertNotNull(spot, "the map has a walkable cell with a wall to the west");
        s.getPlayer().placeAt(spot[0], spot[1]);
        int clock = s.getClockTurns();
        TurnResult r = engine.advance(s, PlayerAction.move(-1, 0, RoguePlayer.WEST));
        assertFalse(r.messages.contains(RunState.LINE_DUSK),
                "a wall bump (refused) must not emit the flip line: " + r.messages);
        assertEquals(clock, s.getClockTurns(), "the refused move spends no turn — still turn 99 Day");
        assertTrue(s.isDay(), "the phase did not flip on a refused turn");
    }

    // --- Task 3: AC-3's night overlay on the return leg (the concrete overreach) ---

    @Test
    void nightWithoutLightRisksTheStumbleAndAd5ReproducesIt() {
        // AC-3 (UJ-2): at night without light, stepping risks a stumble in the dark (a small HP
        // cost + a log line) — the return leg is more dangerous. Deterministic on a fixed seed
        // (AD-5): two same-seed runs must land the same HP on the same step.
        long seed = 7L;
        TurnEngine engine = new TurnEngine();
        RunState a = clear(seed), b = clear(seed);
        driveToNight(a);
        driveToNight(b);
        int[] ca = wildernessCellAndNeighbor(a.getTileMap());
        int[] cb = wildernessCellAndNeighbor(b.getTileMap());
        a.getPlayer().placeAt(ca[0], ca[1]);
        b.getPlayer().placeAt(cb[0], cb[1]);
        a.getEnemies().clear(); b.getEnemies().clear();
        a.getCompanions().clear(); b.getCompanions().clear();
        boolean fired = false;
        for (int i = 0; i < 40 && !fired; i++) {
            int dx = (i % 2 == 0 ? ca[2] : -ca[2]);
            int dy = (i % 2 == 0 ? ca[3] : -ca[3]);
            int before = a.getPlayer().getHp();
            TurnResult ra = engine.advance(a, PlayerAction.move(dx, dy, RoguePlayer.directionOf(dx, dy)));
            int dropped = before - a.getPlayer().getHp();
            assertTrue(dropped == 0 || dropped == 1,
                    "a night wilderness step drops only the night stumble (0 or 1 HP; got " + dropped + ")");
            assertEquals(dropped == 1, ra.messages.contains(HazardSystem.NIGHT_STUMBLE_MESSAGE),
                    "the stumble's damage and its log line come together");
            if (dropped == 1) fired = true;
            engine.advance(b, PlayerAction.move(dx, dy, RoguePlayer.directionOf(dx, dy)));
            assertEquals(a.getPlayer().getHp(), b.getPlayer().getHp(),
                    "seed " + seed + ": same seed reproduces the same night-stumble outcome (AD-5)");
        }
        assertTrue(fired, "night without light risks the stumble within 40 steps on seed " + seed);
    }

    @Test
    void atDayTheNightOverlayNeverFires() {
        RunState s = clear(42L); // clock 0 → Day
        int[] cell = wildernessCellAndNeighbor(s.getTileMap());
        s.getPlayer().placeAt(cell[0], cell[1]);
        s.getEnemies().clear(); s.getCompanions().clear();
        TurnEngine engine = new TurnEngine();
        for (int i = 0; i < 20; i++) {
            int dx = (i % 2 == 0 ? cell[2] : -cell[2]);
            int dy = (i % 2 == 0 ? cell[3] : -cell[3]);
            int hp = s.getPlayer().getHp();
            TurnResult r = engine.advance(s, PlayerAction.move(dx, dy, RoguePlayer.directionOf(dx, dy)));
            assertEquals(hp, s.getPlayer().getHp(), "a day step takes no night damage");
            assertFalse(r.messages.contains(HazardSystem.NIGHT_STUMBLE_MESSAGE),
                    "the night stumble never fires in daylight");
        }
        assertTrue(s.isDay(), "20 steps stay within the day (no boundary crossed)");
    }

    @Test
    void aLitTorchSuppressesTheNightStumble() {
        // Decision 4: light is the counter — a lit torch means you can see the ground, so the
        // night overlay never fires while it lasts. The cost is real (60-turn burn + Wood+Coal).
        RunState s = clear(7L);
        driveToNight(s);
        int[] cell = wildernessCellAndNeighbor(s.getTileMap());
        s.getPlayer().placeAt(cell[0], cell[1]);
        s.getEnemies().clear(); s.getCompanions().clear();
        s.lightTorch(60);
        TurnEngine engine = new TurnEngine();
        for (int i = 0; i < 30; i++) {
            int dx = (i % 2 == 0 ? cell[2] : -cell[2]);
            int dy = (i % 2 == 0 ? cell[3] : -cell[3]);
            int hp = s.getPlayer().getHp();
            TurnResult r = engine.advance(s, PlayerAction.move(dx, dy, RoguePlayer.directionOf(dx, dy)));
            assertEquals(hp, s.getPlayer().getHp(), "a torch keeps the night path visible — no stumble");
            assertFalse(r.messages.contains(HazardSystem.NIGHT_STUMBLE_MESSAGE));
        }
        assertTrue(s.getTorchTurns() > 0, "the torch still burns after 30 steps (60-turn burn)");
    }

    @Test
    void theNightOverlayStacksWithTheAuthoredHazard() {
        // Task 3 + Task 5's seam pin: at night without light, a STRUCTURE step risks BOTH the
        // generic night stumble AND the structure's authored hazard (they stack — a dark structure
        // is doubly dangerous), and the base hazard still resolves on the night path (3.3's seam
        // passes through to the authored hazard; 3.4 authors per-location overrides there).
        RunState s = clear(11L);
        driveToNight(s);
        int[] cell = walkableCellAndNeighbor(s.getTileMap(), RogueTileMap.STRUCTURE_HUNTERS_BLIND);
        s.getPlayer().placeAt(cell[0], cell[1]);
        s.getEnemies().clear(); s.getCompanions().clear();
        TurnEngine engine = new TurnEngine();
        boolean stacked = false;
        for (int i = 0; i < 60; i++) {
            int dx = (i % 2 == 0 ? cell[2] : -cell[2]);
            int dy = (i % 2 == 0 ? cell[3] : -cell[3]);
            int before = s.getPlayer().getHp();
            engine.advance(s, PlayerAction.move(dx, dy, RoguePlayer.directionOf(dx, dy)));
            int dropped = before - s.getPlayer().getHp();
            assertTrue(dropped == 0 || dropped == 1 || dropped == 2,
                    "a night structure step drops only night (1) + hazard (1): got " + dropped);
            if (dropped == 2) stacked = true; // one step landing both proves they stack
        }
        assertTrue(stacked,
                "some night structure step lands BOTH the stumble and the hazard (they stack)");
    }

    @Test
    void theNightOverlayAddsNoExtraClockTick() {
        RunState s = clear(7L);
        driveToNight(s);
        int[] cell = wildernessCellAndNeighbor(s.getTileMap());
        s.getPlayer().placeAt(cell[0], cell[1]);
        s.getEnemies().clear(); s.getCompanions().clear();
        int clock = s.getClockTurns();
        new TurnEngine().advance(s,
                PlayerAction.move(cell[2], cell[3], RoguePlayer.directionOf(cell[2], cell[3])));
        assertEquals(clock + 1, s.getClockTurns(),
                "one acted step = one clock tick — the overlay never adds a tick");
    }

    // --- Task 4: the AC-1 carry-back pin (the loot-carried-back arc) ---

    @Test
    void aDayForayCarriesTheLootBackToTheSafePoint() {
        // AC-1 (FR-10): from a safe point in daylight, travel east to a World-Structure, scavenge a
        // guaranteed-authored item under its hazard, and return — the item rides the inventory home
        // across the ONE continuous region (AD-8). Every step is a MOVE on the SAME tileMap (the
        // codebase has no floor-transition/descend path — retired pre-AD-8), so "one continuous
        // traversal, no floor transitions" holds by construction; the walk is asserted to actually
        // leave the safe point and come back, and to spend exactly one day-turn budget.
        RunState s = clear(42L);
        RogueTileMap m = s.getTileMap();
        WorldSpine spine = new WorldSpine(m.getWidth(), m.getHeight());

        int homeX = s.getPlayer().getTileX();
        int homeY = s.getPlayer().getTileY();
        assertTrue(spine.eastness(homeX) <= RunState.SAFE_TIER_EASTNESS, "the arc starts in the safe cluster");
        assertFalse(s.onForay(), "the arc starts at a safe point");
        assertTrue(s.isDay(), "the arc starts in daylight");

        // The target: Hunter's Blind's guaranteed authored ROPE (non-scatterable — only the
        // authored pass can place it, 3.2 P1 proof pattern) on a walkable footprint cell.
        int[] target = ropeCellIn(m, s, RogueTileMap.STRUCTURE_HUNTERS_BLIND);
        assertNotNull(target, "the authored ROPE sits on a walkable Hunter's Blind cell (seed 42)");

        // Travel east (only MOVE actions; enemies + companion cleared so the walk is deterministic
        // — occupancy can't wedge a step the BFS planned).
        s.getEnemies().clear();
        s.getCompanions().clear();
        List<int[]> outbound = path(m, homeX, homeY, target[0], target[1]);
        assertNotNull(outbound, "a walkable path connects the safe point to the structure (one region)");
        int cx = homeX, cy = homeY, maxEast = homeX;
        boolean onForaySeen = false;
        for (int[] step : outbound) {
            cx += step[0];
            cy += step[1];
            new TurnEngine().advance(s,
                    PlayerAction.move(step[0], step[1], RoguePlayer.directionOf(step[0], step[1])));
            assertEquals(cx, s.getPlayer().getTileX(), "the travel leg lands where the path says");
            assertEquals(cy, s.getPlayer().getTileY(), "the travel leg lands where the path says");
            maxEast = Math.max(maxEast, cx);
            if (s.onForay()) onForaySeen = true;
        }
        assertEquals(target[0], s.getPlayer().getTileX(), "Klein stands on the structure's loot cell");
        assertTrue(onForaySeen, "the arc leaves the safe point (out on a foray mid-travel)");
        assertTrue(spine.eastness(maxEast) > RunState.SAFE_TIER_EASTNESS, "the arc travels east of the safe tier");

        // Scavenge: pick up until the ROPE is in the backpack (a generic-scatter stack could share
        // the cell — takeItemAt returns the first stack there; each pickup commits one acted turn).
        int pickups = 0;
        while (s.getInventory().count(Supply.ROPE.ordinal()) == 0 && pickups < 4) {
            new TurnEngine().advance(s, PlayerAction.pickup(RoguePlayer.EAST));
            pickups++;
        }
        assertEquals(1, s.getInventory().count(Supply.ROPE.ordinal()), "scavenged the authored ROPE");

        // Return to the safe point (only MOVE actions).
        List<int[]> returnPath = path(m, s.getPlayer().getTileX(), s.getPlayer().getTileY(), homeX, homeY);
        assertNotNull(returnPath, "a walkable path connects the structure back to the safe point");
        for (int[] step : returnPath) {
            cx += step[0];
            cy += step[1];
            new TurnEngine().advance(s,
                    PlayerAction.move(step[0], step[1], RoguePlayer.directionOf(step[0], step[1])));
            assertEquals(cx, s.getPlayer().getTileX(), "the return leg lands where the path says");
            assertEquals(cy, s.getPlayer().getTileY(), "the return leg lands where the path says");
        }
        assertEquals(homeX, s.getPlayer().getTileX(), "Klein is back at the safe point");
        assertEquals(homeY, s.getPlayer().getTileY(), "Klein is back at the safe point");
        assertFalse(s.onForay(), "back at the safe point, Klein is no longer on a foray");
        assertEquals(1, s.getInventory().count(Supply.ROPE.ordinal()),
                "the scavenged ROPE rides the inventory home across the one continuous region (AC-1)");

        // "One continuous traversal, no floor transitions": the arc traversed only MOVE steps on
        // the SAME tileMap — the map reference is unchanged from first step to last, and the code
        // has no descend/floor-transition path to call. AC-2's one-budget pin: the clock spent
        // exactly the arc's acted turns (travel + scavenge + return draw from one budget).
        assertSame(m, s.getTileMap(), "the arc never left the one continuous region");
        assertEquals(outbound.size() + pickups + returnPath.size(), s.getClockTurns(),
                "one budget: travel + scavenge + return spent exactly that many acted turns");
    }

    // --- Task 5: AC pins + no-forced-scope (AC: all) ---

    @Test
    void theForayDerivationsNeedNoPersistedState() {
        // AD-6: on-foray / turns-until-nightfall / day-number derive from position + clockTurns +
        // constants — the save JSON carries NO foray state, and the derived queries agree before
        // and after a save/load (the persisted clockTurns + position + campfire drive them).
        RunState s = clear(42L);
        TurnEngine engine = new TurnEngine();
        for (int i = 0; i < 25; i++) engine.advance(s, PlayerAction.wait(RoguePlayer.SOUTH));
        int[] cell = wildernessCellAndNeighbor(s.getTileMap());
        s.getPlayer().placeAt(cell[0], cell[1]); // mid-map — now out on a foray
        s.setCampfire(s.getPlayer().getTileX() - 1, s.getPlayer().getTileY()); // near a fire → at camp

        boolean onForayBefore = s.onForay();
        int budgetBefore = s.turnsUntilNightfall();
        int dayBefore = s.dayNumber();
        int clockBefore = s.getClockTurns();

        String jsonStr = json().toJson(s);
        assertFalse(jsonStr.contains("foray"), "no foray state is persisted (AD-6)");
        assertFalse(jsonStr.contains("nightfall"), "no budget state is persisted (AD-6)");

        RunState loaded = json().fromJson(RunState.class, jsonStr);
        loaded.restoreAfterLoad();

        assertEquals(clockBefore, loaded.getClockTurns(), "the clock persists (the budget derives from it)");
        assertEquals(onForayBefore, loaded.onForay(), "on-foray survives the round-trip without a field");
        assertEquals(budgetBefore, loaded.turnsUntilNightfall(), "the budget derives from the persisted clock");
        assertEquals(dayBefore, loaded.dayNumber(), "the day count derives from the persisted clock");
    }

    // --- Review follow-ups (code review 2026-08-10) ---

    @Test
    void aCampfireSuppressesTheNightStumbleAcrossItsFullSafeRadius() {
        // Review patch (Decision 1): "at camp" (within CAMPFIRE_SAFE_RADIUS) now suppresses the
        // night stumble, so the camp's safety radius and its night-light protection cover the same
        // ground. Under the old code (isPlayerAtFire, Manhattan ≤ 1) a player 2–5 tiles from the
        // fire would stumble; this pins that a campfire 3 tiles away protects the whole oscillation.
        RunState s = clear(7L);
        driveToNight(s);
        int[] cell = wildernessCellAndNeighbor(s.getTileMap());
        s.getPlayer().placeAt(cell[0], cell[1]);
        s.getEnemies().clear();
        s.getCompanions().clear();
        // Fire 3 tiles east — beyond the ≤1 cooking range, inside the ≤5 safe radius. The player
        // oscillates one tile, staying Manhattan 2–4 from the fire (all > 1, all ≤ 5).
        s.setCampfire(cell[0] + 3, cell[1]);
        assertTrue(s.isPlayerAtCampfireSafePoint(), "the player starts within the campfire's safe radius");
        assertFalse(s.isPlayerAtFire(), "but beyond the tight cooking-adjacency range (the old suppression)");
        TurnEngine engine = new TurnEngine();
        for (int i = 0; i < 40; i++) {
            int dx = (i % 2 == 0 ? cell[2] : -cell[2]);
            int dy = (i % 2 == 0 ? cell[3] : -cell[3]);
            int hp = s.getPlayer().getHp();
            TurnResult r = engine.advance(s, PlayerAction.move(dx, dy, RoguePlayer.directionOf(dx, dy)));
            assertEquals(hp, s.getPlayer().getHp(), "at camp (within the safe radius) the night stumble never fires");
            assertFalse(r.messages.contains(HazardSystem.NIGHT_STUMBLE_MESSAGE));
            assertTrue(s.isPlayerAtCampfireSafePoint(), "the oscillation stays within the safe radius");
        }
    }

    @Test
    void aLethalNightStumbleHonorsTheLastStandReprieve() {
        // Review patch (Patch 2a): the night stumble fires inside HazardSystem.step (during the
        // MOVE, TurnEngine:76), BEFORE checkLastStand (TurnEngine:274) — so a stumble that would
        // drop Klein to 0 HP earns the one-per-run reprieve instead of killing him (AD-5, the
        // story's "lethal harm honors the reprieve"). Correct by ordering, now pinned.
        RunState s = clear(7L);
        driveToNight(s);
        int[] cell = wildernessCellAndNeighbor(s.getTileMap());
        s.getPlayer().placeAt(cell[0], cell[1]);
        s.getEnemies().clear();
        s.getCompanions().clear();
        s.getPlayer().hurtRaw(s.getPlayer().getHp() - 1); // to 1 HP, so the next stumble is lethal
        assertEquals(1, s.getPlayer().getHp());
        TurnEngine engine = new TurnEngine();
        boolean reprieved = false;
        for (int i = 0; i < 60 && !reprieved; i++) {
            int dx = (i % 2 == 0 ? cell[2] : -cell[2]);
            int dy = (i % 2 == 0 ? cell[3] : -cell[3]);
            TurnResult r = engine.advance(s, PlayerAction.move(dx, dy, RoguePlayer.directionOf(dx, dy)));
            if (s.isLastStandUsed()) {
                reprieved = true;
                assertEquals(1, s.getPlayer().getHp(), "the lethal night stumble revived Klein to 1 HP, not death");
                assertTrue(r.messages.contains(HazardSystem.NIGHT_STUMBLE_MESSAGE),
                        "the reprieve fired on a night-stumble turn: " + r.messages);
                assertTrue(r.messages.contains("Last Stand!"), "the reprieve announces itself: " + r.messages);
            }
        }
        assertTrue(reprieved, "a lethal night stumble fired and the Last-Stand reprieve caught it (seed 7)");
    }

    @Test
    void theNightOverlayReadsThePreTickPhaseAtTheDuskBoundary() {
        // Review patch (Patch 2b): the overlay resolves during the MOVE (TurnEngine:76), BEFORE
        // tickClock (TurnEngine:254) — so it reads the turn's STARTING phase. A MOVE acted at clock
        // 99 is a daylight step (the overlay early-returns on isDay()), even though the same turn
        // ticks to 100/Night and emits "Dusk falls". This locks the intended pre-tick semantics: a
        // future move of the overlay to a post-tick read would fail here (the step would be a Night
        // step and could stumble). The dawn-side flip line is pinned symmetrically.
        RunState s = clear(7L);
        TurnEngine engine = new TurnEngine();
        for (int i = 0; i < 99; i++) engine.advance(s, PlayerAction.wait(RoguePlayer.SOUTH));
        assertEquals(99, s.getClockTurns());
        assertTrue(s.isDay(), "clock 99 is the last Day turn");
        int[] cell = wildernessCellAndNeighbor(s.getTileMap());
        s.getPlayer().placeAt(cell[0], cell[1]);
        s.getEnemies().clear();
        s.getCompanions().clear();
        int hp = s.getPlayer().getHp();
        TurnResult r = engine.advance(s,
                PlayerAction.move(cell[2], cell[3], RoguePlayer.directionOf(cell[2], cell[3])));
        assertEquals(100, s.getClockTurns(), "the boundary MOVE ticked the clock to Night");
        assertTrue(r.messages.contains(RunState.LINE_DUSK), "and announced the flip");
        assertEquals(hp, s.getPlayer().getHp(), "but the step itself resolved in Day — no night stumble (pre-tick phase)");
        assertFalse(r.messages.contains(HazardSystem.NIGHT_STUMBLE_MESSAGE),
                "the pre-tick Day read means the overlay never rolled on the dusk-crossing step");

        // Dawn side: a MOVE acted at clock 169 crosses to 170/Day and emits "Dawn breaks."
        for (int i = 0; i < 69; i++) engine.advance(s, PlayerAction.wait(RoguePlayer.SOUTH));
        assertEquals(169, s.getClockTurns());
        s.getPlayer().placeAt(cell[0], cell[1]);
        s.lightTorch(10); // hold a torch so the dawn-crossing step is deterministic (no stumble either way)
        TurnResult dawn = engine.advance(s,
                PlayerAction.move(cell[2], cell[3], RoguePlayer.directionOf(cell[2], cell[3])));
        assertEquals(170, s.getClockTurns());
        assertTrue(dawn.messages.contains(RunState.LINE_DAWN), "the night→day boundary MOVE announces dawn");
    }

    // --- helpers ---

    /** Mirrors RunStatePersistenceTest.json() — the production serializer (AD-6). */
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

    /** A walkable cell inside the structure's footprint holding the authored guaranteed ROPE (a
     *  non-scatterable item, so only placeStructureLoot can place it — 3.2 P1 proof pattern), or
     *  null if none. */
    private static int[] ropeCellIn(RogueTileMap m, RunState s, int structureType) {
        int[] box = footprint(m, structureType);
        if (box == null) return null;
        for (FloorItem it : s.getFloorItems()) {
            if (it.type != Supply.ROPE.ordinal()) continue;
            if (it.x >= box[0] && it.x <= box[2] && it.y >= box[1] && it.y <= box[3]
                    && m.isWalkable(it.x, it.y)) {
                return new int[]{it.x, it.y};
            }
        }
        return null;
    }

    /** The bounding box of a structure's stamped footprint, or null if the type isn't on the map
     *  (the StructureContentTest pattern). */
    private static int[] footprint(RogueTileMap m, int type) {
        int minX = m.getWidth(), minY = m.getHeight(), maxX = -1, maxY = -1;
        for (int x = 0; x < m.getWidth(); x++) {
            for (int y = 0; y < m.getHeight(); y++) {
                if (m.getStructureType(x, y) == type) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        return maxX < 0 ? null : new int[]{minX, minY, maxX, maxY};
    }

    /** A 4-directional MOVE list from (sx,sy) to (tx,ty) over walkable cells (the region is one
     *  component, AD-8), or null if none — a deterministic route the TurnEngine walk follows. */
    private static List<int[]> path(RogueTileMap m, int sx, int sy, int tx, int ty) {
        int w = m.getWidth(), h = m.getHeight();
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        boolean[][] visited = new boolean[w][h];
        int[][][] parent = new int[w][h][2];
        java.util.ArrayDeque<int[]> queue = new java.util.ArrayDeque<>();
        queue.add(new int[]{sx, sy});
        visited[sx][sy] = true;
        boolean found = false;
        while (!queue.isEmpty() && !found) {
            int[] cur = queue.poll();
            for (int[] d : dirs) {
                int nx = cur[0] + d[0], ny = cur[1] + d[1];
                if (nx < 0 || nx >= w || ny < 0 || ny >= h) continue;
                if (visited[nx][ny] || !m.isWalkable(nx, ny)) continue;
                visited[nx][ny] = true;
                parent[nx][ny] = cur;
                if (nx == tx && ny == ty) {
                    found = true;
                    break;
                }
                queue.add(new int[]{nx, ny});
            }
        }
        if (!found) return null;
        List<int[]> moves = new ArrayList<>();
        int cx = tx, cy = ty;
        while (!(cx == sx && cy == sy)) {
            int[] p = parent[cx][cy];
            moves.add(new int[]{cx - p[0], cy - p[1]});
            cx = p[0];
            cy = p[1];
        }
        java.util.Collections.reverse(moves);
        return moves;
    }

    /** Drive the clock to the first Night turn (clock 100) via acted WAITs — no steps, so the
     *  night overlay never rolls during the approach. Deterministic on the seed. */
    private static void driveToNight(RunState s) {
        TurnEngine engine = new TurnEngine();
        for (int i = 0; i < 100; i++) engine.advance(s, PlayerAction.wait(RoguePlayer.SOUTH));
        assertFalse(s.isDay(), "the 100th acted turn opens Night");
    }

    /** {ax, ay, dx, dy}: a non-structure walkable cell with a 4-adjacent non-structure walkable
     *  cell, or null (the StructureContentTest pattern). */
    private static int[] wildernessCellAndNeighbor(RogueTileMap m) {
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int x = 0; x < m.getWidth(); x++) {
            for (int y = 0; y < m.getHeight(); y++) {
                if (m.getStructureType(x, y) >= 0 || !m.isWalkable(x, y)) continue;
                for (int[] d : dirs) {
                    int nx = x + d[0], ny = y + d[1];
                    if (m.getStructureType(nx, ny) < 0 && m.isWalkable(nx, ny)) {
                        return new int[]{x, y, d[0], d[1]};
                    }
                }
            }
        }
        return null;
    }

    /** {ax, ay, dx, dy}: a walkable structure cell with a 4-adjacent walkable cell of the SAME
     *  structure type (the destination the player steps onto), or null. */
    private static int[] walkableCellAndNeighbor(RogueTileMap m, int type) {
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int x = 0; x < m.getWidth(); x++) {
            for (int y = 0; y < m.getHeight(); y++) {
                if (m.getStructureType(x, y) != type || !m.isWalkable(x, y)) continue;
                for (int[] d : dirs) {
                    int nx = x + d[0], ny = y + d[1];
                    if (m.getStructureType(nx, ny) == type && m.isWalkable(nx, ny)) {
                        return new int[]{x, y, d[0], d[1]};
                    }
                }
            }
        }
        return null;
    }

    /** A walkable cell with a non-walkable (wall) 4-adjacent cell to the west — a guaranteed
     *  refused MOVE(−1,0), so an un-acted turn is deterministic. */
    private static int[] wallLeft(RogueTileMap m) {
        for (int x = 1; x < m.getWidth(); x++) {
            for (int y = 0; y < m.getHeight(); y++) {
                if (m.isWalkable(x, y) && !m.isWalkable(x - 1, y)) return new int[]{x, y};
            }
        }
        return null;
    }
}

package com.margins.rogue;

import com.margins.rogue.item.Supply;
import com.margins.rogue.state.RunState;
import com.margins.rogue.system.LightSystem;
import com.margins.rogue.system.PlayerAction;
import com.margins.rogue.system.TemperatureSystem;
import com.margins.rogue.system.TorchSystem;
import com.margins.rogue.system.TurnEngine;
import com.margins.rogue.system.TurnResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The torch (FR-7, Story 1.6 AC-3): 1 Wood + 1 Coal craft a carried light that follows the
 * player, burns down over ~one Night (60 acted turns) on the acted path, and on expiry reverts
 * to the campfire's light (if any) or goes dark. Exercised through TurnEngine so the wiring
 * (the CRAFT_TORCH case + TorchSystem burn before LightSystem.emitNoise) is covered. Weather is
 * pinned CLEAR so the temperature driver can't interfere with the light mechanics under test.
 */
class TorchTest {

    private static RunState clearRun() {
        RunState s = new RunState(1L);
        s.setWeather(Weather.CLEAR);
        return s;
    }

    private static void giveWoodAndCoal(RunState s) {
        s.getInventory().tryAdd(Supply.WOOD.ordinal(), 1);
        s.getInventory().tryAdd(Supply.COAL.ordinal(), 1);
    }

    @Test
    void craftTorchConsumesMaterialsLightsAndBurnsOneTurn() {
        RunState s = clearRun();
        giveWoodAndCoal(s);
        TurnEngine te = new TurnEngine();
        int clock = s.getClockTurns();

        te.advance(s, PlayerAction.craftTorch(0));

        assertEquals(clock + 1, s.getClockTurns(), "crafting is a real action (commits a turn)");
        assertEquals(0, s.getInventory().count(Supply.WOOD.ordinal()), "the Wood is spent");
        assertEquals(0, s.getInventory().count(Supply.COAL.ordinal()), "the Coal is spent");
        assertEquals(TorchSystem.TORCH_BURN - 1, s.getTorchTurns(),
                "the torch lights at TORCH_BURN and the craft turn itself burns one");
        assertTrue(s.hasLight(), "the torch lights");
        assertEquals(s.getPlayer().getTileX(), s.getLightX(), "the light is at the player's tile");
        assertEquals(s.getPlayer().getTileY(), s.getLightY(), "the light is at the player's tile");
    }

    @Test
    void craftTorchRefusedWithoutMaterialsSpendsNothing() {
        RunState s = clearRun();
        TurnEngine te = new TurnEngine();
        int clock = s.getClockTurns();

        TurnResult r = te.advance(s, PlayerAction.craftTorch(0));

        assertTrue(r.messages.contains("A torch needs Wood and Coal."), "the refusal is explained");
        assertEquals(0, s.getTorchTurns(), "no torch");
        assertFalse(s.hasLight(), "no light");
        assertEquals(clock, s.getClockTurns(), "no turn committed");
    }

    @Test
    void craftTorchRefusedWithoutCoalEvenWithWood() {
        RunState s = clearRun();
        s.getInventory().tryAdd(Supply.WOOD.ordinal(), 1); // Wood but no Coal
        TurnEngine te = new TurnEngine();
        int clock = s.getClockTurns();

        te.advance(s, PlayerAction.craftTorch(0));

        assertEquals(clock, s.getClockTurns(), "a torch needs BOTH materials — no turn");
        assertEquals(1, s.getInventory().count(Supply.WOOD.ordinal()), "nothing spent");
        assertEquals(0, s.getTorchTurns(), "no torch");
    }

    @Test
    void craftTorchRefusedWhileTorchAlreadyBurns() {
        // Review decision: a torch can't be extinguished, so re-crafting while lit is pure waste —
        // refused (no turn, no materials spent, burn untouched).
        RunState s = clearRun();
        giveWoodAndCoal(s);
        TurnEngine te = new TurnEngine();
        te.advance(s, PlayerAction.craftTorch(0)); // torch lit
        int burn = s.getTorchTurns();
        int clock = s.getClockTurns();
        giveWoodAndCoal(s); // more materials for a would-be refresh

        TurnResult r = te.advance(s, PlayerAction.craftTorch(0));

        assertTrue(r.messages.contains("A torch already burns."), "the refusal is explained");
        assertEquals(clock, s.getClockTurns(), "no turn committed");
        assertEquals(1, s.getInventory().count(Supply.WOOD.ordinal()), "no Wood spent");
        assertEquals(1, s.getInventory().count(Supply.COAL.ordinal()), "no Coal spent");
        assertEquals(burn, s.getTorchTurns(), "the burn is untouched");
    }

    // --- Task 5 checklist scenarios (review follow-ups) ---

    @Test
    void litTorchEmitsNoiseAtThePlayerTile() {
        // The checklist's "the lit torch emits noise (a NoiseEvent appears at the player tile —
        // mirrors LightNoiseTest)": a lit torch is a carried light, so it emits one per-turn
        // NoiseEvent at the bearer's tile (AD-18) — the exposure cost of AC-4.
        RunState s = clearRun();
        giveWoodAndCoal(s);
        TurnEngine te = new TurnEngine();
        te.advance(s, PlayerAction.craftTorch(0)); // torch lit at the player's tile

        LightSystem.emitNoise(s);

        assertEquals(1, s.getNoiseQueue().size(), "a lit torch emits exactly one noise");
        NoiseEvent n = s.getNoiseQueue().get(0);
        assertEquals(s.getPlayer().getTileX(), n.x, "noise is at the torch bearer's tile");
        assertEquals(s.getPlayer().getTileY(), n.y, "noise is at the torch bearer's tile");
        assertEquals(LightSystem.LIGHT_NOISE_RADIUS, n.radius, "at the light's noise radius");
    }

    @Test
    void expiredTorchEmitsNoNoise() {
        // The TorchSystem-before-LightSystem.emitNoise ordering (AD-4): a just-expired torch is
        // unlit by the time the noise step runs, so it neither lights nor lures on that turn.
        RunState s = clearRun();
        s.getEnemies().clear(); // the burn is a pure state countdown — no enemy interference
        giveWoodAndCoal(s);
        TurnEngine te = new TurnEngine();
        te.advance(s, PlayerAction.craftTorch(0)); // lights at 60; the craft turn burns one → 59
        for (int i = 0; i < 59; i++) te.advance(s, PlayerAction.wait(0)); // → 0, expires this turn

        assertFalse(s.hasLight(), "the torch expired on the last acted turn");
        LightSystem.emitNoise(s);
        assertTrue(s.getNoiseQueue().isEmpty(), "an expired torch emits no noise");
    }

    @Test
    void litTorchDoesNotWarmUnderAColdSnap() {
        // The checklist's "the torch provides NO warmth (temperature still drops under a pinned
        // Cold Snap with a torch but no fire)": TemperatureSystem warms only via isPlayerAtFire()
        // — a lit torch is light-only (AC-4), so the bearer still freezes.
        RunState s = new RunState(1L);
        s.setWeather(Weather.COLD_SNAP);
        giveWoodAndCoal(s);
        TurnEngine te = new TurnEngine();
        te.advance(s, PlayerAction.craftTorch(0)); // torch lit, no campfire
        assertTrue(s.hasLight(), "the torch is lit");
        assertFalse(s.isPlayerAtFire(), "no campfire — the player is not at a fire");

        for (int i = 0; i < 10; i++) TemperatureSystem.tick(s, new ArrayList<>());

        assertTrue(s.getPlayer().getTemperature() < 0, "the torch does not warm — the player freezes");
    }

    @Test
    void torchFollowsThePlayerAcrossTiles() {
        RunState s = clearRun();
        giveWoodAndCoal(s);
        TurnEngine te = new TurnEngine();
        RoguePlayer p = s.getPlayer();
        te.advance(s, PlayerAction.craftTorch(0));

        // Step to a walkable neighbor: the carried light must move with the player.
        RogueTileMap m = s.getTileMap();
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        boolean moved = false;
        for (int[] d : dirs) {
            if (m.isWalkable(p.getTileX() + d[0], p.getTileY() + d[1])) {
                te.advance(s, PlayerAction.move(d[0], d[1], 0));
                assertEquals(p.getTileX(), s.getLightX(), "the torch light follows the player");
                assertEquals(p.getTileY(), s.getLightY(), "the torch light follows the player");
                moved = true;
                break;
            }
        }
        assertTrue(moved, "a walkable neighbor exists");
        assertTrue(s.getTorchTurns() < TorchSystem.TORCH_BURN - 1, "the torch kept burning as it moved");
    }

    @Test
    void torchBurnsForAboutANightThenDies() {
        RunState s = clearRun();
        s.getEnemies().clear(); // the burn is a pure state countdown — no enemy interference over 60 turns
        giveWoodAndCoal(s);
        TurnEngine te = new TurnEngine();
        te.advance(s, PlayerAction.craftTorch(0)); // lights at 60; the craft turn burns one → 59

        for (int i = 0; i < 58; i++) te.advance(s, PlayerAction.wait(0)); // …down to 1
        assertTrue(s.hasLight(), "the torch is still lit on its 59th acted turn");
        assertEquals(1, s.getTorchTurns(), "one burn remains");

        te.advance(s, PlayerAction.wait(0)); // the 60th acted turn — it dies
        assertFalse(s.hasLight(), "the torch has burned out (no campfire to revert to)");
        assertEquals(0, s.getTorchTurns(), "no burn remains");
    }

    @Test
    void wallBumpDoesNotBurnTheTorch() {
        // AD-5: the burn is on the acted path only — a wasted keypress must not shorten the torch.
        RunState s = clearRun();
        giveWoodAndCoal(s);
        TurnEngine te = new TurnEngine();
        te.advance(s, PlayerAction.craftTorch(0));
        int burn = s.getTorchTurns();
        RogueTileMap m = s.getTileMap();
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        int[] toWall = null;
        outer:
        for (int x = 1; x < m.getWidth() - 1; x++) {
            for (int y = 1; y < m.getHeight() - 1; y++) {
                if (m.getTile(x, y) != RogueTile.FLOOR) continue;
                for (int[] d : dirs) {
                    if (!m.isWalkable(x + d[0], y + d[1])) { s.getPlayer().placeAt(x, y); toWall = d; break outer; }
                }
            }
        }
        assertNotNull(toWall, "a floor tile adjacent to a wall exists");

        te.advance(s, PlayerAction.move(toWall[0], toWall[1], 0)); // blocked move — no turn

        assertEquals(burn, s.getTorchTurns(), "a wall-bump does not burn the torch");
        assertTrue(s.hasLight(), "and the torch stays lit");
    }

    @Test
    void torchExpiryRevertsTheLightToTheCampfire() {
        RunState s = clearRun();
        giveWoodAndCoal(s);
        TurnEngine te = new TurnEngine();
        RoguePlayer p = s.getPlayer();
        s.setCampfire(p.getTileX(), p.getTileY()); // a fire station at the current tile
        te.advance(s, PlayerAction.craftTorch(0));

        // Step off the fire so the campfire tile ≠ the player's tile — the revert must be visible.
        RogueTileMap m = s.getTileMap();
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        boolean moved = false;
        for (int[] d : dirs) {
            if (m.isWalkable(p.getTileX() + d[0], p.getTileY() + d[1])) {
                te.advance(s, PlayerAction.move(d[0], d[1], 0));
                moved = true;
                break;
            }
        }
        assertTrue(moved, "a walkable neighbor exists to step off the fire");
        assertTrue(s.hasLight(), "the torch light is still on after moving");
        assertEquals(p.getTileX(), s.getLightX(), "the torch light followed the player");

        int guard = 0;
        while (s.getTorchTurns() > 0 && guard++ < 100) te.advance(s, PlayerAction.wait(0));
        assertEquals(0, s.getTorchTurns(), "the torch burned out");

        assertTrue(s.hasLight(), "the campfire re-claims the light on torch expiry");
        assertEquals(s.getCampfireX(), s.getLightX(), "the light reverts to the campfire tile");
        assertEquals(s.getCampfireY(), s.getLightY(), "the light reverts to the campfire tile");
    }
}

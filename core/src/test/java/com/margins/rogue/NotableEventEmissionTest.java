package com.margins.rogue;

import com.margins.rogue.item.Supply;
import com.margins.rogue.state.RunState;
import com.margins.rogue.system.ConsumptionSystem;
import com.margins.rogue.system.DetectionSystem;
import com.margins.rogue.system.HungerSystem;
import com.margins.rogue.system.PlayerAction;
import com.margins.rogue.system.TemperatureSystem;
import com.margins.rogue.system.ThirstSystem;
import com.margins.rogue.system.TurnEngine;
import com.margins.rogue.system.TurnResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Story 1.8 AC-2: the four silent notable-event classes — hunger/thirst tier change, temperature
 * band change, weather roll, detection escalation — emit SPD-tone messages via System observation
 * (Decision 2: the System captures state before its tick, ticks, compares after; the pipeline owns
 * the message channel — RoguePlayer stays a closed state holder). The tier RISE (eating back up)
 * emits from ConsumptionSystem, the System whose apply drives eat()/drink() — tickHunger/tickThirst
 * never rise, so HungerSystem/ThirstSystem can only observe drops.
 */
class NotableEventEmissionTest {

    @Test
    void hungerTierDropEmitsSPDLine() {
        RunState s = new RunState(1L);
        RoguePlayer p = s.getPlayer();
        List<String> msgs = new ArrayList<>();
        for (int i = 0; i < 250; i++) HungerSystem.tick(p, msgs); // SATISFIED(250) -> HUNGRY
        assertEquals(RoguePlayer.HungerStatus.HUNGRY, p.getStatus());
        assertEquals(1, msgs.size(), "one tier-crossing message for the whole drop");
        assertEquals("Hunger gnaws — you're Hungry.", msgs.get(0));
    }

    @Test
    void eatingBackUpEmitsSPDLine() {
        // The rise is notable (Decision 2): hunger only ever rises inside eat(), which only
        // ConsumptionSystem drives — so the rise emits there. HUNGRY + two cooked meats (60 food
        // each, FOOD_PER_TIER 100) crosses SATISFIED on the second.
        RunState s = new RunState(1L);
        RoguePlayer p = s.getPlayer();
        for (int i = 0; i < 250; i++) HungerSystem.tick(p, new ArrayList<>()); // drop to HUNGRY
        assertEquals(RoguePlayer.HungerStatus.HUNGRY, p.getStatus());
        s.getInventory().tryAdd(Supply.COOKED_MEAT.ordinal(), 2);

        List<String> msgs = new ArrayList<>();
        ConsumptionSystem.consume(s, Supply.COOKED_MEAT.ordinal(), msgs);
        ConsumptionSystem.consume(s, Supply.COOKED_MEAT.ordinal(), msgs);

        assertEquals(RoguePlayer.HungerStatus.SATISFIED, p.getStatus(), "two cooked meats cross the tier");
        assertEquals("Your belly is Satisfied.", msgs.get(msgs.size() - 1),
                "the rise emits its tier line after the Consumed lines");
    }

    @Test
    void thirstTierDropEmitsSPDLine() {
        RunState s = new RunState(1L);
        RoguePlayer p = s.getPlayer();
        List<String> msgs = new ArrayList<>();
        for (int i = 0; i < 200; i++) ThirstSystem.tick(p, msgs); // HYDRATED(200) -> THIRSTY
        assertEquals(RoguePlayer.ThirstStatus.THIRSTY, p.getThirstStatus());
        assertEquals(1, msgs.size(), "one tier-crossing message for the whole drop");
        assertEquals("You're getting Thirsty.", msgs.get(0));
    }

    @Test
    void temperatureBandEntryEmitsSPDLine() {
        RunState s = new RunState(1L);
        s.setWeather(Weather.COLD_SNAP); // -2/turn, deterministic (SurvivalTickTest pin precedent)
        List<String> msgs = new ArrayList<>();
        for (int i = 0; i < 40; i++) TemperatureSystem.tick(s, msgs); // 0 -> -80 (Frozen)
        assertEquals(RoguePlayer.TempBand.FROZEN, s.getPlayer().getTempBand());
        assertEquals(3, msgs.size(), "Chilled, Cold, Frozen each emit once"); // crossing ticks 8/25/40
        assertEquals("The cold sinks in — Frozen.", msgs.get(msgs.size() - 1),
                "the lethal entry is loud and lands last");
    }

    @Test
    void weatherRollEmitsTheOnsetLineExactlyWhenTheWeatherChanges() {
        RunState s = new RunState(1L);
        s.setWeather(Weather.CLEAR); // deterministic "before" for the first boundary
        List<String> onsets = Arrays.stream(Weather.values()).map(Weather::onsetLine).toList();
        int changes = 0;
        for (int cycle = 0; cycle < 4; cycle++) {
            // Fast-forward to the last turn of the current cycle via the SAME roll path (tickClock)
            // but WITHOUT the pipeline — only TurnEngine observes, so only the boundary turn emits.
            while (s.getClockTurns() % RunState.CYCLE_LENGTH != RunState.CYCLE_LENGTH - 1) {
                s.tickClock();
            }
            Weather before = s.getWeather();
            TurnResult r = new TurnEngine().advance(s, PlayerAction.wait(0)); // crosses the boundary
            Weather after = s.getWeather();
            if (after != before) {
                changes++;
                assertTrue(r.messages.contains(after.onsetLine()),
                        "the boundary roll emits " + after + "'s onset line: " + r.messages);
            } else {
                assertTrue(r.messages.stream().noneMatch(onsets::contains),
                        "no onset line when the roll holds the weather: " + r.messages);
            }
        }
        assertTrue(changes >= 1, "the seeded RNG changed the weather at least once across 4 cycles");
    }

    @Test
    void debuffEscalationLandsInTheLog() {
        // Task 2 requires debuff escalations to reach the log — pinned through the REAL pipeline
        // (auditor test-gap: the unit-level DebuffSystem calls couldn't prove the ordering in
        // TurnEngine.tick → log append never drops the line). Nausea's 30-turn course escalates to
        // Fever on the 30th wait; 30 waits also drain ~60 thirst (HYDRATED 200 → THIRSTY, no harm)
        // and no enemy reaches the spawn in 30 turns, so the player survives to see it.
        RunState s = new RunState(1L);
        s.getPlayer().beginBacterial(); // Nausea 30 + Diarrhea Stage 1 30
        TurnEngine te = new TurnEngine();
        for (int i = 0; i < RoguePlayer.NAUSEA_TURNS; i++) te.advance(s, PlayerAction.wait(0));
        assertEquals(RoguePlayer.BacterialStage.FEVER, s.getPlayer().getBacterialStage(),
                "Nausea escalates to Fever at the end of its course");
        assertTrue(s.getMessageLog().stream().anyMatch(m -> m.contains("deepens into fever")),
                "the escalation line lands in the log (AC-2): " + s.getMessageLog());
    }

    @Test
    void detectionEscalationEmitsAtMostOneLinePerKindPerTurn() {
        RunState s = new RunState(1L);
        RogueTileMap map = s.getTileMap();
        s.getPlayer().placeAt(25, 25);
        map.setTile(25, 26, RogueTile.FLOOR); // adjacent — guaranteed line of sight
        s.getEnemies().clear(); // isolate the one enemy under test
        RogueEnemy e = new RogueEnemy(25, 26, map);
        s.getEnemies().add(e);

        List<String> msgs = new ArrayList<>();
        DetectionSystem.update(s, msgs); // 1st sight turn: UNAWARE -> SUSPICIOUS
        assertEquals(Detection.SUSPICIOUS, e.getDetection());
        assertEquals(List.of("Something stirs in the trees."), msgs);

        msgs.clear();
        DetectionSystem.update(s, msgs); // 2nd sight turn: SUSPICIOUS -> ALERTED
        assertEquals(Detection.ALERTED, e.getDetection());
        assertEquals(List.of("A patrol has spotted you!"), msgs);

        msgs.clear();
        DetectionSystem.update(s, msgs); // 3rd turn: already ALERTED — no repeat line
        assertTrue(msgs.isEmpty(), "no spam for a garrison that's already alert");
    }
}

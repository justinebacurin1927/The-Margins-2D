package com.margins.rogue;

import com.margins.rogue.item.Supply;
import com.margins.rogue.state.RunState;
import com.margins.rogue.system.ConsumptionSystem;
import com.margins.rogue.system.DebuffSystem;
import com.margins.rogue.system.PlayerAction;
import com.margins.rogue.system.ThirstSystem;
import com.margins.rogue.system.TurnEngine;
import com.margins.rogue.system.TurnResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Debuff system (FR-8, Story 1.7): the bacterial escalation chain (Nausea→Fever→Delirium),
 * the parallel Diarrhea drain, the mushroom/toxin track (Rotgut, Honeymoon→Collapse), and the
 * cures/nourish-out. Driven through DebuffSystem + ConsumptionSystem so the wiring is covered.
 */
class DebuffSystemTest {

    private static RunState state() { return new RunState(1L); }
    private static List<String> messages() { return new ArrayList<>(); }

    private static RoguePlayer p(RunState s) { return s.getPlayer(); }

    /** Place the player on a floor tile adjacent to a wall; return the (dx,dy) toward the wall. */
    private static int[] placeAgainstWall(RunState s) {
        RogueTileMap m = s.getTileMap();
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int x = 1; x < m.getWidth() - 1; x++) {
            for (int y = 1; y < m.getHeight() - 1; y++) {
                if (m.getTile(x, y) != RogueTile.FLOOR) continue;
                for (int[] d : dirs) {
                    if (!m.isWalkable(x + d[0], y + d[1])) {
                        s.getPlayer().placeAt(x, y);
                        return d;
                    }
                }
            }
        }
        throw new IllegalStateException("no floor tile adjacent to a wall");
    }

    // --- Task 1: the bacterial track ---

    @Test
    void failedContaminationRollStartsNauseaAndDiarrheaWithNoFlatHarm() {
        RunState s = null;
        for (long seed = 1; seed <= 100; seed++) { // find a seed whose POND_WATER roll fails (60%)
            RunState cand = new RunState(seed);
            cand.getInventory().tryAdd(Supply.POND_WATER.ordinal(), 1);
            for (int i = 0; i < 210; i++) cand.getPlayer().tickThirst(); // THIRSTY so the drink isn't refused
            ConsumptionSystem.consume(cand, Supply.POND_WATER.ordinal(), new ArrayList<>());
            if (cand.getPlayer().getBacterialStage() != RoguePlayer.BacterialStage.NONE) { s = cand; break; }
        }
        assertNotNull(s, "a seed whose roll fails exists");
        RoguePlayer pl = p(s);
        assertEquals(20, pl.getHp(), "the bacterial onset replaces the flat HP sting (Story 1.7)");
        assertEquals(RoguePlayer.BacterialStage.NAUSEA, pl.getBacterialStage());
        assertEquals(RoguePlayer.NAUSEA_TURNS, pl.getBacterialTimer());
        assertEquals(RoguePlayer.DiarrheaStage.STAGE_1, pl.getDiarrheaStage(), "Diarrhea runs parallel");
        assertEquals(RoguePlayer.DIARRHEA_STAGE_1_TURNS, pl.getDiarrheaTimer());
    }

    @Test
    void safeProvisionNeverPoisons() {
        for (long seed = 1; seed <= 100; seed++) {
            RunState s = new RunState(seed);
            s.getInventory().tryAdd(Supply.BOILED_WATER.ordinal(), 1);
            for (int i = 0; i < 210; i++) s.getPlayer().tickThirst();
            ConsumptionSystem.consume(s, Supply.BOILED_WATER.ordinal(), new ArrayList<>());
            assertEquals(RoguePlayer.BacterialStage.NONE, s.getPlayer().getBacterialStage(),
                    "seed " + seed + ": boiled water (0% risk) never starts the track");
        }
    }

    @Test
    void bacterialEscalatesNauseaToFeverToDelirium() {
        RunState s = state();
        RoguePlayer pl = p(s);
        pl.beginBacterial();
        List<String> msgs = messages();
        for (int i = 0; i < RoguePlayer.NAUSEA_TURNS; i++) DebuffSystem.tick(s, msgs);
        assertEquals(RoguePlayer.BacterialStage.FEVER, pl.getBacterialStage(), "Nausea escalates at its course end");
        assertEquals(RoguePlayer.FEVER_TURNS, pl.getBacterialTimer());
        assertTrue(msgs.stream().anyMatch(m -> m.contains("fever")), "the escalation announces itself");
        for (int i = 0; i < RoguePlayer.FEVER_TURNS; i++) DebuffSystem.tick(s, msgs);
        assertEquals(RoguePlayer.BacterialStage.DELIRIUM, pl.getBacterialStage(), "Fever escalates to Delirium");
        assertEquals(RoguePlayer.DELIRIUM_TURNS, pl.getBacterialTimer());
    }

    @Test
    void untreatedDeliriumNeverAdvances() {
        RunState s = state();
        RoguePlayer pl = p(s);
        pl.beginBacterial();
        for (int i = 0; i < RoguePlayer.NAUSEA_TURNS + RoguePlayer.FEVER_TURNS; i++) DebuffSystem.tick(s, messages());
        assertEquals(RoguePlayer.BacterialStage.DELIRIUM, pl.getBacterialStage());
        for (int i = 0; i < 100; i++) DebuffSystem.tick(s, messages());
        assertEquals(RoguePlayer.BacterialStage.DELIRIUM, pl.getBacterialStage(), "turns alone never clear Delirium (AC-3)");
        assertEquals(RoguePlayer.DELIRIUM_TURNS, pl.getBacterialTimer(), "and its timer is latched while untreated");
    }

    @Test
    void treatedDeliriumShortensToQuarterThenClears() {
        RunState s = state();
        RoguePlayer pl = p(s);
        pl.beginBacterial();
        for (int i = 0; i < RoguePlayer.NAUSEA_TURNS + RoguePlayer.FEVER_TURNS; i++) DebuffSystem.tick(s, messages());
        pl.applyHerbalCure();
        assertTrue(pl.isDeliriumTreated(), "the cure item unlatches the timer");
        assertEquals((int) Math.floor(RoguePlayer.DELIRIUM_TURNS * 0.25f), pl.getBacterialTimer(), "75% shorter");
        List<String> msgs = messages();
        int treatedTurns = pl.getBacterialTimer(); // capture — the timer shrinks as it ticks
        for (int i = 0; i < treatedTurns; i++) DebuffSystem.tick(s, msgs);
        assertEquals(RoguePlayer.BacterialStage.NONE, pl.getBacterialStage(), "the treated timer runs down and clears");
    }

    @Test
    void strPenaltiesCompose() {
        RoguePlayer pl = p(state());
        assertEquals(5, pl.getStr());
        pl.beginBacterial();
        assertEquals(3, pl.getStr(), "Nausea -30%");
        pl.escalateToFever();
        assertEquals(3, pl.getStr(), "Fever -40%");
        pl.escalateToDelirium();
        assertEquals(5, pl.getStr(), "Delirium applies no STR factor");
        // The -30% vs -40% distinction shows under Starving's Fatigue: 5×0.65=3, ×0.70→2, ×0.60→1.
        pl.starve(250); // SATISFIED → HUNGRY
        pl.starve(250); // HUNGRY → STARVING
        pl.clearBacterial();
        assertEquals(3, pl.getStr(), "Starving alone: -35%");
        pl.beginBacterial(); // Nausea
        assertEquals(2, pl.getStr(), "Starving + Nausea stack multiplicatively");
        pl.escalateToFever();
        assertEquals(1, pl.getStr(), "Starving + Fever stack multiplicatively");
    }

    // --- Task 2: Diarrhea ---

    @Test
    void diarrheaStageOneDoublesThirstDrain() {
        RunState s = state();
        RoguePlayer pl = p(s);
        pl.beginBacterial(); // STAGE_1
        int thirst = pl.getThirst();
        ThirstSystem.tick(pl); // the normal acted-turn thirst tick (pipeline order: Thirst → Debuff)
        DebuffSystem.tick(s, messages());
        assertEquals(thirst - 2, pl.getThirst(), "Stage 1 adds +1 extra thirst tick per acted turn (2×)");
        assertEquals(RoguePlayer.DiarrheaStage.STAGE_1, pl.getDiarrheaStage());
    }

    @Test
    void diarrheaStageTwoTriplesThirstAndHungerDrain() {
        RunState s = state();
        RoguePlayer pl = p(s);
        pl.beginBacterial();
        for (int i = 0; i < RoguePlayer.DIARRHEA_STAGE_1_TURNS; i++) DebuffSystem.tick(s, messages());
        assertEquals(RoguePlayer.DiarrheaStage.STAGE_2, pl.getDiarrheaStage(), "escalates at 30 turns");
        int thirst = pl.getThirst(), hunger = pl.getHunger();
        ThirstSystem.tick(pl);
        DebuffSystem.tick(s, messages());
        assertEquals(thirst - 3, pl.getThirst(), "Stage 2 adds +2 thirst (3×)");
        assertEquals(hunger - 2, pl.getHunger(), "and +2 hunger (3×)");
    }

    @Test
    void unmitigatedStageTwoDiarrheaIsLethal() {
        RunState s = state();
        RoguePlayer pl = p(s);
        for (int i = 0; i < 600; i++) pl.tickThirst(); // HYDRATED → PARCHED(0), taking ~60 HP on the way
        pl.heal(pl.getMaxHp()); // restore full health for the measurement phase
        assertTrue(pl.getThirstStatus() == RoguePlayer.ThirstStatus.PARCHED, "the drain cadence is live");
        pl.beginBacterial();
        for (int i = 0; i < RoguePlayer.DIARRHEA_STAGE_1_TURNS; i++) DebuffSystem.tick(s, messages()); // → STAGE_2
        assertTrue(pl.isAlive(), "the escalation phase doesn't kill a full-health player");
        pl.hurtRaw(pl.getHp() - 3); // to 3 HP
        int turns = 0;
        while (pl.isAlive() && turns < 30) {
            pl.tickThirst(); // the normal acted-turn thirst tick
            DebuffSystem.tick(s, messages()); // +2 more (3×)
            turns++;
        }
        assertFalse(pl.isAlive(), "ignored Stage-2 diarrhea under Parched kills through the existing drain (AC-1)");
    }

    // --- Task 3: the toxin track ---

    @Test
    void toxicMushroomInflictsRotgutComposition() {
        RunState s = state();
        RoguePlayer pl = p(s);
        s.getInventory().tryAdd(Supply.TOXIC_MUSHROOM.ordinal(), 1);
        List<String> msgs = messages();
        assertTrue(ConsumptionSystem.consume(s, Supply.TOXIC_MUSHROOM.ordinal(), msgs));
        assertEquals(RoguePlayer.BacterialStage.NAUSEA, pl.getBacterialStage(), "Rotgut begins Nausea");
        assertEquals(RoguePlayer.NAUSEA_TURNS, pl.getBacterialTimer());
        assertTrue(pl.isRotgutCrippled(), "Rotgut applies instant Crippled");
        assertTrue(pl.isCrippled());
        assertEquals(RoguePlayer.DiarrheaStage.STAGE_1, pl.getDiarrheaStage(), "and Diarrhea Stage 1");
        assertEquals(20, pl.getHp(), "toxin is deterministic — no roll, no flat HP harm");
        assertTrue(msgs.stream().anyMatch(m -> m.contains("Rotgut")));
    }

    @Test
    void honeymoonCountsDownHiddenThenCollapses() {
        RunState s = state();
        RoguePlayer pl = p(s);
        s.getInventory().tryAdd(Supply.HONEYMOON_MUSHROOM.ordinal(), 1);
        List<String> msgs = new ArrayList<>();
        ConsumptionSystem.consume(s, Supply.HONEYMOON_MUSHROOM.ordinal(), msgs);
        assertEquals(RoguePlayer.HONEYMOON_TURNS, pl.getHoneymoonCountdown());
        assertFalse(msgs.stream().anyMatch(m ->
                m.contains("collapse") || m.contains("poison") || m.contains("sick")),
                "the countdown is hidden — the onset reads sweet, no warning (AC-2)");
        for (int i = 0; i < RoguePlayer.HONEYMOON_TURNS - 1; i++) DebuffSystem.tick(s, messages());
        assertFalse(pl.isCollapsed(), "collapse fires only at countdown 0");
        DebuffSystem.tick(s, messages());
        assertTrue(pl.isCollapsed());
        assertEquals(20 * RoguePlayer.COLLAPSE_CAP_PERCENT / 100, pl.getMaxHp(), "Max HP capped at 40% of base");
        assertTrue(pl.getHp() <= pl.getMaxHp(), "current HP clamped to the cap");
    }

    @Test
    void herbalCureLiftsTheCollapseCap() {
        RunState s = state();
        RoguePlayer pl = p(s);
        pl.beginHoneymoon();
        for (int i = 0; i < RoguePlayer.HONEYMOON_TURNS; i++) DebuffSystem.tick(s, messages());
        assertTrue(pl.isCollapsed());
        pl.applyHerbalCure();
        assertFalse(pl.isCollapsed(), "the cure item lifts the cap");
        assertEquals(20, pl.getMaxHp());
    }

    @Test
    void bloodveinHarmsFiveCuresBloatedAndRollsNinetyPercentRisk() {
        RunState s = null;
        for (long seed = 1; seed <= 100; seed++) {
            RunState cand = new RunState(seed);
            cand.getPlayer().eat(100); // SATISFIED → WELL_FED (Bloated slow active)
            assertTrue(cand.getPlayer().isSlowed());
            cand.getInventory().tryAdd(Supply.BLOODVEIN_MUSHROOM.ordinal(), 1);
            ConsumptionSystem.consume(cand, Supply.BLOODVEIN_MUSHROOM.ordinal(), new ArrayList<>());
            if (cand.getPlayer().getBacterialStage() != RoguePlayer.BacterialStage.NONE) { s = cand; break; }
        }
        assertNotNull(s, "a seed whose 90% bloodvein roll fails exists");
        RoguePlayer pl = p(s);
        assertEquals(15, pl.getHp(), "bloodvein costs 5 HP");
        assertFalse(pl.isSlowed(), "bloodvein cures Bloated");
        assertEquals(RoguePlayer.BacterialStage.NAUSEA, pl.getBacterialStage(),
                "the 90% contamination rides the ConsumptionSystem roll (FR-8)");
    }

    // --- Task 5: cures + nourish-out ---

    @Test
    void honeyCuresSickAndPoisoned() {
        RunState s = state();
        RoguePlayer pl = p(s);
        pl.beginBacterial(); // Nausea + Diarrhea STAGE_1
        s.getInventory().tryAdd(Supply.HONEY.ordinal(), 1);
        assertTrue(ConsumptionSystem.consume(s, Supply.HONEY.ordinal(), new ArrayList<>()));
        assertEquals(RoguePlayer.BacterialStage.NONE, pl.getBacterialStage(), "Honey clears Sick (Nausea/Fever)");
        assertEquals(RoguePlayer.DiarrheaStage.NONE, pl.getDiarrheaStage(), "and Diarrhea");
        assertFalse(pl.isRotgutCrippled());
    }

    @Test
    void honeyClearsRotgutCripple() {
        RunState s = state();
        RoguePlayer pl = p(s);
        pl.beginRotgut();
        assertTrue(pl.isRotgutCrippled());
        s.getInventory().tryAdd(Supply.HONEY.ordinal(), 1);
        ConsumptionSystem.consume(s, Supply.HONEY.ordinal(), new ArrayList<>());
        assertFalse(pl.isRotgutCrippled(), "Honey cures the Rotgut poison (FR-8)");
    }

    @Test
    void honeyDoesNotClearDelirium() {
        RunState s = state();
        RoguePlayer pl = p(s);
        pl.beginBacterial();
        for (int i = 0; i < RoguePlayer.NAUSEA_TURNS + RoguePlayer.FEVER_TURNS; i++) DebuffSystem.tick(s, messages());
        assertEquals(RoguePlayer.BacterialStage.DELIRIUM, pl.getBacterialStage());
        s.getInventory().tryAdd(Supply.HONEY.ordinal(), 1);
        ConsumptionSystem.consume(s, Supply.HONEY.ordinal(), new ArrayList<>());
        assertEquals(RoguePlayer.BacterialStage.DELIRIUM, pl.getBacterialStage(),
                "Honey leaves Delirium alone — the cure item treats it (Decision 4)");
    }

    @Test
    void curesAreNotRefusedWhenFull() {
        // Decision 6: a Well-Fed player can still take medicine — the canEat() refusal is bypassed.
        RunState s = state();
        RoguePlayer pl = p(s);
        pl.beginBacterial();
        for (int i = 0; i < RoguePlayer.NAUSEA_TURNS + RoguePlayer.FEVER_TURNS; i++) DebuffSystem.tick(s, messages());
        pl.eat(100); // SATISFIED → WELL_FED; nourish-out doesn't touch Delirium
        assertEquals(RoguePlayer.BacterialStage.DELIRIUM, pl.getBacterialStage(), "still sick");
        s.getInventory().tryAdd(Supply.HONEY.ordinal(), 1);
        assertTrue(ConsumptionSystem.consume(s, Supply.HONEY.ordinal(), new ArrayList<>()),
                "a cure is never refused on a full stomach");
        assertEquals(0, s.getInventory().count(Supply.HONEY.ordinal()), "the cure was consumed");
    }

    @Test
    void eatingOrDrinkingNourishesOutNauseaAndFever() {
        RoguePlayer pl = p(state());
        pl.beginBacterial();
        assertEquals(RoguePlayer.BacterialStage.NAUSEA, pl.getBacterialStage());
        pl.eat(40);
        assertEquals(RoguePlayer.BacterialStage.NONE, pl.getBacterialStage(), "eating clears Nausea (AC-3)");
        pl.beginBacterial();
        pl.escalateToFever();
        pl.drink(50);
        assertEquals(RoguePlayer.BacterialStage.NONE, pl.getBacterialStage(), "drinking clears Fever (AC-3)");
    }

    @Test
    void nourishmentDoesNotClearDeliriumOrDiarrhea() {
        RunState s = state();
        RoguePlayer pl = p(s);
        pl.beginBacterial();
        for (int i = 0; i < RoguePlayer.NAUSEA_TURNS + RoguePlayer.FEVER_TURNS; i++) DebuffSystem.tick(s, messages());
        pl.eat(40);
        assertEquals(RoguePlayer.BacterialStage.DELIRIUM, pl.getBacterialStage(),
                "Delirium survives nourishment — only cures clear it (AC-3)");
        assertEquals(RoguePlayer.DiarrheaStage.STAGE_2, pl.getDiarrheaStage(), "Diarrhea survives nourishment too");
    }

    // --- Task 4: movement/stat hooks + AD-5 honesty ---

    @Test
    void crippledMovementCanStumbleOrFreeze() {
        int stumbles = 0, freezes = 0, moves = 0;
        int[][] dirs = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};
        for (long seed = 1; seed <= 40; seed++) {
            RunState s = new RunState(seed);
            RoguePlayer pl = p(s);
            pl.beginRotgut(); // crippled
            RogueTileMap m = s.getTileMap();
            int[] d = null;
            for (int[] cand : dirs) {
                if (m.isWalkable(pl.getTileX() + cand[0], pl.getTileY() + cand[1])) { d = cand; break; }
            }
            if (d == null) continue; // spawned against a wall — skip
            TurnResult r = new TurnEngine().advance(s, PlayerAction.move(d[0], d[1], 3));
            String msg = String.join(" ", r.messages);
            if (msg.contains("freeze")) freezes++;
            else if (msg.contains("stumble")) stumbles++;
            else moves++;
        }
        assertTrue(stumbles > 0, "Crippled movement stumbles (50%)");
        assertTrue(freezes > 0, "Paranoia can freeze movement (25%)");
        assertTrue(moves > 0, "and sometimes still moves (25%)");
    }

    @Test
    void wallBumpCommitsNoDebuffTick() {
        RunState s = new RunState(1L);
        RoguePlayer pl = p(s);
        pl.beginBacterial();
        int timer = pl.getBacterialTimer();
        int dTimer = pl.getDiarrheaTimer();
        int[] toWall = placeAgainstWall(s);
        new TurnEngine().advance(s, PlayerAction.move(toWall[0], toWall[1], 0)); // blocked move
        assertEquals(timer, pl.getBacterialTimer(), "no debuff tick on a wall-bump (AD-5)");
        assertEquals(dTimer, pl.getDiarrheaTimer(), "no Diarrhea escalation on a wall-bump");
    }

    @Test
    void realActionTicksTheDebuffClock() {
        RunState s = new RunState(1L);
        RoguePlayer pl = p(s);
        pl.beginBacterial();
        int timer = pl.getBacterialTimer();
        new TurnEngine().advance(s, PlayerAction.wait(0));
        assertEquals(timer - 1, pl.getBacterialTimer(), "a real action ticks the debuff clock (AD-5)");
    }

    // --- Review regression pins (code review 2026-08-08) ---

    @Test
    void stageTwoDiarrheaDoesNotAccelerateWellFedRegenOrBloated() {
        // Review F-01 (the top finding, blind+edge): the amplified drain uses drainHunger(), so a
        // sick Well-Fed player does NOT regenerate at 3× cadence or shed the Bloated slow 3× faster —
        // the disease must not heal you (AC-1 "lethal if ignored" inverts).
        RunState s = state();
        RoguePlayer pl = p(s);
        pl.eat(100); // SATISFIED → WELL_FED (Bloated slow active)
        assertTrue(pl.isSlowed());
        pl.hurtRaw(10); // 20 → 10 so any regen is observable
        pl.beginBacterial();
        for (int i = 0; i < RoguePlayer.DIARRHEA_STAGE_1_TURNS; i++) DebuffSystem.tick(s, messages()); // → STAGE_2
        assertEquals(RoguePlayer.DiarrheaStage.STAGE_2, pl.getDiarrheaStage());

        // One full acted turn (Hunger + Thirst + Debuff): the single real tickHunger starts the regen
        // clock (3→2, no heal) and sheds the slow once; the 2 extra drainHunger calls touch neither.
        int hunger = pl.getHunger();
        pl.tickHunger();
        ThirstSystem.tick(pl);
        DebuffSystem.tick(s, messages());
        assertEquals(10, pl.getHp(), "no regen acceleration from the amplified drain (F-01)");
        assertEquals(hunger - 3, pl.getHunger(), "the 3× hunger drain still applies");
        assertTrue(pl.isSlowed(), "Bloated slow shed at 1× — still active after one turn");
        // 20 acted turns: 50 - 20 = 30 remain (still slowed); the bug shed 3× → 50 - 60, not slowed.
        for (int i = 0; i < 19; i++) {
            pl.tickHunger();
            ThirstSystem.tick(pl);
            DebuffSystem.tick(s, messages());
        }
        assertTrue(pl.isSlowed(), "Bloated slow not shed 3× faster (F-01)");
    }

    @Test
    void aSecondHerbalCureDoesNotDoubleShortenDelirium() {
        // Review F-04: the ×0.25 applies once — back-to-back cures can't erase the shortened course
        // (40 → 10 → 2 → 0 was the bug).
        RunState s = state();
        RoguePlayer pl = p(s);
        pl.beginBacterial();
        for (int i = 0; i < RoguePlayer.NAUSEA_TURNS + RoguePlayer.FEVER_TURNS; i++) DebuffSystem.tick(s, messages());
        assertEquals(RoguePlayer.BacterialStage.DELIRIUM, pl.getBacterialStage());
        pl.applyHerbalCure(); // 40 → 10
        int timer = pl.getBacterialTimer();
        assertTrue(pl.isDeliriumTreated());
        pl.applyHerbalCure(); // second cure: a no-op (F-04)
        assertEquals(timer, pl.getBacterialTimer(), "a second cure can't shorten a treated Delirium");
    }

    @Test
    void aSickPlayerCanStillEatOrDrinkWhenFullToNourishOut() {
        // Review F-05: AC-3's nourish-out must stay reachable at the full extreme — the refusal only
        // guards "consumption that gains nothing", and a sick player gains the clear.
        RunState s = state();
        RoguePlayer pl = p(s);
        pl.eat(100); // SATISFIED → WELL_FED (canEat false); a fresh player is also HYDRATED (canDrink false)
        pl.beginBacterial(); // NAUSEA — a drink would settle it
        s.getInventory().tryAdd(Supply.BOILED_WATER.ordinal(), 1);
        List<String> msgs = new ArrayList<>();
        assertTrue(ConsumptionSystem.consume(s, Supply.BOILED_WATER.ordinal(), msgs),
                "a sick Well-Fed+HYDRATED player can still drink to nourish-out (F-05)");
        assertEquals(RoguePlayer.BacterialStage.NONE, pl.getBacterialStage(), "the drink settled the sickness");
    }

    @Test
    void toxicMushroomCanBeEatenWhenWellFed() {
        // Review F-08: a mushroom grants no nourishment, so fullness can't refuse it — "the player
        // chose to eat it" (deterministic toxin, no roll).
        RunState s = state();
        RoguePlayer pl = p(s);
        pl.eat(100); // WELL_FED — the provision refusal would otherwise block the mushroom
        s.getInventory().tryAdd(Supply.TOXIC_MUSHROOM.ordinal(), 1);
        List<String> msgs = new ArrayList<>();
        assertTrue(ConsumptionSystem.consume(s, Supply.TOXIC_MUSHROOM.ordinal(), msgs),
                "a toxic mushroom is always eatable (F-08)");
        assertTrue(pl.isRotgutCrippled(), "Rotgut applies while Well Fed");
    }

    @Test
    void honeymoonReDoseDoesNotResetOrRearmTheCountdown() {
        // Review F-03 (Justine: single active countdown): a re-dose while the countdown runs is a
        // no-op (can't postpone Collapse by hoarding), and a post-collapse re-dose can't re-arm a
        // no-op second collapse.
        RunState s = state();
        RoguePlayer pl = p(s);
        pl.beginHoneymoon();
        int start = pl.getHoneymoonCountdown();
        pl.beginHoneymoon();
        assertEquals(start, pl.getHoneymoonCountdown(), "a re-dose can't reset the running countdown (F-03)");
        for (int i = 0; i < RoguePlayer.HONEYMOON_TURNS; i++) DebuffSystem.tick(s, messages());
        assertTrue(pl.isCollapsed(), "the first course collapses");
        pl.beginHoneymoon(); // post-collapse re-dose — must not re-arm a second countdown
        assertEquals(0, pl.getHoneymoonCountdown(), "post-collapse re-dose is a no-op (F-03)");
    }
}

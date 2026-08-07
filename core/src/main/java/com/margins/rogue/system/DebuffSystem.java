package com.margins.rogue.system;

import com.margins.rogue.RoguePlayer;
import com.margins.rogue.item.Supply;
import com.margins.rogue.state.RunState;

import java.util.List;

/**
 * The debuff pipeline step (FR-8, Story 1.7). Owns the onset routes ({@link #applyBacterial},
 * {@link #applyToxin}) and the per-acted-turn tick ({@link #tick}) of the tiered status track on
 * {@code RoguePlayer} — the closed shape DebuffSystem drives but does not own (AD-3, spine line
 * 186). Mirrors {@link HungerSystem}/{@link ThirstSystem}: a {@code System} that ticks {@code
 * RoguePlayer} state on the acted path.
 *
 * <p>The bacterial courses are <b>escalation timers</b>, not auto-clear timers: Nausea→Fever→
 * Delirium advance at each stage's end, and an untreated Delirium timer is latched — turns alone
 * never clear a debuff (AC-3). Diarrhea runs parallel and amplifies the existing thirst/hunger
 * drains by extra {@code tickThirst()}/{@code tickHunger()} calls — no new damage math; the
 * lethality is the existing Parched/Starving cadence. No libGDX types (AD-2).
 */
public final class DebuffSystem {
    private DebuffSystem() {}

    /** Bacterial onset (a failed contamination roll on a risky provision, FR-6): Nausea + parallel
     *  Diarrhea Stage 1. Replaces Story 1.5's flat HP sting — the debuff is the cost (Decision 5). */
    public static void applyBacterial(RunState state, List<String> messages) {
        state.getPlayer().beginBacterial();
        messages.add("Poisoned — you feel sick.");
    }

    /** Toxin onset (a toxic mushroom, FR-8). Deterministic — the player chose to eat it, so there
     *  is no risk roll. Rotgut stacks Nausea + Crippled + Diarrhea; Honeymoon starts a hidden
     *  countdown whose onset message deliberately gives no warning (AC-2). */
    public static void applyToxin(RunState state, Supply.Toxin toxin, List<String> messages) {
        RoguePlayer p = state.getPlayer();
        switch (toxin) {
            case ROTGUT:
                p.beginRotgut();
                messages.add("Toxic mushroom — Rotgut!");
                break;
            case HONEYMOON:
                p.beginHoneymoon();
                messages.add("Sweet as honey...");
                break;
            default:
                break; // NONE never routes here
        }
    }

    /** One acted turn (AD-4): tick the debuff tracks in the fixed order. Runs immediately after
     *  {@code ThirstSystem} so Diarrhea's amplified drain lands before {@code checkLastStand} —
     *  a lethal accelerated drain still honors the one-per-run Last Stand reprieve (AD-5). */
    public static void tick(RunState state, List<String> messages) {
        RoguePlayer p = state.getPlayer();
        tickBacterial(p, messages);
        tickDiarrhea(p, messages);
        tickHoneymoon(p, messages);
    }

    private static void tickBacterial(RoguePlayer p, List<String> messages) {
        RoguePlayer.BacterialStage stage = p.getBacterialStage();
        if (stage == RoguePlayer.BacterialStage.NONE) return;
        // Untreated Delirium's timer is latched: turns alone never clear it (AC-3). A cure item
        // unlatches it (treatDelirium) and the shortened timer then runs down and clears.
        if (stage == RoguePlayer.BacterialStage.DELIRIUM && !p.isDeliriumTreated()) return;
        p.tickBacterialTimer();
        if (p.getBacterialTimer() > 0) return;
        // Course over — escalate, never clear (AC-3).
        switch (stage) {
            case NAUSEA:
                p.escalateToFever();
                messages.add("The sickness deepens into fever.");
                break;
            case FEVER:
                p.escalateToDelirium();
                messages.add("Fever breaks into delirium.");
                break;
            default:
                p.clearBacterial(); // a treated Delirium course ends
                messages.add("The delirium passes.");
                break;
        }
    }

    private static void tickDiarrhea(RoguePlayer p, List<String> messages) {
        RoguePlayer.DiarrheaStage stage = p.getDiarrheaStage();
        if (stage == RoguePlayer.DiarrheaStage.NONE) return;
        // Amplified drain (PRD FR-8): Stage 1 2× Thirst, Stage 2 3× Thirst+Hunger — extra calls to
        // the EXISTING drain paths, so the Parched/Starving damage cadences accelerate (lethal if
        // ignored). Hunger uses drainHunger() (not tickHunger()): the Well Fed regen/Bloated block
        // must not accelerate with the sickness (review F-01 — a disease shouldn't heal you).
        int extraThirst = stage == RoguePlayer.DiarrheaStage.STAGE_1 ? 1 : 2;
        int extraHunger = stage == RoguePlayer.DiarrheaStage.STAGE_2 ? 2 : 0;
        for (int i = 0; i < extraThirst; i++) p.tickThirst();
        for (int i = 0; i < extraHunger; i++) p.drainHunger();
        p.tickDiarrheaTimer();
        if (p.getDiarrheaTimer() > 0) return;
        if (stage == RoguePlayer.DiarrheaStage.STAGE_1) {
            p.escalateDiarrhea(); // STAGE_2; its timer latches at 0 → 3× drain forever
            messages.add("Diarrhea worsens.");
        }
        // STAGE_2 at 0 stays STAGE_2 at 0 — lethal if ignored, as designed (AC-1).
    }

    private static void tickHoneymoon(RoguePlayer p, List<String> messages) {
        if (p.getHoneymoonCountdown() <= 0) return;
        p.tickHoneymoon();
        if (p.getHoneymoonCountdown() <= 0) {
            p.collapse(); // Max HP capped at 40% of base until a cure lifts it (AC-2)
            messages.add("Your body gives out — collapse!");
        }
    }
}

package com.margins.rogue.system;

import com.margins.rogue.RoguePlayer;

import java.util.List;

/**
 * Turn pipeline step: advance hunger (AD-4). Extracted from the screen. Emits the hunger
 * tier-change notable event (Story 1.8 AC-2): when a tick crosses a tier boundary, an SPD line
 * names the new tier. Only DROPS are observable here (tickHunger never rises) — the rise path
 * (eating back up) emits from {@link ConsumptionSystem}, the System that drives eat().
 */
public final class HungerSystem {
    private HungerSystem() {}

    /** One acted turn: drain hunger, emitting an SPD line when a tier boundary is crossed. */
    public static void tick(RoguePlayer player, List<String> messages) {
        RoguePlayer.HungerStatus before = player.getStatus();
        player.tickHunger();
        if (player.getStatus() != before) messages.add(hungerTierLine(player.getStatus()));
    }

    /** SPD text-forward line for a hunger tier (shared with ConsumptionSystem for the rise path). */
    public static String hungerTierLine(RoguePlayer.HungerStatus status) {
        switch (status) {
            case STARVING: return "Hunger bites — you're Starving.";
            case HUNGRY:   return "Hunger gnaws — you're Hungry.";
            case WELL_FED: return "You're Well Fed.";
            default:       return "Your belly is Satisfied.";
        }
    }
}

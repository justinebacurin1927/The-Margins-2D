package com.margins.rogue.system;

import com.margins.rogue.RoguePlayer;

import java.util.List;

/**
 * Turn pipeline step: advance thirst (AD-4). Mirrors {@link HungerSystem}. Emits the thirst
 * tier-change notable event (Story 1.8 AC-2): when a tick crosses a tier boundary, an SPD line
 * names the new tier. Only DROPS are observable here (tickThirst never rises) — the rise path
 * (drinking back up) emits from {@link ConsumptionSystem}, the System that drives drink().
 */
public final class ThirstSystem {
    private ThirstSystem() {}

    /** One acted turn: advance the thirst countdown, emitting an SPD line on a tier drop. */
    public static void tick(RoguePlayer player, List<String> messages) {
        RoguePlayer.ThirstStatus before = player.getThirstStatus();
        player.tickThirst();
        if (player.getThirstStatus() != before) messages.add(thirstTierLine(player.getThirstStatus()));
    }

    /** SPD text-forward line for a thirst tier (shared with ConsumptionSystem for the rise path). */
    public static String thirstTierLine(RoguePlayer.ThirstStatus status) {
        switch (status) {
            case PARCHED:    return "Thirst closes in — you're Parched.";
            case DEHYDRATED: return "Thirst bites — you're Dehydrated.";
            case HYDRATED:   return "You're Hydrated.";
            default:         return "You're getting Thirsty.";
        }
    }
}

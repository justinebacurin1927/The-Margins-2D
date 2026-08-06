package com.margins.rogue.system;

import com.margins.rogue.RoguePlayer;

/** Turn pipeline step: advance temperature exposure (AD-4). Mirrors {@link HungerSystem}. */
public final class TemperatureSystem {
    private TemperatureSystem() {}

    public static void tick(RoguePlayer player) {
        player.tickTemperature();
    }
}

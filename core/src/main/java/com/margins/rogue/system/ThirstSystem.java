package com.margins.rogue.system;

import com.margins.rogue.RoguePlayer;

/** Turn pipeline step: advance thirst (AD-4). Mirrors {@link HungerSystem}. */
public final class ThirstSystem {
    private ThirstSystem() {}

    public static void tick(RoguePlayer player) {
        player.tickThirst();
    }
}

package com.margins.rogue.system;

import com.margins.rogue.RoguePlayer;

/** Turn pipeline step: advance hunger (AD-4). Extracted from the screen. */
public final class HungerSystem {
    private HungerSystem() {}

    public static void tick(RoguePlayer player) {
        player.tickHunger();
    }
}

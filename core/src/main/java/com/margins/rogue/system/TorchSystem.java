package com.margins.rogue.system;

import com.margins.rogue.state.RunState;

/**
 * Turn pipeline step: burn the torch down (FR-7, Story 1.6). Mirrors {@link LightSystem}: the
 * torch is a carried light (AD-18) with a finite burn. {@link #tick} runs on the acted path
 * immediately before {@link LightSystem#emitNoise}, so the light — and the noise it emits — always
 * reflects the torch's state for this turn (a just-expired torch neither lights nor lures).
 */
public final class TorchSystem {
    private TorchSystem() {}

    /** Torch burn (AC-3): ~one 70-turn Night of carried light, spent on acted turns. */
    public static final int TORCH_BURN = 60;

    /** One acted turn: burn the torch down (a no-op when none is lit). */
    public static void tick(RunState state) {
        state.tickTorch();
    }
}

package com.margins.rogue.system;

import com.margins.rogue.state.RunState;

/**
 * The light's ambient noise emitter (AD-9, AD-18, Story 1.4). A lit source
 * (campfire/torch) is bright and audible to patrols (FR-7): each acted turn it
 * emits one {@link com.margins.rogue.NoiseEvent} at the light's tile, enqueued
 * just before the Noise-resolve step so {@link NoiseSystem} consumes it the same
 * turn. This is the ONE mechanism by which light alerts enemies (AD-18) — the
 * noise channel, which is Euclidean and LOS-ignoring, so a patrol behind a wall
 * still hears the fire. No render types (AD-2).
 */
public final class LightSystem {
    public static final int LIGHT_NOISE_RADIUS = 6; // PRD Balance: the fire carries this far

    private LightSystem() {}

    /** Emit the active light's per-turn noise, if any (no-op when unlit). */
    public static void emitNoise(RunState state) {
        if (state.hasLight()) {
            state.emitNoise(state.getLightX(), state.getLightY(), LIGHT_NOISE_RADIUS);
        }
    }
}

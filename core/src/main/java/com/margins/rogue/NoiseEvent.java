package com.margins.rogue;

/**
 * A single-turn noise stimulus at a tile (AD-9). Produced by systems/abilities
 * via {@code RunState.emitNoise} and consumed by the NoiseSystem on the AI tick.
 * Lives only on the transient noise queue — never serialized.
 */
public class NoiseEvent {
    public final int x;
    public final int y;
    public final int radius;

    public NoiseEvent(int x, int y, int radius) {
        this.x = x;
        this.y = y;
        this.radius = radius;
    }
}

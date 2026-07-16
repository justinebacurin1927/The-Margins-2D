package com.margins.rogue;

/**
 * Awareness state of an enemy (AD-9). Only {@link #ALERTED} enemies pursue and
 * attack the player; {@link #UNAWARE} and {@link #SUSPICIOUS} idle-wander.
 * Transitions between states are wired in Story 2.4.
 */
public enum Detection {
    UNAWARE,
    SUSPICIOUS,
    ALERTED
}

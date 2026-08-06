package com.margins.rogue;

/**
 * Day/Night phase of the 170-turn clock (PRD FR-5): Day 100 turns / Night 70 per cycle.
 * The phase is DERIVED from {@code RunState.clockTurns} (never persisted itself), so a
 * load can never desync it. Night's effects (FOV shrink, aggression) are later stories.
 * No libGDX types (AD-2).
 */
public enum DayPhase {
    DAY, NIGHT
}

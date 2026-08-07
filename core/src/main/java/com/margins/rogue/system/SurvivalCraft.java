package com.margins.rogue.system;

import com.margins.rogue.state.RunState;

/**
 * Shared SKILL-check for survival crafting (FR-6/FR-11, Story 1.5): cooking and
 * filtration succeed on a roll whose chance scales with the player's SKILL — the
 * horizontal-growth axis. Boiling is deterministic (fuel, not skill), so it does
 * not use this. Tuning is PRD Balance. No libGDX types (AD-2).
 */
final class SurvivalCraft {
    private SurvivalCraft() {}

    /** Percent success chance (0..95) for a SKILL-governed craft: base 40 + 8 per SKILL point.
     *  Clamped both ways — an extreme or negative SKILL must never leave the valid roll range
     *  (a negative chance would make every roll "succeed" via nextInt(100) < negative, Edge #3). */
    static int skillChance(RunState state) {
        return Math.max(0, Math.min(95, 40 + state.getPlayer().getSkill() * 8));
    }
}

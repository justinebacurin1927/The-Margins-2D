package com.margins.dialog;

/**
 * The stat a {@link DialogOption} gate is evaluated against (FR-19, Story 2.1).
 * FR-19 makes VOICE the primary gate stat (talk down a patrol, haggle, calm
 * animals, press for lore) with occasional INSTINCT gates — a node's choice
 * carries its gating stat explicitly rather than hardcoding one. Pure model
 * (AD-2), the same package as {@link DialogNode}.
 */
public enum GateStat {
    INSTINCT,
    VOICE
}

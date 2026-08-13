package com.margins.rogue;

/**
 * The remake's companion roster of four (FR-15, AD-10): Aldric is the combat companion; Mara,
 * Old Fen, and Yenna are non-combatants. Exactly one is <em>active</em> at a time — materialized
 * as the single positioned {@link Companion} tile-agent — while the other three exist only as
 * abstract {@code FlagStore}/Bond entries (no tile, no noise, no survival tick) until activated.
 *
 * <p>{@link #bindId()} is the stable label that keys per-companion Bond ({@code "bond." + bindId})
 * and later art/dialogue (AD-7). {@link #isCombatant()} gates the combat behavior the AI grants
 * (Story 5.2/5.4) — only Aldric fights.
 */
public enum CompanionId {
    ALDRIC(true, "aldric", "Aldric"),
    MARA(false, "mara", "Mara"),
    OLD_FEN(false, "old_fen", "Old Fen"),
    YENNA(false, "yenna", "Yenna");

    private final boolean combatant;
    private final String bindId;
    private final String displayName;

    CompanionId(boolean combatant, String bindId, String displayName) {
        this.combatant = combatant;
        this.bindId = bindId;
        this.displayName = displayName;
    }

    /** Whether this companion fights through the combat authority (Aldric only, FR-15/AD-10). */
    public boolean isCombatant() { return combatant; }

    /** The stable label keying Bond and art/dialogue ({@code "bond." + bindId}). */
    public String bindId() { return bindId; }

    /** The name shown in observation lines (Story 5.2 — e.g. "Mara panics!"). */
    public String displayName() { return displayName; }
}

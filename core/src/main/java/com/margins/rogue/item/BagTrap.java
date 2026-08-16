package com.margins.rogue.item;

import com.margins.rogue.RoguePlayer;

import java.util.List;

/**
 * Story 6.2 (FR-20, AC-2): a bag's thematic trap. A world-found bag may be trapped (rolled when it
 * is readied — see {@code BagSystem}); the trap stays hidden until it {@link #fire fires}, then the
 * bag breaks and spills its contents (75% recoverable, 25% lost). The starting Traveler's Pack is
 * {@link #NONE untrapped}.
 *
 * <p>Effects reuse the existing player-harm surface (no new damage system, AD-10): DART is a spike of
 * raw HP loss, FIRE scorches (heat + a small burn), FREEZE chills (cold). Headless (AD-2) — the
 * effect is a pure {@link RoguePlayer} mutation plus an observed log line (observation discipline).
 */
public enum BagTrap {
    NONE {
        @Override public void fire(RoguePlayer p, List<String> messages) { /* untrapped — never fires */ }
    },
    DART("A dart springs from the pack — it punches into you.", 4) {
        @Override public void fire(RoguePlayer p, List<String> messages) {
            p.hurtRaw(damage);
            messages.add(line);
        }
    },
    FIRE("The pack bursts into flame — it scorches you.", 2) {
        @Override public void fire(RoguePlayer p, List<String> messages) {
            p.adjustTemperature(TEMPERATURE_SWING);   // a hot flare
            p.hurtRaw(damage);                        // and a small burn
            messages.add(line);
        }
    },
    FREEZE("The pack shatters with frost — the cold bites deep.", 0) {
        @Override public void fire(RoguePlayer p, List<String> messages) {
            p.adjustTemperature(-TEMPERATURE_SWING);  // a cold snap
            messages.add(line);
        }
    };

    /** How far a FIRE/FREEZE trap swings the exposure meter (PRD Balance — tunable content). */
    public static final int TEMPERATURE_SWING = 30;

    final String line;
    final int damage;

    BagTrap() { this("", 0); }
    BagTrap(String line, int damage) {
        this.line = line;
        this.damage = damage;
    }

    /** Apply the trap's effect to Klein and announce it (no-op for {@link #NONE}). */
    public abstract void fire(RoguePlayer p, List<String> messages);

    /** The trap types that can actually spring on a found bag (excludes {@link #NONE}). */
    public static final BagTrap[] ARMED = { DART, FIRE, FREEZE };
}

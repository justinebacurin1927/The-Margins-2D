package com.margins.rogue;

/**
 * The three shapes a companion loss can take (Story 5.5, FR-17, AD-7) — each matching the game's
 * permadeath weight in its own way:
 *
 * <ul>
 *   <li><b>NONE</b> — not lost (the default / never-set state, AD-6).</li>
 *   <li><b>CAPTURED</b> — taken alive; recoverable via a rescue quest (the Story-2.4 Aldric beat).</li>
 *   <li><b>DEPARTED</b> — walked away at low Bond (or was betrayed); gone from the run.</li>
 *   <li><b>DEAD</b> — killed; permanent, the corpse stays where it fell.</li>
 * </ul>
 */
public enum CompanionLoss {
    NONE,
    CAPTURED,
    DEPARTED,
    DEAD;

    /** Only a Captured companion can be won back (via the rescue quest); departure and death cannot. */
    public boolean recoverable() {
        return this == CAPTURED;
    }
}

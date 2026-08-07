package com.margins.rogue.system;

import com.margins.rogue.RoguePlayer;
import com.margins.rogue.Weather;
import com.margins.rogue.state.RunState;

/**
 * Turn pipeline step: advance temperature exposure (AD-4). Mirrors {@link HungerSystem}.
 *
 * <p>Story 1.6 adds the real drivers the meter was waiting on (FR-4):
 * <ul>
 *   <li><b>Cold Snap</b> (the weather driver, AC-1): −2/turn → Frozen (≤ −80) in 40 turns, inside
 *       one 70-turn Night. Recovery outside a Snap is +1/turn — half the onset — the driver-less
 *       drift in {@link RoguePlayer#tickTemperature()}.</li>
 *   <li><b>Campfire warmth</b> (AC-2): +4/turn on the fire tile, capped at the top of the WARM
 *       band so a fire never pushes into HOT/OVERHEATED. Fire beats a Cold Snap (net +2/turn).</li>
 * </ul>
 * Extreme-band harm (Frozen/Overheated) applies AFTER the delta on the driver branches (the
 * post-delta band decides — dropping into Frozen harms that turn, warming out of it does not) and
 * after the drift on the neutral path — see {@link RoguePlayer#tickTemperature()}.
 */
public final class TemperatureSystem {
    private TemperatureSystem() {}

    /** Cold Snap onset: −2/turn (AC-1, starting calibration). */
    public static final int COLD_SNAP_ONSET = 2;
    /** Campfire warmth: +4/turn on the fire tile (AC-2). */
    public static final int FIRE_WARMTH = 4;
    /** Warmth cap — the top of the WARM band (&lt; 50). "+50" would land exactly in HOT, so the cap
     *  is 49: a fire warms you to comfortable but never into HOT/OVERHEATED (AC-2 guard). */
    public static final int FIRE_COMFORT = 49;

    /** One acted turn: apply the weather + fire drivers, then the extreme-band harm. */
    public static void tick(RunState state) {
        RoguePlayer p = state.getPlayer();
        boolean coldSnap = state.getWeather() == Weather.COLD_SNAP;
        if (state.isPlayerAtFire()) {
            // Fire warmth beats Cold Snap: the player nets +2/turn on the fire tile under a Snap
            // (4 warmth − 2 onset) — the fire genuinely "solves warmth" (AC-2).
            p.warmTo(FIRE_WARMTH - (coldSnap ? COLD_SNAP_ONSET : 0), FIRE_COMFORT);
        } else if (coldSnap) {
            p.adjustTemperature(-COLD_SNAP_ONSET);
        } else {
            // The driver-less baseline: drift toward Neutral at half the onset rate (+1/turn).
            p.tickTemperature(); // includes its own harm (harm-then-drift ordering)
            return;
        }
        p.tickTemperatureHarm(); // post-delta band decides the harm on the driver branches
    }
}

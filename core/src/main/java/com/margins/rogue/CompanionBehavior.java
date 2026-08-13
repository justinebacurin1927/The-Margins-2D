package com.margins.rogue;

/**
 * The active companion's autonomous behavior states (Story 5.2, FR-16, AD-10). The
 * {@code CompanionSystem} computes and executes one of these each player-acted turn:
 *
 * <ul>
 *   <li><b>FOLLOW</b> — a combatant trailing its rear-guard station (the calm default).</li>
 *   <li><b>HOLD</b> — stay put on a standing order (set by Story 5.3).</li>
 *   <li><b>HIDE</b> — stay put and quiet on a standing order (emits no noise, Story 5.3).</li>
 *   <li><b>DISTRACT</b> — the one-shot shout ({@code CompanionSystem.distract}); listed for completeness.</li>
 *   <li><b>FIGHT_RETREAT</b> — a combatant engaging a threat and returning to station when it clears.</li>
 *   <li><b>TAKE_COVER</b> — a non-combatant trailing the player, never fighting (the calm default).</li>
 *   <li><b>FLEE</b> — a non-combatant panicking away from a threat, emitting stealth-blowing noise.</li>
 * </ul>
 */
public enum CompanionBehavior {
    FOLLOW,
    HOLD,
    HIDE,
    DISTRACT,
    FIGHT_RETREAT,
    TAKE_COVER,
    FLEE
}

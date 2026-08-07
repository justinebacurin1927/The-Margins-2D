package com.margins.rogue.system;

import com.margins.rogue.Detection;
import com.margins.rogue.RogueEnemy;
import com.margins.rogue.RoguePlayer;
import com.margins.rogue.RogueTileMap;
import com.margins.rogue.state.RunState;

import java.util.List;

/**
 * Advances each enemy's {@link Detection} state (AD-9): radius + line-of-sight,
 * no directional cones. Runs as a pipeline step before enemy movement (AD-4).
 * Escalation on sustained sight, gradual de-escalation once the stimulus stops.
 * Tuning is PRD Balance.
 */
public final class DetectionSystem {
    public static final int VISION_RANGE = 6;              // PRD Balance: enemy vision range
    public static final int ALERT_AFTER_SIGHT_TURNS = 2;   // Suspicious -> Alerted after 2 consecutive in-sight turns
    public static final int DEESCALATE_AFTER_CALM_TURNS = 3; // drop 1 step per 3 turns without stimulus

    private DetectionSystem() {}

    public static void update(RunState state, List<String> messages) {
        RogueTileMap map = state.getTileMap();
        RoguePlayer player = state.getPlayer();
        int px = player.getTileX();
        int py = player.getTileY();

        // Story 1.8 AC-2: at most one line per escalation KIND per turn (the first UNAWARE→SUSPICIOUS
        // and the first SUSPICIOUS→ALERTED) — a full garrison escalating together reads once, not N times.
        boolean someoneSpotted = false;
        boolean someoneAlerted = false;

        for (RogueEnemy e : state.getEnemies()) {
            if (!e.isAlive()) continue;

            if (canSee(map, e, px, py)) {
                e.setCalmTurns(0);
                e.setSightTurns(e.getSightTurns() + 1);
                e.setLastSeen(px, py);
                if (e.getDetection() == Detection.UNAWARE) {
                    e.setDetection(Detection.SUSPICIOUS);
                    if (!someoneSpotted) {
                        messages.add("Something stirs in the trees.");
                        someoneSpotted = true;
                    }
                } else if (e.getDetection() == Detection.SUSPICIOUS
                        && e.getSightTurns() >= ALERT_AFTER_SIGHT_TURNS) {
                    e.setDetection(Detection.ALERTED);
                    if (!someoneAlerted) {
                        messages.add("A patrol has spotted you!");
                        someoneAlerted = true;
                    }
                }
            } else {
                e.setSightTurns(0);
                if (e.getDetection() != Detection.UNAWARE) {
                    e.setCalmTurns(e.getCalmTurns() + 1);
                    if (e.getCalmTurns() >= DEESCALATE_AFTER_CALM_TURNS) {
                        e.setCalmTurns(0);
                        e.setDetection(deescalate(e.getDetection()));
                    }
                }
            }
        }
    }

    private static boolean canSee(RogueTileMap map, RogueEnemy e, int px, int py) {
        int dx = px - e.getTileX();
        int dy = py - e.getTileY();
        if (dx * dx + dy * dy > VISION_RANGE * VISION_RANGE) return false;
        return FovSystem.hasLineOfSight(map, e.getTileX(), e.getTileY(), px, py);
    }

    private static Detection deescalate(Detection d) {
        return d == Detection.ALERTED ? Detection.SUSPICIOUS : Detection.UNAWARE;
    }
}

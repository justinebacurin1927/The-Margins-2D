package com.margins.rogue.system;

import com.margins.rogue.Detection;
import com.margins.rogue.NoiseEvent;
import com.margins.rogue.RogueEnemy;
import com.margins.rogue.state.RunState;

import java.util.List;

/**
 * The "Noise resolve" pipeline step (AD-4, AD-9). Each queued {@link NoiseEvent}
 * draws nearby living enemies toward its origin: an UNAWARE enemy rises to
 * SUSPICIOUS and every in-radius enemy retargets its investigation at the noise
 * and has its de-escalation reset. Enemies are drawn to the sound rather than
 * forced to ALERTED (ALERTED chases the player, not the noise) — so noise lures.
 * If an enemy then spots the player there, DetectionSystem escalates it (2.4).
 * The queue is single-turn and cleared here. Radius is Euclidean.
 */
public final class NoiseSystem {
    private NoiseSystem() {}

    public static void resolve(RunState state) {
        List<NoiseEvent> queue = state.getNoiseQueue();
        if (queue.isEmpty()) return;
        for (NoiseEvent n : queue) {
            for (RogueEnemy e : state.getEnemies()) {
                if (!e.isAlive()) continue;
                int dx = e.getTileX() - n.x;
                int dy = e.getTileY() - n.y;
                if (dx * dx + dy * dy > n.radius * n.radius) continue;
                if (e.getDetection() == Detection.UNAWARE) {
                    e.setDetection(Detection.SUSPICIOUS);
                }
                e.setLastSeen(n.x, n.y); // investigate the sound
                e.setCalmTurns(0);       // noise is a stimulus — holds off de-escalation
            }
        }
        queue.clear();
    }
}

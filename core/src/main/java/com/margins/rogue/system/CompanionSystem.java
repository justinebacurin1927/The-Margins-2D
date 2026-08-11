package com.margins.rogue.system;

import com.margins.rogue.Companion;
import com.margins.rogue.Detection;
import com.margins.rogue.RogueEnemy;
import com.margins.rogue.RoguePlayer;
import com.margins.rogue.state.RunState;

import java.util.List;

/**
 * Turn pipeline step (AD-4): the active companion takes its AI turn in the
 * Companion+Enemy-AI phase — after detection, before the enemy phase — so the
 * ally acts as part of that phase rather than being inlined into input/rendering
 * (AD-10).
 *
 * <p>Aldric FOLLOWS with purpose — not a tail glued to Klein, not aimless drifting:
 * <ul>
 *   <li><b>ENGAGE</b> — an ALERTED enemy within {@link #REACTION_RADIUS} of the PLAYER is a
 *       threat. Aldric closes on it with a greedy one-tile step (the enemy takeTurn pattern) and
 *       strikes it when adjacent. This runs AFTER DetectionSystem, so a threat that spots Klein
 *       this same turn is already ALERTED and met head-on.</li>
 *   <li><b>FOLLOW</b> — otherwise he holds a rear-guard station {@link #STATION_DIST} tiles
 *       directly behind the player (opposite the facing) and trails there — watching Klein's back
 *       at a distance. A fight or an obstacle carries him off-station; each calm turn he closes
 *       back to it.</li>
 * </ul>
 * Occupancy applies to his movement like everyone else's: he never steps onto the player or an
 * enemy tile (the player may walk over HIM — allies share tiles). Writes only the strike line
 * (observation discipline); a dead companion is inert.
 */
public final class CompanionSystem {
    /** PRD Balance: how far a shout travels. Louder than an attack swing (4); matches enemy vision (6). */
    public static final int DISTRACTION_RADIUS = 6;

    /** Aldric notices ALERTED enemies threatening the PLAYER within this radius — a bit wider than
     *  his station distance, so he can step out to intercept before the threat closes on Klein. */
    public static final int REACTION_RADIUS = 8;
    /** The rear-guard station: this many tiles directly behind the player (opposite the facing) —
     *  an empty tile or two between him and Klein, so he follows with purpose instead of tailing. */
    public static final int STATION_DIST = 2;

    private CompanionSystem() {}

    /** The companion's AI turn: engage the nearest threat, else follow to his rear-guard station.
     *  A dead companion is inert. */
    public static void follow(RunState state, List<String> messages) {
        Companion c = state.getActiveCompanion();
        if (c == null || !c.isAlive()) return;
        RoguePlayer player = state.getPlayer();
        RogueEnemy threat = nearestThreat(state, player);
        if (threat != null) {
            if (c.isAdjacentTo(threat.getTileX(), threat.getTileY())) {
                threat.takeDamage(c.getDamage());
                messages.add("Aldric strikes for " + c.getDamage() + "!");
                if (!threat.isAlive()) messages.add("Enemy defeated.");
            } else {
                stepToward(state, c, threat.getTileX(), threat.getTileY(), player);
            }
            return;
        }
        int[] station = rearStation(state, player);
        stepToward(state, c, station[0], station[1], player);
    }

    /** The nearest ALERTED enemy within REACTION_RADIUS of the player — Aldric's mandate is to
     *  defend Klein, wherever Aldric himself drifted. Unaware/suspicious enemies are left alone
     *  (he doesn't pull aggro); a threat that isn't yet committed to the chase isn't engaged. */
    private static RogueEnemy nearestThreat(RunState state, RoguePlayer player) {
        RogueEnemy best = null;
        int bestDist = Integer.MAX_VALUE;
        for (RogueEnemy e : state.getEnemies()) {
            if (!e.isAlive() || e.getDetection() != Detection.ALERTED) continue;
            int d = Math.abs(e.getTileX() - player.getTileX()) + Math.abs(e.getTileY() - player.getTileY());
            if (d <= REACTION_RADIUS && d < bestDist) {
                best = e;
                bestDist = d;
            }
        }
        return best;
    }

    /** One greedy step toward (tx,ty) — the enemy takeTurn pattern — onto walkable tiles only,
     *  never onto the player's tile or an enemy's tile. */
    private static void stepToward(RunState state, Companion c, int tx, int ty, RoguePlayer player) {
        int dx = Integer.compare(tx, c.getTileX());
        boolean moved = false;
        if (dx != 0) {
            int nx = c.getTileX() + dx;
            if (!(nx == player.getTileX() && c.getTileY() == player.getTileY())
                    && !state.isOccupiedByEnemy(nx, c.getTileY())) {
                c.stepTo(nx, c.getTileY());
                moved = true;
            }
        }
        int dy = Integer.compare(ty, c.getTileY());
        if (!moved && dy != 0) {
            int ny = c.getTileY() + dy;
            if (!(c.getTileX() == player.getTileX() && ny == player.getTileY())
                    && !state.isOccupiedByEnemy(c.getTileX(), ny)) {
                c.stepTo(c.getTileX(), ny);
            }
        }
    }

    /** The rear-guard station: {@link #STATION_DIST} tiles behind the player (opposite the facing),
     *  falling back to one behind, then the two shoulder tiles — the first walkable, un-occupied
     *  tile. The player's own tile is the last resort: {@link #stepToward} holds rather than step
     *  onto the player, so Aldric waits as close to his post as he can get. */
    private static int[] rearStation(RunState state, RoguePlayer player) {
        int px = player.getTileX(), py = player.getTileY();
        int dx = RoguePlayer.directionX(player.getFacing());
        int dy = RoguePlayer.directionY(player.getFacing());
        int[][] candidates = {
                {px - dx * STATION_DIST, py - dy * STATION_DIST}, // the post, two behind
                {px - dx, py - dy},                               // one behind (wall/packed post)
                {px - dx * STATION_DIST + dy, py - dy * STATION_DIST + dx}, // shoulder
                {px - dx * STATION_DIST - dy, py - dy * STATION_DIST - dx}, // other shoulder
        };
        for (int[] t : candidates) {
            if (state.getTileMap().isWalkable(t[0], t[1]) && !state.isOccupiedByEnemy(t[0], t[1])) {
                return t;
            }
        }
        return new int[]{px, py};
    }

    /**
     * Galleon shouts: emit a Noise event at the companion's position (AD-9/AD-10 —
     * Distraction produces noise; it never touches enemies directly). Returns true
     * if a turn was committed. Refused without a turn when there's no companion or
     * the per-floor limit is spent (FR-14).
     */
    public static boolean distract(RunState state, List<String> messages) {
        Companion c = state.getActiveCompanion();
        if (c == null) {
            messages.add("No companion to call on.");
            return false;
        }
        if (!c.canDistract()) {
            messages.add("Galleon has no shouts left this floor.");
            return false;
        }
        c.useDistraction();
        state.emitNoise(c.getTileX(), c.getTileY(), DISTRACTION_RADIUS);
        messages.add("Galleon shouts!");
        return true;
    }
}

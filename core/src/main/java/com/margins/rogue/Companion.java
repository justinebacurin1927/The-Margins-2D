package com.margins.rogue;

/**
 * An allied tile-actor that travels with the player (AD-10). Pure model — no
 * libGDX render types (AD-2); the screen picks the sprite. Generic by design:
 * {@code bindId} is a plain label distinguishing Erik vs Galleon for later
 * art/dialogue. Follows with a greedy one-tile step per turn, stopping once
 * adjacent (allies don't shove the player).
 */
public class Companion {
    /** Distraction uses per floor (FR-14; "2 per floor or 6-turn cooldown" — we chose per-floor). */
    public static final int MAX_DISTRACTIONS_PER_FLOOR = 2;

    private int tileX, tileY;
    private String bindId;              // plain label for later art/dialogue ("erik"/"galleon")
    private int distractionsLeft = MAX_DISTRACTIONS_PER_FLOOR; // per-floor use limit (FR-14); persisted
    private transient RogueTileMap map;

    private Companion() {} // for libGDX Json deserialization; map re-injected via setMap

    public Companion(int x, int y, RogueTileMap map, String bindId) {
        this.tileX = x;
        this.tileY = y;
        this.map = map;
        this.bindId = bindId;
    }

    public int getTileX() { return tileX; }
    public int getTileY() { return tileY; }
    public String getBindId() { return bindId; }

    /** Whether Distraction is available this floor (FR-14). */
    public boolean canDistract() { return distractionsLeft > 0; }

    /** Spend one Distraction use (floored at 0). Caller gates on {@link #canDistract()}. */
    public void useDistraction() { distractionsLeft = Math.max(0, distractionsLeft - 1); }

    public int getDistractionsLeft() { return distractionsLeft; }

    /** Refill the per-floor allowance (called on descent — FR-14). */
    public void resetDistractions() { distractionsLeft = MAX_DISTRACTIONS_PER_FLOOR; }

    /** Re-inject the tilemap after a save load (map is transient — AD-6). */
    public void setMap(RogueTileMap map) { this.map = map; }

    /** Reposition the companion (used on descent to place it at the new entrance). */
    public void placeAt(int x, int y) {
        this.tileX = x;
        this.tileY = y;
    }

    /**
     * One greedy step toward the target, reusing {@link RogueEnemy#takeTurn}'s
     * pattern — try the x step first, then the y step — only onto walkable
     * tiles and never onto the target tile. Stops once adjacent (or already on
     * the target) so an ally doesn't crowd the player. Deterministic; no RNG.
     */
    public void followStep(int targetX, int targetY) {
        if (isAdjacentTo(targetX, targetY) || (tileX == targetX && tileY == targetY)) return;
        int dx = Integer.compare(targetX, tileX);
        boolean moved = false;
        if (dx != 0) {
            int nx = tileX + dx;
            if (nx != targetX || tileY != targetY) { // never step onto the target tile
                if (map.isWalkable(nx, tileY)) {
                    tileX = nx;
                    moved = true;
                }
            }
        }
        int dy = Integer.compare(targetY, tileY);
        if (!moved && dy != 0) {
            int ny = tileY + dy;
            if (tileX != targetX || ny != targetY) {
                if (map.isWalkable(tileX, ny)) {
                    tileY = ny;
                }
            }
        }
    }

    public boolean isAdjacentTo(int px, int py) {
        return Math.abs(tileX - px) + Math.abs(tileY - py) == 1;
    }
}

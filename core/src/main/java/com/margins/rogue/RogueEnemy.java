package com.margins.rogue;

import java.util.Random;

public class RogueEnemy {
    private int tileX, tileY;
    private int hp, maxHp;
    private int damage;
    private int ag = 3; // Story 4.1 (FR-12, D3): the enemy-phase turn-order key (highest AG acts first).
                        // Field-initialized (AD-6): the no-arg Json ctor sets no stats, so a pre-4.1
                        // save (no ag key) loads the deterministic slow-Giliman 3.
    private boolean alive;
    private boolean justArrived;
    private Detection detection = Detection.UNAWARE;
    private int sightTurns;          // consecutive turns the player has been in sight
    private int calmTurns;           // consecutive turns without a detection stimulus
    private int lastSeenX = -1;      // last tile the player was seen on (for suspicious investigation)
    private int lastSeenY = -1;
    private transient RogueTileMap map;

    private static final int[] WDX = {0, 0, 1, -1, 0}; // N, S, E, W, stay
    private static final int[] WDY = {1, -1, 0, 0, 0};

    private RogueEnemy() {} // for libGDX Json deserialization; map re-injected via setMap

    public RogueEnemy(int x, int y, RogueTileMap map) {
        this.tileX = x;
        this.tileY = y;
        this.map = map;
        this.hp = 8;
        this.maxHp = 8;
        this.damage = 3;
        this.ag = 3;
        this.alive = true;
        this.justArrived = false;
    }

    public int getTileX() { return tileX; }
    public int getTileY() { return tileY; }
    public int getHp() { return hp; }
    public int getMaxHp() { return maxHp; }
    public int getDamage() { return damage; }
    public int getAg() { return ag; }
    /** Set AG (Story 4.1): the enemy-phase order key; tests + enemy-variety set it (default 3, D3). */
    public void setAg(int value) { ag = value; }
    public boolean isAlive() { return alive; }
    public boolean hasJustArrived() { return justArrived; }
    public void setJustArrived(boolean v) { justArrived = v; }

    public void takeDamage(int amount) {
        hp = Math.max(0, hp - amount);
        if (hp <= 0) alive = false;
    }

    /** {@code blockedX/blockedY} is an actor tile this enemy may never occupy (the living
     *  companion, or -1,-1 for none) — combat fix #2: an enemy stops short of the companion
     *  exactly as it stops short of the player, so nobody shares a tile. */
    public void takeTurn(int playerX, int playerY, int blockedX, int blockedY) {
        if (!alive) return;
        int dx = Integer.compare(playerX, tileX);
        int dy = Integer.compare(playerY, tileY);
        boolean moved = false;
        if (dx != 0) {
            int nx = tileX + dx;
            if (nx == playerX && tileY == playerY) return;
            if (nx == blockedX && tileY == blockedY) return;
            if (map.isWalkable(nx, tileY)) {
                tileX = nx;
                moved = true;
            }
        }
        if (!moved && dy != 0) {
            int ny = tileY + dy;
            if (tileX == playerX && ny == playerY) return;
            if (tileX == blockedX && ny == blockedY) return;
            if (map.isWalkable(tileX, ny)) {
                tileY = ny;
                moved = true;
            }
        }
        if (moved && isAdjacentTo(playerX, playerY)) {
            justArrived = true;
        }
    }

    public boolean isAdjacentTo(int px, int py) {
        return Math.abs(tileX - px) + Math.abs(tileY - py) == 1;
    }

    public Detection getDetection() { return detection; }
    public void setDetection(Detection d) { this.detection = d; }

    public int getSightTurns() { return sightTurns; }
    public void setSightTurns(int v) { this.sightTurns = v; }
    public int getCalmTurns() { return calmTurns; }
    public void setCalmTurns(int v) { this.calmTurns = v; }
    public int getLastSeenX() { return lastSeenX; }
    public int getLastSeenY() { return lastSeenY; }
    public void setLastSeen(int x, int y) { this.lastSeenX = x; this.lastSeenY = y; }

    /** Idle-wander for an unaware enemy: a random walkable step (or stay), never onto the player or
     *  the blocked actor tile (the living companion; -1,-1 for none) — AD-5 + combat fix #2. */
    public void wander(Random rng, int avoidX, int avoidY, int blockedX, int blockedY) {
        if (!alive) return;
        int dir = rng.nextInt(WDX.length); // 4 directions + stay
        int nx = tileX + WDX[dir];
        int ny = tileY + WDY[dir];
        if ((nx != avoidX || ny != avoidY) && (nx != blockedX || ny != blockedY)
                && map.isWalkable(nx, ny)) {
            tileX = nx;
            tileY = ny;
        }
    }

    /** Re-inject the tilemap after a save load (map is transient — AD-6). */
    public void setMap(RogueTileMap map) { this.map = map; }
}

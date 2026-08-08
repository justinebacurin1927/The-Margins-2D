package com.margins.rogue;

/**
 * An allied tile-actor that travels with the player (AD-10). Pure model — no
 * libGDX render types (AD-2); the screen picks the sprite. Generic by design:
 * {@code bindId} is a plain label distinguishing Erik vs Galleon for later
 * art/dialogue.
 *
 * <p>Aldric is a real agent, not a tail: he has combat stats (HP + damage), is a valid enemy
 * target who can fall, and his behavior is driven by the AI in {@code CompanionSystem} — he
 * patrols near the player when the area is calm and charges an alerted enemy to fight it. This
 * class is the model; it exposes the raw one-tile move ({@link #stepTo}) and adjacency that the
 * AI and the enemy phase use, and knows nothing of RunState.
 */
public class Companion {
    /** Distraction uses per floor (FR-14; "2 per floor or 6-turn cooldown" — we chose per-floor). */
    public static final int MAX_DISTRACTIONS_PER_FLOOR = 2;

    /** Combat baseline (fix #1): a bit sturdier than an enemy (8 HP), lighter than the player (20),
     *  striking for the same as a soldier (3). Field-initialized so a save predating these fields
     *  loads a valid starting state (AD-6) — the same pattern as the player's hunger fields. */
    private int maxHp = 14;
    private int hp = 14;
    private int damage = 3;

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

    public int getHp() { return hp; }
    public int getMaxHp() { return maxHp; }
    public int getDamage() { return damage; }

    /** Take combat damage (flat — Aldric has no armor/dodge). Returns the damage dealt. */
    public int takeDamage(int amount) {
        hp = Math.max(0, hp - Math.max(0, amount));
        return Math.max(0, amount);
    }

    /** Whether the companion is up and acting (dead companions stop moving, attacking, and being
     *  targeted — the screen keeps his sprite where he fell until the next run). */
    public boolean isAlive() { return hp > 0; }

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

    /** Move one tile if it's walkable and not the tile already standing on — the raw movement
     *  primitive. The AI ({@link com.margins.rogue.system.CompanionSystem}) validates occupancy
     *  and radius before calling; this checks only the map. */
    public void stepTo(int x, int y) {
        if ((x != tileX || y != tileY) && map.isWalkable(x, y)) {
            tileX = x;
            tileY = y;
        }
    }

    public boolean isAdjacentTo(int px, int py) {
        return Math.abs(tileX - px) + Math.abs(tileY - py) == 1;
    }
}

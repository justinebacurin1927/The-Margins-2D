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

    /** The companion's own condition/debuff vocabulary (Story 5.1, AC-1) — authoritative ON the
     *  companion, never a shadow of the player's Status (AD-3). WOUNDED is the low-HP marker;
     *  PANICKED is the AI-emotion the behavior machine (5.2) will read to emit stealth-blowing noise.
     *  Stored as booleans so it round-trips trivially (AD-6, field-absent → clear). */
    public enum Condition { WOUNDED, PANICKED }

    private int tileX, tileY;
    private String bindId;              // plain label for later art/dialogue ("erik"/"galleon")
    private CompanionId id = CompanionId.ALDRIC; // roster identity (Story 5.1); field-init for save-safety (AD-6)
    private int distractionsLeft = MAX_DISTRACTIONS_PER_FLOOR; // per-floor use limit (FR-14); persisted
    private boolean wounded;            // own condition state (AD-3); field-init false (AD-6)
    private boolean panicked;
    private transient RogueTileMap map;

    private Companion() {} // for libGDX Json deserialization; map re-injected via setMap

    public Companion(int x, int y, RogueTileMap map, String bindId) {
        this.tileX = x;
        this.tileY = y;
        this.map = map;
        this.bindId = bindId;
    }

    /** Roster-aware constructor (Story 5.1): the companion carries its {@link CompanionId} and takes
     *  its {@code bindId} from the roster identity. */
    public Companion(int x, int y, RogueTileMap map, CompanionId id) {
        this(x, y, map, id.bindId());
        this.id = id;
    }

    public int getTileX() { return tileX; }
    public int getTileY() { return tileY; }
    public String getBindId() { return bindId; }

    /** The roster identity this positioned companion represents (Story 5.1, AD-10). */
    public CompanionId getId() { return id; }

    public int getHp() { return hp; }
    public int getMaxHp() { return maxHp; }
    public int getDamage() { return damage; }

    /** Take combat damage (flat — Aldric has no armor/dodge). Returns the damage dealt. */
    public int takeDamage(int amount) {
        hp = Math.max(0, hp - Math.max(0, amount));
        return Math.max(0, amount);
    }

    /** Heal the companion's own HP pool (Story 5.1, AC-1 healable), clamped to its max. Returns the
     *  HP actually restored. A companion at 0 HP is incapacitated, not a corpse — healing can bring
     *  it back up (its death <em>shape</em> is a deferred scope decision, FR-17). */
    public int heal(int amount) {
        int before = hp;
        hp = Math.min(maxHp, hp + Math.max(0, amount));
        return hp - before;
    }

    /** Whether the companion is up and acting (dead companions stop moving, attacking, and being
     *  targeted — the screen keeps his sprite where he fell until the next run). */
    public boolean isAlive() { return hp > 0; }

    /** Downed at 0 HP (Story 5.1, AC-1 incapacitable): distinct from the player's permadeath — an
     *  incapacitated companion is out of the fight but not game-over, and can be healed back up. */
    public boolean isIncapacitated() { return hp <= 0; }

    /** Add one of the companion's own conditions (AC-1). */
    public void addCondition(Condition c) {
        switch (c) {
            case WOUNDED -> wounded = true;
            case PANICKED -> panicked = true;
        }
    }

    /** Remove one of the companion's own conditions. */
    public void removeCondition(Condition c) {
        switch (c) {
            case WOUNDED -> wounded = false;
            case PANICKED -> panicked = false;
        }
    }

    /** Whether the companion currently carries a condition (its own state, independent of the player). */
    public boolean hasCondition(Condition c) {
        return switch (c) {
            case WOUNDED -> wounded;
            case PANICKED -> panicked;
        };
    }

    /** Clear all of the companion's conditions. */
    public void clearConditions() {
        wounded = false;
        panicked = false;
    }

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

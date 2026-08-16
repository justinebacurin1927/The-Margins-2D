package com.margins.rogue.world;

/**
 * Story 6.4 (FR-21, AD-17): a mobile trader — one of the economy's two coin sinks. A stationary
 * per-seed positioned agent Klein walks up to (the "mobile / no fixed shop" flavor is that traders
 * are points on the open map, not shop buildings; roaming AI is deferred). Mirrors {@link
 * com.margins.rogue.RogueEnemy}'s plain-field / no-arg shape for libGDX Json (AD-6). Headless — no
 * libGDX types (AD-2). The transaction rules live in {@code TradeSystem}; this is just the agent.
 */
public class Trader {

    /** The two traders (AC-1/AC-2). The Wanderer takes coin OR barter (Copper/Silver tier); the
     *  Black Market is coin-only (Gold tier), guarded, and killing him locks out the trade. */
    public enum Kind { WANDERER, BLACK_MARKET }

    private Kind kind;
    private int tileX, tileY;
    private int hp, maxHp;
    private boolean alive;

    private Trader() {} // for libGDX Json deserialization

    public Trader(Kind kind, int x, int y) {
        this.kind = kind;
        this.tileX = x;
        this.tileY = y;
        this.hp = 12;
        this.maxHp = 12;
        this.alive = true;
    }

    public Kind getKind() { return kind; }
    public int getTileX() { return tileX; }
    public int getTileY() { return tileY; }
    public int getHp() { return hp; }
    public int getMaxHp() { return maxHp; }
    public boolean isAlive() { return alive; }

    public void takeDamage(int amount) {
        hp = Math.max(0, hp - amount);
        if (hp <= 0) alive = false;
    }

    public boolean isAdjacentTo(int px, int py) {
        return Math.abs(tileX - px) + Math.abs(tileY - py) == 1;
    }

    /** Only the Wanderer barters (AC-1) — the Black Market is coin-only (AC-2). */
    public boolean acceptsBarter() { return kind == Kind.WANDERER; }
}

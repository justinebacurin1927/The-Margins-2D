package com.margins.rogue.system;

/**
 * The player's intent for one turn, produced by the screen from input and
 * consumed by {@link TurnEngine}. Keeps all turn rules out of the screen (AD-2).
 * {@code dir} uses RoguePlayer facing constants (SOUTH=0, NORTH=1, WEST=2, EAST=3).
 */
public class PlayerAction {
    public enum Kind { MOVE, ATTACK, BLOCK, WAIT, USE, DROP, PICKUP }

    public final Kind kind;
    public final int dx;
    public final int dy;
    public final int dir;
    public final int itemType; // backpack type id for USE/DROP; -1 otherwise

    private PlayerAction(Kind kind, int dx, int dy, int dir, int itemType) {
        this.kind = kind;
        this.dx = dx;
        this.dy = dy;
        this.dir = dir;
        this.itemType = itemType;
    }

    public static PlayerAction move(int dx, int dy, int dir) {
        return new PlayerAction(Kind.MOVE, dx, dy, dir, -1);
    }

    public static PlayerAction attack(int dir) {
        return new PlayerAction(Kind.ATTACK, 0, 0, dir, -1);
    }

    public static PlayerAction block(int dir) {
        return new PlayerAction(Kind.BLOCK, 0, 0, dir, -1);
    }

    public static PlayerAction wait(int dir) {
        return new PlayerAction(Kind.WAIT, 0, 0, dir, -1);
    }

    public static PlayerAction use(int itemType, int dir) {
        return new PlayerAction(Kind.USE, 0, 0, dir, itemType);
    }

    public static PlayerAction drop(int itemType, int dir) {
        return new PlayerAction(Kind.DROP, 0, 0, dir, itemType);
    }

    public static PlayerAction pickup(int dir) {
        return new PlayerAction(Kind.PICKUP, 0, 0, dir, -1);
    }
}

package com.margins.rogue.system;

/**
 * The player's intent for one turn, produced by the screen from input and
 * consumed by {@link TurnEngine}. Keeps all turn rules out of the screen (AD-2).
 * {@code dir} uses RoguePlayer facing constants (SOUTH=0, NORTH=1, WEST=2, EAST=3).
 */
public class PlayerAction {
    public enum Kind { MOVE, ATTACK, BLOCK, WAIT }

    public final Kind kind;
    public final int dx;
    public final int dy;
    public final int dir;

    private PlayerAction(Kind kind, int dx, int dy, int dir) {
        this.kind = kind;
        this.dx = dx;
        this.dy = dy;
        this.dir = dir;
    }

    public static PlayerAction move(int dx, int dy, int dir) {
        return new PlayerAction(Kind.MOVE, dx, dy, dir);
    }

    public static PlayerAction attack(int dir) {
        return new PlayerAction(Kind.ATTACK, 0, 0, dir);
    }

    public static PlayerAction block(int dir) {
        return new PlayerAction(Kind.BLOCK, 0, 0, dir);
    }

    public static PlayerAction wait(int dir) {
        return new PlayerAction(Kind.WAIT, 0, 0, dir);
    }
}

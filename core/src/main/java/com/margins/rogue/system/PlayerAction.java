package com.margins.rogue.system;

/**
 * The player's intent for one turn, produced by the screen from input and
 * consumed by {@link TurnEngine}. Keeps all turn rules out of the screen (AD-2).
 * {@code dir} uses RoguePlayer facing constants (SOUTH=0, NORTH=1, WEST=2, EAST=3); an ATTACK may
 * use the full 8-direction set (0-7, diagonals included — the aimed melee, combat fix #3).
 */
public class PlayerAction {
    public enum Kind {
        MOVE, ATTACK, BLOCK, DODGE, FLEE, WAIT, USE, DROP, PICKUP, DISTRACT,
        // Story 1.5 (FR-6): survival crafting actions. COLLECT/BUILD_CAMPFIRE take no itemType;
        // COOK/FILTER/BOIL act on the given backpack itemType (the raw meat / raw water stack).
        COLLECT, BUILD_CAMPFIRE, COOK, FILTER, BOIL,
        // Story 1.6 (FR-7): craft the carried torch from the backpack's Wood + Coal.
        CRAFT_TORCH,
        // Story 3.5 (FR-11): SKILL-governed lockpicking — opens the Old House's locked cellar.
        // No itemType (the COLLECT/BUILD_CAMPFIRE precedent); the tool is checked by LockpickSystem.
        LOCKPICK
    }

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

    // Story 4.1 (FR-12): the Dodge (one-turn boosted evasion) and Flee (break away from the nearest
    // enemy) combat actions. Both are acted branches resolved in TurnEngine — never inside the
    // enemy phase (AD-4).
    public static PlayerAction dodge(int dir) {
        return new PlayerAction(Kind.DODGE, 0, 0, dir, -1);
    }

    public static PlayerAction flee(int dir) {
        return new PlayerAction(Kind.FLEE, 0, 0, dir, -1);
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

    public static PlayerAction distract(int dir) {
        return new PlayerAction(Kind.DISTRACT, 0, 0, dir, -1);
    }

    public static PlayerAction collect(int dir) {
        return new PlayerAction(Kind.COLLECT, 0, 0, dir, -1);
    }

    public static PlayerAction buildCampfire(int dir) {
        return new PlayerAction(Kind.BUILD_CAMPFIRE, 0, 0, dir, -1);
    }

    public static PlayerAction craftTorch(int dir) {
        return new PlayerAction(Kind.CRAFT_TORCH, 0, 0, dir, -1);
    }

    public static PlayerAction cook(int itemType, int dir) {
        return new PlayerAction(Kind.COOK, 0, 0, dir, itemType);
    }

    public static PlayerAction filter(int itemType, int dir) {
        return new PlayerAction(Kind.FILTER, 0, 0, dir, itemType);
    }

    public static PlayerAction boil(int itemType, int dir) {
        return new PlayerAction(Kind.BOIL, 0, 0, dir, itemType);
    }

    public static PlayerAction lockpick(int dir) {
        return new PlayerAction(Kind.LOCKPICK, 0, 0, dir, -1);
    }
}

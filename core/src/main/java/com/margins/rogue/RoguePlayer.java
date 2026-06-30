package com.margins.rogue;

import com.badlogic.gdx.graphics.Texture;
import com.margins.asset.Assets;
import java.util.Random;

public class RoguePlayer {
    public static final int SOUTH = 0;
    public static final int NORTH = 1;
    public static final int WEST = 2;
    public static final int EAST = 3;

    private int tileX;
    private int tileY;
    private int facing;

    private int maxHp;
    private int hp;
    private int hunger;

    private int str;
    private int instinct;
    private int grit;
    private int voice;

    private boolean blocking;
    private Random rand;
    private RogueTileMap map;

    public RoguePlayer(int tileX, int tileY, RogueTileMap map) {
        this.tileX = tileX;
        this.tileY = tileY;
        this.map = map;
        this.facing = SOUTH;

        this.maxHp = 20;
        this.hp = 20;
        this.hunger = 80;

        this.str = 5;
        this.instinct = 7;
        this.grit = 5;
        this.voice = 3;

        this.blocking = false;
        this.rand = new Random();
    }

    public int getTileX() { return tileX; }
    public int getTileY() { return tileY; }
    public int getFacing() { return facing; }
    public int getHp() { return hp; }
    public int getMaxHp() { return maxHp; }
    public int getHunger() { return hunger; }
    public int getStr() { return str; }
    public int getInstinct() { return instinct; }
    public int getGrit() { return grit; }
    public int getVoice() { return voice; }

    public Texture getTexture() {
        return switch (facing) {
            case SOUTH -> Assets.milekSouth;
            case NORTH -> Assets.milekNorth;
            case WEST -> Assets.milekWest;
            case EAST -> Assets.milekEast;
            default -> Assets.milekSouth;
        };
    }

    public boolean tryMove(int dx, int dy) {
        int nx = tileX + dx;
        int ny = tileY + dy;
        if (map.isWalkable(nx, ny)) {
            tileX = nx;
            tileY = ny;
            return true;
        }
        return false;
    }

    public void setFacing(int dir) {
        this.facing = dir;
    }

    public boolean isBlocking() { return blocking; }
    public void setBlocking(boolean v) { blocking = v; }

    public boolean tryDodge() {
        return rand.nextInt(100) < instinct * 3;
    }

    public int getDamageReduction() {
        return grit / 2;
    }

    public int takeDamage(int amount) {
        int reduction = getDamageReduction();
        int finalDmg = Math.max(1, amount - reduction);
        if (blocking) {
            finalDmg = Math.max(1, finalDmg / 2);
            blocking = false;
        }
        hp = Math.max(0, hp - finalDmg);
        return finalDmg;
    }

    public void heal(int amount) {
        hp = Math.min(maxHp, hp + amount);
    }

    public void eat(int amount) {
        hunger = Math.min(100, hunger + amount);
    }

    public void tickHunger() {
        hunger = Math.max(0, hunger - 1);
        if (hunger == 0) {
            takeDamage(1);
        }
    }

    public boolean isAlive() {
        return hp > 0;
    }

    public void setTileX(int x) { this.tileX = x; }
    public void setTileY(int y) { this.tileY = y; }
}

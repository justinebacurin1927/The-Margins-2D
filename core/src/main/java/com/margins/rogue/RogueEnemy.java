package com.margins.rogue;

import com.badlogic.gdx.graphics.Texture;
import com.margins.asset.Assets;

public class RogueEnemy {
    private int tileX, tileY;
    private int hp, maxHp;
    private int damage;
    private boolean alive;
    private boolean justArrived;
    private RogueTileMap map;

    public RogueEnemy(int x, int y, RogueTileMap map) {
        this.tileX = x;
        this.tileY = y;
        this.map = map;
        this.hp = 8;
        this.maxHp = 8;
        this.damage = 3;
        this.alive = true;
        this.justArrived = false;
    }

    public int getTileX() { return tileX; }
    public int getTileY() { return tileY; }
    public int getHp() { return hp; }
    public int getMaxHp() { return maxHp; }
    public int getDamage() { return damage; }
    public boolean isAlive() { return alive; }
    public boolean hasJustArrived() { return justArrived; }
    public void setJustArrived(boolean v) { justArrived = v; }

    public Texture getTexture() { return Assets.rogueEnemyTex; }

    public void takeDamage(int amount) {
        hp = Math.max(0, hp - amount);
        if (hp <= 0) alive = false;
    }

    public void takeTurn(int playerX, int playerY) {
        if (!alive) return;
        int dx = Integer.compare(playerX, tileX);
        int dy = Integer.compare(playerY, tileY);
        boolean moved = false;
        if (dx != 0) {
            int nx = tileX + dx;
            if (nx == playerX && tileY == playerY) return;
            if (map.isWalkable(nx, tileY)) {
                tileX = nx;
                moved = true;
            }
        }
        if (!moved && dy != 0) {
            int ny = tileY + dy;
            if (tileX == playerX && ny == playerY) return;
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
}

package com.margins.entity;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.margins.asset.Assets;
import com.margins.fx.ParticleSystem;
import com.margins.map.TileMap;

public class Player extends Entity {
    private TileMap tileMap;
    private int facing = 0; // 0=down, 1=up, 2=left, 3=right

    private float moveDuration = 0.15f;
    private float moveTimer;
    private int fromX, fromY, targetX, targetY;
    private boolean moving;
    private Texture[] walkTextures;
    private float stepTimer;

    public Player(int tileX, int tileY, TileMap tileMap) {
        super(tileX, tileY, Assets.playerSouth, 24, 48);
        this.tileMap = tileMap;
        walkTextures = new Texture[4];
        walkTextures[0] = Assets.playerSouth;
        walkTextures[1] = Assets.playerNorth;
        walkTextures[2] = Assets.playerWest;
        walkTextures[3] = Assets.playerEast;
    }

    public void update(float delta, ParticleSystem particles) {
        if (moving) {
            moveTimer += delta;
            if (moveTimer >= moveDuration) {
                tileX = targetX;
                tileY = targetY;
                moving = false;
            }
            return;
        }

        int dx = 0, dy = 0;
        if (Gdx.input.isKeyJustPressed(Input.Keys.W) || Gdx.input.isKeyJustPressed(Input.Keys.UP)) { dy = 1; facing = 1; }
        else if (Gdx.input.isKeyJustPressed(Input.Keys.S) || Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) { dy = -1; facing = 0; }
        else if (Gdx.input.isKeyJustPressed(Input.Keys.A) || Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) { dx = -1; facing = 2; }
        else if (Gdx.input.isKeyJustPressed(Input.Keys.D) || Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) { dx = 1; facing = 3; }

        if (dx != 0 || dy != 0) {
            int nx = tileX + dx;
            int ny = tileY + dy;
            if (tileMap.isWalkable(nx, ny)) {
                targetX = nx;
                targetY = ny;
                fromX = tileX;
                fromY = tileY;
                moveTimer = 0;
                moving = true;
                particles.dust(getWorldX(), getWorldY());
            }
        }

        texture = walkTextures[facing];
    }

    @Override
    public float getWorldX() {
        if (moving) return fromX + (targetX - fromX) * (moveTimer / moveDuration);
        return tileX;
    }

    @Override
    public float getWorldY() {
        if (moving) return fromY + (targetY - fromY) * (moveTimer / moveDuration);
        return tileY;
    }

    @Override
    public float getBobOffset() {
        if (moving) return (float) Math.sin((moveTimer / moveDuration) * Math.PI) * 2.5f;
        return 0;
    }

    public boolean isMoving() { return moving; }
}

package com.margins.entity;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;
import com.margins.dialog.DialogNode;
import com.margins.map.TileMap;

public class NPC extends Entity {
    private TileMap tileMap;
    private float moveDuration = 0.2f;
    private float moveProgress;
    private int fromX, fromY, toX, toY;
    private boolean moving;
    private float thinkTimer;
    public DialogNode dialog;
    public String tag;

    public NPC(int tileX, int tileY, Texture texture, TileMap tileMap) {
        super(tileX, tileY, texture, 32, 48);
        this.tileMap = tileMap;
        this.thinkTimer = MathUtils.random(1f, 3f);
    }

    public NPC withDialog(DialogNode d) {
        this.dialog = d;
        return this;
    }

    public NPC withTag(String tag) {
        this.tag = tag;
        return this;
    }

    public void update(float delta) {
        if (moving) {
            moveProgress += delta / moveDuration;
            if (moveProgress >= 1f) {
                moveProgress = 0f;
                tileX = toX;
                tileY = toY;
                moving = false;
            }
        }

        if (!moving) {
            thinkTimer -= delta;
            if (thinkTimer <= 0) {
                thinkTimer = MathUtils.random(1.5f, 4f);
                int[][] dirs = {{0,1},{0,-1},{1,0},{-1,0},{0,2},{2,0},{-2,0},{0,-2}};
                int[] d = dirs[MathUtils.random(dirs.length - 1)];
                int nx = tileX + d[0], ny = tileY + d[1];
                if (tileMap.isWalkable(nx, ny)) {
                    fromX = tileX;
                    fromY = tileY;
                    toX = nx;
                    toY = ny;
                    moveProgress = 0;
                    moving = true;
                }
            }
        }
    }

    @Override
    public float getWorldX() {
        if (moving) return fromX + (toX - fromX) * ease(moveProgress);
        return tileX;
    }

    @Override
    public float getWorldY() {
        if (moving) return fromY + (toY - fromY) * ease(moveProgress);
        return tileY;
    }

    @Override
    public float getBobOffset() {
        if (moving) return (float) Math.sin(moveProgress * Math.PI * 2) * 2.5f;
        return 0;
    }

    private float ease(float t) {
        return t * t * (3f - 2f * t);
    }
}

package com.margins.entity;

import com.badlogic.gdx.graphics.Texture;

public class Decoration extends Entity {
    public int harvestItem = -1;
    public int harvestAmount = 0;
    public boolean harvested;
    public float harvestTimer;
    public static final float RESPAWN_TIME = 25f;

    public Decoration(int tileX, int tileY, Texture texture, float width, float height) {
        super(tileX, tileY, texture, width, height);
    }

    public Decoration harvestable(int itemType, int amount) {
        harvestItem = itemType;
        harvestAmount = amount;
        return this;
    }
}

package com.margins.entity;

import com.badlogic.gdx.graphics.Texture;

public class Entity {
    protected int tileX;
    protected int tileY;
    public Texture texture;
    protected float width;
    protected float height;

    public Entity(int tileX, int tileY, Texture texture, float width, float height) {
        this.tileX = tileX;
        this.tileY = tileY;
        this.texture = texture;
        this.width = width;
        this.height = height;
    }

    public int getTileX() { return tileX; }
    public int getTileY() { return tileY; }
    public Texture getTexture() { return texture; }
    public float getWidth() { return width; }
    public float getHeight() { return height; }
    public float getWorldX() { return tileX; }
    public float getWorldY() { return tileY; }
    public float getBobOffset() { return 0; }
}

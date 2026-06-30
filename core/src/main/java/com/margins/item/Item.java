package com.margins.item;

import com.badlogic.gdx.graphics.Texture;
import com.margins.asset.Assets;

public class Item {
    public static final int WOOD = 0;
    public static final int STONE = 1;
    public static final int BERRIES = 2;

    public static String name(int type) {
        return switch (type) {
            case WOOD -> "Wood";
            case STONE -> "Stone";
            case BERRIES -> "Berries";
            default -> "Unknown";
        };
    }

    public static Texture texture(int type) {
        return switch (type) {
            case WOOD -> Assets.itemWood;
            case STONE -> Assets.itemStone;
            case BERRIES -> Assets.itemBerries;
            default -> null;
        };
    }
}

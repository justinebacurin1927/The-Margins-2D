package com.margins.asset;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;

public class Assets {
    public static Texture playerTex;
    public static Texture playerNorth, playerSouth, playerEast, playerWest;
    public static Texture bushTex, tree1, tree2, rockTex, stoneTex, berriesTex, stumpTex;
    public static Texture itemWood, itemStone, itemBerries;
    public static Texture tileGrass, tileDirt, tileStone, tileWater;
    public static Texture particleTex;
    public static Texture wallTex;
    public static Texture rogueWall, rogueFloor, rogueDoor, rogueStairs, rogueWhite, rogueEnemy;
    public static Texture rogueEnemyTex;
    public static Texture tileFloorTex, tileWallTex, tileDoorTex;

    public static Texture milekSouth, milekNorth, milekWest, milekEast;
    public static Texture treeTex1, treeTex2, treeTex3;

    public static Texture iconHp, iconHunger;
    public static Texture[] numSmall;
    public static Texture[] numMed;

    public static final int TILE = 32;

    public static void load() {
        Pixmap milekPm = new Pixmap(Gdx.files.internal("sprites/swordsman/PNG/Swordsman_lvl1/With_shadow/Swordsman_lvl1_Idle_with_shadow.png"));
        milekSouth = cropPixmap(milekPm, 0, 0, 64, 64);
        milekWest  = cropPixmap(milekPm, 0, 64, 64, 64);
        milekEast  = cropPixmap(milekPm, 0, 128, 64, 64);
        milekNorth = cropPixmap(milekPm, 0, 192, 64, 64);
        milekPm.dispose();

        treeTex1 = new Texture("sprites/trees/PNG/Assets_separately/Trees/Tree1.png");
        treeTex2 = new Texture("sprites/trees/PNG/Assets_separately/Trees/Tree2.png");
        treeTex3 = new Texture("sprites/trees/PNG/Assets_separately/Trees/Tree3.png");
        treeTex1.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        treeTex2.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        treeTex3.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        Pixmap pw = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pw.setColor(0.06f, 0.06f, 0.10f, 1f);
        pw.fill();
        wallTex = new Texture(pw);
        pw.dispose();

        particleTex = makeCircle(8, 0.9f, 0.85f, 0.5f);

        tileGrass = makeColorTex(TILE, TILE, 0.35f, 0.50f, 0.22f);
        tileDirt = makeColorTex(TILE, TILE, 0.45f, 0.33f, 0.20f);
        tileStone = makeColorTex(TILE, TILE, 0.40f, 0.40f, 0.40f);
        tileWater = makeColorTex(TILE, TILE, 0.20f, 0.35f, 0.50f);

        itemWood = makeColorTex(16, 16, 0.55f, 0.35f, 0.15f);
        itemStone = makeColorTex(16, 16, 0.50f, 0.50f, 0.50f);
        itemBerries = makeColorTex(16, 16, 0.70f, 0.15f, 0.15f);

        bushTex = makeColorTex(TILE, TILE, 0.25f, 0.50f, 0.15f);
        tree1 = makeColorTex(TILE * 2, TILE * 2, 0.15f, 0.40f, 0.10f);
        tree2 = makeColorTex(TILE * 2, TILE * 2, 0.20f, 0.45f, 0.12f);
        rockTex = makeColorTex(TILE, TILE, 0.45f, 0.42f, 0.38f);
        stoneTex = makeColorTex(TILE, TILE, 0.50f, 0.50f, 0.50f);
        berriesTex = makeColorTex(TILE, TILE, 0.65f, 0.12f, 0.12f);
        stumpTex = makeColorTex(TILE, TILE, 0.40f, 0.28f, 0.15f);

        rogueWall = makeColorTex(TILE, TILE, 0.60f, 0.50f, 0.60f);
        rogueFloor = makeColorTex(TILE, TILE, 0.80f, 0.70f, 0.55f);
        rogueDoor = makeColorTex(TILE, TILE, 0.30f, 0.20f, 0.12f);
        rogueStairs = makeColorTex(TILE, TILE, 0.90f, 0.75f, 0.15f);
        rogueWhite = makeColorTex(TILE, TILE, 1.00f, 1.00f, 1.00f);
        rogueEnemy = makeColorTex(TILE, TILE, 0.80f, 0.15f, 0.15f);

        Pixmap iconPm = new Pixmap(Gdx.files.internal("sprites/ui/PNG/Icons.png"));
        iconHp = cropPixmap(iconPm, 66, 130, 24, 24);
        iconHunger = cropPixmap(iconPm, 34, 226, 24, 24);
        iconPm.dispose();

        Pixmap numPm = new Pixmap(Gdx.files.internal("sprites/ui/PNG/Numbers.png"));
        numSmall = new Texture[10];
        numMed = new Texture[10];
        for (int i = 0; i < 10; i++) {
            int sy = 4 + i * 16;
            numSmall[i] = cropPixmap(numPm, 0, sy, 16, 7);
            sy = 164 + i * 16;
            numMed[i] = cropPixmap(numPm, 0, sy, 16, 8);
        }
        numPm.dispose();

        Pixmap cultistPm = new Pixmap(Gdx.files.internal("sprites/temple/PNG/Cultist1_Idle.png"));
        rogueEnemyTex = cropPixmap(cultistPm, 0, 0, 64, 64);
        cultistPm.dispose();

        Pixmap tsPm = new Pixmap(Gdx.files.internal("sprites/temple/PNG/Walls_floor.png"));
        tileFloorTex = cropPixmap(tsPm, 29, 96, 32, 32);
        tileWallTex = cropPixmap(tsPm, 45, 176, 32, 32);
        tileDoorTex = cropPixmap(tsPm, 93, 208, 32, 32);
        tsPm.dispose();

        rogueEnemyTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        tileFloorTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        tileWallTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        tileDoorTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    }

    private static Texture makeColorTex(int w, int h, float r, float g, float b) {
        Pixmap pm = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        pm.setColor(r, g, b, 1f);
        pm.fill();
        Texture t = new Texture(pm);
        pm.dispose();
        return t;
    }

    private static Texture cropPixmap(Pixmap source, int x, int y, int w, int h) {
        Pixmap region = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        region.drawPixmap(source, 0, 0, x, y, w, h);
        Texture tex = new Texture(region);
        region.dispose();
        return tex;
    }

    private static Texture makeCircle(int size, float r, float g, float b) {
        Pixmap pm = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pm.setColor(r, g, b, 1f);
        int cx = size / 2, cy = size / 2, rad = size / 2 - 1;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int dx = x - cx, dy = y - cy;
                if (dx * dx + dy * dy <= rad * rad) pm.drawPixel(x, y);
            }
        }
        Texture t = new Texture(pm);
        pm.dispose();
        return t;
    }

    public static void dispose() {
        wallTex.dispose();
        particleTex.dispose();
        tileGrass.dispose();
        tileDirt.dispose();
        tileStone.dispose();
        tileWater.dispose();
        itemWood.dispose();
        itemStone.dispose();
        itemBerries.dispose();
        bushTex.dispose();
        tree1.dispose();
        tree2.dispose();
        rockTex.dispose();
        stoneTex.dispose();
        berriesTex.dispose();
        stumpTex.dispose();
        rogueWall.dispose();
        rogueFloor.dispose();
        rogueDoor.dispose();
        rogueStairs.dispose();
        rogueWhite.dispose();
        rogueEnemy.dispose();
        milekSouth.dispose();
        milekNorth.dispose();
        milekWest.dispose();
        milekEast.dispose();
        treeTex1.dispose();
        treeTex2.dispose();
        treeTex3.dispose();
        iconHp.dispose();
        iconHunger.dispose();
        for (Texture t : numSmall) t.dispose();
        for (Texture t : numMed) t.dispose();
        rogueEnemyTex.dispose();
        tileFloorTex.dispose();
        tileWallTex.dispose();
        tileDoorTex.dispose();
    }
}

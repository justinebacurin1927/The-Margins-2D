package com.margins.asset;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

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
    public static Texture tileGrassTex;
    /** 9-slice forest wall tiles [row][col]; rows = N/mid/S, cols = W/mid/E, [1][1] = interior fill. */
    public static Texture[][] forestWall;

    /** Walk cycles indexed by sheet row: 0=South, 1=West, 2=East, 3=North. */
    public static Animation<TextureRegion>[] milekWalk;
    public static Texture milekWalkSheet;

    public static final int TILE = 32;

    public static void load() {
        // Milek walk cycle: 6 frames x 4 directions (rows S,W,E,N) packed to a shared bbox.
        milekWalkSheet = new Texture(Gdx.files.internal("sprites/player/milek_walk.png"));
        milekWalkSheet.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        int wCols = 6, wRows = 4;
        int fw = milekWalkSheet.getWidth() / wCols, fh = milekWalkSheet.getHeight() / wRows;
        TextureRegion[][] wgrid = TextureRegion.split(milekWalkSheet, fw, fh); // [row][col]
        @SuppressWarnings("unchecked")
        Animation<TextureRegion>[] walks = new Animation[wRows];
        for (int d = 0; d < wRows; d++)
            walks[d] = new Animation<>(0.11f, new Array<>(wgrid[d]), Animation.PlayMode.LOOP);
        milekWalk = walks;

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

        // The "cultist" sprite pack shipped as blank test art (a transparent sheet
        // with one stray mark), so the enemy rendered as a near-invisible white box.
        // Draw a real hooded-road-agent silhouette procedurally, matching the game's
        // other generated art. Real per-entity sprites are the Epic 6.6 art pass.
        rogueEnemyTex = makeCultist();

        tileFloorTex = makeColorTex(TILE, TILE, 0.80f, 0.70f, 0.55f);
        tileWallTex = makeColorTex(TILE, TILE, 0.60f, 0.50f, 0.60f);
        tileDoorTex = makeColorTex(TILE, TILE, 0.30f, 0.20f, 0.12f);

        // Seamless grass ground: Repeat wrap lets each floor tile sample a 32px
        // slice by world-position UVs, so the texture tiles across the map.
        tileGrassTex = new Texture(Gdx.files.internal("sprites/tiles/grass-tiles.png"));
        tileGrassTex.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        tileGrassTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        // Autotiled forest wall: 9-slice tiles chosen per wall by which neighbours are open.
        String[][] wn = {{"nw", "n", "ne"}, {"w", "fill", "e"}, {"sw", "s", "se"}};
        forestWall = new Texture[3][3];
        for (int r = 0; r < 3; r++) for (int c = 0; c < 3; c++) {
            forestWall[r][c] = new Texture(Gdx.files.internal("sprites/tiles/forestwall/" + wn[r][c] + ".png"));
            forestWall[r][c].setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        }

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

    /**
     * The enemy sprite: a hooded red-robed figure with a pale face, drawn as a
     * small pixel silhouette at 64x64 (top-left Pixmap origin, feet at the bottom
     * edge so {@code drawActor} can anchor them on the tile). Rendered with the
     * same Nearest filter as the tiles, it reads as a distinct enemy shape; the
     * companion reuses it under a green ally tint.
     */
    private static Texture makeCultist() {
        Pixmap pm = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
        pm.setColor(0.62f, 0.16f, 0.12f, 1f);
        pm.fillRectangle(18, 30, 28, 10);        // shoulders/torso
        pm.fillTriangle(18, 40, 46, 40, 27, 56); // robe, left taper to hem
        pm.fillTriangle(46, 40, 18, 40, 37, 56); // robe, right taper to hem
        pm.fillRectangle(24, 56, 16, 4);         // hem/feet
        pm.setColor(0.42f, 0.10f, 0.09f, 1f);
        pm.fillCircle(32, 16, 14);               // hood
        pm.setColor(0.86f, 0.78f, 0.64f, 1f);
        pm.fillCircle(32, 14, 6);                // pale face
        pm.setColor(0.08f, 0.05f, 0.05f, 1f);
        pm.fillRectangle(28, 13, 3, 3);          // left eye
        pm.fillRectangle(33, 13, 3, 3);          // right eye
        Texture t = new Texture(pm);
        pm.dispose();
        return t;
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
        rogueEnemyTex.dispose();
        tileFloorTex.dispose();
        tileWallTex.dispose();
        tileDoorTex.dispose();
        tileGrassTex.dispose();
        for (Texture[] row : forestWall) for (Texture t : row) t.dispose();
        milekWalkSheet.dispose();
    }
}

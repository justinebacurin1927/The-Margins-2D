package com.margins.rogue;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.margins.MarginsGame;
import com.margins.asset.Assets;
import com.margins.rogue.FloorGenerator.FloorResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RogueGameScreen implements Screen {
    private MarginsGame game;
    private SpriteBatch batch;
    private OrthographicCamera camera;
    private FitViewport viewport;
    private BitmapFont font;
    private ShapeRenderer shapes;

    private RogueTileMap tileMap;
    private RoguePlayer player;
    private List<RogueEnemy> enemies;
    private Random rand;
    private int floorDepth;
    private boolean waitingForInput;
    private boolean gameOver;
    private String message;
    private float messageTimer;

    private static final int WW = 640;
    private static final int WH = 480;

    public RogueGameScreen(MarginsGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        camera = new OrthographicCamera();
        viewport = new FitViewport(WW, WH, camera);
        font = new BitmapFont();
        shapes = new ShapeRenderer();
        font.getData().setScale(1f);
        floorDepth = 1;
        rand = new Random();
        waitingForInput = true;
        gameOver = false;
        message = "";
        messageTimer = 0;
        generateFloor();
    }

    private void generateFloor() {
        FloorResult result = FloorGenerator.generate(50, 50, rand, floorDepth);
        tileMap = result.map;

        int startCx = result.roomCenters.get(0)[0];
        int startCy = result.roomCenters.get(0)[1];
        player = new RoguePlayer(startCx, startCy, tileMap);

        enemies = new ArrayList<>();
        for (int i = 1; i < result.roomCenters.size(); i++) {
            int cx = result.roomCenters.get(i)[0];
            int cy = result.roomCenters.get(i)[1];
            int count = 1 + rand.nextInt(2);
            for (int e = 0; e < count; e++) {
                int ex = cx + rand.nextInt(3) - 1;
                int ey = cy + rand.nextInt(3) - 1;
                if (tileMap.isWalkable(ex, ey) && !(ex == player.getTileX() && ey == player.getTileY())) {
                    enemies.add(new RogueEnemy(ex, ey, tileMap));
                }
            }
        }
        waitingForInput = true;
        gameOver = false;
    }

    @Override
    public void render(float delta) {
        if (messageTimer > 0) messageTimer -= delta;
        handleInput();

        if (player.isAlive()) {
            camera.position.set(player.getTileX() * 32f + 16f, player.getTileY() * 32f + 16f, 0);
        }
        camera.update();

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        renderWorld();
        renderHUD();
        if (gameOver) renderDeathScreen();
    }

    private void renderWorld() {
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        int px = player.getTileX(), py = player.getTileY();
        int vw = (int)(viewport.getWorldWidth() / 32f) + 3;
        int vh = (int)(viewport.getWorldHeight() / 32f) + 3;
        for (int x = px - vw/2; x <= px + vw/2; x++) {
            for (int y = py - vh/2; y <= py + vh/2; y++) {
                int t = tileMap.getTile(x, y);
                if (t < 0) continue;
                Texture tex = Assets.tileFloorTex;
                if (t == RogueTile.WALL) tex = Assets.tileWallTex;
                else if (t == RogueTile.DOOR) tex = Assets.tileDoorTex;
                else if (t == RogueTile.STAIRS_DOWN || t == RogueTile.STAIRS_UP) tex = Assets.rogueStairs;
                batch.draw(tex, x * 32f, y * 32f, 32f, 32f);
            }
        }
        for (RogueEnemy e : enemies) {
            if (e.isAlive()) {
                batch.draw(e.getTexture(), e.getTileX() * 32f - 16f, e.getTileY() * 32f - 32f, 64f, 64f);
            }
        }
        batch.draw(player.getTexture(), px * 32f - 16f, py * 32f - 32f, 64f, 64f);
        batch.end();

        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (RogueEnemy e : enemies) {
            if (!e.isAlive()) continue;
            float ex = e.getTileX() * 32f;
            float ey = e.getTileY() * 32f + 40f;
            float bw = 32f, bh = 4f;
            shapes.setColor(0.3f, 0.05f, 0.05f, 1);
            shapes.rect(ex, ey, bw, bh);
            float ratio = (float)e.getHp() / e.getMaxHp();
            shapes.setColor(1f - ratio, ratio, 0f, 1f);
            shapes.rect(ex, ey, bw * ratio, bh);
        }
        shapes.end();
    }

    private void renderHUD() {
        batch.getProjectionMatrix().setToOrtho2D(0, 0, WW, WH);
        batch.begin();

        batch.draw(Assets.iconHp, 8, WH - 28, 20, 20);
        drawNum(player.getHp(), 32, WH - 24, false);

        batch.draw(Assets.iconHunger, 8, WH - 52, 20, 20);
        drawNum(player.getHunger(), 32, WH - 48, false);

        font.setColor(0.7f, 0.6f, 0.4f, 1);
        font.draw(batch, "F" + floorDepth, 8, WH - 70);
        font.setColor(1, 1, 1, 1);

        if (messageTimer > 0) {
            font.draw(batch, message, 100, WH - 10);
        }

        font.draw(batch, "WASD move   Q attack   E block   SPACE wait", 10, 18);

        batch.end();
    }

    private void drawNum(int value, float x, float y, boolean floor) {
        String s = String.valueOf(value);
        Texture[] digits = Assets.numSmall;
        float cw = 24, ch = 21;
        for (int i = 0; i < s.length(); i++) {
            int d = s.charAt(i) - '0';
            batch.draw(digits[d], x + i * cw, y, cw, ch);
        }
    }

    private void renderDeathScreen() {
        batch.getProjectionMatrix().setToOrtho2D(0, 0, WW, WH);
        batch.begin();
        batch.setColor(0, 0, 0, 0.7f);
        batch.draw(Assets.rogueWhite, 0, 0, WW, WH);
        batch.setColor(1, 1, 1, 1);

        GlyphLayout gl = new GlyphLayout();

        font.setColor(0.9f, 0.1f, 0.1f, 1);
        gl.setText(font, "YOU DIED");
        font.draw(batch, "YOU DIED", WW / 2f - gl.width / 2f, WH / 2f + 30);

        font.setColor(1, 1, 1, 1);
        gl.setText(font, "R - RESTART");
        font.draw(batch, "R - RESTART", WW / 2f - gl.width / 2f, WH / 2f - 5);
        gl.setText(font, "Q - QUIT");
        font.draw(batch, "Q - QUIT", WW / 2f - gl.width / 2f, WH / 2f - 20);

        batch.end();
    }

    private void handleInput() {
        if (!player.isAlive() && !gameOver) {
            gameOver = true;
            return;
        }
        if (gameOver) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.R)) { floorDepth = 1; generateFloor(); }
            else if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) { Gdx.app.exit(); }
            return;
        }
        if (!waitingForInput) return;

        int dx = 0, dy = 0, dir = player.getFacing();
        boolean attack = false;
        boolean acted = false;
        boolean waitAction = false;
        if (Gdx.input.isKeyJustPressed(Input.Keys.W) || Gdx.input.isKeyJustPressed(Input.Keys.UP)) { dy = 1; dir = 1; }
        else if (Gdx.input.isKeyJustPressed(Input.Keys.S) || Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) { dy = -1; dir = 0; }
        else if (Gdx.input.isKeyJustPressed(Input.Keys.A) || Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) { dx = -1; dir = 2; }
        else if (Gdx.input.isKeyJustPressed(Input.Keys.D) || Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) { dx = 1; dir = 3; }
        else if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) { attack = true; }
        else if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            player.setBlocking(true);
            setMessage("Brace!");
            acted = true;
        }
        else if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            waitAction = true;
            acted = true;
        }
        else return;

        player.setFacing(dir);

        if (attack) {
            int tx = player.getTileX() + (dir == 2 ? -1 : dir == 3 ? 1 : 0);
            int ty = player.getTileY() + (dir == 1 ? 1 : dir == 0 ? -1 : 0);
            RogueEnemy target = enemyAt(tx, ty);
            if (target != null) {
                target.takeDamage(player.getStr());
                setMessage("Hit! " + target.getHp() + "/" + target.getMaxHp());
                acted = true;
            } else {
                setMessage("Nothing there");
                acted = true;
            }
        } else if (dx != 0 || dy != 0) {
            int tx = player.getTileX() + dx;
            int ty = player.getTileY() + dy;
            if (tileMap.isWalkable(tx, ty)) {
                player.tryMove(dx, dy);
                acted = true;
            }
        }

        if (acted) {
            player.tickHunger();
            for (RogueEnemy e : enemies) {
                if (!e.isAlive()) continue;
                if (e.hasJustArrived()) {
                    e.setJustArrived(false);
                    continue;
                }
                if (e.isAdjacentTo(player.getTileX(), player.getTileY())) {
                    if (player.tryDodge()) {
                        setMessage("Dodge!");
                    } else {
                        boolean blocked = player.isBlocking();
                        int dealt = player.takeDamage(e.getDamage());
                        if (blocked) {
                            setMessage("Brace! Blocked " + e.getDamage() + "→" + dealt);
                        } else {
                            setMessage("Hit for " + dealt + "!");
                        }
                    }
                } else {
                    e.takeTurn(player.getTileX(), player.getTileY());
                }
            }
            if (waitAction) setMessage("Wait");
        }
    }

    private RogueEnemy enemyAt(int x, int y) {
        for (RogueEnemy e : enemies) {
            if (e.isAlive() && e.getTileX() == x && e.getTileY() == y) return e;
        }
        return null;
    }

    private void setMessage(String msg) {
        message = msg;
        messageTimer = 2f;
    }

    @Override public void resize(int w, int h) { viewport.update(w, h); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() { batch.dispose(); font.dispose(); shapes.dispose(); }
}

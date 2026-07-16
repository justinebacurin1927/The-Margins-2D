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
import com.margins.rogue.save.SaveService;
import com.margins.rogue.state.RunState;
import com.margins.rogue.system.FovSystem;
import com.margins.rogue.system.PlayerAction;
import com.margins.rogue.system.TurnEngine;
import com.margins.rogue.system.TurnResult;

import java.util.List;

public class RogueGameScreen implements Screen {
    private MarginsGame game;
    private SpriteBatch batch;
    private OrthographicCamera camera;
    private FitViewport viewport;
    private BitmapFont font;
    private ShapeRenderer shapes;

    private RunState state;
    private final TurnEngine turnEngine = new TurnEngine();
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
        waitingForInput = true;
        gameOver = false;
        message = "";
        messageTimer = 0;
        state = SaveService.load();
        if (state == null) state = new RunState();
        FovSystem.compute(state); // initial sight for the first frame (also rebuilds visible after a load)
    }

    @Override
    public void render(float delta) {
        if (messageTimer > 0) messageTimer -= delta;
        handleInput();

        RoguePlayer player = state.getPlayer();
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
        RogueTileMap tileMap = state.getTileMap();
        RoguePlayer player = state.getPlayer();
        List<RogueEnemy> enemies = state.getEnemies();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        int px = player.getTileX(), py = player.getTileY();
        int vw = (int)(viewport.getWorldWidth() / 32f) + 3;
        int vh = (int)(viewport.getWorldHeight() / 32f) + 3;
        for (int x = px - vw/2; x <= px + vw/2; x++) {
            for (int y = py - vh/2; y <= py + vh/2; y++) {
                int t = tileMap.getTile(x, y);
                if (t < 0) continue;
                boolean vis = tileMap.isVisible(x, y);
                if (!vis && !tileMap.isExplored(x, y)) continue; // unexplored → hidden
                Texture tex = Assets.tileFloorTex;
                if (t == RogueTile.WALL) tex = Assets.tileWallTex;
                else if (t == RogueTile.DOOR) tex = Assets.tileDoorTex;
                else if (t == RogueTile.STAIRS_DOWN || t == RogueTile.STAIRS_UP) tex = Assets.rogueStairs;
                if (!vis) batch.setColor(0.45f, 0.45f, 0.5f, 1f); // explored but out of sight → dim
                batch.draw(tex, x * 32f, y * 32f, 32f, 32f);
                if (!vis) batch.setColor(1f, 1f, 1f, 1f);
            }
        }
        for (RogueEnemy e : enemies) {
            if (e.isAlive() && tileMap.isVisible(e.getTileX(), e.getTileY())) {
                batch.draw(e.getTexture(), e.getTileX() * 32f - 16f, e.getTileY() * 32f - 32f, 64f, 64f);
            }
        }
        batch.draw(player.getTexture(), px * 32f - 16f, py * 32f - 32f, 64f, 64f);
        batch.end();

        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (RogueEnemy e : enemies) {
            if (!e.isAlive() || !tileMap.isVisible(e.getTileX(), e.getTileY())) continue;
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
        RoguePlayer player = state.getPlayer();
        batch.getProjectionMatrix().setToOrtho2D(0, 0, WW, WH);
        batch.begin();

        batch.draw(Assets.iconHp, 8, WH - 28, 20, 20);
        drawNum(player.getHp(), 32, WH - 24, false);

        batch.draw(Assets.iconHunger, 8, WH - 52, 20, 20);
        drawNum(player.getHunger(), 32, WH - 48, false);

        font.setColor(0.7f, 0.6f, 0.4f, 1);
        font.draw(batch, "F" + state.getFloorDepth(), 8, WH - 70);
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
        RoguePlayer player = state.getPlayer();

        if (!player.isAlive() && !gameOver) {
            gameOver = true;
            SaveService.deleteSave(); // permadeath: a true-dead run can't be reloaded (FR-21)
            return;
        }
        if (gameOver) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
                state.restart();
                FovSystem.compute(state);
                gameOver = false;
                waitingForInput = true;
            }
            else if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) { Gdx.app.exit(); }
            return;
        }
        if (!waitingForInput) return;

        PlayerAction action = readAction(player.getFacing());
        if (action == null) return;

        TurnResult result = turnEngine.advance(state, action);
        String msg = result.lastMessage();
        if (msg != null) setMessage(msg);
    }

    /** Map the current keypress to a player action, or null if no relevant key. */
    private PlayerAction readAction(int facing) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.W) || Gdx.input.isKeyJustPressed(Input.Keys.UP)) return PlayerAction.move(0, 1, 1);
        if (Gdx.input.isKeyJustPressed(Input.Keys.S) || Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) return PlayerAction.move(0, -1, 0);
        if (Gdx.input.isKeyJustPressed(Input.Keys.A) || Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) return PlayerAction.move(-1, 0, 2);
        if (Gdx.input.isKeyJustPressed(Input.Keys.D) || Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) return PlayerAction.move(1, 0, 3);
        if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) return PlayerAction.attack(facing);
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) return PlayerAction.block(facing);
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) return PlayerAction.wait(facing);
        return null;
    }

    private void setMessage(String msg) {
        message = msg;
        messageTimer = 2f;
    }

    @Override public void resize(int w, int h) { viewport.update(w, h); }
    @Override public void pause() { saveRun(); }
    @Override public void resume() {}
    @Override public void hide() { saveRun(); }
    @Override public void dispose() { batch.dispose(); font.dispose(); shapes.dispose(); }

    /** Persist the current run (skipped once dead — permadeath cleanup arrives in Story 1.5). */
    private void saveRun() {
        if (state != null && !gameOver) SaveService.save(state);
    }
}

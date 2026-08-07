package com.margins;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.margins.rogue.Companion;
import com.margins.rogue.RogueEnemy;
import com.margins.rogue.RoguePlayer;
import com.margins.rogue.RogueTile;
import com.margins.rogue.RogueTileMap;
import com.margins.rogue.item.FloorItem;
import com.margins.rogue.item.Inventory;
import com.margins.rogue.item.Supply;
import com.margins.rogue.state.RunState;
import com.margins.rogue.system.FovSystem;
import com.margins.rogue.system.PlayerAction;
import com.margins.rogue.system.TurnEngine;
import com.margins.rogue.system.TurnResult;

/**
 * First playable vertical slice for The Margin — SPD-style: 2D top-down tiles
 * (placeholder colours, zero art), tile-by-tile turn-based movement through the
 * kept {@link TurnEngine}, fog of war via {@link FovSystem}, and a bottom message
 * log. Klein (bright), Aldric the companion (green), enemies (red). This proves
 * the kept core drives a playable loop; the real Herois presentation is next.
 */
public class MarginScreen implements Screen {
    private static final int TILE = 24;
    private static final int WW = 480, WH = 360;

    private final SpriteBatch batch = new SpriteBatch();
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final BitmapFont font = new BitmapFont();
    private final OrthographicCamera camera = new OrthographicCamera();
    private final FitViewport viewport = new FitViewport(WW, WH, camera);

    private final RunState state = new RunState();
    private final TurnEngine turnEngine = new TurnEngine();

    private String message = "You flee into the pines. Aldric is beside you. [WASD] move.";
    private boolean gameOver = false;

    public MarginScreen() {
        FovSystem.compute(state);
    }

    @Override
    public void render(float delta) {
        handleInput();

        RoguePlayer p = state.getPlayer();
        camera.position.set(p.getTileX() * TILE + TILE / 2f, p.getTileY() * TILE + TILE / 2f, 0);
        camera.update();

        Gdx.gl.glClearColor(0.04f, 0.05f, 0.045f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        renderWorld();
        renderHud();
    }

    private void handleInput() {
        RoguePlayer p = state.getPlayer();

        if (!p.isAlive()) {
            gameOver = true;
            message = "You fell in the margins.   [R] begin again";
            if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
                state.restart();
                FovSystem.compute(state);
                gameOver = false;
                message = "Another life. [WASD] move.";
            }
            return;
        }

        PlayerAction action = readAction(p.getFacing());
        if (action == null) return;
        TurnResult result = turnEngine.advance(state, action);
        String m = result.lastMessage();
        if (m != null) message = m;
    }

    private PlayerAction readAction(int facing) {
        if (down(Input.Keys.W) || down(Input.Keys.UP))    return PlayerAction.move(0, 1, 1);
        if (down(Input.Keys.S) || down(Input.Keys.DOWN))  return PlayerAction.move(0, -1, 0);
        if (down(Input.Keys.A) || down(Input.Keys.LEFT))  return PlayerAction.move(-1, 0, 2);
        if (down(Input.Keys.D) || down(Input.Keys.RIGHT)) return PlayerAction.move(1, 0, 3);
        if (down(Input.Keys.Q))     return PlayerAction.attack(facing);
        if (down(Input.Keys.G))     return PlayerAction.pickup(facing);
        if (down(Input.Keys.SPACE)) return PlayerAction.wait(facing);
        // Story 1.5 survival crafting. Cook/Filter/Boil/Eat act on the FIRST matching backpack
        // stack — a stopgap until the Story 1.8 HUD adds real item selection.
        if (down(Input.Keys.C))     return PlayerAction.collect(facing);
        if (down(Input.Keys.B))     return PlayerAction.buildCampfire(facing);
        if (down(Input.Keys.K)) { int t = firstWhere(s -> s.cooksTo() != null);   if (t >= 0) return PlayerAction.cook(t, facing); }
        if (down(Input.Keys.F)) { int t = firstWhere(s -> s.filtersTo() != null); if (t >= 0) return PlayerAction.filter(t, facing); }
        if (down(Input.Keys.V)) { int t = firstWhere(s -> s.boilsTo() != null);   if (t >= 0) return PlayerAction.boil(t, facing); }
        if (down(Input.Keys.E)) { int t = firstWhere(Supply::isProvision);        if (t >= 0) return PlayerAction.use(t, facing); }
        return null;
    }

    /** The type id of the first backpack stack whose Supply matches, or -1 (Story 1.5 stopgap). */
    private int firstWhere(java.util.function.Predicate<Supply> pred) {
        Inventory inv = state.getInventory();
        for (int i = 0; i < Inventory.BACKPACK_STACKS; i++) {
            Supply s = Supply.byOrdinal(inv.backpackType(i));
            if (s != null && pred.test(s)) return inv.backpackType(i);
        }
        return -1;
    }

    private boolean down(int key) { return Gdx.input.isKeyJustPressed(key); }

    private void renderWorld() {
        RogueTileMap map = state.getTileMap();
        RoguePlayer p = state.getPlayer();
        int px = p.getTileX(), py = p.getTileY();
        int cols = WW / TILE / 2 + 2;
        int rows = WH / TILE / 2 + 2;

        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        // Tiles — visible in full colour, explored-but-unseen dimmed, unknown hidden.
        for (int x = px - cols; x <= px + cols; x++) {
            for (int y = py - rows; y <= py + rows; y++) {
                int t = map.getTile(x, y);
                if (t < 0) continue;
                boolean vis = map.isVisible(x, y);
                if (!vis && !map.isExplored(x, y)) continue;
                setTile(t, vis);
                shapes.rect(x * TILE, y * TILE, TILE, TILE);
            }
        }

        // Floor items (a small gold marker) on visible tiles.
        shapes.setColor(0.85f, 0.72f, 0.25f, 1f);
        for (FloorItem it : state.getFloorItems()) {
            if (map.isVisible(it.x, it.y))
                shapes.rect(it.x * TILE + TILE * 0.35f, it.y * TILE + TILE * 0.35f, TILE * 0.3f, TILE * 0.3f);
        }

        // Enemies (red).
        for (RogueEnemy e : state.getEnemies()) {
            if (!e.isAlive() || !map.isVisible(e.getTileX(), e.getTileY())) continue;
            shapes.setColor(0.80f, 0.22f, 0.16f, 1f);
            shapes.rect(e.getTileX() * TILE + 4, e.getTileY() * TILE + 4, TILE - 8, TILE - 8);
        }

        // Aldric the companion (green ally) if visible.
        Companion comp = state.getActiveCompanion();
        if (comp != null && map.isVisible(comp.getTileX(), comp.getTileY())) {
            shapes.setColor(0.35f, 0.78f, 0.50f, 1f);
            shapes.rect(comp.getTileX() * TILE + 4, comp.getTileY() * TILE + 4, TILE - 8, TILE - 8);
        }

        // Klein (bright).
        shapes.setColor(0.96f, 0.90f, 0.72f, 1f);
        shapes.rect(px * TILE + 3, py * TILE + 3, TILE - 6, TILE - 6);

        shapes.end();
    }

    /** Placeholder tile colours (no art yet); dimmed when explored but out of sight. */
    private void setTile(int t, boolean visible) {
        float r, g, b;
        switch (t) {
            case RogueTile.WALL:        r = 0.16f; g = 0.18f; b = 0.15f; break; // dark trunk/rock
            case RogueTile.DOOR:        r = 0.45f; g = 0.34f; b = 0.20f; break; // wood
            case RogueTile.WELL:        r = 0.30f; g = 0.45f; b = 0.60f; break; // stone well (blue-grey)
            case RogueTile.POND:        r = 0.18f; g = 0.40f; b = 0.42f; break; // murky pond
            case RogueTile.RIVER:       r = 0.22f; g = 0.52f; b = 0.72f; break; // running river
            default:                    r = 0.26f; g = 0.33f; b = 0.23f; break; // forest floor
        }
        if (!visible) { r *= 0.45f; g *= 0.45f; b *= 0.50f; } // fog memory
        shapes.setColor(r, g, b, 1f);
    }

    private void renderHud() {
        RoguePlayer p = state.getPlayer();
        batch.getProjectionMatrix().setToOrtho2D(0, 0, WW, WH);
        batch.begin();

        font.setColor(Color.WHITE);
        font.draw(batch, "HP " + p.getHp() + "/" + p.getMaxHp() + "    " + p.hungerLabel(), 8, WH - 8);
        font.draw(batch, message, 8, 42);

        font.setColor(0.55f, 0.55f, 0.55f, 1f);
        font.draw(batch, "WASD move   Q attack   G grab   SPACE wait", 8, 20);

        batch.end();
    }

    @Override public void resize(int w, int h) { viewport.update(w, h); }
    @Override public void show() {}
    @Override public void hide() {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void dispose() { batch.dispose(); shapes.dispose(); font.dispose(); }
}

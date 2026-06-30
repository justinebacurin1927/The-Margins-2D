package com.margins.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.margins.MarginsGame;
import com.margins.asset.Assets;
import com.margins.dialog.DialogNode;
import com.margins.entity.Entity;
import com.margins.entity.Decoration;
import com.margins.entity.NPC;
import com.margins.entity.Player;
import com.margins.fx.ParticleSystem;
import com.margins.item.Inventory;
import com.margins.item.Item;
import com.margins.map.TileMap;
import com.margins.quest.Quest;
import com.margins.quest.QuestManager;
import com.margins.quest.QuestObjective;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GameScreen implements Screen {
    private MarginsGame game;
    private SpriteBatch batch;
    private ShapeRenderer shapes;
    private OrthographicCamera camera;
    private FitViewport viewport;
    private BitmapFont font;

    private TileMap tileMap;
    private Player player;
    private List<NPC> npcs;
    private List<Decoration> decorations;
    private ParticleSystem particles;

    private Inventory inventory;
    private QuestManager questManager;

    private float darkness;
    private DialogNode currentDialog;
    private int dialogChoice;

    private float timeOfDay;

    public GameScreen(MarginsGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        shapes = new ShapeRenderer();
        camera = new OrthographicCamera();
        viewport = new FitViewport(640, 480, camera);
        font = new BitmapFont();
        font.getData().setScale(0.8f);

        tileMap = new TileMap(64, 48);
        player = new Player(4, 4, tileMap);
        particles = new ParticleSystem();
        inventory = new Inventory();
        questManager = new QuestManager();
        npcs = new ArrayList<>();
        decorations = new ArrayList<>();

        inventory.add(Item.WOOD, 3);
        inventory.add(Item.BERRIES, 2);

        setupWorld();
    }

    private void setupWorld() {
        List<int[]> treeSpots = new ArrayList<>();
        List<int[]> bushSpots = new ArrayList<>();

        for (int x = 6; x < 60; x++) {
            for (int y = 4; y < 44; y++) {
                if (x >= 6 && x <= 20 && y >= 4 && y <= 12) {
                    if (MathUtils.random() < 0.15f) {
                        tileMap.setTile(x, y, TileMap.DIRT);
                    }
                    continue;
                }
                float r = MathUtils.random();
                if (r < 0.008f) treeSpots.add(new int[]{x, y});
                else if (r < 0.02f) bushSpots.add(new int[]{x, y});
                else if (r < 0.025f) {
                    tileMap.setTile(x, y, TileMap.STONE);
                }
            }
        }

        for (int[] spot : treeSpots) {
            int x = spot[0], y = spot[1];
            Texture tex = MathUtils.randomBoolean() ? Assets.tree1 : Assets.tree2;
            decorations.add(new Decoration(x, y, tex, Assets.TILE * 2, Assets.TILE * 2));
        }
        for (int[] spot : bushSpots) {
            int x = spot[0], y = spot[1];
            Decoration bush = new Decoration(x, y, Assets.bushTex, Assets.TILE, Assets.TILE);
            if (MathUtils.random() < 0.3f) bush.harvestable(Item.BERRIES, 1 + MathUtils.random(2));
            decorations.add(bush);
        }
        for (int i = 0; i < 8; i++) {
            int x = MathUtils.random(6, 58), y = MathUtils.random(4, 42);
            if (tileMap.getTile(x, y) == TileMap.GRASS) {
                decorations.add(new Decoration(x, y, Assets.rockTex, Assets.TILE, Assets.TILE));
            }
        }
        for (int i = 0; i < 4; i++) {
            int x = MathUtils.random(6, 58), y = MathUtils.random(4, 42);
            if (tileMap.getTile(x, y) == TileMap.GRASS) {
                decorations.add(new Decoration(x, y, Assets.berriesTex, Assets.TILE, Assets.TILE)
                    .harvestable(Item.BERRIES, 2 + MathUtils.random(2)));
            }
        }
        for (int i = 0; i < 3; i++) {
            int x = MathUtils.random(6, 58), y = MathUtils.random(4, 42);
            if (tileMap.getTile(x, y) == TileMap.GRASS) {
                decorations.add(new Decoration(x, y, Assets.stumpTex, Assets.TILE, Assets.TILE));
            }
        }
        for (int i = 0; i < 3; i++) {
            int x = MathUtils.random(30, 50), y = MathUtils.random(20, 35);
            if (tileMap.getTile(x, y) == TileMap.GRASS) {
                decorations.add(new Decoration(x, y, Assets.stoneTex, Assets.TILE, Assets.TILE));
            }
        }
    }

    @Override
    public void render(float delta) {
        timeOfDay += delta * 0.02f;
        float dayPhase = (float) Math.sin(timeOfDay);
        darkness = MathUtils.clamp(-dayPhase * 0.4f + 0.15f, 0f, 0.75f);

        if (currentDialog != null) {
            handleDialogInput();
        } else {
            player.update(delta, particles);
            particles.manageFireflies(viewport.getWorldWidth(), viewport.getWorldHeight(), darkness, delta);
        }

        for (NPC npc : npcs) npc.update(delta);
        for (Decoration d : decorations) {
            if (d.harvestTimer > 0) d.harvestTimer -= delta;
            if (d.harvested && d.harvestTimer <= 0) {
                d.harvested = false;
                d.texture = findHarvestTex(d);
            }
        }
        particles.update(delta);

        camera.position.set(player.getWorldX() * Assets.TILE + Assets.TILE / 2f,
                            player.getWorldY() * Assets.TILE + Assets.TILE / 2f, 0);
        camera.update();
        batch.setProjectionMatrix(camera.combined);

        Gdx.gl.glClearColor(0.1f, 0.12f, 0.15f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        int viewTilesX = (int) (viewport.getWorldWidth() / Assets.TILE) + 3;
        int viewTilesY = (int) (viewport.getWorldHeight() / Assets.TILE) + 3;
        int playerTileX = player.getTileX();
        int playerTileY = player.getTileY();

        batch.begin();
        renderTiles(playerTileX, playerTileY, viewTilesX, viewTilesY);

        if (darkness > 0.01f) {
            batch.end();
            shapes.setProjectionMatrix(camera.combined);
            shapes.begin(ShapeRenderer.ShapeType.Filled);
            shapes.setColor(0, 0, 0, darkness);
            shapes.rect(camera.position.x - viewport.getWorldWidth() / 2f - 1,
                        camera.position.y - viewport.getWorldHeight() / 2f - 1,
                        viewport.getWorldWidth() + 2, viewport.getWorldHeight() + 2);
            shapes.end();
            batch.begin();
        }

        batch.end();

        renderUI();
    }

    private void renderTiles(int cx, int cy, int vw, int vh) {
        int hw = vw / 2, hh = vh / 2;
        for (int x = cx - hw; x <= cx + hw; x++) {
            for (int y = cy - hh; y <= cy + hh; y++) {
                int t = tileMap.getTile(x, y);
                if (t == -1) continue;
                Texture tex = getTileTex(t);
                batch.draw(tex, x * Assets.TILE, y * Assets.TILE, Assets.TILE, Assets.TILE);
            }
        }

        List<Entity> renderQueue = new ArrayList<>();
        renderQueue.addAll(decorations);
        renderQueue.addAll(npcs);

        renderQueue.sort((a, b) -> {
            if (a.getTileY() != b.getTileY()) return Integer.compare(a.getTileY(), b.getTileY());
            return Float.compare(a.getWorldX(), b.getWorldX());
        });

        boolean playerRendered = false;
        for (Entity e : renderQueue) {
            if (!playerRendered && (e.getTileY() > player.getTileY() || (e.getTileY() == player.getTileY() && e.getWorldX() > player.getWorldX()))) {
                renderPlayer();
                playerRendered = true;
            }
            renderEntity(e);
        }
        if (!playerRendered) renderPlayer();

        if (currentDialog != null) {
            renderDialogOverlay();
        }
    }

    private void renderPlayer() {
        float wx = player.getWorldX() * Assets.TILE;
        float wy = player.getWorldY() * Assets.TILE + player.getBobOffset();
        batch.draw(player.getTexture(), wx - player.getWidth() / 2f + Assets.TILE / 2f,
                   wy - 4, player.getWidth(), player.getHeight());
    }

    private void renderEntity(Entity e) {
        float wx = e.getWorldX() * Assets.TILE;
        float wy = e.getWorldY() * Assets.TILE + e.getBobOffset();
        batch.draw(e.getTexture(), wx + Assets.TILE / 2f - e.getWidth() / 2f,
                   wy - e.getHeight() + 8, e.getWidth(), e.getHeight());
    }

    private Texture getTileTex(int type) {
        return switch (type) {
            case TileMap.GRASS -> Assets.tileGrass;
            case TileMap.DIRT -> Assets.tileDirt;
            case TileMap.STONE -> Assets.tileStone;
            default -> Assets.tileGrass;
        };
    }

    private Texture findHarvestTex(Decoration d) {
        if (d.harvestItem == Item.BERRIES) return Assets.berriesTex;
        return d.texture;
    }

    private void handleDialogInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.W) || Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            dialogChoice = Math.max(0, dialogChoice - 1);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.S) || Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            dialogChoice = Math.min(currentDialog.options.length - 1, dialogChoice + 1);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F)) {
            if (currentDialog.options.length > 0 && dialogChoice < currentDialog.options.length) {
                DialogNode next = currentDialog.options[dialogChoice].next;
                if (next != null) {
                    currentDialog = next;
                    dialogChoice = 0;
                } else {
                    currentDialog = null;
                }
            } else {
                currentDialog = null;
            }
        }
    }

    private void renderDialogOverlay() {
        float wx = player.getWorldX() * Assets.TILE;
        float wy = player.getWorldY() * Assets.TILE;
        batch.draw(Assets.wallTex, wx - 240, wy - 180, 480, 360);
    }

    private void renderUI() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        batch.getProjectionMatrix().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.begin();

        font.setColor(1, 1, 1, 1);
        float y = Gdx.graphics.getHeight() - 20;
        font.draw(batch, "Wood: " + inventory.count(Item.WOOD) + "  Stone: " + inventory.count(Item.STONE) + "  Berries: " + inventory.count(Item.BERRIES), 10, y);
        font.draw(batch, "[WASD] Move  [F] Interact  [I] Inventory  [Q] Quests  [ESC] Pause", 10, y - 20);

        if (!questManager.getActive().isEmpty()) {
            Quest activeQuest = questManager.getActive().get(0);
            font.draw(batch, "Quest: " + activeQuest.name, 10, y - 50);
            for (QuestObjective o : activeQuest.objectives) {
                font.draw(batch, "  " + o.description + " " + o.current + "/" + o.required, 10, y - 68);
            }
        }

        if (currentDialog != null) {
            font.draw(batch, currentDialog.text, 80, 80);
            for (int i = 0; i < currentDialog.options.length; i++) {
                String prefix = (i == dialogChoice) ? "> " : "  ";
                font.draw(batch, prefix + currentDialog.options[i].label, 100, 50 - i * 22);
            }
            font.draw(batch, "[F] Select  [W/S] Navigate", 100, 50 - currentDialog.options.length * 22 - 10);
        }

        batch.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        batch.dispose();
        shapes.dispose();
        font.dispose();
    }
}

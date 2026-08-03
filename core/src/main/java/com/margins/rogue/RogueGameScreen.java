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
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.margins.MarginsGame;
import com.margins.asset.Assets;
import com.margins.rogue.item.FloorItem;
import com.margins.rogue.item.Inventory;
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

    /** Modal UI states that suspend normal turn play (AD-2: rules stay in the model). */
    private enum UiMode { PLAY, INVENTORY, DROP_PROMPT }
    private UiMode uiMode = UiMode.PLAY;
    private int cursor;          // highlighted backpack stack in the inventory panel
    private boolean makeRoom;    // inventory opened from the drop-or-leave prompt to free a slot
    private String message;
    private float messageTimer;

    private int walkFrame = 0;      // current walk frame (0..5) while stepping
    private float sinceMove = 999f; // seconds since the last step; ≥ WALK_HOLD → idle
    private static final float WALK_HOLD = 0.22f;
    /** facing (SOUTH=0,NORTH=1,WEST=2,EAST=3) → walk-sheet row (S=0,W=1,E=2,N=3). */
    private static final int[] FACING_ROW = {0, 3, 1, 2};
    /** per-row standing frame (feet planted) used when idle, by sheet row S,W,E,N. */
    private static final int[] IDLE_COL = {2, 2, 2, 5};

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
        sinceMove += delta;
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
        if (uiMode == UiMode.INVENTORY) renderInventoryPanel();
        if (uiMode == UiMode.DROP_PROMPT) renderDropPrompt();
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
        // Pass 1 — ground: grass under everything (incl. wall bases), plus doors/stairs
        for (int x = px - vw/2; x <= px + vw/2; x++) {
            for (int y = py - vh/2; y <= py + vh/2; y++) {
                int t = tileMap.getTile(x, y);
                if (t < 0) continue;
                boolean vis = tileMap.isVisible(x, y);
                if (!vis && !tileMap.isExplored(x, y)) continue; // unexplored → hidden
                if (!vis) batch.setColor(0.45f, 0.45f, 0.5f, 1f); // explored but out of sight → dim
                if (t == RogueTile.DOOR) {
                    batch.draw(Assets.tileDoorTex, x * 32f, y * 32f, 32f, 32f);
                } else if (t == RogueTile.STAIRS_DOWN || t == RogueTile.STAIRS_UP) {
                    batch.draw(Assets.rogueStairs, x * 32f, y * 32f, 32f, 32f);
                } else {
                    drawWrapped(Assets.tileGrassTex, x, y);       // floor and wall bases
                }
                if (!vis) batch.setColor(1f, 1f, 1f, 1f);
            }
        }
        // Pass 2 — autotiled forest walls: the 9-slice piece is chosen from which neighbours are open (not walls)
        for (int x = px - vw/2; x <= px + vw/2; x++) {
            for (int y = py - vh/2; y <= py + vh/2; y++) {
                if (tileMap.getTile(x, y) != RogueTile.WALL) continue;
                boolean vis = tileMap.isVisible(x, y);
                if (!vis && !tileMap.isExplored(x, y)) continue;
                boolean openN = !isWall(tileMap, x, y + 1);
                boolean openS = !isWall(tileMap, x, y - 1);
                boolean openW = !isWall(tileMap, x - 1, y);
                boolean openE = !isWall(tileMap, x + 1, y);
                int row = openN ? 0 : openS ? 2 : 1;   // top edge / bottom (rock face) / interior
                int col = openW ? 0 : openE ? 2 : 1;
                if (!vis) batch.setColor(0.45f, 0.45f, 0.5f, 1f);
                batch.draw(Assets.forestWall[row][col], x * 32f, y * 32f, 32f, 32f);
                if (!vis) batch.setColor(1f, 1f, 1f, 1f);
            }
        }
        for (RogueEnemy e : enemies) {
            if (e.isAlive() && tileMap.isVisible(e.getTileX(), e.getTileY())) {
                batch.draw(e.getTexture(), e.getTileX() * 32f - 16f, e.getTileY() * 32f - 32f, 64f, 64f);
                Detection d = e.getDetection();
                if (d == Detection.ALERTED) {
                    font.setColor(1f, 0.25f, 0.25f, 1f);
                    font.draw(batch, "!", e.getTileX() * 32f + 12f, e.getTileY() * 32f + 46f);
                    font.setColor(1f, 1f, 1f, 1f);
                } else if (d == Detection.SUSPICIOUS) {
                    font.setColor(1f, 0.9f, 0.3f, 1f);
                    font.draw(batch, "?", e.getTileX() * 32f + 12f, e.getTileY() * 32f + 46f);
                    font.setColor(1f, 1f, 1f, 1f);
                }
            }
        }
        // walk frame while stepping; snap to this direction's standing frame the moment we stop
        int prow = FACING_ROW[player.getFacing()];
        int fi = (sinceMove < WALK_HOLD) ? walkFrame : IDLE_COL[prow];
        TextureRegion frame = Assets.milekWalk[prow].getKeyFrames()[fi];
        float ph = 60f;                                    // ~2 tiles tall
        float pw = ph * frame.getRegionWidth() / frame.getRegionHeight();
        batch.draw(frame, px * 32f + 16f - pw / 2f, py * 32f, pw, ph);
        batch.end();

        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        // Floor items: a placeholder gold marker on visible tiles (dedicated supply art is a later pass).
        for (FloorItem it : state.getFloorItems()) {
            if (!tileMap.isVisible(it.x, it.y)) continue;
            shapes.setColor(0.9f, 0.75f, 0.2f, 1f);
            shapes.rect(it.x * 32f + 11f, it.y * 32f + 11f, 10f, 10f);
        }
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

    /** A wall tile for autotiling — off-map counts as wall so the map border stays solid. */
    private boolean isWall(RogueTileMap m, int x, int y) {
        int t = m.getTile(x, y);
        return t == RogueTile.WALL || t < 0;
    }

    /** Draw a repeat-wrapped tile texture, sampling a 32px world-position slice so it tiles seamlessly. */
    private void drawWrapped(Texture tex, int x, int y) {
        float ts = tex.getWidth();
        float u = x * 32f / ts, v = y * 32f / ts;
        batch.draw(tex, x * 32f, y * 32f, 32f, 32f, u, v, u + 32f / ts, v + 32f / ts);
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

        font.draw(batch, "WASD move  Q atk  E block  SPACE wait  G pick up  I items", 10, 18);

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

    private void renderInventoryPanel() {
        Inventory inv = state.getInventory();
        int[] slots = occupiedSlots();

        batch.getProjectionMatrix().setToOrtho2D(0, 0, WW, WH);
        batch.begin();
        batch.setColor(0, 0, 0, 0.82f);
        batch.draw(Assets.rogueWhite, 60, 60, WW - 120, WH - 120);
        batch.setColor(1, 1, 1, 1);

        font.draw(batch, makeRoom ? "BACKPACK  (drop a stack to make room)" : "BACKPACK", 80, WH - 80);

        float y = WH - 110;
        if (slots.length == 0) {
            font.draw(batch, "(empty)", 92, y);
            y -= 20;
        }
        for (int i = 0; i < slots.length; i++) {
            int type = inv.backpackType(slots[i]);
            String name = state.getIdentifyMap().displayNameFor(type); // true name once identified (FR-12)
            font.draw(batch, (i == cursor ? "> " : "  ") + name + "   x" + inv.backpackCount(slots[i]), 92, y);
            y -= 20;
        }

        y -= 12;
        font.draw(batch, "Equipped:", 80, y);
        y -= 20;
        for (int slot = 0; slot < Inventory.EQUIPPED_SLOTS; slot++) {
            int et = inv.equippedType(slot);
            String label = et < 0 ? "empty" : state.getIdentifyMap().displayNameFor(et);
            font.draw(batch, "  [" + label + "]", 92, y);
            y -= 20;
        }

        font.draw(batch, makeRoom ? "X drop to make room    ESC cancel"
                                  : "U use    X drop    W/S select    I/ESC close", 80, 92);
        batch.end();
    }

    private void renderDropPrompt() {
        batch.getProjectionMatrix().setToOrtho2D(0, 0, WW, WH);
        batch.begin();
        batch.setColor(0, 0, 0, 0.78f);
        batch.draw(Assets.rogueWhite, 110, WH / 2f - 34, WW - 220, 68);
        batch.setColor(1, 1, 1, 1);
        font.draw(batch, "Backpack full  —  [D] drop a stack    [L] leave it", 140, WH / 2f + 8);
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

        if (uiMode == UiMode.INVENTORY) { handleInventoryInput(); return; }
        if (uiMode == UiMode.DROP_PROMPT) { handleDropPromptInput(); return; }

        if (!waitingForInput) return;

        if (Gdx.input.isKeyJustPressed(Input.Keys.I)) { uiMode = UiMode.INVENTORY; cursor = 0; return; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.G)) { handlePickupKey(player); return; }

        PlayerAction action = readAction(player.getFacing());
        if (action == null) return;
        submitTurn(action);
    }

    /** Advance one turn from a player action, updating the walk cycle and message HUD. */
    private void submitTurn(PlayerAction action) {
        RoguePlayer player = state.getPlayer();
        int bx = player.getTileX(), by = player.getTileY();
        TurnResult result = turnEngine.advance(state, action);
        if (player.getTileX() != bx || player.getTileY() != by) { // stepped → advance the walk cycle
            walkFrame = (walkFrame + 1) % 6;
            sinceMove = 0f;
        }
        String msg = result.lastMessage();
        if (msg != null) setMessage(msg);
    }

    /** [G]: pick up the item on Milek's tile, or open the drop-or-leave prompt if the backpack is full. */
    private void handlePickupKey(RoguePlayer player) {
        int type = topItemTypeAt(player.getTileX(), player.getTileY());
        if (type < 0) return; // nothing here → no turn spent
        Inventory inv = state.getInventory();
        if (inv.isBackpackFull() && inv.count(type) == 0) { // no room and no matching stack (FR-9)
            uiMode = UiMode.DROP_PROMPT;
            return;
        }
        submitTurn(PlayerAction.pickup(player.getFacing()));
    }

    /** Type of the first floor item on a tile, or -1 if none. */
    private int topItemTypeAt(int x, int y) {
        for (FloorItem it : state.getFloorItems()) {
            if (it.x == x && it.y == y) return it.type;
        }
        return -1;
    }

    private void handleDropPromptInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.L) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            uiMode = UiMode.PLAY; // leave it
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.D)) {
            uiMode = UiMode.INVENTORY; // pick a stack to drop, then auto-pick-up
            cursor = 0;
            makeRoom = true;
        }
    }

    private void handleInventoryInput() {
        int[] slots = occupiedSlots();
        if (Gdx.input.isKeyJustPressed(Input.Keys.I) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            closeInventory();
            return;
        }
        if (slots.length == 0) return;
        if (cursor >= slots.length) cursor = slots.length - 1;

        if (Gdx.input.isKeyJustPressed(Input.Keys.W) || Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            cursor = (cursor - 1 + slots.length) % slots.length;
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.S) || Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            cursor = (cursor + 1) % slots.length;
            return;
        }

        int type = state.getInventory().backpackType(slots[cursor]);
        int facing = state.getPlayer().getFacing();

        if (makeRoom) { // drop-to-make-room flow: drop the stack, then grab the ground item
            if (Gdx.input.isKeyJustPressed(Input.Keys.X)) {
                submitTurn(PlayerAction.drop(type, facing));
                // Only grab the ground item if the drop turn didn't kill Milek — otherwise a
                // second turn would run on a dead player (death is detected next frame).
                if (state.getPlayer().isAlive()) submitTurn(PlayerAction.pickup(facing));
                closeInventory();
            }
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.U)) {
            submitTurn(PlayerAction.use(type, facing));
            closeInventory();
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.X)) {
            submitTurn(PlayerAction.drop(type, facing));
            closeInventory();
        }
    }

    private void closeInventory() {
        uiMode = UiMode.PLAY;
        makeRoom = false;
    }

    /** Backpack slot indices that currently hold a stack, in slot order. */
    private int[] occupiedSlots() {
        Inventory inv = state.getInventory();
        int n = 0;
        for (int i = 0; i < Inventory.BACKPACK_STACKS; i++) if (inv.backpackType(i) >= 0) n++;
        int[] out = new int[n];
        int k = 0;
        for (int i = 0; i < Inventory.BACKPACK_STACKS; i++) if (inv.backpackType(i) >= 0) out[k++] = i;
        return out;
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

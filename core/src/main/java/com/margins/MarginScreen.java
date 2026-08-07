package com.margins;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.margins.dialog.DialogNode;
import com.margins.rogue.Companion;
import com.margins.rogue.RogueEnemy;
import com.margins.rogue.RoguePlayer;
import com.margins.rogue.RogueTile;
import com.margins.rogue.RogueTileMap;
import com.margins.rogue.item.FloorItem;
import com.margins.rogue.item.Inventory;
import com.margins.rogue.item.Supply;
import com.margins.rogue.narrative.CorneoIntro;
import com.margins.rogue.narrative.DialogController;
import com.margins.rogue.narrative.IntroController;
import com.margins.rogue.narrative.TutorialController;
import com.margins.rogue.state.RunState;
import com.margins.rogue.system.FovSystem;
import com.margins.rogue.system.PlayerAction;
import com.margins.rogue.system.TurnEngine;

import java.util.ArrayList;
import java.util.List;

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
    /** Story 2.1: the open dialogue scene, if any. Transient view-session state (NOT on
     *  RunState — AD-6); while {@code isActive()} the turn loop is suspended (AD-14). */
    private final DialogController dialog = new DialogController();
    /** Story 2.2: the Act 0 paged-text intro, opened once on a fresh run (below). Transient
     *  view-session state (NOT on RunState — AD-6); takes no RunState, so it cannot tick (AD-14). */
    private final IntroController intro = new IntroController();
    /** Story 2.3: Aldric's diegetic tutorial — a passive coach that observes committed turns and
     *  coaches the six opening controls into the log. Transient view-session state (NOT on RunState);
     *  it never suspends the turn loop. Begins when the intro closes on a fresh run; skipped on restart. */
    private final TutorialController tutorial = new TutorialController();

    private boolean gameOver = false;
    /** Selected backpack slot (0..7), -1 = none yet. Screen state only — reset on restart (Task 5). */
    private int selectedSlot = -1;
    private static final int LOG_LINES = 5; // the bottom log shows the last ~5 lines (NFR-3, AD-15)
    /** Single source for the death line — the log seed and the overlay must never drift (review finding). */
    private static final String GAME_OVER_LINE = "You fell in the margins.   [R] begin again";

    public MarginScreen() {
        FovSystem.compute(state);
        // A fresh RunState = a new run: play the Act 0 intro once (Decision 5). restart() does NOT
        // replay it — a new life after death already knows the story.
        intro.start(CorneoIntro.build());
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
            // The game-over line is seeded into the log once — the log is the text surface (AC-1);
            // the red overlay keeps it prominent. [R] restarts: fresh seeded log, selection cleared.
            if (!gameOver) {
                gameOver = true;
                state.appendMessages(List.of(GAME_OVER_LINE));
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.R)) restart();
            return;
        }

        // Story 2.2 safe pause (AD-14): the Act 0 intro plays at app start, before any dialogue can
        // open, so its branch sits first. While it is active only intro keys route and every gameplay
        // key is swallowed — no PlayerAction, so no turn and no survival tick. The controller takes
        // no RunState (it cannot tick); the screen only forwards advance/skip and renders the page.
        if (intro.isActive()) {
            handleIntroInput();
            return;
        }
        // Story 2.3: the intro has closed — arm the diegetic tutorial. begin() is guarded (fires the
        // first Aldric prompt once, then no-ops), so this every-frame call is safe. On a restart run
        // the tutorial was skip()'d, so this stays a no-op (a new life gets no coaching).
        tutorial.begin(state);
        // Story 2.1 safe pause (AD-14): while a scene is open, only dialogue keys route and every
        // gameplay key is swallowed — no PlayerAction, so no turn and no survival tick. The
        // screen only forwards indices and renders the controller's node (AD-1); it never mutates
        // dialogue state itself.
        if (dialog.isActive()) {
            handleDialogueInput();
            return;
        }
        // A consumed selection (its stack was used up) resets the selection so E/K/F/V don't
        // silently no-op on a now-empty slot (edge-review finding).
        if (selectedSlot >= 0 && state.getInventory().backpackType(selectedSlot) < 0) selectedSlot = -1;

        PlayerAction action = readAction(p.getFacing());
        if (action != null) {
            turnEngine.advance(state, action); // the log is fed inside the engine (AD-4)
            tutorial.onAction(action, state);  // Story 2.3: the passive coach observes the committed turn
        }
    }

    /** Restart the run: close any open scene, reset state, recompute FOV, clear the backpack
     *  selection. The single restart path for both game-over [R] and a live-scene [R] (Decision 6 —
     *  a restart always closes an open scene); the new life announces itself (deletion-check
     *  finding) instead of silently re-showing the opening line. */
    private void restart() {
        dialog.end();
        intro.end(); // close any open surface — symmetric with dialog.end() (review: keep the invariant honest)
        tutorial.skip(); // Story 2.3: a new life after death gets no coaching (Decision 6)
        state.restart();
        FovSystem.compute(state);
        gameOver = false;
        selectedSlot = -1;
        state.appendMessages(List.of("Another life. [WASD] move."));
    }

    private PlayerAction readAction(int facing) {
        if (down(Input.Keys.W) || down(Input.Keys.UP))    return PlayerAction.move(0, 1, 1);
        if (down(Input.Keys.S) || down(Input.Keys.DOWN))  return PlayerAction.move(0, -1, 0);
        if (down(Input.Keys.A) || down(Input.Keys.LEFT))  return PlayerAction.move(-1, 0, 2);
        if (down(Input.Keys.D) || down(Input.Keys.RIGHT)) return PlayerAction.move(1, 0, 3);
        if (down(Input.Keys.Q))     return PlayerAction.attack(facing);
        if (down(Input.Keys.G))     return PlayerAction.pickup(facing);
        if (down(Input.Keys.SPACE)) return PlayerAction.wait(facing);
        // Story 1.5/1.6 survival crafting. Cook/Filter/Boil/Eat act on the SELECTED backpack stack
        // (Task 5 — F-09: no first-match quick-eat): a selection that isn't actionable stays inert.
        if (down(Input.Keys.C)) return PlayerAction.collect(facing);
        if (down(Input.Keys.B)) return PlayerAction.buildCampfire(facing);
        if (down(Input.Keys.T)) return PlayerAction.craftTorch(facing); // Story 1.6: 1 Wood + 1 Coal
        Supply s = Supply.byOrdinal(selectedType());
        if (down(Input.Keys.K) && s != null && s.cooksTo() != null)   return PlayerAction.cook(s.ordinal(), facing);
        if (down(Input.Keys.F) && s != null && s.filtersTo() != null) return PlayerAction.filter(s.ordinal(), facing);
        if (down(Input.Keys.V) && s != null && s.boilsTo() != null)   return PlayerAction.boil(s.ordinal(), facing);
        if (down(Input.Keys.E) && s != null && s.isProvision())       return PlayerAction.use(s.ordinal(), facing);
        // Backpack selection cycle (Task 5): TAB / ] forward, [ backward. Not a turn — returns null.
        if (down(Input.Keys.TAB) || down(Input.Keys.RIGHT_BRACKET)) return cycleSelection(1);
        if (down(Input.Keys.LEFT_BRACKET)) return cycleSelection(-1);
        return null;
    }

    /** Story 2.2 (AD-14): the intro input surface while the paged intro is open. SPACE/E advance one
     *  page (the last page closes the intro); ESC skips straight to gameplay in one action (Decision 2).
     *  Everything else is swallowed. Forwarding only (AD-1) — the controller owns page navigation, and
     *  nothing here produces a PlayerAction, so the turn loop stays suspended (no tick, no turn). */
    private void handleIntroInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            intro.advance();
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            intro.end();
        }
    }

    /** Story 2.1 (AD-14): the dialogue input surface while a scene is open. Number keys pick a
     *  choice; SPACE/E close a terminal (optionless) node; ESC cancels; R restarts (Decision 6 —
     *  a restart closes any open scene). Nothing here returns a PlayerAction — the turn loop stays
     *  suspended. Forwarding only (AD-1): the controller owns navigation, effects, and the log writes. */
    private void handleDialogueInput() {
        DialogNode node = dialog.getCurrent();
        if (node == null) return;
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) { restart(); return; }
        for (int i = 1; i <= node.options.length && i <= 9; i++) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1 + (i - 1))) { // NUM_1..NUM_9
                dialog.select(i - 1, state);
                return;
            }
        }
        if (node.options.length == 0
                && (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.isKeyJustPressed(Input.Keys.E))) {
            dialog.end();
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            dialog.end();
        }
    }

    /** The selected stack's type, or -1 when nothing is selected (or the slot went empty). */
    private int selectedType() {
        return selectedSlot < 0 ? -1 : state.getInventory().backpackType(selectedSlot);
    }

    /** Move the selection one occupied stack forward (dir &gt; 0) or back (dir &lt; 0); returns null
     *  (not a turn action) so no turn commits. Forward and back are symmetric mirrors on the ring
     *  (review finding): a backward press steps to the previous occupied stack, never 7 forwards. */
    private PlayerAction cycleSelection(int dir) {
        Inventory inv = state.getInventory();
        if (dir > 0) {
            selectedSlot = inv.nextOccupiedStack(selectedSlot);
        } else {
            selectedSlot = inv.previousOccupiedStack(selectedSlot);
        }
        return null;
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

        // HP + the four survival tracks (AC-1) — split across two lines so a long label can't overflow.
        font.setColor(Color.WHITE);
        font.draw(batch, "HP " + p.getHp() + "/" + p.getMaxHp() + "    " + p.hungerLabel()
                + "    " + p.thirstLabel(), 8, WH - 8);
        // Track 4 is the clock + weather — "Day 45    Clear" (DayPhase is a bare enum, formatted here).
        font.draw(batch, p.tempLabel() + "    " + (state.isDay() ? "Day" : "Night") + " " + state.getClockTurns()
                + "    " + state.getWeather().label(), 8, WH - 22);

        // Active-debuff row (Decision 4) — blank when clean.
        List<String> debuffs = p.getActiveDebuffLabels();
        if (!debuffs.isEmpty()) {
            font.setColor(0.90f, 0.55f, 0.30f, 1f);
            font.draw(batch, String.join("   ", debuffs), 8, WH - 36);
        }

        renderBackpackRow();

        // The bottom message log (NFR-3, AD-15): the PRIMARY text surface. Log-window policy (2.1
        // Decision 1, extended to the 2.2 intro): while the intro or a dialogue scene is open it
        // shows that PAGE (speaker + text + choices/footer), not the last-5 event lines; the event
        // window resumes when the page closes. The intro plays first (app start), so it takes
        // precedence. The log is core-owned (AD-1); never built here.
        if (intro.isActive()) {
            renderTextPage(intro.getCurrent(), "[SPACE] continue   [ESC] skip");
        } else if (dialog.isActive()) {
            renderTextPage(dialog.getCurrent(), "[SPACE] continue");
        } else {
            List<String> log = state.getMessageLog();
            int start = Math.max(0, log.size() - LOG_LINES);
            int y = 22 + 14 * (log.size() - 1 - start);
            font.setColor(Color.WHITE);
            for (int i = start; i < log.size(); i++) {
                font.draw(batch, log.get(i), 8, y);
                y -= 14;
            }
        }

        // Game over — a prominent centered overlay (the line is also in the log above).
        if (gameOver) {
            font.setColor(0.95f, 0.25f, 0.20f, 1f);
            font.draw(batch, GAME_OVER_LINE, (WW - new GlyphLayout(font, GAME_OVER_LINE).width) / 2f, WH / 2f);
        }

        font.setColor(0.55f, 0.55f, 0.55f, 1f);
        font.draw(batch, "WASD move   Q attack   G grab   SPACE wait   TAB select   E use", 8, 8);

        batch.end();
    }

    /** Backpack row (Task 5): each occupied stack as "name xN", the selected one highlighted;
     *  empty slots blank. The names come from the identify map (identified vs mystery, FR-12). */
    private void renderBackpackRow() {
        Inventory inv = state.getInventory();
        int x = 8;
        for (int slot = 0; slot < Inventory.BACKPACK_STACKS; slot++) {
            int type = inv.backpackType(slot);
            if (type < 0) continue; // empty slot — blank
            boolean sel = slot == selectedSlot;
            String label = state.getIdentifyMap().displayNameFor(type) + " x" + inv.backpackCount(slot);
            float w = new GlyphLayout(font, label).width;
            if (x + w > WW) break; // no room left — stop before the right edge (edge-review finding)
            font.setColor(sel ? 0.96f : 0.70f, sel ? 0.90f : 0.75f, sel ? 0.72f : 0.70f, 1f);
            font.draw(batch, label, x, WH - 50);
            x += (int) w + 12;
        }
    }

    /** The text-forward page (AD-15 — the bottom log IS the text surface), shared by the 2.1
     *  dialogue scene and the 2.2 intro. Draws the speaker (nullable → narration, no prefix), the
     *  wrapped node text below the HUD rows, and the numbered choices at the bottom edge. A
     *  zero-option node (a terminal dialogue node, or every intro page — Decision 7) shows the
     *  {@code footer} affordance instead of choices. A null option label renders defensively
     *  (authoring contract — the controller navigates by index, never by label). */
    private void renderTextPage(DialogNode node, String footer) {
        if (node == null) return;

        int y = WH - 66; // below the HP/tracks/debuff/backpack rows
        if (node.speaker != null) {
            font.setColor(0.75f, 0.90f, 0.80f, 1f); // speaker in green, the companion accent
            font.draw(batch, node.speaker + ":", 8, y);
            y -= 14;
        }
        // The choices block is bottom-anchored, so the wrapped text must stop above it — a long
        // node text would otherwise overlap the choices (review finding). A null text renders no
        // lines (defensive, like the null-label render above).
        int choicesTop = 22 + 14 * node.options.length;
        font.setColor(0.95f, 0.90f, 0.75f, 1f);
        String text = node.text != null ? node.text : "";
        for (String line : wrapText(text, WW - 16)) {
            if (y < choicesTop) break; // no room left — stop above the choices block
            font.draw(batch, line, 8, y);
            y -= 14;
        }

        int cy = 22 + 14 * (node.options.length - 1);
        font.setColor(Color.WHITE);
        for (int i = 0; i < node.options.length; i++) {
            String label = node.options[i].label != null ? node.options[i].label : "(…)";
            font.draw(batch, (i + 1) + ". " + label, 8, cy);
            cy -= 14;
        }
        // Decision 7: a zero-option node is a text page closed by an advance key — the footer is the
        // affordance that it's a page, not a dead end (dialogue: "[SPACE] continue"; intro: adds skip).
        if (node.options.length == 0) {
            font.setColor(0.55f, 0.55f, 0.55f, 1f);
            font.draw(batch, footer, 8, 22);
        }
    }

    /** Word-wrap {@code text} to fit {@code maxWidthPx} at the current font (GlyphLayout measure). */
    private List<String> wrapText(String text, int maxWidthPx) {
        List<String> lines = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String word : text.split(" ")) {
            String trial = cur.length() == 0 ? word : cur + " " + word;
            if (cur.length() > 0 && new GlyphLayout(font, trial).width > maxWidthPx) {
                lines.add(cur.toString());
                cur.setLength(0);
                cur.append(word);
            } else {
                if (cur.length() > 0) cur.append(' ');
                cur.append(word);
            }
        }
        if (cur.length() > 0) lines.add(cur.toString());
        return lines;
    }

    @Override public void resize(int w, int h) { viewport.update(w, h); }
    @Override public void show() {}
    @Override public void hide() {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void dispose() { batch.dispose(); shapes.dispose(); font.dispose(); }
}

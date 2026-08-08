package com.margins;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.margins.dialog.DialogNode;
import com.margins.rogue.Companion;
import com.margins.rogue.Detection;
import com.margins.rogue.RogueEnemy;
import com.margins.rogue.RoguePlayer;
import com.margins.rogue.RogueTile;
import com.margins.rogue.RogueTileMap;
import com.margins.rogue.item.FloorItem;
import com.margins.rogue.item.Inventory;
import com.margins.rogue.item.Supply;
import com.margins.rogue.narrative.CaptureController;
import com.margins.rogue.narrative.CorneoIntro;
import com.margins.rogue.narrative.DialogController;
import com.margins.rogue.narrative.IntroController;
import com.margins.rogue.narrative.JournalController;
import com.margins.rogue.narrative.TutorialController;
import com.margins.rogue.state.RunState;
import com.margins.rogue.system.FovSystem;
import com.margins.rogue.system.PlayerAction;
import com.margins.rogue.system.TurnEngine;
import com.margins.rogue.system.TurnResult;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * First playable vertical slice for The Margin — SPD-style 2D top-down: the world wears
 * Pixel Pack v2 sprites ({@link PixelPack}) — environment tiles, actors, and items — with
 * tile-by-tile turn-based movement through the kept {@link TurnEngine}, fog of war via
 * {@link FovSystem}, and a bottom message log. This proves the kept core drives a playable
 * loop; the real Herois presentation is next.
 */
public class MarginScreen implements Screen {
    private static final int TILE = 24;
    private static final int WW = 480, WH = 360;
    private static final float UI_FONT_SCALE = 1f;
    private static final int UI_LINE = 10;
    private static final int HUD_MARGIN = 6;
    private static final int TOP_PANEL_Y = 296;
    private static final int TOP_PANEL_H = 58;
    private static final int STATUS_PANEL_W = 252;
    private static final int STATUS_CHIP_W = 32;
    private static final int STATUS_CHIP_H = 30;
    private static final int STATUS_CHIP_GAP = 2;
    private static final int PACK_PANEL_X = 264;
    private static final int PACK_PANEL_W = 210;
    private static final int LOG_PANEL_Y = 6;
    private static final int LOG_PANEL_H = 64;
    private static final float MOVE_DURATION = 0.26f;
    private static final float ATTACK_DURATION = 0.45f;
    private static final int OLD_HOUSE_ATLAS_COLS = 15;
    private static final int OLD_HOUSE_ATLAS_ROWS = 10;
    private static final int OLD_HOUSE_APRON = 1;
    private static final float STRUCTURE_ACTOR_DROP = 1.5f;
    private static final Color STRUCTURE_ACTOR_SHADOW = new Color(0.015f, 0.02f, 0.015f, 0.34f);

    private static final Color UI_PANEL = new Color(0.025f, 0.040f, 0.034f, 0.90f);
    private static final Color UI_PANEL_STRONG = new Color(0.018f, 0.028f, 0.025f, 0.97f);
    private static final Color UI_SLOT = new Color(0.08f, 0.11f, 0.09f, 0.94f);
    private static final Color UI_BORDER = new Color(0.25f, 0.34f, 0.27f, 1f);
    private static final Color UI_ACCENT = new Color(0.78f, 0.68f, 0.38f, 1f);
    private static final Color UI_TEXT = new Color(0.88f, 0.90f, 0.82f, 1f);
    private static final Color UI_MUTED = new Color(0.48f, 0.55f, 0.49f, 1f);
    private static final Color UI_HEALTH = new Color(0.90f, 0.34f, 0.28f, 1f);
    private static final Color UI_FOOD = new Color(0.73f, 0.67f, 0.36f, 1f);
    private static final Color UI_WATER = new Color(0.40f, 0.68f, 0.76f, 1f);
    private static final Color UI_WARNING = new Color(0.92f, 0.55f, 0.28f, 1f);
    private static final Color ENEMY_BAR_BORDER = new Color(0.06f, 0.035f, 0.03f, 1f);
    private static final Color ENEMY_BAR_EMPTY = new Color(0.22f, 0.07f, 0.06f, 1f);
    private static final Color ENEMY_BAR_HEALTH = new Color(0.88f, 0.20f, 0.16f, 1f);

    private final SpriteBatch batch = new SpriteBatch();
    private final BitmapFont font;
    private final BitmapFont headingFont;
    private final Texture uiPixel;
    private final OrthographicCamera camera = new OrthographicCamera();
    private final FitViewport viewport = new FitViewport(WW, WH, camera);
    private final Vector2 pointerHud = new Vector2();
    /** Pixel Pack v2 atlases (AD-1: screen layer only). Player/companion/enemies use the
     *  characters atlas, items the 40-icon atlas, the world the 64-tile environment atlas. */
    private final PixelPack pixels = new PixelPack();

    // Presentation-only animation state. The turn engine still resolves every action atomically;
    // these short interpolations simply show the resolved before/after states instead of snapping.
    private Motion playerMotion;
    private Motion companionMotion;
    private final IdentityHashMap<RogueEnemy, Motion> enemyMotions = new IdentityHashMap<>();
    private AttackAnimation playerAttack;
    private final IdentityHashMap<RogueEnemy, AttackAnimation> enemyAttacks = new IdentityHashMap<>();

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
    /** Story 2.4: Aldric's capture — a one-shot scripted event that resolves the moment the
     *  tutorial completes. Transient view-session state (NOT on RunState — AD-6); the resolving
     *  wire is the every-frame gate in handleInput (guarded, fires once per life). */
    private final CaptureController capture = new CaptureController();
    /** Story 2.5: the passive Journal — the quest registry + lookup surface. Transient view-session
     *  state (NOT on RunState — AD-6); while {@code isActive()} the turn loop is suspended (AD-14 —
     *  the quest log is a suspended text surface). Renders {@code journal.entries(state)} (AC-2). */
    private final JournalController journal = new JournalController();

    private boolean gameOver = false;
    /** Pause/help surface. It is presentation-only and never advances the turn engine. */
    private boolean menuOpen = false;
    /** Clicked condition card; hover temporarily takes precedence. Presentation-only, never a turn. */
    private String pinnedStatusKey;
    /** Selected backpack slot (0..7), -1 = none yet. Screen state only — reset on restart (Task 5). */
    private int selectedSlot = -1;
    private static final int LOG_LINES = 5; // visual lines in the compact bottom event panel
    /** Single source for the death line — the log seed and the overlay must never drift (review finding). */
    private static final String GAME_OVER_LINE = "You fell in the margins.   [R] begin again";

    // Pixel Pack v2 character cells (row-major): 0 Klein, 1 Aldric, 6 Giliman foot soldier.
    private static final int PLAYER_CHARACTER = 0;
    private static final int COMPANION_CHARACTER = 1;
    private static final int ENEMY_CHARACTER = 6;
    /** Item-icon atlas cell per Supply ordinal (row-major, five rows of eight); -1 = no icon. The
     *  pack has no exact sprite for several provisions, so the closest readable icon is used (e.g.
     *  the meat-spoilage stages share the aged-cheese icon; filtered/boiled water share the purified
     *  waterskin). Kept next to the enum's ordinal list so a new Supply stays in step. */
    private static final int[] ITEM_ICONS = {
        0,  // 0  WRAPPED_BUNDLE   -> bread
        8,  // 1  SEALED_WATERSKIN -> raw waterskin
        11, // 2  SMALL_TIN        -> water jar
        15, // 3  FOLDED_CLOTH     -> cloth
        31, // 4  SEALED_LETTER    -> map fragment (a paper)
        12, // 5  COAL             -> coal
        3,  // 6  RAW_MEAT         -> raw rabbit
        2,  // 7  HALF_ROTTEN_MEAT -> moldy cheese (aged)
        2,  // 8  SPOILED_MEAT     -> moldy cheese
        4,  // 9  COOKED_MEAT      -> cooked meat
        8,  // 10 WELL_WATER       -> raw waterskin
        8,  // 11 POND_WATER       -> raw waterskin
        8,  // 12 RIVER_WATER      -> raw waterskin
        9,  // 13 FILTERED_WATER   -> purified waterskin
        9,  // 14 BOILED_WATER     -> purified waterskin
        12, // 15 SALT             -> coal (mineral chunks)
        13, // 16 WOOD             -> wood bundle
        6,  // 17 TOXIC_MUSHROOM   -> spotted mushroom
        6,  // 18 HONEYMOON_MUSHROOM -> spotted mushroom
        7,  // 19 HONEY            -> honeycomb
        7,  // 20 HONEYCOMB        -> honeycomb
        6,  // 21 BLOODVEIN_MUSHROOM -> spotted mushroom
        30, // 22 HERBAL_CURE      -> bandage
        31, // 23 TORN_PAGE        -> map fragment (a paper)
    };
    /** Backpack-row icon size (px); the stack label sits to its right. */
    private static final int ITEM_ICON = 16;

    private static final class Motion {
        final float fromX, fromY, toX, toY;
        float elapsed;

        Motion(int fromX, int fromY, int toX, int toY) {
            this.fromX = fromX;
            this.fromY = fromY;
            this.toX = toX;
            this.toY = toY;
        }

        float progress() {
            return Math.min(1f, elapsed / MOVE_DURATION);
        }
    }

    private static final class AttackAnimation {
        final int direction;
        float elapsed;

        AttackAnimation(int direction) {
            this.direction = direction;
        }

        float progress() {
            return Math.min(1f, elapsed / ATTACK_DURATION);
        }
    }

    private static final class EnemySnapshot {
        final int x, y;
        final boolean alive, arrivalGrace;

        EnemySnapshot(RogueEnemy enemy) {
            x = enemy.getTileX();
            y = enemy.getTileY();
            alive = enemy.isAlive();
            arrivalGrace = enemy.hasJustArrived();
        }
    }

    private enum StatusIcon {
        FOOD, WATER, TEMPERATURE, NAUSEA, FEVER, DELIRIUM, DIARRHEA, CRIPPLED, COLLAPSED
    }

    /** One compact HUD condition. The key stays stable while its title/value can change tiers. */
    private static final class StatusChip {
        final String key;
        final StatusIcon icon;
        final String title;
        final String value;
        final String explanation;
        final Color color;

        StatusChip(String key, StatusIcon icon, String title, String value,
                   String explanation, Color color) {
            this.key = key;
            this.icon = icon;
            this.title = title;
            this.value = value;
            this.explanation = explanation;
            this.color = color;
        }
    }

    // Guide Asset 1 terrain cells. Ground variation is deliberately weighted toward subtle grass;
    // giving grass, dirt path, and leaf litter equal odds creates a high-contrast checkerboard even
    // when the selector itself is random. Dirt is reserved for a future explicit path tile type.
    private static final int[] FLOOR_CELLS = {0, 0, 0, 0, 0, 0, 1, 1, 1, 3};
    private static final int DOOR_CELL = 7;                // closed wooden door on ground
    private static final int WELL_CELL = 8;                // stone well on ground
    private static final int[] POND_CELLS = {9, 10};       // pond animation variants
    private static final int[] RIVER_CELLS = {11, 12};     // river animation variants
    // Transparent v2 environment props layered over exposed forest edges. They add silhouette
    // variety without replacing the opaque autotile underneath (and therefore cannot reveal black).
    private static final int[] FOREST_TREE_PROPS = {24, 25, 26, 27}; // young/full/old/dead
    private static final int[] FOREST_BUSH_PROPS = {18, 19, 20};     // fern/bush/berry bush
    private static final int[] FOREST_ROCK_PROPS = {32, 33, 34};     // pebble/rock/boulder
    /** Explored-but-out-of-sight tiles draw dimmed (fog memory) — the sprite twin of the old
     *  colour multiplier. */
    private static final Color DIM_TILE = new Color(0.45f, 0.45f, 0.5f, 1f);
    /** One unknown wall row behind a known treeline: visible as deep canopy, not empty black. */
    private static final Color DEEP_FOREST_TILE = new Color(0.28f, 0.34f, 0.30f, 1f);

    public MarginScreen() {
        font = new BitmapFont(Gdx.files.internal("assets/fonts/m5x7.fnt"));
        font.getData().setScale(UI_FONT_SCALE);
        font.setUseIntegerPositions(true);
        font.getRegion().getTexture().setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        headingFont = new BitmapFont(Gdx.files.internal("assets/fonts/press-start-2p.fnt"));
        headingFont.setUseIntegerPositions(true);
        headingFont.getRegion().getTexture().setFilter(TextureFilter.Nearest, TextureFilter.Nearest);

        Pixmap pixel = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixel.setColor(Color.WHITE);
        pixel.fill();
        uiPixel = new Texture(pixel);
        uiPixel.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        pixel.dispose();

        FovSystem.compute(state);
        // A fresh RunState = a new run: play the Act 0 intro once (Decision 5). restart() does NOT
        // replay it — a new life after death already knows the story.
        intro.start(CorneoIntro.build());
    }

    @Override
    public void render(float delta) {
        updateAnimations(Math.min(delta, 0.05f));
        handleInput();

        RoguePlayer p = state.getPlayer();
        camera.position.set(animatedPixelX(playerMotion, p.getTileX()) + TILE / 2f,
                animatedPixelY(playerMotion, p.getTileY()) + TILE / 2f, 0);
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
        if (menuOpen) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.M)
                    || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) menuOpen = false;
            return;
        }
        // Story 2.5 (AC-2): the passive Journal — a safe-pause lookup surface (AD-14). It sits after
        // the menu-CLOSE block but BEFORE the menu-open check, so the two surfaces are truly mutually
        // exclusive (menu-open swallows J; journal-open swallows M and the ESC-for-menu — pressing ESC
        // with the Journal open closes it, it never opens the menu). While open only J/ESC route (plus
        // the dialogue-surface R-restart precedent) — no PlayerAction, so no turn and no survival tick.
        // The screen never derives quest state; it renders journal.entries (AD-1).
        if (journal.isActive()) {
            if (down(Input.Keys.J) || down(Input.Keys.ESCAPE)) journal.close();
            if (down(Input.Keys.R)) restart(); // a text surface can always restart (dialogue precedent)
            return;
        }
        if (down(Input.Keys.J)) {
            journal.open();
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.M)
                || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            menuOpen = true;
            return;
        }
        handleStatusPointer(p);
        // A turn is atomic in the model but gets a fraction of a second to read on screen. Buffering
        // another command here would skip across the current walk/strike before it was presented.
        if (isWorldAnimating()) return;
        // A consumed selection (its stack was used up) resets the selection so E/K/F/V don't
        // silently no-op on a now-empty slot (edge-review finding).
        if (selectedSlot >= 0 && state.getInventory().backpackType(selectedSlot) < 0) selectedSlot = -1;

        PlayerAction action = readAction(p.getFacing());
        if (action != null) {
            // Story 2.3 review fix (H1): the coach observes only COMMITTED turns. advanceAnimated
            // returns whether the engine actually advanced the clock (refused actions — a wall bump,
            // an empty collect, a craft without materials — commit no turn and must not be
            // acknowledged as a performed control).
            if (advanceAnimated(action)) tutorial.onAction(action, state);
            // Story 2.4: the capture resolves the acted turn the tutorial completes (the guard
            // makes it a safe every-frame call; it fires once and never again). If the party were
            // somehow already empty, resolve() no-ops — nothing to capture.
            if (tutorial.isComplete()) capture.resolve(state);
        }
    }

    /** Capture actor positions, resolve one normal turn, then animate the resulting deltas.
     *  Returns whether the turn committed (the engine refused the action / it was a wall bump). */
    private boolean advanceAnimated(PlayerAction action) {
        RoguePlayer player = state.getPlayer();
        int playerX = player.getTileX(), playerY = player.getTileY();
        int clockBefore = state.getClockTurns();

        Companion companion = state.getActiveCompanion();
        int companionX = companion != null ? companion.getTileX() : 0;
        int companionY = companion != null ? companion.getTileY() : 0;

        IdentityHashMap<RogueEnemy, EnemySnapshot> before = new IdentityHashMap<>();
        for (RogueEnemy enemy : state.getEnemies()) before.put(enemy, new EnemySnapshot(enemy));

        TurnResult result = turnEngine.advance(state, action); // log is fed inside the engine (AD-4)
        if (state.getClockTurns() == clockBefore) return false; // refused action / wall bump: no turn

        if (playerX != player.getTileX() || playerY != player.getTileY()) {
            playerMotion = new Motion(playerX, playerY, player.getTileX(), player.getTileY());
        }
        if (action.kind == PlayerAction.Kind.ATTACK) playerAttack = new AttackAnimation(action.dir);

        Companion currentCompanion = state.getActiveCompanion();
        if (companion != null && companion == currentCompanion
                && (companionX != companion.getTileX() || companionY != companion.getTileY())) {
            companionMotion = new Motion(companionX, companionY,
                    companion.getTileX(), companion.getTileY());
        }

        for (RogueEnemy enemy : state.getEnemies()) {
            EnemySnapshot old = before.get(enemy);
            if (old == null || !old.alive || !enemy.isAlive()) continue;
            if (old.x != enemy.getTileX() || old.y != enemy.getTileY()) {
                enemyMotions.put(enemy, new Motion(old.x, old.y,
                        enemy.getTileX(), enemy.getTileY()));
            }
        }

        // The combat system can resolve dodges and blocks without changing HP, so use its emitted
        // combat lines as the authoritative signal that an adjacent alerted soldier struck.
        boolean enemyStrikeResolved = result.messages.stream().anyMatch(line ->
                line.equals("Dodge!") || line.startsWith("Hit for ") || line.startsWith("Brace! Blocked "));
        if (enemyStrikeResolved) {
            for (RogueEnemy enemy : state.getEnemies()) {
                EnemySnapshot old = before.get(enemy);
                if (old == null || !old.alive || old.arrivalGrace || !enemy.isAlive()) continue;
                boolean stayed = old.x == enemy.getTileX() && old.y == enemy.getTileY();
                if (stayed && enemy.getDetection() == Detection.ALERTED
                        && enemy.isAdjacentTo(player.getTileX(), player.getTileY())) {
                    enemyAttacks.put(enemy, new AttackAnimation(directionToward(
                            enemy.getTileX(), enemy.getTileY(), player.getTileX(), player.getTileY())));
                }
            }
        }
        return true; // the clock advanced — this action committed a turn
    }

    /** Restart the run: close any open scene, reset state, recompute FOV, clear the backpack
     *  selection. The single restart path for both game-over [R] and a live-scene [R] (Decision 6 —
     *  a restart always closes an open scene); the new life announces itself (deletion-check
     *  finding) instead of silently re-showing the opening line. */
    private void restart() {
        dialog.end();
        intro.end(); // close any open surface — symmetric with dialog.end() (review: keep the invariant honest)
        tutorial.skip(); // Story 2.3: a new life after death gets no coaching (Decision 6)
        journal.close(); // Story 2.5: a new life opens no Journal surface (transient — quest state rides the reset flagStore)
        // Story 2.4 (Decision 3): the capture is NOT re-armed — a restarted life keeps Aldric.
        // skip() clears isComplete(), so the resolve gate above cannot fire on the new life.
        state.restart();
        FovSystem.compute(state);
        clearAnimations();
        gameOver = false;
        menuOpen = false;
        pinnedStatusKey = null;
        selectedSlot = -1;
        state.appendMessages(List.of("Another life. [WASD] move."));
    }

    private PlayerAction readAction(int facing) {
        // Q first: the aimed melee (combat fix #3). Q alone swings at facing; Q while a direction
        // is HELD aims that tile (all 8 directions — diagonals included). Checked before the move
        // keys so a Q+W same-frame press reads as an aimed attack, not a move.
        if (down(Input.Keys.Q))     return PlayerAction.attack(attackDirection(facing));
        if (down(Input.Keys.W) || down(Input.Keys.UP))    return PlayerAction.move(0, 1, 1);
        if (down(Input.Keys.S) || down(Input.Keys.DOWN))  return PlayerAction.move(0, -1, 0);
        if (down(Input.Keys.A) || down(Input.Keys.LEFT))  return PlayerAction.move(-1, 0, 2);
        if (down(Input.Keys.D) || down(Input.Keys.RIGHT)) return PlayerAction.move(1, 0, 3);
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
        // Story 2.4 (review H1): the discovery note is read with E even though it is not a
        // provision (reading is narration). It is NOT added to isProvision() — that would route
        // it through ConsumptionSystem as food; the explicit gate keeps the note inert-and-readable.
        if (down(Input.Keys.E) && s != null && (s.isProvision() || s == Supply.TORN_PAGE))
            return PlayerAction.use(s.ordinal(), facing);
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
    private boolean held(int key) { return Gdx.input.isKeyPressed(key); }

    /** The aimed direction for a Q press: the currently HELD direction key, resolved to the full
     *  8-direction set (a held W+D aims NORTHEAST), or the facing when no direction is held. */
    private int attackDirection(int facing) {
        int dx = (held(Input.Keys.A) || held(Input.Keys.LEFT)) ? -1
               : (held(Input.Keys.D) || held(Input.Keys.RIGHT)) ? 1 : 0;
        int dy = (held(Input.Keys.W) || held(Input.Keys.UP)) ? 1
               : (held(Input.Keys.S) || held(Input.Keys.DOWN)) ? -1 : 0;
        if (dx == 0 && dy == 0) return facing; // Q alone: the classic front swing
        return RoguePlayer.directionOf(dx, dy);
    }

    /** Clicking a condition pins its explanation; clicking it again or elsewhere closes it. */
    private void handleStatusPointer(RoguePlayer player) {
        if (!Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) return;
        StatusChip hit = statusChipAtPointer(player);
        if (hit == null) {
            pinnedStatusKey = null;
        } else {
            pinnedStatusKey = hit.key.equals(pinnedStatusKey) ? null : hit.key;
        }
    }

    private void renderWorld() {
        RogueTileMap map = state.getTileMap();
        RoguePlayer p = state.getPlayer();
        int px = p.getTileX(), py = p.getTileY();
        int cols = WW / TILE / 2 + 2;
        int rows = WH / TILE / 2 + 2;

        // One batch pass for the whole world: v2 environment tiles first, then actors/items on top.
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        // Tiles — visible in full colour, explored-but-unseen dimmed (fog memory), unknown hidden.
        // The sole exception is one unknown WALL row touching a known wall: drawing that darker
        // second canopy layer gives the border depth without revealing any walkable map space.
        for (int x = px - cols; x <= px + cols; x++) {
            for (int y = py - rows; y <= py + rows; y++) {
                int tile = map.getTile(x, y);
                if (tile < 0) continue;
                boolean vis = map.isVisible(x, y);
                boolean explored = map.isExplored(x, y);
                boolean deepForest = !vis && !explored && tile == RogueTile.WALL
                        && touchesKnownForest(map, x, y);
                if (!vis && !explored && !deepForest) continue;
                batch.setColor(deepForest ? DEEP_FOREST_TILE : vis ? Color.WHITE : DIM_TILE);
                batch.draw(tileRegion(map, x, y), x * TILE, y * TILE, TILE, TILE);
                int structureCell = map.getStructureTile(x, y);
                if (structureCell >= 0) {
                    TextureRegion structure = map.getStructureType(x, y)
                            == RogueTileMap.STRUCTURE_GRAVEYARD
                            ? pixels.graveyard(structureCell) : pixels.oldHouse(structureCell);
                    batch.draw(structure, x * TILE, y * TILE, TILE, TILE);
                }
            }
        }

        // A sparse prop pass breaks the repeated autotile silhouette while keeping its continuous
        // collision-readable boundary. Details inherit the wall's visibility tint and are drawn
        // below all actors/items.
        for (int x = px - cols; x <= px + cols; x++) {
            for (int y = py - rows; y <= py + rows; y++) {
                if (map.getTile(x, y) != RogueTile.WALL || map.getStructureTile(x, y) >= 0) continue;
                int mask = forestMask(map, x, y);
                if (mask == 0) continue; // deep interior has no clearing-facing edge to decorate

                boolean vis = map.isVisible(x, y);
                boolean explored = map.isExplored(x, y);
                boolean deepForest = !vis && !explored && touchesKnownForest(map, x, y);
                if (!vis && !explored && !deepForest) continue;

                int prop = forestEdgeProp(x, y);
                if (prop < 0) continue;
                batch.setColor(deepForest ? DEEP_FOREST_TILE : vis ? Color.WHITE : DIM_TILE);
                drawForestProp(prop, x, y, mask);
            }
        }
        batch.setColor(Color.WHITE);

        // Floor items (native 16px icons), enemies, companion, Klein.
        for (FloorItem it : state.getFloorItems()) {
            if (map.isVisible(it.x, it.y)) {
                // Review H2: guard an unknown icon (a Supply appended without an ITEM_ICONS entry)
                // so it never reaches pixels.item(-1); an icon-less item just isn't drawn.
                int icon = iconFor(it.type);
                if (icon >= 0)
                    drawSprite(pixels.item(icon), it.x * TILE, it.y * TILE, ITEM_ICON, ITEM_ICON);
            }
        }
        for (RogueEnemy e : state.getEnemies()) {
            Motion motion = enemyMotions.get(e);
            if (e.isAlive() && actorIsVisible(map, motion, e.getTileX(), e.getTileY())) {
                float ex = animatedPixelX(motion, e.getTileX());
                float ey = animatedPixelY(motion, e.getTileY());
                AttackAnimation attack = enemyAttacks.get(e);
                float grounding = structureGrounding(map, motion, e.getTileX(), e.getTileY());
                drawAnimatedActor(ENEMY_CHARACTER, motion, attack, ex, ey, grounding);
                drawEnemyHealthBar(e, ex, ey);
            }
        }
        Companion comp = state.getActiveCompanion();
        if (comp != null && actorIsVisible(map, companionMotion, comp.getTileX(), comp.getTileY())) {
            float cx = animatedPixelX(companionMotion, comp.getTileX());
            float cy = animatedPixelY(companionMotion, comp.getTileY());
            float grounding = structureGrounding(map, companionMotion, comp.getTileX(), comp.getTileY());
            drawAnimatedActor(COMPANION_CHARACTER, companionMotion, null, cx, cy, grounding);
            drawCompanionHealthBar(comp, cx, cy);
        }
        float playerX = animatedPixelX(playerMotion, px);
        float playerY = animatedPixelY(playerMotion, py);
        drawAnimatedActor(PLAYER_CHARACTER, playerMotion, playerAttack, playerX, playerY,
                structureGrounding(map, playerMotion, px, py));

        // The south wall has a tall inward-facing stone lip. Redraw only that transparent strip
        // above actors so characters immediately inside the house disappear naturally behind the
        // masonry instead of looking as though they are standing on top of it.
        drawStructureForeground(map, px, py, cols, rows);

        // Effects sit above actors so the strike remains readable against both grass and canopy.
        if (playerAttack != null) drawSlashEffect(animatedPixelX(playerMotion, px),
                animatedPixelY(playerMotion, py), playerAttack);
        for (Map.Entry<RogueEnemy, AttackAnimation> entry : enemyAttacks.entrySet()) {
            RogueEnemy enemy = entry.getKey();
            if (!enemy.isAlive() || !map.isVisible(enemy.getTileX(), enemy.getTileY())) continue;
            Motion motion = enemyMotions.get(enemy);
            drawSlashEffect(animatedPixelX(motion, enemy.getTileX()),
                    animatedPixelY(motion, enemy.getTileY()), entry.getValue());
        }

        batch.end();
    }

    private void drawStructureForeground(RogueTileMap map, int px, int py, int cols, int rows) {
        for (int x = px - cols; x <= px + cols; x++) {
            for (int y = py - rows; y <= py + rows; y++) {
                int structureCell = map.getStructureTile(x, y);
                if (structureCell < 0 || map.getStructureType(x, y)
                        != RogueTileMap.STRUCTURE_OLD_HOUSE) continue;
                boolean vis = map.isVisible(x, y);
                boolean explored = map.isExplored(x, y);
                if (!vis && !explored) continue;
                batch.setColor(vis ? Color.WHITE : DIM_TILE);
                batch.draw(pixels.oldHouseForeground(structureCell),
                        x * TILE, y * TILE, TILE, TILE);
            }
        }
        batch.setColor(Color.WHITE);
    }

    /** Aldric's compact health readout above his head, in a friendly BLUE (the HUD's water-blue) —
     *  clearly an ALLY, distinct from the enemies' red bars. Drawn whenever he is visible; an empty
     *  bar marks where he fell. */
    private void drawCompanionHealthBar(Companion comp, float actorX, float actorY) {
        drawBar(actorX + 2, actorY + TILE - 3, TILE - 4, 4,
                comp.getHp() / (float) Math.max(1, comp.getMaxHp()), UI_WATER);
    }

    /** A compact always-visible combat readout anchored to the top of a visible enemy tile. */
    private void drawEnemyHealthBar(RogueEnemy enemy, float actorX, float actorY) {
        float x = actorX + 2;
        float y = actorY + TILE - 3;
        float width = TILE - 4;
        float ratio = enemy.getHp() / (float) Math.max(1, enemy.getMaxHp());
        float filled = Math.max(1f, (width - 2) * Math.max(0f, Math.min(1f, ratio)));

        fillRect(x, y, width, 4, ENEMY_BAR_BORDER);
        fillRect(x + 1, y + 1, width - 2, 2, ENEMY_BAR_EMPTY);
        if (enemy.getHp() > 0) fillRect(x + 1, y + 1, filled, 2, ENEMY_BAR_HEALTH);
    }

    private void drawSlashEffect(float sourceX, float sourceY, AttackAnimation attack) {
        float progress = attack.progress();
        // The impact frame gets the longest hold; a perfectly even four-way split made the most
        // readable part of this deliberately tiny effect disappear too quickly at runtime.
        int frame = progress < 0.14f ? 0 : progress < 0.38f ? 1 : progress < 0.82f ? 2 : 3;
        float size = 28f;
        float x = sourceX + directionX(attack.direction) * TILE + (TILE - size) / 2f;
        float y = sourceY + directionY(attack.direction) * TILE + (TILE - size) / 2f;
        // CCW angle from east (the slash base sprite is east-pointing): cardinals resolve to the
        // exact old 0/90/180/-90 values; diagonals get their ±45/±135 (combat fix #3).
        float rotation = (float) Math.toDegrees(
                Math.atan2(directionY(attack.direction), directionX(attack.direction)));
        batch.setColor(Color.WHITE);
        batch.draw(pixels.meleeSlash(frame), x, y, size / 2f, size / 2f,
                size, size, 1f, 1f, rotation);
    }

    private void updateAnimations(float delta) {
        playerMotion = advanceMotion(playerMotion, delta);
        companionMotion = advanceMotion(companionMotion, delta);
        for (Iterator<Map.Entry<RogueEnemy, Motion>> it = enemyMotions.entrySet().iterator(); it.hasNext();) {
            Motion motion = it.next().getValue();
            motion.elapsed += delta;
            if (motion.elapsed >= MOVE_DURATION) it.remove();
        }

        playerAttack = advanceAttack(playerAttack, delta);
        for (Iterator<Map.Entry<RogueEnemy, AttackAnimation>> it = enemyAttacks.entrySet().iterator(); it.hasNext();) {
            AttackAnimation attack = it.next().getValue();
            attack.elapsed += delta;
            if (attack.elapsed >= ATTACK_DURATION) it.remove();
        }
    }

    private static Motion advanceMotion(Motion motion, float delta) {
        if (motion == null) return null;
        motion.elapsed += delta;
        return motion.elapsed >= MOVE_DURATION ? null : motion;
    }

    private static AttackAnimation advanceAttack(AttackAnimation attack, float delta) {
        if (attack == null) return null;
        attack.elapsed += delta;
        return attack.elapsed >= ATTACK_DURATION ? null : attack;
    }

    private boolean isWorldAnimating() {
        return playerMotion != null || companionMotion != null || !enemyMotions.isEmpty()
                || playerAttack != null || !enemyAttacks.isEmpty();
    }

    private void clearAnimations() {
        playerMotion = null;
        companionMotion = null;
        enemyMotions.clear();
        playerAttack = null;
        enemyAttacks.clear();
    }

    private static float animatedPixelX(Motion motion, int targetX) {
        if (motion == null) return targetX * TILE;
        float t = motion.progress();
        return (motion.fromX + (motion.toX - motion.fromX) * t) * TILE;
    }

    private static float animatedPixelY(Motion motion, int targetY) {
        if (motion == null) return targetY * TILE;
        float t = motion.progress();
        return (motion.fromY + (motion.toY - motion.fromY) * t) * TILE;
    }

    /** Grounded step cadence: hold each painted leg pose for half of the tile movement. */
    private TextureRegion actorRegion(int character, Motion motion, AttackAnimation attack) {
        if (attack != null) {
            float progress = attack.progress();
            if (progress < 0.25f) return pixels.attackingCharacter(character, 0);
            if (progress < 0.50f) return pixels.attackingCharacter(character, 1);
            if (progress < 0.75f) return pixels.attackingCharacter(character, 2);
            return pixels.character(character);
        }
        if (motion == null) return pixels.character(character);
        return pixels.walkingCharacter(character, motion.progress() < 0.5f ? 0 : 1);
    }

    /** Attack cells are wider so the weapon can extend without translating the actor. */
    private void drawAnimatedActor(int character, Motion motion, AttackAnimation attack,
                                   float x, float y, float structureGrounding) {
        boolean attackPose = attack != null && attack.progress() < 0.75f;
        float width = attackPose ? TILE * 1.5f : TILE;
        boolean flip = attack != null
                ? directionX(attack.direction) < 0 // WEST and the western diagonals face the swing
                : motion != null && motion.toX < motion.fromX;
        if (structureGrounding > 0f) drawActorContactShadow(x, y, structureGrounding);
        drawSprite(actorRegion(character, motion, attack), x,
                y - STRUCTURE_ACTOR_DROP * structureGrounding, width, TILE, flip);
    }

    /**
     * Blend grounding in at a doorway rather than making the actor snap vertically as the map cell
     * changes from grass to an interior floor. The outer atlas ring is a transparent, walkable
     * landscape layer, not part of the indoor floor; treating it as indoors erased this transition
     * and gave actors an indoor contact shadow while they were visibly standing outside.
     */
    private static float structureGrounding(RogueTileMap map, Motion motion, int targetX, int targetY) {
        boolean target = isIndoorStructure(map, targetX, targetY);
        if (motion == null) return target ? 1f : 0f;
        boolean origin = isIndoorStructure(map, (int) motion.fromX, (int) motion.fromY);
        if (origin == target) return target ? 1f : 0f;
        return target ? motion.progress() : 1f - motion.progress();
    }

    /** True for the inset 13x8 building body; false for the one-cell exterior visual apron. */
    static boolean isOldHouseInterior(int structureCell) {
        if (structureCell < 0) return false;
        int col = structureCell % OLD_HOUSE_ATLAS_COLS;
        int row = structureCell / OLD_HOUSE_ATLAS_COLS;
        return col >= OLD_HOUSE_APRON && col < OLD_HOUSE_ATLAS_COLS - OLD_HOUSE_APRON
                && row >= OLD_HOUSE_APRON && row < OLD_HOUSE_ATLAS_ROWS - OLD_HOUSE_APRON;
    }

    private static boolean isIndoorStructure(RogueTileMap map, int x, int y) {
        return map.getStructureType(x, y) == RogueTileMap.STRUCTURE_OLD_HOUSE
                && isOldHouseInterior(map.getStructureTile(x, y));
    }

    /** A compact pixel contact shadow directly under the boots, used on detailed indoor floors. */
    private void drawActorContactShadow(float x, float y, float strength) {
        batch.setColor(STRUCTURE_ACTOR_SHADOW.r, STRUCTURE_ACTOR_SHADOW.g,
                STRUCTURE_ACTOR_SHADOW.b, STRUCTURE_ACTOR_SHADOW.a * strength);
        batch.draw(uiPixel, x + 7f, y + 1f, 10f, 2f);
        batch.setColor(Color.WHITE);
    }

    // Canonical 8-dir offsets live on RoguePlayer (CombatSystem resolves attacks through the same
    // source); the screen only renders them. Delegation keeps one mapping (combat fix #3).
    private static int directionX(int direction) { return RoguePlayer.directionX(direction); }
    private static int directionY(int direction) { return RoguePlayer.directionY(direction); }

    private static int directionToward(int fromX, int fromY, int toX, int toY) {
        if (toX < fromX) return RoguePlayer.WEST;
        if (toX > fromX) return RoguePlayer.EAST;
        return toY > fromY ? RoguePlayer.NORTH : RoguePlayer.SOUTH;
    }

    private static boolean actorIsVisible(RogueTileMap map, Motion motion, int targetX, int targetY) {
        return map.isVisible(targetX, targetY) || (motion != null
                && map.isVisible((int) motion.fromX, (int) motion.fromY));
    }

    /** The guide terrain cell for tile (x,y); multi-cell types vary deterministically by position. */
    private TextureRegion tileRegion(RogueTileMap map, int x, int y) {
        switch (map.getTile(x, y)) {
            case RogueTile.WALL:  return pixels.forest(forestMask(map, x, y));
            case RogueTile.DOOR:  return pixels.terrain(DOOR_CELL);
            case RogueTile.WELL:  return pixels.terrain(WELL_CELL);
            case RogueTile.POND:  return pixels.terrain(POND_CELLS[variant(x, y, POND_CELLS)]);
            case RogueTile.RIVER: return pixels.terrain(RIVER_CELLS[variant(x, y, RIVER_CELLS)]);
            default:              return pixels.terrain(FLOOR_CELLS[variant(x, y, FLOOR_CELLS)]);
        }
    }

    /**
     * Four-neighbor autotile mask for a continuous treeline. A bit is set when that side faces a
     * walkable clearing: north=1, east=2, south=4, west=8. Out-of-map space continues the forest.
     */
    private static int forestMask(RogueTileMap map, int x, int y) {
        int mask = 0;
        if (map.isWalkable(x, y + 1)) mask |= 1;
        if (map.isWalkable(x + 1, y)) mask |= 2;
        if (map.isWalkable(x, y - 1)) mask |= 4;
        if (map.isWalkable(x - 1, y)) mask |= 8;
        return mask;
    }

    /** True only for the first unknown forest row surrounding a visible/explored treeline. */
    private static boolean touchesKnownForest(RogueTileMap map, int x, int y) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                int nx = x + dx, ny = y + dy;
                if (map.getTile(nx, ny) == RogueTile.WALL
                        && (map.isVisible(nx, ny) || map.isExplored(nx, ny))) return true;
            }
        }
        return false;
    }

    /** Sparse deterministic decoration: 18% trees, 25% shrubs, 7% rocks, 50% clean edge. */
    private static int forestEdgeProp(int x, int y) {
        int hash = positionHash(x, y, 0x3c6ef372);
        int roll = Math.floorMod(hash, 100);
        if (roll < 18) {
            return FOREST_TREE_PROPS[Math.floorMod(hash >>> 8, FOREST_TREE_PROPS.length)];
        }
        if (roll < 43) {
            return FOREST_BUSH_PROPS[Math.floorMod(hash >>> 8, FOREST_BUSH_PROPS.length)];
        }
        if (roll < 50) {
            return FOREST_ROCK_PROPS[Math.floorMod(hash >>> 8, FOREST_ROCK_PROPS.length)];
        }
        return -1;
    }

    /**
     * Draw a transparent prop pushed toward a clearing-facing side. Centering it in the wall's
     * dark canopy hides the similarly coloured sprite; this placement makes the silhouette break
     * visibly into the clearing while actors remain on top of it.
     */
    private void drawForestProp(int prop, int x, int y, int mask) {
        int hash = positionHash(x, y, 0xa54ff53a);
        boolean tree = prop >= 24 && prop <= 27;
        float size = tree ? 28f : 16f;

        int edgeX = ((mask & 2) != 0 ? 1 : 0) - ((mask & 8) != 0 ? 1 : 0);
        int edgeY = ((mask & 1) != 0 ? 1 : 0) - ((mask & 4) != 0 ? 1 : 0);
        if (edgeX == 0 && edgeY == 0) {
            // Opposite/all-side masks have no average direction. Pick one of their actual clear
            // sides deterministically so the prop still emerges from the canopy.
            int start = Math.floorMod(hash >>> 11, 4);
            int[] bits = {1, 2, 4, 8};
            int[] dx = {0, 1, 0, -1};
            int[] dy = {1, 0, -1, 0};
            for (int i = 0; i < 4; i++) {
                int direction = (start + i) % 4;
                if ((mask & bits[direction]) != 0) {
                    edgeX = dx[direction];
                    edgeY = dy[direction];
                    break;
                }
            }
        }

        float push = tree ? 4f : 5f;
        float jitterX = Math.floorMod(hash, 3) - 1;
        float jitterY = Math.floorMod(hash >>> 5, 3) - 1;
        batch.draw(pixels.tile(prop),
                x * TILE + (TILE - size) / 2f + edgeX * push + jitterX,
                y * TILE + (TILE - size) / 2f + edgeY * push + jitterY,
                size, size);
    }

    /**
     * Stable pseudo-random pick for this map position. A simple linear expression such as
     * {@code x * 31 + y * 17} produces visible diagonal bands after the modulo operation; the
     * avalanche mix below breaks that coordinate symmetry without making tiles flicker.
     */
    private static int variant(int x, int y, int[] cells) {
        int hash = positionHash(x, y, 0x6d2b79f5);
        return Math.floorMod(hash, cells.length);
    }

    /** Avalanche-mixed coordinate hash with a caller-specific salt. */
    private static int positionHash(int x, int y, int salt) {
        int hash = x * 0x1f1f1f1f ^ y * 0x5f356495 ^ salt;
        hash ^= hash >>> 16;
        hash *= 0x7feb352d;
        hash ^= hash >>> 15;
        hash *= 0x846ca68b;
        hash ^= hash >>> 16;
        return hash;
    }

    /** Draw {@code region} centered on tile (tx,ty), scaled to w×h px. */
    private void drawSprite(TextureRegion region, float tx, float ty, float w, float h) {
        drawSprite(region, tx, ty, w, h, false);
    }

    /** Draw a centered sprite, optionally mirrored horizontally while preserving its baseline. */
    private void drawSprite(TextureRegion region, float tx, float ty, float w, float h, boolean flipX) {
        float x = tx + (TILE - w) / 2f;
        float y = ty + (TILE - h) / 2f;
        if (flipX) batch.draw(region, x + w, y, -w, h);
        else batch.draw(region, x, y, w, h);
    }

    /** The item-icon cell for a Supply type id, or -1 if the pack has no usable icon. */
    private static int iconFor(int type) {
        return type >= 0 && type < ITEM_ICONS.length ? ITEM_ICONS[type] : -1;
    }

    private void renderHud() {
        RoguePlayer p = state.getPlayer();
        batch.getProjectionMatrix().setToOrtho2D(0, 0, WW, WH);
        batch.begin();

        renderStatusPanel(p);
        renderBackpackPanel();
        renderStatusExplanation(p);

        // The bottom message log (NFR-3, AD-15): the PRIMARY text surface. Log-window policy (2.1
        // Decision 1, extended to the 2.2 intro): while the intro or a dialogue scene is open it
        // shows that PAGE (speaker + text + choices/footer), not the last-5 event lines; the event
        // window resumes when the page closes. The intro plays first (app start), so it takes
        // precedence. The log is core-owned (AD-1); never built here.
        if (intro.isActive()) {
            renderTextPage(intro.getCurrent(), "[SPACE] continue   [ESC] skip");
        } else if (dialog.isActive()) {
            renderTextPage(dialog.getCurrent(), "[SPACE] continue");
        } else if (journal.isActive()) {
            renderJournalPage();
        } else {
            renderMessagePanel();
        }

        if (gameOver) renderGameOverPanel();
        else if (menuOpen) renderMenuPanel();

        batch.end();
    }

    /** Compact top-left survival panel: condition names live in hover/click cards, not the bar. */
    private void renderStatusPanel(RoguePlayer p) {
        int x = HUD_MARGIN;
        int y = TOP_PANEL_Y;
        drawPanel(x, y, STATUS_PANEL_W, TOP_PANEL_H, UI_PANEL);

        drawText("HP " + p.getHp() + "/" + p.getMaxHp(), x + 6, y + 53, UI_HEALTH);
        drawBar(x + 54, y + 47, 66, 4, p.getHp() / (float) Math.max(1, p.getMaxHp()), UI_HEALTH);
        String time = (state.isDay() ? "DAY " : "NIGHT ") + state.getClockTurns()
                + "  " + state.getWeather().label().toUpperCase();
        drawText(fitText(time, 116), x + 130, y + 53, UI_ACCENT);

        List<StatusChip> chips = statusChips(p);
        StatusChip hovered = statusChipAtPointer(p);
        int chipX = x + 6;
        int chipY = y + 5;
        for (StatusChip chip : chips) {
            boolean active = chip == hovered || chip.key.equals(pinnedStatusKey);
            fillRect(chipX, chipY, STATUS_CHIP_W, STATUS_CHIP_H,
                    active ? new Color(0.17f, 0.18f, 0.11f, 0.98f) : UI_SLOT);
            strokeRect(chipX, chipY, STATUS_CHIP_W, STATUS_CHIP_H,
                    active ? chip.color : UI_BORDER);
            drawStatusIcon(chip.icon, chipX + 10, chipY + 15, chip.color);
            if (!chip.value.isEmpty()) {
                font.setColor(active ? Color.WHITE : UI_TEXT);
                font.draw(batch, fitText(chip.value, STATUS_CHIP_W - 4), chipX + 2, chipY + 9,
                        STATUS_CHIP_W - 4, Align.center, false);
            }
            chipX += STATUS_CHIP_W + STATUS_CHIP_GAP;
        }
    }

    private List<StatusChip> statusChips(RoguePlayer p) {
        List<StatusChip> chips = new ArrayList<>(7);

        Color hungerColor = p.getStatus() == RoguePlayer.HungerStatus.STARVING ? UI_HEALTH
                : p.getStatus() == RoguePlayer.HungerStatus.HUNGRY ? UI_WARNING : UI_FOOD;
        chips.add(new StatusChip("food", StatusIcon.FOOD, hungerTitle(p),
                Integer.toString(p.getHunger()), hungerExplanation(p), hungerColor));

        Color thirstColor = p.getThirstStatus() == RoguePlayer.ThirstStatus.PARCHED ? UI_HEALTH
                : p.getThirstStatus() == RoguePlayer.ThirstStatus.DEHYDRATED ? UI_WARNING : UI_WATER;
        chips.add(new StatusChip("water", StatusIcon.WATER, thirstTitle(p),
                Integer.toString(p.getThirst()), thirstExplanation(p), thirstColor));

        RoguePlayer.TempBand band = p.getTempBand();
        boolean extremeTemp = band == RoguePlayer.TempBand.FROZEN
                || band == RoguePlayer.TempBand.OVERHEATED;
        boolean warningTemp = band == RoguePlayer.TempBand.COLD || band == RoguePlayer.TempBand.HOT;
        chips.add(new StatusChip("temperature", StatusIcon.TEMPERATURE, tempTitle(band),
                Integer.toString(p.getTemperature()), tempExplanation(band),
                extremeTemp ? UI_HEALTH : warningTemp ? UI_WARNING : UI_TEXT));

        switch (p.getBacterialStage()) {
            case NAUSEA:
                chips.add(new StatusChip("bacterial", StatusIcon.NAUSEA, "NAUSEA",
                        Integer.toString(p.getBacterialTimer()),
                        "Sickness reduces Strength by 30%. If untreated, it escalates to Fever.",
                        UI_WARNING));
                break;
            case FEVER:
                chips.add(new StatusChip("bacterial", StatusIcon.FEVER, "FEVER",
                        Integer.toString(p.getBacterialTimer()),
                        "Severe sickness reduces Strength by 40%. If untreated, it escalates to Delirium.",
                        UI_HEALTH));
                break;
            case DELIRIUM:
                chips.add(new StatusChip("bacterial", StatusIcon.DELIRIUM, "DELIRIUM",
                        Integer.toString(p.getBacterialTimer()),
                        "Vertigo lowers dodge and Crippled can interrupt movement. Use a cure.",
                        UI_HEALTH));
                break;
            default:
                break;
        }

        if (p.getDiarrheaStage() != RoguePlayer.DiarrheaStage.NONE) {
            boolean severe = p.getDiarrheaStage() == RoguePlayer.DiarrheaStage.STAGE_2;
            chips.add(new StatusChip("diarrhea", StatusIcon.DIARRHEA,
                    severe ? "DIARRHEA II" : "DIARRHEA I",
                    severe ? "" : Integer.toString(p.getDiarrheaTimer()),
                    severe
                            ? "Critical dehydration: thirst and hunger drain three times faster."
                            : "Thirst drains twice as fast. It worsens if the timer reaches zero.",
                    severe ? UI_HEALTH : UI_WARNING));
        }
        if (p.isRotgutCrippled()) {
            chips.add(new StatusChip("rotgut", StatusIcon.CRIPPLED, "ROTGUT",
                    "", "Persistent Crippled can interrupt movement. It remains until cured.",
                    UI_HEALTH));
        }
        if (p.isCollapsed()) {
            chips.add(new StatusChip("collapsed", StatusIcon.COLLAPSED, "COLLAPSED",
                    Integer.toString(p.getMaxHp()),
                    "Your maximum HP is capped at 40% until a cure removes the collapse.",
                    UI_HEALTH));
        }
        return chips;
    }

    private static String hungerTitle(RoguePlayer p) {
        switch (p.getStatus()) {
            case WELL_FED: return "WELL FED";
            case SATISFIED: return "SATISFIED";
            case HUNGRY: return "HUNGRY";
            default: return "STARVING";
        }
    }

    private static String hungerExplanation(RoguePlayer p) {
        switch (p.getStatus()) {
            case WELL_FED:
                return "You are full. Health regenerates, though early bloating may slow movement.";
            case SATISFIED:
                return "You are comfortably fed. No hunger penalty; the number is turns remaining.";
            case HUNGRY:
                return "Food is running low. Eat before the countdown falls into Starving.";
            default:
                return "Severe hunger cuts Strength by 35% and drains HP. Eat as soon as possible.";
        }
    }

    private static String thirstTitle(RoguePlayer p) {
        switch (p.getThirstStatus()) {
            case HYDRATED: return "HYDRATED";
            case THIRSTY: return "THIRSTY";
            case DEHYDRATED: return "DEHYDRATED";
            default: return "PARCHED";
        }
    }

    private static String thirstExplanation(RoguePlayer p) {
        switch (p.getThirstStatus()) {
            case HYDRATED:
                return "You have enough water. No thirst penalty; the number is turns remaining.";
            case THIRSTY:
                return "Your water reserve is falling. Drink before dehydration sets in.";
            case DEHYDRATED:
                return "Dehydration is setting in. Find safe water before becoming Parched.";
            default:
                return "Critical thirst drains 2 HP every 5 turns and can cause trembling.";
        }
    }

    private static String tempTitle(RoguePlayer.TempBand band) {
        switch (band) {
            case FROZEN: return "FROZEN";
            case COLD: return "COLD";
            case CHILLED: return "CHILLED";
            case NEUTRAL: return "NEUTRAL";
            case WARM: return "WARM";
            case HOT: return "HOT";
            default: return "OVERHEATED";
        }
    }

    private static String tempExplanation(RoguePlayer.TempBand band) {
        switch (band) {
            case FROZEN:
                return "Lethal cold damages HP each exposure tick. Reach warmth immediately.";
            case COLD:
                return "Dangerous cold. Seek shelter, fire, or warmer weather.";
            case CHILLED:
                return "You are getting cold, but have not reached the dangerous bands yet.";
            case NEUTRAL:
                return "Your body temperature is stable and carries no exposure penalty.";
            case WARM:
                return "You are getting warm, but have not reached the dangerous bands yet.";
            case HOT:
                return "Dangerous heat. Find shade or cooler conditions.";
            default:
                return "Lethal heat damages HP each exposure tick. Cool down immediately.";
        }
    }

    private StatusChip statusChipAtPointer(RoguePlayer p) {
        if (!updateHudPointer()) return null;
        float localX = pointerHud.x - (HUD_MARGIN + 6);
        float localY = pointerHud.y - (TOP_PANEL_Y + 5);
        if (localX < 0 || localY < 0 || localY >= STATUS_CHIP_H) return null;
        int stride = STATUS_CHIP_W + STATUS_CHIP_GAP;
        int index = (int) (localX / stride);
        if (localX - index * stride >= STATUS_CHIP_W) return null;
        List<StatusChip> chips = statusChips(p);
        return index >= 0 && index < chips.size() ? chips.get(index) : null;
    }

    private boolean updateHudPointer() {
        int screenX = viewport.getScreenX();
        int screenY = viewport.getScreenY();
        int screenW = viewport.getScreenWidth();
        int screenH = viewport.getScreenHeight();
        float rawX = Gdx.input.getX();
        float rawY = Gdx.graphics.getHeight() - Gdx.input.getY();
        if (screenW <= 0 || screenH <= 0 || rawX < screenX || rawX >= screenX + screenW
                || rawY < screenY || rawY >= screenY + screenH) return false;
        pointerHud.set((rawX - screenX) * WW / screenW, (rawY - screenY) * WH / screenH);
        return true;
    }

    private void renderStatusExplanation(RoguePlayer p) {
        StatusChip chip = statusChipAtPointer(p);
        if (chip == null && pinnedStatusKey != null) {
            for (StatusChip candidate : statusChips(p)) {
                if (candidate.key.equals(pinnedStatusKey)) {
                    chip = candidate;
                    break;
                }
            }
        }
        if (chip == null) return;

        int x = HUD_MARGIN;
        int y = TOP_PANEL_Y - 70;
        int width = STATUS_PANEL_W;
        int height = 66;
        drawPanel(x, y, width, height, UI_PANEL_STRONG);
        drawStatusIcon(chip.icon, x + 9, y + height - 20, chip.color);
        drawHeading(chip.title, x + 28, y + height - 10, chip.color);
        fillRect(x + 7, y + height - 26, width - 14, 1, UI_BORDER);
        List<String> lines = wrapText(chip.explanation, width - 16);
        int baseline = y + height - 37;
        for (int i = 0; i < Math.min(3, lines.size()); i++) {
            drawText(lines.get(i), x + 8, baseline, UI_TEXT);
            baseline -= UI_LINE;
        }
        drawText(chip.key.equals(pinnedStatusKey) ? "CLICK ICON TO CLOSE" : "HOVER / CLICK TO PIN",
                x + 8, y + 7, UI_MUTED);
    }

    private void drawStatusIcon(StatusIcon icon, int x, int y, Color color) {
        switch (icon) {
            case FOOD:
                fillRect(x + 1, y + 3, 7, 7, color);
                fillRect(x + 7, y + 2, 3, 3, UI_TEXT);
                fillRect(x + 9, y + 1, 2, 2, UI_TEXT);
                break;
            case WATER:
                fillRect(x + 5, y + 9, 2, 2, color);
                fillRect(x + 4, y + 6, 4, 4, color);
                fillRect(x + 2, y + 3, 8, 4, color);
                fillRect(x + 4, y + 1, 4, 2, color);
                break;
            case TEMPERATURE:
                fillRect(x + 5, y + 4, 3, 8, UI_TEXT);
                fillRect(x + 6, y + 5, 1, 6, color);
                fillRect(x + 3, y + 1, 7, 5, color);
                break;
            case NAUSEA:
                fillRect(x + 2, y + 8, 8, 2, color);
                fillRect(x + 2, y + 3, 2, 5, color);
                fillRect(x + 4, y + 2, 6, 2, color);
                fillRect(x + 8, y + 4, 2, 3, color);
                break;
            case FEVER:
                fillRect(x + 4, y + 1, 5, 4, color);
                fillRect(x + 2, y + 4, 8, 4, color);
                fillRect(x + 5, y + 7, 4, 4, color);
                fillRect(x + 7, y + 10, 2, 2, color);
                break;
            case DELIRIUM:
                fillRect(x + 1, y + 5, 10, 3, color);
                fillRect(x + 3, y + 3, 6, 7, color);
                fillRect(x + 5, y + 5, 2, 3, UI_PANEL_STRONG);
                break;
            case DIARRHEA:
                fillRect(x + 2, y + 7, 2, 4, color);
                fillRect(x + 5, y + 4, 2, 7, color);
                fillRect(x + 8, y + 1, 2, 10, color);
                fillRect(x + 1, y + 1, 10, 2, color);
                break;
            case CRIPPLED:
                fillRect(x + 1, y + 2, 3, 3, color);
                fillRect(x + 3, y + 4, 3, 3, color);
                fillRect(x + 5, y + 6, 3, 3, color);
                fillRect(x + 7, y + 8, 3, 3, color);
                fillRect(x + 8, y + 1, 2, 4, UI_TEXT);
                break;
            case COLLAPSED:
                fillRect(x + 1, y + 6, 10, 4, color);
                fillRect(x + 3, y + 3, 6, 6, color);
                fillRect(x + 5, y + 1, 2, 3, color);
                fillRect(x + 6, y + 4, 2, 5, UI_PANEL_STRONG);
                break;
            default:
                break;
        }
    }

    /** Eight-slot icon quickbar; only the selected stack spends horizontal space on its name. */
    private void renderBackpackPanel() {
        int x = PACK_PANEL_X;
        int y = TOP_PANEL_Y;
        drawPanel(x, y, PACK_PANEL_W, TOP_PANEL_H, UI_PANEL);

        drawHeading("BACKPACK", x + 6, y + 53, UI_MUTED);
        Inventory inv = state.getInventory();
        String selected = "[TAB] SELECT ITEM";
        if (selectedSlot >= 0 && inv.backpackType(selectedSlot) >= 0) {
            int type = inv.backpackType(selectedSlot);
            selected = state.getIdentifyMap().displayNameFor(type) + " x" + inv.backpackCount(selectedSlot);
        }
        drawText(fitText(selected, PACK_PANEL_W - 12), x + 6, y + 42,
                selectedSlot >= 0 ? UI_ACCENT : UI_MUTED);

        int slotSize = 22;
        int gap = 2;
        int sx = x + 8;
        int sy = y + 5;
        for (int slot = 0; slot < Inventory.BACKPACK_STACKS; slot++) {
            int type = inv.backpackType(slot);
            boolean sel = slot == selectedSlot;
            int slotX = sx + slot * (slotSize + gap);
            fillRect(slotX, sy, slotSize, slotSize, sel ? new Color(0.20f, 0.18f, 0.09f, 0.96f) : UI_SLOT);
            strokeRect(slotX, sy, slotSize, slotSize, sel ? UI_ACCENT : UI_BORDER);
            if (type < 0) continue;
            int icon = iconFor(type);
            if (icon >= 0) {
                batch.setColor(Color.WHITE);
                batch.draw(pixels.item(icon), slotX + 3, sy + 3, ITEM_ICON, ITEM_ICON);
            }
            int count = inv.backpackCount(slot);
            if (count > 1) {
                drawText(Integer.toString(count), slotX + 14, sy + 9, sel ? Color.WHITE : UI_TEXT);
            }
        }
    }

    /** Bottom event panel uses wrapped visual lines, so long messages cannot leave the screen. */
    private void renderMessagePanel() {
        int x = HUD_MARGIN;
        int width = WW - HUD_MARGIN * 2;
        drawPanel(x, LOG_PANEL_Y, width, LOG_PANEL_H, UI_PANEL);
        drawHeading("EVENTS", x + 6, LOG_PANEL_Y + 59, UI_MUTED);
        font.setColor(UI_MUTED);
        font.draw(batch, "[M] MENU", x + 6, LOG_PANEL_Y + 59,
                width - 12, Align.right, false);

        List<String> lines = recentLogLines(width - 12, LOG_LINES);
        int baseline = LOG_PANEL_Y + 48;
        for (String line : lines) {
            drawText(line, x + 6, baseline, UI_TEXT);
            baseline -= UI_LINE;
        }

    }

    /** Pauses the run and keeps every input reminder off the permanent HUD. */
    private void renderMenuPanel() {
        fillRect(0, 0, WW, WH, new Color(0.01f, 0.015f, 0.012f, 0.68f));

        int x = 78, y = 50, width = 324, height = 260;
        drawPanel(x, y, width, height, UI_PANEL_STRONG);
        headingFont.setColor(UI_ACCENT);
        headingFont.draw(batch, "MENU", x, y + height - 16, width, Align.center, false);
        fillRect(x + 12, y + height - 27, width - 24, 1, UI_BORDER);

        drawHeading("CONTROLS", x + 16, y + height - 40, UI_MUTED);

        int left = x + 18;
        int right = x + 168;
        int top = y + height - 58;

        drawText("WASD / ARROWS", left, top, UI_ACCENT);
        drawText("Move", left + 78, top, UI_TEXT);
        drawText("Q", left, top - 14, UI_ACCENT);
        drawText("Attack", left + 78, top - 14, UI_TEXT);
        drawText("G", left, top - 28, UI_ACCENT);
        drawText("Take item", left + 78, top - 28, UI_TEXT);
        drawText("SPACE", left, top - 42, UI_ACCENT);
        drawText("Wait", left + 78, top - 42, UI_TEXT);

        drawText("TAB / ]", right, top, UI_ACCENT);
        drawText("Next item", right + 54, top, UI_TEXT);
        drawText("[", right, top - 14, UI_ACCENT);
        drawText("Previous item", right + 54, top - 14, UI_TEXT);
        drawText("E", right, top - 28, UI_ACCENT);
        drawText("Use item", right + 54, top - 28, UI_TEXT);
        drawText("J", right, top - 42, UI_ACCENT);
        drawText("Journal", right + 54, top - 42, UI_TEXT);

        fillRect(x + 12, y + 128, width - 24, 1, UI_BORDER);
        drawHeading("SURVIVAL", x + 16, y + 116, UI_MUTED);
        drawText("C  Forage", left, y + 98, UI_TEXT);
        drawText("B  Campfire", left, y + 84, UI_TEXT);
        drawText("T  Craft torch", left, y + 70, UI_TEXT);
        drawText("K  Cook", right, y + 98, UI_TEXT);
        drawText("F  Filter water", right, y + 84, UI_TEXT);
        drawText("V  Boil water", right, y + 70, UI_TEXT);

        fillRect(x + 12, y + 48, width - 24, 1, UI_BORDER);
        font.setColor(UI_ACCENT);
        font.draw(batch, "[M / ESC] RESUME", x, y + 24, width, Align.center, false);
    }

    private List<String> recentLogLines(int maxWidth, int maxLines) {
        List<String> visual = new ArrayList<>();
        List<String> log = state.getMessageLog();
        int first = Math.max(0, log.size() - 10);
        for (int i = first; i < log.size(); i++) visual.addAll(wrapText(log.get(i), maxWidth));
        int start = Math.max(0, visual.size() - maxLines);
        return new ArrayList<>(visual.subList(start, visual.size()));
    }

    /** The text-forward page (AD-15 — the bottom log IS the text surface), shared by the 2.1
     *  dialogue scene and the 2.2 intro. Draws the speaker (nullable → narration, no prefix), the
     *  wrapped node text below the HUD rows, and the numbered choices at the bottom edge. A
     *  zero-option node (a terminal dialogue node, or every intro page — Decision 7) shows the
     *  {@code footer} affordance instead of choices. A null option label renders defensively
     *  (authoring contract — the controller navigates by index, never by label). */
    private void renderTextPage(DialogNode node, String footer) {
        if (node == null) return;

        fillRect(0, 0, WW, WH, new Color(0.01f, 0.015f, 0.012f, 0.58f));
        int panelX = 18, panelY = 58, panelW = WW - 36, panelH = 210;
        drawPanel(panelX, panelY, panelW, panelH, UI_PANEL_STRONG);

        String heading = node.speaker != null ? node.speaker : "NARRATION";
        drawHeading(heading, panelX + 10, panelY + panelH - 10,
                node.speaker != null ? new Color(0.64f, 0.82f, 0.67f, 1f) : UI_MUTED);
        fillRect(panelX + 10, panelY + panelH - 17, panelW - 20, 1, UI_BORDER);

        List<String> choiceLines = new ArrayList<>();
        for (int i = 0; i < node.options.length; i++) {
            String label = node.options[i].label != null ? node.options[i].label : "(...)";
            List<String> wrapped = wrapText((i + 1) + ". " + label, panelW - 24);
            for (int line = 0; line < wrapped.size(); line++) {
                choiceLines.add(line == 0 ? wrapped.get(line) : "   " + wrapped.get(line));
            }
        }

        int choiceTop = panelY + 28 + Math.max(0, choiceLines.size() - 1) * UI_LINE;
        int bodyBottom = choiceLines.isEmpty() ? panelY + 28 : choiceTop + 14;
        int bodyY = panelY + panelH - 27;
        String text = node.text != null ? node.text : "";
        for (String line : wrapText(text, panelW - 20)) {
            if (bodyY < bodyBottom) break;
            drawText(line, panelX + 10, bodyY, new Color(0.91f, 0.86f, 0.69f, 1f));
            bodyY -= UI_LINE;
        }

        int cy = choiceTop;
        for (String line : choiceLines) {
            drawText(line, panelX + 10, cy, UI_TEXT);
            cy -= UI_LINE;
        }

        if (node.options.length == 0) {
            font.setColor(UI_MUTED);
            font.draw(batch, footer.toUpperCase(), panelX + 10, panelY + 10,
                    panelW - 20, Align.right, false);
        }
    }

    /** Story 2.5 (AC-2): the passive Journal page — the quest list DERIVED from FlagStore
     *  (journal.entries(state)), rendered in the text-forward surface with a "[J] close" footer.
     *  Pure lookup: no choices, no advancement, no new chrome (NFR-3). An empty list shows the
     *  "no threads yet" line — the Journal knows nothing before the first quest starts (AC-2). */
    private void renderJournalPage() {
        fillRect(0, 0, WW, WH, new Color(0.01f, 0.015f, 0.012f, 0.58f));
        int panelX = 18, panelY = 58, panelW = WW - 36, panelH = 210;
        drawPanel(panelX, panelY, panelW, panelH, UI_PANEL_STRONG);

        drawHeading("JOURNAL", panelX + 10, panelY + panelH - 10, UI_ACCENT);
        fillRect(panelX + 10, panelY + panelH - 17, panelW - 20, 1, UI_BORDER);

        List<JournalController.JournalEntry> entries = journal.entries(state);
        int y = panelY + panelH - 30;
        if (entries.isEmpty()) {
            drawText("No threads yet.", panelX + 12, y, UI_MUTED);
        } else {
            for (JournalController.JournalEntry e : entries) {
                // The whole entry body (title + status + objective) is panel-bounded — an entry
                // whose title would fall below the footer line is not started (an overflow past the
                // panel bottom only becomes reachable once a later Epic registers more quests).
                if (y < panelY + 24) break;
                drawText(e.title(), panelX + 12, y, UI_ACCENT);
                font.setColor(UI_MUTED);
                font.draw(batch, statusLabel(e.status()), panelX + 12, y, panelW - 24, Align.right, false);
                y -= UI_LINE;
                for (String line : wrapText(e.objective(), panelW - 24)) {
                    if (y < panelY + 24) break;
                    drawText(line, panelX + 16, y, UI_TEXT);
                    y -= UI_LINE;
                }
                y -= UI_LINE; // gap between entries
            }
        }

        font.setColor(UI_MUTED);
        font.draw(batch, "[J] CLOSE", panelX + 10, panelY + 10, panelW - 20, Align.right, false);
    }

    /** The Journal status label (JournalController.QuestStatus) — the passive lookup result.
     *  A switch EXPRESSION over the enum: adding a status value fails the compile here instead
     *  of silently rendering a future status as an existing one. */
    private String statusLabel(JournalController.QuestStatus s) {
        return switch (s) {
            case ACTIVE    -> "ACTIVE";
            case COMPLETED -> "COMPLETED";
            case VOIDED    -> "VOIDED";
        };
    }

    private void renderGameOverPanel() {
        fillRect(0, 0, WW, WH, new Color(0.01f, 0.01f, 0.008f, 0.66f));
        int x = 90, y = 143, w = 300, h = 72;
        drawPanel(x, y, w, h, UI_PANEL_STRONG);
        headingFont.setColor(UI_HEALTH);
        headingFont.draw(batch, "YOU FELL IN THE MARGINS", x, y + 48, w, Align.center, false);
        font.setColor(UI_TEXT);
        font.draw(batch, "[R] BEGIN AGAIN", x, y + 27, w, Align.center, false);
    }

    private void drawText(String value, float x, float y, Color color) {
        font.setColor(color);
        font.draw(batch, fontSafe(value), x, y);
    }

    private void drawHeading(String value, float x, float y, Color color) {
        headingFont.setColor(color);
        headingFont.draw(batch, value, x, y);
    }

    private void drawPanel(float x, float y, float width, float height, Color fill) {
        fillRect(x, y, width, height, fill);
        strokeRect(x, y, width, height, UI_BORDER);
    }

    private void drawBar(float x, float y, float width, float height, float ratio, Color fill) {
        fillRect(x, y, width, height, UI_SLOT);
        float clamped = Math.max(0f, Math.min(1f, ratio));
        if (clamped > 0f) fillRect(x, y, Math.max(1f, width * clamped), height, fill);
        strokeRect(x, y, width, height, UI_BORDER);
    }

    private void fillRect(float x, float y, float width, float height, Color color) {
        batch.setColor(color);
        batch.draw(uiPixel, x, y, width, height);
        batch.setColor(Color.WHITE);
    }

    private void strokeRect(float x, float y, float width, float height, Color color) {
        fillRect(x, y, width, 1, color);
        fillRect(x, y + height - 1, width, 1, color);
        fillRect(x, y, 1, height, color);
        fillRect(x + width - 1, y, 1, height, color);
    }

    /** Ellipsize a single HUD label rather than allowing it to invade a neighboring panel. */
    private String fitText(String value, float maxWidth) {
        value = fontSafe(value);
        if (new GlyphLayout(font, value).width <= maxWidth) return value;
        String suffix = "...";
        int end = value.length();
        while (end > 0 && new GlyphLayout(font, value.substring(0, end) + suffix).width > maxWidth) {
            end--;
        }
        return end == 0 ? suffix : value.substring(0, end).stripTrailing() + suffix;
    }

    /** Word-wrap {@code text} to fit {@code maxWidthPx} at the current font (GlyphLayout measure). */
    private List<String> wrapText(String text, int maxWidthPx) {
        List<String> lines = new ArrayList<>();
        text = fontSafe(text);
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

    /** Keep authored punctuation readable within m5x7's compact ASCII glyph set. */
    private String fontSafe(String value) {
        if (value == null) return "";
        return value
                .replace("\u2014", "-")
                .replace("\u2013", "-")
                .replace("\u2026", "...")
                .replace("\u2018", "'")
                .replace("\u2019", "'")
                .replace("\u201c", "\"")
                .replace("\u201d", "\"")
                .replace("\u2192", "->")
                .replace("\u2190", "<-")
                .replace("\u00d7", "x");
    }

    @Override public void resize(int w, int h) { viewport.update(w, h); }
    @Override public void show() {}
    @Override public void hide() {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void dispose() {
        batch.dispose();
        font.dispose();
        headingFont.dispose();
        uiPixel.dispose();
        pixels.dispose();
    }
}

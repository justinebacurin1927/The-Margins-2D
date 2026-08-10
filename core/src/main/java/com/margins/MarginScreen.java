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
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.margins.dialog.DialogNode;
import com.margins.rogue.Companion;
import com.margins.rogue.Detection;
import com.margins.rogue.RogueEnemy;
import com.margins.rogue.RoguePlayer;
import com.margins.rogue.RogueTile;
import com.margins.rogue.RogueTileMap;
import com.margins.rogue.Weather;
import com.margins.rogue.item.FloorItem;
import com.margins.rogue.item.Inventory;
import com.margins.rogue.item.Supply;
import com.margins.rogue.narrative.CaptureController;
import com.margins.rogue.narrative.CorneoIntro;
import com.margins.rogue.narrative.DialogController;
import com.margins.rogue.narrative.IntroController;
import com.margins.rogue.narrative.JournalController;
import com.margins.rogue.narrative.TutorialController;
import com.margins.rogue.save.SaveService;
import com.margins.rogue.state.RunState;
import com.margins.rogue.system.FovSystem;
import com.margins.rogue.system.PlayerAction;
import com.margins.rogue.system.TorchSystem;
import com.margins.rogue.system.TurnEngine;
import com.margins.rogue.system.TurnResult;
import com.margins.rogue.world.WorldSpine;

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
    private static final int PACK_PANEL_W = 210;
    private static final int INVENTORY_PANEL_W = 410;
    private static final int INVENTORY_PANEL_H = 260;
    private static final int INVENTORY_COLS = 4;
    private static final int INVENTORY_CELL = 46;
    private static final int INVENTORY_GAP = 5;
    private static final int LOG_PANEL_Y = 6;
    private static final int LOG_PANEL_H = 50;
    private static final int LOG_PANEL_W = 236;
    private static final float MOVE_DURATION = 0.26f;
    private static final float ATTACK_DURATION = 0.45f;
    private static final int OLD_HOUSE_ATLAS_COLS = 15;
    private static final int OLD_HOUSE_ATLAS_ROWS = 10;
    private static final int OLD_HOUSE_APRON = 1;
    private static final float STRUCTURE_ACTOR_DROP = 1.5f;
    private static final Color STRUCTURE_ACTOR_SHADOW = new Color(0.015f, 0.02f, 0.015f, 0.34f);

    private static final Color UI_PANEL = new Color(0.025f, 0.040f, 0.034f, 0.90f);
    private static final Color UI_PANEL_STRONG = new Color(0.018f, 0.028f, 0.025f, 0.97f);
    private static final Color UI_LOG_SHADOW = new Color(0.005f, 0.008f, 0.006f, 0.92f);
    private static final Color UI_SLOT = new Color(0.08f, 0.11f, 0.09f, 0.94f);
    private static final Color UI_BORDER = new Color(0.25f, 0.34f, 0.27f, 1f);
    private static final Color UI_ACCENT = new Color(0.78f, 0.68f, 0.38f, 1f);
    private static final Color UI_TEXT = new Color(0.88f, 0.90f, 0.82f, 1f);
    private static final Color UI_MUTED = new Color(0.48f, 0.55f, 0.49f, 1f);
    private static final Color UI_HEALTH = new Color(0.90f, 0.34f, 0.28f, 1f);
    private static final Color UI_FOOD = new Color(0.73f, 0.67f, 0.36f, 1f);
    private static final Color UI_WATER = new Color(0.40f, 0.68f, 0.76f, 1f);
    private static final Color UI_WARNING = new Color(0.92f, 0.55f, 0.28f, 1f);
    // Warm backpack palette: red-brown leather, copper trim, parchment text and gold selection.
    // Kept inventory-specific so the survival HUD retains its dark forest identity.
    private static final Color INV_OVERLAY = new Color(0.035f, 0.020f, 0.010f, 0.78f);
    private static final Color INV_OUTLINE = new Color(0.075f, 0.035f, 0.015f, 1f);
    private static final Color INV_PANEL = new Color(0.145f, 0.080f, 0.040f, 0.98f);
    private static final Color INV_HEADER = new Color(0.205f, 0.115f, 0.055f, 1f);
    private static final Color INV_TRIM = new Color(0.43f, 0.275f, 0.145f, 1f);
    private static final Color INV_HIGHLIGHT = new Color(0.63f, 0.405f, 0.205f, 1f);
    private static final Color INV_SLOT = new Color(0.195f, 0.105f, 0.050f, 1f);
    private static final Color INV_SLOT_SELECTED = new Color(0.34f, 0.205f, 0.070f, 1f);
    private static final Color INV_GOLD = new Color(1f, 0.74f, 0.22f, 1f);
    private static final Color INV_TEXT = new Color(0.96f, 0.88f, 0.73f, 1f);
    private static final Color INV_MUTED = new Color(0.70f, 0.54f, 0.35f, 1f);
    private static final Color INV_WARNING = new Color(1f, 0.43f, 0.11f, 1f);
    private static final Color ENEMY_BAR_BORDER = new Color(0.06f, 0.035f, 0.03f, 1f);
    private static final Color ENEMY_BAR_EMPTY = new Color(0.22f, 0.07f, 0.06f, 1f);
    private static final Color ENEMY_BAR_HEALTH = new Color(0.88f, 0.20f, 0.16f, 1f);
    private static final Color TORCH_AMBER = new Color(1f, 0.62f, 0.20f, 1f);
    private static final Color NIGHT_SHADE = new Color(0.018f, 0.030f, 0.070f, 1f);
    /** The meat states share one accurate meat silhouette; condition is shown with an earthy tint. */
    private static final Color HALF_ROTTEN_TINT = new Color(0.72f, 0.62f, 0.39f, 1f);
    private static final Color SPOILED_TINT = new Color(0.48f, 0.49f, 0.25f, 1f);

    private static final int TORCH_ITEM_ICON = 28;
    private static final int CAMPFIRE_ENV_TILE = 54;
    private static final int EFFECT_FLAME_A = 0;
    private static final int EFFECT_FLAME_B = 1;
    private static final int EFFECT_GLOW = 2;
    private static final int FOG_FRAMES = 4;

    private final SpriteBatch batch = new SpriteBatch();
    private final BitmapFont font;
    private final BitmapFont headingFont;
    private final Texture uiPixel;
    private final OrthographicCamera camera = new OrthographicCamera();
    /** Keep the 360px-tall pixel scale, but reveal more world on widescreen displays. */
    private final ExtendViewport viewport = new ExtendViewport(WW, WH, camera);
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
    /** Free-running presentation clock: visual flicker never consumes a turn or mutates RunState. */
    private float lightingClock;

    private RunState state;
    private final TurnEngine turnEngine = new TurnEngine();
    /** Story 2.1: the open dialogue scene, if any. Transient view-session state (NOT on
     *  RunState — AD-6); while {@code isActive()} the turn loop is suspended (AD-14). */
    private DialogController dialog = new DialogController();
    /** Story 2.2: the Act 0 paged-text intro, opened once on a fresh run (below). Transient
     *  view-session state (NOT on RunState — AD-6); takes no RunState, so it cannot tick (AD-14). */
    private IntroController intro = new IntroController();
    /** Story 2.3: Aldric's diegetic tutorial — a passive coach that observes committed turns and
     *  coaches the six opening controls into the log. Transient view-session state (NOT on RunState);
     *  it never suspends the turn loop. Begins when the intro closes on a fresh run; skipped on restart. */
    private TutorialController tutorial = new TutorialController();
    /** Story 2.4: Aldric's capture — a one-shot scripted event that resolves the moment the
     *  tutorial completes. Transient view-session state (NOT on RunState — AD-6); the resolving
     *  wire is the every-frame gate in handleInput (guarded, fires once per life). */
    private CaptureController capture = new CaptureController();
    /** Story 2.5: the passive Journal — the quest registry + lookup surface. Transient view-session
     *  state (NOT on RunState — AD-6); while {@code isActive()} the turn loop is suspended (AD-14 —
     *  the quest log is a suspended text surface). Renders {@code journal.entries(state)} (AC-2). */
    private JournalController journal = new JournalController();

    private enum MenuPage { ROOT, OPTIONS, HOW_TO_PLAY }

    private boolean gameOver = false;
    /** Startup/title surface; the world remains its animated in-game backdrop. */
    private boolean startupMenuOpen = true;
    /** True when Continue points at either a loaded save or the current in-memory journey. */
    private boolean hasContinue;
    /** Pause/help surface. It is presentation-only and never advances the turn engine. */
    private boolean menuOpen = false;
    private MenuPage menuPage = MenuPage.ROOT;
    private int menuSelection;
    private int startupSelection;
    /** Full backpack surface. Modal presentation state: while open, the turn loop is paused. */
    private boolean inventoryOpen = false;
    /** Clicked condition card; hover temporarily takes precedence. Presentation-only, never a turn. */
    private String pinnedStatusKey;
    /** Selected backpack slot (0..7), -1 = none yet. Screen state only — reset on restart (Task 5). */
    private int selectedSlot = -1;
    private static final int LOG_LINES = 4; // Vision.png-style four-line lower-left message box
    /** Single source for the death line — the log seed and the overlay must never drift (review finding). */
    private static final String GAME_OVER_LINE = "You fell in the margins.   [R] begin again";

    // Pixel Pack v2 character cells (row-major): 0 Klein, 1 Aldric, 6 Giliman foot soldier.
    private static final int PLAYER_CHARACTER = 0;
    private static final int COMPANION_CHARACTER = 1;
    private static final int ENEMY_CHARACTER = 6;
    /** Item-icon atlas cell per Supply ordinal (row-major, five rows of eight); -1 = no icon. The
     *  pack has no separate rotten-meat cells, so all meat stages use the raw-meat silhouette and
     *  {@link #itemTintFor(int)} communicates decay. Filtered/boiled water share the purified
     *  waterskin. Kept next to the enum's ordinal list so a new Supply stays in step. */
    private static final int[] ITEM_ICONS = {
        0,  // 0  WRAPPED_BUNDLE   -> bread
        8,  // 1  SEALED_WATERSKIN -> raw waterskin
        11, // 2  SMALL_TIN        -> water jar
        15, // 3  FOLDED_CLOTH     -> cloth
        31, // 4  SEALED_LETTER    -> map fragment (a paper)
        12, // 5  COAL             -> coal
        3,  // 6  RAW_MEAT         -> raw rabbit
        3,  // 7  HALF_ROTTEN_MEAT -> raw meat, decay tint applied while drawing
        3,  // 8  SPOILED_MEAT     -> raw meat, stronger decay tint applied while drawing
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

    // Dedicated 4x4 ground-material atlas. Context chooses the material family; a smoothed
    // coordinate field chooses its variation so the wilderness forms patches instead of noise.
    private static final int GROUND_GRASS_A = 0;
    private static final int GROUND_GRASS_B = 1;
    private static final int GROUND_FLOWERING_GRASS = 2;
    private static final int GROUND_DAMP_GRASS = 3;
    private static final int GROUND_LEAF_LITTER_A = 4;
    private static final int GROUND_LEAF_LITTER_B = 5;
    private static final int GROUND_ROOTS = 6;
    private static final int GROUND_GRAVEL = 7;
    private static final int GROUND_PACKED_DIRT_A = 8;
    private static final int GROUND_PACKED_DIRT_B = 9;
    private static final int GROUND_MUD = 10;
    private static final int GROUND_PUDDLED_MUD = 11;
    private static final int GROUND_COBBLESTONE = 12;
    private static final int GROUND_MOSSY_COBBLESTONE = 13;
    private static final int GROUND_BROKEN_COBBLESTONE = 14;
    private static final int GROUND_WET_COBBLESTONE = 15;
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

        RunState loaded = SaveService.load();
        state = loaded != null ? loaded : new RunState();
        hasContinue = loaded != null;
        FovSystem.compute(state);
        // A loaded journey resumes directly; only a genuinely new journey owns the Act 0 intro.
        if (loaded != null) tutorial.skip();
        else intro.start(CorneoIntro.build());
    }

    @Override
    public void render(float delta) {
        float frameDelta = Math.min(delta, 0.05f);
        lightingClock += frameDelta;
        updateAnimations(frameDelta);
        handleInput();

        RoguePlayer p = state.getPlayer();
        camera.position.set(animatedPixelX(playerMotion, p.getTileX()) + TILE / 2f,
                animatedPixelY(playerMotion, p.getTileY()) + TILE / 2f, 0);
        camera.update();

        Gdx.gl.glClearColor(0.04f, 0.05f, 0.045f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        renderWorld();
        renderLighting();
        renderHud();
    }

    private void handleInput() {
        if (startupMenuOpen) {
            handleStartupMenuInput();
            return;
        }
        RoguePlayer p = state.getPlayer();

        if (!p.isAlive()) {
            // The game-over line is seeded into the log once — the log is the text surface (AC-1);
            // the red overlay keeps it prominent. [R] restarts: fresh seeded log, selection cleared.
            if (!gameOver) {
                gameOver = true;
                state.appendMessages(List.of(GAME_OVER_LINE));
                SaveService.deleteSave();
                hasContinue = false;
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
        if (inventoryOpen) {
            handleInventoryInput(p);
            return;
        }
        if (menuOpen) {
            handlePauseMenuInput();
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
        if (down(Input.Keys.TAB)) {
            openInventory();
            return;
        }
        if (handleBurgerPointer()
                || Gdx.input.isKeyJustPressed(Input.Keys.M)
                || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            menuOpen = true;
            menuPage = MenuPage.ROOT;
            menuSelection = 0;
            return;
        }
        handleStatusPointer(p);
        if (handleBackpackPointer()) return;
        // A turn is atomic in the model but gets a fraction of a second to read on screen. Buffering
        // another command here would skip across the current walk/strike before it was presented.
        if (isWorldAnimating()) return;
        // A consumed selection (its stack was used up) resets the selection so E/K/F/V don't
        // silently no-op on a now-empty slot (edge-review finding).
        if (selectedSlot >= 0 && state.getInventory().backpackType(selectedSlot) < 0) selectedSlot = -1;

        PlayerAction action = readAction(p.getFacing());
        if (action != null) submitPlayerAction(action);
    }

    private void handleStartupMenuInput() {
        if (menuPage != MenuPage.ROOT) {
            handleMenuSubpageInput(true);
            return;
        }
        String[] labels = startupMenuLabels();
        if (down(Input.Keys.W) || down(Input.Keys.UP)) {
            startupSelection = moveMenuSelection(startupSelection, -1, labels.length);
        } else if (down(Input.Keys.S) || down(Input.Keys.DOWN)) {
            startupSelection = moveMenuSelection(startupSelection, 1, labels.length);
        }
        int hovered = menuButtonAtPointer(startupButtonX(), startupFirstButtonY(),
                230, 27, 7, labels.length);
        if (hovered >= 0) startupSelection = hovered;
        boolean clicked = hovered >= 0 && Gdx.input.isButtonJustPressed(Input.Buttons.LEFT);
        if (clicked || down(Input.Keys.ENTER) || down(Input.Keys.SPACE)) {
            activateStartupSelection(startupSelection);
        }
    }

    private void activateStartupSelection(int selection) {
        if (!hasContinue) {
            switch (selection) {
                case 0: startupMenuOpen = false; break;
                case 1: menuPage = MenuPage.HOW_TO_PLAY; menuSelection = 0; break;
                case 2: menuPage = MenuPage.OPTIONS; menuSelection = 0; break;
                default: Gdx.app.exit(); break;
            }
            return;
        }
        switch (selection) {
            case 0: startupMenuOpen = false; break;
            case 1: startNewJourney(); break;
            case 2: menuPage = MenuPage.HOW_TO_PLAY; menuSelection = 0; break;
            case 3: menuPage = MenuPage.OPTIONS; menuSelection = 0; break;
            default: Gdx.app.exit(); break;
        }
    }

    private void handlePauseMenuInput() {
        if (menuPage != MenuPage.ROOT) {
            handleMenuSubpageInput(false);
            return;
        }
        if (down(Input.Keys.M) || down(Input.Keys.ESCAPE)) {
            menuOpen = false;
            return;
        }
        if (down(Input.Keys.W) || down(Input.Keys.UP)) {
            menuSelection = moveMenuSelection(menuSelection, -1, 4);
        } else if (down(Input.Keys.S) || down(Input.Keys.DOWN)) {
            menuSelection = moveMenuSelection(menuSelection, 1, 4);
        }
        int hovered = menuButtonAtPointer(pauseButtonX(), pauseFirstButtonY(), 210, 32, 8, 4);
        if (hovered >= 0) menuSelection = hovered;
        boolean clicked = hovered >= 0 && Gdx.input.isButtonJustPressed(Input.Buttons.LEFT);
        if (clicked || down(Input.Keys.ENTER) || down(Input.Keys.SPACE)) {
            switch (menuSelection) {
                case 0:
                    menuOpen = false;
                    break;
                case 1:
                    menuPage = MenuPage.OPTIONS;
                    menuSelection = 0;
                    break;
                case 2:
                    menuPage = MenuPage.HOW_TO_PLAY;
                    menuSelection = 0;
                    break;
                default:
                    SaveService.save(state);
                    hasContinue = true;
                    menuOpen = false;
                    startupMenuOpen = true;
                    startupSelection = 0;
                    break;
            }
        }
    }

    private void handleMenuSubpageInput(boolean fromStartup) {
        int count = menuPage == MenuPage.OPTIONS ? 2 : 1;
        if (down(Input.Keys.ESCAPE) || down(Input.Keys.M)) {
            menuPage = MenuPage.ROOT;
            menuSelection = 0;
            return;
        }
        if (down(Input.Keys.W) || down(Input.Keys.UP)) {
            menuSelection = moveMenuSelection(menuSelection, -1, count);
        } else if (down(Input.Keys.S) || down(Input.Keys.DOWN)) {
            menuSelection = moveMenuSelection(menuSelection, 1, count);
        }
        int firstY = menuPage == MenuPage.OPTIONS ? 159 : 42;
        int hovered = menuButtonAtPointer((hudWidth() - 210) / 2, firstY, 210, 30, 9, count);
        if (hovered >= 0) menuSelection = hovered;
        boolean clicked = hovered >= 0 && Gdx.input.isButtonJustPressed(Input.Buttons.LEFT);
        if (!clicked && !down(Input.Keys.ENTER) && !down(Input.Keys.SPACE)) return;

        if (menuPage == MenuPage.OPTIONS && menuSelection == 0) {
            toggleFullscreen();
        } else {
            menuPage = MenuPage.ROOT;
            menuSelection = 0;
            if (fromStartup) startupSelection = 0;
        }
    }

    private void toggleFullscreen() {
        if (Gdx.graphics.isFullscreen()) Gdx.graphics.setWindowedMode(960, 640);
        else Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
    }

    private void startNewJourney() {
        SaveService.deleteSave();
        state = new RunState();
        dialog = new DialogController();
        intro = new IntroController();
        tutorial = new TutorialController();
        capture = new CaptureController();
        journal = new JournalController();
        intro.start(CorneoIntro.build());
        FovSystem.compute(state);
        clearAnimations();
        gameOver = false;
        hasContinue = false;
        startupMenuOpen = false;
        menuOpen = false;
        inventoryOpen = false;
        menuPage = MenuPage.ROOT;
        pinnedStatusKey = null;
        selectedSlot = -1;
    }

    private boolean handleBurgerPointer() {
        if (!Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) || !updateHudPointer()) return false;
        int x = hudWidth() - HUD_MARGIN - 30;
        int y = TOP_PANEL_Y + TOP_PANEL_H - 22;
        return pointerHud.x >= x && pointerHud.x < x + 30
                && pointerHud.y >= y && pointerHud.y < y + 20;
    }

    private int menuButtonAtPointer(int x, int firstY, int width, int height, int gap, int count) {
        if (!updateHudPointer()) return -1;
        for (int i = 0; i < count; i++) {
            int y = firstY - i * (height + gap);
            if (pointerHud.x >= x && pointerHud.x < x + width
                    && pointerHud.y >= y && pointerHud.y < y + height) return i;
        }
        return -1;
    }

    static int moveMenuSelection(int current, int delta, int count) {
        return count <= 0 ? 0 : Math.floorMod(current + delta, count);
    }

    private String[] startupMenuLabels() {
        return hasContinue
                ? new String[] {"CONTINUE JOURNEY", "NEW JOURNEY", "HOW TO PLAY", "OPTIONS", "EXIT"}
                : new String[] {"ENTER THE MARGINS", "HOW TO PLAY", "OPTIONS", "EXIT"};
    }

    private int startupButtonX() { return (hudWidth() - 230) / 2; }
    private int startupFirstButtonY() { return 168; }
    private int pauseButtonX() { return (hudWidth() - 210) / 2; }
    private int pauseFirstButtonY() { return 224; }

    /** Submit from either world controls or the backpack without duplicating story observation. */
    private void submitPlayerAction(PlayerAction action) {
        // Story 2.3 review fix (H1): the coach observes only COMMITTED turns. advanceAnimated
        // returns whether the engine actually advanced the clock (refused actions — a wall bump,
        // an empty collect, a craft without materials — commit no turn and must not be
        // acknowledged as a performed control).
        boolean committed = advanceAnimated(action);
        if (committed) tutorial.onAction(action, state);
        // Story 2.4: the capture resolves the acted turn the tutorial completes (the guard
        // makes it a safe every-frame call; it fires once and never again). If the party were
        // somehow already empty, resolve() no-ops — nothing to capture.
        if (tutorial.isComplete()) capture.resolve(state);
        if (committed && state.getPlayer().isAlive()) {
            SaveService.save(state);
            hasContinue = true;
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
        inventoryOpen = false;
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
        if (down(Input.Keys.E) && canUseFromInventory(s))
            return PlayerAction.use(s.ordinal(), facing);
        // Brackets keep quick-selection available without opening the full TAB backpack.
        if (down(Input.Keys.RIGHT_BRACKET)) return cycleSelection(1);
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

    private void openInventory() {
        Inventory inv = state.getInventory();
        if (selectedSlot < 0 || selectedSlot >= Inventory.BACKPACK_STACKS) {
            selectedSlot = inv.nextOccupiedStack(-1);
        }
        if (selectedSlot < 0) selectedSlot = 0; // an empty pack still has a navigable first slot
        inventoryOpen = true;
        pinnedStatusKey = null;
    }

    private void closeInventory() {
        inventoryOpen = false;
        if (selectedSlot >= 0 && state.getInventory().backpackType(selectedSlot) < 0) {
            selectedSlot = -1;
        }
    }

    /** Safe-pause backpack controls. An item action closes the panel before resolving its turn. */
    private void handleInventoryInput(RoguePlayer player) {
        if (down(Input.Keys.TAB) || down(Input.Keys.ESCAPE)) {
            closeInventory();
            return;
        }

        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            int clicked = inventorySlotAtPointer();
            if (clicked >= 0) selectedSlot = clicked;
        }

        int next = selectedSlot < 0 ? 0 : selectedSlot;
        if (down(Input.Keys.A) || down(Input.Keys.LEFT)) {
            next = moveInventoryCursor(next, -1, 0);
        } else if (down(Input.Keys.D) || down(Input.Keys.RIGHT)) {
            next = moveInventoryCursor(next, 1, 0);
        } else if (down(Input.Keys.W) || down(Input.Keys.UP)) {
            next = moveInventoryCursor(next, 0, -1);
        } else if (down(Input.Keys.S) || down(Input.Keys.DOWN)) {
            next = moveInventoryCursor(next, 0, 1);
        }
        selectedSlot = next;

        PlayerAction action = null;
        int type = selectedType();
        Supply supply = Supply.byOrdinal(type);
        if (down(Input.Keys.T)) {
            action = PlayerAction.craftTorch(player.getFacing());
        } else if (type >= 0 && down(Input.Keys.X)) {
            action = PlayerAction.drop(type, player.getFacing());
        } else if (canUseFromInventory(supply) && down(Input.Keys.E)) {
            action = PlayerAction.use(type, player.getFacing());
        } else if (supply != null && supply.cooksTo() != null && down(Input.Keys.K)) {
            action = PlayerAction.cook(type, player.getFacing());
        } else if (supply != null && supply.filtersTo() != null && down(Input.Keys.F)) {
            action = PlayerAction.filter(type, player.getFacing());
        } else if (supply != null && supply.boilsTo() != null && down(Input.Keys.V)) {
            action = PlayerAction.boil(type, player.getFacing());
        }

        if (action != null) {
            inventoryOpen = false;
            int actedSlot = selectedSlot;
            submitPlayerAction(action);
            if (actedSlot >= 0 && state.getInventory().backpackType(actedSlot) < 0) {
                selectedSlot = state.getInventory().nextOccupiedStack(actedSlot);
            }
        }
    }

    /** Mystery consumables, provisions and the Torn Page all have a meaningful E action. */
    static boolean canUseFromInventory(Supply supply) {
        return supply != null && (supply.isProvision() || supply.isConsumedOnUse()
                || supply == Supply.TORN_PAGE);
    }

    /** Four-by-two cursor movement with SPD-like edge wrapping. Row zero is the visual top row. */
    static int moveInventoryCursor(int slot, int colDelta, int rowDelta) {
        int clamped = Math.max(0, Math.min(Inventory.BACKPACK_STACKS - 1, slot));
        int col = Math.floorMod(clamped % INVENTORY_COLS + colDelta, INVENTORY_COLS);
        int rows = Inventory.BACKPACK_STACKS / INVENTORY_COLS;
        int row = Math.floorMod(clamped / INVENTORY_COLS + rowDelta, rows);
        return row * INVENTORY_COLS + col;
    }

    /** Click a quickbar slot to open the full backpack with that stack focused. */
    private boolean handleBackpackPointer() {
        if (!Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) return false;
        int slot = quickbarSlotAtPointer();
        if (slot < 0) return false;
        selectedSlot = slot;
        openInventory();
        return true;
    }

    private int quickbarSlotAtPointer() {
        if (!updateHudPointer()) return -1;
        int panelX = hudWidth() - HUD_MARGIN - PACK_PANEL_W;
        int slotX = panelX + 8;
        int slotY = LOG_PANEL_Y + 5;
        int stride = 24;
        if (pointerHud.x < slotX || pointerHud.y < slotY || pointerHud.y >= slotY + 22) return -1;
        int slot = (int) ((pointerHud.x - slotX) / stride);
        if (slot < 0 || slot >= Inventory.BACKPACK_STACKS) return -1;
        return pointerHud.x - (slotX + slot * stride) < 22 ? slot : -1;
    }

    private int inventorySlotAtPointer() {
        if (!updateHudPointer()) return -1;
        int panelX = (hudWidth() - INVENTORY_PANEL_W) / 2;
        int panelY = (hudHeight() - INVENTORY_PANEL_H) / 2;
        int gridX = panelX + 14;
        int gridY = panelY + 99;
        for (int slot = 0; slot < Inventory.BACKPACK_STACKS; slot++) {
            int row = slot / INVENTORY_COLS;
            int col = slot % INVENTORY_COLS;
            int sx = gridX + col * (INVENTORY_CELL + INVENTORY_GAP);
            int sy = gridY + (1 - row) * (INVENTORY_CELL + INVENTORY_GAP);
            if (pointerHud.x >= sx && pointerHud.x < sx + INVENTORY_CELL
                    && pointerHud.y >= sy && pointerHud.y < sy + INVENTORY_CELL) return slot;
        }
        return -1;
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
        int cols = (int) Math.ceil(viewport.getWorldWidth() / TILE / 2f) + 2;
        int rows = (int) Math.ceil(viewport.getWorldHeight() / TILE / 2f) + 2;

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
                    TextureRegion structure;
                    switch (map.getStructureType(x, y)) {
                        case RogueTileMap.STRUCTURE_GRAVEYARD:
                            structure = pixels.graveyard(structureCell);
                            break;
                        case RogueTileMap.STRUCTURE_DEEP_CAVE:
                            structure = pixels.deepCave(structureCell);
                            break;
                        case RogueTileMap.STRUCTURE_HUNTERS_BLIND:
                            structure = pixels.huntersBlind(structureCell);
                            break;
                        case RogueTileMap.STRUCTURE_FALLEN_LOG_HOLLOW:
                            structure = pixels.fallenLogHollow(structureCell);
                            break;
                        case RogueTileMap.STRUCTURE_FOREST_SHRINE:
                            structure = pixels.forestShrine(structureCell);
                            break;
                        case RogueTileMap.STRUCTURE_BEEHIVE_GROVE:
                            structure = pixels.beehiveGrove(structureCell);
                            break;
                        case RogueTileMap.STRUCTURE_KITCHEN_CAMP:
                            structure = pixels.kitchenCamp(structureCell);
                            break;
                        case RogueTileMap.STRUCTURE_COLLAPSED_WATCHTOWER:
                            structure = pixels.collapsedWatchtower(structureCell);
                            break;
                        case RogueTileMap.STRUCTURE_POACHERS_CAMP:
                            structure = pixels.poachersCamp(structureCell);
                            break;
                        case RogueTileMap.STRUCTURE_SUNKEN_WELL:
                            structure = pixels.sunkenWell(structureCell);
                            break;
                        default:
                            structure = pixels.oldHouse(structureCell);
                    }
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

        // A built campfire is a world object, not only invisible state. Draw its base beneath
        // actors; the bright animated flame is redrawn by renderLighting after the night shade.
        if (state.hasCampfire()) {
            int fireX = state.getCampfireX(), fireY = state.getCampfireY();
            boolean visible = map.isVisible(fireX, fireY);
            boolean explored = map.isExplored(fireX, fireY);
            if (visible || explored) {
                batch.setColor(visible ? Color.WHITE : DIM_TILE);
                drawSprite(pixels.tile(CAMPFIRE_ENV_TILE), fireX * TILE, fireY * TILE, 16, 16);
                batch.setColor(Color.WHITE);
            }
        }

        // Floor items (native 16px icons), enemies, companion, Klein.
        for (FloorItem it : state.getFloorItems()) {
            if (map.isVisible(it.x, it.y)) {
                // Review H2: guard an unknown icon (a Supply appended without an ITEM_ICONS entry)
                // so it never reaches pixels.item(-1); an icon-less item just isn't drawn.
                int icon = iconFor(it.type);
                if (icon >= 0) {
                    batch.setColor(itemTintFor(it.type));
                    drawSprite(pixels.item(icon), it.x * TILE, it.y * TILE, ITEM_ICON, ITEM_ICON);
                    batch.setColor(Color.WHITE);
                }
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

    /**
     * World-space atmosphere between the scene and HUD. Darkness is a calm blue-black wash;
     * active flames are restored afterward with additive amber light. The model still owns FOV,
     * fuel and detection—this pass is presentation only and never changes what a tile means.
     */
    private void renderLighting() {
        RogueTileMap map = state.getTileMap();
        float darkness = weatherDarkness(state.isDay(), state.getWeather());

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        if (darkness > 0f) {
            fillRect(camera.position.x - viewport.getWorldWidth() / 2f,
                    camera.position.y - viewport.getWorldHeight() / 2f,
                    viewport.getWorldWidth(), viewport.getWorldHeight(),
                    new Color(NIGHT_SHADE.r, NIGHT_SHADE.g, NIGHT_SHADE.b, darkness));
        }

        // Additive radial light lifts the already-shaded world without drawing an opaque disc.
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        if (state.hasCampfire() && map.isVisible(state.getCampfireX(), state.getCampfireY())) {
            drawLightGlow(state.getCampfireX() * TILE + TILE / 2f,
                    state.getCampfireY() * TILE + TILE / 2f, torchGlowAlpha(darkness) * 0.88f);
        }
        if (state.getTorchTurns() > 0) {
            float playerX = animatedPixelX(playerMotion, state.getPlayer().getTileX());
            float playerY = animatedPixelY(playerMotion, state.getPlayer().getTileY());
            drawLightGlow(playerX + TILE / 2f, playerY + TILE / 2f, torchGlowAlpha(darkness));
        } else if (state.hasLight() && !state.hasCampfire()
                && map.isVisible(state.getLightX(), state.getLightY())) {
            drawLightGlow(state.getLightX() * TILE + TILE / 2f,
                    state.getLightY() * TILE + TILE / 2f, torchGlowAlpha(darkness) * 0.82f);
        }
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // Flame sprites stay crisp and fully readable above the shade and radial glow.
        int flame = flameFrame();
        if (state.hasCampfire() && map.isVisible(state.getCampfireX(), state.getCampfireY())) {
            batch.draw(pixels.effect(flame), state.getCampfireX() * TILE + 5f,
                    state.getCampfireY() * TILE + 7f, 14f, 14f);
        }
        if (state.getTorchTurns() > 0) {
            drawCarriedTorch(animatedPixelX(playerMotion, state.getPlayer().getTileX()),
                    animatedPixelY(playerMotion, state.getPlayer().getTileY()),
                    state.getPlayer().getFacing(), flame);
        }
        renderWeatherOverlay(state.getWeather());
        batch.setColor(Color.WHITE);
        batch.end();
    }

    /** Clear day has no wash; Fog and Night stack but remain playable rather than pitch black. */
    static float ambientDarkness(boolean day, boolean fog) {
        float darkness = day ? 0f : 0.36f;
        if (fog) darkness += day ? 0.16f : 0.14f;
        return Math.min(0.50f, darkness);
    }

    /** Wet skies add a restrained cool wash; Fog keeps its stronger visibility treatment. */
    static float weatherDarkness(boolean day, Weather weather) {
        float darkness = ambientDarkness(day, weather == Weather.FOG);
        if (weather == Weather.RAIN) darkness += day ? 0.045f : 0.025f;
        if (weather == Weather.STORM) darkness += day ? 0.10f : 0.06f;
        if (weather == Weather.COLD_SNAP) darkness += day ? 0.025f : 0.015f;
        return Math.min(0.50f, darkness);
    }

    /** Presentation intensity only—weather consequences remain owned by the turn systems. */
    static float rainIntensity(Weather weather) {
        if (weather == Weather.RAIN) return 0.44f;
        if (weather == Weather.STORM) return 0.70f;
        return 0f;
    }

    static int loopingWeatherFrame(float seconds, float frameSeconds, int frames) {
        if (frameSeconds <= 0f || frames <= 0) return 0;
        return Math.floorMod((int) Math.floor(seconds / frameSeconds), frames);
    }

    /** Two short, low-alpha pulses in a long cycle; bright enough to read, never a whiteout. */
    static float stormFlashAlpha(float seconds) {
        float phase = positiveModulo(seconds, 7.4f);
        if (phase < 0.07f) return 0.15f * (1f - phase / 0.07f);
        if (phase >= 0.16f && phase < 0.24f) {
            return 0.08f * (1f - (phase - 0.16f) / 0.08f);
        }
        return 0f;
    }

    private static float positiveModulo(float value, float divisor) {
        return value - (float) Math.floor(value / divisor) * divisor;
    }

    /** Animated world-space precipitation and mist; it never touches RunState or advances a turn. */
    private void renderWeatherOverlay(Weather weather) {
        switch (weather) {
            case RAIN:
                drawRain();
                break;
            case STORM:
                drawStormFlash();
                drawStormRain();
                break;
            case FOG:
                drawFogSpecks();
                break;
            case COLD_SNAP:
                drawColdSnapFrost();
                break;
            default:
                break;
        }
    }

    private void drawRain() {
        float left = camera.position.x - viewport.getWorldWidth() / 2f;
        float bottom = camera.position.y - viewport.getWorldHeight() / 2f;
        float right = left + viewport.getWorldWidth();
        float top = bottom + viewport.getWorldHeight();
        // Individual streaks replace the old repeated 48px rain cells. Stable hash jitter and
        // density gaps keep this continuous field from exposing a square atlas footprint.
        float spacingX = 16f;
        float spacingY = 22f;
        float driftX = lightingClock * 19f;
        float driftY = -lightingClock * 76f;
        int firstCol = (int) Math.floor((left - driftX) / spacingX) - 1;
        int lastCol = (int) Math.ceil((right - driftX) / spacingX) + 1;
        int firstRow = (int) Math.floor((bottom - driftY) / spacingY) - 1;
        int lastRow = (int) Math.ceil((top - driftY) / spacingY) + 1;
        float baseAlpha = rainIntensity(Weather.RAIN);

        for (int row = firstRow; row <= lastRow; row++) {
            for (int col = firstCol; col <= lastCol; col++) {
                int hash = positionHash(col * 17, row * 31, 0x7f4a7c15);
                if (Math.floorMod(hash >>> 18, 100) < 42) continue;
                float jitterX = Math.floorMod(hash, 11) - 5f;
                float jitterY = Math.floorMod(hash >>> 7, 15) - 7f;
                float alpha = baseAlpha * (0.55f + Math.floorMod(hash >>> 14, 31) / 100f);
                int segments = 2 + Math.floorMod(hash >>> 23, 2);
                int segmentHeight = 2 + Math.floorMod(hash >>> 27, 2);
                batch.setColor(0.76f, 0.86f, 0.94f, alpha);
                drawRainStreak(col * spacingX + driftX + jitterX,
                        row * spacingY + driftY + jitterY, segments, segmentHeight);
            }
        }
        batch.setColor(Color.WHITE);
    }

    /**
     * Heavy rain without a lattice. Each numbered drop receives an unrelated hashed position,
     * speed, lifetime, length and wind response; cycling those independent particles through the
     * viewport cannot produce the diagonal rows that a col/row weather grid exposes.
     */
    private void drawStormRain() {
        float left = camera.position.x - viewport.getWorldWidth() / 2f;
        float bottom = camera.position.y - viewport.getWorldHeight() / 2f;
        float width = viewport.getWorldWidth();
        float height = viewport.getWorldHeight();
        float travel = height + 96f;
        float fieldWidth = width + 120f;
        int drops = Math.max(150, Math.min(320, (int) Math.ceil(width * height / 900f)));

        for (int index = 0; index < drops; index++) {
            int hash = positionHash(index * 73, index * index + 17, 0x7f4a7c15);
            int motionHash = positionHash(index * 31 + 11, index * 97, 0x2c1b3c6d);
            float phase = stormDropPhase(lightingClock, hash, travel);
            float baseX = Math.floorMod(hash, 10000) / 10000f * fieldWidth;
            float wind = 18f + Math.floorMod(motionHash >>> 9, 31);
            float flutter = (float) Math.sin(lightingClock
                    * (0.75f + Math.floorMod(motionHash >>> 20, 40) / 100f)
                    + Math.floorMod(hash >>> 8, 628) / 100f)
                    * (2f + Math.floorMod(hash >>> 21, 6));
            float x = left - 60f + positiveModulo(baseX + phase * wind + flutter, fieldWidth);
            float y = bottom - 36f + (1f - phase) * travel;
            float gust = 0.72f + 0.28f * (float) Math.sin(
                    lightingClock * 1.3f + Math.floorMod(motionHash, 628) / 100f);
            float alpha = rainIntensity(Weather.STORM)
                    * (0.62f + Math.floorMod(hash >>> 14, 27) / 100f) * gust;
            int segments = 3 + Math.floorMod(hash >>> 23, 3);
            int segmentHeight = 2 + Math.floorMod(motionHash >>> 25, 3);

            batch.setColor(0.72f, 0.84f, 0.96f, alpha);
            drawRainStreak(x, y, segments, segmentHeight);
        }
        batch.setColor(Color.WHITE);
    }

    /** Independent seeded cycle for one storm drop; presentation-only and always normalized. */
    static float stormDropPhase(float seconds, int seed, float travel) {
        if (travel <= 0f) return 0f;
        float speed = 118f + Math.floorMod(seed >>> 10, 83);
        float offset = Math.floorMod(seed, 10000) / 10000f * travel;
        return positiveModulo(seconds * speed + offset, travel) / travel;
    }

    /** One crisp slash assembled from tiny pixel bars—no texture cell exists to repeat. */
    private void drawRainStreak(float x, float y, int segments, int segmentHeight) {
        for (int i = 0; i < segments; i++) {
            batch.draw(uiPixel, x + i, y + i * segmentHeight, 1f, segmentHeight + 1f);
        }
    }

    /**
     * Cell-local mist emitters inspired by the motion language of dungeon gas fields. Every
     * visible tile owns a few deterministic, short-lived wisps: each one appears at a different
     * point inside the tile, curls sideways, slowly rotates and expands, then dissolves. Rebuilding
     * the particles from the presentation clock keeps the effect smooth without mutating game
     * state, and the scattered lifetimes prevent the fog from forming rows or one giant cloud.
     */
    private void drawFogSpecks() {
        RogueTileMap map = state.getTileMap();
        float left = camera.position.x - viewport.getWorldWidth() / 2f;
        float bottom = camera.position.y - viewport.getWorldHeight() / 2f;
        float right = left + viewport.getWorldWidth();
        float top = bottom + viewport.getWorldHeight();
        int firstX = Math.max(0, (int) Math.floor(left / TILE) - 1);
        int lastX = Math.min(map.getWidth() - 1, (int) Math.ceil(right / TILE) + 1);
        int firstY = Math.max(0, (int) Math.floor(bottom / TILE) - 1);
        int lastY = Math.min(map.getHeight() - 1, (int) Math.ceil(top / TILE) + 1);

        for (int x = firstX; x <= lastX; x++) {
            for (int y = firstY; y <= lastY; y++) {
                if (!map.isVisible(x, y)) continue;
                for (int stream = 0; stream < 2; stream++) {
                    int hash = positionHash(x * 17 + stream * 53, y * 31 - stream * 19,
                            0x165667b1);
                    // Several staggered emitters per cell, with enough empty cycles for airy fog.
                    if (Math.floorMod(hash >>> 17, 100) < 35) continue;

                    float phase = fogSpeckPhase(lightingClock, hash);
                    float lifeAlpha = fogSpeckAlpha(phase);
                    float baseSize = 18f + Math.floorMod(hash >>> 21, 7);
                    float size = baseSize * fogSpeckScale(phase);
                    float anchorX = x * TILE + 2f + Math.floorMod(hash, 21);
                    float anchorY = y * TILE + 1f + Math.floorMod(hash >>> 7, 18);
                    float curl = (float) Math.sin(phase * Math.PI * 2f
                            + Math.floorMod(hash >>> 12, 628) / 100f);
                    float xDrift = curl * (2f + Math.floorMod(hash >>> 24, 5))
                            + phase * (Math.floorMod(hash >>> 28, 5) - 2f);
                    float yDrift = (float) Math.sin(phase * Math.PI
                            + Math.floorMod(hash >>> 6, 314) / 100f)
                            * (1f + Math.floorMod(hash >>> 15, 3));
                    float rotation = Math.floorMod(hash >>> 4, 17) - 8f
                            + phase * (Math.floorMod(hash >>> 26, 2) == 0 ? -10f : 10f);
                    int frame = Math.floorMod((int) (phase * FOG_FRAMES)
                            + Math.floorMod(hash, FOG_FRAMES), FOG_FRAMES);
                    // The fog atlas already has soft per-pixel alpha, so this multiplier must be
                    // materially stronger than the old opaque cloud-sheet tint to remain visible.
                    float alpha = (0.15f + Math.floorMod(hash >>> 13, 6) * 0.015f) * lifeAlpha;

                    batch.setColor(0.59f, 0.67f, 0.61f, alpha);
                    batch.draw(pixels.fog(frame), anchorX + xDrift - size / 2f,
                            anchorY + yDrift - size / 2f, size / 2f, size / 2f,
                            size, size, 1f, 1f, rotation);
                }
            }
        }
        batch.setColor(Color.WHITE);
    }

    /** A stable 1.35–2.84 second lifetime and random age offset for one procedural wisp. */
    static float fogSpeckPhase(float seconds, int seed) {
        float lifespan = 1.35f + Math.floorMod(seed >>> 8, 150) / 100f;
        float offset = Math.floorMod(seed, 1000) / 1000f * lifespan;
        return positiveModulo(seconds + offset, lifespan) / lifespan;
    }

    /** Soft fade-in/fade-out curve; the particle is invisible exactly at either end of its life. */
    static float fogSpeckAlpha(float phase) {
        float clamped = Math.max(0f, Math.min(1f, phase));
        return (float) Math.sqrt(Math.max(0f, Math.sin(clamped * Math.PI)));
    }

    /** Wisps expand as they disperse instead of translating like a flat weather sheet. */
    static float fogSpeckScale(float phase) {
        float clamped = Math.max(0f, Math.min(1f, phase));
        return 0.62f + clamped * 0.76f;
    }

    private void drawColdSnapFrost() {
        RogueTileMap map = state.getTileMap();
        float left = camera.position.x - viewport.getWorldWidth() / 2f;
        float bottom = camera.position.y - viewport.getWorldHeight() / 2f;
        float right = left + viewport.getWorldWidth();
        float top = bottom + viewport.getWorldHeight();
        int firstX = Math.max(0, (int) Math.floor(left / TILE) - 1);
        int lastX = Math.min(map.getWidth() - 1, (int) Math.ceil(right / TILE) + 1);
        int firstY = Math.max(0, (int) Math.floor(bottom / TILE) - 1);
        int lastY = Math.min(map.getHeight() - 1, (int) Math.ceil(top / TILE) + 1);

        // The blue chill is global, but every moving detail now comes from a visible map cell.
        // This follows the same localized emitter language as Fog instead of sweeping a repeated
        // weather grid across the screen.
        batch.setColor(0.48f, 0.65f, 0.86f, 0.055f);
        batch.draw(uiPixel, left, bottom, viewport.getWorldWidth(), viewport.getWorldHeight());
        for (int x = firstX; x <= lastX; x++) {
            for (int y = firstY; y <= lastY; y++) {
                if (!map.isVisible(x, y)) continue;
                for (int stream = 0; stream < 3; stream++) {
                    int hash = positionHash(x * 23 + stream * 47, y * 37 - stream * 17,
                            0x4cf5ad43);
                    if (Math.floorMod(hash >>> 18, 100) < 36) continue;

                    float phase = coldSnapParticlePhase(lightingClock, hash);
                    float lifeAlpha = coldSnapParticleAlpha(phase);
                    float anchorX = x * TILE + 2f + Math.floorMod(hash, 21);
                    float anchorY = y * TILE + 2f + Math.floorMod(hash >>> 7, 20);
                    float direction = Math.floorMod(hash >>> 24, 2) == 0 ? -1f : 1f;
                    float turns = 1.4f + Math.floorMod(hash >>> 14, 6) * 0.22f;
                    float angle = Math.floorMod(hash >>> 4, 628) / 100f
                            + phase * (float) Math.PI * 2f * turns * direction;
                    float radius = 1.5f + (float) Math.sin(phase * Math.PI)
                            * (2.5f + Math.floorMod(hash >>> 21, 4));
                    float particleX = anchorX + (float) Math.cos(angle) * radius;
                    float particleY = anchorY + (float) Math.sin(angle) * radius;
                    float alpha = (0.27f + Math.floorMod(hash >>> 12, 7) * 0.025f)
                            * lifeAlpha;

                    if (Math.floorMod(hash >>> 25, 5) == 0) {
                        // A tiny rotating vapor bloom supplies the blizzard-potion softness.
                        float size = (8f + Math.floorMod(hash >>> 20, 6))
                                * (0.72f + phase * 0.45f);
                        int frame = Math.floorMod((int) (phase * FOG_FRAMES)
                                + Math.floorMod(hash, FOG_FRAMES), FOG_FRAMES);
                        float rotation = phase * 230f * direction;
                        batch.setColor(0.62f, 0.75f, 0.90f, alpha * 0.62f);
                        batch.draw(pixels.fog(frame), particleX - size / 2f,
                                particleY - size / 2f, size / 2f, size / 2f,
                                size, size, 1f, 1f, rotation);
                    } else {
                        // Crystals tumble between horizontal and vertical silhouettes locally;
                        // nothing shares a screen-wide direction or lane.
                        int orientation = Math.floorMod((int) (phase * 8f)
                                + Math.floorMod(hash, 4), 4);
                        float length = 2f + Math.floorMod(hash >>> 22, 3);
                        batch.setColor(0.72f, 0.87f, 1f, alpha);
                        if ((orientation & 1) == 0) {
                            batch.draw(uiPixel, particleX - length / 2f, particleY, length, 1f);
                            batch.draw(uiPixel, particleX, particleY - 1f, 1f, 3f);
                        } else {
                            batch.draw(uiPixel, particleX, particleY - length / 2f, 1f, length);
                            batch.draw(uiPixel, particleX - 1f, particleY, 3f, 1f);
                        }
                    }
                }
            }
        }
        batch.setColor(Color.WHITE);
    }

    /** Stable short life and staggered birth time for a tile-local frost emitter. */
    static float coldSnapParticlePhase(float seconds, int seed) {
        float lifespan = 0.90f + Math.floorMod(seed >>> 8, 130) / 100f;
        float offset = Math.floorMod(seed, 1000) / 1000f * lifespan;
        return positiveModulo(seconds + offset, lifespan) / lifespan;
    }

    /** Frost materializes and dissolves rather than entering from one edge of the screen. */
    static float coldSnapParticleAlpha(float phase) {
        float clamped = Math.max(0f, Math.min(1f, phase));
        return (float) Math.sqrt(Math.max(0f, Math.sin(clamped * Math.PI)));
    }

    private void drawStormFlash() {
        float alpha = stormFlashAlpha(lightingClock);
        if (alpha <= 0f) return;
        float left = camera.position.x - viewport.getWorldWidth() / 2f;
        float bottom = camera.position.y - viewport.getWorldHeight() / 2f;
        batch.setColor(0.72f, 0.82f, 1f, alpha);
        batch.draw(uiPixel, left, bottom, viewport.getWorldWidth(), viewport.getWorldHeight());
        batch.setColor(Color.WHITE);
    }

    /** Barely visible by day; progressively useful as ambient darkness rises. */
    static float torchGlowAlpha(float darkness) {
        return darkness <= 0f ? 0.11f : Math.min(0.48f, 0.20f + darkness * 0.56f);
    }

    private void drawLightGlow(float centerX, float centerY, float baseAlpha) {
        float flicker = 0.94f + 0.06f * (float) Math.sin(lightingClock * 11f);
        float size = TILE * (6.8f + 0.10f * (float) Math.sin(lightingClock * 7f));
        batch.setColor(1f, 0.82f, 0.48f, baseAlpha * flicker);
        batch.draw(pixels.smoothEffect(EFFECT_GLOW), centerX - size / 2f, centerY - size / 2f,
                size, size);
        batch.setColor(Color.WHITE);
    }

    private int flameFrame() {
        return ((int) (lightingClock / 0.18f) & 1) == 0 ? EFFECT_FLAME_A : EFFECT_FLAME_B;
    }

    /** Small diagonal torch plus flame, tucked beside Klein rather than covering his silhouette. */
    private void drawCarriedTorch(float actorX, float actorY, int facing, int flame) {
        boolean left = directionX(facing) < 0;
        float iconX = actorX + (left ? -1f : 12f);
        TextureRegion icon = pixels.item(TORCH_ITEM_ICON);
        if (left) batch.draw(icon, iconX + 13f, actorY + 4f, -13f, 13f);
        else batch.draw(icon, iconX, actorY + 4f, 13f, 13f);
        batch.draw(pixels.effect(flame), actorX + (left ? -3f : 13f), actorY + 10f,
                12f, 12f);
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

    /** The world tile for (x,y); water/doors retain their authored atlas, floors use materials. */
    private TextureRegion tileRegion(RogueTileMap map, int x, int y) {
        // Multi-cell structure art supplies its own ground and collision silhouette. Keep an
        // ordinary floor under every transparent pixel so a cave's blocking rock cells never leak
        // unrelated forest-autotile crowns through the authored structure image.
        if (map.getStructureTile(x, y) >= 0) {
            return pixels.ground(grassCell(x, y));
        }
        switch (map.getTile(x, y)) {
            case RogueTile.WALL:  return pixels.forest(forestMask(map, x, y));
            case RogueTile.DOOR:  return pixels.terrain(DOOR_CELL);
            case RogueTile.WELL:  return pixels.terrain(WELL_CELL);
            case RogueTile.POND:  return pixels.terrain(POND_CELLS[variant(x, y, POND_CELLS)]);
            case RogueTile.RIVER: return pixels.terrain(RIVER_CELLS[variant(x, y, RIVER_CELLS)]);
            default:              return pixels.ground(groundMaterialCell(map, x, y));
        }
    }

    /**
     * Presentation-only material classification. The gameplay map deliberately remains simple:
     * grass, road, mud and stone are all ordinary walkable FLOOR to the turn engine. This method
     * reads durable geography instead of frame/weather state, so materials never pop between turns.
     */
    static int groundMaterialCell(RogueTileMap map, int x, int y) {
        // Wells sit in small worked-stone aprons; ponds and rivers soften the nearby earth.
        if (nearTile(map, x, y, 1, RogueTile.WELL)) {
            int field = patchField(x, y, 0x243f6a88);
            return field < 112 ? GROUND_COBBLESTONE
                    : field < 145 ? GROUND_MOSSY_COBBLESTONE : GROUND_BROKEN_COBBLESTONE;
        }
        if (nearOpenWater(map, x, y, 1)) {
            return patchField(x, y, 0x85a308d3) < 132 ? GROUND_MUD : GROUND_PUDDLED_MUD;
        }

        // The authored Copper Road is three tiles wide: a surviving cobbled center with worn,
        // dirt-and-gravel shoulders. Contextual water above can naturally break it into mud.
        int roadY = Math.round(WorldSpine.ROAD_Y * (map.getHeight() - 1));
        int roadStart = Math.round(WorldSpine.ROAD_X_START * (map.getWidth() - 1));
        int roadEnd = Math.round(WorldSpine.ROAD_X_END * (map.getWidth() - 1));
        if (x >= roadStart && x <= roadEnd && Math.abs(y - roadY) <= 1) {
            int field = patchField(x, y, 0x13198a2e);
            if (y == roadY) {
                if (field < 112) return GROUND_COBBLESTONE;
                if (field < 143) return GROUND_MOSSY_COBBLESTONE;
                return GROUND_BROKEN_COBBLESTONE;
            }
            if (field < 116) return GROUND_PACKED_DIRT_A;
            if (field < 145) return GROUND_PACKED_DIRT_B;
            return GROUND_GRAVEL;
        }

        if (nearOpenWater(map, x, y, 2)) return GROUND_DAMP_GRASS;
        if (nearTile(map, x, y, 1, RogueTile.DOOR)) {
            return patchField(x, y, 0xa4093822) < 132
                    ? GROUND_PACKED_DIRT_A : GROUND_PACKED_DIRT_B;
        }

        // A continuous leaf/root verge visually seats the treeline into the clearing. A sparse
        // second row breaks the ruler-straight edge without filling whole rooms with brown noise.
        if (nearTile(map, x, y, 1, RogueTile.WALL)) {
            int field = patchField(x, y, 0x299f31d0);
            if (field < 114) return GROUND_LEAF_LITTER_A;
            if (field < 145) return GROUND_LEAF_LITTER_B;
            return GROUND_ROOTS;
        }
        if (nearTile(map, x, y, 2, RogueTile.WALL)
                && patchField(x, y, 0x082efa98) < 112) {
            return patchField(x, y, 0xec4e6c89) < 128
                    ? GROUND_LEAF_LITTER_A : GROUND_LEAF_LITTER_B;
        }
        return grassCell(x, y);
    }

    /** Quiet grass dominates; damp and flowered cells form rare overlapping organic patches. */
    private static int grassCell(int x, int y) {
        int field = patchField(x, y, 0x452821e6);
        if (field < 108) return GROUND_DAMP_GRASS;
        if (field > 151) return GROUND_FLOWERING_GRASS;
        return patchField(x, y, 0x38d01377) < 128 ? GROUND_GRASS_A : GROUND_GRASS_B;
    }

    private static boolean nearOpenWater(RogueTileMap map, int x, int y, int radius) {
        return nearTile(map, x, y, radius, RogueTile.POND)
                || nearTile(map, x, y, radius, RogueTile.RIVER);
    }

    /** Chebyshev-radius neighborhood keeps material halos continuous around irregular silhouettes. */
    private static boolean nearTile(RogueTileMap map, int x, int y, int radius, int tile) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                if (dx == 0 && dy == 0) continue;
                if (map.getTile(x + dx, y + dy) == tile) return true;
            }
        }
        return false;
    }

    /**
     * Average overlapping 5x5 coordinate hashes into a stable low-frequency field. Neighboring
     * cells share 20/25 samples, so thresholds create lumpy patches without diagonal bands,
     * checkerboards, fixed macro-cell edges, save-state changes, or per-frame flicker.
     */
    private static int patchField(int x, int y, int salt) {
        int sum = 0;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                sum += positionHash(x + dx, y + dy, salt) & 0xff;
            }
        }
        return sum / 25;
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
    static int iconFor(int type) {
        return type >= 0 && type < ITEM_ICONS.length ? ITEM_ICONS[type] : -1;
    }

    /** Preserve the meat silhouette instead of misrepresenting rotten meat as cheese. */
    private static Color itemTintFor(int type) {
        if (type == Supply.HALF_ROTTEN_MEAT.ordinal()) return HALF_ROTTEN_TINT;
        if (type == Supply.SPOILED_MEAT.ordinal()) return SPOILED_TINT;
        return Color.WHITE;
    }

    private void drawItemIcon(int type, float x, float y, float width, float height) {
        int icon = iconFor(type);
        if (icon < 0) return;
        batch.setColor(itemTintFor(type));
        batch.draw(pixels.item(icon), x, y, width, height);
        batch.setColor(Color.WHITE);
    }

    private void renderHud() {
        RoguePlayer p = state.getPlayer();
        batch.getProjectionMatrix().setToOrtho2D(0, 0, hudWidth(), hudHeight());
        batch.begin();

        if (startupMenuOpen) {
            renderStartupMenu();
            batch.end();
            return;
        }

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
        else if (inventoryOpen) renderInventoryPanel();
        else if (menuOpen) renderMenuPanel();

        batch.end();
    }

    /** Story 3.3 (AC-2): the top-panel time readout. The day is the planning unit (UJ-2): during
     *  the day it shows the cycle/day number, the clock, and the turns-until-nightfall foray budget
     *  ("make it home before dark"); at night it shows the night clock (the budget is spent). The
     *  weather and a lit torch's burn still read. Pure string build from core state (AD-1/AD-2), no
     *  Gdx — so it's headless-testable like the other panel builders. */
    static String timeLabel(RunState state) {
        boolean torchLit = state.getTorchTurns() > 0;
        String weather = state.getWeather() == Weather.COLD_SNAP
                ? "COLD" : state.getWeather().label().toUpperCase();
        if (state.isDay()) {
            String phase = "D" + state.dayNumber() + " " + (torchLit ? "D" : "DAY ")
                    + state.getClockTurns() + " N-" + state.turnsUntilNightfall();
            return torchLit ? phase + " " + weather : phase + "  " + weather;
        }
        return (torchLit ? "N" : "NIGHT ") + state.getClockTurns() + "  " + weather;
    }

    /** Compact top-left survival panel: condition names live in hover/click cards, not the bar. */
    private void renderStatusPanel(RoguePlayer p) {
        int x = HUD_MARGIN;
        int y = TOP_PANEL_Y;
        drawPanel(x, y, STATUS_PANEL_W, TOP_PANEL_H, UI_PANEL);

        drawText("HP " + p.getHp() + "/" + p.getMaxHp(), x + 6, y + 53, UI_HEALTH);
        drawBar(x + 54, y + 47, 66, 4, p.getHp() / (float) Math.max(1, p.getMaxHp()), UI_HEALTH);
        boolean torchLit = state.getTorchTurns() > 0;
        String time = timeLabel(state);
        drawText(fitText(time, torchLit ? 82 : 116), x + 130, y + 53, UI_ACCENT);
        if (torchLit) {
            batch.setColor(Color.WHITE);
            batch.draw(pixels.item(TORCH_ITEM_ICON), x + 213, y + 40, 14, 14);
            drawBar(x + 228, y + 48, 16, 4,
                    state.getTorchTurns() / (float) TorchSystem.TORCH_BURN, TORCH_AMBER);
            drawText(state.getTorchTurns() + "T", x + 226, y + 43, TORCH_AMBER);
        }

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
        pointerHud.set((rawX - screenX) * hudWidth() / screenW,
                (rawY - screenY) * hudHeight() / screenH);
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
        batch.setColor(color);
        batch.draw(pixels.statusIcon(icon.ordinal()), x - 2, y - 2, 16, 16);
        batch.setColor(Color.WHITE);
    }

    /** Lower-right eight-slot quickbar; only the selected stack spends space on its name. */
    private void renderBackpackPanel() {
        int x = hudWidth() - HUD_MARGIN - PACK_PANEL_W;
        int y = LOG_PANEL_Y;
        drawInventoryFrame(x, y, PACK_PANEL_W, TOP_PANEL_H);

        Inventory inv = state.getInventory();
        fillRect(x + 3, y + 37, PACK_PANEL_W - 6, 21, INV_HEADER);
        drawHeading("BACKPACK", x + 7, y + 53, INV_TEXT);
        drawText(inv.backpackStackCount() + "/" + Inventory.BACKPACK_STACKS,
                x + PACK_PANEL_W - 27, y + 53, inv.isBackpackFull() ? INV_WARNING : INV_MUTED);
        String selected = "[TAB] OPEN   [ / ] QUICK SELECT";
        if (selectedSlot >= 0 && inv.backpackType(selectedSlot) >= 0) {
            int type = inv.backpackType(selectedSlot);
            selected = state.getIdentifyMap().displayNameFor(type) + " x" + inv.backpackCount(selectedSlot);
        }
        drawText(fitText(selected, PACK_PANEL_W - 12), x + 7, y + 42,
                selectedSlot >= 0 ? INV_GOLD : INV_MUTED);

        int slotSize = 22;
        int gap = 2;
        int sx = x + 8;
        int sy = y + 5;
        for (int slot = 0; slot < Inventory.BACKPACK_STACKS; slot++) {
            int type = inv.backpackType(slot);
            boolean sel = slot == selectedSlot;
            int slotX = sx + slot * (slotSize + gap);
            drawInventorySlot(slotX, sy, slotSize, slotSize, sel);
            if (type < 0) continue;
            drawItemIcon(type, slotX + 3, sy + 3, ITEM_ICON, ITEM_ICON);
            int count = inv.backpackCount(slot);
            if (count > 1) {
                drawText(Integer.toString(count), slotX + 14, sy + 9,
                        sel ? INV_GOLD : INV_TEXT);
            }
        }
    }

    /** Full SPD-style backpack: large grid left, selected-item knowledge and actions right. */
    private void renderInventoryPanel() {
        fillRect(0, 0, hudWidth(), hudHeight(), INV_OVERLAY);

        int x = (hudWidth() - INVENTORY_PANEL_W) / 2;
        int y = (hudHeight() - INVENTORY_PANEL_H) / 2;
        int top = y + INVENTORY_PANEL_H;
        drawInventoryFrame(x, y, INVENTORY_PANEL_W, INVENTORY_PANEL_H);
        fillRect(x + 4, top - 29, INVENTORY_PANEL_W - 8, 24, INV_HEADER);
        strokeRect(x + 4, top - 29, INVENTORY_PANEL_W - 8, 24, INV_TRIM);

        drawHeading("BACKPACK", x + 14, top - 14, INV_TEXT);
        Inventory inv = state.getInventory();
        String capacity = inv.backpackStackCount() + " / " + Inventory.BACKPACK_STACKS + " STACKS";
        drawText(capacity, x + INVENTORY_PANEL_W - 12 - new GlyphLayout(font, capacity).width,
                top - 14, inv.isBackpackFull() ? INV_WARNING : INV_MUTED);
        fillRect(x + 12, top - 31, INVENTORY_PANEL_W - 24, 1, INV_HIGHLIGHT);

        int gridX = x + 14;
        int gridY = y + 99;
        for (int slot = 0; slot < Inventory.BACKPACK_STACKS; slot++) {
            int row = slot / INVENTORY_COLS;
            int col = slot % INVENTORY_COLS;
            int sx = gridX + col * (INVENTORY_CELL + INVENTORY_GAP);
            int sy = gridY + (1 - row) * (INVENTORY_CELL + INVENTORY_GAP);
            boolean selected = slot == selectedSlot;
            int type = inv.backpackType(slot);

            drawInventorySlot(sx, sy, INVENTORY_CELL, INVENTORY_CELL, selected);
            drawText(Integer.toString(slot + 1), sx + 3, sy + INVENTORY_CELL - 4,
                    selected ? INV_GOLD : INV_MUTED);

            if (type < 0) continue;
            drawItemIcon(type, sx + 11, sy + 9, 26, 26);
            int count = inv.backpackCount(slot);
            if (count > 1) drawText("x" + count, sx + 28, sy + 9,
                    selected ? INV_GOLD : INV_TEXT);
        }

        int dividerX = x + 219;
        fillRect(dividerX, y + 44, 1, INVENTORY_PANEL_H - 82, INV_TRIM);
        renderInventoryDetails(dividerX + 12, y, top,
                INVENTORY_PANEL_W - (dividerX - x) - 24);

        fillRect(x + 12, y + 36, INVENTORY_PANEL_W - 24, 1, INV_TRIM);
        font.setColor(INV_GOLD);
        font.draw(batch, "WASD SELECT   E ACT   X DROP   TAB / ESC CLOSE",
                x, y + 17, INVENTORY_PANEL_W, Align.center, false);
    }

    private void renderInventoryDetails(int detailX, int panelY, int panelTop, int detailWidth) {
        Inventory inv = state.getInventory();
        int type = selectedType();
        Supply supply = Supply.byOrdinal(type);
        if (supply == null) {
            drawHeading("EMPTY SLOT", detailX, panelTop - 49, INV_MUTED);
            drawText("Nothing is stored here.", detailX, panelTop - 68, INV_MUTED);
            drawText("Select another slot or", detailX, panelTop - 82, INV_MUTED);
            drawText("pick up supplies with G.", detailX, panelTop - 92, INV_MUTED);
            return;
        }

        drawItemIcon(type, detailX, panelTop - 82, 34, 34);
        String name = state.getIdentifyMap().displayNameFor(type);
        drawText(fitText(name.toUpperCase(), detailWidth - 40), detailX + 40, panelTop - 52,
                INV_GOLD);
        drawText("x" + inv.backpackCount(selectedSlot) + "  " + inventoryCategory(type, supply),
                detailX + 40, panelTop - 67, INV_MUTED);
        fillRect(detailX, panelTop - 91, detailWidth, 1, INV_TRIM);

        List<String> description = wrapText(inventoryDescription(type, supply), detailWidth);
        int baseline = panelTop - 104;
        for (int i = 0; i < Math.min(4, description.size()); i++) {
            drawText(description.get(i), detailX, baseline, INV_TEXT);
            baseline -= UI_LINE;
        }

        // Actions follow the actual description height. A fixed Y collided with line three on
        // longer descriptions (most visibly Spoiled Meat at fullscreen scale).
        int actionHeadingY = Math.min(panelY + 135, baseline - 2);
        drawHeading("ACTIONS", detailX, actionHeadingY, INV_MUTED);
        int actionY = actionHeadingY - 16;
        String primary = inventoryPrimaryAction(supply);
        if (primary != null) {
            drawText(primary, detailX, actionY, INV_GOLD);
            actionY -= 14;
        }
        String process = inventoryProcessAction(supply);
        if (process != null) {
            drawText(process, detailX, actionY, INV_TEXT);
            actionY -= 14;
        }
        if (supply == Supply.COAL || supply == Supply.WOOD) {
            drawText("T  CRAFT TORCH", detailX, actionY,
                    state.getInventory().count(Supply.COAL.ordinal()) > 0
                            && state.getInventory().count(Supply.WOOD.ordinal()) > 0
                            ? INV_GOLD : INV_MUTED);
            actionY -= 14;
        }
        drawText("X  DROP STACK", detailX, Math.max(panelY + 49, actionY - 8), INV_WARNING);
    }

    private String inventoryCategory(int type, Supply supply) {
        if (supply.possibleIdentities().length > 1 && !state.getIdentifyMap().isIdentified(type)) {
            return "UNIDENTIFIED";
        }
        if (supply == Supply.TORN_PAGE || supply == Supply.SEALED_LETTER) return "DOCUMENT";
        if (supply.isCure()) return "MEDICINE";
        if (supply.isWater()) return "DRINK";
        if (supply.isFood() || supply.cooksTo() != null) return "FOOD";
        if (supply == Supply.COAL || supply == Supply.WOOD || supply == Supply.SALT) return "MATERIAL";
        return "SUPPLY";
    }

    private String inventoryDescription(int type, Supply supply) {
        if (supply.possibleIdentities().length > 1 && !state.getIdentifyMap().isIdentified(type)) {
            return "Its true contents are unknown. Use it to identify every supply of this type for the run.";
        }
        switch (supply) {
            case COAL:
                return "Dense fuel for torches, campfires and boiling water.";
            case WOOD:
                return "Dry crafting fuel. One Wood plus one Coal makes a carried torch.";
            case SALT:
                return "A preservative that slows the spoilage of carried food.";
            case RAW_MEAT:
                return "Restores food, but risks sickness. Cook it at a campfire when possible.";
            case HALF_ROTTEN_MEAT:
                return "Spoiling meat with reduced nourishment and a serious sickness risk.";
            case SPOILED_MEAT:
                return "Barely edible and highly dangerous. Consume only in desperation.";
            case COOKED_MEAT:
                return "Safe, filling meat prepared at a campfire.";
            case WELL_WATER: case POND_WATER: case RIVER_WATER: case FILTERED_WATER:
                return supply.drinkRisk() == 0
                        ? "Drinkable water that restores hydration."
                        : "Restores hydration with a " + supply.drinkRisk() + "% contamination risk.";
            case BOILED_WATER:
                return "Purified water with no contamination risk.";
            case TOXIC_MUSHROOM: case HONEYMOON_MUSHROOM:
                return "A dangerous mushroom carrying a known toxin. Not ordinary food.";
            case HONEY: case HONEYCOMB: case BLOODVEIN_MUSHROOM: case HERBAL_CURE:
                return "A rare treatment for illness or persistent debuffs.";
            case TORN_PAGE:
                return "A torn order tied to Aldric's capture. Read it for its surviving message.";
            case SEALED_LETTER:
                return "A sealed document Milek cannot read yet.";
            default:
                return state.getIdentifyMap().isIdentified(type)
                        ? "Its identity is known for this run. Use it when its effect is needed."
                        : "A compact survival supply recovered from the margins.";
        }
    }

    private static String inventoryPrimaryAction(Supply supply) {
        if (!canUseFromInventory(supply)) return null;
        if (supply == Supply.TORN_PAGE) return "E  READ";
        if (supply.isWater()) return "E  DRINK";
        if (supply.isFood()) return "E  EAT";
        return "E  USE";
    }

    private static String inventoryProcessAction(Supply supply) {
        if (supply.cooksTo() != null) return "K  COOK AT FIRE";
        if (supply.filtersTo() != null) return "F  FILTER   V  BOIL";
        if (supply.boilsTo() != null) return "V  BOIL AT FIRE";
        return null;
    }

    /** Compact four-line message feed floating at the lower-left without panel chrome. */
    private void renderMessagePanel() {
        int x = HUD_MARGIN;
        renderBurgerButton();

        List<String> lines = recentLogLines(LOG_PANEL_W - 16, LOG_LINES);
        int baseline = LOG_PANEL_Y + LOG_PANEL_H - 8;
        for (String line : lines) {
            drawLogText(line, x + 8, baseline);
            baseline -= UI_LINE;
        }

    }

    private void renderBurgerButton() {
        int x = hudWidth() - HUD_MARGIN - 30;
        int y = TOP_PANEL_Y + TOP_PANEL_H - 22;
        fillRect(x, y, 30, 20, UI_PANEL_STRONG);
        strokeRect(x, y, 30, 20, UI_BORDER);
        for (int row = 0; row < 3; row++) {
            fillRect(x + 7, y + 5 + row * 4, 16, 2, UI_MUTED);
        }
    }

    /** Title screen over the original moonlit forest-settlement key art. */
    private void renderStartupMenu() {
        batch.setColor(Color.WHITE);
        batch.draw(pixels.mainMenuBackground(), 0, 0, hudWidth(), hudHeight());
        fillRect(0, 0, hudWidth(), hudHeight(), new Color(0.005f, 0.009f, 0.007f, 0.30f));
        if (menuPage == MenuPage.OPTIONS) {
            renderOptionsPage();
            return;
        }
        if (menuPage == MenuPage.HOW_TO_PLAY) {
            renderHowToPlayPage();
            return;
        }

        drawLargeTitle("THE MARGINS", hudHeight() - 58);
        font.setColor(UI_MUTED);
        font.draw(batch, "SURVIVE WHAT THE FOREST GIVES", 0, hudHeight() - 80,
                hudWidth(), Align.center, false);

        String[] labels = startupMenuLabels();
        int x = startupButtonX(), firstY = startupFirstButtonY();
        for (int i = 0; i < labels.length; i++) {
            drawMenuButton(x, firstY - i * 34, 230, 27, labels[i], i == startupSelection,
                    i == 0);
        }
        drawText("W/S SELECT   ENTER CONFIRM", 10, 10, UI_MUTED);
    }

    /** Pause root requested by the player; subpages replace it in the same modal surface. */
    private void renderMenuPanel() {
        fillRect(0, 0, hudWidth(), hudHeight(), new Color(0.01f, 0.015f, 0.012f, 0.72f));
        if (menuPage == MenuPage.OPTIONS) {
            renderOptionsPage();
            return;
        }
        if (menuPage == MenuPage.HOW_TO_PLAY) {
            renderHowToPlayPage();
            return;
        }

        int width = 270, height = 250;
        int x = (hudWidth() - width) / 2, y = 54;
        drawPanel(x, y, width, height, UI_PANEL_STRONG);
        drawHeadingCentered("PAUSED", y + height - 18, UI_ACCENT);
        fillRect(x + 14, y + height - 31, width - 28, 1, UI_BORDER);

        String[] labels = {"RESUME", "OPTIONS", "HOW TO PLAY", "MAIN MENU"};
        int buttonX = pauseButtonX(), firstY = pauseFirstButtonY();
        for (int i = 0; i < labels.length; i++) {
            drawMenuButton(buttonX, firstY - i * 40, 210, 32, labels[i],
                    i == menuSelection, i == 0);
        }
        drawText("ESC / M  RESUME", x + 14, y + 13, UI_MUTED);
    }

    private void renderOptionsPage() {
        int width = 280, height = 210;
        int x = (hudWidth() - width) / 2, y = 72;
        drawPanel(x, y, width, height, UI_PANEL_STRONG);
        drawHeadingCentered("OPTIONS", y + height - 18, UI_ACCENT);
        fillRect(x + 14, y + height - 31, width - 28, 1, UI_BORDER);

        drawMenuButton((hudWidth() - 210) / 2, 159, 210, 30,
                "FULLSCREEN: " + (Gdx.graphics.isFullscreen() ? "ON" : "OFF"),
                menuSelection == 0, true);
        drawMenuButton((hudWidth() - 210) / 2, 120, 210, 30,
                "BACK", menuSelection == 1, false);
        drawText("ENTER TO TOGGLE", x + 14, y + 18, UI_MUTED);
    }

    private void renderHowToPlayPage() {
        int width = 360, height = 300;
        int x = (hudWidth() - width) / 2, y = 30;
        drawPanel(x, y, width, height, UI_PANEL_STRONG);
        drawHeadingCentered("HOW TO PLAY", y + height - 18, UI_ACCENT);
        fillRect(x + 14, y + height - 31, width - 28, 1, UI_BORDER);

        int left = x + 18, right = x + 190, top = y + height - 50;
        drawHeading("EXPLORE", left, top, UI_MUTED);
        drawText("WASD / ARROWS  Move", left, top - 17, UI_TEXT);
        drawText("Q  Attack", left, top - 31, UI_TEXT);
        drawText("G  Take item", left, top - 45, UI_TEXT);
        drawText("SPACE  Wait", left, top - 59, UI_TEXT);
        drawText("TAB  Backpack", left, top - 73, UI_TEXT);
        drawText("J  Journal", left, top - 87, UI_TEXT);

        drawHeading("SURVIVE", right, top, UI_MUTED);
        drawText("C  Forage", right, top - 17, UI_TEXT);
        drawText("B  Campfire", right, top - 31, UI_TEXT);
        drawText("T  Craft torch", right, top - 45, UI_TEXT);
        drawText("K  Cook", right, top - 59, UI_TEXT);
        drawText("F  Filter water", right, top - 73, UI_TEXT);
        drawText("V  Boil water", right, top - 87, UI_TEXT);
        drawText("E  Use selected item", right, top - 101, UI_TEXT);

        fillRect(x + 14, y + 92, width - 28, 1, UI_BORDER);
        drawText("Stay fed, hydrated, warm, and hidden.", x + 18, y + 76, UI_TEXT);
        drawText("Treelines block sight. Light helps you—and reveals you.",
                x + 18, y + 62, UI_TEXT);
        drawMenuButton((hudWidth() - 210) / 2, 42, 210, 30,
                "BACK", menuSelection == 0, false);
    }

    private void drawMenuButton(int x, int y, int width, int height, String label,
                                boolean selected, boolean primary) {
        Color edge = selected ? UI_ACCENT : UI_BORDER;
        Color fill = selected ? new Color(0.16f, 0.18f, 0.12f, 0.98f)
                : new Color(0.065f, 0.080f, 0.067f, 0.96f);
        fillRect(x + 2, y - 2, width, height, new Color(0f, 0f, 0f, 0.55f));
        fillRect(x, y, width, height, fill);
        strokeRect(x, y, width, height, edge);
        strokeRect(x + 2, y + 2, width - 4, height - 4,
                selected ? UI_ACCENT : new Color(0.16f, 0.20f, 0.17f, 1f));
        font.setColor(selected || primary ? UI_ACCENT : UI_TEXT);
        font.draw(batch, label, x, y + height / 2f + 4, width, Align.center, false);
    }

    private void drawLargeTitle(String value, int baseline) {
        float oldX = headingFont.getData().scaleX;
        float oldY = headingFont.getData().scaleY;
        headingFont.getData().setScale(2f);
        headingFont.setColor(new Color(0.02f, 0.03f, 0.02f, 0.90f));
        headingFont.draw(batch, value, 2, baseline - 2, hudWidth(), Align.center, false);
        headingFont.setColor(UI_TEXT);
        headingFont.draw(batch, value, 0, baseline, hudWidth(), Align.center, false);
        headingFont.getData().setScale(oldX, oldY);
    }

    private void drawHeadingCentered(String value, int baseline, Color color) {
        headingFont.setColor(color);
        headingFont.draw(batch, value, 0, baseline, hudWidth(), Align.center, false);
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

        fillRect(0, 0, hudWidth(), hudHeight(), new Color(0.01f, 0.015f, 0.012f, 0.58f));
        int panelW = WW - 36, panelH = 210;
        int panelX = (hudWidth() - panelW) / 2, panelY = 58;
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
        fillRect(0, 0, hudWidth(), hudHeight(), new Color(0.01f, 0.015f, 0.012f, 0.58f));
        int panelW = WW - 36, panelH = 210;
        int panelX = (hudWidth() - panelW) / 2, panelY = 58;
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
        fillRect(0, 0, hudWidth(), hudHeight(), new Color(0.01f, 0.01f, 0.008f, 0.66f));
        int w = 300, h = 72;
        int x = (hudWidth() - w) / 2, y = 143;
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

    /** Hard one-pixel outline plus a two-pixel cast shadow keeps floating log text terrain-safe. */
    private void drawLogText(String value, float x, float y) {
        drawText(value, x + 2, y - 2, UI_LOG_SHADOW);
        drawText(value, x - 1, y, UI_LOG_SHADOW);
        drawText(value, x + 1, y, UI_LOG_SHADOW);
        drawText(value, x, y - 1, UI_LOG_SHADOW);
        drawText(value, x, y + 1, UI_LOG_SHADOW);
        drawText(value, x, y, UI_TEXT);
    }

    private void drawHeading(String value, float x, float y, Color color) {
        headingFont.setColor(color);
        headingFont.draw(batch, value, x, y);
    }

    private void drawPanel(float x, float y, float width, float height, Color fill) {
        fillRect(x, y, width, height, fill);
        strokeRect(x, y, width, height, UI_BORDER);
    }

    /** Layered leather/copper frame used by both the quickbar and full backpack. */
    private void drawInventoryFrame(float x, float y, float width, float height) {
        fillRect(x + 2, y - 2, width, height, new Color(0.025f, 0.012f, 0.004f, 0.76f));
        fillRect(x, y, width, height, INV_OUTLINE);
        fillRect(x + 1, y + 1, width - 2, height - 2, INV_TRIM);
        fillRect(x + 3, y + 3, width - 6, height - 6, INV_PANEL);
        strokeRect(x + 3, y + 3, width - 6, height - 6, INV_HIGHLIGHT);
    }

    /** Recessed red-leather slot with a thick gold focus state, matching the supplied reference. */
    private void drawInventorySlot(float x, float y, float width, float height, boolean selected) {
        fillRect(x, y, width, height, INV_OUTLINE);
        fillRect(x + 1, y + 1, width - 2, height - 2, selected ? INV_GOLD : INV_TRIM);
        fillRect(x + 3, y + 3, width - 6, height - 6,
                selected ? INV_SLOT_SELECTED : INV_SLOT);
        if (selected) {
            fillRect(x + 3, y + height - 4, width - 6, 1, INV_HIGHLIGHT);
        }
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

    private int hudWidth() {
        return Math.round(viewport.getWorldWidth());
    }

    private int hudHeight() {
        return Math.round(viewport.getWorldHeight());
    }

    @Override public void resize(int w, int h) { viewport.update(w, h); }
    @Override public void show() {}
    @Override public void hide() { saveActiveJourney(); }
    @Override public void pause() { saveActiveJourney(); }
    @Override public void resume() {}

    private void saveActiveJourney() {
        if (state != null && !startupMenuOpen && state.getPlayer().isAlive()) {
            SaveService.save(state);
            hasContinue = true;
        }
    }
    @Override public void dispose() {
        batch.dispose();
        font.dispose();
        headingFont.dispose();
        uiPixel.dispose();
        pixels.dispose();
    }
}

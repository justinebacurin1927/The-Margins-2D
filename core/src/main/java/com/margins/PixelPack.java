package com.margins;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * Loads The Margin Pixel Pack v2 runtime atlases ({@code assets/characters.png},
 * {@code assets/characters-walk.png}, {@code assets/characters-attack.png},
 * {@code assets/items.png}, {@code assets/terrain-tiles.png},
 * {@code assets/ground-materials.png},
 * {@code assets/light-weather-effects.png},
 * {@code assets/fog-effects-v2.png},
 * {@code assets/forest-autotiles.png}, {@code assets/structures/old-house.png},
 * {@code assets/structures/old-house-foreground.png},
 * {@code assets/structures/graveyard.png}, {@code assets/structures/deep-cave.png},
 * {@code assets/structures/hunters-blind.png}, plus the remaining Tier-1/Tier-2 atlases, and
 * {@code assets/environment-tiles.png}) and slices them into
 * per-cell {@link TextureRegion}s for the screen layer (AD-1/AD-2 — no model impact,
 * headless tests unaffected). v2 cells are already game-scale 16px art; nearest-neighbour
 * filtering keeps them crisp drawn at the 24px {@code TILE}. Terrain cells are native
 * 24px, fully opaque replacement tiles; the 16px environment cells remain available as
 * transparent overlay props. The Old House is deliberately sliced into 150 24px cells: a
 * fog-aware, explorable 13x8 structure surrounded by a one-cell visual foundation apron.
 *
 * <p>Indexing is row-major: cell (row, col) = {@code row * cols + col}. See the pack's
 * README for the exact order (Klein=0, Aldric=1, Giliman foot soldier=6; the 40 item
 * icons are five rows of eight; the 64 environment tiles are eight rows of eight).
 */
public class PixelPack {

    private static final int CHARACTER_COLS = 4, CHARACTER_ROWS = 4;
    private static final int WALK_COLS = 2, WALK_ROWS = 3;
    private static final int ATTACK_COLS = 3, ATTACK_ROWS = 3;
    private static final int ITEM_COLS = 8, ITEM_ROWS = 5;
    private static final int STATUS_ICON_COLS = 3, STATUS_ICON_ROWS = 3;
    private static final int TERRAIN_COLS = 4, TERRAIN_ROWS = 4;
    private static final int GROUND_COLS = 4, GROUND_ROWS = 4;
    private static final int EFFECT_COLS = 4, EFFECT_ROWS = 2;
    private static final int FOG_COLS = 4;
    private static final int FOREST_COLS = 4, FOREST_ROWS = 4;
    private static final int ENV_COLS = 8, ENV_ROWS = 8;
    private static final int OLD_HOUSE_COLS = 15, OLD_HOUSE_ROWS = 10;
    private static final int GRAVEYARD_COLS = 11, GRAVEYARD_ROWS = 9;
    private static final int DEEP_CAVE_COLS = 11, DEEP_CAVE_ROWS = 9;
    private static final int HUNTERS_BLIND_COLS = 9, HUNTERS_BLIND_ROWS = 9;
    private static final int STANDARD_STRUCTURE_COLS = 9, STANDARD_STRUCTURE_ROWS = 9;
    private static final int FALLEN_LOG_COLS = 9, FALLEN_LOG_ROWS = 5;
    private static final int BEEHIVE_COLS = 11, BEEHIVE_ROWS = 11;
    private static final int WATCHTOWER_COLS = 11, WATCHTOWER_ROWS = 13;
    private static final int POACHERS_CAMP_COLS = 13, POACHERS_CAMP_ROWS = 11;
    private static final int SUNKEN_WELL_COLS = 11, SUNKEN_WELL_ROWS = 11;
    private static final int MELEE_EFFECT_FRAMES = 4;

    private final Texture charactersTex;
    private final Texture charactersWalkTex;
    private final Texture charactersAttackTex;
    private final Texture itemsTex;
    private final Texture statusIconsTex;
    private final Texture terrainTex;
    private final Texture groundTex;
    private final Texture effectsTex;
    private final Texture effectsSmoothTex;
    private final Texture mainMenuBackgroundTex;
    private final Texture mainMenuLogoTex;
    private final Texture backpackIconTex;
    private final Texture fogTex;
    private final Texture forestTex;
    private final Texture envTex;
    private final Texture meleeEffectsTex;
    private final Texture oldHouseTex;
    private final Texture oldHouseForegroundTex;
    private final Texture graveyardTex;
    private final Texture deepCaveTex;
    private final Texture huntersBlindTex;
    private final Texture fallenLogHollowTex;
    private final Texture forestShrineTex;
    private final Texture beehiveGroveTex;
    private final Texture kitchenCampTex;
    private final Texture collapsedWatchtowerTex;
    private final Texture poachersCampTex;
    private final Texture sunkenWellTex;
    private final TextureRegion[] characters;
    private final TextureRegion[] charactersWalk;
    private final TextureRegion[] charactersAttack;
    private final TextureRegion[] items;
    private final TextureRegion[] statusIcons;
    private final TextureRegion[] terrain;
    private final TextureRegion[] ground;
    private final TextureRegion[] effects;
    private final TextureRegion[] effectsSmooth;
    private final TextureRegion mainMenuBackground;
    private final TextureRegion mainMenuLogo;
    private final TextureRegion backpackIcon;
    private final TextureRegion[] fog;
    private final TextureRegion[] forest;
    private final TextureRegion[] env;
    private final TextureRegion[] meleeEffects;
    private final TextureRegion[] oldHouse;
    private final TextureRegion[] oldHouseForeground;
    private final TextureRegion[] graveyard;
    private final TextureRegion[] deepCave;
    private final TextureRegion[] huntersBlind;
    private final TextureRegion[] fallenLogHollow;
    private final TextureRegion[] forestShrine;
    private final TextureRegion[] beehiveGrove;
    private final TextureRegion[] kitchenCamp;
    private final TextureRegion[] collapsedWatchtower;
    private final TextureRegion[] poachersCamp;
    private final TextureRegion[] sunkenWell;

    public PixelPack() {
        charactersTex = load("assets/characters.png");
        charactersWalkTex = load("assets/characters-walk.png");
        charactersAttackTex = load("assets/characters-attack.png");
        itemsTex = load("assets/items.png");
        statusIconsTex = load("assets/status-icons.png");
        terrainTex = load("assets/terrain-tiles.png");
        groundTex = load("assets/ground-materials.png");
        effectsTex = load("assets/light-weather-effects.png");
        effectsSmoothTex = loadSmooth("assets/light-weather-effects.png");
        mainMenuBackgroundTex = loadSmooth("assets/main-menu-background.png");
        mainMenuLogoTex = load("assets/icons/the-margin-256.png");
        backpackIconTex = load("assets/ui/backpack-icon.png");
        fogTex = load("assets/fog-effects-v2.png");
        forestTex = load("assets/forest-autotiles.png");
        envTex = load("assets/environment-tiles.png");
        meleeEffectsTex = load("assets/melee-slash.png");
        oldHouseTex = load("assets/structures/old-house.png");
        oldHouseForegroundTex = load("assets/structures/old-house-foreground.png");
        graveyardTex = load("assets/structures/graveyard.png");
        deepCaveTex = load("assets/structures/deep-cave.png");
        huntersBlindTex = load("assets/structures/hunters-blind.png");
        fallenLogHollowTex = load("assets/structures/fallen-log-hollow.png");
        forestShrineTex = load("assets/structures/forest-shrine.png");
        beehiveGroveTex = load("assets/structures/beehive-grove.png");
        kitchenCampTex = load("assets/structures/worn-down-kitchen-camp.png");
        collapsedWatchtowerTex = load("assets/structures/collapsed-watchtower.png");
        poachersCampTex = load("assets/structures/poachers-camp.png");
        sunkenWellTex = load("assets/structures/sunken-well.png");
        characters = slice(charactersTex, CHARACTER_COLS, CHARACTER_ROWS);
        charactersWalk = slice(charactersWalkTex, WALK_COLS, WALK_ROWS);
        charactersAttack = slice(charactersAttackTex, ATTACK_COLS, ATTACK_ROWS);
        items = slice(itemsTex, ITEM_COLS, ITEM_ROWS);
        statusIcons = slice(statusIconsTex, STATUS_ICON_COLS, STATUS_ICON_ROWS);
        terrain = slice(terrainTex, TERRAIN_COLS, TERRAIN_ROWS);
        ground = slice(groundTex, GROUND_COLS, GROUND_ROWS);
        effects = slice(effectsTex, EFFECT_COLS, EFFECT_ROWS);
        effectsSmooth = slice(effectsSmoothTex, EFFECT_COLS, EFFECT_ROWS);
        mainMenuBackground = new TextureRegion(mainMenuBackgroundTex);
        mainMenuLogo = new TextureRegion(mainMenuLogoTex);
        backpackIcon = new TextureRegion(backpackIconTex);
        fog = slice(fogTex, FOG_COLS, 1);
        forest = slice(forestTex, FOREST_COLS, FOREST_ROWS);
        env = slice(envTex, ENV_COLS, ENV_ROWS);
        meleeEffects = slice(meleeEffectsTex, MELEE_EFFECT_FRAMES, 1);
        oldHouse = slice(oldHouseTex, OLD_HOUSE_COLS, OLD_HOUSE_ROWS);
        oldHouseForeground = slice(oldHouseForegroundTex, OLD_HOUSE_COLS, OLD_HOUSE_ROWS);
        graveyard = slice(graveyardTex, GRAVEYARD_COLS, GRAVEYARD_ROWS);
        deepCave = slice(deepCaveTex, DEEP_CAVE_COLS, DEEP_CAVE_ROWS);
        huntersBlind = slice(huntersBlindTex, HUNTERS_BLIND_COLS, HUNTERS_BLIND_ROWS);
        fallenLogHollow = slice(fallenLogHollowTex, FALLEN_LOG_COLS, FALLEN_LOG_ROWS);
        forestShrine = slice(forestShrineTex, STANDARD_STRUCTURE_COLS, STANDARD_STRUCTURE_ROWS);
        beehiveGrove = slice(beehiveGroveTex, BEEHIVE_COLS, BEEHIVE_ROWS);
        kitchenCamp = slice(kitchenCampTex, STANDARD_STRUCTURE_COLS, STANDARD_STRUCTURE_ROWS);
        collapsedWatchtower = slice(collapsedWatchtowerTex, WATCHTOWER_COLS, WATCHTOWER_ROWS);
        poachersCamp = slice(poachersCampTex, POACHERS_CAMP_COLS, POACHERS_CAMP_ROWS);
        sunkenWell = slice(sunkenWellTex, SUNKEN_WELL_COLS, SUNKEN_WELL_ROWS);
    }

    private static Texture load(String path) {
        Texture t = new Texture(Gdx.files.internal(path));
        t.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        return t;
    }

    /** Radial light is the one deliberate soft effect; sprites and tiles remain nearest-filtered. */
    private static Texture loadSmooth(String path) {
        Texture t = new Texture(Gdx.files.internal(path));
        t.setFilter(TextureFilter.Linear, TextureFilter.Linear);
        return t;
    }

    /** Slice an atlas into row-major cell regions (cell (r,c) = index {@code r*cols+c}). */
    private static TextureRegion[] slice(Texture tex, int cols, int rows) {
        TextureRegion[] regions = new TextureRegion[cols * rows];
        int cw = tex.getWidth() / cols, ch = tex.getHeight() / rows;
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                regions[r * cols + c] = new TextureRegion(tex, c * cw, r * ch, cw, ch);
        return regions;
    }

    /** A character cell (row-major): 0 Klein, 1 Aldric, 6 Giliman foot soldier, ... */
    public TextureRegion character(int index) {
        return characters[index];
    }

    /**
     * One of two true alternate walking poses for the three currently active actor designs.
     * Rows are Klein, Aldric, and the Giliman soldier; unknown character ids safely stay idle.
     */
    public TextureRegion walkingCharacter(int characterIndex, int frame) {
        int row = characterIndex == 0 ? 0 : characterIndex == 1 ? 1 : characterIndex == 6 ? 2 : -1;
        return row < 0 ? character(characterIndex) : charactersWalk[row * WALK_COLS + (frame & 1)];
    }

    /**
     * Grounded anticipation, strike, and recovery poses for the three active actor designs.
     * The 24x16 source cells leave room for the sword without moving the actor off its tile.
     */
    public TextureRegion attackingCharacter(int characterIndex, int frame) {
        int row = characterIndex == 0 ? 0 : characterIndex == 1 ? 1 : characterIndex == 6 ? 2 : -1;
        int clampedFrame = Math.max(0, Math.min(ATTACK_COLS - 1, frame));
        return row < 0 ? character(characterIndex) : charactersAttack[row * ATTACK_COLS + clampedFrame];
    }

    /** An item-icon cell (row-major, five rows of eight). */
    public TextureRegion item(int index) {
        return items[index];
    }

    /** Tintable survival-status icon (3x3 atlas, row-major). */
    public TextureRegion statusIcon(int index) {
        return statusIcons[Math.max(0, Math.min(statusIcons.length - 1, index))];
    }

    /** A fully opaque terrain cell (row-major, four rows of four, native 24px). */
    public TextureRegion terrain(int index) {
        return terrain[index];
    }

    /** Contextual floor material (4x4 atlas, row-major, native 24px). */
    public TextureRegion ground(int index) {
        return ground[Math.max(0, Math.min(ground.length - 1, index))];
    }

    /** Light/weather effect cell (4x2 atlas, row-major, native 48px). */
    public TextureRegion effect(int index) {
        return effects[Math.max(0, Math.min(effects.length - 1, index))];
    }

    /** Linear-filtered twin for large radial light/vignette cells only. */
    public TextureRegion smoothEffect(int index) {
        return effectsSmooth[Math.max(0, Math.min(effectsSmooth.length - 1, index))];
    }

    /** Full original title-screen artwork, linearly filtered only while fitting the viewport. */
    public TextureRegion mainMenuBackground() {
        return mainMenuBackground;
    }

    /** Transparent tree-and-sword project emblem used by the title screen. */
    public TextureRegion mainMenuLogo() {
        return mainMenuLogo;
    }

    /** Compact leather pouch used as the shared quickbar/full-inventory heading mark. */
    public TextureRegion backpackIcon() {
        return backpackIcon;
    }

    /** One of four transparent, horizontally drifting pixel-mist frames. */
    public TextureRegion fog(int frame) {
        return fog[Math.floorMod(frame, fog.length)];
    }

    /** A forest-wall cell indexed by its N/E/S/W clearing-neighbor bitmask. */
    public TextureRegion forest(int clearingMask) {
        return forest[clearingMask];
    }

    /** An environment-tile cell (row-major, eight rows of eight). */
    public TextureRegion tile(int index) {
        return env[index];
    }

    /** One frame of the four-step melee slash effect. */
    public TextureRegion meleeSlash(int frame) {
        return meleeEffects[Math.max(0, Math.min(MELEE_EFFECT_FRAMES - 1, frame))];
    }

    /** One cell of the 15x10 Old House atlas (13x8 architecture plus foundation apron). */
    public TextureRegion oldHouse(int cell) {
        return oldHouse[Math.max(0, Math.min(oldHouse.length - 1, cell))];
    }

    /** Transparent front-wall cell redrawn above actors to make the wall read as foreground. */
    public TextureRegion oldHouseForeground(int cell) {
        return oldHouseForeground[Math.max(0, Math.min(oldHouseForeground.length - 1, cell))];
    }

    /** One cell of the 11x9 Mercenary Graveyard atlas. */
    public TextureRegion graveyard(int cell) {
        return graveyard[Math.max(0, Math.min(graveyard.length - 1, cell))];
    }

    /** One cell of the 11x9 Deep Cave Mouth atlas. */
    public TextureRegion deepCave(int cell) {
        return deepCave[Math.max(0, Math.min(deepCave.length - 1, cell))];
    }

    /** One cell of the 9x9 Hunter's Blind atlas. */
    public TextureRegion huntersBlind(int cell) {
        return huntersBlind[Math.max(0, Math.min(huntersBlind.length - 1, cell))];
    }

    public TextureRegion fallenLogHollow(int cell) { return structureCell(fallenLogHollow, cell); }
    public TextureRegion forestShrine(int cell) { return structureCell(forestShrine, cell); }
    public TextureRegion beehiveGrove(int cell) { return structureCell(beehiveGrove, cell); }
    public TextureRegion kitchenCamp(int cell) { return structureCell(kitchenCamp, cell); }
    public TextureRegion collapsedWatchtower(int cell) {
        return structureCell(collapsedWatchtower, cell);
    }
    public TextureRegion poachersCamp(int cell) { return structureCell(poachersCamp, cell); }
    public TextureRegion sunkenWell(int cell) { return structureCell(sunkenWell, cell); }

    private static TextureRegion structureCell(TextureRegion[] atlas, int cell) {
        return atlas[Math.max(0, Math.min(atlas.length - 1, cell))];
    }

    public void dispose() {
        charactersTex.dispose();
        charactersWalkTex.dispose();
        charactersAttackTex.dispose();
        itemsTex.dispose();
        statusIconsTex.dispose();
        terrainTex.dispose();
        groundTex.dispose();
        effectsTex.dispose();
        effectsSmoothTex.dispose();
        mainMenuBackgroundTex.dispose();
        mainMenuLogoTex.dispose();
        backpackIconTex.dispose();
        fogTex.dispose();
        forestTex.dispose();
        envTex.dispose();
        meleeEffectsTex.dispose();
        oldHouseTex.dispose();
        oldHouseForegroundTex.dispose();
        graveyardTex.dispose();
        deepCaveTex.dispose();
        huntersBlindTex.dispose();
        fallenLogHollowTex.dispose();
        forestShrineTex.dispose();
        beehiveGroveTex.dispose();
        kitchenCampTex.dispose();
        collapsedWatchtowerTex.dispose();
        poachersCampTex.dispose();
        sunkenWellTex.dispose();
    }
}

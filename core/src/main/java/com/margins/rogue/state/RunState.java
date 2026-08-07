package com.margins.rogue.state;

import com.margins.rogue.Companion;
import com.margins.rogue.DayPhase;
import com.margins.rogue.FloorGenerator;
import com.margins.rogue.FloorGenerator.FloorResult;
import com.margins.rogue.NoiseEvent;
import com.margins.rogue.RogueEnemy;
import com.margins.rogue.RoguePlayer;
import com.margins.rogue.RogueTileMap;
import com.margins.rogue.Weather;
import com.margins.rogue.item.FloorItem;
import com.margins.rogue.item.Inventory;
import com.margins.rogue.item.Supply;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Single owner of all run data (AD-3): tilemap, player, enemies, current map,
 * seed and the seeded RNG. Systems mutate this; nothing else holds an
 * authoritative duplicate. This is the unit that will be serialized for save
 * (AD-6). Contains NO libGDX rendering types (AD-2) so it stays headless-testable.
 */
public class RunState {

    private static final int MAP_W = 50;
    private static final int MAP_H = 50;

    /** Save-format version (AD-6). Bumped when the persisted shape changes incompatibly. */
    public static final int SAVE_VERSION = 1;

    /** Day/Night cycle lengths (FR-5): Day 100 turns / Night 70 = one 170-turn cycle. */
    public static final int DAY_LENGTH = 100;
    public static final int NIGHT_LENGTH = 70;
    public static final int CYCLE_LENGTH = DAY_LENGTH + NIGHT_LENGTH;

    private RogueTileMap tileMap;
    private RoguePlayer player;
    private List<RogueEnemy> enemies;
    // Field-initialized so it's non-null even when a save predating this field is loaded (Json skips the constructor).
    private Inventory inventory = new Inventory();  // finite carry: 8 backpack stacks + 2 equipped slots (FR-9, AD-12)
    private List<FloorItem> floorItems = new ArrayList<>(); // items lying on tiles; persisted so drops survive save/load (FR-10)
    private List<Companion> companions = new ArrayList<>(); // allied turn actors; single party slot (AD-10)
    private IdentifyMap identifyMap;      // per-seed Supply→TrueIdentity binding (FR-11, AD-12); built at run start, persisted
    // Run-scoped narrative state (AD-7): flags + Galleon's Bond. Field-initialized
    // so a pre-4.3 save (no flagStore key) loads empty-but-non-null (AD-6), like inventory.
    private FlagStore flagStore = new FlagStore();
    // Save-format stamp (AD-6). Field-initialized, so a fresh run and a fresh load both
    // report the current version — which is why the pre-AD-8 reject in SaveService can't gate
    // on this int (fromJson runs initializers): it gates on the *raw JSON* lacking the
    // saveVersion key instead. The int is the FORWARD mechanism: a future v1->v2 compares it.
    private int saveVersion = SAVE_VERSION;
    // Day/Night clock (FR-4/FR-5): a turn counter that advances on acted turns. The phase is
    // DERIVED from it (see getClockPhase); weather rolls per cycle boundary (see rollWeather).
    // Field-initialized so a save predating it loads at 0 (AD-6).
    private int clockTurns = 0;
    // Per-cycle weather (FR-5, Story 1.3): exactly one type rolls per 170-turn cycle on the
    // weighted distribution. Effects (FOV/temperature/spoilage) are later stories — here it is
    // state the owning stories key off. Field-initialized so a save predating them loads valid
    // defaults (AD-6); cycleNumber = clockTurns / CYCLE_LENGTH, the cycle last rolled.
    private Weather weather = Weather.CLEAR;
    private int cycleNumber = 0;
    // Active light source (FR-5/FR-7, Story 1.4): the tile of a lit campfire/torch, or -1 = none.
    // A positioned light (not a player boolean) because AD-18 emits the light's noise at ITS tile
    // and a campfire (1.5) is stationary — after Klein walks away, the fire's tile != his tile.
    // Field-initialized to none so a save predating these fields loads light-less (AD-6). The
    // campfire/torch ITEMS that set this are Stories 1.5/1.6; here it is queryable state + wiring.
    private int lightX = -1;
    private int lightY = -1;
    private long seed;
    private transient Random rng;
    private boolean lastStandUsed;        // persisted: one reprieve per run (FR-16/17)
    private transient boolean lastStand;  // turn-scoped desperate flag set when the reprieve fires
    // Transient, regenerated each turn (not saved); field initializer keeps it non-null after a Json load.
    private transient List<NoiseEvent> noiseQueue = new ArrayList<>();

    public RunState() {
        this(System.nanoTime());
    }

    /**
     * A seeded RNG with its cold start skipped: {@link Random}'s first outputs
     * correlate across nearby seeds, which would rig the first supply-identity
     * draw for small debug seeds. Skipping two draws decorrelates it while keeping
     * one seeded stream (AD-5).
     */
    private static Random seededRng(long seed) {
        Random r = new Random(seed);
        r.nextInt();
        r.nextInt();
        return r;
    }

    public RunState(long seed) {
        this.seed = seed;
        this.rng = seededRng(seed);
        this.identifyMap = IdentifyMap.build(rng); // bind supply identities at run start (FR-11, AD-12)
        generateFloor();
        spawnStartingCompanion();
        rollWeather(); // cycle 0's weather, drawn LAST so layout/identity draws stay on the pre-1.3 stream
    }

    /** Builds the continuous region and places a fresh player and enemies (run start/restart). */
    public void generateFloor() {
        FloorResult result = FloorGenerator.generate(MAP_W, MAP_H, rng);
        tileMap = result.map;

        int startCx = result.roomCenters.get(0)[0];
        int startCy = result.roomCenters.get(0)[1];
        player = new RoguePlayer(startCx, startCy, tileMap);

        placeFloorActors(result, player.getTileX(), player.getTileY());
    }

    /**
     * Build the region's enemies and scattered supplies, avoiding the
     * given tile (the player's). Extracted from {@link #generateFloor} so
     * actor placement stays separable from map generation.
     */
    private void placeFloorActors(FloorResult result, int avoidX, int avoidY) {
        enemies = new ArrayList<>();
        for (int i = 1; i < result.roomCenters.size(); i++) {
            int cx = result.roomCenters.get(i)[0];
            int cy = result.roomCenters.get(i)[1];
            int count = 1 + rng.nextInt(2);
            for (int e = 0; e < count; e++) {
                int ex = cx + rng.nextInt(3) - 1;
                int ey = cy + rng.nextInt(3) - 1;
                if (tileMap.isWalkable(ex, ey) && !(ex == avoidX && ey == avoidY)) {
                    enemies.add(new RogueEnemy(ex, ey, tileMap));
                }
            }
        }

        // Scatter a few supplies for this floor, drawn from the seeded RNG so a
        // seed reproduces the floor's items (AD-5). Cleared here so each generated
        // floor starts fresh; a loaded run keeps its saved floorItems (no regen).
        floorItems.clear();
        if (result.roomCenters.size() > 1) {
            int supplyCount = 2 + rng.nextInt(3); // 2..4
            int placed = 0, attempts = 0;
            while (placed < supplyCount && attempts < 100) {
                attempts++;
                int[] c = result.roomCenters.get(1 + rng.nextInt(result.roomCenters.size() - 1));
                int ix = c[0] + rng.nextInt(3) - 1;
                int iy = c[1] + rng.nextInt(3) - 1;
                if (tileMap.isWalkable(ix, iy) && !(ix == avoidX && iy == avoidY)) {
                    floorItems.add(new FloorItem(rng.nextInt(Supply.count()), 1, ix, iy));
                    placed++;
                }
            }
        }
    }

    /**
     * Re-wire transient fields after a libGDX Json load (AD-6): rebuild the RNG
     * from the stored seed and re-inject the tilemap into the player and enemies
     * (they hold it transiently so the map serializes once, under this root only).
     * Future draws restart from the seed — AC-1 requires reproducible layout, not
     * mid-run draw parity.
     */
    public void restoreAfterLoad() {
        this.rng = new Random(seed);
        // IdentifyMap is a persisted field — it loads with the run, so the resumed
        // run keeps its per-seed binding (verified by round-trip); no rebuild needed.
        // Reconcile a save predating the weather/cycle fields (Story 1.3): the no-arg ctor
        // that libGDX Json runs fully constructs with a nanoTime seed, so a field-absent save
        // inherits a non-deterministic weather (different every reload) — the same documented
        // migration wart as identifyMap (deferred 3.3 item). We cannot distinguish it from a
        // legitimately rolled value, so we only null-guard. cycleNumber, however, is fully
        // derivable: clamp it from the persisted clock so a deep-clock old save is
        // self-consistent (its cycle matches its phase) on the very first post-load turn.
        // (AD-6 migration contract.)
        if (weather == null) weather = Weather.CLEAR;
        cycleNumber = clockTurns / CYCLE_LENGTH;
        player.setMap(tileMap);
        for (RogueEnemy e : enemies) {
            e.setMap(tileMap);
        }
        for (Companion c : companions) {
            c.setMap(tileMap);
        }
    }

    /** Restart a fresh run (same seeded RNG stream continues). */
    public void restart() {
        this.lastStandUsed = false;
        this.lastStand = false;
        this.identifyMap = IdentifyMap.build(rng); // a new run rebinds identities (FR-11)
        this.flagStore = new FlagStore(); // narrative state is run-scoped (AD-7): a new run resets flags + Bond
        this.clockTurns = 0; // a new run starts at Day 0 (FR-5)
        this.cycleNumber = 0;
        clearLight(); // a fresh run has no camp/torch lit (Story 1.4)
        generateFloor();
        spawnStartingCompanion();
        rollWeather(); // and rolls its own weather
    }

    /**
     * Spawn a single active companion beside the player at run start (a
     * placeholder bind — real recruitment is 4.3 + Epic 6). One slot (AD-10):
     * the list is cleared first so restart never accumulates.
     */
    private void spawnStartingCompanion() {
        companions.clear();
        int[] spot = companionSpotNear(player.getTileX(), player.getTileY());
        companions.add(new Companion(spot[0], spot[1], tileMap, "galleon"));
    }

    /** Walkable tile near (x,y): 4-dir adjacent first, then widening rings. */
    private int[] companionSpotNear(int x, int y) {
        for (int r = 1; r < 10; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -r; dy <= r; dy++) {
                    if (Math.abs(dx) + Math.abs(dy) != r) continue;
                    int nx = x + dx, ny = y + dy;
                    if (tileMap.isWalkable(nx, ny)) return new int[]{nx, ny};
                }
            }
        }
        return new int[]{x, y}; // unreachable on a carved floor
    }

    public RogueTileMap getTileMap() { return tileMap; }
    public RoguePlayer getPlayer() { return player; }
    public List<RogueEnemy> getEnemies() { return enemies; }

    /** The party (AD-10): at most one companion in MVP. */
    public List<Companion> getCompanions() { return companions; }

    /** The single active companion, or null if the party is empty (AD-10). */
    public Companion getActiveCompanion() {
        return companions.isEmpty() ? null : companions.get(0);
    }

    /** The finite carry container (FR-9): plain int arrays, so it saves/loads under this root for free (AD-6). */
    public Inventory getInventory() { return inventory; }

    /** Items lying on floor tiles (FR-10). */
    public List<FloorItem> getFloorItems() { return floorItems; }

    /** The per-seed Supply→TrueIdentity binding for this run (FR-11, AD-12). */
    public IdentifyMap getIdentifyMap() { return identifyMap; }

    /**
     * The run-scoped narrative store (AD-7): generic flags + Galleon's Bond.
     * Dialogue/quests read and write narrative state only through this.
     */
    public FlagStore getFlagStore() { return flagStore; }

    /** Place an item stack on a tile (a drop, or the return of a failed pickup). */
    public void addFloorItem(int type, int count, int x, int y) {
        floorItems.add(new FloorItem(type, count, x, y));
    }

    /** Remove and return the first item stack on the given tile, or null if none. */
    public FloorItem takeItemAt(int x, int y) {
        for (int i = 0; i < floorItems.size(); i++) {
            FloorItem it = floorItems.get(i);
            if (it.x == x && it.y == y) {
                floorItems.remove(i);
                return it;
            }
        }
        return null;
    }

    /** Whether any item stack sits on the given tile (screen uses this to gate pickup). */
    public boolean hasItemAt(int x, int y) {
        for (FloorItem it : floorItems) {
            if (it.x == x && it.y == y) return true;
        }
        return false;
    }
    /** The save-format version this run was created/loaded under (AD-6). */
    public int getSaveVersion() { return saveVersion; }

    /** Elapsed turns on the Day/Night clock (FR-4). */
    public int getClockTurns() { return clockTurns; }

    /** Current Day/Night phase, derived from the clock (FR-5): Day while clockTurns%170 < 100. */
    public DayPhase getClockPhase() {
        return clockTurns % CYCLE_LENGTH < DAY_LENGTH ? DayPhase.DAY : DayPhase.NIGHT;
    }

    public boolean isDay() { return getClockPhase() == DayPhase.DAY; }

    /** The weather in effect this cycle (FR-5). Effects live in later stories (1.4/1.5/1.6/3.x). */
    public Weather getWeather() { return weather; }

    /** Which 170-turn cycle the run is in (0-based). */
    public int getCycleNumber() { return cycleNumber; }

    /** Advance the Day/Night clock by one turn (called from the acted-branch, AD-4/AD-5).
     *  Crossing a 170-turn cycle boundary rolls the next cycle's weather (FR-5). */
    public void tickClock() {
        clockTurns++;
        if (clockTurns / CYCLE_LENGTH != cycleNumber) {
            cycleNumber = clockTurns / CYCLE_LENGTH;
            rollWeather();
        }
    }

    /** Roll the next cycle's weather from the seeded RNG (AD-5): run start, restart, and
     *  each cycle boundary (see {@link #tickClock}). */
    public void rollWeather() {
        weather = Weather.roll(rng);
    }

    /** Whether a light source (campfire/torch) is currently lit (FR-5/FR-7, Story 1.4). Both
     *  coordinates must be set — the "-1 = none" sentinel lives on the pair, so a half-set light
     *  (one coord still -1) is treated as unlit rather than emitting noise at an off-map row. */
    public boolean hasLight() { return lightX >= 0 && lightY >= 0; }

    /** The lit source's tile (FOV restoration + the tile its noise is emitted from, AD-18). */
    public int getLightX() { return lightX; }
    public int getLightY() { return lightY; }

    /** Light a source at a tile. Driven by the campfire (1.5, a fixed tile) and torch (1.6, the
     *  player's tile) — Story 1.4 provides the state + FOV/noise wiring, not the items. */
    public void setLight(int x, int y) { lightX = x; lightY = y; }

    /** Extinguish the active light (none). */
    public void clearLight() { lightX = -1; lightY = -1; }

    public long getSeed() { return seed; }

    /** The single seeded RNG all gameplay randomness should draw from (AD-5). */
    public Random rng() { return rng; }

    public boolean isLastStandUsed() { return lastStandUsed; }
    public void setLastStandUsed(boolean v) { lastStandUsed = v; }
    public boolean isLastStand() { return lastStand; }
    public void setLastStand(boolean v) { lastStand = v; }

    /** Producer API (AD-9): queue a noise stimulus for the NoiseSystem to resolve this turn. */
    public void emitNoise(int x, int y, int radius) {
        noiseQueue.add(new NoiseEvent(x, y, radius));
    }

    public List<NoiseEvent> getNoiseQueue() { return noiseQueue; }
}

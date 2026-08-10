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
import com.margins.rogue.world.StructureTable;
import com.margins.rogue.world.WorldSpine;
import com.margins.rogue.item.FloorItem;
import com.margins.rogue.item.Inventory;
import com.margins.rogue.item.Supply;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Single owner of all run data (AD-3): tilemap, player, enemies, current map,
 * seed and the seeded RNG. Systems mutate this; nothing else holds an
 * authoritative duplicate. This is the unit that will be serialized for save
 * (AD-6). Contains NO libGDX rendering types (AD-2) so it stays headless-testable.
 */
public class RunState {

    // Story 3.1 (Decision 2): the east/west spatial spine needs a real east, so the continuous
    // region is a ~2:1 horizontal map (west = home/border, east = the invasion). The screen
    // camera scrolls over a 20×15-tile window, so a wider map needs no rendering change.
    private static final int MAP_W = 96;
    private static final int MAP_H = 48;

    /** Save-format version (AD-6). Bumped when the persisted shape changes incompatibly. */
    public static final int SAVE_VERSION = 1;

    /** Day/Night cycle lengths (FR-5): Day 100 turns / Night 70 = one 170-turn cycle. */
    public static final int DAY_LENGTH = 100;
    public static final int NIGHT_LENGTH = 70;
    public static final int CYCLE_LENGTH = DAY_LENGTH + NIGHT_LENGTH;

    /** Story 3.1's safe tier (FR-9, AC-1/AC-2): eastness ≤ 0.2 is the enemy/supply-free home
     *  cluster — the "safe point" a foray leaves and returns to. The inclusive boundary line means
     *  a home-cluster landmark sitting exactly on it (the Graveyard center, x=19) is still safe. */
    public static final float SAFE_TIER_EASTNESS = 0.2f;
    /** Story 3.3 (Decision 1): a built campfire's safe radius (Manhattan) — standing within it is
     *  "at camp", not on a foray (the camp case beside the home cluster). Tunable content (PRD §8). */
    public static final int CAMPFIRE_SAFE_RADIUS = 5;
    /** Story 3.3 (FR-10, AC-2): the day/night boundary log lines. "Dusk falls" announces the end of
     *  the day's foray budget (the DAY→NIGHT flip); "Dawn breaks." opens a new day's budget. The day
     *  is the planning unit (UJ-2) — the clock's flip is the game's rhythm, emitted by the acted
     *  pipeline like the Weather onsetLine (Story 1.3's pattern; AD-4). */
    public static final String LINE_DUSK = "Dusk falls — the forest grows close.";
    public static final String LINE_DAWN = "Dawn breaks.";

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
    // Campfire fire-station (FR-6/FR-7, Story 1.5): the tile of a built campfire, or -1 = none.
    // It is the fire that cooking/boiling require (player must be at/adjacent). Distinct from the
    // light tile so a torch (1.6, carried light) never counts as a cooking fire. Field-initialized
    // (AD-6). WARMTH is Story 1.6 — the campfire here only cooks/purifies/lights/is-exposed.
    private int campfireX = -1;
    private int campfireY = -1;
    // Food-spoilage clock (FR-6, Story 1.5): advances on acted turns like clockTurns; every
    // SPOIL_INTERVAL a SpoilageSystem advances perishable food one stage. Field-initialized (AD-6).
    private int spoilageClock = 0;
    private int spoilageProgress = 0; // accrued spoilage (salt-aware); drives the advance, M2-review
    // Torch burn (FR-7, Story 1.6): acted turns of carried light remaining, 0 = none. Set by
    // lightTorch() (the craft) and burned down by tickTorch() on the acted path; on expiry the
    // light reverts to the campfire (a stationary source) or goes dark. Field-initialized (AD-6)
    // so a pre-1.6 save loads at 0 (no torch).
    private int torchTurns = 0;
    // Authored structure loot placed (Story 3.2, review fix): true once placeStructureLoot has run
    // for this floor. Persisted so a 3.2+ save reloads WITHOUT re-scattering (no double loot).
    // AD-6 migration: field-absent (pre-3.2) saves load FALSE, so restoreAfterLoad backfills the
    // authored loot a 3.1-era save never got — structures + hazards without loot was the AC-2 gap.
    private boolean structureLootPlaced;
    private long seed;
    private transient Random rng;
    private boolean lastStandUsed;        // persisted: one reprieve per run (FR-16/17)
    private transient boolean lastStand;  // turn-scoped desperate flag set when the reprieve fires
    // Transient, regenerated each turn (not saved); field initializer keeps it non-null after a Json load.
    private transient List<NoiseEvent> noiseQueue = new ArrayList<>();
    // The bottom message log (NFR-3, AD-15, Story 1.8): the primary text surface, bounded and
    // session-scoped. Transient so it never enters the save graph (AD-6) — a reloaded run starts
    // fresh (seeded) via the ctor. Owned here (AD-3), appended by TurnEngine (AD-4), rendered by
    // the screen (AD-1). Field initializer keeps it non-null after a Json load.
    public static final int MESSAGE_LOG_CAP = 50;
    private transient List<String> messageLog = new ArrayList<>();

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
        seedMessageLog(); // the opening line, so the surface is never empty at first render
    }

    /** Builds the continuous region and places a fresh player and enemies (run start/restart). */
    public void generateFloor() {
        FloorResult result = FloorGenerator.generate(MAP_W, MAP_H, rng);
        tileMap = result.map;

        int startCx = result.roomCenters.get(0)[0];
        int startCy = result.roomCenters.get(0)[1];
        player = new RoguePlayer(startCx, startCy, tileMap);

        placeFloorActors(result, player.getTileX(), player.getTileY());
        // Story 3.2 (AC-2): scatter each structure's authored loot set inside its footprint, AFTER
        // the generic scatter so that pass's seeded draw sequence is untouched (AD-5). A generated
        // floor carries the authored loot, so the load-time backfill flag is set (review fix).
        placeStructureLoot(player.getTileX(), player.getTileY());
        structureLootPlaced = true;
    }

    /**
     * Build the region's enemies and scattered supplies, avoiding the
     * given tile (the player's). Extracted from {@link #generateFloor} so
     * actor placement stays separable from map generation. Both scale with
     * the map's east/west spine (AC-2): danger AND loot rise east, safety lies west.
     */
    private void placeFloorActors(FloorResult result, int avoidX, int avoidY) {
        WorldSpine spine = result.spine;
        enemies = new ArrayList<>();
        for (int i = 1; i < result.roomCenters.size(); i++) {
            int cx = result.roomCenters.get(i)[0];
            int cy = result.roomCenters.get(i)[1];
            // The enemy COUNT is a deterministic step of the region's eastness (Decision 4) — no
            // rng in the decision, only per-enemy position draws touch the seeded stream (AD-5).
            int count = enemyCountFor(spine.eastness(cx));
            for (int e = 0; e < count; e++) {
                int ex = cx + rng.nextInt(3) - 1;
                int ey = cy + rng.nextInt(3) - 1;
                if (tileMap.isWalkable(ex, ey) && !(ex == avoidX && ey == avoidY)) {
                    enemies.add(new RogueEnemy(ex, ey, tileMap));
                }
            }
        }

        // Scatter supplies weighted eastward, drawn from the seeded RNG so a seed reproduces the
        // floor's items (AD-5). Cleared here so each generated floor starts fresh; a loaded run
        // keeps its saved floorItems (no regen).
        floorItems.clear();
        if (result.roomCenters.size() > 1) {
            // Review M1: draw only scatterable supplies — never the quest-seed Torn Page, which
            // must appear only via its scripted capture placement. One draw per placed item, so
            // the seeded stream's call structure is unchanged (AD-5).
            int[] scatter = Supply.scatterableOrdinals();
            for (int i = 1; i < result.roomCenters.size(); i++) {
                int cx = result.roomCenters.get(i)[0];
                int cy = result.roomCenters.get(i)[1];
                int want = supplyCountFor(spine.eastness(cx));
                for (int k = 0; k < want; k++) {
                    int ix = cx + rng.nextInt(3) - 1;
                    int iy = cy + rng.nextInt(3) - 1;
                    if (tileMap.isWalkable(ix, iy) && !(ix == avoidX && iy == avoidY)) {
                        floorItems.add(new FloorItem(scatter[rng.nextInt(scatter.length)], 1, ix, iy));
                    }
                }
            }
        }
    }

    /**
     * Story 3.2 (FR-10, AC-2): scatter each structure's authored loot set inside its footprint —
     * structures are destinations, so their loot is authored content (StructureTable), not the
     * generic eastness scatter. Runs AFTER {@link #placeFloorActors}, so the generic pass's seeded
     * draw sequence is byte-identical to the 3.1 baseline and structure loot is a stable suffix of
     * the stream (AD-5): one draw per chance entry, one per placed item. Loot lands only on
     * walkable footprint cells (never walls/furniture) and never on the player's tile. The locked
     * cellar's rich loot is data in the table only — Story 3.5's lockpicking exposes it, never this.
     */
    private void placeStructureLoot(int avoidX, int avoidY) {
        for (StructureTable.Structure structure : StructureTable.all()) {
            int[] box = structureFootprint(tileMap, structure.structureType);
            if (box == null) continue; // every structure is stamped, but stay defensive
            List<int[]> cells = new ArrayList<>();
            for (int x = box[0]; x <= box[2]; x++) {
                for (int y = box[1]; y <= box[3]; y++) {
                    if (tileMap.isWalkable(x, y) && !(x == avoidX && y == avoidY)) {
                        cells.add(new int[]{x, y});
                    }
                }
            }
            if (cells.isEmpty()) continue;
            for (StructureTable.LootEntry entry : structure.loot) {
                // Guaranteed entries skip the roll; chance entries draw exactly once (AD-5).
                boolean hit = entry.chancePercent >= 100 || rng.nextInt(100) < entry.chancePercent;
                if (!hit) continue;
                for (int k = 0; k < entry.count; k++) {
                    int[] c = cells.get(rng.nextInt(cells.size()));
                    floorItems.add(new FloorItem(entry.supply.ordinal(), 1, c[0], c[1]));
                }
            }
        }
    }

    /** The bounding box of a structure's stamped footprint, or null if the type isn't on the map. */
    private static int[] structureFootprint(RogueTileMap m, int type) {
        int minX = m.getWidth(), minY = m.getHeight(), maxX = -1, maxY = -1;
        for (int x = 0; x < m.getWidth(); x++) {
            for (int y = 0; y < m.getHeight(); y++) {
                if (m.getStructureType(x, y) == type) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        return maxX < 0 ? null : new int[]{minX, minY, maxX, maxY};
    }

    /** Enemy count per region: 0 in the safe west (near Corneo), rising to 3 in the eastern
     *  interior — the invasion's gradient (AC-2). The safe tier includes the 0.2f boundary line
     *  itself, so a home-cluster landmark that sits exactly on it (the Graveyard center, x=19) is
     *  safe. Pure function of eastness (Decision 4). */
    private static int enemyCountFor(float eastness) {
        if (eastness <= 0.2f) return 0;
        if (eastness < 0.45f) return 1;
        if (eastness < 0.7f) return 2;
        return 3;
    }

    /** Supply count per region: 0 in the safe west, 1 mid-map, 2 in the east — loot rises east
     *  with the danger (AC-2). Same inclusive 0.2f boundary as {@link #enemyCountFor}. Pure
     *  function of eastness (Decision 4). */
    private static int supplyCountFor(float eastness) {
        if (eastness <= 0.2f) return 0;
        if (eastness < 0.5f) return 1;
        return 2;
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
        // The message log is transient, so a loaded run's log content must not depend on which ctor
        // ran during Json load (review finding) — a resumed run opens with the same seeded line a
        // restarted one does.
        seedMessageLog();
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
        if (identifyMap != null) identifyMap.reconcile(Supply.count()); // grow a pre-1.5 save's shorter binding
        // Story 3.2 review fix: a pre-3.2 save (no structureLootPlaced key → false) restored the
        // structureTypes layer but never got the authored loot pass — structures + hazards, no loot
        // (AC-2 gap). Backfill ONCE: only when the typed structure layer exists (a legacy
        // pre-structureTypes save has none — its cells are the getStructureType OLD_HOUSE shim, not
        // real structures, so scattering would be wrong). The flag is persisted, so a 3.2+ save
        // skips; the backfill draws from the rebuilt seed (AD-5: deterministic per loaded seed).
        if (!structureLootPlaced && tileMap.hasStructureTypeLayer()) {
            placeStructureLoot(player.getTileX(), player.getTileY());
            structureLootPlaced = true;
        }
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
        clearCampfire(); // and no campfire built (Story 1.5)
        this.spoilageClock = 0;
        this.spoilageProgress = 0;
        this.torchTurns = 0; // and no torch lit (Story 1.6)
        generateFloor();
        spawnStartingCompanion();
        rollWeather(); // and rolls its own weather
        seedMessageLog(); // a new run clears the old log and re-opens with the opening line
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

    /** Walkable tile near (x,y): starts a couple tiles out (his rear-guard station distance), then
     *  widening rings. He follows to his station from there, so he needn't begin glued to the player. */
    private int[] companionSpotNear(int x, int y) {
        for (int r = 2; r < 10; r++) {
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

    /** Whether a LIVING enemy stands on (x,y) — the player-move occupancy gate (combat fix #2):
     *  a tile held by an enemy is not a valid destination, so nobody shares a tile. Enemies only
     *  (the companion is an ally the player may walk over); the player's own tile is a caller's
     *  origin, never a destination here. */
    public boolean isOccupiedByEnemy(int x, int y) {
        for (RogueEnemy e : enemies) {
            if (e.isAlive() && e.getTileX() == x && e.getTileY() == y) return true;
        }
        return false;
    }

    /** The party (AD-10): at most one companion in MVP. */
    public List<Companion> getCompanions() { return companions; }

    /** The single active companion, or null if the party is empty (AD-10). */
    public Companion getActiveCompanion() {
        return companions.isEmpty() ? null : companions.get(0);
    }

    /** Remove the single party slot (AD-10) — Aldric's capture (Story 2.4, FR-3): he leaves by
     *  capture, not death, so the party empties (Klein escapes alone) rather than gaining a
     *  corpse. The flag records the fact; follow/distract no-op via their existing null-checks. */
    public void removeActiveCompanion() {
        if (!companions.isEmpty()) companions.remove(0);
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

    /** The bottom message log (NFR-3, Story 1.8): the primary text surface, in emission order.
     *  Transient and bounded — the screen renders the last lines; an unmodifiable view so the log
     *  has exactly one writer (appendMessages, called by TurnEngine — AD-4). */
    public List<String> getMessageLog() { return Collections.unmodifiableList(messageLog); }

    /** Append messages to the log, trimming past the cap (oldest first). The log's sole mutator —
     *  called by TurnEngine (AD-4) for acted AND refused turns, by DialogController for dialogue
     *  effect lines (Story 2.1 — a suspended surface, not a turn, so AD-4's acted branch is untouched),
     *  and by TutorialController for Aldric's diegetic coaching lines (Story 2.3 — a passive observer
     *  that runs after a committed turn and writes only log lines, never a turn). */
    public void appendMessages(List<String> messages) {
        messageLog.addAll(messages);
        while (messageLog.size() > MESSAGE_LOG_CAP) messageLog.remove(0);
    }

    /** Clear the log and open it with the story's opening line (run start / restart). */
    private void seedMessageLog() {
        messageLog.clear();
        messageLog.add("You flee into the pines. Aldric is beside you.");
    }

    /** Elapsed turns on the Day/Night clock (FR-4). */
    public int getClockTurns() { return clockTurns; }

    /** Current Day/Night phase, derived from the clock (FR-5): Day while clockTurns%170 < 100. */
    public DayPhase getClockPhase() {
        return clockTurns % CYCLE_LENGTH < DAY_LENGTH ? DayPhase.DAY : DayPhase.NIGHT;
    }

    public boolean isDay() { return getClockPhase() == DayPhase.DAY; }

    /** Whether Klein is out on a foray (Story 3.3, FR-10): away from every safe point — outside the
     *  home-cluster safe tier (eastness ≤ {@link #SAFE_TIER_EASTNESS}, Story 3.1) AND farther than
     *  {@link #CAMPFIRE_SAFE_RADIUS} from any built campfire. Derived from position + constants
     *  (AD-6 — no new persisted field; the WorldSpine/DayPhase precedent). The screen renders it;
     *  no system mutates it. */
    public boolean onForay() {
        WorldSpine spine = new WorldSpine(tileMap.getWidth(), tileMap.getHeight());
        if (spine.eastness(player.getTileX()) <= SAFE_TIER_EASTNESS) return false;
        if (isPlayerAtCampfireSafePoint()) return false;
        return true;
    }

    /** Whether the player stands within a built campfire's safe radius (Manhattan ≤
     *  {@link #CAMPFIRE_SAFE_RADIUS}) — the "at camp" test (Story 3.3, Decision 1). SHARED by
     *  {@link #onForay()} (the safe-point framing) and {@code HazardSystem}'s night-overlay light
     *  suppression (review fix): a single source of truth so the camp's safety radius and its
     *  night-light protection radius can never diverge. Distinct from {@link #isPlayerAtFire()},
     *  which is the tight cooking/boiling adjacency (≤ 1), not the camp's safe extent. */
    public boolean isPlayerAtCampfireSafePoint() {
        return hasCampfire()
                && Math.abs(player.getTileX() - campfireX) + Math.abs(player.getTileY() - campfireY)
                        <= CAMPFIRE_SAFE_RADIUS;
    }

    /** Turns remaining in the current Day phase (Story 3.3, AC-2): the foray budget — "make it home
     *  before dark" (UJ-2). 0 during Night (the budget is spent). Derived from the clock (AD-6);
     *  never persisted. */
    public int turnsUntilNightfall() {
        int intoCycle = clockTurns % CYCLE_LENGTH;
        return intoCycle < DAY_LENGTH ? DAY_LENGTH - intoCycle : 0;
    }

    /** Which 0-based day/night cycle the run is in (Story 3.3, AC-2): the "day is the planning
     *  unit" count (UJ-2). {@code clockTurns / CYCLE_LENGTH} — derived, never persisted (AD-6).
     *  Distinct from {@link #getCycleNumber()} (the weather-roll cycle, which only advances at a
     *  boundary); this is the raw derived day count. */
    public int dayNumber() {
        return clockTurns / CYCLE_LENGTH;
    }

    /** The weather in effect this cycle (FR-5). Effects live in later stories (1.4/1.5/1.6/3.x). */
    public Weather getWeather() { return weather; }

    /** Test/development hook (precedent: {@code RoguePlayer#setSkill}): pin the cycle's weather.
     *  Production never calls this — {@link #rollWeather()} owns the weather (AD-5). Tests use it
     *  to isolate a driver (Story 1.6 pins CLEAR / COLD_SNAP for seed-independent drivers). */
    public void setWeather(Weather w) { weather = w; }

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

    /** Whether a campfire is built (a fire station for cooking/boiling, FR-6). */
    public boolean hasCampfire() { return campfireX >= 0 && campfireY >= 0; }

    public int getCampfireX() { return campfireX; }
    public int getCampfireY() { return campfireY; }

    /** Build the campfire at a tile (Story 1.5). Also lights it (AD-18: FOV restore + exposure). */
    public void setCampfire(int x, int y) { campfireX = x; campfireY = y; }

    /** Extinguish the campfire (none). */
    public void clearCampfire() { campfireX = -1; campfireY = -1; }

    /** True when the player is on or 4-adjacent to the campfire — the range for cooking/boiling. */
    public boolean isPlayerAtFire() {
        if (!hasCampfire()) return false;
        int dx = Math.abs(player.getTileX() - campfireX);
        int dy = Math.abs(player.getTileY() - campfireY);
        return dx + dy <= 1;
    }

    /** The carried light's remaining burn (acted turns), 0 = none (FR-7, Story 1.6). */
    public int getTorchTurns() { return torchTurns; }

    /** Light the torch (Story 1.6 craft): the burn countdown starts and the light is set to the
     *  player's tile. The single-slot light model (AD-18) means the torch takes over from any
     *  campfire light; on expiry it reverts (see {@link #tickTorch}). A burn of ≤ 0 is refused —
     *  a lit light must always have a burn, else {@code tickTorch}'s early-return would leave it
     *  lit forever (review guard). */
    public void lightTorch(int burnTurns) {
        if (burnTurns <= 0) return;
        torchTurns = burnTurns;
        setLight(player.getTileX(), player.getTileY());
    }

    /** One acted turn with a torch lit: burn it down. The light follows the player each turn; on
     *  expiry it reverts to the campfire (a stationary source) or goes dark — never a stale torch
     *  light at a tile the player left. A no-op when no torch is lit. */
    public void tickTorch() {
        if (torchTurns <= 0) return;
        torchTurns--;
        if (torchTurns > 0) {
            setLight(player.getTileX(), player.getTileY());
        } else if (hasCampfire()) {
            setLight(campfireX, campfireY);
        } else {
            clearLight();
        }
    }

    /** The food-spoilage clock (FR-6): elapsed acted turns driving {@code SpoilageSystem}. */
    public int getSpoilageClock() { return spoilageClock; }

    /** Advance the spoilage clock one acted turn (called from the acted branch, AD-5). */
    public void tickSpoilageClock() { spoilageClock++; }

    /** The accrued spoilage (salt-aware, M2-review): the mechanism behind the clock. */
    public int getSpoilageProgress() { return spoilageProgress; }
    public void addSpoilageProgress(int amount) { spoilageProgress += amount; }
    public void subtractSpoilageProgress(int amount) { spoilageProgress -= amount; }

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

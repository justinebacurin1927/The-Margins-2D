package com.margins.rogue.state;

import com.margins.rogue.Companion;
import com.margins.rogue.FloorGenerator;
import com.margins.rogue.FloorGenerator.FloorResult;
import com.margins.rogue.NoiseEvent;
import com.margins.rogue.RogueEnemy;
import com.margins.rogue.RoguePlayer;
import com.margins.rogue.RogueTileMap;
import com.margins.rogue.item.FloorItem;
import com.margins.rogue.item.Inventory;
import com.margins.rogue.item.Supply;
import com.margins.rogue.world.Route;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Single owner of all run data (AD-3): tilemap, player, enemies, current floor,
 * seed and the seeded RNG. Systems mutate this; nothing else holds an
 * authoritative duplicate. This is the unit that will be serialized for save
 * (AD-6). Contains NO libGDX rendering types (AD-2) so it stays headless-testable.
 */
public class RunState {

    private static final int MAP_W = 50;
    private static final int MAP_H = 50;

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
    // The route this run descends (FR-18). A constant singleton, so it's transient
    // and field-initialized. Json never serializes a transient field, and fromJson
    // invokes the no-arg constructor (which runs field initializers), so a load always
    // re-supplies this default — pre-6.1 saves load the Caravan Road (AD-6).
    private transient Route route = Route.CARAVAN_ROAD;
    private int floorDepth;
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
        this.floorDepth = 1;
        this.identifyMap = IdentifyMap.build(rng); // bind supply identities at run start (FR-11, AD-12)
        generateFloor();
        spawnStartingCompanion();
    }

    /** Builds the current floor and places a fresh player and enemies (run start/restart). */
    public void generateFloor() {
        FloorResult result = FloorGenerator.generate(MAP_W, MAP_H, rng, floorDepth);
        tileMap = result.map;

        int startCx = result.roomCenters.get(0)[0];
        int startCy = result.roomCenters.get(0)[1];
        player = new RoguePlayer(startCx, startCy, tileMap);

        placeFloorActors(result, player.getTileX(), player.getTileY());
    }

    /**
     * Build the current floor's enemies and scattered supplies, avoiding the
     * given tile (the player's). Extracted from {@link #generateFloor} so
     * {@link #descend} can rebuild a floor without recreating the player (AC-3).
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
     * One-way descent (AC-2/3): advance {@code floorDepth}, rebuild the floor
     * with fresh enemies/items, and move the existing player + companion to the
     * new entrance — never {@code new RoguePlayer(...)}, so HP/hunger/inventory
     * survive. Per-floor view state is rebuilt by {@code FovSystem.compute}
     * (TurnEngine calls it on the descent turn). Bounded by the route (FR-18):
     * returns {@code false} and mutates nothing once the route's last floor is
     * reached — the "road ends" seam that Stories 6.2 (authored Story Floor)
     * and 6.5 (route completion) build on.
     */
    public boolean descend() {
        if (floorDepth >= route.getFloorCount()) {
            return false; // the route ends — no floor beyond its last
        }
        floorDepth++;
        FloorResult result = FloorGenerator.generate(MAP_W, MAP_H, rng, floorDepth);
        tileMap = result.map;

        int startCx = result.roomCenters.get(0)[0];
        int startCy = result.roomCenters.get(0)[1];
        player.placeAt(startCx, startCy);
        player.setMap(tileMap);

        placeFloorActors(result, startCx, startCy);

        Companion c = getActiveCompanion();
        if (c != null) {
            int[] spot = companionSpotNear(startCx, startCy);
            c.placeAt(spot[0], spot[1]);
            c.setMap(tileMap);
            c.resetDistractions(); // fresh floor, fresh shouts (FR-14)
        }
        return true;
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
        // identifyMap is a persisted field — it loads with the run, so the resumed
        // run keeps its per-seed binding (verified by round-trip); no rebuild needed.
        player.setMap(tileMap);
        for (RogueEnemy e : enemies) {
            e.setMap(tileMap);
        }
        for (Companion c : companions) {
            c.setMap(tileMap);
        }
    }

    /** Restart a fresh run from floor 1 (same seeded RNG stream continues). */
    public void restart() {
        this.floorDepth = 1;
        this.lastStandUsed = false;
        this.lastStand = false;
        this.identifyMap = IdentifyMap.build(rng); // a new run rebinds identities (FR-11)
        this.flagStore = new FlagStore(); // narrative state is run-scoped (AD-7): a new run resets flags + Bond
        generateFloor();
        spawnStartingCompanion();
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
    public int getFloorDepth() { return floorDepth; }
    public void setFloorDepth(int floorDepth) { this.floorDepth = floorDepth; }

    /** The route this run descends (FR-18) — its name, floor count, and end message. */
    public Route getRoute() { return route; }

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

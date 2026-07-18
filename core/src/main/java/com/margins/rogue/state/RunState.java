package com.margins.rogue.state;

import com.margins.rogue.FloorGenerator;
import com.margins.rogue.FloorGenerator.FloorResult;
import com.margins.rogue.NoiseEvent;
import com.margins.rogue.RogueEnemy;
import com.margins.rogue.RoguePlayer;
import com.margins.rogue.RogueTileMap;
import com.margins.rogue.item.FloorItem;
import com.margins.rogue.item.Inventory;
import com.margins.rogue.item.Supply;

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

    public RunState(long seed) {
        this.seed = seed;
        this.rng = new Random(seed);
        this.floorDepth = 1;
        generateFloor();
    }

    /** Builds the current floor and places the player and enemies. */
    public void generateFloor() {
        FloorResult result = FloorGenerator.generate(MAP_W, MAP_H, rng, floorDepth);
        tileMap = result.map;

        int startCx = result.roomCenters.get(0)[0];
        int startCy = result.roomCenters.get(0)[1];
        player = new RoguePlayer(startCx, startCy, tileMap);

        enemies = new ArrayList<>();
        for (int i = 1; i < result.roomCenters.size(); i++) {
            int cx = result.roomCenters.get(i)[0];
            int cy = result.roomCenters.get(i)[1];
            int count = 1 + rng.nextInt(2);
            for (int e = 0; e < count; e++) {
                int ex = cx + rng.nextInt(3) - 1;
                int ey = cy + rng.nextInt(3) - 1;
                if (tileMap.isWalkable(ex, ey)
                        && !(ex == player.getTileX() && ey == player.getTileY())) {
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
                if (tileMap.isWalkable(ix, iy)
                        && !(ix == player.getTileX() && iy == player.getTileY())) {
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
        player.setMap(tileMap);
        for (RogueEnemy e : enemies) {
            e.setMap(tileMap);
        }
    }

    /** Restart a fresh run from floor 1 (same seeded RNG stream continues). */
    public void restart() {
        this.floorDepth = 1;
        this.lastStandUsed = false;
        this.lastStand = false;
        generateFloor();
    }

    public RogueTileMap getTileMap() { return tileMap; }
    public RoguePlayer getPlayer() { return player; }
    public List<RogueEnemy> getEnemies() { return enemies; }

    /** The finite carry container (FR-9): plain int arrays, so it saves/loads under this root for free (AD-6). */
    public Inventory getInventory() { return inventory; }

    /** Items lying on floor tiles (FR-10). */
    public List<FloorItem> getFloorItems() { return floorItems; }

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

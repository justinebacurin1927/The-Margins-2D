package com.margins.rogue.state;

import com.margins.rogue.FloorGenerator;
import com.margins.rogue.FloorGenerator.FloorResult;
import com.margins.rogue.RogueEnemy;
import com.margins.rogue.RoguePlayer;
import com.margins.rogue.RogueTileMap;

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
    private int floorDepth;
    private long seed;
    private transient Random rng;

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
        generateFloor();
    }

    public RogueTileMap getTileMap() { return tileMap; }
    public RoguePlayer getPlayer() { return player; }
    public List<RogueEnemy> getEnemies() { return enemies; }
    public int getFloorDepth() { return floorDepth; }
    public void setFloorDepth(int floorDepth) { this.floorDepth = floorDepth; }
    public long getSeed() { return seed; }

    /** The single seeded RNG all gameplay randomness should draw from (AD-5). */
    public Random rng() { return rng; }
}

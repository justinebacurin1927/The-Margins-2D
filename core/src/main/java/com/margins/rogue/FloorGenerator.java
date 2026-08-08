package com.margins.rogue;

import com.margins.rogue.world.WorldSpine;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Story 3.1 (FR-9, AC-1/AC-2): the hybrid generator — fixed canon landmarks stitched with
 * procedural wilderness on one continuous region (AD-8). The landmark skeleton is AUTHORED
 * (Decision 1: Corneo's home cluster, the Copper Road, the NW border crossing, the Watchtower
 * sit at fixed positions derived from {@link WorldSpine}); only the wilderness varies per seed.
 * Connectivity is a generator guarantee (O4 carry): a flood-fill reachability pass runs after
 * carving and repairs any landmark cut off, so start/Corneo ↔ road ↔ Watchtower ↔ border stay
 * one component. Danger rises east (AC-2); {@link FloorResult#spine} exposes the gradient to
 * actor placement.
 */
public class FloorGenerator {
    private static class Room {
        int x, y, w, h;
        Room(int x, int y, int w, int h) {
            this.x = x; this.y = y; this.w = w; this.h = h;
        }
        int cx() { return x + w / 2; }
        int cy() { return y + h / 2; }
    }

    private static final int MIN_ROOM_SIZE = 5;
    private static final int MAX_ROOM_SIZE = 10;
    private static final int MAX_ROOMS = 9; // one continuous region (AD-8); no per-floor depth scaling
    private static final int MAX_CORRIDOR_DOORS = 0;
    private static final float DOOR_CHANCE = 0f;
    /** Thirteen-by-eight architecture plus a walkable one-cell exterior foundation apron. */
    private static final int OLD_HOUSE_INSET = 1;
    private static final int OLD_HOUSE_BODY_W = 13;
    private static final int OLD_HOUSE_BODY_H = 8;
    private static final int OLD_HOUSE_W = OLD_HOUSE_BODY_W + OLD_HOUSE_INSET * 2;
    private static final int OLD_HOUSE_H = OLD_HOUSE_BODY_H + OLD_HOUSE_INSET * 2;
    private static final int OLD_HOUSE_DOOR_X = OLD_HOUSE_INSET + 7;
    /** Nine-by-seven grave enclosure plus its own one-cell forest transition apron. */
    private static final int GRAVEYARD_INSET = 1;
    private static final int GRAVEYARD_BODY_W = 9;
    private static final int GRAVEYARD_BODY_H = 7;
    private static final int GRAVEYARD_W = GRAVEYARD_BODY_W + GRAVEYARD_INSET * 2;
    private static final int GRAVEYARD_H = GRAVEYARD_BODY_H + GRAVEYARD_INSET * 2;
    private static final int GRAVEYARD_GATE_X = GRAVEYARD_INSET + 4;
    /** The Corneo town plaza (the start room) — an authored 8×6 clearing on the road. */
    private static final int TOWN_X = 12, TOWN_Y = 21, TOWN_W = 8, TOWN_H = 6;
    /** The Old House's authored position — the Corneo outskirts, south-west of the plaza. */
    private static final int OLD_HOUSE_X = 5, OLD_HOUSE_Y = 10;
    /** The Graveyard's authored position — north of the plaza, toward the forest. */
    private static final int GRAVEYARD_X = 14, GRAVEYARD_Y = 30;
    /** The Watchtower's authored footprint: a solid 3×3 furniture block on the road's north shoulder. */
    private static final int TOWER_W = 3, TOWER_H = 3;
    /** The NW border crossing's authored footprint: a 3×3 clearing with a DOOR gate. */
    private static final int BORDER_W = 3, BORDER_H = 3;
    private static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    public static class FloorResult {
        public RogueTileMap map;
        public List<int[]> roomCenters;
        /** The authored geography this map was built from — placement reads the east/west gradient. */
        public WorldSpine spine;
        FloorResult(RogueTileMap map, List<int[]> roomCenters, WorldSpine spine) {
            this.map = map;
            this.roomCenters = roomCenters;
            this.spine = spine;
        }
    }

    public static FloorResult generate(int width, int height, Random rand) {
        RogueTileMap map = new RogueTileMap(width, height);
        map.fill(RogueTile.WALL);
        WorldSpine spine = new WorldSpine(width, height);

        List<Room> rooms = new ArrayList<>();
        List<Room> blocked = new ArrayList<>();

        // --- The authored landmark skeleton (Decision 1 — seed-independent, never varies per run).
        // The home cluster: Corneo's town plaza (the start room), the Old House, the Graveyard.
        Room town = new Room(TOWN_X, TOWN_Y, TOWN_W, TOWN_H);
        Room oldHouse = new Room(OLD_HOUSE_X, OLD_HOUSE_Y, OLD_HOUSE_W, OLD_HOUSE_H);
        Room graveyard = new Room(GRAVEYARD_X, GRAVEYARD_Y, GRAVEYARD_W, GRAVEYARD_H);
        rooms.add(town);       blocked.add(town);
        rooms.add(oldHouse);   blocked.add(oldHouse);
        rooms.add(graveyard);  blocked.add(graveyard);
        for (Room r : rooms) carveRoom(map, r);

        // The Copper Road: a 3-wide floor corridor along the road row, spanning the map. Corneo
        // sits on it (AC-2: the invasion came down this road from the east).
        for (int x = spine.roadStartX(); x <= spine.roadEndX(); x++) {
            for (int dy = -1; dy <= 1; dy++) {
                map.setTile(x, spine.roadY() + dy, RogueTile.FLOOR);
            }
        }

        // The Watchtower: a solid furniture block on the road's north shoulder, east of Corneo.
        // Furniture blocks feet but not sight (RogueTile), reads as a landmark, and has no walkable
        // cell inside so it can never strand an island. Blocked so wilderness rooms avoid it.
        Room watchtower = new Room(spine.watchtowerX(), spine.watchtowerY() + 2, TOWER_W, TOWER_H);
        blocked.add(watchtower);
        for (int dx = 0; dx < watchtower.w; dx++) {
            for (int dy = 0; dy < watchtower.h; dy++) {
                map.setTile(watchtower.x + dx, watchtower.y + dy, RogueTile.FURNITURE);
            }
        }

        // The NW border crossing: a DOOR gate in the far-NW treeline with a small floor clearing
        // (an OPENING, not a wall — the border is always physically walkable, AD-12; the cordon
        // gauntlet mechanics are Story 5.7).
        Room border = new Room(spine.borderX() - 1, spine.borderY() - 1, BORDER_W, BORDER_H);
        blocked.add(border);
        for (int dx = 0; dx < border.w; dx++) {
            for (int dy = 0; dy < border.h; dy++) {
                map.setTile(border.x + dx, border.y + dy, RogueTile.FLOOR);
            }
        }
        map.setTile(spine.borderX(), spine.borderY(), RogueTile.DOOR);

        // --- The procedural wilderness (seed-dependent): random clearings between the landmarks.
        int attempts = 0;
        while (rooms.size() < MAX_ROOMS && attempts < 200) {
            attempts++;
            int rw = rand.nextInt(MAX_ROOM_SIZE - MIN_ROOM_SIZE + 1) + MIN_ROOM_SIZE;
            int rh = rand.nextInt(MAX_ROOM_SIZE - MIN_ROOM_SIZE + 1) + MIN_ROOM_SIZE;
            int rx = rand.nextInt(width - rw - 2) + 1;
            int ry = rand.nextInt(height - rh - 2) + 1;

            Room candidate = new Room(rx, ry, rw, rh);
            if (!overlapsAny(candidate, blocked)) {
                rooms.add(candidate);
                blocked.add(candidate);
                carveRoom(map, candidate);
            }
        }

        // Chain the rooms with corridors, start room first (room 0 = the town plaza), preserving
        // the Old House door and Graveyard gate as the corridor endpoints so the structure
        // entrances stay reachable once their atlases are stamped.
        Room h = rooms.size() > 1 ? rooms.get(1) : null;
        Room g = rooms.size() > 2 ? rooms.get(2) : null;
        int doorsPlaced = 0;
        for (int i = 1; i < rooms.size(); i++) {
            Room a = rooms.get(i - 1);
            Room b = rooms.get(i);
            int ax = a == h ? h.x + OLD_HOUSE_DOOR_X
                    : a == g ? g.x + GRAVEYARD_GATE_X : a.cx();
            int ay = a == h || a == g ? a.y : a.cy();
            int bx = b == h ? h.x + OLD_HOUSE_DOOR_X
                    : b == g ? g.x + GRAVEYARD_GATE_X : b.cx();
            int by = b == h || b == g ? b.y : b.cy();
            doorsPlaced += carveCorridor(map, ax, ay, bx, by, rand,
                    MAX_CORRIDOR_DOORS - doorsPlaced);
        }

        // Rooms and three-wide corridors can occasionally approach with exactly one wall cell
        // between them. As forest art that becomes a thin tendril or a hard one-tile divider,
        // rather than the broad treeline the map is meant to represent. Remove only wall cells
        // pinched between walkable space on opposite sides; ordinary room borders and forest
        // masses keep their thickness. Simultaneous passes continue until stable so stepped
        // one-cell slivers are removed without making the result depend on scan direction.
        smoothForestEdges(map);

        // Paint collision and an independent 15x10 visual layer after forest smoothing. Its
        // outermost cells are a walkable foundation apron around the 13x8 architecture. Rendering
        // slices the large room image per cell, so fog-of-war still reveals it cell by cell.
        if (h != null) stampOldHouse(map, h);
        if (g != null) stampGraveyard(map, g);

        // Water sources (FR-6, Story 1.5) re-anchored to the landmark skeleton (Decision 7): a
        // town well, a roadside pond, and the river at the road's east end. All land on walkable
        // landmark tiles and draw NOTHING from `rand`, so the seeded wilderness/actor stream is
        // untouched (AD-5).
        map.setTile(TOWN_X + 1, TOWN_Y + 1, RogueTile.WELL);   // the town well, by the plaza
        map.setTile(spine.roadEndX() - 5, spine.roadY(), RogueTile.POND);   // a roadside pond
        map.setTile(spine.roadEndX() - 2, spine.roadY(), RogueTile.RIVER);  // the road meets the river

        // Connectivity guarantee (O4 carry): the landmark skeleton must be one component from the
        // start. Runs AFTER the structure stamps — a corridor that crossed a structure's future
        // footprint is walled by the stamp, so only a post-stamp flood can see the seal. The
        // repair routes around stamped walls (exits a structure only through its door/gate), then
        // a final structure-aware smooth cleans any divider the repair corridors left.
        ensureReachable(map, town.cx(), town.cy(), landmarkTargets(spine, town, h, g));
        smoothForestEdges(map);

        List<int[]> centers = new ArrayList<>();
        for (Room r : rooms) {
            centers.add(new int[]{r.cx(), r.cy()});
        }

        return new FloorResult(map, centers, spine);
    }

    /** The landmark tiles every new run must keep reachable from the start (O4/AC-1). */
    private static int[][] landmarkTargets(WorldSpine spine, Room town, Room oldHouse, Room graveyard) {
        return new int[][]{
                {town.cx(), town.cy()},                                  // Corneo's plaza (the origin)
                {spine.roadStartX(), spine.roadY()},                     // the road's west end
                {spine.roadEndX(), spine.roadY()},                       // the road's east end
                {spine.borderX(), spine.borderY()},                      // the NW border gate
                {oldHouse.x + OLD_HOUSE_DOOR_X, oldHouse.y + 1},         // the Old House entrance
                {graveyard.x + GRAVEYARD_GATE_X, graveyard.y + 1},       // the Graveyard gate
        };
    }

    private static boolean overlapsAny(Room candidate, List<Room> blocked) {
        for (Room r : blocked) {
            if (overlaps(r, candidate, 2)) return true;
        }
        return false;
    }

    private static boolean overlaps(Room a, Room b, int padding) {
        return a.x - padding < b.x + b.w &&
               a.x + a.w + padding > b.x &&
               a.y - padding < b.y + b.h &&
               a.y + a.h + padding > b.y;
    }

    private static void carveRoom(RogueTileMap map, Room room) {
        for (int x = room.x; x < room.x + room.w; x++) {
            for (int y = room.y; y < room.y + room.h; y++) {
                map.setTile(x, y, RogueTile.FLOOR);
            }
        }
    }

    /**
     * Collision plan matching the roofless three-room Old House image. The outer atlas ring is a
     * visual-only, walkable foundation apron. The inset building perimeter is solid except for
     * its south entrance; both internal partitions have a walkable doorway.
     */
    private static void stampOldHouse(RogueTileMap map, Room room) {
        for (int dx = 0; dx < OLD_HOUSE_W; dx++) {
            for (int dy = 0; dy < OLD_HOUSE_H; dy++) {
                int x = room.x + dx;
                int y = room.y + dy;
                // Atlas row zero is the image's top row; map dy zero is its bottom row.
                int visualCell = (OLD_HOUSE_H - 1 - dy) * OLD_HOUSE_W + dx;
                map.setStructureTile(x, y, visualCell);

                int bx = dx - OLD_HOUSE_INSET;
                int by = dy - OLD_HOUSE_INSET;
                boolean apron = bx < 0 || bx >= OLD_HOUSE_BODY_W
                        || by < 0 || by >= OLD_HOUSE_BODY_H;
                boolean entrance = by == 0 && bx == 7;
                boolean perimeter = bx == 0 || bx == OLD_HOUSE_BODY_W - 1
                        || by == 0 || by == OLD_HOUSE_BODY_H - 1;
                boolean partition = (bx == 3 || bx == 9)
                        && by > 0 && by < OLD_HOUSE_BODY_H - 1 && by != 3;
                map.setTile(x, y, apron ? RogueTile.FLOOR
                        : entrance ? RogueTile.DOOR
                        : perimeter || partition ? RogueTile.WALL
                        : isOldHouseFurniture(bx, by) ? RogueTile.FURNITURE : RogueTile.FLOOR);
            }
        }
    }

    /** Collision cells beneath the large baked-in props; furniture blocks feet, never line of sight. */
    private static boolean isOldHouseFurniture(int x, int y) {
        // Kitchen: brick oven/hearth, west preparation counter, and south work table/sacks.
        if (y == 6 && (x == 1 || x == 2)) return true;
        if (x == 1 && y >= 2 && y <= 5) return true;
        if (y == 1 && (x == 1 || x == 2)) return true;

        // Main room: fireplace, crate, and the relocated broken table.  Keeping the table in
        // columns 6-7 leaves a full walkable cell on both sides of the internal doorways.
        if (y == 6 && (x == 4 || x == 5)) return true;
        if (x == 4 && (y == 4 || y == 5)) return true;
        if ((x == 6 || x == 7) && y >= 2 && y <= 4) return true;

        // Cellar: chest, open hatch, barrel, and stacked timber against the east wall.
        if (x == 10 && (y == 6 || y == 2)) return true;
        return x == 11 && (y == 1 || y == 2 || y == 3);
    }

    /** Outdoor grave enclosure: low fence and markers stop feet while remaining see-through. */
    private static void stampGraveyard(RogueTileMap map, Room room) {
        for (int dx = 0; dx < GRAVEYARD_W; dx++) {
            for (int dy = 0; dy < GRAVEYARD_H; dy++) {
                int x = room.x + dx;
                int y = room.y + dy;
                int visualCell = (GRAVEYARD_H - 1 - dy) * GRAVEYARD_W + dx;
                map.setStructureTile(x, y, RogueTileMap.STRUCTURE_GRAVEYARD, visualCell);

                int bx = dx - GRAVEYARD_INSET;
                int by = dy - GRAVEYARD_INSET;
                boolean apron = bx < 0 || bx >= GRAVEYARD_BODY_W
                        || by < 0 || by >= GRAVEYARD_BODY_H;
                boolean gate = by == 0 && bx == 4;
                boolean lowFence = bx == 0 || bx == GRAVEYARD_BODY_W - 1
                        || by == 0 || by == GRAVEYARD_BODY_H - 1;
                map.setTile(x, y, apron || gate ? RogueTile.FLOOR
                        : lowFence || isGraveMarker(bx, by)
                        ? RogueTile.FURNITURE : RogueTile.FLOOR);
            }
        }
    }

    /** Two paired marker clusters flanking the open north-south path. */
    private static boolean isGraveMarker(int x, int y) {
        boolean leftCluster = x >= 1 && x <= 2;
        boolean rightCluster = x >= 6 && x <= 7;
        boolean northPair = y >= 4 && y <= 5;
        boolean southPair = y >= 2 && y <= 3;
        return (leftCluster || rightCluster) && (northPair || southPair);
    }

    private static int carveCorridor(RogueTileMap map, int x1, int y1, int x2, int y2,
                                     Random rand, int doorsRemaining) {
        int cx = x1, cy = y1;
        int doorsPlaced = 0;
        while (cx != x2 || cy != y2) {
            if (cx < x2 && (rand.nextBoolean() || cy == y2)) cx++;
            else if (cx > x2 && (rand.nextBoolean() || cy == y2)) cx--;
            else if (cy < y2) cy++;
            else cy--;

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    int tx = cx + dx;
                    int ty = cy + dy;
                    if (map.isWalkable(tx, ty)) continue;
                    if (map.getTile(tx, ty) == RogueTile.WALL) {
                        // A door is a rare centerline gate, never random furniture scattered over
                        // the five cells of a corridor cross. The whole map is capped at two.
                        boolean isDoor = dx == 0 && dy == 0
                                && doorsPlaced < doorsRemaining
                                && rand.nextFloat() < DOOR_CHANCE;
                        map.setTile(tx, ty, isDoor ? RogueTile.DOOR : RogueTile.FLOOR);
                        if (isDoor) doorsPlaced++;
                    }
                }
            }
        }
        return doorsPlaced;
    }

    private static void smoothForestEdges(RogueTileMap map) {
        boolean changed;
        do {
            changed = false;
            boolean[][] carve = new boolean[map.getWidth()][map.getHeight()];
            for (int x = 1; x < map.getWidth() - 1; x++) {
                for (int y = 1; y < map.getHeight() - 1; y++) {
                    // Structure cells are exempt — their walls are authored collision, never a
                    // forest tendril to smooth (the same carve ContinuousMapTest applies).
                    if (map.getTile(x, y) != RogueTile.WALL || map.getStructureTile(x, y) >= 0) continue;
                    boolean north = map.isWalkable(x, y + 1);
                    boolean east = map.isWalkable(x + 1, y);
                    boolean south = map.isWalkable(x, y - 1);
                    boolean west = map.isWalkable(x - 1, y);
                    carve[x][y] = (north && south) || (east && west);
                }
            }
            for (int x = 1; x < map.getWidth() - 1; x++) {
                for (int y = 1; y < map.getHeight() - 1; y++) {
                    if (carve[x][y]) {
                        map.setTile(x, y, RogueTile.FLOOR);
                        changed = true;
                    }
                }
            }
        } while (changed);
    }

    /** Flood-fill walkable reachability from (sx,sy). */
    private static boolean[][] floodFillReachable(RogueTileMap map, int sx, int sy) {
        boolean[][] reached = new boolean[map.getWidth()][map.getHeight()];
        if (!map.isWalkable(sx, sy)) return reached;
        ArrayDeque<int[]> stack = new ArrayDeque<>();
        reached[sx][sy] = true;
        stack.push(new int[]{sx, sy});
        while (!stack.isEmpty()) {
            int[] cur = stack.pop();
            for (int[] d : DIRS) {
                int nx = cur[0] + d[0], ny = cur[1] + d[1];
                if (map.isWalkable(nx, ny) && !reached[nx][ny]) {
                    reached[nx][ny] = true;
                    stack.push(new int[]{nx, ny});
                }
            }
        }
        return reached;
    }

    /**
     * Connectivity guarantee (O4 carry): every authored landmark must be reachable from the start.
     * Any landmark a corridor missed gets a repair corridor carved from the nearest reached tile.
     */
    private static void ensureReachable(RogueTileMap map, int sx, int sy, int[][] targets) {
        boolean[][] reached = floodFillReachable(map, sx, sy);
        for (int[] t : targets) {
            if (reached[t[0]][t[1]]) continue;
            carvePathTo(map, reached, t[0], t[1]);
            reached = floodFillReachable(map, sx, sy);
        }
    }

    /**
     * BFS from an unreached landmark through the forest to the nearest reached walkable tile, then
     * carve a 3-wide corridor back. Runs post-stamp, so it must never punch through a stamped
     * structure's walls: the search refuses to route through a structure wall, forcing the path to
     * exit a sealed structure only through its walkable door/gate/apron — and the carve converts
     * only plain WALL→FLOOR, so a landmark, a door, a structure, or a water source is never
     * destroyed.
     */
    private static void carvePathTo(RogueTileMap map, boolean[][] reached, int tx, int ty) {
        int w = map.getWidth(), h = map.getHeight();
        boolean[][] seen = new boolean[w][h];
        int[][][] parent = new int[w][h][];
        ArrayDeque<int[]> q = new ArrayDeque<>();
        seen[tx][ty] = true;
        q.add(new int[]{tx, ty});
        int[] goal = null;
        while (!q.isEmpty() && goal == null) {
            int[] cur = q.poll();
            if (reached[cur[0]][cur[1]]) {
                goal = cur;
                break;
            }
            for (int[] d : DIRS) {
                int nx = cur[0] + d[0], ny = cur[1] + d[1];
                if (nx < 0 || nx >= w || ny < 0 || ny >= h) continue;
                if (seen[nx][ny]) continue;
                if (map.getTile(nx, ny) == RogueTile.WALL && map.getStructureTile(nx, ny) >= 0) {
                    continue; // never route a repair through a stamped structure's wall
                }
                seen[nx][ny] = true;
                parent[nx][ny] = cur;
                q.add(new int[]{nx, ny});
            }
        }
        if (goal == null) return; // fully enclosed — cannot happen on this open forest
        for (int[] cur = goal; cur != null; cur = parent[cur[0]][cur[1]]) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    int x = cur[0] + dx, y = cur[1] + dy;
                    if (map.getTile(x, y) == RogueTile.WALL && map.getStructureTile(x, y) < 0) {
                        map.setTile(x, y, RogueTile.FLOOR);
                    }
                }
            }
        }
    }
}

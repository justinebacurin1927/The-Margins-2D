package com.margins.rogue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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

    public static class FloorResult {
        public RogueTileMap map;
        public List<int[]> roomCenters;
        FloorResult(RogueTileMap map, List<int[]> roomCenters) {
            this.map = map;
            this.roomCenters = roomCenters;
        }
    }

    public static FloorResult generate(int width, int height, Random rand) {
        RogueTileMap map = new RogueTileMap(width, height);
        map.fill(RogueTile.WALL);

        List<Room> rooms = new ArrayList<>();
        int attempts = 0;

        while (rooms.size() < MAX_ROOMS && attempts < 50) {
            attempts++;
            boolean placingOldHouse = rooms.size() == 1;
            boolean placingGraveyard = rooms.size() == 2;
            int rw = placingOldHouse ? OLD_HOUSE_W
                    : placingGraveyard ? GRAVEYARD_W
                    : rand.nextInt(MAX_ROOM_SIZE - MIN_ROOM_SIZE + 1) + MIN_ROOM_SIZE;
            int rh = placingOldHouse ? OLD_HOUSE_H
                    : placingGraveyard ? GRAVEYARD_H
                    : rand.nextInt(MAX_ROOM_SIZE - MIN_ROOM_SIZE + 1) + MIN_ROOM_SIZE;
            int rx = rand.nextInt(width - rw - 2) + 1;
            int ry = rand.nextInt(height - rh - 2) + 1;

            Room candidate = new Room(rx, ry, rw, rh);
            boolean overlaps = false;
            for (Room r : rooms) {
                if (overlaps(r, candidate, 2)) {
                    overlaps = true;
                    break;
                }
            }
            if (!overlaps) {
                rooms.add(candidate);
                carveRoom(map, candidate);
            }
        }

        // Guarantee a non-empty region: the first placement always succeeds (nothing to
        // overlap), so this is currently unreachable — but callers index roomCenters.get(0),
        // so enforce the "at least one room" invariant rather than depend on that subtlety.
        if (rooms.isEmpty()) {
            Room r = new Room((width - MIN_ROOM_SIZE) / 2, (height - MIN_ROOM_SIZE) / 2,
                              MIN_ROOM_SIZE, MIN_ROOM_SIZE);
            rooms.add(r);
            carveRoom(map, r);
        }

        Room oldHouse = rooms.size() > 1 ? rooms.get(1) : null;
        Room graveyard = rooms.size() > 2 ? rooms.get(2) : null;
        int doorsPlaced = 0;
        for (int i = 1; i < rooms.size(); i++) {
            Room a = rooms.get(i - 1);
            Room b = rooms.get(i);
            int ax = a == oldHouse ? oldHouse.x + OLD_HOUSE_DOOR_X
                    : a == graveyard ? graveyard.x + GRAVEYARD_GATE_X : a.cx();
            int ay = a == oldHouse || a == graveyard ? a.y : a.cy();
            int bx = b == oldHouse ? oldHouse.x + OLD_HOUSE_DOOR_X
                    : b == graveyard ? graveyard.x + GRAVEYARD_GATE_X : b.cx();
            int by = b == oldHouse || b == graveyard ? b.y : b.cy();
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
        // slices the large room image per tile, so fog-of-war still reveals it cell by cell.
        if (oldHouse != null) stampOldHouse(map, oldHouse);
        if (graveyard != null) stampGraveyard(map, graveyard);

        List<int[]> centers = new ArrayList<>();
        for (Room r : rooms) {
            centers.add(new int[]{r.cx(), r.cy()});
        }

        // Water sources (FR-6, Story 1.5): stamp a Well / Pond / River at ordinary room centers
        // 3..5 (room 0 is the start, room 1 the Old House, room 2 the Graveyard). Placed WITHOUT drawing from `rand`, so the seeded
        // actor/supply placement stream is untouched by the water-source addition — for a given
        // seed, layout and supply drops reproduce exactly as they would with no water stamping
        // (AD-5; the single-identity Supply bindings in IdentifyMap also draw nothing, H1-review).
        // A minimal forward-pull — Epic 3 folds water sources into the real world-structure gen.
        int[] sources = {RogueTile.WELL, RogueTile.POND, RogueTile.RIVER};
        for (int i = 0; i < sources.length && i + 3 < centers.size(); i++) {
            int[] c = centers.get(i + 3);
            map.setTile(c[0], c[1], sources[i]);
        }

        return new FloorResult(map, centers);
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
                    if (map.getTile(x, y) != RogueTile.WALL) continue;
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
}

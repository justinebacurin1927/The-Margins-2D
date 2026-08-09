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
        int entranceDx = -1, entranceDy = -1;
        Room(int x, int y, int w, int h) {
            this.x = x; this.y = y; this.w = w; this.h = h;
        }
        Room entrance(int dx, int dy) {
            this.entranceDx = dx;
            this.entranceDy = dy;
            return this;
        }
        int cx() { return x + w / 2; }
        int cy() { return y + h / 2; }
        int corridorX() { return entranceDx >= 0 ? x + entranceDx : cx(); }
        int corridorY() { return entranceDy >= 0 ? y + entranceDy : cy(); }
    }

    private static final int MIN_ROOM_SIZE = 5;
    private static final int MAX_ROOM_SIZE = 10;
    private static final int MAX_ROOMS = 16; // 11 structures + town + four procedural clearings
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
    /** Root-covered rock bowl and approach: a full eleven-by-nine cave-mouth landmark. */
    private static final int DEEP_CAVE_W = 11;
    private static final int DEEP_CAVE_H = 9;
    private static final int DEEP_CAVE_PATH_X = 5;
    /** Raised timber deck and forest apron for the first Tier-1 scavenge destination. */
    private static final int HUNTERS_BLIND_W = 9;
    private static final int HUNTERS_BLIND_H = 9;
    private static final int HUNTERS_BLIND_PATH_X = 4;
    /** The smaller shrine and kitchen camp retain their successful nine-cell authored scale. */
    private static final int STANDARD_STRUCTURE_W = 9;
    private static final int STANDARD_STRUCTURE_H = 9;
    private static final int STANDARD_STRUCTURE_PATH_X = 4;
    /** Revised structures use individual footprints so their world scale matches their purpose. */
    private static final int FALLEN_LOG_W = 9, FALLEN_LOG_H = 5, FALLEN_LOG_PATH_X = 4;
    private static final int BEEHIVE_W = 11, BEEHIVE_H = 11, BEEHIVE_PATH_X = 5;
    private static final int WATCHTOWER_W = 11, WATCHTOWER_H = 13, WATCHTOWER_PATH_X = 5;
    private static final int POACHERS_CAMP_W = 13, POACHERS_CAMP_H = 11;
    private static final int POACHERS_CAMP_PATH_X = 6;
    private static final int SUNKEN_WELL_W = 11, SUNKEN_WELL_H = 11, SUNKEN_WELL_PATH_X = 5;
    /** The Corneo town plaza (the start room) — an authored 8×6 clearing on the road. */
    private static final int TOWN_W = 8, TOWN_H = 6;
    /** Home-cluster offsets from Corneo's plaza center (Decision 1 — the plaza anchors on the
     *  spine's corneoX/corneoY; the Old House and Graveyard travel with it at fixed offsets). */
    private static final int OLD_HOUSE_DX = -4, OLD_HOUSE_DY = -9;   // south-west of the plaza
    private static final int GRAVEYARD_DX = 3, GRAVEYARD_DY = 10;    // north of the plaza
    private static final int DEEP_CAVE_DX = 28, DEEP_CAVE_DY = 10;   // north-east, toward danger
    private static final int HUNTERS_BLIND_DX = 16, HUNTERS_BLIND_DY = -15; // safe western forest
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
        Room town = new Room(spine.corneoX() - TOWN_W / 2, spine.corneoY() - TOWN_H / 2, TOWN_W, TOWN_H);
        Room oldHouse = new Room(town.cx() + OLD_HOUSE_DX - OLD_HOUSE_W / 2,
                town.cy() + OLD_HOUSE_DY - OLD_HOUSE_H / 2, OLD_HOUSE_W, OLD_HOUSE_H)
                .entrance(OLD_HOUSE_DOOR_X, 0);
        Room graveyard = new Room(town.cx() + GRAVEYARD_DX - GRAVEYARD_W / 2,
                town.cy() + GRAVEYARD_DY - GRAVEYARD_H / 2, GRAVEYARD_W, GRAVEYARD_H)
                .entrance(GRAVEYARD_GATE_X, 0);
        Room deepCave = new Room(town.cx() + DEEP_CAVE_DX - DEEP_CAVE_W / 2,
                town.cy() + DEEP_CAVE_DY - DEEP_CAVE_H / 2, DEEP_CAVE_W, DEEP_CAVE_H)
                .entrance(DEEP_CAVE_PATH_X, 0);
        Room huntersBlind = new Room(town.cx() + HUNTERS_BLIND_DX - HUNTERS_BLIND_W / 2,
                town.cy() + HUNTERS_BLIND_DY - HUNTERS_BLIND_H / 2,
                HUNTERS_BLIND_W, HUNTERS_BLIND_H).entrance(HUNTERS_BLIND_PATH_X, 0);
        Room fallenLog = centeredRoom(spine.tileX(.33f), spine.tileY(.90f),
                FALLEN_LOG_W, FALLEN_LOG_H).entrance(FALLEN_LOG_PATH_X, 0);
        Room forestShrine = centeredRoom(spine.tileX(.08f), spine.tileY(.70f))
                .entrance(STANDARD_STRUCTURE_PATH_X, 0);
        Room beehiveGrove = centeredRoom(spine.tileX(.46f), spine.tileY(.15f),
                BEEHIVE_W, BEEHIVE_H).entrance(BEEHIVE_PATH_X, 0);
        Room kitchenCamp = centeredRoom(spine.tileX(.60f), spine.tileY(.19f))
                .entrance(STANDARD_STRUCTURE_PATH_X, 0);
        Room collapsedWatchtower = centeredRoom(spine.watchtowerX(), spine.roadY() + 8,
                WATCHTOWER_W, WATCHTOWER_H).entrance(WATCHTOWER_PATH_X, 0);
        Room poachersCamp = centeredRoom(spine.tileX(.78f), spine.tileY(.21f),
                POACHERS_CAMP_W, POACHERS_CAMP_H).entrance(POACHERS_CAMP_PATH_X, 0);
        Room sunkenWell = centeredRoom(spine.tileX(.85f), spine.tileY(.77f),
                SUNKEN_WELL_W, SUNKEN_WELL_H).entrance(SUNKEN_WELL_PATH_X, 0);
        rooms.add(town);       blocked.add(town);
        rooms.add(oldHouse);   blocked.add(oldHouse);
        rooms.add(beehiveGrove); blocked.add(beehiveGrove);
        rooms.add(huntersBlind); blocked.add(huntersBlind);
        rooms.add(kitchenCamp); blocked.add(kitchenCamp);
        rooms.add(poachersCamp); blocked.add(poachersCamp);
        rooms.add(collapsedWatchtower); blocked.add(collapsedWatchtower);
        rooms.add(sunkenWell); blocked.add(sunkenWell);
        rooms.add(deepCave);   blocked.add(deepCave);
        rooms.add(fallenLog);  blocked.add(fallenLog);
        rooms.add(graveyard);  blocked.add(graveyard);
        rooms.add(forestShrine); blocked.add(forestShrine);
        for (Room r : rooms) carveRoom(map, r);

        // The Copper Road: a 3-wide floor corridor along the road row, spanning the map. Corneo
        // sits on it (AC-2: the invasion came down this road from the east).
        for (int x = spine.roadStartX(); x <= spine.roadEndX(); x++) {
            for (int dy = -1; dy <= 1; dy++) {
                map.setTile(x, spine.roadY() + dy, RogueTile.FLOOR);
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

        // Chain rooms through their authored south entrances; procedural clearings use centers.
        int doorsPlaced = 0;
        for (int i = 1; i < rooms.size(); i++) {
            Room a = rooms.get(i - 1);
            Room next = rooms.get(i);
            doorsPlaced += carveCorridor(map, a.corridorX(), a.corridorY(),
                    next.corridorX(), next.corridorY(), rand,
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
        stampOldHouse(map, oldHouse);
        stampGraveyard(map, graveyard);
        stampHuntersBlind(map, huntersBlind);
        stampDeepCave(map, deepCave);
        stampStructure(map, fallenLog, RogueTileMap.STRUCTURE_FALLEN_LOG_HOLLOW,
                FloorGenerator::fallenLogTile);
        stampStructure(map, forestShrine, RogueTileMap.STRUCTURE_FOREST_SHRINE,
                FloorGenerator::forestShrineTile);
        stampStructure(map, beehiveGrove, RogueTileMap.STRUCTURE_BEEHIVE_GROVE,
                FloorGenerator::beehiveGroveTile);
        stampStructure(map, kitchenCamp, RogueTileMap.STRUCTURE_KITCHEN_CAMP,
                FloorGenerator::kitchenCampTile);
        stampStructure(map, collapsedWatchtower,
                RogueTileMap.STRUCTURE_COLLAPSED_WATCHTOWER, FloorGenerator::watchtowerTile);
        stampStructure(map, poachersCamp, RogueTileMap.STRUCTURE_POACHERS_CAMP,
                FloorGenerator::poachersCampTile);
        stampStructure(map, sunkenWell, RogueTileMap.STRUCTURE_SUNKEN_WELL,
                FloorGenerator::sunkenWellTile);

        // Water sources (FR-6, Story 1.5) re-anchored to the landmark skeleton (Decision 7): a
        // town well, a roadside pond, and the river at the road's east end. All land on walkable
        // landmark tiles and draw NOTHING from `rand`, so the seeded wilderness/actor stream is
        // untouched (AD-5).
        map.setTile(town.x + 1, town.y + 1, RogueTile.WELL);   // the town well, by the plaza
        map.setTile(spine.roadEndX() - 5, spine.roadY(), RogueTile.POND);   // a roadside pond
        map.setTile(spine.roadEndX() - 2, spine.roadY(), RogueTile.RIVER);  // the road meets the river

        // Connectivity guarantee (O4 carry): the landmark skeleton must be one component from the
        // start. Runs AFTER the structure stamps — a corridor that crossed a structure's future
        // footprint is walled by the stamp, so only a post-stamp flood can see the seal. The
        // repair routes around stamped walls (exits a structure only through its door/gate), then
        // a final structure-aware smooth cleans any divider the repair corridors left.
        ensureReachable(map, town.cx(), town.cy(), landmarkTargets(spine, town,
                oldHouse, graveyard, huntersBlind, deepCave, fallenLog, forestShrine,
                beehiveGrove, kitchenCamp, collapsedWatchtower, poachersCamp, sunkenWell));
        ensureWildernessConnected(map, town.cx(), town.cy());
        smoothForestEdges(map);

        List<int[]> centers = new ArrayList<>();
        for (Room r : rooms) {
            centers.add(new int[]{r.cx(), r.cy()});
        }

        return new FloorResult(map, centers, spine);
    }

    /** The landmark tiles every new run must keep reachable from the start (O4/AC-1). */
    private static int[][] landmarkTargets(WorldSpine spine, Room town, Room oldHouse,
                                           Room graveyard, Room huntersBlind, Room deepCave,
                                           Room fallenLog, Room forestShrine, Room beehiveGrove,
                                           Room kitchenCamp, Room watchtower, Room poachersCamp,
                                           Room sunkenWell) {
        return new int[][]{
                {town.cx(), town.cy()},                                  // Corneo's plaza (the origin)
                {spine.roadStartX(), spine.roadY()},                     // the road's west end
                {spine.roadEndX(), spine.roadY()},                       // the road's east end
                {spine.borderX(), spine.borderY()},                      // the NW border gate
                {oldHouse.x + OLD_HOUSE_DOOR_X, oldHouse.y + 1},         // the Old House entrance
                {graveyard.x + GRAVEYARD_GATE_X, graveyard.y + 1},       // the Graveyard gate
                {huntersBlind.x + HUNTERS_BLIND_PATH_X, huntersBlind.y + 1}, // Blind ladder
                {deepCave.x + DEEP_CAVE_PATH_X, deepCave.y + 1},        // the cave approach
                {fallenLog.corridorX(), fallenLog.y + 1},
                {forestShrine.corridorX(), forestShrine.y + 1},
                {beehiveGrove.corridorX(), beehiveGrove.y + 1},
                {kitchenCamp.corridorX(), kitchenCamp.y + 1},
                {watchtower.corridorX(), watchtower.y + 1},
                {poachersCamp.corridorX(), poachersCamp.y + 1},
                {sunkenWell.corridorX(), sunkenWell.y + 1},
        };
    }

    private static Room centeredRoom(int centerX, int centerY) {
        return centeredRoom(centerX, centerY, STANDARD_STRUCTURE_W, STANDARD_STRUCTURE_H);
    }

    private static Room centeredRoom(int centerX, int centerY, int width, int height) {
        return new Room(centerX - width / 2, centerY - height / 2, width, height);
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

    /** Raised blind: ladder/deck cells are open; railings, posts, and braces block feet, not sight. */
    private static void stampHuntersBlind(RogueTileMap map, Room room) {
        for (int dx = 0; dx < HUNTERS_BLIND_W; dx++) {
            for (int dy = 0; dy < HUNTERS_BLIND_H; dy++) {
                int x = room.x + dx;
                int y = room.y + dy;
                int visualRow = HUNTERS_BLIND_H - 1 - dy;
                int visualCell = visualRow * HUNTERS_BLIND_W + dx;
                map.setStructureTile(x, y, RogueTileMap.STRUCTURE_HUNTERS_BLIND, visualCell);
                map.setTile(x, y, isHuntersBlindObstacle(dx, visualRow)
                        ? RogueTile.FURNITURE : RogueTile.FLOOR);
            }
        }
    }

    /** Collision silhouette matching the rails, tool corner, posts, and under-deck braces. */
    private static boolean isHuntersBlindObstacle(int x, int visualRow) {
        if (visualRow == 0) return x >= 2 && x <= 6;
        if (visualRow == 1) return x == 1 || x == 2 || x == 6 || x == 7;
        if (visualRow >= 2 && visualRow <= 4) return x == 1 || x == 2 || x == 7;
        if (visualRow >= 5 && visualRow <= 7) {
            return x >= 1 && x <= 7 && x != HUNTERS_BLIND_PATH_X;
        }
        return false;
    }

    @FunctionalInterface
    private interface StructureCollision {
        int tileAt(int x, int visualRow);
    }

    /** Shared atlas stamp for authored structures with individual cell footprints. */
    private static void stampStructure(RogueTileMap map, Room room, int type,
                                       StructureCollision collision) {
        for (int dx = 0; dx < room.w; dx++) {
            for (int dy = 0; dy < room.h; dy++) {
                int x = room.x + dx;
                int y = room.y + dy;
                int visualRow = room.h - 1 - dy;
                map.setStructureTile(x, y, type,
                        visualRow * room.w + dx);
                map.setTile(x, y, collision.tileAt(dx, visualRow));
            }
        }
    }

    /** Compact horizontal trunk: opaque end grain surrounds a small, enterable earthen hollow. */
    private static int fallenLogTile(int x, int row) {
        boolean trunk = row == 0 ? x >= 1 && x <= 7
                : row == 1 ? x >= 0 && x <= 8
                : row == 2 ? x <= 1 || x >= 7
                : row == 3 ? x <= 1 || x >= 7
                : row == 4 && (x <= 2 || x >= 6);
        if (trunk) return RogueTile.WALL;
        boolean bedding = (row == 2 || row == 3) && (x == 2 || x == 6);
        return bedding ? RogueTile.FURNITURE : RogueTile.FLOOR;
    }

    /** Low shrine masonry and altar block feet without hiding the ritual court. */
    private static int forestShrineTile(int x, int row) {
        boolean stone = row == 0 ? x >= 1 && x <= 7
                : row == 1 ? x <= 2 || x >= 6 || x >= 3 && x <= 5
                : row == 2 ? x == 0 || x == 8 || x >= 3 && x <= 5
                : row <= 6 ? x == 0 || x == 8
                : row == 7 && x != STANDARD_STRUCTURE_PATH_X;
        return stone ? RogueTile.FURNITURE : RogueTile.FLOOR;
    }

    /** Dense hive trees form the ring; the enlarged five-by-five center remains wholly clear. */
    private static int beehiveGroveTile(int x, int row) {
        boolean trees = row == 0 ? x >= 3 && x <= 7
                : row == 1 ? x >= 2 && x <= 8
                : row == 2 ? x >= 1 && x <= 3 || x >= 7 && x <= 9
                : row == 3 ? x <= 2 || x >= 8
                : row >= 4 && row <= 7 ? x <= 1 || x >= 9
                : row == 8 ? x >= 1 && x <= 3 || x >= 7 && x <= 9
                : row == 9 && (x >= 2 && x <= 4 || x >= 6 && x <= 8);
        return trees ? RogueTile.WALL : RogueTile.FLOOR;
    }

    /** Canvas shelter and work stations leave a broad center aisle from the south. */
    private static int kitchenCampTile(int x, int row) {
        boolean prop = row <= 2 ? x >= 1 && x <= 7
                : row <= 4 ? x <= 2 || x >= 6
                : row <= 6 ? x == 1 || x == 2 || x == 6 || x == 7
                : row == 7 && (x <= 2 || x >= 6);
        return prop ? RogueTile.FURNITURE : RogueTile.FLOOR;
    }

    /** Three stepped tower floors remain traversable through two stair lanes and the south stair. */
    private static int watchtowerTile(int x, int row) {
        boolean ruin = row == 0 ? x >= 4 && x <= 6
                : row == 1 || row == 2 ? x == 3 || x == 7
                : row == 3 || row == 4 ? x == 2 || x == 8
                : row == 5 || row == 6 ? x <= 2 || x >= 8
                : row >= 7 && row <= 10 ? x <= 2 || x >= 8
                : row == 11 && (x <= 4 || x >= 6);
        if (ruin) return RogueTile.WALL;
        boolean debris = row == 4 && x == 6
                || row == 5 && (x == 6 || x == 7)
                || row == 6 && (x == 6 || x == 7)
                || row == 8 && x == 6
                || row == 9 && x == 4;
        return debris ? RogueTile.FURNITURE : RogueTile.FLOOR;
    }

    /** Four shelters ring a broad yard; the middle and exact south entrance stay navigable. */
    private static int poachersCampTile(int x, int row) {
        boolean tents = row >= 1 && row <= 3
                && (x >= 2 && x <= 4 || x >= 7 && x <= 9);
        if (tents) return RogueTile.WALL;
        boolean perimeter = row == 0 && x >= 1 && x <= 11
                || row >= 1 && row <= 9 && (x == 0 || x == 12)
                || row == 9 && x != POACHERS_CAMP_PATH_X
                || row == 10 && x >= 1 && x <= 11 && x != POACHERS_CAMP_PATH_X;
        boolean prop = perimeter
                || (row == 3 || row == 4) && (x <= 2 || x >= 10)
                || row == 4 && (x == 3 || x == 6 || x == 9)
                || row == 5 && (x == 7 || x == 8)
                || row == 6 && (x == 2 || x == 9)
                || row == 7 && x == 2;
        return prop ? RogueTile.FURNITURE : RogueTile.FLOOR;
    }

    /** Three-room wellhouse: solid shell and shaft, open internal doors, clear south approach. */
    private static int sunkenWellTile(int x, int row) {
        boolean shell = row == 0 ? x >= 1 && x <= 9
                : row >= 1 && row <= 8 && (x == 0 || x == 10)
                || row == 8 && x >= 1 && x <= 9 && x != SUNKEN_WELL_PATH_X;
        boolean partition = row >= 1 && row <= 7 && row != 5 && (x == 3 || x == 7);
        boolean shaft = row >= 2 && row <= 4 && x >= 4 && x <= 6;
        if (shell || partition || shaft) return RogueTile.WALL;
        boolean equipment = row >= 3 && row <= 6 && x == 1
                || row >= 3 && row <= 4 && (x == 8 || x == 9)
                || row == 6 && x == 9;
        return equipment ? RogueTile.FURNITURE : RogueTile.FLOOR;
    }

    /**
     * The cave is outdoor terrain, but its boulder bowl must behave as real rock rather than a
     * painted decal. The central approach remains open through the lower half; the black mouth is
     * deliberately solid until a later cave-interior transition is implemented, so actors cannot
     * walk through the image into the forest behind it.
     */
    private static void stampDeepCave(RogueTileMap map, Room room) {
        for (int dx = 0; dx < DEEP_CAVE_W; dx++) {
            for (int dy = 0; dy < DEEP_CAVE_H; dy++) {
                int x = room.x + dx;
                int y = room.y + dy;
                int visualRow = DEEP_CAVE_H - 1 - dy;
                int visualCell = visualRow * DEEP_CAVE_W + dx;
                map.setStructureTile(x, y, RogueTileMap.STRUCTURE_DEEP_CAVE, visualCell);
                map.setTile(x, y, isDeepCaveRock(dx, visualRow)
                        ? RogueTile.WALL : RogueTile.FLOOR);
            }
        }
    }

    /** Collision silhouette matching the generated root-and-boulder bowl. */
    private static boolean isDeepCaveRock(int x, int visualRow) {
        if (visualRow == 0) return x >= 3 && x <= 7;
        if (visualRow <= 3) return x >= 1 && x <= 9;
        if (visualRow <= 5) return x <= 3 || x >= 7;
        if (visualRow == 6) return x <= 2 || x >= 8;
        if (visualRow == 7) return x == 2 || x == 8;
        return false;
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
     * A failed repair is a generator bug — surface it (throw) rather than ship a silently
     * disconnected floor the 24-seed suite happens not to cover.
     */
    private static void ensureReachable(RogueTileMap map, int sx, int sy, int[][] targets) {
        boolean[][] reached = floodFillReachable(map, sx, sy);
        for (int[] t : targets) {
            if (reached[t[0]][t[1]]) continue;
            carvePathTo(map, reached, t[0], t[1]);
            reached = floodFillReachable(map, sx, sy);
            if (!reached[t[0]][t[1]]) {
                throw new IllegalStateException("connectivity repair failed: landmark (" + t[0]
                        + "," + t[1] + ") is still unreachable after carving (O4 guarantee violated)");
            }
        }
    }

    /**
     * A late structure stamp can cover the center of a previously carved three-wide corridor and
     * leave one or two walkable fringe cells outside its atlas. Repair every such outdoor pocket,
     * not just the named landmark targets, so the continuous-wilderness contract remains true for
     * every seed and every future multi-tile structure silhouette.
     */
    private static void ensureWildernessConnected(RogueTileMap map, int sx, int sy) {
        boolean[][] reached = floodFillReachable(map, sx, sy);
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                if (!map.isWalkable(x, y) || map.getStructureTile(x, y) >= 0 || reached[x][y]) {
                    continue;
                }
                carvePathTo(map, reached, x, y);
                reached = floodFillReachable(map, sx, sy);
            }
        }
    }

    /**
     * BFS from an unreached landmark through the forest to the nearest reached walkable tile, then
     * carve a 3-wide corridor back. Runs post-stamp, so it must never punch through a stamped
     * structure's walls: the search refuses to route through a structure wall, forcing the path to
     * exit a sealed structure only through its walkable door/gate/apron — and the carve converts
     * only plain WALL→FLOOR, so a landmark, a door, a structure, or a water source is never
     * destroyed. Furniture is refused as a route cell too: it is neither walkable nor carve-able,
     * so a path stepping on it would leave an impassable hole.
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
                int tile = map.getTile(nx, ny);
                if (tile == RogueTile.WALL && map.getStructureTile(nx, ny) >= 0) {
                    continue; // never route a repair through a stamped structure's wall
                }
                if (tile == RogueTile.FURNITURE) {
                    continue; // the carve converts only plain WALL→FLOOR — a furniture cell can't be opened
                }
                seen[nx][ny] = true;
                parent[nx][ny] = cur;
                q.add(new int[]{nx, ny});
            }
        }
        if (goal == null) {
            throw new IllegalStateException("carvePathTo(" + tx + "," + ty + "): the landmark is fully "
                    + "sealed — no walkable/carveable route exists (O4 guarantee violated)");
        }
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

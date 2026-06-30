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

    public static class FloorResult {
        public RogueTileMap map;
        public List<int[]> roomCenters;
        FloorResult(RogueTileMap map, List<int[]> roomCenters) {
            this.map = map;
            this.roomCenters = roomCenters;
        }
    }

    public static FloorResult generate(int width, int height, Random rand, int floorDepth) {
        RogueTileMap map = new RogueTileMap(width, height);
        map.fill(RogueTile.WALL);

        List<Room> rooms = new ArrayList<>();
        int maxRooms = 8 + floorDepth;
        int attempts = 0;

        while (rooms.size() < maxRooms && attempts < 50) {
            attempts++;
            int rw = rand.nextInt(MAX_ROOM_SIZE - MIN_ROOM_SIZE + 1) + MIN_ROOM_SIZE;
            int rh = rand.nextInt(MAX_ROOM_SIZE - MIN_ROOM_SIZE + 1) + MIN_ROOM_SIZE;
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

        for (int i = 1; i < rooms.size(); i++) {
            Room a = rooms.get(i - 1);
            Room b = rooms.get(i);
            carveCorridor(map, a.cx(), a.cy(), b.cx(), b.cy(), rand);
        }

        List<int[]> centers = new ArrayList<>();
        if (!rooms.isEmpty()) {
            Room start = rooms.get(0);
            Room end = rooms.get(rooms.size() - 1);
            map.setTile(start.cx(), start.cy(), RogueTile.STAIRS_UP);
            map.setTile(end.cx(), end.cy(), RogueTile.STAIRS_DOWN);
            for (Room r : rooms) {
                centers.add(new int[]{r.cx(), r.cy()});
            }
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

    private static void carveCorridor(RogueTileMap map, int x1, int y1, int x2, int y2, Random rand) {
        int cx = x1, cy = y1;
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
                        boolean isDoor = (dx == 0 || dy == 0) && rand.nextFloat() < 0.15f;
                        map.setTile(tx, ty, isDoor ? RogueTile.DOOR : RogueTile.FLOOR);
                    }
                }
            }
        }
    }
}

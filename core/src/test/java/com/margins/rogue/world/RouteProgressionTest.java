package com.margins.rogue.world;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import com.margins.rogue.Companion;
import com.margins.rogue.RogueTile;
import com.margins.rogue.RogueTileMap;
import com.margins.rogue.item.FloorItem;
import com.margins.rogue.item.Inventory;
import com.margins.rogue.state.FlagStore;
import com.margins.rogue.state.RunState;
import com.margins.rogue.system.PlayerAction;
import com.margins.rogue.system.TurnEngine;
import com.margins.rogue.system.TurnResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Story 6.1 — route progression through procedural floors (FR-18): a bounded
 * 1→2→3 descent, carry-across of the party (AC-2), and the route-end seam that
 * Stories 6.2/6.5 build on.
 */
class RouteProgressionTest {

    /** Mirrors SaveService.json() element-type registration. */
    private static Json json() {
        Json json = new Json();
        json.setOutputType(JsonWriter.OutputType.json);
        json.setElementType(RunState.class, "enemies", com.margins.rogue.RogueEnemy.class);
        json.setElementType(RunState.class, "floorItems", FloorItem.class);
        json.setElementType(RunState.class, "companions", Companion.class);
        json.setElementType(FlagStore.class, "flags", Integer.class);
        return json;
    }

    // --- AC-1: the route has a shape — floors 1, 2, 3, then the road ends ---

    @Test
    void newRunStartsOnFloorOneOfTheCaravanRoad() {
        RunState s = new RunState(42L);
        assertEquals(1, s.getFloorDepth(), "a run starts on floor 1");
        assertSame(Route.CARAVAN_ROAD, s.getRoute(), "the run rides the Caravan Road route");
        assertEquals("The Caravan Road", s.getRoute().getName());
        assertEquals(3, s.getRoute().getFloorCount());
    }

    @Test
    void descendAdvancesOneToTwoToThreeThenStops() {
        RunState s = new RunState(42L);
        assertTrue(s.descend(), "floor 1 → 2 is allowed");
        assertEquals(2, s.getFloorDepth());
        assertTrue(s.descend(), "floor 2 → 3 is allowed");
        assertEquals(3, s.getFloorDepth());
        assertFalse(s.descend(), "the route ends at floor 3 — no descent");
        assertEquals(3, s.getFloorDepth(), "floorDepth does not advance past the route bound");
        assertFalse(s.descend(), "still ended");
        assertEquals(3, s.getFloorDepth());
    }

    @Test
    void sameSeedReproducesTheSameFloorSequence() {
        RunState a = new RunState(1234L);
        RunState b = new RunState(1234L);
        assertEquals(a.getPlayer().getTileX(), b.getPlayer().getTileX(), "same start X");
        assertEquals(a.getPlayer().getTileY(), b.getPlayer().getTileY(), "same start Y");
        assertMapsEqual(a, b);

        a.descend();
        b.descend();
        assertMapsEqual(a, b, "floor 2 is seed-reproducible");

        a.descend();
        b.descend();
        assertMapsEqual(a, b, "floor 3 is seed-reproducible");
    }

    @Test
    void routeIsATransientConstantThatSurvivesASaveReload() {
        RunState s = new RunState(42L);
        s.descend();
        s.descend();
        RunState loaded = json().fromJson(RunState.class, json().toJson(s));
        loaded.restoreAfterLoad();
        assertEquals(3, loaded.getFloorDepth(), "the persisted floor position round-trips");
        assertSame(Route.CARAVAN_ROAD, loaded.getRoute(),
                "the constant route is re-initialized by the field initializer after a load (AD-6)");
    }

    // --- AC-2: the party carries across transitions ---

    @Test
    void descendPreservesPlayerVitalsInventoryIdentitiesFlagsAndLastStand() {
        RunState s = new RunState(7L);
        var p = s.getPlayer();
        p.takeDamage(3);
        p.starve(4);
        Inventory inv = s.getInventory();
        assertEquals(Inventory.AddResult.ADDED, inv.tryAdd(0, 2));
        s.getIdentifyMap().markIdentified(1);
        FlagStore fs = s.getFlagStore();
        fs.set("scene.test", 5);
        fs.adjustBond(2);
        s.setLastStandUsed(true);

        int hp = p.getHp(), hunger = p.getHunger();
        int count = inv.count(0);
        boolean identified = s.getIdentifyMap().isIdentified(1);
        int flag = fs.get("scene.test"), bond = fs.getBond();

        s.descend();

        assertEquals(hp, p.getHp(), "HP carries across the transition");
        assertEquals(hunger, p.getHunger(), "hunger carries across the transition");
        assertEquals(count, inv.count(0), "inventory carries across the transition");
        assertEquals(identified, s.getIdentifyMap().isIdentified(1), "identified supplies carry");
        assertEquals(flag, fs.get("scene.test"), "scene flags carry");
        assertEquals(bond, fs.getBond(), "Bond carries");
        assertTrue(s.isLastStandUsed(), "Last-Stand state carries");
    }

    @Test
    void companionIsRepositionedWithADistractionRefillOnTheNewFloor() {
        RunState s = new RunState(42L);
        Companion c = s.getActiveCompanion();
        assertNotNull(c, "a run starts with Galleon");
        c.useDistraction(); // spend one so the refill is observable
        assertEquals(1, c.getDistractionsLeft());

        s.descend();

        Companion after = s.getActiveCompanion();
        assertNotNull(after);
        assertSame(c, after, "the same companion travels, not a freshly spawned one");
        assertTrue(after.isAdjacentTo(s.getPlayer().getTileX(), s.getPlayer().getTileY()),
                "the companion lands beside the new floor's entrance");
        assertEquals(Companion.MAX_DISTRACTIONS_PER_FLOOR, after.getDistractionsLeft(),
                "descent refills the per-floor distraction budget (FR-14)");
        // the transient map is re-injected — a follow step must not NPE.
        assertDoesNotThrow(() -> after.followStep(s.getPlayer().getTileX(), s.getPlayer().getTileY()));
    }

    @Test
    void floorItemsAreRescatteredFreshNotCarried() {
        RunState s = new RunState(42L);
        // a drop on the old floor must not follow the party.
        s.addFloorItem(0, 1, s.getPlayer().getTileX(), s.getPlayer().getTileY());
        s.descend();
        List<FloorItem> items = s.getFloorItems();
        assertTrue(items.size() >= 2 && items.size() <= 4, "each floor scatters 2..4 supplies");
        for (FloorItem it : items) {
            assertTrue(s.getTileMap().isWalkable(it.x, it.y),
                    "scattered items sit on walkable tiles of the new floor");
            assertFalse(it.x == s.getPlayer().getTileX() && it.y == s.getPlayer().getTileY(),
                    "the scatter avoids the entrance tile");
        }
    }

    // --- Integration: the turn engine drives the descent (Task 3) ---

    @Test
    void turnEngineDescendsOneTwoThreeThenReportsTheRouteEnd() {
        RunState s = new RunState(42L);
        TurnEngine te = new TurnEngine();

        TurnResult r1 = stepOntoStairs(s, te);
        assertEquals(2, s.getFloorDepth());
        assertNotNull(r1.lastMessage());
        assertTrue(r1.lastMessage().contains("descend"), "message announces the descent: " + r1.lastMessage());

        TurnResult r2 = stepOntoStairs(s, te);
        assertEquals(3, s.getFloorDepth());
        assertNotNull(r2.lastMessage());
        assertTrue(r2.lastMessage().contains("descend"), "message announces the descent: " + r2.lastMessage());

        TurnResult r3 = stepOntoStairs(s, te);
        assertEquals(3, s.getFloorDepth(), "the road ends at floor 3 — no floor 4");
        assertEquals(s.getRoute().endMessage(), r3.lastMessage(), "the route-end message is shown");
    }

    /** Find the current floor's STAIRS_DOWN, stand on a walkable neighbor, and move onto it. */
    private TurnResult stepOntoStairs(RunState s, TurnEngine te) {
        RogueTileMap m = s.getTileMap();
        int sx = -1, sy = -1;
        outer:
        for (int x = 0; x < m.getWidth(); x++) {
            for (int y = 0; y < m.getHeight(); y++) {
                if (m.getTile(x, y) == RogueTile.STAIRS_DOWN) {
                    sx = x;
                    sy = y;
                    break outer;
                }
            }
        }
        assertTrue(sx >= 0, "every generated floor has a STAIRS_DOWN tile");
        // The player stands one tile off the stairs and steps onto it. (ox, oy) is the
        // neighbor offset from the stairs; the move that reaches the stairs is its opposite
        // and the facing is that move's direction, as the screen reads keys (SOUTH=0,...).
        int[][] spots = {{0, 1, 0}, {0, -1, 1}, {1, 0, 2}, {-1, 0, 3}}; // ox, oy, facing toward stairs
        for (int[] p : spots) {
            int nx = sx + p[0], ny = sy + p[1];
            if (m.isWalkable(nx, ny)) {
                s.getPlayer().placeAt(nx, ny);
                s.getPlayer().setFacing(p[2]);
                return te.advance(s, PlayerAction.move(-p[0], -p[1], p[2]));
            }
        }
        fail("the stairs tile has no walkable neighbor");
        return null; // unreachable — fail() throws
    }

    private void assertMapsEqual(RunState a, RunState b) {
        assertMapsEqual(a, b, null);
    }

    private void assertMapsEqual(RunState a, RunState b, String message) {
        RogueTileMap ma = a.getTileMap(), mb = b.getTileMap();
        assertEquals(ma.getWidth(), mb.getWidth());
        assertEquals(ma.getHeight(), mb.getHeight());
        for (int x = 0; x < ma.getWidth(); x++) {
            for (int y = 0; y < ma.getHeight(); y++) {
                assertEquals(ma.getTile(x, y), mb.getTile(x, y),
                        (message == null ? "" : message + ": ") + "tile (" + x + "," + y + ")");
            }
        }
    }
}

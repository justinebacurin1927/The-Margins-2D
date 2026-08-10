package com.margins.rogue.system;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import com.margins.rogue.Companion;
import com.margins.rogue.RogueEnemy;
import com.margins.rogue.RoguePlayer;
import com.margins.rogue.RogueTileMap;
import com.margins.rogue.Weather;
import com.margins.rogue.item.FloorItem;
import com.margins.rogue.state.FlagStore;
import com.margins.rogue.state.RunState;
import com.margins.rogue.world.StructureTable;
import com.margins.rogue.world.StructureTable.Hazard;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Story 3.4 (FR-10/FR-5, AC-1/2/3): night and weather shift a location's danger. Fills the 3.3
 * {@code HazardSystem.nightHazardFor} seam with per-location night flips (Graveyard undead, Sunken
 * Well creature, Poacher's Camp aggressive patrol; Beehive Grove the sole safer-flip) and Storm's
 * structural-collapse stack. Derived from the persisted clock + weather (no new persisted field,
 * AD-6); one seeded draw per structure step (AD-5). Turn-level, deterministic-on-fixed-seed pins.
 */
class NightWeatherHazardTest {

    private static RunState day(long seed) {
        RunState s = new RunState(seed);
        s.setWeather(Weather.CLEAR);
        return s; // clock 0 → Day
    }

    private static RunState night(long seed, Weather w) {
        RunState s = new RunState(seed);
        TurnEngine engine = new TurnEngine();
        // Drive to the first Night turn via DAY_LENGTH acted WAITs (never a hard-coded 100 — a
        // day-length retune must not silently break every night test).
        for (int i = 0; i < RunState.DAY_LENGTH; i++) engine.advance(s, PlayerAction.wait(RoguePlayer.SOUTH));
        assertFalse(s.isDay(), "the DAY_LENGTH-th acted turn opens Night");
        s.setWeather(w); // re-pin after any boundary roll
        return s;
    }

    // --- Task 1: per-location night hazard flips via the nightHazardFor seam (AC: 1) ---

    @Test
    void theFourNamedLocationsFlipAtNightAndHoldTheirBaselineByDay() {
        RunState d = day(42L);
        RunState n = night(42L, Weather.CLEAR);

        // Day: every structure resolves its authored day hazard.
        assertEquals(Hazard.GRAVE_GROUND, nightHazardFor(d, RogueTileMap.STRUCTURE_GRAVEYARD));
        assertEquals(Hazard.SLIP_AND_FALL, nightHazardFor(d, RogueTileMap.STRUCTURE_SUNKEN_WELL));
        assertEquals(Hazard.SNARE_TRAP, nightHazardFor(d, RogueTileMap.STRUCTURE_POACHERS_CAMP));
        assertEquals(Hazard.SWARM, nightHazardFor(d, RogueTileMap.STRUCTURE_BEEHIVE_GROVE));

        // Night: the four named locations flip.
        assertEquals(Hazard.GRAVE_UNDEAD, nightHazardFor(n, RogueTileMap.STRUCTURE_GRAVEYARD),
                "the Graveyard's undead become active at night");
        assertEquals(Hazard.WELL_CREATURE, nightHazardFor(n, RogueTileMap.STRUCTURE_SUNKEN_WELL),
                "the Sunken Well's creature becomes active at night");
        assertEquals(Hazard.POACHER_PATROL, nightHazardFor(n, RogueTileMap.STRUCTURE_POACHERS_CAMP),
                "the Poacher's Camp patrols turn more aggressive at night");
        assertEquals(Hazard.NONE, nightHazardFor(n, RogueTileMap.STRUCTURE_BEEHIVE_GROVE),
                "the Beehive Grove flips SAFER at night (the sole exception)");
    }

    @Test
    void theThreeHostileFlipsAreStrictlyWorseAndBeehiveIsStrictlySafer() {
        RunState n = night(42L, Weather.CLEAR);
        assertWorse(Hazard.GRAVE_GROUND, Hazard.GRAVE_UNDEAD);
        assertWorse(Hazard.SLIP_AND_FALL, Hazard.WELL_CREATURE);
        assertWorse(Hazard.SNARE_TRAP, Hazard.POACHER_PATROL);
        // Beehive: the night hazard is NONE — strictly safer than the daytime swarm.
        assertEquals(0, Hazard.NONE.chancePercent());
        assertEquals(0, Hazard.NONE.damage());
        assertTrue(Hazard.SWARM.chancePercent() > 0, "the daytime swarm is a real hazard the night removes");
    }

    @Test
    void aNonFlippedStructureIsIdenticalDayAndNight() {
        RunState d = day(42L), n = night(42L, Weather.CLEAR);
        assertEquals(nightHazardFor(d, RogueTileMap.STRUCTURE_HUNTERS_BLIND),
                nightHazardFor(n, RogueTileMap.STRUCTURE_HUNTERS_BLIND),
                "a non-flipped structure keeps its authored hazard at all hours");
        assertEquals(Hazard.WEAK_FLOOR_PLANK, nightHazardFor(n, RogueTileMap.STRUCTURE_HUNTERS_BLIND));
    }

    @Test
    void steppingOntoTheNightGraveyardDealsTheUndeadDamage() {
        // Behavioral: at the night Graveyard, holding a torch (suppresses the generic 3.3 overlay),
        // stepping resolves the UNDEAD hazard — so the only HP drop is the undead's damage (AC-1).
        RunState s = night(11L, Weather.CLEAR);
        int[] cell = structureCellAndNeighbor(s.getTileMap(), RogueTileMap.STRUCTURE_GRAVEYARD);
        assertNotNull(cell, "the Graveyard has a walkable cell with a same-type neighbor");
        s.getPlayer().placeAt(cell[0], cell[1]);
        s.getEnemies().clear();
        s.getCompanions().clear();
        s.lightTorch(80); // suppress the generic night stumble so only the structure hazard lands
        TurnEngine engine = new TurnEngine();
        boolean fired = false;
        for (int i = 0; i < 40 && !fired; i++) {
            int dx = (i % 2 == 0 ? cell[2] : -cell[2]);
            int dy = (i % 2 == 0 ? cell[3] : -cell[3]);
            int before = s.getPlayer().getHp();
            engine.advance(s, PlayerAction.move(dx, dy, RoguePlayer.directionOf(dx, dy)));
            int dropped = before - s.getPlayer().getHp();
            assertTrue(dropped == 0 || dropped == Hazard.GRAVE_UNDEAD.damage(),
                    "a torch-lit night Graveyard step drops only the undead's damage (got " + dropped + ")");
            if (dropped > 0) fired = true;
        }
        assertTrue(fired, "the night Graveyard's undead fire within 40 steps on seed 11");
    }

    @Test
    void theNightBeehiveNeverHarmsTheTorchLitPlayer() {
        // Beehive night hazard is NONE; with a torch (overlay suppressed) a night Beehive step is a
        // pure no-op — the sole safer-flip proved behaviorally.
        RunState s = night(11L, Weather.CLEAR);
        int[] cell = structureCellAndNeighbor(s.getTileMap(), RogueTileMap.STRUCTURE_BEEHIVE_GROVE);
        assertNotNull(cell);
        s.getPlayer().placeAt(cell[0], cell[1]);
        s.getEnemies().clear();
        s.getCompanions().clear();
        s.lightTorch(80);
        TurnEngine engine = new TurnEngine();
        for (int i = 0; i < 40; i++) {
            int dx = (i % 2 == 0 ? cell[2] : -cell[2]);
            int dy = (i % 2 == 0 ? cell[3] : -cell[3]);
            int hp = s.getPlayer().getHp();
            engine.advance(s, PlayerAction.move(dx, dy, RoguePlayer.directionOf(dx, dy)));
            assertEquals(hp, s.getPlayer().getHp(), "the night Beehive is safe — no hazard fires");
        }
    }

    // --- Task 2: Storm raises structural-collapse chance (AC: 2) ---

    @Test
    void isStructuralClassifiesTheCollapseFamily() {
        for (Hazard h : new Hazard[]{Hazard.WEAK_FLOOR_PLANK, Hazard.SOFT_ROT, Hazard.COLLAPSING_STONE,
                Hazard.TOWER_COLLAPSE, Hazard.STRUCTURAL_DECAY, Hazard.CAVE_IN}) {
            assertTrue(h.isStructural(), h + " is a structural/collapse hazard");
        }
        for (Hazard h : new Hazard[]{Hazard.NONE, Hazard.SWARM, Hazard.ASH_RESIDUE, Hazard.SNARE_TRAP,
                Hazard.SLIP_AND_FALL, Hazard.GRAVE_GROUND}) {
            assertFalse(h.isStructural(), h + " is not structural");
        }
    }

    @Test
    void stormRaisesAStructuralHazardsFireRateAcrossSeeds() {
        // Forest Shrine's COLLAPSING_STONE (structural, day chance 20%) fires measurably more often
        // under Storm than under Clear (AC-2). Day-time, so the night overlay/flips don't interfere.
        int clearHits = structuralHits(Weather.CLEAR);
        int stormHits = structuralHits(Weather.STORM);
        assertTrue(stormHits > clearHits,
                "Storm raises the structural-collapse fire rate (Clear " + clearHits + " vs Storm " + stormHits + ")");
    }

    @Test
    void stormLeavesANonStructuralHazardUnchanged() {
        // A non-structural hazard (Beehive's SWARM, day) is unaffected by Storm — the SAME seed
        // produces the SAME outcome under Clear and Storm (the bonus never touches it, AD-5).
        for (long seed = 0; seed < 12; seed++) {
            int clear = firstStepDrop(seed, Weather.CLEAR, RogueTileMap.STRUCTURE_BEEHIVE_GROVE);
            int storm = firstStepDrop(seed, Weather.STORM, RogueTileMap.STRUCTURE_BEEHIVE_GROVE);
            assertEquals(clear, storm, "seed " + seed + ": Storm does not change a non-structural hazard");
        }
    }

    // --- Task 3: AD-5/AD-6 discipline + 3.3 overlay coexistence (AC: 3) ---

    @Test
    void theFlipsAddNoPersistedStateAndSurviveRoundTrip() {
        RunState s = night(42L, Weather.STORM);
        Hazard graveNight = nightHazardFor(s, RogueTileMap.STRUCTURE_GRAVEYARD);
        Weather w = s.getWeather();

        String json = json().toJson(s);
        assertFalse(json.contains("nightHazard"), "no night-hazard state is persisted (AD-6)");
        RunState loaded = json().fromJson(RunState.class, json);
        loaded.restoreAfterLoad();

        assertFalse(loaded.isDay(), "night survives the round-trip (derived from clockTurns)");
        assertEquals(w, loaded.getWeather(), "the weather persists (Storm)");
        assertEquals(graveNight, nightHazardFor(loaded, RogueTileMap.STRUCTURE_GRAVEYARD),
                "the night flip re-derives identically after load — no new persisted field");
    }

    @Test
    void theGenericOverlayStillStacksOnAFlippedStructureAtNight() {
        // Without a torch, a night flipped structure resolves the generic 3.3 stumble (1) AND the
        // night structure hazard (undead, 2) — they stack (a dark, active Graveyard is doubly
        // dangerous), while a single step never exceeds their sum.
        RunState s = night(7L, Weather.CLEAR);
        int[] cell = structureCellAndNeighbor(s.getTileMap(), RogueTileMap.STRUCTURE_GRAVEYARD);
        assertNotNull(cell);
        s.getPlayer().placeAt(cell[0], cell[1]);
        s.getEnemies().clear();
        s.getCompanions().clear();
        TurnEngine engine = new TurnEngine();
        boolean stacked = false;
        int max = HazardSystemNightStumbleDamage() + Hazard.GRAVE_UNDEAD.damage();
        for (int i = 0; i < 80; i++) {
            int dx = (i % 2 == 0 ? cell[2] : -cell[2]);
            int dy = (i % 2 == 0 ? cell[3] : -cell[3]);
            int before = s.getPlayer().getHp();
            engine.advance(s, PlayerAction.move(dx, dy, RoguePlayer.directionOf(dx, dy)));
            int dropped = before - s.getPlayer().getHp();
            assertTrue(dropped >= 0 && dropped <= max,
                    "a night Graveyard step drops at most overlay(1)+undead(2): got " + dropped);
            if (dropped == max) stacked = true;
        }
        assertTrue(stacked, "some night Graveyard step lands BOTH the stumble and the undead (they stack)");
    }

    @Test
    void theSameSeedReproducesTheSameNightStormHazardOutcome() {
        // Review pin (AD-5): the invariant the story claimed but did not pin — two identical-seed
        // runs, walked identically at a night flipped structure under Storm, reproduce the SAME HP
        // sequence. A hazard drawn from an unseeded/global Random would pass the other tests but
        // diverge here. (Sunken Well WELL_CREATURE at night; the well cell is also structural-free,
        // so this isolates the night-flip + shared rng determinism.)
        for (long seed : new long[]{2L, 7L, 21L}) {
            RunState a = night(seed, Weather.STORM);
            RunState b = night(seed, Weather.STORM);
            int[] ca = structureCellAndNeighbor(a.getTileMap(), RogueTileMap.STRUCTURE_SUNKEN_WELL);
            int[] cb = structureCellAndNeighbor(b.getTileMap(), RogueTileMap.STRUCTURE_SUNKEN_WELL);
            assertNotNull(ca);
            a.getPlayer().placeAt(ca[0], ca[1]);
            b.getPlayer().placeAt(cb[0], cb[1]);
            a.getEnemies().clear(); b.getEnemies().clear();
            a.getCompanions().clear(); b.getCompanions().clear();
            TurnEngine ea = new TurnEngine(), eb = new TurnEngine();
            for (int i = 0; i < 20; i++) {
                int dx = (i % 2 == 0 ? ca[2] : -ca[2]);
                int dy = (i % 2 == 0 ? ca[3] : -ca[3]);
                ea.advance(a, PlayerAction.move(dx, dy, RoguePlayer.directionOf(dx, dy)));
                eb.advance(b, PlayerAction.move(dx, dy, RoguePlayer.directionOf(dx, dy)));
                assertEquals(a.getPlayer().getHp(), b.getPlayer().getHp(),
                        "seed " + seed + " step " + i + ": same seed reproduces the same night+Storm outcome (AD-5)");
            }
        }
    }

    @Test
    void nightHazardForToleratesAnUnmappedStructure() {
        // Review patch (P1): the now-package-private seam hardens against a null structure (an
        // unmapped type via forType) — step() already guards it, but a direct caller must not NPE.
        RunState n = night(42L, Weather.CLEAR);
        assertEquals(Hazard.NONE, HazardSystem.nightHazardFor(n, StructureTable.forType(-1)),
                "an unmapped/null structure resolves to NONE, not an NPE");
    }

    @Test
    void aNightStructureStepAddsNoExtraClockTick() {
        RunState s = night(7L, Weather.STORM);
        int[] cell = structureCellAndNeighbor(s.getTileMap(), RogueTileMap.STRUCTURE_SUNKEN_WELL);
        assertNotNull(cell);
        s.getPlayer().placeAt(cell[0], cell[1]);
        s.getEnemies().clear();
        s.getCompanions().clear();
        int clock = s.getClockTurns();
        new TurnEngine().advance(s,
                PlayerAction.move(cell[2], cell[3], RoguePlayer.directionOf(cell[2], cell[3])));
        assertEquals(clock + 1, s.getClockTurns(), "one acted step = one clock tick");
    }

    // --- helpers ---

    private static Hazard nightHazardFor(RunState s, int structureType) {
        return HazardSystem.nightHazardFor(s, StructureTable.forType(structureType));
    }

    private static void assertWorse(Hazard day, Hazard night) {
        assertTrue(night.chancePercent() >= day.chancePercent() && night.damage() >= day.damage()
                        && (night.chancePercent() > day.chancePercent() || night.damage() > day.damage()),
                night + " is strictly worse than " + day
                        + " (" + night.chancePercent() + "%/" + night.damage()
                        + " vs " + day.chancePercent() + "%/" + day.damage() + ")");
    }

    /** COLLAPSING_STONE hits at the Forest Shrine over a seed range under the given weather (day). */
    private static int structuralHits(Weather w) {
        int hits = 0;
        for (long seed = 0; seed < 60; seed++) {
            if (firstStepDrop(seed, w, RogueTileMap.STRUCTURE_FOREST_SHRINE) > 0) hits++;
        }
        return hits;
    }

    /** HP dropped by the first landed step onto a structure cell at DAY under the given weather. */
    private static int firstStepDrop(long seed, Weather w, int structureType) {
        RunState s = new RunState(seed);
        s.setWeather(w);
        int[] cell = structureCellAndNeighbor(s.getTileMap(), structureType);
        if (cell == null) return 0;
        s.getPlayer().placeAt(cell[0], cell[1]);
        s.getEnemies().clear();
        s.getCompanions().clear();
        int before = s.getPlayer().getHp();
        new TurnEngine().advance(s,
                PlayerAction.move(cell[2], cell[3], RoguePlayer.directionOf(cell[2], cell[3])));
        return before - s.getPlayer().getHp();
    }

    private static int HazardSystemNightStumbleDamage() {
        return 1; // the 3.3 generic night stumble deals 1 HP (NIGHT_STUMBLE_DAMAGE)
    }

    /** {ax, ay, dx, dy}: a walkable structure cell with a 4-adjacent walkable cell of the SAME
     *  structure type (the destination the player steps onto), or null. */
    private static int[] structureCellAndNeighbor(RogueTileMap m, int type) {
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int x = 0; x < m.getWidth(); x++) {
            for (int y = 0; y < m.getHeight(); y++) {
                if (m.getStructureType(x, y) != type || !m.isWalkable(x, y)) continue;
                for (int[] d : dirs) {
                    int nx = x + d[0], ny = y + d[1];
                    if (m.getStructureType(nx, ny) == type && m.isWalkable(nx, ny)) {
                        return new int[]{x, y, d[0], d[1]};
                    }
                }
            }
        }
        return null;
    }

    private static Json json() {
        Json json = new Json();
        json.setOutputType(JsonWriter.OutputType.json);
        json.setUsePrototypes(false);
        json.setElementType(RunState.class, "enemies", RogueEnemy.class);
        json.setElementType(RunState.class, "floorItems", FloorItem.class);
        json.setElementType(RunState.class, "companions", Companion.class);
        json.setElementType(FlagStore.class, "flags", Integer.class);
        return json;
    }
}

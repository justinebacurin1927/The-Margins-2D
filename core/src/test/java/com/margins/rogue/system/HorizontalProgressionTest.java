package com.margins.rogue.system;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import com.margins.rogue.Companion;
import com.margins.rogue.Detection;
import com.margins.rogue.RogueEnemy;
import com.margins.rogue.RoguePlayer;
import com.margins.rogue.RogueTile;
import com.margins.rogue.RogueTileMap;
import com.margins.rogue.Weather;
import com.margins.rogue.item.FloorItem;
import com.margins.rogue.item.Supply;
import com.margins.rogue.item.TrueIdentity;
import com.margins.rogue.state.FlagStore;
import com.margins.rogue.state.RunState;
import com.margins.rogue.world.StructureTable;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Story 3.5 (FR-11, SM-3): horizontal progression — SKILL and knowledge. AC-1 pins that a kill
 * raises no number (by construction — no XP field exists). AC-2 builds the SKILL-governed lockpicking
 * that opens the Old House's locked cellar (a seeded SurvivalCraft-curve roll, FlagStore-persisted
 * opened state) and the queryable knowledge surface (map-fragment reads + the location-danger query
 * over Story 3.4's authored night flips). AC-3 pins that a knowledge-using run is measurably better
 * off with no stat rising. Scope guard: no new persisted RunState field (AD-6), no SKILL growth,
 * no noise, no extra clock tick.
 */
class HorizontalProgressionTest {

    // --- Task 1: AC-1 — the no-combat-XP pin (AC: 1) ---

    @Test
    void aResolvedKillRaisesNoPlayerNumber() {
        RunState s = killState();
        RoguePlayer p = s.getPlayer();
        RogueEnemy e = new RogueEnemy(p.getTileX(), p.getTileY() + 1, s.getTileMap());
        e.setDetection(Detection.ALERTED);
        s.getEnemies().add(e);

        int str = p.getStr(), grit = p.getGrit(), instinct = p.getInstinct(),
                voice = p.getVoice(), skill = p.getSkill(), maxHp = p.getMaxHp();

        new TurnEngine().advance(s, PlayerAction.attack(RoguePlayer.NORTH));
        assertTrue(e.isAlive(), "the first swing wounds the 8-HP enemy (STR 5)");
        new TurnEngine().advance(s, PlayerAction.attack(RoguePlayer.NORTH));
        assertFalse(e.isAlive(), "the second swing kills");

        // No number, level, or XP rises from the kill — STR/GRIT/INSTINCT/VOICE/SKILL and max-HP
        // are exactly what they were before it (AC-1). HP is not a stat/counter/level: it may fall
        // (the enemy's counter-swing) but never rises from the kill.
        assertEquals(str, p.getStr(), "a kill raises no STR");
        assertEquals(grit, p.getGrit(), "a kill raises no GRIT");
        assertEquals(instinct, p.getInstinct(), "a kill raises no INSTINCT");
        assertEquals(voice, p.getVoice(), "a kill raises no VOICE");
        assertEquals(skill, p.getSkill(), "a kill raises no SKILL");
        assertEquals(maxHp, p.getMaxHp(), "a kill raises no max-HP");
    }

    @Test
    void noLevelingFieldExistsAnywhereOnThePlayerOrRun() {
        // AC-1's "by construction" half: the player and run carry NO kill-count / level / XP field
        // or counter. Reflection guards against a future regression that sneaks one in.
        assertNoLevelingField(RoguePlayer.class);
        assertNoLevelingField(RunState.class);
    }

    private static void assertNoLevelingField(Class<?> cls) {
        for (Field f : cls.getDeclaredFields()) {
            String n = f.getName().toLowerCase();
            if (n.equals("skill")) continue; // SKILL is the horizontal axis, not an XP counter
            assertFalse(n.contains("xp") || n.contains("level") || n.contains("experience")
                            || n.contains("kill"),
                    cls.getSimpleName() + " carries a leveling/XP/kill field (AC-1): " + f.getName());
        }
    }

    // --- Task 2: SKILL-governed lockpicking exposes the Old House cellar (AC: 2) ---

    @Test
    void higherSkillOpensTheCellarMoreOftenAcrossSeeds() {
        // The SKILL curve (SurvivalCraft.skillChance: 40 + 8×SKILL, clamped 0..95): SKILL 0 ≈ 40%,
        // SKILL 10 ≈ 95%. Over a fixed seed range, the high-SKILL run must open the cellar on the
        // first attempt measurably more often than the low-SKILL run — horizontal growth, no number
        // rising (the skill is pinned, not gained).
        int low = 0, high = 0;
        for (long seed = 0; seed < 80; seed++) {
            if (firstLockpickOpens(seed, 0)) low++;
            if (firstLockpickOpens(seed, 10)) high++;
        }
        assertTrue(low > 0, "low SKILL can open the cellar (" + low + "/80) — a real chance");
        assertTrue(high > low, "higher SKILL opens more often (low " + low + " vs high " + high + ")");
        // "Even high SKILL is a chance, not a guarantee" is pinned deterministically by the curve's
        // 95% clamp — a stream-count ceiling (e.g. high < 80) could flake if a future refactor
        // shifts the rng stream (AD-5), so assert the clamp itself instead.
        assertEquals(95, SurvivalCraft.skillChance(lockpickState(0L, 10)),
                "the SKILL curve clamps at 95% — high SKILL never guarantees the roll");
    }

    @Test
    void onSuccessTheCellarLootIsReachableInTheFootprint() {
        // On the first successful lockpick, the Old House's lockedLoot (PRESERVED_FOOD×3 +
        // FOLDED_CLOTH×2, both guaranteed) lands in the structure footprint — the authored-loot
        // placement pattern — on top of the generation loot (PRESERVED_FOOD×2 + FOLDED_CLOTH×1).
        long seed = seedWhereFirstLockpickOpens();
        RunState s = lockpickState(seed, 5);
        int[] box = footprint(s.getTileMap(), RogueTileMap.STRUCTURE_OLD_HOUSE);
        assertNotNull(box);
        int foodBefore = floorItemsOfTypeInBox(s, Supply.PRESERVED_FOOD.ordinal(), box);
        int clothBefore = floorItemsOfTypeInBox(s, Supply.FOLDED_CLOTH.ordinal(), box);
        RoguePlayer p = s.getPlayer();
        // The generation loot may already sit on the lockpick cell (placeStructureLoot avoids only
        // the generation-time tile) — snapshot it so the avoid-rule check diffs the cellar pass only.
        boolean underPlayerBefore = hasItemAt(s, Supply.PRESERVED_FOOD.ordinal(),
                p.getTileX(), p.getTileY());

        new TurnEngine().advance(s, PlayerAction.lockpick(RoguePlayer.EAST));

        assertEquals(1, s.getFlagStore().get(LockpickSystem.KEY_CELLAR_OPENED),
                "the success opens the cellar (flag set)");
        assertEquals(foodBefore + 3, floorItemsOfTypeInBox(s, Supply.PRESERVED_FOOD.ordinal(), box),
                "the cellar's 3 preserved food are reachable in the Old House footprint");
        assertEquals(clothBefore + 2, floorItemsOfTypeInBox(s, Supply.FOLDED_CLOTH.ordinal(), box),
                "the cellar's 2 folded cloth are reachable in the Old House footprint");
        // The cellar placement never lands on the player's tile (the placeStructureLoot avoid rule):
        // whatever was under the player before the pick is exactly what remains after it.
        assertEquals(underPlayerBefore, hasItemAt(s, Supply.PRESERVED_FOOD.ordinal(),
                        p.getTileX(), p.getTileY()),
                "no cellar loot is placed under the player (generation loot, if any, is unchanged)");
    }

    @Test
    void theOpenedFlagPersistsAcrossSaveLoadWithNoReLockAndNoDoubleLoot() {
        // The 3.2 structureLootPlaced lesson, in FlagStore form: the opened state is persisted, so
        // a reload neither re-locks the cellar nor double-places its loot (AD-6 — no new RunState
        // field; the flag rides the already-persisted store).
        long seed = seedWhereFirstLockpickOpens();
        RunState s = lockpickState(seed, 5);
        int[] box = footprint(s.getTileMap(), RogueTileMap.STRUCTURE_OLD_HOUSE);
        new TurnEngine().advance(s, PlayerAction.lockpick(RoguePlayer.EAST));
        int food = floorItemsOfTypeInBox(s, Supply.PRESERVED_FOOD.ordinal(), box);
        int cloth = floorItemsOfTypeInBox(s, Supply.FOLDED_CLOTH.ordinal(), box);

        RunState loaded = json().fromJson(RunState.class, json().toJson(s));
        loaded.restoreAfterLoad();

        assertEquals(1, loaded.getFlagStore().get(LockpickSystem.KEY_CELLAR_OPENED),
                "the opened flag survives the round-trip");
        assertEquals(food, floorItemsOfTypeInBox(loaded, Supply.PRESERVED_FOOD.ordinal(), box),
                "reload does not double-place the cellar loot");
        assertEquals(cloth, floorItemsOfTypeInBox(loaded, Supply.FOLDED_CLOTH.ordinal(), box),
                "reload does not double-place the cellar cloth");
        // A re-lockpick on the open cellar is refused — no turn, nothing new.
        int clock = loaded.getClockTurns();
        new TurnEngine().advance(loaded, PlayerAction.lockpick(RoguePlayer.EAST));
        assertEquals(clock, loaded.getClockTurns(), "the already-open cellar commits no turn");
        assertEquals(food, floorItemsOfTypeInBox(loaded, Supply.PRESERVED_FOOD.ordinal(), box),
                "and places nothing more");
    }

    @Test
    void aRefusedAttemptCommitsNoTurnAndAFailureSpendsIt() {
        // Refused (wrong place / no tool / already open): no turn, like the inert-USE precedent.
        RunState away = new RunState(42L); // the start room center — not the Old House
        away.getInventory().tryAdd(Supply.SMALL_TOOLS.ordinal(), 1);
        int clock = away.getClockTurns();
        new TurnEngine().advance(away, PlayerAction.lockpick(RoguePlayer.EAST));
        assertEquals(clock, away.getClockTurns(), "a lockpick away from the Old House commits no turn");

        RunState noTool = lockpickState(seedWhereFirstLockpickOpens(), 5);
        noTool.getInventory().remove(Supply.SMALL_TOOLS.ordinal(), 1);
        int clock2 = noTool.getClockTurns();
        new TurnEngine().advance(noTool, PlayerAction.lockpick(RoguePlayer.EAST));
        assertEquals(clock2, noTool.getClockTurns(), "a lockpick without Small Tools commits no turn");
        assertTrue(noTool.getMessageLog().stream().anyMatch(m -> m.contains("Small Tools")),
                "the refusal names the missing tool: " + noTool.getMessageLog());

        RunState open = lockpickState(seedWhereFirstLockpickOpens(), 5);
        new TurnEngine().advance(open, PlayerAction.lockpick(RoguePlayer.EAST));
        int clock3 = open.getClockTurns();
        new TurnEngine().advance(open, PlayerAction.lockpick(RoguePlayer.EAST));
        assertEquals(clock3, open.getClockTurns(), "a lockpick on the open cellar commits no turn");

        // A FAILED attempt spends the turn (the lock holds) — it is a real action with a real roll.
        RunState fail = lockpickState(seedWhereFirstLockpickFails(), 5);
        int clock4 = fail.getClockTurns();
        new TurnEngine().advance(fail, PlayerAction.lockpick(RoguePlayer.EAST));
        assertEquals(clock4 + 1, fail.getClockTurns(), "a failed lockpick spends the turn");
        assertEquals(0, fail.getFlagStore().get(LockpickSystem.KEY_CELLAR_OPENED),
                "and leaves the cellar locked");
        assertTrue(fail.getMessageLog().stream().anyMatch(m -> m.contains("lock holds")),
                "the failure is announced: " + fail.getMessageLog());
    }

    @Test
    void theSameSeedReproducesTheSameLockpickOutcome() {
        // AD-5: one seeded roll per attempt, byte-identical per seed. Two identical runs on the
        // same seed resolve the first attempt identically — and a REFUSED attempt (no tool) draws
        // nothing from the stream, so the next draw matches a run that never tried.
        for (long seed : new long[]{2L, 7L, 21L}) {
            RunState a = lockpickState(seed, 5);
            RunState b = lockpickState(seed, 5);
            new TurnEngine().advance(a, PlayerAction.lockpick(RoguePlayer.EAST));
            new TurnEngine().advance(b, PlayerAction.lockpick(RoguePlayer.EAST));
            assertEquals(a.getFlagStore().get(LockpickSystem.KEY_CELLAR_OPENED),
                            b.getFlagStore().get(LockpickSystem.KEY_CELLAR_OPENED),
                    "seed " + seed + ": same seed, same lockpick outcome (AD-5)");
        }
        RunState c = lockpickState(2L, 5);
        RunState d = lockpickState(2L, 5);
        c.getInventory().remove(Supply.SMALL_TOOLS.ordinal(), 1); // refused: no draw
        new TurnEngine().advance(c, PlayerAction.lockpick(RoguePlayer.EAST));
        assertEquals(c.rng().nextInt(100), d.rng().nextInt(100),
                "a refused lockpick consumes no rng draw (AD-5 honesty)");
    }

    // --- Task 3: queryable, persistent knowledge (AC: 2) ---

    @Test
    void readingAMapFragmentRecordsKnowledgeWithoutATurnOrConsumption() {
        // The Torn Page precedent (Decision 4): reading a fragment is narration — it records
        // persisted knowledge, commits no turn, and is never consumed.
        RunState s = new RunState(42L);
        s.getInventory().tryAdd(Supply.MAP_FRAGMENT.ordinal(), 1);
        int clock = s.getClockTurns();

        new TurnEngine().advance(s, PlayerAction.use(Supply.MAP_FRAGMENT.ordinal(), RoguePlayer.EAST));

        assertEquals(1, KnowledgeSystem.mapFragmentsRead(s), "reading records the knowledge");
        assertEquals(clock, s.getClockTurns(), "reading a fragment commits no turn");
        assertEquals(1, s.getInventory().count(Supply.MAP_FRAGMENT.ordinal()),
                "the fragment is never consumed (re-readable, like the Torn Page)");

        new TurnEngine().advance(s, PlayerAction.use(Supply.MAP_FRAGMENT.ordinal(), RoguePlayer.EAST));
        assertEquals(2, KnowledgeSystem.mapFragmentsRead(s), "re-reading accumulates the knowledge");
        assertEquals(clock, s.getClockTurns(), "a second read still commits no turn");
    }

    @Test
    void theMapKnowledgePersistsAcrossSaveLoadWithNoNewRunStateField() {
        RunState s = new RunState(42L);
        s.getInventory().tryAdd(Supply.MAP_FRAGMENT.ordinal(), 1);
        new TurnEngine().advance(s, PlayerAction.use(Supply.MAP_FRAGMENT.ordinal(), RoguePlayer.EAST));
        new TurnEngine().advance(s, PlayerAction.use(Supply.MAP_FRAGMENT.ordinal(), RoguePlayer.EAST));

        String json = json().toJson(s);
        assertFalse(json.contains("mapFragmentsRead"), "map knowledge is FlagStore-backed, not a new "
                + "RunState field (AD-6): " + json);

        RunState loaded = json().fromJson(RunState.class, json);
        loaded.restoreAfterLoad();
        assertEquals(2, KnowledgeSystem.mapFragmentsRead(loaded), "map knowledge survives the round-trip");
    }

    @Test
    void theLocationDangerQueryAnswersTheAuthoredNightFlips() {
        // Location-danger knowledge is a pure query over Story 3.4's authored data (AD-6): the
        // Graveyard/Sunken Well/Poacher's Camp flip strictly worse at night, the Beehive Grove flips
        // SAFER (NONE, the sole exception), every other structure keeps its authored hazard.
        assertEquals(StructureTable.Hazard.GRAVE_UNDEAD,
                KnowledgeSystem.locationNightHazard(RogueTileMap.STRUCTURE_GRAVEYARD));
        assertEquals(StructureTable.Hazard.WELL_CREATURE,
                KnowledgeSystem.locationNightHazard(RogueTileMap.STRUCTURE_SUNKEN_WELL));
        assertEquals(StructureTable.Hazard.POACHER_PATROL,
                KnowledgeSystem.locationNightHazard(RogueTileMap.STRUCTURE_POACHERS_CAMP));
        assertEquals(StructureTable.Hazard.NONE,
                KnowledgeSystem.locationNightHazard(RogueTileMap.STRUCTURE_BEEHIVE_GROVE));

        assertWorse(dayHazard(RogueTileMap.STRUCTURE_GRAVEYARD), StructureTable.Hazard.GRAVE_UNDEAD);
        assertWorse(dayHazard(RogueTileMap.STRUCTURE_SUNKEN_WELL), StructureTable.Hazard.WELL_CREATURE);
        assertWorse(dayHazard(RogueTileMap.STRUCTURE_POACHERS_CAMP), StructureTable.Hazard.POACHER_PATROL);
        assertEquals(0, KnowledgeSystem.locationNightHazard(RogueTileMap.STRUCTURE_BEEHIVE_GROVE).chancePercent(),
                "the Beehive's night hazard is NONE — strictly safer than the daytime swarm");

        assertEquals(StructureTable.forType(RogueTileMap.STRUCTURE_HUNTERS_BLIND).hazard,
                KnowledgeSystem.locationNightHazard(RogueTileMap.STRUCTURE_HUNTERS_BLIND),
                "a non-flipped structure keeps its authored hazard at night");
        assertEquals(StructureTable.Hazard.NONE, KnowledgeSystem.locationNightHazard(-1),
                "an unmapped structure type knows nothing — NONE, not an NPE");
    }

    @Test
    void itemSafetyKnowledgeStaysQueryableViaIdentifyMap() {
        // The knowledge query composes the existing item-safety store (regression-safe): IdentifyMap
        // still answers what Klein knows about a supply.
        RunState s = new RunState(42L);
        int ws = Supply.SEALED_WATERSKIN.ordinal();
        assertFalse(s.getIdentifyMap().isIdentified(ws), "a fresh run knows nothing");
        s.getIdentifyMap().markIdentified(ws);
        assertTrue(s.getIdentifyMap().isIdentified(ws), "identifying reveals the type");
        TrueIdentity id = s.getIdentifyMap().identityOf(ws);
        assertNotNull(id, "the per-seed binding is queryable");
        assertEquals(id.displayName(), s.getIdentifyMap().displayNameFor(ws),
                "the identified name shows the true identity");
    }

    // --- Task 4: AC-3 — the SM-3 knowledge-survives pin (AC: 3) ---

    @Test
    void theKnowingRunEndsMeasurablyBetterOffWithNoStatRising() {
        // Two runs from ONE seed (a tainted-waterskin seed): the knowing run drinks only identified-
        // safe water (learns the type is tainted on the first sip and stops), reads the map fragment,
        // and opens the cellar via SKILL; the naive run drinks all the risky water blindly and never
        // opens the cellar. The knowing run is measurably better off — more HP (avoided the tainted
        // damage) AND more provisions (the cellar's preserved food) — with NO stat or number rising
        // in either (SM-3: knowledge helps, not XP).
        long seed = seedWhereKnowledgePaysOff();
        RunState k = knowingSetup(seed);
        RunState n = naiveSetup(seed);

        int[] kStats = stats(k);
        int[] nStats = stats(n);

        // Knowing: identify the waterskin type by drinking one (the game-path identification), then
        // stop if it is tainted; read the map fragment; pick the cellar open.
        new TurnEngine().advance(k, PlayerAction.use(Supply.SEALED_WATERSKIN.ordinal(), RoguePlayer.EAST));
        boolean safe = k.getIdentifyMap().identityOf(Supply.SEALED_WATERSKIN.ordinal()) == TrueIdentity.CLEAN_WATER;
        if (safe) {
            new TurnEngine().advance(k, PlayerAction.use(Supply.SEALED_WATERSKIN.ordinal(), RoguePlayer.EAST));
            new TurnEngine().advance(k, PlayerAction.use(Supply.SEALED_WATERSKIN.ordinal(), RoguePlayer.EAST));
        }
        new TurnEngine().advance(k, PlayerAction.use(Supply.MAP_FRAGMENT.ordinal(), RoguePlayer.EAST)); // no turn
        int guard = 0;
        while (k.getFlagStore().get(LockpickSystem.KEY_CELLAR_OPENED) == 0 && guard < 20) {
            new TurnEngine().advance(k, PlayerAction.lockpick(RoguePlayer.EAST));
            guard++;
        }
        assertEquals(1, k.getFlagStore().get(LockpickSystem.KEY_CELLAR_OPENED),
                "the knowing run opens the cellar within the cap on seed " + seed);

        // Naive: drink all three risky waterskins blindly; never opens the cellar.
        new TurnEngine().advance(n, PlayerAction.use(Supply.SEALED_WATERSKIN.ordinal(), RoguePlayer.EAST));
        new TurnEngine().advance(n, PlayerAction.use(Supply.SEALED_WATERSKIN.ordinal(), RoguePlayer.EAST));
        new TurnEngine().advance(n, PlayerAction.use(Supply.SEALED_WATERSKIN.ordinal(), RoguePlayer.EAST));

        // Measurably better off: surviving HP (the tainted water the naive run drank) and more
        // usable provisions (the cellar's preserved food, reachable in the footprint).
        int[] box = footprint(n.getTileMap(), RogueTileMap.STRUCTURE_OLD_HOUSE);
        int kFood = floorItemsOfTypeInBox(k, Supply.PRESERVED_FOOD.ordinal(), box);
        int nFood = floorItemsOfTypeInBox(n, Supply.PRESERVED_FOOD.ordinal(), box);
        assertTrue(k.getPlayer().getHp() > n.getPlayer().getHp(),
                "the knowing run survives better (HP " + k.getPlayer().getHp() + " vs naive "
                        + n.getPlayer().getHp() + ") — knowledge, not a stat");
        assertTrue(kFood > nFood,
                "the knowing run has more usable provisions (cellar " + kFood + " vs naive " + nFood + ")");

        // No stat or number rose in EITHER run (SM-3): every player number is unchanged from start.
        assertUnchanged(kStats, k, "knowing run");
        assertUnchanged(nStats, n, "naive run");
    }

    // --- Task 5: scope guards (AC: all) ---

    @Test
    void theBuildAddsNoPersistedRunStateFieldNoNoiseAndNoSkillGrowth() {
        // AD-6 scope guard: lockpicking + reading add NO new RunState field (FlagStore-backed); no
        // noise (AD-9); no extra clock tick on a read; Small Tools is not consumed (durability is
        // Epic 4); SKILL does not grow from doing (the knowledge-centric scope decision).
        long seed = seedWhereFirstLockpickOpens();
        RunState s = lockpickState(seed, 5);
        s.getInventory().tryAdd(Supply.MAP_FRAGMENT.ordinal(), 1);

        new TurnEngine().advance(s, PlayerAction.lockpick(RoguePlayer.EAST)); // opened: one acted turn
        assertEquals(1, s.getFlagStore().get(LockpickSystem.KEY_CELLAR_OPENED));
        int clock = s.getClockTurns();
        int tools = s.getInventory().count(Supply.SMALL_TOOLS.ordinal());
        int skill = s.getPlayer().getSkill();

        new TurnEngine().advance(s, PlayerAction.use(Supply.MAP_FRAGMENT.ordinal(), RoguePlayer.EAST)); // no turn

        assertEquals(clock, s.getClockTurns(), "reading a fragment adds no clock tick");
        assertEquals(tools, s.getInventory().count(Supply.SMALL_TOOLS.ordinal()),
                "Small Tools is not consumed (durability/repair is Epic 4)");
        assertEquals(skill, s.getPlayer().getSkill(),
                "lockpicking does not grow SKILL (no leveling-by-doing)");
        assertTrue(s.getNoiseQueue().isEmpty(), "neither lockpicking nor reading emits noise (AD-9)");

        String json = json().toJson(s);
        assertFalse(json.contains("mapFragmentsRead"), "map knowledge rides FlagStore (AD-6): " + json);
        assertFalse(json.contains("cellarOpened"), "cellar state rides FlagStore (AD-6): " + json);
        assertFalse(json.contains("lockedCellar"), "the cellar loot stays authored data (AD-6): " + json);
    }

    // --- helpers ---

    /** A cleared run with the player on open floor and no ambient actors (the CombatTest pattern). */
    private static RunState killState() {
        RunState s = new RunState(42L);
        RogueTileMap m = s.getTileMap();
        s.getPlayer().placeAt(25, 25);
        for (int x = 19; x <= 33; x++)
            for (int y = 19; y <= 33; y++)
                m.setTile(x, y, RogueTile.FLOOR);
        s.getEnemies().clear();
        s.getCompanions().clear();
        return s;
    }

    /** A run pinned for lockpicking: player at a walkable Old House cell, Small Tools in hand,
     *  SKILL pinned, weather CLEAR, no ambient actors. */
    private static RunState lockpickState(long seed, int skill) {
        RunState s = new RunState(seed);
        s.setWeather(Weather.CLEAR);
        int[] cell = oldHouseCell(s.getTileMap());
        s.getPlayer().placeAt(cell[0], cell[1]);
        s.getPlayer().setSkill(skill);
        s.getEnemies().clear();
        s.getCompanions().clear();
        s.getInventory().tryAdd(Supply.SMALL_TOOLS.ordinal(), 1);
        return s;
    }

    private static boolean firstLockpickOpens(long seed, int skill) {
        RunState s = lockpickState(seed, skill);
        new TurnEngine().advance(s, PlayerAction.lockpick(RoguePlayer.EAST));
        return s.getFlagStore().get(LockpickSystem.KEY_CELLAR_OPENED) != 0;
    }

    private static long seedWhereFirstLockpickOpens() {
        for (long seed = 0; seed < 80; seed++) {
            if (firstLockpickOpens(seed, 5)) return seed;
        }
        fail("no seed in 0..80 opens the cellar on the first attempt at SKILL 5");
        return -1;
    }

    private static long seedWhereFirstLockpickFails() {
        for (long seed = 0; seed < 80; seed++) {
            if (!firstLockpickOpens(seed, 5)) return seed;
        }
        fail("no seed in 0..80 leaves the cellar locked on the first attempt at SKILL 5");
        return -1;
    }

    /** A run at the Old House with a tainted waterskin's risk and a fragment: the knowing policy's
     *  setup (also used as the seed probe). */
    private static RunState knowingSetup(long seed) {
        RunState s = new RunState(seed);
        s.setWeather(Weather.CLEAR);
        int[] cell = oldHouseCell(s.getTileMap());
        s.getPlayer().placeAt(cell[0], cell[1]);
        s.getPlayer().setSkill(5);
        s.getEnemies().clear();
        s.getCompanions().clear();
        s.getInventory().tryAdd(Supply.SEALED_WATERSKIN.ordinal(), 3);
        s.getInventory().tryAdd(Supply.SMALL_TOOLS.ordinal(), 1);
        s.getInventory().tryAdd(Supply.MAP_FRAGMENT.ordinal(), 1);
        return s;
    }

    private static RunState naiveSetup(long seed) {
        RunState s = knowingSetup(seed);
        s.getInventory().remove(Supply.MAP_FRAGMENT.ordinal(), 1); // the naive run reads nothing
        return s;
    }

    /** The first seed whose waterskins bind TAINTED AND whose cellar opens within the knowing
     *  policy's cap — the AC-3 comparison is deterministic on it (AD-5). */
    private static long seedWhereKnowledgePaysOff() {
        for (long seed = 0; seed < 60; seed++) {
            if (new RunState(seed).getIdentifyMap().identityOf(Supply.SEALED_WATERSKIN.ordinal())
                    != TrueIdentity.TAINTED) continue;
            RunState probe = knowingSetup(seed);
            int guard = 0;
            while (probe.getFlagStore().get(LockpickSystem.KEY_CELLAR_OPENED) == 0 && guard < 20) {
                new TurnEngine().advance(probe, PlayerAction.lockpick(RoguePlayer.EAST));
                guard++;
            }
            if (probe.getFlagStore().get(LockpickSystem.KEY_CELLAR_OPENED) != 0) return seed;
        }
        fail("no seed in 0..60 combines a tainted waterskin with an openable cellar");
        return -1;
    }

    /** Snapshot of the five stats + max-HP (the numbers AC-1/AC-3 forbid rising). */
    private static int[] stats(RunState s) {
        RoguePlayer p = s.getPlayer();
        return new int[]{p.getStr(), p.getGrit(), p.getInstinct(), p.getVoice(), p.getSkill(), p.getMaxHp()};
    }

    private static void assertUnchanged(int[] before, RunState s, String label) {
        int[] after = stats(s);
        for (int i = 0; i < before.length; i++) {
            assertEquals(before[i], after[i], label + ": player number " + i + " unchanged");
        }
    }

    private static StructureTable.Hazard dayHazard(int structureType) {
        return StructureTable.forType(structureType).hazard;
    }

    private static void assertWorse(StructureTable.Hazard day, StructureTable.Hazard night) {
        assertTrue(night.chancePercent() >= day.chancePercent() && night.damage() >= day.damage()
                        && (night.chancePercent() > day.chancePercent() || night.damage() > day.damage()),
                night + " is strictly worse than " + day
                        + " (" + night.chancePercent() + "%/" + night.damage()
                        + " vs " + day.chancePercent() + "%/" + day.damage() + ")");
    }

    /** A walkable Old House cell (the lockpick standing spot), or null. */
    private static int[] oldHouseCell(RogueTileMap m) {
        for (int x = 0; x < m.getWidth(); x++) {
            for (int y = 0; y < m.getHeight(); y++) {
                if (m.getStructureType(x, y) == RogueTileMap.STRUCTURE_OLD_HOUSE && m.isWalkable(x, y)) {
                    return new int[]{x, y};
                }
            }
        }
        return null;
    }

    private static int[] footprint(RogueTileMap m, int type) {
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

    private static int floorItemsOfTypeInBox(RunState s, int type, int[] box) {
        if (box == null) return 0;
        int n = 0;
        for (FloorItem it : s.getFloorItems()) {
            if (it.type == type && it.x >= box[0] && it.x <= box[2] && it.y >= box[1] && it.y <= box[3]) {
                n += it.count;
            }
        }
        return n;
    }

    private static boolean hasItemAt(RunState s, int type, int x, int y) {
        for (FloorItem it : s.getFloorItems()) {
            if (it.type == type && it.x == x && it.y == y) return true;
        }
        return false;
    }

    /** Mirrors RunStatePersistenceTest.json() — the production serializer (AD-6). */
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

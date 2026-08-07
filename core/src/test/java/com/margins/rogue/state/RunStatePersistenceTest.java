package com.margins.rogue.state;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import com.margins.rogue.Companion;
import com.margins.rogue.DayPhase;
import com.margins.rogue.RogueEnemy;
import com.margins.rogue.RoguePlayer;
import com.margins.rogue.Weather;
import com.margins.rogue.item.FloorItem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Whole-RunState serialization (AD-6): the FlagStore + companions ride the save
 * root, and a save predating a field loads empty-but-non-null.
 */
class RunStatePersistenceTest {

    /** Mirrors SaveService.json() element-type registration AND usePrototypes(false) (Story 1.1),
     *  so the test serializer matches production (the divergence would mask save-format bugs). */
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

    @Test
    void flagsSurviveRoundTrip() {
        RunState s = new RunState(42L);
        s.getFlagStore().set(FlagStore.KEY_BOND, 2);
        s.getFlagStore().set("cache.revealed", 1);

        RunState loaded = json().fromJson(RunState.class, json().toJson(s));
        loaded.restoreAfterLoad();

        assertNotNull(loaded.getFlagStore());
        assertEquals(2, loaded.getFlagStore().getBond(), "Bond persists as an int, not a Double/String");
        assertEquals(1, loaded.getFlagStore().get("cache.revealed"), "scene flags persist");
        loaded.getFlagStore().applyBondTag(FlagStore.BOND_TAG_HONEST);
        assertEquals(3, loaded.getFlagStore().getBond(), "the store is usable after restoreAfterLoad");
    }

    @Test
    void restartResetsNarrativeState() {
        RunState s = new RunState(42L);
        s.getFlagStore().set(FlagStore.KEY_BOND, 2);
        s.getFlagStore().set("cache.revealed", 1);
        s.restart();
        assertEquals(0, s.getFlagStore().getBond(), "a new run resets Bond (run-scoped, AD-7)");
        assertEquals(0, s.getFlagStore().get("cache.revealed"), "a new run clears scene flags");
    }

    @Test
    void preFlagStoreSaveLoadsEmptyNotNull() {
        RunState withData = new RunState(7L);
        withData.getFlagStore().set(FlagStore.KEY_BOND, 3);
        JsonValue root = new JsonReader().parse(json().toJson(withData));
        root.remove("flagStore"); // emulate a save written before the field existed

        RunState fromOld = json().fromJson(RunState.class, root.toJson(JsonWriter.OutputType.json));
        fromOld.restoreAfterLoad();

        assertNotNull(fromOld.getFlagStore(), "field-initialized, so an old save is non-null");
        assertEquals(0, fromOld.getFlagStore().getBond(), "old save has an empty store");
    }

    @Test
    void vitalsInventoryIdentitiesAndLastStandSurviveRoundTrip() {
        // AC-5 (Story 1.1): the continuous-map migration must not disturb the run's
        // core serialized state. Ported from the deleted RouteProgressionTest, minus floors.
        RunState s = new RunState(42L);
        s.getPlayer().takeDamage(4);                       // non-default HP
        s.getInventory().tryAdd(0, 2);                     // a stacked item
        s.getIdentifyMap().markIdentified(0);              // a revealed identity
        s.setLastStandUsed(true);                          // the reprieve spent

        int hp = s.getPlayer().getHp();
        int hunger = s.getPlayer().getHunger();
        int itemCount = s.getInventory().count(0);

        RunState loaded = json().fromJson(RunState.class, json().toJson(s));
        loaded.restoreAfterLoad();

        assertEquals(hp, loaded.getPlayer().getHp(), "HP survives round-trip");
        assertEquals(s.getPlayer().getMaxHp(), loaded.getPlayer().getMaxHp(), "max HP survives");
        assertEquals(hunger, loaded.getPlayer().getHunger(), "hunger survives");
        assertEquals(itemCount, loaded.getInventory().count(0), "inventory count survives");
        assertTrue(loaded.getIdentifyMap().isIdentified(0), "identity reveal survives");
        assertTrue(loaded.isLastStandUsed(), "the spent Last-Stand survives (no free reprieve on reload)");
    }

    @Test
    void thirstTemperatureAndClockSurviveRoundTrip() {
        // AC (Story 1.2 / AD-6): the new survival tracks are persisted state and must round-trip.
        RunState s = new RunState(42L);
        for (int i = 0; i < 210; i++) s.getPlayer().tickThirst(); // drop into Thirsty
        s.getPlayer().adjustTemperature(-30);                     // Chilled
        s.tickClock(); s.tickClock(); s.tickClock();             // clock = 3

        RoguePlayer.ThirstStatus thirst = s.getPlayer().getThirstStatus();
        int thirstTurns = s.getPlayer().getThirst();
        int temp = s.getPlayer().getTemperature();

        RunState loaded = json().fromJson(RunState.class, json().toJson(s));
        loaded.restoreAfterLoad();

        assertEquals(thirst, loaded.getPlayer().getThirstStatus(), "thirst tier survives");
        assertEquals(thirstTurns, loaded.getPlayer().getThirst(), "thirst countdown survives");
        assertEquals(temp, loaded.getPlayer().getTemperature(), "temperature survives");
        assertEquals(3, loaded.getClockTurns(), "the Day/Night clock survives");
    }

    @Test
    void weatherAndCycleSurviveRoundTrip() {
        // AC (Story 1.3 / AD-6): weather + cycle are persisted state — a load must keep them,
        // never re-roll (that would break the "per cycle" contract on resume).
        RunState s = new RunState(42L);
        for (int i = 0; i < 170; i++) s.tickClock(); // into cycle 1 (Day)
        assertEquals(1, s.getCycleNumber());
        Weather weather = s.getWeather();

        RunState loaded = json().fromJson(RunState.class, json().toJson(s));
        loaded.restoreAfterLoad();

        assertEquals(170, loaded.getClockTurns(), "the clock survives");
        assertEquals(weather, loaded.getWeather(), "weather survives and is not re-rolled on load");
        assertEquals(1, loaded.getCycleNumber(), "the cycle survives");
        assertEquals(DayPhase.DAY, loaded.getClockPhase(), "the phase derives correctly after load");
    }

    @Test
    void preWeatherSaveLoadsNonNullAndCycleZero() {
        // AD-6: a save predating the weather/cycle fields (pre-1.3) must load non-crashing with a
        // sane cycle. NOTE: the no-arg RunState() ctor (which libGDX Json runs) delegates to the
        // full seeded ctor, so its rollWeather() sets a nanoTime-rolled weather for a field-absent
        // save — the same documented migration wart as identifyMap (deferred 3.3 item). We assert
        // non-null + cycle 0, not a specific weather.
        RunState withData = new RunState(7L);
        JsonValue root = new JsonReader().parse(json().toJson(withData));
        root.remove("weather");
        root.remove("cycleNumber"); // emulate a save written before the fields existed

        RunState fromOld = json().fromJson(RunState.class, root.toJson(JsonWriter.OutputType.json));
        fromOld.restoreAfterLoad();

        assertNotNull(fromOld.getWeather(), "old save loads non-null weather (never a crash)");
        assertEquals(0, fromOld.getCycleNumber(), "old save loads cycle 0");
        assertEquals(DayPhase.DAY, fromOld.getClockPhase(), "turn 0 is Day regardless");
    }

    @Test
    void preWeatherSaveDeepInACycleDerivesTheCycleFromTheClock() {
        // Edge Case Med (review): a pre-1.3 save could hold clockTurns deep in a later cycle
        // (the clock substrate shipped in 1.2) while lacking the weather/cycleNumber fields.
        // The field-absent ctor-roll leaves a nanoTime weather (documented wart, same as
        // identifyMap), but cycleNumber MUST derive from the persisted clock — before the
        // review patch it loaded stale 0 and desynced the run's cycle from its weather.
        RunState withData = new RunState(7L);
        for (int i = 0; i < 170; i++) withData.tickClock(); // deep into cycle 1
        assertEquals(1, withData.getCycleNumber());
        JsonValue root = new JsonReader().parse(json().toJson(withData));
        root.remove("weather");
        root.remove("cycleNumber"); // emulate a save written before the fields existed

        RunState fromOld = json().fromJson(RunState.class, root.toJson(JsonWriter.OutputType.json));
        fromOld.restoreAfterLoad();

        assertEquals(170, fromOld.getClockTurns(), "the persisted clock survives the migration");
        assertEquals(1, fromOld.getCycleNumber(), "cycleNumber derives from the persisted clock, not stale 0");
        assertEquals(DayPhase.DAY, fromOld.getClockPhase(), "turn 170 is Day of cycle 1 (self-consistent)");
        assertNotNull(fromOld.getWeather(), "old save still loads non-null weather (never a crash)");
    }

    @Test
    void lightSurvivesRoundTrip() {
        // AD-6 (Story 1.4): a lit camp is persisted world state — a load keeps the fire on its tile.
        RunState s = new RunState(42L);
        s.setLight(13, 21);

        RunState loaded = json().fromJson(RunState.class, json().toJson(s));
        loaded.restoreAfterLoad();

        assertTrue(loaded.hasLight(), "the lit source survives the load");
        assertEquals(13, loaded.getLightX(), "the light's tile survives");
        assertEquals(21, loaded.getLightY(), "the light's tile survives");
    }

    @Test
    void preLightSaveLoadsWithoutLight() {
        // AD-6: a save predating the light fields (pre-1.4) loads light-less — the honest default,
        // never a crash (the -1 sentinel is the field initializer libGDX Json runs on a missing key).
        RunState withData = new RunState(7L);
        JsonValue root = new JsonReader().parse(json().toJson(withData));
        root.remove("lightX");
        root.remove("lightY"); // emulate a save written before the fields existed

        RunState fromOld = json().fromJson(RunState.class, root.toJson(JsonWriter.OutputType.json));
        fromOld.restoreAfterLoad();

        assertFalse(fromOld.hasLight(), "an old save loads with no lit source");
        assertEquals(-1, fromOld.getLightX(), "the light's x defaults to the none sentinel");
        assertEquals(-1, fromOld.getLightY(), "the light's y defaults to the none sentinel");
    }

    @Test
    void companionSurvivesRoundTripWithMapReinjected() {
        RunState s = new RunState(42L);
        assertNotNull(s.getActiveCompanion(), "a run starts with Galleon");
        RunState loaded = json().fromJson(RunState.class, json().toJson(s));
        loaded.restoreAfterLoad(); // re-injects the transient tilemap into companions
        Companion c = loaded.getActiveCompanion();
        assertNotNull(c);
        // followStep would NPE if the map were not re-injected — exercise it.
        assertDoesNotThrow(() -> c.followStep(loaded.getPlayer().getTileX(), loaded.getPlayer().getTileY()));
    }
}

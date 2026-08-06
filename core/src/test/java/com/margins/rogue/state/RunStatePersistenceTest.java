package com.margins.rogue.state;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import com.margins.rogue.Companion;
import com.margins.rogue.RogueEnemy;
import com.margins.rogue.item.FloorItem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Whole-RunState serialization (AD-6): the FlagStore + companions ride the save
 * root, and a save predating a field loads empty-but-non-null.
 */
class RunStatePersistenceTest {

    /** Mirrors SaveService.json() element-type registration. */
    private static Json json() {
        Json json = new Json();
        json.setOutputType(JsonWriter.OutputType.json);
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

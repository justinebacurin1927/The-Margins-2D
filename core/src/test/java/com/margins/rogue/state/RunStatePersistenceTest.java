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

package com.margins.rogue.save;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import com.margins.rogue.Companion;
import com.margins.rogue.RogueEnemy;
import com.margins.rogue.item.FloorItem;
import com.margins.rogue.state.FlagStore;
import com.margins.rogue.state.RunState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AD-6 save-format guard for the AD-8 migration: a pre-AD-8 (floor-descent) save
 * is rejected by the <em>absence</em> of the {@code saveVersion} key (every current
 * save writes it; old saves never had the field). It is not gated on the loaded
 * {@code saveVersion} int — {@code fromJson} runs field initializers, so any loaded
 * run reports the current version regardless of the source save.
 */
class SaveMigrationTest {

    /** Mirrors SaveService.json(): prototype-omission OFF so every field (incl. saveVersion) is written. */
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
    void currentSaveStampsSaveVersionAndIsAccepted() {
        String currentJson = json().toJson(new RunState(42L));

        JsonValue root = new JsonReader().parse(currentJson);
        assertTrue(root.has("saveVersion"), "the new save always stamps saveVersion (prototype-omission off)");
        assertFalse(root.has("floorDepth"), "the new save shape never writes floorDepth");

        assertFalse(SaveService.isPreMigrationSave(currentJson), "a current save is accepted");
    }

    @Test
    void currentSaveRoundTripsSaveVersion() {
        RunState loaded = json().fromJson(RunState.class, json().toJson(new RunState(42L)));
        assertEquals(RunState.SAVE_VERSION, loaded.getSaveVersion(), "saveVersion round-trips");
    }

    @Test
    void preMigrationSaveWithoutSaveVersionIsRejected() {
        // Emulate a pre-AD-8 save: a floor-descent blob that predates the saveVersion field
        // and carries the old floorDepth key. Its defining trait is the MISSING saveVersion.
        JsonValue root = new JsonReader().parse(json().toJson(new RunState(7L)));
        root.remove("saveVersion");
        root.addChild("floorDepth", new JsonValue(2L)); // representative of an old save's shape

        String oldJson = root.toJson(JsonWriter.OutputType.json);
        assertTrue(SaveService.isPreMigrationSave(oldJson),
                "a save lacking saveVersion is a pre-AD-8 save and must be rejected");
    }

    @Test
    void oldFloorOneSaveWithoutFloorDepthKeyIsStillRejected() {
        // The subtle case: an old save taken on floor 1 omitted floorDepth (prototype default),
        // so a floorDepth-based guard would miss it. The saveVersion-absence guard still catches it.
        JsonValue root = new JsonReader().parse(json().toJson(new RunState(3L)));
        root.remove("saveVersion"); // predates the field; NO floorDepth key added

        String oldFloorOneJson = root.toJson(JsonWriter.OutputType.json);
        assertFalse(new JsonReader().parse(oldFloorOneJson).has("floorDepth"), "floor-1 old save had no floorDepth key");
        assertTrue(SaveService.isPreMigrationSave(oldFloorOneJson),
                "an old floor-1 save is still rejected via missing saveVersion");
    }

    @Test
    void handWrittenPreMigrationShapeIsRejected() {
        // A genuine floor-descent save shape — authored independently of the current class,
        // with the retired route/floorDepth fields and no saveVersion — is rejected.
        String oldShape = "{\"seed\":123,\"floorDepth\":2,"
                + "\"route\":{\"name\":\"The Caravan Road\",\"floorCount\":3},"
                + "\"lastStandUsed\":false}";
        assertTrue(SaveService.isPreMigrationSave(oldShape),
                "a real pre-AD-8 save shape (route + floorDepth, no saveVersion) is rejected");
    }

    @Test
    void nullCorruptOrNonObjectSaveIsRejectedNotCrashing() {
        assertTrue(SaveService.isPreMigrationSave(null), "null → rejected, no NPE");
        assertTrue(SaveService.isPreMigrationSave("{ not valid json"), "corrupt → rejected, no throw");
        assertTrue(SaveService.isPreMigrationSave("[1,2,3]"), "non-object root → rejected");
        assertTrue(SaveService.isPreMigrationSave(""), "empty → rejected");
    }
}

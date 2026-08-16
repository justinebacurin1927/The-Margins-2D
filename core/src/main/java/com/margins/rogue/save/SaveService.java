package com.margins.rogue.save;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import com.margins.rogue.Companion;
import com.margins.rogue.RogueEnemy;
import com.margins.rogue.item.Bag;
import com.margins.rogue.item.FloorItem;
import com.margins.rogue.item.Inventory;
import com.margins.rogue.item.Weapon;
import com.margins.rogue.state.FlagStore;
import com.margins.rogue.state.RunState;

/**
 * Single-slot save/resume of the whole run (AD-6). {@link RunState} is the sole
 * serialization root; the tilemap serializes once (entities hold it transiently)
 * and the RNG is rebuilt from the stored seed on load, so nothing is
 * double-serialized (save serialization-root convention, NFR-4).
 */
public final class SaveService {
    private static final String SAVE_PATH = "save/run.json";

    private SaveService() {}

    private static Json json() {
        Json json = new Json();
        json.setOutputType(JsonWriter.OutputType.json);
        // Write every field explicitly (AD-6): with prototype-omission on (libGDX's default),
        // a field equal to a fresh instance's value is skipped — which would drop saveVersion
        // (always == its default) and make the migration guard unreliable. Off = explicit saves.
        json.setUsePrototypes(false);
        json.setElementType(RunState.class, "enemies", RogueEnemy.class);
        json.setElementType(RunState.class, "floorItems", FloorItem.class);
        json.setElementType(RunState.class, "companions", Companion.class);
        json.setElementType(RunState.class, "weapons", Weapon.class); // Story 4.4: per-item durability round-trips
        json.setElementType(Inventory.class, "storageBags", Bag.class); // Story 6.2: readied bags (durability + trap) round-trip
        // For a map field, setElementType registers the value type — FlagStore.flags
        // deserializes as Integer, not Double/String (AD-6).
        json.setElementType(FlagStore.class, "flags", Integer.class);
        return json;
    }

    public static boolean hasSave() {
        return Gdx.files.local(SAVE_PATH).exists();
    }

    public static void save(RunState state) {
        Gdx.files.local(SAVE_PATH).writeString(json().toJson(state), false);
    }

    /**
     * Load the saved run and re-wire its transient fields, or null if no slot
     * exists or the save predates the AD-8 continuous-map migration.
     *
     * <p>Pre-AD-8 saves are rejected by the <em>absence</em> of the {@code saveVersion}
     * key. That key can't be checked via the loaded object — {@code fromJson} runs field
     * initializers, so any loaded run reports the current version — so the raw JSON is
     * inspected instead. Every current save writes {@code saveVersion} (prototype-omission
     * is off, see {@link #json()}); a floor-descent save never had the field, so its absence
     * uniquely marks a pre-AD-8 save (independent of what floor it was saved on).
     */
    public static RunState load() {
        FileHandle f = Gdx.files.local(SAVE_PATH);
        if (!f.exists()) return null;
        String raw = f.readString();
        if (isPreMigrationSave(raw)) return null; // never load a floor-descent save onto the continuous map (AD-6)
        RunState state;
        try {
            state = json().fromJson(RunState.class, raw);
        } catch (RuntimeException e) {
            return null; // a current save that's corrupt on disk → no run, don't crash (AD-6)
        }
        state.restoreAfterLoad();
        return state;
    }

    /**
     * True if the raw save JSON is not a loadable current save: a pre-AD-8 (floor-descent)
     * save lacks the {@code saveVersion} key every current save writes, and a null / corrupt /
     * non-object blob is likewise rejected rather than crashing the load. Headless-testable
     * (no {@code Gdx.files}): the reject rule lives here so it can be unit-tested directly.
     */
    public static boolean isPreMigrationSave(String rawJson) {
        if (rawJson == null) return true;
        JsonValue root;
        try {
            root = new JsonReader().parse(rawJson);
        } catch (RuntimeException e) {
            return true; // unparseable/corrupt → reject, never load
        }
        return root == null || !root.isObject() || !root.has("saveVersion");
    }

    /** Delete the save slot — called on true death so a dead run can't be reloaded (AD-6, FR-21). */
    public static void deleteSave() {
        Gdx.files.local(SAVE_PATH).delete();
    }
}

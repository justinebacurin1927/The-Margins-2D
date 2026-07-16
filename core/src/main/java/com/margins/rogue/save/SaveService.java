package com.margins.rogue.save;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import com.margins.rogue.RogueEnemy;
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
        json.setElementType(RunState.class, "enemies", RogueEnemy.class);
        return json;
    }

    public static boolean hasSave() {
        return Gdx.files.local(SAVE_PATH).exists();
    }

    public static void save(RunState state) {
        Gdx.files.local(SAVE_PATH).writeString(json().toJson(state), false);
    }

    /** Load the saved run and re-wire its transient fields, or null if no slot exists. */
    public static RunState load() {
        FileHandle f = Gdx.files.local(SAVE_PATH);
        if (!f.exists()) return null;
        RunState state = json().fromJson(RunState.class, f.readString());
        state.restoreAfterLoad();
        return state;
    }

    /** Delete the save slot — called on true death so a dead run can't be reloaded (AD-6, FR-21). */
    public static void deleteSave() {
        Gdx.files.local(SAVE_PATH).delete();
    }
}

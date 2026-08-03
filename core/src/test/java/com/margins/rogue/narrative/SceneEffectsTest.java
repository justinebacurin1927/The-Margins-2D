package com.margins.rogue.narrative;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import com.margins.rogue.Companion;
import com.margins.rogue.RogueEnemy;
import com.margins.rogue.item.FloorItem;
import com.margins.rogue.state.FlagStore;
import com.margins.rogue.state.RunState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Story 5.3 — the "cache revealed" flag gates a one-shot content spawn that persists (FR-8). */
class SceneEffectsTest {

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
    void revealedCacheSpawnsExactlyOnce() {
        RunState s = new RunState(42L);
        s.getFlagStore().set(SceneEffects.KEY_CACHE_REVEALED, 1);
        int before = s.getFloorItems().size();

        SceneEffects.applyCacheReveal(s);
        assertEquals(before + 1, s.getFloorItems().size(), "the cache drops one item");
        assertEquals(1, s.getFlagStore().get(SceneEffects.KEY_CACHE_SPAWNED), "the one-shot guard is set");

        SceneEffects.applyCacheReveal(s);
        assertEquals(before + 1, s.getFloorItems().size(), "a second call spawns nothing");
    }

    @Test
    void unrevealedCacheDoesNotSpawn() {
        RunState s = new RunState(7L);
        int before = s.getFloorItems().size();
        SceneEffects.applyCacheReveal(s);
        assertEquals(before, s.getFloorItems().size(), "no reveal flag → no spawn");
    }

    @Test
    void guardPersistsSoNoRespawnAfterReload() {
        RunState s = new RunState(123L);
        s.getFlagStore().set(SceneEffects.KEY_CACHE_REVEALED, 1);
        SceneEffects.applyCacheReveal(s); // spawns + sets the guard
        int items = s.getFloorItems().size();

        RunState loaded = json().fromJson(RunState.class, json().toJson(s));
        loaded.restoreAfterLoad();

        assertEquals(1, loaded.getFlagStore().get(SceneEffects.KEY_CACHE_REVEALED), "reveal flag persists");
        assertEquals(1, loaded.getFlagStore().get(SceneEffects.KEY_CACHE_SPAWNED), "spawned guard persists");
        assertEquals(items, loaded.getFloorItems().size(), "the spawned item round-trips");

        SceneEffects.applyCacheReveal(loaded);
        assertEquals(items, loaded.getFloorItems().size(), "no re-spawn after reload");
    }
}

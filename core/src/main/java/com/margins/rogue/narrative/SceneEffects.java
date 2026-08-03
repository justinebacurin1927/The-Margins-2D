package com.margins.rogue.narrative;

import com.margins.rogue.RoguePlayer;
import com.margins.rogue.state.FlagStore;
import com.margins.rogue.state.RunState;

/**
 * Content that reads run-scoped flags and gates on them (FR-8, AD-7). The single
 * authority for scene flag keys. Pure model — reads/writes only through the
 * {@code RunState} FlagStore, no libGDX.
 */
public final class SceneEffects {

    /** Set by an authored scene when the hidden cache is revealed (FR-8). */
    public static final String KEY_CACHE_REVEALED = "scene.cache.revealed";
    /** One-shot guard: the cache contents have been spawned (persists, so no re-spawn on reload). */
    public static final String KEY_CACHE_SPAWNED = "scene.cache.spawned";

    private SceneEffects() {}

    /**
     * If the cache has been revealed and not yet spawned, drop its contents at the
     * player's tile and mark it spawned (FR-8). One-shot across turns and reloads —
     * the guard flag is persisted with the run. No-op when the reveal flag is unset.
     */
    public static void applyCacheReveal(RunState state) {
        FlagStore fs = state.getFlagStore();
        if (fs.get(KEY_CACHE_REVEALED) == 1 && fs.get(KEY_CACHE_SPAWNED) == 0) {
            RoguePlayer p = state.getPlayer();
            state.addFloorItem(0, 1, p.getTileX(), p.getTileY()); // the cache contents (one supply stack)
            fs.set(KEY_CACHE_SPAWNED, 1);
        }
    }
}

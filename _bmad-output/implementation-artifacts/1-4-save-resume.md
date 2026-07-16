# Story 1.4: Save and resume a run

Status: done

## Story

As Justine (player),
I want to quit mid-run and pick up exactly where I left off,
so that a play session can span more than one sitting.

## Acceptance Criteria

1. Given an in-progress run, when I save and relaunch, the exact `RunState` is restored (floor, positions, inventory, identified supplies, flags, Bond, hunger, HP, last-stand-used, seed) and play continues from the saved turn. (FR-20, AD-6)
2. Serialization uses libGDX `Json` with `RunState` as the sole root; the tilemap and RNG are not double-serialized via entity back-references. (AD-6, save serialization-root convention, NFR-4)
3. A save is written at a sensible cadence (at minimum on quit/hide; ideally each turn or on floor transition).

## Tasks / Subtasks

- [x] Task 1: Create `save/SaveService.java` with `save(RunState)` and `RunState load()` using `com.badlogic.gdx.utils.Json` to a single slot file (`save/run.json`) under the libGDX local storage path; also `hasSave()` (AC: 1, 2)
- [x] Task 2: Make `RunState` serializable-clean (AC: 2)
  - [x] `RoguePlayer.map` and `RogueEnemy.map` are now `transient`; re-injected via `setMap()` in `RunState.restoreAfterLoad()`. (`RoguePlayer.rand` was already removed in Story 1.3.) `RogueTileMap` de-`final`ed + given a private no-arg ctor so Json can reconstruct it once, under the single root.
  - [x] RNG is `transient` and rebuilt from the stored `seed` in `restoreAfterLoad()`. Accepted the documented tradeoff: layout/bindings are seed-reproducible; future draws restart from the seed (AC-1 requires layout reproducibility, not mid-run draw parity).
- [x] Task 3: Wire save triggers — `SaveService.save(state)` on `pause()` and `hide()` (both fire on desktop window-close); `SaveService.load()` on `show()`, falling back to `new RunState()` when no slot exists (AC: 3). Save is skipped while dead (`gameOver`) — explicit delete-on-death lands in Story 1.5.
- [x] Task 4: Verified via headless round-trips (both stronger than manual): (a) Json string round-trip — exact field restore, `tiles` serialized exactly once, no entity `map` back-ref; (b) real disk round-trip through `SaveService` with a `Lwjgl3Files` backend — save then fresh `load()` restored position/HP/enemies/seed exactly with rng rebuilt. Live launch boots clean.

## Dev Notes

### Governing architecture
- **AD-6** — save = serialize whole `RunState` to one slot via libGDX `Json`; load = deserialize; no seed+action replay.
- **AD-3** — `RunState` is the single owner, hence the single serialization root.
- **Save serialization-root convention** — entities carry NO serialized back-refs to `RunState`-owned aggregates (tilemap, RNG); transient + re-wired on load. This closes the adversarial-gate hole logged in the architecture memlog.

### Current-state hazard
`RoguePlayer` holds `RogueTileMap map` and `Random rand`. Left as-is, libGDX `Json` would serialize the whole map graph under the player AND under `RunState`, duplicating it and forking the RNG. Mark these transient and re-inject on load (Task 2). Same audit for `RogueEnemy` (holds a `map` ref too).

### Depends on
Stories 1.1 (RunState), 1.3 (seed on RunState). Fields for inventory/identify/flags/Bond/last-stand won't all exist yet (they arrive in Epics 3/4/5 and Story 1.6) — design `RunState` serialization to naturally include whatever fields exist, so later epics get persistence for free. Re-verify AC-1's full field list as those epics land.

### Testing standards
A headless round-trip test (`save` then `load`, assert field equality) is the ideal guardrail and is possible because `RunState` is render-free (NFR-3/4). Add if a test dep exists.

### References
- [Source: _bmad-output/planning-artifacts/epics.md#Epic 1 · Story 1.4]
- [Source: ARCHITECTURE-SPINE.md#AD-6, #Consistency Conventions (Serialization root)]
- [Source: PRD FR-20; core/src/main/java/com/margins/rogue/RoguePlayer.java]

## Dev Agent Record

### Agent Model Used

claude-opus-4-8[1m] (via bmad-dev-story)

### Debug Log References

- `mvn -o compile` → BUILD SUCCESS
- Headless Json round-trip: `fields match: true`, `"tiles" appears once: true`, `no entity 'map' back-ref: true`, `rng rebuilt: true`, `map re-injected: true` → AC-1 + AC-2 PASS
- Real-disk round-trip via `Lwjgl3Files`: no-slot→`null`, save→`hasSave true`, fresh `load()` restored pos/HP/enemies/seed exactly, rng non-null → FILE ROUND-TRIP PASS
- Launch on display :0, 10s → clean boot (no serialization exceptions)

### Completion Notes List

- `SaveService` (new, `rogue/save/`) serializes the whole `RunState` to a single slot `save/run.json` via libGDX `Json` with `RunState` as the sole root (AD-6). Enemy list element type registered so the collection reads back without per-element class tags.
- Serialization-root convention enforced: `RoguePlayer.map` and `RogueEnemy.map` are `transient` (were live back-refs that would have duplicated the whole map graph under each entity). The tilemap serializes exactly once under `RunState`; `restoreAfterLoad()` re-injects it into player + enemies and rebuilds the RNG from `seed`.
- `RogueTileMap` had `final` fields + only a `(w,h)` ctor — Json can't set finals or instantiate without a no-arg ctor; de-`final`ed the three fields and added a private no-arg ctor. `RoguePlayer`/`RogueEnemy` also got private no-arg ctors for Json.
- Screen wiring: `show()` loads an existing slot or starts fresh; `pause()`/`hide()` persist the run (guarded by `!gameOver`). Both lifecycle hooks fire on desktop close.
- Forward-compat: because Json auto-serializes all non-transient fields, inventory / identify-map / flags / Bond / last-stand fields arriving in Epics 3–5 and Story 1.6 will persist automatically once added to `RunState`; re-verify AC-1's full field list as they land.
- Note: `RogueTileMap.getTile` treats out-of-range as `-1`; the fingerprint/round-trip only compared in-bounds tiles, all of which matched.

### File List

- ADDED: core/src/main/java/com/margins/rogue/save/SaveService.java
- MODIFIED: core/src/main/java/com/margins/rogue/state/RunState.java
- MODIFIED: core/src/main/java/com/margins/rogue/RogueTileMap.java
- MODIFIED: core/src/main/java/com/margins/rogue/RoguePlayer.java
- MODIFIED: core/src/main/java/com/margins/rogue/RogueEnemy.java
- MODIFIED: core/src/main/java/com/margins/rogue/RogueGameScreen.java

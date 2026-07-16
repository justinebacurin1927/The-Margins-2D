# Story 1.4: Save and resume a run

Status: ready-for-dev

## Story

As Justine (player),
I want to quit mid-run and pick up exactly where I left off,
so that a play session can span more than one sitting.

## Acceptance Criteria

1. Given an in-progress run, when I save and relaunch, the exact `RunState` is restored (floor, positions, inventory, identified supplies, flags, Bond, hunger, HP, last-stand-used, seed) and play continues from the saved turn. (FR-20, AD-6)
2. Serialization uses libGDX `Json` with `RunState` as the sole root; the tilemap and RNG are not double-serialized via entity back-references. (AD-6, save serialization-root convention, NFR-4)
3. A save is written at a sensible cadence (at minimum on quit/hide; ideally each turn or on floor transition).

## Tasks / Subtasks

- [ ] Task 1: Create `save/SaveService.java` with `save(RunState)` and `RunState load()` using `com.badlogic.gdx.utils.Json` to a single slot file under the libGDX local/user storage path (AC: 1, 2)
- [ ] Task 2: Make `RunState` serializable-clean (AC: 2)
  - [ ] Ensure entities hold no serialized back-refs to the tilemap or RNG. Mark such fields transient and re-wire them on load (inject `tileMap`/`rng` back into player/enemies after deserialize). This directly addresses `RoguePlayer.map`/`RoguePlayer.rand`.
  - [ ] Reconstruct the RNG from the stored `seed` (and, if exact mid-run reproducibility of future draws matters, store draw-count or accept that only layout/bindings are seed-reproducible — layout+bindings is the AC-1 requirement, not future-draw parity).
- [ ] Task 3: Wire save triggers — call `SaveService.save(state)` on `hide()`/`pause()` and on floor transition; call `load()` on launch when a slot exists, else start a new run (AC: 3)
- [ ] Task 4: Manual test — start a run, move a few turns, quit, relaunch, confirm exact resume (AC: 1)

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

### Debug Log References

### Completion Notes List

### File List

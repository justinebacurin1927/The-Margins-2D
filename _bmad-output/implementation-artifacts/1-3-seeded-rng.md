# Story 1.3: Single seeded RNG for reproducible runs

Status: done

## Story

As the developer,
I want all gameplay randomness to draw from one seeded RNG on `RunState`,
so that a seed reproduces a run for debugging and honest playtesting.

## Acceptance Criteria

1. Two runs started with the same seed produce identical floor layouts and (once Story 3.3 lands) identical identify-by-use bindings. (NFR-2, AD-5)
2. No gameplay class calls `new Random()` — all draws come from `RunState`'s single seeded RNG.

## Tasks / Subtasks

- [x] Task 1: Add `long seed` + `Random rng` to `RunState`, seeded at run init (default: `System.nanoTime()`; allow an explicit seed for testing) (AC: 1) — already satisfied by Story 1.1; verified `RunState(long seed)` + `RunState()` default + `transient Random rng`.
- [x] Task 2: Route floor generation through the shared RNG — `FloorGenerator.generate(w,h, state.rng(), floorDepth)` (AC: 1) — already satisfied by Story 1.1; `generateFloor()` passes `rng`, and enemy placement draws from `rng` too.
- [x] Task 3: Remove scattered `new Random()` usages (AC: 2)
  - [x] `RogueGameScreen.rand` — already removed by the Story 1.2 refactor (screen holds no RNG).
  - [x] `RoguePlayer.rand` (used by `tryDodge`) → removed the field + `new Random()`; `tryDodge(Random rng)` now takes the shared RNG, and `CombatSystem` passes `state.rng()`. No back-ref stored (respects the save serialization-root rule for Story 1.4).
- [x] Task 4: Verify AC-1 by running two floors with a fixed seed and diffing layout — headless assert: two `RunState(123456789L)` produced identical 50×50 tile grid + player + all enemy placements; a different seed diverged.

## Dev Notes

### Governing architecture
- **AD-5** — one seeded RNG owned by `RunState`; all randomness draws from it; corrects the current scattered `new Random()`.
- Interacts with the **save serialization-root convention**: the RNG lives on `RunState` and is serialized once (Story 1.4). Entities must not each hold their own `Random`.

### Current state
- `new Random()` appears in `RogueGameScreen` (line 56) and `RoguePlayer` (constructor, used by `tryDodge`). `FloorGenerator.generate` already accepts a `Random` param — good; just pass the shared one.

### Depends on
Story 1.1 (RunState) and ideally 1.2 (CombatSystem, which is where dodge resolves and can read `state.rng()` cleanly).

### References
- [Source: _bmad-output/planning-artifacts/epics.md#Epic 1 · Story 1.3]
- [Source: ARCHITECTURE-SPINE.md#AD-5]
- [Source: core/src/main/java/com/margins/rogue/RogueGameScreen.java line 56; RoguePlayer.java tryDodge]

## Dev Agent Record

### Agent Model Used

claude-opus-4-8[1m] (via bmad-dev-story)

### Debug Log References

- `mvn -o compile` → BUILD SUCCESS
- Headless determinism assert (throwaway `DetTest`): `same-seed match: true`, `diff-seed differs: true`, enemy count 13 → `AC-1 PASS`
- Launch on display :0, 10s → ran clean (no exceptions in log)

### Completion Notes List

- Tasks 1 & 2 were already implemented by Story 1.1: `RunState` owns `long seed` + `transient Random rng` (default `System.nanoTime()`, explicit-seed constructor for tests), and `generateFloor()` routes both floor layout and enemy placement through that single `rng` (AD-5). No change needed there.
- `RogueGameScreen`'s old `new Random()` was already eliminated by the Story 1.2 refactor — the screen holds no RNG.
- Only remaining scattered RNG was `RoguePlayer.rand`. Removed the field and its `new Random()`; changed `tryDodge()` → `tryDodge(Random rng)` so it draws from the passed-in shared RNG. `CombatSystem.enemyPhase()` now calls `player.tryDodge(state.rng())`. RoguePlayer stores no `Random` back-ref, keeping the save serialization-root convention intact (AD-6 / Story 1.4).
- AC-2 confirmed: the only `new Random` in gameplay code is `RunState`'s `new Random(seed)`.

### File List

- MODIFIED: core/src/main/java/com/margins/rogue/RoguePlayer.java
- MODIFIED: core/src/main/java/com/margins/rogue/system/CombatSystem.java

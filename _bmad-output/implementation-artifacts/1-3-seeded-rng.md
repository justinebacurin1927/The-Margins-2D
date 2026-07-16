# Story 1.3: Single seeded RNG for reproducible runs

Status: ready-for-dev

## Story

As the developer,
I want all gameplay randomness to draw from one seeded RNG on `RunState`,
so that a seed reproduces a run for debugging and honest playtesting.

## Acceptance Criteria

1. Two runs started with the same seed produce identical floor layouts and (once Story 3.3 lands) identical identify-by-use bindings. (NFR-2, AD-5)
2. No gameplay class calls `new Random()` — all draws come from `RunState`'s single seeded RNG.

## Tasks / Subtasks

- [ ] Task 1: Add `long seed` + `Random rng` to `RunState`, seeded at run init (default: `System.nanoTime()`; allow an explicit seed for testing) (AC: 1)
- [ ] Task 2: Route floor generation through the shared RNG — `FloorGenerator.generate(w,h, state.rng(), floorDepth)` (AC: 1)
- [ ] Task 3: Remove scattered `new Random()` usages (AC: 2)
  - [ ] `RogueGameScreen.rand` (line 33/56) → use `state.rng()`.
  - [ ] `RoguePlayer.rand` (used by `tryDodge`) → draw from the shared RNG (pass it in or read from a `RunState` reference supplied to the combat system). Do NOT store a new back-ref that breaks the save serialization-root rule (Story 1.4).
- [ ] Task 4: Verify AC-1 by running two floors with a fixed seed and diffing layout (temporary debug print or a headless assert).

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

### Debug Log References

### Completion Notes List

### File List

# Story 2.2: Explored fog memory

Status: done

## Story

As Justine (player),
I want previously-seen terrain to be remembered dimly,
so that I can navigate without seeing live enemies through walls.

## Acceptance Criteria

1. Given a room I have left, when it is out of sight, its walls/floor render dimmed and any enemy that walked out of sight is no longer drawn. (FR-2)

## Tasks / Subtasks

- [x] Task 1: `renderWorld()` now renders each tile in three cases (AC: 1): `visible` → full-color; `explored && !visible` → dimmed (`batch.setColor(0.45f,0.45f,0.5f,1)` around the draw, reset to white after); neither → skipped.
- [x] Task 2: Enemies (sprite + HP bar) remain gated on `isVisible` only from Story 2.1 — no explored-based entity drawing added, so dynamic entities are not remembered in fog (AC: 1).
- [x] Task 3: Verified — build clean and live boot clean; the dim branch resets batch color immediately so player/enemy draws stay untinted.

## Dev Notes

### Governing architecture
- **AD-2** — this is purely a rendering interpretation of the `visible`/`explored` model flags; no new game rule. All logic lives in `renderWorld()` reading `RogueTileMap` flags.
- Depends on Story 2.1 (`visible`/`explored` flags + FovSystem).

### Current state
- `RogueGameScreen.renderWorld()` (lines 107-130) draws tiles with `batch.draw` at full color and enemies unconditionally-if-alive. Story 2.1 gates both by `visible`; this story adds the dimmed `explored` branch for tiles only.

### References
- [Source: _bmad-output/planning-artifacts/epics.md#Epic 2 · Story 2.2]
- [Source: PRD FR-2; ARCHITECTURE-SPINE.md#AD-2]
- [Source: core/src/main/java/com/margins/rogue/RogueGameScreen.java lines 107-130]

## Dev Agent Record

### Agent Model Used

claude-opus-4-8[1m] (via bmad-dev-story)

### Debug Log References

- `mvn -o compile` / `install` → BUILD SUCCESS
- Launch on display :0, 8s → clean boot

### Completion Notes List

- Pure rendering interpretation of the Story 2.1 `visible`/`explored` flags (AD-2) — no new game rule. Explored-but-out-of-sight tiles draw at a dim blue-grey; the batch color is reset to white right after each dim draw so subsequent enemy/player draws are unaffected.
- Enemies stay visible-only (from 2.1), so a foe that walks out of sight vanishes even though its terrain is remembered — satisfies "navigate without seeing live enemies through walls."

### File List

- MODIFIED: core/src/main/java/com/margins/rogue/RogueGameScreen.java

# Story 2.2: Explored fog memory

Status: ready-for-dev

## Story

As Justine (player),
I want previously-seen terrain to be remembered dimly,
so that I can navigate without seeing live enemies through walls.

## Acceptance Criteria

1. Given a room I have left, when it is out of sight, its walls/floor render dimmed and any enemy that walked out of sight is no longer drawn. (FR-2)

## Tasks / Subtasks

- [ ] Task 1: In `renderWorld()`, render each tile in three cases (AC: 1)
  - [ ] `visible` → full-color draw.
  - [ ] `explored && !visible` → dimmed draw (e.g., `batch.setColor(0.45f,0.45f,0.5f,1)` then reset to white). Uses the `explored` flag set by Story 2.1.
  - [ ] neither → skip (unexplored, hidden).
- [ ] Task 2: Confirm dynamic entities are NOT persisted in fog — enemies draw only when their tile is `visible` (already gated in Story 2.1); do not add explored-based enemy drawing (AC: 1)
- [ ] Task 3: Manual test — leave a room; its layout stays dimly visible; an enemy that left sight disappears.

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

### Debug Log References

### Completion Notes List

### File List

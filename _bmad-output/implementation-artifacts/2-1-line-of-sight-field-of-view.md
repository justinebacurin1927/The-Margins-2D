# Story 2.1: Line-of-sight field of view

Status: ready-for-dev

## Story

As Justine (player),
I want to see only what Milek can actually see,
so that stealth and ambush have meaning.

## Acceptance Criteria

1. Given Milek stands in a room, when the view renders, tiles within the sight radius and not blocked by a wall are visible; opening a door reveals the room beyond next turn. (FR-1)
2. Enemies and supplies render only while on a currently-visible tile.
3. FOV runs as a system on `RunState`, using tile data only — no libGDX render types in the system. (AD-2)

## Tasks / Subtasks

- [ ] Task 1: Add per-tile visibility to `RogueTileMap` — parallel `boolean[][] visible` and `boolean[][] explored` arrays with getters/setters and a `clearVisible()` (AC: 1)
- [ ] Task 2: Create `system/FovSystem.java` implementing recursive shadowcasting (AC: 1, 3)
  - [ ] Origin = player tile; radius = 8 (PRD Balance `FOV sight radius`). Blocking test = `RogueTileMap.isOpaque(x,y)` (already exists; walls opaque).
  - [ ] Each recompute: clear `visible`, mark visible tiles, set `explored=true` for any visible tile.
- [ ] Task 3: Run FOV in the turn pipeline — recompute after the player acts (and once at floor generation) so it reflects the new position (AC: 1). Insert into `TurnEngine` order from Story 1.2 (after PlayerAction, before/around rendering read).
- [ ] Task 4: Gate rendering by visibility (AC: 1, 2)
  - [ ] In `RogueGameScreen.renderWorld()`, draw a tile only if `visible` (bright) — see Story 2.2 for explored/dim handling. Draw an enemy only if its tile is `visible`. (Player always drawn.)
- [ ] Task 5: Manual test — walls block sight; stepping through a door reveals the next room; an enemy behind a wall is not drawn.

## Dev Notes

### Governing architecture
- **AD-2** — `FovSystem` is a model system, no render types. The screen consumes the `visible`/`explored` flags at draw time.
- **AD-4** — FOV recompute is a pipeline step (Story 1.2's `TurnEngine`).
- Tuning: sight radius 8 (PRD Balance).

### Current state / reuse
- `RogueTileMap.isOpaque(x,y)` (line 32-35) already returns wall-opacity via `RogueTile.isOpaque` — use it as the shadowcasting blocker; do not invent a new blocking test.
- `renderWorld()` currently draws every tile in a camera window (RogueGameScreen lines 107-130). Change the per-tile draw to check `visible`; keep the camera-window loop.
- Depends on Story 1.1 (`RunState` owns the tilemap) and 1.2 (`TurnEngine` to host the recompute step). If 1.2 not yet done, recompute FOV inline right after the player acts and refactor into the pipeline when 1.2 lands.

### Testing standards
`FovSystem` is headless-testable: build a small `RogueTileMap` with a wall, run FOV from a point, assert tiles behind the wall are not visible. Add if a test dep exists (NFR-3).

### References
- [Source: _bmad-output/planning-artifacts/epics.md#Epic 2 · Story 2.1]
- [Source: ARCHITECTURE-SPINE.md#AD-2, #AD-4; PRD FR-1, Balance (FOV sight radius 8)]
- [Source: core/src/main/java/com/margins/rogue/RogueTileMap.java lines 32-35; RogueGameScreen.java lines 107-130]

## Dev Agent Record

### Agent Model Used

### Debug Log References

### Completion Notes List

### File List

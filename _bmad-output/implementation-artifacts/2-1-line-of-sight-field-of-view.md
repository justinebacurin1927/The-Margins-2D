# Story 2.1: Line-of-sight field of view

Status: done

## Story

As Justine (player),
I want to see only what Milek can actually see,
so that stealth and ambush have meaning.

## Acceptance Criteria

1. Given Milek stands in a room, when the view renders, tiles within the sight radius and not blocked by a wall are visible; opening a door reveals the room beyond next turn. (FR-1)
2. Enemies and supplies render only while on a currently-visible tile.
3. FOV runs as a system on `RunState`, using tile data only — no libGDX render types in the system. (AD-2)

## Tasks / Subtasks

- [x] Task 1: Added `transient boolean[][] visible` + persisted `boolean[][] explored` to `RogueTileMap`, allocated in the public ctor, with `isVisible`/`setVisible`/`clearVisible` and `isExplored`/`setExplored` (all bounds-checked; lazily allocate so a Json-loaded map — no-arg ctor — is safe) (AC: 1).
- [x] Task 2: Created `system/FovSystem.java` — recursive shadowcasting, radius 8, blocker = `RogueTileMap.isOpaque` (AC: 1, 3). Each `compute` clears `visible`, marks lit tiles, and sets `explored=true` for every lit tile. No libGDX types (AD-2). Added a `compute(map, px, py)` overload as the headless test seam.
- [x] Task 3: `TurnEngine.advance()` recomputes FOV in the acted block (after the player's move resolves); `RogueGameScreen.show()` computes it once after load-or-new, and again after `restart()` (AC: 1). New-run FOV is covered by show(); floor-transition recompute will reuse the same call site when Epic 6 adds descent.
- [x] Task 4: `renderWorld()` now skips tiles where `!isVisible` and draws an enemy (sprite + HP bar) only when its tile is visible; player always drawn (AC: 1, 2). Explored/dim rendering is deferred to Story 2.2 as noted.
- [x] Task 5: Verified via headless occlusion test (a wall column blocks the tiles behind it while open tiles stay lit, explored accumulates only on seen tiles, radius bound holds) + clean live boot.

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

claude-opus-4-8[1m] (via bmad-dev-story)

### Debug Log References

- `mvn -o compile` → BUILD SUCCESS
- Headless FOV occlusion test: origin/near/wall-face lit, tiles behind wall dark, off-shadow lit, radius bound holds, explored only on seen tiles → FOV AC PASS (9/9)
- Launch on display :0, 8s → clean boot (no NPE from visibility gating)

### Completion Notes List

- FOV is a headless model system (AD-2): `FovSystem` reads only tile data and writes `visible`/`explored` flags on `RogueTileMap`; the screen consumes those flags at draw time.
- `visible` is transient (recomputed every pass); `explored` persists in the save (fog memory for Story 2.2, and it rides the Story 1.4 serialization root for free). After a load, `visible` is null until the first `compute`, so `show()` computes FOV right after `SaveService.load()`; `clearVisible`/setters also lazily allocate to stay null-safe.
- Recompute sites: `TurnEngine` acted-block (in-run movement, AD-4), `show()` (new run / post-load first frame), and after `restart()`. Radius 8 per PRD Balance; doors are transparent (only WALL is opaque) so sight passes through them, satisfying "opening a door reveals the room beyond."
- Rendering: tiles draw only when currently visible; enemies (sprite + HP bar) only when standing on a visible tile. Explored-but-not-visible dim rendering is intentionally left for Story 2.2.

### File List

- ADDED: core/src/main/java/com/margins/rogue/system/FovSystem.java
- MODIFIED: core/src/main/java/com/margins/rogue/RogueTileMap.java
- MODIFIED: core/src/main/java/com/margins/rogue/system/TurnEngine.java
- MODIFIED: core/src/main/java/com/margins/rogue/RogueGameScreen.java

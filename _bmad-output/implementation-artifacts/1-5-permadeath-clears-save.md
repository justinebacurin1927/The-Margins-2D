# Story 1.5: Permadeath clears the save

Status: done

## Story

As Justine (player),
I want a dead run to be truly gone,
so that death carries weight and can't be reloaded.

## Acceptance Criteria

1. When true death occurs, the save slot is deleted and no "continue" option is offered — only a new run can start. (FR-21)

## Tasks / Subtasks

- [x] Task 1: On entering the game-over state (`RogueGameScreen.handleInput()`, where `player.isAlive()` becomes false and `gameOver` flips true), call `SaveService.deleteSave()` (AC: 1). Added `SaveService.deleteSave()` (deferred from Story 1.4).
- [x] Task 2: Already satisfied by Story 1.4 — `show()` does load-or-new (`SaveService.load()` → `null` → `new RunState()`), and there is no "continue" UI; after death the slot is gone so launch/`R` start fresh exactly as before (AC: 1).
- [x] Task 3: Verified via disk test — save a run, `deleteSave()`, then `hasSave()==false` and `load()==null` (dead run cannot be resumed). Live boot clean.

## Dev Notes

### Governing architecture
- **AD-6** — delete-on-true-death is part of the save contract.
- **Interaction with Story 1.6:** deletion must happen only on TRUE death (Last Stand already spent), not on the Last Stand reprieve. Sequence 1.6 before/with this so the "true death" condition is correct.

### Current state
- Death is detected at lines 204-207; restart (`R`) resets `floorDepth=1; generateFloor()` at line 209. Keep restart working; just ensure the save file is gone.
- `SaveService.deleteSave()` comes from Story 1.4.

### Depends on
Stories 1.4 (SaveService) and 1.6 (defines true death vs reprieve).

### References
- [Source: _bmad-output/planning-artifacts/epics.md#Epic 1 · Story 1.5]
- [Source: ARCHITECTURE-SPINE.md#AD-6]
- [Source: core/src/main/java/com/margins/rogue/RogueGameScreen.java lines 204-211]

## Dev Agent Record

### Agent Model Used

claude-opus-4-8[1m] (via bmad-dev-story)

### Debug Log References

- `mvn -o compile` → BUILD SUCCESS
- Disk test: `hasSave after save: true`, `hasSave after death: false`, `load after death: null` → AC-1 PASS
- Launch on display :0, 8s → clean boot

### Completion Notes List

- Added `SaveService.deleteSave()` (`Gdx.files.local(SAVE_PATH).delete()`) — the delete-on-death half of the AD-6 save contract, deferred out of Story 1.4.
- `RogueGameScreen`: when the run enters game-over (`!player.isAlive() && !gameOver`), it now deletes the save slot before returning, so a true-dead run leaves no slot to reload (FR-21).
- Forward-compat with Story 1.6 (Last Stand): the reprieve must revive the player *before* this game-over transition fires, so `deleteSave()` only runs on true death (Last Stand already spent) — the current placement needs no change when 1.6 lands.
- No "continue" affordance exists to suppress; `show()`'s load-or-new logic already means a missing slot starts a fresh run.

### File List

- MODIFIED: core/src/main/java/com/margins/rogue/save/SaveService.java
- MODIFIED: core/src/main/java/com/margins/rogue/RogueGameScreen.java

# Story 1.5: Permadeath clears the save

Status: ready-for-dev

## Story

As Justine (player),
I want a dead run to be truly gone,
so that death carries weight and can't be reloaded.

## Acceptance Criteria

1. When true death occurs, the save slot is deleted and no "continue" option is offered — only a new run can start. (FR-21)

## Tasks / Subtasks

- [ ] Task 1: On entering the game-over state (`RogueGameScreen.handleInput()` lines 204-207 where `player.isAlive()` becomes false), call `SaveService.deleteSave()` (AC: 1)
- [ ] Task 2: On launch, only offer resume when a slot exists; after death there is none, so the flow starts a fresh run via `R` (restart) exactly as today (AC: 1)
- [ ] Task 3: Manual test — die, confirm relaunch does not resume the dead run

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

### Debug Log References

### Completion Notes List

### File List

# Story 2.3: Enemy patrol while unaware

Status: ready-for-dev

## Story

As Justine (player),
I want unaware enemies to patrol predictably,
so that I can read patterns and slip past.

## Acceptance Criteria

1. Given an Unaware enemy and Milek never entering its sight or Noise range, when turns pass, the enemy follows its patrol/idle-wander and never initiates combat. (FR-3)

## Tasks / Subtasks

- [ ] Task 1: Add a `Detection` enum (`UNAWARE`, `SUSPICIOUS`, `ALERTED`) field to `RogueEnemy`, default `UNAWARE` (AC: 1) — sets up Story 2.4
- [ ] Task 2: Add unaware behavior to enemy AI (AC: 1)
  - [ ] Simplest viable: idle-wander — while `UNAWARE`, on the enemy AI step move a random step (from `state.rng()`, AD-5) into a walkable neighbor or stay put, instead of chasing.
  - [ ] Optional (nicer): a fixed waypoint loop per enemy. Idle-wander is acceptable for MVP.
- [ ] Task 3: Ensure an `UNAWARE` enemy never attacks — only `ALERTED` (Story 2.4) pursues/attacks (AC: 1). The current unconditional chase in `RogueEnemy.takeTurn()` must become Detection-gated.
- [ ] Task 4: Manual test — stay out of sight; enemies mill about and never approach/attack.

## Dev Notes

### Governing architecture
- **AD-9** — Detection lives on the enemy; enemy behavior branches on Detection state.
- **AD-5** — wander randomness from `state.rng()`, not `new Random()`.
- **AD-4** — enemy AI is the "Enemy AI" pipeline step (Story 1.2).

### Current state / what changes
- `RogueEnemy.takeTurn(playerX, playerY)` (lines 41-65) currently ALWAYS chases (x-first then y). This story makes chase conditional on `ALERTED`; `UNAWARE` wanders. Preserve the existing chase code path for the `ALERTED` case (Story 2.4 wires escalation).
- Arrival-grace (`justArrived`) logic (lines 62-64) stays intact for when an alerted enemy reaches the player.

### Depends on
Story 1.1 (RunState/rng). Sets up Story 2.4 (state transitions) and 2.5 (noise). If TurnEngine (1.2) exists, enemy AI already runs as a step; otherwise the current per-enemy loop in `handleInput` calls `takeTurn`.

### References
- [Source: _bmad-output/planning-artifacts/epics.md#Epic 2 · Story 2.3]
- [Source: PRD FR-3; ARCHITECTURE-SPINE.md#AD-9, #AD-5]
- [Source: core/src/main/java/com/margins/rogue/RogueEnemy.java lines 41-69]

## Dev Agent Record

### Agent Model Used

### Debug Log References

### Completion Notes List

### File List

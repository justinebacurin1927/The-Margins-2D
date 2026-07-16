# Story 2.3: Enemy patrol while unaware

Status: done

## Story

As Justine (player),
I want unaware enemies to patrol predictably,
so that I can read patterns and slip past.

## Acceptance Criteria

1. Given an Unaware enemy and Milek never entering its sight or Noise range, when turns pass, the enemy follows its patrol/idle-wander and never initiates combat. (FR-3)

## Tasks / Subtasks

- [x] Task 1: Added top-level `Detection` enum (`UNAWARE`, `SUSPICIOUS`, `ALERTED`) and a `detection` field on `RogueEnemy` defaulting to `UNAWARE`, with getter/setter (AC: 1). Persisted with the enemy in the save.
- [x] Task 2: Idle-wander implemented as `RogueEnemy.wander(rng, avoidX, avoidY)` — a random walkable step (4 dirs + stay) drawn from `state.rng()` (AD-5), never stepping onto the player. Chose idle-wander (MVP-acceptable) over waypoint loops (AC: 1).
- [x] Task 3: `CombatSystem.enemyPhase` now branches on detection: only `ALERTED` enemies run the arrival-grace + attack-if-adjacent + chase path; `UNAWARE`/`SUSPICIOUS` wander and never initiate combat (AC: 1). The `takeTurn` chase code is preserved for the alerted branch.
- [x] Task 4: Verified headless — an UNAWARE enemy adjacent to the player deals no damage over 60 turns and doesn't self-escalate; an ALERTED adjacent enemy resolves an attack (message emitted). Live boot clean.

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

claude-opus-4-8[1m] (via bmad-dev-story)

### Debug Log References

- `mvn -o compile` → BUILD SUCCESS
- Patrol test: default UNAWARE, no damage over 60 adjacent turns, no self-escalation, ALERTED resolves attack → PATROL AC PASS (4/4)
- Launch on display :0, 8s → clean boot

### Completion Notes List

- `Detection` is a top-level enum in the `rogue` package so Stories 2.4 (transitions) and 2.5 (noise) can reference it; `RogueEnemy.detection` defaults to `UNAWARE` and persists in the save.
- Behavior is detection-gated in `CombatSystem.enemyPhase` (which owns `state.rng()` for wander), not inside `takeTurn`. Only `ALERTED` pursues/attacks; everything else idle-wanders. Since nothing sets `ALERTED` until Story 2.4, all enemies currently patrol and never aggro — the player can still fight them via `playerAttack`.
- Wander draws from the shared seeded RNG (AD-5) and avoids the player's tile to prevent overlap; it does not set `justArrived` (that grace is only meaningful for an alerted arrival).

### File List

- ADDED: core/src/main/java/com/margins/rogue/Detection.java
- MODIFIED: core/src/main/java/com/margins/rogue/RogueEnemy.java
- MODIFIED: core/src/main/java/com/margins/rogue/system/CombatSystem.java

# Story 2.4: Detection state machine

Status: ready-for-dev

## Story

As Justine (player),
I want enemies to notice me gradually and lose me if I break away,
so that stealth is a tense, recoverable state rather than instant failure.

## Acceptance Criteria

1. Given Milek enters an enemy's vision radius with line-of-sight, when turns pass, the enemy rises Unaware→Suspicious, and sustained sight escalates to Alerted (begins pursuit). (FR-4, AD-9)
2. Given the enemy loses sight and stimulus, when the de-escalation interval passes, it drops one Detection step.

## Tasks / Subtasks

- [ ] Task 1: Create `system/DetectionSystem.java` run each turn before enemy movement (AC: 1, 2)
  - [ ] For each living enemy: compute whether it can see the player = within `enemy vision range` (6, PRD Balance) AND line-of-sight clear (reuse the shadowcasting/`isOpaque` line check from `FovSystem` — extract a shared `hasLineOfSight(map, x0,y0,x1,y1)` helper).
- [ ] Task 2: Implement transitions with per-enemy counters (AC: 1)
  - [ ] In sight: increment a `sightTurns` counter; `UNAWARE`→`SUSPICIOUS` immediately on sight; `SUSPICIOUS`→`ALERTED` after 2 consecutive in-sight turns (PRD Balance `Suspicious → Alerted`).
- [ ] Task 3: Implement de-escalation (AC: 2)
  - [ ] Out of sight and no noise stimulus: after 3 turns without stimulus, drop one Detection step (PRD Balance `De-escalation 1 step / 3 turns`); reset `sightTurns`.
- [ ] Task 4: Gate movement/attack on state — `ALERTED` chases + attacks (existing chase in `RogueEnemy.takeTurn` + adjacency attack in CombatSystem); `SUSPICIOUS` may move toward last-seen tile but does not attack; `UNAWARE` wanders (Story 2.3) (AC: 1)
- [ ] Task 5: Above-enemy Detection indicator in `renderWorld()` (small `?`/`!` marker or bar tint) — no full stealth-meter UI (AD-9) (AC: 1)
- [ ] Task 6: Manual test — walk into view (Suspicious), stay (Alerted → chased), break LOS and wait (de-escalates).

## Dev Notes

### Governing architecture
- **AD-9** — radius + line-of-sight, NO directional cones; enemy holds the Detection enum (added in Story 2.3); de-escalation over turns.
- Tuning (PRD Balance): vision range 6; Suspicious→Alerted after 2 consecutive in-sight turns; de-escalate 1 step / 3 turns without stimulus.
- **AD-4** — `DetectionSystem` runs as a pipeline step before enemy AI movement.

### Current state / reuse
- Enemy chase logic already exists in `RogueEnemy.takeTurn()` — drive it only when `ALERTED`.
- Line-of-sight: reuse `FovSystem`'s blocking walk via `RogueTileMap.isOpaque`. Extract a shared static `hasLineOfSight` so FOV and Detection agree.
- Combat adjacency/attack resolution lives in `CombatSystem` (Story 1.2); ensure only `ALERTED` enemies deal damage.

### Depends on
Stories 2.1 (LOS helper), 2.3 (Detection enum + wander), 1.2 (systems/TurnEngine). Story 2.5 feeds Noise as an escalation stimulus.

### References
- [Source: _bmad-output/planning-artifacts/epics.md#Epic 2 · Story 2.4]
- [Source: PRD FR-4, Balance (Detection block); ARCHITECTURE-SPINE.md#AD-9, #AD-4]
- [Source: core/src/main/java/com/margins/rogue/RogueEnemy.java lines 41-69]

## Dev Agent Record

### Agent Model Used

### Debug Log References

### Completion Notes List

### File List

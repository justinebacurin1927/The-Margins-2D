# Story 2.4: Detection state machine

Status: done

## Story

As Justine (player),
I want enemies to notice me gradually and lose me if I break away,
so that stealth is a tense, recoverable state rather than instant failure.

## Acceptance Criteria

1. Given Milek enters an enemy's vision radius with line-of-sight, when turns pass, the enemy rises Unaware→Suspicious, and sustained sight escalates to Alerted (begins pursuit). (FR-4, AD-9)
2. Given the enemy loses sight and stimulus, when the de-escalation interval passes, it drops one Detection step.

## Tasks / Subtasks

- [x] Task 1: Created `system/DetectionSystem.java`, run in `TurnEngine` before `CombatSystem.enemyPhase`. Per living enemy: can-see = within vision range 6 (Euclidean²) AND `FovSystem.hasLineOfSight` clear. Extracted a shared static `hasLineOfSight(map,x0,y0,x1,y1)` (Bresenham over `isOpaque`) so FOV and Detection agree (AC: 1, 2).
- [x] Task 2: Per-enemy counters on `RogueEnemy` (`sightTurns`, `calmTurns`, `lastSeenX/Y`). In sight: reset calm, increment `sightTurns`, record last-seen; `UNAWARE`→`SUSPICIOUS` on first sight; `SUSPICIOUS`→`ALERTED` at `sightTurns >= 2` (AC: 1).
- [x] Task 3: Out of sight: reset `sightTurns`; if not already UNAWARE, increment `calmTurns` and drop one step every 3 calm turns (AC: 2).
- [x] Task 4: `CombatSystem.enemyPhase` gates on state — `ALERTED` chases + attacks (arrival-grace preserved); `SUSPICIOUS` moves toward its last-seen tile via `takeTurn(lastSeenX,lastSeenY)` and never attacks; `UNAWARE` wanders (AC: 1).
- [x] Task 5: `renderWorld()` draws a red `!` over ALERTED and a yellow `?` over SUSPICIOUS enemies (only when visible) — no stealth-meter UI (AD-9) (AC: 1).
- [x] Task 6: Verified headless — escalation UNAWARE→SUSPICIOUS→ALERTED on consecutive in-sight turns, de-escalation ALERTED→SUSPICIOUS→UNAWARE at the 3-calm-turn cadence, LOS helper blocks through walls, no underflow past UNAWARE. Live boot clean.

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

claude-opus-4-8[1m] (via bmad-dev-story)

### Debug Log References

- `mvn -o compile` → BUILD SUCCESS
- Detection test: LOS clear/blocked, escalate on 2 in-sight turns, de-escalate at 3-calm-turn cadence both steps, UNAWARE floor → DETECTION AC PASS (11/11)
- Launch on display :0, 8s → clean boot

### Completion Notes List

- `DetectionSystem` is a pipeline step (AD-4) running before enemy movement, on the player's post-move position. Radius + LOS only, no directional cones (AD-9). Tuning per PRD Balance: vision 6, alert at 2 consecutive in-sight turns, de-escalate 1 step / 3 calm turns.
- Shared `FovSystem.hasLineOfSight` (Bresenham over `isOpaque`) is the single blocker test for both player FOV and enemy sight, so they can't disagree.
- Detection counters + last-seen live on `RogueEnemy` (persist in the save). `SUSPICIOUS` enemies investigate the last-seen tile via the existing `takeTurn` chase (retargeted) and never attack; only `ALERTED` deals damage.
- "No stimulus" currently means "not in sight" — Story 2.5 will add Noise as a second escalation stimulus that also resets `calmTurns`.
- Indicator is a minimal glyph (`!`/`?`) over visible enemies, drawn in the world batch; font color is restored to white after each so the HUD/other draws are unaffected.

### File List

- ADDED: core/src/main/java/com/margins/rogue/system/DetectionSystem.java
- MODIFIED: core/src/main/java/com/margins/rogue/system/FovSystem.java
- MODIFIED: core/src/main/java/com/margins/rogue/RogueEnemy.java
- MODIFIED: core/src/main/java/com/margins/rogue/system/TurnEngine.java
- MODIFIED: core/src/main/java/com/margins/rogue/system/CombatSystem.java
- MODIFIED: core/src/main/java/com/margins/rogue/RogueGameScreen.java

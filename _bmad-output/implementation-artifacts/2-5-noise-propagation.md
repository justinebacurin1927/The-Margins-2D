# Story 2.5: Noise propagation

Status: done

## Story

As Justine (player),
I want loud actions to draw enemies,
so that noise is a tool and a risk.

## Acceptance Criteria

1. Given a Noise event (e.g., forcing a crate), when the AI system ticks, enemies within the Noise radius raise Detection and/or move toward the Noise origin; a silent move produces no such effect. (FR-5, AD-9)

## Tasks / Subtasks

- [x] Task 1: Added `NoiseEvent {final int x,y,radius}` and a `transient List<NoiseEvent> noiseQueue` on `RunState` (field-initialized so it's non-null after a Json load; never serialized) (AC: 1).
- [x] Task 2: Created `system/NoiseSystem.resolve(state)` wired into the TurnEngine's reserved Noise-resolve slot. For each queued event, every living enemy within `radius` (Euclidean, documented) is drawn to the sound; the queue is cleared at the end (AC: 1).
- [x] Task 3: Producer API `RunState.emitNoise(x,y,radius)`; wired one producer — `CombatSystem.playerAttack` emits a radius-4 noise at the player's tile (AC: 1).
- [x] Task 4: Confirmed silent MOVE/BLOCK/WAIT emit nothing — only `playerAttack` calls `emitNoise`, so a silent turn leaves the queue empty and `resolve` is a no-op (AC: 1).
- [x] Task 5: Verified headless — noise draws an in-radius UNAWARE enemy to SUSPICIOUS, retargets its `lastSeen` to the origin, resets `calmTurns`, and clears the queue; an out-of-radius enemy is untouched; a silent turn is a no-op; `playerAttack` enqueues a noise event. Live boot clean; queue survives save/load.

### Design note
Noise raises enemies to **SUSPICIOUS** (not ALERTED) and points their `lastSeen` at the origin. ALERTED enemies chase the *player*, so forcing ALERTED would defeat the "draw them to the sound" intent; SUSPICIOUS enemies investigate `lastSeen`, which is exactly the lure behavior Story 4.2 (Galleon Distraction) needs. If an enemy then sees the player at the noise, DetectionSystem (2.4) escalates it to ALERTED naturally.

## Dev Notes

### Governing architecture
- **AD-9** — Noise is transient events on a `RunState` queue, produced by systems/abilities and consumed on the AI tick. This story builds the queue + `NoiseSystem`; Galleon's Distraction (Story 4.2) is the headline consumer-producer.
- **AD-4** — `NoiseSystem` is the "Noise resolve" step already reserved in the TurnEngine order (Story 1.2).
- Tuning: Distraction noise radius 5 (PRD Balance) — general noise radii per producer.

### Current state
- The TurnEngine (Story 1.2) already includes a Noise-resolve no-op slot; this story fills it. `noiseQueue` is transient so it does not affect save (Story 1.4).

### Depends on
Stories 1.1/1.2 (RunState/TurnEngine), 2.4 (Detection to raise). Enables Story 4.2 (Distraction).

### References
- [Source: _bmad-output/planning-artifacts/epics.md#Epic 2 · Story 2.5]
- [Source: PRD FR-5, Balance (Distraction Noise radius 5); ARCHITECTURE-SPINE.md#AD-9, #AD-4]

## Dev Agent Record

### Agent Model Used

claude-opus-4-8[1m] (via bmad-dev-story)

### Debug Log References

- `mvn -o compile` → BUILD SUCCESS
- Noise test: raise+retarget+calm-reset+queue-clear, out-of-radius untouched, silent no-op, attack emits → NOISE AC PASS (8/8)
- Json round-trip: noiseQueue non-null + usable after load → PASS
- Launch on display :0, 8s → clean boot

### Completion Notes List

- Noise is a transient event queue on `RunState` (AD-9), produced via `emitNoise` and consumed by `NoiseSystem` in the TurnEngine's Noise-resolve step (AD-4). The queue is `transient` with a field initializer, so it's never saved and is guaranteed non-null after a Json-loaded run (verified).
- Radius test is Euclidean (squared-distance), matching FOV/vision for consistency.
- Producer wired: attacking emits a radius-4 noise at the player's tile. Future producers (forcing crates, Galleon Distraction radius 5) call the same `emitNoise` API.
- Lure semantics documented above: raise to SUSPICIOUS + retarget lastSeen, not force ALERTED.

### File List

- ADDED: core/src/main/java/com/margins/rogue/NoiseEvent.java
- ADDED: core/src/main/java/com/margins/rogue/system/NoiseSystem.java
- MODIFIED: core/src/main/java/com/margins/rogue/state/RunState.java
- MODIFIED: core/src/main/java/com/margins/rogue/system/TurnEngine.java
- MODIFIED: core/src/main/java/com/margins/rogue/system/CombatSystem.java

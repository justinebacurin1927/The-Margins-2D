# Story 2.5: Noise propagation

Status: ready-for-dev

## Story

As Justine (player),
I want loud actions to draw enemies,
so that noise is a tool and a risk.

## Acceptance Criteria

1. Given a Noise event (e.g., forcing a crate), when the AI system ticks, enemies within the Noise radius raise Detection and/or move toward the Noise origin; a silent move produces no such effect. (FR-5, AD-9)

## Tasks / Subtasks

- [ ] Task 1: Add a Noise event type and a transient queue on `RunState` (AC: 1)
  - [ ] `NoiseEvent { int x, int y, int radius; }`; `List<NoiseEvent> noiseQueue` on `RunState` (transient — not saved; regenerated each turn).
- [ ] Task 2: Create `system/NoiseSystem.java` as the pipeline "Noise resolve" step (from Story 1.2's TurnEngine order) (AC: 1)
  - [ ] For each queued Noise event, for each enemy within `radius` (Manhattan/Chebyshev — pick one, document): raise Detection toward `ALERTED` and set its move-target to the Noise origin (so `DetectionSystem`/AI heads there).
  - [ ] Clear the queue at end of resolve (events are single-turn).
- [ ] Task 3: Provide a producer API — `state.emitNoise(x,y,radius)` — so future features raise noise (Galleon Distraction in Story 4.2; forcing a crate). For THIS story, wire one producer to prove it: forcing/attacking generates a small noise (AC: 1)
- [ ] Task 4: Confirm a silent move enqueues no noise (AC: 1)
- [ ] Task 5: Manual test — make noise near a patrol; they turn toward it; move silently and they don't react.

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

### Debug Log References

### Completion Notes List

### File List

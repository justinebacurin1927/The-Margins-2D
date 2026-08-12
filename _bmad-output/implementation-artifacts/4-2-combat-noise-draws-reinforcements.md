---
baseline_commit: 58ed421
---

# Story 4.2: Combat noise draws reinforcements

Status: ready-for-dev

## Story

As Klein,
I want fighting to be loud and a wary patrol to be talk-down-able,
so that violence has a spatial consequence and avoidance/VOICE can still beat a fight (FR-12, AD-9).

## Acceptance Criteria

**AC-1 — Combat noise draws reinforcements.**
**Given** an attack or a block **When** it resolves **Then** a `NoiseEvent` is emitted and consumed by `NoiseSystem.resolve`, drawing in-radius living enemies toward the sound (UNAWARE→SUSPICIOUS, retarget their investigation at the noise origin, hold off de-escalation). Enemies are *lured* (SUSPICIOUS + investigate), not force-ALERTED — ALERTED is DetectionSystem's job when they then spot Klein.

**AC-2 — VOICE can talk a wary patrol down.**
**Given** a *wary* (SUSPICIOUS) patrol adjacent to Klein **When** Klein initiates a parley and passes a VOICE-gated option **Then** the encounter is de-escalated instead of fought: in-radius SUSPICIOUS enemies drop to UNAWARE (talked down). A failed VOICE check does NOT de-escalate (the patrol stays wary / is free to escalate normally). The parley entry point is reachable by a bound key (retro action item #1 — no test-only-reachable feature).

**AC-3 — Single authority + AD-9 discipline preserved.**
**Given** the new block noise and de-escalation **When** they run **Then** combat noise is emitted through the same `CombatSystem`/`RunState.emitNoise` → `NoiseSystem.resolve` channel as attack noise (AD-9; no detection mutation outside DetectionSystem/NoiseSystem), and no existing noise/detection behavior regresses.

## Scope decisions (confirmed with Justine, 2026-08-12)

- **D1 — AC-2 = mechanism + one minimal reachable trigger.** Build the new `Deescalate` `DialogEffect` (+ its controller execution routed through a `DetectionSystem` helper) and ONE reachable parley entry: a bound key, active when a SUSPICIOUS patrol is adjacent, opens a short VOICE-gated scene whose success node carries the `Deescalate` effect. **Out of scope (→ Epic 5):** rich per-faction dialogue trees, multi-node negotiation, disposition/Bond consequences of parley, non-adjacent hailing. 4.2 ships the *mechanism* and proves it end-to-end; Epic 5 authors the content.
- **D2 — Block is as loud as an attack (radius 4).** Block/Brace emits its `NoiseEvent` at `ATTACK_NOISE_RADIUS` (4) — a shield/weapon clang rings as far as a swing. No stealth distinction between Attack and Block. Reuse the existing constant; do not introduce a new radius.
- **D3 — Parley de-escalates SUSPICIOUS→UNAWARE only; ALERTED is past talking.** The de-escalation drops in-radius **SUSPICIOUS** enemies to UNAWARE. ALERTED enemies (they've already committed to Klein) are NOT talked down by this mechanism — "a wary patrol," not an active fight. This keeps parley a stealth/avoidance tool, not a combat escape hatch (Flee owns that).

## Baseline (what the substrate already ships — verify before adding)

- **Attack noise already exists.** `CombatSystem.playerAttack` (`CombatSystem.java:34`) already calls `state.emitNoise(px, py, ATTACK_NOISE_RADIUS)` with `ATTACK_NOISE_RADIUS = 4`. AC-1's attack half is DONE — do not re-add it; only add the **block** half.
- **The reinforcement-draw is already implemented and pinned.** `NoiseSystem.resolve` (`NoiseSystem.java:22-39`) is the single AD-9 consumer: for each queued `NoiseEvent`, every in-radius (Euclidean) living enemy has UNAWARE→SUSPICIOUS, `setLastSeen(n.x, n.y)` (retarget/investigate), and `setCalmTurns(0)` (hold de-escalation). `LightNoiseTest.lureRaisesUnawareToSuspicious` already proves the lure. AC-1's "draws enemies" is DONE at the consumer — you are only adding a new *producer* (block).
- **The noise channel.** `RunState.emitNoise(x,y,radius)` (`RunState.java:653`) enqueues a `NoiseEvent`; the queue is `transient`, single-turn, drained by `NoiseSystem.resolve` at pipeline step "Noise resolve" (`TurnEngine.java:301`, after `LightSystem.emitNoise`). Player acts first → combat noise is enqueued in the acted step → resolved same turn.
- **DetectionSystem** (`DetectionSystem.java`) owns escalation-on-sight and gradual de-escalation-once-calm; it has a private `deescalate(Detection)` step-down (`ALERTED→SUSPICIOUS→UNAWARE`). There is currently **no public "de-escalate the enemies near a point" entry** — you will add one.
- **Dialogue substrate (Epic 2).** `DialogEffect` (`com.margins.dialog`) is a **sealed** interface permitting `SetFlag, Bond, GiveItem, TakeItem, Disposition` — you must add a new permit. `DialogController.enter`/`apply` (`DialogController.java:99-124`) executes a node's effects and appends SPD-tone lines (observation discipline, Story 2.1). VOICE gating exists: `DialogController.gateValue` returns `player.getVoice()` for `GateStat.VOICE`; `DialogOption.isGated()` routes a passed check to the success branch, a failed check to the failure branch (FR-19, no dice). `DialogNode`/`DialogOption` are pure content-model builders (`withEffect`, `withSpeaker`). `CorneoIntro`/`IntroController` are examples of authored scenes.
- **VOICE is a real stat.** `RoguePlayer.getVoice()` exists (one of the four current stats STR/INSTINCT/GRIT/VOICE). No new stat needed.

## In / Out of Scope Seam

**In scope (4.2):**
- Block emits combat noise (radius 4) through CombatSystem/emitNoise (AC-1, AC-3).
- New `DialogEffect.Deescalate` permit + record (pure model) and its `DialogController.apply` execution (AC-2).
- A public `DetectionSystem` helper that drops in-radius SUSPICIOUS enemies to UNAWARE, so the effect routes detection changes through DetectionSystem, not ad-hoc (AC-2, AC-3).
- ONE authored minimal parley scene (2–3 nodes: hail → VOICE-gated option → success/fail) built in a small authoring helper (mirror `CorneoIntro`).
- A bound parley key in `MarginScreen.readAction`, gated to fire only when a SUSPICIOUS enemy is adjacent; opens the parley scene. How-to-play legend + reachability audit (retro #1).
- Tests: block-noise emission + reinforcement draw (combat-specific), the `Deescalate` effect (SUSPICIOUS→UNAWARE in radius; ALERTED untouched; VOICE-fail no-op), and the parley trigger gate.

**Out of scope (→ Epic 5 / later):**
- Rich per-faction patrol dialogue content, multi-turn negotiation, parley Bond/disposition effects.
- Non-adjacent hailing, parley cooldowns/limits, parley against ALERTED enemies.
- AD-11 occupation escalation (Story 4.3), durability (4.4).

## Tasks / Subtasks

- [ ] **Task 1 — Block emits combat noise (AC-1, AC-3, D2).**
  - [ ] 1.1 Emit a `NoiseEvent` when Block resolves, at `ATTACK_NOISE_RADIUS` (4). Keep combat-noise emission co-located with combat authority: add a small `CombatSystem.blockNoise(RunState)` (or emit in the existing combat path) rather than reaching into the private constant from `TurnEngine`. Wire the `BLOCK` case in `TurnEngine` (`TurnEngine.java:84-88`) to trigger it.
  - [ ] 1.2 Confirm the noise is enqueued in the acted step (player-first) so `NoiseSystem.resolve` draws reinforcements the same turn. Do not touch `NoiseSystem` — it already does the draw.
- [ ] **Task 2 — De-escalation mechanism, routed through DetectionSystem (AC-2, AC-3, D3).**
  - [ ] 2.1 Add a public helper `DetectionSystem.deescalateNear(RunState, int x, int y, int radius, List<String> messages)` (name to taste): for each living enemy within Euclidean `radius`, if `Detection.SUSPICIOUS` → set `UNAWARE` (D3: ALERTED untouched). Emit at most ONE observation line (Story 1.8 AC-2 discipline — a whole patrol reads once, e.g. "The patrol stands down.").
  - [ ] 2.2 Add `DialogEffect.Deescalate(int radius)` to the sealed `permits` list + a record (pure model, `com.margins.dialog`, AD-2 — no rogue deps in the descriptor).
  - [ ] 2.3 Add the `apply(...)` branch in `DialogController` (`DialogController.java:112`) mapping `Deescalate` → `DetectionSystem.deescalateNear(state, player x/y, radius, ...)`, returning its observation line (the sealed-exhaustive instanceof chain stays exhaustive).
- [ ] **Task 3 — Minimal authored parley scene + VOICE gate (AC-2, D1).**
  - [ ] 3.1 Author a small parley scene (mirror `CorneoIntro`): a hail node → a VOICE-gated `DialogOption` → success node carrying `DialogEffect.Deescalate` ("You talk them down.") and a failure node (they stay wary — no de-escalation; a terse line). Keep it 2–3 nodes.
  - [ ] 3.2 Route the gate on `GateStat.VOICE` so `gateValue` compares `player.getVoice()` (FR-19). Pick the gate threshold consistent with existing VOICE gates in the codebase (check `CorneoIntro`/existing scenes for the convention).
- [ ] **Task 4 — Reachable parley trigger + screen wiring + reachability gate (AC-2, retro #1).**
  - [ ] 4.1 Bind a parley key (propose **T** = Talk/parley) in `MarginScreen.readAction`, active only when a SUSPICIOUS enemy is adjacent to Klein (reuse the adjacency/`enemyAt` helpers). On press, open the parley scene via the existing dialogue entry (`dialog`/DialogController + the screen's dialogue mode).
  - [ ] 4.2 Add `T  Parley` to the how-to-play EXPLORE legend (`renderHowToPlayPage`). Reachability audit: confirm the parley path is keyboard-reachable and does nothing (no turn, a short "No one to talk to." or silent) when no wary patrol is adjacent.
- [ ] **Task 5 — Tests + verification (all ACs).**
  - [ ] 5.1 AC-1: a block enqueues exactly one `NoiseEvent` at Klein's tile, radius 4; then `NoiseSystem.resolve` raises an in-radius UNAWARE enemy to SUSPICIOUS and retargets it (combat-specific pin, distinct from `LightNoiseTest`).
  - [ ] 5.2 AC-2/D3: `DetectionSystem.deescalateNear` drops in-radius SUSPICIOUS→UNAWARE, leaves ALERTED and out-of-radius enemies untouched; the `Deescalate` `DialogEffect` executes via `DialogController` and de-escalates; a VOICE-fail branch does NOT carry the effect (no de-escalation).
  - [ ] 5.3 Parley trigger: the key produces a parley action only when a SUSPICIOUS enemy is adjacent (gate), and is a no-op otherwise.
  - [ ] 5.4 Full suite green via the `docs/BUILD.md` recipe (`mvn -o clean install`), no regressions (currently 416 tests). **Verify:** all green, boot clean.

## Dev Notes

### Current state (what exists, to preserve)

- **CombatSystem is the single combat authority (AD-4).** Attack noise lives here already (`playerAttack` → `emitNoise(..., ATTACK_NOISE_RADIUS)`). Put block noise here too so all combat noise has one home; expose it to `TurnEngine`'s BLOCK case with a method, not by leaking the private constant.
- **NoiseSystem.resolve is the ONLY noise consumer (AD-9).** It is complete for this story — do not modify it. Block noise is just another producer feeding the same queue. The queue is `transient` and single-turn; player-first ordering means combat noise resolves the same turn.
- **DetectionSystem owns all detection transitions.** Route de-escalation through a new *public* helper there — do NOT mutate `e.setDetection(...)` from `DialogController` directly (AC-3 discipline: detection changes belong to DetectionSystem/NoiseSystem). Reuse the Euclidean in-radius test pattern from `NoiseSystem.resolve` (`dx*dx + dy*dy <= r*r`).
- **DialogEffect is sealed — adding a permit is a deliberate, small edit.** The `apply` instanceof chain in `DialogController` is exhaustive by construction; add exactly one branch. The effect *descriptor* stays pure (AD-2); the *execution* (touching enemies/DetectionSystem) lives in the controller — mirror how `GiveItem`/`Bond` keep the record dumb and the mapping in the controller.
- **Observation discipline (Story 1.8 AC-2 / 2.1).** De-escalating a whole patrol emits ONE line, not N. Every player-facing change (talked-down, block clang if you choose to log it) either logs or is deliberately silent — no silent state mutation.

### AD / architecture references

- FR-12 (combat viable-but-costly; noise feeds detection) — `[Source: epics.md:42, epics.md:135, epics.md:524-538]`
- AD-9 (noise is a `NoiseEvent` on a per-turn queue; `NoiseSystem.resolve` is the single consumer; never touches actors directly) — `[Source: architecture/.../ARCHITECTURE-SPINE.md:114-129]`
- FR-19 (VOICE is the primary dialogue gate — "talk down a patrol") — `[Source: GateStat.java; DialogController.java:86-90]`
- AD-2 (no libGDX/rogue deps in the pure `com.margins.dialog` model) — `[Source: DialogEffect.java header]`
- AD-4 pipeline (player acts first; Noise resolve step after Detection) — `[Source: TurnEngine.java:297-301]`

### Previous-story intelligence (Story 4.1, just completed)

- **Reachability gate is a hard requirement (Epic 3 retro action item #1).** 4.1 bound all five combat actions to keys AND documented them in the how-to-play legend. Do the same for Parley (T): bound + legend + audit. A test-only-reachable feature fails review.
- **AD-6 note:** this story adds NO new persisted field (block noise is transient queue; de-escalation mutates existing `Detection`; the parley scene is authored content). No migration needed — but if that changes, a field-absent save must load a deterministic default (bit twice historically).
- **Value-preserving / co-locate-with-authority pattern:** 4.1's code review flagged combat logic that reached across module boundaries; keep block noise inside CombatSystem and detection changes inside DetectionSystem — don't scatter.
- **Sort-a-copy / don't-mutate-shared-list lesson:** if you iterate `state.getEnemies()` for the radius sweep, do not reorder or structurally mutate it.

### Project Structure Notes

- New/edited files (expected): `CombatSystem.java` (block noise) or `TurnEngine.java` (BLOCK wiring); `DetectionSystem.java` (public de-escalate helper); `dialog/DialogEffect.java` (new permit+record); `DialogController.java` (apply branch); a small parley-scene authoring class in `rogue/narrative` (mirror `CorneoIntro`); `MarginScreen.java` (parley key + legend); new/updated tests under `core/src/test/...`.
- Build/verify: `docs/BUILD.md` recipe — `mvn -o clean install` (CI-truth, both modules + full suite); `mvn -o -pl core test -Dtest=<Class>` for a single class; `exec:java` needs `mvn -o -pl core install` first and a display (unavailable in CI/headless).

## Dev Agent Record

### Agent Model Used

Claude Opus 4.8 (1M context) — create-story 2026-08-12.

### Debug Log References

### Completion Notes List

### File List

## Change Log

- 2026-08-12 — created by create-story. Scope decisions D1 (AC-2 = mechanism + one reachable trigger; rich faction dialogue → Epic 5), D2 (block noise = attack radius 4), D3 (parley de-escalates SUSPICIOUS→UNAWARE only) confirmed with Justine. Substrate audit: attack noise + `NoiseSystem.resolve` draw already ship and are pinned; the real work is block noise + the VOICE de-escalation mechanism + one reachable parley trigger.

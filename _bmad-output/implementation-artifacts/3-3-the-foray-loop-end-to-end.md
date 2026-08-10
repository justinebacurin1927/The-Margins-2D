---
baseline_commit: 4445594
---

# Story 3.3: The foray loop, end to end

Status: ready-for-dev

## Story

As Klein,
I want a complete leave→travel→scavenge→return arc under the clock,
So that a day's foray is a real risk/reward decision (FR-10).

## Acceptance Criteria

**AC-1 (FR-10):** Given a safe point and daylight, when I travel east to a World-Structure, scavenge under its hazard, and return, then the loot is carried back and the arc is one continuous traversal (no floor transitions).

**AC-2 (FR-10):** Given a foray in progress, when turns pass, then Hunger (~650 to death), Thirst (~530), Temperature (~38 to Frozen under Cold Snap), and the 170-turn clock all compete for the same turns — travel, scavenge, and return draw from one budget.

**AC-3 (UJ-2 edge case):** Given night catches me mid-return, when I lack light, then the return is more dangerous (shrunken FOV, active night hazards) — the overreach is concrete.

## Baseline (what the substrate already ships)

Story 3.3 **does not** build the foray loop from scratch. The `4445594` baseline (committed before this story) already provides the full mechanical substrate:

- **One continuous traversable Herois region** (AD-8, Story 1.1/3.1): no floor transitions anywhere — `RunState.MAP_W/H = 96×48`, `FloorGenerator.generate` stamps the landmark skeleton + procedural wilderness. AC-1's "one continuous traversal" is already true structurally.
- **All 11 World-Structures placed with loot + hazard** (Story 3.2): `StructureTable` (tier/loot/hazard metadata), `placeStructureLoot` (authored loot pass after `placeFloorActors`), `HazardSystem.step` hooking `TurnEngine`'s successful-move branch. The "scavenge under its hazard" leg is live.
- **All four survival tracks + the 170-turn clock tick on real turns** (Story 1.1–1.8): `HungerSystem`/`ThirstSystem`/`TemperatureSystem`/`DebuffSystem`/`SpoilageSystem` run on the acted path (AD-4); `RunState.tickClock` advances `clockTurns` and rolls per-cycle Weather (FR-5). **AC-2's "compete for the same turns" is already true mechanically** — every acted turn draws from one budget.
- **The day/night clock is derived and queryable** (`RunState.isDay()`/`getClockPhase()`, `DayPhase.DAY/NIGHT`, `DAY_LENGTH=100`/`NIGHT_LENGTH=70`/`CYCLE_LENGTH=170`).
- **Night already shrinks FOV** (Story 1.4, AD-18): `FovSystem.radiusFor` returns `DARK_RADIUS=4` at night or fog, `LIT_RADIUS=6` with a light, `DAY_RADIUS=8` clear day. AC-3's "shrunken FOV" is live.
- **Light exists and is core** (Story 1.6): torch (`craftTorch`, 60-turn burn, `TorchSystem`), campfire (`BUILD_CAMPFIRE`), `state.hasLight()`. A lit source emits a per-turn `NoiseEvent` (AD-18). AC-3's "active night hazards" and the light counter are buildable on this.
- **Scavenge is pickup + inventory** (Story 1.5/3.1): `PICKUP` action, `Inventory.tryAdd`, `FloorItem`, `placeFloorActors` eastness scatter. Loot carried back = inventory already travels with Klein across the continuous map.
- **A safe point exists**: Corneo / the home cluster (west, `eastness ≤ 0.2f` — Story 3.1's safe tier) is enemy/supply-free. `WorldSpine` exposes `eastness(x)`/`dangerAt(x)`.

**What the baseline does NOT have — Story 3.3's actual scope:**

- **No foray-loop framing or budget-awareness.** Nothing tells the player "you are out on a foray" vs "at a safe point"; nothing computes or communicates the turn budget (how far the day's remaining light reaches, when night will flip). The day is not yet "the planning unit" (UJ-2).
- **No "return before night" mechanical pressure beyond the raw clock.** Night's *FOV* shrink exists (1.4), but "active night hazards" beyond FOV — the concrete overreach UJ-2 demands — is not wired. AC-3's "active night hazards" is the story's risk leg.
- **No inventory-foray interaction**: capacity is not a constraint yet (no weight — Epic 6), so "haul the loot back" is trivial once picked up. 3.3 should not build weight; it should make the *turn* budget the constraint.
- **No message-log / HUD surface for the foray state.** The log shows the last 5 lines; there is no "X turns until nightfall" / "day 2, phase NIGHT" readout beyond the raw `clockTurns` in the top panel (Story 1.8).

## In/Out of Scope Seam

**IN:**
- The **foray-loop framing** as a core-owned, derived concept: Klein is "out on a foray" whenever he is away from the safe point (the home cluster / campfire radius), and the game *communicates* the budget (turns-until-night, day count) and the arc.
- **The clock as the planning unit** (AC-2): a turn-budget readout (turns until nightfall, day count) and the honest framing that travel/scavenge/return draw from one 170-turn cycle. No new persisted field — derive from `clockTurns` + `WorldSpine`.
- **AC-3's "return is more dangerous" concrete overreach**: night actively changes risk on the return leg. This is the story's *risk* leg and is deliberately scoped so Story 3.4's night-location-danger flips build on it, not replace it.
- **The loot-carried-back proof** (AC-1): the loot gained at a structure is in the inventory when Klein returns — a test that walks the arc and asserts it.
- Content wiring + the AC pins + full-suite/boot verification.

**OUT (later stories):**
- **Night/weather location-danger flips** (Graveyard undead, Sunken Well creature, Poacher's Camp patrols, Beehive Grove safer) — **3.4**. 3.3 builds the *generic* "the return leg at night is more dangerous" mechanism + a seam; 3.4 authors the per-location night states.
- **SKILL-governed outcomes and knowledge querying** (lockpicking, map-fragment knowledge) — **3.5**.
- **Weight / carrying capacity as a foray constraint** (better bags extend foray range) — **Epic 6**. The turn budget is 3.3's constraint; weight is 6.1's.
- **Occupation escalation / combat density** (the eastern garrison thickening) — **Epic 4**. 3.3's night risk is hazard/light-based, not combat-density.
- **The campfire/camp as a first-class "home base" concept** (a place Klein returns to for safety) — the safe-point notion is *derived* here (home cluster), not a buildable home-base system. That is a later story's scope.
- **The Deep Cave Mouth interior / Region-2** — AD-12 / a later region pass.
- Any new `RogueTile` values — reuse existing 7. Night hazards are a metadata + step-trigger (the 3.2 precedent), never a new tile.
- Currency, traders, bag durability — **Epic 6**.

## Design Decisions (the interpretation calls)

1. **The foray loop is a derived, core-owned concept, not a persisted state machine.** "On a foray" = Klein is not at a safe point (outside the home-cluster / campfire radius — the safe tier `eastness ≤ 0.2f`, Story 3.1's convention, plus a campfire radius for the "camp" case). It is **derived from position + `clockTurns`** (AD-6 — no new persisted field; the `WorldSpine`/`DayPhase` precedent). The game communicates it via the message log + HUD, not via a state machine. This keeps the loop honest (no save/load desync, no extra persisted state) and keeps 3.3's scope minimal.
2. **The budget is the 170-turn clock, made visible.** AC-2 is already mechanically true (one clock, all tracks draw from it). 3.3 makes it *felt*: a "turns until nightfall" readout (derived from `clockTurns % CYCLE_LENGTH` vs `DAY_LENGTH`), a day count (`clockTurns / CYCLE_LENGTH`), and a log line when the day/night flips ("Dusk falls — the forest grows close." / "Dawn breaks."). The **day is the planning unit** (UJ-2). No new persisted field; the day count is derivable from `clockTurns`.
3. **AC-3's "active night hazards" = a night overlay on the existing hazard system, with a clearly-marked seam.** Story 3.4 owns per-location night flips; 3.3 builds the *generic* night-risk mechanism: while it is night AND Klein is not under a light (the UJ-2 "I lack light" precondition), the existing `HazardSystem.step` path gets a **night overlay** — a deterministic-per-step risk that is the concrete overreach (e.g. a stumble/fall risk in the dark on the return leg, distinct from any structure's authored hazard). The overlay is data/step-trigger only (the 3.2 precedent) — no new tile, no new persisted field, no noise, no extra clock tick. **Critically: this must be designed so 3.4 can extend it per-location** (a seam `HazardSystem` resolves: base hazard OR night-override) without 3.3 building 3.4's content.
4. **Light is the counter and must actually matter.** AC-3's precondition is "I lack light" — so a lit torch (Story 1.6) should *suppress* the generic night stumble (you can see the ground). The light-suppresses-night-risk rule is core (AD-2, AD-18). It must not create a free loop: a torch has a 60-turn burn and consumes Wood+Coal — the cost is real.
5. **The loot-carried-back proof is the AC-1 pin.** A test walks the arc: Klein starts at a safe point in daylight, travels east to a structure, scavenges (picks up a guaranteed-authored loot item), and returns to the safe point — asserting the item is still in the inventory (the continuous-map carry). "One continuous traversal (no floor transitions)" is asserted by the absence of any descend/transition call in the arc.

## Tasks / Subtasks

- [ ] **Task 1 — The foray-loop derivation + safe-point query (AC: 1)**
  - [ ] Core-owned, derived: a `Foray` query (pure, in `RunState` or a `rogue/Foray`/`world` helper — the `WorldSpine` precedent) answering: is Klein on a foray (away from the safe point — outside the home-cluster safe tier `eastness ≤ 0.2f` AND outside the campfire radius)? Turns-until-nightfall (from `clockTurns % CYCLE_LENGTH` vs `DAY_LENGTH`)? Day count (`clockTurns / CYCLE_LENGTH`)?
  - [ ] Derived, not persisted (AD-6): no new `RunState` field. The safe point = the Corneo home cluster (safe tier `eastness ≤ 0.2f`) + a built campfire's radius — both already queryable.
  - [ ] Tests: on-foray is false at the spawn/home-cluster, true at a mid-map/east position; turns-until-nightfall and day count derive correctly at cycle boundaries; a campfire counts as a safe point.

- [ ] **Task 2 — The budget readout + day/night log lines (AC: 2)**
  - [ ] A "turns until nightfall" / day-count readout in the top HUD panel (Story 1.8's panel — the existing `time` string at `MarginScreen.renderStatusPanel`). Core-owned derivation; the screen only renders it (AD-1/AD-2).
  - [ ] A log line at each day/night boundary ("Dusk falls — the forest grows close." at the DAY→NIGHT flip, "Dawn breaks." at NIGHT→DAY) — core-owned (AD-4), emitted by the acted-turn pipeline like the Weather `onsetLine` (Story 1.3's pattern).
  - [ ] Tests: the readout values are correct at Day 0 / cycle boundaries; the flip lines emit exactly on the boundary turn (once each), and never on a refused (un-acted) turn.

- [ ] **Task 3 — AC-3's night-overlay on the return leg (the concrete overreach) (AC: 3)**
  - [ ] A **generic night-risk overlay** in the existing hazard step-trigger: while it is night AND Klein lacks a light (no torch, no campfire light at his tile), the return leg is riskier — a deterministic-per-step stumble/fall (message + small HP cost) distinct from any structure's authored hazard. The `HazardSystem` step resolves base-hazard OR night-override through a **clearly-marked seam** (a method/extension point 3.4's per-location flips extend) — 3.3 does NOT author per-location night states.
  - [ ] Light is the counter (Decision 4): a lit torch/campfire suppresses the night-overlay risk (you can see the ground). Cost is the 60-turn torch burn + Wood+Coal — no free loop.
  - [ ] Core-layer only (AD-2): effects land on existing surfaces (message log, HP). One seeded `rng` draw per step (AD-5) where the overlay is probabilistic; no new tile, no persisted field, no noise, no extra clock tick.
  - [ ] Tests: at night without light, stepping (the return leg) risks the stumble — deterministic on a fixed seed; at day or with a lit torch it never fires; it fires on the structure step too (the overlay stacks with the authored hazard); the existing `StructureContentTest` hazard suite stays green.

- [ ] **Task 4 — The AC-1 carry-back pin + no-regression (AC: 1)**
  - [ ] A walk-the-arc test: Klein starts at the safe point in daylight, travels east to a structure (e.g. a guaranteed-loot one), scavenges a guaranteed-authored item (e.g. the Hunter's Blind ROPE or Forest Shrine SALT — a non-scatterable or zero-generic-room item, per 3.2's P1 proof pattern), and returns to the safe point — asserting the item is in the inventory (the continuous-map carry), and that no floor-transition/descend call exists anywhere in the arc.
  - [ ] Full suite: `mvn -o -pl core test` — the **355** 3.2-post-review tests stay green, plus the new foray tests. No regressions across the structure/hazard/survival/persistence suites.
  - [ ] Launch: `mvn -o -q -pl core install` + `timeout 40 mvn -o -pl desktop exec:java` — boot clean (the HUD readout + night overlay render, camera still follows).

- [ ] **Task 5 — AC pins + no-forced-scope (AC: all)**
  - [ ] AC-1 pin: Task 4's carry-back arc. AC-2 pin: the budget readout test (Task 2) — the four tracks + clock share one acted-turn budget (already structurally true; pinned). AC-3 pin: Task 3's night-overlay tests.
  - [ ] Scope guard: assert the seam is genuinely a seam (3.4 can extend the night overlay per-location without reworking 3.3's trigger) — a review-visible marker, not speculative 3.4 content.
  - [ ] No new persisted field, no new `RogueTile`, no noise emission from the overlay, no extra clock tick — each pinned by a test or by construction.

## Dev Notes

### Current state (what exists, to preserve)

- **The arc is already one continuous traversal** (AD-8, Story 1.1/3.1): `RunState.MAP_W/H = 96×48`; `FloorGenerator.generate` → `FloorResult` (map + roomCenters + spine). Do NOT re-generate, re-anchor, or add any transition. The pre-AD-8 reject path (gated on the retired `floorDepth` JSON key, never the int `saveVersion`) stays untouched.
- **Structures + loot + hazard are live** (Story 3.2, `2af12dc` + `0af2602`): `StructureTable`, `placeStructureLoot` (additive, AD-5-safe), `HazardSystem.step` at `TurnEngine`'s successful-move branch (`TurnEngine.java:73-76`, guarded on non-zero displacement AND the move actually landing). Do NOT re-scatter or re-hazard.
- **The acted-turn pipeline** (AD-4, `TurnEngine.java:233-268`): Hunger → Thirst → Debuff → Temperature → Spoilage → Weather/clock → Detection → Companion → Enemy → Torch → Light-noise → Noise resolve → Last Stand → FOV. The new day/night boundary log line and any night-risk overlay hook **here** — the pipeline is the single mutation path (AD-3/AD-4).
- **The clock** (`RunState.clockTurns`, derived phase `getClockPhase()`/`isDay()`, `DAY_LENGTH/NIGHT_LENGTH/CYCLE_LENGTH`, `tickClock()` rolls weather at cycle boundary). All new budget/phase reads derive from this — never persist a parallel counter.
- **FOV night shrink already exists** (Story 1.4, AD-18): `FovSystem.radiusFor` = 4 night/fog, 6 lit, 8 clear day. AC-3's "shrunken FOV" is done; 3.3 adds the *hazard* overlay, not a second FOV change.
- **Light sources** (Story 1.6): `state.hasLight()` (campfire at `campfireX/Y` or torch), `craftTorch` (1 Wood + 1 Coal, `TORCH_BURN`), campfire `BUILD_CAMPFIRE` (lights + exposes). `LightSystem.emitNoise` (AD-18). The night-overlay's "I lack light" precondition reads `hasLight()`.
- **The safe tier** (Story 3.1): home cluster `eastness ≤ 0.2f` is enemy/supply-free — the "safe point". `WorldSpine.eastness(x)` is the query.
- **The HUD top panel** (Story 1.8, `MarginScreen.renderStatusPanel`): the `time` string already shows `DAY/NIGHT + clockTurns + weather` (+ torch). The turns-until-nightfall readout extends this string — no new panel.
- **Weather** (`Weather.java`): `onsetLine()` mirrors `label()` — the pattern for the day/night flip log lines.
- **Journal quest keys** (`JournalController`): `QUEST_ROAD_EAST`/`startedKey` pattern — if 3.3 adds a "foray" thread to the Journal, follow it; but a journal thread is NOT required by the ACs.

### Carried lessons (1.x/2.x/3.1/3.2, applied)

- **Derived, never persisted** (AD-6, the `WorldSpine`/`DayPhase`/`getClockPhase` precedent — and the field-absent migration rule that bit 1.3/3.3-identify): every new concept (on-foray, turns-until-night, day count) derives from `position + clockTurns` + constants. If a dev note proposes a `RunState` field, it needs a deterministic default AND a load-time reconcile — the preferred answer here is "no field."
- **One rng draw per event** (AD-5): the night-overlay's probabilistic stumble is exactly one `nextInt` on the seeded stream, exactly like `HazardSystem`/`placeStructureLoot`. The existing byte-identical-scatter pins (3.2's P1) must stay green — do NOT add draws into `placeFloorActors` or the generic scatter.
- **The additive-content pattern** (3.2 Task 3): new mechanics layer ON TOP of the existing seeded stream — never re-draw. The night overlay is a step-trigger, not a new scatter.
- **Hazard = metadata + step-trigger, never a tile/field** (3.2 Decision 2): the night-overlay follows the same shape. No `RogueTile`, no `RunState` field, no noise (only AD-9 emitters emit), no extra clock tick.
- **Observation discipline** (1.8/2.x): any risk/overlay change is invisible until a turn — the AC pins are *turn-level* assertions (deterministic on a fixed seed), not log-line checks.
- **Keep the act→survival-order** (AD-4, Story 1.6/1.8): the night-overlay damage lands before `checkLastStand` (lethal cold honors the Last-Stand reprieve) — follow the existing Hunger/Thirst/Temperature placement.
- **Read map dims dynamically** (3.1): never hard-code 50/96 — `getWidth()/getHeight()` in any new spatial test.

### Scope discipline (CLAUDE.md §2/§3)

- Minimum code: the `Foray` derivation + budget readout + flip log lines + the generic night-overlay + the seam marker + the carry-back pin + tests. **No** per-location night states (3.4), **no** weight/capacity (Epic 6), **no** lockpicking/knowledge (3.5), **no** combat-density escalation (Epic 4), **no** new tile, **no** new persisted field, **no** new noise emitter.
- Do NOT touch the generic wilderness scatter's seeded stream, `placeStructureLoot`, `HazardSystem`'s base resolution, the pre-AD-8 reject gate, or the `Supply.count()-5` pin (3.2 P6 — update it only if 3.3 *adds* a non-scatterable item, which it should not).
- Do NOT build the 3.4 per-location content — build the **seam** (a clearly-marked extension point in the night-overlay resolution) and stop.

### Testing standards

- JUnit 5 headless core; `new RunState(seedL)` (seed 42 for single-run pins; ranges of seeds for the night-overlay distribution). Read dims dynamically.
- Run: `mvn -o -pl core test` (offline). Full reactor: `mvn -o clean install`. Launch: `mvn -o -q -pl core install` + `timeout 40 mvn -o -pl desktop exec:java` (exit 143 = timeout kill = clean boot).
- Baseline stays **355 green** (the 3.2 post-review count); the story adds the foray tests. No regressions across the structure/hazard/survival/persistence/narrative suites.
- The AC pins are deterministic-on-fixed-seed turn-level assertions, not log-line checks.

### References

- [Source: epics.md#Story-3.3 (446–464)] — the three ACs verbatim; UJ-2 edge case (464).
- [Source: prds/prd-The-Margin-2026-08-06/prd.md#FR-10 (211–217)] — the foray loop; time-pressure numbers (216: Hunger ~650, Thirst ~530, Temp ~38 to Frozen, 170-turn cycle).
- [Source: prds/prd-The-Margin-2026-08-06/prd.md#UJ-2 (61–67)] — the concrete journey (safe camp T1 → Sunken Well T2 → return before night); the day is the planning unit.
- [Source: architecture/ARCHITECTURE-SPINE.md#AD-4 (77–82)] — the fixed pipeline (the night-overlay + flip lines hook here).
- [Source: architecture/ARCHITECTURE-SPINE.md#AD-5 (84–88)] — one committed turn per real action; the overlay draws one seeded roll.
- [Source: architecture/ARCHITECTURE-SPINE.md#AD-6 (90–97)] — no new persisted field; the field-absent migration rule.
- [Source: architecture/ARCHITECTURE-SPINE.md#AD-18 (175–179)] — night/light are core; light restores but reduced; light alerts only via the AD-9 noise channel.
- [Source: architecture/ARCHITECTURE-SPINE.md#AD-16 (163–167)] — the coarse turn-cost smoke stays; the worst-case dense-garrison budget is Epic 4/5.
- [Source: story-3.2 (this story's predecessor, Status: done)] — the structure loot/hazard layer; its Review Findings (P1 proof pattern, P2 zero-displacement guard, P4 same-seed determinism) and the Deferred "Supply.count()-5" note (update only if 3.3 adds a non-scatterable item).
- [Source: story-3.1 (Status: done)] — the hybrid map + `WorldSpine` + the safe tier `eastness ≤ 0.2f`.
- [Source: deferred-work.md#Deferred-from-3-1 (240–242)] — the supply-retry-loop / eastness-clamp / O4 notes (none are 3.3 scope).
- [Source: deferred-work.md#Deferred-from-3-2 (244–255)] — the 3.4 night-override seam note ("doesn't exist yet — Story 3.4 owns the seam") — **3.3 builds the seam, 3.4 authors the content.**

## Dev Agent Record

### Agent Model Used

Claude Opus 4.8 (1M context), the session's model.

### Debug Log References

- (populated during dev-story)

### Completion Notes List

- (populated during dev-story)

### File List

- (populated during dev-story)

## Change Log

- 2026-08-10: Created Story 3.3 — The foray loop, end to end (FR-10). Status backlog → ready-for-dev. Sprint status updated.

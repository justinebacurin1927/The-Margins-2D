---
baseline_commit: a4ad53dea8779b23182b7667012de098a61e6108
---

# Story 1.2: Four survival tracks that tick on real turns

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As Klein,
I want Hunger, Thirst, Temperature, and the Day/Night clock to advance as I act,
so that survival is a real, honest pressure (FR-4).

This story builds directly on Story 1.1 (done): the continuous map and the **single collapsed acted-path** in `TurnEngine`. Hunger already exists and matches FR-4 exactly — **ratify it, do not rebuild.** The new work is **Thirst** (full, mirroring Hunger), **Temperature** (the meter + bands + extreme-harm only), and a **Day/Night turn counter** — all ticking only on acted turns (AD-5).

## Acceptance Criteria

*(From epics.md Story 1.2.)*

1. **Given** a run in progress, **when** I take a real action (move, attack, block, wait, use, drop, pickup, distract), **then** Hunger, Thirst, Temperature, and the Day/Night clock each advance by one turn's worth per their tiers/drift rates (Hunger: Well Fed→Satisfied→Hungry→Starving; Thirst: Hydrated→Thirsty→Dehydrated→Parched).
2. **Given** a run in progress, **when** I press a key into a wall or inert tile, **then** no turn is committed and **no** survival track ticks (survival-clock honesty, AD-5) — this must hold for *every* track, not just Hunger.
3. **Given** Hunger reaches Starving or Thirst reaches Parched, **when** the escalation stages are entered, **then** the listed penalties apply — Starving: Fatigue −35% STR → Trembling −15% AG → Rotting −3 HP/2 turns (ratified, existing); Parched: Withered → Trembling → Dried Out, −2 HP/5 turns (new).

## Scope & the 1.2 ↔ 1.3 ↔ 1.6 seam (read first — prevents over-building)

FR-4 says Temperature is "driven by Weather + Day/Night," but **Weather and the Day/Night *cycle* are Story 1.3, and campfire/Cold-Snap mitigation is Story 1.6.** So in **this** story:

- **Thirst** — build it **fully** (it is self-contained, exactly like Hunger).
- **Temperature** — build the **meter only**: a −100..+100 value, the 7 tier bands (Frozen/Cold/Chilled/Neutral/Warm/Hot/Overheated), and HP harm at the extreme bands, ticking on acted turns. Absent any weather driver, it **drifts gently toward Neutral** (the seam). Do **NOT** implement weather-driven drift rates (Cold Snap −2.0/turn → Frozen in ~38t) or the campfire — those are Stories 1.3/1.6. Expose a setter/adjust hook so 1.3/1.6 can push drift later.
- **Day/Night clock** — add a bare persistent **turn counter** on `RunState` that increments on each acted turn. Do **NOT** implement the 100-day/70-night phase split, phase queries, or Weather — that is Story 1.3. This story only establishes that "the clock advances on real turns."

Everything here is the four tracks *existing and ticking honestly*; the environmental *drivers* land in 1.3/1.6.

## Tasks / Subtasks

- [x] **Task 1 — Ratify Hunger (AC: 1, 3) — no code change**
  - [x] Confirm `RoguePlayer` Hunger (WELL_FED 350 / SATISFIED 250 / HUNGRY 250 / STARVING 150; Fatigue −35% STR via `getStr`, Trembling −15% AG via `dodgePercent`, Rotting −3 HP/2t via `tickHunger`) matches FR-4. It does — leave it untouched. Note it as ratified in Completion Notes.
- [x] **Task 2 — Thirst, full (AC: 1, 3)**
  - [x] On `RoguePlayer`, mirror the Hunger structure: a `ThirstStatus` enum {HYDRATED 200, THIRSTY 150, DEHYDRATED 100, PARCHED 80} (turn-countdown per tier), plus `tickThirst()`, `drink(int amount)`, tier drop/rise, and getters (`getThirstStatus`, `getThirst`, a `thirstLabel()` for the HUD).
  - [x] **Field-initialize** the thirst fields (like `status`/`hungerTurns` at RoguePlayer:40-45) so a save predating them loads a valid starting state (AD-6 — see Dev Notes).
  - [x] Dehydrated applies **Headache** (an intrinsic thirst penalty for now — the full Debuff *system* with stacking/cures is Story 1.7; do not build it here). Parched runs 3 stages **Withered → Trembling → Dried Out** with a **−2 HP/5 turns** damage cadence (mirror the `starveTick` cadence pattern in `tickHunger`).
  - [x] Parched's **Trembling** stage applies the −15% AG penalty. Hunger's Starving-Trembling already does this in `dodgePercent()` — make the AG penalty apply if **either** source is in its Trembling stage; do **not** double-apply (one −15%, not −30%, when both).
- [x] **Task 3 — Temperature meter (AC: 1) — meter/bands/harm only**
  - [x] On `RoguePlayer`, add an `int temperature` in [−100, +100] (field-initialized to 0 = Neutral), a `TempBand` derivation (Frozen ≤ −80 / Cold / Chilled / Neutral / Warm / Hot / Overheated ≥ +80 — pick thresholds consistent with the bible's bands), `getTemperature()`, `getTempBand()`, a `tempLabel()`, and an `adjustTemperature(int delta)` hook for 1.3/1.6 to drive later.
  - [x] `tickTemperature()`: HP harm at the extreme bands (Frozen/Overheated) on a cadence; otherwise **drift toward 0 by a small fixed step** (the driver-less baseline). No weather logic.
- [x] **Task 4 — Day/Night turn counter (AC: 1)**
  - [x] On `RunState`, add a persistent `int clockTurns` (field-initialized 0) and a `tickClock()` that increments it; a `getClockTurns()`. No phase/weather logic (Story 1.3).
- [x] **Task 5 — Wire the ticks into the single acted-path (AC: 1, 2)**
  - [x] In `TurnEngine`'s `if (acted)` block, immediately after `HungerSystem.tick(player)`, add the new ticks in the survival group: `ThirstSystem.tick(player)`, `TemperatureSystem.tick(player)`, and `state.tickClock()`. Preserve the AD-4 order (survival ticks first, then detection → companion → enemy → noise → **checkLastStand** → FOV). Because Thirst/Temperature can deal lethal HP loss, they must run **before** `checkLastStand` so the reprieve covers them (mirror how Hunger sits before `checkLastStand`).
  - [x] Create `ThirstSystem` and `TemperatureSystem` (System-suffix, `system/` package, mirroring `HungerSystem` — a one-line `tick(player)` delegating to the player method). This keeps the pipeline uniform (AD-4).
  - [x] Verify AC-2: a wasted keypress (`acted == false`) still ticks nothing — all four new ticks are inside the `if (acted)` block, so a wall-bump commits no turn. Add/extend a test that asserts *all* tracks are unchanged after a blocked move.
- [x] **Task 6 — Serialization round-trip (AC: 1) — AD-6, per Story 1.1 learnings**
  - [x] The new persisted fields (thirst state + countdown + stage timers, temperature, `clockTurns`) must survive save/load. `SaveService` already uses `usePrototypes(false)` (Story 1.1), so they will always serialize; field-initialization covers old saves. Extend `RunStatePersistenceTest.vitalsInventoryIdentitiesAndLastStandSurviveRoundTrip` (or add a sibling) to mutate and assert thirst status/turns, temperature, and clockTurns round-trip.
- [x] **Task 7 — Tests**
  - [x] `ThirstSystemTest` (mirror `HungerSystemTest`): starts Hydrated at full duration; drains Hydrated→Thirsty→Dehydrated→Parched over acted turns; Parched stages Withered→Trembling→Dried Out; −2 HP/5t cadence; `drink()` rises tiers.
  - [x] `TemperatureSystemTest`: band thresholds; extreme-band HP harm cadence; driver-less drift toward Neutral; `adjustTemperature` moves the meter and clamps to [−100, +100].
  - [x] A `TurnEngine` survival-honesty test: a blocked move ticks none of the four tracks (AC-2); a real action ticks all four once.
  - [x] Full suite green: `mvn -o clean install`.

## Dev Notes

### Current state (what exists, what to ratify, what to preserve)

- **Hunger is complete and FR-4-accurate** (`RoguePlayer.java:15-265`). The enum durations, the Starving `starveTick` cadence (Rotting −3 HP/2t), the STR/AG penalties (`getStr`, `dodgePercent`), Bloated regen/slow, and the eat/drop-tier logic all match the PRD. `HungerSystem.tick(player)` (`system/HungerSystem.java`) is the pipeline step. **Do not touch any of it** — Thirst is built as a parallel sibling, not a refactor of Hunger into a shared abstraction (that abstraction is not requested; keep it simple, CLAUDE.md §2).
- **The tick site** is the post-1.1 single acted-path in `TurnEngine.java` (the `if (acted)` block): `HungerSystem.tick` → `DetectionSystem.update` → `CompanionSystem.follow` → `CombatSystem.enemyPhase` → `NoiseSystem.resolve` → `checkLastStand` → `FovSystem.compute`. The new survival ticks join the survival group at the top, **before `checkLastStand`** (so lethal thirst/cold honors the Last-Stand reprieve, exactly as hunger does).
- **AD-5 honesty is already structural**: `acted` is only true for real actions; a wall-bump leaves `acted=false` and the whole `if (acted)` block is skipped. Putting the new ticks inside that block gives AC-2 for free — but it must be *tested* across all four tracks, since the AC calls it out explicitly.

### Serialization — the Story 1.1 learnings that apply directly

- `SaveService.json()` now sets `usePrototypes(false)` (Story 1.1), so every field — including the new survival fields — is always written. Do **not** re-enable prototype omission.
- **Field-initialize** every new persisted field (e.g. `private ThirstStatus thirstStatus = ThirstStatus.HYDRATED;`, `private int temperature = 0;`, `private int clockTurns = 0;`) so a save written before these fields loads a valid default — this is the established pattern (`RoguePlayer.java:38-45`, `RunState` inventory/flagStore) and libGDX `fromJson` runs field initializers.
- `RoguePlayer` has a private no-arg constructor for Json (`RoguePlayer.java:55`); new fields serialize under the player automatically. `clockTurns` on `RunState` serializes under the run root.
- New enums (`ThirstStatus`, `TempBand`) serialize as their name by libGDX Json — no element-type registration needed (unlike the `Map`/`List` fields in `SaveService.json()`).

### Placement rationale (AD-3)

- **Thirst and Temperature live on `RoguePlayer`** (body meters, alongside Hunger). **The Day/Night clock lives on `RunState`** (world time, shared). `RunState` remains the single owner (AD-3); nothing else holds authoritative survival state.

### Scope discipline (CLAUDE.md §2/§3)

- Do NOT build: Weather, the Day/Night 100/70 phase split or phase queries (Story 1.3), weather-driven temperature drift or the campfire/Cold-Snap mitigation (Story 1.6), or the full Debuff system with stacking/cures (Story 1.7). Thirst's Headache/Withered/Trembling/Dried-Out are **intrinsic tier penalties** here, not Debuff-system entries.
- Do NOT refactor Hunger into a shared "survival track" base class — three parallel implementations are simpler than a speculative abstraction for this story.

### Testing standards

- JUnit Jupiter 5.10.2, `mvn -o clean install` (surefire 3.2.5). Mirror `HungerSystemTest` (`core/src/test/.../HungerSystemTest.java`): `new RunState(seed).getPlayer()`, loop the tick, assert tier transitions and HP cadence. Reference `RunStatePersistenceTest` for the round-trip pattern (it now covers vitals/inventory/identify/Last-Stand after Story 1.1).

### Project Structure Notes

- Core stays headless (AD-2): no libGDX render/input types in `com.margins.rogue.*`. The new systems + player fields are pure model.
- New files: `system/ThirstSystem.java`, `system/TemperatureSystem.java`, `ThirstSystemTest.java`, `TemperatureSystemTest.java`. Modified: `RoguePlayer.java` (thirst + temperature), `RunState.java` (clockTurns), `TurnEngine.java` (wire ticks), `RunStatePersistenceTest.java` (round-trip). The HUD surfacing of the new tracks is **Story 1.8** — do not build HUD here (this story is headless model + pipeline).

### References

- [Source: _bmad-output/planning-artifacts/epics.md#Story-1.2] — the AC source.
- [Source: _bmad-output/planning-artifacts/prds/prd-The-Margin-2026-08-06/prd.md#FR-4] — the tier durations and penalties (Hunger/Thirst/Temperature numbers).
- [Source: ARCHITECTURE-SPINE.md#AD-4] — the fixed pipeline; survival ticks precede checkLastStand.
- [Source: ARCHITECTURE-SPINE.md#AD-5] — a turn commits only on a real player action.
- [Source: ARCHITECTURE-SPINE.md#AD-3] — RunState single owner; player owns body meters.
- [Source: ARCHITECTURE-SPINE.md#AD-6] — serialization; field-init for old-save safety; usePrototypes(false) from Story 1.1.
- [Source: _bmad-output/implementation-artifacts/1-1-retire-floor-descent-for-one-continuous-map.md] — the acted-branch shape, the save-serialization notes, and the round-trip test to extend.

## Dev Agent Record

### Agent Model Used

claude-opus-4-8 (1M context)

### Debug Log References

- Green baseline: 51 tests (HEAD a4ad53d).
- First full run: 1 failure — `ThirstSystemTest.parchedRunsWitheredThenTremblingThenDriedOut` (test loop `while(thirst>26)` overshot the Trembling stage into Dried Out; off-by-one in the *test*, not the code). Fixed the loop bound to `>53`.
- Final: `mvn -o clean install` → **65 tests, 0 failures, BUILD SUCCESS**.

### Completion Notes List

- **All 7 tasks + AC-1/2/3 satisfied.** Hunger ratified (untouched). Thirst built fully on `RoguePlayer` (Hydrated 200 → Thirsty 150 → Dehydrated 100 → Parched 80; Parched stages Withered→Trembling→Dried Out with −2 HP/5t). Temperature built as **meter-only** (−100..+100, 7 bands, **−1 HP per turn** at Frozen/Overheated, drift toward Neutral) — harm-per-turn per review P1: a driver-less meter is only briefly extreme, so the original 3-turn cadence almost never fired; Story 1.6 re-calibrates under a real weather driver. `clockTurns` counter on `RunState`. All wired into the `TurnEngine` acted-branch **before `checkLastStand`** (so lethal thirst/cold honors the reprieve).
- **The Trembling −15% AG penalty is shared, not stacked** (per the story): refactored `dodgePercent()` to use a single `isTrembling()` that fires on Starving-and-worse OR Parched-and-worse, applied once. Tested by `parchedTremblingReducesDodgeButDoesNotStackWithHunger`.
- **Seam held** — no Weather, no Day/Night phase split, no campfire, no Debuff system. Temperature exposes `adjustTemperature(delta)` as the hook for Stories 1.3/1.6 to drive later; the Day/Night counter is a bare `clockTurns` for Story 1.3 to build phases on. Thirst's Headache/Withered/Trembling/Dried-Out are intrinsic tier states, not Debuff-system entries.
- **Serialization** — new fields field-initialized (old-save safe, per Story 1.1); `usePrototypes(false)` already ensures they serialize. `thirstTemperatureAndClockSurviveRoundTrip` proves it.
- **AD-5 honesty tested across all four tracks** — `SurvivalTickTest`: a wall-bump ticks none of hunger/thirst/temperature/clock; a real `WAIT` ticks all four.

### File List

**Modified**
- `core/src/main/java/com/margins/rogue/RoguePlayer.java` — `ThirstStatus`/`TempBand` enums; thirst + temperature fields (field-initialized); `tickThirst`/`drink`/thirst tier logic/`thirstLabel`/getters; `tickTemperature`/`getTempBand`/`adjustTemperature`/`tempLabel`/getters; `dodgePercent` refactored to shared `isTrembling()`.
- `core/src/main/java/com/margins/rogue/state/RunState.java` — `clockTurns` field, `tickClock()`, `getClockTurns()`.
- `core/src/main/java/com/margins/rogue/system/TurnEngine.java` — wired `ThirstSystem`/`TemperatureSystem`/`state.tickClock()` into the acted-branch before `checkLastStand`.
- `core/src/test/java/com/margins/rogue/state/RunStatePersistenceTest.java` — added `thirstTemperatureAndClockSurviveRoundTrip` (+ `RoguePlayer` import).

**Added**
- `core/src/main/java/com/margins/rogue/system/ThirstSystem.java`
- `core/src/main/java/com/margins/rogue/system/TemperatureSystem.java`
- `core/src/test/java/com/margins/rogue/ThirstSystemTest.java`
- `core/src/test/java/com/margins/rogue/TemperatureSystemTest.java`
- `core/src/test/java/com/margins/rogue/SurvivalTickTest.java`

## Change Log

- 2026-08-06 — Implemented Story 1.2 (four survival tracks tick on acted turns, FR-4). Thirst full; Temperature meter-only; Day/Night bare counter — respecting the 1.2↔1.3↔1.6 seam. 5 files changed, 5 added. 51 → 65 tests green.
- 2026-08-06 — Code review (3 parallel layers: Blind Hunter, Edge Case Hunter, Acceptance Auditor). Verdict: **Approve** — AC-1/2/3 met, AD-4/5/6 satisfied, 1.2↔1.3↔1.6 seam held. 3 patches applied (see Review Findings), 2 deferred, 10 dismissed. 65 → 66 tests green.

## Senior Developer Review (AI)

**Date:** 2026-08-06 · **Outcome:** Approve (with patches applied)

Acceptance Auditor confirmed AC-1/2/3 fully met and AD-3/4/5/6 satisfied — and, critically, that the 1.2↔1.3↔1.6↔1.7 seam held (no Weather, no Day/Night phase split, no campfire, no Debuff system). Edge Case Hunter + Blind Hunter each independently flagged the same real defect (extreme-band harm was effectively unreachable); both other patches trace to Blind Hunter. The Dev-record "65 tests green" was correct as of the dev run; the review lands at 66.

### Review Findings

**Patches (applied this review):**
- [x] [Review][Patch] Make extreme-band harm reliable in `tickTemperature()` [core/.../RoguePlayer.java] — Edge Case Hunter + Blind Hunter (Med): with the driver-less drift toward Neutral running in the same tick, the old `tempTick % 3` cadence almost never fired (the meter exits Frozen/Overheated in ~20 turns; the counter needed 3, so a player was nearly never at −100 → −1 HP/3t actually landed). Fixed to **harm every turn** spent at FROZEN/OVERHEATED and keep the drift; deleted the `tempTick` field (also moots the "unbounded counter" Low). PRD FR-4 does not lock the cadence — the numbers are "starting calibration," re-balanced in Story 1.6 under a real weather driver. Test rewritten as `extremeBandHarmsEveryTurnAndStopsOutside` (harms 1/turn while Frozen, stops at the band exit).
- [x] [Review][Patch] Mirror production's `usePrototypes(false)` in the round-trip test serializer [core/.../state/RunStatePersistenceTest.java] — Blind Hunter (Med): `json()` registered element types like `SaveService.json()` but omitted `setUsePrototypes(false)`, so the test serializer diverged from production — capable of masking save-format bugs. Fixed; `preFlagStoreSaveLoadsEmptyNotNull` (which removes a key to emulate an old save) still passes with prototypes off.
- [x] [Review][Patch] Add the Last-Stand reprieve test for lethal exposure [core/.../SurvivalTickTest.java] — Blind Hunter (Low): no test pinned that a lethal thirst/cold tick honors the one-per-run reprieve. Added `lethalTemperatureHonorsLastStandReprieve`: player at 1 HP and Frozen → the temp tick lands the lethal −1 → `checkLastStand` revives to 1, sets `lastStandUsed`/`lastStand`, announces "Last Stand!". This also **pins the AD-4 ordering** (survival ticks before `checkLastStand`); the same ordering covers thirst since both tick in the survival group.

**Deferred (real, but out of Story 1.2 scope — see `deferred-work.md`):**
- [x] [Review][Defer] HUD labels for the new tracks unwired [core/.../RogueGameScreen.java] — Blind Hunter (Med): `thirstLabel()`/`tempLabel()`/`getClockTurns()` exist in the model but the screen only draws `hungerLabel`; the new tracks are invisible in-game. This is Story 1.8's explicit scope ("do not build HUD here") — the labels were built headless for 1.8 to wire. Deferred → Story 1.8, which now knows the exact methods to surface.
- [x] [Review][Defer] Driver-less drift vs. harm balance [core/.../RoguePlayer.java] — Edge Case Hunter: with harm-per-turn (P1) the meter punishes a transient extreme (≈20 HP drifting out of Frozen) — correct for "exposure is lethal," but the real rates live with a weather driver. Deferred → Story 1.6 calibrates harm/drift once Cold Snap/fire exist.

**Dismissed (noise / by-design):** NEUTRAL band asymmetry (thresholds are the consistent ±15/±50/±80 — no defect); Parched phase desync / dead-on-load (stages derive from the persisted `thirstTurns`, and `parchTick` resets on re-entry — a load can't desync); `drink()` escapes lethal Parched (rising a tier on water is intended recovery); leftover `waterPoints` stranded (partial-drink accumulator, mirrors Hunger's `foodPoints`); no `isAlive()` gate on ticks (mirrors the pre-existing Hunger pattern; `checkLastStand` runs after the survival group, so a dead actor's remaining ticks are moot); `drink()` doesn't reset `parchTick` (only read under PARCHED, and re-entry resets it — harmless); parchedStage 53/26 boundaries unpinned exactly (the traversal tests exercise them); unbounded `clockTurns` int overflow (~2.1B acted turns vs. a ~600-turn run — unreachable; Story 1.3 restructures the clock into phases anyway); no `dehydrate()` counterpart (nothing forces thirst down yet — YAGNI until a driver exists); no harm-gate test across every non-extreme band (one shared gate; Warm + the new transition-out assertion cover the else-path and the boundary).

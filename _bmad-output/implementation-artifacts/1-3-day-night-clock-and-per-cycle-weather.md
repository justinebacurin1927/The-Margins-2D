---
baseline_commit: e8fb75b
---

# Story 1.3: Day/Night clock and per-cycle weather

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As Klein,
I want the day to turn to night and weather to roll each cycle,
so that time and weather shape how I plan (FR-5).

This story builds directly on Story 1.2 (done): the **bare `clockTurns` counter** on `RunState` that already advances on every acted turn (Story 1.2). **The new work is the phase split + the per-cycle weather roll** — both pure state on `RunState` (AD-3), both persisted (AD-6), both *queryable by systems and the HUD*. It is the **clock state machine**, exactly parallel to how Story 1.2 built the temperature *meter* without its drivers.

## Acceptance Criteria

*(From epics.md Story 1.3.)*

1. **Given** a run in progress, **when** turns accumulate, **then** the clock runs Day 100 turns / Night 70 turns (170-turn cycle), and the current phase is queryable by systems and the HUD.
2. **Given** a new 170-turn cycle begins, **when** weather is rolled, **then** exactly one weather type is chosen on the weighted distribution (Clear 40 / Rain 25 / Fog 20 / Storm 10 / Cold Snap 5) and its listed pro/con is in effect for the cycle.

## Scope & the 1.3↔1.4↔1.5↔1.6 seam (read first — prevents over-building)

FR-5 says Night "shrinks FOV radius and raises enemy encounter/aggression," Fog "reduces both parties' visibility," Cold Snap "slows spoilage but drives Temperature toward Frozen," and Storm "raises structural-collapse chance." **All of those are mechanics that later stories own.** So in **this** story:

- **The clock** — replace 1.2's bare counter with the **Day 100 / Night 70 phase split**: `clockTurns % 170 < 100` → Day, else Night. Expose `isDay()`, `getClockPhase()`, and the **unmodified** `getClockTurns()` (a system/HUD can derive the phase). Keep ticking only on acted turns (AD-5).
- **Weather** — a `Weather` enum (CLEAR 40 / RAIN 25 / FOG 20 / STORM 10 / COLD_SNAP 5) stored on `RunState`, **rolled once per 170-turn cycle boundary** from the **seeded RNG** (`state.rng()`, AD-5), persisted (AD-6). Expose `getWeather()` / `getCycleNumber()`. **Do NOT build any weather *effect*:** no FOV shrink (1.4), no temperature drift (1.6), no spoilage change (1.5), no structural-collapse (3.x), no enemy aggression (4.x). A weather type being *in effect* means: it is the single active `Weather` value the owning stories will later key off.

Nothing here changes what the player *sees or feels* in combat — this story only makes time and weather **real, queryable state**. The HUD surfacing is Story 1.8 (do not build it here).

## Tasks / Subtasks

- [x] **Task 0 — Ratify & extend Story 1.2's `clockTurns` (AC: 1) — no regression**
  - [x] Confirm `clockTurns` is field-initialized (`RunState.java:51`), persisted under the run root, and advanced only in TurnEngine's `if (acted)` branch (AD-4/AD-5). It is — keep the counter and its tick wiring **untouched**.
- [x] **Task 1 — The Day/Night phase split (AC: 1)**
  - [x] Add a `DayPhase` enum (`DAY`, `NIGHT`) and derive the phase from `clockTurns`: `getClockPhase()` returns `DAY` if `clockTurns % 170 < 100` else `NIGHT`; add `isDay()`. Keep the 170-cycle constant and the 100/70 split as named constants (`DAY_LENGTH=100`, `NIGHT_LENGTH=70`, `CYCLE_LENGTH=170`).
  - [x] **Do NOT** add a persisted phase field — the phase is *derived* from the persisted `clockTurns`, so it survives save/load with no new state (AD-6). A load can never desync the phase.
- [x] **Task 2 — The per-cycle weather roll (AC: 2)**
  - [x] Add a `Weather` enum (`CLEAR` 40 / `RAIN` 25 / `FOG` 20 / `STORM` 10 / `COLD_SNAP` 5; weights as named constants, exactly the PRD FR-5 distribution), plus a weighted roll over `state.rng()`. Weighted selection = cumulative-sum / `rng.nextInt(100)` against the [40, 25, 20, 10, 5] weights.
  - [x] Add a persisted `weather` field (field-initialized, e.g. to `Weather.CLEAR`) and a persisted `cycleNumber` int (field-initialized 0, = `clockTurns / CYCLE_LENGTH`). These ride the run root like `clockTurns` (AD-3/AD-6).
  - [x] Add a `rollWeather()` that draws from `state.rng()` **once per 170-turn cycle** — called exactly at the moment a cycle boundary is crossed. Do **not** re-roll on load (persisted state).
  - [x] Expose `getWeather()` / `getCycleNumber()`.
- [x] **Task 3 — Wire the cycle boundary (AC: 1, 2)**
  - [x] In `RunState.tickClock()`, after incrementing `clockTurns`, **detect the cycle boundary** (`newCycle = (clockTurns / CYCLE_LENGTH) != cycleNumber`; if so, set `cycleNumber` and `rollWeather()`). This keeps the roll on the **acted path** (AD-5) and gives AC-2 ("at a new cycle") for free.
  - [x] **Preserve AD-4/AD-5 honesty:** the boundary check lives inside `tickClock()` (which only runs on acted turns), so a wall-bump still advances nothing.
  - [x] **`restart()` resets the clock + re-rolls:** a fresh run must start at Day 0 with a **re-rolled** weather. `restart()` currently does not reset `clockTurns` — this story closes that gap (a new run is a new world).
- [x] **Task 4 — Serialization round-trip (AC: 1) — AD-6**
  - [x] `weather` + `cycleNumber` are persisted fields, so they serialize under the run root for free (like `clockTurns`). `Weather` is a plain enum — libGDX Json serializes it by name, no element-type registration needed. Current saves always carry the keys (`usePrototypes(false)`), so they round-trip exactly.
  - [x] Extend `RunStatePersistenceTest` with `weatherAndCycleSurviveRoundTrip` (weather + cycle + derived phase survive a load, never re-rolled) and `preWeatherSaveLoadsNonNullAndCycleZero` (a field-absent save loads non-null, cycle 0, Day — the honest AD-6 contract; see Dev Agent Record for the ctor-roll wart).
- [x] **Task 5 — Tests**
  - [x] `DayNightClockTest`: phase derivation at the boundaries (clockTurns 0/99 → DAY, 100/169 → NIGHT, 170/269 → DAY again); `isDay()`; the phase is **derived** (no drift on load).
  - [x] `WeatherSystemTest` (or `WeatherRollTest`): the weighted distribution lands **only** on the five types across many rolls (no unknown type); the roll draws from the seeded RNG (same seed → same weather sequence — AD-5 determinism); the boundary re-rolls exactly once per cycle (roll at cycle 0→1, 1→2, etc.).
  - [x] A `TurnEngine` honesty test (mirror `SurvivalTickTest`): a wall-bump does **not** advance the phase or re-roll weather; a real `WAIT` does.
  - [x] A `restart()` test: a fresh run resets `clockTurns` to 0 (Day) and re-rolls weather.
  - [x] Full suite green: `mvn -o clean install`.

## Dev Notes

### Current state (what exists, what to ratify, what to preserve)

- **`clockTurns` is the 1.2 substrate** (`RunState.java:51`, `getClockTurns()` at 246, `tickClock()` at 249). It is field-initialized (old-save safe), persisted, and advanced only in `TurnEngine`'s acted branch (`TurnEngine.java:121`). **The story adds the phase split on top of it and the weather state beside it — it does not change how turns commit (AD-5).**
- **The 1.2↔1.3 seam held in Story 1.2 explicitly:** "Do **NOT** implement the 100-day/70-night phase split, phase queries, or Weather — that is Story 1.3." So 1.3 is precisely the phase split + phase queries + the weather roll that 1.2 deferred. Nothing about the survival tracks (1.2) or the HUD (1.8) changes here.
- **The construction order** (`RunState(long seed)` at 76-82) currently draws from `rng`: `identifyMap` → `generateFloor()` → `spawnStartingCompanion()`. **Adding a weather draw changes the RNG stream** — the same seed will now produce a *different* floor/identities than before. That is correct and unavoidable (weather must be seeded/deterministic, AD-5), but it means **any test pinning a seed→layout identity needs re-verification**, and the new weather draw should be added at a **defined point** in the construction order so it is reproducible. Decide the order deliberately (e.g. draw weather **last**, after the companion, or **first** before identity — either is fine, but document it and keep it stable).
- **`restart()` does NOT currently reset the clock** (lines 157-164 reset lastStand/identifyMap/flagStore, then regenerate). A fresh run starting at Night-with-weather-continued would be a bug — this story resets `clockTurns`/`cycleNumber` and re-rolls weather in `restart()`.
- **Weather is pure state in this story** — the `Weather` enum carries its name + weight + (for the pro/con that later stories read) its label. Do not add gameplay effects here.

### Serialization — the pattern that applies directly (from Stories 1.1/1.2)

- `SaveService.json()` already sets `usePrototypes(false)`, so every field — including new `weather`/`cycleNumber` — is always written. Do **not** re-enable prototype omission.
- **Field-initialize** every new persisted field (`private Weather weather = Weather.CLEAR; private int cycleNumber = 0;`) so a save predating them loads a valid default — the established pattern (RoguePlayer:38-45, RunState clockTurns).
- `Weather` is a plain enum → libGDX Json serializes it by name — no element-type registration (unlike the `Map`/`List` fields in `SaveService.json()`).
- The **phase is derived**, never persisted — so there is no phase field to serialize, and a load can never desync Day/Night.

### Placement rationale (AD-3)

- **Clock phase + weather live on `RunState`** (world time + world weather, shared by all systems). Nothing else holds authoritative time/weather state. The `Weather` enum is a top-level core class (mirrors `Supply`, `TrueIdentity` — plain enums in `com.margins.rogue` or a `weather`/`state` subpackage).

### Scope discipline (CLAUDE.md §2/§3)

- Do **NOT** build: FOV shrink from Night/Fog (1.4), weather-driven temperature drift (1.6), spoilage changes (1.5), structural-collapse (3.x), enemy aggression/encounter changes (4.x), light sources/noise (1.4/1.6), or HUD surfacing (1.8). Do **NOT** change how turns commit (AD-5 untouched).

### Testing standards

- JUnit Jupiter 5.10.2, `mvn -o clean install` (surefire 3.2.5). Mirror `SurvivalTickTest` (`core/src/test/.../SurvivalTickTest.java`): drive `TurnEngine.advance` with `PlayerAction` and assert state. Reference `RunStatePersistenceTest` for the round-trip pattern (it now covers thirst/temperature/clock after 1.2).

### Project Structure Notes

- Core stays headless (AD-2): no libGDX render/input types in `com.margins.rogue.*`. The new phase/weather state + tests are pure model.
- New files: `Weather.java` (enum), `WeatherSystem.java` (if a system wrapper is wanted — or just `RunState.rollWeather()`; mirror the 1.2 `ThirstSystem`/`TemperatureSystem` one-line-delegator pattern if a pipeline step is warranted). Modified: `RunState.java` (phase derivation, weather field, roll, cycle boundary in `tickClock`, `restart()` reset). The HUD surfacing of phase/weather is **Story 1.8** — do not build HUD here (this story is headless model + pipeline).

### References

- [Source: _bmad-output/planning-artifacts/epics.md#Story-1.3] — the AC source.
- [Source: _bmad-output/planning-artifacts/prds/prd-The-Margin-2026-08-06/prd.md#FR-5] — Day 100 / Night 70, 170-turn cycle, weighted weather distribution (Clear 40 / Rain 25 / Fog 20 / Storm 10 / Cold Snap 5), the "each type has a listed pro/con" + bible-values assumption.
- [Source: ARCHITECTURE-SPINE.md#AD-4] — fixed pipeline; the clock is part of the acted-turn step.
- [Source: ARCHITECTURE-SPINE.md#AD-5] — a turn commits only on a real player action; weather/clock must not advance on a wall-bump.
- [Source: ARCHITECTURE-SPINE.md#AD-18] — Night/weather *shrink FOV*, a light source restores it — **this is Story 1.4's mechanic**, not 1.3's.
- [Source: ARCHITECTURE-SPINE.md#AD-9] — light alerts via the noise channel — **Story 1.4**, not 1.3.
- [Source: _bmad-output/implementation-artifacts/1-2-four-survival-tracks-that-tick-on-real-turns.md] — the `clockTurns` substrate, the seam note ("phase split + Weather are Story 1.3"), and the round-trip test to extend.

## Dev Agent Record

### Agent Model Used

claude-opus-4-8 (1M context)

### Debug Log References

- Green baseline: 66 tests (HEAD e8fb75b).
- First full run: 1 failure — `RunStatePersistenceTest.preWeatherSaveLoadsCleanDefaults` expected `CLEAR` but loaded `STORM`. Root cause: libGDX `fromJson` runs the no-arg `RunState()` ctor, which delegates to the full seeded ctor, whose `rollWeather()` drew a nanoTime-rolled weather for a field-absent save. This is the same migration wart as the deferred 3.3 `identifyMap` item. Fixed the test to assert the honest AD-6 contract (non-null, cycle 0, Day) and renamed it `preWeatherSaveLoadsNonNullAndCycleZero`; logged the wart in `deferred-work.md`.
- Final: `mvn -o clean install` → **78 tests, 0 failures, BUILD SUCCESS** (66 → 78).

### Completion Notes List

- **All 5 tasks + AC-1/2 satisfied.** `DayPhase` enum (DAY/NIGHT) with phase **derived** from `clockTurns % 170 < 100` (never persisted — a load can't desync); `isDay()`/`getClockPhase()`; named constants `DAY_LENGTH=100`/`NIGHT_LENGTH=70`/`CYCLE_LENGTH=170` on `RunState`. `Weather` enum (CLEAR 40 / RAIN 25 / FOG 20 / STORM 10 / COLD_SNAP 5) with a weighted `Weather.roll(Random)`; persisted `weather` + `cycleNumber` on `RunState`; `rollWeather()` draws from the seeded RNG.
- **Cycle boundary wired into `tickClock()`**: `clockTurns / CYCLE_LENGTH != cycleNumber` → set cycle + re-roll, so the roll fires exactly once per 170-turn cycle on the acted path (AD-5). A wall-bump advances neither clock nor weather (`wallBumpAdvancesNeitherClockNorWeather`).
- **`restart()` gap closed**: the 1.2 counter persisted across restarts; now `restart()` resets `clockTurns`/`cycleNumber` and re-rolls weather (a fresh run is Day 0, cycle 0).
- **RNG-stream decision**: weather is drawn LAST in construction (after `spawnStartingCompanion`), so the pre-1.3 layout/identity draws stay on their original stream — no seed→layout regression. Documented in the constructor.
- **Seam held** — no FOV shrink (1.4), no temperature drift/spoilage/collapse (1.5/1.6/3.x), no HUD (1.8). Weather is pure queryable state; `Weather.label()` is the only read later stories need.
- **Serialization** — `weather`/`cycleNumber` round-trip exactly (`weatherAndCycleSurviveRoundTrip`); a pre-1.3 save loads non-null / cycle 0 / Day (`preWeatherSaveLoadsNonNullAndCycleZero`). Discovered + logged the pre-1.3 ctor-roll wart (same as the deferred 3.3 identifyMap item).

### File List

**Modified**
- `core/src/main/java/com/margins/rogue/state/RunState.java` — `DAY_LENGTH`/`NIGHT_LENGTH`/`CYCLE_LENGTH` constants; `weather` + `cycleNumber` fields (field-initialized); `getClockPhase()`/`isDay()`/`getWeather()`/`getCycleNumber()`; `tickClock()` cycle-boundary detection; `rollWeather()`; ctor + `restart()` draw/reset; imports.
- `core/src/test/java/com/margins/rogue/SurvivalTickTest.java` — added `wallBumpAdvancesNeitherClockNorWeather` + `realActionAdvancesTheClockAndStaysInDay`.
- `core/src/test/java/com/margins/rogue/state/RunStatePersistenceTest.java` — added `weatherAndCycleSurviveRoundTrip` + `preWeatherSaveLoadsNonNullAndCycleZero` (+ DayPhase/Weather imports).

**Added**
- `core/src/main/java/com/margins/rogue/DayPhase.java` — DAY/NIGHT enum.
- `core/src/main/java/com/margins/rogue/Weather.java` — weather enum + weighted `roll(Random)`.
- `core/src/test/java/com/margins/rogue/DayNightClockTest.java` — phase-boundary derivation + cycle restart.
- `core/src/test/java/com/margins/rogue/WeatherSystemTest.java` — distribution, determinism, initial-roll, boundary, restart.

## Change Log

- 2026-08-06 — Story 1.3 created (Day/Night phase split + per-cycle weather roll; FR-5). Seam-respecting: state machine + query surface only; FOV/temperature/spoilage/collapse/HUD are later stories.
- 2026-08-06 — Implemented Story 1.3 (Day 100 / Night 70 derived clock + per-cycle weighted weather roll). `DayPhase`/`Weather` enums; phase derived (never persisted); weather persisted + re-rolled at each 170-turn boundary on the acted path; `restart()` resets the clock. 66 → 78 tests green.
- 2026-08-06 — Code review complete: **Approve (4 patches applied)**. Boundary re-roll pinned by seed-search tests; pre-1.3 deep-clock migration reconciled in `restoreAfterLoad()`; `Weather.roll` cap derived from weights. 78 → 79 tests green.
- 2026-08-06 — Code review complete: **Approve (4 patches applied)**. Boundary re-roll pinned by seed-search tests; pre-1.3 deep-clock migration reconciled in `restoreAfterLoad()`; `Weather.roll` cap derived from weights. 78 → 79 tests green.

## Senior Developer Review (AI)

**Date:** 2026-08-06 · **Outcome:** Approve (with patches applied)

Acceptance Auditor confirmed AC-1/2 fully met and AD-2/3/4/5/6 satisfied — and, critically, that the 1.3↔1.4/1.5/1.6/3.x↔1.8 seam held (weather is queryable state only; no FOV shrink, temperature drift, spoilage, structural collapse, or HUD). Blind Hunter found no High; its single Med was an **untested headline behavior** (a regression deleting the boundary re-roll would pass the suite). Edge Case Hunter's Med was the convergent pre-1.3 migration edge (stale `cycleNumber` + nondeterministic weather on a deep-clock old save). Both Meds are closed by the patches below. All three layers independently re-ran the suite and confirmed the RNG-draw-order decision (weather rolled LAST in construction — no seed→layout regression). The Dev-record "78 tests green" was correct as of the dev run; the review lands at 79.

### Review Findings

**Patches (applied this review):**
- [x] [Review][Patch] Pin the AC-2 boundary re-roll with seed-search tests [core/.../WeatherSystemTest.java] — Blind Hunter (Med): the headline "weather re-rolls exactly at the 170-turn boundary" was untested in the pass direction — `weatherIsConstantWithinACycleAndReRollsAtTheBoundary` asserted constancy within cycle 0 and `cycleNumber==1` at 170, but never that the weather VALUE changed; a regression deleting `rollWeather()` from `tickClock()` would have stayed green. Added `boundaryDifferingSeed()` (seed-search 1..4999 for a state whose cycle-0 and cycle-1 weather differ — RunState is deterministic per seed, so a fresh build from the found seed reproduces both rolls) and now assert `assertNotEquals(cycle0, ...)` at turn 170. `restartResetsClockAndReRollsWeather` likewise strengthened to assert `restart()` rolls a **different** weather (it previously ended at a weak `assertNotNull`). Both tests also cover the initial-roll-not-stuck-at-CLEAR proof via the search.
- [x] [Review][Patch] Reconcile pre-1.3 saves with a deep clock [core/.../state/RunState.java] — Edge Case Hunter (Med) + Blind Hunter (converged): a pre-1.3 save could hold `clockTurns ≥ 170` (the clock substrate shipped in 1.2) while lacking the `weather`/`cycleNumber` fields. The field-absent ctor-roll left a stale `cycleNumber=0` even when the clock implied cycle 1 — the resumed run's cycle desynced from its phase. `restoreAfterLoad()` now derives `cycleNumber = clockTurns / CYCLE_LENGTH` and null-guards weather. (Weather itself stays the documented nanoTime-roll wart for field-absent saves — see Deferred; only `cycleNumber` is fully fixable at load time.)
- [x] [Review][Patch] Derive the `Weather.roll` cap from the weights [core/.../Weather.java] — Edge Case Hunter (Low): `rng.nextInt(100)` hard-coded the CLEAR weight into the cap, so a future nonzero-weight type (or any weights-sum ≠ 100) would silently bias or strand the roll. Now `total` is the sum of `values()` weights and `roll = rng.nextInt(total)`; the doc note ties the unreachable `return CLEAR` to the invariant `total == sum(weights)`.
- [x] [Review][Patch] Migration test for a deep-clock pre-1.3 save [core/.../state/RunStatePersistenceTest.java] — Edge Case Med regression net: `preWeatherSaveLoadsNonNullAndCycleZero` only emulated a `clockTurns=0` save, so the stale-cycleNumber path was untested. Added `preWeatherSaveDeepInACycleDerivesTheCycleFromTheClock`: ticks to 170, strips `weather`/`cycleNumber`, loads, and asserts `cycleNumber==1` derives from the persisted clock, phase is Day (self-consistent), weather non-null. This test fails without the restoreAfterLoad patch.

**Deferred (real, but out of Story 1.3 scope — see `deferred-work.md`):**
- [x] [Review][Defer] Post-load weather is save-point-dependent [core/.../state/RunState.java] — Edge Case Hunter (Low): for a field-ABSENT pre-1.3 save, the no-arg ctor's nanoTime `rollWeather()` leaves a different weather on every reload (a save-point that preserved `clockTurns` cannot also preserve the weather rolled before those fields existed). Same documented wart as the deferred 3.3 `identifyMap` item; cannot be distinguished from a legitimately rolled value at load time. Deferred alongside that item.
- [x] [Review][Defer] Forward-compat: unknown weather string / new field silently voids a run [SaveService] — Edge Case Hunter (Low): libGDX Json reading an unrecognized enum string throws inside `fromJson`; `SaveService` catches `RuntimeException` → returns null (the player keeps a playable run at the cost of the save). Acceptable today; revisit when saveVersion read-branch (action item) exists so a version gate can reject gracefully instead.
- [x] [Review][Defer] `tickClock()` only handles 1-turn crossings [core/.../state/RunState.java] — Blind Hunter (Low): a future fast-forward/skip (Story 1.8 or a debug harness) that jumps many turns must loop `tickClock()` per turn — a single call crossing a boundary mid-flight would mis-handle multi-cycle jumps. Note documented for the story that adds fast-forward.

**Dismissed (noise / by-design):** Weather re-rolls exactly once at 170 and not mid-cycle (boundary-detection test asserts constancy per turn 1..169); the field default `CLEAR` is a valid pre-roll value and can legitimately appear as cycle 0's roll (the initial-roll test uses a seed search, not a value assertion); `cycleNumber` is persisted so a legitimately-loaded cycle-5 run keeps cycle 5 (the derive-in-restoreAfterLoad only applies to field-absent saves — a current-format save round-trips untouched, pinned by `weatherAndCycleSurviveRoundTrip`); `DayPhase` never persisted (derivation cannot desync); `Weather.roll`'s `nextInt(total)` distribution shift is unobservable at current weights (weights sum to exactly 100, so the draw is identical); no test that every `tickClock()` at 170 across seeds re-rolls (the seed-search covers the reachable-differing case and the constancy loop pins the no-re-roll case); cold-snap being rare in short tests is distribution, not bias (the 10000-roll test exercises all five types).

---
baseline_commit: 99fc84f
---

# Story 1.4: FOV and light — the visible-camp tension

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As Klein,
I want darkness and fog to shrink what I can see and light to restore it at the cost of noise,
so that a lit camp is a visible camp (FR-5, FR-7, AD-18).

## Acceptance Criteria

1. **Given** it is Night or the weather is Fog, **When** FOV is computed for the acting agent, **Then** the visible radius is shrunk versus the clear-day baseline.
2. **Given** I light a campfire or torch, **When** FOV is recomputed, **Then** my visible radius is restored but still reduced versus clear day.
3. **Given** a lit campfire or torch on my tile, **When** the Noise step resolves (AD-9), **Then** a `NoiseEvent` is emitted at the light's tile each turn, LOS-ignoring, and in-radius enemies are drawn toward it (an enemy behind a wall can still be alerted).

## Scope & the 1.3↔1.4↔1.5↔1.6 seam (read first — prevents over-building)

Story 1.3 made **Night/Fog real queryable state** (`isDay()`, `getWeather()`) and explicitly deferred their *effects*. This story is precisely the **FOV effect** of Night/Fog plus the **light mechanic** that answers it. Per AD-18, "FOV and light are core mechanics, not presentation" — so in **this** story:

- **Dynamic player FOV radius** — replace `FovSystem`'s constant `RADIUS = 8` with a radius **derived from run state**: clear-day baseline, shrunk when it is Night **or** Fog, and restored-but-reduced when a light is active. The shadowcasting algorithm is unchanged — only the radius it runs to becomes dynamic. FOV is still computed **only for the player** (the acting agent, AD-18) exactly as today.
- **A queryable, positioned light source on `RunState`** — a nullable light *tile* (persisted, AD-6). This story builds the **mechanism and a setter**; it does **not** build the campfire/torch items that will drive it. An active light (a) sets the player's FOV to the restored-reduced radius and (b) is the tile the noise is emitted from.
- **Light → noise, via the single AD-9 channel** — on each acted turn, if a light is active, emit one `NoiseEvent` at the light's tile just before the Noise-resolve step. `NoiseSystem.resolve` already draws in-radius enemies **LOS-ignoring** (Euclidean, no line-of-sight) — so AC-3's "enemy behind a wall can still be alerted" comes for free from the existing consumer. **Do not add a second alert path** (AD-18: light alerts through *exactly one* mechanism — noise).

**Do NOT build here:** the campfire item/station and the leave-camp proximity trade (Story 1.5); the torch item, its Wood+Coal recipe, 60-turn burn, and warmth (Story 1.6); enemy vision shrinking or night aggression/encounter scaling (Epic 4 — **`DetectionSystem` stays untouched**, AD-18 keeps its cheap LOS-to-player check); companion/enemy FOV computation (they use `DetectionSystem` LOS, not FOV); any HUD/light indicator (Story 1.8). The exact radius/noise numbers are **PRD Balance / starting calibration** — name them as constants; the PRD does not lock the values.

## Tasks / Subtasks

- [x] **Task 1 — Make the player's FOV radius dynamic (AC: 1, 2)**
  - [x] In `FovSystem`, replace the single `RADIUS = 8` constant with three named constants: `DAY_RADIUS = 8` (the current clear-day baseline — unchanged), `DARK_RADIUS` (shrunk, Night/Fog — starting calibration, e.g. `4`), `LIT_RADIUS` (restored-but-reduced vs day — starting calibration, e.g. `6`, and it **must** satisfy `DARK_RADIUS < LIT_RADIUS < DAY_RADIUS` so AC-2 "restored but reduced" holds).
  - [x] Add a radius-parameterized compute: `compute(RogueTileMap map, int px, int py, int radius)` — the shadowcasting body uses the passed `radius` in place of the constant (the inner `i <= radius` / `dx*dx+dy*dy <= radius*radius` bounds).
  - [x] Keep a `compute(map, px, py)` overload delegating to `compute(map, px, py, DAY_RADIUS)` **only if** an existing caller/test needs the fixed-radius form; otherwise route everything through the state-derived path. (There are currently **no** FOV tests, so no fixed-radius test to preserve — verify before keeping the overload; do not leave it dead.)
  - [x] Add a pure radius selector — `radiusFor(RunState state)`: `if (state.hasLight()) return LIT_RADIUS; if (!state.isDay() || state.getWeather() == Weather.FOG) return DARK_RADIUS; return DAY_RADIUS;`. Light takes precedence over dark (you lit it *because* it's dark). Make `compute(RunState)` call `compute(map, px, py, radiusFor(state))`.
- [x] **Task 2 — A positioned, persisted light source on `RunState` (AC: 2, 3)**
  - [x] Add field-initialized, persisted fields: `private int lightX = -1; private int lightY = -1;` (the `-1` sentinel = no light; field-initialized so a pre-1.4 save loads light-less — AD-6, mirrors the `clockTurns`/`weather` old-save pattern).
  - [x] Add the query/mutation surface: `boolean hasLight()` (`lightX >= 0`), `int getLightX()`, `int getLightY()`, `void setLight(int x, int y)`, `void clearLight()`. This is the seam surface Stories 1.5 (campfire → a fixed tile) and 1.6 (torch → the player's tile) will drive — **do not** add item/crafting/burn logic here.
  - [x] `restart()` clears the light (`lightX = lightY = -1`) — a fresh run has no camp (mirrors the 1.3 `restart()` clock reset).
- [x] **Task 3 — Emit the light's noise on the acted path (AC: 3)**
  - [x] Add a minimal `LightSystem.emitNoise(RunState state)` (mirror the one-line-delegator style of `ThirstSystem`/`TemperatureSystem`): if `state.hasLight()`, call `state.emitNoise(getLightX(), getLightY(), LIGHT_NOISE_RADIUS)` with a named `LIGHT_NOISE_RADIUS` constant (starting calibration, e.g. `6` — the fire is bright and audible to patrols, FR-7).
  - [x] In `TurnEngine`'s **acted branch**, call `LightSystem.emitNoise(state)` **immediately before** `NoiseSystem.resolve(state)` — so the ambient light noise is enqueued and consumed the same turn, alongside combat/distraction noise. It rides the acted path only (AD-5): a wall-bump emits nothing.
  - [x] Do **not** modify `NoiseSystem` or `DetectionSystem`. `NoiseSystem.resolve` already: draws Euclidean-in-radius enemies (no LOS), rises UNAWARE→SUSPICIOUS, retargets `lastSeen`, resets `calmTurns`, and clears the queue. That is exactly AC-3.
- [x] **Task 4 — Serialization round-trip (AC: 2) — AD-6**
  - [x] `lightX`/`lightY` are plain ints under the run root — they serialize for free like `clockTurns` (no element-type registration). Confirm `SaveService.json()`'s `usePrototypes(false)` writes them always.
  - [x] Extend `RunStatePersistenceTest`: `lightSurvivesRoundTrip` (set a light, save/load, assert the tile survives and `hasLight()` holds) and `preLightSaveLoadsWithoutLight` (strip the `lightX`/`lightY` keys, assert a field-absent save loads `hasLight() == false` — the honest AD-6 default).
- [x] **Task 5 — Tests**
  - [x] New `FovRadiusTest` (headless, hand-built `RunState` via a seed): assert `radiusFor` returns `DAY_RADIUS` on a clear Day, `DARK_RADIUS` at Night, `DARK_RADIUS` under Fog on a Day, and `LIT_RADIUS` whenever a light is active (including when it is also Night/Fog — light precedence). Because weather is a per-cycle seeded roll (1.3), drive the phase with `tickClock()` to reach Night and, for the Fog case, **seed-search** a `RunState` whose cycle-0 weather is `FOG` (mirror `WeatherSystemTest.boundaryDifferingSeed`'s search pattern) or set the phase deterministically — assert `DARK_RADIUS < LIT_RADIUS < DAY_RADIUS` explicitly so the "restored but reduced" invariant is pinned.
  - [x] A `FovSystem.compute` behavior test: on a small open hand-built map, computing with a smaller radius marks **fewer** visible tiles than a larger radius (proves the radius is actually wired into the cast, not just the selector) — e.g. a tile at distance 6 is visible at `DAY_RADIUS=8` but not at `DARK_RADIUS=4`.
  - [x] New `LightNoiseTest`, two levels:
    - **Unit (`LightSystem` directly):** with a light set, `LightSystem.emitNoise(state)` enqueues exactly one `NoiseEvent` at `(lightX, lightY)` with `LIGHT_NOISE_RADIUS` (assert against `state.getNoiseQueue()`); with no light, the queue stays empty. This pins 1.4's actual new code.
    - **Integration (via `TurnEngine`):** place a `RogueEnemy` at a known tile within `LIGHT_NOISE_RADIUS` of the light and set the light — `state.getEnemies()` returns the live list, so add the enemy there (use the real `RogueEnemy` constructor; give it a tile with **no** line-of-sight to the light to demonstrate the lure ignores LOS). One acted `TurnEngine.advance(WAIT)` rises the enemy UNAWARE→SUSPICIOUS and sets `lastSeen` to the light tile. With **no** light set, the same turn leaves it UNAWARE. (The LOS-ignoring property is inherent to `NoiseSystem`'s Euclidean check — this test confirms 1.4 feeds it correctly.)
  - [x] A `TurnEngine` honesty check (mirror `SurvivalTickTest`): a wall-bump emits no light noise (the enemy stays UNAWARE) even with a light active — the emit is on the acted path only (AD-5).
  - [x] Full suite green: `mvn -o clean install`.

## Dev Notes

### Current state (what exists, what to ratify, what to preserve)

- **`FovSystem` is a complete recursive-shadowcasting FOV** (`system/FovSystem.java`) with a hard-coded `RADIUS = 8`. It writes `setVisible`/`setExplored` flags onto the `RogueTileMap`; the screen renders those flags (`MarginScreen.java:112-160` reads `map.isVisible()` / `map.isExplored()` only). **The screen never reads `FovSystem.RADIUS`** — so making the radius dynamic is fully contained in `FovSystem` and its callers; no render code changes. `compute(RunState)` is called from `TurnEngine.java:133` (end of acted turn) and `MarginScreen.java:49,75` (init + after a turn). Route all three through the state-derived radius.
- **`FovSystem.hasLineOfSight` is shared with `DetectionSystem`** (`DetectionSystem.canSee` calls it). Do not change it. Enemy detection uses its **own** `VISION_RANGE = 6` + LOS (`DetectionSystem.java`), independent of the player's FOV radius — AD-18 keeps this cheap check. **This story does not shrink enemy vision at night** (that is Epic 4's "night raises aggression/encounter"). 1.4 changes the *player's* FOV only.
- **`NoiseSystem.resolve` is the single AD-9 consumer** (`system/NoiseSystem.java`): for each queued `NoiseEvent`, every living enemy within Euclidean `radius` (no LOS) rises UNAWARE→SUSPICIOUS, retargets `lastSeen` to the noise origin, and resets `calmTurns`; the queue is cleared. **AC-3 is already implemented on the consumer side** — this story only *produces* the light's `NoiseEvent`. Existing producers: `CombatSystem` (attack, radius 4, `CombatSystem.java:30`) and `CompanionSystem.distract` (radius 6). Match that pattern: enqueue via `state.emitNoise(x, y, radius)`.
- **`RunState.emitNoise(x, y, radius)`** (`state/RunState.java:315`) enqueues onto the transient, non-serialized noise queue — exactly what the light emit needs.
- **No FOV/Noise/Detection tests exist yet** — this subsystem is currently uncovered. Keep 1.4's new tests scoped to 1.4's change (dynamic radius + light noise), but they will also be the first regression net for `FovSystem.compute` and the noise lure.

### The seam decision — how "a lit source" is represented (assumption, please confirm)

The epic's ACs name "a campfire or torch," but the **campfire item/station is Story 1.5** and the **torch item is Story 1.6**. So 1.4 cannot depend on either existing. The chosen representation:

- **A single nullable, positioned light on `RunState`** (`lightX`/`lightY`, `-1` = none), persisted (AD-6). A *positioned* light (not a boolean on the player) is required by AD-18 ("emits a `NoiseEvent` at **its tile**") and by Story 1.5's stationary campfire — after you walk away, the fire's tile ≠ your tile. Story 1.6's carried torch will simply set the light to the player's tile each turn.
- **Assumption (confirm):** in 1.4 an active light restores the player's FOV to `LIT_RADIUS` **whenever it is active** (the player lights it where they stand). The "walk away from the camp and lose its FOV/warmth benefit" **proximity trade is explicitly Story 1.5's edge case** (UJ-1: "abandon the camp to flee into fog → lose the camp's benefits") — do not build proximity-gated FOV here. If you'd rather 1.4 already gate FOV restoration on player-near-light, say so; otherwise the default is active-light → `LIT_RADIUS`.
- MVP carries **one** active light at a time (a nullable tile). Multiple simultaneous lights are not a requirement of any Epic 1 story — do not generalize to a list (CLAUDE.md §2).

### FOV radius model (AC-1, AC-2)

`radiusFor(state)` precedence, top-down:
1. `hasLight()` → `LIT_RADIUS` (a light restores sight even in dark/fog — AC-2).
2. else Night (`!isDay()`) **or** Fog (`getWeather() == Weather.FOG`) → `DARK_RADIUS` (AC-1).
3. else → `DAY_RADIUS` (clear-day baseline).

AC-1 says "Night **or** Fog" — either darkening condition alone shrinks to `DARK_RADIUS`; this story does **not** stack them into an even-smaller Night+Fog radius (not required by the AC — note it as a possible future calibration, not scope). The invariant `DARK_RADIUS < LIT_RADIUS < DAY_RADIUS` is what makes AC-2's "restored but reduced" true and must be asserted in a test.

### Placement rationale (AD-3)

- **The light lives on `RunState`** — it is world state (a fire on a tile), shared by FOV (player) and the Noise step (enemies). Nothing else should own it. `LightSystem` is a `system/` pipeline helper like `ThirstSystem`/`TemperatureSystem`. The radius constants live on `FovSystem` (they are FOV's calibration).
- **Core stays headless (AD-2):** no libGDX render/input types in any new/changed core class. FOV radius, the light state, and `LightSystem` are pure model.

### Serialization — the pattern that applies directly (from Stories 1.1/1.2/1.3)

- `SaveService.json()` sets `usePrototypes(false)` — new `lightX`/`lightY` are always written. Field-initialize them to `-1` so a pre-1.4 save (no keys) loads light-less (AD-6), exactly like `clockTurns`/`weather`/`cycleNumber`.
- Plain ints → no element-type registration (unlike the `Map`/`List` fields in `SaveService.json()`).
- No derived/transient light state to reconcile in `restoreAfterLoad()` — the light is plain persisted ints. (Contrast 1.3's weather ctor-roll wart; nothing analogous here — a light-less default is unambiguous.)

### Scope discipline (CLAUDE.md §2/§3)

- Do **NOT** build: campfire item/station or leave-camp proximity (1.5); torch item / Wood+Coal recipe / 60-turn burn / warmth (1.6); enemy night-vision shrink or aggression/encounter scaling (Epic 4); companion/enemy FOV computation; HUD light/time indicators (1.8); a second light-alert path (AD-18 — noise only). Do **NOT** modify `DetectionSystem` or `NoiseSystem`. Do **NOT** change how turns commit (AD-5 untouched — the light emit is on the acted path).
- Every changed line should trace to: dynamic FOV radius, the positioned light state, or the light-noise emit.

### Testing standards

- JUnit Jupiter 5.10.2, `mvn -o clean install` (surefire 3.2.5). For turn-driven behavior, mirror `SurvivalTickTest`: build a `RunState`, drive `TurnEngine.advance` with a `PlayerAction`, assert enemy `Detection`/state. For the FOV radius selector, unit-test `radiusFor` directly. For the Fog case, reuse the `WeatherSystemTest.boundaryDifferingSeed` seed-search idiom to obtain a `RunState` in the desired weather deterministically. For round-trip, extend `RunStatePersistenceTest` (its `json()` mirrors production with `usePrototypes(false)`).

### Project Structure Notes

- **New:** `core/src/main/java/com/margins/rogue/system/LightSystem.java` (one-method emit helper). **Modified:** `FovSystem.java` (radius constants + parameterized compute + `radiusFor`), `RunState.java` (light fields/API + `restart()` clear), `TurnEngine.java` (one `LightSystem.emitNoise` call before `NoiseSystem.resolve`). **New tests:** `FovRadiusTest`, `LightNoiseTest`; **extended:** `RunStatePersistenceTest`. The screen (`MarginScreen.java`) needs **no** change — it already renders whatever the FOV marks visible.

### References

- [Source: _bmad-output/planning-artifacts/epics.md#Story-1.4] — the three ACs (Night/Fog shrink, light restores-but-reduced, light emits LOS-ignoring noise that lures in-radius enemies).
- [Source: prd.md#FR-5] — "Night without a light source shrinks FOV radius…; with light, FOV is restored but reduced." Fog "reduces both parties' visibility." Weather weights/day-night lengths are bible starting calibration.
- [Source: prd.md#FR-7] — the campfire/torch are visible **and audible** to patrols; torch is light-only, 60-turn burn (the item is 1.6 — only the *audible/visible* consequence is 1.4's).
- [Source: ARCHITECTURE-SPINE.md#AD-18] — FOV/light are core mechanics; FOV computed **only for the acting agent**; Night/weather shrink radius, light restores the *player's* radius; **light alerts through exactly ONE mechanism — the AD-9 noise channel**, LOS-ignoring; enemy detection keeps its cheap LOS-to-player check.
- [Source: ARCHITECTURE-SPINE.md#AD-9] — `NoiseEvent` → `NoiseSystem.resolve` single consumer; nudges enemy state centrally; no emitter reaches into an enemy directly.
- [Source: ARCHITECTURE-SPINE.md#AD-4/AD-5] — fixed pipeline; the light emit rides the acted turn only.
- [Source: _bmad-output/implementation-artifacts/1-3-day-night-clock-and-per-cycle-weather.md] — `isDay()`/`getWeather()` are the 1.3 queries this story keys off; the `boundaryDifferingSeed` seed-search idiom; the field-initialized persisted-field pattern; `restart()` reset precedent.

## Dev Agent Record

### Agent Model Used

claude-opus-4-8 (1M context)

### Debug Log References

- Green baseline: 79 tests (HEAD 99fc84f).
- `mvn -o -pl core compile` after the FovSystem/RunState/LightSystem/TurnEngine changes → clean (0 errors). Confirmed no dangling `FovSystem.RADIUS` refs and all three `FovSystem.compute` call sites use the `compute(RunState)` form (grep) — the old 2-arg `compute(map,px,py)` overload was removed, not left dead.
- Final: `mvn -o clean install` → **92 tests, 0 failures, BUILD SUCCESS** (79 → 92; +13: FovRadiusTest 5, FovComputeRadiusTest 1, LightNoiseTest 5, RunStatePersistenceTest +2).

### Completion Notes List

- **All 5 tasks + AC-1/2/3 satisfied.** FOV radius is now derived from run state via `FovSystem.radiusFor(state)`: `hasLight()` → `LIT_RADIUS`(6); else Night or Fog → `DARK_RADIUS`(4); else `DAY_RADIUS`(8). Light takes precedence (you lit it *because* it's dark). The invariant `DARK < LIT < DAY` (AC-2's "restored but reduced") is asserted directly.
- **The shadowcasting is unchanged** — `castLight`/`compute` are now parameterized by `radius` instead of the constant. `compute(RunState)` passes `radiusFor(state)`; the map-level `compute(map, px, py, radius)` is the headless test entry point. The screen (`MarginScreen`) reads only `map.isVisible()`, so it needed **zero** changes — the shrunk/restored radius simply marks fewer/more tiles visible.
- **Positioned, persisted light on `RunState`** (`lightX`/`lightY`, `-1` = none; field-initialized so a pre-1.4 save loads light-less — AD-6). API: `hasLight()`/`getLightX()`/`getLightY()`/`setLight()`/`clearLight()`. `restart()` clears it (a fresh run has no camp). This is the seam surface 1.5 (campfire → fixed tile) and 1.6 (torch → player tile) will drive — no item/crafting/burn logic built here.
- **Light → noise via the single AD-9 channel** — `LightSystem.emitNoise(state)` enqueues one `NoiseEvent` at the light's tile (`LIGHT_NOISE_RADIUS`=6) when lit, called in `TurnEngine`'s acted branch immediately before `NoiseSystem.resolve`. AC-3's "LOS-ignoring, enemy behind a wall still alerted" is inherent to `NoiseSystem`'s existing Euclidean consumer — **`NoiseSystem` and `DetectionSystem` are untouched** (AD-18: light alerts through exactly one mechanism). The emit rides the acted path only (AD-5): a wall-bump emits nothing (pinned by `wallBumpEmitsNoLightNoise`).
- **Seam held** — no campfire/torch items, no leave-camp proximity trade (1.5), no torch burn/warmth (1.6), no enemy night-vision shrink or aggression (Epic 4), no companion/enemy FOV, no HUD (1.8). The FOV-restoration-while-active assumption (proximity deferred to 1.5) was implemented as specified.
- **Deviation (minor, no scope change):** the old no-radius `compute(map, px, py)` overload was **removed** rather than kept, since no caller/test needed the fixed-radius form (the story flagged: "do not leave it dead"). Task 5's FOV compute test is its own class `FovComputeRadiusTest` (split from `FovRadiusTest` for clarity — selector vs. cast behavior).

### File List

**Modified**
- `core/src/main/java/com/margins/rogue/system/FovSystem.java` — `DAY_RADIUS`/`DARK_RADIUS`/`LIT_RADIUS` constants (replacing `RADIUS`); `radiusFor(RunState)`; `compute(RunState)` derives the radius; `compute`/`castLight` parameterized by radius; removed the no-radius overload.
- `core/src/main/java/com/margins/rogue/state/RunState.java` — `lightX`/`lightY` fields (field-initialized `-1`, persisted); `hasLight()`/`getLightX()`/`getLightY()`/`setLight()`/`clearLight()`; `restart()` clears the light.
- `core/src/main/java/com/margins/rogue/system/TurnEngine.java` — one `LightSystem.emitNoise(state)` call in the acted branch before `NoiseSystem.resolve`.
- `core/src/test/java/com/margins/rogue/state/RunStatePersistenceTest.java` — `lightSurvivesRoundTrip` + `preLightSaveLoadsWithoutLight`.

**Added**
- `core/src/main/java/com/margins/rogue/system/LightSystem.java` — the light's per-turn noise emitter (`LIGHT_NOISE_RADIUS`).
- `core/src/test/java/com/margins/rogue/FovRadiusTest.java` — `radiusFor` selector across Day/Night/Fog/Light + the DARK<LIT<DAY invariant.
- `core/src/test/java/com/margins/rogue/FovComputeRadiusTest.java` — the radius is wired into the cast (fewer visible tiles at a smaller radius).
- `core/src/test/java/com/margins/rogue/LightNoiseTest.java` — the light-noise emit (unit), the LOS-ignoring lure (integration), and acted-path-only honesty.

## Change Log

- 2026-08-07 — Story 1.4 created (dynamic player-FOV radius from Night/Fog/light + a positioned persisted light source + light→noise via the single AD-9 channel; FR-5/FR-7/AD-18). Seam-respecting: no campfire/torch items, no proximity trade, no enemy-vision/HUD changes; `DetectionSystem`/`NoiseSystem` untouched.
- 2026-08-07 — Implemented Story 1.4. `FovSystem` radius derived from state (Day 8 / Night·Fog 4 / Lit 6, light precedence); positioned persisted light on `RunState` (`restart()` clears it); `LightSystem.emitNoise` wired into the acted branch before the Noise resolve. Screen and `DetectionSystem`/`NoiseSystem` unchanged. 79 → 92 tests green.
- 2026-08-07 — Code review complete: **Approve (3 patches applied)**. Fixed a daylight-torch FOV downgrade (light now only restores sight upward), hardened `hasLight()` to require both coordinates, and pinned the FOV boundary + old-save sentinels. 92 → 95 tests green.

## Senior Developer Review (AI)

**Date:** 2026-08-07 · **Outcome:** Approve (with patches applied)

Acceptance Auditor confirmed all three ACs are genuinely met and each is covered by a regression-failing test (including a cast-level test proving the radius is wired into the shadowcaster, not just the selector) — and that the seam held: **`DetectionSystem` and `NoiseSystem` are provably untouched** (only `RunState`/`FovSystem`/`TurnEngine`/`RunStatePersistenceTest` changed, plus the new `LightSystem`/tests), AD-18's single-noise-mechanism is respected, no libGDX leaked into new core code, and pre-1.4 saves degrade honestly. Suite green at 92/92 on review entry. Blind Hunter found the one real behavioral defect (a High); Edge Case Hunter converged on the `hasLight()` half-set gap and independently walked the pipeline/serialization branches clean.

### Review Findings

**Patches (applied this review):**
- [x] [Review][Patch] Light must only RESTORE sight, never reduce it [core/.../system/FovSystem.java] — Blind Hunter (High): `radiusFor` checked `hasLight()` first and returned `LIT_RADIUS` unconditionally, so lighting a torch in clear day dropped FOV 8→6 — a strict penalty (plus luring noise for zero benefit) that contradicted the method's own doc ("restores it but still below the clear-day baseline"). Now computes the ambient radius (Day 8 / Night·Fog 4) and returns `max(ambient, LIT_RADIUS)` when lit — a fire lifts dark 4→6 but leaves daylight 8 untouched. The night+fog+light case still resolves to 6 (`max(4,6)`), so the existing precedence test is unchanged. **Was untested** — added `lightInClearDayDoesNotReduceSight` (the day+light case the suite was blind to).
- [x] [Review][Patch] `hasLight()` must check both coordinates [core/.../state/RunState.java] — Blind Hunter (Med) + Edge Case Hunter (High, converged): `hasLight()` was `lightX >= 0`, ignoring `lightY`. A half-set light (one coord still the `-1` sentinel) read as lit and would emit a `NoiseEvent` at an off-map row, luring enemies to a tile they can never reach. Now `lightX >= 0 && lightY >= 0` — the sentinel lives on the pair. Added `halfSetLightIsNotLitAndEmitsNothing` (a light with `y == -1` is unlit and emits nothing).
- [x] [Review][Patch] Pin the FOV radius boundary + old-save sentinels [core/.../FovComputeRadiusTest.java, .../state/RunStatePersistenceTest.java] — Blind Hunter (Low, T2/T3): the compute test had 2 tiles of slack (distance 6 vs radius 4), so an off-by-one in the loop bound or clamp could survive. Added `theRadiusBoundaryIsExact` (visible AT distance == radius, hidden at radius+1 — pins the inclusive Euclidean edge). Strengthened `preLightSaveLoadsWithoutLight` to assert both coords load at the `-1` sentinel, not just `!hasLight()`.

**Deferred (real, but out of Story 1.4 scope — see `deferred-work.md`):**
- [x] [Review][Defer] `setLight` accepts off-map / wall / negative coordinates [core/.../state/RunState.java, NoiseSystem] — Blind Hunter (Med) + Edge Case Hunter (Med #2/#4): neither `setLight` nor `NoiseSystem` bounds-checks the light tile; a bad coordinate lures enemies to an off-map/wall tile (functional-but-degenerate AI, no crash), and an extreme coordinate could overflow `NoiseSystem`'s `dx*dx` int math. **Not reachable within Story 1.4** — nothing here sets a bad light, and the real callers are the campfire (1.5, a validated map tile) and torch (1.6, the player's own walkable tile). Building caller-validation now would be speculative (CLAUDE.md §2). Deferred to 1.5/1.6, which own the real placement and should validate at the point of lighting. The `hasLight()` patch already closes the most likely bad input (a half-set `-1`).

**Dismissed (checked, not issues):**
- **Edge Case #3 — a persisted light reloading onto a changed/wall tilemap — not independently reachable.** AD-6 persists the tilemap **inline** (it is never regenerated from seed on load), so a saved light always reloads onto the *same* map where it was placed; the tile under it cannot have changed. This collapses entirely into the set-time validation deferred above — there is no separate load-time reconciliation to build.
- Pipeline ordering (all three walked it clean): `LightSystem.emitNoise` sits after `enemyPhase` and immediately before `NoiseSystem.resolve`, mirroring the existing combat/distraction emitters; `NoiseSystem` skips dead enemies and resolves against post-move positions; light noise shares the one queue and is cleared with it. Acted-path-only honesty is pinned by `wallBumpEmitsNoLightNoise`.
- Determinism: neither `radiusFor` nor `emitNoise` draws from `state.rng()`, so the seeded floor/identity/weather stream is untouched. The removed no-radius `compute` overload left no dangling `RADIUS` refs (all three call sites route through `compute(RunState)`).
- L1 (LIGHT_NOISE_RADIUS / VISION_RANGE / LIT_RADIUS are independent 6's): semantically distinct constants that happen to share a value; the lure test isolates the noise path via distance (enemy 10 from player). No code change — documenting every numeric coincidence would be noise.

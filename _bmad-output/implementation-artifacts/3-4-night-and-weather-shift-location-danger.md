---
baseline_commit: 1279f91
---

# Story 3.4: Night and weather shift location danger

Status: review

## Story

As Klein,
I want locations to change danger with the clock and weather,
So that *when* I go matters as much as *where* (FR-10).

## Acceptance Criteria

**AC-1 (FR-10, night flips):** Given nightfall, when location states update, then the Mercenary Graveyard's undead and the Sunken Well's creature become active, the Poacher's Camp patrols turn more aggressive, and the Beehive Grove flips *safer* (the sole exception) — every other structure keeps its authored day hazard.

**AC-2 (FR-5/FR-10, weather stacks):** Given a weather roll, when it stacks on a location, then its listed effect applies — Storm raises the structural-collapse chance at decayed/structural structures. (Fog's visibility shrink and Cold Snap's Temperature drive already ship — see Baseline; 3.4 owns the *location-danger* stack, i.e. Storm's collapse bump.)

**AC-3 (AD-5/AD-6 discipline):** Given the flips are derived from the persisted clock + weather (no new persisted field), when a step resolves, then it draws exactly one seeded roll for the structure hazard (the night override *replaces* the day hazard, never adds a second structure draw), the generic 3.3 night-stumble overlay still stacks on top, and the flips survive save/load without any new state.

## Baseline (what the substrate already ships)

Story 3.4 fills the seam Story 3.3 built. The `1279f91` baseline already provides:

- **The 3.4 hazard-override seam** (Story 3.3, `HazardSystem.nightHazardFor(state, structure)`): a private method `HazardSystem.step` calls to resolve a structure's hazard, returning `structure.hazard` today — the clearly-marked extension point where 3.4 authors per-location night flips. **This story fills it; it does NOT rework `step`'s trigger.** [core/src/main/java/com/margins/rogue/system/HazardSystem.java]
- **The authored hazard model** (Story 3.2, `StructureTable.Hazard`): an enum carrying `displayName/chancePercent/damage/message` + `onStep(player, rng, messages)` (one seeded `nextInt(100)` roll → apply damage + log line). The 11 structures each bind one `Hazard`. Two are contractual (Hunter's Blind, Old House); the rest are tunable content (PRD §8) — **none of the four night-flipped structures is a contractual worked example**, so the flips are free to author.
- **The generic 3.3 night overlay** (`HazardSystem.nightOverlay`): at night, no torch, not within the campfire safe radius → a 20%/1-HP stumble, one seeded draw, distinct from any structure hazard. AC-3's "stacks on top" means a night structure step already resolves TWO draws (overlay + structure hazard); 3.4 changes *which* hazard the structure draw resolves, not the draw count.
- **The derived day/night phase** (`RunState.isDay()`/`getClockPhase()`, `DAY_LENGTH=100`/`NIGHT_LENGTH=70`/`CYCLE_LENGTH=170`) and **per-cycle weather** (`RunState.getWeather()`, `Weather.CLEAR/RAIN/FOG/STORM/COLD_SNAP`, rolled at each cycle boundary). Both persist via `clockTurns`/`weather` — the flips derive from them, **no new persisted field** (AD-6).
- **Fog already shrinks FOV** (Story 1.4, `FovSystem.radiusFor`: `!isDay() || weather==FOG → DARK_RADIUS=4`). **Cold Snap already drives Temperature** (Story 1.6, `TemperatureSystem`, ~-2/turn → Frozen in ~38 turns). So two of FR-5's three weather effects already land; **3.4 does NOT rebuild them** — it owns the *location-danger* stack (Storm's structural-collapse bump), the one weather×location interaction not yet wired.
- **The step-trigger wiring** (`TurnEngine:73-76`): `HazardSystem.step` fires on a landed, non-zero-displacement MOVE. Untouched — 3.4 authors data + the seam's resolution, never a new trigger.

**What the baseline does NOT have — Story 3.4's actual scope:**

- **No per-location night states.** `nightHazardFor` returns the day hazard unconditionally; the Graveyard/Sunken Well/Poacher's Camp/Beehive Grove do not change at night.
- **No weather×location danger.** Storm has no effect on any structure's collapse chance; the `Hazard` roll is a fixed `chancePercent` with no weather term.

## In/Out of Scope Seam

**IN:**
- **Per-location night hazard flips through the `nightHazardFor` seam** (AC-1): the four named structures resolve a *different, worse* hazard at night (Graveyard undead, Sunken Well creature, Poacher's Camp aggressive patrol), and the Beehive Grove resolves a *milder/none* hazard at night (the sole safer flip). Every other structure keeps its authored day hazard at all hours.
- **Storm's structural-collapse stack** (AC-2): a weather term that raises the effective collapse chance of structural/decay hazards while Storm is the active weather — applied in `HazardSystem` (the seam owner), one seeded draw against the modified chance.
- **Derived, no new persisted field** (AC-3): the flips read `isDay()` + `getWeather()` (already persisted) — exactly the 3.3 pattern.
- Content wiring (the night `Hazard` variants + the structural category) + AC pins + full-suite/boot verification.

**OUT (later stories / already shipped):**
- **Enemy AI, patrol density, spawning, detection escalation** — "undead/creature *active*" and "patrols *more aggressive*" are expressed here as a **worse step-hazard at the location** (the seam's currency), NOT as new enemies, new detection rules, or occupation density. Real garrison/patrol thickening is **Epic 4** (AD-11 occupation escalation, `DetectionSystem`/`CombatSystem`). 3.4 stays within the hazard seam.
- **Fog's FOV shrink** (Story 1.4) and **Cold Snap's Temperature drive** (Story 1.6) — already ship; 3.4 does not touch `FovSystem`/`TemperatureSystem`.
- **Rain's effect** — Rain has no listed location-danger effect in FR-5 (it's the mild-negative filler); 3.4 authors none.
- **SKILL/knowledge querying** (which locations flip, learned) — **Story 3.5**.
- **Weight/capacity, currency, traders** — Epic 6.
- Any new `RogueTile`, any new persisted `RunState` field, any new noise emitter, any extra clock tick — reuse the 3.2/3.3 shape (metadata + the one step-trigger).

## Design Decisions (the interpretation calls)

1. **"Location danger flips" = the location's step-hazard flips, resolved through the `nightHazardFor` seam.** The seam's currency is a `StructureTable.Hazard`; the minimal, seam-consistent realization of AC-1 is that the four named structures return a different `Hazard` at night. "Undead/creature become active" and "patrols more aggressive" are authored as *worse* night hazards (higher chance and/or damage, a night-specific message); "Beehive Grove safer" is a *milder/NONE* night hazard. The full enemy-behavior version (actual undead actors, patrol routes) is Epic 4 — 3.4 delivers the danger flip within the hazard model, which is exactly what 3.3 scoped the seam for.
2. **The night override REPLACES the day hazard (one structure draw), and stacks with the generic 3.3 overlay (a separate draw).** `nightHazardFor` returns *either* the day hazard *or* the night variant — never both — so a step still makes exactly one structure-hazard roll (AD-5). The generic night-stumble overlay (3.3) is a distinct, already-counted draw; a night structure step therefore resolves overlay + one (night-variant) structure hazard, identical in draw-count to the 3.3 baseline. No new draws.
3. **Beehive Grove is the sole safer flip → its night hazard is `NONE` (bees dormant).** The day `SWARM` (25%/1) drops to no hazard at night. This is the one structure whose danger *falls* after dark; it must be pinned as the explicit exception (a monotone "all worse at night" test would wrongly flag it).
4. **Storm's stack is a chance modifier on structural hazards, applied by `HazardSystem` around the single draw — not a second hazard, not a new tile.** A "structural/decay" hazard category (the collapse family: weak plank, soft rot, collapsing stone, tower collapse, structural decay, cave-in) gets a Storm bonus added to its effective `chancePercent` for the one seeded roll. Fog/Cold Snap are already realized elsewhere (Decision: do not re-implement). Rain has no location effect. The modifier is derived from `getWeather()` (persisted) — no new field.
5. **Derived, never persisted (AD-6); metadata + step-trigger only (3.2/3.3 precedent).** The flips read `isDay()` + `getWeather()`; the night `Hazard` variants are authored constants; no `RogueTile`, no `RunState` field, no noise (only AD-9 emitters emit), no extra clock tick. The `Hazard.onStep` single-draw contract is preserved (extended with a weather-chance term, still one `nextInt`).

## Tasks / Subtasks

- [x] **Task 1 — Per-location night hazard flips via the `nightHazardFor` seam (AC: 1)**
  - [x] Author the night `Hazard` variants (tunable content, PRD §8; new `StructureTable.Hazard` entries): a Graveyard undead hazard, a Sunken Well creature hazard, a Poacher's Camp aggressive-patrol hazard — each worse than its day baseline (higher chance and/or damage, a night-specific `message`). The Beehive Grove night state is `Hazard.NONE` (Decision 3, the sole safer flip).
  - [x] Fill `HazardSystem.nightHazardFor(state, structure)`: when `!state.isDay()`, map the four named structure types (`RogueTileMap.STRUCTURE_GRAVEYARD/SUNKEN_WELL/POACHERS_CAMP/BEEHIVE_GROVE`) to their night hazard; every other structure (and all of daytime) returns `structure.hazard` unchanged. Do NOT touch `step`'s trigger.
  - [x] Tests: at night each of the four flips to its night hazard (undead/creature/patrol worse; Beehive → NONE, no damage); at day all four resolve their authored day hazard; a non-flipped structure (e.g. Hunter's Blind) is identical day and night; the flip is deterministic per fixed seed (AD-5).

- [x] **Task 2 — Storm raises structural-collapse chance at decayed structures (AC: 2)**
  - [x] Categorize the structural/collapse hazards (weak floor plank, soft rot, collapsing stone, tower collapse, structural decay, cave-in) — a `boolean structural` on `Hazard`, or an explicit set. Author a Storm chance bonus (tunable, PRD §8).
  - [x] Apply the bonus in `HazardSystem` (the seam owner) around the SINGLE seeded roll: while `getWeather() == STORM` and the resolved hazard is structural, the effective `chancePercent` rises by the bonus (capped ≤ 100). Preserve the one-`nextInt`-per-step contract (extend `Hazard.onStep`/add a scaled resolve; do NOT add a second draw). Fog/Cold Snap/Rain add no location bonus (Decision 4).
  - [x] Tests: under Storm a structural hazard fires measurably more often than under Clear across a seed range (same structure, same position); a non-structural hazard (e.g. a swarm) is unaffected by Storm; still exactly one seeded draw per step (no draw-count change); deterministic per fixed seed.

- [x] **Task 3 — AD-5/AD-6 discipline + 3.3 overlay coexistence (AC: 3)**
  - [x] Derived, no new persisted field: the flips read `isDay()`/`getWeather()` only. A save/load round-trip reproduces the same night+Storm hazard resolution (string-scan the JSON for any new key — none).
  - [x] One structure draw per step: the night override replaces (never stacks two structure hazards) — pin the draw count. The generic 3.3 night-stumble overlay still fires and stacks on top at a flipped structure (reuse the 3.3 stacking pattern at, e.g., the night Graveyard).
  - [x] No new `RogueTile`, no new persisted field, no noise emission, no extra clock tick — each pinned by a test or by construction.

- [x] **Task 4 — No-regression + boot (AC: all)**
  - [x] Full suite: `mvn -o -pl core test` — the **372** 3.3-post-review tests stay green (the four flipped structures' DAY hazard is unchanged, so `StructureContentTest`'s day-time hazard suite is untouched; `ForayLoopTest`'s `theNightOverlayStacksWithTheAuthoredHazard` uses Hunter's Blind, a non-flipped structure, so it stays green), plus the new night/Storm tests.
  - [x] Launch: `mvn -o -q -pl core install` + `timeout 40 mvn -o -pl desktop exec:java` — boot clean.

- [x] **Task 5 — AC pins + no-forced-scope (AC: all)**
  - [x] AC-1 pin: the four night flips + the Beehive exception (Task 1). AC-2 pin: Storm's structural bump (Task 2). AC-3 pin: derived/one-draw/round-trip (Task 3).
  - [x] Scope guard: assert 3.4 authored NO enemy actors, NO detection/patrol-density change, NO `FovSystem`/`TemperatureSystem` edit (Fog/Cold-Snap untouched), NO new tile/field/noise/clock tick — the flips live entirely in the hazard model + the seam.

## Dev Notes

### Current state (what exists, to preserve)

- **`HazardSystem` (post-3.3-review)** — `step(state, x, y, messages)` runs `nightOverlay(state, messages)` first (the generic 3.3 stumble: night + no torch + not within campfire safe radius → 20%/1-HP, one draw), then resolves the structure hazard via `nightHazardFor(state, structure).onStep(player, rng, messages)`. **`nightHazardFor` returns `structure.hazard` today — this is the seam Task 1 fills.** `isPlayerAtCampfireSafePoint()` and `NIGHT_STUMBLE_MESSAGE` are 3.3's; leave them. [core/src/main/java/com/margins/rogue/system/HazardSystem.java]
- **`StructureTable.Hazard`** (Story 3.2) — enum with `displayName/chancePercent/damage/message` and `onStep(RoguePlayer, Random, List<String>)` = `if (chancePercent > 0 && rng.nextInt(100) < chancePercent) { hurtRaw(damage); messages.add(message); }`. **One seeded draw per call.** Add the night variants here (new enum entries) and the `structural` category. The two contractual hazards (WEAK_FLOOR_PLANK on Hunter's Blind, STRUCTURAL_DECAY on Old House) keep their day values — but WEAK_FLOOR_PLANK/STRUCTURAL_DECAY are in the *structural* set, so Storm may raise their chance; that's a weather stack, not a change to the authored day baseline (the contract is the base numbers). [core/src/main/java/com/margins/rogue/world/StructureTable.java]
- **`StructureTable.Structure`** — binds one `Hazard` per structure via `structure.hazard`. The four flips are keyed by `structureType` in `nightHazardFor`, NOT by adding a second `Hazard` field to `Structure` (keep the table's shape; the night mapping lives in `HazardSystem`, the seam owner). Structure type constants: `RogueTileMap.STRUCTURE_GRAVEYARD/SUNKEN_WELL/POACHERS_CAMP/BEEHIVE_GROVE`.
- **`RunState`** — `isDay()`, `getClockPhase()`, `getWeather()` (persisted `weather`), `getClockTurns()`. All flips derive from these. Do NOT add a field (AD-6; the field-absent migration rule — a new persisted field would inherit nondeterministic ctor state).
- **`TurnEngine.advance`** — the acted pipeline; `HazardSystem.step` is called in the MOVE branch (`TurnEngine:73-76`), before the survival ticks. Untouched by 3.4.
- **`Weather`** — `CLEAR/RAIN/FOG/STORM/COLD_SNAP`; `getWeather()` returns the cycle's roll. Storm is the AC-2 driver.
- **`FovSystem.radiusFor`** — `!isDay() || weather==FOG → DARK_RADIUS`. Fog's visibility effect is DONE; do not touch.
- **`TemperatureSystem`** — Cold Snap's drive is DONE; do not touch.

### Carried lessons (3.2/3.3, applied)

- **Fill the seam, don't rework the trigger** (3.3 built `nightHazardFor` precisely so 3.4 extends one method). The night mapping + Storm modifier live in `HazardSystem`; `step`'s guard and `TurnEngine`'s call site are untouched.
- **One rng draw per step (AD-5)** — the night override *replaces* the day hazard (one structure draw); the Storm bonus modifies the *chance* of that same single draw. Never add a second structure draw. The 3.3 generic overlay is its own already-counted draw.
- **Derived, never persisted (AD-6)** — read `isDay()`/`getWeather()`; author the night `Hazard` variants as constants. No `RunState` field (the field-absent migration rule bit 1.3/3.x — avoid it entirely by not adding a field).
- **Metadata + step-trigger, never a tile/field/noise** (3.2 Decision 2, 3.3 Decision 3) — the flips are authored `Hazard` data resolved at the existing step-trigger. No `RogueTile`, no noise (AD-9 emitters only), no extra clock tick.
- **The day baseline stays byte-stable** — the four flips only change *night* resolution; every day-time hazard test (`StructureContentTest`) must stay green untouched. Storm's bump only applies while Storm is active, so Clear-weather tests (the suite's default via `runWithClearWeather`) are unaffected.
- **Read map dims / structure types dynamically** — `RogueTileMap.STRUCTURE_*` constants and `getStructureType`; never hard-code.
- **Beehive is the exception** — any "night is worse" assertion must special-case the Beehive safer-flip (Decision 3), or it will falsely fail.

### Scope discipline (CLAUDE.md §2/§3)

- Minimum code: the four night `Hazard` variants + the `nightHazardFor` mapping + the `structural` category + the Storm chance term + tests. **No** enemy actors/patrol AI (Epic 4), **no** `FovSystem`/`TemperatureSystem` edits (Fog/Cold-Snap already ship), **no** Rain location effect, **no** SKILL/knowledge (3.5), **no** new tile/persisted field/noise/clock tick.
- Do NOT touch `step`'s trigger, the generic 3.3 `nightOverlay`, the 3.2 day hazards' base numbers, the pre-AD-8 reject gate, or the `Supply.count()-5` scatter pin (3.4 adds no scatterable item).
- Do NOT express "undead/creature/patrol" as new agents — that is Epic 4. Here they are the location's night step-hazard.

### Testing standards

- JUnit 5 headless core; `new RunState(seedL)`. Pin the weather with `setWeather(...)` (the Story 1.6 test hook) to isolate Storm vs Clear. Drive to night via acted `WAIT`s (the `ForayLoopTest.driveToNight` pattern) or place + assert `isDay()==false`. Read structure cells via the `StructureContentTest`/`ForayLoopTest` `footprint`/`walkableCellAndNeighbor` helpers.
- Deterministic-on-fixed-seed turn-level assertions (a hazard fires/does-not-fire on a seed; a chance-shift is measured across a seed range) — not log-line-only checks.
- Run: `mvn -o -pl core test` (offline). Baseline **372 green** (3.3 post-review); the story adds the night/Storm tests. Launch: `mvn -o -q -pl core install` + `timeout 40 mvn -o -pl desktop exec:java` (exit 143/124 = timeout kill = clean boot).

### References

- [Source: epics.md#Story-3.4] — the two ACs (night location flips; weather stacks on a location).
- [Source: prds/prd-The-Margin-2026-08-06/prd.md#FR-10 (215)] — "Night shifts several locations' danger (Graveyard undead active, Sunken Well creature active, Poacher's Camp patrols more aggressive, Beehive Grove — the sole location that flips *safer*)."
- [Source: prds/prd-The-Margin-2026-08-06/prd.md#FR-5 (169-173)] — the weather roll + "each type has a listed pro/con (Fog reduces visibility; Storm raises structural-collapse chance; Cold Snap slows spoilage but drives Temperature toward Frozen)."
- [Source: prds/prd-The-Margin-2026-08-06/prd.md#Glossary (101-102)] — Day/Night "flips several locations hostile (the Mercenary Graveyard's undead, the Sunken Well's creature)"; Weather "stacks with the Day/Night Cycle."
- [Source: prds/prd-The-Margin-2026-08-06/prd.md#UJ-2 (64,67)] — "start back before night flips the well's creature active"; "if night catches him mid-return, the Sunken Well's creature and a dark forest both threaten."
- [Source: architecture/ARCHITECTURE-SPINE.md#AD-5 (84-88)] — one committed turn per real action; one seeded roll.
- [Source: architecture/ARCHITECTURE-SPINE.md#AD-6 (90-97)] — no new persisted field; the field-absent migration rule.
- [Source: architecture/ARCHITECTURE-SPINE.md#AD-18 (175-179)] — FOV/light/weather are core; Fog shrinks the visible radius (already shipped in `FovSystem`).
- [Source: story-3.3 (predecessor, Status: done)] — the `nightHazardFor` seam (built to be filled here), the generic night overlay, the derived-not-persisted pattern, the one-draw-per-step discipline.
- [Source: story-3.2 (Status: done)] — the `StructureTable.Hazard` model + `HazardSystem.step` trigger; the two contractual hazards (Hunter's Blind, Old House) — neither is a night-flipped structure.
- [Source: deferred-work.md (252)] — "The Story 3.4 'override hazard on the night path' seam" — 3.3 built the seam; **3.4 authors the content.**

## Dev Agent Record

### Agent Model Used

Claude Opus 4.8 (1M context), the session's model.

### Debug Log References

- 2026-08-10 dev-story, Tasks 1-5 (red-green, one test file `NightWeatherHazardTest`):
  - RED: `Hazard.GRAVE_UNDEAD/WELL_CREATURE/POACHER_PATROL`, `Hazard.isStructural()`, and the package-private `HazardSystem.nightHazardFor` all missing (compile fail) → GREEN.
  - GREEN Task 1: 3 night `Hazard` variants (35%/2 each, night message) + `nightHazardFor` switch (Graveyard→undead, Sunken Well→creature, Poacher's Camp→patrol, Beehive→NONE; day + all others → `structure.hazard`).
  - GREEN Task 2: `Hazard.isStructural()` (the 6-hazard collapse family) + a `Hazard.onStep(..., chanceBonus)` overload (still one `nextInt`, chance capped ≤100) + `HazardSystem.stormBonus` (Storm × structural → +20pp, else 0). Storm-vs-Clear fire-rate test measured 60-seed Forest Shrine counts (Storm strictly > Clear).
  - GREEN Task 3: round-trip (no new JSON key; night+Storm re-derives identically), overlay-stacks-on-flip (max = overlay 1 + undead 2), one-clock-tick pin.
  - Full core suite: 383 tests, 0 failures (372 baseline + 11 new). Desktop boot clean (`timeout` kill, exit 124, no exceptions).
- No HALT conditions; no new dependencies.

### Completion Notes List

- **Task 1 (AC:1) — the night flips fill the 3.3 seam.** `HazardSystem.nightHazardFor(state, structure)` now returns, at night, `GRAVE_UNDEAD` (Graveyard), `WELL_CREATURE` (Sunken Well), `POACHER_PATROL` (Poacher's Camp) — each strictly worse than its day baseline (35%/2 vs 20-25%/1) — and `Hazard.NONE` for the Beehive Grove (the sole safer-flip, swarm dormant). By day, and for every other structure, it returns the authored `structure.hazard` unchanged. Derived from `isDay()` (no persisted state); made package-private so the mapping is unit-tested directly.
- **Task 2 (AC:2) — Storm stacks on structural locations.** `Hazard.isStructural()` marks the collapse family (weak plank, soft rot, collapsing stone, tower collapse, structural decay, cave-in). A new `Hazard.onStep(p, rng, messages, chanceBonus)` overload raises the effective chance for the SAME single seeded draw (cap 100); `HazardSystem.stormBonus` supplies +20pp only when `getWeather()==STORM` AND the hazard is structural. Fog (FovSystem) and Cold Snap (TemperatureSystem) already land their effects and are untouched; Rain adds nothing.
- **Task 3 (AC:3) — discipline.** No new persisted field (round-trip string-scans the JSON — none; night+Storm re-derives from `clockTurns`/`weather`). One structure draw per step (the flip replaces the day hazard; `steppingOntoTheNightGraveyardDealsTheUndeadDamage` pins HP-drop ∈ {0, undead}). The generic 3.3 night stumble still stacks at a flipped structure (max = 1 + 2). One clock tick per step.
- **Task 5 — scope guard (by construction).** Only `StructureTable.java` + `HazardSystem.java` changed in main — no `FovSystem`/`TemperatureSystem`/`DetectionSystem`/`CombatSystem`/`RogueTile`/`RunState` edits, no enemy actors, no new tile/persisted field/noise/clock tick. "Undead/creature/patrol active" is expressed as the location's worse night step-hazard (the seam's currency); real patrol/garrison AI stays Epic 4.
- **Verification:** `mvn -o -pl core test` = 383 tests / 0 failures. `mvn -o -q -pl core install` + `timeout mvn -o -pl desktop exec:java` = clean boot.

### File List

- `core/src/main/java/com/margins/rogue/world/StructureTable.java` — 3 night `Hazard` variants (`GRAVE_UNDEAD`/`WELL_CREATURE`/`POACHER_PATROL`), `Hazard.isStructural()`, `Hazard.onStep(..., chanceBonus)` overload.
- `core/src/main/java/com/margins/rogue/system/HazardSystem.java` — filled `nightHazardFor` (per-location night flips; package-private), `stormBonus` + `STORM_STRUCTURAL_BONUS`, Storm-aware `step`.
- `core/src/test/java/com/margins/rogue/system/NightWeatherHazardTest.java` — NEW: 11 tests across Tasks 1-5 (night flips, Beehive exception, Storm fire-rate, non-structural-unaffected, round-trip, overlay-stacks, one-tick).

## Change Log

- 2026-08-10: Created Story 3.4 — Night and weather shift location danger (FR-10/FR-5). Fills the 3.3 `nightHazardFor` seam with per-location night flips + Storm's structural-collapse stack. Status backlog → ready-for-dev. Sprint status updated.
- 2026-08-10: Implemented via dev-story — Tasks 1-5 complete (night flips, Beehive safer-flip, Storm structural stack; 11 new tests). 383 core tests green, desktop boot clean. Only `StructureTable`+`HazardSystem` changed in main (scope clean). Status ready-for-dev → review. Sprint status updated.

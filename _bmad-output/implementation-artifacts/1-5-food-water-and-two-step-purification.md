---
baseline_commit: af41e36
---

# Story 1.5: Food, water, and two-step purification

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As Klein,
I want to source, cook, and purify food and water,
so that I can eat and drink without poisoning myself (FR-6).

## Acceptance Criteria

1. **Given** a raw water source, **When** I collect from it (Sunken Well stable / Pond / River 20% direct-drink poison risk), **Then** untreated water carries its source's risk until purified.
2. **Given** raw water and a fire with coal, **When** I filter (SKILL-based, reduces risk) then boil, **Then** the water reaches 0% risk; filtration alone reduces but does not eliminate it.
3. **Given** food in inventory, **When** turns pass, **Then** it advances Fresh → Half Rotten → Fully Spoiled; cooked meat and purified water resist spoilage; storage items slow the rate.
4. **Given** cooking or purification is performed, **When** the outcome is rolled, **Then** it is governed by SKILL (the horizontal growth path, FR-11).

## Scope — this is the largest Epic 1 story (read first)

Justine chose to **build 1.5 fully end-to-end** (playable food/water/purification loop now, not a model-only slice) and to **model perishables by exploding the `Supply` enum** (stateful variant types; a spoil/cook/purify step is a `remove(old)+add(new)` type-swap within the existing int-stack inventory — **no per-item object model, no parallel per-item state**). Honor both. Two seams still hold even under "build fully":

- **The campfire's WARMTH and the torch stay in Story 1.6.** This story builds the campfire only as a **fire station** for cooking/boiling — it *lights* (reuses the Story 1.4 positioned light → FOV + noise) and is *exposed*, but it does **not** warm the player or drive Temperature. Do not touch `TemperatureSystem`/temperature drift.
- **The tiered debuff pipeline (Nausea→Fever→Delirium, Diarrhea) is Story 1.7.** Here, unsafe consumption (a failed poison-risk roll) applies **immediate HP harm** via the existing `RoguePlayer.hurtRaw`/`starve` path (like `TrueIdentity.TAINTED`/`SPOILED_MEAT` do today). Wire the tiered debuffs in 1.7 — leave a clear hook, don't build the debuff system.

Also **minimal, not the full Epic 3 world:** water-source *tiles* (Well/Pond/River) are placed as simple map features by `FloorGenerator` — this is a deliberate forward-pull; Epic 3 (3-1/3-2) folds water sources into the real 11-structure world-gen. Do **not** build the world-structure system, danger tiers, or hybrid map-gen here.

**Dependency the enum-explosion forces (design note):** spoilage is inherently per-item-age, but the stack model has only `type + count` and no per-item timer. So spoilage is a **batch** property: a single `RunState` spoilage clock drives a `SpoilageSystem` that advances each perishable stack one stage at fixed intervals. A stack ages together; a freshly-collected item added to an existing stack shares that stack's batch age. This is the honest consequence of "explode the enum + keep the stack model" — document it, don't fight it.

## Tasks / Subtasks

- [x] **Task 1 — The `SKILL` stat (AC: 4)**
  - [x] Add `private int skill;` to `RoguePlayer` beside `str`/`instinct`/`grit`/`voice` (field-initialized-safe for AD-6 loads), set it in the constructor with the other stats, and add `getSkill()`. Give it a sane MVP starting value (mirror the other stats' constructor init).
  - [x] SKILL is the horizontal-growth axis (FR-11): this story only *reads* it for cook/purify rolls; **no XP/level-up mechanic here** (growth-by-knowledge is later). A test asserts a fresh player has the expected starting SKILL.
- [x] **Task 2 — New consumable types via enum explosion (AC: 1, 2, 3)**
  - [x] Add `Supply` values (self-evident, deterministic): `COAL`, `RAW_MEAT`, `HALF_ROTTEN_MEAT`, `SPOILED_MEAT`, `COOKED_MEAT`, `WELL_WATER`, `POND_WATER`, `RIVER_WATER`, `FILTERED_WATER`, `BOILED_WATER`, `SALT` (a storage item). Each binds to a single self-identity (`possibleIdentities()` returns one), so `IdentifyMap` binds it deterministically.
  - [x] Add `Supply.isSelfEvident()` (default `false`; `true` for all Task-2 types) and mark self-evident types **identified at build time** in `IdentifyMap.build` — a "Cooked Meat" must show its real name immediately, not the unidentified-container name. This preserves the mystery-container gamble for the original Route-1 supplies (leave those untouched). Add a test that a self-evident type reads its true name from a fresh run.
  - [x] Add the matching `TrueIdentity` entries **only for the deterministic effects that need no RNG** (e.g. `COOKED_MEAT` → good `eat`, `BOILED_WATER`/`WELL_WATER` → clean `drink`). **Risky consumption (raw/rotten/spoiled/river/pond/filtered) must NOT go through `TrueIdentity.apply(player)`** — that signature has no RNG. Route risky eat/drink through Task 6's consumption path instead. Keep `TrueIdentity` for the risk-free outcomes only.
- [x] **Task 3 — Water-source tiles + collection (AC: 1)**
  - [x] Add tile constants to `RogueTile`: `WELL`, `POND`, `RIVER` (new ints after `DOOR`). Decide walkability/opacity: they are non-opaque; make them walkable-adjacent features you collect *from* (they need not be walkable themselves — collection acts on an adjacent source tile). Keep `isWalkable`/`isOpaque` correct for the new values.
  - [x] In `FloorGenerator.generate`, place a few water-source tiles (a Well, a Pond, a River patch) at/near room centers using the seeded `rand` (AD-5, deterministic). Do **not** build the world-structure system — just scatter the three source types like supplies are scattered today.
  - [x] Add a `COLLECT` `PlayerAction.Kind` + factory: standing on or adjacent to a source tile, collecting yields one of the matching raw water types (`WELL_WATER`/`POND_WATER`/`RIVER_WATER`) into the backpack (respect `BACKPACK_FULL` — a full pack refuses the collect and commits no turn, mirroring the pickup safety-net). Drinking `RIVER_WATER` without purifying is the AC-1 "20% direct-drink risk" path (Task 6's consumption roll).
  - [x] `MarginScreen` renders the three source tiles distinctly (a simple placeholder colour, like the existing tile colours). No HUD (1.8).
- [x] **Task 4 — The campfire fire-station (AC: 2, 3 — fire for cook/boil only)**
  - [x] Add a `BUILD_CAMPFIRE` `PlayerAction.Kind` + factory. Building places a campfire at the player's tile: reuse Story 1.4's light (`state.setLight(px, py)`) so it lights (FOV restore) and is exposed (per-turn noise), and record the campfire tile as the **fire station** (a persisted `campfireX/campfireY` on `RunState`, `-1` = none, field-initialized; mirrors the light-tile pattern). MVP: building costs no material (document as an assumption; a Wood cost can arrive with 1.6's torch).
  - [x] Add `RunState.hasFireAt(x, y)` / `isAtFire(player)` — true when the player is on or adjacent to the campfire tile. Cooking (Task 5) and boiling (Task 6) require this. `restart()` clears the campfire (mirror `clearLight()`).
  - [x] **Warmth is NOT built here** (Story 1.6). The campfire does not change Temperature.
- [x] **Task 5 — Cooking (AC: 3, 4)**
  - [x] Add a `COOK` action. At a fire (Task 4), cooking transforms one `RAW_MEAT` (or `HALF_ROTTEN_MEAT`) into `COOKED_MEAT` via a `CookingSystem` — a **SKILL-governed roll** on `state.rng()` (higher SKILL → higher success; on failure, the meat is consumed/ruined per a simple rule — document the failure outcome). `COOKED_MEAT` resists spoilage (Task 7).
  - [x] A refused cook (no fire, no raw meat) commits **no turn** (mirror the inert-USE precedent in `TurnEngine`).
- [x] **Task 6 — Purification: filter then boil (AC: 1, 2, 4)**
  - [x] Add a `PURIFY` action with two steps (or two actions `FILTER`/`BOIL` — pick one and be consistent). Implement in a `PurificationSystem` with `state.rng()`:
    - **Filter** (SKILL-based): `WELL/POND/RIVER_WATER` → `FILTERED_WATER`, **reducing** the poison risk by a SKILL-scaled amount but **never to 0** (AC-2: "filtration alone reduces but does not eliminate"). No fire/coal needed.
    - **Boil** (needs the fire station AND consumes one `COAL`): `FILTERED_WATER` (or raw) → `BOILED_WATER` at **0% risk** (AC-2). Boiling is what makes water safe.
  - [x] Model each raw water type's source risk as data (e.g. `WELL_WATER` ≈ stable/low, `RIVER_WATER` 20% direct-drink, `POND_WATER` worst / requires both steps — AC-1/FR-6). The risk travels with the type until boiled.
  - [x] **Consumption risk lives here too:** drinking a risky water (raw/filtered/river-direct) rolls its risk on `state.rng()`; on a failed roll, apply **immediate HP harm** (`hurtRaw`, like `TrueIdentity.TAINTED`) — the tiered debuff pipeline is **Story 1.7** (leave a `// TODO(1.7): route to DebuffSystem` hook, do not build it). `BOILED_WATER`/`WELL_WATER` never harm.
  - [x] **Integration gotcha — the `USE` path:** risky consumables are still *drunk/eaten* via the existing `USE` action, but `TurnEngine`'s `USE` case today applies `identityOf(type).apply(player)` unconditionally (no RNG). Branch it: if the used type is a risk-bearing consumable, route to the consumption method here (with `state.rng()`) **instead of** `TrueIdentity.apply`; keep the existing `identityOf().apply` path for the risk-free types (`COOKED_MEAT`, `BOILED_WATER`, `WELL_WATER`, and the original Route-1 supplies). This is the one place the new model touches existing turn logic — get it right and test both branches.
  - [x] Refused purify/boil (no water / no fire / no coal) commits **no turn**.
- [x] **Task 7 — Food spoilage (AC: 3)**
  - [x] Add a persisted `spoilageClock` int to `RunState` (field-initialized 0; advanced on acted turns like `clockTurns`). Add a `SpoilageSystem.tick(state)` run in `TurnEngine`'s acted branch: every `SPOIL_INTERVAL` turns, advance each perishable food stack one stage by type-swap — `RAW_MEAT`→`HALF_ROTTEN_MEAT`→`SPOILED_MEAT` (terminal). `COOKED_MEAT` **resists** (skipped or a much longer interval); water types do not spoil in this story (satisfies "purified water resists" trivially — note it).
  - [x] **Storage slows the rate:** while a `SALT` stack is in the backpack, multiply `SPOIL_INTERVAL` (e.g. ×2). Keep it a simple presence check.
  - [x] Spoilage advances **only on acted turns** (AD-5) — a wall-bump advances nothing. Document the batch-ageing simplification (a stack ages together; the clock is global).
- [x] **Task 8 — Wire actions + input, and tests**
  - [x] `TurnEngine` acted branch: handle the new actions (`BUILD_CAMPFIRE`, `COOK`, `FILTER`/`BOIL`, `COLLECT`) and call `SpoilageSystem.tick(state)` in the survival group (alongside hunger/thirst/temperature/clock). Preserve the "refused action commits no turn" rule for each.
  - [x] `MarginScreen`: minimal keybinds for the new actions (e.g. collect / build / cook / filter / boil) and render the water-source tiles. No HUD/labels (1.8) — just enough to drive the loop.
  - [x] **Serialization (AD-6):** new persisted `RunState` fields (`campfireX`/`campfireY`, `spoilageClock`) are plain ints → round-trip for free; field-initialize them. New `Supply`/`TrueIdentity`/`RogueTile` values are ordinals/ints → save-safe. Extend `RunStatePersistenceTest`: a campfire + spoilage clock survive a round-trip; a pre-1.5 save loads with no campfire / clock 0.
  - [x] **Tests** (headless, JUnit 5; mirror `SurvivalTickTest`/`WeatherSystemTest` patterns):
    - `PurificationTest`: filter reduces but never zeroes risk; boil (with coal + fire) → 0%; boil without coal/fire is refused (no turn, no transform); SKILL affects the filter outcome (higher SKILL → measurably better across a seed sweep).
    - `CookingTest`: raw→cooked at a fire is SKILL-governed; cook without a fire/raw is refused (no turn).
    - `SpoilageTest`: `RAW_MEAT`→`HALF_ROTTEN`→`SPOILED` across `SPOIL_INTERVAL` boundaries on acted turns; `COOKED_MEAT` resists; `SALT` in the pack slows the rate; a wall-bump advances no spoilage (AD-5).
    - `WaterRiskTest`: drinking `RIVER_WATER` fails its ~20% risk across a seed sweep and harms; `BOILED_WATER`/`WELL_WATER` never harm; risk is deterministic per seed (AD-5).
    - `WaterSourceTest`: `FloorGenerator` places the three source tiles deterministically per seed; `COLLECT` at a source yields the matching raw water type.
    - Full suite green: `mvn -o clean install`.

## Dev Notes

### Current state (what exists, what to ratify, what to preserve)

- **Inventory is a pure stack model** (`item/Inventory.java`): `int[] types` / `int[] counts`, 8 backpack + 2 equipped, `-1` empty sentinel. Its javadoc explicitly chose this "rather than a per-item object model." **Keep it.** Spoilage/cook/purify are `remove(oldType,1)` + `tryAdd(newType,1)` swaps. `count(type)`, `tryAdd`, `remove` are the tools. A full backpack makes `tryAdd` return `BACKPACK_FULL` — a transform that can't place its output must not destroy the input silently; decide and test that edge (e.g. refuse the transform, no turn).
- **`Supply` + `IdentifyMap` + `TrueIdentity`** (`item/`, `state/IdentifyMap.java`): a type's effect is its per-seed-bound `TrueIdentity`, applied in `TurnEngine`'s `USE` case via `identityOf(type).apply(player)`. The new perishables are **self-evident** (single identity, identified at build) so they show real names and behave deterministically. **`TrueIdentity.apply(RoguePlayer)` has no RNG** — so risk/skill rolls cannot live there; they live in the new `PurificationSystem`/`CookingSystem`/consumption path with `state.rng()` (AD-5).
- **`RoguePlayer` stats:** `str/instinct/grit/voice` (no SKILL, no AG — arch confirms these are design-forward). Add `skill` minimally. `eat(int)`, `drink(int)`, `hurtRaw(int)`, `starve(int)`, `heal(int)` exist and are the consumption effect surface. Hunger/Thirst tiers already tick (Story 1.2) — do not change them.
- **`RunState`** owns the seeded RNG (`rng()`, AD-5), the persisted light (`lightX/lightY` + `setLight/clearLight`, Story 1.4), the clock/weather (1.3), and `restart()` (resets run-scoped state). Add the campfire tile + spoilage clock **beside these**, following the exact field-initialized-persisted pattern (`lightX = -1`, `clockTurns = 0`). `restart()` must reset the new fields (mirror the `clearLight()`/clock reset added in 1.3/1.4).
- **`TurnEngine` acted branch** (`system/TurnEngine.java`) is the single acted path (AD-4/AD-5): survival ticks (hunger/thirst/temperature/clock) → detection → companion → enemy → **light noise (1.4)** → noise resolve → last-stand → FOV. Add `SpoilageSystem.tick` in the survival group and the new action cases in the `switch`, each honoring "refused action commits no turn" (see the inert-`USE`/wall-bump precedent). The campfire's light noise already flows through the Story 1.4 `LightSystem.emitNoise` — building a campfire that calls `setLight` makes it exposed for free.
- **`FloorGenerator.generate(w,h,rand)`** returns a `FloorResult { map, roomCenters }` and scatters supplies with the seeded `rand`. Add water-source placement the same way — deterministic, seeded (AD-5). Enemy/supply placement must stay reproducible; add the water draws at a **defined point** in the order and note that seeds now produce a different scatter (same lesson as 1.3's weather draw — document where the new draws sit).
- **`MarginScreen`** maps keys → `PlayerAction` and renders tiles by `map.getTile`. Add minimal keybinds and tile colours for the new content. It reads `map.isVisible()` for fog (1.4) — new tiles obey FOV for free.

### The 1.5↔1.6↔1.7 seam (hold these lines even under "build fully")

- **Warmth / Temperature is Story 1.6.** The campfire here is a *cooking/purification fire station* that lights (1.4) and is exposed (1.4 noise). It must NOT change `temperature` or touch `TemperatureSystem`. 1.6 adds warmth to the same campfire + weather-driven drift + the torch.
- **Tiered debuffs are Story 1.7.** Unsafe consumption applies immediate HP harm now (`hurtRaw`/`starve`), with a `// TODO(1.7)` hook where the bacterial/toxin tracks (Nausea→Fever→Delirium, Diarrhea) will attach. Do not build a debuff/status system.
- **World structures are Epic 3.** Water sources are minimal scattered tiles, not the 11-structure danger-tier system.

### Placement rationale (AD-3)

- **World state on `RunState`:** campfire tile, spoilage clock (like the light tile and clock). **Player state on `RoguePlayer`:** SKILL (like the other stats). **Systems** (`system/`): `SpoilageSystem`, `CookingSystem`, `PurificationSystem` — one-purpose pipeline helpers mirroring `ThirstSystem`/`LightSystem`. **Item taxonomy** (`item/`): the new `Supply`/`TrueIdentity` values. **Tiles:** `RogueTile` constants. Core stays headless (AD-2) — no libGDX in any new `com.margins.rogue.*` class; `MarginScreen` owns all render/input.

### Serialization — the pattern that applies directly (Stories 1.1–1.4)

- `SaveService.json()` sets `usePrototypes(false)` — every new persisted field is always written. Field-initialize `campfireX = -1`, `campfireY = -1`, `spoilageClock = 0` so a pre-1.5 save loads valid defaults (AD-6). New `Supply`/`TrueIdentity` are enums (serialized by name / stored as ordinals in the inventory int arrays); new `RogueTile` ints ride the tilemap which serializes inline (AD-6). No `restoreAfterLoad` reconciliation needed (plain persisted state — contrast 1.3's ctor-roll wart).
- **`IdentifyMap` grows** (more `Supply` ordinals) — its arrays are sized from `Supply.values().length` at build, and `markIdentified` guards range, so it scales. A pre-1.5 save's shorter `boundByOrdinal` still loads (the lazy-alloc / range guards already handle missing entries) — verify with a round-trip test.

### Scope discipline (CLAUDE.md §2/§3)

- Do **NOT** build: warmth/temperature drift or the torch (1.6); the tiered debuff/status system (1.7); the world-structure/danger-tier/hybrid-map system (Epic 3); a per-item object inventory or parallel per-item state (Justine chose enum-explosion + the stack model); the HUD/message-log surfacing of food/water/SKILL (1.8); XP/level-up for SKILL (growth-by-knowledge is later). Every changed line traces to one of the four ACs or its required wiring.
- This story is large; keep **each** mechanic minimal (a single storage item, a single meat line, three water sources, a no-cost campfire) and lean on existing patterns. If a piece balloons, prefer the smallest thing that satisfies the AC + a test.

### Testing standards

- JUnit Jupiter 5.10.2, `mvn -o clean install` (surefire 3.2.5). Drive turn behavior through `TurnEngine.advance` (mirror `SurvivalTickTest`), use seeded `RunState` + the `WeatherSystemTest.seedWithWeather`/`boundaryDifferingSeed` seed-search idiom for probabilistic outcomes (risk/skill rolls), and extend `RunStatePersistenceTest` for the new persisted fields. All randomness draws from `state.rng()` (AD-5) so tests are deterministic per seed.

### Project Structure Notes

- **New:** `system/SpoilageSystem.java`, `system/CookingSystem.java`, `system/PurificationSystem.java`; tests `PurificationTest`, `CookingTest`, `SpoilageTest`, `WaterRiskTest`, `WaterSourceTest`.
- **Modified:** `item/Supply.java` (+11 types, `isSelfEvident`), `item/TrueIdentity.java` (risk-free effects), `state/IdentifyMap.java` (self-evident → identified at build), `state/RunState.java` (campfire tile, spoilage clock, `restart()` reset, fire queries), `RoguePlayer.java` (SKILL), `RogueTile.java` (WELL/POND/RIVER), `FloorGenerator.java` (place sources), `system/PlayerAction.java` (new kinds), `system/TurnEngine.java` (new action cases + spoilage tick), `MarginScreen.java` (keybinds + tile render), `state/RunStatePersistenceTest.java` (round-trips).

### References

- [Source: epics.md#Story-1.5] — the four ACs.
- [Source: prd.md#FR-6] — water sources (Sunken Well stable / Pond both-steps / River 20% direct-drink); two-step purification (filter SKILL-based reduces, boil Coal+fire → 0%); food ladder Fresh→Half Rotten→Fully Spoiled; cooked/purified resist; storage slows; SKILL-governed.
- [Source: prd.md#FR-7] — the campfire is warmth+light+cooking+clean-water, stationary, exposed. **Warmth is 1.6**; 1.5 builds the cooking/purification/light/exposure facets only.
- [Source: prd.md#FR-8 / epics.md#Story-1.7] — unsafe consumption is the tiered debuff pipeline — **Story 1.7**; 1.5 applies immediate HP harm as the placeholder with a 1.7 hook.
- [Source: prd.md#FR-11] — SKILL is the horizontal-growth axis governing cooking/purification/repair; growth is by knowledge, not XP.
- [Source: ARCHITECTURE-SPINE.md — Stats & status row] — current code has STR/INSTINCT/GRIT/VOICE; **SKILL is design-forward** — this story introduces it minimally (read-only for rolls).
- [Source: ARCHITECTURE-SPINE.md#AD-3/AD-5/AD-6] — state ownership; seeded RNG for all rolls; field-initialized persisted fields.
- [Source: 1-4-fov-and-light-the-visible-camp-tension.md] — the positioned-light pattern the campfire reuses (`setLight`/`clearLight`, `LightSystem.emitNoise`), the field-initialized-persisted-int pattern, and the `restart()` reset precedent. Note the deferred item: **1.5/1.6 own light-tile bounds validation** — validate the campfire tile is a real walkable in-bounds tile when it lights.

## Dev Agent Record

### Agent Model Used

claude-opus-4-8 (1M context)

### Debug Log References

- Green baseline: 95 tests (HEAD af41e36).
- Core compiled clean after each layer (systems, wiring, screen).
- First full run: 1 failure — `ContinuousMapTest.mapHasNoStairsTiles` asserted only WALL/FLOOR/DOOR exist (AD-8), now sees the new water-source tiles. That test's intent is "no stairs/descent tiles"; updated its whitelist to allow `RogueTile.isWaterSource` (a legitimate Story 1.5 addition, not stairs).
- Final: `mvn -o clean install` → **117 tests, 0 failures, BUILD SUCCESS** (95 → 117; +22: PurificationTest 5, CookingTest 6, SpoilageTest 4, WaterRiskTest 4, WaterSourceTest 2, RunStatePersistenceTest +2, minus the updated ContinuousMap test).

### Completion Notes List

- **All 8 tasks + AC-1/2/3/4 satisfied**, built fully end-to-end per Justine's scope call, perishables via enum-explosion (no per-item state).
- **SKILL** added to `RoguePlayer` (starts 5) with `getSkill()`/`setSkill()` — read-only for cook/filter rolls; `setSkill` is also the hook FR-11 growth-by-knowledge will use. No XP mechanic.
- **11 new `Supply` provisions** (coal, meat ladder, water by source/treatment, salt), appended so existing ordinals + old saves are unchanged (AD-6). A **data-driven taxonomy** on `Supply` drives every transition: `spoilsTo`/`cooksTo`/`filtersTo`/`boilsTo`/`drinkRisk`/`isProvision`/`isRiskyProvision` — the systems stay thin.
- **Purification** — `PurificationSystem.filter` (SKILL roll → `FILTERED_WATER`, risk 10 not 0) and `.boil` (needs fire + 1 Coal → `BOILED_WATER`, risk 0). Filtration reduces (AC-2), boiling eliminates. Refused steps (no fire/coal/room) commit no turn (safe remove+re-add-on-full dance).
- **Cooking** — `CookingSystem.cook` (SKILL roll at the fire: `RAW_MEAT`→`COOKED_MEAT`, a botched cook ruins it → `SPOILED_MEAT`). `COOKED_MEAT` resists spoilage.
- **Spoilage** — `RunState.spoilageClock` + `SpoilageSystem.tick` on the acted path; every `SPOIL_INTERVAL`(50) turns each perishable food stack advances one stage (batch model, pre-tick snapshot so a stack isn't double-advanced). `SALT` in the pack doubles the interval; cooked/water resist.
- **Consumption risk** — `ConsumptionSystem.consume` applies nourishment (`TrueIdentity.apply`) then rolls the poison risk on `state.rng()` → `hurtRaw(6)` on fail, with a `// TODO(1.7)` hook for the tiered debuffs. The `USE` case routes **risky** provisions here; **safe** ones (cooked/well/boiled) keep the existing `identityOf().apply` path.
- **Campfire station** — `RunState.campfireX/Y` + `setCampfire`/`clearCampfire`/`isPlayerAtFire`; `BUILD_CAMPFIRE` lights it via the Story 1.4 light (FOV + exposure). **Warmth untouched** (1.6). `restart()` clears it.
- **Water sources** — `RogueTile.WELL/POND/RIVER` (walkable, non-opaque); `FloorGenerator` stamps them at room centers 1–3 **without drawing from `rand`**, so the seeded actor/supply stream is byte-identical — **zero layout regression** (a cleaner choice than the spec's "seeded draws," which would have shifted the stream). `COLLECT` yields the matching raw water.
- **Deviations (all documented, none scope-expanding):** (1) skipped the spec's `isSelfEvident`/mark-identified machinery — the new types' `Supply.displayName` IS their real name, so they read correctly with the identify system untouched; (2) all provisions carry nourishment in `TrueIdentity`, with the risk added by `ConsumptionSystem` for risky ones (matches the spec's intent); (3) water sources placed without RNG draws (stronger determinism); (4) added `RoguePlayer.setSkill` (tests + FR-11 hook); (5) updated `ContinuousMapTest`'s tile whitelist for the new tiles; (6) campfire build is free (MVP, per spec assumption).
- **Seams held** — no warmth/temperature drift, no torch (1.6); no debuff/status system (1.7, HP-harm placeholder + hook); no world-structure/danger-tier system (Epic 3); no HUD (1.8, screen keybinds act on the first-matching stack as a stopgap); no per-item inventory.

### File List

**Modified**
- `core/src/main/java/com/margins/rogue/RoguePlayer.java` — `skill` field + `getSkill()`/`setSkill()`.
- `core/src/main/java/com/margins/rogue/item/Supply.java` — 11 provision types + taxonomy (`isProvision`/`isRiskyProvision`/`drinkRisk`/`spoilsTo`/`cooksTo`/`filtersTo`/`boilsTo`); `isConsumedOnUse` excludes coal/salt.
- `core/src/main/java/com/margins/rogue/item/TrueIdentity.java` — provision nourishment identities.
- `core/src/main/java/com/margins/rogue/RogueTile.java` — `WELL`/`POND`/`RIVER` + `isWaterSource`.
- `core/src/main/java/com/margins/rogue/FloorGenerator.java` — stamp water sources at room centers (no RNG draw).
- `core/src/main/java/com/margins/rogue/state/RunState.java` — campfire tile + spoilage clock + API; `restart()` reset.
- `core/src/main/java/com/margins/rogue/system/PlayerAction.java` — `COLLECT`/`BUILD_CAMPFIRE`/`COOK`/`FILTER`/`BOIL` kinds + factories.
- `core/src/main/java/com/margins/rogue/system/TurnEngine.java` — new action cases, USE risky-provision routing, spoilage tick, `collectWater` helper.
- `core/src/main/java/com/margins/MarginScreen.java` — keybinds (C/B/K/F/V/E) + water-source tile colours.
- `core/src/test/java/com/margins/rogue/ContinuousMapTest.java` — allow water-source tiles in the AD-8 tile whitelist.
- `core/src/test/java/com/margins/rogue/state/RunStatePersistenceTest.java` — campfire/clock round-trip + pre-1.5 default.

**Added**
- `core/src/main/java/com/margins/rogue/system/ConsumptionSystem.java`, `CookingSystem.java`, `PurificationSystem.java`, `SpoilageSystem.java`, `SurvivalCraft.java`.
- `core/src/test/java/com/margins/rogue/PurificationTest.java`, `CookingTest.java`, `SpoilageTest.java`, `WaterRiskTest.java`, `WaterSourceTest.java`.

## Change Log

- 2026-08-07 — Story 1.5 created (food/water sourcing, two-step SKILL-based purification, food spoilage ladder, and a cooking/purification campfire station; FR-6). Built fully end-to-end per Justine's scope call; perishables modeled by exploding the `Supply` enum (stateful types, type-swap transitions, no per-item state). Seams held: campfire WARMTH + torch → Story 1.6; tiered debuffs → Story 1.7; world-structure water sources → Epic 3 (minimal scattered tiles here).
- 2026-08-07 — Implemented Story 1.5. SKILL stat; 11 provision types + data-driven taxonomy; `Purification`/`Cooking`/`Spoilage`/`Consumption` systems; water-source tiles (no-RNG placement); campfire fire-station (warmth deferred to 1.6); risky-consumption HP-harm with a 1.7 hook; 5 crafting actions wired into the acted path; minimal screen keybinds. 95 → 117 tests green.
- 2026-08-07 — Senior Developer Review complete: **Approve (6 patches applied, 1 deferred)**. See the review section below. 117 → 122 tests green.

## Senior Developer Review (AI)

**Date:** 2026-08-07 · **Outcome:** Approve (with patches applied)

Acceptance Auditor verified all ACs are genuinely met (SKILL-governed cook/filter rolls, filtration reduces but doesn't eliminate, boiling eliminates, spoilage ladder, risky-consumption risk) with each covered by a regression test — and that all five seams held (no warmth/torch drift into 1.6, no debuff/status system into 1.7, no world-structure gen into Epic 3, no HUD into 1.8, no per-item inventory into Epic 6), plus the six documented deviations were all judged reasonable and scope-contained. Suite green at 117/117 on review entry. Blind Hunter found one High (`IdentifyMap` stream shift), two Meds (salt phase, tautological test) and the global-clock spoilage-age model (Med, deferred as inherent to the chosen model). Edge Case Hunter independently converged on the salt phase, found the High-severity pre-1.5-save identity-binding gap, the "self-poison for nothing" full-consumption trap, and the `skillChance` negative clamp — and walked the undo-dances, coal-not-consumed-on-failed-boil, spoilage merge/overflow, `hurtRaw` ordering, `COLLECT` bounds, and water-stamping (player never starts on water) all clean.

### Review Findings

**Patches (applied this review):**
- [x] [Review][Patch] `IdentifyMap` stops drawing seeded RNG for single-identity supplies [core/.../state/IdentifyMap.java, .../state/IdentifyMapTest.java] — Blind Hunter (High): `build()` drew `rng.nextInt(opts.length)` for EVERY Supply, including the 11 new single-identity provisions — pointless draws that shift the seeded stream for every downstream consumer and made FloorGenerator's "byte-identical" claim only half-true. Now `opts.length > 1 ? opts[rng.nextInt(opts.length)] : opts[0]`. Added `singleIdentityTypesDoNotDrawFromTheRng` (a golden test: after `build()`, the RNG is exactly as if it drew once per ambiguous 2+-identity type — for a given seed, actor/supply placement is byte-identical again). FloorGenerator's comment corrected to say the placement stream is untouched.
- [x] [Review][Patch] A pre-1.5 save grows its identity binding for the appended ordinals [core/.../state/IdentifyMap.java, .../state/RunState.java, .../state/RunStatePersistenceTest.java] — Edge Case Hunter (High #1): a post-AD-8 pre-1.5 save serializes `boundByOrdinal` with the then-5 Supply values; `restoreAfterLoad()` didn't grow it, so `identityOf(newOrdinal)` → null → nourishment silently skipped while the item is consumed and its risk still rolled. Added `IdentifyMap.reconcile(int)` (grows + binds appended types to `possible[0]` — deterministic, no RNG draw, so the resumed stream stays aligned; idempotent for full saves), called from `restoreAfterLoad()`. Added a persistence regression test that truncates a real save's binding to the 5 pre-1.5 entries, loads, and asserts the appended ordinals bind (RAW_MEAT → RAW_MEAT_ID, COAL → COAL_ID, tail covered).
- [x] [Review][Patch] Salt spoilage is ACCRUED, not clock-modulo [core/.../system/SpoilageSystem.java, .../state/RunState.java, .../SpoilageTest.java, .../state/RunStatePersistenceTest.java] — Blind Hunter (Med M2) + Edge Case Hunter (#4, converged): the salt "double the interval" (`clock % (salt ? 100 : 50)`) let toggling salt around a boundary skip a stage or resume a shifted phase — a free-delay exploit. Now spoilage accrues +2 per unsalted turn, +1 per salted turn, threshold `2 * SPOIL_INTERVAL`, carried across advances (subtract, not reset). Unsalted cadence unchanged (50 turns), fully-salted 100, any toggle delays exactly the turns salt was held. Added `togglingSaltDoesNotShiftTheSpoilageCadence` (50 salted + 25 unsalted → exactly one advance, progress 0 after); `spoilageProgress` is persisted and round-trips.
- [x] [Review][Patch] A provision whose nourishment would be wasted is REFUSED [core/.../system/ConsumptionSystem.java, .../system/TurnEngine.java, .../item/Supply.java, .../RoguePlayer.java, .../WaterRiskTest.java] — Edge Case Hunter (Med #2): a HYDRATED/WELL_FED player consuming a risky provision spent the item and rolled the poison with zero benefit ("self-poison for nothing"). Added `RoguePlayer.canEat()/canDrink()` + `Supply.isWater()`; `ConsumptionSystem.consume` now refuses (no item, no turn, no roll) when the track is already maxed. This also consolidated ALL provisions (risky + safe) onto the one `ConsumptionSystem` path — the `USE` case routes every provision here (safe ones have `drinkRisk()==0`, so their roll is a no-op); mystery supplies keep the `identityOf().apply` path. Added refusal tests (drink-when-hydrated, eat-when-well-fed) + `aThirstyPlayerCanDrink`; the risk-rate helpers now drop the player to THIRSTY first so the rates still measure the roll.
- [x] [Review][Patch] `skillChance` clamps both ways [core/.../system/SurvivalCraft.java] — Edge Case Hunter (Med #3): `Math.min(95, 40 + skill*8)` left a negative SKILL producing a negative chance — and `nextInt(100) < negative` is ALWAYS false, i.e. a 0%-chance craft always succeeds. Now `Math.max(0, ...)`. Defensive (SKILL starts 5 and has no negative source yet).
- [x] [Review][Patch] Determinism test made meaningful [core/.../WaterRiskTest.java] — Blind Hunter (Med M3): `riskIsDeterministicPerSeed` compared a method to itself (a fresh `RunState(seed)` is deterministic by construction). Replaced with `riskOutcomeReproducesAfterSaveLoad`: two loads of the same save (taken right before the first risky consume) must roll the same outcome — exercising the `restoreAfterLoad()` re-seed contract (AD-5) that could actually break (e.g. an unseeded load path).

**Deferred (real, but out of Story 1.5 scope — see `deferred-work.md`):**
- [x] [Review][Defer] **Global-clock spoilage — fresh meat can rot within 1 turn of pickup** [core/.../system/SpoilageSystem.java] — Blind Hunter (Med M1): the batch model advances whole stacks on the run-global clock, so a Raw stack added at clock 49 advances at 50 (1 turn of "freshness"). Inherent to the no-per-item-state model Justine chose (enum-explosion) — a per-item age needs the object-based inventory (Epic 6). The accrual patch keeps the cadence honest for whatever is in the pack; the age gap is a documented model limitation, not a defect to patch here.
- [x] [Review][Defer] **Campfire/torch light-tile validation** [RunState.setLight / campfire] — carried from the 1.4 deferral. Story 1.5's `BUILD_CAMPFIRE` sets the fire on the player's OWN tile (always walkable + in-bounds by construction), so the campfire is safe; the remaining validation need is the Story 1.6 torch. Carry forward.

**Dismissed (checked, not issues):**
- Edge Case Hunter walked clean: cook/filter/boil undo-dances (remove+re-add on backpack-full — nothing lost), coal NOT consumed on a failed boil, spoilage RAW→HALF merge with an existing HALF stack (order-independent), spoilage `tryAdd` never drops food, no `nextInt(0)`, `hurtRaw` ordering, `COLLECT` bounds, FloorGenerator water-stamping (player starts at room 0, never on a water source).
- Consumption risk draws from `state.rng()` after the floor/identity draws — AD-5 deterministic; the H1 patch removes the wasteful draws so placement reproduces byte-identical per seed.
- L1–L3 (Blind Hunter lows): style/doc-level — folded into the comment corrections (FloorGenerator "byte-identical" wording, ConsumptionSystem javadoc) rather than itemized; no behavior changes.

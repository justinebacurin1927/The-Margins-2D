---
baseline_commit: 3f09f19
---

# Story 3.5: Horizontal progression — SKILL and knowledge

Status: done

## Story

As Klein,
I want to grow by knowing the forest rather than by kills,
So that mastery is horizontal (FR-11, SM-3).

## Acceptance Criteria

**AC-1 (FR-11, no combat-XP):** Given any kill, when it resolves, then no number, level, or XP rises from it — killing never raises a stat, a counter, or a level.

**AC-2 (FR-11, SKILL governs + knowledge accumulates):** Given repeated doing (cooking, purification, **lockpicking**), when SKILL is exercised, then SKILL-governed outcomes are better at higher SKILL, and accumulated knowledge (map fragments, mushroom/water safety, location dangers) **persists and is queryable**.

**AC-3 (SM-3, knowledge measurably helps):** Given two runs — one that uses knowledge (identified-safe water, an opened cellar, a read map fragment), one that does not — when both play, then the knowledgeable one is measurably better off (more usable provisions / longer survival) with **no XP or stat advantage** (SM-3).

## Scope decision (confirmed with Justine, 2026-08-10)

**Knowledge-centric reading of AC-2** (not a SKILL-growth/leveling mechanic). SKILL stays a **fixed governing attribute** — higher SKILL yields better outcomes (already true), but "repeated doing → outcomes improve" is realized through **accumulated, queryable knowledge**, not a rising number. This matches SM-3 ("grow by knowing, not by kills") and the epic's "no numbers rise from grinding" ethos. **No SKILL-growth-from-practice mechanic is built** (that would be a form of XP-by-doing, cutting against AC-1's spirit). The two pieces the codebase explicitly deferred to 3.5 — **lockpicking** and the **map-fragment knowledge query** — are the concrete build.

## Baseline (what the substrate already ships)

The `3f09f19` baseline already provides most of the horizontal-progression frame:

- **No XP / level / combat-XP system exists anywhere** (verified: no `xp`/`experience`/`level` in `core/src/main`). AC-1 is **true by construction** — `CombatSystem` mutates only HP; killing raises no stat or counter. AC-1 is a **pin**, not a build.
- **SKILL exists and is the horizontal-growth axis** (`RoguePlayer.skill`, `getSkill()`/`setSkill()`, field-initialized to 5). It already **governs** cooking and purification via `SurvivalCraft.skillChance(state) = clamp(40 + skill*8, 0, 95)` (`CookingSystem`, `PurificationSystem`, Story 1.5). Lockpicking reuses this SKILL-check shape.
- **Item-safety knowledge already persists and is queryable** — `IdentifyMap` (per-seed Supply→TrueIdentity binding, FR-11/FR-12, Story 1.5): `isIdentified(ord)`, `identityOf(ord)`, `displayNameFor(ord)`, `markIdentified(ord)`. It is a **persisted** `RunState` field (survives save/load, round-trip-tested). "Mushroom/water safety" knowledge = identify state — **already done**.
- **The Old House locked cellar is authored data awaiting the SKILL hook.** `StructureTable.Structure.lockedLoot` holds the Old House's rich cellar loot (3 `PRESERVED_FOOD` + 2 `FOLDED_CLOTH`); `placeStructureLoot` places only `structure.loot`, **never `lockedLoot`** — the cellar is unreachable. `RunState:224` and `StructureTable:142` both say *"Story 3.5's lockpicking exposes it."* This is 3.5's concrete SKILL-governed action.
- **The map fragment is the inert knowledge collectible awaiting its query.** `Supply.MAP_FRAGMENT` (`Supply.java:57`: "the inert knowledge collectible (the query is Story 3.5)") and `TrueIdentity.MAP_FRAGMENT_ID.apply` (`TrueIdentity.java:67`: "knowledge collectible — inert; query is 3.5") are deliberately no-ops today. 3.5 makes reading/collecting one record **queryable knowledge**.
- **Location-danger data exists** (Story 3.4): `HazardSystem.nightHazardFor` + `StructureTable` encode which locations flip dangerous at night. "Location dangers" as knowledge is a **query over existing data**, not new state.
- **FlagStore** (`RunState.getFlagStore()`, AD-7) — the run-scoped persisted key/value store for booleans/counters (`get(key)`/`set(key,val)`). The established way to persist run state **without a new `RunState` field** (used by quests, Story 2.5). The lockpicking "cellar opened" state and map-fragment knowledge ride this.
- **SurvivalCraft** (`skillChance`) is package-private in `com.margins.rogue.system` — lockpicking's SKILL roll lives in that package (a `LockpickSystem` or the `TurnEngine` action handler) and reuses/mirrors it.

**What the baseline does NOT have — Story 3.5's actual scope:**

- **No lockpicking.** No `PlayerAction` opens the cellar; `lockedLoot` is unreachable.
- **No knowledge query for map fragments / location dangers.** The fragment is inert; nothing records or answers "what do I know?"
- **No AC-1 / AC-3 pins.** The no-XP invariant and the "knowledge measurably helps" success metric are unverified.

## In/Out of Scope Seam

**IN:**
- **SKILL-governed lockpicking** (AC-2): a `PlayerAction` that, at the Old House cellar (an `STRUCTURE_OLD_HOUSE` cell) with the right tool, rolls against SKILL to open the cellar — exposing `lockedLoot` — with higher SKILL succeeding more often. The opened state **persists** (FlagStore) so a reload neither re-locks nor double-places.
- **Queryable, persistent knowledge** (AC-2): reading/collecting a `MAP_FRAGMENT` records a **persisted, queryable** knowledge fact; a query surface answers what Klein knows — unifying item-safety (existing `IdentifyMap`), map-fragment knowledge, and location-danger (a query over Story 3.4 data). AD-6-safe (FlagStore-backed or a field-initialized store).
- **AC-1 pin** (no combat-XP): a test proving a kill raises no stat/counter/level (by construction).
- **AC-3 pin** (SM-3): a comparative test — a knowledge-using run ends measurably better off (usable provisions / survival) than a naive one, with no stat/number rising.
- Content wiring + AC pins + full-suite/boot verification.

**OUT (later stories / already shipped):**
- **SKILL growth / leveling from practice** — explicitly OUT (scope decision above). SKILL is fixed; no number rises from doing.
- **Gear-with-memory / weapon durability / SKILL-based repair** (FR-13/AD-13) — **Epic 4** (Stories 4.4/4.5). FR-11 lists "repair" under SKILL, but the durability system is Epic 4; 3.5 builds **no** durability/repair.
- **Combat itself** (the kill that AC-1 pins) — `CombatSystem` exists (brownfield); 3.5 does not change combat, only pins that it grants no XP.
- **The Sense / "Ant" path and the Buried Truth branch** — later What-if additions (PRD note); not canonical, not 3.5.
- **A full journal/knowledge UI** — the knowledge must be **queryable (a core API)**; a rich screen is presentation polish, not an AC. A minimal HUD/log surface is acceptable but not required.
- **Deep map-reveal from fragments** (fog-of-war map unlock) — 3.5's fragment knowledge is a recorded, queryable fact, not a cartography system. Reuse existing tiles/FOV; no new map-reveal mechanic.
- Currency, traders, recipes-as-a-system — Epic 6 / not required (cooking already works without a recipe store).
- No new `RogueTile`; no new persisted `RunState` field if FlagStore/a field-initialized store suffices (AD-6).

## Design Decisions (the interpretation calls)

1. **AC-1 is a pin, not a build.** There is no XP/level system; `CombatSystem` only touches HP. The test asserts a resolved kill leaves every player number (STR/GRIT/INS/VOICE/SKILL, max-HP) unchanged and that no kill-count/level/XP field exists — by construction. This guards against a future regression that sneaks a counter in.
2. **Lockpicking = a SKILL roll that exposes `lockedLoot`, gated on a tool, persisted via FlagStore.** A new `PlayerAction` (the `COLLECT`/`BUILD_CAMPFIRE` precedent — no itemType). It acts when Klein stands on an `STRUCTURE_OLD_HOUSE` cell and the cellar is not yet open, and he carries the lockpicking tool (`SMALL_TOOLS` — the thematic lock tool, already a loot type). One seeded roll (AD-5) scaled by SKILL (mirror `SurvivalCraft.skillChance`, or a lockpick-specific curve); on success, place `lockedLoot` in the Old House footprint (the `placeStructureLoot` pattern) and set a FlagStore "cellar opened" key; on failure, spend the turn with a message (no tool destruction — durability is Epic 4). A refused attempt (not at the cellar / no tool / already open) commits no turn (the inert-USE precedent). **Persisted opened-state (FlagStore) so reload never re-locks or double-places** (the 3.2 `structureLootPlaced` lesson, in FlagStore form — no new `RunState` field, AD-6-safe).
3. **Knowledge is persistent + queryable, backed by existing stores (AD-6-safe).** Item-safety stays `IdentifyMap` (done). Map-fragment knowledge is recorded on read (`TrueIdentity.MAP_FRAGMENT_ID` / the `USE` path) into a **persisted** store — a FlagStore key (count/boolean) or a field-initialized knowledge object (non-null empty default, like `inventory`/`flagStore`). A small **query API** answers "what does Klein know?" — composing item-safety, map-fragment knowledge, and location-danger. **Location-danger knowledge is a pure query over Story 3.4 data** (`nightHazardFor`/`StructureTable`) — no new persisted state. No new `RogueTile`, no new noise, no extra clock tick.
4. **Reading a map fragment is narration, not a turn (the Torn Page precedent).** Story 2.4's `TORN_PAGE` `USE` reveals lore and commits **no turn** and is never consumed. The map fragment mirrors this: reading it records queryable knowledge (persisted), commits no turn, and (decision) is **not consumed** — it stays a re-readable knowledge token. (If a fragment should be consumed, that is a tunable content call; default = not consumed, matching the Torn Page.)
5. **AC-3 (SM-3) is a comparative survival pin, not a balance guarantee.** Two runs from the same seed: a "knowing" run (drinks only identified-safe water, opens the cellar via SKILL for extra provisions, reads the map fragment) vs a "naive" run (drinks unidentified/risky water, never opens the cellar). Assert the knowing run ends **measurably better off** — more usable provisions and/or longer survival — and that **no stat/number rose** in either (no XP). Deterministic-on-seed, turn-level; it demonstrates the systems *reward knowledge*, not that a number grew.

## Tasks / Subtasks

- [x] **Task 1 — AC-1: the no-combat-XP pin (AC: 1)**
  - [x] A test that resolves a kill through `CombatSystem` and asserts NO player number rises: STR/GRIT/INSTINCT/VOICE/SKILL and max-HP are unchanged after the kill, and there is no kill-count / level / XP field or counter anywhere on the player or run (by construction). Guards against a future regression adding one.

- [x] **Task 2 — SKILL-governed lockpicking exposes the Old House cellar (AC: 2)**
  - [x] A `PlayerAction` (LOCKPICK, the `COLLECT`/`BUILD_CAMPFIRE` precedent — no itemType) + its `TurnEngine`/`LockpickSystem` handler: acts only when Klein is on an `STRUCTURE_OLD_HOUSE` cell, the cellar is not yet open (FlagStore key), and he carries `SMALL_TOOLS`. One seeded SKILL-scaled roll (AD-5) — higher SKILL opens more often; on success expose `lockedLoot` (place it in the Old House footprint, the `placeStructureLoot` pattern) and set the FlagStore opened-key; on failure spend the turn with a message. A refused attempt (wrong place / no tool / already open) commits no turn.
  - [x] Persisted opened-state via FlagStore (no new `RunState` field, AD-6): a reload neither re-locks nor double-places the cellar loot.
  - [x] Tests: higher SKILL opens the cellar more often across a seed range; on success `lockedLoot` is reachable in the footprint; the opened flag persists across save/load (no re-lock, no double loot); a refused attempt (no tool / already open / not at the Old House) commits no turn; one seeded draw, deterministic per seed (AD-5).

- [x] **Task 3 — Queryable, persistent knowledge (AC: 2)**
  - [x] Reading/collecting a `MAP_FRAGMENT` records a **persisted** knowledge fact (FlagStore key or a field-initialized store; AD-6-safe). Wire `TrueIdentity.MAP_FRAGMENT_ID` / the `USE` path (the Torn Page precedent: narration, no turn, not consumed — Decision 4).
  - [x] A **query API** answers what Klein knows — composing item-safety (existing `IdentifyMap.isIdentified`), map-fragment knowledge (new), and location-danger (a pure query over Story 3.4 `nightHazardFor`/`StructureTable`). Core-owned (AD-1/AD-2).
  - [x] Tests: reading a fragment records queryable knowledge; the knowledge persists across save/load (round-trip); the location-danger query answers correctly (e.g. the Graveyard/Sunken Well/Poacher's Camp are night-dangerous, the Beehive is a night-safer exception); item-safety knowledge stays queryable via `IdentifyMap` (regression-safe). No new `RogueTile`, no new noise, no extra clock tick.

- [x] **Task 4 — AC-3: the SM-3 knowledge-survives pin (AC: 3)**
  - [x] A comparative test from one seed: a "knowing" run (drinks only identified-safe water, opens the cellar via SKILL, reads the map fragment) vs a "naive" run (risky water, cellar unopened). Assert the knowing run is measurably better off — more usable provisions and/or longer survival — and that **no player stat/number rose** in either run (no XP; SKILL and all stats unchanged from start). Deterministic-on-seed, turn-level.

- [x] **Task 5 — AC pins + no-regression + boot (AC: all)**
  - [x] AC-1 pin: Task 1. AC-2 pins: Tasks 2-3 (lockpicking + knowledge query). AC-3 pin: Task 4.
  - [x] Scope guard: NO SKILL-growth/leveling, NO durability/repair (Epic 4), NO combat change, NO Sense/Ant, NO new `RogueTile`, NO new persisted `RunState` field (FlagStore/field-initialized store only), NO new noise, NO extra clock tick — each pinned by a test or by construction.
  - [x] Full suite: `mvn -o -pl core test` — the **385** 3.4-post-review tests stay green, plus the new SKILL/knowledge tests. No regressions.
  - [x] Launch: `mvn -o -q -pl core install` + `timeout 40 mvn -o -pl desktop exec:java` — boot clean.

### Review Findings

_Adversarial code review 2026-08-10 (Blind Hunter + Edge Case Hunter + Acceptance Auditor, all Opus). Auditor verdict: **all four ACs satisfied at the core level** — no spec-constraint violations (no SKILL-growth, no durability/repair, no combat change, no new RogueTile, no new persisted RunState field, no noise, no extra clock tick on a read; the `placeStructureLoot` → `placeLootInFootprint` refactor is behavior-preserving, `nightFlipFor` extraction identical). 4 patches applied, 2 deferred, 4 dismissed. Severities are the triage's final call._

- [x] [Review][Patch] Bind a LOCKPICK key so AC-2's SKILL is player-exercisable (High) [`core/src/main/java/com/margins/MarginScreen.java`] — the core LOCKPICK action existed but no screen input produced it, so the cellar was only test-reachable. Raised independently by blind + edge; the auditor's residual [Med] "lockpicking not player-reachable in-game" is this same gap. **Fixed:** `L` in `readAction` (the inert-USE precedent — a refused pick commits no turn) + `L  Lockpick` in the how-to-play SURVIVE column. Boot + 398 tests verified.
- [x] [Review][Patch] Open the E-gate for the MAP_FRAGMENT read (Med) [`core/src/main/java/com/margins/MarginScreen.java`] — `canUseFromInventory` admitted only provisions, consumed-on-use and the Torn Page, so a fragment could never be read from the world-E or backpack-E path. **Fixed:** MAP_FRAGMENT admitted (both gates) + `inventoryPrimaryAction` returns `E  READ` — the Torn Page read-narration precedent (no turn, never consumed).
- [x] [Review][Patch] Diff the under-player cellar-loot check against a pre-lockpick snapshot (Low) [`core/src/test/java/com/margins/rogue/system/HorizontalProgressionTest.java`] — the avoid-rule assertion assumed nothing under the player, but generation loot may legitimately sit on the lockpick cell. **Fixed:** snapshot `underPlayerBefore`; assert the player tile is unchanged by the pick (generation loot, if any, preserved).
- [x] [Review][Patch] Pin the SKILL clamp deterministically instead of a stream-count ceiling (Low) [`core/src/test/java/com/margins/rogue/system/HorizontalProgressionTest.java`] — `high < 80` could flake if a future refactor shifts the rng stream (AD-5). **Fixed:** assert `SurvivalCraft.skillChance` clamps at 95 (the "high SKILL is a chance, not a guarantee" property) in place of the count guard.
- [x] [Review][Defer] The "unifying query surface" is three APIs, not one call (Low) — `KnowledgeSystem` exposes map-fragment + location-danger; item-safety stays on `IdentifyMap`, so "what do I know?" composes three APIs (the tests compose them). AC-2's queryable requirement is met individually; a single `whatDoIKnow(state)` is future UI work, not a spec violation. Deferred — no consumer yet.
- [x] [Review][Defer] AC-3's map-fragment knowledge isn't independently demonstrated to help (Low) — in the comparative test the fragment is read but the HP/provision delta is driven by water-identification + the cellar. The fragment's knowledge is foundation state with no consuming mechanic yet (later stories); a benefit test waits on that consumer. Deferred.
- Dismissed (4): the night-only `locationNightHazard` (consistent with the spec's night seam), SMALL_TOOLS equipped-slot / MAP_FRAGMENT identify visibility (no current consumer), LockpickSystem's success message on an empty-footprint placement (unreachable on generated maps), and all verified-sound items (precondition order per Decision 2, AC-3 probe-vs-run rng alignment, `nightFlipFor` extraction, `placeLootInFootprint` byte-identity).

## Dev Notes

### Current state (what exists, to preserve)

- **`RoguePlayer`** — `skill` (field-init 5), `getSkill()`/`setSkill(int)`. Stats STR/INSTINCT/GRIT/VOICE + SKILL (no AG in the prototype — AD-18 note). `hurtRaw`, `takeDamage`, HP. No XP/level. AC-1 pins that a kill changes none of these.
- **`SurvivalCraft.skillChance(state)`** (`system`, package-private) = `clamp(40 + skill*8, 0, 95)`. Cooking/purification use it. Lockpicking mirrors this SKILL curve (or defines a lockpick-specific one — tunable, PRD §8).
- **`CookingSystem` / `PurificationSystem`** — the existing SKILL-governed crafts (Story 1.5). Do NOT change their balance; 3.5 adds lockpicking alongside, not a rework.
- **`IdentifyMap`** (persisted `RunState` field) — `isIdentified`/`identityOf`/`markIdentified`/`displayNameFor`/`reconcile`. The item-safety knowledge store; the knowledge query composes it. Round-trip-tested (`IdentifyMapTest`, `RunStatePersistenceTest`). Do NOT break its persistence.
- **`StructureTable.Structure.lockedLoot`** — the Old House cellar loot (`OLD_HOUSE` entry: `{PRESERVED_FOOD×3, FOLDED_CLOTH×2}`); `NO_CELLAR` (empty) for every other structure. `placeStructureLoot` places only `structure.loot`. Lockpicking places `lockedLoot` on success — reuse the footprint-cell placement in `RunState.placeStructureLoot` (walkable footprint cells, avoid the player tile).
- **`RunState.placeStructureLoot(avoidX, avoidY)`** + `structureFootprint(m, type)` — the authored-loot placement pattern (walkable footprint cells, one seeded draw per placed item). Lockpicking's success path mirrors it for `lockedLoot`.
- **`Supply.MAP_FRAGMENT`** (non-scatterable; authored loot at Hunter's Blind 20% / Watchtower 25% / Deep Cave 30%) + **`TrueIdentity.MAP_FRAGMENT_ID`** (inert `apply`). Reading it (the `USE` path in `TurnEngine`, the `TORN_PAGE` precedent at `TurnEngine.java:107-127`) records knowledge in 3.5.
- **`FlagStore`** (`RunState.getFlagStore()`, persisted, AD-7) — `get(key)`/`set(key,val)`. The cellar-opened state + map-fragment knowledge ride this (no new `RunState` field). Namespacing precedent: `JournalController.startedKey(...)`.
- **`PlayerAction`** (`system`) — `Kind` enum + static factories; `COLLECT`/`BUILD_CAMPFIRE`/`CRAFT_TORCH` are the no-itemType action precedent for `LOCKPICK`. `TurnEngine.advance` switch handles each; a refused action returns `acted=false` (no turn) — mirror for a refused lockpick.
- **`HazardSystem.nightHazardFor` / `StructureTable`** (Story 3.4) — the location-danger data the knowledge query reads (pure query; no new state).
- **`TurnEngine.advance`** — the acted pipeline (AD-4). Lockpicking is a player action in the switch (like `COLLECT`); on success it commits a turn and runs the survival pipeline. Reading a map fragment commits NO turn (Torn Page precedent).

### Carried lessons (1.5/2.4/2.5/3.2/3.4, applied)

- **The Torn Page precedent (Story 2.4/2.5)** — a `USE` that reveals/records and commits no turn, never consumed; the `FlagStore` discovery-trigger pattern (`TurnEngine.java:107-127`). The map-fragment knowledge follows it exactly.
- **Persisted opened/placed state must be a real flag (Story 3.2 `structureLootPlaced`)** — the cellar-opened state persists (FlagStore) so a reload never re-locks or double-places. The 3.2 backfill/double-place bug is the lesson; use a flag, not a re-derivation.
- **One seeded rng draw per event (AD-5)** — the lockpick roll is exactly one `nextInt` scaled by SKILL, like `SurvivalCraft`/`HazardSystem`. Do NOT perturb the generic scatter / `placeStructureLoot` seeded stream (the 3.2 `seedFortyTwoFloorLayoutIsByteIdentical` pin stays green — lockpicking places `lockedLoot` at *runtime on success*, not at generation, so the generation stream is untouched).
- **Derived/queryable-not-duplicated (AD-6, WorldSpine/DayPhase precedent)** — location-danger knowledge is a pure query over 3.4 data, not a copied store. Item-safety stays in `IdentifyMap`. Only the genuinely new fact (map-fragment read, cellar opened) is persisted, via FlagStore.
- **Refused action = no turn (AD-5 honesty)** — a lockpick with no tool / not at the cellar / already open commits no turn and emits feedback (the inert-USE / `craftTorch`-refused precedent).
- **No new persisted `RunState` field if FlagStore suffices (AD-6 field-absent rule)** — a new `RunState` field inherits nondeterministic ctor state on a field-absent load; FlagStore (already persisted, field-initialized) avoids the wart. If a field-initialized knowledge object is used instead, it must default non-null-empty (like `inventory`/`flagStore`).
- **Read map/structure data dynamically** — `getStructureType`, `STRUCTURE_OLD_HOUSE`, `structureFootprint`; never hard-code coordinates.

### Scope discipline (CLAUDE.md §2/§3)

- Minimum code: the LOCKPICK action + SKILL roll + cellar-open FlagStore flag + `lockedLoot` placement; the map-fragment knowledge record + the query API (composing IdentifyMap + map-fragment + 3.4 location-danger); the AC-1 / AC-3 pins; tests. **No** SKILL growth, **no** durability/repair (Epic 4), **no** combat change, **no** map-reveal cartography, **no** knowledge UI screen, **no** new tile/persisted-RunState-field/noise/clock-tick.
- Do NOT change cooking/purification balance, `IdentifyMap`'s persistence, `placeStructureLoot`'s generation stream, the pre-AD-8 reject gate, or the `Supply.count()-5` scatter pin (3.5 adds no scatterable item — `SMALL_TOOLS`/`MAP_FRAGMENT` already exist and are non-scatterable).
- Lockpicking exposes `lockedLoot` only via the SKILL roll — never at generation (keep the cellar genuinely locked until picked).

### Testing standards

- JUnit 5 headless core; `new RunState(seedL)`. Pin SKILL with `setSkill(...)` (the established test hook) to isolate the lockpick curve (low vs high SKILL open-rate across a seed range). Drive turns via `TurnEngine`/`PlayerAction`. Place at the Old House via `getStructureType == STRUCTURE_OLD_HOUSE` (the `StructureContentTest`/`NightWeatherHazardTest` cell-finding helpers). Round-trip via the shared `json()` helper (mirrors `SaveService`).
- Deterministic-on-fixed-seed, turn-level assertions (open-rate across seeds; knowledge persists round-trip; the comparative survival delta). Not log-line-only checks.
- Run: `mvn -o -pl core test` (offline). Baseline **385 green** (3.4 post-review); the story adds the SKILL/knowledge tests. Launch: `mvn -o -q -pl core install` + `timeout 40 mvn -o -pl desktop exec:java` (exit 143/124 = clean boot).

### References

- [Source: epics.md#Story-3.5 (482-)] — the three ACs verbatim (no combat-XP; SKILL exercised + knowledge queryable; SM-3 knowledge survives longer).
- [Source: prds/prd-The-Margin-2026-08-06/prd.md#FR-11] — horizontal progression: "No combat-XP", "SKILL (doing) governs cooking, purification, repair, lockpicking; knowledge (map fragments, mushroom/water safety, recipes, location dangers) accumulates", "grow by knowing the forest, not by kill count". (Repair = FR-13/Epic 4, OUT.)
- [Source: prds/prd-The-Margin-2026-08-06/prd.md#UJ-2 / SM-3] — the day is the planning unit; the knowledgeable player survives longer with no XP.
- [Source: architecture/ARCHITECTURE-SPINE.md#AD-5] — one seeded roll per event (the lockpick roll).
- [Source: architecture/ARCHITECTURE-SPINE.md#AD-6] — no new persisted field where FlagStore/a field-init store suffices; the field-absent migration rule.
- [Source: architecture/ARCHITECTURE-SPINE.md#AD-7] — FlagStore is the run-scoped persisted narrative/flag store (the cellar-opened + map-fragment knowledge keys).
- [Source: architecture/ARCHITECTURE-SPINE.md#AD-13] — gear-with-memory / SKILL-based repair — **Epic 4**, explicitly OUT of 3.5.
- [Source: story-3.4 (predecessor, Status: done)] — the location-danger data (`nightHazardFor`/`StructureTable`) the knowledge query reads.
- [Source: story-3.2 (Status: done)] — `StructureTable.lockedLoot` (the Old House cellar, "Story 3.5's lockpicking exposes it"); `placeStructureLoot`/`structureFootprint`; the `structureLootPlaced` persisted-flag lesson.
- [Source: story-2.4/2.5 (Status: done)] — the Torn Page `USE` precedent: reveal/record, no turn, never consumed; the `FlagStore` discovery trigger (`TurnEngine.java:107-127`).
- [Source: story-1.5 (Status: done)] — `SurvivalCraft.skillChance` (the SKILL curve), `IdentifyMap` (the item-safety knowledge store), cooking/purification.
- [Source: code — Supply.java:57, TrueIdentity.java:67, RunState.java:224, StructureTable.java:142] — the four in-code markers that name Story 3.5 as the owner of the map-fragment query and the cellar lockpicking.

## Dev Agent Record

### Agent Model Used

Claude Opus 4.8 (1M context), the session's model.

### Debug Log References

- RED: `HorizontalProgressionTest` failed to compile before implementation (LOCKPICK / LockpickSystem / KnowledgeSystem absent) — the red phase.
- GREEN: full core suite 398 tests green (385 3.4-post-review + 13 new), 0 failures.
- Launch: `mvn -o -q -pl core install` + `timeout 40 mvn -o -pl desktop exec:java` — clean boot (exit 143 = timeout kill, no exceptions).

### Completion Notes List

- AC-1 pin (Task 1): a two-swing kill through CombatSystem leaves STR/GRIT/INSTINCT/VOICE/SKILL and max-HP unchanged, plus a reflection scan proving no xp/level/experience/kill field exists on RoguePlayer or RunState (the `skill` field is explicitly excluded from the kill-scan — it is the horizontal axis, not an XP counter).
- SKILL lockpicking (Task 2): `PlayerAction.LOCKPICK` + `LockpickSystem` — one seeded roll at the existing `SurvivalCraft.skillChance` curve (reuse, not a new curve), gated on standing at the Old House + carrying `SMALL_TOOLS` + cellar not open; success places `lockedLoot` (3 PRESERVED_FOOD + 2 FOLDED_CLOTH) in the Old House footprint via the new `RunState.placeLockedCellarLoot` and sets the FlagStore `old-house.cellar-opened` key; failure spends the turn; refused commits no turn. `placeStructureLoot` was refactored to a shared private `placeLootInFootprint` (byte-identical seed-42 layout preserved — StructureContentTest green).
- Knowledge query (Task 3): reading a MAP_FRAGMENT records `knowledge.map-fragments-read` (FlagStore) — narration, no turn, never consumed (Torn Page precedent); `KnowledgeSystem` answers mapFragmentsRead + locationNightHazard (a pure query over the extracted `HazardSystem.nightFlipFor`), composing the existing IdentifyMap item-safety store.
- AC-3 pin (Task 4): a comparative test from one seed (a tainted-waterskin seed where the cellar opens within the cap) — the knowing run (drinks only identified-safe water, reads the fragment, opens the cellar) ends with strictly more HP and more usable provisions than the naive run (drinks all risky water blindly), with NO stat/number rising in either.
- Scope guard (Task 5): lockpicking adds no new persisted RunState field (FlagStore-backed — json carries no mapFragmentsRead/cellarOpened/lockedCellar), no SKILL growth from doing, no noise, no extra clock tick on a read, Small Tools not consumed (durability is Epic 4).

### File List

- core/src/main/java/com/margins/rogue/system/PlayerAction.java — added `LOCKPICK` Kind + `lockpick(int)` factory.
- core/src/main/java/com/margins/rogue/system/LockpickSystem.java — NEW: the SKILL-governed cellar roll + FlagStore opened-state + preconditions.
- core/src/main/java/com/margins/rogue/system/KnowledgeSystem.java — NEW: the queryable knowledge surface (mapFragmentsRead + locationNightHazard).
- core/src/main/java/com/margins/rogue/state/RunState.java — extracted `placeLootInFootprint` (shared), added `placeLockedCellarLoot` (runtime cellar-open placement).
- core/src/main/java/com/margins/rogue/system/HazardSystem.java — extracted `nightFlipFor` (clock-independent night-flip mapping) used by `nightHazardFor` + the knowledge query.
- core/src/main/java/com/margins/rogue/system/TurnEngine.java — added the `LOCKPICK` case + the `MAP_FRAGMENT` read branch.
- core/src/test/java/com/margins/rogue/system/HorizontalProgressionTest.java — NEW: 13 tests (AC-1 kill pin + reflection, lockpick curve/persistence/refusal/determinism, knowledge record/round-trip/location-danger/IdentifyMap, AC-3 comparative, scope guard).

## Change Log

- 2026-08-10: Created Story 3.5 — Horizontal progression: SKILL and knowledge (FR-11, SM-3). Knowledge-centric scope (confirmed): SKILL-governed lockpicking (opens the Old House cellar), the map-fragment/location-danger knowledge query (persistent + queryable), the no-combat-XP pin, and the SM-3 knowledge-survives pin. SKILL-growth/leveling and durability/repair (Epic 4) explicitly OUT. Status backlog → ready-for-dev. Sprint status updated.
- 2026-08-10: Dev-story implemented — `LOCKPICK` action + `LockpickSystem` (SKILL-curve roll, FlagStore opened-state, `placeLockedCellarLoot`), `KnowledgeSystem` (map-fragment count + location-danger query via extracted `HazardSystem.nightFlipFor`), the AC-1/AC-3 pins, and the scope guard. Full suite 398 tests green (385 + 13 new); boot clean. Status ready-for-dev → in-progress → review. Sprint status updated.
- 2026-08-10: Code review (Blind + Edge + Auditor, all Opus) — all four ACs satisfied; 4 patches applied: the L lockpick binding + how-to-play legend (AC-2 player-reachability, the reviewers' High/Med), the MAP_FRAGMENT E-read gate (`canUseFromInventory` + `E  READ` label), the under-player cellar-loot snapshot diff, and the deterministic SKILL-clamp pin. 2 Lows deferred (unifying query surface; fragment-benefit test), 4 dismissed. 398 tests green; boot clean. Status review → done (Epic 3's last story). Sprint status updated.

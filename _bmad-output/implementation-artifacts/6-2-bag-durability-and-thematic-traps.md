---
baseline_commit: 150e962
---

# Story 6.2: Bag durability and thematic traps

Status: review

## Story

As Klein,
I want bags to wear and some to be trapped,
so that storage itself carries risk (FR-20).

## Acceptance Criteria

**AC-1 — Bag durability tracks like gear.**
**Given** a bag with durability **When** it is used/damaged **Then** its durability tracks like any gear (wears down, and at 0 the bag breaks and is removed).

**AC-2 — Thematic traps break the bag and cost contents.**
**Given** a trapped bag (dart/fire/freeze) **When** the trap fires **Then** the bag breaks and **75%** of its contents drop (recoverable), **25%** are lost.

## Scope decisions (author, 2026-08-15 — running the loop autonomously per Justine)

- **D1 — A bag becomes a first-class instance (mirror `Weapon`; AD-13/D1).** Durability + a trap are mutable per-item state, which the flyweight `int`-ordinal store cannot carry — exactly the reason Story 4.4 moved weapons out of `Inventory` into a first-class `Weapon` list. Introduce a headless `item/Bag` model (`Supply` type + `maxDurability`/`durability` + `BagTrap` + derived `broken`), and **replace 6.1's `Inventory.int[] storageItems` with `Inventory.List<Bag> storageBags`**. `mainSlotCapacity()` sums `bag.slotBonus()` over the non-broken readied bags. Stays headless (AD-1/AD-2), serializes under the `RunState`→`Inventory` save root (AD-6), and `SaveService` registers the element type (as it does for `weapons`). This keeps the capacity math in `Inventory` (AD-1) and is the architecturally consistent path.
- **D2 — Durability mirrors `Weapon` (AC-1); wear on combat hits taken.** `Bag` reuses the gear shape: `decay(amount)`, `isBroken()` (`durability <= 0`), `getDurability`/`getMaxDurability`. **Wear trigger (minimal, faithful):** when Klein takes a combat hit, the first non-broken readied bag decays `BAG_DECAY_PER_HIT` (the pack is battered in the fight) — "used/damaged" without inventing a new interaction. At 0 the bag breaks (D4 spill, no loss). **Bag repair is deferred** (the gear-with-memory repair curve + `RepairSystem` wiring is weapon-specific; a bag-repair entry point is a later pass) — 6.2 ships wear→break only.
- **D3 — Trap model + fire-on-ready (AC-2).** New `item/BagTrap { NONE, DART, FIRE, FREEZE }`. A world-found bag MAY be trapped (set at placement, D5); its trap is hidden until it fires. **The trap fires the first time the bag is readied** (`Inventory.equip` of a trapped bag) — you strap on an unknown pack and it springs, mirroring how `HazardSystem` fires a hazard `onStep`. Effects reuse existing player harm (no new damage system): DART → `RoguePlayer.hurtRaw`; FIRE → `adjustTemperature(+)` + a small `hurtRaw` burn; FREEZE → `adjustTemperature(-)`. The trap then breaks the bag with loss (D4, AC-2). The Story-6.1 starting Traveler's Pack is **untrapped, full durability** (`seedStartingBag`).
- **D4 — Break → spill; capacity-shrink handling (AC-2 + closes a 6.1-deferred invariant).** Breaking a bag removes its `slotBonus`, so `mainSlotCapacity()` shrinks. Any main-store stacks now at indices **≥ the new capacity** are the overflow and must **spill** to Klein's tile via `RunState.addFloorItem` (drop-or-leave, FR-9): a **durability-break spills 100% (all recoverable)**; a **trap-break spills 75% (recoverable) and destroys 25% (lost)** per AC-2. This is the FIRST capacity-shrink path in the game and is exactly the "physical-store scan vs. `mainSlotCapacity()` invariant" the 6.1 review deferred to this story — the spill is its guard. Breaking is also the bag removal path the 6.1 review noted was missing.
- **D5 — Minimal world placement (found bags, some trapped) + bounded scope.** Place a small number of bags in `StructureTable` loot so bags are genuinely found (following through on the 6.1 reachability decision that "further bags are found in the world with Story 6.2"), a subset trapped. **AD-5:** append the bag `LootEntry` LAST in the chosen structure(s) and draw it after all existing placement so every prior seed layout stays byte-identical (the Epic-5 retro "append new seeded draws LAST" rule); update the determinism snapshot tests to the new authored count. **Deferred (→ later / polish):** bag **repair** (reuse the gear curve later); voluntary bag **un-readying** (breaking is the only removal in 6.2); the Quick-Access / artifact **UI exposure** (still the deferred 6.1 bespoke-loadout work); a full bag **catalog/tiers**; **currency** (6.3) and **traders** (6.4).

## Baseline (verify before adding)

- **`item/Inventory` (Story 6.1)** — the hybrid store: `MAIN_BASE_SLOTS = 19` main stacks physically sized to `MAX_MAIN_SLOTS = 39`, Quick-Access `equipped`(5 gear)/`quickArtifact`(3), and **`int[] storageItems`** (readied bag ordinals) → `mainSlotCapacity() = 19 + Σ Supply.storageSlotBonus()`, clamped to `MAX_MAIN_SLOTS`. `equip(type)` routes storage bags via `slotsFor`; `restoreAfterLoad()` grows the arrays (AD-6). **6.2 replaces `int[] storageItems` with `List<Bag> storageBags`** — preserve `mainSlotCapacity()`, `storageItemCount()`, `isBackpackFull()`, and the `equip` storage routing so 6.1's consumers/tests keep working.
- **`item/Weapon` (Story 4.4, AD-13)** — the first-class durability model to MIRROR: `maxDurability`/`durability`/`decay(amount)`/`isBroken()`, no-arg ctor + plain fields for libGDX Json, lives in a `RunState List<Weapon>`. `Bag` follows this shape (no repair curve in 6.2).
- **`RunState`** — `List<Weapon> weapons` + `SaveService.setElementType(RunState.class,"weapons",Weapon.class)` is the registration precedent (the `storageBags` list nests inside `Inventory`, so register on `Inventory.class`). `addFloorItem(int type,int count,int x,int y)` is the spill sink (D4). `seedStartingBag()` (6.1) constructs Klein's starting pack — now a full-durability, untrapped `Bag`.
- **`system/HazardSystem` + `StructureTable.Hazard.onStep(player,rng,messages,bonus)`** — the fire-an-effect-with-a-message pattern the trap fire mirrors (D3). Traps are bag-scoped, NOT structure hazards — a small parallel, not a reuse of the `Hazard` enum.
- **`system/CombatSystem`** — where `Weapon.decay(DECAY_PER_ACTION)` fires on an attack (line ~66) and where Klein takes a hit; the bag-wear-on-hit-taken trigger (D2) hooks the damage-taken path here.
- **`RoguePlayer`** — `hurtRaw(int)` and `adjustTemperature(int)` are the trap effects (D3); no new harm system.
- **`SaveService`** — libGDX `Json`, prototypes off. Register the `Bag` element type for the `Inventory.storageBags` list; a pre-6.2 save (with `storageItems` int array, no `storageBags`) must migrate to full-durability untrapped bags in `Inventory.restoreAfterLoad()` (AD-6).
- **Tests to keep green / update** — `InventoryCapacityTest` (storage expansion now via `Bag`s — update the `equip(TRAVELERS_PACK)`-based tests to the `Bag` model), `RunStatePersistenceTest.hybridInventoryRoundTrips` + `pre61SaveLoadsMainItemsAndAnEmptyQuickAccessStructure` (the storage band is now `storageBags`; add a pre-6.2 `storageItems`→`storageBags` migration test), `SurvivalTickTest` (starting bag weight unchanged), `StructureContentTest` (the new authored bag loot → update the scatter/loot-count invariants, AD-5). Suite is at **535**.

## Tasks / Subtasks

- [x] **Task 1 — The `Bag` first-class model + `Inventory` storage refactor (AC-1, D1).**
  - [x] 1.1 New `item/Bag`: `Supply type`, `int maxDurability`/`durability`, `BagTrap trap`, no-arg ctor + plain fields (Json), `decay`/`isBroken`/`slotBonus()`/`isTrapped`/`disarm` + getters. Mirrors `Weapon`. `MAX_DURABILITY = 20`.
  - [x] 1.2 Replaced `Inventory.int[] storageItems` with `List<Bag> storageBags`; `mainSlotCapacity()` sums `slotBonus()` over non-broken bags; `storageItemCount() = storageBags.size()`; `equip(storageType)` → `readyBagFromStore(type, NONE)` (untrapped). Preserved the 6.1 public API + gear/artifact routing (`slotsFor` now non-storage only). Added `readyBagFromStore(type, trap)`, `getStorageBags()`, `breakBag(bag)`.
  - [x] 1.3 `Inventory.restoreAfterLoad()` migrates a legacy `storageItems` int array (kept as a migration-only field) into full-durability untrapped `Bag`s; `SaveService` registers `Bag` for `Inventory.storageBags`. `RunState.seedStartingBag()` unchanged — `equip` now yields an untrapped `Bag`.
- [x] **Task 2 — Durability wear + break (AC-1, D2).**
  - [x] 2.1 `BagSystem.BAG_DECAY_PER_HIT`; `CombatSystem` calls `BagSystem.onPlayerHit` after a hit, decaying the first readied bag; logged.
  - [x] 2.2 At `durability <= 0` the bag breaks → spill (100% recoverable); the break is announced.
- [x] **Task 3 — Traps: model + effects (AC-2, D3).**
  - [x] 3.1 `item/BagTrap { NONE, DART, FIRE, FREEZE }` — hidden until fired; effects via `hurtRaw`/`adjustTemperature`.
  - [x] 3.2 **Design refinement (see Dev Notes):** the trap is *rolled* when a found bag is readied (`BagSystem.ready`, where the RNG lives — Inventory stays pure) and *fires* on a later combat hit (a seeded chance) so it springs on a bag holding contents (AC-2's "75% of its contents"). Firing applies DART/FIRE/FREEZE, then breaks with loss (Task 4).
- [x] **Task 4 — Break → spill (AC-2, D4) — the capacity-shrink guard.**
  - [x] 4.1 `Inventory.breakBag` removes the bag → capacity shrinks; collects + clears the overflow stacks (indices ≥ new capacity).
  - [x] 4.2 `BagSystem.spill`: durability-break 100% recoverable to Klein's tile (`addFloorItem`); trap-break 75% recoverable + 25% lost. **Deterministic** floor split (`count·pct/100`) — no RNG (AD-5). Spill/loss announced.
- [x] **Task 5 — World placement of found bags (D5, AD-5).**
  - [x] 5.1 `StructureTable.FOUND_BAG_LOOT` (Traveler's Pack, 45%); `RunState.placeFoundBags` places it in the Poacher's Camp footprint as the final generation pass, drawn from a **seed-derived sub-stream** (`seed ^ salt`) so the shared gameplay rng is untouched — every pre-6.2 seed's structure loot, cordon, weather, AND runtime hazard rolls stay byte-identical (the isolated stream is what makes the AD-5 "byte-identical" claim actually hold; the two seed-pinned hazard tests needed no change).
- [x] **Task 6 — Tests + verification (all ACs).**
  - [x] 6.1 AC-1: `BagDurabilityTest` — wear→break at 0; `breakBag` shrinks capacity + returns overflow; a worn bag breaks on a hit and spills 100% recoverable.
  - [x] 6.2 AC-2: `BagTrapTest` — each trap's effect (dart/fire/freeze/none); a sprung trap breaks the bag and drops exactly 75% (6 of 8 per stack) while losing 25%.
  - [x] 6.3 Persistence/migration: `RunStatePersistenceTest.pre62SaveMigratesFlyweightStorageItemsToDurableBags` (legacy `storageItems`→`Bag`); `hybridInventoryRoundTrips` + the pre-6.1 test updated to the `Bag` model (element type registered in the test serializer).
  - [x] 6.4 Regression: full suite **535 → 545 green**; determinism (Night/Weather/StructureContent) untouched by the isolated bag stream. **Verified:** `mvn -o -pl core test` (545 pass); `mvn -o compile` (core + desktop clean).

## Dev Notes

- **FR-20 / AD-17 economy spine.** Storage itself now carries risk: bags wear and break, and a found bag may be trapped — reinforcing that carrying is a decision and the economy is always worse than foraging. Spill is drop-or-leave (FR-9), never silent deletion beyond the trap's authored 25% loss.
- **Mirror `Weapon`, don't reinvent (CLAUDE.md §3).** `Bag` reuses the proven first-class-durability shape (no-arg ctor + plain fields, `decay`/`isBroken`, list-under-save-root). Reuse `addFloorItem` for the spill, `hurtRaw`/`adjustTemperature` for trap effects, and the 6.1 `mainSlotCapacity()`/`storageSlotBonus()` for capacity — add no parallel systems.
- **Simplicity (CLAUDE.md §2).** 6.2 is durability + traps + break-spill + minimal found placement. NO bag repair, NO UI redesign, NO voluntary un-ready, NO currency/traders — those are later/deferred.
- **This story closes 6.1 review debt.** D4's capacity-shrink spill is the guard for the deferred "`tryAdd`/`remove`/`count` scan the full physical store while placement is capped" invariant (breaking is the first shrink path); breaking is also the bag-removal path the 6.1 review flagged as missing. See `deferred-work.md` (2026-08-14, story-6.1).
- **AD-6 migration.** New `Bag` fields are plain/defaulted; a pre-6.2 save's `int[] storageItems` migrates to full-durability untrapped bags in `restoreAfterLoad()` (do NOT drop the player's readied bags on load). Register `Bag` in `SaveService` like `Weapon`.
- **AD-5 determinism.** New bag loot is appended LAST and placed after existing draws (the Epic-5 retro rule) so prior seed layouts stay byte-identical — update the snapshot invariants, don't perturb the stream. If the 75/25 spill uses a per-item coin, it's one seeded draw; a deterministic count-based split (floor(0.75·n)) avoids RNG entirely and is preferred.
- **Observation discipline (Epic-1/1.8 lesson, re-flagged in the 6.1 review).** Every wear tick that matters, every trap fire (naming the effect), every break, spill, and the 25% loss must emit a log line — no silent inventory mutation.
- **Reachability (Epic-5 retro + the 6.1 finding).** Traps must be reachable through found bags in real play, not test-only — Task 5 is not optional. Verify a trapped bag can actually be obtained and fire in-game.
- **Coupling caution.** `storageItems`→`storageBags` ripples into `RunState.seedStartingBag`, `SaveService`, any `MarginScreen` storage read, and the 6.1 tests (`InventoryCapacityTest`, `RunStatePersistenceTest`) — update their storage assertions to the `Bag` model; keep the count-agnostic main-store selection ring untouched.
- **Build/verify:** `docs/BUILD.md` — `mvn -o -pl core install` before any `exec:java`; `mvn -o -pl core test` + `mvn -o compile` for the story gate.

### Project Structure Notes

- New: `core/src/main/java/com/margins/rogue/item/Bag.java`, `core/src/main/java/com/margins/rogue/item/BagTrap.java`.
- Edits: `item/Inventory.java` (storage band → `List<Bag>`, capacity, migration, break-spill helper), `rogue/state/RunState.java` (`seedStartingBag` → `Bag`; spill sink already present), `rogue/save/SaveService.java` (register `Bag`), `rogue/system/CombatSystem.java` (bag wear on hit taken), `rogue/world/StructureTable.java` (bag loot, some trapped).
- New/updated tests: `BagDurabilityTest` / `BagTrapTest` (new), `InventoryCapacityTest` + `RunStatePersistenceTest` + `StructureContentTest` (updated to the `Bag` model + new loot invariants).

### References

- [Source: _bmad-output/planning-artifacts/epics.md#Story 6.2: Bag durability and thematic traps] — AC-1/AC-2, FR-20.
- [Source: core/src/main/java/com/margins/rogue/item/Weapon.java] — the AD-13 first-class-durability model `Bag` mirrors (no repair curve in 6.2).
- [Source: _bmad-output/implementation-artifacts/6-1-hybrid-slot-weight-inventory.md] — the 6.1 hybrid store `storageItems` model this story upgrades; its Review Findings + the starting-bag decision.
- [Source: _bmad-output/implementation-artifacts/deferred-work.md#code review of story-6.1 (2026-08-14)] — the capacity-shrink/scan invariant and bag-removal items 6.2 closes.

## Dev Agent Record

### Agent Model Used

Claude Opus 4.8 (1M context) — create-story 2026-08-15 (autonomous loop).

### Debug Log References

- `mvn -o -pl core test` — 545 tests, 0 failures (suite 535 → 545).
- `mvn -o compile` (core + desktop) — clean.

### Completion Notes List

- **AC-1 met.** `Bag` is a first-class instance (mirrors `Weapon`): `Inventory`'s storage band is now `List<Bag> storageBags`. Bags wear via `BagSystem.onPlayerHit` (a combat hit decays the first readied bag `BAG_DECAY_PER_HIT`); at 0 durability the bag breaks, capacity shrinks, and the overflow spills 100% recoverable. Capacity/weight math stayed pure in `Inventory` (AD-1/AD-5); the RNG-bearing rules live in `BagSystem`.
- **AC-2 met.** `BagTrap {NONE,DART,FIRE,FREEZE}` reuses `hurtRaw`/`adjustTemperature`. `BagSystem.spill` drops a deterministic 75% (recoverable) and destroys 25% on a trap break — pinned exactly (6 of 8 per stack). `BagSystem.fireTrap` is the testable "what"; `onPlayerHit`'s seeded chance is the "when".
- **Design refinement of D3 (trap trigger).** The story's original "fires on first ready" had a flaw — a just-readied bag holds no contents to spill. Resolved cleanly: a found bag's trap is **rolled at ready time** (`BagSystem.ready`, which owns the RNG so `Inventory.equip` stays pure/untrapped for the starting pack + 6.1 tests) and **fires on a later combat hit** (a seeded chance), so it springs on a bag that has been in use. The starting Traveler's Pack is readied via `equip` → untrapped.
- **Closed 6.1 review debt.** `breakBag`'s capacity-shrink→overflow-spill is the guard the 6.1 review deferred to this story; breaking is also the bag-removal path 6.1 lacked.
- **AD-5 determinism (key decision).** Found-bag placement draws from a **seed-derived sub-stream** (`seed ^ BAG_PLACEMENT_SALT`) instead of appending to the shared gameplay rng. This is what makes the story's "prior seeds byte-identical" claim literally true: the two seed-pinned hazard tests (`NightWeatherHazardTest`) and `StructureContentTest` needed no change. `placeLootInFootprint` now takes an explicit `Random` (shared `rng` for structure/cellar loot; the sub-stream for found bags).
- **AD-6 migration.** Kept a migration-only `storageItems` int[] field; `restoreAfterLoad` converts a pre-6.2 save's readied bags into full-durability untrapped `Bag`s (verified) so a 6.1 save's worn pack survives the upgrade. `SaveService` + the test serializer register `Bag`.
- **Deferred (unchanged):** bag repair, voluntary un-ready, the Quick-Access/artifact UI exposure, a full bag catalog, currency (6.3), traders (6.4).

### File List

- `core/src/main/java/com/margins/rogue/item/Bag.java` — **new**: first-class bag (durability + trap).
- `core/src/main/java/com/margins/rogue/item/BagTrap.java` — **new**: `NONE/DART/FIRE/FREEZE` + effects.
- `core/src/main/java/com/margins/rogue/system/BagSystem.java` — **new**: trap roll on ready, wear + trap-fire + deterministic spill on a hit.
- `core/src/main/java/com/margins/rogue/item/Inventory.java` — storage band `int[] → List<Bag>`; `readyBagFromStore`, `breakBag`, `getStorageBags`; capacity/weight/count read bags; `restoreAfterLoad` migration.
- `core/src/main/java/com/margins/rogue/state/RunState.java` — `placeFoundBags` (seed-derived sub-stream); `placeLootInFootprint(..., Random)`; `seedStartingBag` yields a `Bag`.
- `core/src/main/java/com/margins/rogue/system/CombatSystem.java` — `BagSystem.onPlayerHit` after a hit taken.
- `core/src/main/java/com/margins/rogue/save/SaveService.java` — register `Bag` for `Inventory.storageBags`.
- `core/src/main/java/com/margins/rogue/world/StructureTable.java` — `FOUND_BAG_LOOT` + `FOUND_BAG_CHANCE`.
- `core/src/main/java/com/margins/MarginScreen.java` — readying a storage bag routes through `BagSystem.ready` (rolls the trap).
- `core/src/test/java/com/margins/rogue/system/BagDurabilityTest.java` — **new** (AC-1).
- `core/src/test/java/com/margins/rogue/system/BagTrapTest.java` — **new** (AC-2).
- `core/src/test/java/com/margins/rogue/state/RunStatePersistenceTest.java` — `Bag` registration; pre-6.1 test strips `storageBags`; new pre-6.2 migration test.

## Change Log

- 2026-08-15 — created by create-story (autonomous loop). Decisions: D1 `Bag` first-class instance mirroring `Weapon`, replacing 6.1's `int[] storageItems` with `List<Bag> storageBags`; D2 durability + wear-on-hit-taken (repair deferred); D3 `BagTrap {NONE,DART,FIRE,FREEZE}` firing on first ready, effects via `hurtRaw`/`adjustTemperature`; D4 break→spill (durability 100% recoverable, trap 75%/25%) which also guards the 6.1-deferred capacity-shrink/scan invariant; D5 minimal found-bag world placement (some trapped, AD-5 append-last). Bounded: bag repair, voluntary un-ready, Quick-Access UI exposure, full bag catalog, currency (6.3), traders (6.4) all deferred. Status → ready-for-dev.
- 2026-08-15 — dev-story (dev). Implemented both ACs. Refined D3's trap trigger: a found bag's trap is rolled at ready time (`BagSystem.ready`, keeping `Inventory` RNG-free) and fires on a later combat hit, so it springs on a bag holding contents (fixes the empty-on-ready flaw). AD-5: found-bag placement uses a seed-derived sub-stream so the shared gameplay rng — and every seed-pinned determinism test — is byte-identical (no snapshot-test churn). AD-6: kept a migration-only `storageItems` field so a 6.1 save's readied bag becomes a full-durability `Bag`. Closed the 6.1-deferred capacity-shrink/overflow-spill invariant + the missing bag-removal path. Suite 535 → 545 green; core + desktop compile clean. Status → review.

---
baseline_commit: 150e962
---

# Story 6.1: Hybrid slot + weight inventory

Status: done

## Story

As Klein,
I want carrying capacity to be a real constraint that better bags extend,
so that what I haul is a decision (FR-20).

## Acceptance Criteria

**AC-1 — Quick-Access slots + a storage-expandable main inventory.**
**Given** my inventory **When** I open it **Then** Quick-Access slots (**5** weapon/armor-type + **3** artifact/ring) are always available, and the main inventory is **19 base slots** expandable by equipping up to **5** storage items (their bonuses merge).

**AC-2 — STR-scaled weight capacity with encumbrance.**
**Given** my STR **When** I carry items **Then** weight capacity scales with STR and over-capacity is handled (encumbrance), so a full pack limits foray range.

## Scope decisions (author, 2026-08-14 — running the loop autonomously per Justine)

- **D1 — Extend the proven stackable `int type/count` model; don't replace it.** The existing `item/Inventory` (Epics 1/3) is a stackable int-array container that serializes as plain int arrays under the `RunState` save root (AD-6). 6.1 **restructures the slot counts and layers weight on top** — it does NOT switch to a per-item object model. This keeps save/load, `SaveService` Json reflection, and every `tryAdd/remove/count` call site working.
- **D2 — The hybrid slot structure (AC-1).** Replace `BACKPACK_STACKS = 8` / `EQUIPPED_SLOTS = 2` with:
  - **Main store:** `MAIN_BASE_SLOTS = 19` stacks.
  - **Storage expansion:** equipping a *storage item* (a bag) adds slots; up to `MAX_STORAGE_ITEMS = 5` storage items, their bonuses **summed/merged** → `mainSlotCapacity() = 19 + Σ(storage bonuses)`. (Bag durability/traps are Story 6.2 — 6.1 ships the slot-bonus mechanic only.)
  - **Quick-Access:** `QUICK_GEAR_SLOTS = 5` (weapon/armor-type) + `QUICK_ARTIFACT_SLOTS = 3` (artifact/ring) = 8 slots, always available.
  - The selection-cycle helpers (`nextOccupiedStack`/`previousOccupiedStack`) are **count-agnostic** (they ring over the constant), so they carry over unchanged; only the constant and the coupled render/test expectations move.
- **D3 — Weight & STR capacity (AC-2).** Add per-item weight: `Supply.weight()` (authored small ints, **default 1** — a minimal authored pass, not a full economy rebalance). `Inventory.totalWeight()` = Σ(count × weight) across the main store, quick-access, and equipped storage. `carryCapacity(int str) = BASE_CAPACITY + str × STR_CAPACITY_FACTOR`. `isEncumbered(int str) = totalWeight() > carryCapacity(str)`. Capacity reads `RoguePlayer.getStr()` (which already folds the bacterial STR penalty, Story 1.7).
- **D4 — Encumbrance effect is detection + one minimal hook; the tuning is deferred (AC-2 "limits foray range").** 6.1 ships the authoritative `isEncumbered` read and a single minimal consequence hook the turn loop can honor (an encumbered move costs/penalizes — the smallest faithful "limits foray range"); the full foray-range balance pass is deferred. Encumbrance never *blocks* carrying (you can always over-pack and pay the cost), matching the drop-or-leave philosophy.
- **D5 — Minimal item categories.** Add predicates on `Supply`: `isStorage()` (bags — the storage-expansion set), `isQuickGear()` (weapon/armor-type), `isQuickArtifact()` (artifact/ring). Most current supplies are consumables → none of these → they live in the main store. A minimal default categorization; a full catalog re-tag rides the item growth in 6.2+.
- **Deferred (→ 6.2+ / polish):** the polished Quick-Access + artifact-slot **UI redesign** (6.1 adapts the existing backpack render to the new counts and exposes the quick-access/artifact model; a bespoke layout is deferred); the encumbrance→foray-range **gameplay tuning**; per-item **weight/category authoring** beyond minimal defaults; **bag durability/traps** (6.2), **currency** (6.3), **traders** (6.4).

## Baseline (verify before adding)

- **`item/Inventory`** — stackable `int[] types/counts` (`BACKPACK_STACKS = 8`) + `int[] equipped` (`EQUIPPED_SLOTS = 2`); `EMPTY = -1` sentinel (type ids must be ≥ 0). API: `tryAdd(type,amount)→AddResult{ADDED,BACKPACK_FULL}`, `remove`, `drop`, `equip`, `unequip`, `count`, `backpackType/backpackCount(slot)`, `nextOccupiedStack/previousOccupiedStack(from)` (count-agnostic ring), `backpackStackCount`, `isBackpackFull`, `equippedType(slot)`. Serializes as plain int arrays (AD-6).
- **Consumers to keep working** — `MarginScreen` (backpack render + selection: `BACKPACK_STACKS`, `EQUIPPED_SLOTS`, `backpackType`, `nextOccupiedStack`, `equip/unequip`, `selectedSlot`); `TurnEngine` (pickup `tryAdd`, use `remove`); `RepairSystem`/`CompanionSystem` (`tryAdd`/`remove`); `InventorySelectionTest` (pins the selection-cycle ring — its expectations must be updated to the new slot count where they depend on the wrap point).
- **`Supply`** — enum with `displayName`, provision/water/food/cure/toxin semantics, `byOrdinal`, `scatterableOrdinals`. **No weight or category yet** — 6.1 adds `weight()` + the three category predicates (default-safe: unknown → weight 1, not storage/gear/artifact).
- **`RoguePlayer`** — `getStr()` (base 5; folds the Story-1.7 bacterial STR penalty). Weight capacity reads this.
- **`SaveService`** — libGDX `Json`, prototypes off; serializes `RunState`/`Inventory` by reflection. New **plain fields** (the storage/quick-access arrays) persist automatically; give each an AD-6-safe default (empty = `-1`-filled) so a pre-6.1 save loads a valid empty structure.
- **Tests to keep green** — `InventorySelectionTest` (update wrap-point expectations to the new count), `RunStatePersistenceTest` (inventory round-trips), plus the pickup/use/repair paths. Suite is at **521**.

## Tasks / Subtasks

- [x] **Task 1 — Restructure the slot model (AC-1, D2).**
  - [x] 1.1 Replace `BACKPACK_STACKS` with `MAIN_BASE_SLOTS = 19`; add `QUICK_GEAR_SLOTS = 5`, `QUICK_ARTIFACT_SLOTS = 3`, `MAX_STORAGE_ITEMS = 5`. Add the quick-access + storage arrays (`-1`-filled defaults, AD-6). Keep the `int type/count` stack model. *(Kept field names `types`/`counts`/`equipped` for AD-6 migration; the main store is physically sized to `MAX_MAIN_SLOTS = 19 + 5×4 = 39` so a bag readied mid-run never reallocates.)*
  - [x] 1.2 `mainSlotCapacity()` = `MAIN_BASE_SLOTS + Σ(equipped storage-item bonuses)`; equipping a storage item consumes one of `MAX_STORAGE_ITEMS` and merges its bonus; `tryAdd` respects the *current* `mainSlotCapacity()` (not a fixed 8). Quick-Access add/equip routes gear→gear slots, artifact→artifact slots (storage→storage), via `slotsFor(type)`.
  - [x] 1.3 Update the coupled consumers to the new counts (`MarginScreen` constants/render/selection; `SpoilageSystem`; the `tryAdd`/`equip` call sites). The selection-cycle helpers are unchanged (count-agnostic — they ring over the physical store).
- [x] **Task 2 — Weight, STR capacity, encumbrance (AC-2, D3/D4).**
  - [x] 2.1 `Supply.weight()` (authored small ints, default 1) + `isStorage()`/`isQuickGear()`/`isQuickArtifact()` predicates (default-safe) + `storageSlotBonus()`.
  - [x] 2.2 `Inventory.totalWeight()`; `carryCapacity(int str)` = `BASE_CAPACITY(10) + str × STR_CAPACITY_FACTOR(4)`; `isEncumbered(int str)`.
  - [x] 2.3 Wire the minimal encumbrance consequence into the turn loop — an encumbered committed MOVE burns `ENCUMBERED_MOVE_HUNGER_COST = 3` extra hunger turns (the smallest faithful "limits foray range"); never blocks carrying. Observation-safe (announces any tier crossing it causes).
- [x] **Task 3 — Persistence & migration safety (AD-6, D1).**
  - [x] 3.1 New arrays serialize/round-trip via `SaveService`; `Inventory.restoreAfterLoad()` (called from `RunState.restoreAfterLoad`) grows a pre-6.1 save's shorter main/gear arrays and defaults the absent quick-access/storage bands to a valid empty structure. Extended `RunStatePersistenceTest`.
- [x] **Task 4 — Tests + verification (all ACs).**
  - [x] 4.1 AC-1 slots: main store holds 19 base; equipping storage items raises `mainSlotCapacity()` by the merged bonus (cap at `MAX_STORAGE_ITEMS`); `tryAdd` returns `BACKPACK_FULL` only at the *current* capacity; quick-access gear vs artifact routing; the selection ring still wraps correctly at the new count. *(`InventoryCapacityTest`.)*
  - [x] 4.2 AC-2 weight: `totalWeight` sums count×weight; `carryCapacity` scales with STR (and drops with the bacterial STR penalty); `isEncumbered` flips exactly at capacity; the encumbrance hook fires when over and not when under. *(`InventoryCapacityTest` + `SurvivalTickTest.anEncumberedStepBurnsExtraHunger…`.)*
  - [x] 4.3 Persistence: inventory (main + quick-access + storage + weight-derived reads) round-trips; a field-absent load is a valid empty structure. *(`RunStatePersistenceTest.hybridInventoryRoundTrips` + `pre61SaveLoadsMainItemsAndAnEmptyQuickAccessStructure`.)*
  - [x] 4.4 Regression: `InventorySelectionTest` (unchanged — the ring is count-agnostic), `MarginScreenInventoryHudTest`/`DialogueEffectTest`/`StructureContentTest` expectations updated to the new counts, pickup/use/repair paths, full suite green (521 → **534**). **Verified:** `mvn -o -pl core test` (534 pass), `mvn -o compile` (core + desktop clean).

## Review Findings (code-review 2026-08-14 — 3-layer parallel: Blind Hunter, Edge-Case Hunter, Acceptance Auditor)

### Decision needed
- [x] [Review][Decision → Patch, RESOLVED] `TRAVELERS_PACK` is unreachable in-game — AC-1's storage-expansion mechanic can only fire in tests. [`Supply.isStorage`, `StructureTable`] — The bag was never looted/spawned/granted, and `equip` requires already holding it, so the headline expansion was model-only in the live game. **Resolution (Justine's call): Klein now starts with the pack worn** — `RunState.seedStartingBag()` grants + readies one Traveler's Pack at run construction, so `mainSlotCapacity()` is 23 and the HUD shows `/23` from turn 1 (AD-5-safe: no RNG/seed impact). Further bags are found in the world with Story 6.2. (auditor)

### Patch
- [x] [Review][Patch, FIXED] Encumbrance hunger cost fires on a non-displacing MOVE (Bloated/Crippled stumble or zero-delta). [`TurnEngine.advance` acted-block] — The hook checked only `kind == MOVE`, not actual displacement. **Fixed:** capture the pre-action tile and gate the +3 cost on real displacement (mirrors the hazard-step guard). Locked by `SurvivalTickTest.anEncumberedNonDisplacingMoveBurnsNoEncumbranceCost`. (blind)

### Deferred
- [x] [Review][Defer] Quick-Access gear slots 2–4 are navigable + returnable but not rendered/highlighted in the loadout page [`MarginScreen.renderBodyInventoryPage`] — recoverable (cursor rings `%5`, `unequip` works blind), not data loss; exposing all 5 gear + 3 artifact + 5 storage slots IS the bespoke Quick-Access UI the story defers. (blind+edge, Med)
- [x] [Review][Defer] No un-ready path for readied bags/artifacts [`Inventory.unequip` gear-only] — bags intentionally stay readied in 6.1; the artifact band has no items yet. → 6.2. (edge, Low)
- [x] [Review][Defer] `moveInventoryCursor` collapses the ragged last-row column (down from slot 15 lands col 2, not col 3) [`MarginScreen.moveInventoryCursor`] — cosmetic navigation wart in the 5×4 grid's short last row (16–18). (blind, Low)
- [x] [Review][Defer] `tryAdd`/`remove`/`count` scan the full physical store while placement is capped at `mainSlotCapacity()` [`Inventory.java`] — safe today (bags never un-ready, so capacity never shrinks and no stack lands ≥ capacity); add a guard/comment when 6.2 introduces bag un-readying/capacity-shrink. (blind+edge, Low)
- [x] [Review][Defer] Encumbrance drain is unannounced unless it crosses a hunger tier [`TurnEngine`] — no "you strain under the load" feedback; observation polish tied to the deferred foray-range tuning. (blind, Low)

### Dismissed (6)
Encumbrance no-op when Starving-at-0 (negligible — already dying); encumbrance ignores non-MOVE acted turns (by design — foray range = movement); `totalWeight` int-overflow (needs ~2B stack count, no game path); oversize-array `grow` passthrough (theoretical future constant-shrink); negative opaque type ids treated as occupied (contract: type ids ≥ 0, `-1` is the sole sentinel); `isQuickArtifact` always false (spec-sanctioned D5 deferral, not a defect).

## Dev Notes

- **FR-20 / AD-17 economy spine.** Carrying is a *decision*: capacity is finite, storage items extend it, weight ties it to STR. Encumbrance is a cost, never a hard block (drop-or-leave philosophy, FR-9).
- **AD-6.** Every new `Inventory` field is a plain int array with a deterministic empty (`-1`) default, so save/load and a pre-6.1 field-absent load both yield a valid structure — no ctor-rolled nondeterminism (contrast the 1.3/3.3 traps).
- **AD-1/AD-2/AD-3.** `Inventory` stays headless (no libGDX), owned by `RunState`; `MarginScreen` only reads slots and renders. The weight/capacity math lives in `Inventory` + `Supply`, not the screen.
- **AD-5.** No RNG in the capacity/weight decisions — pure functions of STR and held items.
- **Reuse (CLAUDE.md §3).** Extend the existing stackable model + selection helpers rather than rewriting to per-item objects; reuse `RoguePlayer.getStr()` (don't re-implement the STR penalty); adapt the existing backpack render to the new counts rather than a new screen mode.
- **Simplicity (CLAUDE.md §2).** 6.1 is the capacity model + weight + encumbrance detection. No durability, no traps, no currency, no UI redesign — those are 6.2–6.4 / polish.
- **Coupling caution.** Changing the slot count ripples into `MarginScreen` and `InventorySelectionTest`; update their count-dependent expectations, but keep the count-agnostic ring logic intact.
- **Build/verify:** `docs/BUILD.md` — `mvn -o clean install`.

### Project Structure Notes

- Edits: `core/src/main/java/com/margins/rogue/item/Inventory.java` (slot model + weight/capacity), `core/src/main/java/com/margins/rogue/item/Supply.java` (`weight()` + category predicates), `core/src/main/java/com/margins/MarginScreen.java` (constants/render/selection to new counts), the turn loop for the encumbrance hook.
- New/updated tests: `InventoryCapacityTest` (new — slots + weight + encumbrance), `InventorySelectionTest` (updated), `RunStatePersistenceTest` (extended).

### References

- [Source: _bmad-output/planning-artifacts/epics.md#Story 6.1: Hybrid slot + weight inventory] — AC-1/AC-2, FR-20.
- [Source: _bmad-output/planning-artifacts/epics.md#Epic 6] — the carrying-as-a-constraint spine; storage items (6.1), durability/traps (6.2), currency (6.3), traders (6.4).

## Dev Agent Record

### Agent Model Used

Claude Opus 4.8 (1M context) — create-story 2026-08-14 (autonomous loop); dev-story 2026-08-14.

### Debug Log References

- `mvn -o compile` (core + desktop) — clean.
- `mvn -o -pl core test` — 534 tests, 0 failures (suite 521 → 534, +13 net).

### Completion Notes List

- **AC-1 met.** The stackable `int type/count` model is extended (not rewritten). Main store is 19 base stacks, physically sized to `MAX_MAIN_SLOTS = 39` so readying a bag mid-run raises usable capacity without reallocating. `mainSlotCapacity()` sums the merged storage-item bonuses; `tryAdd` gates on the *current* capacity. Quick-Access = 5 gear + 3 artifact slots, always available; `equip(type)` routes by category (`slotsFor`): storage→storage, artifact→artifact, else gear. The selection ring is count-agnostic (rings over the physical store) — `InventorySelectionTest` passed **unchanged**.
- **AC-2 met.** `Supply.weight()` (default 1; bulky materials + the bag heavier). `Inventory.totalWeight()` sums main + quick-access + storage. `carryCapacity(str) = 10 + 4·str` reads `RoguePlayer.getStr()`, so the Story-1.7 bacterial/Starving STR penalty tightens encumbrance for free. `isEncumbered` never blocks carrying; the turn-loop hook (an encumbered committed MOVE burns +3 hunger) is the minimal faithful "limits foray range".
- **Scope decision (surfaced).** AC-1's storage expansion needed a *concrete* storage item to be real/reachable (not dead code). Added one minimal bag Supply — **`TRAVELERS_PACK` ("Traveler's Pack")** — appended last (ordinals/saves unchanged, AD-6), non-scatterable (seed stream byte-identical, AD-5), single-identity `TRAVELERS_PACK_ID` (inert on use). It readies via the existing loadout `Y` key (now category-driven `canReadyInLoadout`), so the capacity gain is player-reachable and visible in the pack count ("3/23"). Its durability/traps ride Story 6.2 as specified.
- **AD-6 migration.** Field names `types`/`counts`/`equipped` were preserved (not renamed), so a pre-6.1 save's items load into the same fields; `Inventory.restoreAfterLoad()` grows the shorter loaded arrays and defaults the new `quickArtifact`/`storageItems` bands empty. Verified by `pre61SaveLoadsMainItemsAndAnEmptyQuickAccessStructure`.
- **Deferred as bounded (unchanged):** the bespoke Quick-Access/artifact UI layout (the HUD strip previews the first `HUD_QUICKBAR_SLOTS = 8`; the full 5-row grid is in the overlay), encumbrance→foray-range tuning, per-item weight/category authoring beyond the minimal pass, and 6.2–6.4.

### File List

- `core/src/main/java/com/margins/rogue/item/Inventory.java` — hybrid slot model + storage expansion + weight/capacity/encumbrance + `restoreAfterLoad`.
- `core/src/main/java/com/margins/rogue/item/Supply.java` — `TRAVELERS_PACK`; `weight()`, `isStorage()`/`isQuickGear()`/`isQuickArtifact()`, `storageSlotBonus()`, `MAX_STORAGE_SLOT_BONUS`; non-scatterable + inert-on-use for the bag.
- `core/src/main/java/com/margins/rogue/item/TrueIdentity.java` — `TRAVELERS_PACK_ID` (inert single identity).
- `core/src/main/java/com/margins/rogue/state/RunState.java` — call `inventory.restoreAfterLoad()`; updated field comment.
- `core/src/main/java/com/margins/rogue/system/TurnEngine.java` — `ENCUMBERED_MOVE_HUNGER_COST` + the encumbered-move hook (observation-safe).
- `core/src/main/java/com/margins/rogue/system/SpoilageSystem.java` — iterate the main store over `MAX_MAIN_SLOTS`.
- `core/src/main/java/com/margins/MarginScreen.java` — new slot constants/counts, `mainGridRows()` (5-row grid), capacity in the pack count, category-driven `canReadyInLoadout`, `HUD_QUICKBAR_SLOTS` preview.
- `core/src/test/java/com/margins/rogue/InventoryCapacityTest.java` — **new**: AC-1 slots/expansion/routing/ring + AC-2 weight/capacity/encumbrance.
- `core/src/test/java/com/margins/rogue/SurvivalTickTest.java` — new encumbered-move turn-loop test.
- `core/src/test/java/com/margins/rogue/state/RunStatePersistenceTest.java` — hybrid round-trip + pre-6.1 migration.
- `core/src/test/java/com/margins/MarginScreenInventoryHudTest.java` — cursor test updated to the 5-row grid.
- `core/src/test/java/com/margins/rogue/narrative/DialogueEffectTest.java` — full-pack filler updated to 19.
- `core/src/test/java/com/margins/rogue/StructureContentTest.java` — non-scatterable count 5 → 6 (the bag).

## Change Log

- 2026-08-14 — created by create-story (autonomous loop). Decisions: D1 extend the stackable int model (not a per-item rewrite); D2 the hybrid slots — 19 base + storage expansion (≤5, merged) + 5 gear/3 artifact quick-access; D3 weight via `Supply.weight()` + STR-scaled `carryCapacity` + `isEncumbered`; D4 encumbrance is detection + one minimal turn-loop cost (foray-range tuning deferred); D5 minimal `Supply` category predicates. Bounded: UI redesign, encumbrance tuning, per-item weight/category authoring, and 6.2–6.4 all deferred. Status → ready-for-dev.
- 2026-08-14 — dev-story (dev). Implemented all ACs; added the concrete `TRAVELERS_PACK` storage bag so AC-1 expansion is real/reachable; preserved `types`/`counts`/`equipped` field names + added `Inventory.restoreAfterLoad()` for AD-6 migration; wired the encumbered-move hunger cost. Suite 521 → 534 green; core + desktop compile clean. Status → review.
- 2026-08-14 — code-review (3-layer parallel: Blind Hunter, Edge-Case Hunter, Acceptance Auditor). 1 decision-needed + 1 patch + 5 defer + 6 dismissed. **Patches applied:** (1) `RunState.seedStartingBag()` — Klein starts wearing a Traveler's Pack so AC-1's expansion is live in-game (Justine's call over structure-loot / defer-to-6.2), closing the Acceptance Auditor's "test-only feature" finding; (2) `TurnEngine` encumbrance cost now gated on real displacement (no charge on a stumble / zero-delta MOVE). Test updates: `DialogueEffectTest` full-pack fill made capacity-robust, `RunStatePersistenceTest.hybridInventoryRoundTrips` verifies the worn starting bag, +`SurvivalTickTest.anEncumberedNonDisplacingMoveBurnsNoEncumbranceCost`. 5 Low/Med items deferred to `deferred-work.md` (Quick-Access UI exposure, bag/artifact un-ready, ragged-row cursor, physical-store scan invariant, encumbrance feedback). Suite 534 → **535** green; compile clean. Status → done.

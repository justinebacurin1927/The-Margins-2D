---
baseline_commit: 370f66324a5b55f5fb9b661100872155e4d1a8be
---

# Story 3.1: Backpack and equipped slots

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As Justine (player),
I want limited carry capacity,
so that scarcity forces real choices (FR-9).

## Acceptance Criteria

1. **Given** a full backpack, **When** I try to pick up another item, **Then** the pickup is not silently accepted — the attempt reports "backpack full" so the player can be prompted to drop-or-leave; equipping moves an item from a backpack stack into an equipped slot. (FR-9)
2. Capacity uses the tuning values (**8 backpack / 2 equipped**) and ratifies the existing stackable **type/count** `Inventory` model rather than replacing it with a per-item object model. (AD-12)

**Architectural definition-of-done (non-negotiable, required for the feature to work in the existing system):**

3. The inventory is owned by `RunState` (AD-3), contains **no** libGDX rendering types (AD-2), and survives save/load through the existing `SaveService` Json path with contents intact (AD-6).

## Tasks / Subtasks

- [x] **Task 1 — `Inventory` model with backpack + equipped capacity** (AC: 1, 2)
  - [x] Create `core/src/main/java/com/margins/rogue/item/Inventory.java`. Reuse the proven `int[] types` / `int[] counts` stack design from `com.margins.item.Inventory` (AD-12) — do **not** invent a per-item object model.
  - [x] Constants: `BACKPACK_STACKS = 8`, `EQUIPPED_SLOTS = 2`. Backpack holds up to 8 distinct stacks (stacking an existing type never consumes a new stack); equipped is a small fixed array of 2.
  - [x] `AddResult tryAdd(int type, int amount)` returning an enum `{ADDED, BACKPACK_FULL}`: stacks onto an existing type if present; else uses a free backpack stack if one of the 8 is open; else returns `BACKPACK_FULL` **without mutating** (the item is neither added nor lost — this is what lets the caller prompt drop-or-leave).
  - [x] `boolean drop(int type)` / `remove(int type, int amount)` to free a stack (the drop half of "drop-or-leave"; the on-tile placement is Story 3.2).
  - [x] `boolean equip(int type)`: moves one stack of `type` from backpack into the first free equipped slot; returns `false` if `type` isn't in the backpack or both equipped slots are full. `boolean unequip(int slot)`: returns the equipped item to the backpack (respecting backpack capacity).
  - [x] Query methods: `count(type)`, `backpackStackCount()`, `isBackpackFull()`, `equippedType(slot)`. No libGDX imports anywhere in this class.
- [x] **Task 2 — Own it on `RunState`** (AC: 3)
  - [x] Add a `private Inventory inventory` field to `RunState`, constructed in the constructor (like the other run-scoped fields). Add `Inventory getInventory()`.
  - [x] The field is a **plain serializable object** (int arrays) — not `transient` — so it saves/loads for free via `SaveService` (AD-6). Do not add rendering handles to it (AD-2).
  - [x] Confirm `restoreAfterLoad()` needs no special re-wiring for the inventory (it holds no back-reference to the tilemap/RNG). Leave that method otherwise untouched.
- [x] **Task 3 — Represent the drop-or-leave decision (model-level this story)** (AC: 1)
  - [x] The `BACKPACK_FULL` outcome from `tryAdd` is the contract that a full pickup surfaces a decision instead of silently dropping the item. **Do not** add on-screen prompt UI or a new pickup trigger in `RogueGameScreen` this story — there is no floor-item pickup source yet (that arrives with Story 3.2). Adding UI with no trigger would be dead code (AD-2 keeps rules out of the screen anyway).
  - [x] Leave a one-line `// Story 3.2 wires floor-item pickup + the drop-or-leave prompt here` marker at the natural call site if helpful, but add no behavior.
- [x] **Task 4 — Headless verification** (AC: 1, 2, 3)
  - [x] Fill 8 backpack stacks with 8 distinct types; a 9th distinct `tryAdd` returns `BACKPACK_FULL` and leaves the inventory unchanged; adding more of an existing type still succeeds (stacks, no new slot).
  - [x] `equip` moves a stack out of the backpack into an equipped slot; a 3rd `equip` (both slots full) returns `false`; `unequip` returns it to the backpack.
  - [x] Json round-trip a `RunState`: backpack stacks, counts, and equipped slots are identical after `toJson` → `fromJson` (mirror the Story 1.4 / 2.5 round-trip harness).
  - [x] `mvn -o -pl core install` then live boot on display `:0` for ~8s → clean (no runtime error from the new field).

### Review Findings

_Adversarial code review 2026-07-19 (Blind Hunter · Edge Case Hunter · Acceptance Auditor). Acceptance Auditor verdict: all ACs and AD-1/2/3/6/12 satisfied — no blocking issues. 4 findings dismissed as noise (amount≤0 validation — matches the ratified legacy model, no callers yet; counts overflow; remove() absent-vs-insufficient ambiguity; "serialize for free" comment — populated round-trip empirically verified 37/37)._

**Patch:**

- [x] [Review][Patch] `inventory` field now field-initialized (`= new Inventory()`), matching the `noiseQueue` convention, so it is non-null even if a save predating this field is loaded. Verified: an inventory-less save deserializes to a non-null, usable inventory [core/src/main/java/com/margins/rogue/state/RunState.java:29]
- [x] [Review][Patch] Class doc now reserves non-negative type ids (`-1` is the empty sentinel), guarding Story 3.3's concrete SupplyType ids against silent stack corruption [core/src/main/java/com/margins/rogue/item/Inventory.java:1]

**Deferred (to Story 3.2 — equip/drop UX):**

- [x] [Review][Defer] `equip(type)` permits the same type in both equipped slots; confirm intended semantics (distinct gear vs. duplicate copies) when the equip UI lands [core/src/main/java/com/margins/rogue/item/Inventory.java] — deferred, belongs with 3.2 equip UX
- [x] [Review][Defer] Equipped items can become stuck: `unequip` into a full backpack is (correctly) refused and `drop()` only scans the backpack, so there is no path to shed an equipped item when the pack is full; add an unequip-with-drop flow alongside the drop UI [core/src/main/java/com/margins/rogue/item/Inventory.java] — deferred, needs the 3.2 drop UI

## Dev Notes

### Governing architecture (read before coding)
- **AD-12 — Identify-by-use binding on `RunState`.** RunState will later hold a `SupplyType → TrueIdentity` map plus an `identified` set (Stories 3.3/3.4). For **this** story, the load-bearing clause is: *"the existing stackable type/count `Inventory` model is ratified rather than replaced."* Keep the humble `int type` / `int count` design. [Source: ARCHITECTURE-SPINE.md#AD-12]
- **AD-3 — `RunState` is the single owner of run data**, and explicitly lists `inventory` among what it owns. The inventory must live on `RunState`, not on `RoguePlayer` or the screen. [Source: ARCHITECTURE-SPINE.md#AD-3]
- **AD-2 — Model ⟵ Screen layering.** No libGDX rendering types in the model. Note the existing `com.margins.item.Item` imports `com.badlogic.gdx.graphics.Texture` + `Assets` — that is a rendering coupling, so **do not** pull `Item` into the rogue model layer. The new `Inventory` stores opaque `int` type ids only. [Source: ARCHITECTURE-SPINE.md#AD-2; core/src/main/java/com/margins/item/Item.java]
- **AD-6 — Save = serialize whole `RunState`.** A plain (non-transient) field of int arrays serializes automatically; no `setElementType`/`setSerializer` needed. This is the "every future `RunState` field gets persistence for free" payoff called out in the Epic 1 retro. [Source: ARCHITECTURE-SPINE.md#AD-6; epic-1-retro-2026-07-17.md]
- **Structural seed** places this class at `rogue/item/Inventory.java` with the annotation *"ratify existing type/count model (AD-12); backpack + equipped slots (FR-9)."* [Source: ARCHITECTURE-SPINE.md#Structural Seed]

### Reuse vs. rebuild — the key decision
`com.margins.item.Inventory` already implements the exact type/count stack design you should reuse (see its `add`/`remove`/`count` — copy that shape). **Do not edit or extend that class.** It is referenced only by the frozen legacy `com.margins.screen.GameScreen` and `com.margins.quest.QuestManager`; adding capacity enforcement to its `add()` would change their behavior and risk a regression (AD-1 keeps new work out of legacy code). Create a fresh `com.margins.rogue.item.Inventory` in the rogue spine instead. [Source: core/src/main/java/com/margins/item/Inventory.java; ARCHITECTURE-SPINE.md#AD-1]

### Scope boundary (what this story is and is NOT)
- **IN:** the `Inventory` container, its capacity rules (8/2), the equip/unequip operation, the `BACKPACK_FULL` outcome, ownership on `RunState`, and save/load safety — all headless-testable.
- **OUT (later stories):** on-tile item placement + re-pickup and the drop-or-leave *prompt UI* → **Story 3.2** (Use and drop items). Concrete `SupplyType` values → **Story 3.3**. Identify-on-use reveal → **Story 3.4**. This story deliberately ships no floor-item entity and no new `RogueGameScreen` input, because there is nothing to pick up yet. `RogueGameScreen.handleInput()` currently maps W/A/S/D=move, Q=attack, E=block, SPACE=wait, R=restart — leave it unchanged.

### Current state of files you will touch
- `state/RunState.java` — single owner; today holds tileMap, player, enemies, floorDepth, seed, transient rng, lastStand flags, transient noiseQueue. You add one `Inventory inventory` field + getter, constructed in the constructor. Everything else stays. The `restoreAfterLoad()` method re-wires only the RNG and entity map refs — the inventory needs nothing there.
- `save/SaveService.java` — Json over `RunState` as the sole root; only customization is `setElementType(RunState.class, "enemies", ...)`. A plain-array inventory needs no analogous entry. Leave this file unchanged unless a round-trip proves otherwise.
- **New:** `item/Inventory.java` under `com.margins.rogue.item`.

### Testing standards (Epic 1 retro learnings — apply, don't rediscover)
- **Build quirk:** `-pl desktop exec:java` resolves `core` from `~/.m2`, so **run `mvn -o -pl core install` after code changes** before launching, or you'll boot a stale artifact. [Source: epic-1-retro-2026-07-17.md]
- Verification pattern established across Epic 1/2: a throwaway `main` against `core/target/classes` for headless assertions (round-trip, rule checks) **plus** a live boot on display `:0`. There is still no committed JUnit suite (open Epic 1 action item) — match the existing ad-hoc pattern for this story unless you also want to stand up JUnit (out of scope here).
- Prefer proving AC-1/AC-2 headlessly (fill/overflow/equip/round-trip) since there is no in-game pickup path to exercise them through yet.

### References
- [Source: _bmad-output/planning-artifacts/epics.md#Epic 3 · Story 3.1]
- [Source: ARCHITECTURE-SPINE.md#AD-12, #AD-3, #AD-2, #AD-6, #AD-1, #Structural Seed]
- [Source: core/src/main/java/com/margins/item/Inventory.java (model to reuse), com/margins/item/Item.java (rendering coupling to avoid)]
- [Source: core/src/main/java/com/margins/rogue/state/RunState.java, save/SaveService.java]
- [Source: _bmad-output/implementation-artifacts/epic-1-retro-2026-07-17.md (build/verify quirks, persistence-for-free)]

### Project Structure Notes
- New class path `com.margins.rogue.item.Inventory` matches the architecture Structural Seed. Package-by-feature under `rogue/`, one class per file, `Rogue*`/`*System` naming conventions do not apply to `Inventory` (the seed names it plainly). No conflict with the legacy `com.margins.item.Inventory`, which stays frozen in place.

## Dev Agent Record

### Agent Model Used

claude-opus-4-8[1m] (via bmad-dev-story)

### Debug Log References

- `mvn -o -pl core install` → BUILD SUCCESS; full `mvn -o install` (core + desktop) → BUILD SUCCESS (no regressions).
- Headless harness (throwaway `main` vs `core/target/classes` + gdx-1.12.1.jar), mirroring the Epic 1/2 verification pattern: **37/37 checks PASS**.
  - AC-2: `BACKPACK_STACKS==8`, `EQUIPPED_SLOTS==2`.
  - AC-1 capacity: 8 distinct stacks fill; 9th distinct `tryAdd` → `BACKPACK_FULL` with **no mutation** (type absent, still 8 stacks); stacking an existing type while full still succeeds.
  - AC-1 equip: `equip` moves one item backpack→equipped (backpack count decremented); 3rd `equip` (both slots full) → false; `equip` of absent type → false; `unequip` returns it to backpack; `unequip` of empty slot → false. `drop` frees a backpack stack; dropping an equipped-only type → false.
  - AC-3 round-trip: `RunState` owns the inventory; after `toJson`→`fromJson`→`restoreAfterLoad`, backpack counts and equipped slots are identical.
- Live boot on display `:0` (`exec:java` desktop launcher, ~12s) → clean; the new `RunState.inventory` field caused no construction/runtime error.

### Completion Notes List

- Added `com.margins.rogue.item.Inventory`: the type/count stack model from `com.margins.item.Inventory` (AD-12) reused, not extended — the legacy class stays frozen for `GameScreen`/`QuestManager`. Bounded to 8 backpack stacks + 2 equipped slots (FR-9). No libGDX imports (AD-2).
- Key design choice: `tryAdd` returns `AddResult.ADDED | BACKPACK_FULL`, and `BACKPACK_FULL` **leaves the inventory unchanged**. That is the model-level contract for AC-1's "prompt to drop-or-leave" — the item is neither silently accepted nor lost, so the caller can present a choice. Equipped slots hold one item each; `equip` decrements the backpack stack by 1 and `unequip` returns 1 (failing if the backpack is full, per "respecting backpack capacity").
- Owned on `RunState` as a plain (non-`transient`) field of int arrays, so it serializes under the existing `SaveService` root for free (AD-6) — no `setElementType`/serializer needed, confirmed by round-trip. `restoreAfterLoad()` left untouched; the inventory holds no tilemap/RNG back-reference so it needs no re-wiring.
- **Scope honored:** no `RogueGameScreen` changes and no floor-item entity — there is no pickup source until Story 3.2. Task 3's optional marker comment was **intentionally omitted** (there is no natural pickup call site in the screen yet; a bare comment would be orphaned noise). Story 3.2 wires the pickup trigger + drop-or-leave prompt UI; Story 3.3 introduces concrete `SupplyType` ids (the inventory stores opaque `int` types today).

### File List

- ADDED: core/src/main/java/com/margins/rogue/item/Inventory.java
- MODIFIED: core/src/main/java/com/margins/rogue/state/RunState.java

## Change Log

- 2026-07-19: Implemented Story 3.1 — finite `Inventory` (8 backpack / 2 equipped) owned by `RunState`, with `tryAdd`/`equip`/`unequip`/`drop` and a non-mutating `BACKPACK_FULL` outcome; save/load-safe. Verified 37/37 headless + clean live boot. Status → review.
- 2026-07-19: Addressed code review — 2 patches applied (field-initialized `inventory` for load-safety against pre-field saves; reserved non-negative type ids in class doc), 2 findings deferred to Story 3.2 (equip UX). Re-verified 37/37 headless + inventory-less-save load + clean full build & live boot. Status → done.

---
baseline_commit: 370f66324a5b55f5fb9b661100872155e4d1a8be
---

# Story 3.2: Use and drop items

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As Justine (player),
I want to use or drop what I carry,
so that I can spend supplies and shed weight (FR-10).

## Acceptance Criteria

1. **Given** a consumable, **When** I use it, **Then** its effect applies and it is removed from the backpack. (FR-10)
2. **Given** any carried item, **When** I drop it, **Then** it appears on Milek's tile and can be re-picked. (FR-10)
3. **Given** an item on Milek's tile, **When** I pick it up (dedicated key), **Then** it enters the backpack; **and** if the backpack is full it triggers the drop-or-leave prompt from Story 3.1 (the pickup is not silently lost). (FR-9/10, AD-12)

**Architectural definition-of-done (required for the feature to work in the existing system):**

4. Use/drop/pickup are turn actions handled inside the `TurnEngine` PlayerAction step (AD-4); no game rule is added to `RogueGameScreen` (AD-2). Floor items live on `RunState` (AD-3) and survive save/load (AD-6). Supplies and floor items are seeded from `RunState`'s RNG so placement is reproducible per seed (AD-5). Supplies render only on currently-visible tiles (epics.md §Story 2.1 note).

## Product decisions (locked with Justine, 2026-07-19)

- **Item/use model:** introduce concrete Supply types with **known** effects now; Story 3.3 layers the per-seed randomized *identity* on top of these same types, Story 3.4 hides them until first use. Use the **PRD Route-1 Supply set** (not placeholder names) so 3.3/3.4 need no renaming.
- **Pickup:** a **dedicated key (G)** while standing on an item — not auto-pickup on step.

## Tasks / Subtasks

- [x] **Task 1 — `Supply` enum (model, known effects)** (AC: 1)
  - [x] Create `core/src/main/java/com/margins/rogue/item/Supply.java`. Enum of the 5 PRD Route-1 types: `WRAPPED_BUNDLE`, `SEALED_WATERSKIN`, `SMALL_TIN`, `FOLDED_CLOTH`, `SEALED_LETTER`. Each carries a `displayName` (e.g. "Wrapped Bundle").
  - [x] Each type applies a **known first-pass effect** via the existing `RoguePlayer` API (`eat(int)` caps hunger at 100, `heal(int)` caps HP at maxHp):
    - `WRAPPED_BUNDLE` → `player.eat(40)` (PRD Balance: bread +40)
    - `SEALED_WATERSKIN` → `player.eat(15)` (clean water, minor — first-pass, tunable)
    - `SMALL_TIN` → `player.heal(4)` (rendered fat, minor HP — first-pass)
    - `FOLDED_CLOTH` → `player.heal(6)` (bandages — first-pass)
    - `SEALED_LETTER` → **inert**: no effect, and it is **not consumed** (Milek can't read it)
  - [x] Expose `void apply(RoguePlayer p)` (returns nothing; mutating the RunState-owned player is a model rule, AD-2 permits — no libGDX imports), `boolean isConsumedOnUse()` (false only for `SEALED_LETTER`), `String displayName()`, and `static Supply byOrdinal(int)`.
  - [x] The backpack stores each Supply as its `ordinal()` (non-negative — honors the `Inventory` sentinel-reservation invariant). Add a helper mapping `ordinal ↔ Supply`.
  - [x] Note in the class doc: effects are **known** here; Story 3.3 randomizes them per seed and Story 3.4 hides the name until first use.
- [x] **Task 2 — `FloorItem` + `RunState` ownership & seeding** (AC: 2, 3, 4)
  - [x] Create `core/src/main/java/com/margins/rogue/item/FloorItem.java`: fields `int type, int count, int x, int y`, a no-arg constructor (for libGDX Json, mirroring `RoguePlayer`/`NoiseEvent`) and a full constructor.
  - [x] On `RunState`: add `private List<FloorItem> floorItems = new ArrayList<>();` (**field-initialized**, non-`transient` — persisted and non-null after load, matching the `inventory` fix). Add `getFloorItems()`, `addFloorItem(int type,int count,int x,int y)`, and `FloorItem takeItemAt(int x,int y)` (removes and returns the first item on that tile, or null).
  - [x] In `RunState.generateFloor()` (where enemies are already scattered from `rng`), scatter a few Supplies per floor: pick ~2–4 walkable, non-start tiles via `rng` and place `FloorItem`s of a `rng`-chosen `Supply`. Reproducible per seed (AD-5). Keep it small — this is enough to demo pickup; Story 3.3 governs identity, not spawn counts.
  - [x] In `SaveService.json()`, add `json.setElementType(RunState.class, "floorItems", FloorItem.class)` (same pattern as `enemies`) so the list round-trips.
- [x] **Task 3 — Turn actions: USE / DROP / PICKUP** (AC: 1, 2, 3, 4)
  - [x] `PlayerAction`: add `Kind.USE, Kind.DROP, Kind.PICKUP`; add an `int itemType` field (default -1); factories `use(int type,int dir)`, `drop(int type,int dir)`, `pickup(int dir)`.
  - [x] `TurnEngine.advance` switch:
    - `USE`: `Supply s = Supply.byOrdinal(action.itemType)`; `s.apply(player)`; if `s.isConsumedOnUse()` → `inventory.remove(itemType,1)`; message (e.g. "Used " + name, or "Milek can't read it."). `acted = true`.
    - `DROP`: drop the whole highlighted stack — `int n = inventory.count(type)`; `inventory.drop(type)`; `state.addFloorItem(type, n, px, py)`; message "Dropped " + name. `acted = true`.
    - `PICKUP`: `FloorItem it = state.takeItemAt(px,py)`; if null → `acted = false` (no item, no turn spent). Else `AddResult r = inventory.tryAdd(it.type, it.count)`; if `ADDED` → message "Picked up " + name, `acted = true`; if `BACKPACK_FULL` → **put the item back** (`state.addFloorItem(it.type,it.count,px,py)`), `acted = false` (no wasted turn). The full case is surfaced by the screen (below), which checks fullness *before* submitting PICKUP, so this branch is a safety net.
  - [x] Keep all effect/inventory/floor mutation in the engine/model; the screen only forwards intents (AD-2).
- [x] **Task 4 — Screen wiring: pickup key, inventory panel, drop-or-leave prompt** (AC: 1, 2, 3)
  - [x] **Render floor items** in `renderWorld()`: for each `FloorItem` on a `tileMap.isVisible(x,y)` tile, draw a simple placeholder marker (a glyph/small colored square via the existing `font`/`shapes` — no dedicated art; a supply-art pass is a later art workstream). FOV-gate exactly like enemies.
  - [x] **Pickup [G]:** in `readAction`/`handleInput`, if a floor item is on the player's tile: if `state.getInventory().isBackpackFull()` **and** the item's type is not already a matching stack → open the **drop-or-leave prompt** (modal, screen-side; no turn yet). Otherwise submit `PlayerAction.pickup(facing)`.
  - [x] **Inventory panel [I]:** a modal that suspends turn processing (like the existing `waitingForInput` gate — no turn advances while it's open). List backpack stacks (`displayName` + count) and the 2 equipped slots; a cursor moves with W/S or ↑/↓. `[U]` uses the highlighted stack (submits `use(type)`), `[X]` drops the highlighted stack (submits `drop(type)`); using/dropping commits one turn and closes the panel. `[I]`/`[ESC]` closes without acting. Reading state to render the list is not a rule (AD-2 ok); the mutations go through actions.
  - [x] **Drop-or-leave prompt:** on a full-backpack pickup, show "Backpack full — [D] drop a stack / [L] leave". `[L]` cancels (no turn). `[D]` opens the inventory panel in "make-room" mode: selecting a stack submits its `drop(type)` (stack lands on the tile) and then completes the pickup of the ground item. Keep the two modal states clearly separated in the screen's input handler.
  - [x] Update the HUD hint line to include `G pick up   I items`.
- [x] **Task 5 — Verification** (AC: 1, 2, 3, 4)
  - [x] Headless harness (throwaway `main` vs `core/target/classes` + gdx jar), extending the 3.1 pattern:
    - `Supply.apply`: `WRAPPED_BUNDLE` raises hunger (capped 100); `FOLDED_CLOTH`/`SMALL_TIN` raise HP (capped maxHp); `SEALED_LETTER` changes nothing and `isConsumedOnUse()==false`.
    - Engine `USE` applies effect and removes 1 (except inert letter, which is not removed); `DROP` moves a whole stack onto the tile and frees the backpack slot; `PICKUP` adds a tile item and clears it; a full-backpack PICKUP of a new type puts the item back and spends no turn.
    - Seeding: two `RunState`s with the same seed produce identical `floorItems` (positions, types, counts); different seeds differ.
    - Json round-trip: `floorItems` and inventory identical after `toJson`→`fromJson`→`restoreAfterLoad` (with `setElementType` in place).
  - [x] `mvn -o -pl core install` then live boot on display `:0` (~8s) → clean; manually confirm items render on visible tiles and the panel/prompt open (best-effort; input can't be scripted headlessly).

### Review Findings

_Adversarial code review 2026-07-19 (Blind Hunter · Edge Case Hunter · Acceptance Auditor). Acceptance Auditor verdict: all 3 functional ACs (FR-10 use/drop, pickup + drop-or-leave) and the AD DoD (AD-2/3/4/5/6, non-negative ordinals, PRD Balance alignment, scope discipline) satisfied — no blocking issues. 4 findings dismissed (Inventory null-array-on-load — disproven by verified round-trips; corrupt-save type wedge — impossible in normal play, floorItems only hold valid ordinals; room-0 scatter coupling — consistent with existing enemy placement; modal state not persisted across OS pause — no corruption)._

**Patch:**

- [x] [Review][Patch] Make-room auto-pickup now guarded behind `state.getPlayer().isAlive()`, so a turn can no longer run on a dead player if the drop turn was lethal [core/src/main/java/com/margins/rogue/RogueGameScreen.java, handleInventoryInput make-room branch]
- [x] [Review][Patch] Inert Sealed Letter use no longer spends a turn — `acted=true` moved inside the `isConsumedOnUse()` branch; the "can't read it" message still shows but no turn passes (matches the no-op-costs-no-turn precedent). Verified: consumable use ticks hunger, letter use does not [core/src/main/java/com/margins/rogue/system/TurnEngine.java, USE case]

**Deferred (later inventory/UX refinement):**

- [x] [Review][Defer] Make-room drop→pickup spends two turns (enemies double-move) for one gesture; a single-turn swap is a later refinement [core/src/main/java/com/margins/rogue/RogueGameScreen.java] — deferred, disclosed MVP limitation
- [x] [Review][Defer] Multiple stacks on one tile: pickup/marker act on the first with no disambiguation, and the marker shows no count; add per-tile stacking or a chooser [core/src/main/java/com/margins/rogue/state/RunState.java, takeItemAt] — deferred, UX polish

## Dev Notes

### Governing architecture (read before coding)
- **AD-4 — Ordered turn pipeline.** USE/DROP/PICKUP are handled in the PlayerAction step of `TurnEngine.advance`; a successful use/drop/pickup sets `acted = true` so the rest of the turn (Hunger → AI → Noise → FOV) runs, exactly like MOVE/ATTACK today. A no-op pickup (no item / full) sets `acted = false` so it costs no turn — same as the existing "walk into a wall" behavior. [Source: ARCHITECTURE-SPINE.md#AD-4; core/src/main/java/com/margins/rogue/system/TurnEngine.java]
- **AD-2 — Model ⟵ Screen layering.** The screen renders the panel/prompt and forwards `PlayerAction`s; all effect/inventory/floor mutation lives in `TurnEngine`/model. `Supply.apply(player)` mutating the RunState-owned player is a model rule and stays libGDX-free. [Source: ARCHITECTURE-SPINE.md#AD-2]
- **AD-3 / AD-6 — RunState owns run data and is the save unit.** `floorItems` joins `inventory` as a plain, non-transient `RunState` field → persisted for free, provided `SaveService` gets a `setElementType` for the list element (libGDX needs the concrete type for generic lists, as it already does for `enemies`). [Source: ARCHITECTURE-SPINE.md#AD-3, #AD-6; core/src/main/java/com/margins/rogue/save/SaveService.java]
- **AD-5 — Single seeded RNG.** Supply placement draws only from `RunState.rng()` inside `generateFloor()` (where enemy scatter already does), so a seed reproduces the floor's items. No `new Random()`. [Source: ARCHITECTURE-SPINE.md#AD-5; core/src/main/java/com/margins/rogue/state/RunState.java:48]
- **AD-12 — Ratified stackable inventory.** Pickup uses `Inventory.tryAdd` (from Story 3.1); the full-backpack case reuses the `BACKPACK_FULL` contract to drive the drop-or-leave prompt. Supply types are stored as non-negative `ordinal()` ids — honoring the `-1` empty-sentinel reservation added to `Inventory`. [Source: ARCHITECTURE-SPINE.md#AD-12; core/src/main/java/com/margins/rogue/item/Inventory.java]

### Builds directly on Story 3.1 (just completed)
- `Inventory` already provides `tryAdd(type,amount)→{ADDED,BACKPACK_FULL}`, `remove(type,amount)`, `drop(type)` (drops a whole stack), `count(type)`, `isBackpackFull()`, `equippedType(slot)`. **Reuse these — do not reimplement.** `RunState.getInventory()` exposes it. [Source: _bmad-output/implementation-artifacts/3-1-backpack-and-equipped-slots.md]
- 3.1 deferred two items to this story's UI layer (see `deferred-work.md`): (a) `equip` allows the same type in both slots — only relevant if you surface an equip verb; (b) equipped items can get stuck when the backpack is full (`unequip` refuses, `drop` only scans backpack). **This story's AC is FR-10 (use/drop), which does not require an equip UI** — you may leave equip UNsurfaced and both deferred items open. If you do add an equip key to the panel, address (b) with an unequip-to-tile path. Recommendation: keep equip out of 3.2 to hold scope; note it stays deferred.

### Current state of files you will touch
- `system/PlayerAction.java` — immutable intent with `Kind {MOVE,ATTACK,BLOCK,WAIT}`, fields `dx,dy,dir`, static factories. Add three kinds + an `itemType` field + factories. [Source: core/src/main/java/com/margins/rogue/system/PlayerAction.java]
- `system/TurnEngine.java` — the `switch(action.kind)` sets `acted`; after `acted`, runs Hunger/AI/Noise/FOV. Add three cases. Messages go into `result.messages` (screen shows the last). [Source: core/src/main/java/com/margins/rogue/system/TurnEngine.java]
- `RogueGameScreen.java` — `handleInput()` gates on `waitingForInput`; `readAction()` maps keys → `PlayerAction`; `renderWorld()` FOV-gates enemy drawing (mirror for floor items); `renderHUD()` has the hint line + `setMessage`. You add: floor-item rendering, `[G]`, an `[I]` panel modal, and the drop-or-leave modal. Keep the two modal states cleanly separated from normal `waitingForInput` play. [Source: core/src/main/java/com/margins/rogue/RogueGameScreen.java]
- `RoguePlayer.java` — has `eat(int)` (hunger cap 100) and `heal(int)` (HP cap maxHp); use these for effects. [Source: core/src/main/java/com/margins/rogue/RoguePlayer.java:95-101]

### Tuning
- Backpack 8 / equipped 2 (established 3.1). Hunger max 100. `WRAPPED_BUNDLE` +40 hunger is from the PRD Balance table; the other effect magnitudes are first-pass and explicitly tunable (no PRD value yet). Status-based true identities (Tainted→Weaken, Spoiled→penalty, Bandages→stop Bleed) require a status system that does not exist in the MVP — 3.2 uses only the hunger/HP effects that exist; 3.3/3.4 handle identity within those same available effects. [Source: PRD §Balance — First-Pass Tuning Table (Food Supply restore; MVP Supply set)]

### Testing standards (carried from Epic 1/2)
- **Build quirk:** run `mvn -o -pl core install` after core changes before `-pl desktop exec:java`, or you boot a stale artifact. [Source: epic-1-retro-2026-07-17.md]
- No committed JUnit suite (open Epic 1 action item) — match the established throwaway-`main` headless harness + live boot pattern used by 3.1 and every Epic 1/2 story. Prove the model (Supply/floor/turn/round-trip/seed-reproducibility) headlessly; the panel/prompt are exercised by live boot.

### References
- [Source: _bmad-output/planning-artifacts/epics.md#Epic 3 · Story 3.2 (FR-10)]
- [Source: PRD FR-9/FR-10/FR-11 and §Balance — First-Pass Tuning Table; MVP Supply set = Wrapped Bundle, Sealed Waterskin, Small Tin, Folded Cloth, Sealed Letter]
- [Source: ARCHITECTURE-SPINE.md#AD-2, #AD-3, #AD-4, #AD-5, #AD-6, #AD-12, #Structural Seed (item/Supply.java)]
- [Source: 3-1-backpack-and-equipped-slots.md (Inventory API); deferred-work.md (equip items)]

### Project Structure Notes
- New classes `com.margins.rogue.item.Supply` and `com.margins.rogue.item.FloorItem` sit beside `Inventory` in the item package — matches the architecture Structural Seed (`item/Supply.java`). `FloorItem` is a plain data holder like `NoiseEvent`. No conflict with the frozen legacy `com.margins.item.*`.

## Dev Agent Record

### Agent Model Used

claude-opus-4-8[1m] (via bmad-dev-story)

### Debug Log References

- `mvn -o -pl core install` → BUILD SUCCESS; full `mvn -o install` (core + desktop) → BUILD SUCCESS (no regressions).
- Model/engine headless harness (throwaway `main` vs `core/target/classes` + gdx-1.12.1.jar): **16/16 PASS** —
  - Supply effects: `WRAPPED_BUNDLE` raises hunger (cap 100); `FOLDED_CLOTH`/`SMALL_TIN` heal (cap maxHp); `SEALED_LETTER` inert + `isConsumedOnUse()==false`.
  - Engine `USE` applies effect + removes 1 (letter not removed); `DROP` moves the whole stack to the player tile and frees the backpack slot; `PICKUP` grabs a tile stack and clears the tile; a full-backpack `PICKUP` of a new type returns the item and spends no turn.
  - Same-seed `RunState`s produce identical `floorItems`; Json round-trip preserves `floorItems` + inventory (with `setElementType`).
- 3.1 regression harness re-run after the `Inventory` accessor additions: **37/37 PASS**.
- Live boot on display `:0` (~12s) → clean; screen compiles and runs with the new floor-item rendering, `[G]` pickup, `[I]` panel, and drop-or-leave prompt. (Input can't be scripted headlessly, so the modal UI is verified by clean boot + the model-layer harness, matching the Epic 1/2 verification bar.)

### Completion Notes List

- **Supply model:** `com.margins.rogue.item.Supply` = the 5 PRD Route-1 types with KNOWN first-pass effects (Wrapped Bundle +40 hunger per PRD Balance; others tunable) via the existing `RoguePlayer.eat/heal`. Sealed Letter is inert and not consumed. Stored in the backpack as non-negative `ordinal()` (honors the 3.1 sentinel reservation). Effects are known here by design; Story 3.3 randomizes per seed, 3.4 hides until first use — the enum needs no renaming.
- **Floor items:** `FloorItem {type,count,x,y}` on a field-initialized, non-transient `RunState.floorItems` list (persisted; `SaveService` gets a `setElementType` like `enemies`). `generateFloor()` scatters 2–4 supplies per floor from the seeded RNG (reproducible, AD-5); the list is cleared on regen but preserved on load.
- **Turn actions:** `PlayerAction` gains `USE/DROP/PICKUP` (+`itemType`); `TurnEngine` handles them in the PlayerAction step (AD-4). USE applies+consumes, DROP sheds a whole stack to the tile, PICKUP adds a tile stack; a failed/empty pickup spends no turn (matches the "walk into a wall" precedent). All mutation stays in the model (AD-2).
- **Screen:** floor items render as a gold marker only on visible tiles (FOV-gated like enemies); `[G]` picks up (or opens the drop-or-leave prompt when the pack is full and the type doesn't stack); `[I]` opens a modal backpack panel (W/S select, `[U]` use, `[X]` drop) that suspends turn play; the drop-or-leave prompt's `[D]` opens the panel in make-room mode (drop a stack → auto-pick-up the ground item), `[L]` leaves it. The screen only reads state and forwards `PlayerAction`s.
- **Scope:** equip UI intentionally left out (FR-10 is use/drop); the two 3.1-deferred equip items remain open in `deferred-work.md`. Known minor: the make-room drop→pickup swap spends two turns (drop, then pickup); acceptable for MVP and noted for a later single-turn refinement if desired.

### File List

- ADDED: core/src/main/java/com/margins/rogue/item/Supply.java
- ADDED: core/src/main/java/com/margins/rogue/item/FloorItem.java
- MODIFIED: core/src/main/java/com/margins/rogue/item/Inventory.java (added `backpackType`/`backpackCount` read accessors for the panel)
- MODIFIED: core/src/main/java/com/margins/rogue/state/RunState.java (floorItems field + accessors + seeded scatter)
- MODIFIED: core/src/main/java/com/margins/rogue/save/SaveService.java (floorItems element type)
- MODIFIED: core/src/main/java/com/margins/rogue/system/PlayerAction.java (USE/DROP/PICKUP + itemType)
- MODIFIED: core/src/main/java/com/margins/rogue/system/TurnEngine.java (USE/DROP/PICKUP handling)
- MODIFIED: core/src/main/java/com/margins/rogue/RogueGameScreen.java (floor-item render, pickup key, inventory panel, drop-or-leave prompt)

## Change Log

- 2026-07-19: Implemented Story 3.2 — Supply enum (5 Route-1 types, known effects) + FloorItem on RunState (seeded, persisted); USE/DROP/PICKUP turn actions; screen wiring for pickup key, inventory panel, and the drop-or-leave prompt (wires 3.1's BACKPACK_FULL hook). Verified 16/16 model harness + 37/37 3.1 regression + clean full build & live boot. Status → review.
- 2026-07-19: Addressed code review — 2 patches applied (guard make-room auto-pickup behind `isAlive()`; inert Sealed Letter use no longer spends a turn), 2 findings deferred to inventory/UX polish. Re-verified 18/18 model harness (incl. new turn-cost checks) + 37/37 3.1 regression + clean full build & live boot. Status → done.

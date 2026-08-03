---
baseline_commit: 4dab3deb19ac14acd1c6cab789aa0b8503035289
---

# Story 5.3: Scene and quest flags

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As the developer,
I want authored scenes to set/read run-scoped flags,
so that content can gate on what the player has done (FR-8).

## Acceptance Criteria

1. **Given** a dialogue scene reaches a node that carries a flag effect, **When** that node is entered, **Then** the flag is written to the `RunState` `FlagStore` (AD-7) — dialogue writes narrative state only through the store. (FR-8, AD-7)
2. **Given** the "cache revealed" flag is set, **When** later content reads it, **Then** it gates correctly — the cache contents spawn — and it happens exactly once (a second read does not re-spawn). (FR-8)
3. **Given** the "cache revealed" flag is **not** set, **When** the same content check runs, **Then** nothing spawns. (FR-8, negative gate)
4. **Given** a flag has been set, **When** the run is saved and reloaded, **Then** the flag persists (and the one-shot content stays resolved — it does not re-spawn after reload). (AD-6, AD-7)
5. **Given** the 5.2 INSTINCT-gated read succeeds in the sample scene, **When** it resolves, **Then** entering the success node sets "cache revealed" — closing the FR-7→FR-8 chain (a cunning read reveals the cache, UJ-1). (FR-7+FR-8)

**Architectural definition-of-done:**

6. Flag writes go through `RunState.getFlagStore()` (AD-7) from the **model** dialogue layer, never the screen. The content read-gate is a **model** operation (no libGDX). Flags persist as part of the `FlagStore` already serialized under `RunState` (AD-6, from Story 4.3 — no new save wiring). The turn-suspension (5.1) and INSTINCT routing (5.2) are unchanged.

## Product decisions (recommended defaults baked in)

- **The flag WRITE lives on the node (entered → flag set), driven by the model controller.** Add optional `String setFlagKey` (default null) + `int setFlagValue` (default 1) to the reused `com.margins.dialog.DialogNode`. When the controller makes a node current (open or advance), it applies that node's flag effect through `RunState.getFlagStore().set(key, value)` (AD-7). Node-entry (not option-selection) is the right hook: it composes cleanly with 5.2's gated routing — the gated **success node** carries "cache.revealed", so passing the INSTINCT check → enter success node → flag set (closes FR-7→FR-8, AC-5). Additive to `DialogNode`, backward-compatible (5.1/5.2 nodes without a key set nothing).
- **Consolidate the controller's dependency onto `RunState`.** 5.1 gave `DialogController` a node graph; 5.2 threaded `int instinct` into `select`; 5.3 needs the `FlagStore` too. Rather than grow the parameter list, change `start(DialogNode)`→`start(DialogNode, RunState)` and `select(int, int instinct)`→`select(int, RunState)`. The controller reads what it needs from the model root — `state.getPlayer().getInstinct()` for the gate (5.2 logic unchanged) and `state.getFlagStore()` for the flag effect. `RunState` is pure model, so this respects AD-2 (no libGDX in the controller). Both routes go through one private `enter(DialogNode, RunState)` that sets `current` and applies the node's flag effect — so open and advance both set flags consistently.
- **The READ-gate is a tiny model reader: `SceneEffects.applyCacheReveal(RunState)`.** It reads `cache.revealed`; if set and `cache.spawned` is not, it spawns the cache and sets `cache.spawned` (the one-shot guard, AC-2/AC-4). AC-3 falls out (no flag → no spawn). This is the concrete "content reads the flag and gates" half of FR-8. Keep it a small `narrative/SceneEffects` model class with the flag-key constants as the single authority (mirrors 4.3's tag-authority and 3.4's naming-authority pattern) — `KEY_CACHE_REVEALED = "scene.cache.revealed"`, `KEY_CACHE_SPAWNED = "scene.cache.spawned"`. The screen calls it after a dialogue closes (a plain model call — no rule in the screen, AD-2).
- **"Cache contents" = one supply stack at the player's tile.** Minimal honest spawn: `state.addFloorItem(0, 1, player.tileX, player.tileY)` (a single Supply stack, pick-up-able with `G`). A richer authored cache (placement, multiple items) is Epic 6; 5.3 proves the gate with one visible item. The `cache.spawned` guard makes it one-shot across turns *and* reloads (the guard flag persists).
- **Persistence is already solved (4.3) — this story only re-verifies it.** `FlagStore` serializes under `RunState` and its map value-type is registered in `SaveService` (4.3). New keys are just more entries — no save wiring. The one-shot guard means a reload after the cache spawned does NOT re-spawn (both `cache.revealed` and `cache.spawned` persist), which the harness checks (AC-4).
- **"Quest flags" are just more `FlagStore` keys — `QuestManager` stays out.** FR-8 is run-scoped flags in the store (AD-7); a lightweight quest is a set of such flags. The legacy `com.margins.quest.QuestManager` targets the legacy `com.margins.item.Inventory` (wrong stack) and full quest tracking is Epic 6+ — do not wire it here (consistent with 5.1/5.2). The generic `FlagStore.get/set` already supports arbitrary quest flags.

## Tasks / Subtasks

- [x] **Task 1 — Node flag effect (reuse + extend, FR-8/AD-7)** (AC: 1, 5)
  - [x] Added `public String setFlagKey;` (default null) and `public int setFlagValue = 1;` to `DialogNode`, plus `public DialogNode withFlag(String key, int value)` (chainable). Additive — nodes without a key are unaffected (5.1/5.2 + legacy screen keep compiling).

- [x] **Task 2 — Controller applies flag effects via `RunState` (AD-7/AD-2)** (AC: 1, 5, 6)
  - [x] Changed `start(DialogNode)` → `start(DialogNode, RunState)` and `select(int, int instinct)` → `select(int, RunState)`.
  - [x] Added `private void enter(DialogNode node, RunState state)`: sets `current`, and writes `node.setFlagKey`/`setFlagValue` via `state.getFlagStore().set(...)` when present (AD-7).
  - [x] `start` → `enter(root, state)`; `select` keeps the 5.2 gate but reads instinct from `state.getPlayer().getInstinct()`, computes the target, then `enter(target, state)`. Guards unchanged. Imports `RunState`; no libGDX, no `Random`.

- [x] **Task 3 — `SceneEffects` read-gate (FR-8 read side)** (AC: 2, 3, 4, 6)
  - [x] Created `narrative/SceneEffects.java` (pure model) with `KEY_CACHE_REVEALED = "scene.cache.revealed"` and `KEY_CACHE_SPAWNED = "scene.cache.spawned"` (single key authority).
  - [x] `applyCacheReveal(RunState)`: if revealed and not yet spawned, `addFloorItem(0, 1, player.tileX, player.tileY)` and set the spawned guard. One-shot; no-op when unrevealed (AC-3) or already spawned (AC-2/4).

- [x] **Task 4 — Wire the demo end-to-end** (AC: 2, 5)
  - [x] `SampleDialog`'s INSTINCT success node now `.withFlag(SceneEffects.KEY_CACHE_REVEALED, 1)` — passing the read reveals the cache (AC-5, UJ-1).
  - [x] `RogueGameScreen`: `T` trigger → `dialog.start(SampleDialog.build(), state)`; `handleDialogueInput()` → `dialog.select(choice, state)`. Centralized the scene-close paths into `closeScene()` which ends the scene, returns to `PLAY`, and calls `SceneEffects.applyCacheReveal(state)` — a single model call, no gate logic in the screen (AD-2).

- [x] **Task 5 — Verification** (AC: 1, 2, 3, 4, 5, 6)
  - [x] Headless harness (throwaway `FlagHarness`, real `RunState`) — **13/13 checks passed**:
    - write: entering a flagged node writes it through the FlagStore; a node without a flag writes nothing. (AC-1)
    - gated chain: sample gated read at instinct 7 → success node → `cache.revealed == 1`. (AC-5)
    - read-gate one-shot: revealed cache spawns exactly one item + sets the guard; second call spawns nothing. (AC-2)
    - negative gate: unrevealed → no spawn. (AC-3)
    - persistence: `SaveService.json()` round-trip preserves both flags; no re-spawn after reload; floor items intact. (AC-4)
    - regression: 5.2 gate routes correctly through `select(int, RunState)` (7<9→fail, 7≥5→success).
    - [x] Threw-away harness deleted after the run.
  - [x] **Live boot on `:0`:** boots clean with the flag code, loads the pre-existing save, no exceptions (ran full timeout). NOTE — interactive confirmation (press `T`, pick the `[INSTINCT]` read at instinct 7, close the scene, see a cache item on Milek's tile to grab with `G`) is a **human check**; flagged for Justine.

## Dev Notes

### Governing architecture
- **FR-8 — Scene/quest flags.** "Authored scenes set and read run-scoped flags that gate later content (cache revealed, reunion seen, Bond tier). Choosing the honest reunion option sets a Bond flag readable by later nodes; a set 'cache revealed' flag spawns the cache contents." 5.3 implements the set→read→spawn loop with the cache as the concrete gate. [Source: prd.md#FR-8; epics.md#Story 5.3]
- **AD-7 — Narrative state lives in `RunState.FlagStore`.** Dialogue writes/reads narrative state *only* through the store. The node effect writes via `getFlagStore().set(...)`; `SceneEffects` reads via `getFlagStore().get(...)`. [Source: ARCHITECTURE-SPINE.md#AD-7; core/.../state/FlagStore.java]
- **AD-6 — Whole-`RunState` serialization.** `FlagStore` already persists (Story 4.3: field-initialized, `setElementType(FlagStore.class,"flags",Integer.class)` in `SaveService`). New keys need no save wiring; 5.3 just re-verifies the round-trip + the one-shot-after-reload guarantee. [Source: ARCHITECTURE-SPINE.md#AD-6; core/.../save/SaveService.java; 4-3-bond-tracking.md]
- **AD-2 — Rules in the model; no libGDX in the model; no rule in the screen.** The flag write (controller) and the read-gate (`SceneEffects`) are model operations; the screen just calls `SceneEffects.applyCacheReveal(state)` after a scene closes. [Source: ARCHITECTURE-SPINE.md#AD-2]

### Files being modified / added — current state and what to preserve
- **`com.margins.dialog.DialogNode`** (UPDATE — legacy, shared; already extended in 5.2 with `instinctThreshold`/`failNext`): **add** `setFlagKey`/`setFlagValue` + a `withFlag(...)` helper. Additive; the `(text, options...)` constructors and 5.2's `DialogOption` fields are untouched; the legacy `com.margins.screen.GameScreen` keeps compiling.
- **`narrative/DialogController.java`** (UPDATE — 5.1/5.2): **change** `start`/`select` to take `RunState`, route both through `enter(...)` which applies the node's flag effect. **Preserve** the 5.2 INSTINCT routing (now reading instinct from `RunState`) and the 5.1 open/close semantics. Pure model.
- **`narrative/SceneEffects.java`** (NEW) — the read-gate + flag-key authority. Pure model.
- **`narrative/SampleDialog.java`** (UPDATE — 5.1/5.2): success node sets the cache flag. Placeholder content.
- **`RogueGameScreen.java`** (UPDATE — 5.1/5.2): pass `state` into `start`/`select`; call `SceneEffects.applyCacheReveal(state)` when a scene closes. **Preserve** the `DIALOGUE` suspension gate, the `[INSTINCT]` marker, and all other modes.
- **REUSE UNCHANGED:** `FlagStore` (4.3 — `get`/`set` already do everything needed), `RunState.addFloorItem`, `SaveService` (no new registration). **DO NOT TOUCH:** `TurnEngine`, `SaveService` element types, `RunState` save fields, `QuestManager` (legacy, out of scope).

### Previous-story intelligence (5.1 / 5.2)
- 5.2 changed `select` to `(int, int instinct)` and extended `DialogOption`; 5.3 consolidates `select`/`start` onto `RunState` (single model dependency) — the only callers are in `RogueGameScreen` (the `T` trigger + `handleDialogueInput`), so two call-site updates.
- Pattern held across 5.1/5.2: pure-model classes + throwaway harness (`DialogHarness` 15 checks, `InstinctHarness` 12 checks) + `:0` boot; `FlagStore` round-trip is already proven (4.3's `BondHarness`). Reuse `SaveService.json()`'s element types (incl. `FlagStore.flags`) in the persistence check.
- `RoguePlayer.getInstinct()` = 7 default; `RunState.getFlagStore()` (4.3), `addFloorItem(type,count,x,y)`, `getPlayer().getTileX/Y()` all exist. Baseline for all of Epic 5 is `4dab3de`.

### Scope boundary
- **IN:** node-entry flag write via the controller+`FlagStore` (AD-7); the `SceneEffects` cache read-gate (one-shot); the sample success node setting the flag; screen wiring (pass `RunState`, call the reader on scene close); persistence re-verification.
- **OUT:** `QuestManager` wiring (legacy `Inventory`-bound; Epic 6+); authored scenes / the real opening + reunion (**Epic 6**); Bond-flag-driven dialogue tone (the Bond store + tiers exist from 4.3; authoring nodes that read the tier is Epic 6 content); richer cache contents/placement (Epic 6); any new save wiring (persistence already done in 4.3).

### Testing standards
- No committed JUnit suite yet (open Epic 1/3/4 retro item) — throwaway `main` harness (headless, real `RunState` + libGDX `Json` for the round-trip, both headless-safe) + `:0` live boot, per every prior story. **Build quirk:** `mvn -o -pl core install` before `exec:java`.

### Project Structure Notes
- Flag effect on the **node** (entry), not the option — composes with 5.2's gated routing so the success node reveals the cache.
- Flag-key constants live in `SceneEffects` as the single authority; `SampleDialog` references them (no stringly-typed duplication).
- One-shot guard (`cache.spawned`) is itself a persisted flag, so "don't re-spawn after reload" is free via AD-6.

### Review Findings

Code review 2026-08-03 (Blind Hunter + Edge Case Hunter + Acceptance Auditor, parallel). AC verdict: 5.3 AC 1–6 all satisfied (Auditor confirmed the full FR-6→FR-7→FR-8 chain and save/reload persistence).

- [x] [Review][Patch] Reveal saved mid-scene never spawns on reload [core/src/main/java/com/margins/rogue/RogueGameScreen.java (show)] — the `DialogController` is transient view state, so quitting the app while the reveal node is displayed (flag set, scene still open) persists `cache.revealed=1/cache.spawned=0`; on reload the scene is gone and `applyCacheReveal` (only called from `closeScene()`) never fires → the cache silently never spawns. **Fixed 2026-08-03:** `show()` now calls `SceneEffects.applyCacheReveal(state)` after load, consuming a pending reveal at the (unchanged) reload tile. Rebuilt + booted clean. (blind+edge) [Med]
- [x] [Review][Defer] `SceneEffects` cache keys are single-run-scoped globals; a 2nd authored cache would collide on `scene.cache.*` [SceneEffects.java] — deferred, fine for the one authored cache in MVP; a multi-cache scheme (keyed per scene) is an Epic 6 authoring concern. (blind) [Low]
- [x] [Review][Defer] Bond-tag dialogue wiring is absent; `FlagStore.applyBondTag` (from 4.3) is never called by the dialogue path [FlagStore.java comment vs DialogController] — deferred, no Epic 5 AC requires Bond-from-dialogue; the honest-reunion Bond raise is the Epic 6 reunion scene. The 4.3 `FlagStore` comment slightly overclaims ("Epic 5 dialogue nodes call this"); it becomes accurate when Epic 6 wires it. (blind) [Low]
- [x] [Review][Defer] Authoring-contract robustness in `applyCacheReveal` — gates on `== 1` (a `withFlag(key, 2)` would never spawn); no null-guard on `state`/`player` [SceneEffects.java] — deferred, unreachable today (the sample sets 1; the call path always has a live player). Revisit (`!= 0`, consistency guards) when authored scenes exercise non-1 values. (blind+edge) [Low]

**Dismissed (verified unreachable/false):** cache-spawns-on-unwalkable-tile — the player's tile is *always* walkable (movement is walkable-gated), so the precondition can't occur; reward-applied-to-a-dead-run — the player can't die during a scene (turns are suspended) and can't open a scene while `gameOver`, so the spawn is always on a live player.

## Dev Agent Record

### Agent Model Used
- Claude Opus 4.8 (1M context) — implementation + harness verification.

### Debug Log References
- Harness: `mvn -o -q -pl core install` (clean) then `... exec:java -Dexec.mainClass=com.margins.rogue.FlagHarness` → **ALL 13 CHECKS PASSED**.
- Smoke boot on `:0`: `timeout 10 ... DesktopLauncher` — full duration (exit 124), **zero exceptions**, pre-existing save loaded clean.

### Completion Notes List
- ✅ **Task 1 — node flag effect (FR-8):** `DialogNode` gains `setFlagKey`/`setFlagValue` + chainable `withFlag(...)`. Additive; 5.1/5.2 nodes and the legacy screen unaffected.
- ✅ **Task 2 — controller writes via FlagStore (AD-7):** consolidated `start`/`select` onto `RunState`; a private `enter(node, state)` applies the entered node's flag through `state.getFlagStore().set(...)`. `select` still runs the 5.2 INSTINCT gate (instinct now read from `RunState`). Pure model — no libGDX, no RNG.
- ✅ **Task 3 — SceneEffects read-gate (FR-8):** `narrative/SceneEffects` owns the scene flag-key constants and `applyCacheReveal(RunState)` — spawns one supply stack at the player's tile when `cache.revealed` is set and not yet spawned, guarded one-shot by the persisted `cache.spawned` flag.
- ✅ **Task 4 — demo + screen wiring:** sample INSTINCT success node sets `cache.revealed`; screen passes `state` into `start`/`select` and fires `SceneEffects.applyCacheReveal(state)` from a centralized `closeScene()`. No gate logic in the screen (AD-2).
- ✅ **Task 5 — verification:** 13/13 headless checks (write, gated FR-7→FR-8 chain, one-shot spawn, negative gate, save/reload persistence with no re-spawn, 5.2 regression) + clean `:0` boot.
- **AC coverage:** 1 ✓ (node-entry write via FlagStore) · 2 ✓ (one-shot cache spawn) · 3 ✓ (no flag → no spawn) · 4 ✓ (flags persist, no re-spawn after reload) · 5 ✓ (INSTINCT success sets the flag) · 6 ✓ (writes/read-gate in the model, no new save wiring, suspension + gate unchanged).
- **Out of scope, as specified:** `QuestManager` (legacy `Inventory`-bound), authored scenes / opening + reunion (Epic 6), Bond-tier dialogue tone (Epic 6 content), richer cache contents (Epic 6). `TurnEngine`, `SaveService` element types, and `RunState` save fields untouched (persistence rides 4.3's `FlagStore` serialization).
- **Human check outstanding:** interactive `T` → `[INSTINCT]` read → close → cache item appears on Milek's tile.

### File List
- `core/src/main/java/com/margins/dialog/DialogNode.java` (MODIFIED) — `setFlagKey`/`setFlagValue` + `withFlag()` (additive).
- `core/src/main/java/com/margins/rogue/narrative/DialogController.java` (MODIFIED) — `start`/`select` take `RunState`; `enter()` writes node flags via `FlagStore` (AD-7).
- `core/src/main/java/com/margins/rogue/narrative/SceneEffects.java` (NEW) — scene flag-key authority + one-shot cache read-gate.
- `core/src/main/java/com/margins/rogue/narrative/SampleDialog.java` (MODIFIED) — INSTINCT success node sets `cache.revealed`.
- `core/src/main/java/com/margins/rogue/RogueGameScreen.java` (MODIFIED) — pass `state` into `start`/`select`; `closeScene()` fires the read-gate.

## Change Log

- 2026-08-03 — Story 5.3 spec created: scene and quest flags (FR-8). Node-entry flag writes through `RunState.getFlagStore()` (AD-7) via a consolidated `DialogController.start/select(…, RunState)`; a `SceneEffects` one-shot read-gate spawns the cache when "scene.cache.revealed" is set; the sample INSTINCT success node sets the flag (closes FR-7→FR-8); persistence reuses 4.3's `FlagStore` serialization (AD-6). `QuestManager`, authored content, and richer cache contents are Epic 6+ / out of scope.
- 2026-08-03 — Story 5.3 implemented: `DialogNode.withFlag`, `DialogController.enter()` writes node flags via FlagStore (start/select now take RunState), new `SceneEffects` one-shot cache read-gate, sample success node sets `cache.revealed`, screen `closeScene()` fires the gate. Verified via throwaway `FlagHarness` (13 checks incl. FR-7→FR-8 chain + save/reload persistence with no re-spawn) + clean `:0` boot; harness deleted. Status → review. Interactive `T`-key check left for Justine.

---
baseline_commit: f1005e262fac838ade638e386d9bfc8d7ad90595
---

# Story 4.3: Bond tracking

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As the developer,
I want Galleon's Bond to shift from tagged dialogue choices,
so that later authored dialogue can read it for tone (FR-15).

## Acceptance Criteria

1. **Given** a fresh run, **Then** the `RunState` owns a non-null `FlagStore` with Bond at its neutral baseline (0), readable via `getBond()`, and no combat/transformation behavior is attached to it. (FR-15, AD-7, NFR-5)
2. **Given** a Bond-tagged choice (a stable tag constant), **When** its tag is applied to the store, **Then** Bond shifts by the tag's delta — an honest choice raises it, a dismissive choice lowers it. (FR-15, AD-7)
3. **Given** Bond has shifted, **When** a later read asks for the tone tier, **Then** it returns the tier derived from the current value (warm / neutral / cold) that dialogue will use to select lines. (FR-15, AD-7)
4. **Given** a scene flag set through the store, **When** it is read later, **Then** it returns the stored value. (FR-8-ready, AD-7)
5. **Given** a restart (new run), **When** the run re-initializes, **Then** narrative flags and Bond reset to baseline. (AD-7 run-scoped)
6. **Given** a save/load, **When** the whole `RunState` round-trips, **Then** the `FlagStore` contents (flags + Bond) are preserved. (AD-6)

**Architectural definition-of-done:**

7. `DialogNode`/`QuestManager` read and write narrative state **only** through the `FlagStore` on `RunState` (AD-7). The store is pure model — no libGDX types (AD-2) — and serializes under the `RunState` root (AD-6). Bond unlocks **no** combat bonus or transformation in MVP (NFR-5).

## Product decisions (recommended defaults baked in)

- **Bond is a plain int on a generic k/v store, not a bespoke field on `Companion`.** AD-7 is explicit: flags *and* Galleon's Bond live in a key/value store on `RunState` (structural seed: `state/FlagStore.java`). This is also forward-correct — Story 5.3 needs arbitrary scene flags (FR-8), so the generic store is the right altitude now, not speculative.
- **`FlagStore` = `Map<String, Integer>` with typed accessors.** `LinkedHashMap` (insertion order → deterministic serialization). `get(key)` returns 0 when never set (matches the `-1` empty-slot sentinel convention elsewhere — callers don't null-check). `set(key, value)` and `add(key, delta)` for flags; `getBond()`/`adjustBond(delta)`/`getBondTier()` for Bond.
- **Bond baseline 0 = neutral; 3 tone tiers.** `tier 0 = cold/distant` (bond ≤ −2), `tier 1 = neutral` (−1..1), `tier 2 = warm` (bond ≥ 2). Two honest choices warm Galleon; two dismissive choices chill him. The PRD's example ("honest reunion choice raises Bond; a later node reads the tier to select warmer dialogue") is exactly this shape.
- **The tag→delta mapping is the single authority.** `FlagStore` exposes `applyBondTag(String tag)` with stable `public static final String BOND_TAG_HONEST` / `BOND_TAG_DISMISSIVE` constants (→ +1 / −1; unknown tags are a no-op). Epic 5 dialogue nodes will carry a `bondTag` and call `applyBondTag` on selection — no scattered `adjustBond` literals across content. This mirrors Story 3.4's "single naming authority" pattern.
- **No dialogue UI, no quest wiring in this story.** Bond's *read* API (`getBondTier`) is what Epic 5 will call; nothing consumes it yet. The harness proves the shift + read round-trip. The legacy `com.margins.dialog.DialogNode` is untouched (its evolution is Epic 5).
- **Persistence via map element-type registration.** `json.setElementType(FlagStore.class, "flags", Integer.class)` — libGDX `setElementType` applies to map values. Mirrors the existing `enemies`/`floorItems`/`companions` registration pattern. Field-initialized so a pre-4.3 save (no `flagStore` key) loads with an empty store, not null — the same AD-6 discipline the inventory field uses.

## Tasks / Subtasks

- [x] **Task 1 — `FlagStore` (AD-7)** (AC: 1, 2, 3, 4, 6, 7)
  - [x] Create `core/src/main/java/com/margins/rogue/state/FlagStore.java` (package `com.margins.rogue.state`, beside `RunState`/`IdentifyMap` — the actual layout; the spine's naming is fine here).
  - [x] `private LinkedHashMap<String, Integer> flags = new LinkedHashMap<>();` (field-initialized, deterministic order). `public int get(String key)` → `flags.getOrDefault(key, 0)`; `public void set(String key, int value)`; `public void add(String key, int delta)`.
  - [x] Bond: `public static final String KEY_BOND = "bond.galleon";`, `public int getBond()` (`get(KEY_BOND)`), `public void adjustBond(int delta)` (`add(KEY_BOND, delta)`), `public int getBondTier()` — tier mapping per Product decisions (≤−2 → 0; ≥2 → 2; else 1).
  - [x] Tag authority: `public static final String BOND_TAG_HONEST = "bond.honest";`, `public static final String BOND_TAG_DISMISSIVE = "bond.dismissive";`, and `public void applyBondTag(String tag)` — switch on the constants (+1 / −1), default no-op. Keep it a plain model class; no RNG, no libGDX (AD-2).

- [x] **Task 2 — `RunState` owns the store (AD-3/AD-7)** (AC: 1, 5)
  - [x] Add `private FlagStore flagStore = new FlagStore();` to `RunState` — **field-initialized**, so a pre-4.3 save (Json skips the ctor) loads with an empty store, not null (same pattern as `inventory`).
  - [x] Add `public FlagStore getFlagStore() { return flagStore; }`.
  - [x] In `restart()` (the "fresh run from floor 1" method), reset it: `this.flagStore = new FlagStore();` alongside the existing `identifyMap` rebuild — narrative state is run-scoped (AD-7), and `restart()` starts a new run.
  - [x] `restoreAfterLoad()` needs **no** change — `flagStore` is a persisted field that loads with the run (like `identifyMap`); the field initializer only covers the pre-field save case.

- [x] **Task 3 — Persistence wiring (AD-6)** (AC: 6, 7)
  - [x] In `SaveService.json()`, add `json.setElementType(FlagStore.class, "flags", Integer.class);` — for a map field, `setElementType` registers the **value** type, so integers deserialize as `Integer` (not `Double`/`String`). Mirrors the existing `enemies`/`floorItems`/`companions` lines. The `flagStore` then serializes with `RunState`; no `restoreAfterLoad` work (Task 2).

- [x] **Task 4 — Verification** (AC: 1, 2, 3, 4, 5, 6, 7)
  - [x] Headless harness (throwaway `main`, run via `mvn -o -pl core install` then `exec-maven-plugin:3.1.0:java`, per the 4.1/4.2 pattern):
    - **Fresh run**: `getFlagStore()` non-null, `getBond() == 0`, `getBondTier() == 1` (neutral), no exceptions.
    - **Bond shift via tags**: `applyBondTag(BOND_TAG_HONEST)` → bond 1; again → bond 2, `getBondTier() == 2` (warm). `applyBondTag(BOND_TAG_DISMISSIVE)` × 4 → bond −2, `getBondTier() == 0` (cold). Unknown tag → bond unchanged.
    - **Generic flags**: `set("cache.revealed", 1)` → `get(...) == 1`; `add("counter", 3)` → `get("counter") == 3`; never-set key → `0`.
    - **Persistence**: Json round-trip (element types as `SaveService.json()`, incl. the `FlagStore.flags` registration) preserves Bond + flags; `restoreAfterLoad` keeps the store usable.
    - **Restart reset**: set bond 2 + a flag, `restart()` → `getBond() == 0`, flag gone.
    - **Old-save safety**: `json.fromJson(RunState.class, <JSON without flagStore key>)` → `getFlagStore()` non-null, empty.
  - [x] Live boot on `:0`: no visual change expected (narrative store is backend) — confirm the game loads the pre-existing save cleanly (no `flagStore` key → empty store). Delete the throwaway harness after the run.

## Dev Notes

### Governing architecture
- **AD-7 — Narrative state lives in `RunState`.** Run-scoped flags and Galleon's Bond live in a key/value store on `RunState`; `DialogNode`/`QuestManager` read and write narrative state **only** through that store. This story builds the store; the readers (dialogue/quests) arrive in Epic 5. [Source: ARCHITECTURE-SPINE.md#AD-7]
- **AD-6 — Save = serialize whole `RunState`.** `flagStore` is a persisted field on the root; the map's value type is registered in `SaveService.json()`. Field-initialized so pre-4.3 saves load empty-but-non-null (same discipline as `inventory`, 3.1). [Source: ARCHITECTURE-SPINE.md#AD-6; core/src/main/java/com/margins/rogue/save/SaveService.java]
- **AD-2 — Model owns state; no libGDX in the model.** `FlagStore` is pure model (a `LinkedHashMap<String,Integer>` + typed accessors). [Source: ARCHITECTURE-SPINE.md#AD-2]
- **FR-15 — Bond, narrative-only.** Bond changes from tagged dialogue choices and is readable by dialogue nodes. **NON-GOAL for MVP: Bond unlocking Alpha transformation or combat bonuses (NFR-5).** [Source: PRD.md#FR-15; epics.md#Story 4.3]
- **AD-3 — RunState is the single owner.** The store is owned by `RunState` and reset on `restart()` (run-scoped). [Source: ARCHITECTURE-SPINE.md#AD-3]

### Files being modified / added — current state and what to preserve
- **NEW:** `state/FlagStore.java` — the only new file. Generic k/v store + Bond + tag authority (AD-7).
- **`state/RunState.java`** (UPDATE, built up through 4.1/4.2): has `tileMap/player/enemies/inventory/floorItems/companions/identifyMap/saveVersion/floorDepth/seed/rng/lastStand*/noiseQueue`, `restart()` (resets floorDepth, lastStand, identifyMap, respawns companion), `descend()`, `restoreAfterLoad()` (re-injects maps). **Add** the `flagStore` field + getter, and reset it in `restart()`. **Do not touch** `descend()`/companion/`placeFloorActors` — 4.1/4.2 rely on them.
- **`save/SaveService.java`** (UPDATE, built in 4.1): registers element types for `enemies`/`floorItems`/`companions`. **Add** the `FlagStore.flags` map-value registration. Nothing else.
- **Untouched:** `Companion`, `CompanionSystem`, `TurnEngine`, `PlayerAction`, `RogueGameScreen`, `NoiseSystem`, legacy `com.margins.dialog.DialogNode` (its evolution is Epic 5).

### Scope boundary
- **IN:** the `FlagStore` (generic k/v + Bond + tag authority) on `RunState`, restart reset, persistence wiring, verification.
- **OUT:** the dialogue system and any actual "choice selected" UI (**Epic 5** — 5.1 branching dialogue, 5.2 INSTINCT, 5.3 scene flags; 5.3 consumes this store's generic flag API); INSTINCT checks (AD-8, 5.2); any combat/transformation from Bond (**NFR-5** — explicitly non-goal); reading Bond from a quest/dialogue node (5.1+).

### Testing standards
- No committed JUnit suite — throwaway-`main` headless harness + live boot, as in 4.1/4.2 / 3.1–3.4. `FlagStore`/`RunState` are pure model and run headless; libGDX `Json` (for the round-trip) is headless-safe. **Build quirk:** `mvn -o -pl core install` before `exec:java`.

### References
- [Source: _bmad-output/planning-artifacts/epics.md#Story 4.3 (FR-15); Epic 4 design constraint]
- [Source: ARCHITECTURE-SPINE.md#AD-7, #AD-6, #AD-2, #AD-3; Structural Seed → state/FlagStore.java]
- [Source: core/src/main/java/com/margins/rogue/state/RunState.java (ownership + restart); save/SaveService.java (element-type registration); state/IdentifyMap.java (peer state class in the same package)]
- [Source: _bmad-output/implementation-artifacts/4-2-distraction-leverage.md, 4-1-companion-follow-and-floor-transition.md (the 4.x series this builds on)]

### Project Structure Notes
- The architecture structural seed lists `state/FlagStore.java` — this time the spine's path **matches** the actual codebase (`com.margins.rogue.state` holds `RunState` and `IdentifyMap`). Put `FlagStore.java` there.
- `restart()` semantics: a genuinely fresh run (used on death/new game). Narrative reset there is correct (AD-7 run-scoped). `descend()` is **not** a new run — do not reset the store there.

### Review Findings

Code review 2026-08-03 (Blind Hunter + Edge Case Hunter + Acceptance Auditor, parallel). AC verdict: 4.3 AC 1–7 all satisfied; NFR-5 confirmed (nothing outside FlagStore/RunState reads Bond).

- [x] [Review][Patch] `applyBondTag(null)` throws NPE [core/src/main/java/com/margins/rogue/state/FlagStore.java (applyBondTag)] — `switch (tag)` on a null argument NPEs; the intended Epic 5 caller (authored dialogue) could pass a missing/mistyped node tag. **Fixed 2026-08-03:** added `if (tag == null) return;` before the switch (a tag-less node is a no-op, not a crash); verified with a throwaway harness. (edge) [Low]
- [x] [Review][Defer] No committed JUnit test suite for the new model [core/src/main/java/com/margins/rogue/state/FlagStore.java] — deferred, pre-existing. The FlagStore round-trip (and companion/identify) coverage exists only as throwaway harnesses, so the persistence contract isn't guarded by a committed test. This is the standing Epic 1/3 retro critical-path item (stand up a JUnit 5 core test root); the round-trip itself was verified passing by the 4.3 harness. (blind) [Low]

## Dev Agent Record

### Agent Model Used
- Claude Opus 4.8 (1M context) — implementation + harness verification.

### Debug Log References
- Harness run: `mvn -o -q -pl core install` (clean) then `mvn -o -q -pl core org.codehaus.mojo:exec-maven-plugin:3.1.0:java -Dexec.mainClass=com.margins.rogue.BondHarness` → **ALL 26 CHECKS PASSED**.
- Smoke boots on `:0`: `timeout 12/10 mvn -o -q -pl desktop ... -Dexec.mainClass=com.margins.desktop.DesktopLauncher` — ran full duration, terminated by timeout (exit 143/124), **zero exceptions**; existing pre-4.3 save (`save/run.json`, no `flagStore` key) loaded cleanly.

### Completion Notes List
- ✅ **Task 1 — FlagStore (AD-7):** `state/FlagStore.java` created: `LinkedHashMap<String,Integer>` k/v store with `get(key)`→0-if-never-set, `set`, `add`; Bond as a store key (`KEY_BOND = "bond.galleon"`) with `getBond()`/`adjustBond(delta)`/`getBondTier()` (tier: ≤−2→0 cold, ≥2→2 warm, else 1 neutral); tag authority `applyBondTag(String)` over stable `BOND_TAG_HONEST`/`BOND_TAG_DISMISSIVE` constants (+1/−1, unknown no-op). Pure model — no RNG, no libGDX (AD-2). Deterministic serialization via insertion-ordered map.
- ✅ **Task 2 — RunState owns the store (AD-3/AD-7):** `flagStore` field-initialized (`new FlagStore()`), getter `getFlagStore()` added; `restart()` resets it to a fresh store (narrative is run-scoped) alongside the `identifyMap` rebuild; `restoreAfterLoad()` unchanged (persisted field loads with the run; initializer only covers pre-field saves). `descend()` untouched.
- ✅ **Task 3 — Persistence wiring (AD-6):** `SaveService.json()` adds `json.setElementType(FlagStore.class, "flags", Integer.class)` (map-value registration so integers deserialize as `Integer`, not `Double`/`String`), mirroring the `enemies`/`floorItems`/`companions` lines.
- ✅ **Task 4 — Verification:** Throwaway `BondHarness` (deleted after run) proved all ACs headless: fresh-run baseline (bond 0, tier 1); tag shifts (2 honest → bond 2/warm tier 2; 4 dismissive → bond −2/cold tier 0; unknown tag no-op); generic flags (set/add/never-set→0); whole-`RunState` Json round-trip preserving Bond + flags with `restoreAfterLoad` keeping the store usable; `restart()` reset; old-save safety via `JsonValue.remove("flagStore")` → loads empty-but-non-null. Live boot on `:0` confirmed the pre-4.3 save loads cleanly (no `flagStore` key → empty store). No combat/transformation behavior attached anywhere (NFR-5) — `TurnEngine`/`CombatSystem` never read `getBond()`.
- **AC coverage:** 1 ✓ (baseline + non-null store) · 2 ✓ (tag deltas) · 3 ✓ (tone tier read) · 4 ✓ (generic flags) · 5 ✓ (restart reset) · 6 ✓ (round-trip) · 7 ✓ (store-only narrative read/write seam, pure model, AD-6 root, NFR-5 no combat/transformation).

### File List
- `core/src/main/java/com/margins/rogue/state/FlagStore.java` (NEW) — generic k/v narrative store + Galleon's Bond + tag authority (AD-7).
- `core/src/main/java/com/margins/rogue/state/RunState.java` (MODIFIED) — `flagStore` field + `getFlagStore()` getter; `restart()` resets the store.
- `core/src/main/java/com/margins/rogue/save/SaveService.java` (MODIFIED) — `FlagStore.flags` map-value element-type registration (+ import).

## Change Log

- 2026-08-03 — Story 4.3 spec created: Bond tracking. Generic `FlagStore` (AD-7 k/v store) on `RunState`, Bond as a store key with 3 tone tiers, tag→delta authority (`applyBondTag`) as the single mapping for Epic 5 dialogue, map-value persistence under AD-6, restart reset. Bond explicitly narrative-only (NFR-5).
- 2026-08-03 — Story 4.3 implemented: created `state/FlagStore.java`, wired onto `RunState` (field + getter + restart reset), added `FlagStore.flags` map-value registration in `SaveService.json()`. Verified via throwaway `BondHarness` (26 checks passed) + `:0` live boot against the pre-4.3 save (clean load, no exceptions). Harness deleted. Status → review.

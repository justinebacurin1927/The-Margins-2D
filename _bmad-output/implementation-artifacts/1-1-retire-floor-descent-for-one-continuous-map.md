---
baseline_commit: 85bed5d5530095e382acf85f461ff87638557173
---

# Story 1.1: Retire floor-descent for one continuous map

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As the solo developer,
I want the floor-descent machinery replaced by a single continuous tiled region,
so that the game can express the east/west danger gradient the whole design depends on (AD-8).

This is the **one intentional breaking deviation** from the ratified brownfield (architecture AD-8). It gates every spatial story that follows (Epic 3 world/foray). Keep it surgical: retire descent, land on a continuous map, add the save-version guard. Do **not** build the hybrid landmark world here — that is Story 3.1.

## Acceptance Criteria

*(From epics.md Story 1.1, expanded with the code reality.)*

1. **Given** the ratified brownfield core, **when** the AD-8 refactor is applied, **then** `RunState.descend()`, `floorDepth` (+ `getFloorDepth`/`setFloorDepth`), the `Route` floor-list model (`Route.getFloorCount`, `Route.CARAVAN_ROAD`), `RogueTile.STAIRS_DOWN`/`STAIRS_UP`, `FloorGenerator`'s per-floor BSP coupling (the `floorDepth` parameter and STAIRS placement), and the `TurnEngine` STAIRS_DOWN descent branch are all removed.
2. **Given** the refactor, **when** the map generates, **then** `FloorGenerator` produces **one continuous tiled region** (rooms + corridors, no stairs, no depth), and no `Route`-as-floors model remains. *(Landmark geography — AD-8's eventual `Route` replacement — is Story 3.1, not here.)*
3. **Given** the whole module, **when** `mvn -o clean install` runs, **then** it compiles and **all headless tests pass**, with **no test or source referencing** `descend`/`floorDepth`/`STAIRS_*`/`getFloorCount`/`Route`.
4. **Given** a save written before this refactor (contains a `floorDepth` key, no `saveVersion`), **when** it is loaded, **then** it is **rejected** (returns null / no run) with a clear message — never silently loaded onto the new map shape (AD-6).
5. **Given** a save written after this refactor, **when** it is written then loaded, **then** it round-trips successfully (the `RunStatePersistenceTest` contract still holds: player vitals, inventory, identities, flags, Last-Stand all preserved).
6. **Given** the `TurnEngine` acted-branch, **when** the descent branch is removed, **then** the single remaining acted path honors the fixed order hunger → detection → companion → enemy → noise → **checkLastStand** → FOV (AD-4 invariant), with the reprieve check preserved.

## Tasks / Subtasks

- [x] **Task 1 — Retire the tile stairs (AC: 1, 3)**
  - [x] `RogueTile.java`: delete `STAIRS_DOWN` and `STAIRS_UP` constants; change `isWalkable` to `return tile == FLOOR || tile == DOOR;`.
  - [x] `MarginScreen.java` (screen layer, `core/src/main/java/com/margins/`): remove the two `case RogueTile.STAIRS_DOWN:` / `case RogueTile.STAIRS_UP:` labels at lines ~158–159 (they won't compile once the constants are gone).
- [x] **Task 2 — Decouple `FloorGenerator` from depth and stairs (AC: 1, 2)**
  - [x] Change `generate(int width, int height, Random rand, int floorDepth)` → `generate(int width, int height, Random rand)`. Replace `int maxRooms = 8 + floorDepth;` with a fixed named constant (e.g. `private static final int MAX_ROOMS = 9;`).
  - [x] Remove the `map.setTile(start.cx(), start.cy(), RogueTile.STAIRS_UP)` and `map.setTile(end.cx(), end.cy(), RogueTile.STAIRS_DOWN)` lines (68–69). Room `centers` are still returned (the player + actors spawn from them) — keep that.
- [x] **Task 3 — Strip descent from `RunState` (AC: 1)**
  - [x] Remove the `route` field (line 45), the `import ...world.Route`, `floorDepth` field (46), `getRoute()`, `getFloorDepth()`, `setFloorDepth()`.
  - [x] Delete `descend()` (142–165) entirely.
  - [x] `generateFloor()`: call `FloorGenerator.generate(MAP_W, MAP_H, rng)` (drop `floorDepth` arg). Remove `this.floorDepth = 1;` from the `RunState(long)` constructor (74) and from `restart()` (189).
  - [x] `placeFloorActors` and `restoreAfterLoad` stay as-is (no floor coupling). Leave companion/inventory/identify/flag logic untouched — out of scope.
  - [x] Update the `RunState` class Javadoc (lines 19–24): "current floor" → "current map"; it no longer serializes a floor position. Keep it terse.
- [x] **Task 4 — Delete `Route` (AC: 1, 3)**
  - [x] After Task 3, `Route` has no code users (verified: only `RunState` + `RouteProgressionTest` referenced it; `Supply.java` mentions "Route-1" only in a doc comment — leave that comment alone). Delete `core/src/main/java/com/margins/rogue/world/Route.java`. Remove the now-empty `world` package dir if nothing else lives there.
- [x] **Task 5 — Remove the `TurnEngine` descent branch (AC: 1, 6)**
  - [x] Delete the `if (action.kind == MOVE && ...getTile(...) == STAIRS_DOWN)` block (119–129) and its `else` — collapse to the single acted path (the current `else` body, 131–143): hunger → detection → companion follow → enemy phase → noise → wait-msg → **checkLastStand** → FOV.
  - [x] Update the class Javadoc (13) if it names a descent step. Verify the acted branch still runs `checkLastStand` before `FovSystem.compute` (AD-4 invariant, the carried action item).
- [x] **Task 6 — Add the save-version guard (AC: 4, 5) — stamp AND read-branch together**
  - [x] `RunState`: add `private int saveVersion = SAVE_VERSION;` with `public static final int SAVE_VERSION = 1;` (persisted, non-transient) and `getSaveVersion()`. This is the **forward** migration mechanism (future v1→v2 compares the int). It is stamped on every new save.
  - [x] **Reject pre-AD-8 saves by discriminator key, NOT by the int** (see Dev Notes — libGDX runs the field-initializer on load, so an old save's `saveVersion` comes back as `1`, indistinguishable from a new one). In `SaveService.load()`, before `fromJson`, parse the raw string to a `JsonValue` and reject if the retired `floorDepth` key is present:
    ```java
    String raw = f.readString();
    JsonValue root = new JsonReader().parse(raw);
    if (root.has("floorDepth")) { return null; } // pre-AD-8 save — never load onto the new map (AD-6)
    RunState state = json().fromJson(RunState.class, raw); // fromJson(Class, String) — the proven overload
    state.restoreAfterLoad();
    ```
    (Pass the original `raw` string to `fromJson`, not the `JsonValue` — that's the overload `RunStatePersistenceTest` uses; `fromJson(Class, JsonValue)` is not the pattern in this codebase. `root.has("saveVersion") == false` is an equivalent discriminator, since old saves predate the field; `floorDepth` is the most explicit "this is a floor-descent save" marker.) Do the stamp AND this read-branch in this one change — do not add a dead field.
- [x] **Task 7 — Fix the tests (AC: 3, 5)**
  - [x] **Delete `core/src/test/java/com/margins/rogue/world/RouteProgressionTest.java`** — all 8 methods test retired behavior (descend, floorDepth, STAIRS, Route). Its still-relevant regressions are already covered elsewhere: save round-trip by `RunStatePersistenceTest`; seed-reproduces-map — port `sameSeedReproducesTheSameFloorSequence` into a new lightweight test that two `RunState(seed)` produce identical `tileMap` layouts (no floors).
  - [x] `CompanionTest.java:62`: reword the assertion message "descending refills the budget" → "resetDistractions refills the budget" (the test itself still passes; only the comment references descent).
  - [x] Add a **save-version rejection test** (new, in `state/`, headless — mirror `RunStatePersistenceTest:59–68`): build a current `RunState`, serialize to a `JsonValue`, `root.addChild("floorDepth", new JsonValue(2))` (and remove `saveVersion` if present) to emulate a pre-AD-8 save, and assert the guard rejects it (returns null / the `root.has("floorDepth")` branch fires). Then assert a freshly-serialized current run has **no** `floorDepth` key and loads successfully. (Keep it headless — test the discriminator logic directly; `SaveService` itself needs `Gdx.files`, so either factor the version check into a testable static method or test the `JsonValue` predicate.)
  - [x] Run the full suite: `mvn -o clean install`. Zero references to retired symbols must remain (grep as a final check).

## Dev Notes

### The exact blast radius (already mapped — read before touching anything)

Every file that references a retired symbol, verified by grep across `core` + `desktop`:

| File | What it has | Action |
|------|-------------|--------|
| `rogue/RogueTile.java` | `STAIRS_DOWN=3`, `STAIRS_UP=4`, both in `isWalkable` | Delete constants; trim `isWalkable`. |
| `rogue/FloorGenerator.java` | `generate(...,floorDepth)`, `maxRooms=8+floorDepth`, STAIRS placement (68–69) | Drop param; fixed `MAX_ROOMS`; drop stairs. |
| `rogue/state/RunState.java` | `route` field, `floorDepth`, `descend()`, `getRoute/getFloorDepth/setFloorDepth`, `generateFloor` calls `generate(...,floorDepth)` | Remove all descent members; fix `generateFloor`/ctor/`restart`. |
| `rogue/world/Route.java` | The whole floor-list model | Delete the file. |
| `rogue/system/TurnEngine.java` | STAIRS_DOWN descent branch (115–129) | Delete the branch; collapse to one acted path. |
| `com/margins/MarginScreen.java` | renders STAIRS tile colors (158–159) | Remove the two `case` labels. |
| `rogue/save/SaveService.java` | no version check | Add the reject-old read-branch (Task 6). |
| `test/.../world/RouteProgressionTest.java` | 8 descent tests | Delete (port the seed-repro one). |
| `test/.../CompanionTest.java:62` | comment says "descending" | Reword message only. |

`Supply.java:4` and `RoguePlayer.java:80` contain the *words* "Route-1"/"descend" in **doc comments only** — not code dependencies. Leave them (surgical; comment cleanup is not this story).

### Current-state notes for the UPDATE files (what must be preserved)

- **`RunState`** is the single serialization root (AD-3/AD-6). `restoreAfterLoad()` rebuilds `rng` from `seed` and re-injects `tileMap` into player/enemies/companions — **keep this intact**; it is not floor-coupled. `generateFloor()` is also called by `restart()`, so both call sites must move to the no-arg `generate`. The `spawnStartingCompanion()` / `"galleon"` bind and the `Inventory`/`IdentifyMap`/`FlagStore` fields are **old-design but out of scope** — do not rename or touch them here (companion rework is Epic 5).
- **`TurnEngine`** currently has TWO acted paths: the descent path (115–129) and the normal path (131–143). The descent path was the AD-4 invariant violator (it ran hunger → checkLastStand → FOV but **skipped** detection/enemy/noise on the arrival turn). Removing it leaves the single correct path. After the change, confirm every `acted` branch runs `checkLastStand` before `FovSystem.compute` — this is the carried action item from the old Epic 4 retro ("codify the TurnEngine acted-branch invariants").
- **`MarginScreen`** is the screen layer (AD-1/AD-2) — pure presentation. Removing the two stairs `case` labels is safe; those tiles will never appear once `FloorGenerator` stops placing them.

### The save-version mechanism (get this exactly right — it's a carried lesson)

The old build added a `saveVersion` field **dead** (no read-branch) in Epic 3 and had to remove it in Epic 4 review. **Do not repeat that** — add the field AND its read-branch in this one story (Task 6).

**Critical mechanism (verified against this codebase):** libGDX `Json.fromJson` **invokes the no-arg constructor, which runs field initializers** — `RunState.java:43` says so explicitly, and `RunStatePersistenceTest:59–68` relies on it (a pre-`flagStore` save loads as an empty-but-non-null store because the initializer ran). Consequence: an old save with **no** `saveVersion` key still loads with `saveVersion == 1` (the initializer set it), so an int compare `getSaveVersion() < SAVE_VERSION` would **wrongly accept** the old save. **Do not gate the pre-AD-8 rejection on the int.** Gate it on a **discriminator key in the raw JSON** — the retired `floorDepth` key (present in every old save, never written by the new code). Use the exact `JsonReader().parse(...)` + `root.has(...)` pattern the persistence test already establishes (lines 59–68). The int `saveVersion` still earns its place as the *forward* mechanism for future v1→v2 migrations, where both saves carry the key and the compare is valid. **Test both directions:** an old-shaped blob (has `floorDepth`, no `saveVersion`) is rejected → null; a freshly-saved current run loads fine.

### Scope discipline (CLAUDE.md §2/§3)

- Minimum code to retire descent + land a continuous map + guard the save. **No** hybrid landmark gen, **no** east/west gradient, **no** renaming `generateFloor`→`generateRegion` unless trivial — those are Story 3.1.
- `Companion.resetDistractions()` loses its only caller (`descend`) — it becomes an orphan. **Leave the method** (it is public, tested, and Epic 5 re-wires companion behavior); just fix the test comment. Note the orphan in your completion notes rather than deleting cross-epic API.
- Remove only imports/members **your** change orphans (e.g. the `Route` import in `RunState`).

### Testing standards

- Test root exists: `core/src/test/java/...`, JUnit Jupiter 5.10.2 (per `core/pom.xml`), run via `mvn -o clean install` (surefire 3.2.5). **Correction to the carried action item:** the JUnit core test root is NOT missing — 8 test files exist (`RunStatePersistenceTest`, `FlagStoreTest`, `IdentifyMapTest`, `CompanionTest`, `HungerSystemTest`, `DialogControllerTest`, `SceneEffectsTest`, and the to-be-deleted `RouteProgressionTest`). Build on them.
- `RunStatePersistenceTest` is your reference for save round-trip patterns (no floor coupling — grep-confirmed clean). Mirror its style for the new save-version test.
- Build/verify quirk (carried, still true): reinstall core before running desktop exec — `mvn -o -pl core install` — and desktop file-path tests need a Lwjgl3Files harness; but this story's tests are pure headless core, so plain `mvn -o clean install` suffices.

### Project Structure Notes

- Core stays headless (AD-2): no libGDX render/input types enter `com.margins.rogue.*`. `SaveService` already uses `com.badlogic.gdx.utils.Json` + `Gdx.files` — that is the sanctioned exception (it lives in `rogue/save` and only touches serialization/IO, no render types). The `saveVersion` guard adds no new libGDX surface.
- After deleting `Route.java`, the `com.margins.rogue.world` package is empty — removing the dir is fine; leaving it empty is harmless.

### References

- [Source: _bmad-output/planning-artifacts/architecture/architecture-The-Margin-2026-08-06/ARCHITECTURE-SPINE.md#AD-8] — the retirement list and the "one breaking deviation" mandate.
- [Source: ARCHITECTURE-SPINE.md#AD-6] — `saveVersion`; reject pre-AD-8 saves; tilemap serializes inline (never regenerated on load).
- [Source: ARCHITECTURE-SPINE.md#AD-4] — the fixed pipeline + acted-branch invariant.
- [Source: _bmad-output/planning-artifacts/epics.md#Story-1.1] — the AC source.
- [Source: _bmad-output/implementation-artifacts/sprint-status.yaml#action_items] — carried: implement `saveVersion` stamp+read-branch together; verify TurnEngine acted-branch invariant.

## Dev Agent Record

### Agent Model Used

claude-opus-4-8 (1M context)

### Debug Log References

- Green baseline before changes: `mvn -o -pl core test` → 50 tests pass.
- First full run after implementation: 1 failure — `SaveMigrationTest.currentSaveHasNoFloorDepthKeyAndIsAccepted` (`root.has("saveVersion")` expected true, was false). Root-caused to libGDX `Json` default `usePrototypes=true` omitting fields equal to a fresh instance's value. See Completion Notes.
- Final: `mvn -o clean install` (both modules) → 48 tests pass, BUILD SUCCESS. Retired-symbol grep clean outside intentional test fixtures.

### Completion Notes List

- **All 7 tasks + AC 1–6 satisfied.** Floor-descent machinery fully retired: `RunState.descend()`/`floorDepth`/getters, `Route` (file deleted), `RogueTile.STAIRS_*`, `FloorGenerator`'s `floorDepth` param + stairs placement, and the `TurnEngine` STAIRS_DOWN branch. `FloorGenerator` now emits one continuous region (`MAX_ROOMS=9`, no stairs). The `TurnEngine` acted-branch collapsed to the single AD-4 path (hunger → detection → companion → enemy → noise → checkLastStand → FOV) — this *removed* the old descent branch that violated the invariant by skipping detection/enemy/noise (AC-6 satisfied by construction).
- **Two real bugs the tests caught (both would have shipped otherwise):**
  1. **`saveVersion` was never serialized.** libGDX `Json` defaults to `usePrototypes=true`, which omits any field equal to a freshly-constructed instance's value — `saveVersion` is always `1` (== prototype), so it was dropped from every save. This also meant the story's *original* `floorDepth`-key discriminator had a hole: an old save taken **on floor 1** (`floorDepth==1==prototype`) also omitted `floorDepth` and would have slipped past the guard and silently loaded onto the new map (the exact AC-4 disaster). **Fix:** `SaveService.json()` now sets `usePrototypes(false)` so `saveVersion` is always written, and the guard rejects on the **absence** of `saveVersion` — which uniquely identifies every pre-AD-8 save regardless of floor. Added a dedicated regression (`oldFloorOneSaveWithoutFloorDepthKeyIsStillRejected`).
  2. Corrected two now-stale comments my own change created (`RunState` saveVersion comment described the superseded discriminator; `RoguePlayer.getHunger` referenced the gone "carry-across-descend contract").
- **Scope held:** did not build hybrid landmark gen (Story 3.1), did not touch the `galleon` companion / inventory (Epic 5). `Companion.resetDistractions()` is now an orphan (its only caller `descend` is gone) — **left in place** per the story (public, tested, Epic 5 re-wires companion behavior); only its test comment was reworded.
- **Test delta:** −8 (`RouteProgressionTest` deleted, all floor-descent) +2 (`ContinuousMapTest`: seed-reproduces-map, no-stairs) +4 (`SaveMigrationTest`: stamp/round-trip/reject-old/reject-old-floor-1). Net 50 → 48, all green.

### File List

**Modified**
- `core/src/main/java/com/margins/rogue/RogueTile.java` — removed `STAIRS_DOWN`/`STAIRS_UP`; trimmed `isWalkable`.
- `core/src/main/java/com/margins/rogue/FloorGenerator.java` — dropped `floorDepth` param, added `MAX_ROOMS`, removed stairs placement.
- `core/src/main/java/com/margins/rogue/state/RunState.java` — removed `route`/`floorDepth`/`descend()`/`getFloorDepth`/`setFloorDepth`/`getRoute` + `Route` import; added `SAVE_VERSION`/`saveVersion`/`getSaveVersion`; fixed `generateFloor`/ctor/`restart`; Javadoc.
- `core/src/main/java/com/margins/rogue/system/TurnEngine.java` — removed the STAIRS_DOWN descent branch (single acted path); removed orphaned `RogueTile` import.
- `core/src/main/java/com/margins/rogue/save/SaveService.java` — `usePrototypes(false)`; added `isPreMigrationSave` guard + reject in `load()`.
- `core/src/main/java/com/margins/MarginScreen.java` — removed the two STAIRS `case` labels.
- `core/src/main/java/com/margins/rogue/RoguePlayer.java` — stale-comment fix (getHunger).
- `core/src/test/java/com/margins/rogue/CompanionTest.java` — reworded the `resetDistractions` assertion message.

**Added**
- `core/src/test/java/com/margins/rogue/ContinuousMapTest.java` — seed-reproduction + no-stairs regressions.
- `core/src/test/java/com/margins/rogue/save/SaveMigrationTest.java` — save-version stamp + pre-AD-8 rejection (incl. the floor-1 edge case).

**Deleted**
- `core/src/main/java/com/margins/rogue/world/Route.java` — floor-list model, retired by AD-8.
- `core/src/test/java/com/margins/rogue/world/RouteProgressionTest.java` — 8 floor-descent tests, retired.

## Change Log

- 2026-08-06 — Implemented Story 1.1 (AD-8 floor-descent retirement + AD-6 save-version guard). 8 source files changed, 2 deleted, 2 test files added. 48 tests green. Save-guard hardened during implementation: reject pre-AD-8 saves by `saveVersion` absence (with `usePrototypes(false)`), robust to the old-floor-1 omission case that a `floorDepth` check would have missed.
- 2026-08-06 — Code review (3 parallel layers: Blind Hunter, Edge Case Hunter, Acceptance Auditor). Verdict: **Approve** — AC-1/2/3/4/6 met, AD-8/6/4 satisfied. 4 patches applied (see Review Findings), 6 deferred, 3 dismissed. 48 → 51 tests green.

## Senior Developer Review (AI)

**Date:** 2026-08-06 · **Outcome:** Approve (with patches applied)

Acceptance Auditor confirmed AC-1, AC-2, AC-3, AC-4, AC-6 fully met and AD-8/AD-6/AD-4 satisfied. AC-5 was "satisfied-with-gap" (round-trip code intact but its test coverage was lost when `RouteProgressionTest` was deleted) — closed by patch P3. The mid-implementation save-guard change (`saveVersion`-absence + `usePrototypes(false)`) was validated as a correct improvement over the story's prescribed `floorDepth` discriminator.

### Review Findings

**Patches (applied this review):**
- [x] [Review][Patch] Lock the "≥1 room" invariant in `FloorGenerator` [core/.../FloorGenerator.java] — both hunters flagged `roomCenters.get(0)` as an IOOBE risk; tracing the generator, the first room always places (nothing to overlap), so the crash was unreachable — added a defensive fallback room + comment so a future change can't silently reintroduce it.
- [x] [Review][Patch] Harden `SaveService` against null/corrupt/non-object saves [core/.../save/SaveService.java] — `isPreMigrationSave` now returns `true` (reject) on null / unparseable / non-object JSON instead of throwing; `load()` wraps `fromJson` to return null on a corrupt current save. Serves AD-6 "never silently load a bad save." Covered by `nullCorruptOrNonObjectSaveIsRejectedNotCrashing`.
- [x] [Review][Patch] Close the AC-5 test-coverage gap [core/.../state/RunStatePersistenceTest.java] — added `vitalsInventoryIdentitiesAndLastStandSurviveRoundTrip` asserting HP/hunger/inventory/identify/Last-Stand survive serialize→load (the assertions lost with `RouteProgressionTest`).
- [x] [Review][Patch] Strengthen the pre-AD-8 save fixture [core/.../save/SaveMigrationTest.java] — added `handWrittenPreMigrationShapeIsRejected` using a real old-format blob (route + floorDepth, no saveVersion) authored independently of the current class, per Blind Hunter's critique that the prior test started from a current `RunState`.

**Deferred (real, but pre-existing or out of Story 1.1 scope — see `deferred-work.md`):**
- [x] [Review][Defer] `SaveService` has no production callers [core/.../MarginScreen.java] — deferred, pre-existing. The save/load path (incl. this guard) is never invoked in-game; `MarginScreen` only calls `restart()`. Needs a save/load-UI wiring story. The guard is correct but dormant until then. **Top backlog item.**
- [x] [Review][Defer] `restoreAfterLoad()` reseeds RNG without the constructor's cold-start skip [core/.../state/RunState.java] — deferred, pre-existing. Fresh vs resumed RNG streams diverge; low practical impact now that post-load regeneration is gone.
- [x] [Review][Defer] Map connectivity/reachability not guaranteed or asserted; corridors can truncate at the map edge via silent `setTile` clamp [core/.../FloorGenerator.java] — deferred to Story 3.1 (world-gen owns the real hybrid map + traversability).
- [x] [Review][Defer] `companionSpotNear` fallback can stack the companion on the player on a degenerate map [core/.../state/RunState.java] — deferred, pre-existing; mitigated by the ≥1-room guard (P1).
- [x] [Review][Defer] Comment drift, incl. `DialogController.java` still citing an **old** "AD-8" (AD-number collision between the obsolete and new architectures) [core/.../narrative/DialogController.java] — deferred, out-of-scope comment cleanup; flagged for an architecture-wide AD-reference sweep.
- [x] [Review][Defer] `MarginScreen` would render a stale STAIRS tile (3/4) as floor + treat it unwalkable if a legacy map loaded [core/.../MarginScreen.java] — deferred; mitigated because nothing loads maps in production (ties to the unwired-save item); becomes relevant when save/load is wired (at which point the guard also runs).

**Dismissed (noise / by-design):** value-based `saveVersion` compare not implemented (the *documented deferred* forward mechanism — nothing to compare until a v2 exists); "brittle marker field" (intentional — presence is the contract); hand-edited-save defeats the guard (non-threat for single-player permadeath).

---
baseline_commit: 3233bab
---

# Story 6.1: Route progression through procedural floors

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As Justine (player),
I want to descend three procedural floors of the Caravan Road,
so that the crawl has a shape leading somewhere (FR-18).

## Acceptance Criteria

1. **Given** Floors 1–3, **When** I take stairs, **Then** the next procedural floor loads and Route state advances 1→2→3 (FR-18).
2. **And** Galleon, inventory, identified supplies, flags, and Last-Stand state carry across transitions.

**Architectural definition-of-done:**

3. Route is a first-class model in `world/` (`Route.java`, AD-11/FR-18) that owns the route's shape (name + floor count); `RunState` holds it and `descend()` consults it. No route rule lives in the Screen (AD-2).
4. The route is **bounded**: the descent cannot run past the route's last floor (Floors 1–3 are a real, testable boundary). The "no more floors" case is an explicit seam that Stories 6.2 (authored Story Floor) and 6.5 (route completion) build on — 6.1 does not implement either.
5. Carry-across (AC-2) is guaranteed structurally by AD-3 (everything lives on one `RunState`) **and** locked by committed tests — the story adds regression coverage, not new mechanics, for the carry.

## Product decisions (recommended defaults baked in)

- **`Route` is a constant singleton, not persisted run data.** New `com.margins.rogue.world.Route` (per the architecture file map: `Route.java # floor sequence: 3 BSP + 1 Story Floor (FR-18)`): a small immutable description — `name`, `floorCount`, and a route-end message. `Route.CARAVAN_ROAD = new Route("The Caravan Road", 3, "…")`. On `RunState`: `private transient Route route = Route.CARAVAN_ROAD;` + `getRoute()`. **Transient** because it is a constant (one route in MVP; nothing per-run to save) and **field-initialized** because libGDX Json skips the constructor — so a save from before this field loads the default route, exactly the established `inventory`/`flagStore` pattern (AD-6). No save migration.
- **`floorDepth` already IS the route position — do not add a second counter.** `RunState.floorDepth` is persisted (AD-6), drives generation scaling (the BSP room count grows with depth), and already advances 1→2→3. A parallel `routePosition`/`routeIndex` would violate AD-3 (single owner) and force save churn for no benefit. The Route adds the **bound**, the **name**, and the **end seam** around the existing counter.
- **`descend()` becomes bounded and returns a signal.** `public boolean descend()`: if `floorDepth >= route.getFloorCount()` return `false` (route complete — the road ends), else advance + rebuild exactly as today + return `true`. The boolean drives the TurnEngine message. This is the seam 6.2 and 6.5 extend: 6.2 raises `floorCount` 3→4 and dispatches the authored floor for the 4th; 6.5 attaches the completion screen to the `false` case. **No authored-floor or completion behavior is written in 6.1.**
- **The route-end message lives on `Route` (the model owns the fiction, AD-2).** e.g. `route.endMessage()` returns a short flavor line ("The caravan road ends here."). The Screen never invents route prose.
- **TurnEngine: guard + message, and keep the descent-turn invariants.** The existing descend branch calls `state.descend()` and keeps the fixed order `hunger → checkLastStand → FOV` (the Epic 4 Last-Stand-on-descend fix — do not regress it). On `false` it adds `route.endMessage()` and still runs the same hunger/Last-Stand/FOV (stepping onto stairs committed a move turn). The enemy/noise phases are skipped on that turn, exactly as on a real descent — harmless because the route is over and there is nowhere further to go.
- **The HUD shows the route shape.** The floor line in the HUD (currently a bare `F` + depth) becomes route progress so the "shape leading somewhere" of the user story is actually perceivable: route name (or a short label) + `floorDepth`/`floorCount`, e.g. `Caravan Road 2/3`. Exact string is the dev's call; the point is the player can see where they are within the route.
- **AC-2 is guaranteed by AD-3 — this story locks it with tests, not code.** `descend()` already swaps only tilemap, enemies, floorItems, and the companion's placement; it never recreates the player and never resets the run-scoped fields (inventory, identifyMap, flagStore/Bond, `lastStandUsed`). The tests below make that guarantee explicit and permanent.

## Tasks / Subtasks

- [x] **Task 1 — `world.Route` model (AC: 1, 3)** (FR-18, AD-11)
  - [x] Create `core/src/main/java/com/margins/rogue/world/Route.java` — pure model, no libGDX (AD-2): `name`, `floorCount`, `endMessage()`, and the constant `Route.CARAVAN_ROAD` (name "The Caravan Road", 3 floors). Additive — nothing else depends on it yet.

- [x] **Task 2 — Bounded `descend()` on `RunState` (AC: 1, 3, 4)** (AD-3, AD-6)
  - [x] Add `private transient Route route = Route.CARAVAN_ROAD;` (field-init) + `getRoute()`.
  - [x] Change `public void descend()` → `public boolean descend()`: return `false` when `floorDepth >= route.getFloorCount()` (no mutation), else run the existing advance + rebuild and return `true`. Preserve the current rebuild logic untouched (placeFloorActors, companion reposition + `resetDistractions()`).
  - [x] Do NOT add a second counter, do NOT touch `floorDepth`'s name/type, do NOT add save migration.

- [x] **Task 3 — Route-aware descent in `TurnEngine` (AC: 1, 4)** (AD-4)
  - [x] In the descend branch: `boolean descended = state.descend();` and message = `descended ? "You descend to floor " + state.getFloorDepth() : state.getRoute().endMessage()`. Keep `HungerSystem.tick` → `CombatSystem.checkLastStand` → `FovSystem.compute` in that order for both cases (the Epic 4 invariant).

- [x] **Task 4 — HUD route progress (AC: 1, so-that)** (AD-2)
  - [x] `RogueGameScreen.renderHUD()`: replace the `"F" + state.getFloorDepth()` line with route progress (`state.getRoute().getName() + " " + floorDepth + "/" + floorCount`). Read-only; no rule in the screen.

- [x] **Task 5 — Committed route tests (AC: 1, 2, 3, 4, 5)** — see Testing standards
  - [x] New `RouteProgressionTest` (AC-1: start at 1, advance to 2 then 3, blocked at 3, route identity/floorCount, same-seed floor reproducibility).
  - [x] Carry-across tests (AC-2: player HP/hunger, inventory stack, identified supply, flags + Bond, `lastStandUsed`, companion repositioned on the new floor with distractions reset, floor items regenerated).
  - [x] TurnEngine integration: moving onto STAIRS_DOWN advances the route + descend message; on the last floor the same move produces the route-end message and no advance.

- [x] **Task 6 — Verification (AC: 1, 2, 5)** 
  - [x] `mvn -o -pl core test` — all 30 existing tests plus the 8 new ones green (offline — JUnit deps are cached).
  - [x] `mvn -o -pl core install` clean; boot check on `:0` (full timeout, zero exceptions) — HUD route-progress line rendered every frame without error. (Interactive descend walkthrough is a human check; flagged for Justine.)

## Dev Notes

### Governing architecture

- **FR-18 — Route progression.** "Descending stairs advances Floor 1→2→3→Story Floor; the Story Floor is authored, not procedurally generated." 6.1 builds the first half of the *route* concept: a named, bounded run of procedural floors. The authored Story Floor (the second half) is 6.2's AC, implemented on the seam 6.1 creates. [Source: prd.md#FR-18; epics.md#Story 6.1]
- **AD-3 — `RunState` is the single owner of run data.** It already owns the "current floor index" (`floorDepth`) — 6.1 keeps that fact true; the Route is consulted, not duplicated. [Source: ARCHITECTURE-SPINE.md#AD-3]
- **AD-11 — Story Floor is a fixed authored layout.** The architecture names `Route.java` as the floor-sequence holder precisely so the authored floor can be slotted in (6.2). 6.1 creates the route with 3 procedural floors and the end seam; it does NOT build the fixed-layout mode. [Source: ARCHITECTURE-SPINE.md#AD-11]
- **AD-6 — Save = serialize whole `RunState`.** `floorDepth` already persists. The transient, field-initialized `route` follows the proven `inventory`/`flagStore` pattern: Json calls the no-arg constructor (which runs field initializers) then overwrites serialized fields, so a pre-6.1 save loads the default route. [Source: ARCHITECTURE-SPINE.md#AD-6; RunState.java]
- **AD-2 — Model ⟵ Screen layering.** Route + the route-end message are model concerns; the Screen only reads `getRoute()` for the HUD line. [Source: ARCHITECTURE-SPINE.md#AD-2]
- **AD-4/Epic 4 invariant — every acted branch honors `hunger → checkLastStand → FOV`.** The descend branch currently does this in order (TurnEngine lines ~114–122, the Last-Stand-on-descend fix from the Epic 4 review). Preserve it for both the descend and the route-end case. [Source: TurnEngine.java; epic-4-retro-2026-08-03.md]

### Files being modified / added — current state and what to preserve

- **`world/Route.java`** (NEW) — the route model. Constant singleton; pure model (no libGDX).
- **`state/RunState.java`** (UPDATE): add `transient Route route` (field-init) + `getRoute()`; change `descend()` to `boolean` with the `floorDepth >= route.getFloorCount()` guard. **Preserve verbatim:** the rebuild body (`placeFloorActors`, player `placeAt`/`setMap`, companion reposition + `resetDistractions()`), `floorDepth` name/type (persisted + generation scaling), `restoreAfterLoad`, `restart`. Current `descend()` is a one-way advance — the story only bounds it and returns the signal.
- **`system/TurnEngine.java`** (UPDATE): descend branch — capture `state.descend()`'s boolean, pick the message, keep the hunger/Last-Stand/FOV order for both cases. **Preserve:** the AD-4 pipeline order and every other branch (MOVE/ATTACK/BLOCK/WAIT/USE/DROP/PICKUP/DISTRACT) untouched; the `acted` gating.
- **`RogueGameScreen.java`** (UPDATE): the HUD floor line only (`renderHUD()`). **Preserve:** the `DIALOGUE` suspension gate, inventory/drop/dialogue panels, all input handling.
- **`core/src/test/java/com/margins/rogue/world/RouteProgressionTest.java`** (NEW) — see Testing standards.
- **REUSE UNCHANGED:** `FloorGenerator` (the BSP `generate(…)` signature stays; the authored mode is 6.2), `SaveService` (no new registration — route is transient), `FlagStore`, `IdentifyMap`, `FloorItem`, `Companion`, `RogueTileMap`. **DO NOT TOUCH:** the 30 existing committed tests (must stay green), the dialogue path, `QuestManager`.

### Previous-story intelligence (Epic 5 / committed suite)

- **The committed test suite is now the standard.** The Epic 5 retro action item is done: 30 JUnit 5 tests in `core/src/test/`, green, offline (`mvn -o -pl core test` — junit-jupiter 5.10.2 + surefire 3.2.5 already in `core/pom.xml`, deps cached). **Write committed tests, not throwaway harnesses**; if a temporary headless `main` is used for debugging, delete it before commit. Build quirk: `mvn -o -pl core install` before any `-pl desktop` run.
- **The `RunState` field-init pattern is proven.** `inventory`, `floorItems`, `companions`, `flagStore` are field-initialized so pre-field saves load empty-but-non-null (verified by `RunStatePersistenceTest.preFlagStoreSaveLoadsEmptyNotNull`). `route` follows the same pattern — and being `transient`, it needs no element-type registration.
- **Descend-related regressions are already locked.** `RunStatePersistenceTest.companionSurvivesRoundTripWithMapReinjected` and the Last-Stand-on-descend fix in TurnEngine. The new tests extend this: the carry-across suite is the "descend preserves X" guarantee the retro asked for.
- **The dialogue path is unrelated to this story.** The `T` debug trigger and `DIALOGUE` UiMode (Epic 5) stay untouched; 6.3 replaces the trigger with the real opening.

### Scope boundary

- **IN:** `world.Route` model (3 procedural floors + end seam), bounded `boolean descend()`, route-aware TurnEngine message, HUD route progress, carry-across committed tests (AC-2).
- **OUT:** the authored Story Floor / fixed-layout generation (**6.2**, AD-11 — only the seam is created here); the "Five Nights, Again" opening (**6.3**); the reunion scene (**6.4**); the completion screen / route-complete marking (**6.5**); art (**6.6**). No save migration (route is transient; a pre-6.1 dev save with `floorDepth > 3` is simply blocked at the route bound — acceptable, no shipped players). No multi-route selection. No change to `FloorGenerator`, `SaveService`, `QuestManager`, or the dialogue system.

### Testing standards

- Committed JUnit 5 (already wired in `core/pom.xml`). Run `mvn -o -pl core test` (offline). All 30 existing tests must stay green.
- **`RouteProgressionTest`** (package `com.margins.rogue.world`, or `state/` if preferred — pick one and stay consistent):
  - AC-1: `new RunState(seed)` starts at floor 1 with `Route.CARAVAN_ROAD`; `descend()` returns true → floor 2; again → floor 3; a 4th call returns **false** and floorDepth stays 3 (no floor 4).
  - AC-1 determinism: two `new RunState(sameSeed)` runs descend the same sequence — same starting player tile and identical tilemaps (compare `getTile` across all 50×50) after the same number of descends (AD-5).
  - AC-2 carry: damage/starve the player, `tryAdd` a supply stack, `markIdentified` a type, set a flag + Bond, `setLastStandUsed(true)` → `descend()` → assert HP/hunger unchanged, inventory count intact, `isIdentified` intact, flags/Bond intact, `lastStandUsed` intact.
  - AC-2 companion: after `descend()`, the active companion is non-null ("galleon"), is on the new tilemap, is near the new start, distractions reset to `Companion.MAX_DISTRACTIONS_PER_FLOOR`; `followStep` does not NPE (the map was re-injected).
  - AC-2 floor items: after `descend()`, the previous floor's item list is cleared — items are re-scattered fresh (assert a known dropped stack on the old floor is gone, or assert the new list is non-null and every item is in-bounds/walkable on the new map).
  - Integration (AC-1 + Task 3): build a `RunState`, find a `STAIRS_DOWN` tile (FloorGenerator always places one), place the player on a walkable neighbor, submit `TurnEngine.advance` with a MOVE onto the stairs → floorDepth advances + message contains "descend"; repeat to the last floor → the same move yields the route-end message and no advance.

### Project Structure Notes

- `world/Route.java` per the architecture file map (`Route.java # floor sequence: 3 BSP + 1 Story Floor (FR-18)`) — sibling of `FloorGenerator`/`RogueTileMap`. `RunState` (in `state/`) imports it; dependency points State → World as the spine's dependency graph shows.
- Test mirrors production package (`com.margins.rogue.world`), consistent with `com.margins.rogue.state.FlagStoreTest`/`IdentifyMapTest`/`RunStatePersistenceTest`.

## Dev Agent Record

### Agent Model Used
- Claude Opus 4.8 (1M context) — implementation + committed-test verification.

### Debug Log References
- `mvn -o -pl core test` — first run: **7/8** RouteProgressionTest methods green, 1 failure in `turnEngineDescendsOneTwoThreeThenReportsTheRouteEnd` (`expected <2> but was <1>`). Root cause was a **bug in the test helper**, not the implementation: `stepOntoStairs` placed the player at `(sx, sy+1)` and moved `(0,+1)` — pointing *away* from the stairs (landing at `sx, sy+2`). Fixed the helper to move by the **negation** of the neighbor offset (player at `sx+ox, sy+oy` → `PlayerAction.move(-ox, -oy, facing)`). Re-ran: **38/38 green** (30 existing + 8 new), BUILD SUCCESS.
- `mvn -o -q -pl core install` — clean (exit 0).
- Boot on `:0`: `timeout 12 mvn -o -q -pl desktop exec:java -Dexec.mainClass=com.margins.desktop.DesktopLauncher` — ran the full duration, killed by timeout (`Terminated`, exit 143), **zero exceptions**; the new HUD route-progress line rendered every frame.

### Completion Notes List
- ✅ **Task 1 — `world.Route` (FR-18, AD-11):** new constant-singleton route model — `name`, `floorCount` (3), `endMessage()` ("The caravan road ends here."), and `CARAVAN_ROAD`. Pure model, no libGDX (AD-2).
- ✅ **Task 2 — bounded `boolean descend()` (AD-3/AD-6):** `RunState` holds `private transient Route route = Route.CARAVAN_ROAD;` (field-init, so a pre-6.1 save loads the default route) + `getRoute()`. `descend()` returns `false` and mutates nothing when `floorDepth >= route.getFloorCount()`; otherwise advances + rebuilds exactly as before and returns `true`. No second counter; `floorDepth` remains the single persisted position (AD-3/AD-6).
- ✅ **Task 3 — route-aware TurnEngine (AD-4):** the descend branch captures `state.descend()`'s boolean and emits `"You descend to floor N"` or the route-end message; the `hunger → checkLastStand → FOV` order (Epic 4 invariant) is preserved for both cases.
- ✅ **Task 4 — HUD route progress (AD-2):** `renderHUD()` now draws `"The Caravan Road N/3"` instead of `"FN"`; read-only, no rule in the screen.
- ✅ **Task 5 — committed tests:** `RouteProgressionTest` (8 methods) — route identity/start-at-1, bounded 1→2→3→blocked, same-seed floor reproducibility, transient-route save/reload, AC-2 carry (vitals/inventory/identify/flags/Bond/Last-Stand), companion reposition + distraction refill + no-NPE follow, floor-item re-scatter, and a full TurnEngine 1→2→3→route-end journey.
- ✅ **Task 6 — verification:** 38/38 tests green offline, `install` clean, `:0` boot clean.
- **AC coverage:** 1 ✓ (bounded 1→2→3, route state on `RunState`) · 2 ✓ (carry-across locked by tests) · 3 ✓ (Route in `world/`, `RunState` owns it, `descend` consults it) · 4 ✓ (descent blocked at route end — explicit seam for 6.2/6.5) · 5 ✓ (AC-2 is AD-3-structural + regression-tested).
- **Out of scope, as specified:** authored Story Floor (6.2), opening (6.3), reunion (6.4), completion screen (6.5), art (6.6). No save migration (route is transient), no `FloorGenerator`/`SaveService`/`QuestManager`/dialogue changes.
- **Human check outstanding:** interactive walkthrough — descend three floors, confirm the HUD shows `The Caravan Road 1/3 → 2/3 → 3/3` and the route-end message on the third.

### File List
- `core/src/main/java/com/margins/rogue/world/Route.java` (NEW) — route model: name, floor count, end message; `Route.CARAVAN_ROAD`.
- `core/src/main/java/com/margins/rogue/state/RunState.java` (MODIFIED) — transient field-init `route` + `getRoute()`; `descend()` → `boolean` with the route-end guard.
- `core/src/main/java/com/margins/rogue/system/TurnEngine.java` (MODIFIED) — descend branch emits route-aware message; hunger/Last-Stand/FOV order preserved.
- `core/src/main/java/com/margins/rogue/RogueGameScreen.java` (MODIFIED) — HUD floor line → route progress (`renderHUD()`).
- `core/src/test/java/com/margins/rogue/world/RouteProgressionTest.java` (NEW) — 8 tests: route shape, bounded descent, seed reproducibility, save/reload, AC-2 carry, companion refill, floor-item re-scatter, TurnEngine journey.

### Review Findings

Code review 2026-08-03 (Blind Hunter + Edge Case Hunter + Acceptance Auditor, parallel). AC verdict: all 5 ACs SATISFIED; 38/38 tests green.

- [x] [Review][Defer] Legacy save with `floorDepth` above the route bound loads soft-locked [core/src/main/java/com/margins/rogue/state/RunState.java:143] — a hypothetical pre-6.1 deep save (old `descend()` was unbounded) with `floorDepth:5` round-trips but `descend()` returns `false` immediately and the run is stranded (HUD shows `5/3`). Unreachable from any real save — `setFloorDepth` has zero callers and no repo save has ever contained a `floorDepth` key — but it's the one gap where this codebase's graceful legacy-compat pattern (field-init defaults) does not apply. Deferred: add a `floorDepth` clamp (or route-complete migration) + test when 6.5 builds the completion path. (blind+edge) [Med→Low]
- [x] [Review][Defer] Transient-route design is a landmine for 6.2/6.5 [core/src/main/java/com/margins/rogue/world/Route.java:21] — works only because `route` is a compile-time constant singleton. A future per-run route (different floor count / non-constant) would either fail Json deserialization (`Route` has no no-arg constructor → `SerializationException`) or be silently replaced by `CARAVAN_ROAD` on load. Safe today; revisit when 6.2 makes the route per-run. (blind) [Low]
- [x] [Review][Defer] Road-end turn burns hunger/Last-Stand and is re-triggerable [core/src/main/java/com/margins/rogue/system/TurnEngine.java:116-124] — on a `false` descend the branch still ticks hunger + Last-Stand; the player stands on the end stairs and can step off/on, re-firing the end message and starving with no completion state. This is the documented 6.5 seam — 6.1 deliberately creates it without implementing completion. (blind+edge) [Low]
- [x] [Review][Defer] Legacy-deep-save HUD renders an impossible floor marker (`5/3`) [core/src/main/java/com/margins/rogue/RogueGameScreen.java:239] — same root as the legacy-save finding; cosmetic, only on a state no real save reaches. (blind+edge) [Low]

### Patch findings

- [x] [Review][Patch] Comment misstates the Json serialization mechanism [core/src/main/java/com/margins/rogue/state/RunState.java:41-44] — the comment on `private transient Route route` says "Json skips the constructor (which would otherwise run this initializer)". libGDX Json 1.12.1 (verified from bytecode) *invokes* the no-arg constructor via `ClassReflection.newInstance` — which is exactly what runs the field initializer. The behavior is correct and test-locked (`routeIsATransientConstantThatSurvivesASaveReload`); only the stated rationale is wrong. Fix the comment so a future reader doesn't "fix" it into a real bug. (blind+auditor) [Low, cosmetic] — RESOLVED 2026-08-03: comment rewritten to state the actual mechanism ("Json never serializes a transient field, and fromJson invokes the no-arg constructor, which runs field initializers"); 38/38 tests green after the patch.

## Change Log

- 2026-08-03 — Story 6.1 spec created: route progression through procedural floors (FR-18). Introduces `world.Route` (constant singleton: "The Caravan Road", 3 floors) as the shape holder; `RunState.descend()` becomes bounded and returns a signal (`floorDepth >= route.floorCount()` → false), with `floorDepth` retained as the single persisted position counter (AD-3/AD-6); `TurnEngine` gains route-aware descent + end-seam message while preserving the hunger→Last-Stand→FOV invariant; HUD shows route progress; AC-2 carry-across is locked with committed tests. The authored Story Floor (6.2), opening (6.3), reunion (6.4), completion screen (6.5), and art (6.6) are explicitly out of scope — 6.1 creates only the seam they build on.
- 2026-08-03 — Story 6.1 implemented: `world.Route`, bounded `boolean RunState.descend()` with the transient field-init route, route-aware TurnEngine descent message, HUD route progress. Verified via committed `RouteProgressionTest` (8 methods; one test-helper direction bug found and fixed — the helper moved away from the stairs) — 38/38 tests green offline, install clean, `:0` boot clean. Status → review.
- 2026-08-03 — Code review complete (Blind + Edge Case + Acceptance Auditor in parallel): all 5 ACs SATISFIED, 38/38 green. Triage: 1 patch (route-field comment misstated Json mechanism — rewritten to state the actual constructor-invocation behavior), 4 defers logged to deferred-work.md (legacy floorDepth soft-lock, transient-route landmine for 6.2, road-end turn burn as documented 6.5 seam, legacy-save HUD `5/3`), 1 dismissed (tests "pending commit" — they commit with the story). Status → done.

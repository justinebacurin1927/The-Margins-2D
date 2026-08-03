---
baseline_commit: f1005e262fac838ade638e386d9bfc8d7ad90595
---

# Story 4.1: Companion follow and floor transition

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As Justine (player),
I want my companion to travel with me — following as I move and coming with me down the stairs —
so that he's present to help across the Route (FR-13).

## Acceptance Criteria

1. **Given** an active companion, **When** I move, **Then** he pathfinds one tile per turn to stay near Milek, never landing on Milek's tile or a blocked tile, and stops crowding once adjacent. (FR-13, AD-10)
2. **Given** an active companion, **When** I step onto the down-stairs, **Then** the run descends to the next floor and the companion transitions with me — both Milek and the companion are placed at the new floor's entrance. (FR-13, AD-10)
3. **Given** a descent, **When** the new floor is generated, **Then** Milek's HP / hunger / inventory / identified-supplies are **preserved** (descent moves the existing player; it does not create a new one), `floorDepth` increases by one, and the new floor has its own fresh enemies and items. (AD-6)
4. **Given** exactly one party slot (AD-10), **When** anything queries the party, **Then** there is at most one active companion; it is an allied turn actor that runs inside the turn pipeline (not inlined into input/rendering). (AD-10, AD-4)

**Architectural definition-of-done:**

5. The companion is a `RunState`-owned entity in its own list (AD-10), moved by an **ordered turn system** in the Companion+Enemy-AI phase (AD-4), holds no libGDX types in its serialized state (AD-2), and survives save/load with the whole `RunState` (AD-6). The follow step reuses the existing greedy tile-stepping pattern rather than inventing pathfinding.

## Product decisions (recommended defaults baked in)

- **Build it generic, not Galleon-specific.** Per the epic's design constraint, the entity is a plain `Companion` (a `name`/bind id field distinguishes Erik vs Galleon later). This story wires the *system*; **who** binds and **when** is Bond/dialogue work in Story 4.3 / Epic 5–6.
- **One always-on companion for now.** Binding/recruitment doesn't exist yet (that's 4.3 + Epic 6). So this story spawns a single active companion adjacent to Milek at run start (a placeholder bind) purely so follow + carry are demonstrable and testable. When recruitment lands, that auto-spawn is replaced by a real bind — flagged in the scope boundary. **Recommendation: auto-spawn one now.**
- **Minimal one-way descent, not route progression.** Stepping on `STAIRS_DOWN` regenerates the next floor and carries the party. Floor-count limits, the authored Story Floor, and `STAIRS_UP` ascent are **Epic 6** (FR-18/19) and explicitly out of scope here. This story delivers just enough transition to satisfy AC-2/AC-3. (This also fills a real gap: descent was never wired — `STAIRS_DOWN` tiles existed but did nothing.)
- **Placeholder art.** The companion renders with an existing texture (tinted to read as an ally); real per-bind sprites (Erik/Galleon) are Epic 6's "minimum unique art pass."

## Tasks / Subtasks

- [x] **Task 1 — `Companion` entity (generic, allied tile-actor)** (AC: 1, 4, 5)
  - [x] Create `core/src/main/java/com/margins/rogue/Companion.java` (package `com.margins.rogue`, **beside** `RogueEnemy`/`RoguePlayer` — the project keeps entities here, not in an `entity/` subpackage; see Project Structure Notes). Fields: `int tileX, tileY`, `String bindId` (e.g. `"erik"`/`"galleon"` — a plain label for later art/dialogue), `transient RogueTileMap map`. No `Texture`/libGDX field on the model (AD-2; the screen picks the sprite). Add a no-arg ctor for libGDX Json plus `Companion(int x, int y, RogueTileMap map, String bindId)`.
  - [x] Getters `getTileX/getTileY/getBindId`, and `setMap(RogueTileMap)` for post-load re-injection (mirror `RogueEnemy.setMap`).
  - [x] `followStep(int targetX, int targetY)`: if already `isAdjacentTo(target)` (Manhattan == 1) **or** on the target tile, do nothing (don't crowd). Otherwise take one greedy step toward the target reusing `RogueEnemy.takeTurn`'s pattern — try the x step first, then the y step — only onto `map.isWalkable` tiles, never onto the target tile. Keep it a pure model method (no RNG needed; deterministic follow).
  - [x] `isAdjacentTo(int x, int y)` helper (copy `RogueEnemy.isAdjacentTo`).

- [x] **Task 2 — `RunState` owns the party (single slot, AD-10)** (AC: 2, 3, 4, 5)
  - [x] Add `private List<Companion> companions = new ArrayList<>();` (AD-10 "its own list") and `getCompanions()`, plus `getActiveCompanion()` returning `companions.isEmpty() ? null : companions.get(0)`. Enforce the single slot by only ever adding one (no equip UI yet).
  - [x] Spawn one active companion **once at run start** — in the constructor after `generateFloor()` (and in `restart()` after its `generateFloor()`), on a walkable tile adjacent to the player's spawn (fall back to the player's tile-neighbourhood scan like the enemy placement does). Do **not** spawn inside `generateFloor()` (that runs per-floor; the companion is carried, not respawned).
  - [x] In `restoreAfterLoad()`, re-inject the tilemap into each companion (`for (Companion c : companions) c.setMap(tileMap);`), exactly like the `enemies` loop.

- [x] **Task 3 — Follow runs as an ordered turn system (AD-4)** (AC: 1, 4)
  - [x] Create `core/src/main/java/com/margins/rogue/system/CompanionSystem.java` with `static void follow(RunState state)` — the active companion (if any) takes one `followStep` toward the player's **current** (post-move) tile. Matches the static-system pattern of `HungerSystem`/`DetectionSystem`/`NoiseSystem`/`FovSystem`.
  - [x] In `TurnEngine.advance`, inside the `if (acted)` block, call `CompanionSystem.follow(state)` in the **Companion+Enemy-AI phase (AD-4)** — after `DetectionSystem.update` and **before** `CombatSystem.enemyPhase` (the ally moves as part of that phase). Do not inline movement into the input switch or the screen.

- [x] **Task 4 — Minimal descent that carries the party (AC: 2, 3)**
  - [x] Refactor `RunState` so floor-layout generation is reusable **without recreating the player**. Extract the map + enemies + floorItems build (current `generateFloor` body, RunState.java:69–111) into a private helper that returns the `FloorResult` (or sets the fields) but leaves player handling to the caller. `generateFloor()` keeps its current behavior (build layout, then `player = new RoguePlayer(entrance…)`) for run start/restart. **Do not change run-start/restart behavior.**
  - [x] Add `public void descend()`: `floorDepth++`; rebuild the floor via the shared helper (new `tileMap`, fresh `enemies`, fresh `floorItems`); **reposition the existing player** to the new entrance (`roomCenters.get(0)`) via a new tiny `RoguePlayer.placeAt(int x, int y)` (sets tileX/tileY; there is no such setter today) and `player.setMap(tileMap)` — **never** `new RoguePlayer(...)`, so HP/hunger/inventory survive (AC-3). Then place the active companion on a walkable tile adjacent to the new entrance and `setMap` it. Reset per-floor view state consistent with `generateFloor` (fog/explored is rebuilt by `FovSystem.compute`).
  - [x] Trigger descent in the turn pipeline (AD-4, not the screen): in `TurnEngine.advance`, after the action switch resolves and the player `acted` via MOVE, if `state.getTileMap().getTile(player.getTileX(), player.getTileY()) == RogueTile.STAIRS_DOWN`, call `state.descend()`, add message `"You descend to floor " + state.getFloorDepth()`, then treat descent as the whole turn: tick hunger, recompute `FovSystem.compute`, and **skip** the enemy/noise phases for the arrival turn (the old floor's actors are gone; the new floor's enemies start unaware). Structure this so the normal `acted` systems don't also run on the old floor after descent.

- [x] **Task 5 — Render the companion (placeholder art)** (AC: 1, 2)
  - [x] In `RogueGameScreen`, draw the active companion at its tile in the existing entity-render pass, next to how enemies are drawn, using a placeholder `Assets` texture (reuse e.g. `Assets.playerTex`/`rogueEnemyTex`, tinted via `batch.setColor` to read as an ally, then reset color). Respect the same visibility rule enemies use (only drawn when its tile is visible/explored). Real per-bind sprites are deferred to Epic 6.

- [x] **Task 6 — Persistence wiring (AD-6)** (AC: 3, 5)
  - [x] In `SaveService.json()`, add `json.setElementType(RunState.class, "companions", Companion.class)` (mirrors the `enemies`/`floorItems` element-type registration). The `companions` list then serializes with `RunState`; `restoreAfterLoad` re-injects the map (Task 2).

- [x] **Task 7 — Verification** (AC: 1, 2, 3, 4, 5)
  - [x] Headless harness (throwaway `main`, run via `exec-maven-plugin:3.1.0:java`, per the 3.x pattern):
    - Run start: exactly one active companion, placed on a walkable tile adjacent to the player, not on the player's tile.
    - Follow: drive a sequence of player MOVEs through `TurnEngine`; after each, the companion is within a small radius of the player, never on the player's tile, never on a non-walkable tile; once adjacent it doesn't crowd onto the player.
    - Descent: walk the player onto a `STAIRS_DOWN` tile (place one adjacent for the test or drive to `roomCenters` end) → `floorDepth` increments by 1; the `RoguePlayer` instance is the **same object** with HP/hunger/inventory unchanged; `tileMap` is a new instance; both player and companion sit at/adjacent to the new entrance; still exactly one companion.
    - Persistence: Json round-trip (with the `companions` element type registered) preserves the companion's tile + `bindId` and `floorDepth`; after `restoreAfterLoad` the companion's map is non-null (a follow step works post-load).
  - [x] `mvn -o -pl core install`, then live boot on `:0`: confirm the companion follows as you move and comes with you when you step on the down-stairs. Delete the throwaway harness after the run.

## Dev Notes

### Governing architecture
- **AD-10 — Companion as an allied turn actor.** The companion is a `RunState` entity in its own list, implementing the same tile-actor turn contract as enemies but allied, running inside the turn pipeline. (Distraction — 4.2 — will emit a Noise event, not manipulate enemies directly; not in this story.) [Source: ARCHITECTURE-SPINE.md#AD-10]
- **AD-4 — Fixed turn order.** `TurnEngine.advance` runs systems in order: PlayerAction → Hunger → **Companion+Enemy AI** → Noise → cleanup/flags. Follow inserts as an ordered system in that phase, never inlined into input or rendering. [Source: ARCHITECTURE-SPINE.md#AD-4; core/src/main/java/com/margins/rogue/system/TurnEngine.java:101–114]
- **AD-2 — Model owns state; no libGDX in the model.** `Companion` is pure model (a `transient RogueTileMap` for movement, like `RogueEnemy`; no `Texture` on the serialized model — the screen chooses the sprite). `RunState` owns the party list. [Source: ARCHITECTURE-SPINE.md#AD-2, #AD-3 (RunState ownership line lists "companion")]
- **AD-6 — Save = whole `RunState`.** The `companions` list rides along via libGDX Json; register its element type in `SaveService.json()` beside `enemies`/`floorItems`, and re-inject the map in `restoreAfterLoad`. [Source: ARCHITECTURE-SPINE.md#AD-6; core/src/main/java/com/margins/rogue/save/SaveService.java:22–28]
- **Generic-companion design constraint.** Build follow/(distraction)/(bond) as an entity-agnostic system; Erik is the first bind (Act 0 + Forest), Galleon binds later. [Source: epics.md#Epic 4 design constraint; opening-design-act0-forest.md]

### Files being modified / added — current state and what to preserve
- **`RogueEnemy.java`** (READ, reuse — not modified): `takeTurn(playerX, playerY)` (lines 52–76) is the greedy one-tile chase — x-step then y-step, `map.isWalkable` gated, refuses to step onto the player. `Companion.followStep` mirrors this but *stops when adjacent* (allies shouldn't shove the player). `isAdjacentTo` (78–80) copy verbatim.
- **`RunState.java`** (UPDATE): `generateFloor()` (69–111) currently builds tileMap + enemies + floorItems **and constructs a new `RoguePlayer`** at `roomCenters.get(0)` (line 75) and clears `floorItems`. Preserve this for run start/restart. Descent must **not** hit the `new RoguePlayer` path (it would wipe HP/hunger/inventory — AC-3). `restoreAfterLoad` (120–126) re-injects the map into player + enemies; add companions. `restart` (131–137) resets floorDepth and rebinds identity — add the companion respawn there too.
- **`RoguePlayer.java`** (UPDATE): has `getTileX/getTileY` but **no position setter** — add a tiny `placeAt(int x, int y)` for descent repositioning (model-only). Do not touch HP/hunger/inventory.
- **`TurnEngine.java`** (UPDATE): the `if (acted)` block (101–114) runs Hunger → Detection → enemyPhase → Noise → LastStand → Fov. Insert `CompanionSystem.follow` before `enemyPhase`; add the post-MOVE descent check (which short-circuits the enemy/noise phases for the arrival turn). The USE/DROP/PICKUP cases are unrelated — leave them.
- **`RogueGameScreen.java`** (UPDATE): entity render pass draws player + enemies; add the companion draw (placeholder texture, ally tint, enemy-style visibility gate). `getTile` accessor is `RogueTileMap.getTile(x,y)`; `RogueTile.STAIRS_DOWN == 3`.
- **`SaveService.java`** (UPDATE): add the `companions` element-type line.
- **NEW:** `Companion.java`, `system/CompanionSystem.java`.

### Scope boundary
- **IN:** generic `Companion` entity, single-slot ownership on `RunState`, follow-as-ordered-system, minimal one-way stair descent that carries player + companion (preserving player state), rendering (placeholder), and save/load.
- **OUT:** Distraction/Noise ability (**Story 4.2**); Bond tracking (**Story 4.3**); recruitment/binding gates and the real "who joins when" (4.3 + Epic 5–6) — replaced here by the temporary auto-spawn; route floor-count limits, the authored Story Floor, `STAIRS_UP` ascent, and completion (**Epic 6**, FR-18/19); real per-bind art (**Epic 6** art pass); any combat contribution from the companion (NFR-5 — companions grant no combat bonus in MVP).

### Testing standards
- No committed JUnit suite — throwaway-`main` headless harness + live boot, as in 3.1–3.4. `RunState`/`TurnEngine`/`FloorGenerator` are pure model and run headless; libGDX `Json` (for the round-trip) is headless-safe. **Build quirk:** `mvn -o -pl core install` before any live boot.

### References
- [Source: _bmad-output/planning-artifacts/epics.md#Epic 4 · Story 4.1 (FR-13); Epic 4 design constraint]
- [Source: ARCHITECTURE-SPINE.md#AD-10, #AD-4, #AD-2, #AD-6; Structural Seed → entity/Companion.java, system/CompanionSystem]
- [Source: core/src/main/java/com/margins/rogue/RogueEnemy.java (chase pattern to reuse); state/RunState.java (generateFloor/restore/restart); system/TurnEngine.java (turn order); save/SaveService.java (element-type registration); RogueTileMap.java (getTile); FloorGenerator.java (roomCenters entrance)]

### Project Structure Notes
- The architecture Structural Seed lists `entity/Companion.java` and `system/CompanionSystem`, but the **actual** codebase keeps entities directly under `com.margins.rogue` (`RogueEnemy.java`, `RoguePlayer.java`) and systems under `com.margins.rogue.system`. Follow the **actual** layout: `com/margins/rogue/Companion.java` and `com/margins/rogue/system/CompanionSystem.java`. Variance is naming-only; the spine's `entity/` prefix is not how this project is organized.
- Descent is genuinely new (no prior stairs-down trigger existed). It is deliberately minimal here; Epic 6 owns real route progression.

### Review Findings

Code review 2026-08-03 (Blind Hunter + Edge Case Hunter + Acceptance Auditor, parallel). AC verdict: 4.1 AC 1–5 all satisfied.

- [x] [Review][Decision→Resolved] `saveVersion` dead field rides in the diff — `RunState` declared `private int saveVersion;` (serialized) but nothing read, wrote, or incremented it, giving zero migration signal. **Resolution (2026-08-03): removed from this changeset** (option b). The migration mechanism will be (re)introduced by the story that actually implements save-versioning — it remains an open Epic 3 retrospective critical-path item, not Epic 4 scope. (blind+auditor)
- [x] [Review][Patch] Last Stand reprieve skipped on the descend turn [core/src/main/java/com/margins/rogue/system/TurnEngine.java] — the descend branch ran `HungerSystem.tick(player)` but not `CombatSystem.checkLastStand(...)`, unlike every other turn. A player at 1 HP with hunger 0 who steps onto STAIRS_DOWN died to the hunger tick without the once-per-run reprieve firing (FR-16/17). **Fixed 2026-08-03:** added `CombatSystem.checkLastStand(state, result.messages)` after the hunger tick in the descend branch; verified with a throwaway harness (player primed to 1 HP / 0 hunger, stepped onto stairs, descended, and the reprieve fired — survived at 1 HP). (blind) [Med]
- [x] [Review][Patch] Stale `TurnEngine` class header comment [core/src/main/java/com/margins/rogue/system/TurnEngine.java] — the AD-4 pipeline doc-comment omitted the Companion phase. **Fixed 2026-08-03:** header now reads `PlayerAction -> Hunger -> Detection -> Companion follow -> Enemy AI (Combat) -> Noise resolve -> Last Stand -> cleanup`. (auditor) [Low]
- [x] [Review][Defer] Companion may be placed on / step onto an enemy or STAIRS_DOWN tile [core/src/main/java/com/margins/rogue/state/RunState.java (companionSpotNear), Companion.java (followStep)] — deferred, both use a walkable-only check with no enemy/stairs exclusion, so the ally can render stacked on an enemy or spawn on the down-stairs. The companion is a non-colliding ally by design, so this is cosmetic overlap, not a blocker; ally-collision policy is a deliberate Epic 6 concern. (edge+blind) [Low]

## Dev Agent Record

### Agent Model Used

Claude Opus 4.8 (1M context) — Dev Story workflow (bmad-dev-story)

### Implementation Plan

1. **Companion entity (Task 1)** — pure-model allied actor in `com.margins.rogue` (beside `RogueEnemy`/`RoguePlayer`): `tileX/tileY/bindId` + transient `RogueTileMap`. `followStep` mirrors `RogueEnemy.takeTurn`'s greedy x-then-y chase, gated by `isWalkable` and refusing to step onto the target tile; returns early once adjacent (allies don't shove). Deterministic, no RNG.
2. **RunState party (Task 2)** — `List<Companion> companions` (AD-10 single slot), `getActiveCompanion()` returns first-or-null. Spawned once at run start (ctor + `restart()`), placed via a ring-scan (`companionSpotNear`) that guarantees a walkable non-player tile. `restoreAfterLoad` re-injects the map into each companion like the enemies loop.
3. **Follow system (Task 3)** — `CompanionSystem.follow` runs in the Companion+Enemy-AI phase (AD-4): after `DetectionSystem.update`, before `CombatSystem.enemyPhase`, inside the `if (acted)` block.
4. **Descent (Task 4)** — extracted `placeFloorActors` (enemies + floorItems, avoiding a given tile) so `generateFloor` (run start: `new RoguePlayer`) and `descend` (repositions the *existing* player + companion via `placeAt`, preserving HP/hunger/inventory — AC-3) share the floor build. `TurnEngine` triggers descent when a MOVE lands on `STAIRS_DOWN`: ticks hunger, recomputes FOV, skips enemy/noise phases for the arrival turn.
5. **Render (Task 5)** — companion drawn in the entity pass with the cultist placeholder texture under an ally-green tint, gated by the same visibility rule enemies use.
6. **Persistence (Task 6)** — `json.setElementType(RunState.class, "companions", Companion.class)` beside enemies/floorItems.
7. **Verification (Task 7)** — throwaway headless harness (`exec-maven-plugin:3.1.0:java`), 26 checks across run-start/follow/descent/persistence; green on seeds 1, 7, 42, 1234, 9999, 2026, 55555. Desktop smoke-boot on `:0` ran 10s clean (including loading a pre-existing save with no `companions` key — loads as an empty party). Harness deleted after the run.

### Debug Log References

- **Follow-overlap attribution.** First harness run flagged "companion lands on the player" — root cause: the *player* walked onto the companion (the game lets the player walk through actors; `RoguePlayer.tryMove` only checks `isWalkable`, same as walking through enemies). `followStep`'s target-tile guard already guarantees the companion never moves onto the player. Harness updated to attribute overlap to the mover; AC-1 constrains the companion, which holds.
- **Post-load follow test.** The first post-load assertion followed a far *non-player* tile, so the companion legitimately stepped through the loaded player en route (guard only protects the follow target = the player's tile). Test changed to follow the loaded player, as the game does.
- **Pre-existing `saveVersion` field.** The working tree already carried an uncommitted `private int saveVersion;` (Epic 3 retro critical-path item) before this story started. It is untouched by this story and still uncommitted; `RunState` save-migration logic remains a separate retro action item.

### Completion Notes List

- ✅ All 7 tasks complete; all 5 ACs satisfied and verified headlessly.
- ✅ AC-1 follow: greedy x-then-y, walkable-gated, never onto the player, stops at adjacency (micro-tested).
- ✅ AC-2/AC-3 descent: `floorDepth++`, same `RoguePlayer` instance, HP/hunger/inventory preserved (hunger ticks once on the descent turn), new `tileMap`, party carried to the new entrance.
- ✅ AC-4/AD-4: companion is a `RunState`-owned allied turn actor in its own list, moved by `CompanionSystem` inside the turn pipeline.
- ✅ AC-5/AD-6: Json round-trip preserves companion tile/bindId + floorDepth; map re-injected post-load (follow works).
- ✅ Scope respected: generic `Companion` (placeholder `"galleon"` bind), one-way descent only (STAIRS_UP / route progression are Epic 6), no combat contribution (NFR-5).
- ⏳ **Outstanding human check:** the on-screen visual confirmation (companion follows and descends on `:0`) — smoke boot confirmed clean init/render; the interactive walk is for the player to eyeball.

### File List

- `core/src/main/java/com/margins/rogue/Companion.java` (new)
- `core/src/main/java/com/margins/rogue/system/CompanionSystem.java` (new)
- `core/src/main/java/com/margins/rogue/state/RunState.java` (companions list + getters, spawn-once, `placeFloorActors` refactor, `descend()`, `restoreAfterLoad` injection)
- `core/src/main/java/com/margins/rogue/RoguePlayer.java` (`placeAt(int, int)`)
- `core/src/main/java/com/margins/rogue/system/TurnEngine.java` (`CompanionSystem.follow` call; `STAIRS_DOWN` descent trigger)
- `core/src/main/java/com/margins/rogue/RogueGameScreen.java` (companion render, ally tint)
- `core/src/main/java/com/margins/rogue/save/SaveService.java` (`companions` element-type registration)
- `core/src/main/java/com/margins/rogue/CompanionHarness.java` (throwaway — created for Task 7, deleted after)

## Change Log

- 2026-08-03 — Story 4.1 implemented: generic `Companion` entity, single-slot party on `RunState`, follow-as-ordered-turn-system, minimal one-way stair descent carrying the party, placeholder render, save/load wiring. Verified headlessly (26 checks, 7 seeds); smoke-boot clean. Status ready-for-dev → in-progress → review.

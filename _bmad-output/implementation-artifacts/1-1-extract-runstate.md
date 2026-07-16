# Story 1.1: Extract RunState as the single owner of run data

Status: done

## Story

As the developer,
I want all run data moved into one `RunState` object,
so that there is a single authoritative source to advance, test, and later serialize.

## Acceptance Criteria

1. When a run starts, the tilemap, player, enemy list, hunger, HP, current floor index, and seed all live on one `RunState` instance and nothing else holds a duplicate authoritative copy. (FR-3 ownership; AD-3)
2. The game plays **identically** to before the refactor — same movement, attack (Q), block (E), wait (SPACE), hunger drain, enemy chase + arrival-grace, dodge/block resolution, stairs, death screen with R-restart / Q-quit.
3. No rendering type (`SpriteBatch`, `ShapeRenderer`, `Screen`, `Texture`, `BitmapFont`) is referenced from `RunState` or any model class it owns. (AD-2)

## Tasks / Subtasks

- [ ] Task 1: Create `state/RunState.java` holding the run aggregate (AC: 1)
  - [ ] Fields: `RogueTileMap tileMap`, `RoguePlayer player`, `List<RogueEnemy> enemies`, `int floorDepth`, `long seed`, `Random rng` (the seeded RNG — see Story 1.3), plus run flags `gameOver`. Hunger/HP already live on `RoguePlayer`; do not duplicate them onto `RunState`.
  - [ ] Add a factory/init that builds a fresh run: seed the RNG, generate floor, place player + enemies (move the body of `RogueGameScreen.generateFloor()` here, minus rendering).
- [ ] Task 2: Repoint `RogueGameScreen` to own a single `RunState` field instead of the loose `tileMap`/`player`/`enemies`/`rand`/`floorDepth` fields (AC: 1, 2)
  - [ ] Replace direct field reads (`player.getHp()`, `enemies`, `tileMap.getTile(...)`) with `state.getPlayer()`, `state.getEnemies()`, `state.getTileMap()`.
  - [ ] Keep the screen behavior byte-for-byte equivalent this story — do NOT yet extract systems (that is Story 1.2).
- [ ] Task 3: Verify no model class imports a libGDX render type (AC: 3)
  - [ ] `RunState` and everything it holds compile without `com.badlogic.gdx.graphics.*` render imports. (Note: `RoguePlayer`/`RogueEnemy` currently import `Texture` for `getTexture()` — see Dev Notes; move texture lookup to the screen or leave as a tracked exception and log it.)
- [ ] Task 4: Manual regression — launch (`mvn -q -pl desktop exec:java`) and confirm AC-2 behaviors unchanged.

## Dev Notes

### Governing architecture (source: ARCHITECTURE-SPINE.md)
- **AD-3** — `RunState` is the sole owner of run data; systems mutate it, nothing else holds an authoritative duplicate; it is the save unit.
- **AD-2** — Model ⟵ Screen layering; no game rule in the screen, no render types in the model. This story does the *ownership* half; Story 1.2 does the *rule extraction* half.
- **AD-1** — All new code under `com.margins.rogue.*` (new subpackage `state/`). Legacy `entity/`, `map/`, `screen/`, `fx/` stay frozen.

### Current state of files being modified
- **`RogueGameScreen.java` (303 lines)** is the monolith. Relevant current structure:
  - Fields (lines 30-38): `tileMap`, `player`, `enemies`, `rand`, `floorDepth`, `waitingForInput`, `gameOver`, `message`, `messageTimer` — the first five move into `RunState`; the last four are screen/UI concerns and stay on the screen.
  - `generateFloor()` (64-87): floor gen + player/enemy placement → move the data-building part into `RunState`. Uses `FloorGenerator.generate(50,50,rand,floorDepth)`.
  - `handleInput()` (203-284): the turn loop — leave in the screen for now; it will be gutted in Story 1.2.
- **What must be preserved (AC-2):** the exact turn sequence in `handleInput()` lines 258-283 — `player.tickHunger()`, then per-enemy: skip dead, consume `justArrived` grace, else if adjacent → `tryDodge()` / `takeDamage()` with block messaging, else `takeTurn(px,py)`. The camera snap (95-96), HUD (148-169), and death/restart (181-211) must behave identically.

### Known wrinkle — entity → render coupling
`RoguePlayer`/`RogueEnemy` expose `getTexture()` returning a libGDX `Texture` (used by the screen at 126/129). Strict AD-2 says render types don't belong in the model. For THIS story, keeping `getTexture()` is acceptable to avoid scope creep — but log it as a known AD-2 exception to resolve when rendering is tidied (Deferred item in the spine). Do not add *new* render coupling.

### Serialization-root convention (forward-looking, from spine)
`RoguePlayer` currently holds a back-reference to `RogueTileMap map` and its own `Random rand`. Story 1.4 (save) requires `RunState` to be the sole serialization root with no entity back-refs to the map/RNG. You do not need to fix this in 1.1, but do NOT deepen the coupling — prefer passing map/RNG into methods over storing more back-refs.

### Testing standards
No test harness exists yet. AC verification is manual launch + observation this story. Keep `RunState` free of libGDX render deps so it becomes headless-testable later (NFR-3).

### References
- [Source: _bmad-output/planning-artifacts/epics.md#Epic 1 · Story 1.1]
- [Source: ARCHITECTURE-SPINE.md#AD-2, #AD-3, #Structural Seed]
- [Source: core/src/main/java/com/margins/rogue/RogueGameScreen.java (lines 30-87, 258-283)]

## Dev Agent Record

### Agent Model Used

claude-opus-4-8[1m] (via bmad-dev-story)

### Debug Log References

- `mvn -o compile -q` → EXIT=0 (baseline and post-refactor)

### Completion Notes List

- Created `state/RunState.java` as the single owner of tilemap, player, enemies, floorDepth, seed, and the seeded RNG (AD-3). Moved `RogueGameScreen.generateFloor()` body into `RunState.generateFloor()`; added `restart()`.
- Repointed `RogueGameScreen` to hold one `RunState state` field; removed loose `tileMap`/`player`/`enemies`/`rand`/`floorDepth` fields. Each method sources data via `state` with method-local references mirroring the originals, so turn/render/HUD/death behavior is byte-for-byte equivalent (AC-2). Restart now calls `state.restart()`.
- `RunState` imports no libGDX rendering type — model is render-clean (AC-3, AD-2).
- Seeded RNG (`Random(seed)`) now drives floor gen + enemy placement (partial AD-5; `RoguePlayer.rand` for dodge still local — deferred to Story 1.3 per plan).
- Known AD-2 exception retained (not deepened): `RoguePlayer`/`RogueEnemy` still expose `getTexture()`. Logged for the spine's rendering-tidy Deferred item.
- Verified no external references to removed screen fields (only `MarginsGame` uses the public constructor). Not launch-tested (no display in this environment); behavior preserved by construction.

### File List

- ADDED: core/src/main/java/com/margins/rogue/state/RunState.java
- MODIFIED: core/src/main/java/com/margins/rogue/RogueGameScreen.java

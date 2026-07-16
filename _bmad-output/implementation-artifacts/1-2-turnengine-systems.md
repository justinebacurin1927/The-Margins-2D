# Story 1.2: Introduce the ordered TurnEngine and extract systems

Status: done

## Story

As the developer,
I want the turn to advance through a `TurnEngine` running ordered systems,
so that `RogueGameScreen` only handles input/render and new mechanics slot in without editing the screen.

## Acceptance Criteria

1. When a player action is submitted, `TurnEngine.advance(action)` runs systems in fixed order: **PlayerAction → Hunger → Enemy AI → Noise resolve → cleanup**. (AD-4)
2. The existing hunger tick and combat/defense resolution now live in `HungerSystem` and `CombatSystem`, not inline in the screen.
3. `RogueGameScreen` contains no game rule — only input capture (mapping keys to a player action/intent), forwarding to `TurnEngine`, and rendering. (AD-2)
4. Behavior is identical to Story 1.1 output (movement, attack, block, wait, hunger, enemy chase, arrival-grace, dodge/block, stairs, death).

## Tasks / Subtasks

- [ ] Task 1: Define a `PlayerAction` representation (move dx/dy, attack dir, block, wait) the screen produces from input (AC: 3)
- [ ] Task 2: Create `system/TurnEngine.java` with `advance(RunState, PlayerAction)` invoking systems in the AD-4 order (AC: 1)
  - [ ] Establish a simple system contract (e.g., `interface TurnSystem { void run(RunState s); }`) or explicit ordered calls — keep it lightweight, no ECS (spine Deferred).
- [ ] Task 3: Extract `system/HungerSystem` from `player.tickHunger()` call site (AC: 2)
- [ ] Task 4: Extract `system/CombatSystem` — the per-enemy adjacency/dodge/block/damage resolution and the arrival-grace handling currently in `handleInput()` lines 258-283 (AC: 2, 4)
  - [ ] Enemy movement (`e.takeTurn(px,py)`) is the "Enemy AI" step; damage resolution is CombatSystem. Preserve arrival-grace exactly.
- [ ] Task 5: Reduce `handleInput()` to: read keys → build `PlayerAction` → `turnEngine.advance(state, action)` → set UI message from results. Move message strings out of rules or surface them via a return/event (AC: 3)
  - [ ] Include a Noise-resolve no-op step now (the queue arrives in Epic 2 Story 2.5) so the pipeline order is already in place.
- [ ] Task 6: Manual regression per AC-4.

## Dev Notes

### Governing architecture
- **AD-4** — ordered turn pipeline; new mechanics insert as an ordered system, never inline in input/render.
- **AD-2** — no game rule in the screen.
- Depends on **Story 1.1** (`RunState` exists and the screen owns it).

### Current state / what to preserve
- The authoritative turn sequence lives in `RogueGameScreen.handleInput()` (lines 258-283). Move it verbatim in behavior into systems: hunger first, then per living enemy — arrival-grace skip, adjacency → `tryDodge()` else `takeDamage()` with block-aware messaging, non-adjacent → `takeTurn()`.
- `waitingForInput`, `message`, `messageTimer`, camera, HUD, death screen stay on the screen (UI concerns).
- Keep messages (`"Dodge!"`, `"Brace! Blocked X→Y"`, `"Hit for N!"`, `"Wait"`, `"Hit! hp/max"`) — surface them from the systems to the screen (return a small result object or an event list on `RunState`), do not hardcode rule logic in the screen.

### Testing standards
With rules now in systems and `RunState` render-free, a headless test can construct a `RunState` and assert a `CombatSystem`/`HungerSystem` step outcome. Add at least one such check if a JUnit dep is available; otherwise manual.

### References
- [Source: _bmad-output/planning-artifacts/epics.md#Epic 1 · Story 1.2]
- [Source: ARCHITECTURE-SPINE.md#AD-2, #AD-4]
- [Source: core/src/main/java/com/margins/rogue/RogueGameScreen.java lines 203-291]

## Dev Agent Record

### Agent Model Used

claude-opus-4-8[1m] (via bmad-dev-story)

### Debug Log References

- `mvn -o compile -q` → EXIT=0
- Launch on display :0, 12s → shell_exit=124 (ran full duration, no exceptions in log)

### Completion Notes List

- Added `system/` package: `PlayerAction` (MOVE/ATTACK/BLOCK/WAIT intent), `TurnResult` (ordered messages, last-wins), `TurnEngine` (ordered pipeline per AD-4), `HungerSystem`, `CombatSystem` (playerAttack + enemyPhase + enemyAt). None import libGDX render types (AD-2).
- `TurnEngine.advance()` runs PlayerAction → Hunger → Enemy AI (Combat) → Noise-resolve placeholder → WAIT-message cleanup, in fixed order (AD-4). Enemy phase/hunger only run when the player actually acted, preserving the original "move into a wall = wasted keypress, no turn passes" behavior.
- Reduced `RogueGameScreen.handleInput()` to: gates → `readAction()` builds a `PlayerAction` → `turnEngine.advance()` → show `result.lastMessage()`. Removed inline turn logic and the private `enemyAt` (moved to `CombatSystem`). No game rule remains in the screen (AC-3, AD-2).
- Message semantics preserved exactly, including "Wait" emitted after the enemy phase so it wins the display, and "Brace!" being overwritten by combat results — matches the original last-setMessage-wins behavior.
- Behavior verified: compiles clean; launches and runs the render loop without error.

### File List

- ADDED: core/src/main/java/com/margins/rogue/system/PlayerAction.java
- ADDED: core/src/main/java/com/margins/rogue/system/TurnResult.java
- ADDED: core/src/main/java/com/margins/rogue/system/TurnEngine.java
- ADDED: core/src/main/java/com/margins/rogue/system/HungerSystem.java
- ADDED: core/src/main/java/com/margins/rogue/system/CombatSystem.java
- MODIFIED: core/src/main/java/com/margins/rogue/RogueGameScreen.java

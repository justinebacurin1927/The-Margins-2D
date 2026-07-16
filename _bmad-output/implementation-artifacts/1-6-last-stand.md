# Story 1.6: Last Stand reprieve before permadeath

Status: done

## Story

As Justine (player),
I want one desperate reprieve the first time a blow would kill Milek,
so that death feels earned, not cheap.

## Acceptance Criteria

1. Given Milek has not yet used Last Stand this run, when a hit would drop him to ≤0 HP, he instead survives at 1 HP in a flagged desperate state for that turn and a message communicates the reprieve. (FR-16)
2. Given Last Stand is already spent, when another lethal event occurs, the run ends via permadeath. (FR-17)
3. Starting a new run resets the Last Stand flag.

## Tasks / Subtasks

- [x] Task 1: Added `boolean lastStandUsed` (persisted) to `RunState`; serialized by Story 1.4's Json save, and reset in `restart()` (the `R`-restart reuses the same `RunState`, so it needs an explicit reset — a fresh launch already defaults it false) (AC: 1, 3).
- [x] Task 2: Added `CombatSystem.checkLastStand(state, messages)`, run once per turn after all damage (AC: 1, 2)
  - [x] If HP ≤ 0 and `!lastStandUsed`: `player.reviveTo(1)`, `lastStandUsed = true`, desperate `lastStand = true`, emit `"Last Stand!"`.
  - [x] If HP ≤ 0 and `lastStandUsed`: no revive — HP stays 0 → existing permadeath path (Story 1.5) fires next frame.
- [x] Task 3: Desperate flag `lastStand` is transient and cleared at the start of every `TurnEngine.advance()` (turn-boundary cleanup); the reprieve is a one-time HP floor, not invulnerability — the second lethal event kills. Verified.
- [x] Task 4: Verified via headless AC suite (12 assertions): first lethal → survive at 1 HP + flag + message; second lethal → death, no second message; `restart()` resets and reprieve is available again. Plus hunger-starvation triggers the same reprieve, and `lastStandUsed` survives save/load. Live boot clean.

## Dev Notes

### Governing architecture
- **AD-3/AD-4** — Last Stand state lives on `RunState`; the check runs inside `CombatSystem` in the turn pipeline, not in the screen.
- Tuning (PRD Balance): trigger = first lethal hit per run; survive at 1 HP, flagged desperate, 1 turn; 1 use per run.

### Current state / what to preserve
- Damage currently applied by `RoguePlayer.takeDamage(int)` returning dealt damage (called at `RogueGameScreen` line 271). After Story 1.2 this logic is in `CombatSystem`; add the Last Stand interception there so the reprieve applies to ALL lethal sources (starvation from `HungerSystem` too — verify hunger's `-1 HP/turn` at 0 also routes through the same lethal check or add the check to hunger death as well).
- Preserve the existing armor/dodge/block math; Last Stand is a post-resolution HP-floor, applied after final damage is computed.

### Depends on
Stories 1.1, 1.2 (CombatSystem). Feeds Story 1.5 (defines "true death").

### References
- [Source: _bmad-output/planning-artifacts/epics.md#Epic 1 · Story 1.6]
- [Source: PRD FR-16, FR-17, Balance table (Last Stand)]
- [Source: core/src/main/java/com/margins/rogue/RoguePlayer.java takeDamage; RogueGameScreen.java line 271]

## Dev Agent Record

### Agent Model Used

claude-opus-4-8[1m] (via bmad-dev-story)

### Debug Log References

- `mvn -o compile` → BUILD SUCCESS
- Headless AC suite: 12/12 PASS (AC-1/2/3 + hunger source + save/load persistence) → ALL AC PASS
- Launch on display :0, 8s → clean boot

### Completion Notes List

- Last Stand state lives on `RunState` (AD-3), not the player: `lastStandUsed` is per-run (persists across floors/saves; a player-scoped flag would wrongly reset on floor transition since `generateFloor()` recreates the player), and the turn-scoped `lastStand` desperate flag is transient.
- The reprieve is a single post-damage HP floor applied in the turn pipeline (AD-4): `TurnEngine` runs `CombatSystem.checkLastStand()` after hunger + enemy phase, so it covers ALL lethal sources — enemy hits and hunger starvation both funnel through `RoguePlayer.takeDamage` and are caught by the end-of-turn check. Existing armor/dodge/block math is untouched (Last Stand applies after final damage).
- Message ordering: `checkLastStand` is called last in the pipeline so `"Last Stand!"` wins the last-message-wins HUD display over combat/"Wait" text.
- Closes the Story 1.5 loop: the reprieve revives the player inside `advance()`, before the screen's next-frame `!isAlive()` game-over transition — so `SaveService.deleteSave()` only runs on TRUE death (reprieve already spent).
- `restart()` resets `lastStandUsed`/`lastStand` because the `R` restart reuses the same `RunState` instance.

### File List

- MODIFIED: core/src/main/java/com/margins/rogue/state/RunState.java
- MODIFIED: core/src/main/java/com/margins/rogue/RoguePlayer.java
- MODIFIED: core/src/main/java/com/margins/rogue/system/CombatSystem.java
- MODIFIED: core/src/main/java/com/margins/rogue/system/TurnEngine.java

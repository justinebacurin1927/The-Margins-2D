# Story 1.6: Last Stand reprieve before permadeath

Status: ready-for-dev

## Story

As Justine (player),
I want one desperate reprieve the first time a blow would kill Milek,
so that death feels earned, not cheap.

## Acceptance Criteria

1. Given Milek has not yet used Last Stand this run, when a hit would drop him to ≤0 HP, he instead survives at 1 HP in a flagged desperate state for that turn and a message communicates the reprieve. (FR-16)
2. Given Last Stand is already spent, when another lethal event occurs, the run ends via permadeath. (FR-17)
3. Starting a new run resets the Last Stand flag.

## Tasks / Subtasks

- [ ] Task 1: Add `boolean lastStandUsed` to `RunState` (AC: 1, 3) — included in save (Story 1.4), reset on new run
- [ ] Task 2: In `CombatSystem` damage application (extracted in Story 1.2, originally `RoguePlayer.takeDamage`), intercept lethal damage (AC: 1, 2)
  - [ ] If resulting HP ≤ 0 and `!lastStandUsed`: set HP = 1, set `lastStandUsed = true`, set a `lastStand` desperate flag for the turn, emit message (e.g., "Last Stand!").
  - [ ] If resulting HP ≤ 0 and `lastStandUsed`: allow death (HP ≤ 0 → permadeath path unchanged).
- [ ] Task 3: Confirm the desperate flag is turn-scoped (cleared on cleanup step) and does not grant invulnerability beyond the single reprieve (AC: 1)
- [ ] Task 4: Manual test — take lethal hit (survive at 1 HP once), take another (die); restart and confirm reprieve available again (AC: 1, 2, 3)

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

### Debug Log References

### Completion Notes List

### File List

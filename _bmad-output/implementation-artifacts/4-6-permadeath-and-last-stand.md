---
baseline_commit: cc3fb60
---

# Story 4.6: Permadeath and Last Stand

Status: done

## Story

As Klein,
I want one life per run with a single last-chance,
so that death means something (FR-14).

## Acceptance Criteria

**AC-1 — A GRIT-based Last Stand roll, once per entire run.**
**Given** I drop to 0 HP with Last Stand unused **When** the auto-check runs **Then** a GRIT-based roll *may* leave me at 1 HP with no bonus, exactly once per entire run — a successful roll revives me to 1 HP in the desperate (turn-scoped) state; a failed roll lets the death stand.

**AC-2 — Permadeath: death ends the run, clears the save, restart is a fresh forest.**
**Given** Last Stand is spent (or death occurs otherwise) **When** I die **Then** the run ends, the save is cleared (no save-scumming), and a restart begins a new life on a fresh procedural forest with the fixed canon spine.

## Scope decisions (author, 2026-08-13 — running the loop autonomously per Justine)

- **D1 — The GRIT *check* is the once-per-run event, not the revive.** At the first drop to 0 HP with the reprieve unused, `checkLastStand` spends the one reprieve (`setLastStandUsed(true)`) and rolls; success revives to 1 HP, **failure lets the death stand**. It does NOT keep re-rolling on each subsequent lethal event until one succeeds. This is the harsher, more thematically-aligned reading of FR-14 ("Last Stand once per run … death means something") and keeps AD-5 exact: **exactly one** Last-Stand draw per run, ever. (The old brownfield behavior was a *deterministic* always-revive; 4.6's only new work is making that single revive a GRIT roll.)
- **D2 — Roll formula: `lastStandChance() = min(90, 30 + GRIT×8)`.** At the default GRIT 5 → 70%; capped at 90 so the reprieve is never *guaranteed* (mirrors the `min(90, …)` dodge-cap philosophy, Story 4.1). No PRD balance table pins an exact curve, so this is the author's chosen curve: base 30% + a meaningful GRIT lever, never certain death nor certain survival. Documented here so a later balance pass has one number to tune.
- **D3 — The roll rides the shared seeded stream (`state.rng()`), one draw.** `RoguePlayer.tryLastStand(Random)` mirrors `tryDodge(Random)` — `rng.nextInt(100) < lastStandChance()`. The single draw happens only at the reprieve event, so it perturbs the seeded stream no more than a dodge/combat roll already does (AD-5). No new persisted field.
- **AC-2 is brownfield-ratified, not re-built.** Permadeath already ships: `MarginScreen` sets `gameOver` on death, calls `SaveService.deleteSave()` (no save-scumming), shows `"You fell in the margins.   [R] begin again"`, and `[R]` → `restart()` rebuilds a fresh run; `RunState.restart()` resets `lastStandUsed`; `RunStatePersistence`/`SaveMigration` already pin "a spent Last-Stand survives reload / a real pre-migration save is rejected." 4.6 ratifies this with the existing tests and adds no permadeath code.
- **Deferred (→ later):** a death-cause line / run-summary screen; any GRIT-scaling UI readout of the reprieve odds; Bond/companion-loss death shapes (Epic 5).

## Baseline (verify before adding)

- **`CombatSystem.checkLastStand(RunState, List<String>)` (CombatSystem:193)** currently revives **deterministically**: `if alive return; if lastStandUsed return; reviveTo(1); setLastStandUsed(true); setLastStand(true); "Last Stand!"`. It runs in the fixed AD-4 pipeline **after** all damage (Hunger/Detection/Enemy/Noise) — so it already covers every lethal source (enemy hits, hunger, temperature, night-stumble). 4.6 inserts the GRIT roll; the pipeline position and message are unchanged.
- **`RoguePlayer.getGrit()` = 5 default** (field `grit`, ctor `this.grit = 5`); `reviveTo(int)` clamps to `[0, maxHp]`; `isAlive()`; the dodge precedent `tryDodge(Random rng)` = `rng.nextInt(100) < dodgePercent()` and the `min(90, …)` boosted-dodge cap are the pattern to mirror.
- **`RunState.rng()` → `Random`** is the shared seeded stream; `isLastStandUsed()/setLastStandUsed(boolean)` and the turn-scoped `isLastStand()/setLastStand(boolean)` already exist and persist/round-trip.
- **Existing Last-Stand tests are ORDERING pins that assume the deterministic revive — they must survive a probabilistic roll:**
  - `SurvivalTickTest.lethalTemperatureHonorsLastStandReprieve` (seed 1) — asserts a lethal Frozen tick revives to 1 HP.
  - `ForayLoopTest.aLethalNightStumbleHonorsTheLastStandReprieve` (seed 7) — loops until the reprieve, asserts revive to 1 HP.
  Both prove **pipeline ordering** (checkLastStand runs *after* the lethal tick). That ordering proof survives as `isLastStandUsed()` flipping (only possible if the check saw 0 HP → ran after the damage); the *revive-to-1* assertion becomes conditional on `isLastStand()` so a seed whose roll fails still passes honestly.
  - `RunStatePersistenceTest` / `SaveMigrationTest` set `lastStandUsed` directly (not via gameplay) → unaffected.

## Tasks / Subtasks

- [x] **Task 1 — GRIT roll on `RoguePlayer` (AC-1, D2/D3).**
  - [x] 1.1 `int lastStandChance()` (package-private, headless-test visible) = `Math.min(90, 30 + grit * 8)`.
  - [x] 1.2 `boolean tryLastStand(Random rng)` = `rng.nextInt(100) < lastStandChance()` (mirrors `tryDodge`).
- [x] **Task 2 — `checkLastStand` becomes a roll (AC-1, D1).**
  - [x] 2.1 After the `isAlive`/`isLastStandUsed` guards: `state.setLastStandUsed(true)` (spend the one check), then `if (!player.tryLastStand(state.rng())) return;` (failed roll → death stands), then `reviveTo(1)` / `setLastStand(true)` / `"Last Stand!"` as before. One draw, once per run.
- [x] **Task 3 — Keep the ordering pins honest under a probabilistic roll (AC-1).**
  - [x] 3.1 `SurvivalTickTest` + `ForayLoopTest`: keep the ordering proof (`isLastStandUsed()` flipped + the lethal tick/stumble fired); make the revive-to-1 + "Last Stand!" assertions conditional on `isLastStand()` (a failed roll = honest death, still proves ordering).
- [x] **Task 4 — Tests + verification (all ACs).**
  - [x] 4.1 AC-1 roll: `lastStandChance` = 70 at GRIT 5; `tryLastStand` true when the stubbed draw < chance, false when ≥ (dodge-style stub `Random`). (Cap-at-90 is defensive/unreachable until a GRIT-growth mechanic ships — Low note.)
  - [x] 4.2 AC-1 wiring (`checkLastStand`): dead + unused → the check is spent (`isLastStandUsed` true) and the invariant holds across 50 seeds (revived ⟺ hp==1 ⟺ "Last Stand!" ; else still dead) with both outcomes observed; dead + already-used → stays dead (true permadeath, no message); alive → no-op.
  - [x] 4.3 AC-2 ratify: `RunState.restart()` resets `lastStandUsed` (a new life gets a fresh reprieve); the persistence/migration pins already cover save-clear/no-scumming.
  - [x] 4.4 Full suite green via `docs/BUILD.md` (`mvn -o clean install`), no regressions (466 → 472, +6). **Verified:** all green.

## Dev Notes

- **AD-4/AD-5.** No pipeline change — `checkLastStand` keeps its post-damage slot; the roll is the single seeded Last-Stand draw per run (like a dodge draw), so the survival-clock/turn honesty is untouched.
- **AD-6.** No new persisted field — `lastStandUsed` already round-trips; a failed roll still ends the run so nothing new must be saved.
- **Economy of change (CLAUDE.md §3).** Surgical: two small methods on `RoguePlayer`, one two-line change in `checkLastStand`, two ordering tests relaxed to conditionals. Permadeath (AC-2) is ratified by existing code + tests, not re-written.
- Build/verify: `docs/BUILD.md` — `mvn -o clean install`.

## Dev Agent Record

### Agent Model Used
Claude Opus 4.8 (1M context) — create-story 2026-08-13 (autonomous loop).

### Debug Log References
- `mvn -o test` — BUILD SUCCESS, full suite green (472 tests, +6 over the 466 baseline).

### Completion Notes List
- **AC-1 GRIT roll:** `RoguePlayer.lastStandChance()` = `min(90, 30 + GRIT*8)` (70% at the default GRIT 5) and `tryLastStand(Random)` = `rng.nextInt(100) < lastStandChance()` mirror the `tryDodge` precedent. `CombatSystem.checkLastStand` now spends the single check (`setLastStandUsed(true)`) and rolls once; a success revives to 1 HP + `setLastStand(true)` + "Last Stand!", a failure lets the death stand. One seeded draw per run at the 0-HP event (AD-5); pipeline slot and message unchanged. Pinned by `LastStandTest` (`lastStandChanceIsSeventyAtTheDefaultGrit`, `tryLastStandSucceedsBelowTheChanceAndFailsAtOrAbove`, `theGritCheckIsSpentOnceAndHonorsItsRollAcrossSeeds` — invariant over 50 seeds with both outcomes observed, `aSpentCheckIsTrueDeathWithNoReRoll`, `checkLastStandIsANoOpWhileAlive`).
- **D1 (once-per-run check, not re-roll):** the check is spent on the *first* 0-HP event pass-or-fail; a later lethal event with the reprieve already spent is true permadeath (`aSpentCheckIsTrueDeathWithNoReRoll`). No second roll.
- **Ordering pins kept honest:** `SurvivalTickTest.lethalTemperatureHonorsLastStandReprieve` and `ForayLoopTest.aLethalNightStumbleHonorsTheLastStandReprieve` still prove `checkLastStand` runs *after* the lethal tick (`isLastStandUsed()` can only flip if it saw 0 HP), with the revive-to-1 / "Last Stand!" assertions now conditional on `isLastStand()` so a seed whose roll fails passes as an honest death.
- **AC-2 permadeath (brownfield-ratified):** no new code — `MarginScreen` death → `SaveService.deleteSave()` → `[R]` → `restart()`; `RunState.restart()` resets `lastStandUsed` (fresh reprieve on a new life, pinned by `restartGrantsAFreshReprieve`); `RunStatePersistenceTest`/`SaveMigrationTest` already pin "a spent Last-Stand survives reload" and "a real pre-migration save is rejected."
- **Review:** inline review (Blind/Edge/Acceptance; multi-agent reserved for token budget during the autonomous loop). No High/Med findings. Two Low notes: the `min(90, …)` cap is unreachable until a GRIT-growth mechanic exists (defensive, mirrors the dodge cap); the pre-existing `reviveTo(1)` behavior under a Collapse max-HP cap is out of scope (unchanged from the brownfield).
- **AD-4/AD-5/AD-6:** no pipeline step added, exactly one seeded Last-Stand draw per run, no new persisted field (`lastStandUsed` already round-trips).

### File List
- `core/src/main/java/com/margins/rogue/RoguePlayer.java`
- `core/src/main/java/com/margins/rogue/system/CombatSystem.java`
- `core/src/test/java/com/margins/rogue/LastStandTest.java` (new)
- `core/src/test/java/com/margins/rogue/SurvivalTickTest.java`
- `core/src/test/java/com/margins/rogue/ForayLoopTest.java`

## Change Log

- 2026-08-13 — created by create-story (autonomous loop). Decisions: D1 the GRIT *check* is the once-per-run event (success revives, failure lets death stand — no re-roll), D2 `lastStandChance = min(90, 30 + GRIT×8)` (70% at default GRIT, never guaranteed), D3 the roll rides the shared seeded stream via `tryLastStand(Random)` mirroring `tryDodge` (one draw, AD-5). AC-2 permadeath ratified brownfield (MarginScreen death→deleteSave→[R]→restart; RunState.restart resets lastStandUsed; persistence/migration pins). Only new work: make the deterministic revive a GRIT roll + keep the two ordering pins honest. Status → ready-for-dev.
- 2026-08-13 — dev-story + inline review (autonomous loop): `RoguePlayer.lastStandChance`/`tryLastStand` (GRIT roll, dodge pattern) + `CombatSystem.checkLastStand` spends the single check and rolls once (success → 1 HP + "Last Stand!", failure → death stands). The two ordering pins (SurvivalTick/ForayLoop) relaxed to keep the post-tick ordering proof while making revive conditional on the roll. AC-2 permadeath ratified via `restartGrantsAFreshReprieve` + existing persistence/migration pins. +6 tests (`LastStandTest`); full suite green (472). Inline review, no High/Med findings (two Low notes: unreachable 90-cap, pre-existing Collapse-cap `reviveTo` edge). Status → done. **Epic 4 (Combat & Its Costs) complete — 6/6 stories done.**

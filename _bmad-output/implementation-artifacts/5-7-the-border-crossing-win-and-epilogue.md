---
baseline_commit: c623b80
---

# Story 5.7: The border-crossing win and epilogue

Status: done

## Story

As Klein,
I want the homecoming to be a final tense run past an act-scaled cordon,
so that the win is escape, earned, not a boss (FR-18, AD-12, AD-11 channel b).

## Acceptance Criteria

**AC-1 — The NW border crossing is an Act-3, cordon-gated tense run, not a boss.**
**Given** Act 3 (and the last provisioning done) **When** I reach the NW border **Then** the crossing is a scripted run against the Giliman cordon — **always physically walkable**, but survivable only with Act-3 readiness because the cordon (AD-11 channel b) has thinned as the acts advanced.

**AC-2 — Surviving the crossing lands the canonical win + epilogue (SM-1).**
**Given** I survive the crossing **When** I cross into Novelborne **Then** the canonical ending lands (he makes it home), the run ends as a **victory** (not death), and the epilogue seeds connect to main-story canon (Corneo → Coneros, the Mercenary Graveyard filling now) — validating SM-1 (the story is lived through the systems).

## Scope decisions (author, 2026-08-14 — running the loop autonomously per Justine)

- **D1 — Channel-b cordon: a thinning-per-act presence at the NW border (AD-11 channel b, the win gate).** Add a pure, rng-free `RunState.cordonCountFor(int act)` — the **dual** of channel a: it *thins* as the act rises (Act 1 → 3 cordon foes, Act 2 → 2, Act 3 → 1) because the war consolidates east. It populates enemies **near the border landmark** inside the `inCordon` box (far-west + far-north), **separately** from the `enemyCountFor` interior loop. **Critical (AD-11 "do not merge"):** cordon population must stay OUT of `enemyCountFor` — `enemyCountFor(cordon, act)` must remain `0` so `OccupationEscalationTest.theNwBorderCordonIsNeverThickened` stays green (channel a still never touches the cordon; channel b does). By Act 3 the cordon is thinnest → the crossing is survivable with Act-3 readiness (AC-1).
- **D2 — The crossing = the win, via a headless one-shot controller.** New `narrative/BorderCrossingController.resolve(RunState)` (the `CaptureController`/`ActGateController` "safe every-frame call" precedent): while `getAct() >= 3`, when the player reaches the border box (within a small band of `WorldSpine.borderX()/borderY()`), set `KEY_WON`, append the epilogue beat, and end the run as a **victory**. The "bounded-turn scripted run" (AC-1) is realized as the **tense approach through the thinned channel-b cordon** — the border is **always walkable** (AD-12 permadeath honesty), so the gate is *surviving the cordon*, not an invisible wall or a timed lockout. The explicit turn-countdown "gauntlet timer" is deferred polish (see Deferred).
- **D3 — A victory end-state, paralleling `gameOver`.** New persisted `KEY_WON` flag (unset → 0, AD-6 — no ctor-rolled default needed). `MarginScreen` renders a **victory** end-state that mirrors the existing `gameOver` block (§`update`, ~L496): seed the epilogue lines into the log, `SaveService.deleteSave()` (the run is complete — no save-scum, mirroring permadeath), `[R] begin again`. The headless core owns `KEY_WON` + the epilogue text (controller `LINE_*` constants); the screen only reads the flag and renders (AD-1/AD-2). `restart()` clears the win state alongside `gameOver`.
- **D4 — "Last provisioning done" collapses to "Act 3 reached."** The full Act-3 spine (the choice → last provisioning → Aldric resolution → closing trap, FR-18) was deferred by Story 5.6; reaching Act 3 IS the gate (via 5.6's `resolveRescue` 2→3 flip). 5.7 does not build the provisioning beats — the precondition is satisfied by `getAct() >= 3`. The elaborate Act-3 content stays deferred.
- **D5 — Epilogue seeds validate SM-1, with a minimal Aldric branch.** The win appends epilogue lines connecting to canon: **Corneo → Coneros** (the town's name in the later era) and **the Mercenary Graveyard filling now**. One cheap branch on Aldric's fate makes the ending *lived through the systems* (SM-1): if Aldric is active/rescued (`getActiveCompanionId() == ALDRIC`), he crosses beside Klein; otherwise Klein crosses alone, carrying his memory. Text lives in the controller (the `CaptureController.LINE_*` pattern); no dialogue engine.
- **D6 — Wire site & layering (AD-1/AD-2/AD-4).** `BorderCrossingController.resolve(state)` is invoked from `MarginScreen.submitPlayerAction` **beside `actGate.resolve(state)`** (the same committed-turn hook, before the save so the win persists). Stateless over persisted flags (`KEY_WON` one-shot + the `act >= 3` guard). No libGDX type in the controller.
- **Deferred (→ polish / post-Epic-5):** the explicit bounded-turn **countdown timer** for the crossing (the cordon already supplies tension; AD-12 forbids a lockout wall); the full **Act-3 spine** (choice / last-provisioning beats / closing trap); a dedicated **victory screen** (art/animation) beyond the log+overlay parity with game-over; **ending variants** beyond the Aldric branch (Bond/companion-roster-colored epilogues); the **Deep Cave Mouth** (AD-12: a Region-2 threshold, explicitly NOT the exit — leave untouched).

## Baseline (verify before adding)

- **`RunState`** — the enemy-placement loop (`placeFloorActors`, ~L188–215) computes `enemyCountFor(spine.eastness(cx), act, ny)` per room-center; the cordon corner is base-0 (channel a skips it). `enemyCountFor(eastness, act, ny)` + `baseEnemyCountFor` + `inCordon(eastness, ny)` (`SAFE_TIER_EASTNESS = 0.2`, `CORDON_NY = 0.8`) are package-private, unit-tested. `getAct()`, `getPlayer()`, `appendMessages(List)`, `generateFloor()` (reads act once at run start), `getActiveCompanionId()`. **Channel a must remain unchanged** — 5.7 adds channel b alongside it, never inside `enemyCountFor`.
- **`WorldSpine`** — `borderX()`/`borderY()` (`BORDER_X = 0.05`, `BORDER_Y = 0.9` — far-west + far-north), `tileX/tileY`, `eastness`.
- **`FlagStore`** — `get/set`, `getAct/setAct`, `KEY_ALDRIC_CAPTURED`, `getLoss(CompanionId)`. Add `KEY_WON` here as a plain flag (unset → 0, AD-6). `CompanionId.ALDRIC`.
- **`MarginScreen`** — the `gameOver` end-state block in `update` (`if (!p.isAlive())`, ~L496–508): sets `gameOver`, seeds `deathCauseLine` + `GAME_OVER_LINE`, `SaveService.deleteSave()`, `[R]` → `restart()`. The **win block parallels this** (check `KEY_WON` before/with the death check). `submitPlayerAction` is the controller wire-site (beside `actGate.resolve`). `restart()` (~L905–922) resets `gameOver`/`deathCauseLine` — reset the win flag there too.
- **`ActGateController`** (Story 5.6) — its Act 2→3 rescue flip is 5.7's precondition; its `default -> { /* the border win is 5.7 */ }` comment marks the seam.
- **`OccupationEscalationTest`** — `theNwBorderCordonIsNeverThickened` (channel a leaves the cordon at 0) and `aHigherActRegeneratesADenserInterior` (interior aggregate grows with act) **must stay green**: keep channel-b population out of `enemyCountFor`, and confirm the interior channel-a growth still dominates the aggregate once a few thinning cordon foes are added.
- **Tests to keep green** — `OccupationEscalationTest`, `RunStatePersistenceTest` (`KEY_WON` round-trips as a plain flag), `ActGateControllerTest`, `CaptureControllerTest`, `CompanionLossTest`. Suite is at **511** (as of 5.6).

## Tasks / Subtasks

- [x] **Task 1 — Channel-b cordon population (AC-1, D1).**
  - [x] 1.1 `RunState.cordonCountFor(int act)` (package-private, pure, rng-free): `max(0, 4 - max(1, act))` → Act 1 → 3, Act 2 → 2, Act 3 → 1, Act ≥ 4 → 0; a 0/never-set act clamps to Act 1. Documented as the dual of channel a (AD-11 channel b).
  - [x] 1.2 New `RunState.placeCordon(spine, avoidX, avoidY)` scatters `cordonCountFor(getAct())` enemies in a 5×5 near the border landmark inside the `inCordon` box (seeded draws only), NOT via `enemyCountFor`. **Determinism (AD-5):** placed as the **LAST** rng consumer in `generateFloor` (after `placeFloorActors` AND `placeStructureLoot`) so no pre-5.7 seed's enemy/loot layout shifts — cordon foes append only. (Initial placement between the interior loop and supply scatter perturbed seed 42's loot; moving it last fixed `StructureContentTest`.)
- [x] **Task 2 — The border-crossing win (AC-1/AC-2, D2/D5).**
  - [x] 2.1 `FlagStore.KEY_WON` (plain flag, unset → 0, AD-6); read/written via `get/set(KEY_WON)` (the act idiom).
  - [x] 2.2 New `narrative/BorderCrossingController.resolve(RunState)`: while `getAct() >= 3` and `KEY_WON` unset, when the player is within `REACH_BAND` (2) of `borderX()/borderY()` → `set(KEY_WON, 1)` + append the epilogue beat (`LINE_HOME`, the Aldric-fate branch, `LINE_CORNEO`, `LINE_GRAVEYARD`). One-shot via `KEY_WON`; gated on Act 3 (via 5.6's rescue).
- [x] **Task 3 — Victory end-state in the presentation layer (AC-2, D3/D6).**
  - [x] 3.1 Wired `borderCrossing.resolve(state)` into `MarginScreen.submitPlayerAction` beside `actGate.resolve` (before the save; the save is now also gated on `KEY_WON == 0` so a won run isn't persisted). Added the field + import + `startNewJourney` re-init.
  - [x] 3.2 `MarginScreen.update` victory end-state parallel to `gameOver` (checked **before** the death branch): latch `won`, seed `WIN_LINE` once, `SaveService.deleteSave()`, `[R]` → `restart()`. `won` reset in both `startNewJourney` and `restart` (`RunState.restart` rebuilds the FlagStore → `KEY_WON`/act reset). Added `renderVictoryPanel()` (gold `EVENT_TIME` accent, text-only) at the game-over render site.
- [x] **Task 4 — Tests + verification (all ACs).**
  - [x] 4.1 AC-1 channel b (`OccupationEscalationTest`): `theCordonThinsAsTheActAdvances` (3→2→1→0, clamp); `aLaterActRegeneratesAThinnerCordon` (Act-3 cordon-box aggregate < Act 1 — the dual of the interior test); `channelBIsSeparateFromChannelA` (`enemyCountFor(0.05, act, 0.9) == 0` every act — AD-11 do-not-merge).
  - [x] 4.2/4.3 (`BorderCrossingControllerTest`, 6 tests): no win before Act 3; not-at-border no win; reaching the border in Act 3 wins + lands the canon epilogue; one-shot; the Aldric-with vs alone epilogue branch (both win).
  - [x] 4.4 Regression: full suite green (511 → **521**, +10 incl. the review-fix test). **Verified:** `mvn -o -pl core test` → BUILD SUCCESS; `mvn -o compile` (all modules incl. desktop) → exit 0.

### Review Findings

Inline adversarial review (Blind / Edge-Case / Acceptance), 2026-08-14. **1 Med fixed inline, no High; 2 Low deferred, 1 dismissed.** Suite green (521).

- [x] [Review][Fixed][Med] Dying at the threshold counted as a win [BorderCrossingController.java] — `resolve` ran after the turn's enemy phase without an `isAlive()` check, so a cordon kill on the border tile still set `KEY_WON` (the victory end-state takes precedence over death). Violated AC-2's "Given I **survive** the crossing." Guarded `resolve` on `player.isAlive()`; pinned by `dyingAtTheThresholdIsDeathNotAWin`.
- [x] [Review][Defer] Live cordon thinning shares channel-a's deferred live-regen seam [RunState.java] — `generateFloor` reads the act once at run start, so a single live run's cordon reflects Act 1, not the current act; thinning is proven per-`(seed, act)` via regen tests. Same deferred seam as 4.3's live reinforce. Logged to deferred-work.
- [x] [Review][Defer] Trivial crossing in seeds where the NW border corner is sparse/unwalkable (cordon places 0) [RunState.java] — a balance/tuning item; the win is still Act-3-gated. Logged to deferred-work.
- Dismissed (noise): the explicit bounded-turn countdown timer is deferred by design (AD-12 forbids a lockout wall; the cordon is the tension).

## Dev Notes

- **AD-12.** The win is reaching the NW border tile and surviving; the border is **always walkable** (permadeath honesty — never an invisible wall). The Deep Cave Mouth is a separate Region-2 threshold, NOT the exit — do not touch it.
- **AD-11 (two-channel escalation — do NOT merge).** Channel a (interior thickens per act, `enemyCountFor`) and channel b (cordon thins per act, new `cordonCountFor`) are separate functions applied at different regions. Merging them (e.g., one multiplier over all Gilimans) is wrong — it would *harden* the win gate each act instead of thinning it. Keep `enemyCountFor(cordon, act) == 0`.
- **AD-5.** Both count functions are rng-free pure decisions of `(act[, eastness, ny])`; only per-enemy position draws touch the seeded stream. Deterministic per `(seed, act)`.
- **AD-6.** `KEY_WON` is a plain flag (unset → 0); it round-trips with the store — no new typed `RunState` field, no migration/default trap.
- **AD-1/AD-2.** All logic + the epilogue text live in headless core (`narrative/BorderCrossingController`, `RunState`, `FlagStore`); `MarginScreen` only reads `KEY_WON` and renders the victory end-state. No core class references a libGDX type.
- **AD-4/AD-14.** The crossing resolves on committed acted turns at the screen hook (the `CaptureController`/`ActGateController` precedent), not a new `TurnEngine` pipeline step; the epilogue is a text surface (no survival tick while it shows).
- **SM-1.** The ending is *lived through the systems* — Klein WALKS to the border and survives the thinned cordon; the epilogue branches on the systemic state (Aldric's fate). No cutscene boss.
- **Reuse (CLAUDE.md §3).** Parallel the `gameOver` end-state rather than inventing a new screen mode; reuse the `inCordon` box + `WorldSpine` border landmark (no new geography); reuse the one-shot-controller idiom and the `LINE_*` message pattern.
- **Simplicity (CLAUDE.md §2).** The win is a reach-trigger + a flag + an end-state parallel to death — no countdown engine, no Act-3 provisioning system, no dialogue. Content/polish consumes these seams later.
- **Build/verify:** `docs/BUILD.md` — `mvn -o clean install`.

### Project Structure Notes

- New file: `core/src/main/java/com/margins/rogue/narrative/BorderCrossingController.java`.
- Edits: `core/src/main/java/com/margins/rogue/state/RunState.java` (`cordonCountFor` + placement), `core/src/main/java/com/margins/rogue/state/FlagStore.java` (`KEY_WON`), `core/src/main/java/com/margins/MarginScreen.java` (wire + victory end-state).
- New test: `core/src/test/java/com/margins/rogue/narrative/BorderCrossingControllerTest.java` (+ channel-b assertions may extend `OccupationEscalationTest` in the `state` package, or a new `CordonEscalationTest`).

### References

- [Source: _bmad-output/planning-artifacts/epics.md#Story 5.7: The border-crossing win and epilogue] — AC-1/AC-2, FR-18, AD-12, AD-11 channel b.
- [Source: _bmad-output/implementation-artifacts/epic-5-context.md#Technical Decisions] — AD-12 the border win; AD-11 two-channel escalation (do not merge).
- [Source: _bmad-output/implementation-artifacts/5-6-the-act-gating-quests.md] — the Act 2→3 rescue gate (5.7's precondition) and the one-shot-controller pattern.

## Dev Agent Record

### Agent Model Used

Claude Opus 4.8 (1M context) — create-story 2026-08-14 (autonomous loop).

### Debug Log References

- `mvn -o -pl core test` — BUILD SUCCESS, full suite green (**520** tests, +9 over the 511 baseline).
- First run: 1 red in `StructureContentTest.seedFortyTwoFloorLayoutIsByteIdentical` (an AD-5 determinism snapshot of seed 42's loot). Root cause: the cordon's seeded position draws, placed mid-`placeFloorActors`, shifted the supply/structure-loot rng stream. Fix: extracted `placeCordon` and made it the **last** rng consumer in `generateFloor` (after `placeStructureLoot`) — every pre-5.7 seed layout is byte-identical again; cordon foes append only. Green thereafter.
- `mvn -o compile` (all modules incl. desktop) → exit 0.

### Completion Notes List

- **AC-1 channel b (D1):** `RunState.cordonCountFor(act) = max(0, 4 - max(1, act))` thins the NW cordon 3→2→1→0 as acts advance — the dual of channel a. `placeCordon` scatters that many foes near the border landmark (inside the `inCordon` box), kept OUT of `enemyCountFor` (AD-11 do-not-merge → `enemyCountFor(cordon) == 0` still holds) and run **last** in `generateFloor` for AD-5 determinism preservation. Pinned by `theCordonThinsAsTheActAdvances`, `aLaterActRegeneratesAThinnerCordon`, `channelBIsSeparateFromChannelA`.
- **AC-1/AC-2 the win (D2/D5):** `narrative/BorderCrossingController.resolve` wins the run when Klein reaches within `REACH_BAND` of the NW border in Act 3 (always-walkable, no timer/wall — AD-12): `set(KEY_WON, 1)` + a 4-line epilogue (`LINE_HOME`; the Aldric-fate branch — `LINE_ALDRIC_WITH` if he's the active companion, else `LINE_ALDRIC_ALONE`; `LINE_CORNEO` = Corneo→Coneros; `LINE_GRAVEYARD`). Stateless one-shot on `KEY_WON` + the `act >= 3` gate. Pinned by 6 `BorderCrossingControllerTest` cases.
- **AC-2 victory end-state (D3/D6):** `MarginScreen` resolves the crossing beside `actGate` each committed turn (and no longer saves a won run — `KEY_WON == 0` gate); `update` shows the victory end-state before the death branch (latch `won`, seed `WIN_LINE`, `deleteSave`, `[R]` restart), with a gold `renderVictoryPanel`. `RunState.restart` rebuilds the FlagStore, so `[R]` fully resets `KEY_WON`/act/the cordon; `won` reset in both reset paths.
- **SM-1:** the ending is lived through the systems — Klein WALKS to the border, survives the (thinned) cordon, and the epilogue branches on the systemic state (Aldric's fate). No boss, no cutscene.
- **Deferred (unchanged from the spec, + one noted):** the explicit bounded-turn countdown timer; the full Act-3 provisioning spine; a richer victory screen; ending variants beyond the Aldric branch; the Deep Cave Mouth threshold. **Also shared-deferred:** like channel a, `generateFloor` reads the act once at run start, so in a single live run the cordon reflects the generation-time act (Act 1) rather than thinning live as the act flips — the thinning is proven per-`(seed, act)` via regen tests; wiring the **live** act-flip regen/reinforce is the same deferred seam channel a documents (Story 4.3 D4). The win itself is fully functional end-to-end.

### File List

- `core/src/main/java/com/margins/rogue/narrative/BorderCrossingController.java` (new)
- `core/src/main/java/com/margins/rogue/state/FlagStore.java` (`KEY_WON`)
- `core/src/main/java/com/margins/rogue/state/RunState.java` (`cordonCountFor` + `placeCordon`, wired last in `generateFloor`)
- `core/src/main/java/com/margins/MarginScreen.java` (import + `borderCrossing`/`won`/`WIN_LINE` + wire + victory end-state + `renderVictoryPanel` + reset paths)
- `core/src/test/java/com/margins/rogue/narrative/BorderCrossingControllerTest.java` (new)
- `core/src/test/java/com/margins/rogue/state/OccupationEscalationTest.java` (+3 channel-b tests + `WorldSpine` import)

## Change Log

- 2026-08-14 — dev-story (autonomous loop): channel-b `cordonCountFor` + `placeCordon` (run last in `generateFloor` for AD-5 determinism), `FlagStore.KEY_WON`, `narrative/BorderCrossingController` (win on reaching the NW border in Act 3 + epilogue with the Aldric-fate branch), and the `MarginScreen` victory end-state (wire + `won` latch + `renderVictoryPanel` + won-run save gate). +9 tests (6 `BorderCrossingControllerTest`, 3 `OccupationEscalationTest` channel-b). One red→green: cordon draws moved to the last rng consumer so seed 42's loot stays byte-identical (`StructureContentTest`). Suite 511 → 520; all modules compile. Status → review.
- 2026-08-14 — created by create-story (autonomous loop). Decisions: D1 channel-b `cordonCountFor` thins the NW cordon per act (dual of channel a; kept OUT of `enemyCountFor` so channel a stays 0 — AD-11 do-not-merge); D2 the crossing = the win via a headless one-shot `BorderCrossingController` (tension = the thinned cordon approach; always-walkable, no timer lockout — AD-12); D3 a victory end-state paralleling `gameOver` (`KEY_WON` + epilogue + deleteSave); D4 "last provisioning done" collapses to "Act 3 reached" (the Act-3 spine stays deferred from 5.6); D5 epilogue seeds validate SM-1 with a minimal Aldric-fate branch; D6 wire beside `actGate.resolve`, stateless over `KEY_WON` (AD-1/AD-2/AD-4). Status → ready-for-dev.

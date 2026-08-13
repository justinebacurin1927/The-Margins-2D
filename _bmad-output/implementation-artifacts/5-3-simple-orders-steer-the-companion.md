---
baseline_commit: 3439b5c
---

# Story 5.3: Simple orders steer the companion

Status: done

## Story

As Klein,
I want to tell my companion to hide, hold, or distract,
so that I can use it tactically (FR-16).

## Acceptance Criteria

**AC-1 — Orders switch the behavior state machine.**
**Given** an active companion **When** I issue hide / hold / distract **Then** the state machine switches to that behavior — HOLD/HIDE become standing orders the autonomous machine honors (Story 5.2), and distraction emits a `NoiseEvent` to pull patrols (reusing the existing distraction action).

**AC-2 — Reachable: the orders are bound to keys and in the legend.**
**Given** the order actions **When** I play **Then** they are bound to keys and listed in the how-to-play legend (retro #1) — including **distract, which currently has a handler but no keybind** (an unreachable-action gap this story closes).

## Scope decisions (author, 2026-08-13 — running the loop autonomously per Justine)

- **D1 — One "command" cycle order + the distract shout, the only two free keys.** Exactly two single-letter keys are unbound (**I**, **O**). `O` = **command**: cycles the companion's standing order **autonomous → HOLD → HIDE → autonomous** (covers "hold", "hide", and the release back to the 5.2 autonomous machine). `I` = **shout**: the existing `CompanionSystem.distract` (finally reachable). Three orders (hold/hide/distract) on two keys via the cycle.
- **D2 — An order is a turn-committing `PlayerAction` (AD-1/AD-5).** The screen emits `PlayerAction`s only and never mutates model AI state (AD-1), so an order routes through `TurnEngine`; issuing it commits the turn (commanding mid-crisis costs a beat) and the companion obeys the same turn. Refused with no turn when there's no companion ("No companion to command." — a new line; the existing distract refusal "No companion to call on." is untouched, it's test-pinned).
- **D3 — The cycle reuses the 5.2 behavior field (no new order field).** HOLD/HIDE are already the honored standing states (`isStandingOrder`); setting `behavior = FOLLOW` is "resume autonomous" (FOLLOW is not a standing order, so `act` recomputes the real state next turn). The cycle normalizes any autonomous state (FOLLOW/FIGHT_RETREAT/TAKE_COVER/FLEE) to HOLD on the first press. A deliberate order clears a lingering PANICKED (a command calms).
- **D4 — Distract stays byte-identical (AC "reusing the existing action").** 5.3 only *binds* it — no change to `CompanionSystem.distract` (its budget, noise, and messages, including the stale "Galleon" name which is left as-is per CLAUDE.md §3; noted, not fixed here).
- **Deferred (→ 5.4+):** an order/threat priority model (should HOLD break under a lethal threat?); per-order UI affordances/icons; distinct hold-vs-hide tactical noise/detection nuances beyond 5.2's quiet-hide; the "Galleon"→companion-name message cleanup.

## Baseline (verify before adding)

- **`CompanionSystem.act`** (Story 5.2) honors a standing HOLD/HIDE (`isStandingOrder`) and otherwise runs the autonomous `decideBehavior`. HOLD/HIDE = stay put; HIDE quiet.
- **`CompanionSystem.distract`** already exists and is wired in `TurnEngine` (`Kind.DISTRACT` → shout + `NoiseEvent`), refusing without a companion / with no shouts left. **It is bound to no key** — `PlayerAction.distract` is referenced only by its factory and the `TurnEngine` handler (unreachable via input).
- **`PlayerAction`**: `Kind` enum + factories; `TurnEngine.advance` switches on `Kind` (add an `ORDER` case; a refusal path leaves `acted=false` → no turn, the inert-USE precedent).
- **`MarginScreen.readAction`** binds the turn-action keys; **I** and **O** are the only free single-letter keys. `renderHowToPlayPage` draws the EXPLORE/SURVIVE legend (EXPLORE column is full to the divider; SURVIVE has room for one more row).
- **Tests**: `CaptureControllerTest` pins the distract refusal line `"No companion to call on."` (must stay). `CompanionBehaviorTest`/`CompanionAiTest` pin the 5.2 machine (HOLD/HIDE honored) — the order cycle drives those same states.

## Tasks / Subtasks

- [x] **Task 1 — `ORDER` action (AC-1, D1/D2).**
  - [x] 1.1 `PlayerAction.Kind.ORDER` + `PlayerAction.order(int dir)`.
  - [x] 1.2 `CompanionSystem.order(RunState, List) → boolean`: cycle the active companion's behavior (autonomous→HOLD→HIDE→FOLLOW), clear PANICKED, emit an observation line ("<name> holds position." / "hides." / "follows your lead."); refuse ("No companion to command.", no turn) without a companion.
  - [x] 1.3 `TurnEngine` `case ORDER` → `acted = CompanionSystem.order(...)`.
- [x] **Task 2 — Reachability (AC-2, retro #1).**
  - [x] 2.1 `readAction`: **O** → `PlayerAction.order`, **I** → `PlayerAction.distract` (bind the previously-unreachable shout).
  - [x] 2.2 Legend: a combined companion-command row ("O Command   I Shout") in `renderHowToPlayPage`; panel height 300→314 for the row.
- [x] **Task 3 — Tests + verification (all ACs).**
  - [x] 3.1 AC-1: through `TurnEngine`, ORDER cycles behavior HOLD → HIDE → FOLLOW(resume); a HOLD order keeps the companion still and non-fighting even with a threat present; ORDER commits a turn; a HIDE order clears a prior PANICKED.
  - [x] 3.2 AC-1: distract remains reachable via `TurnEngine` (shout emits the pull-patrol `NoiseEvent` that raises an UNAWARE bystander to SUSPICIOUS), unchanged.
  - [x] 3.3 AC-2/refusal: ORDER without a companion refuses ("No companion to command.") and spends no turn.
  - [x] 3.4 Full suite green via `docs/BUILD.md` (484 → 490, +6). **Verified:** all green.

## Dev Notes

- **AD-1.** The order is a `PlayerAction` — the screen never writes companion AI state directly; `CompanionSystem` owns the transition.
- **AD-5.** ORDER commits a turn (a real action); the refusal path spends none (inert-USE precedent).
- **AD-9/AD-10.** Distract keeps the single noise channel; only the active companion is commandable (the abstract three have no body).
- **Surgical (CLAUDE.md §3).** No change to `distract`'s internals or the 5.2 machine — 5.3 adds the ORDER action + two keybinds + one legend row and reuses the honored HOLD/HIDE states.
- Build/verify: `docs/BUILD.md` — `mvn -o clean install`.

## Dev Agent Record

### Agent Model Used
Claude Opus 4.8 (1M context) — create-story 2026-08-13 (autonomous loop).

### Debug Log References
- `mvn -o test` — BUILD SUCCESS, full suite green (490 tests, +6 over the 484 baseline).

### Completion Notes List
- **AC-1 orders (D1/D3):** `PlayerAction.Kind.ORDER` + `CompanionSystem.order` cycles the active companion's standing order — autonomous → HOLD → HIDE → autonomous (resume). HOLD/HIDE are the 5.2 honored states, so a held companion stays put and does not fight even with a threat present; resuming sets FOLLOW (not a standing order → `act` recomputes). A command clears a lingering PANICKED. Pinned by `orderCyclesHoldThenHideThenResume`, `aHeldCompanionStaysPutAndDoesNotFightAThreat`, `orderingCalmsAPriorPanic`.
- **AC-1 distract:** unchanged (`CompanionSystem.distract` byte-identical) — it still emits the pull-patrol `NoiseEvent`, now reachable. Pinned by `distractIsReachableAndPullsPatrols` (an UNAWARE bystander rises to SUSPICIOUS through the shared `NoiseSystem`).
- **AC-2 reachable (D1):** the only two free single-letter keys — **O** → order (the command cycle), **I** → distract (closing the unreachable-shout gap: `PlayerAction.distract` previously had a `TurnEngine` handler but no keybind). Legend: a combined "O Command   I Shout" row in `renderHowToPlayPage` (panel height 300→314 to fit it above the divider).
- **AD-1/AD-5 (D2):** the order is a `PlayerAction` routed through `TurnEngine` (the screen never mutates AI state) and commits a turn on success; a refused order (no companion, "No companion to command.") spends none. Pinned by `orderCommitsATurn`, `orderIsRefusedWithoutACompanionAndSpendsNoTurn`.
- **Review:** inline review (Blind/Edge/Acceptance; multi-agent reserved for token budget). No High/Med. Low notes: a HOLD order persists even under a lethal threat (an order/threat override is deferred to 5.4); the stale "Galleon" name in `distract`'s success messages is left untouched (surgical — CLAUDE.md §3, noted not fixed); the legend uses one combined row (only two free keys / limited panel space).
- **Surgical:** no change to `distract` internals or the 5.2 machine — 5.3 adds the ORDER action, two keybinds, and one legend row, reusing the honored HOLD/HIDE states.

### File List
- `core/src/main/java/com/margins/rogue/system/PlayerAction.java`
- `core/src/main/java/com/margins/rogue/system/CompanionSystem.java`
- `core/src/main/java/com/margins/rogue/system/TurnEngine.java`
- `core/src/main/java/com/margins/MarginScreen.java`
- `core/src/test/java/com/margins/rogue/CompanionOrdersTest.java` (new)

## Change Log

- 2026-08-13 — created by create-story (autonomous loop). Decisions: D1 one `O` command-cycle (autonomous→HOLD→HIDE→autonomous) + `I` shout, the only two free keys, covering hold/hide/distract; D2 an order is a turn-committing `PlayerAction` through `TurnEngine` (AD-1/AD-5), refusing without a companion; D3 the cycle reuses the 5.2 behavior field (FOLLOW = resume autonomous), clearing PANICKED on command; D4 distract stays byte-identical, 5.3 only binds it (closing the unreachable-action gap). Order/threat priority + name-cleanup deferred. Status → ready-for-dev.
- 2026-08-13 — dev-story + inline review (autonomous loop): `PlayerAction.ORDER` + `CompanionSystem.order` (cycle autonomous→HOLD→HIDE→resume, clears PANICKED, refuses without a companion) wired in `TurnEngine`; **O**→order and **I**→distract bound in `readAction` (distract finally reachable) + a combined legend row. +6 tests (`CompanionOrdersTest`); full suite green (490). Inline review, no High/Med (Lows: HOLD-under-threat override → 5.4, stale "Galleon" name left surgical, combined legend row). Status → done.

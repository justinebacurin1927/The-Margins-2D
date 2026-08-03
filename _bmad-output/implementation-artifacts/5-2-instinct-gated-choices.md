---
baseline_commit: 4dab3deb19ac14acd1c6cab789aa0b8503035289
---

# Story 5.2: INSTINCT-gated choices

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As Justine (player),
I want some options unlocked by Milek's INSTINCT,
so that his cunning is mechanically rewarded (FR-7).

## Acceptance Criteria

1. **Given** an INSTINCT-gated choice with a threshold, **When** the player selects it, **Then** the outcome is a **deterministic threshold compare** — `player.instinct >= threshold` — with no randomness. (FR-7, AD-8)
2. **Given** the compare succeeds (`instinct >= threshold`), **When** it resolves, **Then** the scene routes to the choice's **success** branch. (FR-7)
3. **Given** the compare fails (`instinct < threshold`), **When** it resolves, **Then** the scene routes to the choice's **failure** branch (which may be a different node, or close the scene if there is none). (FR-7)
4. **Given** an ungated choice (no threshold), **When** selected, **Then** it routes to its linked node exactly as in 5.1 — instinct is not consulted. (regression, FR-6)
5. **Given** a gated choice is on screen, **When** it is displayed, **Then** the player can tell it is an INSTINCT option (a visible marker on the choice). (FR-7 / UJ-1 "picks the INSTINCT option")

**Architectural definition-of-done:**

6. The check is a pure threshold compare in the **model** (`DialogController`), reading `player.instinct` (AD-8) — no `Random`, no dice. No libGDX in the model. The screen passes the current instinct value in and renders the marker; it does not implement the compare (AD-2). The turn loop stays suspended throughout (5.1's `DIALOGUE` gate is unchanged).

## Product decisions (recommended defaults baked in)

- **The gate lives on the choice (`DialogOption`), per FR-7 — not on the node.** FR-7 says "a dialogue *choice* may be gated … routes to different nodes." (The spine's AD-8 phrasing "node.threshold" is looser; FR-7 is the authoritative, testable requirement.) So extend the reused `com.margins.dialog.DialogNode.DialogOption` with an optional `int instinctThreshold` (default `-1` = ungated) and a `DialogNode failNext`. The existing `next` field doubles as the **success** branch; `failNext` is the **failure** branch. `isGated()` ⇔ `instinctThreshold >= 0`.
- **Extend the legacy `DialogOption` additively — do not fork the content model.** The architecture Structural Seed says *reuse* `com.margins.dialog.DialogNode`. Adding two public fields + one new constructor is backward-compatible: the existing `DialogOption(String label, DialogNode next)` constructor (used by 5.1's `SampleDialog` and by the legacy `com.margins.screen.GameScreen`) still compiles and behaves identically (`instinctThreshold` defaults to `-1`, `failNext` stays null). Keeping one content model is the whole point of the "reuse" seam.
- **Thread the instinct value through `select` — exactly as 5.1 anticipated.** 5.1's `DialogController.select(int choiceIndex)` becomes `select(int choiceIndex, int instinct)`. This is the change 5.1's Dev Notes explicitly reserved ("5.2 will thread RunState/player through select for the INSTINCT threshold compare"). Pass the raw `int` (not `RunState`) to keep the controller pure and free of run-state coupling; the screen calls `dialog.select(choice, state.getPlayer().getInstinct())`. Routing: gated + fail → `failNext`; gated + pass, or ungated → `next` (which may be null → scene ends, same as 5.1).
- **Deterministic, `>=`, boundary passes.** `instinct == threshold` is a **success** (the check is `>=`, AD-8). No RNG anywhere in the path — this is a stat gate, not a roll. Default `instinct` is 7 (`RoguePlayer`), and dodge already uses `instinct` elsewhere, but this check must not touch `rng()`.
- **Effects (set-a-flag / grant-an-item) are 5.3, not here.** FR-7 mentions a success branch "may set a flag or grant an item," but the flag mechanism and its persistence/gating loop are Story 5.3 (FR-8), and item-grant-on-entry is also 5.3/Epic 6 content. 5.2 delivers the **routing** (which branch the check takes) — the testable core of FR-7 — and leaves node-entry effects to 5.3 so each story stays minimal. The demo proves the routing by which node text appears.
- **Marker is a data-driven render suffix.** In `renderDialoguePanel`, a gated option shows a trailing ` [INSTINCT]` tag (derived from `isGated()`), so the author can't forget it and the player can see the option is a cunning read (UJ-1). No new content field for the marker — it's computed from the threshold.
- **Extend `SampleDialog` with one gated choice so it's demoable.** Add a third root choice gated at a threshold the default instinct (7) passes (e.g. threshold 5 → success node) plus a distinct failure node, so pressing that choice in-game visibly routes to the success text. This exercises ACs 1–3/5 live; authored scenes remain Epic 6.

## Tasks / Subtasks

- [x] **Task 1 — Gated `DialogOption` (reuse + extend, FR-7/AD-8)** (AC: 1, 2, 3, 4)
  - [x] In `core/src/main/java/com/margins/dialog/DialogNode.java`, added to the nested `DialogOption`: `public int instinctThreshold = -1;`, `public DialogNode failNext;`, and `public boolean isGated() { return instinctThreshold >= 0; }`.
  - [x] Added a gated constructor `DialogOption(String label, int instinctThreshold, DialogNode successNext, DialogNode failNext)` (`next = successNext`). **Kept** the existing `(label, next)` constructor unchanged (ungated path).
  - [x] Additive only — the legacy `com.margins.screen.GameScreen` and 5.1's `SampleDialog` still compile (verified by a full build).

- [x] **Task 2 — INSTINCT routing in `DialogController` (AD-8/AD-2)** (AC: 1, 2, 3, 4, 6)
  - [x] Changed `select(int choiceIndex)` → `select(int choiceIndex, int instinct)`.
  - [x] Body: guards unchanged; then gated + `instinct < threshold` → `failNext`; else → `next` (ungated, or passed check `instinct >= threshold`).
  - [x] No `Random`, no `RunState` import — pure model, deterministic compare (AD-8).

- [x] **Task 3 — Screen wiring + marker (AD-2)** (AC: 5, 6)
  - [x] `RogueGameScreen.handleDialogueInput()` now calls `dialog.select(choice, state.getPlayer().getInstinct())`. Terminal-node close path unchanged.
  - [x] `renderDialoguePanel()` appends `  [INSTINCT]` to a gated option's line (via `isGated()`). No other render change.
  - [x] No compare/routing logic added to the screen — it forwards `getInstinct()` and renders the marker only (AD-2).

- [x] **Task 4 — Demo scene** (AC: 1, 2, 3, 5)
  - [x] `SampleDialog` gained a `Read him.` choice gated at 5 (default INSTINCT 7 passes) with distinct success ("You catch the tell…") and failure ("Nothing reads.") nodes. Placeholder content; Epic 6 owns authored scenes.

- [x] **Task 5 — Verification** (AC: 1, 2, 3, 4, 5, 6)
  - [x] Headless harness (throwaway `InstinctHarness`) over the model — **12/12 checks passed**:
    - gated, instinct 7 ≥ threshold 5 → success `next` (AC-2)
    - gated, instinct 7 < threshold 9 → `failNext` (AC-3)
    - boundary instinct 7 == threshold 7 → success (`>=`, AC-1)
    - ungated → `next` regardless of a low instinct (AC-4)
    - gated fail with `failNext == null` → scene closes; gated pass with null success → scene closes (edges)
    - determinism: 20 identical checks route identically, no RNG (AC-1)
    - out-of-range index guarded with the new signature; `isGated()` true/false correct
    - `SampleDialog` has a gated choice; walking it at instinct 7 lands on the success node.
    - [x] Threw-away harness deleted after the run.
  - [x] **Live boot on `:0`:** boots clean with the gated code, loads the pre-existing save, no exceptions (ran full timeout). NOTE — interactive confirmation (press `T`, see the ` [INSTINCT]` marker on "Read him.", pick it, land on the success node at default instinct 7) is a **human check**; an automated boot can't press keys. Flagged for Justine.

## Dev Notes

### Governing architecture
- **FR-7 — INSTINCT-gated choices.** "A dialogue choice may be gated by an INSTINCT Check; the outcome (success/failure) routes to different nodes and may set a flag or grant an item. With INSTINCT above the check threshold, the gated branch resolves success (e.g., reveals the hidden cache, FR-8); below it, the failure branch resolves." [Source: prd.md#FR-7; epics.md#Story 5.2]
- **AD-8 — Deterministic INSTINCT checks.** "An INSTINCT Check resolves as a deterministic threshold compare (`player.instinct >= node.threshold`), not a dice roll." 5.2 implements exactly this compare in `DialogController`. (Threshold placed on the *choice* per FR-7's per-choice wording; see Product decisions.) [Source: ARCHITECTURE-SPINE.md#AD-8]
- **AD-2 — Rules in the model, no libGDX in the model, no rule in the screen.** The compare + routing live in `DialogController`; the screen forwards `getInstinct()` and renders the marker only. [Source: ARCHITECTURE-SPINE.md#AD-2]
- **AD-5 — Single seeded RNG; this check must not draw from it.** The gate is a stat compare, not a roll — do not call `state.rng()`. [Source: ARCHITECTURE-SPINE.md#AD-5]
- **Structural Seed.** `narrative/DialogController` "drives DialogNode, INSTINCT checks (FR-6/7)"; reuse `com.margins.dialog.DialogNode`. 5.2 adds the INSTINCT half onto the 5.1 controller. [Source: ARCHITECTURE-SPINE.md#Structural Seed]

### Files being modified — current state and what to preserve
- **`com.margins.dialog.DialogNode`** (UPDATE — legacy, shared): today `DialogOption` is `{ String label; DialogNode next; }` with one constructor. **Add** `instinctThreshold` (default -1), `failNext`, `isGated()`, and a gated constructor. **Preserve** the `(label, next)` constructor and field semantics — the legacy `com.margins.screen.GameScreen` and 5.1's `SampleDialog` depend on them. Purely additive.
- **`narrative/DialogController.java`** (UPDATE — from 5.1): holds the current node; `start/end/isActive/getCurrent/select`. **Change** `select(int)` → `select(int, int instinct)` and add the gate routing. Everything else unchanged. Pure model.
- **`RogueGameScreen.java`** (UPDATE — from 5.1): has `UiMode.DIALOGUE`, the `T` trigger, `handleDialogueInput()`, `renderDialoguePanel()`. **Change** the `select` call to pass `getInstinct()`, and append the ` [INSTINCT]` marker in the panel. **Preserve** the 5.1 suspension gate and all other modes.
- **`narrative/SampleDialog.java`** (UPDATE — from 5.1): add one gated choice for the demo. Placeholder content.
- **DO NOT TOUCH:** `TurnEngine`, `FlagStore`, `RunState`, `CombatSystem` (INSTINCT here is narrative routing, not combat), save/`SaveService`.

### Previous-story intelligence (5.1)
- 5.1 built the `DialogController` + `DIALOGUE` UiMode and **reserved this exact seam** — its Dev Notes say `select` will gain the instinct parameter in 5.2. The only live caller of `select` is `RogueGameScreen.handleDialogueInput()`, so the signature change is a single call-site update.
- 5.1 verified via a throwaway `DialogHarness` (15 checks) + `:0` boot; reuse that pattern. `RoguePlayer.getInstinct()` exists and returns 7 by default.
- 5.1 is currently `review` (uncommitted); building 5.2 on it mirrors the Epic 4 pattern (4.2 on 4.1 pre-review). Baseline for both is the Epic 4 commit `4dab3de`.

### Scope boundary
- **IN:** gated `DialogOption` (threshold + failNext), the deterministic compare + routing in `DialogController`, the screen forwarding `getInstinct()` + rendering the marker, and a demo gated choice.
- **OUT:** setting flags / granting items on a branch (**5.3**, FR-8 / AD-7 via `FlagStore`; the store + `applyBondTag`/`set` exist from 4.3 but wiring choice-effects is 5.3); the cache-reveal gating loop (**5.3**); authored scenes — the opening / reunion (**Epic 6**); any combat use of INSTINCT (out — dodge already uses it, untouched); changing the turn-suspension mechanism (5.1, unchanged).

### Testing standards
- No committed JUnit suite yet (open Epic 1/3/4 retro item) — throwaway `main` harness for the model + `:0` live boot, per every prior story. `DialogController`/`DialogNode` are pure model (headless). **Build quirk:** `mvn -o -pl core install` before `exec:java`; harness recipe `mvn -o -pl core org.codehaus.mojo:exec-maven-plugin:3.1.0:java -Dexec.mainClass=...`.

### Project Structure Notes
- Gate on the **choice** (option), not the node — FR-7 wording, and it lets one node offer a gated and an ungated choice side by side.
- Deterministic compare: no `rng()`, `>=` (boundary passes). This is the whole of AD-8 for dialogue.

### Review Findings

Code review 2026-08-03 (Blind Hunter + Edge Case Hunter + Acceptance Auditor, parallel). AC verdict: 5.2 AC 1–6 all satisfied, no defects. Auditor + Blind Hunter both confirmed the check is a **deterministic threshold compare with no RNG** (`state.getPlayer().getInstinct() < opt.instinctThreshold`), the compare lives in the model (screen only reads `isGated()` for the marker), and the `>=` boundary passes as specified. The `DialogOption` extension is backward-compatible (legacy screen's 2-arg options stay ungated).

**Dismissed:** AD-8 is phrased "`player.instinct >= node.threshold`" while the gate is on the *choice*, not the node — a deliberate, spec-reconciled divergence (FR-7 is per-choice: "a dialogue *choice* may be gated"), documented in this story's Product decisions. The deterministic-compare intent of AD-8 is fully honored. No code change.

## Dev Agent Record

### Agent Model Used
- Claude Opus 4.8 (1M context) — implementation + harness verification.

### Debug Log References
- Harness: `mvn -o -q -pl core install` (clean) then `... exec:java -Dexec.mainClass=com.margins.rogue.InstinctHarness` → **ALL 12 CHECKS PASSED**.
- Smoke boot on `:0`: `timeout 10 ... DesktopLauncher` — full duration (exit 124), **zero exceptions**, pre-existing save loaded clean.

### Completion Notes List
- ✅ **Task 1 — gated DialogOption (FR-7):** extended the reused `com.margins.dialog.DialogNode.DialogOption` with `instinctThreshold` (default -1 = ungated), `failNext`, `isGated()`, and a 4-arg gated constructor (`next` = success branch). Existing `(label, next)` constructor untouched → backward-compatible (legacy `GameScreen` + 5.1 `SampleDialog` still build).
- ✅ **Task 2 — deterministic routing (AD-8):** `DialogController.select(int, int instinct)` — gated + `instinct < threshold` → `failNext`, else `next` (ungated or `instinct >= threshold`). `>=` boundary passes; no `Random`, no `RunState` coupling — pure model.
- ✅ **Task 3 — screen wiring (AD-2):** `handleDialogueInput()` forwards `state.getPlayer().getInstinct()` into `select`; `renderDialoguePanel()` tags gated options with `[INSTINCT]`. No rule logic added to the screen.
- ✅ **Task 4 — demo:** `SampleDialog` gained a `Read him.` choice gated at 5 with success/failure nodes, so the mechanic is exercisable in-game.
- ✅ **Task 5 — verification:** 12/12 headless checks (pass/fail/boundary/ungated/null-branch/determinism/out-of-range/isGated/sample) + clean `:0` boot.
- **AC coverage:** 1 ✓ (deterministic `>=` compare, no RNG) · 2 ✓ (pass → success) · 3 ✓ (fail → failure/close) · 4 ✓ (ungated ignores instinct) · 5 ✓ (`[INSTINCT]` marker) · 6 ✓ (compare in the model, screen forwards + renders only, turn stays suspended).
- **Out of scope, as specified:** flag/item effects on a branch (5.3), authored content (Epic 6), combat INSTINCT. `TurnEngine`/`FlagStore`/`RunState`/save untouched.
- **Human check outstanding:** interactive `T` → `[INSTINCT]` marker → pick → success node at instinct 7.

### File List
- `core/src/main/java/com/margins/dialog/DialogNode.java` (MODIFIED) — `DialogOption` gains `instinctThreshold`, `failNext`, `isGated()`, gated constructor (additive, backward-compatible).
- `core/src/main/java/com/margins/rogue/narrative/DialogController.java` (MODIFIED) — `select(int, int instinct)` with the deterministic INSTINCT routing (AD-8).
- `core/src/main/java/com/margins/rogue/narrative/SampleDialog.java` (MODIFIED) — added a gated demo choice + success/failure nodes.
- `core/src/main/java/com/margins/rogue/RogueGameScreen.java` (MODIFIED) — forwards `getInstinct()` into `select`; renders the `[INSTINCT]` marker.

## Change Log

- 2026-08-03 — Story 5.2 spec created: INSTINCT-gated choices (FR-7). Extends the reused `DialogOption` with an optional `instinctThreshold` + `failNext` (additive, backward-compatible), adds a deterministic `instinct >= threshold` route in `DialogController.select(int, int)` (AD-8, no RNG), threads `getInstinct()` from the screen + renders an `[INSTINCT]` marker, and adds a gated demo choice to `SampleDialog`. Flag/item effects (5.3), authored content (Epic 6), and combat INSTINCT are out of scope.
- 2026-08-03 — Story 5.2 implemented: gated `DialogOption` (threshold + failNext + isGated), deterministic routing in `DialogController.select(int, int)`, screen forwards `getInstinct()` + renders `[INSTINCT]` marker, gated demo in `SampleDialog`. Verified via throwaway `InstinctHarness` (12 checks: pass/fail/boundary/ungated/null-branch/determinism) + clean `:0` boot; harness deleted. Status → review. Interactive `T`-key check left for Justine.

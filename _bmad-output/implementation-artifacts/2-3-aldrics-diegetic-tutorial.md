# Story 2.3: Aldric's diegetic tutorial

Status: review

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As a new player,
I want Aldric to teach me the opening controls in-world during the flight after the intro,
so that I learn to play without tooltips or UI chrome (FR-2).

## Acceptance Criteria

- **AC-1** — Given the opening flight after the intro, when Aldric speaks, then the how-to-play for the **six opening controls (move, scavenge, eat, craft, hide, rest)** is delivered as in-world dialogue in the message log (Aldric as speaker), not UI chrome (FR-2, AD-15).
- **AC-2** — Given the tutorial is running, when I perform each opening control, then that control is acknowledged and checked off, and each of the six is demonstrated diegetically at least once before the tutorial completes.
- **AC-3** — Given the tutorial, when it is running, then it never suspends the turn loop or blocks input: it is a passive coach layered on live play (real turns tick survival normally — the point is to learn the real game), and a player who already knows the controls simply plays and the prompts resolve as they act.

## In/Out of Scope Seam

**In scope:**
- A **core-owned passive tutorial director** (`TutorialController`) that OBSERVES committed turns and coaches Aldric's lines into the message log (AD-1/AD-2, AD-15). It teaches the **six opening controls named by FR-2**: move, scavenge, eat, craft, hide, rest. It tracks which of the six have been demonstrated, prompts the next un-demonstrated one, and completes when all six are done.
- **Diegetic delivery** (FR-2): every prompt/acknowledgement is an Aldric-spoken line appended to the message log (the primary text surface, AD-15). No tooltip, no overlay, no new chrome (NFR-3).
- **Live, non-blocking coaching** (the passive model): the tutorial does NOT suspend the turn loop and does NOT gate input. It runs during normal gameplay after the intro; the player may act freely and the tutorial checks off controls as they are performed (in any order). This is the deliberate contrast with the 2.1 dialogue surface and the 2.2 intro, which ARE safe-pause surfaces.
- **Auto-start when the intro closes on a fresh run**; `restart()` skips it (a new life after death already knows the controls — mirrors 2.2 Decision 5).
- **"Hide" = move into cover** (chosen call): break line of sight by moving to a tile adjacent to a blocking trunk/rock (`RogueTile.WALL`). Reuses movement + the tilemap; no new stealth mechanic (real stealth is Epic 3/4).
- A **headless-testable core controller** with the prompts authored as content — driven by `PlayerAction` kinds and read-only `RunState` (tilemap/player position for the hide check).

**Out of scope:**
- **Aldric's capture / the rescue seed** (Story 2.4) — the tutorial completing is the seam 2.4 hooks; this story does not build the capture, the chasers, or any east-seed. 2.3 only exposes a queryable "tutorial complete" state; 2.4 decides what to persist.
- **Drink, fight, wait-as-a-taught-control, pickup, torch-as-its-own-beat, selection-cycle** — FR-2 scopes the tutorial to the **six** opening controls; the PRD prose review (`review-prose.md:107`) flags that FR-2's list is intentionally a subset and asks it be called "the opening controls." Other keys still work in play; they are simply not tutorial beats.
- **A real stealth/LOS-vs-enemy mechanic** — "hide" is taught as move-into-cover using shipped movement + tilemap; no enemy, no detection system (that is Epic 3/4).
- **Gating / input lockout** — the passive model was chosen; the tutorial never blocks a key.
- **Any persisted tutorial state / new `RunState` field** (AD-6) — the director's progress is transient view-session state, like the 2.1 dialogue controller and the 2.2 intro.
- **Replaying on save/load** — no load path exists yet (deferred O6); when it lands, a loaded run does not resume the tutorial (note for the save/load story).

## Design Decisions (the interpretation calls)

1. **The tutorial is a passive live observer, not a safe-pause surface** *(chosen with Justine, 2026-08-08)*. Unlike `DialogController` (2.1) and `IntroController` (2.2), which suspend the turn loop, `TutorialController` runs DURING live play: after each committed turn (`TurnEngine.advance`), the screen notifies it with the `PlayerAction`, and it may append Aldric's next line. It never suspends the loop, never gates input, never returns a `PlayerAction`. Survival ticks normally — learning the real game is the point (FR-2, AD-14 is not invoked here because the tutorial is not an intro/dialogue screen).

2. **"Hide" is move-into-cover** *(chosen with Justine, 2026-08-08)*. Epic 1 shipped no stealth action, so hide is demonstrated by a `MOVE` that ends with the player orthogonally adjacent to a blocking `RogueTile.WALL` (a trunk/rock — "put a trunk between you and the road"). No new mechanic; reuses movement + the tilemap. The other five controls map to their existing `PlayerAction.Kind`s.

3. **Control → action matchers** (one control checked off per committed turn, first-match by list order):
   - **move** → `Kind.MOVE`
   - **scavenge** → `Kind.COLLECT`
   - **eat** → `Kind.USE`
   - **craft** → any of `{BUILD_CAMPFIRE, COOK, FILTER, BOIL, CRAFT_TORCH}` (robust to whatever materials the player happens to have)
   - **hide** → `Kind.MOVE` **and** the player is now adjacent to a `WALL` tile (Decision 2)
   - **rest** → `Kind.WAIT`
   List order is `[move, scavenge, eat, craft, hide, rest]`; each committed turn checks off the FIRST still-remaining control whose matcher passes (so a plain move checks *move*; a move-into-cover only checks *hide* once *move* is already done — Aldric's prompts stay sensible).

4. **Out-of-order tolerant, prompt-the-next-remaining.** The director holds the six as an ordered remaining-set. On start it prompts the first remaining control; `onAction` removes whichever remaining control the action satisfies (first-match) and prompts the new head; when the set empties it appends Aldric's closing line and completes. A player who does things out of order still checks each off; a player who ignores Aldric keeps the current prompt until they happen to act.

5. **Delivered as Aldric lines in the message log** (FR-2, AD-15). Every prompt and acknowledgement is `state.appendMessages(...)` with Aldric's voice (an "Aldric: …" line, matching the 2.1 speaker convention in-text since the log is plain lines). The log is the sole text surface; no new panel/overlay/chrome. `RunState.appendMessages` gains a third documented caller (TurnEngine AD-4, DialogController 2.1, now TutorialController 2.3) — still the log's sole mutator.

6. **Auto-start when the intro closes; restart skips it.** The screen arms the tutorial at construction (fresh run). When the intro goes inactive (advanced through or skipped), the screen calls `tutorial.begin(state)` once — the first Aldric prompt appends right as gameplay opens, continuing from the 1.8 seeded line. `restart()` calls `tutorial.skip()` so a post-death life gets no coaching (Decision 6 mirror of 2.2 Decision 5). Whether the intro was read or skipped, the tutorial still runs (control onboarding is separate from the story intro) — but because it is passive, a returning player just plays through it.

7. **The tutorial sets no flags and adds no persisted state** (AD-6 by construction). Progress (remaining-set, started/active/completed) is transient view-session state on the controller — not on `RunState`, not serialized. Completion is exposed as a queryable `isComplete()` for Story 2.4 to hook; 2.4 introduces any persisted "tutorial done" signal it needs then (nothing speculative here — mirrors 2.2 Decision 7).

8. **Observation discipline (1.8 / 2.1 lesson).** The tutorial writes ONLY message-log lines — no `FlagStore` key, no `Inventory` change, no turn. It observes committed turns and narrates; it never silently mutates game state. This keeps it a pure coach layered over the AD-4 single acted branch, which is untouched.

## Tasks / Subtasks

- [x] **Task 1 — The core tutorial director (AC: 1, 2, 3)**
  - [x] New `com.margins.rogue.narrative.TutorialController` — pure model, no libGDX (AD-2). Holds the six opening controls as an ordered remaining-set with each control's Aldric prompt + acknowledgement text, plus `begin(RunState)`, `onAction(PlayerAction, RunState)`, `isActive()`, `isComplete()`, `skip()`, and an `arm()`/armed guard.
  - [x] `begin(RunState)`: fires once (guarded) when armed — appends the first control's prompt (Aldric line) and goes active. A no-op if not armed, already begun, or complete.
  - [x] `onAction(PlayerAction, RunState)`: when active, find the FIRST still-remaining control whose matcher (Decision 3) passes for this action+state; if found, append that control's acknowledgement, remove it, and append the next remaining control's prompt; when the set empties, append the closing line and complete (`isActive()` false, `isComplete()` true). A no-op when inactive.
  - [x] `skip()`: disarm + deactivate WITHOUT marking complete (the restart abort — a life after death gets no coaching and 2.4's completion seam is not spuriously tripped).
  - [x] The hide matcher reads `RunState`'s tilemap/player position (orthogonal adjacency to `RogueTile.WALL`); all other matchers key on `PlayerAction.Kind` only. The controller writes only via `state.appendMessages` (Decision 5/8) — no other `RunState` mutation.

- [x] **Task 2 — Author Aldric's tutorial lines (AC: 1, 2)**
  - [x] The six prompts + acknowledgements + the closing line, in Aldric's voice, SPD text-forward tone, delivered as `"Aldric: …"` log lines. Keep them short (one log line each). Prompts name the control diegetically and hint the key softly (e.g., "Move — stay low, follow me. [WASD]"), acknowledgements confirm in-fiction. The closing line hands the flight forward (the 2.4 seam) without building the capture.
  - [x] Content lives with the controller (a small authored table/among the steps) — pure text, no libGDX, headless-readable for the content pin.

- [x] **Task 3 — Wire the tutorial into the screen (AC: 1, 2, 3)**
  - [x] `MarginScreen`: `private final TutorialController tutorial = new TutorialController();` — transient view-session state, NOT on RunState (AD-6, Decision 7). Arm it for a fresh run.
  - [x] Start seam (Decision 6): in `handleInput`, after the intro branch returns while the intro is active, call `tutorial.begin(state)` once the intro is inactive (the guard makes it a safe every-frame call). The first Aldric prompt appends right as gameplay opens, after the 1.8 seeded line.
  - [x] Observe seam (Decision 1): after `turnEngine.advance(state, action)` commits a real turn, call `tutorial.onAction(action, state)` so the coach checks off the performed control and prompts the next. Only on a committed turn (`action != null`); never returns/short-circuits a `PlayerAction` — the AD-4 branch is untouched.
  - [x] `restart()`: add `tutorial.skip()` (a new life after death gets no coaching — Decision 6), symmetric with the `dialog.end()`/`intro.end()` closes already there.
  - [x] No new render surface — Aldric's lines are ordinary log lines (AD-15); the existing bottom-log render shows them. Do NOT add chrome.

- [x] **Task 4 — Log writer wiring (Decision 5/8)**
  - [x] Update `RunState.appendMessages` javadoc to name the third caller (TurnEngine AD-4, DialogController Story 2.1, TutorialController Story 2.3) — the log's sole mutator, now with three documented writers.

- [x] **Task 5 — Tests: sequencing, matchers, hide, passivity, transient (AC: all)**
  - [x] New `core/src/test/java/com/margins/rogue/narrative/TutorialControllerTest.java`:
    - `begin()` appends the first prompt and goes active; `begin()` again is a no-op; `begin()` when unarmed/skipped is a no-op.
    - Each control matcher checks off its control and appends an acknowledgement + the next prompt: move (`MOVE`), scavenge (`COLLECT`), eat (`USE`), craft (each of `BUILD_CAMPFIRE/COOK/FILTER/BOIL/CRAFT_TORCH`), rest (`WAIT`).
    - **Hide pin** (Decision 2): a `MOVE` that lands adjacent to a `WALL` checks off *hide* (once *move* is done); a `MOVE` in the open does not.
    - **Out-of-order + completion**: performing the six in a scrambled order checks each off exactly once and completes after all six (`isComplete()` true, `isActive()` false).
    - **Passivity pin** (AC-3): `onAction` writes ONLY log lines — assert `getClockTurns()` and the four survival tracks are UNCHANGED by `onAction` itself (the tutorial observes; it does not tick), and no `FlagStore`/`Inventory` mutation occurs. (The turn ticking is `TurnEngine`'s job, already pinned; the tutorial adds nothing to it.)
    - **Skip pin**: `skip()` deactivates without `isComplete()` (the restart abort does not trip 2.4's seam).
  - [x] **Content pin**: the authored lines cover all six controls (each control has a non-empty prompt) and a closing line; every line is Aldric's voice.

- [x] **Task 6 — Full suite, no regressions (AC: all)**
  - [x] The 2.1/2.2 suites stay green (`DialogControllerTest`, `DialogueGateTest`, `DialogueEffectTest`, `DialogueSafePauseTest`, `IntroControllerTest`).
  - [x] `mvn -o clean install` — full suite green, no regressions in the existing 239 tests.
  - [x] Serialization: 2.3 adds NO new persisted field to `RunState` (Decision 7 — the director is transient). AD-6 rule satisfied by construction.

## Dev Notes

### Current state (what exists, to preserve)

- **The turn loop + live play (Epic 1):** `MarginScreen.handleInput` routes: game-over `[R]` branch → intro branch (2.2, `if (intro.isActive()) { handleIntroInput(); return; }`) → dialogue branch (2.1) → selection reset → `readAction` → `if (action != null) turnEngine.advance(state, action)`. `TurnEngine.advance` is the single acted branch (AD-4): it ticks the four survival tracks, the clock, debuffs, FOV, and feeds the log. The tutorial observe-seam sits right after this `advance` call. Read: `core/src/main/java/com/margins/MarginScreen.java`.
- **`PlayerAction`** (`com.margins.rogue.system`): a `Kind` enum + factory methods. The tutorial matches on `kind` (MOVE/COLLECT/USE/WAIT and the five craft kinds) and, for hide, reads `RunState` position vs. the tilemap. Read: `core/src/main/java/com/margins/rogue/system/PlayerAction.java`.
- **`RunState`** (`com.margins.rogue.state`): the log's SOLE mutator is `appendMessages(List<String>)` (called by `TurnEngine` AD-4 and `DialogController` Story 2.1 — javadoc names both; add TutorialController). `getTileMap()`, `getPlayer()` (`getTileX()/getTileY()`), `getClockTurns()`, and the four track getters are read-only here. A fresh `RunState` SEEDS the log with "You flee into the pines. Aldric is beside you." — the tutorial's first prompt appends right after it.
- **`RogueTileMap` / `RogueTile`**: `getTile(x, y)` returns the tile id, or `< 0` off-map; `RogueTile.WALL` is the blocking trunk/rock the hide check keys on (the same constant `MarginScreen.setTile` renders as the dark trunk). Read: `core/src/main/java/com/margins/rogue/RogueTileMap.java`, `RogueTile.java`.
- **`IntroController`** (2.2): the intro opens in the `MarginScreen` constructor and closes on advance-through or ESC-skip. The tutorial begins the frame the intro goes inactive (Decision 6). `restart()` already closes `dialog` + `intro`; add `tutorial.skip()`.
- **`DialogController` (2.1)** stays wired but has no in-game opener (2.2 removed the N key). The tutorial does NOT use it — the coach writes plain Aldric log lines, not full-screen dialogue pages (the player must see the world and act while being coached).

### Carried lessons (2.1 + 2.2 review findings, applied)

- **Passivity is the safety property here — pin it honestly (2.1/2.2 safe-pause lesson, inverted).** 2.1/2.2 pinned "a full scene ticks nothing" because they SUSPEND play. 2.3 is the opposite: it runs during live turns, so the honest pin is that `onAction` itself writes only log lines and mutates no game state (no tick, no flag, no inventory) — the turn ticking belongs to `TurnEngine`, which the tutorial does not touch. Document that the enforcing "start when intro closes / observe after advance" wiring is the screen's (thin glue over the core-tested controller), exactly as the 2.2 review noted for the intro's structural pin.
- **Narration/speaker discipline (2.1 Task 4 + review).** Aldric's tutorial lines DO carry his name ("Aldric: …") — he is the speaker, this is diegetic dialogue, not third-person narration. (Contrast the 2.2 intro, which is speaker-less narration.)
- **AD-6 transient rule (1.1 retro).** 2.3 adds NO new persisted field — the director is transient view-session state; a save mid-tutorial writes nothing tutorial-shaped. Satisfied by construction; Task 5 pins it via the existing round-trip coverage (the tutorial touches none of the persisted stores).
- **Observation discipline (1.8 lesson).** Every control the player performs is acknowledged in the log — the coach never silently checks something off; the ack line IS the observation. Task 5 asserts each control's ack appears.
- **Scope discipline / the FR-2 subset (PRD review-prose.md:107).** The tutorial teaches the six FR-2 controls only ("the opening controls"), not the full 9/12-key set — resolving the flagged PRD tension by construction. Do not add drink/fight/pickup/torch/selection beats.
- **Log-window policy (2.1 Decision 1).** Aldric's lines flow into the same bottom log as gameplay events; the last-5 window applies. Prompts are one line each so a prompt is not pushed off-screen by a single event before the player can read it (keep them terse).

### Placement rationale (AD-1/AD-2/AD-4/AD-15)

- The director lives in the CORE (`rogue/narrative/TutorialController`) — pure model, no libGDX (AD-2), headless-testable — mirroring `DialogController`/`IntroController`. The screen forwards the committed `PlayerAction` and does no tutorial logic (AD-1).
- It is NOT a `System` (the `System`-suffix convention is the turn pipeline; the tutorial is a passive observer that runs *after* the pipeline, not a stage in it) and NOT a safe-pause surface (Decision 1). It sits beside `TurnEngine`, reading committed turns.
- The AD-4 single acted branch is untouched: `onAction` runs after `advance`, returns nothing, and cannot alter the turn (Decision 8).
- Delivery is the bottom message log (AD-15) — the coach adds no chrome (NFR-3).

### Serialization — what NOT to do

- The director + its progress are TRANSIENT view-session state: NOT on `RunState`, NOT in the libGDX Json graph. Do NOT serialize the remaining-set or any tutorial flag. The tutorial writes NO `FlagStore` key and NO `Inventory` count (Decision 7/8) — only log lines, which are already part of the run. A save taken mid-tutorial round-trips exactly as before; no new persisted field (AD-6 checked).
- A future save/load UI (deferred O6) must not resume the tutorial on a loaded run — it begins only when a freshly-constructed screen's intro closes (Decision 6). Note for the save/load story; do not build the mechanism now.

### Scope discipline (CLAUDE.md §2/§3)

- Touch only: new `core/src/main/java/com/margins/rogue/narrative/TutorialController.java`, `core/src/main/java/com/margins/MarginScreen.java` (tutorial field + begin-seam + observe-seam + `restart()` skip), `core/src/main/java/com/margins/rogue/state/RunState.java` (appendMessages javadoc only — no behavior change), and the new `TutorialControllerTest`.
- Do NOT build: the capture/rescue (2.4), quest/Journal (2.5), a real stealth mechanic, gated/lockout tutorials, any persisted tutorial state, any new `FlagStore` key, any chrome beyond log lines, or extra control beats beyond the FR-2 six.
- Keep the 2.1 `DialogController` and 2.2 `IntroController` contracts untouched — the tutorial is a new, independent surface.
- If a prompt's wording needs tuning, keep Aldric's SPD voice and change the string — no message configurability.

### Testing standards

- Headless JUnit 5, no libGDX (AD-2). `TutorialController` tests drive `PlayerAction`s + a `RunState` (real `new RunState(seed)`), asserting the log lines, the checked-off controls, and completion.
- The hide pin needs a known tilemap: use the real `RunState` and move the player to a tile adjacent to a `WALL` (assert hide checks off), and to an open tile (assert it does not). If positioning against the generated map is awkward, assert the matcher against the map at the player's spawn neighbourhood — keep the pin deterministic (`new RunState(fixedSeed)`).
- The passivity pin asserts `onAction` changes no clock/track/flag/inventory — the tutorial observes, it does not tick.
- The screen wiring (begin-on-intro-close, observe-after-advance, restart-skip) stays thin glue over the core-tested controller (the 2.2 review's "document that the enforcing branch is the screen's" applies).

### Project Structure Notes

- New (production): `core/src/main/java/com/margins/rogue/narrative/TutorialController.java` (the passive coach; authored lines live with it).
- New (tests): `core/src/test/java/com/margins/rogue/narrative/TutorialControllerTest.java`.
- Modified: `MarginScreen.java` (tutorial field + begin/observe seams + restart skip), `RunState.java` (appendMessages javadoc).
- Naming: `TutorialController` follows the `DialogController`/`IntroController` precedent (a suspended-or-observing surface controller, NOT a `System`).

### References

- [Source: epics.md#Story-2.3 (lines 360-374)] — the two ACs: how-to-play (move, scavenge, eat, craft, hide, rest) as in-world dialogue not UI chrome; each core control demonstrated diegetically at least once.
- [Source: prd.md#FR-2 (lines 141-145)] — "the tutorial is delivered by Aldric as in-world dialogue, not UI chrome"; "All controls (move, scavenge, eat, craft, hide, rest) are demonstrated diegetically during the opening flight." §4.2 (line 156) lists the full 9-action turn vocabulary — the tutorial teaches the FR-2 subset.
- [Source: prds/prd-The-Margin-2026-08-06/review-prose.md:107] — flags that FR-2's six-control list is a deliberate subset of §4.2's nine actions and asks it be framed as "the opening controls"; this story adopts that framing (scope discipline).
- [Source: architecture spine AD-15] — the bottom message log is the primary text surface (the tutorial's delivery channel); [AD-1/AD-2] core owns the rule, the screen is thin glue; [AD-4] the single acted branch is untouched by the observer.
- [Source: story-2.2-the-skippable-paged-text-intro.md] — the intro that precedes the tutorial (the begin-on-close seam), the transient-controller + AD-6 pattern, and the 2.2 review lesson (document that the enforcing wiring is the screen's).
- [Source: story-2.1-text-forward-dialogue-nodes-with-safe-pause.md + RunState.java] — the log's sole-mutator `appendMessages` (javadoc names its callers); the speaker convention for Aldric's lines.
- [Source: story-2.4 (epics.md lines 376-390)] — the capture that fires when "the tutorial is complete"; 2.3 exposes `isComplete()` as that seam and builds nothing of 2.4.
- [Source: deferred-work.md O6] — "SaveService has no production callers" — why the tutorial's new-run-only begin is screen-construction-level and no load-resume is built.

## Dev Agent Record

### Agent Model Used

Claude Opus 4.8 (1M context)

### Debug Log References

- `mvn -o clean install` — BUILD SUCCESS; core suite 253 tests, 0 failures, 0 errors, 0 skipped (was 239; +14 new `TutorialControllerTest`).

### Completion Notes List

- **Task 1** — `TutorialController`: pure-model passive coach, no libGDX. Holds the six controls as an ordered remaining-set; `begin()` fires the first prompt once (armed guard); `onAction()` first-matches a remaining control against the committed `PlayerAction` (+ tilemap for hide), acknowledges it, prompts the next, and completes on the last; `skip()` aborts without completing; `isActive()`/`isComplete()` are the queryable seams. Writes only `state.appendMessages(...)` — no turn, no flag, no inventory.
- **Task 2** — Authored Aldric's six prompt/ack pairs + the closing line as an enum (`Control`), SPD voice, one log line each, soft key hints. Hide is narrated as "put a trunk between you and the road."
- **Task 3** — `MarginScreen`: added the `tutorial` field (transient); `tutorial.begin(state)` right after the intro branch (fires once the intro closes — guarded, no-op on restart runs); `tutorial.onAction(action, state)` right after `turnEngine.advance` on a committed turn; `restart()` calls `tutorial.skip()`. No new render surface — Aldric's lines are ordinary log lines (AD-15).
- **Task 4** — `RunState.appendMessages` javadoc now names the third caller (TurnEngine AD-4 / DialogController 2.1 / TutorialController 2.3). No behavior change.
- **Task 5** — `TutorialControllerTest` (14): begin idempotence + begin-after-skip no-op; each control matcher (move/scavenge/eat/all five craft kinds/rest); the hide pin (move-into-cover checks off hide, a move in the open does not); out-of-order completion; the passivity pin (`onAction` ticks no clock/track and mutates no flag/inventory); onAction-when-inactive no-op; skip deactivates without completing; the content pin (every control + closing is an Aldric line).
- **Task 6** — Full suite green (253); no new persisted `RunState` field (AD-6 by construction — the controller is transient). The 2.1/2.2 suites stay green; the `DialogController`/`IntroController` contracts are untouched.

### File List

- **New:** `core/src/main/java/com/margins/rogue/narrative/TutorialController.java`
- **New:** `core/src/test/java/com/margins/rogue/narrative/TutorialControllerTest.java`
- **Modified:** `core/src/main/java/com/margins/MarginScreen.java` (tutorial field + begin-seam after intro close + observe-seam after advance + `restart()` skip)
- **Modified:** `core/src/main/java/com/margins/rogue/state/RunState.java` (appendMessages javadoc — third caller; no behavior change)

## Change Log

| Date | Who | Change |
|------|-----|--------|
| 2026-08-08 | Create | Story 2.3 created (Status: ready-for-dev) from epics.md Story 2.3 + PRD FR-2/§4.2 + the PRD prose-review subset note + AD-15, carrying the 2.1/2.2 review lessons. Two interpretation calls resolved with Justine: the tutorial is a **passive live observer** (not a safe-pause surface — Decision 1), and **"hide" = move-into-cover** (Decision 2, no new stealth mechanic). Scope locked to the six FR-2 "opening controls"; completion exposed as a transient `isComplete()` seam for Story 2.4 (AD-6 by construction). |
| 2026-08-08 | Dev | Implemented all 6 tasks. New `TutorialController` (passive coach — observes committed turns, writes only Aldric log lines, never suspends the loop) + authored six prompt/ack pairs. `MarginScreen`: begin-seam after the intro closes, observe-seam after `advance`, `restart()` skip. `RunState.appendMessages` javadoc names the third caller. 14 new tests (matchers, hide-into-cover, out-of-order completion, passivity pin, skip). Full suite green (253 tests, 0 failures). No new persisted field (AD-6 by construction). Status → review. |

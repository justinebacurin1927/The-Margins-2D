# Story 2.2: The skippable paged text intro

Status: review

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As a new player,
I want a skippable, paged intro covering the fall of Corneo,
so that I get the story without being forced to read it (FR-1).

## Acceptance Criteria

- **AC-1** — Given a new run, when the intro plays, then it pages through the before, the fall, and the hand-off (Klein and Aldric fleeing) in the SPD text-forward tone.
- **AC-2** — Given any intro screen, when I choose skip, then it jumps to gameplay in one action.
- **AC-3** — Given any intro screen, when it is shown, then no survival track ticks and no turn is consumed (AD-14).

## In/Out of Scope Seam

**In scope:**
- The **core-owned paged-text sequence** (FR-1, AD-14 — "Act 0 is a core-owned sequence presented by the screen"): an ordered list of narration pages covering the *before* (Klein's posting, his two duties — guard the Copper Road, guard the town —, Magdalene's letter, Aldric), the *fall* (the midmorning horn, the Evermove column, the Sense-user who undoes men with a look, Corneo burning), and the *hand-off* (Klein and Aldric fleeing deep in the trees). Advanced one page at a time; presented by the screen in the bottom-log surface (AD-15 — no new chrome).
- **Skip in one action** (FR-1: "a 'skip' path exists on every intro screen and skips to gameplay in one action"): ESC from ANY page closes the intro and gameplay begins immediately.
- **Structural safe pause** (AD-14, AD-5 — mirrors 2.1 Decision 6): while the intro is active, `handleInput` routes only intro keys — no `PlayerAction`, so `TurnEngine.advance` never runs, no survival tick, no turn committed.
- **Auto-open on a new run** at screen construction (fresh `RunState` = new run); **NOT** on restart.
- **Remove the superseded 2.1 smoke scene + N debug key** (2.1 Decision 8: "superseded by the 2.2 intro, which removes this key"). The intro is the new in-game verification seam.
- A **headless-testable core controller** (`IntroController`) with the pages authored as content — the intro page reuses `DialogNode` (zero-option narration node = the 2.1 Decision-7 terminal-page shape).

**Out of scope:**
- **Aldric's diegetic tutorial** (Story 2.3) — the intro is pure text; no gameplay, no control teaching.
- **Aldric's capture / rescue seed** (Story 2.4) — the intro ends at the hand-off; capture is a later beat.
- **Quest flags / passive Journal** (Story 2.5).
- **Any gameplay during the intro** — it is text-only; control frees at the hand-off.
- **Replaying on save/load** — no load path exists in the screen yet (deferred-work O6: "SaveService has no production callers"). When load lands, the intro opens only on a fresh run (screen construction with a fresh `RunState`), never on a loaded one.
- **Any persisted intro state / new RunState field** (AD-6) — the controller and the open page are transient view-session state, like 2.1's dialogue controller.
- **A player dialogue trigger** — with the N key removed, no authored dialogue is player-triggerable until 2.3 wires the tutorial. The 2.1 `DialogController` machinery stays wired and test-covered; it simply has no in-game opener this story.

## Design Decisions (the interpretation calls)

1. **The intro page reuses `DialogNode` — the 2.1 Decision-7 terminal-page shape.** Each intro page is a zero-option `DialogNode` (text + nullable speaker; narration has no speaker — 2.1 Task 4's lesson). A small new core controller, `IntroController`, sequences them. One content-page shape for every text-forward surface (AD-15); no parallel `IntroPage` type, no new content model. `IntroController` mirrors `DialogController`'s role (navigation authority in the core, AD-1/AD-2) but is SIMPLER: it holds ordered pages and advances by index — it needs no RunState reference because intro pages carry no effects and write no state (Decision 7).

2. **Skip is ESC, from every page, in one action.** SPACE/E advance one page; ESC closes the intro to gameplay immediately from any page (FR-1). The page render shows the affordance footer — "[SPACE] continue   [ESC] skip" — on every page. ESC is already the dialogue-cancel key in 2.1 (`handleDialogueInput`); this extends the same cancel affordance to the intro.

3. **Safe pause is structural — the 2.1 pattern applied to the intro.** While `intro.isActive()`, `handleInput` returns in the intro branch BEFORE `readAction` — no `PlayerAction`, so `advance()` never runs, no tick, no turn (AD-14). The intro branch sits before the dialogue branch in `handleInput` (the intro plays at app start, before any dialogue can open). AD-4's single acted branch is untouched.

4. **The intro is core content, presented by the screen** (AD-14). The pages live in `rogue/narrative/` as authored content (`CorneoIntro.build()`), `IntroController` owns sequencing, and the screen renders `getCurrent()` in the bottom-log surface and forwards advance/skip — thin glue, no logic (AD-1). The screen never mutates intro state.

5. **Auto-open on a new run, once.** The screen opens the intro in its constructor (a fresh `RunState` = a new run). `restart()` does NOT replay it — a new life after death already knows the story, and the 1.8 restart feedback ("Another life. [WASD] move.") stays. When a save/load UI lands (deferred O6), a loaded run constructs its screen with the loaded `RunState` and will not open the intro.

6. **Remove the 2.1 smoke scene + N debug key.** The intro is the new in-game verification seam (2.1 Decision 8). `SampleDialog.java` is deleted; the N binding and its import leave `MarginScreen`; the `sceneKeysAreNamespaced` pin (2.1 Task 5 — it namespaced the smoke scene's `KEY_SMOKE_READ`) dies with the scene, since the intro adds NO keys — the per-scene key rule stays pinned by `SceneEffectsTest` (`KEY_CACHE_REVEALED`/`KEY_CACHE_SPAWNED`). This also resolves the 2.1 review's deferred finding (N-key scene re-fires effects on re-open — farmable coal / repeatable Bond behind a debug key): the real intro is text-only, so the pattern cannot carry over.

7. **The intro sets no flags and adds no persisted state** (AD-6 satisfied by construction). Pages are pure text; completion is the transient `isActive()` flip. When 2.3/2.4 need an "intro finished" signal, they read the closed-intro state or introduce their own flag then — nothing speculative this story.

## Tasks / Subtasks

- [x] **Task 1 — The core paged-text controller (AC: 1, 3)**
  - [x] New `com.margins.rogue.narrative.IntroController` — pure model, no libGDX (AD-2). Holds an ordered list of pages (`DialogNode`, zero-option narration nodes), with `start(List<DialogNode> pages)`, `getCurrent()`, `isActive()`, `advance()` (move to the next page; on the LAST page → close), and `end()` (close — the skip path). No RunState parameter — pages carry no effects (Decision 1/7), so the controller has no run access and physically cannot tick anything (the safe-pause seam).
  - [x] `getCurrent()` returns the current page, or null when inactive. `advance()` on a closed controller is a no-op. Out-of-range/closed states are defensive no-ops (mirror `DialogController`'s posture).
  - [x] The controller is the navigation authority (AD-1/AD-2) — the screen only calls `advance()`/`end()` and renders `getCurrent()`.

- [x] **Task 2 — The authored intro content (AC: 1)**
  - [x] New `com.margins.rogue.narrative.CorneoIntro` (final class, private ctor, `public static List<DialogNode> build()`) — the Act 0 text intro, in SPD-tone narration (nullable speaker — narration has no speaker), covering the three beats from PRD §4.1:
    - **Before** (~2 pages): Klein's posting to Corneo, a routine border knight; his two duties — guard the Copper Road, guard the town; Magdalene's letter (the west pull home); Aldric, the fellow knight who stays by him.
    - **Fall** (~2-3 pages): the midmorning horn; the Evermove column on the Copper Road; the Sense-user who undoes men with a look (the garrison breaks); Corneo burning.
    - **Hand-off** (1 page): Klein and Aldric fleeing deep in the pines, Corneo burning behind them — the page frames the 1.8 seed line ("You flee into the pines. Aldric is beside you."), which is the run's seeded opening line and the 2.2→gameplay hand-off hook.
  - [x] Every page is a zero-option `DialogNode` (Decision 1). No effects, no flags, no options (Decision 7). Page count is small and paged (5-6), so a fresh player reads at their own pace and a vet ESC's through in one press.
  - [x] Do NOT wire it to run-start in this task — the screen wiring is Task 3.

- [x] **Task 3 — The intro surface + structural safe pause (AC: 1, 2, 3)**
  - [x] `MarginScreen`: `private final IntroController intro = new IntroController();` — transient view-session state, NOT on RunState (AD-6, Decision 7).
  - [x] Open the intro in the constructor (a fresh `RunState` = a new run): `intro.start(CorneoIntro.build());` after `FovSystem.compute(state)`. Do NOT open it in `restart()` (Decision 5 — restart skips the intro).
  - [x] Safe-pause routing (Decision 3): in `handleInput`, add an intro-active branch BEFORE the dialogue branch — `if (intro.isActive()) { handleIntroInput(); return; }` — so no `PlayerAction` is produced while the intro is open (no turn, no survival tick; AD-14). The intro swallows every gameplay key.
  - [x] `handleIntroInput()`: SPACE/E → `intro.advance()`; ESC → `intro.end()` (the skip path — Decision 2); everything else is swallowed. Forwarding only (AD-1) — the controller owns navigation.
  - [x] Render (Decision 2, AD-15): while `intro.isActive()`, render the intro page in the bottom-log surface — speaker (null → narration, no prefix), wrapped node text, and the "[SPACE] continue   [ESC] skip" footer — instead of the event window; when closed, revert to the last-5 event lines. Reuse the 2.1 page rendering (`renderDialoguePage`'s speaker/text drawing + `wrapText`) — factor the shared part if it keeps the screen thin (no new chrome, NFR-3).

- [x] **Task 4 — Remove the superseded 2.1 smoke scene (Decision 6)**
  - [x] Delete `core/src/main/java/com/margins/rogue/narrative/SampleDialog.java`.
  - [x] `MarginScreen`: remove the `SampleDialog` import and the N-key binding in `readAction` (`if (down(Input.Keys.N)) { dialog.start(SampleDialog.build(), state); return null; }`).
  - [x] Delete the `sceneKeysAreNamespaced` test from `DialogueSafePauseTest` (its namespace was the smoke scene's `KEY_SMOKE_READ`; the intro adds no keys, so the assertion is vacuous — the per-scene key rule stays pinned by `SceneEffectsTest`).
  - [x] Verify no dangling `SampleDialog`/`KEY_SMOKE_READ` references remain (deletion check).

- [x] **Task 5 — Tests: sequencing, safe-pause pin, transient (AC: all, carry 2.1 lessons)**
  - [x] New `core/src/test/java/com/margins/rogue/narrative/IntroControllerTest.java`: `advance()` walks pages in order (`getCurrent()` advances); `advance()` on the LAST page closes (`isActive()` false); `end()` (skip) from any page closes immediately; `getCurrent()` is null when inactive; `advance()`/`end()` on a closed controller are no-ops.
  - [x] **Content pin**: `CorneoIntro.build()` returns ≥3 pages covering the three beats (before / fall / hand-off), and every page has a null speaker (narration — 2.1 Task 4's "narration has no speaker").
  - [x] **Safe-pause pin** (AD-14, mirror 2.1's `readingAndChoosingATurnTicksNothing`): drive the full intro through the controller and assert `getClockTurns()` and the four tracks (hunger/thirst/temperature/HP) are UNCHANGED. Note: the controller takes no RunState (Decision 1), so this is structural — it documents that the intro physically cannot tick; the enforcing branch is the screen's (Task 3), which stays thin glue over core-tested behavior.
  - [x] **Transient pin** (AD-6): the intro controller holds no RunState reference and adds no persisted field (satisfied by construction); the existing 2.1 round-trip test (`aReloadedRunStartsWithNoSceneOpenAndRoundTripsEffectState`) still covers the stores the intro never touches.

- [x] **Task 6 — Full suite, no regressions (AC: all)**
  - [x] The 2.1 dialogue suites stay green: `DialogControllerTest`, `DialogueGateTest`, `DialogueEffectTest`, `DialogueSafePauseTest` (minus the deleted `sceneKeysAreNamespaced`).
  - [x] `mvn -o clean install` — full suite green, no regressions in the 232 existing tests.
  - [x] Serialization: 2.2 adds NO new persisted field to `RunState` (Decision 7 — the controller/surface are transient; the intro writes nothing). AD-6 rule satisfied by construction.

## Dev Notes

### Current state (what exists, to preserve)

- **The 2.1 dialogue surface + safe pause are in place and test-covered (232 tests green):** `MarginScreen` holds `private final DialogController dialog`; `handleInput` routes an intro-less flow — dead branch (`[R]` → `restart()`), dialogue branch (`if (dialog.isActive()) { handleDialogueInput(); return; }`), selection reset, `readAction` → `turnEngine.advance`. `readAction` binds WASD/UP/DOWN/LEFT/RIGHT/Q/G/SPACE/C/B/T/K/F/V/E/TAB/RIGHT_BRACKET/LEFT_BRACKET/**N (the debug smoke-scene key — REMOVED this story)**; the selection cycle TAB/[/] returns null (not a turn). Read: `core/src/main/java/com/margins/MarginScreen.java`.
- **`handleDialogueInput`** (2.1): R → `restart()`; `NUM_1..NUM_9` → `dialog.select(i-1, state)`; SPACE/E on a zero-option node → `dialog.end()`; ESC → `dialog.end()`. The `renderDialoguePage` draws speaker/text/choices and — for zero-option nodes — a "[SPACE] continue" footer (2.1 Decision 7). `wrapText(text, maxWidthPx)` word-wraps at the font. These are reusable for the intro page (Decision 2 footer is the only addition).
- **`restart()`** (2.1 review patch): closes any open scene (`dialog.end()`), `state.restart()`, `FovSystem.compute(state)`, clears the selection, appends "Another life. [WASD] move." It does NOT open the intro (Decision 5).
- **`DialogNode`** (`com.margins.dialog`, 2.1): `text`, `speaker` (nullable — null = narration), `options[]` (zero-option = the Decision-7 terminal page shape), `effects` list, `withFlag`/`withEffect`/`withSpeaker` builders, `GateStat` gated options. Pure content model, no libGDX. The intro pages are zero-option nodes — already rendered by 2.1's zero-option branch.
- **`RunState`** (`com.margins.rogue.state`): the log's SOLE mutator is `appendMessages` (now called by `TurnEngine` AD-4 and `DialogController` Story 2.1 — its javadoc names both). A fresh `RunState` SEEDS the log with the 1.8 opening line: "You flee into the pines. Aldric is beside you." — the 2.2 intro's hand-off hook. `getClockTurns()` + the four tracks are read-only here.
- **The 2.1 smoke scene (`SampleDialog`)** is the OLD verification seam: opened by the N debug key, `KEY_SMOKE_READ = "scene.smoke.read"`. It is SUPERSEDED by this story's intro (2.1 Decision 8) and DELETED (Task 4). Its only non-screen reference is the 2.1 namespacing test (`DialogueSafePauseTest.sceneKeysAreNamespaced`).
- **`SceneEffects`** keeps the `KEY_CACHE_*` single-authority key contract (`SceneEffectsTest` pins it) — untouched.

### Carried lessons (2.1 review findings + retro discipline, applied)

- **The safe-pause pin tests the controller, not the screen — be explicit about it (2.1 review finding).** The 2.1 review flagged that `readingAndChoosingATurnTicksNothing` drives `DialogController` directly while the real protection is the screen's structural branch. The intro repeats this by design: `IntroController` takes no `RunState` (so the pin is genuinely structural — it cannot tick), and the enforcing branch is `MarginScreen.handleInput` (Task 3). Document it; do not overclaim that the screen branch is pinned headlessly.
- **Narration has no speaker (2.1 Task 4 + review patch).** Third-person intro text must NOT carry a speaker — null speaker renders no prefix. The intro is all narration (Decision 2/7).
- **Zero-option nodes are the Decision-7 terminal-page shape.** A node with no options is a text page closed by an advance key — this is exactly the intro page. Reuse the 2.1 render; add only the "[ESC] skip" affordance.
- **AD-6 transient rule (codified in the 1.1 retro).** A field-absent save inherits ctor-rolled nondeterministic state. 2.2 adds NO new persisted field — the controller/open page are transient view-session state; the intro writes nothing. Satisfied by construction; Task 5 pins it.
- **Observation discipline (1.8 lesson).** The intro writes NO narrative state (Decision 7), so there is nothing to observe — but the lesson constrains the authoring: the intro must not silently mutate anything. Text-only pages guarantee it.
- **The deferred N-key pattern (2.1 review, [Review][Defer] #2).** "Scene entry-effects re-firing on re-entry … must NOT carry into 2.2's real intro authoring." Resolved by Decision 7 (text-only pages) + Decision 6 (smoke scene deleted) — there is no entry-effect to re-fire.
- **Authoring-contract hardening (carry #5).** Not directly exercised (no options/flags in the intro), but the intro pages reuse `DialogNode` — do not add options/effects/flags to them. Keep the intro a pure linear sequence.

### Placement rationale (AD-1/AD-2/AD-4/AD-14/AD-15)

- The intro lives in the CORE (`rogue/narrative/`: `IntroController` + `CorneoIntro`) per AD-14 ("a core-owned sequence … presented by the screen") and the architecture Capability map (Act 0 intro → `rogue/narrative/*` + `MarginScreen`). Pure model, no libGDX (AD-2), headless-testable.
- `IntroController` is the sequencing authority (mirrors `DialogController` for dialogue, `TurnEngine` for turns); the screen renders `getCurrent()` and forwards `advance()`/`end()` — thin glue (AD-1). It is NOT a `System` (the `System`-suffix convention is for turn-pipeline systems; the controller is a suspended-turn surface).
- Safe pause is structural (AD-14): while `intro.isActive()`, `handleInput` returns before `readAction` — no `PlayerAction`, so `advance()` never runs. The AD-4 single acted branch is untouched.
- The intro renders in the bottom-log surface (AD-15): while open, the log shows the intro page (speaker/text/footer) instead of the last-5 event lines; on close it reverts — the 2.1 log-window policy (Decision 1) applied to the intro. No new panel/chrome (NFR-3).

### Serialization — what NOT to do

- The intro controller + open page are TRANSIENT view-session state: NOT on `RunState`, NOT in the libGDX Json graph. Do NOT serialize the current page or any intro state. The intro writes NO `FlagStore` key, NO `Inventory` count, NO log line (Decision 7) — a save taken during gameplay round-trips exactly as before. No new persisted field is added (AD-6 rule checked).
- A future save/load UI (deferred O6) must not replay the intro on a loaded run — the intro opens only when the screen constructs with a fresh `RunState` (Decision 5). Note this for the save/load story; do not build the mechanism now.

### Scope discipline (CLAUDE.md §2/§3)

- Touch only: new `core/src/main/java/com/margins/rogue/narrative/IntroController.java`, new `CorneoIntro.java`, `core/src/main/java/com/margins/MarginScreen.java` (intro field, constructor open, intro branch + `handleIntroInput`, intro page render, remove N key + SampleDialog import), delete `SampleDialog.java`, and the tests (`IntroControllerTest` new; `DialogueSafePauseTest` drop the namespacing test).
- Do NOT build: the tutorial (2.3), capture/rescue (2.4), quest/Journal (2.5), any gameplay in the intro, any persisted intro state, a dialogue trigger (2.3 wires the next opener), any new `FlagStore` key, any chrome beyond the intro footer.
- Keep the 2.1 `DialogController`/`DialogNode`/`FlagStore`/`SceneEffects` contracts untouched — only the screen's N binding and the namespacing test are removed with the smoke scene.
- If a message's wording needs tuning, keep the SPD tone and change the string — no message configurability.

### Testing standards

- Headless JUnit 5, no libGDX (AD-2). `IntroController` needs no `RunState` — its tests are pure sequencing.
- Safe-pause pin (AD-14): assert `getClockTurns()` + four tracks unchanged across a full intro (the controller physically cannot tick — Decision 1; the pin documents it, mirroring the 2.1 honesty pins).
- The controller's `advance()`/`end()` are the headless seam — the screen work (Task 3-4) stays thin glue over core-tested behavior.
- The intro content is authored as a pure builder (`CorneoIntro.build()`) — the content pin checks beats + narration, keeping the prose testable without libGDX.

### Project Structure Notes

- New (production): `core/src/main/java/com/margins/rogue/narrative/IntroController.java` (sequencing authority, no RunState), `CorneoIntro.java` (authored pages — the `SampleDialog` slot, repurposed as the real intro).
- New (tests): `core/src/test/java/com/margins/rogue/narrative/IntroControllerTest.java` (per-feature suite naming — the `DialogControllerTest`/`DialogueSafePauseTest` precedent).
- Modified: `MarginScreen.java` (intro field + branch + render; N key removed), `DialogueSafePauseTest.java` (namespacing test deleted with the smoke scene).
- Deleted: `SampleDialog.java`.
- Naming: `IntroController` follows the `DialogController` precedent (a suspended-turn-surface controller, NOT a `System`). `CorneoIntro` names the content by its subject (the Fall of Corneo).

### References

- [Source: epics.md#Story-2.2 (lines 340-358)] — the three ACs verbatim: paged before/fall/hand-off in the SPD text-forward tone; skip jumps to gameplay in one action; no survival tick and no turn consumed (AD-14).
- [Source: prd.md#FR-1 (lines 128-139)] — §4.1 The Fall of Corneo: the *before* (Klein's posting, his two duties — guard the Copper Road, guard the town —, Magdalene's letter, Aldric), the *fall* (the midmorning horn, the Evermove column, the Sense-user who undoes men with a look, Corneo burning), and the *hand-off* (Klein and Aldric fleeing deep in the trees, Corneo burning behind them); "a 'skip' path exists on every intro screen and skips to gameplay in one action"; "intro screens do not tick survival tracks or consume turns."
- [Source: architecture spine AD-14 (line 151-155)] — "Act 0 is a core-owned sequence … presented by the screen. Intro screens commit no turn and tick no survival clock (AD-5). A 'skip' path exists on every intro screen and skips to gameplay in one action." Also the Capability map (line 223): Act 0 intro → `rogue/narrative/*` + `MarginScreen`, governed by AD-14/AD-5.
- [Source: story-2.1-text-forward-dialogue-nodes-with-safe-pause.md] — the ratified dialogue surface + safe pause this story extends; 2.1 Decision 8 (the smoke scene is "superseded by the 2.2 intro, which removes this key"); 2.1 Decision 7 (zero-option node = terminal text page closed by SPACE/E — the intro page shape); the 2.1 review patches (R-restart path, [SPACE] continue footer, null-text guard, text-cap); the review's deferred #2 (N-key re-fire pattern must not carry into 2.2's authoring).
- [Source: story-1.8-the-survival-hud-and-message-log.md + RunState.java] — the seeded opening line ("You flee into the pines. Aldric is beside you.") is the 2.2 hand-off hook; the log's sole-mutator `appendMessages` (javadoc names TurnEngine + DialogController).
- [Source: deferred-work.md O6] — "SaveService has no production callers" — why no load path exists yet and the intro's new-run-only open is screen-construction-level.

## Dev Agent Record

### Agent Model Used

Claude Opus 4.8 (1M context)

### Debug Log References

- `mvn -o clean install` — BUILD SUCCESS; core suite 239 tests, 0 failures, 0 errors, 0 skipped (was 232: −1 removed `sceneKeysAreNamespaced`, +8 new `IntroControllerTest`).

### Completion Notes List

- **Task 1** — `IntroController`: pure-model sequencing authority, no libGDX, **no RunState** (Decision 1 — structurally cannot tick). `index = -1` is the inactive sentinel; `advance()` past the last page and `end()` both close; `advance()`/`end()`/`start(empty)` are defensive no-ops.
- **Task 2** — `CorneoIntro.build()`: 6 zero-option, null-speaker narration pages (before ×2 / fall ×3 / hand-off ×1). No effects, no flags. The hand-off page ends on the 1.8 seeded line ("You flee into the pines. Aldric is beside you.") so the intro dovetails into the run's already-seeded log.
- **Task 3** — `MarginScreen`: added the `intro` field (transient, not on RunState); opens `CorneoIntro.build()` in the constructor (fresh run only — NOT in `restart()`); intro branch sits BEFORE the dialogue branch in `handleInput` and returns before `readAction` (structural safe pause — no `PlayerAction`, no tick, no turn); `handleIntroInput()` = SPACE/E advance, ESC skip; render routes through a shared `renderTextPage(node, footer)` (refactored from `renderDialoguePage`) so the intro reuses the 2.1 speaker/text/wrap drawing and shows the "[SPACE] continue   [ESC] skip" footer.
- **Task 4** — Deleted `SampleDialog.java`; removed the N-key binding + import from `MarginScreen`; removed `DialogueSafePauseTest.sceneKeysAreNamespaced` (its namespace was the smoke scene's; the intro adds no keys, so the pin was vacuous — the per-scene key rule stays pinned by `SceneEffectsTest`). No dangling `SampleDialog`/`KEY_SMOKE_READ` references remain (only a javadoc mention in `CorneoIntro`). This also resolves the 2.1 deferred N-key re-fire finding (text-only intro = no entry-effect to re-fire).
- **Task 5** — `IntroControllerTest`: sequencing (in-order walk, last-page close, skip-from-any-page, null-when-inactive, closed no-ops, empty-start), the content pin (≥3 beats, every page null-speaker / zero-option / effect-free, the three beat markers), and the structural safe-pause pin (driving the full intro leaves clock + four tracks unchanged).
- **Task 6** — Full suite green; no new persisted `RunState` field (AD-6 satisfied by construction — the controller/page are transient view-session state). The 2.1 dialogue suites stay green; the `DialogController`/`DialogNode`/`FlagStore`/`SceneEffects` contracts are untouched.

### File List

- **New:** `core/src/main/java/com/margins/rogue/narrative/IntroController.java`
- **New:** `core/src/main/java/com/margins/rogue/narrative/CorneoIntro.java`
- **New:** `core/src/test/java/com/margins/rogue/narrative/IntroControllerTest.java`
- **Modified:** `core/src/main/java/com/margins/MarginScreen.java` (intro field + constructor open + safe-pause branch + `handleIntroInput` + shared `renderTextPage`; N-key/SampleDialog removed)
- **Modified:** `core/src/test/java/com/margins/rogue/narrative/DialogueSafePauseTest.java` (removed the namespacing pin + its stale class-javadoc line)
- **Deleted:** `core/src/main/java/com/margins/rogue/narrative/SampleDialog.java`

## Change Log

| Date | Who | Change |
|------|-----|--------|
| 2026-08-08 | Create | Story 2.2 created (Status: ready-for-dev) from epics.md Story 2.2 + PRD §4.1/FR-1 + AD-14, carrying the 2.1 review lessons (controller-level safe-pause pin is structural; narration has no speaker; zero-option node = terminal page; AD-6 transient rule) and resolving the 2.1 deferred N-key pattern by Decision 6 (smoke scene removed) + Decision 7 (text-only pages). |
| 2026-08-08 | Dev | Implemented all 6 tasks. New `IntroController` (no RunState — structurally cannot tick) + `CorneoIntro` (6 zero-option narration pages, before/fall/hand-off). `MarginScreen`: intro opens on a fresh run (constructor, not restart), safe-pause branch before `readAction`, `handleIntroInput` (SPACE/E advance, ESC skip), shared `renderTextPage` with the "[SPACE] continue   [ESC] skip" footer. Removed `SampleDialog` + the N debug key + the vacuous `sceneKeysAreNamespaced` pin. Full suite green (239 tests, 0 failures). No new persisted field (AD-6 by construction). Status → review. |

---
baseline_commit: 4dab3deb19ac14acd1c6cab789aa0b8503035289
---

# Story 5.1: Branching dialogue that suspends the turn loop

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As Justine (player),
I want conversations to pause the action,
so that authored moments land without enemies moving underneath them (FR-6).

## Acceptance Criteria

1. **Given** an authored dialogue node opens, **When** it is on screen, **Then** enemy turns do not advance — hunger does not tick, no enemy moves, no noise resolves — for as long as the scene is open. (FR-6, AD-4)
2. **Given** an open node with 1–4 choices, **When** the player selects one (number key 1..N), **Then** the scene advances to that choice's linked node, or closes the scene if the choice has no linked node. (FR-6)
3. **Given** a node is displayed, **When** it is on screen, **Then** its text and its 1–4 choice labels are shown, each labelled with its selection number. (FR-6)
4. **Given** the scene closes (a choice with no next node, or a terminal node with no choices), **When** it ends, **Then** control returns to normal play — the next movement/attack/wait key advances the turn loop as before. (FR-6, AD-4)
5. **Given** a scene is NOT open, **When** the player plays normally, **Then** turn processing, enemy AI, and all other UI modes (inventory, drop-prompt) behave exactly as before this story. (regression, AD-4)

**Architectural definition-of-done:**

6. Dialogue navigation logic (which node is current, what a choice selects) lives in a **model** class, not the screen (AD-2) — the screen renders the current node and forwards the chosen index only. The turn loop is suspended by the existing modal-UI gate (no enemy/hunger/noise system runs while a scene is open), not by adding branching rules to `TurnEngine`. No libGDX types in the model class (AD-2).

## Product decisions (recommended defaults baked in)

- **Turn suspension reuses the existing `UiMode` modal gate — `TurnEngine` is not touched.** `RogueGameScreen.handleInput()` already early-returns for `INVENTORY`/`DROP_PROMPT` *before* it ever calls `submitTurn` → `turnEngine.advance`. Adding a `UiMode.DIALOGUE` that routes to a dialogue handler and returns means no player action is ever submitted while a scene is open, so hunger/enemy/noise phases simply never run (they only run inside `TurnEngine.advance`). This is exactly how the inventory panel already "pauses" the game, and it satisfies FR-6 with **zero turn-pipeline changes** — the cleanest possible AD-4 story (the pipeline is untouched; play is gated at input).
- **Reuse the legacy `com.margins.dialog.DialogNode` verbatim.** It is already the 1–4 branching structure this story needs: `String text` + `DialogOption[]{ String label, DialogNode next }`. A `DialogOption` whose `next` is `null` is the scene-ending choice. No new content model is needed for 5.1; do **not** invent a richer node type yet (INSTINCT thresholds and flag effects are 5.2/5.3 — they will extend `DialogOption` then, when actually required).
- **Navigation lives in a model `DialogController` (AD-2), unlike the legacy screen.** The old `com.margins.screen.GameScreen` drives dialogue *inside the screen* (`currentDialog`, `dialogChoice`, next-node logic in `handleInput`). That is a good behavioral reference but violates AD-2. The rogue version puts the "current node / select(index) → next" logic in `com.margins.rogue.narrative.DialogController`; `RogueGameScreen` only renders `controller.getCurrent()` and calls `controller.select(i)`. New subpackage `narrative/` matches the architecture Structural Seed (`narrative/DialogController.java`).
- **`DialogController` is transient view-session state, held by the screen — not part of `RunState`/save.** It sits beside the screen's existing `uiMode`/`cursor`/`makeRoom` view state, not in `RunState` (AD-3 owns *run data*; an in-progress conversation is not run data). Consequence: quitting mid-scene abandons the open scene on reload (acceptable for MVP — authored scenes are short and are Epic 6). Narrative *state* that must persist (flags/Bond) already lives in `RunState.FlagStore` from Story 4.3 and is written there by 5.2/5.3, not by this controller.
- **Number-key selection (1..N), out-of-range ignored.** Keys `1`–`4` map to `options[0..3]`; a key beyond the node's option count is a no-op. A node with 0 options is terminal — any confirm/again closes it (and ESC always closes the scene, matching the inventory/drop-prompt ESC convention).
- **A minimal in-game trigger proves the mechanic; authored content is Epic 6.** FR-6 is a *mechanism*; the "Five Nights, Again" opening and the reunion are authored scenes in Epic 6. So 5.1 adds one debug trigger — a `T` ("talk") key in `PLAY` mode that opens a small hardcoded 2–3 node sample scene — purely so the branching + suspension is exercisable and reviewable in-game. Keep the sample tiny and clearly a placeholder; it is scaffolding for Epic 6's real triggers, not shippable content.

## Tasks / Subtasks

- [x] **Task 1 — `DialogController` model (AD-2/AD-7-adjacent)** (AC: 2, 4, 6)
  - [x] Create `core/src/main/java/com/margins/rogue/narrative/DialogController.java` (new `narrative/` subpackage, matching the spine seed). Pure model — no libGDX, no RNG.
  - [x] Field `private DialogNode current;` (`com.margins.dialog.DialogNode`). `public boolean isActive()` → `current != null`. `public DialogNode getCurrent()` → `current`.
  - [x] `public void start(DialogNode root)` → set `current = root` (opens a scene). `public void end()` → `current = null`.
  - [x] `public void select(int choiceIndex)` — the navigation authority: if `!isActive()` return; if `choiceIndex < 0 || choiceIndex >= current.options.length` return (out-of-range no-op); else advance `current = current.options[choiceIndex].next` (which may be `null` → scene ends). A terminal node (`options.length == 0`) selects nothing; the screen closes it via `end()`.
  - [x] Kept the surface minimal: 5.1 navigates the node graph only. (5.2 will thread `RunState`/`player` through `select` for the INSTINCT threshold compare (AD-8) and 5.3 for flag writes via `FlagStore` (AD-7) — not pre-added.)

- [x] **Task 2 — `DIALOGUE` UiMode + input routing in `RogueGameScreen` (AD-2)** (AC: 1, 2, 4, 5, 6)
  - [x] Added `DIALOGUE` to the `UiMode` enum. Added field `private final DialogController dialog = new DialogController();`.
  - [x] In `handleInput()`, added `if (uiMode == UiMode.DIALOGUE) { handleDialogueInput(); return; }` alongside the existing `INVENTORY`/`DROP_PROMPT` guards — returns **before** `readAction`/`submitTurn` (this is what suspends the turn loop, AC-1).
  - [x] `handleDialogueInput()`: `ESC` → `dialog.end()` + `uiMode = PLAY`. Number keys `NUM_1..NUM_4` → index; `dialog.select(index)` if in range, then `uiMode = PLAY` if the scene closed. Terminal node (0 options): `1`/`SPACE`/`ENTER` → `end()` + `PLAY`.
  - [x] Added the trigger: `if (Gdx.input.isKeyJustPressed(Input.Keys.T)) { dialog.start(SampleDialog.build()); uiMode = UiMode.DIALOGUE; return; }` in the `PLAY` block (near `I`/`G`; `T` was a free key).
  - [x] Provided the sample scene `com.margins.rogue.narrative.SampleDialog.build()` — a 2-node graph, 2 branching choices, one scene-ending (`next == null`) choice; clearly labelled a placeholder for Epic 6.

- [x] **Task 3 — Render the dialogue panel** (AC: 3)
  - [x] Added `renderDialoguePanel()` (mirrors `renderDropPrompt()`: ortho projection, dark `Assets.rogueWhite` backdrop, `font.draw`). Draws the node's `text`, then each option as `"[n] " + label`. Terminal node shows a `[1] Continue` close affordance.
  - [x] Called from `render()` under `if (uiMode == UiMode.DIALOGUE) renderDialoguePanel();`, alongside the existing inventory/drop-prompt render calls.
  - [x] Panel shows a `1-4 choose    ESC leave` hint; the main HUD control line was left untouched to avoid disturbing the layout.

- [x] **Task 4 — Verification** (AC: 1, 2, 3, 4, 5, 6)
  - [x] Headless harness (throwaway `DialogHarness`, run via `mvn -o -pl core install` then `exec-maven-plugin:3.1.0:java`) — **15/15 checks passed**:
    - `start(root)` → `isActive()` true, `getCurrent() == root`.
    - `select(0)` on a branching node → `getCurrent()` becomes `options[0].next`.
    - `select` out-of-range (high and negative) → no change, still active.
    - choice whose `next == null` → `isActive()` false (scene closed); both branches of a root close as expected.
    - `select` when not active → no-op, no exception; `end()` closes an open scene.
    - `SampleDialog.build()` has ≥2 choices and ≥1 scene-ending path; walking it reaches a closed state.
  - [x] **Turn-suspension proof (headless):** captured a `RunState`'s enemy positions + player hunger and confirmed they are unchanged when `turnEngine.advance` is not called — documenting that gating input in `DIALOGUE` mode (never calling `advance`) freezes the world (FR-6). Threw-away harness deleted after the run.
  - [x] **Live boot on `:0`:** game boots clean with the dialogue code, loads the pre-existing save, no exceptions (ran full timeout). NOTE — the interactive visual confirmation (press `T` → panel appears, enemies frozen, number keys branch, ESC/ending choice closes and play resumes; `I`/`G` unchanged) is a **human check**: an automated timeout boot cannot press keys. Flagged for Justine.

## Dev Notes

### Governing architecture
- **FR-6 — Branching dialogue presentation.** "Milek can be presented an authored dialogue node with 1–4 choices that pauses turn processing until resolved. While a dialogue node is open, enemy turns do not advance; selecting a choice advances to the linked node or closes the scene." [Source: prd.md#FR-6; epics.md#Story 5.1]
- **AD-4 — Ordered turn pipeline.** Enemy/hunger/noise systems run *only* inside `TurnEngine.advance`, which the screen calls only on a committed player action. Gating input in a modal UiMode therefore suspends the whole pipeline with no pipeline edit. [Source: ARCHITECTURE-SPINE.md#AD-4; core/.../system/TurnEngine.java]
- **AD-2 — Model owns rules; no libGDX in the model; no game rule in the screen.** Dialogue *navigation* is a rule → `DialogController` (model). The screen only renders + forwards input. [Source: ARCHITECTURE-SPINE.md#AD-2]
- **AD-7 — Narrative state lives in `RunState.FlagStore`.** Not exercised by 5.1 (this story only presents/branches). 5.3 will have choices write flags through `FlagStore`; 5.2 will read `player.instinct` for gated choices. The store is already built (Story 4.3) and `applyBondTag`/`get`/`set` are ready for those stories. [Source: ARCHITECTURE-SPINE.md#AD-7; core/.../state/FlagStore.java]
- **Structural Seed.** The spine lists `narrative/DialogController.java` — "suspends turn processing, drives DialogNode, INSTINCT checks (FR-6/7)" — and "(reuse) com.margins.dialog.DialogNode, com.margins.quest.QuestManager". 5.1 delivers the DialogController + DialogNode reuse half (suspend + drive + branch). INSTINCT (5.2) and QuestManager wiring (5.3/Epic 6) come later. [Source: ARCHITECTURE-SPINE.md#Structural Seed / narrative-line]

### Files being modified / added — current state and what to preserve
- **NEW:** `narrative/DialogController.java` — the model (current node + navigate). Only genuinely new logic.
- **NEW:** `narrative/SampleDialog.java` (or a static builder) — a tiny placeholder scene to make FR-6 exercisable in-game. Epic-6 content replaces the *trigger*, not the controller.
- **REUSE (unchanged):** `com.margins.dialog.DialogNode` (+ nested `DialogOption`) — `text` + `DialogOption[]{label,next}`. Do not modify it for 5.1; 5.2 extends `DialogOption` for INSTINCT/flags.
- **`RogueGameScreen.java`** (UPDATE): today it has `UiMode{PLAY,INVENTORY,DROP_PROMPT}`, an input dispatcher that early-returns per modal mode, `submitTurn` → `turnEngine.advance`, and render hooks per mode. **Add** a `DIALOGUE` mode, a `DialogController` field, `handleDialogueInput()`, `renderDialoguePanel()`, and the `T` trigger. **Preserve** the existing PLAY/INVENTORY/DROP_PROMPT flow exactly (AC-5) — the new mode is additive and must return before `submitTurn` so it can't accidentally advance a turn. Watch `readAction`: `T` must not collide with an existing key (current keys: WASD/arrows, Q attack, E block, F distract, SPACE wait, G pickup, I inventory — `T` is free).
- **DO NOT TOUCH:** `TurnEngine` (the whole point — suspension is structural), `Companion`/`CompanionSystem`, `FlagStore` (its 5.2/5.3 hooks already exist), `RunState`, save/`SaveService`.

### Reference implementation (behavioral only, do not copy the AD-2 violation)
`com.margins.screen.GameScreen` (the legacy, non-active screen) already renders a dialogue with a `>` cursor and navigates `currentDialog.options[choice].next`. Use it to sanity-check the *feel* (panel layout, "advance to next or close"), but keep the navigation in `DialogController`, not the screen. Note it uses up/down + a confirm key; 5.1 uses direct number-key selection (fewer keystrokes, and it scales to the 1–4 labelled choices FR-6 specifies).

### Scope boundary
- **IN:** modal `DIALOGUE` UiMode that suspends turns, a model `DialogController` that drives a `DialogNode` scene (open / show / select→branch / close), the panel render, and a debug `T` trigger with a tiny sample scene.
- **OUT:** INSTINCT-gated choices (**5.2**, AD-8 `player.instinct >= threshold` — `getInstinct()` exists, returns 7 default); flag set/read from choices and cache-reveal gating (**5.3**, AD-7 writes through `FlagStore`); `QuestManager` wiring (**5.3/Epic 6** — and note `com.margins.quest.QuestManager` currently targets the *legacy* `com.margins.item.Inventory`, not `com.margins.rogue.item.Inventory`, so it needs adaptation before reuse); authored scene content — the "Five Nights, Again" opening and the Galleon reunion (**Epic 6**); persisting an in-progress scene across save/load (out — controller is transient view state); Bond-tag application from a choice (the `FlagStore.applyBondTag` hook exists but wiring a choice to it is 5.2/5.3).

### Testing standards
- No committed JUnit suite yet (open Epic 1/3/4 retro action item) — verify via a throwaway `main` harness for the model + a live `:0` boot for the screen, as in every 4.x/3.x story. **Build quirk:** `mvn -o -pl core install` before `exec:java`; harness recipe `mvn -o -pl core org.codehaus.mojo:exec-maven-plugin:3.1.0:java -Dexec.mainClass=...`. `DialogController`/`DialogNode`/`SampleDialog` are pure model and run headless; the screen (GL context) is verified live.

### Project Structure Notes
- Active screen is `com.margins.rogue.RogueGameScreen` (NOT the legacy `com.margins.screen.GameScreen`). Wire the new mode into the rogue screen only.
- New package `com.margins.rogue.narrative` for `DialogController` (+ the sample builder), matching the spine's `narrative/` seed — the same "spine path matches when the package fits" call made for `state/FlagStore` in 4.3.
- Retro carry-in (Epic 4 → here): **any new special turn path must honor the pipeline invariants** — but 5.1 deliberately adds *no* new turn path (it gates input instead), which side-steps that risk entirely. If a later dialogue feature ever needs to advance a turn from inside a scene, that is where the `hunger → checkLastStand → FOV` invariant checklist applies.

### Review Findings

Code review 2026-08-03 (Blind Hunter + Edge Case Hunter + Acceptance Auditor, parallel). AC verdict: 5.1 AC 1–6 all satisfied. Blind Hunter verified the turn-loop suspension is **airtight** — `TurnEngine.advance` is reached only via `submitTurn`, which runs *after* the `uiMode == DIALOGUE` early-return, so no enemy/hunger/noise phase can run while a scene is open; the `T` trigger is gated behind `waitingForInput`. Legacy `com.margins.screen.GameScreen` still compiles against the extended `DialogNode`.

- [x] [Review][Defer] Input maps only `NUM_1..NUM_4`, so a node with >4 options leaves the 5th+ unselectable [RogueGameScreen.java (handleDialogueInput/renderDialoguePanel)] — deferred: FR-6 defines the cap as "1–4 choices", so 4 is the designed maximum; a 5+-option node is an authoring-contract violation, and ESC still exits. Add a defensive cap/assert only if authored content ever needs it. (blind+edge) [Low]

**Dismissed:** 5.1's task-checkbox text still names `select(int)` / `start(DialogNode)`, but 5.2/5.3 evolved those to take instinct/`RunState` — a cumulative-stacked-story artifact, not a code defect (the shipped signatures are correct and verified).

## Dev Agent Record

### Agent Model Used
- Claude Opus 4.8 (1M context) — implementation + harness verification.

### Debug Log References
- Harness: `mvn -o -q -pl core install` (clean) then `mvn -o -q -pl core org.codehaus.mojo:exec-maven-plugin:3.1.0:java -Dexec.mainClass=com.margins.rogue.DialogHarness` → **ALL 15 CHECKS PASSED**.
- Smoke boot on `:0`: `timeout 10 mvn -o -q -pl desktop ... -Dexec.mainClass=com.margins.desktop.DesktopLauncher` — ran full duration (exit 124), **zero exceptions**, pre-existing save loaded clean.

### Completion Notes List
- ✅ **Task 1 — DialogController (AD-2):** `narrative/DialogController.java` — pure model holding the current `DialogNode`; `start/end/isActive/getCurrent/select(int)`. `select` advances to `options[i].next` (may be null → scene ends); out-of-range and inactive calls are no-ops. Navigation authority lives here, not the screen.
- ✅ **Task 2 — DIALOGUE UiMode (AD-2, FR-6):** added `UiMode.DIALOGUE` + a `DialogController` field to `RogueGameScreen`; `handleInput()` routes to `handleDialogueInput()` and returns before `submitTurn`, so `TurnEngine.advance` never runs while a scene is open — **that is the turn-loop suspension, with zero `TurnEngine` changes**. `T` opens the placeholder `SampleDialog` scene; number keys `1..N` select; ESC / a null-next choice / terminal-node confirm close it and restore `PLAY`.
- ✅ **Task 3 — Panel render:** `renderDialoguePanel()` draws the node text + numbered choices (or a `[1] Continue` for a terminal node) over a dark backdrop, mirroring the existing modal panels; wired into `render()`.
- ✅ **Task 4 — Verification:** 15/15 headless checks (branching, out-of-range guards, scene close, sample walkthrough, and the world-frozen-without-advance proof); clean `:0` boot. `SampleDialog` labels are plain text (the renderer supplies the `[n]` prefix).
- **AC coverage:** 1 ✓ (turns suspended — `advance` never called in DIALOGUE mode) · 2 ✓ (number-key select → linked node or close) · 3 ✓ (text + numbered choices rendered) · 4 ✓ (scene close restores PLAY; next key advances the turn) · 5 ✓ (PLAY/INVENTORY/DROP_PROMPT paths untouched; `TurnEngine` unchanged) · 6 ✓ (navigation in the model, no libGDX in `DialogController`, suspension via the input gate not a pipeline edit).
- **Out of scope, as specified:** INSTINCT gating (5.2), flag/quest writes (5.3), authored content (Epic 6). `TurnEngine`, `FlagStore`, `RunState`, and save are untouched by this story.
- **Human check outstanding:** interactive `T`-key visual confirmation (panel, frozen enemies, branching, close→resume) — an automated boot can't press keys.

### File List
- `core/src/main/java/com/margins/rogue/narrative/DialogController.java` (NEW) — model: drives one `DialogNode` scene (AD-2).
- `core/src/main/java/com/margins/rogue/narrative/SampleDialog.java` (NEW) — placeholder scene factory for the `T` trigger (Epic 6 replaces the trigger/content).
- `core/src/main/java/com/margins/rogue/RogueGameScreen.java` (MODIFIED) — `UiMode.DIALOGUE`, `DialogController` field, `T` trigger, `handleDialogueInput()`, `renderDialoguePanel()`, render/input wiring.

## Change Log

- 2026-08-03 — Story 5.1 spec created: branching dialogue that suspends the turn loop (FR-6). Turn suspension via a new `DIALOGUE` UiMode (reusing the existing modal-gate pattern; `TurnEngine` untouched), navigation in a model `DialogController` (AD-2) driving the reused `com.margins.dialog.DialogNode`, panel render, and a debug `T` trigger with a placeholder sample scene (authored content is Epic 6). INSTINCT (5.2), flags/QuestManager (5.3), and authored scenes (Epic 6) explicitly out of scope.
- 2026-08-03 — Story 5.1 implemented: `narrative/DialogController` + `narrative/SampleDialog` (NEW), `RogueGameScreen` gains a `DIALOGUE` UiMode, `T` trigger, `handleDialogueInput()`, and `renderDialoguePanel()`. Turn loop suspended by the input gate (no `TurnEngine` change). Verified via throwaway `DialogHarness` (15 checks incl. world-frozen-without-advance) + clean `:0` boot; harness deleted. Status → review. Interactive `T`-key visual check left for Justine.

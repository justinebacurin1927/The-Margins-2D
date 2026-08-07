# Story 2.1: Text-forward dialogue nodes with safe pause

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As Klein,
I want conversations to present a speaker line and numbered choices while the world holds still,
So that reading and choosing never costs me a turn (FR-19, AD-14).

## Acceptance Criteria

1. **Given** a dialogue node, **when** it is shown, **then** it renders a speaker line + up to N numbered choices in the text-forward surface, and the turn loop is suspended (no survival tick, no turn committed).
2. **Given** a choice, **when** I select it, **then** it can advance the node, set a `FlagStore` flag, fire an effect (Bond gain/loss, item give/take, disposition), or be gated by a stat — and the node closes cleanly back to gameplay.
3. **Given** VOICE- or INS-gated choices, **when** the gate is evaluated, **then** the choice routes to the appropriate success/failure branch by the gating stat (VOICE primary).

## In/Out of Scope Seam

**In scope:**
- The **dialogue surface in the text-forward presentation** (AC-1, AD-15, FR-19): while a scene is open, the bottom message log renders the dialogue page — speaker line, wrapped node text, numbered choices — and the turn loop is suspended (AD-14). This is the deliberate **log-window policy** (Decision 1) the epic-1 retro flagged.
- **Safe pause** (AD-14): while `DialogController.isActive()`, no `PlayerAction` is produced → `advance()` is never called → no survival tick, no turn committed. Structurally protected (Decision 6).
- The **stat-agnostic gate** (AC-3): `DialogOption` gates on VOICE or INSTINCT and routes to success/failure by the gating stat. Replaces the brownfield INSTINCT-only gate (FR-19: VOICE primary; `RoguePlayer.getVoice() = 3` exists).
- The **effect model** (AC-2): node-entry effects — set flag (existing `withFlag`), Bond gain/loss (`FlagStore.applyBondTag`), item give/take (`Inventory`), disposition (a `FlagStore` counter key) — **each emitting an SPD-tone observation to the log** (1.8 observation discipline; no silent mutation).
- **Authoring-contract hardening** (carry #5): >4-option nodes, null-label guard, non-1 flag values, per-scene key namespacing — each pinned in a test (Task 5).
- **A minimal smoke scene + debug trigger** so the mechanic is verifiable in-game; explicitly superseded by the real intro (2.2). This reconciles the dropped old-design `SampleDialog` trigger: the NEW design still needs a reachable verification seam for the mechanic (Task 4).
- **Brownfield migration:** `DialogControllerTest`'s five 5.2 INSTINCT-gate tests migrate to the new `GateStat` API; the 5.1/5.3 tests must keep passing unchanged.

**Out of scope (do NOT build):**
- The actual intro content / paged skip (Story 2.2), Aldric's diegetic tutorial (2.3), Aldric's capture + rescue seed (2.4), quest flags + passive Journal (2.5).
- A full NPC disposition system — in 2.1, disposition is a `FlagStore` counter key (`disposition.<npc>`) adjusted by an effect; the content that reads it is later stories / Epic 5.
- Persisting the dialogue surface/controller across save/load — transient view-session state (the brownfield controller's contract; AD-6).
- Any real-time animation (typewriter, auto-advance, timers) — the game is turn-based; the page is static.
- A minimap, inventory UI, or any chrome beyond the dialogue page (NFR-3). Auto-opening a scene at run start (2.2 owns that).

## Design Decisions (the interpretation calls)

1. **Log-window policy: the dialogue page REPLACES the event window while a scene is open.** The 1.8 log window is 5 lines; a speaker + N choices overflows it. Deliberate policy (the retro's "flag for Story 2.1's design, not a blocker"): while `dialog.isActive()`, `renderLog` shows the DIALOGUE PAGE (speaker + wrapped node text + all numbered choices), not the last-5 events; when the scene closes, the render reverts to the event window, which now includes the effect outcomes the scene appended. This honors AD-15 literally — "the bottom message log IS the text-forward surface dialogue renders in" (epic-1 retro) — and adds no new chrome (NFR-3). The 5-line window is the event-history default, not a hard cap on an open scene.

2. **DialogController is the second core writer of the message log.** `RunState.appendMessages` stays the sole mutator (its comment says "called by TurnEngine — AD-4", `RunState.java:315`); this story ADDS `DialogController` as a caller — dialogue effects are NOT turns, so AD-4's single acted branch is untouched. `select()` executes the entered node's effects, collects their messages, and appends them to the log itself; the screen does nothing but forward the index. This keeps AD-1 (the screen stays pure presentation) and makes the 1.8 observation discipline structural.

3. **The gate is stat-agnostic.** `DialogOption`'s INSTINCT-only `instinctThreshold` is replaced by a `(GateStat, threshold)` gate — `GateStat { INSTINCT, VOICE }` (new, in `com.margins.dialog`), `-1`/absent = ungated. `DialogController.select` resolves `stat == VOICE ? player.getVoice() : player.getInstinct()` and routes `>= threshold` → success `next`, below → `failNext` (the brownfield's deterministic threshold compare is preserved — no dice). One gate path, no two-field drift. FR-19: VOICE primary.

4. **Effects are node-entry effect descriptors, executed by the controller — matching the `withFlag` precedent.** A node carries a small effect list; `DialogController.enter` executes each. The descriptors are pure content-model (in `com.margins.dialog` — like `setFlagKey` today), so the content package stays dependency-clean; the EXECUTION lives in `com.margins.rogue.narrative` (the controller, which already imports rogue state). Kinds: SET_FLAG, BOND (a `FlagStore` tag → `applyBondTag`), GIVE_ITEM (type, count → `Inventory.tryAdd`), TAKE_ITEM (type, count → `Inventory.remove`), DISPOSITION (npc key, delta → `FlagStore.add("disposition." + key, delta)`). `withFlag(key, value)` keeps working as the SET_FLAG convenience (may delegate to the effect list internally). Every kind emits one SPD-tone line ("He warms to you." / "He hands you a Coal." / "No room in your pack." / "His eyes narrow." etc.) — no silent mutation (Decision 2).

5. **Disposition is a `FlagStore` counter, not a system.** `disposition.<npc>` adjusted via `FlagStore.add` — the same store as every other narrative flag (AD-7). No new RunState field, no new class beyond the effect kind. The content that consumes it is Epic 5.

6. **Safe pause is structural, not a flag.** While `dialog.isActive()`, `MarginScreen.handleInput()` returns `null` for ALL gameplay keys (WASD/Q/G/SPACE/C/B/T/K/F/V/E/TAB/[ ]) — no `PlayerAction`, so `TurnEngine.advance` never runs. Only choice keys route: `NUM_1..NUM_N` → `dialog.select(i-1, state)`, SPACE/E on a terminal node (zero options) → `dialog.end()`, ESC → `dialog.end()` (cancel). R (restart) also closes any open scene. No pipeline surgery — AD-4's single acted branch is untouched.

7. **A node is terminal when it has zero options.** A choice whose target is `null` closes the scene immediately (`enter(null)` → `current = null`, the brownfield behavior). A non-null node with zero options renders as a terminal text page closed by SPACE/E ("[SPACE] continue"). Both are "clean closes" (AC-2).

8. **The smoke scene is a verification seam, not content.** A hardened tour scene (narrator root with a nullable speaker, a VOICE-gated choice AND an INSTINCT-gated choice, a flagged node with a NON-1 value, a Bond effect, an item-give effect, a disposition effect) behind a free debug key (N suggested — key audit: WASD/UP/DOWN/LEFT/RIGHT/Q/G/SPACE/C/B/T/K/F/V/E/TAB/[ ]/R are bound; free: H/I/J/L/M/N/O/P/U/X/Y/Z). It exists so the mechanic is verifiable in-game and superseded by 2.2's intro. Do NOT wire it to run-start.

## Tasks / Subtasks

- [ ] **Task 1 — The stat-agnostic gate (AC: 3)**
  - [ ] New `com.margins.dialog.GateStat` enum (`INSTINCT`, `VOICE`).
  - [ ] `DialogOption`: replace `instinctThreshold`/the gated ctor with `(GateStat gatedStat, int gateThreshold)` — `-1` = ungated; `isGated()` = `gatedStat != null`. One gate path, no legacy field. Keep the ungated `(label, next)` ctor.
  - [ ] `DialogController.select`: resolve the gate by stat — `VOICE → player.getVoice()`, `INSTINCT → player.getInstinct()`; `>= threshold` → success `next`, below → `failNext` (brownfield semantics preserved).
  - [ ] Migrate `DialogControllerTest`'s five 5.2 gate tests to the new ctor (`GateStat.INSTINCT, 5` etc.). 5.1/5.3 tests pass unchanged.

- [ ] **Task 2 — The effect model (AC: 2)**
  - [ ] `DialogNode`: an effect list (`withEffect(...)` builder); keep `withFlag(key, value)` working as the SET_FLAG convenience.
  - [ ] Effect kinds (com.margins.dialog descriptors + controller execution): SET_FLAG, BOND (tag → `applyBondTag`), GIVE_ITEM (type, count → `Inventory.tryAdd`), TAKE_ITEM (type, count → `Inventory.remove`), DISPOSITION (npc key, delta → `FlagStore.add("disposition."+key, delta)`).
  - [ ] `DialogController.enter`: after the node's flag write, execute each effect, collect its SPD-tone line, then `state.appendMessages(lines)` when non-empty (Decision 2 — every effect emits; no silent mutation).
  - [ ] Tests: each effect kind mutates the right store AND its line lands in `getMessageLog()`; `GIVE_ITEM` with a full pack emits "No room in your pack." and adds nothing; `BOND` via `BOND_TAG_HONEST` → +1 / `BOND_TAG_DISMISSIVE` → −1.

- [ ] **Task 3 — The dialogue surface + safe pause (AC: 1)**
  - [ ] `DialogNode`: `public String speaker` (nullable — narration has no speaker; AC-1 "speaker line").
  - [ ] `MarginScreen`: `private final DialogController dialog = new DialogController();` — transient view-session state, NOT on RunState (Decision 4, AD-6).
  - [ ] Safe-pause routing (Decision 6): while `dialog.isActive()`, all gameplay keys return `null`; `NUM_1..NUM_N` → `dialog.select(i-1, state)`; SPACE/E on a terminal node → `dialog.end()`; ESC → `dialog.end()`; R closes the scene before restarting.
  - [ ] Render (Decision 1): while `dialog.isActive()`, `renderLog` draws the dialogue page — speaker (nullable → narration, no prefix), wrapped node text, numbered choices "1. label" — instead of the event window; when closed, revert to the last-5 event lines. No new chrome (NFR-3).
  - [ ] The screen forwards indices and renders `getCurrent()` — it never mutates dialogue state itself (AD-1).

- [ ] **Task 4 — The smoke scene + debug trigger (verification seam, superseded by 2.2)**
  - [ ] Harden `SampleDialog.build()` (or a renamed equivalent) into the 2.1 smoke scene: narrator root (nullable speaker) with 2-4 options — one VOICE-gated + one INSTINCT-gated (both AC-3 branches), a closing choice; a flagged node with a NON-1 value; a Bond effect; an item-give effect; a disposition effect — a tour of AC-2's effect types.
  - [ ] A free, non-conflicting debug key (N suggested) opens it, clearly commented as a verification seam. Do NOT wire it to run-start.

- [ ] **Task 5 — Tests: authoring-contract hardening + safe-pause pin (carry #5, AD-14)**
  - [ ] >4-option nodes: a node with 5-6 options navigates fine through the controller (the old engine capped at 4).
  - [ ] null option label: the controller never dereferences labels (navigation is by index) — a null label does not throw; the screen renders it defensively.
  - [ ] non-1 flag value: `withFlag(key, 7)` writes 7 through to `FlagStore.get(key) == 7`; effects never assume a value of 1 (`!= 0` is the truth test — `FlagStore.get` returns 0 for absent).
  - [ ] key namespacing: the smoke scene's keys are unique/namespaced (the SceneEffects `KEY_CACHE_*` single-authority pattern) — no collision between two scenes' keys.
  - [ ] Safe-pause pin (AD-14): drive a full scene through the controller and assert `getClockTurns()` and the four tracks are UNCHANGED — dialogue ticks nothing (mirror the AD-5 honesty pins).
  - [ ] Observation pin: after `select()`, every effect's line IS in `getMessageLog()`.

- [ ] **Task 6 — Tests, full suite, serialization check (AC: all)**
  - [ ] All core seams headless, no libGDX types (AD-2): `DialogueGateTest`, `DialogueEffectTest`, `DialogueSafePauseTest` (in `core/src/test/java/com/margins/rogue/narrative/`).
  - [ ] Serialization: 2.1 adds NO new persisted field to RunState (Decision 4/5 — the controller/surface are transient; effects flow through existing persisted stores). Pin a save round-trip leaves the dialogue closed and `FlagStore`/`Inventory` round-trip unchanged (AD-6 rule satisfied by construction).
  - [ ] Full suite green (`mvn -o clean install`), no regressions in the 205 existing tests (especially `DialogControllerTest`, `SceneEffectsTest`, `RunStatePersistenceTest`).

## Dev Notes

### Current state (what exists, to preserve)

- **The brownfield dialogue engine (Epic 5-3 era) is nearly complete and test-covered — Story 2.1 RATIFIES it, it does not rebuild it:**
  - `com.margins.dialog.DialogNode` (+ nested `DialogOption`): `text` + `options[]` + `setFlagKey/setFlagValue` (`withFlag(key, value)` — written on node ENTRY), `instinctThreshold` gate (`-1` = ungated, `>=` = success, `failNext` on fail). Read: `core/src/main/java/com/margins/dialog/DialogNode.java`.
  - `com.margins.rogue.narrative.DialogController`: `start/end/select/getCurrent/isActive`; `select` routes gated options by `getInstinct() >= threshold`; `enter` writes the node flag via `FlagStore` (AD-7). Pure model, no libGDX. Held by the screen as transient view-session state (its javadoc says "beside `uiMode`" — **there is no `uiMode` in the new MarginScreen; this story introduces the surface**). Read: `DialogController.java`.
  - `com.margins.rogue.narrative.SceneEffects`: `KEY_CACHE_REVEALED` / `KEY_CACHE_SPAWNED` + `applyCacheReveal` — the single authority for scene keys (AD-7). **Untouched** (its tests pin it).
  - `com.margins.rogue.narrative.SampleDialog`: a placeholder scavenger scene opened by the OLD debug T key. The new MarginScreen bound T to `craftTorch` (Story 1.6) — the debug trigger is GONE. Task 4 re-purposes it as the 2.1 smoke scene behind a new free key.
  - `com.margins.rogue.state.FlagStore`: `get` (0 for never-set — callers never null-check), `set`, `add`, `getBond/adjustBond/getBondTier`, `applyBondTag` (`BOND_TAG_HONEST` → +1, `BOND_TAG_DISMISSIVE` → −1). Read: `FlagStore.java`.
  - Tests: `DialogControllerTest` (10 — 5.1 navigation ×3, 5.2 INSTINCT gate ×4, 5.3 flag write ×2), `SceneEffectsTest` (3) — committed and green.
- **The screen (`MarginScreen.java`) has NO dialogue wiring.** `handleInput()` returns a `PlayerAction` or `null` (the selection cycle TAB/[/] returns null). Bound keys: WASD/UP/DOWN/LEFT/RIGHT/Q/G/SPACE/C/B/T/K/F/V/E/TAB/RIGHT_BRACKET/LEFT_BRACKET/R. `renderLog` draws the last 5 lines of `state.getMessageLog()` (`LOG_LINES = 5`, AD-15).
- **RunState API available to effects:** `getPlayer()`, `getInventory()` (`tryAdd`/`remove`/`backpackType`…), `getFlagStore()`, `addFloorItem(type,count,x,y)`, `appendMessages(List<String>)` (the log's sole mutator — comment: "called by TurnEngine — AD-4", `RunState.java:315`), `getMessageLog()`, `getClockTurns()`, `restart()`.
- **RoguePlayer:** `getInstinct()` = 7, `getVoice()` = 3. VOICE is FR-19's primary gate stat; the brownfield gate only implements INSTINCT.
- **The 1.8 seed** ("You flee into the pines. Aldric is beside you.") is the 2.2 intro's hand-off hook — 2.1 does NOT auto-open a scene at run start.

### Carried lessons (epic-1 retro action #3 + carry #5 + AD-6)

- **Observation discipline (1.8 lesson, retro action #3):** dialogue effects (Bond, flags, item give/take, disposition) are NEW mutation paths — the exact silent-mutation bug class the 1.8 review found (three separate silent tier-change routes). Every effect MUST emit an SPD-tone line to the log (Decision 2 — the controller collects and appends), and the observation pins in Task 5 prove it. No effect mutates silently.
- **Log-window policy (retro flagged):** the 5-line window overflows with a speaker + choices. The deliberate policy is Decision 1: while a scene is open the log render shows the dialogue page; the event window resumes (with the effect outcomes) after close. Do NOT pour the node text into the 5-line scrollback and do NOT build a separate panel/chrome.
- **Dialogue authoring-contract hardening (carry #5 — the old engine's deferred low findings):**
  1. **>4-option nodes** must work (the old engine capped at 4). The controller navigates any count; the surface renders all. Task 5 pins >4. (Surface note: number keys are single digits — authoring should keep N ≤ 9; the controller itself is unbounded.)
  2. **null labels** must not crash (old engine NPE'd on a null label). The controller ignores labels (navigation is by index); the screen renders defensively. Task 5 pins.
  3. **non-1 flag values (!= 0)** must work. `withFlag(key, 7)` and effects must never assume 1; `FlagStore.get()` returns 0 for absent, so `!= 0` is the truth test. Task 5 pins a non-1 write.
  4. **multi-cache key collisions:** scene keys must be namespaced per scene and single-authority (the SceneEffects `KEY_CACHE_*` pattern). Task 5 pins the smoke scene's keys are unique.
- **AD-6 migration rule (newly codified — retro action #4):** a field-absent save inherits ctor-rolled nondeterministic state — every new persisted field needs a deterministic default OR a load-time reconcile in `restoreAfterLoad()`. Story 2.1 adds NO new persisted field (Decisions 4/5 — the controller/surface are transient; effects flow through existing persisted stores). Satisfied by construction; Task 6 pins the round-trip.

### Placement rationale (AD-1/AD-2/AD-4/AD-7/AD-14/AD-15)

- The gate + effect descriptors live in the content model (`com.margins.dialog` — pure, no rogue deps beyond what `DialogNode` already implies); the EXECUTION lives in the controller (`com.margins.rogue.narrative`) — AD-2 (no libGDX in core), AD-1 (rules in core, the screen renders).
- `DialogController` is the navigation + effect authority (mirrors `TurnEngine` for turns): `select` resolves the gate and fires effects; it appends effect messages via `RunState.appendMessages` — the log's second core writer, explicitly NOT an AD-4 change (dialogue is a suspended-turn surface, not the acted branch).
- Narrative state (flags, Bond, disposition) is written ONLY through `FlagStore` (AD-7) — effects never touch RunState fields directly beyond the log.
- Safe pause is structural (AD-14): while `dialog.isActive()`, `handleInput()` returns no `PlayerAction`, so `advance()` is never called — no survival tick, no turn committed. The AD-4 single acted branch is untouched.
- The surface renders the core node (speaker/text/options) and forwards indices — pure presentation (AD-1). The dialogue page replaces the event window while open (AD-15 — the bottom log IS the text-forward surface, epic-1 retro).

### Serialization — what NOT to do

- The controller + surface are TRANSIENT view-session state: NOT on RunState, NOT in the libGDX Json graph. Do NOT serialize the scene, the current node, or any dialog state. An in-progress scene does not survive save/load (a reloaded run starts with no scene — 2.2's intro re-triggers by run start). All effects write to EXISTING persisted stores (FlagStore keys, Inventory counts) — those round-trip as today. No new persisted field is added (AD-6 rule checked).

### Scope discipline (CLAUDE.md §2/§3)

- Touch only: `com/margins/dialog/DialogNode.java` (speaker, `GateStat` gate, effect list), new `com/margins/dialog/GateStat.java`, new effect descriptors (`com/margins/dialog`), `com/margins/rogue/narrative/DialogController.java` (gate resolution + effect execution + log append), `com/margins/rogue/narrative/SampleDialog.java` (smoke scene), `core/src/main/java/com/margins/MarginScreen.java` (dialog field, safe-pause routing, dialogue-page render, debug key), and the tests (`DialogControllerTest` migration + the three new suites).
- Do NOT build: the intro/paging (2.2), Aldric's tutorial (2.3), capture/rescue (2.4), quest flags/Journal (2.5), a full NPC disposition system, any real-time animation, any chrome beyond the dialogue page. Do NOT serialize the dialog state. Do NOT auto-open a scene at run start.
- Keep `SceneEffects` and its `KEY_CACHE_*` contract untouched (its tests pin it). Keep `FlagStore`'s API as-is — the Bond tags exist, reuse `applyBondTag`; do not add new Bond machinery.
- If a message's wording needs tuning, keep the SPD tone and change the string — no message configurability.

### Testing standards

- Headless JUnit 5, no libGDX (AD-2). Seeded `RunState(seed)` — a fresh run's player has INSTINCT 7 / VOICE 3 (pin gates at those numbers: VOICE 2 passes / 4 fails; INSTINCT 7 boundary passes, 9 fails).
- The safe-pause pin (AD-14) mirrors the survival-clock honesty pins (AD-5): drive a full scene through the controller and assert `getClockTurns()` and the four tracks are UNCHANGED.
- Observation pins: after `select()`, the effect's line IS in `getMessageLog()` (the 1.8 `MessageLogTest` assertion style) — no silent mutation.
- Authoring-contract pins: >4 options, null label, non-1 flag, key uniqueness — each one focused test (Task 5).
- The controller's `select(int, RunState)` is the headless seam — the screen work (Task 3-4) stays thin glue over core-tested behavior.

### Project Structure Notes

- New (production): `core/src/main/java/com/margins/dialog/GateStat.java` (enum INSTINCT/VOICE); effect descriptors in `com/margins/dialog` (records or a Kind-class — matching the `withFlag` descriptor precedent; keep them pure, no rogue deps).
- New (tests): `core/src/test/java/com/margins/rogue/narrative/DialogueGateTest.java`, `DialogueEffectTest.java`, `DialogueSafePauseTest.java` (per-feature suite naming — the `DialogControllerTest`/`SceneEffectsTest` precedent).
- Modified: `DialogNode.java`, `DialogController.java`, `SampleDialog.java`, `MarginScreen.java`, `DialogControllerTest.java` (5.2 migration only).
- Naming: dialogue effect execution lives in the controller (`com.margins.rogue.narrative`); the `System`-suffix pipeline convention (TurnEngine/HungerSystem…) is untouched — the controller is not a System.

### References

- [Source: epics.md#Story-2.1 (lines 320-338)] — the three ACs verbatim: speaker + up to N numbered choices in the text-forward surface with the turn loop suspended; choices advance / set a flag / fire effects (Bond, item give/take, disposition) / gate by a stat and close cleanly; VOICE- or INS-gated choices route by the gating stat (VOICE primary).
- [Source: prd.md#FR-19 (lines 295-299)] — "Dialog is text-forward (speaker line, numbered choices, bottom log)"; "Dialogue suspends the turn loop (safe pause); a node = speaker text + up to N choices; a choice can advance, set a flag, fire an effect (Bond gain/loss, item give/take, disposition), or be gated"; "Primary gate stat: VOICE…; occasional INS gates." Also prd.md line 239 — "VOICE can de-escalate a wary patrol" (the gate's use case).
- [Source: architecture spine AD-14, AD-15, AD-4/AD-5, AD-7] — safe pause (no survival tick, no turn committed); the bottom message log as the primary text-forward surface; the single acted branch (dialogue is a suspended surface, not a branch) + turn honesty; FlagStore as the narrative-state authority.
- [Source: epic-1 retro 2026-08-08 — action #3, carry #5, action #4] — observation discipline (every dialogue effect emits to the log or capture/compares); the authoring-contract hardening list (>4-option nodes, null labels, non-1 flag values, multi-cache key collisions); the AD-6 migration rule (deterministic default OR `restoreAfterLoad()` reconcile).
- [Source: story-1.8-the-survival-hud-and-message-log.md] — the message log seam (`RunState.appendMessages`/`getMessageLog`), the single-writer comment (`RunState.java:315`), the SPD message register, the seeded opening line, and the retro's "log-window concern" flag.
- [Source: brownfield engine files] — `DialogNode.java`, `DialogController.java`, `SceneEffects.java`, `SampleDialog.java`, `FlagStore.java`, `DialogControllerTest.java`, `SceneEffectsTest.java` (contracts in "Current state" above).

## Dev Agent Record

### Agent Model Used

(Dev fills at implementation time.)

### Debug Log References

(Dev fills at implementation time.)

### Completion Notes List

(Dev fills at implementation time.)

### File List

**New (production):**
- `core/src/main/java/com/margins/dialog/GateStat.java` — the gate-stat enum (INSTINCT/VOICE).
- Effect descriptors in `com/margins/dialog` — the five kinds (SET_FLAG, BOND, GIVE_ITEM, TAKE_ITEM, DISPOSITION).

**New (tests):**
- `core/src/test/java/com/margins/rogue/narrative/DialogueGateTest.java`
- `core/src/test/java/com/margins/rogue/narrative/DialogueEffectTest.java`
- `core/src/test/java/com/margins/rogue/narrative/DialogueSafePauseTest.java`

**Modified (production):**
- `core/src/main/java/com/margins/dialog/DialogNode.java` — `speaker`, `GateStat` gate, effect list.
- `core/src/main/java/com/margins/rogue/narrative/DialogController.java` — stat-agnostic gate resolution, effect execution, log append.
- `core/src/main/java/com/margins/rogue/narrative/SampleDialog.java` — the 2.1 smoke scene.
- `core/src/main/java/com/margins/MarginScreen.java` — dialog field, safe-pause routing, dialogue-page render, debug key.

**Modified (tests):**
- `core/src/test/java/com/margins/rogue/narrative/DialogControllerTest.java` — 5.2 gate tests migrate to `GateStat`; 5.1/5.3 unchanged.

## Change Log

| Date | Who | Change |
|------|-----|--------|
| 2026-08-08 | Create | Story 2.1 created (Status: ready-for-dev) with the carried epic-1 retro lessons — observation discipline, log-window policy, authoring-contract hardening (carry #5), AD-6 rule — and the brownfield dialogue engine (DialogNode/DialogController/SceneEffects/FlagStore) ratified rather than rebuilt. |

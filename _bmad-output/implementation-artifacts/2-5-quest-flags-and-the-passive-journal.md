---
baseline_commit: c02770e54eb7fafb3735ec29f8da7bfd785a975e
---

# Story 2.5: Quest flags and the passive Journal

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As Klein,
I want quests to start from NPCs or discoveries and be looked up in a Journal,
so that the story can gate content without a rigid quest UI (FR-19).

## Acceptance Criteria

- **AC-1** — Given an NPC line or a discovery (Journal Note / item), when it triggers a quest, then the quest auto-starts by setting `FlagStore` quest state; killing a quest-giver voids that quest (FR-19).
- **AC-2** — Given active/known quests, when I open the Journal, then it is a passive lookup of quest state — not a delivery mechanism — reading current `FlagStore` state (FR-19).

## In/Out of Scope Seam

**In scope:**
- A **core-owned `JournalController`** (`rogue/narrative`) — the quest registry (authored `QuestDefinition`s) + the **passive status derivation** (AC-2: reads `FlagStore` only, holds NO quest-state copy) + the transient surface lifecycle (`open/close/isActive`). Pure model, no libGDX (AD-2), headless-testable; the screen's glue is thin (Decision 7).
- **Quest state lives in `FlagStore`** (AD-7): the namespaced family `quest.<id>.started` / `quest.<id>.completed` / `quest.<id>.voided` + the giver-death flag `npc.<giver>.dead` — with single-authority key helpers on the Journal (Decision 2). No new persisted `RunState` field (AD-6 by construction).
- **The discovery trigger (AC-1, production):** the 2.4 Torn Page first-read branch extends to auto-start the rescue thread ("The Road East") when `aldric.captured != 0` — consuming BOTH 2.4 seeds (the flag as the precondition, the note as the player-facing trigger). Reading still commits NO turn and the note stays (Decision 3).
- **The NPC-line trigger (AC-1, mechanism):** an authored `DialogNode` carrying the existing `SetFlag` effect (`withFlag(JournalController.startedKey(id), 1)`) auto-starts a quest. Zero new dialogue machinery — Story 2.1 already fires node-entry flags through `FlagStore`; 2.5 makes the Journal READ them (Decision 4).
- **The void-on-kill rule (AC-1, mechanism + test pin):** the derivation voids an active quest when its giver's `npc.<giver>.dead` flag is set. Nothing sets that flag yet (Epic 2 has no named, killable NPC — `RogueEnemy` is anonymous); the combat hook that sets it lands when named NPCs exist. Pinned headlessly (Decision 5).
- **The passive Journal surface (AC-2):** J opens it, J/ESC closes it, while open it is a safe pause (AD-14 — the architecture names the "quest log" as a suspended text surface: no turn, no survival tick). It renders the derived quest list (title + status + objective) in the text-forward surface, replacing the event window (log-window policy extended, Decision 7).
- **Headless tests** for the whole mechanism: discovery trigger, passive derivation, NPC-line trigger, void rule, completed status, unstarted-unlisted, safe pause, AD-6.

**Out of scope (do NOT build):**
- **The act-gating quests themselves** ("Follow the Road", "The Rescue") — Epic 5.6 owns their completion and the Act gates. 2.5 tracks the rescue thread's START ("The Road East"); its Journal entry shows ACTIVE until Epic 5 flips completion. The Journal's completed-status derivation is built now so Epic 5's completion lands for free (Decision 6).
- **Named quest-giver NPCs / a combat kill hook** — the world and NPCs are Epic 3; combat death routing is Epic 4. The void rule reads the death flag; the rule is real and test-pinned, the killer is deferred (Decision 5, mirroring 2.4's narrated chasers).
- **A rigid quest taxonomy / any delivery mechanism** — FR-19 + PRD §5: quests are freeform and source-driven, the Journal is passive lookup only. No quest progress can be advanced through the Journal (AC-2 is explicit).
- **Any change to the dialogue engine** — the NPC-line trigger reuses `SetFlag`; no new `DialogEffect` kind, no `DialogController` change.
- **New persisted `RunState` fields** — none (Decision 1/2: the Journal is transient; quest state rides `flagStore`).
- **A second production quest** — the catalog holds exactly "The Road East". The NPC-line and void rules are proven with synthetic quests registered in tests, so no ghost quest appears in the shipped Journal (Decision 6).

## Design Decisions (the interpretation calls)

1. **The Journal is a transient core controller; quest state is FlagStore.** `JournalController` mirrors `IntroController`/`DialogController` — a screen-held, transient, headless-testable model (AD-1/AD-2). The PERSISTED quest state is purely FlagStore keys (AD-7). **The status derivation is the single authority and holds NO quest-state copy** — `entries(state)` recomputes every time from the store, so the Journal is literally a passive lookup (AC-2) and can never drift from the flags.

2. **Quest state is a namespaced flag family with single-authority key helpers.** `quest.<id>.started` (auto-started by a trigger — AC-1), `quest.<id>.completed` (flipped by the act-gating stories, Epic 5), `quest.<id>.voided` (scripted void), and `npc.<giver>.dead` (giver killed). Static key helpers (`startedKey(id)` etc.) live on the Journal as the single authority — the `SceneEffects` `KEY_CACHE_*` pattern — so a trigger (note read, dialogue effect, future combat) never hand-builds a key string. Status precedence: **VOIDED (either flag) → COMPLETED → ACTIVE (started) → unlisted** (not started quests do not appear).

3. **The discovery trigger is the Torn Page first read, gated on `aldric.captured`.** The 2.4 note is FR-19's "Journal Note or item". Extending the 2.4 `TurnEngine` read branch: on the REVEAL read (`!wasIdentified`), when `flagStore.get(KEY_ALDRIC_CAPTURED) != 0`, set `quest.roadeast.started = 1` and append one SPD observation line. The capture flag is the explicit precondition (the quest can't exist before Aldric is taken — this is how 2.5 "consumes the `aldric.captured` flag" and answers 2.4-review N2); the note is the player-facing trigger. Reading stays no-turn and the note stays in the pack (the 2.4 contract is untouched). A second read is still the "You've read the note." no-op — no re-start, no re-announce.

4. **The NPC-line trigger reuses the dialogue `SetFlag` effect — zero new dialogue machinery.** Story 2.1 already fires node-entry flags through `FlagStore` (`DialogNode.withFlag(key, value)` → `DialogEffect.SetFlag` → `DialogController.enter` → `flagStore.set`). An NPC line that starts a quest is exactly `withFlag(JournalController.startedKey("someQuest"), 1)` on an authored node. 2.5 adds no effect kind and touches no dialogue code; the Journal's derivation is the reader. Proven headlessly by driving a real `DialogController` scene with such a node.

5. **Void-on-giver-death is mechanism + test pin; the killer is deferred.** A quest definition may carry a `giver` (an NPC key; null = discovery-triggered). The derivation voids an active quest when `npc.<giver>.dead != 0`. No named NPC exists in Epic 2, so nothing writes that flag in production yet — the rule is pinned by test (write the flag → VOIDED) and the seam documented for the Epic 3 world / Epic 4 combat stories, exactly as 2.4 narrated its chasers rather than simulating them. The `quest.<id>.voided` flag covers scripted voids independently.

6. **The production catalog is exactly one quest: "The Road East"** (id `roadeast`), the rescue-seed thread the 2.4 note opens — title "The Road East", objective "Aldric was taken — prisoners to the road-head, east along the Copper Road. Follow the road east.", giver null (discovery-triggered). Epic 5.6's act-gating ("Follow the Road", "The Rescue") consumes this thread: it will flip `quest.roadeast.completed` (or its own act flags) when the gates resolve — the Journal derivation already renders COMPLETED. Do NOT author "Follow the Road"/"The Rescue" here. The NPC-line and void rules are proven with synthetic quests registered via `register(...)` in tests, never shipped.

7. **The Journal surface is a passive page in the text-forward surface.** J opens (a free key — key audit below), J/ESC closes. While active, `handleInput` swallows every gameplay key and returns — no `PlayerAction`, so no turn and no survival tick (AD-14 safe pause, structural like 2.1). The page renders `journal.entries(state)` — title, status, objective — in the bottom-log region, replacing the event window while open (AD-15 log-window policy, the 2.1 Decision-1 pattern extended to the quest log). No choices, no advancement — pure lookup (AC-2).

8. **Status precedence is fixed and deterministic.** For a registered quest: `quest.<id>.voided` → **VOIDED**; else `npc.<giver>.dead` (when it has a giver) → **VOIDED**; else `quest.<id>.completed` → **COMPLETED**; else `quest.<id>.started` → **ACTIVE**; else **unlisted** (not known). The Journal never shows a quest the player hasn't started (passive lookup of *known* state — AC-2), so "The Road East" appears only after the note is read.

## Tasks / Subtasks

- [ ] **Task 1 — `JournalController` + `QuestDefinition` + the passive derivation (AC: 1, 2)**
  - [ ] New `com.margins.rogue.narrative.JournalController` — pure model, no libGDX (AD-2), transient (held by the screen, Decision 1).
  - [ ] Quest-id + key authority (Decision 2): `public static final String QUEST_ROAD_EAST = "roadeast";` and static `startedKey(String id)` = `"quest." + id + ".started"`, `completedKey(id)` = `"quest." + id + ".completed"`, `voidedKey(id)` = `"quest." + id + ".voided"`, `giverDeadKey(String giver)` = `"npc." + giver + ".dead"`. Single authority — no hand-built quest keys anywhere else.
  - [ ] `QuestDefinition` (nested record or small model): `id`, `title`, `objective`, `giver` (nullable — null = discovery-triggered, no void-on-kill check).
  - [ ] `JournalEntry` (nested record or small model): `id`, `title`, `objective`, `QuestStatus status` (`enum QuestStatus { ACTIVE, COMPLETED, VOIDED }`).
  - [ ] Registry + surface lifecycle: `register(QuestDefinition)` (tests add synthetic quests; the ctor seeds the one production quest — Decision 6), `open()`, `close()`, `isActive()`.
  - [ ] `List<JournalEntry> entries(RunState state)` — the passive derivation (Decision 8): recompute from `state.getFlagStore()` every call, hold no quest-state copy (Decision 1). Unstarted quests are not listed.
  - [ ] The production catalog entry: `new QuestDefinition(QUEST_ROAD_EAST, "The Road East", "Aldric was taken — prisoners to the road-head, east along the Copper Road. Follow the road east.", null)`.

- [ ] **Task 2 — The discovery trigger: the Torn Page read starts the quest (AC: 1)**
  - [ ] `TurnEngine` case USE, the 2.4 note-read branch: on the REVEAL read (`!wasIdentified && id != null && id.loreLine() != null`), after the lore line, add — when `state.getFlagStore().get(FlagStore.KEY_ALDRIC_CAPTURED) != 0` and the started flag is not already set — `state.getFlagStore().set(JournalController.startedKey(JournalController.QUEST_ROAD_EAST), 1)` and append one SPD observation line (e.g. `"You mark the road ahead — a new thread in the Journal."`; tune phrasing in review).
  - [ ] The read STAYS no-turn and the note STAYS in the pack (the 2.4 contract is untouched); the second-read "You've read the note." no-op is unchanged (no re-start, no re-announce — the flag is already set and the branch is the reveal-only path).

- [ ] **Task 3 — The passive Journal surface (AC: 2)**
  - [ ] `MarginScreen`: `private final JournalController journal = new JournalController();` — transient view-session state, NOT on `RunState` (Decision 1, AD-6).
  - [ ] Safe-pause routing in `handleInput` (after the menu-open block — so the menu and the Journal are mutually exclusive: menu-open swallows J, journal-active swallows M and ESC-for-menu): while `journal.isActive()`, J or ESC → `journal.close()`, all other keys swallowed (return) — no `PlayerAction`, no turn, no tick (AD-14). The J-open check follows it: `if (down(Input.Keys.J)) { journal.open(); return; }`. J is free (key audit below).
  - [ ] Render (Decision 7): in `renderHud`'s text-surface chain, `else if (journal.isActive()) renderJournalPage();` between the dialogue branch and the event-window fallback. `renderJournalPage()` draws the entry list (title + status + objective from `journal.entries(state)`) in the bottom-log region with a `"[J] close"` footer — no choices, no advancement, no new chrome (NFR-3).
  - [ ] `restart()`: add `journal.close();` beside the existing `dialog.end(); intro.end(); tutorial.skip();` — a new life starts with no open surface.

- [ ] **Task 4 — Tests: the mechanism and the AC pins (AC: all)**
  - [ ] New `core/src/test/java/com/margins/rogue/narrative/JournalControllerTest.java`:
    - **Discovery trigger (AC-1):** capture via `CaptureController` (or set the flag directly), plant + pick up the `TORN_PAGE` note, USE it through `TurnEngine` → `startedKey("roadeast")` is 1, the log has the observation line, and `journal.entries(state)` lists "The Road East" ACTIVE with the objective; the read committed NO turn (clock unchanged) and the note stays in the pack.
    - **The capture-flag precondition (2.4-flag consumption):** the note read with `aldric.captured` absent (0) does NOT start the quest (flag stays 0, `entries()` empty).
    - **Reveal-only:** a second read is the "You've read the note." no-op — no duplicate observation line, flag still 1.
    - **Passive derivation (AC-2, Decision 1):** a synthetic registered quest — start → ACTIVE; then set `completedKey` → COMPLETED; then set `voidedKey` → VOIDED. The Journal stores nothing: flipping the flag changes the derived view.
    - **NPC-line trigger (AC-1):** a synthetic giver-quest started by a REAL `DialogController` scene — a `DialogNode` carrying `withFlag(JournalController.startedKey("giverQuest"), 1)`; selecting the node's option fires it → `entries(state)` lists the quest ACTIVE. (The SetFlag path already exists; this pins the Journal reads it.)
    - **Void-on-giver-death (AC-1):** start a synthetic quest with a `giver`; set `npc.<giver>.dead` → `entries(state)` shows VOIDED. Also pin the `voidedKey` scripted void.
    - **Unstarted-unlisted (AC-2, Decision 8):** a registered quest with no flags does not appear in `entries()`.
    - **Safe pause (AD-14):** `open()`/`close()` change no run state — `getClockTurns()` and the four tracks unchanged (mirror the AD-5 honesty pins; the screen's swallow is the enforcing branch, the controller's contract is that opening/closing never mutates the run).
  - [ ] **AD-6 pin:** the Journal adds NO new persisted `RunState` field (transient; quest state rides `flagStore`) — assert a save/load round-trip keeps the started flag (the persistence suite stays green untouched).

- [ ] **Task 5 — Full suite, no regressions (AC: all)**
  - [ ] The 2.1/2.2/2.3/2.4 suites stay green (`DialogControllerTest`, `DialogueGateTest`, `DialogueEffectTest`, `DialogueSafePauseTest`, `IntroControllerTest`, `TutorialControllerTest`, `CaptureControllerTest`).
  - [ ] `mvn -o clean install` — full suite green, no regressions in the existing 288 tests.
  - [ ] Serialization: 2.5 adds NO new persisted `RunState` field (Decision 1 — the Journal is transient; quest state = FlagStore keys). Run the persistence round-trip + AD-5 placement suites (both must stay green — the note branch adds no RNG draw).
  - [ ] Launch: `mvn -o -q -pl core install` + `timeout 40 mvn -o -pl desktop exec:java` — boot clean (the new J key + render branch cause no exceptions).

## Dev Notes

### Current state (what exists, to preserve)

- **The 2.4 seeds (this story's trigger sources):** `FlagStore.KEY_ALDRIC_CAPTURED = "aldric.captured"` (set once by `CaptureController.resolve`); `Supply.TORN_PAGE` + `TrueIdentity.CHASERS_ORDER` (the note's lore line `"Chaser's order: '…prisoners to the road-head, east along the Copper Road.'"`); the `TurnEngine` note-read branch (`case USE`, before the generic mystery reveal — first read reveals the lore + `markIdentified`, later reads "You've read the note.", both NO turn, note never consumed). Read: `core/src/main/java/com/margins/rogue/system/TurnEngine.java:96-104`, `core/src/main/java/com/margins/rogue/item/Supply.java:53`, `TrueIdentity.java` (CHASERS_ORDER), `CaptureController.java`. 2.4-review N2 explicitly hands the Journal this job: "the 2.5 Journal story should derive capture state from the persisted flag instead" of `CaptureController.isResolved()`.
- **The dialogue foundation (2.1) — the NPC-line trigger needs NO change:** `DialogNode.withFlag(key, value)` → `DialogEffect.SetFlag` → `DialogController.enter` → `state.getFlagStore().set(key, value)` (silent scene bookkeeping by design — the node text is the observation). `DialogController` is transient, screen-held. Read: `core/src/main/java/com/margins/dialog/DialogNode.java`, `core/src/main/java/com/margins/dialog/DialogEffect.java`, `core/src/main/java/com/margins/rogue/narrative/DialogController.java`.
- **The screen surface patterns (2.1/2.2/2.4):** `MarginScreen` holds transient controllers (`intro`, `tutorial`, `capture`, `dialog`); `handleInput` routes intro → tutorial.begin → dialog-safe-pause → menu → `readAction`; `renderHud`'s text-surface chain is `intro.isActive() → renderTextPage` / `dialog.isActive() → renderTextPage` / else `renderMessagePanel`. `restart()` closes open surfaces (`dialog.end(); intro.end(); tutorial.skip();`). Read: `core/src/main/java/com/margins/MarginScreen.java` (fields ~116-126, handleInput ~299-363, renderHud ~978-1004, restart ~427-429, renderTextPage ~1409).
- **FlagStore (AD-7):** `get` (0 for never-set), `set`, `add`; narrative keys `KEY_BOND`, `KEY_ALDRIC_CAPTURED`. `restart()` resets `flagStore` (narrative state is run-scoped). Read: `core/src/main/java/com/margins/rogue/state/FlagStore.java`.
- **Key audit (bound today):** WASD/UP/DOWN/LEFT/RIGHT, Q (attack), G (pickup), SPACE (wait), C (collect), B (campfire), T (torch), K (cook), F (filter), V (boil), E (use/read), TAB/] / [ (selection), R (restart), M/ESC (menu), NUM_1..9/SPACE/E/ESC (dialogue). **Free: H, I, J, L, N, O, P, U, X, Y, Z — J is the Journal key.** (2.1's N smoke-scene debug key was removed by 2.2.)
- **Named entities:** `RogueEnemy` is anonymous (no key/name) — there is no quest-giver to kill in Epic 2; the void rule is mechanism + test pin (Decision 5).

### Carried lessons (2.1/2.2/2.3/2.4 review findings, applied)

- **Transient controller, persisted consequences (2.3/2.4 AD-6 pattern).** `JournalController` is transient view-session state like `IntroController`/`DialogController`/`CaptureController`; quest state rides the existing `flagStore` map. No new persisted `RunState` field (AD-6 by construction).
- **FlagStore is the narrative-state authority (AD-7).** Quest state is FlagStore keys, namespaced with single-authority key helpers (the `SceneEffects` `KEY_CACHE_*` pattern). No ad-hoc booleans, no Journal-held quest-state copy — the Journal is a pure reader (AC-2).
- **Safe pause is structural, not a flag (2.1 Decision 6).** While `journal.isActive()`, `handleInput` returns before any `PlayerAction` — no turn, no tick (AD-14 names the "quest log" as a suspended text surface). The enforcing branch is the screen's; the controller's contract is that open/close mutate nothing.
- **Observation discipline (1.8/2.1/2.4).** The quest-start is a mutation — it MUST emit an SPD-tone log line at the moment it fires (Task 2). A SetFlag-triggered quest is observed through the node text (2.1's ratify-silent carve-out for SET_FLAG). The Journal itself is a lookup surface — it announces nothing, it only renders.
- **AD-5 / seeded-stream discipline (1.x H1-review).** The note-read branch's quest-start adds NO RNG draw and NO turn — the AD-5 placement and AD-5 honesty pins must stay green untouched.
- **AD-6 ordinal rule / no new persisted fields (2.4).** No enum append, no `RunState` field. Quest ids are string keys in an existing store — nothing ordinal to migrate.
- **Append-only keys, `!= 0` truth (2.1 authoring contract).** Quest-state flags are `!= 0` truth (a `withFlag(key, 2)` starts the quest); never assume a value of 1.
- **The killer is deferred (2.4 Decision 4 mirror).** Named NPCs and the combat kill hook are Epic 3/4; the void rule reads `npc.<giver>.dead` now and is test-pinned, the writer lands later — documented, not built (no ghost NPC, no speculative CombatSystem hook).

### Placement rationale (AD-1/AD-2/AD-4/AD-7/AD-14/AD-15)

- `JournalController` lives in the CORE (`rogue/narrative`), pure model, no libGDX (AD-2), headless-testable — mirroring `IntroController`/`DialogController`. The screen opens/closes it and renders `entries(state)`; it never derives or mutates quest state itself (AD-1).
- It is NOT a `System` (the `System`-suffix convention is the turn pipeline) and NOT a `PlayerAction.Kind` — opening the Journal is a safe-pause surface, not a turn (AD-14). The AD-4 single acted branch is untouched.
- Quest state is FlagStore (AD-7) — the same store as every other narrative flag; the act-gating stories (Epic 5) read/write these same keys.
- The Journal's delivery is the text-forward surface (AD-15): the page replaces the event window while open (log-window policy, 2.1 Decision 1) — no new chrome (NFR-3).
- The discovery trigger lives in the existing `TurnEngine` note-read branch (the 2.4 seam) — a small extension, not a new controller; reading stays a no-turn narration (AD-5).

### Serialization — what NOT to do

- `JournalController`, its registry, and its open/closed state are TRANSIENT view-session state: NOT on `RunState`, NOT in the libGDX Json graph. Do NOT serialize the quest catalog, the open surface, or any derived view. The PERSISTED quest state is exactly `flagStore` keys (`quest.<id>.started/completed/voided`, `npc.<giver>.dead`) — a save after the note read round-trips with `quest.roadeast.started` set, and the Journal derives the same view on load.
- Do NOT add a new persisted `RunState` field (no `journal`, no `quests`, no `questState` map — the generic `flagStore` map already carries it).
- No enum append in this story (no new Supply/TrueIdentity). Quest ids are plain string keys.
- Future save/load (deferred O6) notes: the Journal surface is transient, so a reloaded run starts with it closed — the quest state itself is persisted in the flags and re-derives on load. Record in the save/load story; do not build the mechanism now.

### Scope discipline (CLAUDE.md §2/§3)

- Touch only: new `core/src/main/java/com/margins/rogue/narrative/JournalController.java`, new `core/src/test/java/com/margins/rogue/narrative/JournalControllerTest.java`, and small edits to `TurnEngine.java` (the note-read branch's quest-start), `MarginScreen.java` (journal field + safe-pause routing + J-open + `renderJournalPage` + restart close).
- Do NOT build: the act-gating quests (Epic 5.6), named NPCs / a combat kill hook (Epic 3/4), a rigid quest taxonomy or any Journal delivery/advancement, a second production quest, any change to the dialogue engine (`DialogController`/`DialogNode`/`DialogEffect` are untouched), any new persisted `RunState` field, any chrome beyond the journal page.
- Do NOT touch `FlagStore`'s API (the key helpers live on the Journal, the single quest authority) or the AD-4 pipeline order.

### Testing standards

- Headless JUnit 5, no libGDX (AD-2). Drive `new RunState(seed)` (seed 42), the real `TurnEngine.advance` for the discovery trigger (capture → plant/pickup the note → USE), and a real `DialogController` scene for the NPC-line trigger.
- The derivation pins mutate `flagStore` keys directly and assert `entries(state)` changes — no Journal-state tampering (the Journal holds none, Decision 1).
- The safe-pause pin mirrors the AD-5 honesty pins: `open()`/`close()` leave `getClockTurns()` and the four tracks unchanged.
- The AD-6 pin: the persistence round-trip + AD-5 placement suites stay green untouched; assert the started flag round-trips.
- Content pin: the "The Road East" entry's title/objective/status render from `entries(state)` — grep/assert the authored strings (AC-2).

### Project Structure Notes

- New (production): `core/src/main/java/com/margins/rogue/narrative/JournalController.java` (quest registry + passive derivation + surface lifecycle; authored quest + key authority live with it).
- New (tests): `core/src/test/java/com/margins/rogue/narrative/JournalControllerTest.java`.
- Modified: `TurnEngine.java` (note-read branch quest-start), `MarginScreen.java` (journal field + safe-pause routing + J key + `renderJournalPage` + restart close).
- Naming: `JournalController` follows the `DialogController`/`IntroController`/`TutorialController`/`CaptureController` precedent (a surface/event controller, NOT a `System`). Quest ids are kebab-case strings (`"roadeast"`); the flag family is `quest.<id>.<state>` / `npc.<giver>.dead` (the `disposition.<npc>` precedent).

### References

- [Source: epics.md#Story-2.5 (lines 392-407)] — the two ACs verbatim: an NPC line or a discovery (Journal Note / item) triggers → the quest auto-starts by setting `FlagStore` quest state, killing a quest-giver voids that quest; active/known quests → the Journal is a passive lookup of quest state — not a delivery mechanism — reading current `FlagStore` state. (FR-19.)
- [Source: prd.md#FR-19 (line 300)] — "Quests are NPC-given or discovery-triggered (a Journal Note or item auto-starts); killing a quest-giver voids the quest; the Journal is passive lookup, not a delivery mechanism." Also prd.md §5 line 344 — "No rigid quest taxonomy — quests are freeform and source-driven, not a Fetch/Kill framework."
- [Source: architecture AD-7] — all run-scoped narrative state (flags, quest state, act progression, Bond) lives in `FlagStore`; act-gating quests flip flags here. **[Source: AD-14]** — "any open text surface (intro, dialogue tree, quest log) commits no turn and ticks no survival clock" — the Journal is the quest log. **[Source: AD-15 / AD-1 / AD-2]** — the bottom log is the primary text surface (the journal page renders here); core owns the rule, the screen is thin glue; no libGDX in core.
- [Source: story-2.4 (this epic's preceding story, Status: done)] — the seeds this story consumes: `KEY_ALDRIC_CAPTURED` + the Torn Page note (the "exact hook Story 2.5's discovery-triggered quest machinery builds on"), the no-turn note-read branch, and review N2's hand-off ("the 2.5 Journal story should derive capture state from the persisted flag"). 2.4 Decision 4 (narrate, don't simulate) is the model for 2.5's deferred killer (Decision 5).
- [Source: story-2.1 (dialogue foundation, Status: done)] — the `DialogNode.withFlag` → `SetFlag` → `FlagStore` path the NPC-line trigger reuses; the safe-pause routing, log-window policy (Decision 1), and the SET_FLAG-ratify-silent carve-out (a SetFlag-triggered quest is observed via the node text).
- [Source: deferred-work.md O6] — the Journal surface is transient (no save/load UI yet); why the surface + serialization notes are documented, not built.

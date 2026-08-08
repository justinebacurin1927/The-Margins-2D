---
baseline_commit: 6d76b98c05e5bb6e7a3f16b072b2ab79acd6de74
---

# Story 2.4: Aldric's capture and the rescue seed

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As Klein,
I want Aldric taken the moment I've learned the ropes,
so that I feel the first loss and inherit the rescue thread (FR-3).

## Acceptance Criteria

- **AC-1** — Given the tutorial is complete, when the chasers catch up, then Aldric leaves by **capture** (a `FlagStore` flag, recoverable later), **not death**, and Klein escapes **alone** (FR-3).
- **AC-2** — Given the capture, when it resolves, then the message log **and** a discovery seed establish Aldric is held **east** along the **Copper Road**, opening Act 1's wound and UJ-3's east-pull (FR-3).

## In/Out of Scope Seam

**In scope:**
- A **core-owned one-shot `CaptureController`** (`rogue/narrative`) that resolves the capture when the tutorial completes — the documented Story 2.4 seam `TutorialController.isComplete()` (2.3 Decision 7). Pure model, no libGDX (AD-2), headless-testable; the screen's glue is thin (Decision 1/2).
- **The capture's persisted consequences** (all in EXISTING stores — AD-6 by construction): a new `FlagStore` key (`KEY_ALDRIC_CAPTURED`) set to 1; Aldric **removed from the party** (a new small `RunState` method, Decision 3); "Klein escapes alone" falls out — `getActiveCompanion()` returns null, so `CompanionSystem.follow` and the DISTRACT action no-op with the existing "No companion to call on." line. No death, no HP change, no corpse.
- **The message-log beat** (AC-2, AD-15): the chase → take → alone lines, SPD text-forward tone, one log line each (log-window policy). Authored as content in the controller (Decision 5).
- **The discovery seed** (AC-2): a new **lore-note `Supply` + `TrueIdentity`** (appended enum ordinals, single identity — no RNG draw, AD-5) planted as a **floor item at Aldric's last tile**, whose first **read** (USE) reveals the east/Copper-Road lore line. It stays in the backpack as a carried seed — the exact hook Story 2.5's discovery-triggered quest machinery builds on.
- **Headless tests** for the whole beat, plus an AD-6 pin that the capture adds no new persisted `RunState` field.

**Out of scope:**
- **Actual chaser enemies / occupation escalation** — the chasers are NARRATED, not simulated (Decision 4). Spawning alerted pursuers that mechanically catch Aldric is Epic 4-3 ("occupation-escalation-thickens-per-act") + Epic 5's rescue-combat; this story ships the beat and the seed.
- **The quest/Journal machinery** (Story 2.5) — the `aldric.captured` flag + the carried note are the seeds 2.5 hooks; 2.5 builds discovery-triggered quests and the passive Journal lookup. Do not build any quest state machine here.
- **The rescue quest itself** (Epic 5, "The Rescue") — 2.4 only opens the thread.
- **Any change to the AD-4 turn pipeline / `TurnEngine.advance` order** — the capture is a screen-triggered event that resolves AFTER a committed turn (the same acted turn that completed the tutorial), not a turn, not a `System`, not a new `PlayerAction.Kind`. The only `TurnEngine` touch is the note's read branch (Task 3), which mirrors the existing inert-USE path.
- **Aldric dying / a capture-during-combat resolution** — if the party is somehow already empty when the capture would fire, it no-ops (guard, Decision 2); combat death stays death (the capture is a scripted opening beat, not a combat outcome).
- **New persisted `RunState` fields** — none (Decision 6); the flag, the removal, and the floor note all serialize through existing stores.

## Design Decisions (the interpretation calls)

1. **The trigger is the tutorial's completion, resolved on the same acted turn.** The screen calls `capture.resolve(state)` every frame once `tutorial.isComplete()` is true (the guard makes it a safe every-frame call, exactly the 2.3 `begin()` pattern). The frame the tutorial's last control is performed, the capture resolves — dramatically immediate: Aldric's closing line "we're not clear yet" is followed on the same turn by the hoofbeats. The capture commits NO extra turn (it observes the turn that completed the tutorial and mutates after it).

2. **One-shot + empty-party guard.** `resolve(RunState)` fires at most once (a `resolved` flag — second calls are a no-op), and no-ops entirely if `getActiveCompanion()` is null (nothing to capture — protects a hypothetical already-alone state and makes the controller safe to call every frame). No arm/begin/skip lifecycle needed: completion is a stable latch, so the resolve gate is just the internal guard.

3. **The capture is a first-life beat; a restarted life keeps Aldric.** The 2.3 precedent (Decision 6 + review M1): `restart()` calls `tutorial.skip()` → `isComplete()` false → the capture never fires on the new life, and the controller is not re-armed. A death-and-restart rewinds the flight; the player keeps Aldric and no second capture plays (the rescue thread is a first-life wound). The persisted capture flag lives in `flagStore`, which `restart()` resets (AD-7) — so a restarted life is internally consistent: Aldric back, flag cleared, no beat. *(Chosen call — the PRD/epics do not cover restart; this mirrors the 2.3 restart decision and keeps the roguelike loop honest.)*

4. **The chasers are narrated, not simulated.** "Chasers catch up" is delivered as the in-fiction log beat (Decision 5), like the Fall of Corneo is narrated in the 2.2 intro. Simulating them (spawned ALERTED enemies, a chase, a capture-instead-of-death combat resolution) is real occupation escalation — Epic 4-3's scope. The AC's observable ("Aldric leaves by capture, not death; Klein escapes alone") is fully met without it. *(Chosen with the 2.3 hide decision's logic: reuse shipped systems, no new mechanic.)*

5. **The capture's log beat** (authored, one line each, Aldric/SPD voice — the log is the sole text surface, AD-15):
   - `"A horn cuts the pines — hoofbeats on the road. Chasers."` (the chase is heard)
   - `"They take Aldric — he goes down fighting and is dragged off."` (capture, not death)
   - `"Klein is alone in the pines."` (escapes alone)
   - `"A torn page lies where Aldric was taken."` (the discovery seed is announced, placed at his last tile)
   Four lines + the tutorial's closing line = 5, inside the log window. The east/Copper-Road establishment lives on the note's read (Task 3), so the beat stays terse and AC-2's "message log AND a discovery seed" both carry it.

6. **AD-6 by construction: no new persisted `RunState` field.** The capture's state — the flag (flagStore), the removal (companions list), the note (floorItems) — all serialize through existing stores. `CaptureController` is transient view-session state (like `TutorialController`/`IntroController`). The only new persisted content is one `FlagStore` key (a generic map) and the note's floor-item entry. A save mid-flight round-trips exactly as before.

7. **The discovery seed is a read-able lore note, not a quest item yet.** New `Supply.TORN_PAGE("Torn Page")` (APPENDED at the enum's end — ordinal stability, the `IdentifyMap.reconcile` growth path already handles a shorter old binding) bound to a new single-identity `TrueIdentity.CHASERS_ORDER("Chaser's order")` with an inert `apply`. Single identity ⇒ `IdentifyMap.build` binds it deterministically with no RNG draw (H1-review lesson: only ambiguous types draw; AD-5 preserved). Reading it costs NO turn and does NOT consume it (mirrors the inert-USE precedent — reading is narration, and the note stays in the backpack as the permanent seed 2.5 references). The lore line is authored on the identity and appended on first read (Task 3).

8. **`CaptureController` is a narrative-event controller, not an observer.** The 2.3 observation discipline says coaches write only log lines. The capture is the opposite in kind: it is a scripted story EVENT that legitimately mutates the run (that is FR-3's job). The discipline holds in spirit — every mutation is announced in the log beat, nothing is silent — and the distinction is documented so review reads the controller's mutations as intentional, not a discipline violation.

## Tasks / Subtasks

- [x] **Task 1 — The persisted capture state (AC: 1, 2)**
  - [x] `FlagStore`: add `public static final String KEY_ALDRIC_CAPTURED = "aldric.captured";` — the rescue thread's run-scoped signal, alongside `KEY_BOND` (the `flagStore` map already serializes; AD-6). A capture sets it to 1; nothing resets it within a run (only `restart()`'s fresh store clears it, AD-7).
  - [x] `RunState`: add a small removal method, e.g. `public void removeActiveCompanion()` — removes the single party slot (AD-10: at most one companion in MVP). Mirror the existing `spawnStartingCompanion` (the list is the party; `getActiveCompanion()` nulls when empty). Do NOT add a "captured" companion state/marker — removal IS the capture (the flag records the fact).

- [x] **Task 2 — `CaptureController` (core, narrative) (AC: 1, 2)**
  - [x] New `com.margins.rogue.narrative.CaptureController` — pure model, no libGDX (AD-2). Transient; the only field is the one-shot `resolved` boolean (Decision 2).
  - [x] `resolve(RunState)`: if `resolved` or `state.getActiveCompanion() == null`, return. Otherwise, once:
    1. Read Aldric's tile (before removal) for the note's placement.
    2. `state.getFlagStore().set(FlagStore.KEY_ALDRIC_CAPTURED, 1)` — the persisted capture (AC-1, recoverable later).
    3. `state.removeActiveCompanion()` — Klein escapes alone (AC-1); follow/distract no-op from here on (their null-checks already exist).
    4. `state.addFloorItem(Supply.TORN_PAGE.ordinal(), 1, aldricX, aldricY)` — the discovery seed (AC-2).
    5. `state.appendMessages(...)` the four Decision-5 lines in order.
  - [x] Author the four lines as content constants in the controller (SPD voice, one line each).

- [x] **Task 3 — The discovery note: Supply + TrueIdentity + the read branch (AC: 2)**
  - [x] `Supply`: append `TORN_PAGE("Torn Page", TrueIdentity.CHASERS_ORDER)` at the ENUM'S END (ordinal stability / AD-6 appended-ordinal migration). Single possible identity.
  - [x] `TrueIdentity`: append `CHASERS_ORDER("Chaser's order")` with an inert `apply` (it is narration, not an effect) and an authored lore line the read appends — e.g. `"'…prisoners to the road-head, east along the Copper Road.'"` (the east/Copper-Road establishment; tune the phrasing in review).
  - [x] `TurnEngine` USE path: add a small branch BEFORE the generic mystery-supply reveal — when the used item is `Supply.TORN_PAGE`: if not yet identified, append the note's lore line (`displayName + ": " + identity.loreLine()`, first-read reveal, and `markIdentified` so the whole type is known — FR-12); later reads append a one-line no-op ("You've read the note.") and commit NO turn — mirroring the inert-USE precedent (no `isConsumedOnUse`, no inventory change; the note stays as the 2.5 seed). Do NOT touch the `"Milek can't read it."` inert branch (pre-existing old-game line, not this story's mess).

- [x] **Task 4 — Wire the capture into the screen (AC: 1, 2)**
  - [x] `MarginScreen`: `private final CaptureController capture = new CaptureController();` — transient view-session state, NOT on `RunState` (AD-6, Decision 6).
  - [x] Resolve seam (Decision 1): next to the tutorial observe-seam (after `turnEngine.advance` commits a turn and `tutorial.onAction` runs), call `if (tutorial.isComplete()) capture.resolve(state);` — the guard makes it a safe every-frame call; it fires on the turn that completes the tutorial and never again.
  - [x] `restart()`: NO touch (Decision 3) — the capture is not re-armed; `tutorial.skip()` already prevents the seam from firing on the new life. Document that in a one-line comment.
  - [x] No new render surface — the beat is ordinary log lines (AD-15); the note renders via the existing floor-item draw.

- [x] **Task 5 — Tests: the beat, the guard, the seed, AD-6 (AC: all)**
  - [x] New `core/src/test/java/com/margins/rogue/narrative/CaptureControllerTest.java`:
    - `resolve` sets `flagStore.get(KEY_ALDRIC_CAPTURED) == 1`, empties the party (`getActiveCompanion() == null`), plants the `TORN_PAGE` floor item at Aldric's tile, and appends the four lines in order.
    - **One-shot pin** (Decision 2): a second `resolve` is a no-op — flag still 1, no extra floor item, no duplicate log lines.
    - **Empty-party guard**: `resolve` on a state with no companion is a no-op (nothing captured, no lines).
    - **Alone pin** (AC-1): after resolve, `CompanionSystem.follow` and the DISTRACT action no-op with the existing refusal line ("No companion to call on.") — the capture never leaves a dangling actor.
    - **No death pin** (AC-1): after resolve, Aldric is GONE from the party (removed), not a corpse — assert `getCompanions().isEmpty()` and no enemy/companion remains at his tile.
    - **Note read pin** (AC-2): picking up the note and reading it (USE via `TurnEngine`) appends the east/Copper-Road lore line, identifies the type, and costs NO turn; a second read is a no-op with no turn.
  - [x] **Content pin**: the four capture lines + the note's lore line cover the AC-2 establishment — grep the authored strings for east/Copper-Road coverage (or assert the lore line text).

- [x] **Task 6 — Full suite, no regressions (AC: all)**
  - [x] The 2.1/2.2/2.3 suites stay green (`DialogControllerTest`, `DialogueGateTest`, `DialogueEffectTest`, `DialogueSafePauseTest`, `IntroControllerTest`, `TutorialControllerTest`).
  - [x] `mvn -o clean install` — full suite green, no regressions in the existing 281 tests.
  - [x] Serialization: 2.4 adds NO new persisted `RunState` field (Decision 6 — the controller is transient; the flag + note ride existing stores). Run the persistence round-trip suite (it must stay green; the companion list and floorItems already round-trip).
  - [x] Note for the save/load story (deferred O6): a mid-flight save loads with the tutorial reset to not-complete, so the capture would not fire on a loaded run until re-completed — and a post-capture save loads with the flag set and Aldric gone. Document in the story's Serialization section, do not build the mechanism now.

## Dev Notes

### Current state (what exists, to preserve)

- **The 2.3 seam:** `MarginScreen` holds a transient `TutorialController tutorial`; `tutorial.begin(state)` fires when the intro closes, `tutorial.onAction(action, state)` observes committed turns (gated on `advanceAnimated` — review H1), and `isComplete()` goes true the turn the last of the six controls is performed. `restart()` calls `tutorial.skip()` (completion is per-life). Read: `core/src/main/java/com/margins/MarginScreen.java` (fields ~117-121, intro branch ~307, observe seam ~351, restart ~418), `core/src/main/java/com/margins/rogue/narrative/TutorialController.java`.
- **The party (AD-10):** `RunState.companions` is a single-slot list (spawned by `spawnStartingCompanion`); `getActiveCompanion()` returns `companions.isEmpty() ? null : companions.get(0)`. `CompanionSystem.follow` and `CompanionSystem.distract` already null-check the active companion — removing Aldric needs NO change there (distract says "No companion to call on."). Read: `core/src/main/java/com/margins/rogue/state/RunState.java` (companion lifecycle ~239-281), `core/src/main/java/com/margins/rogue/system/CompanionSystem.java`.
- **Narrative state (AD-7):** `RunState.getFlagStore()` returns the run-scoped `FlagStore` (a serialized string→int map + Bond). Adding `KEY_ALDRIC_CAPTURED` is a new KEY in an existing store — no structural change. `restart()` resets `flagStore` (AD-7: narrative state is run-scoped). Read: `core/src/main/java/com/margins/rogue/state/FlagStore.java`.
- **Floor items (FR-10):** `RunState.addFloorItem(int type, int count, int x, int y)` + `takeItemAt(x, y)`; the pickup path announces "Picked up <name>." Floor items serialize with the run. Read: `core/src/main/java/com/margins/rogue/state/RunState.java` (~299).
- **The identify-by-use machinery (FR-11/FR-12):** `Supply` (display name + possible identities), `TrueIdentity` (display name + `apply`), `IdentifyMap` (per-seed binding + per-type identified set). Appending a single-identity supply binds deterministically with NO RNG draw (the `opts.length > 1` guard — H1-review), so actor/supply placement stays byte-identical (AD-5). `IdentifyMap.reconcile(supplyCount)` already grows a shorter old binding for appended ordinals (AD-6). Read: `core/src/main/java/com/margins/rogue/item/Supply.java`, `TrueIdentity.java`, `core/src/main/java/com/margins/rogue/state/IdentifyMap.java`.
- **The USE path:** `TurnEngine` case USE (mystery supplies reveal on first use; inert items hit the `"Milek can't read it."` no-turn branch). The lore note's read branch sits BEFORE the generic reveal. Read: `core/src/main/java/com/margins/rogue/system/TurnEngine.java` (case USE).

### Carried lessons (2.1/2.2/2.3 review findings, applied)

- **The 2.3 seam is the trigger (2.3 Decision 7).** 2.3 deliberately exposed `isComplete()` and nothing else; 2.4 introduces the persisted capture signal (the flag) as the one new narrative consequence, exactly as 2.3 predicted. Do not add a second "tutorial done" persisted flag — the capture flag IS the persisted consequence (nothing else queries tutorial completion after this).
- **Transient controller, persisted consequences (2.3/2.1 AD-6 pattern).** `CaptureController` is transient view-session state like `TutorialController`/`IntroController`; the flag, the removal, and the note ride existing serialized stores. The screen's resolve-gate is the enforcing glue — document that the capture fires only because the screen gates on `tutorial.isComplete()` (the 2.2-review "the enforcing branch is the screen's" lesson).
- **Announce every mutation (1.8/2.3 observation discipline, extended).** The capture mutates (flag, party, floor item) and announces all of it in the four log lines — nothing silent. The discipline's spirit (no unannounced state change) holds even though the capture is a deliberate event, not a passive coach (Decision 8).
- **Log-window policy (2.1 Decision 1).** The capture beat is four lines + the tutorial closing line = 5, at the window's edge — keep each line to one log line and terse; the note's east-lore lives on its read, not the beat, so the beat stays readable.
- **AD-5 / seeded-stream discipline (1.x H1-review).** The new note is single-identity so `IdentifyMap.build` draws nothing — appending it shifts NO downstream RNG consumer. Verify with a fixed-seed test that adding the supply does not change a known placement (the existing AD-5 pins must stay green).
- **Append, don't insert (AD-6 ordinal rule).** `TORN_PAGE` goes at the END of the Supply enum; a pre-2.4 save's shorter binding grows via `reconcile`. Inserting mid-enum would silently renumber ordinals and corrupt old saves — the classic AD-6 trap this story must not fall into.
- **Restart mirror of 2.3 Decision 6 (review M1).** The capture is per-first-life; `restart()` already skips the tutorial, which disarms the seam. Do not re-arm the capture on restart, and do not let a completed-tutorial-on-old-life leak into the new life (it can't — `skip()` clears `completed`).

### Placement rationale (AD-1/AD-2/AD-4/AD-15)

- `CaptureController` lives in the CORE (`rogue/narrative`), pure model, no libGDX (AD-2), headless-testable — mirroring `TutorialController`. The screen gates the call on `tutorial.isComplete()` and does no capture logic (AD-1).
- It is NOT a `System` (the `System`-suffix convention is the turn pipeline; the capture is a scripted event that runs AFTER the pipeline) and NOT a new `PlayerAction.Kind` (it is not a player intent). The AD-4 single acted branch is untouched — the capture observes the committed turn that completed the tutorial and mutates after it (Decision 1).
- Delivery is the bottom message log (AD-15) — the beat and the note's lore are ordinary log lines; no new chrome (NFR-3).
- The discovery seed is a floor item because the pickup/read flow already exists end-to-end (FR-10 + the identify-by-use machinery) — the note is a real object the player finds, reads, carries, and (in 2.5) triggers a quest with, not a UI popup.

### Serialization — what NOT to do

- `CaptureController` and its `resolved` flag are TRANSIENT view-session state: NOT on `RunState`, NOT in the libGDX Json graph. The PERSISTED capture state is exactly three existing-store writes: the `flagStore` key, the emptied `companions` list, the `floorItems` note. A save taken after the capture round-trips with the flag set, Aldric gone, and the note on the floor.
- Do NOT add a new persisted `RunState` field (no `captured`, no `tutorialDone`, no `lastAldricTile` — the note is planted at capture time and needs no stored location).
- Append `TORN_PAGE` at the END of the Supply enum only; a pre-2.4 save's shorter `IdentifyMap` binding grows via `reconcile` (AD-6), and its single identity costs no RNG draw (AD-5).
- Future save/load (deferred O6) notes: a mid-flight save loads with the tutorial reset to not-complete (transient), so the capture would not fire until the tutorial re-completes on the loaded run — and a post-capture save loads flag-set/alone. Record this in the save/load story; do not build the mechanism now.

### Scope discipline (CLAUDE.md §2/§3)

- Touch only: new `core/src/main/java/com/margins/rogue/narrative/CaptureController.java`, new `core/src/test/java/com/margins/rogue/narrative/CaptureControllerTest.java`, and small edits to `FlagStore.java` (one key constant), `RunState.java` (one removal method), `Supply.java` (one appended constant), `TrueIdentity.java` (one identity + lore line), `TurnEngine.java` (the note's read branch), `MarginScreen.java` (capture field + resolve gate + restart comment).
- Do NOT build: chaser enemies / occupation escalation (Epic 4-3), the quest/Journal (2.5), the rescue quest (Epic 5), a capture-during-combat resolution, a companion death model, any new `RunState` persisted field, or any chrome beyond log lines.
- Do NOT touch `CompanionSystem` (its null-checks already handle the empty party) or the AD-4 pipeline order.

### Testing standards

- Headless JUnit 5, no libGDX (AD-2). Drive `CaptureController.resolve(state)` on a real `new RunState(seed)` — the companion is spawned at run start, so no placement scaffolding is needed; use `getActiveCompanion()` for his tile.
- The one-shot, empty-party, alone, and no-death pins are pure controller assertions (Task 5).
- The note-read pin drives a real `TurnEngine.advance` USE — pick up the note (PICKUP action) then USE it; assert the lore line, no turn (clock unchanged), and a second read is a no-op. If positioning is awkward on the generated map, place the note deterministically via `addFloorItem` in the test.
- The AD-5 pin: a fixed-seed `new RunState(seed)` before/after the Supply append must place actors identically — the existing AD-5 / placement pins must stay green (the appended single-identity supply draws nothing).
- The AD-6 pin: the persistence round-trip suite must stay green untouched (the flag + note ride existing stores); assert explicitly that a saved post-capture state round-trips flag=1 and an empty party.

### Project Structure Notes

- New (production): `core/src/main/java/com/margins/rogue/narrative/CaptureController.java` (the one-shot capture event; authored beat lines live with it).
- New (tests): `core/src/test/java/com/margins/rogue/narrative/CaptureControllerTest.java`.
- Modified: `FlagStore.java` (key constant), `RunState.java` (companion removal method), `Supply.java` + `TrueIdentity.java` (the lore note, appended), `TurnEngine.java` (note read branch), `MarginScreen.java` (capture field + resolve gate + restart comment).
- Naming: `CaptureController` follows the `TutorialController`/`DialogController`/`IntroController` precedent (a surface/event controller, NOT a `System`).

### References

- [Source: epics.md#Story-2.4 (lines 376-390)] — the two ACs: tutorial complete + chasers catch up → Aldric leaves by capture (a `FlagStore` flag, recoverable later), not death, Klein escapes alone; the capture resolves → message log + discovery seed establish he is held EAST along the Copper Road (Act 1's wound, UJ-3's east-pull).
- [Source: prd.md#FR-3 (lines 147-152)] — "The moment the player clears the how-to-play, the chasers catch Aldric; Klein escapes alone, and his teacher and only comrade is taken. Realizes UJ-3's seed." Recovery via "a later rescue quest" (not death); the log + a discovery seed establish east/Copper Road; the capture is Act 1's wound and UJ-3's seed. §4.2 / UJ-3 (lines 68-79) — the east-pull the seed opens: "learning Aldric is held east along the Copper Road — the map's central tension made personal."
- [Source: brief-The-Margin-2026-08-06/brief.md:40] — "gameplay opens mid-flight with Aldric, a fellow knight who teaches you how to play diegetically, then is captured the moment you've learned the ropes — the first loss, and the seed of a rescue quest."
- [Source: story-2.3 (this epic's preceding story, Status: done)] — the `TutorialController.isComplete()` seam (Decision 7) 2.4 hooks; the transient-controller + AD-6 pattern; the restart / per-life decisions (6 + review M1); the observation-discipline and log-window lessons carried into Decisions 5/8.
- [Source: story-2.5 (epics.md lines 392-407)] — the quest/Journal story that consumes the `aldric.captured` flag + the discovery note as its trigger sources; 2.4 builds the seeds, 2.5 builds the machinery.
- [Source: deferred-work.md O6] — no save/load UI yet; why the capture's transient trigger and the save/load notes in Serialization are documented, not built.
- [Source: architecture AD-15 / AD-1/AD-2/AD-4] — the log is the primary text surface (the beat + lore deliver here); core owns the rule, the screen is thin glue; the single acted branch is untouched by the event.

## Dev Agent Record

### Agent Model Used

Claude Opus 4.8 (1M context)

### Debug Log References

- `mvn -o -pl core test -Dtest=CaptureControllerTest` — red phase confirmed the compile-fail (missing `TORN_PAGE`/`CaptureController`/`KEY_ALDRIC_CAPTURED`/`removeActiveCompanion`), then green: 6 tests, 0 failures.
- `mvn -o -pl core test` — full suite 287 tests, 0 failures, 0 errors, 0 skipped (was 281; +6 new `CaptureControllerTest`). Includes the AD-6 persistence round-trip and AD-5 placement pins staying green.
- `mvn -o -q -pl core install` + `timeout 40 mvn -o -pl desktop exec:java` — app boots cleanly (exit 143 = timeout kill; no exceptions on startup with the new capture field/gate).
- Review patches: `mvn -o -pl core test` — 288 tests, 0 failures (287 + the new dead-companion pin); `mvn -o -q -pl core install` + `timeout 40 mvn -o -pl desktop exec:java` — boot clean after the H1/H2 screen changes.

### Completion Notes List

- **Task 1** — `FlagStore.KEY_ALDRIC_CAPTURED = "aldric.captured"` added beside `KEY_BOND` (the rescue thread's run-scoped signal, recoverable later — not death). `RunState.removeActiveCompanion()` empties the single party slot (AD-10) — removal IS the capture; no separate "captured" marker.
- **Task 2** — `CaptureController` (new, `rogue/narrative`, pure model): one-shot `resolved` flag; `resolve(RunState)` no-ops when already fired OR when the party is already empty. On fire: sets the flag, `removeActiveCompanion()`, plants `TORN_PAGE` at Aldric's last tile, appends the four-line beat (`LINE_CHASE/TAKE/ALONE/NOTE`). Chasers narrated, not simulated (Decision 4).
- **Task 3** — `Supply.TORN_PAGE("Torn Page")` appended at the enum's end (AD-6 ordinal rule) + added to the `isConsumedOnUse` exclusion. `TrueIdentity.CHASERS_ORDER("Chaser's order")` appended with an inert `apply` and a nullable `loreLine` field (new second constructor); lore: `"Chaser's order: '…prisoners to the road-head, east along the Copper Road.'"`. `TurnEngine` USE branch (before the generic reveal): first read reveals the lore + `markIdentified` (FR-12), later reads "You've read the note.", both NO turn and the note stays (the 2.5 seed).
- **Task 4** — `MarginScreen`: `capture` field + import; the resolve gate `if (tutorial.isComplete()) capture.resolve(state);` sits right after the observe-seam (fires the acted turn the tutorial completes, one-shot); `restart()` documented (no re-arm — a restarted life keeps Aldric).
- **Task 5** — `CaptureControllerTest` (6): full beat (flag/empty party/note at Aldric's tile/four lines in order), one-shot pin, empty-party guard, the CompanionSystems-no-op-alone pin (follow writes nothing; DISTRACT refused no-turn), captured-not-killed pin, and the note-read pin (first read reveals east lore no-turn + identifies + note stays; second read no-op).
- **Task 6** — Full suite green (287); no new persisted `RunState` field (AD-6 by construction — controller transient, flag+note ride existing stores). Persistence and placement (AD-5) pins stay green — the appended single-identity supply draws no RNG. Launch boot-check clean.
- **Review patches (H1/H2/M1/M2)** — 3-layer review surfaced two screen-layer defects the headless suite could not see. H1: the E-key gate required `isProvision()`, making the note unreadable in-game — `readAction` now also uses the selected supply when it's `TORN_PAGE`. H2: `TORN_PAGE` (ordinal 23) was absent from `ITEM_ICONS`, so the unguarded floor draw hit `pixels.item(-1)` → added the paper icon entry AND a `icon >= 0` guard. M1: the note leaked into `rng.nextInt(Supply.count())` floor scatter — new `Supply.isScatterable()`/`scatterableOrdinals()`; the scatter now draws only scatterable types (one draw per item, AD-5 preserved). M2: the capture could fire on a dead companion — `resolve` now guards `!aldric.isAlive()`, pinned by `resolveNoOpsWhenTheCompanionIsAlreadyDead`. Suite 288 green; boot clean.

### File List

- **New:** `core/src/main/java/com/margins/rogue/narrative/CaptureController.java` (the one-shot capture event + the four-line beat)
- **New:** `core/src/test/java/com/margins/rogue/narrative/CaptureControllerTest.java` (7 tests — 6 dev + 1 review M2 pin)
- **Modified:** `core/src/main/java/com/margins/rogue/state/FlagStore.java` (`KEY_ALDRIC_CAPTURED`)
- **Modified:** `core/src/main/java/com/margins/rogue/state/RunState.java` (`removeActiveCompanion()`; review M1 — floor scatter draws `Supply.scatterableOrdinals()`)
- **Modified:** `core/src/main/java/com/margins/rogue/item/Supply.java` (appended `TORN_PAGE` + `isConsumedOnUse` exclusion; review M1 — `isScatterable()`/`scatterableOrdinals()`)
- **Modified:** `core/src/main/java/com/margins/rogue/item/TrueIdentity.java` (appended `CHASERS_ORDER` + nullable `loreLine` field)
- **Modified:** `core/src/main/java/com/margins/rogue/system/TurnEngine.java` (the note's read branch in case USE)
- **Modified:** `core/src/main/java/com/margins/MarginScreen.java` (capture field + import + resolve gate after the tutorial observe-seam + restart comment; review H1 — the E-key read gate; review H2 — `TORN_PAGE` icon entry + guarded floor draw)

### Review Findings

Code review run 2026-08-09 (3-layer: Blind Hunter + Edge Case Hunter + Acceptance Auditor). Two critical in-game defects surfaced by review — both were invisible to the headless suite because they live in the SCREEN layer: the discovery note was unreachable via input (the E-key gate requires `isProvision()`) and it crashed the renderer the moment it was visible (ordinal 23 not in `ITEM_ICONS`). Both fixed as review patches; a floor-scatter leak and a dead-companion capture guard also fixed. All findings applied and checked.

- [x] [Review][Patch] **H1 — The discovery note is unreadable in the actual game (AC-2 dead-on-arrival)** [`core/src/main/java/com/margins/MarginScreen.java:461`] — the E-key only issues a USE when `s.isProvision()`, and `TORN_PAGE` is not a provision, so the TurnEngine read branch was unreachable; the note-read test passed only because it drove `TurnEngine.advance` directly. Fixed: `readAction` now also uses the selected supply when `s == Supply.TORN_PAGE` (`s.isProvision() || s == Supply.TORN_PAGE`), without adding `TORN_PAGE` to `isProvision()` (which would route it through ConsumptionSystem as food).
- [x] [Review][Patch] **H2 — The note crashes the renderer the moment it is visible** [`core/src/main/java/com/margins/MarginScreen.java:605`] — `TORN_PAGE` (ordinal 23) is absent from `ITEM_ICONS`, so `iconFor` returns -1 and the unguarded floor-item draw calls `pixels.item(-1)` → `ArrayIndexOutOfBoundsException`. Fixed: added a `TORN_PAGE` → 31 (paper) icon entry AND guarded the floor draw with `if (icon >= 0)` so an unknown icon never reaches `pixels.item`.
- [x] [Review][Patch] **M1 — The note leaks into the random floor scatter, duplicating the quest seed** [`core/src/main/java/com/margins/rogue/state/RunState.java:173`] — `rng.nextInt(Supply.count())` now includes `TORN_PAGE`, so ~1/24 of scattered items is a Torn Page lying around before Aldric is taken (early lore leak + duplicate seed). Fixed: new `Supply.isScatterable()` (everything except the quest seed) + `scatterableOrdinals()`; the scatter draw picks only from those (one draw per item, AD-5 call structure preserved). A bonus of this fix: `resolveNoOpsWhenThePartyIsAlreadyEmpty` is no longer seed-fragile (a note can never scatter).
- [x] [Review][Patch] **M2 — The capture can fire on a dead companion, contradicting "capture, not death"** [`core/src/main/java/com/margins/rogue/narrative/CaptureController.java:56`] — dead companions stay in the party, so `getActiveCompanion()` can return a corpse; `resolve` then narrates the take and plants the note on the corpse tile. Fixed: `if (aldric == null || !aldric.isAlive()) return;` — a dead Aldric cannot be captured. Pinned by `resolveNoOpsWhenTheCompanionIsAlreadyDead`.
- [x] [Review][Defer] **N1 — A reloaded run re-arms the tutorial and can re-fire the capture** [`core/src/main/java/com/margins/MarginScreen.java`] — matches the story's deferred O6 save/load note (documented, not built); the capture stays one-shot per session. Recorded for the save/load story.
- [x] [Review][Defer] **N2 — `isResolved()` reads false after a post-capture reload (flag set, party empty)** [`core/src/main/java/com/margins/rogue/narrative/CaptureController.java:47`] — the getter is currently unused; the 2.5 Journal story should derive capture state from the persisted flag instead. Low.
- [x] [Review][Defer] **N3 — The reveal line is redundant (`"Torn Page: Chaser's order: …"`)** [`core/src/main/java/com/margins/rogue/system/TurnEngine.java:100`] — cosmetic; the spec left the phrasing open. Tune in the 2.5 content pass.
- [x] [Review][Defer] **N4 — `markIdentified` on an unbound identity would silently lose the lore** [`core/src/main/java/com/margins/rogue/system/TurnEngine.java:99`] — unreachable (`TORN_PAGE` is single-identity); a comment documents the invariant.
- [x] [Review][Defer] **N5 — The `isConsumedOnUse` exclusion for `TORN_PAGE` is dead code** [`core/src/main/java/com/margins/rogue/item/Supply.java:70`] — the read branch intercepts first; kept as documentation.
- [x] [Review][Defer] **N6 — The capture can append its beat on the same turn the player dies** [`core/src/main/java/com/margins/MarginScreen.java:360`] — the beat is wiped by the post-death restart anyway; minor narrative ordering, deferred.
- [x] [Review][Defer] **N7 — The spec's explicit post-capture persistence round-trip test was not added** [`core/src/test/java/com/margins/rogue/narrative/CaptureControllerTest.java`] — covered by construction (the persistence suite round-trips the flag key and the companions list); the spec's explicit pin was skipped, deferred rather than duplicating the persistence suite's coverage.

## Change Log

| Date | Who | Change |
|------|-----|--------|
| 2026-08-09 | Create | Story 2.4 created (Status: ready-for-dev) from epics.md Story 2.4 + PRD FR-3/UJ-3 + the 2026-08-06 brief + the 2.3 seam (Decision 7), carrying the 2.1/2.2/2.3 review lessons (transient controller + AD-6, announcement discipline, log-window, AD-5 seeded-stream, appended-ordinal AD-6). Four interpretation calls resolved: the capture fires on the acted turn the tutorial completes (Decision 1, one-shot guard), it is a first-life beat (restart keeps Aldric — Decision 3), the chasers are narrated not simulated (occupation escalation is Epic 4-3 — Decision 4), and the discovery seed is a new read-able lore-note Supply whose first read reveals the east/Copper-Road lore (Decision 7). No new persisted `RunState` field (AD-6 by construction). |
| 2026-08-09 | Dev | Implemented all 6 tasks. New `CaptureController` (one-shot scripted event — flag + party removal + note placement + four-line beat, chasers narrated not simulated) + appended `Supply.TORN_PAGE` / `TrueIdentity.CHASERS_ORDER` (nullable `loreLine` field; single identity draws no RNG, AD-5) + the `TurnEngine` note-read branch (first read reveals the east lore, no turn, note stays). `MarginScreen`: capture field + resolve gate after the tutorial observe-seam + restart no-re-arm comment. `RunState.removeActiveCompanion()`; `FlagStore.KEY_ALDRIC_CAPTURED`. 6 new tests (beat, one-shot, empty-party guard, alone no-op, captured-not-killed, note-read pin). Full suite green (287, was 281). No new persisted `RunState` field (AD-6). Status → review. |
| 2026-08-09 | Review | 3-layer code review (Blind Hunter + Edge Case Hunter + Acceptance Auditor). H1/H2 high (the note was unreadable via the E-key `isProvision()` gate, and it crashed the renderer — ordinal 23 absent from `ITEM_ICONS`, unguarded `pixels.item(-1)`), M1/M2 medium (the note leaked into the random floor scatter; the capture could fire on a dead companion) — all four applied and checked. N1-N6 deferred to deferred-work.md. Suite 288 green (was 287; +1 M2 pin); boot clean. Status → done. |

# Story 1.8: The survival HUD and message log

Status: done
baseline_commit: f772ddc

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As Klein,
I want to see my survival state and read what happens each turn,
so that the survival loop is legible and playable — the Epic 1 milestone (NFR-1..3, AD-15).

## Acceptance Criteria

1. **Given** a run in progress, **when** the screen renders, **then** a minimal HUD shows the four tracks, current time/weather, and HP, and the bottom message log is the primary text surface (SPD-style).
2. **Given** any turn resolves, **when** notable events occur (tier change, debuff, weather roll, detection), **then** they are written to the bottom message log in the SPD text-forward tone.

## In/Out of Scope Seam

**In scope:**
- The bottom **message log** as the primary text surface (NFR-3, AD-15): a bounded, session-scoped scrollback the screen renders; fed by every turn's `TurnResult.messages`.
- **Notable-event emission** (AC-2): the four currently-silent event classes — hunger/thirst tier change, temperature band change, weather roll, detection escalation — must emit SPD-tone messages so the log "reads like survival" (PRD §1 climax).
- **The composite debuff label seam** on `RoguePlayer` (carried from the Story 1.7 review, deferred-work.md F-A1): the HUD's active-debuff row reads one core-owned query.
- **Backpack selection** replacing the E-key `firstWhere()` stopgap (carried from the Story 1.7 review, deferred-work.md F-09 + the `MarginScreen.java:100` stopgap comment): mushrooms/cures consumed only by deliberate selection, never by a first-match quick-eat.
- The minimal HUD (AC-1): the four tracks + clock/weather + HP + debuff row + backpack row + the log + controls hint.

**Out of scope (do NOT build):**
- Dialogue / intro / quest-log surfaces (Epic 2). This story stands up the shared *log surface* they'll write to — not the dialogue system.
- A minimap, game menu, audio, tooltips, or any new chrome beyond the minimal HUD (NFR-3).
- Persisting the log across save/load (the log is transient — see Decision 1).
- Companion HUD / party status (Epic 5), saveVersion (carried open action item), horizontal progression (Epic 3).

## Design Decisions (the interpretation calls)

1. **The message log lives in core as a TRANSIENT, bounded list on RunState.** `private transient List<String> messageLog` (cap ~50, trimmed from the front) with `getMessageLog()`; `TurnEngine.advance` appends `result.messages` to it whenever the turn produced any (acted OR refused — a refused action like "A torch needs Wood and Coal." must still reach the log); `restart()` clears it. Rationale: AD-3 (RunState owns run data — the primary text surface is run data), AD-1 (the screen stays pure presentation — it *renders* the log, never *builds* it), headless-testability (the desktop module has no test net; everything the screen shows must be assertable in core), and the **transient noiseQueue precedent** (`RunState.java:92`) keeps the save format untouched (AD-6). The alternative — a screen-owned scrollback — is simpler but untestable and would push "what happened" reconstruction into the screen (a rules leak, AD-1). Tradeoff: the log does not survive save/load; a reloaded run starts with a fresh (seeded) log. Acceptable — nothing reads it back.

2. **Notable events emit via System observation, not a RoguePlayer message channel.** The four silent classes gain messages the same way `ConsumptionSystem` observes `sickBefore` (Story 1.7): the System captures the relevant state before its tick, ticks, compares after, and emits an SPD-tone line on change. `HungerSystem.tick`, `ThirstSystem.tick`, `TemperatureSystem.tick`, and `DetectionSystem.update` each gain a `List<String> messages` parameter (the pipeline passes `result.messages` — mirroring `DebuffSystem.tick`/`CombatSystem`). `RoguePlayer` stays a closed state holder with no message channel. For the weather roll (inside `RunState.tickClock`, which has no messages), `TurnEngine` observes `getWeather()` before/after `tickClock()` and emits on change — the pipeline owns the message channel (AD-4).

3. **SPD text-forward tone.** Every new message matches the established register (terse, present-tense, diegetic — the 1.7 voice: "Poisoned — you feel sick." / "The sickness deepens into fever."). Examples: hunger drop → "Hunger bites — you're Starving."; thirst drop → "Thirst closes in — Dehydrated."; into Frozen → "The cold sinks in — Frozen."; weather → a per-type onset line (`Weather.onsetLine()`: "Rain settles over the pines." / "A cold snap grips the woods." / "Storm rolls in." / "Fog swallows the trees."); detection → "Something stirs in the trees." (first SUSPICIOUS) and "A patrol has spotted you!" (first ALERTED). PRD §1: "The message log reads like survival."

4. **The debuff label seam must NOT reveal the hidden Honeymoon countdown.** `RoguePlayer.getActiveDebuffLabels()` composes the closed shape (bacterial stage + timer, diarrhea stage + timer, Rotgut, Collapse) but EXCLUDES the honeymoon countdown — Story 1.7 AC-2 makes it deliberately hidden ("no message reveals it"), and the HUD must not leak it. Post-collapse, "Collapsed (max HP 8)" may show (collapse is public — the onset announces it). Empty list when clean.

5. **The HUD stays minimal (NFR-3, AD-15).** Rows: HP; the four tracks (hunger / thirst / temperature / clock+weather); the active-debuff row (Decision 4, blank when clean); the backpack row with the selected stack highlighted (Task 5); the bottom message log (last ~5 lines — the PRIMARY text surface); the controls hint; the game-over line. The screen formats the clock from `getClockPhase()`/`isDay()` + `getClockTurns()` (DayPhase is a bare enum — render "Day"/"Night"). No minimap, no XP, no quest log.

6. **Backpack selection replaces the E-key stopgap.** The screen holds a selected slot (0..7) and a cycle key (TAB or `[`/`]`) moves to the next occupied stack, wrapping. E (and K/F/V) act on the SELECTED stack — `firstWhere()` is removed. The wrap rule is core logic → a headless-testable `Inventory.nextOccupiedStack(int from)` (next occupied index strictly after `from`, wrapping to 0, `-1` if none) so the desktop shell stays thin (AD-1). Selection is screen state (reset on restart; not persisted). This closes F-09: a mushroom is eaten only by selecting it, never by quick-eat.

## Tasks / Subtasks

- [x] **Task 1 — The message log (AC: 2)**
  - [x] `RunState`: `private transient List<String> messageLog` (cap ~50, trimmed from the front), `getMessageLog()` (unmodifiable view or copy), cleared in `restart()`. transient → not serialized (AD-6); mirrors the transient `noiseQueue` precedent (`RunState.java:92`).
  - [x] `TurnEngine.advance`: after the switch, append `result.messages` to the log whenever it is non-empty (acted AND refused turns — the log is the text surface for all feedback). Trim past the cap.
  - [x] Seed the log with the opening line ("You flee into the pines. Aldric is beside you.") so the surface is never empty at first render — the screen currently owns that string; move the seed to the log (see Task 4).
  - [x] Tests: multi-message turns land in order; the log trims at the cap; `restart()` clears it; a refused action's message lands without committing a turn (AD-5 honesty — mirror `wallBumpCommitsNoDebuffTick`); the log is transient (a save round-trip leaves a fresh empty log — mirror the `RunStatePersistenceTest` pattern).

- [x] **Task 2 — Notable-event emission (AC: 2)**
  - [x] `HungerSystem.tick(player, messages)` + `ThirstSystem.tick(player, messages)`: capture the tier before (`getStatus()` / `getThirstStatus()`), tick, compare after, emit an SPD-tone message on ANY change (drop or rise — eating back up is notable). Decision 2 pattern.
  - [x] `TemperatureSystem.tick(state, messages)`: capture `getTempBand()` before, tick, compare, emit on band change (the extreme-band entries are the lethal ones — Frozen/Overheated must be loud; a return to Neutral is optional-but-fine). Decision 2 pattern.
  - [x] Weather roll: `TurnEngine` captures `getWeather()` before `state.tickClock()`, compares after, emits `Weather.onsetLine()` on change. Add `Weather.onsetLine()` (core, AD-2 clean — the enum already owns `label()`).
  - [x] `DetectionSystem.update(state, messages)`: emit at most one line per escalation KIND per turn (first UNAWARE→SUSPICIOUS and/or first SUSPICIOUS→ALERTED) to avoid a full-garrison spam — "Something stirs in the trees." / "A patrol has spotted you!". Decision 3 tone.
  - [x] Debuff escalation already emits (Story 1.7) — verify each transition's message and PIN it in a test that it lands in the log.
  - [x] Tests: one per event class — hunger drop + rise, thirst drop, temp band entry, weather roll at a cycle boundary (pin CLEAR→rain on a seeded cycle — see `SurvivalTickTest`'s `setWeather` pin precedent), detection SUSPICIOUS + ALERTED, debuff escalation already covered.

- [x] **Task 3 — The composite debuff label seam (AC: 1; Story 1.7 deferral F-A1)**
  - [x] `RoguePlayer.getActiveDebuffLabels()`: ordered `List<String>` of active labels — "Nausea (t)", "Fever (t)", "Delirium (t)" (t = remaining timer), "Diarrhea (t)", "Rotgut", "Collapsed (max HP 8)". Empty when clean. Composes ONLY the closed shape (no new flags — spine line 186). **Excludes the hidden Honeymoon countdown** (Decision 4; Story 1.7 AC-2).
  - [x] Tests: composition in each state (Nausea, Fever, Delirium, Diarrhea 1/2, Rotgut, post-Collapse, treated Delirium); the honeymoon countdown is NEVER in the list while hidden; empty when clean; nourish-out clears Nausea/Fever from the list.

- [x] **Task 4 — The minimal HUD (AC: 1)**
  - [x] `renderHud()`: HP + the four tracks + clock/weather + the debuff row (Task 3) + the backpack row (Task 5) + the bottom message log (last ~5 lines from `getMessageLog()`, the PRIMARY surface) + the controls hint + the game-over line. Minimal — no new chrome (NFR-3, AD-15).
  - [x] Screen formats the clock: `(state.isDay() ? "Day" : "Night") + " " + state.getClockTurns()` — DayPhase is a bare enum (`DayPhase.java:9`), no label to reuse.
  - [x] Replace the screen's single `message` field with the log rendering (the game-over line stays a prominent overlay, seeded into the log too).
  - [x] Keep the Story 1.5/1.6 action keys wired (C/B/T/K/F/V/E) — see Task 5 for the E-key change.

- [x] **Task 5 — Backpack selection replaces the E-key stopgap (AC: 1; Story 1.7 deferral F-09)**
  - [x] `Inventory.nextOccupiedStack(int from)`: next occupied stack index strictly after `from`, wrapping to 0, `-1` when the backpack is empty. Headless-tested.
  - [x] `MarginScreen`: a `selectedSlot` (0..7); TAB or `[`/`]` cycles to `nextOccupiedStack(selectedSlot)`; E (and K/F/V) act on the SELECTED stack (not `firstWhere`); remove `firstWhere()` and the stopgap comment (`MarginScreen.java:99-117`). A selection pointing at a stack that isn't consumable/actionable stays inert (no turn — ConsumptionSystem's refusal already handles it).
  - [x] HUD backpack row: the selected stack highlighted, `displayNameFor(type)` + count (`backpackType`/`backpackCount`); empty slots blank.
  - [x] Tests: the wrap rule (skips empty slots, wraps past the end, `-1` when empty, starts after the current selection).

- [x] **Task 6 — Tests, full suite, serialization check (AC: all)**
  - [x] All core seams headless (no libGDX types — AD-2): `MessageLogTest` (Task 1), `NotableEventEmissionTest` (Task 2), `DebuffLabelTest` (Task 3), `InventorySelectionTest` (Task 5).
  - [x] Save/load: the log is transient — pin that a round-trip leaves a fresh empty log (AD-6 honesty).
  - [x] Full suite green (`mvn -o clean install`), no regressions in the 172 existing tests (especially `SurvivalTickTest`, `DebuffSystemTest`, `RunStatePersistenceTest`, `WaterRiskTest`).

## Dev Notes

### Current state (what exists, what to preserve)

- **The screen is a thin shell over a tested core.** `MarginScreen.renderHud()` (`MarginScreen.java:188-201`) today draws one HP/hunger line, one `message` string, and a controls hint; `handleInput()` keeps `message = result.lastMessage()` (`MarginScreen.java:87-88`). `TurnResult.messages` already carries the FULL ordered list per turn (`TurnResult.java:12`) — the screen just discards all but the last. The log is a rendering change over that existing data + a core seam (Decision 1).
- **The message channel already exists at the right seams.** The systems that need to emit take `List<String> messages` today: `DebuffSystem.tick`, `CombatSystem.*`, `ConsumptionSystem`, the TurnEngine switch branches. The four that DON'T and must gain the param (Decision 2): `HungerSystem.tick`, `ThirstSystem.tick` (pure delegates — `HungerSystem` calls `player.tickHunger()`, `ThirstSystem` calls `player.tickThirst()`), `TemperatureSystem.tick(state)` (weather/fire drivers, no messages), `DetectionSystem.update(state)` (advances each enemy's `Detection` UNAWARE→SUSPICIOUS→ALERTED, no messages).
- **Tier-change observation is trivial at these seams.** `RoguePlayer.getStatus()` (hunger, `RoguePlayer.java:154`) and `getThirstStatus()` (`:374`) expose the tiers; `getTempBand()` (`:449`) exposes the temperature band. The Systems capture-before/compare-after is the `sickBefore` pattern already in `ConsumptionSystem` (Story 1.7).
- **The weather roll happens inside `RunState.tickClock()`** (`RunState.java:323`, roll at `:334`) at each 170-turn cycle boundary. `getWeather()` (`:311`) exposes the current weather; `Weather.label()` (`Weather.java:27`) is the display label; a new `onsetLine()` (Decision 3) is the message. `TurnEngine` already calls `state.tickClock()` in the acted branch (`TurnEngine.java:189`) — wrap it with the before/after observation there.
- **The hidden-Honeymoon constraint is real.** Story 1.7 AC-2 ("hidden 60-turn countdown, no message reveals it") and its test `honeymoonCountsDownHiddenThenCollapses` assert the onset reads sweet with no warning. The HUD seam MUST NOT leak the countdown (Decision 4). `getHoneymoonCountdown()` exists (`RoguePlayer.java:510`) but is for the systems, not the HUD.
- **The E-key stopgap is a documented 1.8 deferral.** `MarginScreen.java:99-117` (`firstWhere(Supply::isProvision)`) is explicitly "a stopgap until the Story 1.8 HUD adds real item selection," and the Story 1.7 review deferred F-09 ("E-key quick-eat can auto-feed a poison mushroom") to 1.8 with the note "the HUD item-selection must give deliberate use of the new mushroom/cure supplies." `Inventory` exposes `backpackType(slot)`/`backpackCount(slot)` (`Inventory.java:125-132`) and `EMPTY` — enough for the selection seam.
- **The desktop module has NO test infra** ("No tests to run" — the `desktop` module is a pure libGDX shell). Everything assertable lives in core (AD-2). The screen work in Tasks 4-5 must be thin glue over core-tested seams (`getMessageLog`, `getActiveDebuffLabels`, `nextOccupiedStack`, the emitted messages).

### Placement rationale (AD-1/AD-3/AD-4)

- The log is `RunState` state (AD-3), transient (AD-6); `TurnEngine` is its single writer (AD-4 — it already owns `result.messages`); `MarginScreen` is its reader/rendering (AD-1). This keeps the primary text surface testable in core and out of the save format.
- The notable-event emissions stay in the per-turn pipeline Systems (Decision 2) — the AD-4 fixed order is unchanged; each System merely gains the messages param it should have had (mirroring `DebuffSystem`/`CombatSystem`).
- The debuff labels live on `RoguePlayer` (the Status block owner, spine line 186 — the closed shape's own seam). `DebuffSystem` and the HUD both read it; neither holds a duplicate.
- The backpack selection rule is `Inventory` logic (core, AD-1) so the screen's key handling stays input-glue.

### Serialization — what NOT to do

- The log is transient: it does NOT enter the libGDX Json graph. Do NOT serialize it; do NOT add a save-format field. A round-trip test asserting the loaded log is fresh mirrors the existing transient-noiseQueue handling. (The one wrinkle: `fromJson` runs field initializers, so a loaded `RunState` has an empty-but-non-null `messageLog` — assert empty, not null.)

### Scope discipline (CLAUDE.md §2/§3)

- Touch only: `RunState` (transient log + getter + restart clear), `TurnEngine` (append log + weather observation), `HungerSystem`/`ThirstSystem`/`TemperatureSystem`/`DetectionSystem` (messages param + observation), `RoguePlayer` (`getActiveDebuffLabels()` only — do NOT touch the debuff shape), `Weather` (`onsetLine()` only), `Inventory` (`nextOccupiedStack(int)` only), `MarginScreen` (HUD + log render + selection), and the new test classes.
- Do NOT build: dialogue/intro/quest-log (Epic 2), companion HUD (Epic 5), a minimap or menus, tooltips, audio, saveVersion, or any new chrome beyond the minimal HUD. Do NOT persist the log. Do NOT add ad-hoc debuff flags for the HUD (reuse the closed shape — spine line 186).
- If a message's wording needs tuning, keep the tone and change the string — don't add message configurability.

### Testing standards

- Headless JUnit 5, no libGDX types (AD-2) — the established core test net is the model. Seeded RNG determinism: construct `RunState(seed)` and pin where a roll matters.
- Honesty pins mirroring the survival-clock tests: a wall-bump / refused action must not commit a turn but its message must reach the log (AD-5) — two different facts, both pinned.
- For the weather-roll message test: use the `setWeather` pin precedent (`SurvivalTickTest` — `RunState.setWeather` at `RunState.java:313`) plus a seed whose cycle boundary rolls a non-CLEAR type, or assert the message fires exactly when `getWeather()` changes.
- For the tier-change messages: drive via `HungerSystem.tick`/`ThirstSystem.tick` directly with the seeded player (the established `SurvivalTickTest` style), asserting the message list contents, not the screen.

### Project Structure Notes

- New files: `core/src/test/java/com/margins/rogue/MessageLogTest.java` (Task 1), `core/src/test/java/com/margins/rogue/NotableEventEmissionTest.java` (Task 2), `core/src/test/java/com/margins/rogue/DebuffLabelTest.java` (Task 3), `core/src/test/java/com/margins/rogue/InventorySelectionTest.java` (Task 5). No new core production files are expected (all seams land on existing classes) — unless the selection needs a dedicated helper class, keep it on `Inventory`.
- Modified: `RunState.java`, `TurnEngine.java`, `HungerSystem.java`, `ThirstSystem.java`, `TemperatureSystem.java`, `DetectionSystem.java`, `RoguePlayer.java`, `Weather.java`, `Inventory.java`, `core/src/main/java/com/margins/MarginScreen.java`.
- Naming convention (`System`-suffix pipeline systems, `Rogue`-prefix entities) and the single-mutation-path rule (`TurnEngine.advance` → systems → mutate `RunState`, AD-3/AD-4) apply unchanged.

### References

- [Source: epics.md#Story-1.8 (lines 294-308)] — the two ACs: minimal HUD (four tracks + time/weather + HP, bottom log primary, SPD-style); notable events (tier change, debuff, weather roll, detection) written in the SPD text-forward tone.
- [Source: epics.md#NonFunctionalRequirements (lines 57-64, NFR-1..5) and AD-15 (line 85)] — the SPD-style presentation lock: bottom message log primary, HUD minimal, 2D top-down tiles, turn-based.
- [Source: prd.md §1 (lines 56-57), §4.7 (line 331), §2 SPD-style definition (line 119)] — "The message log reads like survival"; the text-channel NFR; the presentation lock definition.
- [Source: architecture spine AD-1/AD-2 (lines 20-69), AD-3 (RunState single owner), AD-4 (pipeline), AD-6 (serialization), line 229 presentation row] — layered/no-libGDX-in-core, log-as-run-state, fixed pipeline, transient-save safety, `MarginScreen` owns the presentation.
- [Source: story-1.7 review deferrals, deferred-work.md (2026-08-08)] — F-A1 (composite debuff query seam for the 1.8 HUD) and F-09 (E-key quick-eat can auto-feed a poison mushroom; the 1.8 item-selection replaces it).
- [Source: story-1.7-the-debuff-system.md] — the `sickBefore` observation pattern (`ConsumptionSystem`), the hidden-Honeymoon AC-2, the SPD message register, the closed debuff shape.
- [Source: story-1.6-temperature-and-the-campfire-torch.md] — the `TemperatureSystem.tick(RunState)` driver shape; the `setWeather` test pin.
- [Source: story-1.3-day-night-clock-and-per-cycle-weather.md] — the clock/phase queries (`getClockPhase()`, `getClockTurns()`, `isDay()`); the weather-roll site (`RunState.tickClock`).
- [Source: story-1.2-four-survival-tracks-that-tick-on-real-turns.md] — the `HungerSystem`/`ThirstSystem` pure-delegate shape; the survival-clock honesty pins (AD-5).

## Dev Agent Record

### Agent Model Used

Claude Opus 4.8 (1M context)

### Debug Log References

- (2026-08-08) Task 1 append-placement fix: the log append initially sat before the `if (acted)` block, excluding the acted-branch emissions (Wait, combat, Last Stand) — tests caught it (3 vs 2 messages, 1 vs 50 cap). Moved to the very end of `advance()`, after `FovSystem.compute(state)`, just before `return result`.
- (2026-08-08) `ThirstSystem.tick`/`HungerSystem.tick`/`TemperatureSystem.tick`/`DetectionSystem.update` signature changes rippled to 9 test call sites (DebuffSystemTest ×4, TemperatureSystemTest ×17, TorchTest ×1) — all updated to pass a message list.

### Completion Notes List

- Story 1.8 implemented end-to-end: message log (Task 1), notable-event emission (Task 2), the `getActiveDebuffLabels()` seam (Task 3), the minimal HUD (Task 4), backpack selection replacing the E-key stopgap (Task 5). Full reactor green: **199 core tests, 0 failures** (up from 172 — 27 new).
- The tier-RISE emission lives in `ConsumptionSystem` (not `HungerSystem`/`ThirstSystem`): `tickHunger`/`tickThirst` never rise, so the "eating back up is notable" requirement (Task 2 AC) can only be observed where the apply that drives eat()/drink() runs. The Systems observe drops; `ConsumptionSystem` emits rises — both via the same capture-before/compare-after pattern (Decision 2).
- The HUD renders the core-owned log (AD-1); `TurnEngine` is its single writer (AD-4); the log is transient RunState state (AD-3/AD-6 — does not survive save/load, pinned in `MessageLogTest`).
- Screen: removed the `firstWhere` stopgap + single-`message` field; game-over line is now seeded into the log + a centered overlay; clock formatted "Day/Night + turn".

### File List

**Modified (production):**
- `core/src/main/java/com/margins/rogue/state/RunState.java` — transient `messageLog` (cap 50) + `getMessageLog()` (unmodifiable view)/`appendMessages()`/`seedMessageLog()`; seeded in the ctor + `restart()` + `restoreAfterLoad()` (review: deterministic reloaded-log).
- `core/src/main/java/com/margins/rogue/system/TurnEngine.java` — append `result.messages` to the log at the end of `advance()`; Weather before/after observation around `tickClock()`; 4 system call sites gain `result.messages`; review: USE-branch tier observation for the mystery-supply gamble + amplified-drain re-check after `DebuffSystem.tick`.
- `core/src/main/java/com/margins/rogue/system/HungerSystem.java` — `tick(player, messages)` + `hungerTierLine()` (tier-drop observation).
- `core/src/main/java/com/margins/rogue/system/ThirstSystem.java` — `tick(player, messages)` + `thirstTierLine()` (tier-drop observation).
- `core/src/main/java/com/margins/rogue/system/TemperatureSystem.java` — `tick(state, messages)` + `tempBandLine()` (band-change observation); review: emits only when the band worsens (recovery silent — direction-implying lines no longer misread on warming).
- `core/src/main/java/com/margins/rogue/system/DetectionSystem.java` — `update(state, messages)`; at most one line per escalation KIND per turn.
- `core/src/main/java/com/margins/rogue/system/ConsumptionSystem.java` — tier-RISE emission on eating/drinking back up.
- `core/src/main/java/com/margins/rogue/Weather.java` — `onsetLine()` per type.
- `core/src/main/java/com/margins/rogue/RoguePlayer.java` — `getActiveDebuffLabels()` (Honeymoon countdown excluded).
- `core/src/main/java/com/margins/rogue/item/Inventory.java` — `nextOccupiedStack(int)` + `previousOccupiedStack(int)` wrap rules (`Math.floorMod` — safe on negative `from`).
- `core/src/main/java/com/margins/MarginScreen.java` — minimal HUD + log rendering + backpack selection (TAB/`[`/`]`), removed `firstWhere()`; review: single `GAME_OVER_LINE` constant, consumed-selection reset, "Another life." restart feedback, backpack row stops at the right edge.

**New (tests):**
- `core/src/test/java/com/margins/rogue/MessageLogTest.java` (5), `NotableEventEmissionTest.java` (7; +1 review: debuff-escalation-to-log through the real pipeline), `DebuffLabelTest.java` (10), `InventorySelectionTest.java` (11; +5 review: backward mirror + negative-`from`).

**Modified (tests — signature ripple):**
- `DebuffSystemTest.java` (ThirstSystem ×4), `TemperatureSystemTest.java` (×17), `TorchTest.java` (×1).

## Review Findings

Adversarial review (Blind Hunter + Edge Case Hunter + Acceptance Auditor; 2026-08-08). Three layers ran in fresh contexts against `git diff HEAD -- core/` (tracked diff vs `baseline_commit` f772ddc + untracked new files); each finding was verified against the source before reporting. Summary: **12 patch, 5 defer, 4 dismissed** — no decision-needed items (all patch findings were unambiguous). Justine chose **apply every patch**; all 12 applied and re-verified (205 core tests green, +6 from review pinning).

**Patch (12) — all applied (2026-08-08):**

- [x] [Review][Patch] **Backward `[` selection lands on the wrong stack with 3/5/6/7 occupied stacks** [core/.../MarginScreen.java cycleSelection] — auditor+blind+edge, all three layers: backward = 7 forward steps only equals "back" when the occupied count divides 8. Fix: `Inventory.previousOccupiedStack(int)` — the true ring mirror (symmetric on any count), used by the screen. **Applied** + 4 tests (`backwardStepsToThePreviousOccupiedStack`, `backwardWrapsPastTheStartToTheLastOccupied`, `backwardIsTheTrueMirrorOfForwardOnThreeOccupiedStacks`, `backwardFromFreshSelectionLandsOnTheLastOccupied`).
- [x] [Review][Patch] **Diarrhea's amplified drain can cross a hunger/thirst tier without announcing it** [core/.../TurnEngine.java:185-187] — blind+edge: `DebuffSystem.tick` runs after the Hunger/Thirst observation, so a Stage-2 drop into STARVING/PARCHED is never announced. Fix: capture the tiers AFTER the direct ticks, re-compare after `DebuffSystem.tick`, emit only the additional drop (no duplication). **Applied**.
- [x] [Review][Patch] **Mystery-supply USE tier change is silent** [core/.../TurnEngine.java:85-96] — blind: the non-provision `TrueIdentity.apply(player)` path was the one silent tier-change route (ConsumptionSystem already observes its own). Fix: capture/compare around `id.apply(player)` using the same `hungerTierLine`/`thirstTierLine` helpers. **Applied**.
- [x] [Review][Patch] **Temp-band lines fire on recovery with direction-implying text** [core/.../TemperatureSystem.java] — blind+edge: warming out of Frozen read "The chill deepens." Fix: emit only when the band WORSHENS (distance-from-Neutral increases); recovery toward Neutral is silent. Existing `temperatureBandEntryEmitsSPDLine` still passes (Chilled/Cold/Frozen are all worsening). **Applied**.
- [x] [Review][Patch] **Reloaded-run log content couples to constructor side-effects** [core/.../RunState.java restoreAfterLoad] — blind: the transient log isn't written by Json, so a loaded run's content depends on which ctor ran during load. Fix: `restoreAfterLoad()` reseeds the log deterministically. **Applied**.
- [x] [Review][Patch] **`getMessageLog()` returns the live list, not an unmodifiable view** [core/.../RunState.java] — auditor (spec Task 1 letter): the log must have exactly one writer. Fix: return `Collections.unmodifiableList(messageLog)`; `appendMessages` remains the sole mutator (TurnEngine — AD-4). **Applied**.
- [x] [Review][Patch] **`nextOccupiedStack` has a negative-index footgun** [core/.../item/Inventory.java] — blind+edge: `(from + step) % 8` with a negative dividend yields a negative slot index → AIOOBE. Fix: `Math.floorMod(from + step, 8)` (and the new `previousOccupiedStack` uses it too). **Applied** + `neverThrowsOnNegativeFrom`.
- [x] [Review][Patch] **Game-over line is duplicated (log seed vs overlay)** [core/.../MarginScreen.java] — blind: two string literals could drift. Fix: single `GAME_OVER_LINE` constant feeding both the log append and the centered overlay. **Applied**.
- [x] [Review][Patch] **Backpack row draws past the right viewport edge** [core/.../MarginScreen.java renderBackpackRow] — edge: 8 stacks of long names overflow 480px. Fix: measure with `GlyphLayout` before drawing; stop (`break`) when `x + w > WW`. **Applied**.
- [x] [Review][Patch] **A consumed selection leaves E/K/F/V silently inert** [core/.../MarginScreen.java handleInput] — edge: eating the selected stack empties the slot but `selectedSlot` keeps pointing at it. Fix: reset `selectedSlot = -1` when its slot empties. **Applied**.
- [x] [Review][Patch] **Restart loses the "Another life" feedback** [core/.../MarginScreen.java] — edge (deletion-check, medium): the old `firstWhere` era showed a confirmation after death-restart; the new restart just re-showed the opening line. Fix: append `"Another life. [WASD] move."` after reseeding on R. **Applied**.
- [x] [Review][Patch] **Debuff escalation landing in the log is not test-pinned** — auditor (Task 2 test-gap): unit-level `DebuffSystem` calls couldn't prove the TurnEngine ordering never drops the line. Fix: `debuffEscalationLandsInTheLog` drives Nausea's full 30-turn course through the real pipeline and asserts the escalation line reaches the log. **Applied**.

**Defer (5):**

- [x] [Review][Defer] **Weather onset announces a change whose temperature effect lags one turn** [core/.../TurnEngine.java:190-192] — blind+edge: the boundary roll happens after TemperatureSystem already ticked under the OLD weather, so a Cold Snap onset reads as ominous a turn before its −2/turn bites (FOV, by contrast, applies the new weather instantly). Pre-existing Story 1.6 pipeline ordering, out of 1.8 scope — deferred to a future pipeline-ordering story.
- [x] [Review][Defer] **"Wait"/refusal lines crowd notable events out of the 5-line window** [core/.../TurnEngine.java] — blind: grinding WAIT fills the window with "Wait". By-design bounded surface (AD-15); a future polish could suppress the wait line or grow the window.
- [x] [Review][Defer] **`Weather.onsetLine()` default maps any future weather type to "The skies clear."** [core/.../Weather.java] — speculative robustness nit (the 5-type enum is fixed); noted, no current defect.
- [x] [Review][Defer] **Opening (cycle-0 / restart) weather never gets an onset line** [core/.../RunState.java] — edge: the run-start roll is "current state", not a change (AC-2 is about transitions; the HUD clock already shows the weather). Optional atmosphere, deferred.
- [x] [Review][Defer] **Bloated slow invisible in the debuff row** [core/.../RoguePlayer.java getActiveDebuffLabels] — edge: a Well-Fed player's 50% stumble (`isSlowed()`) has no HUD label. The spec's label set (bacterial / diarrhea / Rotgut / Collapse) is explicit and Bloated is a Well-Fed side effect (already legible via `hungerLabel`) — deferred as a future HUD enhancement.

**Dismissed (4):** seeded-not-empty-on-reload (the honest AD-6 contract is a 1-line seeded log; auditor-agreed) · O(n) `remove(0)` trim (single-turn emissions are far below cap 50) · `firstWhere()` quick-eat removal (the intentional F-09 retirement — deliberate selection, not a regression) · E-inert-without-selection (the deliberate-selection requirement itself; selection is inert until TAB).

## Change Log

| Date | Who | Change |
|------|-----|--------|
| 2026-08-08 | Dev | Implemented Story 1.8 (all 6 tasks). Message log on RunState (transient, capped, seeded); notable-event emission across the 4 silent systems + weather + tier-rise in ConsumptionSystem; `getActiveDebuffLabels()` seam; minimal HUD; backpack selection. 199 core tests green. |
| 2026-08-08 | Review | Adversarial review (Blind Hunter + Edge Case Hunter + Acceptance Auditor) → 12 patch findings, all applied (backward-selection mirror, amplified-drain + mystery-supply tier observation, worsening-only temp lines, deterministic reloaded log, unmodifiable log view, floorMod guards, backpack overflow, consumed-selection reset, "Another life." feedback, single game-over constant, escalation-to-log test). 5 deferred, 4 dismissed. 205 core tests green → Status done. |

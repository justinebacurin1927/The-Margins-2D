---
baseline_commit: 4493301
---

# Story 5.5: Bond and the shapes of loss

Status: done

## Story

As Klein,
I want a relationship that deepens and can break,
so that companions carry emotional stakes (FR-17, AD-7).

## Acceptance Criteria

**AC-1 — Bond is per-companion and gates help, loyalty, and hostility.**
**Given** shared survival and dialogue choices **When** Bond changes **Then** it is tracked per-companion (keyed by `bindId`); high Bond unlocks lore/loyalty/personal quests, low Bond withholds help or triggers departure, betrayal turns hostile.

**AC-2 — A companion loss takes one of three shapes.**
**Given** a companion is lost **When** the loss resolves **Then** it takes one of three shapes — Captured (recoverable via quest), Departure (low Bond), or Death (permanent) — matching the game's permadeath weight.

## Scope decisions (author, 2026-08-13 — running the loop autonomously per Justine)

- **D1 — Model now, quest/dialogue content later (the 5.1 pattern).** 5.5 ships the *state model* the ACs require — per-companion Bond effect gates, the three-shape loss record, the betrayal/hostile flag — as authoritative `FlagStore`/`RunState` reads. The narrative payloads that consume them (the personal/loyalty quests, the withheld-help dialogue branches, a live hostile ex-companion enemy) are Story 5.6 / Epic-5 content. This keeps 5.5 testable and bounded, exactly like 5.1's roster abstraction.
- **D2 — A unified `CompanionLoss { NONE, CAPTURED, DEPARTED, DEAD }` (AC-2).** Stored per-companion in `FlagStore` (`loss.<bindId>`, ordinal; unset → NONE, AD-6). `recoverable()` is true only for CAPTURED (recoverable via quest); DEPARTED and DEAD are losses for the run, DEAD permanent. `RunState.loseCompanion(id, shape)` records the shape and removes the active body **except for DEAD** — a dead companion's corpse stays where it fell (the existing render contract), while Captured/Departed leave the map.
- **D3 — Wire the three existing/needed exits into the one model.** DEATH: `CombatSystem.enemyPhase` already emits "Aldric falls!" — it now also records `DEAD`. CAPTURED: the Story-2.4 `CaptureController` already removes Aldric + sets `KEY_ALDRIC_CAPTURED`; it now records `CAPTURED` through `loseCompanion` (the rescue-thread flag is unchanged — the two coexist). DEPARTURE: new — `RunState.checkBondDeparture(id)` departs a companion whose Bond has fallen to `DEPARTURE_BOND`.
- **D4 — Bond effect gates as pure reads (AC-1).** `FlagStore.bondUnlocksLoyalty(id)` (Bond ≥ `LOYALTY_BOND` = 3 — a real investment beyond the warm tier) and `bondWithholdsHelp(id)` (Bond ≤ `WITHHOLD_BOND` = −2 — the cold tier). Content branches on these; 5.5 proves the thresholds. Bond itself is per-companion since 5.1 (ratified).
- **D5 — Betrayal → hostile is a flag, not a live enemy (AC-1).** `RunState.betray(id)` sets a per-companion hostile flag (`FlagStore.setHostile`/`isHostile`), bottoms the Bond, and departs the body (`DEPARTED`). Spawning a positioned hostile ex-companion tile-agent is deferred content (5.6) — the flag is the seam it will read.
- **D6 — Departure is not auto-run in the pipeline.** `checkBondDeparture` is exposed for content to call at a natural beat (dialogue/rest, 5.6); wiring an automatic per-turn check would add an AD-4 pipeline step and risk removing Aldric mid-run. Default Bond is neutral (0), so nothing departs unbidden.
- **Deferred (→ 5.6+):** the personal/loyalty quest content, the withheld-help dialogue branches, a positioned hostile ex-companion, Bond gains from shared survival ticks (only dialogue-tagged Bond exists today), the departure trigger points.

## Baseline (verify before adding)

- **`FlagStore`** (Story 5.1): per-companion Bond — `getBond(id)/adjustBond(id,delta)/getBondTier(id)`, `bondKey(id)`. `KEY_ALDRIC_CAPTURED` is the Story-2.4 rescue-thread flag. Generic `get/set/add` k/v; unset → 0 (AD-6).
- **`CombatSystem.enemyPhase`** emits `"Aldric falls!"` when an enemy kills the barred companion (`companion` in scope with `getId()`).
- **`CaptureController.resolve`** (Story 2.4): sets `KEY_ALDRIC_CAPTURED`, `removeActiveCompanion()`, plants a Torn Page, appends the beat. The Captured shape for Aldric.
- **`RunState`**: `getActiveCompanionId()`, `removeActiveCompanion()`, `getFlagStore()`. A DEAD companion stays in the list as a corpse (`isAlive()` false; `act` early-returns; the screen keeps the sprite).
- **Tests to keep green:** `CaptureControllerTest` (asserts companion removed + `KEY_ALDRIC_CAPTURED`; the added loss record is additive), `FlagStoreTest`, `RunStatePersistenceTest` (Bond/flags round-trip).

## Tasks / Subtasks

- [x] **Task 1 — `CompanionLoss` model (AC-2, D2).**
  - [x] 1.1 New `com.margins.rogue.CompanionLoss { NONE, CAPTURED, DEPARTED, DEAD }` + `recoverable()` (CAPTURED only).
  - [x] 1.2 `FlagStore.lossKey(id)`, `getLoss(id)`, `setLoss(id, loss)` (ordinal storage; unset → NONE, AD-6).
- [x] **Task 2 — Bond effect gates + hostility (AC-1, D4/D5).**
  - [x] 2.1 `FlagStore.bondUnlocksLoyalty(id)` (≥ `LOYALTY_BOND`=3), `bondWithholdsHelp(id)` (≤ `WITHHOLD_BOND`=−2); `hostileKey(id)`, `isHostile(id)`, `setHostile(id)`.
- [x] **Task 3 — Loss orchestration on `RunState` (AC-1/AC-2, D2/D3/D5/D6).**
  - [x] 3.1 `loseCompanion(id, shape)`: record the shape; remove the active body unless DEAD (corpse stays).
  - [x] 3.2 `betray(id)`: `setHostile`, bottom the Bond, `loseCompanion(id, DEPARTED)`. `checkBondDeparture(id)`: if Bond ≤ `DEPARTURE_BOND`(−4) and not already lost, depart (DEPARTED), return true.
- [x] **Task 4 — Wire the three exits (AC-2, D3).**
  - [x] 4.1 DEATH: `CombatSystem.enemyPhase` records `DEAD` after "Aldric falls!".
  - [x] 4.2 CAPTURED: `CaptureController.resolve` records `CAPTURED` via `loseCompanion` (keeps `KEY_ALDRIC_CAPTURED`).
- [x] **Task 5 — Tests + verification (all ACs).**
  - [x] 5.1 AC-1: Bond gates — high Bond unlocks loyalty, cold Bond withholds help (per-companion, independent); `betray` sets hostile + departs; a companion at the departure floor departs via `checkBondDeparture`.
  - [x] 5.2 AC-2: the three shapes — a killed companion records DEAD (corpse stays), a captured one records CAPTURED (recoverable, body gone), a departed one records DEPARTED; `recoverable()` is CAPTURED-only.
  - [x] 5.3 Regression: `CaptureControllerTest` still green (the `loseCompanion` swap is behavior-compatible); full suite green (497 → 503, +6). **Verified:** all green.

## Dev Notes

- **AD-7.** Bond, loss shape, and hostility are run-scoped narrative state in `FlagStore`, keyed per companion (`bindId`).
- **AD-6.** New loss/hostile keys are plain flags — unset reads NONE / not-hostile; they round-trip with the store.
- **AD-3/AD-10.** `CombatSystem` remains the single authority that observes the companion's death (it only *records* the shape; it does not gain a new HP owner). Only the active companion has a body to remove.
- **Simplicity (CLAUDE.md §2).** 5.5 is state + thresholds; no quest engine, no hostile tile-agent, no new pipeline step. Content consumes these reads in 5.6.
- Build/verify: `docs/BUILD.md` — `mvn -o clean install`.

## Dev Agent Record

### Agent Model Used
Claude Opus 4.8 (1M context) — create-story 2026-08-13 (autonomous loop).

### Debug Log References
- `mvn -o test` — BUILD SUCCESS, full suite green (503 tests, +6 over the 497 baseline).

### Completion Notes List
- **AC-2 loss model (D2/D3):** `CompanionLoss { NONE, CAPTURED, DEPARTED, DEAD }` (`recoverable()` = CAPTURED only), stored per-companion in `FlagStore` (`loss.<bindId>` ordinal; unset → NONE, AD-6). `RunState.loseCompanion(id, shape)` records the shape and removes the active body **except for DEAD** (the corpse stays — render contract). Wired into the real exits: `CombatSystem.enemyPhase` records DEAD after "Aldric falls!"; `CaptureController` records CAPTURED via `loseCompanion` (the `KEY_ALDRIC_CAPTURED` rescue flag is unchanged). Pinned by `deathRecordsThePermanentShapeAndKeepsTheCorpse`, `captureRecordsARecoverableShapeAndRemovesTheBody`, `anUntouchedCompanionHasNoLoss`.
- **AC-1 Bond gates (D4):** `FlagStore.bondUnlocksLoyalty(id)` (Bond ≥ 3) and `bondWithholdsHelp(id)` (Bond ≤ −2) — per-companion, independent (proven on ALDRIC/MARA/YENNA). Content branches on these (5.6). Bond itself is per-companion since 5.1 (ratified).
- **AC-1 hostility + departure (D5/D6):** `RunState.betray(id)` sets the per-companion hostile flag (`FlagStore.isHostile`/`setHostile`), bottoms the Bond, and departs the body (DEPARTED); `checkBondDeparture(id)` departs a companion at/under `DEPARTURE_BOND`(−4), guarded against already-lost. Not auto-run in the pipeline (content triggers it — default Bond 0 never departs unbidden). Pinned by `betrayalTurnsTheCompanionHostileAndDepartsTheBody`, `lowBondTriggersDeparture`.
- **Review:** inline review (Blind/Edge/Acceptance; multi-agent reserved for token budget). No High/Med. Low notes: `getLoss` reads the stored ordinal unguarded (our-own-writes; a corrupt save could AIOOBE — same class as other ordinal reads); `betray` doesn't guard an already-lost companion (a nonsensical call); the content payloads (personal/loyalty quests, a live hostile ex-companion tile-agent, withheld-help dialogue branches, Bond-from-survival) are deferred to 5.6 per D1.
- **AD-3/AD-6/AD-7:** loss/hostility/Bond are run-scoped narrative state in `FlagStore` keyed per `bindId`, round-tripping as plain flags; `CombatSystem` only *records* the death shape (no new HP owner).

### File List
- `core/src/main/java/com/margins/rogue/CompanionLoss.java` (new)
- `core/src/main/java/com/margins/rogue/state/FlagStore.java`
- `core/src/main/java/com/margins/rogue/state/RunState.java`
- `core/src/main/java/com/margins/rogue/system/CombatSystem.java`
- `core/src/main/java/com/margins/rogue/narrative/CaptureController.java`
- `core/src/test/java/com/margins/rogue/CompanionLossTest.java` (new)

## Change Log

- 2026-08-13 — created by create-story (autonomous loop). Decisions: D1 model now / quest content deferred to 5.6 (the 5.1 pattern); D2 unified `CompanionLoss{NONE,CAPTURED,DEPARTED,DEAD}` stored per-companion (DEAD keeps the corpse, others remove the body); D3 wire the existing death + capture exits into the one model (plus a new departure path); D4 Bond effect gates as pure reads (`bondUnlocksLoyalty`/`bondWithholdsHelp`); D5 betrayal→hostile is a flag + departure (live hostile enemy deferred); D6 departure not auto-run in the pipeline (content triggers it). Status → ready-for-dev.
- 2026-08-13 — dev-story + inline review (autonomous loop): `CompanionLoss` enum + `FlagStore` loss/hostile storage + Bond gates (`bondUnlocksLoyalty`/`bondWithholdsHelp`); `RunState.loseCompanion`/`betray`/`checkBondDeparture`; wired DEATH (`CombatSystem`) + CAPTURED (`CaptureController`) into the model. +6 tests (`CompanionLossTest`); `CaptureControllerTest` unchanged; full suite green (503). Inline review, no High/Med (Lows: unguarded ordinal read, betray-of-lost, content deferred to 5.6). Status → done.

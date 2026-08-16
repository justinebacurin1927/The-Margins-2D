---
baseline_commit: 35012a6
---

# Story 5.6: The act-gating quests

Status: done

## Story

As Klein,
I want main-story quests to advance the acts,
so that the occupation tightens and the story throttles the run (FR-18, AD-11).

## Acceptance Criteria

**AC-1 — "Follow the Road" gates Act 1→2 and Epic 4's channel reads it.**
**Given** Act 1 **When** I complete "Follow the Road" (reach the Copper Road corridor, a Tier 2 push) **Then** a `FlagStore` quest-completion flag flips and the act advances 1→2, and Epic 4's escalation channel (`RunState.enemyCountFor`, AD-11 channel a) reads the new act.

**AC-2 — "The Rescue" gates Act 2→3 on either outcome.**
**Given** Act 2 **When** I complete "The Rescue" (reach/attempt Aldric's prison) **Then** success (Aldric rejoins) or failure (lost) **both** flip the 2→3 gate — Klein now knows the war and must choose.

## Scope decisions (author, 2026-08-14 — running the loop autonomously per Justine)

- **D1 — Wire the gates now; the live world-reinforce on flip stays deferred (the 4.3 / 5.5 pattern).** 5.6 ships the two location-triggered act-gate quests and the act *advance* (`setAct`), flipping the `quest.<id>.completed` flags that Story 2.5 explicitly deferred to Epic 5. Epic 4's channel already reads `getAct()` — `RunState.enemyCountFor(eastness, act, ny)` ramps for `act > 1` and is proven by `OccupationEscalationTest`, but has been *inert in shipped play because the act never left 1*. 5.6 makes it live-reachable. The **live mid-run reinforcement of the already-generated standing map** (the "regenerate-unexplored / reinforce on act change" seam `RunState` documents at its enemy-placement block) is a separate world-mutation feature and remains deferred — newly generated regions read the higher act; retroactively thickening the standing map is not in 5.6 scope.
- **D2 — Two authored quests on the existing 2.5 quest-flag family; reuse the rescue quest.** Both gates ride `JournalController`'s quest key helpers (`startedKey`/`completedKey`/`voidedKey`) — a trigger never hand-builds a key (AD-7, the `SceneEffects` `KEY_CACHE_*` discipline). **"The Rescue" reuses the existing production quest `QUEST_ROAD_EAST` ("The Road East")** — the rescue thread already *started* by the 2.4 Torn Page discovery, whose objective is literally "prisoners to the road-head, east along the Copper Road." Completing it is the Act 2→3 gate. Only **"Follow the Road"** is a *new* catalog entry (id `follow-the-road`). No duplicate rescue quest is invented.
- **D3 — Locations are named against existing `WorldSpine` landmarks — no new geography.** "The Copper Road corridor, a Tier 2 push" = the **Watchtower** landmark (`watchtowerX/Y`, on the road row at eastness 2/3 — inside the Tier-2 band `[0.45, 0.7)` per `RunState`'s bands). "Aldric's prison / the road-head" = the road's **east end** (`roadEndX(), roadY()` — Tier 3). Reaching a landmark tile is the trigger; no prison structure is added.
- **D4 — The Rescue outcome: both branches built + tested, success is the default reach-trigger, the determinant is deferred content (the 5.5 "model now, content later" posture).** AC-2 requires the gate to flip on *either* outcome, so `resolveRescue(state, success)` flips `setAct(3)` **unconditionally**. Success rejoins Aldric (`activateCompanion(ALDRIC)`, clear `KEY_ALDRIC_CAPTURED`, set his `CompanionLoss` → `NONE`); failure leaves him lost (captured flag persists, no rejoin), recorded as a failed completion. 5.6's default reach-trigger fires **success** (Aldric is still `CAPTURED` ⇒ recoverable). The *thing that decides success vs. failure* — the actual prison encounter/choice — is deferred to 5.7 / dialogue content; the failure branch is exercised by tests and reachable by that later content.
- **D5 — Idempotent one-shots over persisted flags only (AD-6).** Every gate is guarded by its own persisted flag (`quest.follow-the-road.completed`, `quest.the-road-east.completed`, `act.current`, `aldric.captured`). The controller holds **no state of its own** — it reads/writes `FlagStore` only — so save/load resumes correctly and a re-`resolve` after a flip no-ops. No new persisted `RunState`/companion field is added, so the AD-6 field-absent-migration concern does not arise (the 5.5 posture).
- **D6 — A one-shot every-turn controller at the CaptureController wire-site (AD-1/AD-2/AD-4).** New `narrative/ActGateController.resolve(RunState)` is pure core (`com.margins.rogue.narrative`, no libGDX type), invoked from `MarginScreen.submitPlayerAction` **immediately after `capture.resolve(state)`** — the established "safe every-frame call that fires on the acted turn" precedent. It is **not** a new `TurnEngine` pipeline step and never ticks survival (AD-5); it only reacts to the committed turn's resulting position. Log lines follow the `CaptureController.LINE_*` pattern.
- **Deferred (→ 5.7+):** the prison-encounter content that *decides* rescue success/failure; the Act-3 spine (the choice → last provisioning → Aldric resolution → closing trap) and the NW border-crossing win + cordon (AD-11 channel b); the live mid-run map reinforcement on act flip; any Bond/dialogue payloads on these beats. **Open story-design items the epic marks "do not invent" — left to 5.7:** whether a failed rescue reconciles Aldric's loss shape to `DEAD` vs. a foreclosed-`CAPTURED`; whether true death is on the table beyond capture/departure; party size beyond one active companion.

## Baseline (verify before adding)

- **`FlagStore`** — `getAct()` (`KEY_ACT = "act.current"`; never-set sentinel 0 → Act 1), `setAct(int)` (clamps ≥ 1). `KEY_ALDRIC_CAPTURED = "aldric.captured"`. `setLoss(CompanionId, CompanionLoss)`. Generic `get/set/add`; unset → 0 (AD-6).
- **`JournalController`** (Story 2.5) — the quest catalog + single-authority key helpers `startedKey(id)`/`completedKey(id)`/`voidedKey(id)`; `register(QuestDefinition{id,title,objective,giver})` (blank/dup ids rejected); passive status derivation (only STARTED quests appear; precedence VOIDED → COMPLETED → ACTIVE). Production quest constant **`QUEST_ROAD_EAST`** ("The Road East"), started by the 2.4 Torn Page. **Story 2.5 already documents that `quest.<id>.completed` is "flipped by the act-gating stories, Epic 5" — this story is that consumer.**
- **`CaptureController`** (Story 2.4) — `resolve(RunState)` is the every-turn one-shot precedent, invoked from `MarginScreen.submitPlayerAction` after `tutorial.isComplete()`; sets `KEY_ALDRIC_CAPTURED`, `removeActiveCompanion()`, records the `CAPTURED` loss (5.5).
- **`RunState`** — `getFlagStore()`, `getPlayer()` (`getTileX()/getTileY()`), `activateCompanion(CompanionId)`, `getActiveCompanion()`, `getActiveCompanionId()`, `removeActiveCompanion()`, `loseCompanion(CompanionId, CompanionLoss)`. Enemy generation reads `flagStore.getAct()` **once at run start** via `enemyCountFor`; the per-act ramp (`act > 1 && !inCordon → base + (act-1)`) is proven by `OccupationEscalationTest`. The **live act-flip world-reinforce is a documented Epic-5 seam** in the enemy-placement block — **out of 5.6 scope (D1).**
- **`WorldSpine`** (Story 3.1) — landmark accessors `corneoX/Y`, `roadY`, `roadStartX`/`roadEndX`, `watchtowerX/Y`, `borderX/Y`; `eastness(x)`. Tier bands (`RunState`): eastness ≤ 0.2 safe (Tier 0/home), < 0.45 Tier 1, < 0.7 Tier 2, else Tier 3. Watchtower ≈ 0.667 (Tier 2, on the road); road-head = `roadEndX` (Tier 3).
- **`CompanionLoss`** (Story 5.5) — `{ NONE, CAPTURED, DEPARTED, DEAD }`, `recoverable()` = CAPTURED only. `CompanionId.ALDRIC`.
- **Wire site** — `MarginScreen.submitPlayerAction` (the block that calls `capture.resolve(state)` when `tutorial.isComplete()`).
- **Tests to keep green** — `JournalControllerTest`, `CaptureControllerTest`, `OccupationEscalationTest`, `RunStatePersistenceTest`, `CompanionLossTest`. Suite is at **503** (as of 5.5).

## Tasks / Subtasks

- [x] **Task 1 — "Follow the Road" quest + the Act 1→2 gate (AC-1, D2/D3).**
  - [x] 1.1 Registered a new catalog quest in `JournalController` (ctor): id constant `QUEST_FOLLOW_THE_ROAD = "followroad"`, title "Follow the Road", objective (the eastward push along the Copper Road), no giver. Key built via the existing `startedKey`/`completedKey` helpers — never hand-built.
  - [x] 1.2 `ActGateController.resolve` (act==1 branch): auto-starts "Follow the Road" once (`set(startedKey, 1)`) so it reads ACTIVE in the Journal; when `player.getTileX() >= spine.watchtowerX()` → `set(completedKey, 1)`, `setAct(2)`, append `LINE_ROAD`. **Reach = easting threshold** (not exact-tile) — robust and faithful to "a Tier-2 push east" (Watchtower ≈ eastness 0.667, the Tier-2 band).
- [x] **Task 2 — "The Rescue" resolution + the Act 2→3 gate (AC-2, D2/D4).**
  - [x] 2.1 `resolveRescue(RunState, boolean success)`: **success** → clear `KEY_ALDRIC_CAPTURED`, set Aldric's loss → `NONE`, `activateCompanion(ALDRIC)`, append `LINE_RESCUE_WIN`; **failure** → leave Aldric lost (captured flag persists), append `LINE_RESCUE_LOSS`. **Both** set `startedKey`+`completedKey` of `QUEST_ROAD_EAST` and `setAct(3)`. One-shot via `completedKey`. (Marking *started* on completion surfaces the quest in the Journal even if the player reached the road-head without reading the Torn Page — 2.5 Decision 8.)
  - [x] 2.2 `ActGateController.resolve` (act==2 branch): when `player.getTileX() >= spine.roadEndX()` and `QUEST_ROAD_EAST` not yet completed → `resolveRescue(state, success)` where `success` = Aldric still `CAPTURED` (recoverable) — the default success path. The failure determinant is deferred content (5.7).
- [x] **Task 3 — Wire the controller (AD-1/AD-2, D6).**
  - [x] 3.1 Added the `ActGateController actGate` field (+ import + restart re-init) in `MarginScreen`; call `actGate.resolve(state)` in `submitPlayerAction` **immediately after** `capture.resolve(state)` and before the save (so a flip persists). No libGDX type in `ActGateController`.
- [x] **Task 4 — Prove Epic 4's channel reads the flipped act (AC-1, D1).**
  - [x] 4.1 `flippingActOneToTwoThroughTheGateFeedsEpicFoursThickerInterior`: drives the 1→2 flip through the gate for 10 seeds, regenerates the floor, and asserts the aggregate interior enemy count exceeds Act 1 (with a non-vacuity guard) — the `enemyCountFor` ramp is now live-reachable through the gate. No production change to the channel.
- [x] **Task 5 — Tests + verification (all ACs).**
  - [x] 5.1 AC-1: `followTheRoadIsActiveInActOneUntilTheCorridorIsReached`, `reachingTheCopperRoadCorridorFlipsActOneToTwo`, `theActOneGateIsAOneShot`.
  - [x] 5.2 AC-2: `reachingThePrisonWithAldricRecoverableRescuesHimAndFlipsTwoToThree`, `aFailedRescueLeavesAldricLostButStillFlipsTwoToThree`, `theRescueGateIsAOneShot`, `noGateFiresInActThree`.
  - [x] 5.3 Channel (Task 4) — see 4.1.
  - [x] 5.4 Regression: full suite green (503 → **511**, +8), including `JournalControllerTest`, `CaptureControllerTest`, `OccupationEscalationTest`, `RunStatePersistenceTest`, `CompanionLossTest`. **Verified:** `mvn -o -pl core test` → BUILD SUCCESS.

### Review Findings

Inline adversarial review (Blind / Edge-Case / Acceptance), 2026-08-14. **No High/Med.** 3 Low deferred, 1 dismissed as noise. Suite green (511).

- [x] [Review][Defer] "Follow the Road" reach is easting-only, not Copper-Road-row proximity [ActGateController.java] — Low, by-design (D3); refine to a road-row band in 5.7 if the corridor should be a place. Logged to deferred-work.
- [x] [Review][Defer] "Follow the Road" auto-starts during the intro/tutorial, before the capture motivates heading east [ActGateController.java] — Low, matches spec ("auto-start in Act 1"); optionally gate behind tutorial/capture in 5.7. Logged to deferred-work.
- [x] [Review][Defer] `resolveRescue(state, true)` on a DEAD Aldric would resurrect him [ActGateController.java] — Low, unreachable via the safe reach-trigger; guard the public API when 5.7 wires the determinant. Logged to deferred-work.
- Dismissed (noise): `actGate.resolve` runs on non-committed turns (wall bumps) — idempotent, position unchanged, no consequence.

## Dev Notes

- **AD-11.** Act transitions are triggered by *story flags*, not a timer or exploration counter: quest completion flips `FlagStore` flags and `setAct` advances the act. 5.6 owns **only** the flag-flip + advance and channel-a's *read* of the new act; channel-a's live map reinforcement and channel-b's border cordon (5.7) stay separate and deferred (D1).
- **AD-7.** Act and quest state are run-scoped narrative state in `FlagStore` (per-run), round-tripping as plain flags.
- **AD-6.** No new persisted field — the gate reuses `act.current`, `quest.*`, and `aldric.captured`; the controller is stateless. No migration/default concern (contrast the 1.3/3.3 field-absent traps).
- **AD-1/AD-2.** All logic and state live in headless core (`com.margins.rogue.narrative.ActGateController`); `MarginScreen` only *invokes* `resolve` and renders resulting state — no core class references a libGDX type.
- **AD-4/AD-5.** The gate resolves on committed acted turns at the post-action screen hook (the `CaptureController` precedent), **not** a new `TurnEngine` pipeline step, and never ticks survival.
- **AD-14.** The passive Journal is unchanged — opening it still mutates nothing; 5.6 only sets the flags its passive lookup already derives from.
- **Reuse (CLAUDE.md §3).** Extend `JournalController`'s catalog and **reuse `QUEST_ROAD_EAST`** for The Rescue rather than inventing a rescue quest; reuse the `CaptureController` resolve-site + one-shot-flag idiom; reuse `WorldSpine` landmarks (no new geography — the road-head is named as the prison waypoint, not added).
- **Simplicity (CLAUDE.md §2).** Two location-triggered flag-flips over existing families — no quest engine, no world regeneration, no dialogue. Content consumes these gates in 5.7.
- **Build/verify:** `docs/BUILD.md` — `mvn -o clean install`.

### Project Structure Notes

- New file: `core/src/main/java/com/margins/rogue/narrative/ActGateController.java` (alongside `CaptureController`, `JournalController`).
- Edits: `core/src/main/java/com/margins/rogue/narrative/JournalController.java` (register "Follow the Road"), `core/src/main/java/com/margins/MarginScreen.java` (wire `actGate.resolve`).
- New test: `core/src/test/java/com/margins/rogue/ActGateControllerTest.java`.

### References

- [Source: _bmad-output/planning-artifacts/epics.md#Story 5.6: The act-gating quests] — AC-1/AC-2, FR-18, AD-11.
- [Source: _bmad-output/implementation-artifacts/epic-5-context.md#Technical Decisions] — AD-11 two-channel escalation (do not merge), act gating flips flags.
- [Source: _bmad-output/implementation-artifacts/5-5-bond-and-the-shapes-of-loss.md] — the `CompanionLoss` / rejoin-and-lose seams and the "model now, content later" precedent.

## Dev Agent Record

### Agent Model Used

Claude Opus 4.8 (1M context) — create-story 2026-08-14 (autonomous loop).

### Debug Log References

- `mvn -o -pl core test` — BUILD SUCCESS, full suite green (**511** tests, +8 over the 503 baseline).
- First run: 2 red in `ActGateControllerTest` (rescue quest showed `null`, not `COMPLETED`). Root cause: the 2.5 Journal lists only *started* quests, and `QUEST_ROAD_EAST` is started by the Torn-Page read, not by the gate. Fix: `resolveRescue` marks the quest *started* as well as *completed* (completing a quest means Klein lived it — 2.5 Decision 8). Green thereafter.

### Completion Notes List

- **AC-1 (Act 1→2, D2/D3):** new `narrative/ActGateController.resolve` dispatches on `getAct()`. In Act 1 it auto-starts "Follow the Road" (new `JournalController` catalog quest, `QUEST_FOLLOW_THE_ROAD`) so it reads ACTIVE, and completes it + `setAct(2)` + `LINE_ROAD` when `player.getTileX() >= WorldSpine.watchtowerX()` (the Tier-2 easting on the road). Reach is an **easting threshold**, not an exact-tile match — robust to pathing and faithful to "a Tier-2 push east." Pinned by `reachingTheCopperRoadCorridorFlipsActOneToTwo`, `followTheRoadIsActiveInActOneUntilTheCorridorIsReached`, `theActOneGateIsAOneShot`.
- **AC-2 (Act 2→3, D2/D4):** `resolveRescue(state, success)` flips `setAct(3)` on **either** outcome. Success clears `KEY_ALDRIC_CAPTURED` + loss→`NONE` + `activateCompanion(ALDRIC)` (he rejoins); failure leaves him lost. Both mark `QUEST_ROAD_EAST` started+completed. The Act 2 reach-trigger (`player.getTileX() >= roadEndX()`) fires the **default success** path (Aldric still `CAPTURED` ⇒ recoverable); the failure branch is wired + tested and reachable by the deferred prison-encounter content (5.7). Pinned by `reachingThePrisonWithAldricRecoverableRescuesHimAndFlipsTwoToThree`, `aFailedRescueLeavesAldricLostButStillFlipsTwoToThree`, `theRescueGateIsAOneShot`, `noGateFiresInActThree`.
- **Channel (Task 4, D1):** the gate flips `act.current`, which Epic 4's `RunState.enemyCountFor` already reads — `flippingActOneToTwoThroughTheGateFeedsEpicFoursThickerInterior` proves the ramp (tested-but-inert since 4.3) now bites when the gate fires. No change to the channel itself. The **live mid-run reinforcement of the standing map** remains the documented, deferred Epic-5 seam (D1).
- **Wiring (Task 3, AD-1/AD-2/AD-4):** `MarginScreen.submitPlayerAction` calls `actGate.resolve(state)` right after `capture.resolve(state)`, before the save. Stateless controller over persisted flags only — no new `RunState` field (AD-6), self-guarding across save/load.
- **Deferred (unchanged from the spec):** the prison-encounter determinant of rescue success/failure; the failed-rescue loss-shape reconciliation (foreclosed-`CAPTURED` vs `DEAD`); the Act-3 spine + border-crossing win/cordon (5.7); live map reinforcement on flip; Bond/dialogue payloads.

### File List

- `core/src/main/java/com/margins/rogue/narrative/ActGateController.java` (new)
- `core/src/main/java/com/margins/rogue/narrative/JournalController.java` (register "Follow the Road" + doc)
- `core/src/main/java/com/margins/MarginScreen.java` (import + `actGate` field + restart re-init + `resolve` wire)
- `core/src/test/java/com/margins/rogue/narrative/ActGateControllerTest.java` (new)

## Change Log

- 2026-08-14 — created by create-story (autonomous loop). Decisions: D1 wire the gates + act advance now, defer the live map-reinforce-on-flip (4.3/5.5 pattern); D2 two quests on the 2.5 quest-flag family, reuse `QUEST_ROAD_EAST` for The Rescue; D3 name locations against existing `WorldSpine` landmarks (Watchtower = Tier-2 corridor, road-head = prison), no new geography; D4 both rescue branches built + tested, success is the default reach-trigger, the determinant deferred to 5.7; D5 idempotent one-shots over persisted flags, no new field (AD-6); D6 a one-shot `ActGateController.resolve` at the `CaptureController` wire-site (AD-1/AD-2/AD-4). Status → ready-for-dev.
- 2026-08-14 — dev-story (autonomous loop): implemented both gates as `narrative/ActGateController` (stateless, easting-threshold reach), registered "Follow the Road" in `JournalController`, wired `actGate.resolve` into `MarginScreen.submitPlayerAction`. +8 tests (`ActGateControllerTest`) covering both gates, both rescue outcomes, idempotency, Act-3 no-op, and the Epic-4 channel proof. One red→green: `resolveRescue` now marks the rescue quest *started* on completion so it surfaces in the Journal (2.5 Decision 8). Full suite green (503 → 511). No new persisted field; existing regressions untouched. Status → review.

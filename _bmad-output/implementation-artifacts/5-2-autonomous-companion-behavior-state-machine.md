---
baseline_commit: fce188c
---

# Story 5.2: Autonomous companion behavior state machine

Status: done

## Story

As Klein,
I want my companion to act on its own within the turn,
so that it feels alive, not tethered (FR-16, AD-10).

## Acceptance Criteria

**AC-1 — A real behavior state machine, only on player-acted turns.**
**Given** an active companion on a player-acted turn **When** the Companion AI step runs **Then** it acts via a behavior state machine (follow / hold / hide / distract / fight-retreat for combatants; take-cover / flee for non-combatants), only on player-acted turns (AD-5), never merely mirroring my movement.

**AC-2 — Obeys the same detection/noise rules as enemies.**
**Given** the same detection/noise rules as enemies **When** the companion moves or acts **Then** it obeys them — a hidden companion is quiet (emits no noise), a panicking one emits a `NoiseEvent` that can blow my stealth (raises nearby enemies via the existing `NoiseSystem`).

## Scope decisions (author, 2026-08-13 — running the loop autonomously per Justine)

- **D1 — `CompanionBehavior` enum of the seven named states.** `{ FOLLOW, HOLD, HIDE, DISTRACT, FIGHT_RETREAT, TAKE_COVER, FLEE }` — exactly AC-1's list. The companion owns its current behavior (field, default FOLLOW, field-init AD-6). `CompanionSystem` is the single driver: each player-acted turn it computes the behavior and executes it.
- **D2 — The combatant gate is the substantive fix (FR-15).** Today `CompanionSystem.follow` makes *any* active companion charge threats — a non-combatant would fight, which is wrong. 5.2 gates on `CompanionId.isCombatant()`: a **combatant** with a threat → `FIGHT_RETREAT` (the existing charge-and-return-to-station engage, now named); a **non-combatant** with a threat → `FLEE` (run from the threat, panic-noise), and when calm → `TAKE_COVER` (trail the player, never fight). Aldric's behavior is byte-identical to today, so the existing `CompanionAiTest` pins stay green; non-combatants get their own, correct AI.
- **D3 — "fight-retreat" = the existing engage-and-return rhythm (no combat-math change).** The combatant fights the threat and falls back to his rear station when it's gone — that is the retreat, and it already ships. A *critically-wounded tactical break-off* (stop attacking mid-fight to fall back) would re-pin the established "he fights through, takes hits, and wins alone" outcome (`aldricChargesAndFightsAnAlertedEnemyOnHisOwn`), so it is **deferred** (5.4 companion-liability). 5.2 names the state and keeps the math.
- **D4 — HOLD / HIDE are honored standing states; DISTRACT stays a one-shot.** `act` honors a standing HOLD/HIDE (stay put; HIDE is quiet — no noise) rather than autonomously overwriting it — the seam Story 5.3's orders set. In 5.2 those states are only reached by a direct `setBehavior` (standing in for the 5.3 order path); the autonomous transition function never selects them. DISTRACT remains the existing one-shot shout (`CompanionSystem.distract`, emits noise) — the enum lists it for completeness; `act` does not run it as a persistent state.
- **D5 — Panic noise (AC-2).** A `FLEE`ing companion emits a `NoiseEvent` at its tile (`PANIC_NOISE_RADIUS`) every fleeing turn and sets its own `PANICKED` condition — resolved by the existing `NoiseSystem.resolve` later in the same turn (the AD-4 slot order is Companion → … → Noise resolve), so it can raise a nearby UNAWARE enemy to SUSPICIOUS: panic blows Klein's stealth. Every non-flee state (FOLLOW/HOLD/HIDE/TAKE_COVER) emits **no** movement noise (companions are quiet by default today) — so "a hidden companion is quiet" holds.
- **Deferred (→ 5.3/5.4):** player orders that SET hide/hold/distract (5.3); combatant companion combat cost + wounded tactical retreat + the liability economics + death shapes (5.4); richer cover-tile seeking; non-combatant survival tracks.

## Baseline (verify before adding)

- **`CompanionSystem.follow(RunState, List)`** (the AD-4 Companion step, `TurnEngine:326`) already does two implicit states: ENGAGE (nearest ALERTED enemy within `REACTION_RADIUS` of the player → greedy step + strike when adjacent, "Aldric strikes for N!") and FOLLOW (rear-guard station `STATION_DIST` behind the player). It does **not** check `isCombatant` — the bug 5.2 fixes. Rename to `act` (single caller: `TurnEngine:326`; `CompanionAiTest` drives it through `TurnEngine`, not directly).
- **`Companion`** (Story 5.1): `getId()` (`CompanionId`), HP pool, `addCondition/hasCondition` with `Condition{WOUNDED,PANICKED}`. Add a `CompanionBehavior behavior` field + getter/setter.
- **`CompanionId.isCombatant()`** — ALDRIC true; MARA/OLD_FEN/YENNA false. Add a `displayName()` for clean observation lines ("Mara panics!").
- **Detection / Noise**: `Detection{UNAWARE,SUSPICIOUS,ALERTED}`; `RunState.emitNoise(x,y,radius)` enqueues a `NoiseEvent`; `NoiseSystem.resolve` (pipeline, after the companion step) raises in-radius UNAWARE enemies to SUSPICIOUS and points them at the sound. `state.getNoiseQueue()` exposes the pre-resolve queue for a direct unit assertion.
- **`CompanionAiTest`** (7 tests) pins Aldric's trail/engage/return-to-station through `TurnEngine` — must stay green (Aldric = combatant, behavior unchanged). `RunStatePersistenceTest` round-trips the companion (new `behavior` field must field-init).

## Tasks / Subtasks

- [x] **Task 1 — `CompanionBehavior` enum + companion state (AC-1, D1).**
  - [x] 1.1 New `com.margins.rogue.CompanionBehavior { FOLLOW, HOLD, HIDE, DISTRACT, FIGHT_RETREAT, TAKE_COVER, FLEE }`.
  - [x] 1.2 `Companion.behavior` (field-init FOLLOW, AD-6) + `getBehavior()/setBehavior()`. `CompanionId.displayName()`.
- [x] **Task 2 — `CompanionSystem.act` state machine (AC-1, D2/D3).**
  - [x] 2.1 Rename `follow` → `act`; update `TurnEngine:326` (+ the direct `CaptureControllerTest` caller).
  - [x] 2.2 Decide: honor a standing HOLD/HIDE; else `decideBehavior(combatant, threat)` — threat ? (combatant ? FIGHT_RETREAT : FLEE) : (combatant ? FOLLOW : TAKE_COVER). `setBehavior`.
  - [x] 2.3 Execute per state: FIGHT_RETREAT = the existing engage (unchanged — `engage()` helper, "Aldric strikes for N!" via `displayName()`); FOLLOW/TAKE_COVER = rear-station trail (no engage); HOLD/HIDE = stay put (quiet); FLEE = Task 3; DISTRACT = no-op (the shout is `distract`).
- [x] **Task 3 — Flee + panic noise (AC-2, D5).**
  - [x] 3.1 FLEE: `addCondition(PANICKED)`, step one tile away from the threat (occupancy-safe via `stepToward`), `emitNoise(tile, PANIC_NOISE_RADIUS=5)`, one observation line ("<name> panics!").
- [x] **Task 4 — Tests + verification (all ACs).**
  - [x] 4.1 AC-1/D2: a non-combatant (activate MARA) with an ALERTED threat near the player → behavior FLEE and it does **not** strike (contrast Aldric FIGHT_RETREAT); calm non-combatant → TAKE_COVER, never engages an adjacent UNAWARE enemy.
  - [x] 4.2 AC-2/D5: FLEE emits a `NoiseEvent` at the companion's tile (radius = PANIC_NOISE_RADIUS); resolving it via the shared `NoiseSystem.resolve` raises a nearby UNAWARE bystander to SUSPICIOUS (stealth blown).
  - [x] 4.3 AC-2 quiet: a companion in HIDE (set directly) is honored by `act` (stays HIDE, doesn't move, not PANICKED) and emits **no** noise (queue empty).
  - [x] 4.4 Regression: `CompanionAiTest` (Aldric, 7 tests) stays green — combatant behavior byte-identical; full suite green (478 → 484, +6). **Verified:** all green.

## Dev Notes

- **AD-5.** `act` runs only in the player-acted pipeline (unchanged slot) — no companion action on a refused/un-acted turn.
- **AD-9/AD-4.** Panic noise uses the one noise channel (`emitNoise`) and is consumed by the single `NoiseSystem.resolve` after the companion step — no direct enemy mutation from the companion.
- **AD-10/FR-15.** The combatant gate makes non-combatants never fight; only the active companion acts (the abstract three have no body, Story 5.1).
- **AD-3.** The behavior + PANICKED condition are the companion's own state; no player/enemy state is shadowed.
- **Surgical (CLAUDE.md §3).** Aldric's numbers/paths are untouched — the diff is the enum, the combatant gate, the two non-combatant states, and the panic-noise line.
- Build/verify: `docs/BUILD.md` — `mvn -o clean install`.

## Dev Agent Record

### Agent Model Used
Claude Opus 4.8 (1M context) — create-story 2026-08-13 (autonomous loop).

### Debug Log References
- Build caught a hidden direct caller `CaptureControllerTest.follow` (not just `TurnEngine`); renamed to `act`. `mvn -o test` — BUILD SUCCESS, full suite green (484 tests, +6 over the 478 baseline).

### Completion Notes List
- **AC-1 state machine (D1/D2):** new `CompanionBehavior { FOLLOW, HOLD, HIDE, DISTRACT, FIGHT_RETREAT, TAKE_COVER, FLEE }` owned by `Companion`; `CompanionSystem.follow` → `act` computes and executes one state each player-acted turn. The substantive fix is the `isCombatant()` gate: a **combatant** threat-response is `FIGHT_RETREAT` (the pre-existing charge-and-return engage, unchanged) and calm is `FOLLOW`; a **non-combatant** threat-response is `FLEE` and calm is `TAKE_COVER` — non-combatants never fight (FR-15). Pinned by `aCombatantFightRetreatsWhenAThreatIsNear`, `aNonCombatantFleesAndNeverStrikesTheThreat`, `aCalmNonCombatantTakesCoverAndDoesNotFightAnAdjacentEnemy`.
- **AC-2 noise (D5):** a `FLEE`ing companion sets its own PANICKED condition, steps away from the threat, and emits a `NoiseEvent` (`PANIC_NOISE_RADIUS = 5`) at its tile — resolved by the same `NoiseSystem` enemies obey (AD-9), so a nearby UNAWARE enemy rises to SUSPICIOUS: panic blows Klein's stealth. Every non-flee state is quiet; a HIDE order is honored (not autonomously overwritten) and emits no noise. Pinned by `aPanickingCompanionEmitsNoise`, `panicNoiseCanBlowKleinsStealth`, `aHiddenCompanionIsQuietAndItsOrderIsHonored`.
- **Regression:** Aldric's numbers/paths are untouched — `FIGHT_RETREAT` reuses the exact engage logic and the "<name> strikes for N!" line resolves to "Aldric strikes for 3!" via `displayName()`, so all 7 `CompanionAiTest` pins stay green. The new `behavior` enum field field-inits to FOLLOW and round-trips (RunStatePersistenceTest green).
- **D3/D4 scope:** "fight-retreat" = the existing engage-then-return-to-station rhythm (a critically-wounded mid-fight break-off is deferred to 5.4 to avoid re-pinning the established "fights through and wins alone" combat math). HOLD/HIDE are honored standing states — the seam Story 5.3's player orders will set; DISTRACT stays the one-shot `distract()` shout (the enum lists it for completeness). Order-vs-threat priority for HOLD/HIDE is a 5.3 concern.
- **Review:** inline review (Blind/Edge/Acceptance; multi-agent reserved for token budget). No High/Med. Low notes: a companion co-located with the threat would cry out in place (degenerate, harmless — companions never step onto enemy tiles); HOLD/HIDE-vs-threat priority deferred to 5.3; wounded tactical break-off deferred to 5.4.
- **AD-3/AD-4/AD-5/AD-9:** the behavior + PANICKED are the companion's own state; `act` keeps its unchanged pipeline slot (player-acted turns only); panic uses the one noise channel consumed by the single `NoiseSystem.resolve`.

### File List
- `core/src/main/java/com/margins/rogue/CompanionBehavior.java` (new)
- `core/src/main/java/com/margins/rogue/CompanionId.java`
- `core/src/main/java/com/margins/rogue/Companion.java`
- `core/src/main/java/com/margins/rogue/system/CompanionSystem.java`
- `core/src/main/java/com/margins/rogue/system/TurnEngine.java`
- `core/src/test/java/com/margins/rogue/CompanionBehaviorTest.java` (new)
- `core/src/test/java/com/margins/rogue/narrative/CaptureControllerTest.java` (follow → act rename)

## Change Log

- 2026-08-13 — created by create-story (autonomous loop). Decisions: D1 `CompanionBehavior` enum of the seven named states owned by the companion; D2 the `isCombatant()` gate is the substantive fix (non-combatants flee/cover, never fight — Aldric unchanged); D3 "fight-retreat" = the existing engage-and-return rhythm (wounded tactical break-off deferred to 5.4, keeps combat math and `CompanionAiTest`); D4 HOLD/HIDE honored standing states (the 5.3 order seam), DISTRACT stays the one-shot shout; D5 a fleeing companion emits panic noise via the existing channel (blows stealth), every other state is quiet. Status → ready-for-dev.
- 2026-08-13 — dev-story + inline review (autonomous loop): `CompanionBehavior` enum + `Companion.behavior`; `CompanionSystem.follow`→`act` state machine gated on `isCombatant()` (combatant FIGHT_RETREAT/FOLLOW unchanged, non-combatant FLEE/TAKE_COVER never fights); FLEE emits panic noise (blows stealth via `NoiseSystem`), HIDE honored + quiet; `CompanionId.displayName()`. +6 tests (`CompanionBehaviorTest`); `CompanionAiTest` unchanged; full suite green (484). Inline review, no High/Med. Status → done.

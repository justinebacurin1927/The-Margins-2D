---
baseline_commit: 73f4a02
---

# Story 5.4: Companion combat and the liability cost

Status: done

## Story

As Klein,
I want a combatant companion to fight through the same combat authority and cost me to keep,
so that help is never free (FR-17, AD-10).

## Acceptance Criteria

**AC-1 — Companion combat routes through the single `CombatSystem` authority.**
**Given** a combatant companion in FIGHT **When** it attacks **Then** it routes through `CombatSystem` (no second owner of any HP pool), applying at the Companion-AI step.

**AC-2 — Any active companion is a liability: extra food, noise penalty, woundable; non-combatants never fight and must be defended.**
**Given** any active companion **When** it travels with me **Then** it costs extra food, adds a noise penalty to stealth (via its `NoiseEvent`s), and can be wounded; non-combatants (Mara, Old Fen, Yenna) never fight and must be defended.

## Scope decisions (author, 2026-08-13 — running the loop autonomously per Justine)

- **D1 — AC-1 is an authority extraction, not a behavior change.** Today `CompanionSystem.engage` calls `threat.takeDamage(c.getDamage())` inline — the one HP mutation still outside `CombatSystem` (enemy→player and enemy→companion already live in `CombatSystem.enemyPhase`). 5.4 moves it into `CombatSystem.companionAttack(state, companion, target, messages)` (same damage, same "<name> strikes for N!" / "Enemy defeated." lines), so `CombatSystem` is the single owner of every HP pool. Zero behavior change → `CompanionAiTest`/`CombatTest` stay green.
- **D2 — "Extra food" is the party eating Klein's rations, NOT a faster player hunger meter.** A run always spawns the active companion, and the player's hunger is pinned at exactly one tick/turn (`SurvivalTickTest`); accelerating it would retroactively break the survival math. Instead the active companion consumes **one prepared ration from Klein's pack** (COOKED_MEAT, else PRESERVED_FOOD) every `MEAL_INTERVAL` acted turns — a real shared-provisions cost that leaves the player's own meter (and its tests) untouched. No ration when the meal is due → the companion goes hungry and gains its own WOUNDED condition (Story 5.1), warned once on the transition. A dedicated feed action is deferred; provisioning is passive from shared stores.
- **D3 — The noise penalty is a faint `NoiseEvent` when the party moves (AD-10).** An active companion that actually **moves** this turn and is not HIDE/HOLD emits a small `PARTY_NOISE_RADIUS` (2) `NoiseEvent` at its tile — the party isn't perfectly silent (AD-10: "party-stealth penalty is a `NoiseEvent`"). A stationary / held / hidden companion is quiet (waiting-in-place stays sneakable; radius 2 is very local, so it only matters when an enemy is nearly on top of the party). Consumed by the same `NoiseSystem.resolve` (AD-9).
- **D4 — "Woundable" + "non-combatants never fight / must be defended" are ratified.** The companion is already woundable through `CombatSystem.enemyPhase` (an enemy barred by the party strikes the companion; "Aldric falls!" on death), and 5.2's `isCombatant()` gate already makes non-combatants never fight (they flee/take cover — they must be defended). 5.4 ratifies these with tests and ties WOUNDED to a low-HP / unfed companion for observability; no combat-flow change.
- **Deferred (→ 5.5+):** the wounded tactical break-off (a mid-fight retreat maneuver — would re-pin combat outcomes); an order/threat override (should HOLD break under a lethal threat); companion death *shapes* (Captured/Departure/Death — Story 5.5); a dedicated feed action + full companion survival tracks; Bond effects on loyalty.

## Baseline (verify before adding)

- **`CombatSystem`** owns player→enemy (`playerAttack`) and enemy→player / enemy→companion (`enemyPhase`, incl. `companion.takeDamage` + "Aldric falls!"). **Missing:** companion→enemy (still inline in `CompanionSystem.engage`).
- **`CompanionSystem.act`/`engage`** (Story 5.2): `engage` strikes an adjacent threat via `threat.takeDamage(c.getDamage())`. The behavior machine runs each player-acted turn (AD-5) for the active companion only.
- **`Companion`** (5.1/5.2): HP pool, `Condition{WOUNDED,PANICKED}`, `CompanionBehavior`. Add a meal timer for D2.
- **`Inventory`**: `count(type)`, `remove(type, amount)`; `Supply.COOKED_MEAT`/`PRESERVED_FOOD` are the prepared rations (ordinals are the inventory type ids). `RunState.getInventory()`.
- **`RunState.emitNoise(x,y,radius)`** → the one noise channel; `NoiseSystem.resolve` (pipeline, after the companion step) raises in-radius UNAWARE enemies.
- **Tests that must stay green:** `SurvivalTickTest` (exactly one player-hunger tick/turn — D2 protects it), `CompanionAiTest`/`CombatTest` (Aldric combat — D1 keeps it byte-identical), `RunStatePersistenceTest` (companion round-trips — the new int meal field field-inits).

## Tasks / Subtasks

- [x] **Task 1 — AC-1: route companion combat through `CombatSystem` (D1).**
  - [x] 1.1 `CombatSystem.companionAttack(RunState, Companion, RogueEnemy, List)`: `target.takeDamage(companion.getDamage())`, the "<name> strikes for N!" + "Enemy defeated." lines (moved verbatim from `engage`).
  - [x] 1.2 `CompanionSystem.engage` calls `CombatSystem.companionAttack` instead of `threat.takeDamage` directly.
- [x] **Task 2 — AC-2 food: shared-ration upkeep (D2).**
  - [x] 2.1 `Companion` meal timer (`MEAL_INTERVAL=50`, field-init, AD-6) + `tickMeal()`/`resetMeal()`/`setMealTimer()`/`HUNGRY_RETRY`.
  - [x] 2.2 `CompanionSystem.act` runs `feedUpkeep` first (living active companion): a due meal consumes one COOKED_MEAT/PRESERVED_FOOD from the pack (reset timer); if none, set WOUNDED (warn once on the transition) and retry in `HUNGRY_RETRY` turns.
- [x] **Task 3 — AC-2 noise: party-stealth penalty (D3).**
  - [x] 3.1 In `act`, if the companion moved this turn and is not HIDE/HOLD/FLEE (FLEE has its own louder panic noise), `emitNoise(tile, PARTY_NOISE_RADIUS=2)`.
- [x] **Task 4 — AC-2 ratify: woundable + non-combatants defended (D4).**
  - [x] 4.1 Tie WOUNDED to a low-HP (≤⅓) companion in `act` (observability); no combat-flow change. Non-combatants-never-fight already enforced by the 5.2 gate; enemy→companion wounding is already `CombatSystem.enemyPhase`.
- [x] **Task 5 — Tests + verification (all ACs).**
  - [x] 5.1 AC-1: a companion strike damages the enemy through `CombatSystem.companionAttack` (direct call) and via `act`→`engage` (both apply `getDamage()` through the authority).
  - [x] 5.2 AC-2 food: with a ration, a due meal consumes exactly one and the companion is not hungry, and the **player's hunger meter is unchanged**; with none, the companion goes WOUNDED + "is hungry." and retries in `HUNGRY_RETRY`.
  - [x] 5.3 AC-2 noise: a moving active companion emits the faint party `NoiseEvent` (radius 2); a held one does not.
  - [x] 5.4 AC-2 ratify: a badly hurt (1 HP) companion carries WOUNDED after `act`.
  - [x] 5.5 Full suite green (490 → 497, +7), incl. `SurvivalTickTest`/`CompanionAiTest`/detection. **Verified:** all green.

## Dev Notes

- **AD-10 (single HP authority).** After 5.4 every HP-pool mutation (player/companion/enemy) flows through `CombatSystem`; `CompanionSystem` owns movement/behavior and calls the combat authority.
- **AD-9/AD-4.** Both the party noise and the (unchanged) distract/panic noise use the one channel consumed by `NoiseSystem.resolve` after the companion step.
- **AD-5.** Food upkeep and party noise run only on player-acted turns (inside `act`), never on a refused turn.
- **AD-6.** The new meal-timer int field-inits; the companion round-trips unchanged.
- **Simplicity (CLAUDE.md §2/§3).** AC-1 is a pure extraction; the food/noise costs are minimal and, by design, do not touch the player's survival meter or the combat math (so the large existing survival/combat test bodies stay green).
- Build/verify: `docs/BUILD.md` — `mvn -o clean install`.

## Dev Agent Record

### Agent Model Used
Claude Opus 4.8 (1M context) — create-story 2026-08-13 (autonomous loop).

### Debug Log References
- Compile caught an undefined `PARTY_NOISE_RADIUS` (added the constant). `mvn -o test` — BUILD SUCCESS, full suite green (497 tests, +7 over the 490 baseline).

### Completion Notes List
- **AC-1 single HP authority (D1):** `CombatSystem.companionAttack(state, companion, target, messages)` now owns the companion→enemy strike (moved verbatim from `CompanionSystem.engage`); `engage` calls it. After 5.4 every HP-pool mutation (player/companion/enemy) flows through `CombatSystem`. Zero behavior change — `CompanionAiTest`/`CombatTest` stay green. Pinned by `companionAttackAppliesDamageThroughTheCombatAuthority` + `aldricStrikesAnAdjacentThreatThroughEngage...`.
- **AC-2 food (D2):** the active companion eats one prepared ration (COOKED_MEAT else PRESERVED_FOOD) from Klein's pack every `MEAL_INTERVAL`(50) acted turns via `feedUpkeep` in `act`; unfed → WOUNDED + "is hungry." (once) + `HUNGRY_RETRY`(10) retry. Crucially this draws from the **pack, not the player's hunger meter** — so the pinned one-tick/turn survival math (`SurvivalTickTest`) is untouched (verified green). Pinned by `aDueMealEatsOneRation...` (incl. player hunger unchanged) and `withNoRationTheCompanionGoesHungryAndWounded`.
- **AC-2 noise (D3):** a companion that actually moved this turn and isn't HIDE/HOLD/FLEE emits a faint `PARTY_NOISE_RADIUS`(2) `NoiseEvent` — the party-stealth penalty (AD-10), resolved by the shared `NoiseSystem`. Held/hidden = quiet; radius 2 is local enough that no sneak/detection test regressed. Pinned by `aMovingCompanionEmitsAFaintPartyNoise` / `aHeldCompanionMakesNoPartyNoise`.
- **AC-2 ratify (D4):** a low-HP (≤⅓) companion gets the WOUNDED marker in `act`; the enemy→companion wounding (`CombatSystem.enemyPhase`, "Aldric falls!") and non-combatants-never-fight (5.2 `isCombatant` gate) already ship. Pinned by `aBadlyHurtCompanionCarriesTheWoundedMarker`.
- **Review:** inline review (Blind/Edge/Acceptance; multi-agent reserved for token budget). No High/Med. Low notes: feeding does not clear a hunger-set WOUNDED (WOUNDED is overloaded hunger+low-HP; a distinct HUNGRY condition + a recovery path is deferred to 5.5+); the PRESERVED_FOOD fallback is symmetric to COOKED and not separately unit-tested; the wounded tactical break-off / order-threat override remain deferred (not required by 5.4's ACs).
- **AD-5/AD-6/AD-9/AD-10:** food upkeep + party noise run only inside `act` (player-acted turns); the meal-timer int field-inits and round-trips; the party noise uses the one channel consumed by `NoiseSystem.resolve`; the companion HP pool now has a single owner.

### File List
- `core/src/main/java/com/margins/rogue/system/CombatSystem.java`
- `core/src/main/java/com/margins/rogue/system/CompanionSystem.java`
- `core/src/main/java/com/margins/rogue/Companion.java`
- `core/src/test/java/com/margins/rogue/CompanionLiabilityTest.java` (new)

## Change Log

- 2026-08-13 — created by create-story (autonomous loop). Decisions: D1 AC-1 = extract companion→enemy damage into `CombatSystem.companionAttack` (single HP authority, zero behavior change); D2 "extra food" = the companion eats prepared rations from Klein's pack on a cadence (NOT the player's hunger meter, which stays pinned — protects `SurvivalTickTest`), going WOUNDED when unfed; D3 the noise penalty is a faint party `NoiseEvent` on companion movement (quiet when held/hidden, AD-10); D4 woundable + non-combatants-never-fight ratified from existing systems. Wounded break-off, order/threat override, and death shapes (5.5) deferred. Status → ready-for-dev.
- 2026-08-13 — dev-story + inline review (autonomous loop): `CombatSystem.companionAttack` (AC-1, `engage` now routes to it — single HP authority); `Companion` meal timer + `CompanionSystem.feedUpkeep` (AC-2 food: eats pack rations, unfed→WOUNDED, player meter untouched); faint party `NoiseEvent` on movement (AC-2 noise, radius 2); low-HP WOUNDED marker (AC-2 ratify). +7 tests (`CompanionLiabilityTest`); `CompanionAiTest`/`SurvivalTickTest`/detection unchanged; full suite green (497). Inline review, no High/Med (Lows: hunger-WOUNDED not cleared on feed, PRESERVED fallback untested, break-off/override deferred). Status → done.

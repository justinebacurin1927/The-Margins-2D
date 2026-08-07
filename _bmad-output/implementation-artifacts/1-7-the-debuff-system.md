# Story 1.7: The debuff system

Status: done
baseline_commit: b2e81e0

## Story

As Klein,
I want unsafe survival choices to inflict tiered, curable debuffs,
so that scarcity has teeth I must actively answer (FR-8).

## Acceptance Criteria

1. **Bacterial track (contaminated food/water).** **Given** I consume contaminated food/water (a provision whose `drinkRisk()` roll fails on the seeded RNG), **when** the bacterial track triggers, **then** Nausea applies (−30% STR, 30t course); if unmitigated it escalates at the end of its course to Fever (−40% STR, 25t), and at the end of that to Delirium (Paranoia + Vertigo + Crippled, 40t). Diarrhea runs parallel to the whole track (Stage 1: 2× Thirst drain; Stage 2: 3× Thirst+Hunger drain; lethal if ignored — the amplified drain drives the existing parched/starving HP damage).
2. **Toxin track (toxic mushrooms).** **Given** I eat a toxic mushroom, **when** the toxin track triggers, **then** the listed effects apply — Rotgut: instant Nausea + Crippled + Diarrhea Stage 1; Honeymoon: a hidden 60-turn countdown that, at 0, triggers Collapse (Max HP capped at 40% of base until cured).
3. **Cures + persistence.** **Given** an active debuff, **when** I apply the correct cure (Honey/Honeycomb, Bloodvein, cure items), **then** it clears or shortens per the rule; **and** debuffs do NOT clear from turns alone — only eating, drinking, or curing removes them.

**Faithful-to-source notes (do not silently drop):**
- PRD FR-8 adds Diarrhea is "lethal if ignored" and the bacterial chain "escalates" — the courses are escalation timers, not auto-clear timers.
- PRD FR-8 cure map: Honey/Honeycomb cure Sick/Poisoned; Bloodvein Mushroom cures Bloated (at −5 HP, 90% Poison risk); cure items shorten Delirium by 75%; the Honeymoon cap requires a cure item.
- The alcohol-interaction toxin (PRD "latent — only triggers if Ale is consumed afterward") is **deferred** — no Ale item exists in Epic 1 (see Design Decisions).

## Scope & the 1.6↔1.7 seam (read first — prevents over-building)

- **In scope:** the player's debuff model and its tick/cure mechanics, the ConsumptionSystem hook replacement, new Supply entries (mushrooms + cures), serialization, tests. This is the FR-8 core.
- **Out of scope (do NOT build here):**
  - **HUD surfacing of active debuffs** — Story 1.8 owns the HUD. 1.7 only exposes a query (`getActiveDebuffs()` / a `debuffLabel()`-style getter) and writes events to the existing message-log channel (`result.messages`). No screen changes.
  - **Companion conditions** — own debuff state is Epic 5 / AD-10 (the PRD FR-15 seam; architecture spine line 123).
  - **Stamina track** — Diarrhea's PRD "Thirst/Stamina drain" is implemented as **Thirst** (Stage 1) and **Thirst+Hunger** (Stage 2); there is no Stamina stat in Epic 1 (`RoguePlayer` has hunger/thirst/temperature only). Stamina is a design-forward track.
  - **Alcohol-interaction toxin** — deferred (no Ale item; PRD review-prose.md:52 already flags it "unstated").
  - **World-structure/loot acquisition of the new items** — Epic 3 owns the 11 structures + loot tables. 1.7 makes the items exist and be consumed correctly; a minimal floor-loot spawn (dev-tunable weights) so the player can actually find them is sufficient.
- **The replaced seam:** `ConsumptionSystem.consume` currently does `p.hurtRaw(POISON_HARM)` with `// TODO(1.7): route to DebuffSystem for the tiered bacterial/toxin tracks (FR-8).` — 1.7 replaces that flat HP sting with the debuff onset. `POISON_HARM = 6` ("PRD Balance placeholder until Story 1.7's debuffs") is removed. The existing `WaterRiskTest` pins `hurtRaw(6)` on the poisoned roll and MUST be updated to assert the debuff onset instead.

## Design Decisions (the interpretation calls — PRD itself flags several as open)

These are the spots the epics/PRD leave ambiguous. Each gives a **recommended default** so dev-story can proceed without a halt; deviate only with a stated reason.

1. **"Escalates" + "do NOT clear from turns alone" reconciled.** The bacterial stage courses (30t/25t/40t) are **escalation timers**: they tick on acted turns (AD-5) and at 0 the stage **escalates to the next tier** (Nausea→Fever→Delirium) — it never clears. Delirium is **terminal**: its timer is *latched* (does not tick) while untreated, so turns alone can never clear it (AC-3). See Decision 4 for how a cure unlatches it.
2. **Nourish-out.** Eating or drinking clears the **recoverable** bacterial stages (Nausea, Fever) — the PRD "eaten out of, or drunk out of" clause (PRD §2 line 111). Diarrhea is **not** cleared by eating/drinking (that would collapse AC-1's "lethal if ignored"); only cures clear it. Rotgut and the Collapse cap are not cleared by nourishment either.
3. **Diarrhea's mechanical drain (Stamina gap).** Stage 1 = +1 extra `tickThirst()` per acted turn (2× total); Stage 2 = +2 extra `tickThirst()` + 2 extra `tickHunger()` (3× total). The lethality comes entirely from the **existing** parched (−2 HP/5t) and starving damage cadences — no new damage math. Escalation Stage 1→2 after **30t** (dev-tunable, matching Nausea's window).
4. **The "cure items shorten Delirium by 75%" mechanic.** Recommended: consuming a cure item during Delirium sets the timer to `floor(remaining × 0.25)` **and** sets a `deliriumTreated` flag; only a treated Delirium timer ticks (on acted turns) and clears at 0. Untreated Delirium: latched, persists forever. (Simplification option if the dev prefers zero special-casing: a cure item clears Delirium outright — but AC-3's "shorten" wording favors the literal 75% rule; prefer the literal.)
5. **No flat HP sting on the failed roll.** The bacterial onset replaces `hurtRaw(POISON_HARM)` entirely — the debuff *is* the cost. (If playtesting feels toothless, a half-strength sting may be re-added later; not in scope now.)
6. **Cures are never refused on full-ness.** The existing `canEat()`/`canDrink()` refusal in `ConsumptionSystem` (Edge #2-review: refuse when the track is maxed) would block a Well-Fed player from taking medicine. New cure supplies (`isCure()`) bypass the refusal — you can always take a cure.
7. **Rotgut's "instant Nausea"** = begin the full Nausea stage (timer 30) directly — no contamination roll, no extra risk.
8. **Honeymoon re-eat** resets the hidden countdown to 60 (dev-tunable; the simplest sane rule).
9. **Delirium's compound is a bundle on one stage, not three independent debuffs.** While `bacterialStage == DELIRIUM`, the player is simultaneously Paranoia + Vertigo + Crippled. Concrete mechanical hooks (all reuse existing channels, per the "closed shape" convention — architecture spine line 186):
   - **Crippled → movement stumble.** Reuse the Bloated `isSlowed()` 50% channel: while Delirious, MOVE rolls a 50% stumble ("Delirium — you stumble.") exactly like Bloated.
   - **Vertigo → dodge penalty.** Extend the `dodgePercent()` −15% channel (the existing Trembling penalty): while Delirious, add a second −15% to effective instinct (multiplicative with Trembling).
   - **Paranoia → action freeze.** In the MOVE case only, a seeded ~25% roll consumes the turn as a paranoid stagger (no displacement, "Paranoia — you freeze."). Other action kinds are unaffected (minimal mapping — the PRD defines Paranoia only by name).
   - Dev-tunable: the two movement rolls (50% / 25%) and the dodge penalty (−15%); the hooks must stay the existing channels, not new flags.

## Tasks / Subtasks

- [x] **Task 1 — The bacterial track from contaminated food/water (AC: 1)**
  - [x] Add a closed debuff shape to `RoguePlayer`: `BacterialStage` enum (NONE / NAUSEA / FEVER / DELIRIUM), `bacterialTimer` (stage course turns), `deliriumTreated` flag — all field-initialized to the empty state (NONE / 0 / false) so a pre-1.7 save loads non-null-empty (AD-6). Getters for the story's tests.
  - [x] `getStr()` composes the STR penalty: Nausea −30% (×0.70), Fever −40% (×0.60), multiplicative with the existing STARVING Fatigue ×0.65 (`RoguePlayer.java:138-140`). Only the current stage's factor applies (escalation replaces the stage); Delirium applies no STR factor.
  - [x] Replace the `ConsumptionSystem` failed-roll stub (`hurtRaw(POISON_HARM)` + the `// TODO(1.7)` comment) with a route to the bacterial track: on a failed `drinkRisk()` roll, begin Nausea (timer 30) + Diarrhea Stage 1 and emit an onset message. Remove `POISON_HARM`. Keep the roll on `state.rng()` (AD-5).
  - [x] New `DebuffSystem.tick(RunState, List<String>)` — wired into the TurnEngine acted branch immediately **after** `ThirstSystem.tick(player)` (so Diarrhea's amplified drain lands on the same turn and before `checkLastStand`, preserving the AD-5 reprieve for a lethal drain). Each acted turn it: ticks the bacterial stage timer and escalates at 0 (Nausea→Fever at 30→25... precisely: Nausea timer 30, escalates to Fever timer 25; Fever timer 25, escalates to Delirium timer 40); emits an escalation message per transition. Delirium's timer does NOT tick while untreated (Decision 4).
  - [x] Update `WaterRiskTest`'s poisoned-roll assertion from `hurtRaw(6)` to the bacterial onset (Nausea + Diarrhea active, no flat HP harm).

- [x] **Task 2 — Diarrhea's parallel drain (AC: 1)**
  - [x] Add `DiarrheaStage` enum (NONE / STAGE_1 / STAGE_2) + `diarrheaTimer` on `RoguePlayer` (field-init NONE / 0). The bacterial onset starts STAGE_1 (timer 30).
  - [x] `DebuffSystem.tick`: when Diarrhea is active, apply the extra drain by calling the existing public `tickThirst()` / `tickHunger()` additional times (Stage 1: +1 thirst; Stage 2: +2 thirst, +2 hunger) — reuse, don't re-implement. Escalate STAGE_1→STAGE_2 at 0 with a "Diarrhea worsens." message.
  - [x] Lethality test: an unmitigated Stage-2 Diarrhea under Parched/Starving kills through the existing HP cadences (pinned seed).

- [x] **Task 3 — The toxin track + mushroom supplies (AC: 2)**
  - [x] Append to `Supply` (last, AD-6 ordinal safety; single-identity self-evident like the Story 1.5 provisions): `TOXIC_MUSHROOM` ("Toxic Mushroom") and `HONEYMOON_MUSHROOM` ("Honeymoon Mushroom"), with matching `TrueIdentity` entries whose `apply()` is nourishment-free. Add `Supply.toxin()` (enum NONE / ROTGUT / HONEYMOON) and `isProvision() == true` for both.
  - [x] `ConsumptionSystem.consume`: after the provision's `apply()`, route `toxin() != NONE` to the toxin track (deterministic — no roll) instead of a `drinkRisk()` roll. A provision carries either a bacterial risk or a toxin, never both (mushrooms: risk 0, toxin set).
  - [x] `DebuffSystem` toxin handling: **Rotgut** = begin Nausea (timer 30, full course) + Crippled (the Delirium movement bundle, applied as a standalone — see Task 4 hook) + Diarrhea Stage 1. **Honeymoon** = start a hidden 60-turn countdown (acted-turn ticking, no message reveals it — the onset message is deliberately sweet, e.g. "Sweet as honey..."); at 0 → **Collapse**: set `maxHpCapPercent = 40`, clamp HP to the capped max, emit the collapse message.
  - [x] Max-HP cap plumbing: `RoguePlayer.getMaxHp()` returns `maxHp * 40 / 100` (floor, min 1) while capped; `heal()` and `reviveTo()` clamp to `getMaxHp()` (today they clamp to `maxHp` directly — `RoguePlayer.java:206,466`). Base `maxHp` is 20, so Collapse = 8 effective.

- [x] **Task 4 — Delirium's compound + movement/stat hooks (AC: 1, 2)**
  - [x] While `bacterialStage == DELIRIUM` (or Crippled is active from Rotgut): MOVE gains the stumble + freeze rolls (Decision 9) — extend the existing Bloated-stumble branch in `TurnEngine`'s MOVE case (`TurnEngine.java:36-48`) rather than adding new flags, keeping the "closed shape" convention.
  - [x] `dodgePercent()` (`RoguePlayer.java:172-182`): add the Vertigo −15% effective-instinct penalty while Delirious (multiplicative with Trembling).
  - [x] The Crippled-on-Rotgut bundle: Rotgut applies "Crippled" without full Delirium. Give the movement/dodge bundle a single internal predicate (e.g. `isCrippled()` = Delirious OR Rotgut) so the MOVE/dodge hooks share it — one source of truth, no ad-hoc flags.

- [x] **Task 5 — Cures + nourish-out (AC: 3)**
  - [x] Append to `Supply` (last): `HONEY` ("Honey"), `HONEYCOMB` ("Honeycomb"), `BLOODVEIN_MUSHROOM` ("Bloodvein Mushroom"), `HERBAL_CURE` ("Herbal Cure") with matching `TrueIdentity` entries; add `Supply.isCure()`. Cure consumption routes through the normal `ConsumptionSystem` path with `drinkRisk() == 0` (deterministic) — **except** Bloodvein, which sets `drinkRisk() = 90` so its poison risk rides the existing seeded roll (Decision/reuse — the failed roll then starts the bacterial track, exactly like contaminated food).
  - [x] Cure effects (via `TrueIdentity.apply`, no RNG):
    - HONEY / HONEYCOMB: clear the bacterial track if Nausea or Fever; clear the Rotgut toxin effects; clear Diarrhea. (Honeycomb additionally `p.eat(10)` — a comb is edible.)
    - BLOODVEIN_MUSHROOM: `p.hurtRaw(5)`; clear the Bloated slow (`bloatedSlowTurns = 0`); the 90% bacterial risk rides the ConsumptionSystem roll.
    - HERBAL_CURE (the generic "cure item"): clears Nausea/Fever; on Delirium applies Decision 4 (timer ×0.25 + treated); lifts the Collapse `maxHpCapPercent` (to 0).
  - [x] Nourish-out in `RoguePlayer.eat()` / `drink()`: when invoked while Nausea or Fever is active, clear the stage (pure state change). `ConsumptionSystem.consume` emits the "settles your stomach" message after `apply()` when a stage was cleared (it can compare before/after). The mystery-supply USE path (TurnEngine) that calls `eat()` also clears the state; emitting there is a nicety, not an AC.
  - [x] Cures bypass the `canEat()`/`canDrink()` refusal (Decision 6): `ConsumptionSystem` refuses only non-cure provisions when the track is maxed.

- [x] **Task 6 — Serialization, restart, and tests (AC: all)**
  - [x] All new `RoguePlayer` fields serialize via the established AD-6 pattern (field-init defaults; `RunStatePersistenceTest` additions): debuff round-trip AND pre-1.7-save migration (`root.remove("bacterialStage")` … → loads NONE / 0 / false, never a crash). `RunState.restart()` already builds a fresh `RoguePlayer` (`RunState.java:127`), so a new run clears debuffs for free — add a restart assertion.
  - [x] New `DebuffSystemTest` (headless, seeded RNG — see `SurvivalTickTest`/`WaterRiskTest` for the pattern): onset on failed roll; escalation Nausea→Fever→Delirium; Delirium persists untouched (timer never advances) while untreated; cure item shortens ×0.25 + clears at 0; nourish-out clears Nausea/Fever but NOT Delirium/Diarrhea; Diarrhea Stage 1/2 drain amplification (observable thirst/hunger deltas) + Stage 1→2 escalation + lethality; Rotgut composition; Honeymoon hidden countdown (no message reveals it) → Collapse cap + clamp + HERBAL_CURE lift; Bloodvein −5 HP + 90% roll + Bloated clear; Honey clears Sick/Poisoned; cure-while-Well-Fed not refused; wall-bump commits no debuff tick (AD-5 honesty).
  - [x] Full suite green (`mvn -o clean install`), no regressions in `SurvivalTickTest`, `WaterRiskTest`, `RunStatePersistenceTest`, `TorchTest`, `TemperatureSystemTest`.

### Review Findings

Adversarial review (Blind Hunter + Edge Case Hunter + Acceptance Auditor; 2026-08-08). Three layers ran in fresh contexts against `git diff HEAD -- core/`; each finding was verified against the source and empirically probed before reporting. Summary: **5 patch, 3 defer, 3 dismissed** (2 decision-needed resolved by the user → 1 patch, 1 defer).

**Decision needed (2) — awaiting the user's call:**

- [x] [Review][Patch] **Honeymoon re-eat resets the hidden countdown** [core/.../RoguePlayer.java beginHoneymoon] — Edge Case Hunter (Low) → resolved by Justine: **single active countdown**. `beginHoneymoon()` starts the countdown only when not already poisoned; a re-dose is a harmless no-op (item spent, countdown untouched). Blocks the hoard-to-defer exploit AND the post-collapse re-arm. Decision 8's reset is the only behavior removed. **Applied** (2026-08-08) + `honeymoonReDoseDoesNotResetOrRearmTheCountdown`.
- [x] [Review][Defer] **The E key can auto-feed a poison mushroom** [core/src/main/java/com/margins/MarginScreen.java:107] — Blind Hunter (Low) → resolved by Justine: **deferred to Story 1.8**. The quick-eat is a documented stopgap and the story's scope discipline forbids building selection in 1.7; the auto-poison hazard is accepted for one story. Carried as a 1.8 input: the HUD item selection must give deliberate use of the new mushroom/cure supplies.

**Patch (4) — awaiting the user's call on how to handle:**

- [x] [Review][Patch] **Diarrhea's amplified drain accelerates the Well Fed regen (the disease heals you)** [core/.../system/DebuffSystem.java:94] — blind+edge, the top finding, two independent empirical probes: Stage-2 Diarrhea's extra `tickHunger()` calls re-enter `tickHunger()`'s Well Fed block, so a sick Well-Fed player regenerates at 3× cadence and sheds the Bloated slow 3× faster — the opposite of AC-1's "lethal if ignored". Fix: a drain-only `RoguePlayer.drainHunger()` (countdown + Starving cadence, no Well Fed block) for the amplified drain. **Applied** (2026-08-08) + `stageTwoDiarrheaDoesNotAccelerateWellFedRegenOrBloated`.
- [x] [Review][Patch] **A second herbal cure double-shortens a treated Delirium** [core/.../RoguePlayer.java:550] — edge: `treatDelirium()` re-applies ×0.25 even after `deliriumTreated` (40→10→2→0); the flag's purpose is a single unlatch. Fix: early-return when already treated. **Applied** (2026-08-08) + `aSecondHerbalCureDoesNotDoubleShortenDelirium`.
- [x] [Review][Patch] **A full + hydrated sick player is blocked from the nourish-out recovery** [core/.../system/ConsumptionSystem.java:40] — edge: the full-refusal fires before `eat()`/`drink()`, so a WELL_FED+HYDRATED Nausea/Fever player can never settle the sickness by eating/drinking (AC-3). Fix: skip the refusal when the player is sick (nourish-out still helps). **Applied** (2026-08-08) + `aSickPlayerCanStillEatOrDrinkWhenFullToNourishOut`.
- [x] [Review][Patch] **Toxin mushrooms are refused when Well Fed** [core/.../system/ConsumptionSystem.java:40] — blind: a zero-nourishment mushroom triggers "Not hungry enough to eat that." and the deterministic toxin track is unreachable while full — contradicts "the player chose to eat it". Fix: bypass the refusal for toxin items (same guard-line change as the previous patch). **Applied** (2026-08-08) + `toxicMushroomCanBeEatenWhenWellFed`.

**Defer (2):**

- [x] [Review][Defer] **`getStr()` floor hides the Nausea-vs-Fever STR distinction at base stat 5** [core/.../RoguePlayer.java:171] — blind+edge: `floor(5×0.70) == floor(5×0.60) == 3`, so Fever is indistinguishable from Nausea (and Delirium applies no factor) at the only reachable STR. Documented tradeoff in the story's own test; the distinction materializes once Story 3.5 horizontal progression raises STR. Deferred — balance tuning for 3.5, not a 1.7 logic bug.
- [x] [Review][Defer] **No composite debuff query seam (`getActiveDebuffs()`/`debuffLabel()`) for the Story 1.8 HUD** — auditor: 1.7 deliberately keeps the debuff shape closed on `RoguePlayer` (spine line 186) and out of HUD scope; 1.8 will need a composite query/label seam. Deferred — carry to Story 1.8 (the HUD owns it).

**Dismissed (3):** Bloated `isSlowed()` shadowing the Crippled freeze/stumble distribution (chained independent rolls are a valid composition; the net movement penalty still holds) · herbal cure leaving Diarrhea/Rotgut running (by-design two-cure split — honey vs herbal) · the `isCrippled()`/`isDelirious()` predicate split (documented deviation, auditor-accepted).

## Dev Notes

### Current state (what exists, what to ratify, what to preserve)

- **The ConsumptionSystem hook is the trigger point.** `ConsumptionSystem.consume` is the *single path for ALL provisions* (its class doc states this as a contract) — every EAT routes through it (`TurnEngine.java:63-69`). The failed-roll site is the `// TODO(1.7)` stub: `p.hurtRaw(POISON_HARM)`. Keep the single-path contract: mushrooms and cures are provisions and route through it; the toxin and cure logic hangs off `Supply.toxin()` / `Supply.isCure()`, not a new consume path.
- **The "closed shape" convention** (architecture spine line 186): debuffs live as a closed enum/stack on the entity's Status block — no ad-hoc flags. `RoguePlayer` is the player's Status block; the debuff shape is the `BacterialStage`/`DiarrheaStage` enums + their small int/flag fields. The movement/dodge hooks must read one predicate (`isCrippled()`), not scattered booleans.
- **Existing penalty channels to reuse (do NOT invent parallel ones):**
  - STR: `getStr()` already applies STARVING Fatigue ×0.65 (`RoguePlayer.java:138-140`) — compose the debuff factors here.
  - Dodge: `dodgePercent()` already applies Trembling −15% (`RoguePlayer.java:172-182`) — add Vertigo as a second source.
  - Movement stumble: MOVE's `isSlowed()` 50% roll (`TurnEngine.java:40-42`) — extend for Crippled/Delirium.
  - Lethal drains: Parched −2 HP/5t and the Starving cadence already kill — Diarrhea only *accelerates* them by extra `tickThirst()`/`tickHunger()` calls.
- **Turn pipeline placement (AD-4):** the acted branch order is Hunger → Thirst → Temperature → Spoilage → clock → Detection → Companion → Enemy → Torch → Light → Noise → checkLastStand → FOV. `DebuffSystem.tick(state, messages)` slots immediately after `ThirstSystem.tick(player)` — its amplified drain lands before `checkLastStand`, so a lethal accelerated drain still honors the one-per-run Last Stand reprieve (AD-5, pinned by `SurvivalTickTest.lethalTemperatureHonorsLastStandReprieve`).
- **Restart:** `RunState.restart()` → `generateFloor()` → `player = new RoguePlayer(...)` (`RunState.java:127`), so debuff state resets with a fresh player automatically. Only an assertion is needed.

### Placement rationale (AD-3)

- Debuff state lives on `RoguePlayer` (the Status block owner per the closed-shape convention). `DebuffSystem` (new, `system/` package, `System`-suffix naming) is the AD-4 pipeline step that owns *ticking and escalation* — mirroring `HungerSystem`/`ThirstSystem`/`TemperatureSystem`, which tick `RoguePlayer` state. The architecture's own review-adversarial already prescribes "new per-track pipeline steps inserted in fixed order (…, DebuffSystem)" (architecture review-adversarial.md:102) and flags "Debuffs (FR-8) — data shape (tiered stacks), tick cadence, and cure paths ungoverned" (:117) — this story closes that gap.

### Serialization — the pattern that applies directly (AD-6)

- Same as every prior story: `RunState`/`RoguePlayer` serialize via libGDX Json (`usePrototypes(false)`); a save predating a field loads the field's initializer. New fields: field-init NONE / 0 / false, so a pre-1.7 save loads empty-but-non-null. Enum serialization by name is the established norm (`HungerStatus`, `ThirstStatus`). Copy the migration-test shape from `RunStatePersistenceTest.preStory16SaveLoadsWithCampfireLightAndNoTorch` (remove the new keys from a serialized root, assert empty-safe load). The no-arg `RunState` ctor delegates to the seeded ctor (nanoTime-rolled weather wart) — irrelevant here because debuffs are pure field-init.

### Scope discipline (CLAUDE.md §2/§3)

- Touch only the seams listed. Do NOT build the HUD (1.8), companion conditions (Epic 5), a Stamina track, Ale/alcohol toxin, or world-structure loot. Do NOT add immediate-flat-HP poison damage on the bacterial roll (that was the placeholder). Do NOT touch `Supply` ordinals already in the wild — append only (AD-6). Remove only what your changes orphan (`POISON_HARM`, the `// TODO(1.7)` comment, the dead `hurtRaw(6)` test assertion).
- If a design decision above needs re-tuning (timer values, dodge %, stumble %), keep the hook shape and change the constant — don't add configurability that wasn't requested.

### Testing standards

- Headless JUnit 5, no libGDX types (AD-2) — the existing core test net is the model. Seeded RNG determinism: construct `RunState(seed)` and pin where a roll matters (see `SurvivalTickTest`'s `setWeather(Weather.CLEAR)` pin precedent and `RoguePlayer.setSkill` test-hook precedent — if a test needs to force a bacterial roll, prefer a deterministic `drinkRisk()` supply, e.g. SPOILED_MEAT at 90%).
- Honesty pins mirroring the survival-clock tests: a wall-bump must not tick any debuff timer (AD-5).

### Project Structure Notes

- New files: `core/src/main/java/com/margins/rogue/system/DebuffSystem.java`; `core/src/test/java/com/margins/rogue/DebuffSystemTest.java`.
- Modified: `RoguePlayer.java` (debuff shape + `getStr`/`dodgePercent`/`isCrippled`/`getMaxHp`/`heal`/`reviveTo`/`eat`/`drink` nourish-out), `ConsumptionSystem.java` (bacterial onset route + toxin route + cure bypass), `Supply.java` (6 appended entries + `toxin()` + `isCure()`), `TrueIdentity.java` (6 appended identities), `TurnEngine.java` (MOVE stumble/freeze + DebuffSystem pipeline slot), `WaterRiskTest.java`, `RunStatePersistenceTest.java`.
- Naming convention (`System`-suffix pipeline systems, `Rogue`-prefix entities) and the single-mutation-path rule (`TurnEngine.advance` → systems → mutate `RunState`, AD-3/AD-4) apply unchanged.

### References

- [Source: epics.md#Story-1.7] — the three ACs (bacterial chain with parallel Diarrhea; toxin track Rotgut/Honeymoon→Collapse; cures clear/shorten, no turn-clear).
- [Source: PRD §2 line 111, §4.2 FR-8 lines 193-195] — debuff definition; the full bacterial/toxin/cure consequence list; "persist until eaten, drunk, or cured."
- [Source: PRD review-prose.md:52,54] — the two open questions carried as Decisions 8/9: alcohol-toxin unstated (deferred), Honeymoon-cure-item unnamed (→ HERBAL_CURE).
- [Source: architecture spine AD-4/AD-5, line 186 closed-shape convention, line 123 companion seam] — pipeline placement, acted-turn honesty, the "no ad-hoc flags" debuff shape, FR-15 companion conditions deferred to Epic 5.
- [Source: architecture review-adversarial.md:102,117] — the prescribed DebuffSystem per-track step and the "tiered stacks / tick cadence / cure paths ungoverned" gap this story closes; reconcile-prd.md (FR-8 → AD-4, AD-5).
- [Source: story-1.6 temperature-and-the-campfire-torch.md] — the `// TODO(1.7)` hook, `POISON_HARM` placeholder note, the field-init serialization pattern, the `isSlowed()`/Trembling penalty channels, the acted-branch pipeline order.
- [Source: story-1.5-food-water-and-two-step-purification.md] — the single-path provision contract, `drinkRisk()` taxonomy, the `setSkill` test-hook precedent.

## Dev Agent Record

### Agent Model Used

Claude Opus 4.8 (via Claude Code, bmad-dev-story)

### Implementation Plan

Followed the 6 story tasks in order, red→green against the codebase seams.

1. **Bacterial track** — Added the closed debuff shape to `RoguePlayer` (`BacterialStage`/`DiarrheaStage` enums + timer/flag fields, all field-init empty for AD-6), `getStr()` STR composition, replaced `ConsumptionSystem`'s `hurtRaw(POISON_HARM)` stub with `DebuffSystem.applyBacterial`, wrote `DebuffSystem` (new `system/` step) with escalation timers, and wired it into the acted branch after `ThirstSystem` (AD-4, preserving the AD-5 Last-Stand reprieve).
2. **Diarrhea** — Stage 1 (2× thirst) / Stage 2 (3× thirst+hunger) as extra calls to the existing `tickThirst()`/`tickHunger()` — no new damage math; lethality rides the Parched/Starving cadences.
3. **Toxin track** — Appended `TOXIC_MUSHROOM`/`HONEYMOON_MUSHROOM` to `Supply` (last, AD-6), `Supply.Toxin` enum, deterministic `DebuffSystem.applyToxin` (Rotgut composition; Honeymoon hidden countdown → Collapse max-HP cap at 40% via `getMaxHp()`/`heal()`/`reviveTo()`).
4. **Delirium compound** — `isCrippled()` single predicate (Delirious OR Rotgut) driving MOVE stumble+freeze in `TurnEngine`; Vertigo −15% in `dodgePercent()`.
5. **Cures + nourish-out** — Appended `HONEY`/`HONEYCOMB`/`BLOODVEIN_MUSHROOM`/`HERBAL_CURE`, `Supply.isCure()`, cure effects in `TrueIdentity.apply` (Bloodvein's 90% risk rides the existing roll), nourish-out in `eat()`/`drink()`, and the cure bypass of the canEat/canDrink refusal.
6. **Serialization + tests** — Round-trip, pre-1.7 migration, restart, and append-migration (IdentifyMap reconcile) tests; full 22-test `DebuffSystemTest`.

Two red-phase test-logic fixes (not production bugs): the treated-Delirium loop re-read the shrinking timer (captured the count first) and the Parched-setup killed the test player (healed after reaching PARCHED).

### Debug Log References

- Compile: missing `TurnResult` import in `DebuffSystemTest` → added. (one-shot)
- Full suite: 141 → 167 tests green (`mvn -o clean install`), zero failures.

### Completion Notes List

- ✅ Implemented the full FR-8 debuff system: bacterial escalation chain (Nausea→Fever→Delirium) with the parallel Diarrhea drain, the mushroom/toxin track (Rotgut, Honeymoon→Collapse), the four cures + nourish-out, and the Delirium movement/stat hooks — all on the closed-shape `RoguePlayer` Status block (spine line 186).
- ✅ `ConsumptionSystem`'s `// TODO(1.7)` and `POISON_HARM` removed — the bacterial onset (no flat HP) replaced them; `WaterRiskTest` re-pinned to the debuff onset.
- ✅ Design decisions held: escalation timers + latched untreated Delirium (AC-3), Stamina gap → thirst/hunger amplification (Decision 3), cure-bypass of the full-refusal (Decision 6), Bloodvein 90% rides the roll, Honeymoon onset deliberately sweet. Deliberate deviation noted: the Crippled bundle drives MOVE stumble+freeze, while Vertigo (dodge) is Delirium-only per Decision 9.
- ✅ AD-6 honored: 6 Supply/TrueIdentity entries appended last; pre-1.7 saves load empty-but-non-null; IdentifyMap grows the binding for the appended ordinals (no RNG draw).
- ✅ 167 tests green (141 → +26), no regressions across SurvivalTickTest/WaterRiskTest/TorchTest/TemperatureSystemTest/RunStatePersistenceTest.
- Out of scope held: HUD (1.8), companion conditions (Epic 5), Stamina/Ale (deferred), world-structure loot (Epic 3) — the existing uniform floor-loot spawn now covers the new types.

### File List

- `core/src/main/java/com/margins/rogue/system/DebuffSystem.java` (NEW)
- `core/src/main/java/com/margins/rogue/RoguePlayer.java` (debuff shape + STR/dodge/maxHp/heal/reviveTo/eat/drink hooks)
- `core/src/main/java/com/margins/rogue/system/ConsumptionSystem.java` (bacterial onset route + toxin route + cure bypass; removed `POISON_HARM`)
- `core/src/main/java/com/margins/rogue/item/Supply.java` (6 appended types + `Toxin` enum + `toxin()` + `isCure()` + `isProvision`/`drinkRisk` updates)
- `core/src/main/java/com/margins/rogue/item/TrueIdentity.java` (6 appended identities)
- `core/src/main/java/com/margins/rogue/system/TurnEngine.java` (MOVE Crippled stumble/freeze + DebuffSystem pipeline slot)
- `core/src/test/java/com/margins/rogue/DebuffSystemTest.java` (NEW, 22 tests)
- `core/src/test/java/com/margins/rogue/WaterRiskTest.java` (poison-roll assertion → bacterial onset)
- `core/src/test/java/com/margins/rogue/state/RunStatePersistenceTest.java` (debuff round-trip, pre-1.7 migration, restart, append-migration)

## Change Log

- 2026-08-07 — Story authored (create-story): 6 tasks covering the bacterial/toxin/cure pipelines, 9 flagged design decisions resolving the PRD's open questions, seams read against the 1.6 codebase.
- 2026-08-07 — Story implemented (dev-story): full FR-8 debuff system (bacterial chain, Diarrhea, toxin track, cures, nourish-out, Delirium hooks); 141 → 167 tests green; status set to review.
- 2026-08-08 — Senior Developer Review complete: **Approve (5 patches applied, 3 deferred, 3 dismissed)**. Top finding F-01 (High, blind+edge, two independent probes): Stage-2 Diarrhea's amplified drain re-entered the Well Fed regen block, healing a sick Well-Fed player at 3× cadence — fixed with a drain-only `RoguePlayer.drainHunger()`. Also: `treatDelirium` ×0.25 single-apply guard; ConsumptionSystem refusal now bypasses sick (nourish-out) and toxin (chosen poison) consumes; Honeymoon single-active-countdown (Justine's call, overriding Decision 8's reset). 167 → 172 tests green; status set to done.

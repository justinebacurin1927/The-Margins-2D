---
scope: The Margin Remake — architecture spine adversarial review #2
target: ARCHITECTURE-SPINE.md (2026-08-06 revision)
reviewer: adversarial agent (Opus 4.8)
method: pair-construction — for every AD, build two one-level-down units that each obey every AD to the letter and check whether they build the same system
grounding: core/src/main/java/com/margins/rogue/** (RunState, SaveService, TurnEngine, CombatSystem, DetectionSystem, NoiseSystem) where ADs are `[ADOPTED]`
date: 2026-08-06
---

# Adversarial Review 2 — The Margin Architecture Spine

## Verdict: REVISE

The prior review's fixes are real and mostly sound: AD-8's retirement list is concrete and names actual code; AD-10's single-combat-authority direction is right; AD-7 per-companion Bond is internally consistent with AD-10's keyed identity; AD-14's dialogue-safe-pause is coherent with AD-5's shared turn; AD-12's always-walkable border / defended-cordon win gate is the correct honesty fix; AD-17/AD-18 are coherent standalone. **But the revisions introduced or exposed five NEW holes**, the worst being a direction contradiction inside the same escalation ramp (AD-11 vs AD-12) and an under-specified combat-application point (AD-10's "same mutation path as the player" has no pipeline position). None of these is a re-introduction of the previously-fixed findings.

---

## F1 — CRITICAL — AD-11 ↔ AD-12: the same escalation ramp both thickens and thins the border cordon

**The contradictory text.**

- AD-11: "the occupation-escalation ramp reads those flags **to thicken Giliman presence per act**."
- AD-12: "in Act 1 the cordon is fully staffed (near-certain death), and the occupation-escalation ramp (AD-11) **thins it only as the war consolidates east**."

AD-12 explicitly *imports* AD-11's ramp and then states it thins the border. AD-11 states the ramp thickens Giliman presence per act, with no carve-out for the border. The border cordon is "Giliman presence," so AD-11 literally covers it.

**Pair constructed.**

- **Unit A (AD-11-literal):** `OccupationRamp.escalate(act) -> presenceMultiplier {1.0, 1.5, 2.2}` applied uniformly to every Giliman, *including* the NW border cordon garrison. Act 1 = 100 cordon staff (near-certain death), Act 3 = 220. The win gate gets strictly harder each act. This makes AD-12's "thins it as the war consolidates east" unimplementable, and inverts the "home is close enough to taste and lethal to reach" premise (the most-lethal-to-reach moment becomes Act 3, the act AD-12 says the run is survivable with Act-3 readiness). Unit A is a good-faith reading: the ramp is one function, AD-11 says it thickens, AD-12 is the downstream consumer of the same function.
- **Unit B (AD-12-literal):** the ramp has two channels — `BorderCordon.garrison(act) -> {100, 60, 25}` (thins as the war consolidates east) and off-border presence `{1.0, 1.5, 2.2}` (thickens). This is the evidently intended design.

Both units claim to be "the occupation-escalation ramp" that AD-12 binds to AD-11. They produce **opposite difficulty curves for the same named system**. Two teams building from the spine text will build incompatible games — one where the win gate eases over the run, one where it hardens.

**Fix (which AD to tighten):** AD-11 must define the ramp as two explicit channels: (a) off-border/eastward Giliman presence thickens per act; (b) the NW border cordon is a *distinct output* that thins as acts advance (war consolidates east), scaled by story flags, not by act index alone. AD-12 should cite that carve-out instead of re-deriving "the ramp thins it." Also state whether the cordon reads the same flags AD-11's ramp reads, or a subset.

---

## F2 — HIGH — AD-10 ↔ AD-4: "the same mutation path the player uses" has no pipeline position, and combat-ownership scope covers only enemy HP

**The gap.** AD-10 says "A companion's FIGHT issues actions through CombatSystem — the same mutation path the player uses." But AD-4's pipeline never says *where* player-initiated combat damage is applied. The step is named "Enemy AI (Combat)" — the parenthetical "(Combat)" invites the reading that combat resolution happens inside that step. The brownfield code (which AD-4 `[ADOPTED]` as "pipeline shape only") applies the player's attack *at action time* (`TurnEngine.java:48` calls `CombatSystem.playerAttack` synchronously inside the PlayerAction switch) and enemy attacks in `CombatSystem.enemyPhase` later in the same turn. AD-10 does not restate either position.

**Pair constructed.**

- **Unit A (deferred resolution):** PlayerAction and Companion-FIGHT only enqueue `AttackIntent`s; `CombatSystem.resolveAll()` runs once, inside the "Enemy AI (Combat)" step, resolving player + companion + enemy attacks together. Consequence: an enemy the player attacked is still at full HP when **Detection** (step 3) and **Companion AI** (step 4) run that same turn — detection reads pre-combat HP, companion FIGHT target selection runs against pre-combat state. A companion can kill an enemy before the Enemy-AI step lets it act.
- **Unit B (point-of-action, matches the brownfield):** player attack applies during PlayerAction, companion FIGHT applies during the Companion-AI step, enemy attacks during the Enemy-AI step — all routed through `CombatSystem` (single class, so AD-10's "no second owner of enemy HP" holds literally). Consequence: the target is already wounded/dead by the time Detection and Companion AI run; a player-killed enemy never reaches the Enemy-AI step.

Both obey AD-4 (fixed order) and AD-10 (single authority *class*). The observable game differs: **whether an enemy you attacked gets to act/be-seen-as-wounded in the same turn**, and **whether companion kills land before or after enemy actions**. This is exactly the incompatible-pair the mandate asks for, and the ambiguity is *new* because AD-10's fix introduced the companion-attack path without pinning its position.

**Second, narrower hole in the same AD:** AD-10's ownership clause is scoped to enemy HP only ("never mutates an enemy's HP directly"; "No second owner of enemy HP exists"). Companion and player HP are left unguarded. A builder can have `HungerSystem` directly decrement a starving companion's HP (natural: hunger is a survival track, not "combat") while another builder routes all companion HP damage through CombatSystem — both obeying the letter of AD-10. Two owners of companion HP is exactly what the "single owner" clause claims to prevent; it just forgets to include the other two HP pools.

**Fix (which AD to tighten):** AD-4/AD-10 must pin (a) the combat application point and dead-before-act semantics — e.g., "CombatSystem resolves all turn combat once, in the Enemy AI (Combat) step; no HP mutation occurs in PlayerAction / Companion-AI / Hunger steps," or alternatively "player attack applies at action time; companion FIGHT at the Companion-AI step; enemy at the Enemy-AI step — all via CombatSystem; a dead agent never acts later in the turn" — and (b) extend the single-owner clause to companion and player HP, not just enemy HP.

---

## F3 — HIGH — AD-8 ↔ AD-6: floor-descent retirement leaves the save format unversioned and silently migratable

**The gap.** Today `RunState.floorDepth` is a non-transient int and the tilemap is serialized inline under the run root (`SaveService` documents "the tilemap serializes once"); `restoreAfterLoad()` re-injects the serialized map into entities — it does not regenerate. There is **no `saveVersion` field anywhere**. AD-8 retires `floorDepth`/`descend()`/the floor-list `Route`, and AD-6's transient list (route, rng, noise queue, per-turn flags) is untouched. Neither AD says what happens to a save written before the migration. libGDX Json's default is to silently ignore JSON keys the target class no longer declares — it does not error.

**Pair constructed.**

- **Unit A (silent forward-load):** `SaveService` keeps libGDX defaults. A pre-migration save (contains `floorDepth: 2` and a 50×50 per-floor tilemap) loads "successfully": the `floorDepth` key is dropped, and the stale *floor-sized* tilemap is treated as the continuous Herois region. `restoreAfterLoad()` re-injects that wrong map. Player coordinates from the old floor frame now mean something else on a differently-shaped world. No error, no warning, no migration. This is a faithful reading of AD-8's "retired ... must not be preserved or adapted" (no legacy shims) + AD-6 (transient list unchanged).
- **Unit B (versioned migration):** `SaveService` writes/reads a `saveVersion`; on a pre-AD-8 save it either regenerates the continuous map from the stored seed and rebases positions, or rejects the save. Reads AD-8's retirement as "no *descent machinery*," which a save-migration path is not.

Both obey AD-6 (libGDX Json; same transient list) and AD-8 (no `floorDepth` field on the new RunState; descent triggers gone). **One silently corrupts or misplaces saves; the other migrates them.** There is no shared contract for save/load across the migration — a real hole for a permadeath game where a saved run's map is its world.

**Secondary wording defect in the same finding:** AD-8's sentence "AD-6's transient-field list re-supplies map data instead of a floor rebuild on load" is ambiguous — AD-6's transient list (route, rng, noise queue, per-turn flags) contains no map data, and the tilemap is serialized, not regenerated. A spine-only reader can conclude the map is regenerated on load, which would silently require world-gen determinism AD-8 never states.

**Fix (which AD to tighten):** AD-6/AD-8 must add a save-format rule: a `saveVersion` field on RunState; a defined migration policy for pre-AD-8 saves (regenerate map from seed + rebase, vs reject); and an explicit statement that the continuous tilemap serializes inline under the run root (or, if regenerated, that world-gen is deterministic from the run seed with a fixed RNG-consumption order).

---

## F4 — MEDIUM — AD-18 ↔ AD-9: light is both "visible" and "a noise source" — two detection channels, no choice made

**The gap.** AD-18: "a light source (campfire, torch) restores radius but is itself visible and a noise source to enemies." "Visible" points at FOV/LOS; "noise source" points at AD-9's NoiseEvent queue. These are two different detection mechanisms with different consequences.

**Pair constructed.**

- **Unit A (light as NoiseEvent):** torch/campfire emits a periodic NoiseEvent at its tile each player-acted turn; `NoiseSystem.resolve` (the AD-4 Noise step) lures any in-radius enemy to the source — **through walls**, because noise is positional, not LOS (that is exactly how `NoiseSystem` treats companion/attack noise today).
- **Unit B (light as FOV state):** a lit tile expands the enemy's visible set — an enemy is only alerted when its LOS actually intersects the lit radius; no NoiseEvent involved.

A player hiding behind a wall beside a lit campfire: Unit A can alert enemies behind the wall at the noise radius; Unit B alerts only enemies with LOS onto the lit tile. **Divergent stealth outcomes from the same spine text.** AD-10 already set the precedent of routing companion stealth-penalty through AD-9 NoiseEvents; AD-18 should say light uses that same channel or explicitly not.

**Fix (which AD to tighten):** AD-18 must route light to exactly one mechanism — recommend "a per-turn NoiseEvent at the source, consumed by the AD-9 Noise step, plus player-FOV radius restoration" — and state whether light-driven detection ignores LOS. Do not leave both "visible" and "noise source" as co-equal clauses.

---

## F5 — MEDIUM — AD-18 ↔ AD-16: "symmetric FOV for enemies and companions" scales turn cost past AD-16's test scope

**The gap.** AD-18: "the same FOV that governs the player governs companions and enemies symmetrically." AD-16's budget is "one turn renders and pathfinds without perceptible stutter," and its *required* performance test covers "the render/pathfind path" only. The current brownfield detection is cheap — `DetectionSystem` computes LOS-to-player per enemy (`VISION_RANGE=6`, `hasLineOfSight`), not a per-enemy FOV. "Symmetrically" does not say whether enemies/companions recompute a full FOV each turn.

**Pair constructed.**

- **Unit A (symmetric per-agent FOV recompute):** every agent (player + up-to-4 companions + N enemies) recomputes its own FOV every turn on the continuous persistent map (AD-8). Turn cost scales with agent count × map size.
- **Unit B (lazy/cached, brownfield-compatible):** FOV is computed only for the acting agent per pipeline step; detection keeps the cheap LOS-to-player check.

On a continuous Herois map with a full party and a dense eastern garrison, Unit A's turn cost can exceed AD-16's budget — and **AD-16's required test will not catch it**, because its scope is "render/pathfind," not FOV+AI for N agents. The whole-party shared turn (AD-5) and enemy AI (AD-4) multiply the same per-turn work.

**Fix (which AD to tighten):** AD-18 should state FOV recompute cadence (per acting agent, or cached across steps); AD-16's required performance test must include worst-case agent-count FOV + AI, not just render/pathfind.

---

## Secondary notes (low severity)

- **N1 — AD-10 ↔ AD-5 ↔ deferred party-size:** AD-10 says "each companion is a full tile-agent"; AD-5 binds "all agents (player + companions)" to the shared turn; the Deferred table punts the "holdout node." Are off-party roster members (three of the four) positioned tile-agents (frozen in place, still on the map, still emitting noise, survival ticking on player turns) or abstract FlagStore entries? Both ADs are silent. Recommend one sentence in AD-10.
- **N2 — AD-9 wording vs AD-4 pipeline:** AD-9 says "DetectionSystem reads the queue," but the code's queue reader is `NoiseSystem.resolve` (the "Noise resolve" step, *after* the Detection step 3). Pre-existing imprecision, now load-bearing because AD-10's companion-noise and AD-18's light-noise feed the same queue. Recommend AD-9 name the consumer step.
- **N3 — AD-8 phrasing:** the "AD-6's transient-field list re-supplies map data" sentence (see F3) should be tightened so a spine-only reader cannot conclude the map is regenerated on load.

---

## What the revisions got right (not re-opened)

- AD-8's retirement list is concrete and names real code (`RunState.descend/floorDepth`, `Route` floor-list, `RogueTile.STAIRS_*`, per-floor BSP, `TurnEngine` descent trigger). Good.
- AD-10's single-combat-authority principle is correct; the defects are scope (enemy-HP-only) and pipeline position, not the principle.
- AD-7 per-companion Bond (bindId, roster of four) is consistent with AD-10's keyed identity.
- AD-14's dialogue-safe-pause folds cleanly into AD-5's turn economy.
- AD-12's always-walkable border + defended-cordon win gate is the right honesty fix; only its interaction with AD-11's thickening clause is broken (F1).

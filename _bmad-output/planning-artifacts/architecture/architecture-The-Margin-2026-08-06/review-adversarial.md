---
review-of: architecture-The-Margin-2026-08-06/ARCHITECTURE-SPINE.md
type: review-adversarial
severity-scale: CRITICAL / HIGH / MEDIUM / LOW
reviewed: 2026-08-06
---

# Adversarial Architecture Review — The Margin Spine

## Method

I attacked the spine by constructing, for each invariant cluster, **two units one level down** that each read every AD literally and yet build incompatibly. Where two such units exist, the ADs under-determine the build and must be tightened or new ADs added. I read the spine, the PRD (`prd-The-Margin-2026-08-06/prd.md`), the memlog, the bible's `Companion System.md` and `Herois Region.md`, and the actual brownfield code the spine ratifies (`RunState`, `TurnEngine`, `Companion`, `CompanionSystem`, `CombatSystem`, `DetectionSystem`, `NoiseSystem`, `RogueEnemy`, `RoguePlayer`, `FloorGenerator`, `Route`, `SaveService`, `FlagStore`, `PlayerAction`, the `pom.xml` files, and `RunStatePersistenceTest`).

The single most important fact the spine does not confront: **the code it marks `[ADOPTED]` is a floor-descent engine, and the two new ADs it adds (AD-8 continuous map, AD-12 border win) are floor-descent-incompatible.** Every other finding follows from that contradiction and from ADs that name a system without pinning its data shape or its mutation path.

---

## F1 — CRITICAL — The world model contradicts the ratified code: AD-8 (continuous region) vs the `[ADOPTED]` descent pipeline (AD-3/AD-4/AD-6)

**Evidence the spine is two incompatible documents:**

- AD-8 (line 101–105): Herois is "**one continuous tiled region**… Connected sub-areas are rejected."
- AD-4 (line 81, `[ADOPTED]`): the pipeline includes the **descent seam**. `TurnEngine.advance` (TurnEngine.java:119–124) treats STAIRS_DOWN as a turn: `state.descend()` rebuilds the whole map.
- AD-3 (`[ADOPTED]`): `RunState` owns `floorDepth`, `Route`, a `RogueTileMap` sized `MAP_W×MAP_H = 50×50` (RunState.java:27–28, 46).
- `RunState.descend()` (RunState.java:142–165), `Route` ("The Caravan Road", **3 floors**, Route.java:14–15), and `FloorGenerator` (BSP rooms + corridors, per-floor, FloorGenerator.java:29) are the entire world model the ADOPTED ADs ratify. `restoreAfterLoad()` and `SaveService` serialize `floorDepth` and a single 50×50 map.
- AD-12's win is "reaching the NW border tile" — a tile that a 3-floor 50×50 descent cannot express.

**Two units that each "obey every AD":**

- **Unit W1 (continuous world):** one large persistent `RogueTileMap` (hundreds of tiles), fixed canon landmarks placed, procedural wilderness stitched between them (AD-8 literal). Deletes `descend()`/`Route`/`floorDepth`/STAIRS_DOWN. Danger = spatial east/west gradient. Enemy spawns modulated by region.
- **Unit W2 (brownfield descent):** keeps `descend()`/`Route`/`floorDepth`/STAIRS_DOWN; reads AD-8 as "one continuous region *per floor*" and treats each descent as an eastward step (a "zone" = a floor).

Both claim AD-3 (single owner), AD-4 (fixed pipeline — W2 keeps the exact ratified steps; W1 must rewrite the pipeline's descend branch), AD-6 (serialize `RunState` — but the save schema differs: `floorDepth`/`Route`/50×50 map vs a single large map), AD-8 (each has a defensible reading), and AD-12 (each can place a "border" tile). They build incompatible maps, incompatible save files, incompatible spawn logic, and incompatible win detection. The spine has not chosen, and worse, its Deferred section (line 188) punts only the *stitching detail*, never the **retirement of the descent machinery that the ADOPTED ADs ratify**. Downstream, one story builds world-gen continuous and another builds the rescue-Aldric quest against `descend()`.

**Close with:** a new AD that (a) retires `descend()`/`Route`/`floorDepth`/STAIRS_DOWN or explicitly redefines them, (b) pins the physical realization of the continuous region (single monolithic map vs tiled/chunked streaming) and the save-schema consequence (AD-6), and (c) states how the east/west gradient is encoded in the tilemap so AD-12's "border tile" is a first-class, findable thing.

---

## F2 — HIGH — The win gate is under-determined: AD-12 (border win) × AD-8 (continuous map) × AD-11 (act gating)

**Two units that each obey every AD:**

- **Unit G1 (flag-gated crossing):** the border crossing tiles are impassable until the Act-3 flag flips in `FlagStore` (AD-7/AD-11). The "final tense run" is the **scripted** sequence AD-12 names, triggered by that flag.
- **Unit G2 (always-walkable crossing):** the border tile is walkable from turn 1 ("home is a few miles away" — PRD §3 Herois/`Herois Region.md`). Reaching it any time starts the final run; acts only escalate danger (AD-11), so an early crossing is legal but suicidal. The final run is **emergent** from the escalated world.

Both read AD-8 ("one continuous traversable region" — G1 must seal terrain, arguably violating it), AD-11 (acts gate — G2 makes gates difficulty-only), and AD-12 (both "reach the NW border tile and survive a final tense run") on reasonable readings. They are incompatible on map walkability, win detection, and scripted-vs-emergent climax. **What the spine never says: what physically stops the player from walking west on day 1.** That is the load-bearing question of a "home is close but lethal to reach" design, and no AD answers it.

Sub-hole: AD-8 says "safety lies west toward the border" while AD-12's win is *in* the west. If occupation escalation (AD-11) is east-weighted, the border belt is low-danger and the final run must manufacture its own tension (contradicting "the west is safe"). If escalation is global per-act, the west gets *more* dangerous as the player approaches the win (contradicting AD-8's gradient). No AD reconciles the **spatial model of escalation** with the **win's location** — yet FR-12 (occupation escalation) is a core combat-cost lever and the win is the climax.

**Close with:** an AD stating (a) what gates the crossing (flag vs walkability) and where that gate lives, (b) whether the final run is scripted or emergent and which system owns it, (c) whether escalation is east-weighted, global, or both, and how it interacts with AD-8's gradient.

---

## F3 — HIGH — The companion is two different things: AD-10's 7-state machine vs AD-4's "Companion follow" step, and its persistence (AD-6) is unsolved

**State-machine vs follow (AD-10 × AD-4):**

- AD-4 (line 81) names the step **"Companion follow"**. The ratified code is exactly that: `CompanionSystem.follow` → `Companion.followStep`, a greedy one-tile chase (CompanionSystem.java:20–24, Companion.java:58–80). The DISTRACT action is a per-floor counter + `emitNoise` (CompanionSystem.java:32–46).
- AD-10 (line 113–117) mandates a full tile-agent with a behavior state machine (**FOLLOW/HOLD/HIDE/DISTRACT/FIGHT/RETREAT/FLEE**).

**Two units, each citing AD-4 and AD-10:**

- **Unit C-AI:** the Companion step runs a perception→state→action loop. FIGHT attacks adjacent enemies (mutates enemy HP, emits noise); HIDE holds position; RETREAT flees.
- **Unit C-follow:** the Companion step is greedy follow, plus the existing DISTRACT (literal AD-4 reading; the current code).

They are incompatible on mutation ownership: **FIGHT gives the companion AI a second owner of enemy HP** alongside `CombatSystem.playerAttack`/`enemyPhase` (CombatSystem.java:19–31, 40–74). Nothing in the spine says whether the companion AI may damage/kill enemies, whether enemies may target and damage the companion (FR-15's "a body Klein must defend"), or what happens at companion 0 HP (FR-15 open item — death/capture/leave is literally unresolved). The companion's *perception* is also ungoverned: `DetectionSystem.update` (DetectionSystem.java:22–52) only tests enemies against the **player** — a companion that must "obey the same detection/noise rules as enemies" (AD-10) has no detection input defined. Two units build DetectionSystem and CombatSystem that cannot share a save or a turn.

**Persistence (AD-6 × AD-10):**

- **Unit P1:** the state machine's current state + transition timers are **persisted fields** on `Companion` (serialized under the `RunState` root, per SaveService.json() setElementType, SaveService.java:29).
- **Unit P2:** the state machine is **derived each turn from perception**; current state is `transient`, reset (e.g., to FOLLOW) in `restoreAfterLoad()` (RunState.java:174–185).

Both obey AD-6 (transients re-supplied on load; persisted fields saved). They produce **incompatible save schemas and reload behavior**: under P2 a companion mid-RETREAT snaps to FOLLOW after a load, potentially blowing stealth or abandoning a held HIDE position — a determinism break the spine never acknowledges (the code's "future draws restart from the seed" note, RunState.java:170–173, is about the RNG, not about behavior-state continuity).

**Party identity (AD-3 × AD-10):** the memlog's "single party slot" is **not in the spine**. The code models the party as `List<Companion>` with `getActiveCompanion() = companions.get(0)` (RunState.java:231–233). With a 4-companion roster, capture (Aldric, FR-3) and re-rescue (FR-18) mutate that list; a 2-slot alternative (FR-17 open) breaks index-0 semantics. **Unit I1** keeps the list-index party; **Unit I2** moves to a `Map<bindId, Companion>` with an `active` flag. Both obey AD-3 (RunState owns companions) and AD-10; they cannot share a save file.

**Close with:** ADs that (a) rename AD-4's step to "Companion AI" and pin what it may mutate (can it damage enemies? can enemies damage it? what happens at 0 HP — death/capture/leave), (b) pin the companion's perception inputs (does it read DetectionSystem? has its own FOV? who computes it), (c) decide persisted-vs-derived state machine and state it in AD-6, and (d) pin the party's identity model (index vs keyed) and whether multiple companions are ever on-map.

---

## F4 — HIGH — Companion needs and the party-stealth penalty are ungoverned (AD-5 × AD-10 × FR-15/17)

**Two units on companion survival pressure, both obeying AD-5 (tick only on acted turns) and FR-15 ("own Status block, own condition state"):**

- **Unit N1 (derived needs):** the companion has no independent Hunger/Thirst/Temperature fields; the party shares one set of tracks; food is consumed as a multiplier when the player eats (FR-17 "costs extra food").
- **Unit N2 (independent tracks):** the companion carries its own `HungerStatus`-style enum, thirst timer, temperature value, and debuff stack, ticked each acted turn, consuming from its own food share.

The memlog defers "companion stat granularity" (spine line 189: full six-stat block vs role-relevant subset), which is exactly the door N1 walks through — so both units are defensible under the spine as written. They are incompatible in `RunState` shape, in serialization (AD-6), and in the screen readout (AD-1). **The spine has no AD for what "shares the survival pressure" means structurally.**

Second sub-hole: FR-17's "adds a noise penalty to stealth" (Companion System bible: "two people are louder") is **not a `NoiseEvent`** — AD-9's noise is an event on a per-turn queue (NoiseEvent.java, RunState.emitNoise). A passive party-stealth penalty is a modifier on `DetectionSystem` thresholds (DetectionSystem.java:16–18). Two units implement it as (a) a global detection modifier vs (b) conditional/periodic NoiseEvents. AD-9 neither permits nor forbids (a); the spine doesn't choose.

Third: AD-5 × FR-16 ("acts independently each turn") — the spine never states that the companion only acts on **player-acted** turns. The brownfield convention puts the companion step inside `if (acted)` (TurnEngine.java:114, 133), but AD-10's "autonomous" language would support per-turn companion action that commits no player turn. That ambiguity changes noise/detection cadence and the survival-clock meaning.

**Close with:** an AD defining the companion needs model (derived vs independent tracks, which of the four tracks companions carry), an AD for the party-stealth penalty mechanism (modifier vs noise events), and a sentence in AD-5 pinning that companion actions occur only on acted turns.

---

## F5 — MEDIUM — The survival-track expansion is ungoverned: AD-4's step is literally "Hunger"

FR-4 mandates **four** tracks (Hunger, Thirst, Temperature, Day/Night) + Weather (FR-5) + Debuffs (FR-8). AD-4's pipeline step is named **"Hunger"**, and the ratified code implements only hunger (`RoguePlayer.HungerStatus`, `HungerSystem`). The current `RoguePlayer` has `str/instinct/grit/voice` — no AG, no SKILL, no VOICE-gated dialogue (FR-19).

- **Unit S1:** new per-track pipeline steps inserted in fixed order (`ThirstSystem`, `TemperatureSystem`, `WeatherSystem`, `DebuffSystem`).
- **Unit S2:** one `SurvivalSystem` aggregate that ticks all four tracks + weather in a single step.

Both obey AD-4 (fixed order; "no turn rule lives in the screen") and AD-3/AD-5. They differ in mutation cadence, in serialized shape (per-track fields vs aggregate), and in where the 170-turn Day/Night clock and the per-cycle weather roll live. **Nothing says how a new survival track enters the pipeline** — that is the exact "new mechanic inserts as an ordered system" case AD-4 exists for, yet the AD names only the existing one. Also unresolved: does the Day/Night clock tick on acted turns (FR-4) while Weather is a per-cycle roll (FR-5) — and who applies weather to temperature? All ungoverned.

**Close with:** an AD on survival-system granularity (one aggregate vs per-track steps), the shared tick contract, and the serialized shape of all four tracks + debuff stacks.

---

## F6 — MEDIUM — Capability → Architecture Map fall-throughs

| Fall-through | Where it lives / binds | What's missing |
| --- | --- | --- |
| **Currency/trade (FR-20/21)** | Map row says "Inventory/currency (FR-20..21) — **AD-13**" | AD-13 is gear-with-memory (repair decay). Nothing governs the coin economy, the two traders, weight, or barter. Wrong governing AD. |
| **FOV / light (FR-4/5, Night/Fog)** | `system/FovSystem` exists in code | The new spine has **no FOV AD**. Old AD-9 bundled radius+LOS; new AD-9 is noise-only. Night/Fog shrinking FOV and torch restoring it are ungoverned. |
| **Debuffs (FR-8)** | — | Data shape (tiered stacks), tick cadence, and cure paths ungoverned. |
| **Dialogue suspends the turn loop (FR-19)** | `narrative/DialogController` | The "safe pause" interacts with AD-5 (does a suspended loop tick survival?). No AD. |
| **Six-stat model + SKILL growth (FR-11, FR-19 VOICE gates)** | `RoguePlayer` | 4 of 6 stats present; AG/SKILL absent; no AD governs the stat block, SKILL-by-doing, or VOICE-as-gate. |
| **Occupation escalation output (FR-12)** | AD-11 covers flags/ramp-trigger | The ramp's *output* (patrol density, curfew, bounties, spawns) is ungoverned and unwired to AD-8's map. |
| **Permadeath save-scum (FR-14)** | `SaveService.deleteSave` | "True death deletes the slot" is code behavior with no AD in the new spine. |
| **Companion Bond with a 4-roster** | `FlagStore.KEY_BOND` | AD-7 says "a companion Bond value" (singular); code stores one Galleon key. With 4 companions Bond must be per-companion (FR-15). Two units: single key vs per-`bindId` keys — incompatible save shapes. |

---

## F7 — MEDIUM — Two owners of one entity: enemy HP (Companion FIGHT vs CombatSystem) and "active companion" (list index vs key)

Partially covered under F3; stated sharply here because the task names it: **two owners of one entity.** AD-3 says `RunState` owns the data, but AD-4/AD-10 let *systems* mutate it, and nothing says **which system may mutate which field**. The companion's FIGHT (Companion step, step 4) and `CombatSystem.enemyPhase` (step 5) can both mutate enemy HP in the same turn; `CombatSystem.checkLastStand` (step 7, CombatSystem.java:82–90) revives only the player — companion 0 HP has no owner. Two units build `CombatSystem` where (a) enemies never target companions (current) vs (b) enemies pick nearest hostile and damage the companion (FR-15 literal). Both obey AD-3/4/9/10; they cannot share a turn budget or a combat log.

**Close with:** an AD on mutation ownership per field (who may write enemy HP, companion HP, the party list, the noise queue) and a companion-injury/death rule.

---

## F8 — LOW — Web/reality-check mandate not uniformly applied

**Verified correct:** the spine's libGDX claims. Maven Central + GitHub releases confirm **1.14.2 is the latest (June 2026)** and **1.12.1 exists (Nov 2023)** — the pin and the "bump deferred" note are accurate.

**Not verified / wrong:**

- **The Stack table (lines 154–156) lists Mockito 3.11.0 and AssertJ 3.2.5 as project stack, but neither is in the build.** `pom.xml` declares only `gdx` + `gdx-backend-lwjgl3` + `gdx-platform` (parent, `dependencyManagement`) and `gdx` + `junit-jupiter:5.10.2` (core). No source imports Mockito or AssertJ. `AssertJ 3.2.5` is almost certainly a phantom version — `org.assertj:assertj-core` has no 3.2.5 (that version string belongs to a different artifact, `json-unit-assertj`). The "Seed — verified current at authoring (2026-08-06, Maven Central)" claim is overstated for these two rows.
- **Java 17** (line 150): the number is right and the pin is ratifiable, but by 2026 Java 17 is past Oracle's free public-update window (current LTS: 21, 25). The spine "verified" the version number, not the support status or the actually-installed JDK. Flag for a one-line confirmation rather than an assumption.
- **The mandate was applied to versions but not to architecture-vs-code claims.** The two facts the spine web-checked are the libGDX version lines; the design commitments that most need a reality check against the code — AD-8 continuous map vs the ratified descent engine (F1), AD-10 companion-as-state-machine vs the ratified follow-only follower (F3), AD-12 border win vs the 3-floor `Route` — are asserted from the PRD/bible without confronting the contradicting brownfield code. That is where the mandate would have caught F1–F3.

---

## Constructed incompatible pairs (summary)

| # | Cluster | Unit A | Unit B | Conflict | Hole to close |
| --- | --- | --- | --- | --- | --- |
| 1 | World model | Continuous map, no descent (AD-8 literal) | Brownfield descent, "region per floor" (AD-4 ratified) | Save schema, world-gen, spawns, win detection | New AD retires/redefines `descend`/`Route`/`floorDepth`; pins map realization + save consequence |
| 2 | Win gate | Flag-sealed border, scripted run (AD-11/12) | Always-walkable border, emergent run (AD-8) | Map walkability, win trigger, climax type | AD: what gates the crossing; scripted vs emergent; escalation spatial model |
| 3 | Companion | 7-state machine, persisted state (AD-10/6) | Greedy follow + DISTRACT, derived state (AD-4/6) | Enemy-HP ownership, perception inputs, save schema, reload determinism | AD: step renamed "Companion AI"; mutation + perception + persistence + party identity pinned |
| 4 | Companion needs | Derived shared tracks (FR-17) | Independent tracks + own status (FR-15) | `RunState` shape, serialization, UI | AD: companion needs model; party-stealth penalty mechanism; AD-5 acted-turn-only sentence |
| 5 | Survival tracks | Per-track pipeline steps (FR-4) | Single aggregate system | Tick cadence, serialized shape | AD: survival-system granularity + tick contract |
| 6 | Party identity | `List`, index-0 active (current) | `Map<bindId>` + active flag | Save shape; multi-companion; capture/rescue | AD: party identity model |

---

## Verdict

**REVISE before downstream epics.** The layered headless-core/render paradigm and the ADOPTED invariants (AD-1/2/3/6/7/9) are sound and match the code. But the spine is not build-safe as written:

- **One CRITICAL internal contradiction** (F1): AD-8/AD-12 are bolted onto an `[ADOPTED]` floor-descent engine (AD-3/4/6) that cannot express them. A downstream story will build one or the other, and the save format, world-gen, and win detection are committed by whichever it picks.
- **Four HIGH under-determinations** (F2–F4 + companion-injury under F3): the win gate, the companion AI's mutation/perception/persistence, the companion needs model, and the survival-track expansion each admit two incompatible faithful implementations.
- **Capability-map fall-throughs** (F6–F7): currency has no governing AD, FOV is ungoverned, companion Bond is single-key against a 4-roster, enemy-HP/companion-HP mutation has two plausible owners.
- **Reality-check gaps** (F8): the libGDX claim is verified; the Mockito/AssertJ stack rows are phantom (not in the build, AssertJ 3.2.5 likely nonexistent), and the version-checking mandate was not applied to the architecture-vs-code contradictions that F1–F3 expose.

Suggested AD actions, in priority order: (1) new world-model AD retiring the descent machinery and pinning the continuous map's realization + save consequence; (2) tighten AD-12 with the crossing gate and the escalation spatial model; (3) rename AD-4's companion step to "Companion AI" and add a companion AD covering mutation ownership, perception, 0-HP outcome, and state persistence (AD-6); (4) add a companion-needs AD; (5) add a survival-track-expansion AD; (6) fix the Capability Map rows for currency and FOV; (7) correct the Stack table against the actual `pom.xml` and confirm Java 17's support status.

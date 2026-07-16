---
title: The Margins — MVP (Route 1 Vertical Slice)
status: final
created: 2026-07-17
updated: 2026-07-17
---

# PRD: The Margins — MVP (Route 1 Vertical Slice)
*Working title — confirm.*

## 0. Document Purpose

This PRD is for Justine (solo developer, and every downstream BMAD workflow — architecture, epics, stories). It builds on the finalized Product Brief (`_bmad-output/planning-artifacts/briefs/brief-The-Margins-2026-07-17/brief.md`) and the game's `DESIGN.md`; it does not duplicate the narrative vision, it turns the MVP slice of it into testable requirements. Vocabulary is Glossary-anchored (§3); features are grouped with globally-numbered FRs nested; inferred decisions are tagged `[ASSUMPTION]` inline and indexed in §9. Technical *how* (engine classes, algorithms, serialization) lives in `addendum.md` and is the architect's to resolve — this document stays at the capability level. This is a **brownfield** PRD: an existing, working SPD-style Phase-0 core (turn engine, BSP floor generation, melee combat + defense, hunger, enemy chase AI, stairs, permadeath) is the foundation; FRs below extend it.

## 1. Vision

The Margins is a turn-based roguelike where you descend not for treasure but for people. This MVP proves the entire loop through one Route — **The Caravan Road** — playable end to end: Milek picks his way through the picked-over remnant of a convoy camp, rationing food he can't trust, avoiding mercenaries he can't beat in a straight fight, until the search pays off in a reunion with Galleon. It is a **you-first passion project** in the creator's own novel world; success is a finished, playable artifact she is proud of, not a market.

The slice exists to answer one question honestly: *is this loop — SPD scarcity fused with a search-for-people story — worth building all five routes on top of?* Everything in scope serves demonstrating that loop once, at quality; everything that doesn't is deferred to the north-star Vision.

## 2. Target User

### 2.1 Jobs To Be Done
- **As the builder (primary):** I want to make a finished, playable thing set in my novel's world that I'm proud to show a friend — proving to myself the loop works before I commit years to the full campaign.
- **As a reader of the novel (secondary):** I want to *play through* the emotional beats I read — testing an untrusted waterskin the way Milek tested barrels, feeling the weight of the Galleon reunion because I already carry these characters.

### 2.2 Non-Users (v1)
Roguelike players with no knowledge of the novel. The MVP does **not** owe them a cold-open onboarding that makes the story earn them through play; narrative legibility to strangers is an explicit non-goal (see §5).

### 2.3 Key User Journeys
*Light scope — solo/hobby, single protagonist. Journeys sketch the felt experience the FRs must deliver.*

- **UJ-1. Justine plays the opening and the hook lands.** New run. The "Five Nights, Again" opening plays — cold ash, four tents, Milek counting. She meets the scavenger, picks the INSTINCT option, and (on a success) uncovers a hidden cache with no fight. She feels the tone the novel has: observational, careful, not heroic. Realizes FR-20, FR-21, FR-6.
- **UJ-2. The untrusted-supply gamble bites.** Mid-floor, low on food, Milek finds a Sealed Waterskin. She uses it hoping for clean water; it's tainted and applies Weaken. She learns the identify-by-use tension diegetically and rations more carefully after. Realizes FR-7, FR-8.
- **UJ-3. Galleon changes how a floor is solved.** A locked supply crate sits past two patrolling mercenaries. With Galleon in the party she triggers his Distraction — the patrol peels toward the noise, opening a path she could not have forced alone. Realizes FR-16, FR-17.
- **UJ-4. The reunion pays off, then Last Stand keeps a bad run honest.** She reaches the story floor; the Galleon reunion scene plays with a tone-setting choice. Earlier, a mercenary nearly killed Milek — instead of a cheap game-over, Last Stand gave her one desperate low-HP turn to survive. Realizes FR-22, FR-11, FR-12.

## 3. Glossary

- **Route** — A self-contained location made of a sequence of Floors ending in one Story Floor. The MVP contains exactly one Route: *The Caravan Road*.
- **Floor** — One procedurally generated level (existing BSP generation). The Route has 3 procedural Floors.
- **Story Floor** — A hand-authored, non-procedural Floor delivering a fixed narrative beat (here: the Galleon reunion). The Route's 4th and final Floor.
- **Supply** — An unidentified consumable item whose true identity is hidden until used (the diegetic reframing of SPD identify-by-use). Milek cannot read labels.
- **Identify-by-Use** — The mechanic by which a Supply's true identity is randomized per seed and revealed only on first use, then known for the rest of the run.
- **Detection** — An enemy's awareness state of Milek: Unaware, Suspicious, or Alerted.
- **Noise** — A transient, location-based stimulus (from Galleon's Distraction, forcing a crate, or combat) that can raise nearby enemies' Detection or draw them toward a tile.
- **INSTINCT Check** — A dialogue or perception test resolved against Milek's INSTINCT stat; success unlocks otherwise-hidden options/information.
- **Companion** — An allied entity that accompanies Milek and changes how a Floor can be solved. The MVP has exactly one: Galleon.
- **Leverage** — A Companion's signature non-damage ability that alters Floor solutions (Galleon's = Distraction). Distinct from raw combat contribution.
- **Bond** — A per-Companion relationship value raised by dialogue choices. In MVP it is tracked and gates dialogue tone only; it unlocks no mechanics.
- **Last Stand** — A one-time-per-run reprieve: the turn Milek would take lethal damage, he instead survives at 1 HP in a flagged desperate state for one turn before true permadeath applies.
- **Hunger** — Existing depletion resource; reaching starvation causes HP loss.
- **Run** — One playthrough from Route start to death or Route completion. Permadeath ends a Run.

## 4. Features

### 4.1 Field of View & Fog of War
**Description:** Milek sees only what line-of-sight reveals; explored-but-unseen tiles are remembered dim, unexplored tiles hidden. This is the precondition for stealth reading as stealth. Realizes UJ-3. [ASSUMPTION: symmetric shadowcasting FOV; the architect confirms algorithm.]

#### FR-1: Line-of-sight visibility
Milek can see tiles within line of sight bounded by walls; sight radius is a tunable constant.
**Consequences (testable):**
- Tiles blocked by a wall are not visible; opening a door reveals the room beyond on the next turn.
- Enemies and Supplies render only while on a currently-visible tile.

#### FR-2: Explored memory (fog)
Previously-seen static terrain renders dimmed when out of sight; dynamic entities (enemies, Companion) do not persist in fog.
**Consequences (testable):**
- A room visited then left still shows its walls/floor dimmed; an enemy that walked out of sight is no longer drawn.

### 4.2 Stealth & Enemy Awareness
**Description:** Route 1 enemies (mercenary stragglers) patrol and can be avoided rather than fought. Each enemy holds a Detection state driven by sight and Noise. Combat is a failure-adjacent fallback, not the default. Realizes UJ-3. [ASSUMPTION: 3-state Detection model below is the MVP target; no full stealth-meter UI beyond an above-enemy indicator.]

#### FR-3: Enemy patrol behavior
An unaware enemy follows a patrol (fixed waypoint loop or idle-wander) until it detects Milek.
**Consequences (testable):**
- With Milek never entering sight/Noise range, an enemy never leaves Unaware and never initiates combat.

#### FR-4: Detection state machine
An enemy transitions Unaware → Suspicious → Alerted based on Milek entering its vision or generating Noise; it de-escalates over turns without stimulus.
**Consequences (testable):**
- Entering an enemy's vision cone raises it to Suspicious within a defined turn count; sustained line-of-sight escalates to Alerted (begins pursuit); breaking sight for N turns de-escalates one step.

#### FR-5: Noise propagation
Noise events raise Detection of enemies within a radius and/or draw Alerted enemies toward the Noise origin.
**Consequences (testable):**
- Forcing a locked crate generates Noise that moves nearby enemies toward the crate tile; a silent move does not.

### 4.3 Dialogue, Authored Scenes & INSTINCT Checks
**Description:** Wires the existing (currently-unwired) `DialogNode`/`QuestManager` scaffolding into the turn loop so authored scenes pause play, present branching choices, resolve INSTINCT checks, and set flags. Carries the "Five Nights, Again" opening and the reunion. Realizes UJ-1, UJ-4.

#### FR-6: Branching dialogue presentation
Milek can be presented an authored dialogue node with 1–4 choices that pauses turn processing until resolved.
**Consequences (testable):**
- While a dialogue node is open, enemy turns do not advance; selecting a choice advances to the linked node or closes the scene.

#### FR-7: INSTINCT-gated choices
A dialogue choice may be gated by an INSTINCT Check; the outcome (success/failure) routes to different nodes and may set a flag or grant an item.
**Consequences (testable):**
- With INSTINCT above the check threshold, the gated branch resolves success (e.g., reveals the hidden cache, FR-8); below it, the failure branch resolves and the cache is not revealed by dialogue.

#### FR-8: Scene/quest flags
Authored scenes set and read run-scoped flags that gate later content (cache revealed, reunion seen, Bond tier).
**Consequences (testable):**
- Choosing the honest reunion option sets a Bond flag readable by later nodes; a set "cache revealed" flag spawns the cache contents.

### 4.4 Inventory & Supplies
**Description:** A finite backpack plus equipped slots; Milek picks up, carries, uses, and drops items. Supplies are the identify-by-use item class. Realizes UJ-2.

#### FR-9: Backpack & equipped slots
Milek can carry up to a fixed number of backpack items and use/equip them; exceeding capacity forces a drop/skip choice.
**Consequences (testable):**
- Picking up an item when the backpack is full prompts drop-or-leave; equipping moves an item from backpack to an equipped slot.

#### FR-10: Use / drop actions
Milek can use a consumable (applying its effect and removing it) or drop any carried item onto his tile.
**Consequences (testable):**
- Using a food Supply reduces Hunger and removes the item; dropped items reappear on the floor tile and can be re-picked.

### 4.5 Identify-by-Use
**Description:** Each Supply type displays an unidentified name until first use; its true identity is randomized per seed and, once used, is known for the rest of the Run. The MVP item set is the five Route-1 Supplies. Realizes UJ-2.

#### FR-11: Per-seed identity randomization
At Run start, each Supply type is bound to one of its possible true identities for that seed.
**Consequences (testable):**
- Two Runs on different seeds can map "Sealed Waterskin" to different outcomes; within one Run the mapping is stable.

#### FR-12: Identify-on-use & persistence
Using an unidentified Supply reveals and applies its true identity and marks that Supply type identified for the rest of the Run.
**Consequences (testable):**
- After using one Sealed Waterskin, all remaining Sealed Waterskins in that Run display their true identity without being used.

**Notes:** MVP Supply set (true identities per §Balance): Wrapped Bundle, Sealed Waterskin, Small Tin, Folded Cloth, Sealed Letter.

### 4.6 Companion: Galleon
**Description:** Galleon accompanies Milek from the point of recruitment, occupies a party slot, and provides one Leverage ability — Distraction — that generates Noise to pull patrols. He is tanky and can absorb/return hits but is designed as leverage, not a DPS turret. Bond is tracked for dialogue tone only. Realizes UJ-3, UJ-4. [ASSUMPTION: Galleon is available for the stealth Floors preceding the reunion via an early narrative recruitment, OR joins only at the Story Floor; §Open Questions Q1.]

#### FR-13: Companion follow & party slot
When active, Galleon follows Milek across tiles and floor transitions, occupying the single MVP party slot.
**Consequences (testable):**
- Galleon pathfinds to stay adjacent/near Milek each turn and transitions with him to the next Floor.

#### FR-14: Leverage — Distraction
Milek can command Galleon to create Noise at/near his position, drawing Alerted-able enemies toward it.
**Consequences (testable):**
- Triggering Distraction raises nearby enemies' Detection toward the Noise origin, opening a path away from it; the ability has a cooldown or per-Floor use limit.

#### FR-15: Bond tracking
Galleon's Bond value changes from tagged dialogue choices and is readable by dialogue nodes.
**Consequences (testable):**
- The honest reunion choice raises Bond; a later node reads the Bond tier to select warmer dialogue. [NON-GOAL for MVP: Bond unlocking Alpha transformation or combat bonuses.]

### 4.7 Last Stand & Permadeath
**Description:** Preserves permadeath stakes without cheap deaths. The first lethal blow in a Run triggers Last Stand; a second lethal event is true death. Realizes UJ-4.

#### FR-16: Last Stand trigger
The first time Milek would drop to ≤0 HP in a Run, he instead survives at 1 HP in a flagged Last Stand state for that turn.
**Consequences (testable):**
- The first otherwise-fatal hit leaves Milek at 1 HP and sets the Last-Stand-used flag; a message communicates the reprieve.

#### FR-17: True permadeath
With Last Stand already used, the next lethal event ends the Run (existing permadeath/game-over path).
**Consequences (testable):**
- A lethal hit after Last Stand is spent triggers game-over; starting a new Run resets the Last-Stand-used flag.

### 4.8 Route Structure & Story Floor
**Description:** Sequences the Route: 3 procedural Floors then the authored Story Floor, with completion ending the slice. Realizes UJ-4.

#### FR-18: Route progression
Descending stairs advances Floor 1→2→3→Story Floor; the Story Floor is authored, not procedurally generated.
**Consequences (testable):**
- Taking stairs on Floor 3 loads the authored Story Floor layout rather than a BSP-generated one.

#### FR-19: Route completion
Completing the Story Floor's reunion scene reaches a defined MVP end state (completion screen / "to be continued").
**Consequences (testable):**
- Finishing the reunion scene transitions to an end-of-slice screen and marks the Route complete.

### 4.9 Save & Continue
**Description:** A single active Run can be saved and resumed so a session can span sittings. One save slot; permadeath deletes it. Realizes the "save/resume works" success criterion.

#### FR-20: Single-run persistence
Milek's Run state (seed, current Floor, positions, inventory, identified Supplies, flags, Bond, Hunger, HP, Last-Stand-used) can be saved and reloaded to resume mid-Run.
**Consequences (testable):**
- Quitting and relaunching restores the exact Run state; the game continues from the saved Floor and turn.

#### FR-21: Permadeath clears save
True death (FR-17) deletes the active save so a dead Run cannot be reloaded.
**Consequences (testable):**
- After game-over, no continue option is offered; only a new Run can be started. [ASSUMPTION: single slot, save-scumming is not defended against beyond delete-on-death.]

## 5. Non-Goals (Explicit)

- **Not building Routes 2–5**, the Trust Meter, the full companion roster, Alpha transformation / Life-Thread economy, multiple endings, literacy skill tree, alchemy/crafting, shops, or faction systems. (Deferred to Vision.)
- **Not becoming a stranger-legible roguelike** in v1 — no narrative onboarding for players who haven't read the novel.
- **No sound/music, no title-screen polish, no mobile/HTML5 build.**
- **Not a balancing showcase** — combat/economy numbers are first-pass and tunable (§Balance), not the point of the MVP.

## 6. MVP Scope

### 6.1 In Scope
- One Route (The Caravan Road): 3 procedural Floors + 1 authored Story Floor.
- FOV/fog of war; stealth & 3-state enemy Detection; Noise.
- Dialogue system wired to turn loop; INSTINCT checks; scene/quest flags; the "Five Nights, Again" opening and the Galleon reunion scene.
- Inventory (backpack + equipped slots); use/drop.
- Identify-by-Use with the 5-item Route-1 Supply set.
- Galleon: follow, one Leverage (Distraction), Bond tracking (tone only).
- Last Stand + existing permadeath.
- Single-run save/continue.
- Art: reuse existing CraftPix packs + minimum unique sprites (Milek, Galleon, one scavenger NPC).

### 6.2 Out of Scope for MVP
- Everything in §5. `[NOTE FOR PM]` The **Galleon reunion tone-setting choice + Bond** is emotionally load-bearing; it stays in even though the broader Bond system is deferred — the scene is the payoff the whole slice builds toward.
- Second companion, bench passives, multiple party slots — one slot only.
- Ranged/multiple enemy archetypes beyond the mercenary straggler needed for stealth.

## 7. Success Metrics
*Completion-first, per the brief. Hobby calibration — these are pass/fail gates, not analytics.*

**Primary**
- **SM-1**: Route 1 is playable start→finish with no progression-blocking bug. Validates FR-18, FR-19.
- **SM-2**: Each core loop is demonstrated at least once in a single playthrough — stealth avoidance, Hunger pressure, an Identify-by-Use gamble, Galleon Leverage changing a solution. Validates FR-3/4, FR-11/12, FR-14.
- **SM-3**: The three authored beats fire in-game — opening, ≥1 INSTINCT-gated choice, the reunion. Validates FR-6, FR-7, FR-19.
- **SM-4**: Last Stand triggers correctly on otherwise-lethal damage; save/resume restores a Run. Validates FR-16, FR-20.
- **SM-5**: Ships as a runnable desktop build (double-clickable jar/executable).

**Counter-metrics (do not optimize)**
- **SM-C1**: Do **not** measure success by downloads, sales, or store ratings — optimizing these would pull scope toward stranger-onboarding and polish that the you-first framing explicitly defers. Counterbalances the temptation to expand SM-1..5 into a commercial bar.
- **SM-C2**: Do **not** chase combat/economy balance perfection — first-pass numbers that don't block SM-1 are sufficient. Counterbalances SM-2.

## 8. Open Questions
1. **Galleon availability window** — is Galleon recruited early (available across the stealth Floors, so UJ-3 works before the reunion) or only at the Story Floor? Affects FR-13/14 and the pacing of the Distraction teach. *(Recommend: a lightweight early recruitment so Leverage is taught before the reunion; confirm.)*
2. **Vision cone vs. radius** for Detection (FR-4) — directional facing or omnidirectional radius? Directional is more "stealth," more work. Architect + tuning call.
3. **Art pipeline** — how the minimum unique sprites (Milek, Galleon, scavenger) get made (recolor / commission / creator-drawn). A real solo-dev bottleneck; not a code blocker but a schedule risk.
4. **Story Floor authoring format** — hand-placed tile layout vs. constrained-procedural with fixed rooms. Architecture decision.

## 9. Assumptions Index
- §4.1 — Symmetric shadowcasting FOV (algorithm confirmed in architecture).
- §4.2 — 3-state Detection model with above-enemy indicator, no full stealth-meter UI.
- §4.6 — Galleon available for stealth Floors via early recruitment (pending Q1).
- §4.9 — Single save slot; save-scumming undefended beyond delete-on-death.
- §Balance — First-pass numbers below are starting points, expected to change in playtest.

---

## Balance — First-Pass Tuning Table
*Starting values only (resolves brief addendum Q1). Not authoritative; expected to shift in playtest. Existing formulas from the Phase-0 code are noted where they already exist.*

**Core stats (Milek, Run start)**
| Stat | Start | Notes |
|---|---|---|
| HP | 20 | Low-number SPD feel; GRIT scales max HP later |
| STR | 5 | Carry/force checks |
| INSTINCT | 7 | Signature stat; drives dodge % and INSTINCT checks (existing: 3%/pt dodge = 21%) |
| GRIT | 5 | Armor reduction (existing: grit/2 = −2 flat, min 1) |
| VOICE | 3 | Reserved; command/persuasion (minimal use in MVP) |
| Backpack slots | 8 | Scarcity pressure |
| Equipped slots | 2 | Weapon + one utility |

**Combat (existing defense formulas retained)**
| Value | Setting |
|---|---|
| Mercenary HP | 8 |
| Mercenary hit damage | 2–4 (pre-armor) |
| Milek melee damage | 2–4 |
| Block (E) | halves next hit after armor (existing) |
| Dodge | 3% × INSTINCT (existing) |
| Arrival grace | on (existing) |

**Hunger**
| Value | Setting |
|---|---|
| Hunger max | 100 |
| Depletion | 1 / turn |
| Starvation threshold | 0 → −1 HP / turn |
| Food Supply restore | Wrapped Bundle (bread) +40; spoiled −10 HP + hunger penalty |

**Identify-by-Use Supply set (true identities randomized per seed)**
| Unidentified | Possible true identities |
|---|---|
| Wrapped Bundle | Stale bread (food +40) / Spoiled meat (−10 HP + hunger penalty) |
| Sealed Waterskin | Clean water (hunger/heal minor) / Tainted (Weaken status) |
| Small Tin | Feverwort paste (cures Bleed) / Rendered fat (minor HP regen over time) |
| Folded Cloth | Bandages (heal/stop Bleed) / Old rags (no effect, weight only) |
| Sealed Letter | Sells for gold later (illiterate Milek can't read; inert utility in MVP) |

**Stealth / Detection**
| Value | Setting |
|---|---|
| FOV sight radius | 8 tiles |
| Enemy vision range | 6 tiles (line-of-sight) |
| Suspicious → Alerted | 2 consecutive turns in sight |
| De-escalation | 1 step per 3 turns without stimulus |
| Distraction Noise radius | 5 tiles |
| Distraction uses | 2 per Floor (or 6-turn cooldown) |

**Last Stand**
| Value | Setting |
|---|---|
| Trigger | first lethal hit per Run |
| Survive at | 1 HP, flagged desperate, 1 turn |
| Uses | 1 per Run |

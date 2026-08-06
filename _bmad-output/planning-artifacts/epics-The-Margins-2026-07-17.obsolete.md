---
stepsCompleted: [step-01, step-02, step-03, step-04]
inputDocuments:
  - _bmad-output/planning-artifacts/prds/prd-The-Margins-2026-07-17/prd.md
  - _bmad-output/planning-artifacts/prds/prd-The-Margins-2026-07-17/addendum.md
  - _bmad-output/planning-artifacts/architecture/architecture-The-Margins-2026-07-17/ARCHITECTURE-SPINE.md
  - _bmad-output/planning-artifacts/game-vision-northstar.md
  - _bmad-output/planning-artifacts/opening-design-act0-forest.md
---

# The Margins (MVP — Route 1 Vertical Slice) - Epic Breakdown

## Overview

This document decomposes the PRD (21 FRs) and the Architecture Spine (AD-1..AD-12) into implementable epics and stories for the MVP: the Route 1 "The Caravan Road" vertical slice. Brownfield Java 17 + libGDX, solo developer. Epics are sequenced foundation-first because the MVP is a strangler refactor of the existing 303-line `RogueGameScreen` — most feature epics attach as *systems* to the foundation Epic 1 builds, so building it first avoids repeated churn of the same core files (per the workflow's shared-file consolidation guidance).

> **Scope layering (added 2026-07-17).** Epics **1–6 remain the MVP vertical slice** and the first shippable milestone — unchanged. Epics **7+ are the North Star expansion** (`game-vision-northstar.md`): the full hybrid story + survival game, systems-first, one signature system foregrounded per region, regions mapped to the novel's road. Direction is **depth over speed — the roadmap intentionally exceeds 30 epics.** The MVP's Route-1 scaffolding (Epic 6's procedural floors / "Five Nights" opening / reunion) is a *prototype* of the road that the later Region epics (20–26) build at full fidelity and supersede. New epics below carry **story outlines**; full acceptance criteria are authored per-epic via `create-story` when each epic is scheduled (just-in-time), the same way Epics 1–6 were contexted.

## Requirements Inventory

### Functional Requirements

- FR-1: Line-of-sight visibility (bounded sight radius, blocked by walls).
- FR-2: Explored memory (fog) — seen terrain dimmed, dynamic entities not persisted.
- FR-3: Enemy patrol behavior while Unaware.
- FR-4: Detection state machine (Unaware → Suspicious → Alerted, with de-escalation).
- FR-5: Noise propagation (events raise/redirect Detection).
- FR-6: Branching dialogue presentation that suspends turn processing.
- FR-7: INSTINCT-gated choices routing to success/failure branches.
- FR-8: Scene/quest flags set and read to gate content.
- FR-9: Backpack + equipped slots with capacity handling.
- FR-10: Use / drop item actions.
- FR-11: Per-seed Supply identity randomization.
- FR-12: Identify-on-use and run-persistent identification.
- FR-13: Companion follow & single party slot across floors.
- FR-14: Leverage — Galleon Distraction generates Noise to pull patrols.
- FR-15: Bond tracking from tagged dialogue choices.
- FR-16: Last Stand trigger on first otherwise-lethal hit.
- FR-17: True permadeath after Last Stand spent.
- FR-18: Route progression — 3 procedural Floors then authored Story Floor.
- FR-19: Route completion end-state (completion screen).
- FR-20: Single-run save/persistence and resume.
- FR-21: Permadeath clears the save.

#### North Star (Vision) Requirements — Epics 7+

- FR-22: Day/night time-of-day cycle affecting visibility, enemy spawns, NPC schedules, and time-gated resources.
- FR-23: Dynamic weather (rain, storm, wind, snow) affecting visibility, movement, survival meters, and resource availability.
- FR-24: Temperature system (cold/heat) driven by weather, biome, fire, and shelter; out-of-band temperature harms the player.
- FR-25: Fatigue system — accrues with activity/time-awake, restored by rest/sleep, degrades combat/stealth/gather performance.
- FR-26: Wetness system — raised by rain/water crossings, lowers temperature, dried by fire/time/shelter.
- FR-27: Fire & shelter — build/maintain campfires and shelter that mitigate temperature/wetness/fatigue and enable rest.
- FR-28: Environmental hazards (poison, ice, heat/dust, hazardous terrain) that damage or impair.
- FR-29: Crafting & components — combine gathered materials into tools/weapons/armor/consumables/fire/food via recipes.
- FR-30: Treasure & loot — placed caches and drops with rarity and region-specific loot tables.
- FR-31: Hidden resource nodes — environmental gather points (tall grass, logs, trees, rocks) yielding biome-specific materials, some time/weather-gated.
- FR-32: Combat system — weapon-based attacks and damage; enemy variety by region and time of day.
- FR-33: Skills system — unlockable combat and survival skills that improve abilities and gathering/crafting/exploration efficiency.
- FR-34: Player progression — persistent stats/mastery that combat and survival draw on.
- FR-35: Bosses & mini-bosses — per-region encounter framework supporting fightable, evasion (unwinnable-yet), and social (outmaneuver) boss types; a boss/exit gates region progression.
- FR-36: NPC behavior & schedules — allies, neutrals, and bystanders with daily schedules, states, and interactions.
- FR-37: Side quest system — optional quests granting loot, gear, lore, and NPC relationships.
- FR-38: Region (Arc) progression — a multi-region horizontal world; each region has a biome, region-specific enemies, NPCs, resources, quests, a signature system, a mini-boss/boss, and an exit to the next; travel advances the story.
- FR-39: Per-region signature-system introduction — each new region foregrounds a new mechanic rather than only raising difficulty.
- FR-40: Main storyline adaptation — regions map to the novel's road; story beats gate and advance via quests and flags.

### NonFunctional Requirements

- NFR-1 (Reliability): Route 1 completes start→finish with no progression-blocking bug (SM-1). No system may deadlock the turn pipeline.
- NFR-2 (Reproducibility): A given seed reproduces the same floor layout and identify-by-use bindings (enables debugging and honest playtests) — AD-5.
- NFR-3 (Solo-maintainability): New mechanics are isolated systems operating on `RunState`, headless-testable without a rendering context — AD-2.
- NFR-4 (Save integrity): `RunState` serialize→deserialize round-trips without loss or object-graph duplication — AD-6, serialization-root convention.
- NFR-5 (Scope): No Vision-tier system (Trust Meter, roster, endings, etc.) is introduced — PRD §5.

### Additional Requirements

From the Architecture Spine (technical requirements shaping implementation):

- Strangler layering: model (state/systems) ⟵ screen; no game rule in `RogueGameScreen`; no libGDX render types in model packages (AD-1, AD-2).
- `RunState` single-owner + single serialization root; entities carry no serialized back-refs to map/RNG (AD-3, save convention).
- One seeded RNG for all gameplay randomness (AD-5).
- Save via libGDX `Json`, single slot, delete-on-death (AD-6).
- Detection = radius + line-of-sight, no directional cones; Noise as a `RunState` event queue (AD-9).
- INSTINCT checks deterministic threshold compares (AD-8).
- `FloorGenerator` gains a fixed-layout mode for the Story Floor (AD-11).
- Identify-by-use bindings on `RunState`, ratifying the existing stackable `Inventory` type/count model (AD-12).
- First-pass balance values from the PRD Balance table seed the tuning constants.

### Non-Code Workstream

- Art: minimum unique sprites for Milek, Galleon, and one scavenger NPC (schedule risk, not a code blocker) — tracked as a story in Epic 6.

### FR Coverage Map

- FR-1: Epic 2 — Field of view
- FR-2: Epic 2 — Explored fog memory
- FR-3: Epic 2 — Enemy patrol
- FR-4: Epic 2 — Detection state machine
- FR-5: Epic 2 — Noise propagation
- FR-6: Epic 5 — Branching dialogue
- FR-7: Epic 5 — INSTINCT-gated choices
- FR-8: Epic 5 — Scene/quest flags
- FR-9: Epic 3 — Backpack & equipped slots
- FR-10: Epic 3 — Use / drop
- FR-11: Epic 3 — Per-seed identity
- FR-12: Epic 3 — Identify-on-use persistence
- FR-13: Epic 4 — Companion follow
- FR-14: Epic 4 — Distraction leverage
- FR-15: Epic 4 — Bond tracking (paid off in Epic 6 reunion)
- FR-16: Epic 1 — Last Stand trigger
- FR-17: Epic 1 — True permadeath
- FR-18: Epic 6 — Route progression + Story Floor
- FR-19: Epic 6 — Route completion
- FR-20: Epic 1 — Save/resume
- FR-21: Epic 1 — Permadeath clears save
- FR-22: Epic 7 — Day/night cycle
- FR-23: Epic 8 — Dynamic weather
- FR-24: Epic 9 — Temperature
- FR-25: Epic 9 — Fatigue
- FR-26: Epic 9 — Wetness
- FR-27: Epic 10 — Fire, shelter & rest
- FR-28: Epic 11 — Environmental hazards
- FR-29: Epic 12 — Crafting & components
- FR-30: Epic 13 — Treasure & loot
- FR-31: Epic 13 — Hidden resource nodes
- FR-32: Epic 14 — Combat depth
- FR-33: Epic 15 — Skills
- FR-34: Epic 14 — Player progression
- FR-35: Epic 16 — Bosses & mini-bosses
- FR-36: Epic 17 — NPC behavior & schedules
- FR-37: Epic 18 — Side quests
- FR-38: Epic 19 — Region (Arc) progression framework
- FR-39: Epic 19 — Per-region signature systems
- FR-40: Epic 19 — Main storyline adaptation (realized in Region epics 20–26)

## Epic List

### Epic 1: Foundation, Save & Survival
Rebuild the run on a `RunState` + `TurnEngine` spine (strangler-extracted from `RogueGameScreen`) so every later mechanic attaches as a system, then deliver the player-facing payoffs that spine makes possible: quit-and-resume a run, and a Last Stand reprieve before permadeath.
**FRs covered:** FR-16, FR-17, FR-20, FR-21

### Epic 2: Sight & Stealth
The Caravan Road can be *sneaked*. Milek sees only by line of sight; mercenaries patrol, notice, and hunt — and can be avoided rather than fought.
**FRs covered:** FR-1, FR-2, FR-3, FR-4, FR-5

### Epic 3: Scavenged Supplies
Scarcity you can feel: a finite backpack and unlabeled supplies that might save you or hurt you, learned only by trying them.
**FRs covered:** FR-9, FR-10, FR-11, FR-12

### Epic 4: Galleon at Your Side
A companion who changes *how* a floor is solved, not just how hard you hit — Galleon follows, draws patrols off with a shout, and remembers how you treat him.
**FRs covered:** FR-13, FR-14, FR-15

### Epic 5: Voices on the Road
Authored moments interrupt the crawl: conversations with real choices, INSTINCT reads that reward Milek's cunning, and flags that remember what you did.
**FRs covered:** FR-6, FR-7, FR-8

### Epic 6: The Caravan Road (Capstone)
Assemble the slice into a playable Route: three procedural floors into an authored reunion, bookended by the "Five Nights, Again" opening and a completion beat — with the minimum art to make it read.
**FRs covered:** FR-18, FR-19 (integrates all prior epics)

---

> **↓ North Star expansion (Epics 7+).** Post-MVP. Grouped in phases; each attaches to the Epic 1 spine as systems, then Region epics build content on top. Story outlines only until scheduled.

**Phase A — Survival Simulation** (deepens the survival loop the MVP starts)

### Epic 7: Time & the Day/Night Cycle
A turn-driven clock that turns the world over: light and visibility shift, spawns and NPC schedules change, and some resources only appear at certain hours.
**FRs covered:** FR-22

### Epic 8: Dynamic Weather
Rain, storm, wind, and snow that are never just weather — they cut visibility, drag movement, and reshape what you can gather and survive.
**FRs covered:** FR-23

### Epic 9: The Body — Temperature, Fatigue & Wetness
Three interlocking realism meters beside Hunger. Cold and heat, exhaustion, and getting soaked — each a pressure, introduced one region at a time so the loop never becomes meter-soup.
**FRs covered:** FR-24, FR-25, FR-26

### Epic 10: Fire, Shelter & Rest
The crafted answer to the body: campfires and shelter that warm, dry, and let you rest — and a decision about *where and when* it's safe to stop.
**FRs covered:** FR-27

### Epic 11: Environmental Hazards
The world bites back: poison, ice, heat and dust, hazardous terrain — region-signature dangers that reward reading the ground.
**FRs covered:** FR-28

**Phase B — Gameplay Depth** (turns survival into a game with mastery)

### Epic 12: Crafting & Components
Materials become tools, weapons, armor, fire, and food through recipes — the engine that connects gathering to survival and combat.
**FRs covered:** FR-29

### Epic 13: Loot, Treasure & Hidden Resources
Reasons to explore: environmental gather nodes (grass/logs/trees/rocks), placed caches, and region loot tables with rarity.
**FRs covered:** FR-30, FR-31

### Epic 14: Combat Depth & Progression
Combat that's real but honest: weapon-based attacks, region- and time-varied enemies, and persistent mastery Milek grows — fighting is a tool, not always the answer.
**FRs covered:** FR-32, FR-34

### Epic 15: Skills
Unlockable combat and survival techniques that improve fighting *and* gathering, crafting, and exploration efficiency — how the "ant" gets sharper.
**FRs covered:** FR-33

### Epic 16: Bosses & Mini-Bosses
A per-region encounter framework for three boss kinds — **fightable** (regional threats), **evasion** (unwinnable-yet walls like Spearshot), and **social** (outmaneuver, like Swan) — one gating each region's exit.
**FRs covered:** FR-35

**Phase C — World & Content Framework**

### Epic 17: NPCs, Neutrals & Schedules
The road is peopled: allies, neutral bystanders, and benefactors with daily schedules and states you can read, help, or avoid.
**FRs covered:** FR-36

### Epic 18: Side Quests
Optional threads that reward exploration with gear, lore, and relationships — the texture between story beats.
**FRs covered:** FR-37

### Epic 19: Region (Arc) Progression Framework
Generalizes the Route into the novel's horizontal road: a region model with biome, enemies, NPCs, resources, quests, a signature system, a boss, and an exit — travel advances the story.
**FRs covered:** FR-38, FR-39, FR-40

**Phase D — Regions of the Road** (content epics built on the framework; each = one leg of the novel)

### Epic 20: Region 0 — Coneros: The Fall
The fully-playable Prologue→Ch2 intro (Act 0) — controls taught by loss. Realizes `opening-design-act0-forest.md`.
**FRs covered:** FR-40 (+ integrates prior systems)

### Epic 21: Region 1 — North Pines: Whispering Forest
Act 1, the first free-play region: hunger, fire/shelter, rain, crafting, stealth, the five-night camp raid, the "trailed home" climax. Companion = Erik.
**FRs covered:** FR-38, FR-39 (+ Phase A/B systems)

### Epic 22: Region 2 — Pinehurst & The Convoy
Captivity as a region: the gap-in-the-door observation loop, feeding-line rationing, NPC schedules. Allies: Henry, the Pack.
**FRs covered:** FR-38, FR-36

### Epic 23: Region 3 — Tradewick: Gray Law
A lawless hub of skills and side quests, crowd-stealth and "notability." Social boss: Bulwark Swan. Separation beat.
**FRs covered:** FR-38, FR-37, FR-35

### Epic 24: Region 4 — Coastal Road to Oakdale
Exposure and weather on open ground; the evasion wall arrives — Spearshot, whom you survive, not beat.
**FRs covered:** FR-38, FR-24, FR-35

### Epic 25: Region 5 — Mirko: The Frost Pass
Cold, snowstorms, and fire-or-die; ice hazards and pass bandits in an unmapped bottleneck.
**FRs covered:** FR-38, FR-24, FR-28

### Epic 26: Region 6 — Valens: The Wall
Endgame of Route 1: prison starvation, the crack in the wall, the Twilight Knight mentor, the escape gauntlet, the 7-year threshold.
**FRs covered:** FR-38, FR-40

**Phase E — Beyond Route 1** (future region-sets, not yet decomposed)

### Epics 27+: The Later Arcs
Academic · Revolution · Throne War · One Kingdom · The Heaven Fell — each becomes its own region cluster when the game reaches it. The Blackberry Troupe / Theodore recur as benefactors throughout. Placeholder; decomposed when scheduled.
**FRs covered:** FR-38, FR-40 (future)

---

## Epic 1: Foundation, Save & Survival

Rebuild the run on a headless `RunState` + `TurnEngine` spine so later mechanics attach as ordered systems, then ship the payoffs that spine enables: resume-a-run and Last Stand.

### Story 1.1: Extract RunState as the single owner of run data

As the developer,
I want all run data moved into one `RunState` object,
So that there is a single authoritative source to advance, test, and later serialize.

**Acceptance Criteria:**

**Given** the game is refactored, **When** a run starts, **Then** the tilemap, player, enemy list, hunger, HP, current floor index, and seed all live on one `RunState` instance and nothing else holds a duplicate authoritative copy (AD-3).
**And** the game plays identically to before the refactor (same movement, combat, hunger, stairs, permadeath).
**And** no rendering type (`SpriteBatch`, `ShapeRenderer`, `Screen`, `Texture`) is referenced from `RunState` or the model package (AD-2).

### Story 1.2: Introduce the ordered TurnEngine and extract systems

As the developer,
I want the turn to advance through a `TurnEngine` running ordered systems,
So that `RogueGameScreen` only handles input/render and new mechanics slot in without editing the screen.

**Acceptance Criteria:**

**Given** a player action is submitted, **When** `TurnEngine.advance()` runs, **Then** systems execute in fixed order PlayerAction → Hunger → Enemy AI → Noise resolve → cleanup (AD-4).
**And** the existing hunger tick and combat/defense resolution now live in `HungerSystem` and `CombatSystem`, not inline in the screen.
**And** `RogueGameScreen` contains no game rule — only input forwarding and rendering (AD-2).

### Story 1.3: Single seeded RNG for reproducible runs

As the developer,
I want all gameplay randomness to draw from one seeded RNG on `RunState`,
So that a seed reproduces a run for debugging and honest playtesting.

**Acceptance Criteria:**

**Given** two runs started with the same seed, **When** floors generate and identify-by-use binds, **Then** the layouts and bindings are identical (NFR-2, AD-5).
**And** no gameplay class calls `new Random()` — all draws come from `RunState`'s RNG.

### Story 1.4: Save and resume a run

As Justine (player),
I want to quit mid-run and pick up exactly where I left off,
So that a play session can span more than one sitting (FR-20).

**Acceptance Criteria:**

**Given** an in-progress run, **When** I save and relaunch, **Then** the exact `RunState` (floor, positions, inventory, identified supplies, flags, Bond, hunger, HP, last-stand-used, seed) is restored and play continues from the saved turn (FR-20, AD-6).
**And** serialization uses libGDX `Json` with `RunState` as the sole root; the tilemap and RNG are not double-serialized via entity back-references (save convention, NFR-4).

### Story 1.5: Permadeath clears the save

As Justine (player),
I want a dead run to be truly gone,
So that death carries weight and can't be reloaded (FR-21).

**Acceptance Criteria:**

**Given** true death occurs, **When** the game-over state is reached, **Then** the save slot is deleted and no "continue" option is offered — only a new run can start (FR-21).

### Story 1.6: Last Stand reprieve before permadeath

As Justine (player),
I want one desperate reprieve the first time a blow would kill Milek,
So that death feels earned, not cheap (FR-16, FR-17).

**Acceptance Criteria:**

**Given** Milek has not yet used Last Stand this run, **When** a hit would drop him to ≤0 HP, **Then** he instead survives at 1 HP in a flagged desperate state for that turn and a message communicates the reprieve (FR-16).
**And** **Given** Last Stand is already spent, **When** another lethal event occurs, **Then** the run ends via permadeath (FR-17), and starting a new run resets the flag.

---

## Epic 2: Sight & Stealth

Line-of-sight vision plus patrolling, noticing, hunting enemies — so the Caravan Road can be avoided rather than fought.

### Story 2.1: Line-of-sight field of view

As Justine (player),
I want to see only what Milek can actually see,
So that stealth and ambush have meaning (FR-1).

**Acceptance Criteria:**

**Given** Milek stands in a room, **When** the view renders, **Then** tiles within the sight radius and not blocked by a wall are visible; opening a door reveals the room beyond next turn (FR-1).
**And** enemies and supplies render only while on a currently-visible tile.
**And** FOV runs as a system on `RunState`, using the seeded/deterministic tile data (AD-2).

### Story 2.2: Explored fog memory

As Justine (player),
I want previously-seen terrain to be remembered dimly,
So that I can navigate without seeing live enemies through walls (FR-2).

**Acceptance Criteria:**

**Given** a room I have left, **When** it is out of sight, **Then** its walls/floor render dimmed and any enemy that walked out of sight is no longer drawn (FR-2).

### Story 2.3: Enemy patrol while unaware

As Justine (player),
I want unaware enemies to patrol predictably,
So that I can read patterns and slip past (FR-3).

**Acceptance Criteria:**

**Given** an Unaware enemy and Milek never entering its sight or Noise range, **When** turns pass, **Then** the enemy follows its patrol/idle-wander and never initiates combat (FR-3).

### Story 2.4: Detection state machine

As Justine (player),
I want enemies to notice me gradually and lose me if I break away,
So that stealth is a tense, recoverable state rather than instant failure (FR-4).

**Acceptance Criteria:**

**Given** Milek enters an enemy's vision radius with line-of-sight, **When** turns pass, **Then** the enemy rises Unaware→Suspicious, and sustained sight escalates to Alerted (begins pursuit) per the tuning thresholds (FR-4, AD-9).
**And** **Given** the enemy loses sight and stimulus, **When** the de-escalation interval passes, **Then** it drops one Detection step.

### Story 2.5: Noise propagation

As Justine (player),
I want loud actions to draw enemies,
So that noise is a tool and a risk (FR-5).

**Acceptance Criteria:**

**Given** a Noise event (e.g., forcing a crate), **When** the AI system ticks, **Then** enemies within the Noise radius raise Detection and/or move toward the Noise origin; a silent move produces no such effect (FR-5, AD-9).

---

## Epic 3: Scavenged Supplies

A finite backpack and unlabeled supplies whose nature is learned only by using them.

### Story 3.1: Backpack and equipped slots

As Justine (player),
I want limited carry capacity,
So that scarcity forces real choices (FR-9).

**Acceptance Criteria:**

**Given** a full backpack, **When** I try to pick up another item, **Then** I'm prompted to drop-or-leave; equipping moves an item from backpack to an equipped slot (FR-9).
**And** capacity uses the tuning values (8 backpack / 2 equipped) and ratifies the existing type/count `Inventory` model (AD-12).

### Story 3.2: Use and drop items

As Justine (player),
I want to use or drop what I carry,
So that I can spend supplies and shed weight (FR-10).

**Acceptance Criteria:**

**Given** a consumable, **When** I use it, **Then** its effect applies and it is removed; **When** I drop any item, **Then** it appears on Milek's tile and can be re-picked (FR-10).

### Story 3.3: Per-seed supply identity

As the developer,
I want each Supply type bound to a true identity per seed at run start,
So that identify-by-use is a genuine gamble that varies between runs (FR-11).

**Acceptance Criteria:**

**Given** two runs on different seeds, **When** identities bind, **Then** "Sealed Waterskin" can map to different outcomes; within one run the mapping is stable (FR-11, AD-5, AD-12).

### Story 3.4: Identify-on-use persistence

As Justine (player),
I want using one unknown supply to reveal that whole type,
So that risk converts to knowledge for the rest of the run (FR-12).

**Acceptance Criteria:**

**Given** an unidentified Supply, **When** I use one, **Then** its true identity is revealed and applied, and all remaining supplies of that type show their true identity for the rest of the run (FR-12).

---

## Epic 4: Galleon at Your Side

A companion who changes how a floor is solved and remembers how you treat him.

> **Design constraint:** build this as a generic, entity-agnostic **Companion** system (follow / distraction / Bond), not Galleon-specific — Erik is the first bind (Act 0 + Forest), Galleon binds later on the road. See `opening-design-act0-forest.md`.

### Story 4.1: Companion follow and floor transition

As Justine (player),
I want Galleon to travel with me,
So that he's present to help across the Route (FR-13).

**Acceptance Criteria:**

**Given** Galleon is active, **When** I move or take stairs, **Then** he pathfinds to stay near Milek and transitions to the next floor, occupying the single party slot (FR-13, AD-10).

### Story 4.2: Distraction leverage

As Justine (player),
I want to send Galleon to make noise,
So that I can peel patrols off a path I couldn't force alone (FR-14).

**Acceptance Criteria:**

**Given** Galleon is active and Distraction is available, **When** I trigger it, **Then** a Noise event raises nearby enemies' Detection toward the origin, opening a path away from it (FR-14, AD-9, AD-10).
**And** the ability respects its per-floor use limit / cooldown (2 per floor or 6-turn cooldown).

### Story 4.3: Bond tracking

As the developer,
I want Galleon's Bond to shift from tagged dialogue choices,
So that later authored dialogue can read it for tone (FR-15).

**Acceptance Criteria:**

**Given** a Bond-tagged choice, **When** selected, **Then** Bond updates in the `RunState` flag store and is readable by later dialogue nodes (FR-15, AD-7).
**And** Bond unlocks no combat bonus or transformation in MVP (NFR-5).

---

## Epic 5: Voices on the Road

Authored conversations with real choices, INSTINCT reads, and flags that remember.

### Story 5.1: Branching dialogue that suspends the turn loop

As Justine (player),
I want conversations to pause the action,
So that authored moments land without enemies moving underneath them (FR-6).

**Acceptance Criteria:**

**Given** an authored dialogue node opens, **When** it is on screen, **Then** enemy turns do not advance and selecting one of its 1–4 choices advances to the linked node or closes the scene (FR-6, AD-7).

### Story 5.2: INSTINCT-gated choices

As Justine (player),
I want some options unlocked by Milek's INSTINCT,
So that his cunning is mechanically rewarded (FR-7).

**Acceptance Criteria:**

**Given** an INSTINCT-gated choice, **When** resolved, **Then** the outcome is a deterministic threshold compare (`instinct >= threshold`): success routes to the success branch (may set a flag / grant an item), failure routes elsewhere (FR-7, AD-8).

### Story 5.3: Scene and quest flags

As the developer,
I want authored scenes to set/read run-scoped flags,
So that content can gate on what the player has done (FR-8).

**Acceptance Criteria:**

**Given** a scene sets a flag (e.g., "cache revealed"), **When** later content reads it, **Then** it gates correctly (the cache spawns) and the flag persists through save/reload (FR-8, AD-7, AD-6).

---

## Epic 6: The Caravan Road (Capstone)

Assemble the slice into a playable Route with authored bookends and the minimum art to read it.

> **Authoring target:** the Prologue→Ch2 intro (Act 0) and the Ch3 Forest quest (Act 1) are fully specced in `opening-design-act0-forest.md` — Stories 6.2 (authored floor) and 6.3 ("Five Nights, Again") build from it.

### Story 6.1: Route progression through procedural floors

As Justine (player),
I want to descend three procedural floors of the Caravan Road,
So that the crawl has a shape leading somewhere (FR-18).

**Acceptance Criteria:**

**Given** Floors 1–3, **When** I take stairs, **Then** the next procedural floor loads and Route state advances 1→2→3 (FR-18).
**And** Galleon, inventory, identified supplies, flags, and Last-Stand state carry across transitions.

### Story 6.2: Authored Story Floor

As Justine (player),
I want the fourth floor to be a hand-built place,
So that the reunion happens exactly where and how it should (FR-18).

**Acceptance Criteria:**

**Given** stairs are taken on Floor 3, **When** the next floor loads, **Then** it is the authored fixed layout, not a BSP-generated one (FR-18, AD-11).

### Story 6.3: "Five Nights, Again" opening quest

As Justine (player),
I want the opening beat to establish tone and teach the loop,
So that the slice starts in the novel's voice (content; realizes FR-6/7/8).

**Acceptance Criteria:**

**Given** a new run, **When** it begins, **Then** the opening monologue plays and the scavenger encounter offers the direct / INSTINCT / silent options, with the INSTINCT success revealing the hidden cache (FR-6, FR-7, FR-8).

### Story 6.4: Galleon reunion scene

As Justine (player),
I want the reunion to play as an authored beat with a tone-setting choice,
So that the emotional payoff the slice builds toward actually lands (FR-15, FR-19).

**Acceptance Criteria:**

**Given** the Story Floor is reached, **When** the reunion scene plays, **Then** it presents the tone-setting choice (honest / direct / silent), applies the corresponding Bond change, and reads Bond for warmer dialogue (FR-15, FR-19).

### Story 6.5: Route completion end-state

As Justine (player),
I want a clear end to the slice,
So that "finished" is a real, reachable state (FR-19, SM-1).

**Acceptance Criteria:**

**Given** the reunion scene completes, **When** it resolves, **Then** the game transitions to an end-of-slice / "to be continued" screen and marks the Route complete (FR-19).

### Story 6.6: Minimum unique art pass

As Justine (developer/artist),
I want distinct sprites for the three characters the fiction needs,
So that the slice reads as *The Margins*, not a CraftPix demo (non-code workstream).

**Acceptance Criteria:**

**Given** the MVP cast, **When** art is done, **Then** Milek, Galleon, and the scavenger NPC each have a recognizable, distinct sprite (recolor/commission/creator-drawn — approach chosen by Justine), with everything else reusing existing packs.
**And** this is tracked as a parallel workstream and is not a blocker for code stories.

---

# North Star Expansion — Epics 7+

> Post-MVP. Each section carries a **story outline** (titles + intent). Full acceptance criteria are authored per-epic via `create-story` when scheduled — outlines here keep the roadmap complete without over-committing design not yet decided.

## Epic 7: Time & the Day/Night Cycle

A turn-driven time-of-day clock on `RunState` that changes the world as it turns over: light, spawns, schedules, and time-gated resources.
**FRs covered:** FR-22

**Story outline:**
- 7.1 Time-of-day clock on `RunState` — advances with turns through dawn/day/dusk/night phases (seeded, save-persisted).
- 7.2 Light & visibility by phase — sight radius and fog modulate with the hour.
- 7.3 Time-gated spawns & resources — certain enemies and gather nodes appear only in specific phases.
- 7.4 Wait / sleep to pass time — a rest action that advances the clock to a chosen phase.

## Epic 8: Dynamic Weather

A seeded weather state machine whose states are survival inputs, not decoration.
**FRs covered:** FR-23

**Story outline:**
- 8.1 Weather state machine on `RunState` — clear/rain/storm/wind/snow with seeded transitions.
- 8.2 Weather → visibility & movement — rain/storm cut sight; wind/snow slow travel.
- 8.3 Weather → resource availability — nodes enabled or gated by current weather.
- 8.4 Weather → survival hooks — exposes signals consumed by temperature/wetness (Epic 9).

## Epic 9: The Body — Temperature, Fatigue & Wetness

Three interlocking realism meters beside Hunger, each introduced one region at a time to avoid meter-soup.
**FRs covered:** FR-24, FR-25, FR-26

**Story outline:**
- 9.1 Temperature meter & sources — biome/weather/fire/shelter drive it; out-of-band harms.
- 9.2 Fatigue meter — accrues with activity/time-awake; restored by rest.
- 9.3 Wetness meter — rain/water raise it, it lowers temperature, fire/time dry it.
- 9.4 Meter → performance penalties — combat/stealth/gather degrade at bad levels.
- 9.5 Per-region introduction gating — a region foregrounds one meter as its signature pressure.

## Epic 10: Fire, Shelter & Rest

The crafted answer to the body — and a decision about where and when it's safe to stop.
**FRs covered:** FR-27

**Story outline:**
- 10.1 Build & fuel a campfire — consumes tinder/branches; warms and dries; emits light + noise.
- 10.2 Shelter construction — mitigates weather/temperature and enables safe rest.
- 10.3 Rest / sleep — restores fatigue and advances time, with risk while resting.
- 10.4 Safety tension — fire light and noise draw enemies (ties to detection, Epic 2).

## Epic 11: Environmental Hazards

The world bites back with region-signature dangers that reward reading the ground.
**FRs covered:** FR-28

**Story outline:**
- 11.1 Hazard framework — poison, ice, heat/dust, hazardous terrain as tile/effect types.
- 11.2 Region-signature hazards — each mapped to a region's biome.
- 11.3 Hazard mitigation — gear/crafting/skills reduce or negate.

## Epic 12: Crafting & Components

The engine connecting gathering to survival and combat.
**FRs covered:** FR-29

**Story outline:**
- 12.1 Recipe & component model — materials combine into outputs.
- 12.2 Crafting action & discovery — craft from inventory; learn recipes.
- 12.3 Tool / weapon / armor crafting.
- 12.4 Consumable & food crafting — feeds hunger and healing.
- 12.5 Fire/shelter crafting hooks — shared with Epic 10.

## Epic 13: Loot, Treasure & Hidden Resources

Reasons to explore, matched to each biome.
**FRs covered:** FR-30, FR-31

**Story outline:**
- 13.1 Hidden resource nodes — grass/logs/trees/rocks yield biome-specific materials.
- 13.2 Time/weather-gated nodes — availability shifts with Epics 7–8.
- 13.3 Placed treasure & caches — with rarity.
- 13.4 Region loot tables & drops.

## Epic 14: Combat Depth & Progression

Combat that's real but honest — a tool, not always the answer.
**FRs covered:** FR-32, FR-34

**Story outline:**
- 14.1 Weapon model & attacks — melee/ranged, damage types.
- 14.2 Enemy variety by region & time of day.
- 14.3 Player progression / mastery — persistent stats Milek grows.
- 14.4 Combat ↔ survival coupling — hunger/fatigue/cold modify combat.

## Epic 15: Skills

How the ant gets sharper — unlockable combat and survival techniques.
**FRs covered:** FR-33

**Story outline:**
- 15.1 Skill model & unlocks — combat and survival trees.
- 15.2 Combat skills.
- 15.3 Survival / gathering / crafting / exploration efficiency skills.
- 15.4 Skill acquisition — from mentors, quests, or use (e.g., the Twilight Knight).

## Epic 16: Bosses & Mini-Bosses

A framework for three boss kinds, one gating each region's exit.
**FRs covered:** FR-35

**Story outline:**
- 16.1 Boss encounter framework — type + region-exit gating.
- 16.2 Fightable boss template — a regional threat you can beat.
- 16.3 Evasion boss template — unwinnable-yet; objective is survive/escape (Spearshot).
- 16.4 Social boss template — defeated by outmaneuvering via conditions/dialogue (Swan).

## Epic 17: NPCs, Neutrals & Schedules

The road is peopled — allies, neutrals, and benefactors you can read, help, or avoid.
**FRs covered:** FR-36

**Story outline:**
- 17.1 NPC model & states — ally / neutral / bystander.
- 17.2 Daily schedules — tied to Epic 7 time.
- 17.3 Interaction hooks — talk/trade/help (reuse Epic 5 dialogue).
- 17.4 Relationship generalization — reuse the Epic 4 companion Bond system entity-agnostically.

## Epic 18: Side Quests

The texture between story beats.
**FRs covered:** FR-37

**Story outline:**
- 18.1 Side-quest model & tracking — reuse Epic 5 scene/quest flags.
- 18.2 Quest givers & rewards — gear/lore/relationships.
- 18.3 Region side-quest content hooks.

## Epic 19: Region (Arc) Progression Framework

Generalizes the Route into the novel's horizontal road.
**FRs covered:** FR-38, FR-39, FR-40

**Story outline:**
- 19.1 Region model — biome, enemies, NPCs, resources, quests, signature system, boss, exit.
- 19.2 Region travel & transition — horizontal, story-advancing; supersedes the MVP's procedural-floor Route.
- 19.3 Signature-system introduction hook — each region enables its new mechanic on entry.
- 19.4 Main-storyline flag/beat gating across regions.
- 19.5 World-map / route representation.

## Epic 20: Region 0 — Coneros: The Fall

The fully-playable Prologue→Ch2 intro (Act 0). Realizes `opening-design-act0-forest.md`.
**FRs covered:** FR-40 (+ integrates prior systems)

**Story outline:**
- 20.1 Act 0 authored sequence — river → house → flight → platform.
- 20.2 Controls-by-loss scripting — unwinnable beats with expressive inputs.
- 20.3 Palette shift — frost-ash intro blooms to color at the Forest.
- 20.4 Mora handoff & quest bootstrap.

## Epic 21: Region 1 — North Pines: Whispering Forest

Act 1, the first free-play region. Companion = Erik.
**FRs covered:** FR-38, FR-39 (+ Phase A/B systems)

**Story outline:**
- 21.1 Forest region build — biome, hidden-resource table, hollow-trunk camp.
- 21.2 Survival loop instantiation — hunger/fire/rain/craft/day-night together.
- 21.3 The Lamilla camp & five-night raid — greed-curve loot.
- 21.4 "Trailed home" climax — detection = a tail that endangers the companion.
- 21.5 Ashen-merchants exit / region transition.

## Epic 22: Region 2 — Pinehurst & The Convoy

Captivity as a region.
**FRs covered:** FR-38, FR-36

**Story outline:**
- 22.1 Captivity region — caravan interior; the gap-in-the-door observation loop.
- 22.2 Feeding-line rationing — position decides who eats; starvation is lethal.
- 22.3 NPC allies — Henry and the Pack, with schedules.
- 22.4 Intel / "opening" meter — observation unlocks progression.

## Epic 23: Region 3 — Tradewick: Gray Law

A lawless hub of skills and side quests.
**FRs covered:** FR-38, FR-37, FR-35

**Story outline:**
- 23.1 Gray-law hub — crowd-stealth and a "notability" pressure.
- 23.2 Skills & side-quest content.
- 23.3 Social boss — Bulwark Swan (outmaneuver, don't fight).
- 23.4 Separation beat — the auction; the party splits.

## Epic 24: Region 4 — Coastal Road to Oakdale

Exposure and weather on open ground; the first evasion wall.
**FRs covered:** FR-38, FR-24, FR-35

**Story outline:**
- 24.1 Coastal exposure region — temperature/wind/storm.
- 24.2 Open-ground stealth.
- 24.3 Evasion boss — Spearshot (survive, not beat).

## Epic 25: Region 5 — Mirko: The Frost Pass

Cold, snowstorms, and fire-or-die.
**FRs covered:** FR-38, FR-24, FR-28

**Story outline:**
- 25.1 Frost region — cold/snowstorm pressure.
- 25.2 Ice hazards.
- 25.3 Pass bandits / hunting-beast mini-boss.
- 25.4 Weather-closed-pass gating.

## Epic 26: Region 6 — Valens: The Wall

The endgame of Route 1.
**FRs covered:** FR-38, FR-40

**Story outline:**
- 26.1 Prison region — starvation and the crack in the wall.
- 26.2 The Twilight Knight mentor — skills and story.
- 26.3 Escape gauntlet boss.
- 26.4 The 7-year threshold / Route 1 end-state.

## Epics 27+: The Later Arcs (future)

Academic · Revolution · Throne War · One Kingdom · The Heaven Fell. Each becomes its own region cluster when the game reaches it; the Blackberry Troupe / Theodore recur as benefactors. Placeholder — decomposed when scheduled.
**FRs covered:** FR-38, FR-40 (future)

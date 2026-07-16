---
stepsCompleted: [step-01, step-02, step-03, step-04]
inputDocuments:
  - _bmad-output/planning-artifacts/prds/prd-The-Margins-2026-07-17/prd.md
  - _bmad-output/planning-artifacts/prds/prd-The-Margins-2026-07-17/addendum.md
  - _bmad-output/planning-artifacts/architecture/architecture-The-Margins-2026-07-17/ARCHITECTURE-SPINE.md
---

# The Margins (MVP — Route 1 Vertical Slice) - Epic Breakdown

## Overview

This document decomposes the PRD (21 FRs) and the Architecture Spine (AD-1..AD-12) into implementable epics and stories for the MVP: the Route 1 "The Caravan Road" vertical slice. Brownfield Java 17 + libGDX, solo developer. Epics are sequenced foundation-first because the MVP is a strangler refactor of the existing 303-line `RogueGameScreen` — most feature epics attach as *systems* to the foundation Epic 1 builds, so building it first avoids repeated churn of the same core files (per the workflow's shared-file consolidation guidance).

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

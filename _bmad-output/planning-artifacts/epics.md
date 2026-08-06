---
stepsCompleted: [step-01, step-02, step-03, step-04]
inputDocuments:
  - _bmad-output/planning-artifacts/prds/prd-The-Margin-2026-08-06/prd.md
  - _bmad-output/planning-artifacts/architecture/architecture-The-Margin-2026-08-06/ARCHITECTURE-SPINE.md
---

# The Margin - Epic Breakdown

## Overview

This document provides the complete epic and story breakdown for **The Margin** (the Klein/Herois remake), decomposing the requirements from the finalized PRD (`prd-The-Margin-2026-08-06`, 21 FRs across 6 features) and the finalized Architecture Spine (`architecture-The-Margin-2026-08-06`, 18 ADs, layered headless-core/render-screen paradigm) into implementable stories.

**Brownfield.** The existing `com.margins.rogue` headless core (turn engine, tilemap, FOV, detection, noise, combat, hunger, inventory, save) is *ratified*, not rebuilt (AD-1..7, AD-9..10 `[ADOPTED]`). The **one breaking deviation** is **AD-8**: the floor-descent machinery (`RunState.descend()`/`floorDepth`, `Route` floor-list, `STAIRS_DOWN/UP`, per-floor BSP gen, `TurnEngine` descent trigger) is retired and replaced by one continuous, persistent Herois map. This refactor is foundational and gates the world/foray work — it belongs in Epic 1.

**Sequencing intent (from PRD SM-2 "survival loop genuinely playable first"):** deliver the survival core on a continuous map first, then Act 0/tutorial, then world/foray breadth, then combat/gear, then companions/story, then inventory/currency/economy.

> The prior epic breakdown for the obsolete "The Margins" (Milek / Route 1 Caravan Road) design is archived at `epics-The-Margins-2026-07-17.obsolete.md`.

## Requirements Inventory

### Functional Requirements

**Act 0 — The Fall of Corneo (text intro + onboarding)**
- FR-1: Text intro, read or skip — skippable, paged, SPD-tone; covers before/fall/hand-off; ticks no survival tracks and consumes no turns.
- FR-2: Aldric's diegetic tutorial — all controls (move, scavenge, eat, craft, hide, rest) demonstrated in-world via Aldric's dialogue, no tooltips/UI chrome.
- FR-3: Aldric's capture (the wound) — Aldric leaves the party by capture (recoverable, not death) once the player clears the tutorial; log + discovery seed establish he is held east along the Copper Road.

**The Turn & Survival Core**
- FR-4: Four survival tracks — Hunger, Thirst, Temperature/Exposure, Day/Night; tiered with drift rates; tick only on real turns (survival-clock honesty).
- FR-5: Day/Night and Weather — Day 100 / Night 70 turns; per-170-cycle Weather roll (Clear 40 / Rain 25 / Fog 20 / Storm 10 / Cold Snap 5); Night without light shrinks FOV and raises encounter/aggression; each weather type has pro/con.
- FR-6: Food, water, and purification — sourcing/cooking/purifying; two-step purification (filtration then boiling→0% risk); spoilage ladder; SKILL-governed.
- FR-7: Temperature forces fire — Cold Snap → Frozen in ~38 turns; campfire is warmth+light+cooking+purification but visible/audible (the choice core); torch craftable (Wood+Coal), 60-turn burn.
- FR-8: Debuff system — tiered stacked debuffs with escalation chains (Bacterial: Nausea→Fever→Delirium; Mushroom/toxin track; Diarrhea) and real cures; persist until eaten/drunk/cured out of.

**The Foray & The World**
- FR-9: The persistent, traversable Herois map — no floor-descent; danger is a spatial east/west gradient; hybrid (fixed canon landmarks + procedural wilderness); 11 World-Structures across 3 danger tiers.
- FR-10: The foray loop — leave safe point → travel → scavenge under hazard → return before night/weather; per-structure loot/hazard; night shifts several locations' danger; simultaneous time pressure.
- FR-11: Horizontal progression — no combat-XP; SKILL (doing) governs cooking/purification/repair/lockpicking; knowledge accumulates; gear-with-memory; allies add capability+liability, never raw power.

**Combat & Its Costs**
- FR-12: Combat is viable but costly — turn order by AG; actions Attack/Block/Dodge/Use Item/Flee; every attack costs durability; noise feeds detection; per-act occupation escalation; avoidance/stealth/VOICE beat fighting; Sense-users avoid-only.
- FR-13: Gear-with-memory — ~30 weapons (5 categories × 5 tiers); repair restores durability but permanently lowers max on a decay curve (SKILL-modified); scavenge-on-break returns partial materials; SKILL-based repair with weapon-specific materials.
- FR-14: Permadeath and Last Stand — death ends the run (no save-scumming); Last Stand once per run (GRIT check at 0 HP → survive at 1 HP, no bonus); restart = new procedural forest on the fixed canon spine.

**The Story, People, and Companions**
- FR-15: Companions are full tile-agents — own Status block, own HP pool, own condition/debuff state; never a passive follower; companion death is an open scope decision.
- FR-16: Companion AI is autonomous — behavior state machine (follow/hold/hide/distract/fight-retreat for combatants; take-cover/flee for non-combatants) driven by the same detection/noise rules as enemies; simple orders steer it; companions can blow Klein's stealth.
- FR-17: Companions as help and liability — party size (one active recommended); extra food, noise penalty, woundable body to protect; non-combatants don't fight; Bond deepens via survival+dialogue; loss shapes: Captured/Departure/Death.
- FR-18: The act-gating main story — "Follow the Road" gates Act 1→2, "The Rescue" gates 2→3; Act 3 = the choice → last provisioning → Aldric resolution → closing trap → NW border crossing; the border crossing is the win (a final tense run, not a boss); epilogue seeds connect to main-story canon.
- FR-19: Dialog and quest delivery — text-forward (speaker line, numbered choices, bottom log); dialogue suspends the turn loop (safe pause); VOICE-gated (occasional INS); quests NPC-given or discovery-triggered; four dialog channels.

**Inventory, Currency, and Economy**
- FR-20: Inventory — Quick-Access slots (5 weapon/armor + 3 artifact/ring) + 19 base main slots expandable by up to 5 storage items; weight scales with STR; bags carry durability and thematic traps.
- FR-21: Currency and trade — four-tier currency (Copper→Silver 25:1→Gold 10:1→Royal Gold Plaque 1000:1); scarce by design; two mobile traders (Black Market coin-only guarded; Traveling Wanderer coin-or-barter) are the only sinks; coin is weighted; barter keeps non-coin players unblocked.

### NonFunctional Requirements

*From PRD §4.7 — the SPD-style presentation lock, a system-wide constraint every feature and the architecture conforms to.*

- NFR-1 (Rendering): 2D top-down tile-based only — no 3D, no camera tricks; placeholder colors acceptable pre-art.
- NFR-2 (Interaction): turn-based, tile-by-tile; no real-time mechanics.
- NFR-3 (Text channel): the bottom message log is the primary text surface; the HUD is minimal.
- NFR-4 (Platform): desktop-first (Windows/Linux) via libGDX/LWJGL3; the mobile-friendly-HUD lock keeps a future port open.
- NFR-5 (Performance): the persistent Herois map must render and pathfind within a single turn without perceptible stutter on mid-range desktop hardware.

### Additional Requirements

*Technical requirements from the Architecture Spine (18 ADs) that shape epics/stories. No greenfield starter template — this is brownfield Java 17 + libGDX 1.12.1 (project-pinned), Maven multi-module (core + desktop), tested with JUnit Jupiter 5.10.2.*

- **AD-8 (BREAKING — foundational refactor, Epic 1): retire the floor-descent machinery.** Remove `RunState.descend()`/`floorDepth` (+getters/setters), the `Route` floor-list model (`getFloorCount`), `RogueTile.STAIRS_DOWN/STAIRS_UP` walkable stairs, `FloorGenerator`'s per-floor BSP output, and the `TurnEngine:120` STAIRS_DOWN descent trigger. Replace with one continuous region (`Route` = landmark geography); this is the only intentional break from the ratified brownfield.
- AD-1 (ratified): Layered — `com.margins.rogue` headless core owns all rules+state; `com.margins` screen is pure presentation, reads state and emits `PlayerAction`s only.
- AD-2 (ratified): No core class imports a libGDX render/input/graphics type.
- AD-3 (ratified): `RunState` is the single owner of all run data; nothing else holds an authoritative duplicate.
- AD-4 (ratified + tightened): `TurnEngine.advance` runs the fixed pipeline PlayerAction → Hunger → Detection → Companion AI → Enemy AI (Combat) → Noise resolve → Last Stand → cleanup. Combat resolves at the actor's point in the pipeline via `CombatSystem`; a dead agent never acts later in the turn.
- AD-5 (ratified + extended): a turn commits only on a real player action (survival-clock honesty); the whole party shares one turn — companions act only on player-acted turns.
- AD-6 (ratified + extended): `RunState` serializes via libGDX Json; the tilemap serializes inline (never regenerated from seed on load). Add a `saveVersion`; pre-AD-8 saves are rejected with a clear message, never silently loaded.
- AD-7 (ratified + extended): run-scoped narrative state (flags, quest state, act progression, Bond) lives in `FlagStore`; Bond becomes keyed per-companion (roster of four) for the Remake.
- AD-9 (ratified): noise is a `NoiseEvent` on a per-turn queue; `NoiseSystem.resolve` is the single consumer (centrally nudges enemy detection state); `DetectionSystem` is the separate FOV-driven escalation path. Remake adds player-movement noise on the same channel.
- AD-10 (companion model): each active companion is a full tile-agent (own Status/HP/debuffs/survival tracks + 7-state behavior machine); `CombatSystem` is the single authority over all three HP pools (player/companion/enemy); companions keyed by `bindId`; party-stealth penalty is a `NoiseEvent`; only the active companion is a positioned tile-agent (off-party members are abstract FlagStore/Bond entries).
- AD-11: the occupation-escalation ramp has two channels — off-border presence thickens per act; the NW border cordon thins as acts advance (story-flag-scaled). They must not be merged.
- AD-12: the border crossing is the win — always physically walkable; the cordon's strength is AD-11 channel (b), survivable only with Act-3 readiness; a scripted gauntlet, not a boss.
- AD-13: gear-with-memory — repairs restore durability but permanently lower max on a SKILL-modified decay curve.
- AD-14: Act 0 (text intro + Aldric tutorial) is a core-owned sequence presented by the screen; intro/dialogue/quest-log surfaces commit no turn and tick no survival clock (dialogue-safe-pause).
- AD-15: SPD-style presentation is a ratifying, system-wide constraint (2D top-down tiles, turn-based, bottom log primary, minimal HUD).
- AD-16: performance budget — one turn renders and pathfinds without perceptible stutter; a required perf test must cover the worst case (whole-party turn at max agent count + dense garrison, including per-agent FOV+AI+detection), not just render/pathfind.
- AD-17: the economy is scarce — no infinite-money loop; traders convert surplus into readiness, never into safety.
- AD-18: FOV and light are core mechanics (core-owned, symmetric rules); FOV computed only for the acting agent per step (cached across turn), detection keeps the cheap LOS-to-player check; light alerts enemies through exactly one mechanism — a per-turn `NoiseEvent` (AD-9), LOS-ignoring.

### UX Design Requirements

*None — no bmad-ux design contract exists for this project. Presentation requirements are carried as NFR-1..5 (the SPD-style presentation lock) and AD-15. If a UX spec is authored later, its UX-DRs should be folded in here.*

### FR Coverage Map

- **FR-1** → Epic 2 — skippable, paged text intro (the Fall of Corneo).
- **FR-2** → Epic 2 — Aldric's diegetic tutorial.
- **FR-3** → Epic 2 — Aldric's capture (the wound + rescue seed).
- **FR-4** → Epic 1 — four survival tracks.
- **FR-5** → Epic 1 — day/night + weather.
- **FR-6** → Epic 1 — food, water, purification.
- **FR-7** → Epic 1 — temperature forces fire.
- **FR-8** → Epic 1 — debuff system.
- **FR-9** → Epic 3 — persistent hybrid Herois map + 11 World-Structures + tiers. *(The continuous-map substrate — the AD-8 floor-descent retirement — lands earlier, in Epic 1 Story 1, as the enabling refactor; Epic 3 enriches that substrate into the real hybrid world.)*
- **FR-10** → Epic 3 — the foray loop.
- **FR-11** → Epic 3 — horizontal progression (SKILL + knowledge). *(Facets recur: SKILL→cooking/purification in Epic 1, SKILL→repair/gear-with-memory in Epic 4, allies in Epic 5. Primary owner: Epic 3.)*
- **FR-12** → Epic 4 — combat viable-but-costly.
- **FR-13** → Epic 4 — gear-with-memory.
- **FR-14** → Epic 4 — permadeath + Last Stand.
- **FR-15** → Epic 5 — companions are full tile-agents.
- **FR-16** → Epic 5 — autonomous companion AI.
- **FR-17** → Epic 5 — companions as help and liability.
- **FR-18** → Epic 5 — the act-gating main story + the border-crossing win.
- **FR-19** → Epic 2 — dialog & quest delivery (foundation authored here for the intro; reused and extended in Epic 5).
- **FR-20** → Epic 6 — inventory.
- **FR-21** → Epic 6 — currency & trade.

*NFR-1..5 (the SPD-style presentation lock) are cross-cutting: primary surface stood up in Epic 1, enforced in every epic per AD-15. AD-8 (retire floor-descent → continuous map) is Epic 1, Story 1 — the enabling refactor for everything spatial.*

## Epic List

### Epic 1: Survive the Forest
Stand up a playable survival life on one continuous, persistent Herois map — the loop the whole game is built on, delivered first (PRD SM-2: "survival loop genuinely playable first"). The enabling move is Story 1's AD-8 refactor: retire the floor-descent machinery so the map is one traversable region, then prove Klein can survive several in-game days against Hunger, Thirst, Temperature, the Day/Night clock, weather, food/water/purification, fire, and debuffs — rendered on the SPD-style surface.
**FRs covered:** FR-4, FR-5, FR-6, FR-7, FR-8 (+ AD-8 continuous-map refactor; NFR-1..5 presentation surface).

### Epic 2: The Fall of Corneo — Intro & Onboarding
Bring a new player into the world and teach them to play *in-fiction*: the skippable paged text intro (the before and the fall), Aldric's diegetic tutorial of the Epic 1 survival controls, and his capture — the first loss and the seed of the rescue that pulls east. This epic also authors the **dialog & quest delivery foundation** (text-forward nodes, numbered choices, the dialogue-safe-pause, quest flags) that the intro is the first consumer of and that Epic 5 later extends.
**FRs covered:** FR-1, FR-2, FR-3, FR-19 (governed by AD-14).

### Epic 3: Forays into Herois — The World & Its Loot
Turn the continuous substrate into the real Herois: the hybrid map (fixed canon landmarks + procedural wilderness), the east/west danger gradient, and the 11 World-Structure Locations across 3 danger tiers, each with its loot and hazard. Deliver the complete foray loop — leave a safe point, travel east under the clock, scavenge under hazard, haul back before night/weather turns — and the horizontal-progression spine (grow by SKILL and knowledge, never kill count).
**FRs covered:** FR-9, FR-10, FR-11 (governed by AD-16 performance, AD-17 economy scarcity, AD-18 FOV/light).

### Epic 4: Combat & Its Costs
Make fighting real, winnable, and expensive. Combat resolves at the actor's point in the turn pipeline through the single `CombatSystem` authority; every swing costs weapon durability on the gear-with-memory decay curve (repairs permanently lower max), noise draws reinforcements, the occupation thickens per act, and death is permanent save the once-per-run Last Stand. The lesson the systems teach: avoidance and stealth usually beat a straight fight.
**FRs covered:** FR-12, FR-13, FR-14 (governed by AD-4 combat point, AD-10 single HP authority, AD-11 escalation, AD-13 gear decay).

### Epic 5: Companions & The Story
The capstone: recruit companions as **full tile-agents** (own Status, HP, debuffs, and a behavior state machine — help *and* liability), deepen Bond through shared survival and dialogue, and play the act-gating main story ("Follow the Road" → "The Rescue" → the choice) to the canonical homecoming — the NW border crossing as a final tense run, not a boss. Builds on the Epic 2 dialog foundation, the Epic 4 combat authority (companion FIGHT), and the Epic 3 world (act-gates are spatial pushes).
**FRs covered:** FR-15, FR-16, FR-17, FR-18 (governed by AD-10 companion model, AD-7 per-companion Bond, AD-5 party turn, AD-11/AD-12 escalation & border win, AD-14 dialogue-safe-pause).

### Epic 6: Inventory, Currency & Economy
Make carrying a real constraint and give the player a way to convert surplus into readiness: the hybrid slot+weight inventory (Quick-Access + expandable main slots, weight-by-STR, bags with durability and thematic traps) and the deliberately scarce four-tier currency with its two mobile traders as the only sinks. Enriches the basic ratified inventory the earlier forays used; scarcity (AD-17) keeps the economy always worse than foraging.
**FRs covered:** FR-20, FR-21 (governed by AD-17 scarce economy).

---

## Epic 1: Survive the Forest

Stand up a playable survival life on one continuous, persistent Herois map — the loop the whole game is built on (PRD SM-2). Story 1.1 is the enabling AD-8 refactor; the rest prove Klein can survive several in-game days.

### Story 1.1: Retire floor-descent for one continuous map

As the solo developer,
I want the floor-descent machinery replaced by a single continuous tiled region,
So that the game can express the east/west danger gradient the whole design depends on (AD-8).

**Acceptance Criteria:**

**Given** the ratified brownfield core
**When** the AD-8 refactor is applied
**Then** `RunState.descend()`, `floorDepth` (+ getters/setters), the `Route` floor-list model (`getFloorCount`), `RogueTile.STAIRS_DOWN/STAIRS_UP`, `FloorGenerator`'s per-floor BSP output, and the `TurnEngine` STAIRS_DOWN descent trigger are removed
**And** `FloorGenerator` produces one continuous tiled region and `Route` models landmark geography, not floors
**And** the existing headless tests compile and pass with no reference to descent/stairs symbols.

**Given** a save written before the refactor
**When** it is loaded
**Then** it is rejected with a clear message (AD-6 `saveVersion`), never silently loaded onto the new map shape.

### Story 1.2: Four survival tracks that tick on real turns

As Klein,
I want Hunger, Thirst, Temperature, and the Day/Night clock to advance as I act,
So that survival is a real, honest pressure (FR-4).

**Acceptance Criteria:**

**Given** a run in progress
**When** I take a real action (move, scavenge, eat, craft, fight, hide, rest, wait)
**Then** Hunger, Thirst, Temperature, and the Day/Night clock each advance by one turn's worth per their tiers/drift rates (Hunger: Well Fed→Satisfied→Hungry→Starving; Thirst: Hydrated→Thirsty→Dehydrated→Parched).

**Given** a run in progress
**When** I press a key into a wall or inert tile
**Then** no turn is committed and no survival track ticks (survival-clock honesty, AD-5).

**Given** Hunger reaches Starving or Thirst reaches Parched
**When** the escalation stages are entered
**Then** the listed penalties apply (Starving: Fatigue -35% STR → Trembling -15% AG → Rotting -3 HP/2 turns; Parched: Withered → Trembling → Dried Out -2 HP/5 turns).

### Story 1.3: Day/Night clock and per-cycle weather

As Klein,
I want the day to turn to night and weather to roll each cycle,
So that time and weather shape how I plan (FR-5).

**Acceptance Criteria:**

**Given** a run in progress
**When** turns accumulate
**Then** the clock runs Day 100 turns / Night 70 turns (170-turn cycle), and the current phase is queryable by systems and the HUD.

**Given** a new 170-turn cycle begins
**When** weather is rolled
**Then** exactly one weather type is chosen on the weighted distribution (Clear 40 / Rain 25 / Fog 20 / Storm 10 / Cold Snap 5) and its listed pro/con is in effect for the cycle.

### Story 1.4: FOV and light — the visible-camp tension

As Klein,
I want darkness and fog to shrink what I can see and light to restore it at the cost of noise,
So that a lit camp is a visible camp (FR-5, FR-7, AD-18).

**Acceptance Criteria:**

**Given** it is Night or the weather is Fog
**When** FOV is computed for the acting agent
**Then** the visible radius is shrunk versus clear-day baseline.

**Given** I light a campfire or torch
**When** FOV is recomputed
**Then** my visible radius is restored but reduced versus clear day.

**Given** a lit campfire or torch on my tile
**When** the Noise step resolves (AD-9)
**Then** a `NoiseEvent` is emitted at the light's tile each turn, LOS-ignoring, and in-radius enemies are drawn toward it (an enemy behind a wall can still be alerted).

### Story 1.5: Food, water, and two-step purification

As Klein,
I want to source, cook, and purify food and water,
So that I can eat and drink without poisoning myself (FR-6).

**Acceptance Criteria:**

**Given** a raw water source
**When** I collect from it (Sunken Well stable / Pond / River 20% direct-drink poison risk)
**Then** untreated water carries its source's risk until purified.

**Given** raw water and a fire with coal
**When** I filter (SKILL-based, reduces risk) then boil
**Then** the water reaches 0% risk; filtration alone reduces but does not eliminate it.

**Given** food in inventory
**When** turns pass
**Then** it advances Fresh → Half Rotten → Fully Spoiled; cooked meat and purified water resist spoilage; storage items slow the rate.

**Given** cooking or purification is performed
**When** the outcome is rolled
**Then** it is governed by SKILL (the horizontal growth path, FR-11).

### Story 1.6: Temperature and the campfire/torch

As Klein,
I want cold and heat to threaten me and fire to be the mitigation,
So that warmth is a real choice under scarcity (FR-7).

**Acceptance Criteria:**

**Given** a Cold Snap with no mitigation
**When** ~38 turns pass
**Then** Temperature reaches Frozen (inside one 70-turn Night), with recovery ≈ half the onset rate.

**Given** I build a campfire
**When** it is lit
**Then** it provides warmth + light + a cooking/purification station, is stationary, and is exposed (visible/audible to patrols per Story 1.4).

**Given** Wood and Coal in inventory
**When** I craft a torch
**Then** I get a light-only source with a 60-turn burn (≈ one Night).

**Given** a fire has drawn a patrol
**When** I abandon the camp to flee into fog
**Then** I lose the camp's cooking/purify/warmth benefits for the night — trading heat for stealth (UJ-1 edge case).

### Story 1.7: The debuff system

As Klein,
I want unsafe survival choices to inflict tiered, curable debuffs,
So that scarcity has teeth I must actively answer (FR-8).

**Acceptance Criteria:**

**Given** I consume contaminated food/water
**When** the bacterial track triggers
**Then** it escalates Nausea (-30% STR, 30t) → Fever (-40%, 25t) → Delirium (Paranoia+Vertigo+Crippled, 40t), with Diarrhea running parallel (Stage 1 2× / Stage 2 3× drain, lethal if ignored).

**Given** I eat a toxic mushroom
**When** the toxin track triggers
**Then** the listed effects apply (Rotgut: instant Nausea+Crippled+Diarrhea; Honeymoon→Collapse: hidden 60-turn countdown → Max HP capped at 40% until cured).

**Given** an active debuff
**When** I apply the correct cure (Honey/Honeycomb, Bloodvein, cure items)
**Then** it clears or shortens per the rule; debuffs do NOT clear from turns alone — only eating, drinking, or curing removes them.

### Story 1.8: The survival HUD and message log

As Klein,
I want to see my survival state and read what happens each turn,
So that the survival loop is legible and playable — the Epic 1 milestone (NFR-1..3, AD-15).

**Acceptance Criteria:**

**Given** a run in progress
**When** the screen renders
**Then** a minimal HUD shows the four tracks, current time/weather, and HP, and the bottom message log is the primary text surface (SPD-style).

**Given** any turn resolves
**When** notable events occur (tier change, debuff, weather roll, detection)
**Then** they are written to the bottom message log in the SPD text-forward tone.

**Given** the full Epic 1 loop
**When** a playtester plays
**Then** Klein can survive several in-game days against all four tracks, weather, and debuffs (SM-2 met) with no floor-descent anywhere.

---

## Epic 2: The Fall of Corneo — Intro & Onboarding

Bring the player in and teach them in-fiction, ending on Aldric's capture; author the dialog/quest foundation the intro is the first consumer of (AD-14).

### Story 2.1: Text-forward dialogue nodes with safe pause

As Klein,
I want conversations to present a speaker line and numbered choices while the world holds still,
So that reading and choosing never costs me a turn (FR-19, AD-14).

**Acceptance Criteria:**

**Given** a dialogue node
**When** it is shown
**Then** it renders a speaker line + up to N numbered choices in the text-forward surface, and the turn loop is suspended (no survival tick, no turn committed).

**Given** a choice
**When** I select it
**Then** it can advance the node, set a `FlagStore` flag, fire an effect (Bond gain/loss, item give/take, disposition), or be gated by a stat — and the node closes cleanly back to gameplay.

**Given** VOICE- or INS-gated choices
**When** the gate is evaluated
**Then** the choice routes to the appropriate success/failure branch by the gating stat (VOICE primary).

### Story 2.2: The skippable paged text intro

As a new player,
I want a skippable, paged intro covering the fall of Corneo,
So that I get the story without being forced to read it (FR-1).

**Acceptance Criteria:**

**Given** a new run
**When** the intro plays
**Then** it pages through the before, the fall, and the hand-off (Klein and Aldric fleeing) in the SPD text-forward tone.

**Given** any intro screen
**When** I choose skip
**Then** it jumps to gameplay in one action.

**Given** any intro screen
**When** it is shown
**Then** no survival track ticks and no turn is consumed (AD-14).

### Story 2.3: Aldric's diegetic tutorial

As a new player,
I want Aldric to teach me the controls in-world during the opening flight,
So that I learn to play without tooltips (FR-2).

**Acceptance Criteria:**

**Given** the opening flight after the intro
**When** Aldric speaks
**Then** the how-to-play (move, scavenge, eat, craft, hide, rest) is delivered as in-world dialogue, not UI chrome.

**Given** the tutorial sequence
**When** I follow Aldric's prompts
**Then** each core control from Epic 1 is demonstrated diegetically at least once.

### Story 2.4: Aldric's capture and the rescue seed

As Klein,
I want Aldric taken the moment I've learned the ropes,
So that I feel the first loss and inherit the rescue thread (FR-3).

**Acceptance Criteria:**

**Given** the tutorial is complete
**When** the chasers catch up
**Then** Aldric leaves by capture (a `FlagStore` flag, recoverable later), not death, and Klein escapes alone.

**Given** the capture
**When** it resolves
**Then** the message log and a discovery seed establish Aldric is held **east** along the Copper Road, opening Act 1's wound and UJ-3's east-pull.

### Story 2.5: Quest flags and the passive Journal

As Klein,
I want quests to start from NPCs or discoveries and be looked up in a Journal,
So that the story can gate content without a rigid quest UI (FR-19).

**Acceptance Criteria:**

**Given** an NPC line or a discovery (Journal Note / item)
**When** it triggers a quest
**Then** the quest auto-starts by setting `FlagStore` quest state; killing a quest-giver voids that quest.

**Given** active/known quests
**When** I open the Journal
**Then** it is a passive lookup of quest state — not a delivery mechanism — reading current `FlagStore` state.

---

## Epic 3: Forays into Herois — The World & Its Loot

Turn the continuous substrate into the real Herois and deliver the complete foray loop and horizontal-progression spine.

### Story 3.1: Hybrid map generation

As Klein,
I want a forest that varies each run but keeps Herois's canon shape,
So that every life is a new forest on the same spatial spine (FR-9).

**Acceptance Criteria:**

**Given** a new run
**When** the map generates
**Then** fixed canon landmarks (Corneo, the Copper Road, the NW border crossing, the Watchtower) are placed consistently and procedural wilderness fills between them on the continuous region.

**Given** the generated map
**When** the spatial spine is checked
**Then** west/northwest = home/border and east/interior = the invasion down the Copper Road; danger and loot rise east, safety lies west.

### Story 3.2: The 11 World-Structures across three danger tiers

As Klein,
I want distinct scavenge destinations placed along the danger gradient,
So that where I go trades loot against risk (FR-9, FR-10).

**Acceptance Criteria:**

**Given** the generated map
**When** World-Structures are placed
**Then** all 11 exist across 3 tiers consistent with the east/west spine (T1 Hunter's Blind / Fallen Log Hollow / Forest Shrine / Beehive Grove; T2 Kitchen Camp / Collapsed Watchtower / Poacher's Camp / Sunken Well; T3 Old House / Mercenary Graveyard / Deep Cave Mouth).

**Given** any World-Structure
**When** I reach it
**Then** it exposes its listed loot set and hazard (e.g. Hunter's Blind: rope/small tools/20% Map Fragment, weak-floor-plank hazard; Old House: preserved food/cloth/locked cellar, structural-decay hazard).

### Story 3.3: The foray loop, end to end

As Klein,
I want a complete leave→travel→scavenge→return arc under the clock,
So that a day's foray is a real risk/reward decision (FR-10).

**Acceptance Criteria:**

**Given** a safe point and daylight
**When** I travel east to a World-Structure, scavenge under its hazard, and return
**Then** the loot is carried back and the arc is one continuous traversal (no floor transitions).

**Given** a foray in progress
**When** turns pass
**Then** Hunger (~650 to death), Thirst (~530), Temperature (~38 to Frozen under Cold Snap), and the 170-turn clock all compete for the same turns — travel, scavenge, and return draw from one budget.

**Given** night catches me mid-return
**When** I lack light
**Then** the return is more dangerous (shrunken FOV, active night hazards) — the overreach is concrete (UJ-2 edge case).

### Story 3.4: Night and weather shift location danger

As Klein,
I want locations to change danger with the clock and weather,
So that when I go matters as much as where (FR-10).

**Acceptance Criteria:**

**Given** nightfall
**When** location states update
**Then** the Graveyard's undead and the Sunken Well's creature become active, the Poacher's Camp patrols turn more aggressive, and the Beehive Grove flips *safer* (the sole exception).

**Given** a weather roll (Storm/Fog/Cold Snap)
**When** it stacks on a location
**Then** its listed effect applies (e.g. Storm raises structural-collapse chance at decayed structures).

### Story 3.5: Horizontal progression — SKILL and knowledge

As Klein,
I want to grow by knowing the forest rather than by kills,
So that mastery is horizontal (FR-11, SM-3).

**Acceptance Criteria:**

**Given** any kill
**When** it resolves
**Then** no number, level, or XP rises from it (no combat-XP).

**Given** repeated doing (cooking, purification, repair, lockpicking)
**When** SKILL is exercised
**Then** SKILL-governed outcomes improve, and accumulated knowledge (map fragments, mushroom/water safety, recipes, location dangers) persists and is queryable.

**Given** two players — one who knows the forest, one who doesn't
**When** both play
**Then** the knowledgeable one measurably survives longer with no XP advantage (SM-3).

---

## Epic 4: Combat & Its Costs

Make fighting real, winnable, and expensive, so avoidance and stealth usually win.

### Story 4.1: Combat resolves at the actor's point via CombatSystem

As Klein,
I want to fight with a real action set in turn order,
So that combat is a genuine option with clear resolution (FR-12, AD-4).

**Acceptance Criteria:**

**Given** hostiles in range
**When** combat runs
**Then** turn order is by AG (higher acts first), the action set is Attack / Block / Dodge / Use Item / Flee, and all damage routes through the single `CombatSystem` authority.

**Given** I kill an enemy with my attack
**When** the pipeline continues that turn
**Then** the dead enemy never acts later in the turn (dead-before-act, AD-4); no HP is mutated outside `CombatSystem` for combat.

### Story 4.2: Combat noise draws reinforcements

As Klein,
I want fighting to be loud,
So that violence has a spatial consequence (FR-12, AD-9).

**Acceptance Criteria:**

**Given** an attack or block
**When** it resolves
**Then** a `NoiseEvent` is emitted and consumed by `NoiseSystem.resolve`, drawing in-radius enemies toward the sound (UNAWARE→SUSPICIOUS, retarget).

**Given** a wary patrol
**When** I use VOICE to de-escalate (via dialogue)
**Then** the encounter can be talked down instead of fought.

### Story 4.3: Occupation escalation thickens per act

As Klein,
I want the Gilimans to grow denser as the story advances,
So that fighting gets more punishing over a run (FR-12, AD-11 channel a).

**Acceptance Criteria:**

**Given** an act transition (story flags flip, AD-7/AD-11)
**When** the off-border escalation channel reads them
**Then** eastward/interior Giliman patrols, sweeps, curfews, and bounties get denser and more aggressive per act.

**Given** the escalation ramp
**When** it is applied
**Then** it does NOT touch the NW border cordon (that is AD-11 channel b, owned by Epic 5) — the two channels stay separate.

### Story 4.4: Weapon durability and gear-with-memory

As Klein,
I want weapons to wear and repairs to permanently cost their ceiling,
So that gear is precious (FR-13, AD-13).

**Acceptance Criteria:**

**Given** a weapon
**When** I attack/block/chop/throw
**Then** it loses fixed durability per action, and at 0 it is unusable (~30 weapons across 5 categories × 5 tiers).

**Given** a repair
**When** it is performed
**Then** durability is restored but max is permanently lowered on the SKILL-modified decay curve (Fresh 100% → 1st 90/93/96 → … → 6th+ beyond repair; Low SKILL hard-stops at the 6th).

### Story 4.5: Scavenge-on-break and SKILL-based repair

As Klein,
I want broken gear to return materials and repairs to consume the right ones,
So that the gear economy loops through my SKILL (FR-13).

**Acceptance Criteria:**

**Given** a weapon breaks
**When** I scavenge it
**Then** it returns partial materials by tier (T1–T2: 1–2 base; T3+: 2–3 base + possibly rare; T5: 3–4 base + unique).

**Given** a repairable weapon and the right materials
**When** I repair (spears: Wood+Rope; bows: Wood+String/Sinew; blades: Metal Scrap; etc.)
**Then** the repair is SKILL-based and consumes the weapon-specific materials.

### Story 4.6: Permadeath and Last Stand

As Klein,
I want one life per run with a single last-chance,
So that death means something (FR-14).

**Acceptance Criteria:**

**Given** I drop to 0 HP with Last Stand unused
**When** the auto-check runs
**Then** a GRIT-based roll may leave me at 1 HP with no bonus, once per entire run.

**Given** Last Stand is spent (or death occurs otherwise)
**When** I die
**Then** the run ends, the save is cleared (no save-scumming), and a restart begins a new life on a fresh procedural forest with the fixed canon spine.

---

## Epic 5: Companions & The Story

The capstone: full-tile-agent companions and the act-gating spine to the earned homecoming. Builds on Epic 2 (dialog), Epic 4 (combat authority), Epic 3 (world).

### Story 5.1: Companion as a full tile-agent

As Klein,
I want a companion to be a real body with its own state,
So that it is a person to protect, not a shadow (FR-15, AD-10).

**Acceptance Criteria:**

**Given** an active companion
**When** it exists on the map
**Then** it carries its own Status block, its own HP pool (woundable/healable/incapacitable), and its own condition/debuff and survival state.

**Given** the roster of four (Aldric combat; Mara, Old Fen, Yenna non-combat)
**When** only one is active
**Then** the active companion is a positioned tile-agent and the other three are abstract `FlagStore`/Bond entries (no tile, no noise, no survival tick until active) (AD-10).

### Story 5.2: Autonomous companion behavior state machine

As Klein,
I want my companion to act on its own within the turn,
So that it feels alive, not tethered (FR-16, AD-10).

**Acceptance Criteria:**

**Given** an active companion on a player-acted turn
**When** the Companion AI step runs
**Then** it acts via a behavior state machine (follow / hold / hide / distract / fight-retreat for combatants; take-cover / flee for non-combatants), only on player-acted turns (AD-5), never merely mirroring my movement.

**Given** the same detection/noise rules as enemies
**When** the companion moves or acts
**Then** it obeys them — a hidden companion is quiet, a panicking one emits noise that can blow my stealth.

### Story 5.3: Simple orders steer the companion

As Klein,
I want to tell my companion to hide, hold, or distract,
So that I can use it tactically (FR-16).

**Acceptance Criteria:**

**Given** an active companion
**When** I issue hide / hold / distract
**Then** the state machine switches to that behavior (distraction emits a `NoiseEvent` to pull patrols, reusing the existing distraction action).

### Story 5.4: Companion combat and the liability cost

As Klein,
I want a combatant companion to fight through the same combat authority and cost me to keep,
So that help is never free (FR-17, AD-10).

**Acceptance Criteria:**

**Given** a combatant companion in FIGHT
**When** it attacks
**Then** it routes through `CombatSystem` (no second owner of any HP pool), applying at the Companion-AI step.

**Given** any active companion
**When** it travels with me
**Then** it costs extra food, adds a noise penalty to stealth (via its `NoiseEvent`s), and can be wounded; non-combatants (Mara, Old Fen, Yenna) never fight and must be defended.

### Story 5.5: Bond and the shapes of loss

As Klein,
I want a relationship that deepens and can break,
So that companions carry emotional stakes (FR-17, AD-7).

**Acceptance Criteria:**

**Given** shared survival and dialogue choices
**When** Bond changes
**Then** it is tracked per-companion (keyed by `bindId`); high Bond unlocks lore/loyalty/personal quests, low Bond withholds help or triggers departure, betrayal turns hostile.

**Given** a companion is lost
**When** the loss resolves
**Then** it takes one of three shapes — Captured (recoverable via quest), Departure (low Bond), or Death (permanent) — matching the game's permadeath weight.

### Story 5.6: The act-gating quests

As Klein,
I want main-story quests to advance the acts,
So that the occupation tightens and the story throttles the run (FR-18, AD-11).

**Acceptance Criteria:**

**Given** Act 1
**When** I complete "Follow the Road" (reach the Copper Road corridor, a Tier 2 push)
**Then** a `FlagStore` flag flips and the act advances 1→2, and Epic 4's escalation channel reads it.

**Given** Act 2
**When** I complete "The Rescue" (reach/attempt Aldric's prison)
**Then** success (Aldric rejoins) or failure (lost) both flip the 2→3 gate — Klein now knows the war and must choose.

### Story 5.7: The border-crossing win and epilogue

As Klein,
I want the homecoming to be a final tense run past an act-scaled cordon,
So that the win is escape, earned, not a boss (FR-18, AD-12, AD-11 channel b).

**Acceptance Criteria:**

**Given** Act 3 and the last provisioning done
**When** I reach the NW border
**Then** the crossing is a scripted tense run over a bounded number of turns against the Giliman cordon — always physically walkable, but survivable only with Act-3 readiness because the cordon (AD-11 channel b) has thinned as acts advanced.

**Given** I survive the crossing
**When** I cross into Novelborne
**Then** the canonical ending lands (he makes it home) and the epilogue seeds connect to main-story canon (Corneo → Coneros, the Graveyard filling now) — validating SM-1.

---

## Epic 6: Inventory, Currency & Economy

Make carrying a real constraint and give scarce coin a purpose through two traders.

### Story 6.1: Hybrid slot + weight inventory

As Klein,
I want carrying capacity to be a real constraint that better bags extend,
So that what I haul is a decision (FR-20).

**Acceptance Criteria:**

**Given** my inventory
**When** I open it
**Then** Quick-Access slots (5 weapon/armor-type + 3 artifact/ring) are always available, and the main inventory is 19 base slots expandable by equipping up to 5 storage items (bonuses merge).

**Given** my STR
**When** I carry items
**Then** weight capacity scales with STR and over-capacity is handled (encumbrance), so a full pack limits foray range.

### Story 6.2: Bag durability and thematic traps

As Klein,
I want bags to wear and some to be trapped,
So that storage itself carries risk (FR-20).

**Acceptance Criteria:**

**Given** a bag with durability
**When** it is used/damaged
**Then** its durability tracks like any gear.

**Given** a trapped bag (dart/fire/freeze)
**When** the trap fires
**Then** the bag breaks and 75% of contents drop (recoverable), 25% are lost.

### Story 6.3: Four-tier scarce currency

As Klein,
I want coin to be weighted and scarce,
So that money is never a shortcut to safety (FR-21, AD-17).

**Acceptance Criteria:**

**Given** currency
**When** I hold it
**Then** it uses four tiers (Copper → Silver 25:1 → Gold 10:1 → Royal Gold Plaque 1000:1) and coin is weighted like any item (carrying it costs space).

**Given** the economy
**When** it is played
**Then** there is no infinite-money loop and no coin sink is mandatory for survival (AD-17).

### Story 6.4: The two mobile traders

As Klein,
I want two mobile traders as the only coin sinks,
So that trade converts surplus into readiness, never into safety (FR-21, AD-17).

**Acceptance Criteria:**

**Given** the Traveling Wanderer
**When** I meet him
**Then** I can trade by coin or barter (Copper/Silver tier), keeping non-coin players unblocked.

**Given** the Caravan Black Market Trader
**When** I meet him
**Then** he is coin-only (Gold-tier), guarded, and killing him permanently locks out that trade.

**Given** either trader
**When** I transact
**Then** he buys at a loss and sells at a premium (scarcity holds) — there is no fixed shop.

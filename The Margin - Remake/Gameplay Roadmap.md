---
tags: [the-margin, game-design, systems, gameplay, roadmap]
status: draft
---

# Gameplay Roadmap

Region: [[Region 1 - Forest]] (occupied Herois, ~Year 408–418)
Related: [[Lore System - Storyline Roadmap]] · [[Protagonist - Klein]] · [[Status System]] · [[World Structure System]] · [[NPC System]] · [[Combat System]] · [[Inventory System]] · [[Quest System]]

## Overview
The mechanical companion to the [[Lore System - Storyline Roadmap]]. Where the story roadmap answers *what happens*, this answers *how it plays* — the moment-to-moment loop, how all 17 systems interlock into it, how the player progresses without combat-XP, and a proposed build order for the rebuild. This is also the doc that reveals which **NPC roles** the loop demands (setting up the NPC expansion, which sets up the Companion System).

---

## 1. Core Fantasy & Pillars

**You are Klein — a knight who lost the war on day one, surviving a conquered forest to get home.** Not a hero clearing dungeons; a competent, ordinary man against a catastrophe too big for him.

- **Survival is choice, not chores.** Every hunger/thirst/cold decision is a gamble under scarcity, not a meter to top off.
- **The forest remembers; the occupation tightens.** No descent — the world gets more hostile over the arc while Klein gets more capable.
- **Fighting is viable but costly.** Klein is a trained knight (he *can* fight), but the war is bigger than him. Avoidance, stealth, and preparation usually beat a straight fight; combat spends durability, HP, noise, and draws the occupation down on you.
- **You get stronger by knowing more, not killing more.** Progression is horizontal — skill, gear, knowledge, allies — not stat inflation.
- **The goal is small and human:** go home. Every system is pressure against that one thread.

---

## Presentation & Art Direction *(LOCKED — see Vision.png)*

The visual target is **Shattered Pixel Dungeon's style** — borrowed for *look and feel*, not structure:

- **2D top-down, tile-based** pixel art; dark, moody, pine-and-fog forest palette (Herois).
- **Turn-based**, **tile-by-tile** movement (you physically walk to locations — not menu/node hopping).
- A **message / dialogue log at the bottom** carries state and story as text (*"You are hungry," "a cold draft,"* NPC lines) — text-forward, minimal chrome.
- A **minimal, mobile-friendly HUD**: health bar, a compact status strip, an action/item bar.
- The region reads as a **bounded map with placed location markers** — the numbered POIs are the [[World Structure System]] locations.

**Important:** the *"descend to floor 1 / deeper down"* wording in the reference image is **borrowed SPD text, not a design choice.** **No-descent still stands** — The Margin keeps SPD's *presentation* (tiles, turns, bottom log) but replaces SPD's *structure* (floor descent) and *loop* (fight-to-descend) with the survive-across-a-region design. Keep the skin, swap the skeleton.

---

## 2. The Core Loop (nested)

Four loops nest, fastest to slowest:

### Loop 1 — The Turn *(seconds)*
Turn-based. Each turn Klein takes one action; the world ticks. Every turn you weigh:
> *move · scavenge · use/eat/drink · craft/repair · fight · hide · rest · wait*

Each turn, survival tracks advance ([[Hunger System]], [[Thirst System]], [[Temperature and Exposure System]]), the [[Day & Night Cycle System|day/night]] clock moves, active [[Debuff System|debuffs]] tick, and any nearby threat reacts (detection/noise). One keypress into a wall or an inert item spends no turn (preserving the survival-clock honesty).

### Loop 2 — The Foray *(minutes)*
Leave a safe point → travel to a [[World Structure System|world-structure location]] → scavenge/hunt/harvest under its hazard → haul loot back before hunger/thirst/night/weather turns against you. The **risk/reward tension**: better loot sits in higher-danger tiers. Overreach and you don't make it back.

### Loop 3 — The Day *(a cycle)*
[[Day & Night Cycle System|Day 100 turns / Night 70]] + a rolled [[Weather System|weather]] type. Forage and travel by day; shelter, cook, and mend by night (or brave the dark with a torch and worse odds). [[Temperature and Exposure System|Cold/heat]] drifts with weather + time. A day is the unit you plan against.

### Loop 4 — The Act *(the arc)*
Main-story **[[Quest System|quests]]** gate the three acts (Survive → Understand → Decide). Completing an act's key quest tightens the occupation and opens new ground, pushing Klein toward the way home. This is the long throttle from the [[Lore System - Storyline Roadmap]].

---

## 3. System Map — how the 17 systems layer

| Layer | Systems | Role in the loop |
|---|---|---|
| **Survival pressure** | Hunger, Thirst, Temperature/Exposure, Day-Night, Weather, Debuff | The constant clock you play against; the source of tension every turn |
| **Body & stats** | Status (STR/GRIT/INS/AG/VOICE/SKILL) | Modifies everything; drained by the survival layer (sick/starving = weaker) |
| **Resources & craft** | Food, Inventory, Weapon (gear-with-memory), Currency | What you gather, carry, cook, purify, repair, and spend |
| **The world** | World Structure (11 locations, 3 danger tiers), NPCs | The space you forage/fight/trade across; where risk and reward live |
| **Conflict** | Combat | The costly option when hiding/fleeing fails |
| **Progression** | SKILL, gear, knowledge, allies, (Sense — What-if) | How capability grows — horizontally |
| **Narrative** | Quest, Lore/Storyline, Companion, Dialog | Paces the acts, delivers story, gives Klein help and reasons |

**Key interlocks (the web that makes it a game, not a feature list):**
- **Coal** cooks meat *and* boils water → ties Food ↔ Thirst.
- **SKILL** governs cooking, water purification, *and* gear repair → ties Food ↔ Thirst ↔ Weapon.
- **Trembling** is shared by starving *and* dehydration → ties Hunger ↔ Thirst ↔ Debuff.
- **Weather + Day/Night stack** → compound the survival + threat layers at once.
- **Campfire** = warmth + light + cooking + purifying, but visible/noisy → ties Temperature ↔ Day-Night ↔ Food ↔ Thirst ↔ threat.

---

## 4. Tension & Threat

The moment-to-moment danger engine, layered on top of survival:

- **Detection / noise / hide.** Klein is hunted (Gilimans, mercenaries, predators). INS senses threats beyond sight ([[Status System]]); actions make noise; you can break line of sight and cool pursuit down. *(The proven detection/noise/FOV scaffolding from the current build maps directly onto this.)*
- **Fight, flee, or hide.** Combat ([[Combat System]]) is real and winnable — Klein's a knight — but it **costs**: HP, weapon durability, noise that pulls reinforcements, and the occupation escalating. Fleeing costs stamina/position; hiding costs time (survival clock ticks).
- **The world modulates danger.** Night, fog, and storms raise threat; several [[Day & Night Cycle System|locations flip hostile after dark]] (Graveyard undead, wolves at the Hunter's Blind). Danger isn't a constant — it's a *forecast* you plan around.

---

## 5. Safe Points — the Camp Hub

Survival games need a breath between forays. Here it's the **Campfire / Camp** — and it's a gamble:

- **Gives:** warmth (Temperature), light (Night), cooking (Food), water-boiling (Thirst), rest/regen, a place to repair.
- **Costs:** it's *visible and audible* — a fire in occupied woods can draw patrols, predators, or worse at night. Safety and exposure in one act.
- Camps are semi-temporary; better shelter (Fallen Log Hollow, found structures) trades mobility for security. This is the "settle vs. keep moving" rhythm of the whole game.

---

## 6. Progression Without Combat-XP

No "kill things → numbers go up." Klein grows **horizontally**:

| Vector | How it grows | System |
|---|---|---|
| **SKILL** | Doing — cooking, purifying, repairing, lockpicking, crafting | Status, Food, Thirst, Weapon, Inventory |
| **Gear** | Found & repaired (gear-with-memory: repairs lower the max, so gear is precious) | Weapon System |
| **Knowledge** | Map fragments, learned mushroom/water safety, recipes, location dangers | World Structure, Food, Thirst |
| **Carry / logistics** | Better bags = more range before you must turn back | Inventory |
| **Allies** | Companions add capabilities Klein lacks | Companion System |
| **(Sense — What-if)** | Awaken a Sense ability at the Sensory Burden's permanent cost | Status / the "Ant" branch |

The player's real power growth is *learning the forest.* A veteran player is dangerous because they *know things*, not because their numbers are big.

---

## 7. NPC Roles the Loop Demands *(sets up the NPC expansion)*

Reading the loop above, the world needs these NPC **roles** — this is the spec target for expanding [[NPC System]] (currently only the two traders):

| Role | Examples | Loop function |
|---|---|---|
| **Hostile** | Gilimans (occupiers), hired mercenaries, predators | The threat — the reason to hide/fight/flee |
| **Trader** | Traveling Wanderer, Black Market Trader *(already speced)* | Convert loot ↔ coin ↔ gear/cures |
| **Neutral / world-texture** | Refugees, survivors, hermits, Heretics | Make the forest feel inhabited; info, rumors, small trades |
| **Quest-giver** | Story NPCs + side-quest givers | Feed the Quest System (incl. the act-gating main quests) |
| **Companion** | The allies who travel with Klein | ← the subclass the whole expansion is building toward |

> **Companions are a specialized NPC** — a neutral/ally who joins Klein, travels with him, adds capability, and carries lore. The NPC expansion defines the shared NPC foundation; the [[Companion System]] then deep-dives the companion subclass.

---

## 8. Run Structure

- **One continuous life**, permadeath = the end (SPD-style). **No descent** — a **persistent, traversable Herois** (see [[Herois Region]]) with the 11 locations placed across it. Danger is **low near the western border, rising toward the east/interior** (the occupiers) — where the better loot lives. Klein is pulled two ways: east for resources, west to escape.
- **Start:** a **text intro** (read or skip) tells the fall of Corneo; **gameplay spawns Klein in the forest, already fleeing** the chasers. No playable "morning" — the game opens *in motion*, with **run-and-hide** as the first beat. Tutorial happens in this opening flight.
- **Middle:** survive, forage, trade, complete main quests to advance the acts as the occupation tightens.
- **Win:** cross the **NW border** into Novelborne (canonical GET HOME ending). The Deep Cave Mouth is a *separate* threshold (Region-2 / expansion hook), not the exit.
- **Death:** permadeath; replay a fresh life. (Replay is where the later **"What if…"** endings live.)

---

## 9. Build / Development Order *(proposal — deferrable)*

A **vertical-slice-first** plan: get the core survival loop *playable* before layering breadth. Engine is still an open decision (see below); the phases are engine-agnostic.

| Phase | Goal | Systems |
|---|---|---|
| **0 — Skeleton** | Turn engine, grid map, movement, FOV, one survival track playable | Turn loop, tilemap, Hunger |
| **1 — Survival core** | The real loop: survive a few days in the forest | Thirst, Temperature, Day-Night, Weather, Food, Inventory |
| **2 — The world & threat** | Places to go and things that hunt you | World Structure (Tier 1–2), Detection/Noise, Hostile NPCs, Combat basics |
| **3 — Economy & gear** | Loot has meaning; gear is precious | Currency, Traders, Weapon (durability/repair), Debuff cures |
| **4 — Story & people** | The game becomes *Klein's* story | Quest (incl. act-gating), Lore acts, NPC expansion, Companion, Dialog |
| **5 — Depth & endings** | The identity systems & replay | Sense/"Ant", What-if endings, Status full stat integration, polish |

> **Reuse note:** the current Java/libGDX build already has a working turn engine, tilemap, FOV, detection, noise, combat, hunger, inventory, and save/serialization. Much of Phases 0–2 maps directly onto that scaffolding *if* the stack is kept (see engine open item).

---

## Open Items
- [ ] **Engine / stack decision** — keep Java + libGDX (and reuse the proven turn/FOV/detection/save scaffolding) or rebuild on a new stack? Gates the whole build order. *(The SPD-style presentation lock leans toward keeping libGDX — the current build is already 2D-tiled, turn-based, with a bottom message log.)*
- [ ] **Map structure** — is the persistent Herois handcrafted, procedural, or hybrid? (No-descent means the *region* replaces floors — how is it generated/laid out?)
- [ ] Define the moment-to-moment **UI/HUD** for a survival game (four survival tracks + threat + time + weather is a lot to surface cleanly) — lean on the bottom **message log** to carry state as flavor, per the SPD-style lock
- [x] ~~Onboarding-in-motion~~ → **Aldric teaches it.** The surviving-comrade companion delivers the how-to-play diegetically during the opening flight, then is **captured** once the tutorial completes (seeds a rescue quest). See [[Lore System - Storyline Roadmap]] Act 0.
- [ ] Confirm the **stealth-vs-combat balance** for a trained-knight protagonist (how punishing is fighting, exactly?)
- [ ] Decide **how many in-game days / how long** a full canonical run is meant to take
- [ ] Expand [[NPC System]] to the five roles above → then [[Companion System]]
- [ ] Add the main-story / act-gating quest category to [[Quest System]]

## Changelog
- Established the nested core loop (Turn → Foray → Day → Act) and mapped all 17 systems into layers
- Defined the tension engine (detection/noise/hide; combat as viable-but-costly for a knight) and the Camp hub gamble
- Locked progression as horizontal (SKILL / gear / knowledge / allies / Sense), not combat-XP
- Named the five NPC roles the loop demands — the spec target for the NPC expansion → Companion System
- Proposed a vertical-slice-first, engine-agnostic build order; flagged reuse of the existing Java/libGDX scaffolding
- Locked presentation & art direction: **SPD-style** (2D tiled, turn-based, bottom message log, moody pixel-art), tile-by-tile traversal; confirmed the reference's "descend/floor" text is borrowed and no-descent stands
- Corrected run-structure geography to canon: danger rises **east** (interior/occupiers), home is the **NW border crossing** (not the Deep Cave Mouth); grounded the region in [[Herois Region]]
- Opening reworked: **text intro (read/skip)** for the fall of Corneo; gameplay starts with Klein **fleeing in the forest**; onboarding/tutorial moves into that opening flight
- Resolved onboarding: **Aldric** (companion) is the diegetic tutorial guide, captured after the how-to-play — teaches the game, then becomes the first loss + a rescue quest

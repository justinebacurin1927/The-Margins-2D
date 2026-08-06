---
tags: [the-margin, game-design, systems, npcs]
status: draft
---

# NPC System

Region: [[Herois Region]]
Related: [[Gameplay Roadmap]] · [[Currency System]] · [[World Structure System]] · [[Companion System]] · [[Dialog System]] · [[Quest System]] · [[Combat System]] · [[Debuff System]]

## Overview
Everyone Klein meets in the Herois forest — human or beast. NPCs fill **five roles**, each serving a function in the core loop (see [[Gameplay Roadmap]] §7): **Hostile, Trader, Neutral, Quest-giver, Companion.** Roles can stack on one NPC (a refugee who trades, gives a quest, and later joins). All are grounded in the war's factions (see [[Herois Region]] · [[evermove]]).

## The Five Roles

| Role | Loop function | Examples |
|---|---|---|
| **Hostile** | The threat — the reason to hide / flee / fight | Gilimans, hired mercenaries, Evermove Sense-users, predators |
| **Trader** | Convert loot ↔ coin ↔ gear / cures | Traveling Wanderer, Caravan Black Market Trader |
| **Neutral** | World texture — info, small trades, atmosphere | Refugees, deserters, hermits, Heretics |
| **Quest-giver** | Feed the [[Quest System]] (incl. act-gating main quests) | Story NPCs, side-quest givers, found journals |
| **Companion** | Join Klein, travel, add capability, carry lore | → deep-dive in [[Companion System]] |

## Faction & Allegiance

The war sets every disposition. Klein is a **Novelborne knight** — the world reads him through that:

- **Novelborne (Klein's side, losing):** the Lamilla scout-knights, the shattered garrison (deserting/dying), Novelborne civilians & refugees. Mostly allies or wary neutrals — a scattered, defeated side.
- **Evermove (the occupiers):** the **Gilimans** (Herois enforcers / conquest arm), **hired mercenaries** (Valens' mercenary economy), and rare **Sense-users / knights** (the invasion's spearhead). Hostile to Klein.
- **Stateless / amoral:** refugees from the western unknown kingdom, underground **Heretics** (old-faith faithful), poachers, hermits, and the coin-only **Black Market** organization — loyal to no crown.

Faction sets the *default* disposition; **reputation** (per-NPC and per-faction) and Klein's actions shift it.

## Role 1 — Hostile

The human and animal threats the survival loop plays against. All use detection / noise (INS + FOV); combat is **viable but costly** (see [[Combat System]]); [[Day & Night Cycle System|day/night]] modulates them.

- **Gilimans** — Evermove's Herois enforcers / occupiers. The *primary* human threat: patrols, sweeps, curfews, bounties. They **escalate with the occupation** — thicker and more aggressive each act (the story ramp).
- **Hired mercenaries** — contracted from Valens for the conquest; tied to the [[World Structure System|Watchtower → Kitchen Camp → Graveyard]] company, a **living faction in Klein's era** (ancient lore by Year 468). Loot-motivated, deadlier than rank patrols.
- **Evermove Sense-users / knights** — the "elephants." Rare, elite, mostly **scripted or to-be-avoided**, not routine fights — the force that broke Klein's garrison.
- **Predators & wildlife** — wolves (night; Hunter's Blind, Watchtower), coyotes, snakes/insects (Venom), the **Graveyard undead** (night-only), the cave-mouth guardian.

## Role 2 — Traders

Two recurring trader NPCs, intentionally opposite in tone and mechanics — one rewards engagement, one punishes aggression.

---

## 🧭 Traveling Wanderer

*A former knight who traded his blade for a trader's pack.*

### Background
Once part of the same knight/mercenary company tied to [[Kitchen Camp]], [[Collapsed Watchtower]], and [[Mercenary Graveyard]]. Survived that story instead of dying in it, and chose peace after. Living witness to the mercenary company's fate — can react to named items looted from the Graveyard/Watchtower if shown to him.

### Trade
- Accepts **coin or barter**, whichever the player has
- Stock: common goods — Bread, Sausage, Rope, Coal, occasional Journal Note / Map Fragment

### Side-Quests
- Offers optional quests — drip-feeds lore about the mercenary company, may send player to specific locations he can't/won't visit himself

### Combat
- Peaceful by default — **retaliates if attacked** (ex-knight, doesn't die quietly)

### Death Rules
- Drops **2–3 randomized shop items** — but **only if killed before reaching the 2-item purchase threshold**
- Once a player has traded past 2 items, killing him afterward yields **no loot** (closes the trade-then-kill exploit)
- Killing him applies a **reputation debuff**: temporary trade lockout, duration scales with context (unprovoked kill vs. after trading)
- Killing him **immediately voids any active side-quest** — no partial rewards, quest just ends

### Spawn
- Random rotation across Tier 1/2 locations, appears every few in-game days, limited-time window before moving on

---

## 🏴 Caravan Black Market Trader

*A trader who deals in things people don't ask questions about.*

### Background
Representative of a larger **Black Market organization** — not a lone operator. Implies a bigger world/faction system beyond Region 1.

### Trade
- **Coin only** — no barter, no exceptions
- Stock: debuff cures, upgraded weapons/tools, possibly stolen goods (quiet link to [[Poacher's Camp]] — implies he's the buyer)
- Pricing: steep — primary coin-sink in Region 1

### Presence
- **Guaranteed to appear in every region** (unlike Wanderer's random spawn chance)

### Guards (fixed roster)
- 2 Knights
- 2 Mystic-Users
- 1 Magician
- ⚠️ First introduction of magic into The Margin's world — flag for later worldbuilding decision (is magic exclusive to this Organization, or does it exist elsewhere?)

### Death Rules
- Defeating him **and** his full guard team drops **no loot, ever** — magically sealed/protected
- Triggers **permanent Loss Reputation**:
  - Locked out of Black Market trading forever (implied: across all regions, same org)
  - Bounty triggered — 1–2 organization hunter NPCs spawn on every run, indefinitely
  - No way to lift this — permanent, point-of-no-return choice

### Side-Quest
- Completing his quest rewards a **one-time VIP Ticket**
- Grants teleport access to a hidden shop with **exclusive rare items** not found in regular stock

---

## Comparison Table

| | Wanderer | Black Market Trader |
|---|---|---|
| Payment | Coin or barter | Coin only |
| Presence | Random spawn chance | Guaranteed, every region |
| Combat | Solo, retaliates | Guarded (5-unit team) |
| Death loot | Yes, if untraded | None, ever |
| Reputation penalty | Temporary | Permanent |
| Motive to kill | Loot gamble (early only) | None — pure aggression |
| Quests | Yes | Yes (VIP Ticket reward) |

## Role 3 — Neutral / World-Texture

Non-hostile by default; they make the forest feel *inhabited*. Interact via [[Dialog System|dialogue]]; some trade small, some give quests, some become companions. Provoke them and disposition drops.

- **Refugees** — displaced Herois locals and western-kingdom refugees fleeing the war; huddled camps, small barters, rumors, and quests. The largest **companion pool.**
- **Deserters & survivors** — Klein's own scattered Novelborne comrades; broken men, some allies, some turned feral or hostile.
- **Hermits** — long-time forest dwellers who know the land: foraging tips, safe routes, weather reads.
- **Heretics** — underground old-faith faithful (Ashan / Bali / Bruna). Wary and hidden, but hold the **deepest lore** and the only thread toward Mystic / Sense (the "Ant" What-if).

## Role 4 — Quest-giver

Any NPC can carry a quest, plus quests with no living giver:

- **NPC-given** — offered through [[Dialog System|dialogue]]; killing the giver **voids** the quest (see the Wanderer's death rules).
- **Discovery-triggered** — a journal or item auto-starts a quest, no NPC required (environmental storytelling).
- **Main-story / act-gating** — the quests that trigger Act 1→2→3 (the escalation gate; see [[Lore System - Storyline Roadmap]]). Delivered by key story NPCs or discoveries.

## Role 5 — Companion *(→ [[Companion System]])*

A **specialized NPC**: a neutral/ally who **joins Klein, travels with him, adds a capability he lacks, and carries a thread of the world's lore.** Candidates come from the Neutral pool (a refugee, a deserter, a Heretic). This doc defines the shared NPC foundation companions inherit; the deep-dive — how they join, bond, help, and can be lost — lives in the [[Companion System]].

## Shared NPC Mechanics

- **Disposition ladder:** Hostile → Wary → Neutral → Friendly → Ally. Set by faction + reputation + Klein's actions.
- **Reputation:** per-NPC and per-faction; helping or harming shifts it. The Wanderer's temporary lockout and the Black Market's *permanent* Loss Reputation are the two templates (soft vs. point-of-no-return).
- **Detection & conflict:** hostiles run on the detection / noise system — Klein can **hide, flee, or fight** (fighting is costly and loud).
- **Placement & spawn:** NPCs are seeded by **zone and role** — hostiles thicken toward the east / interior; refugees cluster in camps; traders roam; predators own the night. Day/night shifts behavior (see [[Day & Night Cycle System]]).
- **VOICE:** Klein's persuasion stat ([[Status System]]) gates social options — talk down a wary patrol, win better prices, calm an animal, unlock deeper [[Dialog System|dialogue]] and lore.

## Open Items
- [ ] Decide if magic (Mystic-Users, Magician) exists anywhere else in The Margin, or is Organization-exclusive
- [ ] Spec exact price list / stock for both traders
- [ ] Spec VIP-exclusive item list
- [ ] Reconcile the traders to Klein's era (~410): the Wanderer's ex-company backstory and "present in all regions" were written for the old design — in Klein's era the merc company is *active* and the game is Herois-only
- [ ] Spec the **Gilimans** as an enemy faction: patrol behavior, occupation-escalation curve, detection/combat stats
- [ ] Spec mercenary enemy types (tie to the Kitchen Camp / Watchtower / Graveyard company)
- [ ] Define refugee / deserter / hermit / Heretic NPC archetypes (which give info, trades, quests, or become companions)
- [ ] Numeric reputation + disposition model (thresholds, decay, faction-wide vs per-NPC)

## Changelog
- Locked dual-identity question: Wanderer and Black Market Trader are **two separate NPCs**, not one character
- Added anti-exploit rule: Wanderer's loot-drop tied to purchase threshold
- Black Market Trader upgraded from "solo, tougher" to full 5-unit guard team with permanent reputation consequence
- **Expanded from "NPCs — Traders" to the full NPC System:** five roles (Hostile, Trader, Neutral, Quest-giver, Companion) grounded in the war's factions
- Added the Hostile role (Gilimans, mercenaries, Sense-users, predators), Neutral role (refugees, deserters, hermits, Heretics), Quest-giver and Companion roles, plus shared disposition / reputation / placement / VOICE mechanics
- Re-anchored the region to [[Herois Region]]; flagged era-reconciliation of the two traders as open
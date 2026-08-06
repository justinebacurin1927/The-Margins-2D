---
tags: [the-margin, game-design, systems, day-night]
status: draft
---

# Day/Night Cycle System

Related: [[World Structure System]] · [[Combat System]] · [[Status System]] · [[Inventory System]]

## Overview
Turn-based day/night cycle (matches Hunger/Thirst/Debuff/Combat turn structure). Night is meaningfully more dangerous by default, requires a light source to navigate safely, and several locations gain unique night-only threats or behavior shifts.

---

## Cycle Structure

- **Day:** 100 turns
- **Night:** 70 turns
- Full cycle: 170 turns

---

## Light Source Requirement

Without a light source at night:
- Fog of War radius shrinks drastically
- Increased enemy encounter/aggression rate
- Possible disorientation-type debuff while effectively blind (TBD — may reuse Paranoia thematically, or introduce a new debuff)

With a light source:
- Restores a usable (reduced vs. Day) Fog of War radius
- Reduces night-danger multiplier back toward baseline (not fully to Day-level safety)

### Light Source Items
| Item | Type | Notes |
|---|---|---|
| Torch | Craftable (Wood + Coal/fuel) | **60-turn** burn duration — roughly covers most of a 70-turn Night |
| Campfire | Stationary | Doesn't move with player; provides safe lit radius; doubles as cooking/purifying station |
| Lantern | Later-tier | Longer burn time, possibly refillable rather than fully consumable — crafted/Black Market upgrade |

---

## Location Day/Night Behavior

| Location | Day | Night |
|---|---|---|
| [[World Structure System#10-mercenary-graveyard\|Mercenary Graveyard]] | Undead dormant/absent — safer looting | Undead active and aggressive |
| [[World Structure System#4-beehive-grove\|Beehive Grove]] | Bees active, aggression scales with harvest | Bees dormant — safer harvest, but low visibility risk without a torch |
| [[World Structure System#8-sunken-well\|Sunken Well]] | Creature passive/hidden | Creature becomes active — higher encounter chance drawing water |
| [[World Structure System#7-poachers-camp\|Poacher's Camp]] | Traps + 3-4 enemy NPCs, standard | More aggressive patrols / increased enemy presence |
| [[World Structure System#11-deep-cave-mouth\|Deep Cave Mouth]] | Guardian dormant or lower aggression | Guardian fully active |
| [[World Structure System#1-hunters-blind\|Hunter's Blind]] | Safe, standard Tier 1 location | Wolves appear — turns a previously safe location into a real nighttime threat |
| [[World Structure System#6-collapsed-watchtower\|Collapsed Watchtower]] | Structural collapse hazard only | Wolves appear, stacking with existing collapse risk |

**Design pattern:** Beehive Grove is the sole location that flips to *safer* at night (harvesting-wise) — every other listed location becomes more dangerous, reinforcing Night as the default higher-risk state even for previously "easy" Tier 1 spots like Hunter's Blind.

---

## Open Items
- [ ] Define the exact disorientation/blindness debuff for lightless night exploration
- [ ] Confirm NPC availability at night — do the Wanderer/Black Market Trader still spawn/trade after dark?
- [ ] Decide Lantern's exact stats (burn time, refill mechanic)
- [ ] Expand day/night behavior to remaining locations not yet covered (Fallen Log Hollow, Forest Shrine, Kitchen Camp, Old House)
- [ ] Define wolf encounter specifics (pack size, danger tier) for Hunter's Blind / Watchtower

## Changelog
- Locked cycle: Day 100 turns / Night 70 turns
- Locked Torch burn duration at 60 turns
- Confirmed Mercenary Graveyard's undead as night-exclusive threat
- Added day/night behavior for Beehive Grove, Sunken Well, Poacher's Camp, Deep Cave Mouth
- Added wolf encounters to Hunter's Blind and Collapsed Watchtower (night-only)
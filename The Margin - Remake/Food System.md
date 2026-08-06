---
tags: [the-margin, game-design, systems, food]
status: draft
---

# Food System

Region: [[Region 1 - Forest]]
Related: [[Hunger System]] · [[Debuff System]] · [[World Structure System]]

## Overview
Food restores Hunger turns and sometimes HP, but carries risk depending on source, freshness, and raw vs. cooked state. Coal (found at [[Worn Down Kitchen Camp]]) is required to cook meat safely.

---

## 🏕️ Worn Down Kitchen Camp — Base Loot

- Bread
- Sausage
- Ale
- Molded Cheese
- Half Rotten Meat
- Coal (needed for cooking)
- Spoon / Fork

| Food | Hunger Restored | HP | Risk |
|---|---|---|---|
| Bread | +80 turns | — | None (reliable staple) |
| Sausage | +120 turns | +2 HP | None |
| Ale | +30 turns | — | Slight Agility debuff (20 turns, "tipsy") — removes stress/fear effects if present. Also the trigger item for [[Debuff System#8-alcohol-interaction-toxin|Alcohol-interaction toxin]] |
| Molded Cheese | +60 turns | -1 HP | 25% chance of Sick |
| Half Rotten Meat | +150 turns (raw) | -3 HP | 40%+ chance of Poison — see Raw vs Cooked table below |

---

## 🥩 Raw vs. Cooked (Small Animals)

> Cooking requires **Coal**. Raw meat is an emergency-only option — high risk, lower restore.

| Meat | Raw | Cooked (with Coal) |
|---|---|---|
| Rabbit | +60 turns, -2 HP, 35% Sick risk | +100 turns, +3 HP, no risk |
| Chicken | +70 turns, -2 HP, 40% Sick risk | +130 turns, +4 HP, no risk |
| Fish (River Area) | +50 turns, -1 HP, 30% Sick risk | +90 turns, +2 HP, no risk |
| Half Rotten Meat | +90 turns, -5 HP, 60% Poison risk | +150 turns, -3 HP, 40% Poison risk (cooking reduces but does not remove risk — it's already spoiled) |

Sickness/Poison risk here feeds into the [[Debuff System]] — see Nausea → Fever → Delirium escalation chain.

---

## 🍄 Mushrooms

| Mushroom | Type | Effect |
|---|---|---|
| Edible Mushroom | Safe | +40 turns, no risk |
| Spotted Mushroom | Mildly Toxic | +50 turns, 30% Sick risk |
| Pale Cap | Toxic (deceptive — looks safe) | +20 turns, 70% Poison risk |
| Bloodvein Mushroom | Deadly | -5 HP, 90% Poison risk — but cures Bloated instantly if survived |

- ⚠️ Open design idea: Survival/Herbalism skill check to correctly identify mushrooms before eating (not yet confirmed as a system)

---

## 🍓 Nature Structure — Fruits & Berries

- Apricot, Apple, Cherry: +25–35 turns, +1 HP each
- **Blueberries:** gathered in handfuls (5–8 berries), consumed as one stack — +20 turns per handful
- **Strawberries:** picked/eaten individually — +15 turns each
- Edible Mushroom: +40 turns (see mushroom table above)

## 🍯 Honey / Beehives

| Item | Hunger Restored | HP | Notes |
|---|---|---|---|
| Honey | +50 turns | +2 HP | Cures Sick/Poisoned status |
| Honey Comb | +70 turns | +3 HP | Same cure property; riskier to obtain (bee aggression — see [[World Structure System#4-beehive-grove|Beehive Grove]]) |

---

## 🏚️ Old House — Civilian Loot (distinct tone from Kitchen Camp)

- Preserved/canned food — rare, high hunger-restore (sealed from decay)
- Cloth/bandages (early healing item)
- Kitchen utensils: worn knife, frying pan, coal, ale, pouch
- ⚠️ Overlap flag: utensils here duplicate Kitchen Camp's identity — decide if intentional or needs differentiation

---

## Open Items
- [ ] Confirm exact HP/turn values for Apricot, Apple, Cherry individually (currently a shared range)
- [ ] Decide if a Survival/Herbalism check gates mushroom identification
- [ ] Resolve Old House vs Kitchen Camp utensil overlap
- [ ] Define what "treated" means mechanically for early risk foods (does eating a cure item mid-Nausea stop escalation immediately, or just shorten duration?)

## Changelog
- Added raw/cooked distinction for all meats, tied to Coal as a resource
- Expanded mushroom table from 1 safe entry to a 4-tier risk table per user request
- Locked berry handling: Blueberries = per-handful, Strawberries = per-piece
- Confirmed Honey/Honeycomb as cure items for Sick/Poisoned
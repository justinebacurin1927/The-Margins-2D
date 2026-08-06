---
tags: [the-margin, game-design, systems, thirst]
status: draft
---

# Thirst System

Region: [[Region 1 - Forest]]
Related: [[Hunger System]] · [[Debuff System]] · [[World Structure System]] · [[Status System]]

## Overview
Thirst runs as an independent track from Hunger — no direct interaction between the two. Same 4-tier structure as Hunger, but shorter durations overall (thirst kills faster than hunger, matching real survival logic). Parched-stage debuffs persist until the player drinks, rather than clearing on a timer.

---

## Status Tiers

### 🟢 Hydrated — 200 turns
- Starting status
- No buff, or optional minor stamina regen bonus (TBD)

### 🟡 Thirsty — 150 turns
- Warning stage, no effects

### 🟠 Dehydrated — 100 turns
- Triggers **Headache**: -10% Strength/Agility/Grit/Stamina
- Duration: 30 turns
- 35% chance to reapply per 10-turn interval while still in Dehydrated status

### 🔴 Parched — 80 turns, three stages
> Debuffs in this tier persist **until the player drinks** — turns alone will not clear them.

**Stage 1 — Withered**
- -25% Strength/Agility/Grit/Stamina
- Persists until hydrated

**Stage 2 — Trembling** *(shared debuff with [[Hunger System]] Starving Stage 2)*
- -15% Agility
- Persists until hydrated
- Intentionally reused: same physical shake response regardless of cause (food or water deprivation)

**Stage 3 — Dried Out**
- Paranoia debuff
- -2 HP per 5 turns
- Persists until hydrated

---

## Water Sources

### 1. Sunken Well
- Most stable source of drinkable water
- Requires Waterskin or Canteen to collect
- Relatively clean — likely only needs boiling, not filtering

### 2. Pond
- Stagnant, no fish/life — real danger source
- Requires **both** filtering AND boiling (real-world "two is one, one is none" principle) before safe to drink
- Drinking untreated: severe debuff risk (TBD — likely Sick/Poisoned tier)

### 3. River Area
- Rare world structure for Region 1 (forest-set)
- Can drink directly or collect via Waterskin/Canteen
- 20% poison chance if untreated

### 4. Rain / Weather System
- Future source, tied to upcoming Weather System

---

## Purification System

Mirrors the Raw vs. Cooked meat system — reuses existing resources (SKILL stat, Coal) rather than introducing new ones.

**Step 1 — Filtration** (SKILL-based)
- Removes sediment/debris using cloth, sand, or charcoal-type filter
- Reduces poison/sick risk significantly, but not to zero
- Required for Pond water specifically

**Step 2 — Boiling** (Coal + fire, same as cooking)
- Kills remaining pathogens
- Brings risk to 0%
- Required for all raw water sources before becoming "Purified"

Sunken Well / River (cleaner sources) may only need boiling, skipping the filter step.

---

## Drinkable Items

| Item | Type | Notes |
|---|---|---|
| Waterskin (Raw Water) | Raw | Moderate restore, poison/sick risk |
| Waterskin (Purified Water) | Purified | Full restore, no risk |
| Canteen (Raw Water) | Raw | Higher capacity than Waterskin, same risk |
| Canteen (Purified Water) | Purified | Higher capacity, no risk |
| Unknown Water Jar | Gamble | See outcome table below |
| Berry Juice (Blueberry/Strawberry) | Crafted | Small restore; needs many berries per jar (low liquid yield per berry) |
| Ale / Beer | Drinkable, debuff-bearing | Partial Thirst restore; causes Agility "tipsy" debuff; triggers Alcohol-interaction toxin if mixed with toxic mushrooms |

### Unknown Water Jar — Outcome Table
| Roll | Chance | Effect |
|---|---|---|
| Jackpot | 10% | Full restore + minor Stamina regen buff |
| Good | 30% | Solid restore (Purified Water tier) |
| Dud | 30% | Minimal restore (Raw Water tier) |
| Bad | 20% | Triggers Headache |
| Nasty | 10% | Triggers Sick or Poisoned |

---

## Open Items
- [ ] Confirm exact capacity difference between Waterskin and Canteen (e.g., 2x?)
- [ ] Decide Pond's untreated-drinking debuff severity
- [ ] Decide if Hydrated gets an optional buff, or stays neutral like Satisfied in Hunger System
- [ ] Confirm berry-to-juice ratio for crafting

## Changelog
- Built using real-world water purification research: two-step filtration + boiling process, mirrored onto SKILL stat and Coal resource
- Confirmed Thirst and Hunger run as fully independent tracks
- Confirmed Trembling (Stage 2) is intentionally the same shared debuff as Hunger System's Starving Stage 2
- Named Stage 1 placeholder: **Withered**
- Locked Unknown Water Jar as a random gamble item with a 5-tier outcome table
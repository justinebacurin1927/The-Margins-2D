---
tags: [the-margin, game-design, systems, weather]
status: draft
---

# Weather System

Related: [[Day/Night Cycle System]] · [[World Structure System]] · [[Food System]] · [[Combat System]]

## Overview
Randomized weather, rolled per cycle with weighted rarity. Stacks with (does not replace) the Day/Night Cycle — e.g., a Storm at Night compounds both systems' penalties for a genuine worst-case scenario. Each weather type carries both a pro and a con, so weather creates tactical opportunity as much as hazard.

---

## Weather Types

### ☀️ Clear — 40%
- **Pro:** Full visibility, no penalties — safest baseline state
- **Con:** None (neutral)

### 🌧️ Rain — 25%
- **Pro:** Beehive Grove bees less aggressive (grounded by rain) — good harvesting window
- **Con:** Mild visibility reduction, slippery terrain (Crippled/stumble risk), torch burn-rate penalty
- **Note:** Rain does NOT provide free/purified water — [[Thirst System]] purification still required

### 🌫️ Fog — 20%
- **Pro:** Enemies also suffer reduced visibility — easier to sneak past hostile NPCs (Poacher's Camp, guards)
- **Con:** Severe Fog of War reduction (even during Day), increased surprise-encounter chance for player too

### ⛈️ Storm — 10%
- **Pro:** Wildlife/roaming creatures seek shelter — reduced open-area creature encounters (wolves, animals) while traveling
- **Con:** Falling branches, higher structural collapse chance — worst weather for indoor/structure locations ([[World Structure System#6-collapsed-watchtower|Watchtower]], [[World Structure System#9-the-old-house|Old House]])

### ❄️ Cold Snap — 5%
- **Pro:** Slows [[Inventory System#spoilage-system|Spoilage]] — cold naturally preserves food longer
- **Con:** Placeholder — flags future Thirst/Stamina drain once a full Temperature/Exposure system is built
- **Note:** First mechanical seed of a future Temperature/Exposure system

---

## Interaction with Day/Night

Weather stacks independently on top of Day/Night rather than replacing it:
- Storm + Night = worst-case scenario (compounded visibility loss, compounded hazard risk)
- Clear + Day = safest baseline state
- Fog + Night = near-blind without a strong light source

---

## Open Items
- [ ] Lock exact numeric penalties (visibility %, collapse chance increase, torch burn-rate reduction)
- [ ] Build full Temperature/Exposure system to give Cold Snap's con a real mechanical effect
- [ ] Decide if Weather affects NPC spawn behavior (Wanderer/Black Market Trader) — e.g., does the Wanderer avoid traveling during Storms?
- [ ] Decide if Weather can be predicted/forecasted by the player (e.g., via a Survival-type skill check) or is always a surprise

## Changelog
- Locked 5 weather types with weighted randomization (Clear 40%, Rain 25%, Fog 20%, Storm 10%, Cold Snap 5%)
- Added pro/con balance to every weather type, avoiding pure-hazard design
- Confirmed rain does not bypass the Purification system
- Cold Snap's spoilage-slowing pro locked in as the seed for a future Temperature/Exposure system
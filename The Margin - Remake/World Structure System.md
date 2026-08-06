---
tags: [the-margin, game-design, systems, world-structure]
status: draft
---

# World Structure System

Region: [[Region 1 - Forest]]
Related: [[Hunger System]] · [[Debuff System]] · [[Food System]] · [[NPCs]]

## Overview
Generic hazard/danger framework applied across all world locations. Design rule: **better loot = higher structural/creature risk.** Locations rated on a simple 3-tier danger scale.

---

## Hazard Categories

- **Environmental** — traps, structural decay (collapsing floors/scaffolding), weather exposure
- **Creature** — passive-aggressive (bees), territorial (predators near hunting grounds), ambush (hidden threats)
- **Resource-Risk** — spoiled/contaminated loot mixed into legitimate loot tables (player must identify safe vs. risky before consuming)

---

## 🟢 Tier 1 — Low Risk

### 1. Hunter's Blind
*A hidden wooden platform once used to track game.*
- **Loot:** Rope, small tools, 20% chance Map Fragment, Journal Note (lore)
- **Hazard:** Weak floor plank — small chance to fall through (minor HP hit, no debuff)
- **Lore:** Tracking notes hint at something bigger once hunted in this forest

### 2. Fallen Log Hollow
*A massive hollowed-out tree trunk, safe from weather.*
- **Loot:** Mushroom cluster (random), occasional Berries and mosses
- **Hazard:** Snake/insect encounter — Venom debuff (unspec'd)
- **Note:** Calm contrast beat after scarier areas

### 3. Forest Shrine
*A moss-covered stone shrine with faded carvings.*
- **Loot:** Bread, Cheese, Journal Note
- **Hazard:** None
- **Note:** Reserved as a hook for a future blessing/buff system

### 4. Beehive Grove
*A cluster of trees thick with hives.*
- **Loot:** 2–5x Honey/Honeycomb yield vs. a single hive
- **Hazard:** Bee aggression scales with harvest amount — Swollen + Venom debuff

---

## 🟡 Tier 2 — Medium Risk

### 5. Worn Down Kitchen Camp
*An abandoned camp left behind by knights or mercenaries.*
- **Loot:** Molded Cheese, Ale, Bread, Half Rotten Meat, Sausage, Coal, Spoon, Fork
- **Hazard:** None specified — spoiled-food risk is baked into consumption, not exploration

### 6. Collapsed Watchtower
*A half-fallen lookout post from the same lost company.*
- **Loot:** Early-tier weapons (rusted knife, spear)
- **Hazard:** Upper floor collapse — fall damage, chance of Crippled + Bleeding
- **Lore:** Ties to Kitchen Camp faction; possible journal page/insignia

### 7. Poacher's Camp
*A hidden hunting camp, still active.*
- **Loot:** Extra small game spawns (Rabbit/Chicken), stolen goods
- **Hazard:** Player-placed trap (bear trap/snare), 3–4 Enemy NPCs
- **Lore:** Someone's still active in this forest — seeds future rival scavenger/NPC content

### 8. Sunken Well
*An old stone well with fresh water below.*
- **Loot:** Water source (future [[Thirst System]] payoff), rare coin/items from people who fell in
- **Hazard:** Slip-and-fall risk, possible creature living below
- **Note:** Good space for an optional side-encounter

---

## 🔴 Tier 3 — High Risk

### 9. The Old House
*A crumbling house deep in the forest; unclear who lived there.*
- **Loot:** Preserved/canned food, cloth/bandages, locked chest/cellar (needs key or tool), kitchen utensils (worn knife, frying pan, coal, ale, pouch), personal Journal Notes
- **Hazard:** Structural decay, locked cellar door (needs force or light source)
- **Lore:** Second, unrelated thread — a civilian family, possibly connected to what drove the mercenaries into the forest
- ⚠️ Design flag: utensil overlap with Kitchen Camp — decide if intentional (family also cooked) or needs differentiation

### 10. Mercenary Graveyard
*Where the lost company was finally buried.*
- **Loot:** Rare gear, coin, Ale/beer, named unique items tied to specific fallen mercs
- **Hazard:** Undead (curse debuff, name TBD) or wildlife (wolves, coyotes)
- **Lore:** Payoff to Kitchen Camp + Watchtower thread — named grave markers close the loop

### 11. Deep Cave Mouth
*A dark opening into something bigger than this forest.*
- **Loot:** Single degraded weapon/tool from a failed previous explorer
- **Hazard:** Guardian mini-boss (bear or wandering knight)
- **Lore:** Teaser for Region 2 / future dungeon — not a full location yet

---

## Lore Threads
1. **Mercenary company arc:** Watchtower (last stand) → Kitchen Camp (where they lived) → Graveyard (where they died) — same faction, full narrative loop
2. **Old House family arc:** Separate, unrelated civilian thread — possibly connected to what drove the mercenaries into the forest in the first place

## Open Items
- [ ] Name the undead curse debuff (Mercenary Graveyard)
- [ ] Spec Venom debuff fully
- [ ] Decide Old House vs Kitchen Camp utensil overlap
- [ ] Draft found-journal/note text tying Watchtower → Kitchen Camp → Graveyard together as environmental storytelling

## Changelog
- Fixed Forest Shrine numbering (was nested inside Fallen Log Hollow entry)
- Restored Kitchen Camp's original loot table after reorganization
- Filled in Old House (was empty placeholder) and trimmed Deep Cave Mouth's loot to avoid repeating Nature Structure items
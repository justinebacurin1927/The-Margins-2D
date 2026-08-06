---
tags: [the-margin, game-design, systems, weapons, gear]
status: draft
---

# Weapon System + Gear with Memory

Related: [[Status System]] · [[World Structure System]] · [[NPCs]] · [[Currency System]]

## Overview
Weapons are split into 5 categories (Close Range, Range, Melee, Utility, Defense) across 5 tiers (T1–T5). All weapons use a Minecraft-style per-use Durability system, with a unique "Gear with Memory" twist: repairing restores Durability, but each repair lowers the weapon's *max* Durability ceiling — gear wears out permanently over its lifetime rather than being maintainable forever.

---

## Durability System

- **Loss:** Per-use (Minecraft-style) — every attack, block, chop, or throw costs a fixed amount of Durability
- **Break state:** At 0 Durability, weapon becomes unusable
- **Repair:** SKILL-based action, restores Durability using **weapon-specific materials**
- **The Memory twist:** Max Durability shrinks with each repair (see decay curve below) — a weapon can only be mended so many times before it's "beyond repair"
- **Scavenging broken weapons:** Broken gear can be broken down into partial materials rather than lost entirely

### Repair Decay Curve (base, SKILL-modified)
| Repair # | Low SKILL | Mid SKILL | High SKILL |
|---|---|---|---|
| Fresh | 100% | 100% | 100% |
| 1st | 90% | 93% | 96% |
| 2nd | 78% | 84% | 91% |
| 3rd | 65% | 74% | 85% |
| 4th | 50% | 63% | 78% |
| 5th | 35% | 51% | 70% |
| 6th+ | Beyond repair | Beyond repair (marginal) | Beyond repair (marginal) |

### Scavenge Yield (on weapon break)
| Weapon Tier/Type | Example Yield |
|---|---|
| Common (T1–T2) | 1–2 base material |
| Mid (T2–T3) | 2 base material |
| Black Market/Rare (T3+) | 2–3 base material, possibly rare material |
| VIP/Legendary (T5) | 3–4 base material + unique material |

---

## Repair Materials by Category

| Category | Repair Material |
|---|---|
| Close Range (spears/lances) | Wood + Rope |
| Range (bows/thrown) | Wood + String/Sinew |
| Melee (swords/blades) | Metal Scrap |
| Utility (axes/tools) | Wood + Metal Scrap |
| Defense (shields) | Wood + Metal Scrap |

Ammo-style items (Arrows, Metal Pins) are consumable, not durability-based — used up per shot/throw rather than repaired.

---

## Full Weapon Roster (30 total)

### ⚔️ Close Range — Spear/Lance-type (5)
1. Sharpened Branch (Stake) — T1
2. Rusted Spear — T1
3. Boar Spear — T2
4. Knight's Lance — T3
5. Mercenary Captain's Spear — T4 *(named/lore item, likely Mercenary Graveyard loot)*

### 🏹 Range — Bows & thrown (7)
6. Sling — T1
7. Metal Pins — T2 *(throwable metal spikes, consumable-style)*
8. Hunter's Shortbow — T2
9. Recurve Bow — T3
10. Marksman's Long Bow — T3
11. Cross Bow — T3
12. Wind Tempest — T5 *(legendary — mystic ability, overpowered durability)*

### 🗡️ Melee — Swords/blades (6)
13. Wooden Club — T1 *(default starting weapon — no scavenging/crafting required)*
14. Rusted Shortsword — T1
15. Worn-down Dirk — T1
16. Broken Knight's Sword — T2
17. Blackmarket's Long Sword — T3
18. Tomahawk — T4 *(tri-purpose: Utility/Melee/Throwable — rare because it replaces 3 weapon roles in one item)*

### 🪓 Utility — Tools (8, no T1 by design — dual-purpose category)
19. Handaxe — T2
20. Camp Hatchet — T2
21. Poacher's Hatchet — T2
22. Woodman's Axe — T2
23. Mercenary's Dagger — T2
24. Rusted Sickle — T2
25. Pickaxe — T3
26. Rusted Knife — T1 *(exception — base utility knife, lowest tier item in category)*

### 🛡️ Defense — Shields (4, no T2/T4 currently)
27. Cracked Wooden Buckler — T1
28. Knight's Buckler — T3
29. Reinforced Buckler — T3
30. Aegis Ward — T5 *(legendary — mystic-imbued, exceptional durability, pairs narratively with Wind Tempest)*

---

## Design Notes
- **Category rule:** Range and Utility intentionally skip T1 (mostly) since both are dual-purpose categories requiring more complexity than a "crude starter" item — Rusted Knife is the sole exception, serving as Utility's baseline tool
- **Wind Tempest & Aegis Ward** are the game's first confirmed **mystic/legendary-tier** items — ties into the Black Market Trader's Mystic-User/Magician guards as the likely in-world source of magic
- **Tomahawk's tri-purpose nature** means its Durability model may need a different draw rate depending on action type (chop/hit/throw) — open design question
- **Mercenary Captain's Spear** implies a named leader figure for the fallen mercenary company — potential lore expansion for the Watchtower/Kitchen Camp/Graveyard thread

## Open Items
- [ ] Assign exact Durability values + Durability-loss-per-use for each of the 30 weapons
- [ ] Decide Tomahawk's durability draw rate across its 3 use types
- [ ] Decide Arrow/Metal Pins crafting or purchase source
- [ ] Expand magic system lore — is it Black Market-exclusive, or does it exist elsewhere in The Margin's world?
- [ ] Fill remaining tier gaps if desired (T4 Range/Utility/Defense, T2/T4 Defense)

## Changelog
- Locked 30-weapon roster across 5 categories, 5 tiers
- Confirmed category restructure: Close Range = spear/reach weapons, Range = bows/thrown, Melee = swords/blades (distinct from original draft's Ranged/Ranged++ split)
- Added Wind Tempest (T5 legendary Range) and Aegis Ward (T5 legendary Defense) as the game's first magic-tied items
- Added Wooden Club (T1 Melee) as the confirmed default starting weapon
- Confirmed Rusted Knife as Utility's sole T1 exception to the no-T1 rule
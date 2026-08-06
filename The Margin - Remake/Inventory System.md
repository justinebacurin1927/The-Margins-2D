---
tags: [the-margin, game-design, systems, inventory]
status: draft
---

# Inventory System

Related: [[Status System]] · [[Food System]] · [[Debuff System]] · [[Weapon System]] · [[Currency System]] · [[World Structure System]]

## Overview
Hybrid slot + weight system. Fixed Quick-Access equipment slots sit separate from a 19-slot base main inventory, which can be expanded by equipping up to 5 storage items simultaneously. Perishable items spoil over time unless processed or specially stored.

---

## Equipment Structure

### Quick-Access Slots (separate from main inventory, always available)
- **5x** Weapon/Armor-type slots — Weapon (single active weapon only), Defense, Utility, + 2 open categories (TBD)
- **3x** Artifact/Ring-type slots — for legendary/trinket items (e.g., Wind Tempest, Aegis Ward)

### Main Inventory
- **19 base slots**, fixed starting point
- Expandable by equipping up to **5 storage items** simultaneously — all bonuses merge into this single pool (no separate sub-pouches)
- Weapon can only be swapped by accessing inventory (no instant mid-combat swap)
- Journal Notes / lore items take normal inventory space — no exemption

---

## Weight Capacity

STR directly scales base carry capacity (Low/Mid/High tiers — exact values TBD).

---

## Spoilage System

**Ladder:** Fresh → Half Rotten Meat (existing item) → Fully Spoiled (eatable last resort, brutal risk, not blocked)

- Cooked meat and Purified Water resist/avoid spoilage entirely
- Specific storage items reduce spoil rate (Poacher's Game Bag, Traveler's Pack, Expedition Pack)
- Vien's Dimensional Pocket bypasses spoilage completely (100% reduction)

---

## Bag Durability

- Bags have their own Durability, separate from weapon Durability
- **Trigger:** Only specific thematic trap types damage bags — e.g., dart traps (general bags), fire traps (Scroll Holder contents), freeze traps (Potion Bandolier contents)
- Normal combat hits do **not** damage bag Durability, only hazard/trap types do
- **At 0 Durability:** Bag breaks — **75% of contents drop** (recoverable on-site), **25% lost entirely**. The bag itself is destroyed.
- Potion Bandolier / Scroll Holder lose **contents directly** per relevant trap hit (-1 vial/scroll), rather than losing bag structure Durability the same way

---

## Storage Item Roster

| # | Item | Tier | Source | Slots | Durability | Weight Reduction | Special |
|---|---|---|---|---|---|---|---|
| 1 | Woven Pouch | T1 | Craftable (Rope/Cloth) | +10 | 35 | +5 | Dart-vulnerable |
| 2 | Hunter's Satchel | T1 | Hunter's Blind / craftable | +15 | 70 | +7 | Dart-vulnerable |
| 3 | Mercenary's Rucksack | T2 | Watchtower / Kitchen Camp | +25 | 150 | +12 | Dart-vulnerable |
| 4 | Poacher's Game Bag | T2 | Poacher's Camp | +20 (food only) | 100 | +10 | -25% spoil, dart-vulnerable |
| 5 | Family's Trunk | T2 | Old House (reworked) | +15 | 75 | +10 | Dart-vulnerable |
| 6 | Traveler's Pack | T3 | Wanderer (coin/barter) | +50, +3 (bandolier), +3 (scroll) | 200 | +20 | -15% spoil, dart-vulnerable |
| 7 | Reinforced Backpack | T3 | Black Market Trader | +50 | 200 | +30 | Reduces Gear/Weapon/Tool weight, dart-vulnerable |
| 8 | Expedition Pack | T3 | VIP-exclusive | +50 | 200 | +40 | -20% spoil, dart-vulnerable |
| 9 | Potion Bandolier | T3 | TBD | +15 (liquids only) | Per-vial | — | Freeze-vulnerable (-1 vial/hit) |
| 10 | Scroll Holder | T3 | TBD | +20 (scrolls only) | Per-scroll | — | Fire-vulnerable (-1 scroll/hit) |
| 11 | Vien's Dimensional Pocket | T5 | Legendary | +75 | Invincible | Infinite | 100% spoil reduction |
| 12 | Faahard's Oblivion Blade | T5 | Legendary (weapon-inventory) | +60 | 200 | Infinite | Mending +20 per stored loot |

**Non-legendary slot cap:** +50 slots is the intentional ceiling for T1–T3 items — only T5 legendaries (Dimensional Pocket, Oblivion Blade) exceed it.

---

## Open Items
- [ ] Fill remaining 2 Quick-Access equipment slot categories
- [ ] Confirm STR weight tiers (Low/Mid/High exact values)
- [ ] Confirm spoilage turn-count thresholds (Fresh → Half Rotten → Fully Spoiled)
- [ ] Confirm Faahard's Oblivion Blade: does stored loot remain retrievable, or is it consumed entirely for the Mending bonus?
- [ ] Source TBD for Potion Bandolier and Scroll Holder (currently unassigned — craftable? trader stock?)
- [ ] Decide which existing hazards (Watchtower, Poacher's Camp, etc.) count as "dart-type" trap sources for bag damage

## Changelog
- Corrected Mercenary's Rucksack from +50 to +25 slots to preserve T2 < T3 progression
- Confirmed +50 slots as the intentional non-legendary ceiling
- Locked bag-break mechanic: 75% loot drop / 25% loss on Durability reaching 0
- Confirmed bag Durability only depletes from specific thematic trap types, not general combat
---
tags: [the-margin, game-design, systems, debuffs]
status: draft
---

# Debuff System

Region: [[Region 1 - Forest]]
Related: [[Hunger System]] · [[Food System]] · [[World Structure System]]

## Overview
Debuffs primarily originate from eating raw/spoiled meat (bacterial track) or toxic mushrooms (mushroom/toxin track). Each track has its own escalation logic and cure interactions.

---

## 🥩 Raw Meat / Bacterial Track

> Escalation chain — each stage **replaces** the previous one (no stacking).

### 1. Nausea (base stage)
- Effect: **-30% Stamina, -30% Strength**, reduced natural regen
- Duration: 30 turns if untreated/not escalating
- Trigger source: eating raw meat — base chance varies by meat type
  - Rabbit: 30% · Chicken: 40% · Half Rotten Meat: 70%
- Escalation: **50% chance/turn** to escalate into Fever if untreated

### 2. Fever (replaces Nausea)
- Effect: **-40% Strength/Stamina/Agility**, instant
- Duration: 25 turns
- Escalation: **guaranteed** — escalates into Delirium at end of turn if untreated

### 3. Delirium (replaces Fever) — 3-in-1 debuff
- Duration: 40 turns
- Cure item reduces duration by **75%**
- **Paranoia** — hallucinated enemies/NPCs/traps/places (40 turns)
- **Vertigo** — random movement direction, ~12% success rate for intended direction (30 turns)
- **Crippled** — -50% movement speed only, does not affect Agility stat (20 turns)

### 4. Diarrhea (parallel track — runs alongside chain, not part of it)
- **Stage 1:** 2x Thirst/Stamina drain — 50 turns
- **Stage 2 (auto-trigger if Stage 1 untreated):** 3x Thirst/Stamina/Hunger drain — 30 turns, becomes lethal (HP drain) if ignored
- **Severe Effects:** 5–10% chance (checked once when Diarrhea or Fever first triggers) — stacks extra **-2 HP every 5 turns** on top of active debuffs until cured

---

## 🍄 Mushroom / Toxin Track

### 5. Rotgut
- Instant onset: Nausea + Crippled + Diarrhea (3-in-1)
- Duration: 40 turns

### 6. Honeymoon → Collapse (two-phase debuff)
- **Honeymoon phase:** hidden, no visible effect — player appears stable. 60-turn countdown.
- **Collapse (auto-trigger at end of Honeymoon if uncured):** permanent **Max HP cap at 40%** (e.g., 100 Max HP → capped at 40, even after recovery)
- Requires a cure item to lift the cap

### 7. Fevered Mind
- Mild hallucination only, no stat drain
- Duration: 50 turns
- Weaker cousin of Paranoia — flavor debuff from non-deadly mushroom toxins

### 8. Alcohol-interaction toxin
- Dormant/latent — causes no illness unless Ale is consumed afterward
- If never triggered: clears naturally after **100 turns**

---

## Open Items
- [ ] **Venom** debuff (bites/stings from Fallen Log Hollow, Beehive Grove) — not yet spec'd, needs duration/effect
- [ ] Undead curse effect (Mercenary Graveyard) — still unnamed placeholder
- [ ] Confirm whether Severe Effects (Diarrhea) can co-trigger with Delirium, or if they're mutually exclusive by design

## Design Notes
- Escalation asymmetry is intentional: Nausea→Fever is a rolled chance (50%/turn), but Fever→Delirium is guaranteed — Fever is meant to read as the "last warning" stage before things get unavoidable
- Real-world basis: bacterial track mirrors delayed-onset food poisoning progression; mushroom track mirrors amatoxin's real "false recovery" phase (GI symptoms fade before hepatic damage hits)

## Changelog
- Removed duplicate "E. coli-style complication" entry — folded into Diarrhea's Severe Effects
- Confirmed Nausea → Fever → Delirium as one true escalation chain (no stacking)
- Named debuff placeholders: Well Fed's slow-debuff → **Bloated**, Stage 2 Starving debuff → **Trembling**
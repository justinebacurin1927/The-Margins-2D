---
tags: [the-margin, game-design, systems, hunger]
status: draft
---

# Hunger System

Region: [[Region 1 - Forest]]
Related: [[Debuff System]] · [[Food System]] · [[Currency System]]

## Overview
Hunger is a countdown-based status system. Player starts with **50 countdown** (starving threshold reference point). Status decays through four tiers as turns pass, each with its own duration, buffs, and debuffs.

---

## Status Tiers

### 🟢 Well Fed — 350 turns
> Best-fed state, obtained by cooking and eating special dishes/foods.

- **Buff — Bloated (regen):** +1 HP every 2–3 turns
- **Debuff — Bloated (slow):** movement slow-effect, 50-turn duration
- ⚠️ Trade-off status: regen and slowness apply simultaneously (food coma design)

### 🟡 Satisfied — 250 turns
> Default starting status. Most stable tier.

- No buffs, no debuffs

### 🟠 Hungry — 250 turns
> Warning stage after Satisfied ends.

- No effects yet — precedes Starving

### 🔴 Starving — 150 turns total
> Damages HP over time: **-1 HP every 4 turns**
> Debuffs stack until player eats, cleanses, or dies.

#### Stage 1 — Fatigue (150 → 100 remaining)
- Debuff: **-35% max Strength**
- Eating bonus: none

#### Stage 2 — Trembling (100 → 50 remaining)
- Debuff: **-15% Agility**
- Eating bonus: reduces next status duration by **20%**

#### Stage 3 — Rotting (50 → 0 remaining)
- Damage doubles: **-3 HP every 2 turns**
- Eating bonus: reduces next status duration by **50%**
  - e.g., eating here → recover to Hungry status at **125 turns** instead of 250

---

## Design Notes
- [ ] Confirm whether Well Fed's dual buff/debuff (regen + slow) needs a unique name for the combined state, or stays as two separate tags
- [ ] Cross-check food items (see [[Food System]]) against exact turn-restore values per tier
- [ ] Consider whether Stage 1 should ever get an eating-incentive for consistency, or stay intentionally bonus-free

## Changelog
- Fixed Starving math: originally miscounted as 250/200/100, corrected to 150/100/50 stage breakpoints
- Renamed Well Fed's debuff placeholder → **Bloated**
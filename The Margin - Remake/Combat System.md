---
tags: [the-margin, game-design, systems, combat]
status: draft
---

# Combat System

Related: [[Status System]] · [[Weapon System]] · [[Debuff System]] · [[Inventory System]]

## Overview
Turn-based combat (matches Hunger/Thirst/Debuff systems' turn structure). Built around GRIT's signature **Last Stand** mechanic — the game's core permadeath safety valve, usable only once per entire run.

---

## Turn Structure

- Turn order likely determined by **AG** (higher Agility acts first)
- Tie-breaking rule: TBD

## Core Actions (per turn)

| Action | Description |
|---|---|
| Attack | Uses equipped Weapon, costs Weapon Durability per hit |
| Block | Uses equipped Defense item, GRIT-related damage mitigation |
| Dodge | AG-based chance to avoid damage entirely |
| Use Item | Pulls from Quick-Use hotbar (food/potion/cure) |
| Flee | Attempt to escape combat — trigger stat TBD (AG or VOICE) |

---

## Damage Formula (conceptual, values TBD)


---

## Critical Hits

- **Crit Chance:** governed by AG
- **Crit Damage:** governed by SKILL (multiplier)

Splits crit chance and crit damage across two stats so a crit-focused build meaningfully invests in both AG and SKILL, rather than one stat governing everything.

---

## Weapon-Inflicted Debuffs

- Confirmed: debuff-on-hit is **weapon-specific**, not category-wide — individual weapons will carry unique debuff chances (e.g., a specific bladed weapon might cause Bleeding) rather than every Melee weapon sharing the same effect
- Reuses existing debuffs where possible (e.g., **Bleeding**, already established via [[World Structure System#6-collapsed-watchtower|Collapsed Watchtower]]'s fall-damage hazard) rather than creating new ones per weapon
- **Assignment deferred** — full weapon-by-weapon debuff table to be planned later
- Legendary weapons (Wind Tempest, Faahard's Oblivion Blade) likely get unique effects tied to their mystic nature — separate future pass

---

## Last Stand (GRIT)

> The game's core permadeath safety valve — central to the "Permadeath / Last Stand" identity.

- **Trigger:** Automatically checked when HP would hit 0
- **Chance:** GRIT-based % — higher GRIT = more reliable trigger, but never guaranteed
- **Effect:** Survive at 1 HP. **No bonus, no buff** — fully vulnerable immediately after
- **Usage limit:** **Once per entire gameplay run.** Once triggered (successfully or not — TBD whether a failed roll also consumes the use), it can never trigger again for the rest of that run
- Design intent: this is not a per-fight safety net. It's a single emergency reserve across an entire playthrough — using it early means every subsequent death is fully final

---

## Debuff Integration (existing systems feeding into combat)

- Nausea/Fever/Delirium and Thirst's Headache/Withered/Trembling all reduce STR/AG/GRIT — sickness and dehydration measurably weaken combat performance, not just narratively
- Crippled reduces movement speed — likely affects turn order and/or Flee success
- Vertigo (Delirium) could scramble movement-based actions specifically

---

## Open Items
- [ ] Lock exact base damage values and stat modifiers
- [ ] Define turn order tie-breaking rule (equal AG)
- [ ] Decide Flee mechanic's governing stat (AG vs VOICE) and success formula
- [ ] Confirm whether a **failed** Last Stand roll still consumes the once-per-run use, or only a successful trigger does
- [ ] Full weapon-by-weapon debuff assignment (deferred to future session)
- [ ] Legendary weapon unique combat effects (Wind Tempest, Faahard's Oblivion Blade)

## Changelog
- Locked Last Stand as GRIT-gated %, once per entire run, no post-trigger bonus
- Locked crit system: AG → chance, SKILL → damage multiplier
- Confirmed weapon debuffs are per-weapon, not per-category — full assignment deferred
---
tags: [the-margin, game-design, systems, status]
status: draft
---

# Status System

Related: [[Hunger System]] · [[Debuff System]] · [[Combat System]] · [[Gear with Memory]] · [[Fog of War]]

## Overview
Six core stats govern combat, survival, social interaction, and skill-based actions. Each stat ties into at least one existing system.

---

## Stats

### STR — Strength
- Physical power stat
- Already tied into [[Hunger System]] (Starving Stage 1: -35% max Strength) and [[Debuff System]] (Nausea, Fever)

### GRIT
- Resilience stat
- Powers the **Last Stand** mechanic (core game identity: Permadeath / Last Stand)
- Governs blocking/defense

### INS — Instinct
- Perception stat beyond normal vision range
- Under [[Fog of War]]: lets the player sense/detect enemy presence and movement outside visual range
- Surfaced through descriptive text/dialogue cues rather than visual reveal (e.g., "you hear footsteps to the east") — keeps tension without minimap-style cheating

### AG — Agility
- Movement/evasion/dodge stat
- Already tied into [[Debuff System]] (Starving Stage 2: Trembling -15% Agility; Delirium: Vertigo)

### VOICE
- Persuasion stat
- Used on NPCs and possibly animals
- Potential applications: better trade prices, talking down hostile encounters (e.g., [[NPCs#poachers-camp|Poacher's Camp]]), calming aggressive animals, unlocking deeper dialogue/lore (especially with the [[NPCs#traveling-wanderer|Traveling Wanderer]])

### SKILL
- Hands-on competence stat
- Governs: crafting, cooking, restoring/repairing (ties directly into [[Gear with Memory]]), lockpicking (ties into locked content like [[World Structure System#9-the-old-house|The Old House]]'s cellar/chest)

---

## System Hooks (existing ties)

| Stat | Existing System Tie-In |
|---|---|
| STR | Hunger System, Debuff System |
| GRIT | Last Stand mechanic, blocking/defense (Combat — not yet built) |
| INS | Fog of War (not yet built) |
| AG | Debuff System |
| VOICE | NPCs / Trading (not yet built as a mechanic) |
| SKILL | Gear with Memory (not yet built), World Structure locked content |

## Open Items
- [ ] Build Fog of War system to give INS a functional home
- [ ] Build Gear with Memory (crafting/repair) to give SKILL a functional home
- [ ] Decide if VOICE has numeric thresholds (e.g., persuasion success %) or binary gate checks
- [ ] Combat System not yet built — GRIT's blocking/defense role still conceptual

## Changelog
- Defined all six stats: STR, GRIT, INS, AG, VOICE, SKILL
- INS confirmed as an enemy-detection stat working through Fog of War via text/dialogue cues, not visual reveal
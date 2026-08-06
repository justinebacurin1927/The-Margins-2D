---
tags: [the-margin, index, moc]
status: living
---

# 00 — INDEX · The Margin Design Bible

The canonical reading & build order for the design, structured for a future **BMAD workflow**.

> **Note on numbering:** the docs keep their original filenames so the Obsidian `[[wikilinks]]` between them stay intact. The numbers below are the **canonical order**, not filename prefixes. (If you want numbered filenames later, say so — it's a rename + link-update pass.)

**Visual references:** `Map.png` (canonical world map) · `Vision.png` (UI / art-direction target).

---

## I · World & Setting
1. [[evermove]] — Kingdom of Evermove worldbuilding (H.E.R.M.E.T.I.C.S.): history, economy, religion, the Sense/Mystic/Magic power systems, the class ladder
2. [[Herois Region]] — the **playable region**: canon geography, the west=home / east=danger gradient, location placement, map approach

## II · Protagonist & Story
3. [[Protagonist - Klein]] — the main character (Novelborne knight; canonical **GET HOME**)
4. [[Lore System - Storyline Roadmap]] — the main-story spine (Acts 0–3, the one-spine + "What if…" architecture)
5. [[Story - Act Breakdown]] — detailed act beats, cast per act, act-gating quests, the rescue-Aldric thread
6. [[Prologue - The Fall of Corneo]] — the intro text (read / skip)

## III · Gameplay
7. [[Gameplay Roadmap]] — the core loop (Turn→Foray→Day→Act), system map, presentation (SPD-style), and the build order

## IV · People & Narrative Systems
8. [[NPC System]] — the five NPC roles + faction/allegiance
9. [[NPC Roster]] — the cast & backgrounds (Aldric, Mara, Old Fen, Yenna, Vos, the Free Company)
10. [[Companion System]] — companions as help + liability; Bond; loss
11. [[Dialog System]] — the narrative delivery pipe (VOICE-gated, four channels)
12. [[Quest System]] — NPC-given, discovery-triggered, and act-gating quests

## V · Character & Survival Systems
13. [[Status System]] — the six stats (STR / GRIT / INS / AG / VOICE / SKILL)
14. [[Hunger System]]
15. [[Thirst System]]
16. [[Temperature and Exposure System]]
17. [[Debuff System]]
18. [[Food System]]

## VI · Items, Gear & Economy
19. [[Inventory System]]
20. [[Weapon System]] — gear with memory
21. [[Currency System]]

## VII · World Simulation & Conflict
22. [[Day & Night Cycle System]]
23. [[Weather System]]
24. [[World Structure System]] — the 11 locations across 3 danger tiers
25. [[Combat System]]

---

## BMAD Workflow Map

When ready to move from design → build, this bible feeds the BMAD lifecycle:

| BMAD artifact | Fed by |
|---|---|
| **Product Brief / PRD** | I (World) · II (Protagonist & Story) · III (Gameplay Roadmap) |
| **Architecture spine** | III (Gameplay Roadmap: core loop, system map, build order) + the kept code core |
| **Epics & Stories** | II (Act Breakdown) + IV–VII (the systems, each a buildable slice) |
| **Sprint plan** | the build order in [[Gameplay Roadmap]] §9 (Phase 0→5) |

Suggested sequence: `bmad-product-brief` → `bmad-prd` → `bmad-architecture` → `bmad-create-epics-and-stories` → `bmad-sprint-planning`.

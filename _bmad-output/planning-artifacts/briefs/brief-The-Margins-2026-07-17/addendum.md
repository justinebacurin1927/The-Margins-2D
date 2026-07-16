---
title: "Brief Addendum: The Margins"
status: draft
created: 2026-07-17
updated: 2026-07-17
---

# Brief Addendum — The Margins

Depth captured during the brief conversation that belongs downstream (PRD / architecture / solution design) rather than in the 1–2 page brief itself. Not audit information (that lives in `.memlog.md`).

## Open Questions to Resolve in PRD / Architecture

1. **Combat & economy numbers.** Design commits to "low numbers, tactics over stats" but no concrete values exist yet: HP pools, damage ranges, dodge/armor formulas (current code: grit `/2` flat reduction; instinct 3%/pt dodge; block halves), hunger tick rate, XP/progression curve, gold sources/sinks. Needs a first-pass tuning table in the PRD.
2. **Unique-art pipeline.** Current art is CraftPix packs (temple tileset, cultists, orc, UI). The fiction needs distinct, recognizable characters (Milek, Galleon, scavenger NPC for MVP; far more for Vision). Open: commission vs. asset-pack recolor vs. creator-drawn? This is a real solo-dev bottleneck — decide the *minimum* viable approach for MVP before Route 1 story floor.
3. **Field of view / fog of war.** Not yet implemented; MVP needs it for stealth to read. Shadowcasting is the standard approach; confirm during architecture.
4. **Dialog & quest frameworks.** `dialog/` (DialogNode) and `quest/` (QuestManager) classes exist in the codebase but are unwired. Architecture must decide how authored scenes, INSTINCT gates, and branching choices hook into the turn loop and floor transitions.
5. **Save/continue format.** MVP needs single-run save/resume. Open: serialize full state vs. seed + action-log replay. Architecture decision.
6. **Companion "leverage" mechanic — concrete form.** Galleon's MVP ability ("be loud about this, or smart") needs a concrete mechanical expression (aggro pull / noise radius / distraction token). Define in PRD.

## Deferred-System Rationale (why these are OUT of MVP)

- **Trust Meter, full Bond system, Alpha transformation economy, multiple endings, literacy tree, factions, alchemy, shops** — each is a system unto itself. Including any of them in MVP risks the never-ships failure mode. They are proven-out *after* Route 1 demonstrates the loop is worth extending. The Vision section of the brief preserves them as the north star.
- **Standalone narrative legibility** — a "roguelike-players-first" onboarding layer (making the story earn a cold player) was explicitly deprioritized because the audience is you-first / readers-secondary. If the project ever pivots toward a public roguelike audience, this becomes a real work item and should re-enter scope.

## Existing-Codebase Facts (brownfield context for architecture)

Phase 0 SPD core is already working and committed: turn-based grid movement, procedural BSP floor generation, tile types (wall/floor/door/stairs), turn-based melee combat, defense mechanics (grit armor, instinct dodge, E-key block, arrival grace), enemy chase AI, enemy HP bars, wait action, hunger base, stairs-down progression, permadeath + restart. Maven multi-module (`core` + `desktop`/LWJGL3). This is a **brownfield** build — the MVP extends this core, it does not start from zero.

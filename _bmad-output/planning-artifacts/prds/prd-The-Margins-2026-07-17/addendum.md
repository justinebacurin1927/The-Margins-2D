---
title: "PRD Addendum: The Margins MVP"
status: draft
created: 2026-07-17
updated: 2026-07-17
---

# PRD Addendum — The Margins MVP

Technical-how and downstream depth deferred out of the capability-level PRD. This is the architect's raw material (Phase 3).

## Brownfield surface (what already exists to build on)
`core/src/main/java/com/margins/rogue/` holds the working Phase-0 core: `RogueGameScreen` (main loop), `RoguePlayer`, `RogueEnemy` (chase AI), `RogueTileMap`, `RogueTile`, `FloorGenerator` (BSP). Unwired scaffolding exists in `dialog/` (`DialogNode`) and `quest/` (`QuestManager`) — the architecture must decide how to integrate rather than rebuild. `asset/Assets.java` handles texture loading. Maven multi-module (`core` + `desktop`/LWJGL3).

## Technical decisions to resolve in architecture
- **FOV (FR-1/2):** recursive shadowcasting over the existing tile grid; store per-tile `visible`/`explored` bits. Confirm sight-blocking rules for doors.
- **Detection (FR-3/4/5):** enemy state enum on `RogueEnemy`; per-turn update reading FOV + a Noise event queue. Decide vision cone (directional, needs facing) vs. radius. Noise as transient events with origin + radius consumed on the enemy update tick.
- **Dialogue integration (FR-6/7/8):** a modal dialogue controller that suspends turn processing; INSTINCT check = threshold compare (deterministic) or roll (stochastic) — pick one. Flags in a run-scoped key/value store also used by save.
- **Identify-by-Use (FR-11/12):** per-seed map `SupplyType → TrueIdentity` built at Run init from the seed RNG; an `identified` set tracked per Run.
- **Save/continue (FR-20/21):** serialize full Run state. Decide serialize-full-state (simpler, robust to logic changes) vs. seed + action-log replay (smaller, brittle). Single slot; delete on true death.
- **Story Floor (FR-18):** author as a fixed layout the generator can load, vs. constrained procedural with pinned rooms. Recommend fixed layout for authorial control of the reunion beat.
- **Companion pathing (FR-13):** reuse enemy pathfinding for a follow behavior; ensure floor-transition carries Galleon.

## Art pipeline (schedule risk, not a code blocker)
MVP needs distinct sprites for Milek, Galleon, and one scavenger NPC beyond CraftPix packs. Decide minimum-viable approach (recolor existing packs / commission / creator-drawn) before the Story Floor is built. Track as a parallel non-code workstream.

## Deferred-system rationale (unchanged from brief)
Trust Meter, full Bond/roster, Alpha transformation economy, multiple endings, literacy tree, factions, alchemy, shops, sound, mobile — each a system unto itself; included in MVP would risk the never-ships failure mode. Proven out only after Route 1 demonstrates the loop.

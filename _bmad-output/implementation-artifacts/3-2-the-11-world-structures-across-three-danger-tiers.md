---
baseline_commit: aa90c99
---

# Story 3.2: The 11 World-Structures across three danger tiers

Status: ready-for-dev

## Story

As Klein,
I want distinct scavenge destinations placed along the danger gradient,
So that where I go trades loot against risk (FR-9, FR-10).

## Acceptance Criteria

**AC-1 (FR-9):** Given the generated map, when World-Structures are placed, then all 11 exist across 3 tiers consistent with the east/west spine (T1 Hunter's Blind / Fallen Log Hollow / Forest Shrine / Beehive Grove; T2 Kitchen Camp / Collapsed Watchtower / Poacher's Camp / Sunken Well; T3 Old House / Mercenary Graveyard / Deep Cave Mouth).

**AC-2 (FR-10):** Given any World-Structure, when I reach it, then it exposes its listed loot set and hazard (e.g. Hunter's Blind: rope/small tools/20% Map Fragment, weak-floor-plank hazard; Old House: preserved food/cloth/locked cellar, structural-decay hazard).

## Baseline (what Story 3.1 + the placement baseline already shipped)

Story 3.2 does **not** build structure placement from scratch. The `aa90c99` baseline (committed before this story) already stamps, renders, and network-connects all 11 structures:

- `FloorGenerator` stamps all 11 via the generic `stampStructure(map, room, type, StructureCollision)` pattern (`FloorGenerator.java:423`) plus `stampOldHouse`/`stampGraveyard`/`stampHuntersBlind`/`stampDeepCave`. Per-structure collision functions (`fallenLogTile`, `forestShrineTile`, `beehiveGroveTile`, `kitchenCampTile`, `watchtowerTile`, `poachersCampTile`, `sunkenWellTile`) reuse only `WALL/FLOOR/FURNITURE` — **no new `RogueTile` values** (the 3.1 seam held).
- Authored positions (from `FloorGenerator.generate`): town plaza at `spine.corneoX()/corneoY()`; Old House south-west, Graveyard north of the plaza (the canon home cluster); Deep Cave north-east (`town.cx()+28`); Hunter's Blind near town; Fallen Log Hollow `spine.tileX(.33f)`; Forest Shrine `spine.tileX(.08f)`; Beehive Grove `spine.tileX(.46f)`; Kitchen Camp `spine.tileX(.60f)`; Collapsed Watchtower `spine.watchtowerX()`; Poacher's Camp `spine.tileX(.78f)`; Sunken Well `spine.tileX(.85f)`.
- `RogueTileMap` already declares all 11 `STRUCTURE_*` type constants (`RogueTileMap.java:6-17`); `structureTypes` is the persisted per-cell type layer (AD-6, inline with the map).
- `HybridMapTest` already pins: all 11 stamped across seeds, each structure opens onto the network (`structureOpensOntoNetwork` loop, `HybridMapTest.java:205-216`), the non-structure wilderness is one component.
- **336 core tests green** on the baseline.
- The screen (`MarginScreen` + `PixelPack`) renders each structure's atlas with real art (`assets/structures/*.png`, `art/generated/` source). Story 3.2 does **not** add art.

**What the baseline does NOT have — Story 3.2's actual scope:**
- No danger-tier taxonomy (T1/T2/T3 metadata per structure). The current placement does **not** encode the epics.md tier list — e.g. Sunken Well (T2 in spec) sits at far-east x=0.85; Old House/Graveyard (T3) sit in the safe west; Poacher's Camp (T2) at x=0.78; Fallen Log Hollow (T1) at x=0.33.
- No per-structure loot sets. `RunState.placeFloorActors` still scatters the generic `Supply.scatterableOrdinals()` set by region eastness — structure interiors get the same loot as any wilderness clearing.
- No hazards. Nothing triggers when Klein steps onto a structure.
- No new loot items (rope, small tools, Map Fragment, preserved food, cloth don't exist in `Supply`).

## In/Out of Scope Seam

**IN:**
- The structure **metadata model** (pure, transient — the 11 entries: structure type → danger tier, loot set, hazard).
- The **danger-tier taxonomy** and its spine-consistency reconciliation (Design Decision 1).
- Per-structure **loot sets** and the new `Supply` items they need (Rope, Small Tools, Map Fragment, Preserved Food — appended-last per AD-6).
- The **hazard model** and its turn-step trigger in `TurnEngine` (Design Decision 2).
- Content wiring + the AC pins + full-suite/boot verification.

**OUT (later stories):**
- The foray loop (leave→travel→scavenge→return carry-back under one clock) — **3.3**.
- Night/weather location-danger shifts (Graveyard undead, Sunken Well creature, Poacher's Camp patrols, Beehive Grove safer) — **3.4**. Story 3.2 defines each structure's **daytime baseline** hazard only; the night-flip overrides are 3.4's hook.
- SKILL-governed outcomes and knowledge querying (lockpicking the Old House cellar, map-fragment knowledge) — **3.5**. Story 3.2 introduces the **Map Fragment collectible** and the **locked cellar** as content, but not the lockpicking roll or the knowledge system.
- The Deep Cave Mouth interior transition (Region-2 threshold, NOT the exit) — **AD-12 / a later region pass**. Story 3.2 gives the cave its authored surface loot/hazard only.
- New `RogueTile` values — **reuse existing** `WALL/FLOOR/DOOR/FURNITURE` (the 3.1 seam). Hazards are a metadata + step-trigger, never a new tile.
- Any new persisted `RunState` field — **forbidden** (AD-6). Structure metadata is derived from the persisted `structureTypes` layer + constants, exactly like `WorldSpine`.
- Weapon/gear tiers, combat, occupation escalation — **Epic 4**. Rope/Small Tools are inert craft materials here (the repair economics are 4.5).
- Currency, traders, bag durability — **Epic 6**.

## Design Decisions (the interpretation calls)

1. **Danger tier = authored content metadata, reconciled with the spine — NOT derived from position.** The 11 structures sit at authored positions (baseline; the home cluster is canon AD-8). The tier is an authored property per structure matching epics.md's membership list, and it governs the structure's **loot value + hazard severity**. "Consistent with the east/west spine" (AC-1) is enforced as a testable monotonic core with **three named canon exceptions**: tier is non-decreasing in eastness for the 8 mid/east structures (T1 west → T2 east → Sunken Well the easternmost), while **Deep Cave** (T3 at mid-east — it is the Region-2 *threshold*, AD-12, so its T3 is transition depth, not surface east-west), **Old House**, and **Graveyard** (home-cluster T3-by-hazard-depth) are the documented exceptions. Each exception carries a written `reason` in the metadata so the monotonicity check cannot silently exempt structures. The generic *scatter* gradient (Story 3.1's `enemyCountFor`/`supplyCountFor`) is untouched; per-structure loot is authored on top.
2. **Hazards are a structure-metadata + turn-step trigger, not new tiles and not a new persisted field.** A pure model maps each structure (or hazard cell) to a hazard; `TurnEngine`'s player-move branch (the `isWalkable(tx,ty)` success at `TurnEngine.java:48`) checks the destination cell's structure hazard and applies the effect. No `RogueTile` addition, no `RunState` field (derived from `structureTypes` + constants — AD-6 transient, the `WorldSpine` precedent). Deterministic hazards apply directly; probabilistic hazards take **one seeded `rng` draw per event** (AD-5).
3. **Per-structure loot sets are authored tables layered ON TOP of the generic scatter (additive, AD-5-safe).** Each structure's footprint draws from its own loot set (item + count + chance) in a separate pass after `placeFloorActors` — the generic eastness scatter stays byte-identical (removing structure rooms' draws would shift every later room's seeded stream and change the wilderness). New items appended to `Supply` **last** so existing ordinals and saves are unchanged (the Story 1.5/1.7/2.4 append-last pattern). Single-identity types (name IS the real name) — no identify gamble.
4. **Map Fragment is a collectible now, knowledge later.** Introduced as an inert, single-identity collectible (the FR-11 knowledge seed — "map fragments accumulate"); its query/consumption into the journal/knowledge system is 3.5. The Old House **locked cellar** is content (a decay-hazard-locked rich loot room); the lockpicking roll is 3.5's SKILL hook.
5. **The 0.2f safe-tier convention carries.** Story 3.1 made the safe band `<= 0.2f`; the structure tiers must not contradict it — a T1 structure sitting at eastness ≤ 0.2 (Forest Shrine at x=0.08) is fine (T1 = mild loot/hazard), but the authored tier's loot must still rise with the authored tier, not re-introduce an eastness inversion.

## Tasks / Subtasks

- [ ] **Task 1 — The structure metadata model (pure, transient): `world/StructureTable` (AC: 1)**
  - [ ] New `core/src/main/java/com/margins/rogue/world/StructureTable.java` (pure model, no libGDX — AD-2, the `WorldSpine` precedent). One entry per structure type (all 11), each carrying: display name, danger tier (1–3), loot set, hazard.
  - [ ] Danger tiers match epics.md exactly: T1 Hunter's Blind / Fallen Log Hollow / Forest Shrine / Beehive Grove; T2 Kitchen Camp / Collapsed Watchtower / Poacher's Camp / Sunken Well; T3 Old House / Mercenary Graveyard / Deep Cave Mouth.
  - [ ] Loot set = the authored item table (Supply ordinal + count + chance) for that structure; hazard = the authored hazard id (Task 4's model). The two worked examples from PRD FR-10 must resolve: Hunter's Blind = rope/small tools/20% Map Fragment + weak-floor-plank; Old House = preserved food/cloth/locked cellar + structural decay.
  - [ ] Deterministic from constants; no persisted state (AD-6). Keyed by `RogueTileMap.STRUCTURE_*` int.
  - [ ] Tests: exactly 11 entries, one per `STRUCTURE_*` constant; tier membership matches the epics.md list; every entry resolves a loot set AND a hazard (no missing content); the two worked examples resolve to the specified items/hazards.

- [ ] **Task 2 — Danger-tier spine consistency (AC: 1)**
  - [ ] Enforce Decision 1 with a testable formulation. Authored tiers over the non-home structures are: Forest Shrine T1 @ .08, Hunter's Blind T1 @ ~town (.17), Fallen Log Hollow T1 @ .33, Beehive Grove T1 @ .46, Kitchen Camp T2 @ .60, Collapsed Watchtower T2 @ ~.67, Poacher's Camp T2 @ .78, Sunken Well T2 @ .85, Deep Cave T3 @ ~.46. The monotonic core (T1 west → T3 east) holds for **8 of the 9** — the only inversion is **Deep Cave**, whose T3 sits at mid-east, west of every T2. Its justification is canonical: the Deep Cave Mouth is the **threshold into the underground (AD-12 / Region-2)** — its T3 is transition depth, not surface east-west. So:
  - [ ] Test A (monotonic core): tier is non-decreasing in eastness over the **8 non-home structures excluding Deep Cave** (Forest Shrine → Beehive Grove T1, then T2 east of them, Sunken Well the easternmost T2).
  - [ ] Test B (the documented exceptions): Deep Cave, Old House, and Graveyard are **explicitly named as the three canon exceptions** — each carries a written `reason` in `StructureTable` (Deep Cave = Region-2 threshold; Old House/Graveyard = home-cluster T3-by-hazard-depth, AD-8). The test asserts the exceptions are the ONLY non-monotone members, so the check cannot silently pass by mass-exempting structures.
  - [ ] Tests: all 11 exist on the map across seeds with their authored tiers (extend the baseline's `hasStructure` loop to assert tier presence); Test A + Test B above.

- [ ] **Task 3 — Loot sets + the new Supply items (AC: 2)**
  - [ ] Add to `Supply` (append-last, AD-6): `ROPE` (inert craft material — spears/repairs use Wood+Rope per FR-13), `SMALL_TOOLS` (inert tool), `MAP_FRAGMENT` (inert knowledge collectible — Decision 4), `PRESERVED_FOOD` (a nourishing provision — single-identity, slow/zero spoilage). Each with its `TrueIdentity` binding following the existing single-identity pattern.
  - [ ] `StructureTable.lootFor(type)` → the authored item table. A new `placeStructureLoot` pass (called after `placeFloorActors` in `RunState.generate`/`new RunState`) scatters each structure's authored loot inside its footprint (walkable cells, avoiding the player). **Additive, NOT replacing**: the generic eastness scatter stays exactly as-is for every room — AD-5's single seeded stream means removing structure rooms' draws would shift every later room's layout and break byte-identical wilderness. Structure rooms get the generic scatter (as today) PLUS their authored set — structures are destinations, so they are worth going to.
  - [ ] AD-5: one `rng` draw per placed item / per probabilistic loot entry; the authored pass runs AFTER the generic scatter on the same seeded stream, so the generic layout's draw sequence is untouched (reproducible) and the authored loot is a stable suffix of the stream.
  - [ ] Tests: each structure's footprint yields its authored loot across seeds (or, for chance entries, the item can appear and the entry's chance is honored as a distribution, not a guarantee — seed-42 pins a concrete layout); Map Fragment is collectible and inert; `sameSeedReproducesEnemyAndSupplyLayout` still holds; a pinned pre-story seed's non-structure scatter is unchanged (the generic pass is byte-identical).

- [ ] **Task 4 — Hazards: the model + the turn-step trigger (AC: 2)**
  - [ ] Hazard model (in `StructureTable` or a sibling `world/HazardTable`): the hazard ids for the 11 structures with their effects. The two known: weak floor plank (Hunter's Blind — a step-risk: chance to fall, e.g. minor HP + a stumble), structural decay (Old House — step-risk: chance of a partial collapse that damages and/or blocks a tile). Propose + implement the remaining 9 as content (Decision 2's shape; see the proposed table in Dev Notes — flagged for review, not bible-locked).
  - [ ] `TurnEngine` player-move branch: after a successful move onto a walkable tile with a structure hazard, apply the hazard (deterministic or one seeded `rng` draw per event — AD-5). Must NOT tick the survival clock twice or emit spurious noise (the hazard is not combat noise; only AD-9 emitters emit).
  - [ ] Keep the hazard fully core-layer (AD-2): the screen renders nothing new — effects land on existing surfaces (message log, HP, debuffs, tile state).
  - [ ] Tests: each hazard triggers on the right structure tile and applies the right effect; probabilistic hazards honor AD-5 (deterministic on a fixed seed); a hazard cannot trigger on a non-structure walkable tile; stepping on a structure with no hazard is a no-op; the existing acted-turn smoke (`actedTurnsResolveWithinBudgetOnTheWiderMap`) stays under budget with hazards active (AD-16).

- [ ] **Task 5 — AD-6 / serialization seam + no-regression (AC: all)**
  - [ ] NO new persisted `RunState` field; structure metadata transient/derived (Decision 2). Verify a 3.2-state round-trip (map + actors + supplies + structureTypes) loads with hazards and loot intact (they derive from the saved `structureTypes`, exactly like `WorldSpine`).
  - [ ] A pre-3.2 save (50×50-era or the 3.1 96×48) still loads: `aFiftyByFiftyEraSaveLoadsWithItsOwnDimensions` and the persistence suites stay green.
  - [ ] Tests: round-trip a 3.2 run (structure types survive; loot/hazard queries work post-`restoreAfterLoad`); `SaveMigrationTest`/`RunStatePersistenceTest` unchanged and green.

- [ ] **Task 6 — AC pins + full suite, no regressions (AC: all)**
  - [ ] AC-1 pin: all 11 present with authored tiers across seeds (Task 2's test). AC-2 pin: reaching a structure exposes its loot (Task 3) and hazard (Task 4) — a "reach-and-resolve" test walks Klein into each structure and asserts the loot + hazard contract.
  - [ ] Full suite: `mvn -o -pl core test` — the baseline **336 stay green** (the 3.1 persistence/narrative/survival/water/combat suites all pass unchanged), plus the new structure tests.
  - [ ] Launch: `mvn -o -q -pl core install` + `timeout 40 mvn -o -pl desktop exec:java` — boot clean (the structures render with their art, hazards resolve, camera still follows).

## Dev Notes

### Current state (what exists, to preserve)

- **The 11 structures are already on the map** (baseline `aa90c99`, pinned by `HybridMapTest`): stamped footprints, rendered art, network-connected entrances (`structureOpensOntoNetwork` for every type). Do NOT re-stamp, re-render, or re-place them. Story 3.2 adds the *content* layer.
- `WorldSpine` (`core/src/main/java/com/margins/rogue/world/WorldSpine.java`) — the pure, transient east/west query (`eastness(int x)`, `dangerAt`, `tileX/tileY(fraction)`). The structure metadata model follows this exact shape.
- `RogueTileMap.structureTypes` — the **persisted** per-cell structure-type layer (AD-6, inline with the tilemap). This is the derivation source: `getStructureType(x,y)` → the structure type → `StructureTable` entry → tier/loot/hazard. No new persisted field needed.
- `RunState.placeFloorActors` (`RunState.java:150-190`) — the generic eastness scatter (`enemyCountFor`/`supplyCountFor`, one draw per actor/item). It is left byte-identical; Task 3 layers an authored loot pass ON TOP for structure footprints (AD-5-safe: a separate pass after the generic one, so the generic draw sequence is unchanged).
- `TurnEngine` player-move branch (`TurnEngine.java:41-48`): `action.dx/dy` → `isWalkable(tx,ty)` success is where the hazard check hooks (Task 4). The acted-turn pipeline (AD-4) is untouched otherwise.
- `Supply`/`TrueIdentity` append-last pattern (Story 1.5/1.7/2.4): new enum constants appended last so ordinals and saves are unchanged (AD-6). Single-identity types are self-evident (name IS the real name) — no identify gamble.
- `HybridMapTest` connectivity loop (`:205-216`) — the per-structure `structureOpensOntoNetwork` assertion; any re-stamp or footprint change must keep it green.

### Carried lessons (1.5/1.7/2.x/3.1, applied)

- **Authored, not seeded** (3.1 Decision 1): structure positions and metadata are constants; only per-item/per-roll draws touch the seeded `rng` (AD-5). Never derive a structure's position from `rand`.
- **One rng draw per event** (AD-5): a hazard roll or loot draw is exactly one `nextInt`/`nextFloat` on the seeded stream. Structure loot is a **separate pass after** the generic scatter so the generic layout's seed reproducibility is unchanged (pin it).
- **Transient-or-derived, never a new persisted field** (AD-6): structure metadata derives from `structureTypes` + constants, like `WorldSpine`. The field-absent migration rule (bit twice: 1.3 weather, 3.3-identify) — if a dev note proposes a `RunState` field, it needs a deterministic default AND a load-time reconcile; the preferred answer here is "no field."
- **Connectivity is a generator guarantee** (O4/3.1): the repair throws on a failed carve (`ensureReachable`). New hazard/loot work must not break the walkable-cell contract inside a structure (hazards trigger on step, they don't block entry — a sealed structure is a 3.1-style defect).
- **Safe tier `<= 0.2f`** (3.1 review patch): the home cluster is enemy/supply-free. Structure loot is authored on top; it should not re-populate the safe west with T3-value loot via the generic scatter.

### Proposed hazard table (content — flag for review, not bible-locked)

The PRD gives two worked examples (weak floor plank; structural decay). A coherent shape for the rest (daytime baseline only; the night-flips are 3.4):

| Structure | Tier | Hazard (proposal) | Loot (proposal) |
|---|---|---|---|
| Hunter's Blind | 1 | weak floor plank — step-risk (fall: minor HP) | rope, small tools, 20% map fragment |
| Fallen Log Hollow | 1 | soft rot — step-risk (sink/stumble) | small tools, wood, salt |
| Forest Shrine | 1 | collapsing stone — step-risk | salt, coal, sealed letter (lore) |
| Beehive Grove | 1 | swarm — step-risk (poison-ish sting, mild) | honey, honeycomb |
| Worn Down Kitchen Camp | 2 | fire/ash residue — step-risk (scald) | preserved food, salt, coal, small tools |
| Collapsed Watchtower | 2 | tower collapse — step-risk (debris block + damage) | rope, small tools, map fragment |
| Poacher's Camp | 2 | snare trap — step-risk (immobilize/stumble) | rope, small tools, raw meat |
| Sunken Well | 2 | slip-and-fall — step-risk (fall: minor HP) **bible-grounded**; creature = 3.4 night | stable well-water draw + rare coins (bible: "grab rare coins"; coins are a collectible here — the currency economy is 6.3) |
| Old House | 3 | structural decay — step-risk (collapse: damage + blocked tile); locked cellar content | preserved food, cloth, cellar rich-loot (locked) |
| Mercenary Graveyard | 3 | grave-ground contamination (daytime); undead = 3.4 night | cloth, small tools, lore |
| Deep Cave Mouth | 3 | cave-in/creature presence — step-risk | preserved food, cloth, rare map fragment |

Bible grounding (PRD 2026-08-06): only the two worked examples (Hunter's Blind; Old House) are contractual loot/hazard sets. The others' specifics are the dev agent's tunable content — but the **Sunken Well's** water + coins and the **Beehive Grove's** night-flips-safer / Graveyard-undead / Poacher's Camp-patrols / Well-creature night shifts are canon (3.4 owns the night side). Water collection (Story 1.5) and the well's stable-source status (PRD line 178) carry the well's water loot.

The dev agent owns implementing this as the content layer — the exact numbers/hazards are tunable (PRD §8: combat/stealth/numbers are Phase 2–3 calibration), but the **structure of a per-structure loot set + per-structure hazard** and the two worked examples are contractual.

### What NOT to do (scope discipline, CLAUDE.md §2/§3)

- Do NOT add a `RogueTile` value for "hazard" or "structure." Reuse the existing 7.
- Do NOT add a persisted `RunState` field for structure metadata.
- Do NOT build the night-flip system (3.4), the foray carry-back (3.3), lockpicking/knowledge (3.5), or the cave interior (Region-2). Each may get a **hook** (a clearly-marked extension point) but no implementation.
- Do NOT touch the generic wilderness scatter's seeded stream. Structure loot is a separate authored pass.
- Do NOT change the structures' footprints or positions unless a hazard/loot requirement genuinely needs it (and if so, keep `structureOpensOntoNetwork` green).

### Testing standards

- JUnit 5 headless core; `new RunState(seedL)` (seed 42 for single-run pins; ranges of seeds for hybrid/probabilistic properties).
- `mvn -o -pl core test`, `mvn -o -q -pl core install`, `timeout 40 mvn -o -pl desktop exec:java` (exit 143 = timeout kill = clean boot).
- Baseline must stay **336 green**; the story adds the structure tests. No regressions across the 3.1 persistence/narrative/survival/water/combat suites.

### References

- [Source: architecture/ARCHITECTURE-SPINE.md#AD-8 (104–112)] — one continuous region; the world-gen epic owns the stitch.
- [Source: architecture/ARCHITECTURE-SPINE.md#AD-6 (94–97)] — serialization versioned; the field-absent migration rule.
- [Source: architecture/ARCHITECTURE-SPINE.md#AD-5 (84–88)] — one committed turn per real action; companions share the turn.
- [Source: architecture/ARCHITECTURE-SPINE.md#AD-16 (163–167)] — worst-case turn budget; the coarse smoke stays.
- [Source: prds/prd-The-Margin-2026-08-06/prd.md#FR-9 (204–210), FR-10 (211–217)] — the 11-structure placement + the foray loop; the two worked loot/hazard examples (214).
- [Source: prds/prd-The-Margin-2026-08-06/prd.md (105, 178, 215, 222)] — the tier membership list (105); Sunken Well stable water (178); night location-danger flips incl. Beehive Grove safer (215); map-fragment knowledge accumulation (222).
- [Source: epics.md Story 3.2 (430–444)] — the ACs and the tier membership list.
- [Source: 3-1-hybrid-map-generation.md] — the placement baseline, the 3.1 review findings, and the story-seam line "the 11 World-Structures + per-structure loot/hazard sets (3.2)."

## Dev Agent Record

### Agent Model Used

### Debug Log References

### Completion Notes List

## File List

## Change Log

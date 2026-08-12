# Deferred Work

## Triage (2026-08-08)

Full re-triage before Epic 3 (epic-1 retro action #5). Verdicts: **22 closed** (superseded by
work already delivered — mostly the continuous-map refactor (1.1, AD-8), the saveVersion gate
(1.1, AD-6/AD-8), and the Epic 1 story chain 1.2→1.6→1.8), **18 open** (below, each tagged with
its target epic/story). **Epic 2-relevant:** the three items tagged `[Epic 2]` feed Story 2.2's
intro and the Story 2.1/2.4/2.5 authoring work; the dialogue-authoring carry (#5) is already
folded into Story 2.1's dev notes.

**Open (working list):**

| # | Item (source) | Target | Verdict |
|---|---------------|--------|---------|
| O1 | `applyCacheReveal` gates on `== 1` + no null-guards | **Epic 2** (2.4/2.5 scene authors) | open — same non-1 hardening family Story 2.1 covers for the dialogue path |
| O2 | Comment drift / stale "AD-8" + "pre-4.3 save" | **Epic 2** (2.1 — file 2.1 edits) | open — fix the AD citation when 2.1 touches DialogController |
| O3 | Opening (cycle-0) weather never gets an onset line | **Epic 2** (2.2 intro) | open — the intro can decide |
| O4 | Map connectivity/reachability not asserted | Epic 3 (3-1) | open |
| O5 | `getStr()` floor hides Nausea-vs-Fever at STR 5 | Epic 3 (3-5) | open |
| O6 | **SaveService has no production callers** | no story yet — add a save/load-UI story | open (TOP) |
| O7 | `equip` same-type-in-both-slots semantics | Epic 4 (4-4) | open |
| O8 | Equipped-item dead-end (unequip w/ full pack) | Epic 4 (4-4/4-5) | open |
| O9 | Companion placement overlap (enemy / player tile) — merged from 2 items | Epic 5 (5-1) | open |
| O10 | Per-tile item stacking / pickup chooser | Epic 6 (6-1) | open |
| O11 | Global-clock spoilage — fresh meat rots in 1 turn | Epic 6 (object inventory) | open |
| O12 | Torch can't be extinguished/dropped/conserved | Epic 6 (torch as item) | open |
| O13 | `restoreAfterLoad()` RNG reseed diverges | no home (low) | open |
| O14 | `tickClock()` only handles 1-turn crossings | carry to a fast-forward story | open |
| O15 | Weather onset lags the temperature effect one turn | pipeline-ordering story | open |
| O16 | "Wait"/refusal lines crowd the 5-line window | HUD polish | open |
| O17 | `Weather.onsetLine()` default → "The skies clear." | when weather grows | open |
| O18 | Bloated slow invisible in the debuff row | HUD polish | open |

---

# OPEN (working list)

## O1 — `SceneEffects.applyCacheReveal` authoring-contract robustness
**Source:** code review of Epic 5 (5.3). **Target: Epic 2** (2.4/2.5 scene authors).

`applyCacheReveal` gates on `== 1` (a `withFlag(key, 2)` never spawns) and lacks the null-guards
`DialogController` has; unreachable today, revisit (`!= 0`, consistency guards) when authored
scenes use non-1 flag values. Same non-1 hardening family as carry #5 — Story 2.1 now covers the
*dialogue* path (Task 5 pins `withFlag(key, 7)`), this is the SceneEffects side. [Low]
[core/src/main/java/com/margins/rogue/narrative/SceneEffects.java]

## O2 — Comment drift / AD-number collision
**Source:** code review of 1-1. **Target: Epic 2** (2.1 — the file 2.1 modifies).

`DialogController.java:~48` still cites an **old** "AD-8" (the obsolete architecture's AD-8 ≠ the
new architecture's AD-8 = continuous map); `RunState.java:49`'s "pre-4.3 save" comment is
old-design vocabulary; `FlagStore`'s "Epic 5 dialogue nodes call this" overclaim becomes true once
2.1's BOND effect calls `applyBondTag`. **Fix the stale AD-8 citation when Story 2.1 edits
DialogController**; flag an architecture-wide AD-reference sweep for the rest.

## O3 — Opening (cycle-0 / restart) weather never gets an onset line
**Source:** code review of 1-8. **Target: Epic 2** (2.2 intro).

The run-start roll is "current state", not a change (AC-2 is about transitions), and the HUD clock
row already shows the weather. Optional atmosphere — the 2.2 intro can decide whether the opening
weather gets an onset line. [Low] [core/src/main/java/com/margins/rogue/state/RunState.java rollWeather]

## O4 — Map connectivity/reachability is not guaranteed or asserted
**Source:** code review of 1-1. **Target: Epic 3** (3-1).

`FloorGenerator` chains rooms in insertion order with no reachability check, and `carveCorridor`'s
3×3 brush can truncate at the map edge via `RogueTileMap.setTile`'s silent clamp. Story 3.1
(world-gen owns the real hybrid map + traversability guarantees) is the home.

## O5 — `getStr()` floor hides the Nausea-vs-Fever STR distinction at base stat 5
**Source:** code review of 1-7. **Target: Epic 3** (3-5).

Nausea −30% and Fever −40% both floor to 3 at STR 5 (`floor(5×0.70) = floor(5×0.60) = 3`), so the
escalation is mechanically invisible in the only offensive stat; Delirium applies no factor. The
distinction materializes once Story 3.5 (horizontal progression) raises STR — revisit the
factor/floor there; do not retune in isolation. [Low] [core/.../rogue/RoguePlayer.java getStr()]

## O6 — `SaveService` has no production callers (TOP)
**Source:** code review of 1-1. **Target:** no story yet — **add a save/load-UI wiring story** (none
exists in the current epics).

The save/load/migration path — including the AD-6 saveVersion guard (Story 1.1) — is never
invoked in the running game; `MarginScreen` only calls `RunState.restart()`. The guard is correct
and tested but dormant. The game cannot currently save. Needs a UI-wiring story (Epic 3+).

## O7 — `equip` same-type-in-both-slots semantics
**Source:** code review of 3.1 (brownfield). **Target: Epic 4** (4-4).

`equip(type)` currently allows the same item type to occupy both equipped slots. Confirm the
intended model (distinct gear vs. duplicate copies) when the gear-with-memory UI is built in 4-4.
[core/src/main/java/com/margins/rogue/item/Inventory.java]

## O8 — Equipped-item dead-end
**Source:** code review of 3.1 (brownfield). **Target: Epic 4** (4-4/4-5).

`unequip` into a full backpack is refused (no item loss, correct) and `drop()` only scans the
backpack, so a player with a full backpack + equipped items has no way to shed an equipped item.
Add an unequip-with-drop path alongside the 4-4 drop/gear UI. [core/.../rogue/item/Inventory.java]

## O9 — Companion placement overlap (enemy / player tile)
**Source:** code review of Epic 4 (4.1) + code review of 1-1 — **merged** (both are
`companionSpotNear` concerns). **Target: Epic 5** (5-1).

`RunState.companionSpotNear` and `Companion.followStep` use a walkable-only check with no enemy
exclusion, so the ally can render stacked on an enemy; on a degenerate map the fallback returns the
player's own tile, stacking the ally on the player. Cosmetic (the companion is a non-colliding ally
by design); ally-collision policy is a deliberate Epic 5/6 concern. (The stairs half of the old
item is closed — the continuous map has no STAIRS, AD-8.)
[core/.../rogue/state/RunState.java companionSpotNear, Companion.java followStep]

## O10 — Per-tile item stacking / pickup chooser
**Source:** code review of 3.2 (brownfield). **Target: Epic 6** (6-1).

Multiple `FloorItem` stacks can sit on one tile; pickup and the rendered marker both act on the
first stack with no disambiguation, and the marker shows no count. Add stacking-on-drop or a pickup
chooser in the 6-1 inventory work. [core/.../rogue/state/RunState.java, floor-item render]

## O11 — Global-clock spoilage — fresh meat can rot within 1 turn of pickup
**Source:** code review of 1-5. **Target: Epic 6** (object inventory).

The batch model advances whole stacks on the run-global spoilage clock, so a Raw stack added at
clock 49 advances at 50 (1 turn of "freshness"). Inherent to the no-per-item-state model (Story 1.5);
per-item age needs the object-based inventory (Epic 6). [Med] [core/.../system/SpoilageSystem.java]

## O12 — Torch can't be extinguished, dropped, or conserved
**Source:** code review of 1-6. **Target: Epic 6** (torch as carried item).

Once lit, the torch burns 60 acted turns with no opt-out (no unlight action; not an inventory item,
so DROP can't touch it). Deliberate 1.6 modeling (pure 60-turn commitment). Extinguish/conserve
needs a new `PlayerAction` + likely the Epic 6 object inventory. [Med]
[core/.../rogue/state/RunState.java torchTurns]

## O13 — `RunState.restoreAfterLoad()` reseeds RNG with `new Random(seed)`
**Source:** code review of 1-1. **Target:** no home (low).

Skips the constructor's `seededRng` cold-start decorrelation, so a resumed run's RNG stream
diverges from a fresh run's. Low impact now (no load in production, O6); fix if resume-parity ever
matters.

## O14 — `tickClock()` only handles 1-turn crossings
**Source:** code review of 1-3. **Target:** carry to the story that adds fast-forward (none yet).

A future fast-forward/skip that jumps many turns must loop `tickClock()` per turn — a single call
past a boundary only rolls the LAST crossed boundary's weather. Fine for the turn-by-turn loop
today. [Low] [core/.../rogue/state/RunState.java tickClock]

## O15 — Weather onset announces a change whose temperature effect lags one turn
**Source:** code review of 1-8. **Target:** a future pipeline-ordering story.

The boundary roll happens after `TemperatureSystem` already ticked under the OLD weather, so a Cold
Snap onset reads as ominous a turn before its −2/turn bites (FOV applies the new weather instantly).
Pre-existing Story 1.6 pipeline ordering. Revisit in a pipeline-ordering story (align the roll with
the temperature step). [Low] [core/.../system/TurnEngine.java]

## O16 — "Wait"/refusal lines crowd notable events out of the 5-line window
**Source:** code review of 1-8. **Target:** HUD polish (not Epic 2).

Grinding WAIT fills the window with "Wait" and pushes notable events out of the rendered 5 lines.
By-design bounded surface (AD-15); a future polish could suppress the wait line (player-initiated,
visible in the input itself) or grow the window. (Story 2.1's dialogue page is a separate surface.)
[Low] [core/.../system/TurnEngine.java]

## O17 — `Weather.onsetLine()` default maps any future weather type to "The skies clear."
**Source:** code review of 1-8. **Target:** when weather grows.

The default branch is speculative (the 5-type enum is fixed and exhaustive); a future weather
constant would silently announce "The skies clear." No current defect. [Low]
[core/.../rogue/Weather.java]

## O18 — Bloated slow invisible in the debuff row
**Source:** code review of 1-8. **Target:** HUD polish.

A Well-Fed player's 50% stumble (`isSlowed()`) has no HUD label. The 1.8 label set (bacterial /
diarrhea / Rotgut / Collapse) is explicit and Bloated is a Well-Fed side effect (legible via
`hungerLabel`). Future HUD enhancement if the stumble needs surfacing. [Low]
[core/.../rogue/RoguePlayer.java getActiveDebuffLabels]

---

# CLOSED (2026-08-08 triage — superseded or delivered)

| Item (source) | Close reason |
|----------------|--------------|
| Make-room swap spends two turns [3.2] | Old `RogueGameScreen`/D-key gesture gone; new MarginScreen has no drop-D UX. |
| Pre-3.3 save loads a non-deterministic `identifyMap` binding [3.3] | Superseded: Story 1.1's `saveVersion` + AD-8 reject-by-absence gate (`SaveMigrationTest`) rejects pre-AD-8 saves — the field-absent case can never load. |
| Identify arrays not resized on cross-version save (enum growth) [3.4] | Superseded: the saveVersion gate rejects cross-version saves (reject, not resize). |
| No committed JUnit suite for the new model [Epic 4] | **DELIVERED**: carry #1 closed (JUnit 5 core root; 205 tests incl. FlagStoreTest/CompanionTest/IdentifyMapTest). |
| Debug `T` trigger + `SampleDialog` ship in build [Epic 5] | Old screen gone (T = craftTorch since 1.6); carry dropped the old trigger as old-design-specific. New-design equivalent: 2.1's smoke scene behind a debug key, superseded by 2.2 — **2.2 must remove the 2.1 debug key**. |
| Dialogue input caps choices at 4 [Epic 5] | Superseded: Story 2.1's surface maps `NUM_1..NUM_N` and Task 5 pins >4-option navigation (carry #5 hardening). |
| `SceneEffects` cache keys single-run-scoped (collision) [Epic 5] | Superseded: Story 2.1's key-namespacing hardening (Task 5, carry #5 item 4). |
| Bond-tag dialogue wiring absent [Epic 5] | Superseded: Story 2.1 Task 2's BOND effect calls `applyBondTag` (wiring now in 2.1). Residual doc-drift folded into O2. |
| Legacy `floorDepth` save loads soft-locked [6.1] | Floor descent removed by Story 1.1 (continuous map, AD-8). |
| Transient-route design landmine [6.1] | No route/floors in the continuous-map design. |
| Road-end turn burns hunger/Last-Stand, re-triggerable [6.1] | Descend branch removed by Story 1.1. |
| Legacy-deep-save HUD renders `5/3` [6.1] | Old screen + floor descent gone. |
| Stale-tile render of retired STAIRS values [1-1] | The AD-8 map guard rejects legacy maps at load — premise unreachable. |
| HUD labels for the survival tracks unwired [1-2] | **DELIVERED**: Story 1.8's minimal HUD renders all four tracks + clock/weather. |
| Driver-less temperature drift re-balancing [1-2] | **DELIVERED**: Story 1.6 added the weather/fire drivers (Cold Snap, campfire) the rates calibrate against. |
| Pre-1.3 save loads a nanoTime-rolled weather [1-3] | Superseded: AD-6 migration rule codified (retro action #4) + the saveVersion gate rejects pre-AD-8 saves — the field-absent case can never load. |
| Forward-compat: unknown weather string voids a run [1-3] | Superseded: the saveVersion read-branch now exists (reject-by-version); the residual enum-corruption case is the documented accepted catch-all (null → playable run). |
| `setLight` accepts off-map/wall/negative coords [1-4] | Resolved by construction through the 1.5→1.6 chain: campfire + torch light the player's own walkable tile; the `hasLight()` ≥ 0 guard closed the half-set −1 case. |
| Campfire/torch light-tile validation [1-5] | Same as above — resolved by construction (torch lit via craftTorch at the player's tile). |
| Torch light desyncs on `placeAt` repositioning [1-6] | Premise obsolete: no teleport/descent in the continuous-map design; `placeAt` is test-only. |
| Composite debuff query seam [1-7] | **DELIVERED**: Story 1.8's `getActiveDebuffLabels` (Task 3, 10 tests). |
| E-key quick-eat can auto-feed a poison mushroom [1-7] | **DELIVERED**: Story 1.8's backpack selection replaced `firstWhere` (F-09 closed). |

---

## Deferred from: code review of 2-1-text-forward-dialogue-nodes-with-safe-pause (2026-08-08)

- **Bond and disposition effects emit identical strings; disposition never names its NPC.** The controller's `bondLine` and `dispositionLine` both return "He warms to you."/"His eyes narrow." (SPD-tone placeholders, Decision 4's examples). No authored node stacks both effects today, so no collision in-game — but 2.2+ content with real NPC names will need distinct per-NPC disposition lines. [core/src/main/java/com/margins/rogue/narrative/DialogController.java]
- **N-key smoke scene re-fires node-entry effects on re-open** — closing and pressing N again rebuilds the scene and re-fires the Bond gain, the coal gift (an infinite item source behind a debug key), and the disposition shift. Acceptable for the throwaway verification seam (Decision 8 supersedes it), but the pattern — scene entry-effects re-firing on re-entry, with `KEY_SMOKE_READ` written but never read back — must NOT carry into 2.2's real intro authoring. [core/src/main/java/com/margins/rogue/narrative/SampleDialog.java]

---

## Deferred from: code review of 2-3-aldrics-diegetic-tutorial (2026-08-09)

- **SCAVENGE prompt teaches "forage grass/logs/rock" but `COLLECT` only gathers water from WELL/POND/RIVER.** A player pressing C in the forest (the natural reading of "forage") gets a no-turn refusal with no tutorial feedback. Only surfaced via the H1 observe-seam bug (now fixed — a refused C no longer acks SCAVENGE), so it is not blocking; reconcile the prompt wording with Story 2.4's foray/scavenging work or teach the water-collection reality. [core/src/main/java/com/margins/rogue/narrative/TutorialController.java]

---

## Deferred from: code review of 2-4-aldrics-capture-and-the-rescue-seed (2026-08-09)

- **N1 — A reloaded run re-arms the tutorial and can re-fire the capture.** Matches the story's deferred O6 save/load note (documented, not built); the capture stays one-shot per session. Record for the save/load story. [core/src/main/java/com/margins/MarginScreen.java]
- **N2 — `isResolved()` reads false after a post-capture reload (flag set, party empty).** Currently unused; the 2.5 Journal story should derive capture state from the persisted `aldric.captured` flag instead. [core/src/main/java/com/margins/rogue/narrative/CaptureController.java]
- **N3 — The reveal line is redundant (`"Torn Page: Chaser's order: …"`).** Cosmetic; the spec left the phrasing open. Tune in the 2.5 content pass. [core/src/main/java/com/margins/rogue/system/TurnEngine.java]
- **N4 — `markIdentified` on an unbound identity would silently lose the lore.** Unreachable (`TORN_PAGE` is single-identity); a comment documents the invariant. [core/src/main/java/com/margins/rogue/system/TurnEngine.java]
- **N5 — The `isConsumedOnUse` exclusion for `TORN_PAGE` is dead code.** The read branch intercepts first; kept as documentation. [core/src/main/java/com/margins/rogue/item/Supply.java]
- **N6 — The capture can append its beat on the same turn the player dies.** The beat is wiped by the post-death restart anyway; minor narrative ordering. [core/src/main/java/com/margins/MarginScreen.java]

## Deferred from: code review of 2-5-quest-flags-and-the-passive-journal (2026-08-09)

- **MED — the AD-14 safe-pause test is vacuous: no screen-level input-routing test for the Journal surface.** `openingAndClosingTheJournalTicksNothing` pins the controller contract (open/close mutates no run state) but not the screen's enforcing swallow branch, so it could not catch the HIGH input-routing bug review found (ESC/M opened the menu while the Journal was open). A screen-level Gdx input-routing test needs a headless Gdx backend or Mockito harness the repo doesn't have — the existing screen-layer test (`MarginScreenStructureLayerTest`) only exercises static pure functions. The routing defect itself is fixed and boot-verified; build the harness with the save/load or a test-infra story. [core/src/test/java/com/margins/rogue/narrative/JournalControllerTest.java]

---

## Deferred from: code review of 3-1-hybrid-map-generation (2026-08-09)

- **Supply placement dropped the retry loop; `want` is a ceiling, not a count.** A ±1 offset landing on a wall/structure edge silently drops the item (fewer supplies than the eastness gradient intends). Rare (room centers are walkable; offsets are interior); any fix (retry or deterministic fallback) touches the AD-5 seeded stream this story deliberately established — re-visited if Epic 6's economy makes supply counts load-bearing. [core/src/main/java/com/margins/rogue/state/RunState.java:179-188]
- **`eastness`/`dangerAt` unclamped for x outside `[0, width-1]`.** No current call site passes out-of-bounds x (room centers are in-bounds); WorldSpine is public API for 3.2/4.3/5.7. Add a clamp (or a constructor floor) when the first out-of-bounds caller appears. [core/src/main/java/com/margins/rogue/world/WorldSpine.java:65-71]
- **O4 connectivity guarantee scoped to structure entrances, not every interior tile.** The pre-existing sealed cellar nook (16,12) inside the Old House is exempt; `structureOpensOntoNetwork` pins the entrance contract and the dev record discloses the scope. Re-visit if the Old House cellar becomes explorable content (a future structure-interior story). [core/src/test/java/com/margins/rogue/HybridMapTest.java:169-220]

## Deferred from: code review of 3-2-the-11-world-structures-across-three-danger-tiers (2026-08-09)

- **Legacy-save hazard spillover: pre-`structureTypes` cells map to STRUCTURE_OLD_HOUSE.** `getStructureType` returns OLD_HOUSE for transition-era cells with a `structureTiles` entry but no type layer, so 3.2 newly applies the T3 STRUCTURAL_DECAY hazard (with no loot) on those cells. Pre-existing compat shim; same save-migration family as the open decision item (pre-3.2 saves load without authored loot). [core/src/main/java/com/margins/rogue/RogueTileMap.java:62]
- **Old House locked cellar has no spatial cell mapping.** Reachable loot scatters over every walkable cell in the footprint box (the narratively-locked cellar isn't spatially modeled), and the rich `lockedLoot` is never placed anywhere — Story 3.5's lockpicking owns both the spatial lock and the placement target. [core/src/main/java/com/margins/rogue/state/RunState.java:205]
- **count>1 loot entries can stack two items on the same cell** (OLD_HOUSE PRESERVED_FOOD×2; lockedLoot 3/2 when 3.5 exposes it). Cosmetic; an AD-5-safe no-replacement fix would change the seeded draw sequence this story deliberately established. [core/src/main/java/com/margins/rogue/state/RunState.java:222]
- **Authored loot can land on enemy-occupied cells** (the filter excludes only the player's tile). Unreachable until the enemy moves or dies — a minor annoyance, not a blocker. [core/src/main/java/com/margins/rogue/state/RunState.java:212]
- **AD-16 budget test is a coarse smoke gate** (600 acted turns under 10,000 ms) — an order of magnitude looser than a real budget, but it guards catastrophic regressions; the 3.1 `lootRisesEastWithTheDanger` stays green by the additive design. [core/src/test/java/com/margins/rogue/StructureContentTest.java]
- **Production `structureFootprint` and test `footprint` are byte-identical copies**, so the test is self-consistent with the implementation rather than an independent check. Common test pattern; low risk. [core/src/main/java/com/margins/rogue/state/RunState.java:231]
- **The Story 3.4 "override hazard on the night path" seam doesn't exist yet** — `HazardSystem.step` resolves `structure.hazard` directly with no night/weather parameter. Creating it now would be speculative code (CLAUDE.md §2); Story 3.4 owns the seam. [core/src/main/java/com/margins/rogue/system/HazardSystem.java:21]
- **`Supply.count() - 5` scatter-pool pin encodes an intentional AD-5/save-stability guard** that will need updating when a future story adds a non-scatterable item (3.3/3.5). The drift is the guard doing its job; update the constant then. [core/src/test/java/com/margins/rogue/StructureContentTest.java]
- **Task 4's "structure with no hazard is a no-op" test is impossible under current data** — every structure carries a real hazard and `Hazard.NONE` is unused (the `onStep` guard is dead code). Re-enable if a NONE-hazard structure is ever authored. [core/src/main/java/com/margins/rogue/world/StructureTable.java:42]
- **T3 loot value deliberately low for the home-cluster exceptions.** Authored expectations — T2 Kitchen Camp 3.1 / Watchtower 2.55 / Poacher's 2.7 / Sunken Well 2.7; T3 Old House 3.0 / Graveyard 1.5 / Deep Cave 1.9. The home-cluster T3s are near town and trap-heavy, so low value is thematic ("west 1.5–3" band holds); Deep Cave (T3-by-transition-depth at mid-east) is the one below its eastness band — tunable content (PRD §8), revisit when 3.4 or the economy lands.

## Deferred from: code review of 3-5-horizontal-progression-skill-and-knowledge (2026-08-10)

- **The "unifying query surface" is three APIs, not one call.** `KnowledgeSystem` exposes `mapFragmentsRead` + `locationNightHazard`; item-safety stays on `IdentifyMap`, so "what do I know?" composes three APIs (the tests compose them). AC-2's queryable requirement is met individually; a single `KnowledgeSystem.whatDoIKnow(state)` is future UI work — add it when a consumer (journal screen, tutorial) appears. [core/src/main/java/com/margins/rogue/system/KnowledgeSystem.java]
- **AC-3's map-fragment knowledge isn't independently demonstrated to help.** In the comparative test the fragment is read but the HP/provision delta is driven by water-identification + the cellar. The fragment's knowledge is foundation state with no consuming mechanic yet (later stories); a benefit test waits on that consumer. [core/src/test/java/com/margins/rogue/system/HorizontalProgressionTest.java]

## Deferred from: code review of story-4.1 (2026-08-12)

- **AG turn-order sort is inert in shipped content** — every spawned enemy is AG=3 (`setAg` never called in `main/`; `RunState.java:188` uses the default ctor). AC-1 machinery + ordering tests are correct, but the sort has no visible effect until enemies get varied AG (future story — enemy variety / 4.3). Track so it isn't forgotten. [CombatSystem.java:65]
- **`flee` ignores `tryMove`'s return** — the target tile is pre-validated (`isWalkable` + `isOccupiedByEnemy`) at selection, then `player.tryMove(...)` re-checks walkable and its boolean is discarded, yet "You flee!" logs unconditionally. Benign today (single-threaded turn, identical checks); harden by honoring the return if occupancy rules ever enter `tryMove`. [CombatSystem.java:133]
- **`evading` clear is not exception-safe** — `setEvading(false)` runs at `TurnEngine.java:298` after `enemyPhase`; a throw in any pipeline step between the DODGE action and that clear would orphan `evading=true` into the next turn (an unearned boosted dodge). Theoretical (pipeline systems don't throw in normal play); close with a try/finally or a turn-entry reset. [TurnEngine.java:298]
- **`combatDamageRoutesOnlyThroughCombatSystem` is a name-substring guard** — the AC-2 pin reflects over `TurnEngine` method names for HP-mutator substrings; it pins a naming convention, not the architectural invariant. Auditor confirmed via grep that no HP mutation exists in `TurnEngine` (so AC-2 genuinely holds), but the test gives false confidence. Harden to trace actual HP routing. [CombatActionsTest.java]

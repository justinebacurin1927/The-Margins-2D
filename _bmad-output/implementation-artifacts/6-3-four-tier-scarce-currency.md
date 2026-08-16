---
baseline_commit: 3d7dd1b
---

# Story 6.3: Four-tier scarce currency

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As Klein,
I want coin to be weighted and scarce,
so that money is never a shortcut to safety (FR-21, AD-17).

## Acceptance Criteria

**AC-1 — Four weighted currency tiers.**
**Given** currency **When** I hold it **Then** it uses four tiers (Copper → Silver **25:1** → Gold **10:1** → Royal Gold Plaque **1000:1**) and coin is weighted like any item (carrying it costs space).

**AC-2 — Scarce, no infinite-money loop, no mandatory sink (AD-17).**
**Given** the economy **When** it is played **Then** there is no infinite-money loop (no action mints coin; coin enters only through finite, authored world placement) and no coin sink is mandatory for survival (nothing about eating, drinking, curing, repairing, or crossing the border requires coin).

## Scope decisions (author, 2026-08-16)

- **D1 — Coins are four new first-class `Supply` types, stored as ordinary main-store stacks (mirror the existing item model; CLAUDE.md §2/§3).** Unlike bags/weapons, a coin carries **no** mutable per-item state (no durability, no identity gamble) — it is exactly a stackable, self-evident consumable-shaped token, which is what `Supply` already is. So the faithful, minimal move is to append `COPPER`, `SILVER`, `GOLD`, `ROYAL_GOLD_PLAQUE` to `Supply` (single-identity, self-evident — the name IS the real name, no `IdentifyMap` gamble), **appended LAST** so existing ordinals and old saves are byte-identical (AD-6). This makes "coin is weighted like any item" and "carrying it costs space" true **for free** — coins occupy main stacks, weigh via `Supply.weight()`, drop/spill via `addFloorItem`, and round-trip through `SaveService` with zero new plumbing (they are Supply ordinals, like every other stack). NO separate purse object, NO parallel currency store — that would duplicate the stack/weight/save machinery `Inventory` already owns (AD-3 single-owner).
- **D2 — The exchange ratios are a valuation model on `Supply`, not a transaction (AC-1).** `Supply.isCurrency()` + `Supply.copperValue()` encode the tiers as their worth in the base unit: **Copper = 1, Silver = 25, Gold = 250 (10×Silver), Royal Gold Plaque = 250 000 (1000×Gold)**. These live alongside the existing category methods (`isStorage`, `isProvision`, `drinkRisk`, …) — the established home for per-type data on `Supply`. Add `Inventory.walletValueInCopper()` (sum `count × copperValue()` over the main store, mirroring `totalWeight()`) as the single wealth query the HUD and the 6.4 traders read. **The 25:1/10:1/1000:1 ratios are the exchange *rates*; the act of exchanging (make-change, buy/sell) is a trader service — Story 6.4.** 6.3 delivers the *weighted, valued, scarce* currency; 6.4 spends it.
- **D3 — Coins weigh 1 each (the default), and that IS the tier incentive (AC-1 "weighted").** No `weight()` special-case is needed — coins fall through to the default `return 1`. The design pay-off: a pile of 25 Copper weighs 25 while the equal-value 1 Silver weighs 1, so **bulk low-tier coin is heavy and high tiers are dense** — the reason the tiers exist. (The player-facing *convert-up-to-shed-weight* action is a trader/consolidation service → deferred to 6.4; the weight it relieves is real in 6.3, just not yet actionable.) Coins are inert on USE (`isConsumedOnUse()` false, like `ROPE`/`MAP_FRAGMENT`) — using a coin is a no-op, not a consumption.
- **D4 — Coin enters only through finite, authored world placement (AC-2, AD-17 scarce; AD-5 byte-stable).** Coins are **not scatterable** (`isScatterable()` false — an authored economy item, never forest junk, so the generic scatter pool length is unchanged and every seed's wilderness stays byte-identical). Place a **small, finite** amount as authored loot, weighted **east/interior** (value rises east; scarce by design), drawn from a **seed-derived sub-stream** (`seed ^ CURRENCY_PLACEMENT_SALT`) exactly like 6.2's `placeFoundBags` — so the shared gameplay `rng`, and every seed-pinned determinism test (`NightWeatherHazardTest`, `StructureContentTest`), stays byte-identical. This is the whole of AC-2's "no infinite-money loop": no repeatable action mints coin; the world holds a bounded amount.
- **D5 — AC-2's "no mandatory sink" is satisfied by construction and pinned by a guard test.** There is no coin sink at all yet (traders are 6.4), and no survival/consumption/repair/border system reads or requires the wallet. The story adds a guard test asserting survival actions (eat/drink/cure) never change `walletValueInCopper()` and no survival gate consumes coin — so the invariant is captured *before* 6.4 introduces the first (optional) sink. **Deferred (→ 6.4 / polish):** the two mobile traders, buy-at-loss/sell-at-premium, make-change / voluntary consolidation, barter, and any coin HUD panel (a minimal wealth readout may ride 6.4's trade UI).

## Baseline (verify before adding)

- **`item/Supply` (the item taxonomy).** All per-type data lives here as category/value methods (`weight()`, `isStorage()`, `isProvision()`, `drinkRisk()`, `isScatterable()`, `isConsumedOnUse()`). Coins are appended LAST (after `TRAVELERS_PACK`) as single-identity self-evident types; add `isCurrency()` + `copperValue()` here. Precedent for "appended-last, self-evident, non-scatterable authored item": the Story 3.2 structure-loot items and 6.1's `TRAVELERS_PACK` (Supply.java:55-72, :95-135, :256-289).
- **`item/Inventory` (Story 6.1/6.2).** Main store is `types[]`/`counts[]` stacks physically sized to `MAX_MAIN_SLOTS`; `totalWeight()` (Inventory.java:336-345) already sums `count × weightOf()` over the store — coins ride this untouched. Add `walletValueInCopper()` mirroring `totalWeight()`'s main-store iteration. `tryAdd`/stacking already handle any Supply ordinal, so coins stack with no change.
- **`state/RunState` (placement).** `placeFoundBags` (RunState.java:193-199) is the exact pattern to mirror: a seed-derived sub-stream (`new Random(seed ^ BAG_PLACEMENT_SALT)`, salt at :199) feeding `placeLootInFootprint(structure, entries, avoidX, avoidY, rng)` (:294) as the **final** generation pass in `generateFloor` (after `placeStructureLoot` :178 and `placeFoundBags` :185). Add `placeCurrency` with its own `CURRENCY_PLACEMENT_SALT`, called last. `addFloorItem` (:678) is the drop/spill sink coins reuse for free.
- **`world/StructureTable`.** `LootEntry`/`loot(supply,count,chance)` (StructureTable.java:113-125) is the loot-row model; `FOUND_BAG_LOOT`/`FOUND_BAG_CHANCE` (:132-133) is the standalone authored-entry precedent. Add a small `CURRENCY_LOOT` set (finite counts, east-weighted structure(s), e.g. Mercenary Graveyard / Old House / Deep Cave — the eastern/interior T3s).
- **`save/SaveService` + persistence.** Coins are Supply ordinals in `types[]`/`counts[]` — **no new element-type registration** (that was needed for the `Bag` object list, not for ordinal stacks). A pre-6.3 save simply has no coin stacks; loading is unchanged (AD-6, additive enum). Confirm the round-trip in `RunStatePersistenceTest`.
- **Tests to keep green / update** — `RunStatePersistenceTest` (add a coin-in-purse round-trip assertion), `StructureContentTest` (update authored-loot invariants ONLY if it asserts a total loot count; the seed-derived sub-stream keeps the wilderness scatter byte-identical), `SurvivalTickTest` / `ConsumptionSystem` tests (unchanged — the D5 guard test asserts survival never touches the wallet). Suite is at **547**.

## Tasks / Subtasks

- [x] **Task 1 — The four coin `Supply` types + valuation model (AC-1, D1/D2/D3).**
  - [x] 1.1 Appended `COPPER`, `SILVER`, `GOLD`, `ROYAL_GOLD_PLAQUE` to `Supply` LAST (single-identity self-evident; each with a self-evident `TrueIdentity.*_ID` inert-on-use identity, like `SALT`/`WOOD`) — existing ordinals unchanged (AD-6).
  - [x] 1.2 `Supply.isCurrency()` (true for the four) + `Supply.copperValue()` (Copper 1 / Silver 25 / Gold 250 / Royal Gold Plaque 250 000; 0 for non-coin). Encoded via the ratio chain (`25`, `25*10`, `25*10*1000`) so the constants are self-documenting.
  - [x] 1.3 Coins are inert: `isConsumedOnUse()` returns false via `!isCurrency()` (USE is a no-op); `weight()` unchanged (coins fall through to default 1 — the tier weight incentive); `isScatterable()` false via `!isCurrency()` (authored-only, AD-5).
  - [x] 1.4 `Inventory.walletValueInCopper()` — sums `counts[i] × Supply.byOrdinal(types[i]).copperValue()` over the main store (mirrors `totalWeight()`; null-safe; `long` to hold high-tier value).
- [x] **Task 2 — Finite, scarce, authored world placement (AC-2, D4; AD-5).**
  - [x] 2.1 `StructureTable.CURRENCY_LOOT` — a small finite scarce set (1 Copper @100, 1 Silver @30, 1 Gold @6 per eastern footprint; Royal Gold Plaque never world-loot).
  - [x] 2.2 `RunState.placeCurrency(avoidX, avoidY)` — a **seed-derived sub-stream** (`new Random(seed ^ CURRENCY_PLACEMENT_SALT)`, salt `0xC0FFEE`, distinct from the bag salt) feeding `placeLootInFootprint(...)` for every structure whose footprint is **east of midline**; called as the **final** pass in `generateFloor`, after `placeFoundBags`. Shared `rng` untouched → seeds byte-identical.
- [x] **Task 3 — Scarcity invariants (AC-2, D5).**
  - [x] 3.1 No action mints coin: coin appears only in `placeCurrency` (the one-shot generation pass) and enters the backpack only via the existing floor-pickup path (handles any Supply). No survival/consumption/craft/repair path grants coin. Pinned by `CurrencyTest.theWorldHoldsAFiniteBoundedAmountOfCoin` (bounded) + `survivalNeverReadsOrMintsTheWallet`.
  - [x] 3.2 No mandatory sink: nothing in survival/consumption/border reads `walletValueInCopper()` — the wallet is inert to survival in 6.3. Pinned by `CurrencyTest.survivalNeverReadsOrMintsTheWallet` (eating leaves wealth untouched; consuming a coin is a no-op that grants no nourishment).
- [x] **Task 4 — Tests + verification (all ACs).**
  - [x] 4.1 AC-1: `CurrencyTest` (new) — the four `copperValue()`s (1/25/250/250 000) and the 25:1/10:1/1000:1 chain; `isCurrency()`; each coin `weight()==1` + the weight incentive (25 Copper weighs 25 vs 1 Silver weighs 1 at equal worth); a mixed purse's `walletValueInCopper()` (3 Copper + 2 Silver + 1 Gold = 303); coins stack in one main slot.
  - [x] 4.2 AC-2 scarcity: coin `isScatterable()==false` (and absent from `scatterableOrdinals()`); `placeCurrency` places a **finite bounded** amount (`> 0` reachable, `<= 33`) from a fixed seed; two runs of the same seed place identical coin (determinism); the guard test above (no-mandatory-sink).
  - [x] 4.3 Persistence (AD-6): `RunStatePersistenceTest.aMixedCoinPurseSurvivesRoundTrip` — a purse of mixed coin round-trips through save/load byte-for-value (no new element-type registration; coins are ordinal stacks).
  - [x] 4.4 Determinism (AD-5): `HybridMapTest.lootRisesEastWithTheDanger` holds (coin lands east only); `StructureContentTest` snapshot + scatter-pool invariant updated for the additive coin (pre-6.3 items byte-identical). Full suite **547 → 556 green**; `mvn -o -pl core test` + `mvn -o compile` (core + desktop) clean.

## Review Findings (code-review 2026-08-16 — 3-layer parallel: Blind Hunter, Edge-Case Hunter, Acceptance Auditor)

Auditor verdict: **both ACs satisfied and reachable**; all five scope decisions (D1–D5) and cross-cutting constraints (AD-5/AD-6, non-scatterable, transactions-deferred) upheld. The one substantive finding (Blind H1) is a documentation/test-accuracy defect, not a functional one: the placement gate is sound but several comments/tests mislabeled it.

### Patch (all applied)
- [x] [Review][Patch, FIXED] Docs & the bound test falsely claimed coin lands in "the three Tier-3 footprints (Deep Cave …)". The gate is a pure GEOMETRIC east-of-mid (`midX = width/2 = 48`), which actually **excludes** every Tier-3 (Deep Cave center ≈44, Old House ≈12, Graveyard ≈19 all fall west) and places coin in the eastern mid-tier camps (Kitchen Camp/Watchtower/Poacher's/Sunken Well, x 57–81). Functionally correct (scarce, value-rises-east, `lootRisesEast` green) but the wording lied. [`RunState.placeCurrency`, `StructureTable.CURRENCY_LOOT`, `CurrencyTest`] — **Fixed:** reworded both production comments to describe the geometric east-gate honestly (not a tier selection; the near-mid Deep Cave + western cluster stay coinless); rewrote `theWorldHoldsAFiniteBoundedAmountOfCoin` to derive the ceiling from the code (`StructureTable.all().length × CURRENCY_LOOT.length`) and **sweep 300 seeds** (reachable `>0` AND bounded), replacing the magic `<=12` / "2 Copper × 3 Tier-3" rationale; fixed the assertion messages. (blind+auditor)
- [x] [Review][Patch, FIXED] `centerX == midX` got coin though the prose said "east of the midline". [`RunState.placeCurrency`, `StructureTable`] — **Fixed:** prose now reads "at or east of the map midline", matching the `< midX` skip. (edge)
- [x] [Review][Patch, FIXED] `isCurrency()`/`copperValue()` coupling was untested — a future coin tier appended without a `copperValue()` case would silently value 0 and undercount the wallet. [`CurrencyTest`] — **Fixed:** added `everyCurrencyTierHasAPositiveCopperValue` (every `isCurrency()` type has `copperValue() > 0`; every non-currency is 0). (edge)
- [x] [Review][Patch, FIXED] "Guaranteed reachable coin" was asserted only for seed 42. [`CurrencyTest`] — **Fixed:** the reworked bound test sweeps 300 seeds asserting `coins > 0` (empirically min 4, max 9, zero-runs 0 / 3000 in a scratch scan — the canonical map always stamps eastern structures). (blind)

### Deferred
- [x] [Review][Defer] `Supply.copperValue()` returns `int` and stack counts are unbounded — once Story 6.4 mints Royal Gold Plaques (250 000 each) and a trader does `int price = count × copperValue()`, that caller could overflow `int`. Not reachable in 6.3 (no plaque is ever minted; `walletValueInCopper` already accumulates in `long` with a pre-multiply cast). [`Supply.copperValue`, `Inventory.walletValueInCopper`] — harden before 6.4 circulates plaques (return `long` and/or cap stack counts). (blind M1/L4)
- [x] [Review][Defer] `placeCurrency` gates on the footprint bounding-box center `(box[0]+box[2])/2`, a geometric proxy for the authored landmark X. Correct for the current well-separated landmarks; fragile if a future map change moves a footprint across the midline. [`RunState.placeCurrency`] — revisit if map geometry changes (gate on authored landmark X or Tier). (blind M4)
- [x] [Review][Defer] The currency sub-stream uses a raw `new Random(seed ^ CURRENCY_PLACEMENT_SALT)` with no cold-start drain, so for adjacent small debug seeds the low bits are XOR-correlated with the main/bag streams. Pre-existing — the found-bag stream (`BAG_PLACEMENT_SALT`) has the identical shape; determinism and the byte-identical snapshot tests pass. [`RunState.placeCurrency`] — fold into a shared seeded-sub-stream helper if `seededRng`'s cold-start skip is ever generalized. (blind L3)

### Dismissed (3)
Zero-coin run (edge, "High") — FALSE POSITIVE on reachability: a scratch scan of 3000 seeds placed 4–9 coins every run (zero-runs = 0); the canonical map always stamps eastern structures, and the 300-seed sweep now guards it. `walletValueInCopper` counting coin in over-capacity slots (blind M2) — by design: it mirrors `totalWeight()`'s full-array iteration exactly, so wealth and weight agree on held-vs-spilled coin (the correct invariant). No dedicated shared-`rng` byte-stability assertion for currency (auditor, Low) — the established pattern here: `StructureContentTest`'s frozen seed-42 snapshot verifies every pre-6.3 item is byte-identical with coins appended, which IS the byte-stability assertion.

## Dev Notes

- **FR-21 / AD-17 economy spine.** Coin is the surplus-to-readiness converter that is *always worse than foraging*: it is weighted (a pile is dead carry-weight), scarce (a bounded amount exists in the world, no mint), and — in 6.3 — has no sink at all. The tiers exist so value can be carried densely; the exchange that realizes that density is the trader (6.4). Keep 6.3 to the *weighted, valued, scarce* model — do NOT build trading here.
- **Mirror, don't reinvent (CLAUDE.md §2/§3).** Coins are `Supply` stacks — they reuse stacking (`tryAdd`), weight (`totalWeight`/`weight()`), drop/spill (`addFloorItem`), and save (Supply ordinals) with **zero** new plumbing. Valuation is two methods on `Supply` + one query on `Inventory`. Placement mirrors 6.2's `placeFoundBags` seed-derived sub-stream verbatim. No purse object, no currency system, no transaction engine (that is 6.4).
- **AD-6 (additive, no migration).** Unlike 6.2's `int[]→List<Bag>` refactor, coins are purely additive enum values appended last — existing ordinals and pre-6.3 saves are byte-identical, and there is nothing to migrate (a pre-6.3 save simply has no coin stacks). No `SaveService` element-type registration (coins are ordinals, not an object list).
- **AD-5 determinism (the load-bearing decision).** Reuse 6.2's proven pattern: coin placement draws from `seed ^ CURRENCY_PLACEMENT_SALT`, NOT the shared gameplay `rng`, and coins are non-scatterable — so the wilderness scatter pool length and every seed-pinned determinism test stay byte-identical (no snapshot churn). Append the enum values LAST (the Epic-5 retro "append new seeded draws LAST" rule, applied to ordinals). Pick a fresh salt distinct from `BAG_PLACEMENT_SALT` (0x6A6BAD).
- **AC-2 is a *property*, tested as a property.** "No infinite-money loop" = no repeatable action produces coin (coin exists only as finite authored placement) + "no mandatory sink" = survival never reads the wallet. Both are guard tests (Task 4.2), pinned now so 6.4's first sink can't silently make coin load-bearing for survival.
- **Reachability (Epic-5 retro + the 6.1/6.2 findings).** Coin must be genuinely obtainable in real play, not test-only: `placeCurrency` must run in `generateFloor` and land coin in a reachable structure footprint (verify a coin can actually be picked up in-game, as 6.2 did for trapped bags). Task 2 is not optional.
- **Weight incentive is real now, actionable in 6.4.** The convert-up-to-shed-weight pay-off exists in 6.3 (bulk copper is heavy) but is only *actionable* once a trader/consolidation service lets the player exchange — that action is 6.4. Do not add a free player-facing convert action in 6.3 (it invents an interaction path the story doesn't call for; keep scope minimal).
- **Observation discipline (Epic-1/1.8 lesson, re-flagged through Epic 6).** Coin pickups already log via the floor-pickup path; ensure a coin pickup reads sensibly in the message log (name + count). No silent wallet mutation.
- **Build/verify:** `docs/BUILD.md` — `mvn -o -pl core install` before any `exec:java`; `mvn -o -pl core test` + `mvn -o compile` for the story gate.

### Project Structure Notes

- **New:** `core/src/test/java/com/margins/rogue/item/CurrencyTest.java` (AC-1/AC-2).
- **Edits:** `item/Supply.java` (append 4 coin types; `isCurrency`/`copperValue`; inert-on-use + non-scatterable for coins), `item/Inventory.java` (`walletValueInCopper()`), `state/RunState.java` (`placeCurrency` + `CURRENCY_PLACEMENT_SALT`, called last in `generateFloor`), `world/StructureTable.java` (`CURRENCY_LOOT`).
- **Updated tests:** `RunStatePersistenceTest` (coin-purse round-trip), `StructureContentTest` (only if a total authored-loot-count invariant exists).
- No new class in the production `system/` package (no CurrencySystem — valuation is pure data on `Supply`/`Inventory`; there is no per-turn currency behavior in 6.3). No `SaveService` change.

### References

- [Source: _bmad-output/planning-artifacts/epics.md#Story 6.3: Four-tier scarce currency] — AC-1/AC-2, FR-21, AD-17.
- [Source: _bmad-output/planning-artifacts/epics.md#Epic 6: Inventory, Currency & Economy] — the scarce-economy spine (AD-17: traders convert surplus into readiness, never safety).
- [Source: core/src/main/java/com/margins/rogue/item/Supply.java] — the item taxonomy + append-last / self-evident / non-scatterable precedents (3.2 loot, 6.1 `TRAVELERS_PACK`); `weight()`, `isConsumedOnUse()`, `isScatterable()`.
- [Source: core/src/main/java/com/margins/rogue/item/Inventory.java#totalWeight] — the main-store sum `walletValueInCopper` mirrors.
- [Source: core/src/main/java/com/margins/rogue/state/RunState.java#placeFoundBags] — the seed-derived sub-stream authored-placement pattern (`seed ^ SALT`) `placeCurrency` mirrors for AD-5 byte-stability.
- [Source: core/src/main/java/com/margins/rogue/world/StructureTable.java#FOUND_BAG_LOOT] — the standalone `LootEntry[]` authored-entry precedent.
- [Source: _bmad-output/implementation-artifacts/6-2-bag-durability-and-thematic-traps.md] — the AD-5 seed-derived sub-stream decision (BAG_PLACEMENT_SALT) and the "append new seeded draws LAST" rule this story reuses.

## Dev Agent Record

### Agent Model Used

Claude Opus 4.8 (1M context) — create-story 2026-08-16.

### Debug Log References

- `mvn -o -pl core test` — 556 tests, 0 failures (suite 547 → 556).
- `mvn -o compile` (core + desktop) — clean.

### Completion Notes List

- **AC-1 met.** Four new `Supply` tiers (`COPPER/SILVER/GOLD/ROYAL_GOLD_PLAQUE`) appended LAST — ordinary main-store stacks, so weight/stacking/drop/save came for free (no purse object). `Supply.copperValue()` encodes the exchange rates (1 / 25 / 250 / 250 000 = 25:1 / 10:1 / 1000:1); `Inventory.walletValueInCopper()` is the single wealth query (a `long`, sums the main store). Coins weigh 1 (default) → bulk low-tier is heavy while high tiers are dense, the tier incentive (pinned: 25 Copper weighs 25 vs 1 Silver weighs 1 at equal worth).
- **AC-2 met.** Coin enters only via `RunState.placeCurrency` — a bounded, one-shot generation pass; no action mints coin, and coins are non-scatterable (never generic forest junk). No mandatory sink: nothing in survival reads the wallet — eating leaves wealth untouched and consuming a coin is a no-op that grants no nourishment (guard-tested). Traders (6.4) are the first, optional sink.
- **Design refinement of D4 (placement location).** The story targeted "the eastern/interior Tier-3 footprints (Old House / Graveyard / Deep Cave)", but the Old House and Graveyard are the **home-cluster WEST** T3-by-hazard exceptions — placing coin there added western loot and broke `HybridMapTest.lootRisesEastWithTheDanger` (an AD-17 "value rises east" invariant). Corrected to an **east-of-midline gate**: `placeCurrency` iterates all structures and places `CURRENCY_LOOT` only where the footprint center is east of `midX`. This self-corrects (the two western T3s are skipped; Deep Cave + the eastern monotone-core structures carry the coin), keeps coin scarce and eastern, and made the loot-rises-east invariant hold comfortably. Coin `CURRENCY_LOOT` counts trimmed to 1 Copper / 1 Silver / 1 Gold per eastern footprint to stay scarce (AD-17).
- **AD-5 determinism (key decision, reused from 6.2).** `placeCurrency` draws from a seed-derived sub-stream (`seed ^ CURRENCY_PLACEMENT_SALT`, `0xC0FFEE`) distinct from both the shared gameplay `rng` and the found-bag sub-stream — so every pre-6.3 seed's structure loot, cordon, weather, found bags, and runtime hazard rolls stay byte-identical. `StructureContentTest`'s frozen seed-42 layout changed only by the additive COPPER entries (every prior item byte-identical, all coins east x≥56); the scatter-pool invariant went `count()-6` → `count()-10` for the 4 non-scatterable coins.
- **AD-6 (additive, no migration).** Coins are appended enum values — existing ordinals and pre-6.3 saves are byte-identical, nothing to migrate, no `SaveService` change (coins are ordinal stacks, not an object list). Coin purse round-trips (`RunStatePersistenceTest.aMixedCoinPurseSurvivesRoundTrip`).
- **Deferred (unchanged):** the two mobile traders, buy-at-loss/sell-at-premium, make-change / voluntary consolidation, barter, and any coin HUD panel — all Story 6.4.

### File List

- `core/src/main/java/com/margins/rogue/item/Supply.java` — appended the 4 coin tiers; `isCurrency()` + `copperValue()`; coins inert-on-use + non-scatterable (via `!isCurrency()`).
- `core/src/main/java/com/margins/rogue/item/TrueIdentity.java` — 4 self-evident inert-on-use coin identities (`COPPER_ID`/`SILVER_ID`/`GOLD_ID`/`ROYAL_GOLD_PLAQUE_ID`).
- `core/src/main/java/com/margins/rogue/item/Inventory.java` — `walletValueInCopper()` (main-store wealth sum, mirrors `totalWeight()`).
- `core/src/main/java/com/margins/rogue/world/StructureTable.java` — `CURRENCY_LOOT` (scarce standalone set, like `FOUND_BAG_LOOT`).
- `core/src/main/java/com/margins/rogue/state/RunState.java` — `placeCurrency` (east-of-midline gate, seed-derived sub-stream) + `CURRENCY_PLACEMENT_SALT`; called last in `generateFloor`.
- `core/src/test/java/com/margins/rogue/item/CurrencyTest.java` — **new** (AC-1 + AC-2; 9 tests after review: reworked bound test sweeps 300 seeds + derives the ceiling from the code; added the `copperValue()`-coupling guard).
- `core/src/test/java/com/margins/rogue/state/RunStatePersistenceTest.java` — coin-purse round-trip test.
- `core/src/test/java/com/margins/rogue/StructureContentTest.java` — updated the seed-42 snapshot (additive COPPER) + the scatter-pool invariant (`-6` → `-10`).

## Change Log

- 2026-08-16 — created by create-story. Decisions: D1 four coins as `Supply` stacks appended LAST (reuse stack/weight/drop/save; no purse object, AD-3/AD-6); D2 exchange ratios as a valuation model (`isCurrency`/`copperValue` on `Supply` + `Inventory.walletValueInCopper`), transactions deferred to 6.4; D3 coins weigh 1 (default) so bulk low-tier is heavy / high tiers dense — the tier incentive, inert on USE; D4 finite east-weighted authored placement via a seed-derived sub-stream (`seed ^ CURRENCY_PLACEMENT_SALT`), non-scatterable → seeds byte-identical (AD-5); D5 no mandatory sink (survival never reads the wallet), pinned by a guard test. Bounded: traders, buy/sell, make-change/consolidation, barter, coin HUD panel all deferred to 6.4. Status → ready-for-dev.
- 2026-08-16 — code-review (3-layer parallel: Blind Hunter, Edge-Case Hunter, Acceptance Auditor). Auditor: both ACs satisfied + reachable, all D1–D5 + AD-5/AD-6 upheld, coverage complete. 0 decision-needed, 4 patch (all applied), 3 defer, 3 dismissed. **Patches:** reworded the placement docs + rewrote the bound test to stop falsely claiming "Tier-3/Deep Cave" (the gate is a geometric east-of-mid over all structures → eastern mid-tier camps) and to derive the ceiling from the code + sweep 300 seeds; aligned the `centerX==midX` "at or east of" prose; added a `copperValue()`-coupling guard test; broadened reachability to a 300-seed sweep. Dismissed the zero-coin "High" (scratch scan: 4–9 coins every run over 3000 seeds, zero-runs 0) and the over-capacity-slot wallet count (mirrors `totalWeight` by design). Suite 556 → 557 green; core + desktop compile clean. Status → done.
- 2026-08-16 — dev-story (dev). Implemented both ACs. Coins are `Supply` stacks (weight/stack/drop/save free); `copperValue` encodes 25:1/10:1/1000:1; `Inventory.walletValueInCopper` is the wealth query. Refined D4: the story's "3 Tier-3 footprints" was wrong (Old House + Graveyard are the home-cluster WEST exceptions) — corrected to an east-of-midline gate so coin lands only east (fixes the AD-17 loot-rises-east invariant); trimmed `CURRENCY_LOOT` to 1/1/1 for scarcity. AD-5: currency draws from its own seed-derived sub-stream (`0xC0FFEE`), so all prior seeds byte-identical; updated the seed-42 snapshot (additive COPPER) + the scatter-pool invariant (`-6`→`-10`). AD-6: additive enum, no migration, no `SaveService` change. Suite 547 → 556 green; core + desktop compile clean. Status → review.

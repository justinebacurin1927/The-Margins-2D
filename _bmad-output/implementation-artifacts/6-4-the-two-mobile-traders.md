---
baseline_commit: ada183f
---

# Story 6.4: The two mobile traders

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As Klein,
I want two mobile traders as the only coin sinks,
so that trade converts surplus into readiness, never into safety (FR-21, AD-17).

## Acceptance Criteria

**AC-1 — The Traveling Wanderer (coin OR barter, Copper/Silver tier).**
**Given** the Traveling Wanderer **When** I meet him **Then** I can trade by coin **or** barter (Copper/Silver tier), keeping non-coin players unblocked.

**AC-2 — The Caravan Black Market Trader (coin-only, Gold tier, guarded, killable-lockout).**
**Given** the Caravan Black Market Trader **When** I meet him **Then** he is coin-only (Gold-tier), guarded, and killing him permanently locks out that trade.

**AC-3 — Scarcity holds: buy at a loss, sell at a premium, no fixed shop (AD-17).**
**Given** either trader **When** I transact **Then** he buys at a loss and sells at a premium (scarcity holds) — there is no fixed shop, and no trade is required for survival, so coin never becomes a shortcut to safety.

## Scope decisions (author, 2026-08-16)

- **D1 — Two first-class `Trader` tile-agents (headless model; mirror the positioned-agent precedent).** New headless `world/Trader` (AD-1/AD-2, no libGDX): `TraderKind {WANDERER, BLACK_MARKET}`, `tileX`/`tileY`, `alive`, no-arg ctor + plain fields for libGDX Json (mirrors `RogueEnemy`/`Companion`). They are **stationary per-seed points** you walk up to — the "mobile / no fixed shop" flavor (AC-3) is that they are placed points on the open map, not shop buildings; actual roaming/patrol AI is **out of scope** (deferred). Held in a `RunState List<Trader> traders`, serialized under the save root with `SaveService.setElementType(RunState.class,"traders",Trader.class)` (the `enemies`/`companions`/`weapons` registration precedent, AD-6).
- **D2 — `system/TradeSystem` owns pricing with a scarcity spread (AC-3, AD-17).** Prices are in Copper, derived from an authored per-item base value. The player **buys at a premium** (`priceToBuy = base × SELL_NUM/SELL_DEN`, > base) and **sells at a loss** (`priceToSell = base × BUY_NUM/BUY_DEN`, < base). The spread is strict (`priceToBuy > base > priceToSell`), so a sell→buy-back round-trip always loses coin — no infinite-money loop, and the trader is always worse than foraging (AD-17). Integer math only (no floats; round deterministically). This is the single authority over a transaction; `MarginScreen` only presents it (AD-1).
- **D3 — Coin spend + "make change" reuses the 6.3 currency, and circulates the Royal Gold Plaque (closes a 6.3 deferral).** A purchase costs `price` Copper-equivalent. `TradeSystem` checks `Inventory.walletValueInCopper() >= price`, then **spends greedily** (consume highest-tier coins that fit, then hand back change in lower tiers) — this is where the top tier (Royal Gold Plaque) legitimately enters circulation as **trader change** (the 6.3 "trader gives change" note). **Because plaques now circulate, this story must also resolve 6.3's deferred `int`→`long` hardening** (`Supply.copperValue()` → `long`, and any trader price math in `long`) so a large-value trade cannot overflow. A `TradeSystem.makeChange(inventory, priceCopper)` helper (deterministic greedy: Plaque→Gold→Silver→Copper) is the spend path; barter (D4) is the coinless alternative.
- **D4 — Barter keeps the coinless player unblocked (AC-1, Wanderer only).** The Wanderer accepts a **non-coin item** in trade: a bartered item covers a stock purchase when its trade value (its authored base, at the sell-to-trader rate) meets the price. Minimal implementation: `TradeSystem.barter(inventory, offeredType, wantedStockType)` swaps an accepted surplus item for a stock item of `≤` its sell value — a coinless player can always convert forage surplus into one readiness item. The Black Market is **coin-only** (AC-2): `barter` refused for `BLACK_MARKET`.
- **D5 — Guarded + kill-lockout for the Black Market (AC-2).** The Black Market Trader is a **killable agent**: an `ATTACK` into his tile routes through `CombatSystem` (the single HP authority, AD-4), damaging the trader; on his death set `FlagStore.KEY_BLACK_MARKET_DEAD` → `TradeSystem` refuses him forever (mirrors Story 2.5 "killing a quest-giver voids that quest"). **Guarded:** place a few `RogueEnemy` guards adjacent at placement (reuse the cordon/`placeFoundBags` pattern). The Wanderer is not guarded and not specially protected (killing him just removes a trade; no special lockout beyond `alive`).
- **D6 — Interaction: a new `PlayerAction.TRADE` opens a safe-pause trade surface (reuse the DialogController pattern).** When Klein is adjacent to a living, reachable trader and presses the trade key, a paused trade surface opens (AD-14 safe-pause: no survival tick, no turn committed while it is open — the `DialogController.isActive()`/`JournalController` precedent). The headless `TradeSystem` owns buy/sell/barter; `MarginScreen` renders a minimal numbered-choice panel. **Bounded:** the CORE deliverable is the headless `Trader` + `TradeSystem` + placement + kill-lockout, fully unit-tested; the `MarginScreen` panel is a **minimal functional** surface (reuse the paged/numbered menu), not a bespoke shop UI. **Deferred (→ polish / later):** trader **roaming/mobility**, a full bespoke trade UI, a large stock **catalog** (ship a small authored stock per trader), trader restock/economy simulation, and price haggling (VOICE-gated discounts).

## Baseline (verify before adding)

- **`item/Supply` (Story 6.3).** `isCurrency()`/`copperValue()` (1/25/250/250 000), `walletValueInCopper()`. **6.4 changes `copperValue()` `int`→`long`** (D3 — plaques circulate) and adds an authored per-tradable **base value** (`tradeValue()` or a small `TradeSystem` price table) for the stock items. Coins already stack/weigh/drop/save.
- **`item/Inventory` (6.1–6.3).** `tryAdd`/`remove`/`count`/`drop`/`walletValueInCopper`. The trade transaction moves items and coin **only** through these (no direct array access); `remove(type, amount)` + `tryAdd(type, amount)` are the spend/receive primitives. `makeChange` composes them.
- **`RogueEnemy` (killable tile-agent) + `state/RunState` placement.** `Trader` mirrors `RogueEnemy`'s plain-field/no-arg shape. Placement mirrors `placeFoundBags`/`placeCurrency`: a seed-derived sub-stream (`seed ^ TRADER_PLACEMENT_SALT`, a fresh salt distinct from `0x6A6BAD`/`0xC0FFEE`) drawn **LAST** in `generateFloor` so every pre-6.4 seed stays byte-identical (AD-5). Wanderer placed west/mid (reachable by a coinless early player); Black Market placed east + guarded (Gold-tier danger). Traders are non-scatterable authored content.
- **`system/CombatSystem` (AD-4 single HP authority).** The `ATTACK`-into-a-trader-tile path hooks here (D5): recognize a `Trader` at the struck tile, damage him, and on death fire the lockout flag. Do **not** add a second HP owner. Keep the dead-before-acts / acted-branch pipeline invariants (AD-4).
- **`state/FlagStore` (AD-7).** Add `KEY_BLACK_MARKET_DEAD` (a lockout flag, like `KEY_WON`). `get`/`set` are the API. Persisted state, so the lockout survives save/load.
- **`system/PlayerAction` + `narrative/DialogController` (AD-14 safe-pause).** Add `Kind.TRADE` (adjacent-trader trigger, like `LOCKPICK` — no `itemType`). The paused surface follows the `isActive()` safe-pause contract (no turn ticks while open). `MarginScreen` input binds an unused key to `PlayerAction.trade(facing)`.
- **`save/SaveService`.** Register `Trader` for `RunState.traders` (like `weapons`/`companions`). A pre-6.4 save has no `traders` list → loads empty-but-non-null (field-initialized, AD-6); no migration needed (additive).
- **Tests to keep green / update** — `RunStatePersistenceTest` (traders + `KEY_BLACK_MARKET_DEAD` round-trip; the test serializer registers `Trader`), `StructureContentTest` (only if a total-actor/placement invariant counts traders — the seed-derived sub-stream keeps the shared rng byte-identical), `HybridMapTest` (the two authored trader agents append last, must not tip enemy east/west invariants — guards are enemies placed east with the Black Market, consistent with danger-rises-east). Suite is at **557**.

## Tasks / Subtasks

- [x] **Task 1 — The `Trader` model + `Trader.Kind` (AC-1/AC-2, D1).**
  - [x] 1.1 New `world/Trader`: nested `Kind {WANDERER, BLACK_MARKET}`, `tileX`/`tileY`, `hp`/`maxHp`, `alive`, no-arg ctor + plain fields (Json), `takeDamage`/`isAlive`/`isAdjacentTo`/`acceptsBarter()` (WANDERER only)/getters. **Stationary** (no map field — roaming deferred), mirrors `RogueEnemy`.
  - [x] 1.2 `RunState List<Trader> traders` (field-initialized) + `getTraders()` + `traderAt(x,y)`; `SaveService` registers `Trader` for `RunState.traders`; a pre-6.4 save loads an empty list (AD-6).
- [x] **Task 2 — `Supply` value hardening + tradable base values (AC-3, D2/D3).**
  - [x] 2.1 `Supply.copperValue()` `int`→`long` (plaques circulate as change now — closes the 6.3 defer); `walletValueInCopper` already `long`, cast preserved.
  - [x] 2.2 Authored per-item base Copper value in `TradeSystem.baseValue(Supply)` (a small price table: cure 30, pack 40, food 12, tools 10, rope 8, coins = `copperValue`, default 4) — kept off `Supply` to hold the pricing in the economy system.
- [x] **Task 3 — `system/TradeSystem`: pricing spread + spend/make-change (AC-3, D2/D3).**
  - [x] 3.1 `priceToBuy(base) = base×2` (premium) and `priceToSell(base) = base/2` (loss); strict spread `priceToBuy > base > priceToSell` for base ≥ 2 (`long`).
  - [x] 3.2 `makeChange(inventory, priceCopper)`: check `walletValueInCopper() >= price`; take coins **low→high** until covered, refund the overshoot as change **high→low** (Royal Gold Plaque circulates as change). Value-conserving; moves coin only via `Inventory.remove`/`tryAdd`; no mutation when too poor.
  - [x] 3.3 `buy(state, trader, stockType)` and `sell(state, trader, offeredType)` — refuse if the trader won't deal (dead or `KEY_BLACK_MARKET_DEAD`); coin is never sold; log every transaction.
- [x] **Task 4 — Barter + the coin-only/guarded/lockout rules (AC-1/AC-2, D4/D5).**
  - [x] 4.1 `barter(state, trader, offered, wanted)`: WANDERER only (refuse BLACK_MARKET + coins); swaps an accepted surplus item for a stock item of `≤` its base value — a coinless player is never blocked.
  - [x] 4.2 Black Market kill-lockout: an `ATTACK` into a `Trader` tile (when no enemy there) routes through `CombatSystem`, damages him, and on a BLACK_MARKET death sets `FlagStore.KEY_BLACK_MARKET_DEAD`; `TradeSystem.available` refuses him forever. Guards: 3 `RogueEnemy` placed adjacent to the Black Market (appended last, AD-5).
- [x] **Task 5 — Placement (AD-5) + the TRADE interaction (D1/D6).**
  - [x] 5.1 `RunState.placeTraders`: seed-derived sub-stream (`seed ^ TRADER_PLACEMENT_SALT` = `0x77AAD3`, distinct from bag/currency), drawn LAST in `generateFloor` (after `placeCurrency`); Wanderer west/mid via a deterministic outward ring scan, Black Market east + 3 guards. Shared rng untouched → pre-6.4 seeds byte-identical.
  - [x] 5.2 `PlayerAction.Kind.TRADE` + `PlayerAction.trade(dir)`; headless `narrative/TradeController` (safe-pause, mirrors `JournalController`); `MarginScreen` binds `[T]` to open the panel with an adjacent living trader (no turn tick, AD-14), renders trader glyphs + a minimal numbered trade panel (buy by number, `[K]` sell, `[B]` barter), all driving `TradeSystem`.
- [x] **Task 6 — Tests + verification (all ACs).**
  - [x] 6.1 AC-1: `TradeSystemTest` — the Wanderer trades by coin (buy debits by price, sell credits at a loss) AND by barter; a **coinless** inventory still barters a surplus item for a stock item (unblocked); the Wanderer stock is ≤ Silver-tier value.
  - [x] 6.2 AC-2: the Black Market refuses barter (coin-only); killing him via `CombatSystem.playerAttack` sets `KEY_BLACK_MARKET_DEAD` and all subsequent buys/deals are refused.
  - [x] 6.3 AC-3: `priceToBuy > base > priceToSell` and the round-trip loses; `makeChange` conserves value (`wallet -= price`) and breaks a Royal Gold Plaque into lower-tier change without overflow (`long`); a refused/empty-purse trade mutates nothing (survival never requires a trade).
  - [x] 6.4 Persistence (AD-6): `RunStatePersistenceTest` — traders + `KEY_BLACK_MARKET_DEAD` round-trip; both traders placed each run (reachable). Determinism (AD-5): trader placement byte-stable per seed; `StructureContentTest`/`NightWeatherHazardTest` untouched. Full suite **557 → 571 green**; `mvn -o -pl core test` + `mvn -o compile` (core + desktop) clean.

## Dev Notes

- **FR-21 / AD-17 — the economy closes here.** 6.3 made coin weighted, valued, and scarce; 6.4 gives it its only sinks. The spread (buy-at-loss/sell-at-premium) + finite stock + traders-are-the-only-sink is what enforces "convert surplus into readiness, never into safety." The scarcity test (round-trip strictly loses) is the AC-3 guarantee — pin it.
- **Reuse, don't reinvent (CLAUDE.md §2/§3).** `Trader` mirrors `RogueEnemy`/`Companion` (plain fields, list-under-save-root, `SaveService` element type). Placement mirrors `placeCurrency`/`placeFoundBags` (seed-derived sub-stream, drawn last, AD-5). The trade menu reuses the `DialogController` safe-pause contract (AD-14). Coin moves only via `Inventory.remove`/`tryAdd` + `walletValueInCopper` (6.3). Kill routes through `CombatSystem` (AD-4) — no second HP owner. Lockout is a `FlagStore` key like `KEY_WON` (AD-7). Add NO parallel systems.
- **This story spends the 6.3 deferral (make it explicit).** 6.3 deferred `Supply.copperValue()` `int`→`long` "before 6.4 mints plaques." 6.4 mints plaques (as change), so Task 2.1 must land the `long` widening — otherwise a big trade overflows. See `deferred-work.md` (2026-08-16, story-6.3).
- **AD-5 determinism.** Trader placement is the final generation pass from its own seed-derived sub-stream (fresh salt, distinct from bag `0x6A6BAD` and currency `0xC0FFEE`) so prior seeds stay byte-identical — reuse the exact pattern, update snapshot invariants only if they count actors. Do NOT draw traders from the shared `rng`.
- **AD-6 additive.** New `traders` list + `KEY_BLACK_MARKET_DEAD` flag are additive; a pre-6.4 save loads an empty list / absent flag (field-initialized). Register `Trader` in `SaveService` and the test serializer (the `Bag`/`Weapon` precedent).
- **Observation discipline (Epic-1/1.8 lesson, held through Epic 6).** Every transaction (buy, sell, barter, change given), the kill-lockout, and a refused trade must emit a log line — no silent coin/item mutation.
- **Reachability (Epic-5 retro + the 6.1/6.2/6.3 findings).** Both traders must be genuinely reachable in real play: the Wanderer reachable by a coinless early player (west/mid), the Black Market reachable (east, guarded). Verify a trade actually completes in-game and that killing the Black Market locks it out — not test-only. The coinless-barter path (AC-1) is the reachability guarantee that a non-coin player is never blocked.
- **Scope discipline (CLAUDE.md §2).** Ship the headless model + system + placement + kill-lockout + a minimal panel. DEFER trader roaming, a bespoke shop UI, a large catalog, restock, and VOICE haggling. Epic 6 completes when 6.4's ACs are met — resist gold-plating the economy.
- **Build/verify:** `docs/BUILD.md` — `mvn -o -pl core install` before any `exec:java`; `mvn -o -pl core test` + `mvn -o compile` for the story gate.

### Project Structure Notes

- **New:** `core/src/main/java/com/margins/rogue/world/Trader.java`, `core/src/main/java/com/margins/rogue/world/TraderKind.java` (or nested enum), `core/src/main/java/com/margins/rogue/system/TradeSystem.java`; tests `core/src/test/java/com/margins/rogue/system/TradeSystemTest.java` (+ a placement/persistence test).
- **Edits:** `item/Supply.java` (`copperValue` → `long`; tradable base values), `item/Inventory.java` (`walletValueInCopper` → `long`), `state/RunState.java` (`traders` list + `placeTraders` + `TRADER_PLACEMENT_SALT`), `state/FlagStore.java` (`KEY_BLACK_MARKET_DEAD`), `system/CombatSystem.java` (attack-into-trader → kill-lockout), `system/PlayerAction.java` (`TRADE`), `save/SaveService.java` (register `Trader`), `MarginScreen.java` (trade key + minimal safe-pause panel).
- **Updated tests:** `RunStatePersistenceTest` (trader + lockout round-trip; register `Trader`), any test asserting the `copperValue` return type (now `long`).

### References

- [Source: _bmad-output/planning-artifacts/epics.md#Story 6.4: The two mobile traders] — AC-1/AC-2/AC-3, FR-21, AD-17.
- [Source: _bmad-output/implementation-artifacts/6-3-four-tier-scarce-currency.md] — the currency model traders spend (`copperValue`/`walletValueInCopper`), the seed-derived sub-stream placement pattern, and the deferred `int`→`long` hardening this story lands.
- [Source: core/src/main/java/com/margins/rogue/RogueEnemy.java] — the killable positioned tile-agent `Trader` mirrors.
- [Source: core/src/main/java/com/margins/rogue/narrative/DialogController.java] — the AD-14 safe-pause paused-menu contract the trade surface reuses.
- [Source: core/src/main/java/com/margins/rogue/state/FlagStore.java#KEY_WON] — the lockout-flag precedent for `KEY_BLACK_MARKET_DEAD` (also Story 2.5 "killing a quest-giver voids that quest").
- [Source: core/src/main/java/com/margins/rogue/state/RunState.java#placeCurrency] — the seed-derived sub-stream authored-placement pattern (`seed ^ salt`, drawn last) `placeTraders` mirrors.
- [Source: _bmad-output/implementation-artifacts/deferred-work.md#code review of story-6.3 (2026-08-16)] — the `copperValue` int→long item this story closes.

## Dev Agent Record

### Agent Model Used

Claude Opus 4.8 (1M context) — create-story 2026-08-16.

### Debug Log References

- `mvn -o -pl core test` — 571 tests, 0 failures (suite 557 → 571).
- `mvn -o compile` (core + desktop) — clean.

### Completion Notes List

- **AC-1 met.** The Wanderer trades by coin (`buy` debits the wallet by the price and adds the item; `sell` credits at a loss and removes it) OR by barter. A **coinless** player still barters a surplus item for a needed stock item (`TradeSystemTest.aCoinlessPlayerCanStillBarterWithTheWanderer`) — never blocked. Wanderer stock is cheap (≤ Silver-tier value).
- **AC-2 met.** The Black Market refuses barter (coin-only). Killing him routes through `CombatSystem.playerAttack` (the single HP authority, AD-4) — an ATTACK into his tile when no enemy is there damages the `Trader`; his death sets `FlagStore.KEY_BLACK_MARKET_DEAD`, and `TradeSystem.available` refuses him forever (`killingTheBlackMarketLocksOutTheTradeForever`). Placed with 3 adjacent `RogueEnemy` guards.
- **AC-3 met (scarcity).** `priceToBuy = base×2` (premium) > base > `priceToSell = base/2` (loss), so a sell→buy-back always loses coin — no infinite-money loop (AD-17). `makeChange` is value-conserving (`wallet -= price`) and breaks a Royal Gold Plaque into lower-tier change — which lands the 6.3-deferred `Supply.copperValue()` `int`→`long` (plaques now circulate). Survival never requires a trade: a refused/empty-purse buy mutates nothing and nothing in the survival loop reads the wallet.
- **Design refinement of D3 (make-change algorithm).** Spent coins **low→high** until the price is covered, then refunded the overshoot **high→low** — deterministic, value-conserving, and it naturally puts the top tier into circulation as change (rather than the story's under-specified "greedy highest-first", which over-shoots massively on a Plaque).
- **Design refinement of D1/D6 (traders stationary; UI minimal).** Traders are stationary positioned agents (no map field, no AI) — the "mobile" flavor is that they are open-map points, not shop buildings; **roaming AI is deferred**. Interaction is a headless `TradeController` (safe-pause, mirrors `JournalController`) + `PlayerAction.TRADE` + a **minimal** `MarginScreen` panel (`[T]` open when adjacent, buy by number, `[K]` sell, `[B]` barter, `[T]`/ESC close) and tinted trader glyphs so they are findable. Per the story's bound, a **bespoke shop UI** (animated trader sprites, multi-mode shop screen, VOICE haggling) is deferred — the same UI-deferral precedent as 6.1/6.2. The AC-critical logic is the fully-tested headless `TradeSystem`/`Trader`; the kill-lockout is reachable through the existing ATTACK key.
- **AD-5 determinism.** `placeTraders` draws from `seed ^ TRADER_PLACEMENT_SALT` (`0x77AAD3`), distinct from the bag (`0x6A6BAD`) and currency (`0xC0FFEE`) sub-streams, drawn last in `generateFloor` — pre-6.4 seeds stay byte-identical (the determinism snapshot/hazard tests needed no change). Trader positions use a deterministic outward ring scan (no RNG); only the 3 guard positions draw from the sub-stream.
- **AD-6 additive.** New `traders` list + `KEY_BLACK_MARKET_DEAD` flag are additive; a pre-6.4 save loads an empty list / absent flag. `SaveService` + the test serializer register `Trader`. Traders are stationary, so they need no map re-injection on load.
- **Deferred (unchanged):** trader roaming/mobility, a bespoke shop UI, a large stock catalog, restock/economy simulation, and VOICE-gated haggling.

### File List

- `core/src/main/java/com/margins/rogue/world/Trader.java` — **new**: the two-kind stationary trader agent (killable).
- `core/src/main/java/com/margins/rogue/system/TradeSystem.java` — **new**: pricing spread, make-change spend, buy/sell/barter, stock + base values.
- `core/src/main/java/com/margins/rogue/narrative/TradeController.java` — **new**: the safe-pause trade surface + adjacent-trader lookup.
- `core/src/main/java/com/margins/rogue/item/Supply.java` — `copperValue()` `int`→`long` (plaques circulate).
- `core/src/main/java/com/margins/rogue/state/FlagStore.java` — `KEY_BLACK_MARKET_DEAD` lockout key.
- `core/src/main/java/com/margins/rogue/state/RunState.java` — `traders` list, `getTraders`, `traderAt`, `placeTraders` (seed-derived sub-stream) + `TRADER_PLACEMENT_SALT`, called last in `generateFloor`.
- `core/src/main/java/com/margins/rogue/system/CombatSystem.java` — attack-into-a-trader → damage → BLACK_MARKET death sets the lockout.
- `core/src/main/java/com/margins/rogue/save/SaveService.java` — register `Trader` for `RunState.traders`.
- `core/src/main/java/com/margins/MarginScreen.java` — `[T]` trade open + `handleTradeInput` + `renderTradePanel` + trader glyph render.
- `core/src/test/java/com/margins/rogue/system/TradeSystemTest.java` — **new** (AC-1/AC-2/AC-3, 12 tests).
- `core/src/test/java/com/margins/rogue/state/RunStatePersistenceTest.java` — trader + lockout round-trip + placement determinism; register `Trader`.

## Review Findings

3-layer parallel review (Blind Hunter, Edge Case Hunter, Acceptance Auditor) against baseline `ada183f`. 9 findings triaged: 6 patched, 2 deferred, 1 dismissed. All ACs verified met.

**Patched (6):**

- **[HIGH — all 3 reviewers] Silent coin/item loss on a full pack.** `buy`/`sell`/`barter`/`makeChange` handed the player coins or goods via `Inventory.tryAdd` but ignored a `BACKPACK_FULL` result, so change, sale proceeds, and bought/bartered items could vanish. Fixed: `receiveOrSpill` and `giveCoin` now spill any overflow to Klein's tile via `RunState.addFloorItem` (drop-or-leave, FR-9) — a trade never silently destroys what it hands you.
- **[HIGH — Blind Hunter] `[T]` craft-torch regression.** The new trade-open handler at `MarginScreen` returned unconditionally, shadowing the Story 1.6 `[T]` → `craftTorch` binding. Fixed: `[T]` opens the trade surface **only** when a living trader is adjacent, otherwise falls through to `readAction` so craft-torch still works.
- **[MED — Edge Case Hunter] Actors stacked on one tile.** `placeTraders` could drop the Wanderer/Black Market onto an enemy-occupied tile, or a guard onto the trader (a trader hidden under a guard is untradable). Fixed: a single `tileFree(x,y,avoidX,avoidY)` gate (walkable + not player + no living enemy + no placed trader) now guards both the ring scan and the guard draws.
- **[MED — Edge Case Hunter] Zero-price sell.** A base-value item whose `priceToSell` floored to 0 could be sold for nothing. Fixed: `sell` refuses with "It's worth nothing to him." when `price <= 0`.
- **[MED — Acceptance Auditor] Barter took the cheapest affordable item.** `[B]` picked the *first* stock item the offered good could cover, not the best — a high-value item could be bartered for the cheapest thing on offer. Fixed: `handleTradeInput` now selects the **highest**-value stock item the offer covers.
- **[LOW — Blind Hunter] Dead `PlayerAction.TRADE`.** The trade flow runs through `TradeController`/`TradeSystem` and never constructs a `PlayerAction`, leaving `Kind.TRADE` + `trade(dir)` as dead code. Removed.

**Deferred (2):** trader roaming/mobility and a bespoke shop UI (animated sprites, multi-mode shop screen, VOICE haggling) — logged to `deferred-work.md`, consistent with the 6.1/6.2 UI-deferral precedent. The AC-critical logic is the fully-tested headless `TradeSystem`/`Trader`.

**Dismissed (1):** "make-change should be greedy highest-first" (Edge Case Hunter) — the story's under-specified greedy suggestion over-shoots massively on a Plaque; the implemented low→high pay / high→low refund is value-conserving and deterministic, and a test pins it. Working as designed.

Post-patch: `mvn -o -pl core test` 571 tests green; `mvn -o compile` (core + desktop) clean.

## Change Log

- 2026-08-17 — code-review (3-layer). 9 findings: 6 patched (silent coin/item loss → spill-to-floor; `[T]` craft-torch regression; actor tile-stacking in `placeTraders`; zero-price sell guard; barter picks highest-value affordable; removed dead `PlayerAction.TRADE`), 2 deferred (roaming, bespoke shop UI), 1 dismissed (greedy make-change). Suite 571 green; core + desktop clean. Status → done. See Review Findings.
- 2026-08-16 — dev-story (dev). Implemented all three ACs headless-first: `Trader` (stationary killable agent) + `TradeSystem` (spread pricing, value-conserving `makeChange`, buy/sell/barter) + `TradeController` (safe-pause) + placement (seed-derived sub-stream, AD-5) + kill-lockout (`CombatSystem` → `KEY_BLACK_MARKET_DEAD`) + save round-trip. Landed the 6.3-deferred `copperValue` int→long (plaques now circulate as change). Refined D3's make-change to low→high pay / high→low change (deterministic, conserves value, circulates the Plaque). Minimal `MarginScreen` panel + trader glyphs for reachability; bespoke shop UI + roaming deferred (the 6.1/6.2 UI-deferral precedent). Suite 557 → 571 green; core + desktop compile clean. Status → review.
- 2026-08-16 — created by create-story. Decisions: D1 two `Trader` first-class tile-agents (stationary per-seed points, mirror `RogueEnemy`; roaming deferred); D2 `TradeSystem` pricing with a strict buy-at-loss/sell-at-premium spread (AD-17); D3 coin spend + greedy `makeChange` reusing 6.3 currency, circulating the Royal Gold Plaque as change and landing the deferred `copperValue` int→long; D4 barter (Wanderer only) keeps coinless players unblocked; D5 Black Market coin-only + guarded + kill sets `KEY_BLACK_MARKET_DEAD` lockout (via `CombatSystem`, AD-4); D6 `PlayerAction.TRADE` + a minimal safe-pause trade panel (AD-14), bespoke UI deferred. Placement is a final seed-derived sub-stream pass (AD-5). Bounded: roaming, bespoke trade UI, large catalog, restock, VOICE haggling deferred. Status → ready-for-dev.

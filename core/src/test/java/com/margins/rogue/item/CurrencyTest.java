package com.margins.rogue.item;

import com.margins.rogue.state.RunState;
import com.margins.rogue.system.ConsumptionSystem;
import com.margins.rogue.world.StructureTable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Story 6.3 (FR-21, AD-17): the four-tier scarce currency. AC-1 — four weighted tiers with the
 * 25:1 / 10:1 / 1000:1 exchange rates. AC-2 — scarce, no infinite-money loop (coin enters only via
 * finite authored world placement), no coin sink mandatory for survival (the wallet is inert to
 * survival). Trading itself is Story 6.4 — 6.3 delivers the weighted, valued, scarce model.
 */
class CurrencyTest {

    // --- AC-1: four weighted tiers + exchange-rate valuation ---

    @Test
    void theFourTiersEncodeTheEpicsExchangeRates() {
        assertTrue(Supply.COPPER.isCurrency() && Supply.SILVER.isCurrency()
                && Supply.GOLD.isCurrency() && Supply.ROYAL_GOLD_PLAQUE.isCurrency(), "the four tiers are currency");
        assertFalse(Supply.PRESERVED_FOOD.isCurrency(), "a provision is not currency");
        assertFalse(Supply.TRAVELERS_PACK.isCurrency(), "a bag is not currency");

        assertEquals(1, Supply.COPPER.copperValue());
        assertEquals(25, Supply.SILVER.copperValue(), "Silver 25:1 Copper");
        assertEquals(250, Supply.GOLD.copperValue(), "Gold 10:1 Silver = 250 Copper");
        assertEquals(250_000, Supply.ROYAL_GOLD_PLAQUE.copperValue(), "Royal Gold Plaque 1000:1 Gold = 250 000 Copper");

        // The ratio chain, stated as ratios (the epics' 25:1 / 10:1 / 1000:1).
        assertEquals(25, Supply.SILVER.copperValue() / Supply.COPPER.copperValue());
        assertEquals(10, Supply.GOLD.copperValue() / Supply.SILVER.copperValue());
        assertEquals(1000, Supply.ROYAL_GOLD_PLAQUE.copperValue() / Supply.GOLD.copperValue());

        assertEquals(0, Supply.PRESERVED_FOOD.copperValue(), "non-currency is worth 0 Copper");
    }

    @Test
    void coinIsWeightedLikeAnyItemAndTheTiersAreTheWeightIncentive() {
        // Each coin weighs 1 (the default) — so a pile of low-tier coin is heavy while a high tier
        // carries the same value densely. That weight gap is the reason the tiers exist.
        assertEquals(1, Supply.COPPER.weight());
        assertEquals(1, Supply.SILVER.weight());
        assertEquals(1, Supply.GOLD.weight());
        assertEquals(1, Supply.ROYAL_GOLD_PLAQUE.weight());

        Inventory heavy = new Inventory();
        heavy.tryAdd(Supply.COPPER.ordinal(), 25);   // 25 Copper (worth 25) …
        Inventory light = new Inventory();
        light.tryAdd(Supply.SILVER.ordinal(), 1);    // … vs 1 Silver (also worth 25)

        assertEquals(heavy.walletValueInCopper(), light.walletValueInCopper(), "equal worth");
        assertTrue(heavy.totalWeight() > light.totalWeight(),
                "bulk low-tier coin weighs more than the equal-value high tier (the convert-up incentive)");
        assertEquals(25, heavy.totalWeight());
        assertEquals(1, light.totalWeight());
    }

    @Test
    void walletSumsAMixedPurseInCopper() {
        Inventory inv = new Inventory();
        inv.tryAdd(Supply.COPPER.ordinal(), 3);
        inv.tryAdd(Supply.SILVER.ordinal(), 2);
        inv.tryAdd(Supply.GOLD.ordinal(), 1);
        // 3·1 + 2·25 + 1·250 = 303
        assertEquals(303L, inv.walletValueInCopper());

        Inventory plaque = new Inventory();
        plaque.tryAdd(Supply.ROYAL_GOLD_PLAQUE.ordinal(), 4);
        assertEquals(1_000_000L, plaque.walletValueInCopper(), "high-tier value does not overflow (long)");

        assertEquals(0L, new Inventory().walletValueInCopper(), "an empty purse is worth 0");
    }

    @Test
    void coinStacksInOneMainSlot() {
        Inventory inv = new Inventory();
        assertEquals(Inventory.AddResult.ADDED, inv.tryAdd(Supply.COPPER.ordinal(), 5));
        assertEquals(Inventory.AddResult.ADDED, inv.tryAdd(Supply.COPPER.ordinal(), 5), "same type restacks");
        assertEquals(10, inv.count(Supply.COPPER.ordinal()), "coin stacks, does not spill to new slots");
        assertFalse(inv.isBackpackFull(), "a single coin stack does not consume the whole store");
        assertFalse(Supply.COPPER.isConsumedOnUse(), "coin is spent at a trader (6.4), never consumed via USE");
    }

    // --- AC-2: scarce, no infinite loop, no mandatory sink ---

    @Test
    void coinIsAuthoredPlacementOnlyNeverForestScatter() {
        for (Supply coin : new Supply[]{Supply.COPPER, Supply.SILVER, Supply.GOLD, Supply.ROYAL_GOLD_PLAQUE}) {
            assertFalse(coin.isScatterable(), coin + " is never generic forest scatter (AD-5/AD-17)");
        }
        int[] scatter = Supply.scatterableOrdinals();
        for (int ord : scatter) {
            assertFalse(Supply.byOrdinal(ord).isCurrency(), "the scatter pool contains no coin");
        }
    }

    @Test
    void theWorldHoldsAFiniteBoundedAmountOfCoin() {
        // Coin enters ONLY through RunState.placeCurrency — a one-shot pass that draws CURRENCY_LOOT
        // for each structure whose footprint sits east of the map midline (the deeper eastern camps —
        // value rises east; the western home-cluster stays coinless). No action mints coin, so the
        // absolute ceiling per run is every structure × every loot entry — a hard, code-derived bound.
        int maxCoins = StructureTable.all().length * StructureTable.CURRENCY_LOOT.length;
        // Sweep many seeds so "reachable AND bounded" is a structural property, not a seed-42 fluke.
        for (long seed = 0; seed < 300; seed++) {
            int coins = coinCount(new RunState(seed));
            assertTrue(coins > 0, "coin is reachable in real play — the eastern camps always yield some (seed " + seed + ")");
            assertTrue(coins <= maxCoins, "coin is bounded — no infinite source (seed " + seed + " placed " + coins + ")");
        }
    }

    @Test
    void everyCurrencyTierHasAPositiveCopperValue() {
        // Pin the isCurrency()/copperValue() coupling: a future coin tier appended without a
        // copperValue() case would silently value 0 and undercount the wallet — fail loudly here.
        for (Supply s : Supply.values()) {
            if (s.isCurrency()) {
                assertTrue(s.copperValue() > 0, s + " is currency but has no copperValue()");
            } else {
                assertEquals(0, s.copperValue(), s + " is not currency, so it is worth 0 Copper");
            }
        }
    }

    @Test
    void coinPlacementIsDeterministicPerSeed() {
        assertEquals(coinFingerprint(new RunState(7L)), coinFingerprint(new RunState(7L)),
                "the same seed places identical coin (a seed-derived sub-stream, AD-5)");
    }

    @Test
    void survivalNeverReadsOrMintsTheWallet() {
        // No mandatory sink: eating/drinking leaves wealth untouched, and coin is not a provision —
        // consuming one grants no nourishment and mints nothing (AC-2, AD-17). Traders (6.4) are the
        // first, OPTIONAL sink; survival must never depend on coin.
        RunState s = new RunState(42L);
        s.getInventory().tryAdd(Supply.SILVER.ordinal(), 4);
        long before = s.getInventory().walletValueInCopper();
        assertEquals(100L, before);

        s.getInventory().tryAdd(Supply.PRESERVED_FOOD.ordinal(), 1);
        ConsumptionSystem.consume(s, Supply.PRESERVED_FOOD.ordinal(), new ArrayList<>());
        assertEquals(before, s.getInventory().walletValueInCopper(), "eating does not spend coin");

        int hungerBefore = s.getPlayer().getHunger();
        boolean consumed = ConsumptionSystem.consume(s, Supply.COPPER.ordinal(), new ArrayList<>());
        assertFalse(consumed, "a coin is not a provision — consuming it is a no-op");
        assertEquals(hungerBefore, s.getPlayer().getHunger(), "coin grants no nourishment");
    }

    // --- helpers ---

    private static int coinCount(RunState s) {
        int n = 0;
        for (FloorItem fi : s.getFloorItems()) {
            Supply t = Supply.byOrdinal(fi.type);
            if (t != null && t.isCurrency()) n += fi.count;
        }
        return n;
    }

    private static List<String> coinFingerprint(RunState s) {
        List<String> out = new ArrayList<>();
        for (FloorItem fi : s.getFloorItems()) {
            Supply t = Supply.byOrdinal(fi.type);
            if (t != null && t.isCurrency()) out.add(fi.type + "@" + fi.x + "," + fi.y + "x" + fi.count);
        }
        Collections.sort(out);
        return out;
    }
}

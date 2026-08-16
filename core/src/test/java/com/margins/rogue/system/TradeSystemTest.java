package com.margins.rogue.system;

import com.margins.rogue.RoguePlayer;
import com.margins.rogue.item.Supply;
import com.margins.rogue.state.FlagStore;
import com.margins.rogue.state.RunState;
import com.margins.rogue.world.Trader;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Story 6.4 (FR-21, AD-17): the two mobile traders — the economy's only coin sinks. AC-1 the Wanderer
 * (coin OR barter, coinless-unblocked), AC-2 the Black Market (coin-only, killable-lockout), AC-3 the
 * scarcity spread (buy at a premium, sell at a loss; no trade is required for survival).
 */
class TradeSystemTest {

    private static RunState run() { return new RunState(99L); }
    private static int ord(Supply s) { return s.ordinal(); }

    // --- AC-3: the scarcity spread ---

    @Test
    void buyIsAPremiumSellIsALossSoAnyRoundTripLosesCoin() {
        long base = TradeSystem.baseValue(Supply.HERBAL_CURE);
        assertTrue(TradeSystem.priceToBuy(base) > base, "the player buys at a premium");
        assertTrue(TradeSystem.priceToSell(base) < base, "the player sells at a loss");
        assertTrue(TradeSystem.priceToBuy(base) > TradeSystem.priceToSell(base),
                "sell→buy-back always loses coin — no infinite-money loop (AD-17)");
    }

    @Test
    void makeChangeConservesValueAndBreaksAPlaqueIntoChange() {
        RunState s = run();
        s.getInventory().tryAdd(ord(Supply.ROYAL_GOLD_PLAQUE), 1); // 250 000 copper
        long before = s.getInventory().walletValueInCopper();
        assertTrue(TradeSystem.makeChange(s, 60));
        assertEquals(before - 60, s.getInventory().walletValueInCopper(), "value conserved: wallet -= price");
        assertTrue(s.getInventory().count(ord(Supply.GOLD)) > 0,
                "the plaque enters circulation as lower-tier change (closes the 6.3 defer)");
    }

    @Test
    void makeChangeRefusesWhenTooPoorWithoutMutating() {
        RunState s = run();
        s.getInventory().tryAdd(ord(Supply.COPPER), 5);
        long before = s.getInventory().walletValueInCopper();
        assertFalse(TradeSystem.makeChange(s, 100));
        assertEquals(before, s.getInventory().walletValueInCopper(), "no mutation on a refused spend");
    }

    // --- AC-1: the Wanderer (coin OR barter; coinless unblocked) ---

    @Test
    void buyingWithCoinDebitsTheWalletByThePriceAndAddsTheItem() {
        RunState s = run();
        s.getInventory().tryAdd(ord(Supply.GOLD), 1); // 250 copper
        Trader wanderer = new Trader(Trader.Kind.WANDERER, 0, 0);
        long price = TradeSystem.priceToBuy(TradeSystem.baseValue(Supply.PRESERVED_FOOD));
        long before = s.getInventory().walletValueInCopper();
        assertTrue(TradeSystem.buy(s, wanderer, Supply.PRESERVED_FOOD, new ArrayList<>()));
        assertEquals(before - price, s.getInventory().walletValueInCopper(), "debited exactly the price");
        assertTrue(s.getInventory().count(ord(Supply.PRESERVED_FOOD)) > 0, "got the item");
    }

    @Test
    void sellingCreditsCoinAtALossAndRemovesTheItem() {
        RunState s = run();
        s.getInventory().tryAdd(ord(Supply.HERBAL_CURE), 1);
        long price = TradeSystem.priceToSell(TradeSystem.baseValue(Supply.HERBAL_CURE));
        assertTrue(TradeSystem.sell(s, new Trader(Trader.Kind.WANDERER, 0, 0), Supply.HERBAL_CURE, new ArrayList<>()));
        assertEquals(price, s.getInventory().walletValueInCopper(), "credited the sell price");
        assertEquals(0, s.getInventory().count(ord(Supply.HERBAL_CURE)), "the item is gone");
    }

    @Test
    void coinItselfIsNeverSold() {
        RunState s = run();
        s.getInventory().tryAdd(ord(Supply.SILVER), 1);
        assertFalse(TradeSystem.sell(s, new Trader(Trader.Kind.WANDERER, 0, 0), Supply.SILVER, new ArrayList<>()));
        assertEquals(25L, s.getInventory().walletValueInCopper(), "the coin stays");
    }

    @Test
    void aCoinlessPlayerCanStillBarterWithTheWanderer() {
        RunState s = run();
        assertEquals(0L, s.getInventory().walletValueInCopper(), "no coin");
        s.getInventory().tryAdd(ord(Supply.PRESERVED_FOOD), 1); // base 12, surplus to trade
        Trader wanderer = new Trader(Trader.Kind.WANDERER, 0, 0);
        assertTrue(TradeSystem.barter(s, wanderer, Supply.PRESERVED_FOOD, Supply.ROPE, new ArrayList<>()),
                "a coinless player trades surplus for a needed item — never blocked (AC-1)");
        assertTrue(s.getInventory().count(ord(Supply.ROPE)) > 0, "got the wanted item");
        assertEquals(0, s.getInventory().count(ord(Supply.PRESERVED_FOOD)), "gave up the offered item");
    }

    @Test
    void barterRefusesAnUnderValuedOffer() {
        RunState s = run();
        s.getInventory().tryAdd(ord(Supply.ROPE), 1); // base 8 < TRAVELERS_PACK base 40
        assertFalse(TradeSystem.barter(s, new Trader(Trader.Kind.WANDERER, 0, 0),
                Supply.ROPE, Supply.TRAVELERS_PACK, new ArrayList<>()), "he wants more for that");
    }

    // --- AC-2: the Black Market (coin-only, guarded, killable-lockout) ---

    @Test
    void theBlackMarketIsCoinOnlyAndRefusesBarter() {
        RunState s = run();
        s.getInventory().tryAdd(ord(Supply.PRESERVED_FOOD), 1);
        assertFalse(TradeSystem.barter(s, new Trader(Trader.Kind.BLACK_MARKET, 0, 0),
                Supply.PRESERVED_FOOD, Supply.SMALL_TOOLS, new ArrayList<>()), "coin-only (AC-2)");
    }

    @Test
    void killingTheBlackMarketLocksOutTheTradeForever() {
        RunState s = run();
        s.getEnemies().clear(); // isolate the attack onto the trader
        int px = s.getPlayer().getTileX(), py = s.getPlayer().getTileY();
        Trader bm = new Trader(Trader.Kind.BLACK_MARKET, px + 1, py);
        s.getTraders().add(bm);
        assertTrue(TradeSystem.available(s, bm), "alive → deals");
        for (int i = 0; i < 20 && bm.isAlive(); i++) {
            CombatSystem.playerAttack(s, RoguePlayer.EAST, new ArrayList<>()); // STR 5 vs HP 12
        }
        assertFalse(bm.isAlive(), "the guarded trader can be killed (AC-2)");
        assertEquals(1, s.getFlagStore().get(FlagStore.KEY_BLACK_MARKET_DEAD), "his death sets the lockout flag");
        assertFalse(TradeSystem.available(s, bm), "the trade is refused forever after");
        s.getInventory().tryAdd(ord(Supply.GOLD), 4);
        assertFalse(TradeSystem.buy(s, bm, Supply.SMALL_TOOLS, new ArrayList<>()), "no dealing with the dead");
    }

    @Test
    void survivalNeverRequiresATrade() {
        // AD-17: trading is purely optional — an empty-purse buy simply fails, it never throws or
        // forces a trade, and no survival system reads the wallet (pinned for currency in CurrencyTest).
        RunState s = run();
        long wallet = s.getInventory().walletValueInCopper();
        assertFalse(TradeSystem.buy(s, new Trader(Trader.Kind.WANDERER, 0, 0), Supply.ROPE, new ArrayList<>()));
        assertEquals(wallet, s.getInventory().walletValueInCopper(), "a refused trade changes nothing");
    }

    // --- Trader model + tiers ---

    @Test
    void theWandererBartersTheBlackMarketDoesNot() {
        assertTrue(new Trader(Trader.Kind.WANDERER, 0, 0).acceptsBarter());
        assertFalse(new Trader(Trader.Kind.BLACK_MARKET, 0, 0).acceptsBarter());
        // The Wanderer deals cheap (Copper/Silver tier), the Black Market premium (Gold tier).
        for (Supply st : TradeSystem.stockFor(Trader.Kind.WANDERER)) {
            assertTrue(TradeSystem.baseValue(st) <= 30, st + " is Wanderer-tier (cheap)");
        }
        assertTrue(TradeSystem.stockFor(Trader.Kind.BLACK_MARKET).length > 0, "the Black Market has stock");
    }
}

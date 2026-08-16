package com.margins.rogue.system;

import com.margins.rogue.item.Inventory;
import com.margins.rogue.item.Supply;
import com.margins.rogue.state.FlagStore;
import com.margins.rogue.state.RunState;
import com.margins.rogue.world.Trader;

import java.util.List;

/**
 * Story 6.4 (FR-21, AD-17): the single authority over a trade transaction — the economy's only coin
 * sink. Headless (AD-1/AD-2); {@code MarginScreen} only presents what this decides. Reuses Story 6.3's
 * currency: coin moves solely through {@link Inventory#remove}/{@link Inventory#tryAdd} and is valued
 * by {@link Supply#copperValue()} / {@link Inventory#walletValueInCopper()}.
 *
 * <p><b>Scarcity (AC-3, AD-17).</b> The player buys at a premium and sells at a loss, so a
 * sell→buy-back round-trip always loses coin — no infinite-money loop, and the trader is always worse
 * than foraging. No survival system reads the wallet, so coin is never required to live.
 */
public final class TradeSystem {

    private TradeSystem() {}

    /** What the player pays the trader to BUY one unit (a premium, > base). */
    public static long priceToBuy(long base) { return base * 2; }

    /** What the trader pays the player to SELL one unit (a loss, < base for base ≥ 2). */
    public static long priceToSell(long base) { return base / 2; }

    /** Authored base Copper value of a tradable item. Coins value at their {@link Supply#copperValue()};
     *  known readiness goods carry authored prices; any other surplus keeps a small default so forage
     *  can always be sold. The spread (buy > base > sell) keeps every trade worse than foraging. */
    public static long baseValue(Supply s) {
        if (s.isCurrency()) return s.copperValue();
        switch (s) {
            case TRAVELERS_PACK:     return 40;
            case HERBAL_CURE:        return 30;
            case BLOODVEIN_MUSHROOM: return 20;
            case PRESERVED_FOOD:     return 12;
            case SMALL_TOOLS:        return 10;
            case ROPE:               return 8;
            default:                 return 4; // any forage surplus is worth a little
        }
    }

    /** The goods a trader sells (AC-1/AC-2): the Wanderer deals cheap readiness (Copper/Silver tier),
     *  the Black Market premium goods (Gold tier). */
    public static Supply[] stockFor(Trader.Kind kind) {
        switch (kind) {
            case WANDERER:     return new Supply[]{ Supply.PRESERVED_FOOD, Supply.HERBAL_CURE, Supply.ROPE };
            case BLACK_MARKET: return new Supply[]{ Supply.TRAVELERS_PACK, Supply.SMALL_TOOLS };
            default:           return new Supply[0];
        }
    }

    /** Whether this trader will deal now: alive, and (for the Black Market) not killed-out (AC-2). */
    public static boolean available(RunState state, Trader trader) {
        if (trader == null || !trader.isAlive()) return false;
        return trader.getKind() != Trader.Kind.BLACK_MARKET
                || state.getFlagStore().get(FlagStore.KEY_BLACK_MARKET_DEAD) == 0;
    }

    /** Buy one stock item with coin (both kinds). Refuses if the trader won't deal or the purse is short.
     *  A bought item that will not fit spills to Klein's tile (drop-or-leave, never silently lost). */
    public static boolean buy(RunState state, Trader trader, Supply stockType, List<String> messages) {
        if (!available(state, trader)) { messages.add("The trader will not deal."); return false; }
        Inventory inv = state.getInventory();
        long price = priceToBuy(baseValue(stockType));
        if (inv.walletValueInCopper() < price) {
            messages.add("Not enough coin (" + price + " needed).");
            return false;
        }
        makeChange(state, price); // spends the price; change (incl. a broken Plaque) spills if the pack is full
        receiveOrSpill(state, stockType, "Bought " + stockType.displayName() + " for " + price + " copper", messages);
        return true;
    }

    /** Sell one carried item for coin (both kinds). Coin itself is never sold. */
    public static boolean sell(RunState state, Trader trader, Supply offered, List<String> messages) {
        if (!available(state, trader)) { messages.add("The trader will not deal."); return false; }
        if (offered.isCurrency()) { messages.add("Coin is not for sale."); return false; }
        Inventory inv = state.getInventory();
        if (inv.count(offered.ordinal()) <= 0) { messages.add("You have no " + offered.displayName() + "."); return false; }
        long price = priceToSell(baseValue(offered));
        if (price <= 0) { messages.add("It's worth nothing to him."); return false; }
        inv.remove(offered.ordinal(), 1);
        giveCoin(state, price);
        messages.add("Sold " + offered.displayName() + " for " + price + " copper.");
        return true;
    }

    /** Barter a surplus item for a wanted stock item (Wanderer only, AC-1) — keeps a coinless player
     *  unblocked. Goods-for-goods; the offered item must be worth at least the wanted one (still scarce).
     *  The wanted item spills to Klein's tile if the pack is full (never a net item loss). */
    public static boolean barter(RunState state, Trader trader, Supply offered, Supply wanted, List<String> messages) {
        if (!available(state, trader)) { messages.add("The trader will not deal."); return false; }
        if (!trader.acceptsBarter()) { messages.add("The Black Market deals only in coin."); return false; }
        if (offered.isCurrency() || wanted.isCurrency()) { messages.add("Barter is goods for goods."); return false; }
        Inventory inv = state.getInventory();
        if (inv.count(offered.ordinal()) <= 0) { messages.add("You have no " + offered.displayName() + "."); return false; }
        if (baseValue(offered) < baseValue(wanted)) { messages.add("He wants more for that."); return false; }
        inv.remove(offered.ordinal(), 1);
        receiveOrSpill(state, wanted, "Bartered " + offered.displayName() + " for " + wanted.displayName(), messages);
        return true;
    }

    /** Add one {@code item} to the pack, or spill it to Klein's tile if the pack is full (drop-or-leave,
     *  FR-9) — a trade never silently destroys what it hands you. Logs which happened. */
    private static void receiveOrSpill(RunState state, Supply item, String okLine, List<String> messages) {
        if (state.getInventory().tryAdd(item.ordinal(), 1) == Inventory.AddResult.ADDED) {
            messages.add(okLine + ".");
        } else {
            state.addFloorItem(item.ordinal(), 1, state.getPlayer().getTileX(), state.getPlayer().getTileY());
            messages.add(okLine + " — dropped at your feet (pack full).");
        }
    }

    /** Spend {@code price} Copper-equivalent from the wallet: take coins low→high until covered, then
     *  refund the overshoot as change high→low (this is where the Royal Gold Plaque enters play as
     *  change). Value-conserving: wallet after == wallet before − price. No mutation if too poor. */
    public static boolean makeChange(RunState state, long price) {
        Inventory inv = state.getInventory();
        if (price < 0) return false;
        if (inv.walletValueInCopper() < price) return false;
        Supply[] lowToHigh = { Supply.COPPER, Supply.SILVER, Supply.GOLD, Supply.ROYAL_GOLD_PLAQUE };
        long paid = 0;
        for (Supply coin : lowToHigh) {
            while (paid < price && inv.count(coin.ordinal()) > 0) {
                inv.remove(coin.ordinal(), 1);
                paid += coin.copperValue();
            }
            if (paid >= price) break;
        }
        giveCoin(state, paid - price); // refund the overshoot as change
        return true;
    }

    /** Add {@code amount} Copper-equivalent to the wallet as coins, greedy high→low (fewest coins).
     *  Copper = 1 divides every tier, so any amount decomposes exactly. A coin that will not fit the
     *  pack spills to Klein's tile (drop-or-leave) so change/sale proceeds are never silently lost. */
    private static void giveCoin(RunState state, long amount) {
        Inventory inv = state.getInventory();
        int px = state.getPlayer().getTileX(), py = state.getPlayer().getTileY();
        Supply[] highToLow = { Supply.ROYAL_GOLD_PLAQUE, Supply.GOLD, Supply.SILVER, Supply.COPPER };
        for (Supply coin : highToLow) {
            long v = coin.copperValue();
            while (amount >= v) {
                if (inv.tryAdd(coin.ordinal(), 1) != Inventory.AddResult.ADDED) {
                    state.addFloorItem(coin.ordinal(), 1, px, py);
                }
                amount -= v;
            }
        }
    }
}

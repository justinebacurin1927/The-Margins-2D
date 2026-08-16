package com.margins.rogue.narrative;

import com.margins.rogue.state.RunState;
import com.margins.rogue.world.Trader;

/**
 * Story 6.4 (FR-21, AD-14): the safe-pause trade surface — a transient view-session controller (NOT
 * on {@code RunState}, AD-6) the screen renders while {@link #isActive()} suspends the turn loop, so
 * a trade ticks no survival clock and commits no turn (the {@link JournalController} precedent). The
 * transaction rules live in {@code TradeSystem}; this only tracks which trader is open and finds the
 * adjacent one to open. Headless (AD-1/AD-2).
 */
public class TradeController {

    private Trader trader; // non-null iff a trade surface is open

    public boolean isActive() { return trader != null; }

    public Trader getTrader() { return trader; }

    /** Open the surface with a specific (living) trader. */
    public void open(Trader t) { this.trader = t; }

    /** Close the surface back to gameplay. */
    public void close() { this.trader = null; }

    /** The living trader orthogonally adjacent to the player, or null — the open trigger. */
    public static Trader adjacentTrader(RunState state) {
        int px = state.getPlayer().getTileX(), py = state.getPlayer().getTileY();
        for (Trader t : state.getTraders()) {
            if (t.isAlive() && t.isAdjacentTo(px, py)) return t;
        }
        return null;
    }
}

package com.margins.rogue.state;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Story 4.3 — the run-scoped narrative k/v store: generic flags + Galleon's Bond (AD-7). */
class FlagStoreTest {

    @Test
    void unsetKeyReadsZero() {
        FlagStore fs = new FlagStore();
        assertEquals(0, fs.get("never.set"), "an unset key is the 0 sentinel, so callers never null-check");
    }

    @Test
    void setAndAddFlags() {
        FlagStore fs = new FlagStore();
        fs.set("cache.revealed", 1);
        assertEquals(1, fs.get("cache.revealed"));
        fs.add("counter", 3);
        fs.add("counter", 2);
        assertEquals(5, fs.get("counter"), "add accumulates from the 0 default");
    }

    @Test
    void bondStartsNeutral() {
        FlagStore fs = new FlagStore();
        assertEquals(0, fs.getBond(), "Bond baseline is 0");
        assertEquals(1, fs.getBondTier(), "0 is the neutral tier");
    }

    @Test
    void bondTierBoundaries() {
        FlagStore fs = new FlagStore();
        fs.set(FlagStore.KEY_BOND, -1);
        assertEquals(1, fs.getBondTier(), "-1 stays neutral");
        fs.set(FlagStore.KEY_BOND, -2);
        assertEquals(0, fs.getBondTier(), "<= -2 is cold");
        fs.set(FlagStore.KEY_BOND, 1);
        assertEquals(1, fs.getBondTier(), "1 stays neutral");
        fs.set(FlagStore.KEY_BOND, 2);
        assertEquals(2, fs.getBondTier(), ">= 2 is warm");
    }

    @Test
    void bondTagsAreTheSingleAuthority() {
        FlagStore fs = new FlagStore();
        fs.applyBondTag(FlagStore.BOND_TAG_HONEST);
        assertEquals(1, fs.getBond(), "honest tag = +1");
        fs.applyBondTag(FlagStore.BOND_TAG_HONEST);
        assertEquals(2, fs.getBond());
        assertEquals(2, fs.getBondTier(), "two honest choices warm Galleon");
        fs.applyBondTag(FlagStore.BOND_TAG_DISMISSIVE);
        assertEquals(1, fs.getBond(), "dismissive tag = -1");
    }

    @Test
    void unknownBondTagIsNoOp() {
        FlagStore fs = new FlagStore();
        fs.adjustBond(1);
        fs.applyBondTag("bond.mystery");
        assertEquals(1, fs.getBond(), "an unrecognized tag changes nothing");
    }
}

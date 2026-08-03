package com.margins.rogue.state;

import com.margins.rogue.item.Supply;
import com.margins.rogue.item.TrueIdentity;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** Story 3.3/3.4 — per-seed Supply→TrueIdentity binding and identify-on-use reveal (FR-11/12). */
class IdentifyMapTest {

    @Test
    void sameSeedProducesSameBinding() {
        IdentifyMap a = IdentifyMap.build(new Random(1234));
        IdentifyMap b = IdentifyMap.build(new Random(1234));
        for (int i = 0; i < Supply.count(); i++) {
            assertEquals(a.identityOf(i), b.identityOf(i), "the same seed reproduces the identity binding");
        }
    }

    @Test
    void identifyOnUseRevealsTheWholeType() {
        IdentifyMap m = IdentifyMap.build(new Random(7));
        assertFalse(m.isIdentified(0), "a fresh run has nothing identified");
        assertEquals(Supply.byOrdinal(0).displayName(), m.displayNameFor(0),
                "unidentified supplies show their generic name");

        m.markIdentified(0);

        assertTrue(m.isIdentified(0), "using one reveals the type for the run");
        assertEquals(m.identityOf(0).displayName(), m.displayNameFor(0),
                "an identified supply shows its true identity");
    }

    @Test
    void outOfRangeOrdinalsAreSafe() {
        IdentifyMap m = IdentifyMap.build(new Random(7));
        assertNull(m.identityOf(-1));
        assertNull(m.identityOf(999));
        assertFalse(m.isIdentified(999));
        assertDoesNotThrow(() -> m.markIdentified(999), "out-of-range reveal is ignored, not a crash");
    }

    @Test
    void everyTypeBindsToOneOfItsPossibleIdentities() {
        IdentifyMap m = IdentifyMap.build(new Random(55555));
        for (Supply s : Supply.values()) {
            TrueIdentity bound = m.identityOf(s.ordinal());
            assertNotNull(bound, "every supply type is bound");
            boolean legal = false;
            for (TrueIdentity opt : s.possibleIdentities()) if (opt == bound) legal = true;
            assertTrue(legal, s + " binds to one of its declared possible identities");
        }
    }
}

package com.margins.rogue;

import com.margins.rogue.state.FlagStore;
import com.margins.rogue.state.RunState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Story 5.5 (FR-17, AD-7): Bond deepens and breaks. Bond is per-companion and gates loyalty / help /
 * hostility; a loss resolves into one of three shapes — Captured (recoverable), Departure (low Bond),
 * or Death (permanent). The state model; the quest/dialogue payloads are Story 5.6 content.
 */
class CompanionLossTest {

    // --- AC-1: per-companion Bond effect gates ---

    @Test
    void highBondUnlocksLoyaltyAndColdBondWithholdsHelpPerCompanion() {
        FlagStore fs = new RunState(1L).getFlagStore();
        fs.adjustBond(CompanionId.ALDRIC, FlagStore.LOYALTY_BOND);   // warm enough for loyalty
        fs.adjustBond(CompanionId.MARA, FlagStore.WITHHOLD_BOND);    // cold enough to withhold

        assertTrue(fs.bondUnlocksLoyalty(CompanionId.ALDRIC), "high Bond unlocks loyalty/personal quests");
        assertFalse(fs.bondWithholdsHelp(CompanionId.ALDRIC), "a warm companion does not withhold help");

        assertTrue(fs.bondWithholdsHelp(CompanionId.MARA), "cold Bond withholds help");
        assertFalse(fs.bondUnlocksLoyalty(CompanionId.MARA), "a cold companion has no loyalty content");

        assertFalse(fs.bondUnlocksLoyalty(CompanionId.YENNA), "an untouched companion sits neutral — neither gate");
        assertFalse(fs.bondWithholdsHelp(CompanionId.YENNA));
    }

    // --- AC-1: betrayal turns hostile (and departs) ---

    @Test
    void betrayalTurnsTheCompanionHostileAndDepartsTheBody() {
        RunState s = new RunState(1L); // active companion is Aldric
        assertNotNull(s.getActiveCompanion());

        s.betray(CompanionId.ALDRIC);

        assertTrue(s.getFlagStore().isHostile(CompanionId.ALDRIC), "betrayal turns the companion hostile");
        assertEquals(CompanionLoss.DEPARTED, s.getFlagStore().getLoss(CompanionId.ALDRIC), "a betrayer leaves the party");
        assertNull(s.getActiveCompanion(), "the body is gone from the party");
        assertTrue(s.getFlagStore().getBond(CompanionId.ALDRIC) <= FlagStore.DEPARTURE_BOND, "the Bond bottomed out");
    }

    // --- AC-1 / AC-2: departure at low Bond ---

    @Test
    void lowBondTriggersDeparture() {
        RunState s = new RunState(1L);
        // not yet low enough
        s.getFlagStore().adjustBond(CompanionId.ALDRIC, FlagStore.DEPARTURE_BOND + 1);
        assertFalse(s.checkBondDeparture(CompanionId.ALDRIC), "above the floor, the companion stays");
        assertNotNull(s.getActiveCompanion());

        s.getFlagStore().adjustBond(CompanionId.ALDRIC, -1); // now at the departure floor
        assertTrue(s.checkBondDeparture(CompanionId.ALDRIC), "at the floor, the companion departs");
        assertEquals(CompanionLoss.DEPARTED, s.getFlagStore().getLoss(CompanionId.ALDRIC));
        assertNull(s.getActiveCompanion(), "a departed companion leaves the map");
    }

    // --- AC-2: the three loss shapes ---

    @Test
    void deathRecordsThePermanentShapeAndKeepsTheCorpse() {
        RunState s = new RunState(1L);
        Companion c = s.getActiveCompanion();

        s.loseCompanion(c.getId(), CompanionLoss.DEAD);

        assertEquals(CompanionLoss.DEAD, s.getFlagStore().getLoss(c.getId()));
        assertFalse(CompanionLoss.DEAD.recoverable(), "death is permanent");
        assertSame(c, s.getActiveCompanion(), "the corpse stays where it fell (render contract)");
    }

    @Test
    void captureRecordsARecoverableShapeAndRemovesTheBody() {
        RunState s = new RunState(1L);
        s.loseCompanion(CompanionId.ALDRIC, CompanionLoss.CAPTURED);

        assertEquals(CompanionLoss.CAPTURED, s.getFlagStore().getLoss(CompanionId.ALDRIC));
        assertTrue(CompanionLoss.CAPTURED.recoverable(), "a captured companion can be won back via quest");
        assertNull(s.getActiveCompanion(), "a captured companion is taken from the party");
    }

    @Test
    void anUntouchedCompanionHasNoLoss() {
        FlagStore fs = new RunState(1L).getFlagStore();
        assertEquals(CompanionLoss.NONE, fs.getLoss(CompanionId.OLD_FEN), "never-set loss reads NONE (AD-6)");
        assertFalse(CompanionLoss.NONE.recoverable());
    }
}

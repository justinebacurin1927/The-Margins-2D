package com.margins.rogue.narrative;

import com.margins.rogue.CompanionId;
import com.margins.rogue.CompanionLoss;
import com.margins.rogue.state.FlagStore;
import com.margins.rogue.state.RunState;
import com.margins.rogue.world.WorldSpine;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Story 5.6 (FR-18, AD-11): the act-gating quests. "Follow the Road" (reach the Copper Road
 * corridor) flips Act 1→2; "The Rescue" (reach the road-head prison) flips Act 2→3 on either
 * outcome. Advancing the act is what Epic 4's escalation channel reads.
 */
class ActGateControllerTest {

    private WorldSpine spine(RunState s) {
        return new WorldSpine(s.getTileMap().getWidth(), s.getTileMap().getHeight());
    }

    private List<JournalController.JournalEntry> journal(RunState s) {
        return new JournalController().entries(s);
    }

    private JournalController.QuestStatus statusOf(RunState s, String id) {
        for (JournalController.JournalEntry e : journal(s)) {
            if (e.id().equals(id)) return e.status();
        }
        return null; // unlisted
    }

    // --- AC-1: "Follow the Road" gates Act 1→2 ---

    @Test
    void followTheRoadIsActiveInActOneUntilTheCorridorIsReached() {
        RunState s = new RunState(42L);
        ActGateController gate = new ActGateController();

        // The player starts west, near Corneo — well short of the Watchtower easting.
        assertTrue(s.getPlayer().getTileX() < spine(s).watchtowerX());
        gate.resolve(s);

        assertEquals(1, s.getFlagStore().getAct(), "act does not advance before the corridor is reached");
        assertEquals(JournalController.QuestStatus.ACTIVE, statusOf(s, JournalController.QUEST_FOLLOW_THE_ROAD),
                "the quest auto-starts and reads ACTIVE in the Journal");
    }

    @Test
    void reachingTheCopperRoadCorridorFlipsActOneToTwo() {
        RunState s = new RunState(42L);
        ActGateController gate = new ActGateController();

        s.getPlayer().setTileX(spine(s).watchtowerX()); // the Tier-2 push east
        gate.resolve(s);

        assertEquals(2, s.getFlagStore().getAct(), "reaching the corridor advances the act 1→2 (AC-1)");
        assertEquals(JournalController.QuestStatus.COMPLETED, statusOf(s, JournalController.QUEST_FOLLOW_THE_ROAD),
                "the quest completes");
        assertTrue(s.getMessageLog().contains(ActGateController.LINE_ROAD), "the corridor beat is announced");
    }

    @Test
    void theActOneGateIsAOneShot() {
        RunState s = new RunState(42L);
        ActGateController gate = new ActGateController();
        s.getPlayer().setTileX(spine(s).watchtowerX());

        gate.resolve(s);
        int lines = s.getMessageLog().size();
        gate.resolve(s); // a second every-frame call

        assertEquals(2, s.getFlagStore().getAct(), "no double-advance");
        assertEquals(lines, s.getMessageLog().size(), "no duplicate corridor beat");
    }

    // --- AC-2: "The Rescue" gates Act 2→3 on either outcome ---

    @Test
    void reachingThePrisonWithAldricRecoverableRescuesHimAndFlipsTwoToThree() {
        RunState s = new RunState(42L);
        // Aldric is captured (the Story 2.4 state) and we are in Act 2.
        new CaptureController().resolve(s);
        assertNull(s.getActiveCompanion(), "Aldric was captured");
        s.getFlagStore().setAct(2);

        s.getPlayer().setTileX(spine(s).roadEndX()); // the road-head prison
        new ActGateController().resolve(s);

        assertEquals(3, s.getFlagStore().getAct(), "the rescue flips the gate 2→3 (AC-2)");
        assertEquals(CompanionId.ALDRIC, s.getActiveCompanionId(), "success rejoins Aldric");
        assertEquals(0, s.getFlagStore().get(FlagStore.KEY_ALDRIC_CAPTURED), "the captured flag is cleared");
        assertEquals(CompanionLoss.NONE, s.getFlagStore().getLoss(CompanionId.ALDRIC), "his loss is cleared");
        assertEquals(JournalController.QuestStatus.COMPLETED,
                statusOf(s, JournalController.QUEST_ROAD_EAST), "The Road East completes");
        assertTrue(s.getMessageLog().contains(ActGateController.LINE_RESCUE_WIN));
    }

    @Test
    void aFailedRescueLeavesAldricLostButStillFlipsTwoToThree() {
        RunState s = new RunState(42L);
        new CaptureController().resolve(s);
        s.getFlagStore().setAct(2);

        new ActGateController().resolveRescue(s, false); // the wired failure branch (determinant deferred to 5.7)

        assertEquals(3, s.getFlagStore().getAct(), "failure still flips the gate 2→3 (AC-2)");
        assertNull(s.getActiveCompanion(), "a failed rescue does not bring Aldric back");
        assertEquals(1, s.getFlagStore().get(FlagStore.KEY_ALDRIC_CAPTURED), "he remains lost");
        assertEquals(JournalController.QuestStatus.COMPLETED,
                statusOf(s, JournalController.QUEST_ROAD_EAST), "the quest still resolves");
        assertTrue(s.getMessageLog().contains(ActGateController.LINE_RESCUE_LOSS));
    }

    @Test
    void theRescueGateIsAOneShot() {
        RunState s = new RunState(42L);
        new CaptureController().resolve(s);
        s.getFlagStore().setAct(2);
        ActGateController gate = new ActGateController();

        gate.resolveRescue(s, true);
        int lines = s.getMessageLog().size();
        gate.resolveRescue(s, false); // a second call must not re-open a resolved rescue

        assertEquals(3, s.getFlagStore().getAct());
        assertEquals(CompanionId.ALDRIC, s.getActiveCompanionId(), "the succeeded rescue is not undone");
        assertEquals(lines, s.getMessageLog().size(), "no second rescue beat");
    }

    @Test
    void noGateFiresInActThree() {
        RunState s = new RunState(42L);
        s.getFlagStore().setAct(3);
        s.getPlayer().setTileX(spine(s).roadEndX());

        new ActGateController().resolve(s);

        assertEquals(3, s.getFlagStore().getAct(), "Act 3 has no further act-gate quests in 5.6 (the border win is 5.7)");
    }

    // --- Task 4: the flip feeds Epic 4's escalation channel (AD-11 channel a) ---

    @Test
    void flippingActOneToTwoThroughTheGateFeedsEpicFoursThickerInterior() {
        long[] seeds = {1L, 2L, 3L, 5L, 8L, 13L, 42L, 100L, 777L, 2024L};
        int act1Total = 0, act2Total = 0;
        for (long seed : seeds) {
            act1Total += new RunState(seed).getEnemies().size(); // the ctor generates the interior at Act 1

            RunState s = new RunState(seed);
            s.getPlayer().setTileX(spine(s).watchtowerX());
            new ActGateController().resolve(s);
            assertEquals(2, s.getFlagStore().getAct(), "the gate set the act the channel reads");
            s.generateFloor(); // regenerate now that the act is flipped (the live trigger the gate enables)
            act2Total += s.getEnemies().size();
        }
        assertTrue(act1Total > 0, "sanity: the sampled seeds field interior enemies at Act 1 (test not vacuous)");
        assertTrue(act2Total > act1Total,
                "Act 2 (post-gate) fields more enemies than Act 1 across the same seeds ("
                        + act2Total + " vs " + act1Total + ")");
    }
}

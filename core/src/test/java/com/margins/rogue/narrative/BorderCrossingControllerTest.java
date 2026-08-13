package com.margins.rogue.narrative;

import com.margins.rogue.CompanionId;
import com.margins.rogue.RoguePlayer;
import com.margins.rogue.state.FlagStore;
import com.margins.rogue.state.RunState;
import com.margins.rogue.world.WorldSpine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Story 5.7 (FR-18, AD-12): the border-crossing win. Reaching the NW border in Act 3 ends the run as
 * a victory (KEY_WON + epilogue); the border is always walkable and the win is gated on Act 3.
 */
class BorderCrossingControllerTest {

    private WorldSpine spine(RunState s) {
        return new WorldSpine(s.getTileMap().getWidth(), s.getTileMap().getHeight());
    }

    /** Put Klein on the NW border landmark. */
    private void placeAtBorder(RunState s) {
        WorldSpine spine = spine(s);
        RoguePlayer p = s.getPlayer();
        p.setTileX(spine.borderX());
        p.setTileY(spine.borderY());
    }

    @Test
    void reachingTheBorderBeforeActThreeDoesNotWin() {
        RunState s = new RunState(42L);
        placeAtBorder(s);
        BorderCrossingController gate = new BorderCrossingController();

        gate.resolve(s); // Act 1
        assertEquals(0, s.getFlagStore().get(FlagStore.KEY_WON), "no win in Act 1");

        s.getFlagStore().setAct(2);
        gate.resolve(s);
        assertEquals(0, s.getFlagStore().get(FlagStore.KEY_WON), "no win in Act 2 — the crossing is Act 3 (AC-1)");
    }

    @Test
    void notAtTheBorderInActThreeDoesNotWin() {
        RunState s = new RunState(42L);
        s.getFlagStore().setAct(3);
        // Player starts near Corneo, far from the NW border.
        assertTrue(Math.abs(s.getPlayer().getTileX() - spine(s).borderX()) > BorderCrossingController.REACH_BAND);

        new BorderCrossingController().resolve(s);

        assertEquals(0, s.getFlagStore().get(FlagStore.KEY_WON), "reaching the border is required, not just Act 3");
    }

    @Test
    void reachingTheBorderInActThreeWinsAndLandsTheEpilogue() {
        RunState s = new RunState(42L);
        s.getFlagStore().setAct(3);
        placeAtBorder(s);

        new BorderCrossingController().resolve(s);

        assertEquals(1, s.getFlagStore().get(FlagStore.KEY_WON), "the crossing wins the run (AC-2)");
        assertTrue(s.getMessageLog().contains(BorderCrossingController.LINE_HOME), "he reaches home");
        assertTrue(s.getMessageLog().contains(BorderCrossingController.LINE_CORNEO), "Corneo → Coneros seed (AC-2)");
        assertTrue(s.getMessageLog().contains(BorderCrossingController.LINE_GRAVEYARD), "the Graveyard-filling seed (AC-2)");
    }

    @Test
    void theWinIsAOneShot() {
        RunState s = new RunState(42L);
        s.getFlagStore().setAct(3);
        placeAtBorder(s);
        BorderCrossingController gate = new BorderCrossingController();

        gate.resolve(s);
        int lines = s.getMessageLog().size();
        gate.resolve(s); // a second every-frame call

        assertEquals(1, s.getFlagStore().get(FlagStore.KEY_WON));
        assertEquals(lines, s.getMessageLog().size(), "no duplicate epilogue");
    }

    @Test
    void theEpilogueNamesAldricWhenHeWasRescued() {
        RunState s = new RunState(42L); // a fresh run has Aldric as the active companion
        assertEquals(CompanionId.ALDRIC, s.getActiveCompanionId());
        s.getFlagStore().setAct(3);
        placeAtBorder(s);

        new BorderCrossingController().resolve(s);

        assertTrue(s.getMessageLog().contains(BorderCrossingController.LINE_ALDRIC_WITH), "Aldric crosses beside Klein");
        assertFalse(s.getMessageLog().contains(BorderCrossingController.LINE_ALDRIC_ALONE));
    }

    @Test
    void dyingAtTheThresholdIsDeathNotAWin() {
        RunState s = new RunState(42L);
        s.getFlagStore().setAct(3);
        placeAtBorder(s);
        s.getPlayer().takeDamage(9999); // a cordon foe cuts Klein down as he reaches the border
        assertFalse(s.getPlayer().isAlive());

        new BorderCrossingController().resolve(s);

        assertEquals(0, s.getFlagStore().get(FlagStore.KEY_WON), "you must SURVIVE the crossing (AC-2) — no win over a corpse");
    }

    @Test
    void theEpilogueIsAloneWhenAldricWasLost() {
        RunState s = new RunState(42L);
        s.removeActiveCompanion(); // Aldric lost (captured/dead/departed) — no active companion
        s.getFlagStore().setAct(3);
        placeAtBorder(s);

        new BorderCrossingController().resolve(s);

        assertEquals(1, s.getFlagStore().get(FlagStore.KEY_WON), "the win still lands (SM-1 — lived through the systems)");
        assertTrue(s.getMessageLog().contains(BorderCrossingController.LINE_ALDRIC_ALONE), "Klein crosses alone");
        assertFalse(s.getMessageLog().contains(BorderCrossingController.LINE_ALDRIC_WITH));
    }
}

package com.margins.rogue.narrative;

import com.margins.dialog.DialogNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Sequences the Act 0 paged-text intro (FR-1, AD-14). Holds an ordered list of
 * narration pages (zero-option {@link DialogNode}s — the Story 2.1 Decision-7
 * terminal-page shape) and advances through them one page at a time. This is the
 * navigation authority — the rule lives here, not in the screen (AD-1) — and it is
 * pure model (no libGDX, AD-2), headless-testable.
 *
 * <p>Unlike {@link DialogController}, this controller takes NO {@code RunState}: the
 * intro pages carry no effects and write no state (Story 2.2 Decision 1/7), so the
 * controller has no run access and physically cannot tick the clock or the survival
 * tracks. Safe pause (AD-14) is therefore structural — the enforcing branch is the
 * screen's ({@code MarginScreen.handleInput} returns before producing a
 * {@code PlayerAction} while {@link #isActive()}), and this controller could not tick
 * even if that branch were wrong.
 *
 * <p>The controller + the open page are transient view-session state held by the
 * screen; the intro is not part of {@code RunState}/save (AD-6). It opens once on a
 * fresh run (screen construction) and never persists.
 */
public class IntroController {

    private List<DialogNode> pages = new ArrayList<>();
    private int index = -1; // -1 = inactive (no scene open)

    /** Open the intro at the first page. An empty (or null) list leaves the intro inactive. The
     *  pages are defensively copied — the controller owns its own list, never an aliased caller's. */
    public void start(List<DialogNode> pages) {
        if (pages == null || pages.isEmpty()) {
            this.pages = new ArrayList<>();
            this.index = -1;
            return;
        }
        this.pages = new ArrayList<>(pages);
        this.index = 0;
    }

    /** Whether the intro is open (gameplay input should be suspended while true). */
    public boolean isActive() {
        return index >= 0;
    }

    /** The page currently on screen, or null when the intro is not open. */
    public DialogNode getCurrent() {
        return isActive() ? pages.get(index) : null;
    }

    /** Advance to the next page; on the LAST page this closes the intro. A no-op when inactive. */
    public void advance() {
        if (!isActive()) return;
        if (index + 1 < pages.size()) {
            index++;
        } else {
            end();
        }
    }

    /** Close the intro (the skip path, or the natural end). A no-op when already inactive. */
    public void end() {
        index = -1;
    }
}

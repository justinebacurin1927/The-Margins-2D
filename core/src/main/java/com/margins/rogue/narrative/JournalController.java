package com.margins.rogue.narrative;

import com.margins.rogue.state.FlagStore;
import com.margins.rogue.state.RunState;

import java.util.ArrayList;
import java.util.List;

/**
 * Story 2.5 (FR-19): the quest registry + the passive Journal. Holds the authored
 * {@link QuestDefinition}s and derives each quest's current status from
 * {@code RunState}'s FlagStore (AD-7) — it holds NO quest-state copy, so the Journal
 * is literally a passive lookup (AC-2): {@link #entries} recomputes every call and
 * can never drift from the flags. Pure model, no libGDX (AD-2), headless-testable.
 *
 * <p>Quest state is a namespaced flag family with the key helpers here as the single
 * authority (the {@code SceneEffects} {@code KEY_CACHE_*} pattern — a trigger never
 * hand-builds a key): {@code quest.<id>.started} (auto-started by a discovery or an
 * NPC line — AC-1), {@code quest.<id>.completed} (flipped by the act-gating stories,
 * Epic 5), {@code quest.<id>.voided} (scripted void), and {@code npc.<giver>.dead}
 * (giver killed — voids the quest, AC-1). Status precedence (Decision 8): VOIDED →
 * COMPLETED → ACTIVE → unlisted (a registered-but-unstarted quest does not appear).
 *
 * <p>The controller + the open surface are transient view-session state held by the
 * screen — nothing on {@code RunState}, nothing serialized (AD-6 by construction).
 * Opening/closing the Journal mutates nothing (AD-14: a quest log is a suspended text
 * surface); the screen swallows gameplay input while {@link #isActive()} and renders
 * {@link #entries}. The production catalog holds the two act-gating quests (Story 5.6) —
 * "Follow the Road" (the Act 1→2 push) and "The Road East", the rescue-seed thread the
 * Story 2.4 note opens (Decision 6); the NPC-line and void-on-kill rules are proven with
 * synthetic quests registered in tests.
 */
public class JournalController {

    /** Quest id for the rescue thread the 2.4 Torn Page opens (discovery-triggered). Its completion
     *  is Story 5.6's Act 2→3 gate ("The Rescue"). */
    public static final String QUEST_ROAD_EAST = "roadeast";

    /** Quest id for the Act 1→2 push (Story 5.6): reach the Copper Road corridor, a Tier-2 push east.
     *  Auto-started by {@link ActGateController} while in Act 1. */
    public static final String QUEST_FOLLOW_THE_ROAD = "followroad";

    /** The SPD observation line appended when the Torn Page read starts the quest (Task 2 —
     *  the mutation is announced, observation discipline; tune phrasing in review). */
    public static final String LINE_STARTED = "You mark the road ahead — a new thread in the Journal.";

    /** A quest's derived status (Decision 8) — the Journal's passive lookup result. */
    public enum QuestStatus { ACTIVE, COMPLETED, VOIDED }

    /** One authored quest: id (the flag-family root), title, objective, and the optional giver's
     *  NPC key (null = discovery-triggered — no void-on-kill check). */
    public record QuestDefinition(String id, String title, String objective, String giver) {}

    /** One Journal line: the quest's id, title, objective, and its derived status. */
    public record JournalEntry(String id, String title, String objective, QuestStatus status) {}

    private final List<QuestDefinition> catalog = new ArrayList<>();
    private boolean active = false;

    /** Seed the production catalog — the two act-gating quests (Story 5.6, Decision 6). */
    public JournalController() {
        register(new QuestDefinition(QUEST_FOLLOW_THE_ROAD, "Follow the Road",
                "Push east along the Copper Road, deeper toward the occupation.",
                null));
        register(new QuestDefinition(QUEST_ROAD_EAST, "The Road East",
                "Aldric was taken — prisoners to the road-head, east along the Copper Road. "
                        + "Follow the road east.",
                null));
    }

    /** The single-authority key helpers — quest state never hand-builds a key string (AD-7). */
    public static String startedKey(String id)   { return "quest." + id + ".started"; }
    public static String completedKey(String id) { return "quest." + id + ".completed"; }
    public static String voidedKey(String id)    { return "quest." + id + ".voided"; }
    public static String giverDeadKey(String giver) { return "npc." + giver + ".dead"; }

    /** Add an authored quest to the catalog (the ctor seeds the production quest; tests register
     *  synthetic quests to prove the NPC-line and void rules — Decision 6). Blank ids and
     *  duplicate registrations are rejected — a duplicate id would render two Journal rows
     *  deriving from the same flags. */
    public void register(QuestDefinition quest) {
        if (quest == null || quest.id() == null || quest.id().isBlank()) return;
        if (catalog.stream().anyMatch(q -> q.id().equals(quest.id()))) return;
        catalog.add(quest);
    }

    /** Open the Journal (the screen's J key). A no-op when already open. */
    public void open() {
        active = true;
    }

    /** Close the Journal (J/ESC, or the restart path). A no-op when already closed. */
    public void close() {
        active = false;
    }

    /** Whether the Journal page is open (the screen suspends gameplay input while true — AD-14). */
    public boolean isActive() {
        return active;
    }

    /**
     * The passive lookup (AC-2): each catalogued quest's derived status, recomputed from the
     * run's FlagStore every call — no quest-state held here. Only STARTED quests appear; among
     * them the precedence (Decision 8) is VOIDED (scripted void, or the giver is dead — AC-1)
     * → COMPLETED → ACTIVE. A registered-but-unstarted quest never appears — even with a
     * terminal flag set, a quest the player never knew about is no quest at all. Returns a
     * fresh list; never null.
     */
    public List<JournalEntry> entries(RunState state) {
        List<JournalEntry> out = new ArrayList<>();
        if (state == null) return out;
        FlagStore fs = state.getFlagStore();
        for (QuestDefinition q : catalog) {
            // Unstarted quests never appear — even with a terminal flag set (a scripted void or a
            // dead giver on a quest the player never knew about is no quest at all). Precedence only
            // applies among STARTED quests (Decision 8): VOIDED → COMPLETED → ACTIVE → unlisted.
            if (fs.get(startedKey(q.id())) == 0) continue;
            if (fs.get(voidedKey(q.id())) != 0
                    || (q.giver() != null && fs.get(giverDeadKey(q.giver())) != 0)) {
                out.add(new JournalEntry(q.id(), q.title(), q.objective(), QuestStatus.VOIDED));
            } else if (fs.get(completedKey(q.id())) != 0) {
                out.add(new JournalEntry(q.id(), q.title(), q.objective(), QuestStatus.COMPLETED));
            } else {
                out.add(new JournalEntry(q.id(), q.title(), q.objective(), QuestStatus.ACTIVE));
            }
        }
        return out;
    }
}

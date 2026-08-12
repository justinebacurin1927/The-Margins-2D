package com.margins.dialog;

/**
 * A node-entry effect descriptor (FR-19 / AC-2): a side effect an authored scene
 * node can carry — a flag write, a Bond shift, an item give/take, or an NPC
 * disposition shift. Pure content model (no libGDX, no rogue dependencies); the
 * EXECUTION lives in the controller ({@code com.margins.rogue.narrative}), which
 * maps each kind onto RunState/FlagStore/Inventory mutations and an SPD-tone log
 * line (observation discipline — Story 2.1).
 */
public sealed interface DialogEffect permits
        DialogEffect.SetFlag, DialogEffect.Bond, DialogEffect.GiveItem,
        DialogEffect.TakeItem, DialogEffect.Disposition, DialogEffect.Deescalate {

    /** Write a flag to the FlagStore (AD-7) when the node is entered. Scene bookkeeping —
     *  written silently (the node text is the observation). The {@link DialogNode#withFlag}
     *  convenience adds this kind. */
    record SetFlag(String key, int value) implements DialogEffect {}

    /** Apply a Bond tag via FlagStore.applyBondTag (FR-15) — e.g. BOND_TAG_HONEST/DISMISSIVE. */
    record Bond(String tag) implements DialogEffect {}

    /** Add an item stack to the backpack (Inventory.tryAdd); emits "No room in your pack." when full. */
    record GiveItem(int type, int count) implements DialogEffect {}

    /** Remove an item stack from the backpack (Inventory.remove); a no-op (with a line) when absent. */
    record TakeItem(int type, int count) implements DialogEffect {}

    /** Shift an NPC's disposition counter in the FlagStore ("disposition." + npc) by {@code delta}. */
    record Disposition(String npc, int delta) implements DialogEffect {}

    /** VOICE talk-down (Story 4.2, AC-2): on node entry, de-escalate SUSPICIOUS enemies within
     *  {@code radius} tiles of the player to UNAWARE. Execution lives in the controller (routed
     *  through DetectionSystem, AD-9); a parley scene attaches this to its VOICE-pass success node. */
    record Deescalate(int radius) implements DialogEffect {}
}

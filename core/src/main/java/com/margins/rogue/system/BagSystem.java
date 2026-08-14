package com.margins.rogue.system;

import com.margins.rogue.item.Bag;
import com.margins.rogue.item.BagTrap;
import com.margins.rogue.item.Inventory;
import com.margins.rogue.item.Supply;
import com.margins.rogue.state.RunState;

import java.util.List;

/**
 * Story 6.2 (FR-20): bag durability + thematic traps. Owns the RNG-bearing bag rules that must stay
 * out of the headless {@link Inventory} (AD-1/AD-5): rolling a found bag's hidden trap when it is
 * readied (AC-2), and — on a combat hit — wearing the pack down (AC-1) and possibly springing a trap,
 * breaking the bag and spilling its contents (AC-2). {@link Inventory} keeps the pure capacity/weight
 * math and the deterministic break/overflow computation; this system supplies the dice and the world.
 */
public final class BagSystem {

    private BagSystem() {}

    /** Chance (%) that a FOUND bag is trapped when readied (PRD Balance — tunable content). */
    public static final int FOUND_BAG_TRAP_CHANCE = 30;
    /** Chance (%) per combat hit that a readied trapped bag springs (AC-2). */
    public static final int TRAP_FIRE_CHANCE = 20;
    /** Durability a readied bag loses per combat hit taken (AC-1). */
    public static final int BAG_DECAY_PER_HIT = 1;
    /** AC-2: of a trap-broken bag's spilled contents, this % is recoverable on the ground; the rest is lost. */
    public static final int TRAP_RECOVERABLE_PERCENT = 75;
    /** A durability break spills everything — nothing is destroyed, only dropped (drop-or-leave). */
    public static final int DURABILITY_RECOVERABLE_PERCENT = 100;

    /**
     * Ready a FOUND storage bag held in the main store (the UI's Y-key path): roll whether it was
     * trapped (a hidden DART/FIRE/FREEZE) and ready it as a first-class {@link Bag}. Two seeded draws
     * at most (AD-5), on a deliberate player action. Returns whether a bag actually readied.
     */
    public static boolean ready(RunState state, int type, List<String> messages) {
        BagTrap trap = BagTrap.NONE;
        if (state.rng().nextInt(100) < FOUND_BAG_TRAP_CHANCE) {
            trap = BagTrap.ARMED[state.rng().nextInt(BagTrap.ARMED.length)];
        }
        boolean readied = state.getInventory().readyBagFromStore(type, trap); // trap stays hidden until it fires
        if (readied) {
            Supply s = Supply.byOrdinal(type);
            messages.add("Readied " + (s == null ? "a bag" : s.displayName()) + ".");
        }
        return readied;
    }

    /**
     * A combat hit batters the pack (called from {@link CombatSystem} when Klein takes a hit). Wears
     * the first readied bag (AC-1) — breaking it, spilling everything (recoverable), at 0 durability —
     * then gives any readied trapped bag a chance to spring (AC-2): its effect fires, the bag breaks,
     * and 75% of the overflow drops while 25% is lost.
     */
    public static void onPlayerHit(RunState state, List<String> messages) {
        Inventory inv = state.getInventory();
        List<Bag> bags = inv.getStorageBags();
        if (bags.isEmpty()) return;

        Bag worn = bags.get(0);
        worn.decay(BAG_DECAY_PER_HIT);
        if (worn.isBroken()) {
            messages.add("Your " + worn.getType().displayName() + " is battered apart.");
            spill(state, inv.breakBag(worn), DURABILITY_RECOVERABLE_PERCENT, messages);
        }

        Bag trapped = firstTrapped(bags);
        if (trapped != null && state.rng().nextInt(100) < TRAP_FIRE_CHANCE) {
            fireTrap(state, trapped, messages);
        }
    }

    /** Spring a specific readied bag's trap (AC-2): its effect fires, the bag breaks, and its overflow
     *  spills 75% recoverable / 25% lost. Deterministic (no RNG) — the {@link #onPlayerHit} chance gate
     *  decides WHEN; this is the WHAT. Package-visible for direct unit pinning. */
    static void fireTrap(RunState state, Bag trapped, List<String> messages) {
        trapped.getTrap().fire(state.getPlayer(), messages); // dart/fire/freeze — announces itself
        trapped.disarm();
        spill(state, state.getInventory().breakBag(trapped), TRAP_RECOVERABLE_PERCENT, messages);
    }

    /** Spill the overflow stacks to Klein's tile: {@code recoverablePercent}% of each stack drops
     *  (findable), the remainder is destroyed. Deterministic floor split — no RNG (AD-5). */
    private static void spill(RunState state, List<int[]> overflow, int recoverablePercent, List<String> messages) {
        if (overflow.isEmpty()) return;
        int px = state.getPlayer().getTileX();
        int py = state.getPlayer().getTileY();
        int lost = 0;
        for (int[] stack : overflow) {
            int type = stack[0];
            int count = stack[1];
            int keep = count * recoverablePercent / 100; // floor
            if (keep > 0) state.addFloorItem(type, keep, px, py);
            lost += count - keep;
        }
        messages.add(lost > 0
                ? "The pack spills — you salvage what you can, but " + lost + " is lost."
                : "The pack spills its contents onto the ground.");
    }

    private static Bag firstTrapped(List<Bag> bags) {
        for (Bag b : bags) if (b.isTrapped()) return b;
        return null;
    }
}

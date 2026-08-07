package com.margins.rogue.system;

import com.margins.rogue.RoguePlayer;
import com.margins.rogue.item.Inventory;
import com.margins.rogue.item.Supply;
import com.margins.rogue.item.TrueIdentity;
import com.margins.rogue.state.RunState;

import java.util.List;

/**
 * Consuming a provision (FR-6, Story 1.5): apply the item's nourishment (its
 * {@link TrueIdentity#apply}), then roll its poison risk on the seeded RNG (AD-5) —
 * the roll is why this can't live in {@code TrueIdentity.apply}, which has no RNG.
 * On a failed roll the player takes immediate HP harm; the tiered debuff pipeline
 * (Nausea→Fever→Delirium, Diarrhea) is Story 1.7 — see the TODO hook. Safe provisions
 * (cooked meat, well/boiled water) have {@code drinkRisk() == 0}, so their roll is a
 * guaranteed no-op — this class is the single path for ALL provisions (the original
 * mystery supplies keep the {@code identityOf().apply} route in {@link TurnEngine}).
 *
 * <p>A provision whose nourishment would be wasted (already Well Fed / Hydrated) is
 * REFUSED — no item spent, no turn, no risk roll — so a player can't self-poison for
 * nothing (Edge #2-review). No libGDX types (AD-2).
 */
public final class ConsumptionSystem {
    public static final int POISON_HARM = 6; // PRD Balance placeholder until Story 1.7's debuffs

    private ConsumptionSystem() {}

    /** Consume one unit of a provision. Returns true if a turn was spent (false = refused). */
    public static boolean consume(RunState state, int itemType, List<String> messages) {
        Supply s = Supply.byOrdinal(itemType);
        if (s == null || !s.isProvision()) return false;
        Inventory inv = state.getInventory();
        if (inv.count(itemType) <= 0) return false;

        RoguePlayer p = state.getPlayer();
        // Edge #2-review: eat()/drink() gain nothing when the track is already maxed, so a
        // risky provision would charge its poison roll for zero benefit. Refuse instead.
        if (s.isWater() ? !p.canDrink() : !p.canEat()) {
            messages.add(s.isWater() ? "Already hydrated — no need to drink."
                    : "Not hungry enough to eat that.");
            return false;
        }

        TrueIdentity id = state.getIdentifyMap().identityOf(itemType);
        if (id != null) id.apply(p); // nourishment (eat/drink)
        inv.remove(itemType, 1);

        if (state.rng().nextInt(100) < s.drinkRisk()) {
            p.hurtRaw(POISON_HARM);
            // TODO(1.7): route to DebuffSystem for the tiered bacterial/toxin tracks (FR-8).
            messages.add(s.displayName() + " — you're poisoned!");
        } else {
            messages.add("Consumed " + s.displayName() + ".");
        }
        return true;
    }
}

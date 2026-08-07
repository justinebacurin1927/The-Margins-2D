package com.margins.rogue.system;

import com.margins.rogue.RoguePlayer;
import com.margins.rogue.item.Inventory;
import com.margins.rogue.item.Supply;
import com.margins.rogue.item.TrueIdentity;
import com.margins.rogue.state.RunState;

import java.util.List;

/**
 * Consuming a provision (FR-6, Story 1.5): apply the item's effect (its {@link TrueIdentity#apply}
 * — nourishment or a cure), then resolve the track it carries on the seeded RNG (AD-5) — the roll
 * is why this can't live in {@code TrueIdentity.apply}, which has no RNG. A toxin (a toxic
 * mushroom, Story 1.7 FR-8) is deterministic — no roll; a risky provision rolls its contamination
 * and a failed roll routes to {@link DebuffSystem#applyBacterial} (replacing Story 1.5's flat HP
 * sting). Safe provisions (cooked meat, well/boiled water) have {@code drinkRisk() == 0}, so their
 * roll is a guaranteed no-op — this class is the single path for ALL provisions (the original
 * mystery supplies keep the {@code identityOf().apply} route in {@link TurnEngine}).
 *
 * <p>A provision whose nourishment would be wasted (already Well Fed / Hydrated) is REFUSED — no
 * item spent, no turn, no risk roll — so a player can't self-poison for nothing (Edge #2-review).
 * Cures bypass that refusal: a Well-Fed player can still take medicine (Story 1.7 Decision 6). No
 * libGDX types (AD-2).
 */
public final class ConsumptionSystem {
    private ConsumptionSystem() {}

    /** Consume one unit of a provision. Returns true if a turn was spent (false = refused). */
    public static boolean consume(RunState state, int itemType, List<String> messages) {
        Supply s = Supply.byOrdinal(itemType);
        if (s == null || !s.isProvision()) return false;
        Inventory inv = state.getInventory();
        if (inv.count(itemType) <= 0) return false;

        RoguePlayer p = state.getPlayer();
        // Edge #2-review: eat()/drink() gain nothing when the track is already maxed, so a risky
        // provision would charge its poison roll for zero benefit. Refuse instead — EXCEPT when the
        // consumption still helps: cures (always takeable, Decision 6), a sick player (nourish-out
        // settles Nausea/Fever, AC-3 — review F-05), and a toxin mushroom (grants no nourishment, so
        // fullness is meaningless — "the player chose to eat it", review F-08).
        boolean sick = p.getBacterialStage() == RoguePlayer.BacterialStage.NAUSEA
                || p.getBacterialStage() == RoguePlayer.BacterialStage.FEVER;
        if (!s.isCure() && s.toxin() == Supply.Toxin.NONE && !sick
                && (s.isWater() ? !p.canDrink() : !p.canEat())) {
            messages.add(s.isWater() ? "Already hydrated — no need to drink."
                    : "Not hungry enough to eat that.");
            return false;
        }

        // Nourish-out (AC-3) happens inside eat()/drink() — capture whether the player was sick so
        // a settle message can follow (a nicety; the clear is the real contract).
        boolean sickBefore = sick;

        TrueIdentity id = state.getIdentifyMap().identityOf(itemType);
        if (id != null) id.apply(p); // nourishment (eat/drink) or a cure effect
        inv.remove(itemType, 1);

        if (s.toxin() != Supply.Toxin.NONE) {
            DebuffSystem.applyToxin(state, s.toxin(), messages); // deterministic — no roll
        } else if (state.rng().nextInt(100) < s.drinkRisk()) {
            DebuffSystem.applyBacterial(state, messages); // the tiered bacterial track (FR-8)
        } else {
            messages.add("Consumed " + s.displayName() + ".");
        }
        if (sickBefore && p.getBacterialStage() == RoguePlayer.BacterialStage.NONE) {
            messages.add("Your stomach settles.");
        }
        return true;
    }
}

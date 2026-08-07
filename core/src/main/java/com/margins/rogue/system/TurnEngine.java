package com.margins.rogue.system;

import com.margins.rogue.RoguePlayer;
import com.margins.rogue.RogueTile;
import com.margins.rogue.Weather;
import com.margins.rogue.item.FloorItem;
import com.margins.rogue.item.Inventory;
import com.margins.rogue.item.Supply;
import com.margins.rogue.item.TrueIdentity;
import com.margins.rogue.state.RunState;

import java.util.List;

/**
 * Advances one turn by running systems in fixed order (AD-4):
 * PlayerAction -> Hunger -> Detection -> Companion follow -> Enemy AI (Combat)
 * -> Noise resolve -> Last Stand -> cleanup.
 * The screen builds a {@link PlayerAction} and calls {@link #advance}; no turn
 * rule lives in the screen (AD-2). Enemy movement/hunger only occur when the
 * player actually acted, preserving the original "wasted keypress into a wall"
 * behavior.
 */
public class TurnEngine {

    public TurnResult advance(RunState state, PlayerAction action) {
        TurnResult result = new TurnResult();
        RoguePlayer player = state.getPlayer();

        // Turn-boundary cleanup: the desperate flag only lasts the turn it fired.
        state.setLastStand(false);

        // Facing updates regardless of whether the action commits a turn.
        player.setFacing(action.dir);

        boolean acted = false;
        switch (action.kind) {
            case MOVE:
                int tx = player.getTileX() + action.dx;
                int ty = player.getTileY() + action.dy;
                if (state.getTileMap().isWalkable(tx, ty)) {
                    // Bloated slow: a 50% stumble that still spends the turn (spec §1).
                    if (player.isSlowed() && state.rng().nextInt(100) < 50) {
                        result.messages.add("Bloated — you stumble.");
                    } else if (player.isCrippled()) {
                        // Delirium's Crippled / Rotgut (Story 1.7, AC-1/AC-2): a 25% paranoid freeze
                        // and a 50% stumble, both spending the turn — movement is unreliable while
                        // crippled. One predicate, no ad-hoc flags (spine line 186).
                        int roll = state.rng().nextInt(100);
                        if (roll < 25) {
                            result.messages.add("Paranoia — you freeze.");
                        } else if (roll < 75) {
                            result.messages.add("Crippled — you stumble.");
                        } else {
                            player.tryMove(action.dx, action.dy);
                        }
                    } else {
                        player.tryMove(action.dx, action.dy);
                    }
                    acted = true;
                }
                break;
            case ATTACK:
                CombatSystem.playerAttack(state, action.dir, result.messages);
                acted = true;
                break;
            case BLOCK:
                player.setBlocking(true);
                result.messages.add("Brace!");
                acted = true;
                break;
            case WAIT:
                acted = true;
                break;
            case USE: {
                Supply s = Supply.byOrdinal(action.itemType);
                // All provisions (FR-6) route through ConsumptionSystem: it applies nourishment,
                // rolls the poison risk for risky ones (raw/rotten/spoiled meat, river/pond/
                // filtered water) and refuses a wasted drink/eat (Edge #2-review). The original
                // mystery supplies keep the identityOf().apply path below (their effect is the
                // per-seed bound identity, FR-11).
                if (s != null && s.isProvision()) {
                    acted = ConsumptionSystem.consume(state, action.itemType, result.messages);
                    break;
                }
                if (s != null && state.getInventory().count(action.itemType) > 0) {
                    TrueIdentity id = state.getIdentifyMap().identityOf(action.itemType);
                    if (id != null) {
                        // The mystery gamble's apply can change a hunger/thirst tier — observe it like
                        // ConsumptionSystem does, so the change is announced (review finding: the
                        // non-provision USE path was the one silent tier-change route).
                        RoguePlayer.HungerStatus hBefore = player.getStatus();
                        RoguePlayer.ThirstStatus tBefore = player.getThirstStatus();
                        id.apply(player); // effect is the per-seed bound identity (FR-11)
                        if (player.getStatus() != hBefore) result.messages.add(HungerSystem.hungerTierLine(player.getStatus()));
                        if (player.getThirstStatus() != tBefore) result.messages.add(ThirstSystem.thirstTierLine(player.getThirstStatus()));
                    }
                    if (s.isConsumedOnUse()) {
                        boolean wasIdentified = state.getIdentifyMap().isIdentified(action.itemType);
                        state.getIdentifyMap().markIdentified(action.itemType); // reveal the whole type (FR-12)
                        state.getInventory().remove(action.itemType, 1);
                        // First use reveals what it was; later uses just name the known identity.
                        result.messages.add(!wasIdentified && id != null
                                ? s.displayName() + ": " + id.displayName() + "!"
                                : "Used " + state.getIdentifyMap().displayNameFor(action.itemType));
                        acted = true;
                    } else {
                        // Inert item (e.g. the unreadable Sealed Letter): a no-op costs no turn,
                        // matching the empty-pickup / walk-into-wall precedent.
                        result.messages.add("Milek can't read it.");
                    }
                }
                break;
            }
            case DROP: {
                Supply s = Supply.byOrdinal(action.itemType);
                int n = state.getInventory().count(action.itemType);
                if (s != null && n > 0 && state.getInventory().drop(action.itemType)) {
                    state.addFloorItem(action.itemType, n, player.getTileX(), player.getTileY());
                    result.messages.add("Dropped " + state.getIdentifyMap().displayNameFor(action.itemType));
                    acted = true;
                }
                break;
            }
            case PICKUP: {
                FloorItem it = state.takeItemAt(player.getTileX(), player.getTileY());
                if (it != null) {
                    if (state.getInventory().tryAdd(it.type, it.count) == Inventory.AddResult.ADDED) {
                        Supply s = Supply.byOrdinal(it.type);
                        result.messages.add("Picked up " + (s != null ? state.getIdentifyMap().displayNameFor(it.type) : "item"));
                        acted = true;
                    } else {
                        // Backpack full: return the stack to the tile and spend no turn.
                        // The screen gates on fullness before submitting, so this is a safety net.
                        state.addFloorItem(it.type, it.count, it.x, it.y);
                    }
                }
                break;
            }
            case DISTRACT: {
                // A refused shout (no companion / no uses left) spends no turn — mirrors the
                // inert-USE precedent. On success the noise resolves in the Noise step below.
                acted = CompanionSystem.distract(state, result.messages);
                break;
            }
            // Story 1.5 survival crafting (FR-6). Each returns whether a turn was spent; a refused
            // action (no source/fire/coal/room) commits no turn, mirroring the inert-USE precedent.
            case COLLECT:
                acted = collectWater(state, result.messages);
                break;
            case BUILD_CAMPFIRE: {
                int px = player.getTileX(), py = player.getTileY();
                state.setCampfire(px, py);
                state.setLight(px, py); // AD-18: the campfire lights (FOV) and is exposed (noise)
                result.messages.add("Built a campfire.");
                acted = true;
                break;
            }
            case CRAFT_TORCH: {
                // A torch (Story 1.6, FR-7): 1 Wood + 1 Coal craft the carried light that moves
                // with the player and burns down over ~one Night (TorchSystem). Refused without
                // materials OR while a torch already burns — no turn, mirroring the inert-USE
                // precedent (a torch can't be extinguished, so a refresh is pure waste; review
                // decision: refuse while lit).
                if (state.getTorchTurns() > 0) {
                    result.messages.add("A torch already burns.");
                } else if (state.getInventory().count(Supply.WOOD.ordinal()) >= 1
                        && state.getInventory().count(Supply.COAL.ordinal()) >= 1) {
                    state.getInventory().remove(Supply.WOOD.ordinal(), 1);
                    state.getInventory().remove(Supply.COAL.ordinal(), 1);
                    state.lightTorch(TorchSystem.TORCH_BURN);
                    result.messages.add("Crafted a torch.");
                    acted = true;
                } else {
                    result.messages.add("A torch needs Wood and Coal.");
                }
                break;
            }
            case COOK:
                acted = CookingSystem.cook(state, action.itemType, result.messages);
                break;
            case FILTER:
                acted = PurificationSystem.filter(state, action.itemType, result.messages);
                break;
            case BOIL:
                acted = PurificationSystem.boil(state, action.itemType, result.messages);
                break;
        }

        if (acted) {
            // The single acted path (AD-4): every acted turn runs the fixed order
            // survival (hunger/thirst/temperature/clock) -> detection -> companion ->
            // enemy -> noise -> checkLastStand -> FOV. Survival ticks run BEFORE
            // checkLastStand so lethal thirst/cold honors the Last-Stand reprieve (AD-5).
            HungerSystem.tick(player, result.messages);
            ThirstSystem.tick(player, result.messages);
            // Capture AFTER the direct ticks so Diarrhea's amplified drain (DebuffSystem) — which
            // runs after and can cross ANOTHER tier — still announces its drop (review finding).
            // The Systems' own observation already caught their ticks' crossing; this catches only
            // the additional one DebuffSystem caused, so no line duplicates.
            RoguePlayer.HungerStatus hAfterTicks = player.getStatus();
            RoguePlayer.ThirstStatus tAfterTicks = player.getThirstStatus();
            DebuffSystem.tick(state, result.messages); // Story 1.7: debuff escalation + Diarrhea's amplified drain (AD-4)
            if (player.getStatus() != hAfterTicks) result.messages.add(HungerSystem.hungerTierLine(player.getStatus()));
            if (player.getThirstStatus() != tAfterTicks) result.messages.add(ThirstSystem.thirstTierLine(player.getThirstStatus()));
            TemperatureSystem.tick(state, result.messages); // the weather + fire drivers need the whole run (Story 1.6)
            SpoilageSystem.tick(state); // food ages on the acted path (FR-6, Story 1.5)
            Weather before = state.getWeather();
            state.tickClock(); // rolls the next cycle's weather at a boundary (FR-5)
            if (state.getWeather() != before) result.messages.add(state.getWeather().onsetLine());
            DetectionSystem.update(state, result.messages); // advance awareness before enemies move (AD-4)
            CompanionSystem.follow(state); // the ally moves in the Companion+Enemy-AI phase (AD-4, AD-10)
            CombatSystem.enemyPhase(state, result.messages);
            TorchSystem.tick(state); // burn the torch (if lit) before its light/noise step (AD-4, Story 1.6)
            LightSystem.emitNoise(state); // a lit camp/torch is audible (AD-18): enqueue before resolve
            NoiseSystem.resolve(state); // Noise resolve step (AD-4)
            if (action.kind == PlayerAction.Kind.WAIT) {
                result.messages.add("Wait");
            }
            // Reprieve check after all damage this turn; added last so "Last Stand!"
            // wins the message display over combat/wait text.
            CombatSystem.checkLastStand(state, result.messages);
            // Recompute sight from the player's (possibly new) position.
            FovSystem.compute(state);
        }

        // The bottom message log (NFR-3, Story 1.8): every turn's messages land here — acted AND
        // refused (a refusal like "A torch needs Wood and Coal." is feedback the surface must show,
        // even though it commits no turn — AD-5 honesty and the log are two different facts). The
        // append runs LAST so the acted-branch emissions (Wait, combat, Last Stand) are included.
        if (!result.messages.isEmpty()) state.appendMessages(result.messages);

        return result;
    }

    /** Collect raw water from a source tile on or 4-adjacent to the player (FR-6, Story 1.5).
     *  Returns false (no turn) when no source is in reach or the backpack is full. */
    private static boolean collectWater(RunState state, List<String> messages) {
        RoguePlayer p = state.getPlayer();
        int[][] spots = {{0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] d : spots) {
            Supply water = waterFor(state.getTileMap().getTile(p.getTileX() + d[0], p.getTileY() + d[1]));
            if (water == null) continue;
            if (state.getInventory().tryAdd(water.ordinal(), 1) == Inventory.AddResult.ADDED) {
                messages.add("Collected " + water.displayName() + ".");
                return true;
            }
            messages.add("Backpack full."); // a source is here but no room — no turn (like pickup)
            return false;
        }
        return false; // no source in reach → no turn
    }

    private static Supply waterFor(int tile) {
        switch (tile) {
            case RogueTile.WELL:  return Supply.WELL_WATER;
            case RogueTile.POND:  return Supply.POND_WATER;
            case RogueTile.RIVER: return Supply.RIVER_WATER;
            default:              return null;
        }
    }
}

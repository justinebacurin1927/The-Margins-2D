package com.margins.rogue;

import java.util.Random;

public class RoguePlayer {
    public static final int SOUTH = 0;
    public static final int NORTH = 1;
    public static final int WEST = 2;
    public static final int EAST = 3;

    private int tileX;
    private int tileY;
    private int facing;

    /** Hunger tiers (docs/REGION1-FOREST.md §1). Each is a turn countdown; the
     *  enum holds its full duration in turns. SATISFIED is the starting status. */
    public enum HungerStatus {
        WELL_FED("Well Fed", 350),
        SATISFIED("Satisfied", 250),
        HUNGRY("Hungry", 250),
        STARVING("Starving", 150);

        final String label;
        final int fullTurns;

        HungerStatus(String label, int fullTurns) {
            this.label = label;
            this.fullTurns = fullTurns;
        }
    }

    /** Food points that bump one tier up (an eat() amount accumulates toward this). */
    private static final int FOOD_PER_TIER = 100;

    private int maxHp;
    private int hp;

    // Hunger tier fields are field-initialized (not constructor-set) so a Json load
    // of a save predating them still yields a valid starting state (AD-6).
    private HungerStatus status = HungerStatus.SATISFIED;
    private int hungerTurns = HungerStatus.SATISFIED.fullTurns; // turns remaining in this tier
    private int foodPoints;        // accumulated food toward the next tier bump
    private int bloatedSlowTurns;  // Well Fed's Bloated movement-penalty countdown (first 50 turns)
    private int regenTimer;        // Well Fed regen cadence (+1 HP every 3 turns)
    private int starveTick;        // Starving damage cadence (1 HP/4 turns, 3 HP/2 turns in Rotting)

    private int str;
    private int instinct;
    private int grit;
    private int voice;

    private boolean blocking;
    private transient RogueTileMap map;

    private RoguePlayer() {} // for libGDX Json deserialization; map re-injected via setMap

    public RoguePlayer(int tileX, int tileY, RogueTileMap map) {
        this.tileX = tileX;
        this.tileY = tileY;
        this.map = map;
        this.facing = SOUTH;

        this.maxHp = 20;
        this.hp = 20;

        this.str = 5;
        this.instinct = 7;
        this.grit = 5;
        this.voice = 3;

        this.blocking = false;
    }

    public int getTileX() { return tileX; }
    public int getTileY() { return tileY; }
    public int getFacing() { return facing; }
    public int getHp() { return hp; }
    public int getMaxHp() { return maxHp; }
    public HungerStatus getStatus() { return status; }
    /** Turns remaining in the current hunger tier (persisted across save/load). */
    public int getHunger() { return hungerTurns; }

    /** HUD line: current tier + turns left, naming the Starving stage when starving. */
    public String hungerLabel() {
        if (status == HungerStatus.STARVING) return "Starving (" + starvingStageName() + ") " + hungerTurns;
        return status.label + " " + hungerTurns;
    }

    /** Bloated slow: while active, movement can stumble (Region 1 spec §1). */
    public boolean isSlowed() { return bloatedSlowTurns > 0; }

    public int getStr() {
        // Starving applies Fatigue's -35% max-Strength penalty for its whole duration (spec §1).
        if (status == HungerStatus.STARVING) return (int) Math.floor(str * 0.65f);
        return str;
    }
    public int getInstinct() { return instinct; }
    public int getGrit() { return grit; }
    public int getVoice() { return voice; }

    public boolean tryMove(int dx, int dy) {
        int nx = tileX + dx;
        int ny = tileY + dy;
        if (map.isWalkable(nx, ny)) {
            tileX = nx;
            tileY = ny;
            return true;
        }
        return false;
    }

    public void setFacing(int dir) {
        this.facing = dir;
    }

    public boolean isBlocking() { return blocking; }
    public void setBlocking(boolean v) { blocking = v; }

    public boolean tryDodge(Random rng) {
        return rng.nextInt(100) < dodgePercent();
    }

    /** Effective dodge chance (instinct×3), after Trembling's -15% Agility penalty
     *  (mapped to the dodge instinct; spec §1). Package-private for headless tests. */
    int dodgePercent() {
        int eff = instinct;
        if (status == HungerStatus.STARVING && hungerTurns <= 100) { // Trembling and worse
            eff = Math.round(eff * 0.85f);
        }
        return eff * 3;
    }

    public int getDamageReduction() {
        return grit / 2;
    }

    public int takeDamage(int amount) {
        int reduction = getDamageReduction();
        int finalDmg = Math.max(1, amount - reduction);
        if (blocking) {
            finalDmg = Math.max(1, finalDmg / 2);
            blocking = false;
        }
        hp = Math.max(0, hp - finalDmg);
        return finalDmg;
    }

    public void heal(int amount) {
        hp = Math.min(maxHp, hp + amount);
    }

    /** Eat toward the next tier up: FOOD_PER_TIER food points bump one tier. The
     *  Starving-stage eating penalty (Trembling -20% / Rotting -50% on the next
     *  tier's duration) is applied by {@link #riseOneTier()}. Well Fed is already
     *  maxed and gains nothing (spec §1). */
    public void eat(int amount) {
        if (status == HungerStatus.WELL_FED) return;
        foodPoints = Math.min(FOOD_PER_TIER, foodPoints + Math.max(0, amount));
        if (foodPoints >= FOOD_PER_TIER) {
            foodPoints = 0;
            riseOneTier();
        }
    }

    /** Raw HP loss that bypasses armor/dodge/block — for non-combat harm like food poisoning. */
    public void hurtRaw(int amount) {
        hp = Math.max(0, hp - Math.max(0, amount));
    }

    /** Reduce hunger (a penalty — e.g. spoiled food). Pushes the current tier's
     *  countdown down; crossing zero drops one tier. */
    public void starve(int amount) {
        int a = Math.max(0, amount);
        if (status == HungerStatus.STARVING) {
            hungerTurns = Math.max(0, hungerTurns - a); // starving faster, damage cadence unchanged
            return;
        }
        hungerTurns -= a;
        if (hungerTurns <= 0) dropTier();
    }

    /** One acted turn: advance the hunger countdown, then the Well Fed effects
     *  (Bloated regen + slow timer). Starving runs its own damage cadence. */
    public void tickHunger() {
        if (status == HungerStatus.STARVING) {
            starveTick++;
            if (starvingStage() == 3) {
                if (starveTick % 2 == 0) hurtRaw(3);        // Rotting: damage doubles
            } else if (starveTick % 4 == 0) {
                hurtRaw(1);                                 // Fatigue/Trembling: base
            }
            hungerTurns = Math.max(0, hungerTurns - 1);     // 150 → 0, then stays 0 (keeps rotting)
        } else {
            hungerTurns--;
            if (hungerTurns <= 0) dropTier();
        }
        if (status == HungerStatus.WELL_FED) {
            regenTimer--;
            if (regenTimer <= 0) { heal(1); regenTimer = 3; } // "Bloated" regen: +1 HP every 3 turns
            if (bloatedSlowTurns > 0) bloatedSlowTurns--;
        }
    }

    /** Starving stage from remaining turns: 1 Fatigue, 2 Trembling, 3 Rotting (spec §1). */
    private int starvingStage() {
        if (hungerTurns > 100) return 1;
        if (hungerTurns > 50) return 2;
        return 3;
    }

    private String starvingStageName() {
        switch (starvingStage()) {
            case 1: return "Fatigue";
            case 2: return "Trembling";
            default: return "Rotting";
        }
    }

    /** Drop to the next-lower tier (tickHunger or starve crossing zero). */
    private void dropTier() {
        switch (status) {
            case WELL_FED:
                status = HungerStatus.SATISFIED;
                bloatedSlowTurns = 0; // Bloated slow ends with Well Fed
                break;
            case SATISFIED:
                status = HungerStatus.HUNGRY;
                break;
            case HUNGRY:
                status = HungerStatus.STARVING;
                starveTick = 0;
                break;
            default:
                return; // Starving at 0 stays Starving — the damage cadence is already lethal
        }
        hungerTurns = status.fullTurns;
    }

    /** Move up one tier (eat). Recovering from Starving lands on Hungry, with its
     *  duration cut by the current stage's eating penalty (spec §1). */
    private void riseOneTier() {
        switch (status) {
            case STARVING: {
                int duration = HungerStatus.HUNGRY.fullTurns;
                int stage = starvingStage();
                if (stage == 2) duration = (int) Math.floor(duration * 0.8);      // Trembling: -20%
                else if (stage == 3) duration = (int) Math.floor(duration * 0.5); // Rotting: -50%
                status = HungerStatus.HUNGRY;
                hungerTurns = duration;
                break;
            }
            case HUNGRY:
                status = HungerStatus.SATISFIED;
                hungerTurns = HungerStatus.SATISFIED.fullTurns;
                break;
            case SATISFIED:
                status = HungerStatus.WELL_FED;
                hungerTurns = HungerStatus.WELL_FED.fullTurns;
                bloatedSlowTurns = 50; // Bloated slow lasts the first 50 turns of Well Fed
                regenTimer = 3;        // first Bloated regen lands after 3 turns
                break;
            default:
                break; // WELL_FED: already maxed (eat() bails earlier)
        }
    }

    public boolean isAlive() {
        return hp > 0;
    }

    public void setTileX(int x) { this.tileX = x; }
    public void setTileY(int y) { this.tileY = y; }

    /** Reposition for descent (model-only; HP/hunger/inventory untouched — AC-3). */
    public void placeAt(int x, int y) {
        this.tileX = x;
        this.tileY = y;
    }

    /** Re-inject the tilemap after a save load (map is transient — AD-6). */
    public void setMap(RogueTileMap map) { this.map = map; }

    /** Set HP to a specific value (used by the Last Stand reprieve). */
    public void reviveTo(int value) { this.hp = Math.max(0, Math.min(maxHp, value)); }
}

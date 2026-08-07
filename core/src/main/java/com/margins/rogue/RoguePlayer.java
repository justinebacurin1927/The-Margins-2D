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

    /** Thirst tiers (PRD FR-4). Turn countdown per tier; HYDRATED is the start.
     *  Parched runs three stages (Withered → Trembling → Dried Out) with a -2 HP/5t drain. */
    public enum ThirstStatus {
        HYDRATED("Hydrated", 200),
        THIRSTY("Thirsty", 150),
        DEHYDRATED("Dehydrated", 100), // Headache
        PARCHED("Parched", 80);

        final String label;
        final int fullTurns;

        ThirstStatus(String label, int fullTurns) {
            this.label = label;
            this.fullTurns = fullTurns;
        }
    }

    /** Temperature bands over the [-100, +100] exposure meter (PRD FR-4). The
     *  environmental drivers (Weather / Day-Night / campfire) arrive in Stories 1.3/1.6;
     *  here the meter drifts toward Neutral and harms only at the extreme bands. */
    public enum TempBand {
        FROZEN("Frozen"), COLD("Cold"), CHILLED("Chilled"), NEUTRAL("Neutral"),
        WARM("Warm"), HOT("Hot"), OVERHEATED("Overheated");

        final String label;
        TempBand(String label) { this.label = label; }
    }

    /** Food points that bump one tier up (an eat() amount accumulates toward this). */
    private static final int FOOD_PER_TIER = 100;
    /** Water points that bump one thirst tier up (a drink() amount accumulates toward this). */
    private static final int WATER_PER_TIER = 100;

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

    // Thirst track (FR-4). Field-initialized (like hunger) so a save predating these
    // fields loads a valid starting state (AD-6; libGDX fromJson runs field initializers).
    private ThirstStatus thirstStatus = ThirstStatus.HYDRATED;
    private int thirstTurns = ThirstStatus.HYDRATED.fullTurns;
    private int waterPoints;       // accumulated water toward the next tier bump
    private int parchTick;         // Parched damage cadence (-2 HP every 5 turns)

    // Temperature track (FR-4) — the meter only; drivers are Stories 1.3/1.6.
    private int temperature = 0;   // [-100, +100]; 0 = Neutral

    private int str;
    private int instinct;
    private int grit;
    private int voice;
    private int skill; // FR-11 horizontal-growth axis: governs cooking/purification rolls (Story 1.5)

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
        this.skill = 5;

        this.blocking = false;
    }

    public int getTileX() { return tileX; }
    public int getTileY() { return tileY; }
    public int getFacing() { return facing; }
    public int getHp() { return hp; }
    public int getMaxHp() { return maxHp; }
    public HungerStatus getStatus() { return status; }

    /** Whether eating can still benefit the player (Well Fed is maxed — eat() gains nothing).
     *  ConsumptionSystem refuses a provision when this is false (Edge #2-review). */
    public boolean canEat() { return status != HungerStatus.WELL_FED; }
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
    public int getSkill() { return skill; }
    /** Set SKILL (FR-11): the horizontal-growth axis. Growth-by-knowledge raises it in a later story. */
    public void setSkill(int value) { skill = value; }

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

    /** Effective dodge chance (instinct×3), after Trembling's -15% Agility penalty.
     *  Trembling can come from Starving (hunger) or Parched (thirst); the penalty is
     *  applied once, not stacked. Package-private for headless tests. */
    int dodgePercent() {
        int eff = instinct;
        if (isTrembling()) {
            eff = Math.round(eff * 0.85f);
        }
        return eff * 3;
    }

    /** The -15% AG "Trembling" condition, from either Starving-and-worse or Parched-and-worse. */
    private boolean isTrembling() {
        boolean hungerTrembling = status == HungerStatus.STARVING && hungerTurns <= 100; // Trembling + Rotting
        boolean thirstTrembling = thirstStatus == ThirstStatus.PARCHED && parchedStage() >= 2; // Trembling + Dried Out
        return hungerTrembling || thirstTrembling;
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

    // --- Thirst (FR-4): a parallel track to hunger; Parched drains -2 HP / 5 turns ---

    public ThirstStatus getThirstStatus() { return thirstStatus; }

    /** Whether drinking can still benefit the player (Hydrated is maxed — drink() gains nothing).
     *  ConsumptionSystem refuses a provision when this is false (Edge #2-review). */
    public boolean canDrink() { return thirstStatus != ThirstStatus.HYDRATED; }
    /** Turns remaining in the current thirst tier (persisted across save/load). */
    public int getThirst() { return thirstTurns; }

    /** HUD line: current tier + turns left, naming the Parched stage when parched. */
    public String thirstLabel() {
        if (thirstStatus == ThirstStatus.PARCHED) return "Parched (" + parchedStageName() + ") " + thirstTurns;
        return thirstStatus.label + " " + thirstTurns;
    }

    /** One acted turn: advance the thirst countdown; Parched runs its -2 HP/5t drain. */
    public void tickThirst() {
        if (thirstStatus == ThirstStatus.PARCHED) {
            parchTick++;
            if (parchTick % 5 == 0) hurtRaw(2);          // Parched: -2 HP / 5 turns (all stages)
            thirstTurns = Math.max(0, thirstTurns - 1);  // 80 → 0, then stays 0 (keeps draining)
        } else {
            thirstTurns--;
            if (thirstTurns <= 0) dropThirstTier();
        }
    }

    /** Drink toward the next tier up: WATER_PER_TIER points bump one tier (Hydrated is maxed). */
    public void drink(int amount) {
        if (thirstStatus == ThirstStatus.HYDRATED) return;
        waterPoints = Math.min(WATER_PER_TIER, waterPoints + Math.max(0, amount));
        if (waterPoints >= WATER_PER_TIER) {
            waterPoints = 0;
            riseThirstTier();
        }
    }

    /** Parched stage from remaining turns: 1 Withered, 2 Trembling, 3 Dried Out (of 80). */
    private int parchedStage() {
        if (thirstTurns > 53) return 1;
        if (thirstTurns > 26) return 2;
        return 3;
    }

    private String parchedStageName() {
        switch (parchedStage()) {
            case 1: return "Withered";
            case 2: return "Trembling";
            default: return "Dried Out";
        }
    }

    private void dropThirstTier() {
        switch (thirstStatus) {
            case HYDRATED:   thirstStatus = ThirstStatus.THIRSTY; break;
            case THIRSTY:    thirstStatus = ThirstStatus.DEHYDRATED; break;
            case DEHYDRATED: thirstStatus = ThirstStatus.PARCHED; parchTick = 0; break;
            default:         return; // Parched at 0 stays Parched — the drain is already lethal
        }
        thirstTurns = thirstStatus.fullTurns;
    }

    private void riseThirstTier() {
        switch (thirstStatus) {
            case PARCHED:    thirstStatus = ThirstStatus.DEHYDRATED; thirstTurns = ThirstStatus.DEHYDRATED.fullTurns; break;
            case DEHYDRATED: thirstStatus = ThirstStatus.THIRSTY;    thirstTurns = ThirstStatus.THIRSTY.fullTurns; break;
            case THIRSTY:    thirstStatus = ThirstStatus.HYDRATED;   thirstTurns = ThirstStatus.HYDRATED.fullTurns; break;
            default:         break; // HYDRATED: already maxed (drink() bails earlier)
        }
    }

    // --- Temperature (FR-4): the meter only; drivers (weather/fire) are Stories 1.3/1.6 ---

    public int getTemperature() { return temperature; }

    public TempBand getTempBand() {
        if (temperature <= -80) return TempBand.FROZEN;
        if (temperature <= -50) return TempBand.COLD;
        if (temperature <= -15) return TempBand.CHILLED;
        if (temperature <   15) return TempBand.NEUTRAL;
        if (temperature <   50) return TempBand.WARM;
        if (temperature <   80) return TempBand.HOT;
        return TempBand.OVERHEATED;
    }

    public String tempLabel() { return getTempBand().label + " (" + temperature + ")"; }

    /** Drive the exposure meter (Stories 1.3/1.6 push weather/fire deltas here); clamps to [-100, +100]. */
    public void adjustTemperature(int delta) {
        temperature = Math.max(-100, Math.min(100, temperature + delta));
    }

    /** One acted turn: harm at the extreme bands (Frozen/Overheated) every turn spent there, and
     *  drift one step toward Neutral. This is the driver-less fallback: TemperatureSystem routes
     *  here only when there is no Cold Snap and no fire (Story 1.6) — the real drivers (the
     *  weather and the campfire) apply their deltas via {@link #adjustTemperature}/{@link #warmTo}
     *  and then call {@link #tickTemperatureHarm()}. Harm-per-turn, not a counter cadence: a
     *  driver-less meter can only be briefly extreme, so a counter cadence (e.g. every 3rd turn)
     *  would almost never fire before drift exits the band. */
    public void tickTemperature() {
        // Harm-then-drift (unchanged from Story 1.2): the harm uses the PRE-drift band. The Story
        // 1.6 driver branches apply their delta first, then call tickTemperatureHarm() (the
        // post-delta band) — a deliberate, slightly more forgiving ordering for the weather/fire
        // paths (a player warming out of Frozen at a fire is not harmed that turn).
        tickTemperatureHarm();
        if (temperature > 0) temperature--;
        else if (temperature < 0) temperature++;
    }

    /** The shared cost of exposure: 1 HP per turn spent in an extreme band (Frozen/Overheated).
     *  Called by {@link #tickTemperature()} before its drift and by the Story 1.6 driver branches
     *  after their delta, so the band that decides the harm is the band spent at. */
    public void tickTemperatureHarm() {
        TempBand band = getTempBand();
        if (band == TempBand.FROZEN || band == TempBand.OVERHEATED) hurtRaw(1);
    }

    /** Warm toward a comfort cap: apply {@code amount} (already net of any cold driver) but never
     *  exceed {@code cap} — and never the meter's +100 ceiling, whichever is lower. A fire warms
     *  you to the top of the WARM band but never into HOT/OVERHEATED (AC-2, Story 1.6). */
    public void warmTo(int amount, int cap) {
        temperature = Math.max(-100, Math.min(Math.min(cap, 100), temperature + amount));
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

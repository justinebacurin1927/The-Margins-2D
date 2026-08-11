package com.margins.rogue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RoguePlayer {
    public static final int SOUTH = 0;
    public static final int NORTH = 1;
    public static final int WEST = 2;
    public static final int EAST = 3;
    // 8-direction melee (combat fix #3): the diagonals complete the aimed-attack arc. Facing stays
    // cardinal (movement is 4-dir); an ATTACK may aim any of the 8 via its dir.
    public static final int SOUTHWEST = 4;
    public static final int NORTHWEST = 5;
    public static final int SOUTHEAST = 6;
    public static final int NORTHEAST = 7;

    /** The x offset of a direction (0-7): EAST=1, WEST=±diagonal=-1, else 0. */
    public static int directionX(int dir) {
        return (dir == WEST || dir == SOUTHWEST || dir == NORTHWEST) ? -1
             : (dir == EAST || dir == SOUTHEAST || dir == NORTHEAST) ? 1 : 0;
    }

    /** The y offset of a direction (0-7): NORTH=1, SOUTH=±diagonal=-1, else 0. */
    public static int directionY(int dir) {
        return (dir == NORTH || dir == NORTHWEST || dir == NORTHEAST) ? 1
             : (dir == SOUTH || dir == SOUTHWEST || dir == SOUTHEAST) ? -1 : 0;
    }

    /** The direction whose offset is (dx,dy) — the inverse of {@link #directionX}/{@link #directionY}.
     *  -1 for the (0,0) no-direction case (the caller falls back to facing). */
    public static int directionOf(int dx, int dy) {
        for (int d = 0; d < 8; d++) {
            if (directionX(d) == dx && directionY(d) == dy) return d;
        }
        return -1;
    }

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

    /** Debuff tiers (FR-8, Story 1.7). The bacterial chain escalates Nausea→Fever→Delirium at
     *  each stage's course end (turns alone never clear — AC-3); Diarrhea runs parallel and
     *  amplifies the thirst/hunger drains (lethal if ignored). */
    public enum BacterialStage {
        NONE, NAUSEA, FEVER, DELIRIUM
    }

    /** Diarrhea stages: Stage 1 2× Thirst drain, Stage 2 3× Thirst+Hunger (PRD FR-8). */
    public enum DiarrheaStage {
        NONE, STAGE_1, STAGE_2
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

    // Debuff track (FR-8, Story 1.7) — the closed status shape (spine line 186). Field-initialized
    // empty so a save predating the fields loads non-null-empty (AD-6).
    public static final int NAUSEA_TURNS = 30;           // Nausea course (then escalates to Fever)
    public static final int FEVER_TURNS = 25;            // Fever course (then escalates to Delirium)
    public static final int DELIRIUM_TURNS = 40;         // Delirium course (latched until a cure treats it)
    public static final int DIARRHEA_STAGE_1_TURNS = 30; // before Diarrhea Stage 2 escalation
    public static final int HONEYMOON_TURNS = 60;        // hidden countdown to Collapse
    public static final int COLLAPSE_CAP_PERCENT = 40;   // Max-HP % cap until cured

    private BacterialStage bacterialStage = BacterialStage.NONE;
    private int bacterialTimer;          // turns remaining in the current bacterial stage's course
    private boolean deliriumTreated;     // a cure item treated Delirium (unlatches its ticking timer)
    private DiarrheaStage diarrheaStage = DiarrheaStage.NONE;
    private int diarrheaTimer;           // turns to the next Diarrhea stage (STAGE_1 → STAGE_2)
    private boolean rotgutCrippled;      // Rotgut's instant Crippled (persists until cured, AC-2)
    private int honeymoonCountdown;      // hidden Honeymoon countdown; 0 = not poisoned (AC-2)
    private int maxHpCapPercent;         // Collapse's Max-HP cap; 0 = no cap (AC-2)

    private int str;
    private int instinct;
    private int grit;
    private int voice;
    private int skill; // FR-11 horizontal-growth axis: governs cooking/purification rolls (Story 1.5)
    private int ag = 7; // Story 4.1 (FR-12, D3): AG — the evasion/initiative stat (dodge is ag×3, turn order).
                        // Field-initialized (AD-6): a pre-4.1 save (no ag key) loads the deterministic 7.

    private boolean blocking;
    private boolean evading; // Story 4.1 (FR-12): Dodge's one-turn evasion — cleared after the enemy phase (AD-4)
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
        this.ag = 7;

        this.blocking = false;
        this.evading = false;
    }

    public int getTileX() { return tileX; }
    public int getTileY() { return tileY; }
    public int getFacing() { return facing; }
    public int getHp() { return hp; }
    public int getMaxHp() {
        // Honeymoon Collapse caps Max HP at a % of base until a cure lifts it (Story 1.7 AC-2).
        return (maxHpCapPercent > 0) ? Math.max(1, maxHp * maxHpCapPercent / 100) : maxHp;
    }
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
        // The bacterial track stacks its own STR penalty (Story 1.7, FR-8): Nausea -30%, Fever -40%.
        float factor = 1f;
        if (status == HungerStatus.STARVING) factor *= 0.65f;
        if (bacterialStage == BacterialStage.NAUSEA) factor *= 0.70f;
        else if (bacterialStage == BacterialStage.FEVER) factor *= 0.60f;
        return (int) Math.floor(str * factor);
    }
    public int getInstinct() { return instinct; }
    public int getGrit() { return grit; }
    public int getVoice() { return voice; }
    public int getSkill() { return skill; }
    /** Set SKILL (FR-11): the horizontal-growth axis. Growth-by-knowledge raises it in a later story. */
    public void setSkill(int value) { skill = value; }
    public int getAg() { return ag; }
    /** Set AG (Story 4.1, FR-12): the combat/evasion stat. Tests + enemy-variety set it; default 7 (D3). */
    public void setAg(int value) { ag = value; }

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

    public boolean isEvading() { return evading; }
    /** Set the Dodge action's one-turn evasion (Story 4.1). Cleared by TurnEngine after the enemy phase. */
    public void setEvading(boolean v) { evading = v; }

    public boolean tryDodge(Random rng) {
        return rng.nextInt(100) < dodgePercent();
    }

    /** Dodge roll with a percentage boost (Story 4.1 Dodge action): chance = min(90, dodgePercent ×
     *  boost/100). The clamp keeps even a boosted dodge short of a guaranteed evasion. One seeded
     *  draw, like the base roll (AD-5). */
    public boolean tryDodge(Random rng, int boostPercent) {
        int chance = Math.min(90, dodgePercent() * boostPercent / 100);
        return rng.nextInt(100) < chance;
    }

    /** Effective dodge chance (ag×3), after Trembling's -15% Agility penalty — the real AG stat
     *  (Story 4.1, D3: AG=7 reproduces the old instinct=7 value exactly, 21%). Trembling can come
     *  from Starving (hunger) or Parched (thirst); Delirium's Vertigo adds a second -15%
     *  (multiplicative with Trembling, Story 1.7). The penalties are applied once each, not
     *  stacked. Package-private for headless tests. */
    int dodgePercent() {
        int eff = ag;
        if (isTrembling()) {
            eff = Math.round(eff * 0.85f);
        }
        if (isDelirious()) {
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
        hp = Math.min(getMaxHp(), hp + amount);
    }

    /** Eat toward the next tier up: FOOD_PER_TIER food points bump one tier. The
     *  Starving-stage eating penalty (Trembling -20% / Rotting -50% on the next
     *  tier's duration) is applied by {@link #riseOneTier()}. Well Fed is already
     *  maxed and gains nothing (spec §1). */
    public void eat(int amount) {
        nourishOut(); // AC-3: eating settles recoverable food-sickness (Nausea/Fever)
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

    /** The pure hunger drain: advance the countdown; Starving runs its damage cadence. Does NOT
     *  run the Well Fed block — an amplified drain (Diarrhea's, Story 1.7 review F-01) must drain
     *  the meter without accelerating the Bloated regen or shedding the slow. */
    public void drainHunger() {
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
    }

    /** One acted turn: drain hunger, then the Well Fed effects (Bloated regen + slow timer). */
    public void tickHunger() {
        drainHunger();
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
        nourishOut(); // AC-3: drinking settles recoverable food-sickness (Nausea/Fever)
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

    // --- Debuffs (FR-8, Story 1.7): the tiered status track; DebuffSystem drives the ticking ---

    public BacterialStage getBacterialStage() { return bacterialStage; }
    /** Turns remaining in the current bacterial stage's course (escalation at 0). */
    public int getBacterialTimer() { return bacterialTimer; }
    /** A cure item treated Delirium — only a treated Delirium timer ticks (AC-3). */
    public boolean isDeliriumTreated() { return deliriumTreated; }
    public DiarrheaStage getDiarrheaStage() { return diarrheaStage; }
    public int getDiarrheaTimer() { return diarrheaTimer; }
    /** Rotgut's instant Crippled, which persists until a cure clears it (AC-2). */
    public boolean isRotgutCrippled() { return rotgutCrippled; }
    /** Hidden Honeymoon countdown; 0 = not poisoned. Deliberately no message reveals it (AC-2). */
    public int getHoneymoonCountdown() { return honeymoonCountdown; }
    public boolean isCollapsed() { return maxHpCapPercent > 0; }

    /** The active debuff labels for the HUD (Story 1.8 AC-1): an ordered list of what's hurting the
     *  player — the bacterial stage (with its remaining timer), the parallel Diarrhea (with its
     *  timer; Stage 2 latches at 0), Rotgut, and the Honeymoon Collapse cap. Composes ONLY the
     *  closed shape (no new flags — spine line 186). The Honeymoon countdown itself stays hidden
     *  (AC-2): it NEVER appears here, even while it ticks toward Collapse. */
    public List<String> getActiveDebuffLabels() {
        List<String> labels = new ArrayList<>();
        switch (bacterialStage) {
            case NAUSEA:   labels.add("Nausea (" + bacterialTimer + ")"); break;
            case FEVER:    labels.add("Fever (" + bacterialTimer + ")"); break;
            case DELIRIUM: labels.add("Delirium (" + bacterialTimer + ")"); break;
            default:       break; // NONE — nothing to compose
        }
        if (diarrheaStage != DiarrheaStage.NONE) labels.add("Diarrhea (" + diarrheaTimer + ")");
        if (rotgutCrippled) labels.add("Rotgut");
        if (isCollapsed()) labels.add("Collapsed (max HP " + getMaxHp() + ")");
        return labels;
    }

    /** The movement bundle's predicate: Delirium's Crippled OR Rotgut's Crippled. One source of
     *  truth for the MOVE stumble/freeze hooks — no ad-hoc flags (spine line 186). */
    public boolean isCrippled() { return bacterialStage == BacterialStage.DELIRIUM || rotgutCrippled; }

    /** Delirium's Vertigo — the dodge penalty applies only to full Delirium (Story 1.7 Decision 9). */
    public boolean isDelirious() { return bacterialStage == BacterialStage.DELIRIUM; }

    /** Bacterial onset (a failed contamination roll, or Rotgut): begin Nausea + parallel Diarrhea
     *  Stage 1. An already-sick player keeps their stage — re-contamination never downgrades. */
    public void beginBacterial() {
        if (bacterialStage == BacterialStage.NONE) {
            bacterialStage = BacterialStage.NAUSEA;
            bacterialTimer = NAUSEA_TURNS;
        }
        if (diarrheaStage == DiarrheaStage.NONE) {
            diarrheaStage = DiarrheaStage.STAGE_1;
            diarrheaTimer = DIARRHEA_STAGE_1_TURNS;
        }
    }

    /** Rotgut (AC-2): the same onset plus the instant Crippled bundle. */
    public void beginRotgut() {
        rotgutCrippled = true;
        beginBacterial();
    }

    /** Honeymoon (AC-2): start the hidden countdown. Single active countdown (review F-03): a
     *  re-dose while one runs is a harmless no-op — it can't postpone the Collapse (hoarding to
     *  defer it) nor re-arm a post-collapse dose. A cured player (cap lifted) can poison again. */
    public void beginHoneymoon() {
        if (honeymoonCountdown <= 0 && !isCollapsed()) honeymoonCountdown = HONEYMOON_TURNS;
    }

    public void tickBacterialTimer() { bacterialTimer = Math.max(0, bacterialTimer - 1); }
    public void tickDiarrheaTimer() { diarrheaTimer = Math.max(0, diarrheaTimer - 1); }
    public void tickHoneymoon() { honeymoonCountdown = Math.max(0, honeymoonCountdown - 1); }

    /** Nausea → Fever at its course end (turns alone never clear — AC-3). */
    public void escalateToFever() { bacterialStage = BacterialStage.FEVER; bacterialTimer = FEVER_TURNS; }

    /** Fever → Delirium at its course end. The timer is latched until a cure treats it (AC-3). */
    public void escalateToDelirium() { bacterialStage = BacterialStage.DELIRIUM; bacterialTimer = DELIRIUM_TURNS; }

    /** Diarrhea Stage 1 → Stage 2. The Stage-2 timer latches at 0 — 3× drain forever (lethal if ignored). */
    public void escalateDiarrhea() { diarrheaStage = DiarrheaStage.STAGE_2; diarrheaTimer = 0; }

    /** A cure item treats Delirium: shorten to 25% of remaining and un-latch the timer (AC-3, PRD FR-8).
     *  The ×0.25 applies once — a treated Delirium is already unlatched, so a second cure is a no-op
     *  (review F-04: back-to-back cures shouldn't erase the shortened course). */
    public void treatDelirium() {
        if (bacterialStage != BacterialStage.DELIRIUM || deliriumTreated) return;
        deliriumTreated = true;
        bacterialTimer = (int) Math.floor(bacterialTimer * 0.25f);
    }

    /** Clear the whole bacterial track (a cure) — Delirium included. */
    public void clearBacterial() {
        bacterialStage = BacterialStage.NONE;
        bacterialTimer = 0;
        deliriumTreated = false;
    }

    public void clearDiarrhea() { diarrheaStage = DiarrheaStage.NONE; diarrheaTimer = 0; }
    public void clearRotgut() { rotgutCrippled = false; }
    public void clearBloated() { bloatedSlowTurns = 0; }

    /** Nourish-out (AC-3): eating/drinking clears the recoverable food-sickness (Nausea/Fever).
     *  Delirium and Diarrhea are NOT cleared by nourishment — only cures remove them. */
    private void nourishOut() {
        if (bacterialStage == BacterialStage.NAUSEA || bacterialStage == BacterialStage.FEVER) {
            bacterialStage = BacterialStage.NONE;
            bacterialTimer = 0;
        }
    }

    /** Honeymoon's collapse (AC-2): cap Max HP at a % of base and clamp current HP to the cap. */
    public void collapse() {
        maxHpCapPercent = COLLAPSE_CAP_PERCENT;
        hp = Math.min(hp, getMaxHp());
    }

    /** The cure item that lifts the Honeymoon cap. */
    public void liftCollapse() { maxHpCapPercent = 0; }

    /** Honey / Honeycomb (PRD FR-8): cure Sick/Poisoned — the recoverable bacterial stages and
     *  Rotgut's effects, plus Diarrhea. Delirium is NOT cleared by honey (only the cure item is). */
    public void cureWithHoney() {
        if (bacterialStage == BacterialStage.NAUSEA || bacterialStage == BacterialStage.FEVER) clearBacterial();
        clearRotgut();
        clearDiarrhea();
    }

    /** The generic cure item (HERBAL_CURE): clears Nausea/Fever, treats Delirium (75% shorter),
     *  and lifts the Honeymoon Collapse cap. */
    public void applyHerbalCure() {
        if (bacterialStage == BacterialStage.DELIRIUM) treatDelirium();
        else if (bacterialStage == BacterialStage.NAUSEA || bacterialStage == BacterialStage.FEVER) clearBacterial();
        liftCollapse();
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
    public void reviveTo(int value) { this.hp = Math.max(0, Math.min(getMaxHp(), value)); }
}

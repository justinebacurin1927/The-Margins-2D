package com.margins.rogue.item;

/**
 * Story 4.4 (FR-13, AD-13): a wielded weapon with per-instance durability and gear-with-memory.
 * A first-class instance (this sword is 43/90, repaired twice) — the flyweight int-ordinal
 * {@link Inventory} cannot carry mutable per-item state, so weapons live in a {@code RunState}
 * list instead (D1). Pure model, no libGDX (AD-2); serialized under {@code RunState} (AD-6) — the
 * no-arg constructor + plain fields let libGDX Json round-trip it.
 *
 * <p><b>Wear (AC-1):</b> attacking or blocking spends {@link #DECAY_PER_ACTION} durability; at 0 the
 * weapon is {@link #isBroken() broken} and unusable (combat falls back to unarmed base STR).
 *
 * <p><b>Gear-with-memory (AC-2, AD-13):</b> {@link #repair(int)} restores durability but permanently
 * lowers the maximum on a SKILL-modified decay curve, and the 6th repair is beyond repair. This is
 * the hard ceiling on how much fighting any one weapon can endure. Repair is a model method here;
 * the reachable repair action + material cost are Story 4.5 (D4).
 */
public class Weapon {

    /** The five weapon families (material tags/tiers hang off these). Only a representative subset
     *  ships in 4.4 (D2); the enum names all five so the full table is a content pass, not a refactor. */
    public enum Category { SPEAR, BOW, BLADE, AXE, CLUB }

    /** Fixed durability lost per attack/block (AC-1 "fixed durability per action"; PRD Balance). */
    public static final int DECAY_PER_ACTION = 1;

    // SKILL bands for the AD-13 repair curve (PRD Balance owns the exact cut; default skill 5 = Mid).
    public static final int SKILL_LOW_MAX = 3;   // skill <= 3 → Low band
    public static final int SKILL_HIGH_MIN = 7;  // skill >= 7 → High band; between → Mid

    /**
     * AD-13 decay curve: the new maximum as a percentage of {@link #originalMax} after the Nth repair,
     * indexed {@code [repairCount][band]} with band 0=Low, 1=Mid, 2=High. Row 0 = the 1st repair …
     * row 4 = the 5th; a 6th repair is beyond repair ({@link #MAX_REPAIRS}). Verbatim from AD-13.
     */
    private static final int[][] REPAIR_PCT = {
        {90, 93, 96}, // 1st
        {78, 84, 91}, // 2nd
        {65, 74, 85}, // 3rd
        {50, 63, 78}, // 4th
        {35, 51, 70}, // 5th
    };
    /** After this many repairs a weapon is beyond repair (AD-13 "6th+ beyond repair"). */
    public static final int MAX_REPAIRS = REPAIR_PCT.length; // 5

    // Per-tier data for the shipped set (D2). Extensible to the full 5×5 table later; PRD Balance.
    private static final int[] TIER_MAX   = {20, 30, 40, 55, 70}; // originalMax by tier T1..T5
    private static final int[] TIER_BONUS = { 2,  3,  4,  5,  6}; // damage added over base STR by tier

    private Category category;
    private int tier;
    private int damageBonus;
    private int originalMax;
    private int maxDurability;
    private int durability;
    private int repairCount;

    /** libGDX Json. */
    public Weapon() {}

    public Weapon(Category category, int tier, int damageBonus, int originalMax) {
        this.category = category;
        this.tier = tier;
        this.damageBonus = damageBonus;
        this.originalMax = originalMax;
        this.maxDurability = originalMax;
        this.durability = originalMax;
        this.repairCount = 0;
    }

    // --- The shipped minimal set (D2): spans categories and tiers to exercise the mechanism. ---
    public static Weapon spearT1() { return ofTier(Category.SPEAR, 1); }
    public static Weapon bladeT3() { return ofTier(Category.BLADE, 3); }
    public static Weapon bowT5()   { return ofTier(Category.BOW, 5); }

    /** Build a weapon of the given category at a tier (1..5), pulling its ceiling/bonus from the tier table. */
    public static Weapon ofTier(Category category, int tier) {
        int i = tier - 1;
        return new Weapon(category, tier, TIER_BONUS[i], TIER_MAX[i]);
    }

    public Category getCategory() { return category; }
    public int getTier() { return tier; }
    public int getDurability() { return durability; }
    public int getMaxDurability() { return maxDurability; }
    public int getOriginalMax() { return originalMax; }
    public int getRepairCount() { return repairCount; }

    /** The damage this weapon adds over base STR while usable; 0 once broken (defensive — combat
     *  already stops wielding a broken weapon). */
    public int damageBonus() { return isBroken() ? 0 : damageBonus; }

    public boolean isBroken() { return durability <= 0; }

    /** Whether a further repair is possible (AD-13: the 6th is beyond repair). */
    public boolean isRepairable() { return repairCount < MAX_REPAIRS; }

    /** Spend durability for one action (AC-1); never drops below 0. */
    public void decay(int amount) {
        durability = Math.max(0, durability - amount);
    }

    /**
     * Gear-with-memory (AC-2, AD-13): restore durability to the maximum, but permanently lower that
     * maximum on the SKILL-modified curve. Returns false without mutating when the weapon is already
     * beyond repair (the 6th repair). On success the max drops to the curve's percentage of the
     * ORIGINAL ceiling (not the current one — the curve is anchored to fresh), durability refills to
     * the new max, and the repair count advances.
     */
    public boolean repair(int skill) {
        if (!isRepairable()) return false; // 6th+ beyond repair
        int pct = REPAIR_PCT[repairCount][skillBand(skill)];
        repairCount++;
        maxDurability = Math.round(originalMax * pct / 100f);
        durability = maxDurability;
        return true;
    }

    /** Low(0)/Mid(1)/High(2) band for the AD-13 curve. Package-visible for direct unit pinning. */
    static int skillBand(int skill) {
        if (skill <= SKILL_LOW_MAX) return 0;
        if (skill >= SKILL_HIGH_MIN) return 2;
        return 1;
    }

    /** Display name for the log (e.g. "Spear"). The shipped set is one weapon per category, so the
     *  category name reads naturally; a richer per-weapon name is content for the full table. */
    public String displayName() {
        String n = category.name().toLowerCase();
        return Character.toUpperCase(n.charAt(0)) + n.substring(1);
    }
}

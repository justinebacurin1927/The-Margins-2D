---
baseline_commit: 07641f5
---

# Story 4.4: Weapon durability and gear-with-memory

Status: ready-for-dev

## Story

As Klein,
I want weapons to wear with use and repairs to permanently cost their ceiling,
so that gear is precious and fighting spends a finite, non-renewable resource (FR-13, AD-13).

## Acceptance Criteria

**AC-1 — Weapons wear per action and break at 0.**
**Given** a wielded weapon **When** Klein attacks or blocks with it **Then** it loses a fixed amount of durability per action; at 0 durability it is **broken** and unusable — Klein falls back to unarmed (base STR), and the break is observed in the log. (Weapons are first-class instances with per-item durability; the full ~30-weapon table across 5 categories × 5 tiers is content deferred to a later pass — this story ships a minimal representative set that proves the mechanism.)

**AC-2 — Repair restores durability but permanently lowers max on the SKILL-modified AD-13 curve.**
**Given** a weapon and a repair **When** `repair(skill)` is applied **Then** durability is restored to the new maximum, but that maximum is **permanently lowered** on the AD-13 decay curve keyed to the repair count and the SKILL band — Fresh 100% → 1st `90/93/96` → 2nd `78/84/91` → 3rd `65/74/85` → 4th `50/63/78` → 5th `35/51/70` (Low/Mid/High SKILL) → 6th+ **beyond repair** (a further repair is refused). This is the model mechanism; the *reachable* repair action and its material cost are Story 4.5 (see D4).

**AC-3 — A weapon is run-scoped persistent state, AD-6-safe, with no regression.**
**Given** the new weapon model **When** a run saves/loads **Then** each weapon's durability/max/repair-count round-trips (AD-6); a pre-4.4 save with no weapon field loads as **unarmed** (empty weapon list, nothing wielded) — the deterministic default, so no existing combat behavior changes. **And** with nothing wielded, attack damage stays exactly base STR (no regression in existing combat tests).

## Scope decisions (confirmed with Justine, 2026-08-13)

- **D1 — Weapons are first-class instances.** A `Weapon` is an instance object (category, tier, `durability`, `maxDurability`, `repairCount`, and the original ceiling), held in a run-scoped `List<Weapon>` on `RunState` with a `wieldedIndex` reference. This is the correct model for AD-13 per-weapon memory (this sword is 43/90, repaired twice) and for 4.5's scavenge-on-break — the flyweight int-ordinal `Inventory` (Supply stacks) cannot carry per-item mutable state and is left for consumables.
- **D2 — Minimal representative weapon set; the ~30-weapon table is deferred content.** Ship 2–3 weapons spanning categories and tiers (e.g. **Spear T1, Blade T3, Bow T5**) — enough to exercise per-tier durability/damage and the repair curve end-to-end. The full 5 categories × 5 tiers (~30) is a content pass (a later story), matching 4.1 (AG stat, content deferred) / 4.2 (parley mechanism) / 4.3 (density ramp).
- **D3 — A wielded weapon adds damage; broken = unarmed.** Combat damage becomes `STR + (wielded && !broken ? tierBonus : 0)`. At 0 durability the weapon is broken → no bonus, Klein fights unarmed at base STR. This makes breaking actually cost something (FR-13 "gear is precious"). A contained change on the existing flat-STR path; **unwielded is the default**, so existing STR-only combat tests do not regress.
- **D4 — 4.4 ships the repair *curve* (model + tests); the reachable repair + materials are Story 4.5.** `Weapon.repair(skill)` implements the AD-13 max-lowering curve as a pure model method, unit-tested. The bound repair action, weapon-specific material consumption (Wood+Rope / Metal Scrap / …), and scavenge-on-break are **Story 4.5** where the epic puts them. Clean 4.4/4.5 seam.

### Stated defaults (not asked — flagged for the record)

- **Durability decrements on ATTACK + BLOCK only.** `CHOP` and `THROW` (named in the epic AC) are **not real actions today** — `PlayerAction` has no such kinds. Wiring them is out of scope; when they exist they route through the same `Weapon.decay` hook.
- **Repair materials + scavenge-on-break = Story 4.5** (per the epic). 4.4 does not consume or return materials.
- **The O7/O8 equip-slot deferred items** (`Inventory.equip` same-type-both-slots; unequip-into-full-pack dead-end) ride with **4.5's** gear-equip UI — 4.4 wields from the new `RunState` weapon list via a dedicated action, not through the `Inventory` equip slots, so O7/O8 are untouched here.
- **Weapon loot placement** (finding weapons in the world) is deferred content — 4.4 seeds the minimal set into a new run's weapon list so the mechanism is reachable now (see reachability below).

## Baseline (what the substrate already ships — verify before adding)

- **Combat damage is flat STR — no weapon concept exists.** `CombatSystem.playerAttack` (`CombatSystem.java:29`) does `target.takeDamage(player.getStr())`. There is **no** `Weapon` class, no durability, no weapon-specific damage anywhere. You are introducing the model.
- **SKILL is a real stat (no need to add it).** `RoguePlayer.getSkill()`/`setSkill()` exist (`RoguePlayer.java:217-219`), default **5**, already used by `SurvivalCraft` (`40 + skill*8`). AD-13's Low/Mid/High SKILL columns key off this. There is **no existing Low/Mid/High band definition** — define one (see Dev Notes; PRD Balance owns the exact thresholds).
- **The Inventory is flyweight int-ordinals with 2 equip slots.** `Inventory` (`Inventory.java`) holds `int[] types`/`counts` (backpack, 8 stacks) and `int[] equipped` (2 slots), all **type ordinals** — no per-item identity. `equip(int type)`/`unequip(int slot)` move ordinals. Weapons (per-instance state) do **not** fit here — hence D1's separate `List<Weapon>`.
- **Supply has no weapons; it does have the 4.5 repair materials.** `Supply` (`Supply.java`) is consumables/materials only — `WOOD`, `ROPE`, `SMALL_TOOLS` are present (the AD-13 repair materials for 4.5). No weapon entries.
- **PlayerAction kinds:** `MOVE, ATTACK, BLOCK, DODGE, FLEE, WAIT, USE, DROP, PICKUP, DISTRACT, COLLECT, BUILD_CAMPFIRE, COOK, FILTER, BOIL, CRAFT_TORCH, LOCKPICK` (`PlayerAction.java:11-16`). **No CHOP/THROW.** You will add a `WIELD` kind.
- **Block already exists and is loud.** `TurnEngine`'s BLOCK case sets `player.setBlocking(true)` and calls `CombatSystem.blockNoise` (Story 4.2). Hook durability decay for BLOCK alongside that, and for ATTACK in `playerAttack`.
- **AD-6 serialization pattern.** New persisted fields need a deterministic field-absent default (`RunState` docs the pattern at `RunState.java:68-69`; `flagStore` loads "empty-but-non-null"). Weapons must load empty + `wieldedIndex = -1` on a pre-4.4 save.
- **The acted pipeline (AD-4) must stay intact.** Durability decay is a side effect of the existing ATTACK/BLOCK branches — it must not add a pipeline step or disturb the hunger → … → checkLastStand → FOV order.

## In / Out of Scope Seam

**In scope (4.4):**
- A `Weapon` model (pure, AD-2): `Category` enum (the 5 categories, so material tags/tiers are namable), `tier`, `durability`, `maxDurability`, `originalMax`, `repairCount`; `isBroken()`, `decay(int)`, `tierBonus()`, and `repair(int skill)` implementing the AD-13 curve. Serializable by libGDX Json (no-arg ctor / accessible fields).
- A minimal weapon factory/table for the representative set (Spear T1, Blade T3, Bow T5) — per-tier `originalMax`, `tierBonus`, fixed `decayPerAction`, and a per-category material tag (tag only; consumption is 4.5).
- `RunState`: a persisted `List<Weapon> weapons` + `int wieldedIndex` (AD-6 default: empty list, −1); `getWieldedWeapon()` (null when unarmed/broken-fallback); seed the minimal set into a **new** run (unwielded). Round-trip serialization.
- Combat: `playerAttack` damage = `STR + wielded bonus` (0 if unarmed/broken); ATTACK and BLOCK call `Weapon.decay`; a break reverts `wieldedIndex` to −1 (unarmed) and logs one line ("Your <weapon> breaks.").
- A `WIELD` `PlayerAction` + `TurnEngine` case + `MarginScreen` key bind + how-to-play legend (retro #1 reachability): wield/cycle a non-broken weapon from the list; refuse a broken one; unarmed when none.
- `Weapon.repair(skill)` (AD-13 curve) — **model + unit tests only**, not reachable in 4.4.
- Tests: durability decay + break→unarmed, damage with/without a wielded weapon, the AD-13 repair curve across all three SKILL bands incl. 6th-beyond-repair, serialization round-trip, AD-6 field-absent default, and the WIELD action gate.

**Out of scope (→ Story 4.5 / later):**
- Reachable repair action + weapon-specific material consumption + scavenge-on-break (Story 4.5).
- The full ~30-weapon content table (5×5); weapon loot placement in the world (content).
- `CHOP`/`THROW` actions; the O7/O8 `Inventory.equip` fixes (ride 4.5's equip UI); per-weapon stat scaling beyond a simple tier bonus; trader repair/economy (Epic 6).

## Tasks / Subtasks

- [ ] **Task 1 — The `Weapon` model (AC-1, AC-2, D1).**
  - [ ] 1.1 Create `Weapon` (pure model, `com.margins.rogue.item` or `.../combat`, no libGDX — AD-2): `Category` enum (SPEAR, BOW, BLADE, AXE, CLUB — the 5 categories so tiers/materials are namable even though only a few ship), `int tier`, `int durability`, `int maxDurability`, `int originalMax`, `int repairCount`. Json-serializable (no-arg ctor + fields/accessors, mirror the existing model classes).
  - [ ] 1.2 `isBroken()` = `durability <= 0`; `decay(int n)` clamps `durability` at 0; `tierBonus()` = the per-tier damage add; a `decayPerAction` constant (fixed per AC-1; PRD Balance).
  - [ ] 1.3 `repair(int skill)` — AD-13 curve (Task 6). Keep it a pure method on `Weapon`.
- [ ] **Task 2 — Minimal weapon set + tier data (AC-1, D2).**
  - [ ] 2.1 A small factory (e.g. `Weapon.spearT1()/bladeT3()/bowT5()` or a `WeaponTable`) building the representative set with per-tier `originalMax`/`tierBonus` and a per-category material tag (tag only). Keep the tier→(originalMax, tierBonus) mapping in one place, extensible to the full 5×5 later.
- [ ] **Task 3 — `RunState` integration + persistence (AC-3, D1).**
  - [ ] 3.1 Add persisted `List<Weapon> weapons` and `int wieldedIndex` (default `-1`). `getWieldedWeapon()` returns the wielded, non-broken weapon or `null`. AD-6: a field-absent save loads `weapons` empty-but-non-null and `wieldedIndex = -1` (verify no `restoreAfterLoad` reconcile needed beyond null-guarding).
  - [ ] 3.2 Seed the minimal set into a **new** run's `weapons` list, **unwielded** (`wieldedIndex = -1`) so existing combat tests (no wielded weapon) still see base-STR damage. Round-trip test: durability/max/repairCount/wieldedIndex all reproduce.
- [ ] **Task 4 — Combat wiring (AC-1, AC-3, D3).**
  - [ ] 4.1 `playerAttack`: damage = `STR + (w != null ? w.tierBonus() : 0)` where `w = getWieldedWeapon()`. Then `w.decay(decayPerAction)`; if it just broke, set `wieldedIndex = -1` and add "Your <name> breaks." (one line, observation discipline).
  - [ ] 4.2 BLOCK path (`TurnEngine` BLOCK case, beside `blockNoise`): decay the wielded weapon the same way, same break handling. Do not add a pipeline step; this rides the existing acted branches (AD-4 order untouched).
- [ ] **Task 5 — Reachable WIELD action (AC-1, retro #1).**
  - [ ] 5.1 Add `PlayerAction.Kind.WIELD` + a factory; `TurnEngine` case that selects/cycles `wieldedIndex` over the non-broken weapons in `RunState.weapons` (refuse a broken weapon with a terse line; unarmed when the list is empty). Costs a turn or not — match the existing convention for equip-like actions (no combat resolved), dev's call, documented.
  - [ ] 5.2 Bind a key in `MarginScreen.readAction` (propose a currently-unbound key; **verify no collision** — Q/H/R/X/E/P/T/L are taken) + a legend row in the EXPLORE how-to-play. Reachability audit: wield → attack → break → unarmed is playable start to finish.
- [ ] **Task 6 — The AD-13 repair curve (AC-2, D4).**
  - [ ] 6.1 Implement `repair(int skill)`: map `repairCount` (0=fresh) + SKILL band to the new max % of `originalMax` — 1st `90/93/96`, 2nd `78/84/91`, 3rd `65/74/85`, 4th `50/63/78`, 5th `35/51/70` (Low/Mid/High). When `repairCount >= 5` a further repair is **refused** (6th+ beyond repair) — return a signal/false, do not mutate. On success: `maxDurability = round(originalMax * pct)`, `durability = maxDurability`, `repairCount++`.
  - [ ] 6.2 Define the SKILL bands (Low/Mid/High) as named constants (proposal: Low `skill<=3`, Mid `4..6`, High `skill>=7`; default 5 = Mid). PRD Balance owns the exact cut — document it. **Not reachable in 4.4** (4.5 binds the action + materials).
- [ ] **Task 7 — Tests + verification (all ACs).**
  - [ ] 7.1 AC-1: decay reduces durability per action; at 0 `isBroken()`; a wielded weapon that breaks on attack reverts to unarmed and logs once; block decays too.
  - [ ] 7.2 AC-3/D3: `getWieldedWeapon()==null` → attack damage is base STR (no regression); wielded non-broken → `STR + tierBonus`; broken → back to STR.
  - [ ] 7.3 AC-2/D4: the repair curve for all three bands across repairs 1–5 (assert the exact `maxDurability` from the table), the 6th refused (beyond repair, no mutation), and durability restored to the new max each time.
  - [ ] 7.4 AC-3: serialization round-trip (durability/max/repairCount/wieldedIndex) and the AD-6 field-absent default (a save without the weapon field → empty list, `-1`, unarmed, base-STR damage).
  - [ ] 7.5 WIELD gate: wields a non-broken weapon, refuses a broken one, no-op when the list is empty; key-bound + in the legend.
  - [ ] 7.6 Full suite green via `docs/BUILD.md` (`mvn -o clean install`), no regressions (currently 438 tests). **Verify:** all green, boot clean.

## Dev Notes

### Current state (what exists, to preserve)

- **`CombatSystem` is the single combat authority (AD-4/AD-9).** Keep all HP mutation in `CombatSystem`; the weapon only changes the damage *value* and its own durability. Do not let `TurnEngine`/`MarginScreen` mutate weapon or HP state directly — route through `CombatSystem`/`RunState`.
- **Base-STR damage is the regression guard (AC-3/D3).** With nothing wielded, `playerAttack` must deal exactly `getStr()`. Seed the starter weapons **unwielded** and keep the `w == null` branch returning base STR, so `CombatTest`/`CombatActionsTest` stay green without edits. If any existing test wields, it opted in.
- **Observation discipline (1.8/2.1).** A break logs exactly one line; wielding logs one line; a refused wield/repair logs one terse line or is silent. No silent state changes to weapons the player would notice.
- **AD-6 default, done right (1.1 saveVersion lesson).** Add the field AND its field-absent read-branch together: a pre-4.4 save with no `weapons` key loads an empty list + `wieldedIndex = -1`. Add a `SaveMigrationTest`-style pin and a round-trip pin in the same pass — never a dead default.
- **AD-5 determinism.** Seeding a new run's weapon set must not draw from the seeded `rng` mid-stream in a way that shifts existing layout draws (the starter set is authored, not rolled — build it deterministically without touching `rng`, or after the existing draw sequence, mirroring how 4.3 kept the count decision rng-free).

### The AD-13 curve (verbatim, implement exactly)

```
repairCount after repair →   1st     2nd     3rd     4th     5th     6th+
Low  SKILL (%)               90      78      65      50      35      beyond repair
Mid  SKILL (%)               93      84      74      63      51      beyond repair
High SKILL (%)               96      91      85      78      70      beyond repair
```
Percentages are of `originalMax`. `maxDurability = Math.round(originalMax * pct/100f)`; `durability = maxDurability`. Fresh = 100% (0 repairs). The 6th repair is refused for every band ("6th+ beyond repair"); the epic's "Low SKILL hard-stops at the 6th" is this same hard stop (Low just reaches the harshest floor). If PRD Balance later wants Low to stop *earlier*, that's a one-line change — flag it, don't guess.

### AD / architecture references

- FR-13 (weapons wear; repairs permanently cost the ceiling; gear is precious) — `[Source: epics.md:556-566]`
- AD-13 (repairs permanently lower max, SKILL-modified decay curve; SKILL-based repair with weapon-specific materials) — `[Source: architecture/.../ARCHITECTURE-SPINE.md:145-149]`
- AD-2 (the `Weapon` descriptor stays pure — no libGDX in the model) — `[Source: DialogEffect.java header pattern]`
- AD-4 (durability is a side effect of the existing ATTACK/BLOCK acted branches — no new pipeline step) — `[Source: TurnEngine.java acted path]`
- AD-6 (field-absent save → deterministic default: empty weapons, unarmed) — `[Source: RunState.java:68-69]`
- SKILL stat (the repair-curve axis; also cooking/purification) — `[Source: RoguePlayer.java:217-219, SurvivalCraft.java:18]`

### Previous-story intelligence (4.1 / 4.2 / 4.3)

- **Mechanism-now / content-later is the house pattern.** 4.1 shipped the AG stat (content-inert), 4.2 the parley mechanism (faction content deferred), 4.3 the density ramp (inert until Epic 5). 4.4 is the same: the durability + repair-memory *mechanism* + a minimal set; the ~30-weapon table, materials, scavenge, and loot placement are later. State this in the completion notes so review scores the mechanism, not the content breadth.
- **Reachability gate (retro #1).** Every new `PlayerAction` must be key-bound + in the legend + audited — 4.1 (H/R/X), 4.2 (P) did this. The new `WIELD` action must be reachable, and the whole loop (wield → wear → break → unarmed) must be playable, not test-only.
- **Package-private test seam is acceptable (4.3).** If a pure helper (e.g. the curve table lookup) is easier to pin directly, package-private + an in-package test is an accepted, documented seam.
- **Co-locate with authority (4.1/4.2).** Weapon durability decay lives at the combat site (`CombatSystem`/the BLOCK branch), not scattered; detection/HP authorities stay where they are.

### Project Structure Notes

- New/edited files (expected): `rogue/item/Weapon.java` (new) + a small factory/table; `rogue/state/RunState.java` (weapons list + wieldedIndex + persistence + starter set); `rogue/system/CombatSystem.java` (damage + decay + break); `rogue/system/TurnEngine.java` (BLOCK decay + WIELD case); `rogue/system/PlayerAction.java` (WIELD kind); `MarginScreen.java` (key + legend); new/updated tests under `core/src/test/...` (a `WeaponDurabilityTest` + a `WeaponRepairCurveTest`, plus combat/persistence coverage).
- Build/verify: `docs/BUILD.md` — `mvn -o clean install` (CI-truth, full suite); `mvn -o -pl core test -Dtest=<Class>` for a single class; `exec:java` needs `mvn -o -pl core install` first + a display.

## Dev Agent Record

### Agent Model Used

Claude Opus 4.8 (1M context) — create-story 2026-08-13.

### Debug Log References

_(none yet — populated during dev-story)_

### Completion Notes List

_(populated during dev-story)_

### File List

_(populated during dev-story)_

## Change Log

- 2026-08-13 — created by create-story. Scope confirmed with Justine: D1 (first-class `Weapon` instances in a `RunState` list + `wieldedIndex` — the flyweight int-ordinal inventory can't carry per-item durability), D2 (minimal representative set Spear T1 / Blade T3 / Bow T5; the ~30-weapon 5×5 table is deferred content), D3 (a wielded weapon adds a tier damage bonus, broken = unarmed at base STR — makes breaking cost something; unwielded default preserves the STR-only combat tests), D4 (4.4 ships the AD-13 repair *curve* as a model method + tests; the reachable repair action + weapon-specific materials + scavenge-on-break are Story 4.5). Defaults recorded: durability decays on ATTACK+BLOCK only (CHOP/THROW aren't real actions), O7/O8 equip fixes ride 4.5's equip UI, weapon loot placement is deferred content (the minimal set is seeded into a new run so the mechanism is reachable via a new key-bound WIELD action). Substrate audit: no `Weapon`/durability exists today (combat is flat STR); SKILL is already a real stat for the curve; the Inventory equip slots are int-ordinal and unsuited to per-item state. Status → ready-for-dev.

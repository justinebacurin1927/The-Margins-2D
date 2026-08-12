---
baseline_commit: 619edc1
---

# Story 4.1: Combat resolves at the actor's point via CombatSystem

Status: review

## Story

As Klein,
I want to fight with a real action set in turn order,
So that combat is a genuine option with clear resolution (FR-12, AD-4).

## Acceptance Criteria

**AC-1 (FR-12, turn order by AG + five-action set):** Given hostiles in range, when combat runs, then turn order is by AG (higher acts first), the action set is Attack / Block / Dodge / Use Item / Flee, and all damage routes through the single `CombatSystem` authority.

**AC-2 (AD-4, dead-before-act):** Given I kill an enemy with my attack, when the pipeline continues that turn, then the dead enemy never acts later in the turn (dead-before-act, AD-4); no HP is mutated outside `CombatSystem` for combat.

**AC-3 (Project Lead decision 2026-08-10, AG as a real stat):** Given a run (new or resumed), when the stat block loads, then AG exists as a real 6th stat on `RoguePlayer` (a persisted field with a deterministic default — AD-6), serializes round-trip, and the dodge/evasion math (and Trembling's "-15% Agility" penalty) references the real AG field — not a renamed stand-in for another stat.

**AC-4 (Epic 3 retro action item #1, screen-reachability gate):** Given the five combat actions, when the game is played, then every combat `PlayerAction` is reachable from keyboard input (no test-only-reachable feature) and listed in the how-to-play legend.

## Scope decision (confirmed with Justine, 2026-08-10)

Two scope decisions were confirmed at 4.1 create-story time (Project Lead), plus one stat-value decision:

**D1 — Pipeline-preserving AG (not a true initiative reorder).** AG becomes the evasion/initiative stat *within* the existing fixed pipeline (AD-4): (a) the player's dodge is refactored to be AG-derived (the code already names the Trembling penalty "-15% Agility"); (b) within `CombatSystem.enemyPhase`, living enemies act in descending AG order. The player's own combat action still resolves first in their turn-step — their AG advantage as a trained knight. This keeps AD-4's fixed acted-pipeline structurally intact and is minimal/low-risk. **Rejected:** a true initiative system that reorders player-vs-enemy turns (higher AG acts before the player) — that re-architects the acted pipeline, breaks every existing combat test, and contradicts the "player acts at their point, then the enemy phase" shape the codebase ratifies.

**D2 — The full FR-12 five-action set, delivered real-but-minimal.** Attack (exists, Q), Block (exists core-side, gains a UI key), Dodge (new action), Use Item (the existing USE action, pinned in combat), Flee (new action). All five are genuinely usable this story; none is a stub.

**D3 — AG values (behavior-preserving, AD-5-safe):** `RoguePlayer.ag = 7` and `RogueEnemy.ag = 3`. Player dodge is `ag × 3` (the code's existing `dodgePercent()` shape), so **AG=7 reproduces today's exact 21% base dodge** (instinct was 7 × 3) — the refactor is value-preserving, provable by a pinned test, and shifts no seeded rng stream. Enemy AG=3 is the slow-Giliman default; the AG sort is stable, so equal-AG enemies keep insertion order — no existing enemy-phase test changes behavior.

## Baseline (what the substrate already ships)

The `666bcb2` baseline already provides most of the combat frame. **Story 4.1's build is thin; its ACs are mostly pins over existing structure.**

- **`CombatSystem` (AD-2, AD-4) is the single combat authority.** `playerAttack(state, dir, messages)` (`CombatSystem.java:21-33`) resolves the 8-dir aimed melee — `target.takeDamage(player.getStr())` — and emits radius-4 noise. `enemyPhase(state, messages)` (`CombatSystem.java:48-91`) runs every living enemy: ALERTED enemies pursue/attack (arrival-grace, `justArrived` skips one turn; dodge via `player.tryDodge`; armor via `takeDamage`; the living companion blocks the enemy's path and takes the strike instead); SUSPICIOUS enemies investigate; UNAWARE enemies wander. `checkLastStand` (99-107) is the post-damage 1-HP floor, once per run. `enemyAt` (109-114) finds the living enemy on a tile. **All combat HP mutation already lives inside `CombatSystem`** (`target.takeDamage` in `playerAttack`, `player.takeDamage` / `companion.takeDamage` in `enemyPhase`) — AC-2's "no HP mutated outside CombatSystem for combat" is true by construction and is a **pin**, not a build.
- **Dead-before-act (AC-2) is structurally guaranteed today.** The player's attack resolves in the acted step (`TurnEngine` `case ATTACK:` → `CombatSystem.playerAttack`, `TurnEngine.java:80-83`), *before* `enemyPhase` runs; `enemyPhase` skips dead enemies (`if (!e.isAlive()) continue;`, `CombatSystem.java:57`). A killed enemy cannot act later that turn. **Pin, not build** — the story proves it and then guards the AG-sort against breaking it.
- **BLOCK already exists core-side.** `PlayerAction.block(dir)` factory exists (`PlayerAction.java:44-46`) and `TurnEngine` `case BLOCK:` sets `player.setBlocking(true)` + "Brace!" with `acted = true` (`TurnEngine.java:84-88`). `RoguePlayer.takeDamage` halves damage while `blocking` and clears it (`RoguePlayer.java:264-273`). **But BLOCK has no UI trigger** — `B` is bound to `buildCampfire` (`MarginScreen.java:739`), and no other key produces a BLOCK. This is exactly the screen-reachability gap the Epic 3 retro flagged; 4.1 binds a key.
- **The player's dodge is instinct-derived, but the code already calls it Agility.** `dodgePercent()` = `eff × 3` where `eff = instinct`, ×0.85 each for Trembling and Delirium (`RoguePlayer.java:242-251`); `isTrembling()` (`254-258`) is the "-15% AG" condition the doc comment names. **`RoguePlayer` ships STR / INSTINCT / GRIT / VOICE / SKILL — there is no `ag` field** (fields at `RoguePlayer.java:146-150`, ctor values 166-170). AG is the missing 6th stat (architecture "Stats & status" convention; Epic 3 retro discovery).
- **`RogueEnemy` ships no AG either.** Stats are `hp=8, maxHp=8, damage=3` (`RogueEnemy.java:27-29`). Its Json deserialization runs the private no-arg ctor (`RogueEnemy.java:21`) which does **not** set stats — a new field must be **field-initialized** (`private int ag = 3;`) so a pre-4.1 save (no `ag` key) loads deterministically (AD-6). `takeDamage` (`43-46`) flips `alive=false` at 0.
- **`PlayerAction` has no DODGE or FLEE kinds.** The `Kind` enum (`PlayerAction.java:10-20`) has MOVE/ATTACK/BLOCK/WAIT/USE/DROP/PICKUP/DISTRACT/COLLECT/BUILD_CAMPFIRE/COOK/FILTER/BOIL/CRAFT_TORCH/LOCKPICK. USE already works during combat — nothing gates it (`TurnEngine` `case USE:` routes provisions through `ConsumptionSystem.consume`, narration through the identity path). So "Use Item" is real; 4.1 pins it.
- **The acted pipeline is fixed (AD-4).** The turn order is: player's chosen action (acted step) → Hunger → Thirst → Debuff → Temperature → Spoilage → tickClock → Detection → Companion follow → `CombatSystem.enemyPhase` → Torch → Light → NoiseSystem.resolve → `checkLastStand` → FOV (`TurnEngine`). The player always resolves their action first, then the enemy phase. 4.1 must not disturb this order.
- **Serialization / migration rig exists.** `RunState` is libGDX Json with `SAVE_VERSION = 1` (`RunState.java:38`), `saveVersion` persisted (74), and `restoreAfterLoad()` (`313-349`) re-wires rng/map and reconciles the documented field-absent warts (weather, cycleNumber, identifyMap, structureLootPlaced) — the AD-6 read-branch precedent. `RunStatePersistenceTest` + `SaveMigrationTest` prove round-trip and reject-by-absence.
- **Screen surface:** `MarginScreen.readAction` (`MarginScreen.java:725-758`) binds Q (attack), WASD/arrows (move), G (pickup), SPACE (wait), C (collect), B (campfire), T (torch), K/F/V (cook/filter/boil, conditional), L (lockpick), E (use, conditional), [ / ] (cycle selection). **R, X, H are free** — natural for Roll/Dodge, eXit/Flee, and Hold/Block. `renderHowToPlayPage` (`2547-2578`) lists the SURVIVE column; the legend must gain the three combat keys.
- **Tests:** `CombatTest` (14 methods — 8-dir aimed melee, actor-tile exclusivity, companion fight/regroup/fall, alerted priority) + the 398-test core suite. The pinned-seed determinism pattern (`hazardsFireAcrossSeedsAndHonorAd5`, the 3.5 SKILL-clamp pin, the 3.1 behavior-preserving refactor pin) is the established way to prove AD-5-safety.

**What the baseline does NOT have — Story 4.1's actual scope:**

- **No AG stat** on player or enemy (AC-3 — the retro-mandated build).
- **No Dodge or Flee actions** (AC-1).
- **Block is test-reachable only** — no key (AC-4).
- **No pins** for turn-order-by-AG, dead-before-act, or "no HP outside CombatSystem" (AC-1/AC-2).

## In/Out of Scope Seam

**IN:**

- **AG as a real 6th stat** (AC-3): `RoguePlayer.ag` (default **7**, field-init + ctor-set, AD-6-safe) with `getAg()`; `dodgePercent()` refactored to use `ag` (value-preserving: 7 × 3 = the current 21% base; Trembling/Delirium −15% each unchanged); `RogueEnemy.ag` (default **3**, **field-initialized** for the no-arg Json ctor, AD-6) with `getAg()`. Serialization round-trip test + a migration test (a pre-4.1 save without `ag` loads the deterministic default — the AD-6 read-branch, saveVersion discipline).
- **Turn order by AG** (AC-1): `enemyPhase` iterates living enemies in **descending AG order** (stable sort — equal-AG enemies keep insertion order). Player's action still resolves first in the acted step (D1).
- **The five-action combat set, all real** (AC-1):
  - **Attack** — exists; ratify with a route-through-CombatSystem pin (no build).
  - **Block** — core exists; bind a UI key (**H**) and list it in the legend (no core change unless a review finds one).
  - **Dodge** — new `PlayerAction.DODGE` + `TurnEngine` case + a one-turn evading flag on the player; while evading, enemy attacks that turn dodge at a boosted AG-scaled rate (`min(90, dodgePercent() × 2)`), cleared at turn end; message on entry. No noise (silent evasion — 4.2 owns noise).
  - **Use Item** — exists; pin that USE resolves with hostiles in range and commits the turn (enemy phase still runs).
  - **Flee** — new `PlayerAction.FLEE` + `CombatSystem.flee(state, messages)`: move to the walkable cardinal neighbor that strictly maximizes distance from the nearest living enemy (tie-break by the standard move order, dir 0/1/2/3), committing the turn; if no strictly-farther tile exists, refuse with "No way out!" and spend **no turn** (the inert-USE / wall-bump precedent, AD-5).
- **AC-2 pins**: dead-before-act (a killed enemy never acts later that turn — structural today, proven by a seeded test) and "no HP mutated outside CombatSystem for combat" (a reflection/behavior pin auditing that combat HP mutation lives only in `CombatSystem`).
- **Screen wiring + reachability** (AC-4): bind **H = Block, R = Dodge, X = Flee** in `MarginScreen.readAction`; add the three keys to `renderHowToPlayPage`; full-suite + boot verification (no test-only-reachable combat action).

**OUT (later combat stories / already shipped):**

- **Combat noise** (block-draws-reinforcements, UNAWARE→SUSPICIOUS retarget, VOICE de-escalation) → **Story 4.2** (AD-9). 4.1 adds **no** new noise behavior; the existing radius-4 `playerAttack` noise stays exactly as-is.
- **Occupation escalation / act-gating density** → **Story 4.3** (AD-11 channel a). 4.1 adds no escalation; enemy spawn densities are untouched.
- **Weapon durability / gear-with-memory** (FR-13/AD-13) → **Stories 4.4/4.5**. "Every attack costs Durability" is 4.4 — 4.1's attack spends **no** durability; no gear state changes.
- **Permadeath / GRIT-based Last Stand roll / save-clear** (FR-14) → **Story 4.6**. 4.1 ratifies the existing post-damage `checkLastStand` floor but adds no GRIT roll and no save-clear.
- **Combat-numbers tuning** (damage/dodge/block balance) → the PRD §8/Q6 deferred tuning pass; 4.1 keeps every existing number (only re-derives dodge from AG at the identical 7×3 rate).
- **New enemy types / enemy variety** → later (Epic 4 re-triage, hazard-variety). 4.1 adds no enemy types; the AG order operates on the existing enemies.
- **Companion combat** (a full action for Aldric) → Epic 5. The existing companion-block/companion-takeDamage behavior in `enemyPhase` is preserved, not extended.

## Tasks / Subtasks

- [x] **Task 1 — AG as a real 6th stat (AC-3, feeds AC-1).**
  - [x] 1.1 `RoguePlayer`: add `private int ag` **field-initialized** to `7` (AD-6 deterministic default for the Json no-arg ctor) AND set `this.ag = 7` in the parameterized ctor (matching the str/instinct/... pattern); add `getAg()`. Refactor `dodgePercent()` (`RoguePlayer.java:242-251`) to use `ag` in place of `instinct` (`int eff = ag;`) — the Trembling/Delirium −15% factors are unchanged. **Verify:** `dodgePercent()` at AG=7 returns exactly 21% (the old instinct=7 value) — value-preserving, AD-5-safe.
  - [x] 1.2 `RogueEnemy`: add `private int ag = 3;` **field-initialized** (the no-arg Json ctor at `RogueEnemy.java:21` does not set stats, so field-init is the only AD-6-safe default) AND set `this.ag = 3` in the parameterized ctor; add `getAg()`.
  - [x] 1.3 Serialization + migration (AD-6 read-branch): extend the round-trip test to cover the `ag` fields; add a migration test — a pre-4.1 save serialized without the `ag` keys loads with `ag == 7` (player) / `ag == 3` (enemy), deterministic, and `restoreAfterLoad()` needs no new reconcile (the field-init default handles it — prove that, don't assume). **Verify:** round-trip + migration green.

- [x] **Task 2 — Turn order by AG (AC-1).**
  - [x] 2.1 `CombatSystem.enemyPhase`: iterate the living enemies in **descending AG** order (a stable sort on `e.getAg()`, so equal-AG enemies keep their current insertion order — no existing test's order changes). The player's action still resolves first in the acted step (D1 — no pipeline change).
  - [x] 2.2 Pin turn-order: a seeded test with two ALERTED enemies at different AG adjacent to the player — the higher-AG one acts (attacks/messages) before the lower-AG one, while both alive. And a same-AG test proving insertion order is preserved (the stable-sort guarantee). **Verify:** both pins green.

- [x] **Task 3 — The five-action combat set, all real (AC-1).**
  - [x] 3.1 **Attack (ratify):** no build. Pin that `PlayerAction.attack` routes through `CombatSystem.playerAttack` and mutates enemy HP only there (the existing `CombatTest` melee tests already cover this — add an explicit "damage applied via CombatSystem" assertion if not present).
  - [x] 3.2 **Block (bind a key):** bind **H = Block** in `MarginScreen.readAction` (`MarginScreen.java:725-758`) alongside the other action keys; no core change unless review finds one. **Verify:** pressing H while an enemy is adjacent produces `PlayerAction.block` and the existing "Brace!" message + damage-halving path runs (existing `CombatTest` blocking coverage).
  - [x] 3.3 **Dodge (new action):** add `Kind.DODGE` + `PlayerAction.dodge(dir)` to `PlayerAction.java`; `TurnEngine` `case DODGE:` sets a one-turn evading flag on the player (`player.setEvading(true)` — a transient, non-persisted boolean mirroring the blocking shape but cleared at turn end), message ("You sidestep, ready."), `acted = true`. In `CombatSystem.enemyPhase`, when an enemy would hit the player, resolve the dodge at the boosted rate `min(90, player.dodgePercent() × 2)` while evading; clear evading after the enemy phase completes. **Verify:** a seeded pin — a Dodge turn dodges hits that a non-Dodge turn at the same seed takes (boost observable), and the flag clears so the next turn's dodge is back to base.
  - [x] 3.4 **Use Item (ratify + pin):** no build — nothing gates USE during combat. Pin that using a provision while ALERTED enemies are adjacent commits the turn (`acted = true`) and the enemy phase still runs (an adjacent enemy can attack after the use). **Verify:** pin green.
  - [x] 3.5 **Flee (new action):** add `Kind.FLEE` + `PlayerAction.flee(dir)` to `PlayerAction.java`; add `CombatSystem.flee(state, messages)` returning whether a turn was spent. Rule: among the 4 cardinal walkable neighbors that are not occupied by a living enemy, move to the one that **strictly maximizes** distance to the nearest living enemy (tie-break by move order dir 0/1/2/3); if no strictly-farther tile exists (already maximally far / boxed in), refuse with "No way out!" and spend **no turn** (the inert-USE precedent). **Verify:** a pin — an adjacent enemy + an open retreat tile → the player moves to the farthest tile and the turn commits; boxed in → refused, no turn spent, position unchanged.

- [x] **Task 4 — CombatSystem authority + dead-before-act pins (AC-2).**
  - [x] 4.1 Dead-before-act pin: a seeded test where an ALERTED enemy adjacent to the player would attack this turn — the player's attack kills it first (damage enough to reach 0), and the enemy **never acts** later that turn (no attack message, no HP loss). Confirm the AG sort (Task 2) does not regress this. **Verify:** pin green.
  - [x] 4.2 "No HP outside CombatSystem" pin: a reflection/behavior pin (the 3.5 AC-1 pattern) auditing that the only combat HP mutations are `target.takeDamage` (`CombatSystem.playerAttack`), `player.takeDamage` / `companion.takeDamage` (`CombatSystem.enemyPhase`), and `checkLastStand`'s `reviveTo` — and that no `TurnEngine` combat case (ATTACK/BLOCK/DODGE/FLEE) mutates HP directly. **Verify:** pin green.

- [x] **Task 5 — Screen wiring + reachability gate + verification (AC-4, retro action item #1).**
  - [x] 5.1 Bind the combat keys in `MarginScreen.readAction`: **H = Block**, **R = Dodge**, **X = Flee**. Order them with the existing combat key (Q) and the survival keys without shadowing anything.
  - [x] 5.2 Update `renderHowToPlayPage` (`MarginScreen.java:2547-2578`) — the SURVIVE column gains `H  Brace`, `R  Dodge`, `X  Flee` (next to `L  Lockpick`).
  - [x] 5.3 Reachability audit: for each of the five combat actions, confirm a key produces it (Q / H / R / E / X) — no test-only-reachable combat feature. Then full suite (`mvn -o -pl core test`, the `docs/BUILD.md` recipe) + a clean boot via the harness (`mvn -o -pl core install` before `exec:java`). **Verify:** 398+ core tests green, boot clean.

## Dev Notes

### Current state (what exists, to preserve)

- **CombatSystem is the single authority (AD-4).** `playerAttack` (`CombatSystem.java:21-33`) — 8-dir aimed melee, damage = `player.getStr()`, `target.takeDamage`, emits radius-4 noise. `enemyPhase` (`48-91`) — ALERTED pursue/attack with arrival-grace (`justArrived`), dodge via `player.tryDodge(rng)` (which calls `dodgePercent()`), armor via `player.takeDamage` (`max(1, amount − grit/2)`, halves while `blocking` then clears), the living companion blocks the enemy's path and takes the strike, dead enemies are skipped (`57`). `checkLastStand` (`99-107`) — post-damage 1-HP floor, once per run. `enemyAt` (`109-114`). All combat HP mutation lives inside `CombatSystem`.
- **The acted step and its invariants (AD-4).** `TurnEngine` routes the player's action in the acted step — `case ATTACK:` → `CombatSystem.playerAttack` (`TurnEngine.java:80-83`), `case BLOCK:` → `player.setBlocking(true)` + "Brace!" (`84-88`), `case USE:` → `ConsumptionSystem.consume` / narration / identify paths. After the acted step, the pipeline runs in order: Hunger → Thirst → Debuff → Temperature → Spoilage → tickClock → Detection → Companion follow → **enemyPhase** → Torch → Light → NoiseSystem.resolve → checkLastStand → FOV. **Every new combat action (DODGE/FLEE) is an acted branch that must set `acted = true` and resolve in the acted step — never inside or after `enemyPhase`.**
- **AG is missing but pre-named.** `RoguePlayer` ships STR/INSTINCT/GRIT/VOICE/SKILL (`RoguePlayer.java:146-150`, ctor 166-170); `dodgePercent()` uses `instinct` but its doc comment and `isTrembling()` already call the penalty "-15% Agility" (`242-258`). The six-stat target (STR/GRIT/INS/AG/VOICE/SKILL) is the architecture "Stats & status" convention.
- **AD-6 is the load-bearing migration rule.** Every new persisted field needs a deterministic default OR a load-time reconcile in `restoreAfterLoad()`. `RogueEnemy`'s Json no-arg ctor does **not** set stats — so `RogueEnemy.ag` MUST be **field-initialized** (`private int ag = 3;`). `RoguePlayer.ag` likewise field-init (`private int ag = 7;`). A pre-4.1 save then loads deterministic AG with no reconcile.
- **The dodge value is pinned by precedent.** `dodgePercent() = eff × 3`, eff = instinct=7 → 21% base today. AG=7 keeps the identical 21% — the refactor is behavior-preserving, provable by a pinned test (the 3.1 `placeStructureLoot`→`placeLootInFootprint` precedent, and the 3.5 SKILL-clamp pin).
- **The stable AG sort is the AD-5-safe way to order the enemy phase.** Java's `List.sort` is a stable TimSort — equal-AG enemies keep insertion order, so no existing multi-enemy test changes behavior. One seeded rng draw per event stays the rule (AD-5); ordering is not an rng draw.
- **Flee refusal spends no turn (the inert-USE precedent).** The codebase consistently refuses no-op actions without spending a turn: wall bumps, inert USE, LOCKPICK refusals. Flee-boxed-in follows the same shape.
- **Reachability is a done-gate.** The Epic 3 retro's sharpest miss was 3.5's LOCKPICK/MAP_FRAGMENT being core-green but UI-unreachable. 4.1 must not repeat it: H/R/X bound and in the legend before the story is done.
- **Save/load rig:** `RunState` Json, `SAVE_VERSION = 1` (`RunState.java:38`), `saveVersion` persisted (74), `restoreAfterLoad()` (`313-349`) with the documented warts. `RunStatePersistenceTest` + `SaveMigrationTest` prove the round-trip and reject-by-absence.

### Carried lessons (Epic 1/3 retros + action items)

- **AG decision (Project Lead, 2026-08-10):** add AG as a real 6th stat. `ag = 7` (player) / `ag = 3` (enemy), field-init deterministic defaults (AD-6), dodge and Trembling reference the real field, serialization round-trip + migration test, folded into the horizontal-growth spine (SKILL stays the growth axis — AG is a stat, not a progression lever).
- **CombatSystem invariants (Epic 3 retro action item #5):** every combat damage route through `CombatSystem` — no HP mutation outside it for combat; dead-before-act (AD-4); the acted-branch order (hunger → … → checkLastStand → FOV) must not be disturbed by combat's turn-order changes. DODGE/FLEE are acted branches exactly like ATTACK/BLOCK.
- **Screen-reachability gate (retro action item #1):** every new `PlayerAction` must be bound to an input key before the story is done. H/R/X are free keys.
- **Observation discipline (Epic 1 retro, carried to 2.1):** every combat outcome (hit / dodge / block / flee / "No way out!") emits to the message log — no silent resolution.
- **AD-5 seeded-stream discipline:** refactors that shift the rng stream get flagged and weighed. AG=7 keeps dodge draws identical; the stable AG sort keeps enemy order identical; every new roll is one seeded draw per event.
- **AD-6 migration rule (bit twice — 1.3 weather, 3.3 identify):** field-absent saves inherit ctor-rolled state — every new persisted field needs a deterministic default OR a load-time reconcile. Both `ag` fields ship field-init defaults.

### Scope discipline (CLAUDE.md §2/§3)

- **4.2 noise, 4.3 escalation, 4.4 durability, 4.5 repair, 4.6 permadeath-GRIT-roll are NOT 4.1.** The existing `playerAttack` radius-4 noise stays; no new noise emission; no durability accounting; no GRIT save-clear. The existing `checkLastStand` post-damage floor is preserved untouched.
- **Touch only:** `RoguePlayer` (+AG, dodge), `RogueEnemy` (+AG), `PlayerAction` (+DODGE/FLEE kinds + factories), `TurnEngine` (+DODGE/FLEE cases, verify BLOCK), `CombatSystem` (enemyPhase AG sort, evading-dodge boost, `flee`), `MarginScreen` (H/R/X keys + legend), `CombatTest` (+ pins), `RunStatePersistenceTest`/`SaveMigrationTest` (+ AG coverage). Nothing else.
- **Do not "improve" adjacent combat code** — the arrival-grace, companion-block, blocked-tile, and noise behavior are all preserved exactly.
- **Simplicity first:** Attack and Use-Item are ratify-and-pin, not rebuilds. The only new production behavior is AG, the AG sort, Dodge, and Flee.

### Testing standards

- Mirror the established patterns: pinned-seed determinism tests (the 3.4 same-seed pin, the 3.5 SKILL-clamp pin), comparative tests (dodge-turn vs non-dodge-turn at the same seed), reflection pins for invariants (the 3.5 AC-1 no-XP reflection pin → the AC-2 no-HP-outside-CombatSystem pin), round-trip + migration tests for the new fields (AD-6 read-branch), and stable-sort ordering pins.
- Every new `PlayerAction` gets a reachability check: a key produces it (H/R/X) — the AC-4 gate.
- Full suite green + clean boot before the story is done (the `docs/BUILD.md` recipe: `mvn -o -pl core install` before `exec:java`).

### References

- [Source: `_bmad-output/planning-artifacts/epics.md#L504-522`] — Epic 4 intro + Story 4.1 ACs verbatim.
- [Source: `_bmad-output/planning-artifacts/prds/prd-The-Margin-2026-08-06/prd.md#§4.4`] — FR-12 (turn order by AG, five-action set, combat noise), FR-13 (durability → 4.4/4.5), FR-14 (permadeath + Last Stand → 4.6); §8/Q6 defers combat numbers tuning.
- [Source: `_bmad-output/planning-artifacts/architecture/architecture-The-Margin-2026-08-06/ARCHITECTURE-SPINE.md`] — AD-4 combat-resolves-at-the-actor's-point (line 82), AD-6 field-absent migration rule (line 96), "Stats & status" six-stat convention (line 187), AD-9 noise, AD-11 escalation channels, AD-13 gear-with-memory.
- [Source: `_bmad-output/implementation-artifacts/epic-3-retro-2026-08-10.md`] — the AG-stat decision and action items #1 (reachability gate), #2 (AG), #5 (CombatSystem invariants).
- [Source: `_bmad-output/implementation-artifacts/3-5-horizontal-progression-skill-and-knowledge.md`] — the dev-notes/scope-decision/pin-testing pattern 4.1 mirrors.
- [Source: `core/src/main/java/com/margins/rogue/system/CombatSystem.java`] — the combat authority being extended.
- [Source: `core/src/main/java/com/margins/rogue/RoguePlayer.java`, `RogueEnemy.java`] — the stat block gaining AG.
- [Source: `core/src/main/java/com/margins/rogue/system/TurnEngine.java`, `PlayerAction.java`] — the acted step + action kinds.
- [Source: `core/src/main/java/com/margins/MarginScreen.java#L725-758,2547-2578`] — the key surface and legend.
- [Source: `core/src/main/java/com/margins/rogue/state/RunState.java#L38,74,313-349`] — SAVE_VERSION + restoreAfterLoad (AD-6 precedent).
- [Source: `docs/BUILD.md`] — the offline build/verify recipe.

## Dev Agent Record

### Agent Model Used

Claude Opus 4.8 (1M context) — create-story 2026-08-10.

### Debug Log References

- `mvn -o clean install` — BUILD SUCCESS, full suite green (416 tests, 0 failures) with both modules installed.
- Two ordering-test failures on first run (`higherAgEnemyActsFirst`, `equalAgPreservesInsertionOrder`): the companion was engaging/stepping to the nearest player-threat during `CompanionSystem.follow`, so the second enemy struck no one and the "both enemies acted" assertion failed. Fixed by repositioning the companion adjacent to a player-adjacent enemy so it strikes and holds, letting the second enemy strike Aldric in the enemy phase — re-ran green.

### Completion Notes List

- **AC-1 turn order + five actions.** `CombatSystem.enemyPhase` sorts living enemies descending AG (`Comparator.comparingInt(RogueEnemy::getAg).reversed()`) before the loop; stable TimSort keeps equal-AG insertion order (D1/D3), so no existing multi-enemy behavior changes and the sort is AD-5-safe (ordering is not an rng draw). Full FR-12 action set is real and keyboard-reachable: Attack (Q), Block (H), Dodge (R), Use Item (E), Flee (X).
- **AC-2 CombatSystem authority + dead-before-act.** All combat damage routes through `CombatSystem`; `combatDamageRoutesOnlyThroughCombatSystem` reflection-guards `TurnEngine` against HP mutation, and `deadEnemyNeverActsLaterThatTurn` pins AD-4 (the AG sort can't make a corpse act — dead enemies are still skipped in the loop).
- **AC-3 AG as a real 6th stat.** Value-preserving refactor: `RoguePlayer.dodgePercent()` now reads `ag` instead of `instinct`; player `ag = 7` reproduces the old instinct=7 dodge exactly (21%), pinned by `dodgeIsDerivedFromTheRealAgStat` (AG7→21%, AG10→30%). AD-5-safe (no new/changed rng draw at the same seed up to the dodge roll).
- **AD-6 migration.** `RogueEnemy.ag` is FIELD-INITIALIZED (`private int ag = 3;`) because the no-arg Json ctor sets no stats — a pre-4.1 save would otherwise load ag=0. `preAgSaveLoadsDeterministicDefaults` strips the `ag` keys from the JSON and asserts player→7 / enemies→3; `agStatsSurviveRoundTrip` pins the round-trip.
- **Dodge action** is a one-turn evasion: `PlayerAction.dodge` sets `evading`, the enemy phase rolls the boosted chance (min(90, dodgePercent×2), `DODGE_BOOST_PERCENT = 200`), and `TurnEngine` clears `evading` after the enemy phase. **Flee** moves to the walkable cardinal neighbor strictly maximizing distance to the nearest living enemy (first-dir tie-break); boxed-in refuses with no turn (inert-USE precedent, AD-5).
- **AC-4 / retro action item #1 (reachability gate).** All five actions are bound to keys in `MarginScreen.readAction` and documented in the How-to-Play page — no test-only-reachable feature.
- **Note on landing:** implementation was committed as part of `57976cb` (another contributor's presentation/inventory overhaul swept the working tree) rather than a dedicated per-story commit; code and all 13 new tests are on `main` and green.

### File List

- `core/src/main/java/com/margins/rogue/RoguePlayer.java`
- `core/src/main/java/com/margins/rogue/RogueEnemy.java`
- `core/src/main/java/com/margins/rogue/system/CombatSystem.java`
- `core/src/main/java/com/margins/rogue/system/PlayerAction.java`
- `core/src/main/java/com/margins/rogue/system/TurnEngine.java`
- `core/src/main/java/com/margins/MarginScreen.java`
- `core/src/test/java/com/margins/rogue/CombatActionsTest.java` (new)
- `core/src/test/java/com/margins/rogue/state/RunStatePersistenceTest.java`

## Change Log

- 2026-08-10 — created by create-story (epic-3 retro action items #1/#2/#5 carried in; Project Lead scope decisions D1/D2/D3).
- 2026-08-12 — dev-story: implemented AG as a real 6th stat (value-preserving, AD-6 field-init), AG turn-order sort, the full five-action combat set (Attack/Block/Dodge/Use Item/Flee), CombatSystem authority + dead-before-act pins, screen wiring + reachability gate. Full suite green (416 tests). Status → review.

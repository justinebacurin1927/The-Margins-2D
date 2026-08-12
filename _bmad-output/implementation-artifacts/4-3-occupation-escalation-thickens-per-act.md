---
baseline_commit: 1827359
---

# Story 4.3: Occupation escalation thickens per act

Status: done

## Story

As Klein,
I want the Gilimans to grow denser as the story advances,
so that fighting gets more punishing over a run (FR-12, AD-11 channel a).

## Acceptance Criteria

**AC-1 — Act drives interior enemy density (channel a).**
**Given** the current act (read from a `FlagStore` flag, AD-7/AD-11) **When** a region's enemy count is decided at floor generation **Then** the interior/eastward count rises with act: Act 1 = today's baseline, each later act adds enemies to already-dangerous (base-count > 0) regions. The decision stays a **pure, deterministic function** of `(eastness, act)` — no rng in the count (AD-5 preserved; only per-enemy position draws touch the seeded stream, exactly as today).

**AC-2 — The ramp does NOT touch the NW border cordon (channels stay separate).**
**Given** the escalation ramp **When** it is applied **Then** it leaves the far-west/far-north NW border cordon untouched (`WorldSpine` BORDER_X/BORDER_Y corner). The west safe tier (base count 0) stays 0 at every act, and the cordon box is explicitly excluded so channel (a) can never harden AD-12's win gate — that is channel (b), owned by Epic 5 (5.7).

**AC-3 — Act flag is AD-6-safe and Act-1-default; no regression at Act 1.**
**Given** a save with no act flag (every save today) **When** it loads **Then** the act reads as **1** (the `FlagStore` empty-slot sentinel 0 maps to Act 1) — no new persisted `RunState` field, no migration. **And** at Act 1 the density is bit-identical to the current `enemyCountFor` behaviour (no existing seed's layout changes).

## Scope decisions (confirmed with Justine, 2026-08-13)

- **D1 — Act source = a `FlagStore` key, default Act 1.** Add a `FlagStore` act key (`act.current`) read through a typed accessor that maps the never-set/0 sentinel to Act 1 (AD-6 deterministic default). AD-11-compliant: **the trigger is a story-flag, not a timer or exploration counter.** Epic 5's act-gating quests ("Follow the Road" 1→2, "The Rescue" 2→3, Story 5.6) will `set()` it; 4.3 ships the read side + a test-only set path.
- **D2 — Additive per-act bump, east of the safe tier.** Density = today's base step **plus** `(act − 1)` applied only where the base count is already > 0 (the interior). Additive (not multiplicative): monotonic, no compounding blowups, no cap knob, count-0 regions stay 0. Reuse the existing eastness step as the base; do not replace it with a table.
- **D3 — Cordon excluded by an explicit far-west/far-north box.** The ramp skips the NW cordon via an explicit `inCordon(eastness, ny)` guard (`eastness < 0.2 && ny > 0.8`, the BORDER_X/BORDER_Y corner). This is deliberately belt-and-suspenders: the west safe tier already zeroes that corner (far-west → base 0 → never bumped), but naming the cordon makes AC-2 a *pinned* invariant, not an emergent accident, and documents the two-channel split at the code site.
- **D4 — 4.3 ships the mechanism only; the live trigger is an Epic 5 seam.** The map and all enemies generate **once per run** at Act 1 (`generateFloor` runs at ctor + death-restart only — there is no respawn/reinforcement path). So the ramp is a real, tested function but is **inert in live play** until Epic 5 adds a live trigger (regenerate-unexplored / a reinforcement spawner on act-flip). This mirrors how 4.1 shipped the AG stat + turn-order sort (content variety deferred) and 4.2 shipped the parley mechanism (faction content deferred): **mechanism now, wiring/content later.** Flagged up front so review does not re-file it as a surprise the way the AG-sort-inert item surfaced in 4.1. **Out of scope (→ Epic 5):** the live act-flip trigger, reinforcement/respawn spawning, sweeps/curfews/bounties content, and channel (b) the cordon-thinning (5.7).

## Baseline (what the substrate already ships — verify before adding)

- **Enemy count is already a pure step of eastness.** `RunState.enemyCountFor(float eastness)` (`RunState.java:290`) returns `0 / 1 / 2 / 3` across the `≤0.2 / <0.45 / <0.7 / else` bands — **no rng in the decision** (the comment at `RunState.java:181` pins this: "no rng in the decision, only per-enemy position draws touch the seeded stream, AD-5"). You are widening this pure function's signature to `(eastness, act, ny)`, not changing its determinism contract.
- **The spine owns the gradient.** `WorldSpine.eastness(int x)` (`WorldSpine.java:65`) = `x/(width−1)`, 0 west → 1 east; `dangerAt` == eastness. Cordon constants: `BORDER_X = 0.05f`, `BORDER_Y = 0.9f` (far-west, far-north). The class doc already names 4.3: *"occupation escalation (4.3) and the border cordon (5.7) all read it."* There is **no `northness(y)` helper** — compute `ny = cy / (height − 1f)` at the call site (or add a small spine helper, dev's call; keep it pure).
- **The placement call site.** `placeFloorActors` (`RunState.java:175-191`) loops `roomCenters`, and for each reads `int count = enemyCountFor(spine.eastness(cx))` then draws `count` per-enemy positions from `rng`. You have `cx, cy` in hand for the ramp inputs. Keep the count decision rng-free; the position draws stay exactly as they are (AD-5 call-structure unchanged).
- **FlagStore is the run-scoped narrative store (AD-7).** `FlagStore` (`FlagStore.java`) is a `LinkedHashMap<String,Integer>`; `get(key)` returns 0 for never-set keys (the empty-slot sentinel), `set`/`add` write. It already serializes under `RunState` (`RunState.java:69`, "a pre-4.3 save (no flagStore key) loads empty-but-non-null (AD-6)"). Add the act key + accessor **here** (mirror `KEY_BOND`/`getBond`), so a save with no act flag reads Act 1 by construction — **no new `RunState` field, no `restoreAfterLoad` reconcile.**
- **`RunState.getFlagStore()`** (`RunState.java:439`) exposes the store to `placeFloorActors`.

## In / Out of Scope Seam

**In scope (4.3):**
- `FlagStore` act key (`act.current`) + a typed `getAct()` accessor mapping 0/absent → 1 (AC-3, D1).
- Widen `enemyCountFor` to `(eastness, act, ny)`: base step + `(act−1)` additive bump where base > 0 and not in the cordon (AC-1, AC-2, D2/D3).
- An explicit `inCordon(eastness, ny)` guard naming the far-west/far-north box (AC-2, D3).
- Wire `placeFloorActors` to pass the act (from `getFlagStore().getAct()`) and `ny` into the count (AC-1).
- Tests: `getAct()` default/read; Act-1 base parity (no regression); per-act additive bump on interior regions; west safe tier stays 0 at all acts; cordon box stays 0/base at all acts; a `RunState`-level regen proof (same seed, act 3 vs act 1 → denser east); AD-5 determinism (same seed + same act → identical layout).

**Out of scope (→ Epic 5 / later):**
- The **live** act-flip trigger — regenerating unexplored regions or a reinforcement/respawn spawner so the ramp bites mid-run (D4). No respawn path exists today and 4.3 does not add one.
- The act-gating quests that flip the flag (Story 5.6) and channel (b) the NW cordon thinning (Story 5.7).
- Sweeps / curfews / bounties as content; per-act enemy *stat* scaling (AG/HP); supply/loot ramp (`supplyCountFor` untouched).

## Tasks / Subtasks

- [x] **Task 1 — Act flag + accessor in FlagStore (AC-3, D1).**
  - [x] 1.1 Added `public static final String KEY_ACT = "act.current";`, `getAct()` → `Math.max(1, get(KEY_ACT))` (0/never-set → Act 1, AD-6 default), and `setAct(int)` clamped to ≥1 (test path / Epic 5). Mirrors `KEY_BOND`/`getBond`; JavaDoc notes Epic 5's quests own the writes (AD-11).
  - [x] 1.2 Confirmed: no new `RunState` field, no `restoreAfterLoad` change — the flag rides the existing serialized `flagStore` map, so a pre-4.3 save reads Act 1 by construction (RunStatePersistenceTest still green, 22).
- [x] **Task 2 — Per-act, cordon-aware density function (AC-1, AC-2, D2/D3).**
  - [x] 2.1 Widened `enemyCountFor` to `enemyCountFor(float eastness, int act, float ny)`: extracted the old step to `baseEnemyCountFor`, then `if (act > 1 && base > 0 && !inCordon(eastness, ny)) base += (act - 1);`. Pure static, no rng.
  - [x] 2.2 Added pure `inCordon(float eastness, float ny)` → `eastness < 0.2f && ny > 0.8f` (WorldSpine BORDER_X/BORDER_Y corner), with a comment that it is explicit-but-redundant with the west safe tier (documents the AC-2 channel split at the site). Extracted `SAFE_TIER`/`CORDON_NY` constants.
- [x] **Task 3 — Wire the ramp into placement (AC-1).**
  - [x] 3.1 `placeFloorActors` reads `int act = flagStore.getAct();` once before the loop; per region computes `float ny = cy / (spine.getHeight() - 1f);` and calls `enemyCountFor(spine.eastness(cx), act, ny)`. The per-enemy position-draw block is untouched (AD-5 call structure unchanged). Added a greppable `// Epic 5 seam:` comment where the act is read.
- [x] **Task 4 — Tests + verification (all ACs).**
  - [x] 4.1 AC-3: `FlagStoreTest` — `getAct()` returns 1 when unset (raw flag 0), 2/3 after `setAct`, clamps ≤0 to 1, floors a stray negative. Act-1 parity: `OccupationEscalationTest.actOneIsBitIdenticalToTheBaseBands` pins `enemyCountFor(e, 1, ny)` == the pre-4.3 bands across 8 eastness samples (no regression).
  - [x] 4.2 AC-1: `laterActsAddOneEnemyPerActToTheInterior` — base 2 → 3 (Act 2) / 4 (Act 3), base 1 → 2/3, base 3 → 5 (additive, monotonic).
  - [x] 4.3 AC-2: `theWestSafeTierStaysEmptyAtEveryAct` (eastness ≤ 0.2 → 0 at acts 1/2/3), `theNwBorderCordonIsNeverThickened` (0.05, ny 0.9 → 0 at all acts), `inCordonNamesTheFarWestFarNorthCorner` (the box's four boundaries).
  - [x] 4.4 AC-1 end-to-end: `aHigherActRegeneratesADenserInterior` — regen both sides from the same seed (identical base map), Act 3 aggregate strictly > Act 1 across 10 seeds. AD-5: `sameSeedAndActRegeneratesAnIdenticalLayout` — same seed+act reproduces the exact enemy positions.
  - [x] 4.5 Full suite green via `docs/BUILD.md` (`mvn -o clean install`): **438 tests, 0 failures** (+11 over the 427 baseline), both modules installed. BUILD SUCCESS.

### Review Findings

Code review 2026-08-13 (Blind Hunter + Edge-Case Hunter + Acceptance Auditor, focused diff `fba5b59`..`54ed29c`). Acceptance Auditor: **all three ACs SATISFIED, D1–D4 met, D4 inert scope honestly reflected.** No high/medium correctness defects. 5 low-severity patches applied, 5 findings dismissed (unreachable or by-design).

- [x] [Review][Patch] Cordon comment overstated derivation — `CORDON_NY=0.8f` is presented as "WorldSpine BORDER_Y=0.9f" but isn't equal to it. **FIXED: reworded to state 0.8 is a deliberate generous over-approximation that fully *contains* the true BORDER_Y=0.9 cordon (0.8 < 0.9 → the box extends further south than the landmark), and referenced BORDER_X/BORDER_Y explicitly.** Guard kept (create-story "explicit box" decision). [RunState.java inCordon]
- [x] [Review][Patch] Duplicate safe-tier constant — added `SAFE_TIER=0.2f` when `SAFE_TIER_EASTNESS=0.2f` already exists (RunState.java:48). **FIXED: reuse `SAFE_TIER_EASTNESS` in `baseEnemyCountFor`/`inCordon`; deleted the duplicate.** [RunState.java:293]
- [x] [Review][Patch] AD-5 "no existing seed's layout changes" claim overscoped — true only at Act 1; at act>1 the extra enemy draws precede the supply/structure scatter on the shared seeded stream, so loot layout shifts too. **FIXED: scoped the claim to Act 1 in the code comment + completion notes, flagged the act>1 loot-shift for Epic 5 (still deterministic per (seed,act)).** [RunState.java placeFloorActors]
- [x] [Review][Patch] E2E ramp test asserted only an aggregate inequality — could pass vacuously (0 < n) if the sampled seeds placed no interior enemies. **FIXED: added a non-vacuity guard (`act1Total > 0`) so a broken baseline can't hide, kept the strict aggregate increase. Did NOT add per-seed `act3 ≥ act1`: post-walkability placement is not strictly monotonic per seed (act 3 draws more positions, so a seed can reject a few act 1 kept) — that would introduce flakiness. The per-region monotonicity is already pinned deterministically by the unit tests.** [OccupationEscalationTest.aHigherActRegeneratesADenserInterior]
- [x] [Review][Patch] `baseEnemyCountFor` lacked Javadoc while its siblings had it. **FIXED: added a one-line Javadoc.** [RunState.java baseEnemyCountFor]

**Dismissed (5):** height==1 → NaN `ny` (unreachable — `MAP_H=48` is a fixed constant; `ny` is only computed in `placeFloorActors`); act int-overflow via `base += (act-1)` (unreachable — acts are 1–3, no path approaches `MAX_VALUE`); `ny` out-of-range / negative act from non-getAct callers (room centers are always in-bounds, and the `act > 1` guard already no-ops act ≤ 1 including negatives); "feature ships dead / no production writer of the act flag" (that IS D4 — the documented Epic 5 seam, not a defect); `getAct` double-clamp "masks corruption" (clamping a difficulty flag to the Act-1 baseline is a safe AD-6 default, not a bug).

**Review patches applied 2026-08-13:** all 5 above; full suite green (438 tests, unchanged count — patches are comments + a constant dedup + a strengthened assertion in an existing test).

## Dev Notes

### Current state (what exists, to preserve)

- **`enemyCountFor` determinism is load-bearing (AD-5).** The count must stay a pure function with no rng — only the per-enemy *position* draws (`rng.nextInt(3) - 1`) touch the seeded stream. Adding an `act`/`ny` argument keeps it pure (act is a flag read once; eastness/ny are geometry). Do not read `rng` inside the count.
- **Act-1 parity is the regression guard (AC-3).** At `act == 1` the widened function must return exactly the old bands, so every existing seed's layout and every `RunState` round-trip / determinism test stays green. Write the Act-1 parity test first.
- **FlagStore is the single narrative-state authority (AD-7).** The act lives here like any flag; do not add a parallel act field on `RunState`/`RoguePlayer`. The 0→Act-1 mapping in `getAct()` is the AD-6 deterministic default — a field-absent save must not read Act 0.
- **Two channels must not merge (AD-11).** Channel (a) here thickens the interior; channel (b) — the NW cordon that *thins* as acts advance, feeding AD-12's survivable-by-Act-3 win gate — is Epic 5 (5.7). The `inCordon` exclusion is the explicit firewall so a uniform multiplier can never harden the win gate (the "wrong" behaviour AD-11 calls out by name).

### The inert-until-Epic-5 seam (D4 — read this)

- The map generates **once** (`generateFloor` at `RunState.java:146` ctor, `:364` death-restart); there is **no reinforcement/respawn path**. At generation time the act is always 1, so in shipped play the ramp does not yet change anything — it is a *tested mechanism awaiting a live trigger*, exactly like 4.1's AG turn-order sort (inert because every spawned enemy is AG=3) and 4.2's parley (mechanism shipped, faction content deferred).
- **This is intended and must be stated in the dev completion notes**, so code review's Acceptance Auditor scores the ACs against "the mechanism is correct and pinned," not "it's observable in a live run." The live trigger (regenerate-unexplored on act-flip, or a seeded reinforcement spawner) is Epic 5 work and is called out in the Out-of-Scope seam. Add a one-line `// Epic 5 seam: live act-flip trigger (regen/reinforce) not wired here` comment where the act is read, so the seam is greppable.

### AD / architecture references

- FR-12 (occupation grows more punishing over a run) — `[Source: epics.md:540-554]`
- AD-11 (act-gating quests flip narrative flags; **the escalation trigger is story-flags, not a timer/counter**; the ramp has TWO channels that must not be merged — (a) off-border thickens, (b) NW cordon thins; a uniform multiplier including the cordon is *wrong*) — `[Source: architecture/.../ARCHITECTURE-SPINE.md:131-136]`
- AD-12 (the win gate is the NW border crossing, consumes channel (b), survivable by Act 3 — channel (a) must never harden it) — `[Source: architecture/.../ARCHITECTURE-SPINE.md:138-143]`
- AD-7 (run-scoped narrative state lives in `FlagStore`) — `[Source: FlagStore.java header]`
- AD-6 (a field-absent save inherits a deterministic default — here 0→Act 1 in `getAct()`; no new persisted field) — `[Source: RunState.java:68-69]`
- AD-5 (one seeded draw sequence; the count decision stays rng-free) — `[Source: RunState.java:181-190]`
- WorldSpine gradient + cordon constants — `[Source: WorldSpine.java:19-72]`

### Previous-story intelligence (Stories 4.1 / 4.2)

- **Inert-mechanism honesty (4.1 AG lesson).** 4.1's AG turn-order sort is inert because shipped enemies are all AG=3; that surfaced in review as a deferred item. 4.3 has the same shape by design — pre-empt it: document D4 in the completion notes and mark the Epic 5 seam in code. Do **not** try to make it live by adding a spawner (that's the out-of-scope line).
- **Mechanism-now / content-later scoping (4.2 parley).** 4.2 shipped the `Deescalate` mechanism + one reachable trigger and deferred faction content to Epic 5. 4.3 is the same contract: the density function + its wiring into placement, with the live act-flip trigger deferred.
- **Pure-function / no-rng-in-decisions discipline (3.x).** The eastness→count and eastness→loot decisions are deliberately rng-free steps; keep the widened count the same. Any test that regenerates must set the act *before* `generateFloor`.
- **AD-6 default, done right (Story 1.1 saveVersion lesson).** Add the default-mapping (`getAct` → 1) AND a test that a field-absent (unset) store reads Act 1 — the stamp and its read-branch together, never a dead default.

### Project Structure Notes

- New/edited files (expected): `FlagStore.java` (act key + `getAct`/optional `setAct`); `RunState.java` (`enemyCountFor` signature + `inCordon` + `placeFloorActors` wiring); optionally `WorldSpine.java` (a pure `northness(int y)` helper, dev's call); new/updated tests under `core/src/test/...` (a `FlagStore` act test + an occupation-escalation test; consider `OccupationEscalationTest`).
- Build/verify: `docs/BUILD.md` recipe — `mvn -o clean install` (CI-truth, both modules + full suite); `mvn -o -pl core test -Dtest=<Class>` for a single class; `exec:java` needs `mvn -o -pl core install` first and a display (headless CI can't run it).

## Dev Agent Record

### Agent Model Used

Claude Opus 4.8 (1M context) — create-story 2026-08-13.

### Debug Log References

- `mvn -o -pl core test -Dtest=FlagStoreTest,OccupationEscalationTest` — 17 tests green (10 + 7) on first run; no red-phase surprises (the pure function + a clean flag accessor compiled and passed once the helpers were package-private).
- `mvn -o clean install` — BUILD SUCCESS, full suite green (438 tests, 0 failures/errors; +11 over the 427 baseline), both modules installed.

### Completion Notes List

- **AC-1 — act drives interior density (channel a).** `RunState.enemyCountFor(eastness, act, ny)` adds `(act-1)` to the base step where the base is already > 0 and the region is not in the cordon — additive, monotonic, rng-free. Wired into `placeFloorActors` via `flagStore.getAct()` read once per generation. Pinned by `laterActsAddOneEnemyPerActToTheInterior` (unit) and `aHigherActRegeneratesADenserInterior` (end-to-end, 10 seeds).
- **AC-2 — the NW cordon is never touched.** `RunState.inCordon(eastness, ny)` = `eastness < 0.2 && ny > 0.8` (WorldSpine BORDER_X/BORDER_Y corner); the ramp skips it. Belt-and-suspenders (the west safe tier is base-0 anyway), but it makes the channel-a/channel-b split a pinned invariant (`theNwBorderCordonIsNeverThickened`, `theWestSafeTierStaysEmptyAtEveryAct`, `inCordonNamesTheFarWestFarNorthCorner`).
- **AC-3 — AD-6-safe, Act-1-default, no regression.** `FlagStore.getAct()` maps the unset/0 sentinel (and any stray negative) to Act 1, so every existing save reads Act 1 with no new persisted field and no `restoreAfterLoad` change. Act-1 output is bit-identical to the pre-4.3 bands (`actOneIsBitIdenticalToTheBaseBands`), so no existing seed's layout moved — RunStatePersistenceTest (22) and HybridMapTest still green.
- **D4 seam is intentional and inert (READ for review).** The map + all enemies generate once per run at Act 1 (`generateFloor` at ctor + death-restart; no respawn path), so in shipped play the ramp changes nothing yet — it is a tested mechanism awaiting a live trigger. The end-to-end test drives that trigger with an explicit `setAct(n); generateFloor()`. The live trigger (regenerate-unexplored / reinforcement spawn on act-flip) is Epic 5 (marked with a greppable `// Epic 5 seam:` comment in `placeFloorActors`). This mirrors 4.1's inert AG-sort and 4.2's mechanism-now/content-later parley — flagged up front so the Acceptance Auditor scores "mechanism correct + pinned," not "observable live."
- **Deviation (test seam):** `enemyCountFor`, `baseEnemyCountFor`, and `inCordon` are **package-private** (not `private`) so `OccupationEscalationTest` can pin the pure function deterministically, rather than relying on flaky seed-aggregate sampling through `getEnemies()`. Minimal, in-package widening; `supplyCountFor` stays private (untouched).
- **AD-5:** the count decision reads only the act flag + geometry — no rng. Same seed + same act reproduces the exact layout (`sameSeedAndActRegeneratesAnIdenticalLayout`). **Review note for Epic 5:** the "layout unchanged" guarantee holds only at Act 1. At act > 1 the interior places more enemies, and those extra position draws precede the supply/structure scatter on the *same* seeded stream — so a given seed's LOOT layout also shifts once the act advances (still deterministic per `(seed, act)`). Epic 5's live act-flip trigger must expect loot to move with density, not just enemy count.

### File List

- `core/src/main/java/com/margins/rogue/state/FlagStore.java`
- `core/src/main/java/com/margins/rogue/state/RunState.java`
- `core/src/test/java/com/margins/rogue/state/FlagStoreTest.java`
- `core/src/test/java/com/margins/rogue/state/OccupationEscalationTest.java` (new)

## Change Log

- 2026-08-13 — created by create-story. Scope decisions confirmed with Justine: D1 (act from a `FlagStore` key `act.current`, default Act 1 — AD-11 story-flag trigger), D2 (additive `(act−1)` bump east of the safe tier, not multiplicative), D3 (explicit far-west/far-north `inCordon` exclusion so channel (a) never touches the win-gate cordon), D4 (**mechanism only** — the map/enemies generate once at Act 1 with no respawn path, so the ramp is a tested-but-inert function until Epic 5 wires a live act-flip trigger; matches 4.1 AG / 4.2 parley scoping). Substrate audit: `enemyCountFor` is already a pure rng-free step; `FlagStore` already serializes AD-6-safe; the work is the act accessor + widening the count + wiring placement, all determinism-preserving. Status → ready-for-dev.
- 2026-08-13 — dev-story: implemented the `FlagStore` act flag (`KEY_ACT`/`getAct`/`setAct`, AD-6 default Act 1), the per-act cordon-aware density function (`enemyCountFor(eastness, act, ny)` + extracted `baseEnemyCountFor` + `inCordon`), and the `placeFloorActors` wiring (act read once, `// Epic 5 seam` marked; per-enemy draws untouched). +11 tests (`FlagStoreTest` ×4 act tests; new `OccupationEscalationTest` ×7 covering Act-1 parity, additive ramp, west/cordon exclusion, regen end-to-end, AD-5 determinism); full suite green (438, +11). One deviation: the three count helpers are package-private (deliberate test seam) for deterministic pinning. Status → review.
- 2026-08-13 — code review (Blind Hunter + Edge-Case Hunter + Acceptance Auditor, focused diff `fba5b59`..`54ed29c`): all three ACs SATISFIED, D1–D4 met, D4 inert scope honestly reflected; no high/medium correctness defects. 5 low-severity patches applied (cordon-comment honesty + WorldSpine reference; deduped the `SAFE_TIER` constant to reuse `SAFE_TIER_EASTNESS`; scoped the AD-5 "layout unchanged" claim to Act 1 + flagged the act>1 loot-shift for Epic 5; non-vacuity guard on the E2E ramp test; `baseEnemyCountFor` Javadoc). 5 findings dismissed (unreachable NaN/overflow, already-guarded act≤1, by-design D4 dead-feature, acceptable AD-6 clamp floor). Full suite green (438). Status → done.

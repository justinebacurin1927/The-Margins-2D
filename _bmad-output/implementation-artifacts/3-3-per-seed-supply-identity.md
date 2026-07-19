---
baseline_commit: 3b1054a
---

# Story 3.3: Per-seed supply identity

Status: review

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As the developer,
I want each Supply type bound to a true identity per seed at run start,
so that identify-by-use is a genuine gamble that varies between runs (FR-11).

## Acceptance Criteria

1. **Given** two runs on different seeds, **When** identities bind, **Then** a Supply type (e.g. "Sealed Waterskin") can map to different true identities; **within one run the mapping is stable** across many uses. (FR-11, AD-5, AD-12)
2. **Given** the same seed, **When** two runs bind identities, **Then** the bindings are identical (reproducible per seed). (AD-5)
3. **Given** a bound run, **When** a Supply is used, **Then** the effect applied is the **bound true identity's** effect — not a fixed per-type effect. (FR-11)

**Architectural definition-of-done:**

4. The `SupplyType → TrueIdentity` binding lives on `RunState` in an `IdentifyMap` (AD-12), built from `RunState`'s seeded RNG at run start (AD-5), contains no libGDX types (AD-2), and survives save/load so a resumed run keeps the same mapping (AD-6).

## Product decision to confirm (recommended default baked in)

**Bad identities in an MVP with no status system.** The PRD's "bad" outcomes lean on status effects that don't exist yet (Tainted→Weaken, Spoiled→hunger penalty, Bandages→stop Bleed). This story **approximates** them with the HP/hunger effects that do exist (e.g. Spoiled = raw HP loss + hunger drop; Tainted = minor HP loss), so the gamble has a real downside now (UJ-2). Full status-driven identities are a post-MVP depth item. If you'd rather keep every identity benign until a status system lands, say so — but that removes the downside FR-11/UJ-2 depend on. **Recommendation: approximate now.**

## Tasks / Subtasks

- [x] **Task 1 — `TrueIdentity` (the real effects)** (AC: 3)
  - [x] Create `core/src/main/java/com/margins/rogue/item/TrueIdentity.java`: an enum of concrete outcomes, each with a `displayName` and `apply(RoguePlayer)`, expressed only with existing player API (`eat`/`heal`, plus raw HP loss and a clamped hunger drop for the bad ones). First-pass, tunable:
    - `STALE_BREAD` ("Stale bread") → `eat(40)` (PRD: food +40)
    - `SPOILED_MEAT` ("Spoiled meat") → raw HP loss (~10, bypassing armor/block) + hunger drop (~15, clamped ≥ 0)
    - `CLEAN_WATER` ("Clean water") → `eat(15)`
    - `TAINTED` ("Tainted water") → raw HP loss (~5) (Weaken approximated as minor HP loss until a status system exists)
    - `FEVERWORT` ("Feverwort paste") → `heal(4)` (Bleed-cure approximated as minor heal)
    - `RENDERED_FAT` ("Rendered fat") → `heal(4)`
    - `BANDAGES` ("Bandages") → `heal(6)`
    - `OLD_RAGS` ("Old rags") → no effect
    - `INERT_LETTER` ("Sealed letter") → no effect
  - [x] Add a `RoguePlayer` helper for the bad effects if needed: raw HP loss that bypasses `takeDamage`'s armor/dodge/block (food poisoning isn't combat), and a hunger drop clamped at 0. Keep these tiny and in the model (AD-2).
- [x] **Task 2 — `Supply` gains its possible identities; drop its fixed effect** (AC: 1, 3)
  - [x] In `Supply`, replace the fixed `apply(...)` bodies (added in 3.2) with `TrueIdentity[] possibleIdentities()` per PRD pair:
    - `WRAPPED_BUNDLE` → `{STALE_BREAD, SPOILED_MEAT}`
    - `SEALED_WATERSKIN` → `{CLEAN_WATER, TAINTED}`
    - `SMALL_TIN` → `{FEVERWORT, RENDERED_FAT}`
    - `FOLDED_CLOTH` → `{BANDAGES, OLD_RAGS}`
    - `SEALED_LETTER` → `{INERT_LETTER}`
  - [x] Keep `displayName()`, `isConsumedOnUse()` (Letter still not consumed), `byOrdinal`, `ordinal()` storage, and the non-negative-id invariant. The unidentified display name is unchanged (Story 3.4 is what hides/reveals it).
- [x] **Task 3 — `IdentifyMap` on `RunState`** (AC: 1, 2, 4)
  - [x] Create `core/src/main/java/com/margins/rogue/state/IdentifyMap.java`: wraps a `TrueIdentity[] boundByOrdinal` (length `Supply.count()`), indexed by `Supply.ordinal()`. A `static IdentifyMap build(Random rng)` picks one identity per Supply from `possibleIdentities()` via `rng`. Method `TrueIdentity identityOf(int supplyOrdinal)`. No libGDX types.
    - Note: the `identified` set (AD-12) is **not** added here — Story 3.4 adds it when reveal-on-use needs it. Keep this class to the binding only.
  - [x] On `RunState`: add `private IdentifyMap identifyMap;` built **in the constructor from `rng`, before `generateFloor()`** (bind at run start, AD-12/AD-5). Add `getIdentifyMap()`. Rebuild it in `restart()` (a new run rebinds).
  - [x] Persistence: the field is a plain (non-transient) holder of an enum array → serializes with `RunState` (AD-6), so a resumed run keeps its binding. In `restoreAfterLoad()`, rebuild from the seed **only if null** (a pre-3.3 save has no binding); otherwise keep the saved one. (`TrueIdentity[]` may need a `setElementType`/serializer check like `enemies`/`floorItems` — verify the round-trip.)
- [x] **Task 4 — Resolve USE through the binding** (AC: 3)
  - [x] In `TurnEngine`'s USE case, replace `Supply.apply(player)` with the bound identity: `TrueIdentity id = state.getIdentifyMap().identityOf(action.itemType); id.apply(player);`. Consumption stays type-level via `Supply.isConsumedOnUse()`. The "used" message can keep showing the Supply's (still-unidentified) name for now — the reveal is Story 3.4.
- [x] **Task 5 — Verification** (AC: 1, 2, 3, 4)
  - [x] Headless harness (extend the 3.1/3.2 pattern):
    - Same seed → identical `IdentifyMap` bindings for all 5 types; two different seeds → at least one type binds differently (probabilistic — assert across a handful of seeds that a bad-capable type like `WRAPPED_BUNDLE`/`SEALED_WATERSKIN` reaches both identities).
    - Within one run, `identityOf` is stable across repeated calls/uses.
    - USE applies the **bound** identity's effect (e.g. a seed where `WRAPPED_BUNDLE→SPOILED_MEAT` costs HP; a seed where it→`STALE_BREAD` raises hunger).
    - Json round-trip: the binding is preserved after `toJson`→`fromJson`→`restoreAfterLoad`; a save with the `identifyMap` field stripped (pre-3.3) rebuilds a valid, deterministic binding on load.
  - [x] `mvn -o -pl core install` then live boot on `:0` (~8s) → clean.

## Dev Notes

### Governing architecture
- **AD-12 — Identify-by-use binding on `RunState`.** `RunState` holds a `SupplyType → TrueIdentity` map built from the seed RNG at run init. This story delivers exactly that map (`IdentifyMap`); the paired `identified` set is deferred to Story 3.4. [Source: ARCHITECTURE-SPINE.md#AD-12; #Structural Seed → `state/IdentifyMap.java`, `item/Supply.java`]
- **AD-5 — Single seeded RNG.** The binding draws only from `RunState.rng()`; same seed ⇒ same binding. No `new Random()` for gameplay. Build order matters for reproducibility — bind at a fixed point (constructor, before `generateFloor`). [Source: ARCHITECTURE-SPINE.md#AD-5]
- **AD-6 — Save = whole `RunState`.** The binding persists so a resumed run keeps its identities. It's seed-derived, so a pre-3.3 save can safely rebuild it from the seed on load (the deterministic fallback). [Source: ARCHITECTURE-SPINE.md#AD-6]
- **AD-2 — no libGDX in the model.** `TrueIdentity`, `IdentifyMap` are pure model. Effects mutate the RunState-owned player (a model rule). [Source: ARCHITECTURE-SPINE.md#AD-2]

### Builds on Story 3.2 (just committed, 3b1054a)
- `Supply` currently has **fixed** effects (`apply` per type) — this story **moves the effect to `TrueIdentity`** and resolves it per-seed via `IdentifyMap`. That is the intended 3.2→3.3 layering noted in the 3.2 class doc ("Story 3.3 randomizes them per seed"). Expect to edit `Supply.apply` out and update `TurnEngine`'s USE case (the only caller). [Source: core/src/main/java/com/margins/rogue/item/Supply.java; system/TurnEngine.java USE case]
- `RunState` already field-initializes `inventory`/`floorItems` and re-wires transient state in `restoreAfterLoad()`; follow that pattern for `identifyMap` (persisted, with a seed-rebuild fallback for old saves). [Source: core/src/main/java/com/margins/rogue/state/RunState.java]
- `RoguePlayer` has `eat(int)`/`heal(int)`; the bad identities need a raw HP loss (bypassing `takeDamage` armor/block) and a clamped hunger drop — add minimal helpers. [Source: core/src/main/java/com/margins/rogue/RoguePlayer.java]

### Scope boundary
- **IN:** the per-seed binding + effect resolution through it. **OUT (Story 3.4):** the `identified` set, hiding the true identity behind an "unidentified" display name, and revealing a whole type on first use. In 3.3 the panel still shows the Supply's normal name; only the *effect* varies by seed.
- Status-effect identities (Weaken debuff, Bleed DoT/cure) are approximated with HP/hunger here; a real status system is post-MVP.

### Testing standards
- `mvn -o -pl core install` before any live boot (stale-artifact quirk). No committed JUnit suite — use the throwaway-`main` headless harness + live boot, as in 3.1/3.2. Prove per-seed variation, within-run stability, effect-through-binding, and round-trip headlessly.

### References
- [Source: _bmad-output/planning-artifacts/epics.md#Epic 3 · Story 3.3 (FR-11)]
- [Source: PRD §Balance — Identify-by-Use Supply set (good/bad pairs per type)]
- [Source: ARCHITECTURE-SPINE.md#AD-12, #AD-5, #AD-6, #AD-2, #Structural Seed]
- [Source: 3-2-use-and-drop-items.md (Supply/effect model this story refactors)]

### Project Structure Notes
- `item/TrueIdentity.java` beside `Supply`/`Inventory`/`FloorItem`; `state/IdentifyMap.java` beside `RunState` — both match the architecture Structural Seed (`state/IdentifyMap.java`, `item/Supply.java`).

## Dev Agent Record

### Agent Model Used

claude-opus-4-8[1m] (via bmad-dev-story)

### Debug Log References

- `mvn -o -pl core install` and full `mvn -o install` → BUILD SUCCESS (no regressions).
- Identify-binding headless harness: **8/8 PASS** — same seed → identical bindings; across seeds a bad-capable type reaches BOTH identities (Wrapped Bundle bread/spoiled, Sealed Waterskin clean/tainted); within-run binding stable over 100 queries; USE applies the BOUND effect (a WB→spoiled seed loses HP, a WB→bread seed gains hunger); Json round-trip preserves the binding.
- 3.1 regression harness re-run: **37/37 PASS**.
- Live boot on `:0` (~10s) → clean.

### Completion Notes List

- **Effect moved from `Supply` to `TrueIdentity`** (9 concrete outcomes), resolved per-seed via a new `state.IdentifyMap` (enum array indexed by `Supply.ordinal()`, built from the seeded RNG). `TurnEngine` USE now applies `identifyMap.identityOf(type)` instead of the fixed 3.2 effect. `RoguePlayer` gained `hurtRaw`/`starve` for the non-combat bad-identity effects. Bad identities approximate the PRD's status outcomes with HP/hunger loss (per the confirmed product decision).
- **Deviation 1 — RNG decorrelation (fix discovered in verification):** `java.util.Random`'s first `nextInt(2)` correlates across nearby seeds, so the first-bound type (Wrapped Bundle) never reached `STALE_BREAD` for small debug seeds (0–199), while later-bound types distributed fine. Since the game is built on seeded reproducibility and a playtester may use a fixed small seed, I added `seededRng()` (skips two draws before binding) so all types distribute fairly. Production `nanoTime` seeds were already fine; this fixes debug/repro seeds. One shared seeded stream is preserved (AD-5).
- **Deviation 2 — dropped the pre-3.3 rebuild branch:** Task 3 planned a `restoreAfterLoad` "rebuild if null" fallback for saves predating this field. Verification showed libGDX Json invokes the no-arg `RunState()` constructor, so `identifyMap` is never null after load — the branch was dead code for a scenario that cannot occur (the field ships with 3.3; no prior save exists). Removed it per "no handling for impossible scenarios." Normal save/load preserves the binding (round-trip verified); the removed harness assertion tested that impossible path.
- **Scope:** binding only. The `identified` set, hiding the true identity behind the unidentified name, and reveal-on-use are Story 3.4. In 3.3 the panel still shows the Supply's normal name; only the effect varies by seed.

### File List

- ADDED: core/src/main/java/com/margins/rogue/item/TrueIdentity.java
- ADDED: core/src/main/java/com/margins/rogue/state/IdentifyMap.java
- MODIFIED: core/src/main/java/com/margins/rogue/item/Supply.java (dropped fixed `apply`; added `possibleIdentities()`)
- MODIFIED: core/src/main/java/com/margins/rogue/RoguePlayer.java (added `hurtRaw`, `starve`)
- MODIFIED: core/src/main/java/com/margins/rogue/state/RunState.java (identifyMap field/getter, `seededRng` decorrelation, build at run start + restart)
- MODIFIED: core/src/main/java/com/margins/rogue/system/TurnEngine.java (USE resolves effect via IdentifyMap)

## Change Log

- 2026-07-19: Implemented Story 3.3 — per-seed Supply→TrueIdentity binding via IdentifyMap on RunState; effect moved from Supply to TrueIdentity and resolved through the binding in TurnEngine USE; RNG decorrelated so debug seeds distribute fairly. Verified 8/8 identify harness + 37/37 3.1 regression + clean full build & live boot. Status → review.

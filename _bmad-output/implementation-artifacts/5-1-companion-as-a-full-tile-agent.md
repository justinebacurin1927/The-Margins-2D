---
baseline_commit: 42e9849
---

# Story 5.1: Companion as a full tile-agent

Status: done

## Story

As Klein,
I want a companion to be a real body with its own state,
so that it is a person to protect, not a shadow (FR-15, AD-10).

## Acceptance Criteria

**AC-1 — The active companion carries its own full Status.**
**Given** an active companion **When** it exists on the map **Then** it carries its own Status block, its own HP pool (woundable / healable / incapacitable), and its own condition/debuff and survival state — authoritative on the companion, never shadowing the player's.

**AC-2 — Roster of four; only the active one is a positioned tile-agent.**
**Given** the roster of four (Aldric combat; Mara, Old Fen, Yenna non-combat) **When** only one is active **Then** the active companion is a positioned tile-agent (tile, HP, survival tick, noise) and the other three are abstract `FlagStore`/Bond entries — no tile, no noise, no survival tick until activated (AD-10, AD-7 per-companion Bond keying).

## Scope decisions (author, 2026-08-13 — running the loop autonomously per Justine)

- **D1 — The roster is a 4-entry enum of identities, not four live agents.** New `CompanionId { ALDRIC, MARA, OLD_FEN, YENNA }` with a `combatant` flag (ALDRIC only) and a `bindId` string that keys Bond. The *active* companion is the existing positioned `Companion` (one party slot, AD-10); the other three exist **only** as `FlagStore` Bond entries (per-companion keys) + a "known/recruited" flag. No positioned body, no HP field, no survival tick is allocated for an inactive member — that is the whole point of AC-2 (cheap abstraction until activated). Activation swaps which id owns the single positioned `Companion`.
- **D2 — Per-companion Bond keying (AD-7).** Replace the single `FlagStore.KEY_BOND = "bond.galleon"` with `bondKey(CompanionId)` → `"bond." + id.bindId` (e.g. `bond.aldric`), and re-point `getBond/adjustBond/getBondTier/applyBondTag` at a *current-companion* (the active id, defaulting to ALDRIC so all existing Bond tests keep their single-companion semantics). **Migration honesty:** the old `bond.galleon` key is retired to `bond.aldric` (the remake's canon combat companion is Aldric, not Galleon); a pre-5.1 save with only `bond.galleon` inherits Bond 0 (AD-6 field-absent default) — documented, no silent data loss beyond the neutral baseline. Keep a `KEY_BOND` alias constant = `bondKey(ALDRIC)` so no call site breaks.
- **D3 — The companion Status block reuses the player's debuff vocabulary, on its own state.** Add a small, companion-owned condition/debuff + survival container so the companion's Status is *authoritative and separate* (AD-3: no shadow of the player). MVP survival state = a single companion-owned track sufficient to prove "own survival state" (a `condition`/debuff set + a heal path), NOT a full re-port of all four player tracks (Hunger/Thirst/Temperature/Debuffs) — that breadth is 5.4/later content. This story delivers: HP woundable (exists) + **healable** (`heal(int)`, clamped to maxHp) + **incapacitable** (a downed state at 0 HP distinct from permadeath — companion *death* shape is FR-17/an open scope decision, deferred) + an own-debuff container the companion carries independently.
- **D4 — Only the active member ticks / makes noise (AC-2).** The inactive three get no `CompanionSystem.follow`, no survival tick, no `NoiseEvent`. This is already true structurally (only positioned `Companion`s are in `getCompanions()`); 5.1 makes it *explicit and tested* — activating a roster id materializes the positioned body; deactivating returns it to an abstract Bond entry.
- **Deferred (→ 5.2+):** the 7-state behavior machine (5.2), simple orders (5.3), companion combat + liability cost + death shapes (5.4), non-combatant take-cover/flee AI, full companion survival tracks (Hunger/Thirst/Temperature parity), recruit/dialogue flows. 5.1 ships the *body + roster model*, not the AI.

## Baseline (verify before adding)

- **`Companion` (com.margins.rogue.Companion)** already is a positioned tile-agent: `hp/maxHp/damage` (14/14/3), `tileX/tileY`, `bindId`, `takeDamage`/`isAlive`/`stepTo`/`isAdjacentTo`, distraction uses, `setMap` (transient map, AD-6). Field-initialized for save-safety. **Missing for AC-1:** `heal`, an incapacitated/downed state, and an own condition/debuff + survival container.
- **`RunState`**: `companions` (single-slot list), `getActiveCompanion()`, `removeActiveCompanion()`, `spawnStartingCompanion()` (spawns `bindId="galleon"` beside the player — reconcile to the roster's ALDRIC per D2). `restart()` re-spawns.
- **`FlagStore`**: `KEY_BOND = "bond.galleon"`, `getBond/adjustBond/getBondTier/applyBondTag`, `BOND_TAG_HONEST/DISMISSIVE`. Single-companion today (D2 makes it per-companion, ALDRIC-default).
- **`CompanionSystem.follow`** runs in the AD-4 pipeline's Companion step (TurnEngine:326), only on player-acted turns (AD-5). Inactive roster members must never enter this path (D4).
- **Existing Bond/companion tests** (`DialogueEffectTest`, `ParleyDeescalationTest`, persistence) assume single-companion Bond — the ALDRIC-default keeps them green; verify.

## Tasks / Subtasks

- [x] **Task 1 — `CompanionId` roster enum (AC-2, D1).**
  - [x] 1.1 New `com.margins.rogue.CompanionId { ALDRIC(true,"aldric"), MARA(false,"mara"), OLD_FEN(false,"old_fen"), YENNA(false,"yenna") }` with `boolean isCombatant()` and `String bindId()`.
- [x] **Task 2 — Per-companion Bond keying (AC-2, D2, AD-7).**
  - [x] 2.1 `FlagStore.bondKey(CompanionId)`; the no-arg `getBond/adjustBond/getBondTier` delegate to `ALDRIC`; `KEY_BOND = bondKey(ALDRIC)` (derived, no drift) so every call site/test stays correct. (A *settable* current-id was dropped as YAGNI — callers wanting the active member use `getBond(state.getActiveCompanionId())`; keeps FlagStore stateless/serialization-simple.)
  - [x] 2.2 `getBond(CompanionId)` / `adjustBond(CompanionId, delta)` / `getBondTier(CompanionId)` overloads for the three abstract members (read/write their Bond without a body).
- [x] **Task 3 — Companion Status: healable + incapacitable + own debuff state (AC-1, D3).**
  - [x] 3.1 `Companion.heal(int)` (clamped to maxHp); `isIncapacitated()` (downed at 0 HP, distinct from the player's permadeath, heal-recoverable); an own `Condition { WOUNDED, PANICKED }` container (companion-authoritative, AD-3; serialization-safe booleans) with add/has/remove/clear.
  - [x] 3.2 The active companion carries a `CompanionId id` (field-init ALDRIC, AD-6) so its Status/Bond are addressable; `spawnStartingCompanion` uses `ALDRIC` via `activateCompanion`.
- [x] **Task 4 — Activation model (AC-2, D4).**
  - [x] 4.1 `RunState.activateCompanion(CompanionId)` materializes the single positioned body beside the player; the previously active id reverts to an abstract Bond entry (its Bond persists in FlagStore); the inactive three never tick/noise (structural — they are not in `getCompanions()`). `getActiveCompanionId()`.
- [x] **Task 5 — Tests + verification (all ACs).**
  - [x] 5.1 AC-1: the active companion's HP is woundable (`takeDamage`), healable (`heal` clamps at maxHp), incapacitable (0 HP → downed, heal-recoverable, not the player's death), and carries its own condition independent of the player (AD-3).
  - [x] 5.2 AC-2: exactly one positioned companion (Aldric); the other three roster ids have Bond entries but no body/tick/noise; activating a roster id swaps the single body and reverts the prior one; per-companion Bond is independent.
  - [x] 5.3 Regression: existing single-companion Bond tests stay green under the ALDRIC-default; full suite green (472 → 478, +6). **Verified:** all green.

## Dev Notes

- **AD-3 (single authority).** The companion's HP/debuff/survival Status is authoritative *on the companion* — never a duplicate or shadow of the player's. Tests assert the two are independent.
- **AD-7 (per-companion Bond).** Bond becomes keyed per roster id; the ALDRIC-default preserves every existing single-companion call site and test.
- **AD-10 (one positioned agent).** Exactly one companion is a tile-agent; the other three are abstract FlagStore/Bond entries until activated — no tile, no noise, no survival tick.
- **AD-4/AD-5.** No new pipeline step; inactive members never enter `CompanionSystem.follow`. No survival tick fires for an abstract member.
- **AD-6.** New companion fields are field-initialized so a pre-5.1 save loads a valid Status; the retired `bond.galleon` key resolves to Bond 0 for the ALDRIC-default (documented, neutral-baseline only).
- **Simplicity (CLAUDE.md §2).** 5.1 ships the *body + roster model*, not the AI (5.2) or combat/liability (5.4). Survival state is the minimal own-track proving AC-1, not a full four-track re-port.
- Build/verify: `docs/BUILD.md` — `mvn -o clean install`.

## Dev Agent Record

### Agent Model Used
Claude Opus 4.8 (1M context) — create-story 2026-08-13 (autonomous loop).

### Debug Log References
- `mvn -o test` — BUILD SUCCESS, full suite green (478 tests, +6 over the 472 baseline).

### Completion Notes List
- **AC-1 own Status:** `Companion` now carries its own HP pool that is woundable (`takeDamage`), **healable** (`heal(int)`, clamped to its own max), and **incapacitable** (`isIncapacitated()` = 0 HP — a downed, heal-recoverable state distinct from the player's permadeath; the companion *death shape* is FR-17/deferred), plus its own `Condition { WOUNDED, PANICKED }` container (add/has/remove/clear) that is authoritative on the companion and independent of the player (AD-3). Pinned by `CompanionRosterTest.theActiveCompanionHpIsWoundableHealableAndIncapacitable` and `theCompanionsConditionStateIsItsOwnNotThePlayers`.
- **AC-2 roster of four:** new `CompanionId { ALDRIC(combatant), MARA, OLD_FEN, YENNA }`. Exactly one member is a positioned tile-agent (the single `Companion` slot, AD-10) — `RunState.activateCompanion(id)` materializes it beside the player and `getActiveCompanionId()` reports it; the other three exist only as `FlagStore` Bond entries with **no body, no survival tick, no `NoiseEvent`** (structural — they are absent from `getCompanions()`, so `CompanionSystem.follow` and the tick can't reach them). Pinned by `exactlyOneCompanionIsPositionedTheOthersAreAbstract`, `activatingARosterMemberSwapsTheSinglePositionedBody`, `abstractMembersHaveNoBodyAndDoNotTick`.
- **AD-7 per-companion Bond:** `FlagStore.bondKey(CompanionId)` + `getBond/adjustBond/getBondTier(CompanionId)` overloads make each member's Bond an independent key; the no-arg accessors delegate to `ALDRIC` and `KEY_BOND` is derived from `bondKey(ALDRIC)`, so every existing single-companion call site and test stays green (the old `bond.galleon` key is retired to `bond.aldric`; a pre-5.1 save inherits the neutral Bond 0 per AD-6 — documented, neutral-baseline only). Pinned by `perCompanionBondIsIndependent`.
- **Scope (D3):** "survival state" here is the companion's own HP/downed pool + condition container (authoritative, AD-3); full survival-track parity (Hunger/Thirst/Temperature) rides the companion-as-liability work in 5.4 — not re-ported here (CLAUDE.md §2 simplicity).
- **Review:** inline review (Blind/Edge/Acceptance; multi-agent reserved for token budget). No High/Med findings. Low notes: (1) a deactivated companion keeps only its Bond, not its body's HP/conditions — abstract-member-is-Bond-only *is* AC-2's model; per-body persistence across swaps is deferred; (2) the legacy `Companion(x,y,map,String)` ctor leaves `id=ALDRIC` (test-only path; the real spawn uses the `CompanionId` ctor); (3) survival-track parity deferred (D3, above).
- **Serialization (AD-6):** the new `Companion` fields (`id` enum field-init ALDRIC; `wounded`/`panicked` booleans field-init false) round-trip via the full-RunState `RunStatePersistenceTest` (green) — field-absent loads inherit the deterministic defaults; no `SaveService` registration needed (no new collection field).

### File List
- `core/src/main/java/com/margins/rogue/CompanionId.java` (new)
- `core/src/main/java/com/margins/rogue/Companion.java`
- `core/src/main/java/com/margins/rogue/state/FlagStore.java`
- `core/src/main/java/com/margins/rogue/state/RunState.java`
- `core/src/test/java/com/margins/rogue/CompanionRosterTest.java` (new)

## Change Log

- 2026-08-13 — created by create-story (autonomous loop). First story of Epic 5 (the capstone). Decisions: D1 roster as a 4-entry `CompanionId` enum with one positioned active member and three abstract Bond entries; D2 per-companion Bond keying (AD-7) with an ALDRIC-default retiring the single `bond.galleon` key (pre-5.1 saves inherit Bond 0); D3 companion Status = HP woundable/healable/incapacitable + an own debuff container (minimal own survival state, full four-track parity deferred to 5.4+); D4 only the active member ticks/makes noise. Builds on the existing brownfield `Companion` tile-agent. Behavior machine (5.2), orders (5.3), combat/liability (5.4) deferred. Status → ready-for-dev.
- 2026-08-13 — dev-story + inline review (autonomous loop): `CompanionId` roster enum; `Companion` gains `heal`/`isIncapacitated`/own `Condition` set + a `CompanionId` identity; `FlagStore` per-companion Bond (`bondKey` + CompanionId overloads, ALDRIC-default keeps every existing test green); `RunState.activateCompanion`/`getActiveCompanionId` (one positioned body, three abstract Bond entries). +6 tests (`CompanionRosterTest`); full suite green (478). Inline review, no High/Med (three intended Low notes: swap-time body persistence, legacy String ctor id-default, survival-track parity deferred). Status → done.

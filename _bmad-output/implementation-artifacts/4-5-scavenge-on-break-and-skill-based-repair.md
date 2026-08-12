---
baseline_commit: 9b8053b
---

# Story 4.5: Scavenge-on-break and SKILL-based repair

Status: done

## Story

As Klein,
I want broken gear to return materials and repairs to consume the right ones,
so that the gear economy loops through my SKILL and nothing is free (FR-13, AD-13).

## Acceptance Criteria

**AC-1 — Scavenge a broken weapon for materials by tier.**
**Given** a broken weapon **When** Klein scavenges it **Then** it returns partial base materials scaled by tier (T1–T2 → 1–2, T3–T4 → 2–3, T5 → 3–4) and is removed from his weapons. (The "possibly rare / unique" high-tier bonus drops are deferred content; the base-material yield ships.)

**AC-2 — SKILL-based repair consumes weapon-specific materials.**
**Given** a wielded, still-repairable weapon and the right materials **When** Klein repairs it **Then** the weapon-category materials are consumed and `Weapon.repair(skill)` applies the AD-13 SKILL-curve (durability restored, max permanently lowered). Without the materials, or beyond repair (6th), the repair is refused and spends no turn.

**AC-3 — Reachable, economy-honest, no free loop.**
**Given** the repair/scavenge actions **When** they run **Then** both are bound to keys + in the how-to-play legend (retro #1); repair *removes* materials from the pack and scavenge *adds* them, so there is no infinite-gear loop (AD-13/AD-17) — a weapon's total fighting life is still capped by the 6-repair ceiling.

## Scope decisions (author, 2026-08-13 — running the loop autonomously per Justine)

- **D1 — Repair materials are per-category, drawn from EXISTING supplies.** Spear/Bow → Wood + Rope; Blade → Small Tools (the "metal scrap" proxy); Axe → Wood + Small Tools; Club → Wood. The full weapon-specific taxonomy (String/Sinew, dedicated Metal Scrap, rare mats) is deferred content — 4.5 ships the *mechanism* on the supplies that already exist, so no new `Supply` entries / identify-map / scatter-table churn.
- **D2 — Scavenge yield is deterministic by tier (rng-free, AD-5).** Base count = {T1:1, T2:2, T3:2, T4:3, T5:4} of the category's primary material (Spear/Bow/Club → Wood; Blade/Axe → Small Tools). The rare/unique high-tier bonus is deferred. Deterministic so it is testable and never perturbs the seeded stream.
- **D3 — Repair targets the WIELDED weapon; scavenge targets the first BROKEN weapon.** This is the minimal reachable loop without a weapon-selection UI: mend the weapon in hand *before* it breaks, then strip it for parts *after* it breaks. Repairing an already-broken weapon (it un-wields on break) needs selection UI → deferred. Documented.
- **D4 — No separate repair dice.** "SKILL-based" is already satisfied by the AD-13 curve (`Weapon.repair` bands off SKILL, Story 4.4). 4.5 only gates repair on materials + repairability; it does not add a success/fail roll (matches FR-19's no-dice philosophy).
- **Deferred (→ later / Epic 6):** full material taxonomy + rare/unique drops; a weapon-selection UI to repair/scavenge an arbitrary weapon; the O7/O8 `Inventory.equip` fixes; trader repair/economy.

## Baseline (verify before adding)

- **`Weapon.repair(int skill)` already ships (4.4)** — restores durability, lowers max on the AD-13 SKILL curve, refuses the 6th (`isRepairable()`), no gameplay entry yet (D4 note in its JavaDoc). 4.5 is its caller.
- **Materials exist in `Supply`:** `WOOD`, `ROPE`, `SMALL_TOOLS` (and `COAL`) — non-scatterable tool/material types. Use `Supply.WOOD.ordinal()` etc. as the inventory type id.
- **`Inventory`**: `tryAdd(type, amount) → AddResult (ADDED/BACKPACK_FULL)`, `remove(type, amount) → boolean`, `count(type) → int`. Weapons are NOT in the Inventory (they're `RunState.weapons`); materials ARE.
- **`RunState`**: `getWeapons()`, `getWieldedIndex()`, `getWieldedWeapon()` (null if unarmed/broken), `getInventory()`, `getPlayer().getSkill()`. A broken weapon has already reverted `wieldedIndex` to −1 (4.4), so it sits in the list unwielded.
- **`PlayerAction` / `TurnEngine`**: add two kinds (REPAIR, SCAVENGE) as ordinary acted branches; refusal spends no turn (inert-USE precedent). `MarginScreen.readAction` binds keys; the EXPLORE/SURVIVE legend lists them.

## Tasks / Subtasks

- [x] **Task 1 — `RepairSystem` (AC-1, AC-2, D1/D2/D4).**
  - [x] 1.1 New `com.margins.rogue.system.RepairSystem` (pure, AD-2). `repairCost(Weapon.Category) → int[][]` of {supplyOrdinal, count} (D1 mapping); `primaryMaterial(Category) → supplyOrdinal`; `scavengeCount(tier) → int` (D2 table).
  - [x] 1.2 `repair(RunState, List<String>) → boolean`: wielded weapon required (else refuse), must be `isRepairable()` (else "beyond repair — scavenge it"), materials must be present (else refuse). On success: `remove` each material, `w.repair(skill)`, one observation line, return true.
  - [x] 1.3 `scavenge(RunState, List<String>) → boolean`: find the first broken weapon (else refuse "nothing broken"); if the pack can't accept the yield, refuse without removing (no material loss); else `tryAdd` the yield, remove the weapon via a `RunState.removeWeapon(idx)` helper that fixes `wieldedIndex`, one observation line, return true.
- [x] **Task 2 — `RunState.removeWeapon(int)` (AC-1).**
  - [x] 2.1 Remove the weapon at index; if `wieldedIndex` was after it, decrement; if it was the removed index, set −1. Keep the "wieldedIndex valid or −1" invariant total (mirrors the 4.4 review normalization).
- [x] **Task 3 — Reachable actions (AC-2, AC-3, retro #1).**
  - [x] 3.1 `PlayerAction.Kind.REPAIR` + `SCAVENGE` + factories; `TurnEngine` cases calling `RepairSystem` (acted on success, no turn on refusal).
  - [x] 3.2 Bind two free keys in `MarginScreen.readAction` (verify no collision) + legend rows.
- [x] **Task 4 — Tests + verification (all ACs).**
  - [x] 4.1 AC-2: repair consumes the category materials, applies the curve (max lowered), refuses without materials / when beyond repair / when unarmed (no consumption, no turn).
  - [x] 4.2 AC-1: scavenge a broken weapon yields the tier count of the primary material, removes it, fixes `wieldedIndex`; refuses when nothing broken or the pack is full (no loss).
  - [x] 4.3 AC-3: `removeWeapon` index/wield bookkeeping; the actions are key-bound + reachable via `TurnEngine`.
  - [x] 4.4 Full suite green via `docs/BUILD.md` (`mvn -o clean install`), no regressions (currently 457). **Verify:** all green.

## Dev Notes

- **Economy honesty (AD-13/AD-17).** Repair strictly removes materials; scavenge strictly adds; the 6-repair ceiling (4.4) caps a weapon's life regardless. No path creates net materials from nothing (scavenge yields ≤ what building/repairing costs over the weapon's life — a lossy loop).
- **Authority/co-location.** Detection/HP/weapon-durability authorities stay put; `RepairSystem` owns only the material↔weapon exchange and calls the existing `Weapon.repair`. No HP or detection mutation.
- **AD-4/AD-5.** Repair/scavenge are ordinary acted branches (no new pipeline step); the scavenge yield is deterministic (no rng).
- **Reachability (retro #1).** Both actions key-bound + legend + audited; the loop (wield → wear → repair → … → break → scavenge) is playable, not test-only.
- Build/verify: `docs/BUILD.md` — `mvn -o clean install`.

## Dev Agent Record

### Agent Model Used
Claude Opus 4.8 (1M context) — create-story 2026-08-13 (autonomous loop).

### Debug Log References
- `mvn -o clean install` — BUILD SUCCESS, full suite green (466 tests, +9 over the 457 baseline).

### Completion Notes List
- **AC-1 scavenge:** `RepairSystem.scavenge` strips the first broken weapon → deterministic tier-scaled base material (T1..T5 = 1/2/2/3/4) of the category's primary material (Blade/Axe → Small Tools, else Wood), added all-or-nothing (refuses on a full pack, no loss), then `RunState.removeWeapon` (keeps `wieldedIndex` valid). Pinned by `scavengeYieldsTierMaterialsAndRemovesTheWeapon`, `scavengeKeepsTheWieldedIndexValid`, `scavengeRefusedWhenNothingBroken`.
- **AC-2 repair:** `RepairSystem.repair` mends the wielded weapon — refuses (no turn, no consumption) when unarmed / not damaged / beyond repair / lacking materials; else consumes the per-category materials and applies `Weapon.repair(skill)` (AD-13 curve, 4.4). Pinned by `repairConsumesMaterialsAndPermanentlyLowersMax` + four refusal tests.
- **AC-3 reachable + lossy economy:** `REPAIR`→**N**, `SCAVENGE`→**U** bound in `MarginScreen` (+ legend rows), routed through `TurnEngine` (acted on success, no turn on refusal); repair removes materials, scavenge adds, the 6-repair ceiling (4.4) caps a weapon's life → no infinite-gear loop. Pinned by `repairAndScavengeAreReachableViaTurnEngine`.
- **Review:** inline review (Blind/Edge/Acceptance perspectives; the multi-agent review was reserved for token budget during the autonomous loop). No High/Med findings. Two intended Low notes: repair targets the wielded weapon only (D3 — repairing an already-broken/unwielded weapon needs selection UI, deferred); the "doesn't need mending" guard refuses a full weapon to avoid wasting the ceiling (beyond strict AC-2, player-friendly).
- **AD-4/AD-5:** repair/scavenge are ordinary acted branches (no new pipeline step); the scavenge yield is deterministic (rng-free). No new persisted field.

### File List
- `core/src/main/java/com/margins/rogue/system/RepairSystem.java` (new)
- `core/src/main/java/com/margins/rogue/state/RunState.java`
- `core/src/main/java/com/margins/rogue/system/PlayerAction.java`
- `core/src/main/java/com/margins/rogue/system/TurnEngine.java`
- `core/src/main/java/com/margins/MarginScreen.java`
- `core/src/test/java/com/margins/rogue/RepairSystemTest.java` (new)

## Change Log

- 2026-08-13 — created by create-story (autonomous loop). Decisions: D1 per-category repair materials from existing supplies (full taxonomy deferred), D2 deterministic tier-scaled scavenge yield (rare/unique deferred, rng-free), D3 repair the wielded weapon / scavenge the first broken one (selection UI deferred), D4 no separate repair dice (SKILL rides the 4.4 curve). Builds on 4.4's `Weapon.repair`. Status → ready-for-dev.
- 2026-08-13 — dev-story + inline review (autonomous loop): `RepairSystem` (per-category repair cost + deterministic tier scavenge yield from existing supplies), `RunState.removeWeapon`, `REPAIR`/`SCAVENGE` actions bound to N/U + legend. +9 tests (`RepairSystemTest`); full suite green (466). Inline review, no High/Med findings. Status → done.

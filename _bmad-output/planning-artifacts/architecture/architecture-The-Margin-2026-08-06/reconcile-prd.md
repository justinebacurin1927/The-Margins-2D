---
title: PRD ↔ Architecture Spine Reconciliation
artifact: architecture-spine (reconcile-prd)
inputs: prd-The-Margin-2026-08-06/prd.md, ARCHITECTURE-SPINE.md
status: draft
created: 2026-08-06
---

# PRD ↔ Architecture Spine Reconciliation

Reconciles the finalized PRD (`prd.md`) against the architecture spine (`ARCHITECTURE-SPINE.md`) to catch anything the spine dropped, weakened, or contradicted.

## 1. FR → Architectural Home Coverage (FR-1..21)

| FR | Spine home | Verdict |
| --- | --- | --- |
| FR-1 Text intro, read or skip | — | **NO HOME.** No AD covers the paged/skippable Act 0 intro; capability map starts at FR-4. The "intro screens do not tick survival tracks" rule (an AD-5-adjacent invariant) is not captured. |
| FR-2 Aldric's diegetic tutorial | — | **NO HOME.** No AD or convention covers in-fiction tutorial/onboarding delivery. |
| FR-3 Aldric's capture | AD-7 (flags), AD-10 (party leave), AD-11 (rescue quest seed) | Partial. Capture → recoverable-parting isn't explicit, but the mechanics (party removal, rescue-gating, discovery flags) have homes. |
| FR-4 Four survival tracks | AD-4, AD-5 (cap. map FR-4..8) | Covered. |
| FR-5 Day/Night + Weather | AD-4, AD-5 | Covered. |
| FR-6 Food/water/purification | AD-4, AD-5 | Covered. |
| FR-7 Temperature forces fire | AD-4, AD-5 | Covered. |
| FR-8 Debuff system | AD-4, AD-5 | Covered. |
| FR-9 Persistent traversable Herois | AD-8 (continuous tiled region) | Covered (explicitly bound). |
| FR-10 Foray loop | AD-8, AD-4 | Covered. |
| FR-11 Horizontal progression | AD-13 (gear-with-memory) | Partial. SKILL/knowledge accumulation and the **What-if-branch extensibility requirement** ("support the Ant/Buried Truth without a rewrite") have no AD or Deferred note (see Gap 5). |
| FR-12 Combat viable but costly | AD-9 (noise→detection), AD-13 (durability), AD-4 (turn order), AD-7/11 (escalation) | Covered. |
| FR-13 Gear-with-memory | AD-13 | Covered (explicitly bound, decay table reproduced). |
| FR-14 Permadeath + Last Stand | AD-3 (RunState owns Last-Stand), AD-4 (pipeline step) | Covered; the failed-roll sub-question is not flagged (see Gap 4). |
| FR-15 Companions full tile-agents | AD-10 | Covered (own Status/HP/debuff state explicitly). Death-scope sub-question dropped (see Gap 4). |
| FR-16 Companion AI autonomous | AD-10 (behavior state machine), AD-9 (detection/noise parity) | Covered. |
| FR-17 Companions help + liability | AD-10, AD-7 (Bond) | Covered; party-size open item not flagged (see Gap 4). |
| FR-18 Act-gating main story | AD-11, AD-12 | Covered (both quest proposals + border-crossing win explicitly bound). |
| FR-19 Dialog and quest delivery | AD-7, AD-11 | Covered. |
| FR-20 Inventory | AD-13; cap. map FR-20..21 → `rogue/item/*` | Covered structurally. |
| FR-21 Currency and trade | cap. map FR-20..21 → `rogue/item/*` | Covered structurally; coin stack/compression sub-question not flagged (see Gap 4). |

**No-home FRs: FR-1, FR-2.** FR-11's extensibility clause and FR-3's capture detail are partial.

## 2. §8 Open Items (Q1..Q7) Coverage

| Open Item | Spine disposition | Verdict |
| --- | --- | --- |
| Q1 Engine/stack | Stack section (Java 17, libGDX 1.12.1, LWJGL3) | Decided (KEEP). Covered. |
| Q2 Map structure (hybrid; continuous vs sub-areas) | AD-8 resolves **continuous**; stitching deferred to world-gen epic | Decided + deferred detail. Covered. |
| Q3 Border-crossing win | AD-12 (final tense run, not boss; Deep Cave Mouth ≠ exit) | Decided. Covered. |
| Q4 Act-gating quests | AD-11 ("Follow the Road", "The Rescue") | Decided. Covered. |
| Q5 Companion roster + full tile-agents | AD-10 (roster + entity model); stat granularity deferred | Decided. Covered, **but** the companion-death scope sub-question is dropped (see Gap 4). |
| Q6 Combat-vs-stealth numbers | Deferred section (per §8/Q6) | Deferred. Covered. |
| Q7 Run length | Deferred section (per §8/Q7) | Deferred. Covered. |

**Additional §8 surfaced items:** rescue outcome (FR-3/18), escalation-trigger mechanics (FR-12/18), home-untouched texture (FR-18), Magdalene/parents' fate (FR-18), failed Last-Stand roll (FR-14), companion death (FR-15), coin stack/compression (FR-21), Copper Road towns (FR-21).

- Covered: Copper Road towns → Deferred (world-gen epic).
- Resolved silently: escalation-trigger mechanics → AD-11 decides **story-flags-driven** (ramp reads FlagStore) without flagging the PRD's open three-way choice (see Gap 5, note).
- Dropped (not flagged as deferred): companion death, failed Last-Stand roll, coin stack/compression, rescue outcome, party-size (FR-17), home-untouched texture, Magdalene/parents' fate. The last two are story texture and arguably out of architecture scope, but the mechanical ones should be listed in the spine's Deferred section.

## 3. Full-Tile-Agent Model (FR-15/16/17)

**Fully captured.** AD-10 explicitly binds FR-15/16/17, rejects the legacy `followStep` follower model, and enumerates own Status block, own HP pool, own condition/debuff state, and the behavior state machine (FOLLOW / HOLD / HIDE / DISTRACT / FIGHT / RETREAT / FLEE). AD-9 grants companions parity in the detection/noise rules. The only uncaptured aspect is the PRD's open death-vs-capture-vs-departure scope decision.

## 4. Cross-Cutting NFRs (PRD §4.7)

| NFR | Spine disposition | Verdict |
| --- | --- | --- |
| Rendering (2D tiles, no 3D/camera) | AD-1/AD-2 (layering) | Covered generically. |
| Interaction (turn-based, tile-by-tile) | AD-4, AD-5 | Covered. |
| Text channel (bottom message log primary, minimal HUD) | "Presentation (SPD lock) → MarginScreen → AD-1/AD-2" | **WEAKENED.** AD-1/AD-2 are generic layering rules; no AD ratifies the bottom-log/minimal-HUD/tile-only presentation constraint (see Gap 3). |
| Platform (desktop-first, libGDX/LWJGL3, mobile-friendly HUD) | Stack section | Covered; mobile-port future is unremarked but not required. |
| Performance (render + pathfind within one turn, mid-range desktop) | — | **SILENTLY DROPPED.** PRD §4.7 explicitly tags "[ASSUMPTION — budget set in architecture]" and the spine sets no budget, no performance AD, no test (see Gap 2). |

## 5. Contradictions / Decisions the Spine Makes

No hard contradictions found (AD-8 continuous-map, AD-11 quest locks, AD-12 win, stack choice all match the PRD's recommendations).

One silent decision to surface:
- **Escalation-trigger mechanics** — PRD §8 leaves this a three-way open (story flags / occupation-timer / exploration-driven, links FR-12/FR-18). Spine AD-11 quietly decides **story-flags-driven** ("the occupation-escalation ramp reads those flags"). This is a reasonable resolution consistent with AD-7's FlagStore design, but the spine should flag it as a decision taken, not imply the PRD had already settled it.

## Findings Summary

**Gap 1 — Act 0 onboarding (FR-1, FR-2) has no architectural home.** Capability map begins at FR-4; no AD covers the skippable paged text intro, diegetic tutorial, or the "intro screens do not tick survival tracks" rule. Recommend a small AD (or capability-map row) for Act 0/onboarding, or explicit binding of these FRs to AD-5/AD-7.

**Gap 2 — Performance budget NFR silently dropped.** PRD §4.7 explicitly delegated the budget to the architecture; the spine sets none (no render/pathfind budget, no perf AD, no perf test).

**Gap 3 — SPD presentation lock weakened.** §4.7's bottom-message-log / minimal-HUD / tile-only constraint is reduced to generic AD-1/AD-2 layering. The presentation lock deserves a ratifying AD.

**Gap 4 — Explicitly-open sub-questions silently dropped from the spine.** Companion death (FR-15), failed Last-Stand roll (FR-14), coin stack/compression (FR-21), party-size (FR-17), rescue-outcome (FR-3/18) are all PRD-flagged opens with no Deferred-section entry. At minimum they should be listed as deferred.

**Gap 5 — What-if branch / Region-2 extensibility requirement unaddressed.** FR-11 and §5 Non-Goals require the architecture to support the Ant, the Buried Truth, and Region 2 "without a rewrite"; the spine has no AD or Deferred note covering this (AD-12 only names the Deep Cave Mouth as a threshold). Add an extensibility note or deferred entry.

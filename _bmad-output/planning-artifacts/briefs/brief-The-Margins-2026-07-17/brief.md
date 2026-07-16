---
title: "Product Brief: The Margins"
status: ready
created: 2026-07-17
updated: 2026-07-17
---

# Product Brief: The Margins

## Executive Summary

**The Margins** is a turn-based roguelike dungeon-crawler that reframes the genre's core loop: you aren't descending for treasure, you're searching for *people*. Following Milek of Coneros after the events of the source novel's Ch. 25, every "dungeon" is a real place from the world — a supply route, an auction undercroft, a mountain pass — re-skinned as a procedural floor-set, and every "boss" is the obstacle between Milek and the next person he's trying to reach. It takes Shattered Pixel Dungeon's mechanical bones (scarcity, identify-by-use, permadeath, low-number tactical combat) and gives that loop a reason to matter beyond "number go up."

This is a **solo, you-first passion project** by Justine, set in her own original novel's world. Success is defined as *a finished, playable artifact she is proud of* — not downloads or revenue. It is built in **Java 17 + libGDX**, on top of an already-working SPD-style core (turn engine, BSP floor generation, combat with defense mechanics, hunger, enemy AI, stairs, permadeath).

The **MVP is a vertical slice of Route 1, "The Caravan Road"** — playable end-to-end, proving the entire loop once. The full five-route campaign is the documented north-star Vision, deliberately out of MVP scope.

## The Player's Itch (the problem this scratches)

Roguelikes deliver mechanical tension — real stakes, real scarcity, real loss — but hollow motivation: you descend because descending is the game. Narrative games deliver meaning but rarely the replayable, resource-starved desperation of a roguelike. The Margins fuses them so the scarcity *is* the characterization: Milek survived on stolen rations and untested barrels, so hunger, cramped inventory, and identify-by-use aren't difficulty knobs — they're who he is.

For the creator specifically, the itch is different and just as real: a way to **inhabit and extend her own novel's world interactively**, to make its emotional beats something you *play through* rather than read. The genuine risk to solve for is not market fit — it's the passion-project killer: **a scope so large it never ships.**

## The Solution

An SPD-structured roguelike organized into **Routes** instead of one endless dungeon. Each Route is a location with 3–5 procedural floors plus one authored story floor; Routes unlock by story progress, not raw depth. Core systems, all diegetically justified:

- **Scarcity as character** — hunger, limited slots, and **identify-by-use** as *unlabeled supplies* (Milek can't read Novelborne script yet). A sealed waterskin might be clean or poisoned.
- **No wasted violence** — low numbers, tactics over stats; every fight costs something.
- **Companions as leverage** — they change *how* you solve a floor (Galleon = loud distraction), not just damage output.
- **Reframed permadeath** — a **Last Stand** mechanic precedes true death; "dying" can mean capture or separation, narratively heavy, not a cheap random game-over.

## What Makes This Different

Being honest — this competes with nothing commercially, so the differentiator isn't a moat, it's **authenticity**: a mechanically-grounded adaptation of the creator's own fiction where *every system expresses a character trait* rather than being a genre checkbox. Against a generic SPD reskin, the difference is that the scarcity, the pacifism, and the companion design all trace directly to how Milek actually survives in the books. The "unfair advantage" is simply that no one else can make *this* game — it's built from source material only the creator holds.

## Who This Serves

- **Primary: Justine (the creator).** Success = a finished thing she's proud of, that lives in her world. This is the load-bearing user.
- **Secondary: readers of the novel.** For them the game is an interactive companion piece; authored beats (Galleon's return, the Erik reunion) can assume they carry the weight already.
- **Explicitly not designed to cold-onboard** roguelike strangers with no book knowledge. Legibility to outsiders is a non-goal for the MVP; it does not have to earn a stranger in its first hour.

## Success Criteria (completion-first, passion-project calibrated)

The MVP is a success when **all** of the following are true:

1. **Route 1 is playable start → finish** with no progression-blocking bugs.
2. **Each core loop is demonstrated at least once:** stealth avoidance, hunger pressure, an identify-by-use gamble, and one companion (Galleon) visibly changing how a floor can be solved.
3. **The three authored beats land in-game:** the "Five Nights, Again" opening, at least one INSTINCT-gated dialogue choice, and the Galleon reunion scene.
4. **Last Stand triggers correctly** on otherwise-lethal damage.
5. **A single run can be saved and resumed.**
6. **It ships as a runnable desktop build** (an executable/jar a friend could double-click) — a real artifact that exists.
7. **The "proud bar":** Justine would show it to a friend or the novel's readers without apologizing for it.

**Anti-goals (how we deliberately do NOT measure success):** download counts, sales, store ratings, or feature-completeness against the full Vision.

## Scope

### In (MVP — Route 1: The Caravan Road)

- **Floors:** 3 procedural stealth-leaning floors + 1 authored story floor.
- **Systems to build on the existing core:** field-of-view / fog of war; identify-by-use item system with the Route-1 item set (Wrapped Bundle, Sealed Waterskin, Small Tin, Folded Cloth, Sealed Letter); an inventory (backpack + a couple of equipped slots); the dialog system wired to run the authored scenes and INSTINCT checks; **one** companion (Galleon) with a single "leverage" ability and minimal bond hooks; the Last Stand mechanic; save/continue for one active run.
- **Content:** the "Five Nights, Again" opening quest; the scavenger info-gate NPC; the Galleon reunion story floor.
- **Art:** reuse existing CraftPix packs; add the *minimum* unique sprites needed to read the fiction (Milek, Galleon, one scavenger NPC).

### Out (deferred to Vision — explicitly not MVP)

Routes 2–5; the Trust Meter; the full companion roster (Pietro, Maru, Perfect, Rooster, Charcoal) and bench-passive system; Galleon's Alpha transformation / Life-Thread economy; multiple endings; the literacy skill-tree; alchemy/crafting; shops/merchants; faction systems; sound/music; mobile & HTML5 builds; title-screen polish.

_(Combat number-tuning, the full item-economy curve, and the unique-art production pipeline are known open questions carried in `addendum.md`, not blockers for the brief.)_

## Vision (North Star)

The complete five-route campaign — Caravan Road → Dawnbury Undercity → Blackberry Troupe's Ledger → Mirko Pass Reprise → Return to Coneros — with the win-state **"Return to Coneros with everyone you chose to keep."** Companions with individual mechanics and bond arcs; the hidden Trust Meter shaping Theodore's help; SPD-style **multiple endings determined by Bond + survival + chosen path**, not a binary good/bad. The MVP exists to prove the loop is worth building all of that on top of.

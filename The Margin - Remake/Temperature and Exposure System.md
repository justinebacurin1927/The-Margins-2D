---
tags: [the-margin, game-design, systems, temperature]
status: draft
---

# Temperature / Exposure System

Related: [[Weather System]] · [[Day/Night Cycle System]] · [[Hunger System]] · [[Thirst System]] · [[Inventory System]]

## Overview
A single bidirectional meter (Cold ↔ Neutral ↔ Heat), driven entirely by Weather and Day/Night conditions rather than fixed turn-decay like Hunger/Thirst. Fully independent track — does not interact with existing Debuffs directly. First seeded via [[Weather System#-cold-snap|Cold Snap]]'s spoilage-slowing effect.

---

## Meter Structure

| Range | Stage | Debuff |
|---|---|---|
| -100 to -76 | **Frozen** | Significant STR/AG penalty, HP drain over time (frostbite-style), until warmed |
| -75 to -31 | **Cold** | Reduced Stamina regen, mild AG penalty |
| -30 to -1 | Chilled | None — early warning stage |
| 0 | Neutral | None — safe baseline |
| 1 to 30 | Warm | None — early warning stage |
| 31 to 75 | **Hot** | Increased Thirst drain rate, mild Stamina penalty |
| 76 to 100 | **Overheated** | Significant Stamina/STR penalty, HP drain over time (heatstroke-style), until cooled |

---

## Drift Rates (points per turn)

| Condition | Drift | Direction |
|---|---|---|
| Cold Snap | -2.0 | Toward Frozen |
| Storm | -1.5 | Toward Cold (rain + wind chill) |
| Rain | -0.5 | Toward Cold (mild, wetness) |
| Clear (Night) | -0.5 | Toward Cold (natural night temperature drop) |
| Fog | 0 | No drift |
| Clear (Day) | +0.5 | Toward Heat (mild warmth) |

**Recovery rate:** ~half the onset rate when conditions return to Neutral-favorable (e.g., near a Campfire, or weather shifts to Clear) — recovering from extremes is intentionally slower than falling into them.

**Example pacing:** Cold Snap unmitigated reaches Frozen (-76) in ~38 turns from Neutral — comparable urgency to Thirst's Dehydrated tier, fitting within a single 70-turn Night.

---

## Mitigation

**Cold:**
- Campfire (doubles as warmth source, not just light)
- Torch (TBD — may provide minor warmth in addition to light)
- Hot food / Ale
- Warm clothing/armor (not yet built — future Armor category hook)

**Heat:**
- Shade (location-dependent, TBD)
- Water consumption
- Removing heavy gear
- Proximity to water sources (Sunken Well, River Area)

---

## Design Notes
- Bidirectional and future-proofed — Region 1 (Forest) will lean Cold-biased in practice, but Heat becomes relevant once later regions (e.g., a desert) are introduced
- Deliberately kept as a fully separate track from Hunger/Thirst, with one exception: Hot stage increases Thirst *drain rate* (a rate modifier, not a direct debuff-to-debuff interaction) — mirrors real dehydration logic without breaking system independence
- Campfire's dual light+warmth role reinforces it as a key "safe camp" moment distinct from a disposable Torch

## Open Items
- [ ] Confirm whether Torch provides any warmth, or purely light
- [ ] Design Armor/Clothing category for passive Cold/Heat resistance
- [ ] Define exact HP drain rate for Frozen/Overheated stages
- [ ] Decide if any Region 1 locations have inherent Warm/Cold bias (e.g., Sunken Well area cooler, Deep Cave Mouth colder)
- [ ] Build Heat-relevant content once a future desert-type region exists

## Changelog
- Built as a bidirectional single-meter system (not two separate tracks)
- Locked meter range (-100 to 100) with 7 named stages
- Locked drift rates per Weather/Day-Night condition
- Confirmed Hot stage's Thirst-drain-rate increase as the one intentional cross-system touch
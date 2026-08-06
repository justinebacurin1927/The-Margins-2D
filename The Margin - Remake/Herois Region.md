---
tags: [the-margin, game-design, world, region, map]
status: draft
---

# Herois Region — The Game World

Era: ~Year 408–418 (Novelborne-held / contested Herois, during the Evermove conquest)
Related: [[World Structure System]] · [[Gameplay Roadmap]] · [[Lore System - Storyline Roadmap]] · [[Protagonist - Klein]] · [[NPC System]] · [[Currency System]]

## Overview
The **entire game takes place in Herois** — the westernmost, poorest, forested frontier of the Kingdom of Evermove (Region E). This doc defines the *playable* region: its canon geography, its spatial spine, the danger gradient (which replaces floor-descent), where the 11 [[World Structure System]] locations sit, and where Klein starts and escapes. Grounded in The Margins' own world map and novel — invented nothing, placed everything.

---

## Canon Anchors (from The Margins)

| Fact | Source |
|---|---|
| **Coneros (Corneo)** is a **northern/northwest border town**, at the **treeline**, on the **NW border with Novelborne** | Prologue; the Lamilla cross "from the neighboring kingdom beyond the northwest border" |
| **The Copper Road** is Herois's spine — runs **west from Magnus** through Herois (past **Westwatch**) toward **Tradewick** and the border towns | Main story ("travels the Copper Road toward Tradewick", "Westwatch is…") |
| **A stone watchtower** stands "on the border of Herois and Magnus, near the Copper Road" | Main story → the [[World Structure System|Collapsed Watchtower]] (active in Klein's era) |
| Herois settlements: **Westwatch, Tradewick, Mildtown, Silverkeep** | Main story / rough notes |
| **The Gilimans** are Herois's enforcers (Evermove's arm in the conquest) | Prologue |
| Dense **pines and fog**; "the poorest corner of Evermove" | Prologue |

> Note: this is the region as it becomes by Year 468. In Klein's era (~410) the *political* overlay differs — Herois is Novelborne-held/contested as Evermove takes it — but the **geography is stable** (rivers, the Copper Road, the NW border, the towns).

---

## Orientation — The Spatial Spine

The region has a built-in direction, and it's the heart of the game:

- **West / Northwest = HOME.** Novelborne lies just across the **treeline / NW border**. Safety. Klein's goal.
- **East / Interior = THE INVASION.** Evermove and the Gilimans sweep in from the interior, **down the Copper Road**. Danger — and the best loot.
- **The Copper Road = the east–west spine** connecting them.

**The core tension:** Coneros is a *border* town, so **home is only a few miles of forest away** — but the invasion turns those few miles into the deadliest journey of Klein's life. He is pulled two ways: **east toward danger** (for the resources survival demands) and **west toward the border** (to escape). Home is close enough to taste and lethal to reach.

---

## The Danger Gradient *(replaces descent)*

Instead of going *down* floors, difficulty rises *across* the map:

| Zone | Danger | Character |
|---|---|---|
| **Western border belt** | Low | Nearer home; thinner occupation; the starting/foraging woods — *but the crossing itself is contested* |
| **Central — the Copper Road** | Medium | The occupiers' artery; patrols, mercenaries, the towns and camps strung along it |
| **Eastern interior / deep forest** | High | Toward the invasion's source; the richest loot, the worst threats, the deep-forest secrets |

Add the [[Day & Night Cycle System|day/night]] and [[Weather System|weather]] overlays and any tile's danger becomes a *forecast* to plan around.

---

## Location Placement — the 11 World Structures in Herois

Placing [[World Structure System]]'s locations into the real geography:

| Tier | Location | Where it sits |
|---|---|---|
| **1** | Hunter's Blind | Western/near woods — a tracking platform in the starting forest |
| **1** | Fallen Log Hollow | Scattered wilds — the safe-rest beat |
| **1** | Forest Shrine | Off the trails — quiet, no hazard |
| **1** | Beehive Grove | Deeper thickets — harvest risk scales |
| **2** | Worn Down Kitchen Camp | Off the **Copper Road** — an abandoned mercenary camp |
| **2** | Collapsed Watchtower | **Herois–Magnus border, near the Copper Road** (canon) — eastward |
| **2** | Poacher's Camp | Central forest, near game trails |
| **2** | Sunken Well | Near an old settlement / the road |
| **3** | The Old House | Deep forest — a civilian ruin off the beaten path |
| **3** | Mercenary Graveyard | Deep/eastern — where the merc company is buried (being *filled* in Klein's era) |
| **3** | Deep Cave Mouth | Far edge — a **Region-2 / expansion threshold**, *not* the home exit |

---

## Start & Exit

- **Start:** **Coneros (Corneo)**, the NW border town — playable in Act 0 before and during its fall.
- **Win / Exit:** the **northwest border crossing** into Novelborne. Reaching the Novelborne side = escaping the war = the canonical GET HOME ending. The full reunion (the Novelborne mainland, Magdalene, his parents) plays out in the **epilogue beyond the playable region**.
- **Not the exit:** the **Deep Cave Mouth** points the *other* way (deeper/east, toward a future Region 2). It's an expansion/"What if" threshold, never the road home.

---

## Map Structure — Handcrafted / Procedural / Hybrid *(proposal)*

Recommended: **hybrid.** The **canon landmarks are fixed** (Coneros, the Copper Road, the NW border crossing, the Watchtower on the Magnus border), while the **wilderness between them is procedural** — so every run is a different forest, but the world stays canonically Herois and the spatial spine (west=home, east=danger) always holds. This preserves roguelike variety without breaking canon or the story's geography.

---

## Open Items
- [ ] Confirm map form: **one continuous tiled region**, or **connected sub-areas** (e.g. Coneros → forest zones → border) stitched together?
- [ ] Design the **border-crossing "win" mechanic** — reach a tile? survive a final gauntlet? a scripted crossing?
- [ ] Lock **handcrafted vs procedural vs hybrid** and the generation rules for the procedural wilderness
- [ ] Pin exact relative placement/spacing of the 11 locations (a rough region layout sketch — see Map.png context)
- [ ] Confirm the **Gilimans**' role/allegiance/naming in Klein's era (canon enforcers of Herois vs. Evermove's conquering arm)
- [ ] Decide whether Copper Road towns (Westwatch, Tradewick, Mildtown, Silverkeep) appear as visitable locations or remain off-map lore

## Changelog
- Locked the game scope to the **Herois region**, grounded in The Margins' canon map + novel
- Established the spatial spine: **west = home (NW border), east = the invasion (down the Copper Road)** — home is close but lethal to reach
- Defined the **danger gradient** (west→east) as the no-descent replacement for floors
- Placed the 11 World Structure locations into canon geography
- Set start = **Coneros**, exit = the **NW border crossing** (Deep Cave Mouth reclassified as a Region-2/expansion threshold, not the home exit)
- Proposed a **hybrid** map (fixed canon landmarks + procedural wilderness)

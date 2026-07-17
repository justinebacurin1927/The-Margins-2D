# The Margins — Game Vision (North Star)

**Status:** North Star / destination doc. Not the MVP scope — the full-game ambition. Established 2026-07-17.
**Scope stance (explicit):** depth over speed. The roadmap may grow well past 30 epics; that is intended, not a risk to manage away. Every system below is committed to the final game. Build order stays *systems-first*, one signature system foregrounded per region.
**Companion doc:** the opening (Act 0 + Forest) is specced in `opening-design-act0-forest.md`.

---

## Vision statement

The Margins is a **hybrid story + survival game** with **region-based (arc) progression** instead of a deep dungeon. The player travels *horizontally* along the road of the novel — each region a self-contained arc with its own biome, enemies, resources, NPCs, quests, mechanics, and story events. Progress is made by moving *forward* through the story, not *down* through difficulty tiers.

**Pillar — Hybrid (Story + Gameplay):** every system serves the novel, and the novel supplies every system's content. Combat, bosses, NPCs, and quests are all drawn from *The Margins*' actual characters and events — no parallel invented world.

**Combat identity (reconciled):** avoidance-first survival, but combat is real and present.
- **Fightable:** region enemies (Gilimans, Lamilla scouts, bandits, beasts) — beatable with weapons, skills, and survival prep.
- **Unwinnable story walls:** certain antagonists (e.g. **Spearshot**) cannot be beaten yet — they are *evasion* bosses that teach you to survive, not win. This is faithful to the source, where Milek's fights fail and the strong are "elephants to ants."
- **Social bosses:** some antagonists (e.g. **Bulwark Swan**) are defeated by outmaneuvering, not fighting.

---

## Core Systems (all committed)

**World**
- Region (Arc) progression — the novel's road
- Main storyline (adapted chapter-by-chapter)
- Day & Night cycle
- Dynamic weather
- Fog of War *(shipped — Epic 2)*

**Survival**
- Hunger *(shipped — Epic 1)*
- Temperature · Fatigue · Wetness (realism meters, introduced one region at a time)
- Fire & Shelter
- Environmental hazards

**Combat & Progression**
- Combat system (weapons + player progression)
- **Bosses & mini-bosses** (story-drawn; fightable, evasion, or social)
- Skills system (combat + survival techniques)

**Items**
- Inventory *(Epic 3)* · Treasure & loot · Crafting & components

**World Interaction**
- Dialogue & text *(Epic 5)*
- Main quests *(Epic 6)* · Side quests
- **NPCs & neutral characters** (allies, benefactors, bystanders) with behavior & schedules
- Hidden resource system

---

## Gameplay Loop

Enter a region → **talk to NPCs & neutral characters** → accept main + side quests → explore under Fog of War (day/night, weather) → gather resources & hidden items → **evade or fight enemies; face the region's mini-boss/boss** → manage hunger, temperature, fatigue, wetness → craft tools/weapons/consumables/fire → gain skills & better gear → complete quests → advance the story → **travel to the next region.**

---

## Region Progression = the Novel's Road

Replaces the generic Forest/Swamp/Frost/Ashen/Ruins list. Each region maps to a leg of Milek's journey. Signature system = the new mechanic that region foregrounds. **Route 1 (Pre-7-Years Arc)** is the near-term game; later macro-arcs are future region-sets.

| # | Region (novel) | Signature system(s) | Enemies | Boss / Mini-boss | Key NPCs & neutrals | Story beat |
|---|----------------|---------------------|---------|------------------|---------------------|------------|
| **0** | **Coneros — The Fall** (Prologue–Ch2) | authored intro; controls-by-loss | Gilimans (scripted, unbeatable) | **Boss:** Giliman Banneret *(scripted loss)* | Family, **Mora** (helper), townsfolk (neutral) | The massacre; Liga-Meteor; flight into the pines |
| **1** | **North Pines — Whispering Forest** (Ch3) | Hunger, fire/shelter, **rain/wetness**, day/night, crafting, stealth | Lamilla scout-knights | **Mini-boss:** the two knights who trail you home *(Ch3 climax)* | **Ashen merchants** (goal, neutral), Erik (companion) | Survive the pines; the five-night camp raid; the tail home |
| **2** | **Pinehurst / The Convoy** (Ch17–18) | **captivity/observation** (the gap in the door), feeding-line rationing, NPC schedules | Rhys's borrowed knights | **Social boss:** the feeding system itself (starvation) | **Henry** (ally), the Pack — Charcoal/Perfect/Rooster (allies) | Sold to Swan; the road as a cage; banking intel |
| **3** | **Tradewick — Gray Law** (Ch19) | **skills & side quests** (a lawless hub), stealth in a crowd, "notability" | Guild mercenaries, slavers | **Social boss:** **Bulwark Swan** (outmaneuver, don't fight) | **Yuna Montclaire** (ambiguous buyer), **Clara & Cleopathra** | The auction; separation; the first real opening |
| **4** | **Coastal Road → Oakdale** | **temperature/exposure**, open-ground stealth, weather (wind/storm) | Unknown coastal mercenaries | **Evasion boss:** **Spearshot** (unbeatable — survive, don't win) | Wallington locals (neutral) | The salt air; the road narrows toward Mirko |
| **5** | **Mirko — Frost Pass** | **cold/snowstorms**, fire & shelter critical, ice hazards | Mountain bandits, beasts | **Mini-boss:** the pass bandits / a hunting beast | Mirko mercenaries (neutral-for-hire) | The unmapped bottleneck; weather can close the road |
| **6** | **Valens — The Wall** | endgame survival; prison starvation; the crack in the wall | Prison guards | **Boss:** the escape gauntlet | **The old Twilight Knight** (mentor, neutral→ally) | Imprisonment; apprenticeship; the 7-year threshold |

**Beyond Route 1 (future region-sets):** Academic Arc · Revolution Arc · Throne War Arc · One Kingdom Arc · The Heaven Fell Arc. Each becomes its own cluster of regions when the game reaches it. **The Blackberry Troupe / Theodore** recur as mysterious benefactor NPCs throughout.

---

## System Interactions (the design philosophy)

Every system feeds another; none stands alone.

- **Weather → survival:** rain raises wetness & drops temperature; snow brings cold hazards; storms cut movement & visibility; wind shifts noise and scent.
- **Survival → combat:** low hunger/fatigue/cold weakens combat and stealth; a fed, rested, dry Milek fights and hides better.
- **Combat → exploration:** clearing or evading threats opens routes and caches; bosses gate region exits.
- **Exploration → crafting:** hidden resources (below) feed tools/weapons/fire/food.
- **Crafting → survival:** fire, shelter, and food are crafted answers to weather and hunger.
- **Day/night → everything:** visibility, NPC schedules, enemy spawns, and time-gated resources all shift; camp & rest matter.
- **Dialogue → story & quests:** talking advances the main story, unlocks side quests, and builds NPC relationships (Bond).
- **Story → progression:** completing a region's arc unlocks the next region and its new signature system.

---

## Hidden Resources (Forest example — Region 1)

- **Tall Grass** → Corn · Berries
- **Fallen Logs** → Mushrooms · Dried Moss · Tinder Bundle
- **Trees** → Wet Branches · Dry Branches · Bark
- **Rocks** → Pebbles

Each later region defines its own hidden-resource table matched to its biome and signature system.

---

## Guiding principle

Each new region expands the game by introducing a **new mechanic and a new chapter of the story together** — never by only raising enemy numbers. Exploration, survival, combat, and storytelling evolve as one. The game is finished when the road is walked, however many epics that takes.

---
title: The Margin
status: final
created: 2026-08-06
updated: 2026-08-06
---

# PRD: The Margin
*Working title — confirm.*

## 0. Document Purpose

This PRD is the build contract for **The Margin**, a 2D top-down, turn-based survival roguelike set in Herois, the westernmost forested frontier of the Kingdom of Evermove, ~50–60 years before the main novel. You play **Klein**, a young noble-born Novelborne knight on his first, routine posting to the border town of Corneo. On day one, Evermove invades; you flee into the pine-and-fog forest alone and hunted, and your goal is the smallest and most human one imaginable: **get home** across the northwest border to your fiancée Magdalene and your aging parents.

This PRD builds on two sources and does not duplicate them:
- The **finalized Product Brief** (`planning-artifacts/briefs/brief-The-Margin-2026-08-06/brief.md`) — the why and the scope.
- The **26-doc design bible** (`The Margin - Remake/`) — the authoritative system and world reference.

It is for the solo developer (Justine), and for the downstream BMad workflow owners (UX, architecture, epics-and-stories, sprint planning). It is structured: §3 Glossary anchors vocabulary; §4 Features groups the capability requirements with globally numbered FRs; §7 Success Metrics cross-references them; every `[ASSUMPTION]` is tagged inline and indexed in §9. This PRD resolves the brief's seven Open Items with recommendations (tagged `[RECOMMENDED]`); where a decision formally rests with the solo dev, the tag reads as `[ASSUMPTION]` pending confirmation.

## 1. Vision

The Margin is one continuous, permadeath life across a persistent Herois — not a floor-descent, not a combat-XP ladder. You are a trained knight who can fight, in a war too big for fighting: every fight costs HP, weapon durability, noise that draws reinforcements, and occupation escalation. Survival is choice, not chores — every hunger, thirst, and cold decision is a gamble under scarcity, and the campfire that gives you warmth, light, cooking, and clean water is also the light and smoke a patrol can see.

The story is earned, not told. A skippable text intro covers the fall of Corneo; gameplay opens mid-flight with **Aldric**, a fellow knight who teaches you how to play diegetically, then is captured the moment you've learned the ropes — the first loss and the seed of a rescue quest. The whole game is pulled two ways: east toward the Copper Road and the occupiers for what survival demands, west across the border toward home. In a world of god-kings and 174-year immortals, Klein's goal is the smallest and most human one imaginable — survive, go home, marry the girl, live.

Progression is horizontal — you grow by SKILL, by gear-with-memory (repairs lower its max, so gear is precious), by knowledge of the forest, and by allies, never by kill count. One canonical spine (GET HOME) is built first and deep; the two unchosen purposes — become the Ant, chase the Buried Truth — are architecturally supported as later player-decided branches, so depth can be layered on — Region 2 through the Deep Cave Mouth, more companions, more of the buried truth — without ever breaking the canonical story or the core survival loop. The Margin is a complete prequel: the Corneo you flee becomes the Coneros Milek is born into, the Mercenary Graveyard fills during your run, and the occupation that shapes a later world begins here.

## 2. Target User

### 2.1 Jobs To Be Done

- **Survive the collapse** — stay alive as the garrison and the region fall around you, day by day.
- **Go home** — cross the conquered kingdom to Magdalene and your aging parents; reclaim the ordinary future the war stole.
- **Understand the war** — find out what is actually happening; you are on the losing side of a war whose scale and reasons a border town can't see.
- **Learn the forest** — grow by knowing Herois (locations, safe routes, mushroom safety, recipes), not by inflating numbers.
- *(Builder's JTBD)* **Prove the design** — a game that is *mine*, honoring the source canon and the SPD-style presentation lock, not a reskin of Shattered Pixel Dungeon.

### 2.2 Secondary Player

- **The narrative-first player** — burned by story games where death means losing the story. Here death is the run, and the run is a life; the canonical GET HOME spine is the through-line, not a cosmetic wrapper. *[RECOMMENDED: treat as secondary but design for; the survival loop must stay playable-first per SM-2.]*

### 2.3 Non-Users (v1)

- **The power-fantasy roguelike player** — the game is deliberately not a "hero clearing dungeons" power trip; the protagonist's training is his edge, not his destiny.
- **The completionist** — permadeath means one life per run; no save-scumming, no "collect everything" framing.
- **Players who want a purely mechanical loop** — the story is load-bearing, not cosmetic.

### 2.4 Key User Journeys

The user journeys are captured from the bible's design beats; they are meant to be validated (or replaced by your own lived playthroughs) during PRD review. UJ IDs are stable for downstream reference. *[ASSUMPTION — UJ narratives are bible-derived, not playtest-derived; validate in review.]*

- **UJ-1. Klein's first night alone.**
  - **Persona + context:** Klein, 24, Novelborne knight, fleeing into the pines after Corneo's fall; Aldric just captured; no supplies beyond what he grabbed.
  - **Entry state:** Act 1 start, one Tile T1-4 ("Ash and Pine"), full Hunger/Thirst, day. No items, no light.
  - **Path:** scavenge a fallen log hollow and a hunter's blind by daylight → read a torn journal note (discovery quest seed) → as dusk approaches, bank a fire — gather wood + coal, find water, craft a torch → hold the night: campfire warmth/light vs. its visibility to a patrol → survive to dawn.
  - **Climax:** dawn breaks; Klein is alive, slightly Hungry/Thirsty, and has learned the day/night planning rhythm. The message log reads like survival.
  - **Resolution:** the first solo night is survived; the player now understands the four countdowns (Hunger, Thirst, Temperature, Day/Night) and the "lit camp is a visible camp" tension.
  - **Edge case:** if the fire draws a Giliman patrol, the player must flee into fog — losing the camp's cooking/purify benefits for the night but trading heat for stealth.

- **UJ-2. A day's foray east for water and coal.**
  - **Persona + context:** Klein, Day 2–3, camp established; needs clean water (Sunken Well) and coal (to boil it), so he pushes one danger-tier east.
  - **Entry state:** Safe camp in Tier 1, Satisfied/Hydrated, a few turns of daylight left; inventory near full of scavenged food.
  - **Path:** travel the eastward gradient → reach the **Sunken Well** (Tier 2) → weigh the well's clean water against slip-and-fall risk → draw and boil water with the coal he carried → grab rare coins → start back before night flips the well's creature active and the return trip darkens.
  - **Climax:** he returns to camp with purified water and coal before nightfall, an eastward foray completed against the clock.
  - **Resolution:** the foray loop — leave a safe point, travel to a location, scavenge under its hazard, haul back before night/weather turns — is now internalized; the day is the planning unit.
  - **Edge case:** if night catches him mid-return, the Sunken Well's creature and a dark forest both threaten; he burns a torch for the retreat.

- **UJ-3. Choosing the east pull.**
  - **Persona + context:** Klein, mid-Act 1, learning Aldric is held east along the Copper Road — the map's central tension made personal.
  - **Entry state:** Act 1 established (Tier 1 range), a small safe camp, barely enough supplies for one more day west toward the border or one day east toward danger.
  - **Path:** the rescue thread pulls east (danger + loot) against home's west pull → a limited daylight window forces a choice: forage west toward the border for safety, or risk a Tier 2 push east for what Aldric's rescue (and survival) demands → weather rolls; fog favors stealth.
  - **Climax:** he commits east, and the occupation's face (a Giliman patrol, Commander Vos's reach) is met for the first time.
  - **Resolution:** the survival need and the rescue need have merged into one decision; the player has lived the "pulled two ways every day" core tension.
  - **Edge case:** overreach means not making it back before night; the game's four countdowns make the overreach concrete, not abstract.

- **UJ-4. The earned homecoming.**
  - **Persona + context:** Klein, Act 3, the last venture east for what the crossing demands, then the hard drive west.
  - **Entry state:** Act 3 begun; the choice is made — home; last provisioning (Tier 3: Old House, Mercenary Graveyard), then the NW border crossing.
  - **Path:** the closing trap tightens (Vos's final pursuit) → the NW border crossing is a final tense run, not a boss → Klein crosses into Novelborne, earned, having lived the story told through the systems.
  - **Climax:** the border; home a few miles of forest away; the run that was the whole game.
  - **Resolution:** the canonical ending lands — he makes it home — and the epilogue seeds (Corneo becomes Coneros, the Graveyard is the grave he watched dug) visibly connect to the main-story canon.

## 3. Glossary

Every domain noun the rest of the document uses, defined once. Downstream workflows must use these terms exactly. *[ASSUMPTION — Glossary is bible-anchored; terms fixed for downstream workflows.]*

- **Herois** — the westernmost, poorest, forested frontier region of the Kingdom of Evermove (Region E), where the game is set (~Year 408–418). Canon-anchored; danger rises east, safety lies west.
- **Novelborne** — the mainland kingdom Klein is from and serves; home to Magdalene and his aging parents, across the NW border. Herois is Novelborne-held before the Evermove conquest.
- **Corneo** — the pre-conquest Novelborne border town at the treeline; Klein's posting. Becomes **Coneros** in the main-story era (the town's name is a casualty of the war he's living through).
- **Coneros** — Corneo's name ~50 years later, carried into the main story (Milek's hometown). Canon tie.
- **Copper Road** — the east–west trade artery and the game's spatial spine: the invasion comes down it from the east; it connects the border towns to the interior. Runs through Corneo, past Westwatch, toward Tradewick and the interior.
- **Evermove** — the kingdom; the occupying power. Its king has reigned 174 years through the **Tribunal Beings**, and claims divine power (the Buried Truth thread).
- **Gilimans** — Evermove's Herois enforcers; the occupation's face, the primary human threat. Named face: **Commander Vos**, Klein's recurring pursuer.
- **The Free Company** — Valens sellswords, active and dangerous in Klein's era; their end is the Watchtower → Kitchen Camp → Graveyard lore loop.
- **Sense-users** — Evermove's elite, "elephants"; scripted, avoid-only set-pieces. They broke Klein's garrison in the intro.
- **Sense** — the world's earned-power system (five senses awakened through discipline); carries the **Sensory Burden** (Debt → Overload → irreversible **Echo**). The "Ant" What-if branch.
- **The Ant** — the What-if ending where Klein awakens Sense power at the Sensory Burden's cost; a later branch, not canonical.
- **The Buried Truth** — the What-if ending where Klein pursues the king's questionable divinity; a later branch, not canonical.
- **Turn** — one action in the tile-by-tile economy (move, scavenge, eat, craft, fight, hide, rest). Day = 100 turns, Night = 70, full cycle = 170.
- **Day/Night Cycle** — the 170-turn day/night clock; Night without a light source shrinks **FOV**, raises encounter/aggression risk, and flips several locations hostile (the Mercenary Graveyard's undead, the Sunken Well's creature).
- **Weather** — a per-cycle roll (Clear 40% / Rain 25% / Fog 20% / Storm 10% / Cold Snap 5%) that stacks with the Day/Night Cycle to shape visibility, travel, and Temperature.
- **FOV** — field of view: the tiles Klein can currently see, computed by line of sight; Night and Fog shrink it, light sources restore it (reduced).
- **Foray** — the mid loop: leave a safe point → travel to a World-Structure location → scavenge under its hazard → haul loot back before night/weather turns.
- **World-Structure Location** — one of 11 fixed-content scavenge destinations across 3 danger tiers (Tier 1 Hunter's Blind / Fallen Log Hollow / Forest Shrine / Beehive Grove; Tier 2 Worn Down Kitchen Camp / Collapsed Watchtower / Poacher's Camp / Sunken Well; Tier 3 The Old House / Mercenary Graveyard / Deep Cave Mouth).
- **Danger Tier** — a 1–3 rating of a World-Structure's hazard/loot trade; better loot carries higher structural, creature, or contamination risk.
- **Copper / Silver / Gold / Royal Gold Plaque** — the four-tier currency (25 Copper = 1 Silver, 10 Silver = 1 Gold, 1,000 Gold = 1 Royal Gold Plaque). Scarce by design; no fixed shop, only two mobile traders.
- **Traders** — the **Traveling Wanderer** (barter or coin; Free Company deserter) and the **Caravan Black Market Trader** (coin-only; guarded; killing = permanent trade lockout). The only coin sinks.
- **Campfire** — the stationary survival anchor: warmth + light + cooking + water purification, but visible and audible to patrols (the choice core).
- **Torch** — craftable (Wood + Coal) light, 60-turn burn (~one Night); warmth TBD.
- **Debuff** — a stacked, tiered negative condition (Nausea→Fever→Delirium; Trembling; Headache; Withered; etc.) that reduces stats until cured, eaten out of, or drunk out of.
- **Last Stand** — the permadeath safety valve: once per run, at 0 HP, a GRIT-based check may leave Klein at 1 HP with no bonus. The run is otherwise permanent.
- **SKILL** — one of the six core stats (STR, GRIT, INS, AG, VOICE, SKILL); governs crafting, cooking, purification, repair, lockpicking — the horizontal growth path.
- **Bond** — a per-companion relationship value deepened by shared survival and dialogue choices; unlocks lore, loyalty, personal quests.
- **Companion** — a recruitable Neutral-pool NPC who travels with Klein as a **full tile-agent**, not a follower: own **Status** block, own **HP** pool, and autonomous **AI** (a behavior state machine — follow / hold / hide / distract / fight / flee). Help *and* liability (feeds, louder, must be protected). Non-combatants do not fight. Roster: **Aldric** (combat), **Mara** (caches/shelter), **Old Fen** (survival mastery), **Sister Yenna** (hidden knowledge / Mystic / the Ant seed).
- **Act** — the story's throttle: main-story quests gate the three acts (Survive → Understand → Decide). Act 0 (the Fall of Corneo text intro) is not a numbered gameplay act.
- **GET HOME** — the canonical goal and the win condition: cross the conquered kingdom to the Novelborne mainland. Ending locked: he makes it home.
- **Epilogue** — the post-ending canon seed: Corneo → Coneros, the Graveyard filling now, the occupation that shapes Milek's world.
- **SPD-style presentation** — the presentation lock borrowed from Shattered Pixel Dungeon: 2D top-down tiles, turn-based, bottom message log, minimal HUD. The game keeps SPD's *skin* and swaps its *skeleton* (no floor-descent, no combat-XP).
- **Mystic** — one of the world's power grammars (collective belief); the old faiths Sister Yenna guards; threads into the "Ant" What-if branch.
- **Neutral-pool** — NPCs who are neither hostile nor traders (quest-givers, world-texture, companions); the pool the four companions are drawn from.
- **Region 2** — the future expansion beyond Herois, reached via the Deep Cave Mouth threshold; deferred, not built in v1.

## 4. Features

Features are grouped; FRs are numbered globally and stable. User journeys are referenced by ID ("realizes UJ-X").

### 4.1 The Fall of Corneo (Act 0 — Text Intro + Onboarding)

**Description:** The game opens with a text-forward intro covering the *before* (Klein's posting, his two duties — guard the Copper Road, guard the town —, Magdalene's letter, Aldric) and the *fall* (the midmorning horn, the Evermove column, the Sense-user who undoes men with a look, Corneo burning). Read or skip, paged screen by screen. It hands into gameplay mid-flight: Klein and Aldric, fleeing deep in the trees, Corneo burning behind them. Realizes UJ-1's entry.

**Functional Requirements:**

#### FR-1: Text intro, read or skip
Klein's story opens with a skippable, paged text intro covering the before and the fall of Corneo. Realizes UJ-1.
**Consequences (testable):**
- A "skip" path exists on every intro screen and skips to gameplay in one action.
- The intro reads in the SPD-style text-forward tone and covers the before, the fall, and the hand-off (Klein and Aldric fleeing).
- Intro screens do not tick survival tracks or consume turns.

#### FR-2: Aldric's diegetic tutorial
During the opening flight, Aldric teaches the how-to-play *in-fiction* — controls, hiding, reading danger, survival basics — with no tooltips. Realizes UJ-1.
**Consequences (testable):**
- The tutorial is delivered by Aldric as in-world dialogue, not UI chrome.
- All controls (move, scavenge, eat, craft, hide, rest) are demonstrated diegetically during the opening flight.

#### FR-3: Aldric's capture (the wound)
The moment the player clears the how-to-play, the chasers catch Aldric; Klein escapes alone, and his teacher and only comrade is taken. Realizes UJ-3's seed.
**Consequences (testable):**
- Aldric leaves the party by capture (recoverable via a later rescue quest), not death.
- The message log and a discovery seed establish that Aldric is held **east**, along the Copper Road.
- The capture is the opening of Act 1's wound; the rescue thread is the seed of UJ-3's east-pull tension.

### 4.2 The Turn & Survival Core

**Description:** The turn is one action (move · scavenge · eat · drink · craft/repair · fight · hide · rest · wait), each advancing the four survival tracks, the day/night clock, debuffs, and nearby-threat detection/noise. The four nested loops — Turn (seconds), Foray (minutes), Day (170 turns), Act (the arc) — are the game's economy. Realizes UJ-1, UJ-2.

**Functional Requirements:**

#### FR-4: Four survival tracks
Klein has four independent, persistent survival tracks: **Hunger**, **Thirst**, **Temperature/Exposure**, and the **Day/Night** clock. Realizes UJ-1, UJ-2.
**Consequences (testable):**
- Hunger tiers: Well Fed (350 turns; Bloated regen+slow), Satisfied (250, default), Hungry (250, warning), Starving (150 total: Fatigue -35% STR → Trembling -15% AG → Rotting -3 HP/2 turns). Starvation to death ≈ 650 turns from Satisfied.
- Thirst tiers: Hydrated (200), Thirsty (150), Dehydrated (100; Headache), Parched (80, 3 stages: Withered → Trembling → Dried Out, -2 HP/5 turns). Thirst-to-death ≈ 530 turns.
- Temperature: a -100..+100 bidirectional meter driven by Weather + Day/Night (Frozen/Cold/Chilled/Neutral/Warm/Hot/Overheated), with drift rates per condition (Cold Snap -2.0/turn → Frozen in ~38 turns). Recovery ≈ half the onset rate.
- All four tracks tick only on actual turns (a keypress into a wall spends no turn — survival-clock honesty).

#### FR-5: Day/Night and Weather
Day = 100 turns / Night = 70; each 170-turn cycle rolls a Weather type (Clear 40% / Rain 25% / Fog 20% / Storm 10% / Cold Snap 5%) that stacks with the clock. Realizes UJ-1, UJ-2.
**Consequences (testable):**
- Night without a light source shrinks FOV radius and raises enemy encounter/aggression; with light, FOV is restored but reduced.
- Weather type is rolled per cycle with the weighted distribution above, and each type has a listed pro/con (e.g., Fog reduces both parties' visibility; Storm raises structural-collapse chance; Cold Snap slows spoilage but drives Temperature toward Frozen). *[ASSUMPTION — weather weights and day/night lengths are bible values, carried as the starting calibration.]*
- Day is the planning unit: forage/travel by day; shelter/cook/mend by night (or brave the dark with a torch at worse odds).

#### FR-6: Food, water, and purification
Klein must source, cook, and purify food and water; both spoil, and unsafe consumption is the debuff pipeline. Realizes UJ-2.
**Consequences (testable):**
- Water sources: Sunken Well (stable), Pond (requires both filtration AND boiling), River Area (drink direct with a 20% poison risk, or collect and purify).
- Purification is two-step: filtration (SKILL-based, reduces but doesn't eliminate risk) then boiling (Coal + fire → 0% risk). Untreated raw water carries its source's risk (river 20%; pond worse); boiling is what makes any raw water safe.
- Food spoilage ladder: Fresh → Half Rotten → Fully Spoiled; cooked meat and purified water resist spoilage; storage items reduce the rate.
- Cooking and purification are SKILL-governed; SKILL is the horizontal growth path (see FR-11).

#### FR-7: Temperature forces fire
Cold/heat are choices under scarcity: the campfire gives warmth, light, cooking, and clean water but is visible and audible to patrols. Realizes UJ-1.
**Consequences (testable):**
- An unmitigated Cold Snap reaches Frozen in ~38 turns (inside one 70-turn Night) — the campfire is the mitigation that simultaneously solves warmth, light, cooking, and purification.
- The campfire is stationary and lit; it doubles as a cooking/purification station and is exposed (visible/noisy to patrols) — the player can trade heat for stealth by leaving it.
- Torch: craftable (Wood + Coal), 60-turn burn (≈ one Night), light-only (warmth TBD).

#### FR-8: Debuff system
Survival pressure expresses as tiered, stacked debuffs with escalation chains and real cures. Realizes UJ-1, UJ-2.
**Consequences (testable):**
- Bacterial track: Nausea (-30% STR, 30t) → Fever (-40%, 25t) → Delirium (Paranoia + Vertigo + Crippled, 40t); Diarrhea runs parallel (Stage 1: 2x Thirst/Stamina drain, Stage 2: 3x Thirst/Stamina/Hunger drain, lethal if ignored).
- Mushroom/toxin track: Rotgut (instant Nausea + Crippled + Diarrhea); Honeymoon → Collapse (hidden 60-turn countdown → permanent Max HP cap at 40% until cured); alcohol-interaction toxin (latent — only triggers if Ale is consumed afterward).
- Cures: Honey/Honeycomb cure Sick/Poisoned; Bloodvein Mushroom cures Bloated (at -5 HP, 90% Poison risk); cure items shorten Delirium by 75%; the Honeymoon cap requires a cure item.
- Debuffs persist until the player eats, drinks, or is cured — turns alone do not clear them.

### 4.3 The Foray & The World

**Description:** The Foray is the mid loop: leave a safe point, travel the danger gradient east, scavenge one of 11 World-Structure Locations under its hazard, haul the loot back before night/weather turns. The map is a hybrid — fixed canon landmarks (Corneo, the Copper Road, the NW border crossing, the Watchtower) with procedural wilderness between them — so every run is a different forest but the world stays canonically Herois. Realizes UJ-2, UJ-3.

**Functional Requirements:**

#### FR-9: The persistent, traversable Herois map
The game is set in a persistent, traversable Herois — no floor-descent; danger is a gradient across a map, not a ladder down. Realizes UJ-2, UJ-3.
**Consequences (testable):**
- The spatial spine holds: west/northwest = home/border, east/interior = the invasion down the Copper Road; danger rises east, loot rises east, safety lies west.
- The map is hybrid: canon landmarks fixed, procedural wilderness between them (so the spine always holds, the forest varies per run).
- The 11 World-Structure Locations are placed across the 3 danger tiers consistent with the east/west spine.

#### FR-10: The foray loop
A foray is a complete risk/reward arc: leave a safe point, travel to a location, scavenge under its hazard, return before night/weather turns. Realizes UJ-2, UJ-3.
**Consequences (testable):**
- Each World-Structure has a listed loot set and hazard (Tier 1 Hunter's Blind: rope, small tools, 20% Map Fragment; hazard weak floor plank — through Tier 3 Old House: preserved food, cloth, locked cellar; hazard structural decay).
- Night shifts several locations' danger (Graveyard undead active, Sunken Well creature active, Poacher's Camp patrols more aggressive, Beehive Grove — the sole location that flips *safer*).
- Time pressure is simultaneous: Hunger (~650 turns to death), Thirst (~530), Temperature (fastest, ~38 to Frozen under Cold Snap), and the 170-turn Day/Night cycle — travel, scavenge, and return compete for the same turns.

#### FR-11: Horizontal progression (SKILL, gear-with-memory, knowledge, allies)
Klein grows by knowing the forest, not by kill count. Realizes UJ-2, UJ-3.
**Consequences (testable):**
- No combat-XP: killing things never raises numbers.
- SKILL (doing) governs cooking, purification, repair, lockpicking; knowledge (map fragments, mushroom/water safety, recipes, location dangers) accumulates.
- Gear-with-memory: each repair restores durability but permanently lowers max (decay curve, values per Low/Mid/High SKILL: Fresh 100% → 1st repair 90/93/96 → 2nd 78/84/91 → 3rd 65/74/85 → 4th 50/63/78 → 5th 35/51/70 → 6th+ beyond repair — Low SKILL hard-stops at the 6th repair, Mid/High get "beyond repair (marginal)"), SKILL-modified. See FR-13 for the full rule.
- Allies (companions) add capabilities and liabilities, never raw power.
- *(The Sense/"Ant" path and the Buried Truth branch are later What-if additions, not canonical; the architecture must support them without a rewrite.)*

### 4.4 Combat & Its Costs

**Description:** Combat is real, winnable, and costly — HP, weapon durability, noise that draws reinforcements, and occupation escalation. Avoidance, stealth, and preparation usually beat a straight fight. Klein is a trained knight; the war is too big for one knight. Realizes UJ-3, UJ-4.

**Functional Requirements:**

#### FR-12: Combat is viable but costly
Klein can fight, but fighting spends more than it returns. Realizes UJ-3.
**Consequences (testable):**
- Turn order by AG (higher AG acts first; tie-break TBD); actions: Attack, Block, Dodge, Use Item, Flee. *[ASSUMPTION — combat action set and turn-order-by-AG are bible-anchored; tie-break TBD.]*
- Every attack costs weapon Durability (see FR-13); combat noise feeds detection; hostiles run on a detection/noise system.
- Occupation escalation: Gilimans thicken every act — patrols, sweeps, curfews, bounties get denser and more aggressive, so fighting becomes more punishing over a run.
- Avoidance/stealth/preparation beats fighting: hide/flee are the sane option; Sense-users are avoid-only set-pieces; VOICE can de-escalate a wary patrol.

#### FR-13: Gear-with-memory
Weapons and equipment are precious because repairs permanently lower their max durability. Realizes UJ-2, UJ-4.
**Consequences (testable):**
- ~30 weapons across 5 categories × 5 tiers (T1–T5), each attack/block/chop/throw costing fixed durability; 0 = unusable.
- The repair decay curve is the hard numeric ceiling on how much fighting any one weapon can endure (roughly 5 repairs before near-dead at Low SKILL; marginal at Mid/High).
- Scavenge-on-break returns partial materials (T1–T2: 1–2 base; T3+: 2–3 base + possibly rare; T5 VIP/Legendary: 3–4 base + unique).
- Repair is SKILL-based, using weapon-specific materials (spears: Wood+Rope; bows: Wood+String/Sinew; blades: Metal Scrap; etc.).

#### FR-14: Permadeath and Last Stand
A run is one life; Last Stand is the single, once-per-run safety valve. Realizes UJ-4.
**Consequences (testable):**
- Death ends the run; permadeath (no save-scumming).
- Last Stand: auto-checked at 0 HP, GRIT-based %, survive at 1 HP with no post-trigger bonus, once per entire run. *(Open: does a failed roll also consume it?)*
- A restart begins a new life (fresh procedural forest, fixed canon spine).

### 4.5 The Story, People, and Companions

**Description:** The story is the load-bearing spine: act-gating quests tighten the occupation (Survive → Understand → Decide), Aldric's capture seeds the rescue, and the canonical GET HOME ending is the win. The four companions are full tile-agents, not followers — each with its own Status block, HP pool, and autonomous AI — who are help and liability, each carrying a lore thread. Realizes UJ-3, UJ-4.

**Functional Requirements:**

#### FR-15: Companions are full tile-agents (own Status, own HP)
Each companion is an autonomous entity with its own stat line, health pool, and condition state — never a passive follower of Klein. Realizes UJ-3, UJ-4.
**Consequences (testable):**
- Every companion has **its own Status block** — HP plus the core stats relevant to its role (combatants like Aldric carry the combat-relevant line; non-combatants carry a lighter line but still an independent HP and status state).
- Every companion has **its own HP pool** — woundable, healable, incapacitable; a non-combatant caught in a fight is a body Klein must defend.
- Companions carry their **own condition/debuff state** (wounded, hungry, thirsty, cold, Trembling) and share the survival pressure like any body in the forest.
- Whether a companion can truly die (vs. only be captured or leave) is a scope decision; if death is on the table it is rare and devastating, matching the game's permadeath weight. *(Open.)*

#### FR-16: Companion AI is autonomous, not follow-only
Companions act on their own within the turn economy via a behavior state machine driven by the same detection/noise rules as enemies — not a scripted follower. Realizes UJ-3, UJ-4.
**Consequences (testable):**
- Behaviors include **follow, hold, hide, distract, fight/retreat** (combatants) and **take cover/flee** (non-combatants); each companion's behavior set matches its role.
- A companion can act independently each turn — it does not merely mirror Klein's movement; greedy follow is a fallback, not the model.
- Companions obey the same detection/noise rules as everyone else: they can *blow* Klein's stealth as easily as help it — a hidden companion is quiet, a panicking one is loud.
- Simple orders let Klein steer behavior (hide / hold / distract — the distraction action already exists in the build).

#### FR-17: Companions as help and liability
A companion is never free — a second mouth to feed, more noise, a body to protect. Realizes UJ-3, UJ-4.
**Consequences (testable):**
- Party size: one active companion recommended *(open: 2, or a safe-node holdout that gives passive support — supply drops, intel — rather than traveling)*; companions cost extra food, add a noise penalty to stealth, and can be wounded.
- Non-combatants do not fight (Mara, Old Fen, Yenna must be protected; only Aldric and future soldier-types trade blows).
- Bond deepens through shared survival and dialogue choices; high Bond unlocks lore, loyalty, personal quests; low Bond → withheld help or departure; betrayal → hostile.
- Loss has three shapes: Captured (recoverable via quest), Departure (low bond), Death (permanent).

#### FR-18: The act-gating main story
Main-story quests gate the three acts and deliver the canonical spine. Realizes UJ-4.
**Consequences (testable):**
- Act 1 (Survive) → Act 2 (Understand) gate: proposed quest **"Follow the Road"** — reach the Copper Road corridor (a Tier 2 push).
- Act 2 → Act 3 (Decide) gate: proposed quest **"The Rescue"** — reach/attempt Aldric's prison; success (rejoins) or failure (lost) both deliver the same beat: Klein now knows the war, and knowing forces the choice.
- Act 3: the choice — home → last provisioning (Tier 3) → Aldric resolution → the closing trap → the NW border crossing.
- **The border crossing is the win:** a final tense run, not a boss; climax is escape, not a duel. Ending locked: he makes it home.
- The epilogue seeds connect to main-story canon (Corneo → Coneros, the Graveyard filling now, the occupation that shapes Milek's world).

#### FR-19: Dialog and quest delivery
Dialog is text-forward (speaker line, numbered choices, bottom log); quests are freeform and source-driven. Realizes UJ-3, UJ-4.
**Consequences (testable):**
- Dialogue suspends the turn loop (safe pause); a node = speaker text + up to N choices; a choice can advance, set a flag, fire an effect (Bond gain/loss, item give/take, disposition), or be gated.
- Primary gate stat: VOICE (talk down a patrol, haggle, calm animals, press for lore); occasional INS gates.
- Quests are NPC-given or discovery-triggered (a Journal Note or item auto-starts); killing a quest-giver voids the quest; the Journal is passive lookup, not a delivery mechanism.
- Four dialog channels: NPC conversation, companion dialogue, quest delivery, found-lore read-text.

### 4.6 Inventory, Currency, and Economy

**Description:** Inventory is a hybrid slot + weight system with gear-with-memory bags; currency is deliberately scarce, with two mobile traders as the only sinks. Realizes UJ-2, UJ-4.

**Functional Requirements:**

#### FR-20: Inventory
Carrying capacity is a real constraint, and better bags extend foray range. Realizes UJ-2.
**Consequences (testable):**
- Quick-Access slots (5× weapon/armor-type + 3× artifact/ring) are always available; main inventory is 19 base slots, expandable by equipping up to 5 storage items (bonuses merge).
- Weight capacity scales with STR (tiers TBD).
- Bags carry their own durability; thematic traps (dart/fire/freeze) can break a bag — 75% of contents drop (recoverable), 25% lost.

#### FR-21: Currency and trade
Currency is scarce; two mobile traders are the economy. Realizes UJ-2, UJ-4.
**Consequences (testable):**
- Currency: Copper → Silver (25:1) → Gold (10:1) → Royal Gold Plaque (1,000:1).
- Sinks: Black Market Trader (coin-only, Gold-tier, guarded) and the Traveling Wanderer (coin or barter, Copper/Silver). No fixed shop.
- Coin is weighted like any other item (carrying currency costs inventory space). *(Open: stack/compression rule.)* *[ASSUMPTION — currency scarcity and the two-trader economy are bible-anchored; the coin-weight rule is open.]*
- Barter keeps non-coin players unblocked; no coin sink is mandatory for survival.

### 4.7 Cross-Cutting NFRs (the SPD-style presentation lock)

**Description:** The SPD-style presentation is a system-wide constraint every feature and the architecture must conform to. It bounds UX, performance, and platform decisions for the whole PRD.

**Non-functional requirements:**
- **Rendering:** 2D top-down tile-based only — no 3D, no camera tricks. Placeholder colors are acceptable pre-art.
- **Interaction:** turn-based, tile-by-tile; no real-time mechanics.
- **Text channel:** the bottom message log is the primary text surface; the HUD is minimal.
- **Platform:** desktop-first (Windows/Linux) via libGDX/LWJGL3 *[ASSUMPTION — engine decision pending Open Item 1]*; the mobile-friendly-HUD lock keeps a future port open.
- **Performance:** the persistent Herois map must render and pathfind within a single turn without perceptible stutter on mid-range desktop hardware *[ASSUMPTION — budget set in architecture]*.

## 5. Non-Goals (Explicit)

- **No floor-descent.** The world's danger is a gradient across a persistent map, not a ladder down.
- **No combat-XP.** Killing things never raises numbers; progression is horizontal.
- **No save-scumming / no difficulty tiers that change the loop's rules.** Permadeath is the loop.
- **No power fantasy.** The protagonist's training is his edge, not his destiny; the war is too big for one knight.
- **No multiplayer, online, or service features** in v1.
- **No "What if…" branch endings in v1** — the Ant and the Buried Truth are architecturally supported but not built now.
- **Region 2 / the Deep Cave Mouth expansion** is deferred (it is a threshold, not the exit).
- **No rigid quest taxonomy** — quests are freeform and source-driven, not a Fetch/Kill framework.

## 6. MVP Scope

### 6.1 In Scope

- The persistent, traversable Herois (hybrid map: fixed canon landmarks + procedural wilderness).
- The three survival acts (Survive → Understand → Decide) gated by main-story quests.
- The survival core (Hunger, Thirst, Temperature, Day/Night, Weather, Debuffs), 11 World-Structure Locations across 3 danger tiers.
- Klein's training expressed mechanically (combat as viable-but-costly); horizontal progression (SKILL, gear-with-memory, knowledge, allies).
- Aldric's opening (diegetic tutorial + capture) and the companion/dialog/quest systems.
- One canonical ending: the NW border crossing and the earned homecoming.
- SPD-style presentation (2D top-down tiles, turn-based, bottom message log, minimal HUD), desktop-first (libGDX/LWJGL3, Windows/Linux) *[ASSUMPTION — engine decision pending Open Item 1]*.

### 6.2 Out of Scope for MVP

- The "What if…" branch endings (the Ant, the Buried Truth) — architecturally supported, not built now.
- Region 2 / the Deep Cave Mouth expansion — deferred to a later region pass.
- Multiplayer, online, or service features.
- Non-roguelike modes (no save-scumming, no difficulty tiers that change the loop's rules).
- *[NOTE FOR PM]* Full companion roster depth (all four companions with personal quests) may need to be trimmed to a canonical subset (Aldric always; Mara, Old Fen, Yenna as reach) if timeline requires — flag for revisit.

## 7. Success Metrics

*Each SM cross-references the FR(s) it validates.*

**Primary**
- **SM-1**: Full canonical run beatable and lands — from intro through the fall of Corneo, the three acts, and the NW border crossing, Klein gets home, earned, story told through the systems; epilogue seeds visibly connected to the main-story canon. Validates FR-18.
- **SM-2**: Survival loop genuinely playable first — you can survive several in-game days in the forest (Phase 0–1 of the build order) before breadth is added. Validates FR-4, FR-5, FR-6, FR-7.

**Secondary**
- **SM-3**: Horizontal progression proven — a player who knows the forest measurably survives longer than one who doesn't, without any XP. Validates FR-11.
- **SM-4**: The east/west pull is felt — the player is genuinely pulled two ways (east for resources, west to escape) and reports the tension in playtesting. Validates FR-9, FR-10, FR-18.

**Counter-metrics (do not optimize)**
- **SM-C1**: Combat win rate — this should *not* be optimized up. Combat is viable-but-costly by design; raising win rate would break the stealth/avoidance balance. Counterbalances SM-3, SM-4.

## 8. Open Questions

*The brief's seven Open Items, resolved with recommendations where the bible supports them.*

1. **Engine/stack decision** — *[RECOMMENDED: KEEP]* Java + libGDX. The bible's own "reuse note" says the current build already has working turn engine, tilemap, FOV, detection, noise, combat, hunger, inventory, save/serialization — Phases 0–2 map directly onto it if the stack is kept. The SPD-style presentation lock leans strongly the same way. **Decision formally rests with the solo dev; confirm in architecture.**

2. **Map structure** — *[RECOMMENDED: HYBRID]* Fixed canon landmarks (Corneo, the Copper Road, the NW border crossing, the Watchtower) + procedural wilderness between them. The bible's own proposal; preserves roguelike variety without breaking canon. Sub-question open: one continuous tiled region vs. connected sub-areas stitched together — flag for architecture.

3. **Border-crossing win mechanic** — *[RECOMMENDED: FINAL TENSE RUN, NOT A BOSS]* The win is crossing the NW border into Novelborne. Act 3's climax is a final tense run (escape, not a duel); the Deep Cave Mouth is a separate threshold (Region-2 hook), NOT the exit. Confirmed by the bible's act structure.

4. **Act-gating quest definitions** — *[RECOMMENDED: LOCK the two proposals]* **"Follow the Road"** (Act 1→2: reach the Copper Road corridor) and **"The Rescue"** (Act 2→3: reach/attempt Aldric's prison; success or failure both turn the act). The bible's proposals are the strongest candidates; lock them in a story pass.

5. **Companion roster & lore threads** — *[RECOMMENDED: LOCK the four]* Aldric (garrison's fall; combat), Mara (war's human cost; caches), Old Fen (the forest; survival mastery), Sister Yenna (the old faiths; the Ant seed). One active companion recommended; whether any companion can truly die is a scope decision. **Companions are full tile-agents (FR-15, FR-16)**: own Status, own HP, autonomous AI — the bible's "follow AI + simple orders" is a follower model and must be upgraded to a full entity model (flag for the architecture and a bible-doc update).

6. **Combat-vs-stealth balance calibration** — *[RECOMMENDED: DESIGN INTENT LOCKED, NUMBERS DEFERRED]* Fighting is mechanically penalized (durability decay, noise→reinforcements, per-act escalation, avoid-only Sense-users, debuff drains) but no damage/HP/accuracy numbers are locked. The calibration is a Phase 2–3 tuning pass, not a PRD decision.

7. **Run length** — *[OPEN — DERIVED]* No explicit number in the bible. The turn economy implies ~3–6 in-game days (a handful of 170-turn cycles) before the survival clocks force death or a border-crossing attempt (Thirst-to-death ≈ 530 turns ≈ 3 cycles; starvation ≈ 650 ≈ 3.8; one Night = 70 turns). This derived range is a starting calibration target, not a commitment. **Real hours: a full canonical run is targeted at roughly 4–8 hours of play** *[ASSUMPTION — calibrate in the Phase 2–3 playability pass]*, per the brief's "survival loop genuinely playable first" framing and the horizontal-progression design (growth by knowledge, not grind).

**Additional open items surfaced during extraction (for the story/architecture passes; FR-IDs link to where the question lives):**
- Rescue-Aldric outcome: always rescuable / always lost / player-determined. (FR-3, FR-18)
- Escalation trigger mechanics: story flags vs. occupation-timer vs. exploration-driven. (FR-12, FR-18)
- Whether the home Klein returns to is untouched (ending texture). (FR-18)
- Whether the player learns Magdalene/parents' fate en route, or they remain a haunting unknown until the end. (FR-18)
- Does a failed Last Stand roll also consume the once-per-run save? (FR-14)
- Can a companion truly die, or only be captured / leave? (FR-15)
- Coin stack/compression rule (100 Copper = 1 weight, or 1:1 physical?). (FR-21)
- Whether Copper Road towns (Westwatch, Tradewick, Mildtown, Silverkeep) are visitable locations or off-map lore.

## 9. Assumptions Index

*Every `[ASSUMPTION]` and `[RECOMMENDED]` from the document, surfaced for explicit confirmation. `[RECOMMENDED]` items are the §8 Open-Item resolutions — they read as assumptions pending the solo dev's confirmation.*

- §1/§4.7/§6.1 — Desktop-first (libGDX/LWJGL3, Windows/Linux); mobile-friendly-HUD presentation lock keeps a future port open. Inline tag: `[ASSUMPTION — engine decision pending Open Item 1]`. (Carried from the brief.)
- §4.7 — Performance budget for the persistent map (render + pathfind within one turn, mid-range desktop). Inline tag: `[ASSUMPTION — budget set in architecture]`.
- §8/Q7 — Full canonical run targeted at ~4–8 real hours. Inline tag: `[ASSUMPTION — calibrate in the Phase 2–3 playability pass]`.
- §2.2 — Secondary player is secondary but designed-for. Inline tag: `[RECOMMENDED: treat as secondary but design for; survival loop must stay playable-first per SM-2]`.
- §2.4 — The four UJ narratives are captured from the bible's design beats, not from lived playtests; validate in review. Inline tag: `[ASSUMPTION — UJ narratives are bible-derived, not playtest-derived; validate in review]`.
- §3 — The Glossary is bible-anchored; terms are fixed for downstream workflows. Inline tag: `[ASSUMPTION — Glossary is bible-anchored; terms fixed for downstream workflows]`.
- §4.2/FR-5 — Weather weights (Clear 40 / Rain 25 / Fog 20 / Storm 10 / Cold Snap 5) and day/night lengths (100/70) are bible values, carried as the starting calibration. Inline tag: `[ASSUMPTION — weather weights and day/night lengths are bible values]`.
- §4.4/FR-12 — Combat action set (Attack/Block/Dodge/Use Item/Flee) and turn-order-by-AG are bible-anchored; tie-break TBD. Inline tag: `[ASSUMPTION — combat action set and turn-order-by-AG are bible-anchored]`.
- §4.6/FR-21 — Currency scarcity and the two-trader economy are bible-anchored; the coin-weight rule is open. Inline tag: `[ASSUMPTION — currency scarcity and the two-trader economy are bible-anchored]`.
- §8/Q1 — Engine decision leans KEEP (Java + libGDX) pending solo-dev confirmation in architecture.
- §8/Q2 — Map structure leans HYBRID (fixed canon + procedural wilderness); continuous-vs-sub-areas flag for architecture.
- §8/Q3 — Border-crossing win is a final tense run, not a boss; Deep Cave Mouth is NOT the exit.
- §8/Q4 — Act-gating quests lock the bible's two proposals ("Follow the Road", "The Rescue").
- §8/Q5 — Companion roster locks the four; one active companion recommended.
- §8/Q6 — Combat-vs-stealth numbers are a later tuning pass, not a PRD decision.
- §8/Q7 — Run length ~3–6 in-game days is a derived starting target, not a commitment.

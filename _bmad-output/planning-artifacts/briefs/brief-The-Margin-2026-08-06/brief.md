---
title: The Margin
status: final
created: 2026-08-06
updated: 2026-08-06
tags: [the-margin, product-brief, remake, roguelike, survival]
---

# Product Brief: The Margin

## Executive Summary

**The Margin** is a 2D top-down, turn-based survival roguelike set in Herois — the westernmost, poorest, forested frontier of the Kingdom of Evermove — about 50–60 years before the main story. You play **Klein**, a young noble-born knight of Novelborne on his first, routine posting to the border town of Corneo. On day one, Evermove invades, the garrison dissolves, and he finds himself alone and hunted in a pine-and-fog forest. His goal is the smallest and most human one imaginable in a world of god-kings and 174-year immortals: **get home** — across the northwest border, to his fiancée Magdalene and his aging parents.

The game pairs a **Shattered Pixel Dungeon–style presentation** (tile-based, turn-based, text-forward bottom log, permadeath) with a **survival-game skeleton** (hunger, thirst, temperature, day/night, weather) and a **storied spine** (text intro, act-gated narrative, companions). It deliberately keeps SPD's *skin* while swapping its *skeleton*: no floor-descent, no combat-XP — instead a persistent, traversable Herois where the danger rises east toward the occupiers and safety lies west across the border. Progression is horizontal: you grow by *knowing the forest*, not by inflating numbers.

**Why now:** the design bible is fully written (26 docs: world, protagonist, story spine, and 17 interlocking systems), and the kept headless (logic-only) core already proves out the turn engine, FOV, detection, noise, combat, hunger, inventory, and save/serialization. The vertical-slice playable screen is built and runs. The Margin is ready to move from design to build — this brief is the first artifact of that build phase.

## The Problem

Survival roguelikes and narrative games rarely coexist, and each existing option leaves a gap:

- **Pure survival roguelikes** (SPD and its family) are mechanically deep but narratively thin — they loop *descend-and-fight* with no reason beyond the next floor.
- **Story-driven games** are rarely survival games — when you do die, you lose narrative momentum, not just a run.
- **The protagonist is usually the problem.** Most roguelikes default to "a hero clearing dungeons" — a power fantasy that leaves no room for a *small, human* goal like going home, and forces violence as the answer.

The status quo means the player must choose: mechanical depth *or* a story that means something. The Margin wants both, in one life.

## The Solution

The Margin is one continuous, permadeath life across a persistent Herois. Four nested loops carry the experience:

- **The Turn** — tile-by-tile movement; each turn you weigh *move · scavenge · eat · craft · fight · hide · rest* while hunger, thirst, cold, the clock, and nearby threats all tick.
- **The Foray** — leave a safe point, travel to a world-structure location (one of 11 across 3 danger tiers), scavenge under its hazard, haul loot back before night or weather turns against you.
- **The Day** — day/night cycle plus rolled weather; forage by day, shelter and cook by night.
- **The Act** — main-story quests gate the three acts (Survive → Understand → Decide), tightening the occupation and pushing Klein toward home.

**The tension is spatial.** The northwest border — and home — is only a few miles of forest away. The invasion comes down the Copper Road from the east. Klein is pulled two ways every day: **east toward danger** for the resources survival demands, **west toward the border** to escape. Home is close enough to taste and lethal to reach.

**The story is earned, not told.** A skippable text intro covers the fall of Corneo; gameplay opens mid-flight with **Aldric**, a fellow knight who teaches you how to play diegetically, then is captured the moment you've learned the ropes — the first loss, and the seed of a rescue quest. Companions, dialogue, and quests layer in from there, all serving the canonical spine: get home.

## What Makes This Different

- **A knight who can fight, in a war too big for fighting.** Combat is real and winnable but *costs* — HP, weapon durability, noise that draws reinforcements, and occupation escalation. Avoidance, stealth, and preparation usually beat a straight fight. The protagonist's training is his edge; it is not destiny.
- **No floor-descent, no combat-XP.** The world's danger is a *gradient across a map*, not a ladder down. You grow by SKILL, gear-with-memory (repairs lower its max, so gear is precious), knowledge of the forest, and allies — never by kill count.
- **An authored canonical spine with built-in "What if…" branches.** One true ending (get home) is built first and deep; the two unchosen purposes (become the Ant — awakened Sense power; chase the Buried Truth — the king's divinity) are architecturally supported as later player-decided alternate endings. *Author for the canon, architect for the branch.*
- **The prequel hook.** Every survival choice happens in the world that becomes the main story — Corneo becomes Coneros, the Mercenary Graveyard is filling now, the occupation that shapes Milek's world begins here.
- **Survival is choice, not chores.** Every hunger/thirst/cold decision is a gamble under scarcity — a campfire gives warmth, light, cooking, and clean water but is visible and audible to patrols.

## Audience & Stakeholders

**Primary player:** the roguelike-and-survival player who's played the genre's mechanics to death and wants the loop to mean something — "survive, go home, marry the girl, live." They already understand permadeath, FOV, and turn economies; they want the *why* the genre usually lacks.

**Secondary player:** the narrative-first player who's been burned by story games where death means losing the story. Here death is the run, and the run is a life.

**The creator is a solo developer** building this as a passion project and prequel to an existing novel universe (The Margins). Success is a game that is *theirs* — not a reskin of SPD — and that honors the source canon.

## Success Criteria

- **The full canonical run is beatable and lands:** from the text intro through the fall of Corneo, the three acts, and the northwest-border crossing, Klein gets home — earned — with the story told through the systems, not around them, and the epilogue seeds visibly connected to the main-story canon.
- **The survival loop is genuinely playable first:** you can survive several in-game days in the forest (Phase 0–1 of the build order) before breadth is added.

## Scope

**In (first version — the canonical spine):**
- Persistent, traversable Herois (hybrid map: fixed canon landmarks + procedural wilderness) — *proposal, see Open Items*
- The three survival acts (Survive → Understand → Decide) gated by main-story quests
- The survival core (hunger, thirst, temperature, day/night, weather, debuffs), 11 world-structure locations across 3 danger tiers
- Klein's training expressed mechanically (combat as viable-but-costly); horizontal progression (SKILL, gear-with-memory, knowledge, allies)
- Aldric's opening (diegetic tutorial + capture) and the companion/dialog/quest systems
- One canonical ending: the northwest-border crossing and the earned homecoming
- SPD-style presentation (2D top-down tiles, turn-based, bottom message log, minimal HUD)
- Desktop-first (libGDX/LWJGL3, Windows/Linux); the mobile-friendly-HUD presentation lock keeps a future port open *[ASSUMPTION — engine decision pending Open Item 1]*

**Out (later or never):**
- The "What if…" branch endings (the Ant, the Buried Truth) — architecturally supported, not built now
- Region 2 / the Deep Cave Mouth expansion
- Multiplayer, online, or service features
- Non-roguelike modes (no save-scumming, no difficulty tiers that change the loop's rules)

## Vision

If The Margin succeeds, it becomes a complete prequel to The Margins — a game that makes the novel's world *felt* rather than read. The canonical GET HOME spine ships first and fully; the "What if…" branches then turn each replay into a different answer to a question the canon deliberately leaves open: *what else could this war have made of Klein?* The architecture (one spine, branching later, systems-first build) means depth is layered on — Region 2 through the Deep Cave Mouth, more companions, more of the buried truth — without ever breaking the canonical story or the core survival loop. In 2–3 years, The Margin stands alone as a complete game *and* deepens the world it is a prequel to.

## Open Items (carried from the bible — to resolve in PRD/architecture)

1. **Engine/stack decision** — keep Java + libGDX (reusing the proven turn/FOV/detection/save scaffolding) or rebuild. *The SPD-style presentation lock and the working playable screen lean strongly toward keeping it.*
2. **Map structure** — one continuous tiled region vs. connected sub-areas; hybrid generation rules.
3. **Border-crossing win mechanic** — reach a tile, survive a final gauntlet, or a scripted crossing.
4. **Act-gating quest definitions** — the specific main-story quests that trigger Act 1→2→3.
5. **Companion roster & their lore threads** — who exactly, and which threads each carries.
6. **Combat-vs-stealth balance calibration** — how punishing is fighting, exactly.
7. **Run length** — how many in-game days / real hours a canonical run should take.

---
tags: [the-margin, game-design, systems, companions]
status: draft
---

# Companion System

Related: [[NPC System]] · [[NPC Roster]] · [[Protagonist - Klein]] · [[Dialog System]] · [[Quest System]] · [[Gameplay Roadmap]] · [[Status System]] · [[Combat System]] · [[Lore System - Storyline Roadmap]]

## Overview
Companions are the **specialized NPCs** (Role 5 in [[NPC System]]) who travel with Klein. Each is **help *and* burden**: a capability he lacks, a voice on the road, a bond that deepens — and a mouth to feed, a body to protect, a loss that lands. In a one-life game, a companion's death or capture is **permanent and heavy.** [[NPC Roster|Aldric]] — the tutorial guide, taken in the opening — is the template for all of it.

---

## Core Pillar — Help AND Liability

The design rule that keeps companions from being a free power-up:

> **A companion is never free.** They add a real capability, but they *cost* you — more food foraged, more noise (harder to hide), a second body to keep alive, sometimes a slower pace. The player is always weighing *is the help worth the burden?*

This makes taking (and keeping) a companion a **survival decision**, not a reward. It also means *losing* one is a mechanical relief and an emotional blow at the same time — exactly the knife you want.

---

## Party Size

- **Recommended: one active traveling companion at a time.** Intimate, thematically true ("two people surviving the forest together"), and it keeps the liability legible. Klein is a fugitive on the run, not a warband leader.
- **Open alternative:** allow up to **2**, and/or a **holdout** (a refugee camp / safe node) where recruited-but-inactive companions wait and provide *passive* support (supply drops, intel) rather than traveling. Flagged as a decision below.

---

## Recruiting

Companions are drawn from the **Neutral pool** ([[NPC Roster]]) — trust is *earned*, not clicked:

- Met as neutral NPCs first; recruited through **[[Dialog System|dialogue]]**, a **VOICE** check ([[Status System]]), and/or **completing a quest** for them.
- Each has **conditions**: Aldric is already with you; Mara won't leave until her family's safe; Old Fen tests you; Sister Yenna trusts *slowly* and only once she reads Klein as no threat to the underground.
- Recruitment is a [[Quest System]] beat as often as a conversation.

---

## What a Companion Brings

Each active companion grants a **capability Klein lacks** while traveling with him (from [[NPC Roster]]):

| Companion | Capability (while active) | Lore thread |
|---|---|---|
| **Aldric** — fellow knight | A second blade — fights, draws aggro, bolsters morale | The garrison's fall; fight-vs-flee |
| **Mara** — refugee mother | Supply sense — town caches, shelter, better forage/ration stretch | The war's human cost; Coneros's memory |
| **Old Fen** — hermit | Survival mastery — safe routes, higher forage yield, weather reads, danger-sense | The forest itself; pre-war Herois |
| **Sister Yenna** — heretic | The hidden — old-lore, cures, secret paths, the **Mystic/Sense** thread | The old faiths; the "Ant" What-if |

**Combat rule (from the old design's good instinct):** the **non-combat companions do NOT fight.** Mara, Old Fen, and Yenna help *other* ways and must be protected — forcing them into a brawl is a failure state, not a tactic. Only Aldric (and future soldier-types) trade blows.

---

## The Bond

A per-companion relationship value that deepens through **shared survival, dialogue choices, and honoring what they want.**

- **Rising Bond unlocks:** deeper [[Dialog System|dialogue]] and lore, loyalty (they won't leave under pressure), stronger help, a **personal quest**, and — for Yenna — the seed of the **"Ant" What-if** branch.
- **Falling/broken Bond:** they grow distant, withhold help, or **leave** (low bond), or turn hostile (if Klein betrays them).
- Bond is how companions tie the three narrative systems together: [[NPC System|who they are]] → [[Dialog System|how you talk]] → [[Lore System - Storyline Roadmap|the story they carry]].

*(Reuse note: the current libGDX build already has a **CompanionSystem** with follow AI, a distraction action, and a Bond value — real scaffolding this design maps onto if the stack is kept.)*

---

## Needs & Liability *(the cost side)*

A traveling companion **shares the survival pressure**:

- **Food:** a second mouth — foraging has to feed two (or you ration and Bond suffers).
- **Noise:** two people are louder — harder to sneak past the Gilimans/detection.
- **Protection:** they can be **wounded**; a non-combatant caught in a fight is on you.
- **Pace:** some (Mara with a family, an injured ally) may slow travel.

The payoff must beat the cost — that's the whole tension. A companion you can't feed is a companion you have to leave.

---

## Loss — Death, Capture, Departure

Per Klein's **permadeath-reframed** principle, "losing" a companion has *three* shapes, not just death:

- **Captured** — taken by the enemy, **recoverable via a quest.** *Aldric's opening capture is the template* — lost early, gettable back later. The kindest loss.
- **Departure** — low Bond, or their goal diverges from Klein's; they walk away (maybe recruitable again).
- **Death** — permanent, heavy. Whether *any* companion can truly die (vs. only be captured/leave) is a scope decision below — but if it's on the table, it should be rare and devastating, matching the game's treatment of true death.

Every loss strips a capability *and* a voice — which is why the survival math and the story both feel it.

---

## Field Behavior *(mechanics)*

- **Follow AI:** the active companion moves with Klein tile-by-tile (scaffolding exists).
- **Simple orders:** hide / hold / distract (the **distraction** action already exists — draw a patrol so Klein slips past). Non-combatants default to *hide/flee*.
- **Combatants** (Aldric) engage per [[Combat System]]; **non-combatants** never do — they take cover and you defend them.
- Companions obey the same detection/noise rules — they can *blow* your stealth as easily as help it.

---

## The Roster (this game)

The four candidates, with recruitment and fate hooks:

### Aldric — first companion *(LOCKED opening)*
Already at Klein's side in the opening flight; the **diegetic tutorial guide**; **captured** the moment the how-to-play ends → the game's first loss and the seed of the **rescue-Aldric quest**. If rescued, rejoins as your combat companion — carrying the fight-vs-flee tension.

### Mara — the refugee mother
Recruited by getting her family to safety (a quest). Non-combatant; supply/shelter capability; her Bond puts the war's human cost in Klein's hands.

### Old Fen — the hermit
Recruited by proving himself in the woods (a test/quest). Non-combatant; survival-mastery capability; the mentor who makes the forest legible.

### Sister Yenna — the heretic
Recruited slowly, once trust is earned; guards the old faiths. Non-combatant; the **Mystic/Sense** thread — her deep Bond is the doorway to the "Ant" What-if ending.

---

## Open Items
- [ ] **Party size:** lock 1 active, or allow 2 / a holdout for passive support?
- [ ] Can a companion **truly die**, or only be captured / leave? (permadeath-reframed scope)
- [ ] Companion **needs model** — do they consume Klein's food/water, and how much?
- [ ] **Bond** numeric model — thresholds, what raises/lowers it, decay
- [ ] Per-companion **recruitment conditions** and personal quests
- [ ] **Rescue-Aldric quest** placement (Act 1 / Act 2 / spanning)
- [ ] Approve / rename the roster (Aldric, Mara, Old Fen, Yenna)
- [ ] Does Yenna's Bond literally gate the "Ant" awakening, or just foreshadow it?

## Changelog
- Established the Companion System: companions as **help + liability**, drawn from the [[NPC Roster]] Neutral pool
- Recommended **one active traveling companion**; flagged 2 / holdout as an open alternative
- Locked the combat rule: **non-combatants don't fight** (must be protected) — only soldier-types like Aldric trade blows
- Defined the **Bond** (deepens via shared survival + dialogue; gates lore, loyalty, personal quests, and Yenna→"Ant")
- Defined **loss** as capture / departure / death (permadeath-reframed), with Aldric's capture as the template
- Noted reuse of the existing libGDX CompanionSystem (follow, distraction, Bond)
- Seated the four candidates with recruitment + fate hooks

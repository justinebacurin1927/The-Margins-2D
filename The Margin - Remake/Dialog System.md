---
tags: [the-margin, game-design, systems, dialog]
status: draft
---

# Dialog System

Related: [[NPC System]] · [[Companion System]] · [[Quest System]] · [[Lore System - Storyline Roadmap]] · [[Protagonist - Klein]] · [[Status System]] · [[evermove]]

## Overview
The **delivery pipe** for everything narrative — NPC talk, companion bonds, quest offers, and found lore. Text-forward (SPD-style), branching, and gated by Klein's **VOICE**. On its own it's plumbing; its whole value is that [[Lore System - Storyline Roadmap|Lore]], [[Companion System|Companions]], and [[Quest System|Quests]] *all happen through it.*

---

## Presentation

- SPD-style: a **dialogue panel** over the tiled world — speaker line on top, **numbered choices** below; the **bottom message log** carries shorter beats and companion barks.
- **The turn loop suspends** while a conversation is open — no survival tick, no enemy phase. A conversation is a safe pause. *(Existing behavior in the current build.)*
- Minimal chrome; the words do the work.

---

## Structure — Nodes & Choices

Branching dialogue trees (the existing `DialogNode` / `DialogController` scaffolding):

- A **node** = speaker text + up to N choices; choosing advances to the next node or ends the scene.
- A **choice** can: advance, **set a flag**, fire an **effect** (start/advance a quest, adjust **Bond**, give/take an item, shift **disposition**), or be **gated**.
- Terminal nodes (no choices) close on confirm.

---

## VOICE Gates *(Klein's social stat)*

The old build gated choices on INSTINCT; **for Klein the gate stat is [[Status System|VOICE]]** (persuasion). Swap the stat, keep the machinery.

- **Gated choices** — talk down a wary patrol, haggle a better price, calm a spooked animal, press an NPC for deeper lore, de-escalate a fight. Flagged in the UI (the old `[INSTINCT]` tag → **`[VOICE]`**).
- **Below threshold:** the option is either closed, or **attempted-and-can-fail** (a risk — see Open Items).
- Occasionally **other stats gate** — e.g. **INS** to *read* a bluff or notice a lie (open question).

---

## The Four Channels *(what dialogue delivers)*

1. **NPC conversation** — traders (trade + lore), neutrals (info, quests, recruitment), and **hostiles you talk down** (VOICE de-escalation). Availability is shaped by the NPC's **disposition** ([[NPC System]]).
2. **Companion dialogue** — recruitment, **road banter** (barks), and **deep conversations that build [[Companion System|Bond]]** and trigger personal quests. Choices raise or lower Bond.
3. **Quest delivery** — NPC-given quests are **offered and updated in dialogue**; the **Journal/Log** tracks them ([[Quest System]]). Killing a giver voids the quest.
4. **Found lore (read-text)** — journals, notes, grave markers: **speakerless text the player reads.** The backbone of environmental storytelling and discovery-triggered quests.

---

## Disposition & Reputation in Dialogue

An NPC's standing ([[NPC System]] disposition ladder) decides what dialogue even exists:

- **Hostile** → no talk (unless a VOICE de-escalation is offered).
- **Wary** → guarded, a VOICE check to open up.
- **Neutral/Friendly** → info, trade, quests.
- **Ally** → the deepest lines, personal quests, loyalty.

Choices **shift disposition, reputation, and Bond** — dialogue is how you *move* an NPC along that ladder (the Wanderer's temporary lockout / the Black Market's permanent break are the extremes).

---

## The Literacy Hook *(optional — canon-grounded)*

Klein is noble-born and literate, and canon notes the **spoken** Novelborne and Evermove tongues are close (he can *talk* to almost anyone). But the **script — Ambrosian Imotte, the vine-like writing — he may not read fluently.**

- **Optional gate:** found **occupier documents** (orders, ledgers, notes) are in Imotte and are **partly unreadable** until Klein learns the script or a companion translates (**Sister Yenna** / a Heretic reads the old vine-script).
- This reintroduces the old "can't read the script" lore-gate for a **fresh reason** (the *enemy's* writing), distinct from Milek's illiteracy — and gives Yenna a concrete in-field use.
- Flagged optional; include only if it earns its weight.

---

## Companion Banter *(life on the road)*

- **Barks:** short contextual lines in the bottom log — react to weather, a nearby danger, a location, low food, a near-miss. Cheap texture that makes a companion feel *alive*.
- **Set-piece conversations:** longer talks at rest/camp that **raise Bond** and drip the companion's lore thread. This is where Aldric argues fight-vs-flee, Mara grieves Coneros, Yenna hints at the old faiths.

---

## Integration Map *(why this system exists)*

| Feeds | How |
|---|---|
| [[NPC System]] | Disposition gates availability; VOICE de-escalation; reputation shifts |
| [[Companion System]] | Recruitment, Bond ↕, banter, personal quests |
| [[Quest System]] | NPC-given offer/track; discovery quests via found read-text |
| [[Lore System - Storyline Roadmap]] | Main-story beats and **act-gating quests** are delivered here |

---

## Open Items
- [ ] Confirm **VOICE** as the primary gate stat (replacing INSTINCT); decide if **INS** gates "read the bluff / spot the lie" choices
- [ ] Failed VOICE checks: **hard-closed** options, or **attempt-and-fail with consequences** (anger, alarm, blown stealth)?
- [ ] The **Imotte literacy gate** — in or out? If in, how is it learned / translated?
- [ ] **Banter** scope — authored barks vs. templated; how many, how often
- [ ] How much true **branching** the main story needs vs. simple info dialogue
- [ ] **Journal/Log UI** — shared surface with [[Quest System]]
- [ ] Does dialogue ever run *without* pausing the turn loop (e.g. a bark mid-flight), or always pause?

## Changelog
- Established the Dialog System as the delivery pipe for NPC talk, companion Bond, quests, and found lore
- Defined **four channels**: NPC conversation, companion dialogue, quest delivery, found read-text
- Swapped the choice-gate stat to **VOICE** (Klein's persuasion) from the old INSTINCT gate; noted reuse of the existing `DialogNode` / `DialogController` scaffolding (branching, gates, turn-loop suspension)
- Tied dialogue availability to NPC **disposition**, and choices to disposition/reputation/Bond shifts
- Added the optional **Imotte literacy hook** (occupier-script gate, canon-grounded; gives Yenna a use)
- Specced companion **banter** (barks + camp set-pieces) as the Bond/lore surface

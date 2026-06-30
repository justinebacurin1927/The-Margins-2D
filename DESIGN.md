# THE MARGINS: Design Doc (Working Title)

**Genre:** Roguelike dungeon-crawler (Shattered Pixel Dungeon-inspired) with persistent narrative
**Starting point:** Campaign begins after Ch. 25 ("Blackberry Troupe") — original continuation, not a retelling
**Player Character:** Milek of Coneros

---

## 1. Core Concept

SPD's loop is: descend procedurally generated floors, manage limited resources (HP, food, identify-by-use items, gold), fight enemies with permadeath stakes, find a boss, descend further. The Margins gives you a built-in reason that loop matters: **Milek isn't dungeon-diving for treasure. He's searching for people.**

Every "dungeon" in this game is a real place from the world — a Lamilla supply route, the Tradewick auction undercroft, a Wallington mountain pass, eventually Dawnbury itself — re-skinned as a procedural floor-set. The "boss" of each isn't a monster for its own sake; it's the obstacle standing between Milek and the next piece of his actual goal: **find Galleon and the Pack. Find Henry. Find out what happened to Erik. Survive long enough to matter.**

This gives you SPD's mechanical bones with a reason to keep playing past "fun number go up."

---

## 2. Core Pillars

1. **Scarcity is character, not just difficulty.** Milek survived on stolen rations and bitter mushrooms. Hunger, limited inventory slots, and identify-by-use items aren't just SPD systems — they're the same scarcity logic from the books. A potion you don't recognize is a barrel you're testing with a torch.
2. **No wasted violence.** Milek is eight years old and the book is explicit that every fight costs him something. Combat should feel earned and slightly desperate, not power-fantasy. Numbers stay low; tactics matter more than stats.
3. **Companions are leverage, not stat sticks.** Galleon, the Pack, Pietro, Maru — when present, they change *how* you can solve a floor (Galleon = loud distraction options, Perfect = trap/puzzle insight, Charcoal = squeeze through gaps) rather than just adding DPS.
4. **Permadeath is reframed.** Milek "dying" doesn't always mean game over — sometimes it means capture, separation, or a worse outcome (a child sold, a companion lost) that the run has to recover from. True death is rare and narratively heavy, the way the book treats it.

---

## 3. Meta-structure: "Routes" instead of "The Dungeon"

Each Route = a self-contained location with 3–5 procedural floors + one fixed narrative floor (boss/story beat). Routes unlock based on story progress, not raw floor depth like vanilla SPD.

| Route | Source Material | Tone |
|---|---|---|
| **The Caravan Road** | aftermath of Ch. 25 — tracking Swan's remaining convoys before Theodore's troupe fully absorbs them | Tutorial route. Stealth-heavy, low combat, teaches identify-by-use and hunger systems |
| **Dawnbury Undercity** | the political war Theodore engineered | Faction-based floors — Western nobles vs. Eastern faction vs. neutral civilians caught between |
| **The Blackberry Troupe's Ledger** | Theodore's guild, its "objectives" | Investigation route — learn what the Troupe actually does, moral ambiguity about Theodore |
| **Mirko Pass, Reprise** | the mountain pass, now permanently changed by what happened there | Optional/hard route — Spearshot's fate, the wolf-transformation aftermath |
| **Return to Coneros** | Galleon's promise to bring the Pack home | Endgame route — full circle, Randy confrontation, Erik reunion |

---

## 4. Character System

### Milek — Base Stats (low-number SPD-style, not D&D-style)
- **STR** — affects what he can carry, drag, or force open (doors, supply crates, downed knights' gear)
- **INSTINCT** — his actual stat-in-fiction. Governs perception (trap/ambush detection, reading patrol patterns), crit chance on sneak attacks, and dialogue checks. This is Milek's signature stat — the book repeatedly shows him out-thinking adults, not out-fighting them.
- **GRIT** — endurance/HP scaling, resistance to fear/freeze effects (notably: immune to Porter's Fear Freeze by late game, as a narrative unlock, not a stat grind)
- **VOICE** — persuasion/command, unlocked properly once he starts directing companions tactically (post Ch. 9's "Plan" sequence as precedent)

### Identify-by-Use, reframed
Instead of unidentified "Potion of X," items are **unlabeled supplies** — the same way Milek tested barrels for flammability in Ch. 13. A waterskin might be poisoned. A wrapped bundle might be medicine or might be spoiled. This keeps SPD's tension but justifies it diegetically: Milek doesn't read Novelborne script yet (his literacy arc with Pietro is a real unlockable skill tree branch).

### Companion Slots
Max 2 active companions per Route (mirrors the book — Milek rarely operates with a full crowd in tense moments). Bench companions still grant passive camp bonuses (Helli-style supply caches, Perfect's intel on upcoming floor layouts).

- **Galleon** — loud, tanky, draws aggro; his post-transformation arc could be a *risk/reward* mechanic: triggering "Alpha" form is powerful but burns a resource (his "life force," per Theodore's warning) and requires Claire-type support to safely end it.
- **Pietro** — stealth/bait specialist, ranged.
- **Maru** — once recovered, support/medic.
- **Perfect, Rooster, Charcoal** — non-combat utility companions (trap disarm, counting/intel, squeezing into vents) — they explicitly should NOT fight, per Milek's own rule in the books. This is a good design constraint, not a limitation.

---

## 5. Sample Floor Concept (for "The Caravan Road" Route, Floor 1)

**Setting:** Remnant of Swan's convoy camp, now picked over, three days after the mountain pass.
**Objective:** Find any surviving children/intel before scavengers (human or otherwise) do.
**Mechanical teach:** Hunger system, identify-by-use (test supply crates), basic stealth (avoid Spearshot's old mercenary contacts patrolling for stragglers).

**Sample barks (ambient/companion dialogue, not branching yet):**

> **Galleon** (if in party, approaching a locked supply crate): "Want me to be loud about this, or should we be smart about it?"
> **Milek** (player choice): ["Smart." / "Loud — draw them off."]

> **Perfect** (on entering a trapped corridor): "Three pressure points. I count three. You see two."

> **Milek** (internal monologue on finding an empty cell, SPD-style flavor text instead of generic loot description): *Another cage. Empty. He didn't let himself wonder how long ago.*

---

## Decisions Locked In

**1. Theodore — Trust Meter system.** Mechanically: a hidden-ish "Troupe Trust" stat that shifts based on dialogue choices and how Milek handles morally grey orders from the Troupe (e.g., does he report everything he sees, or hold things back for himself/his people). High trust unlocks Troupe resources and Theodore's rarer appearances with real answers. Low trust doesn't make him an enemy — it makes him *withholding*, which fits the book's Theodore perfectly. He never confirms or denies his real agenda either way.

**2. Win-state — proposed, tell me if you want changes:**
The campaign arc should mirror the book's actual emotional throughline: Milek keeps accumulating people he's responsible for and keeps almost losing them. So the win-state isn't "defeat the final boss," it's **"Return to Coneros with everyone you chose to keep."** Structurally:
- Mid-campaign: Dawnbury resolves, Galleon's promise comes due
- Late campaign: a forced choice point — Theodore offers Milek a permanent place in the Troupe (power, but it means leaving Erik/Porter's unit behind) vs. returning to Porter and Erik (family, but a harder, slower road) vs. a third "go it alone with the Pack" route that's hardest mechanically but is the most "Milek" ending
- True ending = how many of your companions survive AND which path you chose. SPD-style multiple endings based on run performance, not just a binary good/bad.

**3. Permadeath is real for Milek**, but "death" triggers a **Last Stand** mechanic first (one chance, low HP, desperate — mirrors the wolf-transformation save in Ch. 24) before true permadeath. This keeps tension without making an 8-year-old protagonist's death feel cheap or random.

---

# Floor Dialogue Tree: "The Caravan Road" — Opening Quest

**Quest title:** *Five Nights, Again*
**Trigger:** Campaign start, Milek alone, days after the mountain pass.

### Opening (no choice, sets tone)
> *The camp smells like cold ash and someone else's leftovers. Milek crouches at the treeline and counts.*
> **Milek (internal):** *Four tents standing. Three burned. Whoever picked this place clean didn't finish the job.*

### First NPC Encounter — a scavenger (non-hostile, info-gate)
> **Scavenger:** "You're small to be out here alone. Lamilla left this place to rot three days back. Nothing left but rats and regret."
> **[Milek dialogue options:]**
> A. "Did you see children come through here?" *(direct — costs nothing, gates basic intel)*
> B. "What's worth taking?" *(INSTINCT check — success reveals a hidden cache the scavenger doesn't know about)*
> C. *(Say nothing, search the camp yourself)* *(no info from NPC, but avoids tipping anyone off you're looking for someone — relevant later if Trust/Reputation systems track who knows what)*

> *If A:* **Scavenger:** "Children?" *(laughs, not unkindly)* "Boy, everyone who comes through this corridor is somebody's children. You'll have to be more specific, or luckier."

> *If B succeeds:* **Milek (internal):** *The ash pile by the southeast tent isn't sitting right. Something's under it.* [Unlocks hidden supply cache, no combat]

This establishes the pattern I'd use for the whole campaign: **Milek's choices are almost never "good vs evil," they're "direct vs careful vs alone"** — matching his actual characterization (flat, observational, rarely impulsive except when Erik's involved).

---

## Quick Item List — Identify-by-Use, Route 1

| Unidentified Name | True Identity (random per seed) |
|---|---|
| Wrapped Bundle | Stale bread (food) / Spoiled meat (causes Hunger penalty + minor HP loss) |
| Sealed Waterskin | Clean water / Tainted water (Weaken status) |
| Small Tin | Feverwort paste (cures Bleed) / Rendered fat (minor HP regen over time) |
| Folded Cloth | Bandages / Old rags (no effect, just inventory weight) |
| Sealed Letter | Sells for gold (illiterate Milek can't read it yet — literacy unlock changes this item class entirely later) |

---

## Galleon Recruitment Scene — "The Pack Comes Home"

**Trigger:** End of Caravan Road Route (Floor 4/Story Floor), after Dawnbury's political resolution per Theodore's promise. This is the *reunion*, not a random companion pickup — it should land as a story beat, not a menu screen.

### Setup
> *Milek hears him before he sees him. That's new — Galleon was never quiet a day in his life, but something in the rhythm of his footsteps has changed. Heavier. Like he's carrying more than his own weight now, even when he isn't.*

### The Approach (no combat, full dialogue scene)
> **Galleon:** *(stops mid-stride, stares)* "...Milek?"
> **Milek:** "You're alive."
> **Galleon:** "You say that like it was in question."
> **Milek:** "It was."

**[Player dialogue choice — this is a tone-setter for the whole companion relationship, not just flavor]**

A. *"I looked for you in every line. Every camp."* (Honest — raises Galleon's personal Bond stat fast, costs nothing mechanically but locks a warmer dialogue tree for the rest of the campaign)

B. *"Are the others alive."* (Direct, Milek's flattest default — doesn't raise or lower Bond, but unlocks the Pack status info immediately without preamble)

C. *(Say nothing. Just look at him.)* (INSTINCT-coded silence — Galleon fills the gap himself, slightly different info reveal, feels truest to book-Milek)

> *If C:* **Galleon:** *(soft laugh, not the big one)* "Yeah. I know. Me too." *(beat)* "They're alive, Milek. All of them. Perfect's leg healed clean. Rooster hasn't shut up about throwing rocks at a guy with a glowing arm." *(quieter)* "I turned into something in that pass. I don't — I don't fully have the words for it yet. But I came back. That's what matters."

### Recruitment Mechanic
Galleon joins as a **bondable companion** — not unlocked by a fetch quest, but by this conversation. His **Alpha Wolf transformation** becomes a late-game ability unlocked at high Bond + a specific narrative trigger (a true Last Stand moment for an ally, not for Milek himself — mirroring the book exactly, where it triggered to protect the Pack, not himself).

**Mechanical note for transformation:** Burns a "Life Thread" resource that only regenerates through rest at a safe camp (or Claire-type healing, if you want to bring her in as a rare event NPC later). This keeps it from being spammable — it should feel like the book: rare, costly, last-resort.

### Closing the Scene
> **Galleon:** "Theodore's letting me bring everyone home first. Like he said." *(looks at Milek)* "You coming with us? Or are you still out here counting things?"

**[Final choice — sets next Route]**
A. "I'm still counting." → unlocks optional solo detour floor (Milek investigates Theodore's Troupe alone, Trust Meter implications)
B. "I'm coming home." → fast-tracks to Return to Coneros Route prep

---

## The Erik Reunion — "Charlie Porter"

**Trigger:** First major story floor of the Return to Coneros Route, set at Porter's unit camp (Novelborne territory). This should be the emotional high point of the early-mid campaign — treat it with weight, not as a checkpoint.

### Setup
> *The camp smells like woodsmoke and drilling sweat. Milek hears a voice before he sees the source — small, commanding, entirely too confident for its size.*
> **Voice (off-screen):** "Left foot forward — MORE. You're not gripping it like a handshake, it's a HOLD."
> *Milek stops walking.*
> **Milek (internal):** *That's not Erik's voice. Erik doesn't give instructions. Erik asks questions.*

### The Approach
> *A boy is squared up against a training post, sword raised wrong but with conviction, lecturing absolutely no one. He turns. He sees Milek. The sword drops.*

**[No dialogue choice here — this moment should be uninterrupted, matching the book's restraint with Milek's emotional beats]**

> **Erik:** *(doesn't move at first, like he's checking it's real)* "...Milek?"
> **Milek:** "Hey."
> *That's all either of them manages for a moment. Then Erik crosses the distance at a dead sprint and Milek catches him exactly the way he always has.*

### After the Embrace — first real dialogue choice
> **Erik:** *(pulling back, scanning him head to toe like he's doing inventory)* "You're hurt. Why are you always hurt when you come back."
> **[Player choice:]**
> A. "It's nothing." *(protective deflection — Milek's old pattern; small Erik-trust penalty, he's old enough now to resent being shielded)*
> B. *(Tell him the truth — Tradewick, the auction, the cell)* *(costs nothing mechanically, raises Erik Bond significantly, unlocks a later scene where Erik processes it on his own terms)*
> C. "Who taught you to hold a sword like that." *(deflect with a question — Milek's classic move; Erik answers but the wound question lingers, flagged for a forced conversation later in the Route)*

> *If B:* **Erik:** *(quiet for a long moment, working his jaw the way Milek does)* "I'm not a baby anymore." *(beat)* "Next time. Tell me next time too. Even the bad parts."
> **Milek:** "...Okay."
> *(This line should function as a soft commitment — track it. If Milek later picks Option A again in a future scene, Erik should call it back specifically.)*

### Porter's Entrance — closes the scene, opens the Route's central choice
> *Porter appears at the tent line, unhurried as ever, watching the two of them with the particular patience of a man who already knows what he's about to ask.*
> **Porter:** "Milek of Coneros." *(small pause — almost warmth)* "You kept your deadline. Eventually."
> **Milek:** "I had complications."
> **Porter:** "You always do." *(beat)* "Walk with me. There's a decision waiting that I'd rather not make for you."

**[This is the hook into the campaign's first major branch point — Porter formally re-offering the squire position now that Erik is settled and safe, vs. Galleon's "everyone goes home" promise pulling the other way. I'd recommend NOT resolving this here — let it simmer as a Route-spanning tension, mirroring how the book holds Porter's offer unanswered through the battle.]**



# Prose Review: PRD — The Margin

**Reviewer role:** Editorial reviewer, prose quality
**File reviewed:** `prd-The-Margin-2026-08-06/prd.md`
**Date:** 2026-08-06
**Scope:** Clarity, consistency, specificity, FR testability, grammar, and 6-month readability for a solo dev.

## Verdict

Structurally strong and tonally consistent. The dark, human voice ("Survival is choice, not chores," "the campfire that gives you warmth, light, cooking, and clean water is also the light and smoke a patrol can see") holds across Vision, User Journeys, and FR descriptions — that is the document's best feature and it does not slip. The FR consequences are mostly concrete, self-contained numeric constants (the hunger/thirst tier math checks out: 250+250+150 = 650 starvation turns; 200+150+100+80 = 530 thirst turns), which is exactly what a build contract needs.

The weaknesses are a small set of internal contradictions/ambiguities in the survival-systems FRs, and a family of telegraphic FR-consequence shorthand that reads like design-note shorthand rather than testable spec. **One P1 (a hard self-contradiction in FR-6 that a dev would trip on), roughly a dozen P2s (mostly ambiguity/jargon that costs time, not judgment), and a scattering of P3 polish items.** Not blocking for review, but the FR-6 contradiction and the FR-8 shorthand should be resolved before any Phase 1 build.

---

## P1 — Should fix (contradicts the build contract)

### P1-1. FR-6: water purification self-contradiction (lines 175–176)
Consequence 1 says **River Area (drink direct or collect; 20% poison if untreated)** — drinking raw river water is explicitly permitted. Consequence 2 says **boiling (Coal + fire → 0% risk; required for all raw water)**. If boiling is *required for all raw water*, then "drink direct" cannot exist as an option. As written, a solo dev implementing the water rules gets two conflicting requirements.

Same consequence also leaves filtration's role ambiguous: if boiling is mandatory anyway, what does "filtration (SKILL-based, reduces but doesn't eliminate risk)" actually buy the player (a fallback when there's no coal/fire? a risk reducer when you choose not to boil?)? This is the single most build-blocking sentence in the document.

**Fix:** state which raw sources may be drunk untreated and at what risk, and what filtration does when boiling is unavailable. "Required for all raw water" should be softened to "required to reach 0% risk" or the drink-direct option removed.

---

## P2 — Should fix (costs time or misleads on a second read)

### P2-1. Line 19 — the tag-convention sentence is a garden path
> "This PRD resolves the brief's seven Open Items with recommendations (tagged `[RECOMMENDED]`, where the decision formally rests with the solo dev, they read as `[ASSUMPTION]` pending confirmation)."

Unanchored "they" (recommendations? decisions? Open Items?), a comma splice after "dev," and the dangling "where" clause. This sentence is the document's abstract and its only explanation of the `[RECOMMENDED]`/`[ASSUMPTION]` convention. Suggested rewrite: "The brief's seven Open Items are resolved here with recommendations. Where the decision formally rests with the solo dev, those recommendations read as `[ASSUMPTION]` pending confirmation."

### P2-2. FR-5 (lines 166, 169) vs Glossary (lines 99–100): "each day" vs "per cycle"
FR-5 says "**each day** rolls a Weather type," and its consequence says "Weather type is rolled **per cycle**." The Glossary defines Day = 100 turns and full cycle = 170. Given the Cold-Snap example ("~38 turns, inside one 70-turn Night"), weather must persist across a night — so it is clearly a per-170-turn-cycle roll. A dev cross-referencing the glossary will read "each day" as a 100-turn roll. Pick one word ("cycle" or "day-and-night cycle") and use it in both places.

### P2-3. FR-15 (lines 262–263): "an own Status block" / "an own HP pool"
Repeated grammar slip — should be "**its own**." This is in a load-bearing feature definition; the error repeats twice in three lines.

### P2-4. Line 127: punctuation break inside an em-dash pair
> "his two duties — guard the Copper Road, guard the town —, Magdalene's letter"

There is a stray comma after the closing em-dash. The em-dash pair itself is heavy inside an already-comma-heavy sentence; consider parentheses instead.

### P2-5. Line 92: "reigned 174 years through the Tribunal Beings"
"Reigned through" is unclear (aided by? by means of? ruling over?). Suggest "has reigned 174 years, propped up by the Tribunal Beings" or similar.

### P2-6. FR-8 (lines 190–192): design-note shorthand, not testable consequences
Under a "(testable)" heading, these are effectively untestable as written:
- "Diarrhea runs parallel (**2x then 3x drain**, lethal if ignored)" — drain of what, at what cadence?
- "Rotgut (**instant 3-in-1**)" — which three debuffs?
- "alcohol-interaction toxin (**latent unless Ale**)" — latent until you drink Ale, then it triggers? Unstated.
- "cure items **shorten Delirium by 75%**" — any cure item? A specific one?
- "the Honeymoon cap **requires a cure item**" — which one? (Earlier the cap is "permanent... until cured" — naming the cure closes the loop.)

This is the FR with the most "bible-open-required" feel. Flag for a specificity pass before build.

### P2-7. FR-11 (line 220): decay-curve format unexplained
"Fresh 100% → 90/93/96 → 78/84/91 → 65/74/85 → 50/63/78 → 35/51/70 → 6th+ beyond repair), SKILL-modified." The slash-triples are never explained — presumably Low/Mid/High SKILL values, but a reader must guess. "6th+ beyond repair" is also cryptic (the 6th repair onward is impossible?). Add a parenthetical: "(three values per repair = Low/Mid/High SKILL)".

### P2-8. FR-13 (line 242): "marginal at Mid/High"
"roughly 5 repairs before near-dead at Low SKILL; marginal at Mid/High" — marginal *what*? Marginal durability gain per repair? Marginal value of repairing at all? Unanchored adjective.

### P2-9. FR-17 (line 278): "holdout node" is undefined jargon
"one active companion recommended *(open: 2 or a holdout node)*" — "holdout node" appears nowhere else and is never defined. If it means a waiting/rest location for inactive companions, say so.

### P2-10. FR-18 (line 287): "both deliver the turn" collides with the mechanical Turn
"success (rejoins) or failure (lost) both deliver the turn: Klein now knows the war." "Turn" is already defined (Glossary line 99) as one action in the tile economy. Using "the turn" for a story pivot here is confusing. "Deliver the story turn" or "the pivot" removes the collision.

### P2-11. Glossary completeness claim vs content (line 84)
The Glossary claims "Every domain noun the rest of the document uses, defined once." But **Novelborne** — Klein's home kingdom, the thing you are trying to reach, the thing the whole west/east pull is anchored to — appears at lines 13, 34, 80, 89, and 114 and is never in the glossary. It is contextually inferable, but it is exactly the kind of load-bearing domain noun the glossary exists to pin down. Add it. (Minor: "Milek" also appears with no entry, though as a main-novel cross-reference he is less load-bearing.)

### P2-12. UJ-4 (line 82): "the grave he watched dug"
"the Graveyard is the grave he watched dug" is a compressed construction that reads wrong. "the grave he watched being dug" (or "... watched them dig") is clear.

### P2-13. UJ-2 (line 64): "night flips the well's creature active"
Awkward verb+adjective construction ("flips ... active"). "activates the well's creature" is clearer. (The "flips [adj]" idiom is used consistently elsewhere and is fine; this instance is the clunkiest.)

### P2-14. FR-4/FR-8 (lines 160, 191): "Trembling" appears in two different progression chains
Hunger's Starving stage runs Fatigue → **Trembling** → Rotting (-15% AG); Thirst's Parched stage runs Withered → **Trembling** → Dried Out. If Trembling is one shared debuff, its two entries should agree; if they are distinct debuffs that happen to share a name, that will confuse anyone implementing the debuff system. State which.

### P2-15. FR-10 (line 211): "hazard weak floor plank — through Tier 3 Old House"
The bridging "through" is confusing: "Tier 1 Hunter's Blind: rope, small tools, 20% Map Fragment; hazard weak floor plank — through Tier 3 Old House: preserved food, cloth, locked cellar; hazard structural decay." Restructure as two discrete examples (e.g., "Tier 1 ... / ... Tier 3 ...") so the em-dash isn't doing double duty.

---

## P3 — Polish (nits; fix opportunistically)

- **Line 56:** "bank a fire" — banking normally means covering embers to slow a burn; context means *build* a fire. Consider "make a campfire."
- **Line 236:** "the sane option" — idiom; "the safe option" is the plain reading.
- **Line 366 (SM-1):** "Full canonical run beatable and lands" — "lands" is vague; "is completable and lands" or rephrase.
- **Line 27:** "built first and deep" — reads compressed; "built first, and built deep."
- **Line 168:** FOV "restored but reduced" — clarify "relative to daytime FOV."
- **Line 309:** "(bonuses merge)" — vague; "bonuses stack" is clearer.
- **Line 243:** "T5 VIP/Legendary" — "VIP" is an unexplained rarity label.
- **Line 136:** "do not tick survival tracks" — odd transitive use of "tick"; "do not advance survival tracks."
- **Line 295:** "Bond ±" — "Bond +/-" survives plain-text rendering better.
- **Line 250:** "GRIT-based %" — "a GRIT-based chance."
- **Line 271:** "greedy follow is a fallback" — fine for a dev audience, but quote it or gloss it ("always-chasing movement").
- **Line 317 vs Glossary line 105:** "Black Market Trader" (FR-21) vs "Caravan Black Market Trader" (Glossary) — one name. Also "Gold-tier" (trader) is ambiguous (tier of goods? price range?).
- **Glossary line 112 vs FR-17 line 278:** "Help and liability (feeds, louder, must be protected)" — "feeds" as a shorthand noun is awkward; FR-17's "a second mouth to feed" is the clearer phrasing. Align.
- **Section numbering:** sections jump §4.7 → §6; there is no §5. Either renumber or note the intentional gap — a solo dev will otherwise wonder if a section is missing.
- **POV:** second person in Vision/prose ("you are a trained knight") vs third person in UJ/FR ("he pushes east", "Klein has four tracks"). Defensible and consistent per section, but worth a stated convention so future writers don't wobble mid-section.
- **FR-1 (line 135):** "The intro reads in the SPD-style text-forward tone" — subjective; under a "(testable)" header, restate as an observable ("the intro is paged text; the bottom message log is the primary text surface").
- **Line 23:** "every fight costs HP, weapon durability, noise that draws reinforcements, and occupation escalation" — "costs ... occupation escalation" is a strained collocation; "spends HP and durability, makes noise that draws reinforcements, and escalates the occupation."
- **Line 25:** "east toward the Copper Road and the occupiers for what survival demands, west across the border toward home" — the "for what survival demands" modifier attaches only to the east clause; slight asymmetry.
- **Line 142 vs line 153:** FR-2's diegetic control list (move, scavenge, eat, craft, hide, rest) omits drink, fight, wait, and craft/repair that §4.2's 9-action list includes, yet FR-2 says "All controls ... are demonstrated." If the tutorial intentionally covers a subset, say "the opening controls."

---

## What's working (don't touch)

- The tonal through-line is the strongest element — Vision's voice survives into FR descriptions without jarring register shifts.
- The numeric tier math is internally consistent (hunger/thirst death times reconcile with the per-tier durations).
- The `[ASSUMPTION]`/`[RECOMMENDED]` inline-tag system and §9 index are a genuine readability asset for a solo dev; the P1/P2 fixes above make the system itself parseable.
- UJ cards are consistently structured (Persona/Entry/Path/Climax/Resolution/Edge case), which makes the three-act tension readable at a glance.

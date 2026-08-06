# PRD Quality Review — The Margin

## Overall verdict

This is a strong PRD for a draft. The thesis ("get home" against a war too big for fighting; survival as choice under scarcity; horizontal progression) is coherent end-to-end, trade-offs are named with what is given up rather than smoothed to neutral, and the FRs carry genuinely testable numeric consequences. What is at risk is downstream mechanics, not substance: the Assumptions Index roundtrip is broken (§0 promises inline `[ASSUMPTION]` tags; the body has none), the engine stack is asserted in scope while still an open decision, and several FR consequences embed TBDs that story creation will hit. Fix those before handing to epics-and-stories; the decision content itself is ready.

## Decision-readiness — strong

The PRD states decisions as decisions, and open items as open. §8 resolves all seven of the brief's Open Items with explicit `[RECOMMENDED]` stances, and the one item whose decision formally rests with the solo dev (§8/Q1, engine) says so plainly: "Decision formally rests with the solo dev; confirm in architecture." Q7 (run length) is a genuine open question with derived math, not a rhetorical one: "~3–6 in-game days (a handful of 170-turn cycles)… a starting calibration target, not a commitment."

Trade-offs are named with the thing given up, not just what is chosen — the campfire that "gives warmth, light, cooking, and clean water" is simultaneously "visible and audible to patrols" (§4.2/FR-7, §1); the foray loop trades "a safe point" for "scavenge under its hazard" (§4.3/FR-10); companions are "help *and* liability (feeds, louder, must be protected)" (§3, Glossary — Companion). The lone `[NOTE FOR PM]` at §6.2 sits on a real tension (companion-roster trim), not a safe checkpoint.

One place the PRD smooths over a decision: the engine stack is listed unconditionally in scope even though §8/Q1 marks it open.

### Findings
- **[medium]** Engine stack asserted in scope while still open (§6.1 In Scope, §8/Q1, §9). §6.1 lists "desktop-first (libGDX/LWJGL3, Windows/Linux)" with no caveat under In Scope, and §9 carries it as an assumption, yet §8/Q1 declares the decision "formally rests with the solo dev; confirm in architecture." The source brief was more careful — its equivalent scope line carried an inline tag "`[ASSUMPTION — engine decision pending Open Item 1]`" (brief §Scope), which the PRD dropped. A reader extracting §6.1 as the build list would treat libGDX as locked. *Fix:* add the pending-confirmation caveat to the §6.1 entry and restore the inline assumption tag.

## Substance over theater — strong

No dimension here reads as furniture. The Vision (§1) is product-specific to the point of being un-swappable — "the campfire that gives you warmth, light, cooking, and clean water is also the light and smoke a patrol can see," "east toward the Copper Road and the occupiers… west across the border toward home." It could not be dropped into any other PRD.

Personas earn their place: the Non-Users (§2.2 — power-fantasy player, completionist, pure-mechanical-loop player) map directly onto Non-Goals (§5: no power fantasy, no save-scumming, story is load-bearing). The four UJs each name Klein and carry a decision-driving conflict, not flavor. Differentiation claims are specific and reappear as system consequences (gear-with-memory's repair-decay curve in FR-13, the sole location that "flips *safer*" at night in FR-10). There is no NFR boilerplate — if anything, the PRD errs the other way (no NFR section at all, see Done-ness).

No findings needed.

## Strategic coherence — strong

The PRD has a thesis and the features serve it. §1 states the bet ("one continuous, permadeath life across a persistent Herois… Survival is choice, not chores"), and the feature groups trace the arc: survival core (FR-4–8) → foray/world/horizontal growth (FR-9–11) → combat as cost (FR-12–14) → story/companions (FR-15–19) → scarcity economy (FR-20–21). Everything loops back to the east/west pull that §1 names.

Success Metrics validate the thesis rather than measure activity: SM-3 tests "a player who knows the forest measurably survives longer than one who doesn't, without any XP" (horizontal progression), SM-4 tests the pull itself ("the player is genuinely pulled two ways… and reports the tension in playtesting"). The counter-metric is named and argued: SM-C1 "Combat win rate — this should *not* be optimized up." The MVP scope is a coherent experience/problem-solving kind, and the scope list matches the thesis (canonical spine first; What-if branches out).

No findings needed. (Minor: SM-C1's "Counterbalances SM-3, SM-4" link is loose but defensible — over-strong combat would let players fight instead of know the forest or feel the pull.)

## Done-ness clarity — adequate

The "Consequences (testable)" pattern is the PRD's strongest structural choice, and most FRs earn it with real numbers: hunger tier turn-counts that sum to the stated death time (FR-4: 250 + 250 + 150 = 650), weather weights that sum to 100 (FR-5), the full repair-decay curve "Fresh 100% → 90/93/96 → 78/84/91 → 65/74/85 → 50/63/78 → 35/51/70 → 6th+ beyond repair" (FR-13), and currency ratios 25:1 / 10:1 / 1,000:1 (FR-21). This is exactly the material story creation needs.

It is not yet done-clean, for three reasons: (1) open items are embedded inside FR consequences, so an engineer reading a single FR still cannot build it to done; (2) a few consequences are subjective adjectives rather than observables; (3) there is no non-functional/UX section at all, so the "SPD-style presentation" lock has no bounds.

### Findings
- **[medium]** Open items embedded in FR consequences (§4.4/FR-12 "tie-break TBD"; §4.4/FR-14 "does a failed roll also consume it?"; §4.5/FR-15 companion death "*(Open.)*"; §4.5/FR-17 "open: 2 or a holdout node"; §4.6/FR-20 "Weight capacity scales with STR (tiers TBD)"; §4.6/FR-21 "*(Open: stack/compression rule.)*"). Each is honestly flagged, but the per-FR contract is not implementable as written, and story creation will trip on them one by one. *Fix:* move each to a per-FR "Open" sub-block or into §8 with an FR back-reference, keeping the locked consequence set unambiguous.
- **[medium]** Subjective language where an observable is needed (§4.1/FR-1 "The intro reads in the SPD-style text-forward tone" — tone is not testable; §7/SM-1 "the full canonical run is beatable and *lands* … earned, story told through the systems" — "lands" and "earned" are the whole success criterion). *Fix:* phrase SM-1 as a checklist of observable end-states (crossing occurs, epilogue screens present, no progression-killing softlocks, journal entries reachable) and define "SPD-style tone" by example or by reference.
- **[low]** No non-functional section (§4–§6). The only NFR-family constraint is the "SPD-style presentation" lock (§6.1, §1), which is unquantified — no target FPS, screen sizes, message-log rules, or asset/turn-cost bounds. *Fix:* add a small NFR block (perf target, HUD/log rules, load times) so UX and story passes inherit guardrails rather than inventing them.

## Scope honesty — adequate

The explicit scope machinery is good. §5 Non-Goals does real work and names specific design commitments (no floor-descent, no combat-XP, no save-scumming, no What-if endings in v1, Region 2 deferred). §6.2 Out of Scope is explicit, and the roster-trim flag there is a genuine de-scope proposal, not a silent cut. The open-items density (7 resolved-from-brief + 6 additional) is appropriate for a draft that has not been green-lit.

What fails is the Assumptions Index roundtrip — the mechanism that was supposed to surface inferences for confirmation is asserted in §0 but not actually present, and one scope item is stated as decided when §8 says it is not (see Decision-readiness finding).

### Findings
- **[high]** Assumptions Index roundtrip is broken — the body has no inline `[ASSUMPTION]` tags. §0 promises "every `[ASSUMPTION]` is tagged inline and indexed in §9," and §9's header repeats "Every `[ASSUMPTION]` from the document," but a document scan finds the string only in §0 and the §9 header — no `[ASSUMPTION]` tags exist at point of use. §9 is a hand-curated prose list keyed to section numbers, and §0's taxonomy ("where the decision formally rests with the solo dev, they read as `[ASSUMPTION]`") does not match §8's actual `[RECOMMENDED]`/`[OPEN]` tags. A reader cannot confirm, at the point of use, which statements are assumptions vs. decided. The brief (its source) actually modeled the correct pattern — see the inline `[ASSUMPTION — engine decision pending Open Item 1]` tag. *Fix:* either tag each assumption inline with `[ASSUMPTION]` and keep §9 as the index, or rewrite §0/§9 to describe what is actually present (curated assumptions list).

## Downstream usability — adequate

The Glossary (§3) is the right anchor and the FRs respect it — Campfire, World-Structure Location, Danger Tier, Foray, SKILL, Last Stand are used identically across §4, §7, and the UJs. IDs are contiguous and unique (UJ-1–4, FR-1–21, SM-1–4 + SM-C1), cross-references by ID resolve, and every UJ names its protagonist (Klein) with inline context, none floating. The Assumptions Index entries keyed to §4.2/FR-5, §4.4/FR-12, and §8/Q1–Q7 all resolve to the right content.

Three snags, one of them a genuine broken reference, hold this back from strong.

### Findings
- **[medium]** Broken cross-reference in the Assumptions Index (§9). The entry "§4.6/FR-19 — Currency scarcity and the two-trader economy are bible-anchored; the coin-weight rule is open" points at FR-19, which is *Dialog and quest delivery* in §4.5. The currency content is FR-21 (§4.6). *Fix:* retarget the entry to §4.6/FR-21.
- **[low]** Glossary drift / undefined domain terms. "SPD" is spelled out only inside the §2.1 builder JTBD ("not a reskin of Shattered Pixel Dungeon") but used bare in §1, §6.1, and FR-1; "Mystic" and "Neutral-pool" appear inside the Companion glossary entry (§3) without their own definitions; "Region 2" (§5, §6.2, §8/Q3) is used as a domain noun but never defined. *Fix:* add SPD, Mystic, Neutral-pool, and Region 2 to the Glossary, or spell out at first use.

## Shape fit — strong

The shape matches the product. This is a consumer game with meaningful UX and story, so the four UJs with named protagonists are load-bearing, not overhead — and each is substantive (entry state, path, climax, resolution, edge case). Rigor is calibrated to a solo developer: not over-formalized (no UJ density for an internal tool), and the substance bar still holds. It is also an honest brownfield document: references to the existing build are specific and checkable ("the distraction action already exists in the build," §4.5/FR-16; "the bible's own 'reuse note' says the current build already has working turn engine, tilemap, FOV, detection, noise, combat, hunger, inventory, save/serialization," §8/Q1), which the brief corroborates.

No findings needed.

## Mechanical notes

- **Assumptions roundtrip:** broken — no inline `[ASSUMPTION]` tags in the body despite §0 and §9 claiming them (see Scope honesty, high).
- **ID continuity:** FR-1–21, UJ-1–4, SM-1–4 + SM-C1 contiguous and unique; no duplicates.
- **Broken cross-refs:** one — §9 "§4.6/FR-19" should be §4.6/FR-21 (see Downstream usability, medium).
- **Glossary drift:** SPD, Mystic, Neutral-pool, Region 2 undefined or spelled out only once in prose (see Downstream usability, low).
- **UJ protagonist naming:** all four UJs name Klein with inline context; none floating. ✓
- **Required sections:** all present for the stakes — Vision, Target User (JTBD / Non-Users / UJs), Glossary, Features (21 FRs in 6 groups), Non-Goals, MVP Scope (In/Out), Success Metrics (incl. counter-metric), Open Questions, Assumptions Index. ✓

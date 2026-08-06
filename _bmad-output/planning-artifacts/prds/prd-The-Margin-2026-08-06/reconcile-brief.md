---
title: "Reconcile: Product Brief → PRD"
created: 2026-08-06
input: planning-artifacts/briefs/brief-The-Margin-2026-08-06/brief.md
target: planning-artifacts/prds/prd-The-Margin-2026-08-06/prd.md
---

# Reconcile: Brief → PRD

**Verdict:** Every checklist commitment is present. **4 gaps found** — all qualitative/coverage, no hard contradictions. The PRD is a faithful, richer expansion of the brief; what was dropped is tone, audience framing, and one architecture guarantee.

## Checklist confirmed

- **Exec summary** — fully captured (PRD §0, §1): Herois, ~50–60 yrs pre-novel, Klein, Novelborne/Corneo, Evermove invasion, alone-and-hunted, "get home" to Magdalene + parents.
- **4 nested loops** — The Turn (FR-4/§4.2, includes brief's move·scavenge·eat·craft·fight·hide·rest plus drink/wait), The Foray (FR-9/FR-10), The Day (FR-5), The Act (FR-18).
- **East/west spatial tension** — FR-9, §1, UJ-3 ("pulled two ways every day"), UJ-4 climax ("home a few miles of forest away").
- **5 differentiators** — all present: knight-in-a-war-too-big (§1, FR-12, §5), no floor-descent/no combat-XP (FR-11, §5), canon-spine-with-What-if-branches (FR-11, §5, §8/Q5), prequel hook (Glossary Corneo→Coneros, FR-18 epilogue, §1), survival-is-choice-not-chores (§1, FR-7).
- **Both success criteria** — SM-1 (canonical run, story through systems, epilogue seeds) and SM-2 (several in-game days, Phase 0–1) mirror the brief exactly.
- **Scope In** — §6.1 mirrors all 8 brief In items, including desktop-first libGDX/LWJGL3 (port-open clause survives in §9 index).
- **Scope Out** — §5/§6.2 mirror all 4 brief Out items; **no Out-scope item leaked in**.
- **7 Open Items** — all present in §8 Q1–Q7; Q1 KEEP, Q2 HYBRID, Q3 final-tense-run-not-boss, Q4 locks "Follow the Road" + "The Rescue", Q5 locks the four companions, Q6 numbers-deferred, Q7 derived ~3–6 days.
- **No direct wording contradictions** between brief and PRD.

## Gaps

1. **Vision's extensibility guarantee dropped.** The brief's Vision promises "depth is layered on — Region 2 through the Deep Cave Mouth, more companions, more of the buried truth — **without ever breaking the canonical story or the core survival loop**," plus the replay framing "what else could this war have made of Klein?" PRD §1 condenses this to "branch support" and drops the "without breaking canon / core loop" architecture guarantee — which is the load-bearing half of "author for the canon, architect for the branch."

2. **Secondary player persona absent.** The brief's Audience section names the narrative-first player and the promise "death is the run, and the run is a life" (losing a run must not feel like losing the story). PRD §2 has JTBDs and Non-Users only; this persona and its promise are missing (only implicit in FR-14/permadeath). Recommend an explicit §2 persona or design constraint.

3. **"Small goal / huge world" tone contrast diluted.** The brief's emotional counterpoint — "the smallest and most human one imaginable **in a world of god-kings and 174-year immortals**" — is cut in PRD §0 to "smallest and most human one imaginable"; the god-kings/174-year contrast survives only as glossary data ("reigned 174 years through the Tribunal Beings"). Also trimmed: "noble-born," "routine posting," and the primary player's "survive, go home, marry the girl, live" framing.

4. **Run-length open item only partially resolved.** Brief Q7 asks "how many in-game days / **real hours** a canonical run should take." PRD §8/Q7 answers in-game days (~3–6, derived from survival clocks) but drops the real-hours half of the question and explicitly declines to commit.

## Minor notes (not counted as gaps)

- The brief's "why now" — "the vertical-slice playable screen is built and runs" — is referenced only obliquely in §8/Q1; the working playable screen is the strongest KEEP signal and could be stated outright.
- The mobile-friendly-HUD / future-port clause is inline in §6.1 only as "desktop-first"; the port-open intent appears only in the §9 assumptions index.

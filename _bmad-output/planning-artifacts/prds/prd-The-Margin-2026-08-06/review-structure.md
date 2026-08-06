# Structural Review — PRD The Margin

- **File reviewed:** `prd-The-Margin-2026-08-06/prd.md` (422 lines)
- **Review scope:** structure only (ordering, section integrity, FR/UJ/§ cross-references, Assumptions Index completeness, Glossary consistency, length/stakes fit)
- **Reviewer:** editorial reviewer (structure), tiered P1/P2/P3
- **Date:** 2026-08-06

---

## Overall verdict

**Structurally sound, with two defects that should be fixed before downstream handoff.**

The document's section purposes are clear, the FR numbering (FR-1…FR-21) is globally sequential and thematically grouped, UJ/§/SM cross-references are accurate and internally consistent, and the length is appropriate for a solo-dev hobby-project PRD (complete, not padded). Two structural problems dominate:

1. **The §5 Non-Goals section is missing.** The document jumps §4 → §6. The non-goal content exists but is misfiled inside §4.7 "Cross-Cutting NFRs" (as "No…" bullets that are not NFRs) and partially duplicated in §6.2 "Out of Scope for MVP."
2. **The Assumptions Index (§9) contract fails in both directions.** One inline `[RECOMMENDED]` tag is not indexed (§2.2), and five index entries point at prose that has no inline `[ASSUMPTION]` tag.

Everything else is P3 polish.

---

## P1 — Should fix

### P1-1. Missing §5 Non-Goals; non-goal content misfiled in §4.7 and duplicated in §6.2

- The heading sequence is `## 4. Features` (line 121) → `## 6. MVP Scope` (line 341). There is no §5, and nothing named "Non-Goals" exists anywhere in the document.
- §4.7 is titled **"Cross-Cutting NFRs (the SPD-style presentation lock)"** (line 321) and its description says it bounds "UX, performance, and platform decisions." That is the NFR job. But lines 332–339 then list eight non-goals that are not NFRs and not SPD-presentation constraints:
  - No floor-descent; No combat-XP; No save-scumming / no difficulty tiers; No power fantasy; No multiplayer/online/service; No "What if…" branch endings; Region 2 deferred; No rigid quest taxonomy.
  - "No multiplayer" and "No power fantasy" are scope/exclusions, not quality attributes; they leak outside the section's stated job.
- §6.2 "Out of Scope for MVP" (lines 353–359) re-states four of the same items (What-if branches, Region 2, multiplayer, non-roguelike modes/no save-scumming) — so non-goal content appears in **two** places (§4.7 and §6.2) plus a third echo inside FR-11 (line 222, the What-if parenthetical).
- **Why it matters:** a reader of §4 (Features) cannot tell NFRs from exclusions; the "Non-Goals" section the structure spec requires is absent; and future edits will drift the two lists out of sync.
- **Fix:** create **§5 Non-Goals**, move the eight bullets from §4.7 into it, keep §4.7 strictly NFRs (Rendering/Interaction/Text/Platform/Performance), and collapse §6.2 to scope-specific exclusions (e.g., the companion-roster trim note at line 359) plus a pointer to §5 rather than restating it.

### P1-2. Assumptions Index (§9) is incomplete: an inline `[RECOMMENDED]` tag is not indexed

- §9's contract (line 405): "Every `[ASSUMPTION]` and `[RECOMMENDED]` from the document, surfaced for explicit confirmation."
- The tag at **§2.2 / line 41** — `[RECOMMENDED: treat as secondary but design for; the survival loop must stay playable-first per SM-2.]` — is the only `[RECOMMENDED]`/`[ASSUMPTION]` tag in the document with **no** corresponding index entry. The other six `[RECOMMENDED]` tags (§8/Q1–Q6) are all indexed.
- **Why it matters:** the doc's own promise (stated again in §0, line 19) is "every `[ASSUMPTION]` is tagged inline and indexed in §9." A lone orphan tag means the §8-style confirmation loop will silently miss a real design decision (the secondary-player stance).
- **Fix:** add an index entry for §2.2, or drop the tag and fold the note into prose.

---

## P2 — Should fix when convenient

### P2-1. Assumptions Index entries with no inline tag to point at

Five index entries (lines 410–414) have **no** matching inline `[ASSUMPTION]`/`[RECOMMENDED]` tag in the body:

| Index entry | Inline evidence |
|---|---|
| §2.4 — UJ narratives from bible beats, validate during review (line 410) | §2.4 (line 51) states it in prose, no tag |
| §3 — Glossary bible-anchored (line 411) | §3 (line 86) states it in prose, no tag |
| §4.2/FR-5 — Weather weights, day/night lengths (line 412) | FR-5 (line 166) lists weights in prose, no tag |
| §4.4/FR-12 — Combat action set, AG turn order (line 413) | FR-12 (line 233) lists actions in prose, no tag |
| §4.6/FR-21 — Currency scarcity, two-trader economy (line 414) | FR-21 (line 316) in prose, no tag |

- **Why it matters:** these read as "assumptions" but are not tagged, so they are invisible to any automated tag-scan and live outside the confirmation loop the §9 intro describes.
- **Fix (pick one):** (a) add inline `[ASSUMPTION — …]` tags at §2.4, §3, FR-5, FR-12, FR-21 so index entries resolve to tags; or (b) revise the §9 intro to distinguish "tagged assumptions/recommendations" from "bible-anchored parameters carried for confirmation" and label these five as the latter.

### P2-2. Open scope decisions embedded inside FR consequence lists (leak into Features)

The checklist calls out "Scope decisions that belong in §6 but appear in §4." Four FRs carry unnumbered `*(Open: …)*` markers inside their testable consequences:

- FR-14, line 250 — `*(Open: does a failed roll also consume it?)*` (Last Stand)
- FR-15, line 265 — `*(Open.)*` + "is a scope decision" (companion death)
- FR-17, line 278 — `*(open: 2 or a holdout node)*` (party size)
- FR-21, line 318 — `*(Open: stack/compression rule.)*` (coin weight)

These are all echoed in §8's "Additional open items surfaced during extraction" (lines 394–401), so they are not lost — but the §8 list does not reference the FR IDs, so the two surfaces are linked only by inference. A reader scanning §8 cannot tell which FR each open item belongs to.

- **Fix:** keep the `(Open:)` markers inline (they are useful flags), but in §8's additional-open-items list, prefix each item with its FR ID (e.g., "FR-14: Does a failed Last Stand roll also consume the once-per-run save?") so the cross-reference is explicit.

---

## P3 — Polish

### P3-1. Index entry 1 cites §1, but no tag exists in §1
Line 407 lists "§1/§4.7/§6.1 — Desktop-first… Inline tag: `[ASSUMPTION — engine decision pending Open Item 1]`." The tag actually appears only in §4.7 (line 329) and §6.1 (line 351). Drop §1 from the reference or add a tag in §1.

### P3-2. Gear-with-memory decay curve restated in two FRs
FR-11 bullet (line 220) and FR-13 bullet (line 242) both state the repair-decay curve. FR-13 could cross-reference FR-11 ("see FR-11") instead of restating the curve, keeping one authoritative home. (Related: "gear-with-memory" is load-bearing in three FRs — FR-11 progression, FR-13 combat durability, FR-20 inventory bags — each a distinct facet, so no change needed beyond the dedup.)

### P3-3. Glossary coverage gaps vs. its own "defined once" claim
§3 (line 86) claims "Every domain noun the rest of the document uses, defined once," but these load-bearing terms are used in FRs and not defined: **Weather** (FR-5, line 166), **Tile** (UJ-1, line 55), **FOV** (FR-5, lines 100/168). Consider adding minimal entries (Weather + FOV are the most consequential; Tile is cosmetic).

### P3-4. Minor terminology variance
- "Day/Night Cycle" (Glossary, line 100) vs "Day/Night clock" (FR-4, line 158) vs "Day/Night" (FR-5, UJ-1). Same concept, three surface forms.
- "What-if" (Glossary, lines 97–98) vs "What if…" (lines 337, 355). Same concept, hyphen inconsistency.

### P3-5. UJ section (§2.4) uses glossary terms before the Glossary (§3)
UJ-2 (line 64) uses "Sunken Well (Tier 2)" before §3 defines Danger Tier / World-Structure Locations. The prescribed order (Target User → Glossary) is met, but readers hit undefined terms. A one-line pointer in §2.4 ("terms defined in §3") would close the gap without reordering.

### P3-6. SM coverage is sparse by design, but two cheap additions would close it
§7 intro only claims each SM validates FRs (not full coverage), so this is acceptable. Still, 13 of 21 FRs have no validating SM (FR-1,2,3,8,12,13,14,15,16,17,19,20,21). A lightweight onboarding SM (intro/capture, FR-1–3) and one for the debuff pipeline (FR-8) would round out §7 with minimal cost.

### P3-7. §9 index padding
Entries for §2.4 and §3 (lines 410–411) duplicate prose already stated in those sections; the §8/Q1–Q7 entries (lines 415–421) restate the §8 recommendations by design but bloat the index. The index would be tighter as: full entries for genuinely new assumptions (§4.7, §8/Q7) + a single pointer "§8/Q1–Q7 — see Open Questions" for the recommendations.

### P3-8. §6.1 duplicates §4.7 platform NFR
Line 351 repeats the libGDX/LWJGL3 desktop-first line (with the same assumption tag) that §4.7 line 329 already states. One reference can cross-link the other.

---

## Checklist reconciliation

| # | Check | Status |
|---|---|---|
| 1 | Section ordering (Purpose → Vision → Target User → Glossary → Features → **Non-Goals** → MVP → SM → Open Q → Assumptions) | **FAIL** — §5 Non-Goals missing (P1-1) |
| 2 | Each section does one job, no leakage | **PARTIAL** — non-goals inside §4.7 (P1-1); scope decisions embedded in FRs (P2-2) |
| 3 | FR numbering/grouping coherent; FR/UJ/§ cross-refs consistent | **PASS** — FR-1…21 sequential and themed; all FR→UJ, FR→FR, SM→FR, § refs verified accurate; minor FR-11/FR-13 curve restatement (P3-2) |
| 4 | Assumptions Index indexes all inline tags, and vice-versa | **FAIL** — orphan §2.2 tag (P1-2); five tag-less index entries (P2-1); §1 mis-citation (P3-1) |
| 5 | Glossary used consistently, no synonyms | **PARTIAL** — consistent usage; minor undefined terms (Weather/Tile/FOV, P3-3) and variant naming (P3-4) |
| 6 | Length/stakes fit solo-dev hobby PRD | **PASS** — appropriate weight; slight padding from §4.7/§6.2 overlap and §9 restatements |

---

## Suggested fix order

1. Create **§5 Non-Goals**; move §4.7's eight "No…" bullets into it; slim §4.7 to true NFRs; dedupe §6.2. *(P1-1)*
2. Add a §2.2 index entry. *(P1-2)*
3. Tag or relabel the five prose assumptions, and drop §1 from index entry 1. *(P2-1, P3-1)*
4. Add FR IDs to §8's additional open items. *(P2-2)*
5. Apply the P3 polish items opportunistically.

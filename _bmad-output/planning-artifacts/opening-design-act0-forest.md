# Opening Design — Act 0 (Intro) + Act 1 (Forest)

**Status:** Design captured 2026-07-17. Feeds **Epic 6** stories `6-3-five-nights-again-opening-quest`, `6-2-authored-story-floor`, `6-4-galleon-reunion-scene`.
**Source material:** `Forward/The Margins` novel — Prologue, Ch 1 (*The Day White Snow Became Red*), Ch 2 (*Spoils of War*), Ch 3 (*Forest*).
**Sequencing decision (locked):** *Systems-first* — build the survival/stealth/companion/dialogue systems (Epics 2–5) first, then **author** this opening in Epic 6 on top of them. This doc is the authoring target, not new early scope.
**Delivery decision (locked):** Act 0 is **fully playable** — every beat is interactive, including the doomed encounters, which are scripted "unwinnable" moments that teach the rules by taking them away.

---

## The pitch in one line

The intro is the novel's Prologue→Ch 2. It teaches every core rule **as a loss** — you fight and it does nothing, you hide and get caught, salvation comes from a power you don't understand — so that by the time control hands over in the pines, the player *feels* why survival, not combat, is the game. Then **Act 1 = Ch 3**, the first free-play region, where the whole survival loop comes alive.

---

## ACT 0 — The Intro (fully playable, ~5–8 min)

A guided sequence in a **desaturated frost-and-ash palette**. Color returns the moment the player breaks into the free Forest — a mechanical signal of "now you're playing for real."

**Design guard-rail (mitigates forced-loss frustration):** each interactive beat is about *character expression*, not winning. The player chooses *how* Milek loses, never *whether*. Keep every beat short; telegraph clearly that this is the inciting tragedy, not player failure.

| # | Beat | Novel source | What the player does | Teaches |
|---|------|--------------|----------------------|---------|
| 0.1 | **The river** | Prologue | Walk Milek to the river, fill two buckets; Erik follows and banters | movement, interact, companion-follow — in total safety |
| 0.2 | **The house** | Prologue | Bound on the floor. Only inputs: *struggle* (does nothing) and a timed *"Nira — run!"* shout that lets her flee | the world ignores your force; your one real power is a **word** (foreshadows VOICE) |
| 0.3 | **The flight** | Ch 1 | Guide the family through back routes under light stealth (avoid sightlines), carrying Erik (slowed). Knights are **pre-positioned** at the river — capture is inevitable; a final dagger-swing is caught | stealth matters but can be out-thought; fighting knights is futile; "the opening comes later" |
| 0.4 | **The platform** | Ch 1–2 | On-rails march (look at faces = optional flavor). Mother hanged. Rope settles; a last *hold Erik's gaze* prompt. Lever drops → **Liga-Meteor** → control snaps back: cut Erik free (dagger still in boot, timed) and **run** into the pines | salvation comes from outside/above (Blackberry Troupe hook); your instinct is to protect first — exactly what Theodore notes from the watchtower |

**Handoff — Mora (Ch 3 open):** Mora finds them, presses the **folding knife** into Milek's hand (first tool), and gives the objective:
> *North pines → follow it until the trees thin → find water and stay near it → find the Ashen merchants on the ridge road before the Gilimans do.*

Control goes fully free. Palette blooms to color. **Act 1 begins.**

---

## ACT 1 — The Forest (first playable region = Ch 3)

**Objective:** survive the pines and reach the ridge road / the Ashen merchants.
This is the tutorial-through-play for the survival loop (all systems already shipped by Epic 6):

- **Shelter:** claim the hollow rotted pine trunk as a camp/hideout.
- **Forage nodes** (Ch 3, 1:1 with the design table): Tall Grass → corn, berries · Logs → mushroom, moss, tinder · Tree → wet/dry branches, bark · Rock → pebbles.
- **Hunger clock** (Well-fed → Stable → Hungry → Starving) is the pressure; "it was not enough" is the felt tutorial.
- **Identify-on-use:** the mushroom check (safe vs poison) as a time-cost + Instinct check.
- **Craft to Ch 3's own to-do list:** moss = warmth/bedding · tinder + dry branch = fire · bark + waxy leaves = water collector · stolen canvas = waterproofing. Wet/dry + weather threatens the camp track.
- **The camp raid = "Five Nights, Again":** discover the Lamilla camp; raid it across multiple nights; learn which guards sleep lightly; greed-curve the loot (bread → rope → blanket → eggs → waxed canvas). Pebbles feed the shipped Noise system as distractions.
- **Region climax — "trailed home":** on the deepest raid, a detected enemy **follows Milek back to the trunk and the sleeping companion** (the Ch 3 cliffhanger). Detection = a *tail you must lose*, not just an alarm. This set-piece ends Act 1 and launches the road.

---

## How it decomposes into existing Epic 6 stories

- **`6-3-five-nights-again-opening-quest`** → Act 1's multi-night camp-raid loop + the trailed-home climax (the core of the Forest region).
- **`6-2-authored-story-floor`** → the authored Act 0 sequence (0.1–0.4) + Mora handoff, scripted over the shipped systems.
- **`6-4-galleon-reunion-scene`** → later road payoff; unaffected here except by the companion decision below.

---

## Resolved — the Forest companion: **Option C (Hybrid)** ✔ (locked 2026-07-17)

The novel's Forest companion is **Erik** (Milek's 6-year-old brother), but the game's companion system (**Epic 4 "Galleon at Your Side"** — follow, distraction, Bond) and the capstone **`galleon-reunion-scene`** center **Galleon**, who in the novel does not join Milek until ~Ch 6–7.

**Decision — Hybrid:**
- **Erik is the Act 0 + Act 1 (Forest) companion** — the intro and first region stay emotionally exact to the source.
- The **companion system (follow / distraction / Bond) is authored entity-agnostic** — it binds to *whichever* companion is active, not to Galleon specifically.
- Galleon becomes a **later-arc road companion**; when he joins, the same system re-skins onto him, and **`galleon-reunion-scene`** pays off the Blackberry-Troupe hook that Act 0 plants from the watchtower.

**Implication for Epic 4:** build "Galleon at Your Side" as a generic **Companion** entity + system (Erik = first bind, Galleon = later bind). Do **not** hard-code Galleon into follow/distraction/Bond logic. The story labels change; the mechanic does not.

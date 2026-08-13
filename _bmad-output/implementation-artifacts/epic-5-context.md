# Epic 5 Context: Companions & The Story

<!-- Generated from planning artifacts. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Epic 5 is the capstone that turns the survival sandbox into a story. It delivers companions as full tile-agents — each with its own Status block, HP pool, condition/debuff state, and a behavior state machine that makes it both genuine help and genuine liability — deepens the per-companion Bond through shared survival and dialogue, and plays the act-gating main story ("Follow the Road" → "The Rescue" → the choice) to its canonical homecoming. The climax is the NW border crossing: a final tense escape run past an act-scaled Giliman cordon, not a boss fight. This epic makes Klein's small, human goal — get home — into playable, earned game state, and validates the game's core thesis (SM-1) that the story is lived through the systems rather than told. It builds on the Epic 2 dialogue/quest foundation, the Epic 4 combat authority (companion FIGHT), and the Epic 3 world (act-gates are spatial pushes east).

## Stories

- Story 5.1: Companion as a full tile-agent
- Story 5.2: Autonomous companion behavior state machine
- Story 5.3: Simple orders steer the companion
- Story 5.4: Companion combat and the liability cost
- Story 5.5: Bond and the shapes of loss
- Story 5.6: The act-gating quests
- Story 5.7: The border-crossing win and epilogue

## Requirements & Constraints

- Companions are autonomous entities, never passive followers: each carries its own stat/Status line (combatants a combat-relevant line, non-combatants a lighter one), its own woundable/healable/incapacitable HP pool, and its own survival + condition/debuff state that shares the forest's pressure like any body.
- Companion behavior is driven by the same detection and noise rules as enemies — a hidden companion is quiet, a panicking one is loud and can blow Klein's stealth. Greedy follow is a fallback, not the model.
- Companions are help *and* liability: extra food to feed, added stealth-noise penalty, a body to protect. Non-combatants (Mara, Old Fen, Yenna) never fight and must be defended; only combatants (Aldric) trade blows. One active companion is the recommended party size.
- Loss takes exactly three shapes: Captured (recoverable via quest), Departure (low Bond), or Death (permanent — permadeath-weighted; whether true death is on the table is a story-scope decision, so keep it a rare, devastating case if enabled).
- Bond deepens via shared survival and dialogue choices: high Bond unlocks lore/loyalty/personal quests, low Bond withholds help or triggers departure, betrayal turns the companion hostile.
- The main story is act-gated across three acts (Survive → Understand → Decide). "Follow the Road" (reach the Copper Road corridor, a Tier 2 push) gates Act 1→2; "The Rescue" (reach/attempt Aldric's prison) gates Act 2→3, with success (Aldric rejoins) and failure (lost) both flipping the gate. Act 3 is the choice → last provisioning → Aldric resolution → closing trap → border crossing.
- The border crossing is the win condition and is always physically walkable — the game never lies with an invisible wall. Its difficulty comes from an occupation cordon that is survivable only with Act-3 readiness. It is a scripted, bounded-turn gauntlet, not a boss. Surviving it lands the canonical ending (Klein reaches Novelborne) and fires epilogue seeds that connect to main-story canon (Corneo → Coneros, the Mercenary Graveyard filling), validating SM-1.
- Dialogue and quest delivery is text-forward and suspends the turn loop as a safe pause: no survival tick while a text surface is open (a choice may commit the turn it resolves). VOICE-gated (occasionally INS) choices route to success/failure branches. Quests start from NPC lines or discoveries by setting quest flags; killing a quest-giver voids that quest.

## Technical Decisions

- **Companion model (AD-10):** each active companion is a full tile-agent with its own Status/HP/debuffs/survival tracks and a seven-state behavior machine — FOLLOW / HOLD / HIDE / DISTRACT / FIGHT / RETREAT / FLEE. Combatants and non-combatants differ by *behavior set*, not entity depth. Companions are keyed by `bindId` (roster slot by name, never by list index).
- **Active vs. abstract roster:** the roster is four (Aldric = combat; Mara, Old Fen, Sister Yenna = non-combat). Only *one* companion is active and positioned on the map at a time; the other three are abstract `FlagStore`/Bond entries — they occupy no tile, emit no noise, and their survival does not tick until active. "Off the map" is a state transition, not a hidden on-map entity.
- **Single combat authority (AD-10 / AD-4):** `CombatSystem` is the sole owner of combat damage to *all* HP pools (player, companion, enemy). A companion's FIGHT issues actions through `CombatSystem` at the Companion-AI pipeline step; it never mutates any HP directly. Survival-track HP drain is a separate, named channel (the Hunger step), never a second combat mutator.
- **Party turn economy (AD-5):** the whole party shares one turn — companions act only on player-acted turns, inside the fixed pipeline (PlayerAction → Hunger → Detection → Companion AI → Enemy AI → Noise resolve → Last Stand → cleanup), never on their own clock.
- **Party-stealth penalty is noise, not a modifier (AD-9/AD-10):** a companion's stealth cost is expressed as `NoiseEvent`s emitted at its position when it moves or acts, consumed by `NoiseSystem.resolve` like any other noise — not a static DetectionSystem modifier. The DISTRACT order likewise emits a `NoiseEvent` to pull patrols.
- **Behavior is persistent state (AD-10 / AD-6):** a companion's current behavior and target serialize with the companion, so save/load resumes mid-behavior. Any new persisted `RunState`/companion field must ship with a deterministic default or a load-time reconcile (field-absent saves inherit nondeterministic ctor-rolled state, not a zero default — verify before adding a field).
- **Bond is per-companion (AD-7):** run-scoped narrative state (flags, quest state, act progression, Bond) lives in `FlagStore`; Bond is keyed per companion by `bindId` — a single shared Bond value is wrong for the four-member roster.
- **Act gating flips flags (AD-11):** act transitions are triggered by *story flags*, not an occupation-timer or exploration counter. Quest completion flips `FlagStore` flags; Epic 4's off-border escalation channel reads them.
- **Two-channel escalation — do not merge (AD-11):** (a) off-border Giliman presence *thickens* per act (owned by Epic 4); (b) the NW border cordon *thins* as acts advance because the war consolidates east. The cordon scales on a *subset* of the story flags. A uniform multiplier over all Gilimans is wrong — it would harden the win gate each act.
- **The border win (AD-12):** the win is reaching the NW border tile and surviving a bounded-turn scripted run against cordon channel (b), survivable with Act-3 readiness. The border is always walkable (permadeath honesty); the Deep Cave Mouth is a separate Region-2 threshold, not the exit.
- **Dialogue-safe-pause (AD-14):** any open text surface (dialogue tree, quest log) commits no turn and ticks no survival clock; the turn economy resumes only when the surface closes.
- **Layering (AD-1/AD-2/AD-3):** all logic and state live in the headless core (`com.margins.rogue`, `Companion` + `rogue/narrative/*`); the screen only reads state and emits `PlayerAction`s. No core class references a libGDX render/input type. `RunState` is the single authoritative owner.

## Cross-Story Dependencies

- Stories 5.2–5.5 depend on 5.1: the tile-agent, its HP/Status/survival state, and `bindId` keying must exist before behavior, orders, combat, and Bond can attach.
- Story 5.4 (companion FIGHT) depends on Epic 4's `CombatSystem` being the single HP authority; Story 5.3's DISTRACT reuses the Epic 4/Epic 1 noise-emit action.
- Stories 5.6–5.7 depend on the Epic 2 dialogue/quest foundation (text nodes, numbered choices, quest flags, safe-pause) and on Epic 3's world (act-gates are spatial pushes; the border crossing is a fixed canon landmark on the continuous map).
- Story 5.6's act-flag flips are consumed by Epic 4's off-border escalation channel (AD-11 channel a); Story 5.7's border cordon consumes AD-11 channel b — the two channels must stay separate.
- Open scope decisions to resolve within this epic (do not invent answers): whether a companion can truly die vs. only be captured/leave; whether party size allows two companions or a holdout support node; the Rescue-Aldric outcome (always rescuable / always lost / player-determined); companion stat granularity (full six-stat block vs. role-relevant subset).

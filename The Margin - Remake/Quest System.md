---
tags: [the-margin, game-design, systems, quest]
status: draft
---

# Quest System

Related: [[NPCs]] · [[World Structure System]] · [[Dialog System]]

## Overview
Freeform quest structure — no rigid categories (Fetch/Kill/etc.), each quest unique to its source. Two quest origin types: NPC-given (dialogue) and Discovery-triggered (found items/Journal Notes). Tracked via a Journal/Log for player reference. Specific quest content/lore to be defined separately under a future **Lore System** or the main story — this note covers mechanics only.

---

## Quest Sources

### NPC-Given
- Delivered and updated through direct NPC dialogue
- Established examples: [[NPCs#traveling-wanderer|Traveling Wanderer]] (lore + item rewards), [[NPCs#caravan-black-market-trader|Caravan Black Market Trader]] (VIP Ticket reward)

### Discovery-Triggered
- Reading a specific Journal Note or finding a specific item can auto-start a quest with **no NPC required**
- Fits environmental storytelling already established in [[World Structure System]] (e.g., mercenary company arc, Old House family thread)
- No living "giver" tied to these — cancellation rules differ from NPC-given quests (see below)

---

## Tracking

- **Journal/Log:** passive reference recording active quests — objective, source, relevant context
- No heavy HUD marker system — fits the game's turn-based, text-forward tone
- Delivery stays dialogue/discovery-driven; the Journal/Log is purely a lookup tool, not the quest-giving mechanism itself

---

## Cancellation Rules

- **NPC-given quests:** Universal rule — if the quest-giver dies, the quest is voided immediately, no partial credit (already established via [[NPCs#traveling-wanderer|Wanderer]]'s death rules)
- **Discovery-triggered quests:** No living giver to lose — failure conditions (if any) would need separate rules (time limits, wrong choices, etc.) — open item below

---

## Rewards

- Mix of Currency, Items, and pure narrative/lore payoff — reward type varies per quest, not every quest needs a material reward
- Established examples: Wanderer → lore/items, Black Market Trader → one-time VIP Ticket (exclusive shop access)

---

## Open Items
- [ ] Define failure conditions for Discovery-triggered quests (no giver to kill, but may still need fail-states)
- [ ] Specific quest content, chains, and rewards — deferred to future **Lore System** / main story design pass
- [ ] Decide if Discovery-triggered quests can be permanently missed (e.g., destroying a Journal Note before reading it) or are always recoverable
- [ ] Integrate with upcoming [[Dialog System]] for how NPC-given quests are actually presented/accepted in conversation

## Changelog
- Locked freeform quest structure (no formal categories)
- Established two quest origin types: NPC-given and Discovery-triggered
- Confirmed universal NPC-death-cancels-quest rule
- Confirmed Journal/Log as passive tracker, not quest-delivery mechanism
- Deferred all specific quest content to a future Lore System
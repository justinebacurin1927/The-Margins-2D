# The Margin — Design Summary (Region 1: Forest)

## 1. Hunger System

*Status tiers (countdown-based):*
- *Well Fed* — 350 turns. Buff: "Bloated" — small regen (+1 HP every 2-3 turns). Debuff: also "Bloated" causes a slow-effect (movement penalty) lasting 50 turns. Trade-off status: regen + slowness together.
- *Satisfied* — 250 turns. Starting status. No buff/debuff.
- *Hungry* — 250 turns. No effects yet, just a warning stage.
- *Starving* — 150 turns total, damages HP (-1 HP every 4 turns). Three stages:
  - *Stage 1 – Fatigue* (150→100 remaining): -35% max Strength. No eating-bonus.
  - *Stage 2 – Trembling* (100→50 remaining): -15% Agility. Eating reduces next status duration by 20%.
  - *Stage 3 – Rotting* (50→0 remaining): damage doubles (-3 HP every 2 turns). Eating reduces next status duration by 50% (e.g., recovering to Hungry gives 125 turns instead of 250).

Debuffs stack until eaten, cleansed, or death.

## 2. Debuff System

*Raw Meat / Bacterial Track (escalation chain — each stage replaces the previous, no stacking):*
1. *Nausea* — -30% Stamina/Strength, reduced regen. 30 turns. Base trigger chance varies by meat (Rabbit 30%, Chicken 40%, Half Rotten Meat 70%). 50%/turn chance to escalate to Fever if untreated.
2. *Fever* — -40% Strength/Stamina/Agility. 25 turns. Guaranteed escalation into Delirium at turn's end if untreated.
3. *Delirium* (3-in-1) — Paranoia (hallucinations, 40 turns), Vertigo (12% intended-direction success, 30 turns), Crippled (-50% move speed, 20 turns). Cure item cuts duration 75%.
4. *Diarrhea* (parallel track, not part of chain) — Stage 1: 2x Thirst/Stamina drain, 50 turns. Stage 2 (auto-trigger): 3x Thirst/Stamina/Hunger drain, 30 turns, becomes lethal if ignored. Severe Effects: 5-10% chance for extra -2 HP/5 turns stacked on top, until cured.

*Mushroom / Toxin Track:*
5. *Rotgut* — Nausea + Crippled + Diarrhea, instantly. 40 turns.
6. *Honeymoon → Collapse* — 60-turn hidden "feels fine" phase, then permanent Max HP cap at 40% unless cured during the window.
7. *Fevered Mind* — mild hallucination only, no stat drain. 50 turns.
8. *Alcohol-interaction toxin* — dormant, triggers only if Ale is consumed afterward; otherwise clears naturally at 100 turns.

Open items: "Venom" debuff (bites/stings) needs full spec; Mercenary Graveyard's undead curse effect still needs a name.

## 3. Food System

*Worn Down Kitchen Camp loot:* Molded Cheese, Ale, Bread, Half Rotten Meat, Sausage, Coal, Spoon, Fork

*Raw vs. Cooked (meats):*
| Meat | Raw | Cooked (needs Coal) |
|---|---|---|
| Rabbit | +60 turns, -2 HP, 35% Sick risk | +100 turns, +3 HP, no risk |
| Chicken | +70 turns, -2 HP, 40% Sick risk | +130 turns, +4 HP, no risk |
| Fish | +50 turns, -1 HP, 30% Sick risk | +90 turns, +2 HP, no risk |
| Half Rotten Meat | +90 turns, -5 HP, 60% Poison risk | +150 turns, -3 HP, 40% Poison risk (reduced, not removed) |

*Mushrooms:* Edible (safe, +40 turns), Spotted (mildly toxic, +50 turns/30% Sick), Pale Cap (deceptively toxic, +20 turns/70% Poison), Bloodvein (deadly, -5 HP/90% Poison, cures Bloated if survived)

*Berries:* Blueberries gathered in handfuls (+20 turns/handful), Strawberries picked individually (+15 turns each)

*Honey/Honeycomb:* Cure items for Sick/Poisoned status; Honeycomb yields more but riskier to obtain (bee aggression)

## 4. World Structure System (Region 1 — Forest)

Danger tiers: better loot = higher structural/creature risk.

*Tier 1 (Low Risk)*
1. *Hunter's Blind* — hidden tracking platform. Loot: Rope, small tools, 20% Map Fragment, Journal Note. Hazard: weak floor plank (minor fall).
2. *Fallen Log Hollow* — hollow tree, safe rest spot. Loot: Mushroom cluster, Berries, mosses. Hazard: snake/insect encounter (Venom debuff).
3. *Forest Shrine* — mossy stone shrine. Loot: Bread, Cheese, Journal Note. No hazard.
4. *Beehive Grove* — multi-hive cluster. Loot: 2-5x Honey/Honeycomb. Hazard: bee aggression scales with harvest (Swollen + Venom).

*Tier 2 (Medium Risk)*
5. *Worn Down Kitchen Camp* — abandoned mercenary camp (loot table above).
6. *Collapsed Watchtower* — mercenary lookout. Loot: early weapons (rusted knife/spear). Hazard: floor collapse (Crippled + Bleeding). Lore: ties to Kitchen Camp faction.
7. *Poacher's Camp* — active illicit hunting camp. Loot: extra small game, stolen goods. Hazard: player-set traps, 3-4 enemy NPCs.
8. *Sunken Well* — water source. Loot: water (future Thirst system), rare coin/items. Hazard: slip-and-fall, possible creature below.

*Tier 3 (High Risk)*
9. *The Old House* — crumbling civilian house. Loot: preserved food, cloth/bandages, locked chest/cellar, kitchen utensils, personal Journal Notes. Hazard: structural decay, locked cellar (needs tool/light). Lore: second, unrelated thread — a family connected to what drove mercenaries into the forest.
10. *Mercenary Graveyard* — burial site of the lost company. Loot: rare gear, coin, Ale, named unique items. Hazard: undead (curse debuff, name TBD) or wildlife. Lore: closes the Kitchen Camp/Watchtower story thread.
11. *Deep Cave Mouth* — gateway to Region 2. Loot: single degraded weapon/tool from a failed prior explorer. Hazard: guardian mini-boss (bear/wandering knight). Lore: teaser only.

## 5. NPCs

### Traveling Wanderer
- Former knight from the same mercenary company tied to Watchtower/Kitchen Camp/Graveyard.
- Trade: coin *or* barter, flexible.
- Offers side-quests (lore delivery, may send player to specific locations).
- Combat: peaceful by default, retaliates if attacked.
- Death: drops 2-3 randomized shop items *only if killed before reaching the 2-item purchase threshold*; no loot if killed after trading.
- Killing him applies a reputation debuff (temporary/variable duration, lockout from trading with him) and *cancels any active side-quest immediately*, no partial rewards.
- Spawn: random rotation across Tier 1/2 locations, limited-time appearances.

### Caravan Black Market Trader
- Representative of a larger Black Market organization (not a lone operator).
- Guaranteed to spawn in every region (unlike Wanderer's random chance).
- Trade: coin only, no barter, no exceptions.
- Stock: debuff cures, upgraded weapons/tools, possibly stolen goods (link to Poacher's Camp).
- Guarded by a fixed team: 2 Knights, 2 Mystic-Users, 1 Magician.
- Death: no loot ever — magically sealed/protected.
- Defeating the entire guard team triggers *permanent* Loss Reputation: locked out of all Black Market trading forever, and 1-2 organization hunter NPCs spawn on every run indefinitely.
- Side-quest reward: one-time VIP Ticket — teleport access to a hidden shop with *exclusive rare items* not available in the regular stock.

## 6. Currency System (adapted from Evermove worldbuilding doc)

*Four-tier denomination:*
| Currency | Conversion |
|---|---|
| Copper Coin | Base unit |
| Silver Coin | 25 Copper = 1 Silver |
| Gold Coin | 10 Silver = 1 Gold |
| Royal Gold Plaque | 1,000 Gold = 1 Royal Gold Plaque (essentially mythical, late-game/other regions) |

- Sources: Tier 2/3 loot drops (mostly Copper, occasional Silver), selling loot back to traders.
- Sinks: Black Market Trader (Gold-tier pricing, coin-only), Wanderer (Copper/Silver or barter), future region shops.
- Coin is weighted like any other item — carrying currency has an inventory cost, same as raw loot.
- Open question: whether The Margin's Forest Region exists within the Evermove kingdom itself (possibly Region E/Herois, given matching "poor forest region" tone) or is a separate setting reusing the same currency convention.

---
Open threads to resolve later: Venom debuff spec, undead curse name, magic system scope beyond the Black Market Trader's guards, Old House vs. Kitchen Camp utensil overlap, coin stack/weight compression rules.

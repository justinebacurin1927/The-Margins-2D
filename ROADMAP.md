# Roadmap

The project targets an SPD-style turn-based roguelike. See [DESIGN.md](DESIGN.md) for narrative design and [ARCHITECTURE.md](ARCHITECTURE.md) for technical architecture. Below is the mechanical build roadmap.

## Phase 0 — Strip to SPD Core ✅

- [x] Remove real-time systems (smooth movement, day/night, particle follower)
- [x] Convert to turn-based: wait for input → process turn
- [x] Procedural floor generation (rooms + corridors via BSP)
- [x] Tile types: wall, floor, door, stairs
- [x] Player grid movement (1 tile per turn, WASD + arrows)
- [x] Basic enemy with chase AI
- [x] Turn-based combat (melee, low numbers)
- [x] Simple HUD (HP, hunger, floor depth, messages)
- [x] Defense mechanics: dodge (instinct), armor (grit), block (E key)
- [x] Arrival grace (enemies don't hit on approach turn)
- [x] Enemy HP bars (ShapeRenderer overlay)
- [x] Wait action (SPACE — pass turn, enemies move)
- [x] Stairs down → next floor
- [x] Permadeath + restart
- [x] Temple dungeon tileset (CraftPix) replacing colored rects
- [x] Cultist enemy sprites replacing orc sprites
- [x] UI icons + pixel number rendering

## Phase 1 — SPD Mechanics

- [ ] Hunger system with starvation (base exists, needs items)
- [ ] Identify-by-use items (potions, scrolls, food) — see [DESIGN.md: Identify-by-Use](DESIGN.md#identify-by-use-reframed)
- [ ] Inventory system (backpack + equipped slots)
- [ ] Field of view / fog of war (shadowcasting)
- [ ] Equipment: weapons, armor (degradation optional)
- [ ] Multiple floor navigation (stairs down + up)
- [ ] Item generation per seed

## Phase 2 — Depth Systems

- [ ] Multiple enemy types (melee, ranged, boss variants)
- [ ] Status effects (bleed, poison, weaken, freeze)
- [ ] Alchemy / crafting
- [ ] Shops / merchants on safe floors
- [ ] Well of health / upgrade
- [ ] Permadeath + Last Stand mechanic — see [DESIGN.md: Decisions Locked In](DESIGN.md#decisions-locked-in)
- [ ] Save/load (seed + state serialization)

## Phase 3 — The Margins Content

### Phase 3a — Story Systems (foundation)

- [ ] Dialog system with branching choices (DialogNode exists, needs wiring)
- [ ] Quest system (QuestManager exists, needs wiring)
- [ ] Companion Bond system — active slots (max 2) + bench passives
- [ ] Theodore Trust Meter — hidden stat, shifts by dialogue choices
- [ ] Route unlock system (story-progress gated, not floor depth)

### Phase 3b — Route: The Caravan Road (tutorial)

- [ ] Opening quest: "Five Nights, Again" — scavenger NPC, 3-choice dialogue
- [ ] 3-5 procedural stealth floors, teaches identify-by-use + hunger
- [ ] Identify-by-use item set: Wrapped Bundle, Sealed Waterskin, etc.
- [ ] Story floor: Galleon recruitment — see [DESIGN.md: Galleon Recruitment](DESIGN.md#galleon-recruitment-scene--the-pack-comes-home)
- [ ] Galleon joins as bondable companion (tanky, draws aggro)
- [ ] Branching choice: solo Theodore detour vs fast-track to Coneros

### Phase 3c — Route: Dawnbury Undercity

- [ ] Faction-based floors (Western nobles vs Eastern faction vs neutral)
- [ ] Theodore's political war narrative
- [ ] Mid-campaign resolution, Galleon's promise comes due

### Phase 3d — Route: The Blackberry Troupe's Ledger

- [ ] Investigation-style floors
- [ ] Troupe's true objectives revealed
- [ ] Trust Meter consequences

### Phase 3e — Route: Mirko Pass, Reprise (optional/hard)

- [ ] Spearshot's fate, wolf-transformation aftermath
- [ ] Galleon's Alpha Wolf transformation unlock
- [ ] Companion Pietro (stealth/ranged) and Maru (support/medic)
- [ ] Non-combat utility companions: Perfect, Rooster, Charcoal

### Phase 3f — Route: Return to Coneros (endgame)

- [ ] Porter's camp, Erik reunion — see [DESIGN.md: The Erik Reunion](DESIGN.md#the-erik-reunion--charlie-porter)
- [ ] Central forced choice: Porter's squire vs Galleon's promise vs alone
- [ ] Randy confrontation
- [ ] Multiple endings resolved by Bond + survival + route path

## Phase 4 — Polish

- [ ] Additional sprite passes (more enemy types, items, effects)
- [ ] Sound effects and music
- [ ] Title screen with animated background
- [ ] Tutorial / tooltip system
- [ ] Mobile touch controls
- [ ] HTML5 build target

# Architecture

## Overview

```
DesktopLauncher (main)
       │
       ▼
  MarginsGame (extends Game)
       │
       └── RogueGameScreen (current gameplay)
                │
         ┌──────┼──────────┐
         ▼      ▼          ▼
    FloorGen  Player     Enemies
    (BSP)     (stats,    (AI,
              combat)    combat)
         │
         ▼
      TileMap
      (grid,
       walls/floors/
       doors/stairs)
```

## Current Package Layout

```
com.margins
├── MarginsGame.java           # Game entry point (switches screens)
├── rogue/                     # Current roguelike dungeon systems
│   ├── RogueGameScreen.java   # Main gameplay: input, rendering, HUD
│   ├── RoguePlayer.java       # Milek: HP, hunger, stats, combat
│   ├── RogueEnemy.java        # Enemy: HP, chase AI, combat
│   ├── RogueTileMap.java      # 2D grid of tile types
│   ├── RogueTile.java         # Constants: WALL, FLOOR, DOOR, STAIRS
│   └── FloorGenerator.java    # BSP dungeon generation
├── screen/                    # Legacy overworld screens (not used)
│   └── GameScreen.java, TitleScreen.java
├── entity/                    # Legacy overworld entities (not used)
│   └── Entity.java, Player.java, NPC.java, Decoration.java
├── dialog/                    # Dialog system (not yet wired)
│   └── DialogNode.java
├── quest/                     # Quest system (not yet wired)
│   └── QuestManager.java, Quest.java, QuestObjective.java
├── map/                       # Legacy overworld map (not used)
│   └── TileMap.java
├── item/                      # Legacy item system (not used)
│   └── Item.java, Inventory.java
├── fx/                        # Particles (not yet wired)
│   └── ParticleSystem.java
└── asset/
    └── Assets.java            # Texture loading, Pixelmap cropping
```

## Core Systems

### Turn System
The game waits for player input, commits one player action, then processes all enemy turns in the same frame. No action queue — immediate resolution.

**`RogueGameScreen.handleInput()` flow:**
1. Read keypress (WASD/arrows = move, Q = attack, E = block, SPACE = wait)
2. Set player facing direction
3. Execute action (attack or movement or nothing for wait/block)
4. `acted = true` triggers:
   a. Player hunger tick
   b. All alive enemies take a turn (move toward player, attack if adjacent)
5. Render updated world + HUD
6. Wait for next input

### Floor Generation (`FloorGenerator.java`)
- BSP-based room placement
- Rooms connected by L-shaped corridors
- Door placed at corridor-room intersections
- Enemies spawn in non-start rooms (1-2 per room)
- Stairs down placed in last room
- Map size: 50x50 tiles
- `floorDepth` parameter affects spawn difficulty

### Tile Types (`RogueTile.java`)
```java
EMPTY = 0    // void/unreachable
WALL  = 1    // solid, blocks movement + vision
FLOOR = 2    // walkable
DOOR  = 3    // walkable
STAIRS_DOWN = 4
STAIRS_UP = 5
```

### Combat System

**Player stats (RoguePlayer):**
- HP: 20 max
- Hunger: 80→0 (starvation at 0: -1 HP/turn)
- STR: 5 (attack damage)
- INSTINCT: 7 (dodge rate = 3%/pt = 21%)
- GRIT: 5 (armor = grit/2 = 2 flat reduction)
- VOICE: 3 (unused)

**Enemy stats (RogueEnemy):**
- HP: 8 max
- Damage: 3 per hit
- AI: move toward player (x-first then y), avoid walls

**Defense mechanics:**
- **Armor**: `max(1, incomingDamage - grit/2)`
- **Dodge**: `random(100) < instinct * 3` → negates damage
- **Block**: Press E, next hit halved after armor: `max(1, armoredDamage/2)`
- **Arrival grace**: Enemy that just moved adjacent doesn't attack on arrival turn

### Enemy AI (`RogueEnemy.takeTurn()`)
1. If not alive, skip
2. Move toward player: try x-direction first, then y-direction
3. Don't walk into player's tile or through walls
4. If movement results in adjacency, set `justArrived = true` (grace flag)
5. On player's next action turn, enemies process in order:
   - If `justArrived`: clear flag, skip attack
   - If adjacent: roll player dodge → if fail, apply damage (armor + block)
   - If not adjacent: call `takeTurn()` to move

### Rendering Pipeline

1. Clear screen (black)
2. Set camera to player position (32px tiles, camera snaps to player)
3. Batch render: tiles in view → enemies → player
4. ShapeRenderer: enemy health bars (32×4, green→red)
5. HUD batch: HP icon + number, hunger icon + number, floor label, messages, controls
6. Death screen overlay (if game over)

### Viewport
- **Internal resolution**: 640x480 (`FitViewport`)
- **Tile size**: 32x32 pixels
- **Sprite size**: 64x64 pixels (player, enemies drawn offset -16,-32)
- **Camera**: centered on player

## Asset Loading (`Assets.java`)

Textures loaded from `sprites/` directory:

| Asset | Source | Coordinates |
|-------|--------|-------------|
| `tileFloorTex` | `temple/PNG/Walls_floor.png` | (29, 96) 32×32 |
| `tileWallTex` | `temple/PNG/Walls_floor.png` | (45, 176) 32×32 |
| `tileDoorTex` | `temple/PNG/Walls_floor.png` | (93, 208) 32×32 |
| `rogueEnemyTex` | `temple/PNG/Cultist1_Idle.png` | (0, 0) 64×64 |
| `milekSouth/North/West/East` | `swordsman/.../Swordsman_lvl1_Idle_with_shadow.png` | 64×64 each direction |
| `iconHp` | `ui/PNG/Icons.png` | (66, 130) 24×24 |
| `iconHunger` | `ui/PNG/Icons.png` | (34, 226) 24×24 |
| `numSmall/numMed` | `ui/PNG/Numbers.png` | 16×7 / 16×8 per digit |

## Sprite Atlas Structure

### Walls_floor.png (176×256)
Content area starts at (13, 16):
- Band 1 (y=16-79): Wall tiles — 4 rows of 16×16 tiles
- Band 2 (y=96-143): Floor tiles — 3 rows of 16×16 tiles
- Band 3 (y=156-239): Misc tiles (shadows, variations) — ~5 rows

Tile colors:
- DARK walls: (45,176): avg (49,43,42) — used for walls
- LIGHT floors: (29,96): avg (165,157,143) — used for floors
- TAN doors: (93,208): avg (173,146,111) — used for doors

### Cultist Sprites (384×128)
- 6 columns × 2 rows of 64×64 frames
- Row 0: facing down animation frames
- Row 1: facing up animation frames

## Planned Additions
- Field of view / fog of war (shadowcasting)
- Identify-by-use items
- Equipment system
- Multiple enemy types
- Status effects
- Dialog/quest system wiring
- Companion system
- Save/load

# The Margins 2D

A turn-based roguelike dungeon-crawler built with **Java 17** and **libGDX 1.12.1**, mechanically based on **Shattered Pixel Dungeon**. Follow **Milek of Coneros** as he searches for Galleon, the Pack, and the truth about what happened to Erik.

> **Current status:** Turn-based dungeon crawling working. Combat, hunger, procedural floors, and defense mechanics implemented. Art from CraftPix sprite packs.

## Prerequisites

- Java 17+
- Apache Maven 3.6+

## Quick Start

```bash
mvn clean compile exec:java -pl desktop
```

## Current Features

| Feature | Status |
|---------|--------|
| Turn-based grid movement (WASD + arrows) | Done |
| Procedural floor generation (rooms + corridors) | Done |
| Turn-based combat (low numbers, tactical) | Done |
| Hunger system | Done |
| Enemy AI (chase, pathfind) | Done |
| Defense mechanics (dodge, block, grit armor) | Done |
| Enemy health bars | Done |
| Temple dungeon tileset (CraftPix) | Done |
| Cultist enemy sprites (CraftPix) | Done |
| UI icons + numbers (CraftPix) | Done |
| Pixel-art digit rendering | Done |
| Stairs down progression | Done |
| Permadeath / game over | Done |
| Field of view / fog of war | Planned |
| Identify-by-use items | Planned |
| Equipment / degradation | Planned |
| Save/load | Planned |

## Project Structure

```
The Margins 2D/
├── core/src/main/java/com/margins/
│   ├── MarginsGame.java         # Game entry point
│   ├── rogue/                   # Roguelike dungeon systems
│   │   ├── RogueGameScreen.java # Main gameplay screen
│   │   ├── RoguePlayer.java     # Player entity (stats, combat)
│   │   ├── RogueEnemy.java      # Enemy entity (AI, combat)
│   │   ├── RogueTileMap.java    # Grid-based tile map
│   │   ├── RogueTile.java       # Tile type constants
│   │   └── FloorGenerator.java  # Procedural BSP generation
│   ├── screen/                  # Legacy overworld screens
│   ├── entity/                  # Legacy overworld entities
│   ├── dialog/                  # Branching dialog system
│   ├── quest/                   # Quest system framework
│   ├── map/                     # Legacy overworld map
│   ├── item/                    # Legacy item/inventory
│   ├── fx/                      # Particle effects
│   └── asset/Assets.java        # Texture loading & management
├── desktop/                     # Desktop launcher (LWJGL3)
├── sprites/                     # All pixel art assets
│   ├── swordsman/               # Milek sprites
│   ├── temple/                  # Ruined temple dungeon pack
│   ├── orc/                     # Orc enemy pack
│   ├── trees/                   # Tree sprites
│   ├── tileset/                 # Road tileset (TMX)
│   └── ui/                      # UI icons, numbers, panels
├── DESIGN.md                    # Full game design document
├── ARCHITECTURE.md              # Architecture documentation
├── ROADMAP.md                   # Development roadmap
└── pom.xml                      # Maven multi-module build
```

## Controls

| Key | Action |
|-----|--------|
| WASD / Arrows | Move 1 tile (turn-based) |
| Q | Attack in facing direction |
| E | Brace — halve next incoming hit (costs a turn) |
| SPACE | Wait — pass turn, enemies move |
| R | Restart (on death screen) |
| Q | Quit (on death screen) |

## Combat System

- **Grit armor**: `grit / 2` flat damage reduction (grit=5 → -2 damage, min 1)
- **Instinct dodge**: 3% per instinct point (instinct=7 → 21% dodge chance)
- **Block**: Press E to brace; next hit deals half damage after armor
- **Arrival grace**: Enemies that just moved adjacent don't attack on arrival
- **Enemy HP bars**: Visual green→red bar above each enemy
- All damage displayed as messages in HUD ("Hit for 2!", "Dodge!", "Brace! Blocked 3→1")

## Tech Stack

- **libGDX 1.12.1** — cross-platform game framework
- **LWJGL3** — desktop backend
- **Maven** — build system

## Sprite Assets

All sprites from [CraftPix.net](https://craftpix.net) free/purchased packs:
- Ruined Temple Top-Down Location (walls, floors, cultists, objects)
- Orc Warriors (enemy variants)
- Top-Down UI Elements (icons, numbers, panels)
- Road Tileset (Tiled TMX format)
- Swordsman (Milek player character)
- Trees and foliage

## The Full Vision

See [DESIGN.md](DESIGN.md) for the complete game design — routes, character system, companions, narrative structure, and story.

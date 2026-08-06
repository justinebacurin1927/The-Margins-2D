# The Margin

A text-forward, **SPD-style 2D survival roguelike** built with **Java 17** and **libGDX**. You play **Klein**, a young Novelborne knight whose quiet border posting falls on the first morning of an Evermove invasion. Stripped of rank and cut off from home, he survives the occupied **Herois** forest with one goal: **get home** — across the treeline, to his fiancée and the ordinary life the war tried to erase.

Set ~50–60 years *before* the events of *The Margins*, in the same universe.

> **Status:** design complete; the codebase has been **stripped to its reusable core** and is being rebuilt. The app compiles and runs (a blank shell) while the new game is built on top.

## Design Bible

The complete design lives in [`The Margin - Remake/`](The%20Margin%20-%20Remake/) — start with the numbered index:

**→ [`00 - INDEX`](The%20Margin%20-%20Remake/00%20-%20INDEX.md)** — world, protagonist, story, gameplay, and all systems, ordered for a BMAD workflow.

## Current Codebase

The old prototype's art (~30 MB) and render layer were removed; the **reusable, headless, tested core** remains under `com.margins.rogue`:

```
core/src/main/java/com/margins/
├── MarginsGame.java        # minimal runnable shell (blank screen)
├── rogue/
│   ├── system/             # TurnEngine, Combat, Hunger, Detection, Fov, Noise, PlayerAction
│   ├── state/              # RunState, FlagStore, IdentifyMap
│   ├── save/               # SaveService (JSON serialization)
│   ├── item/               # Inventory, Supply, TrueIdentity, FloorItem
│   ├── narrative/          # DialogController, SceneEffects
│   ├── world/              # Route
│   ├── RoguePlayer · RogueEnemy · RogueTileMap · RogueTile · FloorGenerator
│   └── Companion · Detection · NoiseEvent
└── dialog/DialogNode.java
desktop/                    # LWJGL3 launcher
```

- **50 passing tests** cover the kept systems.
- These are **scaffolding**: models still carry old-design shapes (e.g. `RoguePlayer` has 4 stats, not Klein's six) and will be reworked per the bible.

## Prerequisites
- Java 17+
- Apache Maven 3.6+

## Build & Run
```bash
mvn -pl core test                          # run the test suite
mvn clean compile exec:java -pl desktop    # run (blank shell for now)
```

## Tech Stack
- **libGDX** (LWJGL3 desktop backend) · **Java 17** · **Maven**

---

*History: this repo began as "The Margins 2D," an SPD-style dungeon crawler starring Milek. It has been repurposed into **The Margin**, a survival roguelike starring Klein, set in the same world ~50–60 years earlier. The old prototype remains in git history.*

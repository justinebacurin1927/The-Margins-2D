# Session 1 — Combat, Defense, Tiles, Docs, Git

## What was built

- **Asset swap**: Temple pack (CraftPix) replaces colored rects and road tileset. Wall/floor/door tiles from `Walls_floor.png`, cultist enemy sprites.
- **Tile contrast fix**: Walls (dark grey avg 49), floors (light beige avg 165), doors (tan avg 173) — now visually distinct.
- **Combat defense**: Grit armor (flat -2 dmg), instinct dodge (21%), E key block (halves hit). `takeDamage()` returns final dealt.
- **Arrival grace**: Enemies that just moved adjacent skip attack on arrival turn.
- **Enemy HP bars**: 32×4 green→red bars via ShapeRenderer.
- **SPACE wait**: Pass turn, enemies move, shows "Wait".
- **Message feedback**: "Dodge!", "Brace! Blocked 3→1", "Hit for 1!", "Wait" — no more overwrites.
- **Obsidian sync**: README, Architecture, Roadmap updated in vault with frontmatter + wikilinks.
- **Git init**: `git@github.com:justinebacurin1927/The-Margins-2D.git` — 1,399 files, 110MB.

## Controls

| Key | Action |
|-----|--------|
| WASD / Arrows | Move |
| Q | Attack |
| E | Block (brace) |
| SPACE | Wait |

## What needs doing next

Probable next steps: field of view / fog of war, stairs-down floor transitions, items/inventory, identify-by-use system.

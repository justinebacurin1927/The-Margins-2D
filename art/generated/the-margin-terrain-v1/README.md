# The Margin Terrain v1

Guide-compliant runtime terrain generated from Asset 1 in `art/ASSET-GENERATION-GUIDE.md`.

- `runtime/terrain-atlas.png` — 96x96, 4x4, exact 24px cells
- `runtime/00-*.png` through `runtime/12-*.png` — thirteen fully opaque terrain cells
- `runtime/13-*.png` through `runtime/15-*.png` — transparent empty cells
- `preview/terrain-atlas.png` — nearest-neighbor 8x preview
- `source/terrain-chroma.png` — untouched generated source
- `transparent/terrain.png` — full-resolution keyed source

The two wall-tree cells, wall-rock cell, door, and well include an opaque matching ground
underlay. They are replacement terrain tiles, not transparent props, so the game's black clear
color cannot show around them.

## Row-major cell order

1. Grass A, grass B, dirt path, leaf litter
2. Pine wall A, pine wall B, rock wall, wooden door
3. Stone well, pond A, pond B, river A
4. River B, empty, empty, empty

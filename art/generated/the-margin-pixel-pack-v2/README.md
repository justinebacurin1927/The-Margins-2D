# The Margin Pixel Pack v2

Low-resolution replacement art based on the compact pixel density and simplified readability of
the user-provided reference. This pack is intentionally much smaller and less detailed than v1.

## Runtime masters

- `runtime/characters-atlas.png` — 64x64; 4x4 cells; each character is 16x16
- `runtime/items-atlas.png` — 128x80; 8x5 cells; each item is 16x16
- `runtime/environment-tiles-atlas.png` — 128x128; 8x8 cells; each tile is 16x16
- `runtime/world-structures-atlas.png` — 128x96; 4x3 cells; each structure is 32x32
- Matching folders under `runtime/` contain every cell as an individual PNG.
- `preview/` contains nearest-neighbor 8x previews of the runtime atlases.

## Source layers

- `source/` preserves the untouched flat-magenta generated images.
- `transparent/` preserves full-resolution transparent versions.
- `pack_runtime.py` trims each generated cell and reproducibly rebuilds the runtime atlases.

Run `python pack_runtime.py` from any working directory to regenerate `runtime/` and `preview/`.

## Environment order

The 8x8 environment atlas is row-major:

1. Pine grass x4, moss grass x2, forest dirt x2
2. Dirt-path horizontal, vertical, four corners, T-junction, cross
3. Short grass, tall grass, fern, bush, berry bush, flowers, mushrooms, reeds
4. Young pine, full pine, old pine, dead tree, stump, fallen log, log pile, roots
5. Pebble, small rock, mossy boulder, rock cluster, cracked earth, mud, leaf litter, pine needles
6. Still pond, rippled pond, shallow water, current, muddy bank, grassy bank, stepping stones, puddle
7. Fence horizontal, vertical, corner, gate, sign, snare, campfire, cold ashes
8. Shadow grass, cold grass, rain-dark grass, blood-marked grass, bones, tracks, broken branch, empty

## Integration note

The pack is saved separately and does not overwrite `assets/characters.png`, `assets/items.png`,
`MarginScreen.java`, or `PixelPack.java`. To adopt v2, copy the runtime character/item atlases into
the application assets and extend the renderer to load the environment atlas.

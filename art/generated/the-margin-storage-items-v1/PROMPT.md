# Storage-item Journal atlas v1

Built-in image-generation source for `assets/generated/journal-storage-v1.png`.

## Runtime layout

- Source order: 4 columns × 3 rows, row-major.
- Runtime atlas: 128 × 96 PNG with alpha; 32 × 32 per cell.
- Nearest-neighbor filtering in `PixelPack`.

| Cell | Item |
|---:|---|
| 0 | Woven Pouch |
| 1 | Hunter's Satchel |
| 2 | Mercenary's Rucksack |
| 3 | Poacher's Game Bag |
| 4 | Family's Trunk |
| 5 | Traveler's Pack |
| 6 | Reinforced Backpack |
| 7 | Expedition Pack |
| 8 | Potion Bandolier |
| 9 | Scroll Holder |
| 10 | Vien's Dimensional Pocket |
| 11 | Faahard's Oblivion Blade |

## Final prompt

Create exactly twelve distinct storage-item sprites in an exact four-column by three-row
atlas, in the order above. Use strict deliberately low-resolution pixel art: every sprite
looks authored on a 24 × 24 logical pixel grid, enlarged with nearest-neighbor hard square
pixels. Use chunky color clusters, hard one-logical-pixel near-black outlines, four to seven
flat colors for ordinary items, and abrupt one-step highlights. Legendary items may add only
two restrained glow colors. Keep one isolated centered complete sprite per equal cell, with
identical visual scale, clear padding, no overlap, no cell borders, and a front-facing
three-quarter inventory-icon view. Use bark brown, tan canvas, muted olive, dull iron, and
restrained amber; reserve violet/cyan for Vien's pocket. Render on a perfectly flat uniform
`#ff00ff` chroma-key background. No text, labels, numbers, UI, shadows, floor, gradients,
antialiasing, photorealism, or 3D rendering.

Distinctive subject cues were supplied for every item: crude woven construction; hunter
feather/snare; mercenary blanket and iron buckles; poacher meat compartment and hook; old
wooden family trunk; travel bedroll/bottle/scroll loops; riveted reinforcement; organized
expedition pockets and rope; separated potion vials; cylindrical scroll case and parchment;
an impossible star-dark pocket; and a blackened inventory blade drawing in amber loot motes.

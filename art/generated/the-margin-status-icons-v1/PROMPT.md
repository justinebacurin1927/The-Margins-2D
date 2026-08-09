# Survival Status Icons v1 — final prompt

Mode: built-in image generation, using the gameplay screenshot only as a compact HUD scale and
readability reference. The source was chroma-keyed locally and normalized into nine 16x16 cells.

```text
Use case: stylized-concept
Asset type: production source for a 3-by-3 runtime status-icon atlas in The Margins 2D, a dark
medieval forest survival roguelike.
Input image: use the attached gameplay screenshot only to understand the compact survival-chip
scale, current pixel-art density, and dark HUD contrast. Do not reproduce its layout or content.
Primary request: exactly nine distinct icons in an exact 3-column by 3-row atlas.
Row 1: food (bread ration), water (single droplet), temperature (old thermometer).
Row 2: nausea (queasy stomach and curl), fever (head with heat waves), delirium (spiral eye).
Row 3: diarrhea (tasteful gut and downward droplet), crippled (broken boot/lower leg), collapsed
(fallen human silhouette reduced to a readable status symbol).
Style: crisp deliberately low-resolution game-UI pixel art, chunky clusters, hard pixel edges,
no antialiasing. Use only near-white highlight, pale-gray midtone, and charcoal inner shadow so
the engine can tint the sprites. Strong closed silhouettes, readable at 16x16.
Composition: exact evenly spaced 3x3 atlas, one centered icon per equal square cell, consistent
12-to-14-pixel-looking subject scale, equal padding, no overlap, borders, or grid.
Backdrop: perfectly flat uniform #ff00ff chroma key; no variation and no #ff00ff inside icons.
Constraints: no text, labels, numbers, UI panels, frames, logos, watermark, cast shadows, soft
edges, emoji style, realistic anatomy, or extra symbols.
```

## Outputs

- Chroma source: `source/status-icons-chroma.png`
- Transparent full-resolution source: `transparent/status-icons.png`
- Runtime atlas: `runtime/status-icons.png` (48x48; nine 16x16 cells)
- Integrated game asset: `assets/status-icons.png`

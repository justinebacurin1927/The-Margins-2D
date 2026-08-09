# The Margin Ground Materials v1

## Final generation prompt

```text
Use case: stylized-concept.
Create a production-source raster atlas for a dark medieval forest survival roguelike, matching the crisp hand-made low-resolution pixel-art look of classic top-down dungeon crawlers.

Asset: EXACTLY a 4 by 4 grid of sixteen seamless, top-down, floor-only ground-material tiles. Every cell is equal and square. Every tile fills its cell edge-to-edge.

Cell order:
Row 1: quiet forest grass A; quiet forest grass B; sparse flowering grass; damp dark grass.
Row 2: dry leaf litter A; dry leaf litter B; rooty forest floor; fine gray-brown gravel.
Row 3: packed dirt A; packed dirt B; churned wet mud; shallow puddled mud.
Row 4: old irregular cobblestone; mossy cobblestone; broken cobble-to-gravel transition; rain-darkened cobblestone.

Style: deliberately low-resolution pixel art, chunky pixel clusters, hard clean edges, absolutely no antialiasing or smoothing, restrained detail that remains readable when each tile is reduced to 24x24 pixels, 2-4 main shades per material. Moody palette of pine green, olive grass, bark brown, wet earth, muted amber leaves, and cool gray stone. Consistent overhead lighting. Natural organic textures without diagonal stripes, checkerboards, obvious symmetry, or large centered motifs.

Technical composition: exact straight-on orthographic top-down view; seamless/tileable on all four sides per cell; no perspective; no gaps; no gutters; no borders; no grid lines; no frames; no labels. Opaque square atlas, no transparency.

Do not include characters, creatures, items, flowers larger than a few pixels, trees, bushes, walls, doors, buildings, torches, UI, text, icons, shadows from tall objects, water bodies, magenta, lighting gradients, or decorative presentation backgrounds.
```

## Output and normalization

- Generated with the built-in image-generation tool as a new raster asset.
- Source output: `source/ground-materials-source.png` (1254x1254).
- Runtime output: `../../../assets/ground-materials.png` (96x96).
- Normalization: the exact 4x4 source grid was cropped cell-by-cell, each cell was reduced to native 24x24 with nearest-neighbor sampling, then the sixteen cells were reassembled without gutters.
- Runtime cell order is row-major and matches the prompt above.


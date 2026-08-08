# Terrain generation prompt

Generated with the built-in image tool using these references:

- `The Margin - Remake/VIsion .png` — presentation, forest mood, and scale reference
- `The Margin - Remake/Map.png` — Herois palette and medieval-setting reference
- User-provided tiny-pixel screenshot — target density, block size, and simplification reference

```text
PROJECT: The Margin — dark medieval forest survival roguelike, top-down grid, 24px runtime tiles.
Use case: stylized-concept
Asset type: runtime-ready seamless top-down terrain tile sheet
Primary request: Produce the dedicated 4x4 terrain sheet defined by art/ASSET-GENERATION-GUIDE.md. Correct the current black-square problem: EVERY USED TILE must be a completely opaque square, filled edge-to-edge. Trees, rocks, the door, and the well must include their own matching grass/dirt ground underlay in all corners. They are full replacement terrain tiles, NOT transparent overlay props.
Input references: Image 1 is only presentation/forest-mood/scale guidance; do not copy its UI, sprites, structures, or layout. Image 2 is only Herois palette and medieval-setting guidance; do not copy its map, labels, borders, or icons. Image 3 is the target tiny-pixel density, chunky blocks, minimal shading, clean tile geometry, and compact readable style; do not copy its dungeon or characters.
Subjects: exactly 16 equal tiles in row-major order.
Row 1: floor grass A; floor grass B with subtle variation; floor dirt path; floor leaf litter.
Row 2: wall pine tree A on a full grass ground square; wall pine tree B on a full grass ground square; wall mossy large rock on a full grass ground square; closed wooden door/gate in a walkable full dirt-and-grass ground square.
Row 3: stone well ring on a full grass ground square; pond water A filling the whole square; pond water B filling the whole square; river water A filling the whole square.
Row 4: river water B filling the whole square; empty transparent cell; empty transparent cell; empty transparent cell.
Pixel specification: author at apparent 48x48 per tile for clean nearest-neighbor reduction to 24x24. Crisp deliberately low-resolution pixel art, chunky clusters, hard 1-pixel edges, no antialiasing, 2-3 shades per material, no micro-detail. Tile-edge pixels must meet seamlessly in every direction. All used cells 0-12 must touch all four cell boundaries and have NO transparent or black corners.
Palette: moody pine green, bark brown, wet-stone gray, muted amber; compatible with the existing v2 characters and items.
Composition: exact 4 columns x 4 rows, equal square cells, no grid lines, no labels, no overlap. Used cells are fully opaque square tiles; only cells 13-15 use the flat key background.
Backdrop: perfectly flat solid #ff00ff chroma-key ONLY in the three empty cells and outside the tile sheet; no gradients, shadows, floor plane, reflections, or texture in the key.
Constraints: no text, labels, numbers, UI, logos, watermark, copied designs, cast shadows, black background squares, transparent corners in used tiles, isolated floating trees, or isolated floating doors.
```

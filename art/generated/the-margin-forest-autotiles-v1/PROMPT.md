# Forest-autotile generation prompt

Generated with the built-in image tool. References were the current terrain atlas, the
user's connected forest-border example, and the requested compact pixel-density example.

```text
Use case: stylized-concept
Asset type: runtime-ready top-down forest-wall autotile sheet for a 24px-grid dark medieval survival roguelike.

Primary request: Create a companion 4 columns x 4 rows atlas of exactly sixteen fully opaque square forest-wall terrain tiles. The game selects a cell using a four-neighbor bitmask, so every boundary must connect cleanly. Form one massive, continuous, impassable treeline around grassy clearings, not isolated tree sprites.

Cell semantics: a CLEAR side means the grassy clearing touches that side; all other sides continue into dense forest.
Row 1: no clear sides / clear NORTH / clear EAST / clear NORTH+EAST.
Row 2: clear SOUTH / clear NORTH+SOUTH / clear EAST+SOUTH / clear NORTH+EAST+SOUTH.
Row 3: clear WEST / clear NORTH+WEST / clear EAST+WEST / clear NORTH+EAST+WEST.
Row 4: clear SOUTH+WEST / clear NORTH+SOUTH+WEST / clear EAST+SOUTH+WEST / clear ALL FOUR SIDES.

Forest-continuing sides use dense dark overlapping canopy touching the whole edge. Clear sides use matching grass reaching the edge, with dark under-canopy, clustered shrubs, and warm brown trunks along the organic boundary. Interior and corners read as a solid woodland mass. Every cell has its own opaque ground/background.

Pixel specification: apparent 48x48 authoring scale for nearest-neighbor reduction to 24x24; crisp low-resolution pixel art; chunky clusters; hard edges; no antialiasing; diffuse neutral top-down three-quarter lighting.

Composition: exact 4x4 equal square cells, no margins, gutters, grid lines, labels, or overlap. Every cell fills edge-to-edge and matching side pixels align.

Palette: deep pine green, moss green, bark brown, dark teal shadows, restrained muted highlights.

Constraints: no characters, items, structures, text, UI, logos, watermarks, transparency, black background squares, isolated sparse trees, checkerboards, diagonal striping, copied designs, blur, gradients, or antialiasing.
```

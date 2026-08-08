# Character animation presentation assets

`source/melee-slash-chroma.png` is the image-generated source sheet. It was generated against the
existing character pack as a style reference, on a flat magenta chroma background. The prompt asked
for four sequential frames: glint, slash, impact, and fade, with no character redesigns.

`transparent/melee-slash.png` is the chroma-removed archival source. `assets/melee-slash.png` is the
runtime 96x24 atlas: four 24x24 cells in a single row. The effect is drawn over the target tile while
the character remains anchored to its own tile.

The `*-walk-chroma.png` files are two-pose walking sheets generated from individual character
references. Their `transparent/` counterparts are the cleaned archival sources.
`assets/characters-walk.png` is the runtime 32x48 atlas: two 16x16 step frames per row, with Klein,
Aldric, and the Giliman soldier in rows 0, 1, and 2. Chroma cleanup thresholds stray translucent
pixels before packing; every pose then receives the same 11x14 opaque bounds and boot baseline.
During a tile move the renderer holds step A for the first half and step B for the second half,
returning to idle only after the movement ends. Position interpolation is linear and there is no
vertical bob, so changing leg poses cannot make the actor appear to jump or flicker.

The `*-attack-chroma.png` files are three-pose melee sheets generated from the same character
references. Their `transparent/` counterparts are the cleaned archival sources.
`assets/characters-attack.png` is the runtime 72x48 atlas: three 24x16 cells per row, with the same
character row order. Columns are anticipation, strike, and recovery. The wider cells provide sword
room while keeping every character on a shared boot baseline; the renderer finishes on the original
idle frame instead of translating or lunging the whole sprite.

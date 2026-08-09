# The Margin Deep Cave v1

An authored 11×9-cell Deep Cave Mouth landmark generated to match the game's existing dark,
grounded pixel-art world structures. The runtime sheet is 264×216 pixels: exactly 11 columns by
9 rows of 24-pixel cells.

- `source/deep-cave-chroma.png`: full-resolution generated source with a flat chroma background.
- `transparent/deep-cave.png`: full-resolution transparency master.
- `runtime/deep-cave-2x.png`: 48-pixel authoring-grid version.
- `../../../assets/structures/deep-cave.png`: nearest-neighbour 24-pixel runtime atlas.

The art is sliced row-major at runtime. Collision is authored separately in `FloorGenerator` so
the boulder bowl and cave back block movement while the dirt approach remains walkable.

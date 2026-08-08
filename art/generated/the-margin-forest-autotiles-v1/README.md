# The Margin Forest Autotiles v1

A 16-cell, native-24px forest-wall bitmask atlas. Adjacent wall cells now form a
continuous woodland mass instead of rendering as isolated trees.

- `runtime/forest-autotiles.png` — 96x96 runtime atlas, 4x4 cells
- `runtime/00-*.png` through `runtime/15-*.png` — individual opaque 24px cells
- `preview/forest-autotiles.png` — nearest-neighbor 8x inspection preview
- `source/forest-autotiles.png` — untouched built-in image-generation output

## Cell indexing

The atlas index is a four-neighbor clearing mask:

```text
NORTH = 1, EAST = 2, SOUTH = 4, WEST = 8
```

A set bit means that edge faces a non-wall clearing. An unset bit means the forest
continues through that edge. The row-major atlas order is masks 0 through 15.

The runtime pack is fully opaque. The generated presentation grid is cropped before
nearest-neighbor downscaling, so it cannot create dark seams in the game. Six inspected
canonical shapes (interior, edge, adjacent corner, opposite-side strip, three-sided edge,
and isolated clump) are rotated mechanically into the exact 0–15 mask order.

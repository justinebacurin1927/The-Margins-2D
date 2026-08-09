# World Structure Revisions v2 — final prompt set

Mode: built-in image editing from each matching v1 chroma source, followed by the project's
magenta-removal script and nearest-neighbour normalization to native 24px runtime cells.

Shared production direction:

```text
Preserve the existing dark medieval forest survival-roguelike house style: crisp deliberately
low-resolution top-down orthographic pixel art, chunky color clusters, hard pixel edges, no
antialiasing, moody pine-green, bark-brown, wet-gray and muted-amber palette. Recompose the
referenced structure as one complete multi-tile explorable location on a perfectly uniform flat
solid #ff00ff chroma background with generous padding. Use clear cell-scale silhouettes and an
exact south-center approach. No characters, text, UI, grid, watermark, cast shadow, isometric
perspective, smooth painting, copied artwork, or #ff00ff inside the structure.
```

## Beehive Grove v2 — 11x11 cells

```text
Expand the grove to an 11x11-cell circular clearing. Move every tree, hive, root, flower, smoke
pot, honeycomb and harvesting prop outside the central 5x5 play area. Build an irregular but
continuous perimeter of old hive-bearing trees, with the harvest station far north and small
natural gaps between trunks. Keep the whole central 5x5 as plain walkable grass and preserve a
clean exact south-center entry lane. It must feel spacious, never like a cluttered arena.
```

## Fallen Log Hollow v2 — 9x5 cells

```text
Make this unmistakably a compact horizontal fallen tree trunk running west-to-east, not a cave.
Show cylindrical bark and readable cut end-grain at both ends. Use a roof-cutaway opening to reveal
one small earthen interior chamber only two or three cells deep, with restrained bedding and
supplies against the inner sides. Keep a clear exact south-center notch and path into the hollow.
No huge stone arch, black cavern mouth, rock ring, or tall chamber. It must be visibly smaller than
the game's 11x9 Deep Cave.
```

## Collapsed Watchtower v2 — 11x13 cells

```text
Rebuild the ruin as a true three-floor collapsed watchtower shown as three vertically stepped,
roofless cutaway terraces from south to north. The south ground floor has the entrance stair; the
middle guard platform is reached by a clearly visible stair lane; the upper lookout floor is
reached by a second stair and ends in broken crenellations. Keep each floor broad enough to walk
on and maintain a continuous route across all three levels. Use asymmetric collapsed stone,
timber, rubble and torn cloth around the outer edges, never across the connecting stair lanes.
```

## Poacher's Camp v2 — 13x11 cells

```text
Expand the camp into a large 13x11-cell concealed compound. Use four distinct shelters or work
zones around the perimeter: two north sleeping tents, a western supply awning, and an eastern
workshop or cage shelter. Add drying racks, snares, crates, hides, a cold firepit and branch screens
at the edges. Preserve a broad central yard at least 5x4 cells, mostly open, with a clean branching
route from the exact south-center entrance. It must read as an operating camp, not merely two
tents placed together.
```

## Sunken Wellhouse v2 — 11x11 cells

```text
Turn the sunken well into a roofless 11x11-cell ruined wellhouse with three connected interior
zones. Put a small tool-and-jar storage room on the west, the large circular stone well chamber in
the center, and a pump-and-barrel alcove on the east. Use real outer masonry and two open internal
doorways so both side rooms are enterable. Keep the open well shaft clearly solid/impassable and
preserve a clean flagstone route from the exact south-center entrance. Add damp moss, channels,
reeds and collapsed edge stones without obstructing the internal doors.
```

## Saved outputs

- Runtime: `assets/structures/beehive-grove.png` (264x264, 11x11)
- Runtime: `assets/structures/fallen-log-hollow.png` (216x120, 9x5)
- Runtime: `assets/structures/collapsed-watchtower.png` (264x312, 11x13)
- Runtime: `assets/structures/poachers-camp.png` (312x264, 13x11)
- Runtime: `assets/structures/sunken-well.png` (264x264, 11x11)
- Source, transparent, and 2x-normalized intermediates are stored in each matching
  `art/generated/the-margin-*-v2/` directory.

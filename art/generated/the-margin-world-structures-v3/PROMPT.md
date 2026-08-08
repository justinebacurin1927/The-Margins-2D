# Old House generation prompt

Mode: built-in image-generation tool, with three reference images.

- Image 1: user-supplied multi-room dungeon layout reference; layout and scale only.
- Image 2: current `assets/terrain-tiles.png`; palette and pixel-density reference.
- Image 3: previous world-structure atlas; Old House identity reference only.

```text
Create one original multi-tile explorable structure called The Old House for a dark medieval
forest roguelike. It occupies an exact 10×8 tile footprint and is a roofless top-down cutaway with
two connected rooms, one-tile-thick mossy stone-and-timber perimeter walls, a south entrance, an
internal doorway, worn plank and cracked-stone floor zones, a chest, cellar hatch, broken table,
cold hearth, and sparse wall-side debris. Keep central walk paths open. Use crisp deliberately
low-resolution pixel art, chunky clusters, hard edges, no antialiasing, and The Margin's pine-green,
bark-brown, wet-stone-gray, and muted-amber palette. Center the complete rectangular structure on a
perfectly flat solid #ff00ff chroma background. No characters, text, UI, grid lines, roof,
isometric distortion, cast shadow, watermark, black rectangle, or copied game art. It must remain
readable after reduction to 240×192 pixels and must never look like a one-tile building icon.
```

## Kitchen expansion edit

Mode: built-in image-generation edit. The original chroma source was the edit target and the live
game screenshot was the scale/readability reference.

```text
Preserve the complete existing two-room Old House and attach a genuinely separate walkable kitchen
to its west side. Match the existing mossy stone-and-timber walls and crisp pixel style. Give the
kitchen a reddish worn tile floor, brick oven, preparation counters, shelves, pots, utensils,
sacks, and small barrels while keeping a clear central path. Connect it through one open internal
doorway and retain the existing south entrance, chest, cellar hatch, fireplace, and room identities.
Keep the complete roofless structure on a perfectly flat #ff00ff chroma background. No characters,
text, UI, roof, exterior scenery, shadows, watermark, black rectangle, or copied game art.
```

## Doorway-clear furniture edit

Mode: built-in image-generation edit, followed by chroma removal and exact 13×8 grid normalization.
The live kitchen-expanded house was the edit target.

```text
Relocate the large broken wooden table and its debris away from the west kitchen partition and
deeper into the central plank room. Preserve the roofless three-room Old House, its kitchen,
fireplace, cellar, chest, hatch, barrel, south entrance, palette, top-down perspective, and crisp
dark-fantasy pixel treatment. Leave a visibly empty one-tile passage through the kitchen doorway
and keep both internal doorways clear. Isolate the house on uniform #ff00ff chroma. No characters,
HUD, text, grid, roof, new furniture, blocked doorway, smooth painting, or layout redesign.
```

The normalized pass reuses the established doorway trim and places the table in central columns
6–7, leaving columns 4 and 8 as clear landing cells beside the kitchen and cellar passages.

## Layered exterior foundation edit

Mode: built-in image-generation edit. The doorway-clear 13×8 house was centered inside a one-cell
magenta apron as the alignment target; only the generated exterior ring was retained, and the exact
existing house was composited back over its center.

```text
Preserve the finished Old House and add a second exterior foundation layer only in the one-tile
apron around its walls. Form an irregular but continuous transition of dark foundation shadow,
mossy edge stones, short roots, damp leaf litter, and sparse low brush, layered outward like the
forest treeline. Join corners smoothly, keep the rear and sides denser, and leave the south-door
approach low, clear, and walkable. Retain the hard-edged moody pixel style and uniform #ff00ff
outside the apron. Do not alter the rooms, walls, doors, furniture, scale, or footprint; no tall
props, implied collision, characters, HUD, text, grid, roof, cast shadow, or repeated checkerboard.
```

# The Margins — Consistent SPD-Style Image Prompt Guide

This is the canonical visual prompt reference for all future generated art for **The Margins**.
Use it together with [`ASSET-GENERATION-GUIDE.md`](./ASSET-GENERATION-GUIDE.md), which defines
the technical slicing, chroma-key, scaling, integration, and testing pipeline.

The goal is a cohesive **classic mobile dungeon-roguelike pixel style** inspired by the visual
clarity and economy of Shattered Pixel Dungeon (SPD), without copying its sprites, characters,
items, layouts, or proprietary artwork.

## 1. Required visual references

Attach the relevant subject reference plus these project references whenever possible.

### Primary style reference

- [`assets/generated/journal-storage-v2.png`](../assets/generated/journal-storage-v2.png)
  is the approved runtime reference for pixel density, outline weight, color clustering, and
  readable silhouettes.
- [`art/generated/the-margin-storage-items-v2/storage-items-source.png`](./generated/the-margin-storage-items-v2/storage-items-source.png)
  is the high-resolution generation source. Use it only to understand the subjects; the runtime
  atlas above is the authority for the final amount of detail.

### Presentation and world references

- [`The Margin - Remake/VIsion .png`](../The%20Margin%20-%20Remake/VIsion%20.png) is a mood,
  world-scale, and readability reference. Do not reproduce its exact layout or UI.
- Existing in-game actor, structure, or item sprites are the identity reference when revising
  something already present in the game. Never redesign an existing subject unless explicitly
  requested.

### Reference priority

When references disagree, follow this order:

1. Existing in-game sprite for subject identity.
2. `journal-storage-v2.png` for pixel density and rendering simplicity.
3. This document for palette, outlines, lighting, and composition.
4. `VIsion .png` for atmosphere and presentation only.

## 2. Non-negotiable style constants

Include these rules in every generation prompt.

- Deliberately low-resolution, hand-authored-looking pixel art.
- Large square pixels and obvious stair-step contours.
- Hard edges with no antialiasing, smoothing, or subpixel detail.
- One logical-pixel dark outline around important silhouettes.
- Use compact, intentional color clusters instead of scattered single-pixel noise.
- Ordinary sprites use approximately **3–6 principal colors** plus transparency.
- Each material gets at most one shadow cluster and one highlight cluster.
- Lighting comes from the upper-left unless a specific scene requires otherwise.
- Highlights are small and restrained; shadows carry most of the form.
- Silhouette and gameplay identity are more important than realistic material detail.
- No tiny stitches, repeated buckles, hair strands, scratches, pores, realistic reflections,
  painted texture, or decorative micro-detail.
- No gradients, bloom, glossy 3D rendering, painterly shading, or near-photorealistic pixel art.
- No copied SPD sprites. Match its **economy, scale readability, and chunky visual logic**, not
  its exact artwork.

## 3. Approved palette language

The overall palette is dark, earthy, and slightly desaturated:

- Pine and moss: deep blue-green, dark forest green, muted olive.
- Wood and leather: near-black brown, bark brown, muted ochre.
- Stone and metal: charcoal, wet-stone gray, dull steel.
- Cloth: faded gray-blue, weathered green-gray, muted brown.
- Skin: warm restrained peach and brown, never highly saturated.
- Highlights: muted amber, bone, pale gray, or a small faction accent.
- UI selection and important information: restrained amber/gold.

Bright saturated colors are reserved for tiny gameplay-readable accents such as potions,
hazards, magic, blood, or legendary properties.

## 4. Logical resolution by asset type

The image model must be told the **logical sprite resolution**, even when it outputs a larger
image. The generated art should look as if it were drawn at this small resolution and enlarged
with nearest-neighbor scaling.

| Asset type | Logical art target | Runtime cell | Detail rule |
|---|---:|---:|---|
| Small item or icon | 12×12 to 16×16 | 24×24 or 32×32 | Simplest form; silhouette first |
| Actor or enemy | 16×20 to 24×24 | 24×24 | Readable head, torso, hands/weapon, feet |
| Status icon | 8×8 to 12×12 | 16×16 or 24×24 | One symbol, no decoration |
| Environmental prop | 16×16 to 24×24 | 24×24 | One dominant mass and one accent |
| Ground tile | 24×24 | 24×24 | Full-cell coverage and seamless edges |
| Structure | Built from 24×24 tile logic | Multi-tile | Chunky modules; no miniature painting |
| UI ornament | 8px modular segments | Variable | Flat borders and restrained highlights |

Do not ask the model to create “highly detailed pixel art.” That phrase produces polished
pixel illustrations that do not fit the game.

## 5. Composition constants

### Isolated items, actors, and props

- Center one complete subject inside each equal atlas cell.
- Leave consistent padding on every side.
- Do not crop straps, weapons, feet, branches, or handles.
- No cast shadow, floor plane, scenery, text, labels, borders, or cell dividers.
- Use a perfectly flat `#ff00ff` chroma-key background from edge to edge.
- Never use `#ff00ff` inside a sprite.

### Actors and enemies

- Use the same top-down three-quarter viewing angle as existing actors.
- Keep the head slightly oversized and the body compact for 24px readability.
- Hands, weapon, and feet should be separate clusters, not fully rendered anatomy.
- Preserve the existing faction colors and equipment silhouette.
- If an in-game sprite already exists, edit or reuse that sprite as the identity reference.
- Do not invent a new species, armor set, or face for an existing record.

### Structures

- Build the structure visually from clear 24×24 tile-sized masses.
- Floors, walls, doors, stairs, and obstacles must be readable as gameplay geometry.
- Keep doors and required paths visibly clear.
- Use large material clusters; avoid roof-shingle, masonry, plank, or foliage micro-noise.
- Structures may be richer than item icons, but must still resemble assembled game tiles rather
  than a detailed concept painting.

### Tiles

- Cover the entire cell; never make a tile float on transparency.
- Repeat-edge pixels must match on opposing sides where seamless tiling is required.
- Avoid obvious diagonals, checkerboards, or repeated symmetric decoration.
- Variation should look organic but remain low-frequency and readable.

## 6. Canonical base prompt

Paste this block into every image-generation request, then append an asset-specific block from
the next section.

```text
PROJECT: The Margins, a dark medieval forest survival roguelike rendered on a 24px grid.

STYLE AUTHORITY: Use the attached The Margins runtime reference for pixel density and rendering
simplicity. Create original artwork with the chunky scale readability and economical sprite
logic associated with classic mobile dungeon roguelikes. Do not copy any existing game's exact
sprite, silhouette, layout, or design.

STYLE: Deliberately low-resolution hand-authored pixel art. Large square pixels, angular
stair-step silhouettes, hard one-logical-pixel dark outlines, compact color clusters, and no
antialiasing. Ordinary sprites use 3 to 6 principal colors. Give each material at most one
shadow cluster and one small highlight cluster. Prioritize silhouette and gameplay identity
over realism.

PALETTE: Dark desaturated pine green, bark brown, wet-stone gray, charcoal, dull steel, muted
ochre, gray-blue cloth, warm restrained skin, and small muted amber highlights. Bright colors
only for tiny gameplay-important accents.

BANNED: High-detail pixel illustration, smooth curves, gradients, bloom, glossy rendering,
realistic texture, texture noise, tiny stitches, tiny buckles, scratches, hair strands,
subpixel details, excessive highlights, scattered single pixels, antialiasing, blur, text,
labels, numbers, UI, logos, watermarks, cast shadows, and floor planes.

BACKGROUND: Perfectly flat uniform solid #ff00ff chroma-key background, edge to edge. Do not
use #ff00ff inside any subject.
```

## 7. Asset-specific prompt blocks

### Items and storage equipment

```text
ASSET TYPE: Runtime inventory item sprite or atlas.
LOGICAL SCALE: The artwork must look authored at 12x12 to 16x16 logical pixels, then enlarged
with nearest-neighbor hard square pixels.
RENDERING: Use one dominant silhouette, one identifying secondary shape, a dark outline, and
only essential clasps, straps, contents, or magic accents. Remove all decorative micro-detail.
COMPOSITION: One isolated complete item centered in each equal cell with generous consistent
padding. Preserve the exact supplied subject identity and atlas order.
```

### Characters and enemies

```text
ASSET TYPE: Runtime actor sprite or animation atlas.
LOGICAL SCALE: The artwork must look authored within a 16x20 to 24x24 logical-pixel body.
CAMERA: Top-down three-quarter game view matching the attached existing actor sprites.
RENDERING: Compact body, slightly oversized head, readable torso color, separate hand/weapon
cluster, and two simple foot clusters. No anatomy rendering or costume micro-detail.
IDENTITY: Preserve the attached in-game sprite's species, face, armor, faction palette, weapon,
and silhouette. Change animation pose or facing only. Never replace an established character
or enemy with a newly invented design.
```

### Structures

```text
ASSET TYPE: Multi-tile runtime world structure.
GRID: Design every wall, floor, door, stair, prop, and obstacle around visible 24x24 tile logic.
CAMERA: Consistent top-down three-quarter game view.
RENDERING: Use large connected material clusters and readable gameplay geometry. Simplify
planks, stones, foliage, and debris into broad patterns. Do not create a miniature concept
painting. Keep entrances and mandatory movement paths visibly unobstructed.
```

### Ground and environment tiles

```text
ASSET TYPE: Seamless 24x24 runtime tile atlas.
RENDERING: Each tile fills its complete cell. Use broad irregular clusters with sparse accents.
Avoid diagonal striping, checkerboards, symmetry, and obvious repeating motifs.
SEAMS: Opposing edge pixels must match wherever the tile is required to repeat seamlessly.
```

### Effects and weather

```text
ASSET TYPE: Runtime overlay effect atlas.
RENDERING: Sparse bold clusters that remain legible over dark terrain. Use asymmetrical timing,
positions, and shapes. Avoid tile-sized repeated blocks or evenly spaced lines. Soft alpha is
allowed only when the effect's function requires atmospheric falloff; particle cores remain
hard pixel clusters.
```

## 8. Complete copy-ready prompt template

```text
Use case: [new generation / style-transfer edit / atlas revision]
Asset type: [item atlas / actor sheet / enemy sheet / structure / tile sheet / effect sheet]

INPUT REFERENCES:
- Image 1 is the existing subject identity reference. Preserve its defining design.
- Image 2 is The Margins' approved runtime pixel-density reference.
- Image 3 is an optional mood or world-scale reference only.

PRIMARY REQUEST:
[Describe exactly what must be created or changed.]

PRESERVE EXACTLY:
[Subject identities, existing colors, equipment, order, cell count, facings, and layout.]

SUBJECTS AND ATLAS ORDER:
[List every row and column explicitly.]

PROJECT: The Margins, a dark medieval forest survival roguelike rendered on a 24px grid.

STYLE: Deliberately low-resolution hand-authored pixel art. Make it look authored at
[logical resolution] and enlarged with nearest-neighbor square pixels. Use angular stair-step
silhouettes, hard one-logical-pixel dark outlines, compact clusters, 3 to 6 principal colors,
one shadow cluster, and one restrained highlight cluster per material. Match the simplicity and
readability of the attached approved runtime reference. Create original artwork; do not copy
another game's exact sprite or design.

PALETTE:
[Relevant dark desaturated project colors and any essential accent colors.]

COMPOSITION:
[Exact sheet dimensions, rows, columns, cell size, centering, padding, and camera angle.]

BACKGROUND: Perfectly flat uniform solid #ff00ff chroma-key background, edge to edge. Do not
use #ff00ff inside sprites.

BANNED: High-detail pixel illustration, tiny stitches, tiny buckles, scratches, texture noise,
realistic material shading, gradients, bloom, glossy rendering, smooth curves, antialiasing,
blur, text, labels, numbers, UI, logos, watermarks, cast shadows, floor planes, overlap, crop,
cell borders, and invented redesigns of existing subjects.

SUCCESS TEST: At final runtime size, each subject must be identifiable from its silhouette and
one or two signature color/shape clusters. If the art looks like a polished illustration when
zoomed in, it is too detailed and must be simplified.
```

## 9. Revision prompt when a result is too polished

Use this exact correction block instead of regenerating from a vague request:

```text
STYLE-TRANSFER REVISION: Preserve the exact subjects, identities, colors, atlas order, and
composition. Change only the rendering complexity. The current image is too polished and
near-perfect. Tone it down aggressively into coarser runtime sprite art.

Make every sprite appear authored at 12x12 to 16x16 logical pixels. Replace smooth contours
with larger angular stair-step shapes. Reduce each ordinary sprite to 3 to 6 principal colors.
Merge tiny details into broad clusters. Keep only one shadow cluster and one highlight cluster
per material. Remove stitches, repeated straps, small buckles, reflections, texture noise,
realistic shading, and decorative single pixels. Preserve one dominant silhouette and one or
two gameplay-identifying features per subject. Do not alter subject identity or layout.
```

## 10. Rejection checklist

Reject and revise an image if any answer is “yes”:

- Does it look like a polished pixel illustration instead of a runtime sprite?
- Are there details that disappear at 24px or 32px?
- Are outlines smooth, rounded, antialiased, or inconsistent?
- Are there too many nearly identical shades of one material?
- Is texture carried by noise rather than deliberate clusters?
- Did the model invent or change an existing character, enemy, weapon, or structure?
- Is the atlas order, facing, or cell count wrong?
- Are sprites cropped, touching cell edges, overlapping, or inconsistently scaled?
- Is there a floor plane, cast shadow, label, border, or nonuniform chroma background?
- Does a structure hide its doorway or make its walkable route unclear?
- Do seamless tiles create diagonals, checkerboards, symmetry, or visible seams?

## 11. Final runtime validation

Before integrating any generated image:

1. Remove the `#ff00ff` chroma background to transparency.
2. Slice and normalize every cell without changing the requested atlas order.
3. Downscale using nearest-neighbor only.
4. Inspect at actual runtime size, not just zoomed in.
5. Compare directly with `journal-storage-v2.png` and existing actors on the same screen.
6. Confirm transparent corners and non-empty content in every required cell.
7. Reject any sprite whose identity depends on details invisible at runtime scale.
8. Integrate under a new versioned filename; keep the generated source and exact prompt.
9. Build, run, and capture an in-game screenshot before considering the asset finished.

## 12. Consistency rule for future agents

Any AI agent generating or revising raster art for The Margins must read this document and
`ASSET-GENERATION-GUIDE.md` first. It must use the approved runtime atlas as a style reference,
preserve existing in-game subject identity, record the exact generation prompt beside the
source image, and validate the final asset at runtime scale.

# Pixel Pack v2 generation prompts

Every image was generated with the built-in image tool. The two user-provided images were treated
as references, not edit targets:

- Image 1: implementation context showing why the v1 art did not fit the 24px game tiles.
- Image 2: target pixel density, simplified shapes, limited shading, and compact proportions.

## Characters

```text
Use case: stylized-concept
Asset type: true low-resolution top-down roguelike character sprite atlas, v2
Primary request: Rebuild The Margin character pack in a MUCH SIMPLER tiny-pixel style that fits a 24px game tile. This is a new original atlas, not an edit of either reference.
Input images: Image 1 is implementation-context reference only: it shows that the previous tall detailed sprites become tiny and visually mismatched when drawn in the game; do not copy its UI or artwork. Image 2 is the TARGET pixel-density and simplification reference only: match its tiny chibi scale, chunky pixels, minimal shading, compact silhouette and limited palette, but do not reproduce its soldiers, room, colors, poses, or exact sprite designs.
Subjects in a 4x4 row-major atlas: Klein; Aldric; Mara; Old Fen; Sister Yenna; Commander Vos; Giliman foot soldier; Free Company mercenary captain; Traveling Wanderer; Black Market trader; Evermove Sense-user; frightened survivor; wolf; venomous snake; undead mercenary; bear.
Pixel specification: each subject must be designed as a genuine 16x16-pixel master sprite, roughly 12-15 pixels tall, shown enlarged with exact nearest-neighbor square pixels. Extremely economical detail: large readable head, 1-pixel eyes, compact 3-5-pixel torso, tiny limbs, at most 3 shades per material, one strong identifying color or prop. No painterly rendering, no smooth curves, no micro-detail, no texture noise, no antialiasing, no semi-transparent edge pixels.
View: top-down three-quarter RPG view like the target reference, all facing downward, neutral idle pose, consistent feet baseline and scale.
Composition: exact 4 columns x 4 rows, one centered sprite per equal cell, wide empty padding, no overlap, no grid lines.
Backdrop: perfectly flat solid #ff00ff chroma-key background; totally uniform, no shadows, no floor, no gradients, no texture. Do not use #ff00ff in sprites.
Constraints: original designs; no text, labels, UI, borders, logos, watermark, weapons crossing cell boundaries, cast shadows, glow beyond a sprite cell, copied characters, or high-resolution concept-art detail.
```

## Environment tiles

```text
Use case: stylized-concept
Asset type: true 16x16 top-down forest environment tileset for The Margin
Primary request: Create an ORIGINAL compact forest tileset for the game's Herois pine woods, in the tiny simplified pixel language of Image 2. It must visually replace the large flat rectangles seen in Image 1.
Input images: Image 1 is implementation-context only, showing the current flat-color forest floor; do not reproduce its UI or composition. Image 2 is the target pixel density, block size, limited shading, clean tile edges and restrained palette; do not reproduce its dungeon room or characters.
Tile contents, exact 8x8 grid:
Row 1: eight seamless ground tiles — four dark pine-grass variants, two mossy grass variants, two packed forest-dirt variants.
Row 2: dirt path tiles — horizontal, vertical, four corners, T-junction, four-way junction.
Row 3: vegetation props — short grass tuft, tall grass, fern, low bush, berry bush, tiny white flowers, mushrooms, reeds.
Row 4: trees/wood — young pine, full pine, dark old pine, leafless dead tree, stump, fallen log, cut-log pile, twisted roots.
Row 5: rocks/terrain — pebble, small rock, mossy boulder, three-rock cluster, cracked earth, muddy patch, leaf litter, pine-needle patch.
Row 6: water — still pond, rippled pond, shallow water, river current, muddy bank, grassy bank, stepping stones, puddle.
Row 7: simple forest construction props — rough wood fence horizontal, fence vertical, fence corner, wooden gate, signpost, rope snare, small campfire, cold ash campfire.
Row 8: fog/danger variants — shadow grass, cold-blue grass, rain-dark grass, blood-marked grass, bones, animal tracks, broken branch marker, empty transparent tile.
Pixel specification: every cell is a genuine 16x16-pixel tile master shown enlarged using exact nearest-neighbor square pixels; use only large pixel clusters, 2-3 shades per material, roughly 16-24 total palette colors, no texture noise, no micro-detail, no antialiasing, no semi-transparent pixels. Ground and path tiles fill their square cleanly and tile seamlessly at the edges. Props are centered with transparent-ready magenta around them.
Composition: exact 8 columns x 8 rows, perfectly equal square cells, no grid lines, no labels, no spacing variation.
Backdrop/gutters: flat solid #ff00ff only where a tile is meant to be transparent; do not use #ff00ff in artwork. Ground tiles themselves remain fully opaque squares.
View: straight top-down with a slight readable front face only on trees/rocks, consistent with Image 2.
Constraints: no characters, text, labels, numbers, UI, borders, logos, watermark, painterly rendering, gradients, blur, cast shadows, lighting effects, or copied dungeon tiles.
```

## Items

```text
Use case: stylized-concept
Asset type: true low-resolution roguelike inventory item atlas, v2
Primary request: Rebuild The Margin item pack in the tiny simplified pixel style of Image 2 so icons remain readable at 16x16 and fit the game's compact HUD. This is a new original atlas.
Input images: Image 1 is implementation-context only; do not copy its UI or prior detailed icons. Image 2 is the target pixel density, chunky block size, minimal shading, strong silhouette, and limited palette; do not reproduce its characters or dungeon.
Exact 8x5 row-major contents: row 1 bread, sausage, moldy cheese, raw meat, cooked meat, berries, spotted mushroom, honeycomb; row 2 raw waterskin, purified waterskin, ale bottle, unknown clay jar, coal, wood bundle, rope, cloth; row 3 wooden club, rusted spear, shortbow, broken knight sword, captain spear, tomahawk, knight buckler, Aegis Ward; row 4 rusted knife, handaxe, pickaxe, campfire kit, torch, metal scrap, bandage, map fragment; row 5 woven pouch, hunter satchel, mercenary rucksack, poacher game bag, traveler pack, potion bandolier, scroll holder, Wind Tempest bow.
Pixel specification: each icon is a genuine 16x16-pixel master shown enlarged with exact nearest-neighbor square pixels. Use only 1-pixel dark outline clusters, 2-3 shades per material, one small highlight, no texture noise, no micro-details, no antialiasing, no smooth curves, no partial alpha.
Composition: exact 8 columns x 5 rows, one centered icon per equal cell, consistent 12-15 pixel visual size, generous uniform padding, no grid lines or overlap.
Backdrop: perfectly flat solid #ff00ff chroma-key, totally uniform, no shadows, gradients, texture or floor; never use #ff00ff in icons.
Constraints: original icons; no text, labels, numbers, UI frames, logos, watermark, painterly detail, glows extending beyond cells, or copied inventory designs.
```

## World structures

```text
Use case: stylized-concept
Asset type: true low-resolution top-down world-structure atlas, v2
Primary request: Rebuild The Margin's eleven Herois structures in the tiny simplified pixel style of Image 2, suitable for placing on the new 16x16 forest tiles. This is a new original atlas.
Input images: Image 1 is implementation context only; do not copy its UI or earlier detailed structures. Image 2 defines the target blocky pixels, compact proportions, minimal shading and limited palette; do not reproduce its room or soldiers.
Exact 4x3 row-major contents: Hunter's Blind; Fallen Log Hollow; Forest Shrine; Beehive Grove; Worn Down Kitchen Camp; Collapsed Watchtower; Poacher's Camp; Sunken Well; The Old House; Mercenary Graveyard; Deep Cave Mouth; empty transparent cell.
Pixel specification: each structure is a genuine 32x32-pixel master built from the same apparent pixel size as a 16x16 character/tile, shown enlarged with exact nearest-neighbor square pixels. Use bold block shapes, dark 1-pixel outlines, 2-3 shades per material, almost no surface texture, no micro-details, no antialiasing or partial transparency.
View/scale: compact top-down three-quarter RPG view compatible with 16x16 forest terrain. Small structures may occupy about 24x24; large structures may fill 30x30. Consistent ground contact and scale.
Composition: exact 4 columns x 3 rows, centered within equal cells, generous uniform padding, no overlaps or grid lines.
Backdrop: perfectly flat solid #ff00ff chroma-key, no shadows, ground plane, gradients or texture; never use #ff00ff in structures.
Palette: dark pine green, muted moss, bark brown, tan canvas, cool stone gray, tiny amber highlights.
Constraints: original designs; no characters, text, labels, numbers, UI, logos, watermark, painterly detail, excessive realism, fog, cast shadows, or copied structures.
```

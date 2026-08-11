# Storage Item Atlas v2

Generated with the built-in image generation tool in image-edit mode, using the v1 storage atlas as the identity and layout reference.

## Prompt summary

- Preserve the exact four-column by three-row order and all twelve storage items.
- Redraw every item as coarse classic mobile dungeon-roguelike pixel art.
- Target a 12x12 to 16x16 logical-pixel appearance enlarged with hard nearest-neighbor pixels.
- Use angular silhouettes, a one-pixel dark outline, flat color clusters, and very limited highlights.
- Remove tiny stitches, straps, reflections, texture noise, gradients, antialiasing, and realistic shading.
- Keep the two legendary items visually distinct but equally coarse.
- Render on a flat `#ff00ff` chroma-key background with no labels, borders, shadows, or floor plane.

## Atlas order

1. Woven Pouch
2. Hunter's Satchel
3. Mercenary's Rucksack
4. Poacher's Game Bag
5. Family's Trunk
6. Traveler's Pack
7. Reinforced Backpack
8. Expedition Pack
9. Potion Bandolier
10. Scroll Holder
11. Vien's Dimensional Pocket
12. Faahard's Oblivion Blade

## Runtime processing

The generated source was chroma-keyed, divided into twelve equal cells, and reduced to a 128x96 transparent atlas. Each runtime icon occupies a 32x32 cell. The direct 32px reduction was selected because the stricter 16px-doubled experiment made several storage-item silhouettes indistinguishable.

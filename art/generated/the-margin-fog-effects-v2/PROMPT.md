# The Margin Fog Effects v2

## Problem reference

- `assets/fog-effects.png`: the v1 source formed long horizontal ribbons.
- The in-game Fog screenshot showed those repeated ribbons resolving into straight lanes.

## Final edit prompt

```text
Use case: precise-object-edit.
Asset type: replacement runtime animated fog overlay sprite sheet for The Margin.
Input image 1 is the current four-frame chroma-key fog source and edit target. Input image 2
is the in-game problem reference showing straight fog lanes.
Replace only the four fog silhouettes with irregular, asymmetrical, pixelated drifting fog
patches. Each phase is a loose broken cloud bank made from offset rounded clusters, curled
wisps, open holes, and uneven X/Y contours. Change the silhouette gently between frames.
Absolutely no long straight horizontal ribbons, parallel lines, lane-like strips, repeated
bars, or symmetrical cloud shapes.
Keep exactly four isolated frames in one horizontal row. Use chunky low-resolution pixel art,
pale desaturated blue-gray and muted sage-gray, with a perfectly flat uniform #ff00ff
chroma-key background. No text, UI, characters, terrain, precipitation, fire, logos, or
watermarks.
```

## Runtime normalization

- Generated with the built-in image-generation tool.
- Source: `source/fog-effects-v2-chroma.png`.
- Chroma-keyed, edge-contracted, despilled, desaturated to remove residual key color, and
  normalized into four transparent 48×48 cells.
- Runtime atlas: `../../../assets/fog-effects-v2.png` (192×48).

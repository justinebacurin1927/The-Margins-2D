# The Margin Fog Effects v1

## Reference

- `assets/light-weather-effects.png`: pixel density, restrained effect palette, and atlas style.

## Final generation prompt

```text
Use case: stylized-concept.
Asset type: runtime animated fog overlay sprite sheet for The Margin, a 24px-grid dark
medieval forest survival roguelike.
Input image: use the attached light-weather-effects atlas only as the exact pixel density,
restrained palette, and effects-sheet style reference. Do not alter or reproduce its fire,
rain, snow, or darkness cells.
Primary request: create exactly four isolated fog animation frames in one horizontal row.
Each equal cell contains a different phase of the same low drifting ground mist: thin broken
horizontal ribbons and sparse wisps designed to slide slowly over a top-down forest map.
Frames loop smoothly without changing overall density.
Style: deliberately low-resolution chunky pixel art, crisp hard pixel clusters, no
antialiasing. Pale desaturated blue-gray and muted sage-gray mist; no gradients or
photoreal smoke.
Backdrop: perfectly flat solid #ff00ff chroma-key background. Never use #ff00ff in fog.
Constraints: no text, UI, characters, terrain, precipitation, fire, lightning, borders,
logos, watermark, cast shadow, or copied copyrighted designs.
```

## Runtime normalization

- Built-in image-generation tool output: `source/fog-effects-chroma.png`.
- Chroma-keyed with the installed image-generation helper.
- Each of the four cells was independently cropped, fitted into a transparent 48×48 cell,
  and assembled as `../../../assets/fog-effects.png` (192×48).
- Runtime opacity, tint, drift, layering, and loop timing are controlled in `MarginScreen`.

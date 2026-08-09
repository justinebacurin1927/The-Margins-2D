# The Margin Light & Weather Effects v1

## References

- Existing `items/28-torch.png`: torch palette and pixel-density reference.
- Existing `environment-tiles/54-campfire.png`: flame shape and palette reference.

## Final generation prompt

```text
Use case: stylized-concept.
Asset type: runtime 2D effects sprite sheet for a 24px-grid dark medieval forest survival roguelike.
Input images: Image 1 is the existing handheld torch icon and palette reference. Image 2 is the existing campfire sprite and flame-shape reference. Use them only as style, palette, and pixel-density references.

Primary request: create EXACTLY eight isolated effect cells in a 4-column by 2-row atlas.
Row 1: handheld torch flame frame A; handheld torch flame frame B; warm amber torch glow; radial darkness vignette with a clear circular center.
Row 2: diagonal rain streak frame A; diagonal rain streak frame B; sparse falling snow; completely empty magenta cell.

Style/medium: deliberately low-resolution chunky pixel art matching the two reference sprites. Flame frames use crisp hard pixel clusters, restrained yellow-white cores, amber midtones, and dark orange edges. Glow and darkness may use a deliberately smooth radial falloff because they are blended lighting effects. Rain/snow stay sparse and readable.
Composition: one isolated effect centered in every equal cell; exact straight 4x2 layout; generous clear margins; absolutely no cell borders, dividers, grid lines, labels, or framing. Flame A and B must remain the same size/position but differ visibly enough for a calm two-frame flicker.
Scene/backdrop: perfectly flat solid #ff00ff chroma-key background in every unused pixel. Background must be one uniform color with no shadows, gradients, floor, texture, or lighting variation. Never use #ff00ff within any effect.
Constraints: no characters, torch handle, items, terrain, UI, text, numbers, logos, watermark, cast shadows, perspective, extra particles, or copied copyrighted designs. The final sheet must remain readable when normalized to 192x96 pixels (48px per cell).
```

## Output and normalization

- Generated with the built-in image-generation tool using the two local sprite references.
- Source: `source/light-weather-effects-chroma.png`.
- Runtime: `../../../assets/light-weather-effects.png` (192x96, eight 48px cells).
- Each generated cell was cropped independently, reduced to 48x48 with nearest-neighbor sampling,
  chroma-keyed with the installed image-generation helper, and reassembled without gutters.


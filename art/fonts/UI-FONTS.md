# Runtime UI fonts

The Margins uses two fonts at their native pixel-grid sizes:

- **m5x7** at size 16 for dialogue, event messages, values, controls, and item names.
  Copyright Daniel Linssen; distributed under CC0 1.0. See `m5x7/LICENSE.md`.
- **Press Start 2P** at size 8 for short panel headings and game-over titles.
  Copyright The Press Start 2P Project Authors; distributed under the SIL Open Font
  License 1.1. See `press-start-2p/OFL.txt`.

Generate the LibGDX bitmap atlases with:

```sh
python3 art/fonts/generate_ui_fonts.py
```

The generated glyphs have one-bit alpha and are rendered with nearest-neighbor filtering.
No fractional font scaling is used at runtime.

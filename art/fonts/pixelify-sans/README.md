# Pixelify Sans UI font

The Margins uses **Pixelify Sans Regular** for its HUD, dialogue, narration, and
game-over text. It replaces Tiny5 with cleaner lowercase forms and more comfortable
spacing at the game's 480x360 virtual resolution.

Pixelify Sans is Copyright 2021 The Pixelify Sans Project Authors and is distributed
under the SIL Open Font License 1.1; see `OFL.txt`.

Source: <https://github.com/google/fonts/tree/main/ofl/pixelifysans>

Generate the runtime atlas with:

```sh
python3 art/fonts/pixelify-sans/generate_bitmap_font.py
```

The generated atlas is hard-thresholded to one-bit alpha and rendered with nearest-
neighbor filtering, keeping every edge crisp without the heavy Tiny5 letterforms.

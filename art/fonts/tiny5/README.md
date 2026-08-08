# Tiny5 UI font

The Margins uses **Tiny5 Regular** for its HUD, dialogue, narration, and game-over text.
Tiny5 is Copyright 2022-2024 The Tiny5 Project Authors and is distributed under the
SIL Open Font License 1.1; see `OFL.txt`.

Source: <https://github.com/google/fonts/tree/main/ofl/tiny5>

Runtime files are generated with:

```sh
python3 art/fonts/tiny5/generate_bitmap_font.py
```

The generator rasterizes at the native UI size, hard-thresholds every glyph to remove
antialiasing, and writes LibGDX-compatible `assets/fonts/tiny5.fnt` and `tiny5.png` files.

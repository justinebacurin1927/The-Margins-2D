#!/usr/bin/env python3
"""Slice the guide terrain sheet into exact 24px opaque runtime tiles."""

from pathlib import Path

from PIL import Image


ROOT = Path(__file__).resolve().parent
SOURCE = ROOT / "transparent" / "terrain.png"
RUNTIME = ROOT / "runtime"
PREVIEW = ROOT / "preview"
NAMES = [
    "floor-grass-a", "floor-grass-b", "floor-dirt-path", "floor-leaf-litter",
    "wall-pine-a", "wall-pine-b", "wall-large-rock", "wooden-door",
    "stone-well", "pond-a", "pond-b", "river-a",
    "river-b", "empty-1", "empty-2", "empty-3",
]


def main() -> None:
    source = Image.open(SOURCE).convert("RGBA")
    atlas = Image.new("RGBA", (96, 96), (0, 0, 0, 0))
    RUNTIME.mkdir(parents=True, exist_ok=True)
    PREVIEW.mkdir(parents=True, exist_ok=True)

    for index, name in enumerate(NAMES):
        row, col = divmod(index, 4)
        x0 = round(col * source.width / 4)
        x1 = round((col + 1) * source.width / 4)
        y0 = round(row * source.height / 4)
        y1 = round((row + 1) * source.height / 4)

        if index < 13:
            # The generated preview has a dark presentation gutter around each cell. Remove it,
            # then make the gameplay tile completely opaque so no clear-color square can show.
            inset = 10
            tile = source.crop((x0 + inset, y0 + inset, x1 - inset, y1 - inset))
            tile = tile.resize((24, 24), Image.Resampling.NEAREST)
            tile.putalpha(Image.new("L", (24, 24), 255))
        else:
            tile = Image.new("RGBA", (24, 24), (0, 0, 0, 0))

        tile.save(RUNTIME / f"{index:02d}-{name}.png")
        atlas.alpha_composite(tile, (col * 24, row * 24))

    atlas.save(RUNTIME / "terrain-atlas.png")
    atlas.resize((768, 768), Image.Resampling.NEAREST).save(PREVIEW / "terrain-atlas.png")


if __name__ == "__main__":
    main()

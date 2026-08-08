#!/usr/bin/env python3
"""Slice the generated 4x4 forest bitmask sheet into native 24px runtime tiles."""

from pathlib import Path

from PIL import Image


ROOT = Path(__file__).resolve().parent
SOURCE = ROOT / "source" / "forest-autotiles.png"
RUNTIME = ROOT / "runtime"
PREVIEW = ROOT / "preview"

# Bit order: NORTH=1, EAST=2, SOUTH=4, WEST=8. A set bit means that side
# touches a walkable clearing; an unset bit continues into dense forest.
NAMES = [
    "interior",
    "clear-n",
    "clear-e",
    "clear-ne",
    "clear-s",
    "clear-ns",
    "clear-es",
    "clear-nes",
    "clear-w",
    "clear-nw",
    "clear-ew",
    "clear-new",
    "clear-sw",
    "clear-nsw",
    "clear-esw",
    "clear-nesw",
]

# Image models are good at the six canonical forest-boundary silhouettes but unreliable at
# placing every rotated equivalent in exact bitmask order. Build the runtime atlas mechanically
# from those six inspected source cells. Values are (source cell, clockwise quarter-turns).
MASK_SOURCES = {
    0: (0, 0),    # forest interior
    1: (1, 0),    # one clear side: N
    2: (1, 1),    # E
    3: (3, 0),    # adjacent clear sides: NE
    4: (1, 2),    # S
    5: (10, 1),   # opposite clear sides: NS
    6: (3, 1),    # ES
    7: (7, 0),    # three clear sides: NES
    8: (1, 3),    # W
    9: (3, 3),    # NW
    10: (10, 0),  # opposite clear sides: EW
    11: (7, 3),   # NEW
    12: (3, 2),   # SW
    13: (7, 2),   # NSW
    14: (7, 1),   # ESW
    15: (15, 0),  # isolated clump, clear on all sides
}


def source_tile(source: Image.Image, index: int) -> Image.Image:
    row, col = divmod(index, 4)
    x0 = round(col * source.width / 4)
    x1 = round((col + 1) * source.width / 4)
    y0 = round(row * source.height / 4)
    y1 = round((row + 1) * source.height / 4)

    # The image tool added a thin black presentation grid. Crop it before
    # nearest-neighbor reduction so every gameplay cell remains edge-to-edge.
    inset = 6
    tile = source.crop((x0 + inset, y0 + inset, x1 - inset, y1 - inset))
    tile = tile.resize((24, 24), Image.Resampling.NEAREST)
    tile.putalpha(Image.new("L", tile.size, 255))
    return tile


def main() -> None:
    source = Image.open(SOURCE).convert("RGBA")
    atlas = Image.new("RGBA", (96, 96), (0, 0, 0, 0))
    RUNTIME.mkdir(parents=True, exist_ok=True)
    PREVIEW.mkdir(parents=True, exist_ok=True)

    canonical = {
        source_index: source_tile(source, source_index)
        for source_index, _ in set(MASK_SOURCES.values())
    }

    for index, name in enumerate(NAMES):
        row, col = divmod(index, 4)
        source_index, turns = MASK_SOURCES[index]
        tile = canonical[source_index]
        for _ in range(turns):
            tile = tile.transpose(Image.Transpose.ROTATE_270)

        tile.save(RUNTIME / f"{index:02d}-{name}.png")
        atlas.alpha_composite(tile, (col * 24, row * 24))

    atlas.save(RUNTIME / "forest-autotiles.png")
    atlas.resize((768, 768), Image.Resampling.NEAREST).save(
        PREVIEW / "forest-autotiles.png"
    )


if __name__ == "__main__":
    main()

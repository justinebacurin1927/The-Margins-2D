#!/usr/bin/env python3
"""Trim generated atlas cells and repack them as actual low-resolution game assets."""

from pathlib import Path

from PIL import Image


ROOT = Path(__file__).resolve().parent
SOURCE = ROOT / "transparent"
RUNTIME = ROOT / "runtime"
PREVIEW = ROOT / "preview"

CHARACTERS = [
    "klein", "aldric", "mara", "old-fen",
    "sister-yenna", "commander-vos", "giliman-soldier", "mercenary-captain",
    "traveling-wanderer", "black-market-trader", "sense-user", "frightened-survivor",
    "wolf", "venomous-snake", "undead-mercenary", "bear",
]

ITEMS = [
    "bread", "sausage", "moldy-cheese", "raw-meat", "cooked-meat", "berries", "spotted-mushroom", "honeycomb",
    "raw-waterskin", "purified-waterskin", "ale", "unknown-water-jar", "coal", "wood-bundle", "rope", "cloth",
    "wooden-club", "rusted-spear", "shortbow", "broken-knight-sword", "captain-spear", "tomahawk", "knight-buckler", "aegis-ward",
    "rusted-knife", "handaxe", "pickaxe", "campfire-kit", "torch", "metal-scrap", "bandage", "map-fragment",
    "woven-pouch", "hunter-satchel", "mercenary-rucksack", "poacher-game-bag", "traveler-pack", "potion-bandolier", "scroll-holder", "wind-tempest",
]

STRUCTURES = [
    "hunters-blind", "fallen-log-hollow", "forest-shrine", "beehive-grove",
    "kitchen-camp", "collapsed-watchtower", "poachers-camp", "sunken-well",
    "old-house", "mercenary-graveyard", "deep-cave-mouth", "empty",
]

ENVIRONMENT = [
    "pine-grass-1", "pine-grass-2", "pine-grass-3", "pine-grass-4", "moss-grass-1", "moss-grass-2", "forest-dirt-1", "forest-dirt-2",
    "path-horizontal", "path-vertical", "path-corner-1", "path-corner-2", "path-corner-3", "path-corner-4", "path-t-junction", "path-cross",
    "short-grass", "tall-grass", "fern", "low-bush", "berry-bush", "white-flowers", "mushrooms", "reeds",
    "young-pine", "full-pine", "old-pine", "dead-tree", "stump", "fallen-log", "cut-log-pile", "twisted-roots",
    "pebble", "small-rock", "mossy-boulder", "rock-cluster", "cracked-earth", "muddy-patch", "leaf-litter", "pine-needles",
    "still-pond", "rippled-pond", "shallow-water", "river-current", "muddy-bank", "grassy-bank", "stepping-stones", "puddle",
    "fence-horizontal", "fence-vertical", "fence-corner", "wooden-gate", "signpost", "rope-snare", "campfire", "cold-ashes",
    "shadow-grass", "cold-blue-grass", "rain-dark-grass", "blood-marked-grass", "bones", "animal-tracks", "broken-branch", "empty",
]


def cells(image: Image.Image, cols: int, rows: int):
    """Yield cells using proportional boundaries, so non-divisible generated sizes stay aligned."""
    for row in range(rows):
        for col in range(cols):
            x0 = round(col * image.width / cols)
            x1 = round((col + 1) * image.width / cols)
            y0 = round(row * image.height / rows)
            y1 = round((row + 1) * image.height / rows)
            yield row, col, image.crop((x0, y0, x1, y1))


def fit(cell: Image.Image, size: int, padding: int, bottom_align: bool, fill_cell: bool) -> Image.Image:
    alpha = cell.getchannel("A")
    bbox = alpha.getbbox()
    output = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    if bbox is None:
        return output

    sprite = cell.crop(bbox)
    # The generation cleanup may leave a soft matte. Runtime pixel art uses hard alpha only.
    alpha = sprite.getchannel("A").point(lambda value: 255 if value >= 128 else 0)
    sprite.putalpha(alpha)
    hard_bbox = alpha.getbbox()
    if hard_bbox is None:
        return output
    sprite = sprite.crop(hard_bbox)
    if fill_cell:
        sprite = sprite.resize((size, size), Image.Resampling.NEAREST)
        output.alpha_composite(sprite, (0, 0))
        return output

    maximum = size - padding * 2
    scale = min(maximum / sprite.width, maximum / sprite.height)
    width = max(1, round(sprite.width * scale))
    height = max(1, round(sprite.height * scale))
    sprite = sprite.resize((width, height), Image.Resampling.NEAREST)
    x = (size - width) // 2
    y = size - padding - height if bottom_align else (size - height) // 2
    output.alpha_composite(sprite, (x, y))
    return output


def pack(source_name: str, names: list[str], cols: int, rows: int, size: int,
         padding: int, bottom_align: bool, fill_indices: set[int] | None = None) -> None:
    source = Image.open(SOURCE / source_name).convert("RGBA")
    atlas = Image.new("RGBA", (cols * size, rows * size), (0, 0, 0, 0))
    output_dir = RUNTIME / Path(source_name).stem
    output_dir.mkdir(parents=True, exist_ok=True)
    fill_indices = fill_indices or set()

    for index, (row, col, cell) in enumerate(cells(source, cols, rows)):
        tile = fit(cell, size, padding, bottom_align, index in fill_indices)
        atlas.alpha_composite(tile, (col * size, row * size))
        if index < len(names):
            tile.save(output_dir / f"{index:02d}-{names[index]}.png")

    atlas_name = Path(source_name).stem + "-atlas.png"
    atlas.save(RUNTIME / atlas_name)
    PREVIEW.mkdir(parents=True, exist_ok=True)
    atlas.resize((atlas.width * 8, atlas.height * 8), Image.Resampling.NEAREST).save(PREVIEW / atlas_name)


def main() -> None:
    RUNTIME.mkdir(parents=True, exist_ok=True)
    pack("characters.png", CHARACTERS, 4, 4, 16, 1, True)
    pack("items.png", ITEMS, 8, 5, 16, 1, False)
    pack("world-structures.png", STRUCTURES, 4, 3, 32, 1, True)

    # Opaque terrain tiles fill their 16x16 cell. Props remain trimmed, centered sprites.
    filled_environment = set(range(0, 16)) | set(range(40, 48)) | set(range(56, 60))
    pack("environment-tiles.png", ENVIRONMENT, 8, 8, 16, 1, True, filled_environment)


if __name__ == "__main__":
    main()

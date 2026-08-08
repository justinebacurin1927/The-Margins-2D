#!/usr/bin/env python3
"""Build the Tiny5 runtime bitmap font used by The Margins HUD."""

from math import ceil
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[3]
SOURCE = Path(__file__).with_name("Tiny5-Regular.ttf")
OUTPUT = ROOT / "assets" / "fonts"
FONT_SIZE = 10
ATLAS_SIZE = 128
PADDING = 1
EXTRA_GLYPHS = "\u00d7\u2013\u2014\u2018\u2019\u201c\u201d\u2026\u2190\u2192"


def main() -> None:
    font = ImageFont.truetype(SOURCE, FONT_SIZE)
    ascent, descent = font.getmetrics()
    line_height = ascent + descent
    codepoints = sorted(set(range(32, 127)) | {ord(char) for char in EXTRA_GLYPHS})

    glyphs = []
    for codepoint in codepoints:
        character = chr(codepoint)
        bbox = font.getbbox(character)
        left, top, right, bottom = bbox
        width = max(0, right - left)
        height = max(0, bottom - top)
        advance = max(1, ceil(font.getlength(character)))
        glyphs.append((codepoint, character, left, top, width, height, advance))

    atlas = Image.new("L", (ATLAS_SIZE, ATLAS_SIZE), 0)
    descriptors = []
    x = PADDING
    y = PADDING
    row_height = 0

    for codepoint, character, left, top, width, height, advance in glyphs:
        if width == 0 or height == 0:
            descriptors.append((codepoint, 0, 0, 0, 0, left, top, advance))
            continue
        if x + width + PADDING > ATLAS_SIZE:
            x = PADDING
            y += row_height + PADDING
            row_height = 0
        if y + height + PADDING > ATLAS_SIZE:
            raise RuntimeError(f"Tiny5 glyphs exceed {ATLAS_SIZE}x{ATLAS_SIZE} atlas")

        glyph = Image.new("L", (width, height), 0)
        draw = ImageDraw.Draw(glyph)
        draw.text((-left, -top), character, font=font, fill=255)
        glyph = glyph.point(lambda alpha: 255 if alpha >= 96 else 0)
        atlas.paste(glyph, (x, y))
        descriptors.append((codepoint, x, y, width, height, left, top, advance))
        x += width + PADDING
        row_height = max(row_height, height)

    OUTPUT.mkdir(parents=True, exist_ok=True)
    rgba = Image.new("RGBA", atlas.size, (255, 255, 255, 0))
    rgba.putalpha(atlas)
    rgba.save(OUTPUT / "tiny5.png", optimize=True)

    lines = [
        f'info face="Tiny5" size={FONT_SIZE} bold=0 italic=0 charset="" unicode=1 '
        'stretchH=100 smooth=0 aa=0 padding=0,0,0,0 spacing=1,1',
        f"common lineHeight={line_height} base={ascent} scaleW={ATLAS_SIZE} "
        f"scaleH={ATLAS_SIZE} pages=1 packed=0",
        'page id=0 file="tiny5.png"',
        f"chars count={len(descriptors)}",
    ]
    for codepoint, gx, gy, width, height, left, top, advance in descriptors:
        lines.append(
            f"char id={codepoint:<5} x={gx:<4} y={gy:<4} width={width:<3} height={height:<3} "
            f"xoffset={left:<3} yoffset={top:<3} xadvance={advance:<3} page=0 chnl=15"
        )
    lines.append("kernings count=0")
    (OUTPUT / "tiny5.fnt").write_text("\n".join(lines) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()

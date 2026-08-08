# The Margin Mercenary Graveyard v1

An explorable outdoor structure occupying an **11×9 atlas**: a 9×7 low-walled grave enclosure
inside a one-tile forest transition apron. The runtime renderer slices it into 99 fog-aware 24px
cells. The low fence, graves, memorial, banner, and weapon piles block movement without blocking
line of sight; the south gate and central dirt path remain open.

## Files

- `source/graveyard-chroma.png` — built-in image-generation result on flat magenta.
- `transparent/graveyard.png` — full-resolution source after chroma removal.
- `runtime/graveyard-2x.png` — normalized 528×432 authoring atlas (48px cells).
- `assets/structures/graveyard.png` — 264×216 runtime atlas (24px cells).

## Collision footprint

`~` is the walkable transition apron, `#` is low non-opaque fence, `F` is non-opaque blocking
grave furniture, `.` is walkable graveyard ground, and `+` is the open south gate.

```text
~~~~~~~~~~~
~#########~
~#FF...FF#~
~#FF...FF#~
~#FF...FF#~
~#FF...FF#~
~#.......#~
~####+####~
~~~~~~~~~~~
```


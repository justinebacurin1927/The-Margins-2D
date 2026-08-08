# The Margin world structures v3

This pass replaces the old one-object-per-cell landmark treatment with explorable room
architecture. The first structure is **The Old House**, a roofless three-room cottage with a real
**13×8 architectural footprint** inside a walkable **15×10 layered foundation apron**. Its western
room is a dedicated kitchen.

## Files

- `source/old-house-chroma.png` — built-in image-generation result on flat magenta.
- `transparent/old-house.png` — archival full-resolution source after chroma removal.
- `runtime/old-house-2x.png` — normalized 480×384 authoring canvas (48px per tile).
- `source/old-house-kitchen-chroma.png` — expanded three-room image-generation result.
- `transparent/old-house-kitchen.png` — expanded full-resolution source after chroma removal.
- `runtime/old-house-kitchen-2x.png` — normalized 624×384 authoring canvas.
- `source/old-house-kitchen-clear-chroma.png` — doorway-clear image-generation edit source.
- `transparent/old-house-kitchen-clear.png` — full-resolution edit after chroma removal.
- `runtime/old-house-kitchen-clear-2x.png` — normalized doorway-clear authoring canvas.
- `source/old-house-foundation-chroma.png` — built-in image edit adding the exterior border layer.
- `transparent/old-house-foundation.png` — full-resolution border source after chroma removal.
- `runtime/old-house-foundation-2x.png` — normalized 720×480 authoring canvas.
- `assets/structures/old-house.png` — current 360×240 runtime image (24px per tile).
- `assets/structures/old-house-foreground.png` — transparent south-wall lip rendered above actors.

The runtime renderer slices the final image into 150 independent 24×24 cells. That keeps normal
tile fog, explored-memory tinting, collision, and per-cell reveal behavior. The world model stores
the visual cell in `RogueTileMap.structureTiles`; ordinary `RogueTile` values still own collision.
The front wall's upper stone lip is also sliced into a transparent companion atlas and redrawn after
the actor pass, giving the wall correct foreground occlusion without covering the indoor floor.
Actor grounding uses only the inset 13×8 building body: the exterior `~` apron remains an outdoor
tile layer, so its actors retain outdoor placement and blend into the indoor offset at the doorway.

## Collision footprint

The atlas is fifteen columns by ten rows. `~` is the walkable exterior foundation layer, `#` is an
opaque wall, `k` is walkable kitchen, `.` is another walkable floor, `F` is non-opaque blocking
furniture, `D` is the south entrance, and `+` is an open internal passage.

```text
~~~~~~~~~~~~~~~
~#############~
~#FF#FF...#F.#~
~#Fk#F....#..#~
~#Fk#F.FF.#..#~
~#Fk+..FF.+.F#~
~#Fk#..FF.#FF#~
~#FF#.....#.F#~
~#######D#####~
~~~~~~~~~~~~~~~
```

The structure is generated as the second room in the continuous forest map. Corridors terminate at
the meaningful entrance; random corridor doors remain disabled.

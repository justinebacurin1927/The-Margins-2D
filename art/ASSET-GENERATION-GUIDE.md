# The Margin — Asset Generation Guide

Production pipeline for the art assets of **The Margin** (a dark medieval forest survival
roguelike; grid-based, top-down, **24px tiles**). This document is the master plan and the
handoff instruction for any image-generation AI model. It tells you **what to make, in what
order, with what prompt, and what to do with the result before it goes into the game.**

The Pixel Pack v1 concept atlases already exist in `art/generated/the-margin-pixel-pack-v1/`
and are **wired into the current build** for actors and items. This guide drives the next
generation rounds — runtime-ready assets that replace the remaining placeholder colours and
unlock the later epics.

---

## 0. How to use this document

**If you are an AI image model:** read §1 (the house style) and §2 (the order). Then, to make
any single asset, take the **context handout** (§3) + the asset's **prompt** (§4) and generate.
After generation, follow the **step-by-step pipeline** (§5) before handing the PNG to the game.

**If you are the developer:** generate in §2's order, run each asset through §5, then integrate
per the file map at the end of §5. Don't jump ahead — later assets depend on earlier ones being
on-screen (see "why now" in §2).

---

## 1. The house style (must match v1)

Every asset — concept or runtime — must follow the same discipline so the pack stays cohesive:

- **Look:** crisp, deliberately low-resolution pixel art. Chunky color clusters, hard 1-pixel
  edges, **no antialiasing**.
- **Palette:** moody forest — pine-green, bark-brown, wet-stone gray, muted amber highlights,
  warm skin tones, and small restrained faction accent colors.
- **Game scale:** readable at **24px**. Author at **48px (2×)** so nearest-neighbor downscale
  to 24px is a clean 2:1 (see §5). World structures author at **96px**.
- **Backdrop:** a perfectly flat solid `#ff00ff` chroma-key background — one uniform color, no
  shadows, gradients, texture, floor plane, reflections, or lighting variation inside the sprite.
  The magenta is removed in post (§5).
- **Banned everywhere:** text, labels, numbers, UI, logos, watermarks, cast shadows, and copied
  copyrighted sprite designs. No `#ff00ff` inside any sprite.

---

## 2. The order (generate in this sequence)

| # | Asset | Why now | Consumed by |
|---|-------|---------|-------------|
| 1 | **Tile sheet** | Floor is ~80% of every frame and is still flat green rects; the biggest visual hole | `MarginScreen.setTile` (current build) |
| 2 | **Klein character sheet** | The player, currently one static concept sprite blown down to 24px; directional frames make the grid feel alive | `MarginScreen` player (current build) |
| 3 | **Aldric + Giliman soldier sheets** | In every scene / every fight; same treatment as Klein | `MarginScreen` companion + enemies (current build) |
| 4 | **Light & weather ambience** | Lands on systems already shipped (FOV/light, day-night, weather, campfire) — night currently just dims colors | `MarginScreen` render (current build) |
| 5 | **World structures (runtime pass)** | The 11 structures exist only as 362px concepts; runtime versions unlock the Epic 3 foray loop | Epic 3 |
| 6 | **Combat FX** | Sells the weight of combat — the whole point of Epic 4 ("combat has costs") | Epic 4 |
| 7 | **Traders, currency, companion states** | Nothing earlier wants them; Epic 5/6 need them | Epics 5–6 |
| 8 | **UI chrome** | The text HUD works (a deliberate design choice); UI art is taste-sensitive and last | polish / ship |

**If you only generate one thing: the tile sheet.** It's the largest share of the screen, it
makes the sprites you already wired land on something real, and tile seams are the one class of
AI-generation weakness you want to discover early.

---

## 3. Context handout (paste this to the image model first)

> PROJECT: The Margin — a dark medieval forest survival roguelike. Grid-based, top-down,
> 24px tiles. The world is the forested land of Herois under the Giliman occupation.
>
> STYLE: crisp deliberately low-resolution pixel art, chunky color clusters, hard 1-pixel
> edges, no antialiasing. Moody forest palette: pine-green, bark-brown, wet-stone gray,
> muted amber highlights, warm skin tones, small restrained faction accent colors.
> Readable at 24px game scale; author everything at 2× (48px) so downscale is clean.
>
> INPUT REFERENCES — attach both to every call:
> - Image 1 (`The Margin - Remake/VIsion .png`) is only a presentation, scale-readability,
>   forest-mood reference; do not reproduce its UI, sprites, or layout.
> - Image 2 (`The Margin - Remake/Map.png`) is only a Herois world palette and medieval-setting
>   reference; do not reproduce the map, lettering, borders, or icons.
>
> CONSTRAINTS — every asset: no text, no labels, no numbers, no UI, no logos, no watermark;
> no `#ff00ff` inside any sprite; crisp closed silhouettes; no cast shadows; no copied
> copyrighted sprite designs; perfectly flat solid `#ff00ff` chroma-key background.

---

## 4. Generation prompts (one per asset)

Prompts follow the v1 `PROMPTS.md` format. Replace `{REFERENCES}` with the two reference
images from §3.

### Asset 1 — Tile sheet (runtime-ready, 4×4 = 16 cells, 48px cells → 192×192)

```text
Use case: runtime-ready
Asset type: seamless top-down tile sheet for a 24px-grid forest survival roguelike
Primary request: The game renders every tile as one full cell. Produce a seamless tile
set for the six tile types: floor, wall, door, well, pond, river — with grass/dirt
variants and 2 water animation frames. Edges must tile seamlessly in every direction.
{REFERENCES}
Subjects: exactly 16 tiles —
Row 1: floor grass A, floor grass B (subtle variation), floor dirt path, floor leaf litter
Row 2: wall tree trunk (full-cell tree canopy/trunk blocking the tile), wall tree B,
wall large rock, wooden door (closed, in a walkable frame)
Row 3: stone well ring, pond water A, pond water B, river water A
Row 4: river water B, empty, empty, empty
Style/medium: crisp deliberately low-resolution pixel art, chunky clusters, hard pixel
edges, no antialiasing, moody pine-green / bark-brown / wet-stone-gray / muted-amber
palette, readable at 24x24.
Composition/framing: exactly one seamless tile per equal 48x48 cell; identical edge
pixels left-right and top-bottom so tiles repeat with no visible seam; no borders.
Scene/backdrop: perfectly flat solid #ff00ff chroma-key background; no shadows,
gradients, texture variation beyond the tile's own detail, reflections, or lighting.
Constraints: no text, labels, numbers, UI, logos, or watermarks; no #ff00ff inside any
tile; crisp closed silhouettes; no cast shadows; no copied copyrighted tile designs.
```

### Asset 2 — Klein character sheet (runtime-ready, 4×3 = 12 cells, 48px cells → 192×144)

```text
Use case: runtime-ready
Asset type: top-down character sprite sheet (walk cycle + facings) for a 24px-grid roguelike
Primary request: The player moves in four grid directions (N/E/S/W) and is drawn one
cell per tile. Produce a 4-direction walk cycle for Klein, the young Novelborne knight.
Keep the exact v1 design.
{REFERENCES}
Subjects: Klein, young Novelborne knight — worn gray-blue gambeson, short brown hair,
wooden club. Sheet layout is 4 rows (one per facing, top-down) x 3 columns (walk cycle):
Row 1 facing north (back of head), Row 2 facing east (profile), Row 3 facing south
(toward viewer — the default/idle), Row 4 facing west (profile). Column 2 of each row
is the neutral stance used as the idle frame; columns 1 and 3 are the two walk frames.
Style/medium: crisp deliberately low-resolution pixel art, chunky clusters, hard pixel
edges, no antialiasing, restrained moody forest palette with warm skin and a small
gray-blue accent.
Composition/framing: evenly spaced 4-by-3 atlas, one isolated centered full-body sprite
per equal 48x48 cell, consistent body scale and ground orientation across all 12 cells,
generous separation, no overlap, no cell borders.
Scene/backdrop: perfectly flat solid #ff00ff chroma-key background; no shadows,
gradients, floor plane, reflections, or lighting variation.
Constraints: no text, labels, numbers, UI, logos, or watermarks; no #ff00ff inside any
sprite; crisp complete silhouette; no cast shadows; no copied copyrighted designs.
```

### Asset 3a — Aldric companion sheet (runtime-ready, 4×3 = 12 cells, 48px cells → 192×144)

Same as Asset 2, with this subject block:

```text
Subjects: Aldric, Klein's loyal fellow knight — weathered green-gray armor, short sword.
Sheet layout is identical to the player sheet (4 rows = facings N/E/S/W, 3 columns =
walk cycle, column 2 = neutral idle). Consistent body scale with Klein so the two
stand at the same size on the grid.
```

### Asset 3b — Giliman foot soldier sheet (runtime-ready, 4×3 = 12 cells, 48px cells → 192×144)

Same as Asset 2, with this subject block:

```text
Subjects: a Giliman occupation foot soldier — the faction's cold officer look: dark
black-and-red coat over steel armor, practical helmet, sword or spear. Sheet layout is
identical to the player sheet (4 rows = facings N/E/S/W, 3 columns = walk cycle, column 2
= neutral idle). Body scale consistent with Klein and Aldric.
```

### Asset 4 — Light & weather effects sheet (runtime-ready, 4×2 = 8 cells, 48px cells → 192×96)

```text
Use case: runtime-ready
Asset type: 2D effect sprite sheet (soft light, rain, snow) for a forest survival roguelike
Primary request: Small effect sprites the engine draws on top of the world. Row 1 is the
campfire + darkness set for night; Row 2 is precipitation for the weather system.
{REFERENCES}
Subjects: exactly 8 cells —
Row 1: campfire flame frame A, campfire flame frame B (a 2-frame flicker), campfire glow
(a warm orange radial glow, larger soft-edged), darkness vignette (a soft radial dark
gradient with a clear hole in the center — the light ring the engine subtracts)
Row 2: rain streak A, rain streak B (2 frames of diagonal pale streaks), falling snow
(sparse white dots), empty
Style/medium: chunky readable pixel art. Flame/darkness/gradient cells may use soft
radial falloff (that is their function); everything else keeps hard pixel edges.
Composition/framing: evenly spaced 4-by-2 atlas, one isolated effect per equal cell,
generous clear margins, no cell borders. Effects are drawn blended over the world, so
keep the chroma-key magenta ONLY in the empty background, never inside an effect.
Scene/backdrop: perfectly flat solid #ff00ff chroma-key background.
Constraints: no text, labels, numbers, UI, logos, or watermarks; no #ff00ff inside any
effect; no cast shadows; no copied copyrighted designs.
```

### Asset 5 — World structures (runtime re-author, 4×3 = 12 cells, 96px cells → 384×288)

The 11 structures already exist as concepts in v1 (`world-structures.png`). This round
**re-authors them at game density**, not reuses the 362px cells:

```text
Use case: runtime-ready (re-author of the v1 concept atlas)
Asset type: top-down world-structure sprite sheet for a 24px-grid forest survival roguelike
Primary request: Re-author the eleven Herois forest structures as runtime sprites —
consistent footprint, one complete structure per cell, silhouette still readable when
displayed at 48px. Same order and designs as the v1 concept atlas (cell 12 empty).
{REFERENCES}
Subjects: exactly 11 isolated structures: Hunter's Blind (rickety raised wooden hunting
platform with ladder); Fallen Log Hollow (massive hollow mossy fallen trunk used as
shelter); Forest Shrine (small ancient moss-covered carved stone shrine); Beehive Grove
(cluster of dark trees with several golden hives); Worn Down Kitchen Camp (abandoned
canvas camp, cold cookfire, barrels and utensils); Collapsed Watchtower (half-fallen
timber and stone lookout); Poacher's Camp (hidden rough tents, meat rack and snare);
Sunken Well (old circular stone well with broken winch); The Old House (crumbling
forest cottage with damaged roof and cellar hatch); Mercenary Graveyard (crooked marked
graves, broken weapons, one weathered company banner); Deep Cave Mouth (black rocky
entrance under roots with a few old bones).
Style/medium: crisp deliberately low-resolution pixel art, chunky color clusters, hard
pixel edges, no antialiasing, moody pine-green / bark-brown / wet-stone-gray / muted
amber palette.
Composition/framing: evenly spaced 4-by-3 atlas with the final cell empty; one complete
isolated structure per equal 96x96 cell; consistent approximate footprint scale;
consistent isometric-top-down three-quarter camera; no overlap, no cell borders.
Scene/backdrop: perfectly flat solid #ff00ff chroma-key background.
Constraints: no characters, text, labels, numbers, UI, logos, or watermarks; no #ff00ff
inside any structure; crisp closed silhouettes; no copied copyrighted designs.
```

### Asset 6 — Combat FX sheet (runtime-ready, 4×3 = 12 cells, 48px cells → 192×144)

```text
Use case: runtime-ready
Asset type: 2D combat effect sprite sheet for a grid roguelike
Primary request: Small transient combat effects the engine plays over a tile when an
actor attacks, gets hit, or dies. All cells are short loops or single flashes.
{REFERENCES}
Subjects: exactly 12 cells —
Row 1: slash arc A, slash arc B, slash arc C (a 3-frame swing in front of the attacker)
Row 2: hit spark (white impact flash), block spark (blue/pale deflected flash), blood
puff (small red puff, 1 frame), dust A
Row 3: dust B (2-frame footstep/footfall dust), death collapse A, death collapse B
(a 2-frame collapse to the ground), empty, empty
Style/medium: chunky readable pixel art, hard pixel edges, no antialiasing; sparks and
flashes brighter and higher-contrast than the world so they read instantly.
Composition/framing: evenly spaced 4-by-3 atlas, one isolated effect per equal 48x48
cell, centered in the cell, no cell borders.
Scene/backdrop: perfectly flat solid #ff00ff chroma-key background; chroma only in the
background, never inside an effect.
Constraints: no text, labels, numbers, UI, logos, or watermarks; no #ff00ff inside any
effect; no cast shadows; no copied copyrighted designs.
```

### Asset 7 — Traders, currency & companion states (runtime-ready, 4×3 = 12 cells, 48px / 96px)

```text
Use case: runtime-ready
Asset type: mixed character + icon sprite sheet for a trading and companion system
Primary request: Two traveling traders (full-body, 96px), four tiered currency icons
(48px), and two companion-state frames for Aldric (48px).
{REFERENCES}
Subjects: exactly 12 cells —
Row 1: Black Market trader (layered dark robes — reuse the v1 design), Traveling
Wanderer deserter (satchel — reuse the v1 design), empty, empty
Row 2: currency tier 1, currency tier 2, currency tier 3, currency tier 4 (four
visually distinct coin/barter icons that read as increasing value; concrete names come
from the design bible, Story 6-3 — keep them unnamed here)
Row 3: Aldric companion-state A (steady / neutral), Aldric companion-state B (wounded or
grieving — the shapes-of-loss frame), empty, empty
Style/medium: crisp deliberately low-resolution pixel art, chunky clusters, hard pixel
edges, no antialiasing, restrained moody palette.
Composition/framing: evenly spaced 4-by-3 atlas; characters one per equal 96x96 cell,
icons one per equal 48x48 cell; consistent scale within each group; no cell borders.
Scene/backdrop: perfectly flat solid #ff00ff chroma-key background.
Constraints: no text, labels, numbers, UI, logos, or watermarks; no #ff00ff inside any
sprite; crisp silhouettes; no cast shadows; no copied copyrighted designs.
```

### Asset 8 — UI chrome (last, optional)

The game deliberately uses a text HUD (the message log is the primary text surface). If UI
art is ever wanted, generate a minimal set — panel corner, panel edge, health/status bar
segments — as a single 4×2 sheet at 48px, same style. This is taste-sensitive and the last
thing before a visual polish pass. **Do not generate this until every other asset is in.**

---

## 5. Step-by-step pipeline (what to do AFTER generation)

Run every generated asset through this before it goes into the game. This is the production
note from the v1 README, made concrete.

1. **Slice.** Cut the atlas into its cells at the cell size the prompt specified.
2. **Chroma out.** Remove the flat `#ff00ff` background → transparent PNG. The `transparent/`
   folder of the v1 pack shows the expected result.
3. **Normalize.** Center each sprite on a consistent canvas and anchor: **characters/structures
   anchored feet-at-bottom-center**, items centered, tiles flush to the full cell (no gaps at
   edges — tiles must cover the whole 48×48, not float). Same body scale across all cells of
   one sheet.
4. **Downscale.** Nearest-neighbor to the game scale: **48px → 24px** (2:1), **96px → 24px**
   (4:1). Never bilinear/linear — it blurs the pixel art.
5. **Inspect at game scale.** Put the downscaled result next to the v1 sprites already in the
   build and check: tile seams (edges must be flush — the #1 failure mode), silhouette
   readability at 24px, palette cohesion, animation frames consistent in pose and position.
   **Iterate the prompt and regenerate on any failure** — never accept a muddy sprite into the
   build.
6. **Integrate.** Copy the PNG to `assets/`. Wire the cells into `core/src/main/java/com/margins/PixelPack.java`
   (load + slice) and the draw calls into `MarginScreen.java` (or the Epic 3/4 module when it
   exists). The current build's wiring map:

   | Asset | File | Where it lands |
   |-------|------|----------------|
   | Tiles | `MarginScreen.setTile` (region lookup by `RogueTile` id: WALL 0, FLOOR 1, DOOR 2, WELL 3, POND 4, RIVER 5) | replaces the color switch |
   | Klein / Aldric / Soldier | `MarginScreen` player, companion, enemy draws | replaces the sprite cells now hard-coded (player=0, companion=1, enemy=6) |
   | Effects | `MarginScreen` render pass (overlays after the world) | drives FOV/weather/campfire visuals |
   | Structures | Epic 3 module | foray landmarks |
   | Combat FX | Epic 4 module | transient effects |
   | Traders/currency/states | Epic 5/6 modules | trading + companion |
   | UI | HUD pass | polish |

7. **Verify.** Rebuild offline + run the suite (`mvn -o clean install`), launch
   (`mvn -o -pl desktop exec:java`), screenshot, and compare at game scale. Fix and repeat.

---

## Quick reference — target sheet specs

| Asset | Sheet | Cells | Cell (px, 2×) | Sheet (px) |
|-------|-------|-------|---------------|------------|
| 1. Tiles | 4×4 | 16 | 48 | 192×192 |
| 2. Klein | 4×3 | 12 | 48 | 192×144 |
| 3a. Aldric | 4×3 | 12 | 48 | 192×144 |
| 3b. Soldier | 4×3 | 12 | 48 | 192×144 |
| 4. Light/weather effects | 4×2 | 8 | 48 | 192×96 |
| 5. Structures | 4×3 | 12 | 96 | 384×288 |
| 6. Combat FX | 4×3 | 12 | 48 | 192×144 |
| 7. Traders/currency/states | 4×3 | 12 | 48 / 96 | 384×192 |
| 8. UI chrome | 4×2 | 8 | 48 | 192×96 |

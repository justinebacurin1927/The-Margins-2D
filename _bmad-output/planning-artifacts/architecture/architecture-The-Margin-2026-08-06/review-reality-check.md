# Reality-Check Review — Architecture Spine (The Margin)

Reviewed: `ARCHITECTURE-SPINE.md` (2026-08-06, draft)
Method: read the spine in full; verified every cited build version against `pom.xml`, `core/pom.xml`, `desktop/pom.xml`; verified every cited code symbol against `core/src`; web-verified the "current version" claims against Maven Central / GitHub releases / libgdx.com.
Verdict date: 2026-08-06.

## Verdict

**PASS WITH CAVEATS.** Every Stack-table version is real and matches the poms (the phantom Mockito/AssertJ from an earlier draft is correctly excised); every cited code symbol exists at the cited location, including the AD-8 retirement list (`descend()`, `floorDepth`, `getFloorCount`, `STAIRS_DOWN/UP`, `FloorGenerator.generate(..., floorDepth)`, `TurnEngine.java:120`). The "libGDX latest is 1.14.2" claim is current and correct as of today. Three AD text passages marked `[ADOPTED]` (or phrased as current project state) do not match the code they ratify — the noise-pipeline attribution, the six-stat block, and the per-companion Bond keying. None invalidate the spine; all should be corrected before the spine is treated as authoritative.

---

## 1. Stack table — row by row (poms + web)

| Spine row | Claimed | Verified | Status |
| --- | --- | --- | --- |
| Java | 17 (LTS), `<java.version>` in pom | `pom.xml:20` `<java.version>17</java.version>` | ✅ |
| libGDX | 1.12.1, project-pinned `${gdx.version}` | `pom.xml:19` `<gdx.version>1.12.1</gdx.version>` | ✅ |
| libGDX desktop backend | `gdx-backend-lwjgl3` 1.12.1 | `desktop/pom.xml:22-24` (inherits `${gdx.version}`) | ✅ |
| JUnit Jupiter (tests) | 5.10.2 | `core/pom.xml:21-24` `junit-jupiter` 5.10.2, test scope | ✅ |
| maven-compiler-plugin | 3.11.0 | `core/pom.xml:33` and `desktop/pom.xml:36` | ✅ |
| maven-surefire-plugin | 3.2.5 | `core/pom.xml:42` | ✅ |
| exec-maven-plugin | 3.1.0 | `desktop/pom.xml:46`, `mainClass` = `com.margins.desktop.DesktopLauncher` | ✅ |

- **Mockito/AssertJ correction is verified.** No `org.mockito` / `org.assertj` anywhere in the poms or any `core/src` file; all tests import only `org.junit.jupiter` (confirmed by grep). Test isolation relies on JUnit alone — the spine's rebuttal is accurate.
- **Web check — "libGDX latest is 1.14.2" is current.** GitHub releases (`github.com/libgdx/libgdx/releases`) list **1.14.2 as "Latest"** (published 05 Jun 2026; 1.14.1 was the first 2026 release). libgdx.com versions page and Sonatype both show 1.14.2 as the newest. No 1.14.3/1.15 exists. The "stay on 1.12.1; bump deferred" note is a sound, current decision.
- JUnit 5.10.2 and all three plugin versions are real Maven Central artifacts (released 2022–2023). No phantom versions.
- **Minor gap:** the note "these are the only versions the build actually declares" omits `gdx-platform` (classifier `natives-desktop`), declared at 1.12.1 in `desktop/pom.xml:25-29`. Not a phantom — just an omitted (necessary) runtime artifact from the "only versions" claim. Low severity.

## 2. Code symbols — verification

Every symbol the spine cites (mainly in AD-8's retirement list) was located in `core/src`:

| Cited symbol | Location | Status |
| --- | --- | --- |
| `RunState.floorDepth` / `descend()` (+ `getFloorDepth`/`setFloorDepth`) | `state/RunState.java:46`, `:142`, `:274-275` | ✅ exists, per-floor descent machinery as described |
| `Route.getFloorCount()`, Caravan Road 3 floors | `world/Route.java:14-15, 28` | ✅ exists; `CARAVAN_ROAD` has `floorCount = 3` |
| `RogueTile.STAIRS_DOWN`/`STAIRS_UP` walkable | `RogueTile.java:7-8`; walkable per `:10-12` | ✅ exists, walkable as claimed |
| `FloorGenerator.generate(width, height, rand, floorDepth)` per-floor BSP | `FloorGenerator.java:29` (uses `floorDepth` at `:34`; rooms/corridors/stairs) | ✅ exists, per-floor room/corridor output |
| `TurnEngine.java:120` STAIRS_DOWN descent trigger | `system/TurnEngine.java:119-120` | ✅ exists — the `== RogueTile.STAIRS_DOWN` check is exactly line 120 |
| `Companion.followStep()` greedy follower | `Companion.java:58` | ✅ exists (greedy one-tile step, as AD-10 calls "the current followStep follower") |
| `CompanionSystem` (pipeline step) | `system/CompanionSystem.java` (`follow` `:20`, `distract` `:32`) | ✅ exists |
| `NoiseEvent` | `NoiseEvent.java` | ✅ exists |
| `FlagStore` | `state/FlagStore.java` | ✅ exists |
| `MarginScreen` in `com.margins` (screen layer) | `MarginScreen.java:1,32` | ✅ exists |
| Package layout (`com.margins.rogue.{state,system,item,narrative,save,world}`, `com.margins`, `com.margins.desktop`) | full tree | ✅ exists |
| `SaveService` at `save/SaveService.java` (AD-6) | `rogue/save/SaveService.java` | ✅ exists; uses libGDX Json exactly as AD-6 describes |
| AD-5 `acted=true` turn economy (companions act only on player-acted turns) | `TurnEngine.java:32,114` (companion follow only inside `if (acted)`) | ✅ adopted as claimed |
| AD-2 no render types in `RunState` / core | `RunState.java` has zero libGDX imports; only `rogue/save/SaveService.java` imports libGDX (`Gdx`, `files.FileHandle`, `utils.Json/JsonWriter`) — none are render/input/graphics | ✅ holds as literally written |

**Starter note (greenfield clause):** N/A. This is brownfield; the build is a hand-written Maven parent/module layout, not a gdx-liftoff/gdx-setup generated starter, so there is no starter-default drift to check. The desktop entrypoint `com.margins.desktop.DesktopLauncher` is present and matches `exec-maven-plugin`'s `mainClass`.

## 3. Findings

### F1 — MEDIUM — AD-9 (marked `[ADOPTED]`) mis-describes the noise pipeline it ratifies
`ARCHITECTURE-SPINE.md:113-115`. AD-9 rule: *"Combat, distraction, and movement emit `NoiseEvent`s into a per-turn queue; `DetectionSystem` reads the queue; nothing directly manipulates an enemy because a noise was emitted."*
The code reads the queue in **`NoiseSystem.resolve`** (`system/NoiseSystem.java:22-39`) — the AD-4 "Noise resolve" step — not `DetectionSystem` (which is the vision/LOS step, `DetectionSystem.java:22`). And noise resolution **does directly mutate enemies**: `NoiseSystem.java:31-35` calls `e.setDetection(SUSPICIOUS)`, `e.setLastSeen(...)`, `e.setCalmTurns(0)`. Also, only **combat** (`CombatSystem.java:30`) and **distraction** (`CompanionSystem.java:43`) emit noise today; player movement and companion follow emit none, so "movement emits NoiseEvents" is not in the ratified code. AD-10's party-stealth clause (`:125`) repeats the mis-attribution ("`DetectionSystem` consumes it"). The spine's own intent — noise feeds a detection chain and never damages/moves an actor directly — is real; the rule text should say `NoiseSystem` reads the queue and noise mutates awareness/investigation state (never damage/position). As written, an implementer could route noise through the wrong system or wrongly assume enemies are never touched by noise.

### F2 — MEDIUM — "Stats & status" convention row asserts a six-stat block + per-entity Status block that do not exist
`ARCHITECTURE-SPINE.md:182`. The row states the six-stat block **(STR / GRIT / INS / AG / VOICE / SKILL)** "lives on `RoguePlayer` / `Companion`" and that debuffs form "a closed shape living on each Status block (player, companion, enemy)." The code has a **four**-stat block on `RoguePlayer` only — `str`, `instinct`, `grit`, `voice` (`RoguePlayer.java:47-50`, initialized `:66-69`). There is **no `ag` and no `skill` stat**; the Trembling "Agility" penalty is explicitly folded into instinct (`RoguePlayer.java:123-131`). `Companion.java` carries **no stats at all** (bindId, position, distractionsLeft only), and `RogueEnemy.java` has no Status block. This row is phrased as current project convention but is aspirational, and it contradicts the spine's own Deferred entry ("Companion stat granularity — deferred," `:231`). Implementers must not assume `skill`/`ag` getters exist on `RoguePlayer`, or any stats on `Companion`.

### F3 — LOW/MEDIUM — AD-7 (marked `[ADOPTED]`) claims per-companion Bond keying that the ratified code does not have
`ARCHITECTURE-SPINE.md:99`. AD-7 rule: *"Bond is keyed per-companion (one value per roster member, keyed by `bindId` — the roster is four; a single Bond value would be wrong)."* The ratified code uses a **single** Bond key, `FlagStore.KEY_BOND = "bond.galleon"` (`FlagStore.java:20`), a single active companion (`bindId` `"galleon"` spawned in `RunState.spawnStartingCompanion`, `RunState.java:203-207`), and the test suite asserts that single-value API (`RunStatePersistenceTest.java:34,41,50,53`). The "roster is four" per-`bindId` keying is design ambition (AD-10's Aldric/Mara/Old Fen/Yenna) presented as an adopted invariant. The single Bond value is correct for the current one-companion roster; either drop the `[ADOPTED]` tag and mark per-companion keying as a required change, or the text contradicts the code it ratifies.

### F4 — LOW — Stack note "these are the only versions the build actually declares" omits `gdx-platform` natives-desktop
`ARCHITECTURE-SPINE.md:197`. `desktop/pom.xml:25-29` declares `gdx-platform` (classifier `natives-desktop`) at 1.12.1 — the LWJGL3 backend needs it at runtime. Not a phantom (the point of that sentence is the Mockito/AssertJ rebuttal, which is correct), but the "only versions" claim is technically incomplete.

### F5 — INFO — AD-8 "Route floor-list model" is slightly mis-worded
`ARCHITECTURE-SPINE.md:107`. `Route` is not a floor-list; it is a single named route holding one `floorCount` int (`Route.java:17-25`). Purely a wording nit — the retirement of the descent machinery itself is verified exactly as cited (including `TurnEngine.java:120`).

## 4. Confirmations worth recording

- AD-8 is an honest, accurately-referenced breaking deviation: the descent machinery it retires is all real, and the git history confirms the brownfield basis (`70b4ed4 "Strip old prototype to reusable core for The Margin rebuild"`; `8f05391 "Epic 6: The Caravan Road — route progression through procedural floors"`).
- AD-4's pipeline order text matches `TurnEngine.advance` exactly (Hunger → Detection → Companion follow → Enemy AI → Noise resolve → Last Stand → FOV/cleanup), and AD-4's own note correctly flags that the "Companion follow" step is superseded by Companion AI per AD-10.
- AD-6's transient-field list (`route`, `rng`, `noiseQueue`, per-turn `lastStand`) matches `RunState.java:45-52`; `SaveService` + `RunStatePersistenceTest` confirm libGDX-Json serialization with `restoreAfterLoad()` re-injection.
- The three build plugin versions and JUnit 5.10.2 are genuine Maven Central artifacts; the libGDX "latest = 1.14.2" claim is correct as of 2026-08-06.

---

### Sources
- Project poms: `/home/jaycee/Projects/The Margins 2D/pom.xml`, `core/pom.xml`, `desktop/pom.xml`
- Core sources: `/home/jaycee/Projects/The Margins 2D/core/src/main/java/com/margins/rogue/...` (paths per the tables above)
- Web: [libgdx/libgdx releases](https://github.com/libgdx/libgdx/releases) (1.14.2 = Latest, 2026-06-05), [libGDX versions](https://libgdx.com/dev/versions/), [Sonatype — com.badlogicgames.gdx:gdx](https://central.sonatype.com/artifact/com.badlogicgames.gdx/gdx), [MVN Repository — junit-jupiter](https://mvnrepository.com/artifact/org.junit.jupiter/junit-jupiter)

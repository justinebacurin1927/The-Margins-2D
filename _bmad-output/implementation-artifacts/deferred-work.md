# Deferred Work

## Deferred from: code review of story-3.1 (2026-07-19)

- **`equip` same-type-in-both-slots semantics** — `equip(type)` currently allows the same item type to occupy both equipped slots. Confirm the intended model (distinct gear vs. duplicate copies) when the equip UI is built in Story 3.2. [core/src/main/java/com/margins/rogue/item/Inventory.java]
- **Equipped-item dead-end** — `unequip` into a full backpack is refused (no item loss, correct) and `drop()` only scans the backpack, so a player with a full backpack + equipped items has no way to shed an equipped item. Add an unequip-with-drop path alongside the drop UI in Story 3.2. [core/src/main/java/com/margins/rogue/item/Inventory.java]

## Deferred from: code review of story-3.2 (2026-07-19)

- **Make-room swap spends two turns** — the drop-or-leave `[D]` → `[X]` gesture runs `drop` then `pickup` as two separate turns, so enemies double-move and hunger ticks twice for one player action. Refactor to a single-turn swap when the inventory UX is revisited. [core/src/main/java/com/margins/rogue/RogueGameScreen.java, handleInventoryInput]
- **No per-tile item stacking / chooser** — multiple `FloorItem` stacks can sit on one tile; pickup and the rendered marker both act on the first stack with no disambiguation, and the marker shows no count. Add stacking-on-drop or a pickup chooser. [core/src/main/java/com/margins/rogue/state/RunState.java, takeItemAt / RogueGameScreen floor-item render]

## Deferred from: code review of story-3.3 (2026-08-03)

- **Pre-3.3 save reloads with a non-deterministic identity binding** — a save written before the `identifyMap` field existed (no `identifyMap` key in JSON) does not crash (the no-arg `RunState()` ctor builds a binding, so the field is non-null and `identityOf` is null-guarded), but that binding is seeded from `System.nanoTime()` rather than rebuilt from the stored `seed` — so reloading the same pre-3.3 save yields a different binding each time. Task 3's dropped seed-rebuild fallback would have fixed this deterministically; dropping it (Deviation 2) misdiagnosed the branch as a null-guard. Negligible real-world risk (single-slot, no-migration AD-6, field ships with 3.3). A clean fix needs a save-version/sentinel since the non-null field defeats a null check. [core/src/main/java/com/margins/rogue/state/RunState.java, restoreAfterLoad]

## Deferred from: code review of story-3.4 (2026-08-03)

- **Identify arrays not resized on cross-version save load (enum growth)** — `IdentifyMap.markIdentified` guards on `Supply.count()` but indexes `identifiedByOrdinal`; a save serialized under a smaller `Supply` enum deserializes a shorter (non-null) array, so the first use of a newly-added supply type would `ArrayIndexOutOfBounds`. Not reachable in the current build (array is always `Supply.count()` long); only manifests if a future story adds a `Supply` constant and an in-flight save is resumed across that change. Shares the same latent limitation as 3.3's `boundByOrdinal`. If cross-version save robustness is ever wanted, resize both identify arrays on load (or grow on demand). [core/src/main/java/com/margins/rogue/state/IdentifyMap.java, markIdentified]

## Deferred from: code review of Epic 4 (4.1 / 4.2 / 4.3) (2026-08-03)

- **Companion may be placed on / step onto an enemy or STAIRS_DOWN tile** (story 4.1) — `RunState.companionSpotNear` and `Companion.followStep` use a walkable-only check with no enemy/stairs exclusion, so the ally can render stacked on an enemy or spawn on the down-stairs. The companion is a non-colliding ally by design, so this is cosmetic overlap rather than a gameplay blocker; ally-collision policy is a deliberate Epic 6 concern. [Low] [core/src/main/java/com/margins/rogue/state/RunState.java companionSpotNear, Companion.java followStep]
- **No committed JUnit test suite for the new model** (story 4.3, cross-cutting) — FlagStore round-trip and companion/identify coverage exist only as throwaway harnesses, so the persistence contract is not guarded by a committed test. This is the standing Epic 1/3 retrospective critical-path item (stand up a JUnit 5 core test source root and port the harnesses). The 4.3 round-trip itself was verified passing by its harness before deletion. [Low]

## Deferred from: code review of Epic 5 (5.1 / 5.2 / 5.3) (2026-08-03)

- **Debug `T` dialogue trigger + `SampleDialog` ship in the build** (story 5.1) — spec-sanctioned scaffolding so FR-6 is exercisable; it opens a placeholder scene and (via 5.3) spawns a real persisted cache. Epic 6 replaces the trigger with authored triggers/content and should remove or gate the debug key. [Low] [core/.../rogue/RogueGameScreen.java (T handler), narrative/SampleDialog.java]
- **Dialogue input caps selectable choices at 4** (story 5.1) — only `NUM_1..NUM_4` are mapped; matches FR-6's "1–4 choices" cap, so 4 is by design, but a 5+-option authored node would have dead choices. Add a defensive cap/assert if authored content ever approaches the limit. [Low] [core/.../rogue/RogueGameScreen.java (handleDialogueInput)]
- **`SceneEffects` cache keys are single-run-scoped globals** (story 5.3) — `scene.cache.*` would collide if a second authored cache is added; needs per-scene keying for multiple caches. [Low] [core/.../rogue/narrative/SceneEffects.java]
- **Bond-tag dialogue wiring absent** (story 5.3, cross-cutting) — `FlagStore.applyBondTag` (built in 4.3) is never called by the dialogue path; no Epic 5 AC requires it (the honest-reunion Bond raise is the Epic 6 reunion). The 4.3 `FlagStore` doc comment slightly overclaims ("Epic 5 dialogue nodes call this") until Epic 6 wires it. [Low] [core/.../rogue/state/FlagStore.java, narrative/DialogController.java]
- **`applyCacheReveal` authoring-contract robustness** (story 5.3) — gates on `== 1` (a `withFlag(key, 2)` never spawns) and lacks the null-guards `DialogController` has; unreachable today, revisit (`!= 0`, consistency guards) when authored scenes use non-1 flag values. [Low] [core/.../rogue/narrative/SceneEffects.java]

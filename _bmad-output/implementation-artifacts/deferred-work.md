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

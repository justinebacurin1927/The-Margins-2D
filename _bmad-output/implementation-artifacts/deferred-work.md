# Deferred Work

## Deferred from: code review of story-3.1 (2026-07-19)

- **`equip` same-type-in-both-slots semantics** — `equip(type)` currently allows the same item type to occupy both equipped slots. Confirm the intended model (distinct gear vs. duplicate copies) when the equip UI is built in Story 3.2. [core/src/main/java/com/margins/rogue/item/Inventory.java]
- **Equipped-item dead-end** — `unequip` into a full backpack is refused (no item loss, correct) and `drop()` only scans the backpack, so a player with a full backpack + equipped items has no way to shed an equipped item. Add an unequip-with-drop path alongside the drop UI in Story 3.2. [core/src/main/java/com/margins/rogue/item/Inventory.java]

## Deferred from: code review of story-3.2 (2026-07-19)

- **Make-room swap spends two turns** — the drop-or-leave `[D]` → `[X]` gesture runs `drop` then `pickup` as two separate turns, so enemies double-move and hunger ticks twice for one player action. Refactor to a single-turn swap when the inventory UX is revisited. [core/src/main/java/com/margins/rogue/RogueGameScreen.java, handleInventoryInput]
- **No per-tile item stacking / chooser** — multiple `FloorItem` stacks can sit on one tile; pickup and the rendered marker both act on the first stack with no disambiguation, and the marker shows no count. Add stacking-on-drop or a pickup chooser. [core/src/main/java/com/margins/rogue/state/RunState.java, takeItemAt / RogueGameScreen floor-item render]

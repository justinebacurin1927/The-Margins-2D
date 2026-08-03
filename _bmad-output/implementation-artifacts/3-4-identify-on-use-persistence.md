---
baseline_commit: 7ccd105
---

# Story 3.4: Identify-on-use persistence

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As Justine (player),
I want using one unknown supply to reveal that whole type,
so that risk converts to knowledge for the rest of the run (FR-12).

## Acceptance Criteria

1. **Given** an unidentified Supply type, **When** I use one, **Then** its bound true identity is applied (unchanged from 3.3) **and** that type becomes identified for the rest of the run. (FR-12)
2. **Given** a type I have just identified, **When** I view the inventory panel, **Then** every remaining supply of that type — backpack stacks, equipped slots, and any later-picked-up stack of the same type — shows its **true identity name** (e.g. "Stale bread"), not the unidentified type name ("Wrapped Bundle"). (FR-12)
3. **Given** an unidentified type, **When** it is shown anywhere in the UI or messages, **Then** it still shows the unidentified type name until first use; identification is per-type (all instances flip together, AD-12), and one type's reveal does not reveal any other type.
4. **Given** a run where I identified a type, **When** I save and resume (or the run is reloaded from disk), **Then** the identified type is still revealed — the `identified` state persists with `RunState` (AD-6). A save written **before** this story (no `identified` field) resumes safely with nothing identified (no crash).

**Architectural definition-of-done:**

5. The `identified` set lives on the existing `IdentifyMap` (AD-12), beside the per-seed binding from 3.3. It contains no libGDX types (AD-2), serializes with `RunState` (AD-6), and is the **single source of truth** for both "is this type identified?" and "what name do I show for this type?" — no duplicate identified-state on the screen, inventory, or player.

## Product decisions (recommended defaults baked in)

- **The Sealed Letter is exempt from reveal.** `SEALED_LETTER` is not consumed on use (`isConsumedOnUse() == false`); its USE path is the inert "Milek can't read it" no-op that spends no turn. It has one possible identity (`INERT_LETTER`, display "Sealed letter"), whose name already matches its unidentified name — so there is nothing to reveal. **Do not mark it identified**: reveal fires only on a real, consumed use, which keeps the reveal tied to spending the gamble. (If you'd rather flip the letter too, it's a one-line change, but it buys nothing and muddies "reveal = you spent it".)
- **Reveal happens the moment the bound effect is applied**, in the same USE turn — not on a separate "inspect" action (none exists in the MVP). The reveal message replaces the current generic "Used X" so the player sees what it was.

## Tasks / Subtasks

- [x] **Task 1 — Add the `identified` set to `IdentifyMap`** (AC: 1, 2, 5)
  - [x] In `core/src/main/java/com/margins/rogue/state/IdentifyMap.java`, add a plain `boolean[] identifiedByOrdinal` (length `Supply.count()`), parallel to `boundByOrdinal`. Allocate it in `build(Random)` right after `boundByOrdinal` so a fresh run starts with nothing identified.
  - [x] Add `boolean isIdentified(int supplyOrdinal)` — false when the array is null or the ordinal is out of range (defensive, mirrors `identityOf`'s bounds guard).
  - [x] Add `void markIdentified(int supplyOrdinal)` — sets the flag; if `identifiedByOrdinal` is null (a pre-3.4 save, see Task 4) lazily allocate it to `new boolean[Supply.count()]` first, then set. Ignore out-of-range ordinals.
  - [x] Add `String displayNameFor(int supplyOrdinal)` — the **single naming authority**: if `isIdentified(ordinal)` return `identityOf(ordinal).displayName()` (the TrueIdentity name); otherwise return `Supply.byOrdinal(ordinal).displayName()` (the unidentified type name). Guard the null Supply / null identity cases the way the existing callers do (fall back to a plain `"Item " + ordinal` only if Supply is null, matching the screen's current fallback).
  - [x] Keep the class free of libGDX types (AD-2). Update the class Javadoc: 3.3's note said "the paired `identified` set arrives in Story 3.4" — this story delivers it.

- [x] **Task 2 — Reveal on use in `TurnEngine`** (AC: 1, 3)
  - [x] In `core/src/main/java/com/margins/rogue/system/TurnEngine.java` USE case (currently ~lines 52–67): after resolving `TrueIdentity id` and applying it, and **inside the `isConsumedOnUse()` branch** (so the inert letter is not flagged), call `state.getIdentifyMap().markIdentified(action.itemType)` **before** building the message.
  - [x] Replace the reveal message. On the use that identifies the type, show the reveal, e.g. `result.messages.add(s.displayName() + ": " + id.displayName() + "!")` (→ "Wrapped Bundle: Stale bread!"). On uses of an already-identified type, `"Used " + id.displayName()`. Simplest correct form: capture `boolean wasIdentified = imap.isIdentified(type)` **before** marking, then branch the message on it. Effect application and consumption are unchanged from 3.3.

- [x] **Task 3 — Route all Supply names through `displayNameFor`** (AC: 2, 3)
  - [x] `RogueGameScreen.renderInventoryPanel()` — backpack loop (~line 283–285) and equipped loop (~line 294–295): replace `Supply.byOrdinal(type).displayName()` with `state.getIdentifyMap().displayNameFor(type)`. Keep the `> `/`  ` cursor prefix, the `x`count suffix, and the `empty` label for `et < 0` exactly as-is.
  - [x] `TurnEngine` DROP message (~line 74) and PICKUP message (~line 84): use `state.getIdentifyMap().displayNameFor(type)` so dropping/picking an already-identified type reads with its true name, and an unidentified one stays hidden. (The USE message is handled in Task 2.)
  - [x] Grep for any other `Supply....displayName()` UI/message call site and route it through `displayNameFor` too, so there is exactly one place that decides identified-vs-not. Do **not** change `Supply.displayName()` itself — it stays the unidentified name and is the fallback inside `displayNameFor`.

- [x] **Task 4 — Persistence + pre-3.4 save safety** (AC: 4, 5)
  - [x] `identifiedByOrdinal` is a plain `boolean[]` on the already-persisted `IdentifyMap` field of `RunState`, so it serializes with the run via libGDX `Json` (AD-6) with no `RunState` change. Verify the round-trip actually restores it (booleans in an array serialize cleanly; no `setElementType` needed as for enum/object arrays, but confirm in the harness).
  - [x] **Pre-3.4 save:** a save written under 3.3 has an `IdentifyMap` with `boundByOrdinal` but no `identifiedByOrdinal` → the field deserializes as `null`. The null-guards in `isIdentified` (returns false) and `markIdentified` (lazy-allocates) mean such a run resumes with nothing identified and never NPEs. No `restoreAfterLoad` change is required; do **not** rebuild or reset the array on load (that would wipe a legitimately-identified resumed run). Confirm this with the stripped-field harness case below.
  - [x] Do not add an identified-reset anywhere except a genuinely new run. `restart()` already calls `IdentifyMap.build(rng)`, which allocates a fresh all-false array — so a new run correctly starts unidentified with no extra code.

- [x] **Task 5 — Verification** (AC: 1, 2, 3, 4, 5)
  - [x] Extend the 3.3 headless harness (throwaway `main`, same pattern):
    - Before any use: `displayNameFor(type)` equals the Supply's unidentified name for all 5 types; `isIdentified` all false.
    - After using one of a consumable type: `isIdentified(type)` true; `displayNameFor(type)` equals the **bound** identity's name; a second stack / equipped instance of the same type also reports the true name (per-type flip, AC-2); the bound effect still applied (3.3 behavior intact).
    - Isolation: identifying one type leaves the other four unidentified (AC-3).
    - Letter exemption: attempting to USE `SEALED_LETTER` does not set it identified and spends no turn (AC per product decision).
    - Reveal message: the identifying use yields the "Type: Identity!" message; a subsequent use of the same type yields "Used Identity".
    - Json round-trip: identify a type, `toJson`→`fromJson`→`restoreAfterLoad`, assert the type is still identified and names still resolve to the true identity (AC-4).
    - Pre-3.4 save: hand-build/strip a saved `IdentifyMap` JSON so `identifiedByOrdinal` is absent, load it, assert no crash and `isIdentified` all false, then a use identifies normally (AC-4).
  - [x] Re-run the 3.3 identify harness (expect 8/8) and the 3.1 regression (expect 37/37) — no regressions.
  - [x] `mvn -o -pl core install` then live boot on `:0` (~8–10s): pick up a couple of supplies, use one, confirm the panel flips that type to its true name and other types stay hidden → clean.

### Review Findings

- [x] [Review][Defer] Identify arrays not resized on cross-version save load (enum growth) [core/src/main/java/com/margins/rogue/state/IdentifyMap.java:markIdentified] — deferred, pre-existing. `markIdentified` guards on `Supply.count()` but indexes `identifiedByOrdinal`; a save serialized under a *smaller* `Supply` enum deserializes a shorter (non-null) array, so the first use of a newly-added supply type would `ArrayIndexOutOfBounds`. Not reachable in the current build (the array is always `Supply.count()` long) — it only manifests if a future story adds a `Supply` constant and an in-flight save is resumed across that change. Shares the same latent limitation as 3.3's `boundByOrdinal`. Out of scope under AD-6 (single slot, no save-migration contract). (source: blind+edge)

## Dev Notes

### Governing architecture
- **AD-12 — Identify-by-use binding on `RunState`.** `RunState` holds the `SupplyType → TrueIdentity` map built from the seed RNG **plus an `identified` set**. 3.3 delivered the binding and explicitly deferred the `identified` set to this story; 3.4 completes AD-12 by adding that set to the same `IdentifyMap`. Identity is per-type-per-seed, so the stackable type/count `Inventory` is ratified, not changed. [Source: ARCHITECTURE-SPINE.md#AD-12]
- **AD-6 — Save = whole `RunState`.** The `identified` set persists so a resumed run keeps what the player has learned. `IdentifyMap` is already a persisted (non-transient) field of `RunState` (`RunState.java:34`), so a plain `boolean[]` rides along with no serializer wiring. [Source: ARCHITECTURE-SPINE.md#AD-6; RunState.java:34,120–122,150]
- **AD-2 — no libGDX in the model.** `IdentifyMap`, `Supply`, `TrueIdentity` stay pure model. The reveal is model state; the screen only *reads* `displayNameFor`. [Source: ARCHITECTURE-SPINE.md#AD-2]
- **AD-5 — single seeded RNG.** Unchanged here — 3.4 adds no randomness. The binding still comes from `RunState.rng()` (built in the constructor, `RunState.java:62–64`). [Source: ARCHITECTURE-SPINE.md#AD-5]

### Builds on Story 3.3 (current HEAD, 7ccd105)
- `IdentifyMap` currently holds only `boundByOrdinal` and `identityOf(int)` — see the class doc's own note: *"The paired `identified` set (AD-12) arrives in Story 3.4."* Add the set here; keep `build`, `identityOf`, the no-arg Json constructor, and the bounds-guard style intact. [Source: core/src/main/java/com/margins/rogue/state/IdentifyMap.java]
- `TurnEngine` USE already resolves the effect via `state.getIdentifyMap().identityOf(action.itemType)` and consumes via `Supply.isConsumedOnUse()` (TurnEngine.java:52–67). 3.4 adds the `markIdentified` call and swaps the message — do not touch effect application or consumption. [Source: core/src/main/java/com/margins/rogue/system/TurnEngine.java:52–92]
- `Supply.displayName()` is the **unidentified** name and must stay that way — it is the fallback branch inside `displayNameFor`. `TrueIdentity.displayName()` is the revealed name. [Source: core/src/main/java/com/margins/rogue/item/Supply.java:39–41; item/TrueIdentity.java:34–36]
- 3.3's Deviation 2 established that libGDX `Json` always constructs `RunState`/`IdentifyMap` via the no-arg constructor and then sets fields — which is exactly why a pre-3.4 `identifiedByOrdinal` deserializes as `null` rather than being absent-and-defaulted. Handle it with the null-guards in Task 1/4, not a rebuild. [Source: 3-3-per-seed-supply-identity.md#Completion Notes — Deviation 2]

### Files being modified — current state and what must be preserved
- **`state/IdentifyMap.java`** (UPDATE): today = binding + `identityOf` only. Add the identified array + `isIdentified`/`markIdentified`/`displayNameFor`. Preserve: no-arg ctor for Json, `build(Random)` signature, `identityOf` bounds behavior, zero libGDX imports.
- **`system/TurnEngine.java`** (UPDATE): today USE applies bound effect, consumes, and emits "Used {unidentified name}"; DROP/PICKUP emit their own messages. Add the reveal (consumed branch only) + true-name messages. Preserve: the inert-letter no-turn path (lines 61–65), `acted` bookkeeping, DROP/PICKUP turn semantics.
- **`rogue/RogueGameScreen.java`** (UPDATE): `renderInventoryPanel()` (lines 264–296) draws backpack + equipped names via `Supply.displayName()`. Swap to `displayNameFor`. Preserve: cursor prefix, `x`count, `empty` slot label, panel layout/coords.

### Scope boundary
- **IN:** the per-type `identified` set, reveal-on-use, routing every Supply name through one resolver, and persistence (incl. pre-3.4 save safety).
- **OUT:** any new "inspect/examine" action, floor-item labels on the map (supplies are named only in the panel/messages today — do not add map labels), status effects, and any change to the per-seed binding or the effect magnitudes (those are 3.3 / post-MVP).

### Testing standards
- No committed JUnit suite yet — use the throwaway-`main` headless harness plus a live boot, exactly as 3.1/3.2/3.3 did. Prove: reveal-on-use, per-type flip across all instances, type isolation, letter exemption, message change, Json round-trip, and pre-3.4-save safety.
- **Build quirk:** run `mvn -o -pl core install` before any live boot (stale-artifact quirk noted in the Epic 1 retro); boot `:0` for ~8–10s.

### References
- [Source: _bmad-output/planning-artifacts/epics.md#Epic 3 · Story 3.4 (FR-12)]
- [Source: ARCHITECTURE-SPINE.md#AD-12, #AD-6, #AD-2, #AD-5]
- [Source: 3-3-per-seed-supply-identity.md — IdentifyMap/TrueIdentity model this story extends; Deviation 2 on Json construction]
- [Source: core/src/main/java/com/margins/rogue/state/IdentifyMap.java; system/TurnEngine.java; rogue/RogueGameScreen.java; item/Supply.java; item/TrueIdentity.java; state/RunState.java]

### Project Structure Notes
- All changes land in existing files under `core/.../rogue/{state,system,item}` and `core/.../rogue/RogueGameScreen.java` — matches the architecture Structural Seed (`state/IdentifyMap.java` owns the identify state; `item/Supply.java` the type). No new files, no new modules.

## Dev Agent Record

### Agent Model Used

claude-opus-4-8[1m] (via bmad-dev-story)

### Debug Log References

- `mvn -o -pl core install` → BUILD SUCCESS (with and without the harness).
- Story 3.4 headless harness (throwaway `main`, run via `exec-maven-plugin:3.1.0:java`): **30/30 PASS** — pre-use names hidden for all 5 types; bound-identity apply changes hp/hunger; use reveals the type and its name flips to the bound identity; one consumed, remaining same-type stack shows the true name (per-type reveal); reveal message = "Wrapped Bundle: Spoiled meat!", second use = "Used Spoiled meat"; the other 4 types stay hidden (isolation); Sealed Letter is neither identified nor consumed; identify survives a Json round-trip; a pre-3.4 save (stripped `identifiedByOrdinal`) loads with nothing identified and no crash, then a use identifies via lazy alloc; same seed → same binding (3.3 intact).
- Harness deleted after the run (no committed JUnit suite; mirrors the 3.1/3.2/3.3 throwaway pattern).
- Live boot on `:0` (~14s) → clean, no exceptions.

### Completion Notes List

- **`identified` set added to `IdentifyMap`** as a plain `boolean[] identifiedByOrdinal` beside the 3.3 binding, completing AD-12. New methods: `isIdentified`, `markIdentified` (lazy-allocates for pre-3.4 saves), and `displayNameFor` — the single naming authority (revealed `TrueIdentity` name once identified, else the hidden `Supply` name). No libGDX types (AD-2).
- **Reveal on use** in `TurnEngine`'s USE case, inside the consumed branch only (so the inert Sealed Letter is exempt): capture `wasIdentified`, `markIdentified`, then emit a reveal message ("Type: Identity!") on the first use and "Used <identity>" thereafter. Effect application and consumption are unchanged from 3.3.
- **All Supply names routed through `displayNameFor`**: inventory panel (backpack + equipped) in `RogueGameScreen`, plus the USE/DROP/PICKUP messages in `TurnEngine`. `Supply.displayName()` is untouched (still the hidden name, and the fallback inside `displayNameFor`). Removed the now-orphaned `Supply` import from `RogueGameScreen` (its only two uses were replaced).
- **Persistence** needed no `RunState` change: `identifiedByOrdinal` rides the already-persisted `IdentifyMap` field via libGDX Json (AD-6); primitive boolean arrays serialize without `setElementType`. Pre-3.4 saves (field absent → null) are handled by the null-guards in `isIdentified`/`markIdentified`; no `restoreAfterLoad` change, and `restart()`'s existing `IdentifyMap.build` gives a fresh all-false set for new runs.
- **Note (not a bug):** at seed 12345 Wrapped Bundle binds to Spoiled Meat, so a second use drops HP to 0 and the Last Stand system appends its own message after "Used …". The reveal message is still emitted; the harness asserts message *presence*, not that it is last.

### File List

- MODIFIED: core/src/main/java/com/margins/rogue/state/IdentifyMap.java (added `identified` set + `isIdentified`/`markIdentified`/`displayNameFor`)
- MODIFIED: core/src/main/java/com/margins/rogue/system/TurnEngine.java (reveal-on-use in USE; DROP/PICKUP messages via `displayNameFor`)
- MODIFIED: core/src/main/java/com/margins/rogue/RogueGameScreen.java (inventory panel names via `displayNameFor`; dropped orphaned `Supply` import)

## Change Log

- 2026-08-03: Implemented Story 3.4 — per-type `identified` set on `IdentifyMap` (completes AD-12); reveal-on-use in `TurnEngine` (consumed items only, Sealed Letter exempt); all Supply names routed through the single `displayNameFor` resolver; identify persists with `RunState` and tolerates pre-3.4 saves via null-guarded lazy alloc. Verified 30/30 headless harness + clean build & live boot. Status → review.

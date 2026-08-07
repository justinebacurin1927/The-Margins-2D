# Story 1.6: Temperature and the campfire/torch

Status: done
baseline_commit: 56ec734

## Story

As Klein,
I want cold and heat to threaten me and fire to be the mitigation,
so that warmth is a real choice under scarcity (FR-7).

## Acceptance Criteria

1. **Given** a Cold Snap with no mitigation, **when** ~38 turns pass, **then** Temperature reaches Frozen (inside one 70-turn Night), with recovery ≈ half the onset rate.
2. **Given** I build a campfire, **when** it is lit, **then** it provides warmth + light + a cooking/purification station, is stationary, and is exposed (visible/audible to patrols per Story 1.4).
3. **Given** Wood and Coal in inventory, **when** I craft a torch, **then** I get a light-only source with a 60-turn burn (≈ one Night).
4. **Given** a fire has drawn a patrol, **when** I abandon the camp to flee into fog, **then** I lose the camp's cooking/purify/warmth benefits for the night — trading heat for stealth (UJ-1 edge case).

## Scope & the 1.6↔1.7 seam (read first — prevents over-building)

This story makes Temperature **real**: a weather driver (Cold Snap), a campfire warmth mitigation, and the **torch** (Wood+Coal craft → a 60-turn carried light). It is the direct carry of two earlier deferrals: Story 1.2's "driver-less temperature drift needs re-balancing under a real driver (Cold Snap −2.0/turn) and the campfire mitigation", and Story 1.4's "validate the light tile when the campfire/torch lights it".

**The 1.7 seam holds:** the tiered debuff pipeline (Nausea→Fever→Delirium, Diarrhea) is Story 1.7. Temperature exposure keeps the existing immediate `hurtRaw(1)` at the extreme bands (Story 1.2 behavior) — do NOT build a debuff/status system. Cold exposure is not a "debuff" yet; it is direct HP harm. The ConsumptionSystem `// TODO(1.7)` hook stays for the 1.7 track.

**Other lines held (do NOT build):**
- **HUD / message-log surfacing is Story 1.8.** The screen gets the keybind for the torch and the existing message log carries craft/burn messages; do NOT wire temperature/thirst HUD labels (that is 1.8's explicit scope).
- **Day/Night cold is NOT a driver here.** The PRD says Temperature is "driven by Weather + Day/Night", but the ONLY AC'd driver is Cold Snap (AC-1: "inside one 70-turn Night"). A day/night temperature offset is a follow-up, not part of this story — do not add it. Cold Snap is the one cold driver.
- **No heat driver.** No weather type is hot (Clear/Rain/Fog/Storm/Cold Snap); the Hot/Overheated bands + harm already exist from Story 1.2. Do not invent a heat source.
- **Campfire build stays FREE** (the 1.5 deviation — "building costs no material"). Do not add a Wood/fuel cost here; the economy is Epic 6.
- **The torch is NOT an inventory item.** AC-3's plain reading: crafting a torch *produces the lit light source directly* (Wood+Coal → 60-turn light). Do not add a `TORCH` Supply value, an equip model, or a light/unlight toggle. Craft = light.
- **Wood is a real Supply** (it is also the future repair material "spears: Wood+Rope" and the PRD's "gather wood + coal"). Add it once, appended.

**The single-slot light consequence (design note — do not fight it):** the light model is ONE positional source (`lightX/lightY`, Story 1.4) — the campfire OR the torch, never both. The torch is a *carried* light that takes over the slot and follows the player; on expiry it reverts to the campfire light (if a campfire is built) or goes dark. Two simultaneous lit sources are out of the model's shape; document the limitation, don't build a light-source list.

## Tasks / Subtasks

- [x] **Task 1 — The Cold Snap temperature driver (AC: 1)**
  - [x] Change `TemperatureSystem.tick` to take `RunState` (was `RoguePlayer`): `tick(state)` computes the per-turn temperature delta and applies it.
  - [x] **Cold Snap:** when `state.getWeather() == Weather.COLD_SNAP` (and the player is not at the fire), drop temperature 2/turn (`adjustTemperature(-2)`). From 0 → Frozen (≤ −80) in **40 turns** — inside one 70-turn Night (AC-1's "~38", starting calibration). 
  - [x] **No mitigation → Frozen:** a player away from any fire under Cold Snap keeps dropping until clamped at −100; the existing extreme-band harm (`hurtRaw(1)` while FROZEN) applies every turn there — lethal unless mitigated (that is the point).
  - [x] **Recovery ≈ half onset:** when NOT Cold Snap and NOT at the fire, the meter keeps the existing neutral drift toward 0 at +1/turn — exactly half the −2 onset (AC-1). `RoguePlayer.tickTemperature()` already does this; keep it as the fallback branch.
  - [x] Extract the extreme-band harm so the driver branches share it: add `RoguePlayer.tickTemperatureHarm()` (harm at FROZEN/OVERHEATED) and have the existing `tickTemperature()` call it after its drift (behavior unchanged). The fire/cold branches apply their delta then call it too.
  - [x] Extreme-band harm ordering: the driver branches apply the delta THEN harm (a player warming out of Frozen at the fire is not harmed that turn; a player dropping into Frozen is harmed). Document this vs. the neutral path's harm-then-drift (a deliberate, more-forgiving semantics).
  - [x] `TurnEngine` survival group: `TemperatureSystem.tick(state)` (was `tick(player)`). Nothing else changes there.
- [x] **Task 2 — Campfire warmth (AC: 2)**
  - [x] At the fire (`state.isPlayerAtFire()`, Manhattan ≤ 1 — the SAME range as the cooking station), the campfire warms: `adjustTemperature(+4)` per turn **up to a comfort cap** (+50, the WARM band). Guard it so a fire can never push into HOT/OVERHEATED (a fire warms you to comfortable, not to overheating).
  - [x] Fire warmth beats Cold Snap: at the fire under Cold Snap the player nets +2/turn (4 − 2) — the fire genuinely "solves warmth" (AC-2), letting a Cold Snap night be survived by staying put.
  - [x] Stationary (AC-2): warmth requires `isPlayerAtFire()` — walking away immediately stops it (this is the AC-4 trade: leave the camp, lose the warmth). The campfire's light/cooking/purify/exposure already exist from 1.4/1.5 — do NOT rebuild them; this task only adds the warmth to the same fire.
  - [x] Warmth radius = `isPlayerAtFire()` (Manhattan ≤ 1). Note the choice; do not widen it.
- [x] **Task 3 — The Wood supply + torch craft (AC: 3)**
  - [x] Append `WOOD("Wood", TrueIdentity.WOOD_ID)` to `Supply` **after `SALT`** (AD-6: existing ordinals unchanged; old saves safe). Add `WOOD_ID` to `TrueIdentity` as an inert identity (`apply` no-op), like `COAL_ID`/`SALT_ID`. `WOOD.isConsumedOnUse()` = false (fuel/material, not used via USE); `isProvision()` = false; `drinkRisk()`/`spoilsTo()`/`cooksTo()`/`filtersTo()`/`boilsTo()` default (not a provision — no taxonomy entries needed beyond the defaults).
  - [x] **Wood sourcing:** WOOD is a world drop via the EXISTING uniform supply scatter (`RunState.placeFloorActors`: `rng.nextInt(Supply.count())`) — appending WOOD widens that range, so it spawns like COAL does. No new tile, no gather action. (A wood-weight/scarcity model is Epic 6 — note it, don't build it.)
  - [x] Add a `CRAFT_TORCH` `PlayerAction.Kind` + factory. In `TurnEngine`: if the backpack has ≥1 `WOOD` AND ≥1 `COAL`, remove one of each and call `state.lightTorch()`; else refuse (message "A torch needs Wood and Coal.") — a refused craft commits **no turn** (the inert-USE precedent).
  - [x] `RunState.lightTorch()`: `torchTurns = TorchSystem.TORCH_BURN (60)` and `setLight(player.x, player.y)` — the torch lights AT the player, immediately, carrying the Story 1.4 light (FOV restore + exposure noise).
- [x] **Task 4 — The torch burn (AC: 3)**
  - [x] Add `torchTurns` (persisted int, field-init 0) to `RunState` + `getTorchTurns()`. `restart()` resets it to 0.
  - [x] Add `RunState.tickTorch()` (the burn step, mirroring `tickHunger`/`tickThirst`): if `torchTurns == 0` no-op; else decrement; if it just hit 0 → spent: **restore the campfire light** if `hasCampfire()` (`setLight(campfireX, campfireY)`) else `clearLight()`; while still burning → `setLight(player.x, player.y)` (the carried light FOLLOWS the player).
  - [x] Add `TorchSystem.tick(state)` (a thin pipeline step like `TemperatureSystem`/`LightSystem`) that calls `state.tickTorch()`. Constant `TorchSystem.TORCH_BURN = 60` (≈ one 70-turn Night, AC-3).
  - [x] Wire `TorchSystem.tick(state)` into `TurnEngine`'s acted branch **immediately before `LightSystem.emitNoise`** — so the burn re-sets the light at the player's post-action tile and the noise emits from the current position. A wall-bump (no acted turn) does NOT burn the torch (AD-5).
  - [x] **Torch is light-only (AC-3):** the torch does NOT warm the player — `TemperatureSystem` warms only via `isPlayerAtFire()` (the campfire), never via the torch. Under a Cold Snap with a torch but no fire, the player still freezes (torch = light to move/retreat, not warmth to camp). This is the AC-4 shape: torch lets you flee into fog WITH light but WITHOUT heat.
  - [x] **Exposure (AC-4):** the lit torch rides the Story 1.4 `LightSystem.emitNoise` — a carried light is visible/audible exactly like the campfire. No new noise code.
- [x] **Task 5 — Serialization + tests (AC: all)**
  - [x] **AD-6:** `torchTurns` is a plain persisted int (field-init 0) → round-trips free; a pre-1.6 save loads `torchTurns = 0` (no torch lit). `temperature`/`weather`/`lightX`/`lightY`/`campfireX`/`campfireY` already persist. No `restoreAfterLoad` change needed for 1.6.
  - [x] **Test hook (precedent: 1.5's `setSkill`):** add `RunState.setWeather(Weather)` — the story's tests need to pin COLD_SNAP (onset) and CLEAR (recovery); weather is otherwise only rolled. Document it as a test/development hook. Update `SurvivalTickTest.realActionTicksAllFour` to `setWeather(CLEAR)` before asserting the +1 neutral drift (today it relies on seed 1's weather; the 1.6 driver makes it weather-dependent — pin it).
  - [x] **Light-tile validation (closes the 1.4 deferral):** the torch and campfire both set the light at the PLAYER's own tile (always walkable + in-bounds by construction), so neither needs bounds validation. Confirm this in the review notes and close the deferred item — do NOT add speculative validation to `setLight`.
  - [x] **Tests** (headless, JUnit 5; mirror `SurvivalTickTest`/`TemperatureSystemTest` patterns):
    - `TemperatureSystemTest` (extend/rework for the driver): Cold Snap (pinned) drops 2/turn and reaches FROZEN within a 70-turn Night; a player at the fire under Cold Snap nets warm (does NOT freeze); a player at the fire in clear weather warms toward +50 and stops (never OVERHEATS); away from the fire in non-Cold-Snap weather the meter drifts toward 0 (+1 = half onset); harm only at the extreme bands (existing assertions preserved).
    - `TorchTest` (new): craft with Wood+Coal consumes both, lights the player's tile, `torchTurns == 60`; craft refused without Wood OR Coal (no turn, nothing consumed); each acted turn burns (torchTurns decrements) and the light follows the player's move; at 0 the torch is spent (light clears, or reverts to a built campfire); the torch provides NO warmth (temperature still drops under a pinned Cold Snap with a torch but no fire); the lit torch emits noise (a `NoiseEvent` appears at the player tile — mirrors `LightNoiseTest`); a wall-bump does not burn the torch (AD-5).
    - `RunStatePersistenceTest` (extend): `torchTurns` survives a round-trip; a pre-1.6 save loads with `torchTurns == 0`.
    - `Supply`/`IdentifyMap`: WOOD appends after SALT (ordinal order pinned) and binds its single inert identity; `singleIdentityTypesDoNotDrawFromTheRng` (the 1.5 H1 test) still passes — WOOD is single-identity, draws nothing.
  - [x] Full suite green: `mvn -o clean install`.

### Review Findings

- [x] [Review][Patch] **Craft-while-torch-lit REFUSED** [core/.../system/TurnEngine.java CRAFT_TORCH, .../TorchTest.java] — Blind Hunter (Med) → resolved by Justine: refuse. Guard `state.getTorchTurns() > 0` → "A torch already burns.", no turn. Added `craftTorchRefusedWhileTorchAlreadyBurns` (no turn, no materials spent, burn untouched). Applied.
- [x] [Review][Patch] **Add the required "torch provides NO warmth" test** [core/src/test/java/com/margins/rogue/TorchTest.java] — Acceptance Auditor: added `litTorchDoesNotWarmUnderAColdSnap` (pin COLD_SNAP, craft a torch with no campfire, assert the temperature still drops). Applied.
- [x] [Review][Patch] **Add the required torch-noise test** [core/src/test/java/com/margins/rogue/TorchTest.java] — Acceptance Auditor: added `litTorchEmitsNoiseAtThePlayerTile` (mirrors `LightNoiseTest`: a lit torch emits one `NoiseEvent` at the bearer's tile) + `expiredTorchEmitsNoNoise` (the `TorchSystem`-before-`emitNoise` ordering: a just-expired torch neither lights nor lures). Applied.
- [x] [Review][Patch] **Fix the mis-aimed AD-6 migration test** [core/src/test/java/com/margins/rogue/state/RunStatePersistenceTest.java] — Blind Hunter (Med): replaced `preStory16SaveLoadsWithoutTorchOrTorchLight` (which removed lightX/lightY, emulating pre-1.4) with `preStory16SaveLoadsWithCampfireLightAndNoTorch` — a real 1.5-era save carries a built campfire + lit light but no torchTurns; loads with torchTurns 0 AND the campfire light surviving. Applied.
- [x] [Review][Patch] **`warmTo` clamps to the +100 ceiling** [core/.../RoguePlayer.java warmTo] — Edge Case Hunter: `Math.min(Math.min(cap, 100), ...)` + javadoc note. Applied.
- [x] [Review][Patch] **`lightTorch` guards `burnTurns <= 0`** [core/.../state/RunState.java lightTorch] — Edge Case Hunter: early-return on `burnTurns <= 0` + javadoc ("a lit light must always have a burn"). Applied.
- [x] [Review][Defer] **Torch can't be extinguished, dropped, or conserved** [core/.../state/RunState.java torchTurns] — Blind Hunter (Med): once lit, the torch burns 60 acted turns with no opt-out (no unlight action; not an inventory item so DROP can't touch it). Out of Story 1.6 scope — the story deliberately models the torch as a pure 60-turn commitment with no inventory item; extinguish/conserve needs a new PlayerAction + likely the Epic 6 object inventory. Deferred, pre-existing-in-scope.
- [x] [Review][Defer] **Torch light desyncs on non-acted repositioning** [core/.../RoguePlayer.java placeAt] — Blind Hunter (Low): `placeAt` moves the player while the light stays on the old tile until the next acted turn. No production caller today (`placeAt` is test-only repositioning) — latent; re-sync the light there if a teleport/descent ever lands under a lit torch. Deferred, not player-reachable.

## Dev Notes

### Current state (what exists, what to ratify, what to preserve)

- **Temperature substrate (Story 1.2, preserved):** `RoguePlayer.temperature` is a `[-100,+100]` int meter with `TempBand` (FROZEN ≤ −80 / COLD ≤ −50 / CHILLED ≤ −15 / NEUTRAL < 15 / WARM < 50 / HOT < 80 / OVERHEATED). `adjustTemperature(delta)` clamps to ±100. `tickTemperature()` harms at FROZEN/OVERHEATED (`hurtRaw(1)`) then drifts one step toward Neutral (+1 when negative, −1 when positive) — the driver-less baseline. **Do not change `tickTemperature()`'s existing behavior**; it becomes the "no cold snap, no fire" fallback. `TemperatureSystem.tick(RoguePlayer)` is a thin wrapper currently called from `TurnEngine`'s acted branch — its signature changes to `tick(RunState)`.
- **Campfire (Story 1.5, preserved):** `RunState.campfireX/Y` (`−1` = none), `hasCampfire()`, `setCampfire/clearCampfire`, `isPlayerAtFire()` (Manhattan ≤ 1). `BUILD_CAMPFIRE` sets both the campfire tile and the light (`setLight`). It is the cooking/purification station. **This story only ADDS warmth; the fire-station/light/exposure is already there.** The campfire is a **single positional light** — the story's torch shares that one slot.
- **Light model (Story 1.4, preserved):** `RunState.lightX/lightY` (`−1` = none), `hasLight()`, `setLight/clearLight`. `LightSystem.emitNoise(state)` emits a per-turn `NoiseEvent` at the light's tile (AD-9/AD-18) — a lit torch is exposed for free. `FovSystem` restores sight from the light tile. The torch re-uses all of this.
- **Weather (Story 1.3):** `RunState.getWeather()`; `Weather.COLD_SNAP` exists and is rollable but has **no effect yet** — this story wires it. The RNG draw order in the constructor (layout → identity → weather) must NOT change (AD-5); the 1.6 driver reads `getWeather()`, it doesn't re-roll it.
- **Supply/TrueIdentity (Stories 1.1/1.5):** supplies append at the END (AD-6). `COAL_ID`/`SALT_ID` show the inert-identity pattern WOOD_ID reuses. `isConsumedOnUse()` excludes fuel/storage (COAL/SALT) — add WOOD to that exclusion. The 1.5 H1 patch means single-identity supplies (like WOOD) draw NO RNG in `IdentifyMap.build` — adding WOOD must not shift the seeded stream.
- **Inventory (Story 1.5):** pure int-stack; `count(type)`, `remove(type, n)`, `tryAdd`. The torch craft is a `remove(WOOD,1)` + `remove(COAL,1)` — no inventory item produced (craft → light directly).
- **`TurnEngine` acted branch (AD-4/AD-5):** survival (hunger/thirst/temperature/clock) → spoilage → detection → companion → enemy → light-noise → noise-resolve → last-stand → FOV. The torch burn slots in **right before `LightSystem.emitNoise`**. "Refused action commits no turn" applies to the refused torch craft.

### The 1.6↔1.7 seam (hold these lines)

- **Tiered debuffs are Story 1.7.** Cold exposure stays immediate `hurtRaw(1)` at FROZEN/OVERHEATED (1.2 behavior) + the Cold Snap meter drop. Do NOT build a debuff/status system or a "Cold" debuff. The ConsumptionSystem `// TODO(1.7)` hook remains the only debuff seam.
- **HUD/labels are Story 1.8.** `RoguePlayer.tempLabel()` exists and is used by tests; the screen's HUD does not surface temperature yet (1.8's explicit carry). The screen gets only the torch keybind + the message log.

### Placement rationale (AD-3)

- **World state on `RunState`:** `torchTurns` (like the light/campfire/spoilage-clock persisted ints), `lightTorch()`/`tickTorch()`, `setWeather()` (test hook, like `setSkill`).
- **Player state on `RoguePlayer`:** temperature meter + bands + harm (existing); `tickTemperatureHarm()` extracted so all branches share it.
- **Systems (`system/`):** `TemperatureSystem.tick(RunState)` (the driver model) and new `TorchSystem.tick(RunState)` (the burn) — one-purpose pipeline helpers mirroring `HungerSystem`/`LightSystem`. Constants: `COLD_SNAP_ONSET`/`FIRE_WARMTH`/`FIRE_COMFORT` on `TemperatureSystem`, `TORCH_BURN` on `TorchSystem`.
- **Items (`item/`):** `WOOD` Supply + `WOOD_ID` TrueIdentity (appended, inert). **Tiles:** none. Core stays headless (AD-2) — no libGDX in any new `com.margins.rogue.*` class; `MarginScreen` owns render/input.

### Serialization — the pattern that applies directly

- `SaveService.json()` sets `usePrototypes(false)` — every persisted field is always written. Field-initialize `torchTurns = 0` so a pre-1.6 save loads "no torch lit" (AD-6). New `Supply.WOOD` is an enum value (ordinal appended after SALT) — old saves and old ordinals unchanged. **No `restoreAfterLoad` change for 1.6** (all new state is plain persisted ints with field-init defaults).

### Scope discipline (CLAUDE.md §2/§3)

- Do **NOT** build: the debuff/status system (1.7); HUD/label surfacing (1.8); a Day/Night temperature offset or any heat driver (not AC'd); a `TORCH` inventory item / equip / light-toggle; a campfire fuel cost (Epic 6); a wood-weight/scarcity model (Epic 6); a second light source / light-source list. Every changed line traces to one of the four ACs or its required wiring.
- Keep the numbers as "starting calibration" (the PRD marks them so): `COLD_SNAP_ONSET = 2`, `FIRE_WARMTH = 4`, `FIRE_COMFORT = 50`, `TORCH_BURN = 60`, neutral recovery `+1`. Document each choice in the code comment (the PRD cites the exact values; the exact "~38 turns" vs 40-from-0 is a calibration note — 40 is inside one 70-turn Night, which is the AC's hard constraint).

### Testing standards

- JUnit Jupiter 5.10.2, `mvn -o clean install`. Drive turn behavior through `TurnEngine.advance` (mirror `SurvivalTickTest`). Pin weather with the new `RunState.setWeather` hook (the 1.6 temperature behavior is weather-dependent by design — tests must not rely on a lucky seed). Extend `RunStatePersistenceTest` for `torchTurns`. Mirror `LightNoiseTest` for the torch's exposure. All randomness draws from `state.rng()` (AD-5) — 1.6 adds no new draws (the driver reads weather; the torch craft is deterministic), so the seeded stream is untouched.

### Project Structure Notes

- **New:** `system/TorchSystem.java`; tests `TorchTest.java`; `TemperatureSystem.java` reworked (signature `tick(RunState)` + the driver constants).
- **Modified:** `item/Supply.java` (+WOOD), `item/TrueIdentity.java` (+WOOD_ID), `RoguePlayer.java` (`tickTemperatureHarm()` extraction), `state/RunState.java` (`torchTurns` + `lightTorch()`/`tickTorch()`/`getTorchTurns()`, `setWeather()` test hook, `restart()` reset), `system/PlayerAction.java` (CRAFT_TORCH), `system/TurnEngine.java` (CRAFT_TORCH case, `TemperatureSystem.tick(state)`, `TorchSystem.tick`), `MarginScreen.java` (torch keybind), `core/src/test/java/.../SurvivalTickTest.java` (pin CLEAR), `TemperatureSystemTest.java` (driver rework), `RunStatePersistenceTest.java` (torchTurns round-trips).

### References

- [Source: epics.md#Story-1.6] — the four ACs (Cold Snap → Frozen ~38t; campfire warmth+light+station+exposed; torch Wood+Coal 60t; abandon-camp trade).
- [Source: PRD §2/§4.2 — Temperature] — the −100..+100 meter, Cold Snap −2.0/turn → Frozen in ~38 turns, recovery ≈ half onset, torch "light-only (warmth TBD)".
- [Source: PRD §1] — "gather wood + coal, find water, craft a torch"; the campfire as warmth+light+cooking+purification but visible/audible (the choice core).
- [Source: architecture spine AD-5/AD-18 + review-adversarial-2 §Unit A] — light as a per-turn NoiseEvent (the torch's exposure channel); acted-turn-only honesty.
- [Source: story-1.2-deferred — "Driver-less temperature drift vs. harm needs re-balancing under a real driver" + story-1.4-deferred — "validate the light tile when the campfire/torch lights it"] — the two carries this story closes.
- [Source: story-1.5-food-water-and-two-step-purification.md] — the campfire fire-station, `isPlayerAtFire()`, the `setSkill` test-hook precedent, the H1 single-identity-no-RNG patch, the "campfire build is free" deviation.

## Dev Agent Record

### Agent Model Used

claude-opus-4-8 (1M context)

### Implementation Plan

- **Task 1 — Cold Snap driver:** rewrote `TemperatureSystem.tick` to take `RunState` (was `RoguePlayer`). COLD_SNAP and no fire → `adjustTemperature(-COLD_SNAP_ONSET)` (−2/turn → Frozen in 40t, inside one 70-turn Night); else the driver-less `tickTemperature()` (+1/turn recovery, half the onset). Extracted `RoguePlayer.tickTemperatureHarm()` (harm at FROZEN/OVERHEATED) shared by all branches; the driver branches harm post-delta (dropping into Frozen harms that turn, warming out does not), the neutral path keeps harm-then-drift (both documented on the methods). `TurnEngine` survival group passes `state`.
- **Task 2 — Campfire warmth:** folded into the driver's at-fire branch — `isPlayerAtFire()` (Manhattan ≤ 1, the cooking range), `warmTo(+4 [net +2 under a Snap], FIRE_COMFORT)`. Added `RoguePlayer.warmTo(int, int)` (apply an amount, never exceed a cap) so the comfort guard reads clearly.
- **Task 3 — Wood + torch craft:** appended `WOOD`/`WOOD_ID` after SALT/COAL_ID (AD-6: old ordinals + saves unchanged; `isConsumedOnUse` excludes WOOD; single-identity → no RNG draw, the 1.5 H1 test holds). `CRAFT_TORCH` action in TurnEngine: ≥1 WOOD AND ≥1 COAL → remove one of each + `state.lightTorch(TORCH_BURN)`; else refuse (no turn). `TORCH_BURN` lives on `TorchSystem` and is passed into `lightTorch(int)` so `state` keeps zero `system` imports (the story's literal no-arg `lightTorch()` referencing `TorchSystem.TORCH_BURN` would create the first state→system dependency).
- **Task 4 — Torch burn:** `RunState.torchTurns` (persisted, field-init 0, restart reset) + `getTorchTurns`/`lightTorch`/`tickTorch`. `tickTorch()` decrements on the acted path, re-lights at the player's post-action tile, and on 0 reverts to the campfire light (if any) or clears. `TorchSystem.tick` wired immediately before `LightSystem.emitNoise` (AD-4). Wall-bumps don't burn (AD-5). Torch is light-only — `TemperatureSystem` never reads the torch.
- **Task 5 — Serialization + tests:** `torchTurns` is a plain persisted int → round-trips free, a pre-1.6 save loads 0 (no `restoreAfterLoad` change). Added `RunState.setWeather(Weather)` test hook (precedent: 1.5's `setSkill`) so the driver tests pin CLEAR/COLD_SNAP and are seed-independent. Reworked `TemperatureSystemTest` (11 driver tests), new `TorchTest` (7), extended `RunStatePersistenceTest` (+2), pinned `SurvivalTickTest` to CLEAR. Full suite green.

### Debug Log References

- Green baseline: 122 tests (HEAD 56ec734).
- Core compiled clean after each layer (driver, warmth, supply/craft, burn, serialization).
- First test-compile failure: `TorchTest` asserted a non-existent `TurnResult.acted` — rewrote to infer acted-ness from observable state (clock advance / inventory consumption / torchTurns), matching the existing suite's pattern.
- One stale-comment fix: `RoguePlayer.tickTemperature()`'s javadoc still said "weather/fire arrive later" — updated to describe the 1.6 driver-less fallback role.
- Final: `mvn -o clean install` → **137 tests, 0 failures, BUILD SUCCESS** (122 → 137; +15: TorchTest 7, TemperatureSystemTest +6 [5 → 11], RunStatePersistenceTest +2).

### Completion Notes List

- **All 5 tasks + AC-1/2/3/4 satisfied**, built fully end-to-end.
- **Cold Snap driver (AC-1):** `TemperatureSystem.tick(RunState)` — COLD_SNAP → −2/turn → Frozen (≤ −80) in 40 turns, inside one 70-turn Night (AC-1's "~38", starting calibration documented). Recovery outside a Snap is +1/turn — half the onset. Extreme-band harm extracted to `RoguePlayer.tickTemperatureHarm()`, shared by the driver branches (delta-then-harm: dropping into Frozen harms that turn, warming out does not) and the neutral path (harm-then-drift, unchanged).
- **Campfire warmth (AC-2):** `FIRE_WARMTH = +4`/turn on `isPlayerAtFire()` (Manhattan ≤ 1, the same range as the cooking station), capped at `FIRE_COMFORT`. **Deviation:** the story said "cap +50 (the WARM band)" but +50 lands exactly in HOT (`getTempBand`: temp < 50 → WARM, < 80 → HOT) — the cap is **49**, the true top of WARM, so the AC-2 guard "a fire never pushes into HOT/OVERHEATED" holds. Fire beats a Cold Snap (net +2 at the fire). Stationary (AC-4).
- **Wood + torch craft (AC-3):** `Supply.WOOD` appended after SALT (AD-6: old ordinals + saves unchanged), inert `WOOD_ID`; `isConsumedOnUse()` excludes WOOD; single-identity so the 1.5 H1 no-RNG-draw test still passes. `CRAFT_TORCH` (1 WOOD + 1 COAL → the torch), refused without materials commits no turn (the inert-USE precedent). **Implementation detail:** `lightTorch(int burnTurns)` takes the burn as a parameter (TorchSystem owns `TORCH_BURN`) — keeps `state` free of `system` imports (the story's literal no-arg would create the first state→system dependency).
- **Torch burn (AC-3):** `RunState.torchTurns` (persisted, field-init 0, restart reset); `tickTorch()` decrements on the acted path, re-lights at the player's post-action tile, and on expiry reverts to the campfire light (if any) or goes dark. `TorchSystem.tick` wired immediately before `LightSystem.emitNoise` (AD-4). Wall-bumps don't burn (AD-5). Torch is light-only — a torch under a Cold Snap still freezes you (AC-4: flee with light, not warmth).
- **Serialization (AC-all/AD-6):** `torchTurns` round-trips free (plain int); a pre-1.6 save loads 0. No `restoreAfterLoad` change needed. `RunState.setWeather(Weather)` test hook pins CLEAR/COLD_SNAP for seed-independent drivers. `SurvivalTickTest.realActionTicksAllFour` now pins CLEAR (it previously relied on seed 1's weather — the 1.6 driver made it weather-dependent). The **1.4 light-tile-validation deferral closes**: the torch and campfire both set the light at the player's own walkable tile — safe by construction, no speculative validation added.
- **Deviations (all documented, none scope-expanding):** (1) `FIRE_COMFORT = 49` not the story's "+50" (WARM-band guard, above); (2) `lightTorch(int)` param instead of the literal no-arg (layering, above); (3) the long-burn `TorchTest` clears enemies for determinism (the burn is pure state; enemy interference over 60 acted turns would be flaky); (4) `T` keybind for the torch (screen stopgap until the 1.8 HUD).
- **Seams held** — no debuff/status system (1.7; HP harm is the placeholder); no HUD (1.8); no economy (Epic 6 — WOOD is a uniform world drop, no scarcity model); no torch warmth.

### File List

**Modified**
- `core/src/main/java/com/margins/rogue/RoguePlayer.java` — `tickTemperatureHarm()` extraction + `warmTo(int, int)` comfort-cap warmth; javadoc updated.
- `core/src/main/java/com/margins/rogue/item/Supply.java` — `WOOD` appended after SALT; `isConsumedOnUse` excludes WOOD.
- `core/src/main/java/com/margins/rogue/item/TrueIdentity.java` — `WOOD_ID` inert identity.
- `core/src/main/java/com/margins/rogue/state/RunState.java` — `torchTurns` field + `getTorchTurns`/`lightTorch`/`tickTorch`; `setWeather` test hook; `restart()` reset.
- `core/src/main/java/com/margins/rogue/system/PlayerAction.java` — `CRAFT_TORCH` kind + factory.
- `core/src/main/java/com/margins/rogue/system/TemperatureSystem.java` — `tick(RunState)` weather+fire driver + `COLD_SNAP_ONSET`/`FIRE_WARMTH`/`FIRE_COMFORT`.
- `core/src/main/java/com/margins/rogue/system/TurnEngine.java` — `CRAFT_TORCH` case; `TemperatureSystem.tick(state)`; `TorchSystem.tick` before `LightSystem.emitNoise`.
- `core/src/main/java/com/margins/MarginScreen.java` — `T` keybind (torch craft).
- `core/src/test/java/com/margins/rogue/SurvivalTickTest.java` — pin CLEAR in `realActionTicksAllFour` + `lethalTemperatureHonorsLastStandReprieve`.
- `core/src/test/java/com/margins/rogue/TemperatureSystemTest.java` — reworked for the driver (5 → 11 tests).
- `core/src/test/java/com/margins/rogue/state/RunStatePersistenceTest.java` — torch round-trip + pre-1.6 save (14 → 16 tests).

**Added**
- `core/src/main/java/com/margins/rogue/system/TorchSystem.java` — `TORCH_BURN = 60` + the burn pipeline step.
- `core/src/test/java/com/margins/rogue/TorchTest.java` — craft/refusal (x3)/follow/burn-duration/revert-to-campfire/wall-bump-no-burn/no-warmth-under-Cold-Snap/torch-noise + expiry-no-noise (11 tests, incl. 4 review follow-ups).

## Change Log

- 2026-08-07 — Story 1.6 created (temperature driver: Cold Snap −2/turn → Frozen in 40t inside one 70-turn Night with ≈half-onset recovery; campfire warmth at +4/turn capped at WARM; Wood supply; torch craft Wood+Coal → 60-turn carried light; the AC-4 abandon-camp stealth trade). Carries the 1.2 temperature-rebalance deferral and closes the 1.4 light-tile-validation deferral (torch/campfire light at the player's own walkable tile — safe by construction). Seams held: debuffs → 1.7, HUD → 1.8, economy → Epic 6.
- 2026-08-07 — Implemented Story 1.6. `TemperatureSystem.tick(RunState)` now drives temperature from the weather (Cold Snap −2/turn → Frozen in 40t, ≈half-onset recovery) and the campfire (+4/turn capped at WARM — never into HOT); `WOOD` supply + `CRAFT_TORCH` → a 60-turn carried light (`RunState.torchTurns` + `TorchSystem`) that follows the player and reverts to the campfire on expiry; `setWeather` test hook pins the driver in tests; `SurvivalTickTest` pinned CLEAR. 122 → 137 tests green. Seams held: debuffs → 1.7, HUD → 1.8, economy → Epic 6.
- 2026-08-07 — Senior Developer Review complete: **Approve (6 patches applied, 2 deferred)**. See the review section below. 137 → 141 tests green.

## Senior Developer Review (AI)

**Date:** 2026-08-07 · **Outcome:** Approve (with patches applied)

Acceptance Auditor traced all four ACs and all five tasks' behavioral subtasks and confirmed them genuinely implemented — but flagged two Task-5-required tests the checklist demanded that were absent (torch-provides-no-warmth, torch-noise). Blind Hunter found the torch-as-beacon exposure (by-design, AD-18/AC-4 — its test gap folds into the noise test), the mis-aimed AD-6 migration test (removed pre-1.4 fields while claiming pre-1.6), the craft-while-lit material sink (→ Justine's design decision: refuse), and several by-design dismissals (single-slot suppression, non-COLD_SNAP weather equivalence, Wood-via-scatter seed shift). Edge Case Hunter found two defensive gaps (`warmTo` +100 clamp, `lightTorch` burn ≤ 0 guard) — both unreachable at their current call sites but one-line contract guards. Suite green at 137/137 on review entry; 141/141 after the 6 patches (4 new TorchTest scenarios + the refuse-while-lit guard, 1 migration-test correction, 2 guards). Two Med/Low defers recorded (torch no-extinguish — out of scope, Epic 6 inventory; torch-light desync on `placeAt` — latent, test-only caller).

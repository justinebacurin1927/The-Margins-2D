---
baseline_commit: f1005e262fac838ade638e386d9bfc8d7ad90595
---

# Story 4.2: Distraction leverage

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As Justine (player),
I want to command my companion to make noise,
so that I can peel patrols off a path I couldn't force alone (FR-14).

## Acceptance Criteria

1. **Given** an active companion with Distraction available, **When** I trigger it, **Then** a Noise event is emitted at the companion's position and nearby enemies' Detection is raised toward the origin, opening a path away from it — a nearby UNAWARE enemy becomes SUSPICIOUS and retargets its investigation at the noise origin; an enemy outside the radius is unaffected. (FR-14, AD-9, AD-10)
2. **Given** a Distraction use, **When** it succeeds, **Then** it commits a full turn through the pipeline (AD-4): hunger ticks once, the companion still takes its follow step, and the noise resolves on the Noise step of the same turn. (AD-4)
3. **Given** the per-floor limit (2 uses), **When** I trigger Distraction a 3rd time on the same floor, **Then** it is refused with feedback ("Galleon has no shouts left this floor.") and spends **no** turn — hunger does not tick and no enemies act. (FR-14)
4. **Given** a floor descent, **When** the new floor is generated, **Then** the per-floor limit resets to 2. (FR-14, AD-10)
5. **Given** no active companion, **When** I trigger Distraction, **Then** it is refused with feedback ("No companion to call on.") and spends **no** turn. (FR-14)

**Architectural definition-of-done:**

6. Distraction **emits a Noise event** via `RunState.emitNoise` and is resolved by the existing `NoiseSystem` (AD-9) — it never directly manipulates enemies (AD-10). The trigger is a `PlayerAction` kind handled by `TurnEngine` (AD-4), never inlined into input/rendering (AD-2).
7. The remaining-uses counter lives on the `Companion` entity as a plain int field — it persists with the whole `RunState` via libGDX Json for free (AD-6), holds no libGDX types (AD-2), and resets on descent (AC-4).

## Product decisions (recommended defaults baked in)

- **"2 per floor" over "6-turn cooldown."** The epic AC offers either ("2 per floor or 6-turn cooldown"). We bake in **2 per floor**: it is simpler, pairs naturally with the floor-transition system Story 4.1 just built (`descend()` already repositions the companion — resetting a counter there is trivial), and a per-floor resource fits the roguelike-scarcity feel. The 6-turn cooldown is the documented alternative if playtesting wants a tighter leash — a swap, not a rework.
- **Noise origin = the companion's position at the moment the action resolves.** `emitNoise` captures x/y at call time, and the companion's follow step runs *after* in the acted block — so enemies investigate **where he shouted**, not where he ends the turn. Since he follows adjacent to Milek (4.1), this is effectively at the player's side. This is the intended tactical shape: shout, enemies converge on that tile, Milek slips through the path they vacate.
- **Radius = 6.** A deliberate call-out is louder than an attack swing (`ATTACK_NOISE_RADIUS = 4` in CombatSystem); 6 matches the enemy `VISION_RANGE` so it reliably reaches the patrols you're peeling. Named constant on `CompanionSystem` (`DISTRACTION_RADIUS`), PRD-balance tuning.
- **Distraction peels patrols, not alerted pursuers.** The existing `NoiseSystem` raises UNAWARE→SUSPICIOUS and retargets investigation; it does **not** force ALERTED (an ALERTED enemy chases the player, not the noise — see `NoiseSystem` doc). That's correct for "peel patrols off a path." Also note: it's most effective while the player is out of sight — if an enemy can see Milek, the *next* `DetectionSystem.update` re-targets it at him. No change to those systems.
- **Key: `F`.** Unused, one-handed, fits "command Galleon." A successful use shows "Galleon shouts!" in the message HUD.
- **No HUD / no new rendering in this story.** The companion already renders (4.1). A "shouts left" readout is a nice-to-have, explicitly deferred (see Scope boundary) — the AC only requires the ability + the limit, and refusal feedback is carried by the message line.

## Tasks / Subtasks

- [x] **Task 1 — `Companion` carries the per-floor use limit** (AC: 3, 4, 7)
  - [x] Add `public static final int MAX_DISTRACTIONS_PER_FLOOR = 2;` and `private int distractionsLeft = MAX_DISTRACTIONS_PER_FLOOR;` to `Companion.java`. Plain int field (not transient) so it serializes with the entity under the `RunState` root (AD-6).
  - [x] Accessors: `public boolean canDistract()` (`distractionsLeft > 0`), `public void useDistraction()` (decrement, floor at 0), `public int getDistractionsLeft()`. No RNG, no libGDX (AD-2).
  - [x] `public void resetDistractions()` (`distractionsLeft = MAX_DISTRACTIONS_PER_FLOOR`).

- [x] **Task 2 — `CompanionSystem.distract` emits the noise** (AC: 1, 5, 6)
  - [x] In `CompanionSystem.java` (already holds `follow` from 4.1), add `public static final int DISTRACTION_RADIUS = 6;` and:
    ```java
    public static boolean distract(RunState state, List<String> messages) {
        Companion c = state.getActiveCompanion();
        if (c == null) { messages.add("No companion to call on."); return false; }
        if (!c.canDistract()) { messages.add("Galleon has no shouts left this floor."); return false; }
        c.useDistraction();
        state.emitNoise(c.getTileX(), c.getTileY(), DISTRACTION_RADIUS);
        messages.add("Galleon shouts!");
        return true;
    }
    ```
  - [x] Import `java.util.List`. Keep the signature/static-system pattern consistent with `HungerSystem`/`NoiseSystem`/`FovSystem`/`follow`.

- [x] **Task 3 — `PlayerAction.DISTRACT`** (AC: 1, 6)
  - [x] In `PlayerAction.java`, add `DISTRACT` to the `Kind` enum (after `PICKUP`), and a factory `public static PlayerAction distract(int dir)` mirroring the others (dir keeps the facing-update uniform; `itemType = -1`).
  - [x] The `Kind` switch in `TurnEngine` is exhaustive — adding the enum member forces the `case` (Task 4), which is the compile-time guard we want.

- [x] **Task 4 — `TurnEngine` DISTRACT case + no-op semantics** (AC: 2, 3, 5, 6)
  - [x] Add a `case DISTRACT:` to the action switch: `acted = CompanionSystem.distract(state, result.messages); break;`. A success commits a turn (acted=true → hunger, detection, follow, enemyPhase, noise-resolve all run in the fixed AD-4 order); a refusal returns false → **no turn** (mirrors the inert-USE "no turn" precedent in the same file).
  - [x] Confirm the descent trigger and the `WAIT` message are both guarded by `action.kind == PlayerAction.Kind.MOVE` / `== WAIT` respectively, so DISTRACT never descends and never prints "Wait" — no changes needed there, just verify.

- [x] **Task 5 — Descend resets the limit** (AC: 4)
  - [x] In `RunState.descend()` (the `if (c != null)` block that already repositions the companion), call `c.resetDistractions();` alongside the existing `placeAt`/`setMap`. Run start already gives 2 via the field initializer (fresh `Companion` in `spawnStartingCompanion`); `restart()` spawns a new companion, so it also starts at 2 — no other reset sites needed.

- [x] **Task 6 — Screen input: `F` triggers Distraction** (AC: 1, 5)
  - [x] In `RogueGameScreen.readAction(int facing)` (the same method that maps WASD/Q/E/SPACE), add `if (Gdx.input.isKeyJustPressed(Input.Keys.F)) return PlayerAction.distract(facing);` before the `return null;`. The message HUD already displays `result.lastMessage()` via `submitTurn` — the refusal and success messages surface with no extra rendering.

- [x] **Task 7 — Verification** (AC: 1, 2, 3, 4, 5, 6, 7)
  - [x] Headless harness (throwaway `main`, run via `mvn -o -pl core install` then `exec-maven-plugin:3.1.0:java`, per the 4.1 pattern). Place a probe enemy in range and one out of range; capture the companion's position *before* the advance:
    - **Success**: `engine.advance(state, PlayerAction.distract(...))` → message "Galleon shouts!", `canDistract()` now false, `getDistractionsLeft() == 1`.
    - **Noise raised detection**: the in-range UNAWARE enemy is now SUSPICIOUS with `lastSeen == companion's pre-follow position`; the out-of-range enemy is unchanged. Noise queue is empty after the turn (resolved).
    - **Turn cost**: hunger ticked exactly once (successful use); the companion still took its follow step (moved or adjacent).
    - **Limit**: a 2nd use succeeds (→ 0 left); a 3rd is refused — hunger does **not** tick again, no message "Galleon shouts!", message says "Galleon has no shouts left this floor."
    - **Descent reset**: after a successful `descend()`, `canDistract()` is true again and `getDistractionsLeft() == 2`.
    - **No companion**: clear the companions list, trigger Distraction → message "No companion to call on.", no turn spent (hunger unchanged).
    - **Persistence**: Json round-trip (element types as `SaveService.json()`) preserves `distractionsLeft`; `restoreAfterLoad` leaves it intact and the companion's map non-null.
  - [x] Live boot on `:0`: press `F` with the companion present → "Galleon shouts!" and nearby unaware patrols raise `?` and drift toward the shout; a 3rd press refuses with the no-shouts message. Delete the throwaway harness after the run.

## Dev Notes

### Governing architecture
- **AD-10 — Companion as an allied turn actor.** Distraction is the companion's ability and **emits a Noise event**; it does not directly manipulate enemies. [Source: ARCHITECTURE-SPINE.md#AD-10]
- **AD-9 — Radius Detection + Noise queue.** `RunState.emitNoise(x, y, radius)` queues a transient `NoiseEvent`; `NoiseSystem.resolve` (the "Noise resolve" pipeline step) draws in-radius living enemies toward the origin: UNAWARE→SUSPICIOUS, `setLastSeen(origin)`, `calmTurns = 0`. Distraction needs **zero new noise behavior** — it just produces an event. [Source: ARCHITECTURE-SPINE.md#AD-9; core/src/main/java/com/margins/rogue/system/NoiseSystem.java; core/src/main/java/com/margins/rogue/NoiseEvent.java]
- **AD-4 — Ordered turn pipeline.** `TurnEngine.advance` order is PlayerAction → Hunger → Detection update → Companion follow → enemyPhase → Noise resolve → FOV. A DISTRACT action emits its noise in the PlayerAction step; `NoiseSystem.resolve` processes it *after* enemyPhase, so enemies respond to the shout on the Noise step of the same turn and act on it (move toward the origin) on their next AI tick. This is the same ordering attacks already use — no pipeline reorder. [Source: ARCHITECTURE-SPINE.md#AD-4; core/src/main/java/com/margins/rogue/system/TurnEngine.java:101–114]
- **AD-6 — Save = whole `RunState`.** `distractionsLeft` is a plain int on `Companion`, which already serializes under the `RunState` root (element type registered in `SaveService.json()` by 4.1). No new persistence code. [Source: ARCHITECTURE-SPINE.md#AD-6; core/src/main/java/com/margins/rogue/save/SaveService.java]
- **AD-2 — Model owns state; no libGDX in the model.** The limit lives on the model (`Companion`); the screen only forwards the keypress via `PlayerAction`. [Source: ARCHITECTURE-SPINE.md#AD-2]

### Files being modified / added — current state and what to preserve
- **`Companion.java`** (UPDATE, built in 4.1): holds `tileX/tileY/bindId`, `transient RogueTileMap`, `followStep`, `isAdjacentTo`. **Add** only the distraction counter + accessors. Do not touch follow/placement — 4.1's descent and follow rely on them.
- **`system/CompanionSystem.java`** (UPDATE, built in 4.1): currently just `follow(state)`. **Add** `DISTRACTION_RADIUS` + `distract(...)`. Keep it static/stateless like every other system.
- **`system/PlayerAction.java`** (UPDATE): add `Kind.DISTRACT` + `distract(int dir)` factory. The enum is exhaustive in `TurnEngine` — expect the compile error until the case lands.
- **`system/TurnEngine.java`** (UPDATE, built in 4.1): has MOVE/ATTACK/BLOCK/WAIT/USE/DROP/PICKUP cases + the `if (acted)` pipeline with the `STAIRS_DOWN` descent branch. **Add** the DISTRACT case. Preserve the descent guard (`action.kind == MOVE`) and the WAIT message guard.
- **`state/RunState.java`** (UPDATE, built in 4.1): `descend()` repositions player + companion and rebuilds the floor. **Add** `c.resetDistractions()` in the companion block. Nothing else.
- **`RogueGameScreen.java`** (UPDATE, built in 4.1): `readAction(int facing)` maps keys → `PlayerAction`. **Add** the `F` key. The companion is already rendered; no render changes.
- **NEW:** none (4.1 already created `Companion` and `CompanionSystem`).

### Scope boundary
- **IN:** the Distraction ability (noise at companion's position), the 2-per-floor limit + refusal feedback, descent reset, `F` key, message feedback, save/load of the counter.
- **OUT:** Bond tracking (**Story 4.3**); recruitment/binding gates (4.3 + Epic 5–6); any change to `NoiseSystem`/`DetectionSystem` semantics (they already implement AD-9 exactly as distraction needs); a "shouts left" HUD readout or ability icon (deferred — message feedback suffices for the AC); the 6-turn cooldown alternative (documented swap); real per-bind art (**Epic 6**).

### Testing standards
- No committed JUnit suite — throwaway-`main` headless harness + live boot, as in 4.1 / 3.1–3.4. `RunState`/`TurnEngine`/`NoiseSystem`/`Companion` are pure model and run headless; libGDX `Json` (for the round-trip) is headless-safe. **Build quirk:** `mvn -o -pl core install` before `exec:java`.

### References
- [Source: _bmad-output/planning-artifacts/epics.md#Epic 4 · Story 4.2 (FR-14); Epic 4 design constraint]
- [Source: ARCHITECTURE-SPINE.md#AD-9, #AD-10, #AD-4, #AD-2, #AD-6]
- [Source: core/src/main/java/com/margins/rogue/system/NoiseSystem.java (noise resolve — no changes); system/CompanionSystem.java (add distract); system/PlayerAction.java (add Kind); system/TurnEngine.java (case + pipeline); state/RunState.java (descend reset); RogueGameScreen.java (F key); Companion.java (counter)]
- [Source: _bmad-output/implementation-artifacts/4-1-companion-follow-and-floor-transition.md (the system this builds on: follow, descend, party, persistence)]

### Project Structure Notes
- Entities live under `com.margins.rogue` (`Companion.java` beside `RogueEnemy`/`RoguePlayer`), systems under `com.margins.rogue.system` — the actual codebase layout, per the 4.1 note (the architecture spine's `entity/` prefix is naming-only).
- `NoiseEvent` is transient single-turn state on the `RunState` noise queue (`RunState.emitNoise` producer API, AD-9) — already built, reused verbatim.

### Review Findings

Code review 2026-08-03 (Blind Hunter + Edge Case Hunter + Acceptance Auditor, parallel). AC verdict: 4.2 AC 1–7 all satisfied, no defects. Noise emits at the companion's tile before the follow step (AC-2 ordering confirmed); the shout resolves in the Noise step so enemies act on it their next AI tick (spec-acknowledged); refused shouts spend no turn. The `DISTRACTION_RADIUS = 6` "louder than an attack swing (4)" comment was verified against `CombatSystem.ATTACK_NOISE_RADIUS = 4` — accurate. No findings.

## Dev Agent Record

### Agent Model Used

Claude Opus 4.8 (1M context) — Dev Story workflow (bmad-dev-story)

### Implementation Plan

1. **Companion counter (Task 1)** — `MAX_DISTRACTIONS_PER_FLOOR = 2`, `distractionsLeft` field (plain int, persisted AD-6), `canDistract()/useDistraction()/getDistractionsLeft()/resetDistractions()`. Field initializer gives 2 at run start / restart (fresh Companion); no extra reset sites.
2. **`CompanionSystem.distract` (Task 2)** — static system method, `DISTRACTION_RADIUS = 6`. Emits a `NoiseEvent` at the companion's position via `RunState.emitNoise` (AD-9/AD-10 — produces noise, never touches enemies directly). Refusals add feedback messages and return false.
3. **`PlayerAction.DISTRACT` (Task 3)** — enum member + `distract(int dir)` factory. Exhaustive `Kind` switch in `TurnEngine` forced the case.
4. **`TurnEngine` case (Task 4)** — `acted = CompanionSystem.distract(...)`. Success → acted=true → full AD-4 pipeline (hunger, detection, follow, enemyPhase, noise-resolve) on the same turn; refusal → no turn. Descent/WAIT guards verified untouched (both keyed on `action.kind`).
5. **Descent reset (Task 5)** — `RunState.descend()` calls `c.resetDistractions()` in the existing companion-reposition block.
6. **`F` key (Task 6)** — added to `RogueGameScreen.readAction`. No render changes; message HUD surfaces success/refusal via `submitTurn`.
7. **Verification (Task 7)** — throwaway headless harness, 18 checks (success/noise-raises-detection/turn-cost/limit/descent-reset/no-companion/persistence), green on seeds 1, 7, 42, 1234, 9999, 2026, 55555. Desktop smoke-boot on `:0` clean (loaded the pre-existing save, which now carries `distractionsLeft`). Harness deleted.

### Debug Log References

- **Harness assertion bug (not a code bug).** First run flagged "companion spent a use (now 1)" — the check asserted `!c.canDistract() && getDistractionsLeft() == 1`, but after spending 1 of 2 uses `canDistract()` correctly still returns **true** (one shout left). The 18-check mechanics (2→1→0→refused→reset) all passed. Fixed the assertion to `canDistract() && == 1`; reran green.
- **No new noise semantics.** `NoiseSystem`/`DetectionSystem` untouched — Distraction just emits a `NoiseEvent`; the existing resolve step does the raising/retargeting. Verified in-harness that the in-range enemy becomes SUSPICIOUS with `lastSeen == pre-follow origin` and the out-of-range enemy is unaffected.
- **Pre-existing `saveVersion` field.** Still uncommitted from the Epic 3 retro critical path; untouched by this story.

### Completion Notes List

- ✅ All 7 tasks complete; all 5 ACs + both architectural DoD points satisfied and verified headlessly.
- ✅ AC-1: success emits noise at the companion's position; in-range UNAWARE→SUSPICIOUS retargeted at the origin; out-of-range unaffected.
- ✅ AC-2: success commits a full turn (hunger ticked once; companion still followed; noise resolved on the same turn's Noise step).
- ✅ AC-3: 3rd use refused with "Galleon has no shouts left this floor." and spends no turn (hunger unchanged).
- ✅ AC-4: descent resets the limit to 2.
- ✅ AC-5: no companion → "No companion to call on.", no turn.
- ✅ DoD-6: Distraction emits a Noise event via `RunState.emitNoise` (AD-9/AD-10), triggered through `PlayerAction`/`TurnEngine` (AD-4), never inlined (AD-2).
- ✅ DoD-7: counter lives on `Companion` as a plain int, persists under the whole-`RunState` save (AD-6), resets on descent.
- ⏳ **Outstanding human check:** the on-screen visual confirmation (press `F`, watch patrols raise `?` and drift toward the shout; 3rd press refuses) — smoke boot confirmed clean init/render; the interactive walk is for the player.

### File List

- `core/src/main/java/com/margins/rogue/Companion.java` (`MAX_DISTRACTIONS_PER_FLOOR`, `distractionsLeft` + accessors, `resetDistractions`)
- `core/src/main/java/com/margins/rogue/system/CompanionSystem.java` (`DISTRACTION_RADIUS`, `distract(...)`)
- `core/src/main/java/com/margins/rogue/system/PlayerAction.java` (`Kind.DISTRACT`, `distract(int dir)`)
- `core/src/main/java/com/margins/rogue/system/TurnEngine.java` (`case DISTRACT`)
- `core/src/main/java/com/margins/rogue/state/RunState.java` (`descend()` → `resetDistractions()`)
- `core/src/main/java/com/margins/rogue/RogueGameScreen.java` (`F` key in `readAction`)
- `core/src/main/java/com/margins/rogue/DistractHarness.java` (throwaway — created for Task 7, deleted after)

## Change Log

- 2026-08-03 — Story 4.2 spec created: Distraction leverage. Chose "2 per floor" over "6-turn cooldown"; noise emitted at the companion's position reusing the existing `NoiseSystem` (AD-9/AD-10); per-floor counter on the `Companion` entity persisting under AD-6; `F` key triggers via a new `PlayerAction.DISTRACT` through the fixed AD-4 pipeline.
- 2026-08-03 — Story 4.2 implemented: per-floor distraction counter on `Companion`, `CompanionSystem.distract` emitting noise at the companion's position, `PlayerAction.DISTRACT` + `TurnEngine` case (refusal spends no turn), descent reset, `F` key. Verified headlessly (18 checks, 7 seeds); smoke-boot clean. Status ready-for-dev → in-progress → review.

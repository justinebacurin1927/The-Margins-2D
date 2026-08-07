# Building, Testing & Running The Margins 2D

The quirks below were rediscovered across Epics 1/3/4 of the old design and again in the Epic 1 remake
(flagged in two retrospectives). Read this once so you don't rediscover them a third time.

## Prerequisites

- **Java 17** and **Maven 3.8+** on PATH.
- **Offline mode (`-o`) is the norm**: all dependencies are already cached in `~/.m2`. Every command
  below runs offline. If a dependency is genuinely missing, drop `-o` for that one command (and note
  the new artifact so it stays cached).

## Module layout

```
pom.xml          # parent aggregator: core + desktop
core/            # headless game model + rules (com.margins.rogue.*) + MarginScreen/MarginsGame
desktop/         # LWJGL3 launcher only (com.margins.desktop.DesktopLauncher)
```

`core` holds **no libGDX render types in the model** (AD-2) — that's what makes it headless-testable.
The screen layer (`com.margins.MarginScreen`) is in `core` (not `desktop`) so it compiles under the
headless test build.

## Build & test

| Task | Command |
|------|---------|
| Full offline build + all tests | `mvn -o clean install` |
| Single test class | `mvn -o -q -pl core test -Dtest=<ClassName> -DfailIfNoTests=false` |
| Reinstall `core` to `~/.m2` (see the quirk below) | `mvn -o -pl core install` |
| Run the game | `mvn -o -pl desktop exec:java` |

- Tests live in `core/src/test/java` (JUnit 5; surefire 3.2.5). No test needs a display — headless
  by construction.
- `clean install` is the CI-truth command: it compiles both modules and runs the **full** suite. The
  surefire summary (`Tests run: N, Failures: 0, Errors: 0`) is the success bar.
- Always add a test that pins a fix (red → green → refactor). The 205-test net is the regression guard.

## ⚠️ The one quirk: reinstall `core` before `exec:java`

`desktop` declares `core` as a normal Maven dependency, so `mvn -pl desktop exec:java` resolves
`com.margins:core` from `~/.m2` — **not** from your working tree. After any change in `core/`, you
MUST run `mvn -o -pl core install` first or the launcher runs the stale artifact. The symptom of
forgetting: the game boots with old behavior, or fails looking for a file/class the new code deleted.

```
# after editing core/... :
mvn -o -pl core install     # ← never skip before running the game
mvn -o -pl desktop exec:java
```

`clean install` at the root does the reinstall for you (core is built and installed as part of it).

## Running a one-off harness (the `exec-maven-plugin` recipe)

For throwaway headless harnesses — e.g. a `Lwjgl3Files` file-path check that proves save/load behavior
without opening a window (the save-on-quit lifecycle path can't be exercised by a normal launch) —
run any class in `desktop`/`core` explicitly by overriding the main class:

```
mvn -o -q -pl desktop exec-maven-plugin:3.1.0:java -Dexec.mainClass=com.example.MyHarness
```

`exec-maven-plugin:3.1.0` is the pinned version (see `desktop/pom.xml`); quoting it fully avoids the
goal-version resolution. Harnesses are verification-only — do not commit them as the test suite
(commit real JUnit tests instead).

## Git house rules

- **`The Margin - Remake/.obsidian/workspace.json` is ALWAYS excluded from commits.** It is local
  Obsidian state and churns on every session; stage files explicitly rather than `git add -A`.
- One commit per story (with the `Co-Authored-By` line), pushed to `origin/main`.

## Verify recipe (the loop that works)

```
mvn -o -q -pl core test -Dtest=<NewTest> -DfailIfNoTests=false   # red first
# implement…
mvn -o clean install                                             # green + full suite + no regressions
```

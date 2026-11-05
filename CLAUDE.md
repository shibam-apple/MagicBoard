# Doubt Board

The full project spec is in [docs/SPEC.md](docs/SPEC.md). Read it before writing code; when it and a
request conflict, ask. The hard constraints in §1 are never broken.

## Build

- JDK 17+, Android SDK with platform 36. Point `sdk.dir` at it in `local.properties` (gitignored).
- `./gradlew assembleDebug` — build the app.
- `./gradlew testDebugUnitTest :core:adapt:test :core:net:test` — all unit tests (what CI runs).

## Modules

- Android libraries use the `doubtboard.android.library` convention plugin, pure-Kotlin modules
  `doubtboard.jvm.library` (both in `build-logic/`). Both wire JUnit 5 + Kotest assertions.
- `core/adapt` and `core/net` are plain JVM modules: no Android types there.
- Versions live only in `gradle/libs.versions.toml`.

## Rules of thumb

- Tests first for `core/math` and `core/adapt`.
- Maths shown on the board comes from the CAS, never from an LLM.
- Ask before adding a dependency > 5 MB or any network SDK. Check licences (GPL!).
- Unsure how a step should be explained → leave a `// PEDAGOGY:` note and ask.

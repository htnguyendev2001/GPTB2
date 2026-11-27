---
name: android-testing
description: Design, implement and execute Kotlin JVM unit tests and Android instrumentation tests for HTAVk.
---

# Android Testing Skill

## Test layers

- Local JVM tests: `app/src/test`; use for deterministic Kotlin domain logic that does not require Android framework runtime.
- Instrumentation tests: `app/src/androidTest`; use for Activity, UI, framework and device integration behavior.
- All test source must be Kotlin. Do not add Java test files.
- Prefer small tests with clear arrange / act / assert structure and descriptive names.

## Test design

- Cover normal behavior, boundary values, invalid input and failure paths.
- Keep tests independent; avoid shared mutable state and timing assumptions.
- Do not use real credentials, production services or personal data in tests.
- Use deterministic fakes/test doubles for external dependencies.
- Add regression coverage for bug fixes.
- Test reward points, XP, levels, streak calculations and ownership rules as pure Kotlin domain logic where possible.

## Commands

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
./gradlew connectedDebugAndroidTest
```

Connected tests require a running emulator or connected device. If unavailable, report them as not run rather than passed.

## Reporting

For each validation command, state whether it passed, failed or was not run. Include the first actionable failure and environment limitation without hiding it.

---
name: android-development
description: Implement and review native Android features in HTVA Vk using Kotlin and Android XML resources, including Activities, lifecycle, permissions, accessibility and API compatibility.
---

# Android Development Skill — HTVA Vk

## Before changing code

- Read the relevant Kotlin classes, XML layouts, manifest, styles, strings, tests and product specifications.
- Identify supported API range (minSdk 21) and target API (36).
- Kotlin is mandatory for all app and test source; do not add Java files.
- Keep the existing XML UI approach unless Jetpack Compose is explicitly approved.

## Implementation principles

- Use idiomatic Kotlin and null-safe APIs.
- Keep Activities focused on UI coordination; place business rules in testable Kotlin classes.
- Respect Activity/Fragment lifecycle; use lifecycle-aware coroutine scopes when asynchronous work is needed.
- Inject dispatchers where useful and keep disk/network work off the main thread.
- Avoid retaining Activity, View or Context references in long-lived objects.
- Use Android resources for user-visible strings, dimensions, colors and themes.
- Add content descriptions and meaningful labels; check touch targets, contrast and font scaling.
- Request only permissions required by the feature.
- Keep exported components explicit and validate external input.
- Guard APIs newer than minSdk 21.

## HTVA Vk-specific product constraints

- Learning content is sourced from public Google Sheets.
- Firebase Auth, Firestore and Storage are used for user-specific data/services.
- Room is the offline cache.
- Do not add a custom backend or Cloud Functions.
- Scope personal data by authenticated Firebase UID.
- MCQ answer visibility is public by product decision; no anti-cheat feature is required.
- Essay submissions are graded by teachers in Firebase Console.
- Streak and reward progression are calculated on Android and must have deterministic tests.

Verify these decisions against current approved specs before altering data contracts.

## Android 16 / API 36 checklist

- Validate edge-to-edge rendering, insets, status/navigation bars and gesture navigation.
- Check small/large screens and different font scales.
- Verify runtime permission flows and settings return paths.
- Review targetSdk 36 behavior changes.
- Test on API 36 emulator/device; compilation alone is insufficient.

## Validation

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

Run connected tests when a device is available. Report commands that could not be run and why.

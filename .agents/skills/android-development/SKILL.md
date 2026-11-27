---
name: android-development
description: Implement and review native Android features in this Java and XML application, including Activities, resources, manifests, permissions, lifecycle, accessibility, and API compatibility.
---

# Android Development Skill

## Before changing code

- Read the relevant Activity / class, layout XML, manifest, styles, strings, and tests.
- Identify supported API range (minSdk 21) and target API (36).
- Preserve the existing Java + XML approach unless migration is explicitly requested.

## Implementation principles

- Keep Activities focused on UI coordination; move reusable logic into focused classes.
- Respect Activity / Fragment lifecycle and avoid leaking Context or View references.
- Perform network, disk, and expensive work off the main thread.
- Use Android resources for strings, colors, dimensions, and themes.
- Add content descriptions and meaningful labels; check touch targets and contrast.
- Request only permissions required by the feature and explain each permission.
- Keep exported components explicit; validate intent filters and externally supplied input.
- Guard APIs newer than minSdk 21 with version checks or AndroidX compatibility APIs.
- Avoid logging tokens, credentials, personal information, or sensitive data.

## Android 16 / API 36 checklist

- Validate edge-to-edge rendering, insets, status/navigation bars, and gesture navigation.
- Check layouts on small screens, large screens, and different font scales.
- Verify runtime permission flows and settings return paths.
- Review platform behavior changes that apply to targetSdk 36.
- Test on API 36 emulator/device; compilation alone is insufficient.

## Validation

Run:

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

Run `./gradlew connectedDebugAndroidTest` when a device is available. Report commands that could not be run and why.

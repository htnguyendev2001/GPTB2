# HTAVk

HTAVk is a native Android learning, practice and self-assessment application.

## Application identity

| Item | Value |
|---|---|
| App name | HTAVk |
| Application ID | `com.htanh.appvk` |
| Gradle project | `HTAVk` |
| Language | Kotlin |
| UI | Android XML resources |
| Compile SDK / Target SDK | 36 |
| Minimum SDK | 21 |
| Android Gradle Plugin | 8.13.2 |
| Gradle Wrapper | 8.13 |
| Gradle runtime JDK | 17 |
| Kotlin Gradle plugin | 2.2.21 |
| Kotlin JVM target | 1.8 |

**All Android application and test source code must be Kotlin.** Do not add Java source files. XML layouts and Android resources remain valid UI resources; Kotlin is the required programming language.

HTAVk is separate from HTA Ck. HTAVk must use `com.htanh.appvk`; HTA Ck uses `com.htanh.appck`. Never interchange these IDs.

## Product scope

HTAVk supports learning and self-assessment. The planned product areas include:

- Learning content organized by subjects, chapters and topics.
- Question banks and multiple-choice questions.
- Practice sessions and exams.
- Essay submissions and teacher grading.
- Study-time tracking and learning streaks.
- Reward Points, XP, levels, badges and missions.

These are product requirements and roadmap areas; do not describe a feature as implemented until the source code confirms it.

## Architecture decisions to preserve

- Public learning content is sourced from Google Sheets.
- Firebase Authentication, Firestore and Firebase Storage are used for user-specific data and related app services.
- Room provides local/offline caching.
- No custom backend or Firebase Cloud Functions.
- Personal data is isolated by Firebase UID and must be protected by ownership checks.
- Public multiple-choice answers are accepted as part of the learning model; no anti-cheat system is required.
- Essay submissions are reviewed by teachers through Firebase Console; do not invent a separate teacher web application.
- Android is responsible for calculating study streaks and reward progression. Keep reward rules deterministic and testable.

Before implementing or changing data contracts, inspect the latest approved product/technical specifications in the repository and keep implementation aligned with them.

## Current implementation status

The repository was originally a small Java/XML equation-solver starter. The Kotlin foundation branch migrates application and test source to Kotlin, updates the application identity, and replaces the demo screen with an initial HTAVk dashboard shell.

The learning-content vertical slice is implemented on the `feat/learning-content-pipeline` branch: Google Sheets API v4 read-only batch fetch, header-based Kotlin parsing and validation, transactional Room snapshot cache, Flow-backed repository/ViewModel, and XML navigation from subjects to chapters, topics and questions. Live sync requires the public spreadsheet and restricted API key to be configured as described in [Google Sheets setup](docs/setup/GOOGLE_SHEETS_SETUP.md). Build/runtime validation is tracked in the feature PR. Authentication, Firebase user data, practice sessions, exams, essay submissions/grading and rewards remain separate work items.

## Learning content setup

Read [the learning-content technical specification](docs/features/LEARNING_CONTENT_MODULE_SPEC.md) and [Google Sheets setup](docs/setup/GOOGLE_SHEETS_SETUP.md). Configure `htavk.sheets.spreadsheetId` and `htavk.sheets.apiKey` in the ignored root `local.properties` file before testing live sync.

## Requirements

- Android Studio with Android SDK Platform 36.
- JDK 17 to run Gradle / AGP 8.13.2.
- Android SDK Build-Tools installed through SDK Manager.
- Emulator or Android device for instrumentation tests.

## Build and test

macOS / Linux:

```bash
./gradlew --version
./gradlew clean
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

Windows:

```bat
gradlew.bat --version
gradlew.bat clean
gradlew.bat testDebugUnitTest
gradlew.bat assembleDebug
```

Connected tests require an emulator or device:

```bash
./gradlew connectedDebugAndroidTest
```

Debug APK output:

```
app/build/outputs/apk/debug/app-debug.apk
```

## Repository guidance

- [AGENTS.md](AGENTS.md) — mandatory instructions for AI coding agents.
- `.agents/skills/` — reusable development, testing, build and review skills.
- `.github/agents/` — specialized GitHub Copilot agents.

## Engineering principles

1. Kotlin only for app, test and tooling source code.
2. Preserve the existing Android XML UI approach unless a Compose migration is explicitly approved.
3. Keep UI, domain rules, data access and Firebase integration separated.
4. Do not add a custom backend or Cloud Functions.
5. Keep Firebase rules UID-scoped; never trust a client-supplied UID as authorization.
6. Keep permissions minimal and avoid exposing personal data in logs, analytics or notifications.
7. Add deterministic tests for scoring, streaks, XP and reward calculations.
8. Never commit API keys, signing credentials, keystores, tokens or local machine configuration.

## Contribution workflow

1. Work on a focused feature branch.
2. Read AGENTS.md and the relevant skill.
3. Inspect existing source and product specifications before implementation.
4. Make the smallest coherent change.
5. Run unit tests and assemble the debug APK when the environment permits.
6. Report actual validation results and known limitations.
7. Open a Pull Request for review; do not push directly to master unless explicitly authorized.

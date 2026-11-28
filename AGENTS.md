# AI Agent Instructions — HTVA Vk

These instructions apply to the entire HTVA Vk repository.

## Project identity

- App name: HTVA Vk.
- Application ID and Gradle namespace: `com.htanh.appvk`.
- Related but separate app HTVA Ck uses `com.htanh.appck`. Never swap them.
- App module: `:app`.
- Language: **Kotlin only** for application, unit-test and instrumentation-test source.
- UI: Android XML resources in the current baseline.
- Build scripts: Gradle Groovy DSL.
- compileSdk / targetSdk: 36; minSdk: 21.
- Android Gradle Plugin: 8.13.2.
- Gradle Wrapper: 8.13.
- Gradle runtime JDK: 17.
- Kotlin Gradle plugin: 2.2.21; Kotlin JVM target: 1.8.

## Mandatory Kotlin-only rule

1. All new Android application and test code must be Kotlin.
2. Do not create or reintroduce `.java` source files.
3. Convert any legacy Java source to Kotlin when touching that area.
4. Kotlin source belongs under the module's supported Kotlin source roots; keep package declarations aligned with `com.htanh.appvk`.
5. XML layouts, drawables, menus, navigation resources and values resources are allowed; the Kotlin-only rule concerns programming source.
6. Do not migrate XML to Jetpack Compose unless the project owner explicitly approves it.

## Mandatory workflow

1. Inspect current branch, working tree, source, resources, manifest, Gradle files, specs and tests before editing.
2. Read the relevant skill under `.agents/skills/`.
3. Confirm task scope, acceptance criteria, dependencies and data-flow impact.
4. Preserve unrelated user changes and avoid broad speculative refactors.
5. Implement the smallest coherent Kotlin change.
6. Add/update Kotlin tests for changed behavior.
7. Run validation commands when the environment allows.
8. Review the diff for package, privacy, security, data ownership and dependency mistakes.
9. Report changed files, commands actually run, results and remaining risks.

## Product scope and decisions

HTVA Vk is a learning, practice and self-assessment app. Product areas include subjects, chapters, topics, questions, practice, exams, essay submissions/grading, study time, streaks, Reward Points, XP, levels, badges and missions.

Preserve these decisions unless the project owner explicitly changes them:

- Public learning content is sourced from Google Sheets.
- Firebase Authentication, Firestore and Firebase Storage support user-specific app data/services.
- Room is used for offline/local caching.
- No custom backend and no Firebase Cloud Functions.
- User-specific records are isolated by Firebase UID and protected by ownership checks.
- Public MCQ answers are accepted; there is no anti-cheat requirement.
- Teachers grade essay submissions through Firebase Console; do not invent a separate teacher web application.
- Android computes streak and reward progression. Keep calculations deterministic and unit-tested.

These are product/architecture decisions, not proof that every integration is currently implemented. Inspect actual code and approved specifications before changing contracts or claiming functionality.

## Learning-content implementation

- Read `docs/features/LEARNING_CONTENT_MODULE_SPEC.md` before changing the public learning-content data contract.
- Keep the public Google Spreadsheet read-only from the app and use one batch read for the five required tabs.
- Parse by header name; validate all rows and relationships before persisting.
- Replace the Room snapshot in one transaction only after a complete valid remote response.
- Keep cached content visible while refreshing and after network/validation failures.
- Do not create question-version history or move essay submissions/grading into public Sheets.
- Configure spreadsheet ID/API key only through ignored local.properties; never commit them.

## Product voice and learning motivation

- The app's Vietnamese user-facing language should carry the warmth of a loving boyfriend speaking affectionately to his girlfriend, with the purpose of encouraging her to learn and helping her feel accompanied.
- Prefer gentle, caring, intimate and encouraging wording over cold, bureaucratic, mechanical or overly academic phrasing. The emotional tone should make learning feel like something she is supported in, not judged on.
- Use natural Vietnamese address and affectionate expressions consistently. Keep the voice tender and sincere; avoid forced slang, excessive pet names, melodrama, or wording that sounds childish.
- Encourage effort, curiosity, consistency and small progress. Celebrate attempts and improvement, not only correct answers, high scores or streaks.
- When the learner makes a mistake, respond patiently: reassure her, explain the next step and invite another attempt. Never shame, scold, compare, threaten, guilt-trip or imply that affection depends on performance.
- Motivation copy may be affectionate and playful, but instructions, question content, answer explanations, accessibility labels and important notices must remain clear and unambiguous.
- Keep system feedback accurate. Do not disguise errors, sync failures, missing data or unfinished features with romantic wording, and never promise rewards or functionality that the app does not actually provide.
- Centralize reusable UI copy in Android string resources. When adding or revising user-facing text, review it against this voice rule and preserve the intended meaning.
- Examples of the intended tone (adapt naturally to context; do not repeat every phrase mechanically):
  - Empty state: "Hôm nay mình học một chút nhé? Em chọn một chủ đề mà em thích nha."
  - Correct answer: "Giỏi quá, em làm đúng rồi! Mình tiếp tục nhé."
  - Incorrect answer: "Không sao đâu em, sai một câu là mình biết thêm một điều mới. Thử lại nhé."
  - Encouragement: "Chậm một chút cũng được, miễn là em vẫn đang tiến bộ."
  - Session completion: "Em đã cố gắng rất tốt hôm nay. Anh tự hào vì em đã dành thời gian cho việc học."

## Android engineering

- Kotlin is mandatory; use idiomatic Kotlin and avoid Java interop unless required by an Android API.
- Keep Activities/Fragments focused on UI coordination.
- Respect lifecycle and cancellation; do not retain Activity, View or Context references in long-lived objects.
- Use coroutines only with lifecycle-aware scopes and explicit dispatchers.
- Avoid blocking the main thread with network, disk or expensive work.
- Use resources for user-facing strings, colors, dimensions and themes.
- Check accessibility, touch target sizes, content descriptions and font scaling.
- Request only necessary permissions and explain their user-facing purpose.
- Keep exported components explicit and validate external input/deep links.
- Maintain minSdk 21 compatibility and guard newer APIs.
- Validate Android 16 / API 36 edge-to-edge and system-bar behavior.

## Data and Firebase security

- Never treat a client-provided UID as authorization.
- Enforce ownership in Firestore/Storage security rules and in repository query design.
- Keep public learning content separate from user-private records.
- Do not store private answers, essay content or user data in logs or analytics.
- No custom backend or Cloud Functions unless explicitly approved.
- Do not introduce additional Firebase products or third-party SDKs without a documented reason and approval.
- Never commit secrets, service-account keys, keystores, tokens or local machine settings.

## Gradle and dependencies

- Use the checked-in Gradle Wrapper.
- Keep AGP 8.13.2 and Gradle 8.13 compatible.
- JDK 17 runs Gradle; Kotlin bytecode target remains 1.8.
- Pin dependency versions; no dynamic versions.
- Explain plugin/dependency additions and upgrades.
- Keep repositories centralized in settings.gradle.

## Build and validation

```bash
./gradlew --version
./gradlew clean
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

When a device/emulator is available:

```bash
./gradlew connectedDebugAndroidTest
```

Do not claim a build/test passed unless it was executed and succeeded. Separate build success from runtime/device verification.

## Git and delivery

- Work on a focused feature branch; do not push directly to master unless explicitly authorized.
- Do not force-push, rewrite history, delete branches or merge without approval.
- Do not commit generated build output, local configuration or credentials.
- Keep commits focused and descriptive.
- PR descriptions must include scope, test evidence, risks and manual checks still needed.

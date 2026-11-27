# AI Agent Instructions

These instructions apply to the entire GPTB2 repository.

## Repository facts

- Native Android application.
- Language: Java (source / target compatibility 8).
- UI: Android XML resources.
- Build scripts: Gradle Groovy DSL.
- Application module: `:app`.
- Application ID and namespace: `com.example.gptb2`.
- `compileSdk` / `targetSdk`: 36; `minSdk`: 21.
- Android Gradle Plugin: 8.13.2.
- Gradle Wrapper: 8.13.
- JDK 17 is required to run Gradle.

## Mandatory workflow

1. Inspect relevant source, resources, manifest, build files, and tests before editing.
2. Confirm the task scope and identify behavior / compatibility risks.
3. Prefer focused changes; do not perform unrelated refactors or dependency upgrades.
4. Preserve Java + XML unless the project owner explicitly approves a migration.
5. Keep applicationId, namespace, minSdk, versionCode, and versionName unchanged unless requested.
6. Add or update tests for changed behavior.
7. Run the validation commands below when an execution environment is available.
8. Review the final diff and report files changed, checks run, results, and remaining risks.

## Build and validation

```bash
./gradlew --version
./gradlew clean
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

When an emulator or physical device is available:

```bash
./gradlew connectedDebugAndroidTest
```

Do not claim a build or test passed unless the command actually ran and succeeded. If Android SDK, JDK, network, or device access is unavailable, state that limitation explicitly.

## Android engineering rules

- Use AndroidX and the existing dependency set unless a change is justified.
- Keep lifecycle-aware work out of Activity constructors and avoid retaining Activity references.
- Handle runtime permissions with least privilege; never add permissions speculatively.
- Keep exported components explicit and review intent filters / deep links for security.
- Do not log secrets, authentication tokens, personal data, or sensitive payloads.
- Avoid blocking the main thread with disk, network, or expensive computation.
- Use resources for user-visible strings, dimensions, colors, and themes.
- Check accessibility: labels, touch target sizes, contrast, and content descriptions.
- Validate Android 16 / API 36 edge-to-edge and system-bar behavior.
- Consider minSdk 21 compatibility when using platform APIs; guard newer APIs.

## Gradle and dependency rules

- Use the checked-in Gradle Wrapper; do not require a globally installed Gradle.
- Keep AGP and Gradle versions compatible.
- JDK 17 runs AGP 8.13.2; Java source compatibility remains 8.
- Pin dependency versions. Do not introduce dynamic versions such as `+`.
- Explain dependency additions, removals, and upgrades in the PR.
- Never commit local SDK paths, machine-specific settings, signing files, or credentials.

## Git and delivery

- Work on a dedicated branch; do not push directly to `master` unless explicitly instructed.
- Do not rewrite history or force-push without explicit approval.
- Keep commits focused and messages descriptive.
- PR descriptions must include summary, test evidence, risks, and manual verification still required.

## Skills and agents

- Read the relevant skill under `.agents/skills/` before specialized work.
- Use GitHub Copilot custom agents under `.github/agents/` when the task matches their role.
- The role files are guidance, not a substitute for inspecting the code or verifying results.

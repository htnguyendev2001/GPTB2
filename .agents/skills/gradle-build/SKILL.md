---
name: gradle-build
description: Maintain and troubleshoot the GPTB2 Gradle build, Android Gradle Plugin, wrapper, SDK configuration, dependency resolution, and build variants.
---

# Gradle Build Skill

## Project toolchain

- Android Gradle Plugin: 8.13.2.
- Gradle Wrapper: 8.13.
- Gradle runtime JDK: 17.
- compileSdk / targetSdk: 36.
- Java source / target compatibility: 8.
- Groovy build scripts.

## Safe build changes

- Inspect root and module build scripts plus settings before changing plugin or dependency versions.
- Use `./gradlew` / `gradlew.bat`; do not assume a global Gradle installation.
- Keep AGP and Gradle versions aligned with the official compatibility table.
- Keep repositories centralized in settings.gradle; respect FAIL_ON_PROJECT_REPOS.
- Pin versions; avoid dynamic versions and unnecessary dependency churn.
- Do not hardcode local Android SDK paths or machine-specific JDK paths.
- Explain why each plugin / dependency change is needed.

## Troubleshooting sequence

1. Capture the exact failing task and first meaningful exception.
2. Check Gradle runtime JDK with `./gradlew --version`.
3. Check wrapper distribution and AGP compatibility.
4. Check SDK Platform 36 and Build Tools installation.
5. Check repository / dependency resolution and network failures.
6. Make one focused correction at a time.

## Validation

```bash
./gradlew --version
./gradlew clean
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

Do not report success unless the command completed successfully. Separate configuration validation from a full Android build.

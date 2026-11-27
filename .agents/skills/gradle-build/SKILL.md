---
name: gradle-build
description: Maintain and troubleshoot the HTVA Vk Gradle build, Kotlin Android plugin, Android Gradle Plugin, wrapper, SDK configuration and dependencies.
---

# Gradle Build Skill — HTVA Vk

## Project toolchain

- Android Gradle Plugin: 8.13.2.
- Gradle Wrapper: 8.13.
- Gradle runtime JDK: 17.
- Kotlin Gradle plugin: 2.2.21.
- compileSdk / targetSdk: 36; minSdk: 21.
- Kotlin JVM target: 1.8.
- Gradle build scripts use Groovy DSL.

## Safe build changes

- Inspect root/module build scripts and settings before changing plugin or dependency versions.
- Use ./gradlew / gradlew.bat; do not assume a global Gradle installation.
- Keep AGP, Gradle and Kotlin plugin versions compatible.
- Keep repositories centralized in settings.gradle; respect FAIL_ON_PROJECT_REPOS.
- Pin dependency versions; avoid dynamic versions and unnecessary dependency churn.
- Do not hardcode local Android SDK paths or machine-specific JDK paths.
- Explain why each plugin/dependency change is needed.
- Kotlin is the only allowed language for application and test source.

## Troubleshooting sequence

1. Capture the exact failing task and first meaningful exception.
2. Check Gradle runtime JDK with ./gradlew --version.
3. Check wrapper distribution and AGP/Kotlin plugin compatibility.
4. Check SDK Platform 36 and Build Tools installation.
5. Check repository/dependency resolution and network failures.
6. Make one focused correction at a time.

## Validation

```bash
./gradlew --version
./gradlew clean
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

Do not report success unless the command completed successfully. Separate configuration validation from a full Android build.

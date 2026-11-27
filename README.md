# GPTB2

GPTB2 is a native Android application built with **Java**, **XML layouts**, and **Gradle (Groovy DSL)**.

## Project status

| Item | Value |
|---|---|
| Application ID | `com.example.gptb2` |
| Namespace | `com.example.gptb2` |
| Compile SDK | Android 36 |
| Target SDK | Android 36 |
| Minimum SDK | Android 21 |
| Android Gradle Plugin | 8.13.2 |
| Gradle Wrapper | 8.13 |
| Gradle runtime JDK | 17 |
| Java source / target | 8 |

The current repository contains a starter activity and XML-based UI. Keep this README aligned with the actual implementation as features are added.

## Requirements

- Android Studio with Android SDK Platform 36 installed.
- JDK 17 to run Gradle / Android Gradle Plugin 8.13.2.
- Android SDK Build-Tools installed through SDK Manager.
- An emulator or Android device for instrumentation tests.

> Java source compatibility remains Java 8. This is independent from the JDK 17 required to run Gradle.

## Build

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

The debug APK is normally generated at:

```
app/build/outputs/apk/debug/app-debug.apk
```

Run connected instrumentation tests when an emulator or device is available:

```bash
./gradlew connectedDebugAndroidTest
```

## Project structure

```
.
├── app/
│   ├── build.gradle
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/example/gptb2/
│       │   └── res/
│       ├── test/          # Local JVM tests
│       └── androidTest/   # Device / emulator tests
├── gradle/wrapper/
├── .agents/skills/        # Reusable AI skills
├── .github/agents/        # GitHub Copilot custom agents
├── AGENTS.md              # Repository-wide AI instructions
├── build.gradle
└── settings.gradle
```

## AI-assisted development

- Read [AGENTS.md](AGENTS.md) before making code changes.
- Reusable skills live in `.agents/skills/<skill-name>/SKILL.md`.
- GitHub Copilot custom agents live in `.github/agents/*.agent.md`.
- Select the smallest relevant skill / agent for each task; do not apply every workflow to every change.

## Development guidelines

1. Preserve the existing Java + XML architecture unless a task explicitly approves a migration.
2. Keep UI, business logic, and data access responsibilities separated as the codebase grows.
3. Do not commit API keys, signing credentials, keystores, tokens, or local machine configuration.
4. Add or update tests for behavior changes.
5. Keep Android permissions minimal and explain why each new permission is needed.
6. Review target-SDK behavior changes on Android 36 before release.
7. Update documentation when build commands, architecture, or project conventions change.

## Android 36 notes

Targeting API 36 opts the app into the platform behavior changes applicable to Android 16. Validate edge-to-edge layouts, system bars, navigation, orientation / large-screen behavior, and permission flows on representative devices before publishing. A successful compile alone does not establish runtime or Play policy readiness.

## Contribution workflow

1. Create a focused feature or maintenance branch.
2. Make the smallest coherent change.
3. Run unit tests and assemble the debug APK.
4. Run connected tests when a device is available.
5. Review the diff for regressions, secrets, permissions, and documentation.
6. Open a Pull Request with scope, validation results, and known limitations.

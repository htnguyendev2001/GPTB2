---
name: code-review
description: Review HTAVk changes for correctness, security, Kotlin/Android lifecycle issues, API compatibility, test coverage and regressions.
---

# Code Review Skill

## Review order

1. Understand intended behavior and changed-file scope.
2. Read the complete diff and nearby callers/resources/tests.
3. Look for correctness defects and regressions before style concerns.
4. Check security, privacy, lifecycle, threading and platform compatibility.
5. Verify tests and documentation match implementation.
6. Confirm all app and test source remains Kotlin; no Java files are introduced.

## Android-specific checks

- Manifest permissions, exported components, intent filters and deep links.
- Activity lifecycle, Context leaks, coroutine cancellation and main-thread blocking.
- API 21 compatibility and guarded newer APIs.
- API 36 edge-to-edge, insets, system bars and target-SDK behavior.
- Accessibility labels, content descriptions, touch targets and contrast.
- Resource qualifiers and configuration changes.

## Product/data checks

- Firebase UID ownership and Firestore/Storage rules.
- Separation of public learning content and user-private data.
- No custom backend or Cloud Functions.
- Correctness of answer evaluation, essay submission, streak and reward rules.

## Build / dependency checks

- AGP, Gradle Wrapper and Kotlin plugin compatibility.
- JDK 17 for Gradle runtime; Kotlin JVM target 1.8.
- Pinned dependency versions and no accidental repository changes.
- No secrets, keystores, local SDK paths or generated build artifacts.

## Findings format

Report findings by severity (Critical, High, Medium, Low), with file and line references where available. Explain impact and a concrete fix. If no findings are identified, state reviewed scope and validation limitations; do not imply exhaustive proof of correctness.

---
name: code-review
description: Review GPTB2 changes for correctness, security, Android lifecycle issues, API compatibility, test coverage, and unintended regressions.
---

# Code Review Skill

## Review order

1. Understand the intended behavior and changed-file scope.
2. Read the complete diff and nearby callers / resources / tests.
3. Look for correctness defects and regressions before style concerns.
4. Check security, privacy, lifecycle, threading, and platform compatibility.
5. Verify tests and documentation match the implementation.

## Android-specific checks

- Manifest permissions, exported components, intent filters, and deep links.
- Activity lifecycle, Context leaks, main-thread blocking, and cancellation.
- API 21 compatibility and guarded newer APIs.
- API 36 edge-to-edge, insets, system bars, and target-SDK behavior.
- Accessibility labels, content descriptions, touch targets, and contrast.
- Resource qualifiers and configuration changes.

## Build / dependency checks

- AGP and Gradle Wrapper compatibility.
- JDK 17 for Gradle runtime; Java source compatibility remains 8.
- Pinned dependency versions and no accidental repository changes.
- No secrets, keystores, local SDK paths, or generated build artifacts.

## Findings format

Report findings by severity (Critical, High, Medium, Low), with file and line references where available. Explain impact and a concrete fix. If no findings are identified, state the reviewed scope and validation limitations; do not imply exhaustive proof of correctness.

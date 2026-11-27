# Configure the HTAVk public learning spreadsheet

This setup is required before the app can download live learning content. The app remains usable with its last valid Room cache when the network is unavailable.

## 1. Prepare one Google Spreadsheet

Create one spreadsheet and add these five required tabs:

- `SUBJECTS`
- `CHAPTERS`
- `TOPICS`
- `QUESTIONS`
- `QUESTION_OPTIONS`

Use the exact v1 column headers in [LEARNING_CONTENT_MODULE_SPEC.md](../features/LEARNING_CONTENT_MODULE_SPEC.md). Set the spreadsheet locale to United States (or a locale that formats decimal values with a dot). Keep the header row at row 1. The parser matches headers by name, so columns can be reordered, but do not rename required headers.

<strong>Approved Project Owner decision (2026-11-27):</strong> share the workbook as **Anyone with the link — Viewer**. The Android app reads it directly; it must never write to Sheets. This explicitly supersedes the older private-sharing instruction in the uploaded workbook GUIDE, which should be updated by the spreadsheet owner.

Treat every cell in this workbook as public information. Do not put private user records, essay submissions, grading results, credentials, API keys, tokens or personal data in it. Essay attempts and grading remain UID-scoped Firebase data.

## 2. Enable Google Sheets API

1. Open Google Cloud Console and select/create the project that owns the API key.
2. Enable **Google Sheets API**.
3. Create an API key.
4. Restrict the key to Android applications.
5. Add the application package `com.htanh.appvk` and the SHA-1 certificate fingerprints for each build variant you distribute.
6. Under API restrictions, allow only Google Sheets API.

The API key is included in the APK because this is a public read-only client integration; assume it can be extracted by users. Restrict it to the Android package/signing certificate(s) and Google Sheets API where supported, monitor usage/quotas, rotate it if abused, and never commit it to Git. An API key identifies the calling project; it does not make private spreadsheets readable.

## 3. Configure local.properties

Open the repository root `local.properties` (Android Studio creates this file and it is already ignored by Git). Preserve the existing `sdk.dir` line and add:

```properties
htavk.sheets.spreadsheetId=YOUR_SPREADSHEET_ID
htavk.sheets.apiKey=YOUR_RESTRICTED_API_KEY
```

The spreadsheet ID is the segment between `/d/` and `/edit` in its URL.

Do not put the values in `gradle.properties`, source code, README, screenshots, commits or CI logs.

## 4. Build and run

```bash
./gradlew clean testDebugUnitTest assembleDebug
```

Open HTAVk, choose **Mở kho học tập**, and use **Làm mới** to retry a sync. The first open triggers a sync automatically.

## 5. Validate the workbook before sharing

- Sharing is set to Anyone with the link — Viewer (not Editor).
- The workbook contains only information approved for public access.
- Every required tab exists and has a header row.
- IDs are unique within each tab and remain stable when rows are reordered.
- Every chapter references a subject, every topic references a chapter, every question references a topic and every option references a question.
- Every MCQ has at least two options and exactly one correct option.
- Essay questions have no option rows.
- `sort_order` is an integer; `points` is a non-negative decimal using a dot.
- `is_active` accepts TRUE/FALSE, 1/0 or YES/NO; blank defaults to TRUE except `QUESTION_OPTIONS.is_correct`, which is required.
- Keep essay attempts and grades out of this public spreadsheet.

A failed fetch or invalid workbook never clears the existing Room cache. Fix the spreadsheet and retry.

# Configure the HTAVk public learning spreadsheet

This setup is required before the app can download live learning content. The app remains usable with its last valid Room cache when the network is unavailable.

## 1. Prepare one Google Spreadsheet

Create one spreadsheet and add these five required tabs:

- `SUBJECTS`
- `CHAPTERS`
- `TOPICS`
- `QUESTIONS`
- `QUESTION_OPTIONS`

Use the exact v1 column headers in [LEARNING_CONTENT_MODULE_SPEC.md](../features/LEARNING_CONTENT_MODULE_SPEC.md). Keep the header row at row 1. The parser matches headers by name, so columns can be reordered, but do not rename required headers. The app requests `UNFORMATTED_VALUE` from Sheets so numeric cells are returned as raw numbers rather than locale-formatted display strings; enter numeric fields as numeric spreadsheet values, not localized text.

**Project Owner decision (2026-11-27):** share the workbook as **Anyone with the link — Viewer**. The Android app reads it directly; it must never write to Sheets. This supersedes the older private-sharing instruction in the workbook GUIDE.

Treat every cell in this workbook as public information. Do not put private user records, essay submissions, grading results, credentials, API keys, tokens or personal data in it. Essay attempts and grading remain UID-scoped Firebase data.

## 2. Enable and restrict Google Sheets API

1. Open Google Cloud Console and select/create the project that owns the API key.
2. Enable **Google Sheets API**.
3. Create an API key.
4. Under **API restrictions**, select **Restrict key** and allow only **Google Sheets API**.
5. Under **Application restrictions**, select **Android apps**.
6. Add package name `com.htanh.appvk` and the SHA-1 signing certificate fingerprint for the build you install:
   - For a locally built debug APK, use the SHA-1 shown by `./gradlew signingReport` under `Variant: debug`.
   - For a release APK, use the release signing certificate SHA-1.
   - If distributing through Google Play App Signing, add the **App signing key certificate** SHA-1 from Play Console, not only the upload key.
7. Save the key restrictions.

The app now automatically sends `X-Android-Package` and `X-Android-Cert` headers. It obtains the package name and SHA-1 from the installed app's actual signing certificate, so no certificate fingerprint needs to be copied into `local.properties`. The certificate value sent in the header is uppercase hexadecimal without colons; Google Cloud Console expects the same fingerprint displayed with colons.

The API key is included in the APK because this is a public read-only client integration; assume it can be extracted by users. Keep both Android application restrictions and the Google Sheets API restriction enabled, monitor usage/quotas, rotate the key if abused, and never commit it to Git. An API key identifies the calling project; it does not make private spreadsheets readable.

## 3. Configure local.properties

Copy `local.properties.example` to `local.properties` in the repository root. Android Studio may already have created this file; if so, preserve its existing `sdk.dir` line and add/update the two properties below. The real `local.properties` is ignored by Git and must stay untracked. The spreadsheet ID is prefilled in the example; replace the API-key placeholder with your restricted key:

```properties
htavk.sheets.spreadsheetId=1E65nmazXqOSANbg5_OXH0O_DpL2KTZfIyfBOT0VFFuU
htavk.sheets.apiKey=YOUR_RESTRICTED_API_KEY
```

The spreadsheet ID is the segment between `/d/` and `/edit` in its URL.

Do not put the values in `gradle.properties`, source code, README, screenshots, commits or CI logs.

## 4. Build and run

```bash
./gradlew clean testDebugUnitTest assembleDebug
```

Open HTVA Vk, choose **Mở kho học tập**, and use **Làm mới** to retry a sync. The first open triggers a sync automatically.

## 5. Validate the workbook before sharing

- Sharing is set to Anyone with the link — Viewer (not Editor).
- The workbook contains only information approved for public access.
- Every required tab exists and has a header row.
- IDs are unique within each tab and remain stable when rows are reordered.
- Every chapter references a subject, every topic references a chapter, every question references a topic and every option references a question.
- Every MCQ has at least two options and exactly one correct option.
- Essay questions have no option rows.
- `sort_order` is an integer used for ordering; `max_score` is a non-negative numeric cell. Do not store numeric values as localized text.
- `is_active` accepts TRUE/FALSE, 1/0 or YES/NO; blank defaults to TRUE except `QUESTION_OPTIONS.is_correct`, which is required.
- Keep essay attempts and grades out of this public spreadsheet.

A failed fetch or invalid workbook never clears the existing Room cache. Fix the spreadsheet and retry.

# HTAVk — Learning Content Module Specification

- Document ID: HTAVK-SPEC-LEARNING-001
- Version: 1.1
- Status: Implementation baseline; question practice UI in progress
- Platform: Android native, Kotlin + XML
- Application ID: `com.htanh.appvk`
- Source of truth: one public, read-only Google Spreadsheet
- Local cache: Room
- Backend policy: no custom backend and no Firebase Cloud Functions

## 1. Purpose and scope

The Learning Content module loads the public learning catalogue from one Google Spreadsheet, validates it, maps it into typed Kotlin domain models, atomically replaces the local Room snapshot, and exposes cached content to the Android UI.

The first implementation covers:

1. Subjects
2. Chapters
3. Topics
4. Questions
5. Question options

The workbook contract also reserves tabs for app configuration, exams, exam-question mapping, badges, levels and missions. Those future domains are documented here but are not part of this first vertical slice.

### Explicit exclusions

- No question version history or historical snapshots.
- No editing of public learning content from the Android app; Sheets is read-only from the app.
- No essay submission or grading records in the public spreadsheet.
- No custom backend, Cloud Functions or anti-cheat service.
- No Firebase dependency for public catalogue content.
- Project Owner approved public read-only spreadsheet access on 2026-11-27: Anyone with the link — Viewer. This supersedes the older private-sharing instruction in the uploaded workbook GUIDE.
- No Compose migration; UI remains XML with Kotlin controllers/ViewModels.

Essay prompts may be represented as public questions. A learner's essay response and its grading workflow belong to the user-specific Firebase data flow, not this public workbook.

## 2. Approved product constraints and implementation assumptions

### Approved constraints carried forward

- Use one Google Spreadsheet for public learning content.
- Subjects, chapters and topics are editable by the content owner in that spreadsheet.
- The app automatically refreshes content from Sheets.
- MCQ answer keys are public as part of the product model.
- Do not keep question version history.
- Essay submission/grading data is separate from the public content workbook.
- Room is the offline cache.
- Firebase Auth/Firestore/Storage remain for user-specific app data; they are not required to read this public catalogue.
- No custom backend or Cloud Functions.

### Explicit implementation contract introduced by this spec

The source workbook GPTB2_Google_Sheets_Template_v1.0.xlsx was inspected on 2026-11-27. The tables below document the actual workbook headers and distinguish fields currently consumed by the first Android implementation from fields reserved for a later model/UI increment. The Android parser accepts the original workbook names; the spreadsheet does not need to be renamed to match internal Kotlin property names.

## 3. Workbook contract

One spreadsheet contains the following tabs. Tab names are case-sensitive. Header names are trimmed and matched case-insensitively by the parser.

### 3.1 Required tabs for v1 implementation

#### SUBJECTS

| Column | Type | Required | Meaning |
|---|---|---:|---|
| subject_id | String | Yes | Stable unique ID; do not reuse after deletion |
| name | String | Yes | Display name |
| description | String | No | Short description |
| icon | Material icon identifier | No | Workbook value such as calculate/science; not a URL. Preserved as Subject.iconName; not interpreted as a URL |
| image_url | URL/String | No | Optional subject cover; mapped to the current coverImageUrl field |
| sort_order | Integer | No | Display order; default 0 |
| is_active | Boolean | No | true when blank; false hides the item |
| updated_at | ISO-8601/String | No | Editorial timestamp, informational only |

#### CHAPTERS

| Column | Type | Required | Meaning |
|---|---|---:|---|
| chapter_id | String | Yes | Stable unique ID |
| subject_id | String | Yes | Foreign key to SUBJECTS.subject_id |
| name | String | Yes | Display name |
| description | String | No | Short description |
| sort_order | Integer | No | Display order; default 0 |
| is_active | Boolean | No | true when blank |
| updated_at | ISO-8601/String | No | Informational timestamp |

#### TOPICS

| Column | Type | Required | Meaning |
|---|---|---:|---|
| topic_id | String | Yes | Stable unique ID |
| chapter_id | String | Yes | Foreign key to CHAPTERS.chapter_id |
| name | String | Yes | Display name |
| description | String | No | Short description |
| sort_order | Integer | No | Display order; default 0 |
| is_active | Boolean | No | true when blank |
| updated_at | ISO-8601/String | No | Informational timestamp |

#### QUESTIONS

| Column | Type | Required | Meaning |
|---|---|---:|---|
| question_id | String | Yes | Stable unique ID |
| subject_id | String | Yes | Redundant hierarchy key; validated against topic -> chapter -> subject |
| chapter_id | String | Yes | Redundant hierarchy key; validated against topic -> chapter |
| topic_id | String | Yes | Foreign key to TOPICS.topic_id |
| question_type | Enum | Yes | MCQ or ESSAY |
| difficulty | Enum | No | EASY, MEDIUM or HARD; defaults to MEDIUM |
| content | String | Yes | Question prompt; plain text in v1 |
| image_url | URL/String | No | Optional question image; preserved as LearningQuestion.imageUrl (rendering is pending) |
| explanation | String | No | Public explanation shown by the practice feature |
| max_score | Decimal | No | Non-negative score value; mapped to the current points field, defaults to 1 |
| estimated_time_seconds | Integer | No | Estimated duration; preserved as nullable LearningQuestion.estimatedTimeSeconds |
| sort_order | Integer | No | Display order; defaults to 0 |
| is_active | Boolean | No | true when blank |
| updated_at | ISO-8601/String | No | Informational timestamp |

#### QUESTION_OPTIONS

| Column | Type | Required | Meaning |
|---|---|---:|---|
| option_id | String | Yes | Stable unique ID |
| question_id | String | Yes | Foreign key to QUESTIONS.question_id |
| option_label | String | No | Display label such as A/B/C/D; preserved as QuestionOption.label |
| content | String | Yes | Option text; parser also accepts the legacy option_text alias |
| image_url | URL/String | No | Optional option image; preserved as QuestionOption.imageUrl (rendering is pending) |
| is_correct | Boolean | Yes | Public answer key |
| sort_order | Integer | No | Display order; defaults to 0 |

### 3.2 Reserved tabs for subsequent modules

These tabs belong to the same workbook contract but are not read by the first implementation:

- GUIDE: workbook editing instructions and schema version.
- APP_CONFIG: key/value configuration for content owners. It must not contain API keys, credentials or private user data.
- EXAMS: exam metadata.
- EXAM_QUESTIONS: ordered mapping between exams and questions.
- BADGES: public badge definitions.
- LEVELS: public level thresholds.
- MISSIONS: public mission definitions.

Essay attempts, essay submissions, grading comments and grading outcomes are deliberately excluded from the public workbook. They are user-specific records handled by the Firebase data layer and must be UID-scoped.

## 4. IDs, relationships and validation

- IDs are stable opaque strings. They may be human-readable (for example `SUBJ_MATH`) or UUIDs; the app must not infer meaning from an ID.
- IDs are unique within their own tab.
- Never use row number as an ID.
- Renaming or reordering a row must not change its ID.
- Deleting a parent row while child rows still reference it invalidates the snapshot.
- A chapter must reference an existing subject.
- A topic must reference an existing chapter.
- A question must reference an existing topic.
- An option must reference an existing question.
- A MCQ must have at least two options and exactly one option with `is_correct=true`.
- An ESSAY question must not have QUESTION_OPTIONS rows.
- Unknown question types, malformed numbers, malformed booleans, duplicate IDs, missing required headers and broken foreign keys invalidate the whole incoming snapshot.
- If validation fails, do not partially write data. Keep the previous Room snapshot intact and report a sync error.
- Blank optional values use documented defaults. Blank required values are errors.
- Boolean values accepted by the parser: true/false, 1/0, yes/no (case-insensitive).
- Decimal values use a dot as decimal separator. Thousands separators are not accepted; set the spreadsheet locale to United States (or another locale that renders decimals with a dot).
- Sort order is ascending, then display name as a stable secondary sort.

## 5. Remote transport and security

### Transport

Use Google Sheets API v4 read-only `spreadsheets.values:batchGet` over HTTPS. Fetch the five required ranges in one request:

- `SUBJECTS!A:Z`
- `CHAPTERS!A:Z`
- `TOPICS!A:Z`
- `QUESTIONS!A:Z`
- `QUESTION_OPTIONS!A:Z`

The spreadsheet must be shared for public read access. The Android app must not request write scopes or ask the user to sign into Google to read this public catalogue.

### Configuration

Configure the following local Gradle properties in the developer's ignored `local.properties` file:

```properties
htavk.sheets.spreadsheetId=YOUR_PUBLIC_SPREADSHEET_ID
htavk.sheets.apiKey=YOUR_RESTRICTED_GOOGLE_API_KEY
```

The API key is necessarily present in the distributed Android client and is not a secret. Restrict it in Google Cloud Console to the Android package `com.htanh.appvk`, the appropriate signing certificate SHA-1 fingerprints, and the Google Sheets API only. Never commit the key or spreadsheet credentials.

If either value is missing, the app must keep showing cached content and expose a clear configuration error when sync is attempted. Do not silently use a guessed spreadsheet ID.

### Network behavior

- Request timeout: 15 seconds to connect and 20 seconds to read.
- Execute all network work off the main thread.
- Do not log API keys, full request URLs, response bodies or user data.
- Treat HTTP 401/403 as access/configuration errors; 404 as invalid spreadsheet ID; 429 and 5xx as retryable; other non-2xx responses as sync failures.
- No background polling is required in v1. Sync automatically when the Learning screen is opened and provide a manual refresh action.
- Do not start concurrent sync requests. A new refresh while one is active is ignored or coalesced.

## 6. Internal domain model

The network layer must not expose raw JSON, spreadsheet rows or column positions to the UI.

Typed Kotlin models:

- Subject(id, name, description, iconUrl, coverImageUrl, sortOrder, isActive, iconName)
- Chapter(id, subjectId, name, description, sortOrder, isActive)
- Topic(id, chapterId, name, description, sortOrder, isActive)
- LearningQuestion(id, topicId, type, difficulty, content, explanation, points, sortOrder, isActive, imageUrl, estimatedTimeSeconds)
- QuestionOption(id, questionId, text, isCorrect, sortOrder, label, imageUrl)
- LearningCatalog(subjects, chapters, topics, questions, options)

Room 2.7.2 is pinned for this implementation because the app retains minSdk 21; Room 2.8.x raises the minimum Android API to 23. The source `updated_at` value is informational and is not a version-history mechanism. v1 stores only the most recent valid snapshot.

## 7. Data pipeline

```text
Public Google Spreadsheet
        |
        | HTTPS Sheets API v4 values:batchGet
        v
GoogleSheetsRemoteDataSource
        |
        | raw value ranges
        v
LearningSheetParser + CatalogValidator
        |
        | typed LearningCatalog or validation error
        v
LearningContentRepository
        |
        | one Room transaction: replace complete snapshot
        v
Room database (offline source for UI)
        |
        | Flow<LearningCatalog>
        v
LearningViewModel
        |
        v
Learning UI (subjects -> chapters -> topics -> questions)
```

### Cold start / opening the Learning screen

1. Observe Room immediately and render any cached catalogue.
2. Start a single remote sync in the background.
3. Fetch all required ranges in one batch request.
4. Parse headers and rows into typed models.
5. Validate IDs, types, values and relationships.
6. If valid, replace all five content tables in one Room transaction and write the last-successful-sync timestamp.
7. Room Flow emits the new catalogue and the UI updates automatically.
8. If remote fetch or validation fails, keep cached data unchanged and show a non-blocking status/error with retry.

### Offline behavior

- If Room has a valid cached snapshot, show it immediately without waiting for the network.
- If the device is offline, do not clear or partially modify cached data.
- If no cache exists, show an empty state with retry guidance.
- The UI must distinguish “no content yet” from “sync failed”.

### Consistency

A content refresh is all-or-nothing. Never clear Room before remote fetch and validation complete. Use one database transaction to delete and insert the five tables. Persist `last_successful_sync_at` only in the same successful transaction. Keep only one current snapshot; do not create content-version tables.

## 8. Room schema

Database: `htavk_learning_content.db`, version 1.

Tables:

- `subjects`: primary key `id`.
- `chapters`: primary key `id`, index on `subjectId`.
- `topics`: primary key `id`, index on `chapterId`.
- `learning_questions`: primary key `id`, index on `topicId`.
- `question_options`: primary key `id`, index on `questionId`.
- `sync_metadata`: key/value metadata; v1 uses `last_successful_sync_at`.

Room is the offline read source. Do not query Sheets directly from Activities or Views.

## 9. Repository and UI responsibilities

### Repository

- Exposes a Flow of typed catalogue data from Room.
- Exposes a suspend sync operation returning a typed result.
- Coordinates remote fetch, parsing, validation and transactional persistence.
- Does not expose Android Views or Activity references.

### ViewModel

- Observes repository Flow.
- Initiates refresh on Learning screen entry.
- Prevents duplicate concurrent sync.
- Maintains selected subject/chapter/topic navigation state.
- Exposes loading, cached-content, empty, success and error states.

### UI

- Main dashboard provides an entry point to Learning.
- Learning screen navigation: Subjects -> Chapters -> Topics -> Questions.
- Show item counts where useful.
- Provide a visible manual refresh action and last successful sync time.
- Show cached data during refresh; do not block browsing.
- Show retry affordance for failures.
- Use XML resources and Kotlin. No Java source and no Compose.

## 10. Error model

The repository must distinguish at least:

- Missing local configuration.
- Network unavailable / timeout.
- Permission denied / spreadsheet not public.
- Spreadsheet not found.
- Google API quota / server failure.
- Missing tab/header.
- Invalid row value.
- Duplicate ID.
- Broken foreign-key relationship.
- Invalid MCQ option structure.
- Room persistence failure.

User-facing messages should be understandable and must not expose API keys or raw server payloads. Detailed diagnostic categories may be logged without credentials or content.

## 11. Acceptance criteria

### Specification / workbook

- [ ] One spreadsheet is shared as Anyone with the link — Viewer and contains only public learning content.
- [ ] One public spreadsheet contains all five required tabs with the exact v1 headers.
- [ ] Reserved tabs are documented but are not required for the first sync.
- [ ] Essay attempts and grading are not stored in public Sheets.
- [ ] The API key and spreadsheet ID are configured outside version control.

### Data layer

- [ ] One Sheets API batch request retrieves all five required ranges.
- [ ] Header-based parsing is independent of column order.
- [ ] Required values, enums, numbers, booleans and relationships are validated.
- [ ] Duplicate IDs and invalid MCQ option structures reject the incoming snapshot.
- [ ] Invalid or failed sync leaves the previous Room snapshot unchanged.
- [ ] Successful sync atomically replaces all five content tables.
- [ ] Last successful sync time is updated only after successful persistence.
- [ ] No question-version history is stored.

### Offline and UI

- [ ] Cached content renders before remote refresh completes.
- [ ] Offline mode continues to show the last valid Room snapshot.
- [ ] Empty cache and sync failure are different UI states.
- [ ] Users can navigate subjects, chapters, topics and questions.
- [ ] Users can manually retry refresh.
- [ ] No network or database work runs on the main thread.

### Engineering

- [ ] All application and test source files are Kotlin.
- [ ] UI remains XML.
- [ ] Unit tests cover parser, validator, mapping and repository failure/atomicity behavior.
- [ ] `testDebugUnitTest` and `assembleDebug` pass.
- [ ] No secrets, API keys or local configuration are committed.

## 12. Open setup dependency

The source spreadsheet ID and restricted Google API key were not present in the inspected repository. The code must therefore support local configuration and a safe unconfigured state. Production sync cannot be verified until the owner supplies the actual public spreadsheet ID, aligns its headers to this contract, configures the API key locally, and enables the Sheets API for the associated Google Cloud project.


## 13. Question practice interaction (implementation increment 1.1)

The learning catalogue now opens an individual practice screen when a learner selects a question.

### MCQ behavior

- The question list shows the prompt, type, difficulty, points and option count only. It does not reveal option correctness or the explanation before the learner opens the question.
- The practice screen renders the question options as a single-choice RadioGroup.
- The learner must select one option before checking the answer.
- Answer evaluation is a pure Kotlin domain operation and is covered by JVM unit tests.
- After checking, the screen reports whether the selection is correct. For an incorrect selection it shows the correct option; if an explanation exists, it is shown with the result.
- The answer key is read from the public catalogue. This is a learning/practice interaction, not a secure exam or anti-cheat mechanism.

### Essay behavior

- ESSAY questions open a multiline answer editor.
- The current increment is deliberately draft-only: text is held by the screen and is not submitted, persisted, or sent to a backend.
- The UI explicitly tells the learner that saving/submission is not available yet.
- Essay attempt records, submitted content and grading must be added later through UID-scoped Firestore records; they must not be added to the public workbook.

### Technical boundaries

- Android native Kotlin + XML only.
- The practice Activity observes the existing Room-backed repository Flow; it does not read Google Sheets directly.
- The practice screen is registered as a non-exported Activity and receives only a question ID through its Intent.
- No answer attempt history, score aggregation, exam timer, submission API or Firestore write is included in this increment.


Room schema version 2 adds nullable columns for the new workbook metadata through a registered 1-to-2 migration. The repository maps the added fields to and from Room. Remote image rendering remains a separate UI task; image URLs are retained in the local cache.

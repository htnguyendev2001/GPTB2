# HTAVk — Learning Content Module Specification

- Document ID: HTAVK-SPEC-LEARNING-001
- Version: 1.2
- Updated: 2026-11-28
- Status: Implementation increment — exam/practice catalogue and mapping
- Status: Implementation baseline; question practice UI in progress
- Platform: Android native, Kotlin + XML
- Application ID: `com.htanh.appvk`
- Source of truth: one public, read-only Google Spreadsheet
- Local cache: Room
- Backend policy: no custom backend and no Firebase Cloud Functions

## 1. Purpose and scope

The Learning Content module loads the public learning catalogue from one Google Spreadsheet, validates it, maps it into typed Kotlin domain models, atomically replaces the local Room snapshot, and exposes cached content to the Android UI.

The learning catalogue implementation covers:

1. Subjects
2. Chapters
3. Topics
4. Questions
5. Question options
6. Exam and practice definitions
7. Ordered exam-question mappings

The workbook contract also reserves tabs for app configuration, badges, levels and missions. Exams and exam-question mappings are now consumed by the exam/practice increment described in section 13.

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
| image_url | URL/String | No | Optional question image; preserved as LearningQuestion.imageUrl and rendered on the practice screen for HTTPS URLs |
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
| image_url | URL/String | No | Optional option image; preserved as QuestionOption.imageUrl and rendered alongside the option for HTTPS URLs |
| is_correct | Boolean | Yes | Public answer key |
| sort_order | Integer | No | Display order; defaults to 0 |

### 3.2 Reserved tabs for subsequent modules

These tabs belong to the workbook contract but are not read by this learning-content increment:

- GUIDE: workbook editing instructions and schema version.
- APP_CONFIG: key/value configuration for content owners. It must not contain API keys, credentials or private user data.
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
- Numeric fields should be stored as numeric spreadsheet cells, not localized text. The API requests unformatted values so display-locale decimal separators do not affect numeric parsing.
- Sort order is ascending, then display name as a stable secondary sort.

## 5. Remote transport and security

### Transport

Use Google Sheets API v4 read-only `spreadsheets.values:batchGet` over HTTPS with `valueRenderOption=UNFORMATTED_VALUE` so numeric cells are parsed independently of spreadsheet display locale. Fetch all seven required ranges in one request:

- `SUBJECTS!A:Z`
- `CHAPTERS!A:Z`
- `TOPICS!A:Z`
- `QUESTIONS!A:Z`
- `QUESTION_OPTIONS!A:Z`
- `EXAMS!A:Z`
- `EXAM_QUESTIONS!A:Z`

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
- LearningExam(id, name, description, subjectId, durationMinutes, type, totalScore, isActive, startAt, endAt)
- ExamQuestion(examId, questionId, sortOrder, score)
- LearningCatalog(subjects, chapters, topics, questions, options, exams, examQuestions)

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

A content refresh is all-or-nothing. Never clear Room before remote fetch and validation complete. Use one database transaction to delete and insert all seven catalogue tables. Persist `last_successful_sync_at` only in the same successful transaction. Keep only one current snapshot; do not create content-version tables.

## 8. Room schema

Database: `htavk_learning_content.db`, version 3.

Tables:

- `subjects`: primary key `id`.
- `chapters`: primary key `id`, index on `subjectId`.
- `topics`: primary key `id`, index on `chapterId`.
- `learning_questions`: primary key `id`, index on `topicId`.
- `question_options`: primary key `id`, index on `questionId`.
- `learning_exams`: primary key `id`, foreign key to `subjects.id`.
- `exam_questions`: composite primary key `(examId, questionId)`, foreign keys to `learning_exams.id` and `learning_questions.id`; index on `(examId, sortOrder)`.
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
- [ ] One public spreadsheet contains all seven required tabs with the exact v1 headers.
- [ ] Reserved tabs are documented but are not required for the first sync.
- [ ] Essay attempts and grading are not stored in public Sheets.
- [ ] The API key and spreadsheet ID are configured outside version control.

### Data layer

- [ ] One Sheets API batch request retrieves all five required ranges.
- [ ] Header-based parsing is independent of column order.
- [ ] Required values, enums, numbers, booleans and relationships are validated.
- [ ] Duplicate IDs and invalid MCQ option structures reject the incoming snapshot.
- [ ] Invalid or failed sync leaves the previous Room snapshot unchanged.
- [ ] Successful sync atomically replaces all seven content tables.
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
- The practice screen renders the question options as a single-choice list of RadioButtons; selection is coordinated by the Activity.
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


Room schema version 2 adds nullable columns for the new workbook metadata through a registered 1-to-2 migration. The repository maps the added fields to and from Room. Question and option image URLs are retained in the local cache and rendered by the practice screen when they use HTTPS.


### Learning media rendering

The question practice screen renders `QUESTIONS.image_url` below the question prompt and `QUESTION_OPTIONS.image_url` alongside its option. The Kotlin image loader accepts HTTPS URLs only, uses 10-second connect/read timeouts, rejects non-image responses and downloads over 5 MiB, downsamples to a maximum 1280-pixel dimension, and uses a bounded in-memory LRU cache. Redirects and HTTP URLs are not followed. Images are not persisted to disk, so they are downloaded again after process restart. Device/emulator verification remains required. When a refreshed catalogue changes the current question or its options, the practice screen rebuilds the options and clears the stale selection; identical Room emissions do not discard the current feedback.


## 13. Exam and practice catalogue increment (2026-11-28)

The workbook's `EXAMS` and `EXAM_QUESTIONS` tabs are now part of the seven-tab public learning-content batch read. The app continues to read the spreadsheet only; all learner-specific attempts and answers remain outside the workbook.

### EXAMS

Workbook headers (case-insensitive, header order independent):

| Column | Required | Mapping / validation |
|---|---:|---|
| exam_id | Yes | Stable unique exam identifier |
| name | Yes | Display title |
| description | No | Description shown in the exam/practice list |
| subject_id | Yes | Must reference SUBJECTS.subject_id |
| duration_minutes | Yes | Non-negative integer; EXAM requires a positive duration; PRACTICE may be zero |
| exam_type | Yes | EXAM or PRACTICE |
| total_score | Yes | Finite, non-negative number |
| is_active | Yes | Boolean; inactive definitions are hidden from the list |
| start_at | No | Retained as source text for future scheduling behavior |
| end_at | No | Retained as source text for future scheduling behavior |

### EXAM_QUESTIONS

| Column | Required | Mapping / validation |
|---|---:|---|
| exam_id | Yes | Must reference EXAMS.exam_id |
| question_id | Yes | Must reference QUESTIONS.question_id |
| sort_order | Yes | Non-negative integer; controls question order within the selected exam/practice set |
| score | Yes | Finite, non-negative number; overrides the generic question points for this session |

A question may be reused by multiple exams/practice sets. The pair (exam_id, question_id) must be unique within EXAM_QUESTIONS. Each exam must contain at least one mapped question. The parser validates all references before Room is changed; an invalid exam row or mapping rejects the entire incoming catalogue and preserves the previous cache.

### Local persistence and navigation

- Room schema version 3 adds `learning_exams` and `exam_questions`, with a registered 2→3 migration; existing v1 databases migrate through 1→2→3.
- Both tables are replaced in the same Room transaction as the five existing catalogue tables.
- The Learning screen exposes a combined exam/practice list, labels each item by `exam_type`, and displays question count, total score and duration.
- Selecting an item opens the existing question-session screen with question IDs ordered by `EXAM_QUESTIONS.sort_order`; per-session score comes from `EXAM_QUESTIONS.score`.
- In increment 1.2, `start_at` and `end_at` were retained but not yet used to gate availability. Increment 1.3 below adds availability gating, countdown, submission/result and UID-scoped learner attempt persistence.
- No learner identity, answer, attempt, score result or essay submission is read from or written to Google Sheets.


## 14. Timed sessions, submission and Firebase learner records (implementation increment 1.3)

### Session behavior

- Selecting an EXAM or PRACTICE definition creates a session attempt when Firebase is configured and the user is authenticated.
- EXAM sessions with a positive `duration_minutes` calculate an absolute deadline at session start. If `end_at` is earlier than the duration deadline, `end_at` becomes the hard deadline. The UI displays a countdown derived from that deadline, so pausing/resuming the Activity does not reset the clock. When the deadline is reached, the app submits automatically.
- The deadline is currently based on the device clock. This is not a tamper-resistant timer; a trusted server deadline would require a backend/Cloud Function and is outside the approved architecture.
- `start_at` and `end_at` are ISO-8601 timestamps with an explicit timezone/UTC offset. The opening interval is inclusive at start and exclusive at end. Invalid timestamps or a start timestamp not earlier than end timestamp make the definition unavailable.
- Availability is rechecked when the learner taps the item, not only when the list is rendered. A not-yet-open row remains tappable to show its opening-state message and can start without requiring a catalogue refresh once the time window begins.
- EXAM mode never reveals correctness, answer keys or explanations before submission. PRACTICE mode retains immediate feedback.
- Manual submission and timeout submission both calculate MCQ points from `EXAM_QUESTIONS.score` (falling back to the question's `max_score` only when no session override exists). ESSAY responses are not auto-scored and remain pending teacher review.
- The result screen reports automatic score, correct/incorrect/unanswered counts and the number of essay responses awaiting review. It does not represent pending essay points as a final score.

### Firestore ownership and document layout

Private records live under the authenticated Firebase UID in the top-level `HTVAData` collection:

```text
HTVAData/{uid}/
  LEARNING_ATTEMPTS/{attemptId}
    ANSWERS/{questionId}
  ESSAY_ATTEMPTS/{attemptId}
  ESSAY_SUBMISSIONS/{attemptId_questionId}
```

- `LEARNING_ATTEMPTS`: one parent record for every EXAM/PRACTICE session; includes status, timing, question IDs, per-question scores and automatic result summary.
- `ANSWERS`: one document per question with selected option or essay draft.
- `ESSAY_ATTEMPTS`: one parent record only when the session contains ESSAY questions.
- `ESSAY_SUBMISSIONS`: one document per answered ESSAY question. Draft status is `DRAFT`; final submission status is `PENDING_REVIEW`. Teacher grading fields include `awarded_score`, `teacher_feedback`, `graded_at` and `status`.
- Firestore rules must enforce `request.auth.uid == uid` for every document under `HTVAData/{uid}`. The repository includes `firestore.rules` with this ownership check.
- The app never writes learner answers, attempts or results to the public Google Spreadsheet.

### Firebase setup and known dependency

Firebase Auth and Firestore Android SDKs are included. The Google Services plugin is applied only when the untracked `app/google-services.json` exists. See `docs/setup/FIREBASE_LEARNING_DATA_SETUP.md` and deploy `firestore.rules` to the Firebase project.

The current source branch does not yet provide a login/registration UI or an approved sign-in provider flow. The Firestore repository requires `FirebaseAuth.currentUser`; it does not silently create anonymous accounts. Until the authentication flow signs a user in, the UI can complete a session locally but cannot persist it to Firestore. This is an explicit integration dependency, not a completed end-to-end authentication feature.

### Scope not included

- Resuming an in-progress attempt after process death or reinstall.
- A trusted server-side timer or anti-cheat enforcement.
- Teacher-facing grading UI; teachers review/update essay documents through Firebase Console.
- Refreshing the learner's result automatically after a teacher changes essay grading fields.
- Reward points, XP, badges or learning streak progression.

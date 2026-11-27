# Google Sheets Workbook Compatibility Audit

- Project: HTAVk
- Source workbook: `GPTB2_Google_Sheets_Template_v1.0.xlsx`
- Audited on: 2026-11-27
- Workbook version: v1.0
- Status: **PARTIALLY RESOLVED — public read-only access approved; workbook schema alignment remains blocked**
- Scope: structural inspection of the uploaded XLSX; this does not verify the live Google Spreadsheet or its current permissions.

## 1. Executive summary

The uploaded workbook contains 14 visible worksheets. The current learning parser requires five tabs, but several required column names and fields differ. The current implementation therefore cannot parse the workbook as-is.

### Access-policy decision — APPROVED by Project Owner on 2026-11-27

The Project Owner explicitly approved **Anyone with the link — Viewer** so the Android app can read the learning spreadsheet directly using the Google Sheets API. The app must remain read-only; it must never write to the spreadsheet.

This is an explicit HTAVk product decision that overrides the uploaded workbook GUIDE's older instruction not to share the spreadsheet publicly. The GUIDE should be updated in the source workbook by its owner so future content editors do not follow the superseded private-sheet instruction.

Security constraints:
- Only public learning catalogue/configuration intended for every app user may be placed in this spreadsheet.
- Never put learner identity, essay answers, submissions, grades, tokens, API keys, private contact details or other private data in it.
- Restrict the Google API key to the Android package/signing certificate(s) and Google Sheets API where supported; do not commit the key.
- Public sharing is read-only. Content changes remain the responsibility of authorized spreadsheet editors.
- The approval defines the intended access model; it does not prove that the live spreadsheet sharing setting or API key currently works.

The workbook also defines `ESSAY_ATTEMPTS` and `ESSAY_SUBMISSIONS` as grading workflow tabs. HTAVk's product decision is that learner attempts, submitted essay text, attachments and grading are private UID-scoped Firebase records, not public/shared content data. These tabs must not be read or written by the Android public-content pipeline.

## 2. Workbook inventory

All 14 worksheets are visible. No hidden worksheets, defined names or cell formulas were found. The sheets have header-row autofilters where applicable. Data-validation rules are present on the configuration and structured data tabs.

| Worksheet | Columns | Non-header rows | Role |
|---|---:|---:|---|
| GUIDE | 2 | 18 | Usage, access and schema guidance |
| APP_CONFIG | 5 | 9 | App/content configuration |
| SUBJECTS | 7 | 3 | Subject catalogue |
| CHAPTERS | 6 | 3 | Chapter catalogue |
| TOPICS | 6 | 3 | Topic catalogue |
| QUESTIONS | 13 | 2 | MCQ/essay question catalogue |
| QUESTION_OPTIONS | 7 | 4 | MCQ options |
| EXAMS | 10 | 2 | Exam/practice definitions |
| EXAM_QUESTIONS | 4 | 3 | Exam-question mapping |
| ESSAY_ATTEMPTS | 10 | 1 | Attempt-level grading workflow; currently empty |
| ESSAY_SUBMISSIONS | 12 | 1 | Per-question essay submissions; currently empty |
| BADGES | 9 | 4 | Badge definitions |
| LEVELS | 8 | 3 | Level thresholds |
| MISSIONS | 11 | 3 | Mission definitions |

GUIDE explicitly says the populated sample rows in the content/configuration tabs are illustrative and must be replaced or removed before production use. The current sample rows are not production learning content.

## 3. Required parser contract mismatches

### SUBJECTS

Workbook headers:
`subject_id, name, description, icon, image_url, sort_order, is_active`

Current parser additionally looks for optional `icon_url` and `cover_image_url`. These names do not exist in the workbook. Candidate mapping is `icon -> iconUrl` and `image_url -> coverImageUrl`, subject to product confirmation of the image semantics.

### CHAPTERS

Workbook headers:
`chapter_id, subject_id, name, description, sort_order, is_active`

The required identity/relationship/name fields align. Optional `updated_at` is absent from the workbook and is not required by the current model.

### TOPICS

Workbook headers:
`topic_id, chapter_id, name, description, sort_order, is_active`

The required identity/relationship/name fields align. Optional `updated_at` is absent.

### QUESTIONS

Workbook headers:
`question_id, subject_id, chapter_id, topic_id, question_type, difficulty, content, image_url, explanation, max_score, estimated_time_seconds, is_active, sort_order`

Differences:
- Workbook carries `subject_id` and `chapter_id` in addition to `topic_id`; current model/parser only consume `topic_id`.
- Workbook uses `max_score`; parser expects `points`.
- Workbook includes `image_url` and `estimated_time_seconds`; current model does not represent these fields.
- Workbook includes `is_active`; parser handles this optional field.
- The sample question rows use MCQ and ESSAY and the EASY/MEDIUM/HARD difficulty vocabulary expected by the parser.

### QUESTION_OPTIONS

Workbook headers:
`option_id, question_id, option_label, content, image_url, is_correct, sort_order`

Differences:
- Workbook uses `content`; parser requires `option_text`.
- Workbook includes `option_label` and `image_url`; current domain model does not represent either field.
- `is_correct` and `sort_order` align.

## 4. Additional workbook tabs not in the first parser slice

The workbook contains APP_CONFIG, EXAMS, EXAM_QUESTIONS, BADGES, LEVELS and MISSIONS. The current first-slice parser intentionally reads only SUBJECTS, CHAPTERS, TOPICS, QUESTIONS and QUESTION_OPTIONS. These additional public-definition tabs are not yet implemented.

The workbook's APP_CONFIG includes values such as `content_revision`, minimum app version, study goal, streak-protection cost, timezone and idle timeout. These values must not silently be treated as active app configuration until their validation, ownership and consumption rules are specified.

EXAMS and EXAM_QUESTIONS are exam definitions and mappings; they are not implemented by the current learning pipeline.

BADGES, LEVELS and MISSIONS are public definitions; their runtime reward/XP calculations remain outside this first slice.

## 5. Essay data conflict

The workbook GUIDE describes `ESSAY_ATTEMPTS` and `ESSAY_SUBMISSIONS` as backend-created records, with Firebase Storage attachment URLs and grading updates.

HTAVk's approved product architecture instead requires user-specific essay attempts, submitted answer text, attachments and grading to be stored in Firebase with UID ownership isolation. These records must not be placed in a shared content workbook or exposed through the public catalogue API.

Decision for HTAVk implementation:
- Do not include ESSAY_ATTEMPTS or ESSAY_SUBMISSIONS in the Android content batch request.
- Do not write learner data to Google Sheets.
- Implement future essay records under authenticated UID-scoped Firestore paths, with attachments in Firebase Storage and ownership enforced by Firestore/Storage security rules.
- Treat the corresponding workbook tabs as incompatible legacy/template guidance for HTAVk unless the product owner explicitly changes the approved data-ownership decision.

## 6. Access and security mismatch

The uploaded GUIDE says not to share the spreadsheet publicly and describes backend service-account access. This instruction conflicts with the Project Owner's explicit decision on 2026-11-27 to allow public read-only access for direct Android client reads.

For HTAVk, the approved rule is:
1. Share the spreadsheet as **Anyone with the link — Viewer**.
2. The Android app uses Sheets API v4 with a restricted API key for read-only requests.
3. The spreadsheet must contain only content intended to be public to all app users.
4. Do not embed service-account private keys or put private learner data in the workbook.
5. Keep the spreadsheet ID and API key out of source control and localize configuration as documented.
6. Do not claim live sync works until the live sharing setting, API key restrictions and actual response are tested.

Google Sheets is not a transactional database and must not own balances, streak validation, essay submissions or user ownership.

## 7. Validation rules observed

The workbook uses dropdown/numeric data validations, including:
- APP_CONFIG.value_type: STRING, INTEGER, DECIMAL, BOOLEAN, URL.
- Boolean fields: TRUE/FALSE.
- QUESTIONS.question_type: MCQ/ESSAY.
- QUESTIONS.difficulty: EASY/MEDIUM/HARD.
- EXAMS.exam_type: EXAM/PRACTICE.
- ESSAY_ATTEMPTS.grading_mode: PER_QUESTION/TOTAL_ATTEMPT.
- ESSAY_ATTEMPTS.grading_status: PENDING/PARTIALLY_GRADED/GRADED/SYNCED.
- ESSAY_SUBMISSIONS.grading_status: PENDING/GRADED/SYNCED.
- MISSIONS.repeat_policy: ONCE/DAILY/UNLIMITED.
- Numeric/order/score/threshold fields have non-negative numeric validation.

No cell formulas or defined names were found. Data validation is spreadsheet input assistance, not a substitute for runtime validation in the app.

## 8. Recommended next implementation sequence

1. Update the workbook GUIDE to reflect the approved public read-only sharing policy.
2. Approve the field mapping for SUBJECTS, QUESTIONS and QUESTION_OPTIONS, including question/option images, max score and estimated time.
3. Update domain models, parser and parser tests to the approved workbook headers; keep strict validation and all-or-nothing Room replacement.
4. Add APP_CONFIG/EXAMS/EXAM_QUESTIONS/BADGES/LEVELS/MISSIONS only as separately scoped features with explicit acceptance criteria.
5. Keep essay attempt/submission/grading data in UID-scoped Firebase only.
6. Verify the live spreadsheet schema and permissions with a non-production test before enabling production sync.


## 9. Parser compatibility progress (2026-11-27)

The parser now accepts the original workbook v1.0 headers as well as the prior internal aliases for these fields:
- SUBJECTS.image_url maps to the existing coverImageUrl field (cover_image_url remains accepted as a fallback alias).
- QUESTIONS.max_score maps to the existing points field (points remains accepted as an alias).
- QUESTION_OPTIONS.content maps to the existing option text field (option_text remains accepted as an alias).
- When QUESTIONS.subject_id and chapter_id are present, the parser checks them against the canonical topic -> chapter -> subject hierarchy.

Compatibility remains partial. The current domain/Room schema does not preserve SUBJECTS.icon (a Material icon identifier), QUESTIONS.image_url, QUESTIONS.estimated_time_seconds, QUESTION_OPTIONS.option_label or QUESTION_OPTIONS.image_url. These values are currently not consumed by the learning UI or retained in the local cache. A follow-up model/Room schema increment and migration strategy are required before claiming full workbook compatibility. Do not map the Material icon identifier into a URL-named field.

## 10. Verification boundary

This audit inspected the uploaded XLSX at the workbook-structure level. It did not inspect the live Google Spreadsheet contents, actual sharing setting, API key restrictions, Firebase project configuration or production data. The public read-only access model is approved, but its live configuration and end-to-end API access remain unverified.


## 11. Model and Room field preservation progress (2026-11-27)

The parser/domain/Room pipeline now preserves the remaining first-slice workbook metadata:
- SUBJECTS.icon -> Subject.iconName (Material icon identifier; the legacy iconUrl field remains for compatibility and is not populated from this column).
- QUESTIONS.image_url -> LearningQuestion.imageUrl.
- QUESTIONS.estimated_time_seconds -> nullable non-negative LearningQuestion.estimatedTimeSeconds.
- QUESTION_OPTIONS.option_label -> QuestionOption.label.
- QUESTION_OPTIONS.image_url -> QuestionOption.imageUrl.

Room schema is now version 2. The application registers migration 1 -> 2, adding nullable columns to the existing tables so previously cached catalogue rows remain intact. Repository mapping now carries these fields in both directions. The practice screen displays option labels and estimated duration; remote image rendering is not yet implemented, but image URLs are retained in the cache rather than discarded.

Automated verification is pending for the new commits. Do not treat this section as a successful CI or live Google Sheets sync report until the corresponding workflow completes and the spreadsheet's Viewer permission/API access are verified.

# Firebase setup — HTVA Vk learning attempts

## 1. Firebase Android configuration

1. Register the Android app with package name `com.htanh.appvk`.
2. Download `google-services.json` and place it at `app/google-services.json`. This file is ignored by Git and must not be committed.
3. Enable the Firebase Authentication provider used by the product and ensure the app has a signed-in Firebase user before starting a learning session. The current branch deliberately does not create anonymous accounts or invent a sign-in method.
4. Enable Cloud Firestore in the Firebase project.
5. Deploy the repository's `firestore.rules` to the target Firebase project.

The Firebase Android BoM is pinned to `33.3.0` for this increment because the app's approved minimum SDK is API 21; newer Firebase Authentication releases raise the minimum Android API. The Google Services Gradle plugin is applied only when `app/google-services.json` exists. Without that file, the app still builds and the learning catalogue remains usable, but user-specific history is not initialized.

## 2. Firestore data layout

All private learning records are nested under the authenticated UID:

```text
HTVAData/{uid}/
  LEARNING_ATTEMPTS/{attemptId}
    ANSWERS/{questionId}
  ESSAY_ATTEMPTS/{attemptId}
  ESSAY_SUBMISSIONS/{attemptId_questionId}
```

- `LEARNING_ATTEMPTS` stores one record for every EXAM or PRACTICE session, including status, start/deadline/submission timestamps, question IDs, per-question maximum scores, automatic score and summary counts.
- `ANSWERS` stores the selected option or essay draft for each question in that session.
- `ESSAY_ATTEMPTS` stores the essay-review parent record only when the session contains at least one ESSAY question.
- `ESSAY_SUBMISSIONS` stores one essay response per attempt/question. Drafts use `DRAFT`; submitted responses use `PENDING_REVIEW`. Teachers can inspect and update grading fields through Firebase Console.

The app never writes learner data to the public Google Spreadsheet.

## 3. Ownership rules

The provided `firestore.rules` allows access only when `request.auth.uid` matches the UID segment in `HTVAData/{uid}`. Do not weaken this to client-supplied UID checks or public access. Firebase Console administrators can review essay submissions without an additional teacher-facing app.

## 4. Authentication integration dependency

The repository uses `FirebaseAuth.currentUser.uid` as the ownership boundary. The current HTVA Vk source branch does not yet contain a login/registration UI. Until the app's approved authentication flow signs a user in, creating a Firestore attempt returns an authentication-required message and the session can only be completed locally. Do not silently create anonymous accounts; that would change account identity and recovery behavior.

## 5. Attempt lifecycle

- `IN_PROGRESS`: attempt created when the session starts.
- `SUBMITTED`: learner submitted manually or the timer expired.
- Essay submissions move from `DRAFT` to `PENDING_REVIEW` at final submission.
- `grading_status` is `PENDING_REVIEW` when essay responses await a teacher, `GRADED` when no essay grading is required, or `NO_SUBMISSIONS` when the attempt contains essay questions but none were answered.

MCQ results are calculated on Android from the public answer key. This product does not claim anti-cheat protection. Timed exams use a client-calculated deadline; a trusted server deadline would require a backend/Cloud Function, which is outside the approved architecture.

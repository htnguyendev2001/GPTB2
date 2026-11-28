package com.htanh.appvk.data.repository

import android.content.Context
import android.content.Context.MODE_PRIVATE
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.htanh.appvk.domain.learning.ExamSessionResult
import com.htanh.appvk.domain.learning.LearningExam
import com.htanh.appvk.domain.learning.SessionAnswer
import java.util.Date
import java.util.UUID

data class StartedExamAttempt(
    val id: String,
    val deadlineAtEpochMillis: Long?,
    val persistenceTask: Task<Void>?
)

class ExamAttemptRepository(
    context: Context,
    private val firestore: FirebaseFirestore?,
    private val historyStore: ExamHistoryStore
) {
    private val preferences = context.applicationContext.getSharedPreferences(IDENTITY_PREFERENCES, MODE_PRIVATE)

    val deviceId: String
        get() {
            val existing = preferences.getString(KEY_DEVICE_ID, null)
            if (!existing.isNullOrBlank()) return existing
            val created = UUID.randomUUID().toString()
            preferences.edit().putString(KEY_DEVICE_ID, created).apply()
            return created
        }

    private fun deviceRoot() = requireNotNull(firestore).collection(ROOT_COLLECTION).document(deviceId)
    private fun attempts() = deviceRoot().collection(ATTEMPTS)
    private fun essayAttempts() = deviceRoot().collection(ESSAY_ATTEMPTS)
    private fun essaySubmissions() = deviceRoot().collection(ESSAY_SUBMISSIONS)

    fun createAttempt(
        exam: LearningExam,
        questionIds: List<String>,
        questionScores: Map<String, Double>,
        questionTypes: Map<String, String>,
        sessionEndAtEpochMillis: Long? = null,
        startedAtEpochMillis: Long = System.currentTimeMillis()
    ): StartedExamAttempt {
        val attemptId = UUID.randomUUID().toString()
        val durationDeadline = if (exam.type.name == "EXAM" && exam.durationMinutes > 0) {
            startedAtEpochMillis + exam.durationMinutes * 60_000L
        } else null
        val deadline = listOfNotNull(durationDeadline, sessionEndAtEpochMillis).minOrNull()
        val hasEssay = questionTypes.values.any { it == "ESSAY" }

        historyStore.upsert(
            ExamHistoryItem(
                attemptId = attemptId,
                examId = exam.id,
                examName = exam.name,
                sessionType = exam.type.name,
                startedAtMillis = startedAtEpochMillis
            )
        )

        val database = firestore ?: return StartedExamAttempt(attemptId, deadline, null)
        val data = hashMapOf<String, Any?>(
            "attempt_id" to attemptId,
            "device_id" to deviceId,
            "exam_id" to exam.id,
            "exam_name" to exam.name,
            "exam_type" to exam.type.name,
            "status" to "IN_PROGRESS",
            "started_at" to FieldValue.serverTimestamp(),
            "started_at_client_ms" to startedAtEpochMillis,
            "deadline_at" to deadline?.let(::Date),
            "question_ids" to questionIds,
            "question_scores" to questionScores,
            "maximum_score" to exam.totalScore,
            "grading_status" to if (hasEssay) "PENDING_REVIEW" else "NOT_REQUIRED"
        )
        val root = database.collection(ROOT_COLLECTION).document(deviceId)
        val batch = database.batch()
        batch.set(root.collection(ATTEMPTS).document(attemptId), data)
        if (hasEssay) {
            batch.set(root.collection(ESSAY_ATTEMPTS).document(attemptId), hashMapOf(
                "attempt_id" to attemptId,
                "device_id" to deviceId,
                "exam_id" to exam.id,
                "exam_name" to exam.name,
                "status" to "IN_PROGRESS",
                "started_at" to FieldValue.serverTimestamp(),
                "submitted_at" to null,
                "grading_status" to "NOT_SUBMITTED"
            ))
        }
        return StartedExamAttempt(attemptId, deadline, batch.commit())
    }

    fun saveAnswer(
        attemptId: String,
        examId: String,
        answer: SessionAnswer,
        questionType: String,
        maximumScore: Double
    ): Task<Void>? {
        val database = firestore ?: return null
        val answerData = hashMapOf<String, Any?>(
            "question_id" to answer.questionId,
            "question_type" to questionType,
            "selected_option_id" to answer.selectedOptionId,
            "essay_text" to answer.essayText,
            "maximum_score" to maximumScore,
            "updated_at" to FieldValue.serverTimestamp()
        )
        val root = database.collection(ROOT_COLLECTION).document(deviceId)
        val attempt = root.collection(ATTEMPTS).document(attemptId)
        val batch = database.batch()
        batch.set(attempt.collection(ANSWERS).document(answer.questionId), answerData)
        if (questionType == "ESSAY") {
            batch.set(
                root.collection(ESSAY_SUBMISSIONS).document("${attemptId}_${answer.questionId}"),
                hashMapOf(
                    "submission_id" to "${attemptId}_${answer.questionId}",
                    "attempt_id" to attemptId,
                    "device_id" to deviceId,
                    "exam_id" to examId,
                    "question_id" to answer.questionId,
                    "answer_text" to answer.essayText.orEmpty(),
                    "maximum_score" to maximumScore,
                    "awarded_score" to null,
                    "status" to "DRAFT",
                    "updated_at" to FieldValue.serverTimestamp(),
                    "graded_at" to null,
                    "teacher_feedback" to null
                )
            )
        }
        return batch.commit()
    }

    fun submitAttempt(
        attemptId: String,
        examId: String,
        answers: List<SessionAnswer>,
        questionTypes: Map<String, String>,
        questionScores: Map<String, Double>,
        result: ExamSessionResult,
        autoSubmitted: Boolean
    ): Task<Void>? {
        val submittedAt = System.currentTimeMillis()
        historyStore.upsert(
            ExamHistoryItem(
                attemptId = attemptId,
                examId = examId,
                examName = historyStore.all().firstOrNull { it.attemptId == attemptId }?.examName.orEmpty(),
                sessionType = historyStore.all().firstOrNull { it.attemptId == attemptId }?.sessionType ?: "EXAM",
                startedAtMillis = historyStore.all().firstOrNull { it.attemptId == attemptId }?.startedAtMillis ?: submittedAt,
                submittedAtMillis = submittedAt,
                status = "SUBMITTED",
                syncStatus = if (firestore == null) "LOCAL_ONLY" else "PENDING",
                maximumScore = result.maximumScore,
                earnedScore = result.earnedAutomaticScore,
                correctCount = result.correctCount,
                incorrectCount = result.incorrectCount,
                unansweredCount = result.unansweredCount,
                pendingEssayCount = result.pendingEssayCount,
                autoSubmitted = autoSubmitted
            )
        )

        val database = firestore ?: return null
        val root = database.collection(ROOT_COLLECTION).document(deviceId)
        val batch = database.batch()
        val attempt = root.collection(ATTEMPTS).document(attemptId)
        answers.forEach { answer ->
            val type = questionTypes[answer.questionId] ?: "MCQ"
            val maxScore = questionScores[answer.questionId] ?: 0.0
            batch.set(
                attempt.collection(ANSWERS).document(answer.questionId),
                hashMapOf(
                    "question_id" to answer.questionId,
                    "question_type" to type,
                    "selected_option_id" to answer.selectedOptionId,
                    "essay_text" to answer.essayText,
                    "maximum_score" to maxScore,
                    "updated_at" to FieldValue.serverTimestamp()
                )
            )
            if (type == "ESSAY") {
                val hasAnswer = !answer.essayText.isNullOrBlank()
                batch.set(
                    root.collection(ESSAY_SUBMISSIONS).document("${attemptId}_${answer.questionId}"),
                    hashMapOf(
                        "submission_id" to "${attemptId}_${answer.questionId}",
                        "attempt_id" to attemptId,
                        "device_id" to deviceId,
                        "exam_id" to examId,
                        "question_id" to answer.questionId,
                        "answer_text" to answer.essayText.orEmpty(),
                        "maximum_score" to maxScore,
                        "awarded_score" to null,
                        "status" to if (hasAnswer) "PENDING_REVIEW" else "NOT_SUBMITTED",
                        "submitted_at" to if (hasAnswer) FieldValue.serverTimestamp() else null,
                        "graded_at" to null,
                        "teacher_feedback" to null,
                        "updated_at" to FieldValue.serverTimestamp()
                    )
                )
            }
        }
        batch.update(
            attempt,
            mapOf(
                "status" to "SUBMITTED",
                "submitted_at" to FieldValue.serverTimestamp(),
                "auto_submitted" to autoSubmitted,
                "earned_automatic_score" to result.earnedAutomaticScore,
                "maximum_score" to result.maximumScore,
                "correct_count" to result.correctCount,
                "incorrect_count" to result.incorrectCount,
                "unanswered_count" to result.unansweredCount,
                "pending_essay_count" to result.pendingEssayCount,
                "grading_status" to when {
                    result.pendingEssayCount > 0 -> "PENDING_REVIEW"
                    questionTypes.values.any { it == "ESSAY" } -> "NO_SUBMISSIONS"
                    else -> "GRADED"
                },
                "updated_at" to FieldValue.serverTimestamp()
            )
        )
        if (questionTypes.values.any { it == "ESSAY" }) {
            batch.update(
                root.collection(ESSAY_ATTEMPTS).document(attemptId),
                mapOf(
                    "status" to "SUBMITTED",
                    "submitted_at" to FieldValue.serverTimestamp(),
                    "grading_status" to if (result.pendingEssayCount > 0) "PENDING_REVIEW" else "NO_SUBMISSIONS",
                    "pending_submission_count" to result.pendingEssayCount
                )
            )
        }
        val task = batch.commit()
        task.addOnSuccessListener {
            val saved = historyStore.all().firstOrNull { it.attemptId == attemptId } ?: return@addOnSuccessListener
            historyStore.upsert(saved.copy(syncStatus = "SYNCED"))
        }
        task.addOnFailureListener {
            // Keep the local submission history and pending status for a later retry.
        }
        return task
    }

    companion object {
        const val ROOT_COLLECTION = "HTVAData"
        const val ATTEMPTS = "LEARNING_ATTEMPTS"
        const val ESSAY_ATTEMPTS = "ESSAY_ATTEMPTS"
        const val ESSAY_SUBMISSIONS = "ESSAY_SUBMISSIONS"
        const val ANSWERS = "ANSWERS"
        private const val IDENTITY_PREFERENCES = "htavk_device_identity"
        private const val KEY_DEVICE_ID = "device_id"
    }
}

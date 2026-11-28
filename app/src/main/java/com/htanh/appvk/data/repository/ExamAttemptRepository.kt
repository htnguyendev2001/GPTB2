package com.htanh.appvk.data.repository

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.htanh.appvk.domain.learning.ExamSessionResult
import com.htanh.appvk.domain.learning.LearningExam
import com.htanh.appvk.domain.learning.SessionAnswer
import java.util.Date
import java.util.UUID

data class StartedExamAttempt(
    val id: String,
    val deadlineAtEpochMillis: Long?
)

class ExamAttemptRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    private fun userRoot() = firestore.collection(ROOT_COLLECTION)
        .document(auth.currentUser?.uid ?: throw IllegalStateException(AUTH_REQUIRED))

    fun createAttempt(
        exam: LearningExam,
        questionIds: List<String>,
        questionScores: Map<String, Double>,
        startedAtEpochMillis: Long = System.currentTimeMillis()
    ): StartedExamAttempt {
        val uid = auth.currentUser?.uid ?: throw IllegalStateException(AUTH_REQUIRED)
        val attemptId = UUID.randomUUID().toString()
        val deadline = if (exam.type.name == "EXAM" && exam.durationMinutes > 0) {
            startedAtEpochMillis + exam.durationMinutes * 60_000L
        } else null
        val data = hashMapOf<String, Any?>(
            "attempt_id" to attemptId,
            "uid" to uid,
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
            "grading_status" to "NOT_REQUIRED"
        )
        userRoot().collection(ATTEMPTS).document(attemptId).set(data)
        return StartedExamAttempt(attemptId, deadline)
    }

    fun saveAnswer(
        attemptId: String,
        examId: String,
        answer: SessionAnswer,
        questionType: String,
        maximumScore: Double
    ): Task<Void> {
        val uid = auth.currentUser?.uid ?: throw IllegalStateException(AUTH_REQUIRED)
        val answerData = hashMapOf<String, Any?>(
            "question_id" to answer.questionId,
            "question_type" to questionType,
            "selected_option_id" to answer.selectedOptionId,
            "essay_text" to answer.essayText,
            "maximum_score" to maximumScore,
            "updated_at" to FieldValue.serverTimestamp()
        )
        val attempt = userRoot().collection(ATTEMPTS).document(attemptId)
        val batch = firestore.batch()
        batch.set(attempt.collection(ANSWERS).document(answer.questionId), answerData)
        if (questionType == "ESSAY") {
            batch.set(
                userRoot().collection(ESSAY_SUBMISSIONS).document("${attemptId}_${answer.questionId}"),
                hashMapOf(
                    "submission_id" to "${attemptId}_${answer.questionId}",
                    "attempt_id" to attemptId,
                    "uid" to uid,
                    "exam_id" to examId,
                    "question_id" to answer.questionId,
                    "answer_text" to answer.essayText.orEmpty(),
                    "maximum_score" to maximumScore,
                    "awarded_score" to null,
                    "status" to "DRAFT",
                    "created_at" to FieldValue.serverTimestamp(),
                    "graded_at" to null,
                    "teacher_feedback" to null
                )
            )
        }
        return batch.commit()
    }

    fun submitAttempt(
        attemptId: String,
        answers: List<SessionAnswer>,
        questionTypes: Map<String, String>,
        questionScores: Map<String, Double>,
        result: ExamSessionResult,
        autoSubmitted: Boolean
    ): Task<Void> {
        val uid = auth.currentUser?.uid ?: throw IllegalStateException(AUTH_REQUIRED)
        val batch = firestore.batch()
        val attempt = userRoot().collection(ATTEMPTS).document(attemptId)
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
            if (type == "ESSAY" && !answer.essayText.isNullOrBlank()) {
                batch.set(
                    userRoot().collection(ESSAY_SUBMISSIONS).document("${attemptId}_${answer.questionId}"),
                    hashMapOf(
                        "submission_id" to "${attemptId}_${answer.questionId}",
                        "attempt_id" to attemptId,
                        "uid" to uid,
                        "question_id" to answer.questionId,
                        "answer_text" to answer.essayText,
                        "maximum_score" to maxScore,
                        "awarded_score" to null,
                        "status" to "PENDING_REVIEW",
                        "submitted_at" to FieldValue.serverTimestamp(),
                        "graded_at" to null,
                        "teacher_feedback" to null
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
                "grading_status" to if (result.gradingPending) "PENDING_REVIEW" else "GRADED",
                "updated_at" to FieldValue.serverTimestamp()
            )
        )
        return batch.commit()
    }

    companion object {
        const val ROOT_COLLECTION = "HTVAData"
        const val ATTEMPTS = "ESSAY_ATTEMPTS"
        const val ESSAY_SUBMISSIONS = "ESSAY_SUBMISSIONS"
        const val ANSWERS = "ANSWERS"
        const val AUTH_REQUIRED = "Vui lòng đăng nhập để lưu lịch sử làm bài."
    }
}

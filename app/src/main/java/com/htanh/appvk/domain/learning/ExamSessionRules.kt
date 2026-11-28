package com.htanh.appvk.domain.learning

import java.time.Instant
import java.time.OffsetDateTime

enum class ExamAvailability { AVAILABLE, NOT_STARTED, ENDED, INVALID_SCHEDULE }

data class SessionAnswer(
    val questionId: String,
    val selectedOptionId: String? = null,
    val essayText: String? = null
)

data class ExamSessionResult(
    val maximumScore: Double,
    val earnedAutomaticScore: Double,
    val correctCount: Int,
    val incorrectCount: Int,
    val unansweredCount: Int,
    val pendingEssayCount: Int
) {
    val gradingPending: Boolean get() = pendingEssayCount > 0
}

object ExamSessionRules {
    fun availability(exam: LearningExam, now: Instant): ExamAvailability {
        val start = parseInstant(exam.startAt) ?: if (exam.startAt.isNullOrBlank()) null else return ExamAvailability.INVALID_SCHEDULE
        val end = parseInstant(exam.endAt) ?: if (exam.endAt.isNullOrBlank()) null else return ExamAvailability.INVALID_SCHEDULE
        if (start != null && end != null && !start.isBefore(end)) return ExamAvailability.INVALID_SCHEDULE
        if (start != null && now.isBefore(start)) return ExamAvailability.NOT_STARTED
        if (end != null && !now.isBefore(end)) return ExamAvailability.ENDED
        return ExamAvailability.AVAILABLE
    }

    fun parseInstant(value: String?): Instant? {
        if (value.isNullOrBlank()) return null
        return runCatching { Instant.parse(value.trim()) }.getOrElse {
            runCatching { OffsetDateTime.parse(value.trim()).toInstant() }.getOrNull()
        }
    }

    fun score(
        questions: List<LearningQuestion>,
        options: List<QuestionOption>,
        answers: Map<String, SessionAnswer>,
        scores: Map<String, Double>
    ): ExamSessionResult {
        var earned = 0.0
        var correct = 0
        var incorrect = 0
        var unanswered = 0
        var pendingEssay = 0
        var maximum = 0.0

        questions.forEach { question ->
            val maxScore = (scores[question.id] ?: question.points).coerceAtLeast(0.0)
            maximum += maxScore
            val answer = answers[question.id]
            if (question.type == QuestionType.ESSAY) {
                if (answer?.essayText.isNullOrBlank()) unanswered++ else pendingEssay++
                return@forEach
            }
            val selected = answer?.selectedOptionId
            if (selected.isNullOrBlank()) {
                unanswered++
                return@forEach
            }
            val correctOption = options.firstOrNull { it.questionId == question.id && it.id == selected }?.isCorrect == true
            if (correctOption) {
                correct++
                earned += maxScore
            } else {
                incorrect++
            }
        }
        return ExamSessionResult(maximum, earned, correct, incorrect, unanswered, pendingEssay)
    }
}

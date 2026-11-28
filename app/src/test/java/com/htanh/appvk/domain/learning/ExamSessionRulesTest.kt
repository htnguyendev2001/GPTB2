package com.htanh.appvk.domain.learning

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ExamSessionRulesTest {
    private val now = Instant.parse("2026-11-28T10:00:00Z")

    @Test
    fun examBeforeStartIsNotAvailable() {
        val exam = exam(startAt = "2026-11-28T10:01:00Z")
        assertEquals(ExamAvailability.NOT_STARTED, ExamSessionRules.availability(exam, now))
    }

    @Test
    fun examAtEndBoundaryIsClosed() {
        val exam = exam(endAt = "2026-11-28T10:00:00Z")
        assertEquals(ExamAvailability.ENDED, ExamSessionRules.availability(exam, now))
    }

    @Test
    fun examInsideWindowIsAvailable() {
        val exam = exam(
            startAt = "2026-11-28T09:00:00Z",
            endAt = "2026-11-28T11:00:00Z"
        )
        assertEquals(ExamAvailability.AVAILABLE, ExamSessionRules.availability(exam, now))
    }

    @Test
    fun malformedOrReversedScheduleIsInvalid() {
        assertEquals(ExamAvailability.INVALID_SCHEDULE, ExamSessionRules.availability(exam(startAt = "tomorrow"), now))
        assertEquals(
            ExamAvailability.INVALID_SCHEDULE,
            ExamSessionRules.availability(exam(startAt = "2026-11-28T11:00:00Z", endAt = "2026-11-28T10:00:00Z"), now)
        )
    }

    @Test
    fun scoreSeparatesAutomaticAndPendingEssayMarks() {
        val questions = listOf(
            question("Q1", QuestionType.MCQ, 2.0),
            question("Q2", QuestionType.MCQ, 3.0),
            question("Q3", QuestionType.ESSAY, 5.0)
        )
        val options = listOf(
            QuestionOption("A1", "Q1", "Correct", true, 0),
            QuestionOption("A2", "Q1", "Wrong", false, 1),
            QuestionOption("B1", "Q2", "Correct", true, 0),
            QuestionOption("B2", "Q2", "Wrong", false, 1)
        )
        val answers = mapOf(
            "Q1" to SessionAnswer("Q1", selectedOptionId = "A1"),
            "Q2" to SessionAnswer("Q2", selectedOptionId = "B2"),
            "Q3" to SessionAnswer("Q3", essayText = "My essay")
        )

        val result = ExamSessionRules.score(questions, options, answers, mapOf("Q1" to 1.5, "Q2" to 2.5, "Q3" to 6.0))

        assertEquals(10.0, result.maximumScore, 0.0)
        assertEquals(1.5, result.earnedAutomaticScore, 0.0)
        assertEquals(1, result.correctCount)
        assertEquals(1, result.incorrectCount)
        assertEquals(0, result.unansweredCount)
        assertEquals(1, result.pendingEssayCount)
        assertTrue(result.gradingPending)
    }

    private fun exam(startAt: String? = null, endAt: String? = null) = LearningExam(
        id = "EXAM_1", name = "Exam", description = "", subjectId = "SUBJECT_1",
        durationMinutes = 30, type = ExamType.EXAM, totalScore = 10.0,
        isActive = true, startAt = startAt, endAt = endAt
    )

    private fun question(id: String, type: QuestionType, points: Double) = LearningQuestion(
        id = id, topicId = "TOPIC_1", type = type, difficulty = QuestionDifficulty.MEDIUM,
        content = id, explanation = "", points = points, sortOrder = 0, isActive = true
    )
}

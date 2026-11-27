package com.htanh.appvk.domain.learning

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnswerEvaluatorTest {
    private val options = listOf(
        QuestionOption("OPT_A", "Q_01", "Đáp án A", true, 1),
        QuestionOption("OPT_B", "Q_01", "Đáp án B", false, 2)
    )

    @Test
    fun returnsCorrectForCorrectSelection() {
        val result = AnswerEvaluator.evaluate("OPT_A", options)

        assertTrue(result?.isCorrect == true)
        assertEquals("Đáp án A", result?.correctOptionText)
    }

    @Test
    fun returnsIncorrectAndCorrectOptionForWrongSelection() {
        val result = AnswerEvaluator.evaluate("OPT_B", options)

        assertFalse(result?.isCorrect == true)
        assertEquals("Đáp án A", result?.correctOptionText)
    }

    @Test
    fun returnsNullForUnknownOption() {
        assertNull(AnswerEvaluator.evaluate("OPT_UNKNOWN", options))
    }
}

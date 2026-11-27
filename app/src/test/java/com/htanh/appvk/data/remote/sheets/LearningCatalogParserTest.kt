package com.htanh.appvk.data.remote.sheets

import com.htanh.appvk.domain.learning.QuestionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LearningCatalogParserTest {
    @Test
    fun parsesRowsByHeaderNameAndAppliesDefaults() {
        val tables = validTables().toMutableMap()
        tables[LearningCatalogParser.SUBJECTS] = SheetTable.fromValues(
            LearningCatalogParser.SUBJECTS,
            listOf(
                listOf("name", "subject_id", "sort_order"),
                listOf("Toán học", "SUBJ_MATH", "2")
            )
        )

        val catalog = LearningCatalogParser.parse(tables)

        assertEquals("SUBJ_MATH", catalog.subjects.single().id)
        assertEquals("Toán học", catalog.subjects.single().name)
        assertTrue(catalog.subjects.single().isActive)
        assertEquals(QuestionType.MCQ, catalog.questions.single().type)
        assertEquals(1.0, catalog.questions.single().points, 0.0)
    }

    @Test
    fun acceptsEssayQuestionWithoutOptions() {
        val tables = validTables(
            questions = listOf(
                listOf("question_id", "topic_id", "question_type", "content"),
                listOf("Q_ESSAY", "TOP_01", "ESSAY", "Trình bày cách giải.")
            ),
            options = listOf(
                listOf("option_id", "question_id", "option_text", "is_correct")
            )
        )

        val catalog = LearningCatalogParser.parse(tables)

        assertEquals(QuestionType.ESSAY, catalog.questions.single().type)
        assertTrue(catalog.options.isEmpty())
    }

    @Test(expected = CatalogValidationException::class)
    fun rejectsDuplicateSubjectIds() {
        val tables = validTables().toMutableMap()
        tables[LearningCatalogParser.SUBJECTS] = table(
            LearningCatalogParser.SUBJECTS,
            listOf(
                listOf("subject_id", "name"),
                listOf("SUBJ_MATH", "Toán"),
                listOf("SUBJ_MATH", "Toán nâng cao")
            )
        )

        LearningCatalogParser.parse(tables)
    }

    @Test(expected = CatalogValidationException::class)
    fun rejectsBrokenChapterRelationship() {
        val tables = validTables().toMutableMap()
        tables[LearningCatalogParser.CHAPTERS] = table(
            LearningCatalogParser.CHAPTERS,
            listOf(
                listOf("chapter_id", "subject_id", "name"),
                listOf("CH_01", "SUBJ_UNKNOWN", "Chương 1")
            )
        )

        LearningCatalogParser.parse(tables)
    }

    @Test(expected = CatalogValidationException::class)
    fun rejectsMcqWithMoreThanOneCorrectOption() {
        val tables = validTables(
            options = listOf(
                listOf("option_id", "question_id", "option_text", "is_correct"),
                listOf("OPT_A", "Q_01", "A", "true"),
                listOf("OPT_B", "Q_01", "B", "yes")
            )
        )

        LearningCatalogParser.parse(tables)
    }

    @Test(expected = CatalogValidationException::class)
    fun rejectsEssayQuestionWithOptions() {
        val tables = validTables(
            questions = listOf(
                listOf("question_id", "topic_id", "question_type", "content"),
                listOf("Q_ESSAY", "TOP_01", "ESSAY", "Viết bài.")
            ),
            options = listOf(
                listOf("option_id", "question_id", "option_text", "is_correct"),
                listOf("OPT_A", "Q_ESSAY", "Không hợp lệ", "false")
            )
        )

        LearningCatalogParser.parse(tables)
    }

    @Test
    fun parsesBooleanAliases() {
        val tables = validTables()
        val catalog = LearningCatalogParser.parse(tables)
        assertTrue(catalog.options.first { it.id == "OPT_A" }.isCorrect)
        assertFalse(catalog.options.first { it.id == "OPT_B" }.isCorrect)
    }

    private fun validTables(
        questions: List<List<String>> = listOf(
            listOf("question_id", "topic_id", "question_type", "content"),
            listOf("Q_01", "TOP_01", "MCQ", "2 + 2 = ?")
        ),
        options: List<List<String>> = listOf(
            listOf("option_id", "question_id", "option_text", "is_correct"),
            listOf("OPT_A", "Q_01", "4", "true"),
            listOf("OPT_B", "Q_01", "5", "false")
        )
    ): Map<String, SheetTable> = mapOf(
        LearningCatalogParser.SUBJECTS to table(
            LearningCatalogParser.SUBJECTS,
            listOf(listOf("subject_id", "name"), listOf("SUBJ_MATH", "Toán"))
        ),
        LearningCatalogParser.CHAPTERS to table(
            LearningCatalogParser.CHAPTERS,
            listOf(listOf("chapter_id", "subject_id", "name"), listOf("CH_01", "SUBJ_MATH", "Số học"))
        ),
        LearningCatalogParser.TOPICS to table(
            LearningCatalogParser.TOPICS,
            listOf(listOf("topic_id", "chapter_id", "name"), listOf("TOP_01", "CH_01", "Phép cộng"))
        ),
        LearningCatalogParser.QUESTIONS to table(LearningCatalogParser.QUESTIONS, questions),
        LearningCatalogParser.QUESTION_OPTIONS to table(LearningCatalogParser.QUESTION_OPTIONS, options)
    )

    private fun table(tabName: String, rows: List<List<String>>) =
        SheetTable.fromValues(tabName, rows)
}

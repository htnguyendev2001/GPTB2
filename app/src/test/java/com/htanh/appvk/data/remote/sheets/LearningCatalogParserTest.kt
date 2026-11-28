package com.htanh.appvk.data.remote.sheets

import com.htanh.appvk.domain.learning.QuestionType
import com.htanh.appvk.domain.learning.ExamType
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
    fun parsesWorkbookV10ColumnNamesWithoutRenamingHeaders() {
        val tables = mapOf(
            LearningCatalogParser.SUBJECTS to table(
                LearningCatalogParser.SUBJECTS,
                listOf(
                    listOf("subject_id", "name", "description", "icon", "image_url", "sort_order", "is_active"),
                    listOf("SUBJ_MATH", "Toán học", "Mô tả", "calculate", "https://example.com/math.png", "1", "TRUE")
                )
            ),
            LearningCatalogParser.CHAPTERS to table(
                LearningCatalogParser.CHAPTERS,
                listOf(
                    listOf("chapter_id", "subject_id", "name", "description", "sort_order", "is_active"),
                    listOf("CH_01", "SUBJ_MATH", "Số học", "", "1", "TRUE")
                )
            ),
            LearningCatalogParser.TOPICS to table(
                LearningCatalogParser.TOPICS,
                listOf(
                    listOf("topic_id", "chapter_id", "name", "description", "sort_order", "is_active"),
                    listOf("TOP_01", "CH_01", "Phép cộng", "", "1", "TRUE")
                )
            ),
            LearningCatalogParser.QUESTIONS to table(
                LearningCatalogParser.QUESTIONS,
                listOf(
                    listOf("question_id", "subject_id", "chapter_id", "topic_id", "question_type", "difficulty", "content", "image_url", "explanation", "max_score", "estimated_time_seconds", "is_active", "sort_order"),
                    listOf("Q_01", "SUBJ_MATH", "CH_01", "TOP_01", "MCQ", "EASY", "2 + 2 = ?", "https://example.com/q.png", "Giải thích", "10", "30", "TRUE", "1")
                )
            ),
            LearningCatalogParser.QUESTION_OPTIONS to table(
                LearningCatalogParser.QUESTION_OPTIONS,
                listOf(
                    listOf("option_id", "question_id", "option_label", "content", "image_url", "is_correct", "sort_order"),
                    listOf("OPT_A", "Q_01", "A", "4", "https://example.com/a.png", "TRUE", "1"),
                    listOf("OPT_B", "Q_01", "B", "5", "", "FALSE", "2")
                )
            ),
            LearningCatalogParser.EXAMS to table(LearningCatalogParser.EXAMS, listOf(
                listOf("exam_id", "name", "subject_id", "duration_minutes", "exam_type", "total_score", "is_active"),
                listOf("EXAM_01", "Bài kiểm tra", "SUBJ_MATH", "45", "EXAM", "10", "TRUE")
            )),
            LearningCatalogParser.EXAM_QUESTIONS to table(LearningCatalogParser.EXAM_QUESTIONS, listOf(
                listOf("exam_id", "question_id", "sort_order", "score"),
                listOf("EXAM_01", "Q_01", "1", "10")
            ))
        )

        val catalog = LearningCatalogParser.parse(tables)

        assertEquals("calculate", catalog.subjects.single().iconName)
        assertEquals("https://example.com/math.png", catalog.subjects.single().coverImageUrl)
        assertEquals(10.0, catalog.questions.single().points, 0.0)
        assertEquals("https://example.com/q.png", catalog.questions.single().imageUrl)
        assertEquals(30, catalog.questions.single().estimatedTimeSeconds)
        assertEquals("4", catalog.options.first { it.id == "OPT_A" }.text)
        assertEquals("A", catalog.options.first { it.id == "OPT_A" }.label)
        assertEquals("https://example.com/a.png", catalog.options.first { it.id == "OPT_A" }.imageUrl)
        assertEquals(ExamType.EXAM, catalog.exams.single().type)
        assertEquals("EXAM_01", catalog.examQuestions.single().examId)
        assertEquals("Q_01", catalog.examQuestions.single().questionId)
        assertEquals(10.0, catalog.examQuestions.single().score, 0.0)
    }

    @Test(expected = CatalogValidationException::class)
    fun rejectsNegativeEstimatedQuestionDuration() {
        val tables = validTables().toMutableMap()
        tables[LearningCatalogParser.QUESTIONS] = table(
            LearningCatalogParser.QUESTIONS,
            listOf(
                listOf("question_id", "topic_id", "question_type", "content", "estimated_time_seconds"),
                listOf("Q_01", "TOP_01", "MCQ", "2 + 2 = ?", "-5")
            )
        )
        LearningCatalogParser.parse(tables)
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

    @Test(expected = CatalogValidationException::class)
    fun rejectsWorkbookQuestionWhoseRedundantChapterIdConflictsWithTopic() {
        val tables = mapOf(
            LearningCatalogParser.SUBJECTS to table(
                LearningCatalogParser.SUBJECTS,
                listOf(listOf("subject_id", "name"), listOf("SUBJ_MATH", "Toán"))
            ),
            LearningCatalogParser.CHAPTERS to table(
                LearningCatalogParser.CHAPTERS,
                listOf(listOf("chapter_id", "subject_id", "name"), listOf("CH_01", "SUBJ_MATH", "Chương 1"))
            ),
            LearningCatalogParser.TOPICS to table(
                LearningCatalogParser.TOPICS,
                listOf(listOf("topic_id", "chapter_id", "name"), listOf("TOP_01", "CH_01", "Chủ đề"))
            ),
            LearningCatalogParser.QUESTIONS to table(
                LearningCatalogParser.QUESTIONS,
                listOf(
                    listOf("question_id", "subject_id", "chapter_id", "topic_id", "question_type", "content"),
                    listOf("Q_01", "SUBJ_MATH", "CH_WRONG", "TOP_01", "MCQ", "Câu hỏi")
                )
            ),
            LearningCatalogParser.QUESTION_OPTIONS to table(
                LearningCatalogParser.QUESTION_OPTIONS,
                listOf(
                    listOf("option_id", "question_id", "content", "is_correct"),
                    listOf("OPT_A", "Q_01", "A", "TRUE"),
                    listOf("OPT_B", "Q_01", "B", "FALSE")
                )
            ),
            LearningCatalogParser.EXAMS to table(LearningCatalogParser.EXAMS, listOf(
                listOf("exam_id", "name", "subject_id", "duration_minutes", "exam_type", "total_score", "is_active"),
                listOf("EXAM_01", "Test", "SUBJ_MATH", "30", "EXAM", "10", "TRUE")
            )),
            LearningCatalogParser.EXAM_QUESTIONS to table(LearningCatalogParser.EXAM_QUESTIONS, listOf(
                listOf("exam_id", "question_id", "sort_order", "score"), listOf("EXAM_01", "Q_01", "1", "10")
            ))
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
        LearningCatalogParser.QUESTION_OPTIONS to table(LearningCatalogParser.QUESTION_OPTIONS, options),
        LearningCatalogParser.EXAMS to table(LearningCatalogParser.EXAMS, listOf(
            listOf("exam_id", "name", "subject_id", "duration_minutes", "exam_type", "total_score", "is_active"),
            listOf("EXAM_TEST", "Luyện tập", "SUBJ_MATH", "0", "PRACTICE", "10", "TRUE")
        )),
        LearningCatalogParser.EXAM_QUESTIONS to table(LearningCatalogParser.EXAM_QUESTIONS, listOf(
            listOf("exam_id", "question_id", "sort_order", "score"),
            listOf("EXAM_TEST", questions.getOrNull(1)?.firstOrNull() ?: "Q_01", "1", "10")
        ))
    )

    private fun table(tabName: String, rows: List<List<String>>) =
        SheetTable.fromValues(tabName, rows)
}

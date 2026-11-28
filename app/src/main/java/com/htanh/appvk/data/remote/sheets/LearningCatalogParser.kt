package com.htanh.appvk.data.remote.sheets

import com.htanh.appvk.domain.learning.Chapter
import com.htanh.appvk.domain.learning.LearningCatalog
import com.htanh.appvk.domain.learning.LearningQuestion
import com.htanh.appvk.domain.learning.LearningExam
import com.htanh.appvk.domain.learning.ExamQuestion
import com.htanh.appvk.domain.learning.ExamType
import com.htanh.appvk.domain.learning.ExamSessionRules
import com.htanh.appvk.domain.learning.QuestionDifficulty
import com.htanh.appvk.domain.learning.QuestionOption
import com.htanh.appvk.domain.learning.QuestionType
import com.htanh.appvk.domain.learning.Subject
import com.htanh.appvk.domain.learning.Topic

object LearningCatalogParser {
    private val requiredHeaders = mapOf(
        SUBJECTS to setOf("subject_id", "name"),
        CHAPTERS to setOf("chapter_id", "subject_id", "name"),
        TOPICS to setOf("topic_id", "chapter_id", "name"),
        QUESTIONS to setOf("question_id", "topic_id", "question_type", "content"),
        QUESTION_OPTIONS to setOf("option_id", "question_id", "is_correct"),
        EXAMS to setOf("exam_id", "name", "subject_id", "duration_minutes", "exam_type", "total_score", "is_active"),
        EXAM_QUESTIONS to setOf("exam_id", "question_id", "sort_order", "score")
    )

    fun parse(tables: Map<String, SheetTable>): LearningCatalog {
        val missingTabs = requiredHeaders.keys - tables.keys
        if (missingTabs.isNotEmpty()) {
            throw CatalogValidationException("Missing required tabs: ${missingTabs.joinToString()}")
        }
        requiredHeaders.forEach { (tab, headers) -> tables.getValue(tab).requireHeaders(tab, headers) }
        if (!tables.getValue(QUESTION_OPTIONS).hasAnyHeader("content", "option_text")) {
            throw CatalogValidationException("QUESTION_OPTIONS is missing required headers: content (or option_text)")
        }

        val subjects = parseSubjects(tables.getValue(SUBJECTS))
        val chapters = parseChapters(tables.getValue(CHAPTERS))
        val topics = parseTopics(tables.getValue(TOPICS))
        val questions = parseQuestions(tables.getValue(QUESTIONS))
        val options = parseOptions(tables.getValue(QUESTION_OPTIONS))
        val exams = parseExams(tables.getValue(EXAMS))
        val examQuestions = parseExamQuestions(tables.getValue(EXAM_QUESTIONS))

        ensureUnique("SUBJECTS", subjects.map { it.id })
        ensureUnique("CHAPTERS", chapters.map { it.id })
        ensureUnique("TOPICS", topics.map { it.id })
        ensureUnique("QUESTIONS", questions.map { it.id })
        ensureUnique("QUESTION_OPTIONS", options.map { it.id })
        ensureUnique("EXAMS", exams.map { it.id })
        ensureUnique("EXAM_QUESTIONS", examQuestions.map { "${it.examId}::${it.questionId}" })

        val subjectIds = subjects.map { it.id }.toSet()
        val chapterIds = chapters.map { it.id }.toSet()
        val topicIds = topics.map { it.id }.toSet()
        val questionIds = questions.map { it.id }.toSet()

        chapters.forEach { if (it.subjectId !in subjectIds) invalid("CHAPTERS", it.id, "unknown subject_id '${it.subjectId}'") }
        topics.forEach { if (it.chapterId !in chapterIds) invalid("TOPICS", it.id, "unknown chapter_id '${it.chapterId}'") }
        val chapterById = chapters.associateBy { it.id }
        val topicById = topics.associateBy { it.id }
        val questionTable = tables.getValue(QUESTIONS)
        questions.forEach { question ->
            val topic = topicById[question.topicId]
                ?: invalid("QUESTIONS", question.id, "unknown topic_id '${question.topicId}'")
            val chapter = chapterById[topic.chapterId]
                ?: invalid("TOPICS", topic.id, "unknown chapter_id '${topic.chapterId}'")
            val row = questionTable.rows.firstOrNull { questionTable.value(it, "question_id") == question.id }.orEmpty()
            val declaredChapterId = questionTable.value(row, "chapter_id")
            val declaredSubjectId = questionTable.value(row, "subject_id")
            if (declaredChapterId.isNotBlank() && declaredChapterId != chapter.id) {
                invalid("QUESTIONS", question.id, "chapter_id does not match topic hierarchy")
            }
            if (declaredSubjectId.isNotBlank() && declaredSubjectId != chapter.subjectId) {
                invalid("QUESTIONS", question.id, "subject_id does not match topic hierarchy")
            }
        }
        options.forEach { if (it.questionId !in questionIds) invalid("QUESTION_OPTIONS", it.id, "unknown question_id '${it.questionId}'") }
        exams.forEach { if (it.subjectId !in subjectIds) invalid("EXAMS", it.id, "unknown subject_id '${it.subjectId}'") }
        val examIds = exams.map { it.id }.toSet()
        examQuestions.forEach { mapping ->
            if (mapping.examId !in examIds) invalid("EXAM_QUESTIONS", mapping.examId, "unknown exam_id")
            if (mapping.questionId !in questionIds) invalid("EXAM_QUESTIONS", mapping.questionId, "unknown question_id")
        }
        exams.forEach { exam -> if (examQuestions.none { it.examId == exam.id }) invalid("EXAMS", exam.id, "exam must contain at least one question") }

        val optionsByQuestion = options.groupBy { it.questionId }
        questions.forEach { question ->
            val questionOptions = optionsByQuestion[question.id].orEmpty()
            when (question.type) {
                QuestionType.MCQ -> {
                    if (questionOptions.size < 2) invalid("QUESTIONS", question.id, "MCQ must have at least two options")
                    if (questionOptions.count { it.isCorrect } != 1) {
                        invalid("QUESTIONS", question.id, "MCQ must have exactly one correct option")
                    }
                }
                QuestionType.ESSAY -> if (questionOptions.isNotEmpty()) {
                    invalid("QUESTIONS", question.id, "ESSAY must not have QUESTION_OPTIONS rows")
                }
            }
        }

        return LearningCatalog(
            subjects = subjects.sortedWith(compareBy<Subject> { it.sortOrder }.thenBy { it.name.lowercase() }),
            chapters = chapters.sortedWith(compareBy<Chapter> { it.sortOrder }.thenBy { it.name.lowercase() }),
            topics = topics.sortedWith(compareBy<Topic> { it.sortOrder }.thenBy { it.name.lowercase() }),
            questions = questions.sortedWith(compareBy<LearningQuestion> { it.sortOrder }.thenBy { it.id }),
            options = options.sortedWith(compareBy<QuestionOption> { it.sortOrder }.thenBy { it.id }),
            exams = exams.sortedWith(compareBy<LearningExam> { it.name.lowercase() }.thenBy { it.id }),
            examQuestions = examQuestions.sortedWith(compareBy<ExamQuestion> { it.examId }.thenBy { it.sortOrder }.thenBy { it.questionId })
        )
    }

    private fun parseSubjects(table: SheetTable) = table.rows.mapIndexedNotNull { index, row ->
        if (row.isBlankRow()) return@mapIndexedNotNull null
        Subject(
            id = row.required(table, "subject_id", SUBJECTS, index),
            name = row.required(table, "name", SUBJECTS, index),
            description = table.value(row, "description"),
            iconUrl = table.value(row, "icon_url"),
            coverImageUrl = table.valueAny(row, "image_url", "cover_image_url"),
            sortOrder = row.intValue(table, "sort_order", SUBJECTS, index, 0),
            isActive = row.booleanValue(table, "is_active", SUBJECTS, index, true),
            iconName = table.value(row, "icon").takeIf(String::isNotBlank)
        )
    }

    private fun parseChapters(table: SheetTable) = table.rows.mapIndexedNotNull { index, row ->
        if (row.isBlankRow()) return@mapIndexedNotNull null
        Chapter(
            id = row.required(table, "chapter_id", CHAPTERS, index),
            subjectId = row.required(table, "subject_id", CHAPTERS, index),
            name = row.required(table, "name", CHAPTERS, index),
            description = table.value(row, "description"),
            sortOrder = row.intValue(table, "sort_order", CHAPTERS, index, 0),
            isActive = row.booleanValue(table, "is_active", CHAPTERS, index, true)
        )
    }

    private fun parseTopics(table: SheetTable) = table.rows.mapIndexedNotNull { index, row ->
        if (row.isBlankRow()) return@mapIndexedNotNull null
        Topic(
            id = row.required(table, "topic_id", TOPICS, index),
            chapterId = row.required(table, "chapter_id", TOPICS, index),
            name = row.required(table, "name", TOPICS, index),
            description = table.value(row, "description"),
            sortOrder = row.intValue(table, "sort_order", TOPICS, index, 0),
            isActive = row.booleanValue(table, "is_active", TOPICS, index, true)
        )
    }

    private fun parseQuestions(table: SheetTable) = table.rows.mapIndexedNotNull { index, row ->
        if (row.isBlankRow()) return@mapIndexedNotNull null
        val type = when (row.required(table, "question_type", QUESTIONS, index).uppercase()) {
            "MCQ" -> QuestionType.MCQ
            "ESSAY" -> QuestionType.ESSAY
            else -> invalid(QUESTIONS, "row ${index + 2}", "question_type must be MCQ or ESSAY")
        }
        val difficulty = when (table.value(row, "difficulty").ifBlank { "MEDIUM" }.uppercase()) {
            "EASY" -> QuestionDifficulty.EASY
            "MEDIUM" -> QuestionDifficulty.MEDIUM
            "HARD" -> QuestionDifficulty.HARD
            else -> invalid(QUESTIONS, "row ${index + 2}", "difficulty must be EASY, MEDIUM or HARD")
        }
        val pointsText = table.valueAny(row, "max_score", "points")
        val points = if (pointsText.isBlank()) 1.0 else pointsText.toDoubleOrNull()
            ?: invalid(QUESTIONS, "row ${index + 2}", "points must be a decimal number")
        if (!points.isFinite() || points < 0.0) invalid(QUESTIONS, "row ${index + 2}", "points must be non-negative")
        LearningQuestion(
            id = row.required(table, "question_id", QUESTIONS, index),
            topicId = row.required(table, "topic_id", QUESTIONS, index),
            type = type,
            difficulty = difficulty,
            content = row.required(table, "content", QUESTIONS, index),
            explanation = table.value(row, "explanation"),
            points = points,
            sortOrder = row.intValue(table, "sort_order", QUESTIONS, index, 0),
            isActive = row.booleanValue(table, "is_active", QUESTIONS, index, true),
            imageUrl = table.value(row, "image_url").takeIf(String::isNotBlank),
            estimatedTimeSeconds = row.optionalNonNegativeInt(table, "estimated_time_seconds", QUESTIONS, index)
        )
    }

    private fun parseOptions(table: SheetTable) = table.rows.mapIndexedNotNull { index, row ->
        if (row.isBlankRow()) return@mapIndexedNotNull null
        QuestionOption(
            id = row.required(table, "option_id", QUESTION_OPTIONS, index),
            questionId = row.required(table, "question_id", QUESTION_OPTIONS, index),
            text = row.requiredAny(table, QUESTION_OPTIONS, index, "content", "option_text"),
            isCorrect = row.booleanValue(table, "is_correct", QUESTION_OPTIONS, index, false, required = true),
            sortOrder = row.intValue(table, "sort_order", QUESTION_OPTIONS, index, 0),
            label = table.value(row, "option_label").takeIf(String::isNotBlank),
            imageUrl = table.value(row, "image_url").takeIf(String::isNotBlank)
        )
    }

    private fun parseExams(table: SheetTable) = table.rows.mapIndexedNotNull { index, row ->
        if (row.isBlankRow()) return@mapIndexedNotNull null
        val type = when (row.required(table, "exam_type", EXAMS, index).uppercase()) {
            "EXAM" -> ExamType.EXAM
            "PRACTICE" -> ExamType.PRACTICE
            else -> invalid(EXAMS, "row ${index + 2}", "exam_type must be EXAM or PRACTICE")
        }
        val duration = row.intValue(table, "duration_minutes", EXAMS, index, 0)
        if (duration < 0 || (type == ExamType.EXAM && duration == 0)) invalid(EXAMS, "row ${index + 2}", "duration_minutes is invalid")
        val totalScore = table.value(row, "total_score").toDoubleOrNull()
            ?: invalid(EXAMS, "row ${index + 2}", "total_score must be a decimal number")
        if (!totalScore.isFinite() || totalScore < 0.0) invalid(EXAMS, "row ${index + 2}", "total_score must be non-negative")
        val startAt = table.value(row, "start_at").takeIf(String::isNotBlank)
        val endAt = table.value(row, "end_at").takeIf(String::isNotBlank)
        val parsedStart = startAt?.let(ExamSessionRules::parseInstant)
        val parsedEnd = endAt?.let(ExamSessionRules::parseInstant)
        if (startAt != null && parsedStart == null) invalid(EXAMS, "row ${index + 2}", "start_at must be an ISO-8601 timestamp with timezone")
        if (endAt != null && parsedEnd == null) invalid(EXAMS, "row ${index + 2}", "end_at must be an ISO-8601 timestamp with timezone")
        if (parsedStart != null && parsedEnd != null && !parsedStart.isBefore(parsedEnd)) {
            invalid(EXAMS, "row ${index + 2}", "start_at must be earlier than end_at")
        }
        LearningExam(
            row.required(table, "exam_id", EXAMS, index), row.required(table, "name", EXAMS, index),
            table.value(row, "description"), row.required(table, "subject_id", EXAMS, index),
            duration, type, totalScore, row.booleanValue(table, "is_active", EXAMS, index, true, required = true),
            startAt, endAt
        )
    }

    private fun parseExamQuestions(table: SheetTable) = table.rows.mapIndexedNotNull { index, row ->
        if (row.isBlankRow()) return@mapIndexedNotNull null
        val score = table.value(row, "score").toDoubleOrNull()
            ?: invalid(EXAM_QUESTIONS, "row ${index + 2}", "score must be a decimal number")
        if (!score.isFinite() || score < 0.0) invalid(EXAM_QUESTIONS, "row ${index + 2}", "score must be non-negative")
        val sortOrder = row.intValue(table, "sort_order", EXAM_QUESTIONS, index, 0)
        if (sortOrder < 0) invalid(EXAM_QUESTIONS, "row ${index + 2}", "sort_order must be non-negative")
        ExamQuestion(
            row.required(table, "exam_id", EXAM_QUESTIONS, index),
            row.required(table, "question_id", EXAM_QUESTIONS, index),
            sortOrder, score
        )
    }

    private fun ensureUnique(tab: String, ids: List<String>) {
        val duplicate = ids.groupingBy { it }.eachCount().entries.firstOrNull { it.value > 1 }?.key
        if (duplicate != null) invalid(tab, duplicate, "duplicate ID")
    }

    private fun invalid(tab: String, row: String, reason: String): Nothing =
        throw CatalogValidationException("$tab $row: $reason")

    private fun List<String>.isBlankRow() = all(String::isBlank)

    private fun SheetTable.hasAnyHeader(vararg names: String): Boolean =
        names.any { name -> headers.any { header -> header.equals(name, ignoreCase = true) } }

    private fun SheetTable.valueAny(row: List<String>, vararg names: String): String =
        names.firstNotNullOfOrNull { name ->
            if (hasAnyHeader(name)) value(row, name).takeIf(String::isNotBlank) else null
        }.orEmpty()

    private fun List<String>.requiredAny(
        table: SheetTable, tab: String, index: Int, vararg headers: String
    ): String = table.valueAny(this, *headers).ifBlank {
        invalid(tab, "row ${index + 2}", "required field '${headers.joinToString(" or ")}' is blank")
    }

    private fun List<String>.required(table: SheetTable, header: String, tab: String, index: Int): String =
        table.value(this, header).ifBlank { invalid(tab, "row ${index + 2}", "required field '$header' is blank") }

    private fun List<String>.intValue(
        table: SheetTable, header: String, tab: String, index: Int, default: Int
    ): Int {
        val value = table.value(this, header)
        return if (value.isBlank()) default else value.toIntOrNull()
            ?: invalid(tab, "row ${index + 2}", "'$header' must be an integer")
    }

    private fun List<String>.optionalNonNegativeInt(
        table: SheetTable, header: String, tab: String, index: Int
    ): Int? {
        val value = table.value(this, header)
        if (value.isBlank()) return null
        val parsed = value.toIntOrNull()
            ?: invalid(tab, "row ${index + 2}", "'$header' must be an integer")
        if (parsed < 0) invalid(tab, "row ${index + 2}", "'$header' must be non-negative")
        return parsed
    }

    private fun List<String>.booleanValue(
        table: SheetTable,
        header: String,
        tab: String,
        index: Int,
        default: Boolean,
        required: Boolean = false
    ): Boolean {
        val value = table.value(this, header)
        if (value.isBlank()) {
            if (required) invalid(tab, "row ${index + 2}", "required field '$header' is blank")
            return default
        }
        return when (value.lowercase()) {
            "true", "1", "yes" -> true
            "false", "0", "no" -> false
            else -> invalid(tab, "row ${index + 2}", "'$header' must be true/false, 1/0 or yes/no")
        }
    }

    const val SUBJECTS = "SUBJECTS"
    const val CHAPTERS = "CHAPTERS"
    const val TOPICS = "TOPICS"
    const val QUESTIONS = "QUESTIONS"
    const val QUESTION_OPTIONS = "QUESTION_OPTIONS"
    const val EXAMS = "EXAMS"
    const val EXAM_QUESTIONS = "EXAM_QUESTIONS"
}

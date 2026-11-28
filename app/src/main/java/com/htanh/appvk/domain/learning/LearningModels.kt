package com.htanh.appvk.domain.learning

enum class QuestionType { MCQ, ESSAY }
enum class QuestionDifficulty { EASY, MEDIUM, HARD }
enum class ExamType { EXAM, PRACTICE }

data class LearningExam(
    val id: String, val name: String, val description: String, val subjectId: String,
    val durationMinutes: Int, val type: ExamType, val totalScore: Double, val isActive: Boolean,
    val startAt: String? = null, val endAt: String? = null
)
data class ExamQuestion(val examId: String, val questionId: String, val sortOrder: Int, val score: Double)

data class Subject(
    val id: String,
    val name: String,
    val description: String,
    val iconUrl: String,
    val coverImageUrl: String,
    val sortOrder: Int,
    val isActive: Boolean,
    val iconName: String? = null
)

data class Chapter(
    val id: String,
    val subjectId: String,
    val name: String,
    val description: String,
    val sortOrder: Int,
    val isActive: Boolean
)

data class Topic(
    val id: String,
    val chapterId: String,
    val name: String,
    val description: String,
    val sortOrder: Int,
    val isActive: Boolean
)

data class LearningQuestion(
    val id: String,
    val topicId: String,
    val type: QuestionType,
    val difficulty: QuestionDifficulty,
    val content: String,
    val explanation: String,
    val points: Double,
    val sortOrder: Int,
    val isActive: Boolean,
    val imageUrl: String? = null,
    val estimatedTimeSeconds: Int? = null
)

data class QuestionOption(
    val id: String,
    val questionId: String,
    val text: String,
    val isCorrect: Boolean,
    val sortOrder: Int,
    val label: String? = null,
    val imageUrl: String? = null
)

data class LearningCatalog(
    val subjects: List<Subject> = emptyList(),
    val chapters: List<Chapter> = emptyList(),
    val topics: List<Topic> = emptyList(),
    val questions: List<LearningQuestion> = emptyList(),
    val options: List<QuestionOption> = emptyList(),
    val exams: List<LearningExam> = emptyList(),
    val examQuestions: List<ExamQuestion> = emptyList(),
    val lastSuccessfulSyncAt: Long? = null
)

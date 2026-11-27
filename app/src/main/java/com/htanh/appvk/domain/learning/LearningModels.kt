package com.htanh.appvk.domain.learning

enum class QuestionType { MCQ, ESSAY }
enum class QuestionDifficulty { EASY, MEDIUM, HARD }

data class Subject(
    val id: String,
    val name: String,
    val description: String,
    val iconUrl: String,
    val coverImageUrl: String,
    val sortOrder: Int,
    val isActive: Boolean
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
    val isActive: Boolean
)

data class QuestionOption(
    val id: String,
    val questionId: String,
    val text: String,
    val isCorrect: Boolean,
    val sortOrder: Int
)

data class LearningCatalog(
    val subjects: List<Subject> = emptyList(),
    val chapters: List<Chapter> = emptyList(),
    val topics: List<Topic> = emptyList(),
    val questions: List<LearningQuestion> = emptyList(),
    val options: List<QuestionOption> = emptyList(),
    val lastSuccessfulSyncAt: Long? = null
)

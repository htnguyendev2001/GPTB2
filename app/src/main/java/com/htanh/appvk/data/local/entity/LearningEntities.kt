package com.htanh.appvk.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val iconUrl: String,
    val coverImageUrl: String,
    val sortOrder: Int,
    val isActive: Boolean
)

@Entity(tableName = "chapters", indices = [Index("subjectId")])
data class ChapterEntity(
    @PrimaryKey val id: String,
    val subjectId: String,
    val name: String,
    val description: String,
    val sortOrder: Int,
    val isActive: Boolean
)

@Entity(tableName = "topics", indices = [Index("chapterId")])
data class TopicEntity(
    @PrimaryKey val id: String,
    val chapterId: String,
    val name: String,
    val description: String,
    val sortOrder: Int,
    val isActive: Boolean
)

@Entity(tableName = "learning_questions", indices = [Index("topicId")])
data class LearningQuestionEntity(
    @PrimaryKey val id: String,
    val topicId: String,
    val type: String,
    val difficulty: String,
    val content: String,
    val explanation: String,
    val points: Double,
    val sortOrder: Int,
    val isActive: Boolean
)

@Entity(tableName = "question_options", indices = [Index("questionId")])
data class QuestionOptionEntity(
    @PrimaryKey val id: String,
    val questionId: String,
    val text: String,
    val isCorrect: Boolean,
    val sortOrder: Int
)

@Entity(tableName = "sync_metadata")
data class SyncMetadataEntity(
    @PrimaryKey val key: String,
    val value: String
)

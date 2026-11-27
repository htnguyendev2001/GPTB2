package com.htanh.appvk.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
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

@Entity(
    tableName = "chapters",
    indices = [Index("subjectId")],
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ChapterEntity(
    @PrimaryKey val id: String,
    val subjectId: String,
    val name: String,
    val description: String,
    val sortOrder: Int,
    val isActive: Boolean
)

@Entity(
    tableName = "topics",
    indices = [Index("chapterId")],
    foreignKeys = [
        ForeignKey(
            entity = ChapterEntity::class,
            parentColumns = ["id"],
            childColumns = ["chapterId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TopicEntity(
    @PrimaryKey val id: String,
    val chapterId: String,
    val name: String,
    val description: String,
    val sortOrder: Int,
    val isActive: Boolean
)

@Entity(
    tableName = "learning_questions",
    indices = [Index("topicId")],
    foreignKeys = [
        ForeignKey(
            entity = TopicEntity::class,
            parentColumns = ["id"],
            childColumns = ["topicId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
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

@Entity(
    tableName = "question_options",
    indices = [Index("questionId")],
    foreignKeys = [
        ForeignKey(
            entity = LearningQuestionEntity::class,
            parentColumns = ["id"],
            childColumns = ["questionId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
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

package com.htanh.appvk.data.repository

import androidx.room.withTransaction
import com.htanh.appvk.data.local.LearningDatabase
import com.htanh.appvk.data.local.entity.ChapterEntity
import com.htanh.appvk.data.local.entity.LearningQuestionEntity
import com.htanh.appvk.data.local.entity.LearningExamEntity
import com.htanh.appvk.data.local.entity.ExamQuestionEntity
import com.htanh.appvk.data.local.entity.QuestionOptionEntity
import com.htanh.appvk.data.local.entity.SubjectEntity
import com.htanh.appvk.data.local.entity.SyncMetadataEntity
import com.htanh.appvk.data.local.entity.TopicEntity
import com.htanh.appvk.data.remote.sheets.LearningContentRemoteDataSource
import com.htanh.appvk.data.remote.sheets.LearningContentSyncException
import com.htanh.appvk.domain.learning.LearningCatalog
import com.htanh.appvk.domain.learning.QuestionDifficulty
import com.htanh.appvk.domain.learning.QuestionType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class LearningContentRepositoryImpl(
    private val database: LearningDatabase,
    private val remoteDataSource: LearningContentRemoteDataSource
) : LearningContentRepository {
    private val dao = database.learningContentDao()
    private val syncMutex = Mutex()

    override fun observeCatalog(): Flow<LearningCatalog> {
        val hierarchy = combine(
            dao.observeSubjects(),
            dao.observeChapters(),
            dao.observeTopics()
        ) { subjects, chapters, topics ->
            Triple(subjects, chapters, topics)
        }
        val questionContent = combine(
            dao.observeQuestions(),
            dao.observeOptions()
        ) { questions, options ->
            questions to options
        }
        val allContent = combine(questionContent, combine(dao.observeExams(), dao.observeExamQuestions()) { exams, mappings -> exams to mappings }) { questions, exams -> Triple(questions.first, questions.second, exams) }
        val cachedCatalog = combine(hierarchy, allContent) { tree, content ->
            LearningCatalog(
                subjects = tree.first.map { it.toDomain() }, chapters = tree.second.map { it.toDomain() },
                topics = tree.third.map { it.toDomain() }, questions = content.first.map { it.toDomain() },
                options = content.second.map { it.toDomain() }, exams = content.third.first.map { it.toDomain() },
                examQuestions = content.third.second.map { it.toDomain() }
            )
        }
        return combine(cachedCatalog, dao.observeMetadata(LAST_SUCCESSFUL_SYNC_KEY)) { catalog, syncTimestamp ->
            catalog.copy(lastSuccessfulSyncAt = syncTimestamp?.toLongOrNull())
        }
    }

    override suspend fun refresh(): LearningContentSyncResult = syncMutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                val catalog = remoteDataSource.fetchCatalog()
                val syncedAt = System.currentTimeMillis()
                database.withTransaction {
                    dao.clearExamQuestions()
                    dao.clearExams()
                    dao.clearOptions()
                    dao.clearQuestions()
                    dao.clearTopics()
                    dao.clearChapters()
                    dao.clearSubjects()
                    dao.insertSubjects(catalog.subjects.map { it.toEntity() })
                    dao.insertChapters(catalog.chapters.map { it.toEntity() })
                    dao.insertTopics(catalog.topics.map { it.toEntity() })
                    dao.insertQuestions(catalog.questions.map { it.toEntity() })
                    dao.insertOptions(catalog.options.map { it.toEntity() })
                    dao.insertExams(catalog.exams.map { it.toEntity() })
                    dao.insertExamQuestions(catalog.examQuestions.map { it.toEntity() })
                    dao.upsertMetadata(SyncMetadataEntity(LAST_SUCCESSFUL_SYNC_KEY, syncedAt.toString()))
                }
                LearningContentSyncResult.Success(syncedAt)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: LearningContentSyncException) {
                LearningContentSyncResult.Failure(
                    error.message ?: "Learning content sync failed.",
                    error.category.toRepositoryCategory()
                )
            } catch (error: Exception) {
                LearningContentSyncResult.Failure(
                    error.message ?: "Unable to save the learning content cache.",
                    LearningContentSyncResult.Category.STORAGE
                )
            }
        }
    }

    private fun LearningContentSyncException.Category.toRepositoryCategory() = when (this) {
        LearningContentSyncException.Category.CONFIGURATION -> LearningContentSyncResult.Category.CONFIGURATION
        LearningContentSyncException.Category.NETWORK -> LearningContentSyncResult.Category.NETWORK
        LearningContentSyncException.Category.ACCESS -> LearningContentSyncResult.Category.ACCESS
        LearningContentSyncException.Category.NOT_FOUND -> LearningContentSyncResult.Category.NOT_FOUND
        LearningContentSyncException.Category.API -> LearningContentSyncResult.Category.API
        LearningContentSyncException.Category.INVALID_CONTENT -> LearningContentSyncResult.Category.INVALID_CONTENT
    }

    private fun LearningExamEntity.toDomain() = com.htanh.appvk.domain.learning.LearningExam(
        id, name, description, subjectId, durationMinutes, com.htanh.appvk.domain.learning.ExamType.valueOf(type), totalScore, isActive, startAt, endAt
    )
    private fun ExamQuestionEntity.toDomain() = com.htanh.appvk.domain.learning.ExamQuestion(examId, questionId, sortOrder, score)
    private fun com.htanh.appvk.domain.learning.LearningExam.toEntity() = LearningExamEntity(id, name, description, subjectId, durationMinutes, type.name, totalScore, isActive, startAt, endAt)
    private fun com.htanh.appvk.domain.learning.ExamQuestion.toEntity() = ExamQuestionEntity(examId, questionId, sortOrder, score)

    private fun SubjectEntity.toDomain() = com.htanh.appvk.domain.learning.Subject(
        id, name, description, iconUrl, coverImageUrl, sortOrder, isActive, iconName
    )

    private fun ChapterEntity.toDomain() = com.htanh.appvk.domain.learning.Chapter(
        id, subjectId, name, description, sortOrder, isActive
    )

    private fun TopicEntity.toDomain() = com.htanh.appvk.domain.learning.Topic(
        id, chapterId, name, description, sortOrder, isActive
    )

    private fun LearningQuestionEntity.toDomain() = com.htanh.appvk.domain.learning.LearningQuestion(
        id,
        topicId,
        QuestionType.valueOf(type),
        QuestionDifficulty.valueOf(difficulty),
        content,
        explanation,
        points,
        sortOrder,
        isActive,
        imageUrl,
        estimatedTimeSeconds
    )

    private fun QuestionOptionEntity.toDomain() = com.htanh.appvk.domain.learning.QuestionOption(
        id, questionId, text, isCorrect, sortOrder, label, imageUrl
    )

    private fun com.htanh.appvk.domain.learning.Subject.toEntity() =
        SubjectEntity(id, name, description, iconUrl, coverImageUrl, sortOrder, isActive, iconName)

    private fun com.htanh.appvk.domain.learning.Chapter.toEntity() =
        ChapterEntity(id, subjectId, name, description, sortOrder, isActive)

    private fun com.htanh.appvk.domain.learning.Topic.toEntity() =
        TopicEntity(id, chapterId, name, description, sortOrder, isActive)

    private fun com.htanh.appvk.domain.learning.LearningQuestion.toEntity() =
        LearningQuestionEntity(id, topicId, type.name, difficulty.name, content, explanation, points, sortOrder, isActive, imageUrl, estimatedTimeSeconds)

    private fun com.htanh.appvk.domain.learning.QuestionOption.toEntity() =
        QuestionOptionEntity(id, questionId, text, isCorrect, sortOrder, label, imageUrl)

    companion object {
        private const val LAST_SUCCESSFUL_SYNC_KEY = "last_successful_sync_at"
    }
}

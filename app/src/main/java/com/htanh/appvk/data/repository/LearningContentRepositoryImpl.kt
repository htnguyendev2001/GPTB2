package com.htanh.appvk.data.repository

import androidx.room.withTransaction
import com.htanh.appvk.data.local.LearningDatabase
import com.htanh.appvk.data.local.entity.ChapterEntity
import com.htanh.appvk.data.local.entity.LearningQuestionEntity
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
import kotlinx.coroutines.withContext

class LearningContentRepositoryImpl(
    private val database: LearningDatabase,
    private val remoteDataSource: LearningContentRemoteDataSource
) : LearningContentRepository {
    private val dao = database.learningContentDao()

    override fun observeCatalog(): Flow<LearningCatalog> {
        val cachedCatalog = combine(
            dao.observeSubjects(),
            dao.observeChapters(),
            dao.observeTopics(),
            dao.observeQuestions(),
            dao.observeOptions()
        ) { subjects, chapters, topics, questions, options ->
            LearningCatalog(
                subjects = subjects.map { it.toDomain() },
                chapters = chapters.map { it.toDomain() },
                topics = topics.map { it.toDomain() },
                questions = questions.map { it.toDomain() },
                options = options.map { it.toDomain() }
            )
        }
        return combine(cachedCatalog, dao.observeMetadata(LAST_SUCCESSFUL_SYNC_KEY)) { catalog, syncTimestamp ->
            catalog.copy(lastSuccessfulSyncAt = syncTimestamp?.toLongOrNull())
        }
    }

    override suspend fun refresh(): LearningContentSyncResult = withContext(Dispatchers.IO) {
        try {
            val catalog = remoteDataSource.fetchCatalog()
            val syncedAt = System.currentTimeMillis()
            database.withTransaction {
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
                dao.upsertMetadata(SyncMetadataEntity(LAST_SUCCESSFUL_SYNC_KEY, syncedAt.toString()))
            }
            LearningContentSyncResult.Success(syncedAt)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: LearningContentSyncException) {
            LearningContentSyncResult.Failure(error.message ?: "Learning content sync failed.", error.category.toRepositoryCategory())
        } catch (error: Exception) {
            LearningContentSyncResult.Failure(
                error.message ?: "Unable to save the learning content cache.",
                LearningContentSyncResult.Category.STORAGE
            )
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

    private fun SubjectEntity.toDomain() = com.htanh.appvk.domain.learning.Subject(
        id, name, description, iconUrl, coverImageUrl, sortOrder, isActive
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
        isActive
    )

    private fun QuestionOptionEntity.toDomain() = com.htanh.appvk.domain.learning.QuestionOption(
        id, questionId, text, isCorrect, sortOrder
    )

    private fun com.htanh.appvk.domain.learning.Subject.toEntity() =
        SubjectEntity(id, name, description, iconUrl, coverImageUrl, sortOrder, isActive)

    private fun com.htanh.appvk.domain.learning.Chapter.toEntity() =
        ChapterEntity(id, subjectId, name, description, sortOrder, isActive)

    private fun com.htanh.appvk.domain.learning.Topic.toEntity() =
        TopicEntity(id, chapterId, name, description, sortOrder, isActive)

    private fun com.htanh.appvk.domain.learning.LearningQuestion.toEntity() =
        LearningQuestionEntity(id, topicId, type.name, difficulty.name, content, explanation, points, sortOrder, isActive)

    private fun com.htanh.appvk.domain.learning.QuestionOption.toEntity() =
        QuestionOptionEntity(id, questionId, text, isCorrect, sortOrder)

    companion object {
        private const val LAST_SUCCESSFUL_SYNC_KEY = "last_successful_sync_at"
    }
}

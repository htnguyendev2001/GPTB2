package com.htanh.appvk.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.htanh.appvk.data.local.entity.ChapterEntity
import com.htanh.appvk.data.local.entity.LearningQuestionEntity
import com.htanh.appvk.data.local.entity.LearningExamEntity
import com.htanh.appvk.data.local.entity.ExamQuestionEntity
import com.htanh.appvk.data.local.entity.QuestionOptionEntity
import com.htanh.appvk.data.local.entity.SubjectEntity
import com.htanh.appvk.data.local.entity.SyncMetadataEntity
import com.htanh.appvk.data.local.entity.TopicEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LearningContentDao {
    @Query("SELECT * FROM subjects WHERE isActive = 1 ORDER BY sortOrder ASC, name COLLATE NOCASE ASC")
    fun observeSubjects(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM chapters WHERE isActive = 1 ORDER BY sortOrder ASC, name COLLATE NOCASE ASC")
    fun observeChapters(): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM topics WHERE isActive = 1 ORDER BY sortOrder ASC, name COLLATE NOCASE ASC")
    fun observeTopics(): Flow<List<TopicEntity>>

    @Query("SELECT * FROM learning_questions WHERE isActive = 1 ORDER BY sortOrder ASC, id ASC")
    fun observeQuestions(): Flow<List<LearningQuestionEntity>>

    @Query("SELECT * FROM question_options ORDER BY sortOrder ASC, id ASC")
    fun observeOptions(): Flow<List<QuestionOptionEntity>>

    @Query("SELECT * FROM learning_exams WHERE isActive = 1 ORDER BY name COLLATE NOCASE ASC, id ASC")
    fun observeExams(): Flow<List<LearningExamEntity>>

    @Query("SELECT * FROM exam_questions ORDER BY examId ASC, sortOrder ASC, questionId ASC")
    fun observeExamQuestions(): Flow<List<ExamQuestionEntity>>

    @Query("SELECT value FROM sync_metadata WHERE key = :key LIMIT 1")
    fun observeMetadata(key: String): Flow<String?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubjects(items: List<SubjectEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(items: List<ChapterEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopics(items: List<TopicEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(items: List<LearningQuestionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOptions(items: List<QuestionOptionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMetadata(item: SyncMetadataEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExams(items: List<LearningExamEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExamQuestions(items: List<ExamQuestionEntity>)

    @Query("DELETE FROM exam_questions")
    suspend fun clearExamQuestions()

    @Query("DELETE FROM learning_exams")
    suspend fun clearExams()

    @Query("DELETE FROM question_options")
    suspend fun clearOptions()

    @Query("DELETE FROM learning_questions")
    suspend fun clearQuestions()

    @Query("DELETE FROM topics")
    suspend fun clearTopics()

    @Query("DELETE FROM chapters")
    suspend fun clearChapters()

    @Query("DELETE FROM subjects")
    suspend fun clearSubjects()
}

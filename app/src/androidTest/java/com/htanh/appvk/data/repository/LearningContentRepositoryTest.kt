package com.htanh.appvk.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.htanh.appvk.data.local.LearningDatabase
import com.htanh.appvk.data.remote.sheets.LearningContentRemoteDataSource
import com.htanh.appvk.data.remote.sheets.LearningContentSyncException
import com.htanh.appvk.domain.learning.Chapter
import com.htanh.appvk.domain.learning.LearningCatalog
import com.htanh.appvk.domain.learning.LearningExam
import com.htanh.appvk.domain.learning.ExamQuestion
import com.htanh.appvk.domain.learning.ExamType
import com.htanh.appvk.domain.learning.LearningQuestion
import com.htanh.appvk.domain.learning.QuestionDifficulty
import com.htanh.appvk.domain.learning.QuestionOption
import com.htanh.appvk.domain.learning.QuestionType
import com.htanh.appvk.domain.learning.Subject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LearningContentRepositoryTest {
    @Test
    fun failedRemoteRefreshKeepsPreviouslyCachedSnapshot() = runBlocking {
        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, LearningDatabase::class.java).build()
        val remote = FakeRemoteDataSource(sampleCatalog())
        val repository = LearningContentRepositoryImpl(database, remote)

        try {
            assertTrue(repository.refresh() is LearningContentSyncResult.Success)
            remote.shouldFail = true

            assertTrue(repository.refresh() is LearningContentSyncResult.Failure)

            val cached = repository.observeCatalog().first()
            assertEquals("SUBJ_MATH", cached.subjects.single().id)
            assertEquals("CH_01", cached.chapters.single().id)
            assertEquals("calculate", cached.subjects.single().iconName)
            assertEquals("https://example.com/question.png", cached.questions.single().imageUrl)
            assertEquals(45, cached.questions.single().estimatedTimeSeconds)
            assertEquals("A", cached.options.single().label)
            assertEquals("https://example.com/option.png", cached.options.single().imageUrl)
            assertEquals("EXAM_01", cached.exams.single().id)
            assertEquals(ExamType.EXAM, cached.exams.single().type)
            assertEquals(10.0, cached.examQuestions.single().score, 0.0)
            assertTrue(cached.lastSuccessfulSyncAt != null)
        } finally {
            database.close()
        }
    }

    private fun sampleCatalog() = LearningCatalog(
        subjects = listOf(Subject("SUBJ_MATH", "Toán", "", "", "", 0, true, iconName = "calculate")),
        chapters = listOf(Chapter("CH_01", "SUBJ_MATH", "Số học", "", 0, true)),
        topics = listOf(com.htanh.appvk.domain.learning.Topic("TOP_01", "CH_01", "Cộng", "", 0, true)),
        questions = listOf(
            LearningQuestion(
                "Q_01", "TOP_01", QuestionType.MCQ, QuestionDifficulty.EASY,
                "1 + 1 = ?", "", 1.0, 0, true,
                imageUrl = "https://example.com/question.png",
                estimatedTimeSeconds = 45
            )
        ),
        options = listOf(
            QuestionOption(
                "OPT_A", "Q_01", "2", true, 0,
                label = "A",
                imageUrl = "https://example.com/option.png"
            )
        ),
        exams = listOf(LearningExam("EXAM_01", "Toán kiểm tra", "Đề mẫu", "SUBJ_MATH", 45, ExamType.EXAM, 10.0, true)),
        examQuestions = listOf(ExamQuestion("EXAM_01", "Q_01", 1, 10.0))
    )

    private class FakeRemoteDataSource(
        private val catalog: LearningCatalog
    ) : LearningContentRemoteDataSource {
        var shouldFail: Boolean = false

        override suspend fun fetchCatalog(): LearningCatalog {
            if (shouldFail) throw LearningContentSyncException(LearningContentSyncException.Category.NETWORK, "Simulated network failure.")
            return catalog
        }
    }
}

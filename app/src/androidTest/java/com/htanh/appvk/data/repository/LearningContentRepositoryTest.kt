package com.htanh.appvk.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.htanh.appvk.data.local.LearningDatabase
import com.htanh.appvk.data.remote.sheets.LearningContentRemoteDataSource
import com.htanh.appvk.domain.learning.Chapter
import com.htanh.appvk.domain.learning.LearningCatalog
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
            assertTrue(cached.lastSuccessfulSyncAt != null)
        } finally {
            database.close()
        }
    }

    private fun sampleCatalog() = LearningCatalog(
        subjects = listOf(Subject("SUBJ_MATH", "Toán", "", "", "", 0, true)),
        chapters = listOf(Chapter("CH_01", "SUBJ_MATH", "Số học", "", 0, true))
    )

    private class FakeRemoteDataSource(
        private val catalog: LearningCatalog
    ) : LearningContentRemoteDataSource {
        var shouldFail: Boolean = false

        override suspend fun fetchCatalog(): LearningCatalog {
            if (shouldFail) error("Simulated network failure")
            return catalog
        }
    }
}

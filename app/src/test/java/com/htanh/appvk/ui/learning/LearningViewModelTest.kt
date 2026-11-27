package com.htanh.appvk.ui.learning

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import com.htanh.appvk.data.repository.LearningContentRepository
import com.htanh.appvk.data.repository.LearningContentSyncResult
import com.htanh.appvk.domain.learning.LearningCatalog

class LearningViewModelTest {

    @Test
    fun rapidRefreshCallsOnlyStartOneRepositoryRequest() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val completion = CompletableDeferred<LearningContentSyncResult>()
            val repository = FakeLearningContentRepository { completion.await() }
            val viewModel = LearningViewModel(repository)

            // The constructor starts the initial refresh synchronously; another
            // call before the launched coroutine runs must be ignored.
            viewModel.refresh()
            assertTrue(viewModel.state.value.isRefreshing)

            runCurrent()
            assertEquals(1, repository.refreshCalls)

            completion.complete(LearningContentSyncResult.Success(123L))
            advanceUntilIdle()

            assertFalse(viewModel.state.value.isRefreshing)
            assertEquals("Đồng bộ học liệu thành công.", viewModel.state.value.syncMessage)
            assertEquals(1, repository.refreshCalls)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun unexpectedRepositoryExceptionReleasesRefreshingState() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = FakeLearningContentRepository {
                throw IllegalStateException("unexpected failure")
            }
            val viewModel = LearningViewModel(repository)

            advanceUntilIdle()

            assertFalse(viewModel.state.value.isRefreshing)
            assertEquals("unexpected failure", viewModel.state.value.errorMessage)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private class FakeLearningContentRepository(
        private val refreshBlock: suspend () -> LearningContentSyncResult
    ) : LearningContentRepository {
        var refreshCalls: Int = 0
            private set

        override fun observeCatalog(): Flow<LearningCatalog> = flowOf(LearningCatalog())

        override suspend fun refresh(): LearningContentSyncResult {
            refreshCalls += 1
            return refreshBlock()
        }
    }
}

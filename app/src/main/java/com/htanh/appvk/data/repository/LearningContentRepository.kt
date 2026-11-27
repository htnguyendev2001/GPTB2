package com.htanh.appvk.data.repository

import com.htanh.appvk.domain.learning.LearningCatalog
import kotlinx.coroutines.flow.Flow

interface LearningContentRepository {
    fun observeCatalog(): Flow<LearningCatalog>
    suspend fun refresh(): LearningContentSyncResult
}

sealed class LearningContentSyncResult {
    data class Success(val syncedAt: Long) : LearningContentSyncResult()
    data class Failure(val message: String, val category: Category) : LearningContentSyncResult()

    enum class Category { CONFIGURATION, NETWORK, ACCESS, NOT_FOUND, API, INVALID_CONTENT, STORAGE }
}

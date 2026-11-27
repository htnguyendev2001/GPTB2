package com.htanh.appvk.data.remote.sheets

import com.htanh.appvk.domain.learning.LearningCatalog

interface LearningContentRemoteDataSource {
    suspend fun fetchCatalog(): LearningCatalog
}

package com.htanh.appvk

import android.app.Application
import androidx.room.Room
import com.htanh.appvk.data.local.LearningDatabase
import com.htanh.appvk.data.remote.sheets.GoogleSheetsRemoteDataSource
import com.htanh.appvk.data.repository.LearningContentRepository
import com.htanh.appvk.data.repository.LearningContentRepositoryImpl

class HtavkApplication : Application() {
    lateinit var learningContentRepository: LearningContentRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val database = Room.databaseBuilder(
            applicationContext,
            LearningDatabase::class.java,
            "htavk_learning_content.db"
        ).addMigrations(LearningDatabase.MIGRATION_1_2).build()

        val remoteDataSource = GoogleSheetsRemoteDataSource(
            spreadsheetId = BuildConfig.LEARNING_CONTENT_SPREADSHEET_ID,
            apiKey = BuildConfig.GOOGLE_SHEETS_API_KEY
        )
        learningContentRepository = LearningContentRepositoryImpl(database, remoteDataSource)
    }
    
}

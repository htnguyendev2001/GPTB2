package com.htanh.appvk

import android.app.Application
import androidx.room.Room
import com.htanh.appvk.data.local.LearningDatabase
import com.htanh.appvk.data.remote.sheets.GoogleSheetsRemoteDataSource
import com.htanh.appvk.data.repository.LearningContentRepository
import com.htanh.appvk.data.repository.LearningContentRepositoryImpl
import com.htanh.appvk.data.repository.ExamAttemptRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HtavkApplication : Application() {
    lateinit var learningContentRepository: LearningContentRepository
        private set

    var examAttemptRepository: ExamAttemptRepository? = null
        private set

    override fun onCreate() {
        super.onCreate()
        val database = Room.databaseBuilder(
            applicationContext,
            LearningDatabase::class.java,
            "htavk_learning_content.db"
        ).addMigrations(LearningDatabase.MIGRATION_1_2, LearningDatabase.MIGRATION_2_3).build()

        val remoteDataSource = GoogleSheetsRemoteDataSource(
            context = applicationContext,
            spreadsheetId = BuildConfig.LEARNING_CONTENT_SPREADSHEET_ID,
            apiKey = BuildConfig.GOOGLE_SHEETS_API_KEY
        )
        learningContentRepository = LearningContentRepositoryImpl(database, remoteDataSource)

        if (FirebaseApp.initializeApp(applicationContext) != null) {
            examAttemptRepository = ExamAttemptRepository(
                FirebaseAuth.getInstance(),
                FirebaseFirestore.getInstance()
            )
        }
    }
}

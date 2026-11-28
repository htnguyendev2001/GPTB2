package com.htanh.appvk

import android.app.Application
import androidx.room.Room
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.firestore.FirebaseFirestore
import com.htanh.appvk.data.local.LearningDatabase
import com.htanh.appvk.data.remote.sheets.GoogleSheetsRemoteDataSource
import com.htanh.appvk.data.repository.ExamAttemptRepository
import com.htanh.appvk.data.repository.ExamHistoryStore
import com.htanh.appvk.data.repository.LearningContentRepository
import com.htanh.appvk.data.repository.LearningContentRepositoryImpl

class HtavkApplication : Application() {
    lateinit var learningContentRepository: LearningContentRepository
        private set

    lateinit var examHistoryStore: ExamHistoryStore
        private set

    lateinit var examAttemptRepository: ExamAttemptRepository
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

        examHistoryStore = ExamHistoryStore(applicationContext)
        val firebaseApp = FirebaseApp.initializeApp(applicationContext)
        val firestore = if (firebaseApp != null) {
            val appCheck = FirebaseAppCheck.getInstance(firebaseApp)
            val providerFactory = if (BuildConfig.DEBUG) {
                DebugAppCheckProviderFactory.getInstance()
            } else {
                PlayIntegrityAppCheckProviderFactory.getInstance()
            }
            appCheck.installAppCheckProviderFactory(providerFactory)
            FirebaseFirestore.getInstance(firebaseApp)
        } else {
            null
        }
        examAttemptRepository = ExamAttemptRepository(
            context = applicationContext,
            firestore = firestore,
            historyStore = examHistoryStore
        )
    }
}

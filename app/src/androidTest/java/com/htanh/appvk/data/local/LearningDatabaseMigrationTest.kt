package com.htanh.appvk.data.local

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LearningDatabaseMigrationTest {
    @Test
    fun migratesVersionOneCacheWithoutDroppingExistingRows() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val databaseName = "learning_migration_v1_test.db"
        context.deleteDatabase(databaseName)
        val file = context.getDatabasePath(databaseName)
        file.parentFile?.mkdirs()

        val legacy = SQLiteDatabase.openOrCreateDatabase(file, null)
        try {
            createVersionOneSchema(legacy)
            legacy.execSQL(
                "INSERT INTO subjects (id, name, description, iconUrl, coverImageUrl, sortOrder, isActive) " +
                    "VALUES ('SUBJ_MATH', 'Toán', '', '', '', 0, 1)"
            )
            legacy.execSQL(
                "INSERT INTO chapters (id, subjectId, name, description, sortOrder, isActive) " +
                    "VALUES ('CH_01', 'SUBJ_MATH', 'Số học', '', 0, 1)"
            )
            legacy.execSQL(
                "INSERT INTO topics (id, chapterId, name, description, sortOrder, isActive) " +
                    "VALUES ('TOP_01', 'CH_01', 'Phép cộng', '', 0, 1)"
            )
            legacy.execSQL(
                "INSERT INTO learning_questions (id, topicId, type, difficulty, content, explanation, points, sortOrder, isActive) " +
                    "VALUES ('Q_01', 'TOP_01', 'MCQ', 'EASY', '1 + 1 = ?', '', 1.0, 0, 1)"
            )
            legacy.execSQL(
                "INSERT INTO question_options (id, questionId, text, isCorrect, sortOrder) " +
                    "VALUES ('OPT_A', 'Q_01', '2', 1, 0)"
            )
            legacy.execSQL("PRAGMA user_version = 1")
        } finally {
            legacy.close()
        }

        val migrated = Room.databaseBuilder(context, LearningDatabase::class.java, databaseName)
            .addMigrations(LearningDatabase.MIGRATION_1_2, LearningDatabase.MIGRATION_2_3)
            .allowMainThreadQueries()
            .build()

        try {
            val dao = migrated.learningContentDao()
            val subjects = dao.observeSubjects().first()
            val questions = dao.observeQuestions().first()
            val options = dao.observeOptions().first()
            val exams = dao.observeExams().first()
            val examQuestions = dao.observeExamQuestions().first()

            assertEquals("SUBJ_MATH", subjects.single().id)
            assertNull(subjects.single().iconName)
            assertEquals("Q_01", questions.single().id)
            assertNull(questions.single().imageUrl)
            assertNull(questions.single().estimatedTimeSeconds)
            assertEquals("OPT_A", options.single().id)
            assertNull(options.single().label)
            assertNull(options.single().imageUrl)
            assertEquals(emptyList<Any>(), exams)
            assertEquals(emptyList<Any>(), examQuestions)
        } finally {
            migrated.close()
            context.deleteDatabase(databaseName)
        }
    }

    private fun createVersionOneSchema(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE subjects (" +
                "id TEXT NOT NULL, name TEXT NOT NULL, description TEXT NOT NULL, " +
                "iconUrl TEXT NOT NULL, coverImageUrl TEXT NOT NULL, sortOrder INTEGER NOT NULL, " +
                "isActive INTEGER NOT NULL, PRIMARY KEY(id))"
        )
        db.execSQL(
            "CREATE TABLE chapters (" +
                "id TEXT NOT NULL, subjectId TEXT NOT NULL, name TEXT NOT NULL, description TEXT NOT NULL, " +
                "sortOrder INTEGER NOT NULL, isActive INTEGER NOT NULL, PRIMARY KEY(id), " +
                "FOREIGN KEY(subjectId) REFERENCES subjects(id) ON UPDATE NO ACTION ON DELETE CASCADE)"
        )
        db.execSQL("CREATE INDEX index_chapters_subjectId ON chapters(subjectId)")
        db.execSQL(
            "CREATE TABLE topics (" +
                "id TEXT NOT NULL, chapterId TEXT NOT NULL, name TEXT NOT NULL, description TEXT NOT NULL, " +
                "sortOrder INTEGER NOT NULL, isActive INTEGER NOT NULL, PRIMARY KEY(id), " +
                "FOREIGN KEY(chapterId) REFERENCES chapters(id) ON UPDATE NO ACTION ON DELETE CASCADE)"
        )
        db.execSQL("CREATE INDEX index_topics_chapterId ON topics(chapterId)")
        db.execSQL(
            "CREATE TABLE learning_questions (" +
                "id TEXT NOT NULL, topicId TEXT NOT NULL, type TEXT NOT NULL, difficulty TEXT NOT NULL, " +
                "content TEXT NOT NULL, explanation TEXT NOT NULL, points REAL NOT NULL, " +
                "sortOrder INTEGER NOT NULL, isActive INTEGER NOT NULL, PRIMARY KEY(id), " +
                "FOREIGN KEY(topicId) REFERENCES topics(id) ON UPDATE NO ACTION ON DELETE CASCADE)"
        )
        db.execSQL("CREATE INDEX index_learning_questions_topicId ON learning_questions(topicId)")
        db.execSQL(
            "CREATE TABLE question_options (" +
                "id TEXT NOT NULL, questionId TEXT NOT NULL, text TEXT NOT NULL, " +
                "isCorrect INTEGER NOT NULL, sortOrder INTEGER NOT NULL, PRIMARY KEY(id), " +
                "FOREIGN KEY(questionId) REFERENCES learning_questions(id) ON UPDATE NO ACTION ON DELETE CASCADE)"
        )
        db.execSQL("CREATE INDEX index_question_options_questionId ON question_options(questionId)")
        db.execSQL(
            "CREATE TABLE sync_metadata (" +
                "key TEXT NOT NULL, value TEXT NOT NULL, PRIMARY KEY(key))"
        )
    }
}

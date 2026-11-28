package com.htanh.appvk.data.local

import androidx.room.Database
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.RoomDatabase
import com.htanh.appvk.data.local.entity.ChapterEntity
import com.htanh.appvk.data.local.entity.LearningQuestionEntity
import com.htanh.appvk.data.local.entity.QuestionOptionEntity
import com.htanh.appvk.data.local.entity.SubjectEntity
import com.htanh.appvk.data.local.entity.SyncMetadataEntity
import com.htanh.appvk.data.local.entity.TopicEntity
import com.htanh.appvk.data.local.entity.LearningExamEntity
import com.htanh.appvk.data.local.entity.ExamQuestionEntity

@Database(
    entities = [
        SubjectEntity::class,
        ChapterEntity::class,
        TopicEntity::class,
        LearningQuestionEntity::class,
        QuestionOptionEntity::class,
        SyncMetadataEntity::class,
        LearningExamEntity::class,
        ExamQuestionEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class LearningDatabase : RoomDatabase() {
    abstract fun learningContentDao(): LearningContentDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS learning_exams (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, description TEXT NOT NULL, subjectId TEXT NOT NULL, durationMinutes INTEGER NOT NULL, type TEXT NOT NULL, totalScore REAL NOT NULL, isActive INTEGER NOT NULL, startAt TEXT, endAt TEXT, FOREIGN KEY(subjectId) REFERENCES subjects(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_learning_exams_subjectId ON learning_exams(subjectId)")
                db.execSQL("CREATE TABLE IF NOT EXISTS exam_questions (examId TEXT NOT NULL, questionId TEXT NOT NULL, sortOrder INTEGER NOT NULL, score REAL NOT NULL, PRIMARY KEY(examId, questionId), FOREIGN KEY(examId) REFERENCES learning_exams(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(questionId) REFERENCES learning_questions(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_exam_questions_questionId ON exam_questions(questionId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_exam_questions_examId_sortOrder ON exam_questions(examId, sortOrder)")
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE subjects ADD COLUMN iconName TEXT")
                db.execSQL("ALTER TABLE learning_questions ADD COLUMN imageUrl TEXT")
                db.execSQL("ALTER TABLE learning_questions ADD COLUMN estimatedTimeSeconds INTEGER")
                db.execSQL("ALTER TABLE question_options ADD COLUMN label TEXT")
                db.execSQL("ALTER TABLE question_options ADD COLUMN imageUrl TEXT")
            }
        }
    }
}

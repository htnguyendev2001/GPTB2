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

@Database(
    entities = [
        SubjectEntity::class,
        ChapterEntity::class,
        TopicEntity::class,
        LearningQuestionEntity::class,
        QuestionOptionEntity::class,
        SyncMetadataEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class LearningDatabase : RoomDatabase() {
    abstract fun learningContentDao(): LearningContentDao

    companion object {
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

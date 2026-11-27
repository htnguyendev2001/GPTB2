package com.htanh.appvk.data.local

import androidx.room.Database
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
}

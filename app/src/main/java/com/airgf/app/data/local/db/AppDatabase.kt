package com.airgf.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.airgf.app.data.local.db.dao.ConversationDao
import com.airgf.app.data.local.db.dao.GfConfigDao
import com.airgf.app.data.local.db.dao.MessageDao
import com.airgf.app.data.local.db.entity.ConversationEntity
import com.airgf.app.data.local.db.entity.GfConfigEntity
import com.airgf.app.data.local.db.entity.MessageEntity

@Database(
    entities = [
        MessageEntity::class,
        ConversationEntity::class,
        GfConfigEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun conversationDao(): ConversationDao
    abstract fun gfConfigDao(): GfConfigDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE messages ADD COLUMN imagePath TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE messages ADD COLUMN imageDescription TEXT DEFAULT NULL")
            }
        }
    }
}

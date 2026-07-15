package com.airgf.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.airgf.app.data.local.db.dao.ConversationDao
import com.airgf.app.data.local.db.dao.GfConfigDao
import com.airgf.app.data.local.db.dao.MessageDao
import com.airgf.app.data.local.db.dao.CompanionMemoryDao
import com.airgf.app.data.local.db.dao.AiReportDao
import com.airgf.app.data.local.db.entity.ConversationEntity
import com.airgf.app.data.local.db.entity.GfConfigEntity
import com.airgf.app.data.local.db.entity.MessageEntity
import com.airgf.app.data.local.db.entity.CompanionMemoryEntity
import com.airgf.app.data.local.db.entity.AiReportEntity

@Database(
    entities = [
        MessageEntity::class,
        ConversationEntity::class,
        GfConfigEntity::class,
        CompanionMemoryEntity::class,
        AiReportEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun conversationDao(): ConversationDao
    abstract fun gfConfigDao(): GfConfigDao
    abstract fun companionMemoryDao(): CompanionMemoryDao
    abstract fun aiReportDao(): AiReportDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE messages ADD COLUMN imagePath TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE messages ADD COLUMN imageDescription TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE gf_config ADD COLUMN presentation TEXT NOT NULL DEFAULT 'FEMININE'")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS companion_memories (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        category TEXT NOT NULL,
                        content TEXT NOT NULL,
                        normalizedContent TEXT NOT NULL,
                        sourceMessageId INTEGER,
                        confidence REAL NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        lastAccessedAt INTEGER,
                        state TEXT NOT NULL,
                        pinned INTEGER NOT NULL,
                        sensitive INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_companion_memories_state ON companion_memories(state)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_companion_memories_category ON companion_memories(category)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_companion_memories_sourceMessageId ON companion_memories(sourceMessageId)")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS ai_reports (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        messageId INTEGER NOT NULL,
                        reason TEXT NOT NULL,
                        content TEXT NOT NULL,
                        context TEXT,
                        createdAt INTEGER NOT NULL,
                        status TEXT NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_ai_reports_status ON ai_reports(status)")
            }
        }
    }
}

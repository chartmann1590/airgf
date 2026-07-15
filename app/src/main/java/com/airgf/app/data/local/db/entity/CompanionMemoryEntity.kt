package com.airgf.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "companion_memories",
    indices = [Index("state"), Index("category"), Index("sourceMessageId")],
)
data class CompanionMemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,
    val content: String,
    val normalizedContent: String,
    val sourceMessageId: Long?,
    val confidence: Float,
    val createdAt: Long,
    val updatedAt: Long,
    val lastAccessedAt: Long?,
    val state: String,
    val pinned: Boolean,
    val sensitive: Boolean,
)

package com.airgf.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "ai_reports", indices = [Index("status")])
data class AiReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val messageId: Long,
    val reason: String,
    val content: String,
    val context: String?,
    val createdAt: Long,
    val status: String = "PENDING",
)

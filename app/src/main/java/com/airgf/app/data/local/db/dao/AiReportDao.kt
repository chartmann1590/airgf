package com.airgf.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.airgf.app.data.local.db.entity.AiReportEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AiReportDao {
    @Insert suspend fun insert(report: AiReportEntity): Long
    @Query("SELECT * FROM ai_reports ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<AiReportEntity>>
    @Query("UPDATE ai_reports SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)
    @Query("DELETE FROM ai_reports") suspend fun deleteAll()
}

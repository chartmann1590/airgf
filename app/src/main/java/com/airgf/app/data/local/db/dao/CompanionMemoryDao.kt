package com.airgf.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.airgf.app.data.local.db.entity.CompanionMemoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CompanionMemoryDao {
    @Query("SELECT * FROM companion_memories WHERE state != 'DELETED' ORDER BY pinned DESC, updatedAt DESC")
    fun observeAll(): Flow<List<CompanionMemoryEntity>>

    @Query("SELECT * FROM companion_memories WHERE state = 'APPROVED' ORDER BY pinned DESC, updatedAt DESC")
    suspend fun getApproved(): List<CompanionMemoryEntity>

    @Query("SELECT * FROM companion_memories WHERE normalizedContent = :normalized AND state != 'DELETED' LIMIT 1")
    suspend fun findDuplicate(normalized: String): CompanionMemoryEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(memory: CompanionMemoryEntity): Long

    @Update suspend fun update(memory: CompanionMemoryEntity)

    @Query("UPDATE companion_memories SET state = :state, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateState(id: Long, state: String, updatedAt: Long)

    @Query("DELETE FROM companion_memories") suspend fun deleteAll()
}

package com.airgf.app.domain.repository

import com.airgf.app.domain.model.CompanionMemory
import com.airgf.app.domain.model.MemoryState
import kotlinx.coroutines.flow.Flow

interface MemoryRepository {
    fun observeMemories(): Flow<List<CompanionMemory>>
    suspend fun getApproved(): List<CompanionMemory>
    suspend fun suggest(memory: CompanionMemory): Long
    suspend fun setState(id: Long, state: MemoryState)
    suspend fun deleteAll()
}

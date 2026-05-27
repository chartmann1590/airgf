package com.airgf.app.domain.repository

import com.airgf.app.data.model.DownloadState
import kotlinx.coroutines.flow.Flow

interface ModelRepository {
    suspend fun isModelDownloaded(): Boolean
    suspend fun getModelPath(): String?
    suspend fun setModelDownloaded(path: String)
    suspend fun clearModel()
    fun isNetworkAvailable(): Boolean
    fun hasSufficientStorage(): Boolean
    fun downloadModel(): Flow<DownloadState>
}

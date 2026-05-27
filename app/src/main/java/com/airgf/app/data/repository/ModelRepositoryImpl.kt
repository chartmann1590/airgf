package com.airgf.app.data.repository

import android.content.Context
import com.airgf.app.core.util.NetworkUtil
import com.airgf.app.core.util.StorageUtil
import com.airgf.app.data.local.datastore.UserPreferences
import com.airgf.app.data.model.DownloadState
import com.airgf.app.domain.repository.ModelRepository
import com.airgf.app.llm.ModelConstants
import com.airgf.app.llm.ModelDownloader
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferences: UserPreferences,
    private val modelDownloader: ModelDownloader,
) : ModelRepository {

    override suspend fun isModelDownloaded(): Boolean {
        val path = getModelPath()
        return path != null && File(path).exists()
    }

    override suspend fun getModelPath(): String? {
        val storedPath = userPreferences.modelFilePath.first()
        if (storedPath != null && File(storedPath).exists()) {
            return storedPath
        }
        return modelDownloader.getModelPath()
    }

    override suspend fun setModelDownloaded(path: String) {
        userPreferences.setModelDownloaded(downloaded = true, path = path)
    }

    override suspend fun clearModel() {
        modelDownloader.deleteModel()
        userPreferences.setModelDownloaded(downloaded = false, path = null)
    }

    override fun isNetworkAvailable(): Boolean = NetworkUtil.isNetworkAvailable(context)

    override fun hasSufficientStorage(): Boolean =
        StorageUtil.hasMinimumFreeSpace(context, ModelConstants.MIN_FREE_SPACE_BYTES)

    override fun downloadModel(): Flow<DownloadState> = flow {
        modelDownloader.download().collect { state ->
            if (state is DownloadState.Complete) {
                setModelDownloaded(state.filePath)
            }
            emit(state)
        }
    }
}

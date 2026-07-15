package com.airgf.app.llm

import android.content.Context
import com.airgf.app.core.di.DownloadOkHttp
import com.airgf.app.data.model.DownloadState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.ensureActive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

@Singleton
class ModelDownloader @Inject constructor(
    @ApplicationContext private val context: Context,
    @DownloadOkHttp private val client: OkHttpClient,
) {
    fun download(variant: ModelVariant): Flow<DownloadState> = flow {
        val destFile = File(context.filesDir, variant.filename)
        if (destFile.exists() && isValidModelFile(destFile)) {
            emit(DownloadState.Complete(destFile.absolutePath))
            return@flow
        }

        val partFile = File(context.filesDir, "${variant.filename}.part")
        if (partFile.exists()) {
            partFile.delete()
        }

        val request = Request.Builder().url(variant.url).build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                emit(DownloadState.Error("Download failed: HTTP ${response.code}"))
                return@flow
            }

            val body = response.body
            if (body == null) {
                emit(DownloadState.Error("Download failed: empty response"))
                return@flow
            }

            val totalBytes = when (val length = body.contentLength()) {
                -1L -> variant.expectedSizeBytes
                else -> length
            }
            var downloadedBytes = 0L

            body.byteStream().use { input ->
                partFile.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        coroutineContext.ensureActive()
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        emit(DownloadState.Progress(downloadedBytes, totalBytes))
                    }
                }
            }

            if (!partFile.renameTo(destFile)) {
                partFile.delete()
                emit(DownloadState.Error("Download failed: could not save model file"))
                return@flow
            }

            if (!isValidModelFile(destFile)) {
                destFile.delete()
                emit(DownloadState.Error("Download failed: invalid model file"))
                return@flow
            }

            emit(DownloadState.Complete(destFile.absolutePath))
        } catch (e: IOException) {
            partFile.delete()
            emit(DownloadState.Error(e.message ?: "Download failed"))
        } catch (e: Exception) {
            partFile.delete()
            emit(DownloadState.Error(e.message ?: "Download failed"))
        }
    }.flowOn(Dispatchers.IO)

    fun getModelPath(variant: ModelVariant): String? {
        val file = File(context.filesDir, variant.filename)
        return if (file.exists() && isValidModelFile(file)) file.absolutePath else null
    }

    fun deleteModel() {
        ModelVariant.entries.forEach { variant ->
            File(context.filesDir, variant.filename).delete()
            File(context.filesDir, "${variant.filename}.part").delete()
        }
    }

    private fun isValidModelFile(file: File): Boolean {
        return file.exists() &&
            file.length() > 0 &&
            file.name.endsWith(".litertlm", ignoreCase = true)
    }
}

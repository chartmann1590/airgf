package com.airgf.app.imagegen

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
import android.util.Log
import java.io.File
import java.io.IOException
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

@Singleton
class ImageGenModelDownloader @Inject constructor(
    @ApplicationContext private val context: Context,
    @DownloadOkHttp private val client: OkHttpClient,
) {
    private val modelDir get() = File(context.filesDir, ImageGenConstants.MODEL_DIR)
    private val extractedDir get() = File(modelDir, EXTRACTED_DIR_NAME)

    fun download(): Flow<DownloadState> = flow {
        modelDir.mkdirs()
        val destFile = File(modelDir, ImageGenConstants.MODEL_FILENAME)
        if (destFile.exists() && isValidModelFile(destFile) && isExtracted()) {
            emit(DownloadState.Complete(extractedDir.absolutePath))
            return@flow
        }
        if (destFile.exists() && isValidModelFile(destFile) && !isExtracted()) {
            val extracted = unzipModel(destFile)
            if (extracted != null) {
                emit(DownloadState.Complete(extracted))
                return@flow
            }
        }

        val partFile = File(modelDir, "${ImageGenConstants.MODEL_FILENAME}.part")
        if (partFile.exists()) partFile.delete()

        val request = Request.Builder().url(ImageGenConstants.MODEL_URL).build()
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
                -1L -> ImageGenConstants.EXPECTED_SIZE_BYTES
                else -> length
            }
            var downloadedBytes = 0L
            var lastEmitTime = 0L
            body.byteStream().use { input ->
                partFile.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        coroutineContext.ensureActive()
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        val now = System.currentTimeMillis()
                        if (now - lastEmitTime >= 200) {
                            lastEmitTime = now
                            emit(DownloadState.Progress(downloadedBytes, totalBytes))
                        }
                    }
                }
            }
            emit(DownloadState.Progress(downloadedBytes, totalBytes))
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
            val extracted = unzipModel(destFile)
            if (extracted != null) {
                emit(DownloadState.Complete(extracted))
            } else {
                emit(DownloadState.Error("Failed to extract model files"))
            }
        } catch (e: IOException) {
            partFile.delete()
            emit(DownloadState.Error(e.message ?: "Download failed"))
        } catch (e: Exception) {
            partFile.delete()
            emit(DownloadState.Error(e.message ?: "Download failed"))
        }
    }.flowOn(Dispatchers.IO)

    fun getModelPath(): String? {
        if (isExtracted()) return extractedDir.absolutePath
        val file = File(modelDir, ImageGenConstants.MODEL_FILENAME)
        return if (file.exists() && isValidModelFile(file)) file.absolutePath else null
    }

    fun deleteModel() {
        if (modelDir.exists()) modelDir.deleteRecursively()
    }

    private fun isExtracted(): Boolean {
        return extractedDir.exists() && extractedDir.isDirectory &&
            extractedDir.listFiles()?.isNotEmpty() == true
    }

    private suspend fun unzipModel(zipFile: File): String? {
        return try {
            if (extractedDir.exists()) extractedDir.deleteRecursively()
            extractedDir.mkdirs()
            ZipInputStream(zipFile.inputStream()).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    coroutineContext.ensureActive()
                    val outFile = File(extractedDir, entry.name)
                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        outFile.outputStream().use { output ->
                            val buffer = ByteArray(8192)
                            var len: Int
                            while (zis.read(buffer).also { len = it } > 0) {
                                output.write(buffer, 0, len)
                            }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            Log.d(TAG, "Model extracted to ${extractedDir.absolutePath}")
            extractedDir.listFiles()?.forEach { Log.d(TAG, "  - ${it.name}") }
            extractedDir.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unzip model", e)
            if (extractedDir.exists()) extractedDir.deleteRecursively()
            null
        }
    }

    private fun isValidModelFile(file: File): Boolean {
        return file.exists() && file.length() > 0
    }

    companion object {
        private const val TAG = "ImageGenModelDownloader"
        private const val EXTRACTED_DIR_NAME = "extracted"
    }
}
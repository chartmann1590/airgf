package com.airgf.app.data.repository

import android.content.Context
import com.airgf.app.core.di.DownloadOkHttp
import com.airgf.app.data.model.AvatarAvailability
import com.airgf.app.data.model.AvatarModelSource
import com.airgf.app.data.model.DownloadState
import com.airgf.app.domain.model.AvatarDeliveryMode
import com.airgf.app.domain.model.VisualTemplate
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

@Singleton
class AvatarAssetRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @DownloadOkHttp private val client: OkHttpClient,
) {
    private val downloadDir: File
        get() = File(context.filesDir, "avatars").apply { mkdirs() }

    fun resolveAvailability(template: VisualTemplate): AvatarAvailability {
        if (!template.supportsSceneView) {
            return AvatarAvailability(
                source = AvatarModelSource.ProceduralFallback(
                    reason = "This GLB skeleton exceeds the renderer bone limit.",
                ),
                canDownload = false,
                statusMessage = "3D preview is disabled for this avatar because the model has too many bones for the renderer.",
            )
        }

        downloadedModelFile(template)?.let { file ->
            return AvatarAvailability(
                source = AvatarModelSource.DownloadedFile(file.absolutePath),
                canDownload = false,
                statusMessage = "Downloaded 3D avatar ready.",
            )
        }

        if (assetExists(template.modelAssetPath)) {
            return AvatarAvailability(
                source = AvatarModelSource.BundledAsset(template.modelAssetPath),
                canDownload = false,
                statusMessage = null,
            )
        }

        if (template.deliveryMode == AvatarDeliveryMode.ON_DEMAND && template.remoteModelUrl != null) {
            return AvatarAvailability(
                source = AvatarModelSource.ProceduralFallback("3D model not downloaded yet."),
                canDownload = true,
                statusMessage = "Download the high-detail 3D model or use the built-in fallback.",
            )
        }

        return AvatarAvailability(
            source = AvatarModelSource.ProceduralFallback(
                reason = if (template.deliveryMode == AvatarDeliveryMode.ON_DEMAND) {
                    "No remote model URL is configured yet."
                } else {
                    "Bundled GLB is not present in this checkout."
                },
            ),
            canDownload = false,
            statusMessage = "3D model could not be loaded, using fallback for now.",
        )
    }

    fun resolveThumbnailAssetPath(template: VisualTemplate): String? =
        template.thumbnailAssetPath.takeIf(::assetExists)

    fun downloadModel(template: VisualTemplate): Flow<DownloadState> = flow {
        if (template.deliveryMode != AvatarDeliveryMode.ON_DEMAND) {
            emit(DownloadState.Error("This avatar is bundled with the app."))
            return@flow
        }

        val url = template.remoteModelUrl
        if (url.isNullOrBlank()) {
            emit(DownloadState.Error("This avatar does not have a download URL configured yet."))
            return@flow
        }

        val destFile = File(downloadDir, template.downloadFileName)
        if (destFile.exists() && destFile.length() > 0L) {
            emit(DownloadState.Complete(destFile.absolutePath))
            return@flow
        }

        val partFile = File(downloadDir, "${template.downloadFileName}.part")
        if (partFile.exists()) {
            partFile.delete()
        }

        val request = Request.Builder().url(url).build()

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

            val totalBytes = body.contentLength().takeIf { it > 0 } ?: template.expectedSizeBytes
            var downloadedBytes = 0L

            body.byteStream().use { input ->
                partFile.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
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
                emit(DownloadState.Error("Download failed: could not move model file into place"))
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

    fun deleteDownloadedModel(template: VisualTemplate) {
        File(downloadDir, template.downloadFileName).delete()
        File(downloadDir, "${template.downloadFileName}.part").delete()
    }

    private fun downloadedModelFile(template: VisualTemplate): File? {
        val file = File(downloadDir, template.downloadFileName)
        return file.takeIf { it.exists() && it.length() > 0L }
    }

    private fun assetExists(assetPath: String): Boolean =
        runCatching {
            context.assets.open(assetPath).use { }
            true
        }.getOrDefault(false)
}

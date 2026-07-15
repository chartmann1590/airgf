package com.airgf.app.llm

import android.content.Context
import android.graphics.Bitmap
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.imagedescription.ImageDescription
import com.google.mlkit.genai.imagedescription.ImageDescriptionRequest
import com.google.mlkit.genai.imagedescription.ImageDescriberOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.Executor
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@Singleton
class ImageDescriptionService @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val client = ImageDescription.getClient(
        ImageDescriberOptions.builder(context).build(),
    )

    suspend fun describe(bitmap: Bitmap): String? = runCatching {
        val status = client.checkFeatureStatus().await()
        if (status == FeatureStatus.UNAVAILABLE) return null

        val request = ImageDescriptionRequest.builder(bitmap).build()
        client.runInference(request).await().description.trim().ifBlank { null }
    }.getOrNull()
}

private suspend fun <T> ListenableFuture<T>.await(): T = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { cancel(true) }
    addListener(
        {
            runCatching { get() }
                .onSuccess(continuation::resume)
                .onFailure { continuation.cancel(it) }
        },
        DIRECT_EXECUTOR,
    )
}

private val DIRECT_EXECUTOR = Executor { command -> command.run() }

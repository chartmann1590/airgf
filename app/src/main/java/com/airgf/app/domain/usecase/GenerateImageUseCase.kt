package com.airgf.app.domain.usecase

import com.airgf.app.core.util.ImageStorageUtil
import com.airgf.app.imagegen.ImageGenerator
import javax.inject.Inject

sealed class GenerateImageEvent {
    data class Generating(val prompt: String) : GenerateImageEvent()
    data class Complete(val filePath: String) : GenerateImageEvent()
    data class Error(val message: String) : GenerateImageEvent()
}

class GenerateImageUseCase @Inject constructor(
    private val imageGenerator: ImageGenerator,
    private val imageStorageUtil: ImageStorageUtil,
) {
    suspend operator fun invoke(prompt: String): GenerateImageEvent {
        var bitmap: android.graphics.Bitmap? = null
        return try {
            bitmap = imageGenerator.generate(prompt)
            val saved = imageStorageUtil.saveGeneratedBitmap(bitmap, prompt)
            GenerateImageEvent.Complete(saved.path)
        } catch (e: Throwable) {
            GenerateImageEvent.Error(e.message ?: "Failed to generate image")
        } finally {
            bitmap?.recycle()
            imageGenerator.release()
        }
    }
}

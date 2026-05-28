package com.airgf.app.imagegen

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.airgf.app.domain.repository.ImageGenRepository
import com.google.mediapipe.framework.image.BitmapExtractor
import com.google.mediapipe.tasks.vision.imagegenerator.ImageGenerator as MpImageGenerator
import com.google.mediapipe.tasks.vision.imagegenerator.ImageGenerator.ImageGeneratorOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageGenerator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imageGenRepository: ImageGenRepository,
) {
    private var mpGenerator: MpImageGenerator? = null
    private var modelPath: String? = null

    sealed class GenState {
        data object Uninitialized : GenState()
        data object Loading : GenState()
        data object Ready : GenState()
        data class Error(val message: String) : GenState()
    }

    private var _state: GenState = GenState.Uninitialized
    val state: GenState get() = _state

    fun isAvailable(): Boolean = mpGenerator != null

    suspend fun ensureInitialized() {
        if (mpGenerator != null) return
        val path = runCatching { imageGenRepository.getModelPath() }.getOrNull() ?: return
        initialize(path)
    }

    suspend fun initialize(path: String) {
        if (mpGenerator != null && modelPath == path) return
        release()
        _state = GenState.Loading
        try {
            val dir = File(path)
            if (!dir.exists()) {
                _state = GenState.Error("Model directory not found at: $path")
                return
            }
            val options = ImageGeneratorOptions.builder()
                .setImageGeneratorModelDirectory(path)
                .setModelType(ImageGeneratorOptions.ModelType.SD_1)
                .build()
            mpGenerator = MpImageGenerator.createFromOptions(context, options)
            modelPath = path
            _state = GenState.Ready
            Log.d(TAG, "ImageGenerator initialized with path: $path")
        } catch (e: Exception) {
            _state = GenState.Error(e.message ?: "Failed to initialize image generator")
            Log.e(TAG, "Failed to initialize ImageGenerator", e)
        }
    }

    suspend fun generate(prompt: String, steps: Int = 20, seed: Int = -1): Bitmap =
        withContext(Dispatchers.Default) {
            val generator = mpGenerator
            if (generator == null) {
                ensureInitialized()
                val retryGen = mpGenerator
                    ?: throw IllegalStateException("Image generator not initialized")
                generateWithGenerator(retryGen, prompt, steps, seed)
            } else {
                generateWithGenerator(generator, prompt, steps, seed)
            }
        }

    private fun generateWithGenerator(
        generator: MpImageGenerator,
        prompt: String,
        steps: Int,
        seed: Int,
    ): Bitmap {
        Log.d(TAG, "Generating image for prompt: $prompt, steps: $steps, seed: $seed")
        val actualSeed = if (seed < 0) (System.currentTimeMillis() % Int.MAX_VALUE).toInt() else seed
        val result = generator.generate(prompt, steps, actualSeed)
        val bitmap = BitmapExtractor.extract(result?.generatedImage())
        Log.d(TAG, "Generated bitmap: ${bitmap.width}x${bitmap.height}")
        return bitmap
    }

    fun release() {
        runCatching { mpGenerator?.close() }
        mpGenerator = null
        modelPath = null
        _state = GenState.Uninitialized
    }

    companion object {
        private const val TAG = "ImageGenerator"
    }
}

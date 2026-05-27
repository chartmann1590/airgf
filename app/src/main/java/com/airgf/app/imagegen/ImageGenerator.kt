package com.airgf.app.imagegen

import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipFile
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageGenerator @Inject constructor() {
    private var isLoaded = false
    private var modelPath: String? = null

    sealed class GenState {
        data object Uninitialized : GenState()
        data object Loading : GenState()
        data object Ready : GenState()
        data class Error(val message: String) : GenState()
    }

    private var _state: GenState = GenState.Uninitialized
    val state: GenState get() = _state

    fun isAvailable(): Boolean = modelPath != null

    suspend fun initialize(path: String) {
        if (isLoaded && modelPath == path) return
        _state = GenState.Loading
        try {
            val file = File(path)
            if (!file.exists()) {
                _state = GenState.Error("Model file not found")
                return
            }
            modelPath = path
            isLoaded = true
            _state = GenState.Ready
        } catch (e: Exception) {
            _state = GenState.Error(e.message ?: "Failed to initialize image generator")
        }
    }

    suspend fun generate(prompt: String, steps: Int = 20, seed: Int = -1): Bitmap =
        withContext(Dispatchers.Default) {
            if (!isLoaded) throw IllegalStateException("Image generator not initialized")
            Log.d(TAG, "Generating image for prompt: $prompt")
            Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        }

    fun release() {
        isLoaded = false
        modelPath = null
        _state = GenState.Uninitialized
    }

    companion object {
        private const val TAG = "ImageGenerator"
    }
}
package com.airgf.app.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
interface ImageCleanup {
    fun cleanupAllImages()
}

class ImageStorageUtil @Inject constructor(
    @ApplicationContext private val context: Context,
) : ImageCleanup {

    private val userImagesDir get() = File(context.filesDir, USER_IMAGES_DIR)
    private val generatedImagesDir get() = File(context.filesDir, GENERATED_IMAGES_DIR)

    fun ensureDirsExist() {
        userImagesDir.mkdirs()
        generatedImagesDir.mkdirs()
    }

    fun savePickedImageFromUri(uri: android.net.Uri, description: String? = null): SavedImage? {
        ensureDirsExist()
        val filename = "${UUID.randomUUID()}.$IMAGE_EXTENSION"
        val destFile = File(userImagesDir, filename)
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            SavedImage(destFile.absolutePath, description)
        } catch (_: Exception) {
            null
        }
    }

    fun saveGeneratedBitmap(bitmap: Bitmap, prompt: String? = null): SavedImage {
        ensureDirsExist()
        val filename = "${UUID.randomUUID()}.$IMAGE_EXTENSION"
        val destFile = File(generatedImagesDir, filename)
        FileOutputStream(destFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return SavedImage(destFile.absolutePath, prompt)
    }

    fun getImageFile(path: String): File? {
        val file = File(path)
        return if (file.exists()) file else null
    }

    fun loadImageBitmap(path: String): Bitmap? {
        val file = getImageFile(path) ?: return null
        return BitmapFactory.decodeFile(file.absolutePath)
    }

    fun calculateImageStorageBytes(): Long {
        val dirs = listOf(userImagesDir, generatedImagesDir)
        return dirs.sumOf { dir -> dir.walkTopDown().filter { it.isFile }.sumOf { it.length() } }
    }

    fun calculateImageStorageFormatted(): String = FileUtil.formatFileSize(calculateImageStorageBytes())

    override fun cleanupAllImages() {
        cleanupUserImages()
        cleanupGeneratedImages()
    }

    fun cleanupUserImages() {
        userImagesDir.walkTopDown().filter { it.isFile }.forEach { it.delete() }
    }

    fun cleanupGeneratedImages() {
        generatedImagesDir.walkTopDown().filter { it.isFile }.forEach { it.delete() }
    }

    fun deleteImage(path: String) {
        val file = File(path)
        if (file.exists()) file.delete()
    }

    data class SavedImage(
        val path: String,
        val description: String?,
    )

    companion object {
        const val USER_IMAGES_DIR = "images/user"
        const val GENERATED_IMAGES_DIR = "images/generated"
        private const val IMAGE_EXTENSION = "png"
    }
}

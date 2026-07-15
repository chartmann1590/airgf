package com.airgf.app.data.feedback

import android.content.Context
import android.net.Uri
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageUploadHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun uriToBase64(uri: Uri): String {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Cannot open image URI")

        val bytes = inputStream.use { stream ->
            val outputStream = ByteArrayOutputStream()
            stream.copyTo(outputStream)
            outputStream.toByteArray()
        }

        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}

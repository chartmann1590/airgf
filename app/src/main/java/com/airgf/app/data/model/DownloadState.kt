package com.airgf.app.data.model

sealed class DownloadState {
    data object Idle : DownloadState()

    data class Progress(val bytesDownloaded: Long, val totalBytes: Long) : DownloadState() {
        val percent: Float
            get() = if (totalBytes > 0) bytesDownloaded.toFloat() / totalBytes else 0f
    }

    data class Complete(val filePath: String) : DownloadState()

    data class Error(val message: String) : DownloadState()
}

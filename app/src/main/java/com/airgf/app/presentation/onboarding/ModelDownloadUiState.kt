package com.airgf.app.presentation.onboarding

import com.airgf.app.llm.ModelConstants

enum class DownloadPhase {
    Checking,
    Downloading,
    Complete,
    Error,
}

enum class DownloadErrorType {
    NoNetwork,
    InsufficientStorage,
    DownloadFailed,
}

data class ModelDownloadUiState(
    val phase: DownloadPhase = DownloadPhase.Checking,
    val errorType: DownloadErrorType? = null,
    val errorMessage: String? = null,
    val percent: Float = 0f,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = ModelConstants.EXPECTED_SIZE_BYTES,
    val speedBytesPerSec: Long = 0L,
    val etaSeconds: Long? = null,
    val tipIndex: Int = 0,
)

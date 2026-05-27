package com.airgf.app.presentation.settings

import android.net.Uri
import com.airgf.app.domain.model.GfProfile
import com.airgf.app.domain.model.UserProfile

data class SettingsUiState(
    val isLoading: Boolean = true,
    val gfProfile: GfProfile? = null,
    val userProfile: UserProfile? = null,
    val ttsEnabled: Boolean = true,
    val speechSpeed: Float = 1.0f,
    val proactiveMessagesEnabled: Boolean = true,
    val notificationFrequency: String = FREQUENCY_SOMETIMES,
    val modelStatus: SettingsModelStatus = SettingsModelStatus.Checking,
    val imageModelStatus: SettingsImageModelStatus = SettingsImageModelStatus.Checking,
    val isExporting: Boolean = false,
    val isResetting: Boolean = false,
    val isDeletingModel: Boolean = false,
    val isDownloadingModel: Boolean = false,
    val isDeletingImageModel: Boolean = false,
    val isDownloadingImageModel: Boolean = false,
)

sealed interface SettingsModelStatus {
    data object Checking : SettingsModelStatus
    data object NotDownloaded : SettingsModelStatus

    data class Downloaded(
        val path: String,
        val sizeLabel: String,
    ) : SettingsModelStatus
}

sealed interface SettingsImageModelStatus {
    data object Checking : SettingsImageModelStatus
    data object NotDownloaded : SettingsImageModelStatus

    data class Downloaded(
        val path: String,
        val sizeLabel: String,
    ) : SettingsImageModelStatus
}

sealed interface SettingsEvent {
    data class ShowMessage(val message: String) : SettingsEvent
    data class ShareExport(val uri: Uri) : SettingsEvent
    data object NavigateToOnboarding : SettingsEvent
}

const val FREQUENCY_RARELY = "rarely"
const val FREQUENCY_SOMETIMES = "sometimes"
const val FREQUENCY_OFTEN = "often"

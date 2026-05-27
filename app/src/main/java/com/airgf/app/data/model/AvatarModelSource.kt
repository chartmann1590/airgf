package com.airgf.app.data.model

sealed interface AvatarModelSource {
    data class BundledAsset(val assetPath: String) : AvatarModelSource

    data class DownloadedFile(val absolutePath: String) : AvatarModelSource

    data class ProceduralFallback(val reason: String) : AvatarModelSource
}

data class AvatarAvailability(
    val source: AvatarModelSource,
    val canDownload: Boolean,
    val statusMessage: String? = null,
)

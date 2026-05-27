package com.airgf.app.domain.model

enum class AvatarDeliveryMode(val label: String) {
    BUNDLED("Bundled"),
    ON_DEMAND("Download"),
}

data class AvatarFeatureSet(
    val blendShapeOrder: List<String> = emptyList(),
    val idleAnimationNames: List<String> = emptyList(),
    val supportsBlink: Boolean = true,
    val supportsHeadTilt: Boolean = true,
) {
    val supportsMorphTargets: Boolean
        get() = blendShapeOrder.isNotEmpty()
}

data class AvatarFallbackPalette(
    val skinColor: Long,
    val hairColor: Long,
    val accentColor: Long,
    val backdropStartColor: Long,
    val backdropEndColor: Long,
)

package com.airgf.app.domain.model

data class GfProfile(
    val name: String,
    val visualTemplate: VisualTemplate,
    val personalityTraits: List<PersonalityTrait>,
    val relationshipType: RelationshipType,
    val voiceOption: VoiceOption,
    val spicyModeEnabled: Boolean,
    val customPromptAdditions: String? = null,
    val presentation: CompanionPresentation = CompanionPresentation.FEMININE,
)

enum class CompanionPresentation(val relationshipNoun: String) {
    FEMININE("girlfriend"),
    MASCULINE("boyfriend"),
    NEUTRAL("partner"),
}

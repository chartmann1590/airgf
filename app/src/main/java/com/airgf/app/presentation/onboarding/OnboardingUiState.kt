package com.airgf.app.presentation.onboarding

import com.airgf.app.domain.model.CommunicationStyle
import com.airgf.app.domain.model.PersonalityTrait
import com.airgf.app.domain.model.RelationshipType
import com.airgf.app.domain.model.VisualTemplate
import com.airgf.app.domain.model.VoiceOption

data class OnboardingUiState(
    val userName: String = "",
    val userAge: String = "",
    val interests: Set<String> = emptySet(),
    val communicationStyle: CommunicationStyle = CommunicationStyle.CASUAL,
    val gfName: String = "",
    val visualTemplate: VisualTemplate = VisualTemplate.ARIADNA,
    val personalityTraits: List<PersonalityTrait> = emptyList(),
    val relationshipType: RelationshipType = RelationshipType.CASUAL,
    val voiceOption: VoiceOption = VoiceOption.SOFT,
    val isSaving: Boolean = false,
    val saveError: String? = null,
)

val INTEREST_OPTIONS = listOf(
    "Gaming", "Music", "Movies", "Reading", "Fitness",
    "Art", "Travel", "Food", "Technology", "Nature",
)

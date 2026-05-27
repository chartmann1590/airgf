package com.airgf.app.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airgf.app.domain.model.CommunicationStyle
import com.airgf.app.domain.model.GfProfile
import com.airgf.app.domain.model.PersonalityTrait
import com.airgf.app.domain.model.RelationshipType
import com.airgf.app.domain.model.UserProfile
import com.airgf.app.domain.model.VisualTemplate
import com.airgf.app.domain.model.VoiceOption
import com.airgf.app.domain.repository.GfConfigRepository
import com.airgf.app.domain.repository.UserRepository
import com.airgf.app.notification.ProactiveMessageScheduler
import com.airgf.app.tts.TtsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val gfConfigRepository: GfConfigRepository,
    private val proactiveScheduler: ProactiveMessageScheduler,
    private val ttsManager: TtsManager,
) : ViewModel() {

    init {
        ttsManager.ensureInitialized()
    }

    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    fun updateUserName(name: String) {
        _state.update { it.copy(userName = name) }
    }

    fun updateUserAge(age: String) {
        _state.update { it.copy(userAge = age.filter { c -> c.isDigit() }.take(3)) }
    }

    fun toggleInterest(interest: String) {
        _state.update { current ->
            val updated = current.interests.toMutableSet()
            if (updated.contains(interest)) {
                updated.remove(interest)
            } else {
                updated.add(interest)
            }
            current.copy(interests = updated)
        }
    }

    fun updateCommunicationStyle(style: CommunicationStyle) {
        _state.update { it.copy(communicationStyle = style) }
    }

    fun updateGfName(name: String) {
        _state.update { it.copy(gfName = name) }
    }

    fun updateVisualTemplate(template: VisualTemplate) {
        _state.update { it.copy(visualTemplate = template) }
    }

    fun togglePersonalityTrait(trait: PersonalityTrait) {
        _state.update { current ->
            val traits = current.personalityTraits.toMutableList()
            if (traits.contains(trait)) {
                traits.remove(trait)
            } else if (traits.size < 3) {
                traits.add(trait)
            }
            current.copy(personalityTraits = traits)
        }
    }

    fun updateRelationshipType(type: RelationshipType) {
        _state.update { it.copy(relationshipType = type) }
    }

    fun updateVoiceOption(option: VoiceOption) {
        _state.update { it.copy(voiceOption = option) }
    }

    fun previewVoice(option: VoiceOption) {
        updateVoiceOption(option)
        ttsManager.previewVoice(option)
    }

    override fun onCleared() {
        ttsManager.stop()
        super.onCleared()
    }

    fun canProceedFrom(step: OnboardingStep): Boolean {
        val s = _state.value
        return when (step) {
            OnboardingStep.USER_PROFILE -> {
                s.userName.isNotBlank() &&
                    (s.userAge.toIntOrNull() ?: 0) >= 18 &&
                    s.interests.size >= 3
            }
            OnboardingStep.GF_CUSTOMIZATION -> {
                s.gfName.isNotBlank() &&
                    s.personalityTraits.isNotEmpty() &&
                    s.personalityTraits.size <= 3
            }
            OnboardingStep.VOICE_SELECTION -> true
            OnboardingStep.MODEL_DOWNLOAD -> true
            OnboardingStep.IMAGE_MODEL_DOWNLOAD -> true
            OnboardingStep.SETUP_COMPLETE -> !s.isSaving
        }
    }

    fun saveAndComplete(
        enableProactiveMessages: Boolean,
        onSuccess: () -> Unit,
    ) {
        val s = _state.value
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, saveError = null) }
            try {
                userRepository.saveProfile(
                    UserProfile(
                        name = s.userName.trim(),
                        age = s.userAge.toInt(),
                        interests = s.interests.toList(),
                        communicationStyle = s.communicationStyle,
                    ),
                )
                userRepository.setProactiveMessagesEnabled(enableProactiveMessages)
                gfConfigRepository.saveProfile(
                    GfProfile(
                        name = s.gfName.trim(),
                        visualTemplate = s.visualTemplate,
                        personalityTraits = s.personalityTraits,
                        relationshipType = s.relationshipType,
                        voiceOption = s.voiceOption,
                        spicyModeEnabled = false,
                    ),
                )
                userRepository.setOnboardingComplete(true)
                proactiveScheduler.sync()
                _state.update { it.copy(isSaving = false) }
                onSuccess()
            } catch (e: Exception) {
                _state.update {
                    it.copy(isSaving = false, saveError = e.message ?: "Failed to save")
                }
            }
        }
    }
}

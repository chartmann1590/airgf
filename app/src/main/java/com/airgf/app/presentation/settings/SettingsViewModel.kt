package com.airgf.app.presentation.settings

import android.app.Activity
import com.airgf.app.ads.ConsentManager
import com.airgf.app.ads.RewardedAdManager
import com.airgf.app.data.model.DownloadState
import com.airgf.app.domain.model.GfProfile
import com.airgf.app.domain.model.PersonalityTrait
import com.airgf.app.domain.model.UserProfile
import com.airgf.app.domain.model.VisualTemplate
import com.airgf.app.domain.model.VoiceOption
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airgf.app.domain.repository.GfConfigRepository
import com.airgf.app.domain.repository.ImageGenRepository
import com.airgf.app.domain.repository.ModelRepository
import com.airgf.app.domain.repository.UserRepository
import com.airgf.app.domain.repository.MemoryRepository
import com.airgf.app.domain.model.MemoryState
import com.airgf.app.domain.model.CompanionPresentation
import com.airgf.app.llm.ModelVariant
import com.airgf.app.domain.repository.SubscriptionRepository
import com.airgf.app.domain.usecase.ExportChatUseCase
import com.airgf.app.domain.usecase.GetEffectiveSpicyModeUseCase
import com.airgf.app.domain.usecase.GrantSpicyModeCreditUseCase
import com.airgf.app.domain.usecase.ResetEverythingUseCase
import com.airgf.app.notification.ProactiveMessageScheduler
import com.airgf.app.tts.TtsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val gfConfigRepository: GfConfigRepository,
    private val userRepository: UserRepository,
    private val modelRepository: ModelRepository,
    private val imageGenRepository: ImageGenRepository,
    private val exportChatUseCase: ExportChatUseCase,
    private val resetEverythingUseCase: ResetEverythingUseCase,
    private val proactiveScheduler: ProactiveMessageScheduler,
    private val ttsManager: TtsManager,
    private val memoryRepository: MemoryRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val getEffectiveSpicyModeUseCase: GetEffectiveSpicyModeUseCase,
    private val grantSpicyModeCreditUseCase: GrantSpicyModeCreditUseCase,
    val rewardedAdManager: RewardedAdManager,
    val consentManager: ConsentManager,
) : ViewModel() {

    fun launchSubscriptionPurchase(activity: Activity) {
        subscriptionRepository.launchPurchaseFlow(activity)
    }

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    init {
        ttsManager.ensureInitialized()
        observeSettings()
        refreshModelStatus()
        refreshImageModelStatus()
        viewModelScope.launch {
            memoryRepository.observeMemories().collect { memories ->
                _state.update { it.copy(memories = memories) }
            }
        }
    }

    fun updateGfName(name: String) {
        updateGfProfile { it.copy(name = name) }
    }

    fun updateVisualTemplate(template: VisualTemplate) {
        updateGfProfile { it.copy(visualTemplate = template) }
        ttsManager.setAvatar(template)
    }

    fun togglePersonalityTrait(trait: PersonalityTrait) {
        updateGfProfile { profile ->
            val updatedTraits = profile.personalityTraits.toMutableList()
            if (updatedTraits.contains(trait)) {
                updatedTraits.remove(trait)
            } else if (updatedTraits.size < MAX_PERSONALITY_TRAITS) {
                updatedTraits.add(trait)
            }
            profile.copy(personalityTraits = updatedTraits)
        }
    }

    fun updateCustomNotes(notes: String) {
        updateGfProfile { it.copy(customPromptAdditions = notes.ifBlank { null }) }
    }

    fun setSpicyMode(enabled: Boolean) {
        if (enabled && (_state.value.userProfile?.age ?: 0) < MINIMUM_AGE) {
            viewModelScope.launch { _events.emit(SettingsEvent.ShowMessage("Romance mode is available only to adults 18 and older.")) }
            return
        }
        if (enabled && !_state.value.isSubscribed) {
            _state.update { it.copy(showPaywall = true) }
            return
        }
        updateGfProfile { it.copy(spicyModeEnabled = enabled) }
    }

    fun dismissPaywall() {
        _state.update { it.copy(showPaywall = false) }
    }

    fun requestSubscribe() {
        viewModelScope.launch { _events.emit(SettingsEvent.LaunchSubscriptionPurchase) }
    }

    fun requestWatchRewardedAd() {
        viewModelScope.launch { _events.emit(SettingsEvent.LaunchRewardedAd) }
    }

    fun openPrivacyOptions() {
        viewModelScope.launch { _events.emit(SettingsEvent.OpenPrivacyOptions) }
    }

    /** Called once the rewarded ad's earned-reward callback has fired. */
    fun onRewardedAdEarned() {
        viewModelScope.launch {
            when (grantSpicyModeCreditUseCase()) {
                GrantSpicyModeCreditUseCase.Result.Granted -> {
                    _state.update { it.copy(showPaywall = false) }
                    _events.emit(SettingsEvent.ShowMessage("+${GrantSpicyModeCreditUseCase.CREDIT_MINUTES_PER_AD} min of Spicy Mode unlocked."))
                }
                GrantSpicyModeCreditUseCase.Result.DailyCapReached -> {
                    _events.emit(SettingsEvent.ShowMessage("You've reached today's Spicy Mode credit limit. It resets at midnight."))
                }
            }
        }
    }

    fun setVoiceOption(option: VoiceOption) {
        updateGfProfile { it.copy(voiceOption = option) }
        ttsManager.setVoice(option)
    }

    fun setCompanionPresentation(presentation: CompanionPresentation) {
        ttsManager.setPresentation(presentation)
        _state.value.gfProfile?.let { profile ->
            ttsManager.setAvatar(
                profile.visualTemplate.takeIf { it.supports(presentation) }
                    ?: VisualTemplate.entries.first { it.supports(presentation) },
            )
        }
        updateGfProfile { profile ->
            profile.copy(
                presentation = presentation,
                visualTemplate = profile.visualTemplate.takeIf { it.supports(presentation) }
                    ?: VisualTemplate.entries.first { it.supports(presentation) },
            )
        }
    }

    fun setModelVariant(variant: ModelVariant) {
        viewModelScope.launch {
            modelRepository.setSelectedVariant(variant)
            _events.emit(SettingsEvent.ShowMessage("${variant.displayName} selected. Use Re-download Model to install it."))
        }
    }

    fun previewVoice(option: VoiceOption) {
        _state.value.gfProfile?.let {
            ttsManager.setPresentation(it.presentation)
            ttsManager.setAvatar(it.visualTemplate)
        }
        ttsManager.setSpeechSpeed(_state.value.speechSpeed)
        ttsManager.previewVoice(option)
    }

    fun updateUserName(name: String) {
        updateUserProfile { it.copy(name = name) }
    }

    fun updateUserAge(age: String) {
        val parsedAge = age.filter(Char::isDigit).take(3).toIntOrNull() ?: return
        if (parsedAge < MINIMUM_AGE) {
            viewModelScope.launch { _events.emit(SettingsEvent.ShowMessage("AirGF is for adults 18 and older.")) }
            return
        }
        updateUserProfile { it.copy(age = parsedAge) }
    }

    fun addInterest(interest: String) {
        val normalized = interest.trim()
        if (normalized.isEmpty()) return

        updateUserProfile { profile ->
            if (profile.interests.any { it.equals(normalized, ignoreCase = true) }) {
                profile
            } else {
                profile.copy(interests = profile.interests + normalized)
            }
        }
    }

    fun removeInterest(interest: String) {
        updateUserProfile { profile ->
            profile.copy(interests = profile.interests.filterNot { it.equals(interest, ignoreCase = true) })
        }
    }

    fun setTtsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                userRepository.setTtsEnabled(enabled)
                if (!enabled) {
                    ttsManager.stop()
                }
            } catch (error: Throwable) {
                emitFailure(error)
            }
        }
    }

    fun setSpeechSpeed(speed: Float) {
        val clamped = speed.coerceIn(TtsManager.MIN_SPEECH_RATE, TtsManager.MAX_SPEECH_RATE)
        viewModelScope.launch {
            try {
                userRepository.setSpeechSpeed(clamped)
                ttsManager.setSpeechSpeed(clamped)
            } catch (error: Throwable) {
                emitFailure(error)
            }
        }
    }

    fun setProactiveMessagesEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                userRepository.setProactiveMessagesEnabled(enabled)
                proactiveScheduler.sync()
            } catch (error: Throwable) {
                emitFailure(error)
            }
        }
    }

    fun setNotificationFrequency(frequency: String) {
        viewModelScope.launch {
            try {
                userRepository.setNotificationFrequency(frequency)
                proactiveScheduler.sync()
            } catch (error: Throwable) {
                emitFailure(error)
            }
        }
    }

    fun stopPreview() {
        ttsManager.stop()
    }

    fun approveMemory(id: Long) {
        viewModelScope.launch { memoryRepository.setState(id, MemoryState.APPROVED) }
    }

    fun forgetMemory(id: Long) {
        viewModelScope.launch { memoryRepository.setState(id, MemoryState.DELETED) }
    }

    fun forgetAllMemories() {
        viewModelScope.launch {
            memoryRepository.deleteAll()
            _events.emit(SettingsEvent.ShowMessage("Companion memory cleared."))
        }
    }

    fun exportChatHistory() {
        viewModelScope.launch {
            _state.update { it.copy(isExporting = true) }
            try {
                val uri = exportChatUseCase()
                _events.emit(SettingsEvent.ShareExport(uri))
            } catch (error: Throwable) {
                emitFailure(error)
            }
            _state.update { it.copy(isExporting = false) }
        }
    }

    fun resetEverything() {
        viewModelScope.launch {
            _state.update { it.copy(isResetting = true) }
            try {
                resetEverythingUseCase()
                _events.emit(SettingsEvent.NavigateToOnboarding)
                _events.emit(SettingsEvent.ShowMessage("Everything was reset."))
            } catch (error: Throwable) {
                emitFailure(error)
            }
            _state.update { it.copy(isResetting = false) }
        }
    }

    fun deleteModel() {
        viewModelScope.launch {
            _state.update { it.copy(isDeletingModel = true) }
            try {
                modelRepository.clearModel()
                refreshModelStatusInternal()
                _events.emit(SettingsEvent.ShowMessage("Model deleted from this device."))
            } catch (error: Throwable) {
                emitFailure(error)
            }
            _state.update { it.copy(isDeletingModel = false) }
        }
    }

    fun redownloadModel() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isDownloadingModel = true,
                    modelStatus = SettingsModelStatus.NotDownloaded,
                )
            }
            try {
                if (!modelRepository.isNetworkAvailable()) {
                    error("No internet connection available for model download.")
                }
                if (!modelRepository.hasSufficientStorage()) {
                    error("Not enough free storage to re-download the model.")
                }
                modelRepository.clearModel()
                modelRepository.downloadModel().collect { state ->
                    when (state) {
                        is DownloadState.Complete -> {
                            refreshModelStatusInternal()
                            _events.emit(SettingsEvent.ShowMessage("Model download complete."))
                        }
                        is DownloadState.Error -> error(state.message)
                        DownloadState.Idle,
                        is DownloadState.Progress -> Unit
                    }
                }
            } catch (error: Throwable) {
                refreshModelStatusInternal()
                emitFailure(error)
            }
            _state.update { it.copy(isDownloadingModel = false) }
        }
    }

    fun deleteImageModel() {
        viewModelScope.launch {
            _state.update { it.copy(isDeletingImageModel = true) }
            try {
                imageGenRepository.clearModel()
                refreshImageModelStatusInternal()
                _events.emit(SettingsEvent.ShowMessage("Image model deleted from this device."))
            } catch (error: Throwable) {
                emitFailure(error)
            }
            _state.update { it.copy(isDeletingImageModel = false) }
        }
    }

    fun redownloadImageModel() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isDownloadingImageModel = true,
                    imageModelStatus = SettingsImageModelStatus.NotDownloaded,
                )
            }
            try {
                if (!imageGenRepository.isNetworkAvailable()) {
                    error("No internet connection available for image model download.")
                }
                if (!imageGenRepository.hasSufficientStorage()) {
                    error("Not enough free storage to download the image model.")
                }
                imageGenRepository.clearModel()
                imageGenRepository.downloadModel().collect { state ->
                    when (state) {
                        is DownloadState.Complete -> {
                            refreshImageModelStatusInternal()
                            _events.emit(SettingsEvent.ShowMessage("Image model download complete."))
                        }
                        is DownloadState.Error -> error(state.message)
                        DownloadState.Idle,
                        is DownloadState.Progress -> Unit
                    }
                }
            } catch (error: Throwable) {
                refreshImageModelStatusInternal()
                emitFailure(error)
            }
            _state.update { it.copy(isDownloadingImageModel = false) }
        }
    }

    fun refreshModelStatus() {
        viewModelScope.launch {
            try {
                refreshModelStatusInternal()
            } catch (error: Throwable) {
                _state.update { it.copy(modelStatus = SettingsModelStatus.NotDownloaded) }
                emitFailure(error)
            }
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            combine(
                gfConfigRepository.getProfileFlow(),
                userRepository.getProfileFlow(),
                userRepository.ttsEnabledFlow(),
                userRepository.speechSpeedFlow(),
                userRepository.proactiveMessagesEnabledFlow(),
                userRepository.notificationFrequencyFlow(),
                modelRepository.selectedVariantFlow(),
            ) { values ->
                SettingsSnapshot(
                    gfProfile = values[0] as GfProfile?,
                    userProfile = values[1] as UserProfile?,
                    ttsEnabled = values[2] as Boolean,
                    speechSpeed = values[3] as Float,
                    proactiveMessagesEnabled = values[4] as Boolean,
                    notificationFrequency = values[5] as String,
                    selectedModelVariant = values[6] as ModelVariant,
                )
            }.collect { snapshot ->
                _state.update { current ->
                    current.copy(
                        isLoading = false,
                        gfProfile = snapshot.gfProfile,
                        userProfile = snapshot.userProfile,
                        ttsEnabled = snapshot.ttsEnabled,
                        speechSpeed = snapshot.speechSpeed,
                        proactiveMessagesEnabled = snapshot.proactiveMessagesEnabled,
                        notificationFrequency = snapshot.notificationFrequency,
                        selectedModelVariant = snapshot.selectedModelVariant,
                    )
                }
                ttsManager.setSpeechSpeed(snapshot.speechSpeed)
                snapshot.gfProfile?.let { profile ->
                    ttsManager.setPresentation(profile.presentation)
                    ttsManager.setAvatar(profile.visualTemplate)
                    ttsManager.setVoice(profile.voiceOption)
                }
            }
        }
        viewModelScope.launch {
            combine(
                subscriptionRepository.isSubscribedFlow(),
                getEffectiveSpicyModeUseCase.flow(),
                subscriptionRepository.spicyModeGrantedUntilFlow(),
                subscriptionRepository.spicyCreditMinutesRemainingTodayFlow(),
                subscriptionRepository.subscriptionProductDetailsFlow(),
            ) { values ->
                MonetizationSnapshot(
                    isSubscribed = values[0] as Boolean,
                    spicyModeActive = values[1] as Boolean,
                    spicyModeGrantedUntil = values[2] as Long?,
                    spicyCreditMinutesRemainingToday = values[3] as Int,
                    subscriptionPriceLabel = (values[4] as? com.android.billingclient.api.ProductDetails)
                        ?.subscriptionOfferDetails?.firstOrNull()
                        ?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice,
                )
            }.collect { snapshot ->
                _state.update { current ->
                    current.copy(
                        isSubscribed = snapshot.isSubscribed,
                        spicyModeActive = snapshot.spicyModeActive,
                        spicyModeGrantedUntil = snapshot.spicyModeGrantedUntil,
                        spicyCreditMinutesRemainingToday = snapshot.spicyCreditMinutesRemainingToday,
                        subscriptionPriceLabel = snapshot.subscriptionPriceLabel,
                    )
                }
            }
        }
    }

    private fun updateGfProfile(transform: (GfProfile) -> GfProfile) {
        val profile = _state.value.gfProfile ?: return
        viewModelScope.launch {
            try {
                gfConfigRepository.saveProfile(transform(profile))
            } catch (error: Throwable) {
                emitFailure(error)
            }
        }
    }

    private fun updateUserProfile(transform: (UserProfile) -> UserProfile) {
        val profile = _state.value.userProfile ?: return
        viewModelScope.launch {
            try {
                userRepository.saveProfile(transform(profile))
            } catch (error: Throwable) {
                emitFailure(error)
            }
        }
    }

    private suspend fun refreshModelStatusInternal() {
        val path = modelRepository.getModelPath()
        val status = if (path == null) {
            SettingsModelStatus.NotDownloaded
        } else {
            SettingsModelStatus.Downloaded(
                path = path,
                sizeLabel = File(path).length().toReadableSize(),
            )
        }
        _state.update { it.copy(modelStatus = status) }
    }

    private suspend fun refreshImageModelStatusInternal() {
        val path = imageGenRepository.getModelPath()
        val status = if (path == null) {
            SettingsImageModelStatus.NotDownloaded
        } else {
            SettingsImageModelStatus.Downloaded(
                path = path,
                sizeLabel = File(path).length().toReadableSize(),
            )
        }
        _state.update { it.copy(imageModelStatus = status) }
    }

    private fun refreshImageModelStatus() {
        viewModelScope.launch {
            try {
                refreshImageModelStatusInternal()
            } catch (error: Throwable) {
                _state.update { it.copy(imageModelStatus = SettingsImageModelStatus.NotDownloaded) }
            }
        }
    }

    private suspend fun emitFailure(error: Throwable) {
        _events.emit(
            SettingsEvent.ShowMessage(
                error.message ?: "Unable to update settings right now.",
            ),
        )
    }

    private data class SettingsSnapshot(
        val gfProfile: GfProfile?,
        val userProfile: UserProfile?,
        val ttsEnabled: Boolean,
        val speechSpeed: Float,
        val proactiveMessagesEnabled: Boolean,
        val notificationFrequency: String,
        val selectedModelVariant: ModelVariant,
    )

    private data class MonetizationSnapshot(
        val isSubscribed: Boolean,
        val spicyModeActive: Boolean,
        val spicyModeGrantedUntil: Long?,
        val spicyCreditMinutesRemainingToday: Int,
        val subscriptionPriceLabel: String?,
    )

    private fun Long.toReadableSize(): String {
        if (this <= 0L) return "0 B"
        val units = listOf("B", "KB", "MB", "GB", "TB")
        var value = this.toDouble()
        var unitIndex = 0
        while (value >= 1024 && unitIndex < units.lastIndex) {
            value /= 1024
            unitIndex++
        }
        return if (unitIndex == 0) {
            "${value.toInt()} ${units[unitIndex]}"
        } else {
            String.format("%.1f %s", value, units[unitIndex])
        }
    }

    companion object {
        private const val MAX_PERSONALITY_TRAITS = 3
        private const val MINIMUM_AGE = 18
    }
}

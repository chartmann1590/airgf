package com.airgf.app.testutil

import com.airgf.app.data.model.DownloadState
import com.airgf.app.domain.model.Conversation
import com.airgf.app.domain.model.GfProfile
import com.airgf.app.domain.model.Message
import com.airgf.app.domain.model.UserProfile
import com.airgf.app.domain.repository.ChatRepository
import com.airgf.app.domain.repository.GfConfigRepository
import com.airgf.app.domain.repository.ImageGenRepository
import com.airgf.app.domain.repository.ModelRepository
import com.airgf.app.domain.repository.UserRepository
import com.airgf.app.domain.repository.MemoryRepository
import com.airgf.app.domain.model.CompanionMemory
import com.airgf.app.domain.model.MemoryState
import com.airgf.app.domain.repository.SubscriptionRepository
import com.airgf.app.notification.ProactiveMessageScheduler
import com.airgf.app.llm.ModelVariant
import com.android.billingclient.api.ProductDetails
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

class FakeGfConfigRepository(
    profile: GfProfile? = null,
) : GfConfigRepository {
    private val state = MutableStateFlow(profile)
    var deleted = false

    override suspend fun getProfile(): GfProfile? = state.value

    override fun getProfileFlow(): Flow<GfProfile?> = state

    override suspend fun saveProfile(profile: GfProfile) {
        state.value = profile
    }

    override suspend fun delete() {
        deleted = true
        state.value = null
    }
}

class FakeUserRepository(
    profile: UserProfile? = null,
    private var onboardingComplete: Boolean = false,
    private var ttsEnabled: Boolean = true,
    private var speechSpeed: Float = 1.0f,
    private var proactiveMessagesEnabled: Boolean = true,
    private var notificationFrequency: String = "sometimes",
) : UserRepository {
    private val profileState = MutableStateFlow(profile)
    private val ttsEnabledState = MutableStateFlow(ttsEnabled)
    private val speechSpeedState = MutableStateFlow(speechSpeed)
    private val proactiveMessagesEnabledState = MutableStateFlow(proactiveMessagesEnabled)
    private val notificationFrequencyState = MutableStateFlow(notificationFrequency)
    var cleared = false

    override suspend fun getProfile(): UserProfile? = profileState.value

    override fun getProfileFlow(): Flow<UserProfile?> = profileState

    override suspend fun saveProfile(profile: UserProfile) {
        profileState.value = profile
    }

    override suspend fun isOnboardingComplete(): Boolean = onboardingComplete

    override suspend fun setOnboardingComplete(complete: Boolean) {
        onboardingComplete = complete
    }

    override fun ttsEnabledFlow(): Flow<Boolean> = ttsEnabledState

    override suspend fun isTtsEnabled(): Boolean = ttsEnabledState.value

    override suspend fun setTtsEnabled(enabled: Boolean) {
        ttsEnabledState.value = enabled
    }

    override fun speechSpeedFlow(): Flow<Float> = speechSpeedState

    override suspend fun getSpeechSpeed(): Float = speechSpeedState.value

    override suspend fun setSpeechSpeed(speed: Float) {
        speechSpeedState.value = speed
    }

    override fun proactiveMessagesEnabledFlow(): Flow<Boolean> = proactiveMessagesEnabledState

    override suspend fun areProactiveMessagesEnabled(): Boolean = proactiveMessagesEnabledState.value

    override suspend fun setProactiveMessagesEnabled(enabled: Boolean) {
        proactiveMessagesEnabledState.value = enabled
    }

    override fun notificationFrequencyFlow(): Flow<String> = notificationFrequencyState

    override suspend fun getNotificationFrequency(): String = notificationFrequencyState.value

    override suspend fun setNotificationFrequency(frequency: String) {
        notificationFrequencyState.value = frequency
    }

    override suspend fun clearAll() {
        cleared = true
        onboardingComplete = false
        profileState.value = null
        ttsEnabledState.value = true
        speechSpeedState.value = 1.0f
        proactiveMessagesEnabledState.value = true
        notificationFrequencyState.value = "sometimes"
    }
}

class FakeChatRepository(
    private var recentMessages: List<Message> = emptyList(),
    private var conversations: List<Conversation> = emptyList(),
    private var allMessages: List<Message> = emptyList(),
) : ChatRepository {
    var deleteAllMessagesCalled = false

    override fun getMessagesFlow(conversationId: Long): Flow<List<Message>> =
        flowOf(allMessages.filter { it.conversationId == conversationId })

    override suspend fun getRecentMessages(limit: Int): List<Message> = recentMessages.take(limit)

    override suspend fun insertMessage(message: Message): Long {
        allMessages = allMessages + message
        return message.id
    }

    override suspend fun getOrCreateConversation(): Long =
        conversations.firstOrNull()?.id ?: 1L

    override suspend fun deleteAllMessages() {
        deleteAllMessagesCalled = true
        allMessages = emptyList()
        recentMessages = emptyList()
    }

    override suspend fun getAllConversations(): List<Conversation> = conversations

    override suspend fun getAllMessages(): List<Message> = allMessages
}

class FakeModelRepository(
    private var modelPath: String? = null,
) : ModelRepository {
    private val variantState = MutableStateFlow(ModelVariant.E2B)
    var clearModelCalled = false
    var setModelDownloadedPath: String? = null

    override suspend fun isModelDownloaded(): Boolean = modelPath != null

    override suspend fun getModelPath(): String? = modelPath

    override suspend fun setModelDownloaded(path: String) {
        modelPath = path
        setModelDownloadedPath = path
    }

    override suspend fun clearModel() {
        clearModelCalled = true
        modelPath = null
    }

    override fun isNetworkAvailable(): Boolean = true

    override fun hasSufficientStorage(): Boolean = true

    override fun downloadModel(): Flow<DownloadState> = flowOf(DownloadState.Idle)
    override fun selectedVariantFlow(): Flow<ModelVariant> = variantState
    override suspend fun getSelectedVariant(): ModelVariant = variantState.value
    override suspend fun setSelectedVariant(variant: ModelVariant) { variantState.value = variant }
}

class FakeImageGenRepository(
    private var modelDownloaded: Boolean = false,
) : ImageGenRepository {
    override suspend fun isModelDownloaded(): Boolean = modelDownloaded
    override suspend fun getModelPath(): String? = if (modelDownloaded) "/tmp/sd_model" else null
    override suspend fun setModelDownloaded(path: String) { modelDownloaded = true }
    override suspend fun clearModel() { modelDownloaded = false }
    override fun isNetworkAvailable(): Boolean = true
    override fun hasSufficientStorage(): Boolean = true
    override fun downloadModel(): Flow<DownloadState> = flowOf(DownloadState.Idle)
}

class FakeProactiveMessageScheduler : ProactiveMessageScheduler {
    var syncCalls = 0
    var disableCalls = 0

    override suspend fun sync() {
        syncCalls++
    }

    override fun disable() {
        disableCalls++
    }
}

class FakeSubscriptionRepository(
    isSubscribed: Boolean = false,
    spicyModeGrantedUntil: Long? = null,
) : SubscriptionRepository {
    private val isSubscribedState = MutableStateFlow(isSubscribed)
    private val grantedUntilState = MutableStateFlow(spicyModeGrantedUntil)
    private val creditMinutesRemainingState = MutableStateFlow(240)

    override fun isSubscribedFlow(): Flow<Boolean> = isSubscribedState
    override suspend fun isSubscribed(): Boolean = isSubscribedState.value
    override fun spicyModeGrantedUntilFlow(): Flow<Long?> = grantedUntilState
    override fun spicyCreditMinutesRemainingTodayFlow(): Flow<Int> = creditMinutesRemainingState
    override suspend fun grantSpicyModeCreditMinutes(minutes: Int) {
        grantedUntilState.value = System.currentTimeMillis() + minutes * 60_000L
        creditMinutesRemainingState.value = (creditMinutesRemainingState.value - minutes).coerceAtLeast(0)
    }
    override fun subscriptionProductDetailsFlow(): Flow<ProductDetails?> = flowOf(null)
    override fun launchPurchaseFlow(activity: android.app.Activity) {}
}

class FakeMemoryRepository : MemoryRepository {
    private val memories = MutableStateFlow<List<CompanionMemory>>(emptyList())
    var deleteAllCalled = false

    override fun observeMemories(): Flow<List<CompanionMemory>> = memories
    override suspend fun getApproved(): List<CompanionMemory> = memories.value.filter { it.state == MemoryState.APPROVED }
    override suspend fun suggest(memory: CompanionMemory): Long {
        val id = (memories.value.maxOfOrNull { it.id } ?: 0) + 1
        memories.value = memories.value + memory.copy(id = id)
        return id
    }
    override suspend fun setState(id: Long, state: MemoryState) {
        memories.value = memories.value.map { if (it.id == id) it.copy(state = state) else it }
    }
    override suspend fun deleteAll() {
        deleteAllCalled = true
        memories.value = emptyList()
    }
}

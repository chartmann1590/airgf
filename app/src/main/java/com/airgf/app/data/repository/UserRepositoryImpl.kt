package com.airgf.app.data.repository

import com.airgf.app.data.local.datastore.UserPreferences
import com.airgf.app.domain.model.UserProfile
import com.airgf.app.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userPreferences: UserPreferences,
) : UserRepository {

    override suspend fun getProfile(): UserProfile? =
        userPreferences.userProfile.first()

    override fun getProfileFlow(): Flow<UserProfile?> =
        userPreferences.userProfile

    override suspend fun saveProfile(profile: UserProfile) {
        userPreferences.saveUserProfile(profile)
    }

    override suspend fun isOnboardingComplete(): Boolean =
        userPreferences.onboardingComplete.first()

    override suspend fun setOnboardingComplete(complete: Boolean) {
        userPreferences.setOnboardingComplete(complete)
    }

    override fun ttsEnabledFlow(): Flow<Boolean> = userPreferences.ttsEnabled

    override suspend fun isTtsEnabled(): Boolean = userPreferences.ttsEnabled.first()

    override suspend fun setTtsEnabled(enabled: Boolean) {
        userPreferences.setTtsEnabled(enabled)
    }

    override fun speechSpeedFlow(): Flow<Float> = userPreferences.speechSpeed

    override suspend fun getSpeechSpeed(): Float = userPreferences.speechSpeed.first()

    override suspend fun setSpeechSpeed(speed: Float) {
        userPreferences.setSpeechSpeed(speed)
    }

    override fun proactiveMessagesEnabledFlow(): Flow<Boolean> =
        userPreferences.proactiveMessagesEnabled

    override suspend fun areProactiveMessagesEnabled(): Boolean =
        userPreferences.proactiveMessagesEnabled.first()

    override suspend fun setProactiveMessagesEnabled(enabled: Boolean) {
        userPreferences.setProactiveMessagesEnabled(enabled)
    }

    override fun notificationFrequencyFlow(): Flow<String> =
        userPreferences.notificationFrequency

    override suspend fun getNotificationFrequency(): String =
        userPreferences.notificationFrequency.first()

    override suspend fun setNotificationFrequency(frequency: String) {
        userPreferences.setNotificationFrequency(frequency)
    }

    override suspend fun clearAll() {
        userPreferences.clearAll()
    }
}

package com.airgf.app.domain.repository

import com.airgf.app.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun getProfile(): UserProfile?
    fun getProfileFlow(): Flow<UserProfile?>
    suspend fun saveProfile(profile: UserProfile)
    suspend fun isOnboardingComplete(): Boolean
    suspend fun setOnboardingComplete(complete: Boolean)
    fun ttsEnabledFlow(): Flow<Boolean>
    suspend fun isTtsEnabled(): Boolean
    suspend fun setTtsEnabled(enabled: Boolean)
    fun speechSpeedFlow(): Flow<Float>
    suspend fun getSpeechSpeed(): Float
    suspend fun setSpeechSpeed(speed: Float)
    fun proactiveMessagesEnabledFlow(): Flow<Boolean>
    suspend fun areProactiveMessagesEnabled(): Boolean
    suspend fun setProactiveMessagesEnabled(enabled: Boolean)
    fun notificationFrequencyFlow(): Flow<String>
    suspend fun getNotificationFrequency(): String
    suspend fun setNotificationFrequency(frequency: String)
    suspend fun clearAll()
}

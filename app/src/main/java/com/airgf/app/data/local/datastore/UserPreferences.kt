package com.airgf.app.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.airgf.app.domain.model.UserProfile
import com.airgf.app.llm.ModelVariant
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        const val MAX_SPICY_CREDIT_MINUTES_PER_DAY = 240
    }

    private object Keys {
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val MODEL_DOWNLOADED = booleanPreferencesKey("model_downloaded")
        val MODEL_FILE_PATH = stringPreferencesKey("model_file_path")
        val USER_PROFILE = stringPreferencesKey("user_profile")
        val TTS_ENABLED = booleanPreferencesKey("tts_enabled")
        val SPEECH_SPEED = floatPreferencesKey("speech_speed")
        val PROACTIVE_MESSAGES_ENABLED = booleanPreferencesKey("proactive_messages_enabled")
        val NOTIFICATION_FREQUENCY = stringPreferencesKey("notification_frequency")
        val IMAGE_MODEL_DOWNLOADED = booleanPreferencesKey("image_model_downloaded")
        val IMAGE_MODEL_PATH = stringPreferencesKey("image_model_path")
        val MODEL_VARIANT = stringPreferencesKey("model_variant")
        val IS_SUBSCRIBED = booleanPreferencesKey("is_subscribed")
        val SPICY_CREDIT_MINUTES_USED_TODAY = intPreferencesKey("spicy_credit_minutes_used_today")
        val SPICY_CREDIT_DAY = stringPreferencesKey("spicy_credit_day")
        val SPICY_MODE_GRANTED_UNTIL = longPreferencesKey("spicy_mode_granted_until")
    }

    val onboardingComplete: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.ONBOARDING_COMPLETE] ?: false
    }

    val modelDownloaded: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.MODEL_DOWNLOADED] ?: false
    }

    val modelFilePath: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.MODEL_FILE_PATH]
    }

    val userProfile: Flow<UserProfile?> = context.dataStore.data.map { prefs ->
        prefs[Keys.USER_PROFILE]?.let { json.decodeFromString(UserProfile.serializer(), it) }
    }

    val ttsEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.TTS_ENABLED] ?: true
    }

    val speechSpeed: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[Keys.SPEECH_SPEED] ?: 1.0f
    }

    val proactiveMessagesEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.PROACTIVE_MESSAGES_ENABLED] ?: true
    }

    val notificationFrequency: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.NOTIFICATION_FREQUENCY] ?: "sometimes"
    }

    val imageModelDownloaded: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.IMAGE_MODEL_DOWNLOADED] ?: false
    }

    val imageModelFilePath: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.IMAGE_MODEL_PATH]
    }

    val modelVariant: Flow<ModelVariant> = context.dataStore.data.map { prefs ->
        prefs[Keys.MODEL_VARIANT]?.let { runCatching { ModelVariant.valueOf(it) }.getOrNull() }
            ?: ModelVariant.E2B
    }

    val isSubscribed: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.IS_SUBSCRIBED] ?: false
    }

    val spicyModeGrantedUntil: Flow<Long?> = context.dataStore.data.map { prefs ->
        prefs[Keys.SPICY_MODE_GRANTED_UNTIL]
    }

    /** Minutes of rewarded-ad Spicy Mode credit left to earn today (resets at local midnight). */
    val spicyCreditMinutesRemainingToday: Flow<Int> = context.dataStore.data.map { prefs ->
        val today = LocalDate.now().toString()
        val usedMinutes = if (prefs[Keys.SPICY_CREDIT_DAY] == today) {
            prefs[Keys.SPICY_CREDIT_MINUTES_USED_TODAY] ?: 0
        } else {
            0
        }
        (MAX_SPICY_CREDIT_MINUTES_PER_DAY - usedMinutes).coerceAtLeast(0)
    }

    suspend fun setSubscribed(subscribed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.IS_SUBSCRIBED] = subscribed
        }
    }

    /** Records a rewarded-ad credit redemption and extends the Spicy Mode window accordingly. */
    suspend fun addSpicyCreditMinutes(minutes: Int) {
        context.dataStore.edit { prefs ->
            val today = LocalDate.now().toString()
            val usedMinutes = if (prefs[Keys.SPICY_CREDIT_DAY] == today) {
                prefs[Keys.SPICY_CREDIT_MINUTES_USED_TODAY] ?: 0
            } else {
                0
            }
            val newUsedMinutes = (usedMinutes + minutes).coerceAtMost(MAX_SPICY_CREDIT_MINUTES_PER_DAY)
            prefs[Keys.SPICY_CREDIT_DAY] = today
            prefs[Keys.SPICY_CREDIT_MINUTES_USED_TODAY] = newUsedMinutes

            val now = System.currentTimeMillis()
            val currentUntil = prefs[Keys.SPICY_MODE_GRANTED_UNTIL] ?: 0L
            val extendFrom = maxOf(now, currentUntil)
            prefs[Keys.SPICY_MODE_GRANTED_UNTIL] = extendFrom + minutes * 60_000L
        }
    }

    suspend fun setModelVariant(variant: ModelVariant) {
        context.dataStore.edit { it[Keys.MODEL_VARIANT] = variant.name }
    }

    suspend fun setOnboardingComplete(complete: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ONBOARDING_COMPLETE] = complete
        }
    }

    suspend fun setModelDownloaded(downloaded: Boolean, path: String?) {
        context.dataStore.edit { prefs ->
            prefs[Keys.MODEL_DOWNLOADED] = downloaded
            if (path != null) {
                prefs[Keys.MODEL_FILE_PATH] = path
            } else {
                prefs.remove(Keys.MODEL_FILE_PATH)
            }
        }
    }

    suspend fun saveUserProfile(profile: UserProfile) {
        context.dataStore.edit { prefs ->
            prefs[Keys.USER_PROFILE] = json.encodeToString(UserProfile.serializer(), profile)
        }
    }

    suspend fun setTtsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.TTS_ENABLED] = enabled
        }
    }

    suspend fun setSpeechSpeed(speed: Float) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SPEECH_SPEED] = speed
        }
    }

    suspend fun setProactiveMessagesEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.PROACTIVE_MESSAGES_ENABLED] = enabled
        }
    }

    suspend fun setNotificationFrequency(frequency: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.NOTIFICATION_FREQUENCY] = frequency
        }
    }

    suspend fun setImageModelDownloaded(downloaded: Boolean, path: String?) {
        context.dataStore.edit { prefs ->
            prefs[Keys.IMAGE_MODEL_DOWNLOADED] = downloaded
            if (path != null) {
                prefs[Keys.IMAGE_MODEL_PATH] = path
            } else {
                prefs.remove(Keys.IMAGE_MODEL_PATH)
            }
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}

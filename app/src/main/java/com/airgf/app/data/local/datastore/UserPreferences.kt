package com.airgf.app.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.airgf.app.domain.model.UserProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val json = Json { ignoreUnknownKeys = true }

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

package com.airgf.app

import android.content.Context
import android.content.Intent
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.airgf.app.data.local.datastore.UserPreferences
import com.airgf.app.data.local.db.AppDatabase
import com.airgf.app.data.repository.GfConfigRepositoryImpl
import com.airgf.app.domain.model.CommunicationStyle
import com.airgf.app.domain.model.GfProfile
import com.airgf.app.domain.model.PersonalityTrait
import com.airgf.app.domain.model.RelationshipType
import com.airgf.app.domain.model.UserProfile
import com.airgf.app.domain.model.VisualTemplate
import com.airgf.app.domain.model.VoiceOption
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppLaunchSmokeTest {
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Before
    fun seedLaunchState() = runBlocking {
        context.deleteDatabase(DATABASE_NAME)

        val userPreferences = UserPreferences(context)
        userPreferences.clearAll()
        userPreferences.saveUserProfile(
            UserProfile(
                name = "Alex",
                age = 27,
                interests = listOf("Music", "Games", "Travel"),
                communicationStyle = CommunicationStyle.CASUAL,
            ),
        )
        userPreferences.setOnboardingComplete(true)
        userPreferences.setProactiveMessagesEnabled(false)

        val database = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            DATABASE_NAME,
        ).build()
        try {
            GfConfigRepositoryImpl(database.gfConfigDao()).saveProfile(
                GfProfile(
                    name = "Mina",
                    visualTemplate = VisualTemplate.MAYA,
                    personalityTraits = listOf(PersonalityTrait.ROMANTIC, PersonalityTrait.PLAYFUL),
                    relationshipType = RelationshipType.ROMANTIC,
                    voiceOption = VoiceOption.SOFT,
                    spicyModeEnabled = false,
                ),
            )
        } finally {
            database.close()
        }
    }

    @Test
    fun launchesIntoChatAndNavigatesToSettings() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(launchIntent)

        assertTrue(device.wait(Until.hasObject(By.desc("Settings")), TIMEOUT_MS))
        device.findObject(By.desc("Settings")).click()
        assertTrue(device.wait(Until.hasObject(By.text("Settings")), TIMEOUT_MS))
    }

    companion object {
        private const val DATABASE_NAME = "airgf.db"
        private const val TIMEOUT_MS = 5_000L
    }
}

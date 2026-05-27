package com.airgf.app.domain.usecase

import com.airgf.app.domain.model.CommunicationStyle
import com.airgf.app.domain.model.GfProfile
import com.airgf.app.domain.model.PersonalityTrait
import com.airgf.app.domain.model.RelationshipType
import com.airgf.app.domain.model.UserProfile
import com.airgf.app.domain.model.VisualTemplate
import com.airgf.app.domain.model.VoiceOption
import com.airgf.app.testutil.FakeGfConfigRepository
import com.airgf.app.testutil.FakeImageGenRepository
import com.airgf.app.testutil.FakeUserRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildSystemPromptUseCaseTest {
    @Test
    fun `builds prompt from configured profiles`() = runBlocking {
        val gfRepository = FakeGfConfigRepository(
            GfProfile(
                name = "Mina",
                visualTemplate = VisualTemplate.ARIADNA,
                personalityTraits = listOf(PersonalityTrait.ROMANTIC, PersonalityTrait.PLAYFUL),
                relationshipType = RelationshipType.ROMANTIC,
                voiceOption = VoiceOption.SOFT,
                spicyModeEnabled = true,
                customPromptAdditions = "She loves stargazing.",
            ),
        )
        val userRepository = FakeUserRepository(
            profile = UserProfile(
                name = "Alex",
                age = 28,
                interests = listOf("music", "games"),
                communicationStyle = CommunicationStyle.DEEP,
            ),
        )

        val prompt = BuildSystemPromptUseCase(gfRepository, userRepository, FakeImageGenRepository())()

        assertTrue(prompt.contains("You are Mina, a virtual girlfriend."))
        assertTrue(prompt.contains("Your partner's name is Alex."))
        assertTrue(prompt.contains(RelationshipType.ROMANTIC.description))
        assertTrue(prompt.contains(PersonalityTrait.ROMANTIC.promptFragment))
        assertTrue(prompt.contains(PersonalityTrait.PLAYFUL.promptFragment))
        assertTrue(prompt.contains("MODE: Romantic/Flirty"))
        assertTrue(prompt.contains("Additional personality: She loves stargazing."))
        assertTrue(prompt.contains("Your partner enjoys: music, games."))
    }

    @Test
    fun `throws when girlfriend profile is missing`() {
        val useCase = BuildSystemPromptUseCase(
            gfConfigRepository = FakeGfConfigRepository(),
            userRepository = FakeUserRepository(),
            imageGenRepository = FakeImageGenRepository(),
        )

        assertThrows(IllegalStateException::class.java) {
            runBlocking { useCase() }
        }
    }
}

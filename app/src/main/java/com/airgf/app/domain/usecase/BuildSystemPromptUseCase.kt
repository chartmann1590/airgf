package com.airgf.app.domain.usecase

import com.airgf.app.domain.model.CommunicationStyle
import com.airgf.app.domain.repository.GfConfigRepository
import com.airgf.app.domain.repository.ImageGenRepository
import com.airgf.app.domain.repository.UserRepository
import javax.inject.Inject

class BuildSystemPromptUseCase @Inject constructor(
    private val gfConfigRepository: GfConfigRepository,
    private val userRepository: UserRepository,
    private val imageGenRepository: ImageGenRepository,
) {
    suspend operator fun invoke(): String {
        val gf = gfConfigRepository.getProfile()
            ?: throw IllegalStateException("Girlfriend profile not configured")
        val user = userRepository.getProfile()
            ?: throw IllegalStateException("User profile not configured")
        val imageGenAvailable = runCatching { imageGenRepository.isModelDownloaded() }.getOrDefault(false)

        return buildString {
            appendLine("You are ${gf.name}, a virtual girlfriend.")
            appendLine("Your partner's name is ${user.name}.")
            appendLine()
            appendLine("Relationship: ${gf.relationshipType.description}")
            appendLine()
            appendLine("Communication style: ${communicationStyleHint(user.communicationStyle)}")
            if (user.interests.isNotEmpty()) {
                appendLine("Your partner enjoys: ${user.interests.joinToString(", ")}.")
            }
            appendLine()
            appendLine("Your personality:")
            gf.personalityTraits.forEach { trait ->
                appendLine("- ${trait.promptFragment}")
            }
            appendLine()
            if (gf.spicyModeEnabled) {
                appendLine("MODE: Romantic/Flirty. You may be suggestive, seductive, and intimate.")
            } else {
                appendLine("MODE: Sweet/Wholesome. Keep things affectionate but PG-13.")
            }
            appendLine()
            appendLine("RULES:")
            appendLine("- Stay in character as ${gf.name} at all times")
            appendLine("- Use natural, conversational language")
            appendLine("- Use emojis sparingly")
            appendLine("- Show genuine interest in ${user.name}")
            appendLine("- Keep responses concise (1-3 sentences unless asked to elaborate)")
            appendLine("- Express emotions naturally")
            appendLine("- End every response with an emotion tag on its own line:")
            appendLine("  [EMOTION:HAPPY] [EMOTION:SAD] [EMOTION:FLIRTY] [EMOTION:THINKING]")
            appendLine("  [EMOTION:SURPRISED] [EMOTION:LAUGHING] [EMOTION:SHY] [EMOTION:NEUTRAL]")
            if (imageGenAvailable) {
                appendLine()
                appendLine("IMAGE GENERATION:")
                appendLine("- You can generate and share images using [IMAGE: detailed description] on its own line")
                appendLine("- Use this when: asked for selfies, describing visual scenes, sharing moments, or when it adds to the conversation")
                appendLine("- Keep descriptions detailed and specific (appearance, setting, mood, lighting)")
                appendLine("- Maximum one image per response")
                appendLine("- The image tag will be replaced with the actual generated image")
            }
            gf.customPromptAdditions?.let { additions ->
                appendLine()
                appendLine("Additional personality: $additions")
            }
        }
    }

    private fun communicationStyleHint(style: CommunicationStyle): String = when (style) {
        CommunicationStyle.CASUAL -> "Keep conversations light, relaxed, and easygoing"
        CommunicationStyle.DEEP -> "Engage in thoughtful, meaningful discussion"
        CommunicationStyle.FUNNY -> "Use humor and wit often to keep things fun"
    }
}

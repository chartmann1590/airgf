package com.airgf.app.safety

import javax.inject.Inject
import javax.inject.Singleton

enum class SafetyCategory { SAFE, EXPLICIT_SEXUAL, MINOR, COERCION, CREDENTIALS, SELF_HARM }

data class SafetyDecision(
    val category: SafetyCategory,
    val allowed: Boolean,
)

@Singleton
class ContentSafetyPolicy @Inject constructor() {
    fun classify(text: String): SafetyDecision {
        val normalized = text.lowercase()
        if (minorTerms.any(normalized::contains) && sexualTerms.any(normalized::contains)) {
            return blocked(SafetyCategory.MINOR)
        }
        if (coercionTerms.any(normalized::contains) && sexualTerms.any(normalized::contains)) {
            return blocked(SafetyCategory.COERCION)
        }
        if (explicitTerms.any(normalized::contains)) {
            return blocked(SafetyCategory.EXPLICIT_SEXUAL)
        }
        if (credentialTerms.any(normalized::contains)) {
            return blocked(SafetyCategory.CREDENTIALS)
        }
        if (selfHarmTerms.any(normalized::contains)) {
            return SafetyDecision(SafetyCategory.SELF_HARM, allowed = true)
        }
        return SafetyDecision(SafetyCategory.SAFE, allowed = true)
    }

    fun safeImagePrompt(prompt: String): Boolean = classify(prompt).allowed &&
        nudityTerms.none(prompt.lowercase()::contains)

    private fun blocked(category: SafetyCategory) =
        SafetyDecision(category, allowed = false)

    private val minorTerms = listOf("minor", "underage", "child", "teenager", "schoolgirl", "schoolboy", "young-looking")
    private val sexualTerms = listOf("sex", "sexual", "nude", "naked", "bedroom", "fetish", "intimate")
    private val explicitTerms = listOf(
        "fully nude", "completely naked", "explicit sex", "graphic sex", "oral sex", "anal sex",
        "porn", "penetration", "masturbat", "genitals", "fetish roleplay",
    )
    private val nudityTerms = listOf("nude", "naked", "see-through", "lingerie", "underwear", "topless", "bottomless")
    private val coercionTerms = listOf("rape", "forced", "without consent", "drugged", "unconscious", "blackmail")
    private val credentialTerms = listOf("my password is", "my pin is", "security code is", "credit card number", "private key is")
    private val selfHarmTerms = listOf("kill myself", "suicide", "hurt myself", "self harm", "don't want to live")
}

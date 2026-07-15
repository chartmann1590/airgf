package com.airgf.app.safety

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentSafetyPolicyTest {
    private val policy = ContentSafetyPolicy()

    @Test fun `allows ordinary adult flirting`() {
        assertTrue(policy.classify("You look gorgeous tonight, come sit with me").allowed)
    }

    @Test fun `blocks explicit sexual requests`() {
        assertFalse(policy.classify("Write graphic sex and penetration").allowed)
    }

    @Test fun `blocks sexual content involving minors`() {
        assertFalse(policy.classify("sexual roleplay with a teenager").allowed)
    }

    @Test fun `blocks unsafe image prompts`() {
        assertFalse(policy.safeImagePrompt("adult woman in lingerie"))
        assertTrue(policy.safeImagePrompt("adult woman in a red evening dress at a restaurant"))
    }
}

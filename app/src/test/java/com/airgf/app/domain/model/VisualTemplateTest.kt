package com.airgf.app.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualTemplateTest {
    @Test fun `release catalog includes feminine and masculine adults`() {
        val release = VisualTemplate.entries.filter { it.releaseEligible }

        assertTrue(release.any { it.supports(CompanionPresentation.FEMININE) })
        assertTrue(release.any { it.supports(CompanionPresentation.MASCULINE) })
        assertTrue(release.all { it.supportedPresentations.isNotEmpty() })
    }

    @Test fun `presentation filters incompatible avatars`() {
        assertTrue(VisualTemplate.MAYA.supports(CompanionPresentation.FEMININE))
        assertFalse(VisualTemplate.MAYA.supports(CompanionPresentation.MASCULINE))
        assertTrue(VisualTemplate.LEO.supports(CompanionPresentation.MASCULINE))
        assertFalse(VisualTemplate.LEO.supports(CompanionPresentation.FEMININE))
        assertTrue(VisualTemplate.LEO.supports(CompanionPresentation.NEUTRAL))
    }

    @Test fun `Japanese avatar has long hair asset and locale fallback`() {
        assertTrue(VisualTemplate.SORA.displayName.contains("Japanese"))
        assertTrue(VisualTemplate.SORA.preferredVoiceLocaleTags.first() == "ja-JP")
        assertTrue(VisualTemplate.SORA.preferredVoiceLocaleTags.contains("en-US"))
    }
}

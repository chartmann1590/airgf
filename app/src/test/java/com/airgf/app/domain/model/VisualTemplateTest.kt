package com.airgf.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualTemplateTest {
    @Test
    fun `maps legacy template names onto new avatar catalog`() {
        assertEquals(VisualTemplate.ARIADNA, VisualTemplate.fromPersistedName("ANIME_CUTE"))
        assertEquals(VisualTemplate.CATWOMAN, VisualTemplate.fromPersistedName("STYLIZED_PUNK"))
        assertEquals(VisualTemplate.RUBY, VisualTemplate.fromPersistedName("REALISTIC_AMERICAN"))
    }

    @Test
    fun `falls back to default template for unknown persisted names`() {
        assertEquals(VisualTemplate.ARIADNA, VisualTemplate.fromPersistedName("UNKNOWN_TEMPLATE"))
        assertEquals(VisualTemplate.ARIADNA, VisualTemplate.fromPersistedName(null))
    }

    @Test
    fun `avatar catalog has unique model paths`() {
        val modelPaths = VisualTemplate.entries.map { it.modelAssetPath }

        assertEquals(modelPaths.size, modelPaths.distinct().size)
    }

    @Test
    fun `bundled avatars declare nonzero expected sizes`() {
        VisualTemplate.entries
            .filter { it.deliveryMode == AvatarDeliveryMode.BUNDLED }
            .forEach { template ->
                assertTrue("${template.name} expectedSizeBytes must be positive", template.expectedSizeBytes > 0L)
            }
    }

    @Test
    fun `hayley smith is excluded from SceneView because its skeleton is over renderer limit`() {
        assertEquals(false, VisualTemplate.HAYLEY_SMITH.supportsSceneView)
    }
}

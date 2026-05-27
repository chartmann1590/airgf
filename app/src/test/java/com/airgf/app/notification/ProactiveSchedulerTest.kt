package com.airgf.app.notification

import org.junit.Assert.assertEquals
import org.junit.Test

class ProactiveSchedulerTest {
    @Test
    fun `maps notification frequency to expected work intervals`() {
        assertEquals(FrequencySpec(8, 4), notificationFrequencySpec("rarely"))
        assertEquals(FrequencySpec(4, 2), notificationFrequencySpec("sometimes"))
        assertEquals(FrequencySpec(2, 1), notificationFrequencySpec("often"))
        assertEquals(FrequencySpec(4, 2), notificationFrequencySpec("unexpected"))
    }
}

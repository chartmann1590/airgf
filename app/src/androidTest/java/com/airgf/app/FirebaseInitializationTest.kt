package com.airgf.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.perf.FirebasePerformance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FirebaseInitializationTest {

    private val context: Context =
        ApplicationProvider.getApplicationContext()

    @Test
    fun firebaseAppIsInitializedWithExpectedProject() {
        val app = FirebaseApp.getInstance()
        assertNotNull(app)
        assertEquals("airgf-companion", app.options.projectId)
    }

    @Test
    fun crashlyticsInstanceIsAvailable() {
        assertNotNull(FirebaseCrashlytics.getInstance())
    }

    @Test
    fun performanceInstanceIsAvailable() {
        assertNotNull(FirebasePerformance.getInstance())
    }
}
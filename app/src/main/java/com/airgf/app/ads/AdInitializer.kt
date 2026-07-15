package com.airgf.app.ads

import android.content.Context
import com.google.android.gms.ads.MobileAds
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Initializes the Google Mobile Ads SDK exactly once, after UMP consent is resolved.
 * Per Google's documented flow, calling MobileAds.initialize() before consent is fine
 * as long as no ad is requested/loaded before consent resolves - callers must gate
 * their first ad load on [ConsentManager.canRequestAds] regardless of init timing.
 */
@Singleton
class AdInitializer @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var initialized = CompletableDeferred<Unit>()
    private var started = false

    suspend fun ensureInitialized() {
        if (!started) {
            started = true
            MobileAds.initialize(context) {
                initialized.complete(Unit)
            }
        }
        initialized.await()
    }
}

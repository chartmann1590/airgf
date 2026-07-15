package com.airgf.app.ads

import android.app.Activity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Preloads and shows interstitials on cold app launch and on navigation between
 * the main tabs (Chat/Call/Character/Settings). A minimum cooldown between shows
 * prevents rapid tab-switching from spamming full-screen ads. Never shown to
 * subscribers - callers must check `!isSubscribed` before calling [showIfReady].
 */
@Singleton
class InterstitialAdManager @Inject constructor() {

    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false
    private var lastShownAtMillis = 0L

    fun preload(activity: Activity) {
        if (interstitialAd != null || isLoading) return
        isLoading = true
        InterstitialAd.load(
            activity,
            AdUnitIds.interstitial,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    isLoading = false
                    interstitialAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoading = false
                    interstitialAd = null
                }
            },
        )
    }

    fun showIfReady(activity: Activity, onDismissed: () -> Unit = {}) {
        val now = System.currentTimeMillis()
        val ad = interstitialAd
        if (ad == null || now - lastShownAtMillis < MIN_COOLDOWN_MILLIS) {
            onDismissed()
            preload(activity)
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                lastShownAtMillis = System.currentTimeMillis()
                preload(activity)
                onDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                interstitialAd = null
                preload(activity)
                onDismissed()
            }
        }
        ad.show(activity)
    }

    companion object {
        private const val MIN_COOLDOWN_MILLIS = 90_000L
    }
}

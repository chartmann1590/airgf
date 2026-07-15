package com.airgf.app.ads

import android.app.Activity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implements Google's documented rewarded-ad flow:
 * https://developers.google.com/admob/android/rewarded-video
 */
@Singleton
class RewardedAdManager @Inject constructor() {

    private var rewardedAd: RewardedAd? = null
    private var isLoading = false

    val isReady: Boolean get() = rewardedAd != null

    fun preload(activity: Activity) {
        if (rewardedAd != null || isLoading) return
        isLoading = true
        RewardedAd.load(
            activity,
            AdUnitIds.rewarded,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    isLoading = false
                    rewardedAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoading = false
                    rewardedAd = null
                }
            },
        )
    }

    /**
     * Shows the rewarded ad if loaded. [onEarnedReward] fires only when the user
     * watches to completion; [onClosed] always fires afterward (earned or not).
     */
    fun show(
        activity: Activity,
        onEarnedReward: () -> Unit,
        onClosed: () -> Unit,
        onNotReady: () -> Unit,
    ) {
        val ad = rewardedAd
        if (ad == null) {
            onNotReady()
            preload(activity)
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                preload(activity)
                onClosed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                rewardedAd = null
                preload(activity)
                onClosed()
            }
        }

        ad.show(activity) { _ ->
            onEarnedReward()
        }
    }

    companion object {
        const val EXPECTED_REWARD_MINUTES = 15
    }
}

package com.airgf.app.ads

import com.airgf.app.BuildConfig

/**
 * Google explicitly warns that requesting ads with real ad unit IDs on a
 * development/debug build risks AdMob account suspension for invalid traffic.
 * Debug builds always use Google's published test ad unit IDs; only release
 * builds use the real IDs sourced from local.properties/CI secrets.
 */
object AdUnitIds {
    // https://developers.google.com/admob/android/test-ads#sample_ad_units
    private const val TEST_BANNER = "ca-app-pub-3940256099942544/9214589741"
    private const val TEST_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
    private const val TEST_REWARDED = "ca-app-pub-3940256099942544/5224354917"

    val banner: String get() = if (BuildConfig.DEBUG) TEST_BANNER else BuildConfig.ADMOB_BANNER_UNIT_ID
    val interstitial: String get() = if (BuildConfig.DEBUG) TEST_INTERSTITIAL else BuildConfig.ADMOB_INTERSTITIAL_UNIT_ID
    val rewarded: String get() = if (BuildConfig.DEBUG) TEST_REWARDED else BuildConfig.ADMOB_REWARDED_UNIT_ID
}

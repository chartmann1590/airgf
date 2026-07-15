package com.airgf.app.ads

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * Adaptive anchored banner. Hidden entirely for subscribers - callers should
 * wrap this in `if (!isSubscribed) { BannerAdView(...) }`.
 */
@Composable
fun BannerAdView(
    modifier: Modifier = Modifier,
    canRequestAds: Boolean,
) {
    if (!canRequestAds) return

    val context = LocalContext.current
    val adView = remember { AdView(context) }

    AndroidView(
        modifier = modifier.fillMaxWidth().wrapContentHeight(),
        factory = {
            adView.apply {
                setAdSize(AdSize.BANNER)
                adUnitId = AdUnitIds.banner
                loadAd(AdRequest.Builder().build())
            }
        },
    )

    DisposableEffect(Unit) {
        onDispose { adView.destroy() }
    }
}

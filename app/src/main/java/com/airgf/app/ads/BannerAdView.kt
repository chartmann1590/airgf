package com.airgf.app.ads

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * Adaptive anchored banner. Hidden entirely for subscribers - callers should
 * wrap this in `if (!isSubscribed) { BannerAdView(...) }`.
 *
 * Rendered on a distinct background with an explicit "Ad" label above the
 * creative so it can't be mistaken for app content or navigation UI, per
 * Google Play's Deceptive Ads policy.
 */
@Composable
fun BannerAdView(
    modifier: Modifier = Modifier,
    canRequestAds: Boolean,
) {
    if (!canRequestAds) return

    val context = LocalContext.current
    val adView = remember { AdView(context) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(top = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Ad",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        AndroidView(
            modifier = Modifier.fillMaxWidth().wrapContentHeight(),
            factory = {
                adView.apply {
                    setAdSize(AdSize.BANNER)
                    adUnitId = AdUnitIds.banner
                    loadAd(AdRequest.Builder().build())
                }
            },
        )
    }

    DisposableEffect(Unit) {
        onDispose { adView.destroy() }
    }
}

package com.airgf.app.presentation.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.airgf.app.ads.findActivity
import com.airgf.app.presentation.theme.Primary
import kotlinx.coroutines.suspendCancellableCoroutine

@Composable
fun SplashScreen(
    onNavigateToWelcome: () -> Unit,
    onNavigateToChat: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel(),
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = Primary)
    }

    LaunchedEffect(Unit) {
        val activity = context.findActivity()
        if (activity != null) {
            suspendCancellableCoroutine<Unit> { continuation ->
                viewModel.consentManager.requestConsentAndLoadForm(activity) {
                    if (continuation.isActive) continuation.resumeWith(Result.success(Unit))
                }
            }
            if (viewModel.consentManager.canRequestAds) {
                viewModel.ensureAdsInitialized()
                viewModel.interstitialAdManager.preload(activity)
                viewModel.rewardedAdManager.preload(activity)
            }
        }

        val complete = viewModel.userRepository.isOnboardingComplete()
        if (complete) {
            onNavigateToChat()
        } else {
            onNavigateToWelcome()
        }
    }
}

package com.airgf.app.presentation.splash

import androidx.lifecycle.ViewModel
import com.airgf.app.ads.AdInitializer
import com.airgf.app.ads.ConsentManager
import com.airgf.app.ads.InterstitialAdManager
import com.airgf.app.ads.RewardedAdManager
import com.airgf.app.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    val userRepository: UserRepository,
    val consentManager: ConsentManager,
    val interstitialAdManager: InterstitialAdManager,
    val rewardedAdManager: RewardedAdManager,
    private val adInitializer: AdInitializer,
) : ViewModel() {
    suspend fun ensureAdsInitialized() = adInitializer.ensureInitialized()
}

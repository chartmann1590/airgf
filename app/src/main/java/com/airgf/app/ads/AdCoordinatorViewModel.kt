package com.airgf.app.ads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airgf.app.domain.repository.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Shared across the nav graph so interstitial cooldown/preload state survives
 * screen transitions between the four main tabs.
 */
@HiltViewModel
class AdCoordinatorViewModel @Inject constructor(
    val interstitialAdManager: InterstitialAdManager,
    val consentManager: ConsentManager,
    private val adInitializer: AdInitializer,
    private val subscriptionRepository: SubscriptionRepository,
) : ViewModel() {

    private val _isSubscribed = MutableStateFlow(false)
    val isSubscribed: StateFlow<Boolean> = _isSubscribed.asStateFlow()

    init {
        viewModelScope.launch {
            subscriptionRepository.isSubscribedFlow().collect { _isSubscribed.value = it }
        }
    }

    suspend fun ensureAdsInitialized() {
        adInitializer.ensureInitialized()
    }
}

package com.airgf.app.domain.usecase

import com.airgf.app.domain.repository.GfConfigRepository
import com.airgf.app.domain.repository.SubscriptionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Spicy Mode is off by default. It's only active when either:
 * - the user is subscribed AND has manually toggled it on (permanent, user-controlled), or
 * - a rewarded-ad credit window is currently active (temporary, granted by watching ads).
 */
class GetEffectiveSpicyModeUseCase @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository,
    private val gfConfigRepository: GfConfigRepository,
) {
    fun flow(): Flow<Boolean> = combine(
        gfConfigRepository.getProfileFlow(),
        subscriptionRepository.isSubscribedFlow(),
        subscriptionRepository.spicyModeGrantedUntilFlow(),
    ) { gf, subscribed, grantedUntil ->
        val manuallyEnabled = gf?.spicyModeEnabled == true
        val creditActive = grantedUntil != null && grantedUntil > System.currentTimeMillis()
        (subscribed && manuallyEnabled) || creditActive
    }

    suspend operator fun invoke(): Boolean = flow().first()
}

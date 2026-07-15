package com.airgf.app.domain.usecase

import com.airgf.app.domain.repository.SubscriptionRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Called when a rewarded ad's earned-reward callback fires. Grants credit minutes
 * toward the daily Spicy Mode cap (see [com.airgf.app.data.local.datastore.UserPreferences.MAX_SPICY_CREDIT_MINUTES_PER_DAY]).
 */
class GrantSpicyModeCreditUseCase @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository,
) {
    suspend operator fun invoke(): Result {
        val remaining = subscriptionRepository.spicyCreditMinutesRemainingTodayFlow().first()
        if (remaining <= 0) return Result.DailyCapReached
        subscriptionRepository.grantSpicyModeCreditMinutes(CREDIT_MINUTES_PER_AD.coerceAtMost(remaining))
        return Result.Granted
    }

    enum class Result { Granted, DailyCapReached }

    companion object {
        const val CREDIT_MINUTES_PER_AD = 15
    }
}

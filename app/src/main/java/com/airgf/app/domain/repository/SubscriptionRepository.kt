package com.airgf.app.domain.repository

import android.app.Activity
import com.android.billingclient.api.ProductDetails
import kotlinx.coroutines.flow.Flow

interface SubscriptionRepository {
    fun isSubscribedFlow(): Flow<Boolean>
    suspend fun isSubscribed(): Boolean
    fun spicyModeGrantedUntilFlow(): Flow<Long?>
    fun spicyCreditMinutesRemainingTodayFlow(): Flow<Int>
    suspend fun grantSpicyModeCreditMinutes(minutes: Int)
    fun subscriptionProductDetailsFlow(): Flow<ProductDetails?>
    fun launchPurchaseFlow(activity: Activity)
}

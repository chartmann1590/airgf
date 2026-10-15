package com.airgf.app.data.repository

import android.app.Activity
import android.content.Context
import com.airgf.app.data.local.datastore.UserPreferences
import com.airgf.app.domain.repository.SubscriptionRepository
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetailsResponseListener
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single bundled subscription: unlocks Spicy Mode permanently and removes all ads.
 * Product must be created in Play Console (Monetize > Products > Subscriptions)
 * with this exact ID before purchases can be tested.
 */
@Singleton
class SubscriptionRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferences: UserPreferences,
) : SubscriptionRepository, PurchasesUpdatedListener {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _productDetails = MutableStateFlow<ProductDetails?>(null)

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    init {
        startConnection()
    }

    private fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProductDetails()
                    queryExistingPurchases()
                }
            }

            override fun onBillingServiceDisconnected() {
                // Billing library recommends retrying with backoff; a fresh call to
                // queryExistingPurchases()/launchPurchaseFlow() will implicitly reconnect
                // on the next explicit user action since startConnection() is idempotent.
            }
        })
    }

    private fun queryProductDetails() {
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(SUBSCRIPTION_PRODUCT_ID)
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(product))
            .build()
        billingClient.queryProductDetailsAsync(
            params,
            object : ProductDetailsResponseListener {
                override fun onProductDetailsResponse(
                    result: BillingResult,
                    productDetailsList: MutableList<ProductDetails>,
                ) {
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        _productDetails.value = productDetailsList.firstOrNull()
                    }
                }
            },
        )
    }

    private fun queryExistingPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                handlePurchases(purchases)
            }
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            handlePurchases(purchases.orEmpty())
        }
    }

    private fun handlePurchases(purchases: List<Purchase>) {
        repositoryScope.launch {
            val active = purchases.any { it.purchaseState == Purchase.PurchaseState.PURCHASED }
            userPreferences.setSubscribed(active)
            purchases.forEach { acknowledgeIfNeeded(it) }
        }
    }

    private fun acknowledgeIfNeeded(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient.acknowledgePurchase(params) { }
        }
    }

    override fun isSubscribedFlow(): Flow<Boolean> = userPreferences.isSubscribed

    override suspend fun isSubscribed(): Boolean = userPreferences.isSubscribed.first()

    override fun spicyModeGrantedUntilFlow(): Flow<Long?> = userPreferences.spicyModeGrantedUntil

    override fun spicyCreditMinutesRemainingTodayFlow(): Flow<Int> =
        userPreferences.spicyCreditMinutesRemainingToday

    override suspend fun grantSpicyModeCreditMinutes(minutes: Int) {
        userPreferences.addSpicyCreditMinutes(minutes)
    }

    override fun subscriptionProductDetailsFlow(): Flow<ProductDetails?> = _productDetails.asStateFlow()

    override fun launchPurchaseFlow(activity: Activity) {
        val details = _productDetails.value ?: return
        val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: return
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .setOfferToken(offerToken)
                .build(),
        )
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()
        billingClient.launchBillingFlow(activity, flowParams)
    }

    companion object {
        const val SUBSCRIPTION_PRODUCT_ID = "amoura_premium_monthly"
    }
}

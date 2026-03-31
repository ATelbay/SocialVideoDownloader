package com.socialvideodownloader.core.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.socialvideodownloader.core.domain.model.CloudTier
import com.socialvideodownloader.core.domain.repository.BillingRepository
import com.socialvideodownloader.core.domain.repository.BillingResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

// TODO: Configure product ID "cloud_history_10k" in Google Play Console as a one-time purchase
// product before releasing. Set price to ~500 ₸ (~$1 USD) with title "10,000 Cloud Records"
// and description "Unlock 10,000 cloud record capacity".

@Singleton
class PlayBillingRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : BillingRepository {
        internal val _tier = MutableStateFlow(CloudTier.FREE)
        internal var billingClient: BillingClient? = null

        init {
            setupBillingClient()
        }

        private fun setupBillingClient() {
            billingClient =
                BillingClient.newBuilder(context)
                    .setListener { billingResult, purchases ->
                        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                            for (purchase in purchases) {
                                handlePurchase(purchase)
                            }
                        }
                    }
                    .enablePendingPurchases(
                        com.android.billingclient.api.PendingPurchasesParams.newBuilder()
                            .enableOneTimeProducts()
                            .build(),
                    )
                    .build()

            startConnection()
        }

        internal fun startConnection() {
            billingClient?.startConnection(
                object : BillingClientStateListener {
                    override fun onBillingSetupFinished(billingResult: com.android.billingclient.api.BillingResult) {
                        // Connection established; restorePurchases() is called explicitly on app launch
                    }

                    override fun onBillingServiceDisconnected() {
                        // Retry connection on disconnect
                        startConnection()
                    }
                },
            )
        }

        private fun handlePurchase(purchase: Purchase) {
            if (purchase.products.contains(PRODUCT_ID) &&
                purchase.purchaseState == Purchase.PurchaseState.PURCHASED
            ) {
                if (!purchase.isAcknowledged) {
                    val params =
                        AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(purchase.purchaseToken)
                            .build()
                    billingClient?.acknowledgePurchase(params) { result ->
                        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                            _tier.value = CloudTier.PAID
                        }
                    }
                } else {
                    _tier.value = CloudTier.PAID
                }
            }
        }

        override fun observeTier(): Flow<CloudTier> = _tier.asStateFlow()

        override suspend fun restorePurchases(): CloudTier {
            val client = billingClient ?: return CloudTier.FREE
            if (!client.isReady) {
                awaitConnection(client) ?: return CloudTier.FREE
            }
            return restorePurchasesInternal(client)
        }

        private suspend fun awaitConnection(client: BillingClient): BillingClient? =
            suspendCancellableCoroutine { cont ->
                client.startConnection(
                    object : BillingClientStateListener {
                        override fun onBillingSetupFinished(result: com.android.billingclient.api.BillingResult) {
                            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                                cont.resume(client)
                            } else {
                                cont.resume(null)
                            }
                        }

                        override fun onBillingServiceDisconnected() {
                            if (cont.isActive) cont.resume(null)
                        }
                    },
                )
            }

        private suspend fun restorePurchasesInternal(client: BillingClient): CloudTier {
            val params =
                QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            val result = client.queryPurchasesAsync(params)
            return restorePurchasesFromList(result.purchasesList)
        }

        /**
         * Determines tier from a purchase list and updates internal state.
         * Exposed as internal for testing.
         */
        internal fun restorePurchasesFromList(purchases: List<Purchase>): CloudTier {
            val paid =
                purchases.any { purchase ->
                    purchase.products.contains(PRODUCT_ID) &&
                        purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                        purchase.isAcknowledged
                }
            val tier = if (paid) CloudTier.PAID else CloudTier.FREE
            _tier.value = tier
            return tier
        }

        override suspend fun launchPurchaseFlow(activityRef: Any): BillingResult {
            val activity =
                activityRef as? Activity
                    ?: return BillingResult.Error("Invalid activity reference")

            val client = billingClient ?: return BillingResult.Error("Billing client not initialized")

            if (!client.isReady) {
                awaitConnection(client) ?: return BillingResult.Error("Billing service unavailable")
            }

            val productList =
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build(),
                )

            val queryParams =
                QueryProductDetailsParams.newBuilder()
                    .setProductList(productList)
                    .build()

            val detailsResult = client.queryProductDetails(queryParams)
            if (detailsResult.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                return mapBillingResponseCode(detailsResult.billingResult.responseCode)
            }

            val productDetails =
                detailsResult.productDetailsList?.firstOrNull()
                    ?: return BillingResult.Error("Product not found. Ensure '$PRODUCT_ID' is configured in Google Play Console.")

            val productDetailsParams =
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .build()

            val billingFlowParams =
                BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(listOf(productDetailsParams))
                    .build()

            return suspendCancellableCoroutine { cont ->
                val purchasesUpdatedListener =
                    com.android.billingclient.api.PurchasesUpdatedListener { result, purchases ->
                        val mappedResult =
                            if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                                for (purchase in purchases) {
                                    handlePurchase(purchase)
                                }
                                BillingResult.Success
                            } else {
                                mapBillingResponseCode(result.responseCode)
                            }
                        if (cont.isActive) cont.resume(mappedResult)
                    }

                // Re-build client with one-shot listener for this flow
                val flowClient =
                    BillingClient.newBuilder(context)
                        .setListener(purchasesUpdatedListener)
                        .enablePendingPurchases(
                            com.android.billingclient.api.PendingPurchasesParams.newBuilder()
                                .enableOneTimeProducts()
                                .build(),
                        )
                        .build()

                flowClient.startConnection(
                    object : BillingClientStateListener {
                        override fun onBillingSetupFinished(setupResult: com.android.billingclient.api.BillingResult) {
                            if (setupResult.responseCode == BillingClient.BillingResponseCode.OK) {
                                flowClient.launchBillingFlow(activity, billingFlowParams)
                            } else {
                                if (cont.isActive) cont.resume(mapBillingResponseCode(setupResult.responseCode))
                            }
                        }

                        override fun onBillingServiceDisconnected() {
                            if (cont.isActive) cont.resume(BillingResult.Error("Billing service disconnected"))
                        }
                    },
                )
            }
        }

        companion object {
            const val PRODUCT_ID = "cloud_history_10k"

            fun mapBillingResponseCode(responseCode: Int): BillingResult =
                when (responseCode) {
                    BillingClient.BillingResponseCode.OK -> BillingResult.Success
                    BillingClient.BillingResponseCode.USER_CANCELED -> BillingResult.Cancelled
                    else -> BillingResult.Error("Billing error code: $responseCode")
                }
        }
    }

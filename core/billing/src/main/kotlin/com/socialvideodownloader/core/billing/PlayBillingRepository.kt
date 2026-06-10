package com.socialvideodownloader.core.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.socialvideodownloader.core.domain.model.CloudTier
import com.socialvideodownloader.core.domain.repository.BillingRepository
import com.socialvideodownloader.core.domain.repository.BillingResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

// TODO: Configure product ID "cloud_history_10k" in Google Play Console as a one-time purchase
// product before releasing. Set price to ~500 ₸ (~$1 USD) with title "10,000 Cloud Records"
// and description "Unlock 10,000 cloud record capacity".

/**
 * Google Play Billing implementation backed by a single, long-lived [BillingClient].
 *
 * Connection is established lazily and de-duplicated via [ensureConnected]; purchase results are
 * delivered through one [PurchasesUpdatedListener] and bridged back to the suspending
 * [launchPurchaseFlow] caller via [pendingPurchaseResult]. Purchases are signature-verified
 * (see [PurchaseSecurity]) and unacknowledged ones are acknowledged on restore so Google does not
 * auto-refund them.
 */
@Singleton
class PlayBillingRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : BillingRepository,
        PurchaseFlowLauncher {
        internal val _tier = MutableStateFlow(CloudTier.FREE)
        internal var billingClient: BillingClient? = null

        private val connectionMutex = Mutex()
        private var connectionDeferred: CompletableDeferred<Boolean>? = null

        /** Bridges the one-shot purchase result from the listener back to [launchPurchaseFlow]. */
        @Volatile
        private var pendingPurchaseResult: CompletableDeferred<BillingResult>? = null

        init {
            setupBillingClient()
        }

        private fun setupBillingClient() {
            billingClient =
                BillingClient.newBuilder(context)
                    .setListener(PurchasesUpdatedListener(::handlePurchasesUpdate))
                    .enablePendingPurchases(
                        PendingPurchasesParams.newBuilder()
                            .enableOneTimeProducts()
                            .build(),
                    )
                    .build()
        }

        /**
         * Ensures the single [billingClient] is connected, de-duplicating concurrent attempts so we
         * never call [BillingClient.startConnection] more than once at a time.
         */
        private suspend fun ensureConnected(): Boolean {
            val client = billingClient ?: return false
            if (client.isReady) return true
            val deferred =
                connectionMutex.withLock {
                    connectionDeferred?.takeIf { !it.isCompleted }
                        ?: CompletableDeferred<Boolean>().also { newDeferred ->
                            connectionDeferred = newDeferred
                            client.startConnection(
                                object : BillingClientStateListener {
                                    override fun onBillingSetupFinished(result: com.android.billingclient.api.BillingResult) {
                                        if (newDeferred.isActive) {
                                            newDeferred.complete(
                                                result.responseCode == BillingClient.BillingResponseCode.OK,
                                            )
                                        }
                                    }

                                    override fun onBillingServiceDisconnected() {
                                        if (newDeferred.isActive) newDeferred.complete(false)
                                    }
                                },
                            )
                        }
                }
            return deferred.await()
        }

        private fun handlePurchasesUpdate(
            billingResult: com.android.billingclient.api.BillingResult,
            purchases: List<Purchase>?,
        ) {
            val deferred = pendingPurchaseResult
            pendingPurchaseResult = null
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                var outcome: BillingResult = BillingResult.Success
                for (purchase in purchases) {
                    if (handlePurchase(purchase) == BillingResult.Pending) {
                        outcome = BillingResult.Pending
                    }
                }
                deferred?.complete(outcome)
            } else {
                deferred?.complete(mapBillingResponseCode(billingResult.responseCode))
            }
        }

        /**
         * Verifies and applies a single purchase. Acknowledges (asynchronously) and flips the tier
         * to PAID for completed purchases. Returns the [BillingResult] describing this purchase.
         */
        private fun handlePurchase(purchase: Purchase): BillingResult {
            if (!purchase.products.contains(PRODUCT_ID)) return BillingResult.Error("Unknown product")
            if (!PurchaseSecurity.verifyPurchase(purchase.originalJson, purchase.signature)) {
                return BillingResult.Error("Purchase verification failed")
            }
            return when (purchase.purchaseState) {
                Purchase.PurchaseState.PURCHASED -> {
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
                    BillingResult.Success
                }

                Purchase.PurchaseState.PENDING -> BillingResult.Pending
                else -> BillingResult.Error("Purchase not completed")
            }
        }

        override fun observeTier(): Flow<CloudTier> = _tier.asStateFlow()

        override suspend fun restorePurchases(): CloudTier {
            val client = billingClient ?: return CloudTier.FREE
            if (!ensureConnected()) return CloudTier.FREE
            return restorePurchasesInternal(client)
        }

        private suspend fun restorePurchasesInternal(client: BillingClient): CloudTier {
            val params =
                QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            val result = client.queryPurchasesAsync(params)
            val verified =
                result.purchasesList.filter {
                    PurchaseSecurity.verifyPurchase(it.originalJson, it.signature)
                }
            // Acknowledge owned-but-unacknowledged purchases so Google does not auto-refund them
            // after 3 days. restorePurchasesFromList grants access on ownership alone.
            verified
                .filter {
                    it.products.contains(PRODUCT_ID) &&
                        it.purchaseState == Purchase.PurchaseState.PURCHASED &&
                        !it.isAcknowledged
                }
                .forEach { acknowledgePurchaseSuspend(client, it) }
            return restorePurchasesFromList(verified)
        }

        private suspend fun acknowledgePurchaseSuspend(
            client: BillingClient,
            purchase: Purchase,
        ): Boolean =
            suspendCancellableCoroutine { cont ->
                val params =
                    AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                client.acknowledgePurchase(params) { result ->
                    if (cont.isActive) {
                        cont.resume(result.responseCode == BillingClient.BillingResponseCode.OK)
                    }
                }
            }

        /**
         * Determines tier from a (signature-verified) purchase list and updates internal state.
         * Grants PAID on ownership of a completed purchase; acknowledgement is handled separately
         * in [restorePurchasesInternal]. Exposed as internal for testing.
         */
        internal fun restorePurchasesFromList(purchases: List<Purchase>): CloudTier {
            val paid =
                purchases.any { purchase ->
                    purchase.products.contains(PRODUCT_ID) &&
                        purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                }
            val tier = if (paid) CloudTier.PAID else CloudTier.FREE
            _tier.value = tier
            return tier
        }

        override suspend fun launchPurchaseFlow(activity: Activity): BillingResult {
            val client = billingClient ?: return BillingResult.Error("Billing client not initialized")
            if (!ensureConnected()) return BillingResult.Error("Billing service unavailable")

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

            val deferred = CompletableDeferred<BillingResult>()
            // Supersede any still-pending flow before replacing the bridge, so an
            // earlier launchPurchaseFlow() caller never hangs on await() if a second
            // purchase is started before the first delivered its result.
            pendingPurchaseResult
                ?.takeIf { !it.isCompleted }
                ?.complete(BillingResult.Error("Superseded by a new purchase flow"))
            pendingPurchaseResult = deferred
            val launchResult = client.launchBillingFlow(activity, billingFlowParams)
            if (launchResult.responseCode != BillingClient.BillingResponseCode.OK) {
                pendingPurchaseResult = null
                return mapBillingResponseCode(launchResult.responseCode)
            }
            return deferred.await()
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

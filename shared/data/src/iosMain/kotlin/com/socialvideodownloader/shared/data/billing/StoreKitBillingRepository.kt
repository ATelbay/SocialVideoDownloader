package com.socialvideodownloader.shared.data.billing

import com.socialvideodownloader.core.domain.model.CloudTier
import com.socialvideodownloader.core.domain.repository.BillingRepository
import com.socialvideodownloader.core.domain.repository.BillingResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * iOS implementation of [BillingRepository] using StoreKit 2 via [PlatformBillingProvider].
 *
 * StoreKit 2 is a Swift-native API. All actual StoreKit calls are delegated to the
 * [PlatformBillingProvider] bridge interface, which is implemented in Swift.
 *
 * Product IDs (to be configured in App Store Connect):
 *   - `com.socialvideodownloader.premium.monthly` — Monthly Premium subscription
 *   - `com.socialvideodownloader.premium.annual`  — Annual Premium subscription (optional)
 *
 * The current tier is observed reactively via [observeTier]. Updates are triggered when:
 *   - The app launches and [restorePurchases] is called
 *   - A successful purchase completes via [launchPurchaseFlow]
 *   - A subscription lapses (detected on next app launch or via StoreKit transaction listener)
 *
 * TODO: Implement transaction listener in Swift to push real-time entitlement changes.
 *       Until then, tier updates only propagate when explicitly refreshed.
 */
class StoreKitBillingRepository(
    private val billingProvider: PlatformBillingProvider,
) : BillingRepository {

    private val _currentTier = MutableStateFlow<CloudTier>(CloudTier.FREE)

    /**
     * Observe the current [CloudTier] reactively.
     *
     * Emits immediately with the cached tier. The tier is refreshed by calling
     * [restorePurchases] on app launch.
     *
     * TODO: Add a StoreKit 2 transaction listener in Swift that calls back into Kotlin
     * whenever an entitlement changes, enabling real-time updates.
     */
    override fun observeTier(): Flow<CloudTier> = _currentTier.asStateFlow()

    /**
     * Check and restore active purchases. Called on app launch to sync entitlements.
     *
     * This calls StoreKit 2's Transaction.currentEntitlements to determine the user's
     * current subscription status without initiating a purchase flow.
     *
     * Returns the current [CloudTier] and updates the observed tier flow.
     */
    override suspend fun restorePurchases(): CloudTier {
        // TODO: Call AppStore.sync() + re-check currentEntitlements via billingProvider.
        val tier = billingProvider.restorePurchases()
        _currentTier.value = tier
        return tier
    }

    /**
     * Initiate the StoreKit 2 purchase flow.
     *
     * On iOS, [activityRef] is ignored — StoreKit 2 presents its own UI on top
     * of the key window without needing an Activity reference.
     *
     * Updates [observeTier] on successful purchase.
     */
    override suspend fun launchPurchaseFlow(activityRef: Any): BillingResult {
        // Note: activityRef is an Android concept. On iOS, StoreKit manages its own window.
        // TODO: Call billingProvider.purchase() which calls StoreKit 2 Product.purchase().
        val result = billingProvider.purchase()
        if (result is BillingResult.Success) {
            // Refresh the tier after successful purchase
            val tier = billingProvider.getCurrentTier()
            _currentTier.value = tier
        }
        return result
    }
}

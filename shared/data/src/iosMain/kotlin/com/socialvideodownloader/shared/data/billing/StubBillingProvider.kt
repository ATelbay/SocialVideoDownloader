package com.socialvideodownloader.shared.data.billing

import com.socialvideodownloader.core.domain.model.CloudTier
import com.socialvideodownloader.core.domain.repository.BillingResult

/**
 * Stub [PlatformBillingProvider] used until the Swift StoreKit 2 implementation is wired.
 *
 * Always reports [CloudTier.FREE] and returns [BillingResult.Error] for purchase attempts.
 * Safe for use in development and the simulator.
 *
 * Replace by registering the Swift `StoreKitBillingProvider` via `KoinHelper.registerBillingProvider()`
 * when the App Store Connect products are configured.
 */
class StubBillingProvider : PlatformBillingProvider {
    override suspend fun getCurrentTier(): CloudTier = CloudTier.FREE

    override suspend fun purchase(): BillingResult =
        BillingResult.Error("StoreKit not configured — implement StoreKitBillingProvider in Swift")

    override suspend fun restorePurchases(): CloudTier = CloudTier.FREE
}

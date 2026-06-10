package com.socialvideodownloader.core.domain.repository

import com.socialvideodownloader.core.domain.model.CloudTier
import kotlinx.coroutines.flow.Flow

interface BillingRepository {
    /** Observe current tier (reactive, updates on purchase/refund). */
    fun observeTier(): Flow<CloudTier>

    /** Check and restore purchases on app launch. */
    suspend fun restorePurchases(): CloudTier
}

/**
 * Result of a platform purchase flow.
 *
 * The purchase flow itself is launched through a platform-specific entry point
 * (Android: PurchaseFlowLauncher with an Activity; iOS: StoreKit) rather than
 * this domain repository, so the contract stays free of platform UI types.
 */
sealed interface BillingResult {
    data object Success : BillingResult

    data object Cancelled : BillingResult

    /** Purchase acknowledged by the store but not yet completed (e.g. awaiting payment). */
    data object Pending : BillingResult

    data class Error(val message: String) : BillingResult
}

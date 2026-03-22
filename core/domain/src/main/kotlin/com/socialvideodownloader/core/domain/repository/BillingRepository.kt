package com.socialvideodownloader.core.domain.repository

import com.socialvideodownloader.core.domain.model.CloudTier
import kotlinx.coroutines.flow.Flow

interface BillingRepository {
    /** Observe current tier (reactive, updates on purchase/refund). */
    fun observeTier(): Flow<CloudTier>

    /** Check and restore purchases on app launch. */
    suspend fun restorePurchases(): CloudTier

    /**
     * Initiate purchase flow.
     * [activityRef] is expected to be an android.app.Activity at runtime;
     * typed as Any here to keep :core:domain free of Android SDK.
     */
    suspend fun launchPurchaseFlow(activityRef: Any): BillingResult
}

sealed interface BillingResult {
    data object Success : BillingResult

    data object Cancelled : BillingResult

    data class Error(val message: String) : BillingResult
}

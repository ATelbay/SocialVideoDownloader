package com.socialvideodownloader.core.billing

import android.app.Activity
import com.socialvideodownloader.core.domain.repository.BillingResult

/**
 * Android-only entry point for launching the Google Play Billing purchase flow.
 *
 * This is intentionally kept out of [com.socialvideodownloader.core.domain.repository.BillingRepository]
 * so the domain contract stays free of platform UI types: launching the Play
 * billing flow requires a real [Activity], which only exists on Android.
 */
interface PurchaseFlowLauncher {
    /** Launch the purchase flow for the upgrade product, hosted by [activity]. */
    suspend fun launchPurchaseFlow(activity: Activity): BillingResult
}

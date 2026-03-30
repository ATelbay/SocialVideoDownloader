package com.socialvideodownloader.shared.data.billing

import com.socialvideodownloader.core.domain.model.CloudTier
import com.socialvideodownloader.core.domain.repository.BillingResult

/**
 * Bridge interface implemented by Swift using StoreKit 2 to provide in-app purchase
 * capabilities to the Kotlin layer.
 *
 * StoreKit 2 is a Swift-native async/await API and cannot be called from Kotlin/Native
 * without bridging. This interface decouples the Kotlin billing implementation from
 * the Swift StoreKit calls.
 *
 * Swift implementation outline:
 * ```swift
 * class StoreKitBillingProvider: PlatformBillingProvider {
 *     // Product IDs from App Store Connect
 *     static let premiumProductId = "com.socialvideodownloader.premium.monthly"
 *
 *     func getCurrentTier() async -> CloudTier {
 *         // Check Transaction.currentEntitlements for active subscription
 *         for await result in Transaction.currentEntitlements {
 *             if case .verified(let transaction) = result,
 *                transaction.productID == Self.premiumProductId {
 *                 return CloudTier.paid
 *             }
 *         }
 *         return CloudTier.free
 *     }
 *
 *     func purchase() async -> BillingResult {
 *         let products = try await Product.products(for: [Self.premiumProductId])
 *         guard let product = products.first else {
 *             return BillingResult.Error(message: "Product not found")
 *         }
 *         let result = try await product.purchase()
 *         switch result {
 *         case .success: return BillingResult.Success()
 *         case .userCancelled: return BillingResult.Cancelled()
 *         default: return BillingResult.Error(message: "Purchase failed")
 *         }
 *     }
 * }
 * ```
 *
 * TODO: Implement in Swift and register in KoinHelper.
 */
interface PlatformBillingProvider {
    /**
     * Returns the current [CloudTier] based on active in-app purchases.
     * Checks StoreKit 2 Transaction.currentEntitlements for an active subscription.
     */
    suspend fun getCurrentTier(): CloudTier

    /**
     * Initiates the StoreKit 2 purchase flow for the premium tier product.
     * Returns [BillingResult] indicating success, cancellation, or error.
     */
    suspend fun purchase(): BillingResult

    /**
     * Restores previous purchases (calls StoreKit 2 AppStore.sync()).
     * Returns the current [CloudTier] after restoration.
     */
    suspend fun restorePurchases(): CloudTier
}

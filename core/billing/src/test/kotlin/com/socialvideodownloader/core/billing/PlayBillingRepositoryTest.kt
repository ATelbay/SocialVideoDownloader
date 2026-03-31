package com.socialvideodownloader.core.billing

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase
import com.socialvideodownloader.core.domain.model.CloudTier
import com.socialvideodownloader.core.domain.repository.BillingResult
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

/**
 * Tests for PlayBillingRepository billing logic.
 *
 * BillingClient is a final Android class that's hard to construct in unit tests.
 * These tests exercise the logic methods that are exposed internally:
 * - restorePurchasesFromList: tier determination from a purchase list
 * - mapBillingResponseCode: BillingResult mapping
 *
 * The tier state is tested via direct state inspection via the internal _tier field.
 */
class PlayBillingRepositoryTest {
    /**
     * Creates a minimal PlayBillingRepository for testing by bypassing the
     * constructor using reflection (avoids needing a real Android Context).
     */
    private fun createTestRepo(): PlayBillingRepository {
        val unsafe =
            Class.forName("sun.misc.Unsafe")
                .getDeclaredField("theUnsafe")
                .also { it.isAccessible = true }
                .get(null)
        val allocateInstance = unsafe.javaClass.getMethod("allocateInstance", Class::class.java)
        val repo = allocateInstance.invoke(unsafe, PlayBillingRepository::class.java) as PlayBillingRepository

        // Initialize internal state manually
        val tierField = PlayBillingRepository::class.java.getDeclaredField("_tier")
        tierField.isAccessible = true
        tierField.set(repo, MutableStateFlow(CloudTier.FREE))

        val clientField = PlayBillingRepository::class.java.getDeclaredField("billingClient")
        clientField.isAccessible = true
        clientField.set(repo, null)

        return repo
    }

    @Test
    fun `initial tier is FREE`() =
        runTest {
            val repo = createTestRepo()
            val tierField = PlayBillingRepository::class.java.getDeclaredField("_tier")
            tierField.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val tier = (tierField.get(repo) as MutableStateFlow<CloudTier>).value
            assertEquals(CloudTier.FREE, tier)
        }

    @Test
    fun `restorePurchases returns FREE when no purchase found`() =
        runTest {
            val repo = createTestRepo()
            val result = repo.restorePurchasesFromList(emptyList())
            assertEquals(CloudTier.FREE, result)
        }

    @Test
    fun `restorePurchases returns PAID when cloud_history_10k purchase is acknowledged`() =
        runTest {
            val repo = createTestRepo()

            val purchase =
                mockk<Purchase> {
                    every { products } returns listOf(PlayBillingRepository.PRODUCT_ID)
                    every { purchaseState } returns Purchase.PurchaseState.PURCHASED
                    every { isAcknowledged } returns true
                }

            val result = repo.restorePurchasesFromList(listOf(purchase))
            assertEquals(CloudTier.PAID, result)
        }

    @Test
    fun `restorePurchases returns FREE when purchase is not acknowledged`() =
        runTest {
            val repo = createTestRepo()

            val purchase =
                mockk<Purchase> {
                    every { products } returns listOf(PlayBillingRepository.PRODUCT_ID)
                    every { purchaseState } returns Purchase.PurchaseState.PURCHASED
                    every { isAcknowledged } returns false
                }

            val result = repo.restorePurchasesFromList(listOf(purchase))
            assertEquals(CloudTier.FREE, result)
        }

    @Test
    fun `restorePurchasesFromList with empty list simulates refund returns FREE`() =
        runTest {
            val repo = createTestRepo()
            // A refunded purchase no longer appears in queryPurchasesAsync
            val result = repo.restorePurchasesFromList(emptyList())
            assertEquals(CloudTier.FREE, result)
        }

    @Test
    fun `mapBillingResponseCode maps OK to Success`() {
        val result = PlayBillingRepository.mapBillingResponseCode(BillingClient.BillingResponseCode.OK)
        assertInstanceOf(BillingResult.Success::class.java, result)
    }

    @Test
    fun `mapBillingResponseCode maps USER_CANCELED to Cancelled`() {
        val result = PlayBillingRepository.mapBillingResponseCode(BillingClient.BillingResponseCode.USER_CANCELED)
        assertInstanceOf(BillingResult.Cancelled::class.java, result)
    }

    @Test
    fun `mapBillingResponseCode maps SERVICE_UNAVAILABLE to Error`() {
        val result = PlayBillingRepository.mapBillingResponseCode(BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE)
        assertInstanceOf(BillingResult.Error::class.java, result)
    }
}

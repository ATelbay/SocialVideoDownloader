package com.socialvideodownloader.core.domain.sync

import app.cash.turbine.test
import com.socialvideodownloader.core.domain.model.CloudTier
import com.socialvideodownloader.core.domain.repository.BillingRepository
import com.socialvideodownloader.core.domain.repository.CloudBackupRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ObserveCloudCapacityUseCaseTest {
    private lateinit var cloudBackupRepository: CloudBackupRepository
    private lateinit var billingRepository: BillingRepository
    private lateinit var useCase: ObserveCloudCapacityUseCase

    private val tierFlow = MutableStateFlow(CloudTier.FREE)

    @BeforeEach
    fun setup() {
        cloudBackupRepository = mockk()
        billingRepository = mockk()
        coEvery { billingRepository.observeTier() } returns tierFlow
        useCase = ObserveCloudCapacityUseCase(cloudBackupRepository, billingRepository)
    }

    @Test
    fun `combines cloud record count with tier limit to emit capacity`() =
        runTest {
            coEvery { cloudBackupRepository.getCloudRecordCount() } returns 500

            useCase().test {
                val capacity = awaitItem()
                assertEquals(500, capacity.used)
                assertEquals(CloudTier.FREE.maxRecords, capacity.limit)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `isNearLimit is false when count is below 90 percent of limit`() =
        runTest {
            coEvery { cloudBackupRepository.getCloudRecordCount() } returns 800

            useCase().test {
                val capacity = awaitItem()
                // 800 / 1000 = 80%, not near limit
                assertFalse(capacity.isNearLimit)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `isNearLimit is true when count is exactly 90 percent of limit`() =
        runTest {
            coEvery { cloudBackupRepository.getCloudRecordCount() } returns 900

            useCase().test {
                val capacity = awaitItem()
                // 900 / 1000 = 90%, at threshold
                assertTrue(capacity.isNearLimit)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `isNearLimit is true when count is above 90 percent of limit`() =
        runTest {
            coEvery { cloudBackupRepository.getCloudRecordCount() } returns 950

            useCase().test {
                val capacity = awaitItem()
                assertTrue(capacity.isNearLimit)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `tier change updates the capacity info`() =
        runTest {
            coEvery { cloudBackupRepository.getCloudRecordCount() } returns 950

            useCase().test {
                val freeTierCapacity = awaitItem()
                assertEquals(CloudTier.FREE.maxRecords, freeTierCapacity.limit)
                assertTrue(freeTierCapacity.isNearLimit)

                tierFlow.value = CloudTier.PAID

                val paidTierCapacity = awaitItem()
                assertEquals(CloudTier.PAID.maxRecords, paidTierCapacity.limit)
                // 950 / 10000 = 9.5%, not near limit
                assertFalse(paidTierCapacity.isNearLimit)
                cancelAndIgnoreRemainingEvents()
            }
        }
}

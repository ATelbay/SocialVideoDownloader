package com.socialvideodownloader.core.domain.sync

import app.cash.turbine.test
import com.socialvideodownloader.core.domain.fake.FakeBillingRepository
import com.socialvideodownloader.core.domain.fake.FakeCloudBackupRepository
import com.socialvideodownloader.core.domain.model.CloudTier
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ObserveCloudCapacityUseCaseTest {
    private lateinit var cloudBackupRepository: FakeCloudBackupRepository
    private lateinit var billingRepository: FakeBillingRepository
    private lateinit var useCase: ObserveCloudCapacityUseCase

    @BeforeTest
    fun setup() {
        cloudBackupRepository = FakeCloudBackupRepository()
        billingRepository = FakeBillingRepository()
        useCase = ObserveCloudCapacityUseCase(cloudBackupRepository, billingRepository)
    }

    @Test
    fun combinesCloudRecordCountWithTierLimitToEmitCapacity() =
        runTest {
            cloudBackupRepository.cloudRecordCount = 500

            useCase().test {
                val capacity = awaitItem()
                assertEquals(500, capacity.used)
                assertEquals(CloudTier.FREE.maxRecords, capacity.limit)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun isNearLimitIsFalseWhenCountIsBelow90PercentOfLimit() =
        runTest {
            cloudBackupRepository.cloudRecordCount = 800

            useCase().test {
                val capacity = awaitItem()
                // 800 / 1000 = 80%, not near limit
                assertFalse(capacity.isNearLimit)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun isNearLimitIsTrueWhenCountIsExactly90PercentOfLimit() =
        runTest {
            cloudBackupRepository.cloudRecordCount = 900

            useCase().test {
                val capacity = awaitItem()
                // 900 / 1000 = 90%, at threshold
                assertTrue(capacity.isNearLimit)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun isNearLimitIsTrueWhenCountIsAbove90PercentOfLimit() =
        runTest {
            cloudBackupRepository.cloudRecordCount = 950

            useCase().test {
                val capacity = awaitItem()
                assertTrue(capacity.isNearLimit)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun tierChangeUpdatesTheCapacityInfo() =
        runTest {
            cloudBackupRepository.cloudRecordCount = 950

            useCase().test {
                val freeTierCapacity = awaitItem()
                assertEquals(CloudTier.FREE.maxRecords, freeTierCapacity.limit)
                assertTrue(freeTierCapacity.isNearLimit)

                billingRepository.tierFlow.value = CloudTier.PAID

                val paidTierCapacity = awaitItem()
                assertEquals(CloudTier.PAID.maxRecords, paidTierCapacity.limit)
                // 950 / 10000 = 9.5%, not near limit
                assertFalse(paidTierCapacity.isNearLimit)
                cancelAndIgnoreRemainingEvents()
            }
        }
}

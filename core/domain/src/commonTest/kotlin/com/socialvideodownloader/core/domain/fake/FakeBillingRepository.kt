package com.socialvideodownloader.core.domain.fake

import com.socialvideodownloader.core.domain.model.CloudTier
import com.socialvideodownloader.core.domain.repository.BillingRepository
import com.socialvideodownloader.core.domain.repository.BillingResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeBillingRepository : BillingRepository {
    val tierFlow = MutableStateFlow(CloudTier.FREE)

    override fun observeTier(): Flow<CloudTier> = tierFlow

    override suspend fun restorePurchases(): CloudTier = tierFlow.value

    override suspend fun launchPurchaseFlow(activityRef: Any): BillingResult = BillingResult.Success
}

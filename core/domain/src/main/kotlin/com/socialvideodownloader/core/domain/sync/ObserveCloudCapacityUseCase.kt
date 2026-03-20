package com.socialvideodownloader.core.domain.sync

import com.socialvideodownloader.core.domain.repository.BillingRepository
import com.socialvideodownloader.core.domain.repository.CloudBackupRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

data class CloudCapacity(
    val used: Int,
    val limit: Int,
    val isNearLimit: Boolean, // used >= limit * 0.9
)

class ObserveCloudCapacityUseCase @Inject constructor(
    private val cloudBackupRepository: CloudBackupRepository,
    private val billingRepository: BillingRepository,
) {
    operator fun invoke(): Flow<CloudCapacity> =
        billingRepository.observeTier().flatMapLatest { tier ->
            flow {
                val count = cloudBackupRepository.getCloudRecordCount()
                val limit = tier.maxRecords
                emit(
                    CloudCapacity(
                        used = count,
                        limit = limit,
                        isNearLimit = count >= limit * 0.9,
                    ),
                )
            }
        }
}

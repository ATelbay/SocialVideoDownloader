package com.socialvideodownloader.core.domain.model

enum class CloudTier(val maxRecords: Int) {
    FREE(maxRecords = 1000),
    PAID(maxRecords = 10000),
}

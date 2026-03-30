package com.socialvideodownloader.shared.feature.download

import java.util.UUID

internal actual fun generateUuid(): String = UUID.randomUUID().toString()

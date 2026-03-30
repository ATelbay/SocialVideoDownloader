package com.socialvideodownloader.shared.feature.download

import platform.Foundation.NSUUID

internal actual fun generateUuid(): String = NSUUID().UUIDString()

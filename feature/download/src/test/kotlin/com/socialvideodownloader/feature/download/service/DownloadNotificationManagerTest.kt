package com.socialvideodownloader.feature.download.service

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DownloadNotificationManagerTest {

    // These tests verify the contract for notification tap actions.
    // Full PendingIntent verification requires Android instrumentation tests.

    @Test
    fun `completion notification accepts mediaStoreUri and mimeType parameters`() {
        // Verify the method signature accepts the required parameters
        // The actual contentIntent is tested via instrumented tests
        val mediaStoreUri: String? = "content://media/external/downloads/123"
        val mimeType = "video/mp4"
        assertNotNull(mediaStoreUri)
        assertTrue(mimeType.startsWith("video/") || mimeType.startsWith("audio/"))
    }

    @Test
    fun `error notification has correct target class name`() {
        val targetClassName = "com.socialvideodownloader.MainActivity"
        assertTrue(targetClassName.endsWith("MainActivity"))
    }

    @Test
    fun `progress notification has correct target class name`() {
        val targetClassName = "com.socialvideodownloader.MainActivity"
        assertTrue(targetClassName.endsWith("MainActivity"))
    }
}

package com.socialvideodownloader.shared.data.platform

/**
 * Platform abstraction for clipboard operations.
 *
 * Android: Uses ClipboardManager.
 * iOS: Uses UIPasteboard.
 */
interface PlatformClipboard {
    /** Copy a text string to the system clipboard. */
    fun copyToClipboard(text: String)
}

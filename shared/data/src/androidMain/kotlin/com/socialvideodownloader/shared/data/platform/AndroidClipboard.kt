package com.socialvideodownloader.shared.data.platform

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

/**
 * Android implementation of [PlatformClipboard].
 *
 * Uses the system ClipboardManager to copy text to clipboard.
 */
class AndroidClipboard(
    private val context: Context,
) : PlatformClipboard {
    override fun copyToClipboard(text: String) {
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("url", text)
        clipboardManager.setPrimaryClip(clip)
    }
}

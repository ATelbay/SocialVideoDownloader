package com.socialvideodownloader.shared.data.platform

import platform.UIKit.UIPasteboard

/**
 * iOS implementation of [PlatformClipboard] using UIPasteboard.
 *
 * No permissions required for writing to the general pasteboard.
 */
class IosClipboard : PlatformClipboard {
    override fun copyToClipboard(text: String) {
        UIPasteboard.generalPasteboard.string = text
    }
}

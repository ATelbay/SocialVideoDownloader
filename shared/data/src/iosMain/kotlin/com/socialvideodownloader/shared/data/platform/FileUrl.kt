package com.socialvideodownloader.shared.data.platform

import platform.Foundation.NSURL

/**
 * Resolves a file path or file:// URI string into an [NSURL].
 *
 * If [uri] already has a `file://` scheme it is treated as an absolute URL;
 * otherwise it is treated as a POSIX path.
 */
fun resolveFileUrl(uri: String): NSURL =
    if (uri.startsWith("file://")) {
        NSURL(string = uri)
    } else {
        NSURL(fileURLWithPath = uri)
    }

package com.socialvideodownloader.shared.data.platform

/**
 * Single source of truth for turning an arbitrary video title into a safe file name.
 *
 * Shared across every platform storage adapter (Android MediaStore + direct file,
 * iOS Documents) so the same title produces a consistent name everywhere instead of
 * each adapter rolling its own divergent rules.
 */
object FileNames {
    private val ILLEGAL_CHARS = Regex("[/\\\\:*?\"<>|]")

    const val DEFAULT_MAX_LENGTH = 200

    /**
     * Replaces filesystem-reserved characters, strips leading dots (hidden files),
     * trims surrounding whitespace and caps the length. Never returns blank.
     */
    fun sanitize(
        name: String,
        maxLength: Int = DEFAULT_MAX_LENGTH,
    ): String =
        name
            .replace(ILLEGAL_CHARS, "_")
            .trim()
            .trimStart('.')
            .take(maxLength)
            .ifBlank { "download" }
}

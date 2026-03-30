package com.socialvideodownloader.shared.feature.library

/** One-shot side effects emitted by [SharedLibraryViewModel]. */
sealed interface LibraryEffect {
    data class OpenContent(val contentUri: String) : LibraryEffect
    data class ShareContent(val contentUri: String) : LibraryEffect
    /** Replaces @StringRes Int — platform layer resolves [LibraryMessageType] to a localized string. */
    data class ShowMessage(val messageType: LibraryMessageType) : LibraryEffect
}

/** Typed message keys replacing @StringRes Int in library effects. */
enum class LibraryMessageType {
    DELETE_SUCCESS,
    FILE_NOT_FOUND,
    SHARE_ERROR,
    OPEN_ERROR,
}

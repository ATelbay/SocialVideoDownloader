package com.socialvideodownloader.feature.library.ui

import androidx.annotation.StringRes

sealed interface LibraryEffect {
    data class OpenContent(val contentUri: String) : LibraryEffect
    data class ShareContent(val contentUri: String) : LibraryEffect
    data class ShowMessage(@StringRes val messageResId: Int) : LibraryEffect
}

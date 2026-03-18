package com.socialvideodownloader.feature.library.ui

sealed interface LibraryIntent {
    data class ItemClicked(val itemId: Long) : LibraryIntent
    data class ItemLongPressed(val itemId: Long) : LibraryIntent
}

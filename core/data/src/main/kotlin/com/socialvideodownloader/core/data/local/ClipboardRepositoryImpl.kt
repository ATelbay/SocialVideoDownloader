package com.socialvideodownloader.core.data.local

import android.content.ClipboardManager
import android.content.Context
import com.socialvideodownloader.core.domain.repository.ClipboardRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class ClipboardRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : ClipboardRepository {

    override fun getVideoUrl(): String? {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip ?: return null
        if (clip.itemCount == 0) return null

        val text = clip.getItemAt(0).text?.toString() ?: return null
        return if (VIDEO_URL_PATTERN.containsMatchIn(text)) text.trim() else null
    }

    companion object {
        private val VIDEO_URL_PATTERN = Regex(
            "(https?://)?(www\\.)?(youtube\\.com|youtu\\.be|tiktok\\.com|instagram\\.com|twitter\\.com|x\\.com|vimeo\\.com)",
            RegexOption.IGNORE_CASE,
        )
    }
}

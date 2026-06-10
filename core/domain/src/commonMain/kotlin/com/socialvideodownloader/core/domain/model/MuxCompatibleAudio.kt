package com.socialvideodownloader.core.domain.model

/**
 * Picks the best audio-only format that can be muxed with a video-only stream of [videoExt]
 * without transcoding. Container compatibility is required by platform muxers
 * (Android MediaMuxer, iOS AVAssetWriter): mp4 video pairs with m4a/mp4 (AAC) audio,
 * webm video pairs with webm/opus audio.
 *
 * Returns null when the container is unknown or no compatible audio with a direct URL exists —
 * callers should then keep the existing single-stream behavior.
 */
fun List<VideoFormatOption>.bestMuxCompatibleAudio(videoExt: String): VideoFormatOption? {
    val compatibleExts =
        when (videoExt) {
            "mp4", "m4v" -> setOf("m4a", "mp4")
            "webm" -> setOf("webm", "opus")
            else -> return null
        }
    return this
        .filter { it.isAudioOnly && it.ext in compatibleExts && it.directDownloadUrl != null }
        .maxByOrNull { it.fileSizeBytes ?: 0L }
}

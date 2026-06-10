package com.socialvideodownloader.shared.data.platform

// Progress segments for the two-stream (video + audio) download on the iOS path,
// expressed as fractions 0..1: video 0..0.85, audio 0.85..0.95, mux + finalize
// complete at 1.0. The Android service path in :core:data keeps its own 0..100 scale.
internal const val MUX_VIDEO_PROGRESS_END = 0.85f
internal const val MUX_AUDIO_PROGRESS_END = 0.95f

/**
 * Maps a single-stream progress [fraction] (0..1, negative when total size is unknown)
 * into the [start]..[end] segment of the overall mux download. Negative fractions map
 * to [start]; fractions above 1 are clamped so segments stay monotonic.
 */
internal fun scaleMuxProgress(
    fraction: Float,
    start: Float,
    end: Float,
): Float =
    if (fraction < 0f) {
        start
    } else {
        start + fraction.coerceAtMost(1f) * (end - start)
    }

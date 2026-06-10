package com.socialvideodownloader.core.data.media

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File
import java.nio.ByteBuffer
import javax.inject.Inject

/**
 * Merges a video-only stream and an audio-only stream into a single container without
 * transcoding, using the platform MediaExtractor/MediaMuxer.
 *
 * Container support follows MediaMuxer: mp4 output (H.264/H.265/AV1 + AAC) and
 * webm output (VP8/VP9 + Opus/Vorbis). Streams must already be container-compatible —
 * pairing is the caller's responsibility (see bestMuxCompatibleAudio in :core:domain).
 */
class StreamMuxer
    @Inject
    constructor() {
        fun mux(
            videoFile: File,
            audioFile: File,
            outputFile: File,
            containerExt: String,
        ) {
            val outputFormat =
                when (containerExt) {
                    "mp4", "m4v" -> MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
                    "webm" -> MediaMuxer.OutputFormat.MUXER_OUTPUT_WEBM
                    else -> throw IllegalArgumentException("Unsupported mux container: $containerExt")
                }

            val videoExtractor = MediaExtractor()
            val audioExtractor = MediaExtractor()
            var muxer: MediaMuxer? = null
            var started = false
            try {
                videoExtractor.setDataSource(videoFile.absolutePath)
                audioExtractor.setDataSource(audioFile.absolutePath)
                val videoTrackIndex = selectTrack(videoExtractor, "video/")
                val audioTrackIndex = selectTrack(audioExtractor, "audio/")
                val videoFormat = videoExtractor.getTrackFormat(videoTrackIndex)
                val audioFormat = audioExtractor.getTrackFormat(audioTrackIndex)

                muxer = MediaMuxer(outputFile.absolutePath, outputFormat)
                val sources =
                    listOf(
                        TrackSource(videoExtractor, muxer.addTrack(videoFormat)),
                        TrackSource(audioExtractor, muxer.addTrack(audioFormat)),
                    )
                if (outputFormat == MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4 &&
                    videoFormat.containsKey(MediaFormat.KEY_ROTATION)
                ) {
                    muxer.setOrientationHint(videoFormat.getInteger(MediaFormat.KEY_ROTATION))
                }
                muxer.start()
                started = true

                copySamplesInterleaved(sources, muxer, bufferSize(videoFormat, audioFormat))

                muxer.stop()
                started = false
            } finally {
                videoExtractor.release()
                audioExtractor.release()
                if (started) runCatching { muxer?.stop() }
                runCatching { muxer?.release() }
            }
        }

        private class TrackSource(
            val extractor: MediaExtractor,
            val outTrackIndex: Int,
        ) {
            var exhausted = false
        }

        /**
         * Copies samples ordered by presentation time across both tracks. WebM clusters require
         * near-monotonic timestamps across tracks, so track-by-track copying is not safe.
         */
        private fun copySamplesInterleaved(
            sources: List<TrackSource>,
            muxer: MediaMuxer,
            bufferSize: Int,
        ) {
            val buffer = ByteBuffer.allocateDirect(bufferSize)
            val bufferInfo = MediaCodec.BufferInfo()
            while (true) {
                val source =
                    sources
                        .filter { !it.exhausted }
                        .minByOrNull { it.extractor.sampleTime }
                        ?: break
                val sampleTime = source.extractor.sampleTime
                if (sampleTime < 0) {
                    source.exhausted = true
                    continue
                }
                val size = source.extractor.readSampleData(buffer, 0)
                if (size < 0) {
                    source.exhausted = true
                    continue
                }
                bufferInfo.offset = 0
                bufferInfo.size = size
                bufferInfo.presentationTimeUs = sampleTime
                bufferInfo.flags =
                    if (source.extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) {
                        MediaCodec.BUFFER_FLAG_KEY_FRAME
                    } else {
                        0
                    }
                muxer.writeSampleData(source.outTrackIndex, buffer, bufferInfo)
                if (!source.extractor.advance()) {
                    source.exhausted = true
                }
            }
        }

        private fun selectTrack(
            extractor: MediaExtractor,
            mimePrefix: String,
        ): Int {
            for (i in 0 until extractor.trackCount) {
                val mime = extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith(mimePrefix)) {
                    extractor.selectTrack(i)
                    return i
                }
            }
            throw IllegalStateException("No $mimePrefix* track found")
        }

        private fun bufferSize(vararg formats: MediaFormat): Int {
            val maxInputSize =
                formats.maxOf { format ->
                    if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                        format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
                    } else {
                        0
                    }
                }
            return maxOf(maxInputSize, DEFAULT_BUFFER_SIZE_BYTES)
        }

        private companion object {
            // readSampleData throws if the buffer is smaller than the sample; high-resolution
            // keyframes can exceed 1 MiB, so default generously when the format omits
            // KEY_MAX_INPUT_SIZE.
            const val DEFAULT_BUFFER_SIZE_BYTES = 4 * 1024 * 1024
        }
    }

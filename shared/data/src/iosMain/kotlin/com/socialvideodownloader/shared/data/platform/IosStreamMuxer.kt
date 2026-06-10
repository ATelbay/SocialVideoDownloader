package com.socialvideodownloader.shared.data.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readValue
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFoundation.AVAssetExportPresetPassthrough
import platform.AVFoundation.AVAssetExportSession
import platform.AVFoundation.AVAssetExportSessionStatusCompleted
import platform.AVFoundation.AVAssetTrack
import platform.AVFoundation.AVFileTypeMPEG4
import platform.AVFoundation.AVMediaTypeAudio
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.AVMutableComposition
import platform.AVFoundation.AVURLAsset
import platform.AVFoundation.addMutableTrackWithMediaType
import platform.AVFoundation.preferredTransform
import platform.AVFoundation.tracksWithMediaType
import platform.CoreMedia.CMTimeRangeMake
import platform.CoreMedia.kCMTimeZero
import platform.Foundation.NSError
import platform.Foundation.NSURL
import kotlin.coroutines.resume

/**
 * Merges a video-only stream and an audio-only stream into a single mp4 container without
 * transcoding, using AVMutableComposition + AVAssetExportSession passthrough export.
 *
 * AVFoundation cannot read webm, so only mp4/m4v video (+m4a/mp4 audio) inputs are supported —
 * the caller gates on container ext before invoking. There is no automated test for this class
 * (it needs real media fixtures); verify manually on a device/simulator by downloading a
 * YouTube format above 360p and checking the saved file has audio.
 */
@OptIn(ExperimentalForeignApi::class)
class IosStreamMuxer {
    /**
     * Returns true on success; false when the export failed or either stream lacks the
     * expected track — the caller then falls back to saving the video-only stream.
     */
    suspend fun mux(
        videoUrl: NSURL,
        audioUrl: NSURL,
        outputUrl: NSURL,
    ): Boolean {
        val videoAsset = AVURLAsset(uRL = videoUrl, options = null)
        val audioAsset = AVURLAsset(uRL = audioUrl, options = null)
        val videoTrack =
            videoAsset.tracksWithMediaType(AVMediaTypeVideo).firstOrNull() as? AVAssetTrack
                ?: return false
        val audioTrack =
            audioAsset.tracksWithMediaType(AVMediaTypeAudio).firstOrNull() as? AVAssetTrack
                ?: return false

        val composition = AVMutableComposition()
        val compositionVideoTrack =
            composition.addMutableTrackWithMediaType(
                AVMediaTypeVideo,
                preferredTrackID = K_CM_PERSISTENT_TRACK_ID_INVALID,
            ) ?: return false
        val compositionAudioTrack =
            composition.addMutableTrackWithMediaType(
                AVMediaTypeAudio,
                preferredTrackID = K_CM_PERSISTENT_TRACK_ID_INVALID,
            ) ?: return false

        // Audio is clamped to the video duration so the merged file never ends on a black frame.
        val timeZero = kCMTimeZero.readValue()
        val videoRange = CMTimeRangeMake(timeZero, videoAsset.duration)
        memScoped {
            val insertError = alloc<ObjCObjectVar<NSError?>>()
            if (!compositionVideoTrack.insertTimeRange(
                    videoRange,
                    ofTrack = videoTrack,
                    atTime = timeZero,
                    error = insertError.ptr,
                )
            ) {
                return false
            }
            if (!compositionAudioTrack.insertTimeRange(
                    videoRange,
                    ofTrack = audioTrack,
                    atTime = timeZero,
                    error = insertError.ptr,
                )
            ) {
                return false
            }
        }
        // Passthrough export keeps the source rotation metadata only on the composition track.
        compositionVideoTrack.preferredTransform = videoTrack.preferredTransform

        val exportSession =
            AVAssetExportSession(
                asset = composition,
                presetName = AVAssetExportPresetPassthrough,
            )
        exportSession.outputURL = outputUrl
        exportSession.outputFileType = AVFileTypeMPEG4
        exportSession.shouldOptimizeForNetworkUse = true

        return suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { exportSession.cancelExport() }
            exportSession.exportAsynchronouslyWithCompletionHandler {
                continuation.resume(exportSession.status == AVAssetExportSessionStatusCompleted)
            }
        }
    }

    private companion object {
        // kCMPersistentTrackID_Invalid — not exposed by the CoreMedia interop as a Kotlin symbol.
        const val K_CM_PERSISTENT_TRACK_ID_INVALID = 0
    }
}

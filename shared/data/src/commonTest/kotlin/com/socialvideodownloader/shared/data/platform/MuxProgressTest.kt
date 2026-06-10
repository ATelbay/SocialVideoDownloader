package com.socialvideodownloader.shared.data.platform

import kotlin.test.Test
import kotlin.test.assertEquals

class MuxProgressTest {
    @Test
    fun zeroFractionMapsToSegmentStart() {
        assertEquals(0f, scaleMuxProgress(0f, 0f, MUX_VIDEO_PROGRESS_END))
        assertEquals(
            MUX_VIDEO_PROGRESS_END,
            scaleMuxProgress(0f, MUX_VIDEO_PROGRESS_END, MUX_AUDIO_PROGRESS_END),
        )
    }

    @Test
    fun fullFractionMapsToSegmentEnd() {
        assertEquals(MUX_VIDEO_PROGRESS_END, scaleMuxProgress(1f, 0f, MUX_VIDEO_PROGRESS_END))
        assertEquals(
            MUX_AUDIO_PROGRESS_END,
            scaleMuxProgress(1f, MUX_VIDEO_PROGRESS_END, MUX_AUDIO_PROGRESS_END),
        )
    }

    @Test
    fun midFractionMapsLinearlyInsideSegment() {
        assertEquals(0.425f, scaleMuxProgress(0.5f, 0f, MUX_VIDEO_PROGRESS_END), absoluteTolerance = 1e-6f)
        assertEquals(
            0.9f,
            scaleMuxProgress(0.5f, MUX_VIDEO_PROGRESS_END, MUX_AUDIO_PROGRESS_END),
            absoluteTolerance = 1e-6f,
        )
    }

    @Test
    fun negativeFractionMapsToSegmentStart() {
        // NSURLSession reports unknown totals as a negative fraction.
        assertEquals(0f, scaleMuxProgress(-1f, 0f, MUX_VIDEO_PROGRESS_END))
        assertEquals(
            MUX_VIDEO_PROGRESS_END,
            scaleMuxProgress(-1f, MUX_VIDEO_PROGRESS_END, MUX_AUDIO_PROGRESS_END),
        )
    }

    @Test
    fun fractionAboveOneClampsToSegmentEnd() {
        assertEquals(MUX_VIDEO_PROGRESS_END, scaleMuxProgress(1.5f, 0f, MUX_VIDEO_PROGRESS_END))
        assertEquals(
            MUX_AUDIO_PROGRESS_END,
            scaleMuxProgress(2f, MUX_VIDEO_PROGRESS_END, MUX_AUDIO_PROGRESS_END),
        )
    }

    @Test
    fun segmentsAreContiguousAndMonotonic() {
        // The audio segment starts exactly where the video segment ends, so the overall
        // progress never moves backwards when the stage switches.
        assertEquals(
            scaleMuxProgress(1f, 0f, MUX_VIDEO_PROGRESS_END),
            scaleMuxProgress(0f, MUX_VIDEO_PROGRESS_END, MUX_AUDIO_PROGRESS_END),
        )
    }
}

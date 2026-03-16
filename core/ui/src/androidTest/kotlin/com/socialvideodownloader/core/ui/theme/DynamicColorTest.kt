package com.socialvideodownloader.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.socialvideodownloader.core.ui.tokens.PlatformColors
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verify SVD theme applies the correct fixed color tokens.
 */
@RunWith(AndroidJUnit4::class)
class DynamicColorTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var capturedPrimary = Color.Unspecified
    private var capturedSuccess = Color.Unspecified
    private var capturedOnSuccess = Color.Unspecified
    private var capturedSuccessContainer = Color.Unspecified

    @Composable
    private fun CaptureColors() {
        capturedPrimary = MaterialTheme.colorScheme.primary
        capturedSuccess = MaterialTheme.extendedColors.success
        capturedOnSuccess = MaterialTheme.extendedColors.onSuccess
        capturedSuccessContainer = MaterialTheme.extendedColors.successContainer
    }

    @Test
    fun theme_primaryMatchesSvdPalette() {
        composeRule.setContent {
            SocialVideoDownloaderTheme {
                CaptureColors()
            }
        }

        assertEquals(
            "Primary must match SvdPrimary",
            SvdPrimary,
            capturedPrimary,
        )
    }

    @Test
    fun extendedColors_successMatchesSvdPalette() {
        composeRule.setContent {
            SocialVideoDownloaderTheme {
                CaptureColors()
            }
        }

        assertEquals(SvdExtendedColors.success, capturedSuccess)
        assertEquals(SvdExtendedColors.onSuccess, capturedOnSuccess)
        assertEquals(SvdExtendedColors.successContainer, capturedSuccessContainer)
    }

    @Test
    fun platformColors_areAlwaysFixed() {
        assertEquals(Color(0xFFFF0000), PlatformColors.YouTube)
        assertEquals(Color(0xFFC13584), PlatformColors.Instagram)
        assertEquals(Color(0xFF69C9D0), PlatformColors.TikTok)
        assertEquals(Color(0xFF1DA1F2), PlatformColors.Twitter)
        assertEquals(Color(0xFF1AB7EA), PlatformColors.Vimeo)
        assertEquals(Color(0xFF1877F2), PlatformColors.Facebook)
        assertEquals(Color.White, PlatformColors.TextOnPlatform)
    }

    @Test
    fun platformColors_forPlatform_returnsCorrectBrandColor() {
        assertEquals(Color(0xFFFF0000), PlatformColors.forPlatform("YouTube"))
        assertEquals(Color(0xFFC13584), PlatformColors.forPlatform("instagram"))
        assertEquals(Color(0xFF69C9D0), PlatformColors.forPlatform("TikTok"))
        assertEquals(Color(0xFF1DA1F2), PlatformColors.forPlatform("twitter"))
        assertEquals(Color(0xFF1AB7EA), PlatformColors.forPlatform("vimeo"))
        assertEquals(Color(0xFF1877F2), PlatformColors.forPlatform("Facebook"))
        assertEquals(PlatformColors.Default, PlatformColors.forPlatform("SomeUnknownSite"))
        assertEquals(PlatformColors.Default, PlatformColors.forPlatform(null))
    }
}

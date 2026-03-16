package com.socialvideodownloader.core.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.socialvideodownloader.core.ui.tokens.PlatformColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * T040: Verify dynamic color behavior on API 31+ emulator.
 *
 * Checks:
 * 1. On API 31+ with dynamicColor=true, M3 palette primary/secondary are wallpaper-derived
 *    (i.e. they differ from the static Figma palette values).
 * 2. ExtendedColors (success) are always fixed — never changed by dynamic color.
 * 3. PlatformColors (badge colors) are always fixed constants.
 */
@RunWith(AndroidJUnit4::class)
class DynamicColorTest {

    @get:Rule
    val composeRule = createComposeRule()

    // --- Helpers to capture colors out of composition ---

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

    // --- Tests ---

    /**
     * On API 31+, dynamic color is enabled so the M3 primary color should come from
     * the wallpaper palette, NOT from the static Figma value #6750A4.
     *
     * On API < 31 the static palette is used, so primary equals the Figma value.
     */
    @Test
    fun dynamicColor_api31Plus_primaryDiffersFromStaticPalette() {
        composeRule.setContent {
            SocialVideoDownloaderTheme(darkTheme = false, dynamicColor = true) {
                CaptureColors()
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Dynamic color overrides the static Figma primary
            assertNotEquals(
                "On API 31+ with dynamic color, primary should be wallpaper-derived, not the static #6750A4",
                LightColorScheme.primary,
                capturedPrimary,
            )
        } else {
            // Below API 31 fallback to static palette
            assertEquals(
                "Below API 31, primary must equal the static Figma palette value",
                LightColorScheme.primary,
                capturedPrimary,
            )
        }
    }

    /**
     * When dynamicColor=false, even on API 31+, the static Figma primary is used.
     */
    @Test
    fun dynamicColorDisabled_primaryMatchesStaticPalette() {
        composeRule.setContent {
            SocialVideoDownloaderTheme(darkTheme = false, dynamicColor = false) {
                CaptureColors()
            }
        }

        assertEquals(
            "With dynamicColor=false, primary must match the static Figma palette value",
            LightColorScheme.primary,
            capturedPrimary,
        )
    }

    /**
     * ExtendedColors.success is always fixed — never influenced by dynamic color.
     */
    @Test
    fun extendedColors_success_isFixedRegardlessOfDynamicColor() {
        // With dynamic color ON
        composeRule.setContent {
            SocialVideoDownloaderTheme(darkTheme = false, dynamicColor = true) {
                CaptureColors()
            }
        }
        val successWithDynamic = capturedSuccess
        val onSuccessWithDynamic = capturedOnSuccess
        val successContainerWithDynamic = capturedSuccessContainer

        // With dynamic color OFF
        composeRule.setContent {
            SocialVideoDownloaderTheme(darkTheme = false, dynamicColor = false) {
                CaptureColors()
            }
        }

        assertEquals(
            "success color must be the same whether dynamic color is on or off",
            successWithDynamic,
            capturedSuccess,
        )
        assertEquals(
            "onSuccess color must be the same whether dynamic color is on or off",
            onSuccessWithDynamic,
            capturedOnSuccess,
        )
        assertEquals(
            "successContainer color must be the same whether dynamic color is on or off",
            successContainerWithDynamic,
            capturedSuccessContainer,
        )

        // Also assert exact values match design spec
        assertEquals(LightExtendedColors.success, capturedSuccess)
        assertEquals(LightExtendedColors.onSuccess, capturedOnSuccess)
        assertEquals(LightExtendedColors.successContainer, capturedSuccessContainer)
    }

    /**
     * Dark theme ExtendedColors.success uses dark palette values regardless of dynamic color.
     */
    @Test
    fun extendedColors_darkTheme_success_isFixedRegardlessOfDynamicColor() {
        composeRule.setContent {
            SocialVideoDownloaderTheme(darkTheme = true, dynamicColor = true) {
                CaptureColors()
            }
        }
        assertEquals(DarkExtendedColors.success, capturedSuccess)
        assertEquals(DarkExtendedColors.onSuccess, capturedOnSuccess)
        assertEquals(DarkExtendedColors.successContainer, capturedSuccessContainer)
    }

    /**
     * PlatformColors are plain Color constants — they must never be changed by theming.
     */
    @Test
    fun platformColors_areAlwaysFixed() {
        assertEquals(Color(0xFFFF0000), PlatformColors.YouTube)
        assertEquals(Color(0xFFC13584), PlatformColors.Instagram)
        assertEquals(Color(0xFF010101), PlatformColors.TikTok)
        assertEquals(Color(0xFF1DA1F2), PlatformColors.Twitter)
        assertEquals(Color(0xFF1AB7EA), PlatformColors.Vimeo)
        assertEquals(Color(0xFF1877F2), PlatformColors.Facebook)
        assertEquals(Color.White, PlatformColors.TextOnPlatform)
    }

    /**
     * PlatformColors.forPlatform() returns the right brand color for known platforms
     * and the fallback outline color for unknowns.
     */
    @Test
    fun platformColors_forPlatform_returnsCorrectBrandColor() {
        assertEquals(Color(0xFFFF0000), PlatformColors.forPlatform("YouTube"))
        assertEquals(Color(0xFFC13584), PlatformColors.forPlatform("instagram"))
        assertEquals(Color(0xFF010101), PlatformColors.forPlatform("TikTok"))
        assertEquals(Color(0xFF1DA1F2), PlatformColors.forPlatform("twitter"))
        assertEquals(Color(0xFF1AB7EA), PlatformColors.forPlatform("vimeo"))
        assertEquals(Color(0xFF1877F2), PlatformColors.forPlatform("Facebook"))
        assertEquals(PlatformColors.Default, PlatformColors.forPlatform("SomeUnknownSite"))
        assertEquals(PlatformColors.Default, PlatformColors.forPlatform(null))
    }
}

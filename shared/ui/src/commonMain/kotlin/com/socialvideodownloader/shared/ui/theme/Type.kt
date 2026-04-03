package com.socialvideodownloader.shared.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.socialvideodownloader.shared.ui.generated.resources.Res
import com.socialvideodownloader.shared.ui.generated.resources.inter_medium
import com.socialvideodownloader.shared.ui.generated.resources.inter_regular
import com.socialvideodownloader.shared.ui.generated.resources.inter_semibold
import com.socialvideodownloader.shared.ui.generated.resources.space_grotesk_bold
import com.socialvideodownloader.shared.ui.generated.resources.space_grotesk_medium
import com.socialvideodownloader.shared.ui.generated.resources.space_grotesk_semibold
import org.jetbrains.compose.resources.Font

@Composable
fun spaceGroteskFontFamily(): FontFamily =
    FontFamily(
        Font(Res.font.space_grotesk_bold, FontWeight.Bold),
        Font(Res.font.space_grotesk_semibold, FontWeight.SemiBold),
        Font(Res.font.space_grotesk_medium, FontWeight.Medium),
    )

@Composable
fun interFontFamily(): FontFamily =
    FontFamily(
        Font(Res.font.inter_regular, FontWeight.Normal),
        Font(Res.font.inter_medium, FontWeight.Medium),
        Font(Res.font.inter_semibold, FontWeight.SemiBold),
    )

@Composable
fun svdTypography(): Typography {
    val spaceGrotesk = spaceGroteskFontFamily()
    val inter = interFontFamily()
    return Typography(
        displayLarge =
            TextStyle(
                fontFamily = spaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                lineHeight = 40.sp,
            ),
        headlineLarge =
            TextStyle(
                fontFamily = spaceGrotesk,
                fontWeight = FontWeight.SemiBold,
                fontSize = 24.sp,
                lineHeight = 32.sp,
            ),
        headlineMedium =
            TextStyle(
                fontFamily = spaceGrotesk,
                fontWeight = FontWeight.Medium,
                fontSize = 20.sp,
                lineHeight = 28.sp,
            ),
        titleMedium =
            TextStyle(
                fontFamily = inter,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 24.sp,
            ),
        bodyLarge =
            TextStyle(
                fontFamily = inter,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
            ),
        bodyMedium =
            TextStyle(
                fontFamily = inter,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            ),
        labelLarge =
            TextStyle(
                fontFamily = inter,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            ),
        labelMedium =
            TextStyle(
                fontFamily = inter,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            ),
    )
}

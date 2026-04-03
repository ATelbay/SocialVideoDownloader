package com.socialvideodownloader.shared.di

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIColor
import platform.UIKit.UIViewController

private fun sharedAppBackgroundColor(): UIColor =
    UIColor(
        red = 246.0 / 255.0,
        green = 243.0 / 255.0,
        blue = 236.0 / 255.0,
        alpha = 1.0,
    )

fun SharedAppViewController(): UIViewController =
    ComposeUIViewController { SharedApp() }.apply {
        view.backgroundColor = sharedAppBackgroundColor()
    }

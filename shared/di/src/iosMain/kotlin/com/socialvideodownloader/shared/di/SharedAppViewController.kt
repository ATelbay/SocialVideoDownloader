package com.socialvideodownloader.shared.di

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

fun SharedAppViewController(): UIViewController = ComposeUIViewController { SharedApp() }

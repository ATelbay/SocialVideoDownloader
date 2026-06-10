plugins {
    id("svd.android.library")
}

android {
    namespace = "com.socialvideodownloader.core.ui"
}

// This module now only hosts Android share/open intent utilities (IntentUtils). The Compose
// design system lives in :shared:ui; the former duplicate here was removed. IntentUtils relies
// solely on the Android framework, so no extra dependencies are required.
